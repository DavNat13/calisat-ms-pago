package com.califorge.mspago.dto;

import jakarta.validation.constraints.NotBlank;

/** Confirmacion (commit) del pago con el token devuelto por WebPay al retornar al sitio. */
public record ConfirmarPagoRequest(@NotBlank String token) {
}
