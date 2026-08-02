# 2026-08-02-2231-2 rc-ma1-a1-11-mfg-f4-variance-batch-kanban mfg-F4 差异/批次/看板需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-02
> Mission: requirement-compliance
> Work Item: A1.11（MA1 需求追踪矩阵审计 — mfg-F4 差异/批次/看板：UC-MFG-11 制造看板 + UC-MFG-12 生产成本差异分析 + UC-MFG-13 生产批次追溯）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.11
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.11 的 0.2 依赖）、`2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（A1.9，同域同范式；UC-MFG-07 完工成本结转/差异过账链归 A1.9，本切片 UC-MFG-12 差异计算/入账/重算从完工触发视角核验，交叉引用完工编排）、`2026-08-02-2231-1-rc-ma1-a1-10-mfg-f3-bom-routing.md`（A1.10，同批次同范式）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点被审功能现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.11 给出 UC 清单 = `UC-MFG-11/12/13`（3 UC），锚点 `use-cases.md:195` / `:216` / `:238`（inventory :345 确认一致 ✅）。

- **L1 需求契约（权威真相源）**：`docs/design/manufacturing/use-cases.md`：
  - UC-MFG-11 制造看板（`:195`）：KPI 卡片值 == 对应实体实时聚合(按期间/orgId/权限过滤)，非硬编码（在制工单/完工量/准时率/齐套待产/状态分布/齐套不足·延期预警）；预警项 == 满足阈值条件的记录(阈值来自系统配置,非硬编码)；看板数据受行级权限约束(只看自己组织/部门/成本中心)。
  - UC-MFG-12 生产成本差异分析（`:216`）：工单完工过账→触发差异计算(6 类：材料用量/人工效率/人工费率/制造费用/产量/委外费；PPV 归采购域 inventory InvPostingDispatcher.dispatchPurchasePriceVariance 捕获 PURCHASE_PRICE_VARIANCE，不在生产差异内避免重复计入)；差异逐条写入 ErpMfgCostVariance(每类型一条, varianceType 枚举)；差异报表可按工作中心/产品/期间/差异类型分组聚合。
  - UC-MFG-13 生产批次追溯（`:238`）：完工入库时记录输入批次→输出批次关系到 ErpMfgBatchGenealogy；前向追溯(给定 outputLotId→全部 inputLotId)；反向追溯(给定 inputLotId→全部 outputLotId)；多级追溯(递归上下游完整批次链)；召回报告(从问题批次识别所有受影响成品批次)。

- **L2 owner doc 设计参考**：`docs/design/manufacturing/variance-analysis.md`（差异分类表 + ErpMfgCostVariance 数据模型 + 核心计算逻辑 + 报表维度 + 使用流程 `:81-89`：PPV 归采购 + ProductionVarianceCalculator 6 类 + ProductionVarianceDispatcher PRODUCTION_VARIANCE 过账 + 手动入口 calculateVariances + 阈值告警 dispatchVarianceAlertIfOverThreshold + 重算幂等实现注记 `:91-109`：红冲→删旧→重算→派发四步 + config 三键）+ `docs/design/manufacturing/batch-genealogy.md`（追溯模型 + ErpMfgBatchGenealogy 数据模型 + 核心查询前向/反向/全链 + Decision 1/2/3 + **recallReport 降级说明 `:141-143`：当前 IErpInvStockBalanceBiz/IErpInvBatchBiz 仅 CRUD，recallReport 降级为仅返回受影响成品批次集合(RecallReport.degraded=true)，位置/去向查询归 inventory successor**）+ `docs/design/dashboards.md` §制造看板（全局看板设计参考）。**注意**：L2 为设计参考，与 L1 冲突时按 §4 Q1 以 L1 为准；recallReport 降级标注是否构成 §4 判据"显式人工批准 documented simplification"须执行时核验批准来源。

- **L3 代码实现现状（执行时实测核验）**：
  - **制造看板（UC-MFG-11）**：`module-manufacturing/erp-mfg-service/.../dashboard/ErpMfgDashboardBizModel.java`（KPI 聚合查询入口；执行时核验：KPI 是否实时聚合 SQL/聚合方法 vs 硬编码常量、阈值是否读 config `erp-mfg.variance-alert-threshold` 等、orgId/deptId/costCenter 行级权限过滤是否生效）。
  - **生产差异（UC-MFG-12）**：`ProductionVarianceCalculator`（6 类差异计算 MATERIAL_USAGE/LABOR_EFFICIENCY/LABOR_RATE/OVERHEAD/VOLUME/SUBCONTRACT + `dispatchVarianceAlertIfOverThreshold` 阈值告警调 IErpSysNotificationBiz.notify 派发 `mfg.production-variance`）+ `ProductionVarianceDispatcher`（按成本要素汇总净差异组装 PostingEvent 经 IErpFinVoucherBiz.post 提交过账 PRODUCTION_VARIANCE 业务类型 + `dispatchIfApplicable:111-117` catch(Exception) 吞异常保持 posted=false + `reverseIfExists` 红冲）+ 手动重算入口 `ErpMfgCostVarianceBizModel.calculateVariances:69`（红冲→删旧→重算→派发四步幂等）。config：`erp-mfg.variance-auto-calc-enabled`(默认 false, 完工 willFinish 触发) / `erp-mfg.variance-alert-enabled`(默认 true) / `erp-mfg.variance-alert-threshold`(100)。**A1.9 已交叉引用完工触发差异过账链（reportCompletion:227-239 四步 catch(Exception)→LOG.error 吞异常 = P1-MA4-007）**，本切片从差异计算/入账/重算视角核验。
  - **批次追溯（UC-MFG-13）**：`module-manufacturing/erp-mfg-service/.../genealogy/BatchGenealogyTracer.java`（前向/反向/多级递归追溯查询）+ `BatchGenealogyWriter.java`（完工时一次性写入 input→output 基因行, Decision 1）+ `ErpMfgBatchGenealogyBizModel.java`（recallReport 入口；执行时核验降级实现 RecallReport.degraded=true 仅返回受影响成品批次集合 vs L1"识别所有受影响成品批次"——降级版满足"识别受影响成品批次"，位置/去向查询缺失归 inventory successor）+ 完工自动建批 `ensureOutputLot`(Decision 2, batchNo=FG-{woCode})。config：`erp-mfg.genealogy-write-enabled`(默认 true, best-effort try/catch 不阻断完工, Decision 3)。

- **L4 测试证据现状**：UC-MFG-11——`TestErpMfgDashboard` + `TestErpMfgDashboardCrpChart`（执行时核验 KPI 实时聚合断言 + 阈值断言 + 行级权限断言强度）。UC-MFG-12——`TestErpMfgProductionVariance`（6 类差异）+ `TestErpMfgVarianceAlert`（阈值告警）+ `TestErpMfgVarianceRecomputeReversal`（重算红冲幂等四步 + 一致不变量 ErpFinVoucherBillR 反查 {wo.code}-PV 仅 1 条 NORMAL + 数据行与凭证金额一致 + 全 posted=true）。**业财异常路径零覆盖**（A4.2a P1-MA4-009：dispatcher 过账失败悬挂 posted=false 无测试触发）。UC-MFG-13——`TestErpMfgBatchGenealogy`（执行时核验前向/反向/多级 + 召回报告降级断言强度）。**best-effort 写失败路径无测试**（Decision 3 缺口可观测性）。

- **L5 既有证据（MA2 复用输入，方法论 §去重协议）**：
  - **`docs/audits/2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（A4.2a）= 工单/BOM 代码质量审计**：Verdict **FAIL**（零 P0、3 P1：**P1-MA4-007 完工编排层差异计算/过账失败吞异常致业财悬挂**（reportCompletion:227-239 + ProductionVarianceDispatcher.dispatchIfApplicable:111-117 同型）/ P1-MA4-008 跨域 daoFor / **P1-MA4-009 业财一体异常路径零覆盖 + 完工入库 GL voucher 行级断言缺失** + 1 P2）。**A4.2a 范围 = ProductionVarianceDispatcher 调用点 + 完工触发编排**（A4.2a:244 显式交接：ProductionVarianceCalculator / CostRollupService 实现质量 + BatchGenealogyWriter 实现质量归 A4.2b 执行时复核）。
  - **`docs/audits/2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md`（A4.2b）= MRP/质量集成/成本核算/基因追溯代码质量审计**：Verdict 维度 2 核心实现正确性 **PASS**（无活跃算术/事务/幂等缺陷）。**直接覆盖本切片 UC-MFG-12/13 的实现质量**：ProductionVarianceCalculator 6 类差异算术 PASS（A4.2b:54，逐类复核 + divideSafe 防除零 + BigDecimal 类型安全，TestErpMfgProductionVariance 行级数值断言印证）+ dispatchVarianceAlertIfOverThreshold 错误传播降级 PASS（A4.2b:55）+ BatchGenealogyWriter 基因链写入幂等一致性 PASS（A4.2b:56，writeOnCompletion best-effort + ensureOutputLot 幂等 + usedInputLots 去重）+ 重算幂等四步链（红冲→删旧→重算→派发 + 一致不变量）。finding：P1-MA4-010（SubcontractPostingDispatcher issue/receipt 过账失败吞咽无闭环，业财悬挂同型根因）/ P1-MA4-011（CostRollupService ERR_BOM_CYCLE 路径无直接测试，测试有效性）/ P1-MA4-012（MRP/成本/基因/委外跨域只读 daoFor 绕 I*Biz，同 P1-MA1-022/P1-MA4-008 根因投影，含 BatchGenealogyWriter daoFor(ErpInvBatch) mfg→inv 写 O-4 豁免未登记）。
  - **`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a）**：工单/作业卡状态机 pass（完工 COMPLETED 触发差异/批次写入的触发点已覆盖）。
  - **注意**：A4.2a/A4.2b/A2.6a 覆盖**差异算术正确性/基因链幂等/代码质量/状态机触发点**，但本切片从**需求契约↔实现符合性**视角补差异（UC-MFG-11 看板 KPI 实时聚合/阈值/权限符合性 + UC-MFG-12 差异计算 6 类完整性与 PPV 归属 + UC-MFG-13 追溯链完整性与召回报告降级符合性 + resolved finding HEAD 复核）。

- **arm-index 既有 finding 衔接**：差异/批次相关——`P1-MA4-007`（完工编排差异过账吞异常致业财悬挂，resolved 状态执行时 HEAD 复核）/ `P1-MA4-009`（业财异常路径测试有效性）/ `P1-MA4-010`（委外 issue/receipt 过账吞咽无闭环）/ `P1-MA4-011`（CostRollup 测试有效性）/ `P1-MA4-012`（MRP/成本/基因/委外跨域 daoFor 投影，含 BatchGenealogyWriter daoFor(ErpInvBatch) 写 O-4 豁免未登记）。UC-MFG-13 召回报告降级 + best-effort 写失败缺口为新维度（既有审计未从需求契约视角裁决），执行时 grep `arm-index.md` mfg 差异/批次/看板同域同控制点后裁决复用 or 新建 `P*-RC-xxx`。

- **保护区域**：本审计为**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按方法论 §10，P0 经 MR0 即时通道、P1 经 MR1（R1.0 展开 RC-R1.n）；触及会计过账逻辑（差异过账凭证/重算红冲）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.11 切片的五级追踪审计报告缺失 = MA4（A4.2 扩展域展开器，Deps=MA1 done）及 MR1（R1.0，Deps=MA1-MA4 done）的该切片证据缺口来源。本计划产出 A1.11 报告并登记 finding，解除其在 MA4/MR1 链路的该切片证据缺口。

## Goals

- 产出 A1.11 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md`，含方法论 §6 **9 段全部内容**：①UC-MFG-11/12/13 需求契约原文（逐字引用，不转述）②实现证据（`file:line`，含 ErpMfgDashboardBizModel KPI 聚合 + ProductionVarianceCalculator 6 类 + ProductionVarianceDispatcher 过账/红冲 + BatchGenealogyTracer/Writer 追溯链 + recallReport 降级）③测试证据（注明断言强度）④运行时行为证据（复用 A4.2a/A2.6a，补差异）⑤五级追踪矩阵 + 每 UC 符合性结论（P0/P1/P2/接受）⑥与 arm-index 衔接（复用 or 新增 裁决）⑦静态存疑点清单（供 MA4 展开）⑧过程纪律自检段 ⑨与 MA2/MA4 报告差异增量声明。
- 对 3 UC 逐条核验**每条验收标准**（完整枚举，§3）：UC-MFG-11（KPI 实时聚合非硬编码 + 阈值来自配置 + 行级权限过滤）+ UC-MFG-12（6 类差异计算 + PPV 归采购不重复计入 + 逐条写 ErpMfgCostVariance + 多维分组聚合报表）+ UC-MFG-13（完工记录 input→output + 前向/反向/多级追溯 + 召回报告识别受影响成品批次），各一矩阵行。
- 对候选缺口/偏离给出分级结论：**UC-MFG-12 P1-MA4-007 完工触发差异过账 catch(Exception)→LOG.error 吞异常致 GL 缺 PRODUCTION_VARIANCE 凭证 posted 悬挂（会计正确性类，Q4 无例外）HEAD 复核** + 业财异常路径零覆盖（P1-MA4-009 dedup）；**UC-MFG-13 召回报告降级（RecallReport.degraded=true 仅返回受影响成品批次集合，位置/去向查询缺失归 inventory successor）——按 §4 Q1 L1"识别所有受影响成品批次"裁决（降级版满足"识别"，位置/去向为增强维度 successor）+ best-effort 写失败缺口（Decision 3 try/catch 不阻断完工，可观测性兜底）**；UC-MFG-11 KPI 实时聚合/阈值/权限（执行时 HEAD 核验是否硬编码/权限过滤缺失）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区）。

