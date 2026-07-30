package com.demo.custody.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.custody.entity.ClearingBatch;
import com.demo.custody.mapper.ClearingBatchMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清算批次 Service - 清算流程
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClearingService {

    private final ClearingBatchMapper clearingBatchMapper;

    /**
     * 清算批次初始化 (PENDING 状态)
     * 由 FetchPendingDelegate 调用
     */
    public ClearingBatch initBatch(String batchNo, String operator) {
        ClearingBatch batch = ClearingBatch.builder()
            .batchNo(batchNo)
            .operator(operator)
            .tradeCount(0)
            .totalAmount(java.math.BigDecimal.ZERO)
            .status("PENDING")
            .build();
        clearingBatchMapper.insert(batch);
        log.info("📥 [清算] 批次初始化: batchNo={}, operator={}", batchNo, operator);
        return batch;
    }

    /**
     * 清算确认
     * 由 WriteClearingDelegate 调用
     */
    public ClearingBatch confirmBatch(String batchNo, int tradeCount, java.math.BigDecimal totalAmount) {
        ClearingBatch batch = clearingBatchMapper.selectOne(
            new QueryWrapper<ClearingBatch>().eq("batch_no", batchNo)
        );
        if (batch == null) {
            throw new IllegalArgumentException("清算批次不存在: " + batchNo);
        }
        batch.setTradeCount(tradeCount);
        batch.setTotalAmount(totalAmount);
        batch.setStatus("CONFIRMED");
        batch.setConfirmedAt(LocalDateTime.now());
        clearingBatchMapper.updateById(batch);
        log.info("💾 [清算] 确认: batchNo={}, tradeCount={}, totalAmount={}", batchNo, tradeCount, totalAmount);
        return batch;
    }

    /**
     * 按批次号查
     */
    public ClearingBatch getByBatchNo(String batchNo) {
        return clearingBatchMapper.selectOne(
            new QueryWrapper<ClearingBatch>().eq("batch_no", batchNo)
        );
    }

    public List<ClearingBatch> listAll() {
        return clearingBatchMapper.selectList(null);
    }
}
