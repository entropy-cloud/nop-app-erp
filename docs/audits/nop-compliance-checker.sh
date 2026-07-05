#!/usr/bin/env bash
# nop-compliance-checker.sh — Nop 平台反模式启发式检测工具
# ============================================================
# 用途：扫描 nop-app-erp 仓库中违反 nop-entropy/docs-for-ai 最佳实践的代码模式
# 用法：bash docs/audits/nop-compliance-checker.sh [--module <name>]
# 输出：按规则分类的违规列表 + 汇总表
#
# 规则来源：
#   - nop-entropy/docs-for-ai/02-core-guides/service-layer.md（反模式表）
#   - nop-entropy/docs-for-ai/04-reference/safe-api-reference.md（安全 API）
#   - nop-entropy/docs-for-ai/04-reference/bizmodel-method-selfcheck.md（方法自检）
#   - 项目 AGENTS.md "Nop Platform 特定规则" 节
#   - 2026-07-05 补充审计中发现的新规则（R8/R9/R10）

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"

TMPDIR=$(mktemp -d)
trap "rm -rf $TMPDIR" EXIT

# --- 辅助函数 ---
# 递归搜索（排除 _gen、target、.git）
rgrep() {
  local pattern="$1"
  local dir="$2"
  local include="${3:-}"
  local extra_args=()
  [[ -n "$include" ]] && extra_args=(-name "$include")
  find "$dir" "${extra_args[@]}" \
    -not -path '*_gen*' -not -path '*/target/*' -not -path '*/.git/*' \
    -type f -exec grep -Hn "$pattern" {} \; 2>/dev/null || true
}

# 只在 BizModel 文件中搜索
rgrep_bizmodel() {
  rgrep "$1" "$REPO_ROOT" '*BizModel.java'
}

# 在所有 Java 中搜索（排除 _gen、test、target）
rgrep_alljava() {
  rgrep "$1" "$REPO_ROOT" '*.java'
}

# 只在非测试 Java 中搜索
rgrep_prodjava() {
  find "$REPO_ROOT" -name '*.java' \
    -not -path '*_gen*' -not -path '*/test/*' -not -path '*/target/*' -not -path '*/.git/*' \
    -type f -exec grep -Hn "$1" {} \; 2>/dev/null || true
}

cnt() { [[ -z "$1" ]] && echo 0 || echo "$1" | wc -l | tr -d ' '; }

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  Nop 平台合规性启发式检测器                                  ║"
echo "║  仓库: $REPO_ROOT"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ============================================================
# R1: BizModel 中 dao() 直接调用
# ============================================================
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R1] 🔴 高 — BizModel 中 dao() 直接调用（绕过 CrudBizModel 生命周期）"
echo "规则: safe-api-reference.md — 禁止绕过 CrudBizModel 管道"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "▸ R1a: dao().saveEntity() — 应用 saveEntity(entity, null, context)"
R1A=$(rgrep_bizmodel 'dao()\.saveEntity' | grep -v '_gen/' || true)
R1A_N=$(cnt "$R1A")
[[ $R1A_N -gt 0 ]] && echo "$R1A" | sed 's/^/  /'
echo "  → 命中: $R1A_N 处"
echo "$R1A_N" > "$TMPDIR/r1a"

echo ""
echo "▸ R1b: dao().updateEntity() — 应用 updateEntity(entity, null, context)"
R1B=$(rgrep_bizmodel 'dao()\.updateEntity' | grep -v '_gen/' || true)
R1B_N=$(cnt "$R1B")
[[ $R1B_N -gt 0 ]] && echo "$R1B" | head -30 | sed 's/^/  /'
[[ $R1B_N -gt 30 ]] && echo "  ... (共 $R1B_N 处，显示前 30)"
echo "  → 命中: $R1B_N 处"
echo "$R1B_N" > "$TMPDIR/r1b"

echo ""
echo "▸ R1c: dao().getEntityById() — 应用 requireEntity(id, null, context)"
R1C=$(rgrep_bizmodel 'dao()\.getEntityById' | grep -v '_gen/' || true)
R1C_N=$(cnt "$R1C")
[[ $R1C_N -gt 0 ]] && echo "$R1C" | sed 's/^/  /'
echo "  → 命中: $R1C_N 处"
echo "$R1C_N" > "$TMPDIR/r1c"

echo ""
echo "▸ R1d: dao().findAllByQuery() — 应用 findList(query, null, context)"
R1D=$(rgrep_bizmodel 'dao()\.findAllByQuery' | grep -v '_gen/' || true)
R1D_N=$(cnt "$R1D")
[[ $R1D_N -gt 0 ]] && echo "$R1D" | sed 's/^/  /'
echo "  → 命中: $R1D_N 处"
echo "$R1D_N" > "$TMPDIR/r1d"

