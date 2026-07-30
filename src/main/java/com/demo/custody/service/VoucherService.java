package com.demo.custody.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.custody.entity.Voucher;
import com.demo.custody.mapper.VoucherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 凭证 Service - 登账流程
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherService {

    private final VoucherMapper voucherMapper;

    /**
     * 登账成功后, 创建凭证记录
     * 由 BalanceValidateDelegate 调用
     */
    public Voucher createVoucher(Voucher voucher) {
        voucher.setStatus("APPROVED");
        voucherMapper.insert(voucher);
        log.info("💾 [凭证] 创建: businessKey={}, amount={}", voucher.getBusinessKey(), voucher.getAmount());
        return voucher;
    }

    /**
     * 按业务主键查
     */
    public Voucher getByBusinessKey(String businessKey) {
        return voucherMapper.selectOne(
            new QueryWrapper<Voucher>().eq("business_key", businessKey)
        );
    }

    /**
     * 列出所有凭证
     */
    public List<Voucher> listAll() {
        return voucherMapper.selectList(null);
    }
}
