package com.careup.branch.domain.branch.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.branch.service.KpiCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * KPI 공식 관련 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/kpi/formula")
@RequiredArgsConstructor
public class KpiFormulaController {

    private final KpiCalculationService kpiCalculationService;

    /**
     * 사용 가능한 변수 목록 조회
     */
    @GetMapping("/variables")
    public ResponseEntity<?> getAvailableVariables() {
        Map<String, String> variables = kpiCalculationService.getAvailableVariables();
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(variables)
                        .status_code(HttpStatus.OK.value())
                        .status_message("사용 가능한 변수 목록 조회 완료")
                        .build()
        );
    }

    /**
     * 공식 예제 목록 조회
     */
    @GetMapping("/examples")
    public ResponseEntity<?> getFormulaExamples() {
        Map<String, String> examples = kpiCalculationService.getFormulaExamples();
        return ResponseEntity.ok(
                CommonSuccessDto.builder()
                        .result(examples)
                        .status_code(HttpStatus.OK.value())
                        .status_message("공식 예제 목록 조회 완료")
                        .build()
        );
    }

    /**
     * 공식 유효성 검증
     */
    @PostMapping("/validate")
    public ResponseEntity<?> validateFormula(@RequestBody Map<String, String> request) {
        String formula = request.get("formula");
        log.info("========== KPI 공식 검증 요청 시작 ==========");
        log.info("요청 공식: {}", formula);

        try {
            kpiCalculationService.validateFormula(formula);
            log.info("공식 유효성 검증 성공");

            // 공식에서 사용된 변수 추출
            Set<String> variables = kpiCalculationService.extractVariables(formula);
            log.info("추출된 변수 목록: {}", variables);

            Map<String, Object> result = Map.of(
                "valid", true,
                "usedVariables", variables,
                "message", "유효한 공식입니다."
            );

            log.info("응답 결과: {}", result);
            log.info("========== KPI 공식 검증 요청 완료 (성공) ==========");

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(result)
                            .status_code(HttpStatus.OK.value())
                            .status_message("공식 검증 완료")
                            .build()
            );
        } catch (IllegalArgumentException e) {
            log.warn("공식 유효성 검증 실패 - 에러: {}", e.getMessage());

            Map<String, Object> result = Map.of(
                "valid", false,
                "message", e.getMessage()
            );

            log.info("응답 결과: {}", result);
            log.info("========== KPI 공식 검증 요청 완료 (실패) ==========");

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(result)
                            .status_code(HttpStatus.OK.value())
                            .status_message("공식 검증 실패")
                            .build()
            );
        }
    }

    /**
     * 공식 테스트 계산
     */
    @PostMapping("/test")
    public ResponseEntity<?> testFormula(@RequestBody Map<String, Object> request) {
        log.info("========== KPI 공식 테스트 계산 요청 시작 ==========");

        try {
            String formula = (String) request.get("formula");
            @SuppressWarnings("unchecked")
            Map<String, Object> variableValues = (Map<String, Object>) request.get("variables");

            log.info("요청 공식: {}", formula);
            log.info("요청 변수: {}", variableValues);

            // Object를 Double로 변환
            Map<String, Double> doubleVariables = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : variableValues.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    doubleVariables.put(entry.getKey(), ((Number) value).doubleValue());
                } else {
                    doubleVariables.put(entry.getKey(), Double.parseDouble(value.toString()));
                }
            }

            log.info("변환된 변수 (Double): {}", doubleVariables);

            // 공식 계산
            java.math.BigDecimal result = kpiCalculationService.calculateFormula(formula, doubleVariables);
            log.info("계산 결과: {}", result);

            Map<String, Object> responseResult = Map.of(
                "success", true,
                "result", result,
                "message", "계산 완료"
            );

            log.info("응답 결과: {}", responseResult);
            log.info("========== KPI 공식 테스트 계산 요청 완료 (성공) ==========");

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(responseResult)
                            .status_code(HttpStatus.OK.value())
                            .status_message("공식 테스트 완료")
                            .build()
            );
        } catch (Exception e) {
            log.error("공식 테스트 계산 실패 - 에러: {}", e.getMessage(), e);

            Map<String, Object> responseResult = Map.of(
                "success", false,
                "message", e.getMessage()
            );

            log.info("응답 결과: {}", responseResult);
            log.info("========== KPI 공식 테스트 계산 요청 완료 (실패) ==========");

            return ResponseEntity.ok(
                    CommonSuccessDto.builder()
                            .result(responseResult)
                            .status_code(HttpStatus.OK.value())
                            .status_message("공식 테스트 실패")
                            .build()
            );
        }
    }
}

