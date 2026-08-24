package com.aieoms.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "kafka.enabled",
        havingValue = "true"
)
public class KafkaTopicConfig {

    public static final String INCIDENT_TOPIC =
            "incident-events";

    @Bean
    public NewTopic incidentTopic() {

        return new NewTopic(
                INCIDENT_TOPIC,
                1,
                (short) 1
        );
    }
}