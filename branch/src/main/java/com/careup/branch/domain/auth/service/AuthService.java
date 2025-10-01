package com.careup.branch.domain.auth.service;

import com.careup.branch.common.auth.JwtProperties;
import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.domain.auth.dto.request.AuthLoginRequest;
import com.careup.branch.domain.auth.dto.response.AuthLoginResponse;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwt;
    private final JwtProperties jwtProps;

    public AuthLoginResponse login(AuthLoginRequest req) {
        if (req.getId() == null || req.getId().isBlank()) {
            throw new IllegalArgumentException("아이디(이메일 또는 휴대폰 번호)는 필수입니다.");
        }
        if (req.getPassword() == null || req.getPassword().isBlank()) {
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }

        final String rawId = req.getId().trim();

        Employee emp = rawId.contains("@")
                ? employeeRepository.findByEmailIgnoreCase(rawId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 이메일입니다."))
                : employeeRepository.findByMobile(normalizeToHyphenPhone(rawId))
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 휴대폰 번호입니다."));

        if (!Boolean.TRUE.equals(emp.getEnabled())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }
        if (!passwordEncoder.matches(req.getPassword(), emp.getPasswordHash())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        String role = emp.getAuthorityType().name(); // HQ_ADMIN / BRANCH_ADMIN / FRANCHISE_OWNER / STAFF
        String at = jwt.createAccessToken(emp.getEmail(), role, emp.getId());
        String rt = jwt.createRefreshToken(emp.getEmail(), req.isRememberMe());

        LocalDate today = LocalDate.now();
        var activeDispatchOpt =
                dispatchStatusRepository
                        .findFirstByEmployeeIdAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqualOrderByAssignedFromDesc(
                                emp.getId(), "Y", today, today);

        Long branchId = null;
        String branchName = null;
        if (activeDispatchOpt.isPresent()) {
            DispatchStatus d = activeDispatchOpt.get();
            if (d.getBranch() != null) {
                branchId = d.getBranch().getId();
                branchName = d.getBranch().getName();
            }
        }

        String title = (emp.getJobGrade() != null) ? emp.getJobGrade().getName() : null;

        return AuthLoginResponse.builder()
                .tokenType("Bearer")
                .accessToken(at)
                .refreshToken(rt)
                .expiresInMinutes(jwtProps.getAccessTokenExpiryMinutes())
                .role(role)
                .employeeId(emp.getId())
                .name(emp.getName())
                .title(title)
                .email(emp.getEmail())
                .mobile(emp.getMobile())
                .branchId(branchId)
                .branchName(branchName)
                .build();
    }

    private String normalizeToHyphenPhone(String input) {
        String digits = input.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        } else if (digits.length() == 10) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        } else {
            return input;
        }
    }

    public static boolean isStrongPassword(String raw) {
        if (raw == null) return false;
        return raw.length() >= 8 && raw.matches(".*[A-Za-z].*") && raw.matches(".*\\d.*");
    }
}
