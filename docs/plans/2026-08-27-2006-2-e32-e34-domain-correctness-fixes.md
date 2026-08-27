# 2026-08-27-2006-2-e32-e34-domain-correctness-fixes E3.2 对账键维度 + E3.4 瓶颈窗口上界单点修复

> Plan Status: active（独立草案审查共识：iteration 1 needs-revision → 修订 → iteration 2 accept，task `ses_fbcde0104ffet06VTCUEk1Y5bY` / `ses_fbcd6ee6bffeos3qoxDoF7n5WG`）
> Last Reviewed: 2026-08-27
> Source: `docs/audits/2026-08-26-2226-multi-audit-erp-enhancement.md` P1-4/P1-5（多面审计 needs revision）
> Related: plan `2026-08-26-0735-2`（E3 整体实现，已关闭——本计划修复其遗留 P1）
> Audit: required

## Current Baseline

- **P1-4**：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockLedgerBizModel.java:173-180` `balanceKey` 比对键仅 `{warehouseId,locationId,materialId,skuId,batchNo}` 5 维；同文件派生聚合 `SNAPSHOT_DIMS`（:47-48）与余额表自然键 `UK_INV_STOCK_BALANCE_NATURAL`（`module-inventory/model/app-erp-inventory.orm.xml:415`，= `orgId,materialId,skuId,warehouseId,locationId,batchNo,ownerId`）均为 7 维（含 `orgId`/`ownerId`）。多组织/多货主数据下键碰撞 → 假 `BOOK_ONLY_NO_LEDGER`/`LEDGER_ONLY_NO_BALANCE` 且遮蔽真实差异（`checkStockBalanceConsistency` 在多组织下产出错误结论）。`mismatchRow`（:182-201）输出行同样缺 orgId/ownerId 诊断字段。
- 测试现状：`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvSnapshotAndStockCheck.java` 仅种子 `orgId="1"` 单组织，从未暴露。
- **P1-5**：`module-aps/erp-aps-service/src/main/java/app/erp/aps/service/scheduling/ApsBottleneckDetector.java:123-131` `findPlannedInHorizon` 仅 `ge("plannedEndDateT", horizonStart)`，缺 `le("plannedStartDateT", horizonEnd)`——完全排在 horizon 之后的 PLANNED 工序被计入窗口负荷 → 负荷率虚高 → `scheduleToc` 误报瓶颈中心。同仓窗口重叠范式 `ErpApsSchedulingProcessor.loadPlannedInWindow`（`module-aps/erp-aps-service/.../processor/ErpApsSchedulingProcessor.java:209-210`）`ge(plannedEndDateT, start) + le(plannedStartDateT, end)` 双边界齐全。
- 测试现状：`module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsSchedulingToc.java` 无 beyond-horizon 种子用例。

## Goals

- **对账键对齐自然键 7 维（P1-4）**：`balanceKey` 补 `orgId`/`ownerId`，多组织/多货主场景零假差异、真实差异不被遮蔽；差异输出行携带全维度诊断字段。
- **瓶颈窗口双边界（P1-5）**：`findPlannedInHorizon` 补 `le("plannedStartDateT", horizonEnd)`（对齐 `loadPlannedInWindow` 范式），horizon 之后工序不计入窗口负荷，`scheduleToc` 不再误报虚假瓶颈。

## Non-Goals

- 不改 ORM（键修复在服务层比对逻辑；`SNAPSHOT_DIMS`/UK 自然键已是 7 维，无需动模型）。
- 不动 `getInventorySnapshot` 的 role-row-filter 绕过问题（多面审计 P2-2，归 G1 族收口）与 `TestErpInvSnapshotAndStockCheck` 零值等价断言空转（P2-14）——均已在 roadmap Follow-up Backlog。
- 不动 TOC 阈值等值边界语义（`==` 不触发，P2-14 子项）与 SPI 产能兜底逻辑。
- 多面审计其余 P2 项不在范围。

## Task Route

- Type: `implementation-only change`（已确认实时缺陷修复）
- Owner Docs: `docs/design/inventory/audit-snapshot-cycle-count.md`（§1/§3 快照与一致性校验语义）、`docs/design/aps/constraint-based-planning.md`（TOC 瓶颈识别语义）
- Skill Selection Basis: `nop-backend-dev`（BizModel 查询构造与跨实体读取约定）；`nop-testing`（多组织/越 horizon 种子用例为本计划核心 Proof）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - E3.2 对账键补 orgId/ownerId + 多组织负路径测试

Status: planned
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/ErpInvStockLedgerBizModel.java`、`module-inventory/erp-inv-service/src/test/java/app/erp/inv/service/TestErpInvSnapshotAndStockCheck.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: none

- [ ] `Fix` `balanceKey` 维度补齐为 7 维（`orgId`/`ownerId` 加入比对键，与 `SNAPSHOT_DIMS`/`UK_INV_STOCK_BALANCE_NATURAL` 自然键对齐）；`mismatchRow` 输出行同步携带 `orgId`/`ownerId` 诊断字段——LEDGER_ONLY_NO_BALANCE 分支 `balance=null`，两维须取自派生聚合行（非 balance 行）。Skill: `nop-backend-dev`
- [ ] `Proof` 多组织负路径测试：①双组织同 warehouse/material 的流水+余额（各组织值均正确）→ `consistent=true` 零假差异（旧 5 维键下 `derivedByKey` 同键折叠必然产生假 `BOOK_ONLY_NO_LEDGER`/假 `QTY_OR_COST_MISMATCH`；折叠只丢失 derived 条目，不会产生假 `LEDGER_ONLY`）；②多组织下构造单组织真实差异 → 该差异被检出且 `dimensionKey` 含 org 维；③多货主（ownerId）同型断言。Skill: `nop-testing`

Exit Criteria:

- [ ] `mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvSnapshotAndStockCheck` 绿（含新增多组织/多货主用例）

### Phase 2 - E3.4 瓶颈窗口上界 + beyond-horizon 负路径测试

Status: planned
Targets: `module-aps/erp-aps-service/src/main/java/app/erp/aps/service/scheduling/ApsBottleneckDetector.java`、`module-aps/erp-aps-service/src/test/java/app/erp/aps/service/TestErpApsSchedulingToc.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: none（与 Phase 1 无依赖，可并行）

