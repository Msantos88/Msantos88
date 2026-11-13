package br.com.barbearia.application;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class TarefasAgendadas {

    private final AgendamentoService service;

    TarefasAgendadas(AgendamentoService service) {
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${barbearia.verificacao-pendentes-ms:60000}", initialDelay = 30000)
    void verificarPendentes() {
        service.verificarPendentes();
    }
}
