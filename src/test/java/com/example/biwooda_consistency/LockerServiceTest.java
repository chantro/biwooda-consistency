package com.example.biwooda_consistency;

import com.example.biwooda_consistency.controller.dto.LockerDto;
import com.example.biwooda_consistency.domain.Locker;
import com.example.biwooda_consistency.domain.LockerRepo;
import com.example.biwooda_consistency.domain.LockerStatus;
import com.example.biwooda_consistency.service.LockerService;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
public class LockerServiceTest {
  @Autowired
  private LockerService lockerService;

  @Autowired
  private LockerRepo lockerRepo;

  @Test
  void 기능_테스트(){
    Locker locker = lockerService.createLocker(new LockerDto.CreateLockerRequest(3));

    lockerService.rent(locker.getId());
    lockerService.returnBack(locker.getId());

    Locker reloaded = lockerRepo.findById(locker.getId()).orElseThrow();
    assertThat(reloaded.getUmbrellaRest()).isEqualTo(3);
    assertThat(reloaded.getStatus()).isEqualTo(LockerStatus.AVAILABLE);
  }

  @Test
  void 동시성_테스트_재고_1개일때() throws Exception{
    Locker locker = lockerService.createLocker(new LockerDto.CreateLockerRequest(1));
    Long lockerId = locker.getId();

    int threadCount = 10; //10명의 사용자가 동시에 대여한다고 가정
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);

    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);

    AtomicInteger success = new AtomicInteger();
    AtomicInteger fail = new AtomicInteger();

    for(int i =0; i<threadCount; i++){
      executorService.submit(() -> {
        try{
          ready.countDown();
          start.await();

          try{
            lockerService.rent(lockerId);
            success.incrementAndGet();
          }catch (OptimisticLockException | ObjectOptimisticLockingFailureException e){
            fail.incrementAndGet();
          }catch(IllegalStateException e){
            fail.incrementAndGet();
          }
        } catch (InterruptedException ignored){
        } finally {
          done.countDown();
        }
      });
    }

    ready.await();
    start.countDown();

    done.await();
    executorService.shutdown();

    assertThat(success.get()).isEqualTo(1);
    assertThat(fail.get()).isEqualTo(threadCount-1);

    Locker reloaded = lockerRepo.findById(lockerId).orElseThrow();
    assertThat(reloaded.getUmbrellaRest()).isEqualTo(0);
  }

  @Test
  void 동시성_테스트_재고_20개일때() throws Exception{
    Locker locker = lockerService.createLocker(new LockerDto.CreateLockerRequest(20));
    Long lockerId = locker.getId();

    int threadCount = 20; //100명의 사용자가 동시에 대여한다고 가정
    ExecutorService executorService = Executors.newFixedThreadPool(20);

    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(threadCount);

    AtomicInteger success = new AtomicInteger();
    AtomicInteger lockFailure = new AtomicInteger();
    AtomicInteger otherFailure = new AtomicInteger();

    for(int i =0; i<threadCount; i++){
      executorService.submit(() -> {
        try{
          ready.countDown();
          start.await();

          try{
            lockerService.rent(lockerId);
            success.incrementAndGet();
          }catch (OptimisticLockException | ObjectOptimisticLockingFailureException e){
            lockFailure.incrementAndGet();
          }catch(IllegalStateException e){
            otherFailure.incrementAndGet();
            e.printStackTrace();
          }
        } catch (InterruptedException ignored){
        } finally {
          done.countDown();
        }
      });
    }

    ready.await();
    start.countDown();

    done.await();
    executorService.shutdown();

    Locker reloaded = lockerRepo.findById(lockerId).orElseThrow();

    System.out.println("success = " + success.get());
    System.out.println("lockFailure = " + lockFailure.get());
    System.out.println("otherFailure = " + otherFailure.get());
    System.out.println("final umbrellaRest = " + reloaded.getUmbrellaRest());

    assertThat(success.get())
            .as("성공한 대여 횟수는 20이어야 한다.")
            .isEqualTo(20);

    assertThat(lockFailure.get())
            .as("락 충돌로 완전히 실패한 요청은 없어야 한다.")
            .isEqualTo(0);

    assertThat(otherFailure.get())
            .as("예상치 못한 다른 종류의 예외는 없어야 한다.")
            .isEqualTo(0);

    assertThat(reloaded.getUmbrellaRest())
            .as("최종 재고는 0이어야 한다.")
            .isEqualTo(0);
  }
}
