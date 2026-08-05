# 2026-08-05-2200-2 rc-ma1-a1-34-projects-f1-startup-cost-collection projects 域 projects-F1 立项与成本归集需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.34（MA1 需求追踪矩阵审计 — projects-F1 立项/工时人工成本凭证/多来源成本归集/暂停关闭约束）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.34
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.34 的 0.2 依赖）、`2026-08-05-2200-3-rc-ma1-a1-35-projects-f2-budget-dag.md`（projects 域同批 N=3，F2 预算/DAG 为成本归集的预算控制前置衔接）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.34 给出 UC 清单 = `UC-PRJ-01/02/03/09`（4 UC），含 `use-cases.md:15/30/49/147` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 projects 域第一个 RC 切片（projects 域共 3 切片 A1.34/A1.35/A1.36）。

- **L1 需求契约（权威真相源）**：`docs/design/projects/use-cases.md`（机制见 `state-machine.md`、`cost-collection.md §二/§四`）：
  - **UC-PRJ-01 项目立项**（`:15`）：项目: DRAFT → OPEN(立项)；OPEN 后: 允许新单据(工时/采购/领料/报销)标注该项目归集成本；DRAFT 状态: 不允许成本归集。
  - **UC-PRJ-02 工时提交触发人工成本凭证**（`:30`）：工时提交(项目=OPEN, 任务, 时长) → 校验 项目.状态==OPEN(暂停/关闭拒绝) → 成本率解析(优先级: **用户费率 > 角色费率 > 活动类型费率**) → 人工成本 = 时长 × 成本率 → 发布过账事件(businessType=**PROJECT_LABOR_COST**) → 生成凭证: 借 项目成本(人工), 贷 应付职工薪酬/劳务成本 → 工时.已过账 == true。
  - **UC-PRJ-03 多来源成本归集**（`:49`）：采购订单行.项目==P → 入库时成本归集到 P(**物料类**)；领料单.项目==P → 归集(**物料**)；费用报销.项目==P → 归集(**费用**)；各来源按成本分类(**人工/物料/费用/分包**)汇总到 ProjectPnl。
  - **UC-PRJ-09 项目暂停/关闭约束**（`:147`）：OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销)；ON_HOLD → OPEN(恢复): 恢复归集；→ COMPLETED/CANCELLED(关闭): 冻结,不可再归集,保留审计；关闭后历史成本/收入数据保留。
  - **L1 关键不变量**：① DRAFT 拒绝成本归集；② 成本率三级优先（用户>角色>活动类型）；③ 多来源归集含**物料（采购入库/领料）+ 分包**四分类；④ ON_HOLD/COMPLETED/CANCELLED 拒绝新归集；⑤ 关闭后历史保留。

