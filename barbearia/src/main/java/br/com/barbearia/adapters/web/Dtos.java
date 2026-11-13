package br.com.barbearia.adapters.web;

import br.com.barbearia.domain.Agendamento;
import br.com.barbearia.domain.Barbeiro;
import br.com.barbearia.domain.Servico;
import br.com.barbearia.domain.StatusAgendamento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

final class Dtos {

    private Dtos() {
    }

    record ServicoDto(Long id, String nome, String descricao, BigDecimal preco, BigDecimal sinal, int duracaoMinutos) {
    }

    record BarbeiroDto(Long id, String nome) {
        static BarbeiroDto de(Barbeiro b) {
            return new BarbeiroDto(b.getId(), b.getNome());
        }
    }

    record NovoAgendamentoRequest(
            @NotNull Long servicoId,
            @NotNull Long barbeiroId,
            @NotNull LocalDateTime inicio,
            @NotBlank @Size(max = 100) String clienteNome,
            @NotBlank @Pattern(regexp = "^[0-9()+\\-\\s]{10,20}$", message = "telefone inválido") String clienteTelefone,
            @NotBlank @Email @Size(max = 150) String clienteEmail) {
    }

    record PixDto(String copiaECola, String qrCodeBase64, String url) {
    }

    /** Visão do cliente: sem dados de outros clientes. */
    record AgendamentoDto(UUID id, StatusAgendamento status, String servico, String barbeiro,
                          LocalDateTime inicio, LocalDateTime fim, String clienteNome,
                          BigDecimal valorTotal, BigDecimal valorSinal, BigDecimal valorRestante,
                          java.time.OffsetDateTime pagarAte, PixDto pix) {
        static AgendamentoDto de(Agendamento a, java.time.ZoneId fuso) {
            PixDto pix = a.getStatus() == StatusAgendamento.AGUARDANDO_PAGAMENTO
                    ? new PixDto(a.getPixCopiaECola(), a.getPixQrCodeBase64(), a.getPixUrl())
                    : null;
            return new AgendamentoDto(a.getId(), a.getStatus(), a.getServico().getNome(), a.getBarbeiro().getNome(),
                    a.getInicio(), a.getFim(), a.getClienteNome(), a.getValorTotal(), a.getValorSinal(),
                    a.getValorRestante(), a.getExpiraEm().atZone(fuso).toOffsetDateTime(), pix);
        }
    }

    record AgendamentoAdminDto(UUID id, StatusAgendamento status, String servico, String barbeiro,
                               LocalDateTime inicio, LocalDateTime fim, String clienteNome,
                               String clienteTelefone, String clienteEmail, BigDecimal valorTotal,
                               BigDecimal valorSinal, BigDecimal valorRestante, String pagamentoId,
                               LocalDateTime pagoEm) {
        static AgendamentoAdminDto de(Agendamento a) {
            return new AgendamentoAdminDto(a.getId(), a.getStatus(), a.getServico().getNome(),
                    a.getBarbeiro().getNome(), a.getInicio(), a.getFim(), a.getClienteNome(),
                    a.getClienteTelefone(), a.getClienteEmail(), a.getValorTotal(), a.getValorSinal(),
                    a.getValorRestante(), a.getPagamentoId(), a.getPagoEm());
        }
    }

    record InfoDto(String nome, String slogan, String whatsapp, String instagram, String endereco,
                   String bairroCidade, String horarioAtendimento, int percentualSinal, int minutosParaPagarSinal, int diasMaximosParaAgendar,
                   java.util.Set<java.time.DayOfWeek> diasDeFuncionamento, boolean pagamentoSimulado) {
    }

    record ErroDto(String mensagem) {
    }

    static ServicoDto servico(Servico s, BigDecimal sinal) {
        return new ServicoDto(s.getId(), s.getNome(), s.getDescricao(), s.getPreco(), sinal, s.getDuracaoMinutos());
    }
}
