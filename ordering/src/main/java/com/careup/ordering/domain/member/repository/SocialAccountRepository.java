package com.careup.ordering.domain.member.repository;

import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.entity.SocialAccount;
import com.careup.ordering.domain.member.entity.SocialProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndSocialId(SocialProvider provider, String socialId);

    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);

    /** 해당 회원이 하나 이상의 소셜 계정을 연동했는지 여부 */
    boolean existsByMember(Member member);

    /** 해당 회원의 모든 소셜 연결 조회 (언링크 처리용) */
    List<SocialAccount> findAllByMember(Member member);
}
