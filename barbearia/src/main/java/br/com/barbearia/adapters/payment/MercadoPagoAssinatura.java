package br.com.barbearia.adapters.payment;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Validação do cabeçalho x-signature dos webhooks do Mercado Pago.
 * Manifesto: "id:{data.id};request-id:{x-request-id};ts:{ts};" assinado com HMAC-SHA256.
 */
public final class MercadoPagoAssinatura {

    private MercadoPagoAssinatura() {
    }

    public static boolean valida(String segredo, String xSignature, String xRequestId, String dataId) {
        if (xSignature == null || dataId == null) {
            return false;
        }
        String ts = null;
        String v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals("ts")) ts = kv[1];
            if (kv.length == 2 && kv[0].equals("v1")) v1 = kv[1];
        }
        if (ts == null || v1 == null) {
            return false;
        }
        StringBuilder manifesto = new StringBuilder("id:").append(dataId.toLowerCase()).append(';');
        if (xRequestId != null) {
            manifesto.append("request-id:").append(xRequestId).append(';');
        }
        manifesto.append("ts:").append(ts).append(';');
        return MessageDigest.isEqual(
                hmac(segredo, manifesto.toString()).getBytes(StandardCharsets.UTF_8),
                v1.getBytes(StandardCharsets.UTF_8));
    }

    static String hmac(String segredo, String mensagem) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(segredo.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(mensagem.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
