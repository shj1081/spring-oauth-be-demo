package dev.hyzoon.oauth_test.domain.user;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserRole {

    // spring security 에서는 권한 코드에 항상 `ROLE_` 접두사가 붙어야 함
    GUEST("ROLE_GUEST", "방문자"),
    USER("ROLE_USER", "일반 사용자");

    private final String code;
    private final String displayName;
}
