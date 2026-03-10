package com.careup.branch.domain.chat.service;

import com.careup.branch.common.client.ChatFeignClient;
import com.careup.branch.common.client.OrderingInventoryClient;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.repository.BranchRepository;
import com.careup.branch.domain.chat.dto.sales.ProductSalesResponseDto;
import com.careup.branch.domain.chat.dto.stock.StockResponseDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class StockStrategy implements ChatbotStrategy{
    private final BranchRepository branchRepository;
    private final OrderingInventoryClient orderingInventoryClient;
    private final ObjectMapper objectMapper;
    private final ChatFeignClient client;

    @Autowired
    @Qualifier("inventoryAdvisorClient")
    private ChatClient inventoryAdvisorClient;


    @Override
    public boolean isSupport(String intent, String action) {
        return false;
    }

    @Override
    public ResponseEntity<?> execute(String intent, String action, JSONObject params, Long branchId) {
        switch (action) {
            case "GET" -> {

                CommonSuccessDto response = (CommonSuccessDto) orderingInventoryClient.getBranchProducts(branchId);
                List<OrderingInventoryClient.BranchProductResponseDto> stocks =
                        objectMapper.convertValue(
                                response.getResult(),
                                new TypeReference<List<OrderingInventoryClient.BranchProductResponseDto>>() {}
                        );


//                CommonSuccessDto response = orderingInventoryClient.getBranchProducts(branchId);
                System.out.println("branchID ======" + branchId);
                System.out.println("response =====" + response);
                StockResponseDto dto = new StockResponseDto().makeDto(intent,action,stocks);
                return ResponseEntity.ok(dto); }
            case "PATCH" -> { JSONArray itemsArr = params.getJSONArray("items");

                for (int i = 0; i < itemsArr.length(); i++) {
                    JSONObject item = itemsArr.getJSONObject(i);
                    Long productId = item.getLong("productId");
                    String product = item.getString("product");
                    Long quantity = item.getLong("quantity");
                    String type = quantity>=0 ? "INCREASE" : "DECREASE";
                    quantity = Math.abs(quantity);
                    String reason = item.getString("reason");
                    System.out.println("재고 변경 type " + type);
                    OrderingInventoryClient.StockAdjustRequest requestDto = OrderingInventoryClient.StockAdjustRequest.makeDto(productId,quantity, type,reason);
                    orderingInventoryClient.adjustStock(requestDto);
                }
                return ResponseEntity.ok("재고 수정 완료");}
            case "ANALYZE" -> {

                CommonSuccessDto response = (CommonSuccessDto) client.getBranchProducts(branchId);

                List<OrderingInventoryClient.BranchProductResponseDto> currentStocks =
                        objectMapper.convertValue(
                                response.getResult(),
                                new TypeReference<List<OrderingInventoryClient.BranchProductResponseDto>>() {}
                        );

                ZoneId zone = ZoneId.of("Asia/Seoul");
                LocalDate today = LocalDate.now(zone);
                LocalDate targetDate = today.plusDays(1);

                // ✅ 지난 4주간 주별 판매 데이터 수집
                List<Map<String, Object>> last4Weeks = new ArrayList<>();
                for (int i = 4; i >= 1; i--) {
                    LocalDate start = today.minusWeeks(i).with(DayOfWeek.MONDAY);
                    LocalDate end = today.minusWeeks(i).with(DayOfWeek.SUNDAY);

                    CommonSuccessDto weekSales = client.getProductSales(branchId, start, end, "HIGH_MARGIN");
                    ProductSalesResponseDto weekData =
                            objectMapper.convertValue(weekSales.getResult(), ProductSalesResponseDto.class);

                    Map<String, Object> weekMap = new HashMap<>();
                    weekMap.put("weekStart", start);
                    weekMap.put("weekEnd", end);
                    weekMap.put("products", weekData.getProducts());
                    last4Weeks.add(weekMap);
                }



                Map<String, Object> input = new HashMap<>();
                input.put("branchId", branchId);
                input.put("stocks", last4Weeks);       // ✅ 지난 4주간 판매량
                input.put("products", currentStocks);
                String jsonInput = null;
                try {
                    jsonInput = objectMapper.writeValueAsString(input);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                System.out.println("input data" + jsonInput);


                String result = inventoryAdvisorClient
                        .prompt()
                        .user(jsonInput)
                        .call()
                        .content();
                JsonNode parsed = null;
                try {
                    parsed = objectMapper.readTree(result);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }

                ObjectNode responseNode = (ObjectNode) parsed;

                responseNode.put("intent", intent);
                responseNode.put("action", action);

                return ResponseEntity.ok(responseNode); }
            default -> throw new IllegalArgumentException("지원하지 않는 요청입니다.");
        }

    }
}
