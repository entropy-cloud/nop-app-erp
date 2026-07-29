# 2026-07-29-2225-1 跨域 daoFor 治理统一裁决（读侧 Decision + 写侧豁免补登）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MR1 工作项 R1.5 + R1.7（MA1 结构审计 + MA4 代码质量审计 P1 findings）
> Related: `docs/plans/00-plan-authoring-and-execution-guide.md`；`docs/analysis/governed-path-cost-evaluation.md`；`docs/audits/arm-index.md`（P1-MA1-016/022/029 + P1-MA2-038 + P1-MA4-003/006/008/012/015/022）
> Audit: required

## Current Baseline

跨域 `IDaoProvider.daoFor(Other*)` 直访是全域 9+ 域的系统性架构模式。MA1（A1.10 DAG / A1.14 架构治理复审）+ MA4（A4.1a-A4.5 代码质量）共登记 10 项 P1 finding（8 读侧 + 2 写侧半治理），全部指向同一根因：跨域读/写未经 I*Biz 接口。

**既有一致性证据**（`docs/analysis/governed-path-cost-evaluation.md`，2026-07-24 实测）：
- Governed path 成本裁决 = 分支 (b)：I*Biz 强注入**会**破坏单模块测试启动（contract/aps 两域实测零跨域 I*Biz 注入 → 测试全绿；反事实推理：注入则级联依赖链致 NoSuchBeanException）。
- Type 1（ORM 导航可替代）：~37 处 chained/variable-split 已重构清零；`findAllByQuery` 子集 <10 处可机械替换 → watch-only residual（触发条件未满足）。
- Type 4（设计边界跨域写/读聚合）：~10-30 处 → 阻塞中，需平台 lazy/SPI 解耦或登记豁免。
- 既已登记写侧豁免 3 条：`MrpReleaseService`(mfg→pur) / `ErpCtRebateSettlementBizModel`(ct→pur/sal) / `ErpB2bAsnBizModel`(b2b→pur)，均在 `posting-exemptions.md`。

**读侧 8 项 P1 finding 明细**（全部经 MA2 状态机运行时复核确认「仅治理缺陷，不破坏运行时正确性」）：

| Finding ID | 域方向 | 典型站点 | MA 维度 |
|------------|--------|----------|---------|
| P1-MA1-016 | finance→assets | `ErpFinAccountingPeriodProcessor.reverseDepreciation` `daoFor(ErpAstDepreciationSchedule).findAllByQuery`（行号在不同审计中记为 :385/:389，执行时以实仓 grep 为准） | A1.10 DAG |
| P1-MA1-022 | 9 域→md/fin/inv/mfg | pur/sal/ast/inv/mnt/prj/qa/drp/aps 各 posting dispatcher + cost resolver + report facade | A1.11-A1.14 平台合规 |
| P1-MA4-003 | finance→md | 过账链路投影（VoucherFact ErpMdSubject 解析等 6 站点） | A4.1a 代码质量 |
| P1-MA4-006 | finance→md/pur/sal/ast | 预算/AR-AP/成本/期间 helpers（含 `DualSideConsistencyChecker:133,141` finance→pur/sal 新方向）+ `ErpFinAccountingPeriodProcessor` finance→ast（= P1-MA1-016 站点） | A4.1b 代码质量 |
| P1-MA4-008 | mfg→inv/md | 工单/BOM 链路 5 站点 posting dispatcher | A4.2a 代码质量 |
| P1-MA4-012 | mfg→inv/md/sal | MRP/成本/基因/委外跨域 daoFor 投影（含 `SubcontractPostingDispatcher`/`DemandAggregator` daoFor ErpSal*） | A4.2b 代码质量 |
| P1-MA4-015 | assets→fin/md | 折旧 Processor 链路（`ErpAstDepreciationScheduleProcessor:290` ErpFinAccountingPeriod + 9 dispatcher ErpMdSubject） | A4.3 代码质量 |
| P1-MA4-022 | pur/sal/inv→md/fin | pur/sal/inv 跨域 daoFor 投影 | A4.5 代码质量 |

**写侧 2 项 P1 finding 明细**（跨域写半治理——有 javadoc bypass rationale 但未登记 posting-exemptions.md）：

| Finding ID | 域方向 | 站点 | 现状 |
|------------|--------|------|------|
| P1-MA1-029 | contract→pur/sal | `ErpCtInvoicePlanBizModel:159,196` `daoFor(ErpInvoiceLine).saveEntity` 跨域写发票计划草稿 | javadoc rationale 存在但 posting-exemptions.md 未收录 |
| P1-MA2-038 | mfg→mfg（同域写绕审批） | `MrpReleaseService` 委外单创建 `ErpMfgSubcontractOrder`（同属 manufacturing 域，绕审批管道 O-4） | 需核实是否已被既有 MrpReleaseService 豁免条目（覆盖 mfg→pur `ErpPurOrder`）覆盖——P1-MA2-038 的 `ErpMfgSubcontractOrder` 是**同域**目标，与既有豁免的跨域 `ErpPurOrder` 不同 |

