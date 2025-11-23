package com.softuni.gms.app.ai;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class AiMechanicServiceUTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private AiMechanicService service;

    @Test
    void testAskMechanic_success() {

        AssistantMessage msg = new AssistantMessage("Faulty spark plug");
        Generation gen = new Generation(msg);
        ChatResponse resp = new ChatResponse(List.of(gen));

        Mockito.when(chatModel.call(Mockito.any(Prompt.class)))
                .thenReturn(resp);

        String result = service.askMechanic("Engine knock");

        Assertions.assertEquals("Faulty spark plug", result);
    }

    @Test
    void testAskMechanic_nullQuestion_shouldThrow() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.askMechanic(null));
    }

    @Test
    void testAskMechanic_emptyQuestion_shouldThrow() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.askMechanic("   "));
    }

    @Test
    void testAskMechanic_chatModelReturnsNull_shouldThrow() {

        Mockito.when(chatModel.call(Mockito.any(Prompt.class)))
                .thenReturn(null);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.askMechanic("noise"));
    }

    @Test
    void testAskMechanic_chatModelReturnsResultWithoutOutput_shouldThrow() {

        ChatResponse resp = Mockito.mock(ChatResponse.class);
        Mockito.when(resp.getResult()).thenReturn(null);

        Mockito.when(chatModel.call(Mockito.any(Prompt.class)))
                .thenReturn(resp);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.askMechanic("noise"));
    }

    @Test
    void testAskMechanic_emptyText_shouldThrow() {

        AssistantMessage msg = new AssistantMessage("");
        Generation gen = new Generation(msg);
        ChatResponse resp = new ChatResponse(List.of(gen));

        Mockito.when(chatModel.call(Mockito.any(Prompt.class)))
                .thenReturn(resp);

        Assertions.assertThrows(IllegalStateException.class,
                () -> service.askMechanic("noise"));
    }
}
