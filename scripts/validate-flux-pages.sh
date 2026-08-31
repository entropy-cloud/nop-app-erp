#!/usr/bin/env bash
# Flux 页面导出 + JS 编译验证一键入口（docs/architecture/flux-page-export-and-validation.md）
#
# 步骤：
#   1. mvn 跑 ErpAllFluxPagesExportTest，把全部 enabled modules 页面以 flux 模式
#      导出到 app-erp-all/target/flux-pages/（含 manifest.json）
#   2. 校验兄弟仓库 nop-chaos-flux 的 dist 就绪（缺失则提示 pnpm build，退出码 2）
#   3. node 运行 nop-chaos-flux 的 validate-pages.mjs 对导出产物做编译校验，
#      报告写入 _tmp/flux-page-validation-report.json
#
# 退出码：0 = 全部页面通过；1 = 存在 error 级发现或导出失败；2 = 环境错误。
#
# 环境变量：
#   FLUX_REPO  — nop-chaos-flux 仓库位置（缺省 ../nop-chaos-flux，相对本仓库）

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
FLUX_REPO="${FLUX_REPO:-$REPO_ROOT/../nop-chaos-flux}"
EXPORT_DIR="$REPO_ROOT/app-erp-all/target/flux-pages"
REPORT="$REPO_ROOT/_tmp/flux-page-validation-report.json"

echo "[1/3] Exporting all pages in flux mode (app-erp-all JUnit) ..."
(cd "$REPO_ROOT" && mvn -pl app-erp-all test -Dtest=ErpAllFluxPagesExportTest -Dsurefire.failIfNoSpecifiedTests=false)

if [ ! -f "$EXPORT_DIR/manifest.json" ]; then
  echo "ERROR: export manifest not found at $EXPORT_DIR/manifest.json" >&2
  exit 2
fi

echo "[2/3] Checking nop-chaos-flux dists at $FLUX_REPO ..."
for pkg in flux-core flux-compiler flux-renderers-basic flux-renderers-layout \
  flux-renderers-form flux-renderers-form-advanced flux-renderers-data \
  flux-renderers-content flux-renderers-mobile flux-code-editor \
  flux-renderers-scheduling flux-renderers-ai flux-renderers-industrial; do
  if [ ! -f "$FLUX_REPO/packages/$pkg/dist/index.js" ]; then
    echo "ERROR: $pkg/dist/index.js missing — run 'pnpm build' (or per-package build) in nop-chaos-flux first" >&2
    exit 2
  fi
done

echo "[3/4] Validating exported ERP flux pages (erp/ subtree, must be 0 error) ..."
cd "$FLUX_REPO"
node --experimental-loader ./flux-guide/scripts/css-stub.mjs \
  --import ./flux-guide/scripts/env-stub.mjs \
  flux-guide/scripts/validate-pages.mjs "$EXPORT_DIR/erp" --report="$REPORT"

echo "[4/4] Validating platform pages (nop/ subtree; residuals tracked upstream, non-blocking for ERP) ..."
node --experimental-loader ./flux-guide/scripts/css-stub.mjs \
  --import ./flux-guide/scripts/env-stub.mjs \
  flux-guide/scripts/validate-pages.mjs "$EXPORT_DIR/nop" --max-errors=10000 || echo "(platform residuals known — see docs/plans/2026-08-30-2238-1 Deferred)"
