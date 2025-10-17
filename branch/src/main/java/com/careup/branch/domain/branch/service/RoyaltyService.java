package com.careup.branch.domain.branch.service;

import com.careup.branch.domain.branch.repository.RoyaltyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class RoyaltyService {

    private final RoyaltyRepository royaltyRepository;


}
