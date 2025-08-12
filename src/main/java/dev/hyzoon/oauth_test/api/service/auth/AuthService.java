package dev.hyzoon.oauth_test.api.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hyzoon.oauth_test.api.exception.InvalidRefreshTokenException;
import dev.hyzoon.oauth_test.config.properties.JwtProperties;
import dev.hyzoon.oauth_test.domain.user.User;
import dev.hyzoon.oauth_test.oauth.token.TokenDto;
import dev.hyzoon.oauth_test.oauth.token.TokenProvider;
import dev.hyzoon.oauth_test.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final TokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;

    // @SneakyThrows = ObjectMapper로 객체를 JSON 문자열로 바꾸는 과정에서 `JsonProcessingException` 이라는 Checked Exception을 던질 수 있는데 이의 발생을 무시
    @SneakyThrows
    @Transactional
    public TokenDto exchangeCodeForToken(String code) {
        // 임시 코드로 redis의 교환해줄 토큰 조회
        String key = "auth_code:" + code;
        String tokenDtoJson = redisTemplate.opsForValue().get(key);
        if (tokenDtoJson == null) {
            throw new RuntimeException("Invalid or expired authorization code.");
        }
        log.info("token in redis that attained by auth_code :{}", tokenDtoJson);

        // 1회성 교환의 보장을 위해 redis 에서 해당 정보 삭제
        redisTemplate.delete(key);

        // 조회한 토큰 DTO JSON을 역직렬화하여 객체로 반환
        return objectMapper.readValue(tokenDtoJson, TokenDto.class);
    }


    @Transactional
    public TokenDto refreshToken(String refreshTokenFromCookie) {
        // Refresh Token 에서 이메일 추출
        String email = tokenProvider.getEmailFromToken(refreshTokenFromCookie);

        // Redis의 토큰 DTO를 조회하고 그 Refresh Token 과 일치하는지 확인
        String storedRefreshToken = redisTemplate.opsForValue().get(email);
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshTokenFromCookie)) {
            throw new InvalidRefreshTokenException("Refresh Token does not match or not found in Redis.");
        }

        // DB 에서 사용자 정보 조회 (role 업데이트 등의 정보 실시간 반영)
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found."));

        // 새로운 토큰 DTO 생성
        TokenDto newTokenDto = tokenProvider.generateTokenDto(user.getEmail(), user.getRoleKey());

        // redis 정보 업데이트
        redisTemplate.opsForValue().set(
                email,
                newTokenDto.getRefreshToken(),
                jwtProperties.getRefreshTokenExpiry(),
                TimeUnit.MILLISECONDS
        );

        log.info("token refreshed");

        return newTokenDto;
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            // 이미 유효하지 않은 토큰이면 그냥 로그만 남김
            log.warn("Attempted to logout with an invalid refresh token.");
            return;
        }

        // Refresh Token 에서 이메일을 가져옴
        String email = tokenProvider.getEmailFromToken(refreshToken);

        // Redis 에서 해당 이메일을 Key로 가진 Refresh Token 을 삭제
        if (redisTemplate.opsForValue().get(email) != null) {
            redisTemplate.delete(email);
            log.info("Logout successful. Deleted refresh token for email: {}", email);
        } else {
            log.warn("Logout attempt for a non-existent refresh token in Redis. Email: {}", email);
        }
    }
}