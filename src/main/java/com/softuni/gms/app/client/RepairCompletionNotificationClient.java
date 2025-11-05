package com.softuni.gms.app.client;

import com.softuni.gms.app.web.dto.RepairCompletionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notificationClient", url = "http://localhost:8081/api/v1/whatsapp")
public interface RepairCompletionNotificationClient {

    @PostMapping(value = "/complete-order", consumes = MediaType.APPLICATION_JSON_VALUE)
    void sendMessageForCompletion(@RequestBody RepairCompletionRequest repairCompletionRequest);
}
