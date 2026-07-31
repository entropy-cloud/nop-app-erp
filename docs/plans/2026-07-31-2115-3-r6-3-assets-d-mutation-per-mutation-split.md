# 2026-07-31-2115-3-r6-3-assets-d-mutation-per-mutation-split assets 域 D-mutation per-mutation 拆分

> **草案审查修正（iteration 1）**：`ErpAstCipProcessor` 处置自相矛盾——标 delete-after-extract 但持 2 个 `:45` 只读查询（findCostItems/findProgressBillings）须保留，修正为 **slim-to-query-only-facade**（非 delete）。另补类别 A BizModel 重配线缺口（4 BizModel `@Inject` facade 委托 D-mutation，facade 瘦身后断链）。catA 须拆 22 不变。

> Plan Status: active
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.3
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage）；`docs/plans/2026-07-30-1909-1-mr5-r5-4-assets-s-mutation.md`（R5.4 assets S-mutation 先例）；`docs/architecture/processor-extension-pattern.md`（真相源）
> Mission: audit-remediation
> Work Item: R6.3
> Audit: required

## Current Baseline

- **MR5 assets 域 S-mutation 已完成**：30 个 S-mutation per-mutation Processor 自包含（Capitalization/Disposal/Merge/Split/ValueAdjustment/Inventory/Maintenance），含 S-mutation 的 facade 公共方法已精简为单行委托。MR6 不重开 MR5。
- **类别 B = 0**：R6.0 实仓复核确认 assets 域 BizModel 已全部委托 Processor（无内联 `@BizMutation` 零 Processor 引用违规）。本 plan **仅类别 A**。
- **类别 A 违规 facade（4 个，持 ≥2 D-mutation 共用，违反 `processor-extension-pattern.md:42`）——实测行数 + 公共 D-mutation 入口**：
  - `ErpAstCipProcessor`（487 行）— D-mutation 入口 5：`startConstruction`、`addCostItem`、`addProgressBilling`、`transferToAsset`、`reverseTransfer`。豁免保留：`findCostItems`（:144）/`findProgressBillings`（:154）（`:45` 只读子实体查询）。处置：**slim-to-query-only-facade**（delete 5 D-mutation，保留 2 `:45` 查询——非纯 delete）。
  - `ErpAstDepreciationScheduleProcessor`（352 行）— D-mutation 入口 4：`executeDepreciation`、`executeBatchDepreciation`、`reverseDepreciation`、`recalculateForCapitalizationMaintenance`。处置：**delete-after-extract**（纯 D-mutation facade，无 `:45` 查询）。
  - `ErpAstInventoryProcessor`（487 行）— D-mutation 入口 6：`createInventory`、`submitForCount`、`reconcile`、`processVariance`、`post`、`reverse`。处置：**slim-to-S-delegation-facade**（R6.0 triage 标注含 Inventory approve S-mutation 委托；delete D-mutation，保留 S-mutation 单行委托）。
  - `ErpAstMaintenanceProcessor`（377 行）— D-mutation 入口 7：`createMaintenance`、`submit`、`startWork`、`completeWork`、`decideTreatment`、`post`、`reverse`。处置：**slim-to-S-delegation-facade**（含 Maintenance approve S-mutation 委托；delete D-mutation，保留 S-mutation 单行委托）。
  - **类别 A 须拆合计：22 D-mutation → 22 个新 `<Entity><Method>Processor`**。D-mutation per-mutation 文件尚不存在，本 plan 须新建。
  - 合法豁免 0。`:45` 查询豁免 2（Cip findCostItems/findProgressBillings）保留 facade。
