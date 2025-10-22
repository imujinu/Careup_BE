package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.Royalty;
import com.careup.branch.domain.branch.entity.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoyaltyRepository extends JpaRepository<Royalty, Long> {

    // 지점별 로열티 목록 조회 (지점 정보 포함)
    @Query("SELECT r FROM Royalty r JOIN FETCH r.branch ORDER BY r.applicableMonth DESC, r.branch.name")
    List<Royalty> findAllWithBranch();

    // 특정 지점의 로열티 상세 조회
    @Query("SELECT r FROM Royalty r JOIN FETCH r.branch WHERE r.id = :royaltyId")
    Optional<Royalty> findByIdWithBranch(@Param("royaltyId") Long royaltyId);

    // 특정 지점의 정산 내역 조회 (정산 상태별 필터링)
    @Query("SELECT r FROM Royalty r WHERE r.branch.id = :branchId AND r.settlementStatus = :status ORDER BY r.applicableMonth DESC")
    List<Royalty> findByBranchIdAndSettlementStatus(@Param("branchId") Long branchId, @Param("status") SettlementStatus status);

    // 특정 지점의 전체 정산 내역 조회
    @Query("SELECT r FROM Royalty r WHERE r.branch.id = :branchId ORDER BY r.applicableMonth DESC")
    List<Royalty> findByBranchIdOrderByApplicableMonthDesc(@Param("branchId") Long branchId);
}
