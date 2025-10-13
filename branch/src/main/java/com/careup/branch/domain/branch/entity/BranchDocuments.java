package com.careup.branch.domain.branch.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchDocuments extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private BranchDocumentType branchDocumentType;

    /// 관리자 입력 문서명 (ex: 임성후 근로계약서, 이승지 인감증명서, 최재혁 가맹계약서)
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "document_url", columnDefinition = "TEXT", nullable = false)
    private String branchDocumentUrl;

    public void update(
            Long id, Branch findBranch, BranchDocumentType branchDocumentType, String title, String branchDocumentUrl
    ) {
        this.id = id;
        this.branch = findBranch;
        this.branchDocumentType = branchDocumentType;
        this.title = title;
        this.branchDocumentUrl = branchDocumentUrl;
    }
}
