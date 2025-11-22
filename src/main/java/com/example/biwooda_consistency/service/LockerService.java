package com.example.biwooda_consistency.service;

import com.example.biwooda_consistency.controller.dto.LockerDto;
import com.example.biwooda_consistency.domain.Locker;
import com.example.biwooda_consistency.domain.LockerRepo;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class LockerService {
  private final LockerRepo lockerRepo;
  private final TransactionTemplate transactionTemplate;

  private static final int MAX_RETRY = 10;

  @Transactional
  public Locker createLocker(LockerDto.CreateLockerRequest request){
    Locker locker;
    if(request.umbrellaTotal() == null){
      return lockerRepo.save(Locker.createAvailable());
    }
    return lockerRepo.save(Locker.createAvailableWithTotal(request.umbrellaTotal()));
  }

  @Transactional
  public List<Locker> findAllLockers(){
    return lockerRepo.findAll();
  }

  public Locker rent(Long id){
    return retry(id, Locker::rent);
  }

  public Locker returnBack(Long id){
    return retry(id, Locker::returnBack);
  }

  public Locker broken(Long id){
    return retry(id, Locker::broken);
  }

  public Locker repair(Long id, LockerDto.RepairLockerRequest request){
    return retry(id, locker -> locker.repair(request.umbrellaRest()));
  }

  public void delete(Long id){
    Locker locker = getLocker(id);
    lockerRepo.delete(locker);
  }

  private Locker getLocker(Long id){
    return lockerRepo.findById(id).orElseThrow(() ->
            new IllegalArgumentException("존재하지 않는 락커입니다. id=" + id));
  }

  private Locker retry(Long id, Consumer<Locker> consumer){
    int attempt = 0;
    while(true){
      try{
        return transactionTemplate.execute(status -> {
          Locker locker = getLocker(id);
          consumer.accept(locker);
          return lockerRepo.save(locker);
        });
      } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e){
        attempt++;
        if(attempt >= MAX_RETRY){
          throw e; //409 error
        }
        try { Thread.sleep(50); } catch (InterruptedException ignored) {}
      }
    }
  }

}
