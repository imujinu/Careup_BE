package com.careup.ordering.domain.member.repository;

import com.careup.ordering.domain.member.entity.Member;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmailIgnoreCase(String email);

    Optional<Member> findByPhone(String phone);

    Optional<Member> findByNickname(String nickname); // ★ 추가: 자기 자신 제외 중복검사용

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByNickname(String nickname);

    @Override
    Page<Member> findAll(Pageable pageable);

    @Override
    Optional<Member> findById(Long id);

    ///  고객 계정 찾기 전용
    Optional<Member> findByNameAndBirthdayAndNickname(String name, LocalDate birthday, String nickname);
}
