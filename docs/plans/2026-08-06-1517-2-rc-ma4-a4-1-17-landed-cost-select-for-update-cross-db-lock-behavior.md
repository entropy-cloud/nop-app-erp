# 2026-08-06-1517-2 rc-ma4-a4-1-17-landed-cost-select-for-update-cross-db-lock-behavior LandedCost SELECT FOR UPDATE 跨数据库方言锁行为评估

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.17（MA4 运行时行为验证 — A1.5 §7-3：UC-FIN-10 P1-MA2-085 SELECT FOR UPDATE 路径在 H2 内存库（测试环境）外的真实 DB（PG/MySQL）的锁行为评估，`ormTemplate.lock` 跨数据库方言一致性；P1-MA2-085 已 resolved R1.28）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.17；存疑点来源 `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 3
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/plans/2026-08-07-1400-3-rc-ma4-a4-1-15-fifo-landed-cost-delta-layer-consumption-correctness.md`（A4.1.15 done 同切片 A1.5 costing 范畴）、`docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md`（A1.5 报告 §7 存疑点 3 + §6.2 P1-MA2-085 HEAD 复核 resolved[SELECT FOR UPDATE 替代路径]）、`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（A2.4 P1-MA2-085 原始发现 + resolved R1.28）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.17 验证报告（落盘 `docs/audits/2026-08-06-1517-rc-ma4-a4-1-17-landed-cost-select-for-update-cross-db-lock-behavior.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `ErpInvLandedCostProcessor.lockReceiveForAllocation` + `ormTemplate.lock` 平台实现 + PG/MySQL/H2 方言 Dialect 的 SELECT FOR UPDATE 生成 + 既有并发测试覆盖普查）。范式对齐 A4.1.12/A4.1.15（已 done 的只读行为评估同型工作项）。

- **存疑点原文**（A1.5 报告 §7 存疑点 3，`2026-08-02-2045-...-a1-5-costing.md` §7）：「P1-MA2-085 SELECT FOR UPDATE 路径在 H2 内存库（测试环境）外的真实 DB（PG/MySQL）的锁行为——`ormTemplate.lock` 跨数据库方言一致性」。触发条件 = 生产部署。交 A4.1 运行时验证（非本切片阻塞，P1-MA2-085 已 resolved）。

