package com.careup.ordering.domain.member.repository;

import com.careup.ordering.domain.member.entity.LoyalCustomer;
import com.careup.ordering.domain.member.entity.LoyalGrade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoyalCustomerRepository extends JpaRepository<LoyalCustomer, Long> {
    
    /**
     * 특정 지점의 단골 고객 목록 조회
     */
    List<LoyalCustomer> findByBranchId(Long branchId);
    
    /**
     * 특정 회원의 특정 지점 단골 고객 조회
     */
    Optional<LoyalCustomer> findByMemberIdAndBranchId(Long memberId, Long branchId);
    
    /**
     * 특정 등급의 단골 고객 조회
     */
    List<LoyalCustomer> findByBranchIdAndGrade(Long branchId, LoyalGrade grade);
    
    /**
     * 회원이 특정 지점의 단골 고객인지 확인
     */
    boolean existsByMemberIdAndBranchId(Long memberId, Long branchId);
}
