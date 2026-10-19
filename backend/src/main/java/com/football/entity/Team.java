package com.football.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("team")
public class Team {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String coach;

    private String city;

    private String description;

    private Integer status;

    @TableField(exist = false)
    private Long userId;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
