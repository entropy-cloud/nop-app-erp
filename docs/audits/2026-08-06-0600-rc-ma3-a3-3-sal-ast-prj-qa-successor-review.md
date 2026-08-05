# rc-ma3-a3-3-sal-ast-prj-qa-successor-review sales+assets+projects+quality MA3 successor 追踪完整性与回队复查报告（A3.3）

> Plan Status: completed
> 产出时间：2026-08-06
> 来源 Plan：`docs/plans/2026-08-06-0442-3-rc-ma3-a3-3-sal-ast-prj-qa-successor-review.md`（Work Item A3.3）
> Mission：requirement-compliance（MA3 successor 触发条件复查）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（A3.3 sales+assets+projects+quality 域 successor 复查 + Work Item Details MA3）
> 复查全集：`docs/audits/rc-existing-inventory.md`（§successor 三源对账清单 sales+assets+projects+quality 域分组 — 5 项 + §对账差异登记 #5）
> Skill：`docs/skills/open-ended-audit-prompt.md`
> 审计性质：**只读审计**——读 arm-index / owner doc / backlog README / 实仓代码 / config / ORM 裁决 successor 触发条件，**不修改任何代码/ORM/api.xml/真相源**

---

## §复查口径与 Q4 修复义务边界

本报告复查对象 = M0.3（`rc-existing-inventory.md` §successor 三源对账清单）导出的 sales+assets+projects+quality 域 design-level successor 去重并集 **5 项**。逐项完成方法论 §MA3 四任务：① 触发条件是否已满足（grep 实仓代码/config/ORM 字段验证）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog successor）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

**Q4 修复义务边界（§5）**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。本复查 5 项触发条件**全部未满足**（grep 实仓逐项证实）→ 5 项**全部维持 backlog successor**，无回队 MR1。

**finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x），也不重审 finding 是否修复（属 A1.x→MR1）。即：successor 回队与否（A3.x）≠ finding 是否修复（A2.x/A1.x→MR1），两者各自裁决、交叉引用不冲突。本复查的 §对账差异登记 #5 处置（区分「finding 已修复/关闭」与「successor 仍待触发」）正是此原则的体现：

- #2 `P1-MA2-061`（IDLE）：**finding 经 R1.18 resolved-via-deferral**（owner doc Deferred 标注，属 A2.6 RC 复查范围）；其 **successor**（IDLE 状态机迁移 + 折旧引擎扩展）触发条件未满足 → 本 A3.3 维持 backlog successor。两者各自裁决不冲突。
- #3 `P1-MA2-043`（工时单）：**finding 经 R1.15 resolved-via-deferral**（方案 B Deferred，A1.14 RC 从 L1 视角重开经 MR1[属 A1.x 裁决]）；其 **successor**（工时归集 projects/cost-collection）触发条件未满足 → 本 A3.3 维持 backlog successor。
- #4 `P1-MA2-064`（质检联动）：**finding 经 R1.20 resolved-via-deferral**（A1.31 RC 从 L1 视角登记 P1-RC-041[属 A1.x 裁决]）；其 **successor**（quality cancel 回调接线）触发条件未满足 → 本 A3.3 维持 backlog successor。
- #5 `P1-MA6-002`（employee-id）：**finding 经 R3.4 部分实现修复**（userId 域子集：sales 业务员 createdBy + quality RiskRegister ownerId；属 A6.3/MR3 裁决）；其 **successor**（employee-id 域子集：quality inspectorId / maintenance assignedTo）触发条件未满足[ErpMdEmployee 无 userId 列] → 本 A3.3 维持 backlog successor。
- #1 `P2-MA2-013`（订单维度核销）：**finding watch-only 未关闭**（arm-index :699 无 resolution 标记，属 A2.5 RC 空集认证范围）；其 **successor**（订单+发票双维度语义）触发条件未满足 → 本 A3.3 维持 backlog successor。

---

## 1. successor 三源对账清单（sales+assets+projects+quality 域，段 1，§6 MA3 适配）

> 三源：S1 = `docs/audits/arm-index.md` 行内 successor/触发条件声明 / S2 = owner doc 内嵌 successor / Deferred 段落 / S3 = `docs/backlog/README.md` 既有追踪行。

| # | successor 项 | 域 | 三源覆盖 | 触发条件摘要 | 复杂度 | A2.x 关闭裁决交叉（two-faces） |
|---|-------------|----|---------|-------------|--------|------------------------------|
| 1 | 订单维度核销（receipt prepayment against order） | sales | S1+S2 | owner doc §L3 订单+发票双维度语义恢复（报表/核销需求驱动） | A | `P2-MA2-013`（A2.5 空集认证：watch-only 未关闭，owner doc 标注本期仅发票维度） |
| 2 | 资产 IDLE 闲置状态机迁移 + 折旧扩展 | assets | S1+S2 | 资产暂停/恢复业务上线时（PM 要求正式资产闲置/恢复工作流） | A | `P1-MA2-061`（A2.6 空集认证：resolved-via-deferral R1.18，§4 三判据复核归 A1.24 RC） |
| 3 | 工时单 approve/reject + 工时归集（projects/cost-collection） | projects（hr 域投影） | S1 | projects 工时归集 successor（hr 工时单 APPROVED→归集 projects 成本） | S | `P1-MA2-043`（A2.4 空集认证：resolved-via-deferral R1.15，§4 三判据复核归 A1.14 RC） |
| 4 | 业务单据作废联动取消质检单 | quality | S1+S2 | quality 域 cancel 回调接线（业务作废自动取消质检需求时） | A | `P1-MA2-064`（A2.7 空集认证：resolved-via-deferral R1.20，§4 三判据复核归 A1.31 RC P1-RC-041） |
| 5 | employee-id 行过滤（quality inspectorId / maintenance assignedTo） | quality(+mnt 跨域) | S1 | ErpMdEmployee 增 userId 列 + 解析器（**ask-first ORM**） | A | `P1-MA6-002`（A6.3：MR3 done R3.4 实现修复 userId 域子集，employee-id 子集为 successor） |

