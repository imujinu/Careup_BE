package com.careup.branch.domain.branch.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.employee.entity.Employee;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(length = 100, nullable = false)
    private String requestedName; // 요청된 지점명

    @Column(columnDefinition = "TEXT")
    private String requestedProfileImageUrl; // 요청된 프로필 이미지

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING; // 요청 상태 (PENDING, APPROVED, REJECTED)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approver_id")
    private Employee approver; // 승인/거부한 관리자

    private LocalDateTime processedAt; // 처리 일시

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    // 승인 처리
    public void approve(Employee approver) {
        this.status = RequestStatus.APPROVED;
        this.approver = approver;
        this.processedAt = LocalDateTime.now();
    }

    // 거부 처리
    public void reject(Employee approver) {
        this.status = RequestStatus.REJECTED;
        this.approver = approver;
        this.processedAt = LocalDateTime.now();
    }
}
