package com.mogacmogac.userservice.common.oauth2;

import com.mogacmogac.userservice.domain.login.User;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;

/**
 * 직렬화 기능을 가진 User클래스
 */
@Getter
public class UserDto implements Serializable {
    private String name;
    private String email;
    private String picture;

    public UserDto(User user){
        this.name = user.getName();
        this.email = user.getEmail();
        this.picture = user.getPicture();
    }

    @Builder
    public UserDto(String email, String name, String picture) {
        this.email = email;
        this.name = name;
        this.picture = picture;
    }
}