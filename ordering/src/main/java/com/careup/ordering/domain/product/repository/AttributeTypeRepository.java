package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.AttributeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttributeTypeRepository extends JpaRepository<AttributeType, Long> {
    
    /**
     * 이름으로 속성 타입 조회
     */
    Optional<AttributeType> findByName(String name);
    
    /**
     * 이름 존재 여부 확인
     */
    boolean existsByName(String name);
    
    /**
     * 표시 순서대로 정렬하여 전체 조회
     */
    List<AttributeType> findAllByOrderByDisplayOrderAsc();
    
    /**
     * 속성 값을 포함하여 조회
     */
    @Query("SELECT DISTINCT at FROM AttributeType at " +
           "LEFT JOIN FETCH at.attributeValues av " +
           "WHERE at.id = :id")
    Optional<AttributeType> findByIdWithValues(Long id);
    
    /**
     * 모든 속성 타입과 값을 조회
     */
    @Query("SELECT DISTINCT at FROM AttributeType at " +
           "LEFT JOIN FETCH at.attributeValues av " +
           "ORDER BY at.displayOrder ASC, av.displayOrder ASC")
    List<AttributeType> findAllWithValues();
}
