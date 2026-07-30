# 2026-07-31-0310-1-r2-6-mfg-owner-doc-drift

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` MR2 / R2.6（P1-MA3-040 ~ P1-MA3-045）
> Related: `docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（A3.3 mfg drift 审计报告）；`docs/audits/arm-index.md §P1-MA3-040~045`；plan `2026-07-31-0010-3-r2-3-4-5-finance-owner-doc-drift-cluster.md`（同型 owner-doc drift 修复先例）；`docs/plans/2026-07-30-0143-3-r1-14-mfg-dict-dead-state-owner-doc-drift.md`（R1.14 mfg dict/owner-doc 先例）
> Audit: required

## Current Baseline

审计来源：A3.3 owner doc vs 代码 drift 审计登记 mfg 域 6 项 P1（P1-MA3-040~045）。R2.0 将其展开为 R2.6 单行（status `todo`）。6 项均为**文档↔代码 drift**，drift 方向 = **更新文档对齐已核实的 code/ORM**（与 R2.3/4/5 同型），**不改应用代码 / ORM / 常量**。逐项实时基线（grep / file:line 可复现）：

- **P1-MA3-040 [doc→code，内部矛盾]**：`state-machine.md:155-165` §质检约束声明表以"工单可从 **INSPECTING** → COMPLETED"为真实状态，但 `erp-mfg/work-order-status` 字典（ORM `orm.xml:35-46`）10 态**无 INSPECTING**，code `ErpMfgWorkOrderProcessor.java:188-201` 完工门控 config-gated throw 保持 `IN_PROCESS`。§实现偏离补注（:171）已承认偏离，但上方声明表仍以 INSPECTING 为真实状态——**doc 内部矛盾**。
- **P1-MA3-041 [doc→code，逃生通道不存在]**：`state-machine.md:71` 声明"报工超过工单数量→拒绝（除非配置允许超产）"；code `ErpMfgWorkOrderProcessor.java:181-185` **硬编码拒绝** + `ErpMfgErrors.java:98-99` 错误信息引用"未启用超产配置"，但 `ErpMfgConstants` **无此 config key**。doc 承诺的逃生通道在 code 中不存在。（与 P2-MA2-042 报工超产 config-gate 缺失同根。）
- **P1-MA3-042 [doc→code，blocker]**：`material-reservation.md`（288 行）声明完整预留子系统（WorkOrder `reservationStatus` 6 态 + `ErpMfgMaterialReservation` 实体 + 审核触发预留 flow + 领料扣减预留 + 预留释放 + 5 config key）。Code 完全未实现——`KitAvailabilityChecker.java:30-33` 显式只读"不写预留"，`ErpMfgWorkOrder` 无 `reservationStatus` 字段，5 config key 全不在 `ErpMfgConstants`。doc `:9` 已有"业务语义说明，实际落位以库存域为准"部分免责声明，但**不足以覆盖**后续详细 mfg 侧 flow/config/状态维度（仍以 mfg 侧 `ErpMfgMaterialReservation` + 6 态 + 5 config 为可执行设计）。
- **P1-MA3-043 [doc→code，两 design doc 互斥]**：`use-cases.md:223-227` UC-MFG-12 列 4 差异公式（材料用量/材料价格/人工效率/人工费率），code `ProductionVarianceCalculator.java:128-203` 实现 6 类（含制造费用 OVERHEAD + 产量 VOLUME + 委外 SUBCONTRACT，不含材料价格[PPV 归采购]）。兄弟文档 `variance-analysis.md:62-68` **正确列全**——两 design doc 互斥。
- **P1-MA3-044 [doc→code]**：`README.md:36-37` 列 `ProductionPlan`（生产计划）+ `DowntimeEntry`（停机记录）为核心业务对象，ORM 34 实体**无** `ErpMfgDowntimeEntry`、**无** `ErpMfgProductionPlan`（最接近 `ErpMfgMrpPlan`）。`crp.md:129` 承认 maintenance downtime 为 Non-Goal 但 README 无免责声明。
- **P1-MA3-045 [code→doc]**：`variance-analysis.md:89` 标"异常预警 **Deferred**，依赖通知派发通道"，但 code `ProductionVarianceCalculator.java:225-261` 已**完整实现** `dispatchVarianceAlertIfOverThreshold`（调 `IErpSysNotificationBiz.notify` + event `mfg.production-variance`），`ErpMfgConstants.java:214-223` 有 2 config key（`variance-alert-enabled` 默认 true + `variance-alert-threshold` 默认 100）。doc §配置点表（:113-115）未列此 2 config。

