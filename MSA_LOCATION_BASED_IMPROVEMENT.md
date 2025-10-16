# MSA 구조로 개선된 매출 관리 기능 - 위치 기반 인근 지점 조회

## 개요
매출 관리 기능을 MSA 구조에 맞게 개선하여, branch 서비스와 ordering 서비스가 Feign Client를 통해 통신하도록 변경했습니다. 특히 **위치 기반(latitude, longitude)으로 인근 지점을 자동 조회**하는 기능을 추가했습니다.

---

## 🔄 주요 개선 사항

### 1. **MSA 통신 구조 도입**
- **기존**: ordering 서비스에서 branchId만으로 "Branch-{id}" 형식의 임시 이름 사용
- **개선**: branch 서비스에서 실제 Branch 정보를 조회하여 사용

### 2. **위치 기반 인근 지점 조회**
- **기존**: 수동으로 nearbyBranchIds 목록을 파라미터로 전달
- **개선**: latitude, longitude를 활용한 **Haversine 공식**으로 자동 인근 지점 조회

### 3. **거리 계산 알고리즘**
- Haversine 공식을 사용하여 두 지점 간 실제 거리(km) 계산
- 지정된 반경(radiusKm) 내의 지점만 자동 필터링
- 거리순으로 자동 정렬

---

## 📂 새로 추가된 파일

### branch 서비스

#### DTO
```
dto/branch/
├── BranchSimpleDto.java        # ordering 서비스용 간단한 Branch 정보
└── NearbyBranchDto.java        # 인근 지점 정보 (거리 포함)
```

#### API 추가
- `GET /branch/list-by-ids`: Branch ID 목록으로 정보 조회
- `GET /branch/{branchId}/nearby`: 위치 기반 인근 지점 조회

### ordering 서비스

#### Feign Client
```
common/client/
└── BranchClient.java            # branch 서비스 통신용 Feign Client
```

#### DTO
```
dto/
├── BranchInfoDto.java           # Branch 정보
└── NearbyBranchInfoDto.java     # 인근 지점 정보
```

---

## 🛠 수정된 기능

### 1. 인근 지점 매출 비교 API (ordering 서비스)

#### 기존 API
```http
GET /sales/comparison?branchId=1&nearbyBranchIds=2,3,4&startDate=2025-01-01&endDate=2025-01-31
```
- 수동으로 비교할 지점 ID를 지정해야 했음

#### 개선된 API
```http
GET /sales/comparison?branchId=1&startDate=2025-01-01&endDate=2025-01-31&radiusKm=10
```
- `radiusKm` 파라미터로 반경을 지정하면 자동으로 인근 지점 조회
- branch 서비스와 통신하여 실제 Branch 이름 표시
- 위치 기반으로 정확한 인근 지점 분석

#### 응답 예시
```json
[
  {
    "branchId": 1,
    "branchName": "강남점",
    "totalSales": 5000000,
    "totalOrders": 150,
    "averageOrderAmount": 33333,
    "salesGrowthRate": 15.5
  },
  {
    "branchId": 3,
    "branchName": "서초점",
    "totalSales": 4500000,
    "totalOrders": 140,
    "averageOrderAmount": 32142,
    "salesGrowthRate": 12.3
  }
]
```

---

## 🗺️ 위치 기반 인근 지점 조회 원리

### Haversine 공식
두 지점 간의 실제 거리를 계산하는 공식으로, 지구의 곡률을 고려합니다.

```java
/**
 * Haversine 공식을 사용한 두 지점 간 거리 계산 (km)
 */
private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
    final int EARTH_RADIUS = 6371; // 지구 반경 (km)
    
    double latDistance = Math.toRadians(lat2 - lat1);
    double lonDistance = Math.toRadians(lon2 - lon1);
    
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
    
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    
    return EARTH_RADIUS * c;
}
```

### 처리 과정
1. **대상 지점 확인**: branchId의 latitude, longitude 조회
2. **모든 지점 조회**: 위치 정보가 있는 모든 지점 가져오기
3. **거리 계산**: 각 지점과의 거리를 Haversine 공식으로 계산
4. **필터링**: 지정된 반경(radiusKm) 내의 지점만 선택
5. **정렬**: 거리순으로 정렬 (가까운 순)

---

## 🔄 MSA 통신 흐름

### 인근 지점 매출 비교 플로우
```
[사용자]
   ↓
   GET /sales/comparison?branchId=1&radiusKm=10
   ↓
[ordering 서비스 - SalesController]
   ↓
[ordering 서비스 - SalesService]
   ↓ (1) 인근 지점 조회 요청
[BranchClient (Feign)]
   ↓ HTTP Request
   GET /branch/1/nearby?radiusKm=10
   ↓
[branch 서비스 - BranchController]
   ↓
[branch 서비스 - BranchService]
   ├─ (2) Branch 조회
   ├─ (3) 모든 지점의 거리 계산
   └─ (4) 반경 내 지점 필터링
   ↓
[응답: 인근 지점 목록 + 거리 정보]
   ↓
[ordering 서비스 - SalesService]
   ├─ (5) Branch 이름 조회 (Feign)
   ├─ (6) 각 지점의 매출 통계 계산
   └─ (7) 비교 데이터 생성
   ↓
[응답: 매출 비교 결과]
```

---

## 🎯 사용 예시

### 1. 인근 10km 이내 지점 매출 비교
```bash
GET /sales/comparison?branchId=1&startDate=2025-01-01&endDate=2025-01-31&radiusKm=10
```
→ 1번 지점 기준 10km 이내의 모든 지점과 매출 비교

