package com.califorge.mspago.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    /** Reloj inyectable: permite probar la ventana del antifraude sin esperas reales. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
