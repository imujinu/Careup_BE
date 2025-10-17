package com.careup.branch.domain.employee.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "attendance.window")
public class AttendanceWindowProperties {
    private boolean enforce = true;
    private int earlyMinutes = 60;
    private int lateMinutes = 120;
}
