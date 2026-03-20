package com.careup.branch.domain.chat.service;

import org.json.JSONObject;
import org.springframework.http.ResponseEntity;

public interface ChatbotStrategy {
    boolean isSupport(String intent, String action);
    ResponseEntity<?> execute(String intent, String action, JSONObject params, Long branchId);
}