**剩余差距**：8 读侧 finding 无统一裁决（方案 A 迁移 vs 方案 B 登记豁免）；2 写侧 finding 未登记 posting-exemptions.md。`mvn clean install -DskipTests` 全绿基线（154 模块）。

## Goals

- R1.5：对 8 项读侧 daoFor P1 finding 做统一裁决——分类为「可迁移至 I*Biz」（md 目标域，service jar 已在全域 classpath）vs「登记为永久只读豁免」（fin/inv/mfg 目标域，受 governed-path 成本阻塞），并落地裁决产物。**裁决落地仅含文档登记**；若裁决认为 md 目标域迁移可行，仅命名 successor 计划条件，不在本 plan 内执行代码迁移。
- R1.7：将 2 项写侧半治理 finding 登记到 `posting-exemptions.md`（含位置/触发场景/理由/风险/补偿机制/收敛条件）。
- 回填 `docs/audits/arm-index.md` 中全部 10 项 finding 的状态。

## Non-Goals

- 不做 Type 1 `findAllByQuery` 机械重构（governed-path eval §3.6 已裁决 watch-only residual，触发条件未满足）。
- 不做 Type 4 跨域写迁移至 I*Biz（受平台 lazy/SPI 解耦阻塞，governed-path eval §3.1 裁决分支 b）。
- 不修改 `posting-exemptions.md` 既有 3 条豁免条目（MrpReleaseService/ErpCtRebateSettlementBizModel/ErpB2bAsnBizModel）。
- 不改业务逻辑、状态机行为、BizModel 方法签名（读侧 finding 全部经 MA2 复核确认无运行时正确性影响）。

## Task Route

- Type: `architecture change`（跨域访问治理策略裁决 + 豁免登记）
- Owner Docs: `docs/architecture/posting-exemptions.md` + `docs/architecture/data-dependency-matrix.md` + `docs/analysis/governed-path-cost-evaluation.md`
- Skill Selection Basis: `nop-backend-dev`（跨实体访问规则裁决）；`none`（本计划不改 BizModel 方法，但需理解 I*Biz 跨实体规则）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- governed-path eval 已提供单模块测试成本实测证据，无需重复探针。

## Execution Plan

### Phase 1 - 读侧 daoFor 统一裁决

