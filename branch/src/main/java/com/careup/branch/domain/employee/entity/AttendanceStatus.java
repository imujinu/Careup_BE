package com.careup.branch.domain.employee.entity;

public enum AttendanceStatus {
    PLANNED,          /// 근무 예정 (이벤트 발생 전) - 파랑 캘린더 카드 및 뱃지
    LATE,             /// 지각 (이벤트 발생 후) - 주황 캘린더 카드 및 뱃지
    CLOCKED_IN,       /// 근무 중 (이벤트 발생 후) - 초록 캘린더 카드 및 뱃지
    ON_BREAK,         /// 휴게 중 (이벤트 발생 후) - 초록 캘린더 카드 및 뱃지
    EARLY_LEAVE,      /// 조퇴 (이벤트 발생 후) - 주황 캘린더 카드 및 뱃지
    CLOCKED_OUT,      /// 근무 완료 (이벤트 발생 후) - 초록 캘린더 카드 및 뱃지
    OVERTIME,         /// 초과 근무 (이벤트 발생 후) - 주황 캘린더 카드 및 뱃지
    MISSED_CHECKOUT,  /// 퇴근 누락: 출근 O, 퇴근 X (마감 이후) - 주황 캘린더 카드 및 뱃지
    LEAVE,            /// 휴가/휴무 (이벤트 발생 전, 후 모두) - 보라 캘린더 카드 및 뱃지
    ABSENT            /// 무단결근: 출근 X, 퇴근 X (마감 이후) - 빨강 캘린더 카드 및 뱃지
}
