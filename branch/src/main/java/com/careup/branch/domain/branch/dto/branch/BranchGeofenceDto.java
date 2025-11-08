package com.careup.branch.domain.branch.dto.branch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 지점 지오펜스 간단 응답 DTO
 * - enabled: 유효한 지오펜스(위/경도 존재 + radius>0) 여부
 * - latitude/longitude/radius: 지도/검증 용도
 * - lat/lng 별칭을 함께 제공해 프론트의 다양한 키 매핑을 지원
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchGeofenceDto {
    private Double latitude;
    private Double longitude;
    private Integer radius;
    private boolean enabled;

    @JsonProperty("lat")
    public Double getLat() {
        return latitude;
    }

    @JsonProperty("lng")
    public Double getLng() {
        return longitude;
    }
}
