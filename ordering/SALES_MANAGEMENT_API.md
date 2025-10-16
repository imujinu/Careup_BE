# 매출 관리 API 명세서

## 개요
지점 관리자(BRANCH_ADMIN, FRANCHISE_OWNER)를 위한 매출 관리 기능입니다.

## 주요 기능
1. **매출 통계 조회**: 요일별, 시간별, 기간별(일/주/월) 매출 통계
2. **상품별 매출 조회**: 마진율 높은 상품, 판매량 많은 상품 분석
3. **지점 간 매출 비교**: 인근 지역 가맹점과의 매출 비교
4. **예상 매출 조회**: 지난 30일 데이터 기반 예상 매출 예측

---

## API 엔드포인트

### 1. 매출 통계 조회
```
GET /sales/statistics
```

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| branchId | Long | O | 지점 ID | - |
| startDate | LocalDate | O | 시작일 (yyyy-MM-dd) | - |
| endDate | LocalDate | O | 종료일 (yyyy-MM-dd) | - |
| periodType | String | X | 통계 주기 타입 | DAY |

**periodType 값**
- `HOUR`: 시간별 통계
- `DAY_OF_WEEK`: 요일별 통계
- `DAY`: 일별 통계
- `WEEK`: 주별 통계
- `MONTH`: 월별 통계

#### 응답 예시
```json
{
  "branchId": 1,
  "periodType": "DAY",
  "totalSales": 5000000,
  "totalOrders": 150,
  "statistics": [
    {
      "date": "2025-01-01",
      "period": "DAY",
      "totalSales": 150000,
      "totalOrders": 5,
      "averageOrderAmount": 30000
    }
  ]
}
```

#### 사용 예시
```bash
# 일별 통계
GET /sales/statistics?branchId=1&startDate=2025-01-01&endDate=2025-01-31&periodType=DAY

# 시간별 통계
GET /sales/statistics?branchId=1&startDate=2025-01-01&endDate=2025-01-01&periodType=HOUR

# 요일별 통계
GET /sales/statistics?branchId=1&startDate=2025-01-01&endDate=2025-01-31&periodType=DAY_OF_WEEK
```

---

### 2. 상품별 매출 조회
```
GET /sales/products
```

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| branchId | Long | O | 지점 ID | - |
| startDate | LocalDate | O | 시작일 (yyyy-MM-dd) | - |
| endDate | LocalDate | O | 종료일 (yyyy-MM-dd) | - |
| sortType | String | X | 정렬 타입 | HIGH_SALES |

**sortType 값**
- `HIGH_MARGIN`: 마진율 높은 순
- `LOW_MARGIN`: 마진율 낮은 순
- `HIGH_SALES`: 매출 높은 순
- `LOW_SALES`: 매출 낮은 순

#### 응답 예시
```json
{
  "branchId": 1,
  "sortType": "HIGH_MARGIN",
  "products": [
    {
      "productId": 10,
      "productName": "프리미엄 음료",
      "totalQuantity": 100,
      "totalSales": 500000,
      "supplyPrice": 2000,
      "averageSellingPrice": 5000,
      "marginRate": 60.0,
      "orderCount": 50
    }
  ]
}
```

#### 사용 예시
```bash
# 마진율 높은 상품 조회
GET /sales/products?branchId=1&startDate=2025-01-01&endDate=2025-01-31&sortType=HIGH_MARGIN

# 매출 높은 상품 조회
GET /sales/products?branchId=1&startDate=2025-01-01&endDate=2025-01-31&sortType=HIGH_SALES
```

---

### 3. 지점 간 매출 비교
```
GET /sales/comparison
```

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| branchId | Long | O | 본인 지점 ID |
| nearbyBranchIds | List<Long> | O | 비교할 인근 지점 ID 목록 (콤마 구분) |
| startDate | LocalDate | O | 시작일 (yyyy-MM-dd) |
| endDate | LocalDate | O | 종료일 (yyyy-MM-dd) |

