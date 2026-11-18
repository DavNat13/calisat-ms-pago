package com.califorge.mspago.service;

import java.time.Instant;

/** La tarjeta esta bloqueada por el antifraude (se traduce en HTTP 429). */
public class TarjetaBloqueadaException extends RuntimeException {

    private final Instant bloqueadoHasta;

    public TarjetaBloqueadaException(Instant bloqueadoHasta) {
        super("Tarjeta bloqueada preventivamente por intentos repetidos. Reintente mas tarde.");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
