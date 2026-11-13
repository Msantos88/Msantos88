package br.com.barbearia.adapters.payment;

import br.com.barbearia.application.GatewayPagamento;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gateway em memória para desenvolvimento: gera um "PIX" de mentira e permite
 * aprovar o pagamento pelo botão "Simular pagamento" da tela.
 * NUNCA use em produção.
 */
@Component
@ConditionalOnProperty(name = "pagamento.provedor", havingValue = "simulador", matchIfMissing = true)
public class SimuladorGateway implements GatewayPagamento {

    private final Map<String, PagamentoConsultado> pagamentos = new ConcurrentHashMap<>();

    @Override
    public CobrancaPix criarCobrancaPix(NovaCobranca c) {
        String id = "SIM-" + UUID.randomUUID();
        pagamentos.put(id, new PagamentoConsultado(id, c.referencia(), c.valor(), StatusPagamento.PENDENTE));
        String copiaECola = "00020126SIMULADO-" + id + "5204000053039865406" + c.valor() + "5802BR6304ABCD";
        return new CobrancaPix(id, copiaECola, null, null);
    }

    @Override
    public PagamentoConsultado consultar(String pagamentoId) {
        PagamentoConsultado p = pagamentos.get(pagamentoId);
        if (p == null) {
            throw new IllegalArgumentException("Pagamento simulado inexistente: " + pagamentoId);
        }
        return p;
    }

    public void aprovar(String pagamentoId) {
        pagamentos.computeIfPresent(pagamentoId, (id, p) ->
                new PagamentoConsultado(id, p.referencia(), p.valor(), StatusPagamento.APROVADO));
    }
}
