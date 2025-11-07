package com.careup.branch.domain.chat.dto.attendance;

import com.careup.branch.domain.chat.dto.BaseResponseDto;
import com.careup.branch.domain.employee.dto.response.ScheduleListDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class TodayAttendanceResDto extends BaseResponseDto {
    private String branchName;
    private LocalDate date;
    private List<EmployeeTodayAttendanceDto> employees;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EmployeeTodayAttendanceDto {
        private Long employeeId;
        private String employeeName;
        private String workType;
        private String status;
        private ClockInfo clockInfo;
        private Integer workMinutes;
        private Integer breakMinutes;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ClockInfo {
        private LocalDateTime plannedClockIn;
        private LocalDateTime plannedClockOut;
        private LocalDateTime actualClockIn;
        private LocalDateTime actualClockOut;
    }

    public static TodayAttendanceResDto makeDto(String intent, String action, List<ScheduleListDto> schedules, String branchName) {
        LocalDate today = LocalDate.now();

        List<EmployeeTodayAttendanceDto> employeeDtos = schedules.stream()
                .filter(s -> s.getRegisteredDate() != null &&
                        s.getRegisteredDate().isEqual(LocalDate.now(ZoneId.of("Asia/Seoul")))) // 오늘만 필터링
                .map(s -> {
                    String status = resolveStatus(s);
                    return EmployeeTodayAttendanceDto.builder()
                            .employeeId(s.getEmployeeId())
                            .employeeName(s.getEmployeeName())
                            .workType(s.getWorkTypeName())
                            .status(status)
                            .clockInfo(ClockInfo.builder()
                                    .plannedClockIn(s.getRegisteredClockIn())
                                    .plannedClockOut(s.getRegisteredClockOut())
                                    .actualClockIn(s.getActualClockIn())
                                    .actualClockOut(s.getActualClockOut())
                                    .build())
                            .workMinutes((int) s.getTotalWorkMinutes())
                            .breakMinutes((int) s.getTotalBreakMinutes())
                            .build();
                })
                .collect(Collectors.toList());

        return TodayAttendanceResDto.builder()
                .intent(intent)
                .action(action)
                .branchName(branchName)
                .date(today)
                .employees(employeeDtos)
                .build();
    }

    private static String resolveStatus(ScheduleListDto s) {
        if (s.getActualClockIn() != null && s.getActualClockOut() == null) {
            return "WORKING"; // 근무 중
        } else if (s.getActualClockOut() != null) {
            return "CLOCKED_OUT"; // 퇴근 완료
        } else if ("ABSENT".equals(s.getStatus())) {
            return "ABSENT"; // 결근
        } else if ("LEAVE".equals(s.getCategory())) {
            return "LEAVE"; // 휴가
        } else {
            return "PLANNED"; // 예정
        }
    }
}
