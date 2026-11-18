package com.califorge.mspago.dto;

import java.time.Instant;

/**
 * Evento publicado en {@code payment-attempts} por cada intento de pago.
 * Key del mensaje: {@code tarjetaHash}, de modo que todos los intentos de una
 * misma tarjeta caen en la misma particion y conservan su orden cronologico.
 * Nunca se transporta el numero de tarjeta, solo su huella (SHA-256) y los 4 ultimos digitos.
 */
public record IntentoPagoEvent(
        String idTransaccion,
        String tarjetaHash,
        String tarjetaUltimos4,
        String clienteSub,
        String ordenId,
        Long monto,
        boolean bloqueado,
        Instant timestamp) {
}
