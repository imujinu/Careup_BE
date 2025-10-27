package com.careup.branch.domain.branch.dto.branch;

import com.careup.branch.domain.branch.entity.OwnershipType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchUpdateDto {

    @NotBlank(message = "지점명은 필수입니다.")
    private String name; // 지점명

    @NotBlank(message = "업종은 필수입니다.")
    private String businessDomain; // 업종

    @NotNull(message = "직영여부는 필수입니다.")
    private OwnershipType ownershipType; // 직영 여부 (YES, NO)

    @NotNull(message = "개업연월은 필수입니다.")
    private LocalDate openDate; // 개업연월

    @NotBlank(message = "사업자등록번호는 필수입니다.")
    private String businessNumber; // 사업자등록번호

    @NotBlank(message = "법인등록번호는 필수입니다.")
    private String corporationNumber; // 법인등록번호

    @NotBlank(message = "지점 우편번호는 필수입니다.")
    private String zipcode; // 지점 우편번호

    @NotBlank(message = "주소는 필수입니다.")
    private String address; // 지점 주소

    private String addressDetail; // 지점 상세 주소

    @NotBlank(message = "지점 전화번호는 필수입니다.")
    private String phone; // 지점 전화번호

    @NotBlank(message = "대표 이메일은 필수입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email; // 대표 이메일

    @NotNull(message = "출퇴근 반경은 필수입니다.")
    private Integer geofenceRadius;

    private String remark;

    // 위/경도(선택)
    @DecimalMin(value = "-90.0", message = "위도는 -90 ~ 90 범위여야 합니다.")
    @DecimalMax(value = "90.0",  message = "위도는 -90 ~ 90 범위여야 합니다.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도는 -180 ~ 180 범위여야 합니다.")
    @DecimalMax(value = "180.0",  message = "경도는 -180 ~ 180 범위여야 합니다.")
    private Double longitude;
}
