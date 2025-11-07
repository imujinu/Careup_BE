package com.careup.branch.domain.chat.dto.attendance;

import com.careup.branch.domain.chat.dto.BaseResponseDto;
import com.careup.branch.domain.employee.dto.response.AttendanceTemplateListDto;
import com.careup.branch.domain.employee.dto.response.LeaveTypeDetailDto;
import com.careup.branch.domain.employee.dto.response.WorkTypeDetailDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class AttendanceAllResDto extends BaseResponseDto {
    private AttendanceCompareResDto attendance;
    private List<AttendanceTemplateListDto> templates;
    private List<LeaveTypeDetailDto> leaveTypes;
    private List<WorkTypeDetailDto> workTypes;
}