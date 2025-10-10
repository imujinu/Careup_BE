package com.careup.ordering.domain.product.repository;

import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product,Long> {

}
