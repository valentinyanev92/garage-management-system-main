package com.softuni.gms.app.event;

import com.softuni.gms.app.repair.model.RepairOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class RepairEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public RepairEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void publishRepairStatusChanged(RepairOrder order, String oldStatus, String newStatus) {

        eventPublisher.publishEvent(new RepairStatusChangedEvent(this, order, oldStatus, newStatus));
    }
}
