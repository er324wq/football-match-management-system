package com.football.service;

import com.football.entity.GoalRecord;
import com.football.mapper.GoalRecordMapper;
import com.football.service.impl.GoalRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GoalRecordServiceTest {

    private GoalRecordMapper goalRecordMapper;
    private GoalRecordServiceImpl goalRecordService;

    @BeforeEach
    void setUp() {
        goalRecordMapper = mock(GoalRecordMapper.class);
        goalRecordService = new GoalRecordServiceImpl();
        ReflectionTestUtils.setField(goalRecordService, "baseMapper", goalRecordMapper);
        when(goalRecordMapper.insert(any(GoalRecord.class))).thenReturn(1);
    }

    @Test
    void savePreservesSubmittedMatchMinute() {
        GoalRecord record = new GoalRecord();
        record.setMatchId(42L);
        record.setPlayerId(7L);
        record.setGoalTime(57);

        assertTrue(goalRecordService.save(record));

        assertEquals(57, record.getGoalTime());
        verify(goalRecordMapper).insert(record);
    }
}
