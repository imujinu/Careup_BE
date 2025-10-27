package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchProductRepository extends JpaRepository<BranchProduct, Long> {

    // 지점별 상품 조회 (Product와 함께 로드)
    @Query("SELECT bp FROM BranchProduct bp JOIN FETCH bp.product WHERE bp.branchId = :branchId")
    List<BranchProduct> findByBranchIdWithProduct(@Param("branchId") Long branchId);

    // 지점별 상품 조회
    List<BranchProduct> findByBranchId(Long branchId);

    // 지점&상품으로 조회
    @Query("SELECT bp FROM BranchProduct bp WHERE bp.branchId = :branchId AND bp.product.id = :productId")
    Optional<BranchProduct> findByBranchIdAndProductId(@Param("branchId") Long branchId, @Param("productId") Long productId);

    // 지점&상품 존재 여부 확인
    boolean existsByBranchIdAndProductId(Long branchId, Long productId);
    
    // 상품 ID로 지점별 상품 조회
    @Query("SELECT bp FROM BranchProduct bp WHERE bp.product.id = :productId")
    List<BranchProduct> findByProductId(@Param("productId") Long productId);

    // 상품명으로 검색
    List<BranchProduct> findByProduct_NameContaining(String keyword);

    // 지점별 + 상품명 검색
    List<BranchProduct> findByBranchIdAndProduct_NameContaining(Long branchId, String keyword);

    /**
     * 특정 상품을 판매하는 모든 지점별 상품 조회
     */
    List<BranchProduct> findByProduct(Product product);
}
