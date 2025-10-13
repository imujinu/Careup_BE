package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 카테고리별 상품 조회
    List<Product> findByCategoryId(Long categoryId);

    // 카테고리 + 상태 + 삭제여부로 조회
    List<Product> findByCategoryIdAndStatusAndIsDelYn(Long categoryId, ProductStatus status, String isDelYn);

    // 상품명 검색
    List<Product> findByNameContaining(String name);

    // 상품명 검색 + 상태 + 삭제여부
    List<Product> findByNameContainingAndStatusAndIsDelYn(String name, ProductStatus status, String isDelYn);

    // 상태별 조회
    List<Product> findByStatus(ProductStatus status);

    // 상태 + 삭제여부로 조회
    List<Product> findByStatusAndIsDelYn(ProductStatus status, String isDelYn);
}
