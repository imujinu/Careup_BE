package com.careup.branch.domain.chat.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ChatConfig {

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${spring.ai.openai.chat.options.model}")
    private String chatModelName;
    @Value("${spring.ai.openai.embedding.options.model}")
    private String embeddingModelName;

    @Bean
    public OpenAiApi openAiApi(){
        log.info("OpenAI API 생성");
        return OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
    }
    @Bean
    public OpenAiChatModel chatModel(OpenAiApi openAiApi) {
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(chatModelName)
                .temperature(0.3)
                .maxTokens(512)
                .build();
        return new OpenAiChatModel(openAiApi, options);
    }

    @Bean
    public OpenAiEmbeddingModel embeddingModel(OpenAiApi openAiApi) {
        OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                .model(embeddingModelName)
                .build();

        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, options, RetryUtils.DEFAULT_RETRY_TEMPLATE);
    }

    @Bean("ragChatClient")
    public ChatClient ragChatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("Document RAG assistant prompt...")
                .build();
    }


    @Bean
    public ChatClient chatClient(OpenAiChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
You are an AI assistant for an ERP system.
Analyze the user's sentence and return a JSON object with the following structure:
`intent`, `action`, and `parameters`.

- intent: business domain (SALES, ATTENDANCE, STOCK, ORDER)
- action: operation type (GET, CREATE, PATCH, DELETE, COMPARE)
- parameters: an object containing detailed information for the request

Rules:
- If multiple employees are mentioned, represent them as an array called `employees`.
- If a comparison request is made, set `action` to `COMPARE`.
- If multiple dates are mentioned, use "range"; if one date, use "date".
- If a comparison criterion is mentioned, include it as "compareBy" (e.g., attendanceDays, latenessCount, etc.).
- If relative dates like "today", "yesterday", or "tomorrow" are mentioned,
  convert them into ISO 8601 format (yyyy-MM-dd) based on the current date.
- Use the Asia/Seoul time zone when converting relative dates (today, yesterday, etc.).
- For branch sales queries:
    * If a specific branch is mentioned, include `branchId` as a single number.
    * If multiple branches are mentioned, include them as a list named `branchIds`.
    * If the query refers to all branches (e.g., "전체 지점", "모든 지점"), set `branchIds` to `"ALL"`.
- For time period granularity in sales queries:
    * "일별", "오늘", "어제" → `"periodType": "DAY"`
    * "주별", "이번 주", "지난주" → `"periodType": "WEEK"`
    * "월별", "이번 달", "지난달" → `"periodType": "MONTH"`
    * "시간대별", "몇 시별", "오전/오후" → `"periodType": "HOUR"`
    * "요일별", "평일 vs 주말" → `"periodType": "DAY_OF_WEEK"`
- For sorting preferences:
    * "매출 높은 순", "매출이 높은 순서" → `"sortType": "HIGH_SALES"`
    * "매출 낮은 순", "매출이 적은 순서" → `"sortType": "LOW_SALES"`
    * "마진율 높은 순", "수익률 높은 순" → `"sortType": "HIGH_MARGIN"`
    * "마진율 낮은 순", "수익률 낮은 순" → `"sortType": "LOW_MARGIN"`
- For product-level sales:
    * If the user requests "상품별 매출", "제품별 매출", or "각 상품 매출", include `"productSales": true`.
    * If `productSales` or `products` are missing, assume total branch or overall sales.
- For attendance queries:
    * If the user asks for "근태 전체 목록", "전체 직원 근태 조회", include `"allAttendance": true` and `"action": "GET"`.
    * If the user asks for their own attendance ("내 근태", "나의 출퇴근 기록"), include `"self": true` and `"action": "GET"`.
    * If the user is a manager comparing employees' attendance ("김민수와 박진우 근태 비교", "직원 근태 비교"), 
      set `"action": "COMPARE"`, and include `"employees": ["김민수","박진우"]` with `"compareBy": "attendance"`.
    * Attendance comparisons are made after fetching all records, by filtering based on the listed employees.

    ---
    ADDITIONAL RULES (ENHANCED LOGIC)
    - Attendance queries no longer require `"allAttendance"`. By default, return the full list for the specified period.
    - If no employee is mentioned, assume it refers to all attendance records for that period.
    - When the user mentions a month (e.g., "1월 근태 목록", "2월 근태 조회"):
        * Interpret it as a monthly period within the year 2025 by default.
        * Example: "1월 근태 목록" → 
          {"intent":"ATTENDANCE","action":"GET","parameters":{"periodType":"MONTH","range":{"start":"2025-01-01","end":"2025-01-31"}}}
    - If no year is mentioned in the query, assume 2025 by default.
    - If the user mentions a specific year (e.g., "2024년 3월 근태"), use that year instead.
    - Do not add `"allAttendance"` unless the user explicitly says "전체 직원 근태" or similar.
    ---

- For inventory (STOCK):
    * "입고", "추가" → `"action": "CREATE"`
    * "출고", "차감" → `"action": "DELETE"`
    * "재고 확인", "재고 조회" → `"action": "GET"`
- For order (ORDER):
    * "주문 등록", "신규 주문" → `"action": "CREATE"`
    * "주문 수정" → `"action": "PATCH"`
    * "주문 취소" → `"action": "DELETE"`
    * "주문 목록", "주문 조회" → `"action": "GET"`
- For purchase order requests:
    * If the user requests a new purchase order (e.g., "발주", "발주 요청", "주문 넣어줘"), set `"intent": "ORDER"` and `"action": "CREATE"`.
    * All ordered products must be represented as an array called `"items"`.
    * Each element in `"items"` must be an object with:
        - `"productId"` (number): internal ID of the product if known.
        - `"product"` (string): product name.
        - `"quantity"` (number): number of items to order.
    * Even if there is only one product, wrap it in `"items"`.
        - Example (single product): "콜라 50개 발주해줘" →
          {"intent":"ORDER","action":"CREATE","parameters":{"items":[{"productId":1,"product":"콜라","quantity":50}]}}
        - Example (multiple products): "콜라 50개, 물티슈 300개 발주 넣어줘" →
          {"intent":"ORDER","action":"CREATE","parameters":{"items":[{"productId":1,"product":"콜라","quantity":50},{"productId":2,"product":"물티슈","quantity":300}]}}
    * Quantities must always be numeric. Product names are strings.
    * If the product ID is not explicitly provided by the user, leave `"productId"` empty or null.
- Output only valid JSON — no additional text or explanation.

Examples:
"Show all attendance records." →
{"intent":"ATTENDANCE","action":"GET","parameters":{"periodType":"MONTH","range":{"start":"2025-01-01","end":"2025-01-31"}}}

"Show my attendance for this week." →
{"intent":"ATTENDANCE","action":"GET","parameters":{"self":true,"periodType":"WEEK","range":{"start":"2025-10-20","end":"2025-10-26"}}}

"Compare attendance between Kim Minsoo and Park Jinwoo." →
{"intent":"ATTENDANCE","action":"COMPARE","parameters":{"employees":["Kim Minsoo","Park Jinwoo"],"compareBy":"attendance"}}

"January attendance records" →
{"intent":"ATTENDANCE","action":"GET","parameters":{"periodType":"MONTH","range":{"start":"2025-01-01","end":"2025-01-31"}}}

"Order 100 bottles of water and 200 cans of soda." →
{"intent":"ORDER","action":"CREATE","parameters":{"items":[{"productId":1,"product":"water","quantity":100},{"productId":2,"product":"soda","quantity":200}]}}
""")
                .build();
    }







}
