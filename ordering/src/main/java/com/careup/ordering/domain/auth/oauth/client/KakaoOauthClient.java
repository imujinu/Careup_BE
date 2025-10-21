package com.careup.ordering.domain.auth.oauth.client;

import com.careup.ordering.domain.auth.oauth.dto.KakaoProfileDto;
import com.careup.ordering.domain.auth.oauth.dto.OauthTokenDto;
import java.time.Duration;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
public class KakaoOauthClient {

    private final RestClient rest;
    private final String clientId;
    private final String redirectUri;
    private final String adminKey;

    private static final String TOKEN_URL   = "https://kauth.kakao.com/oauth/token";
    private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String UNLINK_URL  = "https://kapi.kakao.com/v1/user/unlink";

    public KakaoOauthClient(
            @Value("${oauth.kakao.client-id}") String clientId,
            @Value("${oauth.kakao.redirect-uri}") String redirectUri,
            @Value("${oauth.kakao.admin-key:}") String adminKey
    ) {
        // 간단한 타임아웃 설정
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(2).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(2).toMillis());

        this.rest = RestClient.builder()
                .requestFactory(factory)
                .build();

        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.adminKey = adminKey;
    }

    /** 인가코드로 토큰 교환 */
    public OauthTokenDto exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        return rest.post()
                .uri(TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(OauthTokenDto.class);
    }

    /** 사용자 프로필 조회 */
    public KakaoProfileDto getProfile(String accessToken) {
        return rest.get()
                .uri(PROFILE_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(KakaoProfileDto.class);
    }

    /**
     * 관리자 권한 언링크(동의 기록 제거)
     *  - target_id_type=user_id, target_id = 카카오 user id
     *  - Authorization: KakaoAK {ADMIN_KEY}
     */
    public void unlinkByAdmin(String kakaoUserId) {
        if (adminKey == null || adminKey.isBlank()) {
            log.warn("[KAKAO][UNLINK] skipped: admin-key not configured");
            return;
        }

        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("target_id_type", "user_id");
            form.add("target_id", kakaoUserId);

            var res = rest.post()
                    .uri(UNLINK_URL)
                    .header(HttpHeaders.AUTHORIZATION, "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (res.getStatusCode().is2xxSuccessful()) {
                log.info("[KAKAO][UNLINK] success user_id={}", kakaoUserId);
            } else {
                log.warn("[KAKAO][UNLINK] non-2xx user_id={}, status={}", kakaoUserId, res.getStatusCode());
            }
        } catch (RestClientException e) {
            log.warn("[KAKAO][UNLINK] failed user_id={}, err={}", kakaoUserId, e.getMessage());
        } catch (Exception e) {
            log.warn("[KAKAO][UNLINK] failed user_id={}, err={}", kakaoUserId, e.toString());
        }
    }
}
