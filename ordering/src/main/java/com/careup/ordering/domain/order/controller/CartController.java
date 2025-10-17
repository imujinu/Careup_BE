package com.careup.ordering.domain.order.controller;

import com.careup.ordering.common.dto.ResponseDto;
import com.careup.ordering.domain.order.dto.CartRequestDto;
import com.careup.ordering.domain.order.dto.CartResponseDto;
import com.careup.ordering.domain.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니 추가
     * POST /api/cart
     */
    @PostMapping
    public ResponseEntity<ResponseDto<CartResponseDto>> addToCart(
            @Valid @RequestBody CartRequestDto requestDto) {

        CartResponseDto response = cartService.addToCart(requestDto);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.CREATED), HttpStatus.CREATED);
    }

    /**
     * 장바구니 한건 조회
     */
    @GetMapping("/{cartId}")
    public ResponseEntity<ResponseDto<CartResponseDto>> getCart(
            @PathVariable Long cartId) {

        CartResponseDto response = cartService.getCartById(cartId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 회원별 장바구니 목록 조회
     */
    @GetMapping("/member/{memberId}")
    public ResponseEntity<ResponseDto<List<CartResponseDto>>> getCartsByMember(
            @PathVariable Long memberId) {

        List<CartResponseDto> response = cartService.getCartsByMember(memberId);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 회원별 장바구니 개수 조회
     */
    @GetMapping("/member/{memberId}/count")
    public ResponseEntity<ResponseDto<Long>> getCartCount(
            @PathVariable Long memberId) {

        Long count = cartService.getCartCount(memberId);

        return new ResponseEntity<>(ResponseDto.ok(count, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 장바구니 수량 수정
     */
    @PutMapping("/{cartId}")
    public ResponseEntity<ResponseDto<CartResponseDto>> updateCart(
            @PathVariable Long cartId,
            @RequestParam Long quantity) {

        CartResponseDto response = cartService.updateCart(cartId, quantity);

        return new ResponseEntity<>(ResponseDto.ok(response, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 장바구니 한건 삭제
     */
    @DeleteMapping("/{cartId}")
    public ResponseEntity<ResponseDto<Void>> deleteCart(
            @PathVariable Long cartId) {

        cartService.deleteCart(cartId);

        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }

    /**
     * 회원별 장바구니 전체 삭제 (체크아웃 후)
     */
    @DeleteMapping("/member/{memberId}")
    public ResponseEntity<ResponseDto<Void>> clearCart(
            @PathVariable Long memberId) {

        cartService.clearCart(memberId);

        return new ResponseEntity<>(ResponseDto.ok(null, HttpStatus.OK), HttpStatus.OK);
    }
}