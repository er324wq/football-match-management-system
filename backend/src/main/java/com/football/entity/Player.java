package com.football.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("player")
public class Player {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer age;

    private String position;

    private Integer jerseyNumber;

    private Long teamId;

    private Integer status;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String teamName;
}
