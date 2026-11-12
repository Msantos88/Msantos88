package br.com.barbearia;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Relógio controlável para testar prazos de pagamento. */
public class RelogioDeTeste extends Clock {

    private final ZoneId zona;
    private Instant agora;

    public RelogioDeTeste(LocalDateTime inicio, ZoneId zona) {
        this.zona = zona;
        this.agora = inicio.atZone(zona).toInstant();
    }

    public void definir(LocalDateTime momento) {
        this.agora = momento.atZone(zona).toInstant();
    }

    public void avancar(Duration d) {
        this.agora = agora.plus(d);
    }

    @Override public ZoneId getZone() { return zona; }
    @Override public Clock withZone(ZoneId zone) { throw new UnsupportedOperationException(); }
    @Override public Instant instant() { return agora; }
}
