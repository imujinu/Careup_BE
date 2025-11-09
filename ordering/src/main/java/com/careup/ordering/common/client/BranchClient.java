package com.careup.ordering.common.client;

import com.careup.ordering.common.dto.CommonSuccessDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "branch-service", url = "${feign.branch-service.url}") // Eureka를 통한 서비스 디스커버리 사용 url = "${feign.branch.url:http://localhost:8081}")주석처리함 문제있음 복구함
public interface BranchClient {

    /**
     * Branch ID 목록으로 Branch 정보 조회
     */
    @GetMapping("/branch/list-by-ids")
    Map<String, Object> getBranchesByIds(@RequestParam("branchIds") List<Long> branchIds);

    /**
     * 단일 Branch ID로 지점 정보 조회
     */
    @GetMapping("/branch/{branchId}")
    Map<String, Object> getBranchById(@PathVariable("branchId") Long branchId);

    /**
     * 특정 지점의 인근 지점 조회 (위치 기반)
     */
    @GetMapping("/branch/{branchId}/nearby")
    Map<String, Object> getNearbyBranches(
            @PathVariable("branchId") Long branchId,
            @RequestParam(value = "radiusKm", defaultValue = "10.0") Double radiusKm);

    /**
     * 지점의 예상 매출액 조회 (branch 모듈의 SalesForecast 사용)
     */
    @GetMapping("/sales-forecast/branch/{branchId}")
    Map<String, Object> getBranchSalesForecast(@PathVariable("branchId") Long branchId);

    /**
     * 주문 생성 알림
     */

    @GetMapping("/employees/chat/list")
    CommonSuccessDto getAllEmployees(
            @RequestHeader("Authorization") String token
    );

    /**
     * 내부 API용: employeeId로 현재 활성화된 branchId 조회
     */
    @GetMapping("/employees/internal/branch-id/{employeeId}")
    Map<String, Object> getBranchIdByEmployeeId(@PathVariable("employeeId") Long employeeId);

    /**
     * 내부 API용: employeeId로 직원 정보 조회
     */
    @GetMapping("/employees/internal/{employeeId}")
    Map<String, Object> getEmployeeById(@PathVariable("employeeId") Long employeeId);
}
