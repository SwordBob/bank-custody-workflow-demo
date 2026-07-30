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
 * 对账日志 (Reconciliation Log) - 对账流程业务实体
 *
 * 状态流转:
 * - PENDING (调账中) → ADJUSTED (调账完成) / AUTO_PASSED (无差异自动通过)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("reconciliation_log")
public class ReconciliationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessKey;

    /** 差异笔数 */
    private Integer diffCount;

    /** 差异金额 */
    private BigDecimal diffAmount;

    /** 状态: PENDING / ADJUSTED / AUTO_PASSED */
    private String status;

    /** 调账人 (无差异时为 null) */
    private String operator;

    /** 备注 */
    private String note;

    private LocalDateTime createdAt;
}
