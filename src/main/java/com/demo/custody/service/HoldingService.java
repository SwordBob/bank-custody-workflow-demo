package com.demo.custody.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.demo.custody.entity.Holding;
import com.demo.custody.mapper.HoldingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * 持仓 Service - 估值流程前置数据
 *
 * 估值流程: FetchHoldingsDelegate 调用 sumMarketValueByFundCode()
 *          读取当天该基金所有持仓, 累加 market_value 作为 total_asset
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HoldingService {

    private final HoldingMapper holdingMapper;

    /**
     * 查某基金的当日持仓
     * 由 FetchHoldingsDelegate 调用
     */
    public List<Holding> listByFundCode(String fundCode) {
        return holdingMapper.selectList(
            new QueryWrapper<Holding>().eq("fund_code", fundCode)
        );
    }

    /**
     * 累加某基金的总市值
     */
    public BigDecimal sumMarketValueByFundCode(String fundCode) {
        List<Holding> holdings = listByFundCode(fundCode);
        BigDecimal sum = BigDecimal.ZERO;
        for (Holding h : holdings) {
            sum = sum.add(h.getMarketValue());
        }
        log.info("📥 [持仓] 累加: fundCode={}, 持仓数={}, 总市值={}", fundCode, holdings.size(), sum);
        return sum;
    }
}
