package com.careup.ordering.domain.recomendation.repository;

import com.careup.ordering.domain.recomendation.entity.ProductCoPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCoPurchaseRepository extends JpaRepository<ProductCoPurchase, Long> {
    Optional<ProductCoPurchase> findByProductAIdAndProductBId(Long productAId, Long productBId);
}