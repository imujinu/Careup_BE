package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.AttributeValue;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductAttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductAttributeValueRepository extends JpaRepository<ProductAttributeValue, Long> {
    
    /**
     * 상품의 모든 속성 값 조회
     */
    @Query("SELECT pav FROM ProductAttributeValue pav " +
           "JOIN FETCH pav.attributeValue av " +
           "JOIN FETCH av.attributeType at " +
           "WHERE pav.product.id = :productId " +
           "ORDER BY at.displayOrder ASC, av.displayOrder ASC")
    List<ProductAttributeValue> findByProductIdWithDetails(@Param("productId") Long productId);
    
    /**
     * 상품과 속성 값으로 조회
     */
    Optional<ProductAttributeValue> findByProductAndAttributeValue(Product product, AttributeValue attributeValue);
    
    /**
     * 상품의 특정 속성 타입 값 조회
     */
    @Query("SELECT pav FROM ProductAttributeValue pav " +
           "JOIN FETCH pav.attributeValue av " +
           "WHERE pav.product.id = :productId " +
           "AND av.attributeType.id = :attributeTypeId")
    List<ProductAttributeValue> findByProductIdAndAttributeTypeId(
        @Param("productId") Long productId, 
        @Param("attributeTypeId") Long attributeTypeId
    );
    
    /**
     * 상품의 모든 속성 값 삭제
     */
    @Modifying
    @Query("DELETE FROM ProductAttributeValue pav WHERE pav.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
    
    /**
     * 상품 속성 값 단건 조회 (연관 엔티티 포함)
     */
    @Query("SELECT pav FROM ProductAttributeValue pav " +
           "JOIN FETCH pav.attributeValue av " +
           "JOIN FETCH av.attributeType at " +
           "JOIN FETCH pav.product p " +
           "WHERE pav.id = :id")
    Optional<ProductAttributeValue> findByIdWithDetails(@Param("id") Long id);
    
    /**
     * 중복 확인
     */
    boolean existsByProductAndAttributeValue(Product product, AttributeValue attributeValue);
}
