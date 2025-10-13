package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.branch.entity.Branch;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispatchStatus extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="branch_id")
    private Branch branch;

    @Column(name = "assigned_from", nullable = false)
    private LocalDate assignedFrom;

    @Column(name = "assigned_to", nullable = false)
    private LocalDate assignedTo;

    @Column(name = "displacement_date")
    private LocalDate displacementDate;

    @Column(name = "placement_yn", nullable = false, length = 1)
    @Builder.Default
    private String placementYn = "N";

    public void endAssignment(LocalDate endDate) {
        this.assignedTo = endDate;
        this.placementYn = "Y";
        this.displacementDate = endDate;
    }
}
