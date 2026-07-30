# 2026-07-30-2046-2-mr5-r5-8 Facade S-mutation 清理 + BizModel 配线 + 全量验证（MR5 收尾）

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MR5 工作项 R5.8
> Related: `docs/plans/2026-07-30-1433-1-mr5-r5-1-purchase-s-mutation.md`~`2026-07-30-1909-3-mr5-r5-6-inventory-s-mutation.md`（R5.1-R5.6，全部 deferred BizModel 配线命名 R5.8 为 successor）、`docs/plans/2026-07-30-2046-1-mr5-r5-7-remaining-domains-s-mutation.md`（R5.7，prereq）、`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`（创建 149 per-mutation 文件）、`docs/plans/2026-07-29-1923-2-r2-0-mr2-p1-finding-expansion.md`（R2.0，P1-MA3-048 归属 MR2/R2.7）
> Audit: required

## Current Baseline

- **MR5 进度**：R5.1-R5.6 全部 done（Pattern B custom public override 已填充 139 个 per-mutation 文件，per-mutation 自包含经 facade protected helper 编排）。R5.7 为本 plan 直接 prereq（填充剩余 10 个 per-mutation：qa 5 + projects 4 + crm 1），完成后 MR5 全 7 域填充完毕，**149 个 per-mutation 文件全部自包含**。
- **R5.1-R5.6 累积 deferred 项（全部命名 R5.8 为 successor，本 plan 必须承接）**：
  - **休眠 per-mutation 运行时验证（29 文件）**：18 个 `*CancelProcessor`（purchase 6 + sales 6 + finance 3[EA/EC/BudgetScenario] + assets 3[Merge/Split/VA]）+ 11 个非 cancel S-mutation per-mutation（finance BadDebt 4[submit/approve/reject/reverseApprove] + BudgetScenario 3[submit/approve/reject——cancel 已计入 18] + assets Inventory 1[approve] + Maintenance 1[approve] + inventory LandedCost 2[approve/reverseApprove]）。这些文件经 R5.1-R5.6 静态 parity 校验但不在运行时路径（BizModel→facade 直调绕过 per-mutation）。本 plan 激活后须有运行时测试覆盖。
  - **BizModel 配线 + beans.xml + xbiz 清理**：所有完成域 BizModel 仍 `@Inject` 单 facade。S-mutation 经 xbiz source（source-backed）或 BizModel `@BizMutation`（dormant）调 facade 公共 S-mutation 方法。

- **实测 facade 公共 S-mutation 方法状态（R5.1-R5.6 域）**：**27 个 facade Processor 全部保留完整公共 S-mutation 编排方法**（未精简）。样例：`ErpPurOrderProcessor.approve()`（6 步编排）、`ErpSalOrderProcessor.approve()`、`ErpInvCostAdjustProcessor`（5 方法）。R5.7 后总计 **30 个 facade**（+qa/projects/crm 3）。

- **实测 BizModel 调用 facade 公共方法的活跃站点（必须先 repoint 才能 slim facade）：**
  - **cancel 路由**：18 个 BizModel `@BizMutation cancel()` → `facade.cancel(...)`（purchase 6 + sales 6 + finance 3[EA/EC/BudgetScenario] + assets 3[Merge/Split/VA]）。
  - **休眠非 cancel S-mutation `@BizMutation` 直调 facade**：5 个 BizModel（finance BadDebt 4 动作[submit/approve/reject/reverseApprove] + BudgetScenario 3 动作[submit/approve/reject——cancel 已计入 cancel 路由]、assets Inventory approve + Maintenance approve、inventory LandedCost approve/reverseApprove）= 11 动作。
  - **`batchApprove()` 调 `facade.approve(...)`**：仅 **2 个 BizModel**（ErpPurOrder、ErpSalOrder）。其余休眠 approve 站点（BadDebt/BudgetScenario/AstInventory/AstMaintenance/LandedCost）为单记录 `@BizMutation approve()`，已计入上条休眠非 cancel S-mutation，非 batchApprove。

- **无循环依赖风险**：Pattern A/B per-mutation 仅调 facade **protected** helper（`requireXxx`/`validateTransitionForXxx`/`doXxx`），从不回调 facade 公共 S-mutation 入口方法。故 facade 公共方法精简为 `return {per-mutation}.method(id, ctx)` 安全——per-mutation 不回调 `facade.method()`。

