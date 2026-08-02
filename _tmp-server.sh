#!/usr/bin/env bash
# Manual server lifecycle helper for SKIP_WEBSERVER=1 Playwright runs (the canonical
# E2E workflow: playwright.config.ts webServer polls 8080 but application.yaml binds
# 8011, so the webServer never successfully starts; run this script then
# `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test`).
# JVM args MUST stay in sync with playwright.config.ts webServer.command (single
# source of truth = playwright.config.ts; this script mirrors it for manual runs).
# Usage:
#   ./_tmp-server.sh start   # kill existing, reset H2 db (fresh seed), boot on 8011, wait ready
#   ./_tmp-server.sh stop    # kill server on 8011
#   ./_tmp-server.sh restart # stop + start (fresh DB)
set -e

cd "$(dirname "$0")"
PORT=8011
JAR="app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar"
LOG="_tmp/e2e-server.log"
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
    -Dnop.orm.init-database-data=true \
    -Dnop.web.validate-page-model=false \
    -Derp-qua.ncr-default-acct-schema=1 \
    -Derp-mfg.variance-auto-calc-enabled=true \
    -Derp-mfg.inspection-gate-enabled=true \
    -Derp-mfg.subcontract-posting-enabled=true \
    -Derp-mfg.subcontract-release-enabled=true \
    -Derp-fin.bad-debt-allowance-subject-code=1231 \
    -Derp-fin.bad-debt-expense-subject-code=6701 \
    -Derp-fin.ar-subject-code=1122 \
    -Derp-fin.ap-subject-code=2202 \
    -Derp-fin.exchange-gain-loss-subject-code=6603 \
    -Derp-fin.current-year-profit-subject-code=4103 \
    -Derp-fin.auto-depreciation-on-close=false \
    -Derp-fin.bad-debt-allowance-gate-enabled=false \
    -Derp-fin.reverse-close-approval-required=false \
    -Derp-fin.auto-reconcile=true \
    -Derp-fin.budget-check-enabled=true \
    -Derp-fin.budget-purchase-expense-subject-code=6601 \
    -Derp-fin.expense-budget-check-enabled=true \
    -Derp-fin.budget-expense-subject-code=6602 \
    -Derp-fin.gl-mapping.org-dimension-enabled=true \
    -Derp-fin.budget-commitment-enabled=true \
    -Derp-fin.budget-commitment-subject-code=2202 \
    -Derp-fin.budget-commitment-sales-subject-code=5001 \
    -Derp-fin.budget-roll-forward-enabled=true \
    -Derp-fin.budget-carry-forward-enabled=true \
    -Derp-fin.intercompany-posting-enabled=true \
    -Derp-fin.consolidation-elimination-enabled=true \
    -Derp-fin.period-end-exchange-rate=8.5 \
    -Derp-fin.credit-facility-default-interest-rate=0.05 \
    -Derp-fin.notes-fx-gain-loss-enabled=true \
    -Derp-mnt.spare-part-posting-enabled=true \
    -Derp-mnt.labor-posting-enabled=true \
    -Derp-mnt.default-labor-hourly-rate=80 \
    -Derp-prj.default-payroll-subject-id=2211 \
    -Derp-b2b.asn-auto-create-receive=true \
    -Derp-log.webhook-signature-required=false \
    -Derp-log.path2-landed-cost-auto-create=true \
    -Derp-mfg.simulation-enabled=true \
    -Derp-drp.simulation-enabled=true \
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
