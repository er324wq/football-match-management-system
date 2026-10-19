package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.MatchResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface MatchResultMapper extends BaseMapper<MatchResult> {

    @Select("SELECT id, match_id, home_score, away_score, referee, deleted, create_time, update_time "
            + "FROM match_result WHERE match_id = #{matchId} LIMIT 1")
    MatchResult selectIncludingDeletedByMatchId(@Param("matchId") Long matchId);

    @Update("UPDATE match_result SET home_score = #{homeScore}, away_score = #{awayScore}, "
            + "referee = #{referee}, deleted = 0 WHERE id = #{id}")
    int restoreDeletedResult(MatchResult result);

    @Select("SELECT mr.*, m.id as match_id, ht.name as home_team_name, at.name as away_team_name, " +
            "CONCAT(ht.name, ' vs ', at.name) as match_info " +
            "FROM match_result mr " +
            "JOIN `match` m ON mr.match_id = m.id " +
            "JOIN team ht ON m.home_team_id = ht.id " +
            "JOIN team at ON m.away_team_id = at.id " +
            "WHERE mr.deleted = 0 " +
            "ORDER BY m.match_time DESC")
    List<MatchResult> selectAllWithMatchInfo();

    @Select("SELECT mr.*, m.id as match_id, ht.name as home_team_name, at.name as away_team_name, " +
            "CONCAT(ht.name, ' vs ', at.name) as match_info " +
            "FROM match_result mr " +
            "JOIN `match` m ON mr.match_id = m.id " +
            "JOIN team ht ON m.home_team_id = ht.id " +
            "JOIN team at ON m.away_team_id = at.id " +
            "WHERE mr.deleted = 0 " +
            "ORDER BY m.match_time DESC")
    Page<MatchResult> selectPageWithMatchInfo(Page<MatchResult> page);
}
