package com.mogacmogac.userservice.config.oauth2;

import com.mogacmogac.userservice.common.jwt.OAuth2SuccessHandler;
import com.mogacmogac.userservice.common.jwt.TokenFactory;
import com.mogacmogac.userservice.common.jwt.filter.JwtAuthFilter;
import com.mogacmogac.userservice.common.oauth2.CustomOAuth2UserService;
import com.mogacmogac.userservice.domain.login.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@RequiredArgsConstructor
@EnableWebSecurity // Spring Security 설정 활성화
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private final CustomOAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler successHandler;
    private final TokenFactory tokenFactory;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.httpBasic().disable()
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/token/**").permitAll()
                .anyRequest().authenticated()
                .and()
                .oauth2Login().loginPage("/token/expired")
                .successHandler(successHandler)
                .userInfoEndpoint().userService(oAuth2UserService);

        http.addFilterBefore(new JwtAuthFilter(tokenFactory), UsernamePasswordAuthenticationFilter.class);
    }
}