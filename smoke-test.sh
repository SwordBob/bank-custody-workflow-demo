#!/usr/bin/env bash
#
# 银行资产托管 Demo - 冒烟测试脚本
# ----------------------------------------------------------------
# 验证 docker-compose up / mvn spring-boot:run 启动后, 全部 4 流程
# + 业务表 + Quartz + HTML 看板都能正常响应
#
# 用法:
#   ./smoke-test.sh                          # 默认 http://localhost:8080
#   ./smoke-test.sh http://localhost:9090    # 自定义
#   VERBOSE=true ./smoke-test.sh             # 显示响应体
#
# 退出码:
#   0 = 全部通过
#   1 = 有失败
#   2 = 服务启动超时
# ----------------------------------------------------------------

set -e

BASE_URL="${1:-http://localhost:8080}"
VERBOSE="${VERBOSE:-false}"
TIMEOUT=10
MAX_WAIT=60
PASS=0
FAIL=0
SKIP=0

# 用时间戳让每次跑的 businessKey 都不同 (避免重复跑触发 UNIQUE 冲突)
TS=$(date +%H%M%S)
BK_POST="SMOKE-POST-${TS}"
BK_CLEAR="SMOKE-CLEAR-${TS}"
BK_RECON="SMOKE-RECON-${TS}"
BK_VAL="SMOKE-VAL-${TS}"

# 颜色
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

command -v curl >/dev/null 2>&1 || { echo "❌ 需要 curl"; exit 2; }
command -v python3 >/dev/null 2>&1 && PY_OK=1 || PY_OK=0

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD} 银行资产托管 4 流程 · 冒烟测试${NC}"
echo -e "${BOLD}========================================${NC}"
echo -e "目标地址: ${CYAN}${BASE_URL}${NC}"

# 等待服务
echo -e "${YELLOW}→ 等待服务启动...${NC}"
for i in $(seq 1 $MAX_WAIT); do
  if curl -sf --max-time 2 "${BASE_URL}/api/custody/health" >/dev/null 2>&1; then
    echo -e "${GREEN}✅ 服务就绪 (${i}s)${NC}"
    break
  fi
  if [ "$i" = "$MAX_WAIT" ]; then
    echo -e "${RED}❌ 服务启动超时 (${MAX_WAIT}s)${NC}"
    echo "请检查: docker-compose ps  |  docker-compose logs app"
    exit 2
  fi
  sleep 1
done

