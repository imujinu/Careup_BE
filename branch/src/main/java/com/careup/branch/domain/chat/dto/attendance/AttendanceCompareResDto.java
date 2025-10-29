package com.careup.branch.domain.chat.dto.attendance;

import com.careup.branch.domain.employee.dto.response.ScheduleListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceCompareResDto {

    private String branchName;
    private PeriodDto period;
    private List<EmployeeAttendanceDto> employees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PeriodDto {
        private LocalDate start;
        private LocalDate end;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeAttendanceDto {
        private Long employeeId;
        private String employeeName;
        private SummaryDto summary;
        private List<AttendanceDetailDto> details;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryDto {
        private int totalDays;
        private int workDays;
        private int absentDays;
        private int leaveDays;
        private int totalWorkMinutes;
        private double averageWorkMinutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AttendanceDetailDto {
        private Long scheduleId;
        private LocalDate date;
        private String workType;
        private String status;
        private Integer workMinutes;
        private Integer breakMinutes;
        private String leaveType;
    }

    public static AttendanceCompareResDto makeDto(
            List<ScheduleListDto> attendances,
            String branchName,
            LocalDate startDate,
            LocalDate endDate
    ) {

        Map<Long, List<ScheduleListDto>> groupedByEmployee = attendances.stream()
                .collect(Collectors.groupingBy(ScheduleListDto::getEmployeeId));

        List<EmployeeAttendanceDto> employeeDtos = groupedByEmployee.entrySet().stream()
                .map(entry -> {
                    Long employeeId = entry.getKey();
                    List<ScheduleListDto> records = entry.getValue();
                    String employeeName = records.get(0).getEmployeeName();

                    List<AttendanceDetailDto> details = records.stream()
                            .map(a -> AttendanceDetailDto.builder()
                                    .scheduleId(a.getId())
                                    .date(a.getRegisteredDate())
                                    .workType(a.getWorkTypeName())
                                    .status(String.valueOf(a.getStatus()))
                                    .workMinutes((int) a.getTotalWorkMinutes())
                                    .breakMinutes((int) a.getTotalBreakMinutes())
                                    .leaveType(a.getLeaveTypeName())
                                    .build())
                            .collect(Collectors.toList());

                    int totalDays = details.size();
                    int workDays = (int) details.stream().filter(d -> "CLOCKED_OUT".equals(d.getStatus())).count();
                    int absentDays = (int) details.stream().filter(d -> "ABSENT".equals(d.getStatus())).count();
                    int leaveDays = (int) details.stream().filter(d -> "LEAVE".equals(d.getStatus())).count();
                    int totalWorkMinutes = details.stream()
                            .mapToInt(d -> d.getWorkMinutes() != null ? d.getWorkMinutes() : 0)
                            .sum();
                    double averageWorkMinutes = workDays > 0 ? (double) totalWorkMinutes / workDays : 0;

                    SummaryDto summary = SummaryDto.builder()
                            .totalDays(totalDays)
                            .workDays(workDays)
                            .absentDays(absentDays)
                            .leaveDays(leaveDays)
                            .totalWorkMinutes(totalWorkMinutes)
                            .averageWorkMinutes(averageWorkMinutes)
                            .build();

                    return EmployeeAttendanceDto.builder()
                            .employeeId(employeeId)
                            .employeeName(employeeName)
                            .summary(summary)
                            .details(details)
                            .build();
                })
                .collect(Collectors.toList());

        return AttendanceCompareResDto.builder()
                .branchName(branchName)
                .period(PeriodDto.builder()
                        .start(startDate)
                        .end(endDate)
                        .build())
                .employees(employeeDtos)
                .build();
    }
}