## Non-Goals

- **不修复 finding**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / mfg use-cases / variance-analysis.md / batch-genealogy.md 需求契约段落；§9 冻结条款——分歧记入报告，不直改真相源）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.8/A1.9/A1.10 mfg done/draft；A1.11 只覆盖 UC-MFG-11/12/13）。**UC-MFG-07 完工成本结转凭证归 A1.9**，本切片 UC-MFG-12 差异计算/入账从完工触发视角核验（交叉引用完工编排 reportCompletion 差异过账链，不重复核验成本结转凭证本身）。
- **不重跑既有状态机/代码质量行为审计**（§去重协议：A4.2a/A2.6a 已证实差异算术/重算幂等/状态机触发点/代码质量，只补需求视角差异；不重审架构/代码质量维度）。

## Task Route

- Type: `verification or audit work`（需求→实现符合性五级追踪审计；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（审计契约 §1-§10 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.11 工作项 + Work Item Details MA1）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.11 UC 锚点）+ `docs/design/manufacturing/use-cases.md`（L1 真相源）+ `docs/design/manufacturing/variance-analysis.md` + `docs/design/manufacturing/batch-genealogy.md` + `docs/design/dashboards.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ A4.2a/A4.2b/A2.6a 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。该技能定义多维审计 prompt 范式，本切片需求↔实现符合性审计复用其维度框架；其必需输入（owner doc + use-cases + 代码路径 + 测试）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。**L5 行为证据**默认复用 A4.2a/A2.6a 审计（方法论 §去重协议），无需起服务；若需对存疑点做即时行为确认，可跑既有 JUnit（`mvn test -pl module-manufacturing/erp-mfg-service -Dtest=TestErpMfgDashboard,TestErpMfgDashboardCrpChart,TestErpMfgProductionVariance,TestErpMfgVarianceAlert,TestErpMfgVarianceRecomputeReversal,TestErpMfgBatchGenealogy`），不引入新依赖。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论 + resolved finding HEAD 复核

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md`（新建，先填 §1-§5；命名遵循方法论 §归档规范）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done（方法论契约 + UC 锚点就绪）

