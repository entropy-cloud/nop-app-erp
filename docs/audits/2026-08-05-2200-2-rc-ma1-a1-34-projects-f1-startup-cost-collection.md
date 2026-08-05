# A1.34 projects-F1 立项与成本归集 需求-实现符合性五级追踪审计报告

> 报告类型：MA1（RC）需求-实现符合性五级追踪审计
> 切片：A1.34 projects-F1 立项与成本归集（项目立项 + 工时人工成本凭证 + 多来源成本归集 + 项目暂停/关闭约束）
> 审计范围：UC-PRJ-01 / UC-PRJ-02 / UC-PRJ-03 / UC-PRJ-09（4 UC，逐 UC 一矩阵行，§3 完整枚举）
> 真相源层级（§4 Q1）：L1 = `docs/design/projects/use-cases.md`（UC-PRJ-01 `:15` / UC-PRJ-02 `:30` / UC-PRJ-03 `:49` / UC-PRJ-09 `:147`）；L2 = `docs/design/projects/state-machine.md §1/§2/§4` + `cost-collection.md §二/§四/§七`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.13 + 本切片差异。
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`
> 计划：`docs/plans/2026-08-05-2200-2-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md`（A1.34 UC 清单 = UC-PRJ-01/02/03/09，覆盖率 ✅ 一致，无基线分歧）
> 结论速览：⚠️(P1) + 多项接受 + 1 P2 新登记 — **UC-PRJ-01 立项[接受 on 主断言，requireReferenceable 单一咽喉未使用 P2 新登记]** / **UC-PRJ-02 工时人工成本凭证[接受 on 主断言，成本率三级降级 P1 新登记]** / **UC-PRJ-03 多来源成本归集[人工 ✅ + 费用 ✅，物料 + 领料 + 分包 三类来源完全缺失 P1 新登记]** / **UC-PRJ-09 暂停/关闭约束[closeProject 任务校验 ✅ resolved P1-MA2-067 + 历史保留 ✅，ON_HOLD 费用路径状态门控缺失 P1 新登记]**。**零 P0**。**新登记 4 finding**：`P1-RC-048`（UC-PRJ-02 成本率三级降级）+ `P1-RC-049`（UC-PRJ-03 物料+领料+分包 三类归集来源缺失）+ `P1-RC-050`（UC-PRJ-09 ON_HOLD 费用路径状态门控缺失）+ `P2-RC-048`（UC-PRJ-01 requireReferenceable 单一咽喉未被使用）。**复用 4 resolved finding**：P1-MA2-067/068/069/070（全部 R1.16/R1.21 resolved，本切片 HEAD 复核确认无回退）。**接受 + drift 登记 1 项**：businessType `PROJECT_LABOR_COST`（L1 文本）vs `PROJECT_COST_COLLECTION`（实际枚举，行为满足）。

---

## 9. 与既有 MA2 报告差异增量声明（§6 段落 9，置顶便于去重）

本切片为 projects 域**第一批 RC 切片**（projects 域共 3 切片 A1.34/A1.35/A1.36，本切片覆盖 UC-PRJ-01/02/03/09 立项 + 成本归集 + 暂停/关闭约束；A1.35/A1.36 由独立 plan 覆盖 F2 预算/DAG + F3 结算/看板）。按 §去重协议，本报告**不复跑** MA2 状态机/业财链路审计，直接复用既有 MA2 报告已证实行为作为 L5 既有证据输入，只补"需求契约↔实际行为"差异。

### 复用 A2.13（projects 状态机审计，`docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`）

- **复用 A2.13 已证实行为**：项目 5 态主路径迁移守卫齐全（startProject/holdProject/resumeProject/closeProject/cancelProject 全 status 守卫，A2.13 §2.1）+ 任务 4 态主路径 + 任务依赖 DAG 成环检测（TaskDependencyValidator.detectCycle 完整实现，与 task-dag.md §2.1 算法 1:1 对应，A2.13 §2.5）+ 工时审批轴（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED 4 态）+ 项目结算三轴标准审批 + PnL CALCULATED 2 态（A2.13 §1 表 16 状态字段）。
- **复用 A2.13 P1 finding**（全部 resolved，本切片 HEAD 复核确认无回退）：
  | Finding | 描述 | Resolution | 本切片 HEAD 复核 |
  |---------|------|-----------|------------------|
  | **P1-MA2-067** | closeProject 未强制任务已结束（owner doc §迁移完整性 + §审查提示） | R1.21 done | `ErpPrjProjectCloseProjectProcessor.validateTasksFinished:80-95`（未结束状态集 {TODO,IN_PROGRESS,BLOCKED} + config-gated STRICT `ERR_PROJECT_HAS_UNFINISHED_TASKS`/WARN）已落地，**确认 resolved 无回退** |
  | **P1-MA2-068** | TimesheetPostingDispatcher tryPost 吞异常悬挂致 posted=false 无告警闭环 | R1.16 done | `ErpPrjTimesheetApproveProcessor.approve:42-52`（**仅 tryPost 返回 true 时**设 posted=true/postedAt/postedBy）+ `dispatchFailureAlert:77-94` 派发 `prj.timesheet-posting-failure`，**确认 resolved 无回退** |
  | **P1-MA2-069** | Milestone/Billing/CostCollection doc-status 字典语义复用偏移 + CRUD 桩死状态 | R1.21 done（方案 B Deferred） | `state-machine.md §适用对象三` 显式 Deferred 段落存在 + CostCollection APPROVED drift documented in `cost-collection.md §4.2:194`，**确认 resolved（resolved-via-deferral）无回退** |
  | **P1-MA2-070** | startProject 缺前置 + cancelProject 多源 | R1.21 done（part）| startProject 前置（`validateStartPreconditions:136-165`）已落地确认 resolved；cancelProject 多源 owner doc §迁移完整性补充（state-machine.md `:36-38`）已落地，**确认 resolved 无回退** |
- **复用 A2.13 P2 watch-only**：P2-MA2-065（state-machine.md 缺 5 状态承载实体独立章节，部分 resolved 经 §适用对象三 Deferred 段落）/ P2-MA2-066（state-machine.md §7 IErpFinAcctDocProvider vs 实现 IErpFinVoucherBiz 文字漂移）——本切片不投影，与本切片 4 UC 无控制点重叠。
- **复用 A2.13 已登记 MA1 finding 状态机角度复核**（无升级）：P1-MA1-010 多币种四件套 propId 缺失（工时过账 buildEvent 硬编码 exchangeRate=ONE 单币种路径，状态机角度无升级，**维持 todo MR1**）/ P1-MA1-022 跨域只读 daoFor ErpMdSubject + ErpFinExpenseClaimLine（**维持 todo MR1**）。

### 本切片只补的需求视角差异（5 项）

1. **UC-PRJ-02 成本率三级降级**（**新根因**——既有 arm-index 无 RC finding 涉及工时成本率层级缺失）：L1 `use-cases.md:38` 逐字「成本率解析(优先级: 用户费率 > 角色费率 > 活动类型费率)」要求**三级**优先级，L3 `CostRateResolver.resolve:40-67` 实为「单填(timesheet.costRate) > 活动类型(activityType.costRate) > 全局 config(erp-prj.default-labor-cost-rate)」**三级但层级错位**——用户级/角色级独立费率载体未实现（ORM `ErpPrjProjectUser.role` 为纯文本无费率列）。owner doc `cost-collection.md §2.2:58-61` 显式声明「本期实现为『单填 > 活动类型 > 全局默认』...用户级/角色级独立费率载体本期不存在，为 Non-Goal」，但 git log 显示该 Non-Goal 标注全为 AI-authored `docs:` / `docs(audit-remediation):` commits 无人工批准痕迹（新 finding **P1-RC-048**，§4 三判据复核：AI 自标 Non-Goal ≠ 人工批准，对齐 A1.24 UC-AST-03 / A1.8 UC-MFG-05 先例）。
2. **UC-PRJ-03 物料 + 领料 + 分包 三类归集来源缺失**（**新根因**——既有 arm-index 无 RC finding 涉及 projects 物料/分包归集）：L1 `use-cases.md:55-58` 逐字「采购订单行.项目 == P → 入库时成本归集到 P(物料类) / 领料单.项目 == P → 归集(物料) / 各来源按成本分类(人工/物料/费用/分包)汇总到 ProjectPnl」要求**四分类**（人工/物料/费用/分包），L3 实仓仅 `ProjectCostAggregator:59 setCostCategory(LABOR)` + `ExpenseCostAggregator:189 setCostCategory(EXPENSE)` **2/4 分类生产 writer**；MATERIAL（采购入库→项目）inventory 模块对 `ErpPrjCostCollection/ErpPrjProject/projectId/PROJECT_COST/project` **零引用**（实仓 grep 实证），`InvPostingDispatcher.resolveBusinessType:152-178` 仅 emit PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT；MATERIAL（领料→项目）本仓为制造专用（`ErpMfgMaterialIssue`/MFG_ISSUE）写 WIP `1411`/Cr Inventory `1401`，从不写项目成本；SUBCONTRACT 常量 `ErpPrjConstants.COST_CATEGORY_SUBCONTRACT` 定义 + PnL 读侧 `ProjectPnlCalculator:187` 但**生产代码零 `setCostCategory(SUBCONTRACT)` writer**。owner doc `cost-collection.md §4.1:171` 显式声明「采购入库/领料归集为本期 Non-Goal（successor）」，分包来源未声明；git log 显示该 Non-Goal 标注全为 AI commits 无人工批准痕迹（新 finding **P1-RC-049**，§4 三判据复核不满足人工批准意义）。
3. **UC-PRJ-09 ON_HOLD 费用路径状态门控缺失**（**新根因**——既有 arm-index 无 RC finding 涉及 ExpenseCostAggregator 状态门控）：L1 `use-cases.md:153` 逐字「OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销)」，L3 工时路径 `ErpPrjTimesheetSubmitProcessor.validateProjectReferenceable:65-78` 拒绝非 OPEN ✅，但**费用路径 `ExpenseCostAggregator.refreshExpenseCost:56-126` 仅查 `project==null`（`:60-64` 缺失抛 `ERR_PROJECT_NOT_REFERENCEABLE`），从不校验项目状态**——ON_HOLD/COMPLETED/CANCELLED 项目的报销行仍被归集，违反 UC-PRJ-09 AC-1「拒绝新费用归集」。owner doc `state-machine.md §1:15` ON_HOLD 明确「可被新单据引用 = 否（新费用不归集）」+ `§4:53`「暂停后仍有费用流入→配置控制：暂停项目拒绝新费用归集」（新 finding **P1-RC-050**）。
4. **UC-PRJ-01 requireReferenceable 单一咽喉未被使用**（**新根因**——既有 arm-index 无 RC finding 涉及此控制点）：L2 owner doc `state-machine.md §7:76` 声明「采购/销售/费用单据引用项目→通过 projectId 字段标注，项目关闭后拒绝引用」隐含统一门控语义。L3 `ErpPrjProjectBizModel.requireReferenceable:62-73` 仅 OPEN 通过（DRAFT/ON_HOLD/COMPLETED/CANCELLED 全拒 `ERR_PROJECT_NOT_REFERENCEABLE`）实现正确，**但全 module-projects/inventory/purchase/sales 生产代码 grep 零调用方**（实仓 grep 实证）——工时路径内联自有 `validateProjectReferenceable`，费用路径完全不校验状态。**双门控承诺未落地**（新 finding **P2-RC-048**，watch-only：单一咽喉 API 存在但未被消费，主路径[工时状态门控]OK，跨域/费用门控弱）。
5. **UC-PRJ-02 businessType `PROJECT_LABOR_COST` 文本 drift**（**接受 + §9 真相源 drift 登记**，不降级）：L1 `use-cases.md:40` 逐字「发布过账事件(businessType=PROJECT_LABOR_COST)」，L3 实仓 `TimesheetPostingDispatcher:125 event.setBusinessType(ErpFinBusinessType.PROJECT_COST_COLLECTION)`（枚举 `ErpFinBusinessType.java:24 PROJECT_COST_COLLECTION(110)`），**L1 文本字面常量 `PROJECT_LABOR_COST` 在仓库零代码命中**。owner doc `cost-collection.md §八:302-304` 显式 documented「设计原文写作 `PROJECT_LABOR_COST` 为命名偏差，实际复用既有 `ErpFinBusinessType.PROJECT_COST_COLLECTION(110)` 枚举（保持『不新增 finance 契约』边界）」+ `use-cases.md:198` 同步注记。**行为完全满足**（凭证 Dr 5101 项目成本 / Cr 2211 应付职工薪酬 + projectId 维度标注 `ProjectCostCollectionProvider:67,71`）。**裁决**：L1 字面常量为命名偏差，行为满足验收标准「发布过账事件 + 生成凭证」；按 §9 冻结条款，分歧记入报告不直改真相源（§9 真相源冻结 + drift 登记）。

---

## 1. 需求契约原文（逐字引用，§1 L1 格式）

> 来源 `docs/design/projects/use-cases.md`，逐字引用验收标准（禁止转述）。

### UC-PRJ-01 项目立项（`:15`）
```
项目: DRAFT → OPEN(立项)
OPEN 后: 允许新单据(工时/采购/领料/报销)标注该项目归集成本
DRAFT 状态: 不允许成本归集
```

**断言计数（逐条完整枚举，禁止抽样）**：UC-PRJ-01 共 **3 条验收标准**：
- **断言①**「项目: DRAFT → OPEN(立项)」（状态迁移 DRAFT→OPEN）
- **断言②**「OPEN 后: 允许新单据(工时/采购/领料/报销)标注该项目归集成本」（OPEN 允许归集 + 四类单据来源）
- **断言③**「DRAFT 状态: 不允许成本归集」（DRAFT 拒绝归集）

### UC-PRJ-02 工时提交触发人工成本凭证（`:30`）
```
工时提交(项目=OPEN, 任务, 时长) →
  校验 项目.状态 == OPEN(暂停/关闭拒绝)
  成本率解析(优先级: 用户费率 > 角色费率 > 活动类型费率)
  人工成本 = 时长 × 成本率
  发布过账事件(businessType=PROJECT_LABOR_COST)
