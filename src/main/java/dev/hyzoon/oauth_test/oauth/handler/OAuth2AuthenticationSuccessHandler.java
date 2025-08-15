package dev.hyzoon.oauth_test.oauth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hyzoon.oauth_test.auth.JwtTokenProvider;
import dev.hyzoon.oauth_test.auth.dto.JwtTokenDto;
import dev.hyzoon.oauth_test.global.config.JwtProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

// 로그인 과정에서 인증이 성공했을 때 호출되는 handler
@Slf4j
@RequiredArgsConstructor
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    // @SneakyThrows = ObjectMapper로 객체를 JSON 문자열로 바꾸는 과정에서 `JsonProcessingException` 이라는 Checked Exception을 던질 수 있는데 이의 발생을 무시
    @SneakyThrows
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        // Authentication 에서 OAuth2User 객체 (principal) 를 추출하고, 사용자 이메일과 권한 정보를 가져옴
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String authorities = oAuth2User.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        // 토큰 DTO 생성
        JwtTokenDto jwtTokenDto = jwtTokenProvider.generateTokenDto(email, authorities);

        // 최초 교환을 위해, 임시 코드와 함께 토큰 DTO를 Redis에 저장 (짧은 만료 시간)
        String authCode = UUID.randomUUID().toString();
        String tokenDtoJson = objectMapper.writeValueAsString(jwtTokenDto); // TokenDto를 JSON 문자열로 변환
        long authCodeExpiry = jwtProperties.getAuthCodeExpiry();
        redisTemplate.opsForValue().set("auth_code:" + authCode, tokenDtoJson, authCodeExpiry, TimeUnit.MILLISECONDS);
        log.info("Temporary auth_code-token pair stored in Redis. code ={} TTL: {}s", authCode, authCodeExpiry / 1000);

        // Refresh Token 을 이메일과 함께 Redis에 저장 (긴 만료 시간)
        long refreshTokenExpiry = jwtProperties.getRefreshTokenExpiry();
        redisTemplate.opsForValue().set(email, jwtTokenDto.getRefreshToken(), refreshTokenExpiry, TimeUnit.MILLISECONDS);
        log.info("Permanent Refresh Token stored in Redis for {}. TTL: {}s", email, refreshTokenExpiry / 1000);

        // 프론트엔드로는 임시 코드만 포함하여 redirection
        String targetUrl = createRedirectUrl(authCode);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String createRedirectUrl(String code) {
        return UriComponentsBuilder.fromUriString("http://localhost:3000/oauth/redirect")
                .queryParam("code", code)
                .build().toUriString();
    }

}
