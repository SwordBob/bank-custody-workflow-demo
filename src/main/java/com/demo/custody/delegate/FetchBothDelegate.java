package com.demo.custody.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 对账 - 拉取两方数据
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FetchBothDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("📥 [对账] 拉取两方数据 (托管方 + 委托方)");
        execution.setVariable("custodyRecords", 1000);
        execution.setVariable("clientRecords", 1000);
    }
}