- **类别 A BizModel 配线现状**（实测）：assets 域 4 BizModel（Cip/DepreciationSchedule/Inventory/Maintenance）各 `@Inject` 对应 facade 并委托 D-mutation（`return facade.method(...)`）。facade 瘦身 D-mutation 后，BizModel 须**重配线**为 `@Inject` 对应 per-mutation Processor + 单行委托（`:45` 查询保留 facade 委托不变）。
- **[会计保护区域]** DepreciationSchedule executeDepreciation/executeBatchDepreciation/reverseDepreciation 涉及折旧凭证过账（assets 域最高密度 Processor 链路，A4.3 专属审计标的）；CIP transferToAsset 涉及资本化凭证（在建工程→固定资产）；Inventory post/reverse 涉及盘点差异凭证；Maintenance post 涉及维修费用凭证。owner doc `docs/design/assets/`（depreciation-and-posting / state-machine）已固化语义；R1.16/R1.18/R2.12 已修复相关缺陷。本 plan 仅做编排位置迁移，不改业务语义。
- **既有测试基线**：assets 域 `mvn test` ~97 测试 0 failures（R5.4 实测）。
- D-mutation 无审批 hook → 直接写 `process()` + protected step（`:80-97`，roadmap line 295）。

## Goals

- assets 域 22 个须拆 D-mutation 全部拆为独立 `<Entity><Method>Processor`，每 Processor 自包含 `process()` + protected step，对齐 `:29/:42/:80-97`。
- 类别 A 4 facade 按 triage 处置（1 delete-after-extract + 1 slim-to-query-only + 2 slim-to-S-delegation-facade）；类别 A BizModel 重配线为 `@Inject` per-mutation Processor + 单行委托；`:45` 查询保留 facade。
- beans.xml 注册全部新 Processor bean；assets 域 `mvn test` 全绿（0 failures），折旧/资本化/盘点过账语义不变。

## Non-Goals

- R6.1/R6.2/R6.4-R6.8（其他域 + 全量验证）。
- MR5 S-mutation 重构（已完成）。
- 类别 B 拆分——assets 域类别 B = 0（BizModel 已全部委托 Processor）。
- 新增业务测试——仅验证既有测试行为等价。
- 业务语义变更。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/assets/`（depreciation-and-posting / state-machine）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`。涉及会计保护区域（折旧过账/资本化/盘点/维修），须对照 R1.16/R1.18/R2.12 owner doc 静态校验语义不变。`nop-testing` 用于回归。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 拆分（4 facade → 22 per-mutation Processor）+ BizModel 重配线

Status: planned
Targets: `module-assets/erp-ast-service/.../processor/ErpAst{Cip,DepreciationSchedule,Inventory,Maintenance}*Processor.java`（新建 22 文件）；4 facade 瘦身/删除；4 BizModel 重配线
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [ ] Decision: 辅助方法归属策略——与 R6.1/R6.2 裁决对齐。assets facade 均同包（service.processor），故优先保留 facade protected helper + per-mutation 调用（单一真相源，对齐 R5.4 Pattern B）。折旧引擎 helper（如 `calculateDepreciationAmount`/`postDepreciationVoucher`）是会计保护区域核心，必须单一真相源——保留 facade protected helper，per-mutation 调用不复制。记录替代分析。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpAstCipProcessor` 5 D-mutation 拆分 → `ErpAstCipStartConstructionProcessor` / `...AddCostItemProcessor` / `...AddProgressBillingProcessor` / `...TransferToAssetProcessor` / `...ReverseTransferProcessor`。`:45` 只读 `findCostItems`/`findProgressBillings` 保留 facade。facade slim-to-query-only-facade（非 delete）。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpAstDepreciationScheduleProcessor` 4 D-mutation 拆分 → `ErpAstDepreciationScheduleExecuteDepreciationProcessor` / `...ExecuteBatchDepreciationProcessor` / `...ReverseDepreciationProcessor` / `...RecalculateForCapitalizationMaintenanceProcessor`。折旧计算/过账 helper 保留 facade protected。facade delete-after-extract。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpAstInventoryProcessor` 6 D-mutation 拆分 → `ErpAstInventoryCreateInventoryProcessor` / `...SubmitForCountProcessor` / `...ReconcileProcessor` / `...ProcessVarianceProcessor` / `...PostProcessor` / `...ReverseProcessor`。facade slim-to-S-delegation-facade（保留 approve S-mutation 委托）。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpAstMaintenanceProcessor` 7 D-mutation 拆分 → `ErpAstMaintenanceCreateMaintenanceProcessor` / `...SubmitProcessor` / `...StartWorkProcessor` / `...CompleteWorkProcessor` / `...DecideTreatmentProcessor` / `...PostProcessor` / `...ReverseProcessor`。facade slim-to-S-delegation-facade（保留 approve S-mutation 委托）。
  - Skill: `nop-backend-dev`
