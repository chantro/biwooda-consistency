-- 우산함 테이블 생성
CREATE TABLE umbrella_lockers (
  locker_id       BIGSERIAL PRIMARY KEY,
  status          VARCHAR(16) NOT NULL,
  updated_at      TIMESTAMP   NOT NULL,
  umbrella_rest   INTEGER     NOT NULL,
  umbrella_total  INTEGER     NOT NULL,
  version         BIGINT      DEFAULT 0
);

-- 변경 사항을 NOTIFY로 쏘는 함수
CREATE OR REPLACE FUNCTION notify_locker_change() RETURNS trigger AS $$
DECLARE
  payload JSON;
BEGIN
  IF (TG_OP = 'DELETE') THEN
    payload := json_build_object(
      'locker_id', OLD.locker_id,
      'status', OLD.status,
      'umbrella_rest', OLD.umbrella_rest,
      'umbrella_total', OLD.umbrella_total,
      'deleted', true,
      'updated_at', to_char(NOW(), 'YYYY-MM-DD"T"HH24:MI:SS')
  );
  ELSE
    payload := json_build_object(
      'locker_id', NEW.locker_id,
      'status', NEW.status,
      'umbrella_rest', NEW.umbrella_rest,
      'umbrella_total', NEW.umbrella_total,
      'deleted', false,
      'updated_at', to_char(NEW.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS')
    );
  END IF;

  PERFORM pg_notify('locker_changes', payload::text);
  RETURN COALESCE(NEW, OLD);
END;
$$ LANGUAGE plpgsql;

-- 트리거: 우산함 DB INSERT/UPDATE마다 위 함수 실행
DROP TRIGGER IF EXISTS trg_notify_locker_change ON umbrella_lockers;

CREATE TRIGGER trg_notify_locker_change
AFTER INSERT OR UPDATE OR DELETE ON umbrella_lockers
FOR EACH ROW EXECUTE FUNCTION notify_locker_change();
