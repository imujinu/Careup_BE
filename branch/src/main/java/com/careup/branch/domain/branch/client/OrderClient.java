package com.careup.branch.domain.branch.client;

import com.careup.branch.domain.branch.dto.royalty.OrderSalesResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "ordering-service")
public interface OrderClient {

    @GetMapping("/sales/branch-sales")
    OrderSalesResponseDto getBranchSales(
            @RequestParam("branchId") Long branchId,
            @RequestParam("applicableMonth") String applicableMonth
    );
}
