package com.careup.ordering.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Data
@Builder
public class ResponseDto<T> {

    private final boolean success;
    private final int code;
    private final String message;
    private final T data;

    public static <T> ResponseDto<T> ok(T data, HttpStatus status) {
        return ResponseDto.<T>builder()
                .success(true)
                .code(status.value())
                .data(data)
                .message("요청이 성공적으로 처리되었습니다.")
                .build();
    }

    public static <T> ResponseDto<T> fail(HttpStatus status, String message) {
        return ResponseDto.<T>builder()
                .success(false)
                .code(status.value())
                .message(message)
                .data(null)
                .build();
    }
}