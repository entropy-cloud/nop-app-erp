# 2026-07-28-1020-2-audit-remediation-ma2-projects-state-machine MA2 projects 状态机审查（A2.13）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A2.13 projects 状态机审查（A 级单域，16 状态字段）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.13）
> Related: `docs/plans/2026-07-28-1020-1-audit-remediation-ma2-quality-state-machine.md`（A2.12 quality 状态机同批审查——NCR 过账 + 跨域只读 + tryPost 容错同型范式）；`docs/plans/2026-07-28-0400-3-audit-remediation-ma2-inventory-state-machine.md`（A2.11 inventory 状态机范式——posted 三件套 + 业务单据双轴审批 + tryPost 容错悬挂同型）；`docs/plans/2026-07-03-1018-2-projects-cost-collection.md`（项目成本归集 owner doc §实现偏离补注来源）；`docs/plans/2026-07-07-0930-3-projects-task-dependency-dag-cycle-validation.md`（任务依赖 DAG 成环检测——上行链 + HashSet + maxDepth 算法 + startTask/completeTask/blockTask/unblockTask 状态迁移链）；`docs/plans/2026-07-04-0831-2-hr-payroll-engine-income-tax.md`（工时成本凭证 TimesheetPostingDispatcher 跨域过账同型）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/projects/state-machine.md`（项目 5 态 + 任务 4 态 + §审查提示 + §任务依赖 DAG 校验）+`task-dag.md`+`cost-collection.md`+`profitability.md`（owner doc）
> Audit: required

## Current Baseline

projects（项目）域 A 级状态机审查（单域单工作项，16 状态字段）。projects 是采购/销售/工时/费用归集的**项目辅助核算枢纽**（单据引用 projectId 归集成本），状态机驱动**项目生命周期**（DRAFT→OPEN→ON_HOLD→COMPLETED/CANCELLED）+ **任务执行进度**（TODO→IN_PROGRESS→DONE/BLOCKED + 依赖 DAG）+ **成本归集/工时凭证副作用**（TimesheetPostingDispatcher 跨域过账）。项目状态机的核心不变量：**OPEN 状态才允许费用归集**（owner doc §2 ON_HOLD 暂停费用归集可配置）+ **项目完成需任务已结束**（owner doc §审查提示）+ **任务依赖成环拒绝**（DAG 校验，owner doc §任务依赖规则）+ **项目关闭后拒绝引用**（owner doc §7）。

实时仓库已落地的项目状态机实现（待审查，路径 `module-projects/`）：

- **状态字段清单**（ORM `app-erp-projects.orm.xml`，16 状态字段分布于多类状态对象）：
  - **项目轴**（`ErpPrjProject`）：`status`(erp-prj/project-status DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED)
  - **任务轴**（`ErpPrjTask`）：`status`(erp-prj/task-status TODO/IN_PROGRESS/DONE/BLOCKED) + `dependsOn` 依赖关系
  - **成本归集轴**（`ErpPrjCostCollection`）：`costType`(erp-prj/cost-type) + `approveStatus` + 多币种四件套（exchangeRate/amountSource/amountFunctional — P1-MA1-010 propId 缺失）
  - **开票轴**（`ErpPrjBilling`）：`status` + `approveStatus` + 多币种四件套（amountSource/amountFunctional — P1-MA1-010 propId 缺失）
  - **工时轴**（`ErpPrjTimesheet`）：`status` + `approveStatus`
  - **里程碑轴**：`ErpPrjMilestone` status
- **项目生命周期迁移实现**：DRAFT→OPEN（立项，项目信息完整+起止日期有效+预算已定）→ON_HOLD（暂停，费用归集可配置拦截）/ →COMPLETED（任务已结束或确认剩余不再执行+成本已归集）/ →CANCELLED（已归集成本保留）。ON_HOLD↔OPEN 合法往复（退出条件 COMPLETED/CANCELLED）。owner doc §3 终态不可直接恢复，重启新建项目。
- **任务依赖 DAG 实现**（plan 2026-07-07-0930-3 落地）：任务可有前置依赖（dependsOn）；前置任务未完成时后继任务不可开始（迁移 TODO→IN_PROGRESS 时校验）；依赖成环拒绝（DAG 校验：上行链 + HashSet + maxDepth 算法，见 task-dag.md）。状态迁移完整链：startTask/completeTask/blockTask/unblockTask。
- **成本归集副作用**：采购/销售/费用单据引用 projectId 归集成本；工时提交触发项目成本凭证（经 `IErpFinAcctDocProvider` 注册工时成本 businessType + TimesheetPostingDispatcher 跨域过账）。**ON_HOLD 项目费用归集暂停**（config-gated，owner doc §2）。
- **跨域访问**：TimesheetPostingDispatcher daoFor(ErpMdSubject)（科目解析只读，P1-MA1-022 登记 MR1）+ `ErpPrjReportBizModel` facade read-only。工时成本凭证经 `IErpFinAcctDocProvider` Facade（非 daoFor 直写）。项目采购/销售经 `IErpPurOrderBiz`/`IErpSalOrderBiz`（标注 projectId）。MA1 已确认 purchase/sales→projects 只读 ORM 引用合法（DAG 无环，历史审计 M1 已文档对齐）。
- **多币种**：ErpPrjCostCollection/ErpPrjBilling 多币种四件套部分缺 propId（P1-MA1-010 ORM 规范层）。
- **测试覆盖**：需审查项目状态机相关测试（立项/暂停恢复/完成取消 / 任务依赖 DAG 成环检测 / 工时成本凭证 / ON_HOLD 费用归集拦截等）。

**已登记的直指项目状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-010`（todo MR1，projects）：`ErpPrjCostCollection.{exchangeRate, amountSource, amountFunctional}`/`ErpPrjBilling.{amountSource, amountFunctional}` 共 5 列 propId 缺失。**状态机 scope**：ORM 规范层（多币种四件套补字段），不参与状态机判定——本审计复核多币种成本归集对状态机的影响。
- `P1-MA1-022`（todo MR1，9 域合并含 prj）：`TimesheetPostingDispatcher` daoFor(ErpMdSubject)（科目解析只读）+ `ErpPrjReportBizModel` facade read-only。**状态机 scope**：跨域只读是工时过账副作用，不破坏状态机——本审计复核异常路径无悬挂。

