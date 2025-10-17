package com.careup.branch.domain.employee.service;

import com.careup.branch.common.util.GeoUtils;
import com.careup.branch.domain.employee.config.GeofenceProperties;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.entity.WorkType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GeofenceValidator {

    private final GeofenceProperties props;

    public void validateIfRequired(Schedule schedule,
                                   Double lat,
                                   Double lng,
                                   Integer accuracyMeters,
                                   ScheduleAuthService.Auth auth) {
        if (!props.isEnabled()) return;
        if (schedule == null) throw new IllegalArgumentException("스케줄이 필요합니다.");
        if (schedule.getCategory() == ScheduleTypeCategory.LEAVE) return;

        WorkType wt = schedule.getWorkType();
        if (wt == null || !Boolean.TRUE.equals(wt.getGeofenceRequired())) return;

        if (lat == null || lng == null) {
            throw new IllegalArgumentException("위치 정보가 필요합니다.");
        }
        if (accuracyMeters != null && accuracyMeters > props.getMaxAccuracyMeters()) {
            throw new IllegalArgumentException("위치 정확도가 낮습니다. 다시 시도해주세요.");
        }

        var b = schedule.getBranch();
        if (b == null) throw new IllegalStateException("지점 정보가 없습니다.");
        if (b.getLatitude() == null || b.getLongitude() == null) {
            throw new IllegalStateException("지점 기준 좌표가 설정되지 않았습니다.");
        }
        Integer radius = b.getGeofenceRadius();
        if (radius == null || radius <= 0) {
            throw new IllegalStateException("지점 지오펜스 반경이 유효하지 않습니다.");
        }

        double dist = GeoUtils.distanceMeters(lat, lng, b.getLatitude(), b.getLongitude());
        if (dist > (radius + props.getRadiusSlackMeters())) {
            throw new IllegalArgumentException("지점 반경을 벗어났습니다.");
        }
    }
}
