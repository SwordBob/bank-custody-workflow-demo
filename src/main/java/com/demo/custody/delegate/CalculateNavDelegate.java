package com.demo.custody.delegate;

import com.demo.custody.entity.FundValuation;
import com.demo.custody.service.FundValuationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 估值 - 计算 NAV + 写估值表
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CalculateNavDelegate implements JavaDelegate {

    private final FundValuationService fundValuationService;

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String fundCode = (String) execution.getVariable("fundCode");
        BigDecimal totalAsset = (BigDecimal) execution.getVariable("totalAsset");
        String operator = (String) execution.getVariable("operator");

        log.info("🧮 [估值] 计算 NAV: fundCode={}, totalAsset={}", fundCode, totalAsset);
        // CalculateNav 在 UserTask 之前, operator 还没设置, 用 'pending-confirm' 占位
        FundValuation valuation = fundValuationService.saveValuation(
            businessKey, fundCode, totalAsset,
            operator != null ? operator : "pending-confirm"
        );

        execution.setVariable("nav", valuation.getNav());
        execution.setVariable("valuationId", valuation.getId());
    }
}
