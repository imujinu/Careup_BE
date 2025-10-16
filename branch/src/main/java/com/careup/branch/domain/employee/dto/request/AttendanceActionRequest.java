package com.careup.branch.domain.employee.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceActionRequest {
    private Double lat;  // 위도
    private Double lng;  // 경도

    // 선택 입력: 액션 시각(없으면 서버 now 사용)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime at;
}
