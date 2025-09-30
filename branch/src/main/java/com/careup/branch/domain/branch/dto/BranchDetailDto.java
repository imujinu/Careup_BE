package com.careup.branch.domain.branch.dto;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchStatus;
import com.careup.branch.domain.branch.entity.OwnershipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
