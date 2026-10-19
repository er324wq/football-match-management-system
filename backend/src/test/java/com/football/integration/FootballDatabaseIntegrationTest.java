package com.football.integration;

import com.football.entity.GoalRecord;
import com.football.entity.MatchResult;
import com.football.entity.Player;
import com.football.service.GoalRecordService;
import com.football.service.MatchService;
import com.football.service.MatchResultService;
import com.football.service.PlayerService;
import com.football.vo.StandingVO;
import com.football.vo.TopScorerVO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MySQL-backed tests. Run only with -Dphase3.db.integration=true and a disposable
 * schema configured through SPRING_DATASOURCE_URL; never point this suite at business data.
 */
@SpringBootTest
@Transactional
@EnabledIfSystemProperty(named = "phase3.db.integration", matches = "true")
class FootballDatabaseIntegrationTest {

    @Autowired
    private MatchService matchService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private MatchResultService matchResultService;

    @Autowired
    private GoalRecordService goalRecordService;

    @Test
    void standingsAggregateWinDrawLossGoalsPointsAndOrderingFromSeedMatches() {
        List<StandingVO> standings = matchService.getStandings();

        assertEquals(4, standings.size());
        assertStanding(standings.get(0), 4L, 1, 1, 0, 0, 2, 0, 2, 3);
        assertStanding(standings.get(1), 1L, 1, 1, 0, 0, 2, 1, 1, 3);
        assertStanding(standings.get(2), 2L, 1, 0, 0, 1, 1, 2, -1, 0);
        assertStanding(standings.get(3), 3L, 1, 0, 0, 1, 0, 2, -2, 0);
    }

    @Test
    void scorerAggregateCountsSeedGoalRecords() {
        List<TopScorerVO> scorers = playerService.getTopScorers();
        Map<Long, Integer> counts = new HashMap<>();
        for (TopScorerVO scorer : scorers) {
            counts.put(scorer.getPlayerId(), scorer.getGoalCount());
        }

        assertEquals(5, counts.size());
        assertEquals(Integer.valueOf(1), counts.get(1L));
        assertEquals(Integer.valueOf(1), counts.get(2L));
        assertEquals(Integer.valueOf(1), counts.get(3L));
        assertEquals(Integer.valueOf(1), counts.get(7L));
        assertEquals(Integer.valueOf(1), counts.get(8L));
    }

    @Test
    void goalRecordInsertPersistsSubmittedMinuteAndRelations() {
        GoalRecord goal = new GoalRecord();
        goal.setMatchId(1L);
        goal.setPlayerId(1L);
        goal.setGoalTime(57);

        assertTrue(goalRecordService.save(goal));
        assertNotNull(goal.getId());
        GoalRecord persisted = goalRecordService.getById(goal.getId());
        assertEquals(Integer.valueOf(57), persisted.getGoalTime());

        GoalRecord detail = goalRecordService.listWithDetails().stream()
                .filter(row -> row.getId().equals(goal.getId()))
                .findFirst().orElse(null);
        assertNotNull(detail);
        assertEquals("猛虎队 vs 飞鹰队", detail.getMatchInfo());
        assertEquals("李四", detail.getPlayerName());
        assertEquals("猛虎队", detail.getTeamName());

        TopScorerVO scorer = playerService.getTopScorers().stream()
                .filter(row -> row.getPlayerId().equals(1L))
                .findFirst().orElse(null);
        assertNotNull(scorer);
        assertEquals(Integer.valueOf(2), scorer.getGoalCount());
    }

    @Test
    void duplicateResultForSameMatchIsRejectedByUniqueConstraint() {
        MatchResult duplicate = new MatchResult();
        duplicate.setMatchId(1L);
        duplicate.setHomeScore(9);
        duplicate.setAwayScore(9);

        org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> matchResultService.save(duplicate));
    }

    @Test
    void playerCannotReferenceMissingTeam() {
        Player player = new Player();
        player.setName("invalid-team-test");
        player.setTeamId(999999L);

        org.junit.jupiter.api.Assertions.assertThrows(DataIntegrityViolationException.class,
                () -> playerService.save(player));
    }

    private void assertStanding(StandingVO actual, Long teamId, int played, int won, int drawn,
                                int lost, int goalsFor, int goalsAgainst, int goalDifference, int points) {
        assertEquals(teamId, actual.getTeamId());
        assertEquals(Integer.valueOf(played), actual.getPlayed());
        assertEquals(Integer.valueOf(won), actual.getWon());
        assertEquals(Integer.valueOf(drawn), actual.getDrawn());
        assertEquals(Integer.valueOf(lost), actual.getLost());
        assertEquals(Integer.valueOf(goalsFor), actual.getGoalsFor());
        assertEquals(Integer.valueOf(goalsAgainst), actual.getGoalsAgainst());
        assertEquals(Integer.valueOf(goalDifference), actual.getGoalDifference());
        assertEquals(Integer.valueOf(points), actual.getPoints());
    }
}
