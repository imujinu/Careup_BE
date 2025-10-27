package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // ========== 관리자용 조회 (모든 상품) ==========

    /**
     * 카테고리별 상품 조회 (관리자용)
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

    // ========== 고객용 조회 (활성화 + visibility 필터링) ==========

    /**
     * 고객용: visibility=ALL인 활성 상품만 조회 (페이지네이션)
     * - status = ACTIVE
     * - isDelYn = 'N'
     * - visibility = ALL
     */
    @Query("SELECT p FROM Product p WHERE p.visibility = :visibility AND p.status = 'ACTIVE' AND p.isDelYn = 'N'")
    Page<Product> findByVisibilityAndActive(@Param("visibility") Visibility visibility, Pageable pageable);

    /**
     * 고객용: visibility + 카테고리로 활성 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.visibility = :visibility AND p.category.id = :categoryId AND p.status = 'ACTIVE' AND p.isDelYn = 'N'")
    Page<Product> findByVisibilityAndCategoryIdAndActive(
            @Param("visibility") Visibility visibility,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    /**
     * 고객용: visibility + 검색어로 활성 상품 조회
     */
    @Query("SELECT p FROM Product p WHERE p.visibility = :visibility AND (p.name LIKE %:keyword% OR p.description LIKE %:keyword%) AND p.status = 'ACTIVE' AND p.isDelYn = 'N'")
    Page<Product> findByVisibilityAndKeywordAndActive(
            @Param("visibility") Visibility visibility,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    /**
     * 가격 범위로 상품 검색
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

    // ========== 더미 데이터 초기화용 메서드 ==========

    /**
     * 상품명 존재 여부 확인
     */
    boolean existsByName(String name);

    /**
     * 상품명으로 첫 번째 상품 조회
     */
    Optional<Product> findFirstByName(String name);
}