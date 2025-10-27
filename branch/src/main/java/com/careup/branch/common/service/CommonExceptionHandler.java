package com.careup.branch.common.service;

import com.careup.branch.common.dto.CommonErrorDto;
import com.careup.branch.domain.employee.service.MissedCheckoutLockException;
import io.jsonwebtoken.JwtException;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class CommonExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegalArgumentException(IllegalArgumentException e) {
        log.error("[CAREUP][ERROR] - CommonExceptionHandler/IllegalArgumentException - {}", e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> entityNotFoundException(EntityNotFoundException e) {
        log.error("[CAREUP][ERROR] - CommonExceptionHandler/EntityNotFoundException - {}", e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.NOT_FOUND.value(), e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<?> entityExistsException(EntityExistsException e) {
        log.error("[CAREUP][ERROR] - CommonExceptionHandler/EntityExistsException - {}", e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.CONFLICT.value(), e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(MethodArgumentNotValidException e) {
        String errorMessage = e.getBindingResult()
                .getFieldError()
                .getDefaultMessage();
        log.error("[CAREUP][ERROR] - CommonExceptionHandler/MethodArgumentNotValidException - {}", errorMessage);
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), errorMessage), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleAllException(Exception e) {
        log.error("[CAREUP][ERROR] - CommonExceptionHandler/Exception - {}", e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<?> handleJwt(JwtException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new CommonErrorDto(HttpStatus.UNAUTHORIZED.value(), "유효하지 않은 토큰입니다."));
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<?> authCredentialsNotFound(AuthenticationCredentialsNotFoundException e) {
        log.error("[CAREUP][ERROR] - AuthenticationCredentialsNotFoundException - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new CommonErrorDto(HttpStatus.UNAUTHORIZED.value(), "인증 정보가 없습니다."));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> accessDenied(AccessDeniedException e) {
        log.error("[CAREUP][ERROR] - AccessDeniedException - {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new CommonErrorDto(HttpStatus.FORBIDDEN.value(), "권한이 없습니다."));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> illegalStateException(IllegalStateException e) {
        log.error("[CAREUP][ERROR] - CommonExceptionHandler/IllegalStateException - {}", e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.BAD_REQUEST.value(), e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    // 추가: 퇴근 누락 락 상황은 409로 통일
    @ExceptionHandler(MissedCheckoutLockException.class)
    public ResponseEntity<?> missedCheckoutLock(MissedCheckoutLockException e) {
        log.warn("[CAREUP][WARN] - CommonExceptionHandler/MissedCheckoutLockException - {}", e.getMessage());
        return new ResponseEntity<>(new CommonErrorDto(HttpStatus.CONFLICT.value(), e.getMessage()), HttpStatus.CONFLICT);
    }
}