# ============================================================
# R2: 跨域 daoFor() 绕过 I*Biz
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R2] 🔴 高 — daoFor() 绕过 I*Biz 接口"
echo "规则: service-layer.md — 跨实体访问必须注入 I*Biz 接口"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "▸ R2a: BizModel 中 daoFor(ErpMd*) — 跨 master-data 域"
R2A=$(rgrep_bizmodel 'daoFor(ErpMd' | grep -v '_gen/' || true)
R2A_N=$(cnt "$R2A")
[[ $R2A_N -gt 0 ]] && echo "$R2A" | sed 's/^/  /'
echo "  → 命中: $R2A_N 处"
echo "$R2A_N" > "$TMPDIR/r2a"

echo ""
echo "▸ R2b: BizModel 中 daoFor(Erp*) — 全部跨域引用"
R2B=$(rgrep_bizmodel 'daoFor(Erp' | grep -v '_gen/' || true)
R2B_N=$(cnt "$R2B")
[[ $R2B_N -gt 0 ]] && echo "$R2B" | head -15 | sed 's/^/  /'
[[ $R2B_N -gt 15 ]] && echo "  ... (共 $R2B_N 处)"
echo "  → 命中: $R2B_N 处"
echo "$R2B_N" > "$TMPDIR/r2b"

echo ""
echo "▸ R2c: 全生产代码 daoFor() 总量"
R2C=$(rgrep_prodjava 'daoFor(' | grep -v '_gen/' || true)
R2C_N=$(cnt "$R2C")
echo "  → 生产代码总计: $R2C_N 处"
echo "$R2C_N" > "$TMPDIR/r2c"

echo ""
echo "▸ R2d: Processor/Dispatcher/Engine 中 daoFor(ErpMd*)"
R2D=$(find "$REPO_ROOT" \( -name '*Processor.java' -o -name '*Dispatcher.java' -o -name '*Engine.java' \) \
  -not -path '*_gen*' -not -path '*/test/*' -not -path '*/target/*' \
  -type f -exec grep -Hn 'daoFor(ErpMd' {} \; 2>/dev/null || true)
R2D_N=$(cnt "$R2D")
[[ $R2D_N -gt 0 ]] && echo "$R2D" | sed 's/^/  /'
echo "  → 命中: $R2D_N 处"
echo "$R2D_N" > "$TMPDIR/r2d"

# ============================================================
# R3: new Erp*() 构造实体
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R3] 🟡 中 — new Erp*() 直接构造实体"
echo "规则: safe-api-reference.md — 应使用 newEntity()"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
R3=$(rgrep_prodjava 'new Erp[A-Z]' | grep -v '_gen/' | grep -v 'Test' | grep -v '/test/' || true)
R3_N=$(cnt "$R3")
[[ $R3_N -gt 0 ]] && echo "$R3" | head -15 | sed 's/^/  /'
[[ $R3_N -gt 15 ]] && echo "  ... (共 $R3_N 处)"
echo "  → 命中: $R3_N 处"
echo "$R3_N" > "$TMPDIR/r3"

# ============================================================
# R4: extends RuntimeException
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R4] 🟢 低 — extends RuntimeException"
R4=$(rgrep_prodjava 'extends RuntimeException' | grep -v '_gen/' || true)
R4_N=$(cnt "$R4")
echo "  → 命中: $R4_N 处"
echo "$R4_N" > "$TMPDIR/r4"

# ============================================================
# R5: @Inject private
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R5] 🟡 中 — @Inject + private"
R5=$(rgrep_prodjava '@Inject' | grep -v '_gen/' | while read -r line; do
  file=$(echo "$line" | cut -d: -f1)
  lineno=$(echo "$line" | cut -d: -f2)
  nextline=$((lineno + 1))
  nextcontent=$(sed -n "${nextline}p" "$file" 2>/dev/null || true)
  if echo "$nextcontent" | grep -q 'private '; then
    echo "$file:$lineno:$line → $nextcontent"
  fi
done || true)
R5_N=$(cnt "$R5")
[[ $R5_N -gt 0 ]] && echo "$R5" | sed 's/^/  /'
echo "  → 命中: $R5_N 处"
echo "$R5_N" > "$TMPDIR/r5"

# ============================================================
# R6: @Transactional in BizModel
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R6] 🟢 低 — @Transactional 在 BizModel 上"
R6=$(rgrep_bizmodel '@Transactional' | grep -v '_gen/' || true)
R6_N=$(cnt "$R6")
[[ $R6_N -gt 0 ]] && echo "$R6" | sed 's/^/  /'
echo "  → 命中: $R6_N 处（需逐个判断是否有意的 REQUIRES_NEW）"
echo "$R6_N" > "$TMPDIR/r6"

# ============================================================
# R7: System.currentTimeMillis
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R7] 🟢 低 — System.currentTimeMillis()"
R7=$(rgrep_prodjava 'System\.currentTimeMillis' | grep -v '_gen/' || true)
R7_N=$(cnt "$R7")
echo "  → 命中: $R7_N 处"
echo "$R7_N" > "$TMPDIR/r7"

# ============================================================
# R8: Processor 编排完整性
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R8] 🔴 高 — Processor 缺少 xbiz 接线"
echo "规则: service-layer-orchestration.md — Processor 需 xbiz 绑定"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
R8_N=0
while IFS= read -r proc; do
  base=$(basename "$proc" Processor.java)
  module_dir=$(echo "$proc" | sed -E 's|/src/main/java.*||')
  if ! find "$module_dir" -name "${base}.xbiz.xml" 2>/dev/null | grep -q .; then
    echo "  ✗ $(echo "$proc" | sed "s|$REPO_ROOT/||")"
    R8_N=$((R8_N + 1))
  fi
