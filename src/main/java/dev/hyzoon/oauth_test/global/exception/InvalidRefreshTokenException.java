package dev.hyzoon.oauth_test.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 이 예외가 발생하면 HTTP 401 Unauthorized 상태 코드를 응답하도록 지정
@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}