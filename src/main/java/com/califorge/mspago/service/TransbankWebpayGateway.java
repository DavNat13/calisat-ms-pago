package com.califorge.mspago.service;

import cl.transbank.common.IntegrationApiKeys;
import cl.transbank.webpay.webpayplus.WebpayPlus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** WebPay Plus de Transbank en ambiente de INTEGRACION (credenciales publicas de prueba). */
@Component
@ConditionalOnProperty(name = "calisat.pago.webpay.enabled", havingValue = "true", matchIfMissing = true)
public class TransbankWebpayGateway implements WebpayGateway {

    private static final Logger log = LoggerFactory.getLogger(TransbankWebpayGateway.class);

    private final WebpayPlus.Transaction tx;

    public TransbankWebpayGateway(@Value("${calisat.pago.webpay.commerce-code}") String commerceCode) {
        this.tx = WebpayPlus.Transaction.buildForIntegration(commerceCode, IntegrationApiKeys.WEBPAY);
    }

    @Override
    public TransaccionCreada crear(String buyOrder, String sessionId, double monto, String returnUrl) {
        try {
            var resp = tx.create(buyOrder, sessionId, monto, returnUrl);
            log.info("Transaccion WebPay creada buyOrder={}", buyOrder);
            return new TransaccionCreada(resp.getToken(), resp.getUrl());
        } catch (Exception e) {
            log.error("Error al crear transaccion en Transbank: {}", e.getMessage());
            throw new PagoGatewayException("No se pudo iniciar el pago en WebPay", e);
        }
    }

    @Override
    public ResultadoCommit confirmar(String token) {
        try {
            var resp = tx.commit(token);
            boolean ok = "AUTHORIZED".equalsIgnoreCase(resp.getStatus()) && resp.getResponseCode() == 0;
            return new ResultadoCommit(ok, resp.getAuthorizationCode());
        } catch (Exception e) {
            log.error("Error al confirmar transaccion en Transbank: {}", e.getMessage());
            throw new PagoGatewayException("No se pudo confirmar el pago en WebPay", e);
        }
    }

    @Override
    public boolean esSimulado() {
        return false;
    }
}
