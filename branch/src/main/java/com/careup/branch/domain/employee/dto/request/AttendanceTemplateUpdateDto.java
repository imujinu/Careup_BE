package com.careup.branch.domain.employee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceTemplateUpdateDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    private LocalTime defaultClockIn;
    private LocalTime defaultBreakStart;
    private LocalTime defaultBreakEnd;
    private LocalTime defaultClockOut;

    // 템플릿 변경사항 기존 스케줄에 반영할지 여부
    private Boolean propagate;

    // 반영 전략: ALL(무조건 갱신), ONLY_IF_UNMODIFIED(템플릿 그대로였던 스케줄만 갱신)
    @Builder.Default
    private PropagationStrategy strategy = PropagationStrategy.ONLY_IF_UNMODIFIED;

    // 반영 대상 기간(옵션) — 둘 다 null이면 전체
    private LocalDate propagateFrom;
    private LocalDate propagateTo;

    public enum PropagationStrategy {
        ALL,
        ONLY_IF_UNMODIFIED
    }
}
