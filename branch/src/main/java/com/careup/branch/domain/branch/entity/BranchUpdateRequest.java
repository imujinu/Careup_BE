package com.careup.branch.domain.branch.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 지점 정보 수정 요청 엔티티
 *
 * <p>설계 노트:</p>
 * <ul>
 *   <li>개별 컬럼 방식 채택 이유:
 *     <ol>
 *       <li>감사(Audit) 추적: 수정 요청의 정확한 스냅샷 보존</li>
 *       <li>데이터 무결성: 원본 Branch와 독립적 관리</li>
 *       <li>성능: 단일 테이블 쿼리로 모든 정보 조회 가능</li>
 *       <li>이력 관리: 승인/거부 후에도 요청 내용 보존</li>
 *     </ol>
 *   </li>
 * </ul>
 */
@Entity
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BranchUpdateRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id")
    private Employee requester; // 요청한 직원

    // 요청된 지점 정보 필드들
    @Column(length = 100, nullable = false)
    private String requestedName;

    @Column(length = 30, nullable = false)
    private String requestedBusinessDomain;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnershipType requestedOwnershipType;

    @Column(nullable = false)
    private LocalDate requestedOpenDate;

    @Column(length = 20, nullable = false)
    private String requestedBusinessNumber;

    @Column(length = 20, nullable = false)
    private String requestedCorporationNumber;

    @Column(length = 20, nullable = false)
    private String requestedZipcode;

    @Column(nullable = false)
    private String requestedAddress;

    private String requestedAddressDetail;

    @Column(columnDefinition = "TEXT")
    private String requestedProfileImageUrl;

    @Column(length = 32, nullable = false)
    private String requestedPhone;

    @Column(nullable = false)
    private String requestedEmail;

    @Column
    private Double requestedLatitude;

    @Column
    private Double requestedLongitude;

    @Column(nullable = false)
    private Integer requestedGeofenceRadius;

    @Column(length = 200)
    private String requestedRemark;

    @Column(name = "requested_attorney_name", length = 100)
    private String requestedAttorneyName;

    @Column(name = "requested_attorney_phone_number", length = 100)
    private String requestedAttorneyPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Employee approver;

    private LocalDateTime processedAt;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    /**
     * 현재 지점 정보로부터 수정 요청 생성 (팩토리 메서드)
     */
    public static BranchUpdateRequest from(Branch branch, Employee requester) {
        return BranchUpdateRequest.builder()
                .branch(branch)
                .requester(requester)
                .requestedName(branch.getName())
                .requestedBusinessDomain(branch.getBusinessDomain())
                .requestedOwnershipType(branch.getOwnershipType())
                .requestedOpenDate(branch.getOpenDate())
                .requestedBusinessNumber(branch.getBusinessNumber())
                .requestedCorporationNumber(branch.getCorporationNumber())
                .requestedZipcode(branch.getZipcode())
                .requestedAddress(branch.getAddress())
                .requestedAddressDetail(branch.getAddressDetail())
                .requestedProfileImageUrl(branch.getProfileImageUrl())
                .requestedPhone(branch.getPhone())
                .requestedEmail(branch.getEmail())
                .requestedLatitude(branch.getLatitude())
                .requestedLongitude(branch.getLongitude())
                .requestedGeofenceRadius(branch.getGeofenceRadius())
                .requestedRemark(branch.getRemark())
                .requestedAttorneyName(branch.getAttorneyName())
                .requestedAttorneyPhoneNumber(branch.getAttorneyPhoneNumber())
                .status(RequestStatus.PENDING)
                .build();
    }

    /**
     * 승인 처리 및 Branch에 변경사항 적용
     */
    public void approve(Employee approver) {
        this.status = RequestStatus.APPROVED;
        this.approver = approver;
        this.processedAt = LocalDateTime.now();

        // Branch 엔티티에 변경사항 적용
        applyToBranch();
    }

    /**
     * 거부 처리
     */
    public void reject(Employee approver) {
        this.status = RequestStatus.REJECTED;
        this.approver = approver;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 요청된 변경사항을 Branch에 적용하는 내부 메서드
     */
    private void applyToBranch() {
        // Branch 엔티티에 직접 필드 업데이트
        // Branch에 updateFromRequest() 메서드가 있다고 가정
        branch.updateFromRequest(this);
    }

    /**
     * 요청 내용 부분 수정 (재요청 시 사용)
     */
    public void updateRequestedData(
            String name,
            String businessDomain,
            OwnershipType ownershipType,
            LocalDate openDate,
            String businessNumber,
            String corporationNumber,
            String zipcode,
            String address,
            String addressDetail,
            String phone,
            String email,
            Double latitude,
            Double longitude,
            Integer geofenceRadius,
            String remark,
            String attorneyName,
            String attorneyPhoneNumber
    ) {
        this.requestedName = name;
        this.requestedBusinessDomain = businessDomain;
        this.requestedOwnershipType = ownershipType;
        this.requestedOpenDate = openDate;
        this.requestedBusinessNumber = businessNumber;
        this.requestedCorporationNumber = corporationNumber;
        this.requestedZipcode = zipcode;
        this.requestedAddress = address;
        this.requestedAddressDetail = addressDetail;
        this.requestedPhone = phone;
        this.requestedEmail = email;
        this.requestedLatitude = latitude;
        this.requestedLongitude = longitude;
        this.requestedGeofenceRadius = geofenceRadius;
        this.requestedRemark = remark;
        this.requestedAttorneyName = attorneyName;
        this.requestedAttorneyPhoneNumber = attorneyPhoneNumber;
    }

    /**
     * 프로필 이미지 URL 업데이트
     */
    public void updateProfileImageUrl(String profileImageUrl) {
        this.requestedProfileImageUrl = profileImageUrl;
    }
}

