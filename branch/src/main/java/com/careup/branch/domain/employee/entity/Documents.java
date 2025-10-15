package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

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

    public void update(
            Long id, Employee findEmployee, DocumentType documentType, String title, String documentUrl
    ) {
        this.id = id;
        this.employee = findEmployee;
        this.documentType = documentType;
        this.title = title;
        this.documentUrl = documentUrl;
    }
}
