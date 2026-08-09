#!/usr/bin/env bash
#
# rebuild-flux-chain.sh — 统一前端构建链（单入口）
#
# 从 flux 打包 → nop-chaos-next 打包 → nop-entropy nop-web-site 打包 → ERP runner jar 重建，
# 一条命令完成前端 bundle 更新的全链路发布。诊断清楚前端问题后，直接执行本脚本让修改生效。
#
# 链路（对应 docs/testing/e2e-runbook.md「flux 调试三路径」第 3 条）：
#   1. nop-chaos-flux  : pnpm --filter @nop-chaos/flux build + pack:release（经 nop-chaos-next
#                        的 scripts/repack-flux-and-refresh.sh 编排：打包→拷贝 tgz→同步 ui 源码→刷新依赖）
#   2. nop-chaos-next  : pnpm build（apps/main dist）
#   3. 同步            : scripts/sync-site.sh → nop-entropy/nop-web-site/src/main/resources
#   4. nop-entropy     : mvn clean install -pl nop-frontend-support/nop-web-site -DskipTests
#   5. nop-app-erp     : mvn clean install -DskipTests（重建 runner jar）
#
# 用法：
#   bash scripts/rebuild-flux-chain.sh                       # 全链路
#   bash scripts/rebuild-flux-chain.sh --skip-flux           # 不重打包 flux，仍重建 nop-chaos-next
#   bash scripts/rebuild-flux-chain.sh --skip-next           # 跳过 nop-chaos-next 构建
#   bash scripts/rebuild-flux-chain.sh --skip-site           # 跳过 nop-entropy nop-web-site install
#   bash scripts/rebuild-flux-chain.sh --skip-erp            # 不重建 ERP runner jar
#   bash scripts/rebuild-flux-chain.sh --erp-only            # 仅重建 ERP runner jar
#
# 注意：
#   - 步骤 3 的 sync-site.sh 会清空并整体重涂 nop-web-site 资源目录（含 extension/plugins/locales）。
#   - 步骤 4 用 clean 安装，避免 target/classes 残留旧资源混入 jar（2026-08-03 实测教训）。
#   - maven 依赖 nop-web-site 的新 bundle 后必须重建 ERP runner jar 才生效（步骤 5）。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ERP_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
NEXT_ROOT="$(cd "$ERP_ROOT/../nop-chaos-next" && pwd)"
ENTROPY_ROOT="$(cd "$ERP_ROOT/../nop-entropy" && pwd)"

DO_FLUX=true
DO_NEXT=true
DO_SITE=true
DO_ERP=true

for arg in "$@"; do
  case "$arg" in
    --skip-flux) DO_FLUX=false ;;
    --skip-next) DO_NEXT=false ;;
    --skip-site) DO_SITE=false ;;
    --skip-erp)  DO_ERP=false ;;
    --erp-only)  DO_FLUX=false; DO_NEXT=false; DO_SITE=false; DO_ERP=true ;;
    *) echo "未知参数: $arg"; exit 1 ;;
  esac
done

[[ -d "$NEXT_ROOT" ]]    || { echo "[chain] nop-chaos-next 不存在: $NEXT_ROOT"; exit 1; }
[[ -d "$ENTROPY_ROOT" ]] || { echo "[chain] nop-entropy 不存在: $ENTROPY_ROOT"; exit 1; }

info() { echo "[chain] $*"; }
run() { info ">> $*"; (cd "$1" && shift 1 && "$@"); }

if [ "$DO_FLUX" = true ]; then
  info "步骤 2/5：flux 打包 + 刷新（经 repack-flux-and-refresh.sh）"
  (cd "$NEXT_ROOT" && bash scripts/repack-flux-and-refresh.sh)
fi

if [ "$DO_NEXT" = true ]; then
  info "步骤 3/5：nop-chaos-next 构建（--force 绕过 turbo 缓存）"
  # 必须 --force：pnpm build 是 turbo run build，turbo 的缓存 key 不感知
  # libs/ 下 flux tgz 内容变化（依赖声明/lockfile 未变），会命中旧缓存跳过
  # 构建，导致 apps/main/dist 仍是旧 flux bundle（2026-08-09 实测：改完
  # flux 后 dist 停留在旧产物，前端 closeOnSubmit 等新逻辑不生效）。
  (cd "$NEXT_ROOT" && pnpm build --force)
fi

if [ "$DO_SITE" = true ]; then
  info "步骤 4/5：同步 dist → nop-entropy nop-web-site 资源"
  (cd "$NEXT_ROOT" && bash scripts/sync-site.sh)
  info "步骤 4/5：clean install nop-web-site"
  (cd "$ENTROPY_ROOT" && mvn clean install -pl nop-frontend-support/nop-web-site -DskipTests -q)
fi

if [ "$DO_ERP" = true ]; then
  info "步骤 5/5：ORM 强制 flux（codegen 持久化）→ 重建 ERP runner jar"
  (cd "$ERP_ROOT" && bash scripts/flip-orm-to-flux.sh)
  (cd "$ERP_ROOT" && mvn clean install -DskipTests -q)
  info "步骤 5/5：菜单兜底翻转（手写 action-auth.xml）"
  (cd "$ERP_ROOT" && bash scripts/flip-menu-to-flux.sh)
fi

info "完成，链路更新已生效。"