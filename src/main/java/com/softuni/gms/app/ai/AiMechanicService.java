package com.softuni.gms.app.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiMechanicService{

    private static final String SYSTEM_PROMPT = """
            You are a professional auto mechanic. Given the described car symptoms, respond with:
            - A brief, technical summary of the most probable cause (for mechanics).
            - Avoid user instructions.
            - Use diagnostic terms or components only.
            - Keep the response concise (max 100 characters), plain text only.
            """;

    private final ChatModel chatModel;

    @Autowired
    public AiMechanicService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String askMechanic(String question) {

        if (question == null || question.trim().isEmpty()) {
            log.error("askMechanic(): Question is null or empty");
            throw new IllegalArgumentException("Question must not be empty");
        }

        Prompt prompt = new Prompt(SYSTEM_PROMPT + "\n\nUser description:\n" + question.trim());
        ChatResponse response = chatModel.call(prompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            log.error("askMechanic(): Response is null or empty");
            throw new IllegalStateException("AI model returned no response");
        }

        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            log.error("askMechanic(): Text is null or empty");
            throw new IllegalStateException("AI model returned an empty response");
        }

        return text.trim();
    }
}
