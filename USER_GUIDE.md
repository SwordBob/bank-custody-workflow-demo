# 银行资产托管 4 流程 Demo · 使用手册

> **面向**：面试官 / 候选人的求职演示 / Flowable 7 学习者
>
> **配套**：[README.md](./README.md)（10 秒亮点）/ [docs/](./docs/)（4 张架构图）

---

## 1. 这是什么

简历项目 3（**银行资产托管 · 宁波银行 2016-2017, 赢时胜外包**）的公开 demo。
用 **Flowable 7.x**（Activiti 5.x 的开源 fork，API 100% 兼容）复现 4 个核心 BPMN 2.0 流程。

**关键业务约束**（面试官最关心的）：
- 4 流程独立硬编码 → 配置化（BPMN XML 替代 Java 代码）
- **对账必须在估值前**（资金/资产账实相符）
- 业务规则上线周期 2 周 → 2 天

---

## 2. 30 秒快速开始

```bash
# 1. 启动 (MySQL + Spring Boot + smoke-test)
./start-demo.sh
# → 等待 14s 服务就绪, 自动跑 smoke-test 20 PASS

# 2. 浏览器点 4 Tab 一键演示 (5 分钟)
open http://localhost:8080/demo.html

# 3. API 验证
curl http://localhost:8080/api/custody/health
# {"status":"UP","engine":"Flowable 7","database":"MySQL 9.7.0 (本地)"}

# 4. 停服务
./stop-demo.sh
```

**环境要求**：
- Java 17 (Zulu 17 已装在 `/tmp/zulu17.50.19-ca-jdk17.0.11-macosx_aarch64/`)
- Maven 3.9+ (apache-maven-3.9.16 已装在 `/tmp/`)
- MySQL 8/9（`./start-demo.sh` 会自动启动本地 MySQL）
- Node 16+ + `npm i -g playwright`（**仅端到端测试需要**）

---

## 3. 4 流程业务详解

### 3.1 登账 (Posting)

**业务**：资金收付后，生成会计凭证。

**BPMN 节点**：
- Start → **拉取待登账** (ServiceTask)
- → **凭证录入** (UserTask：操作员填字段)
- → **借贷平衡校验** (ServiceTask：`BalanceValidateDelegate`)
- → End

**业务表**：`voucher` (`id, business_key, amount, account_no, trade_date, operator, status`)

**API 启动示例**：
```bash
curl -X POST "http://localhost:8080/api/custody/processes/posting/start?businessKey=POST-001" \
  -H "Content-Type: application/json" \
  -d '{"operator":"张三","amount":50000,"accountNo":"6225881234567","tradeDate":"2026-07-31"}'
```

### 3.2 清算 (Clearing)

**业务**：登账后按批次批量清算（多笔交易归一）。

**BPMN 节点**：
- Start → **拉取待清算** (ServiceTask：`FetchPendingDelegate`)
- → **清算确认** (UserTask)
- → **写入清算结果** (ServiceTask：`WriteClearingDelegate`)
- → End

**业务表**：`clearing_batch` (`id, business_key, batch_no, trade_count, total_amount, operator, status`)

**关键设计**：清算按 **batchNo** 组织（不是 processInstanceId）。`FetchPendingDelegate` 在 batchNo 缺失时**兜底用 businessKey 顶上**（修过 UNIQUE 冲突的脏数据场景）。

### 3.3 对账 (Reconciliation)

**业务**：核对两方数据（银行账 vs 托管账），差异处理。

**BPMN 节点**：
- Start → **拉取两方数据** (ServiceTask：`FetchBothDelegate`)
- → **自动比对** (ServiceTask：`AutoMatchDelegate`)
- → **差异 > 0?** (Exclusive Gateway)
  - ① 是 → **调账** (UserTask：人工处理) → 写入对账结果
  - ② 否 → **写入对账结果** (ServiceTask：`WriteReconciliationDelegate`)
- → End

**业务表**：`reconciliation_log` (`id, business_key, diff_count, diff_amount, status, note`)

**两种演示模式**（demo.html 第 3 个 Tab 切换）：
- **无差异 (auto)**：`diffCount=0`，走全自动分支，3 秒出结果
- **有差异 (diff)**：`diffCount=3`，走人工调账分支，需操作员完成调账 UserTask

