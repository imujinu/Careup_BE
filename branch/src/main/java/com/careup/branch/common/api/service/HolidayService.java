package com.careup.branch.common.api.service;

import com.careup.branch.common.api.dto.response.HolidayApiResponseDto;
import com.careup.branch.common.api.dto.response.HolidayApiResponseDto.Body.Items.Item;
import com.careup.branch.common.api.dto.response.HolidayDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class HolidayService {

    @Value("${HOLIDAY_SERVICE_KEY:}")
    private String rawServiceKey;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService")
            .build();

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static class CacheEntry {
        List<HolidayDto> data;
        Instant expiresAt;
    }

    private final Map<YearMonth, CacheEntry> monthCache = new ConcurrentHashMap<>();
    private final Duration ttl = Duration.ofHours(12);

    public List<HolidayDto> fetchKoreanHolidays(LocalDate from, LocalDate to) {
        String key = normalizedKey();
        if (key.isBlank()) {
            log.warn("HolidayService: HOLIDAY_SERVICE_KEY is empty. Return empty list.");
            return List.of();
        }

        LocalDate cursor = from.withDayOfMonth(1);
        LocalDate endMonth = to.withDayOfMonth(1);

        List<HolidayDto> all = new ArrayList<>();
        while (!cursor.isAfter(endMonth)) {
            all.addAll(fetchMonth(YearMonth.from(cursor), key));
            cursor = cursor.plusMonths(1);
        }

        List<HolidayDto> filteredSorted = all.stream()
                .filter(h -> {
                    LocalDate d = LocalDate.parse(h.getYmd(), YMD);
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .sorted(Comparator.comparing(HolidayDto::getYmd))
                .toList();

        Map<String, HolidayDto> dedup = new LinkedHashMap<>();
        for (HolidayDto h : filteredSorted) {
            dedup.put(h.getYmd(), h);
        }
        return new ArrayList<>(dedup.values());
    }

    private List<HolidayDto> fetchMonth(YearMonth ym, String serviceKey) {
        CacheEntry cached = monthCache.get(ym);
        if (cached != null && Instant.now().isBefore(cached.expiresAt)) return cached.data;

        String y = String.valueOf(ym.getYear());
        String m = String.format("%02d", ym.getMonthValue());

        HolidayApiResponseDto resp;
        try {
            resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getRestDeInfo")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", 100)
                            .queryParam("pageNo", 1)
                            .queryParam("solYear", y)
                            .queryParam("solMonth", m)
                            .build())
                    .retrieve()
                    .body(HolidayApiResponseDto.class);
        } catch (RestClientResponseException e) {
            log.warn("HolidayService: HTTP {} calling getRestDeInfo {}-{} : {}",
                    e.getRawStatusCode(), y, m, e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            log.warn("HolidayService: Exception calling getRestDeInfo {}-{} : {}", y, m, e.toString());
            return List.of();
        }

        String code = null;
        if (resp != null && resp.getResponse() != null && resp.getResponse().getHeader() != null) {
            code = Objects.toString(resp.getResponse().getHeader().getResultCode(), null);
        }
        if (code != null && !"00".equalsIgnoreCase(code)) {
            String msg = resp.getResponse().getHeader().getResultMsg();
            log.warn("HolidayService: API non-success for {}-{} : [{}] {}", y, m, code, msg);
            return List.of();
        }

        List<HolidayDto> list = new ArrayList<>();
        if (resp != null
                && resp.getResponse() != null
                && resp.getResponse().getBody() != null
                && resp.getResponse().getBody().getItems() != null
                && resp.getResponse().getBody().getItems().getItem() != null) {

            for (Item it : resp.getResponse().getBody().getItems().getItem()) {
                if (it == null) continue;

                String loc = Objects.toString(it.getLocdate(), "");
                if (loc.length() == 8) {
                    String ymd = loc.substring(0, 4) + "-" + loc.substring(4, 6) + "-" + loc.substring(6, 8);
                    String name = Objects.toString(it.getDateName(), "");
                    if (!name.isBlank()) {
                        list.add(HolidayDto.builder().ymd(ymd).name(name).build());
                    }
                }
            }
        }

        CacheEntry next = new CacheEntry();
        next.data = list;
        next.expiresAt = Instant.now().plus(ttl);
        monthCache.put(ym, next);

        log.debug("HolidayService: fetched {} holidays for {}-{}", list.size(), y, m);
        return list;
    }

    private String normalizedKey() {
        String k = Objects.toString(rawServiceKey, "").trim();
        if (k.isEmpty()) return "";
        if (k.contains("%")) {
            try {
                return URLDecoder.decode(k, StandardCharsets.UTF_8);
            } catch (Exception ignored) {
                return k;
            }
        }
        return k;
    }
}
