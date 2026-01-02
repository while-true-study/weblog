package com.example.blog.global.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(staticName = "of")
public class ApiResponse<T> {
    private boolean success;
    private T data;
    private ErrorBody error;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        ApiResponse<T> r = new ApiResponse<>(false, null, new ErrorBody(code, message));
        r.success = false;
        r.data = null;
        r.error = new ErrorBody(code, message);
        return r;
    }

    public record ErrorBody(String code, String message) {}
}
