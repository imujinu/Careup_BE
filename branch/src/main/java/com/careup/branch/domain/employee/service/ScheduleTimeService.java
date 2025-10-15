package com.careup.branch.domain.employee.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleTimeService {

    /**
     * 명시값(LocalDateTime)이 있으면 그대로, 없으면 날짜+템플릿(LocalTime)로 합성.
     */
    public LocalDateTime coalesceDateTime(LocalDateTime explicit, LocalDate date, LocalTime tmpl) {
        if (explicit != null) return explicit;
        if (date != null && tmpl != null) return LocalDateTime.of(date, tmpl);
        return null;
    }

    /**
     * 날짜 + (명시 시간 또는 템플릿 시간)로 LocalDateTime 합성.
     */
    public LocalDateTime coalesce(LocalDate date, LocalTime explicit, LocalTime tmpl) {
        if (explicit != null) return LocalDateTime.of(date, explicit);
        if (date != null && tmpl != null) return LocalDateTime.of(date, tmpl);
        return null;
    }

    /**
     * out이 in보다 빠르면 익일로 보정.
     */
    public LocalDateTime normalizeOut(LocalDateTime in, LocalDateTime out) {
        if (out == null) return null;
        if (in == null) return out;
        if (out.isAfter(in)) return out;
        return out.plusDays(1);
    }

    /**
     * 두 시각 간 분(min). end가 start 이전이면 익일로 간주.
     */
    public long minutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0L;
        LocalDateTime normEnd = end.isAfter(start) ? end : end.plusDays(1);
        return Duration.between(start, normEnd).toMinutes();
    }

    /**
     * (from ~ to) 양끝 포함 일자 리스트.
     */
    public List<LocalDate> datesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            list.add(d);
            d = d.plusDays(1);
        }
        return list;
    }

    /** '하루 종일' 간격 */
    public Interval allDay() { return Interval.allDay(); }

    /** 구간 생성(끝이 시작 이전이면 익일로 보정) */
    public Interval interval(LocalDateTime start, LocalDateTime end) {
        return Interval.of(start, end.isAfter(start) ? end : end.plusDays(1));
    }

    /**
     * 두 구간이 겹치는지. all-day가 하나라도 있으면 무조건 겹치는 것으로 간주.
     */
    public boolean isOverlap(Interval a, Interval b) {
        if (a.isAllDay() || b.isAllDay()) return true;
        return a.start.isBefore(b.end) && b.start.isBefore(a.end);
    }

    /**
     * 두 구간이 정확히 같은지(시작/끝 동일). 둘 다 all-day면 동일로 간주.
     */
    public boolean isExactlySame(Interval a, Interval b) {
        if (a.isAllDay() && b.isAllDay()) return true;
        if (a.isAllDay() || b.isAllDay()) return false;
        return a.start.equals(b.start) && a.end.equals(b.end);
    }

    /**
     * 내부 표현: 하루종일 또는 [start, end) (end는 익일 가능)
     */
    public static class Interval {
        private final boolean allDay;
        private final LocalDateTime start;
        private final LocalDateTime end;

        private Interval(boolean allDay, LocalDateTime start, LocalDateTime end) {
            this.allDay = allDay;
            this.start = start;
            this.end = end;
        }

        public static Interval allDay() { return new Interval(true, null, null); }

        public static Interval of(LocalDateTime start, LocalDateTime end) {
            if (start == null || end == null) throw new IllegalArgumentException("start/end가 필요합니다.");
            return new Interval(false, start, end);
        }

        public boolean isAllDay() { return allDay; }
        public LocalDateTime start() { return start; }
        public LocalDateTime end() { return end; }
    }
}
