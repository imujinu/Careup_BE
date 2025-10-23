package com.careup.branch.domain.chat.controller;


import com.careup.branch.domain.chat.dto.DocumentSearchResultDto;
import com.careup.branch.domain.chat.dto.req.ChatBotReqDto;
import com.careup.branch.domain.chat.dto.req.ChatOrderDto;
import com.careup.branch.domain.chat.dto.req.QueryRequestDto;
import com.careup.branch.domain.chat.dto.res.*;
import com.careup.branch.domain.chat.dto.res.sales.SalesPredictionResponseDto;
import com.careup.branch.domain.chat.service.ChatService;
import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.chat.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("chatbot")
@Slf4j
public class ChatController {
    private final ChatService chatService;
    private final RagService ragService;

    @PostMapping("/ask")
    public ResponseEntity<?> sendMessage(@RequestBody ChatBotReqDto dto){
        System.out.println("[Chat][Controller] : " + dto);
        ResponseEntity<?> response = chatService.handleUserQuery(dto);
        return new ResponseEntity<>(new CommonSuccessDto(response, HttpStatus.ACCEPTED.value(), "챗봇 응답 완료"), HttpStatus.ACCEPTED);
    }

    @PostMapping("/pdf")
    public ResponseEntity<?> searchPdf(@RequestBody QueryRequestDto dto){
        long step1Start = System.currentTimeMillis();
        List<DocumentSearchResultDto> list = ragService.retrieve(dto.getMessage(), dto.getMaxResult());
        log.info("유사도 검색 완료 - 걸린 시간: {} ms", System.currentTimeMillis() - step1Start);

        String response = ragService.generateAnswerWithContexts(dto.getMessage(), list);


        return new ResponseEntity<>(new CommonSuccessDto(response, HttpStatus.ACCEPTED.value(), "챗봇 응답 완료"), HttpStatus.ACCEPTED);
    }

    @GetMapping("/{branchId}/dailySales")
    public ResponseEntity<?> getDailySales(@PathVariable Long branchId){
        ChatDailySalesDto dto = chatService.getDailySales(branchId);
        return new ResponseEntity<>(new CommonSuccessDto("stockResponse", HttpStatus.ACCEPTED.value(), "챗봇 재고 응답 완료"), HttpStatus.ACCEPTED);
    }

    // [일일 매출 조회]
    @GetMapping("/{branchId}/dayLabor")
    public ResponseEntity<?> getLaborCost(@PathVariable Long branchId){
        ChatLaborCostDto dto = chatService.getLaborCost(branchId);
        return new ResponseEntity<>(new CommonSuccessDto(dto, HttpStatus.ACCEPTED.value(), "챗봇 일일 매출 조회 완료"), HttpStatus.ACCEPTED);
    }
    // [일일 매출 예측 조회]
    @GetMapping("/{branchId}/predictDayLabor")
    public ResponseEntity<?> getPredictDailySales(@PathVariable Long branchId){
        SalesPredictionResponseDto dto = chatService.getPredictDailySales(branchId);
        /*
         * 현재 매출은 {$}원 입니다.
         * 전일 대비 % , 전주 대비 % 변경 사항을 보이며
         * 남은 영업 시간 {%} 시간 동안 예상 되는 매출액은 {$} 입니다.
         *
         **/
        return new ResponseEntity<>(new CommonSuccessDto(dto, HttpStatus.ACCEPTED.value(), "챗봇 일일 매출 조회 완료"), HttpStatus.ACCEPTED);
    }

    //일일 근태 조회

    //재고 조회
    @GetMapping("/{branchId}/stock")
    public ResponseEntity<?> getStock(@PathVariable Long branchId){
        ChatStockDto dto = chatService.getStock(branchId);
        return new ResponseEntity<>(new CommonSuccessDto("stockResponse", HttpStatus.ACCEPTED.value(), "챗봇 재고 응답 완료"), HttpStatus.ACCEPTED);
    }
    //발주 요청
    @PostMapping("/{branchId}/order")
    public ResponseEntity<?> createOrder(@RequestBody ChatOrderDto dto){
        Long orderId = chatService.createOrder(dto);
        return new ResponseEntity<>(new CommonSuccessDto(orderId, HttpStatus.ACCEPTED.value(), "챗봇 발주 요청 완료"), HttpStatus.ACCEPTED);
    }
    //고객 조회
    @GetMapping("/{branchId}/visits")
    public ResponseEntity<?> getTodayVisits(@PathVariable Long branchId){
        ChatTodayVisitsDto dto = chatService.getTodayVisits(branchId);
        return new ResponseEntity<>(new CommonSuccessDto(dto, HttpStatus.ACCEPTED.value(), "챗봇 방문 고객 조회 완료"), HttpStatus.ACCEPTED);
    }


}
