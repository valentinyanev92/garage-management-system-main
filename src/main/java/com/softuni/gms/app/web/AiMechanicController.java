package com.softuni.gms.app.web;

import com.softuni.gms.app.ai.AiMechanicService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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

        try {
            String answer = aiMechanicService.askMechanic(sanitizedQuestion);
            ModelAndView modelAndView = new ModelAndView("ai-chat");
            modelAndView.addObject("carId", carId);
            modelAndView.addObject("question", sanitizedQuestion);
            modelAndView.addObject("answer", answer);
            return modelAndView;
        } catch (Exception ex) {
            String encodedError = URLEncoder.encode("We could not get an answer right now. Please try again.", StandardCharsets.UTF_8);
            return new ModelAndView("redirect:/ai/ask?carId=" + carId + "&question="
                    + URLEncoder.encode(sanitizedQuestion, StandardCharsets.UTF_8) + "&error=" + encodedError);
        }
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
}


