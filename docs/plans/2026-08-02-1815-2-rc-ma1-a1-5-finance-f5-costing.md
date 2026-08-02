# 2026-08-02-1815-2 rc-ma1-a1-5-finance-f5-costing finance-F5 成本核算（FIFO 出库 + 到岸成本）需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.5（MA1 需求追踪矩阵审计 — finance-F5 成本核算：FIFO 出库成本与到岸成本）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.5
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.5 的 0.2 依赖）、`2026-08-02-1600-1-rc-ma1-a1-1-finance-f1-posting-engine.md`（A1.1 done，同 finance 审计范式）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.5 给出 UC 清单 = `UC-FIN-10`（1 UC），含 `use-cases.md:183` 锚点。

- **L1 需求契约（权威真相源）**：`docs/design/finance/use-cases.md` UC-FIN-10 FIFO 出库成本与到岸成本（`:183`）：
  - FIFO 出库：出库移动单 → 按 `incomingDate` 升序消耗 StockQueue；队列不足 → 跨队列消耗；出库成本 = Σ(各队列消耗量 × 队列单价)。
  - 到岸成本：运费/保险/关税 → 按金额比例分摊到入库批次；入库成本 += 分摊费用；后续出库按更新后的队列单价计算。

- **L2 owner doc 设计参考**：`docs/design/finance/costing-methods.md`（§FIFO 队列 `:196-222`、§FIFO 出库逻辑 `:224-249`、§到岸成本定义 `:350-363`、§到岸成本分摊+算例 `:365-413`、§成本调整 `:415-477`、§实现注记 `:35-38/:42-50/:60-66`）。**注意命名漂移**：设计文档称队列实体为 "StockQueue"（`:204-222`），ORM 权威名为 `ErpInvCostLayer`（`costMethod=FIFO` 过滤）——属 cosmetic owner-doc 命名漂移（P2 MA3 文档类，非功能缺口）。owner doc 显式声明权威实现位于 `module-inventory/erp-inv-service`（`:31-38`）。

