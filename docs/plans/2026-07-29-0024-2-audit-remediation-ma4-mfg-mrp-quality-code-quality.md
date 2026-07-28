# 2026-07-29-0024-2-audit-remediation-ma4-mfg-mrp-quality-code-quality MA4 manufacturing 代码质量审计 — MRP/DRP 引擎 / 质量集成与 NCR（A4.2b）

> Plan Status: active
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.2b，S 级拆分 2/2）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」行 + §1.3 manufacturing 功能模块拆分「MRP/DRP 引擎 / 质量集成与 NCR」切片；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/code-quality-audit-prompt.md`（审计方法）；`docs/design/manufacturing/mrp.md` + `simulation-engine.md` + `variance-analysis.md` + `batch-genealogy.md` + `subcontracting.md` + `crp.md`（owner doc 锚点）；`docs/plans/2026-07-28-0109-2-audit-remediation-ma2-mfg-mrp-bom-state-machine.md`（A2.6b 同切片业务正确性审计——MRP/BOM 状态机，本审计聚焦**代码实现质量**，互补不重叠）；`docs/plans/2026-07-29-0024-1-audit-remediation-ma4-mfg-work-order-bom-code-quality.md`（A4.2a 同域拆分 1/2——工单/BOM，不同功能模块，独立计划）；`docs/plans/2026-07-28-2130-3-audit-remediation-ma4-finance-budget-arap-cost-period-code-quality.md`（A4.1b finance——MA4 多功能模块合并范式参照）
> Audit: required

## Current Baseline

manufacturing 代码质量审计 MRP/DRP 引擎 / 质量集成与 NCR 切片（代码与前端质量层 MA4 第四项，S 级拆分 2/2）。roadmap 工作项 A4.2b 声明审查"manufacturing 代码质量审计 — MRP/质量集成（S 级拆分 2/2）"，owner doc 标注 `docs/design/manufacturing/mrp.md`，skill `docs/skills/code-quality-audit-prompt.md`。

**关键基线事实（实时仓库核实）**：

- **manufacturing 域是 S 级域**（scope matrix §1.1 快照 2026-07-27，用于驱动 S 级分级）：41 实体 / 74 mutation / 21 Proc/Engine/Resolver / 11 状态机实体 / 30 测试 / 47 跨域 daoFor。S 级（mutation ≥ 70 满足，分级结论稳定）。按 scope matrix §1.3 功能模块拆分为 4 片，本审计覆盖「MRP/DRP 引擎」+「质量集成与 NCR」2 个功能模块片，并合并覆盖关联计算引擎（成本卷积/生产差异/批次基因/CRP/预测/委外）——这些是计划与计算子系统，非订单执行，自然归入 MRP 引擎同侧。（slug `mrp-quality` 为简写，实际覆盖范围在下方盘点。）
- **MRP/质量集成 + 关联计算链路代码规模**（实时仓库核实）：`find module-manufacturing -path "*service*" \( -name "*Mrp*" -o -name "*Drp*" -o -name "*Genealogy*" -o -name "*Variance*" -o -name "*Cost*" -o -name "*Schedule*" -o -name "*Crp*" -o -name "*Forecast*" -o -name "*Inspection*" -o -name "*Quality*" \) -name "*.java" -not -path "*/target/*"` = 41 文件（含 ~15 测试）。（基线诚实注记：该 glob 的 `-name "*Schedule*"` 附带捕获 `ErpMfgScheduleToJobCardProcessor` + `TestErpMfgScheduleToJobCard` 2 文件，二者属 A4.2a 工单与报工片（A4.2a find 已含 `*JobCard*`，A4.2a Phase 1 Targets 显式拥有 ScheduleToJobCard；本审计 Phase 1 Targets 不列 ScheduleToJobCard，Non-Goals 将 jobcard 交接 A4.2a）——故实际属本审计范围的文件约 39，与 A4.2a 无审计重复，仅计数 glob 工件。）核心组件：
  - **MRP/DRP 引擎**：`MrpEngine`（MRP 计算——净需求/计划订单/低层码）/ `MrpReleaseService`（释放——采购单/委外单/生产单 config-gated 多路径）/ `SimulationMrpEngine`（仿真引擎 What-If）/ `ErpMfgMrpPlanBizModel` + `ErpMfgMrpPlanLineBizModel` + `ErpMfgMrpScenarioBizModel` 系列（场景版本管理）
  - **关联计算引擎**：`CostRollupService`（成本卷积）/ `ProductionVarianceCalculator`（生产差异 6 类——材料用量/人工效率/制造费用/产量/委外，+ 差异阈值预警 dispatchVarianceAlertIfOverThreshold）/ `ProductionVarianceDispatcher` + `ProductionVarianceAcctDocProvider`（差异过账）/ `BatchGenealogyTracer` + `BatchGenealogyWriter`（批次基因链 forward/backward trace）/ `CrpLoadCalculator` + `ErpMfgCrpRunJob`（CRP 负荷计算 + cron）/ `ErpMfgForecastBizModel` 系列（预测消费）
  - **委外**：`SubcontractPostingDispatcher` + `SubcontractIssue/Receipt/Fee AcctDocProvider`（委外过账链路）
  - **质量集成与 NCR**：制造-质量跨域（reportCompletion 质检门控 config-gated `inspection-gate-enabled`——门控逻辑在 WorkOrderProcessor，A4.2a 覆盖；本审计复核 mfg 侧 NCR 触发/质检结果读取的跨域 Facade 调用点实现质量）
- **owner docs**：`mrp.md`（MRP 计算 + 释放 + 仿真引擎 + 实现偏离补注）/ `simulation-engine.md`（What-If）/ `variance-analysis.md`（差异 6 类 + 阈值预警 config）/ `batch-genealogy.md`（基因链）/ `subcontracting.md`（委外）/ `crp.md`（CRP 负荷）/ `material-reservation.md`（预留子系统）。
- **MA2 已审计的已知 finding（代码质量审计输入，非重复审计）**：A2.6b MRP/BOM 状态机审查（P1-MA2-036 MRP CANCELLED + 预测 CONSUMED dict 死状态 / P1-MA2-037 mrp.md RELEASED vs isFirmed 布尔 drift / P1-MA2-038 MrpReleaseService 委外单 APPROVED 豁免登记缺失）；A2.17 并发审计（P1-MA2-086 mfg cron job 并发——erp-mfg-jobcard-auto-generate[工单侧归 A4.2a]；CRP/DemandAggregator 等计算引擎并发敏感点交接）；A2.6b 交接 Subcontract/ProductionVariance posting dispatcher tryPost 吞异常悬挂同型根因（同 finance/hr/assets/qa/projects/maintenance/logistics 同型）。
- **MA1 已审计的已知 finding**：P1-MA1-022（aps/drp 跨域只读 daoFor ErpMfgForecast/ErpMfgBom 等读侧投影）；MRP 相关实体 ORM 规范。
- **MA3 已审计的已知 finding（owner-doc drift，复核输入）**：P1-MA3-042（material-reservation.md 整个预留子系统未实现——288 行 owner doc 几乎完全未实现，mfg 域最大单一 drift）；P1-MA3-043（use-cases.md UC-MFG-12 差异公式列表错误——4 公式 vs code 6 类）；P1-MA3-045（差异阈值预警已实现但 doc 标 Deferred——code dispatchVarianceAlertIfOverThreshold 已落地）。

**审计张力**：MA2 审计了 MRP/质量链路的**业务正确性**（状态机/并发），但**代码实现质量**是 MA4 的独立维度。MA2/MA3 已知 finding 是本审计的**输入**。本审计聚焦 MA2 未覆盖的代码质量维度：如 MrpEngine 净需求计算的算术正确性与低层码递归 / MrpReleaseService 多路径释放（采购/委外/生产）的事务边界与幂等 / SimulationMrpEngine 与 MrpEngine 的代码复用与一致性 / CostRollupService 卷积递归终止与成环检测 / ProductionVarianceCalculator 6 类差异的算术正确性 + 阈值预警 dispatchVarianceAlertIfOverThreshold 的错误传播 / BatchGenealogyWriter 基因链写入的幂等与一致性 / CrpLoadCalculator 负荷计算的算术正确性 / SubcontractPostingDispatcher 过账异常吞咽与悬挂 / Forecast 消费的去重机制 / cron job 并发重复副作用（P1-MA2-086 运行时复核）。

剩余差距：需要一次 MRP/DRP 引擎 / 质量集成与 NCR + 关联计算链路的代码实现质量审计。发现的缺陷分类同 A4.2a：(a) 架构边界违规；(b) 核心实现正确性（事务/幂等/异常悬挂/算术错误）；(c) 错误处理与操作安全；(d) 测试有效性（异常路径）；(e) 可维护性风险。blocker/major 登记为 P1（代码类目标 MR2 / 业务正确性类目标 MR1）。若发现活跃数据破坏路径，升级标注走 P0 即时通道。

## Goals

- 按 `code-quality-audit-prompt.md` 7 重点领域对 manufacturing MRP/DRP 引擎 / 质量集成与 NCR + 关联计算链路代码做系统性实现质量审计，产出审计报告。
- 审计覆盖核心组件（slug `mrp-quality` 为简写，实际含关联计算引擎）：MrpEngine + MrpReleaseService + SimulationMrpEngine / CostRollupService + ProductionVarianceCalculator + ProductionVarianceDispatcher/AcctDocProvider / BatchGenealogyTracer + BatchGenealogyWriter / CrpLoadCalculator + ErpMfgCrpRunJob / Forecast 消费 / SubcontractPostingDispatcher + 委外 AcctDocProvider 系列 / mfg 侧质量集成跨域 Facade 调用点。
- 复核 MA1/MA2/MA3 已知 finding（P1-MA1-022 / P1-MA2-036/037/038/086 / P1-MA3-042/043/045 + Subcontract/ProductionVariance posting dispatcher tryPost 悬挂同型）的运行时状态，标记是否有 MA2 未发现的代码层缺陷。
- scope matrix §2.4「代码质量（MA4）」行增 manufacturing 全片完成注记段（§2.4 无 per-domain 列；与 A4.2a 合并后 manufacturing 代码质量全片终态在此收口）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总。roadmap A4.2b 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做工单与报工 / BOM 与工艺路线代码质量 — 归 A4.2a（同批起草，S 级拆分 1/2，不同功能模块）。
- **不**做业务正确性/状态机审计 — 归 A2.6b（已 done）。本审计聚焦**代码实现质量**，MA2 已知 finding 作为输入复核。
- **不**做 view.xml vs 后端契约 drift — 归 A4.6（MA4 view drift 批次）。
- **不**做 owner doc vs 代码 drift — 归 A3.3/A3.4（已 done）。
- **不**做完工入库 GL 过账 Provider 实现质量（制造完工过账依赖 finance 域 Provider，归 A4.1a）——本审计复核 mfg 侧 Subcontract/ProductionVariance posting dispatcher 调用点的错误传播与悬挂。
- **不**做质检门控 enforceGate 实现质量（门控逻辑在 WorkOrderProcessor，归 A4.2a）——本审计仅复核 mfg 侧质量集成跨域 Facade 调用点。
- **不**做测试覆盖深度统计 — 归 A5.2（MA5 测试层）。
- **不**做权限注解完整性 — 归 A6.1/A6.2（MA6 安全层）。
- **不**在本计划内批量修复代码缺陷 — P1 经 R2.0/R1.0 展开机制进入 MR2/MR1。本审计只识别缺陷 + 分类。
- **不**手改生成物或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/manufacturing/mrp.md` + `simulation-engine.md` + `variance-analysis.md` + `batch-genealogy.md` + `subcontracting.md` + `crp.md` + `material-reservation.md`（roadmap A4.2b owner docs）；`module-manufacturing/erp-mfg-service/`（MRP/质量/计算链路代码实现——审计对象）；`docs/audits/2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（A2.6b 已知 finding——本审计输入）；`docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md` §mfg（A3.4 已知 drift——本审计输入）
- Skill Selection Basis: `code-quality-audit-prompt.md`（roadmap A4.2b 指定此 skill——7 重点领域 + 严重性指南。项目定制化层见 `docs/skills/README.md`）。与 A4.2a 不同结果表面（MRP/质量集成 vs 工单/BOM），独立计划。与 A2.6b 不同维度（代码实现质量 vs 业务正确性状态机），互补不重叠。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。代码缺陷修复在 MR2/MR1 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为代码静态审查 + 测试有效性抽样，不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。代码静态审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - MRP/DRP 引擎 / 质量集成与 NCR + 关联计算链路代码实现质量系统性审计（7 重点领域）

Status: planned
Targets: `module-manufacturing/erp-mfg-service/` MRP/质量/计算链路代码（MrpEngine + MrpReleaseService + SimulationMrpEngine + MRP 场景 BizModel 系列 / CostRollupService + ProductionVarianceCalculator + ProductionVarianceDispatcher + ProductionVarianceAcctDocProvider / BatchGenealogyTracer + BatchGenealogyWriter / CrpLoadCalculator + ErpMfgCrpRunJob / Forecast 系列 / SubcontractPostingDispatcher + 委外 AcctDocProvider 系列 / mfg 侧质量集成跨域 Facade 调用点）；owner docs `docs/design/manufacturing/{mrp,simulation-engine,variance-analysis,batch-genealogy,subcontracting,crp,material-reservation}.md`
Skill: `code-quality-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 + MA2 + MA3 done（已知 finding 作为输入）；A2.6b done（状态机基线）；A3.4 done（owner-doc drift 基线）；A4.1b done（多功能模块合并范式参照）；A4.2a done（仅「质量集成门控」子项依赖 A4.2a 交接——质检门控逻辑在 WorkOrderProcessor，其余 MRP/计算功能模块与工单/BOM 无显著依赖）。

