package dev.hyzoon.oauth_test.oauth.service;

import dev.hyzoon.oauth_test.domain.user.User;
import dev.hyzoon.oauth_test.domain.user.UserRole;
import dev.hyzoon.oauth_test.oauth.dto.OAuth2UserInfo;
import dev.hyzoon.oauth_test.oauth.factory.OAuth2UserInfoFactory;
import dev.hyzoon.oauth_test.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final OAuth2UserInfoFactory userInfoFactory;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 기본 OAuth2User 객체 로드
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // oauth factory 를 사용하여 provider 별 user info 가져오기
        OAuth2UserInfo oAuth2UserInfo = userInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
        if (oAuth2UserInfo.getEmail() == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider.");
        }

        // 사용자 정보 DB 저장 또는 업데이트
        User user = saveOrUpdate(oAuth2UserInfo);

        // Spring Security가 사용할 최종 OAuth2User 객체 생성 및 반환
//        String userNameAttributeName = userRequest.getClientRegistration()
//                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        String emailAttributeName = "email";
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())), // DB 에서 조회한 role 을 기반으로 권한을 부여
                oAuth2UserInfo.getAttributes(), //  OAuth 2.0 제공자로부터 받은 원본 사용자 정보를 그대로
                emailAttributeName // OAuth 2.0 로그인 성공 시 사용자를 식별하는 기준이 되는 키 값
        );
    }

    private User saveOrUpdate(OAuth2UserInfo oAuth2UserInfo) {
        User user = userRepository.findByEmail(oAuth2UserInfo.getEmail())
                .map(entity -> entity.update(oAuth2UserInfo.getName(), oAuth2UserInfo.getPicture())) // 사용자가 DB에 존재하는 경우 (최신 정보로 업데이트)
                .orElseGet(() -> createUser(oAuth2UserInfo)); // 사용자가 DB에 존재하지 않는 경우
        return userRepository.save(user);
    }

    private User createUser(OAuth2UserInfo oAuth2UserInfo) {
        return User.builder()
                .email(oAuth2UserInfo.getEmail())
                .name(oAuth2UserInfo.getName())
                .picture(oAuth2UserInfo.getPicture())
                .role(UserRole.GUEST) // 기본 role = ROLE_GUEST
                .build();
    }
}