- [ ] Add: beans.xml 注册全部 22 新 Processor bean。
  - Skill: `nop-backend-dev`
- [ ] Add: 类别 A BizModel 重配线——4 BizModel（Cip/DepreciationSchedule/Inventory/Maintenance）的 D-mutation `@BizMutation` 方法从 `@Inject facade` 改为 `@Inject` 对应 per-mutation Processor + 单行委托。delete-after-extract facade（DepreciationSchedule）删除后 BizModel 必须重配线才能编译；Cip `:45` 查询保留 facade 委托不变。
  - Skill: `nop-backend-dev`
- [ ] Proof: assets service 本地编译通过（`mvn compile -pl module-assets/erp-ast-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 A 22 per-mutation 自包含 + 4 facade 瘦身/删除 + 4 BizModel 重配线 + 编译通过。

- [ ] 22 个新 Processor 文件存在且自包含
- [ ] 4 facade 按处置执行（1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation）+ 4 BizModel D-mutation 重配线 + beans.xml 更新
- [ ] assets service 本地编译通过

### Phase 2 - assets 域运行时行为等价回归

Status: planned
Targets: `module-assets/erp-ast-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1

- [ ] Proof: assets 域 `mvn test -pl module-assets/erp-ast-service -am` 全绿（~97 测试，0 failures）。22 D-mutation 经 BizModel→Processor 新路径验证行为等价。会计保护区域（折旧/资本化/盘点/维修）语义不变经既有断言覆盖。快照漂移仅限类名/堆栈，重录为新基线。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 assets 域行为等价证据。assets 类别 B = 0，无 Phase 2 类别 B 阶段（与 R6.1/R6.2 不同）。

- [ ] assets 域 `mvn test` 全绿（0 failures）
- [ ] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: needs revision（task `ses_0474a708affe7mkMI7wcW8HL1G`）—B1：`ErpAstCipProcessor` 处置自相矛盾——标 delete-after-extract 但持 2 `:45` 查询须保留（findCostItems:144/findProgressBillings:154），修正为 slim-to-query-only-facade。B2：4 类别 A BizModel `@Inject` facade 委托 D-mutation，facade 瘦身后断链，BizModel 重配线缺口。已修正：Cip 处置改 slim-to-query-only + 新增 BizModel 重配线 item + Closure Gate 计数修正（1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation）。
- Independent draft review iteration 2: accept（task `ses_04742db40ffeUcYZ2VTzH34Qjx`）—Cip slim-to-query-only 处置一致 + BizModel 重配线 item 已补 + Closure Gate 1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation 正确。catA 22 不变。可转 active。

## Closure Gates

> 完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 assets 域 + compliance + 全量编译。

- [ ] assets 域 22 须拆 D-mutation 全部拆为独立 `<Entity><Method>Processor`
- [ ] 4 类别 A facade 按处置执行（1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation）
- [ ] 4 类别 A BizModel D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托
- [ ] beans.xml 注册一致性（22 新 bean id 与 @Inject 匹配）
- [ ] 折旧/资本化/盘点/维修会计保护区域语义不变（既有测试行为等价）
- [ ] `mvn compile` 全域通过 + `mvn test -pl module-assets/erp-ast-service -am` 全绿
- [ ] compliance checker 基线不高于当前基线
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——assets 域类别 B = 0，R6.0 triage 已确认；合法豁免 0）_

## Closure

Status Note: _（待执行后填充）_

Closure Audit Evidence:

- Auditor / Agent: _（待独立结束审计）_
- Evidence: _（待填充）_

Follow-up:

- 折旧引擎 helper 单一真相源策略（facade protected 保留）的裁决结果，回注供 R6.4 inventory 折旧相关参考。
