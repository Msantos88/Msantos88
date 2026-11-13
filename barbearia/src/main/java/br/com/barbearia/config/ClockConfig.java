package br.com.barbearia.config;

import br.com.barbearia.application.BarbeariaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
class ClockConfig {

    @Bean
    Clock clock(BarbeariaProperties props) {
        return Clock.system(props.fusoHorario());
    }
}
