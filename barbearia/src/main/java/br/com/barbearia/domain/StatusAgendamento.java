package br.com.barbearia.domain;

public enum StatusAgendamento {
    /** Horário reservado provisoriamente, aguardando o pagamento do sinal. */
    AGUARDANDO_PAGAMENTO,
    /** Sinal pago: horário garantido. */
    CONFIRMADO,
    /** Atendimento realizado (restante pago na barbearia). */
    CONCLUIDO,
    /** Prazo para pagar o sinal acabou; o horário foi liberado. */
    EXPIRADO,
    /** Cancelado pela barbearia ou pelo cliente. */
    CANCELADO,
    /** Pagamento chegou depois que o horário já tinha sido ocupado por outra pessoa: devolver o sinal. */
    REEMBOLSO_PENDENTE;

    public boolean ocupaHorario() {
        return this == AGUARDANDO_PAGAMENTO || this == CONFIRMADO || this == CONCLUIDO;
    }
}
