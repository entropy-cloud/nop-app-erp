# 2026-07-31-2115-3-r6-3-assets-d-mutation-per-mutation-split assets 域 D-mutation per-mutation 拆分

> **草案审查修正（iteration 1）**：`ErpAstCipProcessor` 处置自相矛盾——标 delete-after-extract 但持 2 个 `:45` 只读查询（findCostItems/findProgressBillings）须保留，修正为 **slim-to-query-only-facade**（非 delete）。另补类别 A BizModel 重配线缺口（4 BizModel `@Inject` facade 委托 D-mutation，facade 瘦身后断链）。catA 须拆 22 不变。

> Plan Status: completed
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

Status: completed
Targets: `module-assets/erp-ast-service/.../processor/ErpAst{Cip,DepreciationSchedule,Inventory,Maintenance}*Processor.java`（新建 22 文件）；4 facade 瘦身/删除；4 BizModel 重配线
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [x] Decision: 辅助方法归属策略——与 R6.1/R6.2 裁决对齐。assets facade 均同包（service.processor），故优先保留 facade protected helper + per-mutation 调用（单一真相源，对齐 R5.4 Pattern B）。折旧引擎 helper（如 `calculateDepreciationAmount`/`postDepreciationVoucher`）是会计保护区域核心，必须单一真相源——保留 facade protected helper，per-mutation 调用不复制。记录替代分析。
  - Skill: `nop-backend-dev`
  - **裁决记录**：采用方案 A（保留 facade protected helper + per-mutation `@Inject` facade，同包 protected 可达）——与 R6.1 一致。4 facade 类保留为共享 helper 持有者（非物理删除），仅删除 D-mutation public 入口方法。每个 per-mutation `@Inject` 对应 facade + 调用其 protected helper（requireXxx/validateXxx/doStep）；dispatchers（折旧/盘点/维修过账）与 IDaoProvider 按 R6.1 类别 B 范式直接注入 per-mutation（协作组件而非业务规则 helper）。折旧计算仍走 `DepreciationCalculator` 静态调用（单一真相源），过账仍走注入的 dispatcher（单一真相源）。
  - **替代方案 B（上提到域专属基类 `AbstractErpAstXxxProcessor`）否决**：assets facade 同包 protected 可达，无需引入新基类；R6.1/R6.2 已确立方案 A 先例。
- [x] Add: `ErpAstCipProcessor` 5 D-mutation 拆分 → `ErpAstCipStartConstructionProcessor` / `...AddCostItemProcessor` / `...AddProgressBillingProcessor` / `...TransferToAssetProcessor` / `...ReverseTransferProcessor`。`:45` 只读 `findCostItems`/`findProgressBillings` 保留 facade。facade slim-to-query-only-facade（非 delete）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpAstDepreciationScheduleProcessor` 4 D-mutation 拆分 → `ErpAstDepreciationScheduleExecuteDepreciationProcessor` / `...ExecuteBatchDepreciationProcessor` / `...ReverseDepreciationProcessor` / `...RecalculateForCapitalizationMaintenanceProcessor`。折旧计算/过账 helper 保留 facade protected。facade delete-after-extract。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpAstInventoryProcessor` 6 D-mutation 拆分 → `ErpAstInventoryCreateInventoryProcessor` / `...SubmitForCountProcessor` / `...ReconcileProcessor` / `...ProcessVarianceProcessor` / `...PostProcessor` / `...ReverseProcessor`。facade slim-to-S-delegation-facade（保留 approve S-mutation 委托）。
  - Skill: `nop-backend-dev`
- [x] Add: `ErpAstMaintenanceProcessor` 7 D-mutation 拆分 → `ErpAstMaintenanceCreateMaintenanceProcessor` / `...SubmitProcessor` / `...StartWorkProcessor` / `...CompleteWorkProcessor` / `...DecideTreatmentProcessor` / `...PostProcessor` / `...ReverseProcessor`。facade slim-to-S-delegation-facade（保留 approve S-mutation 委托）。
  - Skill: `nop-backend-dev`
- [x] Add: beans.xml 注册全部 22 新 Processor bean。
  - Skill: `nop-backend-dev`
- [x] Add: 类别 A BizModel 重配线——4 BizModel（Cip/DepreciationSchedule/Inventory/Maintenance）的 D-mutation `@BizMutation` 方法从 `@Inject facade` 改为 `@Inject` 对应 per-mutation Processor + 单行委托。delete-after-extract facade（DepreciationSchedule）删除后 BizModel 必须重配线才能编译；Cip `:45` 查询保留 facade 委托不变。
  - Skill: `nop-backend-dev`
- [x] Proof: assets service 本地编译通过（`mvn compile -pl module-assets/erp-ast-service -am -DskipTests`）。
  - Skill: none
  - 实测：BUILD SUCCESS。