Status: completed
Targets: `docs/architecture/data-dependency-matrix.md`（新增「跨域只读访问裁决」段）+ `docs/audits/arm-index.md`
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Proof`
- Prereqs: governed-path-cost-evaluation.md 已存在（§3.1 裁决分支 b + §3.2 分类表）

- [x] Explore: 枚举 8 项读侧 finding 的全部生产站点，按「目标域」分类统计（md/fin/inv/mfg/sal 各多少处），核实 governed-path eval §2.2 Maven 依赖结构表是否仍准确（md-service 是否全域已依赖）
      - Skill: `nop-backend-dev`
      - Evidence: 实仓 grep 核实——md-service 在 15 个业务域为 compile-scope，仅 finance-service 与 logistics-service 为 test-scope（pom.xml 实测：`module-finance/erp-fin-service/pom.xml:45` + `module-logistics/erp-log-service/pom.xml:41`）。8 项 finding 站点按目标域分类统计见 `data-dependency-matrix.md §9.2`。
- [x] Decision: 读侧 daoFor 分类裁决——
  - 目标域为 master-data 的跨域只读站点：裁决是否可迁移至 `IErpMd*Biz` 便捷只读方法（md-service 已在全域 classpath，governed-path 成本不适用）。裁决仅产出 successor 计划条件（站点数 + 估算工作量），**不在本 plan 内执行迁移代码**。
  - 目标域为 finance/inventory/manufacturing/sales 的跨域只读站点：按 governed-path eval §3.1 裁决分支 b，登记为永久只读豁免（受平台 lazy/SPI 解耦阻塞），命名 successor 触发条件。
  - Dashboard/Report facade read-only 聚合：维持既有「永久接受」裁决（无需额外登记）。
  - **裁决落地位置**：`data-dependency-matrix.md` 新增「跨域只读访问裁决」段（posting-exemptions.md 保持纯写侧豁免范围，不扩展）。
  - 记录选择、替代方案、残留风险。
  - Skill: `nop-backend-dev`
  - Evidence: `data-dependency-matrix.md §9.1-9.4` 落地——§9.1 裁决原则（md=可迁移 / fin·inv·mfg=永久豁免）+ md-service classpath 实测校正 + §9.2 八项 finding 明细表 + §9.3 汇总（可迁移 ~65+ compile-scope + ~26 test-scope前置 / 永久豁免 ~20）+ §9.4 选择记录与残留风险。
- [x] Proof: 验证裁决产物完整性——读侧裁决段覆盖全部 8 项 finding 的分类（每项标明目标域 / 处数 / 裁决 / successor 条件）。本 plan 无代码变更，无需跑单模块测试（governed-path eval §2.1 既有证据已覆盖迁移成本结论）。
      - Skill: `none`
      - Evidence: `data-dependency-matrix.md §9.2` 表格含 8 项 finding（P1-MA1-016/022 + P1-MA4-003/006/008/012/015/022）逐项分类（方向/目标域/处数/裁决/successor 条件），覆盖完整。

Exit Criteria:

- [x] `data-dependency-matrix.md` 新增「跨域只读访问裁决」段，含全部 8 项 finding 的分类（目标域 / 处数 / 裁决[迁移-successor-named or 豁免] / successor 条件）

### Phase 2 - 写侧豁免补登 + 索引回填

Status: completed
Targets: `docs/architecture/posting-exemptions.md` + `docs/audits/arm-index.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Explore | Fix`
- Prereqs: Phase 1 裁决已落地（读侧裁决段已建立，写侧补登可复用格式）

- [x] Add: 在 `posting-exemptions.md` 补登 P1-MA1-029（`ErpCtInvoicePlanBizModel` contract→pur/sal 跨域写），格式对齐既有 3 条豁免条目（位置/触发场景/理由/风险/补偿机制/收敛条件——待 pur/sal 提供 `createFromInvoicePlan` I*Biz）
      - Skill: `nop-backend-dev`
      - Evidence: `posting-exemptions.md §ErpCtInvoicePlanBizModel` 新增——位置 `ErpCtInvoicePlanBizModel:127,147,159,164,182,184,196` + 触发 `triggerInvoice`/`triggerDuePlans` + config-gated `erp-ct.invoiceplan-auto-trigger` + 理由（javadoc :41-45 governed-path branch b）+ 风险 + 补偿（DRAFT/UNSUBMITTED 不自动过账 + 合同 ACTIVE 守卫）+ 收敛条件。
- [x] Explore | Add: 核实 P1-MA2-038（`MrpReleaseService` 创建 `ErpMfgSubcontractOrder` 同域写绕审批 O-4）的覆盖性——
  - 既有 MrpReleaseService 豁免条目覆盖的是 mfg→pur `ErpPurOrder`（跨域写）；P1-MA2-038 的 `ErpMfgSubcontractOrder` 是**同域**目标（mfg→mfg），属不同写入路径。
  - 若已覆盖（既有条目范围含同域 subcontract）：在 arm-index 注记「P1-MA2-038 已由既有 MrpReleaseService 豁免覆盖」。
  - 若未覆盖：在 posting-exemptions.md 补登同域写豁免条目（或扩展既有条目范围声明）。
  - Skill: `nop-backend-dev`
  - Evidence: 经实仓核实（`MrpReleaseService.java:185-216` `releaseToSubcontractOrder` 直接创建 `ErpMfgSubcontractOrder` 为 APPROVED 终态 + javadoc :202 已声明 O-4 豁免），既有条目原仅覆盖 mfg→pur `ErpPurOrder`，**未覆盖**同域委外写。裁决=扩展既有 MrpReleaseService 条目范围声明（header 改为 `manufacturing → purchase [跨域写] + manufacturing → manufacturing [同域写绕审批 O-4]`，新增同域委外写入目标 + 风险 + 补偿[postedStatus=DRAFT 须经 SubcontractPostingDispatcher 过账] + 收敛条件 + P1-MA2-038 覆盖性裁决注记）。
- [x] Fix: 回填 `docs/audits/arm-index.md` 中全部 10 项 finding（P1-MA1-016/022/029 + P1-MA2-038 + P1-MA4-003/006/008/012/015/022）的状态为 done/resolved，交叉引用本 plan。
      - Skill: `none`
      - Evidence: arm-index.md `### P1 详细清单` 表格 10 项 finding 的「修复状态」列全部从 `MR1 todo (R1.x)` 改为 `✅ resolved (plan 2026-07-29-2225-1: ...)`（读侧 8 项标注 `data-dependency-matrix.md §9` 裁决位置 + 写侧 2 项标注 `posting-exemptions.md` 豁免位置）。验证：`rg "MR1 todo (R1.5)|MR1 todo (R1.7)"` 对 10 项 finding 零剩余；`rg "✅ resolved (plan 2026-07-29-2225-1"` 计数=10。

Exit Criteria:

