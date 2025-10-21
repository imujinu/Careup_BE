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
            if (m.isDeactivated()) {
                m.reactivate();
                memberRepository.save(m);
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

        // (B) 같은 이메일의 기존 회원이 있으면 → 비활성이어도 즉시 재활성화 후 링크 & 로그인
        if (email != null && !email.isBlank()) {
            var owner = memberRepository.findByEmailIgnoreCase(email).orElse(null);
            if (owner != null) {
                if (owner.isDeactivated()) {
                    owner.reactivate();
                    memberRepository.save(owner);
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

        // (C) 신규 가입(추가정보 흐름으로 위임)
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

        String nickname = req.getNickname().trim();
        String finalEmail = (payload.email() != null && !payload.email().isBlank())
                ? payload.email().trim()
                : placeholderEmail(payload.provider(), payload.socialId());
        String normalizedPhone = PhoneUtils.normalize(req.getPhone());

        // 1) 이메일 기준 재활성화 후보
        var emailOwnerOpt = memberRepository.findByEmailIgnoreCase(finalEmail);
        if (emailOwnerOpt.isPresent()) {
            Member emailOwner = emailOwnerOpt.get();
            if (emailOwner.isDeactivated()) {
                // 닉네임/휴대폰 충돌(본인 제외) 확인
                memberRepository.findByNickname(nickname).ifPresent(other -> {
                    if (!other.getId().equals(emailOwner.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                    }
                });
                memberRepository.findByPhone(normalizedPhone).ifPresent(other -> {
                    if (!other.getId().equals(emailOwner.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
                    }
                });

                emailOwner.changePhone(normalizedPhone);
                emailOwner.changeProfile(nickname, req.getName().trim(), req.getBirthday(), req.getGender());
                emailOwner.reactivate();
                memberRepository.save(emailOwner);

                // 소셜 링크
                socialAccountRepository.save(
                        SocialAccount.builder()
                                .member(emailOwner)
                                .provider(payload.provider())
                                .socialId(payload.socialId())
                                .profileImageUrl(payload.profileImageUrl())
                                .build()
                );

                String at = jwt.createAccessToken(emailOwner.getId(), "CUSTOMER");
                String rt = jwt.createRefreshToken(emailOwner.getId(), true);
                return OauthLoginResponse.builder()
                        .status("COMPLETE")
                        .tokenType("Bearer")
                        .accessToken(at)
                        .refreshToken(rt)
                        .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                        .memberId(emailOwner.getId())
                        .role("CUSTOMER")
                        .email(emailOwner.getEmail())
                        .name(emailOwner.getName())
                        .nickname(emailOwner.getNickname())
                        .phone(emailOwner.getPhone())
                        .provider(payload.provider().name())
                        .socialId(payload.socialId())
                        .build();
            }
        }

        // 2) 휴대폰 기준 재활성화 후보
        var phoneOwnerOpt = memberRepository.findByPhone(normalizedPhone);
        if (phoneOwnerOpt.isPresent()) {
            Member phoneOwner = phoneOwnerOpt.get();
            if (phoneOwner.isDeactivated()) {
                // 이메일이 타인에게 점유되었는지 확인(본인 제외)
                var emailOtherOpt = memberRepository.findByEmailIgnoreCase(finalEmail);
                if (emailOtherOpt.isPresent() && !emailOtherOpt.get().getId().equals(phoneOwner.getId())) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
                // 닉네임 충돌(본인 제외)
                memberRepository.findByNickname(nickname).ifPresent(other -> {
                    if (!other.getId().equals(phoneOwner.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                    }
                });

                phoneOwner.changeEmail(finalEmail);
                phoneOwner.changeProfile(nickname, req.getName().trim(), req.getBirthday(), req.getGender());
                phoneOwner.reactivate();
                memberRepository.save(phoneOwner);

                socialAccountRepository.save(
                        SocialAccount.builder()
                                .member(phoneOwner)
                                .provider(payload.provider())
                                .socialId(payload.socialId())
                                .profileImageUrl(payload.profileImageUrl())
                                .build()
                );

                String at = jwt.createAccessToken(phoneOwner.getId(), "CUSTOMER");
                String rt = jwt.createRefreshToken(phoneOwner.getId(), true);
                return OauthLoginResponse.builder()
                        .status("COMPLETE")
                        .tokenType("Bearer")
                        .accessToken(at)
                        .refreshToken(rt)
                        .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                        .memberId(phoneOwner.getId())
                        .role("CUSTOMER")
                        .email(phoneOwner.getEmail())
                        .name(phoneOwner.getName())
                        .nickname(phoneOwner.getNickname())
                        .phone(phoneOwner.getPhone())
                        .provider(payload.provider().name())
                        .socialId(payload.socialId())
                        .build();
            } else {
                // 이미 정상 계정이 해당 휴대폰을 점유
                throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
            }
        }

        // 3) 신규 생성 (중복 체크)
        if (memberRepository.existsByEmailIgnoreCase(finalEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        memberRepository.findByPhone(normalizedPhone).ifPresent(m -> {
            throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
        });

        String randomPw = "OAUTH_" + UUID.randomUUID();

        Member m = Member.builder()
                .email(finalEmail)
                .password(passwordEncoder.encode(randomPw))
                .nickname(nickname)
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
