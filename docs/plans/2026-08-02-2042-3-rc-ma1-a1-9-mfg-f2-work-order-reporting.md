# 2026-08-02-2042-3 rc-ma1-a1-9-mfg-f2-work-order-reporting mfg-F2 工单与报工需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.9（MA1 需求追踪矩阵审计 — mfg-F2 工单与报工：UC-MFG-01 工单正常生产全流程 + UC-MFG-03 齐套校验 + UC-MFG-04 部分齐套强制开工 + UC-MFG-06 领料扣减 + UC-MFG-07 完工入库与成本结转 + UC-MFG-09 完工质检不合格→返工）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.9
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.9 的 0.2 依赖）、`2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（A1.8，同批次同范式；UC-MFG-05/08 预留写路径归 A1.8，本切片仅交叉引用领料扣减/释放的库存侧）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.9 给出 UC 清单 = `UC-MFG-01/03/04/06/07/09`（6 UC），含 `use-cases.md:18` / `:59` / `:75` / `:107` / `:125` / `:160` 锚点（inventory :343 确认一致）。

- **L1 需求契约（权威真相源）**：`docs/design/manufacturing/use-cases.md`：
  - UC-MFG-01 工单正常生产全流程（`:18`）：创建(产成品/数量/BOM)→提交→审核→齐套校验(BOM 展开 vs 可用量)→预留子件→开工(IN_PROCESS)→领料→报工(作业卡)→完工入库(COMPLETED)→成本结转。状态流转 DRAFT→SUBMITTED→(STOCK_RESERVED|STOCK_PARTIAL)→IN_PROCESS→COMPLETED；完工入库 库存余额[产成品]+=完工数量；成本结转 材料+人工+制造费用→产成品存货估值凭证。
  - UC-MFG-03 齐套校验（`:59`）：BOM 展开子件需求 vs 可用量(物料×仓库)；全部满足→STOCK_RESERVED；部分满足→STOCK_PARTIAL；齐套状态决定可否生产（配置 ErpMfgBom.consumption）。
  - UC-MFG-04 部分齐套强制开工（`:75`）：ErpMfgBom.consumption != STRICT 或主管权限 → STOCK_PARTIAL 可迁移 IN_PROCESS（强制开工）；缺件部分后续补料。
  - UC-MFG-06 领料扣减预留（`:107`）：领料单.数量 <= 预留剩余量（超预留拒绝或警告 erp-mfg.over-pick-warning）；领料后 MaterialReservation.pickedQty+=、reservedQty-=；库存余额.现有量-=、预留量-=。
  - UC-MFG-07 完工入库与成本结转（`:125`）：完工入库 库存余额[产成品]+=完工数量；产成品单位成本=(材料+人工+制造费用)/完工数量；生成存货估值凭证（借 产成品存货，贷 WIP/各成本要素）；材料成本=Σ领料单成本；人工成本=ΣJobCard.工时×费率；制造费用=Σ工序.工时×费率。
  - UC-MFG-09 完工质检不合格→返工（`:160`）：完工触发质检(若 BOM.inspection_required)→质检 REJECTED→不合格；原工单不可恢复(终态)，新建返工工单(关联原工单)；返工工单走标准流程产出合格品。

- **L2 owner doc 设计参考**：`docs/design/manufacturing/state-machine.md`（§1/§2 工单状态机 + §4 异常路径 + §6 部分齐套 + §质检约束声明 + §实现偏离补注）、`docs/design/manufacturing/bom-and-routing.md`（§成本计算 / §多级 BOM 展开）、`docs/design/manufacturing/material-reservation.md`（§齐套校验 `:120`，齐套定义；领料与预留 `:225`；**注意：预留写路径整节 Deferred，见 A1.8**）。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准。

- **L3 代码实现现状（执行时实测核验）**——工单主链**已实现**（10 态状态机 + 步骤化 Processor）：
  - **工单 BizModel/Processor**：`module-manufacturing/erp-mfg-service/.../entity/ErpMfgWorkOrderBizModel.java`（@BizMutation 64-121：提交/审核/开工/领料/报工/完工等）+ `processor/ErpMfgWorkOrderProcessor.java`（10 态状态机 `erp-mfg/work-order-status` + approveStatus 审批轴 + posted + 步骤化 protected 方法 + 状态迁移守卫 requireStatus/validateTransition*）。
  - **作业卡/报工**：`ErpMfgJobCard`（8 态状态机 `erp-mfg/job-card-status`）+ 报工 laborCost=durationMins/60×hourlyRate。
  - **领料**：`ErpMfgMaterialIssueBizModel`（R6.2 后为 thin Facade，`confirm` 委托至 `ErpMfgMaterialIssueConfirmProcessor:55` 调 `stockMoveBiz.generateMove`，共享 helper 在 `AbstractErpMfgMaterialIssueProcessor`；**注意 R6.2 per-mutation 拆分致行号偏移，执行时按逻辑复验**；4 态 `erp-mfg/issue-status` + posted）。
  - **齐套校验**：`KitAvailabilityChecker`（只读，读 ErpInvStockBalance.availableQuantity 决定 STOCK_RESERVED/STOCK_PARTIAL；**预留写路径 Deferred，见 A1.8**）。
  - **完工成本结转**：完工编排层差异计算/过账（ProductionVarianceDispatcher + ManufacturingIssuePostingDispatcher；**A4.2a 发现 P1-MA4-007 完工编排层差异计算/过账失败吞异常致业财悬挂，resolved 路径执行时 HEAD 复核**）。
  - **质检门控**：`reportCompletion` config-gated 钩子（`erp-mfg.inspection-gate-enabled` 默认 false）+ 跨域 `IErpQaInspectionBiz` + `InspectionTrigger.enforceGate`（UC-MFG-09 返工路径执行时核验）。

- **L4 测试证据现状**：`TestErpMfgWorkOrderEndToEnd`（全链）、`TestErpMfgWorkOrderStateMachine`（正反向+非法迁移）、`TestErpMfgBomExplosion`（BOM 展开）、`TestErpMfgMaterialIssue`（领料）、`TestErpMfgProductionVariance`（6 类差异）、`TestErpMfgVarianceRecomputeReversal`（差异重算红冲）、`TestErpMfgVarianceAlert`、`TestErpMfgCostRollup`（成本卷积）。**执行时逐项核验断言强度**：UC-MFG-06 领料扣减（预留写路径 Deferred → 核验是否有"超预留拒绝"断言）、UC-MFG-07 完工成本结转凭证（业财一体异常路径覆盖 — A4.2a 标 P1-MA4-009 测试有效性不足）、UC-MFG-09 返工路径。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a）= 工单/作业卡/领料/委外状态机审计**：Verdict **pass**（零 P0、1 P1：P1-MA2-035 作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态 + 3 P2 watch-only）。确认 @BizMutation 事务回滚一致性、委外 reverseCompletion 红冲闭环、部分齐套强制开工异常路径有出口、INSPECTING 态 config-gated 钩子偏离有 owner doc 记录。
  - **`docs/audits/2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（A4.2a）= 工单/BOM 代码质量审计**：Verdict **FAIL**（零 P0、3 P1：P1-MA4-007 完工编排层差异计算/过账失败吞异常致业财悬挂 / P1-MA4-008 跨域 daoFor 绕 I*Biz / P1-MA4-009 测试有效性不足 + 1 P2）。BomExpander DFS 环检测/算术正确性扎实。
  - **`docs/audits/2026-07-06-use-case-implementation-audit.md`**：UC-MFG-01~12 全部 ✅（粗粒度）。
  - **注意**：A2.6a/A4.2a 覆盖**状态机/代码质量**，但本切片从**需求契约↔实现符合性**视角补差异（UC-MFG-06 领料扣减预留的 L1 字面要求 vs Deferred 实现、UC-MFG-07 成本结转凭证完整性、UC-MFG-09 返工路径需求符合性、resolved finding HEAD 复核）。
  - **本切片须声明与上述 MA2/MA4 报告的差异增量**（报告段落 9）：复用 A2.6a 已证实的状态机/事务/红冲行为 + A4.2a 已证实代码质量 finding，只补需求视角差异。

