package com.careup.ordering.domain.member.service;

import com.careup.ordering.common.auth.CustomerRevokeStore;
import com.careup.ordering.common.auth.JwtTokenProvider;
import com.careup.ordering.domain.auth.oauth.client.KakaoOauthClient;
import com.careup.ordering.domain.auth.oauth.client.GoogleOauthClient;
import com.careup.ordering.domain.member.dto.request.CustomerWithdrawRequest;
import com.careup.ordering.domain.member.dto.response.CustomerWithdrawResponse;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.entity.SocialAccount;
import com.careup.ordering.domain.member.entity.SocialProvider;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.member.repository.SocialAccountRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerWithdrawalService {

    private final MemberRepository memberRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRevokeStore revokeStore;
    private final JwtTokenProvider jwt;

    private final KakaoOauthClient kakaoClient;
    private final GoogleOauthClient googleClient;

    public CustomerWithdrawResponse withdraw(CustomerWithdrawRequest req) {
        Long meId = readCustomerIdFromContext();

        Member me = memberRepository.findById(meId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        if (me.isDeactivated()) {
            throw new IllegalArgumentException("이미 탈퇴(비활성)된 계정입니다.");
        }

        boolean hasSocial = socialAccountRepository.existsByMember(me);

        if (!hasSocial) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                throw new IllegalArgumentException("현재 비밀번호가 필요합니다.");
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), me.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
        } else {
            if (req.getCurrentPassword() != null && !req.getCurrentPassword().isBlank()) {
                if (!passwordEncoder.matches(req.getCurrentPassword(), me.getPassword())) {
                    throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
                }
            }
        }

        List<SocialAccount> links = socialAccountRepository.findAllByMember(me);
        for (SocialAccount link : links) {
            if (link.getProvider() == SocialProvider.KAKAO) {
                try {
                    kakaoClient.unlinkByAdmin(link.getSocialId());
                } catch (Exception e) {
                    log.warn("[KAKAO][UNLINK] failed user_id={}, err={}", link.getSocialId(), e.toString());
                }
            } else if (link.getProvider() == SocialProvider.GOOGLE) {
                try {
                    String rt = link.getProviderRefreshToken();
                    if (rt != null && !rt.isBlank()) {
                        googleClient.revokeRefreshToken(rt);
                    } else {
                        log.warn("[GOOGLE][REVOKE] refresh_token not stored for socialId={}", link.getSocialId());
                    }
                } catch (Exception e) {
                    log.warn("[GOOGLE][REVOKE] failed socialId={}, err={}", link.getSocialId(), e.toString());
                }
            }
        }
        socialAccountRepository.deleteAll(links);

        me.deactivate();
        memberRepository.save(me);

        revokeStore.scheduleCutoverAfterSeconds(meId, 10);
        jwt.revokeRefreshToken(meId);

        return CustomerWithdrawResponse.builder()
                .deactivated(true)
                .memberId(me.getId())
                .email(me.getEmail())
                .deactivatedAt(Instant.now())
                .build();
    }

    private Long readCustomerIdFromContext() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = a.getDetails();
        if (!(details instanceof Map<?, ?> m)) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object realm = m.get("realm");
        if (!"CUS".equals(realm)) {
            throw new AuthenticationCredentialsNotFoundException("고객 권한이 필요합니다.");
        }

        Object principal = a.getPrincipal();
        if (principal instanceof Long id) return id;
        if (principal instanceof String s && s.matches("\\d+")) return Long.valueOf(s);
        throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
    }
}
