package com.careup.ordering.domain.member.repository;

import com.careup.ordering.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailIgnoreCase(String email);
    Optional<Member> findByPhone(String phone); // 저장/조회는 PhoneUtils.normalize 기준
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByNickname(String nickname);
}
