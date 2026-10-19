package com.football.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.TeamMember;
import com.football.entity.User;
import com.football.mapper.TeamMemberMapper;
import com.football.service.TeamMemberService;
import com.football.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeamMemberServiceImpl extends ServiceImpl<TeamMemberMapper, TeamMember> implements TeamMemberService {

    @Autowired
    private UserService userService;

    @Override
    public List<TeamMember> getByTeamId(Long teamId) {
        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember>()
                        .eq("team_id", teamId)
                        .eq("status", 1)
        );
    }

    @Override
    public List<TeamMember> getByUserId(Long userId) {
        return baseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember>()
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
    }

    @Override
    public TeamMember getByTeamIdAndUserId(Long teamId, Long userId) {
        return baseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember>()
                        .eq("team_id", teamId)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addMember(Long teamId, Long userId, String role) {
        // The unique key covers inactive rows too, so reactivate a prior membership in place.
        TeamMember existing = baseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember>()
                        .eq("team_id", teamId)
                        .eq("user_id", userId));
        if (existing != null) {
            boolean active = existing.getStatus() != null && existing.getStatus() == 1;
            if (active && java.util.Objects.equals(existing.getRole(), role)) {
                syncUserRoleByTeamMembership(userId);
                return true;
            }
            existing.setRole(role);
            existing.setStatus(1);
            boolean success = updateById(existing);
            if (success) {
                syncUserRoleByTeamMembership(userId);
            }
            return success;
        }

        // 创建新成员
        TeamMember member = new TeamMember();
        member.setTeamId(teamId);
        member.setUserId(userId);
        member.setRole(role);
        member.setStatus(1);
        boolean success = save(member);
        if (success) {
            syncUserRoleByTeamMembership(userId);
        }
        return success;
    }

    @Override
    public boolean updateMemberRole(Long teamId, Long userId, String role) {
        TeamMember member = getByTeamIdAndUserId(teamId, userId);
        if (member != null) {
            member.setRole(role);
            return updateById(member);
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeMember(Long teamId, Long userId) {
        TeamMember member = getByTeamIdAndUserId(teamId, userId);
        if (member != null) {
            member.setStatus(0);
            boolean success = updateById(member);
            if (success) {
                syncUserRoleByTeamMembership(userId);
            }
            return success;
        }
        return false;
    }

    @Override
    public void syncUserRoleByTeamMembership(Long userId) {
        if (userId == null) {
            return;
        }
        User user = userService.getById(userId);
        if (user == null || (user.getDeleted() != null && user.getDeleted() == 1)) {
            return;
        }

        String role = user.getRole();
        if ("admin".equals(role) || "referee".equals(role)) {
            return;
        }

        long activeTeamCount = count(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TeamMember>()
                .eq("user_id", userId)
                .eq("status", 1));

        if (activeTeamCount > 0 && "user".equals(role)) {
            user.setRole("player");
            if (!userService.updateById(user)) {
                throw new IllegalStateException("用户角色同步失败");
            }
            return;
        }

        if (activeTeamCount == 0 && "player".equals(role)) {
            user.setRole("user");
            if (!userService.updateById(user)) {
                throw new IllegalStateException("用户角色同步失败");
            }
        }
    }
}
