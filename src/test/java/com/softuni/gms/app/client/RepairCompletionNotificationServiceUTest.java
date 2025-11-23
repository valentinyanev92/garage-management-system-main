package com.softuni.gms.app.client;

import com.softuni.gms.app.exeption.MicroserviceDontRespondException;
import com.softuni.gms.app.web.dto.RepairCompletionRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class RepairCompletionNotificationServiceUTest {

    @Mock
    private RepairCompletionNotificationClient notificationClient;

    @InjectMocks
    private RepairCompletionNotificationService service;

    @Test
    void testSendMessageForCompletion_success() {

        RepairCompletionRequest request = new RepairCompletionRequest(
                "BMW",
                "E46",
                "Gosho",
                "Mechanikov",
                "0888123456"
        );

        service.sendMessageForCompletion(request);

        Mockito.verify(notificationClient, Mockito.times(1))
                .sendMessageForCompletion(request);
    }

    @Test
    void testSendMessageForCompletion_clientThrowsException_shouldWrapInCustomException() {

        RepairCompletionRequest request = new RepairCompletionRequest(
                "Audi",
                "A4",
                "Ivan",
                "Petrov",
                "0888999999"
        );

        Mockito.doThrow(new RuntimeException("Client DOWN"))
                .when(notificationClient)
                .sendMessageForCompletion(request);

        Assertions.assertThrows(MicroserviceDontRespondException.class,
                () -> service.sendMessageForCompletion(request));
    }
}