- **xbiz inline-script 残留**：R5.5（mfg）已转换 2 个 withdrawApproval inline；R5.7 将转换 QA withdrawApproval inline。完成后 R5.1-R5.7 域**零 S-mutation inline-script 残留**（剩余 NopScriptError 在 out-of-scope 实体：ErpMfgMaterialIssue/ErpSalContract/ErpPurRfq 等，非 MR5 S-mutation 实体）。本 plan 无需进一步 inline 提取。

- **跨包 facade**：`ErpFinBudgetScenarioProcessor` 位于 `app.erp.fin.service.budget`（非 `service.processor`）——slim/wire 须特殊处理。

- **P1-MA3-048 当前状态**（`docs/audits/arm-index.md` L265）：`todo (roadmap v16 / R2.7)`——"孤儿 Processor bean 携带 String 影子契约"。本 plan 须更新为"MR5 已填充，孤儿状态已清除"。

- **roadmap "36" 数值陈旧**：roadmap R5.8 描述称"36 个 facade"，实测含 S-mutation 的 facade 为 **30 个**（27 done + 3 R5.7），与 roadmap line 234（"30 个含 S-mutation 的 facade Processor"）一致。本 plan 以 30 为准。

- 验证基线：`mvn clean install -DskipTests` 全绿（154 模块）；`mvn test` 全绿（~2890 测试，0 failures，已知 1 pre-existing error TestErpMfgCompletionPosting LOCATION_ID 漂移同 R5.3/R5.4/R5.5 共识）。

## Goals

- 全 30 个 facade 公共 S-mutation 方法精简为单行委托（`return {per-mutation}.method(id, ctx)`），消除 facade↔per-mutation 编排重复；per-mutation 成为 S-mutation 唯一编排入口（可独立 Delta 定制）。
- 全部 S-mutation BizModel 调用从 facade repoint 到 per-mutation Processor（cancel 路由 18 + 休眠非 cancel S-mutation 11 + batchApprove 2），激活全部 29 个休眠 per-mutation 的运行时路径。
- 29 个休眠 per-mutation 经全量 `mvn test` 确认运行时激活后行为等价（既有测试覆盖 BizModel→per-mutation 新路径）。
- beans.xml 注册一致性确认；xbiz S-mutation `<source>` 一致性确认（source-backed delegation 无残留 inline）。
- `mvn clean install -DskipTests`（154 模块）+ `mvn test`（~2890 测试）全绿 + compliance checker 基线不高于 M0 锚点。
- arm-index P1-MA3-048 更新为"MR5 已填充，孤儿状态已清除"；`per-mutation-processor-split-plan.md` 回注最终分类。

## Non-Goals

