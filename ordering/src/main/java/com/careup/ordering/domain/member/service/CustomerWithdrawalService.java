package com.careup.ordering.domain.member.service;

import com.careup.ordering.common.auth.CustomerRevokeStore;
import com.careup.ordering.common.auth.JwtTokenProvider;
import com.careup.ordering.domain.auth.oauth.client.KakaoOauthClient;
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

    /** 카카오 언링크(Disconnect) 클라이언트 */
    private final KakaoOauthClient kakaoClient;

    public CustomerWithdrawResponse withdraw(CustomerWithdrawRequest req) {
        Long meId = readCustomerIdFromContext();

        Member me = memberRepository.findById(meId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        if (me.isDeactivated()) {
            throw new IllegalArgumentException("이미 탈퇴(비활성)된 계정입니다.");
        }

        // 소셜 연동 여부
        boolean hasSocial = socialAccountRepository.existsByMember(me);

        // 검증 규칙:
        //  - 소셜 연동 없음(순수 일반 계정): 비밀번호 필수
        //  - 소셜 연동 있음: 비밀번호 없이도 허용 (OAuth 가입자는 랜덤PW일 수 있음)
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

        // (1) 카카오 언링크: 서버 Admin Key로 사용자를 앱에서 연결 해제 → 다음 로그인 시 동의창 재노출
        List<SocialAccount> links = socialAccountRepository.findAllByMember(me);
        for (SocialAccount link : links) {
            if (link.getProvider() == SocialProvider.KAKAO) {
                String kakaoUserId = link.getSocialId();
                kakaoClient.unlinkByAdmin(kakaoUserId);
            }
            // (선택) GOOGLE은 이미 동의창 재노출 이슈가 적어 별도 처리 생략
            // 필요시 Google Grant Revoke 로직을 추가할 수 있습니다.
        }

        // (2) 소프트 삭제 (isDelYn = 'Y')
        me.deactivate();
        memberRepository.save(me);

        // (3) 즉시 세션 무효화 전략 (10초 후 AT 컷오프 + RT 폐기)
        revokeStore.scheduleCutoverAfterSeconds(meId, 10);
        jwt.revokeRefreshToken(meId);

        // (4) 소셜 매핑은 남겨둠(선택). 남겨두면 재로그인 시 자동 재활성화 정책 사용 가능.
        // 개인정보 최소화가 필요하면 아래 주석 해제로 매핑 삭제.
        // socialAccountRepository.deleteAll(links);

        return CustomerWithdrawResponse.builder()
                .deactivated(true)
                .memberId(me.getId())
                .email(me.getEmail())
                .deactivatedAt(Instant.now())
                .build();
    }

    /** JwtTokenFilter 컨벤션에 맞춘 고객 ID 추출 */
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
