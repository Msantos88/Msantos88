package br.com.barbearia.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

@ConfigurationProperties(prefix = "barbearia")
public record BarbeariaProperties(
        String nome,
        /** Linha abaixo do nome, ex.: "Barbearia · Desde 2010". */
        String slogan,
        /** Número com DDD, só dígitos, ex.: 11912345678 (vazio esconde o botão). */
        String whatsapp,
        /** Usuário do Instagram sem @ (vazio esconde o link). */
        String instagram,
        String endereco,
        String bairroCidade,
        /** Texto livre de horário de atendimento, ex.: "Ter a Sáb · 9h às 19h". */
        String horarioAtendimento,
        ZoneId fusoHorario,
        int percentualSinal,
        int minutosParaPagarSinal,
        int intervaloEntreHorariosMinutos,
        int antecedenciaMinimaMinutos,
        int diasMaximosParaAgendar,
        LocalTime abertura,
        LocalTime fechamento,
        Set<DayOfWeek> diasDeFuncionamento) {
}
