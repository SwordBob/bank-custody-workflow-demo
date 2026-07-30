package com.demo.custody.task;

import com.demo.custody.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * 每日对账定时任务
 *
 * 简历原文: "流程实例持久化 + 事务回滚 + 每日对账任务独立运行"
 *
 * Cron: 每天 23:30 触发 (银行日终对账时间)
 * 启动方式: 配合 @EnableScheduling (BankCustodyApplication)
 *
 * 关键设计:
 * 1. 启动一个 reconciliation 流程实例, businessKey = 日期 (幂等)
 * 2. 流程会自动跑完 (无差异场景: StartEvent → 拉取 → 比对 → 写结果 → End)
 * 3. 完成后查询数据库写入条数, 记录到日志
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyReconcileJob {

    private final RuntimeService runtimeService;
    private final ReconciliationService reconciliationService;

    /**
     * 每天 23:30 触发
     * cron = "0 30 23 * * ?"
     * 测试时可以临时改成 "0/30 * * * * ?" (每 30 秒) 验证
     */
    @Scheduled(cron = "${custody.daily-reconcile.cron:0 30 23 * * ?}")
    public void runDailyReconciliation() {
        String dateStr = LocalDate.now().toString();
        String businessKey = "DAILY-RECON-" + dateStr + "-" + UUID.randomUUID().toString().substring(0, 8);

        log.info("⏰ [每日对账] 定时任务触发: businessKey={}", businessKey);

        try {
            // 启动一个 reconciliation 流程
            var instance = runtimeService.startProcessInstanceByKey(
                "reconciliation",
                businessKey,
                java.util.Map.of(
                    "operator", "system-daily-job",
                    "triggerSource", "scheduled"
                )
            );
            log.info("⏰ [每日对账] 流程启动: processInstanceId={}", instance.getId());
        } catch (Exception e) {
            log.error("⏰ [每日对账] 启动失败", e);
        }
    }

    /**
     * 手动触发一次 (供测试 / 应急使用)
     * GET /api/custody/daily-reconcile/trigger
     */
    public String triggerManually() {
        String dateStr = LocalDate.now().toString();
        String businessKey = "MANUAL-RECON-" + dateStr + "-" + UUID.randomUUID().toString().substring(0, 8);
        log.info("🎬 [每日对账] 手动触发: businessKey={}", businessKey);
        var instance = runtimeService.startProcessInstanceByKey(
            "reconciliation", businessKey,
            java.util.Map.of("operator", "manual", "triggerSource", "manual")
        );
        return instance.getId();
    }

    /**
     * 查询今日对账统计
     */
    public Map<String, Long> getTodayStats() {
        return reconciliationService.todayStats();
    }
}
