package com.careup.branch.domain.chat.dto.attendance;

import com.careup.branch.domain.chat.dto.BaseResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ScheduleUpdateReq extends BaseResponseDto {
    private Long branchId;
    private Long scheduleId;
    private Long workTypeId;
    private Long leaveTypeId;
    private Long templateId;
    private LocalDate date;
}
