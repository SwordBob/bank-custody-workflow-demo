package com.demo.custody.delegate;

import com.demo.custody.service.ClearingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 清算 - 写入清算结果
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WriteClearingDelegate implements JavaDelegate {

    private final ClearingService clearingService;

    @Override
    public void execute(DelegateExecution execution) {
        String batchNo = (String) execution.getVariable("batchNo");
        Integer tradeCount = (Integer) execution.getVariable("tradeCount");
        BigDecimal totalAmount = (BigDecimal) execution.getVariable("totalAmount");

        log.info("💾 [清算] 写入结果: batchNo={}, tradeCount={}, totalAmount={}", batchNo, tradeCount, totalAmount);
        clearingService.confirmBatch(batchNo,
            tradeCount != null ? tradeCount : 0,
            totalAmount != null ? totalAmount : BigDecimal.ZERO);
    }
}
