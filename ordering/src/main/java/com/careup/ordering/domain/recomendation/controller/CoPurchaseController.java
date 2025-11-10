package com.careup.ordering.domain.recomendation.controller;

import com.careup.ordering.common.dto.CommonSuccessDto;
import com.careup.ordering.domain.product.dto.ProductResponseDto;
import com.careup.ordering.domain.recomendation.dto.ProductCoPurchaseResDto;
import com.careup.ordering.domain.recomendation.dto.ProductsResDto;
import com.careup.ordering.domain.recomendation.service.CoPurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rec")
public class CoPurchaseController {
    private final CoPurchaseService queryService;

    @GetMapping("/{memberId}")
    public ResponseEntity<?> getCoPurchaseList(@PathVariable Long memberId,
                                               @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Object result = queryService.getCoPurchaseList(memberId, pageable);
        return new ResponseEntity<>(new CommonSuccessDto(result, HttpStatus.OK.value(), "상품 함께 구매 횟수 조회 성공"), HttpStatus.OK);
    }

    @GetMapping("/products")
    public ResponseEntity<?> getProducts(@PathVariable Long memberId,
                                               @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProductsResDto> result = queryService.getProducts(pageable);
        return new ResponseEntity<>(new CommonSuccessDto(result, HttpStatus.OK.value(), "상품 함께 구매 횟수 조회 성공"), HttpStatus.OK);
    }

}
