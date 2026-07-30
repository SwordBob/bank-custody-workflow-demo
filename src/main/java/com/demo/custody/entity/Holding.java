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
 * 持仓 (Holding) - 估值流程的前置数据
 *
 * 每天清算完成后会更新当日持仓, 估值流程读取持仓计算 NAV
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("holding")
public class Holding {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 基金代码 */
    private String fundCode;

    /** 证券代码 (股票/债券) */
    private String securityCode;

    /** 持仓数量 */
    private BigDecimal quantity;

    /** 市价 */
    private BigDecimal marketPrice;

    /** 市值 = quantity × market_price */
    private BigDecimal marketValue;

    /** 估值日期 */
    private LocalDate valuationDate;

    private LocalDateTime createdAt;
}
