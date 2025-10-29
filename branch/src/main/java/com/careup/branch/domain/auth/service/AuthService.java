package com.careup.branch.domain.auth.service;

import com.careup.branch.common.auth.ForceLogoutStore;
import com.careup.branch.common.auth.JwtProperties;
import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.common.auth.PasswordResetTokenStore;
import com.careup.branch.common.service.EmailSender;
import com.careup.branch.common.util.PhoneUtils;
import com.careup.branch.domain.auth.dto.request.AuthLoginRequest;
import com.careup.branch.domain.auth.dto.response.AuthLoginResponse;
import com.careup.branch.domain.auth.dto.response.AuthLogoutResponse;
import com.careup.branch.domain.auth.dto.response.AuthRefreshResponse;
import com.careup.branch.domain.auth.exception.LoginException;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProps;

    private final PasswordResetTokenStore passwordResetTokenStore;
    private final EmailSender emailSender;

    private final ForceLogoutStore forceLogoutStore;

    public AuthLoginResponse login(AuthLoginRequest req) {
        if (req.getId() == null || req.getId().isBlank())
            throw LoginException.idFormatInvalid("아이디는 필수입니다.");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw LoginException.pwdFormatInvalid("비밀번호는 필수입니다.");

        final String rawId = req.getId().trim();
        final boolean emailLogin = rawId.contains("@");
        final String loginId = emailLogin ? rawId : PhoneUtils.normalize(rawId);

        Employee emp = emailLogin
                ? employeeRepository.findByEmailIgnoreCase(loginId)
                .orElseThrow(LoginException::emailNotFound)
                : employeeRepository.findByMobile(loginId)
                .orElseThrow(LoginException::mobileNotFound);

        if (!Boolean.TRUE.equals(emp.getEnabled()))
            throw LoginException.accountInactive();

        if (!passwordEncoder.matches(req.getPassword(), emp.getPasswordHash()))
            throw LoginException.passwordMismatch();

        String role = emp.getAuthorityType().name();
        Long employeeId = emp.getId();

        LocalDate today = LocalDate.now();
        Optional<DispatchStatus> active = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        emp, "N", today, today);

        Long branchId = null; String branchName = null;
        if (active.isPresent() && active.get().getBranch() != null) {
            branchId = active.get().getBranch().getId();
            branchName = active.get().getBranch().getName();
        }

        String title = (emp.getJobGrade() != null) ? emp.getJobGrade().getName() : null;

        // ★ AT에 email + name + profileImageUrl + title 포함
        String at = jwt.createAccessToken(
                employeeId,
                role,
                branchId,
                emp.getEmail(),
                emp.getName(),
                emp.getProfileImageUrl(),
                title
        );
        String rt = jwt.createRefreshToken(employeeId, req.isRememberMe());

        return AuthLoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(at)
                .refreshToken(rt)
                .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                .role(role)
                .employeeId(employeeId)
                .name(emp.getName())
                .title(title)
                .email(emp.getEmail())
                .mobile(emp.getMobile())
                .branchId(branchId)
                .branchName(branchName)
                // 응답에도 사진 포함(프론트 안정성↑)
                .profileImageUrl(emp.getProfileImageUrl())
                .build();
    }

    public AuthRefreshResponse refresh(String refreshToken) {
        Claims claims = jwt.validateRefreshToken(refreshToken);
        Long employeeId = Long.valueOf(claims.getSubject());
        long rtIatMs = claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : 0L;

        Optional<Long> cutOpt = forceLogoutStore.readCutoverAt(employeeId);
        if (cutOpt.isPresent()) {
            long cut = cutOpt.get();
            if (System.currentTimeMillis() >= cut && rtIatMs < cut) {
                throw new IllegalArgumentException("세션이 만료되었습니다(보안 변경 적용). 다시 로그인하세요.");
            }
        }

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));
        String role = emp.getAuthorityType().name();

        LocalDate today = LocalDate.now();
        Optional<DispatchStatus> active = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        emp, "N", today, today);

        Long branchId = null; String branchName = null;
        if (active.isPresent() && active.get().getBranch() != null) {
            branchId = active.get().getBranch().getId();
            branchName = active.get().getBranch().getName();
        }

        String title = (emp.getJobGrade() != null) ? emp.getJobGrade().getName() : null;

        // ★ 리프레시 시에도 동일 클레임으로 AT 재발급
        String at = jwt.createAccessToken(
                employeeId,
                role,
                branchId,
                emp.getEmail(),
                emp.getName(),
                emp.getProfileImageUrl(),
                title
        );

        return AuthRefreshResponse.builder()
                .tokenType("Bearer")
                .accessToken(at)
                .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                .employeeId(employeeId)
                .name(emp.getName())
                .email(emp.getEmail())
                .mobile(emp.getMobile())
                .role(role)
                .branchId(branchId)
                .branchName(branchName)
                .issuedAt(Instant.now())
                // 응답에도 사진 포함(일관성)
                .profileImageUrl(emp.getProfileImageUrl())
                .build();
    }

    @Transactional
    public AuthLogoutResponse logout(String refreshToken) {
        Claims claims = jwt.validateRefreshToken(refreshToken);
        Long employeeId = Long.valueOf(claims.getSubject());

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));

        LocalDate today = LocalDate.now();
        Optional<DispatchStatus> active = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        emp, "N", today, today);

        Long branchId = null; String branchName = null;
        if (active.isPresent() && active.get().getBranch() != null) {
            branchId = active.get().getBranch().getId();
            branchName = active.get().getBranch().getName();
        }

        jwt.revokeRefreshToken(employeeId);

        return AuthLogoutResponse.builder()
                .tokenRevoked(true)
                .employeeId(employeeId)
                .name(emp.getName())
                .email(emp.getEmail())
                .mobile(emp.getMobile())
                .role(emp.getAuthorityType().name())
                .branchId(branchId)
                .branchName(branchName)
                .revokedAt(Instant.now())
                .build();
    }

    @Transactional
    public void issueResetTokenByIdentity(String email, String mobile) {
        String normalizedMobile = PhoneUtils.normalize(mobile);

        Employee emp = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 계정을 찾을 수 없습니다."));

        if (!normalizedMobile.equals(PhoneUtils.normalize(emp.getMobile()))) {
            throw new IllegalArgumentException("이메일과 휴대폰 정보가 일치하지 않습니다.");
        }

        String token = passwordResetTokenStore.issue(email);
        emailSender.sendPasswordReset(email, token);
    }

    @Transactional
    public void resetPassword(String email, String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("새 비밀번호와 확인 값이 일치하지 않습니다.");
        }

        String saved = passwordResetTokenStore.get(email)
                .orElseThrow(() -> new IllegalArgumentException("재설정 토큰이 만료되었거나 존재하지 않습니다."));
        if (!saved.equals(token))
            throw new IllegalArgumentException("재설정 토큰이 올바르지 않습니다.");
        if (!isStrongPassword(newPassword))
            throw new IllegalArgumentException("비밀번호는 8자 이상이며 영문/숫자를 포함해야 합니다.");

        Employee emp = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));
        emp.changePasswordHash(passwordEncoder.encode(newPassword));

        // 10초 후 전역 컷오프 + RT 폐기
        forceLogoutStore.scheduleCutoverAfterSeconds(emp.getId(), 10);
        jwt.revokeRefreshToken(emp.getId());

        passwordResetTokenStore.delete(email);
    }

    public static boolean isStrongPassword(String raw) {
        if (raw == null) return false;
        return raw.length() >= 8 && raw.matches(".*[A-Za-z].*") && raw.matches(".*\\d.*");
    }
}