**R1.14 协调**：R1.14 已修复 mfg dict 死状态/owner doc 漂移（`mrp.md` RELEASED→FIRMED 等）。本计划处理 A3.3 登记的**不同维度**（质检约束/超产/物料预留/差异公式/README 实体/预警 Deferred 标注），与 R1.14 无重叠。

## Goals

- **G1**：消除 mfg owner doc 内部矛盾与 doc↔code drift——更新 6 处文档对齐已核实的 code/ORM 实际（质检约束状态值 / 超产逃生通道 / 物料预留子系统 / 差异公式 / README 实体清单 / 差异预警实证）。
- **G2**：保留未实现能力（物料预留子系统、超产放行）为明确的 **Deferred + successor**，而非 doc 内部矛盾或幻影 config。

## Non-Goals

- **任何应用代码 / ORM / 常量变更**——drift 方向 = doc→code（040/041/042/043/044）与 code→doc（045），本计划只改 doc 对齐 code，不改 code 对齐 doc。code 侧已由 MR1（R1.14 等）核实或属有意简化。
- 实现 P1-MA3-042 物料预留子系统——裁决 Deferred（owner doc 正式化，successor = 库存域 `ErpInvReservation*` 承载，mfg 侧只读校验）。
- 实现 P1-MA3-041 超产放行 config——裁决 Deferred（标注"当前硬编码拒绝超产"）。
- P2 watch-only 项（mfg 域 A3.3 P2 项等）——显式 out-of-scope（非本批次 P1 范围）。
- R2.1 mfg 文档执行状态 scrub——R2.1 已 done；本计划聚焦 dict/状态值/config/实体清单 drift，不同维度。

## Task Route

- Type: `app-layer design change`（mfg owner doc 语义对齐，无代码变更）
- Owner Docs: `docs/design/manufacturing/`（state-machine.md / material-reservation.md / use-cases.md / README.md / variance-analysis.md）+ `docs/design/manufacturing/crp.md`（P1-MA3-044 协调核对）
- Skill Selection Basis: 无匹配技能。可用技能集（nop-backend-dev / nop-frontend-dev / nop-testing / nop-debugging / nop-git-master）均针对代码/前端/测试/调试/Git，不覆盖纯文档编辑。本计划为 mfg 文档对齐 code，**Skill: none**。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 质检约束状态值 + 超产逃生通道 drift（P1-MA3-040 + P1-MA3-041）

Status: completed
Targets: `docs/design/manufacturing/state-machine.md`
Skill: none

- Item Types: `Fix`
- Prereqs: none

- [x] `Fix`: §质检约束声明表（:155-165）移除 `INSPECTING` 为真实状态的表述——改为"完工质检门控经 config-gated 钩子替代（无 INSPECTING 字典态）：`reportCompletion` 达量且 `inspectionRequired=true` 且 `erp-mfg.inspection-gate-enabled=true`（默认 false）时抛 `ERR_INSPECTION_REQUIRED`，工单保持 `IN_PROCESS`"，与 :171 实现偏离补注统一为单一真相源（消除"声明表 INSPECTING vs 补注无 INSPECTING"内部矛盾）。
- [x] `Fix`: §报工超量段（:71）"拒绝（除非配置允许超产）"改为"**当前硬编码拒绝报工超量**；可配置超产放行（config-gate）为 successor，未落地"——移除不存在的 config key 引用，标注 `ErpMfgErrors` 错误信息文案与实际行为（拒绝）一致但逃生通道 Deferred。

Exit Criteria:

> 阶段交付：state-machine.md 内部矛盾消除 + 超产逃生通道幻影 config 移除并标 Deferred。无代码变更，无 owner-doc 行为契约之外的文档受影响。完整仓库验证属 Closure Gates。

- [x] state-machine.md §质检约束声明表与 §实现偏离补注对"INSPECTING 不存在、以 config-gated 钩子替代"表述一致（grep `INSPECTING` 无残留为"真实状态"的表述）
- [x] state-machine.md 超产段不再引用不存在的 config key，且显式标注"当前硬编码拒绝"

### Phase 2 - 物料预留子系统 Deferred 正式化（P1-MA3-042）

Status: completed
Targets: `docs/design/manufacturing/material-reservation.md`
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: none