done < <(find "$REPO_ROOT" -name '*Processor.java' -not -path '*_gen*' -not -path '*/test/*' -not -path '*/target/*' 2>/dev/null || true)
echo "  → 命中: $R8_N 个 Processor 缺少 xbiz"
echo "$R8_N" > "$TMPDIR/r8"

# ============================================================
# R9: 跨 Processor 行为一致性
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R9] 🟡 中 — doReverseApprove 行为不一致"
echo "启发式: 同类 Processor 的反审核方法应有一致的字段处理"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  反审核时清除 approvedBy/approvedAt 的 Processor:"
find "$REPO_ROOT" -name '*Processor.java' -not -path '*_gen*' -not -path '*/target/*' \
  -exec grep -l 'setApprovedBy(null)\|setApprovedAt(null)' {} \; 2>/dev/null | while read -r f; do
  echo "    ✓ $(echo "$f" | sed "s|$REPO_ROOT/||")"
done || true
echo ""
echo "  反审核时未清除的 Processor（仅改 approveStatus）:"
find "$REPO_ROOT" -name '*Processor.java' -not -path '*_gen*' -not -path '*/target/*' \
  -exec grep -l 'doReverseApprove' {} \; 2>/dev/null | while read -r f; do
  if ! grep -q 'setApprovedBy(null)\|setApprovedAt(null)' "$f" 2>/dev/null; then
    echo "    ✗ $(echo "$f" | sed "s|$REPO_ROOT/||")"
  fi
done || true
echo "$R8_N" > "$TMPDIR/r9"

# ============================================================
# R10: REQUIRES_NEW
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R10] 🟡 中 — REQUIRES_NEW 事务"
R10=$(rgrep_prodjava 'REQUIRES_NEW' | grep -v '_gen/' || true)
R10_N=$(cnt "$R10")
[[ $R10_N -gt 0 ]] && echo "$R10" | sed 's/^/  /'
echo "  → 命中: $R10_N 处"
echo "$R10_N" > "$TMPDIR/r10"

# ============================================================
# 汇总
# ============================================================
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  汇总                                                      ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
printf "%-6s %-42s %-8s %s\n" "规则" "描述" "严重度" "命中"
printf "%-6s %-42s %-8s %s\n" "------" "------------------------------------------" "--------" "------"
printf "%-6s %-42s %-8s %s\n" "R1a" "dao().saveEntity (BizModel)" "🔴 高" "$(cat $TMPDIR/r1a)"
printf "%-6s %-42s %-8s %s\n" "R1b" "dao().updateEntity (BizModel)" "🔴 高" "$(cat $TMPDIR/r1b)"
printf "%-6s %-42s %-8s %s\n" "R1c" "dao().getEntityById (BizModel)" "🔴 高" "$(cat $TMPDIR/r1c)"
printf "%-6s %-42s %-8s %s\n" "R1d" "dao().findAllByQuery (BizModel)" "🔴 高" "$(cat $TMPDIR/r1d)"
printf "%-6s %-42s %-8s %s\n" "R2a" "BizModel daoFor(ErpMd*)" "🔴 高" "$(cat $TMPDIR/r2a)"
printf "%-6s %-42s %-8s %s\n" "R2b" "BizModel daoFor(Erp*) 跨域" "🔴 高" "$(cat $TMPDIR/r2b)"
printf "%-6s %-42s %-8s %s\n" "R2c" "全生产代码 daoFor() 总量" "🔴 高" "$(cat $TMPDIR/r2c)"
printf "%-6s %-42s %-8s %s\n" "R2d" "Processor daoFor(ErpMd*)" "🔴 高" "$(cat $TMPDIR/r2d)"
printf "%-6s %-42s %-8s %s\n" "R3" "new Erp*() 构造实体" "🟡 中" "$(cat $TMPDIR/r3)"
printf "%-6s %-42s %-8s %s\n" "R4" "extends RuntimeException" "🟢 低" "$(cat $TMPDIR/r4)"
printf "%-6s %-42s %-8s %s\n" "R5" "@Inject private" "🟡 中" "$(cat $TMPDIR/r5)"
printf "%-6s %-42s %-8s %s\n" "R6" "@Transactional in BizModel" "🟢 低" "$(cat $TMPDIR/r6)"
printf "%-6s %-42s %-8s %s\n" "R7" "System.currentTimeMillis()" "🟢 低" "$(cat $TMPDIR/r7)"
printf "%-6s %-42s %-8s %s\n" "R8" "Processor 无 xbiz 接线" "🔴 高" "$(cat $TMPDIR/r8)"
printf "%-6s %-42s %-8s %s\n" "R10" "REQUIRES_NEW 事务" "🟡 中" "$(cat $TMPDIR/r10)"
echo ""
echo "检测完成。"
echo "注意: 命中项需人工逐一确认是否为合理偏离（如文档化的 REQUIRES_NEW）。"
