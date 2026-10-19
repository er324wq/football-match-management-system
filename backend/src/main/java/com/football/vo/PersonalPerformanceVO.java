package com.football.vo;

import lombok.Data;

@Data
public class PersonalPerformanceVO {
    private Boolean hasResult;
    private String playerName;
    private String teamName;
    private Integer goals;
    private Integer scorerRank;
    private Integer teamRank;
    private Integer teamPoints;
}