- [x] `Fix`: 将 `material-reservation.md` 详细 mfg 侧 flow/状态/实体/config（`ErpMfgMaterialReservation` 实体 + `reservationStatus` 6 态 + 5 config key）标注为**设计意图 / Deferred**——明确：(a) 持久化真相源 = 库存域 `ErpInvReservation`/`ErpInvReservationLine`（已 :9 声明，扩展为整节适用）；(b) 制造域当前仅 `KitAvailabilityChecker` 只读齐套校验，**不写预留**；(c) `ErpMfgWorkOrder` 无 `reservationStatus` 字段、5 config key 均未落地；(d) 完整预留写路径（审核触发预留 + 领料扣减 + 释放）为 successor，触发条件 = 库存域 `ErpInvReservation*` 写接口落地后接线。
- [x] `Decision`: 修复方式裁决 = **保留 doc 作为设计语义说明 + 全节 Deferred 标注**（不删除文档）。理由：物料预留是 mfg 业务语义重要组成，删除会丢失设计意图；保留并标注 Deferred 使文档诚实反映"设计有、实现 Deferred"。替代方案 = 删除整节（风险：丢失设计意图，未来实现时无参考）→ 不采纳。

Exit Criteria:

- [x] material-reservation.md 全节明确标注 Deferred + 持久化真相源指向库存域 + 当前实现边界（只读齐套校验）显式声明；grep `ErpMfgMaterialReservation` 出现处均带 Deferred/设计意图语境

### Phase 3 - 差异公式 + README 实体清单 + 差异预警实证标注（P1-MA3-043 + P1-MA3-044 + P1-MA3-045）

Status: completed
Targets: `docs/design/manufacturing/use-cases.md`、`docs/design/manufacturing/README.md`、`docs/design/manufacturing/variance-analysis.md`
Skill: none

- Item Types: `Fix`
- Prereqs: none

- [x] `Fix`: `use-cases.md:223-227` UC-MFG-12 差异公式列表对齐 `variance-analysis.md:62-68` + code `ProductionVarianceCalculator`——改为 6 类（材料用量 / 人工效率 / 人工费率 / 制造费用 OVERHEAD / 产量 VOLUME / 委外 SUBCONTRACT；材料价格 PPV 归采购域不在此），消除两 design doc 互斥。
- [x] `Fix`: `README.md:36-37` 移除或标注 Deferred——`ProductionPlan`（生产计划，最接近 `ErpMfgMrpPlan`）+ `DowntimeEntry`（停机记录）当前 ORM 不存在；与 `crp.md:129` maintenance downtime Non-Goal 声明一致，加 README 免责声明。
- [x] `Fix`: `variance-analysis.md:89` 移除"异常预警 Deferred"标注——差异阈值预警**已实现**（`ProductionVarianceCalculator.dispatchVarianceAlertIfOverThreshold` + `IErpSysNotificationBiz.notify` + event `mfg.production-variance`）；§配置点表（:113-115）补 `erp-mfg.variance-alert-enabled`（默认 true）+ `erp-mfg.variance-alert-threshold`（默认 100）2 config key。

Exit Criteria:

- [x] use-cases.md UC-MFG-12 与 variance-analysis.md 差异公式列表一致（无互斥）
- [x] README.md 核心业务对象清单与 ORM 实体一致（不存在的实体标注 Deferred 或移除）
- [x] variance-analysis.md 预警段反映已实现状态 + §配置点表含 2 config key

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is** (ses_04b8d0093ffewPLT96lDOg4xvO) — 基线 file:line 抽查全准确（INSPECTING 内部矛盾 / variance-analysis Deferred + config 表 gap / README 实体清单），6 findings (040-045) 干净映射三阶段，结构镜像 R2.3/4/5 已验证先例。采纳两条非阻塞建议：P1-MA3-043 差异公式括注补"人工费率"（原列 5 类应 6 类，权威以 variance-analysis.md:62-68 为准）+ P2 watch-only 交叉引用措辞收紧。无 blocker，可进入实施。

## Closure Gates

> 纯文档计划：无代码变更，删除完整仓库 `build`/`test` 门控。保留 doc 一致性 + compliance checker（确认零新增命中，因 doc 改动不触发 compliance 规则）+ 独立审计门控。

