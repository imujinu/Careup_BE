# 예상 매출액 관리 API 명세서

## 개요
branch 모듈의 SalesForecast 엔티티를 활용한 예상 매출액 관리 기능입니다.
ordering 서비스의 실제 매출 데이터를 기반으로 예상 매출을 계산하고 관리합니다.

## MSA 구조
- **branch 서비스**: 예상 매출액 저장 및 조회
- **ordering 서비스**: 실제 매출 통계 제공
- **Feign Client**: 서비스 간 통신

---

## API 엔드포인트

### 1. 지점 관리자용 API

#### 1-1. 소속 가맹점의 예상 매출액 조회
```
GET /sales-forecast/branch/{branchId}
```

**권한**: `@PreAuthorize("hasAnyRole('BRANCH_ADMIN', 'FRANCHISE_OWNER', 'HQ_ADMIN')")`

**설명**: 지점 관리자가 자신의 소속 가맹점의 예상 매출액을 조회합니다.

**경로 파라미터**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| branchId | Long | O | 조회할 지점 ID |

**응답 예시**
```json
{
  "branchId": 1,
  "branchName": "강남점",
  "currentForecast": {
    "id": 10,
    "branchId": 1,
    "branchName": "강남점",
    "amount": 15000000,
    "periodStart": "2025-01-01",
    "periodEnd": "2025-01-31",
    "createdAt": "2024-12-25T10:00:00",
    "forecastBasis": "지난 30일 평균 매출 기반 + 요일별 가중치"
  },
  "forecastHistory": [
    {
      "id": 10,
      "branchId": 1,
      "branchName": "강남점",
      "amount": 15000000,
      "periodStart": "2025-01-01",
      "periodEnd": "2025-01-31",
      "createdAt": "2024-12-25T10:00:00",
      "forecastBasis": "저장된 예상 매출"
    },
    {
      "id": 9,
      "branchId": 1,
      "branchName": "강남점",
      "amount": 14000000,
      "periodStart": "2024-12-01",
      "periodEnd": "2024-12-31",
      "createdAt": "2024-11-25T10:00:00",
      "forecastBasis": "저장된 예상 매출"
    }
  ]
}
```

**사용 예시**
```bash
GET /sales-forecast/branch/1
```

---

### 2. 본사 관리자용 API

#### 2-1. 특정 지점의 예상 매출액 계산
```
GET /sales-forecast/calculate/{branchId}?forecastDays=30
```

**권한**: `@PreAuthorize("hasRole('HQ_ADMIN')")`

**설명**: ordering 서비스의 실제 매출 데이터를 기반으로 예상 매출을 계산합니다.

**경로 파라미터**
| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| branchId | Long | O | 계산할 지점 ID |

**요청 파라미터**
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| forecastDays | Integer | X | 예측 기간 (일수) | 30 |

**응답 예시**
```json
{
  "id": null,
  "branchId": 1,
  "branchName": "강남점",
  "amount": 15000000,
  "periodStart": "2025-01-01",
  "periodEnd": "2025-01-31",
  "createdAt": null,
  "forecastBasis": "지난 30일 평균 매출 기반 + 요일별 가중치"
}
```

**계산 로직**
1. ordering 서비스에서 지난 30일 매출 통계 조회
2. 일평균 매출 계산
3. 예측 기간의 요일별 가중치 적용
   - 주말(토,일): 1.3배 (30% 증가)
   - 금요일: 1.15배 (15% 증가)
   - 월요일: 0.9배 (10% 감소)
   - 평일: 1.0배 (기본)
4. 총 예상 매출액 산출

**사용 예시**
```bash
# 30일 예상 매출 계산
GET /sales-forecast/calculate/1?forecastDays=30

# 90일 예상 매출 계산
GET /sales-forecast/calculate/1?forecastDays=90
```

---

#### 2-2. 가맹점의 예상 매출액 전송 (단일)
```
POST /sales-forecast
```

**권한**: `@PreAuthorize("hasRole('HQ_ADMIN')")`

**설명**: 계산된 예상 매출액을 데이터베이스에 저장합니다.

**요청 본문**
```json
{
  "branchId": 1,
  "amount": 15000000,
  "periodStart": "2025-01-01",
  "periodEnd": "2025-01-31"
}
```

