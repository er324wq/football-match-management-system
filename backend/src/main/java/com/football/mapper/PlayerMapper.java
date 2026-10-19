package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.football.entity.Player;
import com.football.vo.TopScorerVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PlayerMapper extends BaseMapper<Player> {

    @Select("SELECT p.*, t.name as teamName FROM player p LEFT JOIN team t ON p.team_id = t.id WHERE p.deleted = 0 AND p.status = 1")
    List<Player> selectAllWithTeamName();

    @Select("SELECT p.id as player_id, p.name as player_name, t.name as team_name, COUNT(g.id) as goal_count " +
            "FROM player p " +
            "LEFT JOIN goal_record g ON p.id = g.player_id " +
            "LEFT JOIN team t ON p.team_id = t.id " +
            "WHERE p.deleted = 0 AND g.deleted = 0 " +
            "GROUP BY p.id, p.name, t.name " +
            "ORDER BY goal_count DESC")
    List<TopScorerVO> selectTopScorers();
}
