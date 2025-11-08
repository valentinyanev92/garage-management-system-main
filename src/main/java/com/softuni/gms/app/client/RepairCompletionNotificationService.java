package com.softuni.gms.app.client;

import com.softuni.gms.app.web.dto.RepairCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RepairCompletionNotificationService {

    private final RepairCompletionNotificationClient repairCompletionNotificationClient;

    @Autowired
    public RepairCompletionNotificationService(RepairCompletionNotificationClient repairCompletionNotificationClient) {
        this.repairCompletionNotificationClient = repairCompletionNotificationClient;
    }

    public void sendMessageForCompletion(RepairCompletionRequest  repairCompletionRequest) {

        log.info("RepairCompletionNotificationService send message for completion to {}", repairCompletionRequest.getPhoneNumber());
        repairCompletionNotificationClient.sendMessageForCompletion(repairCompletionRequest);
    }
}
