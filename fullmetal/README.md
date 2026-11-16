# Fullmetal Cachemist

Projeto da atividade "Cache de Conteúdo com HashMap no Spring Boot" (Parte 2).

A API devolve conteúdos que vêm de uma fonte lenta (uma busca no banco com atraso de 2 segundos) e usa um cache em memória feito à mão com `ConcurrentHashMap` para não repetir essa busca. Não usamos `@Cacheable`, Caffeine, Redis nem outra biblioteca de cache.

O tema é Fullmetal Alchemist: o cache é a memória do alquimista, a fonte lenta é a biblioteca e cada `Conteudo` é um círculo de transmutação. Os nomes das classes e rotas seguem o enunciado.

Integrantes: César Augusto Araujo Miguel e Guilherme Trevizan Conceicao.

## Como rodar

Precisa de JDK 21 ou mais novo.

```bash
cd fullmetal
./mvnw spring-boot:run      # Linux/macOS
mvnw.cmd spring-boot:run    # Windows
```

A aplicação sobe em `http://localhost:8080` e cadastra 7 conteúdos (ids 1 a 7) quando o banco está vazio.

### Banco

O `application.properties` está no `.gitignore` porque tem usuário e senha. Para usar o MySQL, copie `src/main/resources/application.properties.example` para `application.properties` na mesma pasta e ajuste os dados de conexão.

Sem esse arquivo o Spring Boot usa um H2 em memória, então dá para rodar direto depois de clonar.

Os testes usam `src/test/resources/application.properties`, que aponta para o H2.

### Configuração

| Propriedade | Padrão | Para que serve |
|---|---|---|
| `app.cache.max-itens` | 5 | quantos itens cabem no cache |
| `app.cache.ttl-segundos` | 60 | tempo de validade de cada item |
| `app.cache.politica` | LRU | qual item sai quando o cache enche (`LRU` ou `FIFO`) |
| `app.cache.limpeza-segundos` | 30 | intervalo da rotina que remove itens expirados |
| `app.biblioteca.atraso-ms` | 2000 | atraso da fonte lenta |

## Rotas

| Método | Rota | O que faz |
|---|---|---|
| GET | `/conteudos/{id}` | busca no cache e, se não achar, na fonte. Responde com o header `X-Cache: HIT` ou `MISS` |
| GET | `/conteudos` | lista todos direto do banco (usado só para conferir os dados) |
| POST | `/conteudos` | cadastra. Body: `{"titulo": "...", "corpo": "...", "autor": "..."}` |
| PUT | `/conteudos/{id}` | atualiza e remove o id do cache |
| DELETE | `/conteudos/{id}` | exclui e remove o id do cache |
| GET | `/cache/estatisticas` | tamanho, limite, TTL, política, hits, misses, evictions, expirações e hit ratio |
| DELETE | `/cache` | limpa o cache (os contadores continuam) |

Id que não existe retorna 404 e body inválido retorna 400.

## Como o cache funciona

O GET por id segue o padrão cache-aside:

1. o `ConteudoService` procura o id no `ConteudoCache`;
2. se encontrar um item válido, devolve (HIT);
3. se não encontrar ou se o item expirou, busca na `BibliotecaLenta`, guarda no cache e devolve (MISS).

Cada item do cache é uma `EntradaCache` com o valor, o momento em que foi guardado (`armazenadoEm`) e o último acesso (`ultimoAcesso`).

- **TTL:** o item vale por `ttl-segundos` contados a partir de `armazenadoEm`. Acessar o item não renova esse prazo. Quando passa do prazo, a busca conta como MISS e o item é recarregado.
- **Limite e LRU:** com o cache cheio, antes de guardar um item novo sai o que tem o `ultimoAcesso` mais antigo. Cada HIT atualiza o `ultimoAcesso`.
- **FIFO:** com `app.cache.politica=FIFO`, sai o item com o `armazenadoEm` mais antigo, mesmo que tenha sido usado há pouco.
- **Invalidação:** PUT e DELETE removem o id do cache logo depois de gravar no banco, então a próxima leitura já traz o dado novo.
- **Rotina de limpeza:** a `RotinaLimpezaCache` (`@Scheduled`) remove os itens expirados a cada `limpeza-segundos`.
- **Concorrência:** o mapa é um `ConcurrentHashMap`, os contadores são `AtomicLong` e o método `guardar` é `synchronized`, para que duas requisições ao mesmo tempo não passem do limite.

Exemplo de log:

