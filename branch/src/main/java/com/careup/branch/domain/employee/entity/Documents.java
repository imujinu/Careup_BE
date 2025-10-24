package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Documents extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="employee_id")
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private DocumentType documentType;

    /// 관리자 입력 문서명 (ex: 임성후 근로계약서, 이승지 인감증명서, 최재혁 가맹계약서)
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "document_url", columnDefinition = "TEXT", nullable = false)
    private String documentUrl;

    @Column(name = "file_size")
    private Long fileSize;  // 바이트 단위

    @Column(name = "expiry_date")
    private LocalDate expiryDate;  // 만료일 (선택)

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;  // 설명 (선택)

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 20)
    private DocumentStatus documentStatus;  // 서류 상태

    public void update(
            Long id, Employee findEmployee, DocumentType documentType, String title,
            String documentUrl, Long fileSize, LocalDate expiryDate, String description
    ) {
        this.id = id;
        this.employee = findEmployee;
        this.documentType = documentType;
        this.title = title;
        this.documentUrl = documentUrl;
        this.fileSize = fileSize;
        this.expiryDate = expiryDate;
        this.description = description;
        this.documentStatus = calculateStatus();
    }

    // 서류 상태 계산
    public DocumentStatus calculateStatus() {
        if (expiryDate == null) {
            return DocumentStatus.ACTIVE;
        }

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysLater = today.plusDays(7);

        if (expiryDate.isBefore(today)) {
            return DocumentStatus.EXPIRED;  // 만료됨
        } else if (expiryDate.isBefore(sevenDaysLater) || expiryDate.isEqual(sevenDaysLater)) {
            return DocumentStatus.EXPIRING_SOON;  // 만료 임박 (7일 이내)
        } else {
            return DocumentStatus.ACTIVE;  // 활성
        }
    }

    // 상태 업데이트 (스케줄러나 조회 시 호출 가능)
    public void updateStatus() {
        this.documentStatus = calculateStatus();
    }
}
