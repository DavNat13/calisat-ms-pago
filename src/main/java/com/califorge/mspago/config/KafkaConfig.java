package com.califorge.mspago.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuracion centralizada del cliente Kafka: bootstrap, serializers/deserializers tipados,
 * trusted packages, y manejo de errores (reintentos con backoff + dead-letter topic).
 */
@Configuration
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    /** Alias de tipo en la cabecera __TypeId__: desacopla productor/consumidor del nombre de clase. */
    private static final String TYPE_MAPPINGS =
            "intentoPago:com.califorge.mspago.dto.IntentoPagoEvent,"
            + "alertaFraude:com.califorge.mspago.dto.AlertaFraudeEvent";

    @Bean
    public ProducerFactory<String, Object> producerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap) {
        Map<String, Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        p.put(JsonSerializer.TYPE_MAPPINGS, TYPE_MAPPINGS);
        return new DefaultKafkaProducerFactory<>(p);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> pf) {
        return new KafkaTemplate<>(pf);
    }

    /** Plantilla para republicar en DLT los registros no deserializables (bytes originales). */
    @Bean
    public KafkaTemplate<String, byte[]> bytesKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap) {
        Map<String, Object> p = new HashMap<>();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(p));
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrap,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String offsetReset) {
        Map<String, Object> p = new HashMap<>();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrap);
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, offsetReset);
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        p.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        p.put(JsonDeserializer.TRUSTED_PACKAGES, "com.califorge.mspago.dto");
        p.put(JsonDeserializer.TYPE_MAPPINGS, TYPE_MAPPINGS);
        return new DefaultKafkaConsumerFactory<>(p);
    }

    /**
     * Errores de procesamiento: 3 reintentos con backoff exponencial y luego dead-letter topic
     * ({@code <topico>.DLT}). Los payloads corruptos (DeserializationException) no se reintentan.
     */
    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> jsonTemplate,
                                                 KafkaTemplate<String, byte[]> bytesTemplate) {
        Map<Class<?>, KafkaOperations<? extends Object, ? extends Object>> templates = new java.util.LinkedHashMap<>();
        templates.put(byte[].class, bytesTemplate);
        templates.put(Object.class, jsonTemplate);
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(templates,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(
                        record.topic() + ".DLT", record.partition()));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(3);
        backOff.setInitialInterval(500);
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(4000);
        DefaultErrorHandler handler = new DefaultErrorHandler((rec, ex) -> {
            log.error("[DLT] Registro descartado tras reintentos: topic={} partition={} offset={} key={} causa={}",
                    rec.topic(), rec.partition(), rec.offset(), rec.key(), ex.getMessage());
            recoverer.accept(rec, ex);
        }, backOff);
        handler.addNotRetryableExceptions(org.springframework.kafka.support.serializer.DeserializationException.class,
                IllegalArgumentException.class);
        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> cf, DefaultErrorHandler errorHandler) {
        ConcurrentKafkaListenerContainerFactory<String, Object> f = new ConcurrentKafkaListenerContainerFactory<>();
        f.setConsumerFactory(cf);
        f.setCommonErrorHandler(errorHandler);
        f.setConcurrency(3);
        return f;
    }
}
