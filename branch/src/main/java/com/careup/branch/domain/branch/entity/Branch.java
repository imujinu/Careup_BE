package com.careup.branch.domain.branch.entity;


import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.branch.dto.branch.BranchUpdateDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(
        name = "branch",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_branch_business_number", columnNames = {"businessNumber"}),
                @UniqueConstraint(name = "uk_branch_corporation_number", columnNames = {"corporationNumber"}),
                @UniqueConstraint(name = "uk_branch_phone", columnNames = {"phone"}),
                @UniqueConstraint(name = "uk_branch_email", columnNames = {"email"})
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
public class Branch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false)
    private String name; // 지점명

    @Column(length = 30, nullable = false)
    private String businessDomain; // 업종

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OwnershipType ownershipType; // 직영 여부 (YES, NO)

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private BranchStatus status = BranchStatus.OPENED; // 지점 상태(OPENED, CLOSED, SUSPENDED))

    @Column(nullable = false)
    private LocalDate openDate; // 개업연월

    @Column(nullable = true)
    private LocalDate closeDate;


    @Column(length = 20, unique = true, nullable = false)
    private String businessNumber; // 사업자등록번호

    @Column(length = 20, nullable = false)
    private String corporationNumber; // 법인등록번호

    @Column(length = 20, nullable = false)
    private String zipcode; // 지점 우편번호

    @Column(nullable = false)
    private String address; // 지점 주소

    private String addressDetail; // 지점 상세 주소

    @Column(columnDefinition = "TEXT")
    @Builder.Default
    private String profileImageUrl = null; // 지점 프로필 이미지

    @Column(length = 32, nullable = false)
    private String phone; // 지점 전화번호

    @Column(nullable = false)
    private String email; // 지점 이메일

    // 지오펜스 중심 좌표 (필수는 아님)
    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(nullable = false)
    private Integer geofenceRadius; // 출퇴근 가능 반경(미터)

    @Column(length = 200)
    private String remark; // 비고


    @Column(name = "attorney_name", length = 100, nullable = true)
    private String attorneyName; // 대리인명

    @Column(name = "attorney_phone_number", length = 100, nullable = true)
    private String attorneyPhoneNumber; // 대리인 연락처

    // 지점 수정 메서드
    public void updateBranch(BranchUpdateDto request) {
        this.name = request.getName();
        this.businessDomain = request.getBusinessDomain();
        this.ownershipType = request.getOwnershipType();
        this.businessNumber = request.getBusinessNumber();
        this.corporationNumber = request.getCorporationNumber();
        this.zipcode = request.getZipcode();
        this.address = request.getAddress();
        this.addressDetail = request.getAddressDetail();
        this.phone = request.getPhone();
        this.profileImageUrl = request.getProfileImageUrl();
        this.email = request.getEmail();
        this.geofenceRadius = request.getGeofenceRadius();
        this.remark = request.getRemark();
        this.latitude = request.getLatitude();
        this.longitude = request.getLongitude();
        this.openDate = request.getOpenDate(); // 필요 시 수정 허용
    }

    // --- 자기수정(HQ 승인 후 적용)용 보조 메서드 ---
    public void changeName(String name) {
        this.name = name;
    }

    public void changeProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    // 자기수정 승인요청 메모 누적용 보조 메서드
    public void appendRemark(String note) {
        if (note == null || note.isBlank()) return;
        String combined;
        if (this.remark == null || this.remark.isBlank()) {
            combined = note;
        } else {
            combined = this.remark + "\n" + note;
        }
        // remark 컬럼 길이(200) 제한 보호: 뒤에서부터 최대 200자 유지
        if (combined.length() > 200) {
            combined = combined.substring(combined.length() - 200);
        }
        this.remark = combined;
    }
}
