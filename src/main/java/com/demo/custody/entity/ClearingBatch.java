package com.demo.custody.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 清算批次 (Clearing Batch) - 清算流程业务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("clearing_batch")
public class ClearingBatch {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 批次号 */
    private String batchNo;

    /** 交易笔数 */
    private Integer tradeCount;

    /** 总金额 */
    private BigDecimal totalAmount;

    /** 经办人 */
    private String operator;

    /** 状态: PENDING / CONFIRMED / REJECTED */
    private String status;

    private LocalDateTime createdAt;

    /** 确认时间 */
    private LocalDateTime confirmedAt;
}
