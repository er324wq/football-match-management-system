package com.football.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.Application;
import com.football.entity.Player;
import com.football.entity.Team;
import com.football.entity.TeamMember;
import com.football.exception.ResourceNotFoundException;
import com.football.service.ApplicationService;
import com.football.service.NotificationService;
import com.football.service.PlayerService;
import com.football.service.TeamMemberService;
import com.football.service.TeamService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/team")
public class TeamController {

    @Autowired
    private TeamService teamService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private NotificationService notificationService;

    @GetMapping("/list")
    public ResultVO<Page<Team>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String city) {

        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Team::getDeleted, 0).or().isNull(Team::getDeleted));
        wrapper.eq(Team::getStatus, 1);
        if (name != null && !name.isEmpty()) {
            wrapper.like(Team::getName, name);
        }
        if (city != null && !city.isEmpty()) {
            wrapper.like(Team::getCity, city);
        }
        wrapper.orderByDesc(Team::getCreateTime);

        Page<Team> pageResult = teamService.page(new Page<>(page, size), wrapper);
        return ResultVO.success(pageResult);
    }

    @GetMapping("/all")
    public ResultVO<List<Team>> getAllTeams() {
        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Team::getDeleted, 0).or().isNull(Team::getDeleted))
                .eq(Team::getStatus, 1)
                .orderByDesc(Team::getCreateTime);
        return ResultVO.success(teamService.list(wrapper));
    }

    @GetMapping("/{id}")
    public ResultVO<Team> getById(@PathVariable Long id) {
        Team team = teamService.getById(id);
        if (team != null && (team.getDeleted() == null || team.getDeleted() == 0)) {
            return ResultVO.success(team);
        }
        throw new ResourceNotFoundException("球队不存在");
    }

    @PostMapping
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<Void> save(@RequestBody Team team) {
        if (team.getStatus() == null) {
            team.setStatus(1);
        }
        if (team.getDeleted() == null) {
            team.setDeleted(0);
        }
        boolean success = teamService.save(team);
        if (!success) {
            return ResultVO.error("Create failed");
        }

        // If userId is provided, bind this team to user's "my teams" view via approved create_team application.
        if (team.getUserId() != null) {
            boolean existed = applicationService.count(new QueryWrapper<Application>()
                    .eq("user_id", team.getUserId())
                    .eq("type", "create_team")
                    .eq("target_id", team.getId())
                    .eq("status", "approved")) > 0;
            if (!existed) {
                Application app = new Application();
                app.setUserId(team.getUserId());
                app.setType("create_team");
                app.setTargetId(team.getId());
                app.setName(team.getName());
                app.setDescription("city=" + safe(team.getCity()) + ";coach=" + safe(team.getCoach()) + ";description=" + safe(team.getDescription()));
                app.setStatus("approved");
                if (!applicationService.save(app)) {
                    throw new IllegalStateException("球队已创建，但关联申请未能保存");
                }
            }
        }

        return ResultVO.success();
    }

    @PutMapping
    public ResultVO<Void> update(@RequestBody Team team) {
        boolean success = teamService.updateById(team);
        return success ? ResultVO.success() : ResultVO.error("Update failed");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        boolean success = teamService.removeById(id);
        return success ? ResultVO.success() : ResultVO.error("Delete failed");
    }

    @PutMapping("/dissolve/{id}")
    @Transactional(rollbackFor = Exception.class)
    public ResultVO<Void> dissolve(@PathVariable Long id) {
        Team team = teamService.getById(id);
        if (team == null || (team.getDeleted() != null && team.getDeleted() == 1)) {
            throw new ResourceNotFoundException("球队不存在");
        }

        String teamName = team.getName() == null ? ("#" + id) : team.getName();

        Set<Long> notifyUserIds = new HashSet<>();
        List<TeamMember> members = teamMemberService.list(new QueryWrapper<TeamMember>()
                .eq("team_id", id)
                .eq("status", 1));
        for (TeamMember member : members) {
            if (member.getUserId() != null) {
                notifyUserIds.add(member.getUserId());
            }
            member.setStatus(0);
            if (!teamMemberService.updateById(member)) {
                throw new IllegalStateException("球队成员解绑失败");
            }
            if (member.getUserId() != null) {
                teamMemberService.syncUserRoleByTeamMembership(member.getUserId());
            }
        }

        List<Application> relatedApps = applicationService.list(new QueryWrapper<Application>()
                .eq("target_id", id)
                .eq("status", "approved")
                .and(w -> w.eq("type", "team").or().eq("type", "create_team")));
        for (Application app : relatedApps) {
            if (app.getUserId() != null) {
                notifyUserIds.add(app.getUserId());
            }
        }

        List<Player> players = playerService.list(new QueryWrapper<Player>()
                .eq("team_id", id)
                .eq("deleted", 0));
        for (Player player : players) {
            player.setTeamId(null);
            if (!playerService.updateById(player)) {
                throw new IllegalStateException("球员球队关联清理失败");
            }
        }

        for (Long userId : notifyUserIds) {
            notificationService.createNotification(
                    userId,
                    "球队已解散",
                    "你所在的球队【" + teamName + "】已解散，你当前处于无队伍状态。",
                    "warning"
            );
        }

        boolean success = teamService.removeById(id);
        if (!success) {
            throw new IllegalStateException("球队解散失败");
        }
        return ResultVO.success();
    }

    @GetMapping("/user")
    public ResultVO<List<Team>> getUserTeams(@RequestParam Long userId,
                                             @RequestAttribute("userId") Long authenticatedUserId,
                                             @RequestAttribute("role") String role) {
        if (!"admin".equals(role)) {
            userId = authenticatedUserId;
        }
        List<Application> approvedApps = applicationService.list(new QueryWrapper<Application>()
                .eq("user_id", userId)
                .eq("status", "approved")
                .and(w -> w.eq("type", "create_team").or().eq("type", "team"))
                .isNotNull("target_id")
                .orderByDesc("update_time"));

        List<Long> teamIds = approvedApps.stream()
                .map(Application::getTargetId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());

        if (teamIds.isEmpty()) {
            return ResultVO.success(new ArrayList<>());
        }

        LambdaQueryWrapper<Team> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.eq(Team::getDeleted, 0).or().isNull(Team::getDeleted))
                .eq(Team::getStatus, 1)
                .in(Team::getId, teamIds)
                .orderByDesc(Team::getCreateTime);
        return ResultVO.success(teamService.list(wrapper));
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(";", " ");
    }
}