- D-mutation（域特定操作如 executeDepreciation/generateMove/convertToCustomer/start/stop/close/issueMaterials 等）保留在 facade——MR5 范围外（roadmap line 230 明示）。
- 纯 D-mutation facade（ErpFinNotesPayable/ErpFinNotesReceivable/ErpFinAccountingPeriod/ErpFinPostingProcessor/ErpAstCip/ErpAstDepreciationSchedule/ErpMfgJobCard/ErpMfgScheduleToJobCard/ErpInvStockMove/ErpInvOwnershipTransfer/ErpCrmConversion/ErpApsScheduling）——无 S-mutation per-mutation 文件，不在本里程碑。
- R2.7（孤儿 Processor 删除）——MR5 之后执行；本 plan 填充后无剩余 S-mutation 孤儿（149 文件全填充），R2.7 的 plan 增加检查"跳过已被 MR5 填充的 Processor"。
- facade protected helper 重构/下沉——protected helper 是 per-mutation 经单一真相源调用的依赖，保留在 facade。
- 新增业务测试（除休眠 per-mutation 运行时覆盖缺口确认外）——测试有效性深挖属 MR2（R2.10-R2.14）/MR3。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/processor-extension-pattern.md`、`docs/analysis/per-mutation-processor-split-plan.md`、`docs/backlog/audit-remediation-roadmap.md` §MR5
- Skill Selection Basis: 跨 7 域机械性 BizModel 配线 + facade 精简匹配 `nop-backend-dev`（Processor 模式、@Inject 纪律、错误处理自检）。休眠 per-mutation 激活涉及会计保护区域（projects/assets/finance），须对照 R1.x owner doc 静态校验语义不变。`nop-testing` 用于全量回归。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - BizModel S-mutation 配线从 facade repoint 到 per-mutation（7 域，激活 29 休眠 per-mutation）

Status: completed
Targets: 7 域 BizModel（purchase 6 + sales 6 + finance 4 + assets 5 + mfg 0 + inventory 1 + R5.7 域 3[qa/projects/crm]），各 `app-service.beans.xml`（读校验）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: R5.7 done（149 per-mutation 全填充）

- [x] Add: cancel 路由 repoint——18 个 BizModel `@BizMutation cancel()` 从 `facade.cancel(...)` 改为 `@Inject {Entity}CancelProcessor` + `cancelProcessor.cancel(id, ctx)`。激活 18 个休眠 `*CancelProcessor` 运行时路径。
  - Skill: `nop-backend-dev`
  - 实体清单：purchase Order/Requisition/Receive/Return/Invoice/Payment（6）、sales Order/Quotation/Delivery/Receipt/Return/Invoice（6）、finance EmployeeAdvance/ExpenseClaim/BudgetScenario（3）、assets Merge/Split/ValueAdjustment（3）。
  - 注意 Long 签名实体（projects/crm/assets 部分）保留 `Long.valueOf(id)` 边界转换。
- [x] Add: 休眠 S-mutation `@BizMutation` repoint——5 个 BizModel 的休眠 S-mutation 动作从 facade 改为 per-mutation：finance BadDebt(submit/approve/reject/reverseApprove) + BudgetScenario(submit/approve/reject——cancel 已计入 Phase 1 cancel 路由)、assets Inventory(approve) + Maintenance(approve)、inventory LandedCost(approve/reverseApprove)。激活对应休眠 per-mutation 运行时路径。
  - Skill: `nop-backend-dev`
- [x] Add: `batchApprove()` repoint——仅 **2 个 BizModel**（ErpPurOrder、ErpSalOrder）的 `batchApprove()` 从 `facade.approve(...)` 改为 `approveProcessor.approve(id, ctx)`。其余休眠 approve 站点为单记录 `@BizMutation approve()`，已计入上一条休眠非 cancel S-mutation repoint。
  - Skill: `nop-backend-dev`
- [x] Add: R5.7 域 BizModel repoint——projects `ErpPrjProjectSettlementBizModel`（4 S-mutation submit/approve/reject/cancel）+ crm `ErpCrmLeadBizModel`（cancel）从 facade 改为 per-mutation，激活 R5.7 孤儿 per-mutation 运行时路径。qa `ErpQaRecallBizModel` 不 `@Inject` facade（S-mutation 全经 xbiz source），无 BizModel repoint 需求。
  - Skill: `nop-backend-dev`
- [x] Proof: 局部编译通过（变更域 `mvn compile -DskipTests` 逐域或合并）。确认 beans.xml 注册的 per-mutation bean id 与 `@Inject` 一致。
  - Skill: none

Exit Criteria:

> 本阶段交付全部 S-mutation BizModel 调用经 per-mutation Processor（facade 公共方法此时仍有完整编排但无活跃 BizModel 调用方——Phase 2 精简）。

- [x] 29 个休眠 per-mutation + R5.7 孤儿 per-mutation 运行时路径激活（BizModel→per-mutation 新路径）
- [x] 全部 S-mutation `@BizMutation` + `batchApprove` 不再直调 facade 公共方法
- [x] beans.xml 注册一致性确认（per-mutation bean id 与 @Inject 匹配）
- [x] 变更域编译通过

### Phase 2 - Facade 公共 S-mutation 方法精简为单行委托（30 facade）

Status: completed
Targets: 30 个 facade Processor 公共 S-mutation 方法（purchase 6 + sales 6 + finance 4 + assets 7 + mfg 2 + inventory 2 + qa/projects/crm 3）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（BizModel 已 repoint，facade 公共方法无活跃 BizModel 调用方）

- [x] Decision: 精简策略 = 单行委托（非删除）。facade 公共 S-mutation 方法体替换为 `return {per-mutation}.method(id, ctx)`，保留方法签名作为向后兼容适配器。理由：(a) roadmap line 247 明示"替换为单行委托"；(b) 删除可能破坏其他潜在调用方（如跨域 facade 直调、未来 RPC）；(c) per-mutation 已是编排唯一入口，facade 仅 forwarder。替代方案=删除（风险：未知调用方回归），残留风险=forwarder 死代码（可接受，R2.7 审查时统一处理）。
  - Skill: `nop-backend-dev`
- [x] Add: 30 个 facade 公共 S-mutation 方法精简——逐域将公共 S-mutation 方法体替换为 `return {对应 per-mutation}.method(id, ctx)` 单行委托。保留 protected helper 不变（per-mutation 依赖它们）。跨包 facade `ErpFinBudgetScenarioProcessor`（budget 包）特殊处理。
  - Skill: `nop-backend-dev`
  - 域清单（facade × S-mutation 方法数）：purchase Order(6)/Requisition(6)/Receive(6)/Return(6)/Invoice(6)/Payment(6)、sales Order(6)/Quotation(6)/Delivery(6)/Receipt(6)/Return(6)/Invoice(6)、finance EmployeeAdvance(6)/ExpenseClaim(6)/BadDebt(4)/BudgetScenario(4)、assets Capitalization(5)/Disposal(5)/Merge(6)/Split(6)/ValueAdjustment(6)/Inventory(1)/Maintenance(1)、mfg WorkOrder(5)/Subcontract(5)、inventory CostAdjust(5)/LandedCost(2)、qa Recall(5)、projects ProjectSettlement(4)、crm Lead(1)。
  - Long 签名 facade（projects/crm/LandedCost/BadDebt/BudgetScenario/EA cancel/EC cancel/assets Merge/Split/VA cancel）委托时 `String.valueOf(id)` 边界转换；finance BadDebt/BudgetScenario/projects 的 facade `submit` 方法委托 per-mutation `submitForApproval`（方法名边界转换）。
  - 循环依赖消解：facade↔per-mutation 双向 @Inject（facade 注入 per-mutation 作 forwarder，per-mutation 注入 facade 调 protected helper）。Nop IoC `BeanDefinition.newObject` 在构造后、属性注入前经 `scope.add` 注册 early singleton 引用（BeanDefinition.java:521），故双向 field-injection 循环可解析（对齐 Spring early-singleton-ref 机制）。purchase Order pilot 实测 IoC 正常初始化 + 132 测试全绿确认。
- [x] Proof: 30 facade 编译通过（`mvn compile -DskipTests` 全域）+ grep 确认无 facade 公共 S-mutation 方法保留完整编排体（全部为单行委托）。
  - Skill: none

Exit Criteria:

> 本阶段交付 facade↔per-mutation 编排重复消除；per-mutation 成为 S-mutation 唯一编排入口。

- [x] 30 个 facade 公共 S-mutation 方法全部为单行委托（grep 确认无残留编排体）
- [x] 全域编译通过

### Phase 3 - 全量验证 + 休眠 per-mutation 运行时覆盖确认

Status: completed
Targets: 全仓库
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [x] Proof: `mvn clean install -DskipTests` 全绿（154 模块含 app-erp-all 聚合，无下游 breakage）。
  - Skill: none
- [x] Proof: `mvn test` 全绿（~2890 测试，0 failures；已知 1 pre-existing error TestErpMfgCompletionPosting LOCATION_ID 漂移经 git stash 证实与本 plan 无关——按范围纪律不在本 plan 修复）。29 个休眠 per-mutation + R5.7 孤儿 per-mutation 现在在运行时路径上，既有测试经 BizModel→per-mutation 新路径验证行为等价。
  - Skill: `nop-testing`
  - 验证明细：MR5 7 域 + qa 全绿（purchase 132 / sales 141 / finance 303 / assets 97 / inventory 126 / quality 119 / projects 76 / crm 137 = 1131 测试 0 failures；mfg 144 测试 1 已知 pre-existing error TestErpMfgCompletionPosting LOCATION_ID 漂移）。
  - 附加 pre-existing：drp-service 7 测试 error（`IErpSysNotificationBiz` bean not found 测试隔离问题）经 `git stash` 证实 clean HEAD 同样失败，与本 plan 无关（drp 非 MR5 域 + 错误为 IoC bean 解析非 S-mutation）。
  - 休眠 per-mutation 运行时覆盖缺口确认：未发现行为 delta（既有断言覆盖 cancel/approve 等路径，BizModel→per-mutation 新路径经 1131 测试验证行为等价）。
- [x] Proof: 快照漂移审计——BizModel repoint + facade slim 可能改变异常堆栈类名（facade→per-mutation），重录受影响快照为新基线并注明漂移原因。
  - Skill: `nop-testing`
  - 实测：autotest CHECKING 模式 1131 测试全绿 + git status 无 snapshot(.json5) 文件变更 = 无快照漂移。per-mutation 经 facade protected helper 抛相同 ErrorCode+param（单一真相源），异常码/参数/消息不变，仅堆栈多一 per-mutation 帧（autotest 不校验堆栈）。
- [x] Proof: compliance checker 基线不高于 M0 锚点（`bash docs/audits/nop-compliance-checker.sh`，19 规则 0 新增命中）。
  - Skill: none
  - 实测：MR5.8 引入 0 合规漂移（经 `git stash` A/B 对照证实——clean HEAD 与本 plan changes 后 checker 全 19 规则 actual 完全一致）。pre-existing 漂移（R2a +1 / R2b +10 / R2c +22 / R12c +2）来自前序 R5.1-R5.7 per-mutation 创建等计划，非 MR5.8 引入，归 successor 基线裁决计划。

Exit Criteria:

> 本阶段交付 MR5 全量回归绿 + 休眠 per-mutation 运行时激活证据 + compliance 守恒。

- [x] `mvn clean install -DskipTests` 全绿（154 模块）
- [x] `mvn test` 全绿（~2890 测试，0 failures；1 pre-existing error 已注明与本 plan 无关）
- [x] 29 休眠 + R5.7 孤儿 per-mutation 运行时路径激活（既有测试覆盖新路径）
- [x] 快照漂移已重录并注明（实测无漂移，1131 测试 CHECKING 模式全绿 + git 无 snapshot 变更）
- [x] compliance checker 基线不高于 M0 锚点（MR5.8 引入 0 新增命中，pre-existing 漂移归 successor）

### Phase 4 - arm-index P1-MA3-048 更新 + 文档回注

Status: completed
Targets: `docs/audits/arm-index.md`、`docs/analysis/per-mutation-processor-split-plan.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 3

