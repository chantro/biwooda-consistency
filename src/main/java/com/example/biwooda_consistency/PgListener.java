package com.example.biwooda_consistency;

import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class PgListener implements InitializingBean, DisposableBean {
  private final DataSource dataSource; //커넥션 풀 -> 나중에 LISTEN 전용으로 붙잡기 위해
  private final Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer(); //여러 클라이언트가 동시에 구독 가능
  private volatile Connection connection; //PostgreSQL 데이터베이스 커넥션 객체

  public PgListener(DataSource dataSource){
    this.dataSource = dataSource;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    initConnection();
    startListenerThread();
  }

  private void initConnection() throws Exception {
    connection = dataSource.getConnection(); //NOTIFY 이벤트만 듣는 용도로 계속 유지되는 DB 커넥션 하나 빌려오기
    connection.setAutoCommit(true); // 듣기 전용 커넥션은 트랜잭션 관리가 별도로 필요가 없음

    //locker_changes 채널 계속 듣기
    try (Statement statement = connection.createStatement()){
      statement.execute("LISTEN locker_changes");
    }
  }

  private void startListenerThread(){
    Thread thread = new Thread(this::runListenerLoop, "pg-listener");
    thread.setDaemon(true);  // 백그라운드용 스레드 -> 메인 스레드와 다른 사용자 스레드 모두 종료 시 자동으로 함께 종료됨
    thread.start();
  }

  private void runListenerLoop(){
    try{
      PGConnection pg = connection.unwrap(PGConnection.class);

      while (!Thread.currentThread().isInterrupted()){ //무한 루프
        PGNotification[] notifications = pg.getNotifications(5000);
        handleNotifications(notifications);
      }
    }catch (Exception ignored){
      // 커넥션이 닫히면, 스레드가 무한루프에서 빠져나오게 됨
    }
  }

  private void handleNotifications(PGNotification[] notifications){
    if(notifications != null){
      for(PGNotification notification : notifications){
        //DB에서 받은 알림을 내부로 전달
        //해당 sink를 구독하고 있는 SSE 컨트롤러가 실시간으로 해당 문자열을 받음
        sink.tryEmitNext(notification.getParameter());
      }
    }
  }

  // 스트림 인터페이스
  public Flux<String> flux(){
    return sink.asFlux();
  }

  @Override
  public void destroy() throws Exception {
    if (connection != null){
      connection.close();
    }
  }
}
