package com.football.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.Application;
import com.football.entity.Match;

import java.util.Map;
import java.util.List;

public interface ApplicationService extends IService<Application> {

    List<Application> getByUserId(Long userId);

    Application createTeamApplication(Long userId, Long teamId, String reason);

    Application createMatchApplication(Long userId, Long matchId, String reason);

    Application createMatchCreateApplication(Long userId, String name, String time, String location, String matchType, String description);

    Application createTeamCreateApplication(Long userId, String name, String description, String city, String coach);

    Application updateStatus(Long id, String status);

    List<Application> getApprovedMatchEvents();

    List<Map<String, Object>> getEventTeamApplications(Long eventId);

    List<Match> getEventArrangedMatches(Long eventId);

    int randomArrangeMatches(Long eventId);

    List<Map<String, Object>> getTeamMembersForLineup(Long teamId);

    List<Map<String, Object>> getTeamLineup(Long eventId, Long teamId);

    void saveTeamLineup(Long eventId, Long teamId, Long arrangerUserId, List<Map<String, Object>> lineup);
}
