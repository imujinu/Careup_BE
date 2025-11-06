package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductViewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductViewLogRepository extends JpaRepository<ProductViewLog, Long> {
    Optional<ProductViewLog> findTopByMemberOrderByCreatedAtDesc(Member member);
}