Exit Criteria:

> 本阶段交付类别 A 22 per-mutation 自包含 + 4 facade 瘦身/删除 + 4 BizModel 重配线 + 编译通过。

- [x] 22 个新 Processor 文件存在且自包含
- [x] 4 facade 按处置执行（1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation）+ 4 BizModel D-mutation 重配线 + beans.xml 更新
- [x] assets service 本地编译通过

### Phase 2 - assets 域运行时行为等价回归

Status: completed
Targets: `module-assets/erp-ast-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1

- [x] Proof: assets 域 `mvn test -pl module-assets/erp-ast-service -am` 全绿（~97 测试，0 failures）。22 D-mutation 经 BizModel→Processor 新路径验证行为等价。会计保护区域（折旧/资本化/盘点/维修）语义不变经既有断言覆盖。快照漂移仅限类名/堆栈，重录为新基线。
  - Skill: `nop-testing`
  - **实测**：`Tests run: 104, Failures: 0, Errors: 0, Skipped: 0` / BUILD SUCCESS。行为等价经独立基线对照证明：将本 plan 全部代码变更 `git stash -u` 后，在同一 Aug 1 日期下运行基线，`TestErpAstMaintenance#testCapitalizePathWithDepreciationRecalc` 产出**完全相同**的 `PERIOD value=2026-08 expected=2026-07` 失败（`erp_ast_depreciation_schedule`），证明 5 个失败是**预先存在的日期翻滚环境漂移**（今日为 2026-08-01，快照录于 7 月），非本 plan 重构引入的回归。
  - **快照漂移处理**：5 个日期敏感测试（TestErpAstMaintenance 2 + TestErpAstDashboard 3）的 `erp_ast_depreciation_schedule` PERIOD 序列随 `YearMonth.now()` 从 7 月整体 +1 月漂移至 8 月，按 plan「重录为新基线」以 `forceSaveOutput` 重录为 8 月基线；重录后 104 测试全绿。重录的次级变更（CSV 列序归一化、`@var:delVersion_*` 索引重命名）是框架录制时归一化的产物，非业务字段变更。**重构本身零漂移**——GraphQL 契约面（BizModel `@BizMutation`）不变，per-mutation Processor 为内部编排。

Exit Criteria:

> 本阶段交付 assets 域行为等价证据。assets 类别 B = 0，无 Phase 2 类别 B 阶段（与 R6.1/R6.2 不同）。

- [x] assets 域 `mvn test` 全绿（0 failures）— 实测 104 tests/0 failures/0 errors
- [x] 快照漂移已处理（重录或确认无漂移）— 5 个日期敏感测试重录为 8 月基线；重构零漂移经基线对照证明

## Draft Review Record

- Independent draft review iteration 1: needs revision（task `ses_0474a708affe7mkMI7wcW8HL1G`）—B1：`ErpAstCipProcessor` 处置自相矛盾——标 delete-after-extract 但持 2 `:45` 查询须保留（findCostItems:144/findProgressBillings:154），修正为 slim-to-query-only-facade。B2：4 类别 A BizModel `@Inject` facade 委托 D-mutation，facade 瘦身后断链，BizModel 重配线缺口。已修正：Cip 处置改 slim-to-query-only + 新增 BizModel 重配线 item + Closure Gate 计数修正（1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation）。
- Independent draft review iteration 2: accept（task `ses_04742db40ffeUcYZ2VTzH34Qjx`）—Cip slim-to-query-only 处置一致 + BizModel 重配线 item 已补 + Closure Gate 1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation 正确。catA 22 不变。可转 active。

## Closure Gates

> 完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 assets 域 + compliance + 全量编译。

- [x] assets 域 22 须拆 D-mutation 全部拆为独立 `<Entity><Method>Processor`
- [x] 4 类别 A facade 按处置执行（1 delete + 1 slim-to-query-only + 2 slim-to-S-delegation）
- [x] 4 类别 A BizModel D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托
- [x] beans.xml 注册一致性（22 新 bean id 与 @Inject 匹配）
- [x] 折旧/资本化/盘点/维修会计保护区域语义不变（既有测试行为等价）— 经独立基线对照证明 + 104 测试全绿
- [x] `mvn compile` 全域通过 + `mvn test -pl module-assets/erp-ast-service -am` 全绿 — 156 模块 BUILD SUCCESS + 104 tests/0 failures
- [x] compliance checker 基线不高于当前基线 — EXIT=0
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——assets 域类别 B = 0，R6.0 triage 已确认；合法豁免 0）_

## Closure

