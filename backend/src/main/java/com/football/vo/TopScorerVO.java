package com.football.vo;

import lombok.Data;

@Data
public class TopScorerVO {

    private Long playerId;

    private String playerName;

    private String teamName;

    private Integer goalCount;
}
