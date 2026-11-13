package br.com.barbearia.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Set;

@ConfigurationProperties(prefix = "barbearia")
public record BarbeariaProperties(
        String nome,
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
