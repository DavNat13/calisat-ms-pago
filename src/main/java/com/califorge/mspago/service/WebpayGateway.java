package com.califorge.mspago.service;

/** Abstraccion de la pasarela WebPay (Transbank real o simulacion interna). */
public interface WebpayGateway {

    record TransaccionCreada(String token, String url) {
    }

    record ResultadoCommit(boolean autorizado, String codigoAutorizacion) {
    }

    TransaccionCreada crear(String buyOrder, String sessionId, double monto, String returnUrl);

    ResultadoCommit confirmar(String token);

    boolean esSimulado();
}
