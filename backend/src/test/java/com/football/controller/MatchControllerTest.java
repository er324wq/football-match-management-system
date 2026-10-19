package com.football.controller;

import com.football.entity.Match;
import com.football.service.MatchService;
import com.football.vo.ResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MatchControllerTest {

    private MatchService matchService;
    private MatchController controller;

    @BeforeEach
    void setUp() {
        matchService = mock(MatchService.class);
        controller = new MatchController();
        ReflectionTestUtils.setField(controller, "matchService", matchService);
    }

    @Test
    void creatingFinishedMatchCreatesLinkedResult() {
        Match match = match(101L, "finished", 2, 1);
        when(matchService.createMatch(match)).thenReturn(true);

        ResultVO<Void> result = controller.save(match);

        assertEquals(0, result.getCode());
        org.mockito.Mockito.verify(matchService).createMatch(match);
    }

    @Test
    void updatingFinishedMatchUpdatesExistingResultInsteadOfCreatingDuplicate() {
        Match match = match(102L, "finished", 3, 0);
        when(matchService.updateMatch(match)).thenReturn(true);

        ResultVO<Void> result = controller.update(match);

        assertEquals(0, result.getCode());
        org.mockito.Mockito.verify(matchService).updateMatch(match);
    }

    @Test
    void changingMatchBackToUpcomingRemovesLinkedResult() {
        Match match = match(103L, "upcoming", null, null);
        when(matchService.updateMatch(match)).thenReturn(true);

        ResultVO<Void> result = controller.update(match);

        assertEquals(0, result.getCode());
        org.mockito.Mockito.verify(matchService).updateMatch(match);
    }

    @Test
    void failedMatchCreateDoesNotWriteResult() {
        Match match = match(104L, "finished", 1, 0);
        when(matchService.createMatch(match)).thenReturn(false);

        ResultVO<Void> result = controller.save(match);

        assertEquals(-1, result.getCode());
        org.mockito.Mockito.verify(matchService).createMatch(match);
    }

    @Test
    void missingMatchReadRaisesNotFound() {
        when(matchService.getById(999L)).thenReturn(null);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.football.exception.ResourceNotFoundException.class,
                () -> controller.getById(999L));
    }

    private Match match(Long id, String status, Integer homeScore, Integer awayScore) {
        Match match = new Match();
        match.setId(id);
        match.setStatus(status);
        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setReferee("referee");
        return match;
    }
}
