package br.com.cachemist.fullmetal.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Classe responsável por simular um relógio nos testes: o tempo só anda quando o teste manda */
class RelogioFalso extends Clock {

    private Instant agora = Instant.parse("2026-01-01T00:00:00Z");

    void avancar(Duration tempo) {
        agora = agora.plus(tempo);
    }

    @Override
    public Instant instant() {
        return agora;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }
}
