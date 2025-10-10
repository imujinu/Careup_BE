package com.careup.ordering.domain.order.service;

import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.order.dto.CartRequestDto;
import com.careup.ordering.domain.order.dto.CartResponseDto;
import com.careup.ordering.domain.order.entity.Cart;
import com.careup.ordering.domain.order.repository.CartRepository;
import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartRepository cartRepository;
    private final MemberRepository memberRepository;
    private final BranchProductRepository branchProductRepository;

    /**
     * 장바구니 추가
     */
    @Transactional
    public CartResponseDto addToCart(CartRequestDto requestDto) {
        log.info("장바구니 추가 - memberId: {}, branchProductId: {}",
                requestDto.getMemberId(), requestDto.getBranchProductId());

        // 1. 회원 조회
        Member member = memberRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 회원입니다. ID: " + requestDto.getMemberId()));

        // 2. 상품 조회
        BranchProduct branchProduct = branchProductRepository.findById(requestDto.getBranchProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 상품입니다. ID: " + requestDto.getBranchProductId()));

        // 3. 재고 확인
        if (branchProduct.getStockQuantity() < requestDto.getQuantity()) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: " + branchProduct.getStockQuantity());
        }

        // 4. 이미 장바구니에 있는지 확인
        Optional<Cart> existingCart = cartRepository.findByMemberIdAndBranchProductId(
                requestDto.getMemberId(), requestDto.getBranchProductId());

        Cart cart;
        if (existingCart.isPresent()) {
            // 이미 있으면 수량만 증가
            cart = existingCart.get();
            cart.increaseQuantity(requestDto.getQuantity());
            log.info("장바구니 수량 증가 - cartId: {}, newQuantity: {}", cart.getId(), cart.getQuantity());
        } else {
            // 없으면 새로 생성
            cart = Cart.builder()
                    .member(member)
                    .branchProduct(branchProduct)
                    .quantity(requestDto.getQuantity())
                    .attributeName(requestDto.getAttributeName())
                    .attributeValue(requestDto.getAttributeValue())
                    .build();
            cart = cartRepository.save(cart);
            log.info("장바구니 생성 - cartId: {}", cart.getId());
        }

        return convertToResponseDto(cart);
    }

    /**
     * 장바구니 단건 조회
     */
    public CartResponseDto getCartById(Long cartId) {
        log.info("장바구니 조회 - cartId: {}", cartId);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 장바구니입니다. ID: " + cartId));

        return convertToResponseDto(cart);
    }

    /**
     * 회원별 장바구니 목록 조회
     */
    public List<CartResponseDto> getCartsByMember(Long memberId) {
        log.info("회원별 장바구니 목록 조회 - memberId: {}", memberId);

        List<Cart> carts = cartRepository.findByMemberIdWithProduct(memberId);

        return carts.stream()
                .map(this::convertToResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * 장바구니 수량 수정
     */
    @Transactional
    public CartResponseDto updateCart(Long cartId, Long quantity) {
        log.info("장바구니 수정 - cartId: {}, quantity: {}", cartId, quantity);

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 장바구니입니다. ID: " + cartId));

        // 재고 확인
        if (cart.getBranchProduct().getStockQuantity() < quantity) {
            throw new IllegalStateException("재고가 부족합니다. 현재 재고: "
                    + cart.getBranchProduct().getStockQuantity());
        }

        cart.updateQuantity(quantity);

        return convertToResponseDto(cart);
    }

    /**
     * 장바구니 단건 삭제
     */
    @Transactional
    public void deleteCart(Long cartId) {
        log.info("장바구니 삭제 - cartId: {}", cartId);

        if (!cartRepository.existsById(cartId)) {
            throw new IllegalArgumentException("존재하지 않는 장바구니입니다. ID: " + cartId);
        }

        cartRepository.deleteById(cartId);
    }

    /**
     * 회원별 장바구니 전체 삭제 (체크아웃 후)
     */
    @Transactional
    public void clearCart(Long memberId) {
        log.info("장바구니 전체 삭제 - memberId: {}", memberId);

        cartRepository.deleteByMemberId(memberId);
    }

    /**
     * 회원별 장바구니 개수 조회
     */
    public Long getCartCount(Long memberId) {
        log.info("장바구니 개수 조회 - memberId: {}", memberId);

        return cartRepository.countByMemberId(memberId);
    }

    /**
     * Entity -> DTO 변환
     */
    private CartResponseDto convertToResponseDto(Cart cart) {
        BranchProduct branchProduct = cart.getBranchProduct();

        return CartResponseDto.builder()
                .cartId(cart.getId())
                .memberId(cart.getMember().getId())
                .memberName(cart.getMember().getName())
                .branchProductId(branchProduct.getId())
                .productId(branchProduct.getProduct().getId())
                .productName(branchProduct.getProduct().getName())
                .productPrice(branchProduct.getPrice())
                .quantity(cart.getQuantity())
                .totalPrice(branchProduct.getPrice() * cart.getQuantity())
                .attributeName(cart.getAttributeName())
                .attributeValue(cart.getAttributeValue())
                .build();
    }
}