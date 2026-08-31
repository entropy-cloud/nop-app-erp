# 2026-08-28-2054-1 entity-state-machine M5.3 最终跨域回归与 closure audit

> Plan Status: draft
> Last Reviewed: 2026-08-28
> Source: `entity-state-machine-migration-roadmap.md` M5.3（最终跨域回归、Delta 覆盖回归、owner doc 对齐及独立 closure audit，todo 状态）
> Related: `2026-08-28-1607-1`（M5.1 工具落地）、`2026-08-28-1607-2`（M5.2 wrapper 落地）、`docs/architecture/state-machine-matrix.md` §9（M5.3 closure audit 6 项 CG）
> Audit: required

## Purpose

执行 entity-state-machine-migration mission 的**最终收官里程碑 M5.3**：跑 §9 的 6 项 closure audit CG，由独立子代理用 `closure-audit-prompt.md` 验证。若 6 项全过，M5.3 → done，entity-state-machine-migration mission closure audit PASS，roadmap 全部工作项 done，**mission 完结**。

## Current Baseline（live 状态，2026-08-28-2054 核实）

- **M5.1 done**（commit `1f62edfd3`）：plan + 工具 `state-machine-coverage-check.py`（252 行 Python，4 维度对账 + writer 索引 + 白名单） + 报告 `state-machine-matrix-audit.md`（116 行 D1 报告） + 维护入口 `docs/architecture/state-machine-matrix.md`（281 行 D3 报告）
- **M5.2 done**（commit `87c4f5364`）：wrapper `tools/check-state-machine-coverage.sh`（91 行 Bash）+ manual + strict 双入口 + 多次执行隔离目录 + LATEST 链接 + scripts/README.md + 矩阵文档强化 §7-§9
- **M5.3 todo**：本 plan 启动执行
- **状态机 Bean 全部覆盖**：105 个 Erp*StateMachine.java（M2/M3/M4 共 65+ plan 落地）
- **基线**：2026-08-28 全量绿 3947/0/0/1/669 + compliance 19 规则 zero 漂移

## Goals

- 跑 §9 全部 6 项 CG（CG1-CG6）并产出 closure audit evidence
- 验证 M5.1 + M5.2 全部 done 后的状态机 Bean 矩阵终态
- 独立子代理 closure-audit-prompt 复审通过
- M5.3 → done，entity-state-machine-migration mission 收官

## Non-Goals

- 不重做 M5.1 / M5.2 工作
- 不修改任何 StateMachine Bean（仅验证）
- 不展开新状态轴迁移（mission 已 done）
- 不修改平台 nop-entropy 代码（保护区域）

## Task Route

- Type: `verification or audit work`（仅验证 + 审计）
- Owner Docs: `docs/architecture/state-machine-matrix.md`（§9 6 项 CG）、`docs/audits/state-machine-matrix-audit.md`（M5.1 报告）、`docs/audits/check/2026-08-28-1620-entity-state-machine-m5-2/audit.md`（M5.2 报告）
- Skill: `closure-audit-prompt.md`（独立子代理跑）
- 保护区域：n/a（仅验证，不动代码）

## Infrastructure And Config Prereqs

- 无外部依赖
- 既有工具 `bash tools/check-state-machine-coverage.sh` 须可直接运行
- 既有 wrapper strict mode 验证（5 stub finding 触发 + exit 1）需在 CG2 重现

## Execution Plan

### Phase 0 — 收官前基线快照

Status: planned
Targets: `docs/testing/known-good-baselines.md`（引用 2026-08-28 3947/669）
Skill: none

- Item Types: `Proof`
- Prereqs: 无
- 跑测试估算：~30 秒（compliance checker 不重跑 mvn install）

- [ ] 引用 2026-08-28 全量绿基线（3947/669/0/0/1）作为 M5.3 收官锚定行
- [ ] `git status` 确认 M5.3 收官期间无未提交代码改动
- [ ] `bash docs/audits/nop-compliance-checker.sh` 跑 19 规则 → actual ≤ baseline 零漂移
- [ ] **当日最新 baseline 行复核**（M5.3 收官时点若有同日期后新增行，须引用最新行；本日已有 4 个 erp-enhancement plan 收口批——M5.3 应取累计最大值）

Exit Criteria:
- [ ] 既有 3947/669 基线行被引用（**不重跑 mvn install/test**——避免与权威基线行覆盖冲突）
- [ ] git status 零代码改动
- [ ] compliance checker 19 规则零漂移

### Phase 1 — CG1 全域矩阵终态复核（M5.1 工具 strict 模式）

Status: planned
Targets: `tools/check-state-machine-coverage.sh --strict`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 0
- 跑测试估算：~2 秒（工具已存在）

- [ ] 跑 `bash tools/check-state-machine-coverage.sh --strict` → exit 0
- [ ] 确认 105 Bean / 0 finding 维持（M5.1 锚定）
- [ ] 报告落 `docs/audits/check/2026-08-28-2054-entity-state-machine-m5-3/audit.md`
- [ ] 跨轮聚合：`docs/architecture/state-machine-matrix.md` §4 白名单表 0 增长

Exit Criteria:
- [ ] `bash tools/check-state-machine-coverage.sh --strict` exit 0
- [ ] 报告落盘
- [ ] 4 维度对账全通过

### Phase 2 — CG2 stub 场景下 finding 检测能力

Status: planned
Targets: `docs/audits/scripts/state-machine-coverage-check.py`（通过 stub Bean 复现 5 finding）
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 1
- 跑测试估算：~10 分钟（5 stub × 工具跑 × 复跑）

