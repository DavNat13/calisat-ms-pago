package com.califorge.mspago.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Solicitud de inicio de pago. {@code tarjeta} es el identificador de la tarjeta
 * usado por el antifraude (numero o huella); nunca se persiste en claro.
 */
public record IniciarPagoRequest(
        @NotBlank String ordenId,
        @NotNull @Positive Long monto,
        @NotBlank String tarjeta) {
}
