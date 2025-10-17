# 예상 매출액 관련 기능 추가 및 수정 완료 보고서

## 작업 개요
지점 관리자(BRANCH_ADMIN, FRANCHISE_OWNER)의 소속 지점 예상 매출액(SalesForecast) 조회 기능을 branch 모듈과 연동하도록 수정하였습니다.

## 주요 변경사항

### 1. ordering 모듈 - BranchClient 수정
**파일**: `ordering/src/main/java/com/careup/ordering/common/client/BranchClient.java`

#### 변경 내용
- branch 모듈의 예상 매출액 조회 API 메서드 추가
```java
@GetMapping("/sales-forecast/branch/{branchId}")
Map<String, Object> getBranchSalesForecast(@PathVariable("branchId") Long branchId);
```

### 2. ordering 모듈 - 새로운 DTO 생성
**파일**: `ordering/src/main/java/com/careup/ordering/domain/order/dto/response/SalesForecastResponseDto.java`

#### 변경 내용
- branch 모듈의 SalesForecast 응답을 받기 위한 DTO 생성
- `CurrentForecast`: 현재 적용 중인 예상 매출 정보
- `ForecastHistory`: 예상 매출 이력 정보
- 주요 필드:
  - branchId: 지점 ID
  - branchName: 지점 이름
  - amount: 예상 매출액
  - periodStart: 시작 기간
  - periodEnd: 종료 기간
  - forecastBasis: 예측 근거

### 3. ordering 모듈 - SalesService 수정
**파일**: `ordering/src/main/java/com/careup/ordering/domain/order/service/SalesService.java`

#### 변경 내용
- `getSalesForecast()` 메서드를 branch 모듈의 API를 호출하도록 수정
- **MSA 통신 방식 적용**: Feign Client를 통해 branch 모듈과 통신
- **Fallback 로직 구현**: branch 모듈 통신 실패 시 로컬 계산 방식으로 대체
- 기존 로직은 `getLocalSalesForecast()` 메서드로 분리하여 유지

#### 주요 로직
1. **정상 흐름**: branch 모듈의 `/sales-forecast/branch/{branchId}` API 호출
2. **예외 처리**: 통신 실패 시 ordering 모듈 내부의 지난 30일 평균 매출 기반 예측 사용

### 4. ordering 모듈 - SalesController 수정
**파일**: `ordering/src/main/java/com/careup/ordering/domain/order/controller/SalesController.java`

#### 변경 내용
- 예상 매출액 조회 엔드포인트의 응답 타입을 `SalesForecastResponseDto`로 변경
- 에러 메시지 개선
- 불필요한 import 제거

## API 명세

### ordering 모듈 - 예상 매출액 조회 API
```
GET /sales/forecast?branchId={branchId}&targetDate={targetDate}
```

#### 요청 파라미터
- `branchId` (Long, required): 지점 ID
- `targetDate` (LocalDate, required): 예측 대상 날짜 (형식: yyyy-MM-dd)

#### 권한
- `BRANCH_ADMIN`, `FRANCHISE_OWNER`

#### 응답 예시
```json
{
  "result": {
    "branchId": 1,
    "branchName": "강남점",
    "currentForecast": {
      "id": 123,
      "branchId": 1,
      "branchName": "강남점",
      "amount": 5000000,
      "periodStart": "2025-10-01T00:00:00.000+00:00",
      "periodEnd": "2025-10-31T00:00:00.000+00:00",
      "createdAt": "2025-10-01T09:00:00.000+00:00",
      "forecastBasis": "지난 30일 평균 매출 기반 + 요일별 가중치"
    },
    "forecastHistory": [
      {
        "id": 122,
        "branchId": 1,
        "branchName": "강남점",
        "amount": 4800000,
        "periodStart": "2025-09-01T00:00:00.000+00:00",
        "periodEnd": "2025-09-30T00:00:00.000+00:00",
        "createdAt": "2025-09-01T09:00:00.000+00:00",
        "forecastBasis": "지난 30일 평균 매출 기반 + 요일별 가중치"
      }
    ]
  },
  "status_code": 200,
  "status_message": "소속 가맹점의 예상 매출액 조회 성공"
}
```

