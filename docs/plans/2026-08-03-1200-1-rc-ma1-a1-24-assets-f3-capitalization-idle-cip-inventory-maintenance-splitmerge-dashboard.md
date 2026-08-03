# 2026-08-03-1200-1 rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard assets-F3 资本化/闲置/转固/盘点/维修/拆分合并/看板需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.24（MA1 需求追踪矩阵审计 — assets-F3 资本化/拆分/盘点/维修/看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.24
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.24 的 0.2 依赖）、`2026-08-03-0900-2-rc-ma1-a1-22-assets-f1-depreciation-engine.md`（assets 域 F1 折旧引擎切片 active，A1.24 补齐 assets 域剩余 7 UC）、`2026-08-03-0900-3-rc-ma1-a1-23-assets-f2-disposal.md`（assets 域 F2 处置切片 active）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.24 给出 UC 清单 = `UC-AST-01/03/06/09/10/11/12`（7 UC），含 `use-cases.md:15/:50/:101/:147/:166/:185/:213` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/assets/use-cases.md`（机制见 `docs/design/assets/depreciation-and-posting.md` §二/§三/§四、`state-machine.md`、`docs/design/dashboards.md` §资产看板）：
  - UC-AST-01 设备购置资本化入账（`:15`）：卡片 DRAFT→IN_SERVICE（资本化）；生成入账凭证（借 固定资产，贷 在建工程/银行存款/应付）；自动生成折旧计划（按折旧方法/年限/残值）；卡片.已过账==true。
  - UC-AST-03 资产闲置停提与恢复（`:50`）：IN_SERVICE→IDLE（闲置）期间不参与折旧计提；IDLE→IN_SERVICE（恢复）恢复计提；闲置期间不计提（折旧计划跳过）。
  - UC-AST-06 在建工程转固（`:101`）：在建工程余额→结转到固定资产；生成转固凭证（借 固定资产，贷 在建工程）；转固后开始折旧（生成折旧计划）。
  - UC-AST-09 资产盘点（`:147`）：盘点单（范围：部门/类别）→录入实盘数量；差异=实盘−账面（卡片.数量）；盘盈→生成资产卡片增加（价值评估入账）；盘亏→触发处置流程（UC-AST-04 报废）或调查；盘点差异生成调整凭证（借/贷固定资产，差额）。
  - UC-AST-10 资产维修（`:166`）：维修单（关联资产卡片）→记录维修费用；若维修延长寿命/提升效能→资本化（增加原值，重算折旧计划）；否则→费用化（借 维修费用，贷 存货/银行）；资本化维修：卡片.原值+=资本化金额，折旧计划调整；维修费用可关联维护域（ErpMntVisit，若设备资产）。
  - UC-AST-11 资产拆分与合并（`:185`）：拆分—原卡片→拆为 N 张新卡片，新卡片.原值/累计折旧/净值按 proportion 分配，Σ新卡片.原值==原卡片.原值（平衡），原卡片状态→SCRAPPED 或保留（按配置），生成拆分凭证；合并—N 张卡片→合并为 1 张，新卡片.原值=Σ原卡片.原值，新卡片.累计折旧=Σ原卡片.累计折旧，原卡片状态→SCRAPPED，生成合并凭证；拆分/合并不影响总账平衡（总资产不变）。
  - UC-AST-12 资产看板（`:213`）：KPI 指标数据源正确（实时聚合，非硬编码）；KPI 卡片值==对应实体的实时聚合（按期间/orgId/权限过滤）—原值/累计折旧/净值，本期折旧，类别分布，折旧未计提预警；预警项==满足阈值条件的记录（阈值来自系统配置，非硬编码）；看板数据受行级权限约束（只看自己组织/部门/成本中心）。

- **L3 代码实现现状（实测）**——6/7 UC 已实现，UC-AST-03 闲置停提为方案 A doc-only Deferred（须 §4 三判据复核）：
  - **UC-AST-01 资本化入账**（✅ 已实现）：`ErpAstAssetCapitalizationBizModel.java:17-28`（thin Facade）；`ErpAstAssetCapitalizationProcessor.java:65 approve()`→`:69 executeApprove()`：`:70 createAndActivateAsset()`（`:201 setStatus(IN_SERVICE)` + 设 originalValue/currentValue/netBookValue/residualValue/usefulLifeMonths）→`:71 generateDepreciationSchedule()`（`:206-241` 从 capitalizationDate.plusMonths(1) 生成 N 月 PENDING 计划，直线法含末期残值修正 `:243-253`）→`:74 postingDispatcher.tryPost(cap)`。reverse `:92-115`。跨域 `CapitalizationPostingDispatcher.java:55 tryPost()`→`AssetPostingExecutor.java:27 postEvent()`→`IErpFinVoucherBiz.post()`；失败 `IErpSysNotificationBiz.notify("ast.capitalization-posting-failure",...)`（`:46,72-88`）。
  - **UC-AST-03 闲置停提与恢复**（⚠️ 方案 A doc-only Deferred，最高风险缺口）：`ErpAstConstants.java:67 ASSET_STATUS_IDLE="IDLE"` 但 `setStatus(IDLE)` 在 `src/main` **零 writer**（5 处 read-only guard/filter：`ErpAstValueAdjustmentProcessor:196`、`ErpAstDisposalProcessor:194`、`ErpAstInventoryProcessor:113`）；无 `pauseDepreciation`/`resumeDepreciation`/`setIdle`/`markIdle`/`suspend` BizMutation（grep 零匹配）；`ErpAstAssetBizModel.java:11-17` 17 行 CRUD 桩（`ErpAstAsset.xbiz` `<actions/>` 空）；折旧引擎 `ErpAstDepreciationScheduleExecuteDepreciationProcessor.java:40 validateAssetInService()` + `ExecuteBatchDepreciationProcessor:42` 仅查 `IN_SERVICE`（IDLE 被静默跳过≈pause 语义，但无 resume 动作）。既有 finding **P1-MA2-061**（`arm-index.md:300`）标记 ✅ resolved（R1.18 done）但 resolution path = **方案 A（owner doc Deferred 标注 only，`state-machine.md §1/§2/§8` annotated Deferred + 维持 dict IDLE 项），无任何代码新增**。须复核 §4 三判据：(i) plan-audit 通过记录 / (ii) owner doc 显式 documented simplification + 人工批准痕迹 / (iii) product-scope 范围裁剪登记。
  - **UC-AST-06 在建工程转固**（✅ 已实现）：`ErpAstCipBizModel.java:33-120`（Facade 6 mutations：`:60 startConstruction` / `:66 addCostItem` / `:79 addProgressBilling` / `:106 transferToAsset(cipId,costItemIds,transferDate)` / `:115 reverseTransfer`）；`ErpAstCipTransferToAssetProcessor.java:22-34 transferToAsset()`（6 步：requireCip→resolveCostItems→validateTransferable→buildCapitalizationRequest→doTransfer 创建 ErpAstAssetCapitalization→postProcess，支持 costItemIds 白名单 partial transfer）；doTransfer 复用 UC-AST-01 资本化链 → `CapitalizationPostingDispatcher`（creditSubject 默认 1603 CIP，`:127`）。
  - **UC-AST-09 资产盘点**（✅ 已实现，含实现偏离记录）：`ErpAstInventoryBizModel.java:26-104`（8 mutations）；`ErpAstInventoryProcessor.java:42`（409L，`:90 expandAssetsToLines()` 范围扩展 filter status IN (IN_SERVICE,IDLE) `:111-114`，range 字段 orgId/rangeDepartmentId/rangeCategoryId/rangeLocationId）+ 7 per-mutation processors（R6.3 split）。跨域过账 `AssetInventoryPostingDispatcher` → businessType `ASSET_INVENTORY_ADJUSTMENT(460)` + `AssetInventoryAcctDocProvider`（盘盈 借1601/贷6301，盘亏 借6711/贷1601）。**实现偏离**（`docs/design/assets/inventory.md §四/§八`）：盘盈/盘亏处置链复用收窄为直接建卡/SCRAPPED（避免与 CAPITALIZATION/DISPOSAL 凭证双重过账）。
  - **UC-AST-10 资产维修**（✅ 已实现）：`ErpAstMaintenanceBizModel.java:33-131`（9 mutations：`:70 createMaintenance(assetId,code,name,businessDate,maintenanceVisitId,reason)` 可选关联维护域 / `:101 decideTreatment(treatment,capitalizedAmount)` CAPITALIZE/EXPENSE 裁决 / `:110 approve` / `:116 post` / `:128 reverse`）；`ErpAstMaintenanceProcessor.java` + 8 per-mutation processors（R6.3 split）；六态状态机 DRAFT/SUBMITTED/IN_PROGRESS/COMPLETED/POSTED+CANCELLED。跨域 `MaintenanceExpensePostingDispatcher`(470) + `MaintenanceCapitalizationPostingDispatcher`(480) → `IErpFinVoucherBiz.post()`；资本化路径 `IErpAstDepreciationScheduleBiz.recalculateForCapitalizationMaintenance(assetId,increment)` 加性扩展重算折旧。weak link `maintenanceVisitId`→`ErpMntVisit`（assets→maintenance R-only）。
  - **UC-AST-11 拆分与合并**（✅ 已实现）：`ErpAstSplitBizModel.java:24-39` + `ErpAstMergeBizModel.java:24-39`（Facade）；`ErpAstSplitProcessor.java:46-546`（`:89 executeApprove()` 6 步：validateBeforeExecute→`:95 computeAllocation()` 含 PROPORTION_TOLERANCE=0.000001 + AMOUNT_TOLERANCE=0.01 + max-item residual fix→`:98 createTargetAssets()` N 张新 ErpAstAsset+折旧计划 target IN_SERVICE `:368`→`:101 disposeSourceAsset()` source→DISPOSED NBV=0→`:112 doPost()`→postProcess）+ `ErpAstMergeProcessor.java`（486L，对称 `:329 target IN_SERVICE`）+ 12 per-mutation processors。跨域 `AssetSplitPostingDispatcher` + `AssetMergePostingDispatcher` → `IErpFinVoucherBiz.post()`。**特殊契约**：`reverseApprove` 抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`/`ERR_AST_MERGE_REVERSE_NOT_SUPPORTED`（不可逆，owner doc `split-merge.md §关键业务规则 5`，BizModel javadoc `:21-22`）。
  - **UC-AST-12 资产看板**（✅ 已实现，caveat：行级权限）：`ErpAstDashboardBizModel.java:47-229`（service-type BizObject，4 @BizQuery：`:56 getDashboardKpi(periodId)` → originalValue/accumulatedDepreciation/netBookValue/periodDepreciation/cipBalance / `:84 getAssetCategoryDistribution()` / `:111 getDashboardTrend(months=12)` / `:141 findDepreciationMissingAlert()` IN_SERVICE 无 EXECUTED 折旧计划资产）。纯域内读聚合（ErpAstAsset/Category/DepreciationSchedule/Cip），无跨域调用。**caveat**：`loadInServiceAssets():176-181` 发出无 orgId scope 的 QueryBean——跨组织/多账套部署会跨域聚合（项目级 P1-MA2-093/P1-MA3-007 覆盖，非 assets 独有）。
  - **跨域 Facade 汇总**：`IErpFinVoucherBiz`（post/reverse，经 AssetPostingExecutor）；`IErpAstDepreciationScheduleBiz`（recalculateForCapitalizationMaintenance）；`IErpAstAssetCapitalizationBiz`/`IErpAstDisposalBiz`（盘点盘盈/盘亏复用 hook）；`IErpSysNotificationBiz`（过账失败通知）；`IErpMdSubjectBiz`/`IErpMdAcctSchemaBiz`（读）。

