package com.califorge.mspago.service;

public class PagoGatewayException extends RuntimeException {
    public PagoGatewayException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
