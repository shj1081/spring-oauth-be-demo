package dev.hyzoon.oauth_test.auth;

import dev.hyzoon.oauth_test.auth.dto.JwtTokenDto;
import dev.hyzoon.oauth_test.global.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private static final String AUTHORITIES_KEY = "auth";
    private final Key key;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    // 이메일과 권한 정보를 바탕으로 토큰 DTO 생성
    public JwtTokenDto generateTokenDto(String email, String authorities) {
        long now = (new Date()).getTime();
        Date accessTokenExpiresIn = new Date(now + jwtProperties.getAccessTokenExpiry());
        Date refreshTokenExpiresIn = new Date(now + jwtProperties.getRefreshTokenExpiry());

        String accessToken = Jwts.builder()
                .setSubject(email)
                .claim(AUTHORITIES_KEY, authorities)
                .setExpiration(accessTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        String refreshToken = Jwts.builder()
                .setSubject(email)
                .setExpiration(refreshTokenExpiresIn)
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();

        return JwtTokenDto.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    // Access Token 에서 Authentication 생성
    public Authentication getAuthentication(String accessToken) {
        Claims claims = parseClaims(accessToken);
        if (claims.get(AUTHORITIES_KEY) == null) {
            throw new RuntimeException("권한 정보가 없는 토큰입니다.");
        }

        // claim 에서 권한 정보를 추출하여 Spring Security가 이해할 수 있는 GrantedAuthority 객체 컬렉션으로 변환
        Collection<? extends GrantedAuthority> authorities =
                Arrays.stream(claims.get(AUTHORITIES_KEY).toString().split(","))
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

        // User = Authentication principal 중 하나인 UserDetail의 구현체 중 하나
        User principal = new User(claims.getSubject(), "", authorities);

        // UsernamePasswordAuthenticationToken = Authentication 의 구현체 중 하나
        return new UsernamePasswordAuthenticationToken(principal, accessToken, authorities);
    }

    // Refresh Token 에서 이메일 추출 (/refresh API 에서 Redis 조회 시 이용)
    public String getEmailFromToken(String token) {
        if (!validateToken(token)) {
            throw new RuntimeException("Invalid Token");
        }
        return parseClaims(token).getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.info("유효하지 않은 JWT 토큰입니다 - {}", e.getMessage());
            return false;
        }
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        }
    }
}