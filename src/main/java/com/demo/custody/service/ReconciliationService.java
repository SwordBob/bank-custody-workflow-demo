package com.demo.custody.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.custody.entity.ReconciliationLog;
import com.demo.custody.mapper.ReconciliationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 对账日志 Service - 对账流程
 *
 * 关键业务:
 * - 差异=0: AUTO_PASSED (无差异自动通过)
 * - 差异>0: 走 UserTask 调账, 调账完成后 ADJUSTED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final ReconciliationLogMapper reconciliationLogMapper;

    /**
     * 对账完成, 写入结果
     * 由 WriteReconciliationDelegate 调用
     */
    public ReconciliationLog writeReconciliation(String businessKey, int diffCount, BigDecimal diffAmount, String operator) {
        String status = diffCount > 0 ? "ADJUSTED" : "AUTO_PASSED";
        ReconciliationLog record = ReconciliationLog.builder()
            .businessKey(businessKey)
            .diffCount(diffCount)
            .diffAmount(diffAmount)
            .status(status)
            .operator(diffCount > 0 ? operator : null)
            .note(diffCount > 0 ? "已调账" : "无差异自动通过")
            .build();
        reconciliationLogMapper.insert(record);
        log.info("💾 [对账] 写入: businessKey={}, diffCount={}, status={}", businessKey, diffCount, status);
        return record;
    }

    public ReconciliationLog getByBusinessKey(String businessKey) {
        return reconciliationLogMapper.selectOne(
            new QueryWrapper<ReconciliationLog>().eq("business_key", businessKey)
        );
    }

    public List<ReconciliationLog> listAll() {
        return reconciliationLogMapper.selectList(
            new QueryWrapper<ReconciliationLog>().orderByDesc("created_at")
        );
    }

    /**
     * 统计: 今日对账数 + 调账数
     */
    public java.util.Map<String, Long> todayStats() {
        List<ReconciliationLog> all = listAll();
        long total = all.size();
        long adjusted = all.stream().filter(l -> "ADJUSTED".equals(l.getStatus())).count();
        long autoPassed = all.stream().filter(l -> "AUTO_PASSED".equals(l.getStatus())).count();
        return java.util.Map.of(
            "total", total,
            "adjusted", adjusted,
            "autoPassed", autoPassed
        );
    }
}