- **L3 代码实现现状（实测）**——立项/工时成本/费用归集已实现，但**物料/分包来源完全缺失**、**成本率三级优先降级**：
  - **UC-PRJ-01 立项（✅ 前置校验已实现）**：
    - `ErpPrjProjectBizModel.startProject:89-102`（Facade）→ `validateStartPreconditions:136-165`（**项目信息完整**：name `:138-140`；**起止日期有效**：startDate/endDate 存在 + startDate≤endDate `:141-151`；**预算已定**：budget 非空 `:152-154`）；config-gated STRICT（默认抛 `ERR_PROJECT_START_PRECONDITION_FAILED` `:158-162`）/ WARN（LOG 放行 `:163-164`），`ErpPrjConfigs:30-31/110-115`。
    - DRAFT→OPEN 守卫 `:93-98`（status≠DRAFT 抛错，**复用 `ERR_PROJECT_NOT_CLOSABLE` 名**——误导性命名但行为正确）。
    - 成本归集门控 `requireReferenceable:62-73`（仅 `PROJECT_STATUS_OPEN` 通过，DRAFT/ON_HOLD/COMPLETED/CANCELLED 全拒 `ERR_PROJECT_NOT_REFERENCEABLE`）——**但该 API 生产代码零调用方**（工时路径内联自有校验，费用路径不校验状态，见下）。
  - **UC-PRJ-02 工时人工成本凭证（⚠️ 成本率降级 + businessType 偏差，其余 ✅）**：
    - `ErpPrjTimesheetBizModel.submit/approve`（Facade）→ `ErpPrjTimesheetSubmitProcessor.submit:34-61`：项目 OPEN 校验 `validateProjectReferenceable:65-78`（非 OPEN 抛 `ERR_TIMESHEET_PROJECT_NOT_OPEN`）；任务状态校验 `validateTaskAcceptsTimesheet:80-98`（仅 TODO/IN_PROGRESS 接受）。
    - **⚠️ 成本率解析降级（关键缺口）**：`CostRateResolver.resolve:40-67`（tier 1 timesheet.costRate `:41-44` > tier 2 activityType.costRate `:46-56` > tier 3 全局 config `erp-prj.default-labor-cost-rate` `:58-62`；类声明 `:29`）——**L1 要求"用户费率 > 角色费率 > 活动类型费率"三级，实现为"单填 > 活动类型 > 全局默认"**，**用户级/角色级费率层未实现**。类 Javadoc `:22-26` 显式声明 Non-Goal（"ORM 中 ErpPrjProjectUser.role 纯文本无费率列，无用户级/角色级独立费率载体；本期 Non-Goal"）。
    - 人工成本 = 时长 × 成本率：`CostRateResolver.computeCostAmount:82-87`，调用 `ErpPrjTimesheetSubmitProcessor.submit:51-53`（`.setScale(4, HALF_UP)`）✅。
    - **⚠️ businessType 偏差（L1 文本陈旧）**：`TimesheetPostingDispatcher.java:125 event.setBusinessType(ErpFinBusinessType.PROJECT_COST_COLLECTION)`（枚举 `ErpFinBusinessType.java:24 PROJECT_COST_COLLECTION(110)`）——**L1 写 `PROJECT_LABOR_COST`，代码无此常量**。`ProjectCostCollectionProvider.java:29-31` 注释声明"设计文档写作 PROJECT_LABOR_COST 为命名偏差，实际复用既有 PROJECT_COST_COLLECTION(110)"。**功能行为（借项目成本/贷应付职工薪酬凭证）已实现**（Dr subject 解析 `TimesheetPostingDispatcher.buildEvent:110-116` 从 projectType.defaultSubjectId，缺失抛 `ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED`；Cr subject `:118-122` 从 config `erp-prj.default-payroll-subject-id`，缺失抛 `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED`；默认 Dr `5101`/Cr `2211`；projectId 维度标注 `ProjectCostCollectionProvider:67,71`）。
    - posted 回写：`TimesheetPostingDispatcher.tryPost:60-74`（异常返回 false）→ `ErpPrjTimesheetApproveProcessor.approve:42-52`（**仅 tryPost 返回 true 时**设 posted=true/postedAt/postedBy `:47-51`，失败派发告警 `dispatchFailureAlert:77-94`）；status 仍迁移 APPROVED（`:44`，文档化容错设计 cost-collection.md:266）。**P1-MA2-068 已 resolved**（arm-index.md:375 确认）。
  - **UC-PRJ-03 多来源成本归集（❌ 物料/分包来源完全缺失，仅人工+费用）**：
    - **LABOR（工时）✅**：`ProjectCostAggregator.aggregateFromTimesheet:44-99`，`setCostCategory(COST_CATEGORY_LABOR)` `:59`。
    - **EXPENSE（费用报销）✅**：`ExpenseCostAggregator.refreshExpenseCost:56-126`，`setCostCategory(COST_CATEGORY_EXPENSE)` `:189`；经 `IErpFinExpenseClaimBiz.findList` Facade 只读 + `daoFor(ErpFinExpenseClaimLine)` 跨域只读；幂等 `existsLine:171-177`（sourceBillType=EXPENSE + sourceBillCode 去重）；config-gated，closeProject 前 `ErpPrjProjectCloseProjectProcessor.closeProject:63-65` 触发。
    - **MATERIAL（采购入库→项目）❌ 未实现**：inventory 模块对 `ErpPrjCostCollection`/`ErpPrjProject`/`projectId` **零引用**（grep `module-inventory` 零命中）；`InvPostingDispatcher.resolveBusinessType:152-178` 仅 emit PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT，**从不写项目成本**。采购订单行虽暴露 `project` FK（`_ErpPurOrderLine.java:1477-1481`）但**无聚合器消费**。
    - **MATERIAL（领料→项目）❌ 未实现**："领料"在本仓为制造专用（`ErpMfgMaterialIssue`/MFG_ISSUE），WIP 过账 `ManufacturingIssueAcctDocProvider:21-32` 写 Dr WIP `1411`/Cr Inventory `1401`，**从不写项目成本**。
    - **SUBCONTRACT ❌ 未实现**：常量 `ErpPrjConstants.java:92 COST_CATEGORY_SUBCONTRACT` 定义 + PnL 读 `ProjectPnlCalculator.java:187`，但**生产代码零 `setCostCategory(COST_CATEGORY_SUBCONTRACT)` 写入**。
    - **四分类汇总**：`ProjectPnlCalculator.sumCostByCategory:163-194` 支持 4 类（LABOR `:181`/MATERIAL `:183`/EXPENSE `:185`/SUBCONTRACT `:187`，未知归 EXPENSE `:189-191`）——**分类机制支持 4 类，但生产仅写入 2 类（LABOR/EXPENSE）**；MATERIAL/SUBCONTRACT 仅测试手工 seed（`TestErpPrjProjectPnl:82-84`/`TestErpPrjProjectSettlement:215`）。
  - **UC-PRJ-09 暂停/关闭约束（⚠️ 状态机 ✅ 但费用路径状态门控缺失，closeProject 任务校验已实现）**：
    - 暂停/恢复/关闭/取消：`holdProject:106-108`/`resumeProject:112-114`/`closeProject:83-85`/`cancelProject:118-130`（→ 各 per-mutation Processor：`ErpPrjProjectHoldProjectProcessor`/`ResumeProjectProcessor`/`CloseProjectProcessor`/inline cancel）。
    - **closeProject 任务结束校验 ✅ 已实现**：`ErpPrjProjectCloseProjectProcessor.closeProject:50-70` 调 `validateTasksFinished:80-95`（未结束状态集 {TODO,IN_PROGRESS,BLOCKED} `:36-39`；config-gated STRICT 默认抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS` `:88-91`/ WARN `:93-94`）——**MA2 报告 P1-MA2-067"缺任务结束校验"已 resolved**（arm-index 确认），本审计须复核 resolved 状态。
    - **⚠️ ON_HOLD 费用归集拒绝仅工时路径落实**：工时 `ErpPrjTimesheetSubmitProcessor.validateProjectReferenceable:65-78` 拒绝非 OPEN ✅；但**费用路径 `ExpenseCostAggregator.refreshExpenseCost:60-64` 仅查 `project==null`（缺失抛 `ERR_PROJECT_NOT_REFERENCEABLE`），从不校验项目状态**——ON_HOLD/COMPLETED/CANCELLED 项目的报销行仍被归集，**违反 UC-PRJ-09 AC-1/3"拒绝新费用归集"**。
    - **requireReferenceable 单一咽喉未被使用**：工时内联自有校验，费用跳过——"单一门控"承诺未落地。
    - 关闭后历史保留 ✅：closeProject/cancelProject 仅 setStatus + updateEntity，不删除归集行/工时/成本头；ORM `useLogicalDelete=true`。
  - **跨域 daoFor**：projects 跨域只读（`TimesheetPostingDispatcher daoFor(ErpMdSubject)` + `daoProvider.daoFor(ErpFinExpenseClaim/ErpFinExpenseClaimLine)` + `ErpPrjReportBizModel`）归 P1-MA1-022 todo MR1，本切片不重审。

- **L4 测试证据现状**（`module-projects/erp-prj-service/src/test/java/`）：
  - **强**：`TestErpPrjProjectPrecheck.java`（6 @Test：closeProject STRICT 拒未完成任务 `:53-67`/全 DONE 通过 `:70-81`/WARN 放行 `:84-100`；startProject STRICT 缺字段抛 `:105-118`/全字段通过 `:121-128`/WARN `:131-144`）、`TestErpPrjTimesheetCost.java`（7 @Test：成本率 tier1 覆盖 `:67-93`/tier2 回退 `:96-119`/无费率抛 `:122-140`/COMPLETED 项目拒 `:143-161`/BLOCKED 任务拒 `:164-182`/approve 过账 PROJECT_COST_COLLECTION Dr5101/Cr2211=8000 + projectId 维度精确 `:185-225`/非法迁移 `:228-247`）、`TestErpPrjBudgetAndCollection.java`（7 @Test：WARNING/STRICT + approve 产 LABOR 行 + actualCost 回写 + 幂等 + closeProject 冻结拒新工时 + 拒非 OPEN + requireReferenceable 拒 CANCELLED）、`TestErpPrjExpenseAggregation.java`（4 @Test：从已审报销归集 + 幂等 + config-gated + closeProject 前刷新）。
  - **强但手工 seed**：`TestErpPrjProjectPnl.java`（4 @Test，**手工 seed MATERIAL/SUBCONTRACT 行**——证明分类机制但非生产路径）、`TestErpPrjProjectSettlement.java`（含手工 seed MATERIAL 行）。
  - **故障路径**：`posting/TestTimesheetPostingFailureAlert.java`（1 @Test，失败派发 `prj.timesheet-posting-failure` 事件）、`posting/TestPrjPostingFaultInjection.java`（1 @Test，同告警经 FaultInjectionStubs）。
  - **E2E**：`tests/e2e/business-actions/projects-timesheet-posting.action.spec.ts`（强值断言 submit→approve(posted) Dr5101/Cr2211=800 + cancel→reversal 反向 + 2 非法迁移守卫）、`projects-settlement-posting.action.spec.ts`/`projects-pnl-settlement.action.spec.ts`（结算/损益相邻切片）。
  - **⚠️ 测试缺口**：① UC-PRJ-03 物料/分包来源**零生产测试**（仅手工 seed 证明分类读侧）；② UC-PRJ-09 ON_HOLD/resumeProject 迁移**零单测/E2E 覆盖**（仅 COMPLETED 侧拒绝测）；③ ExpenseCostAggregator 状态门控缺失无对应负向测试（ON_HOLD 项目报销行仍归集无断言拒绝）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`（A2.13）：项目 5 态 + 任务 4 态 + 工时审批轴状态机经实仓确认；**P1-MA2-067**（closeProject 缺任务结束校验）resolved（本切片 HEAD 复核 `ErpPrjProjectCloseProjectProcessor.validateTasksFinished:80-95` 已落地）；**P1-MA2-068**（tryPost 吞异常悬挂）resolved（`approve:42-52` 条件 posted + 告警派发）；**P1-MA2-069**（Milestone/Billing dict 死状态 + CostCollection docStatus APPROVED 不在字典）resolved；**P1-MA2-070**（startProject 缺前置 + cancelProject 多源）——本切片 HEAD 复核 startProject 前置**已实现**（validateStartPreconditions:136-165），cancelProject 多源仍存（P1-MA2-070 part resolved/pending 须核）；**P2-MA2-065/066**（state-machine.md 缺独立章节 + IErpFinAcctDocProvider 文字 drift watch-only）。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：复用 P1-MA2-067/068/069/070 resolved 状态机行为，只补需求视角差异（物料/分包来源缺失 / 成本率三级降级 / businessType PROJECT_LABOR_COST 文本 drift / ON_HOLD 费用路径状态门控缺失 / requireReferenceable 未被使用）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-067`（closeProject 任务校验 resolved）、`P1-MA2-068`（tryPost 悬挂 resolved）、`P1-MA2-069`（dict 死状态 resolved）、`P1-MA2-070`（startProject 前置 resolved + cancelProject 多源）、`P1-MA1-010`（多币种四件套 propId 缺失 todo MR1）、`P1-MA1-022`（跨域只读 todo MR1）。**RC 系列对 projects 为零**（本切片为 projects 域首批 RC 切片）。本切片须 grep arm-index prj cost-collection/material/requisition/subcontract/cost-rate/PROJECT_LABOR_COST/requireReferenceable 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。本切片候选偏差（物料/分包来源缺失 / 成本率降级 / ON_HOLD 费用门控）属**代码逻辑**类（预授权——BizModel/Aggregator 调整）；若物料归集须 inventory 侧新增 projectId 写入或 CostRateResolver 须 ORM 加费率列则触及跨域契约/ORM ask-first。

- **剩余差距**：A1.34 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.34 报告并登记 finding，解除 projects 域立项/成本归集切片证据缺口。

## Goals

- 产出 A1.34 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-PRJ-01/02/03/09 逐条核验**每条验收标准**（完整枚举，§3）：立项前置/DRAFT 拒归集、成本率三级/人工成本/凭证/posted、多来源四分类（人工/物料/费用/分包）、ON_HOLD/关闭拒绝新归集/历史保留 全链逐条。
- 对候选缺口给出分级结论：①UC-PRJ-03 物料（采购入库→项目）+ 分包来源**完全缺失**（倾向 **P1**，验收标准 AC-1/AC-2/AC-4 物料/分包归集功能完全缺失——须 §4 三判据复核 owner doc cost-collection.md §四 是否显式 documented simplification Deferred + 人工批准痕迹；若 AI 自标 Non-Goal 无人工批准→重开 P1，对齐 A1.24 UC-AST-03 先例）；②UC-PRJ-02 成本率三级降级为单填>活动类型>默认（倾向 **P1**，Non-Goal 由 CostRateResolver Javadoc AI 声明——§4 三判据复核：AI 声明 ≠ 人工批准）；③UC-PRJ-02 businessType PROJECT_LABOR_COST vs PROJECT_COST_COLLECTION（L1 文本陈旧，**行为已满足**——倾向**接受 + §9 真相源 drift 登记**，不复核不降级）；④UC-PRJ-09 ON_HOLD 费用路径状态门控缺失（ExpenseCostAggregator 不校验状态，倾向 **P1/P2**）；⑤requireReferenceable 单一咽喉未使用（倾向 **P2**）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（与既有 RC 系列协调，最新 P2-RC-043/P1-RC-041）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/cost-collection.md/state-machine.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.35 projects-F2 预算/DAG / A1.36 projects-F3 结算/看板 独立 plan；A1.34 只覆盖 UC-PRJ-01/02/03/09）。
- **不复审项目结算/转固/损益主路径**（UC-PRJ-06/07/08/10 属 A1.36；本切片仅核立项 + 成本归集 + 暂停/关闭约束）。
- **不重审 P1-MA2-067/068/069 状态机行为**（§去重协议：resolved，只补需求视角差异[物料/分包来源/成本率/ON_HOLD 费用门控]）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.34 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.34 UC 锚点）+ `docs/design/projects/use-cases.md`（L1 真相源）+ `docs/design/projects/state-machine.md` + `cost-collection.md §二/§四/§七`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.13 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjProjectPrecheck,TestErpPrjTimesheetCost,TestErpPrjBudgetAndCollection,TestErpPrjExpenseAggregation`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-PRJ-01/02/03/09 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:15/30/49/147` 验收标准原文；L2 引用 `state-machine.md §1/§2/§3/§4` + `cost-collection.md §二/§四/§七`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpPrjProjectBizModel`/`ErpPrjProjectCloseProjectProcessor`/`ErpPrjTimesheetSubmitProcessor`/`ErpPrjTimesheetApproveProcessor`/`CostRateResolver`/`TimesheetPostingDispatcher`/`ProjectCostCollectionProvider`/`ProjectCostAggregator`/`ExpenseCostAggregator`/`ProjectPnlCalculator`（含行号）；L4 引用上述测试类#method（注明断言强度 + 手工 seed 注记）；L5 复用 A2.13（P1-MA2-067/068/069/070 resolved）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：UC-PRJ-01 ①DRAFT→OPEN + 前置（startProject:89-102 + validateStartPreconditions:136-165 ✅）②OPEN 后允许归集（requireReferenceable:62-73 仅 OPEN）③DRAFT 拒归集（工时内联 ✅，**但 requireReferenceable 生产零调用方**）；UC-PRJ-02 ①项目 OPEN 校验（validateProjectReferenceable:65-78 ✅）②**成本率三级降级**（CostRateResolver:29 单填>活动类型>默认，**用户/角色层缺失**，Javadoc:22-26 AI 声明 Non-Goal）③人工成本（computeCostAmount:82-87 ✅）④**businessType 偏差**（PROJECT_COST_COLLECTION 实 vs PROJECT_LABOR_COST L1，行为满足[借项目成本/贷应付职工薪酬]）⑤凭证（buildEvent:110-122 + Provider:48-75 ✅）⑥posted（tryPost:60-74 + approve:42-52 条件 ✅，P1-MA2-068 resolved）；UC-PRJ-03 ①**采购入库→项目物料 ❌ 未实现**（inventory 零引用）②**领料→项目物料 ❌ 未实现**（制造专用 WIP）③费用报销→费用 ✅（ExpenseCostAggregator:56-126）④**四分类仅 2/4 生产写入**（LABOR/EXPENSE ✅，MATERIAL/SUBCONTRACT ❌ 无生产 writer，PnL 读侧支持）；UC-PRJ-09 ①ON_HOLD 拒新归集（**工时 ✅ / 费用 ❌ 不校验状态 :60-64**）②resume 恢复 ✅（代码，零测试）③closeProject 任务校验 ✅（validateTasksFinished:80-95，P1-MA2-067 resolved）+ 冻结 ④历史保留 ✅（不删除 + useLogicalDelete）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对 4 UC 给出符合性结论（取最高）：UC-PRJ-01 → 倾向**接受**（前置 + DRAFT 拒归集落实，requireReferenceable 未用属 P2 注记）；UC-PRJ-02 → 成本率三级降级倾向 **P1**（§4 三判据复核 CostRateResolver Javadoc AI 声明 Non-Goal：AI 声明 ≠ 人工批准，对齐 A1.24 UC-AST-03 先例）+ businessType 偏差**接受**（行为满足，§9 drift 登记）+ 其余接受；UC-PRJ-03 → 物料/分包来源缺失倾向 **P1**（§4 三判据复核 cost-collection.md §四 是否显式 Deferred + 人工批准；若 AI 自标 Non-Goal→重开 P1）+ 四分类 2/4 同根因；UC-PRJ-09 → closeProject 任务校验**接受**（resolved）+ ON_HOLD 费用门控缺失倾向 **P1/P2**（"拒绝新费用归集"部分未落实）。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（P1 项核 plan-audit/owner doc documented simplification/product-scope 裁剪 + 人工批准痕迹）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-PRJ-01/02/03/09 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L3 含行号、L4 注明断言强度（含手工 seed 注记）、L5 标注复用 A2.13 来源
- [ ] 4 UC 各有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-⑤ 有明确分级；物料/分包缺失 + 成本率降级有 §4 三判据复核路径（核 owner doc Deferred + 人工批准）；businessType 偏差有明确接受+drift 裁决

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` prj cost-collection/material/requisition/subcontract/cost-rate/PROJECT_LABOR_COST/requireReferenceable/onHold-expense 同域同控制点后裁决——closeProject 任务校验 + tryPost 悬挂**复用** P1-MA2-067/068（追加 RC A1.34 交叉引用，已 resolved）；物料/分包来源缺失 + 成本率降级 + ON_HOLD 费用门控为**新根因/新功能点**（既有 arm-index 无 RC finding 涉及 projects 物料归集/成本率三级/费用状态门控）→ 若确认 P1 则新建 `P*-RC-xxx`（与既有 RC 系列协调，最新 P2-RC-043/P1-RC-041）列明差异依据。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如 ExpenseCostAggregator 在 ON_HOLD 项目上 refreshExpenseCost 的实际运行时行为 / requireReferenceable 是否被任何未来 delta 调用 / 物料归集经 inventory 配置触发是否部分可达 / 多币种 exchangeRate=ONE 在多币种项目的运行时影响 等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-projects-state-machine.md`（A2.13 P1-MA2-067/068/069/070 resolved + 项目/任务/工时状态机已证实），列明只补的需求视角差异（物料/分包来源缺失 / 成本率三级降级 / businessType drift / ON_HOLD 费用门控 / requireReferenceable 未用）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；audit reports 表新增 A1.34 行。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [ ] 新 RC finding（若有）已写入 `arm-index.md`；P1-MA2-067/068 追加 RC A1.34 交叉引用；静态存疑点清单已登记（供 A4.1/A4.2 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_030952f38ffetipUOQmaGmIami，fresh session，未起草本计划）。全部 6 项 load-bearing 引用经实仓复核 CONFIRMED TRUE：①UC-PRJ-03 物料归集未实现（grep `module-inventory` `ErpPrjCostCollection|ErpPrjProject|projectId|PROJECT_COST` 零匹配 + InvPostingDispatcher.resolveBusinessType 无 project business type）；②仅 2/4 分类有生产 writer（`ProjectCostAggregator:59` LABOR + `ExpenseCostAggregator:189` EXPENSE，零生产 MATERIAL/SUBCONTRACT writer，后者仅常量+PnL 读侧+测试 seed）；③`CostRateResolver.java:40-67` 无用户/角色层（Javadoc :23-25 AI 声明 Non-Goal）；④`ErpPrjProjectCloseProjectProcessor.validateTasksFinished:80-95` EXISTS（查 {TODO,IN_PROGRESS,BLOCKED} + config-gated STRICT `ERR_PROJECT_HAS_UNFINISHED_TASKS`/WARN）——P1-MA2-067 genuinely RESOLVED；⑤`TimesheetPostingDispatcher:125` PROJECT_COST_COLLECTION（PROJECT_LABOR_COST 全仓零命中）；⑥`ExpenseCostAggregator.refreshExpenseCost:56-126` 仅查 projectId/project==null，不校验状态。requireReferenceable 生产零调用方确认；cost-collection.md §四（:157/:171）显式声明"采购入库/领料归集为本期 Non-Goal（successor）"——§4 三判据复核路径有真实既有声明可查。scope（UC-PRJ-01/02/03/09 only）、anti-slack（零禁用词）、exit criteria、methodology §1-§9 + §4 三判据（物料/分包→owner doc Deferred+人工批准；成本率→AI Non-Goal≠人工批准 引 A1.24 UC-AST-03 先例）+ MA2 reuse 全对齐。1 处亚行精度非阻塞注记（CostRateResolver.resolve 类声明 :29 vs 方法 :40，已据此修订为 `:40-67`），不影响可验证性。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.34 报告 9 段齐全 + UC-PRJ-01/02/03/09 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.34 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（物料/分包来源 / 成本率三级 / ON_HOLD 费用门控）属**代码逻辑**类（预授权——BizModel/Aggregator 调整）；物料归集若须 inventory 侧新增 projectId 写入则触及跨域契约 ask-first；成本率三级若须 ORM 加费率列则触及 ORM ask-first。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: <待执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理>
- Evidence: <待填>
