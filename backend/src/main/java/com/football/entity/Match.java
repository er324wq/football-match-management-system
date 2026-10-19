package com.football.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`match`")
public class Match {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long homeTeamId;

    private Long awayTeamId;

    private LocalDateTime matchTime;

    private String stadium;

    private String status;

    @TableField(exist = false)
    private String tag;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String homeTeamName;

    @TableField(exist = false)
    private String awayTeamName;

    @TableField(exist = false)
    private Integer homeScore;

    @TableField(exist = false)
    private Integer awayScore;

    @TableField(exist = false)
    private String referee;
}
