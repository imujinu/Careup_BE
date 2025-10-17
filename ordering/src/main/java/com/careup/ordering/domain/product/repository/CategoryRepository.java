package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category,Long> {
    List<Category> findAllByOrderByName();
    
    // 카테고리명으로 조회
    Category findByName(String name);
}