- **关联既有 finding**：
  - **P1-MA2-085**（arm-index，LandedCost TOCTOU + 非唯一索引）：原发现到岸成本并发分摊 TOCTOU（check-then-act 窗口）+ `(receiveId, approveStatus)` 无唯一索引。**resolved R1.28** 经 `lockReceiveForAllocation:388-389` `ormTemplate.lock(receive)` = SELECT FOR UPDATE 串行化并发同 receiveId 审核（非 UK 路径，arm-index 列明的合法修复方案之一）。测试 `TestErpInvLandedCostReceiveMutex` 覆盖（H2 内存库）。本验证只评估 SELECT FOR UPDATE 在真实 DB 的锁行为一致性，不重新核实 TOCTOU 修复本身（A1.5 §6.2 + A2.4 已证 resolved）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:183` UC-FIN-10 隐含到岸成本分摊并发安全（防重复分摊）。L2 `costing-methods.md §到岸成本编排` + `state-machine.md`（到岸成本防重复分摊 `validateNotAlreadyAllocated`）。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - 锁入口：`ErpInvLandedCostProcessor.lockReceiveForAllocation:388-389`（`protected void lockReceiveForAllocation(ErpPurReceive receive) { ormTemplate.lock(receive); }`），在 `approve` 编排中分配前调用（串行化并发同 receiveId 审核）。
  - 平台实现：`ormTemplate.lock(entity)` → nop-entropy `IOrmTemplate.lock` → Hibernate-style `SELECT ... FOR UPDATE`（行级悲观锁）。
  - 方言差异点（本存疑点核心）：①**H2**（测试环境）：支持 `SELECT ... FOR UPDATE` 行级锁；②**PostgreSQL**：支持 `SELECT ... FOR UPDATE` 行级锁（标准）；③**MySQL**：`SELECT ... FOR UPDATE` 行级锁，但 **MVCC 隔离级别下** 行为与 PG 一致（InnoDB）；④**SQL Server / Oracle**（若部署）：各有方言变体（`WITH (UPDLOCK)` / `FOR UPDATE`）。关键差异：某些方言在特定隔离级别下 SELECT FOR UPDATE 行为退化（如 MySQL REPEATABLE READ 下 GAP 锁范围差异）或锁等待超时配置不同。
  - 防重复分摊（锁之后的 check）：`ErpInvLandedCostProcessor.validateNotAlreadyAllocated:392-407`（查同 receiveId + APPROVED sibling，排除自身 → `ERR_LANDED_COST_ALREADY_ALLOCATED`）。锁 + check 的 check-then-act 在锁保护下原子化（同一 receive 行的 SELECT FOR UPDATE 串行化两个并发事务的 check）。

- **既有证据（复用输入）**：
  - A2.4 P1-MA2-085 resolved R1.28：`lockReceiveForAllocation` SELECT FOR UPDATE 路径在 H2 测试环境行为已证实（`TestErpInvLandedCostReceiveMutex` 覆盖）。本验证只补「H2 外真实 DB 锁行为一致性」差异。
  - A1.5 §6.2 P1-MA2-085 HEAD 复核：resolved（SELECT FOR UPDATE 替代路径，非 UK 路径），维持 resolved。

- **剩余差距**：P1-MA2-085 的 SELECT FOR UPDATE 路径在真实 DB（PG/MySQL）的锁行为未验证——`ormTemplate.lock` 跨方言一致性 + 生产隔离级别下的锁行为（行锁 vs GAP 锁范围）+ 锁等待超时配置。本验证评估该跨方言锁行为风险面。

- **保护区域**：只读评估（读锁代码路径 + 平台 Dialect 方言生成逻辑 + 既有并发测试普查 + 跨方言锁行为推理），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（P1-MA2-085 已 resolved；若发现跨方言锁退化，登记 finding 归 MR1，触及锁逻辑须 ask-first）。

## Goals

- SELECT FOR UPDATE 跨方言锁行为评估：核验 `lockReceiveForAllocation:388-389` → `ormTemplate.lock` → SELECT FOR UPDATE 在 H2（测试）/ PG / MySQL 的行级锁行为一致性——是否所有目标方言都生成有效行级悲观锁 + 是否有方言在特定隔离级别下锁退化（如 MySQL REPEATABLE READ GAP 锁范围扩大 vs PG 行锁精确）+ 锁等待超时配置跨方言差异。
- 锁保护下的 check-then-act 原子性评估：核验 `lockReceiveForAllocation`（锁 receive 行）+ `validateNotAlreadyAllocated`（check APPROVED sibling）在 SELECT FOR UPDATE 保护下的原子性——同一 receive 行的悲观锁是否有效串行化两个并发事务的 check（防 check-then-act 窗口），跨方言是否一致。
- 既有测试覆盖边界普查：grep `TestErpInvLandedCostReceiveMutex`（H2 并发测试）+ nop-entropy Dialect/`IOrmTemplate.lock` 平台文档/测试，确认跨方言锁行为的测试/文档覆盖边界（H2 覆盖 + 真实 DB 覆盖缺口）。
- 对齐 UC-FIN-10 防重复分摊契约 + P1-MA2-085 resolved 状态给出结论：确认/调整 P1-MA2-085 resolved 结论——①若 SELECT FOR UPDATE 跨方言一致（行级锁有效 + check 原子性保持）→ P1-MA2-085 维持 resolved（跨方言无退化）；②若发现某方言锁退化（如 MySQL GAP 锁范围扩大致死锁，或某方言 SELECT FOR UPDATE 在特定隔离级别下无效）→ 登记新 finding（watch-only 或 MR1，触及锁逻辑）。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 P1-MA2-085**（已 resolved R1.28；若发现跨方言退化，登记 finding 归 MR1）。
- **不部署真实 PG/MySQL 跑并发测试**（本验证为只读跨方言锁行为推理 + 平台 Dialect 文档/代码路径分析；真实 DB 并发测试属 MR1 修复验证范围，非本验证范围）。
- **不修改代码/ORM/api.xml/真相源**（只读评估）。
- **不重新核实 P1-MA2-085 TOCTOU 修复本身**（A1.5 §6.2 + A2.4 已证 resolved；本验证只评跨方言锁行为差异）。
- **不展开 A1.5 §7-1/§7-2**（A4.1.15 forward 消耗 done / A4.1.16 reverse 删除余额守恒范围）。

## Task Route

- Type: `verification or audit work`（跨方言锁行为评估 + P1-MA2-085 resolved 结论确认/调整）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.17 行）+ `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` §7 存疑点 3 + §6.2 P1-MA2-085 HEAD 复核（输入）+ nop-entropy 平台 Dialect/`IOrmTemplate.lock` 文档（`../nop-entropy/docs-for-ai/`）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。跨方言锁行为评估需多维度归类（SELECT FOR UPDATE 方言生成 / 隔离级别交互 / check-then-act 原子性 / 锁等待超时 / 既有测试覆盖边界 / resolved 结论确认-or-新 finding）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读锁代码路径 + 平台 Dialect 方言生成逻辑 + 既有并发测试普查 + 跨方言锁行为推理）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - SELECT FOR UPDATE 跨方言锁行为与 check 原子性评估

Status: planned
Targets: `docs/audits/2026-08-06-1517-rc-ma4-a4-1-17-landed-cost-select-for-update-cross-db-lock-behavior.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.17 行）；A1.5 done（§7 存疑点 3 已落盘 + §6.2 P1-MA2-085 HEAD 复核 resolved）

