package com.careup.branch.domain.branch.service;

import lombok.extern.slf4j.Slf4j;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KPI 공식 계산 서비스
 * exp4j 라이브러리를 사용하여 수식을 평가합니다.
 */
@Service
@Slf4j
public class KpiCalculationService {

    /**
     * KPI 공식을 계산하여 결과를 반환
     * @param formula 계산 공식 (예: "total_sales / 1000000")
     * @param variables 변수명-값 맵
     * @return 계산 결과
     */
    public BigDecimal calculateFormula(String formula, Map<String, Double> variables) {
        try {
            log.info("공식 계산 시작 - Formula: {}, Variables: {}", formula, variables);

            // 공식 검증
            validateFormula(formula);

            // 표현식 빌더 생성
            ExpressionBuilder builder = new ExpressionBuilder(formula);

            // 변수 추가
            for (Map.Entry<String, Double> entry : variables.entrySet()) {
                builder.variable(entry.getKey());
            }

            // 표현식 빌드
            Expression expression = builder.build();

            // 변수 값 설정
            for (Map.Entry<String, Double> entry : variables.entrySet()) {
                expression.setVariable(entry.getKey(), entry.getValue());
            }

            // 계산 실행
            double result = expression.evaluate();

            // 결과가 무한대이거나 NaN인 경우 처리
            if (Double.isInfinite(result) || Double.isNaN(result)) {
                log.warn("계산 결과가 유효하지 않음: {}", result);
                return BigDecimal.ZERO;
            }

            // BigDecimal로 변환 (소수점 4자리까지)
            BigDecimal resultDecimal = BigDecimal.valueOf(result).setScale(4, RoundingMode.HALF_UP);

            log.info("공식 계산 완료 - Result: {}", resultDecimal);
            return resultDecimal;

        } catch (Exception e) {
            log.error("공식 계산 실패 - Formula: {}, Error: {}", formula, e.getMessage(), e);
            throw new IllegalArgumentException("공식 계산 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 공식에서 사용된 변수명 추출
     * @param formula 계산 공식
     * @return 변수명 Set
     */
    public Set<String> extractVariables(String formula) {
        try {
            // 정규식으로 변수 추출 (알파벳으로 시작하고 알파벳, 숫자, 언더스코어로 구성)
            Pattern variablePattern = Pattern.compile("\\b([a-zA-Z][a-zA-Z0-9_]*)\\b");
            Matcher matcher = variablePattern.matcher(formula);
            Set<String> variables = new java.util.HashSet<>();

            while (matcher.find()) {
                String token = matcher.group(1);
                // exp4j의 내장 함수 제외
                if (!isBuiltInFunction(token)) {
                    variables.add(token);
                }
            }

            log.debug("추출된 변수: {}", variables);
            return variables;
        } catch (Exception e) {
            log.error("변수 추출 실패: {}", e.getMessage());
            throw new IllegalArgumentException("잘못된 공식입니다: " + e.getMessage());
        }
    }

    /**
     * exp4j 내장 함수인지 확인
     */
    private boolean isBuiltInFunction(String token) {
        Set<String> builtInFunctions = Set.of(
            "abs", "acos", "asin", "atan", "cbrt", "ceil", "cos", "cosh",
            "exp", "floor", "log", "log10", "log2", "sin", "sinh", "sqrt",
            "tan", "tanh", "max", "min", "pow", "signum"
        );
        return builtInFunctions.contains(token.toLowerCase());
    }

    /**
     * 공식 유효성 검증
     * @param formula 검증할 공식
     */
    public void validateFormula(String formula) {
        if (formula == null || formula.trim().isEmpty()) {
            throw new IllegalArgumentException("공식이 비어있습니다.");
        }

        // 허용되지 않는 문자 체크 (보안)
        Pattern dangerousPattern = Pattern.compile("[;'\"\\\\]");
        Matcher matcher = dangerousPattern.matcher(formula);
        if (matcher.find()) {
            throw new IllegalArgumentException("공식에 허용되지 않는 문자가 포함되어 있습니다.");
        }

        try {
            // 변수 추출
            Set<String> variables = extractVariables(formula);

            // 표현식 빌더 생성 및 변수 등록
            ExpressionBuilder builder = new ExpressionBuilder(formula);
            for (String variable : variables) {
                builder.variable(variable);
            }

            // 표현식 빌드 테스트
            builder.build();

            log.debug("공식 검증 성공 - 사용된 변수: {}", variables);
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 공식 형식입니다: " + e.getMessage());
        }
    }

    /**
     * 공식 예제 및 설명 반환
     * @return 공식 예제 맵
     */
    public Map<String, String> getFormulaExamples() {
        return Map.of(
            "월 매출액(백만원)", "total_sales / 1000000",
            "주문 완료율(%)", "(completed_orders / total_orders) * 100",
            "평균 객단가", "total_sales / completed_orders",
            "발주 승인율(%)", "purchase_approval_rate",
            "수량 승인율(%)", "quantity_approval_rate",
            "출근율(%)", "((total_work_days - absent_count) / total_work_days) * 100",
            "시간당 매출", "total_sales / total_work_hours",
            "발주 대비 매출 비율", "(total_sales / total_purchase_amount) * 100",
            "발주 효율성 지수", "(approved_order_count / purchase_order_count) * (total_sales / total_purchase_amount)"
        );
    }

    /**
     * 사용 가능한 변수 목록 반환
     * @return 변수명-설명 맵
     */
    public Map<String, String> getAvailableVariables() {
        Map<String, String> variables = new java.util.HashMap<>();

        // 매출 관련
        variables.put("total_sales", "총 매출액");
        variables.put("avg_order_value", "평균 객단가");

        // 주문 관련
        variables.put("total_orders", "총 주문 건수");
        variables.put("completed_orders", "완료된 주문 건수");
        variables.put("cancelled_orders", "취소된 주문 건수");

        // 재고/발주 관련
        variables.put("purchase_order_count", "발주 건수");
        variables.put("total_purchase_amount", "총 발주 금액");
        variables.put("approved_order_count", "승인된 발주 건수");
        variables.put("rejected_order_count", "거절된 발주 건수");
        variables.put("avg_purchase_amount", "평균 발주 금액");
        variables.put("purchase_approval_rate", "발주 승인율(%)");
        variables.put("total_purchase_quantity", "총 발주 수량");
        variables.put("total_approved_quantity", "총 승인 수량");
        variables.put("quantity_approval_rate", "수량 승인율(%)");
        variables.put("inventory_turnover", "재고 회전율");

        // 출근 관련
        variables.put("total_work_days", "총 근무 일수");
        variables.put("total_work_hours", "총 근무 시간");
        variables.put("late_count", "지각 횟수");
        variables.put("absent_count", "결근 횟수");

        return variables;
    }
}