- [ ] 领域「架构和边界完整性」：核查 MRP/质量/计算链路代码的跨域访问合规性——MrpReleaseService 释放采购单/委外单是否经 IErpPurOrderBiz/IErpMfgSubcontractOrderBiz（复核 P1-MA2-038 委外单 daoFor 直写半治理）/ 需求聚合 DemandAggregator 跨域读 ErpMfgForecast/ErpInvStockBalance/ErpMdMaterial 是否经 Facade / 差异过账是否经 IErpFinVoucherBiz Facade / 基因链读 ErpInvBatch 是否合规。复核 P1-MA1-022（aps/drp 读 ErpMfg* 跨域投影）运行时状态。标记边界违规站点。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「核心实现正确性」：核查 MrpEngine 净需求计算的算术正确性 + 低层码递归终止 / MrpReleaseService 多路径释放（采购/委外/生产）的事务边界与幂等 + 委外单 APPROVED 绕审批（复核 P1-MA2-038）/ SimulationMrpEngine 与 MrpEngine 代码复用一致性 / CostRollupService 卷积递归终止与成环检测 / ProductionVarianceCalculator 6 类差异算术正确性 + 阈值预警 dispatchVarianceAlertIfOverThreshold 错误传播 / BatchGenealogyWriter 基因链写入幂等一致性 / CrpLoadCalculator 负荷算术 / Subcontract/ProductionVariance posting dispatcher tryPost 吞咽悬挂（复核同型根因）/ Forecast 消费去重（复核 P1-MA2-036 CONSUMED 不回写）/ cron job 并发重复（复核 P1-MA2-086）。标记事务/幂等/异常悬挂/算术缺陷。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「类型和契约质量」：核查 MrpReleaseService 多路径释放参数返回契约一致性 / 差异 6 类的 BigDecimal 类型安全 / 基因链 trace 返回结构契约 / 仿真引擎 What-If 入参契约。标记类型不匹配/契约漂移。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「错误处理和操作安全」：核查 MRP/质量/计算链路异常是否全部扩展 NopException + ErrorCode（`erp.err.mfg.*`）/ 释放失败/卷积成环/差异过账失败/cron job 异常的错误传播。复核差异公式与 owner doc 一致性（P1-MA3-043）/ 预警实现与 doc Deferred 标注（P1-MA3-045）。标记裸异常/ErrorCode 缺失/错误信息不足。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「测试有效性」：抽样 manufacturing 30 测试中 MRP/质量/计算相关测试（TestErpMfgMrpEndToEnd / TestErpMfgMrpEngine / TestErpMfgMrpSimulation / TestErpMfgProductionVariance / TestErpMfgVarianceAlert / TestErpMfgVarianceRecomputeReversal / TestErpMfgBatchGenealogy / TestErpMfgCostRollup / TestErpMfgCostFlowEndToEnd / TestErpMfgCrpLoad / TestErpMfgForecastSource），核查**异常路径覆盖**（释放失败/卷积成环/差异过账悬挂/cron 并发重复/委外绕审批）+ 断言强度（是否仅断言 status 还是校验差异行数值/基因链/凭证行）。标记测试空洞。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「可维护性和未来变更风险」：核查 MrpEngine/CostRollupService 复杂度（行数/圈复杂度）/ SimulationMrpEngine 与 MrpEngine 重复模式 / 委外 AcctDocProvider 系列 4 个的对称性 / 预留子系统未实现的 owner doc 维护风险（P1-MA3-042）。标记 P2 可维护性风险。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 领域「自动化和防护覆盖」：核查 MRP/质量/计算链路是否有 compliance checker 规则守护 / 是否有测试门控防止回归（释放/卷积/差异过账/基因链）。标记防护缺口。
      - Skill: `code-quality-audit-prompt.md`
