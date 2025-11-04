package com.careup.branch.common.api.dto.response;

import lombok.Getter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayDto {
    private String ymd;   // YYYY-MM-DD
    private String name;  // 예: 설날, 추석, 어린이날 등
}
