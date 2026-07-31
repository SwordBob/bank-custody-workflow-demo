package com.demo.custody.delegate;

import com.demo.custody.entity.ClearingBatch;
import com.demo.custody.service.ClearingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 清算 - 拉取待清算 + 初始化批次
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FetchPendingDelegate implements JavaDelegate {

    private final ClearingService clearingService;

    @Override
    public void execute(DelegateExecution execution) {
        String batchNo = (String) execution.getVariable("batchNo");
        // 兜底: 如果启动时没传 batchNo, 用 businessKey 顶上 (清算是按批次号组织的)
        if (batchNo == null || batchNo.isBlank()) {
            batchNo = execution.getProcessInstanceBusinessKey();
            if (batchNo == null || batchNo.isBlank()) {
                batchNo = "BATCH-" + execution.getProcessInstanceId().substring(0, 8);
            }
            execution.setVariable("batchNo", batchNo);
        }
        String operator = (String) execution.getVariable("operator");

        log.info("📥 [清算] 拉取待清算: batchNo={}", batchNo);
        ClearingBatch batch = clearingService.initBatch(batchNo, operator != null ? operator : "system");

        int tradeCount = 100;
        BigDecimal totalAmount = new BigDecimal("1000000.00");

        execution.setVariable("batchId", batch.getId());
        execution.setVariable("tradeCount", tradeCount);
        execution.setVariable("totalAmount", totalAmount);
    }
}
