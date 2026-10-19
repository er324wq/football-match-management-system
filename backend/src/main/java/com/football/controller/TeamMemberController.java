package com.football.controller;

import com.football.entity.TeamMember;
import com.football.exception.ResourceNotFoundException;
import com.football.service.TeamMemberService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/team-member")
public class TeamMemberController {

    @Autowired
    private TeamMemberService teamMemberService;

    @GetMapping("/team/{teamId}")
    public ResultVO<List<TeamMember>> getByTeamId(@PathVariable Long teamId) {
        List<TeamMember> members = teamMemberService.getByTeamId(teamId);
        return ResultVO.success(members);
    }

    @GetMapping("/user")
    public ResultVO<List<TeamMember>> getByUserId(@RequestAttribute("userId") Long userId) {
        List<TeamMember> members = teamMemberService.getByUserId(userId);
        return ResultVO.success(members);
    }

    @PostMapping
    public ResultVO<Void> addMember(
            @RequestAttribute("userId") Long userId,
            @RequestBody TeamMember member) {
        boolean success = teamMemberService.addMember(member.getTeamId(), member.getUserId(), member.getRole());
        return success ? ResultVO.success() : ResultVO.error("添加成员失败");
    }

    @PutMapping("/role")
    public ResultVO<Void> updateMemberRole(
            @RequestAttribute("userId") Long userId,
            @RequestBody TeamMember member) {
        boolean success = teamMemberService.updateMemberRole(member.getTeamId(), member.getUserId(), member.getRole());
        if (!success) {
            throw new ResourceNotFoundException("球队成员不存在");
        }
        return ResultVO.success();
    }

    @DeleteMapping("/{teamId}/{memberUserId}")
    public ResultVO<Void> removeMember(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long teamId,
            @PathVariable Long memberUserId) {
        boolean success = teamMemberService.removeMember(teamId, memberUserId);
        if (!success) {
            throw new ResourceNotFoundException("球队成员不存在");
        }
        return ResultVO.success();
    }
}
