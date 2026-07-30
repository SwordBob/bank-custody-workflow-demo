package com.demo.custody.delegate;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

/**
 * 4 流程的 9 个 ServiceTask Java Delegate 实现
 *
 * 关键设计：
 * 1. 每个 delegate 只做一件事（单一职责）
 * 2. 流程变量用 execution.getVariable() 读取
 * 3. 计算结果用 execution.setVariable() 写回
 * 4. 实际业务逻辑 (DB 读写) 在 Day 2-3 加,Day 1 只打日志演示流程跑通
 *
 * 简历亮点对应：
 *   "流程变量绑定业务字段 (金额/账户/交易日等)"
 *   "流程实例持久化 + 事务回滚 + 每日对账任务独立运行"
 */
@Slf4j
class BalanceValidateDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 登账: 校验借贷平衡
        Object amount = execution.getVariable("amount");
        log.info("🔍 [登账] 校验借贷平衡, amount={}", amount);
        // Day 2 真实业务: SELECT SUM(debit) = SUM(credit)
        execution.setVariable("balanceChecked", true);
    }
}

@Slf4j
@Component
class FetchPendingDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 清算: 拉取待清算数据
        String batchNo = (String) execution.getVariable("batchNo");
        log.info("📥 [清算] 拉取待清算数据, batchNo={}", batchNo);
        // Day 2 真实业务: SELECT * FROM trade WHERE status='PENDING'
        execution.setVariable("tradeCount", 100);
        execution.setVariable("totalAmount", 1_000_000.00);
    }
}

@Slf4j
class WriteClearingDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 清算: 写入清算结果
        String batchNo = (String) execution.getVariable("batchNo");
        Integer tradeCount = (Integer) execution.getVariable("tradeCount");
        log.info("💾 [清算] 写入清算结果, batchNo={}, tradeCount={}", batchNo, tradeCount);
        // Day 2 真实业务: INSERT INTO clearing_result (...)
    }
}

@Slf4j
class FetchBothDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 对账: 拉取两方对账数据
        log.info("📥 [对账] 拉取托管方+委托方数据");
        // Day 2 真实业务: SELECT * FROM custody_data UNION SELECT * FROM client_data
        execution.setVariable("custodyRecords", 1000);
        execution.setVariable("clientRecords", 1000);
    }
}

@Slf4j
class AutoMatchDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 对账: 自动比对
        log.info("🔍 [对账] 自动比对两方数据");
        // Day 2 真实业务: 外连接 + 差异检测
        // Demo 默认无差异, 走"直接通过"分支
        execution.setVariable("diffCount", 0);
        execution.setVariable("diffAmount", 0.0);
    }
}

@Slf4j
class WriteReconciliationDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 对账: 写入对账结果
        Integer diffCount = (Integer) execution.getVariable("diffCount");
        log.info("💾 [对账] 写入对账结果, diffCount={}", diffCount);
        // Day 2 真实业务: INSERT INTO reconciliation_log (...)
    }
}

@Slf4j
class FetchHoldingsDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 估值: 拉取持仓数据
        String fundCode = (String) execution.getVariable("fundCode");
        log.info("📥 [估值] 拉取基金 {} 持仓数据", fundCode);
        // Day 2 真实业务: SELECT * FROM holding WHERE fund_code=?
        execution.setVariable("totalAsset", 50_000_000.00);
    }
}

@Slf4j
class CalculateNavDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 估值: 计算 NAV
        Double totalAsset = (Double) execution.getVariable("totalAsset");
        // 简化公式: NAV = 总资产 / 份额 (假设 5000 万份)
        double nav = totalAsset != null ? totalAsset / 50_000_000 : 1.0;
        log.info("🧮 [估值] 计算 NAV = {}", nav);
        execution.setVariable("nav", nav);
        execution.setVariable("navDate", LocalDate.now().toString());
    }
}

@Slf4j
class WriteValuationDelegate implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) {
        // 估值: 写入估值表
        String fundCode = (String) execution.getVariable("fundCode");
        Double nav = (Double) execution.getVariable("nav");
        log.info("💾 [估值] 写入估值表, fundCode={}, nav={}", fundCode, nav);
        // Day 2 真实业务: INSERT INTO fund_valuation (...)
    }
}
