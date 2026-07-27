# 2026-07-28-0109-2-audit-remediation-ma2-mfg-mrp-bom-state-machine MA2 manufacturing 状态机审查 — MRP/BOM（A2.6b）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A2.6b manufacturing 状态机审查 — MRP/BOM（S 级拆分 2/2）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.6b）
> Related: `docs/plans/2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a 工单与报工，S 级拆分 1/2，先执行——生产执行消费 MRP 释放的工单）；`docs/plans/2026-07-27-2315-1-audit-remediation-ma2-finance-period-budget-state-machine.md`（A2.5b finance 预算方案状态机范式，MRP 计划状态机与预算方案状态机同型——DRAFT→批准类终态 + 结转校验前置）；`docs/plans/2026-07-22-1000-2-manufacturing-mrp-drp-simulation-engine.md`（仿真引擎 owner doc 化——fork 而非触及单次 MRP 路径）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/manufacturing/mrp.md`（MRP 流程/建议单释放/实现偏离补注）+`bom-and-routing.md`（BOM/工艺）+`simulation-engine.md`（仿真状态机）+`state-machine.md`（owner doc）
> Audit: required

## Current Baseline

manufacturing（制造）域 S 级状态机审查拆分 2 片：**A2.6a = 生产执行类状态机**（done 后执行本计划）；**A2.6b = 计划规划类状态机**（MRP 计划头 / 运营预测 / 建议单释放生命周期 / BOM 激活）。本审计聚焦**物料需求计划的规划生命周期**——与 finance A2.5b 预算方案状态机同型（DRAFT→运算完成→转工单确认 + 结转/释放前置校验）。

实时仓库已落地的计划规划状态机实现（逐项核实，路径 `module-manufacturing/`）：