- **L4 测试证据现状**（`module-assets/erp-ast-service/src/test/`）：UC-AST-01 `TestErpAstCapitalization`（asserts IN_SERVICE `:83`）+ `TestErpAstPostingReverse` + `TestErpAstAcctDocProviderAccountKey`；UC-AST-03 **无测试**（IDLE 仅 `TestErpAstDashboard.java:142` 用作 filter 测试数据，无 pause/resume 行为测试）；UC-AST-06 `TestErpAstCipTransfer`（asserts IN_SERVICE `:104`）；UC-AST-09 `TestErpAstInventory`（5 @Test，surplus 新卡 IN_SERVICE `:130`）；UC-AST-10 `TestErpAstMaintenance`（12 @Test per plan `2026-07-07-0842-2`）；UC-AST-11 `TestErpAstSplitMerge`（平衡 + target IN_SERVICE `:119,:377`）；UC-AST-12 `TestErpAstDashboard`。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`（A2.10）：assets 状态机审计 done；**P1-MA2-061**（`:300`，IDLE 状态机迁移完全未实现，resolved R1.18 doc-only Deferred）；P2-MA1-023（`:481`，owner doc 缺 DISPOSED）；P2-MA1-024（`:482`，缺 CANCELLED）；P2-MA2-059（`:533`，缺 7 业务单据状态机章节）；P2-MA2-060（`:534`，6 PROC+1 INLINE 实现模式未文档化）；P2-MA2-061（`:535`，6 业务单据 Processor cancel 死代码）。
  - `docs/audits/2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md`（A4.3）：48 Processor 最高密度代码质量审计 done。
  - `docs/audits/2026-07-29-1430-arm-ma5-assets-test-coverage.md`（A5.4）：assets 测试覆盖 done。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（UC-AST-03 闲置 Deferred 复核 §4 三判据 / 拆分合并不可逆性是否 L1 要求 / 盘点实现偏离是否 L1 允许 / 看板行级权限 caveat / 维修资本化重算正确性等）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-061`（IDLE resolved R1.18 doc-only Deferred，`:300`）、`P1-MA2-060`（Cap/Disposal tryPost 吞异常 resolved R1.16，`:299`——**直接关系 UC-AST-01 资本化过账健壮性**）、`P1-MA1-008`（8 ast 实体缺 propId fixed，`:249`）、`P2-MA2-061`（6 业务单据 cancel 死代码 watch-only，`:535`——涉及 Capitalization/Split/Merge/Disposal/ValueAdjustment/Inventory/Maintenance）、`P2-MA1-023`/`P2-MA1-024`（owner doc 状态机 drift watch-only）。**RC 系列对 assets 为零**——A1.22/A1.23 active 但未完成，A1.24 为 assets 域第三个 RC 切片。本切片新发现的静默缺口（UC-AST-03 闲置 Deferred §4 复核 / UC-AST-11 不可逆性 / UC-AST-09 实现偏离 / UC-AST-12 行级权限）须按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；触及会计过账逻辑（资本化凭证/维修资本化重算/盘点调整凭证）或 ORM 结构（补 idle BizMutation/拆分合并 reverse）的修复行须 ask-first（§5 保护区域暂停协议）。UC-AST-03 闲置 Deferred（P1-MA2-061 resolved R1.18 doc-only）须复核 §4 三判据，若不符合则重新打开为 P1 入 MR1。

