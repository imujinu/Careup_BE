package com.careup.ordering.domain.auth.oauth.service;

import com.careup.ordering.common.auth.JwtProperties;
import com.careup.ordering.common.auth.JwtTokenProvider;
import com.careup.ordering.common.util.PhoneUtils;
import com.careup.ordering.domain.auth.oauth.client.GoogleOauthClient;
import com.careup.ordering.domain.auth.oauth.client.KakaoOauthClient;
import com.careup.ordering.domain.auth.oauth.dto.*;
import com.careup.ordering.domain.auth.oauth.store.OauthTempStore;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.entity.SocialAccount;
import com.careup.ordering.domain.member.entity.SocialProvider;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.member.repository.SocialAccountRepository;
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

    public OauthLoginResponse googleLogin(ProvideCodeRequest req) {
        OauthTokenDto token = googleClient.exchangeCode(req.getCode());
        GoogleProfileDto p = googleClient.getProfile(token.getAccessToken());

        SocialProvider provider = SocialProvider.GOOGLE;
        String socialId = p.getSub();
        String email = p.getEmail();
        String name = p.getName();
        String pic = p.getPicture();

        return handleAfterProfile(provider, socialId, email, name, pic);
    }

    public OauthLoginResponse kakaoLogin(ProvideCodeRequest req) {
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

    private OauthLoginResponse handleAfterProfile(SocialProvider provider, String socialId,
                                                  String email, String name, String profileImageUrl) {
        // 1) 기존 소셜 연결 여부
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

        // 2) 이메일 보유 회원과 연결(이미 가입한 사용자)
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

        // 3) 신규 가입(추가정보 필요) -> 임시 토큰 발급
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

        // 닉네임/이메일/휴대폰 중복 체크
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

        // 신규 Member 생성 (Member 엔티티 형태 그대로)
        String randomPw = "{noop}OAUTH_" + System.nanoTime(); // 실제 운영 시 강한 난수 + encoder 적용
        Member m = Member.builder()
                .email(payload.email() != null ? payload.email().trim()
                        : (payload.provider().name().toLowerCase() + "_" + payload.socialId() + "@placeholder.local"))
                .password(passwordEncoder.encode(randomPw))
                .nickname(req.getNickname().trim())
                .name(req.getName().trim())
                .birthday(req.getBirthday())
                .phone(normalizedPhone)
                .gender(req.getGender())
                .build();
        memberRepository.save(m);

        // 소셜 연결 생성
        socialAccountRepository.save(
                SocialAccount.builder()
                        .member(m)
                        .provider(payload.provider())
                        .socialId(payload.socialId())
                        .profileImageUrl(payload.profileImageUrl())
                        .build()
        );

        // 토큰 발급
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
}
