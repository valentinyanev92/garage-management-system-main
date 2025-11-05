package com.softuni.gms.app.client;

import com.softuni.gms.app.web.dto.RepairCompletionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RepairCompletionNotificationService {

    private final RepairCompletionNotificationClient repairCompletionNotificationClient;

    @Autowired
    public RepairCompletionNotificationService(RepairCompletionNotificationClient repairCompletionNotificationClient) {
        this.repairCompletionNotificationClient = repairCompletionNotificationClient;
    }

    public void sendMessageForCompletion(RepairCompletionRequest  repairCompletionRequest) {
        repairCompletionNotificationClient.sendMessageForCompletion(repairCompletionRequest);
    }
}
