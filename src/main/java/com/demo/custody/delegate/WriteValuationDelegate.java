package com.demo.custody.delegate;

import com.demo.custody.service.FundValuationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 估值 - 确认 + 写回
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WriteValuationDelegate implements JavaDelegate {

    private final FundValuationService fundValuationService;

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        String operator = (String) execution.getVariable("operator");
        log.info("💾 [估值] 确认: businessKey={}, operator={}", businessKey, operator);
        fundValuationService.confirmValuation(businessKey, operator != null ? operator : "system");
    }
}
