package com.football.vo;

import lombok.Data;

@Data
public class StandingVO {

    private Long teamId;

    private String teamName;

    private Integer played;

    private Integer won;

    private Integer drawn;

    private Integer lost;

    private Integer goalsFor;

    private Integer goalsAgainst;

    private Integer goalDifference;

    private Integer points;
}
