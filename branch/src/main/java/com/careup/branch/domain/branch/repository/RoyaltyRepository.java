package com.careup.branch.domain.branch.repository;

import com.careup.branch.domain.branch.entity.Royalty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoyaltyRepository extends JpaRepository<Royalty, Long> {
}
