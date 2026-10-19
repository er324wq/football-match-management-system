package com.football.integration;

import com.football.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Real multi-table workflow tests, enabled only for the guarded Phase 5 disposable schema. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfSystemProperty(named = "phase5.db.integration", matches = "true")
class Phase5WorkflowIntegrationTest {
    private static final String SCHEMA = "football_admin_phase5_tx_20261017";

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void failedEventMatchMappingInsertRollsBackNewScheduleMatches() {
        assertDisposableSchema();
        Long eventId = createApprovedEvent("__phase5_arrange__");
        createApprovedTeamApplications(eventId);
        String trigger = "phase5_fail_arrangement_mapping";
        jdbcTemplate.execute("CREATE TRIGGER " + trigger + " BEFORE INSERT ON application "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase5 injected arrangement failure'");
        try {
            ResponseEntity<String> response = exchange("/api/application/match-event/" + eventId
                    + "/random-arrange", HttpMethod.POST, "{}");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertGenericServerError(response);
            assertEquals(0, count("SELECT COUNT(*) FROM `match` WHERE stadium='__phase5_arrange__'"));
            assertEquals(0, count("SELECT COUNT(*) FROM application WHERE type='event_match' AND target_id=" + eventId));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
            jdbcTemplate.update("DELETE FROM application WHERE target_id=?", eventId);
            jdbcTemplate.update("DELETE FROM application WHERE id=?", eventId);
        }
    }

    @Test
    void failedLineupInsertRestoresPreviouslySavedLineup() {
        assertDisposableSchema();
        Long eventId = createApprovedEvent("__phase5_lineup__");
        createApprovedTeamApplication(eventId, 1L);
        jdbcTemplate.update("INSERT INTO team_member (team_id,user_id,role,status) VALUES (1,3,'member',1)");
        jdbcTemplate.update("INSERT INTO application (user_id,type,target_id,name,reason,status) "
                        + "VALUES (1,'match_lineup',?,'1','userId=3;position=old','approved')", eventId);
        String trigger = "phase5_fail_lineup_insert";
        jdbcTemplate.execute("CREATE TRIGGER " + trigger + " BEFORE INSERT ON application "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase5 injected lineup failure'");
        try {
            ResponseEntity<String> response = exchange("/api/application/match-event/" + eventId
                    + "/team/1/lineup", HttpMethod.POST,
                    "{\"lineup\":[{\"userId\":3,\"position\":\"forward\"}]}");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertGenericServerError(response);
            assertEquals(1, count("SELECT COUNT(*) FROM application WHERE type='match_lineup' AND target_id=" + eventId
                    + " AND name='1' AND reason='userId=3;position=old'"));
            assertEquals(1, count("SELECT COUNT(*) FROM application WHERE type='match_lineup' AND target_id=" + eventId));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
            jdbcTemplate.update("DELETE FROM application WHERE target_id=?", eventId);
            jdbcTemplate.update("DELETE FROM application WHERE id=?", eventId);
            jdbcTemplate.update("DELETE FROM team_member WHERE team_id=1 AND user_id=3");
        }
    }

    @Test
    void failedTeamDissolveRollsBackMemberRolePlayerAndTeamChanges() {
        assertDisposableSchema();
        String originalRole = jdbcTemplate.queryForObject("SELECT role FROM user WHERE id=3", String.class);
        jdbcTemplate.update("UPDATE user SET role='player' WHERE id=3");
        jdbcTemplate.update("INSERT INTO team_member (team_id,user_id,role,status) VALUES (1,3,'member',1)");
        int notificationsBefore = count("SELECT COUNT(*) FROM notification WHERE user_id=3 AND title='球队已解散'");
        String trigger = "phase5_fail_dissolve_team_update";
        jdbcTemplate.execute("CREATE TRIGGER " + trigger + " BEFORE UPDATE ON team "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase5 injected dissolve failure'");
        try {
            ResponseEntity<String> response = exchange("/api/team/dissolve/1", HttpMethod.PUT, "");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertGenericServerError(response);
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                    "SELECT deleted FROM team WHERE id=1", Integer.class));
            assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                    "SELECT status FROM team_member WHERE team_id=1 AND user_id=3", Integer.class));
            assertEquals("player", jdbcTemplate.queryForObject("SELECT role FROM user WHERE id=3", String.class));
            assertEquals(Long.valueOf(1), jdbcTemplate.queryForObject(
                    "SELECT team_id FROM player WHERE id=1", Long.class));
            assertEquals(notificationsBefore,
                    count("SELECT COUNT(*) FROM notification WHERE user_id=3 AND title='球队已解散'"));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
            jdbcTemplate.update("DELETE FROM team_member WHERE team_id=1 AND user_id=3");
            jdbcTemplate.update("UPDATE user SET role=? WHERE id=3", originalRole);
        }
    }

    @Test
    void failedPlayerCreationRollsBackJoinApprovalMembershipAndUserRole() {
        assertDisposableSchema();
        String originalRole = jdbcTemplate.queryForObject("SELECT role FROM user WHERE id=3", String.class);
        jdbcTemplate.update("INSERT INTO application "
                + "(user_id,type,target_id,reason,status) VALUES (3,'team',1,'position=forward','pending')");
        Long applicationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM application WHERE user_id=3 "
                + "AND type='team' AND target_id=1 AND status='pending'", Long.class);
        String trigger = "phase5_fail_approved_player_insert";
        jdbcTemplate.execute("CREATE TRIGGER " + trigger + " BEFORE INSERT ON player "
                + "FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='phase5 injected join approval failure'");
        try {
            ResponseEntity<String> response = exchange("/api/application/status/" + applicationId
                    + "?status=approved", HttpMethod.PUT, "");

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
            assertGenericServerError(response);
            assertEquals("pending", jdbcTemplate.queryForObject(
                    "SELECT status FROM application WHERE id=?", String.class, applicationId));
            assertEquals(0, count("SELECT COUNT(*) FROM team_member WHERE team_id=1 AND user_id=3"));
            assertEquals(originalRole, jdbcTemplate.queryForObject("SELECT role FROM user WHERE id=3", String.class));
            assertEquals(0, count("SELECT COUNT(*) FROM player WHERE team_id=1 AND name='普通用户'"));
        } finally {
            jdbcTemplate.execute("DROP TRIGGER IF EXISTS " + trigger);
            jdbcTemplate.update("DELETE FROM application WHERE id=?", applicationId);
            jdbcTemplate.update("DELETE FROM team_member WHERE team_id=1 AND user_id=3");
            jdbcTemplate.update("UPDATE user SET role=? WHERE id=3", originalRole);
        }
    }

    @Test
    void repeatedMembershipAddIsIdempotentAndInactiveUniqueRowIsReactivated() {
        assertDisposableSchema();
        String originalRole = jdbcTemplate.queryForObject("SELECT role FROM user WHERE id=3", String.class);
        String body = "{\"teamId\":1,\"userId\":3,\"role\":\"member\"}";
        try {
            ResponseEntity<String> first = exchange("/api/team-member", HttpMethod.POST, body);
            ResponseEntity<String> duplicate = exchange("/api/team-member", HttpMethod.POST, body);
            assertEquals(HttpStatus.OK, first.getStatusCode());
            assertEquals(HttpStatus.OK, duplicate.getStatusCode());
            assertEquals(1, count("SELECT COUNT(*) FROM team_member WHERE team_id=1 AND user_id=3"));

            assertEquals(HttpStatus.OK, exchange("/api/team-member/1/3", HttpMethod.DELETE, "").getStatusCode());
            assertEquals(Integer.valueOf(0), jdbcTemplate.queryForObject(
                    "SELECT status FROM team_member WHERE team_id=1 AND user_id=3", Integer.class));
            assertEquals(HttpStatus.OK, exchange("/api/team-member", HttpMethod.POST, body).getStatusCode());
            assertEquals(1, count("SELECT COUNT(*) FROM team_member WHERE team_id=1 AND user_id=3"));
            assertEquals(Integer.valueOf(1), jdbcTemplate.queryForObject(
                    "SELECT status FROM team_member WHERE team_id=1 AND user_id=3", Integer.class));
            assertTrue(responseCode(first));
            assertTrue(responseCode(duplicate));
            ResponseEntity<String> missingMember = exchange("/api/team-member/1/999999", HttpMethod.DELETE, "");
            assertEquals(HttpStatus.NOT_FOUND, missingMember.getStatusCode());
            assertFalse(responseCode(missingMember));
        } finally {
            jdbcTemplate.update("DELETE FROM team_member WHERE team_id=1 AND user_id=3");
            jdbcTemplate.update("UPDATE user SET role=? WHERE id=3", originalRole);
        }
    }

    @Test
    void malformedQueryParameterReturnsHttp400WithSafeEnvelope() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/match/list?page=not-a-number", String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":400"));
        assertFalse(response.getBody().contains("NumberFormatException"));
    }

    @Test
    void missingMatchReturnsHttp404WithSafeEnvelope() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/match/99999999", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":404"));
        assertTrue(response.getBody().contains("比赛不存在"));
    }

    @Test
    void duplicateMatchResultReturnsHttp409WithoutDatabaseDetails() {
        assertDisposableSchema();
        ResponseEntity<String> response = exchange("/api/match-result", HttpMethod.POST,
                "{\"matchId\":1,\"homeScore\":2,\"awayScore\":1}");
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":409"));
        assertTrue(response.getBody().contains("完整性约束"));
        assertFalse(response.getBody().contains("Duplicate entry"));
        assertFalse(response.getBody().contains("uk_match_id"));
    }

    @Test
    void joinApprovalWithMissingTeamReturns404WithoutChangingApplicationOrMembership() {
        assertDisposableSchema();
        String originalRole = jdbcTemplate.queryForObject("SELECT role FROM user WHERE id=3", String.class);
        jdbcTemplate.update("INSERT INTO application (user_id,type,target_id,reason,status) "
                + "VALUES (3,'team',99999999,'phase5_missing_team','pending')");
        Long applicationId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM application WHERE user_id=3 "
                + "AND type='team' AND target_id=99999999 AND status='pending'", Long.class);
        try {
            ResponseEntity<String> response = exchange("/api/application/status/" + applicationId
                    + "?status=approved", HttpMethod.PUT, "");
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertTrue(response.getBody().contains("\"code\":404"));
            assertEquals("pending", jdbcTemplate.queryForObject(
                    "SELECT status FROM application WHERE id=?", String.class, applicationId));
            assertEquals(0, count("SELECT COUNT(*) FROM team_member WHERE team_id=99999999 AND user_id=3"));
            assertEquals(originalRole, jdbcTemplate.queryForObject("SELECT role FROM user WHERE id=3", String.class));
        } finally {
            jdbcTemplate.update("DELETE FROM application WHERE id=?", applicationId);
        }
    }

    private Long createApprovedEvent(String marker) {
        jdbcTemplate.update("INSERT INTO application (user_id,type,name,schedule_time,location,status) "
                        + "VALUES (1,'create',?,'2026-12-01 10:00:00',?,'approved')",
                marker, marker);
        return jdbcTemplate.queryForObject("SELECT id FROM application WHERE name=? AND type='create' ORDER BY id DESC LIMIT 1",
                Long.class, marker);
    }

    private void createApprovedTeamApplications(Long eventId) {
        createApprovedTeamApplication(eventId, 1L);
        createApprovedTeamApplication(eventId, 2L);
    }

    private void createApprovedTeamApplication(Long eventId, Long teamId) {
        jdbcTemplate.update("INSERT INTO application (user_id,type,target_id,reason,status) "
                        + "VALUES (1,'match',? ,?,'approved')",
                eventId, "teamId=" + teamId);
    }

    private boolean responseCode(ResponseEntity<String> response) {
        return response.getBody() != null && response.getBody().contains("\"code\":0");
    }

    private void assertGenericServerError(ResponseEntity<String> response) {
        assertTrue(response.getBody() != null && response.getBody().contains("\"code\":500"));
        assertTrue(response.getBody().contains("服务器内部错误"));
        assertFalse(response.getBody().contains("phase5 injected"));
        assertFalse(response.getBody().contains("SQLException"));
    }

    private void assertDisposableSchema() {
        assertEquals(SCHEMA, jdbcTemplate.queryForObject("SELECT DATABASE()", String.class),
                "Refusing Phase 5 fixture writes outside the named disposable schema");
    }

    private int count(String sql) {
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    private ResponseEntity<String> exchange(String path, HttpMethod method, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtService.createToken(1L, "admin", "admin"));
        headers.set("Content-Type", "application/json");
        return restTemplate.exchange("http://localhost:" + port + path, method,
                new HttpEntity<>(body, headers), String.class);
    }
}
