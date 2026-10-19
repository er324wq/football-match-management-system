package com.football.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.football.entity.TeamMember;

import java.util.List;

public interface TeamMemberService extends IService<TeamMember> {
    List<TeamMember> getByTeamId(Long teamId);

    List<TeamMember> getByUserId(Long userId);

    TeamMember getByTeamIdAndUserId(Long teamId, Long userId);

    boolean addMember(Long teamId, Long userId, String role);

    boolean updateMemberRole(Long teamId, Long userId, String role);

    boolean removeMember(Long teamId, Long userId);

    void syncUserRoleByTeamMembership(Long userId);
}
