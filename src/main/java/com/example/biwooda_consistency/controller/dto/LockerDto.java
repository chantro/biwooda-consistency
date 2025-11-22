package com.example.biwooda_consistency.controller.dto;

import com.example.biwooda_consistency.domain.Locker;
import com.example.biwooda_consistency.domain.LockerStatus;

import java.time.Instant;

public class LockerDto {

  public record CreateLockerRequest(
          Integer umbrellaTotal   // null이면 기본값 사용
  ) {}

  public record RepairLockerRequest(
          Integer umbrellaRest
  ) {}

  public record LockerResponse(
          Long id,
          LockerStatus status,
          int umbrellaRest,
          int umbrellaTotal,
          Instant updatedAt
  ) {
    public static LockerResponse from(Locker locker) {
      return new LockerResponse(
              locker.getId(),
              locker.getStatus(),
              locker.getUmbrellaRest(),
              locker.getUmbrellaTotal(),
              locker.getUpdatedAt()
      );
    }
  }
}
