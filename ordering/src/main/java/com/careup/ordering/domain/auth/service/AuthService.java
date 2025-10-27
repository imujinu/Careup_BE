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
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    /** 회원가입 후 자동 로그인(AT/RT 발급) */
    @Transactional
    public AuthLoginResponse signUp(SignUpRequest req) {
        String email = req.getEmail().trim();
        String nickname = req.getNickname().trim();
        String phoneNorm = PhoneUtils.normalize(req.getPhone());
        String zipcode = req.getZipcode().trim();
        String address = req.getAddress().trim();
        String addressDetail = req.getAddressDetail().trim();

        if (!isStrongPassword(req.getPassword())) {
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문/숫자를 포함해야 합니다.");
        }

        Optional<Member> emailOwnerOpt = memberRepository.findByEmailIgnoreCase(email);
        if (emailOwnerOpt.isPresent()) {
            Member emailOwner = emailOwnerOpt.get();
            if (emailOwner.isDeactivated()) {
                memberRepository.findByNickname(nickname).ifPresent(other -> {
                    if (!other.getId().equals(emailOwner.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                    }
                });
                memberRepository.findByPhone(phoneNorm).ifPresent(other -> {
                    if (!other.getId().equals(emailOwner.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
                    }
                });

                emailOwner.changePassword(passwordEncoder.encode(req.getPassword()));
                emailOwner.changePhone(phoneNorm);
                emailOwner.changeProfile(nickname, req.getName().trim(), req.getBirthday(), req.getGender());
                emailOwner.changeAddress(zipcode, address, addressDetail);
                emailOwner.reactivate();
                memberRepository.save(emailOwner);

                scheduleWelcomeMail(emailOwner.getEmail(), emailOwner.getName(), emailOwner.getNickname());
                return issueTokensAndBuildLoginResponse(emailOwner);
            }
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        Optional<Member> phoneOwnerOpt = memberRepository.findByPhone(phoneNorm);
        if (phoneOwnerOpt.isPresent()) {
            Member phoneOwner = phoneOwnerOpt.get();
            if (phoneOwner.isDeactivated()) {
                Optional<Member> emailOther = memberRepository.findByEmailIgnoreCase(email);
                if (emailOther.isPresent() && !emailOther.get().getId().equals(phoneOwner.getId())) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
                memberRepository.findByNickname(nickname).ifPresent(other -> {
                    if (!other.getId().equals(phoneOwner.getId())) {
                        throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
                    }
                });

                phoneOwner.changeEmail(email);
                phoneOwner.changePassword(passwordEncoder.encode(req.getPassword()));
                phoneOwner.changeProfile(nickname, req.getName().trim(), req.getBirthday(), req.getGender());
                phoneOwner.changeAddress(zipcode, address, addressDetail);
                phoneOwner.reactivate();
                memberRepository.save(phoneOwner);

                scheduleWelcomeMail(phoneOwner.getEmail(), phoneOwner.getName(), phoneOwner.getNickname());
                return issueTokensAndBuildLoginResponse(phoneOwner);
            } else {
                throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
            }
        }

        if (memberRepository.existsByNickname(nickname)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        Member m = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(req.getPassword()))
                .nickname(nickname)
                .name(req.getName().trim())
                .birthday(req.getBirthday())
                .phone(phoneNorm)
                .gender(req.getGender())
                .zipcode(zipcode)
                .address(address)
                .addressDetail(addressDetail)
                .build();
        memberRepository.save(m);

        scheduleWelcomeMail(m.getEmail(), m.getName(), m.getNickname());
        return issueTokensAndBuildLoginResponse(m);
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

        if (member.isDeactivated()) {
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
        long rtIatMs = claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : 0L;

        Optional<Long> cutOpt = revokeStore.readCutoverAt(memberId);
        if (cutOpt.isPresent()) {
            long cut = cutOpt.get();
            if (System.currentTimeMillis() >= cut && rtIatMs < cut) {
                throw new IllegalArgumentException("세션이 만료되었습니다(보안 변경 적용). 다시 로그인하세요.");
            }
        }

        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        if (m.isDeactivated()) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

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

    private void scheduleWelcomeMail(String email, String name, String nickname) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                log.info("[MAIL][WELCOME] after-commit send schedule to={}", email);
                try { emailSender.sendWelcomeEmail(email, name, nickname); }
                catch (Exception e) { log.warn("[MAIL][WELCOME] send failed to={} : {}", email, e.getMessage()); }
            }
        });
    }

    private AuthLoginResponse issueTokensAndBuildLoginResponse(Member member) {
        String role = "CUSTOMER";
        Long memberId = member.getId();
        String at = jwt.createAccessToken(memberId, role);
        // OAuth와 동일하게 첫 가입은 rememberMe=true로 RT 발급
        String rt = jwt.createRefreshToken(memberId, true);

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
}
