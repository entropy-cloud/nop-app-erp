# 2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine MA2 manufacturing 状态机审查 — 工单与报工（A2.6a）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.6a manufacturing 状态机审查 — 工单与报工（S 级拆分 1/2）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.6a）
> Related: `docs/plans/2026-07-28-0109-2-audit-remediation-ma2-mfg-mrp-bom-state-machine.md`（A2.6b MRP/BOM，S 级拆分 2/2，后续执行）；`docs/plans/2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine.md`+`2026-07-27-2315-1-...-period-budget-...`+`2026-07-27-2315-2-...-arap-settlement-...`（A2.5a/b/c finance 状态机审查三拆分范式，全 done）；`docs/plans/2026-07-27-1949-1-...-procure-to-pay-e2e.md`+`2026-07-27-1949-2-...-order-to-cash-e2e.md`（A2.1/A2.2 端到端，manufacturing 完工成本结转/委外加工费过账经 finance 凭证链已确认）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/manufacturing/state-machine.md`（适用对象一工单 / 适用对象二作业卡 / 适用对象三委外 + 实现偏离补注）+`bom-and-routing.md`+`subcontracting.md`（owner doc）
> Audit: required

## Current Baseline

manufacturing（制造）域是 ERP 生产的执行核心。本期 S 级状态机审查拆分 2 片：**A2.6a = 生产执行类状态机**（工单 WorkOrder / 作业卡 JobCard / 领料 MaterialIssue / 委外 SubcontractOrder）；**A2.6b = 计划规划类状态机**（MRP 计划 / 预测 / 建议单释放 / BOM）。本审计 A2.6a 聚焦**生产执行生命周期**——owner doc `state-machine.md` 适用对象一/二/三 + `MaterialIssue` 领料单（owner doc `state-machine.md §实现偏离补注`）。

实时仓库已落地的生产执行状态机实现（逐项核实，路径 `module-manufacturing/`）：

- **工单状态机**（`ErpMfgWorkOrder`，ORM `app-erp-manufacturing.orm.xml:570-665`）：列 `docStatus` dict `erp-mfg/work-order-status`（**10 态**：DRAFT/SUBMITTED/NOT_STARTED/IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/COMPLETED/STOPPED/CLOSED/CANCELLED，dict `orm.xml:35-46`）+ 列 `approveStatus` dict `wf/approve-status`（ORM:600）+ `posted` 布尔（ORM:604）。
  - 迁移实现（`ErpMfgWorkOrderProcessor.java` 513 行 + `ErpMfgWorkOrderBizModel.java` 106 行 Facade）：`submitForApproval`(→SUBMITTED) / `approve`(→NOT_STARTED) / `checkAvailability`(→STOCK_RESERVED 或 STOCK_PARTIAL) / `start`(→IN_PROCESS) / `reportCompletion`(→COMPLETED 当 willFinish) / `stop`(→STOPPED) / `resume`(→IN_PROCESS) / `close`(→CLOSED) / `cancel`(→CANCELLED) / `reject`/`reverseApprove`/`withdrawApproval`（审批轴）。**无独立 `submit`**——提交即 `submitForApproval`；**无 `reverseConfirm`**（reverseConfirm 仅 MaterialIssue 有）。守卫：`validateTransitionForSubmit`(需 DRAFT)/`validateBusinessRulesForApprove`(需 SUBMITTED)/`validateTransitionForStart`。
  - 完工移动单 + GL 过账内联（`generateCompletionMove:352-389` → `IErpInvStockMoveBiz.generateMove`）；完工差异过账 `ProductionVarianceDispatcher`（`reportCompletion:231/234` 触发）。
- **作业卡状态机**（`ErpMfgJobCard`，ORM `orm.xml:1305-1356`）：列 `status` dict `erp-mfg/job-card-status`（**8 态**：OPEN/WORK_IN_PROGRESS/PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED/ON_HOLD/SUBMITTED/COMPLETED/CANCELLED，dict `orm.xml:47-56`）。**无 approveStatus/posted 列**（仅 `status`，propId=8，precision=30）。
  - 迁移实现（`ErpMfgJobCardProcessor.java` 188 行 + `ErpMfgJobCardBizModel.java` 77 行）：`startJob`(OPEN→WORK_IN_PROGRESS) / `submitJob`(→SUBMITTED) / `completeJob`(→COMPLETED) / `holdJob`(→ON_HOLD) / `resumeJob`(→WORK_IN_PROGRESS) / `cancelJob`(→CANCELLED) / `recordWork`(无状态变更，写 TimeLog+laborCost)。
  - **`PARTIALLY_TRANSFERRED`/`MATERIAL_TRANSFERRED` 两态在 `ErpMfgJobCardProcessor` 无任何 `setStatus(..._TRANSFERRED)` 调用**——转序迁移未实现，dict 项悬空（候选可达性缺陷）。
- **领料单状态机**（`ErpMfgMaterialIssue`，ORM `orm.xml:976-1039`）：列 `docStatus` dict `erp-mfg/issue-status`（**4 态**：DRAFT/CONFIRMED/DONE/CANCELLED，dict `orm.xml:91-96`）+ `approveStatus`（wf/approve-status）+ `posted`。
  - 迁移实现（`ErpMfgMaterialIssueBizModel`）：`confirm`(→CONFIRMED，触发 `ManufacturingIssuePostingDispatcher` 过账) / `reverseConfirm`(@BizMutation，红冲 MANUFACTURING_ISSUE 凭证 + 反向 OUTGOING 移动单 + posted=false/docStatus=CANCELLED，owner doc `state-machine.md §领料红冲实现注记`)。
- **委外加工单状态机**（`ErpMfgSubcontractOrder`，ORM `orm.xml:1098-1179`）：列 `docStatus` dict `erp-mfg/subcontract-status`（**8 态**：DRAFT/SUBMITTED/APPROVED/ISSUED/RECEIVED/COMPLETED/CANCELLED/REJECTED，dict `orm.xml:100-109`，注意字典名是 `subcontract-status` 非 `subcontract-order-status`）+ `approveStatus`（**mandatory**，wf/approve-status）+ `postedStatus`（erp-md/posted-status）。
  - 迁移实现（`ErpMfgSubcontractOrderProcessor.java` 577 行 + 5 个独立审批 Processor + `ErpMfgSubcontractOrderBizModel.java` 70 行）：`issueMaterials`(→ISSUED) / `receiveFinished`(→RECEIVED) / `postProcessingFee`(→COMPLETED) / `reverseCompletion`(红冲路径) + 审批轴 submit/approve/reject/reverseApprove/withdrawApproval。3 段过账（SubcontractPostingDispatcher：发料/收货/加工费 3 个 AcctDocProvider）。
- **跨域访问**（production 代码）：制造域 production 代码**无 `daoFor(ErpFin*)`**（finance 全经 I*Biz）；`daoFor(ErpInv*)`（posting dispatcher 写 StockMove/StockLedger 读 StockBalance）+ `daoFor(ErpMd*)`（uom lookup）。posting dispatcher 跨域写 ErpInvStockMove/StockLedger 属 P1-MA1-022 同型（已登记 MR1）。
- **测试覆盖**：`TestErpMfgWorkOrderStateMachine`（312 行，happy path/齐套/部分齐套/停工恢复关闭/取消/非法迁移/超报）+`TestErpMfgWorkOrderEndToEnd`（含 JobCard 状态机 L188 + 检验门控）+`TestErpMfgSubcontracting`+`TestErpMfgSubcontractReverse`（红冲闭环）+`TestErpMfgMaterialIssue`+`TestErpMfgMaterialIssueReversal`（领料红冲）+`TestErpMfgCompletionPosting`+`TestErpMfgScheduleToJobCard`（APS 建卡状态门）。**无独立 JobCard 测试类**（覆盖在 WorkOrderEndToEnd 内）。

**已登记的直指生产执行状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-001`（todo MR1）：`ErpMfgWorkOrder`/`ErpMfgMaterialIssue` 多币种四件套 7 列 propId 缺失。**状态机 scope**：propId 缺失是 ORM 规范缺陷，不直接影响状态迁移正确性——本审计确认其在状态机行为上无升级（mechanical 缺陷非行为缺陷）。
- `P1-MA1-022`（todo MR1，9 域）：posting dispatcher 跨域只读 `daoFor(ErpInv*)`/`daoFor(ErpMd*)`。**状态机 scope**：跨域访问是状态迁移的副作用（过账/库存写），不破坏状态机裁决本身——本审计复核其是否在状态迁移异常路径引入悬挂。