- **L3 代码实现现状（实测，subagent 探查）**——功能**已完整实现于 inventory 模块**（非 stub，finance 侧仅持 GL 凭证目的地 + 期间结账调用，无 finance 侧 costing service）：
  - FIFO 出库：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/costing/FifoCostingStrategy.java`（210 行）——按 `incomingDate` 升序消耗（`:202-203` 排序）；历史成本守卫 `incomingDate <= businessDate`（`:198-200`）；跨队列消耗循环 `:105-120`（`take = remaining.min(avail)` `:113`）；出库成本 = Σ(消耗量 × 队列单价)（`:114/118`）；不足拒绝 `ERR_COST_NOT_AVAILABLE`（`:97-101/:121-126`）；红冲语义（Decision (a)）刷新 `line.unitCost` 为加权成本 `:130-132`。
  - 策略分派：`StockMoveBookkeeper.java:116-130`（逐行按 `costMethodResolver.resolve` 分派 onIncoming/onOutgoing；内部调拨成本桥 `:120-122`；7 策略注册 `:95-110`：MovingAverage/Fifo/Standard/Lifo/WeightedAverage/Specific/Batch）。
  - 到岸成本分摊引擎：`LandedCostAllocationEngine.java`（174 行，纯函数无 ORM）——按金额比例分摊（BY_AMOUNT 默认 `:108-109`，BY_QUANTITY/BY_WEIGHT `:101-110`）；新单价 = old + 分摊÷qty `:82-88`；Σ 分摊 == total（末行吸收尾差 `:72-74`）；空输入拒绝 `:50-53/:59-62`。
  - 到岸成本编排：`ErpInvLandedCostProcessor.java`（504 行）——approve `:87-89` → `ErpInvLandedCostApproveProcessor`；`createAndApplyCostAdjust:294-337`；校验 receive 已审+无重复分配 `:372-407`；并发守卫 receive 悲观锁 `:388-390`；分配预览（只读）`:93-113`；红冲 `reverseApprove:137-139` / `doReverseApprove:156-201`；红冲失败告警 `:483-499`。
  - 队列单价更新（到岸成本 FIFO 路径）：`CostAdjustmentService.java:108-112`（按 costMethod 分派）；FIFO 追加 delta 调整层 `applyFifo:137-147` / `appendFifoAdjustLayer:149-171`（哨兵 `incomingMoveId = -line.getId()` `:168`）；MA/Standard 更新 `balance.avgCost` `:123-135`；成本调整台账行（moveId=0 哨兵）`writeLedger:248-270`；红冲回滚 + 物理删除 FIFO delta 层 `reverseLine:175-192/:194-202`。
  - 过账分派：FIFO COGS 通道经 `FifoCostingStrategy.writeLedger`（`ctx.writeLedger:145-146`）写 `ErpInvStockLedger`，`InvPostingDispatcher` 取 `ledger.totalCost`；LANDED_COST 经 `LandedCostPostingDispatcher` + `LandedCostAcctDocProvider`（businessType=LANDED_COST(490)，Dr 1401 / Cr 2202）。

- **L4 测试证据现状**：FIFO——`TestErpInvFifoCosting`（6，含 `testOutgoingSpansMultipleLayersWeightedCost:111-143` 跨队列消耗断言精确匹配需求公式 / `testReverseRestoresCostInvariant:163-204` 红冲不变式 / `testFirstOutgoingWithoutCostLayerRejected` 拒绝）、`TestErpInvFifoCostingEndToEnd`（3，含全链→SALES_OUTPUT 凭证 + reclose 重建缺失层）。到岸成本——`TestErpInvLandedCostAllocationEngine`（5，含 `testAllocateByAmount:33-58` 精确匹配 owner doc 算例 / 尾差吸收 / 空拒绝）、`TestErpInvLandedCostEndToEnd`（4，BY_AMOUNT/BY_QUANTITY/多 AP partner/重复分配拒绝）、`TestErpInvLandedCostReversal`（2，红冲原凭证 isReversed + 回滚 + 非 posted 拒绝）、`TestErpInvCostAdjust`、`TestErpInvLandedCostReceiveMutex`、`TestErpInvLandedCostReverseFailureAlert`。**注意**：`TestErpInvLandedCostEndToEnd` 与 `TestErpInvLandedCostReversal` 均用 `MOVING_AVERAGE` 物料（`:82-83/:90`），**FIFO 物料 + 到岸成本交互（delta 层 + 后续 FIFO 出库消耗更新后单价）无 E2E 测试**（P2-MA2-029 successor）；FIFO 物料到岸成本红冲（delta 层部分消耗后物理删除）无测试（P2-MA2-029 successor）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（331 行）= THE costing 审计**：多维 MA2，覆盖 7 costMethod + 三方对账 + reclose + 成本调整 + 到岸成本 + PPV。结论 0 P0、2 P1（P1-MA2-023 SPECIFIC、P1-MA2-024 STANDARD）、5 P2 watch-only。审计 **closed**。
  - `docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`（A2.11，含 LandedCost approve/reverseApprove 状态机）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5，含 `StockMoveBookkeeper` + `ErpInvLandedCostProcessor` + 7 CostingStrategy 代码质量；P1-MA4-020 到岸成本红冲业财悬挂、P1-MA4-022 跨域 daoFor）。
  - `docs/audits/2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（A4.1b，finance 侧 GL 成本映射 + 期间结账，**不含** FIFO/到岸成本算法本身）。
  - `docs/audits/2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md:178`（inventory 28 测试文件覆盖到岸成本 E2E）。
  - **陈旧证据**：`docs/audits/2026-07-06-use-case-implementation-audit.md:117` 将 UC-FIN-10 标 🔶"到岸成本分摊未实现(Non-Goal)"——**已过期**，到岸成本经 plan `2026-07-10-1100-3` 已完整落地，本审计须重新核验，不引用此陈旧行为缺口证据。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实的 costing 行为，只补"需求契约↔行为"差异（FIFO+到岸成本交互测试缺口、StockQueue 命名漂移、resolved finding 在 HEAD 复核等）。

