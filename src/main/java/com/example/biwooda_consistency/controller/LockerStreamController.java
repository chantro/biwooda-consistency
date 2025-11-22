package com.example.biwooda_consistency.controller;

import com.example.biwooda_consistency.PgListener;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class LockerStreamController {
  private final PgListener listener;

  @GetMapping(value = "/stream/lockers", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public Flux<ServerSentEvent<String>> stream(){
    return listener.flux().map(json -> ServerSentEvent.builder(json).event("locker_change").build());
  }
}
