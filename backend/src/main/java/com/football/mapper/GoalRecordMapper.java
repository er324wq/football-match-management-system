package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.GoalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoalRecordMapper extends BaseMapper<GoalRecord> {

    @Select("SELECT gr.*, " +
            "CONCAT(ht.name, ' vs ', at.name) as match_info, " +
            "p.name as player_name, " +
            "t.name as team_name " +
            "FROM goal_record gr " +
            "JOIN `match` m ON gr.match_id = m.id " +
            "JOIN team ht ON m.home_team_id = ht.id " +
            "JOIN team at ON m.away_team_id = at.id " +
            "JOIN player p ON gr.player_id = p.id " +
            "JOIN team t ON p.team_id = t.id " +
            "WHERE gr.deleted = 0 " +
            "ORDER BY m.match_time DESC, gr.goal_time ASC")
    List<GoalRecord> selectAllWithDetails();

    @Select({"<script>",
            "SELECT gr.*, " +
            "CONCAT(ht.name, ' vs ', at.name) as match_info, " +
            "p.name as player_name, " +
            "t.name as team_name " +
            "FROM goal_record gr " +
            "JOIN `match` m ON gr.match_id = m.id " +
            "JOIN team ht ON m.home_team_id = ht.id " +
            "JOIN team at ON m.away_team_id = at.id " +
            "JOIN player p ON gr.player_id = p.id " +
            "JOIN team t ON p.team_id = t.id " +
            "WHERE gr.deleted = 0 " +
            "<if test='matchId != null'>AND gr.match_id = #{matchId}</if> " +
            "<if test='playerId != null'>AND gr.player_id = #{playerId}</if> " +
            "ORDER BY m.match_time DESC, gr.goal_time ASC",
            "</script>"})
    Page<GoalRecord> selectPageWithDetails(Page<GoalRecord> page, @Param("matchId") Long matchId, @Param("playerId") Long playerId);
}