- **MRP 计划头状态机**（`ErpMfgMrpPlan`，ORM `app-erp-manufacturing.orm.xml:768-804`）：列 `status` dict `erp-mfg/mrp-status`（**5 态**：DRAFT/RUNNING/COMPLETED/FIRMED/CANCELLED，dict `orm.xml:72-78`）。**无 approveStatus**（审批并入单一 status 枚举）。
  - 迁移实现（`MrpEngine.java` 277 行，非 BizModel helper）：`runMrp:77` 入口守卫需 DRAFT/null（`MrpEngine.java:79-83` 抛 `ERR_MRP_INVALID_PLAN_STATUS`）→ RUNNING(:84) → COMPLETED(:98)。→ FIRMED **非 MrpEngine 设置**，由 `MrpReleaseService.advancePlanToFirmedIfComplete:218-236` 在**所有行 isFirmed=true** 时置 FIRMED(:233)。**CANCELLED 永不被代码写入**（无 `cancelMrp` mutation，dict 项悬空）。
  - 引擎逻辑：`clearLines:243` 清旧计划行 → top-down 递归 `processMaterial:102` 写新 `ErpMfgMrpPlanLine`（isFirmed=FALSE:140）→ orderType 决策(:122-124：有 default+active BOM → WORK_ORDER_REQUEST，否则 PURCHASE_REQUEST；**SUBCONTRACT_REQUEST 引擎永不产生** `MrpEngine.java:48-50` Non-Goals）。
- **运营预测状态机**（`ErpMfgForecast`，ORM `orm.xml:896-928`）：列 `status` dict `erp-mfg/forecast-status`（**4 态**：DRAFT/APPROVED/CONSUMED/CANCELLED，dict `orm.xml:126-131`）。**无独立 approveStatus**（APPROVED 是 status 值）。`ErpMfgForecastLine`（ORM:931-973）**无 status 列**（生命周期在头）。
  - 迁移实现（`ErpMfgForecastBizModel.java` 66 行）：`approve:34`(DRAFT→APPROVED，需 DRAFT，否则 `ERR_FORECAST_ILLEGAL_STATUS_TRANSITION`) / `cancel:50`(DRAFT|APPROVED→CANCELLED，拒绝已 CANCELLED 或 CONSUMED)。**无 submit/reject/consume 方法**。
  - **`CONSUMED` 是死 dict 值**——dict 定义 + `ErpMfgConstants.FORECAST_STATUS_CONSUMED` 存在，但**永不被代码写入**：`DemandAggregator.collectForecastDemands:160-230` 只读 status=APPROVED 预测（filter:175），**不回写 CONSUMED**。`ErpMfgForecastBizModel:21` javadoc 明示"CONSUMED 已预留但本期不自动迁移"（owner doc `mrp.md §实现偏离补注` 已文档化 Deferred）。
- **建议单（计划行）生命周期**（`ErpMfgMrpPlanLine`，ORM `orm.xml:807-853`）：**无 status 列**。生命周期由 `orderType`（dict `erp-mfg/mrp-order-type`：PLANNED_ORDER/PURCHASE_REQUEST/WORK_ORDER_REQUEST/SUBCONTRACT_REQUEST，`orm.xml:79-84`）+ `isFirmed` 布尔（默认 false，`orm.xml:824`，**事实上的 released 标记**）+ `convertedBillCode`(:825，生成的目标单号 PO-MRP-/WO-MRP-/SUB-MRP-)承载。**无 RELEASED 状态值**——释放=翻转 isFirmed=true + 填 convertedBillCode。
  - 释放实现（`MrpReleaseService.java` 277 行）：`releasePurchaseRequest:67`(需 PURCHASE_REQUEST) / `releaseWorkRequest:83`(需 WORK_ORDER_REQUEST) / `releaseSubcontractRequest:98`(需 SUBCONTRACT_REQUEST，config-gated `erp-mfg.subcontract-release-enabled` 默认 false，off 时抛 `ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE`)。共享幂等守卫 `requireReleasable:115`（拒已 firmed 行 `ERR_MRP_LINE_ALREADY_FIRMED` + 拒 orderType 不匹配）。释放后 `markFirmed:129-133`(isFirmed=true + convertedBillCode) + `advancePlanToFirmedIfComplete` 头级回写。
- **需求单**（`ErpMfgMrpDemand`，ORM `orm.xml:856-893`）：**无 status 列**。`demandSource`(dict `erp-mfg/mrp-demand-source`：SALES_ORDER/FORECAST/SAFETY_STOCK/MANUAL)是**来源分类**非生命周期状态。合成需求（非 MANUAL）每次 `DemandAggregator.aggregate` 运行时删除重建（`clearSynthesized:257`）；MANUAL 行保留。`ErpMfgMrpDemandBizModel` 是纯 CrudBizModel 无自定义方法。
- **BOM**（`ErpMfgBom`，ORM `orm.xml:193-230`）：**无状态机**——`isActive` 布尔(:202，默认 true)+`isDefault`(:203)，无 status/docStatus/approveStatus 列。`ErpMfgBomOperation`(:291-327) 也无 status 列。`ErpMfgBomBizModel.java`(69 行)无 approve/activate/deactivate 方法（仅 findDefaultBom/explode/rollupCost），激活/停用仅经通用 CRUD update isActive。
- **仿真引擎**（`ErpMfgMrpScenario`/`ErpMfgMrpScenarioVersion`/`ErpMfgMrpScenarioParam`，ORM:1534/1572/1609）：config-gated `erp-mfg.simulation-enabled`（默认 false）。`SimulationMrpEngine.java`(513 行)是 **fork 而非 caller**——不调 `MrpEngine.runMrp`/`MrpReleaseService`，重实现算法读场景覆盖值。操作自己的 `ErpMfgMrpPlan` 实例（新 DRAFT plan），`promoteToFormalPlan` 创建新 DRAFT plan 不自动释放。仿真 status（simulation-status.dict.yaml：DRAFT/RUNNING/COMPLETED/ARCHIVED）独立。**单次 MRP 路径零触及**（owner doc `simulation-engine.md` + javadoc 声明）。
- **跨域访问**（计划/释放路径）：**唯一持久化他域实体的是 `MrpReleaseService`**——`daoFor(ErpPurOrder):137`+`daoFor(ErpPurOrderLine):152`（写采购单骨架 saveEntity:150/161，设 DRAFT/UNSUBMITTED）+ `daoFor(ErpMfgSubcontractOrder):187`（写委外单 APPROVED 直接绕审批管道:199-201）。`MrpReleaseService:42-53/148-149` javadoc 明示这是 **O-4 架构豁免**（目标 I*Biz 仅通用 CRUD save(Map) 无法通过必填校验），引用 `docs/architecture/posting-exemptions.md §MrpReleaseService`。`MrpEngine`/`DemandAggregator` 跨域读 ErpInvStockBalance/ErpMdMaterial/ErpSalOrder 是只读（P1-MA1-022 同型，已登记 MR1）。
- **测试覆盖**：`TestErpMfgMrpEngine`（引擎运算/净需求/批量/提前期）+`TestErpMfgMrpEndToEnd`（聚合→引擎→释放）+`TestErpMfgMrpSimulation`（仿真引擎/场景/版本/promote）+`TestErpMfgForecastCrudSmoke`+`TestErpMfgForecastSource`（预测作为需求源被消费）+`TestErpMfgBomExplosion`（BOM 多级展开）。**无独立建议单释放测试类**（覆盖在 MrpEndToEnd 内）+**无 BOM 状态机测试**（BOM 无状态机）。

**已登记的直指计划规划状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-022`（todo MR1，9 域）：`MrpReleaseService` 跨域写 ErpPurOrder/ErpMfgSubcontractOrder + `MrpEngine`/`DemandAggregator` 跨域只读 ErpInv/ErpMd/ErpSal。**状态机 scope**：释放路径跨域写是状态迁移的副作用（markFirmed + 生成目标单），本审计复核其在异常路径（目标单生成失败）是否引入建议单与目标单悬挂半状态。
- `P1-MA1-029`（todo MR1，contract）：`ErpCtInvoicePlanBizModel` 跨域写半治理。**状态机 scope**：与 MRP 释放同型（O-4 豁免跨域持久化目标域实体绕审批管道）——本审计复核 MrpReleaseService 跨域写委外单 APPROVED 绕审批是否应登记豁免（与 P1-MA1-029 同型裁决）。

**但从未做过一次覆盖计划规划状态机（MRP 计划头 + 运营预测 + 建议单释放生命周期 + BOM 激活）、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：MRP 计划 5 态——CANCELLED dict 有但无 writer（死 dict 值，同 finance A2.5c CANCELLED 同型）；预测 4 态——CONSUMED 是死 dict 值（owner doc Deferred，需确认是否破坏状态机）；FIRMED 是"动作完成"还是"等待"（所有行释放后头置 FIRMED——是终态还是可再运算）；建议单无 status 列（生命周期用 isFirmed 布尔——是否应建模为状态）。
- **转换完整性**：MRP 计划 DRAFT→RUNNING→COMPLETED（MrpEngine）/ COMPLETED→FIRMED（MrpReleaseService 头级回写）/ **CANCELLED 无写入路径**（迁移缺失）；预测 DRAFT→APPROVED / DRAFT|APPROVED→CANCELLED（**无 SUBMITTED 审核中间态 + 无 CONSUMED 写入**）；建议单 isFirmed false→true（释放）/ 已 firmed 行拒绝重复释放（幂等）；BOM isActive 翻转（无状态机，仅 CRUD——是否够）。
- **终端状态与恢复**：MRP FIRMED/CANCELLED 终态（FIRMED 可再运算回 DRAFT？——无路径）；预测 APPROVED→CONSUMED（死状态——终态但不可达）/ CANCELLED 终态；已 firmed 建议单是否可取消释放（无 unfirmed 路径——释放不可逆？）；仿真 ARCHIVED 版本终态。
- **异常路径**：MRP 运算失败（RUNNING 中途异常→状态是否回滚 DRAFT 还是悬挂 RUNNING——**重点**）；建议单 orderType 不匹配释放（拒绝）；已 firmed 行重复释放（`ERR_MRP_LINE_ALREADY_FIRMED` 幂等）；SUBCONTRACT_REQUEST 释放 config-gated off 时拒绝；**释放路径生成目标单失败时建议单与目标单一致性**（MrpReleaseService 跨域 saveEntity 失败→事务回滚是否覆盖 isFirmed + 目标单——@BizMutation 事务边界评估）；仿真 promote 失败回滚。
- **可达性**：**MRP CANCELLED 是否可达**（无 writer → dict 项死状态）；**预测 CONSUMED 是否可达**（无 writer → dict 项死状态）；MRP 从 DRAFT 到 FIRMED/CANCELLED 可达性；预测从 DRAFT 到 APPROVED/CONSUMED/CANCELLED 可达性；建议单 isFirmed true 后是否有回退。
- **角色与权限**：MRP 运算（计划员）/ 预测 approve（计划主管？）/ 建议单释放（采购员/计划员，config-gated 委外）；危险操作（释放生成采购单/工单骨架影响下游——单价/金额=0 须补录；释放生成委外单 APPROVED 绕审批）；多角色冲突（计划员释放 vs 采购员补录）。
- **外部依赖**：建议单释放跨域写 ErpPurOrder/ErpMfgSubcontractOrder（O-4 豁免，绕 IErpPurOrderBiz 审批管道——**与 P1-MA1-029 同型半治理**，本审计复核豁免登记完整性）；需求聚合跨域读 ErpSalOrder/ErpInvStockBalance/ErpMdMaterial（只读，P1-MA1-022）；APS 排程来源建卡（仿真 promote 后单次释放路径）；CRM 金额预测 vs 运营数量预测 disaggregation 未实现（owner doc Deferred）；外部步骤失败是否阻断状态迁移。
- **TODO/任务策略**：MRP COMPLETED 状态是否产生"待释放建议单"待办（计划员决策释放）；STOCK_PARTIAL 类缺料建议是否产生 TODO；CONSUMED 预测回写缺失是否导致预测长期 APPROVED 静默下沉（重复消费？）；是否存在期望有人行动但不产生待办的状态（长期 COMPLETED 未释放计划——建议单滞留）。
- **场景演练**：(a) MRP 快乐路径（DRAFT→聚合需求→RUNNING→COMPLETED→释放采购建议→释放工单建议→全 firmed→FIRMED）；(b) 部分释放（仅部分行 firmed，头保持 COMPLETED）；(c) MRP 运算失败回滚（RUNNING 中途异常→状态一致性）；(d) 已 firmed 行重复释放拒绝（幂等）；(e) 预测生命周期（DRAFT→APPROVED→被 DemandAggregator 消费→CONSUMED 未回写漂移）；(f) 预测 cancel（APPROVED→CANCELLED）；(g) SUBCONTRACT_REQUEST 释放 config-gated（off 拒绝 / on 生成 APPROVED 委外单绕审批）；(h) 仿真 promote（场景版本→新 DRAFT plan→ARCHIVED，单次路径零触及）；(i) 并发释放同建议单（isFirmed 无 @Version——双读双写？交接 A2.17）。
- **与设计文档一致性**：`mrp.md` MRP 流程/建议单释放 vs 实现——**重点漂移**：(1) MRP CANCELLED dict 有但无 writer（owner doc 未注记——漂移）；(2) 预测 CONSUMED 死状态（owner doc `§实现偏离补注`已注记 Deferred，但需确认状态机章节是否声明）；(3) `mrp.md §建议单释放`描述"释放后建议单状态标记为 RELEASED" vs 实现无 RELEASED 状态值用 isFirmed 布尔（owner doc 文字 vs 实现偏离）；(4) `mrp.md §lot sizing`物料级 fixedLotSize/minOrderQty/maxOrderQty 列不存在（已注记 Deferred）；(5) 可用量在途/在制未实时跨域汇总（已注记简化）；(6) BOM 无状态机（owner doc 未声明 BOM 状态机——一致）；(7) 建议单释放生成采购单单价/金额=0（owner doc `§实现偏离补注`已注记残留）。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（**MRP 运算 RUNNING 中途异常状态悬挂** [状态未回滚 DRAFT] / **释放路径生成目标单失败致建议单 isFirmed 与目标单悬挂半状态** [事务回滚缺口] / **SUBCONTRACT_REQUEST 释放 config-gated on 时生成 APPROVED 委外单绕审批** [若破坏业务规则]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **MRP 计划头状态机**（5 态）+ **运营预测状态机**（4 态）+ **建议单释放生命周期**（isFirmed 布尔 + convertedBillCode，无 status 列的隐式状态机）+ **BOM 激活**（isActive 布尔，无状态机）+ **仿真状态机**（config-gated，独立 status）做系统性业务审查，产出审计报告。**严格限定 A2.6b scope = 计划规划类状态机**；生产执行类（工单/作业卡/领料/委外）归 A2.6a。
- 重点核验已识别控制点：(1) 状态定义清晰性（MRP CANCELLED 死状态 / 预测 CONSUMED 死状态 / FIRMED 终态归属 / 建议单无 status 用布尔）；(2) 转换完整性（**MRP CANCELLED 无写入路径** / 预测无 CONSUMED 写入 / 建议单 isFirmed 翻转 + 头级回写 / BOM isActive）；(3) 终端与恢复（FIRMED/CANCELLED 终态 / 预测 CONSUMED 不可达终态 / 已 firmed 建议单不可逆）；(4) 异常路径（**MRP 运算失败状态回滚** / orderType 不匹配 / 幂等重复释放 / SUBCONTRACT config-gated / **释放生成目标单失败事务一致性**）；(5) 可达性（**MRP CANCELLED / 预测 CONSUMED 是否可达**）；(6) 角色权限（释放生成骨架影响下游 / 委外绕审批危险操作）；(7) 外部依赖（**释放跨域写 ErpPurOrder/ErpMfgSubcontractOrder 绕审批管道——O-4 豁免登记与 P1-MA1-029 同型裁决** / 需求聚合跨域读 / CRM disaggregation Deferred）；(8) TODO 任务策略（COMPLETED 待释放建议单 TODO / 长期未释放计划滞留）；(9) 场景演练（9 个代表性场景含仿真 promote）。
- 复核已登记 finding 在计划规划状态机运行时的行为影响：P1-MA1-022（释放/聚合跨域访问——异常路径悬挂评估）/ P1-MA1-029（MrpReleaseService 跨域写委外单 APPROVED 绕审批——O-4 豁免同型裁决，确认豁免登记完整性），标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
- scope matrix §2.x manufacturing/计划规划状态机 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.6b 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.6a 生产执行类状态机（工单/作业卡/领料/委外） — 那是 S 级拆分 1/2；本审计只确认建议单释放生成的工单/委外单骨架进入目标域后状态正确（DRAFT 工单骨架 / APPROVED 委外单绕审批——后者在 scope 内复核）。
- **不**审计 MRP 运算正确性（净需求/批量/提前期/BOM 展开算法） — 那是业务逻辑正确性，归 A4.2b 代码质量/业务逻辑审计；本审计只做**状态机生命周期**审查（status 迁移/isFirmed 翻转/释放事务一致性）。
- **不**审计仿真引擎算法正确性 — config-gated 默认关 + 单次路径零触及；本审计只确认仿真状态机（DRAFT/RUNNING/COMPLETED/ARCHIVED）迁移正确 + promote 不破坏单次路径。
- **不**审计 A2.17 并发与乐观锁 — 并发释放同建议单（isFirmed 无 @Version）归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 config-gated Deferred 偏离是否应实现（预测 CONSUMED 回写 / 物料级 lot sizing 列 / 在途在制实时汇总 / CRM disaggregation / SUBCONTRACT_RELEASE config） — 这些是 owner doc 已裁定的 Deferred/Non-Goal，本审计只确认其 config-gated 在状态机上不引入悬挂（CONSUMED 死状态归状态定义清晰性维度裁决为 P1/P2 dict 死状态清理而非实现）。
- **不**审计 A4.2b manufacturing 代码质量 — MRP 引擎/释放代码质量（异常处理/N+1/索引）系统性审查归 A4.2b；本审计只做状态机业务正确性审查。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/manufacturing/mrp.md`（MRP 流程/建议单释放/实现偏离补注 + CRM vs 运营预测关系 + 仿真引擎关系 — **需复核 §建议单释放"RELEASED"文字 vs 实现 isFirmed 布尔漂移 + CANCELLED/CONSUMED 死状态 owner doc 注记**）；`docs/design/manufacturing/bom-and-routing.md`（BOM/工艺——BOM 无状态机，确认 owner doc 未声明状态机）；`docs/design/manufacturing/simulation-engine.md`（仿真状态机 + 单次路径零触及声明）；`docs/design/manufacturing/state-machine.md`（owner doc——**无 MRP/预测独立状态机章节，散落在 mrp.md §实现偏离补注**，需复核是否应补）；`docs/architecture/posting-exemptions.md`（**MrpReleaseService 跨域写 O-4 豁免登记——与 P1-MA1-029 同型复核**）；`docs/architecture/processor-extension-pattern.md`
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.6b 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：计划规划状态机本身非 ask-first 最高级保护区域，但**建议单释放跨域写 ErpPurOrder/ErpMfgSubcontractOrder 触及 purchase/contract 域实体**（O-4 豁免），且 SUBCONTRACT 释放生成 APPROVED 委外单绕审批管道。P0 即时修复若触及 `MrpReleaseService`/`MrpEngine`/`ErpMfgForecastBizModel`/`ErpMfgMrpPlanBizModel`/`SimulationMrpEngine`，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认。ORM 字典变更（mrp-status/forecast-status/mrp-order-type/mrp-demand-source）属 ask-first。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 计划规划状态机系统性业务审查

Status: planned
Targets: `module-manufacturing/erp-mfg-service/.../service/mrp/MrpEngine.java`（runMrp:77/守卫:79-83/RUNNING:84/COMPLETED:98/clearLines:243/processMaterial:102/orderType 决策:122-124/writeLine:140 + Non-Goals SUBCONTRACT:48-50）；`.../service/mrp/MrpReleaseService.java`（releasePurchaseRequest:67/releaseWorkRequest:83/releaseSubcontractRequest:98/守卫 requireReleasable:115/markFirmed:129-133/advancePlanToFirmedIfComplete:218-236/跨域写 ErpPurOrder:137,150/ErpPurOrderLine:152,161/ErpMfgSubcontractOrder:187,199-201 + O-4 豁免 javadoc:42-53,148-149）；`.../service/mrp/DemandAggregator.java`（aggregate:68/只读跨域 ErpSalOrder:86/ErpMdMaterial:125/ErpInvStockBalance:238 + collectForecastDemands:160-230 filter APPROVED:175 不回写 CONSUMED/clearSynthesized:257）；`.../service/entity/ErpMfgForecastBizModel.java`（approve:34/cancel:50 + CONSUMED Deferred javadoc:21）；`.../service/entity/ErpMfgMrpDemandBizModel.java`（纯 Crud 无自定义）；`.../service/entity/ErpMfgBomBizModel.java`（findDefaultBom/explode/rollupCost，无状态机方法）；`.../service/entity/ErpMfgMrpPlanBizModel.java`（若有）；`.../service/simulation/SimulationMrpEngine.java`（fork 算法:286/runSimulation/promoteToFormalPlan:171/新 DRAFT plan:195/ARCHIVED:226 + 单次路径零触及 javadoc:45-46）+`ErpMfgMrpScenarioBizModel`（requireSimulationEnabled:72-79）；`module-manufacturing/model/app-erp-manufacturing.orm.xml`（mrp-status:72-78/forecast-status:126-131/mrp-order-type:79-84/mrp-demand-source:85-90 + ErpMfgMrpPlan:768-804 status:777/ErpMfgMrpPlanLine:807-853 isFirmed:824 convertedBillCode:825/ErpMfgMrpDemand:856-893/ErpMfgForecast:896-928 status:906/ErpMfgBom:193-230 isActive:202/ErpMfgMrpScenario:1534）；`docs/design/manufacturing/mrp.md`+`bom-and-routing.md`+`simulation-engine.md`+`state-machine.md`+`docs/architecture/posting-exemptions.md §MrpReleaseService`；服务层 `TestErpMfgMrpEngine`+`TestErpMfgMrpEndToEnd`+`TestErpMfgMrpSimulation`+`TestErpMfgForecastCrudSmoke`+`TestErpMfgForecastSource`+`TestErpMfgBomExplosion`
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-022 跨域只读/写 + P1-MA1-029 跨域写半治理已登记待 MR1，本审计复核状态机角度 + O-4 豁免同型裁决）；A2.6a done（生产执行状态机，建议单释放生成的工单/委外单进入目标域后状态正确——本审计复核释放路径）

- [ ] 维度「状态定义」：审查 MRP 计划 5 态语义清晰性——CANCELLED dict 有但无 writer（死状态）/ FIRMED 是"所有行释放完成"动作结果还是等待点；预测 4 态——CONSUMED 死状态（owner doc Deferred）；建议单无 status 列用 isFirmed 布尔（是否应建模为状态——隐式状态机的清晰性）；BOM isActive 布尔无状态机（是否够——BOM 生命周期是否需 approve/activate 显式迁移）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「转换完整性」：列出 MRP 计划每个状态所有传入/传出——DRAFT→RUNNING→COMPLETED（MrpEngine）/ COMPLETED→FIRMED（MrpReleaseService 头级回写）/ **CANCELLED 无写入路径**（迁移缺失——dict 死状态）；预测 DRAFT→APPROVED（approve）/ DRAFT|APPROVED→CANCELLED（cancel）/ **无 SUBMITTED 审核中间态 + 无 CONSUMED 写入**；建议单 isFirmed false→true（释放 markFirmed）/ 已 firmed 拒绝重复（幂等）/ 无 unfirmed 回退；BOM isActive 翻转（仅 CRUD）；仿真 DRAFT→RUNNING→COMPLETED→ARCHIVED（promote）。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「终端状态和恢复」：MRP FIRMED/CANCELLED 终态（FIRMED 可再运算回 DRAFT？——无路径确认终态 / CANCELLED 不可达）；预测 APPROVED→CONSUMED（不可达终态——死状态）/ CANCELLED 终态；已 firmed 建议单是否可取消释放（无 unfirmed——释放不可逆？释放后改主意的回退路径）；仿真 ARCHIVED 版本终态（promote 后不可恢复？）。归档与活动计划是否可区分（status/isFirmed）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「异常路径」：核验全覆盖——**MRP 运算 RUNNING 中途异常→状态是否回滚 DRAFT 还是悬挂 RUNNING**（**重点：@BizMutation 事务边界——MrpEngine 是非 BizModel helper，runMrp 由谁包事务？RUNNING 写入后异常是否回滚 status**）；建议单 orderType 不匹配释放（拒绝）/ 已 firmed 行重复释放（`ERR_MRP_LINE_ALREADY_FIRMED` 幂等）/ SUBCONTRACT_REQUEST 释放 config-gated off 拒绝；**释放路径生成目标单失败时建议单 isFirmed 与目标单一致性**（MrpReleaseService 跨域 saveEntity 失败→@BizMutation 事务回滚是否覆盖 isFirmed + convertedBillCode + 目标单——事务边界评估）；预测 cancel 已 CONSUMED/CANCELLED 拒绝；仿真 promote 失败回滚。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「可达性」：**重点——MRP CANCELLED 是否可达**（无 writer → dict 项死状态，同 finance A2.5c CANCELLED 同型裁决）；**预测 CONSUMED 是否可达**（无 writer → dict 项死状态，owner doc Deferred——裁决是 P1 dict 死状态清理还是 P2 接受）；MRP 从 DRAFT 到 FIRMED/CANCELLED 可达性；预测从 DRAFT 到 APPROVED/CONSUMED/CANCELLED 可达性；建议单 isFirmed true 后回退可达性；是否有死循环或不可达终态路径。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「角色和权限」：每个转换绑定执行角色——MRP 运算（计划员）/ 预测 approve（计划主管）/ 建议单释放（采购员 releasePurchaseRequest / 计划员 releaseWorkRequest / config-gated releaseSubcontractRequest）；危险操作（**释放生成采购单/工单骨架影响下游——单价/金额=0 须补录** / **释放生成 APPROVED 委外单绕审批管道——P1-MA1-029 同型**）；多角色冲突（计划员释放 vs 采购员补录）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「外部依赖」：**重点——建议单释放跨域写 ErpPurOrder/ErpMfgSubcontractOrder 绕 IErpPurOrderBiz 审批管道（O-4 豁免）**：与 P1-MA1-029（ErpCtInvoicePlanBizModel 跨域写半治理）同型裁决——`MrpReleaseService:42-53/148-149` javadoc 已有 bypass rationale，`docs/architecture/posting-exemptions.md §MrpReleaseService` 是否已登记豁免（登记完整性复核）；需求聚合跨域读 ErpSalOrder/ErpInvStockBalance/ErpMdMaterial（只读，P1-MA1-022 已登记）；APS 排程来源建卡（仿真 promote 后单次释放路径，config-gated）；CRM 金额预测 vs 运营数量预测 disaggregation 未实现（owner doc Deferred）；外部步骤失败是否阻断状态迁移（释放生成目标单失败事务回滚）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「TODO/任务策略」：每个非终端状态是否产生正确类型待办——MRP COMPLETED 是否产生"待释放建议单"待办（计划员决策释放采购/工单）；缺料建议（PURCHASE_REQUEST）是否产生采购 TODO；CONSUMED 预测回写缺失是否导致预测长期 APPROVED 静默下沉（DemandAggregator 重复消费同一 APPROVED 预测——每次 MRP 运行都消费，无去重？）；是否存在期望有人行动但不产生待办的状态（长期 COMPLETED 未释放计划——建议单滞留）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) MRP 快乐路径（DRAFT→聚合需求→RUNNING→COMPLETED→释放采购建议 releasePurchaseRequest→释放工单建议 releaseWorkRequest→全行 isFirmed→头 FIRMED）；(b) 部分释放（仅部分行 firmed，头保持 COMPLETED——advancePlanToFirmedIfComplete 检查全 firmed）；(c) **MRP 运算失败回滚**（RUNNING 中途异常→status 一致性——回滚 DRAFT 还是悬挂 RUNNING）；(d) 已 firmed 行重复释放拒绝（幂等 ERR_MRP_LINE_ALREADY_FIRMED）；(e) 预测生命周期（DRAFT→approve→APPROVED→DemandAggregator 消费→**CONSUMED 未回写——预测保持 APPROVED 漂移**）；(f) 预测 cancel（APPROVED→CANCELLED）；(g) **SUBCONTRACT_REQUEST 释放 config-gated**（off 抛 ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE / on 生成 APPROVED 委外单绕审批管道）；(h) 仿真 promote（场景版本 COMPLETED→promoteToFormalPlan→新 DRAFT plan→版本 ARCHIVED，**单次路径零触及**）；(i) 并发释放同建议单（isFirmed 无 @Version——双读双写？交接 A2.17）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「与设计文档一致性」：每个状态/转换在 `mrp.md`/`bom-and-routing.md`/`simulation-engine.md`/`state-machine.md` 是否有匹配——**重点漂移**：(1) MRP CANCELLED dict 有但无 writer（owner doc 未注记死状态——漂移）；(2) 预测 CONSUMED 死状态（owner doc `§实现偏离补注`已注记 Deferred，但状态机章节是否声明——需复核）；(3) `mrp.md §建议单释放`"释放后建议单状态标记为 RELEASED" vs 实现无 RELEASED 状态值用 isFirmed 布尔（owner doc 文字 vs 实现偏离——**重点**）；(4) `mrp.md §lot sizing`物料级 fixedLotSize/minOrderQty/maxOrderQty 列不存在（已注记 Deferred）；(5) 可用量在途/在制未实时跨域汇总（已注记简化）；(6) BOM 无状态机（owner doc 未声明 BOM 状态机——一致）；(7) 建议单释放生成采购单单价/金额=0（owner doc `§实现偏离补注`已注记残留）；(8) `state-machine.md` 无 MRP/预测独立状态机章节（散落在 mrp.md §实现偏离补注——是否应补）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 复核已登记 finding 计划规划状态机角度：P1-MA1-022（释放/聚合跨域访问——释放写 ErpPurOrder/ErpMfgSubcontractOrder 是状态迁移副作用，异常路径是否引入悬挂）/ P1-MA1-029（MrpReleaseService 跨域写委外单 APPROVED 绕审批——O-4 豁免同型裁决，确认 `posting-exemptions.md` 豁免登记完整性）。标注每项终态（仅治理缺陷 / 产生运行时缺陷升级）。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（含：MRP 计划头状态图 + 预测状态图 + 建议单隐式生命周期图 + 仿真状态图、各维度通过/失败裁决、控制点 PASS/FAIL、MA2 finding 运行时影响复核表 + O-4 豁免同型裁决、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。仅本阶段交付的本地化检查列在此。