## 아키텍처 개선 사항

### MSA 통신 구조
```
[ordering 모듈]
    ↓ Feign Client 호출
[branch 모듈]
    ↓ DB 조회
[SalesForecast 테이블]
```

### 장점
1. **관심사 분리**: 예상 매출액 데이터는 branch 모듈에서 관리
2. **중복 제거**: ordering 모듈에서 독립적으로 계산하지 않고 branch 모듈의 데이터 활용
3. **일관성 유지**: 모든 서비스에서 동일한 예상 매출액 데이터 사용
4. **장애 격리**: branch 모듈 장애 시에도 로컬 계산으로 서비스 제공 가능

## 테스트 시나리오

### 1. 정상 시나리오
1. 지점 관리자 권한으로 로그인
2. `GET /sales/forecast?branchId=1&targetDate=2025-10-31` 호출
3. branch 모듈에서 예상 매출액 데이터 조회
4. 현재 예상 매출과 이력 반환

### 2. Fallback 시나리오
1. branch 모듈 서비스 다운 또는 네트워크 장애
2. `GET /sales/forecast?branchId=1&targetDate=2025-10-31` 호출
3. ordering 모듈 내부 로직으로 예상 매출 계산
4. 로컬 계산 결과 반환 (forecastBasis에 "로컬 계산" 표시)

## 추가 구현된 기능

### ordering 모듈 - branch 서비스용 매출 통계 조회 API
```
GET /sales/statistics-for-forecast?branchId={branchId}&days={days}
```

#### 설명
- branch 모듈의 SalesForecastService에서 예상 매출 계산 시 사용
- 지정된 기간의 매출 통계 제공
- 평균 일일 매출, 총 매출, 총 주문 수 등 반환

## 구현 원칙 준수 확인

✅ **Controller - Service - Repository 구조**
- Controller: 요청/응답 처리
- Service: 비즈니스 로직 및 Feign 통신
- Repository: 데이터베이스 접근 (기존 OrderRepository 활용)

✅ **DTO 활용**
- `SalesForecastResponseDto`: branch 모듈 응답 매핑
- `CurrentForecast`, `ForecastHistory`: 중첩 DTO로 구조화

✅ **MSA Feign 통신 방식**
- BranchClient를 통한 branch 모듈 통신
- 기존 인근 지점 조회, Branch 정보 조회와 동일한 패턴 적용

## 관련 파일 목록

### 수정된 파일
1. `ordering/src/main/java/com/careup/ordering/common/client/BranchClient.java`
2. `ordering/src/main/java/com/careup/ordering/domain/order/service/SalesService.java`
3. `ordering/src/main/java/com/careup/ordering/domain/order/controller/SalesController.java`

### 생성된 파일
1. `ordering/src/main/java/com/careup/ordering/domain/order/dto/response/SalesForecastResponseDto.java`

### 기존 branch 모듈 활용
- `branch/src/main/java/com/careup/branch/domain/branch/controller/SalesForecastController.java`
- `branch/src/main/java/com/careup/branch/domain/branch/service/SalesForecastService.java`
- `branch/src/main/java/com/careup/branch/domain/branch/entity/SalesForecast.java`

## 향후 개선 사항
1. 캐싱 전략 적용: 자주 조회되는 예상 매출액 데이터를 Redis에 캐싱
2. 실시간 알림: 예상 매출액 갱신 시 지점 관리자에게 알림
3. 예측 정확도 개선: 머신러닝 모델 적용 고려
4. API 문서화: Swagger/OpenAPI 명세 추가

## 결론
ordering 모듈의 예상 매출액 조회 기능을 branch 모듈과 MSA 방식으로 연동하여 다음과 같은 개선을 달성했습니다:
- ✅ 지점 관리자의 소속 지점 예상 매출액 조회 기능 구현
- ✅ branch 모듈의 SalesForecast 엔티티 활용
- ✅ Feign Client를 통한 MSA 통신 방식 적용
- ✅ 장애 대응을 위한 Fallback 로직 구현
- ✅ Controller-Service-Repository 구조 준수
- ✅ DTO 패턴 적용