> **§对账差异登记 #5 覆盖**：#2/#3/#4/#5 四项的「finding 已修复/关闭」与「successor 仍待触发」区分见 §复查口径段（上）。#1 为 watch-only 未关闭 finding，无 §对账差异 #5 张力。
>
> **#5 跨 quality+maintenance 两域投影去重**：按 §去重协议，#5 同一控制点（ErpMdEmployee userId 列 + employee-id→userId 解析器）在 quality（inspectorId）+ maintenance（assignedTo）两域投影，本复查**合并裁决一次**（不重复登记）。A3.5（扩展域含 maintenance）复查时交叉引用本 A3.3 §2.5/#5 结论，不重复裁决。

---

## 2. 逐项四任务核证（段 2，§6 MA3 适配）

> 四任务：① 触发条件是否已满足（grep 实仓代码/config/ORM）；② 是否该回队；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

### 2.1 #1 订单维度核销（receipt prepayment against order）— sales

- **① 触发条件状态**：**未满足**。实仓 grep `ReceiptSettler.java` 全方法均按 **invoiceId 维度**核销（`:106` touchedInvoices / `:107,161,163,196,198,216,219` 全 invoiceId 过滤；`requireInvoiceForSettle:141` 强制 invoiceId 校验；`reverseSettlement:116,129,134` 同 invoiceId 维度）——**无 orderId 维度核销路径**（receipt prepayment against order before invoice 未实现）。`SettlementAllocation`（master-data `erp-md-dao`）同样按 invoiceId 维度承载。owner doc `flow-overview.md:157`「收款核销按订单/发票维度」声明订单+发票双维度，实现仅发票维度（与 `P2-MA2-013` arm-index 描述一致）。触发条件 = 「报表/核销需求驱动订单+发票双维度语义恢复」，该需求**未触发**（无业务路径驱动订单维度核销）。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——订单维度核销业务需求未驱动；属 sales 域核销维度扩展，修复归 MR1 而非本审计）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P2-MA2-013` 行 + owner doc `flow-overview.md:157` 均存在）。
- **④ README 覆盖复核**：`docs/backlog/README.md` 无独立「订单维度核销」design successor 行（README 81 行经 M0.3 §对账差异登记 #4 核实为 E2E 测试 successor，非 design successor；本域 README 行如核销汇兑损益 watch-only residual `:78` / AR-AP 核销 E2E `:53,:56` 为 E2E + watch-only，非本 design successor）。design successor 经 S1（arm-index `P2-MA2-013` 行）+ S2（owner doc）覆盖，**无「已登记但从未触发」风险**（触发条件「报表/核销需求驱动」未触发明确，未误标 done）。

### 2.2 #2 资产 IDLE 闲置状态机迁移 + 折旧扩展 — assets

- **① 触发条件状态**：**未满足**。实仓 grep 全 `module-assets/erp-ast-service/src/main` `ASSET_STATUS_IDLE`：常量定义于 `ErpAstConstants.java:67` + `_ErpAstDaoConstants.java:19`，但**零 `setStatus(ASSET_STATUS_IDLE)` writer**——仅 3 处只读引用（`ErpAstValueAdjustmentProcessor:196` + `ErpAstDisposalProcessor:194` 守卫读取 + `ErpAstInventoryProcessor:113` 盘点范围过滤）。无 suspend/resume/setIdle/toIdle/fromIdle BizMutation 方法。折旧引擎 `ErpAstDepreciationScheduleProcessor.executeDepreciation:70-71` 守卫 `assetStatus == IN_SERVICE` 否则抛 `ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE`——**折旧引擎仅查询 IN_SERVICE，IDLE 不参与折旧批量**。触发条件 = 「PM 要求正式资产闲置/恢复工作流时」（owner doc `state-machine.md §46` Successor 触发条件原文），该业务**未上线**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——资产暂停/恢复业务未上线；修复触及折旧引擎扩展查询 IN_SERVICE+IDLE + 新增 BizMutation，属代码逻辑类但语义敏感，修复归 MR1 而非本审计）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA2-061` 行 + owner doc `state-machine.md §1:19/§2:41-42/§46` 显式 Deferred + Successor 触发条件标注最详）。
- **④ README 覆盖复核**：无独立 design successor 行（README assets 域行为 E2E 测试 successor，非本 design successor）。S1+S2 覆盖充分，无悬空。
- **结构性约束标注（§对账差异 #5）**：`P1-MA2-061` **finding 已 resolved-via-deferral**（R1.18，owner doc Deferred 标注，非方案 A 实现）。本 A3.3 裁决的是 **successor 触发条件**（未满足→维持 backlog），**不重审** finding 关闭裁决（归 A2.6 RC 空集认证 + A1.24 RC §4 三判据复核倾向重开——属 A1.x/A2.x 通道，与本 successor 裁决各自独立交叉不冲突）。

