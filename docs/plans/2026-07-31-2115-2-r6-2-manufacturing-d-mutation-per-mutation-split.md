# 2026-07-31-2115-2-r6-2-manufacturing-d-mutation-per-mutation-split manufacturing 域 D-mutation + 内联多步 mutation per-mutation 拆分

> **草案审查修正（iteration 1）**：R6.0 triage 对 SubcontractOrder 枚举含 2 行重复（issueMaterials/receiveFinished 各列 2 次），致 catA 误计 20→实为 **18** 去重；另发现 `ErpMfgWorkOrderProcessor.cancel:158` + `ErpMfgSubcontractOrderProcessor.cancel:120` 是 `:46` 单步状态翻转（require+守卫+setStatus+updateEntity，零副作用），须登记豁免。total 31→**29**。另补类别 A BizModel 重配线缺口。

> Plan Status: active
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.2
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage）；`docs/plans/2026-07-30-1909-2-mr5-r5-5-mfg-s-mutation.md`（R5.5 mfg S-mutation 先例）；`docs/architecture/processor-extension-pattern.md`（真相源）
> Mission: audit-remediation
> Work Item: R6.2
> Audit: required

## Current Baseline

- **MR5 manufacturing 域 S-mutation 已完成**：WorkOrder 5 + SubcontractOrder 5 = 10 个 S-mutation per-mutation Processor 自包含，2 个含 S-mutation 的 facade（WorkOrder/Subcontract）公共 S-mutation 已精简为单行委托。MR6 不重开 MR5。
- **类别 A 违规 facade（4 个，持 ≥2 D-mutation 共用，违反 `processor-extension-pattern.md:42`）——实测行数 + 公共 D-mutation 入口**：
  - `ErpMfgJobCardProcessor`（188 行）— D-mutation 入口 7：`startJob`、`recordWork`、`submitJob`、`completeJob`、`holdJob`、`resumeJob`、`cancelJob`。处置：**delete-after-extract**（JobCard 无标准审批六动作，纯 D-mutation facade）。
  - `ErpMfgScheduleToJobCardProcessor`（336 行）— D-mutation 入口 2：`generateJobCardsFromSchedule`、`generatePendingJobCards`。豁免保留：`findWorkOrdersPendingJobCards`（`:45` 只读查询）。处置：**delete-after-extract**（剩余查询迁回 BizModel 或保留 facade）。
  - `ErpMfgSubcontractOrderProcessor`（574 行）— D-mutation 入口 4：`issueMaterials`、`receiveFinished`、`postProcessingFee`、`reverseCompletion`（roadmap 原列举 issueMaterials/receiveFinished 各重复 2 行，去重后实为 4 非 6）。豁免保留：`cancel`（:120，`:46` 单步状态翻转 require+守卫+setStatus+updateEntity 零副作用）。处置：**slim-to-S-delegation-facade**（保留 S-mutation 单行委托 + delete D-mutation）。
  - `ErpMfgWorkOrderProcessor`（552 行）— D-mutation 入口 5：`start`、`stop`、`resume`、`close`、`reportCompletion`。豁免保留：`checkAvailability`（`:45` 只读可用性校验）+ `cancel`（:158，`:46` 单步状态翻转 require+守卫+setStatus+updateEntity 零副作用）。处置：**slim-to-S-delegation-facade**（保留 S-mutation 单行委托 + delete D-mutation）。
  - **类别 A 须拆合计：18 D-mutation → 18 个新 `<Entity><Method>Processor`**（JobCard 7 + ScheduleToJobCard 2 + SubcontractOrder 4 + WorkOrder 5）。新增豁免 2（WorkOrder.cancel + SubcontractOrder.cancel，`:46`）。D-mutation per-mutation 文件尚不存在，本 plan 须新建。
