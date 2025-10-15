package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.response.ScheduleEventDetailDto;

public class MissedCheckoutLockException extends RuntimeException {

    private final ScheduleEventDetailDto latest;

    public MissedCheckoutLockException(String message) {
        super(message);
        this.latest = null;
    }

    public MissedCheckoutLockException(String message, ScheduleEventDetailDto latest) {
        super(message);
        this.latest = latest;
    }

    public ScheduleEventDetailDto getLatest() {
        return latest;
    }
}
