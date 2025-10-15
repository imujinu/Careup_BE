package com.careup.branch.domain.employee.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEventUpdateDto {

    private LocalDate eventDate;

    private LocalDateTime clockInAt;

    private LocalDateTime breakStartAt;

    private LocalDateTime breakEndAt;

    private LocalDateTime clockOutAt;

    private Boolean clearMissedCheckout;
}
