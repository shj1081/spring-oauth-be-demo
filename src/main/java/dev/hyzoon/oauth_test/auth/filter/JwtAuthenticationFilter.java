package dev.hyzoon.oauth_test.auth.filter;

import dev.hyzoon.oauth_test.auth.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

// API 요청 헤더에 담겨 오는 Access Token을 검증하고, SecurityContext에 인증 정보를 저장하는 역할을 하는 필터
// Spring Security 설정에서 UsernamePasswordAuthenticationFilter 앞에 위치
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;

    // 모든 요청이 DispatcherServlet에 도달하기 전에 이 method를 거침
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // 요청 헤더에서 JWT 토큰을 추출
        String jwt = resolveToken(request);

        // 토큰이 존재하고, 유효성 검증(validateToken)에 성공한 경우
        if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
            // 토큰에서 인증 정보(Authentication 객체)를 가져옴
            Authentication authentication = jwtTokenProvider.getAuthentication(jwt);
            // SecurityContextHolder에 인증 정보를 설정 (해당 요청이 처리되는 동안에는 사용자가 인증된 것으로 간주)
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 다음 필터로 요청과 응답을 전달
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
}