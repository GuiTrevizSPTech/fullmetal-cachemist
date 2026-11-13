package br.com.cachemist.fullmetal.conteudo;

import br.com.cachemist.fullmetal.conteudo.dto.ConteudoMapper;
import br.com.cachemist.fullmetal.conteudo.dto.ConteudoRequest;
import br.com.cachemist.fullmetal.conteudo.dto.ConteudoResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/conteudos")
public class ConteudoController {

    private final ConteudoService service;

    public ConteudoController(ConteudoService service) {
        this.service = service;
    }

    /** Busca primeiro no cache; se não houver (ou estiver expirado), busca na fonte e armazena. Retorna o header X-Cache: HIT ou X-Cache: MISS. */
    @GetMapping("/{id}")
    public ResponseEntity<ConteudoResponse> buscar(@PathVariable Long id) {
        ResultadoBusca resultado = service.buscarPorId(id);
        return ResponseEntity.ok()
                .header("X-Cache", resultado.hit() ? "HIT" : "MISS")
                .body(ConteudoMapper.toResponse(resultado.conteudo()));
    }

    /** Lista tudo direto do banco (sem cache), so para ajudar na demonstracao. */
    @GetMapping
    public List<ConteudoResponse> listar() {
        return service.listar().stream().map(ConteudoMapper::toResponse).toList();
    }

    /** Cadastra um conteúdo. */
    @PostMapping
    public ResponseEntity<ConteudoResponse> criar(@RequestBody @Valid ConteudoRequest request) {
        Conteudo salvo = service.criar(ConteudoMapper.toEntity(request));
        return ResponseEntity.created(URI.create("/conteudos/" + salvo.getId()))
                .body(ConteudoMapper.toResponse(salvo));
    }

    /** Atualiza o conteúdo e invalida a entrada no cache. */
    @PutMapping("/{id}")
    public ConteudoResponse atualizar(@PathVariable Long id, @RequestBody @Valid ConteudoRequest request) {
        Conteudo atualizado = service.atualizar(id, ConteudoMapper.toEntity(request));
        return ConteudoMapper.toResponse(atualizado);
    }

    /** Exclui o conteúdo e invalida a entrada no cache. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        service.remover(id);
        return ResponseEntity.noContent().build();
    }
}
