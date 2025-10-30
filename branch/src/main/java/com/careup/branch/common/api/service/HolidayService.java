package com.careup.branch.common.api.service;

import com.careup.branch.calendar.dto.HolidayApiResponseDto.Body.Items.Item;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class HolidayService {

    // 환경변수 또는 설정에서 주입 (빈 문자열 허용)
    @Value("${HOLIDAY_SERVICE_KEY:}")
    private String serviceKey;

    private final RestClient restClient = RestClient.builder()
            .baseUrl("https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService")
            .messageConverters(c -> c.add(new Jackson2ObjectMapperBuilder().build()))
            .build();

    public List<String> fetchKoreanHolidays(LocalDate from, LocalDate to) {
        if (serviceKey == null || serviceKey.isBlank()) {
            // 키가 없으면 빈 목록 반환(또는 예외)
            return List.of();
        }

        List<String> ymds = new ArrayList<>();
        LocalDate cursor = from.withDayOfMonth(1);
        LocalDate endMonth = to.withDayOfMonth(1);

        while (!cursor.isAfter(endMonth)) {
            String y = String.valueOf(cursor.getYear());
            String m = String.format("%02d", cursor.getMonthValue());

            com.careup.branch.calendar.dto.HolidayApiResponseDto resp = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getHoliDeInfo")
                            .queryParam("serviceKey", serviceKey) // 보통 인코딩된 키 사용 필요
                            .queryParam("_type", "json")
                            .queryParam("solYear", y)
                            .queryParam("solMonth", m)
                            .build())
                    .retrieve()
                    .body(com.careup.branch.calendar.dto.HolidayApiResponseDto.class);

            if (resp != null
                    && resp.getResponse() != null
                    && resp.getResponse().getBody() != null
                    && resp.getResponse().getBody().getItems() != null
                    && resp.getResponse().getBody().getItems().getItem() != null) {

                for (Item it : resp.getResponse().getBody().getItems().getItem()) {
                    if (it == null) continue;
                    // isHoliday가 "Y"인 날만 사용(원한다면 기념일 등 다른 API도 추가)
                    if (!"Y".equalsIgnoreCase(Objects.toString(it.getIsHoliday(), ""))) continue;

                    String loc = String.valueOf(it.getLocdate()); // 예: 20251003
                    if (loc != null && loc.length() == 8) {
                        String ymd = loc.substring(0, 4) + "-" + loc.substring(4, 6) + "-" + loc.substring(6, 8);
                        ymds.add(ymd);
                    }
                }
            }

            cursor = cursor.plusMonths(1);
        }

        return ymds.stream()
                .map(LocalDate::parse)
                .filter(d -> !d.isBefore(from) && !d.isAfter(to))
                .map(LocalDate::toString)
                .distinct()
                .sorted()
                .toList();
    }
}