### 2.3 #3 工时单 approve/reject + 工时归集（projects/cost-collection）— projects（hr 域投影）

- **① 触发条件状态**：**未满足**。实仓 grep `module-hr/erp-hr-service/src/main` `ErpHrTimesheetBizModel.java` **仅 `submit:36-37`（@BizMutation，DRAFT→SUBMITTED）**——无 approve/reject BizMutation 方法（grep `approve|reject` × timesheet 零业务命中）。hr 工时单 APPROVED/REJECTED 为 dict 死状态（无 setStatus writer）。owner doc `hr/state-machine.md §适用对象三 工时表` 声明 DRAFT→SUBMITTED→APPROVED（工时归集到项目成本）/ REJECTED 全链，**代码仅实现 submit**。触发条件 = 「projects 工时归集 successor」（hr 工时单 APPROVED→归集 projects 成本），由于 hr 侧 approve 缺失，hr→projects 工时归集链**在触发点断裂**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——hr approve/reject 未实现致归集触发点不可达；修复归 MR1 而非本审计）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-043` 行含 successor 声明「工时归集 projects/cost-collection」）。
- **④ README 覆盖复核**：README `:62`「projects 工时/结算过账生命周期 E2E」覆盖的是 **projects 侧 ErpPrjTimesheet**（DIRECT 状态机 UNSUBMITTED→submit→SUBMITTED→approve→APPROVED+posted，PROJECT_COST_COLLECTION 凭证）——**与本 successor（hr 侧 ErpHrTimesheet approve/reject + hr→projects 归集）是不同实体不同控制点**（projects 侧 ErpPrjTimesheet 有独立 approve 工作经 E2E 证实；hr 侧 ErpHrTimesheet approve 缺失）。README 行非本 design successor。S1 覆盖充分。
- **结构性约束标注（§对账差异 #5 + 同型过账悬挂根因）**：`P1-MA2-043` **finding 已 resolved-via-deferral**（R1.15 方案 B Deferred，A1.14 RC 从 L1 视角重开经 MR1[属 A1.x 裁决]）。**同型过账悬挂根因关系**：projects 侧 `TimesheetPostingDispatcher.tryPost:60-72`（处理 ErpPrjTimesheet）显式「失败吞异常返回 false（保持 posted=false）」——与 P1-MA2-068 族（finance/hr/mfg/assets/qa 同型 tryPost 吞异常悬挂）同根因。但本 successor（#3）裁决的是 **hr 侧 approve 触发点缺失**（hr→projects 归集链断裂在触发点），**独立于** projects 侧 tryPost 悬挂根因（后者是 projects 侧 ErpPrjTimesheet 的过账失败悬挂，已有独立 finding 族覆盖）。两者各自裁决不冲突：hr approve 缺失（本 successor）≠ projects tryPost 悬挂（P1-MA2-068 族）。

### 2.4 #4 业务单据作废联动取消质检单 — quality

- **① 触发条件状态**：**未满足**。实仓 grep 跨 `module-quality` + `module-purchase` + `module-sales` + `module-manufacturing` `cancelForBusinessBill|cancelOnBillVoid|onBusinessBillCancelled|cancelInspection` **全零命中**。`IErpQaInspectionBiz`（含 recordResult/findByRelatedBill/createForBusinessBill/isInspectionCleared/passInspection/failInspection/batchPassInspection）**无 cancelForBusinessBill Facade**。业务域 cancel Processor 无 `IErpQaInspectionBiz.cancel*` 调用。触发条件 = 「业务作废自动取消质检需求时」（owner doc `quality/state-machine.md §实现约定:190` Successor 触发条件原文），该需求**未触发**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——业务作废自动取消质检需求未驱动；修复属代码逻辑类[新增 Facade + Processor config-gated wiring]，预授权可自动执行，但触发条件未满足不强制实现）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA2-064` 行 + owner doc `state-machine.md §4:50/§实现约定:190` 显式 Deferred + Successor 触发条件标注）。
- **④ README 覆盖复核**：无独立 design successor 行。S1+S2 覆盖充分。
- **结构性约束标注（§对账差异 #5）**：`P1-MA2-064` **finding 已 resolved-via-deferral**（R1.20，A1.31 RC 从 L1 视角登记 P1-RC-041 + arm-index 状态澄清 resolved-via-deferral[属 A1.x 裁决]）。本 A3.3 裁决的是 **successor 触发条件**（未满足→维持 backlog），不重审 finding 关闭裁决。

### 2.5 #5 employee-id 行过滤（quality inspectorId / maintenance assignedTo）— quality(+mnt 跨域)