### 2. 인근 5km 이내 지점 매출 비교
```bash
GET /sales/comparison?branchId=1&startDate=2025-01-01&endDate=2025-01-31&radiusKm=5
```
→ 더 가까운 지점들만 비교

### 3. branch 서비스에서 직접 인근 지점 조회
```bash
GET /branch/1/nearby?radiusKm=10
```
→ 1번 지점 기준 10km 이내 지점 목록 + 거리 정보

**응답 예시**:
```json
[
  {
    "id": 3,
    "name": "서초점",
    "address": "서울시 서초구 서초대로 123",
    "latitude": 37.4833,
    "longitude": 127.0322,
    "distance": 2.5
  },
  {
    "id": 5,
    "name": "역삼점",
    "address": "서울시 강남구 역삼로 456",
    "latitude": 37.5000,
    "longitude": 127.0364,
    "distance": 4.8
  }
]
```

---

## 📊 Branch 엔티티 활용

### latitude, longitude 필드
```java
@Entity
public class Branch {
    // ...
    
    @Column
    private Double latitude;   // 위도
    
    @Column
    private Double longitude;  // 경도
    
    @Column(nullable = false)
    private String address;    // 주소
    
    // ...
}
```

### 위치 정보 필수 조건
- `latitude`, `longitude`가 **모두 있는 지점**만 인근 지점 조회 가능
- 위치 정보가 없는 지점은 자동으로 제외됨
- 위치 정보가 없는 지점으로 조회 시 예외 발생

---

## 🛡️ 에러 처리

### 1. 위치 정보 없음
```json
{
  "status_code": 400,
  "status_message": "해당 지점의 위치 정보가 없습니다."
}
```

### 2. Feign 통신 실패
- ordering 서비스는 자동으로 **본인 지점만 포함한 결과** 반환
- 서비스 중단 없이 계속 진행
- 로그에 에러 기록

### 3. Branch 조회 실패
- Branch 이름이 조회되지 않으면 "Branch-{id}" 형식으로 표시
- 매출 비교 기능은 정상 작동

---

## 🚀 성능 최적화

### 1. 거리 계산 최적화
- 위치 정보가 있는 지점만 대상으로 계산
- 반경 필터링으로 불필요한 데이터 제외

### 2. Feign 통신 최적화
- Branch 정보는 한 번에 일괄 조회 (`/branch/list-by-ids`)
- 개별 API 호출 대신 배치 조회로 통신 횟수 최소화

### 3. 캐싱 고려 사항
- Branch 정보는 자주 변경되지 않으므로 캐싱 권장
- Spring Cache 또는 Redis 활용 가능

---

## 📈 향후 개선 사항

### 1. 지역별 그룹화
```java
// 시/구 단위로 그룹화
GET /sales/comparison-by-region?city=서울&district=강남구
```

### 2. 더 정교한 거리 계산
- 도로 거리 (실제 이동 거리) 계산
- Google Maps API 또는 Kakao Maps API 연동

### 3. 지도 시각화
- 지점 위치와 매출을 지도에 표시
- 히트맵으로 매출 밀집 지역 표시

### 4. 동적 반경 조정
```java
// 최소 N개 지점이 포함되도록 반경 자동 조정
GET /sales/comparison?branchId=1&minBranches=5
```

### 5. 위치 기반 추천
- 매출이 높은 지역 근처에 신규 지점 추천
- 경쟁이 적은 지역 분석

---

## 🎓 기술적 세부사항

### Haversine 공식의 정확도
- **오차 범위**: 약 0.5% 이내
- **적용 거리**: 수백 km까지 정확
- **장점**: 계산이 빠르고 간단함
- **단점**: 지구를 완전한 구로 가정 (실제는 타원체)

### 더 정확한 계산이 필요한 경우
- **Vincenty 공식**: 오차 범위 약 0.5mm (매우 정확, 계산 복잡)
- **대안**: 외부 API 사용 (Google Distance Matrix API 등)

---

## 🔧 설정 필요 사항

### application.yml (ordering 서비스)
```yaml
feign:
  branch:
    url: http://localhost:8081  # branch 서비스 URL
```

### application.yml (branch 서비스)
```yaml
server:
  port: 8081
```

---

## ✅ 테스트 체크리스트

### branch 서비스
- [ ] `/branch/list-by-ids` API 테스트
- [ ] `/branch/{branchId}/nearby` API 테스트
- [ ] 위치 정보 없는 지점 예외 처리 테스트
- [ ] 거리 계산 정확도 검증

### ordering 서비스
- [ ] `/sales/comparison` API 테스트 (radiusKm 사용)
- [ ] Feign Client 통신 테스트
- [ ] Feign 통신 실패 시 폴백 동작 테스트
- [ ] Branch 이름 조회 테스트

### 통합 테스트
- [ ] branch + ordering 서비스 연동 테스트
- [ ] 10km, 5km, 20km 등 다양한 반경 테스트
- [ ] 실제 지점 데이터로 거리 계산 검증

---

## 📝 요약

### 개선 전
```
❌ 수동으로 nearbyBranchIds 지정
❌ Branch 이름이 "Branch-{id}" 형식
❌ 실제 거리와 무관한 비교
```

### 개선 후
```
✅ 위치 기반 자동 인근 지점 조회
✅ 실제 Branch 이름 표시
✅ Haversine 공식으로 정확한 거리 계산
✅ MSA 구조에 맞는 서비스 분리
✅ Feign Client를 통한 안정적인 통신
```

이제 매출 관리 기능이 **진정한 MSA 구조**를 갖추게 되었고, **위치 기반 분석**이 가능해졌습니다! 🎉