```
Cache MISS - id=3 - 2003 ms
Cache HIT  - id=3 - 0 ms
Memoria cheia: esquecendo id=2 (LRU, guardado em ..., ultimo acesso ...)
Lembranca desbotou (TTL) - id=1 - guardada em ...
Rotina de limpeza: 1 lembrancas expiradas removidas
Lembranca invalidada - id=1
```

## Estrutura

```
fullmetal/src/main/java/br/com/cachemist/fullmetal/
├── FullmetalApplication.java
├── conteudo/
│   ├── Conteudo.java              entidade JPA
│   ├── ConteudoRepository.java
│   ├── ConteudoService.java       regras de negócio e uso do cache
│   ├── ResultadoBusca.java        conteúdo + se foi HIT ou MISS
│   ├── ConteudoController.java    rotas /conteudos
│   └── dto/                       ConteudoRequest, ConteudoResponse, ConteudoMapper
├── cache/
│   ├── CacheSimples.java          cache genérico <K, V>
│   ├── ConteudoCache.java         CacheSimples<Long, Conteudo> usado pelo service
│   ├── EntradaCache.java
│   ├── PoliticaRemocao.java       LRU e FIFO
│   ├── RotinaLimpezaCache.java    @Scheduled que remove expirados
│   ├── CacheProperties.java       leitura do app.cache.*
│   ├── EstatisticasCache.java
│   └── CacheController.java       rotas /cache
├── biblioteca/
│   ├── BibliotecaLenta.java       fonte lenta (Thread.sleep)
│   └── PovoarBiblioteca.java      cadastra os 7 conteúdos iniciais
└── erro/
    ├── ConteudoNaoEncontradoException.java
    └── GlobalExceptionHandler.java
```

O controller trabalha com DTOs e usa o `ConteudoMapper` para converter. O service recebe e devolve a entidade e é a única classe que usa o cache.

## Roteiro de demonstração

Os comandos usam `curl -i` para mostrar o header `X-Cache`. No Postman, o header aparece na aba Headers.

1. Listar os conteúdos:
   ```bash
   curl http://localhost:8080/conteudos
   ```
2. MISS e depois HIT:
   ```bash
   curl -i http://localhost:8080/conteudos/1   # MISS, cerca de 2 s
   curl -i http://localhost:8080/conteudos/1   # HIT
   ```
3. Limite e LRU:
   ```bash
   curl -X DELETE http://localhost:8080/cache
   for id in 1 2 3 4 5; do curl -s -o /dev/null http://localhost:8080/conteudos/$id; done
   curl -i http://localhost:8080/conteudos/1   # HIT
   curl -i http://localhost:8080/conteudos/6   # MISS, o id 2 sai do cache
   curl -i http://localhost:8080/conteudos/2   # MISS
   ```
4. Invalidação depois do PUT:
   ```bash
   curl -i http://localhost:8080/conteudos/4   # HIT
   curl -X PUT http://localhost:8080/conteudos/4 -H "Content-Type: application/json" \
        -d '{"titulo":"Transmutacao de armadura (revisada)","corpo":"Revisao do Estado","autor":"Edward Elric"}'
   curl -i http://localhost:8080/conteudos/4   # MISS, já com o título novo
   ```
5. TTL:
   ```bash
   curl -i http://localhost:8080/conteudos/7   # MISS
   # esperar mais de 60 s
   curl -i http://localhost:8080/conteudos/7   # MISS de novo
   ```
6. Estatísticas:
   ```bash
   curl http://localhost:8080/cache/estatisticas
   ```

Para ver o FIFO, troque `app.cache.politica` para `FIFO`, reinicie e repita o passo 3: o id 1 sai do cache mesmo depois do HIT.

## Desafios extras

- Cache genérico: `CacheSimples<K, V>`. O `ConteudoCache` estende `CacheSimples<Long, Conteudo>`.
- Política configurável: enum `PoliticaRemocao` e a propriedade `app.cache.politica`.
- Remoção de expirados com `@Scheduled`: `RotinaLimpezaCache`.
- Testes unitários do cache: `CacheSimplesTest`.

## Testes

```bash
cd fullmetal
./mvnw test
```

O `CacheSimples` recebe um `Clock`. Nos testes usamos um relógio falso (`RelogioFalso`) que só avança quando o teste manda, então dá para testar o TTL sem esperar.

Os testes cobrem: MISS seguido de HIT, limite de itens, remoção por LRU e por FIFO, regravar uma chave existente, expiração no limite exato do TTL, TTL sem renovação, a rotina de remoção de expirados, invalidação, limpeza do cache e limite inválido na configuração.
