package com.careup.ordering.domain.auth.oauth.client;

import com.careup.ordering.domain.auth.oauth.dto.KakaoProfileDto;
import com.careup.ordering.domain.auth.oauth.dto.OauthTokenDto;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoOauthClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${oauth.kakao.rest-api-key}")
    private String clientId;

    @Value("${oauth.kakao.redirect-uri}")
    private String redirectUri;

    /** 관리자 언링크용 Admin Key (절대 프론트에 노출 금지) */
    @Value("${oauth.kakao.admin-key}")
    private String adminKey;

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String PROFILE_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    /** 인가코드로 토큰 교환 */
    public OauthTokenDto exchangeCode(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", clientId);
        body.add("redirect_uri", redirectUri);
        body.add("code", code);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(body, headers);
        ResponseEntity<OauthTokenDto> res = restTemplate.postForEntity(URI.create(TOKEN_URL), req, OauthTokenDto.class);
        return res.getBody();
    }

    /** 사용자 프로필 조회 */
    public KakaoProfileDto getProfile(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> req = new HttpEntity<>(headers);
        ResponseEntity<KakaoProfileDto> res = restTemplate.exchange(PROFILE_URL, HttpMethod.GET, req, KakaoProfileDto.class);
        return res.getBody();
    }

    /**
     * **관리자 권한 언링크** (동의 기록 제거)
     * - 대상 식별: user_id (socialId 가 카카오의 user id)
     * - 헤더: Authorization: KakaoAK {ADMIN_KEY}
     * - 성공 시 해당 앱과 사용자 연결이 해제되어, 다음 로그인에서 동의화면이 재노출됨
     */
    public void unlinkByAdmin(String kakaoUserId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", "KakaoAK " + adminKey); // ★ Admin Key 사용

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("target_id_type", "user_id");
            body.add("target_id", kakaoUserId);

            HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(body, headers);
            ResponseEntity<Map> res = restTemplate.postForEntity(URI.create(UNLINK_URL), req, Map.class);

            if (res.getStatusCode().is2xxSuccessful()) {
                log.info("[KAKAO][UNLINK] success user_id={}", kakaoUserId);
            } else {
                log.warn("[KAKAO][UNLINK] non-200 response. user_id={}, status={}", kakaoUserId, res.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            log.warn("[KAKAO][UNLINK] failed user_id={}, status={}, body={}", kakaoUserId, e.getRawStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.warn("[KAKAO][UNLINK] failed user_id={}, err={}", kakaoUserId, e.toString());
        }
    }
}
