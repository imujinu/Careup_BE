package com.careup.ordering.domain.auth.oauth.client;

import com.careup.ordering.domain.auth.oauth.dto.KakaoProfileDto;
import com.careup.ordering.domain.auth.oauth.dto.OauthTokenDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class KakaoOauthClient {

    private final RestClient rest;
    private final String clientId;
    private final String redirectUri;

    // 단일 생성자(스프링이 자동 주입)
    public KakaoOauthClient(RestClient.Builder restBuilder,
                            @Value("${oauth.kakao.client-id}") String clientId,
                            @Value("${oauth.kakao.redirect-uri}") String redirectUri) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(2000);

        this.rest = restBuilder
                .requestFactory(factory) // ← 람다 말고 팩토리 인스턴스 직접 전달
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .build();

        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    public OauthTokenDto exchangeCode(String code) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        return rest.post()
                .uri("https://kauth.kakao.com/oauth/token")
                .body(params)
                .retrieve()
                .body(OauthTokenDto.class);
    }

    public KakaoProfileDto getProfile(String accessToken) {
        return rest.get()
                .uri("https://kapi.kakao.com/v2/user/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoProfileDto.class);
    }
}
