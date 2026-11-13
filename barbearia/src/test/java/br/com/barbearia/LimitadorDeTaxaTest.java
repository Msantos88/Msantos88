package br.com.barbearia;

import br.com.barbearia.config.LimitadorDeTaxa;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LimitadorDeTaxaTest {

    @Test
    void permiteAteOLimiteELiberaNaProximaJanela() {
        RelogioDeTeste relogio = new RelogioDeTeste(LocalDateTime.of(2026, 1, 1, 10, 0), ZoneId.of("UTC"));
        LimitadorDeTaxa limitador = new LimitadorDeTaxa(2, Duration.ofMinutes(10), relogio);

        assertThat(limitador.permitir("a")).isTrue();
        assertThat(limitador.permitir("a")).isTrue();
        assertThat(limitador.permitir("a")).isFalse();
        assertThat(limitador.permitir("b")).isTrue();
        assertThat(limitador.segundosParaLiberar("a")).isEqualTo(600);

        relogio.avancar(Duration.ofMinutes(10));
        assertThat(limitador.permitir("a")).isTrue();
    }
}