生成凭证: 借 项目成本(人工), 贷 应付职工薪酬/劳务成本
工时.已过账 == true
```

**断言计数**：UC-PRJ-02 共 **6 条验收标准**：
- **断言①**「工时提交(项目=OPEN, 任务, 时长)」（前置：项目=OPEN + 任务 + 时长）
- **断言②**「校验 项目.状态 == OPEN(暂停/关闭拒绝)」（状态校验 + 暂停/关闭拒绝）
- **断言③**「成本率解析(优先级: 用户费率 > 角色费率 > 活动类型费率)」（**三级优先**成本率解析）
- **断言④**「人工成本 = 时长 × 成本率」（算术公式）
- **断言⑤**「发布过账事件(businessType=PROJECT_LABOR_COST) / 生成凭证: 借 项目成本(人工), 贷 应付职工薪酬/劳务成本」（过账事件 + 凭证借贷方向）
- **断言⑥**「工时.已过账 == true」（posted 回写）

### UC-PRJ-03 多来源成本归集（`:49`）
```
采购订单行.项目 == P → 入库时成本归集到 P(物料类)
领料单.项目 == P → 归集(物料)
费用报销.项目 == P → 归集(费用)
各来源按成本分类(人工/物料/费用/分包)汇总到 ProjectPnl
```

**断言计数**：UC-PRJ-03 共 **4 条验收标准**：
- **断言①**「采购订单行.项目 == P → 入库时成本归集到 P(物料类)」（采购入库→项目物料类）
- **断言②**「领料单.项目 == P → 归集(物料)」（领料→项目物料）
- **断言③**「费用报销.项目 == P → 归集(费用)」（费用报销→项目费用）
- **断言④**「各来源按成本分类(人工/物料/费用/分包)汇总到 ProjectPnl」（**四分类**汇总）

### UC-PRJ-09 项目暂停/关闭约束（`:147`）
```
OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销)
ON_HOLD → OPEN(恢复): 恢复归集
→ COMPLETED/CANCELLED(关闭): 冻结,不可再归集,保留审计
关闭后历史成本/收入数据保留
```

**断言计数**：UC-PRJ-09 共 **5 条验收标准**：
- **断言①**「OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销)」（暂停迁移 + 三类费用拒绝）
- **断言②**「ON_HOLD → OPEN(恢复): 恢复归集」（恢复迁移 + 恢复归集）
- **断言③**「→ COMPLETED/CANCELLED(关闭): 冻结,不可再归集」（关闭迁移 + 冻结）
- **断言④**「保留审计」（关闭后保留审计）
- **断言⑤**「关闭后历史成本/收入数据保留」（历史保留）

---

## 2. 实现证据（代码路径，§1 L3 格式，含跨域调用链）

> 全部 `module-projects/erp-prj-service/src/main/...` + 跨域 `module-inventory/erp-inv-service` + `module-finance/erp-fin-service`，含行号。

| 控制点 | 代码路径（file:line） | 备注 |
|--------|----------------------|------|
| **项目立项迁移（DRAFT→OPEN）** | `entity/ErpPrjProjectBizModel.java:89-102`（thin Facade，R6.6）→ `validateStartPreconditions:136-165`（项目信息完整：name `:138-140`；起止日期有效：startDate/endDate 存在 + startDate≤endDate `:141-151`；预算已定：budget 非空 `:152-154`；config-gated STRICT 默认抛 `ERR_PROJECT_START_PRECONDITION_FAILED` `:158-162` / WARN LOG.warn 放行 `:163-164`）+ DRAFT→OPEN 守卫 `:93-98`（status≠DRAFT 抛 `ERR_PROJECT_NOT_CLOSABLE`） | R1.21 resolved P1-MA2-070 part ✅ |
| **项目引用单一咽喉（requireReferenceable）** | `entity/ErpPrjProjectBizModel.java:62-73`（仅 `PROJECT_STATUS_OPEN` 通过，DRAFT/ON_HOLD/COMPLETED/CANCELLED 全拒 `ERR_PROJECT_NOT_REFERENCEABLE`）——**生产代码零调用方**（实仓 grep `module-projects/erp-prj-service/src/main` + `module-inventory` + `module-purchase` + `module-sales` 全零命中，唯一引用为本类 Javadoc `:35`） | P2-RC-048（咽喉未被使用） |
| **工时项目状态校验（内联）** | `processor/ErpPrjTimesheetSubmitProcessor.java:65-78 validateProjectReferenceable`（非 OPEN 抛 `ERR_TIMESHEET_PROJECT_NOT_OPEN`）+ 任务状态校验 `validateTaskAcceptsTimesheet:80-98`（仅 TODO/IN_PROGRESS 接受） | 内联自有校验，非调 requireReferenceable |
| **成本率解析（三级降级）** | `cost/CostRateResolver.java:29 类声明` + `resolve:40-67`：tier 1 `timesheet.costRate` `:41-44` > tier 2 `activityType.costRate` `:46-56` > tier 3 全局 config `erp-prj.default-labor-cost-rate` `:58-62`；Javadoc `:22-26` 显式声明 Non-Goal（"ORM 中 ErpPrjProjectUser.role 纯文本无费率列，无用户级/角色级独立费率载体；本期 Non-Goal"）；三处皆无抛 `ERR_COST_RATE_NOT_AVAILABLE` `:64-66` | **P1-RC-048**（用户/角色层缺失） |
| **人工成本计算** | `CostRateResolver.computeCostAmount:82-87`（hours × costRate）→ `ErpPrjTimesheetSubmitProcessor.submit:51-53` 调用（`.setScale(4, HALF_UP)`） | ✅ |
| **工时过账事件 + 凭证** | `posting/TimesheetPostingDispatcher.java:125 event.setBusinessType(ErpFinBusinessType.PROJECT_COST_COLLECTION)`（枚举 `ErpFinBusinessType.java:24 PROJECT_COST_COLLECTION(110)`，**非 L1 字面 PROJECT_LABOR_COST**）→ Dr subject 解析 `buildEvent:110-116`（从 projectType.defaultSubjectId，缺失抛 `ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED`，默认 5101）+ Cr subject `:118-122`（从 config `erp-prj.default-payroll-subject-id`，缺失抛 `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED`，默认 2211）→ 跨域 `ProjectPostingExecutor` → `IErpFinVoucherBiz.post Facade REQUIRES_NEW`；`ProjectCostCollectionProvider:29-31` 注释声明「设计文档写作 PROJECT_LABOR_COST 为命名偏差」+ projectId 维度标注 `:67,71` | **行为满足**（Dr 5101/Cr 2211 + projectId 维度），L1 文本 drift → §9 登记 |
| **posted 回写** | `TimesheetPostingDispatcher.tryPost:60-74`（异常返回 false）→ `ErpPrjTimesheetApproveProcessor.approve:42-52`（**仅 tryPost 返回 true 时**设 posted=true/postedAt/postedBy `:47-51`，失败派发 `dispatchFailureAlert:77-94` `prj.timesheet-posting-failure`）；status 仍迁移 APPROVED `:44`（owner doc `cost-collection.md §6.1:266` 文档化容错设计） | R1.16 resolved P1-MA2-068 ✅ |
| **LABOR 归集（工时）** | `cost/ProjectCostAggregator.java:44-99 aggregateFromTimesheet`，`setCostCategory(COST_CATEGORY_LABOR)` `:59`；增量回写 actualCost `:89-94` | ✅ 生产 writer（2/4 之一） |
| **EXPENSE 归集（费用报销）** | `cost/ExpenseCostAggregator.java:56-126 refreshExpenseCost`：`loadProject:60-64`（**仅 project==null 抛 ERR_PROJECT_NOT_REFERENCEABLE，从不校验状态**）→ 跨域只读 `IErpFinExpenseClaimBiz.findList` Facade + `daoFor(ErpFinExpenseClaimLine)`；`setCostCategory(COST_CATEGORY_EXPENSE)` `:189`；幂等 `existsLine:171-177`（sourceBillType=EXPENSE + sourceBillCode 去重）；config-gated，closeProject 前 `ErpPrjProjectCloseProjectProcessor.closeProject:63-65` 触发 | ✅ 生产 writer（2/4 之二）；**P1-RC-050**（无状态门控） |
| **MATERIAL 归集（采购入库→项目）❌** | `module-inventory/erp-inv-service/src/main` grep `ErpPrjCostCollection\|ErpPrjProject\|projectId\|PROJECT_COST\|project` **零业务命中**（仅 implicits/无关字段）；`InvPostingDispatcher.resolveBusinessType:152-178` 仅 emit PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT，**从不写项目成本**。采购订单行暴露 `project` FK（`_ErpPurOrderLine.java:1477-1481`）但**无聚合器消费** | **P1-RC-049**（物料归集完全缺失） |
| **MATERIAL 归集（领料→项目）❌** | "领料"在本仓为制造专用（`ErpMfgMaterialIssue`/MFG_ISSUE）：`ManufacturingIssueAcctDocProvider:21-32` 写 Dr WIP `1411`/Cr Inventory `1401`，**从不写项目成本** | **P1-RC-049**（同上） |
| **SUBCONTRACT 归集 ❌** | 常量 `ErpPrjConstants.java:92 COST_CATEGORY_SUBCONTRACT` 定义 + PnL 读 `ProjectPnlCalculator.java:187`，但**生产代码零 `setCostCategory(COST_CATEGORY_SUBCONTRACT)` 写入**（实仓 grep 实证，仅测试手工 seed `TestErpPrjProjectPnl:82-84`/`TestErpPrjProjectSettlement:215`） | **P1-RC-049**（同上） |
| **四分类汇总（读侧）** | `pnl/ProjectPnlCalculator.java:163-194 sumCostByCategory`（4 类全支持：LABOR `:181`/MATERIAL `:183`/EXPENSE `:185`/SUBCONTRACT `:187`/未知归 EXPENSE `:189-191`） | 分类机制支持 4 类，生产仅写入 2 类 |
| **暂停/恢复/关闭/取消迁移** | `ErpPrjProjectBizModel.holdProject:106-108`/`resumeProject:112-114`/`closeProject:83-85`/`cancelProject:118-130`（R6.6 thin Facade）→ 各 per-mutation Processor（`ErpPrjProjectHoldProjectProcessor`/`ResumeProjectProcessor`/`CloseProjectProcessor`/inline cancel） | ✅ 迁移齐全 |
| **closeProject 任务结束校验** | `processor/ErpPrjProjectCloseProjectProcessor.java:50-70 closeProject` → `validateTasksFinished:80-95`（未结束状态集 {TODO,IN_PROGRESS,BLOCKED} `:36-39`；config-gated STRICT 默认抛 `ERR_PROJECT_HAS_UNFINISHED_TASKS` `:88-91`/WARN `:93-94`）+ 冻结（仅 setStatus + updateEntity） | R1.21 resolved P1-MA2-067 ✅ |
| **历史保留（关闭后）** | closeProject/cancelProject 仅 setStatus + updateEntity，不删除归集行/工时/成本头；ORM `useLogicalDelete=true`（`app-erp-projects.orm.xml` 全状态机实体声明） | ✅ |

---

## 3. 测试证据（测试断言，§1 L4 格式，注明断言强度）

> 全部 `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/` + `tests/e2e/business-actions/`。

### 强断言（立项 + 工时成本 + 预算 + 归集）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `entity/TestErpPrjProjectPrecheck.java`（6 @Test：closeProject STRICT 拒未完成任务 `:53-67`/全 DONE 通过 `:70-81`/WARN 放行 `:84-100`；startProject STRICT 缺字段抛 `:105-118`/全字段通过 `:121-128`/WARN `:131-144`） | 强 | UC-PRJ-01 AC-① + UC-PRJ-09 AC-③ 任务结束 + 立项前置 |
| `service/TestErpPrjTimesheetCost.java`（7 @Test：成本率 tier1 覆盖 `:67-93`/tier2 回退 `:96-119`/无费率抛 `:122-140`/COMPLETED 项目拒 `:143-161`/BLOCKED 任务拒 `:164-182`/approve 过账 PROJECT_COST_COLLECTION Dr5101/Cr2211=8000 + projectId 维度精确 `:185-225`/非法迁移 `:228-247`） | 强 | UC-PRJ-02 AC-②③④⑤⑥（**注**：tier1=timesheet.costRate 单填，tier2=activityType.costRate；**用户级/角色级无测试**因实现缺失）；Dr/Cr 数值精确 |
| `service/TestErpPrjBudgetAndCollection.java`（7 @Test：WARNING/STRICT + approve 产 LABOR 行 + actualCost 回写 + 幂等 + closeProject 冻结拒新工时 + 拒非 OPEN + requireReferenceable 拒 CANCELLED） | 强 | UC-PRJ-01 AC-②③ + UC-PRJ-02 AC-①② + UC-PRJ-09 AC-①③ + UC-PRJ-04 跨切片 |
| `service/TestErpPrjExpenseAggregation.java`（4 @Test：从已审报销归集 + 幂等 + config-gated + closeProject 前刷新） | 强 | UC-PRJ-03 AC-③ EXPENSE 归集（**注**：未覆盖 ON_HOLD/COMPLETED 状态拒绝——与 P1-RC-050 实现缺口一致） |

### 强但手工 seed（证明分类机制读侧，非生产路径）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `service/TestErpPrjProjectPnl.java`（4 @Test，**手工 seed MATERIAL/SUBCONTRACT 行**——证明分类机制但非生产路径） | 强（读侧） | UC-PRJ-03 AC-④ 四分类读侧机制；**生产 writer 缺失**与 P1-RC-049 一致 |
| `service/TestErpPrjProjectSettlement.java`（含手工 seed MATERIAL 行） | 强（读侧） | 同上 |

### 故障路径（过账失败告警）
| 测试方法 | 断言强度 | 备注 |
|---------|---------|------|
| `service/posting/TestTimesheetPostingFailureAlert.java`（1 @Test，失败派发 `prj.timesheet-posting-failure` 事件） | 强 | UC-PRJ-02 AC-⑥ posted 回写失败路径（R1.16 resolved P1-MA2-068） |
| `service/posting/TestPrjPostingFaultInjection.java`（1 @Test，同告警经 FaultInjectionStubs） | 强 | 同上 |

### E2E（`tests/e2e/business-actions/`）
| Spec | 断言强度 | 备注 |
|------|---------|------|
| `business-actions/projects-timesheet-posting.action.spec.ts` | 强值断言 | submit→approve(posted) Dr5101/Cr2211=800 + cancel→reversal 反向 + 2 非法迁移守卫 |
| `business-actions/projects-settlement-posting.action.spec.ts` / `projects-pnl-settlement.action.spec.ts` | 强值断言 | 结算/损益相邻切片（A1.36） |

### ⚠️ 测试缺口（与功能缺口一致）
1. **UC-PRJ-02 成本率三级**：tier1/tier2 测试存在，**用户级/角色级费率零测试**（与 P1-RC-048 实现缺口一致）；
2. **UC-PRJ-03 物料 + 分包**：**零生产路径测试**（仅手工 seed 证明分类读侧，与 P1-RC-049 实现缺口一致）；
3. **UC-PRJ-09 ON_HOLD/resumeProject 迁移**：**零单测/E2E 覆盖**（仅 COMPLETED 侧拒绝测；ON_HOLD 迁移 + resumeProject 恢复零行为测试）；
4. **UC-PRJ-09 ExpenseCostAggregator 状态门控缺失**：ON_HOLD/COMPLETED 项目报销行仍归集无断言拒绝（与 P1-RC-050 实现缺口一致）；
5. **UC-PRJ-01 requireReferenceable**：作为公共 API 零调用方测试（与 P2-RC-048 实现缺口一致）。

---

## 4. 运行时行为证据（§1 L5 格式）

> 按 §去重协议，本切片复用 A2.13 已证实行为，只补需求视角差异。

### 复用 A2.13（projects 状态机审计）已证实行为
- **项目 5 态主路径迁移守卫齐全**（A2.13 §2.1）：startProject/holdProject/resumeProject/closeProject/cancelProject 全 status 守卫；本切片 HEAD 复核 R6.6 拆分后 per-mutation Processor 行为等价。
- **任务 4 态 + DAG 成环检测**（A2.13 §2.5）：`TaskDependencyValidator.detectCycle` 上行链 + HashSet + maxDepth + 自环优先 + 跨项目校验 + 保存/更新双钩子，与 `task-dag.md §2.1` 算法 1:1 对应。
- **工时审批轴 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED**（A2.13 §1 表）：标准审批 + posted 三件套。
- **跨域写经 I*Biz Facade 合规**（A2.13 §1 表）：`TimesheetPostingDispatcher → ProjectPostingExecutor → IErpFinVoucherBiz.post REQUIRES_NEW` 跨域失败隔离 + `ExpenseCostAggregator → IErpFinExpenseClaimBiz Facade` 只读，production 代码无 `daoFor(ErpFin*).saveEntity/updateEntity` 跨域写。

### 复用 A2.13 P1 finding HEAD 复核（全部 resolved 无回退）

| Finding | A2.13 状态 | 本切片 HEAD 行号复核 | 复核结论 |
|---------|-----------|---------------------|---------|
| **P1-MA2-067** | ✅ resolved R1.21 | `ErpPrjProjectCloseProjectProcessor.validateTasksFinished:80-95` EXISTS（{TODO,IN_PROGRESS,BLOCKED} + STRICT `ERR_PROJECT_HAS_UNFINISHED_TASKS`/WARN） | **确认 resolved 无回退**（genuinely RESOLVED via implementation） |
| **P1-MA2-068** | ✅ resolved R1.16 | `ErpPrjTimesheetApproveProcessor.approve:42-52`（条件 posted + `dispatchFailureAlert:77-94`）+ `TestTimesheetPostingFailureAlert` 强断言 | **确认 resolved 无回退**（genuinely RESOLVED via implementation） |
| **P1-MA2-069** | ✅ resolved R1.21（方案 B Deferred） | `state-machine.md §适用对象三` Deferred 段落 + `cost-collection.md §4.2:194` APPROVED drift documented | **确认 resolved（resolved-via-deferral）无回退**——属 doc-only Deferred 非 implementation；**本切片不重开**（§去重协议：不重审 audit-remediation MA2 行为裁决），但与 P1-RC-049 物料归集缺失需求契约维度互补（不同审计轴） |
| **P1-MA2-070** | ✅ resolved R1.21 | startProject 前置 `validateStartPreconditions:136-165` EXISTS（name/startDate/endDate/budget + STRICT `ERR_PROJECT_START_PRECONDITION_FAILED`/WARN）+ cancelProject 多源 owner doc state-machine.md `:36-38` 补充 | **确认 resolved 无回退**（genuinely RESOLVED via implementation） |

### 本切片 L5 行为判读（结合 L3 代码静态分析 + 既有测试）
- **UC-PRJ-01**：DRAFT→OPEN 迁移 + 立项前置 + requireReferenceable 单一咽喉 API 存在；**但 requireReferenceable 生产代码零调用方**——工时路径内联自有 `validateProjectReferenceable`，费用路径完全不校验状态。主路径（工时状态门控）✅，跨域/费用门控弱。
- **UC-PRJ-02**：项目 OPEN 校验 + 人工成本计算 + 凭证生成（Dr 5101/Cr 2211 + projectId 维度）+ posted 回写（条件 + 失败告警）全链已实现（`TestErpPrjTimesheetCost` 7 @Test 强断言 + E2E `projects-timesheet-posting` 强值断言）；**但成本率三级降级**（用户/角色层缺失）+ businessType drift（行为满足）。
- **UC-PRJ-03**：LABOR（工时）+ EXPENSE（费用报销）归集 ✅ 实现；**MATERIAL（采购入库 + 领料）+ SUBCONTRACT 完全缺失**（inventory 模块零引用 + 制造领料写 WIP 不写项目 + 分包零生产 writer）；分类机制（PnL 读侧）支持 4 类但生产仅写入 2 类。
- **UC-PRJ-09**：closeProject 任务校验 ✅（resolved P1-MA2-067）+ 历史保留 ✅（useLogicalDelete）+ 工时路径 ON_HOLD 拒绝 ✅；**但 ON_HOLD/resumeProject 迁移零测试覆盖 + 费用路径状态门控缺失**（ON_HOLD/COMPLETED/CANCELLED 项目报销行仍被归集）。

---

## 5. 五级追踪矩阵 + 符合性结论（§1 矩阵 + §2 判据）

### UC-PRJ-01 项目立项

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① 项目: DRAFT → OPEN(立项) | `use-cases.md:21` | `ErpPrjProjectBizModel.startProject:89-102` + `validateStartPreconditions:136-165`（前置校验 STRICT/WARN）+ DRAFT→OPEN 守卫 `:93-98` | `TestErpPrjProjectPrecheck` 3 @Test 强（STRICT 缺字段抛 / 全字段通过 / WARN 放行） | 行为已证实（A2.13 §2.1 + R1.21 resolved P1-MA2-070 part） | **接受**（迁移 + 前置校验全实现） |
| ② OPEN 后: 允许新单据(工时/采购/领料/报销)标注该项目归集成本 | `use-cases.md:22` | 工时路径 `ErpPrjTimesheetSubmitProcessor.validateProjectReferenceable:65-78` 拒绝非 OPEN ✅；**采购/领料/报销 跨域消费方缺失**（MATERIAL 归集 P1-RC-049）+ 费用路径不校验状态（P1-RC-050） | `TestErpPrjTimesheetCost` COMPLETED 拒 `:143-161` 强；**采购/领料/报销 状态校验零测试** | 工时 ✅；采购/领料归集缺失（P1-RC-049）；报销不校验状态（P1-RC-050） | **接受 on 工时子维度**；采购/领料/报销子维度 → P1-RC-049/P1-RC-050 |
| ③ DRAFT 状态: 不允许成本归集 | `use-cases.md:23` | 工时内联 `validateProjectReferenceable` 拒非 OPEN（含 DRAFT）✅；`requireReferenceable:62-73` DRAFT 全拒但**零调用方** | `TestErpPrjBudgetAndCollection` 拒非 OPEN 强 | 工时 ✅；费用路径 `ExpenseCostAggregator` 不校验状态（P1-RC-050 同根因） | **接受 on 工时子维度**；requireReferenceable 单一咽喉未被使用 → **P2-RC-048** |

**UC-PRJ-01 整体裁决：接受 on 主断言（DRAFT→OPEN + 立项前置）+ P2 on requireReferenceable 未被使用**（断言①接受，断言②③接受 on 工时子维度，跨域/费用子维度投影到 P1-RC-049/P1-RC-050，requireReferenceable 单一咽喉未使用 → 新建 **P2-RC-048** watch-only）。

### UC-PRJ-02 工时提交触发人工成本凭证

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① 工时提交(项目=OPEN, 任务, 时长) | `use-cases.md:36` | `ErpPrjTimesheetSubmitProcessor.submit:34-61`（项目+任务+时长三要素） | `TestErpPrjTimesheetCost` 7 @Test 强 | 行为已证实（A2.13 §1） | **接受** |
| ② 校验 项目.状态 == OPEN(暂停/关闭拒绝) | `use-cases.md:37` | `validateProjectReferenceable:65-78`（非 OPEN 抛 `ERR_TIMESHEET_PROJECT_NOT_OPEN`）+ 任务状态校验 `:80-98`（TODO/IN_PROGRESS 接受） | `TestErpPrjTimesheetCost` COMPLETED 拒 `:143-161` + BLOCKED 任务拒 `:164-182` 强 | 行为已证实 | **接受** |
| ③ 成本率解析(优先级: 用户费率 > 角色费率 > 活动类型费率) | `use-cases.md:38` | `CostRateResolver.resolve:40-67`（tier1 timesheet.costRate > tier2 activityType.costRate > tier3 全局 config；**用户级/角色级缺失**） | tier1/tier2 测试存在；**用户级/角色级零测试** | 三级降级实现，用户/角色层缺失 | **P1** → **P1-RC-048** |
| ④ 人工成本 = 时长 × 成本率 | `use-cases.md:39` | `CostRateResolver.computeCostAmount:82-87`（hours × costRate）→ `submit:51-53` 调用 `.setScale(4, HALF_UP)` | `TestErpPrjTimesheetCost` approve 数值 8000 精确 | ✅ | **接受** |
| ⑤ 发布过账事件(businessType=PROJECT_LABOR_COST) / 生成凭证: 借 项目成本(人工), 贷 应付职工薪酬/劳务成本 | `use-cases.md:40-41` | `TimesheetPostingDispatcher:125 PROJECT_COST_COLLECTION`（**非 L1 字面 PROJECT_LABOR_COST**，owner doc §八:302-304 documented）+ `buildEvent:110-122`（Dr 从 projectType.defaultSubjectId 默认 5101 / Cr 从 config `erp-prj.default-payroll-subject-id` 默认 2211）+ `ProjectCostCollectionProvider:67,71` projectId 维度 | `TestErpPrjTimesheetCost#testApprovePostsProjectCostCollectionVoucher:185-225` 强（Dr5101/Cr2211=8000 + projectId 维度精确）+ E2E `projects-timesheet-posting` 强值 | **行为完全满足**（Dr 5101/Cr 2211 + projectId 维度）；businessType 命名 drift | **接受**（行为满足；businessType drift → §9 登记） |
| ⑥ 工时.已过账 == true | `use-cases.md:42` | `tryPost:60-74`（异常返回 false）→ `approve:42-52`（**仅 tryPost 返回 true 时**设 posted=true `:47-51`，失败派发告警 `:77-94`） | `TestErpPrjTimesheetCost` 强 + `TestTimesheetPostingFailureAlert` 强 | R1.16 resolved P1-MA2-068 | **接受** |

