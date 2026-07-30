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
 * 凭证 (Voucher) - 登账流程业务实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("voucher")
public class Voucher {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务主键 (= 流程实例 businessKey) */
    private String businessKey;

    /** 金额 */
    private BigDecimal amount;

    /** 账户 */
    private String accountNo;

    /** 交易日 */
    private LocalDate tradeDate;

    /** 经办人 */
    private String operator;

    /** 状态: PENDING / APPROVED / REJECTED */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
