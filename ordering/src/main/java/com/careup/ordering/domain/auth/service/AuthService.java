package com.careup.ordering.domain.auth.service;

import com.careup.ordering.common.auth.CustomerRevokeStore;
import com.careup.ordering.common.auth.JwtProperties;
import com.careup.ordering.common.auth.JwtTokenProvider;
import com.careup.ordering.common.auth.PasswordResetTokenStore;
import com.careup.ordering.common.service.EmailSender;
import com.careup.ordering.common.util.PhoneUtils;
import com.careup.ordering.domain.auth.dto.request.AuthLoginRequest;
import com.careup.ordering.domain.auth.dto.request.SignUpRequest;
import com.careup.ordering.domain.auth.dto.response.AuthLoginResponse;
import com.careup.ordering.domain.auth.dto.response.AuthLogoutResponse;
import com.careup.ordering.domain.auth.dto.response.AuthRefreshResponse;
import com.careup.ordering.domain.auth.dto.response.SignUpResponse;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProps;
    private final PasswordResetTokenStore resetTokenStore;
    private final EmailSender emailSender;
    private final CustomerRevokeStore revokeStore;

    @Transactional
    public SignUpResponse signUp(SignUpRequest req) {
        String email = req.getEmail().trim();
        String nickname = req.getNickname().trim();
        String phoneNorm = PhoneUtils.normalize(req.getPhone());

        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        if (memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        if (!isStrongPassword(req.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문/숫자를 포함해야 합니다.");
        }

        Member m = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(nickname)
                .name(req.getName().trim())
                .birthday(req.getBirthday())
                .phone(phoneNorm)
                .gender(req.getGender())
                .build();

        memberRepository.save(m);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    emailSender.sendWelcomeEmail(m.getEmail(), m.getName());
                } catch (Exception e) {
                    log.warn("[MAIL][WELCOME] send failed to={} : {}", m.getEmail(), e.getMessage());
                }
            }
        });

        return SignUpResponse.builder()
                .memberId(m.getId())
                .email(m.getEmail())
                .nickname(m.getNickname())
                .build();
    }

    public AuthLoginResponse login(AuthLoginRequest req) {
        if (req.getId() == null || req.getId().isBlank())
            throw new IllegalArgumentException("아이디(이메일 또는 휴대폰 번호)는 필수입니다.");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new IllegalArgumentException("비밀번호는 필수입니다.");

        final String rawId = req.getId().trim();
        final boolean emailLogin = rawId.contains("@");

        Member member = emailLogin
                ? memberRepository.findByEmailIgnoreCase(rawId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."))
                : memberRepository.findByPhone(PhoneUtils.normalize(rawId))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 휴대폰 번호입니다."));

        if (!"N".equals(member.getIsDelYn())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }
        if (!passwordEncoder.matches(req.getPassword(), member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String role = "CUSTOMER";
        Long memberId = member.getId();

        String at = jwt.createAccessToken(memberId, role);
        String rt = jwt.createRefreshToken(memberId, req.isRememberMe());

        return AuthLoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(at)
                .refreshToken(rt)
                .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                .role(role)
                .memberId(memberId)
                .name(member.getName())
                .email(member.getEmail())
                .nickname(member.getNickname())
                .phone(member.getPhone())
                .build();
    }

    public AuthRefreshResponse refresh(String refreshToken) {
        Claims claims = jwt.validateRefreshToken(refreshToken);
        Long memberId = Long.valueOf(claims.getSubject());

        if (revokeStore.readCutoverAt(memberId).isPresent()) {
            throw new IllegalArgumentException("세션이 만료되었습니다(보안 변경 적용). 다시 로그인하세요.");
        }

        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        String role = "CUSTOMER";
        String at = jwt.createAccessToken(memberId, role);

        return AuthRefreshResponse.builder()
                .tokenType("Bearer")
                .accessToken(at)
                .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                .memberId(memberId)
                .role(role)
                .email(m.getEmail())
                .nickname(m.getNickname())
                .issuedAt(Instant.now())
                .build();
    }

    @Transactional
    public AuthLogoutResponse logout(String refreshToken) {
        Claims claims = jwt.validateRefreshToken(refreshToken);
        Long memberId = Long.valueOf(claims.getSubject());

        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        jwt.revokeRefreshToken(memberId);

        return AuthLogoutResponse.builder()
                .tokenRevoked(true)
                .memberId(memberId)
                .email(m.getEmail())
                .nickname(m.getNickname())
                .revokedAt(Instant.now())
                .build();
    }

    @Transactional
    public void issueResetTokenByIdentity(String email, String mobile) {
        String normalizedMobile = PhoneUtils.normalize(mobile);

        Member m = memberRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 계정을 찾을 수 없습니다."));

        if (!normalizedMobile.equals(PhoneUtils.normalize(m.getPhone()))) {
            throw new IllegalArgumentException("이메일과 휴대폰 정보가 일치하지 않습니다.");
        }

        String token = resetTokenStore.issue(email);
        emailSender.sendPasswordReset(email, token);
    }

    @Transactional
    public void resetPassword(String email, String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호와 확인 값이 일치하지 않습니다.");
        }
        if (!isStrongPassword(newPassword)) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문/숫자를 포함해야 합니다.");
        }

        String saved = resetTokenStore.get(email)
                .orElseThrow(() -> new IllegalArgumentException("재설정 토큰이 만료되었거나 존재하지 않습니다."));
        if (!saved.equals(token)) {
            throw new IllegalArgumentException("재설정 토큰이 올바르지 않습니다.");
        }

        Member m = memberRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        m.changePassword(passwordEncoder.encode(newPassword));

        revokeStore.scheduleCutoverAfterSeconds(m.getId(), 10);
        jwt.revokeRefreshToken(m.getId());

        resetTokenStore.delete(email);
    }

    private boolean isStrongPassword(String raw) {
        if (raw == null) return false;
        return raw.length() >= 8 && raw.matches(".*[A-Za-z].*") && raw.matches(".*\\d.*");
    }
}