- [ ] MRP 计划头（5 态）+ 预测（4 态）+ 建议单隐式生命周期（isFirmed）+ BOM（isActive）+ 仿真（config-gated）的状态图与转换矩阵产出，每个状态/转换有通过/失败裁决与证据
- [ ] 已识别控制点（状态定义 / 转换完整性[含 CANCELLED/CONSUMED 无写入路径] / 终端与恢复 / 异常路径[含 MRP 运算失败回滚 + 释放事务一致性] / 可达性[含 CANCELLED/CONSUMED 死状态] / 角色权限 / 外部依赖[含 O-4 豁免同型裁决] / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [ ] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 计划规划状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.x manufacturing/计划规划状态机行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（**MRP 运算 RUNNING 中途异常状态悬挂** [status 未回滚 DRAFT——若破坏状态机] / **释放路径生成目标单失败致建议单 isFirmed 与目标单悬挂半状态** [事务回滚缺口——若跨域写未在同一事务] / **SUBCONTRACT_REQUEST 释放 config-gated on 生成 APPROVED 委外单绕审批** [若破坏业务规则——但 config-gated 默认 off，裁决为 P1 治理]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及跨域保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。注意：本审计对已登记 finding（P1-MA1-022/029）只复核状态机运行时影响不重复登记根因；若发现新 P1（如 MRP CANCELLED dict 死状态 [若裁决为 P1 清理而非 P2 接受] / 预测 CONSUMED 死状态 [同型裁决] / MRP §建议单释放 RELEASED 文字 vs isFirmed 布尔 owner doc 漂移 / MrpReleaseService 跨域写委外单 APPROVED O-4 豁免登记缺失 [与 P1-MA1-029 同型]）按新 finding ID 登记。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.x manufacturing/计划规划状态机 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05b6f3200ffe2oUyy28uH5btWU`，独立 general 子代理，fresh-context，对照实时仓库逐行复核）。VERDICT = accept，**无 BLOCKER**。核实要点：3 个字典（mrp-status 5 态 `orm.xml:72-78` 含 CANCELLED / forecast-status 4 态 `:126-131` 含 CONSUMED / mrp-order-type `:79-84`）行号精确 ✓；`ErpMfgMrpPlan.status:777` + `ErpMfgMrpPlanLine` 无 status 列用 isFirmed:824 + convertedBillCode:825 ✓；`ErpMfgForecast.status:906` + ForecastLine 无 status ✓；`ErpMfgBom` isActive:202 布尔无状态机 ✓；`ErpMfgMrpDemand` 无 status（demandSource 是分类非生命周期）✓；**MrpEngine 无 CANCELLED writer**（grep setStatus 无 CANCELLED 写入）✓ + runMrp:77/守卫:79-83/RUNNING:84/COMPLETED:98/clearLines:243/orderType:122-124/isFirmed=FALSE:140/Non-Goals SUBCONTRACT:48-50 ✓；**ForecastBizModel javadoc:21 CONSUMED deferred** + approve:34/cancel:50 无 submit/reject/consume ✓；**DemandAggregator 读 APPROVED:175 不回写 CONSUMED** + clearSynthesized:257 ✓；**MrpReleaseService 跨域写 ErpPurOrder:137/saveEntity:150 DRAFT/UNSUBMITTED + ErpMfgSubcontractOrder:187 APPROVED:199-201 绕审批 + O-4 豁免 javadoc:42-53/148-149 + advancePlanToFirmedIfComplete:218-236 FIRMED:233** ✓；**SimulationMrpEngine fork 不调 MrpEngine.runMrp（grep 仅 javadoc 引用）** + promoteToFormalPlan:171/新 DRAFT:195/ARCHIVED:226/单次路径零触及 javadoc:45-46 ✓；ErpMfgMrpPlanBizModel 仅 runMrp 无 cancelMrp ✓；6 个测试文件存在 ✓。**事务边界补充观察（支持审计非基线错误）**：`ErpMfgMrpPlanBizModel.runMrp:36` 是 @BizMutation，Nop 事务包裹整个 MrpEngine.runMrp（含 RUNNING→COMPLETED 写入）——若 MrpEngine 在置 RUNNING 后抛异常则事务回滚 RUNNING 不持久化；计划正确地将此标为待调查控制点而非预判（审计适当）。检查清单全 PASS（基线准确性零偏差/范围单一结果表面计划规划类状态机/Item 类型 Proof+Fix/Add/Follow-up/技能匹配工作方法/反松弛无 optional·consider·maybe/不可降级项 CANCELLED·CONSUMED 死 dict 已标为控制点裁决 P1/P2 而非 Follow-up/结束门控含独立结束审计门控 + 全量验证在 Closure Gates 非阶段退出/退出标准可观察无样板/A2.6a↔A2.6b 拆分各为单一结果表面不过度拆分 + A2.6a 先执行的软排序合理 [mission driver 按文档顺序执行，roadmap 表中 A2.6a 在 A2.6b 前]）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。建议单释放跨域写 ErpPurOrder/ErpMfgSubcontractOrder 触及跨域保护区域，P0 即时修复须额外人工确认。

- [ ] 范围内行为完成（A2.6b 计划规划状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、mrp/bom-and-routing/simulation-engine/state-machine owner doc + posting-exemptions §MrpReleaseService 结论已反映）
- [ ] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-manufacturing/erp-mfg-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.6a 生产执行类状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 那是 S 级拆分 1/2，先执行。本审计只复核建议单释放生成的工单/委外单骨架进入目标域后状态正确（DRAFT 工单骨架 / APPROVED 委外单绕审批——后者在 scope 内）。
- Successor Required: `no`——A2.6a 先于本计划执行（done）。

### A4.2b manufacturing 代码质量审计 — MRP/质量集成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做 MRP/预测/建议单状态机**业务正确性**审查；MRP 引擎/释放代码质量（异常处理/N+1/索引/算法正确性）系统性审查归 A4.2b。
- Successor Required: `yes`——A4.2b 执行时复核。

### A2.17 并发与乐观锁（并发释放同建议单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（isFirmed 无 @Version 并发释放 / advancePlanToFirmedIfComplete 头级回写竞态 / 仿真 promote 并发），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated Deferred 偏离本身（预测 CONSUMED 回写 / 物料级 lot sizing 列 / 在途在制实时汇总 / CRM disaggregation / SUBCONTRACT_RELEASE config / 仿真算法）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 Deferred/Non-Goal。本审计只确认其在状态机上不引入悬挂（CONSUMED/CANCELLED 死状态归状态定义清晰性维度裁决为 P1/P2 dict 死状态清理而非实现；仿真 config-gated 默认关 + 单次路径零触及）。
- Successor Required: `yes`——各 successor 触发条件满足时（如预测消费后状态回写需求落地 / 物料级批量精细化 / 在途实时汇总 / CRM 金额→数量 disaggregation / 委外释放业务上线 / 事件驱动实时仿真）。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- <待执行后填写>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
