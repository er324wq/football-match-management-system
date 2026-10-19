package com.football.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.football.entity.CardRecord;
import com.football.service.CardRecordService;
import com.football.vo.ResultVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/card-record")
public class CardRecordController {

    @Autowired
    private CardRecordService cardRecordService;

    @GetMapping("/list")
    public ResultVO<Page<CardRecord>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) Long matchId,
                                           @RequestParam(required = false) Long playerId,
                                           @RequestParam(required = false) String cardType) {
        return ResultVO.success(cardRecordService.listWithDetails(page, size, matchId, playerId, cardType));
    }

    @GetMapping("/with-details")
    public ResultVO<java.util.List<CardRecord>> listWithDetails() {
        return ResultVO.success(cardRecordService.listWithDetails());
    }

    @GetMapping("/{id}")
    public ResultVO<CardRecord> getById(@PathVariable Long id) {
        CardRecord cardRecord = cardRecordService.getById(id);
        if (cardRecord == null) {
            return ResultVO.error("记录不存在");
        }
        return ResultVO.success(cardRecord);
    }

    @PostMapping
    public ResultVO<Void> save(@RequestBody CardRecord cardRecord) {
        boolean success = cardRecordService.save(cardRecord);
        return success ? ResultVO.success() : ResultVO.error("添加失败");
    }

    @PutMapping
    public ResultVO<Void> update(@RequestBody CardRecord cardRecord) {
        boolean success = cardRecordService.updateById(cardRecord);
        return success ? ResultVO.success() : ResultVO.error("更新失败");
    }

    @DeleteMapping("/{id}")
    public ResultVO<Void> delete(@PathVariable Long id) {
        boolean success = cardRecordService.removeById(id);
        return success ? ResultVO.success() : ResultVO.error("删除失败");
    }
}
