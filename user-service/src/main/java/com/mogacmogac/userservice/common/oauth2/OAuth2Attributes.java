package com.mogacmogac.userservice.common.oauth2;

import com.mogacmogac.userservice.domain.login.User;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

@Log4j2
@Getter
public class OAuth2Attributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String picture;
    private String registrationId;

    @Builder
    public OAuth2Attributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String picture, String registrationId){
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.registrationId = registrationId;
    }

    /**
     * 카카오, 네이버, 페이스북, 구글, 깃허브 등에 따른 속성을 만들어줌
     * @param registrationId 소셜 타입, 즉 네이버, 카카오, 페이스북, 구글, 깃허브
     * @param userNameAttributeName Principal.getName 하게 되면 나오는 로그인한 유저의 이름으로 등록할 필드명
     * @param attributes 각 플랫폼에서 반환받은 유저 정보
     * @return 인증 객체
     */
    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        log.info("요청 :: "+registrationId);
        log.info("유저이름 :: "+userNameAttributeName);
        log.info("속성 :: "+attributes);
        switch(registrationId){
            case "naver":
                return ofNaver(registrationId, "name", attributes);
            case "kakao":
                return ofKakao(registrationId, "nickname", attributes);
            case "github":
                return ofGithub(registrationId, "login", attributes);
            case "facebook":
                return ofFacebook(registrationId, "name", attributes);
            case "google":
                return ofGoogle(registrationId, "name", attributes);
            default:
                throw new IllegalArgumentException("해당 로그인을 찾을 수 없습니다.");
        }
    }
    private static OAuth2Attributes ofFacebook(String registrationId, String userNameAttributeName, Map<String, Object> attributes){
        return OAuth2Attributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("avatar_url"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .registrationId(registrationId)
                .build();
    }
    private static OAuth2Attributes ofGithub(String registrationId, String userNameAttributeName, Map<String, Object> attributes){
        return OAuth2Attributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("avatar_url"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .registrationId(registrationId)
                .build();
    }
    private static OAuth2Attributes ofKakao(String registrationId, String userNameAttributeName, Map<String, Object> attributes){
        Map<String, Object> properties = (Map<String, Object>) attributes.get("properties");
        properties.put("id", attributes.get("id"));
        return OAuth2Attributes.builder()
                .name((String) properties.get("nickname"))
                .email((String) properties.get("email"))
                .picture((String) properties.get("profile_image"))
                .attributes(properties)
                .nameAttributeKey(userNameAttributeName)
                .registrationId(registrationId)
                .build();
    }
    private static OAuth2Attributes ofGoogle(String registrationId, String userNameAttributeName, Map<String, Object> attributes){
        return OAuth2Attributes.builder()
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .picture((String) attributes.get("picture"))
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .registrationId(registrationId)
                .build();
    }
    private static OAuth2Attributes ofNaver(String registrationId, String userNameAttributeName, Map<String, Object> attributes){
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");

        return OAuth2Attributes.builder()
                .name((String) response.get("name"))
                .email((String) response.get("email"))
                .picture((String) response.get("profile_image"))
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .registrationId(registrationId)
                .build();
    }
    public User toEntity(){
        return User.builder().name(name).email(email).picture(picture).role(User.Role.USER).social(User.Social.valueOf(registrationId.toUpperCase())).build();
    }

}