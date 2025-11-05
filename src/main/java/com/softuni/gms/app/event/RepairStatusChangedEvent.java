package com.softuni.gms.app.event;

import com.softuni.gms.app.repair.model.RepairOrder;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RepairStatusChangedEvent extends ApplicationEvent {

    private final RepairOrder repairOrder;
    private final String oldStatus;
    private final String newStatus;

    public RepairStatusChangedEvent(Object source, RepairOrder repairOrder, String oldStatus, String newStatus) {
        super(source);
        this.repairOrder = repairOrder;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }
}