**但从未做过一次覆盖项目全状态机（项目 5 态 + 任务 4 态 + 成本归集/开票/工时/里程碑，16 状态字段）、按 `state-machine-business-review-prompt.md` 10 维度的系统性业务审查**。已知未核验控制点（owner doc §审查提示 + 已登记 finding）：

- **状态定义清晰性**：项目 status(erp-prj/project-status) 5 态清晰性；任务 status(erp-prj/task-status) 4 态清晰性；ON_HOLD（等待恢复决策 vs 做什么）；成本归集/开票/工时 approveStatus 轴；里程碑状态轴清晰性。
- **转换完整性**：项目生命周期迁移完整性（DRAFT→OPEN→ON_HOLD/COMPLETED/CANCELLED + ON_HOLD→OPEN）；**任务依赖 DAG 校验**（TODO→IN_PROGRESS 时校验前置完成，成环拒绝）；**项目完成需任务已结束**（owner doc §审查提示）；**ON_HOLD 费用归集暂停**（config-gated，owner doc §2）；**项目关闭后拒绝引用**（owner doc §7）。是否有非法跳转或缺失条件分支。
- **终端状态与恢复**：COMPLETED/CANCELLED 终态（不可直接恢复，重启新建项目）；已取消项目保留已归集成本不可删除（审计要求）；任务 DONE 终态；终态恢复路径（新建关联）。
- **异常路径**：**完成时仍有未结束任务**（提示先关闭任务或确认剩余取消）；**暂停后仍有费用流入**（配置控制：暂停项目拒绝新费用归集或允许标记）；**预算超支**（警告或拦截按配置，不阻止状态迁移）；并发状态变更（乐观锁）；**项目删除**（草稿可删除；进行中及以后只能取消/完成）；**任务依赖成环**（DAG 拒绝）；**工时成本凭证过账失败悬挂**（TimesheetPostingDispatcher tryPost 容错，与 finance P1-MA2-032/hr P1-MA2-048 同型——升级评估）。
- **可达性**：从 DRAFT 可达 OPEN→ON_HOLD/COMPLETED/CANCELLED；ON_HOLD 可回 OPEN；任务从 TODO 可达 IN_PROGRESS→DONE/BLOCKED→IN_PROGRESS；无不可达状态；无死锁（ON_HOLD↔OPEN 合法往复，退出条件 COMPLETED/CANCELLED）。
- **角色与权限**：立项/完成/取消（项目经理/管理员——影响报表与成本结转）；暂停/恢复（项目经理）；任务 startTask/completeTask/blockTask（任务负责人/项目经理）。
- **外部依赖**：采购/销售/费用单据引用项目（projectId 标注，项目关闭后拒绝引用）；工时触发成本凭证（经 IErpFinAcctDocProvider）；跨域经 I\*Biz Facade（IErpPurOrderBiz/IErpSalOrderBiz）。
- **TODO/任务策略**：DRAFT 产生 assigned（项目经理待立项）；OPEN 产生 monitor（进度监控）；ON_HOLD 产生 assigned（项目经理待决策恢复/取消）；COMPLETED/CANCELLED 否（终态）；任务 IN_PROGRESS 产生 TODO（执行）；BLOCKED 产生 assigned（解除阻塞决策）。**避免项目静默下沉**（owner doc §8）。
- **场景演练**：(a) 研发项目 happy path（DRAFT→OPEN→成本归集→COMPLETED→报表）；(b) **项目暂停与恢复**（OPEN→ON_HOLD→费用归集暂停→恢复）；(c) 项目取消（已归集成本保留）；(d) **任务依赖成环拒绝**（DAG 校验）；(e) **完成时仍有未结束任务**（提示先关闭或确认取消）；(f) **暂停后费用流入**（配置控制）；(g) **预算超支**（警告或拦截）；(h) **工时成本凭证过账**（TimesheetPostingDispatcher 跨域）；(i) **工时成本凭证过账失败悬挂**（tryPost 容错）；(j) **项目关闭后拒绝引用**。
- **与设计文档一致性**：`state-machine.md`/`task-dag.md`/`cost-collection.md`/`profitability.md` vs 实现——重点核验：(1) §2 ON_HOLD 费用归集暂停 config-gated 落实；(2) §任务依赖规则 DAG 成环校验（task-dag.md 上行链+HashSet+maxDepth）；(3) §审查提示 项目完成需任务已结束；(4) §7 项目关闭后拒绝引用；(5) §3 终态不可恢复 + 已取消保留成本；(6) §8 TODO 策略（DRAFT/OPEN/ON_HOLD 避免沉没）；(7) 多币种成本归集 owner doc 一致性。

