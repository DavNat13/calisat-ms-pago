package com.califorge.mspago.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Simulacion interna del pago (sin Transbank): autoriza siempre. Activa con WEBPAY_ENABLED=false. */
@Component
@ConditionalOnProperty(name = "calisat.pago.webpay.enabled", havingValue = "false")
public class SimulatedWebpayGateway implements WebpayGateway {

    private final String returnUrl;

    public SimulatedWebpayGateway(@Value("${calisat.pago.webpay.return-url}") String returnUrl) {
        this.returnUrl = returnUrl;
    }

    @Override
    public TransaccionCreada crear(String buyOrder, String sessionId, double monto, String retorno) {
        String token = "SIM-" + UUID.randomUUID();
        return new TransaccionCreada(token, (retorno != null ? retorno : returnUrl) + "?token_ws=" + token);
    }

    @Override
    public ResultadoCommit confirmar(String token) {
        return new ResultadoCommit(true, "SIM" + Math.abs(token.hashCode() % 1_000_000));
    }

    @Override
    public boolean esSimulado() {
        return true;
    }
}