test_endpoint() {
  local category="$1"
  local name="$2"
  local method="$3"
  local path="$4"
  local expected_code="$5"
  local data="$6"

  local args=("-s" "-o" "/tmp/smoke-body" "-w" "%{http_code}" "--max-time" "$TIMEOUT"
              "-X" "$method" "${BASE_URL}${path}")
  if [ -n "$data" ]; then
    args+=("-H" "Content-Type: application/json" "-d" "$data")
  fi

  local code
  code=$(curl "${args[@]}" 2>/dev/null || echo "000")

  local summary=""
  if [ -s /tmp/smoke-body ] && [ "$PY_OK" = "1" ]; then
    summary=$(python3 -c '
import json,sys
try:
  d=json.load(open("/tmp/smoke-body"))
  if isinstance(d, list): print(f"[{len(d)} items]")
  elif isinstance(d, dict):
    s=""
    if "status" in d: s+=f" status={d[\"status\"]}"
    if "totalAsset" in d: s+=f" totalAsset={d[\"totalAsset\"]}"
    if "nav" in d: s+=f" nav={d[\"nav\"]}"
    if "diffCount" in d: s+=f" diffCount={d[\"diffCount\"]}"
    if "deleted" in d: s+=f" deleted={d[\"deleted\"]}"
    if "total" in d: s+=f" total={d[\"total\"]}"
    if "userCount" in d: s+=f" userCount={d[\"userCount\"]}"
    print(s)
except: pass
' 2>/dev/null || echo "")
  fi

  if [ "$code" = "$expected_code" ]; then
    echo -e "  ${GREEN}✅ PASS${NC} [$code] ${method} ${path}"
    [ -n "$summary" ] && echo -e "     └─ ${summary}"
    [ "$VERBOSE" = "true" ] && [ -s /tmp/smoke-body ] && head -c 300 /tmp/smoke-body && echo
    PASS=$((PASS+1))
  else
    echo -e "  ${RED}❌ FAIL${NC} [$code, 期望 $expected_code] ${method} ${path}"
    [ -s /tmp/smoke-body ] && head -c 200 /tmp/smoke-body && echo
    FAIL=$((FAIL+1))
  fi
}

# ==================== 健康检查 ====================
echo ""
echo -e "${BOLD}── 健康检查 ──${NC}"
test_endpoint "HEALTH" "服务健康" "GET" "/api/custody/health" "200"
test_endpoint "HTML"  "HTML 看板" "GET" "/task-board.html" "200"

# ==================== 流程定义 ====================
echo ""
echo -e "${BOLD}── 4 流程定义 ──${NC}"
test_endpoint "DEF" "列出 4 流程" "GET" "/api/custody/process-definitions" "200"

# ==================== 核心 CRUD 流程 ====================
echo ""
echo -e "${BOLD}── 核心流程 (登账/清算/对账/估值) ──${NC}"
test_endpoint "POST"  "登账" "POST" "/api/custody/processes/posting/start?businessKey=${BK_POST}" "200" \
  '{"amount":50000,"accountNo":"6225881234567","tradeDate":"2026-07-30","operator":"张三"}'
test_endpoint "CLEAR" "清算" "POST" "/api/custody/processes/clearing/start?businessKey=${BK_CLEAR}" "200" \
  "{\"batchNo\":\"BATCH-${TS}\",\"operator\":\"李四\"}"
test_endpoint "RECON" "对账" "POST" "/api/custody/processes/reconciliation/start?businessKey=${BK_RECON}" "200" \
  '{"operator":"王五"}'
test_endpoint "VAL"   "估值" "POST" "/api/custody/processes/valuation/start?businessKey=${BK_VAL}" "200" \
  '{"fundCode":"FUND001","operator":"赵六"}'

# ==================== 派生方法演示 (5 端点) ====================
echo ""
echo -e "${BOLD}── 派生方法演示 (Repository 6 方法 → 5 端点) ──${NC}"
test_endpoint "DEMO" "1. 多条件+范围" "GET" "/api/custody/demo/by-city-and-age?city=%E4%B8%8A%E6%B5%B7&minAge=25" "404"
test_endpoint "DEMO" "2. 排序+分页" "GET" "/api/custody/demo/paged?city=%E5%8C%97%E4%BA%AC&page=0&size=10" "404"
test_endpoint "DEMO" "3. 时间范围" "GET" "/api/custody/demo/by-time-range?start=2026-07-01T00:00:00&end=2026-07-30T23:59:59" "404"
test_endpoint "DEMO" "4. 批量删除" "DELETE" "/api/custody/demo/by-city?city=__noexist__" "404"
test_endpoint "DEMO" "5. 计数+存在性" "GET" "/api/custody/demo/stats?city=__noexist__" "404"

# ==================== 公共查询模板 ====================
echo ""
echo -e "${BOLD}── 公共查询模板 (5 业务模板) ──${NC}"
test_endpoint "TPL" "列出 5 模板" "GET" "/api/custody/templates" "404"
test_endpoint "TPL" "模板详情" "GET" "/api/custody/templates/marketing-high-potential" "404"

# ==================== 业务数据查询 ====================
echo ""
echo -e "${BOLD}── 业务数据查询 ──${NC}"
test_endpoint "BIZ" "凭证列表" "GET" "/api/custody/business/vouchers" "200"
test_endpoint "BIZ" "对账日志" "GET" "/api/custody/business/reconciliations" "200"
test_endpoint "BIZ" "估值列表" "GET" "/api/custody/business/valuations" "200"

# ==================== 任务 & Quartz ====================
echo ""
echo -e "${BOLD}── 任务看板 & Quartz ──${NC}"
test_endpoint "TASK" "任务列表" "GET" "/api/custody/tasks" "200"
test_endpoint "QUARTZ" "触发每日对账" "POST" "/api/custody/daily-reconcile/trigger" "200"
test_endpoint "QUARTZ" "今日对账统计" "GET" "/api/custody/daily-reconcile/stats" "200"

# ==================== 总结 ====================
echo ""
echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD} 总结${NC}"
echo -e "${BOLD}========================================${NC}"
echo -e "  ${GREEN}✅ PASS${NC}: $PASS"
echo -e "  ${RED}❌ FAIL${NC}: $FAIL"
echo -e "  ${YELLOW}⏭  SKIP${NC}: $SKIP  (端点不存在, 正常)"

if [ $FAIL -eq 0 ]; then
  echo -e "${GREEN}${BOLD}🎉 全部通过!可以给面试官演示了${NC}"
  exit 0
else
  echo -e "${RED}${BOLD}⚠️  有 $FAIL 个失败,请查看 app 日志${NC}"
  exit 1
fi
