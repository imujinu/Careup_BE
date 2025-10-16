package com.careup.branch.domain.employee.service;

import com.careup.branch.common.util.GeoUtils;
import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.Schedule;
import com.careup.branch.domain.employee.entity.ScheduleTypeCategory;
import com.careup.branch.domain.employee.entity.WorkType;
import org.springframework.stereotype.Component;

@Component
public class GeofenceValidator {

    public void validateIfRequired(Schedule schedule, Double lat, Double lng) {
        if (schedule == null) throw new IllegalArgumentException("스케줄이 필요합니다.");
        if (schedule.getCategory() == ScheduleTypeCategory.LEAVE) return;

        WorkType wt = schedule.getWorkType();
        if (wt == null) return;

        boolean required = Boolean.TRUE.equals(wt.getGeofenceRequired());
        if (!required) return; // 반경 외 허용(지오펜스 미적용)

        Branch b = schedule.getBranch();
        if (b == null) throw new IllegalStateException("지점 정보가 없습니다.");

        // 지점 좌표/반경 필수
        if (b.getLatitude() == null || b.getLongitude() == null) {
            throw new IllegalStateException("지오펜스 기준 좌표가 설정되지 않았습니다.");
        }
        Integer radiusMeters = b.getGeofenceRadius();
        if (radiusMeters == null || radiusMeters <= 0) {
            throw new IllegalStateException("지오펜스 반경 정보가 유효하지 않습니다. (양수 필요)");
        }

        if (lat == null || lng == null) throw new IllegalArgumentException("위치 정보(lat/lng)가 필요합니다.");

        double dist = GeoUtils.distanceMeters(lat, lng, b.getLatitude(), b.getLongitude());
        if (dist > radiusMeters) {
            throw new IllegalArgumentException(
                    String.format("반경 초과(%.0fm/허용 %dm). 지점 반경 내에서만 출퇴근이 가능합니다.", dist, radiusMeters)
            );
        }
    }
}
