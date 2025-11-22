package com.example.biwooda_consistency.controller;

import com.example.biwooda_consistency.controller.dto.LockerDto;
import com.example.biwooda_consistency.domain.Locker;
import com.example.biwooda_consistency.service.LockerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lockers")
public class LockerController {
  private final LockerService lockerService;

  //우산함 생성
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public LockerDto.LockerResponse create(@RequestBody LockerDto.CreateLockerRequest request) {
    Locker saved = lockerService.createLocker(request);
    return LockerDto.LockerResponse.from(saved);
  }

  //전체 우산함 조회
  @GetMapping
  public List<LockerDto.LockerResponse> findAll(){
    return lockerService.findAllLockers().stream().map(LockerDto.LockerResponse::from).toList();
  }

  @PostMapping("/{id}/rent")
  public LockerDto.LockerResponse rent(@PathVariable long id){
    return LockerDto.LockerResponse.from(lockerService.rent(id));
  }

  @PostMapping("/{id}/return")
  public LockerDto.LockerResponse returnBack(@PathVariable long id){
    return LockerDto.LockerResponse.from(lockerService.returnBack(id));
  }

  @PostMapping("/{id}/broken")
  public LockerDto.LockerResponse broken(@PathVariable long id){
    return LockerDto.LockerResponse.from(lockerService.broken(id));
  }

  @PostMapping("/{id}/repair")
  public LockerDto.LockerResponse repair(@PathVariable long id, @RequestBody LockerDto.RepairLockerRequest body){
    return LockerDto.LockerResponse.from(lockerService.repair(id, body));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    lockerService.delete(id);
  }
}
