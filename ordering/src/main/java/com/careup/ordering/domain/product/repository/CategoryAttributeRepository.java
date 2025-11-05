package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.entity.Category;
import com.careup.ordering.domain.product.entity.CategoryAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, Long> {
    
    /**
     * 카테고리별 속성 조회 (표시 순서대로)
     */
    @Query("SELECT ca FROM CategoryAttribute ca " +
           "JOIN FETCH ca.attributeType at " +
           "WHERE ca.category.id = :categoryId " +
           "ORDER BY ca.displayOrder ASC")
    List<CategoryAttribute> findByCategoryIdWithAttributeType(@Param("categoryId") Long categoryId);
    
    /**
     * 카테고리와 속성 타입으로 조회
     */
    Optional<CategoryAttribute> findByCategoryAndAttributeType(Category category, AttributeType attributeType);
    
    /**
     * 카테고리별 필수 속성만 조회
     */
    @Query("SELECT ca FROM CategoryAttribute ca " +
           "JOIN FETCH ca.attributeType at " +
           "WHERE ca.category.id = :categoryId " +
           "AND ca.isRequired = true " +
           "ORDER BY ca.displayOrder ASC")
    List<CategoryAttribute> findRequiredByCategoryId(@Param("categoryId") Long categoryId);
    
    /**
     * 중복 확인
     */
    boolean existsByCategoryAndAttributeType(Category category, AttributeType attributeType);
    
    /**
     * 카테고리에 속한 모든 속성 타입의 값까지 조회
     */
    @Query("SELECT DISTINCT ca FROM CategoryAttribute ca " +
           "JOIN FETCH ca.attributeType at " +
           "LEFT JOIN FETCH at.attributeValues av " +
           "WHERE ca.category.id = :categoryId " +
           "ORDER BY ca.displayOrder ASC, av.displayOrder ASC")
    List<CategoryAttribute> findByCategoryIdWithValues(@Param("categoryId") Long categoryId);
}
