package com.football.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.football.entity.CardRecord;
import com.football.mapper.CardRecordMapper;
import com.football.service.CardRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CardRecordServiceImpl extends ServiceImpl<CardRecordMapper, CardRecord> implements CardRecordService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private volatile boolean tableReady = false;

    @Override
    public boolean save(CardRecord entity) {
        ensureTable();
        return super.save(entity);
    }

    @Override
    public boolean updateById(CardRecord entity) {
        ensureTable();
        return super.updateById(entity);
    }

    @Override
    public boolean removeById(java.io.Serializable id) {
        ensureTable();
        return super.removeById(id);
    }

    @Override
    public CardRecord getById(java.io.Serializable id) {
        ensureTable();
        return super.getById(id);
    }

    @Override
    public List<CardRecord> listWithDetails() {
        ensureTable();
        return baseMapper.selectAllWithDetails();
    }

    @Override
    public Page<CardRecord> listWithDetails(Integer page, Integer size, Long matchId, Long playerId, String cardType) {
        ensureTable();
        return baseMapper.selectPageWithDetails(new Page<>(page, size), matchId, playerId, cardType);
    }

    private synchronized void ensureTable() {
        if (tableReady) {
            return;
        }
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS card_record (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "match_id BIGINT NOT NULL," +
                "player_id BIGINT NOT NULL," +
                "card_type VARCHAR(20) NOT NULL," +
                "card_time INT NOT NULL," +
                "reason VARCHAR(255) DEFAULT NULL," +
                "deleted TINYINT DEFAULT 0," +
                "create_time DATETIME DEFAULT CURRENT_TIMESTAMP," +
                "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "INDEX idx_card_match_id (match_id)," +
                "INDEX idx_card_player_id (player_id)," +
                "INDEX idx_card_type (card_type)," +
                "FOREIGN KEY (match_id) REFERENCES `match`(id) ON DELETE CASCADE," +
                "FOREIGN KEY (player_id) REFERENCES player(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='红黄牌记录表'");
        tableReady = true;
    }
}
