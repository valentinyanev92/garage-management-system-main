package com.softuni.gms.app.ai;


import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiMechanicService {

    private static final String SYSTEM_PROMPT = """
            You are an auto mechanic.
            Respond with ONLY one short technical cause.
            Max 140 characters.
            No explanations, no lists, no extra text.
            Plain text only.
            """;

    private final ChatModel chatModel;

    @Autowired
    public AiMechanicService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askMechanic(String question) {

        if (question == null || question.trim().isEmpty()) {
            throw new IllegalArgumentException("Question must not be empty");
        }

        ChatOptions options = ChatOptions.builder()
                .maxTokens(50)
                .temperature(0.2)
                .build();

        Prompt prompt = new Prompt(
                SYSTEM_PROMPT + "\nUser description: " + question.trim(),
                options
        );

        ChatResponse response = chatModel.call(prompt);

        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("AI model returned empty response");
        }

        String text = response.getResult().getOutput().getText();

        if (text == null || text.isBlank()) {
            throw new IllegalStateException("AI model returned empty text");
        }

        text = text.trim();
        if (text.length() > 200) {
            text = text.substring(0, 200);
        }

        return text;
    }
}
