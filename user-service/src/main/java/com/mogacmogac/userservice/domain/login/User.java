package com.mogacmogac.userservice.domain.login;

import com.mogacmogac.userservice.common.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
public class User extends BaseTimeEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column
    private String picture;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;


    @Enumerated(EnumType.STRING)
    private Social social;

    @Getter
    @RequiredArgsConstructor
    public enum Role {
        USER("ROLE_USER", "일반 사용자"),
        ADMIN("ROLE_ADMIN", "관리자");

        private final String key;
        private final String title;
    }

    @Getter
    @RequiredArgsConstructor
    public enum Social {
        KAKAO("카카오"),
        NAVER("네이버"),
        GOOGLE("구글"),
        FACEBOOK("페이스북"),
        GITHUB("깃허브");

        private final String title;
    }

    @Builder
    public User(String name, String email, String picture, Role role, Social social){
        this.name = name;
        this.email = email;
        this.picture = picture;
        this.role = role;
        this.social = social;
    }

    public User update(String name, String picture){
        this.name = name;
        this.picture = picture;

        return this;
    }

    public String getRoleKey(){
        return this.role.getKey();
    }
}