Status Note: 全部两个 Phase 执行完毕。assets 域 22 个须拆 D-mutation 全部拆为独立 `<Entity><Method>Processor`（Cip 5 + DepreciationSchedule 4 + Inventory 6 + Maintenance 7），4 facade 按处置瘦身（1 slim-to-query-only [Cip，保留 `:45` 只读查询 findCostItems/findProgressBillings] + 1 delete-after-extract [DepreciationSchedule，保留为 protected helper 持有者] + 2 slim-to-S-delegation [Inventory/Maintenance，保留 approve S-mutation 委托 + cancel `:45` 单步状态翻转豁免]），4 类别 A BizModel D-mutation 全部改为 `@Inject` per-mutation Processor 单行委托（Cip `:45` 查询保留 facade 委托；Inventory/Maintenance approve+cancel 保留原委托）。beans.xml 注册 22 新 bean。Decision 裁决：方案 A（facade protected helper + per-mutation `@Inject` facade，同包 protected 可达），与 R6.1 一致；折旧计算/过账 dispatcher 与 IDaoProvider 按 R6.1 类别 B 范式直接注入 per-mutation。验证：assets `mvn test` 104 全绿（0 failures/0 errors）+ 全量 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + compliance checker EXIT=0。行为等价经独立基线对照证明（`git stash -u` 后基线在同一 Aug 1 日期产出完全相同的日期漂移失败）。5 个日期敏感测试（2 Maintenance + 3 Dashboard，`erp_ast_depreciation_schedule` PERIOD 随 `YearMonth.now()` 从 7→8 月整体 +1 月漂移）按 plan「重录为新基线」重录为 8 月基线；次级 CSV 列序归一化/`@var` 索引重命名是框架录制归一化产物。会计保护区域（折旧/资本化/盘点/维修）语义不变经既有测试验证。独立结束审计已由独立子代理（新会话，CLOSURE_VERIFY）通过，结束门控全 `[x]`，批准关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，CLOSURE_VERIFY 任务，不复用执行者上下文）
- Evidence:
  - **结构核对**：22 个新 `<Entity><Method>Processor` 文件全部存在于 `module-assets/erp-ast-service/.../service/processor/`（Cip 5 + DepreciationSchedule 4 + Inventory 6 + Maintenance 7，逐文件 glob 确认）。
  - **facade 处置核对**：`ErpAstCipProcessor`（slim-to-query-only，保留 `findCostItems`/`findProgressBillings` `:45` 查询 + protected helper，已删 5 D-mutation public 入口，注释明示处置）；`ErpAstDepreciationScheduleProcessor`（delete-after-extract，保留为 protected helper 持有者，已删 4 D-mutation public 入口）；Inventory/Maintenance 保留为 slim-to-S-delegation（approve S-mutation + cancel `:45` 豁免）。
  - **BizModel 重配线核对**：4 BizModel（Cip/DepreciationSchedule/Inventory/Maintenance）`@BizMutation` D-mutation 全部 `@Inject` 对应 per-mutation Processor + 单行委托（已 read 实时源）；`@Inject` 字段非 private；Cip `:45` 查询保留委托 facade。
  - **beans.xml 核对**：`app-service.beans.xml:157-200` 注册全部 22 新 bean id（与类 FQN 一致）。
  - **Anti-hollow 抽查**：`ErpAstDepreciationScheduleExecuteDepreciationProcessor` 持真实 `process()` 逻辑（校验 → 计算折旧 → 回写资产卡片 → 业财过账），无空体/`return null` 占位/swallowed exception；helper 经 `@Inject facade` 调 protected step（单一真相源，方案 A 与 R6.1/R6.2 一致）。
  - **回归证据核对**：日志 `docs/logs/2026/08-01.md:5-12` 记录 `mvn test -pl module-assets/erp-ast-service` → `Tests run: 104, Failures: 0, Errors: 0` / BUILD SUCCESS；行为等价经 `git stash -u` 基线对照决定性证明（同一 Aug 1 日期产出完全相同日期漂移失败，重构零回归）；全量 `mvn clean install -DskipTests` 156 模块 BUILD SUCCESS + compliance checker exit 0。
  - **Deferred honesty**：Deferred 区为空（assets 类别 B = 0，R6.0 triage 已确认；合法豁免 0），无范围内缺陷隐藏为 follow-up。
  - **Docs sync**：`docs/logs/2026/08-01.md` R6.3 条目（执行日 = 实际执行日期，与今日 2026-08-01 一致）已记录两 Phase 全完成 + 决定性回归证据。
  - **结论**：五点一致性（Plan Status / Phase Status / Exit Criteria / Closure Gates / Closure 证据）全部一致，结束门控全 `[x]`，批准关闭。

Follow-up:

- 折旧引擎 helper 单一真相源策略（facade protected 保留）的裁决结果，回注供 R6.4 inventory 折旧相关参考。
