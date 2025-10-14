package com.careup.ordering.domain.member.repository;

import com.careup.ordering.domain.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailIgnoreCase(String email);

    Optional<Member> findByPhone(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNickname(String nickname);

    @Override
    Page<Member> findAll(Pageable pageable);

    @Override
    Optional<Member> findById(Long id);
}
