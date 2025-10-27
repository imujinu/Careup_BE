package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.EmployeeIdLookupRequest;
import com.careup.branch.domain.employee.dto.response.EmployeeIdLookupResponse;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeIdLookupService {

    private final EmployeeRepository employeeRepository;

    /** 사번 + 이름 + 생년월일로 직원 아이디(이메일/휴대폰) 조회 */
    public EmployeeIdLookupResponse lookup(EmployeeIdLookupRequest req) {
        final String name = req.getName().trim();
        // 사번에 공백이 섞여 들어오는 경우 방지 (중간 공백 제거까지 원치 않으면 .replaceAll 제거하세요)
        final String empNo = req.getEmployeeNumber().trim().replaceAll("\\s+", "");
        final var dob = req.getDateOfBirth();

        Employee e = employeeRepository
                .findByNameAndDateOfBirthAndEmployeeNumber(name, dob, empNo)
                .orElseThrow(() -> new IllegalArgumentException("일치하는 직원 정보가 없습니다."));

        if (Boolean.FALSE.equals(e.getEnabled())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        return EmployeeIdLookupResponse.builder()
                .email(e.getEmail())
                .mobile(e.getMobile())
                .build();
    }
}
