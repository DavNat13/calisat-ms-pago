package com.califorge.mspago;

import com.califorge.mspago.config.KafkaConfig;
import com.califorge.mspago.config.KafkaTopicsConfig;
import com.califorge.mspago.dto.AlertaFraudeEvent;
import com.califorge.mspago.dto.IntentoPagoEvent;
import com.califorge.mspago.listener.PagoKafkaListeners;
import com.califorge.mspago.service.AuditoriaPagoService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

/** Integracion con broker Kafka embebido: productor de PagoService + consumidores + DLT. */
@SpringBootTest(
        classes = {KafkaConfig.class, KafkaTopicsConfig.class, PagoKafkaListeners.class},
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "calisat.pago.kafka.topic-intentos=payment-attempts",
                "calisat.pago.kafka.topic-alertas=payment-alerts",
                "calisat.pago.kafka.group-auditoria=payment-audit-group",
                "calisat.pago.kafka.group-alertas=payment-alerts-group"
        })
@ImportAutoConfiguration(KafkaAutoConfiguration.class)
@EmbeddedKafka(partitions = 3, topics = {"payment-attempts", "payment-attempts.DLT", "payment-alerts",
        "payment-alerts.DLT"})
class KafkaIntegrationTest {

    @MockitoBean
    AuditoriaPagoService servicio;

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;     // el mismo bean que usa PagoService

    @Autowired
    EmbeddedKafkaBroker broker;

    @Test
    void intentoYAlertaSeSerializanConAliasYLosConsumenSusGrupos() {
        kafkaTemplate.send("payment-attempts", "hash-1", new IntentoPagoEvent("tx-1", "hash-1", "4242",
                "user-1", "orden-10", 5000L, false, Instant.now()));
        kafkaTemplate.send("payment-alerts", "hash-1", new AlertaFraudeEvent("hash-1", "4242", "user-1", 4, 10,
                Instant.now().plusSeconds(1800), Instant.now()));

        verify(servicio, timeout(15000)).registrarIntento(any(IntentoPagoEvent.class));
        verify(servicio, timeout(15000)).registrarAlerta(any(AlertaFraudeEvent.class));
    }

    @Test
    void payloadCorruptoVaALaDlt() {
        Map<String, Object> p = new HashMap<>();
        p.put("bootstrap.servers", broker.getBrokersAsString());
        p.put("key.serializer", StringSerializer.class);
        p.put("value.serializer", StringSerializer.class);
        new KafkaTemplate<String, String>(new DefaultKafkaProducerFactory<>(p))
                .send("payment-attempts", "hash-x", "{{no-json");

        Map<String, Object> cfg = KafkaTestUtils.consumerProps(broker, "test-dlt-pago", true);
        cfg.put("key.deserializer", StringDeserializer.class);
        cfg.put("value.deserializer", StringDeserializer.class);
        cfg.put("auto.offset.reset", "earliest");
        try (Consumer<String, String> c = new DefaultKafkaConsumerFactory<String, String>(cfg).createConsumer()) {
            broker.consumeFromAnEmbeddedTopic(c, "payment-attempts.DLT");
            ConsumerRecords<String, String> recs = KafkaTestUtils.getRecords(c, Duration.ofSeconds(20), 1);
            assertTrue(recs.count() >= 1);
            for (ConsumerRecord<String, String> r : recs) {
                assertEquals("hash-x", r.key());
                assertTrue(r.value().contains("no-json"));
            }
        }
    }

    /** Evita ambiguedad: PagoService usa el KafkaTemplate<String,Object> de KafkaConfig. */
    @org.springframework.context.annotation.Configuration
    static class ClockConfig {
        @org.springframework.context.annotation.Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }
}