- **类别 A BizModel 配线现状**（实测）：`ErpMfgJobCardBizModel`/`ErpMfgWorkOrderBizModel`/`ErpMfgSubcontractOrderBizModel` 各 `@Inject` 对应 facade 并委托 D-mutation（`return facade.method(...)`）。facade 删除/瘦身 D-mutation 后，BizModel 须**重配线**为 `@Inject` 对应 per-mutation Processor + 单行委托。
- **类别 B 违规 BizModel（7 个 BizModel，11 个内联 `@BizMutation`，零 Processor 引用，违反 `:5/:7`）——R6.0 triage 须拆清单**：
  - `ErpMfgBomBizModel`（1：rollupCost）
  - `ErpMfgCostVarianceBizModel`（1：calculateVariances）
  - `ErpMfgCrpLoadBizModel`（1：calculateLoad）
  - `ErpMfgMaterialIssueBizModel`（2：confirm/reverseConfirm）
  - `ErpMfgMrpPlanBizModel`（1：runMrp）
  - `ErpMfgMrpPlanLineBizModel`（3：releasePurchaseRequest/releaseSubcontractRequest/releaseWorkRequest）
  - `ErpMfgMrpScenarioBizModel`（2：promoteToFormalPlan/runSimulation）
  - **类别 B 须拆合计：11 个新 `<Entity><Method>Processor`**（target 命名见 roadmap §R6.0 triage 展开 §R6.2）。
  - 合法豁免 4（`ErpMfgForecast.approve/cancel` `:46` + `ErpMfgWorkOrder.cancel` `:46` + `ErpMfgSubcontractOrder.cancel` `:46`），保留 BizModel/facade 不动。后 2 项为 R6.0 triage 遗漏，本 plan 补登 `processor-per-mutation-exemption-registry.md`。
- **[会计保护区域]** WorkOrder reportCompletion/close 涉及完工入库业财过账；SubcontractOrder receiveFinished 涉及委外入库过账；CostVariance calculateVariances 涉及差异过账。owner doc `docs/design/manufacturing/`（state-machine / variance-analysis / mrp）已固化语义；R1.26/R2.11 已修复相关缺陷。本 plan 仅做编排位置迁移，不改业务语义。
- **既有测试基线**：mfg 域 `mvn test` ~144 测试（R5.5 实测；已知 1 pre-existing error TestErpMfgCompletionPosting LOCATION_ID 漂移，经 git stash 证实与本工作无关）。
- D-mutation 无审批 hook → 直接写 `process()` + protected step（`:80-97`，roadmap line 295）。

## Goals

- manufacturing 域 29 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 18 + 类别 B 11），每 Processor 自包含 `process()` + protected step，对齐 `:29/:42/:80-97`。
- 类别 A 4 facade 按 triage 处置（2 delete-after-extract + 2 slim-to-S-delegation-facade）；类别 A BizModel 重配线为 `@Inject` per-mutation Processor + 单行委托；`:45`/`:46` 豁免保留 facade/BizModel。
- 类别 B 7 BizModel 的 11 个内联 `@BizMutation` 改为 `@Inject` Processor + 单行委托。
- beans.xml 注册全部新 Processor bean；mfg 域 `mvn test` 全绿（0 failures，排除已知 pre-existing），业财过账语义不变。

## Non-Goals

