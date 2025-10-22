package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 카테고리별 상품 조회
     */
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * 상품 검색
     */
    @Query("SELECT p FROM Product p WHERE p.name LIKE %:keyword% OR p.description LIKE %:keyword%")
    Page<Product> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 카테고리 + 검색
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND (p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    Page<Product> searchByCategoryAndKeyword(@Param("categoryId") Long categoryId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 가격 범위로 상품 검색 (선택적 사용)
     */
    @Query("SELECT p FROM Product p WHERE " +
            "(:categoryId IS NULL OR p.category.id = :categoryId) AND " +
            "(:minPrice IS NULL OR p.minPrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR p.maxPrice <= :maxPrice) AND " +
            "(:keyword IS NULL OR p.name LIKE %:keyword% OR p.description LIKE %:keyword%)")
    Page<Product> filterProducts(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * 활성화된 상품만 조회
     */
    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND p.isDelYn = 'N'")
    Page<Product> findActiveProducts(Pageable pageable);

    /**
     * 카테고리별 활성화된 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.category.id = :categoryId AND p.status = 'ACTIVE' AND p.isDelYn = 'N'")
    Page<Product> findActiveByCategoryId(@Param("categoryId") Long categoryId, Pageable pageable);
}