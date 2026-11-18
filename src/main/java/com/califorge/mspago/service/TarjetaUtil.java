package com.califorge.mspago.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Huella (SHA-256) y ultimos 4 digitos de una tarjeta: el numero nunca se persiste ni se publica. */
public final class TarjetaUtil {

    private TarjetaUtil() {
    }

    public static String hash(String tarjeta) {
        String normalizada = tarjeta == null ? "" : tarjeta.replaceAll("[\\s-]", "");
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(normalizada.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String ultimos4(String tarjeta) {
        String digitos = tarjeta == null ? "" : tarjeta.replaceAll("\\D", "");
        return digitos.length() >= 4 ? digitos.substring(digitos.length() - 4) : digitos;
    }
}
