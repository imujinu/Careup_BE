package com.careup.ordering.domain.product.chat.service;

import com.careup.ordering.domain.product.entity.BranchProduct;
import com.careup.ordering.domain.product.repository.BranchProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ChatInventoryService {
    private final BranchProductRepository branchProductRepository;
    public List<BranchProduct> getProducts(Long branchId) {
        return branchProductRepository.findByBranchIdWithProduct(branchId);
    }
}