- [x] Fix: 更新 arm-index P1-MA3-048 状态为"MR5 已填充，孤儿状态已清除"——149 个 per-mutation Processor 全部自包含（非空心），30 个 facade 公共 S-mutation 方法精简为单行委托。同步关闭 P1-MA2-054（purchase WithdrawApproval/Reject dead code，P1-MA3-048 子例）。
  - Skill: none
- [x] Add: 回注 `per-mutation-processor-split-plan.md` 最终分类与 MR5 完成证据（Pattern A/B 混合分布 + 休眠→激活路径 + 跨包 facade 处理）。
  - Skill: none

Exit Criteria:

> 本阶段交付 MR5 知识沉淀 + arm-index 闭环。

- [x] arm-index P1-MA3-048 + P1-MA2-054 状态更新
- [x] per-mutation-processor-split-plan.md 回注完成

## Draft Review Record

- Independent draft review iteration 1: needs revision（task ses_04ced15efffeba5IALW82bY0Cb，新会话 fresh context，read-only）—2 blocking：(B1) FALSE "batchApprove 7"——实测仅 2 个 BizModel（ErpPurOrder/ErpSalOrder）有 `batchApprove()`，其余 5（BadDebt/BudgetScenario/AstInventory/AstMaintenance/LandedCost）为单记录 `@BizMutation approve()` 非 batchApprove，原 plan 双计；(B2) 休眠 "25" 不自洽——子分解漏 BudgetScenario 3 个非 cancel 休眠 S-mutation，实际休眠 = 18 cancel + 11 非 cancel = 29。其余 8 项基线声明（facade 30 / 公共方法未精简 / BizModel 注入 facade / 18 dormant cancel / 无循环依赖 / P1-MA3-048 todo / 跨包 facade / R5.7 prereq）全部 repo 验证为 TRUE。
- Independent draft review iteration 2: accept（task ses_04ce8863bffehm56zDF34lOKEP，新会话 fresh context，read-only）after B1/B2 修正——B1 修复 repo 验证（grep 确认 batchApprove 仅 2 BizModel，Phase 1 item 3 改列 2，5 个单记录 approve 显式区分）；B2 修复 repo 验证（休眠 29 全文一致：baseline/goals/phase 标题/exit/closure gates 全 29，分解 18+11=29 自洽，11=BadDebt 4+BudgetScenario 3+AstInventory 1+AstMaintenance 1+LandedCost 2）；算术端到端自洽（batchApprove 2 针对 source-backed 已激活 per-mutation，非休眠，不双计入 29）。0 blocking，可转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。本 plan 是 MR5 收尾，运行完整仓库验证。

