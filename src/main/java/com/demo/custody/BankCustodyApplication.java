package com.demo.custody;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 银行资产托管 4 流程工作流 Demo
 *
 * 核心：4 个 BPMN 2.0 流程（登账 → 清算 → 对账 → 估值）
 *      引擎: Flowable 7（Activiti 5.x 的开源 fork, API 高度兼容）
 *      持久化: MySQL 8
 *
 * 简历对应：项目 3 · 银行资产托管 · 宁波银行 (2016-2017, 赢时胜外包)
 *   - "用 Activiti 5.x 工作流引擎重构 4 个核心流程"
 *   - "对账必须在估值前" (SequenceFlow 强约束)
 *   - "业务规则上线周期从 2 周 → 2 天"
 */
@SpringBootApplication
@MapperScan("com.demo.custody.mapper")
@EnableScheduling   // 开启 @Scheduled 定时任务 (每日对账)
public class BankCustodyApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCustodyApplication.class, args);
    }
}
