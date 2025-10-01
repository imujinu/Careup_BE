package com.careup.branch.domain.auth.controller;

import com.careup.branch.common.dto.CommonSuccessDto;
import com.careup.branch.domain.auth.dto.request.AuthLoginRequest;
import com.careup.branch.domain.auth.dto.response.AuthLoginResponse;
import com.careup.branch.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<CommonSuccessDto> login(@RequestBody AuthLoginRequest req) {
        AuthLoginResponse result = authService.login(req);
        return ResponseEntity.status(HttpStatus.OK).body(
                CommonSuccessDto.builder()
                        .result(result)
                        .status_code(HttpStatus.OK.value())
                        .status_message("로그인 성공")
                        .build()
        );
    }
}
