package br.com.barbearia.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "agendamento")
public class Agendamento {

    /** UUID para que o link do agendamento não seja adivinhável. */
    @Id
    private UUID id;

    @ManyToOne(optional = false)
    private Servico servico;

    @ManyToOne(optional = false)
    private Barbeiro barbeiro;

    private String clienteNome;
    private String clienteTelefone;
    private String clienteEmail;

    private LocalDateTime inicio;
    private LocalDateTime fim;

    private BigDecimal valorTotal;
    private BigDecimal valorSinal;

    @Enumerated(EnumType.STRING)
    private StatusAgendamento status;

    /** Até quando o sinal pode ser pago antes de o horário ser liberado. */
    private LocalDateTime expiraEm;

    private String pagamentoId;
    @Column(name = "pix_copia_e_cola", length = 2000)
    private String pixCopiaECola;
    @Column(columnDefinition = "TEXT")
    private String pixQrCodeBase64;
    @Column(length = 1000)
    private String pixUrl;

    private LocalDateTime criadoEm;
    private LocalDateTime pagoEm;

    @Version
    private long versao;

    protected Agendamento() {
    }

    public Agendamento(Servico servico, Barbeiro barbeiro, String clienteNome, String clienteTelefone,
                       String clienteEmail, LocalDateTime inicio, BigDecimal valorSinal,
                       LocalDateTime agora, LocalDateTime expiraEm) {
        this.id = UUID.randomUUID();
        this.servico = servico;
        this.barbeiro = barbeiro;
        this.clienteNome = clienteNome;
        this.clienteTelefone = clienteTelefone;
        this.clienteEmail = clienteEmail;
        this.inicio = inicio;
        this.fim = inicio.plusMinutes(servico.getDuracaoMinutos());
        this.valorTotal = servico.getPreco();
        this.valorSinal = valorSinal;
        this.status = StatusAgendamento.AGUARDANDO_PAGAMENTO;
        this.criadoEm = agora;
        this.expiraEm = expiraEm;
    }

    public void registrarCobranca(String pagamentoId, String pixCopiaECola, String pixQrCodeBase64, String pixUrl) {
        this.pagamentoId = pagamentoId;
        this.pixCopiaECola = pixCopiaECola;
        this.pixQrCodeBase64 = pixQrCodeBase64;
        this.pixUrl = pixUrl;
    }

    public void confirmarPagamento(LocalDateTime agora) {
        this.status = StatusAgendamento.CONFIRMADO;
        this.pagoEm = agora;
    }

    public void marcarReembolsoPendente(LocalDateTime agora) {
        this.status = StatusAgendamento.REEMBOLSO_PENDENTE;
        this.pagoEm = agora;
    }

    public void expirar() {
        if (status != StatusAgendamento.AGUARDANDO_PAGAMENTO) {
            throw new IllegalStateException("Só é possível expirar agendamentos aguardando pagamento");
        }
        this.status = StatusAgendamento.EXPIRADO;
    }

    public void cancelar() {
        if (status == StatusAgendamento.CONCLUIDO) {
            throw new IllegalStateException("Agendamento já concluído não pode ser cancelado");
        }
        this.status = StatusAgendamento.CANCELADO;
    }

    public void concluir() {
        if (status != StatusAgendamento.CONFIRMADO) {
            throw new IllegalStateException("Só é possível concluir agendamentos confirmados");
        }
        this.status = StatusAgendamento.CONCLUIDO;
    }

    public boolean isPendenteExpirado(LocalDateTime agora) {
        return status == StatusAgendamento.AGUARDANDO_PAGAMENTO && !agora.isBefore(expiraEm);
    }

    public BigDecimal getValorRestante() {
        return valorTotal.subtract(valorSinal);
    }

    public UUID getId() { return id; }
    public Servico getServico() { return servico; }
    public Barbeiro getBarbeiro() { return barbeiro; }
    public String getClienteNome() { return clienteNome; }
    public String getClienteTelefone() { return clienteTelefone; }
    public String getClienteEmail() { return clienteEmail; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFim() { return fim; }
    public BigDecimal getValorTotal() { return valorTotal; }
    public BigDecimal getValorSinal() { return valorSinal; }
    public StatusAgendamento getStatus() { return status; }
    public LocalDateTime getExpiraEm() { return expiraEm; }
    public String getPagamentoId() { return pagamentoId; }
    public String getPixCopiaECola() { return pixCopiaECola; }
    public String getPixQrCodeBase64() { return pixQrCodeBase64; }
    public String getPixUrl() { return pixUrl; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getPagoEm() { return pagoEm; }
}
