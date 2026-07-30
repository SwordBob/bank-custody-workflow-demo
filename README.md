# bank-custody-workflow-demo

> **银行资产托管 4 流程工作流 demo** —— Flowable 7 + MySQL 8
>
> 🟢 **GitHub**: [github.com/SwordBob/bank-custody-workflow-demo](https://github.com/SwordBob/bank-custody-workflow-demo)

## 这是什么

**简历项目 3: 银行资产托管 · 宁波银行 (2016-2017, 赢时胜外包)** 的**公开 demo**。

简历原文 (v4.4-B)：
> "主导宁波银行新一代资产托管系统 **Activiti 5.x 工作流引擎** 重构，4 个核心流程（**登账/清算/对账/估值**）独立硬编码，**对账必须在估值前**。业务规则上线周期从 2 周 → 2 天。"

本 demo 复现这 4 个 BPMN 2.0 流程 + 关键业务约束，可被 HR 直接验证。

## 4 流程时序

```
   登账 (Posting)          清算 (Clearing)         对账 (Reconciliation)     估值 (Valuation)
   ┌────────────┐         ┌─────────────┐         ┌─────────────────┐        ┌──────────────┐
   │ StartEvent │         │ StartEvent  │         │ StartEvent      │        │ StartEvent   │
   │     ↓      │         │     ↓       │         │     ↓           │        │     ↓        │
   │ 凭证录入   │         │ 拉取待清算  │         │ 拉取两方数据    │        │ 拉取持仓     │
   │  (UserTask)│         │(ServiceTask)│         │ (ServiceTask)   │        │(ServiceTask) │
   │     ↓      │         │     ↓       │         │     ↓           │        │     ↓        │
   │ 借贷平衡   │         │ 清算确认    │         │ 自动比对        │        │ 计算 NAV     │
   │  (SvcTask) │         │  (UserTask) │         │ (ServiceTask)   │        │(ServiceTask) │
   │     ↓      │         │     ↓       │         │     ↓           │        │     ↓        │
   │  EndEvent  │         │ 写入清算    │         │ 差异>0? (网关)  │        │ 估值确认     │
   └────────────┘         │ (SvcTask)   │         │  ├─ 是→ 调账    │        │  (UserTask)  │
                          │     ↓       │         │  └─ 否→ 通过    │        │     ↓        │
                          │  EndEvent   │         │     ↓           │        │ 写入估值表   │
                          └─────────────┘         │ 写入对账结果    │        │ (SvcTask)    │
                                                   │     ↓           │        │     ↓        │
                                                   │  EndEvent       │        │  EndEvent    │
                                                   └─────────────────┘        └──────────────┘

   ✅ 业务强约束: 对账流程的 "差异=0 通过" 必须在估值流程启动前完成
      (BPMN 流程间 SequenceFlow, 在 4 流程外层有 Composition)
```

## 技术栈

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 工作流引擎 | Flowable | 7.1.0 | Activiti 5.x 的开源 fork, API 95% 兼容 |
| 框架 | Spring Boot | 3.2.5 | 跟 mongo/pg demo 对齐 |
| 数据库 | MySQL | 8.0 | 简历写的 MySQL（生产用 5.7/8 都行）|
| ORM | MyBatis-Plus | 3.5.7 | 业务表读写 |
| JDK | OpenJDK | 17+ | 跟 mongo/pg demo 对齐 |
| 构建 | Maven | 3.9+ | 跟 mongo/pg demo 对齐 |

## 快速开始（3 步跑通）

### 1. 启动 MySQL + 应用

```bash
cd bank-custody-workflow-demo
docker-compose up -d
```

启动 2 个容器：
- `bank-custody-mysql` —— MySQL 8.0（含 Flowable 自动建 23 张 ACT_* 表）
- `bank-custody-app` —— Spring Boot 应用（端口 8082 → 容器内 8080）

### 2. 验证 4 流程已部署

```bash
# 健康检查
curl http://localhost:8082/api/custody/health
# → {"status":"UP","service":"bank-custody-workflow-demo","engine":"Flowable 7","database":"MySQL 8"}

# 列出 4 个流程定义
curl http://localhost:8082/api/custody/process-definitions
# → 4 个 BPMN 流程 (posting/clearing/reconciliation/valuation)
```

### 3. 启动一个流程

```bash
# 登账: 业务方提交凭证
curl -X POST "http://localhost:8082/api/custody/processes/posting/start?businessKey=POST-2026-07-30-001" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100000, "accountNo": "6225881234567", "tradeDate": "2026-07-30", "operator": "张三"}'
# → {"processInstanceId":"...","processDefinitionKey":"posting","status":"RUNNING"}

# 清算: 提交清算批次
curl -X POST "http://localhost:8082/api/custody/processes/clearing/start?businessKey=CLEAR-2026-07-30-001" \
  -H "Content-Type: application/json" \
  -d '{"batchNo": "B001", "operator": "李四"}'

# 对账: 触发日终对账
curl -X POST "http://localhost:8082/api/custody/processes/reconciliation/start?businessKey=RECON-2026-07-30" \
  -H "Content-Type: application/json" \
  -d '{"operator": "王五"}'

# 估值: 计算基金净值
curl -X POST "http://localhost:8082/api/custody/processes/valuation/start?businessKey=VAL-2026-07-30" \
  -H "Content-Type: application/json" \
  -d '{"fundCode": "FUND001", "operator": "赵六"}'
```

## 4 流程详解

### ① 登账 (Posting)

**业务含义**: 把交易流水记入总账 (会计科目)
**BPMN 节点**: StartEvent → 凭证录入 (UserTask) → 借贷平衡校验 (ServiceTask) → EndEvent
**关键变量**: `amount` (金额), `accountNo` (账户), `tradeDate` (交易日)
**业务规则**: 借贷必须平衡 (`SUM(debit) = SUM(credit)`), 否则抛错

### ② 清算 (Clearing)

**业务含义**: 批量清算待处理交易
**BPMN 节点**: StartEvent → 拉取待清算 (ServiceTask) → 清算确认 (UserTask) → 写入清算结果 (ServiceTask) → EndEvent
**关键变量**: `batchNo` (批次号), `tradeCount` (交易笔数), `totalAmount` (总金额)

### ③ 对账 (Reconciliation) ⭐

**业务含义**: 核对托管方与委托方数据, **差异>0 触发调账**
**BPMN 节点**: StartEvent → 拉取两方数据 (ServiceTask) → 自动比对 (ServiceTask) → **差异>0? (ExclusiveGateway)** → 调账处理 (UserTask) → 写入对账结果 → EndEvent
**关键变量**: `diffCount` (差异笔数), `diffAmount` (差异金额)
**业务规则**: 差异>0 必须人工调账; 差异=0 自动通过 (BPMN ExclusiveGateway + 条件表达式 `${diffCount > 0}`)

### ④ 估值 (Valuation)

**业务含义**: 计算基金/产品的净值 (NAV)
**BPMN 节点**: StartEvent → 拉取持仓 (ServiceTask) → 计算 NAV (ServiceTask) → 估值确认 (UserTask) → 写入估值表 → EndEvent
**关键变量**: `fundCode` (基金代码), `nav` (净值), `totalAsset` (总资产)
**公式**: `NAV = 总资产 / 份额`

## 业务规则：4 流程的强约束

| 业务约束 | BPMN 实现 | 简历亮点对应 |
|---------|-----------|-------------|
| **对账必须在估值前** | 流程间 SequenceFlow: reconciliation.END → valuation.START | "对账必须在估值前" |
| **清算依赖登账完成** | 流程间 SequenceFlow: posting.END → clearing.START | "登账 → 清算 → 对账 → 估值" 顺序 |
| **借贷必须平衡** | ServiceTask `BalanceValidateDelegate` 校验后 setVariable | "金融级准确性是硬要求" |
| **差异>0 必须人工调账** | ExclusiveGateway + UserTask 调账处理 | "流程实例持久化 + 事务回滚" |

## 状态

- [x] **Day 1** —— 骨架: pom + docker + 4 BPMN + 9 Delegate + 1 Controller + 4 端点
- [ ] **Day 2** —— 业务表: MyBatis-Plus 5 表 (凭证/清算批次/对账日志/持仓/估值)
- [ ] **Day 3** —— Delegate 真实业务: 真实读写业务表 + 流程变量绑定
- [ ] **Day 4** —— Quartz 定时: 每日对账任务自动跑 (简历 line "每日对账任务独立运行")
- [ ] **Day 5** —— 前端: 简单 HTML 任务看板 (Flowable Tasklist 简化版)
- [ ] **Day 6** —— README + 4 架构图 + smoke-test.sh + 公开仓

## 简历加分项

面试话术（30 秒版）：

> "**简历上那个 4 流程的工作流 demo, GitHub 上有公开仓库** —— 我用 Flowable 7 复现了 2016 年用 Activiti 5.x 做的宁波银行资产托管 4 流程：登账、清算、对账、估值。
> 关键设计是 **对账必须在估值前**（BPMN 流程间 SequenceFlow 控制），还有 **差异>0 走人工调账、差异=0 自动通过**（BPMN ExclusiveGateway + 条件表达式）。
> 现在用 Flowable 是因为 Activiti 5.x 已经 EOL 了 —— Flowable 是它的开源 fork，API 95% 兼容，我做迁移基本零成本。"

## 配套博客 / 简历

- 简历 v4.4-B: `resume-v4.4-B-insurance-securities.md` (项目 3: 银行资产托管)
- 配套 demo: `mongodb-user-profile-demo` / `pg-user-profile-demo` (双子星 MongoDB/PG)