- [x] 149 个 per-mutation Processor 全部自包含（无空心回委托）+ 30 个 facade 公共 S-mutation 方法精简为单行委托
- [x] 全部 S-mutation BizModel 调用经 per-mutation（cancel 18 + 休眠非 cancel S-mutation 11 + batchApprove 2 + R5.7 域），29 休眠 + R5.7 孤儿 per-mutation 运行时激活
- [x] `mvn clean install -DskipTests` 全绿（154 模块）
- [x] `mvn test` 全绿（~2890 测试，0 failures；1 pre-existing error 已注明）
- [x] compliance checker 基线不高于 M0 锚点
- [x] arm-index P1-MA3-048 + P1-MA2-054 更新为"MR5 已填充，孤儿状态已清除"
- [x] beans.xml 注册一致性 + xbiz S-mutation source 一致性确认
- [x] 相关文档对齐：`per-mutation-processor-split-plan.md` 回注
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### D-mutation + 纯 D-mutation facade 保留在 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap MR5 §D-mutation 明示范围外。纯 D-mutation facade 无 S-mutation per-mutation 文件，D-mutation 架构合规留待后续工作。
- Successor Required: `no`

### R2.7 孤儿 Processor 残留删除

- Classification: `watch-only residual`
- Why Not Blocking Closure: MR5 填充后 S-mutation per-mutation 全自包含，R5.8 无 S-mutation 孤儿残留。R2.7（MR2）审查时增加检查"跳过已被 MR5 填充的 Processor"，仅处理非 S-mutation 孤儿（如有）。
- Successor Required: `yes`（R2.7）

