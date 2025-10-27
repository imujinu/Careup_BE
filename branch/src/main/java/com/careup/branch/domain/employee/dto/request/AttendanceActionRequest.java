package com.careup.branch.domain.employee.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceActionRequest {
    private Double lat;
    private Double lng;
    private Integer accuracyMeters;
    private LocalDateTime at;
}
