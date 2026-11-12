package br.com.barbearia.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Porta de saída para o provedor de pagamentos (Mercado Pago, simulador, etc.). */
public interface GatewayPagamento {

    CobrancaPix criarCobrancaPix(NovaCobranca cobranca);

    PagamentoConsultado consultar(String pagamentoId);

    record NovaCobranca(String referencia, BigDecimal valor, String descricao,
                        String pagadorNome, String pagadorEmail, OffsetDateTime expiraEm) {
    }

    record CobrancaPix(String pagamentoId, String copiaECola, String qrCodeBase64, String url) {
    }

    record PagamentoConsultado(String pagamentoId, String referencia, BigDecimal valor, StatusPagamento status) {
    }

    enum StatusPagamento { PENDENTE, APROVADO, RECUSADO_OU_CANCELADO, ESTORNADO }
}