- **剩余差距**：A1.24 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.24 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.24 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-24-assets-f3-<slug>.md`，含方法论 §6 **9 段全部内容**。
- 对 7 UC（UC-AST-01/03/06/09/10/11/12）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：#1 UC-AST-03 闲置停提 Deferred（复核 P1-MA2-061 是否满足 §4 三判据——R1.18 doc-only 标注是否有人工批准痕迹，不满足则重新打开 P1 入 MR1）、#2 UC-AST-11 拆分合并不可逆性（L1 未要求 reverse，确认接受）、#3 UC-AST-09 盘点实现偏离（盘盈/盘亏链复用收窄，owner doc `inventory.md §四/§八` 有显式偏离记录，复核是否 L1 允许）、#4 UC-AST-12 看板行级权限 caveat（orgId 未 scope，项目级 P1-MA2-093 覆盖）、#5 UC-AST-10 维修资本化折旧重算正确性（`recalculateForCapitalizationMaintenance` 加性扩展是否满足 L1"折旧计划调整"）、#6 UC-AST-01 资本化折旧计划生成（直线法 + 末期残值修正是否满足 L1"按折旧方法/年限/残值"）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。对既有 documented simplification（#1）按 §4 复核人工批准证据，若不符合三判据则重新打开并入 MR1。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；既有行追加 RC 注记）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/depreciation-and-posting.md/state-machine.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.22 折旧引擎 / A1.23 处置各自独立 plan active；A1.24 只覆盖 UC-AST-01/03/06/09/10/11/12）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：资产状态机/代码质量/测试覆盖由 A2.10/A4.3/A5.4 证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据，**§4 三判据为本切片复核 P1-MA2-061 doc-only Deferred 的关键**）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.24 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.24 UC 锚点）+ `docs/design/assets/use-cases.md`（L1 真相源）+ `docs/design/assets/depreciation-and-posting.md`（L2 §二资本化/§三处置/§四价值调整，非真相源）+ `docs/design/assets/state-machine.md`（L2 §1/§2/§8）+ `docs/design/assets/inventory.md`（L2 §四/§八盘点偏离）+ `docs/design/assets/maintenance.md`（L2 维修）+ `docs/design/assets/split-merge.md`（L2 §关键业务规则 5）+ `docs/design/dashboards.md`（L2 §资产看板）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/A4/A5 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstCapitalization,TestErpAstInventory,TestErpAstMaintenance,TestErpAstSplitMerge,TestErpAstCipTransfer`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-03-1200-1-rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-AST-01/03/06/09/10/11/12 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:15/:50/:101/:147/:166/:185/:213` 验收标准原文；L2 引用 `depreciation-and-posting.md` §二/§三/§四、`state-machine.md` §1/§2/§8、`inventory.md` §四/§八、`maintenance.md`、`split-merge.md` §关键业务规则 5、`dashboards.md` §资产看板（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-assets/.../ErpAstAssetCapitalizationBizModel.java` / `ErpAstAssetCapitalizationProcessor.java` / `ErpAstCipBizModel.java` / `ErpAstCipTransferToAssetProcessor.java` / `ErpAstInventoryBizModel.java` / `ErpAstInventoryProcessor.java` / `ErpAstMaintenanceBizModel.java` / `ErpAstMaintenanceProcessor.java` / `ErpAstSplitBizModel.java` / `ErpAstMergeBizModel.java` / `ErpAstSplitProcessor.java` / `ErpAstMergeProcessor.java` / `ErpAstDashboardBizModel.java`（含行号 + 跨域 `IErpFinVoucherBiz`/`IErpAstDepreciationScheduleBiz`/`IErpSysNotificationBiz`）；L4 引用 `TestErpAst*.java#method`（注明断言强度）；L5 复用 MA2 A2.10/A4.3/A5.4 + E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-AST-01 资本化入账（DRAFT→IN_SERVICE + 入账凭证 + 折旧计划生成，已实现 `ErpAstAssetCapitalizationProcessor:69-74`）；②#1 UC-AST-03 闲置停提（`ASSET_STATUS_IDLE` 零 writer + 无 BizMutation + 折旧引擎仅查 IN_SERVICE——复核 P1-MA2-061 R1.18 doc-only Deferred 是否满足 §4 三判据）；③UC-AST-06 转固（`ErpAstCipBizModel.transferToAsset:106` + 复用资本化链，已实现）；④#3 UC-AST-09 盘点实现偏离（盘盈直接建卡/盘亏 SCRAPPED，owner doc `inventory.md §四/§八` 显式记录偏离——复核是否 L1 允许）；⑤#5 UC-AST-10 维修资本化折旧重算（`recalculateForCapitalizationMaintenance` 加性扩展——复核是否满足 L1"折旧计划调整"语义）；⑥#2 UC-AST-11 拆分合并不可逆（`ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`——L1 未要求 reverse，确认接受）；⑦UC-AST-11 拆分平衡（PROPORTION_TOLERANCE + max-item residual fix——复核 Σ平衡是否满足 L1）；⑧#4 UC-AST-12 看板行级权限（`loadInServiceAssets:176-181` 无 orgId scope——项目级 P1-MA2-093 覆盖，复核 assets 视角增量）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：#1 UC-AST-03 闲置 Deferred 复核 P1-MA2-061 §4 三判据——R1.18 resolution 为 owner doc Deferred 标注 only，须核实是否有人工批准痕迹（git log / commit message / 讨论文档），不满足 (i)/(ii) 则按 §4 重新打开为 P1 入 MR1（须人工确认是否 product-scope 范围裁剪）；#3 盘点偏离若有 owner doc 显式记录且 L1 未禁止则倾向 P2；#5 维修重算若语义等价则倾向接受；#2 不可逆性若 L1 无 reverse 要求则接受；#4 看板行级权限若 P1-MA2-093 已覆盖则 watch-only。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-AST-01/03/06/09/10/11/12 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.10/A4.3/A5.4 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#6 有明确分级（非悬空"待查"）；#1 P1-MA2-061 §4 三判据复核结论已记录（满足/不满足）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-03-1200-1-rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` assets 同域同控制点（如 P1-MA2-061 IDLE、P2-MA2-061 cancel 死代码、P2-MA1-023/024 状态机 drift）后裁决——同根因同控制点 → 复用（追加 RC 注记）；新根因 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）；记录 #1 P1-MA2-061 §4 复核结论（满足三判据则维持 resolved watch-only；不满足则按 §4 重新打开为 P1 入 MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如资本化折旧计划末期残值修正的实际取值行为、维修资本化重算后折旧计划行 PENDING→EXECUTED 的迁移正确性、拆分 proportion tolerance 在极端比例下的平衡行为等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-assets-state-machine.md`（A2.10 状态机 + P1-MA2-061 IDLE resolved）+ `2026-07-29-0024-...-code-quality.md`（A4.3 代码质量 PASS）+ `2026-07-29-1430-...-test-coverage.md`（A5.4 测试覆盖），列明只补的需求视角差异（UC-AST-03 闲置 Deferred §4 复核 / 盘点偏离 / 维修重算 / 拆分平衡 / 看板行级权限 / 不可逆性）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；既有行（P1-MA2-061）追加 RC 复核注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据；#1 复核结论已记录
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03af71030ffeuekIpLF3RI3tep，fresh session，未起草本计划）。规则 1-13 全 PASS：(1) Deps A1.24=0.2 done；(2) 单结果表面（A1.24 报告 UC-AST-01/03/06/09/10/11/12，无 A1.22/1.23 范围泄漏）；(3) 格式 + 命名合规（N=1）；(4) UC 覆盖精确（baseline-inventory:358）；(5) Baseline spot-check 全 CONFIRMED——`ErpAstConstants.java:67 ASSET_STATUS_IDLE="IDLE"` + `setStatus(IDLE)` 零 writer（rg 确认 4 命中 = 1 def + 3 read-only guard）/ `ErpAstAssetBizModel.java` 17 行 CRUD 桩 + `ErpAstAsset.xbiz:4 <actions/>` 空 / `ErpAstAssetCapitalizationProcessor:65-74` 资本化链行号精确 / `ErpAstInventoryProcessor:42,:90,:111-114` 精确 / arm-index P1-MA2-061:300 内容匹配 / grep `pauseDepreciation|resumeDepreciation|setIdle|markIdle|suspend` 零匹配；(6) 方法论 §1-§10 + §去重 + §4 三判据 + §7 对齐；(7) 反松弛；(8) Q4 vs documented simplification 正确——UC-AST-03 P1-MA2-061 §4 三判据复核 wired 进 Goals + Phase 1 Decision + Phase 2 + 两段 exit + Closure Gates，双向逻辑（满足则维持 watch-only；不满足则重新打开 P1 入 MR1）；(9) Closure Gates audit-only 有据；(10) Non-Goals 守约；(11) 不自决范围——UC-AST-03 idle 须人工确认 product-scope 裁剪。Non-blocking（informational，无需修订）："5 处 read-only guard"计数继承自 arm-index P1-MA2-061 原文（执行时 grep 会校正确实数）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.24 报告 9 段齐全 + 7 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议 + §4 三判据一致；与 rc-requirement-baseline-inventory A1.24 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯 + documented simplification 复核结论可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；触及会计过账逻辑（资本化凭证/维修资本化重算/盘点调整凭证）或 ORM 结构（补 idle BizMutation/拆分合并 reverse）的修复行须 ask-first + 独立 plan-audit（§5）。#1 UC-AST-03 闲置 Deferred 须人工确认是否为 product-scope 范围裁剪（若裁剪则改真相源非降级；若未裁剪则 P1 强制实现）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；#1 待人工确认 product-scope 范围 + §4 三判据复核）

