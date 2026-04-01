package com.example.blog.global.exception;

import com.example.blog.global.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 도메인/비즈니스 예외 — BlogException(ErrorCode.XXX)
     * ErrorCode에 HTTP 상태, 코드, 메시지가 이미 담겨 있으므로 그대로 사용.
     */
    @ExceptionHandler(BlogException.class)
    public ResponseEntity<ApiResponse<Void>> handleBlogException(BlogException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("[BlogException] code={} message={}", code.code(), e.getMessage());
        return ResponseEntity
                .status(code.status())
                .body(ApiResponse.fail(code));
    }

    /**
     * @Valid / @Validated 검증 실패 — MethodArgumentNotValidException
     * 모든 필드 오류를 "fieldName: message" 형태로 이어 붙여 반환.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[ValidationException] {}", message);
        return ResponseEntity
                .status(400)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.code(), message));
    }

    /**
     * @Validated 경로 변수/쿼리 파라미터 검증 실패 — ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining(", "));
        log.warn("[ConstraintViolation] {}", message);
        return ResponseEntity
                .status(400)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.code(), message));
    }

    /**
     * 필수 쿼리 파라미터 누락 — MissingServletRequestParameterException
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException e) {
        String message = "필수 파라미터 '" + e.getParameterName() + "'이(가) 누락되었습니다.";
        log.warn("[MissingParam] {}", message);
        return ResponseEntity
                .status(400)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.code(), message));
    }

    /**
     * 경로 변수 타입 불일치 (예: /posts/abc) — MethodArgumentTypeMismatchException
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String message = "파라미터 '" + e.getName() + "'의 값이 올바르지 않습니다.";
        log.warn("[TypeMismatch] {}", message);
        return ResponseEntity
                .status(400)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.code(), message));
    }

    /**
     * 요청 본문 역직렬화 실패 (잘못된 JSON 등) — HttpMessageNotReadableException
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("[MessageNotReadable] {}", e.getMessage());
        return ResponseEntity
                .status(400)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.code(), "요청 본문을 읽을 수 없습니다."));
    }

    /**
     * Spring Security 인증 실패 — AuthenticationException
     * 로그인 실패, 토큰 불량 등
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        log.warn("[AuthenticationException] {}", e.getMessage());
        return ResponseEntity
                .status(401)
                .body(ApiResponse.fail(ErrorCode.LOGIN_FAILED));
    }

    /**
     * Spring Security 접근 거부 — AccessDeniedException
     * 인증은 됐으나 권한 없음
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AccessDeniedException] {}", e.getMessage());
        return ResponseEntity
                .status(403)
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN));
    }

    /**
     * 예상치 못한 예외 — 500
     * 로그에는 전체 스택 트레이스를 남기고, 응답에는 노출하지 않음.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("[UnexpectedException] {}", e.getMessage(), e);
        return ResponseEntity
                .status(500)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_ERROR));
    }
}
