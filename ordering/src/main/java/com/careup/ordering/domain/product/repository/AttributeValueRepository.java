package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.AttributeType;
import com.careup.ordering.domain.product.entity.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeValueRepository extends JpaRepository<AttributeValue, Long> {
    
    /**
     * 속성 타입별 속성 값 조회 (표시 순서대로)
     */
    List<AttributeValue> findByAttributeTypeOrderByDisplayOrderAsc(AttributeType attributeType);
    
    /**
     * 속성 타입 ID로 속성 값 조회
     */
    @Query("SELECT av FROM AttributeValue av " +
           "WHERE av.attributeType.id = :attributeTypeId " +
           "ORDER BY av.displayOrder ASC")
    List<AttributeValue> findByAttributeTypeId(@Param("attributeTypeId") Long attributeTypeId);
    
    /**
     * 활성화된 속성 값만 조회
     */
    @Query("SELECT av FROM AttributeValue av " +
           "WHERE av.attributeType.id = :attributeTypeId " +
           "AND av.isActive = true " +
           "ORDER BY av.displayOrder ASC")
    List<AttributeValue> findActiveByAttributeTypeId(@Param("attributeTypeId") Long attributeTypeId);
    
    /**
     * 속성 타입과 값으로 조회
     */
    Optional<AttributeValue> findByAttributeTypeAndValue(AttributeType attributeType, String value);
    
    /**
     * 중복 확인 (같은 속성 타입 내에서)
     */
    boolean existsByAttributeTypeAndValue(AttributeType attributeType, String value);
}
