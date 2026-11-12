package br.com.barbearia.adapters.payment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoAssinaturaTest {

    @Test
    void aceitaAssinaturaCorretaERejeitaAdulterada() {
        String segredo = "segredo";
        String v1 = MercadoPagoAssinatura.hmac(segredo, "id:123;request-id:req-1;ts:1700000000;");
        String header = "ts=1700000000,v1=" + v1;

        assertThat(MercadoPagoAssinatura.valida(segredo, header, "req-1", "123")).isTrue();
        assertThat(MercadoPagoAssinatura.valida(segredo, header, "req-1", "999")).isFalse();
        assertThat(MercadoPagoAssinatura.valida("outro", header, "req-1", "123")).isFalse();
        assertThat(MercadoPagoAssinatura.valida(segredo, null, "req-1", "123")).isFalse();
    }
}
