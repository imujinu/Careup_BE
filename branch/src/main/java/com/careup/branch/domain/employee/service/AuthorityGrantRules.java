package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.entity.AuthorityType;

public final class AuthorityGrantRules {
    private AuthorityGrantRules() {}

    public static boolean canGrant(AuthorityType actor, AuthorityType target) {
        if (actor == AuthorityType.HQ_ADMIN) return true;
        return target == AuthorityType.STAFF;
    }
}