**응답 예시**
```json
{
  "id": 10,
  "branchId": 1,
  "branchName": "강남점",
  "amount": 15000000,
  "message": "예상 매출액이 성공적으로 등록되었습니다."
}
```

**사용 예시**
```bash
POST /sales-forecast
Content-Type: application/json

{
  "branchId": 1,
  "amount": 15000000,
  "periodStart": "2025-01-01",
  "periodEnd": "2025-01-31"
}
```

---

#### 2-3. 여러 가맹점의 예상 매출액 일괄 전송
```
POST /sales-forecast/bulk
```

**권한**: `@PreAuthorize("hasRole('HQ_ADMIN')")`

**설명**: 여러 가맹점의 예상 매출액을 한 번에 저장합니다.

**요청 본문**
```json
{
  "forecasts": [
    {
      "branchId": 1,
      "amount": 15000000,
      "periodStart": "2025-01-01",
      "periodEnd": "2025-01-31"
    },
    {
      "branchId": 2,
      "amount": 12000000,
      "periodStart": "2025-01-01",
      "periodEnd": "2025-01-31"
    },
    {
      "branchId": 3,
      "amount": 18000000,
      "periodStart": "2025-01-01",
      "periodEnd": "2025-01-31"
    }
  ]
}
```

**응답 예시**
```json
[
  {
    "id": 10,
    "branchId": 1,
    "branchName": "강남점",
    "amount": 15000000,
    "message": "예상 매출액이 성공적으로 등록되었습니다."
  },
  {
    "id": 11,
    "branchId": 2,
    "branchName": "서초점",
    "amount": 12000000,
    "message": "예상 매출액이 성공적으로 등록되었습니다."
  },
  {
    "id": 12,
    "branchId": 3,
    "branchName": "역삼점",
    "amount": 18000000,
    "message": "예상 매출액이 성공적으로 등록되었습니다."
  }
]
```

---

#### 2-4. 모든 지점의 예상 매출액 자동 계산 및 전송
```
POST /sales-forecast/calculate-all?forecastDays=30
```

**권한**: `@PreAuthorize("hasRole('HQ_ADMIN')")`

**설명**: 모든 가맹점의 예상 매출을 자동으로 계산하고 저장합니다.

**요청 파라미터**
| 파라미터 | 타입 | 필수 | 설명 | 기본값 |
|---------|------|------|------|--------|
| forecastDays | Integer | X | 예측 기간 (일수) | 30 |

**응답 예시**
```json
[
  {
    "id": 10,
    "branchId": 1,
    "branchName": "강남점",
    "amount": 15000000,
    "message": "예상 매출액이 성공적으로 등록되었습니다."
  },
  {
    "id": 11,
    "branchId": 2,
    "branchName": "서초점",
    "amount": 12000000,
    "message": "예상 매출액이 성공적으로 등록되었습니다."
  },
  {
    "id": null,
    "branchId": 3,
    "branchName": "역삼점",
    "message": "예상 매출액 자동 생성 실패: Feign 통신 오류"
  }
]
```

**사용 예시**
```bash
# 모든 지점의 30일 예상 매출 자동 생성
POST /sales-forecast/calculate-all?forecastDays=30

# 모든 지점의 90일 예상 매출 자동 생성
POST /sales-forecast/calculate-all?forecastDays=90
```

---

## 구현 구조

### branch 모듈

#### Controller
- **SalesForecastController**: 예상 매출액 관리 REST API

#### Service
- **SalesForecastService**: 예상 매출액 비즈니스 로직
  - 예상 매출액 조회
  - ordering 서비스와 통신하여 예상 매출 계산
  - 예상 매출액 저장 (단일/일괄)
  - 모든 지점 자동 계산 및 저장

#### Repository
- **SalesForecastRepository**: 예상 매출액 데이터 접근
  - 지점별 예상 매출 조회
  - 기간별 예상 매출 조회
  - 최신 예상 매출 조회

#### DTO
**Request**
- `SalesForecastRequest`: 예상 매출 등록 요청
- `BulkSalesForecastRequest`: 일괄 등록 요청

**Response**
- `SalesForecastResponse`: 예상 매출 조회 응답
- `SalesForecastCreateResponse`: 예상 매출 생성 응답

**Data**
- `SalesForecastDto`: 예상 매출 데이터

#### Feign Client
- **OrderingSalesClient**: ordering 서비스 통신
  - `getSalesStatisticsForForecast()`: 매출 통계 조회

