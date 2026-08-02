# 2026-08-02-2042-2 rc-ma1-a1-8-mfg-f1-mrp-drp-engine mfg-F1 MRP/DRP 引擎需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.8（MA1 需求追踪矩阵审计 — mfg-F1 MRP/DRP 引擎：UC-MFG-05 工单审核触发物料预留 + UC-MFG-08 工单取消/完工释放预留）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.8
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.8 的 0.2 依赖）、`2026-08-02-2042-1-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（A1.7，同批次同范式）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.8 给出 UC 清单 = `UC-MFG-05/08`（2 UC），含 `use-cases.md:90` / `:144` 锚点（inventory :342 确认一致）。

- **L1 需求契约（权威真相源）**：`docs/design/manufacturing/use-cases.md`（**注意**：roadmap 切片标签为"MRP/DRP 引擎"，但 A1.8 权威 UC 范围 = UC-MFG-05/08，即工单审核触发/释放物料预留，而非 MRP 净需求计算本身；MRP 引擎状态机/代码质量作为 L3 上下文 + A2.6b/A4.2b 行为复用输入，本切片裁决焦点 = 预留写路径符合性）：
  - UC-MFG-05 工单审核触发物料预留（`:90`）：工单审核通过 → BOM 展开 → 创建 MaterialReservation（每个子件一条）；执行预留：预留量 = min(需求量, 可用量)；库存余额.预留量 += 预留量；工单.reservationStatus = RESERVED（或 PARTIAL_RESERVED）。
  - UC-MFG-08 工单取消/完工释放预留（`:144`）：工单.CANCELLED 或 COMPLETED → 释放未领料的预留（reservedQty - pickedQty）；库存余额.预留量 -= 释放量；MaterialReservation.状态 = RELEASED。

- **L2 owner doc 设计参考（关键：整节 Deferred）**：`docs/design/manufacturing/material-reservation.md`。**文首 Deferred 实现说明（`:9-16`）明确**：完整物料预留子系统（`ErpMfgMaterialReservation` 实体 + 工单 `reservationStatus` 6 态 + 审核触发预留 / 领料扣减 / 预留释放写路径 + 5 个 `erp-mfg.reservation-*` config key）当前**均未落地**，属**设计意图（Deferred）**。已核实事实（执行时复核 HEAD 仍成立）：
  - **持久化真相源** = 库存域 `ErpInvReservation`/`ErpInvReservationLine`（`module-inventory/model/app-erp-inventory.orm.xml`），**非**制造域独立 `ErpMfgMaterialReservation`（该实体未物化，仅 `ErpMfgDashboardBizModel` 代码注释 Non-Goal 引用）。
  - **当前实现边界**：制造域仅 `KitAvailabilityChecker` 做**只读齐套校验**（读 `ErpInvStockBalance.availableQuantity` 决定 `STOCK_RESERVED`/`STOCK_PARTIAL`），**不写预留**；实际扣减由开工后领料出库移动单 DONE 完成。
  - **未落地工件**：`ErpMfgWorkOrder` 无 `reservationStatus` 字段；`ErpMfgConstants` 无 `erp-mfg.reservation-*` config key。
  - **Successor 触发条件**：完整预留写路径须库存域 `ErpInvReservation*` 写接口先行落地后，再于制造域接线（领料扣减/释放经 `IErpInvReservationBiz`）。
  - `mrp.md`（§MRP 流程 `:13` / §关键业务规则 `:54`：需求时界/提前期偏移/低层编码/Pegging 追溯/MRP 范围 / §建议单释放 `:62`：MANUFACTURING→工单 / PURCHASE→采购订单 / 释放后 isFirmed）。

- **L3 代码实现现状（执行时实测核验）**：
  - **MRP 引擎（已落地）**：`module-manufacturing/erp-mfg-service/.../mrp/MrpEngine.java`（净需求/低层码递归）+ `mrp/MrpReleaseService.java`（采购/委外/工单三路径释放，`markFirmed` + `advancePlanToFirmedIfComplete`）+ `mrp/DemandAggregator.java`（销售/安全库存/预测需求整合）+ `entity/ErpMfgMrpPlanBizModel.java`（`runMrp:29` @BizMutation 事务包裹，**注意 R6.2 per-mutation 拆分致行号偏移，执行时按逻辑复验**）+ `processor/ErpMfgMrpPlanRunMrpProcessor.java`。
  - **物料预留写路径（UC-MFG-05/08 核心 — Deferred）**：制造域**无**审核触发预留的写实现（`ErpMfgWorkOrderProcessor` 无预留创建调用，执行时 grep 核验）；预留实体在库存域 `module-inventory/erp-inv-service/.../entity/ErpInvReservationBizModel.java` + `ErpInvReservationLineBizModel.java`（写接口存在性执行时核验）；齐套校验 = `KitAvailabilityChecker` 只读。**这是本切片核心裁决点**：UC-MFG-05/08 字面要求"审核触发预留写 + 取消/完工释放预留写"在制造域**未实现**（owner doc 已标 Deferred），按 §4 Q1 以 L1 为准 + §5 Q4（P0/P1 必须实现、禁止方案 B 无例外）→ 候选 P1（会计/数据安全类无例外，但预留量属库存可用量管控，需 §2 判据精确定级）。

- **L4 测试证据现状**：`TestErpMfgMrpEngine`、`TestErpMfgMrpEndToEnd`、`TestErpMfgMrpSimulation`（MRP 引擎/释放/仿真）。**预留写路径无测试**（功能 Deferred）。执行时核验：齐套校验只读路径是否有断言（`KitAvailabilityChecker` STOCK_RESERVED/STOCK_PARTIAL 决定）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（A2.6b）= MRP 计划规划状态机审计**：MRP 计划头 5 态 + 预测 4 态 + 建议单隐式生命周期（isFirmed）+ BOM isActive + 仿真 4 态。Verdict **pass**（零 P0、3 P1：P1-MA2-036 CANCELLED 不可达死状态 / P1-MA2-037 建议单释放 / P1-MA2-038 委外 config-gated 绕审批）。事务边界覆盖 MRP 运算 + 释放原子性确认。
  - **`docs/audits/2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md`（A4.2b）= MRP 代码质量审计**：MrpEngine/MrpReleaseService/DemandAggregator/CostRollupService/ProductionVarianceCalculator/BatchGenealogy/C RP。
  - **注意**：A2.6b/A4.2b 覆盖 **MRP 引擎状态机/代码质量**，但 **UC-MFG-05/08 的物料预留写路径是需求契约维度**——既有审计将其归为 owner-doc drift（A3.4）+ Deferred，**未从"需求↔实现符合性"视角裁决**。本切片正是补此差异：按 L1 字面 + Q4 修复义务裁决预留写路径未实现的定级。
  - **本切片须声明与上述 MA2/MA4 报告的差异增量**（报告段落 9）：复用 A2.6b 已证实的 MRP 运算/释放事务原子性 + 建议单生命周期，只补需求视角差异（UC-MFG-05/08 预留写路径 Deferred 的符合性裁决）。

- **arm-index 既有 finding 衔接**：MRP 相关——`P1-MA2-036`（MRP CANCELLED 不可达）/`P1-MA2-037`（建议单释放）/`P1-MA2-038`（委外 config-gated 绕审批）。**物料预留 Deferred** 可能已在 owner-doc drift（A3.4）或 successor（MA3 A3.2"物料预留实现"）登记——执行时 grep `arm-index.md` + `docs/backlog/README.md` 核验既有追踪后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；**预留写路径修复触及跨域（库存域 ErpInvReservation* 写接口 + 制造域接线），且可能触及 ORM 结构（reservationStatus 字段）**，属 ask-first 保护区域（§5 保护区域暂停协议）。

- **剩余差距**：A1.8 切片的五级追踪审计报告缺失 = MA4（A4.2 扩展域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.8 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.8 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`，含方法论 §6 **9 段全部内容**：①UC-MFG-05/08 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 MrpEngine/MrpReleaseService 调用链 + KitAvailabilityChecker 只读齐套 + 库存域 ErpInvReservation* 预留实体位置）③测试证据（注明断言强度）④运行时行为证据（复用 A2.6b/A4.2b，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2/MA4 报告差异增量声明。
- 对 2 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-MFG-05（4 断言：BOM 展开创建 MaterialReservation / 预留量=min(需求,可用) / 库存余额.预留量+= / reservationStatus=RESERVED）+ UC-MFG-08（3 断言：CANCELLED/COMPLETED 释放未领料预留 / 库存余额.预留量-= / MaterialReservation 状态=RELEASED），各一矩阵行。
- 对**核心裁决点（物料预留写路径 Deferred）**给出分级结论：UC-MFG-05/08 字面"审核触发预留写 + 释放预留写"在制造域未实现（owner doc Deferred，仅 KitAvailabilityChecker 只读齐套）——按 §4 Q1（L1 为准）+ §5 Q4（P0/P1 必须实现、禁止方案 B 无例外）+ §2 判据精确定级。须显式论证：预留量管控属"库存可用量/数据安全"还是"业务便利"，是否命中 Q4"会计/数据安全类强制实现无例外"；并裁决该 Deferred 是否已有合法人工批准记录（owner doc 自标 Deferred 非 AI 人工批准证据标准，见 MA2 §"显式人工批准记录"）。若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / mfg use-cases / material-reservation.md / mrp.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.1-A1.7 done/draft；A1.9 mfg-F2 工单与报工 = 独立切片独立 plan；A1.8 只覆盖 UC-MFG-05/08 即 MRP/预留触发释放维度，工单主链归 A1.9）。
- **不重跑既有状态机/代码质量行为审计**（§去重协议：A2.6b/A4.2b 已证实 MRP 运算/释放事务原子性 + 建议单生命周期，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.8 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.8 UC 锚点）+ `docs/design/manufacturing/use-cases.md`（L1 真相源）+ `docs/design/manufacturing/material-reservation.md` + `docs/design/manufacturing/mrp.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md` + `docs/backlog/README.md`（finding/successor 衔接）+ A2.6b/A4.2b 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A2.6b/A4.2b 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgMrpEngine,TestErpMfgMrpEndToEnd,TestErpMfgMrpSimulation`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + 预留写路径 Deferred 裁决 + resolved finding HEAD 复核

Status: completed
Targets: `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（已新建，§1-§5 已落盘；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [x] `Proof` 对 UC-MFG-05/08 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:90/:144` 验收标准原文（禁止转述）；L2 引用 `material-reservation.md`（文首 Deferred 说明 `:9-16` / §预留流程 `:71` / §预留释放 `:184`，标注"设计参考且整节 Deferred，冲突以 L1 为准"）+ `mrp.md`（§建议单释放 `:62`）；L3 引用 `module-manufacturing/erp-mfg-service/.../mrp/MrpEngine.java:line` + `MrpReleaseService.java:line` + `ErpMfgMrpPlanBizModel.java:line`（runMrp）+ `KitAvailabilityChecker` 只读齐套 + 库存域 `ErpInvReservationBizModel.java`（预留实体位置，跨域标注）；L4 引用 `Test*.java#method`（注明断言强度 + 预留写路径无测试）；L5 复用 A2.6b/A4.2b + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**核心裁决点（物料预留写路径）**（逐条验收标准对照）：UC-MFG-05——①BOM 展开创建 MaterialReservation（制造域无实现，KitAvailabilityChecker 只读）；②预留量=min(需求,可用)（无写）；③库存余额.预留量+=（无写，预留量字段在 ErpInvStockBalance）；④reservationStatus=RESERVED（ErpMfgWorkOrder 无该字段，docStatus STOCK_RESERVED/STOCK_PARTIAL 承载）；UC-MFG-08——⑤CANCELLED/COMPLETED 释放未领料预留（无释放写实现）；⑥库存余额.预留量-=（无写）；⑦MaterialReservation 状态=RELEASED（无写）。逐条记录"未实现/部分实现/已实现"。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` **resolved finding + Deferred 追踪 HEAD 复核**：① arm-index + `docs/backlog/README.md` grep "物料预留/reservation/ErpInvReservation" 核验预留 Deferred 是否已有 successor 追踪（如 MA3 A3.2）+ 是否有合法人工批准记录（owner doc 自标 Deferred 不算人工批准，见 MA2 §证据标准）；② A2.6b P1-MA2-036/037/038 在当前 HEAD 实际落地（按逻辑非行号核验）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：**核心**——预留写路径未实现的定级。须显式论证：(a) 预留量管控是否属 §2 "数据安全/会计正确性"强制类（命中 Q4 无例外）vs "业务便利/优化"类（可 P2 登记）；(b) 当前只读齐套 + 领料移动单 DONE 扣减是否构成"功能等价"（L1 字面要求预留独立追踪，不等价）；(c) Deferred 是否有合法人工批准（裁决是否需 Q4 重开）。MRP 引擎本身（runMrp/释放/事务原子性）复用 A2.6b pass 结论。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-MFG-05/08 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.6b/A4.2b 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；预留写路径 Deferred 裁决含 Q4 显式论证（非悬空"待查"）；resolved finding + Deferred 追踪 HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（§6-§9 已落盘，报告定稿）；`docs/audits/arm-index.md`（新 RC finding P1-RC-008 入分区 + 3 交叉引用注记）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` mfg 预留/MRP 同域同控制点（如 P1-MA2-036/037/038、A3.4 owner-doc drift、MA3 A3.2 successor 行）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点 → 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：预留 Deferred 若 arm-index/backlog 已有 successor 追踪则复用并追加 RC 交叉引用（标注 Q4 重开裁决）。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如预留量并发扣减运行时行为、只读齐套在 STOCK_PARTIAL 强制开工后领料可用量校验运行时；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2/MA4 报告差异增量声明：声明复用 A2.6b（MRP 计划头/建议单/预测/仿真状态机 + 事务原子性）+ A4.2b（MRP 代码质量）已证实结论，列明本切片只补的需求视角差异（UC-MFG-05/08 预留写路径 Deferred 的符合性裁决 + Q4 重开论证）。
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

