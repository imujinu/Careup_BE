package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.Branch;
import com.careup.branch.domain.branch.entity.BranchStatus;
import com.careup.branch.domain.employee.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    /**
     * 검색 및 필터 기능을 포함한 지점 목록 조회
     * @param keyword 검색어 (지점명, 업종, 주소에서 검색)
     * @param status 지점 상태 필터
     * @param pageable 페이징 정보
     * @return 필터링된 지점 목록
     */
    @Query("SELECT b FROM Branch b WHERE " +
            "(:keyword IS NULL OR " +
            "LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.businessDomain) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(b.address) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:status IS NULL OR b.status = :status)")
    Page<Branch> searchBranches(
            @Param("keyword") String keyword,
            @Param("status") BranchStatus status,
            Pageable pageable
    );

}
