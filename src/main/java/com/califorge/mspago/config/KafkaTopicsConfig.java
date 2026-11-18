package com.califorge.mspago.config;

import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.apache.kafka.clients.admin.NewTopic;

/**
 * Definicion explicita de topicos (particiones, replicas, retencion y cleanup policy).
 * KafkaAdmin los crea al arrancar si no existen (auto.create.topics.enable=false en el broker).
 */
@Configuration
public class KafkaTopicsConfig {

    private static final String UN_DIA_MS = "86400000";
    private static final String SIETE_DIAS_MS = "604800000";

    @Bean
    public NewTopic topicIntentosPago(@Value("${calisat.pago.kafka.topic-intentos}") String nombre) {
        return TopicBuilder.name(nombre)
                .partitions(3).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, UN_DIA_MS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    @Bean
    public NewTopic topicAlertasPago(@Value("${calisat.pago.kafka.topic-alertas}") String nombre) {
        return TopicBuilder.name(nombre)
                .partitions(1).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, SIETE_DIAS_MS)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT + ","
                        + TopicConfig.CLEANUP_POLICY_DELETE)
                .build();
    }

    /** Dead-letter topics: reciben los registros que agotan reintentos en los consumidores. */
    @Bean
    public NewTopic topicIntentosPagoDlt(@Value("${calisat.pago.kafka.topic-intentos}") String nombre) {
        return TopicBuilder.name(nombre + ".DLT").partitions(3).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, SIETE_DIAS_MS).build();
    }

    @Bean
    public NewTopic topicAlertasPagoDlt(@Value("${calisat.pago.kafka.topic-alertas}") String nombre) {
        return TopicBuilder.name(nombre + ".DLT").partitions(1).replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, SIETE_DIAS_MS).build();
    }
}
