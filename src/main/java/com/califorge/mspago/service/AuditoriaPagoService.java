package com.califorge.mspago.service;

import com.califorge.mspago.dto.AlertaFraudeEvent;
import com.califorge.mspago.dto.IntentoPagoEvent;
import com.califorge.mspago.model.AlertaFraude;
import com.califorge.mspago.model.IntentoPago;
import com.califorge.mspago.repository.AlertaFraudeRepository;
import com.califorge.mspago.repository.IntentoPagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Logica de dominio de los consumidores Kafka (los listeners solo delegan aqui). */
@Service
public class AuditoriaPagoService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaPagoService.class);

    private final IntentoPagoRepository intentos;
    private final AlertaFraudeRepository alertas;

    public AuditoriaPagoService(IntentoPagoRepository intentos, AlertaFraudeRepository alertas) {
        this.intentos = intentos;
        this.alertas = alertas;
    }

    /** Persiste el intento (idempotente por idTransaccion: un reintento de Kafka no duplica). */
    @Transactional
    public void registrarIntento(IntentoPagoEvent e) {
        validar(e == null || e.idTransaccion() == null || e.tarjetaHash() == null || e.timestamp() == null,
                "IntentoPagoEvent invalido (campos obligatorios ausentes)");
        if (intentos.existsByIdTransaccion(e.idTransaccion())) {
            log.debug("Intento {} ya registrado, se ignora", e.idTransaccion());
            return;
        }
        IntentoPago i = new IntentoPago();
        i.setIdTransaccion(e.idTransaccion());
        i.setTarjetaHash(e.tarjetaHash());
        i.setClienteSub(e.clienteSub());
        i.setMonto(e.monto());
        i.setBloqueado(e.bloqueado());
        i.setMomento(e.timestamp());
        intentos.save(i);
        log.info("Intento de pago auditado: tx={} bloqueado={}", e.idTransaccion(), e.bloqueado());
    }

    @Transactional
    public void registrarAlerta(AlertaFraudeEvent e) {
        validar(e == null || e.tarjetaHash() == null || e.timestamp() == null,
                "AlertaFraudeEvent invalido (campos obligatorios ausentes)");
        AlertaFraude a = new AlertaFraude();
        a.setTarjetaHash(e.tarjetaHash());
        a.setTarjetaUltimos4(e.tarjetaUltimos4());
        a.setClienteSub(e.clienteSub());
        a.setIntentos(e.intentos());
        a.setVentanaSegundos(e.ventanaSegundos());
        a.setBloqueadoHasta(e.bloqueadoHasta());
        a.setCreada(e.timestamp());
        alertas.save(a);
        log.warn("ALERTA DE FRAUDE registrada: tarjeta ****{} intentos={} en {}s, bloqueada hasta {}",
                e.tarjetaUltimos4(), e.intentos(), e.ventanaSegundos(), e.bloqueadoHasta());
    }

    public List<AlertaFraude> ultimasAlertas() {
        return alertas.findTop50ByOrderByCreadaDesc();
    }

    private static void validar(boolean invalido, String mensaje) {
        if (invalido) {
            throw new IllegalArgumentException(mensaje);
        }
    }
}
