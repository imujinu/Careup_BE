// src/main/java/com/careup/ordering/domain/auth/oauth/service/OauthAuthService.java
package com.careup.ordering.domain.auth.oauth.service;

import com.careup.ordering.common.auth.JwtProperties;
import com.careup.ordering.common.auth.JwtTokenProvider;
import com.careup.ordering.common.service.EmailSender;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityExistsException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestClient;

@Slf4j
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

    private final EmailSender emailSender;

    // 환영 CTA/문의 메시지
    private final String welcomeCtaUrl = "https://ordering.careup.com/customer/home";
    private final String supportEmail = "support@careup.com";

    private final ObjectMapper om = new ObjectMapper();

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
        String refreshToken = token.getRefreshToken();

        if (Boolean.FALSE.equals(p.getEmail_verified())) {
            email = null;
        }
        return handleAfterProfile(provider, socialId, email, name, pic, refreshToken, null);
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
        String refreshToken = null;

        var acc = p.getKakao_account();
        if (acc != null) {
            boolean emailOk = Boolean.TRUE.equals(acc.getIs_email_valid()) && Boolean.TRUE.equals(acc.getIs_email_verified());
            if (emailOk) email = acc.getEmail();
            if (acc.getProfile() != null) {
                name = acc.getProfile().getNickname();
                pic = acc.getProfile().getProfile_image_url();
            }
        }
        String accessToken = token.getAccessToken(); // 톡 메모용

        return handleAfterProfile(provider, socialId, email, name, pic, refreshToken, accessToken);
    }

    private OauthLoginResponse handleAfterProfile(
            SocialProvider provider, String socialId, String email, String name, String profileImageUrl,
            String refreshToken, String accessTokenForTalkMemo) {

        var linkedOpt = socialAccountRepository.findByProviderAndSocialId(provider, socialId);
        if (linkedOpt.isPresent()) {
            SocialAccount linked = linkedOpt.get();
            Member m = linked.getMember();

            if (m.isDeactivated()) {
                socialAccountRepository.delete(linked);
                return startFirstJoinFlow(provider, socialId, email, name, profileImageUrl, refreshToken, accessTokenForTalkMemo);
            }

            if (provider == SocialProvider.GOOGLE && refreshToken != null && !refreshToken.isBlank()) {
                linked.updateRefreshToken(refreshToken);
                socialAccountRepository.save(linked);
            }
            // 기존 회원은 환영 톡 메모 X
            return finishLogin(m, provider.name(), socialId);
        }
        return startFirstJoinFlow(provider, socialId, email, name, profileImageUrl, refreshToken, accessTokenForTalkMemo);
    }

    private void verifyStateOrThrow(String state) {
        if (!stateStore.consume(state)) {
            throw new IllegalArgumentException("유효하지 않은 state 입니다. 다시 시도해 주세요.");
        }
    }

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
            SocialProvider provider, String socialId, String email, String name, String profileImageUrl,
            String refreshToken, String accessTokenForTalkMemo) {
        String temp = tempStore.save(new OauthTempStore.Payload(
                provider, socialId, email, name, profileImageUrl, refreshToken, accessTokenForTalkMemo
        ));
        return OauthLoginResponse.builder()
                .status("INCOMPLETE")
                .oauthTempToken(temp)
                .provider(provider.name())
                .socialId(socialId)
                .email(email)
                .name(name)
                .build();
    }

    @Transactional
    public OauthLoginResponse completeAdditionalInfo(OauthUpdateRequest req) {
        var payload = tempStore.require(req.getOauthTempToken());

        // [추가] 카카오가 이메일을 주지 않은 경우, 사용자 입력 이메일을 필수로 강제
        boolean providerEmailMissing = (payload.email() == null || payload.email().isBlank());
        boolean requestEmailMissing  = (req.getEmail() == null || req.getEmail().isBlank());
        if (payload.provider() == SocialProvider.KAKAO && providerEmailMissing && requestEmailMissing) {
            throw new IllegalArgumentException("카카오 계정에서 이메일이 제공되지 않았습니다. 이메일을 입력해 주세요.");
        }

        String nickname = req.getNickname().trim();

        String suppliedEmail = (req.getEmail() != null && !req.getEmail().isBlank())
                ? req.getEmail().trim() : null;
        String finalEmail = (payload.email() != null && !payload.email().isBlank())
                ? payload.email().trim()
                : (suppliedEmail != null ? suppliedEmail : placeholderEmail(payload.provider(), payload.socialId()));

        String normalizedPhone = PhoneUtils.normalize(req.getPhone());
        String zipcode = req.getZipcode().trim();
        String address = req.getAddress().trim();
        String addressDetail = req.getAddressDetail().trim();

        String displayName = buildDisplayName(req.getName().trim(), nickname);

        var emailOwnerOpt = memberRepository.findByEmailIgnoreCase(finalEmail);
        if (emailOwnerOpt.isPresent()) {
            Member emailOwner = emailOwnerOpt.get();
            if (emailOwner.isDeactivated()) {
                memberRepository.findByNickname(nickname).ifPresent(other -> {
                    if (!other.getId().equals(emailOwner.getId())) {
                        throw new EntityExistsException("이미 사용 중인 닉네임입니다.");
                    }
                });
                memberRepository.findByPhone(normalizedPhone).ifPresent(other -> {
                    if (!other.getId().equals(emailOwner.getId())) {
                        throw new EntityExistsException("이미 사용 중인 휴대폰 번호입니다.");
                    }
                });

                emailOwner.changePhone(normalizedPhone);
                emailOwner.changeProfile(nickname, req.getName().trim(), req.getBirthday(), req.getGender());
                emailOwner.changeAddress(zipcode, address, addressDetail);
                emailOwner.reactivate();
                memberRepository.save(emailOwner);

                upsertSocialAccount(payload, emailOwner);

                sendWelcomeAfterCommit(emailOwner.getEmail(), emailOwner.getName(), emailOwner.getNickname());
                if (payload.provider() == SocialProvider.KAKAO) {
                    sendKakaoWelcomeMemoAfterCommit(payload.accessToken(), displayName);
                }

                consumeAfterCommit(req.getOauthTempToken());
                return finishLogin(emailOwner, payload.provider().name(), payload.socialId());
            }
        }

        var phoneOwnerOpt = memberRepository.findByPhone(normalizedPhone);
        if (phoneOwnerOpt.isPresent()) {
            Member phoneOwner = phoneOwnerOpt.get();
            if (phoneOwner.isDeactivated()) {
                var emailOtherOpt = memberRepository.findByEmailIgnoreCase(finalEmail);
                if (emailOtherOpt.isPresent() && !emailOtherOpt.get().getId().equals(phoneOwner.getId())) {
                    throw new EntityExistsException("이미 사용 중인 이메일입니다.");
                }
                memberRepository.findByNickname(nickname).ifPresent(other -> {
                    if (!other.getId().equals(phoneOwner.getId())) {
                        throw new EntityExistsException("이미 사용 중인 닉네임입니다.");
                    }
                });

                phoneOwner.changeEmail(finalEmail);
                phoneOwner.changeProfile(nickname, req.getName().trim(), req.getBirthday(), req.getGender());
                phoneOwner.changeAddress(zipcode, address, addressDetail);
                phoneOwner.reactivate();
                memberRepository.save(phoneOwner);

                upsertSocialAccount(payload, phoneOwner);

                sendWelcomeAfterCommit(phoneOwner.getEmail(), phoneOwner.getName(), phoneOwner.getNickname());
                if (payload.provider() == SocialProvider.KAKAO) {
                    sendKakaoWelcomeMemoAfterCommit(payload.accessToken(), displayName);
                }

                consumeAfterCommit(req.getOauthTempToken());
                return finishLogin(phoneOwner, payload.provider().name(), payload.socialId());
            } else {
                throw new EntityExistsException("이미 사용 중인 휴대폰 번호입니다.");
            }
        }

        if (memberRepository.existsByEmailIgnoreCase(finalEmail)) {
            throw new EntityExistsException("이미 사용 중인 이메일입니다.");
        }
        memberRepository.findByNickname(nickname).ifPresent(m -> {
            throw new EntityExistsException("이미 사용 중인 닉네임입니다.");
        });
        memberRepository.findByPhone(normalizedPhone).ifPresent(m -> {
            throw new EntityExistsException("이미 사용 중인 휴대폰 번호입니다.");
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
                .zipcode(zipcode)
                .address(address)
                .addressDetail(addressDetail)
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

        sendWelcomeAfterCommit(m.getEmail(), m.getName(), m.getNickname());
        if (payload.provider() == SocialProvider.KAKAO) {
            sendKakaoWelcomeMemoAfterCommit(payload.accessToken(), displayName);
        }

        consumeAfterCommit(req.getOauthTempToken());
        return finishLogin(m, payload.provider().name(), payload.socialId());
    }

    private void upsertSocialAccount(OauthTempStore.Payload payload, Member owner) {
        if (!socialAccountRepository.existsByProviderAndSocialId(payload.provider(), payload.socialId())) {
            socialAccountRepository.save(
                    SocialAccount.builder()
                            .member(owner)
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
    }

    private void consumeAfterCommit(String token) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                tempStore.consume(token);
            }
        });
    }

    private void sendWelcomeAfterCommit(String email, String name, String nickname) {
        if (email == null || email.toLowerCase().endsWith("@placeholder.local")) {
            log.info("[MAIL][WELCOME] skipped(non-deliverable) email={}", email);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { emailSender.sendWelcomeEmail(email, name, nickname); }
                catch (Exception e) { log.warn("[MAIL][WELCOME] send failed to={} : {}", email, e.getMessage()); }
            }
        });
    }

    private void sendKakaoWelcomeMemoAfterCommit(String kakaoAccessToken, String displayName) {
        if (kakaoAccessToken == null || kakaoAccessToken.isBlank()) {
            log.info("[KAKAO][TALK] skipped: no access token");
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try {
                    Map<String, Object> link = new HashMap<>();
                    link.put("web_url", welcomeCtaUrl);
                    link.put("mobile_web_url", welcomeCtaUrl);

                    Map<String, Object> obj = new HashMap<>();
                    obj.put("object_type", "text");
                    obj.put("text",
                            "환영합니다 " + displayName + " 님.\n\n"
                                    + "SHARK 가입이 정상적으로 완료되었습니다.\n\n"
                                    + "바로가기: " + welcomeCtaUrl + "\n"
                                    + "문의: " + supportEmail);
                    obj.put("link", link);
                    obj.put("button_title", "서비스 바로가기");

                    String templateJson = om.writeValueAsString(obj);
                    String body = "template_object=" + URLEncoder.encode(templateJson, StandardCharsets.UTF_8);

                    RestClient.create("https://kapi.kakao.com")
                            .post()
                            .uri("/v2/api/talk/memo/default/send")
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .header("Authorization", "Bearer " + kakaoAccessToken)
                            .body(body)
                            .retrieve()
                            .toBodilessEntity();

                    log.info("[KAKAO][TALK] welcome memo sent");
                } catch (Exception e) {
                    log.warn("[KAKAO][TALK] memo send failed: {}", e.getMessage());
                }
            }
        });
    }

    private String buildDisplayName(String name, String nickname) {
        String n = name == null ? "" : name.trim();
        String nn = nickname == null ? "" : nickname.trim();
        if (!nn.isBlank() && !nn.equalsIgnoreCase(n)) return n.isBlank() ? nn : n + " (" + nn + ")";
        return n.isBlank() ? nn : n;
    }

    private String placeholderEmail(SocialProvider provider, String socialId) {
        int fallback = Math.abs((provider.name() + ":" + socialId).hashCode());
        return provider.name().toLowerCase() + "_" + fallback + "@placeholder.local";
    }
}
