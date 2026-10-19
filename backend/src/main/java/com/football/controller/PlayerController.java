package com.football.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.Player;
import com.football.exception.ResourceNotFoundException;
import com.football.service.PlayerService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @GetMapping("/list")
    public ResultVO<Page<Player>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Long teamId) {

        LambdaQueryWrapper<Player> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Player::getDeleted, 0);
        wrapper.eq(Player::getStatus, 1); // 只返回启用的球员
        if (name != null && !name.isEmpty()) {
            wrapper.like(Player::getName, name);
        }
        if (teamId != null) {
            wrapper.eq(Player::getTeamId, teamId);
        }
        wrapper.orderByDesc(Player::getCreateTime);

        Page<Player> pageResult = playerService.page(new Page<>(page, size), wrapper);
        return ResultVO.success(pageResult);
    }

    @GetMapping("/all")
    public ResultVO<java.util.List<Player>> getAllPlayers(@RequestParam(required = false) Long teamId) {
        LambdaQueryWrapper<Player> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Player::getDeleted, 0)
               .eq(Player::getStatus, 1); // 只返回启用的球员
        if (teamId != null) {
            wrapper.eq(Player::getTeamId, teamId);
        }
        wrapper.orderByDesc(Player::getCreateTime);
        java.util.List<Player> list = playerService.list(wrapper);
        return ResultVO.success(list);
    }

    @GetMapping("/with-team")
    public ResultVO<java.util.List<Player>> listWithTeamName() {
        java.util.List<Player> list = playerService.listWithTeamName();
        // 过滤掉已禁用和已删除的球员
        list.removeIf(player -> player.getDeleted() == 1 || player.getStatus() != 1);
        return ResultVO.success(list);
    }

    @GetMapping("/{id}")
    public ResultVO<Player> getById(@PathVariable Long id) {
        Player player = playerService.getById(id);
        if (player != null && player.getDeleted() == 0) {
            return ResultVO.success(player);
        }
        throw new ResourceNotFoundException("球员不存在");
    }

    @PostMapping
    public ResultVO<Void> save(@RequestBody Player player) {
        boolean success = playerService.save(player);
        return success ? ResultVO.success() : ResultVO.error("添加失败");
    }

    @PutMapping
    public ResultVO<Void> update(@RequestBody Player player) {
        boolean success = playerService.updateById(player);
        return success ? ResultVO.success() : ResultVO.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        boolean success = playerService.removeById(id);
        return success ? ResultVO.success() : ResultVO.error("删除失败");
    }

    @GetMapping("/team/{teamId}")
    public ResultVO<java.util.List<Player>> getByTeamId(@PathVariable Long teamId) {
        LambdaQueryWrapper<Player> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Player::getDeleted, 0)
               .eq(Player::getStatus, 1) // 只返回启用的球员
               .eq(Player::getTeamId, teamId)
               .orderByDesc(Player::getCreateTime);
        java.util.List<Player> list = playerService.list(wrapper);
        return ResultVO.success(list);
    }
}
