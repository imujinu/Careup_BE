package com.careup.branch.domain.employee.entity;

import com.careup.branch.common.domain.BaseTimeEntity;
import com.careup.branch.domain.employee.dto.request.EmployeeUpdateDto;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class Employee extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 30, unique = true, nullable = false)
    private String employeeNumber;

    @Column(length = 100, nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "job_grade_id")
    private JobGrade jobGrade;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(length = 20, nullable = false)
    private String zipcode;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String addressDetail;

    @Column(length = 32, nullable = false, unique = true)
    private String mobile;

    @Column(length = 32, nullable = false)
    private String emergencyTel;

    @Column(length = 50, nullable = false)
    private String emergencyName;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    @Column(nullable = false)
    private LocalDate hireDate;

    private LocalDate terminateDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AuthorityType authorityType;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EmploymentStatus employmentStatus;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String profileImageUrl;

    @Column(length = 200)
    private String remark;

    @JsonIgnore
    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<DispatchStatus> dispatchStatuses = new ArrayList<>();

    public void updateFromDto(EmployeeUpdateDto dto, JobGrade jobGrade) {
        this.name = dto.getName();
        this.jobGrade = jobGrade;
        this.dateOfBirth = dto.getDateOfBirth();
        this.gender = dto.getGender();
        this.email = dto.getEmail();
        this.zipcode = dto.getZipcode();
        this.address = dto.getAddress();
        this.addressDetail = dto.getAddressDetail();
        this.mobile = dto.getMobile();
        this.emergencyTel = dto.getEmergencyTel();
        this.emergencyName = dto.getEmergencyName();
        this.relationship = dto.getRelationship();
        this.hireDate = dto.getHireDate();
        this.terminateDate = dto.getTerminateDate();
        this.authorityType = dto.getAuthorityType();
        this.employmentStatus = dto.getEmploymentStatus();
        this.employmentType = dto.getEmploymentType();
        this.profileImageUrl = dto.getProfileImageUrl();
        this.remark = dto.getRemark();
    }
}
