#!/usr/bin/env bash
#
# flip-menu-to-flux.sh
#
# 强制启用 flux 渲染模式（幂等）：
#   1. 所有 module-*/erp-*-web 的 action-auth.xml（生成 _erp-xx + 手写 erp-xx）
#      component="AMIS" → component="FLUX"（单向，AMIS 不再考虑）
#   2. app-erp-all 根级菜单 app.action-auth.xml 同规则翻转
#   3. app-erp-all application.yaml 强制 nop.web.render-mode: flux
#      （PageProvider__getPage 对全部页面输出 flux JSON）
#
# 用法：
#   bash scripts/flip-menu-to-flux.sh
#   bash scripts/flip-menu-to-flux.sh --check   # 只检查不修改
#
# 与 docs/architecture/view-and-page-strategy.md「渲染模式（flux-only，强制）」对应。
# 生成式 _erp-*.action-auth.xml 的持久 FLUX 由 scripts/flip-orm-to-flux.sh
# （orm.xml 实体 ext:web-renderer="flux"）在 codegen 源头保证，本脚本为兜底。

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
APP_YAML="$REPO_ROOT/app-erp-all/src/main/resources/application.yaml"

CHECK_ONLY=false
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=true

info() { echo "[flip-flux] $*"; }
die() { echo "[flip-flux] ERROR: $*" >&2; exit 1; }

FILES=$(grep -rl 'component="AMIS"' \
  "$REPO_ROOT"/module-*/erp-*-web/src/main/resources/_vfs/erp/*/auth/*.action-auth.xml \
  "$REPO_ROOT"/app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml 2>/dev/null || true)

AMIS_TOTAL=0
FILE_COUNT=0
for f in $FILES; do
  n=$(grep -c 'component="AMIS"' "$f" || true)
  AMIS_TOTAL=$((AMIS_TOTAL + n))
  FILE_COUNT=$((FILE_COUNT + 1))
done

info "发现 ${FILE_COUNT} 个 action-auth.xml，共 ${AMIS_TOTAL} 处 component=\"AMIS\""

if [ "$CHECK_ONLY" = true ]; then
  info "--check 模式：不修改。"
  exit 0
fi

if [ "$AMIS_TOTAL" -gt 0 ]; then
  for f in $FILES; do
    sed -i '' 's/component="AMIS"/component="FLUX"/g' "$f"
  done
  info "已翻转 ${FILE_COUNT} 个文件、${AMIS_TOTAL} 处 AMIS→FLUX"
else
  info "无 AMIS 需要翻转（已是 flux-only）"
fi

if ! grep -q 'render-mode: flux' "$APP_YAML"; then
  if [ "$CHECK_ONLY" = true ]; then exit 0; fi
  sed -i '' '/^    validate-page-model: true$/a\
    render-mode: flux' "$APP_YAML"
  info "application.yaml 已追加 nop.web.render-mode: flux"
else
  info "application.yaml 已含 nop.web.render-mode: flux"
fi

REMAIN=$(grep -rl 'component="AMIS"' \
  "$REPO_ROOT"/module-*/erp-*-web/src/main/resources/_vfs/erp/*/auth/*.action-auth.xml \
  "$REPO_ROOT"/app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml 2>/dev/null | wc -l | tr -d ' ' || true)
FLUX_TOTAL=$(grep -rh 'component="FLUX"' \
  "$REPO_ROOT"/module-*/erp-*-web/src/main/resources/_vfs/erp/*/auth/*.action-auth.xml \
  "$REPO_ROOT"/app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml 2>/dev/null | wc -l | tr -d ' ' || true)
info "验证：剩余 AMIS=${REMAIN} 文件，FLUX 总数=${FLUX_TOTAL}"
[ "$REMAIN" = "0" ] || die "仍有 AMIS 残留！"
info "完成：全部菜单已强制 FLUX，render-mode=flux 已生效"