---

### ordering 모듈

#### Controller (추가)
- **SalesController**: 매출 통계 제공 API
  - `GET /sales/statistics-for-forecast`: branch 서비스용 매출 통계 조회

#### Service (추가)
- **SalesService**: 매출 통계 계산
  - `getSalesStatisticsForForecast()`: 예상 매출 계산용 통계

---

## 데이터베이스 구조

### SalesForecast 엔티티
```java
@Entity
public class SalesForecast extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;
    
    @Column(nullable = false)
    private Long amount; // 예상 매출액
    
    @Column(name = "period_start", nullable = false)
    private Date periodStart; // 시작 기간
    
    @Column(name = "period_end", nullable = false)
    private Date period_end; // 종료 기간
}
```

---

## MSA 통신 흐름

### 예상 매출 계산 플로우
```
1. HQ_ADMIN이 예상 매출 계산 요청
   ↓
2. branch 서비스 → OrderingSalesClient (Feign)
   ↓
3. ordering 서비스의 /sales/statistics-for-forecast 호출
   ↓
4. ordering 서비스에서 지난 30일 매출 통계 계산
   - 총 매출액
   - 총 주문 수
   - 일평균 매출
   ↓
5. branch 서비스에서 통계 데이터 수신
   ↓
6. 요일별 가중치 적용하여 예상 매출 계산
   ↓
7. 결과 반환
```

### 예상 매출 저장 플로우
```
1. HQ_ADMIN이 예상 매출 저장 요청
   ↓
2. branch 서비스 SalesForecastService
   ↓
3. SalesForecast 엔티티 생성
   ↓
4. SalesForecastRepository.save()
   ↓
5. 데이터베이스에 저장
   ↓
6. 결과 반환
```

---

## 사용 시나리오

### 시나리오 1: 지점 관리자가 자신의 예상 매출 확인
```bash
# 지점 관리자 (branchId: 1)
GET /sales-forecast/branch/1
Authorization: Bearer {token}
```
→ 현재 적용 중인 예상 매출과 이력 조회

---

### 시나리오 2: 본사 관리자가 특정 지점의 예상 매출 계산
```bash
# 1단계: 예상 매출 계산
GET /sales-forecast/calculate/1?forecastDays=30
Authorization: Bearer {token}

# 2단계: 계산된 값 확인 후 저장
POST /sales-forecast
Authorization: Bearer {token}
Content-Type: application/json

{
  "branchId": 1,
  "amount": 15000000,
  "periodStart": "2025-01-01",
  "periodEnd": "2025-01-31"
}
```

---

### 시나리오 3: 본사 관리자가 모든 지점의 예상 매출 일괄 생성
```bash
POST /sales-forecast/calculate-all?forecastDays=30
Authorization: Bearer {token}
```
→ 모든 지점의 예상 매출이 자동으로 계산되어 저장됨

---

## 주요 기능 특징

### 1. 실제 매출 기반 예측
- ordering 서비스의 실제 매출 데이터 활용
- 지난 30일 평균 매출 기반 계산
- 신뢰성 있는 예측 제공

### 2. 요일별 가중치 적용
- 주말/평일에 따른 매출 패턴 반영
- 더 정확한 예측 가능

### 3. MSA 구조
- branch와 ordering 서비스 분리
- Feign Client를 통한 서비스 간 통신
- 독립적인 서비스 운영

### 4. 유연한 관리
- 단일 지점 계산/저장
- 여러 지점 일괄 저장
- 전체 지점 자동 생성

### 5. 이력 관리
- 과거 예상 매출 이력 조회
- 예측 정확도 분석 가능

---

## 에러 처리

### Feign 통신 실패 시
- 기본값 반환 (이전 예상 매출 또는 100만원)
- 에러 로그 기록
- 서비스 중단 방지

### 지점 없음
```json
{
  "message": "존재하지 않는 지점입니다."
}
```

---

## 향후 개선 사항

1. **머신러닝 기반 예측**: 더 정교한 예측 모델 적용
2. **계절성 반영**: 계절별, 월별 패턴 분석
3. **외부 요인 반영**: 날씨, 이벤트 등 고려
4. **예측 정확도 평가**: 실제 매출과 비교 분석
5. **알림 기능**: 예상 매출 대비 실제 매출 차이 알림
6. **대시보드**: 시각화된 예상 매출 현황

