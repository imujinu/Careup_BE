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

    /** 프론트가 선발급 요청하는 state */
    public String issueState() {
        return stateStore.issue();
    }

    public OauthLoginResponse googleLogin(ProvideCodeRequest req) {
        verifyStateOrThrow(req.getState());

        OauthTokenDto token = googleClient.exchangeCode(req.getCode(), req.getCodeVerifier());
        GoogleProfileDto p = googleClient.getProfile(token.getAccessToken());

        SocialProvider provider = SocialProvider.GOOGLE;
        String socialId = p.getSub();
        String email = p.getEmail();
        String name = p.getName();
        String pic = p.getPicture();

        return handleAfterProfile(provider, socialId, email, name, pic);
    }

    public OauthLoginResponse kakaoLogin(ProvideCodeRequest req) {
        verifyStateOrThrow(req.getState());

        OauthTokenDto token = kakaoClient.exchangeCode(req.getCode());
        KakaoProfileDto p = kakaoClient.getProfile(token.getAccessToken());

        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = p.getId();
        String email = (p.getKakao_account() != null) ? p.getKakao_account().getEmail() : null;
        String name = (p.getKakao_account() != null && p.getKakao_account().getProfile() != null)
                ? p.getKakao_account().getProfile().getNickname() : null;
        String pic = (p.getKakao_account() != null && p.getKakao_account().getProfile() != null)
                ? p.getKakao_account().getProfile().getProfile_image_url() : null;

        return handleAfterProfile(provider, socialId, email, name, pic);
    }

    private void verifyStateOrThrow(String state) {
        if (!stateStore.consume(state)) {
            throw new IllegalArgumentException("유효하지 않은 state 입니다. 다시 시도해 주세요.");
        }
    }

    private OauthLoginResponse handleAfterProfile(SocialProvider provider, String socialId,
                                                  String email, String name, String profileImageUrl) {
        var linked = socialAccountRepository.findByProviderAndSocialId(provider, socialId).orElse(null);
        if (linked != null) {
            Member m = linked.getMember();
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

        if (email != null && !email.isBlank()) {
            var owner = memberRepository.findByEmailIgnoreCase(email).orElse(null);
            if (owner != null) {
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

    /** 이메일 컬럼(50자) 내에서 충돌·길이 안전한 placeholder 생성 */
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
