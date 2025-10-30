package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "job_grade",
        uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
public class JobGrade extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, nullable = false)
    private AuthorityType authorityType;

    @Column(nullable = false)
    private Integer orderIndex; // 정렬 순서(작을수록 상단)

    public void update(String name, AuthorityType authorityType) {
        this.name = name;
        this.authorityType = authorityType;
    }

    public void changeOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
