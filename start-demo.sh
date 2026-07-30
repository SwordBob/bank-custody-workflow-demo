#!/usr/bin/env bash
#
# 银行资产托管 Demo - 一键启动脚本
# ----------------------------------------------------------------
# 用法:
#   ./start-demo.sh        # 后台启动, 完事 ./stop-demo.sh
#
# 启动顺序:
#   1. 启动 MySQL 9.7.0 (datadir=$HOME/mysql-data)
#   2. 启动 Spring Boot 应用 (端口 8080)
#   3. 等 health check 通过
#   4. 自动跑 smoke-test.sh
#
# 停止: ./stop-demo.sh
# ----------------------------------------------------------------

set -e

PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
MYSQL_HOME="/usr/local/mysql"
MYSQL_DATA="$HOME/mysql-data"
MYSQL_PID="$MYSQL_DATA/mysql.pid"
APP_PORT=8080
APP_LOG="$PROJECT_DIR/app.log"

# 颜色
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD} 银行资产托管 Demo · 一键启动${NC}"
echo -e "${BOLD}========================================${NC}"

# ===== 1. 检查 Java + Maven =====
if ! command -v java >/dev/null 2>&1; then
  echo -e "${YELLOW}⚠️  Java 未安装, 尝试用 /tmp 下的 JDK 17${NC}"
  if [ ! -d "/tmp/zulu17.50.19-ca-jdk17.0.11-macosx_aarch64" ]; then
    echo "❌ 找不到 JDK 17, 请安装 Java 17+: brew install openjdk@17"
    exit 1
  fi
  export JAVA_HOME="/tmp/zulu17.50.19-ca-jdk17.0.11-macosx_aarch64/zulu-17.jdk/Contents/Home"
  export PATH="$JAVA_HOME/bin:$PATH"
fi
echo -e "${GREEN}✅ Java: $(java -version 2>&1 | head -1)${NC}"

if ! command -v mvn >/dev/null 2>&1; then
  echo -e "${YELLOW}⚠️  Maven 未安装, 尝试用 /tmp 下的 Maven${NC}"
  if [ ! -d "/tmp/apache-maven-3.9.16" ]; then
    echo "❌ 找不到 Maven, 请安装: brew install maven"
    exit 1
  fi
  export PATH="/tmp/apache-maven-3.9.16/bin:$PATH"
fi
echo -e "${GREEN}✅ Maven: $(mvn -version | head -1)${NC}"

# ===== 2. 启动 MySQL =====
echo ""
echo -e "${YELLOW}→ 启动 MySQL 9.7.0...${NC}"
if [ -f "$MYSQL_PID" ] && kill -0 "$(cat "$MYSQL_PID")" 2>/dev/null; then
  echo -e "${GREEN}✅ MySQL 已在运行 (PID $(cat "$MYSQL_PID"))${NC}"
else
  if [ ! -d "$MYSQL_DATA" ]; then
    echo "初始化 MySQL 数据目录..."
    mkdir -p "$MYSQL_DATA"
    "$MYSQL_HOME/bin/mysqld" --initialize-insecure --user=niejq \
      --datadir="$MYSQL_DATA" --basedir="$MYSQL_HOME" 2>&1 | tail -3
  fi
  "$MYSQL_HOME/bin/mysqld" \
    --user=niejq \
    --datadir="$MYSQL_DATA" \
    --basedir="$MYSQL_HOME" \
    --port=3306 \
    --socket=/tmp/mysql.sock \
    --pid-file="$MYSQL_PID" 2>&1 &
  sleep 5
  if kill -0 "$(cat "$MYSQL_PID" 2>/dev/null)" 2>/dev/null; then
    echo -e "${GREEN}✅ MySQL 启动 (PID $(cat "$MYSQL_PID"))${NC}"
  else
    echo "❌ MySQL 启动失败, 查看日志"
    exit 1
  fi
fi

# 创建数据库 + 用户 (如果不存在)
"$MYSQL_HOME/bin/mysql" -uroot --socket=/tmp/mysql.sock <<EOF 2>/dev/null
CREATE DATABASE IF NOT EXISTS bank_custody_demo DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'custody'@'localhost' IDENTIFIED BY 'custody123';
GRANT ALL PRIVILEGES ON bank_custody_demo.* TO 'custody'@'localhost';
FLUSH PRIVILEGES;
EOF
echo -e "${GREEN}✅ 数据库 bank_custody_demo + 用户 custody 就绪${NC}"

# ===== 3. 启动 Spring Boot =====
echo ""
echo -e "${YELLOW}→ 启动 Spring Boot 应用 (端口 $APP_PORT)...${NC}"
cd "$PROJECT_DIR"

# 如果已经在跑, 先杀掉
if lsof -nP -iTCP:$APP_PORT -sTCP:LISTEN >/dev/null 2>&1; then
  echo "  端口 $APP_PORT 被占用, 先杀掉..."
  kill -9 $(lsof -nP -iTCP:$APP_PORT -sTCP:LISTEN -t) 2>/dev/null || true
  sleep 2
fi

nohup mvn org.springframework.boot:spring-boot-maven-plugin:run > "$APP_LOG" 2>&1 &
APP_PID=$!
echo "  启动中 (PID $APP_PID) ..."

# ===== 4. 等待 health =====
for i in $(seq 1 60); do
  if curl -sf --max-time 2 "http://localhost:$APP_PORT/api/custody/health" >/dev/null 2>&1; then
    echo -e "${GREEN}✅ 应用就绪 (${i}s)${NC}"
    break
  fi
  if [ "$i" = "60" ]; then
    echo "❌ 60s 内未就绪, 查看 app.log 最后 30 行"
    tail -30 "$APP_LOG"
    exit 1
  fi
  sleep 1
done

# ===== 5. 跑 smoke-test =====
echo ""
echo -e "${YELLOW}→ 跑 smoke-test (20 端点验证)...${NC}"
./smoke-test.sh 2>&1 | tail -10

# ===== 总结 =====
echo ""
echo -e "${BOLD}========================================${NC}"
echo -e "${BOLD} 🎉 Demo 启动成功!${NC}"
echo -e "${BOLD}========================================${NC}"
echo ""
echo -e "  ${CYAN}📊 API 端点${NC}:  http://localhost:$APP_PORT/api/custody/health"
echo -e "  ${CYAN}🌐 HTML 任务看板${NC}:  http://localhost:$APP_PORT/task-board.html"
echo -e "  ${CYAN}📋 流程定义${NC}:    curl http://localhost:$APP_PORT/api/custody/process-definitions"
echo -e "  ${CYAN}📂 业务数据${NC}:    curl http://localhost:$APP_PORT/api/custody/business/reconciliations"
echo ""
echo -e "  ${YELLOW}应用日志${NC}:  tail -f $APP_LOG"
echo -e "  ${YELLOW}停止服务${NC}:  ./stop-demo.sh"
echo -e "  ${YELLOW}重跑 smoke-test${NC}:  ./smoke-test.sh"
echo ""
