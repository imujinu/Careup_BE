package com.careup.branch.domain.branch.dto.branch;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.OwnershipType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchRegisterReqDto {

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
    private Integer geofenceRadius; // 출퇴근 가능 반경

    private String remark; // 비고

    @Length(max = 100, message = "대리인명은 100자를 넘을 수 없습니다.")
    private String attorneyName; // 대리인명

    @Length(max = 30, message = "대리인 연락처은 30자를 넘을 수 없습니다.")
    private String attorneyPhoneNumber; // 대리인 연락처

    // 위/경도(선택)
    @DecimalMin(value = "-90.0", message = "위도는 -90 ~ 90 범위여야 합니다.")
    @DecimalMax(value = "90.0",  message = "위도는 -90 ~ 90 범위여야 합니다.")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "경도는 -180 ~ 180 범위여야 합니다.")
    @DecimalMax(value = "180.0",  message = "경도는 -180 ~ 180 범위여야 합니다.")
    private Double longitude;

    // DTO -> Entity
    public Branch toEntity() {
        return Branch.builder()
                .name(this.name)
                .businessDomain(this.businessDomain)
                .ownershipType(this.ownershipType)
                .openDate(this.openDate)
                .businessNumber(this.businessNumber)
                .corporationNumber(this.corporationNumber)
                .zipcode(this.zipcode)
                .address(this.address)
                .addressDetail(this.addressDetail)
                .phone(this.phone)
                .email(this.email)
                .geofenceRadius(this.geofenceRadius)
                .remark(this.remark)
                .attorneyName(this.attorneyName)
                .attorneyPhoneNumber(this.attorneyPhoneNumber)
                .latitude(this.latitude)
                .longitude(this.longitude)
                .build();
    }
}
