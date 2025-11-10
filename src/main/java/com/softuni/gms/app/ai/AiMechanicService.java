package com.softuni.gms.app.ai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AiMechanicService{

    private static final String SYSTEM_PROMPT = """
            You are an experienced automotive mechanic. When the user provides symptoms, respond with:
            - A concise summary of the likely issue.
            - Key checks or tests the user can perform safely.
            - Recommended next steps for the repair shop.
            Keep the answer within 1200 characters, use plain text (no markdown), and avoid disclaimers unless safety is involved.
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

        Prompt prompt = new Prompt(SYSTEM_PROMPT + "\n\nUser description:\n" + question.trim());
        ChatResponse response = chatModel.call(prompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("AI model returned no response");
        }

        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("AI model returned an empty response");
        }

        return text.trim();
    }
}
