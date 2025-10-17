package com.careup.branch.domain.employee.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "attendance.geofence")
public class GeofenceProperties {
    private boolean enabled = true;
    private int radiusSlackMeters = 0;
    private int maxAccuracyMeters = 100;

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRadiusSlackMeters(int v) { this.radiusSlackMeters = v; }
    public void setMaxAccuracyMeters(int v) { this.maxAccuracyMeters = v; }
}
