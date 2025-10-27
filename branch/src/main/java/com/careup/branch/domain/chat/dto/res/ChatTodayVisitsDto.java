package com.careup.branch.domain.chat.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatTodayVisitsDto {
    private int firstTimeCount;
    private int returningCount;
    private double YesterdayPercent;
    private double LastWeekPercent;
}
