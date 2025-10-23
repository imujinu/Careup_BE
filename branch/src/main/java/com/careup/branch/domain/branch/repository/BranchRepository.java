package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {
    boolean existsByName(String name);
    boolean existsByBusinessNumber(String businessNumber);
    boolean existsByCorporationNumber(String corporationNumber);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);

    /// 추후 지점 검색 드롭다운 메뉴에서 사용 예정
    List<Branch> findByNameContaining(String keyword);

    Optional<Branch> findByBusinessNumber(String businessNumber);

}