**UC-PRJ-02 整体裁决：P1**（取最高）。**接受 on 主断言①②④⑤⑥**（前置 + 状态校验 + 算术 + 凭证 + posted 全实现强测）；**P1 on ③**（成本率三级降级）。**§4 三判据复核 P1-RC-048** 见下。**businessType drift 接受 + §9 登记**（不降级，行为满足）。

### UC-PRJ-03 多来源成本归集

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① 采购订单行.项目 == P → 入库时成本归集到 P(物料类) | `use-cases.md:55` | **inventory 模块零 ErpPrjCostCollection/ErpPrjProject/projectId/PROJECT_COST 引用**；`InvPostingDispatcher.resolveBusinessType:152-178` 仅 PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT，**从不写项目成本**；采购订单行 `project` FK（`_ErpPurOrderLine.java:1477-1481`）**无聚合器消费** | **零生产测试** | 完全缺失 | **P1** → **P1-RC-049** |
| ② 领料单.项目 == P → 归集(物料) | `use-cases.md:56` | "领料"本仓为制造专用（`ErpMfgMaterialIssue`/MFG_ISSUE），`ManufacturingIssueAcctDocProvider:21-32` 写 Dr WIP `1411`/Cr Inventory `1401`，**从不写项目成本** | **零生产测试** | 完全缺失 | **P1** → **P1-RC-049**（同上） |
| ③ 费用报销.项目 == P → 归集(费用) | `use-cases.md:57` | `ExpenseCostAggregator.refreshExpenseCost:56-126`（`IErpFinExpenseClaimBiz` Facade 只读 + `daoFor(ErpFinExpenseClaimLine)` 跨域只读；幂等 `existsLine:171-177`；config-gated，closeProject 前 `ErpPrjProjectCloseProjectProcessor:63-65` 触发） | `TestErpPrjExpenseAggregation` 4 @Test 强（归集 + 幂等 + config-gated + closeProject 前刷新） | ✅ | **接受**（状态门控缺失投影到 P1-RC-050，不同控制点） |
| ④ 各来源按成本分类(人工/物料/费用/分包)汇总到 ProjectPnl | `use-cases.md:58` | `ProjectPnlCalculator.sumCostByCategory:163-194`（4 类全支持读侧：LABOR/MATERIAL/EXPENSE/SUBCONTRACT + 未知归 EXPENSE）；**生产 writer 仅 2/4**（LABOR `ProjectCostAggregator:59` + EXPENSE `ExpenseCostAggregator:189`；MATERIAL/SUBCONTRACT 零生产 writer） | `TestErpPrjProjectPnl` 4 @Test 强（**手工 seed MATERIAL/SUBCONTRACT 行**证明读侧）+ `TestErpPrjProjectSettlement`（手工 seed MATERIAL） | 分类机制支持 4 类，生产仅写入 2 类（与 ①② 同根因） | **P1** → **P1-RC-049**（同上） |

