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
# 性能优化（plan 2026-07-20-2200-1 M-1）：使用 `-prune` 跳过 target/_gen/node_modules/.git，
# 而非 `-not -path`（后者仍会进入被排除目录后再过滤）。
# 实测优化前 81s，优化后约 30s（prune 避免对 ~850 个被排除目录的递归 descend）。
PRUNE_DIRS='-type d \( -name target -o -name _gen -o -name node_modules -o -name .git \) -prune'

# 递归搜索（性能优化：-prune 跳过 _gen/target/node_modules/.git 目录）
rgrep() {
  local pattern="$1"
  local dir="$2"
  local include="${3:-}"
  local extra_args=()
  [[ -n "$include" ]] && extra_args=(-name "$include")
  eval "find '$dir' $PRUNE_DIRS -o ${extra_args[*]:-} -type f -print" 2>/dev/null \
    | xargs grep -Hn "$pattern" 2>/dev/null || true
}

# 只在 BizModel 文件中搜索
rgrep_bizmodel() {
  rgrep "$1" "$REPO_ROOT" '*BizModel.java'
}

# 在所有 Java 中搜索（排除 _gen、test、target）
rgrep_alljava() {
  rgrep "$1" "$REPO_ROOT" '*.java'
}

# 只在非测试 Java 中搜索（性能优化：-prune 跳过 test 目录）
rgrep_prodjava() {
  eval "find '$REPO_ROOT' $PRUNE_DIRS -o -type d -name test -prune -o -name '*.java' -type f -print" 2>/dev/null \
    | xargs grep -Hn "$1" 2>/dev/null || true
}