- [ ] `Proof` 对 UC-MFG-11/12/13 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:195/:216/:238` 验收标准原文（禁止转述）；L2 引用 `variance-analysis.md`（差异分类表 + 数据模型 + 计算逻辑 + 使用流程 + 重算幂等注记 + config 三键）+ `batch-genealogy.md`（追溯模型 + 数据模型 + 核心查询 + Decision 1/2/3 + recallReport 降级说明）+ `dashboards.md` §制造看板（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ErpMfgDashboardBizModel.java:line`（KPI 聚合）+ `ProductionVarianceCalculator:line`（6 类差异 + dispatchVarianceAlertIfOverThreshold）+ `ProductionVarianceDispatcher:line`（过账/红冲 + dispatchIfApplicable:111-117 catch 吞异常）+ `ErpMfgCostVarianceBizModel.calculateVariances:69`（重算四步）+ `BatchGenealogyTracer:line`/`BatchGenealogyWriter:line`/`ErpMfgBatchGenealogyBizModel:line`（recallReport 降级）；L4 引用 `Test*.java#method`（注明断言强度；业财异常路径零覆盖 + best-effort 写失败无测试）；L5 复用 A4.2a/A2.6a + 本切片差异。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口/偏离**（逐条验收标准对照）：UC-MFG-11——①KPI 卡片值实时聚合(非硬编码常量)；②预警阈值来自 config(非硬编码)；③行级权限(orgId/deptId/costCenter 过滤生效)。UC-MFG-12——④完工过账触发差异计算；⑤6 类差异(MATERIAL_USAGE/LABOR_EFFICIENCY/LABOR_RATE/OVERHEAD/VOLUME/SUBCONTRACT)逐条写 ErpMfgCostVariance；⑥PPV 归采购域不重复计入(PURCHASE_PRICE_VARIANCE 由 inventory InvPostingDispatcher.dispatchPurchasePriceVariance 捕获)；⑦多维分组聚合报表(工作中心/产品/期间/差异类型)。UC-MFG-13——⑧完工入库记录 input→output 关系；⑨前向追溯(outputLotId→inputLotId 全集)；⑩反向追溯(inputLotId→outputLotId 全集)；⑪多级递归追溯；⑫召回报告识别受影响成品批次（**降级版 RecallReport.degraded=true 是否满足"识别受影响成品批次"，位置/去向查询缺失裁决 successor**）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` **resolved finding HEAD 复核**：对差异/批次相关 finding（P1-MA4-007 完工编排差异过账吞异常 / P1-MA4-009 业财异常路径测试有效性 / P1-MA4-010 委外 issue/receipt 过账吞咽无闭环 / P1-MA4-011 CostRollup 测试有效性 / P1-MA4-012 跨域 daoFor 投影——**resolved 状态执行时经 arm-index grep 确认，未确认者按"未定"处理**）在当前 HEAD 代码实际落地（按逻辑非行号核验），逐条记录复核结论（已落地/回退/部分落地/documented simplification 仍 open successor）。**P1-MA4-007 为会计正确性类（差异凭证悬挂），Q4 无例外，HEAD 复核为关闭门控关键证据**。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（P0/P1/P2/接受）：UC-MFG-12 P1-MA4-007 完工触发差异过账吞异常（会计正确性类 Q4 无例外 → dedup P1-MA4-007，HEAD 复核确认悬挂则维持 P1 触发 MR1；已 resolved 则记录闭环）；业财异常路径零覆盖（dedup P1-MA4-009）。UC-MFG-13 召回报告降级（按 §4 Q1 L1"识别所有受影响成品批次"——降级版满足"识别"→ 倾向接受/P2 successor；位置/去向为增强维度归 inventory successor，核验 successor 触发条件登记）；best-effort 写失败（Decision 3 可观测性兜底 → 存疑点交 MA4）。UC-MFG-11 KPI/阈值/权限（执行时 HEAD 核验：硬编码倾向 P1、权限缺失倾向 P0/P1 安全类）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-MFG-11/12/13 各一矩阵行（验收标准全覆盖），L1 逐字引用、L3 含行号、L4 注明断言强度（含业财异常路径零覆盖 + best-effort 无测试标注）、L5 标注复用 A4.2a/A2.6a 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）+ §2 判据编号；候选缺口 ①-⑫ 有明确分级（非悬空"待查"）；UC-MFG-13 召回报告降级结论含 §4 裁决；P1-MA4-007 HEAD 复核结论已记录（会计正确性类关键证据）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md`（补 §6-§9，报告定稿）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（矩阵 + 结论已出）