**UC-PRJ-03 整体裁决：P1**（取最高）。**接受 on ③ EXPENSE 归集**；**P1 on ①②④**（MATERIAL 采购入库 + 领料 + SUBCONTRACT 三类归集来源完全缺失 + 四分类仅 2/4 生产 writer）。**§4 三判据复核 P1-RC-049** 见下。

### UC-PRJ-09 项目暂停/关闭约束

| 验收标准 | L1（逐字） | L3（代码） | L4（测试） | L5（行为） | 符合性 |
|---------|-----------|-----------|-----------|-----------|--------|
| ① OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销) | `use-cases.md:153` | `holdProject:106-108` 迁移 ✅；工时拒绝非 OPEN ✅（`validateProjectReferenceable:65-78`）；**费用路径 `ExpenseCostAggregator.refreshExpenseCost:60-64` 仅查 project==null，从不校验状态** | 工时拒绝非 OPEN 测试强；**ON_HOLD 费用拒绝零测试** | 工时 ✅；**费用路径状态门控缺失** | **P1** → **P1-RC-050** |
| ② ON_HOLD → OPEN(恢复): 恢复归集 | `use-cases.md:154` | `resumeProject:112-114` 迁移 ✅ | **零单测/E2E 覆盖**（ON_HOLD/resumeProject 迁移零行为测试） | 迁移实现存在，行为测试缺失 | **P2**（接受 on 迁移实现；测试覆盖弱，归入 P1-RC-050 描述） |
| ③ → COMPLETED/CANCELLED(关闭): 冻结,不可再归集 | `use-cases.md:155` | `closeProject:83-85`（+ `validateTasksFinished:80-95` resolved P1-MA2-067）+ `cancelProject:118-130`（多源 resolved P1-MA2-070）+ requireReferenceable COMPLETED/CANCELLED 全拒但**零调用方** | `TestErpPrjProjectPrecheck` closeProject 6 @Test 强 + `TestErpPrjBudgetAndCollection` closeProject 冻结拒新工时 强 | 行为已证实（A2.13 §2.1 + R1.21 resolved P1-MA2-067/070） | **接受** |
| ④ 保留审计 | `use-cases.md:155` | closeProject/cancelProject 仅 setStatus + updateEntity，不删除归集行/工时/成本头 | （隐式覆盖） | ORM `useLogicalDelete=true` | **接受** |
| ⑤ 关闭后历史成本/收入数据保留 | `use-cases.md:156` | 同 ④（useLogicalDelete 全状态机实体） | `TestErpPrjBudgetAndCollection` closeProject 后归集保留 强 | ✅ | **接受** |

