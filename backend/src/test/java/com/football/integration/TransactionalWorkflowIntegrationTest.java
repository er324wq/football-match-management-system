package com.football.integration;

import com.football.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real HTTP + MySQL rollback tests. Enabled only against the named disposable Phase 4 schema.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "phase4.db.integration", matches = "true")
class TransactionalWorkflowIntegrationTest {
    private static final String SCHEMA = "football_admin_phase4_tx_20261017";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void failedResultInsertRollsBackMatchInsert() {
        assertDisposableSchemaAndTransactionManager();
        String trigger = "phase4_fail_match_result_insert";
        jdbcTemplate.execute("CREATE TRIGGER " + trigger
                + " BEFORE INSERT ON match_result FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4 injected result failure'");
        try {
            String body = "{\"homeTeamId\":1,\"awayTeamId\":2,"
                    + "\"matchTime\":\"2026-09-01T11:00:00\","
                    + "\"stadium\":\"__phase4_tx_match__\",\"status\":\"finished\","
                    + "\"homeScore\":2,\"awayScore\":1}";
            ResponseEntity<String> response = post("/api/match", body);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals(0, count("SELECT COUNT(*) FROM `match` WHERE stadium='__phase4_tx_match__'"));
            assertEquals(0, count("SELECT COUNT(*) FROM match_result WHERE referee IS NULL AND match_id NOT IN (SELECT id FROM `match`)"));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
        }
    }

    @Test
    void failedApplicationInsertRollsBackTeamInsert() {
        assertDisposableSchemaAndTransactionManager();
        String trigger = "phase4_fail_application_insert";
        jdbcTemplate.execute("CREATE TRIGGER " + trigger
                + " BEFORE INSERT ON application FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4 injected application failure'");
        try {
            String body = "{\"name\":\"__phase4_tx_team__\",\"city\":\"test\","
                    + "\"coach\":\"test\",\"userId\":1}";
            ResponseEntity<String> response = post("/api/team", body);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals(0, count("SELECT COUNT(*) FROM team WHERE name='__phase4_tx_team__'"));
            assertEquals(0, count("SELECT COUNT(*) FROM application WHERE name='__phase4_tx_team__'"));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
        }
    }

