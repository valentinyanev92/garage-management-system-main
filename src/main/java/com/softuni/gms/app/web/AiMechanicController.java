package com.softuni.gms.app.web;

import com.softuni.gms.app.ai.AiMechanicService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Controller
@RequestMapping("/ai")
public class AiMechanicController {

    private final AiMechanicService aiMechanicService;

    @Autowired
    public AiMechanicController(AiMechanicService aiMechanicService) {
        this.aiMechanicService = aiMechanicService;
    }

    @GetMapping("/ask")
    public ModelAndView showChatPage(@RequestParam UUID carId,
                                     @RequestParam(value = "question", required = false) String question,
                                     @RequestParam(value = "answer", required = false) String answer,
                                     @RequestParam(value = "error", required = false) String error) {

        ModelAndView modelAndView = new ModelAndView("ai-chat");
        modelAndView.addObject("carId", carId);
        modelAndView.addObject("question", question);
        modelAndView.addObject("answer", answer);
        modelAndView.addObject("error", error);
        return modelAndView;
    }

    @PostMapping("/ask")
    public ModelAndView askMechanic(@RequestParam UUID carId,
                                    @RequestParam String question) {
        String sanitizedQuestion = question == null ? "" : question.trim();
        if (sanitizedQuestion.isEmpty()) {
            String encodedError = URLEncoder.encode("Please describe the problem before asking.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/ai/ask?carId=" + carId + "&error=" + encodedError);
        }

        String answer = aiMechanicService.askMechanic(sanitizedQuestion);
        ModelAndView modelAndView = new ModelAndView("ai-chat");
        modelAndView.addObject("carId", carId);
        modelAndView.addObject("question", sanitizedQuestion);
        modelAndView.addObject("answer", answer);
        return modelAndView;
    }

    @PostMapping("/apply")
    public ModelAndView applyAnswer(@RequestParam UUID carId,
                                    @RequestParam String answer) {
        if (answer == null) {
            answer = "";
        }
        String encodedAnswer = URLEncoder.encode(answer, StandardCharsets.UTF_8);
        return new ModelAndView("redirect:/repairs/request/" + carId + "?aiSuggestion=" + encodedAnswer);
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAiErrors(HttpServletRequest request, Exception ex) throws Exception {

        String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.contains("/ask")) {
            throw ex;
        }

        String carIdParam = request.getParameter("carId");
        String questionParam = request.getParameter("question");
        String sanitizedQuestion = questionParam == null ? "" : questionParam.trim();
        String encodedError = URLEncoder.encode("We could not get an answer right now. Please try again.", StandardCharsets.UTF_8);

        StringBuilder redirect = new StringBuilder("redirect:/ai/ask");
        String separator = "?";
        if (carIdParam != null && !carIdParam.isBlank()) {
            redirect.append(separator).append("carId=").append(carIdParam);
            separator = "&";
        }
        if (!sanitizedQuestion.isEmpty()) {
            redirect.append(separator).append("question=")
                    .append(URLEncoder.encode(sanitizedQuestion, StandardCharsets.UTF_8));
            separator = "&";
        }
        redirect.append(separator).append("error=").append(encodedError);

        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName(redirect.toString());
        return modelAndView;
    }
}
