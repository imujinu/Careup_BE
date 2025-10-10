package com.careup.ordering.domain.order.repository;

import com.careup.ordering.domain.order.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    // 회원별 장바구니 조회
    @Query("SELECT c FROM Cart c " +
            "JOIN FETCH c.member " +
            "JOIN FETCH c.branchProduct bp " +
            "JOIN FETCH bp.product " +
            "WHERE c.member.id = :memberId")
    List<Cart> findByMemberIdWithProduct(@Param("memberId") Long memberId);

    // 회원 + 상품으로 장바구니 조회 (중복 체크용)
    @Query("SELECT c FROM Cart c " +
            "WHERE c.member.id = :memberId " +
            "AND c.branchProduct.id = :branchProductId")
    Optional<Cart> findByMemberIdAndBranchProductId(
            @Param("memberId") Long memberId,
            @Param("branchProductId") Long branchProductId
    );

    // 회원별 장바구니 전체 삭제
    void deleteByMemberId(Long memberId);

    // 회원별 장바구니 개수 조회
    Long countByMemberId(Long memberId);
}