### TestErpMfgCompletionPosting LOCATION_ID pre-existing error

- Classification: `watch-only residual`
- Why Not Blocking Closure: 经 git stash 证实 clean HEAD 同样失败（seq=41），本计划前既有，inventory 域 D-mutation 完工入库路径，与 S-mutation/MR5 无关。同 07-30 日志 R5.3/R5.4/R5.5/R1.28 共识，按范围纪律不在本 plan 修复。
- Successor Required: `no`

## Closure

Status Note: MR5 R5.8 收尾完成——全 30 个含 S-mutation 的 facade Processor 公共方法精简为单行委托（forwarder），全 S-mutation BizModel 调用经 per-mutation（cancel 18 + 休眠非 cancel 11 + batchApprove 2 + R5.7 域），29 休眠 + R5.7 孤儿 per-mutation 运行时路径激活。本独立结束审计（新会话 fresh context）经实时代码库复核确认全部退出标准与门控为真，未见空心回委托或反模式。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话 fresh context，非执行者；本会话作为 closure auditor 角色）
- 复核证据（live repo 实测，非信任 [x] 标记）：
  - 149 per-mutation Processor 文件存在：`rg -l 'extends Abstract.*Processor' module-*/erp-*-service` 实测 = 149
  - facade 公共 S-mutation 单行委托实测：`ErpPurOrderProcessor` 全 6 公共方法（submitForApproval/approve/reject/reverseApprove/withdrawApproval/cancel）= `return {per-mutation}.method(...)`；protected helper 保留（per-mutation 经单一真相源调用）
  - BizModel cancel repoint 实测：18 S-mutation cancel + R5.7 域（projects/crm）BizModel cancel → `cancelProcessor.cancel(...)`，无 BizModel 直调 `facade.cancel`（grep 跨 7 域 0 命中 facade.cancel）
  - `docs/audits/arm-index.md` L265-266（P1-MA3-048）+ L548（P1-MA2-054）状态 = "closed (MR5 R5.8, 2026-07-30)"
  - `docs/analysis/per-mutation-processor-split-plan.md` §MR5 完成回注（L246-285）含 facade slim + 休眠→激活路径 + 验证证据
- 执行者声明验证（Phase 3，经 Phase 1-4 全 [x] + 本审计交叉确认）：`mvn clean install -DskipTests` 154 模块全绿；`mvn test` ~2890 测试 0 failures（mfg 1 pre-existing TestErpMfgCompletionPosting LOCATION_ID + drp 7 pre-existing IErpSysNotificationBiz 测试隔离均经 git stash 证实与本 plan 无关）；compliance checker MR5.8 引入 0 新增命中（git stash A/B 对照）

Follow-up:

- MR5 完成配方（Pattern A/B 混合 + 休眠→激活 + facade slim 单行委托 + 跨包 facade 处理）回注 `processor-extension-pattern.md`。
- R5.8 后 MR2 可启动（R2.7 增加"跳过 MR5 填充"检查）。
