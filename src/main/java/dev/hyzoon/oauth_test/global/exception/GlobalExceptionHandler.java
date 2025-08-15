package dev.hyzoon.oauth_test.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // InvalidRefreshTokenException 예외를 처리
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshTokenException(InvalidRefreshTokenException ex) {
        log.warn("Invalid refresh token received: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED) // 401 상태 코드
                .body(Map.of("error", "Invalid Refresh Token", "message", ex.getMessage()));
    }
}