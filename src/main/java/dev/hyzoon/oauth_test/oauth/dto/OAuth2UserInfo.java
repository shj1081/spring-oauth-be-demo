package dev.hyzoon.oauth_test.oauth.dto;

import java.util.Map;

public interface OAuth2UserInfo {
    Map<String, Object> getAttributes();

    String getProviderId();

    String getProvider(); // "github", "google" 등

    String getEmail();

    String getName();

    String getPicture();
}
