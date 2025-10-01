package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.chat.entity.ChatRoom;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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

    //배치 현황 N:1 직원
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="employee_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Employee employee;

    //배치 현황 N:1 지점
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="branch_id")
    private Branch branch;

    //배치 시작일
    @Column(name = "assigned_from", nullable = false)
    private LocalDate assignedFrom;

    //배치 종료일
    @Column(name = "assigned_to", nullable = false)
    private LocalDate assignedTo;

    //배치 취소 일자
    @Column(name = "displacement_date")
    private LocalDate displacementDate;

    // 배치 취소 여부(Y/N) – 기본 N
    @Column(name = "placement_yn", nullable = false, length = 1)
    @Builder.Default
    private String placementYn = "N";
}