- [ ] 创建 5 个 stub Bean（`/tmp/M5_3StubBean*.java`）——故意引入 5 finding 模式（ORPHAN_DICT_VALUE / UNREACHABLE_STATE / TERMINAL_OUT_EDGE / DUPLICATE_EDGE / NO_WRITER）
- [ ] 跑工具（带 stub 路径或临时指定）→ 检测到 ≥5 finding + exit 1
- [ ] 删除 stub Bean → 跑工具 → 0 finding + exit 0
- [ ] 复现证据落入报告

Exit Criteria:
- [ ] stub 场景下工具检测 5 finding + exit 1
- [ ] 恢复后工具 0 finding + exit 0
- [ ] 复现证据落盘（脚本命令 + 输出日志）

### Phase 3 — CG3 多次执行隔离目录

Status: planned
Targets: `docs/audits/check/<TS>-entity-state-machine-m5-2/`（M5.2 多次执行隔离纪律）
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1
- 跑测试估算：~4 秒

- [ ] 跑 `bash tools/check-state-machine-coverage.sh` 两次（不同时间戳）
- [ ] 验证 `docs/audits/check/` 下生成 2 个独立子目录（`<TS1>-entity-state-machine-m5-2/` + `<TS2>-entity-state-machine-m5-2/`）
- [ ] 验证 LATEST 链接指向最新一份
- [ ] 验证历史子目录只读保留（不被覆盖）

Exit Criteria:
- [ ] 2 个独立子目录生成
- [ ] LATEST 链接正确
- [ ] 历史子目录未受影响

### Phase 4 — CG4 owner doc 9 章节齐全

Status: planned
Targets: `docs/architecture/state-machine-matrix.md`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1
- 跑测试估算：~30 秒

- [ ] 验证 §1 审计方法学 / §2 工具使用 / §3 维护义务 / §4 白名单 / §5 工具开发约定 / §6 关联文档 / §7 M5.2 守卫层 / §8 误报裁决 / §9 closure audit checklist 9 章节齐全
- [ ] 9 章节标题 grep 验证
- [ ] 0 章节为空（`grep -E '^## '` + 检查每章节 `wc -l` ≥ 5 行）

Exit Criteria:
- [ ] 9 章节标题齐
- [ ] 每章节非空

### Phase 5 — CG5 全量构建 + compliance 零漂移

Status: planned
Targets: `mvn clean install -DskipTests` + `docs/audits/nop-compliance-checker.sh`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 0（基线锚定）
- 跑测试估算：~30 秒（不重跑 mvn install）

- [ ] **不重跑 mvn install**（基线已锚定 3947/669，零代码改动无需重跑）
- [ ] 跑 `bash docs/audits/nop-compliance-checker.sh` → 19 规则 actual ≤ baseline
- [ ] 引用 known-good-baselines.md 2026-08-28 行作为权威锚定

Exit Criteria:
- [ ] compliance checker exit 0 + 19 规则全 actual ≤ baseline

### Phase 6 — CG6 独立子代理 closure audit

Status: planned
Targets: `closure-audit-prompt.md` 跑 6 项 CG 复审
Skill: `closure-audit-prompt`

- Item Types: `Proof | Add`
- Prereqs: Phase 1-5 全 done
- 跑测试估算：~30 分钟（独立子代理全 CG 复审 + 决议落盘）

- [ ] 派发独立子代理（fresh session）用 `closure-audit-prompt.md` 复审本 plan
- [ ] 子代理返回 ACCEPT/REJECT 决议
- [ ] 若 ACCEPT：M5.3 → done，mission 完结
- [ ] 若 REJECT：按子代理发现项修复并重跑
- [ ] **失败回退**：若独立子代理通道不可用，**plan 须显式登记 successor 触发条件 = 子代理通道恢复 + 人工裁决**（**当前会话已知子代理通道结构性不可用**——此 plan 须 Pending Successor 状态保留至下会话）

Exit Criteria:
- [ ] 独立子代理 closure-audit-prompt 复审通过（ACCEPT）
- [ ] 决议落 `docs/audits/check/2026-08-28-2054-entity-state-machine-m5-3/closure-audit.md`
- [ ] 失败回退：若不可用，落 Pending Successor 决策记录

## Draft Review Record

- Independent draft review iteration 1: pending

## Closure Gates

- [ ] Phase 0-5 全部 done
- [ ] Phase 6 独立子代理 closure-audit-prompt ACCEPT
- [ ] `docs/audits/check/2026-08-28-2054-entity-state-machine-m5-3/audit.md` 落盘（6 项 CG 全部 evidence）
- [ ] `docs/audits/check/2026-08-28-2054-entity-state-machine-m5-3/closure-audit.md` 落盘（独立子代理决议）
- [ ] `docs/backlog/entity-state-machine-migration-roadmap.md` M5.3 状态 todo → done
- [ ] `docs/logs/2026/08-28.md` 追加 M5.3 done 日志
- [ ] `docs/architecture/state-machine-matrix.md` §9 6 项 CG 全 [x] 勾选
- [ ] `docs/audits/check/ai-check-r2-index.md` 跨轮聚合适当更新（如 ai-check-r2 阶段到达 V.2）

## Deferred But Adjudicated

### M5.x 之后的 entity-state-machine 后继 mission

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: entity-state-machine-migration 完结后，状态机 Bean 已 105 全面覆盖；后继 mission（如新增状态轴、Delta 重新优化、跨域状态机协奏）按业务需要时启动
- Successor Required: `no`（按业务触发）

## Closure

Status Note: 6 项 CG 全部 evidence 落盘 + 独立子代理 closure-audit-prompt ACCEPT → M5.3 → done，entity-state-machine-migration mission 完结，roadmap 全部工作项 done。本 plan 必须在 Phase 6 独立子代理决议后实施 closure 段撰写。

Closure Audit Evidence:

- Reviewer / Agent: pending（Phase 6 独立子代理）
- Evidence: pending
