package com.demo.custody.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.custody.entity.FundValuation;
import com.demo.custody.mapper.FundValuationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 基金估值 Service - 估值流程
 *
 * NAV = 总资产 / 份额
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FundValuationService {

    /** 默认份额 (demo 用) */
    public static final long DEFAULT_SHARES = 50_000_000L;

    private final FundValuationMapper fundValuationMapper;

    /**
     * 计算并保存估值
     * 由 CalculateNavDelegate + WriteValuationDelegate 链式调用
     */
    public FundValuation saveValuation(String businessKey, String fundCode, BigDecimal totalAsset, String operator) {
        // 估值计算: NAV = totalAsset / 份额 (保留 4 位小数)
        BigDecimal nav = totalAsset.divide(
            BigDecimal.valueOf(DEFAULT_SHARES), 4, RoundingMode.HALF_UP
        );

        FundValuation valuation = FundValuation.builder()
            .businessKey(businessKey)
            .fundCode(fundCode)
            .totalAsset(totalAsset)
            .shareCount(DEFAULT_SHARES)
            .nav(nav)
            .navDate(java.time.LocalDate.now())
            .operator(operator != null ? operator : "system")
            .status("PENDING")
            .build();
        fundValuationMapper.insert(valuation);
        log.info("💾 [估值] 保存: fundCode={}, totalAsset={}, nav={}", fundCode, totalAsset, nav);
        return valuation;
    }

    /**
     * 估值确认
     * 由 WriteValuationDelegate 调用 (UserTask 完成后)
     */
    public FundValuation confirmValuation(String businessKey, String operator) {
        FundValuation valuation = fundValuationMapper.selectOne(
            new QueryWrapper<FundValuation>().eq("business_key", businessKey)
        );
        if (valuation == null) {
            throw new IllegalArgumentException("估值记录不存在: " + businessKey);
        }
        valuation.setStatus("CONFIRMED");
        valuation.setOperator(operator);
        valuation.setConfirmedAt(LocalDateTime.now());
        fundValuationMapper.updateById(valuation);
        log.info("💾 [估值] 确认: businessKey={}, operator={}, nav={}", businessKey, operator, valuation.getNav());
        return valuation;
    }

    public FundValuation getByBusinessKey(String businessKey) {
        return fundValuationMapper.selectOne(
            new QueryWrapper<FundValuation>().eq("business_key", businessKey)
        );
    }

    public List<FundValuation> listAll() {
        return fundValuationMapper.selectList(
            new QueryWrapper<FundValuation>().orderByDesc("created_at")
        );
    }
}
