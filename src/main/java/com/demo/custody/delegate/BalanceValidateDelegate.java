package com.demo.custody.delegate;

import com.demo.custody.entity.Voucher;
import com.demo.custody.service.VoucherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 登账 - 校验借贷平衡 + 写凭证表
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BalanceValidateDelegate implements JavaDelegate {

    private final VoucherService voucherService;

    @Override
    public void execute(DelegateExecution execution) {
        String businessKey = execution.getProcessInstanceBusinessKey();
        Object amountObj = execution.getVariable("amount");
        String accountNo = (String) execution.getVariable("accountNo");
        String tradeDateStr = (String) execution.getVariable("tradeDate");
        String operator = (String) execution.getVariable("operator");

        BigDecimal amount = amountObj != null ? new BigDecimal(amountObj.toString()) : BigDecimal.ZERO;
        LocalDate tradeDate = tradeDateStr != null ? LocalDate.parse(tradeDateStr) : LocalDate.now();

        log.info("🔍 [登账] 校验借贷平衡: businessKey={}, amount={}", businessKey, amount);

        Voucher voucher = Voucher.builder()
            .businessKey(businessKey)
            .amount(amount)
            .accountNo(accountNo)
            .tradeDate(tradeDate)
            .operator(operator != null ? operator : "system")
            .status("APPROVED")
            .build();
        voucherService.createVoucher(voucher);

        execution.setVariable("balanceChecked", true);
        execution.setVariable("voucherId", voucher.getId());
    }
}