- [x] 范围内行为/文档完成（6 项 finding doc 对齐）
- [x] 相关文档对齐（mfg owner doc 内部一致 + 与 code/ORM 一致）
- [x] 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（预期零新增命中——本计划不改代码）；doc 一致性 grep 复核（INSPECTING 残留 / 超产 config key / ErpMfgMaterialReservation Deferred 语境 / UC 差异公式对齐 / README 实体一致 / 预警 config 表）
- [x] 无范围内项目降级为 deferred/follow-up（P1-MA3-042 物料预留 + P1-MA3-041 超产放行 = 处置裁决 Deferred + 已命名 successor，非范围内缺陷隐瞒；doc 对齐部分范围内存活）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 物料预留子系统完整写路径（P1-MA3-042 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 持久化真相源为库存域 `ErpInvReservation*`，当前 mfg 侧仅只读齐套校验；完整预留写路径属跨域 substantial slice，须库存域写接口先行；doc 已诚实标注 Deferred + successor。
- Successor Required: `yes`（触发条件 = 库存域 `ErpInvReservation*` 写接口落地后接线 mfg 领料扣减/释放）

### 可配置超产放行（P1-MA3-041 successor）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前硬编码拒绝报工超量是安全默认；doc 已标注逃生通道 Deferred；属补能力非活跃缺陷。
- Successor Required: `yes`（触发条件 = 业务确认需允许超产时，加 `erp-mfg.over-completion-allowed` config + Processor 守卫）

## Closure

Status Note: 结束审计 PASS（独立子代理 ses_04b83f6beffeaZw1yuD4tNODNu，新会话，未参与执行）。6 项 finding 全部 doc 对齐已核实 code/ORM：040 质检约束声明表移除 INSPECTING 真实态表述 + 与实现约定统一为 config-gated 钩子单一真相源；041 超产段移除幻影 config key 标注当前硬编码拒绝 + 逃生通道 Deferred；042 material-reservation 整节 Deferred 正式化（持久化真相源=库存域 ErpInvReservation*）；043 UC-MFG-12 差异公式对齐 code 6 类（PPV 归采购）；044 README ProductionPlan/DowntimeEntry 标 Deferred + 免责声明；045 预警段改"已实现"+ §配置点表补 2 config key。零代码/ORM/常量变更（git status 确认仅 5 个 docs/design/manufacturing/*.md 改动）；compliance checker 零新增命中；roadmap R2.6 → done。

Closure Audit Evidence:

- 独立结束审计 VERDICT: **PASS**（ses_04b83f6beffeaZw1yuD4tNODNu，2026-07-31）。逐项核实：
  - P1-MA3-040 PASS — ORM `app-erp-manufacturing.orm.xml:35-46` 10 态无 INSPECTING；`ErpMfgWorkOrderProcessor.java:189-192` config-gated throw 保持 IN_PROCESS；state-machine.md INSPECTING 仅 2 处均述其为"无/字典缺失"，内部矛盾消除。
  - P1-MA3-041 PASS — `ErpMfgWorkOrderProcessor.java:182-185` 硬编码 ERR_OVER_REPORT；`ErpMfgErrors.java:99` 文案"未启用超产配置"；`ErpMfgConstants` grep over-completion=0；doc 标"当前硬编码拒绝"+逃生通道 successor 未落地。
  - P1-MA3-042 PASS — `ErpMfgMaterialReservation` 仅 Non-Goal 代码注释（无实体）；WorkOrder ORM 无 reservationStatus；ErpMfgConstants 无 4 reservation key；KitAvailabilityChecker 只读；material-reservation.md 2 处 ErpMfgMaterialReservation 均在 Deferred banner 内；ErpInvReservation/Line 实体确认存在。
  - P1-MA3-043 PASS — ProductionVarianceCalculator 实现 6 类（无材料价格）；variance-analysis.md:36 字段注释列 6 类+PPV归采购；use-cases.md UC-MFG-12 列 6 类+PPV归采购 note，两 doc 不再互斥。
  - P1-MA3-044 PASS — ORM 无 ErpMfgDowntimeEntry/ErpMfgProductionPlan；README:36-37 标 Deferred + :39 免责声明，与 crp.md:129 Non-Goal 一致。
  - P1-MA3-045 PASS — dispatchVarianceAlertIfOverThreshold:225-261 调 notify + event mfg.production-variance；ErpMfgConstants:214/217/223 两 config key + 事件常量；variance-analysis.md:89"已实现"+:116-117 config 表含 2 key；旧"Deferred，依赖通知派发"grep=0。
- 非 .md 文件改动：**0**（git status 仅 5 个 docs/design/manufacturing/*.md + 本 plan 文件）。
- 非阻塞残留观察：variance-analysis.md §差异分类 taxonomy 表（:15-23）仍列材料价格差异为概念行（预存、字段注释/core-logic/UC/code 已正确消歧 PPV归采购，不在 P1-MA3-043 修复范围），可留待未来 doc-hygiene pass。
