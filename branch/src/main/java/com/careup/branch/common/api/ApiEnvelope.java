package com.careup.branch.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiEnvelope<T> {
    public boolean success;
    public int status_code;
    public String status_message;
    public String error_code;     // 오류 시에만 세팅
    public T result;              // 성공 시 payload
    public String timestamp = OffsetDateTime.now().toString();

    private ApiEnvelope(boolean success, int statusCode, String message, String errorCode, T result) {
        this.success = success;
        this.status_code = statusCode;
        this.status_message = message;
        this.error_code = errorCode;
        this.result = result;
    }

    public static <T> ApiEnvelope<T> ok(T result) {
        return new ApiEnvelope<>(true, 200, "OK", null, result);
    }

    public static <T> ApiEnvelope<T> of(int statusCode, String message, String errorCode) {
        return new ApiEnvelope<>(false, statusCode, message, errorCode, null);
    }
}