- [x] `posting-exemptions.md` 含 P1-MA1-029 豁免条目（格式与既有条目一致）
- [x] P1-MA2-038 覆盖性已裁决（扩展既有 MrpReleaseService 条目范围声明覆盖同域委外写）
- [x] arm-index 10 项 finding 状态回填完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_051b8f106ffeT0NCTB38bp0pnC) — 3 factual errors in finding direction columns (P1-MA4-012 fin→sal / P1-MA2-038 mfg→pur 应为 mfg→mfg 同域 / P1-MA4-003 md/ast 应为 md) + md-target 迁移执行范围模糊 + Phase 1 缺 Explore 类型标签 + 裁决落地位置"或"歧义。已全部修订：方向列修正、md 迁移执行明确移出范围（仅命名 successor）、Phase 1 加 Explore 标签、裁决落地位置确定为 data-dependency-matrix.md 新增段（posting-exemptions.md 不扩展范围）、P1-MA2-038 改 Explore|Add 标签、行号歧义注记。
- Independent draft review iteration 2: acceptable as-is (ses_051b8f106ffeT0NCTB38bp0pnC) — 全部 5 项 iteration 1 问题经实仓证据验证已修复（方向列修正经代码站点确认 / md 迁移范围明确移出 / Explore 标签已加 / 裁决落地位置已确定 / P1-MA2-038 标签已改）；无新阻塞问题；3 项非阻塞观察（ErpInvoiceLine 简写 / Closure Gates 保留 doc-only mvn 门控为保守偏差 / 写侧 header 标注 mfg→mfg 同域不精确但细节正确）不影响可执行性。

## Closure Gates

- [x] 范围内行为完成（裁决产物落地 + 豁免登记 + 索引回填）
- [x] 相关文档对齐（`data-dependency-matrix.md` / `posting-exemptions.md` / `arm-index.md`）
- [x] 已运行验证：本 plan 无代码变更（纯文档登记），`mvn clean install -DskipTests` 仅作回归基线确认（154 模块 BUILD SUCCESS）
- [x] compliance checker 基线不高于 M0 锚点（`bash docs/audits/nop-compliance-checker.sh`）— 本 plan 无代码变更，预期零漂移（实测 R2a=37/R2b=315/R2c=1228/R2d=28/R12a=69/R12b=66/R12c=38 全部持平，零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### nop-entropy 平台层 lazy/SPI 解耦（解除 fin/inv/mfg 目标域读侧迁移阻塞）

- Classification: `watch-only residual`
- Why Not Blocking Closure: governed-path eval §3.1 已裁决分支 b——I*Biz 强注入破坏单模块测试启动；在 nop-entropy 提供平台 lazy/SPI 解耦前，fin/inv/mfg 目标域的读侧 daoFor 只能登记为永久豁免。这不阻塞本 plan 的裁决产物落地。
- Successor Required: yes（nop-entropy 平台层提供 lazy/SPI 解耦后，可重新评估 fin/inv/mfg 读侧迁移）

## Closure

Status Note: 两阶段全部完成——Phase 1 读侧统一裁决落地于 `data-dependency-matrix.md §9`（8 项 finding 分类：md 目标域=可迁移[successor 已命名] / fin·inv·mfg·sal·ast 目标域=永久只读豁免[受 nop-entropy lazy/SPI 阻塞]）；Phase 2 写侧豁免登记于 `posting-exemptions.md`（P1-MA1-029 新增 + P1-MA2-038 扩展既有条目）+ `arm-index.md` 10 项 finding 状态回填。纯文档登记，154 模块 BUILD SUCCESS 基线不受影响，compliance checker 零漂移。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 ses_051a2e94effelHV3zItwj7Tsaf，未执行本 plan）
- Evidence: 独立结束审计已核实全部两阶段产物：Phase 1 `data-dependency-matrix.md §9` 覆盖 8 项读侧 P1 finding（逐项含目标域/处数/裁决/successor 条件，方向列与实仓代码 + 草案审查 iteration 1 三项修正一致）；Phase 2 `posting-exemptions.md` 新增 P1-MA1-029 豁免条目 + 扩展 MrpReleaseService 覆盖 P1-MA2-038 同域委外写；`arm-index.md` 10 项 finding 状态全部回填（`rg "MR1 todo"` 零剩余，`✅ resolved (plan 2026-07-29-2225-1)` 计数=10）。三项事实性抽查经实仓代码核实全部属实（fin-service md-service=test scope / ErpFinAccountingPeriodProcessor 写已走 I*Biz daoFor 仅读 / MrpReleaseService 委外单 APPROVED 绕 O-4）。**Verdict: PASS，全部 Closure Gates 可满足。**

Follow-up:

- nop-entropy lazy/SPI 解耦后，重新评估 fin/inv/mfg 读侧 daoFor 迁移可行性（governed-path eval §3.3 已列出平台需求）
- Type 1 `findAllByQuery` watch-only residual：若可机械替换站点数增至 ≥10，开独立 successor（governed-path eval §3.6 触发条件）
