package com.careup.ordering.domain.product.chat.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.product.chat.service.ChatInventoryService;
import com.careup.ordering.domain.product.dto.BranchProductResponseDto;
import com.careup.ordering.domain.product.dto.StockAdjustRequestDto;
import com.careup.ordering.domain.product.entity.BranchProduct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatInventoryController {
    private final ChatInventoryService chatInventoryService;
    @GetMapping("/inventory/{branchId}")
    public ResponseEntity<?> getBranchProducts(
            @PathVariable Long branchId) {
        List<BranchProduct> branchProducts = chatInventoryService.getProducts(branchId);
        List<BranchProductResponseDto> response = branchProducts.stream()
                .map(this::convertToBranchProductResponse)
                .collect(Collectors.toList());
        return new ResponseEntity<>(new CommonSuccessDto(response, HttpStatus.ACCEPTED.value(), " 챗봇 재고 서비스 응답 완료"), HttpStatus.ACCEPTED);
    }
    private BranchProductResponseDto convertToBranchProductResponse(BranchProduct branchProduct) {
        return BranchProductResponseDto.from(branchProduct);
    }



}
