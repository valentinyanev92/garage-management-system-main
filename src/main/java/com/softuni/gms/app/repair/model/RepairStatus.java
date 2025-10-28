package com.softuni.gms.app.repair.model;

import lombok.Getter;

@Getter
public enum RepairStatus {
    PENDING("Pending"),
    ACCEPTED("Accepted"),
    COMPLETED("Completed"),
    CANCELED("Canceled");

    private final String displayName;

    RepairStatus(final String displayName) {
        this.displayName = displayName;
    }

}
