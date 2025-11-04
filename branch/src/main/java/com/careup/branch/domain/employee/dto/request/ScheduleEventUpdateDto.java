package com.careup.branch.domain.employee.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
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

    @JsonAlias({"actualDate"})
    private LocalDate eventDate;

    @JsonAlias({"actualClockIn"})
    private LocalDateTime clockInAt;

    @JsonAlias({"actualBreakStart"})
    private LocalDateTime breakStartAt;

    @JsonAlias({"actualBreakEnd"})
    private LocalDateTime breakEndAt;

    @JsonAlias({"actualClockOut"})
    private LocalDateTime clockOutAt;

    private Boolean clearMissedCheckout;
}