#### 응답 예시
```json
[
  {
    "branchId": 1,
    "branchName": "Branch-1",
    "totalSales": 5000000,
    "totalOrders": 150,
    "averageOrderAmount": 33333,
    "salesGrowthRate": 15.5
  },
  {
    "branchId": 2,
    "branchName": "Branch-2",
    "totalSales": 4500000,
    "totalOrders": 140,
    "averageOrderAmount": 32142,
    "salesGrowthRate": 12.3
  }
]
```

#### 사용 예시
```bash
GET /sales/comparison?branchId=1&nearbyBranchIds=2,3,4&startDate=2025-01-01&endDate=2025-01-31
```

---

### 4. 예상 매출 조회
```
GET /sales/forecast
```

#### 요청 파라미터
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| branchId | Long | O | 지점 ID |
| targetDate | LocalDate | O | 예측 대상 날짜 (yyyy-MM-dd) |

#### 응답 예시
```json
{
  "branchId": 1,
  "forecastDate": "2025-02-01",
  "expectedSales": 180000,
  "previousPeriodSales": 5000000,
  "growthRate": 8.0,
  "forecastBasis": "지난 30일 평균 매출 기반 + 요일별 가중치"
}
```

#### 예측 로직
- 지난 30일 평균 매출을 기반으로 계산
- 요일별 가중치 적용:
  - 주말(토,일): 1.3배 (30% 증가)
  - 금요일: 1.15배 (15% 증가)
  - 월요일: 0.9배 (10% 감소)
  - 평일: 1.0배 (기본)

#### 사용 예시
```bash
GET /sales/forecast?branchId=1&targetDate=2025-02-01
```

---

## 구현 구조

### Controller
- `SalesController`: REST API 엔드포인트 제공

### Service
- `SalesService`: 비즈니스 로직 처리
  - 요일별, 시간별, 기간별 매출 통계 계산
  - 상품별 매출 및 마진율 분석
  - 지점 간 매출 비교 및 성장률 계산
  - 예상 매출 예측

### Repository
- `OrderRepository`: 주문 데이터 조회
  - 기간별 주문 조회
  - 총 매출액 계산
  - 주문 수 계산
- `OrderedItemRepository`: 주문 상품 데이터 조회
  - 상품별 판매 통계 조회

### DTO
- Request: `SalesStatisticsRequest`
- Response: `SalesStatisticsResponse`, `ProductSalesResponse`
- Data: `SalesStatisticsDto`, `ProductSalesDto`, `BranchComparisonDto`, `SalesForecastDto`

---

## 데이터베이스 엔티티

### Order (주문)
- `id`: 주문 ID
- `branchId`: 지점 ID
- `totalAmount`: 총 주문 금액
- `orderStatus`: 주문 상태 (PENDING, CONFIRMED, CANCELLED)
- `orderType`: 주문 타입 (ONLINE, OFFLINE)
- `createdAt`: 생성 시간

### OrderedItem (주문 상품)
- `id`: 주문 상품 ID
- `order`: 주문 (FK)
- `product`: 상품 (FK)
- `quantity`: 수량
- `unitPrice`: 단가
- `totalPrice`: 총 가격

---

## 주의사항

1. **권한 체크**: 실제 운영 시 해당 지점의 관리자만 조회할 수 있도록 권한 검증 필요
2. **MSA 통신**: 지점명 조회는 Branch 서비스와 통신 필요 (현재는 "Branch-{id}" 형식으로 반환)
3. **성능 최적화**: 대량 데이터 조회 시 캐싱 또는 페이징 처리 고려
4. **예측 정확도**: 현재는 단순 평균 기반 예측이며, 더 정교한 예측을 위해서는 머신러닝 모델 적용 가능

---

## 향후 개선 사항

1. **폐기/반품 상품 분석**: 재고 관리와 연계하여 폐기/반품 많은 상품 추적
2. **실시간 대시보드**: WebSocket을 통한 실시간 매출 현황 제공
3. **더 정교한 예측 모델**: 시계열 분석, 머신러닝 기반 예측
4. **알림 기능**: 매출 목표 달성률, 이상 패턴 감지 시 알림
5. **엑셀 다운로드**: 통계 데이터 Excel 파일로 다운로드 기능

