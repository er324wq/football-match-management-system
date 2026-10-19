package com.football.controller;

import com.football.service.MatchService;
import com.football.service.PlayerService;
import com.football.service.TeamMemberService;
import com.football.service.TeamService;
import com.football.service.UserService;
import com.football.vo.ResultVO;
import com.football.vo.PersonalPerformanceVO;
import com.football.vo.StandingVO;
import com.football.vo.TopScorerVO;
import com.football.entity.Player;
import com.football.entity.Team;
import com.football.entity.TeamMember;
import com.football.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private MatchService matchService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @Autowired
    private TeamMemberService teamMemberService;

    @GetMapping("/standings")
    public ResultVO<java.util.List<StandingVO>> getStandings() {
        java.util.List<StandingVO> standings = matchService.getStandings();
        return ResultVO.success(standings);
    }

    @GetMapping("/top-scorers")
    public ResultVO<java.util.List<TopScorerVO>> getTopScorers() {
        java.util.List<TopScorerVO> topScorers = playerService.getTopScorers();
        return ResultVO.success(topScorers);
    }

    @GetMapping("/personal/{userId}")
    public ResultVO<PersonalPerformanceVO> getPersonalPerformance(
            @PathVariable Long userId,
            @org.springframework.web.bind.annotation.RequestAttribute("userId") Long authenticatedUserId,
            @org.springframework.web.bind.annotation.RequestAttribute("role") String role) {
        if (!"admin".equals(role) && !authenticatedUserId.equals(userId)) {
            throw new AccessDeniedException("无权查看其他用户的个人表现");
        }
        PersonalPerformanceVO vo = new PersonalPerformanceVO();
        vo.setHasResult(false);
        vo.setGoals(0);

        User user = userService.getById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            return ResultVO.success(vo);
        }

        java.util.List<TeamMember> teamMembers = teamMemberService.getByUserId(userId);
        Long teamId = (teamMembers == null || teamMembers.isEmpty()) ? null : teamMembers.get(0).getTeamId();
        Team team = teamId == null ? null : teamService.getById(teamId);

        java.util.List<String> candidateNames = new java.util.ArrayList<>();
        if (user.getNickname() != null && !user.getNickname().trim().isEmpty()) {
            candidateNames.add(user.getNickname().trim());
        }
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            candidateNames.add(user.getUsername().trim());
        }

        Player player = null;
        if (teamId != null && !candidateNames.isEmpty()) {
            player = playerService.getOne(new QueryWrapper<Player>()
                    .eq("team_id", teamId)
                    .eq("deleted", 0)
                    .in("name", candidateNames)
                    .last("LIMIT 1"));
        }
        if (player == null && !candidateNames.isEmpty()) {
            player = playerService.getOne(new QueryWrapper<Player>()
                    .eq("deleted", 0)
                    .in("name", candidateNames)
                    .last("LIMIT 1"));
        }

        if (player == null) {
            return ResultVO.success(vo);
        }

        vo.setHasResult(true);
        vo.setPlayerName(player.getName());
        vo.setTeamName(team == null ? "-" : team.getName());

        java.util.List<TopScorerVO> scorers = playerService.getTopScorers();
        if (scorers != null) {
            for (int i = 0; i < scorers.size(); i++) {
                TopScorerVO row = scorers.get(i);
                if (row.getPlayerId() != null && row.getPlayerId().equals(player.getId())) {
                    vo.setScorerRank(i + 1);
                    vo.setGoals(row.getGoalCount() == null ? 0 : row.getGoalCount());
                    break;
                }
            }
        }

        java.util.List<StandingVO> standings = matchService.getStandings();
        if (standings != null) {
            for (int i = 0; i < standings.size(); i++) {
                StandingVO standing = standings.get(i);
                boolean sameTeam = (team != null && standing.getTeamId() != null && standing.getTeamId().equals(team.getId()))
                        || (team != null && standing.getTeamName() != null && standing.getTeamName().equals(team.getName()));
                if (sameTeam) {
                    vo.setTeamRank(i + 1);
                    vo.setTeamPoints(standing.getPoints());
                    break;
                }
            }
        }

        return ResultVO.success(vo);
    }
}
