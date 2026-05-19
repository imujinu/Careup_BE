package com.careup.branch.domain.chat.service;

import com.careup.branch.domain.chat.dto.req.ChatBotReqDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Service
@Slf4j
public class ChatbotProxyService {

    private final RestClient restClient;
    private final String chatbotServiceUrl;

    public ChatbotProxyService(@Value("${chatbot.service.url:http://localhost:8000}") String chatbotServiceUrl) {
        this.chatbotServiceUrl = chatbotServiceUrl;
        this.restClient = RestClient.builder()
                .baseUrl(chatbotServiceUrl)
                .build();
    }

    public Optional<Object> ask(ChatBotReqDto dto) {
        try {
            Object response = restClient.post()
                    .uri("/chatbot/ask")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dto)
                    .retrieve()
                    .body(Object.class);
            return Optional.ofNullable(response);
        } catch (RestClientException e) {
            log.warn("FastAPI chatbot call failed. Falling back to deprecated Spring AI chatbot. url={}", chatbotServiceUrl, e);
            return Optional.empty();
        }
    }
}
