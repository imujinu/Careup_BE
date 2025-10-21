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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    // ===================== Google =====================
    public OauthLoginResponse googleLogin(ProvideCodeRequest req) {
        verifyStateOrThrow(req.getState());

        OauthTokenDto token = googleClient.exchangeCode(req.getCode(), req.getCodeVerifier());
        GoogleProfileDto p = googleClient.getProfile(token.getAccessToken());

        SocialProvider provider = SocialProvider.GOOGLE;
        String socialId = p.getSub();
        String email = p.getEmail();
        String name = p.getName();
        String pic = p.getPicture();
        String refreshToken = token.getRefreshToken(); // 처음 동의 시에만 내려올 수 있음

        if (Boolean.FALSE.equals(p.getEmail_verified())) {
            email = null;
        }

        return handleAfterProfile(provider, socialId, email, name, pic, refreshToken);
    }

    // ===================== Kakao =====================
    public OauthLoginResponse kakaoLogin(ProvideCodeRequest req) {
        verifyStateOrThrow(req.getState());

        OauthTokenDto token = kakaoClient.exchangeCode(req.getCode());
        KakaoProfileDto p = kakaoClient.getProfile(token.getAccessToken());

        SocialProvider provider = SocialProvider.KAKAO;
        String socialId = p.getId();

        String email = null;
        String name = null;
        String pic = null;
        String refreshToken = null; // Kakao는 AdminKey로 언링크할 것이므로 저장 불필요

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

        return handleAfterProfile(provider, socialId, email, name, pic, refreshToken);
    }

    // ===================== 공통 후처리 =====================
    private OauthLoginResponse handleAfterProfile(
            SocialProvider provider, String socialId, String email, String name, String profileImageUrl, String refreshToken) {

        var linkedOpt = socialAccountRepository.findByProviderAndSocialId(provider, socialId);

        if (linkedOpt.isPresent()) {
            SocialAccount linked = linkedOpt.get();
            Member m = linked.getMember();

            if (m.isDeactivated()) {
                // 비활성 계정이면 매핑 제거 후 최초가입(INCOMPLETE)로
                socialAccountRepository.delete(linked);
                return startFirstJoinFlow(provider, socialId, email, name, profileImageUrl, refreshToken);
            }

            // 활성 계정이면 정상 로그인 + Google refresh_token 갱신(있다면)
            if (provider == SocialProvider.GOOGLE && refreshToken != null && !refreshToken.isBlank()) {
                linked.updateRefreshToken(refreshToken);
                socialAccountRepository.save(linked);
            }
            return finishLogin(m, provider.name(), socialId);
        }

        // 매핑 없음 → 최초가입(INCOMPLETE), 필요한 경우 refresh_token도 임시 보관
        return startFirstJoinFlow(provider, socialId, email, name, profileImageUrl, refreshToken);
    }

    private void verifyStateOrThrow(String state) {
        if (!stateStore.consume(state)) {
            throw new IllegalArgumentException("유효하지 않은 state 입니다. 다시 시도해 주세요.");
        }
    }

    // ===================== 응답 빌더 =====================
    private OauthLoginResponse finishLogin(Member m, String providerName, String socialId) {
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
                .provider(providerName)
                .socialId(socialId)
                .build();
    }

    private OauthLoginResponse startFirstJoinFlow(
            SocialProvider provider, String socialId, String email, String name, String profileImageUrl, String refreshToken) {
        String temp = tempStore.save(new OauthTempStore.Payload(provider, socialId, email, name, profileImageUrl, refreshToken));
        return OauthLoginResponse.builder()
                .status("INCOMPLETE")
                .oauthTempToken(temp)
                .provider(provider.name())
                .socialId(socialId)
                .email(email)
                .name(name)
                .build();
    }

    // ===================== 추가정보 완료 =====================
    public OauthLoginResponse completeAdditionalInfo(OauthUpdateRequest req) {
        var payload = tempStore.consume(req.getOauthTempToken())
                .orElseThrow(() -> new IllegalArgumentException("임시 토큰이 만료되었거나 유효하지 않습니다. 다시 시도해 주세요."));

        String nickname = req.getNickname().trim();
        String finalEmail = (payload.email() != null && !payload.email().isBlank())
                ? payload.email().trim()
                : placeholderEmail(payload.provider(), payload.socialId());
        String normalizedPhone = PhoneUtils.normalize(req.getPhone());

        // (1) 이메일 기준 재활성화 후보
        var emailOwnerOpt = memberRepository.findByEmailIgnoreCase(finalEmail);
        if (emailOwnerOpt.isPresent()) {
            Member emailOwner = emailOwnerOpt.get();
            if (emailOwner.isDeactivated()) {
                // 닉네임/휴대폰 충돌 검사
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

                if (!socialAccountRepository.existsByProviderAndSocialId(payload.provider(), payload.socialId())) {
                    socialAccountRepository.save(
                            SocialAccount.builder()
                                    .member(emailOwner)
                                    .provider(payload.provider())
                                    .socialId(payload.socialId())
                                    .profileImageUrl(payload.profileImageUrl())
                                    .providerRefreshToken(payload.refreshToken())
                                    .build()
                    );
                } else {
                    // 존재한다면 Google refresh_token 갱신 가능
                    socialAccountRepository.findByProviderAndSocialId(payload.provider(), payload.socialId())
                            .ifPresent(sa -> {
                                sa.updateRefreshToken(payload.refreshToken());
                                socialAccountRepository.save(sa);
                            });
                }

                return finishLogin(emailOwner, payload.provider().name(), payload.socialId());
            }
        }

        // (2) 휴대폰 기준 재활성화 후보
        var phoneOwnerOpt = memberRepository.findByPhone(normalizedPhone);
        if (phoneOwnerOpt.isPresent()) {
            Member phoneOwner = phoneOwnerOpt.get();
            if (phoneOwner.isDeactivated()) {
                var emailOtherOpt = memberRepository.findByEmailIgnoreCase(finalEmail);
                if (emailOtherOpt.isPresent() && !emailOtherOpt.get().getId().equals(phoneOwner.getId())) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
                memberRepository.findByNickname(nickname).ifPresent(other -> {
                    if (!other.getId().equals(phoneOwner.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                    }
                });

                phoneOwner.changeEmail(finalEmail);
                phoneOwner.changeProfile(nickname, req.getName().trim(), req.getBirthday(), req.getGender());
                phoneOwner.reactivate();
                memberRepository.save(phoneOwner);

                if (!socialAccountRepository.existsByProviderAndSocialId(payload.provider(), payload.socialId())) {
                    socialAccountRepository.save(
                            SocialAccount.builder()
                                    .member(phoneOwner)
                                    .provider(payload.provider())
                                    .socialId(payload.socialId())
                                    .profileImageUrl(payload.profileImageUrl())
                                    .providerRefreshToken(payload.refreshToken())
                                    .build()
                    );
                } else {
                    socialAccountRepository.findByProviderAndSocialId(payload.provider(), payload.socialId())
                            .ifPresent(sa -> {
                                sa.updateRefreshToken(payload.refreshToken());
                                socialAccountRepository.save(sa);
                            });
                }

                return finishLogin(phoneOwner, payload.provider().name(), payload.socialId());
            } else {
                throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
            }
        }

        // (3) 신규 생성 — 중복 체크
        if (memberRepository.existsByEmailIgnoreCase(finalEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        memberRepository.findByNickname(nickname).ifPresent(m -> {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        });
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
                        .providerRefreshToken(payload.refreshToken())
                        .build()
        );

        return finishLogin(m, payload.provider().name(), payload.socialId());
    }

    // ===================== 유틸 =====================
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
