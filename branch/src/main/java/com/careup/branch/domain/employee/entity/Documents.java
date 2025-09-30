package com.careup.branch.domain.employee.entity;

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
public class Documents extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    //서류 N:1 직원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="employee_id")
    private Employee employee;

    //서류 타입
    private DocumentType documentType;

    //서류
    @Column(name = "document_url", columnDefinition = "TEXT", nullable = false)
    private String documentUrl;


}
