# 본사 관리자(HQ_ADMIN) 매출 관리 API 명세서

## 개요
본사 관리자(HQ_ADMIN)를 위한 전체 가맹점 매출 관리 및 비교 분석 기능입니다.

## 권한
모든 API는 `@PreAuthorize("hasRole('HQ_ADMIN')")` 적용되어 본사 관리자만 접근 가능합니다.

---

## API 엔드포인트

### 1. 전체 지점 매출 내역 기간별 조회
```
GET /hq/sales/all
```

#### 설명
모든 가맹점의 통합 매출 통계를 기간별로 조회합니다.

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| startDate | LocalDate | O | 시작일 (yyyy-MM-dd) | - |
| endDate | LocalDate | O | 종료일 (yyyy-MM-dd) | - |
| periodType | String | X | 통계 주기 타입 | DAY |

**periodType 값**
- `DAY`: 일별 통계
- `WEEK`: 주별 통계
- `MONTH`: 월별 통계

#### 응답 예시
```json
{
  "periodType": "DAY",
  "totalSales": 50000000,
  "totalOrders": 1500,
  "totalBranchCount": 25,
  "salesData": [
    {
      "date": "2025-01-01",
      "period": "DAY",
      "totalSales": 2000000,
      "totalOrders": 60,
      "averageOrderAmount": 33333,
      "activeBranchCount": 20,
      "averageSalesPerBranch": 100000
    },
    {
      "date": "2025-01-02",
      "period": "DAY",
      "totalSales": 2500000,
      "totalOrders": 75,
      "averageOrderAmount": 33333,
      "activeBranchCount": 22,
      "averageSalesPerBranch": 113636
    }
  ]
}
```

#### 사용 예시
```bash
# 1월 일별 전체 매출 조회
GET /hq/sales/all?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY

# 1분기 월별 전체 매출 조회
GET /hq/sales/all?startDate=2025-01-01&endDate=2025-03-31&periodType=MONTH
```

---

### 2. 선택한 가맹점의 매출 내역 기간별 조회
```
GET /hq/sales/branch/{branchId}
```

#### 설명
특정 가맹점의 상세 매출 정보를 조회하며, 전체 대비 점유율과 순위를 제공합니다.

#### 경로 파라미터
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| branchId | Long | O | 조회할 지점 ID |

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| startDate | LocalDate | O | 시작일 (yyyy-MM-dd) | - |
| endDate | LocalDate | O | 종료일 (yyyy-MM-dd) | - |
| periodType | String | X | 통계 주기 타입 | DAY |

#### 응답 예시
```json
{
  "branchId": 1,
  "branchName": "Branch-1",
  "periodType": "DAY",
  "totalSales": 5000000,
  "totalOrders": 150,
  "marketShare": 10.0,
  "ranking": 3,
  "salesData": [
    {
      "branchId": 1,
      "branchName": "Branch-1",
      "date": "2025-01-01",
      "period": "DAY",
      "totalSales": 150000,
      "totalOrders": 5,
      "averageOrderAmount": 30000,
      "marketShare": null,
      "ranking": null
    }
  ]
}
```

#### 주요 필드 설명
- **marketShare**: 전체 가맹점 매출 대비 해당 지점의 점유율 (%)
- **ranking**: 전체 가맹점 중 매출 순위

#### 사용 예시
```bash
# 1번 지점의 1월 일별 매출 조회
GET /hq/sales/branch/1?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY

# 5번 지점의 주별 매출 조회
GET /hq/sales/branch/5?startDate=2025-01-01&endDate=2025-01-31&periodType=WEEK
```

---

### 3. 가맹점 간 매출 비교
```
GET /hq/sales/comparison
```

#### 설명
선택한 여러 가맹점의 매출을 비교 분석합니다. 상대적 성과를 파악하는데 유용합니다.

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| branchIds | List<Long> | O | 비교할 지점 ID 목록 (콤마 구분) | - |
| startDate | LocalDate | O | 시작일 (yyyy-MM-dd) | - |
| endDate | LocalDate | O | 종료일 (yyyy-MM-dd) | - |
| periodType | String | X | 통계 주기 타입 | DAY |

#### 응답 예시
```json
{
  "periodType": "DAY",
  "branchIds": [1, 2, 3],
  "totalSales": 15000000,
  "branchNames": {
    "1": "Branch-1",
    "2": "Branch-2",
    "3": "Branch-3"
  },
  "comparisonData": [
    {
      "branchId": 1,
      "branchName": "Branch-1",
      "date": "2025-01-01",
      "period": "DAY",
      "totalSales": 6000000,
      "totalOrders": 180,
      "averageOrderAmount": 33333,
      "marketShare": 40.0,
      "ranking": 1
    },
    {
      "branchId": 2,
      "branchName": "Branch-2",
      "date": "2025-01-01",
      "period": "DAY",
      "totalSales": 5500000,
      "totalOrders": 165,
      "averageOrderAmount": 33333,
      "marketShare": 36.67,
      "ranking": 2
    },
    {
      "branchId": 3,
      "branchName": "Branch-3",
      "date": "2025-01-01",
      "period": "DAY",
      "totalSales": 3500000,
      "totalOrders": 105,
      "averageOrderAmount": 33333,
      "marketShare": 23.33,
      "ranking": 3
    }
  ]
}
```

