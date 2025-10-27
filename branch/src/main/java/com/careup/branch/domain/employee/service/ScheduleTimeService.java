package com.careup.branch.domain.employee.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class ScheduleTimeService {

    /** [start, end) 반개구간 표현 */
    public static record Interval(LocalDateTime start, LocalDateTime end) {}

    /** 하루 전체 구간 [00:00, 익일 00:00) */
    public Interval daySpan(LocalDate date) {
        return new Interval(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    /** 기본 interval 빌더 (end는 start 이후여야 함) */
    public Interval interval(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !end.isAfter(start)) {
            throw new IllegalArgumentException("잘못된 시간 구간입니다.");
        }
        return new Interval(start, end);
    }

    /** LocalDate + LocalTime 합성 (null 허용) */
    public LocalDateTime coalesce(LocalDate base, LocalTime override, LocalTime template) {
        LocalTime t = override != null ? override : template;
        return t == null ? null : LocalDateTime.of(base, t);
    }

    /** LocalDate + LocalTime 합성 (null 허용, DTO가 LocalDateTime일 때도 사용) */
    public LocalDateTime coalesceDateTime(LocalDateTime override, LocalDate base, LocalTime template) {
        if (override != null) return override;
        return template == null ? null : LocalDateTime.of(base, template);
    }

    /** 두 구간이 정확히 동일한가 */
    public boolean isExactlySame(Interval a, Interval b) {
        if (a == null || b == null) return false;
        return a.start().equals(b.start()) && a.end().equals(b.end());
    }

    /** 두 구간이 겹치는가 (반개구간 기준) */
    public boolean isOverlap(Interval a, Interval b) {
        if (a == null || b == null) return false;
        // [a.start, a.end) 와 [b.start, b.end)의 교집합
        return a.start().isBefore(b.end()) && b.start().isBefore(a.end());
    }

    /** 순수 근무 분(휴게 제외). null 있으면 계산 불가 시 null. */
    public Long workMinutes(LocalDateTime in, LocalDateTime bs, LocalDateTime be, LocalDateTime out) {
        if (in == null || out == null || !out.isAfter(in)) return null;
        long total = Duration.between(in, out).toMinutes();

        long breakMin = 0L;
        if (bs != null && be != null) {
            if (!be.isAfter(bs)) {
                // 휴게 역전은 0으로 처리
            } else {
                // 휴게가 전체 밖으로 나가면 교차분만 차감
                LocalDateTime s = bs.isBefore(in) ? in : bs;
                LocalDateTime e = be.isAfter(out) ? out : be;
                if (e.isAfter(s)) breakMin = Duration.between(s, e).toMinutes();
            }
        }
        return Math.max(0L, total - breakMin);
    }

    /** 휴게 분. 둘 다 있어야 계산. */
    public Long breakMinutes(LocalDateTime bs, LocalDateTime be) {
        if (bs == null || be == null || !be.isAfter(bs)) return null;
        return Duration.between(bs, be).toMinutes();
    }

    /* =========================
       🔧 다른 서비스들이 기대하는 유틸
       ========================= */

    /** (자정 경계 보정) out이 in보다 이르면 out을 +1일 */
    public LocalDateTime normalizeOut(LocalDateTime in, LocalDateTime out) {
        if (out == null) return null;
        if (in == null) return out;
        return out.isAfter(in) ? out : out.plusDays(1);
    }

    /** 두 시각 사이 분. null 포함 시 0, 역전 시 0 */
    public long minutes(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) return 0L;
        long mins = Duration.between(start, end).toMinutes();
        return Math.max(mins, 0L);
    }

    /** from~to (포함) 날짜 리스트 */
    public List<LocalDate> datesBetween(LocalDate from, LocalDate to) {
        List<LocalDate> list = new ArrayList<>();
        if (from == null || to == null) return list;
        LocalDate cur = from;
        while (!cur.isAfter(to)) {
            list.add(cur);
            cur = cur.plusDays(1);
        }
        return list;
    }
}
