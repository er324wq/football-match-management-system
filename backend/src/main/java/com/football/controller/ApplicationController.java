package com.football.controller;

import com.football.entity.Application;
import com.football.service.ApplicationService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/application")
public class ApplicationController {

    @Autowired
    private ApplicationService applicationService;

    @GetMapping("/user")
    public ResultVO<List<Application>> getByUserId(@RequestParam Long userId,
                                                   @RequestAttribute("userId") Long authenticatedUserId,
                                                   @RequestAttribute("role") String role) {
        Long targetUserId = "admin".equals(role) ? userId : authenticatedUserId;
        return ResultVO.success(applicationService.getByUserId(targetUserId));
    }

    @PostMapping("/team")
    public ResultVO<Application> createTeamApplication(@RequestBody Map<String, Object> params,
                                                       @RequestAttribute("userId") Long userId) {
        Long teamId = toLong(params.get("teamId"));
        String reason = toStr(params.get("reason"));
        if (teamId == null) {
            throw new IllegalArgumentException("teamId 不能为空");
        }
        return ResultVO.success(applicationService.createTeamApplication(userId, teamId, reason));
    }

    @PostMapping("/match")
    public ResultVO<Application> createMatchApplication(@RequestBody Map<String, Object> params,
                                                        @RequestAttribute("userId") Long userId) {
        Long matchId = toLong(params.get("matchId"));
        String reason = toStr(params.get("reason"));
        if (matchId == null) {
            throw new IllegalArgumentException("matchId 不能为空");
        }
        return ResultVO.success(applicationService.createMatchApplication(userId, matchId, reason));
    }

    @PostMapping("/create-match")
    public ResultVO<Application> createMatchCreateApplication(@RequestBody Map<String, Object> params,
                                                              @RequestAttribute("userId") Long userId) {
        String name = toStr(params.get("name"));
        String time = toStr(params.get("time"));
        String location = toStr(params.get("location"));
        String matchType = toStr(params.get("type"));
        String description = toStr(params.get("description"));
        if (userId == null || name.isEmpty()) {
            throw new IllegalArgumentException("userId 和 name 不能为空");
        }
        return ResultVO.success(applicationService.createMatchCreateApplication(
                userId, name, time, location, matchType, description
        ));
    }

    @PostMapping("/create-team")
    public ResultVO<Application> createTeamCreateApplication(@RequestBody Map<String, Object> params,
                                                             @RequestAttribute("userId") Long userId) {
        String name = toStr(params.get("name"));
        String description = toStr(params.get("description"));
        String city = toStr(params.get("city"));
        String coach = toStr(params.get("coach"));
        if (userId == null || name.isEmpty() || city.isEmpty() || coach.isEmpty()) {
            throw new IllegalArgumentException("userId、name、city、coach 不能为空");
        }
        return ResultVO.success(applicationService.createTeamCreateApplication(
                userId, name, description, city, coach
        ));
    }

    @PutMapping("/status/{id}")
    public ResultVO<Application> updateStatus(@PathVariable Long id, @RequestParam String status) {
        Application application = applicationService.updateStatus(id, status);
        return ResultVO.success(application);
    }

    @GetMapping("/all")
    public ResultVO<List<Application>> getAllApplications() {
        return ResultVO.success(applicationService.list());
    }

    @GetMapping("/match-events")
    public ResultVO<List<Application>> getApprovedMatchEvents() {
        return ResultVO.success(applicationService.getApprovedMatchEvents());
    }

    @GetMapping("/match-event/{eventId}/teams")
    public ResultVO<List<Map<String, Object>>> getEventTeamApplications(@PathVariable Long eventId) {
        return ResultVO.success(applicationService.getEventTeamApplications(eventId));
    }

    @GetMapping("/match-event/{eventId}/matches")
    public ResultVO<List<com.football.entity.Match>> getEventArrangedMatches(@PathVariable Long eventId) {
        return ResultVO.success(applicationService.getEventArrangedMatches(eventId));
    }

    @PostMapping("/match-event/{eventId}/random-arrange")
    public ResultVO<Integer> randomArrange(@PathVariable Long eventId) {
        return ResultVO.success(applicationService.randomArrangeMatches(eventId));
    }

    @GetMapping("/match-event/{eventId}/team/{teamId}/members")
    public ResultVO<List<Map<String, Object>>> getTeamMembersForLineup(@PathVariable Long eventId, @PathVariable Long teamId) {
        return ResultVO.success(applicationService.getTeamMembersForLineup(teamId));
    }

    @GetMapping("/match-event/{eventId}/team/{teamId}/lineup")
    public ResultVO<List<Map<String, Object>>> getTeamLineup(@PathVariable Long eventId, @PathVariable Long teamId) {
        return ResultVO.success(applicationService.getTeamLineup(eventId, teamId));
    }

    @PostMapping("/match-event/{eventId}/team/{teamId}/lineup")
    public ResultVO<Void> saveTeamLineup(
            @PathVariable Long eventId,
            @PathVariable Long teamId,
            @RequestBody Map<String, Object> params,
            @RequestAttribute("userId") Long arrangerUserId) {
        Object lineupObj = params.get("lineup");
        List<Map<String, Object>> lineup = new java.util.ArrayList<>();
        if (lineupObj instanceof List) {
            for (Object item : (List<?>) lineupObj) {
                if (item instanceof Map) {
                    lineup.add((Map<String, Object>) item);
                }
            }
        }
        applicationService.saveTeamLineup(eventId, teamId, arrangerUserId, lineup);
        return ResultVO.success();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception e) {
            throw new IllegalArgumentException("ID 参数格式错误");
        }
    }

    private String toStr(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
