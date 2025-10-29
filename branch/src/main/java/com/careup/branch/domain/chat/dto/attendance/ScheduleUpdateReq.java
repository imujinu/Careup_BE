package com.careup.branch.domain.chat.dto.attendance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleUpdateReq {
    private Long branchId;
    private Long scheduleId;
    private Long workTypeId;
    private Long leaveTypeId;
    private Long templateId;
    private LocalDate date;
}
