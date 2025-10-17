package com.careup.branch.domain.branch.controller;

import com.careup.branch.domain.branch.service.RoyaltyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/royalties")
@RequiredArgsConstructor
public class RoyaltyController {

    private final RoyaltyService royaltyService;


}
