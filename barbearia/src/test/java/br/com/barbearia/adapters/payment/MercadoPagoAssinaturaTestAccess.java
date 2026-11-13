package br.com.barbearia.adapters.payment;

/** Expõe o HMAC (package-private) para os testes de outros pacotes. */
public final class MercadoPagoAssinaturaTestAccess {
    private MercadoPagoAssinaturaTestAccess() {
    }

    public static String hmac(String segredo, String mensagem) {
        return MercadoPagoAssinatura.hmac(segredo, mensagem);
    }
}
