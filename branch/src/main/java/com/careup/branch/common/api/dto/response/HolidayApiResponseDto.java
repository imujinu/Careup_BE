// src/main/java/com/careup/branch/calendar/dto/HolidayApiResponse.java
package com.careup.branch.common.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HolidayApiResponseDto {
    private Response response;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Response {
        private Header header;
        private Body body;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Body {
        private Items items;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Items {
            // item이 단일 객체로 올 때가 있어 배열 허용
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            private java.util.List<Item> item;

            @Getter
            @Builder
            @NoArgsConstructor
            @AllArgsConstructor
            @JsonIgnoreProperties(ignoreUnknown = true)
            public static class Item {
                // 예: 20251003
                private Integer locdate;
                // 예: 개천절
                private String dateName;
                // "Y"/"N"
                private String isHoliday;
            }
        }
    }
}
