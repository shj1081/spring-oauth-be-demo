package dev.hyzoon.oauth_test.auth;

import dev.hyzoon.oauth_test.auth.dto.JwtTokenDto;
import dev.hyzoon.oauth_test.global.config.JwtProperties;
import dev.hyzoon.oauth_test.global.util.CookieUtil;
import dev.hyzoon.oauth_test.oauth.handler.OAuth2AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    // temp auth code를 토큰으로 교환
    @PostMapping("/token")
    public ResponseEntity<JwtTokenDto> exchangeToken(@RequestBody Map<String, String> payload, HttpServletResponse response) {
        String code = payload.get("code");
        JwtTokenDto jwtTokenDto = authService.exchangeCodeForToken(code);

        // Refresh Token은 HttpOnly 쿠키로 설정
        int cookieMaxAgeSeconds = (int) (jwtProperties.getRefreshTokenExpiry() / 1000);
        CookieUtil.addCookie(response, OAuth2AuthenticationSuccessHandler.REFRESH_TOKEN_COOKIE_NAME, jwtTokenDto.getRefreshToken(), cookieMaxAgeSeconds);

        // body 에 토큰 DTO 담아 반환
        return ResponseEntity.ok(jwtTokenDto);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtTokenDto> refreshToken(
            @CookieValue(name = OAuth2AuthenticationSuccessHandler.REFRESH_TOKEN_COOKIE_NAME) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // 새로운 토큰 DTO를 생성
        JwtTokenDto newJwtTokenDto = authService.refreshToken(refreshToken);

        // 새로운 Refresh Token을 쿠키에 덮어쓰기 위해 기존 쿠키를 삭제하고 새로 추가
        int cookieMaxAgeSeconds = (int) (jwtProperties.getRefreshTokenExpiry() / 1000);
        CookieUtil.deleteCookie(request, response, OAuth2AuthenticationSuccessHandler.REFRESH_TOKEN_COOKIE_NAME);
        CookieUtil.addCookie(response, OAuth2AuthenticationSuccessHandler.REFRESH_TOKEN_COOKIE_NAME, newJwtTokenDto.getRefreshToken(), cookieMaxAgeSeconds);

        // body 에 토큰 DTO 담아 반환
        return ResponseEntity.ok(newJwtTokenDto);
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            @CookieValue(name = OAuth2AuthenticationSuccessHandler.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        // 클라이언트 측의 쿠키도 삭제
        CookieUtil.deleteCookie(request, response, OAuth2AuthenticationSuccessHandler.REFRESH_TOKEN_COOKIE_NAME);
        return ResponseEntity.ok("Logout successful");
    }
}