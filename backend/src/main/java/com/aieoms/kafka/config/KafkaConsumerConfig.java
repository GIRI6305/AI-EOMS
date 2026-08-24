package com.aieoms.kafka.config;

import com.aieoms.kafka.event.IncidentEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConsumerFactory<String, IncidentEvent> incidentEventConsumerFactory() {
        Map<String, Object> properties = new HashMap<>();

        properties.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        );

        properties.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                System.getenv().getOrDefault("KAFKA_CONSUMER_GROUP", "ai-eoms-group")
        );

        properties.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                System.getenv().getOrDefault("KAFKA_AUTO_OFFSET_RESET", "earliest")
        );

        JsonDeserializer<IncidentEvent> valueDeserializer =
                new JsonDeserializer<>(IncidentEvent.class);

        valueDeserializer.addTrustedPackages("com.aieoms.kafka.event");

        return new DefaultKafkaConsumerFactory<>(
                properties,
                new StringDeserializer(),
                valueDeserializer
        );
    }

    @Bean(name = "kafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, IncidentEvent>
    kafkaListenerContainerFactory() {

        ConcurrentKafkaListenerContainerFactory<String, IncidentEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(incidentEventConsumerFactory());

        return factory;
    }
}
