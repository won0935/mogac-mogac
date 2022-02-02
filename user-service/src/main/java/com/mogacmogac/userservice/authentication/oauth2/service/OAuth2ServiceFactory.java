package com.mogacmogac.userservice.authentication.oauth2.service;

import org.springframework.web.client.RestTemplate;

public class OAuth2ServiceFactory {
    public static OAuth2Service getOAuth2Service(RestTemplate restTemplate, String registrationId) {

        if (registrationId.equalsIgnoreCase("google"))
            return new GoogleOAuth2Service(restTemplate);
        if (registrationId.equalsIgnoreCase("naver"))
            return new NaverOAuth2Service(restTemplate);
        if (registrationId.equalsIgnoreCase("kakao"))
            return new KakaoOAuth2Service(restTemplate);
        else
            throw new IllegalArgumentException(registrationId.toUpperCase() + " 로그인은 지원하지 않습니다.");
    }
}