# 共享内核跨域 import 计数（plan 2026-07-24-1400-1 R12）：
# 排除 test 目录 + 类型所属域 + _gen/target（与 R12 基线 69/66/38 计数口径一致）
rgrep_shared_import() {
  local pattern="$1"
  local exclude_dir="$2"
  eval "find '$REPO_ROOT' $PRUNE_DIRS -o -type d -name test -prune -o -type d -name '$exclude_dir' -prune -o -name '*.java' -type f -print" 2>/dev/null \
    | xargs grep -l "$pattern" 2>/dev/null || true
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
# 测量口径校准（plan 2026-07-27-0823-1 Phase 2 Decision 1 option b）：
# 原 grep 不区分代码行与 javadoc/注释行，~11/28 命中为 javadoc `*` / 行注释 `//` 引用
# （`* 经 dao().findAllByQuery 绕过 findList 管道` 类说明）。校准=管道后追加注释行排除过滤
# （per-rule，不动 rgrep_bizmodel helper，避免影响已稳定的 R1a/R1b/R1c/R2a/R2b）。
# 排除：javadoc 续行（`*`）+ 行注释（`//`）+ 块注释开闭（`/*`/`*/`）+ {@code}/{@link 安全网。
# 残留风险：块注释 `/* ... */` 跨行命中（无 `*` 续行前缀）漏排除——实测为 0；未来出现则升级 AST。
R1D=$(rgrep_bizmodel 'dao()\.findAllByQuery' | grep -v '_gen/' \
  | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*|\*/)' | grep -vE '\{@code|\{@link' || true)
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
R2D=$(eval "find '$REPO_ROOT' $PRUNE_DIRS -o -type d -name test -prune -o -type f \\( -name '*Processor.java' -o -name '*Dispatcher.java' -o -name '*Engine.java' \\) -print" 2>/dev/null \
  | xargs grep -Hn 'daoFor(ErpMd' 2>/dev/null || true)
R2D_N=$(cnt "$R2D")
[[ $R2D_N -gt 0 ]] && echo "$R2D" | sed 's/^/  /'
echo "  → 命中: $R2D_N 处"
echo "$R2D_N" > "$TMPDIR/r2d"

# ============================================================
# R3: new Erp*() 构造实体
# ============================================================
# 测量口径校准（plan 2026-07-24-0941-2 Phase 1 裁决 option c）：
# 原 regex `new Erp[A-Z]` 匹配任何 Erp* 前缀类，但 ~14/19 为非 ORM 实体类
# （引擎 / support / value / DTO / 私有内部投影类），规则显著过匹配。
# 校准=交叉引用 *.orm.xml <entity className> 声明构建已注册实体白名单，
# R3 仅对 `new <RegisteredEntity>()` 计数（精确校准，0 FP / 0 FN；
# 未来新增实体自动纳入白名单，因 checker 运行时从 orm.xml 动态提取）。
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R3] 🟡 中 — new Erp*() 直接构造实体"
echo "规则: safe-api-reference.md — 应使用 newEntity()（仅计已注册 ORM 实体构造）"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
# 构建已注册 ORM 实体短名白名单（从源 model/*.orm.xml 动态提取，排除 _gen/target）
ENTITY_WHITELIST=$(eval "find '$REPO_ROOT' $PRUNE_DIRS -o -path '*/model/*.orm.xml' -type f -print" 2>/dev/null \
  | xargs grep -oh '<entity className="[^"]*"' 2>/dev/null \
  | sed -E 's/.*className="([^"]*)".*/\1/' \
  | sed -E 's/.*\.//' | sort -u)
R3_RAW=$(rgrep_prodjava 'new Erp[A-Z]' | grep -v '_gen/' | grep -v 'Test' | grep -v '/test/' || true)
R3=""
if [[ -n "$R3_RAW" ]]; then
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    cls=$(echo "$line" | grep -oE 'new Erp[A-Za-z0-9_]+' | head -1 | sed 's/new //')
    if [[ -n "$cls" ]] && echo "$ENTITY_WHITELIST" | grep -qxF "$cls"; then
      R3="$R3$line"$'\n'
    fi
  done <<< "$R3_RAW"
fi
R3="${R3%$'\n'}"
R3_N=$(cnt "$R3")
[[ $R3_N -gt 0 ]] && echo "$R3" | head -15 | sed 's/^/  /'
[[ $R3_N -gt 15 ]] && echo "  ... (共 $R3_N 处)"
echo "  → 命中: $R3_N 处（仅计已注册 ORM 实体；非实体 Erp* 前缀类已校准排除）"
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
# 测量口径校准（plan 2026-07-27-0823-1 Phase 2 Decision 1 option b）：
# 原 grep 不区分代码行与 javadoc/注释行，~5/7 命中为 javadoc `*` / 行注释 `//` 引用
# （`* 不叠加 {@code @Transactional}` / `// nop-check: allow @Transactional` 类）。校准=同 R1d。
R6=$(rgrep_bizmodel '@Transactional' | grep -v '_gen/' \
  | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*|\*/)' | grep -vE '\{@code|\{@link' || true)
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
# 测量口径校准（plan 2026-07-25-1057-1 Phase 2 Decision A）：
# 原 find 收集全部 `*Processor.java`，但 `module-common-service/` 下的 7 个
# `Abstract*Processor`（`public abstract class ... extends AbstractProcessor<T>`）
# 为 2200-1 Phase 1 创建的抽象基类——非领域 Processor，不经 xbiz 路由（由具体
# 子类继承后经 BizModel @Inject 消费）。R8 原始语义「领域 Processor 缺少 xbiz
# 接线」不覆盖抽象基类 → 校准=排除 `module-common-service/` 目录（对齐 0941-2
# R3 交叉引用 orm.xml 先例的「排除集」思路，最小侵入）。
# 残留风险：若未来在 module-common-service/ 新增具体（非 abstract）领域 Processor，
# 本排除会静默豁免——届时升级为动态 `abstract class` 提取（开独立 successor）。
#
# 二次校准（plan 2026-07-25-1057-2 Phase 4 R8 Decision）：
# per-mutation Processor 文件（如 `ErpPurOrderApproveProcessor`）继承
# `AbstractApproveProcessor<T>` 等 7 个抽象基类，经 BizModel @BizMutation →
# @Inject 路由消费（非 Processor xbiz 接线）。R8 原始语义「领域 Processor 缺少
# xbiz 接线」不覆盖 per-mutation Processor（其路由面是 BizModel @BizMutation，
# 而 BizModel 已有自身的方法声明经反射自动生成 GraphQL schema）。校准=循环内
# 跳过类体含 `extends Abstract*Processor` 的文件。全域 149 per-mutation 文件
# 全部继承 7 抽象基类，本次校准排除后 R8 回落至原 42 monolithic 基线。
# 残留风险：若未来 per-mutation Processor 不继承抽象基类（如手写 per-mutation
# 类未 extends Abstract*），本 grep 会漏排除——届时升级为 per-mutation 文件
# 命名规则动态匹配（开独立 successor）。
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R8] 🔴 高 — Processor 缺少 xbiz 接线"
echo "规则: service-layer-orchestration.md — Processor 需 xbiz 绑定"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
R8_N=0
while IFS= read -r proc; do
  # 跳过 per-mutation Processor（继承 Abstract*Processor，经 BizModel 路由非 xbiz）
  if grep -qE 'extends Abstract[A-Z][a-zA-Z]*Processor' "$proc" 2>/dev/null; then
    continue
  fi
  base=$(basename "$proc" Processor.java)
  module_dir=$(echo "$proc" | sed -E 's|/src/main/java.*||')
  if ! find "$module_dir" -name "${base}.xbiz.xml" 2>/dev/null | grep -q .; then
    echo "  ✗ $(echo "$proc" | sed "s|$REPO_ROOT/||")"
    R8_N=$((R8_N + 1))
  fi
done < <(eval "find '$REPO_ROOT' $PRUNE_DIRS -o -type d -name test -prune -o -type d -name module-common-service -prune -o -name '*Processor.java' -type f -print" 2>/dev/null || true)
echo "  → 命中: $R8_N 个 Processor 缺少 xbiz（已排除 module-common-service 抽象基类 + per-mutation 子类）"
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
eval "find '$REPO_ROOT' $PRUNE_DIRS -o -name '*Processor.java' -type f -print" 2>/dev/null \
  | xargs grep -l 'setApprovedBy(null)\|setApprovedAt(null)' 2>/dev/null | while read -r f; do
  echo "    ✓ $(echo "$f" | sed "s|$REPO_ROOT/||")"
done || true
echo ""
echo "  反审核时未清除的 Processor（仅改 approveStatus）:"
eval "find '$REPO_ROOT' $PRUNE_DIRS -o -name '*Processor.java' -type f -print" 2>/dev/null \
  | xargs grep -l 'doReverseApprove' 2>/dev/null | while read -r f; do
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
# 测量口径校准（plan 2026-07-27-0823-1 Phase 2 Decision 1 option b）：
# 原 grep 不区分代码行与 javadoc/注释行，~45/51 命中为跨 11 域 *PostingExecutor/*PostingDispatcher
# 的 javadoc `* ...{@code REQUIRES_NEW} 承接...` 引用 + `// nop-check:` / `// 容错...` 行注释。
# 真实代码站点仅 6 处（ErpFinVoucherBizModel post/reverse + ErpFinPostingExceptionRecorder/
# ErpFinDeferredPostingRetryHelper 的 runInTransaction），全部为 processor-extension-pattern.md
# 硬规则 1 文档化的合法跨域失败隔离事务边界。校准=同 R1d（per-rule 注释行排除）。
R10=$(rgrep_prodjava 'REQUIRES_NEW' | grep -v '_gen/' \
  | grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*|\*/)' | grep -vE '\{@code|\{@link' || true)
R10_N=$(cnt "$R10")
[[ $R10_N -gt 0 ]] && echo "$R10" | sed 's/^/  /'
echo "  → 命中: $R10_N 处"
echo "$R10_N" > "$TMPDIR/r10"

# ============================================================
# R11: Processor 状态判断方法重复（应上提到实体）
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R11] 🟡 中 — Processor 中重复定义的状态判断方法（应上提到实体）"
echo "规则: DDD 实体方法上提——isAlreadyApproved/isAlreadyRejected 应为实体方法 isApproved/isRejected"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
R11=$(eval "find '$REPO_ROOT' $PRUNE_DIRS -o -type d -name test -prune -o -name '*Processor.java' -type f -print" 2>/dev/null \
  | xargs grep -HnE 'protected boolean (isAlreadyApproved|isAlreadyRejected)\(' 2>/dev/null || true)
R11_N=$(cnt "$R11")
[[ $R11_N -gt 0 ]] && echo "$R11" | sed 's/^/  /'
echo "  → 命中: $R11_N 处（实体方法上提后应为 0）"
echo "$R11_N" > "$TMPDIR/r11"

# ============================================================
# R12: 共享内核跨域 import 计数（F4 闭包项 #5 显式共享内核基线）
# ============================================================
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "[R12] 🟡 中 — 共享内核跨域 import 计数（裁决见 docs/analysis/shared-kernel-extraction-decision.md）"
echo "规则: F4 闭包项 #5 — 3 共享类型跨域 import 不得无记录增长（基线 69/66/38）"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "▸ R12a: import ErpFinBusinessType（跨域，排除 module-finance）"
R12A=$(rgrep_shared_import 'import app\.erp\.fin\.dao\.ErpFinBusinessType;' 'module-finance')
R12A_N=$(cnt "$R12A")
echo "  → 命中: $R12A_N 处"
echo "$R12A_N" > "$TMPDIR/r12a"

echo ""
echo "▸ R12b: import PostingEvent（跨域，排除 module-finance）"
R12B=$(rgrep_shared_import 'import app\.erp\.fin\.dao\.PostingEvent;' 'module-finance')
R12B_N=$(cnt "$R12B")
echo "  → 命中: $R12B_N 处"
echo "$R12B_N" > "$TMPDIR/r12b"

echo ""
echo "▸ R12c: import AcctSchemaResolver（跨域，排除 module-master-data）"
R12C=$(rgrep_shared_import 'import app\.erp\.md\.dao\.AcctSchemaResolver;' 'module-master-data')
R12C_N=$(cnt "$R12C")
echo "  → 命中: $R12C_N 处"
echo "$R12C_N" > "$TMPDIR/r12c"

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
printf "%-6s %-42s %-8s %s\n" "R11" "Processor 重复状态判断方法" "🟡 中" "$(cat $TMPDIR/r11)"
printf "%-6s %-42s %-8s %s\n" "R12a" "共享内核 import ErpFinBusinessType" "🟡 中" "$(cat $TMPDIR/r12a)"
printf "%-6s %-42s %-8s %s\n" "R12b" "共享内核 import PostingEvent" "🟡 中" "$(cat $TMPDIR/r12b)"
printf "%-6s %-42s %-8s %s\n" "R12c" "共享内核 import AcctSchemaResolver" "🟡 中" "$(cat $TMPDIR/r12c)"
echo ""
echo "检测完成。"
echo "注意: 命中项需人工逐一确认是否为合理偏离（如文档化的 REQUIRES_NEW）。"
