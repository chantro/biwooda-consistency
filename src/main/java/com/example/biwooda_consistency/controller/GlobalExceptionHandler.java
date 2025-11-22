package com.example.biwooda_consistency.controller;

import jakarta.persistence.OptimisticLockException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private final String LOCK_EXCEPT_TEXT = "다른 사용자가 먼저 우산을 대여/반납했습니다. 다시 시도해주세요.";

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  public ResponseEntity<?> handleIllegalArgumentState(IllegalArgumentException e) {
    return ResponseEntity
            .badRequest()
            .body(Map.of("error", e.getMessage()));
  }

  // 낙관적 락 충돌 발생 처리
  @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
  public ResponseEntity<?> handleOptimistic(ObjectOptimisticLockingFailureException e) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("error", LOCK_EXCEPT_TEXT));
  }
}
