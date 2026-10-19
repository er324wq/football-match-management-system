package com.football.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.CardRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CardRecordMapper extends BaseMapper<CardRecord> {

    @Select("SELECT cr.*, " +
            "CONCAT(ht.name, ' vs ', at.name) as match_info, " +
            "p.name as player_name, " +
            "t.name as team_name " +
            "FROM card_record cr " +
            "JOIN `match` m ON cr.match_id = m.id " +
            "JOIN team ht ON m.home_team_id = ht.id " +
            "JOIN team at ON m.away_team_id = at.id " +
            "JOIN player p ON cr.player_id = p.id " +
            "LEFT JOIN team t ON p.team_id = t.id " +
            "WHERE cr.deleted = 0 " +
            "ORDER BY m.match_time DESC, cr.card_time ASC")
    List<CardRecord> selectAllWithDetails();

    @Select({"<script>",
            "SELECT cr.*, " +
                    "CONCAT(ht.name, ' vs ', at.name) as match_info, " +
                    "p.name as player_name, " +
                    "t.name as team_name " +
                    "FROM card_record cr " +
                    "JOIN `match` m ON cr.match_id = m.id " +
                    "JOIN team ht ON m.home_team_id = ht.id " +
                    "JOIN team at ON m.away_team_id = at.id " +
                    "JOIN player p ON cr.player_id = p.id " +
                    "LEFT JOIN team t ON p.team_id = t.id " +
                    "WHERE cr.deleted = 0 " +
                    "<if test='matchId != null'>AND cr.match_id = #{matchId}</if> " +
                    "<if test='playerId != null'>AND cr.player_id = #{playerId}</if> " +
                    "<if test='cardType != null and cardType != \"\"'>AND cr.card_type = #{cardType}</if> " +
                    "ORDER BY m.match_time DESC, cr.card_time ASC",
            "</script>"})
    Page<CardRecord> selectPageWithDetails(Page<CardRecord> page,
                                           @Param("matchId") Long matchId,
                                           @Param("playerId") Long playerId,
                                           @Param("cardType") String cardType);
}
