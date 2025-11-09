package com.softuni.gms.app.client;

import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.web.dto.RepairCompletionRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.softuni.gms.app.exeption.MicroserviceDontRespondExceptionMessages.NOTIFICATION_SERVICE_UNAVAILABLE;

@Slf4j
@Service
public class RepairCompletionNotificationService {

    private final RepairCompletionNotificationClient repairCompletionNotificationClient;

    @Autowired
    public RepairCompletionNotificationService(RepairCompletionNotificationClient repairCompletionNotificationClient) {
        this.repairCompletionNotificationClient = repairCompletionNotificationClient;
    }

    public void sendMessageForCompletion(RepairCompletionRequest repairCompletionRequest) {

        log.info("RepairCompletionNotificationService send message for completion to {}", repairCompletionRequest.getPhoneNumber());
        try {
            repairCompletionNotificationClient.sendMessageForCompletion(repairCompletionRequest);
        } catch (Exception ex) {
            log.error("Failed to notify completion for phone {}: {}", repairCompletionRequest.getPhoneNumber(), ex.getMessage());
            throw new MicroserviceDontRespondException(NOTIFICATION_SERVICE_UNAVAILABLE, ex);
        }
    }
}
