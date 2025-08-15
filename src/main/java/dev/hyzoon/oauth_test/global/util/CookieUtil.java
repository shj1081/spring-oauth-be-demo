package dev.hyzoon.oauth_test.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Optional;

public class CookieUtil {

    // 요청 헤더에서 쿠키를 이름으로 조회
    public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null && cookies.length > 0) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return Optional.of(cookie);
                }
            }
        }
        return Optional.empty();
    }

    // 응답 헤더에 HttpOnly 쿠키 추가
    public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/"); // 쿠키가 모든 경로에서 유효하도록 설정
        cookie.setHttpOnly(true); // 자바스크립트 접근 방지
        cookie.setMaxAge(maxAge); // 쿠키 만료 시간 설정
        // cookie.setSecure(true); // HTTPS 환경에서만 쿠키가 전송되도록 설정 (prod 환경에서는 필수)
        response.addCookie(cookie);
    }

    // 쿠키 삭제
    public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    cookie.setValue("");
                    cookie.setPath("/");
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);
                }
            }
        }
    }
}