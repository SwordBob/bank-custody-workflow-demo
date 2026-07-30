package com.demo.custody.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 基金估值 (Fund Valuation) - 估值流程业务实体
 *
 * NAV = 总资产 / 份额
 * 状态: PENDING (待确认) → CONFIRMED (已确认) / REJECTED
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("fund_valuation")
public class FundValuation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessKey;

    /** 基金代码 */
    private String fundCode;

    /** 总资产 = SUM(holding.market_value) */
    private BigDecimal totalAsset;

    /** 份额 */
    private Long shareCount;

    /** 净值 NAV = totalAsset / shareCount */
    private BigDecimal nav;

    /** 估值日期 */
    private LocalDate navDate;

    /** 经办人 */
    private String operator;

    /** 状态: PENDING / CONFIRMED / REJECTED */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
}
