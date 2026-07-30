package com.demo.custody.controller;

import com.demo.custody.service.ClearingService;
import com.demo.custody.service.FundValuationService;
import com.demo.custody.service.ReconciliationService;
import com.demo.custody.service.VoucherService;
import com.demo.custody.task.DailyReconcileJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 银行资产托管 4 流程 REST API (含任务看板后端)
 *
 * 端点：
 * - GET    /api/custody/health                           健康检查
 * - GET    /api/custody/process-definitions              列出 4 个部署的流程定义
 * - POST   /api/custody/processes/{key}/start            启动指定流程
 * - GET    /api/custody/processes/{key}/instances        查流程实例列表
 * - GET    /api/custody/tasks?assignee=张三              查待办任务 (Flowable TaskService)
 * - POST   /api/custody/tasks/{taskId}/complete          完成任务
 * - POST   /api/custody/daily-reconcile/trigger          手动触发每日对账
 * - GET    /api/custody/daily-reconcile/stats            今日对账统计
 * - GET    /api/custody/business/vouchers                凭证列表
 * - GET    /api/custody/business/reconciliations         对账日志列表
 * - GET    /api/custody/business/valuations              估值列表
 */
@RestController
@RequestMapping("/api/custody")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final VoucherService voucherService;
    private final ClearingService clearingService;
    private final ReconciliationService reconciliationService;
    private final FundValuationService fundValuationService;
    private final DailyReconcileJob dailyReconcileJob;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "bank-custody-workflow-demo",
            "engine", "Flowable 7",
            "database", "MySQL 9.7.0 (本地)"
        ));
    }

    /**
     * 列出已部署的 4 个流程定义
     */
    @GetMapping("/process-definitions")
    public ResponseEntity<List<Map<String, Object>>> listProcessDefinitions() {
        List<ProcessDefinition> defs = repositoryService.createProcessDefinitionQuery()
            .orderByProcessDefinitionKey().asc()
            .list();
        List<Map<String, Object>> result = defs.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", d.getKey());
            m.put("name", d.getName());
            m.put("version", d.getVersion());
            m.put("id", d.getId());
            m.put("deploymentId", d.getDeploymentId());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 启动指定流程
     */
    @PostMapping("/processes/{key}/start")
    public ResponseEntity<Map<String, Object>> startProcess(
            @PathVariable String key,
            @RequestParam(required = false) String businessKey,
            @RequestBody(required = false) Map<String, Object> variables) {

        log.info("启动流程: key={}, businessKey={}", key, businessKey);
        Map<String, Object> vars = variables != null ? variables : new HashMap<>();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(key, businessKey, vars);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("processDefinitionKey", instance.getProcessDefinitionKey());
        result.put("businessKey", instance.getBusinessKey());
        result.put("startTime", instance.getStartTime());
        result.put("status", "RUNNING");
        return ResponseEntity.ok(result);
    }

    /**
     * 查流程实例
     */
    @GetMapping("/processes/{key}/instances")
    public ResponseEntity<List<Map<String, Object>>> listInstances(@PathVariable String key) {
        List<ProcessInstance> instances = runtimeService.createProcessInstanceQuery()
            .processDefinitionKey(key)
            .orderByStartTime().desc()
            .list();
        List<Map<String, Object>> result = instances.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i.getId());
            m.put("businessKey", i.getBusinessKey());
            m.put("startTime", i.getStartTime());
            m.put("status", i.isEnded() ? "ENDED" : "RUNNING");
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 查待办任务 (TaskService)
     * GET /api/custody/tasks?assignee=张三
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<Map<String, Object>>> listTasks(
            @RequestParam(required = false) String assignee) {
        List<Task> tasks = assignee != null
            ? taskService.createTaskQuery().taskAssignee(assignee).list()
            : taskService.createTaskQuery().list();

        List<Map<String, Object>> result = tasks.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("taskId", t.getId());
            m.put("name", t.getName());
            m.put("assignee", t.getAssignee());
            m.put("processInstanceId", t.getProcessInstanceId());
            m.put("processDefinitionKey", t.getProcessDefinitionId().split(":")[0]);
            m.put("createTime", t.getCreateTime());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /**
     * 完成任务 (UserTask 完成)
     * POST /api/custody/tasks/{taskId}/complete
     * body: { "operator": "张三" }  (可选, 写回流程变量)
     */
    @PostMapping("/tasks/{taskId}/complete")
    public ResponseEntity<Map<String, Object>> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) Map<String, Object> variables) {
        log.info("完成任务: taskId={}, variables={}", taskId, variables);
        if (variables != null && !variables.isEmpty()) {
            taskService.complete(taskId, variables);
        } else {
            taskService.complete(taskId);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", taskId);
        result.put("status", "COMPLETED");
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发每日对账
     * POST /api/custody/daily-reconcile/trigger
     */
    @PostMapping("/daily-reconcile/trigger")
    public ResponseEntity<Map<String, Object>> triggerDailyReconcile() {
        String processInstanceId = dailyReconcileJob.triggerManually();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", processInstanceId);
        result.put("status", "TRIGGERED");
        result.put("note", "已启动一个 reconciliation 流程, 跑完即完成");
        return ResponseEntity.ok(result);
    }

    /**
     * 今日对账统计
     */
    @GetMapping("/daily-reconcile/stats")
    public ResponseEntity<Map<String, Long>> getDailyReconcileStats() {
        return ResponseEntity.ok(dailyReconcileJob.getTodayStats());
    }

    // ==================== 业务数据查询 ====================

    @GetMapping("/business/vouchers")
    public ResponseEntity<List<com.demo.custody.entity.Voucher>> listVouchers() {
        return ResponseEntity.ok(voucherService.listAll());
    }

    @GetMapping("/business/reconciliations")
    public ResponseEntity<List<com.demo.custody.entity.ReconciliationLog>> listReconciliations() {
        return ResponseEntity.ok(reconciliationService.listAll());
    }

    @GetMapping("/business/valuations")
    public ResponseEntity<List<com.demo.custody.entity.FundValuation>> listValuations() {
        return ResponseEntity.ok(fundValuationService.listAll());
    }

    @GetMapping("/business/clearing-batches")
    public ResponseEntity<List<com.demo.custody.entity.ClearingBatch>> listClearingBatches() {
        return ResponseEntity.ok(clearingService.listAll());
    }
}
