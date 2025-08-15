package dev.hyzoon.oauth_test.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt") // "jwt" 접두사를 가진 설정을 binding
public class JwtProperties {
    private String secret;
    private long accessTokenExpiry;
    private long refreshTokenExpiry;
    private long authCodeExpiry;
}
