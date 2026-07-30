package com.demo.custody.delegate;

import com.demo.custody.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 对账 - 写入对账结果
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WriteReconciliationDelegate implements JavaDelegate {

    private final ReconciliationService reconciliationService;

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        Integer diffCount = (Integer) execution.getVariable("diffCount");
        BigDecimal diffAmount = (BigDecimal) execution.getVariable("diffAmount");
        String operator = (String) execution.getVariable("operator");

        log.info("💾 [对账] 写入结果: businessKey={}, diffCount={}, diffAmount={}",
            businessKey, diffCount, diffAmount);
        reconciliationService.writeReconciliation(businessKey,
            diffCount != null ? diffCount : 0,
            diffAmount != null ? diffAmount : BigDecimal.ZERO,
            operator);
    }
}
