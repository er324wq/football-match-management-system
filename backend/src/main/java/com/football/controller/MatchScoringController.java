package com.football.controller;

import com.football.entity.MatchScoring;
import com.football.service.MatchScoringService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/scoring")
public class MatchScoringController {

    @Autowired
    private MatchScoringService matchScoringService;

    @GetMapping("/referee")
    public ResultVO<List<MatchScoring>> getByRefereeId(@RequestAttribute("userId") Long refereeId) {
        List<MatchScoring> scorings = matchScoringService.getByRefereeId(refereeId);
        return ResultVO.success(scorings);
    }

    @GetMapping("/match/{matchId}")
    public ResultVO<List<MatchScoring>> getByMatchId(@PathVariable Long matchId) {
        List<MatchScoring> scorings = matchScoringService.getByMatchId(matchId);
        return ResultVO.success(scorings);
    }

    @PostMapping("/match")
    public ResultVO<MatchScoring> createScoring(
            @RequestAttribute("userId") Long refereeId,
            @RequestParam Long matchId,
            @RequestParam Double score,
            @RequestParam String comment) {
        MatchScoring scoring = matchScoringService.createScoring(refereeId, matchId, score, comment);
        return ResultVO.success(scoring);
    }

    @GetMapping("/all")
    public ResultVO<List<MatchScoring>> getAllScorings() {
        List<MatchScoring> scorings = matchScoringService.list();
        return ResultVO.success(scorings);
    }
}
