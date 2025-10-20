package com.careup.branch.domain.branch.dto.branch;

import com.careup.branch.domain.branch.entity.OwnershipType;
import lombok.*;

import java.time.LocalDate;

/**
 * 지점 정보 수정 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchUpdateRequestDto {

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
}

