package com.careup.ordering.domain.auth.oauth.service;

import com.careup.ordering.common.auth.JwtProperties;
import com.careup.ordering.common.auth.JwtTokenProvider;
import com.careup.ordering.common.util.PhoneUtils;
import com.careup.ordering.domain.auth.oauth.client.GoogleOauthClient;
import com.careup.ordering.domain.auth.oauth.client.KakaoOauthClient;
import com.careup.ordering.domain.auth.oauth.dto.GoogleProfileDto;
import com.careup.ordering.domain.auth.oauth.dto.KakaoProfileDto;
import com.careup.ordering.domain.auth.oauth.dto.OauthLoginResponse;
import com.careup.ordering.domain.auth.oauth.dto.OauthTokenDto;
import com.careup.ordering.domain.auth.oauth.dto.OauthUpdateRequest;
import com.careup.ordering.domain.auth.oauth.dto.ProvideCodeRequest;
import com.careup.ordering.domain.auth.oauth.store.OauthStateStore;
import com.careup.ordering.domain.auth.oauth.store.OauthTempStore;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.entity.SocialAccount;
import com.careup.ordering.domain.member.entity.SocialProvider;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.member.repository.SocialAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OauthAuthService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProps;

    private final GoogleOauthClient googleClient;
    private final KakaoOauthClient kakaoClient;
    private final OauthTempStore tempStore;
    private final OauthStateStore stateStore;

    public String issueState() { return stateStore.issue(); }

    public OauthLoginResponse googleLogin(ProvideCodeRequest req) {
        verifyStateOrThrow(req.getState());

        OauthTokenDto token = googleClient.exchangeCode(req.getCode(), req.getCodeVerifier());
        GoogleProfileDto p = googleClient.getProfile(token.getAccessToken());

        SocialProvider provider = SocialProvider.GOOGLE;
        String socialId = p.getSub();
        String email = p.getEmail();
        String name = p.getName();
        String pic = p.getPicture();

        if (Boolean.FALSE.equals(p.getEmail_verified())) {
            email = null;
        }

        return handleAfterProfile(provider, socialId, email, name, pic);
    }

    public OauthLoginResponse kakaoLogin(ProvideCodeRequest req) {
        verifyStateOrThrow(req.getState());

        OauthTokenDto token = kakaoClient.exchangeCode(req.getCode());
        KakaoProfileDto p = kakaoClient.getProfile(token.getAccessToken());

        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = p.getId();

        String email = null;
        String name = null;
        String pic = null;

        var acc = p.getKakao_account();
        if (acc != null) {
            boolean emailOk = Boolean.TRUE.equals(acc.getIs_email_valid()) && Boolean.TRUE.equals(acc.getIs_email_verified());
            if (emailOk) {
                email = acc.getEmail();
            }
            if (acc.getProfile() != null) {
                name = acc.getProfile().getNickname();
                pic = acc.getProfile().getProfile_image_url();
            }
        }

        return handleAfterProfile(provider, socialId, email, name, pic);
    }

    private void verifyStateOrThrow(String state) {
        if (!stateStore.consume(state)) {
            throw new IllegalArgumentException("유효하지 않은 state 입니다. 다시 시도해 주세요.");
        }
    }

    private OauthLoginResponse handleAfterProfile(SocialProvider provider, String socialId,
                                                  String email, String name, String profileImageUrl) {
        // (A) 이미 소셜 계정 연결된 경우
        var linked = socialAccountRepository.findByProviderAndSocialId(provider, socialId).orElse(null);
        if (linked != null) {
            Member m = linked.getMember();
            // 비활성 계정 차단
            if (!"N".equalsIgnoreCase(m.getIsDelYn())) {
                throw new IllegalArgumentException("비활성화된 계정입니다.");
            }

            String at = jwt.createAccessToken(m.getId(), "CUSTOMER");
            String rt = jwt.createRefreshToken(m.getId(), true);
            return OauthLoginResponse.builder()
                    .status("COMPLETE")
                    .tokenType("Bearer")
                    .accessToken(at)
                    .refreshToken(rt)
                    .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                    .memberId(m.getId())
                    .role("CUSTOMER")
                    .email(m.getEmail())
                    .name(m.getName())
                    .nickname(m.getNickname())
                    .phone(m.getPhone())
                    .provider(provider.name())
                    .socialId(socialId)
                    .build();
        }

        // (B) 같은 이메일의 기존 회원이 있으면 연결 후 로그인
        if (email != null && !email.isBlank()) {
            var owner = memberRepository.findByEmailIgnoreCase(email).orElse(null);
            if (owner != null) {
                // 비활성 계정 차단
                if (!"N".equalsIgnoreCase(owner.getIsDelYn())) {
                    throw new IllegalArgumentException("비활성화된 계정입니다.");
                }

                socialAccountRepository.save(SocialAccount.builder()
                        .member(owner)
                        .provider(provider)
                        .socialId(socialId)
                        .profileImageUrl(profileImageUrl)
                        .build());

                String at = jwt.createAccessToken(owner.getId(), "CUSTOMER");
                String rt = jwt.createRefreshToken(owner.getId(), true);
                return OauthLoginResponse.builder()
                        .status("COMPLETE")
                        .tokenType("Bearer")
                        .accessToken(at)
                        .refreshToken(rt)
                        .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                        .memberId(owner.getId())
                        .role("CUSTOMER")
                        .email(owner.getEmail())
                        .name(owner.getName())
                        .nickname(owner.getNickname())
                        .phone(owner.getPhone())
                        .provider(provider.name())
                        .socialId(socialId)
                        .build();
            }
        }

        // (C) 신규 가입(추가정보 흐름)
        String temp = tempStore.save(new OauthTempStore.Payload(provider, socialId, email, name, profileImageUrl));
        return OauthLoginResponse.builder()
                .status("INCOMPLETE")
                .oauthTempToken(temp)
                .provider(provider.name())
                .socialId(socialId)
                .email(email)
                .name(name)
                .build();
    }

    public OauthLoginResponse completeAdditionalInfo(OauthUpdateRequest req) {
        var payload = tempStore.consume(req.getOauthTempToken())
                .orElseThrow(() -> new IllegalArgumentException("임시 토큰이 만료되었거나 유효하지 않습니다. 다시 시도해 주세요."));

        if (memberRepository.existsByNickname(req.getNickname().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        if (payload.email() != null && memberRepository.existsByEmailIgnoreCase(payload.email().trim())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        var normalizedPhone = PhoneUtils.normalize(req.getPhone());
        memberRepository.findByPhone(normalizedPhone).ifPresent(m -> {
            throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
        });

        String randomPw = "OAUTH_" + UUID.randomUUID();
        String finalEmail = (payload.email() != null && !payload.email().isBlank())
                ? payload.email().trim()
                : placeholderEmail(payload.provider(), payload.socialId());

        Member m = Member.builder()
                .email(finalEmail)
                .password(passwordEncoder.encode(randomPw))
                .nickname(req.getNickname().trim())
                .name(req.getName().trim())
                .birthday(req.getBirthday())
                .phone(normalizedPhone)
                .gender(req.getGender())
                .build();
        memberRepository.save(m);

        socialAccountRepository.save(
                SocialAccount.builder()
                        .member(m)
                        .provider(payload.provider())
                        .socialId(payload.socialId())
                        .profileImageUrl(payload.profileImageUrl())
                        .build()
        );

        String at = jwt.createAccessToken(m.getId(), "CUSTOMER");
        String rt = jwt.createRefreshToken(m.getId(), true);

        return OauthLoginResponse.builder()
                .status("COMPLETE")
                .tokenType("Bearer")
                .accessToken(at)
                .refreshToken(rt)
                .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                .memberId(m.getId())
                .role("CUSTOMER")
                .email(m.getEmail())
                .name(m.getName())
                .nickname(m.getNickname())
                .phone(m.getPhone())
                .provider(payload.provider().name())
                .socialId(payload.socialId())
                .build();
    }

    private String placeholderEmail(SocialProvider provider, String socialId) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((provider.name() + ":" + socialId).getBytes(StandardCharsets.UTF_8));
            String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(digest).substring(0, 22);
            return provider.name().toLowerCase() + "_" + hash + "@placeholder.local";
        } catch (Exception e) {
            int fallback = Math.abs((provider.name() + ":" + socialId).hashCode());
            return provider.name().toLowerCase() + "_" + fallback + "@placeholder.local";
        }
    }
}