- **arm-index 既有 finding 衔接**：costing 相关——`P1-MA2-023`（SPECIFIC 历史成本守卫缺失，**resolved R1.12**）、`P1-MA2-024`（STANDARD 红冲跨重估不变式，**resolved R1.12**）、`P1-MA2-085`（LandedCost TOCTOU + 非唯一索引，**resolved R1.28**）、`P1-MA4-020`（到岸成本红冲业财悬挂，**resolved R1.16**）、`P1-MA4-021`（pur+sal+inv 测试有效性，**resolved R2.14**）、`P1-MA1-022`（costing 跨域只读 daoFor 治理，todo MR1 运行时正确）、`P2-MA2-026/027/028/029/030`（watch-only，含 P2-MA2-029 到岸成本 FIFO 红冲 successor）、`P2-MA4-010/011`（watch-only）。**本切片须复核 resolved finding 在 HEAD 实际落地**（§2 判据 + 防止"已 resolved 但代码回退"），并对候选新缺口（StockQueue 命名漂移、FIFO+到岸成本交互测试缺口）按 §7 grep 比对后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及成本过账逻辑（FIFO/到岸成本/成本调整凭证）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.5 切片的五级追踪审计报告缺失 = MA4（A4.1 业财展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.5 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.5 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-5-finance-f5-costing.md`，含方法论 §6 **9 段全部内容**：①UC-FIN-10 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，跨模块 inventory↔finance 调用链列全）③测试证据（注明断言强度）④运行时行为证据（复用 MA2/E2E，补差异）⑤五级追踪矩阵 + 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2 报告差异增量声明。
- 对 UC-FIN-10 逐条核验**每条验收标准**（完整枚举，§3）：FIFO 出库三条（incomingDate 升序/跨队列/Σ单价）+ 到岸成本三条（按金额比例分摊/入库成本+=/后续出库用更新单价）。
- 复核 arm-index 中标记 resolved 的 costing finding（P1-MA2-023/024/085、P1-MA4-020/021）在 HEAD 代码实际落地，记录复核结论。
- 对候选缺口给出分级结论：StockQueue↔ErpInvCostLayer 命名漂移、FIFO+到岸成本交互 E2E 测试缺口、FIFO 物料到岸成本红冲测试缺口——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance use-cases / `costing-methods.md` 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.1-A1.4 done/进行中；A1.6-A1.51 各自独立 plan；A1.5 只覆盖 UC-FIN-10）。
- **不重跑既有 MA2 行为审计**（§去重协议：A2.4 costing consistency 已证实行为直接引用，只补需求视角差异；不重审架构/代码质量维度）。
- **不复跑 MA1-MA7 架构漂移类审计**（以 audit-remediation 收口为准）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.5 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.5 UC 锚点）+ `docs/design/finance/use-cases.md`（L1 真相源）+ `docs/design/finance/costing-methods.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用既有 A2.4 costing 审计 + E2E recordings（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-inventory/erp-inv-service -Dtest=TestErpInvFifoCosting,TestErpInvLandedCost* -am`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与符合性结论 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-FIN-10 **一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:183` 验收标准原文（FIFO 出库三条 + 到岸成本三条，禁止转述）；L2 引用 `costing-methods.md` 对应 section（§FIFO 队列/出库/到岸成本分摊/算例/成本调整，标注"设计参考，冲突以 L1 为准"，记录 StockQueue↔ErpInvCostLayer 命名漂移）；L3 引用 `module-inventory/erp-inv-service/.../<file>:line`（含 `FifoCostingStrategy`/`StockMoveBookkeeper`/`LandedCostAllocationEngine`/`ErpInvLandedCostProcessor`/`CostAdjustmentService` 跨模块调用链列全）；L4 引用 `Test*.java#method`（注明断言强度，引用 MA5 评级）；L5 复用 A2.4 costing 审计已证实行为 + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**需求↔实现逐条对照**（逐条验收标准）：①FIFO 按 incomingDate 升序消耗（`FifoCostingStrategy:202-203`）；②队列不足跨队列消耗（`:105-120`）；③出库成本 = Σ(消耗量×单价)（`:114/118`）；④到岸成本按金额比例分摊（`LandedCostAllocationEngine:108-109/:76-78`）；⑤入库成本 += 分摊（`:82-88`）；⑥后续出库按更新单价（`CostAdjustmentService.appendFifoAdjustLayer:149-171` 追加 delta 层 + FIFO 出库消耗）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding HEAD 复核**：对 arm-index 标记 resolved 的 costing finding（P1-MA2-023 SPECIFIC 守卫 / P1-MA2-024 STANDARD 红冲 / P1-MA2-085 LandedCost TOCTOU / P1-MA4-020 到岸成本红冲悬挂 / P1-MA4-021 测试有效性）在当前 HEAD 代码实际落地，逐条记录复核结论（已落地 / 回退 / 部分落地）；防止"已 resolved 但代码回退"。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据给出 UC-FIN-10 符合性结论（P0/P1/P2/接受）：核心算法已实现且 MA2 已证实→倾向接受；候选缺口（FIFO+到岸成本交互测试缺口、StockQueue 命名漂移）按 §2 定级——测试缺口若为"会计正确性无直接断言"考虑 P1/P2（§2 P1②/P2）；命名漂移属 P2 文档类。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-FIN-10 一矩阵行（6 条验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.4 来源
- [x] UC-FIN-10 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；resolved finding HEAD 复核结论已记录；候选缺口有明确分级（非悬空"待查"）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` costing 同域同控制点（如 P2-MA2-029 到岸成本 FIFO 红冲 successor、P2-MA4-010 层复制 partial-consumption、P1-MA1-022 跨域 daoFor 等）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：声明复用 `2026-07-27-2211-arm-ma2-inventory-costing-consistency.md`（A2.4）等已证实行为，列明本切片只补的需求视角差异（FIFO+到岸成本交互测试缺口 / StockQueue 命名漂移 / resolved finding HEAD 复核结论 / 陈旧 2026-07-06 审计行校正等）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [x] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03e0443c3ffeOxqOzFoMe7ZQf1，fresh session，未起草本计划）。10 项检查 A-J 全 PASS：格式完整、Deps 正确（A1.5 Deps=0.2 done）、单结果表面、Baseline 准确（逐项实测命中：实现位于 inventory 模块非 finance / FifoCostingStrategy:202-203 incomingDate 升序 + :105-120 跨队列 + :114/118 Σ 成本 + :130-132 红冲 Decision(a) / LandedCostAllocationEngine BY_AMOUNT / CostAdjustmentService FIFO delta 层 / StockQueue↔ErpInvCostLayer 命名漂移正确框定为 cosmetic P2 / 到岸成本 E2E 测试用 MOVING_AVERAGE 非 FIFO = 交互缺口 / A2.4 costing 审计存在且 closed / 5 个 resolved P1 finding 状态 / 陈旧 2026-07-06 审计行），UC 覆盖 UC-FIN-10（FIFO 3 + 到岸成本 3 断言全枚举），方法论 §1-§10 + §去重对齐，resolved finding HEAD 复核为合理方法论增强（防"已 resolved 但回退"，范围限定 5 个命名 finding 非重审），反松弛合规，Closure Gates audit-only 有据，无范围蔓延（不复跑 MA1-MA7），item typing 合规，Skill 就绪。无阻塞。Non-blocking（已评估，无需修订）：①`## Closure` 段落缺失——draft 标准无此段（A1.1 draft 亦无），执行后填；②owner doc :350-477 行范围未逐行读，但经 MA2 报告内容 + 实现注记 :42-66 交叉确认，在容差内。共识达成，可转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + resolved finding HEAD 复核 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。（§8 含 checker 实测记录，但 checker 是 reporter 非门控；门控真值在 CI workflow。）

