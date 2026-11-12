package br.com.barbearia.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regra de negócio: para garantir a reserva o cliente paga um sinal
 * (por padrão 50%) do valor do serviço. O restante é pago na barbearia.
 */
public final class PoliticaDeSinal {

    private final BigDecimal percentual;

    public PoliticaDeSinal(int percentual) {
        if (percentual <= 0 || percentual > 100) {
            throw new IllegalArgumentException("Percentual do sinal deve estar entre 1 e 100");
        }
        this.percentual = BigDecimal.valueOf(percentual);
    }

    public BigDecimal calcularSinal(BigDecimal valorTotal) {
        return valorTotal.multiply(percentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