**业务约束**（简历亮点）：**对账必须在估值前**——业务规则通过对账结果 `status='AUTO_PASSED'` 决定是否能启动估值。

### 3.4 估值 (Valuation)

**业务**：根据持仓计算基金净值（NAV = 总资产 / 份额）。

**BPMN 节点**：
- Start → **拉取持仓** (ServiceTask：`FetchHoldingsDelegate`)
- → **计算 NAV** (ServiceTask：`CalculateNavDelegate`)
- → **估值确认** (UserTask)
- → **写入估值表** (ServiceTask：`WriteValuationDelegate`)
- → End

**业务表**：`fund_valuation` (`id, business_key, fund_code, total_asset, share_count, nav, nav_date, operator, status`) + `holding` (持仓快照)

**预置演示数据**：`FUND001` 持仓 3 条（总市值 2,252,100 元）/ 5,000 万份额 → NAV = 0.0450

**关键设计**：`startValuation` 拆分 `businessKey`（带时间戳防 UNIQUE）和 `fundCode`（原值，**给后端查持仓用**）。如果 fundCode 也拼时间戳会超过字段长度（varchar(8)）。

---

## 4. 一键演示 demo.html

打开 `http://localhost:8080/demo.html`，4 个 Tab 顶部都有 **🎬 一键演示** 按钮。

**点击后自动跑**：
1. 启动当前 Tab 流程（startMap[tab]()）
2. 循环完成所有 UserTask（**最多 5 轮，防死循环**）
3. 实时显示 status 文本（步骤详情）
4. 刷新业务表（最新 5 条）

**4 Tab 顺序点**（约 40s 跑完）：
1. 登账 → 凭证录入（自动 UserTask）
2. 清算 → 清算确认
3. 对账 → 选 "无差异" 模式 → 自动跑通
4. 估值 → 估值确认

**业务表单字段**：
- 每个 Tab 都有业务字段（金额 / 批次号 / 差异数 等）+ `businessKey` 输入框
- 一键演示前**自动给 businessKey 加时间戳后缀**（防 UNIQUE 冲突）
- 修改表单后点 "▶ 启动 XXX 流程" 是**手动模式**（不走 demoRun）

**TAB_PREFIX 映射**（demo.html 内部）：
- 业务 tab 名（`posting`）→ DOM id 前缀（`post`）
- 防止 JS 用全名拼 id 找不到元素（命名不一致 bug 已修）

---

## 5. 完整 API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/custody/health` | GET | 服务健康（engine / database） |
| `/api/custody/process-definitions` | GET | 4 流程定义 |
| `/api/custody/processes/{key}/start` | POST | 启动流程（key=posting/clearing/reconciliation/valuation） |
| `/api/custody/processes/{key}/instances` | GET | 流程实例列表 |
| `/api/custody/tasks` | GET | 待办 UserTask（支持 `?assignee=张三`） |
| `/api/custody/tasks/{taskId}/complete` | POST | 完成任务（body: `{operator: "..."}`） |
| `/api/custody/daily-reconcile/trigger` | POST | 手动触发每日对账（Quartz 任务） |
| `/api/custody/daily-reconcile/stats` | GET | 今日对账统计 |
| `/api/custody/business/vouchers` | GET | 凭证表 |
| `/api/custody/business/clearing-batches` | GET | 清算批次表 |
| `/api/custody/business/reconciliations` | GET | 对账日志表 |
| `/api/custody/business/valuations` | GET | 估值表 |
| `/task-board.html` | GET | 旧版任务看板（保留） |
| `/demo.html` | GET | 4 Tab 综合演示页（**推荐**） |

**完整启动 valuation + 完成的 curl 序列**：
```bash
# 1. 启动
INST=$(curl -sX POST "http://localhost:8080/api/custody/processes/valuation/start?businessKey=VAL-001" \
  -H "Content-Type: application/json" \
  -d '{"operator":"赵六","fundCode":"FUND001","navDate":"2026-07-31"}' | jq -r .processInstanceId)
echo "Instance: $INST"

# 2. 查任务
TASK=$(curl -s "http://localhost:8080/api/custody/tasks?assignee=赵六" | jq -r '.[] | select(.processDefinitionKey=="valuation") | .taskId' | head -1)
echo "Task: $TASK"

# 3. 完成任务
curl -X POST "http://localhost:8080/api/custody/tasks/$TASK/complete" \
  -H "Content-Type: application/json" \
  -d '{"operator":"赵六"}'

# 4. 查估值结果
curl -s http://localhost:8080/api/custody/business/valuations | jq
```

