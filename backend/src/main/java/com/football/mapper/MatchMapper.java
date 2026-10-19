package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.Match;
import com.football.vo.StandingVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MatchMapper extends BaseMapper<Match> {

    @Select("SELECT m.*, ht.name as home_team_name, at.name as away_team_name, " +
            "mr.home_score, mr.away_score, mr.referee " +
            "FROM `match` m " +
            "LEFT JOIN team ht ON m.home_team_id = ht.id " +
            "LEFT JOIN team at ON m.away_team_id = at.id " +
            "LEFT JOIN match_result mr ON m.id = mr.match_id " +
            "WHERE m.deleted = 0 " +
            "ORDER BY m.match_time DESC")
    List<Match> selectAllWithDetails();

    @Select("<script>" +
            "SELECT m.*, ht.name as home_team_name, at.name as away_team_name, " +
            "mr.home_score, mr.away_score, mr.referee " +
            "FROM `match` m " +
            "LEFT JOIN team ht ON m.home_team_id = ht.id " +
            "LEFT JOIN team at ON m.away_team_id = at.id " +
            "LEFT JOIN match_result mr ON m.id = mr.match_id " +
            "WHERE m.deleted = 0 " +
            "<if test='status != null and status != \"\"'>" +
            "AND m.status = #{status} " +
            "</if>" +
            "ORDER BY m.match_time DESC" +
            "</script>")
    Page<Match> selectPageWithDetails(Page<Match> page, @Param("status") String status);

    @Select("SELECT " +
            "team_id, " +
            "team_name, " +
            "COUNT(*) as played, " +
            "SUM(CASE WHEN is_home = 1 AND home_score > away_score THEN 1 WHEN is_home = 0 AND away_score > home_score THEN 1 ELSE 0 END) as won, " +
            "SUM(CASE WHEN home_score = away_score THEN 1 ELSE 0 END) as drawn, " +
            "SUM(CASE WHEN is_home = 1 AND home_score < away_score THEN 1 WHEN is_home = 0 AND away_score < home_score THEN 1 ELSE 0 END) as lost, " +
            "SUM(CASE WHEN is_home = 1 THEN home_score ELSE away_score END) as goals_for, " +
            "SUM(CASE WHEN is_home = 1 THEN away_score ELSE home_score END) as goals_against, " +
            "(SUM(CASE WHEN is_home = 1 THEN home_score ELSE away_score END) - SUM(CASE WHEN is_home = 1 THEN away_score ELSE home_score END)) as goal_difference, " +
            "(SUM(CASE WHEN is_home = 1 AND home_score > away_score THEN 3 WHEN is_home = 0 AND away_score > home_score THEN 3 ELSE 0 END) + " +
             "SUM(CASE WHEN home_score = away_score THEN 1 ELSE 0 END)) as points " +
            "FROM (" +
                "SELECT " +
                    "m.home_team_id as team_id, " +
                    "ht.name as team_name, " +
                    "1 as is_home, " +
                    "mr.home_score, " +
                    "mr.away_score " +
                "FROM `match` m " +
                "JOIN team ht ON m.home_team_id = ht.id " +
                "JOIN match_result mr ON m.id = mr.match_id " +
                "WHERE m.status IN ('finished', 'ended') AND m.deleted = 0 " +
                "UNION ALL " +
                "SELECT " +
                    "m.away_team_id as team_id, " +
                    "at.name as team_name, " +
                    "0 as is_home, " +
                    "mr.home_score, " +
                    "mr.away_score " +
                "FROM `match` m " +
                "JOIN team at ON m.away_team_id = at.id " +
                "JOIN match_result mr ON m.id = mr.match_id " +
                "WHERE m.status IN ('finished', 'ended') AND m.deleted = 0 " +
            ") as team_matches " +
            "GROUP BY team_id, team_name " +
            "ORDER BY points DESC, goal_difference DESC, goals_for DESC")
    List<StandingVO> selectStandings();
}
