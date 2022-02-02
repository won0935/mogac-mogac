package com.mogacmogac.userservice.config;

import com.mogacmogac.userservice.jwt.filter.JwtAuthenticationFilter;
import com.mogacmogac.userservice.security.StatelessCSRFFilter;
import com.mogacmogac.userservice.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfigurer extends WebSecurityConfigurerAdapter {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /*
         AuthenticationManager 에서 authenticate 메소드를 실행할때
         내부적으로 사용할 UserDetailsService 와 PasswordEncoder 를 설정
    */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and() //세션 사용 x
                .csrf().disable()
                .cors().disable()
                .formLogin().disable()
                .logout().disable() // '/logout' uri 를 사용하기 위한 설정
                .httpBasic().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers("/csrf-token").permitAll()
                .antMatchers(HttpMethod.POST, "/authorize", "/users").anonymous()
                .antMatchers(HttpMethod.POST, "/oauth2/unlink").authenticated()
                .antMatchers("/oauth2/**").permitAll()
                .anyRequest().authenticated().and()
                .exceptionHandling()
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED));

        //로그인 인증을 진행하는 필터 이전에 jwtAuthenticationFilter 가 실행되도록 설정
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                //CSRF 필터 설정
                .addFilterBefore(new StatelessCSRFFilter(), CsrfFilter.class);
    }

    /*PasswordEncoder를 BCryptPasswordEncoder로 사용하도록 Bean 등록*/
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
