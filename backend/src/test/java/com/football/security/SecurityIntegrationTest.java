package com.football.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Test
    void protectedUserListRejectsAnonymousRequest() {
        ResponseEntity<String> response = get("/api/user/list?page=1&size=10", null);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":401"));
    }

    @Test
    void publicMatchListAllowsAnonymousRequest() {
        assertEquals(HttpStatus.OK, get("/api/match/list?page=1&size=10", null).getStatusCode());
        assertEquals(HttpStatus.OK, get("/api/match-result/with-match", null).getStatusCode());
    }

    @Test
    void malformedAndExpiredTokensAreRejected() {
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/user/list", "not-a-jwt").getStatusCode());

        long now = System.currentTimeMillis();
        String wrongSignature = JWT.create()
                .withClaim("userId", 1L)
                .withClaim("username", "admin")
                .withClaim("role", "admin")
                .withExpiresAt(new Date(now + 60_000L))
                .sign(Algorithm.HMAC256(UUID.randomUUID().toString()));
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/user/list", wrongSignature).getStatusCode());

        String expired = JWT.create()
                .withClaim("userId", 1L)
                .withClaim("username", "admin")
                .withClaim("role", "admin")
                .withIssuedAt(new Date(now - 10_000L))
                .withExpiresAt(new Date(now - 1_000L))
                .sign(Algorithm.HMAC256(jwtSecret));
        assertEquals(HttpStatus.UNAUTHORIZED, get("/api/user/list", expired).getStatusCode());
    }

    @Test
    void normalUserCannotAccessAdminUserList() {
        ResponseEntity<String> response = get("/api/user/list?page=1&size=10",
                jwtService.createToken(3L, "user1", "user"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(response.getBody().contains("\"code\":403"));
    }

    @Test
    void administratorCanAccessAdminUserList() {
        assertEquals(HttpStatus.OK,
                get("/api/user/list?page=1&size=10", jwtService.createToken(1L, "admin", "admin")).getStatusCode());
        assertEquals(HttpStatus.OK,
                get("/api/scoring/all", jwtService.createToken(1L, "admin", "admin")).getStatusCode());
    }

    @Test
    void nonAdministratorCannotReadAllRefereeScores() {
        assertEquals(HttpStatus.FORBIDDEN,
                get("/api/scoring/all", jwtService.createToken(2L, "referee1", "referee")).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN,
                get("/api/scoring/referee", jwtService.createToken(3L, "user1", "user")).getStatusCode());
        assertEquals(HttpStatus.OK,
                get("/api/scoring/referee", jwtService.createToken(2L, "referee1", "referee")).getStatusCode());
    }

    @Test
    void onlyAdministratorCanCreateMatchesAndOnlyRefereeOrAdministratorCanCreateGoalRecords() {
        String userToken = jwtService.createToken(3L, "user1", "user");
        String refereeToken = jwtService.createToken(2L, "referee1", "referee");

        assertEquals(HttpStatus.FORBIDDEN, post("/api/match", "{}", userToken).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, post("/api/match", "{}", refereeToken).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, post("/api/goal-record", "{}", userToken).getStatusCode());
    }

    @Test
    void refereeGoalRecordRequestsRejectMissingOrOutOfRangeMinutes() {
        String refereeToken = jwtService.createToken(2L, "referee1", "referee");

        assertEquals(HttpStatus.BAD_REQUEST,
                post("/api/goal-record", "{\"matchId\":1,\"playerId\":1}", refereeToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                post("/api/goal-record", "{\"matchId\":1,\"playerId\":1,\"goalTime\":0}", refereeToken).getStatusCode());
        assertEquals(HttpStatus.BAD_REQUEST,
                post("/api/goal-record", "{\"matchId\":1,\"playerId\":1,\"goalTime\":131}", refereeToken).getStatusCode());
    }

    @Test
    void userCanReadOwnUserRecordButNotAnotherUsersRecord() {
        String token = jwtService.createToken(3L, "user1", "user");
        ResponseEntity<String> own = get("/api/user/3", token);
        assertEquals(HttpStatus.OK, own.getStatusCode());
        assertTrue(own.getBody().contains("\"code\":0"));

        ResponseEntity<String> other = get("/api/user/1", token);
        assertEquals(HttpStatus.FORBIDDEN, other.getStatusCode());
    }

    @Test
    void nonOwnersReceiveHttp403FromPersonalEndpoints() {
        String token = jwtService.createToken(3L, "user1", "user");
        assertEquals(HttpStatus.FORBIDDEN, get("/api/statistics/personal/1", token).getStatusCode());
        assertEquals(HttpStatus.FORBIDDEN, get("/api/notification/user?userId=1", token).getStatusCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Content-Type", "application/json");
        ResponseEntity<String> updateOtherUser = restTemplate.exchange(
                url("/api/user"), HttpMethod.PUT,
                new HttpEntity<>("{\"id\":1,\"nickname\":\"not-allowed\"}", headers), String.class);
        assertEquals(HttpStatus.FORBIDDEN, updateOtherUser.getStatusCode());
    }

    @Test
    void invalidPasswordIsRejected() {
        String body = "{\"username\":\"admin\",\"password\":\"definitely-not-the-password\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        try {
            new RestTemplate().exchange(
                    url("/api/auth/login"), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
        } catch (HttpClientErrorException exception) {
            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
            return;
        }
        throw new AssertionError("Invalid password must return HTTP 401");
    }

    private ResponseEntity<String> get(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(url(path), HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> post(String path, String body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.set("Content-Type", "application/json");
        return restTemplate.exchange(url(path), HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
