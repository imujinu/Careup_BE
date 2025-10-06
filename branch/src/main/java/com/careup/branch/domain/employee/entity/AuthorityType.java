package com.careup.branch.domain.employee.entity;

public enum AuthorityType {
    HQ_ADMIN,           /// 본사(본점) 관리자
    BRANCH_ADMIN,       /// 지점(직영) 관리자
    FRANCHISE_OWNER,    /// 가맹점주 (가맹점 관리자)
    STAFF               /// 일반 직원
}