---

## 6. 数据库

**5 业务表**（应用层）：
- `voucher`（凭证）
- `clearing_batch`（清算批次）
- `reconciliation_log`（对账日志）
- `holding`（持仓快照）
- `fund_valuation`（基金估值）

**23 张 ACT_* 表**（Flowable 启动自动管理，**不要手动改**）：
- `act_ru_*`（运行时：task / execution / variable / identitylink）
- `act_hi_*`（历史）
- `act_re_*`（仓库：deployment / procdef）

**Schema 初始化**：`src/main/resources/schema.sql`（首次启动自动建表，**幂等检查**靠 `IF NOT EXISTS` + UNIQUE 约束组合）

**JDBC 连接**：
```
jdbc:mysql://localhost:3306/bank_custody_demo?useUnicode=true&characterEncoding=UTF-8&connectionCollation=utf8mb4_unicode_ci
```
- 用户：`custody / custody123`
- **踩坑**：`characterEncoding=UTF-8`（**不是 `utf8mb4`**——utf8mb4 是 MySQL 字符集不是 Java charset）

---

## 7. BPMN 关键设计

### 7.1 ServiceTask vs UserTask

- **ServiceTask**（4 个 Tab 都有）：自动执行的 Service 节点，**无 UI**
  - 用 `flowable:delegateExpression="${beanName}"` 调用 Spring Bean
  - 不用 `flowable:class`（避免 no-arg 构造问题 + @Autowired 失效）
- **UserTask**（4 个 Tab 都有）：需要人工参与的待办节点，**有 assignee**

### 7.2 Exclusive Gateway (对账)

对账流程的 **差异判断** 用 Exclusive Gateway：
- 条件 `${diffCount == 0 || diffCount == null}` → 走 "否" 分支（自动写对账结果）
- 条件 `${diffCount > 0}` → 走 "是" 分支（人工调账）

### 7.3 delegate 加载机制

```xml
<serviceTask id="fetch-pending" name="拉取待清算"
  flowable:delegateExpression="${fetchPendingDelegate}" />
```

`delegateExpression` 直接从 Spring 容器拿 Bean（保证单例 + `@Autowired` 注入）。
**vs `flowable:class`**：class 触发 new 实例（绕过 Spring 容器，DI 失效）。

### 7.4 业务表 UNIQUE 约束

5 业务表的 `business_key` 都设了 UNIQUE（CREATE TABLE 时定义）——保证同 businessKey 只能有一个流程实例。
- **应用层**：启动前检查 + smoke-test 用时间戳避免冲突
- **demo.html 一键演示**：自动加时间戳后缀（**不污染用户输入框**）

---

## 8. 测试

### 8.1 快速冒烟（5-10 秒）

```bash
./smoke-test.sh
# ✅ PASS: 20  (健康/4 流程启动/业务查询/任务/Quartz)
# ❌ FAIL: 0
```

### 8.2 完整端到端（~50 秒，含 Playwright 真 browser 自动化）

```bash
WITH_BROWSER=true ./smoke-test.sh
# ✅ PASS: 21  (20 API + 1 Playwright 4 Tab 浏览器真点)
```

**Playwright 段要求**：
- 已装 Node + `npm i -g playwright`
- Playwright 默认装在 `/opt/homebrew/lib/node_modules/playwright`
- `smoke-test.sh` 自动设 `NODE_PATH=/opt/homebrew/lib/node_modules`

### 8.3 单独跑 Playwright

```bash
node e2e/click-demo.cjs
# → 4 Tab 切 + 抓 page error + 截图
# 输出: /tmp/tab-{posting,clearing,reconciliation,valuation}.png
```

`click-demo.cjs` 启动前**自动清理** `act_ru_task` 历史（服务重启后 MySQL 持久化，旧 task 必撞 UNIQUE）。

---