- **① 触发条件状态**：**未满足**。实仓 grep `module-master-data/model/app-erp-master-data.orm.xml` `ErpMdEmployee` 实体（propId 1-15：id/code/name/orgId/position/phone/email/partnerId/status/delVersion/version/createdBy/createTime/updatedBy/updateTime）——**无 userId 列**（15 列实测，与 R3.4 闭合记录「ErpMdEmployee 无 userId 列（实测 15 列）」一致）。quality `inspectorId`（`ErpQaInspection:253`/`ErpQaSpcSample:184` BIGINT employee-id）+ maintenance `assignedTo`（`ErpMntErrors.ARG_ASSIGNED_TO` + `ErpQaNonConformance:291` BIGINT employee-id）均为 employee-id 而非 userId，**无 employee-id→userId 解析器**。触发条件 = 「ErpMdEmployee 增 userId 列 + 解析器」，该 ORM 变更**未落地**。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——ErpMdEmployee userId 列未落地；修复触及 ORM 结构变更[ErpMdEmployee 加 userId 列]，**ask-first 保护区域**，修复归 MR1 须 ask-first + 独立 plan-audit[§5]，非本审计实施）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA6-002` 行 R3.4 闭合记录显式 successor：employee-id 域规则[inspectorId/assignedTo，触发=ErpMdEmployee 增 userId 列+解析器]）。
- **④ README 覆盖复核**：README `:124`「权限 enforcement 开启（测试环境）」E2 data 级强制 role-row-filter→employee-id 规则为 **E2E 测试 enforcement successor**（触发=RBAC 精细化/合规审计需求），与本 design successor（ORM userId 列落地）不同类别（前者是 enforcement 翻转测试，后者是 ORM 结构变更前置）。S1 覆盖充分。
- **结构性约束标注（§对账差异 #5 + ORM ask-first 保护区域 + 跨域投影去重）**：
  - `P1-MA6-002` **finding 已 MR3 done R3.4 实现修复 userId 域子集**（sales 业务员×6 单据 createdBy + quality RiskRegister ownerId，userId 域直接可用），**employee-id 域子集**（quality inspectorId / maintenance assignedTo）为显式 successor（须 ErpMdEmployee userId 列 ORM 变更，R3.4 闭合时框定为 Non-Goal 保护区域）。本 A3.3 裁决的是 employee-id 子集 **successor 触发条件**（未满足→维持 backlog）。
  - **ORM ask-first 保护区域**：本 A3.3 **仅裁决 successor 触发条件**是否回队（未满足→维持 backlog），**不实施** ORM 变更。ErpMdEmployee userId 列的修复实施**须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议），属 MR1 修复期门控，非本裁决期。
  - **跨 quality+maintenance 两域投影去重**：#5 按 §去重协议**合并裁决一次**——quality inspectorId + maintenance assignedTo 是同一控制点（ErpMdEmployee userId 列 + employee-id→userId 解析器）的两域投影。A3.5（扩展域含 maintenance）复查时交叉引用本结论，不重复裁决。

---

## 3. 既有行为证据（段 3，复用既有 arm 审计，§去重协议）

> 本复查为 successor 触发条件复查（需求契约视角），不重做 doc↔code 文本一致性 / 状态机行为 / 代码质量。实现证据复用既有 arm MA2/MA4 报告 + A1.x RC 复查报告已证实的代码路径，仅列锚点供四任务核证溯源。

| # | successor 项 | 代码锚点（复用 arm MA2/MA4 + A1.x RC 已证实） | 既有证实报告 |
|---|-------------|----------------------------------------------|-------------|
| 1 | 订单维度核销 | `ReceiptSettler.java:106-219`（全 invoiceId 维度，无 orderId）+ `SettlementAllocation.java:11`（master-data invoiceId 承载）+ `flow-overview.md:157` owner doc 双维度声明 | `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（P2-MA2-013 登记）；A1.18/A1.19 RC（sales 域已证实核销主路径 invoice 维度） |
| 2 | 资产 IDLE | `ErpAstConstants.java:67` + `_ErpAstDaoConstants.java:19`（常量定义）+ 零 setStatus writer + `ErpAstDepreciationScheduleProcessor:70-71` ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE（仅 IN_SERVICE）+ 3 处只读引用（`ErpAstValueAdjustmentProcessor:196`/`ErpAstDisposalProcessor:194`/`ErpAstInventoryProcessor:113`）+ `ErpAstDashboardBizModel:179` IN_SERVICE 过滤 | `2026-07-28-0400-arm-ma2-assets-state-machine.md`（P1-MA2-061 IDLE 死状态已证实）；A1.24 RC §3 |
| 3 | 工时单 approve/reject + 工时归集 | `ErpHrTimesheetBizModel.java:36-37`（仅 submit）+ 零 approve/reject + projects 侧 `TimesheetPostingDispatcher.tryPost:60-72`（tryPost 吞异常悬挂同型，处理 ErpPrjTimesheet 非 ErpHrTimesheet） | `2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（P1-MA2-043 死状态已证实）；A1.14 RC §3；projects 侧 E2E `2026-07-14-0742-2`（ErpPrjTimesheet approve 工作已证实） |
| 4 | 质检联动 | `IErpQaInspectionBiz`（85 行无 cancelForBusinessBill）+ grep 跨 quality/purchase/sales/mfg cancelForBusinessBill 零命中 + 业务域 cancel Processor 无 IErpQaInspectionBiz.cancel* 调用 | `2026-07-28-1020-arm-ma2-quality-state-machine.md`（P1-MA2-064 作废联动未落地已证实）；A1.31 RC §3（P1-RC-041 状态澄清 resolved-via-deferral） |
| 5 | employee-id 行过滤 | `app-erp-master-data.orm.xml:859-895` ErpMdEmployee 15 列无 userId + quality `ErpQaInspection:253`/`ErpQaSpcSample:184` inspectorId BIGINT employee-id + maintenance `ErpMntErrors.ARG_ASSIGNED_TO` + `ErpQaNonConformance:291` assignedTo BIGINT employee-id + `ErpRoleDataAuthChecker`（R3.4 userId 域子集落地，employee-id 子集 successor） | `2026-07-29-1410-arm-ma6-data-permission-runtime.md`（P1-MA6-002 角色侧行级过滤已证实）；R3.4 闭合记录（employee-id successor 显式登记） |

---

## 4. 运行时行为证据（段 4，复用既有 arm MA2/MA6，§去重协议）

> 本 mission MA3 = successor 触发条件复查（需求契约视角），与 audit-remediation MA2（状态机/链路行为视角）/ MA4（代码质量视角）/ MA6（数据权限运行时视角）维度不重叠（methodology §去重协议）。既有 arm 报告 + A1.x RC 报告已证实的运行时行为直接引用：

- **#1 订单维度核销**：收款核销按 invoiceId 维度成立（`ReceiptSettler` invoice 维度核销 + 辅助账 openAmount 回写一致），订单维度核销功能未实现（无运行时路径）——经 O2C E2E 报告 + A1.18/A1.19 RC 证实。
- **#2 资产 IDLE**：IN_SERVICE 折旧主路径完整（资本化建卡→IN_SERVICE→期末批量折旧→处置终态），IDLE 为预留死状态（零 writer + 折旧引擎仅查 IN_SERVICE 等价于「IDLE 默认停提」owner doc §1 设计意图），无运行时数据破坏——经 `2026-07-28-0400-arm-ma2-assets-state-machine.md` + A1.24 RC §4 证实。
- **#3 工时单/工时归集**：hr 工时单 DRAFT→SUBMITTED 完整（submit），APPROVED/REJECTED 死状态不破坏主路径（SUBMITTED 为审批等待点，无悬挂半状态）；projects 侧 ErpPrjTimesheet approve→posted 工作经 E2E 证实（独立实体）；hr→projects 归集链在 hr approve 触发点断裂——经 `2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md` + A1.14 RC §4 + projects E2E `2026-07-14-0742-2` 证实。
- **#4 质检联动**：CANCELLED 业务单据不再流转，质检单悬挂不破坏主路径（强制质检门控经 isInspectionCleared 检查，CANCELLED 业务单据不触发 enforceGate 二次流转）+ 残留质检单经 useLogicalDelete 手工清理——经 `2026-07-28-1020-arm-ma2-quality-state-machine.md` + A1.31 RC §4 证实。
- **#5 employee-id 行过滤**：R3.4 userId 域子集落地（sales 业务员 createdBy + quality RiskRegister ownerId，config-gated 默认 OFF 灰度），employee-id 域子集（inspectorId/assignedTo）未落地（ErpMdEmployee 无 userId 列致 user→employee 解析不可行）——经 `2026-07-29-1410-arm-ma6-data-permission-runtime.md` + R3.4 闭合记录证实。

---

## 5. 复查结论（段 5，§6 MA3 适配：触发条件状态 + 回队决策）

> 复查结论三分：`回队 MR1`（触发条件已满足 / Q4 强制）/ `维持 backlog successor`（触发条件未满足）/ `补登记`（owner doc 内嵌但 arm-index 无行）。

### 5.1 逐项复查结论

| # | successor 项 | 触发条件状态 | 证据 | 回队决策 | 与 A2.x 关闭裁决交叉 |
|---|-------------|-------------|------|---------|---------------------|
| 1 | 订单维度核销 | ❌ 未满足（订单维度核销业务需求未驱动） | `ReceiptSettler.java:106-219` 全 invoiceId 维度无 orderId | **维持 backlog successor** | #1 ↔ `P2-MA2-013`（A2.5 空集认证：watch-only 未关闭）一致 |
| 2 | 资产 IDLE 状态机迁移 + 折旧扩展 | ❌ 未满足（资产暂停/恢复业务未上线） | 零 setStatus(IDLE) writer + `ErpAstDepreciationScheduleProcessor:70-71` 仅 IN_SERVICE | **维持 backlog successor** | #2 ↔ `P1-MA2-061`（A2.6 空集认证：resolved-via-deferral R1.18）一致；successor 维持 backlog ≠ finding 重开（finding 经 A1.24 RC §4 三判据复核倾向重开归 A1.x→MR1 通道，两者各自裁决交叉不冲突） |
| 3 | 工时单 approve/reject + 工时归集 | ❌ 未满足（hr approve 缺失致归集触发点断裂） | `ErpHrTimesheetBizModel.java:36-37` 仅 submit 无 approve/reject | **维持 backlog successor** | #3 ↔ `P1-MA2-043`（A2.4 空集认证：resolved-via-deferral R1.15）一致；successor 维持 backlog ≠ finding 重开（finding 经 A1.14 RC 从 L1 视角重开归 A1.x→MR1 通道）；**同型过账悬挂根因**：projects 侧 `TimesheetPostingDispatcher.tryPost` 吞异常悬挂（P1-MA2-068 族）独立于本 successor（hr 触发点缺失） |
| 4 | 业务单据作废联动取消质检单 | ❌ 未满足（业务作废自动取消质检需求未驱动） | grep 跨 quality/purchase/sales/mfg cancelForBusinessBill 零命中 | **维持 backlog successor** | #4 ↔ `P1-MA2-064`（A2.7 空集认证：resolved-via-deferral R1.20）一致；successor 维持 backlog ≠ finding 重开（finding 经 A1.31 RC 登记P1-RC-041 归 A1.x→MR1 通道） |
| 5 | employee-id 行过滤（inspectorId/assignedTo） | ❌ 未满足（ErpMdEmployee 无 userId 列） | `app-erp-master-data.orm.xml:859-895` ErpMdEmployee 15 列无 userId + inspectorId/assignedTo BIGINT employee-id 无解析器 | **维持 backlog successor** | #5 ↔ `P1-MA6-002`（A6.3：MR3 done R3.4 userId 域子集实现修复，employee-id 子集为 successor）一致；**ORM ask-first 保护区域**：修复实施须 ask-first + 独立 plan-audit（§5），本裁决仅判 successor 触发条件；**跨 quality+maintenance 两域投影去重**：合并裁决一次，A3.5 交叉引用 |

### 5.2 统计

- **回队 MR1**：0 项（5 项触发条件全部未满足，无 Q4 强制回队）
- **维持 backlog successor**：5 项（#1-#5 全部维持 backlog）
- **补登记**：0 项（5 项均有 S1 arm-index 覆盖，#1/#2/#4 另有 S2 owner doc 双源覆盖，无 owner doc 内嵌但 arm-index 无行的遗漏项）
- **本审计新发现 P0**：0 项（无 MR0 即时通道触发）

### 5.3 结构性约束（§对账差异 #5 + 同型根因 + 保护区域 + 跨域去重）

- **§对账差异 #5（实现修复项 successor 残留）**：#2/#3/#4/#5 四项的 finding 均已有 resolution（#2/#3/#4 resolved-via-deferral，#5 MR3 done 部分实现），但其 successor 触发条件均未满足。本 A3.3 严格区分「finding 已修复/关闭」与「successor 仍待触发」——不误将已修复 finding 重新纳入 MR1（finding 重开归 A1.x/A2.x 通道，successor 维持归 A3.x，两者各自裁决）。
- **#3 同型过账悬挂根因**：projects 侧 `TimesheetPostingDispatcher.tryPost` 吞异常悬挂（P1-MA2-068 族）与本 successor（hr 工时单 approve 缺失致 hr→projects 归集触发点断裂）**独立**——前者是 projects 侧 ErpPrjTimesheet 过账失败悬挂（已有独立 finding 族），后者是 hr 侧 approve 触发点缺失。两者各自裁决不冲突。
- **#5 ORM ask-first 保护区域**：ErpMdEmployee userId 列修复实施须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本 A3.3 仅裁决 successor 触发条件（未满足→维持 backlog），ORM 变更属 MR1 修复期门控，非本裁决期。
- **#5 跨 quality+maintenance 两域投影去重**：按 §去重协议合并裁决一次（同一控制点 ErpMdEmployee userId 列 + 解析器的两域投影）。A3.5（扩展域含 maintenance）复查时交叉引用本 §2.5/#5 结论，不重复裁决。

---

## 6. 与 arm-index 衔接（段 6，§7「复用 or 新增」裁决）

> §7 规则：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC MA3 注记；仅当发现 owner doc 内嵌但 arm-index 无独立行的 successor（如 A3.1 #7/#8）才补登记。本 A3.3 5 项**全部有 S1 arm-index 覆盖**（5 项均有既有 arm finding 行），故**全部复用**，无补登记。

### 6.1 逐项「复用 or 补登记」裁决

| # | successor 项 | arm-index grep 结果 | 裁决 | 操作 |
|---|-------------|---------------------|------|------|
| 1 | 订单维度核销 | 既有 `P2-MA2-013` 行（arm-index :699）含 watch-only successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.3）：触发条件未满足[订单维度核销业务需求未驱动]→维持 backlog successor」注记 |
| 2 | 资产 IDLE 状态机迁移 + 折旧扩展 | 既有 `P1-MA2-061` 行（arm-index :503）含 successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.3）：触发条件未满足[资产暂停/恢复业务未上线]→维持 backlog successor；successor 维持 ≠ finding 重开[finding 重开归 A1.24 RC→MR1]」注记 |
| 3 | 工时单 approve/reject + 工时归集 | 既有 `P1-MA2-043` 行（arm-index :489）含 successor 声明（工时归集 projects/cost-collection） | **复用** | 既有行追加「RC MA3 复查（A3.3）：触发条件未满足[hr approve 缺失致 hr→projects 归集触发点断裂]→维持 backlog successor；successor 维持 ≠ finding 重开[finding 重开归 A1.14 RC→MR1]；同型过账悬挂[projects 侧 tryPost]独立于本 successor」注记 |
| 4 | 业务单据作废联动取消质检单 | 既有 `P1-MA2-064` 行（arm-index :506）含 successor 声明（state-machine.md §实现约定:190） | **复用** | 既有行追加「RC MA3 复查（A3.3）：触发条件未满足[业务作废自动取消质检需求未驱动]→维持 backlog successor；successor 维持 ≠ finding 重开[finding 归 A1.31 RC P1-RC-041→MR1]」注记 |
| 5 | employee-id 行过滤（inspectorId/assignedTo） | 既有 `P1-MA6-002` 行（arm-index :598）含 R3.4 闭合 successor 声明（employee-id 域子集） | **复用** | 既有行追加「RC MA3 复查（A3.3）：触发条件未满足[ErpMdEmployee 无 userId 列]→维持 backlog successor；ORM ask-first 保护区域[修复实施须 ask-first + 独立 plan-audit]；跨 quality+maintenance 两域投影合并裁决[A3.5 交叉引用]」注记 |

**裁决依据**：5 项均为既有 arm finding 的同一根因/同一控制点 successor，复用既有 ID 追加 RC MA3 注记。**不新建 `P*-RC-xxx`**（禁止未经比对直接新建）——5 项全部有既有 arm-index 行覆盖，无 owner doc 内嵌但 arm-index 无独立行的遗漏项（与 A3.1 #7/#8 补登记情形不同）。

### 6.2 双向可追溯

- **回队项 ↔ MR1 R1.0 预留展开行**：**0 项**（5 项触发条件全部未满足，无回队 MR1）。
- **维持 backlog 项 ↔ A3.x successor 登记**：#1-#5 全部维持 backlog，交叉引用本 A3.3 报告 + arm-index successor 注记。
- **finding 重开项（非本 A3.3 裁决，交叉引用）**：#2（`P1-MA2-061` 经 A1.24 RC §4 三判据复核倾向重开→MR1）/ #3（`P1-MA2-043` 经 A1.14 RC 从 L1 视角重开→MR1）/ #4（`P1-MA2-064` 经 A1.31 RC 登记 P1-RC-041→MR1）——这些 finding 重开属 **A1.x→MR1 通道**，与本 A3.3 successor 裁决各自独立（§MA2↔MA3 协作：关闭裁决/finding 修复归 A1.x/A2.x，successor 触发条件归 A3.x，交叉引用不重复）。
- **arm-index 回填**：§6.1 注记已写入 `arm-index.md`（5 既有行追加 RC MA3 注记）。

---

## 7. 静态存疑点清单（段 7，供 MA4 A4.2 展开）

> L5 无法静态定论、需运行时确认的点。本复查为 successor 触发条件复查（读 arm-index/owner doc/实仓代码/config/ORM），以下为复查中静态无法定论、建议 MA4/A4.2 运行时确认的点：

1. **#3 hr→projects 工时归集链运行时可达性**（projects 侧 ErpPrjTimesheet 与 hr 侧 ErpHrTimesheet 是否存在隐性数据同步路径）：本复查静态确认 hr `ErpHrTimesheetBizModel` 仅 submit 无 approve，projects 侧 `ErpPrjTimesheet` 有独立 approve（经 E2E `2026-07-14-0742-2` 证实）。但两实体间是否存在隐性同步/映射路径（如 hr 工时单 submit 后是否触发 projects 侧 ErpPrjTimesheet 创建），需运行时跨域调用链确认——建议 A4.2 展开运行时探针。**低优先级**（静态证据已表明两实体独立，触发点断裂成立，本存疑点仅为排除隐性路径）。

> 其余 4 项（#1/#2/#4/#5）的运行时行为已由既有 arm MA2/MA6 报告 + A1.x RC §4 充分证实（§4），无新增静态存疑点。特别地：
> - #2 IDLE 死状态经 `2026-07-28-0400-arm-ma2-assets-state-machine.md` 运行时复核确认无数据破坏（零 writer + 折旧引擎仅 IN_SERVICE）。
> - #5 ErpMdEmployee userId 列缺失经 R3.4 闭合记录 + ORM 实测（15 列）静态确认，无运行时存疑。

---

## 8. 过程纪律自检（段 8，§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（actual 见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码不反映 actual vs baseline），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以 checker 脚本退出码作为门控通过依据**。**本审计无生产代码变更（纯审计报告 + arm-index 文档注记），checker 无回归风险**——actual 计数与本审计行为正交（未触及任何生产代码），任何 actual vs baseline 差异均非本审计引入。

  | 规则 | 基线（compliance-baseline.md §BASELINE machine-readable :296-316） | actual（本次实测） | 漂移 | 归因 |
  |------|-------------------------------|-------------------|------|------|
  | R1a | 0 | 0 | 0 | — |
  | R1b | 0 | 0 | 0 | — |
  | R1c | 0 | 0 | 0 | — |
  | R1d | 14 | 14 | 0 | — |
  | R2a | 34 | 34 | 0 | — |
  | R2b | 229 | 229 | 0 | — |
  | R2c | 1382 | 1382（生产代码总计） | 0 | — |
  | R2d | 34 | 34 | 0 | — |

  > 本审计仅产出本报告 + `arm-index.md` 注记（纯文档），未触及 `module-*/` 任何生产代码。actual 全规则 = baseline，零漂移，无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见来源 plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部 5 项 successor 已按 §7 规则 grep arm-index 同域同控制点后给出「复用」裁决（§6.1），无未经比对直接新建的 `P*-RC-xxx` finding（5 项全部复用既有 arm finding ID 追加 RC MA3 注记）。

---

## 9. 与既有审计差异增量声明（段 9，§去重协议）

本报告与既有 arm 审计（`docs/audits/2026-07-2*-arm-ma2-*` / `arm-ma6-*`）+ A1.x RC 复查报告的差异增量：

- **复用既有证据**（不重复验证）：
  - `2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`（#1 P2-MA2-013 订单维度核销缺失已登记）；
  - `2026-07-28-0400-arm-ma2-assets-state-machine.md`（#2 P1-MA2-061 IDLE 死状态已证实）；
  - `2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（#3 P1-MA2-043 工时单 approve/reject 死状态已证实）；
  - `2026-07-28-1020-arm-ma2-quality-state-machine.md`（#4 P1-MA2-064 业务作废联动未落地已证实）；
  - `2026-07-29-1410-arm-ma6-data-permission-runtime.md`（#5 P1-MA6-002 角色侧行级过滤 + R3.4 闭合记录 employee-id successor 已证实）；
  - A1.x RC 报告（A1.18/A1.19 sales 核销主路径 / A1.24 assets IDLE §4 三判据复核 / A1.14 hr 工时单 L1 重开 / A1.31 quality 作废联动 P1-RC-041 / A1.32 NCR-CAPA 闭包）已证实的代码路径 + §4 三判据复核。

