package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.request.EmployeeIdLookupRequest;
import com.careup.branch.domain.employee.dto.response.EmployeeIdLookupResponse;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeIdLookupService {

    private final EmployeeRepository employeeRepository;

    public EmployeeIdLookupResponse lookup(EmployeeIdLookupRequest req) {
        Employee e = employeeRepository
                .findByNameAndDateOfBirthAndEmployeeNumber(
                        req.getName().trim(),
                        req.getDateOfBirth(),
                        req.getEmployeeNumber().trim()
                )
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

