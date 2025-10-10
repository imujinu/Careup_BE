// com.careup.branch.domain.auth.service.AuthService
package com.careup.branch.domain.auth.service;

import com.careup.branch.common.auth.JwtProperties;
import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.common.auth.PasswordResetTokenStore;
import com.careup.branch.common.service.EmailSender;
import com.careup.branch.common.util.PhoneUtils;
import com.careup.branch.domain.auth.dto.request.AuthLoginRequest;
import com.careup.branch.domain.auth.dto.response.AuthLoginResponse;
import com.careup.branch.domain.auth.dto.response.AuthLogoutResponse;
import com.careup.branch.domain.auth.dto.response.AuthRefreshResponse;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

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

    /** 이메일 또는 휴대폰 로그인 → employeeId 기준 AT/RT 발급 */
    public AuthLoginResponse login(AuthLoginRequest req) {
        if (req.getId() == null || req.getId().isBlank())
            throw new IllegalArgumentException("아이디(이메일 또는 휴대폰 번호)는 필수입니다.");
        if (req.getPassword() == null || req.getPassword().isBlank())
            throw new IllegalArgumentException("비밀번호는 필수입니다.");

        final String rawId = req.getId().trim();
        final boolean emailLogin = rawId.contains("@");
        final String loginId = emailLogin ? rawId : PhoneUtils.normalize(rawId);

        Employee emp = emailLogin
                ? employeeRepository.findByEmailIgnoreCase(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."))
                : employeeRepository.findByMobile(loginId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 휴대폰 번호입니다."));

        if (!Boolean.TRUE.equals(emp.getEnabled()))
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        if (!passwordEncoder.matches(req.getPassword(), emp.getPasswordHash()))
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");

        String role = emp.getAuthorityType().name();
        Long employeeId = emp.getId();

        String at = jwt.createAccessToken(employeeId, role);

        // rememberMe=false면 RT 미발급을 원하면 아래를 null 처리하세요.
        String rt = jwt.createRefreshToken(employeeId, req.isRememberMe());

        // 배치 지점
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

        return AuthLoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(at)
                .refreshToken(rt) // rememberMe=false 시 null로 바꾸려면 여기 조정
                .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                .role(role)
                .employeeId(employeeId)
                .name(emp.getName())
                .title(title)
                .email(emp.getEmail())
                .mobile(emp.getMobile())
                .branchId(branchId)
                .branchName(branchName)
                .build();
    }

    /** 자동로그인: RT로 AT 재발급 */
    public AuthRefreshResponse refresh(String refreshToken) {
        Claims claims = jwt.validateRefreshToken(refreshToken);
        Long employeeId = Long.valueOf(claims.getSubject());

        Employee emp = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("계정을 찾을 수 없습니다."));
        String role = emp.getAuthorityType().name();

        String at = jwt.createAccessToken(employeeId, role);

        LocalDate today = LocalDate.now();
        Optional<DispatchStatus> active = dispatchStatusRepository
                .findFirstByEmployeeAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                        emp, "N", today, today);

        Long branchId = null; String branchName = null;
        if (active.isPresent() && active.get().getBranch() != null) {
            branchId = active.get().getBranch().getId();
            branchName = active.get().getBranch().getName();
        }

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
                .build();
    }

    /** 로그아웃: RT 폐기 */
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

    /** (NEW) 비밀번호 재설정 메일 발송: 이메일+휴대폰 모두 DB와 일치해야 함 */
    @Transactional
    public void issueResetTokenByIdentity(String email, String mobile) {
        String normalizedMobile = PhoneUtils.normalize(mobile);

        Employee emp = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 이메일의 계정을 찾을 수 없습니다."));

        // 이메일은 위에서 일치, 휴대폰 일치 추가 확인
        if (!normalizedMobile.equals(PhoneUtils.normalize(emp.getMobile()))) {
            throw new IllegalArgumentException("이메일과 휴대폰 정보가 일치하지 않습니다.");
        }

        // 토큰 발급(키는 email 기준)
        String token = passwordResetTokenStore.issue(email);

        // 메일 발송(링크 포함)
        emailSender.sendPasswordReset(email, token);
    }

    /** (NEW) 토큰으로 비밀번호 재설정: 확인값까지 검증 */
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

        // 토큰 1회성 소진
        passwordResetTokenStore.delete(email);
    }

    public static boolean isStrongPassword(String raw) {
        if (raw == null) return false;
        return raw.length() >= 8 && raw.matches(".*[A-Za-z].*") && raw.matches(".*\\d.*");
    }
}
