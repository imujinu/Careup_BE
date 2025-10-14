package com.careup.branch.domain.employee.service;

public class MissedCheckoutLockException extends RuntimeException {
    public MissedCheckoutLockException(String message) {
        super(message);
    }
}
