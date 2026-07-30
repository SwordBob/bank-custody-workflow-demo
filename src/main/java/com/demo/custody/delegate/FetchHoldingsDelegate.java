package com.demo.custody.delegate;

import com.demo.custody.service.FundValuationService;
import com.demo.custody.service.HoldingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 估值 - 拉取持仓累加
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FetchHoldingsDelegate implements JavaDelegate {

    private final HoldingService holdingService;

    @Override
    public void execute(DelegateExecution execution) {
        String fundCode = (String) execution.getVariable("fundCode");
        log.info("📥 [估值] 拉取持仓: fundCode={}", fundCode);

        BigDecimal totalAsset = holdingService.sumMarketValueByFundCode(fundCode);
        execution.setVariable("totalAsset", totalAsset);
        execution.setVariable("shareCount", FundValuationService.DEFAULT_SHARES);
    }
}
