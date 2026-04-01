package com.example.blog.global.exception;

import lombok.Getter;

/**
 * 도메인/비즈니스 예외의 기반 클래스.
 * 서비스 계층에서 throw new BlogException(ErrorCode.XXX) 형태로 사용합니다.
 */
@Getter
public class BlogException extends RuntimeException {

    private final ErrorCode errorCode;

    public BlogException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }
}
