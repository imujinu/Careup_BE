package com.careup.ordering.domain.member.repository;

import com.careup.ordering.domain.member.entity.SocialAccount;
import com.careup.ordering.domain.member.entity.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndSocialId(SocialProvider provider, String socialId);

    boolean existsByProviderAndSocialId(SocialProvider provider, String socialId);
}