- [ ] 产出审计报告 `docs/audits/2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md`（含：7 领域逐项审查结果 / MA1/MA2/MA3 已知 finding 运行时复核 / P0-P3 finding 清单按严重性排序 / 每项含文件路径+行引用 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [ ] 7 重点领域逐项审查结果产出（每领域至少一句裁决，含"本领域无缺陷"）
- [ ] MA1/MA2/MA3 已知 finding 运行时复核产出（每项标记"如 owner doc 声明"或"发现新代码层缺陷"）
- [ ] P0-P3 finding 清单产出按严重性排序，每个含文件路径+行引用+严重性+缺陷描述+影响+目标 MR

### Phase 2 - finding 汇总交接 MR2/MR1 + 索引/矩阵更新

Status: planned
Targets: MRP/质量/计算链路代码质量 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」manufacturing 列
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] finding 汇总：全部缺陷 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，起始编号 = A4.1a/A4.1b/A4.2a 已分配最大 P1-MA4-N + 1，避免命名空间碰撞；报告、领域、功能模块、缺陷描述、目标 MR2[代码类]/MR1[业务正确性类]、修复状态 todo）。与 MA2/MA3/A4.1a/A4.1b/A4.2a 已登记 P1 经交叉去重无冲突。
      - Skill: none
- [ ] 分类裁决：代码实现质量 finding 目标 MR2；业务正确性类 finding 目标 MR1；活跃数据破坏走 P0 即时通道，在报告中明确标注。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「代码质量（MA4）」行增 manufacturing 全片完成注记段（§2.4 无 per-domain 列；与 A4.2a 合并后 manufacturing 代码质量全片终态收口）。
      - Skill: none

