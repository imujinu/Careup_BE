package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart,Long> {
    // 회원별 장바구니 조회
    List<Cart> findByMemberId(Long memberId);

    // 장바구니 항목 존재 확인
    boolean existsByMemberIdAndBranchProductsId(Long memberId,Long branchProductId);

    // 장바구니 항목 삭제
    void deleteByCartIdAndMemberId(Long cartId,Long memberId);

}
