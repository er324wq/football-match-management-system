package com.football.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("match_scoring")
public class MatchScoring {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long refereeId;

    private Long matchId;

    private Double score;

    private String comment;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}