- **本复查只补的差异增量**：**successor 触发条件是否已满足 + 是否该回队**——从 methodology §MA3 四任务（① 触发条件状态 grep 实仓验证 / ② 回队决策 / ③ 补登记 / ④ README 覆盖复核）出发，逐项核证 5 项 sales+assets+projects+quality successor 的触发条件现状。这是既有 arm 审计（状态机行为 / 数据权限运行时维度）+ A1.x RC（L1 验收标准视角 + §4 三判据 finding 复核维度）未覆盖的「successor 触发条件完整性 + 回队决策」维度（methodology §去重协议 §MA2↔MA3 协作——关闭裁决/finding 修复归 A1.x/A2.x，successor 触发条件归 A3.x，交叉引用不重复）。

- **不重复**：不重做 doc↔code 文本一致性（audit-remediation MA3 已收口）、不重做状态机/链路行为（arm MA2 已收口）、不重做代码质量（arm MA4 已收口）、不重做数据权限运行时（arm MA6 已收口）、不重审方案 B 关闭裁决本身（A2.x RC 空集认证已收口）、不重审 finding §4 三判据复核（A1.x RC 已收口，本 A3.3 只复查 successor 触发条件，交叉引用）。

---

## 结论

sales+assets+projects+quality MA3 successor 复查（A3.3）完成：5 项 design-level successor 逐项经 §MA3 四任务核证。

