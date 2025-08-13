package dev.hyzoon.oauth_test.config.security;

import dev.hyzoon.oauth_test.oauth.filter.JwtAuthenticationFilter;
import dev.hyzoon.oauth_test.oauth.handler.JwtAccessDeniedHandler;
import dev.hyzoon.oauth_test.oauth.handler.JwtAuthenticationEntryPoint;
import dev.hyzoon.oauth_test.oauth.handler.OAuth2AuthenticationFailureHandler;
import dev.hyzoon.oauth_test.oauth.handler.OAuth2AuthenticationSuccessHandler;
import dev.hyzoon.oauth_test.oauth.service.CustomOAuth2UserService;
import dev.hyzoon.oauth_test.oauth.token.TokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final TokenProvider tokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 (localhost:3000 에서의 테스트를 위해 허용하도록 설정)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // CSRF 비활성화 (큰 기반 인증을 사용하는 경우, 서버는 세션 상태를 저장하지 않으므로(stateless))
                .csrf(AbstractHttpConfigurer::disable)

                // 세션을 사용하지 않는 'STATELESS' 정책으로 설정 (든 요청이 토큰을 통해 인증되어야 함을 의미)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP 요청에 대한 접근 권한 설정
                .authorizeHttpRequests(authz -> authz
                        // 인증 없이도 접근을 허용할 API 명시
                        .requestMatchers("/api/v1/auth/refresh", "/api/v1/auth/token", "/api/v1/auth/logout").permitAll() // auth 관련
                        .requestMatchers("/", "/login/**", "/oauth2/**").permitAll() // 소셜 로그인 관련
                        .requestMatchers("/api/v1/user/for-user").hasAuthority("ROLE_USER")

                        // accessDeniedHandler에서 sendError의 경우 내부적인 `/error` 경로로 요청 처리
                        // `/error` 의 경우에도 인증 확인 절차가 이루어져 403 떠야하는 상황에 401 뜨는 것 방지하기 위해
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated()) // 나머지 요청은 모두 인증 필요

                // Exception Handling 설정
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 인증 실패
                        .accessDeniedHandler(jwtAccessDeniedHandler) // 인가 실패
                )

                // OAuth2 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)) // 커스텀 서비스 등록
                        .successHandler(oAuth2AuthenticationSuccessHandler) // 성공 handler
                        .failureHandler(oAuth2AuthenticationFailureHandler)) // 실패 handler

                // JWT 필터 추가 ( 모든 요청에 대해 토큰 검사를 먼저 수행)
                .addFilterBefore(new JwtAuthenticationFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000")); // 프론트엔드 서버 주소 허용
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*")); // 모든 HTTP 헤더 허용
        configuration.setAllowCredentials(true); // 자격 증명(쿠키 등) 허용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 대해 위에서 정의한 CORS 설정을 적용
        return source;
    }
}
