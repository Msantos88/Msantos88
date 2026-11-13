package br.com.barbearia;

import br.com.barbearia.domain.PoliticaDeSinal;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PoliticaDeSinalTest {

    private final PoliticaDeSinal metade = new PoliticaDeSinal(50);

    @Test
    void calculaCinquentaPorCento() {
        assertThat(metade.calcularSinal(new BigDecimal("50.00"))).isEqualByComparingTo("25.00");
        assertThat(metade.calcularSinal(new BigDecimal("45.50"))).isEqualByComparingTo("22.75");
    }

    @Test
    void arredondaCentavosParaCima() {
        assertThat(metade.calcularSinal(new BigDecimal("33.33"))).isEqualByComparingTo("16.67");
    }

    @Test
    void rejeitaPercentualInvalido() {
        assertThatThrownBy(() -> new PoliticaDeSinal(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PoliticaDeSinal(101)).isInstanceOf(IllegalArgumentException.class);
    }
}