- [ ] `Proof` 锁入口与平台实现核验：给出 `ErpInvLandedCostProcessor.lockReceiveForAllocation:388-389`（`ormTemplate.lock(receive)`）证据（file:line）+ 追踪 `IOrmTemplate.lock` → nop-entropy/Hibernate Dialect SELECT FOR UPDATE 生成路径（H2Dialect/PostgreSQLDialect/MySQLDialect 的 `FOR UPDATE` SQL 生成差异）。证实锁机制 = SELECT ... FOR UPDATE 行级悲观锁。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 跨方言行级锁一致性评估：评估 SELECT FOR UPDATE 在 H2（测试）/ PG / MySQL 的行级锁行为——①是否所有目标方言都生成有效行级悲观锁；②隔离级别交互（MySQL InnoDB REPEATABLE READ 默认下 FOR UPDATE 是否触发 GAP 锁/Next-Key 锁范围扩大 → 潜在死锁面 vs PG 行锁精确）；③锁等待超时配置跨方言差异（`innodb_lock_wait_timeout` vs PG `lock_timeout`/`statement_timeout`）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 锁保护下 check-then-act 原子性评估：核验 `lockReceiveForAllocation`（锁 receive 行）+ `validateNotAlreadyAllocated:392-407`（check APPROVED sibling）在 SELECT FOR UPDATE 保护下的原子性——同一 receive 行悲观锁是否有效串行化两个并发事务的 check（事务 A 锁 receive → check 无 APPROVED sibling → 创建 → commit；事务 B 阻塞在锁 → A commit 后 B 获锁 → check 发现 A 创建的 APPROVED sibling → ERR_LANDED_COST_ALREADY_ALLOCATED）。跨方言是否一致（行锁精确性决定 check 原子性）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 既有测试覆盖边界普查：grep `TestErpInvLandedCostReceiveMutex`（H2 并发测试覆盖 check-then-act 原子性）+ nop-entropy Dialect/`IOrmTemplate.lock` 平台文档/测试（跨方言锁行为覆盖）全集，产出测试覆盖边界清单 + 标注真实 DB（PG/MySQL）并发测试缺口。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（SELECT FOR UPDATE 跨方言锁行为是否一致），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` P1-MA2-085 resolved 结论确认/调整（方法论 §2 判据 + 三源对照）：①若 SELECT FOR UPDATE 跨方言一致（行级锁有效 + check 原子性保持）→ P1-MA2-085 维持 resolved（跨方言无退化）；②若发现某方言锁退化 → 登记 finding（watch-only 或按 §2 判据升 P1，触及锁逻辑归 MR1）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.5 §6.2 P1-MA2-085 resolved 结论分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 锁入口 + 平台实现 + 跨方言行级锁一致性 + check 原子性 + 测试覆盖边界证据落盘（全集，无遗漏），每条有证据（file:line）
- [ ] P1-MA2-085 resolved 结论确认/调整有明确结论（维持 resolved 或登记新 finding），与 A1.5 §6.2 分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-06-1517-rc-ma4-a4-1-17-landed-cost-select-for-update-cross-db-lock-behavior.md`（定稿）；`docs/audits/arm-index.md`（P1-MA2-085 注记或新 finding，若有）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 跨方言锁行为评估 + 结论确认完成

