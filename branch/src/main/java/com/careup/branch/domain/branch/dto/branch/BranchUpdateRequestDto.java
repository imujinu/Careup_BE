package com.careup.branch.domain.branch.dto.branch;

import com.careup.branch.domain.branch.entity.BranchUpdateRequest;
import com.careup.branch.domain.branch.entity.OwnershipType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지점 정보 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchUpdateRequestDto {

    private Long id;
    private Long branchId; // 지점 ID
    private String branchName; // 원본 지점명 (변경 전)
    private Long requesterId; // 요청자 ID
    private String requesterName; // 요청자 이름
    private String name; // 지점명
    private String businessDomain; // 업종
    private OwnershipType ownershipType; // 직영 여부
    private LocalDate openDate; // 개업연월
    private String businessNumber; // 사업자등록번호
    private String corporationNumber; // 법인등록번호
    private String zipcode; // 우편번호
    private String address; // 주소
    private String addressDetail; // 상세 주소
    private String phone; // 전화번호
    private String email; // 이메일
    private Double latitude; // 위도
    private Double longitude; // 경도
    private Integer geofenceRadius; // 출퇴근 가능 반경(미터)
    private String remark; // 비고
    private String attorneyName; // 대리인명
    private String attorneyPhoneNumber; // 대리인 연락처

    // 상태 관련 필드
    private BranchUpdateRequest.RequestStatus status; // 요청 상태
    private Long approverId; // 승인자/거부자 ID
    private String approverName; // 승인자/거부자 이름
    private LocalDateTime processedAt; // 처리 시간
    private LocalDateTime createdAt; // 요청 생성 시간

    public static BranchUpdateRequestDto fromEntity(BranchUpdateRequest request) {
        return BranchUpdateRequestDto.builder()
                .id(request.getId())
                .branchId(request.getBranch().getId())
                .branchName(request.getBranch().getName())
                .requesterId(request.getRequester().getId())
                .requesterName(request.getRequester().getName())
                .name(request.getRequestedName())
                .businessDomain(request.getRequestedBusinessDomain())
                .ownershipType(request.getRequestedOwnershipType())
                .openDate(request.getRequestedOpenDate())
                .businessNumber(request.getRequestedBusinessNumber())
                .corporationNumber(request.getRequestedCorporationNumber())
                .zipcode(request.getRequestedZipcode())
                .address(request.getRequestedAddress())
                .addressDetail(request.getRequestedAddressDetail())
                .phone(request.getRequestedPhone())
                .email(request.getRequestedEmail())
                .latitude(request.getRequestedLatitude())
                .longitude(request.getRequestedLongitude())
                .geofenceRadius(request.getRequestedGeofenceRadius())
                .remark(request.getRequestedRemark())
                .attorneyName(request.getRequestedAttorneyName())
                .attorneyPhoneNumber(request.getRequestedAttorneyPhoneNumber())
                .status(request.getStatus())
                .approverId(request.getApprover() != null ? request.getApprover().getId() : null)
                .approverName(request.getApprover() != null ? request.getApprover().getName() : null)
                .processedAt(request.getProcessedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}

