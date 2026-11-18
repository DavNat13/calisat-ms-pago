package com.califorge.mspago.listener;

import com.califorge.mspago.dto.AlertaFraudeEvent;
import com.califorge.mspago.dto.IntentoPagoEvent;
import com.califorge.mspago.service.AuditoriaPagoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumidores Kafka de ms-pago. Dos consumer groups distintos con responsabilidades separadas:
 * auditoria de intentos (payment-audit-group) y registro de alertas de fraude (payment-alerts-group).
 * Los listeners solo reciben, registran contexto y delegan a la capa de servicio.
 */
@Component
public class PagoKafkaListeners {

    private static final Logger log = LoggerFactory.getLogger(PagoKafkaListeners.class);

    private final AuditoriaPagoService servicio;

    public PagoKafkaListeners(AuditoriaPagoService servicio) {
        this.servicio = servicio;
    }

    @KafkaListener(topics = "${calisat.pago.kafka.topic-intentos}",
            groupId = "${calisat.pago.kafka.group-auditoria}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onIntento(@Payload(required = false) IntentoPagoEvent evento,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset,
                          @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key) {
        log.debug("payment-attempts recibido p={} o={} key={}", partition, offset, key);
        servicio.registrarIntento(evento);
    }

    @KafkaListener(topics = "${calisat.pago.kafka.topic-alertas}",
            groupId = "${calisat.pago.kafka.group-alertas}",
            containerFactory = "kafkaListenerContainerFactory")
    public void onAlerta(@Payload(required = false) AlertaFraudeEvent evento,
                         @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                         @Header(KafkaHeaders.OFFSET) long offset) {
        log.debug("payment-alerts recibido p={} o={}", partition, offset);
        servicio.registrarAlerta(evento);
    }
}
