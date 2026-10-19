package com.football.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.Application;
import com.football.entity.Match;
import com.football.entity.Player;
import com.football.entity.Team;
import com.football.entity.TeamMember;
import com.football.entity.User;
import com.football.exception.ResourceNotFoundException;
import com.football.mapper.ApplicationMapper;
import com.football.service.ApplicationService;
import com.football.service.MatchService;
import com.football.service.NotificationService;
import com.football.service.PlayerService;
import com.football.service.TeamMemberService;
import com.football.service.TeamService;
import com.football.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ApplicationServiceImpl extends ServiceImpl<ApplicationMapper, Application> implements ApplicationService {

    @Autowired
    private TeamService teamService;

    @Autowired
    private MatchService matchService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private TeamMemberService teamMemberService;

    @Autowired
    private PlayerService playerService;

    @Autowired
    private UserService userService;

    @Override
    public List<Application> getByUserId(Long userId) {
        return baseMapper.selectList(new QueryWrapper<Application>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
    }

    @Override
    public Application createTeamApplication(Long userId, Long teamId, String reason) {
        Application application = new Application();
        application.setUserId(userId);
        application.setType("team");
        application.setTargetId(teamId);
        application.setReason(reason);
        application.setStatus("pending");
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        save(application);
        return application;
    }

    @Override
    public Application createMatchApplication(Long userId, Long matchId, String reason) {
        Application event = getById(matchId);
        if (event == null || !"create".equals(event.getType()) || !"approved".equals(event.getStatus())) {
            throw new IllegalArgumentException("赛事不存在或未通过审核，无法报名");
        }

        Long teamId = parseLong(parseMeta(reason).get("teamId"));
        if (teamId == null || teamId <= 0) {
            throw new IllegalArgumentException("报名失败：缺少球队信息");
        }

        Application existed = getOne(new QueryWrapper<Application>()
                .eq("type", "match")
                .eq("target_id", matchId)
                .eq("reason", reason)
                .last("LIMIT 1"));
        if (existed != null && ("pending".equals(existed.getStatus()) || "approved".equals(existed.getStatus()))) {
            return existed;
        }

        Application application = new Application();
        application.setUserId(userId);
        application.setType("match");
        application.setTargetId(matchId);
        application.setReason(reason);
        application.setStatus("pending");
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        save(application);
        return application;
    }

    @Override
    public Application createMatchCreateApplication(Long userId, String name, String time, String location, String matchType, String description) {
        Application application = new Application();
        application.setUserId(userId);
        application.setType("create");
        application.setName(name);
        application.setTime(time);
        application.setLocation(location);
        application.setMatchType(matchType);
        application.setDescription(description);
        application.setStatus("pending");
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        save(application);
        return application;
    }

    @Override
    public Application createTeamCreateApplication(Long userId, String name, String description, String city, String coach) {
        Application application = new Application();
        application.setUserId(userId);
        application.setType("create_team");
        application.setName(name);
        application.setDescription("city=" + safe(city) + ";coach=" + safe(coach) + ";description=" + safe(description));
        application.setStatus("pending");
        application.setCreateTime(LocalDateTime.now());
        application.setUpdateTime(LocalDateTime.now());
        save(application);
        return application;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Application updateStatus(Long id, String status) {
        Application application = getById(id);
        if (application == null) {
            throw new ResourceNotFoundException("申请记录不存在");
        }

        if ("approved".equals(status)) {
            if ("create_team".equals(application.getType())) {
                if (application.getTargetId() == null || application.getTargetId() <= 0 || teamService.getById(application.getTargetId()) == null) {
                    Long existingTeamId = findExistingTeamIdFromApplication(application);
                    application.setTargetId(existingTeamId != null ? existingTeamId : createTeamFromApplication(application));
                }
            } else if ("team".equals(application.getType())) {
                handleTeamJoinApproved(application);
            }
        }

        application.setStatus(status);
        application.setUpdateTime(LocalDateTime.now());
        if (!updateById(application)) {
            throw new IllegalStateException("申请状态更新失败: " + application.getId());
        }
        notifyApplicationStatus(application, status);
        return application;
    }

    @Override
    public List<Application> getApprovedMatchEvents() {
        return list(new QueryWrapper<Application>()
                .eq("type", "create")
                .eq("status", "approved")
                .orderByDesc("create_time"));
    }

    @Override
    public List<Map<String, Object>> getEventTeamApplications(Long eventId) {
        List<Application> apps = list(new QueryWrapper<Application>()
                .eq("type", "match")
                .eq("target_id", eventId)
                .orderByDesc("create_time"));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Application app : apps) {
            Map<String, String> meta = parseMeta(app.getReason());
            Long teamId = parseLong(meta.get("teamId"));
            Team team = teamId == null ? null : teamService.getById(teamId);
            User user = app.getUserId() == null ? null : userService.getById(app.getUserId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", app.getId());
            row.put("userId", app.getUserId());
            row.put("userName", user == null ? ("用户#" + app.getUserId()) : firstNotBlank(user.getNickname(), user.getUsername(), "用户#" + app.getUserId()));
            row.put("teamId", teamId);
            row.put("teamName", team == null ? firstNotBlank(meta.get("teamName"), "-") : team.getName());
            row.put("position", firstNotBlank(meta.get("position"), "待定"));
            row.put("reason", firstNotBlank(meta.get("reason"), "-"));
            row.put("status", app.getStatus());
            row.put("createTime", app.getCreateTime());
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Match> getEventArrangedMatches(Long eventId) {
        List<Application> mappings = list(new QueryWrapper<Application>()
                .eq("type", "event_match")
                .eq("target_id", eventId)
                .eq("status", "approved")
                .orderByAsc("id"));

        List<Match> matches = new ArrayList<>();
        for (Application mapping : mappings) {
            Long matchId = parseLong(mapping.getReason());
            if (matchId == null) {
                continue;
            }
            Match match = matchService.getById(matchId);
            if (match != null && (match.getDeleted() == null || match.getDeleted() == 0)) {
                matches.add(match);
            }
        }

        if (!matches.isEmpty()) {
            Map<Long, String> teamNameMap = teamService.list(new QueryWrapper<Team>().eq("deleted", 0))
                    .stream().collect(Collectors.toMap(Team::getId, Team::getName, (a, b) -> a));
            for (Match match : matches) {
                match.setHomeTeamName(teamNameMap.getOrDefault(match.getHomeTeamId(), "-"));
                match.setAwayTeamName(teamNameMap.getOrDefault(match.getAwayTeamId(), "-"));
            }
        }
        return matches;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int randomArrangeMatches(Long eventId) {
        Application event = getById(eventId);
        if (event == null || !"create".equals(event.getType()) || !"approved".equals(event.getStatus())) {
            throw new IllegalArgumentException("赛事不存在或未通过审核");
        }

        List<Application> approvedTeamApps = list(new QueryWrapper<Application>()
                .eq("type", "match")
                .eq("target_id", eventId)
                .eq("status", "approved")
                .orderByAsc("id"));

        Set<Long> teamIdSet = new LinkedHashSet<>();
        for (Application app : approvedTeamApps) {
            Long teamId = parseLong(parseMeta(app.getReason()).get("teamId"));
            if (teamId != null && teamId > 0) {
                teamIdSet.add(teamId);
            }
        }

        List<Long> teamIds = new ArrayList<>(teamIdSet);
        if (teamIds.size() < 2) {
            throw new IllegalArgumentException("至少需要 2 支已通过审核的参赛球队");
        }

        List<Application> oldMappings = list(new QueryWrapper<Application>()
                .eq("type", "event_match")
                .eq("target_id", eventId)
                .eq("status", "approved"));
        List<Long> oldMatchIds = oldMappings.stream()
                .map(Application::getReason)
                .map(this::parseLong)
                .filter(v -> v != null && v > 0)
                .collect(Collectors.toList());

        if (!oldMatchIds.isEmpty()) {
            List<Match> existingMatches = matchService.listByIds(oldMatchIds);
            List<Long> existingMatchIds = existingMatches.stream()
                    .map(Match::getId)
                    .collect(Collectors.toList());
            if (!existingMatchIds.isEmpty() && !matchService.removeByIds(existingMatchIds)) {
                throw new IllegalStateException("旧赛程清理失败");
            }
        }
        if (!oldMappings.isEmpty()) {
            if (!remove(new QueryWrapper<Application>()
                    .eq("type", "event_match")
                    .eq("target_id", eventId))) {
                throw new IllegalStateException("旧赛程关联清理失败");
            }
        }

        Collections.shuffle(teamIds);
        int arrangedCount = 0;
        LocalDateTime baseTime = parseMatchTime(event.getTime());
        String stadium = firstNotBlank(event.getLocation(), "待定球场");

        for (int i = 0; i + 1 < teamIds.size(); i += 2) {
            Match match = new Match();
            match.setHomeTeamId(teamIds.get(i));
            match.setAwayTeamId(teamIds.get(i + 1));
            match.setMatchTime(baseTime.plusHours(arrangedCount * 2L));
            match.setStadium(stadium);
            match.setStatus("upcoming");
            if (!matchService.save(match)) {
                throw new IllegalStateException("赛程比赛创建失败");
            }

            Application mapping = new Application();
            mapping.setUserId(event.getUserId());
            mapping.setType("event_match");
            mapping.setTargetId(eventId);
            mapping.setReason(String.valueOf(match.getId()));
            mapping.setStatus("approved");
            mapping.setName(event.getName());
            mapping.setCreateTime(LocalDateTime.now());
            mapping.setUpdateTime(LocalDateTime.now());
            if (!save(mapping)) {
                throw new IllegalStateException("赛程关联记录创建失败");
            }
            arrangedCount++;
        }
        return arrangedCount;
    }

    @Override
    public List<Map<String, Object>> getTeamMembersForLineup(Long teamId) {
        List<TeamMember> members = teamMemberService.getByTeamId(teamId);
        List<Map<String, Object>> rows = new ArrayList<>();
        for (TeamMember member : members) {
            User user = userService.getById(member.getUserId());
            if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", user.getId());
            row.put("name", firstNotBlank(user.getNickname(), user.getUsername(), "用户#" + user.getId()));
            row.put("role", firstNotBlank(member.getRole(), "member"));
            rows.add(row);
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> getTeamLineup(Long eventId, Long teamId) {
        List<Application> lineups = list(new QueryWrapper<Application>()
                .eq("type", "match_lineup")
                .eq("target_id", eventId)
                .eq("name", String.valueOf(teamId))
                .eq("status", "approved")
                .orderByAsc("id"));

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Application lineup : lineups) {
            Map<String, String> meta = parseMeta(lineup.getReason());
            Long userId = parseLong(meta.get("userId"));
            User user = userId == null ? null : userService.getById(userId);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("userId", userId);
            row.put("name", user == null ? ("用户#" + (userId == null ? "-" : userId)) : firstNotBlank(user.getNickname(), user.getUsername(), "用户#" + userId));
            row.put("position", firstNotBlank(meta.get("position"), "待定"));
            row.put("arrangerUserId", lineup.getUserId());
            row.put("createTime", lineup.getCreateTime());
            rows.add(row);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTeamLineup(Long eventId, Long teamId, Long arrangerUserId, List<Map<String, Object>> lineup) {
        Application event = getById(eventId);
        if (event == null || !"create".equals(event.getType()) || !"approved".equals(event.getStatus())) {
            throw new IllegalArgumentException("赛事不存在或未通过审核");
        }

        boolean teamApproved = list(new QueryWrapper<Application>()
                .eq("type", "match")
                .eq("target_id", eventId)
                .eq("status", "approved"))
                .stream()
                .anyMatch(item -> teamId.equals(parseLong(parseMeta(item.getReason()).get("teamId"))));
        if (!teamApproved) {
            throw new IllegalArgumentException("当前球队尚未通过该赛事参赛审核");
        }

        List<Application> previousLineups = list(new QueryWrapper<Application>()
                .eq("type", "match_lineup")
                .eq("target_id", eventId)
                .eq("name", String.valueOf(teamId)));
        if (!previousLineups.isEmpty() && !remove(new QueryWrapper<Application>()
                .eq("type", "match_lineup")
                .eq("target_id", eventId)
                .eq("name", String.valueOf(teamId)))) {
            throw new IllegalStateException("旧阵容清理失败");
        }

        Team team = teamService.getById(teamId);
        String teamName = team == null ? ("球队#" + teamId) : firstNotBlank(team.getName(), "球队#" + teamId);

        if (lineup == null) {
            lineup = new ArrayList<>();
        }

        for (Map<String, Object> row : lineup) {
            Long userId = toLong(row.get("userId"));
            String position = firstNotBlank(toStr(row.get("position")), "待定");
            if (userId == null || userId <= 0) {
                continue;
            }

            TeamMember member = teamMemberService.getByTeamIdAndUserId(teamId, userId);
            if (member == null || member.getStatus() == null || member.getStatus() != 1) {
                continue;
            }

            Application item = new Application();
            item.setUserId(arrangerUserId);
            item.setType("match_lineup");
            item.setTargetId(eventId);
            item.setName(String.valueOf(teamId));
            item.setReason("userId=" + userId + ";position=" + safe(position));
            item.setStatus("approved");
            item.setCreateTime(LocalDateTime.now());
            item.setUpdateTime(LocalDateTime.now());
            if (!save(item)) {
                throw new IllegalStateException("阵容记录保存失败");
            }

            try {
                notificationService.createNotification(
                        userId,
                        "参赛安排通知",
                        "你已被安排代表【" + teamName + "】参加赛事【" + firstNotBlank(event.getName(), "赛事#" + eventId) + "】，位置：" + position,
                        "info"
                );
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyApplicationStatus(Application application, String status) {
        String title = "";
        String content = "";
        String type = "info";

        if ("approved".equals(status)) {
            type = "success";
            if ("create_team".equals(application.getType())) {
                title = "球队创建申请已通过";
                content = "你申请创建的球队【" + nvl(application.getName(), "") + "】已通过审核。";
            } else if ("team".equals(application.getType())) {
                title = "入队申请已通过";
                content = "你的入队申请已通过审核。";
            } else if ("match".equals(application.getType())) {
                title = "参赛申请已通过";
                content = "你的参赛申请已通过审核。";
            } else if ("create".equals(application.getType())) {
                title = "赛事创建申请已通过";
                content = "你申请创建的赛事【" + nvl(application.getName(), "") + "】已通过审核，可开始接收战队报名。";
            }
        } else if ("rejected".equals(status)) {
            type = "danger";
            if ("create_team".equals(application.getType())) {
                title = "球队创建申请被拒绝";
                content = "你申请创建的球队【" + nvl(application.getName(), "") + "】未通过审核。";
            } else if ("team".equals(application.getType())) {
                title = "入队申请被拒绝";
                content = "你的入队申请未通过审核。";
            } else if ("match".equals(application.getType())) {
                title = "参赛申请被拒绝";
                content = "你的参赛申请未通过审核。";
            } else if ("create".equals(application.getType())) {
                title = "赛事创建申请被拒绝";
                content = "你申请创建的赛事【" + nvl(application.getName(), "") + "】未通过审核。";
            }
        }

        if (!title.isEmpty() && application.getUserId() != null) {
            try {
                notificationService.createNotification(application.getUserId(), title, content, type);
            } catch (Exception ignored) {
            }
        }
    }

    private Long createTeamFromApplication(Application application) {
        Map<String, String> meta = parseTeamMeta(application.getDescription());
        Team team = new Team();
        team.setName(application.getName());
        team.setCity(meta.getOrDefault("city", ""));
        team.setCoach(meta.getOrDefault("coach", ""));
        team.setDescription(meta.getOrDefault("description", ""));
        team.setStatus(1);
        team.setDeleted(0);
        team.setCreateTime(LocalDateTime.now());
        team.setUpdateTime(LocalDateTime.now());
        if (!teamService.save(team)) {
            throw new IllegalStateException("审批通过，但关联球队创建失败");
        }
        return team.getId();
    }

    private void handleTeamJoinApproved(Application application) {
        if (application.getTargetId() == null || application.getTargetId() <= 0 || application.getUserId() == null) {
            throw new IllegalArgumentException("入队申请缺少球队或用户信息");
        }

        Team targetTeam = teamService.getById(application.getTargetId());
        if (targetTeam == null || (targetTeam.getDeleted() != null && targetTeam.getDeleted() == 1)) {
            throw new ResourceNotFoundException("申请关联的球队不存在");
        }

        if (!teamMemberService.addMember(application.getTargetId(), application.getUserId(), "member")) {
            throw new IllegalStateException("球队成员同步失败");
        }

        Map<String, String> joinMeta = parseJoinMeta(application.getReason());
        String requestedPosition = joinMeta.getOrDefault("position", "").trim();
        if (requestedPosition.isEmpty()) {
            requestedPosition = "待定";
        }

        User user = userService.getById(application.getUserId());
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            throw new ResourceNotFoundException("申请关联的用户不存在");
        }
        String baseName = user != null && notBlank(user.getNickname())
                ? user.getNickname().trim()
                : (user != null && notBlank(user.getUsername()) ? user.getUsername().trim() : ("用户" + application.getUserId()));

        if (user != null && "user".equals(user.getRole())) {
            user.setRole("player");
            if (!userService.updateById(user)) {
                throw new IllegalStateException("用户角色同步失败");
            }
        }

        Player player = playerService.getOne(new QueryWrapper<Player>()
                .eq("team_id", application.getTargetId())
                .eq("name", baseName)
                .eq("deleted", 0)
                .last("LIMIT 1"));

        if (player == null) {
            player = playerService.getOne(new QueryWrapper<Player>()
                    .isNull("team_id")
                    .eq("name", baseName)
                    .eq("deleted", 0)
                    .last("LIMIT 1"));
        }

        if (player == null) {
            Player created = new Player();
            created.setName(baseName);
            created.setTeamId(application.getTargetId());
            created.setPosition(requestedPosition);
            created.setStatus(1);
            created.setDeleted(0);
            if (!playerService.save(created)) {
                throw new IllegalStateException("球员档案创建失败");
            }
        } else {
            player.setTeamId(application.getTargetId());
            player.setPosition(requestedPosition);
            if (player.getStatus() == null) {
                player.setStatus(1);
            }
            if (!playerService.updateById(player)) {
                throw new IllegalStateException("球员档案更新失败");
            }
        }
    }

    private Long findExistingTeamIdFromApplication(Application application) {
        if (!notBlank(application.getName())) {
            return null;
        }
        Team existing = teamService.getOne(new QueryWrapper<Team>()
                .eq("name", application.getName().trim())
                .eq("deleted", 0)
                .last("LIMIT 1"));
        return existing == null ? null : existing.getId();
    }

    private Map<String, String> parseTeamMeta(String raw) {
        Map<String, String> result = new HashMap<>();
        if (!notBlank(raw)) {
            return result;
        }
        if (raw.contains("city=") || raw.contains("coach=")) {
            String[] parts = raw.split(";");
            for (String part : parts) {
                int index = part.indexOf('=');
                if (index > 0) {
                    String key = part.substring(0, index).trim();
                    String val = part.substring(index + 1).trim();
                    result.put(key, val);
                }
            }
            return result;
        }
        result.put("description", raw);
        return result;
    }

    private Map<String, String> parseJoinMeta(String raw) {
        Map<String, String> result = new HashMap<>();
        if (!notBlank(raw)) {
            return result;
        }
        String[] parts = raw.split(";");
        for (String part : parts) {
            int index = part.indexOf('=');
            if (index > 0) {
                String key = part.substring(0, index).trim();
                String val = part.substring(index + 1).trim();
                result.put(key, val);
            }
        }
        if (result.isEmpty()) {
            result.put("reason", raw);
        }
        return result;
    }

    private Map<String, String> parseMeta(String raw) {
        return parseJoinMeta(raw);
    }

    private LocalDateTime parseMatchTime(String raw) {
        if (!notBlank(raw)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(raw.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception ignored) {
        }
        return LocalDateTime.now();
    }

    private Long parseLong(String raw) {
        if (!notBlank(raw)) {
            return null;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return parseLong(String.valueOf(value));
    }

    private String toStr(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String firstNotBlank(String... values) {
        for (String value : values) {
            if (notBlank(value)) {
                return value.trim();
            }
        }
        return "";
    }

    private String safe(String value) {
        return value == null ? "" : value.replace(";", " ");
    }

    private boolean notBlank(String v) {
        return v != null && !v.trim().isEmpty();
    }

    private String nvl(String v, String def) {
        return v == null ? def : v;
    }
}
