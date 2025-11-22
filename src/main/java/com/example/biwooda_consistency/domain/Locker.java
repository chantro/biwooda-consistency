package com.example.biwooda_consistency.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Entity
@Table(name = "umbrella_lockers")
public class Locker {
  @Getter
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "locker_id")
  private Long id;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 16)
  private LockerStatus status;

  @Getter
  @Column(nullable = false)
  private Instant updatedAt;

  @Getter
  @Column(nullable = false)
  private int umbrellaRest;

  @Getter
  @Column(nullable = false)
  private int umbrellaTotal;

  //동시성 제어
  @Getter
  @Version
  private Long version;

  private static final int DEFAULT_UMBRELLA_TOTAL = 8;

  protected Locker(){
  }

  private Locker(LockerStatus status, int umbrellaTotal){
    validateLockerInit(umbrellaTotal);
    this.status = status;
    this.updatedAt = Instant.now();
    this.umbrellaTotal = umbrellaTotal;
    this.umbrellaRest = umbrellaTotal;
  }

  private Locker(LockerStatus status){
    this.status = status;
    this.updatedAt = Instant.now();
    this.umbrellaTotal = DEFAULT_UMBRELLA_TOTAL;
    this.umbrellaRest = DEFAULT_UMBRELLA_TOTAL;
  }

  public static Locker createAvailable(){
    return new Locker(LockerStatus.AVAILABLE);
  }

  public static Locker createAvailableWithTotal(int umbrellaTotal){
    return new Locker(LockerStatus.AVAILABLE, umbrellaTotal);
  }

  public void rent() {
    validateRentAvailable();
    this.umbrellaRest--;
    this.updatedAt = Instant.now();
  }

  public void returnBack(){
    validateReturnAvailable();
    this.umbrellaRest++;
    this.updatedAt = Instant.now();
  }

  public void broken(){
    this.status = LockerStatus.DISABLE;
    this.updatedAt = Instant.now();
  }

  public void repair(int umbrellaRest){
    validateLockerRepair(umbrellaRest);
    this.umbrellaRest = umbrellaRest;
    this.status = LockerStatus.AVAILABLE;
  }

  private void validateLockerStatus(){
    if (this.status == LockerStatus.DISABLE){
      throw new IllegalStateException("현재 해당 우산함은 사용할 수 없습니다.");
    }
  }

  private void validateLockerInit(int umbrellaTotal){
    if (umbrellaTotal < 0){
      throw new IllegalArgumentException("총 우산 개수는 0 이상이어야 합니다.");
    }
  }

  private void validateLockerRepair(int umbrellaRest){
    if(umbrellaRest < 0 || umbrellaRest > this.umbrellaTotal){
      throw new IllegalArgumentException("잘못된 재고 설정입니다.");
    }
  }

  private void validateRentAvailable(){
    validateLockerStatus();
    if (this.umbrellaRest <= 0){
      throw new IllegalStateException("대여 가능한 우산이 없습니다.");
    }
  }

  private void validateReturnAvailable(){
    validateLockerStatus();
    if (this.umbrellaRest >= this.umbrellaTotal){
      throw new IllegalStateException("우산함이 꽉 차 있습니다.");
    }
  }
}
