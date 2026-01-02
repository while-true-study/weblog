package com.example.blog.global.exception;

public enum ErrorCode {
    DUPLICATE_EMAIL(409, "이미 가입된 이메일입니다."),
    UNAUTHORIZED(401, "인증이 필요합니다."),
    FORBIDDEN(403, "권한이 없습니다."),
    BAD_REQUEST(400, "잘못된 요청입니다."),
    INTERNAL_ERROR(500, "서버 오류입니다.");

    private final int status;
    private final String message;

    ErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    public int status() { return status; }
    public String message() { return message; }
}