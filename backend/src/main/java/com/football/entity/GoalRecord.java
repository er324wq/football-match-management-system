package com.football.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@TableName("goal_record")
public class GoalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long matchId;

    private Long playerId;

    @NotNull(message = "进球时间不能为空")
    @Min(value = 1, message = "进球时间不能小于 1 分钟")
    @Max(value = 130, message = "进球时间不能大于 130 分钟")
    private Integer goalTime;

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
    private String playerName;

    @TableField(exist = false)
    private String teamName;
}
