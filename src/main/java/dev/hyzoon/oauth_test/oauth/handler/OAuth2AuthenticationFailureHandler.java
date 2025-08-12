package dev.hyzoon.oauth_test.oauth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

// 로그인 과정에서 인증이 실패했을 때 호출되는 handler
@Slf4j
@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException {
        log.error("OAuth2 Login Failed: {}", exception.getMessage());

        // 사용자를 redirection 시킬 프론트엔드의 에러 페이지 URL을 생성하여 브라우저 redirection
        String targetUrl = UriComponentsBuilder.fromUriString("http://localhost:3000/login/error")
                .queryParam("error", exception.getLocalizedMessage())
                .build().toUriString();
        response.sendRedirect(targetUrl);
    }
}