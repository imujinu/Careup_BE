package com.careup.branch.domain.branch.dto;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchStatus;
import com.careup.branch.domain.branch.entity.OwnershipType;
import lombok.*;

import java.time.LocalDate;

/**
 * 지점 상세 조회 DTO
 * 해당 지점 상세 조회 및 수정에 사용
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class BranchDetailDto {

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

    // Entity -> DTO
    public static BranchDetailDto fromEntity(Branch branch) {
        return BranchDetailDto.builder()
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
                .build();
    }
}
