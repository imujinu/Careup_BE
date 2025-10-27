package com.careup.branch.domain.branch.dto.branch;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchStatus;
import com.careup.branch.domain.branch.entity.OwnershipType;
import lombok.*;

import java.time.LocalDate;

/**
 * 지점 상세 조회 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDto {

    private Long id;
    private String name;
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

    public static BranchDto fromEntity(Branch branch) {
        return BranchDto.builder()
                .id(branch.getId())
                .name(branch.getName())
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
                .build();
    }
}
