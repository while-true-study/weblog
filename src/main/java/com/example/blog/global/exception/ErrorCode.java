package com.example.blog.global.exception;

public enum ErrorCode {

    // ── Post ─────────────────────────────────────────────────────────────────
    POST_NOT_FOUND(404, "P001", "게시글을 찾을 수 없습니다."),
    POST_ALREADY_DELETED(409, "P002", "이미 삭제된 게시글입니다."),
    POST_FORBIDDEN(403, "P003", "게시글 수정/삭제 권한이 없습니다."),

    // ── Comment ──────────────────────────────────────────────────────────────
    COMMENT_NOT_FOUND(404, "C001", "댓글을 찾을 수 없습니다."),
    COMMENT_FORBIDDEN(403, "C002", "댓글 수정/삭제 권한이 없습니다."),

    // ── User ─────────────────────────────────────────────────────────────────
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(409, "U002", "이미 가입된 이메일입니다."),

    // ── Series ───────────────────────────────────────────────────────────────
    SERIES_NOT_FOUND(404, "S001", "시리즈를 찾을 수 없습니다."),

    // ── Notification ────────────────────────────────────────────────────────
    NOTIFICATION_NOT_FOUND(404, "N001", "알림을 찾을 수 없습니다."),

    // ── Auth ─────────────────────────────────────────────────────────────────
    UNAUTHORIZED(401, "A001", "인증이 필요합니다."),
    FORBIDDEN(403, "A002", "접근 권한이 없습니다."),
    LOGIN_FAILED(401, "A003", "이메일 또는 비밀번호가 올바르지 않습니다."),

    // ── Common ───────────────────────────────────────────────────────────────
    INVALID_INPUT(400, "CMN001", "잘못된 요청입니다."),
    INTERNAL_ERROR(500, "CMN002", "서버 내부 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public int status()   { return status; }
    public String code()  { return code; }
    public String message() { return message; }
}
