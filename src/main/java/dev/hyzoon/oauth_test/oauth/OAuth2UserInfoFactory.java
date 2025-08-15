package dev.hyzoon.oauth_test.oauth;

import dev.hyzoon.oauth_test.oauth.dto.GithubUserInfo;
import dev.hyzoon.oauth_test.oauth.dto.OAuth2UserInfo;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Map;

// 소셜 로그인 제공자(Provider)에 따라 적절한 OAuth2UserInfo 구현체를 생성하는 factory
@Component
public class OAuth2UserInfoFactory {
    public OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes) {
        switch (registrationId.toLowerCase()) {
            case "github":
                return new GithubUserInfo(attributes);

            // 다른 provider 추가 시 case 추가
            // case "google":
            //     return new GoogleUserInfo(attributes);

            default:
                // 지원하지 않는 제공자인 경우 예외 발생
                throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }
    }
}