    @Test
    void failedBatchStatusUpdateReturnsServerErrorAndRollsBackEarlierRows() {
        assertDisposableSchemaAndTransactionManager();
        String trigger = "phase4_fail_match_status_update";
        jdbcTemplate.update("INSERT INTO `match` (home_team_id, away_team_id, match_time, stadium, status, deleted) "
                        + "VALUES (1, 2, '2020-01-01 10:00:00', '__phase4_status__', 'upcoming', 0)");
        Long matchId = jdbcTemplate.queryForObject(
                "SELECT id FROM `match` WHERE stadium='__phase4_status__'", Long.class);
        jdbcTemplate.execute("CREATE TRIGGER " + trigger
                + " BEFORE UPDATE ON `match` FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4 injected status failure'");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtService.createToken(1L, "admin", "admin"));
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port
                            + "/api/match/update-status", HttpMethod.GET,
                    new HttpEntity<>(headers), String.class);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("upcoming", jdbcTemplate.queryForObject(
                    "SELECT status FROM `match` WHERE id=?", String.class, matchId));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
            jdbcTemplate.update("DELETE FROM `match` WHERE id=?", matchId);
        }
    }

    @Test
    void failedApprovedApplicationTeamCreationKeepsApplicationPending() {
        assertDisposableSchemaAndTransactionManager();
        String marker = "__phase4_approval__";
        jdbcTemplate.update("INSERT INTO application (user_id, type, name, description, status) "
                + "VALUES (1, 'create_team', ?, 'city=test;coach=test;description=', 'pending')", marker);
        Long applicationId = jdbcTemplate.queryForObject(
                "SELECT id FROM application WHERE name=?", Long.class, marker);
        String trigger = "phase4_fail_team_insert";
        jdbcTemplate.execute("CREATE TRIGGER " + trigger
                + " BEFORE INSERT ON team FOR EACH ROW "
                + "SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase4 injected team failure'");
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwtService.createToken(1L, "admin", "admin"));
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:" + port
                            + "/api/application/status/" + applicationId + "?status=approved",
                    HttpMethod.PUT, new HttpEntity<>(headers), String.class);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertEquals("pending", jdbcTemplate.queryForObject(
                    "SELECT status FROM application WHERE id=?", String.class, applicationId));
            assertEquals(0, count("SELECT COUNT(*) FROM team WHERE name='" + marker + "'"));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
            jdbcTemplate.update("DELETE FROM application WHERE id=?", applicationId);
        }
    }

    @Test
    void resultCanBeRestoredAfterMatchReturnsToUpcoming() {
        assertDisposableSchemaAndTransactionManager();
        String marker = "__phase4_result_restore__";
        Long matchId = null;
        try {
            String create = "{\"homeTeamId\":1,\"awayTeamId\":2,"
                    + "\"matchTime\":\"2026-09-01T11:00:00\",\"stadium\":\"" + marker
                    + "\",\"status\":\"finished\",\"homeScore\":2,\"awayScore\":1}";
            assertEquals(HttpStatus.OK, exchange("/api/match", HttpMethod.POST, create).getStatusCode());
            matchId = jdbcTemplate.queryForObject(
                    "SELECT id FROM `match` WHERE stadium=?", Long.class, marker);
            assertEquals(1, count("SELECT COUNT(*) FROM match_result WHERE match_id=" + matchId
                    + " AND deleted=0"));

            assertEquals(HttpStatus.OK, exchange("/api/match", HttpMethod.PUT,
                    "{\"id\":" + matchId + ",\"status\":\"upcoming\"}").getStatusCode());
            assertEquals(1, count("SELECT COUNT(*) FROM match_result WHERE match_id=" + matchId
                    + " AND deleted=1"));

            assertEquals(HttpStatus.OK, exchange("/api/match", HttpMethod.PUT,
                    "{\"id\":" + matchId + ",\"status\":\"finished\","
                            + "\"homeScore\":3,\"awayScore\":1}").getStatusCode());
            assertEquals(1, count("SELECT COUNT(*) FROM match_result WHERE match_id=" + matchId
                    + " AND deleted=0"));
            assertEquals(1, count("SELECT COUNT(*) FROM match_result WHERE match_id=" + matchId
                    + " AND home_score=3 AND away_score=1 AND deleted=0"));
            assertEquals(1, count("SELECT COUNT(*) FROM match_result WHERE match_id=" + matchId));

            assertEquals(HttpStatus.OK, exchange("/api/match", HttpMethod.PUT,
                    "{\"id\":" + matchId + ",\"status\":\"upcoming\"}").getStatusCode());
            assertEquals(HttpStatus.OK, exchange("/api/match", HttpMethod.PUT,
                    "{\"id\":" + matchId + ",\"status\":\"upcoming\","
                            + "\"stadium\":\"" + marker + "_updated\"}").getStatusCode());
            assertEquals(0, count("SELECT COUNT(*) FROM match_result WHERE match_id=" + matchId
                    + " AND deleted=0"));
            assertEquals(1, count("SELECT COUNT(*) FROM match_result WHERE match_id=" + matchId));
        } finally {
            if (matchId != null) {
                jdbcTemplate.update("DELETE FROM `match` WHERE id=?", matchId);
            }
        }
    }

    private void assertDisposableSchemaAndTransactionManager() {
        String expectedSchema = Boolean.getBoolean("phase5.db.integration")
                ? "football_admin_phase5_tx_20261017" : SCHEMA;
        assertEquals(expectedSchema, jdbcTemplate.queryForObject("SELECT DATABASE()", String.class),
                "Refusing to run destructive failure injection outside the explicitly selected disposable schema");
        assertTrue(transactionManager instanceof DataSourceTransactionManager,
                "Expected the Spring-managed JDBC transaction manager for the MyBatis datasource");
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private ResponseEntity<String> post(String path, String body) {
        return exchange(path, HttpMethod.POST, body);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.createToken(1L, "admin", "admin"));
        headers.set("Content-Type", "application/json");
        return restTemplate.exchange("http://localhost:" + port + path, method,
                new HttpEntity<>(body, headers), String.class);
    }
}
