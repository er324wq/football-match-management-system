package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("application")
public class Application {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String type; // team, match, create

    private Long targetId; // teamId or matchId

    private String name; // for create match

    /** 计划比赛时间；列名 schedule_time，避免与 MySQL 保留字 time 冲突 */
    @TableField("schedule_time")
    private String time;

    private String location; // for create match

    private String matchType; // for create match

    private String description; // for create match

    private String reason; // for team and match application

    private String status; // pending, approved, rejected

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}