Exit Criteria:

- [ ] 所有缺陷 blocker/major 已登记 arm-index §P1 汇总（代码类 MR2 / 业务正确性类 MR1），待展开
- [ ] 与 MA2/MA3/A4.1a/A4.1b/A4.2a 已登记 P1 经交叉去重无重复登记
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论（manufacturing 代码质量全片终态）

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_056717ec8ffebXvF14upm74zv4`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT: accept，无 BLOCKER。LIVE-REPO 复核：find 计数 41（=基线声称）可复现；核心组件全部存在（MrpEngine/MrpReleaseService/SimulationMrpEngine/ProductionVarianceCalculator/BatchGenealogyWriter/CrpLoadCalculator/SubcontractPostingDispatcher/CostRollupService PASS）；finding ID 真实（P1-MA2-036/037/038 + P1-MA3-042/043/045 + P1-MA2-086 均 PASS，mfg 归属正确）；owner docs 存在（7 个全 PASS）；scope matrix §2.4 无 per-domain 列；与 A4.2a 范围边界 clean（不同功能模块，Phase 1 Targets 不相交）。NON-BLOCKING NOTE（已处理）：find glob `-name "*Schedule*"` 附带捕获 ScheduleToJobCard 2 文件属 A4.2a 片——已在 Current Baseline 增基线诚实注记（实际属本审计范围约 39 文件，无审计重复，仅 glob 计数工件）。逐项裁决：规则 1 基线诚实 PASS / 规则 2 边界清晰 PASS / 规则 4+14 多功能模块合并正当（A4.1b 先例）PASS / 规则 5/7 类型标注 PASS / 规则 8/9 skill 正确 PASS / anti-slack PASS / P1-MA4 命名空间起始 = A4.1a/A4.1b/A4.2a max+1 collision-safe PASS / 命名 N=2 PASS / Plan Status draft PASS。Plan Status 转 active。

## Closure Gates

> 本计划主体是代码静态审查 + 测试有效性抽样（不改代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。代码缺陷修复在 MR2/MR1 批量进行；活跃数据破坏走 P0 即时通道。本审计只识别缺陷 + 分类。

- [ ] 范围内行为完成（A4.2b MRP/DRP 引擎 / 质量集成与 NCR + 关联计算链路代码质量审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [ ] 已运行验证：代码静态审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 工单与报工 / BOM 与工艺路线代码质量（A4.2a）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计聚焦 MRP/DRP 引擎 + 质量集成与 NCR + 关联计算功能模块；工单与报工 + BOM 与工艺路线归 A4.2a（同批起草，S 级拆分 1/2）。质检门控逻辑在 WorkOrderProcessor 实现质量由 A4.2a 覆盖；本审计复核 mfg 侧质量集成跨域 Facade 调用点。
- Successor Required: `no`——A4.2a 同批起草。

### 业务正确性/状态机（A2.6b）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审代码**实现质量**；MRP/BOM 链路业务正确性/状态机归 A2.6b（已 done）。MA2 已知 finding 作为本审计输入复核。
- Successor Required: `no`——A2.6b 已 done。

### view.xml drift（A4.6）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端 view.xml drift 归 A4.6。本审计审后端代码实现质量。
- Successor Required: `yes`——A4.6 执行时复核 mfg view。

### 测试覆盖深度统计（A5.2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计"测试有效性"维度审异常路径覆盖 + 断言强度；覆盖深度统计归 A5.2。
- Successor Required: `yes`——A5.2 执行时复核 mfg 测试深度。

## Closure

Status Note: <填入关闭理由>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
