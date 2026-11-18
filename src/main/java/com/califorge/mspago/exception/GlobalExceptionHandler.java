package com.califorge.mspago.exception;

import com.califorge.mspago.service.PagoGatewayException;
import com.califorge.mspago.service.PagoNoEncontradoException;
import com.califorge.mspago.service.TarjetaBloqueadaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TarjetaBloqueadaException.class)
    public ResponseEntity<Map<String, Object>> bloqueada(TarjetaBloqueadaException e) {
        long segundos = e.getBloqueadoHasta() == null ? 0
                : Math.max(1, Duration.between(Instant.now(), e.getBloqueadoHasta()).toSeconds());
        return ResponseEntity.status(429)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(segundos))
                .body(Map.of("error", e.getMessage(), "codigo", "TARJETA_BLOQUEADA",
                        "reintentarEnSegundos", segundos));
    }

    @ExceptionHandler(PagoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> noEncontrado(PagoNoEncontradoException e) {
        return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(PagoGatewayException.class)
    public ResponseEntity<Map<String, String>> pasarela(PagoGatewayException e) {
        log.error("Fallo de pasarela: {}", e.getMessage());
        return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacion(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Solicitud invalida"));
    }
}