**UC-PRJ-09 整体裁决：P1**（取最高）。**接受 on ③④⑤**（closeProject 任务校验 + 冻结 + 历史保留，resolved P1-MA2-067/070）；**P1 on ①**（ON_HOLD 费用路径状态门控缺失）；**P2 on ②**（resume 迁移实现存在但零测试覆盖，归入 P1-RC-050 描述）。**§4 三判据复核 P1-RC-050** 见下。

### 矩阵结论汇总

| UC | 整体裁决 | 主 finding |
|----|---------|-----------|
| UC-PRJ-01 | **接受 on 主断言 + P2** | P2-RC-048（requireReferenceable 单一咽喉未被使用 watch-only） |
| UC-PRJ-02 | **P1**（接受 on 主断言①②④⑤⑥） | P1-RC-048（成本率三级降级）+ businessType drift 接受 + §9 登记 |
| UC-PRJ-03 | **P1**（接受 on ③ EXPENSE） | P1-RC-049（MATERIAL 采购入库 + 领料 + SUBCONTRACT 三类归集缺失 + 四分类仅 2/4 生产 writer） |
| UC-PRJ-09 | **P1**（接受 on ③④⑤） | P1-RC-050（ON_HOLD 费用路径状态门控缺失）+ resolved P1-MA2-067/070（closeProject 任务校验 + cancelProject 多源） |

**零 P0**。**3 新 P1 + 1 新 P2 + 4 复用 resolved finding + 1 接受 drift**。

### §4 三判据复核（P1 项强制）

| P1 候选 | (i) plan-audit | (ii) owner doc documented simplification | (iii) product-scope 裁剪 | 裁决 |
|---------|---------------|------------------------------------------|------------------------|------|
| **P1-RC-048**（UC-PRJ-02 成本率三级降级） | ❌（本切片候选偏差未经独立 plan-audit 裁决为简化；`CostRateResolver` Javadoc `:22-26` 为执行者自标 Non-Goal，非独立审查） | ❌（owner doc `cost-collection.md §2.2:58-61` 显式声明 Non-Goal「本期以『单填 > 活动类型 > 默认』实现；用户级/角色级独立费率载体本期不存在，为 Non-Goal；待多级费率配置需求落地时新增用户费率实体（successor）」存在，但 git log `cost-collection.md` 全为 AI commits `docs:` / `docs(audit-remediation):` 无人工批准痕迹——methodology §4 line 168 明确「AI 自写标注**不算**人工批准」） | ❌（`product-scope.md` grep `费率\|cost.?rate\|用户级\|角色级` 零命中——成本率层级未列入范围裁剪） | **P1 强制实现**（Q4=(a) 三判据均不成立；对齐 A1.24 UC-AST-03 / A1.8 UC-MFG-05 先例——AI 自标 Non-Goal ≠ 人工批准）。**非 P0**（不破坏活跃数据/会计过账正确性/核心循环——成本率层级降级影响人工成本归集精度但 Dr/Cr 平衡 + projectId 维度正确 + 主路径成本率存在）。修复 = ORM 加用户/角色费率列或新增 `ErpPrjUserCostRate`/`ErpPrjRoleCostRate` 实体 + `CostRateResolver` 增 tier；**触及 ORM 结构变更须 ask-first + 独立 plan-audit（§5）** |
| **P1-RC-049**（UC-PRJ-03 物料 + 领料 + 分包归集缺失） | ❌（未经独立 plan-audit；owner doc §四为 AI 自标 Non-Goal） | ⚠ **部分满足但非人工批准**：owner doc `cost-collection.md §4.1:171` 显式声明「采购入库/领料归集为本期 Non-Goal（successor）」存在，但 git log 同 P1-RC-048 全为 AI commits 无人工批准痕迹——methodology §4 line 168 明确「AI 自写标注**不算**人工批准」；**SUBCONTRACT 来源 owner doc 未显式 Deferred**（仅常量定义 + PnL 读侧） | ❌（`product-scope.md` grep `物料归集\|领料\|分包\|subcontract\|material.?collection` 零命中——多来源归集未列入范围裁剪；line 7-48 仅列「成本归集」为域能力未裁剪四分类） | **P1 强制实现**（Q4=(a)：MATERIAL 物料归集 owner doc Deferred 无人工批准 → 重开 P1；SUBCONTRACT 完全无 owner doc Deferred → 直接 P1）。**非 P0**（不破坏库存守恒——物料归集是"叠加维度"非库存移动；不破坏 GL 平衡——缺归集行不影响既有凭证；非核心循环断裂——projects PnL 物料类成本数据缺失但不阻断主路径）。修复 = inventory 侧 `InvPostingDispatcher` 接 `IErpPrjCostCollectionBiz` Facade 写物料归集（采购入库）+ projects 侧 `MaterialIssueCostAggregator` 新增（领料）+ `SubcontractCostAggregator` 新增（分包）；**触及跨域契约（inventory→projects 写）须 ask-first + 独立 plan-audit（§5）** |
| **P1-RC-050**（UC-PRJ-09 ON_HOLD 费用路径状态门控缺失） | ❌（未经独立 plan-audit） | ❌（owner doc `state-machine.md §1:15` ON_HOLD 明确「可被新单据引用 = 否（新费用不归集）」+ `§4:53`「暂停后仍有费用流入→配置控制：暂停项目拒绝新费用归集」**未声明 Deferred**，是明确需求契约） | ❌（`product-scope.md` 未将 ON_HOLD 费用门控裁剪） | **P1 强制实现**（Q4=(a) 三判据均不成立，L1+L2 均要求）。**非 P0**（不破坏主路径——ON_HOLD 项目报销行被归集是"过度归集"非"丢失数据"，业财影响：项目成本虚增 + PnL 失真，但 GL 借贷平衡 + 不影响工时归集主路径）。修复 = `ExpenseCostAggregator.refreshExpenseCost:60-64` 增 `validateProjectReferenceable(projectId)` 状态守卫（仅 OPEN 通过，ON_HOLD/COMPLETED/CANCELLED 拒绝）；**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first** |

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

