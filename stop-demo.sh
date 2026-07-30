#!/usr/bin/env bash
#
# 银行资产托管 Demo - 一键停止脚本
# ----------------------------------------------------------------
# 停止顺序:
#   1. 杀掉 Spring Boot (端口 8080)
#   2. 杀掉 MySQL 9.7.0 (datadir=$HOME/mysql-data)
# ----------------------------------------------------------------

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD} 银行资产托管 Demo · 一键停止${NC}"
echo -e "${BOLD}========================================${NC}"

# 1. 杀 Spring Boot
echo -e "${YELLOW}→ 停止 Spring Boot...${NC}"
APP_PIDS=$(lsof -nP -iTCP:8080 -sTCP:LISTEN -t 2>/dev/null)
if [ -n "$APP_PIDS" ]; then
  echo "  杀掉 PID: $APP_PIDS"
  kill -9 $APP_PIDS 2>/dev/null || true
  # 也杀掉 mvn/java 进程
  pkill -9 -f "spring-boot:run" 2>/dev/null || true
  pkill -9 -f "BankCustodyApplication" 2>/dev/null || true
  sleep 2
  echo -e "${GREEN}✅ Spring Boot 已停止${NC}"
else
  echo -e "${GREEN}✅ Spring Boot 未运行${NC}"
fi

# 2. 杀 MySQL
echo -e "${YELLOW}→ 停止 MySQL...${NC}"
MYSQL_PID="$HOME/mysql-data/mysql.pid"
if [ -f "$MYSQL_PID" ] && kill -0 "$(cat "$MYSQL_PID")" 2>/dev/null; then
  MYSQL_PID_NUM=$(cat "$MYSQL_PID")
  echo "  杀掉 PID: $MYSQL_PID_NUM"
  kill "$MYSQL_PID_NUM" 2>/dev/null || true
  sleep 3
  if kill -0 "$MYSQL_PID_NUM" 2>/dev/null; then
    kill -9 "$MYSQL_PID_NUM" 2>/dev/null || true
  fi
  echo -e "${GREEN}✅ MySQL 已停止${NC}"
else
  echo -e "${GREEN}✅ MySQL 未运行${NC}"
fi

echo ""
echo -e "${CYAN}所有服务已停止. 下次启动: ./start-demo.sh${NC}"
