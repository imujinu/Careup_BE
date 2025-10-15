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

    public LocalDateTime coalesceDateTime(LocalDateTime explicit, LocalDate date, LocalTime tmpl) {
        if (explicit != null) return explicit;
        if (date != null && tmpl != null) return LocalDateTime.of(date, tmpl);
        return null;
    }

    public LocalDateTime coalesce(LocalDate date, LocalTime explicit, LocalTime tmpl) {
        if (explicit != null) return LocalDateTime.of(date, explicit);
        if (date != null && tmpl != null) return LocalDateTime.of(date, tmpl);
        return null;
    }

    public LocalDateTime normalizeOut(LocalDateTime in, LocalDateTime out) {
        if (out == null) return null;
        if (in == null) return out;
        if (out.isAfter(in)) return out;
        return out.plusDays(1);
    }

    public long minutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0L;
        LocalDateTime normEnd = end.isAfter(start) ? end : end.plusDays(1);
        return Duration.between(start, normEnd).toMinutes();
    }

    public List<LocalDate> datesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> list = new ArrayList<>();
        LocalDate d = from;
        while (!d.isAfter(to)) {
            list.add(d);
            d = d.plusDays(1);
        }
        return list;
    }

    public Interval allDay() { return Interval.allDay(); }

    public Interval daySpan(LocalDate date) {
        return Interval.of(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    public Interval interval(LocalDateTime start, LocalDateTime end) {
        return Interval.of(start, end.isAfter(start) ? end : end.plusDays(1));
    }

    public boolean isOverlap(Interval a, Interval b) {
        if (a.isAllDay() || b.isAllDay()) return true;
        return a.start.isBefore(b.end) && b.start.isBefore(a.end);
    }

    public boolean isExactlySame(Interval a, Interval b) {
        if (a.isAllDay() && b.isAllDay()) return true;
        if (a.isAllDay() || b.isAllDay()) return false;
        return a.start.equals(b.start) && a.end.equals(b.end);
    }

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