- Independent draft review iteration 1: `acceptable as-is`（独立子代理 `ses_03d4f1becffeGSyHogCRyQ0233`，fresh session，未起草本计划）。逐项实测核验全 PASS：roadmap 对齐（A1.8 / UC-MFG-05/08 / Deps=0.2 done / Skill）、UC 锚点 :90/:144 匹配、断言计数（UC-MFG-05×4 + UC-MFG-08×3 = 7）、L3 路径全集存在（MrpEngine/MrpReleaseService/DemandAggregator/ErpMfgMrpPlanBizModel runMrp/ErpMfgMrpPlanRunMrpProcessor/ErpInvReservationBizModel/ErpInvReservationLineBizModel/KitAvailabilityChecker）、**核心 Deferred 主张实测确认**（material-reservation.md:9-16 Deferred 声明 + reservationStatus 全 module-manufacturing rg 零命中 + ErpMfgWorkOrderProcessor 无预留写 + ErpMfgMaterialReservation 未物化）、L2/L4/L5 存在、核心裁决框架（预留写 Deferred 按 Q4 + §193"owner doc 自圆不能闭 P0/P1"）正确、arm-index 已追踪 P1-MA3-042(R2.6)/P1-MA5-006(successor) 且 plan 正确推迟复用裁决至执行、保护区域 ask-first 正确。无阻塞 issue。Non-blocking（已吸收）：①runMrp 行号 `:36`→**已修正**为 `:29` + 加 R6.2 行号偏移 caveat；②加"MRP/DRP 引擎为 roadmap 标签、权威 UC 范围=预留触发/释放"scope 说明。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + 预留写路径 Deferred 裁决 + finding arm-index 衔接 + resolved finding HEAD 复核 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.8 报告 9 段齐全 + UC-MFG-05/08 逐矩阵行 + 预留写路径 Deferred Q4 裁决 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.8 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Closure

