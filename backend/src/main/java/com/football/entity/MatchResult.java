package com.football.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("match_result")
public class MatchResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchId;

    private Integer homeScore;

    private Integer awayScore;

    private String referee;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String matchInfo;

    @TableField(exist = false)
    private String homeTeamName;

    @TableField(exist = false)
    private String awayTeamName;
}