- **arm-index 既有 finding 衔接**：工单相关——`P1-MA2-035`（作业卡 TRANSFERRED 死状态）/`P1-MA4-007`（完工编排吞异常）/`P1-MA4-008`（跨域 daoFor）/`P1-MA4-009`（测试有效性）/`P1-MA3-040/041/044/048`（owner-doc drift）。UC-MFG-06 领料扣减预留的 L1 字面要求可能关联 A1.8 预留 Deferred（交叉引用）。执行时 grep `arm-index.md` mfg 工单同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及会计过账逻辑（完工成本结转凭证/差异过账）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.9 切片的五级追踪审计报告缺失 = MA4（A4.2 扩展域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.9 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.9 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`，含方法论 §6 **9 段全部内容**：①UC-MFG-01/03/04/06/07/09 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 ErpMfgWorkOrderProcessor 10 态状态机 + ErpMfgWorkOrderBizModel mutation 链 + ErpMfgMaterialIssueBizModel 领料 + 作业卡报工 + 完工成本结转/差异过账链）③测试证据（注明断言强度）④运行时行为证据（复用 A2.6a/A4.2a，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2/MA4 报告差异增量声明。
- 对 6 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-MFG-01（全链状态流转 + 完工入库 + 成本结转）+ UC-MFG-03（齐套校验决定 STOCK_RESERVED/STOCK_PARTIAL + consumption 配置）+ UC-MFG-04（部分齐套强制开工条件）+ UC-MFG-06（领料扣减预留 — L1 字面 vs Deferred 交叉 A1.8）+ UC-MFG-07（完工单位成本 + 存货估值凭证 + 三成本要素计算）+ UC-MFG-09（质检不合格返工路径），各一矩阵行。
- 对候选缺口/偏离给出分级结论：UC-MFG-06 领料"超预留拒绝"（预留 Deferred 关联）、UC-MFG-07 完工成本结转凭证完整性（P1-MA4-007 吞异常 HEAD 复核）、UC-MFG-09 返工路径（config-gated 质检门控 + 返工工单关联）、作业卡 TRANSFERRED 死状态（P1-MA2-035 HEAD 复核）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / mfg use-cases / state-machine.md / bom-and-routing.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.1-A1.8 done/draft；A1.10 mfg-F3 BOM 与工艺路线 = 独立切片独立 plan；A1.9 只覆盖 UC-MFG-01/03/04/06/07/09）。**UC-MFG-05/08 预留写路径归 A1.8**，本切片 UC-MFG-06 领料扣减仅交叉引用预留 Deferred（库存侧可用量扣减归本切片）。
- **不重跑既有状态机/代码质量行为审计**（§去重协议：A2.6a/A4.2a 已证实状态机/事务/红冲/代码质量，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.9 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.9 UC 锚点）+ `docs/design/manufacturing/use-cases.md`（L1 真相源）+ `docs/design/manufacturing/state-machine.md` + `docs/design/manufacturing/bom-and-routing.md` + `docs/design/manufacturing/material-reservation.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A2.6a/A4.2a 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.6a/A4.2a 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgWorkOrderEndToEnd,TestErpMfgWorkOrderStateMachine,TestErpMfgMaterialIssue,TestErpMfgProductionVariance,TestErpMfgVarianceRecomputeReversal,TestErpMfgCostRollup`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-MFG-01/03/04/06/07/09 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:18/:59/:75/:107/:125/:160` 验收标准原文（禁止转述）；L2 引用 `state-machine.md`（§1/§2/§4/§6/§质检约束/§实现偏离补注）+ `bom-and-routing.md`（§成本计算/§展开）+ `material-reservation.md`（§齐套 `:120`，预留写路径交叉引用 A1.8 Deferred）对应 section（标注"设计参考，冲突以 L1 为准"）；L3 引用 `module-manufacturing/erp-mfg-service/.../entity/ErpMfgWorkOrderBizModel.java:line` + `processor/ErpMfgWorkOrderProcessor.java:line`（10 态状态机 + 步骤化 protected 方法）+ `ErpMfgMaterialIssueBizModel.java:line` + 作业卡报工 + 完工成本结转/差异过账链（调用链列全）；L4 引用 `Test*.java#method`（注明断言强度）；L5 复用 A2.6a/A4.2a + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-MFG-01——①全链状态流转 DRAFT→...→COMPLETED；②完工入库 库存余额+=；③成本结转凭证；UC-MFG-03——④齐套校验决定 STOCK_RESERVED/STOCK_PARTIAL；⑤consumption 配置（STRICT）；UC-MFG-04——⑥部分齐套强制开工条件（consumption!=STRICT 或主管权限 + config `erp-mfg.allow-partial-kit-start`）；UC-MFG-06——⑦领料扣减（**L1 字面"超预留拒绝/警告 + pickedQty+=/reservedQty-="，预留写 Deferred → 核验库存侧现有量扣减是否等价 + 超领是否有出口，交叉 A1.8**）；UC-MFG-07——⑧完工单位成本=(材料+人工+制造费用)/完工数量；⑨存货估值凭证（借产成品贷WIP）；⑩三成本要素计算（JobCard 工时×费率 / 工序工时×费率）；UC-MFG-09——⑪完工质检门控（config-gated + inspection_required）；⑫返工工单（关联原工单 + 原工单终态）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding HEAD 复核**：对工单相关 finding（P1-MA2-035 作业卡 TRANSFERRED 死状态 / P1-MA4-007 完工编排吞异常 / P1-MA4-008 跨域 daoFor / P1-MA4-009 测试有效性 / P1-MA3-040/041/044/048 owner-doc drift——**resolved 状态执行时经 arm-index grep 确认，未确认者按"未定"处理**）在当前 HEAD 代码实际落地（按逻辑非行号核验），逐条记录复核结论（已落地/回退/部分落地/documented simplification 仍 open successor）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-MFG-06 领料扣减（预留 Deferred 关联，按 §4 Q1 L1 为准 + 是否功能等价裁决）；UC-MFG-07 完工成本结转凭证完整性（P1-MA4-007 吞异常是否致凭证悬挂，会计正确性类 Q4 无例外）；UC-MFG-09 返工路径（config-gated 质检门控偏离 owner doc 已记录倾向接受）；作业卡死状态（P1-MA2-035 dict 治理）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-MFG-01/03/04/06/07/09 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.6a/A4.2a 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-⑫ 有明确分级（非悬空"待查"）；resolved finding HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` mfg 工单/领料/完工/质检同域同控制点（如 P1-MA2-035、P1-MA4-007/008/009、P1-MA3-040/041/044/048）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-MFG-06 领料扣减若与 A1.8 预留 Deferred 同根因则交叉引用而非重复新建。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如完工成本结转运行时凭证完整性、差异过账失败运行时悬挂、config-gated 质检门控运行时行为；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2/MA4 报告差异增量声明：声明复用 A2.6a（工单/作业卡/领料/委外状态机 + 事务回滚 + 红冲闭环）+ A4.2a（工单/BOM 代码质量 finding）已证实结论，列明本切片只补的需求视角差异（UC-MFG-06 领料扣减预留符合性 / UC-MFG-07 成本结转凭证完整性 / UC-MFG-09 返工路径 / resolved finding HEAD 复核）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03d4ef6d9ffePMQggYCtjxqEz`，fresh session，未起草本计划）。逐项实测核验全 PASS：roadmap 对齐（A1.9 / UC-MFG-01/03/04/06/07/09 / Deps=0.2 done / Skill）、6 UC 锚点 :18/:59/:75/:107/:125/:160 全匹配（完整枚举无跳无合并）、L3 路径存在（ErpMfgWorkOrderBizModel @BizMutation 64/70/76/82/88/94/100/108/121 + ErpMfgWorkOrderProcessor requireStatus/validateTransition*/doSubmit/doApprove/doStart + KitAvailabilityChecker + ProductionVarianceDispatcher + ManufacturingIssuePostingDispatcher + InspectionTrigger.enforceGate @ErpMfgWorkOrderReportCompletionProcessor:52 跨域 qa）、L5 finding 实测命中（P1-MA2-035 @A2.6a:349 / P1-MA4-007/008/009 @A4.2a:171/182/193）、跨切片边界正确（UC-MFG-05/08 归 A1.8，本切片仅 UC-MFG-06 库存侧交叉引用）、item typing/skill/anti-slack/只读 Closure-Gate 删门控有据/保护区域 ask-first 全合规。无阻塞 issue。Non-blocking（已吸收）：①ErpMfgMaterialIssueBizModel 行号 `confirm:88-128` 陈旧（R6.2 thin Facade 46 行，委托 ErpMfgMaterialIssueConfirmProcessor:55）→**已修正** + 加 R6.2 caveat；②P1-MA3-040/041/044/048 resolved 预设→**已修正**为"resolved 状态执行时 grep 确认"。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.9 报告 9 段齐全 + UC-MFG-01/03/04/06/07/09 逐矩阵行 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.9 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及会计过账逻辑（完工成本结转凭证/差异过账）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: A1.9 切片需求-实现符合性五级追踪审计闭环。结果表面 = `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（9 段齐全，382 行）+ `docs/audits/arm-index.md` 衍生登记。本计划为只读审计，无代码/ORM/api.xml/view.xml/真相源变更，故无需仓库验证命令门控（见 Closure Gates 删门控理由）。6 UC 全部出结论（5 接受/倾向接受 + 1 UC-MFG-06 领料扣减预留 P1 **复用** A1.8 `P1-RC-008` 不新建 finding ID），零 P0；resolved finding HEAD 复核 8/8 维持（HEAD `3c4beba78`，含 P1-MA4-007 修复落地确认）。报告与方法论 §1-§10 + §去重协议一致；§8 checker 19 规则全 actual==baseline 零裸漂移。UC-MFG-09 返工路径运行时存疑点 SP-2 已登记入 §7 交 MA4 A4.1/A4.2 展开（非阻塞 follow-up）。文本一致性已验证：Plan Status completed / 2 Phase completed / 所有 Exit Criteria `[x]` / Closure Gates 8/8 `[x]` / `docs/logs/2026/08-02.md` 同步。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，本审计执行者上下文外，由 mission-driver 调度）。本审计执行者未自我审计，Closure Gates 第 7 项已声明独立子代理会话要求。
- Evidence:
  - 审计报告产物：`docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（382 行，§1-§9 + Verdict 段齐全；§5.1 五级追踪矩阵 6 UC 行；§5.3 候选缺口逐条分级；§5.4 resolved finding HEAD 复核 8/8；§6 arm-index 衔接含 grep 比对证据；§8 actual vs baseline 实测表；Verdict 段整体 ⚠️(P1)）
  - arm-index 衍生登记：`docs/audits/arm-index.md` A1.9 报告清单登记 + `P1-RC-008` 行追加 RC A1.9 UC-MFG-06 领料扣减预留投影复用注记 + A1.9 summary 注记
  - HEAD 锚点：`3c4beba78`（resolved finding 复核基准，§5.4 + §8 实测表）
  - 日志同步：`docs/logs/2026/08-02.md:5-10`（A1.9 EXECUTE 条目 + bookkeeping + roadmap 状态同步）
  - draft review 证据：`## Draft Review Record`（独立子代理 `ses_03d4ef6d9ffePMQggYCtjxqEz`，fresh session，逐项实测核验全 PASS）
  - 只读审计零回归证明：本切片无生产代码变更（`docs/audits/nop-compliance-checker.sh` 19 规则零裸漂移，仅作 reporter）

Follow-up:

- UC-MFG-09 返工工单操作员驱动简化路径运行时操作流程存疑点（SP-2）→ MA4 A4.1/A4.2 运行时展开（已登记报告 §7）
- UC-MFG-06 领料扣减预留追踪侧缺失 → 复用 A1.8 `P1-RC-008`，按方法论 §10 经 MR1 R1.0 展开 RC-R1.n 修复（不在本审计计划实施）
