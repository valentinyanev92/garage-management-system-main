package com.softuni.gms.app.kafka;

import com.softuni.gms.app.shared.kafka.dto.RepairKafkaEventRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private static final String TOPIC = "gms-events";

    private final KafkaTemplate<String, RepairKafkaEventRequest> kafkaTemplate;

    public KafkaProducerService(KafkaTemplate<String, RepairKafkaEventRequest> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendEvent(RepairKafkaEventRequest eventRequest) {
        kafkaTemplate.send(TOPIC, eventRequest);
    }
}