- R6.1/R6.3-R6.8（其他域 + 全量验证）。
- MR5 S-mutation 重构（已完成）。
- 新增业务测试——仅验证既有测试行为等价。
- 合法豁免 2 项（`ErpMfgForecast.approve/cancel`，保留 BizModel）。
- 业务语义变更。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/manufacturing/`（state-machine / mrp / variance-analysis）、`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`。涉及业财过账保护区域（完工入库/委外/差异），须对照 R1.26/R2.11 owner doc 静态校验。`nop-testing` 用于回归。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 A facade D-mutation 拆分（4 facade → 18 per-mutation Processor）+ BizModel 重配线

Status: planned
Targets: `module-manufacturing/erp-mfg-service/.../processor/ErpMfg{JobCard,ScheduleToJobCard,SubcontractOrder,WorkOrder}*Processor.java`（新建 18 文件）；4 facade 瘦身/删除；3 BizModel 重配线
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: R6.0 done（已满足）

- [ ] Decision: 辅助方法归属策略——与 R6.1 裁决对齐（facade protected helper 保留为单一真相源 vs 域专属基类上提）。mfg facade 均同包（service.processor），故优先保留 facade protected helper + per-mutation 调用。记录替代分析。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpMfgJobCardProcessor` 7 D-mutation 拆分 → `ErpMfgJobCardStartJobProcessor` / `...RecordWorkProcessor` / `...SubmitJobProcessor` / `...CompleteJobProcessor` / `...HoldJobProcessor` / `...ResumeJobProcessor` / `...CancelJobProcessor`。facade delete-after-extract。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpMfgScheduleToJobCardProcessor` 2 D-mutation 拆分 → `ErpMfgScheduleToJobCardGenerateJobCardsFromScheduleProcessor` / `...GeneratePendingJobCardsProcessor`。`:45` 只读 `findWorkOrdersPendingJobCards` 迁回 BizModel（`@BizQuery`）使 facade 可 delete。facade delete-after-extract。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpMfgSubcontractOrderProcessor` 4 D-mutation 拆分 → `ErpMfgSubcontractOrderIssueMaterialsProcessor` / `...ReceiveFinishedProcessor` / `...PostProcessingFeeProcessor` / `...ReverseCompletionProcessor`。`cancel`（`:46` 单步翻转）保留 facade。facade slim-to-S-delegation-facade。
  - Skill: `nop-backend-dev`
- [ ] Add: `ErpMfgWorkOrderProcessor` 5 D-mutation 拆分 → `ErpMfgWorkOrderStartProcessor` / `...StopProcessor` / `...ResumeProcessor` / `...CloseProcessor` / `...ReportCompletionProcessor`。`checkAvailability`（`:45`）+ `cancel`（`:46`）保留 facade。facade slim-to-S-delegation-facade。
  - Skill: `nop-backend-dev`
- [ ] Add: beans.xml 注册全部 18 新 Processor bean（类别 A）。
  - Skill: `nop-backend-dev`
- [ ] Add: 类别 A BizModel 重配线——3 BizModel（JobCard/WorkOrder/SubcontractOrder）的 D-mutation `@BizMutation` 方法从 `@Inject facade` 改为 `@Inject` 对应 per-mutation Processor + 单行委托。delete-after-extract facade（JobCard/ScheduleToJobCard）删除后 BizModel 必须重配线才能编译。
  - Skill: `nop-backend-dev`
- [ ] Add: 补登 2 新豁免（WorkOrder.cancel + SubcontractOrder.cancel）到 `processor-per-mutation-exemption-registry.md`。
  - Skill: `nop-backend-dev`
