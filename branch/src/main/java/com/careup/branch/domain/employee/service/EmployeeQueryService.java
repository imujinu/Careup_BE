package com.careup.branch.domain.employee.service;

import com.careup.branch.domain.employee.dto.response.EmployeeDetailDto;
import com.careup.branch.domain.employee.dto.response.EmployeeDispatchDto;
import com.careup.branch.domain.employee.entity.DispatchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import com.careup.branch.domain.employee.repository.DispatchStatusRepository;
import com.careup.branch.domain.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeQueryService {

    private final EmployeeRepository employeeRepository;
    private final DispatchStatusRepository dispatchStatusRepository;

    public EmployeeDetailDto getDetail(Long employeeId) {
        Employee e = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EntityNotFoundException("직원을 찾을 수 없습니다."));

        var all = dispatchStatusRepository.findAllByEmployeeIdOrderByAssignedFromDesc(employeeId);
        var dispatchDtos = all.stream().map(EmployeeDispatchDto::fromEntity).toList();

        return EmployeeDetailDto.fromEntity(e, dispatchDtos);
    }

    public Page<EmployeeDetailDto> listWithActiveDispatch(Pageable pageable) {
        Page<Employee> page = employeeRepository.findAll(pageable);

        List<Long> ids = page.getContent().stream().map(Employee::getId).toList();
        if (ids.isEmpty()) {
            return page.map(e -> EmployeeDetailDto.fromEntity(e, Collections.emptyList()));
        }

        LocalDate today = LocalDate.now();
        List<DispatchStatus> activeList =
                dispatchStatusRepository.findByEmployeeIdInAndPlacementYnAndAssignedFromLessThanEqualAndAssignedToGreaterThanEqual(
                        ids, "Y", today, today);

        Map<Long, DispatchStatus> activeByEmp =
                activeList.stream()
                        .collect(Collectors.toMap(ds -> ds.getEmployee().getId(),
                                Function.identity(),
                                (a, b) -> a.getAssignedFrom().isAfter(b.getAssignedFrom()) ? a : b));

        return page.map(e -> {
            DispatchStatus ds = activeByEmp.get(e.getId());
            List<EmployeeDispatchDto> dispatchDtos =
                    (ds == null) ? Collections.emptyList() : List.of(EmployeeDispatchDto.fromEntity(ds));
            return EmployeeDetailDto.fromEntity(e, dispatchDtos);
        });
    }
}