按 §7 规则，本报告产出 finding 前已 grep `arm-index.md` 同域（projects）同控制点（cost-collection / cost-rate / material / subcontract / onHold-expense / requireReferenceable / PROJECT_LABOR_COST），裁决如下：

### 6.1 复用既有 finding（追加 RC A1.34 交叉引用，不新建）

| Finding ID | 报告 | 复用理由 |
|-----------|------|---------|
| **P1-MA2-067** | `2026-07-28-1020-arm-ma2-projects-state-machine.md` | UC-PRJ-09 AC-③ closeProject 任务结束校验**同根因同控制点**（closeProject OPEN→COMPLETED 未强制任务已结束）；R1.21 genuinely RESOLVED via implementation（`ErpPrjProjectCloseProjectProcessor.validateTasksFinished:80-95` HEAD 复核确认）。**追加 RC A1.34 交叉引用注记**：本切片需求契约视角复核 UC-PRJ-09 AC-③ 任务结束校验**确认 resolved**，无升级 |
| **P1-MA2-068** | `2026-07-28-1020-arm-ma2-projects-state-machine.md` | UC-PRJ-02 AC-⑥ posted 回写 + 失败告警**同根因同控制点**（TimesheetPostingDispatcher tryPost 吞异常悬挂）；R1.16 genuinely RESOLVED via implementation（`approve:42-52` 条件 posted + `dispatchFailureAlert:77-94` HEAD 复核确认 + `TestTimesheetPostingFailureAlert` 强断言）。**追加 RC A1.34 交叉引用注记**：本切片需求契约视角复核 UC-PRJ-02 AC-⑥ posted 回写**确认 resolved**，无升级 |
| **P1-MA2-069** | `2026-07-28-1020-arm-ma2-projects-state-machine.md` | UC-PRJ-03 AC-④ 四分类读侧 PnL `ProjectPnlCalculator.sumCostByCategory` 支持 4 类**同根因同控制点**（Milestone/Billing/CostCollection doc-status 字典语义复用偏移）；R1.21 resolved-via-deferral（owner doc Deferred 段落 + APPROVED drift documented）。**追加 RC A1.34 交叉引用注记**：本切片**不重开**（§去重协议——audit-remediation MA2 已裁决方案 B Deferred），只补需求契约维度差异（**物料/分包归集缺失需求契约维度 → P1-RC-049**，不同审计轴互补不重复） |
| **P1-MA2-070** | `2026-07-28-1020-arm-ma2-projects-state-machine.md` | UC-PRJ-01 AC-① 立项前置校验**同根因同控制点**（startProject 缺前置 + cancelProject 多源）；R1.21 genuinely RESOLVED via implementation（`validateStartPreconditions:136-165` HEAD 复核确认 + cancelProject 多源 owner doc state-machine.md `:36-38` 补充）。**追加 RC A1.34 交叉引用注记**：本切片需求契约视角复核 UC-PRJ-01 AC-① 立项前置**确认 resolved**，无升级 |
| **P1-MA1-010** | `2026-07-2*-arm-ma1-*` | UC-PRJ-02 多币种四件套 propId 缺失（状态机 + 需求契约角度均无升级——工时过账 buildEvent 硬编码 exchangeRate=ONE 单币种路径）；**维持 todo MR1** |
| **P1-MA1-022** | `2026-07-2*-arm-ma1-*` | UC-PRJ-02/03 跨域只读 daoFor（projects→finance ErpMdSubject + ErpFinExpenseClaimLine）；**维持 todo MR1**（A2.13 已交接 A4.8/A4.15） |
| **P2-MA2-065** | `2026-07-28-1020-arm-ma2-projects-state-machine.md` | state-machine.md 缺 5 状态承载实体独立章节 watch-only；本切片不投影（与本切片 4 UC 无控制点重叠） |
| **P2-MA2-066** | 同上 | state-machine.md §7 IErpFinAcctDocProvider vs 实现 IErpFinVoucherBiz 文字漂移 watch-only；本切片不投影 |

### 6.2 新建 finding（与既有 arm-index 无同根因同控制点）

