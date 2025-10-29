package com.careup.branch.domain.chat.dto.attendance;

import com.careup.branch.domain.employee.dto.response.AttendanceTemplateListDto;
import com.careup.branch.domain.employee.dto.response.LeaveTypeDetailDto;
import com.careup.branch.domain.employee.dto.response.WorkTypeDetailDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AttendanceAllResDto {
    private AttendanceCompareResDto attendance;
    private List<AttendanceTemplateListDto> templates;
    private List<LeaveTypeDetailDto> leaveTypes;
    private List<WorkTypeDetailDto> workTypes;
}