- [ ] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 前 grep `arm-index.md` mfg 差异/批次/看板同域同控制点（如 P1-MA4-007/P1-MA4-009）后裁决——同根因同控制点 → 复用既有 ID（追加 RC 交叉引用注记，不新建）；新根因/新功能点（如 UC-MFG-13 召回报告降级 = 需求契约视角新维度 / UC-MFG-11 看板 KPI 硬编码若发现）→ 新建 `P0-RC-xxx`/`P1-RC-xxx` 并列明与既有 finding 的差异依据。**特别注意**：UC-MFG-12 差异过账吞异常若与 A1.9/A4.2a P1-MA4-007 同根因则交叉引用而非重复新建。禁止未经比对直接新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 的复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记本切片 L5 无法静态定论、需运行时确认的点（如完工触发差异过账失败运行时 posted 悬挂、best-effort 基因链写失败运行时缺口、看板 KPI 运行时聚合 vs 硬编码、行级权限运行时过滤；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 在报告登记并在本计划记录"已触发 MR0 追加 R0.n 实体行"（本计划不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**（区分 reporter vs CI 门控）。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2/MA4 报告差异增量声明：声明复用 A4.2a（ProductionVarianceDispatcher 调用点 + 完工触发编排 reportCompletion:227-239 + P1-MA4-007/009 finding）+ A4.2b（ProductionVarianceCalculator 6 类算术 PASS + dispatchVarianceAlertIfOverThreshold 降级 + BatchGenealogyWriter 幂等 + 重算幂等四步链 + P1-MA4-010/011/012 finding）+ A2.6a（完工 COMPLETED 触发点）已证实结论，列明本切片只补的需求视角差异（UC-MFG-11 看板 KPI/阈值/权限符合性 + UC-MFG-12 6 类完整性与 PPV 归属 + UC-MFG-13 追溯链完整性与召回报告降级 + resolved finding HEAD 复核）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（MA1 finding 区），既有行追加 RC 交叉引用注记。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查 §1-§9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据（无未经比对新建）
- [ ] 新 RC finding 已写入 `arm-index.md` 对应分区；静态存疑点清单已登记（供 A4.2 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_03d19fb47ffe3ZtNdx4KVu6nl1`，fresh session，未起草本计划）。逐项实测核验：roadmap 对齐（A1.11 / UC-MFG-11/12/13 / Deps=0.2 done / Skill）、3 UC 锚点 :195/:216/:238 全匹配（完整枚举无跳无合并）、L3 路径存在（ErpMfgDashboardBizModel + ProductionVarianceCalculator 6 类 :131-203 + ProductionVarianceDispatcher dispatchIfApplicable :111-117/reverseIfExists + BatchGenealogyTracer/Writer/BizModel + 4 config key）、L4 6 测试全存在、跨切片边界正确（UC-MFG-07 归 A1.9）、Closure Gates 删门控有据、保护区域 ask-first 全合规。**1 阻塞 issue**：L5 误将 ProductionVarianceCalculator 6 类算术 PASS + 重算幂等归 A4.2a，实则 A4.2a:244 显式交接给 A4.2b（`...mfg-mrp-quality-code-quality.md`），A4.2b:54 才是 6 类算术 PASS 来源且覆盖 BatchGenealogyWriter + P1-MA4-010/011/012。修复：补 A4.2b 为 L5 dedup 输入、修正 A4.2a 范围、arm-index 衔接 + Phase 1 HEAD 复核 + §9 增列 P1-MA4-010/011/012 + Task Route 加 A4.2b。
- Independent draft review iteration 2: `acceptable as-is`（独立子代理 `ses_03d15960cffe2at0KFGM8941f3`，fresh session，未起草本计划）。复审确认 iter1 阻塞 issue 已完全解决：A4.2a 范围正确收窄（ProductionVarianceDispatcher 调用点 + 完工触发编排 reportCompletion:227-239 + P1-MA4-007/009，A4.2a:244 显式交接）、A4.2b 增列为独立 L5 dedup 输入（6 类算术 PASS A4.2b:54 + dispatchVarianceAlertIfOverThreshold A4.2b:55 + BatchGenealogyWriter 幂等 A4.2b:56 + 重算幂等四步链 + P1-MA4-010/011/012，行号引用实测命中）。arm-index 衔接 / Task Route / Phase 1 HEAD 复核 / Phase 2 §9 五处一致更新。回归项全 intact（UC 锚点 :195/:216/:238 三矩阵行、跨切片边界 UC-MFG-07→A1.9、只读审计删门控、item typing + Skill）。无新阻塞 issue。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——审计报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + resolved finding HEAD 复核 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.11 报告 9 段齐全 + UC-MFG-11/12/13 逐矩阵行 + resolved finding HEAD 复核 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.11 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按方法论 §10 经 MR0（P0 即时通道）/ MR1（R1.0 展开 RC-R1.n，P1 批量）实施；触及会计过账逻辑（差异过账凭证/重算红冲 PRODUCTION_VARIANCE 路径）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本审计闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: <结束审计通过后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理 fresh session>
- Evidence: <报告产物 + arm-index 登记 + HEAD 锚点 + 日志同步 + draft review 证据 + 只读零回归证明>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