- [x] 范围内行为完成：A1.5 报告 9 段齐全 + UC-FIN-10 矩阵行（6 验收标准）+ resolved finding HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.5 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Closure

> 独立结束审计由独立子代理 `ses_03d775af1`（fresh session，未执行本计划工作）于 2026-08-02 执行，verdict = `CLOSURE_AUDIT: pass`。

**结束审计证据**（独立子代理实测复核）：
- §6 9 段完整性：§1-§9 全部存在且实质填充（L1 逐字 6 验收标准 + 跨模块调用链 inventory↔finance↔purchase + 8 测试文件断言强度 + 复用 A2.4/A2.11/A4.5 + 1 UC 矩阵 + 6 验收标准裁决 + 3 候选缺口分级 + 复用/新增裁决 + HEAD 复核 + 双向追溯 + 3 MA4 交接 + checker actual vs baseline + 3 声明 + MA2 差异增量）。
- 抽样实测命中：`FifoCostingStrategy:202-203` incomingDate 排序 / `:105-120` 跨层循环 / `:114/118` Σ 成本 / `LandedCostAllocationEngine:108-109` BY_AMOUNT 默认 / `CostAdjustmentService:149-171` delta 层 -lineId 哨兵 / `SpecificCostingStrategy:174+192-194` P1-MA2-023 守卫 / `StandardCostingStrategy:55-59` P1-MA2-024 / `lockReceiveForAllocation:388-390` P1-MA2-085 SELECT FOR UPDATE[报告正确承认非 UK 路径] / `dispatchReverseFailureAlert:483-499` P1-MA4-020 / 测试文件齐备 P1-MA4-021 / `testOutgoingSpansMultipleLayersWeightedCost` 断言 -620 精确匹配 L1 Σ 公式 / L1 逐字引用 use-cases.md:189-197。
- arm-index 双向追溯：报告行 line 79 done / P2-RC-004+005 入 RC 表 / A1.5 summary 注记 / P2-MA2-029 行追加 RC 交叉引用。
- 方法论纪律：L1 逐字（§1/Q1）/ 完整枚举（§3 6 验收标准全进 L5）/ §7 grep-before-create / §9 真相源未修改 / §8 checker 为 reporter 非门控。
- 候选缺口分级合理性：3 项 P2 定级（测试覆盖/cosmetic 文档）上限正确，未欠分级为 P1（算法全实现 + 强测试）。
- Non-blocking 观察（非缺陷）：RC ID 序号空间按 P 级分段（P1-RC-001..005 与 P2-RC-001..005 并行），全 ID 在 arm-index 无歧义；A1.5 沿用 A1.4 先例。方法论 §7 序号约定可在未来澄清，不阻塞本报告闭环。

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及成本过账逻辑（FIFO/到岸成本/成本调整凭证）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）
