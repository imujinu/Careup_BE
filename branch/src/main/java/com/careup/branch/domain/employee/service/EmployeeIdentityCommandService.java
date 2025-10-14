package com.careup.branch.domain.employee.service;

import com.careup.branch.common.auth.ForceLogoutStore;
import com.careup.branch.common.auth.JwtTokenProvider;
import com.careup.branch.domain.employee.dto.request.EmployeeIdentityChangeRequest;
import com.careup.branch.domain.employee.dto.response.EmployeeIdentityChangeResponse;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import io.jsonwebtoken.Claims;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeIdentityCommandService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final ForceLogoutStore forceLogoutStore;
    private final JwtTokenProvider jwt;

    public EmployeeIdentityChangeResponse changeMyIdentity(EmployeeIdentityChangeRequest req) {
        Long meId = readEmployeeId();
        Employee me = employeeRepository.findById(meId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        if (Boolean.FALSE.equals(me.getEnabled())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }
        if (!passwordEncoder.matches(req.getCurrentPassword(), me.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        if (hasText(req.getNewEmail())) {
            final String newEmail = req.getNewEmail().trim();
            employeeRepository.findByEmailIgnoreCase(newEmail).ifPresent(other -> {
                if (!other.getId().equals(me.getId())) {
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }
            });
            me.changeEmail(newEmail);
        }

        if (hasText(req.getNewMobile())) {
            final String normalized = req.getNewMobile().replaceAll("\\D", "");
            employeeRepository.findByMobile(normalized).ifPresent(other -> {
                if (!other.getId().equals(me.getId())) {
                    throw new IllegalArgumentException("이미 사용 중인 휴대폰 번호입니다.");
                }
            });
            me.changeMobile(normalized);
        }

        employeeRepository.save(me);

        // 10초 후 전역 컷오프 + RT 폐기
        forceLogoutStore.scheduleCutoverAfterSeconds(meId, 10);
        jwt.revokeRefreshToken(meId);

        return EmployeeIdentityChangeResponse.builder()
                .email(me.getEmail())
                .mobile(me.getMobile())
                .build();
    }

    private static boolean hasText(String s) { return s != null && !s.isBlank(); }

    private Long readEmployeeId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || a.getDetails() == null || !a.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Object details = a.getDetails();
        if (!(details instanceof Claims claims)) {
            throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        }
        Long id = claims.get("employeeId", Long.class);
        if (id == null) throw new AuthenticationCredentialsNotFoundException("인증 정보가 없습니다.");
        return id;
    }
}
