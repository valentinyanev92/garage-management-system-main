package com.softuni.gms.app.event;

import com.softuni.gms.app.kafka.KafkaProducerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static com.softuni.gms.app.web.mapper.DtoMapper.mapRepairOrderForKafka;

@Component
public class RepairEventListener {

    private final KafkaProducerService kafkaProducer;

    @Autowired
    public RepairEventListener(KafkaProducerService kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @EventListener
    public void handleStatusChange(RepairStatusChangedEvent event) {

        kafkaProducer.sendEvent(mapRepairOrderForKafka(event));
    }
}