Status Note: 本计划为只读需求-实现符合性审计（verification or audit work），结果表面 = 一份 9 段审计报告 + arm-index finding 登记。两个 Phase 均 completed，全部执行项目与 Exit Criteria 已勾选 [x]，Closure Gates 8/8 全勾。报告产出 `docs/audits/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（9 段齐全）+ `docs/audits/arm-index.md` 新增 A1.8 清单行 + P1-RC-008 finding 行 + A1.8 summary 注记（含 P1-MA3-042/P1-MA5-006/A3.2 三向交叉引用 + P1-MA2-036/037/038 HEAD `5953f07c1` 复核 3/3 resolved）。无 P0 → 不触发 MR0；P1-RC-008 由 MR1 R1.0 展开（触及 ORM 结构变更 + 跨域写 + 库存可用量管控，须 ask-first + 独立 plan-audit；successor 触发条件对齐 A3.2），3 项静态存疑点交 MA4 A4.1 运行时展开。MRP 引擎本身复用 A2.6b pass 结论 = 接受（§去重协议，不重审）。finding 的修复实施归类为 out-of-scope（本审计不实施修复），不阻塞本审计闭环。

Closure 证据（独立结束审计可复核）：
- **结构完整性**：报告 9 段齐全（§1 需求契约 L1 逐字 + §2 实现 L3 行号 + §3 测试 L4 断言强度 + §4 运行时 L5 复用 + §5 矩阵/结论/Q4 裁决 + §6 arm-index 衔接 + §7 静态存疑点 + §8 自检 + §9 差异增量）；Front matter `Plan Status: completed` / 2 Phase 均 `Status: completed` / Phase Exit Criteria 全 [x] / Closure Gates 8/8 [x] 一致。
- **裁决完整性**：UC-MFG-05 = P1（§2 P1①+⑤）+ UC-MFG-08 = P1（§2 P1①+⑤），Q4 三论证（a 数据安全/会计类裁决非 P0 + b 功能不等价 + c Deferred 无合法人工批准→重开）非悬空；resolved finding HEAD 复核 3/3（P1-MA2-036/037/038 实测落地）。
- **过程纪律**：报告 §8 含 `nop-compliance-checker.sh` actual vs baseline 实测表（全 19 规则 actual==baseline，零裸漂移，HEAD `5953f07c1`）；本审计为只读（零代码/ORM/api.xml/view.xml/真相源变更），Closure Gates 显式删除 build/test/lint/typecheck 门控合规（adjudicated gate set）；独立草案审查 1 轮 acceptable-as-is 已记录（`ses_03d4f1becffeGSyHogCRyQ0233`）。
- **文本一致性五点**：Plan Status completed ↔ 2 Phase Status completed ↔ Phase Exit Criteria 全 [x] ↔ Closure Gates 8/8 [x] ↔ Closure evidence 齐全（报告 + arm-index 双向可追溯）。

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；**预留写路径修复触及跨域（库存域 ErpInvReservation* 写接口 + 制造域接线）+ 可能触及 ORM 结构（reservationStatus 字段）**，属 ask-first 保护区域 + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）