#### 주요 필드 설명
- **totalSales**: 선택한 지점들의 총 매출 합계
- **marketShare**: 선택한 지점들 내에서의 점유율 (%)
- **ranking**: 선택한 지점들 내에서의 매출 순위
- **branchNames**: 지점 ID와 지점명 매핑 정보

#### 사용 예시
```bash
# 3개 지점 비교
GET /hq/sales/comparison?branchIds=1,2,3&startDate=2025-01-01&endDate=2025-01-31&periodType=DAY

# 5개 지점 월별 비교
GET /hq/sales/comparison?branchIds=1,2,3,4,5&startDate=2025-01-01&endDate=2025-03-31&periodType=MONTH
```

---

## 구현 구조

### Controller
- **HqSalesController**: 본사 관리자용 REST API 엔드포인트
  - `@PreAuthorize("hasRole('HQ_ADMIN')")` 적용으로 권한 제어

### Service
- **HqSalesService**: 본사 관리자용 비즈니스 로직
  - 전체 지점 매출 통계 계산
  - 선택한 지점 상세 분석 (점유율, 순위)
  - 가맹점 간 매출 비교

### Repository
- **OrderRepository**: 본사 관리자용 쿼리 메서드 추가
  - `calculateTotalSalesAllBranches()`: 전체 지점 총 매출
  - `countOrdersAllBranches()`: 전체 지점 총 주문 수
  - `findAllByOrderStatusAndCreatedAtBetween()`: 전체 지점 주문 조회
  - `countActiveBranches()`: 활성 지점 수
  - `findBranchSalesStatistics()`: 지점별 매출 통계
  - `findBranchSalesComparison()`: 선택한 지점들의 매출 비교

### DTO
#### Request
- `HqSalesRequest`: 매출 조회 요청

#### Response
- `AllBranchesSalesResponse`: 전체 지점 매출 응답
- `BranchSalesDetailResponse`: 특정 지점 상세 응답
- `BranchComparisonResponse`: 지점 비교 응답

#### Data
- `AllBranchesSalesDto`: 전체 지점 매출 데이터
- `BranchSalesDetailDto`: 지점별 상세 데이터

---

## 주요 기능 특징

### 1. 전체 지점 통합 관리
- 모든 가맹점의 매출을 통합하여 조회
- 일별/주별/월별 통계 제공
- 활성 지점 수 및 지점당 평균 매출 자동 계산

### 2. 지점별 상세 분석
- 특정 지점의 매출 상세 정보
- 전체 대비 시장 점유율 자동 계산
- 매출 순위 제공으로 성과 파악 용이

### 3. 지점 간 비교 분석
- 여러 지점의 매출을 동시에 비교
- 상대적 점유율과 순위 제공
- 매출 순위별로 자동 정렬

### 4. 보안
- 모든 API에 `@PreAuthorize("hasRole('HQ_ADMIN')")` 적용
- 본사 관리자만 접근 가능

---

## 데이터 계산 로직

### 점유율 계산
```
점유율(%) = (해당 지점 매출 / 전체 지점 매출) × 100
```

### 순위 계산
- 매출액 기준 내림차순 정렬
- 1위부터 순차적으로 순위 부여

### 평균 계산
```
지점당 평균 매출 = 총 매출 / 활성 지점 수
평균 주문 금액 = 총 매출 / 총 주문 수
```

---

## 사용 시나리오

### 시나리오 1: 월별 전체 매출 현황 파악
```bash
GET /hq/sales/all?startDate=2025-01-01&endDate=2025-12-31&periodType=MONTH
```
→ 연간 월별 매출 추이를 파악하여 성수기/비수기 분석

### 시나리오 2: 우수 지점 분석
```bash
GET /hq/sales/branch/1?startDate=2025-01-01&endDate=2025-01-31&periodType=DAY
```
→ 1위 지점의 일별 매출 패턴 분석

### 시나리오 3: 지역별 지점 비교
```bash
GET /hq/sales/comparison?branchIds=1,2,3,4,5&startDate=2025-01-01&endDate=2025-01-31&periodType=WEEK
```
→ 같은 지역 지점들의 주별 매출 비교

---

## 향후 개선 사항

1. **지역별 그룹 분석**: 지역별로 그룹화하여 지역 간 비교
2. **성장률 분석**: 전월 대비, 전년 대비 성장률 자동 계산
3. **목표 대비 달성률**: 지점별 매출 목표 대비 달성률 표시
4. **실시간 대시보드**: WebSocket을 통한 실시간 매출 현황
5. **엑셀 리포트**: 통계 데이터 Excel 다운로드 기능
6. **알림 기능**: 특정 지점의 매출 급감/급증 시 알림
7. **Branch 서비스 연동**: 실제 지점명 조회 (현재는 "Branch-{id}" 형식)

---

## 주의사항

1. **권한 체크**: `HQ_ADMIN` 권한이 있는 사용자만 접근 가능
2. **대용량 데이터**: 장기간 조회 시 성능 고려 필요
3. **캐싱 전략**: 반복적인 조회는 캐싱 적용 권장
4. **MSA 통신**: 실제 운영 시 Branch 서비스와 통신하여 지점명 조회 필요

