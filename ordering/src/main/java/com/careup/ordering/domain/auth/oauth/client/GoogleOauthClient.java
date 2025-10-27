package com.careup.ordering.domain.auth.oauth.client;

import com.careup.ordering.domain.auth.oauth.dto.GoogleProfileDto;
import com.careup.ordering.domain.auth.oauth.dto.OauthTokenDto;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Component
public class GoogleOauthClient {

    private final RestClient rest;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    private static final String TOKEN_URL   = "https://oauth2.googleapis.com/token";
    private static final String PROFILE_URL = "https://openidconnect.googleapis.com/v1/userinfo";
    private static final String REVOKE_URL  = "https://oauth2.googleapis.com/revoke";

    public GoogleOauthClient(
            @Value("${oauth.google.client-id}") String clientId,
            @Value("${oauth.google.client-secret}") String clientSecret,
            @Value("${oauth.google.redirect-uri}") String redirectUri
    ) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());

        this.rest = RestClient.builder()
                .requestFactory(factory)
                .build();

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public OauthTokenDto exchangeCode(String code, @Nullable String codeVerifier) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");
        if (codeVerifier != null && !codeVerifier.isBlank()) {
            params.add("code_verifier", codeVerifier);
        }

        return rest.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(params)
                .retrieve()
                .body(OauthTokenDto.class);
    }

    public GoogleProfileDto getProfile(String accessToken) {
        return rest.get()
                .uri(PROFILE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(GoogleProfileDto.class);
    }

    public void revokeRefreshToken(String token) {
        if (token == null || token.isBlank()) return;

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("token", token);

        rest.post()
                .uri(REVOKE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
}
