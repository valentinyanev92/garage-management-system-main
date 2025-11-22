package com.softuni.gms.app.web;

import com.softuni.gms.app.ai.AiMechanicService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.anyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiMechanicController.class)
public class AiMechanicControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AiMechanicService aiMechanicService;

    private UUID randomCarId() {
        return UUID.randomUUID();
    }

    @Test
    void getAskPage_shouldReturnView_withParams() throws Exception {

        UUID carId = randomCarId();

        MockHttpServletRequestBuilder requestBuilder = get("/ai/ask")
                .with(user("testUser").roles("USER"))
                .with(csrf())
                .param("carId", carId.toString())
                .param("question", "question")
                .param("answer", "answer")
                .param("error", "none");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("ai-chat"))
                .andExpect(model().attribute("carId", carId))
                .andExpect(model().attribute("question", "question"))
                .andExpect(model().attribute("answer", "answer"))
                .andExpect(model().attribute("error", "none"));
    }

    @Test
    void postAsk_shouldReturnChatView_withAnswer() throws Exception {

        UUID carId = randomCarId();

        when(aiMechanicService.askMechanic(anyString())).thenReturn("Valid AI Answer");

        MockHttpServletRequestBuilder requestBuilder = post("/ai/ask")

                .with(user("testUser").roles("USER"))
                .with(csrf())
                .param("carId", carId.toString())
                .param("question", "question");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(view().name("ai-chat"))
                .andExpect(model().attribute("carId", carId))
                .andExpect(model().attribute("question", "question"))
                .andExpect(model().attribute("answer", "Valid AI Answer"));
    }

    @Test
    void postAsk_shouldRedirect_whenQuestionEmpty() throws Exception {

        UUID carId = randomCarId();

        String encodedError = URLEncoder.encode(
                "Please describe the problem before asking.",
                StandardCharsets.UTF_8
        );

        MockHttpServletRequestBuilder requestBuilder = post("/ai/ask")

                .with(user("testUser").roles("USER"))
                .with(csrf())
                .param("carId", carId.toString())
                .param("question", "   ");

        mockMvc.perform(requestBuilder)           // empty after trim
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ai/ask?carId=" + carId + "&error=" + encodedError));
    }

    @Test
    void postApply_shouldRedirectToRepairRequest_withEncodedAnswer() throws Exception {

        UUID carId = randomCarId();
        String answer = "Change oil & filter!";
        String encoded = URLEncoder.encode(answer, StandardCharsets.UTF_8);

        MockHttpServletRequestBuilder requestBuilder = post("/ai/apply")
                .with(user("testUser").roles("USER"))
                .with(csrf())
                .param("carId", carId.toString())
                .param("answer", answer);

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repairs/request/" + carId + "?aiSuggestion=" + encoded));
    }

    @Test
    void postApply_shouldHandleEmptyAnswer() throws Exception {

        UUID carId = randomCarId();

        String encodedEmpty = URLEncoder.encode("", StandardCharsets.UTF_8);

        MockHttpServletRequestBuilder requestBuilder = post("/ai/apply")
                .with(user("testUser").roles("USER"))
                .with(csrf())
                .param("carId", carId.toString())
                .param("answer", "");

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/repairs/request/" + carId + "?aiSuggestion=" + encodedEmpty));
    }

    @Test
    void handleAiErrors_shouldRedirectBackToAskWithError() throws Exception {

        UUID carId = randomCarId();

        when(aiMechanicService.askMechanic(anyString()))
                .thenThrow(new RuntimeException("AI Failure"));

        String question = "My engine is knocking";
        String encodedQuestion = URLEncoder.encode(question, StandardCharsets.UTF_8);
        String encodedError = URLEncoder.encode(
                "We could not get an answer right now. Please try again.",
                StandardCharsets.UTF_8
        );

        MockHttpServletRequestBuilder requestBuilder = post("/ai/ask")
                .with(user("testUser").roles("USER"))
                .with(csrf())
                .param("carId", carId.toString())
                .param("question", question);

        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ai/ask?carId=" + carId +
                        "&question=" + encodedQuestion +
                        "&error=" + encodedError));
    }

    @Test
    void handleAiErrors_shouldRethrow_whenNotAskEndpoint() throws Exception {

        when(aiMechanicService.askMechanic(anyString()))
                .thenThrow(new RuntimeException("Boom"));

        MockHttpServletRequestBuilder requestBuilder = post("/ai/apply")
                .with(user("testUser").roles("USER"))
                .with(csrf())
                .param("carId", randomCarId().toString())
                .param("question", "My engine is knocking");

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest());
    }
}