## Closure

Status Note: 全部 2 个 Phase 执行完成。A1.24 切片审计报告 `docs/audits/2026-08-03-1200-1-rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard.md` 已落盘（9 段齐全，§1-§9）。7 UC 五级追踪矩阵填齐：UC-AST-01 接受 / UC-AST-03 P1（reuse P1-MA2-061 §4 复核倾向重开须人工确认）/ UC-AST-06 接受 / UC-AST-09 接受 on ①②⑤ + P2 on ③④（new P2-RC-028 watch-only）/ UC-AST-10 接受 / UC-AST-11 接受含 caveat（reuse P2-MA1-023 DISPOSED drift）/ UC-AST-12 接受 on ①② + ③ reuse P1-MA2-093。零 P0。arm-index 已更新（A1.24 报告行 + P2-RC-028 finding 行 + A1.24 RC 交叉引用注记）。§4 三判据复核 P1-MA2-061 结论：在"人工批准"意义上不满足（(i) AI 子代理审计 ≠ 人工批准 + (ii) 无人工批准痕迹 + (iii) product-scope 未裁剪）→ 倾向重开 P1 入 MR1，须人工确认 product-scope 是否裁剪 IDLE。纯审计报告（无代码/无 ORM），验证聚焦报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + checker actual vs baseline 实测 + finding 复用/新增裁决可追溯。独立结束审计已由独立子代理执行并通过（见下方 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，fresh session，未起草本计划、未执行任何 Phase item）。本次为 SCRIPT_CHECK_RESULT=FAIL 触发的修正闭环（执行者依规则 12 不得自勾结束审计门控，正确保留为 `[ ]`；本会话以独立审计身份核实后勾选）。
- Evidence: 6 项语义验证全通过——(1) **Phase 状态/项一致性**：Phase 1/2 全 `Status: completed`，阶段体内零残留 `- [ ]`（执行项 3+7 项 + Exit Criteria 2+3 项全 `[x]`；唯一历史 `- [ ]` 为本结束审计门控，由本独立会话勾选）；(2) **Exit Criteria vs 实时仓库**：报告 `docs/audits/2026-08-03-1200-1-rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard.md` 存在且 §1-§9 9 段齐全（实测 grep）；arm-index.md 实测含 A1.24 报告清单行（`:92`）+ P2-RC-028 finding 行（`:157`）+ A1.24 RC 交叉引用注记段（`:179`）；关键代码断言 rg 复核全 CONFIRMED——`ErpAstConstants.java:67 ASSET_STATUS_IDLE="IDLE"` + `setStatus(*IDLE*)` src/main **零 writer**（rg 零命中）+ 3 read-only guard（`ErpAstValueAdjustmentProcessor:196`/`ErpAstInventoryProcessor:113`/`ErpAstDisposalProcessor:194`）+ `ErpAstSplitProcessor.java:48 PROPORTION_TOLERANCE=0.000001`/`:49 AMOUNT_TOLERANCE=0.01` + `ErpAstSplitReverseApproveProcessor:24 throw ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED` + `ErpAstMergeReverseApproveProcessor:24 throw ERR_AST_MERGE_REVERSE_NOT_SUPPORTED` + `ErpAstDashboardBizModel.java:176-181 loadInServiceAssets()` 仅 `eq("status", IN_SERVICE)` **无 orgId scope**（IServiceContext 收而不用）+ `recalculateForCapitalizationMaintenance` 经 `ErpAstMaintenanceProcessor:96` 实际调用；(3) **Anti-Hollow**：本计划为只读审计（无新代码），交付物=报告 + arm-index 登记，均含逐 UC/逐 finding 的实仓行号 + 三源对照 + 裁决依据，非 `{}`/`return null` 占位；§7 静态存疑点清单 5 项均带"静态结论 + A4.1 运行时展开计划"；(4) **五点一致性**：Plan Status=completed / Phase 1+2 Status=completed / 全 Exit Criteria `[x]` / Closure Gates（本审计后全 `[x]` 8/8）/ Closure 证据（Status Note 具体 + 本审计 evidence）全一致；(5) **Deferred honesty**：`Deferred But Adjudicated` 段诚实登记 finding 修复属 MR0/MR1 successor（Successor Required: yes），#1 UC-AST-03 IDLE P1 未隐藏于 Deferred 而是公开 reuse P1-MA2-061 + 显式标注"须人工确认 product-scope 是否裁剪"（裁剪→§4(iii) 改真相源非降级；未裁剪→P1 强制实现 suspend/resume），P2-RC-028 watch-only 公开登记；零范围内项目降级为 deferred/follow-up；(6) **Docs sync**：`docs/logs/2026/08-03.md` 已更新（5-12 行记录 EXECUTE Phase 1/2 + 产出文件 + bookkeeping + successor），本审计为只读故无 `docs/architecture/` 更新义务。结论：passes closure audit，批准关闭。

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围
- UC-AST-03 闲置 Deferred 须人工确认 product-scope 是否范围裁剪 + §4 三判据复核 P1-MA2-061 人工批准痕迹
