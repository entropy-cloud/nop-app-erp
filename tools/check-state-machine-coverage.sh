#!/usr/bin/env bash
# tools/check-state-machine-coverage.sh — entity-state-machine 守卫层 (M5.2)
#
# 包装 docs/audits/scripts/state-machine-coverage-check.py（M5.1 工具）：
#   - 本地 manual 模式（默认）：exit 0 even if finding（开发期友好）
#   - CI strict 模式（--strict）：exit 1 if unwhitelisted finding（CI 友好）
#   - 自动产出 JSON + Markdown 报告到 docs/audits/check/<TS>-entity-state-machine-m5-2/
#     （按本会话建立的多次执行隔离纪律——每次执行新建日期时间前缀子目录）
#
# 用法：
#   bash tools/check-state-machine-coverage.sh
#   bash tools/check-state-machine-coverage.sh --strict
#   bash tools/check-state-machine-coverage.sh --root /path/to/repo
#   bash tools/check-state-machine-coverage.sh --help

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TOOL_PATH="$REPO_ROOT/docs/audits/scripts/state-machine-coverage-check.py"

usage() {
    echo "Usage: $0 [--strict] [--root PATH] [--help]"
    echo ""
    echo "Options:"
    echo "  --strict        CI mode: exit 1 if unwhitelisted findings (default: local manual, exit 0 always)"
    echo "  --root PATH     repo root (default: parent of this script)"
    echo "  --help          show this help"
}

STRICT=0
ROOT="$REPO_ROOT"

while [[ $# -gt 0 ]]; do
    case "$1" in
        --strict)
            STRICT=1
            shift
            ;;
        --root)
            ROOT="$2"
            shift 2
            ;;
        --help)
            usage
            exit 0
            ;;
        *)
            echo "[ERROR] unknown arg: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if [[ ! -f "$TOOL_PATH" ]]; then
    echo "[FATAL] tool not found: $TOOL_PATH" >&2
    exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
    echo "[FATAL] python3 not found" >&2
    exit 2
fi

# 时间戳 + 多次执行隔离目录（按多次执行隔离纪律）
TS=$(date +%Y-%m-%d-%H%M)
REPORT_DIR="$REPO_ROOT/docs/audits/check/${TS}-entity-state-machine-m5-2"
mkdir -p "$REPORT_DIR"

JSON_PATH="$REPORT_DIR/audit.json"
MD_PATH="$REPORT_DIR/audit.md"
LATEST_LINK="$REPO_ROOT/docs/audits/check/LATEST-m5-2"

# 维护 LATEST 符号链接（best-effort，失败也不阻断）
ln -sfn "$REPORT_DIR" "$LATEST_LINK" 2>/dev/null || true

echo "[INFO] running state-machine coverage check (M5.2 wrapper)"
echo "[INFO] tool: $TOOL_PATH"
echo "[INFO] root: $ROOT"
echo "[INFO] mode: $([ "$STRICT" -eq 1 ] && echo 'CI strict' || echo 'local manual')"
echo "[INFO] report dir: $REPORT_DIR"
echo "[INFO] latest link: $LATEST_LINK"
echo ""

ARGS=(--root "$ROOT" --json-output "$JSON_PATH" --md-output "$MD_PATH")
if [[ "$STRICT" -eq 1 ]]; then
    ARGS+=(--strict)
fi

set +e
python3 "$TOOL_PATH" "${ARGS[@]}"
TOOL_EXIT=$?
set -e

echo ""
echo "[INFO] tool exit code: $TOOL_EXIT"
echo "[INFO] report written:"
echo "  - $JSON_PATH"
echo "  - $MD_PATH"

# human-friendly 摘要
if [[ -f "$JSON_PATH" ]]; then
    echo ""
    echo "=== Summary ==="
    python3 -c "
import json, sys
d = json.load(open(sys.argv[1]))
s = d['summary']
print(f\"  Beans 扫描: {s['beans_scanned']}\")
print(f\"  Findings 总数: {s['findings_total']}\")
print(f\"  白名单命中: {s['whitelisted']}\")
print(f\"  待处理: {s['findings_after_whitelist']}\")
print()
if d.get('by_domain'):
    print('  按域分布 (TOP 5):')
    for dn, n in sorted(d['by_domain'].items(), key=lambda x: -x[1])[:5]:
        print(f'    {dn}: {n}')
    print()
if d.get('by_finding_type'):
    print('  按类型分布:')
    for tn, n in sorted(d['by_finding_type'].items(), key=lambda x: -x[1]):
        print(f'    {tn}: {n}')
" "$JSON_PATH"
fi

exit "$TOOL_EXIT"
