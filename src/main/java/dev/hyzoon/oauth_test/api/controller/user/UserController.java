package dev.hyzoon.oauth_test.api.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, String>> getUserInfo(@AuthenticationPrincipal User user) {
        // @AuthenticationPrincipal = 현재 인증된 사용자의 정보를 받아옴
        // User는 user entity가 아닌 JwtAuthenticationFilter 에서 SecurityContext에 저장한 User 객체를 의미
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "User not authenticated"));
        }
        return ResponseEntity.ok(Map.of("email", user.getUsername()));
    }
}