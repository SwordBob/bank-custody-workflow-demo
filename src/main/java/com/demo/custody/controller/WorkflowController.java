package com.demo.custody.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 银行资产托管 4 流程 REST API
 *
 * 端点：
 * - GET    /api/custody/health                    健康检查
 * - GET    /api/custody/process-definitions       列出 4 个部署的流程定义
 * - POST   /api/custody/processes/{key}/start     启动指定流程 (key: posting/clearing/reconciliation/valuation)
 * - GET    /api/custody/processes/{key}/instances 查流程实例列表
 *
 * 4 流程 key 约定（与 processes/*.bpmn20.xml 对应）：
 * - posting          登账
 * - clearing         清算
 * - reconciliation   对账
 * - valuation        估值
 *
 * 强约束: 对账必须在估值前 (BPMN SequenceFlow 控制)
 */
@RestController
@RequestMapping("/api/custody")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final RepositoryService repositoryService;
    private final RuntimeService runtimeService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "bank-custody-workflow-demo",
            "engine", "Flowable 7",
            "database", "MySQL 8"
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
     * 启动指定流程 (业务方提交登账/清算/对账/估值 任务)
     *
     * @param key 流程定义 key (posting/clearing/reconciliation/valuation)
     * @param businessKey 业务主键 (如 交易流水号)
     * @param variables 流程变量 (金额/账户/交易日等业务字段)
     */
    @PostMapping("/processes/{key}/start")
    public ResponseEntity<Map<String, Object>> startProcess(
            @PathVariable String key,
            @RequestParam(required = false) String businessKey,
            @RequestBody(required = false) Map<String, Object> variables) {

        log.info("启动流程: key={}, businessKey={}, variables={}", key, businessKey, variables);

        Map<String, Object> vars = variables != null ? variables : new HashMap<>();
        ProcessInstance instance = runtimeService.startProcessInstanceByKey(
            key, businessKey, vars
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("processInstanceId", instance.getId());
        result.put("processDefinitionKey", instance.getProcessDefinitionKey());
        result.put("businessKey", instance.getBusinessKey());
        result.put("startTime", instance.getStartTime());
        result.put("status", "RUNNING");
        return ResponseEntity.ok(result);
    }

    /**
     * 查指定流程的运行实例
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
}