| Finding ID | 报告 | 域 | UC | 描述 | 分级判据 | 目标 MR | 修复状态 |
|-----------|------|---|----|------|---------|--------|---------|
| **P1-RC-048** | rc-ma1-a1-34-projects-f1-startup-cost-collection | projects | UC-PRJ-02 AC-③ | **工时成本率三级降级（用户级/角色级费率载体缺失）**：L1（`use-cases.md:38`）逐字「成本率解析(优先级: 用户费率 > 角色费率 > 活动类型费率)」要求**三级**优先级。L3 实仓 `CostRateResolver.resolve:40-67` 实为「单填(timesheet.costRate) > 活动类型(activityType.costRate) > 全局 config(erp-prj.default-labor-cost-rate)」**三级但层级错位**——用户级/角色级独立费率载体未实现（ORM `ErpPrjProjectUser.role` 为纯文本无费率列），实际层级是"单填>活动类型>默认"。owner doc `cost-collection.md §2.2:58-61` 显式 Non-Goal 标注，但 git log 显示全为 AI commits 无人工批准痕迹。**§4 三判据均不成立**（(i) 无独立 plan-audit + (ii) owner doc Deferred 无人工批准痕迹[AI 自标 ≠ 人工批准，methodology §4 line 168] + (iii) product-scope 未裁剪）→ Q4=(a) 强制实现。**新根因**（既有 arm-index 全分区 grep `cost.?rate\|费率\|user.?rate\|role.?rate` 无 RC finding 涉及成本率层级缺失；与 P1-MA1-010 多币种 propId 不同维度/控制点）。**非 P0**（不破坏活跃数据/会计过账正确性/核心循环——成本率降级影响人工成本精度但 Dr/Cr 平衡 + projectId 维度正确 + 主路径成本率存在）。**对齐先例**：A1.24 UC-AST-03 IDLE Deferred + A1.8 UC-MFG-05 物料预留 Deferred 同型（AI 自标 Non-Goal 经 §4 三判据复核倾向重开 P1）。 | §2 P1①（功能实质偏离验收标准——成本率三级层级降级）+ §2 P1⑤（验收标准零断言——用户级/角色级费率零测试） | MR1（R1.0 展开为 RC-R1.n） | todo（本审计仅登记，不实施修复；修复 = ORM 加用户/角色费率列（如 `ErpPrjProjectUser.costRate` + `ErpPrjRole.costRate`）或新增 `ErpPrjUserCostRate`/`ErpPrjRoleCostRate` 实体 + `CostRateResolver` 增 tier 用户级/角色级，**触及 ORM 结构变更须 ask-first + 独立 plan-audit（§5）**） |
| **P1-RC-049** | rc-ma1-a1-34-projects-f1-startup-cost-collection | projects | UC-PRJ-03 AC-①②④ | **MATERIAL（采购入库→项目）+ MATERIAL（领料→项目）+ SUBCONTRACT 三类归集来源完全缺失 + 四分类仅 2/4 生产 writer**：L1（`use-cases.md:55-58`）逐字「采购订单行.项目 == P → 入库时成本归集到 P(物料类) / 领料单.项目 == P → 归集(物料) / 各来源按成本分类(人工/物料/费用/分包)汇总到 ProjectPnl」要求**四分类**（人工/物料/费用/分包）。L3 实仓：(a) **MATERIAL（采购入库→项目）❌**：inventory 模块对 `ErpPrjCostCollection/ErpPrjProject/projectId/PROJECT_COST/project` **零引用**（grep 实证），`InvPostingDispatcher.resolveBusinessType:152-178` 仅 emit PURCHASE_INPUT/SALES_OUTPUT/MANUFACTURING_RECEIPT 从不写项目成本，采购订单行 `project` FK（`_ErpPurOrderLine.java:1477-1481`）无聚合器消费；(b) **MATERIAL（领料→项目）❌**："领料"在本仓为制造专用（`ErpMfgMaterialIssue`/MFG_ISSUE），`ManufacturingIssueAcctDocProvider:21-32` 写 Dr WIP `1411`/Cr Inventory `1401` 从不写项目成本；(c) **SUBCONTRACT ❌**：常量 `ErpPrjConstants.java:92 COST_CATEGORY_SUBCONTRACT` 定义 + PnL 读侧 `ProjectPnlCalculator.java:187` 但**生产代码零 `setCostCategory(COST_CATEGORY_SUBCONTRACT)` writer**（仅测试手工 seed）。分类机制（`ProjectPnlCalculator.sumCostByCategory:163-194` 读侧）支持 4 类但生产仅写入 2 类（LABOR/EXPENSE）。owner doc `cost-collection.md §4.1:171` 显式声明「采购入库/领料归集为本期 Non-Goal（successor）」，分包来源未声明；git log 全为 AI commits 无人工批准痕迹。**§4 三判据**：MATERIAL 物料归集 owner doc Deferred 无人工批准 → 重开 P1；SUBCONTRACT 完全无 owner doc Deferred → 直接 P1。**新根因**（既有 arm-index 全分区 grep `material.?collection\|领料\|subcontract\|requisition` 无 RC finding 涉及 projects 物料/分包归集；与 P1-MA2-069 dict drift 不同维度/控制点）。**非 P0**（不破坏库存守恒——物料归集是"叠加维度"非库存移动；不破坏 GL 平衡——缺归集行不影响既有凭证；非核心循环断裂——projects PnL 物料类成本数据缺失但不阻断主路径）。 | §2 P1①（功能完全缺失——MATERIAL + SUBCONTRACT 三类归集来源零实现）+ §2 P1⑤（验收标准零断言——物料/分包生产路径零测试） | MR1（R1.0 展开为 RC-R1.n） | todo（本审计仅登记，不实施修复；修复 = inventory 侧 `InvPostingDispatcher` 接 `IErpPrjCostCollectionBiz` Facade 写物料归集（采购入库）+ projects 侧 `MaterialIssueCostAggregator` 新增（领料）+ `SubcontractCostAggregator` 新增（分包），**触及跨域契约（inventory→projects 写）+ 可能触及 ORM（采购订单行 project FK 已存在但消费方需新增）须 ask-first + 独立 plan-audit（§5）**） |
| **P1-RC-050** | rc-ma1-a1-34-projects-f1-startup-cost-collection | projects | UC-PRJ-09 AC-① | **ON_HOLD/COMPLETED/CANCELLED 费用报销归集路径状态门控缺失（违反"拒绝新费用归集"）**：L1（`use-cases.md:153`）逐字「OPEN → ON_HOLD(暂停): 拒绝新费用归集(工时/采购/报销)」。L3 实仓工时路径 `ErpPrjTimesheetSubmitProcessor.validateProjectReferenceable:65-78` 拒绝非 OPEN ✅，但**费用路径 `ExpenseCostAggregator.refreshExpenseCost:60-64` 仅查 `project==null`（缺失抛 `ERR_PROJECT_NOT_REFERENCEABLE`），从不校验项目状态**——ON_HOLD/COMPLETED/CANCELLED 项目的报销行仍被归集，违反 UC-PRJ-09 AC-①"拒绝新费用归集(工时/采购/报销)"。owner doc `state-machine.md §1:15` ON_HOLD 明确「可被新单据引用 = 否（新费用不归集）」+ `§4:53`「暂停后仍有费用流入→配置控制：暂停项目拒绝新费用归集」**未声明 Deferred**，是明确需求契约。**§4 三判据均不成立**（(i) 无独立 plan-audit + (ii) owner doc 未声明 Deferred + (iii) product-scope 未裁剪）→ Q4=(a) 强制实现。**新根因**（既有 arm-index 全分区 grep `onHold\|expense.?aggregator\|requireReferenceable` 无 RC finding 涉及 ExpenseCostAggregator 状态门控；与 P2-RC-048 requireReferenceable 单一咽喉未使用不同控制点[API 未被使用 vs API 被使用但状态门控缺失]）。**非 P0**（不破坏主路径——ON_HOLD 项目报销行被归集是"过度归集"非"丢失数据"，业财影响：项目成本虚增 + PnL 失真，但 GL 借贷平衡 + 不影响工时归集主路径；非活跃数据破坏——已归集行经幂等去重可手工清理）。 | §2 P1①（功能实质偏离验收标准——ON_HOLD 费用门控缺失）+ §2 P1②（异常路径未实现——ON_HOLD 项目应拒绝新费用归集未实现） | MR1（R1.0 展开为 RC-R1.n） | todo（本审计仅登记，不实施修复；修复 = `ExpenseCostAggregator.refreshExpenseCost:60-64` 增 `validateProjectReferenceable(projectId)` 状态守卫（仅 OPEN 通过，ON_HOLD/COMPLETED/CANCELLED 拒绝抛 `ERR_PROJECT_NOT_REFERENCEABLE`，与 requireReferenceable 单一咽喉语义一致）；**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**；建议补 ON_HOLD/COMPLETED/CANCELLED 项目报销行归集拒绝负向测试） |
| **P2-RC-048** | rc-ma1-a1-34-projects-f1-startup-cost-collection | projects | UC-PRJ-01 AC-②③ | **requireReferenceable 单一咽喉未被使用（API 存在生产代码零调用方）**：L1 `use-cases.md:22-23` 隐含统一门控语义（"OPEN 后允许新单据标注该项目归集成本 / DRAFT 状态: 不允许成本归集"）；L2 owner doc `state-machine.md §7:76` 显式「采购/销售/费用单据引用项目→通过 projectId 字段标注，项目关闭后拒绝引用」。L3 实仓 `ErpPrjProjectBizModel.requireReferenceable:62-73` 仅 OPEN 通过（DRAFT/ON_HOLD/COMPLETED/CANCELLED 全拒 `ERR_PROJECT_NOT_REFERENCEABLE`）实现正确，**但全 module-projects/inventory/purchase/sales 生产代码 grep 零调用方**——工时路径内联自有 `ErpPrjTimesheetSubmitProcessor.validateProjectReferenceable:65-78`，费用路径完全不校验状态（P1-RC-050）。**双门控承诺未落地**（单一咽喉 API 存在但未被消费；工时主路径经内联自有校验 OK，跨域/费用门控弱）。**新根因**（既有 arm-index 全分区 grep `requireReferenceable` 无 RC finding 涉及此控制点；与 P1-RC-050 ExpenseCostAggregator 状态门控缺失不同控制点[API 未被使用 vs API 被使用但门控不全]，互补不重复）。**主路径[工时]OK 边界[跨域/费用门控]弱**。 | §2 P2①（次要验收标准未完全满足——主路径[工时内联校验]OK 边界[requireReferenceable 单一咽喉未使用]弱）+ §2 P2②（可用性/可观测性——API 存在但无运维文档说明使用约定） | successor watch-only（P2 登记不强制） | todo（修复 = a) 工时路径 `ErpPrjTimesheetSubmitProcessor.validateProjectReferenceable:65-78` 改调 `IErpPrjProjectBiz.requireReferenceable` Facade（消除内联重复，统一咽喉）；b) 费用路径 `ExpenseCostAggregator.refreshExpenseCost:60-64` 改调 `IErpPrjProjectBiz.requireReferenceable` Facade（与 P1-RC-050 修复协同——P1-RC-050 修复时若改调 requireReferenceable 自动闭合本 finding）；c) 跨域 inventory 侧物料归集实现时（P1-RC-049 修复）同样调 requireReferenceable Facade。**纯 BizModel 代码逻辑修复，按 roadmap 预授权类目可自动执行，不触发 §5 ask-first**；与 P1-RC-050 修复协同可一次性闭合） |

### 6.3 businessType drift 裁决（接受 + §9 登记，不新建 finding）

| Drift 点 | L1 字面 | L3 实现 | 裁决 |
|---------|---------|---------|------|
| businessType `PROJECT_LABOR_COST` | `use-cases.md:40`「发布过账事件(businessType=PROJECT_LABOR_COST)」 | `TimesheetPostingDispatcher:125 event.setBusinessType(ErpFinBusinessType.PROJECT_COST_COLLECTION)`（枚举 `ErpFinBusinessType.java:24 PROJECT_COST_COLLECTION(110)`） | **接受 + §9 真相源 drift 登记**：owner doc `cost-collection.md §八:302-304` 显式 documented「设计原文写作 `PROJECT_LABOR_COST` 为命名偏差，实际复用既有 `ErpFinBusinessType.PROJECT_COST_COLLECTION(110)` 枚举（保持『不新增 finance 契约』边界）」+ `use-cases.md:198` 同步注记。**行为完全满足**（凭证 Dr 5101 项目成本 / Cr 2211 应付职工薪酬 + projectId 维度标注）。L1 字面常量为命名偏差，按 §9 冻结条款分歧记入报告不直改真相源。**不新建 finding，不复核不降级**（行为满足验收标准「发布过账事件 + 生成凭证」） |

### 6.4 finding → 修复追踪

