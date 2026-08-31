#!/usr/bin/env bash
# Demo/sandbox server lifecycle helper for app-erp-all (演示/沙盒语义).
#
# IMPORTANT:
#   - 本脚本是"演示/沙盒"启动入口，仅用于本地演示与数据可见性验证。
#   - 生产真实部署不应使用此脚本——生产应改 MySQL/PostgreSQL + 业务动作闸门正确配置
#     + 数据集隔离（多租户/账套级）。
#   - fresh-DB 强制：DataInitInitializer 非幂等（无存在性检查），seed 表已有行时
#     重复启动会主键冲突；故每次启动前删除 db/erp 文件（fresh-DB 重置）。
#   - JVM args 仅保留演示必需；业务动作闸门类配置（erp-fin.budget-check-enabled 等）
#     演示默认不开启，按需另行追加 -D。
#   - 镜像 _tmp-server.sh（SKIP_WEBSERVER=1 测试专用）的 stop/start/wait 范式，
#     但去除 quarkus.profile=test 与测试专用 JVM flags。
#
# Usage:
#   ./scripts/start-app.sh start    # kill existing, reset H2 db (fresh seed), boot on 8011, wait ready
#   ./scripts/start-app.sh stop     # kill server on 8011
#   ./scripts/start-app.sh restart  # stop + start (fresh DB)
set -e

cd "$(dirname "$0")/.."
PORT=8011
JAR="app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar"
LOG="_tmp/app.log"
mkdir -p _tmp

stop_server() {
  local pid
  pid=$(lsof -ti:${PORT} 2>/dev/null || true)
  if [ -n "$pid" ]; then
    echo "stopping server pid=$pid on ${PORT}"
    kill "$pid" 2>/dev/null || true
    for i in $(seq 1 20); do
      sleep 1
      pid=$(lsof -ti:${PORT} 2>/dev/null || true)
      [ -z "$pid" ] && break
      kill -9 "$pid" 2>/dev/null || true
    done
  fi
  # also kill any java runner.jar processes left over
  pkill -f "app-erp-all-1.0-SNAPSHOT-runner.jar" 2>/dev/null || true
  sleep 1
}

start_server() {
  stop_server
  echo "resetting H2 db (fresh seed)"
  rm -f db/erp.mv.db db/erp.trace.db
  echo "booting server on ${PORT}"
  nohup java -Dfile.encoding=UTF8 \
    -Dnop.auth.service-public=true \
    -Dnop.auth.login.allow-create-default-user=true \
    -Dnop.web.render-mode=flux \
    -jar "${JAR}" > "${LOG}" 2>&1 &
  echo "waiting for server on ${PORT}"
  for i in $(seq 1 90); do
    if curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${PORT}/" 2>/dev/null | grep -q '200\|302\|401\|403'; then
      echo "server ready after ${i}s"
      return 0
    fi
    sleep 1
  done
  echo "ERROR: server did not become ready in 90s. Tail of log:"
  tail -30 "${LOG}"
  return 1
}

case "${1:-}" in
  start) start_server ;;
  stop) stop_server ;;
  restart) start_server ;;
  *) echo "usage: $0 {start|stop|restart}"; exit 1 ;;
esac