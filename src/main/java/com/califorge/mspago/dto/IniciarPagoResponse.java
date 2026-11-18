package com.califorge.mspago.dto;

/** Resultado del inicio de pago: URL y token de WebPay (o de la simulacion). */
public record IniciarPagoResponse(Long pagoId, String token, String url, boolean simulado) {
}