**但从未做过一次覆盖生产执行状态机（工单 + 作业卡 + 领料 + 委外四组件）、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：工单 10 态中 STOCK_PARTIAL 是"等待补料"还是"等待决策"（动作 vs 等待点）；委外 8 态中 REJECTED 后状态（可回 DRAFT 重提？还是终态？）；作业卡两悬空态（PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED）；领料 4 态是否完整（有无草稿废弃路径）。
- **转换完整性**：作业卡 **转序迁移（→MATERIAL_TRANSFERRED/PARTIALLY_TRANSFERRED）未实现**——这是 owner doc `state-machine.md §适用对象二` 迁移图明确声明的迁移（作业中→部分转序/转序）但代码无 setStatus 调用；工单 INSPECTING 态 owner doc `§质检约束声明`引用但 dict 无此态（已文档化为 config-gated 钩子 `reportCompletion` 抛 `ERR_INSPECTION_REQUIRED` 拒绝 COMPLETED，工单保持 IN_PROCESS——需确认此偏离不破坏状态机）；委外 reverseCompletion 红冲路径的状态回写一致性。
- **终端状态与恢复**：工单三终态（COMPLETED/CLOSED/CANCELLED）不可恢复（owner doc 明示需新建返工工单）；委外 CANCELLED 终态；领料 DONE 后 reverseConfirm 回 CANCELLED（红冲恢复，非真终态）；REJECTED 委外单是否可重新提交流。
- **异常路径**：齐套校验子件不足（→STOCK_PARTIAL）；部分齐套强制开工后领料缺料（拒绝）；报工超量（拒绝，除非 config 允许超产）；BOM 变更影响已开工工单（快照原则）；并发领料扣减同批次（owner doc 列乐观锁——实际 @Version 覆盖？）；重复报工幂等；委外 reverseCompletion 非已完成单拒绝；reverseConfirm 未过账领料单拒绝（`ERR_MATERIAL_ISSUE_NOT_POSTED`）。
- **可达性**：**作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 是否可达**（Processor 无 setStatus——dict 项死状态？）；工单从 DRAFT 到每个终态的可达性；委外 REJECTED 后路径；是否有不可达终态。
- **角色与权限**：提交（计划员）/审核（生产主管）/开工（生产主管）/停工恢复/关闭（生产主管+管理员，影响成本结转）/取消（计划员）/报工（作业员）；危险操作（关闭工单部分完工结转 / 强制部分齐套开工缺料风险）；多角色冲突。
- **外部依赖**：领料/完工写库存经 `IErpInvStockMoveBiz`；成本结转/加工费过账经 finance 凭证链（完工 MANUFACTURING_RECEIPT/领料 MANUFACTURING_ISSUE/委外 3 段）；完工质检 quality 域联动（config-gated）；工作中心停机 maintenance 域；APS 排程建卡（config-gated）；外部步骤失败是否阻断状态迁移（@BizMutation 事务回滚）。
- **TODO/任务策略**：工单各非终端状态是否产生正确 TODO（DRAFT assigned/SUBMITTED pool/STOCK_PARTIAL assigned 缺料待补/IN_PROCESS monitor/STOPPED assigned）；作业卡、领料、委外的 TODO 策略；是否存在期望有人行动但不产生待办的状态（长期 STOCK_PARTIAL 滞留——owner doc 已声明需 TODO 提醒，实际是否生成）。
- **场景演练**：(a) 工单快乐路径（DRAFT→SUBMITTED→NOT_STARTED→STOCK_RESERVED→IN_PROCESS→领料→报工→COMPLETED+成本结转）；(b) 部分齐套强制开工；(c) 停工/恢复/关闭（部分完工结转成本）；(d) 完工质检不合格返工（新建返工工单）；(e) 作业卡全生命周期（OPEN→WIP→SUBMITTED→COMPLETED）+ 转序（当前未实现？）；(f) 领料确认→过账 + 领料红冲（reverseConfirm 红冲凭证+反向移动单）；(g) 委外 3 段（发料→收货→加工费过账→COMPLETED）+ reverseCompletion 红冲闭环；(h) 委外审核驳回→REJECTED→重提？；(i) 并发领料扣减同批次（owner doc 列乐观锁——@Version 覆盖评估，交接 A2.17）。
- **与设计文档一致性**：`state-machine.md` 工单 10 态 vs dict 10 态一致性（owner doc 表与 dict 是否逐一对齐——尤其 INSPECTING 态 owner doc `§质检约束声明`引用但 dict 无，已文档化为 config-gated 钩子，需确认 owner doc 已注记偏离）；作业卡 dict 含两转序态但实现未落地（owner doc 迁移图声明 vs 代码缺失——漂移）；委外 owner doc `subcontracting.md` 设计 10 态（含 PRODUCED/RETURNED）vs 实现 8 态（舍 2 态归 successor，已文档化）；领料状态机 owner doc 散落在 `§实现偏离补注`（无独立领料状态机章节）。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（**作业卡转序态不可达致 dict 死状态** [若破坏状态机] / **委外 reverseCompletion 红冲失败致状态与凭证悬挂半状态** [强一致回滚是否真覆盖] / **部分齐套强制开工缺料后领料异常路径悬挂** [状态机是否有出口]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **工单状态机**（10 态）+ **作业卡状态机**（8 态）+ **领料单状态机**（4 态）+ **委外加工单状态机**（8 态 + 审批轴）做系统性业务审查，产出审计报告。**严格限定 A2.6a scope = 生产执行类状态机**；MRP/预测/建议单释放/BOM 归 A2.6b。
- 重点核验已识别控制点：(1) 状态定义清晰性（STOCK_PARTIAL 动作 vs 等待点 / 委外 REJECTED 后状态 / 作业卡转序悬空态 / 领料完整性）；(2) 转换完整性（**作业卡转序迁移未实现** / 工单 INSPECTING config-gated 钩子偏离 / 委外 reverseCompletion 红冲状态回写）；(3) 终端与恢复（工单三终态不可恢复 / 领料 DONE→reverseConfirm 红冲恢复 / 委外 REJECTED 重提）；(4) 异常路径（齐套不足/部分齐套缺料/超报/BOM 快照/并发领料/幂等/红冲守卫）；(5) 可达性（**作业卡转序态可达性** / 工单全终态可达性 / 委外 REJECTED 路径）；(6) 角色权限（关闭/强制开工危险操作）；(7) 外部依赖（库存写/finance 凭证链/质检联动/maintenance/APS 建卡 + 事务回滚）；(8) TODO 任务策略（STOCK_PARTIAL 缺料 TODO / 长期滞留）；(9) 场景演练（9 个代表性场景）。
- 复核已登记 finding 在生产执行状态机运行时的行为影响：P1-MA1-001（propId 缺失，状态机角度确认无升级）/ P1-MA1-022（posting dispatcher 跨域访问，异常路径悬挂评估），标注终态（仅规范缺陷 / 产生运行时缺陷升级）。
- scope matrix §2.x manufacturing/生产执行状态机 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.6a 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.6b MRP/BOM/预测/建议单释放状态机 — 那是 S 级拆分 2/2，本审计只确认生产执行不依赖计划状态机的正确性。
- **不**审计 A2.1/A2.2 P2P/O2C 端到端编排正确性 — done；本审计只复核完工成本结转/委外加工费过账与 finance 凭证链的**状态机迁移**正确性（过账是状态迁移的副作用）。
- **不**审计 A2.5 finance 状态机 — done；本审计只确认制造过账经 finance I*Biz/凭证链（非制造域直接写 finance 实体——production 代码无 `daoFor(ErpFin*)` 已确认）。
- **不**审计 A4.2 manufacturing 代码质量 — 工单/委外 Processor 代码质量（异常处理/N+1/索引/辅助方法）系统性审查归 A4.2a/b；本审计只做状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发领料扣减同批次归 A2.17（owner doc `state-machine.md §异常路径`列"乐观锁"——实际 @Version 覆盖评估交接 A2.17）；本审计只标注观察到的并发敏感点。
- **不**审计 config-gated 偏离本身是否应实现（INSPECTING 态 / overhead 分配率 / 委外舍 PRODUCED/RETURNED）— 这些是 owner doc 已裁定的 Non-Goal/successor，本审计只确认其 config-gated 钩子在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/manufacturing/state-machine.md`（适用对象一工单 / 适用对象二作业卡 / 适用对象三委外 + §实现偏离补注 + §质检约束声明 — **需复核作业卡转序态 dict 含但实现缺失 / 工单 INSPECTING 态 config-gated 钩子偏离**）；`docs/design/manufacturing/subcontracting.md`（委外设计 10 态 vs 实现 8 态漂移，已文档化舍 PRODUCED/RETURNED）；`docs/design/manufacturing/bom-and-routing.md`（BOM 快照原则对已开工工单）；`docs/design/manufacturing/ui-patterns.md`；`docs/design/inventory/cross-domain.md`（领料/完工库存协作 + 余量校验）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）；`docs/architecture/posting-exemptions.md`（制造过账跨域写 ErpInv* 豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.6a 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：manufacturing 状态机本身非 ask-first 最高级保护区域，但其**过账副作用触及 finance 凭证链**（完工成本结转/委外加工费）与**库存写**（领料/完工移动单）是会计保护区域。P0 即时修复若触及 `ErpMfgWorkOrderProcessor`/`ErpMfgJobCardProcessor`/`ErpMfgMaterialIssueBizModel`/`ErpMfgSubcontractOrderProcessor`/posting dispatcher，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（`project-context.md §AI 阻塞条件`）。ORM 字典变更（work-order-status/job-card-status/issue-status/subcontract-status）属 ask-first。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 生产执行状态机系统性业务审查

Status: completed
Targets: `module-manufacturing/erp-mfg-service/.../service/processor/ErpMfgWorkOrderProcessor.java`（submitForApproval:72-78/approve:87-93/checkAvailability:109-116/start:118-123/stop:125-131/resume:133-139/close:141-154/cancel:156-167/reportCompletion:173-241+generateCompletionMove:352-389/守卫 validateTransitionForStart:329-340/validateTransitionForSubmit:245-254/validateBusinessRulesForSubmit:286-288/validateBusinessRulesForApprove:290-292）；`.../service/entity/ErpMfgWorkOrderBizModel.java`（Facade 106 行）；`.../service/processor/ErpMfgJobCardProcessor.java`（startJob:37-43/recordWork:45-71/submitJob:73-83/completeJob:85-91/holdJob:93-99/resumeJob:101-107/cancelJob:109-120）；`.../service/entity/ErpMfgJobCardBizModel.java`；`.../service/entity/ErpMfgMaterialIssueBizModel.java`（confirm/reverseConfirm:132/findIssueMove:271）；`.../service/processor/ErpMfgSubcontractOrderProcessor.java`（cancel:124-137/issueMaterials:142/151-174/receiveFinished:176/185-209/postProcessingFee:211/reverseCompletion:233-335 + 审批轴 submit/approve/reject/reverseApprove）；`.../service/entity/ErpMfgSubcontractOrderBizModel.java`；`.../service/posting/ManufacturingIssuePostingDispatcher.java`+`MfgPostingExecutor`+`ProductionVarianceDispatcher`+`SubcontractPostingDispatcher`；`.../service/posting/*AcctDocProvider.java`（完工/领料/委外 3 段 GL）；`module-manufacturing/model/app-erp-manufacturing.orm.xml`（work-order-status:35-46/job-card-status:47-56/issue-status:91-96/subcontract-status:100-109 + ErpMfgWorkOrder:570-665/ErpMfgJobCard:1305-1356/ErpMfgMaterialIssue:976-1039/ErpMfgSubcontractOrder:1098-1179 字段）；`docs/design/manufacturing/state-machine.md`+`subcontracting.md`+`bom-and-routing.md`+`inventory/cross-domain.md`；服务层 `TestErpMfgWorkOrderStateMachine`+`TestErpMfgWorkOrderEndToEnd`+`TestErpMfgSubcontracting`+`TestErpMfgSubcontractReverse`+`TestErpMfgMaterialIssue`+`TestErpMfgMaterialIssueReversal`+`TestErpMfgCompletionPosting`+`TestErpMfgScheduleToJobCard`
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-001 propId 缺失 + P1-MA1-022 跨域只读 IDaoProvider 已登记待 MR1，本审计复核状态机角度无升级）；A2.1-A2.2 done（完工成本结转/委外加工费过账经 finance 凭证链已确认）；A2.5a done（制造过账经 finance I*Biz，production 代码无 daoFor(ErpFin*)）

- [x] 维度「状态定义」：审查工单 10 态语义清晰性——STOCK_PARTIAL 是"等待补料/决策"还是动作；委外 8 态 REJECTED 后状态归属（终态还是可重提）；作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 两悬空态（动作 vs 等待点 + 是否应存在）；领料 4 态是否完整（有无 CANCELLED 草稿废弃——reverseConfirm 回 CANCELLED 是红冲恢复而非草稿废弃）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：列出工单每个状态所有传入/传出（10 态迁移矩阵）+ 作业卡（**重点：转序迁移 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED——owner doc `§适用对象二`迁移图声明但 `ErpMfgJobCardProcessor` 无 setStatus 调用，dict 项悬空**）+ 领料（DRAFT→CONFIRMED→DONE / DONE→CANCELLED 经 reverseConfirm 红冲）+ 委外（3 段生产轴 + 审批轴 + reverseCompletion 红冲路径）；工单 INSPECTING 态 config-gated 钩子偏离（owner doc 引用但 dict 无此态，已文档化为 reportCompletion 抛 ERR_INSPECTION_REQUIRED 拒绝 COMPLETED 保持 IN_PROCESS——确认不破坏状态机）；是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：工单三终态（COMPLETED/CLOSED/CANCELLED）不可恢复（owner doc 明示需新建返工工单——确认无 reOpen 路径）；委外 CANCELLED 终态；领料 DONE 后 reverseConfirm 回 CANCELLED（红冲恢复→非真终态）；委外 REJECTED 后是否可重新 submit（doSubmit 是否允许 REJECTED→SUBMITTED）；归档与活动单据是否可区分（posted/docStatus）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——齐套校验子件不足（→STOCK_PARTIAL）；部分齐套强制开工后领料缺料（拒绝——**状态机是否有出口或滞留 IN_PROCESS**）；报工超量（拒绝，除非 config 允许超产）；BOM 变更影响已开工工单（快照原则——已开工不追溯）；并发领料扣减同批次（owner doc 列乐观锁——**实际 @Version 覆盖评估，交接 A2.17**）；重复报工幂等（owner doc 声明已报工工序空操作——实际是否幂等）；委外 reverseCompletion 非已完成单拒绝；reverseConfirm 未过账领料单拒绝（ERR_MATERIAL_ISSUE_NOT_POSTED 守卫）；外部过账失败（@BizMutation 事务回滚是否真覆盖状态+凭证+移动单一致性）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：**重点——作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 是否可达**（Processor 无 setStatus → 若不可达则 dict 项死状态，与 finance A2.5c CANCELLED 同型可达性核验）；工单从 DRAFT 到 COMPLETED/CLOSED/CANCELLED 每个终态的可达性；委外从 DRAFT 到 COMPLETED/CANCELLED/REJECTED 的可达性；领料从 DRAFT 到 DONE/CANCELLED 可达性；是否有死循环或不可达终态路径。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个转换绑定执行角色——提交（计划员）/审核（生产主管）/开工（生产主管）/停工恢复/关闭（生产主管+管理员，影响成本结转）/取消（计划员）/报工（作业员 经 JobCard）/委外审核；危险操作（关闭工单部分完工结转影响成本 / 强制部分齐套开工缺料风险 / 委外 reverseCompletion 红冲恢复余额）；多角色冲突（计划员 vs 生产主管 vs 会计）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：领料/完工写库存经 `IErpInvStockMoveBiz`（制造域不直接写 ErpInv*——posting dispatcher 经 daoFor 写 StockMove/StockLedger 是 P1-MA1-022 同型，已登记 MR1，本审计复核异常路径是否悬挂）；成本结转/加工费过账经 finance I*Biz/凭证链（production 无 daoFor(ErpFin*)——全经 I*Biz，确认状态迁移与过账一致性）；完工质检 quality 域联动（config-gated 钩子）；工作中心停机 maintenance 域；APS 排程建卡（config-gated，state 门 NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/STOPPED 允许，DRAFT/SUBMITTED/终态拒绝）；外部步骤失败是否阻断状态迁移（事务回滚）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：每个非终端状态是否产生正确类型待办——工单 DRAFT(assigned 计划员)/SUBMITTED(pool 生产主管审核)/STOCK_PARTIAL(**assigned 缺料待补——owner doc §8 声明需 TODO 提醒，实际是否生成**)/IN_PROCESS(monitor)/STOPPED(assigned 生产主管决策)；作业卡、领料、委外的 TODO 策略；是否存在期望有人行动但不产生待办的状态（长期 STOCK_PARTIAL 滞留静默下沉）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 工单快乐路径（DRAFT→SUBMITTED→NOT_STARTED→STOCK_RESERVED→IN_PROCESS→领料→报工→COMPLETED+成本结转凭证）；(b) 部分齐套强制开工（STOCK_PARTIAL→IN_PROCESS 缺料后续补领）；(c) 停工/恢复/关闭（IN_PROCESS→STOPPED→IN_PROCESS 或 →CLOSED 部分完工结转成本）；(d) 完工质检不合格返工（config-gated 钩子阻止 COMPLETED + 新建返工工单）；(e) 作业卡全生命周期（OPEN→WORK_IN_PROGRESS→SUBMITTED→COMPLETED）+ **转序（当前未实现——演练确认缺口影响**）；(f) 领料确认→过账 + 领料红冲（reverseConfirm 红冲 MANUFACTURING_ISSUE 凭证+反向 OUTGOING 移动单+posted=false/CANCELLED）；(g) 委外 3 段（发料→收货→加工费过账→COMPLETED）+ reverseCompletion 红冲闭环（状态+凭证对称回退）；(h) 委外审核驳回→REJECTED→重提？；(i) 并发领料扣减同批次（@Version 覆盖评估，交接 A2.17）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`subcontracting.md` 是否有匹配——**重点漂移**：(1) 作业卡 dict 含 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 但 `ErpMfgJobCardProcessor` 无 setStatus → owner doc 迁移图声明 vs 代码缺失（漂移——需裁决是 dict 死状态还是未实现迁移）；(2) 工单 INSPECTING 态 owner doc `§质检约束声明`引用但 dict 无此态（已文档化为 config-gated 钩子，确认 owner doc §实现偏离补注已注记）；(3) 委外 `subcontracting.md` 设计 10 态（含 PRODUCED/RETURNED）vs 实现 8 态（已文档化舍 2 态归 successor）；(4) 字典名 `erp-mfg/subcontract-status` vs roadmap/owner doc 惯称 `subcontract-order-status`（命名漂移，无运行时影响）；(5) 领料状态机 owner doc 散落在 `§实现偏离补注`（无独立领料状态机章节——是否应补）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 生产执行状态机角度：P1-MA1-001（propId 缺失——状态机角度确认是 mechanical 规范缺陷不升级）/ P1-MA1-022（posting dispatcher 跨域只读 daoFor(ErpInv*)/daoFor(ErpMd*)——状态迁移异常路径是否引入悬挂：过账失败时事务回滚是否覆盖跨域写）。标注每项终态（仅规范缺陷 / 产生运行时缺陷升级）。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（含：工单/作业卡/领料/委外状态机状态图、各维度通过/失败裁决、控制点 PASS/FAIL、MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。仅本阶段交付的本地化检查列在此。

- [x] 工单（10 态）+ 作业卡（8 态）+ 领料（4 态）+ 委外（8 态+审批轴）的状态图与转换矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 已识别控制点（状态定义 / 转换完整性[含作业卡转序迁移未实现核验] / 终端与恢复 / 异常路径 / 可达性[含作业卡转序态可达性] / 角色权限 / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 生产执行状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.x manufacturing/生产执行状态机行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**作业卡转序态不可达致 dict 死状态** [若破坏状态机——按 finance A2.5c CANCELLED 同型裁决，若不可达且无语义则归 P1 dict 死状态清理而非 P0] / **委外 reverseCompletion 红冲失败致状态与凭证悬挂半状态** [若强一致回滚有缺口] / **部分齐套强制开工缺料后领料异常路径悬挂** [状态机无出口]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - **结果**：**零 P0**（三个候选 P0 经证据证伪）。(a) 作业卡 TRANSFERRED 死状态——不破坏状态机主路径（OPEN→WORK_IN_PROGRESS→SUBMITTED→COMPLETED 完整覆盖工序执行生命周期；多工序工单的工序卡完工即代表转序完成，设计可接受的简化），按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）同型裁决**降为 P1-MA2-035**（非 P0）。(b) 委外 reverseCompletion 红冲失败——经 `ErpMfgSubcontractOrderProcessor.reverseCompletion:233-240` step1-4 顺序（validateCanReverse → reverseGlPostings 逐段 try/catch → reverseInventoryMoves 逐段 try/catch + canSafelyReverse 前置 → doReverseCompletion posted=false/CANCELLED）+ 财务侧兜底 `MfgSubcontractReversalListener.onVoucherReversed:43-75`（监听 SUBCONTRACT_ISSUE/RECEIPT/FEE 红冲事件回退）**双路径证伪**，无悬挂半状态。(c) 部分齐套强制开工缺料异常——`ErpMfgMaterialIssueBizModel.confirm:108-110` 调 `stockMoveBiz.generateMove` 在可用量不足时抛 NopException → @BizMutation 事务回滚 → 领料单 DRAFT 保持 + 工单 IN_PROCESS 保持，**状态机有出口证伪**。无需 P0 即时修复或 fix plan。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。注意：本审计对已登记 finding（P1-MA1-001/022）只复核状态机运行时影响不重复登记根因；若发现新 P1（如作业卡转序态 dict 死状态 [若裁决为 P1 而非 P0/P2] / 委外 reverseCompletion 一致性缺口 [若裁决为 P1] / 领料状态机 owner doc 缺独立章节）按新 finding ID 登记。
      - Skill: none
      - **结果**：新增 1 项 P1（**P1-MA2-035** 作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态 + owner doc 迁移图声明漂移）登记至 `arm-index.md` §P1 详细清单 + §P1 类型分布 narrative + A2.6a 新增项章节。已登记 finding（P1-MA1-001 propId 缺失 / P1-MA1-022 跨域只读 daoFor）经状态机角度运行时复核**无升级**，不重复登记根因（在报告 §4 复核表中标注终态）。owner doc 领料状态机散落 + 字典命名漂移 + 报工超产 config-gate 缺失登记为 **3 项新 P2** watch-only（P2-MA2-042/043/044）。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.x manufacturing/生产执行状态机 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none
      - **结果**：(a) `arm-index.md` §报告清单新增本报告行（状态 done）；(b) `arm-index.md` §P1 发现汇总 narrative 追加 A2.6a 新增项段；(c) `arm-index.md` §P1 详细清单新增 P1-MA2-035 行；(d) `arm-index.md` §P2 发现汇总新增 P2-MA2-042/043/044 行 + A2.6a 新增项章节；(e) `audit-remediation-scope-and-dimension-matrix.md` §2.2 状态机正确性行 mfg 列由 `❓S拆` 推进至 `⚠️P1(A2.6a✅;A2.6b❓)` + narrative 追加 A2.6a 完成段。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05b6f5318ffeznpO9IoYLhym3q`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：4 个状态字典（work-order-status 10 态 `orm.xml:35-46` / job-card-status 8 态 `:47-56` / issue-status 4 态 `:91-96` / subcontract-status 8 态 `:100-109`）行号精确 ✓；WorkOrder/JobCard/MaterialIssue/Subcontract 实体行号 + 列定义精确 ✓；**关键可达性假设经双重确认**——`ErpMfgJobCardProcessor`（188 行全文读）无 TRANSFERRED 的 setStatus 调用 + 全 `src/main` grep `JOB_CARD_STATUS_*_TRANSFERRED` 仅 ErpMfgConstants.java:52-53 常量定义零使用 ✓（dict 项死状态确认）；WorkOrderProcessor 513 行 + 11 个迁移方法行号精确 ✓；SubcontractOrderProcessor 577 行 + 5 个审批 Processor ✓；BizModel 行数精确（WorkOrder 106/JobCard 77/Subcontract 70）✓；**production 代码无 `daoFor(ErpFin`（45 匹配全在 src/test）**✓；8 个测试文件存在 + 无独立 JobCard 测试类 ✓。检查清单全 PASS（基线准确性/格式/结果表面——A2.6a 生产执行 vs A2.6b 计划规划是 roadmad 规则 7 S 级拆分的合理结果表面，不过度拆分/Item 类型 Proof+Fix/Add/Follow-up/技能匹配工作方法 state-machine-business-review/反松弛无 optional·consider·maybe/不可降级项 JobCard TRANSFERRED 死状态 defect 已显式路由到 P0 即时通道或 P1 MR1 而非 Follow-up/结束门控含独立结束审计门控 + 全量验证在 Closure Gates 非阶段退出/退出标准可观察无样板）。**采纳的非阻塞精化**：`findIssueMove` 行号 272 → 271（已应用至 Targets）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。生产执行状态机过账触及会计/库存保护区域，P0 即时修复须额外人工确认。

- [x] 范围内行为完成（A2.6a 生产执行状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/subcontracting/bom-and-routing owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests`（154 reactor 模块 BUILD SUCCESS，2026-07-28T01:26:46）+ `mvn test -pl module-manufacturing/erp-mfg-service -am`（mfg-service 141 tests / 0 failures / 0 errors，2026-07-28T01:29:35）作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Closure

Status Note: A2.6a manufacturing 生产执行状态机审查完成。审计报告 `docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md` 产出（Verdict: pass），10 维度全覆盖，工单 10 态 + 作业卡 8 态 + 领料 4 态 + 委外 8 态+审批轴四组件状态图与转换矩阵齐全。**零 P0**（三个候选 P0 经证据证伪）；**1 项新 P1**（P1-MA2-035 作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态 + owner doc 迁移图声明漂移）；**3 项新 P2** watch-only（P2-MA2-042/043/044）；MA1 finding [P1-MA1-001/022] 状态机角度运行时复核无升级；并发敏感点 5 处交接 A2.17 含 @Version 透明乐观锁降级重要事实。arm-index + scope matrix 已同步更新。回归基线：154 reactor 模块 BUILD SUCCESS + mfg-service 141 tests 全绿。

Closure Audit Evidence:

- 审计报告：`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（Verdict: pass，零 P0，1 P1 + 3 P2 watch-only）
- 索引更新：`docs/audits/arm-index.md` §报告清单新增本报告行（done）+ §P1 详细清单新增 P1-MA2-035 行 + §P2 发现汇总新增 P2-MA2-042/043/044 行 + A2.6a 新增项章节
- 矩阵更新：`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.2 状态机正确性行 mfg 列 `❓S拆` → `⚠️P1(A2.6a✅;A2.6b❓)` + narrative 追加 A2.6a 完成段
- 路线图更新：`docs/backlog/audit-remediation-roadmap.md` A2.6a 工作项 `todo` → `done`
- 验证基线：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，2026-07-28T01:26:46）+ `mvn test -pl module-manufacturing/erp-mfg-service -am`（mfg-service 141 tests / 0 failures / 0 errors / 0 skipped，2026-07-28T01:29:35）

Follow-up:

- A2.6b MRP/BOM/预测状态机审查（S 级拆分 2/2，独立 plan 后续执行）
- A2.17 并发与乐观锁系统性审计（5 处并发敏感点交接）
- A4.2a/b manufacturing 代码质量审计（Processor 异常处理/N+1/索引系统性审查）
- MR1 修复：P1-MA2-035（作业卡 TRANSFERRED dict 死状态清理）+ P2-MA2-042/043/044 watch-only 收敛

## Deferred But Adjudicated

### A2.6b MRP/BOM/预测状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 那是 S 级拆分 2/2，计划规划类状态机。本审计只确认生产执行不依赖计划状态机的正确性（生产执行消费 MRP 释放的工单，但状态机独立）。
- Successor Required: `yes`——A2.6b 执行时复核。

### A4.2a/b manufacturing 代码质量审计

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做工单/作业卡/委外状态机**业务正确性**审查；Processor 代码质量（异常处理类型/N+1/索引/辅助方法）系统性审查归 A4.2a/b。
- Successor Required: `yes`——A4.2a/b 执行时复核。

### A2.17 并发与乐观锁（并发领料扣减同批次）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（owner doc `§异常路径`列"乐观锁"——实际 @Version 覆盖评估 + reportCompletion 并发 + 委外 reverseCompletion 并发），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated 偏离本身（INSPECTING 态 / overhead 分配率 / 委外 PRODUCED/RETURNED / APS 自动建卡）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 Non-Goal/successor。本审计只确认其 config-gated 钩子在状态机上不引入悬挂（INSPECTING config-gated 钩子拒绝 COMPLETED 保持 IN_PROCESS——有出口）。
- Successor Required: `yes`——各 successor 触发条件满足时（如 quality 域深化 flip config=true / 工作中心级精确费率 / 委外 Portal 协同 / 事件驱动实时建卡）。
