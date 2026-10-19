USE football_admin_bench_20261017_a1;
CREATE TABLE bench_write_dup (
 id BIGINT NOT NULL AUTO_INCREMENT, match_id BIGINT NOT NULL, home_score INT DEFAULT 0, away_score INT DEFAULT 0, referee VARCHAR(50), deleted TINYINT DEFAULT 0,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_match_id(match_id), KEY idx_match_id(match_id),
 CONSTRAINT fk_bench_write_dup_match FOREIGN KEY(match_id) REFERENCES `match`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE bench_write_unique (
 id BIGINT NOT NULL AUTO_INCREMENT, match_id BIGINT NOT NULL, home_score INT DEFAULT 0, away_score INT DEFAULT 0, referee VARCHAR(50), deleted TINYINT DEFAULT 0,
 create_time DATETIME DEFAULT CURRENT_TIMESTAMP, update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_match_id(match_id),
 CONSTRAINT fk_bench_write_unique_match FOREIGN KEY(match_id) REFERENCES `match`(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_1' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_1' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_2' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_2' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_3' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_3' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_4' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_4' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_5' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_5' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_6' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_6' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_7' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_7' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_8' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_8' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_9' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_9' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_unique;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_unique (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'unique_round_10' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_unique;

TRUNCATE TABLE bench_write_dup;
START TRANSACTION;
SET @bench_started=NOW(6);
INSERT INTO bench_write_dup (match_id,home_score,away_score,referee,deleted) SELECT match_id,home_score,away_score,referee,deleted FROM match_result;
COMMIT;
SELECT 'dup_round_10' AS sample,COUNT(*) AS inserted_rows,ROUND(TIMESTAMPDIFF(MICROSECOND,@bench_started,NOW(6))/1000,3) AS elapsed_ms FROM bench_write_dup;