## 9. 故障排查

| 现象 | 根因 | 修法 |
|------|------|------|
| 启动时 `Unsupported character encoding 'utf8mb4'` | JDBC URL 用了 `characterEncoding=utf8mb4`（Java 不认） | 改 `characterEncoding=UTF-8` + `connectionCollation=utf8mb4_unicode_ci` |
| `CREATE INDEX IF NOT EXISTS ...` SQL 报错 | MySQL 8/9 不支持 `CREATE INDEX IF NOT EXISTS` | schema.sql 改用 UNIQUE 约束（CREATE TABLE 时定义） |
| 业务表 INSERT UNIQUE 冲突 | 同 businessKey 重复 | smoke-test 用时间戳；demo.html 一键演示自动加戳 |
| demo.html 点一键演示后 alert 弹窗阻塞 | `completeTask` 错误处理用 `alert()` | 已修：`catch + skip`，不阻塞 |
| 清算启动 500（INSERT 漏字段） | `batchNo` 没传 | `FetchPendingDelegate` 加 businessKey 兜底 |
| valuation fund_code 字段 "Data too long" | businessKey 带时间戳后当 fundCode | `startValuation` 拆分 businessKey 和 fundCode |
| click-demo 跑出 500 | 服务重启后 act_ru_task 有历史 task | `click-demo.cjs` 启动前 `mysql DELETE FROM act_ru_task` |
| 找不到 `playwright`（smoke-test WITH_BROWSER 段 SKIP） | Playwright 装在全局但不在 `NODE_PATH` | smoke-test 自动 export `NODE_PATH=/opt/homebrew/lib/node_modules` |
| `flowable:class` 实例化失败 | BPMN 用 `flowable:class` 但 delegate 无 no-arg 构造 | 改 `flowable:delegateExpression="${beanName}"` |

---

## 10. 进阶改造指南

### 10.1 加新流程（5 步）

1. `src/main/resources/processes/myflow.bpmn20.xml`（BPMN 2.0）
2. `entity/MyEntity.java` + `mapper/MyMapper.java`（MyBatis-Plus）
3. `service/MyService.java`（业务逻辑）
4. `delegate/MyDelegate.java`（ServiceTask 实现，`@Component`）
5. BPMN 里 `flowable:delegateExpression="${myDelegate}"` 引用
6. `controller/WorkflowController.java` 加 `@PostMapping("/processes/myflow/start")`
7. `schema.sql` 加 `CREATE TABLE my_entity ...`
8. 跑 `./start-demo.sh` + `./smoke-test.sh` 验证

### 10.2 改字段

1. 改 entity + mapper XML（MyBatis-Plus）
2. 改 `schema.sql` + DROP TABLE 旧表（**开发期**）
3. 改 BPMN 流程变量名（如果改了）
4. 改 `demo.html` 表单 input + `loadXxx()` 渲染的列
5. 改 `smoke-test.sh` 端点的请求体 JSON

### 10.3 接 RabbitMQ DLQ

（参考独立项目 `dlq-demo`）
- Spring AMQP + RabbitMQ DLX（死信交换机）
- 三档 TTL 模拟延迟队列
- Redis 幂等表防重复消费
- Prometheus 指标监控

---

## 11. 架构图

- [docs/01-architecture.png](./docs/01-architecture.png) - 系统架构总览
- [docs/02-data-model.png](./docs/02-data-model.png) - 5 业务表 ER 图
- [docs/03-bpmn-flow.png](./docs/03-bpmn-flow.png) - 4 流程时序图
- [docs/04-task-board.png](./docs/04-task-board.png) - 任务看板 UI（旧版）

---

## 12. 配套公开仓

- [SwordBob/pg-user-profile-demo](https://github.com/SwordBob/pg-user-profile-demo) - 简历项目 1: PG + JSONB 信创
- [SwordBob/mongodb-user-profile-demo](https://github.com/SwordBob/mongodb-user-profile-demo) - 简历项目 1: MongoDB 双子星
- [SwordBob/bank-custody-workflow-demo](https://github.com/SwordBob/bank-custody-workflow-demo) - 本仓

---

**最后更新**：2026-07-31 (v4.4) · smoke-test 20 PASS / WITH_BROWSER 21 PASS
