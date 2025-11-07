package com.careup.ordering;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {
    @GetMapping("/health")
    public String healthCheck(){

        System.out.println("ordering은 건강합니다!");
        return "ordering-Service OK";
    }
}
