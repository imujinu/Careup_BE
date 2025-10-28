package com.careup.branch.domain.branch.dto.branch;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchStatus;
import com.careup.branch.domain.branch.entity.OwnershipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 내 소속 지점 조회 DTO (지점 정보 + 점주 정보)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyBranchDto {

    // 지점 정보
    private Long branchId;
    private String branchName;
    private String businessDomain;
    private OwnershipType ownershipType;
    private BranchStatus status;
    private LocalDate openDate;
    private String phone;
    private String businessNumber;
    private String corporationNumber;
    private String zipcode;
    private String address;
    private String addressDetail;
    private String profileImageUrl;
    private String email;
    private Integer geofenceRadius;
    private Double latitude;
    private Double longitude;
    private String remark;
    private String attorneyName;
    private String attorneyPhoneNumber;

    // 점주 정보
    private OwnerInfoDto ownerInfo;

    public static MyBranchDto fromEntity(Branch branch, OwnerInfoDto ownerInfo) {
        return MyBranchDto.builder()
                .branchId(branch.getId())
                .branchName(branch.getName())
                .businessDomain(branch.getBusinessDomain())
                .ownershipType(branch.getOwnershipType())
                .status(branch.getStatus())
                .openDate(branch.getOpenDate())
                .phone(branch.getPhone())
                .businessNumber(branch.getBusinessNumber())
                .corporationNumber(branch.getCorporationNumber())
                .zipcode(branch.getZipcode())
                .address(branch.getAddress())
                .addressDetail(branch.getAddressDetail())
                .profileImageUrl(branch.getProfileImageUrl())
                .email(branch.getEmail())
                .geofenceRadius(branch.getGeofenceRadius())
                .latitude(branch.getLatitude())
                .longitude(branch.getLongitude())
                .remark(branch.getRemark())
                .attorneyName(branch.getAttorneyName())
                .attorneyPhoneNumber(branch.getAttorneyPhoneNumber())
                .ownerInfo(ownerInfo)
                .build();
    }

    /**
     * 점주(Owner) 정보 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OwnerInfoDto {
        private Long employeeId;
        private String employeeNumber;
        private String name;
        private String email;
        private String mobile;
        private String authorityType;
        private String profileImageUrl;
    }
}

