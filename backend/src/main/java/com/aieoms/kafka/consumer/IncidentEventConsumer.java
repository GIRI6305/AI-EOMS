package com.aieoms.kafka.consumer;

import com.aieoms.kafka.config.KafkaTopicConfig;
import com.aieoms.kafka.event.IncidentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class IncidentEventConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(IncidentEventConsumer.class);

   @KafkaListener(
    topics = KafkaTopicConfig.INCIDENT_TOPIC,
    groupId = "ai-eoms-group",
    autoStartup = "${KAFKA_ENABLED:false}"
)
    public void consume(IncidentEvent event) {
        log.info(
                "Received incident event: {} - {}",
                event.getIncidentId(),
                event.getTitle()
        );
    }
}