- **回队 MR1**：0 项（5 项触发条件全部未满足，无 Q4 强制回队）。
- **维持 backlog successor**：5 项（#1-#5 全部维持 backlog，待各自触发条件满足）：
  - #1 订单维度核销（触发=报表/核销需求驱动订单+发票双维度）；
  - #2 资产 IDLE 状态机迁移 + 折旧扩展（触发=资产暂停/恢复业务上线）；
  - #3 工时单 approve/reject + 工时归集（触发=hr approve 落地后 hr→projects 归集链可达）；
  - #4 业务单据作废联动取消质检单（触发=业务作废自动取消质检需求）；
  - #5 employee-id 行过滤 inspectorId/assignedTo（触发=ErpMdEmployee 增 userId 列 + 解析器，ORM ask-first）。
- **补登记**：0 项（5 项均有 S1 arm-index 覆盖，无 owner doc 内嵌但 arm-index 无行的遗漏项）。
- **结构性约束**：§对账差异 #5（#2/#3/#4/#5 finding 已修复/关闭 vs successor 仍待触发 区分）；#3 同型过账悬挂根因（projects 侧 tryPost 独立于 hr 触发点缺失）；#5 ORM ask-first 保护区域（修复实施须 ask-first + 独立 plan-audit）；#5 跨 quality+maintenance 两域投影合并裁决（A3.5 交叉引用）。
- **arm-index 衔接**：5 项全部复用既有 ID 追加 RC MA3 注记（无新 `P*-RC-xxx`，无补登记）。
- **本审计无生产代码变更**（纯报告 + arm-index 文档注记），§9 真相源冻结条款遵守（未修改 product-scope / owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实 / backlog README）。
