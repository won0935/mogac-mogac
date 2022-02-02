package com.mogacmogac.userservice.authentication;

import com.mogacmogac.userservice.authentication.oauth2.*;
import com.mogacmogac.userservice.authentication.oauth2.account.OAuth2AccountDTO;
import com.mogacmogac.userservice.authentication.oauth2.service.OAuth2Service;
import com.mogacmogac.userservice.authentication.oauth2.service.OAuth2ServiceFactory;
import com.mogacmogac.userservice.authentication.oauth2.userInfo.OAuth2UserInfo;
import com.mogacmogac.userservice.jwt.JwtProvider;
import com.mogacmogac.userservice.security.StatelessCSRFFilter;
import com.mogacmogac.userservice.security.UserDetailsImpl;
import com.mogacmogac.userservice.users.UserService;
import com.mogacmogac.userservice.util.CookieUtils;
import com.mogacmogac.userservice.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
public class AuthenticationController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final InMemoryOAuth2RequestRepository inMemoryOAuth2RequestRepository;
    private final RestTemplate restTemplate;
    private final JwtProvider jwtProvider;

    @GetMapping("/csrf-token")
    public ResponseEntity<?> getCsrfToken(HttpServletRequest request, HttpServletResponse response) {
        String csrfToken = UUID.randomUUID().toString();

        Map<String, String> resMap = new HashMap<>();
        resMap.put(StatelessCSRFFilter.CSRF_TOKEN, csrfToken);

        generateCSRFTokenCookie(response);
        return ResponseEntity.ok(resMap);
    }

    /* 사용자의 계정을 인증하고 로그인 토큰을 발급해주는 컨트롤러 */
    @PostMapping("/authorize")
    public void authenticateUsernamePassword(@Valid @RequestBody AuthorizationRequest authorizationRequest, BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (bindingResult.hasErrors()) throw new ValidationException("로그인 유효성 검사 실패.", bindingResult.getFieldErrors());
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(authorizationRequest.getUsername(), authorizationRequest.getPassword()));
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            generateTokenCookie(userDetails, request, response);
            generateCSRFTokenCookie(response);
        } catch (AuthenticationException e) {
            throw new AuthenticationFailedException("아이디 또는 패스워드가 틀렸습니다.");
        }
    }

    /* 토큰 쿠키를 삭제하는 컨트롤러 (로그아웃) */
    @PostMapping("/logout")
    public ResponseEntity<?> expiredToken(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, "access_token");
        CookieUtils.deleteCookie(request, response, StatelessCSRFFilter.CSRF_TOKEN);
        return ResponseEntity.ok("success");
    }

    /* 사용자의 소셜 로그인 요청을 받아 각 소셜 서비스로 인증을 요청하는 컨트롤러 */
    @GetMapping("/oauth2/authorize/{provider}")
    public void redirectSocialAuthorizationPage(@PathVariable String provider, @RequestParam(name = "redirect_uri") String redirectUri, @RequestParam String callback, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String state = generateState();

        // 콜백에서 사용할 요청 정보를 저장
        inMemoryOAuth2RequestRepository.saveOAuth2Request(state, OAuth2AuthorizationRequest.builder().referer(request.getHeader("referer")).redirectUri(redirectUri).callback(callback).build());

        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
        OAuth2Service oAuth2Service = OAuth2ServiceFactory.getOAuth2Service(restTemplate, provider);
        oAuth2Service.redirectAuthorizePage(clientRegistration, state, response);
    }

    /* 각 소셜 서비스로부터 인증 결과를 처리하는 컨트롤러 */
    @RequestMapping("/oauth2/callback/{provider}")
    public void oAuth2AuthenticationCallback(@PathVariable String provider, OAuth2AuthorizationResponse oAuth2AuthorizationResponse, HttpServletRequest request, HttpServletResponse response, @AuthenticationPrincipal UserDetailsImpl loginUser) throws Exception {

        //인증을 요청할 때 저장했던 request 정보를 가져온다.
        OAuth2AuthorizationRequest oAuth2AuthorizationRequest = inMemoryOAuth2RequestRepository.deleteOAuth2Request(oAuth2AuthorizationResponse.getState());

        //유저가 로그인 페이지에서 로그인을 취소하거나 오류가 발생했을때 처리
        if (oAuth2AuthorizationResponse.getError() != null) {
            redirectWithErrorMessage(oAuth2AuthorizationRequest.getReferer(), oAuth2AuthorizationResponse.getError(), response);
            return;
        }

        //사용자의 요청에 맞는 OAuth2 클라이언트 정보를 매핑한다
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(provider);
        OAuth2Service oAuth2Service = OAuth2ServiceFactory.getOAuth2Service(restTemplate, provider);

        //토큰과 유저 정보를 요청
        OAuth2Token oAuth2Token = oAuth2Service.getAccessToken(clientRegistration, oAuth2AuthorizationResponse.getCode(), oAuth2AuthorizationResponse.getState());
        OAuth2UserInfo oAuth2UserInfo = oAuth2Service.getUserInfo(clientRegistration, oAuth2Token.getToken());

        //로그인에 대한 콜백 처리
        if (oAuth2AuthorizationRequest.getCallback().equalsIgnoreCase("login")) {
            UserDetails userDetails = userService.loginOAuth2User(provider, oAuth2Token, oAuth2UserInfo);
            generateTokenCookie(userDetails, request, response);
            generateCSRFTokenCookie(response);
        }
        //계정 연동에 대한 콜백 처리
        else if (oAuth2AuthorizationRequest.getCallback().equalsIgnoreCase("link")) {
            //로그인 상태가 아니면
            if (loginUser == null) {
                redirectWithErrorMessage(oAuth2AuthorizationRequest.getReferer(), "unauthorized", response);
                return;
            }
            try {
                userService.linkOAuth2Account(loginUser.getUsername(), provider, oAuth2Token, oAuth2UserInfo);
            } catch (Exception e) {
                redirectWithErrorMessage(oAuth2AuthorizationRequest.getReferer(), e.getMessage(), response);
                return;
            }
        }

        //콜백 성공
        response.sendRedirect(oAuth2AuthorizationRequest.getRedirectUri());
    }

    @PostMapping("/oauth2/unlink")
    public void unlinkOAuth2Account(@AuthenticationPrincipal UserDetailsImpl loginUser) {

        OAuth2AccountDTO oAuth2AccountDTO = userService.unlinkOAuth2Account(loginUser.getUsername());

        //OAuth 인증 서버에 연동해제 요청
        ClientRegistration clientRegistration = clientRegistrationRepository.findByRegistrationId(oAuth2AccountDTO.getProvider());
        OAuth2Service oAuth2Service = OAuth2ServiceFactory.getOAuth2Service(restTemplate, oAuth2AccountDTO.getProvider());
        oAuth2Service.unlink(clientRegistration, oAuth2AccountDTO.getOAuth2Token());
    }

    private void generateTokenCookie(UserDetails userDetails, HttpServletRequest request, HttpServletResponse response) {
        final int cookieMaxAge = jwtProvider.getTokenExpirationDate().intValue();
        //https 프로토콜인 경우 secure 옵션사용
        boolean secure = request.isSecure();
        CookieUtils.addCookie(response, "access_token", jwtProvider.generateToken(userDetails.getUsername()), true, secure, cookieMaxAge);
    }

    private void generateCSRFTokenCookie(HttpServletResponse response) {
        CookieUtils.addCookie(response, StatelessCSRFFilter.CSRF_TOKEN, UUID.randomUUID().toString(), 60 * 60 * 24);
    }


    private void redirectWithErrorMessage(String uri, String message, HttpServletResponse response) throws IOException {
        String redirectUri = UriComponentsBuilder.fromUriString(uri)
                .replaceQueryParam("error", message).encode().build().toUriString();
        response.sendRedirect(redirectUri);
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }
}