| Finding | 目标 MR | 触及保护区域 | 修复状态 |
|---------|--------|-------------|---------|
| **P1-MA2-067**（reuse，R1.21 resolved） | MR1（resolved） | N/A（已 resolved） | resolved（genuinely RESOLVED via implementation，本切片 HEAD 复核确认） |
| **P1-MA2-068**（reuse，R1.16 resolved） | MR1（resolved） | N/A（已 resolved） | resolved（genuinely RESOLVED via implementation，本切片 HEAD 复核确认） |
| **P1-MA2-069**（reuse，R1.21 resolved-via-deferral） | MR1（resolved-via-deferral） | N/A | resolved-via-deferral（**本切片不重开**[§去重协议]，与 P1-RC-049 需求契约维度互补不重复） |
| **P1-MA2-070**（reuse，R1.21 resolved） | MR1（resolved） | N/A（已 resolved） | resolved（genuinely RESOLVED via implementation，本切片 HEAD 复核确认） |
| **P1-MA1-010**（reuse，todo MR1） | MR1 | 是——ORM 结构变更（多币种四件套 propId） | todo（多币种维度，本切片仅引用不重审） |
| **P1-MA1-022**（reuse，todo MR1） | MR1 | 否（跨域只读 daoFor 读侧统一裁决登记于 `data-dependency-matrix.md §9`） | todo（跨域只读，本切片仅引用不重审） |
| **P1-RC-048**（新建） | MR1（R1.0 → RC-R1.n） | **是——ORM 结构变更**（用户/角色费率载体：`ErpPrjProjectUser.costRate` 字段或新增 `ErpPrjUserCostRate`/`ErpPrjRoleCostRate` 实体） | todo（**先须人工确认 product-scope 是否裁剪成本率层级**；若裁剪→§4 (iii) 改真相源；若未裁剪→P1 强制实现触及 ORM 须 ask-first + 独立 plan-audit） |
| **P1-RC-049**（新建） | MR1（R1.0 → RC-R1.n） | **是——跨域契约 + 可能 ORM 结构变更**（inventory→projects 写物料归集 + 采购订单行 project FK 消费方新增） | todo（**先须人工确认 product-scope 是否裁剪物料/分包归集**；若裁剪→§4 (iii) 改真相源；若未裁剪→P1 强制实现触及跨域契约须 ask-first + 独立 plan-audit） |
| **P1-RC-050**（新建） | MR1（R1.0 → RC-R1.n） | 否（纯 BizModel 代码逻辑——`ExpenseCostAggregator.refreshExpenseCost:60-64` 增 `validateProjectReferenceable` 状态守卫，按 roadmap 预授权类目[代码逻辑修复]可自动执行，**不触发 §5 ask-first**） | todo |
| **P2-RC-048**（新建） | successor watch-only（P2 登记不强制） | 否（纯 BizModel 代码逻辑——工时/费用路径改调 `IErpPrjProjectBiz.requireReferenceable` Facade，与 P1-RC-050 修复协同；按 roadmap 预授权类目可自动执行，**不触发 §5 ask-first**） | todo（watch-only；与 P1-RC-050 修复协同可一次性闭合） |

---

## 7. 静态存疑点清单（供 MA4 展开）

> §1 L5 存疑点：L5 无法静态定论、需运行时确认的点。每存疑点一行。

| # | 存疑点 | 静态判定 | MA4 展开方向 |
|---|--------|---------|-------------|
| SP-1 | `ExpenseCostAggregator.refreshExpenseCost` 在 ON_HOLD/COMPLETED/CANCELLED 项目上归集的实际运行时行为（P1-RC-050 静态判定为"过度归集"——ON_HOLD 项目报销行被归集使项目成本虚增 + PnL 失真，但 GL 借贷平衡不受影响；需运行时确认归集行是否经 closeProject 触发链路反向清理或仅累积） | 静态：L3 `:60-64` 仅查 project==null 不校验状态；closeProject 前 `:63-65` 触发 `refreshExpenseCost`——若项目已 ON_HOLD 时报销行已归集，closeProject 触发的 refresh 是否包含已 ON_HOLD 期间的单据需运行时确认 | A4.1 运行时：构造 ON_HOLD 项目 + 期间已审报销单 → 断言 `refreshExpenseCost` 是否归集该报销行 + closeProject 后 actualCost 是否含该期间金额 |
| SP-2 | `requireReferenceable` 是否被任何 delta/未来定制消费（P2-RC-048 静态判定为"生产代码零调用方"——是否经 xbiz 跨域 GraphQL 调用或前端 AMIS 按钮触发需运行时确认） | 静态：全 module-projects/inventory/purchase/sales 生产代码 grep 零调用方；xbiz `ErpPrjProject.xbiz.xml` 是否暴露 requireReferenceable 为 @BizMutation 可被前端调用需运行时确认 | A4.1 运行时：grep `_vfs/erp/prj/ErpPrjProject.xbiz.xml` + AMIS view.xml 是否含 requireReferenceable mutation 按钮；若有则前端可达但跨域消费方仍缺失 |
| SP-3 | 物料归集经 inventory 配置触发是否部分可达（P1-RC-049 静态判定为"完全缺失"——inventory 模块零引用 ErpPrjCostCollection，但 `InvPostingDispatcher` 是否经 config-gated 路径部分触发项目归集需运行时确认） | 静态：`InvPostingDispatcher.resolveBusinessType:152-178` 全文 grep 无 PROJECT_COST 分支；但 config-gated 路径（如 `erp-prj.material-collection-enabled`）是否存在但默认关闭需运行时确认 | A4.1 运行时：grep `erp-prj.material-collection-enabled\|erp-inv.project-collection` config key + 实跑采购入库（订单行标 projectId）→ 断言 erp_prj_cost_collection 表是否新增物料类行 |
| SP-4 | 多币种 exchangeRate=ONE 在多币种项目的运行时影响（P1-MA1-010 投影）：工时过账 `buildEvent` 硬编码 `exchangeRate=ONE` 单币种路径，多币种项目（如 USD 项目 + CNY 本位币）的实际人工成本折算是否失真 | 静态：`TimesheetPostingDispatcher.buildEvent` exchangeRate=ONE 硬编码 + `ErpPrjCostCollection.exchangeRate` 字段经 `ExpenseCostAggregator:112 setExchangeRate(ONE)` 单币种路径；多币种项目 PnL 折算失真需运行时确认（与 P1-MA1-010 多币种四件套 propId 同根因） | A4.1 运行时：构造 USD 项目（orgId 归属 USD 法人）+ CNY 本位账套 → 工时过账 → 断言凭证 amountFunctional 是否正确折算（复用 P1-MA1-010 运行时确认） |
| SP-5 | `ON_HOLD→OPEN resumeProject` 迁移 + resume 后归集恢复的实际运行时行为（UC-PRJ-09 AC-② 静态判定为"迁移实现存在但零测试覆盖"——resume 后工时/费用归集是否正确恢复 + 期间 ON_HOLD 累积的报销行是否在 resume 后被归集） | 静态：`resumeProject:112-114` 迁移 setStatus(OPEN) ✅；resume 后归集路径需运行时确认（与 SP-1 同根因——若 ON_HOLD 期间归集未暂停，resume 后行为差异不显著） | A4.1 运行时：构造 ON_HOLD→OPEN 迁移 + 期间 + resume 后工时提交 → 断言归集行为是否正确恢复（与 P1-RC-050 修复协同） |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 见下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告无生产代码变更（纯审计报告），checker 无回归风险**。

  | 规则 | 描述 | actual | baseline | 状态 |
  |------|------|--------|----------|------|
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | ✓ ≤ |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | ✓ ≤ |
  | R2b | BizModel daoFor(Erp*) 跨域 | 229 | 240 | ✓ ≤（下降） |
  | R2c | 全生产代码 daoFor() 总量 | 1382 | 1380 | ⚠ +2（pre-existing baseline 漂移，本审计无代码变更不引入） |
  | R2d | Processor/Dispatcher/Engine 中 daoFor(ErpMd*) | 34 | 32 | ⚠ +2（pre-existing baseline 漂移，本审计无代码变更不引入） |
  | R3 | new Erp*() 构造实体 | 5 | 5 | ✓ ≤ |
  | R5 | @Inject private | 0 | 0 | ✓ |
  | R6 | @Transactional in BizModel | 2 | 2 | ✓ |
  | R8 | Processor 无 xbiz 接线 | 0 | 0 | ✓ |
  | R10 | REQUIRES_NEW 事务 | 6 | 6 | ✓ |
  | R11 | Processor 重复状态判断方法 | 0 | 0 | ✓ |

  > R2c actual=1382 vs baseline=1380 + R2d actual=34 vs baseline=32 为 **pre-existing** 基线漂移（本审计为只读审计，零生产代码变更），非本审计引入。CI workflow 门控会捕获此漂移；本报告不处理基线对齐（属 compliance-baseline 维护范畴，非审计范围）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index projects 同域同控制点后给出"复用 or 新增"裁决（§6.1 复用裁决表 + §6.2 新建裁决表），无未经比对直接新建的 finding。P1-RC-048（成本率三级降级）+ P1-RC-049（物料+领料+分包归集缺失）+ P1-RC-050（ON_HOLD 费用门控）+ P2-RC-048（requireReferenceable 单一咽喉未使用）经 grep 确认为 projects 域新控制点；P1-MA2-067/068/069/070（closeProject 任务校验 / tryPost 悬挂 / dict drift / startProject 前置）经 grep 确认同根因同控制点 → 复用并列明差异依据。

---

## Verdict

**pass（零 P0、3 新 P1[P1-RC-048 成本率三级降级 + P1-RC-049 物料+领料+分包归集缺失 + P1-RC-050 ON_HOLD 费用门控] + 1 新 P2[P2-RC-048 requireReferenceable 单一咽喉未使用 watch-only] + 4 复用 resolved finding[P1-MA2-067/068/069/070 全 R1.16/R1.21 resolved HEAD 复核无回退] + 2 复用 todo MR1 finding[P1-MA1-010/022 跨切片] + 1 接受 drift[businessType PROJECT_LABOR_COST vs PROJECT_COST_COLLECTION 行为满足 §9 登记] + 1 UC 接受含 caveat[UC-PRJ-01 立项主断言接受 + requireReferenceable P2] + 1 UC P1[UC-PRJ-02 成本率三级降级] + 1 UC P1[UC-PRJ-03 物料+分包缺失] + 1 UC P1[UC-PRJ-09 ON_HOLD 费用门控]）**。resolved finding HEAD 复核：P1-MA2-067（genuinely RESOLVED via implementation `ErpPrjProjectCloseProjectProcessor.validateTasksFinished:80-95`）/ P1-MA2-068（genuinely RESOLVED via implementation `approve:42-52` 条件 posted + 告警派发）/ P1-MA2-069（resolved-via-deferral，本切片不重开[§去重协议]，与 P1-RC-049 需求契约维度互补）/ P1-MA2-070（genuinely RESOLVED via implementation `validateStartPreconditions:136-165`）。本切片解除 A1.34 在 MA4（A4.1 扩展域展开器 SP-1~SP-5）及 MR1（R1.0）链路的该切片证据缺口。**P0 即时通道未触发**（本切片无 P0——3 P1 均为功能实质偏离/完全缺失/异常路径未实现类，非 §2 P0①②③④ 活跃数据破坏/核心循环断裂/会计过账破坏；projects PnL 物料/分包数据缺失不破坏 GL 平衡或库存守恒）。