- [ ] `Add` finding 注记更新：若维持 resolved → 在 arm-index P1-MA2-085 行追加「A4.1.17 跨方言锁行为评估确认 resolved 维持（SELECT FOR UPDATE 跨 H2/PG/MySQL 行级锁一致）」注记；若发现方言退化 → 登记新 finding（P2 watch-only 或 P1 MR1）。禁止未经比对新建重复 finding（P1-MA2-085 已登记，本验证只确认 or 增量登记）。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.5 §6.2 P1-MA2-085 / A2.4 costing 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（锁入口 + 平台实现 + 跨方言锁行为 + check 原子性 + 测试覆盖边界 + 结论确认 + finding 衔接 + §8 自检齐全）
- [ ] P1-MA2-085 注记或新 finding 已登记（若有变更）或有明确「维持 resolved 无变更」记录并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver 2026-08-04-224309-mission-driver 独立子代理 ses_02a0b1b75ffeTb53JeABycNrT2，新会话不重用执行者上下文) — 全 checklist 通过：live baseline file:line 精确核验（lockReceiveForAllocation:388-389 ormTemplate.lock(receive) / validateNotAlreadyAllocated:392-407 / ApproveProcessor:52→:53 lock→check 时序 / TestErpInvLandedCostReceiveMutex 存在）零漂移；格式合规；单一结果表面（一报告 + 条件性 arm-index 注记）；anti-slack 零命中（rg 验证）；item typing 合规；Deps 门控满足（A4.1 expander done + A1.5 done）；保护区域纪律（只读不改锁逻辑 + Non-Goals 显式排除真实 PG/MySQL 部署）；逻辑健全（per-Dialect FOR UPDATE SQL 生成静态可读 + MySQL GAP/Next-Key vs PG 行锁 + innodb_lock_wait_timeout vs lock_timeout + check-then-act 原子性推理，经验性真实 DB 测试诚实归 MR1）；Closure Gates 删除全仓 typecheck/build（只读）对齐 guide 模板。无 Blocker/Major。2 Minors（expander 交叉引用完整性 nit + use-cases.md:183 推断非逐字）非阻塞。promote to active。

## Closure Gates

> 本计划为**只读跨方言锁行为评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 锁入口 + 平台实现 + 跨方言锁行为 + check 原子性 + 测试覆盖边界 + 结论确认 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.17 验证报告锁入口 + 平台实现 + 跨方言锁行为 + check 原子性 + 测试覆盖边界 + 结论确认齐全 + P1-MA2-085 注记或新 finding（若有）
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §去重协议一致；与 A1.5 §7-3 + §6.2 P1-MA2-085 resolved 一致
- [ ] 已运行验证：锁入口 + 跨方言锁行为 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（若新 finding 是验证**输出**，非范围内项目降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA2-085 跨方言锁退化（若 A4.1.17 发现某方言退化后的修复归口）

- Classification: `out-of-scope improvement`（本验证是跨方言锁行为评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是锁行为评估，结果表面 = 验证报告 + P1-MA2-085 resolved 结论确认。若发现跨方言退化，修复（触及锁逻辑/隔离级别配置）归 MR1，须 ask-first。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告修复方向[若发现退化]展开）

## Closure

Status Note: <待执行完毕后填充>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理新会话，待填充>
- Evidence: <待填充>

Follow-up:

- MR1 修复 P1-MA2-085 跨方言退化（若发现）：触及锁逻辑须 ask-first + 独立 plan-audit