剩余差距：需要一次系统性状态机业务审查，发现任何遗漏的 P0（**项目完成未强制任务已结束** [若破坏完成前置——owner doc §审查提示] / **任务依赖 DAG 成环校验缺失** [若破坏依赖不变量——owner doc §任务依赖规则] / **ON_HOLD 费用归集未暂停** [若破坏归集控制——owner doc §2] / **项目关闭后仍可被引用** [若破坏引用约束——owner doc §7] / **工时成本凭证过账失败悬挂无告警闭环** [若破坏业财一致——同型升级评估]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **项目 5 态 + 任务 4 态 + 成本归集/开票/工时/里程碑（16 状态字段）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（ON_HOLD 语义 / 里程碑状态轴）；(2) 转换完整性（生命周期迁移 + **任务依赖 DAG 成环校验** / **项目完成需任务已结束** / **ON_HOLD 费用归集暂停** / **项目关闭后拒绝引用**）；(3) 终端与恢复（终态不可恢复 + 已取消保留成本）；(4) 异常路径（**完成时未结束任务** / **暂停后费用流入** / 预算超支 / **任务依赖成环** / **工时凭证过账失败悬挂**）；(5) 可达性；(6) 角色权限（**立项/完成/取消项目经理+管理员**）；(7) 外部依赖（单据引用 projectId + 工时凭证 I\*Biz Facade）；(8) TODO 任务策略（**DRAFT/OPEN/ON_HOLD 避免沉没**）；(9) 场景演练（10 个代表性场景）。
- 复核已登记 finding 在项目状态机运行时的行为影响：P1-MA1-010（多币种四件套 propId——状态机角度复核）/ P1-MA1-022（TimesheetPostingDispatcher daoFor(ErpMdSubject)——异常路径复核），标注终态。
- scope matrix §状态机正确性 prj 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.13 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.6a manufacturing 状态机 — done；本审计只复核工时成本凭证反馈 finance 的跨域交互状态机角度。
- **不**审计 A2.1 P2P / A2.2 O2C 端到端 — done；本审计只复核项目采购/项目销售引用 projectId 的状态机角度（成本归集归 cost-collection.md）。
- **不**审计 A5.x 测试覆盖深度 — 测试覆盖系统性审查归 MA5；本审计只复核工时凭证过账失败悬挂对状态机的影响。
- **不**审计 A2.17 并发与乐观锁 — 并发状态变更归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 A4.x view.xml drift / A4.5 projects 代码质量抽样 — 归 MA4。
- **不**审计 config-gated Deferred 偏离是否应实现（ON_HOLD 费用归集 config / 预算超支 config） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/projects/state-machine.md`（项目 5 态 + 任务 4 态 + §审查提示 — **需复核 ON_HOLD 费用归集 + 项目完成需任务结束 + 项目关闭后拒绝引用**）；`docs/design/projects/task-dag.md`（任务依赖 DAG 成环检测——上行链+HashSet+maxDepth + startTask/completeTask/blockTask/unblockTask）；`docs/design/projects/cost-collection.md`（成本归集+多币种）；`docs/design/projects/profitability.md`（项目成本/利润报表）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）；`docs/architecture/posting-exemptions.md`（工时过账豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.13 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：项目状态机本身非 ask-first 最高级保护区域，但**工时成本凭证触及 finance 凭证链**（TimesheetPostingDispatcher 跨域过账）+ **项目辅助核算影响成本/利润报表**（业主财一体面）。P0 即时修复若触及 `TimesheetPostingDispatcher`/`ErpPrj*Processor`/`ErpPrjTaskBizModel`（DAG 校验）/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计保护区域）。ORM 字典变更（project-status/task-status）属 ask-first。xbiz 文件变更属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - projects 状态机系统性业务审查

Status: planned
Targets: `module-projects/erp-prj-service/.../service/`（项目 status 迁移 DRAFT→OPEN→ON_HOLD/COMPLETED/CANCELLED + 立项守卫 + 完成前置校验任务已结束 + ON_HOLD 费用归集 config-gated）；`ErpPrjTaskBizModel`/`ErpPrjTask*Processor`（任务 4 态 + DAG 依赖 startTask/completeTask/blockTask/unblockTask + 成环检测上行链+HashSet+maxDepth）；`ErpPrjCostCollection*Processor`/`ErpPrjBilling*Processor`（成本归集/开票双轴审批 + 多币种）；`TimesheetPostingDispatcher`（工时成本凭证跨域过账 + daoFor(ErpMdSubject) + tryPost 容错）；里程碑状态迁移组件
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-010 propId + P1-MA1-022 跨域只读已登记，本审计复核状态机角度）；A2.1 P2P done（项目采购引用 projectId）；A2.2 O2C done（项目销售引用 projectId）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞异常悬挂同型范式）；A2.11 done（inventory 状态机 posted 三件套 + tryPost 容错同型范式）

- [ ] 维度「状态定义」：审查项目 status(erp-prj/project-status DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED) 5 态清晰性；任务 status(erp-prj/task-status TODO/IN_PROGRESS/DONE/BLOCKED) 4 态清晰性；ON_HOLD（等待恢复决策 vs 做什么）；成本归集/开票/工时 approveStatus 轴；里程碑状态轴清晰性。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「转换完整性」：项目生命周期迁移完整性（DRAFT→OPEN→ON_HOLD/COMPLETED/CANCELLED + ON_HOLD→OPEN）；**任务依赖 DAG 校验**（TODO→IN_PROGRESS 时校验前置完成，成环拒绝——task-dag.md 上行链+HashSet+maxDepth）；**项目完成需任务已结束**（owner doc §审查提示）；**ON_HOLD 费用归集暂停**（config-gated，owner doc §2）；**项目关闭后拒绝引用**（owner doc §7）。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「终端状态和恢复」：COMPLETED/CANCELLED 终态（不可直接恢复，重启新建项目）；已取消项目保留已归集成本不可删除（审计要求）；任务 DONE 终态；归档与活跃区分。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「异常路径」：核验全覆盖——**完成时仍有未结束任务**（提示先关闭任务或确认剩余取消）；**暂停后仍有费用流入**（配置控制：暂停项目拒绝新费用归集或允许标记）；**预算超支**（警告或拦截按配置，不阻止状态迁移）；并发状态变更（乐观锁）；**项目删除**（草稿可删除；进行中及以后只能取消/完成）；**任务依赖成环**（DAG 拒绝）；**工时成本凭证过账失败悬挂**（TimesheetPostingDispatcher tryPost 容错，与 finance P1-MA2-032/hr P1-MA2-048 同型——升级评估）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「可达性」：从 DRAFT 可达 OPEN→ON_HOLD/COMPLETED/CANCELLED；ON_HOLD 可回 OPEN；任务从 TODO 可达 IN_PROGRESS→DONE/BLOCKED→IN_PROGRESS；无不可达状态；无死锁（ON_HOLD↔OPEN 合法往复，退出条件 COMPLETED/CANCELLED）；里程碑状态轴可达性。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「角色和权限」：每个迁移绑定执行角色——立项/完成/取消（项目经理/管理员——影响报表与成本结转）；暂停/恢复（项目经理）；任务 startTask/completeTask/blockTask/unblockTask（任务负责人/项目经理）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「外部依赖」：采购/销售/费用单据引用项目（projectId 标注，项目关闭后拒绝引用）；工时触发成本凭证（经 IErpFinAcctDocProvider）；跨域经 I\*Biz Facade（IErpPurOrderBiz/IErpSalOrderBiz）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「TODO/任务策略」：DRAFT 产生 assigned（项目经理待立项）；OPEN 产生 monitor（进度监控）；ON_HOLD 产生 assigned（项目经理待决策恢复/取消）；COMPLETED/CANCELLED 否（终态）；任务 IN_PROGRESS 产生 TODO（执行）；BLOCKED 产生 assigned（解除阻塞决策）。**避免项目静默下沉**（owner doc §8）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 研发项目 happy path（DRAFT→OPEN→成本归集→COMPLETED→报表）；(b) **项目暂停与恢复**（OPEN→ON_HOLD→费用归集暂停→恢复）；(c) 项目取消（已归集成本保留）；(d) **任务依赖成环拒绝**（DAG 校验）；(e) **完成时仍有未结束任务**（提示先关闭或确认取消）；(f) **暂停后费用流入**（配置控制）；(g) **预算超支**（警告或拦截）；(h) **工时成本凭证过账**（TimesheetPostingDispatcher 跨域）；(i) **工时成本凭证过账失败悬挂**（tryPost 容错）；(j) **项目关闭后拒绝引用**。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`task-dag.md`/`cost-collection.md`/`profitability.md` 是否有匹配——重点核验：(1) §2 ON_HOLD 费用归集暂停 config-gated 落实；(2) §任务依赖规则 DAG 成环校验（task-dag.md 上行链+HashSet+maxDepth）；(3) §审查提示 项目完成需任务已结束；(4) §7 项目关闭后拒绝引用；(5) §3 终态不可恢复 + 已取消保留成本；(6) §8 TODO 策略（DRAFT/OPEN/ON_HOLD 避免沉没）；(7) 多币种成本归集 owner doc 一致性。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 复核已登记 finding 项目状态机角度：P1-MA1-010（多币种四件套 propId——状态机角度复核）/ P1-MA1-022（TimesheetPostingDispatcher daoFor(ErpMdSubject) + ErpPrjReportBizModel facade——异常路径复核），标注终态。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`（含：项目状态图 + 任务状态图 + 成本归集/开票/工时/里程碑状态轴迁移矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、ON_HOLD 费用归集/DAG 成环/完成前置/关闭后引用/工时凭证悬挂裁决、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] 项目状态图 + 任务状态图 + 成本归集/开票/工时/里程碑状态轴迁移矩阵产出，每个状态/转换有通过/失败裁决与证据
- [ ] 已识别控制点（状态定义[含 ON_HOLD 语义 + 里程碑] / 转换完整性[含 DAG 成环 + 完成前置任务结束 + ON_HOLD 费用归集 + 关闭后引用] / 终端与恢复 / 异常路径[含工时凭证悬挂 + 任务成环] / 可达性 / 角色权限 / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [ ] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 项目状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 prj 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（**项目完成未强制任务已结束** [若破坏完成前置——owner doc §审查提示] / **任务依赖 DAG 成环校验缺失** [若破坏依赖不变量——owner doc §任务依赖规则] / **ON_HOLD 费用归集未暂停** [若破坏归集控制——owner doc §2] / **项目关闭后仍可被引用** [若破坏引用约束——owner doc §7] / **工时成本凭证过账失败悬挂无告警闭环** [若破坏业财一致——同型升级评估]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。本审计对已登记 finding 只复核状态机运行时影响不重复登记根因；新 P1（如 ON_HOLD 费用归集缺口 / 完成前置任务校验缺口 / 里程碑死状态 [若确认]）按新 finding ID 登记。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 prj 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05972febbffewwPuiONA07Y9hs`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：`module-projects/` 全命名 BizModel/Processor 存在（ErpPrjProjectBizModel/ErpPrjTaskBizModel/TaskDependencyValidator.detectCycle/TimesheetPostingDispatcher:149 daoFor(ErpMdSubject)+tryPost:51）✓；ORM 字典 project-status/task-status 确认 ✓；finding ID（P1-MA1-010/P1-MA1-022）arm-index 描述匹配 ✓；scope matrix prj 列 `❓` ✓；roadmap A2.13 `todo` + 16 状态字段匹配 ✓；DAG 成环检测（上行链+HashSet+maxDepth + startTask/completeTask/blockTask/unblockTask）确认 ✓。**1 项非阻塞已修订**：`ErpPrjDeliverable`/「交付物轴」在 live ORM 不存在（仅 `ErpPrjMilestone`），草案审查迭代 1 后已移除全部交付物引用以保持基线准确性（rule 1）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。工时成本凭证触及会计保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [ ] 范围内行为完成（A2.13 projects 状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/task-dag/cost-collection/profitability owner doc 结论已反映）
- [ ] 已运行验证：审计不改代码，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）；projects 域自上次 codegen + 后续 fix plans 已建立全绿基线
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.6a manufacturing 状态机（工时成本凭证反馈 finance）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.6a done（生产执行状态机组件齐备已确认）。本审计做项目状态机**业务正确性**审查；工时成本凭证跨域过账归 finance TimesheetPostingDispatcher。
- Successor Required: `no`——A2.6a 已 done。

### A5.x 测试覆盖深度 / A5.5 测试隔离性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做项目状态机**业务正确性**审查；测试覆盖系统性审查归 MA5。本审计只复核工时凭证过账失败悬挂对状态机的影响。
- Successor Required: `yes`——MA5 执行时复核。

### A2.17 并发与乐观锁（并发状态变更）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（并发项目状态变更乐观锁），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated Deferred 偏离本身（ON_HOLD 费用归集 config / 预算超支 config）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/Deferred。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如严格的暂停归集控制启用 / 预算超支硬阻断启用）。

## Closure

Status Note: _（待执行 + 独立 closure audit）_

Closure Audit Evidence:

- _（待执行后填充）_

Follow-up:

- _（待执行后填充非阻塞跟进项；已确认缺陷不得出现在此处）_
