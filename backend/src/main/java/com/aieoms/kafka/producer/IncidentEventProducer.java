
package com.aieoms.kafka.producer;

import com.aieoms.kafka.config.KafkaTopicConfig;
import com.aieoms.kafka.event.IncidentEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(
        name = "kafka.enabled",
        havingValue = "true"
)
public class IncidentEventProducer {

    private static final Logger log =
            LoggerFactory.getLogger(IncidentEventProducer.class);

    private final KafkaTemplate<String, IncidentEvent> kafkaTemplate;

    public IncidentEventProducer(
            KafkaTemplate<String, IncidentEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(IncidentEvent event) {

        if (event == null) {
            log.warn("Kafka event is null. Event was not published.");
            return;
        }

        if (event.getIncidentId() == null) {
            log.warn("Incident event has no incident ID. Event was not published.");
            return;
        }

        kafkaTemplate.send(
                KafkaTopicConfig.INCIDENT_TOPIC,
                String.valueOf(event.getIncidentId()),
                event
        ).whenComplete((result, exception) -> {

            if (exception != null) {

                log.error(
                        "Failed to publish incident event. incidentId={}",
                        event.getIncidentId(),
                        exception
                );

                return;
            }

            if (result != null) {

                log.debug(
                        "Incident event published successfully. incidentId={}, topic={}, partition={}, offset={}",
                        event.getIncidentId(),
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                );
            }
        });
    }
}
