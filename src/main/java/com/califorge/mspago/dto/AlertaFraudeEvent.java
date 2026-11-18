package com.califorge.mspago.dto;

import java.time.Instant;

/** Evento publicado en {@code payment-alerts} cuando el antifraude bloquea una tarjeta. */
public record AlertaFraudeEvent(
        String tarjetaHash,
        String tarjetaUltimos4,
        String clienteSub,
        int intentos,
        int ventanaSegundos,
        Instant bloqueadoHasta,
        Instant timestamp) {
}
