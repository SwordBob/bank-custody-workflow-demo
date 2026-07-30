package com.demo.custody.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 对账 - 自动比对两方数据
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoMatchDelegate implements JavaDelegate {

    @Override
    public void execute(DelegateExecution execution) {
        log.info("🔍 [对账] 自动比对两方数据");
        // Demo: 默认无差异, 走"通过"分支
        execution.setVariable("diffCount", 0);
        execution.setVariable("diffAmount", BigDecimal.ZERO);
    }
}
