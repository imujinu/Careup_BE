package com.careup.ordering.domain.member.service;

import com.careup.ordering.common.auth.CustomerRevokeStore;
import com.careup.ordering.common.auth.JwtTokenProvider;
import com.careup.ordering.common.util.PhoneUtils;
import com.careup.ordering.domain.member.dto.request.CustomerIdentityChangeRequest;
import com.careup.ordering.domain.member.dto.response.CustomerIdentityChangeResponse;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerIdentityCommandService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRevokeStore revokeStore;
    private final JwtTokenProvider jwt;

    public CustomerIdentityChangeResponse changeMyIdentity(CustomerIdentityChangeRequest req) {
        Long meId = readMemberId();
        Member me = memberRepository.findById(meId)
                .orElseThrow(() -> new EntityNotFoundException("회원을 찾을 수 없습니다."));

        if (!passwordEncoder.matches(req.getCurrentPassword(), me.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (hasText(req.getNewEmail())) {
            final String newEmail = req.getNewEmail().trim();
            memberRepository.findByEmailIgnoreCase(newEmail).ifPresent(other -> {
                if (!other.getId().equals(me.getId())) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
            });
            me.changeEmail(newEmail);
        }

        if (hasText(req.getNewPhone())) {
            final String normalized = PhoneUtils.normalize(req.getNewPhone());
            memberRepository.findByPhone(normalized).ifPresent(other -> {
                if (!other.getId().equals(me.getId())) {
                    throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
                }
            });
            me.changePhone(normalized);
        }

        memberRepository.save(me);

        // 보안 컷오프 + RT 폐기: 10초 지연 후 적용
        revokeStore.scheduleCutoverAfterSeconds(meId, 10);
        jwt.revokeRefreshToken(meId);

        return CustomerIdentityChangeResponse.builder()
                .email(me.getEmail())
                .phone(me.getPhone())
                .build();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * JwtTokenFilter 컨벤션:
     *  - details: Map { realm="CUS" | "EMP", ... }
     *  - principal: Long(고객 ID) 또는 String 숫자
     */
    private Long readMemberId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }

        Object details = a.getDetails();
        if (!(details instanceof Map<?, ?> m)) {
            throw new AuthenticationCredentialsNotFoundException("인증 컨텍스트 형식이 올바르지 않습니다.");
        }

        Object realm = m.get("realm");
        if (!"CUS".equals(realm)) {
            throw new AccessDeniedException("고객 영역에서만 호출할 수 있습니다.");
        }

        Object principal = a.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof String s && s.matches("\\d+")) {
            return Long.valueOf(s);
        }
        throw new AuthenticationCredentialsNotFoundException("고객 식별자를 확인할 수 없습니다.");
    }
}