- [ ] Proof: mfg service 本地编译通过（`mvn compile -pl module-manufacturing/erp-mfg-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 A 18 per-mutation 自包含 + 4 facade 瘦身/删除 + 3 BizModel 重配线 + 编译通过。

- [ ] 18 个新 Processor 文件存在且自包含
- [ ] 4 facade 按处置执行 + 3 BizModel D-mutation 重配线 + beans.xml 更新
- [ ] mfg service 本地编译通过

### Phase 2 - 类别 B BizModel 内联 mutation 拆分（7 BizModel → 11 per-mutation Processor）

Status: planned
Targets: `module-manufacturing/erp-mfg-service/.../processor/ErpMfg*Processor.java`（新建 11 文件）；7 BizModel `@BizMutation` 改单行委托
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: 11 个类别 B mutation 拆分——逐 BizModel 内联 `@BizMutation` 提取到 `<Entity><Method>Processor`，BizModel 改 `@Inject` Processor + 单行委托。完整清单（Entity.method → target Processor）：Bom.rollupCost、CostVariance.calculateVariances、CrpLoad.calculateLoad、MaterialIssue.confirm/reverseConfirm、MrpPlan.runMrp、MrpPlanLine.releasePurchaseRequest/releaseSubcontractRequest/releaseWorkRequest、MrpScenario.promoteToFormalPlan/runSimulation。
  - Skill: `nop-backend-dev`
- [ ] Add: beans.xml 注册全部 11 新 Processor bean（类别 B）。
  - Skill: `nop-backend-dev`
- [ ] Proof: mfg service 本地编译通过 + grep 确认 7 BizModel 内联 `@BizMutation` 已改为单行委托。
  - Skill: none

Exit Criteria:

> 本阶段交付类别 B 11 per-mutation 自包含 + 7 BizModel 改单行委托 + 编译通过。

- [ ] 11 个新 Processor 文件存在且自包含
- [ ] 7 BizModel 内联 `@BizMutation` 已改为单行委托（grep 确认）
- [ ] beans.xml 更新 + mfg service 本地编译通过

### Phase 3 - manufacturing 域运行时行为等价回归

Status: planned
Targets: `module-manufacturing/erp-mfg-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [ ] Proof: mfg 域 `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿（~144 测试，0 failures；已知 1 pre-existing error TestErpMfgCompletionPosting LOCATION_ID 经 git stash 证实与本 plan 无关）。类别 A + B mutation 行为等价。快照漂移仅限类名/堆栈，重录为新基线。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 mfg 域行为等价证据。

- [ ] mfg 域 `mvn test` 全绿（0 failures，排除已知 pre-existing）
- [ ] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: needs revision（task `ses_0474a912affe4ANb8DWN9XeOtQ`）—B1：SubcontractOrder 枚举含 2 行重复（issueMaterials/receiveFinished 各列 2 次），catA 20 实为 18 去重；另 `ErpMfgWorkOrderProcessor.cancel:158` + `ErpMfgSubcontractOrderProcessor.cancel:120` 是 `:46` 单步状态翻转未分类（须拆 vs 豁免未定）。total 31→29。B2：3 类别 A BizModel（JobCard/WorkOrder/SubcontractOrder）`@Inject` facade 委托 D-mutation，facade 删除/瘦身后断链，BizModel 重配线缺口。已修正：去重 catA 18 + 2 cancel 登记 `:46` 豁免 + 新增 BizModel 重配线 item + ScheduleToJobCard `:45` 查询迁 BizModel 使 facade 可删。
- Independent draft review iteration 2: accept（task `ses_04742e8aaffeOfjzM0zcYc6xXQ`）—catA 18 / total 29 一致性确认 + 2 cancel 登记 `:46` 豁免 + BizModel 重配线 item 已补。可转 active。

## Closure Gates

> 完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 mfg 域 + compliance + 全量编译。

- [ ] mfg 域 29 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（类别 A 18 + 类别 B 11）
- [ ] 4 类别 A facade 按处置执行（2 delete / 2 slim-to-S-delegation）
- [ ] 3 类别 A BizModel D-mutation 重配线为 `@Inject` per-mutation Processor 单行委托
- [ ] 7 类别 B BizModel 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托
- [ ] beans.xml 注册一致性（29 新 bean id 与 @Inject 匹配）
- [ ] 合法豁免 4 项（`ErpMfgForecast.approve/cancel` + `ErpMfgWorkOrder.cancel` + `ErpMfgSubcontractOrder.cancel`）保留 + registry 补登 2 项
- [ ] 业财过账语义不变（既有测试行为等价）
- [ ] `mvn compile` 全域通过 + `mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿（排除已知 pre-existing）
- [ ] compliance checker 基线不高于当前基线
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### TestErpMfgCompletionPosting LOCATION_ID pre-existing error

- Classification: `watch-only residual`
- Why Not Blocking Closure: 经 R5.5 git stash 证实 clean HEAD 同样失败（inventory 域 D-mutation 完工入库路径），与 per-mutation 拆分无关。
- Successor Required: `no`

## Closure

Status Note: _（待执行后填充）_

Closure Audit Evidence:

- Auditor / Agent: _（待独立结束审计）_
- Evidence: _（待填充）_

Follow-up:

- R6.0 triage 计数错误（SubcontractOrder 枚举重复行致 catA 20→18 去重 / total 31→29）须回填 roadmap §MR6 R6.2 行 + arm-index P1-MA3-062。