- [ ] `Fix` `findPlannedInHorizon` 补 `le("plannedStartDateT", horizonEnd)` 上界（对齐 `ErpApsSchedulingProcessor.loadPlannedInWindow:209-210` 窗口重叠范式：工序区间与 horizon 区间相交才计入）。Skill: `nop-backend-dev`
- [ ] `Proof` beyond-horizon 负路径测试：①完全排在 horizon 之后的 PLANNED 工序（`plannedStartDateT > horizonEnd`）不计入负荷、不产生瓶颈判定；②与 horizon 相交（跨边界）工序正常计入；③既有 in-horizon 用例零回归。Skill: `nop-testing`

Exit Criteria:

- [ ] `mvn test -pl module-aps/erp-aps-service -Dtest=TestErpApsSchedulingToc` 绿（含 beyond-horizon 用例）

### Phase 3 - 收口验证与登记

Status: planned
Targets: `docs/logs/2026/`、本计划 Closure
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1、Phase 2

- [ ] `Proof` scoped 复跑：`mvn test -pl module-inventory/erp-inv-service` + `mvn test -pl module-aps/erp-aps-service` 全绿（含既有用例零回归）。Skill: `nop-testing`
- [ ] `Proof` 受影响 E2E 复跑（flux 模式，多面审计复审要求②）：`tests/e2e/dashboards/inv-snapshot.value.spec.ts`（:55-62 断言 `checkStockBalanceConsistency` 结果）+ `tests/e2e/business-actions/aps-schedule-toc.action.spec.ts`（exercise `scheduleToc` 负荷率）绿。Skill: `none`
- [ ] `Proof` compliance checker 复跑：`bash docs/audits/nop-compliance-checker.sh`（预期零新站点、零漂移——两修复均为既有查询/键逻辑内变更；若实测出现新增 daoFor 站点，不得内联上调基线，须开基线裁决 successor——复审要求③）。Skill: `none`
- [ ] `Proof` 日志条目（`docs/logs/` 当日，含验证状态）。Skill: `none`

Exit Criteria:

- [ ] checker actual ≤ baseline
- [ ] 日志条目落盘

## Draft Review Record

- Independent draft review iteration 1: **needs-revision**（task `ses_fbcde0104ffet06VTCUEk1Y5bY`，fresh session）——1 BLOCKER：Closure Gate 引用复审要求②③但缺「受影响 E2E 复跑」义务（`inv-snapshot.value.spec.ts`/`aps-schedule-toc.action.spec.ts` 两 spec 实存）；5 MINOR（LEDGER_ONLY 分支 orgId/ownerId 取值来源、假差异机理措辞修正、owner-doc 对齐门缺失、全量验证落点「亦可」措辞、checker 命令与冗余退出标准）。已修复：Phase 3 补两 spec flux 复跑项 + Closure Gates 同步；Fix 项补 LEDGER_ONLY 两维来源；Proof ①措辞修正；补 owner-doc 对齐门（条件式）；全量 mvn test 落点改定论（mission VERIFY 批）；checker 命令写实；删除两域被 Phase 3 吸收的重复退出标准。
- Independent draft review iteration 2: **accept（3 非阻塞 MINOR 采纳修订）**（task `ses_fbcd6ee6bffeos3qoxDoF7n5WG`，fresh session）——BLOCKER 与 5 MINOR 逐点确认修复（含假差异机理独立代码复证：折叠产生假 BOOK_ONLY/假 QTY_MISMATCH，不产生假 LEDGER_ONLY）；3 MINOR 已修：①checker 项补「新增 daoFor 站点须开裁决 successor 不得内联」应急路径；②多面审计 `Audit Status → closed` 翻转触发权登记至 plan 2006-1 Phase 2（三计划全 completed 后由 mission 审计闭环独立复审执行）；③spec 行号精确化 :55-62。

## Closure Gates

> 完整仓库验证在此处：结束时运行一次全量 `mvn test`——本批（2006-1/2/3）统一归 mission VERIFY 批执行，结审计前完成。

- [ ] 范围内行为完成（P1-4/P1-5 修复且多组织/越 horizon 负路径测试同落——多面审计复审要求①）
- [ ] 相关文档对齐（`audit-snapshot-cycle-count.md`/`constraint-based-planning.md` 实现注记中受影响的证据行——键语义与测试计数——随修复更新，仅当该阶段实际改变其表述）
- [ ] 已运行验证（两域 scoped mvn test + 受影响 E2E 复跑 + compliance checker——复审要求②③）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中（多面审计复审要求④由独立复审/审计闭环执行，执行者只备好证据）

## Deferred But Adjudicated

（无）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（P2 项已归 roadmap Follow-up Backlog，非本计划范围）
