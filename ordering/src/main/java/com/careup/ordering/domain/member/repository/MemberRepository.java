package com.careup.ordering.domain.member.repository;

import com.careup.ordering.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member,Long> {
}
