# ARM MA2 — manufacturing 计划规划状态机业务审查（A2.6b，S 级拆分 2/2）

> Audit Status: closed
> Mission: audit-remediation
> Work Item: A2.6b manufacturing 状态机审查 — MRP/BOM（S 级拆分 2/2）
> Source Plan: `docs/plans/2026-07-28-0109-2-audit-remediation-ma2-mfg-mrp-bom-state-machine.md`
> Skill: `docs/skills/state-machine-business-review-prompt.md`（+ 项目定制化层 `docs/skills/README.md §项目定制化层`）
> Reviewed: 2026-07-28
> Scope: **MRP 计划头状态机**（`ErpMfgMrpPlan.status` dict `erp-mfg/mrp-status` 5 态：DRAFT/RUNNING/COMPLETED/FIRMED/CANCELLED，无 approveStatus 审批轴）+ **运营预测状态机**（`ErpMfgForecast.status` dict `erp-mfg/forecast-status` 4 态：DRAFT/APPROVED/CONSUMED/CANCELLED；ForecastLine 无 status 列）+ **建议单隐式生命周期**（`ErpMfgMrpPlanLine` 无 status 列，生命周期由 `orderType` + `isFirmed` 布尔 + `convertedBillCode` 承载）+ **BOM 激活**（`ErpMfgBom.isActive` 布尔，无状态机）+ **仿真状态机**（config-gated `erp-mfg.simulation-enabled` 默认 false，独立 `erp-mfg/simulation-status` 4 态 DRAFT/RUNNING/COMPLETED/ARCHIVED）。生产执行类状态机（工单/作业卡/领料/委外）归 A2.6a（done）。
> Related: A2.6a manufacturing 生产执行状态机审查 done（`docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`，本审计复核建议单释放生成的工单骨架 DRAFT / 委外单 APPROVED 绕审批——后者在 scope 内）；A2.5b finance 预算方案状态机 done（`docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`，MRP 计划状态机与预算方案状态机同型——DRAFT→批准类终态 + 结转校验前置）；`docs/plans/2026-07-22-1000-2-manufacturing-mrp-drp-simulation-engine.md`（仿真引擎 owner doc 化——fork 而非触及单次 MRP 路径）。

## 1. 裁决

**Verdict: pass（零 P0、3 项新 P1、2 项新 P2 watch-only）**

manufacturing 计划规划五组件状态机（MRP 计划头 5 态 + 预测 4 态 + 建议单隐式生命周期 isFirmed + BOM isActive + 仿真 4 态 config-gated）核心契约经实仓逐项证据确认：状态迁移守卫齐全（`requirePlan`/`requireReleasable`/`requireSimulationEnabled`/`requireScenario`/`requireVersion` 前置校验）、`@BizMutation` 事务边界覆盖 MRP 运算全链（`ErpMfgMrpPlanBizModel.runMrp:36` → `MrpEngine.runMrp` 整体事务包裹 RUNNING→COMPLETED 写入）+ 释放全链（`ErpMfgMrpPlanLineBizModel.releaseXxxRequest:29/39/46` → `MrpReleaseService` 跨域 saveEntity + markFirmed + advancePlanToFirmedIfComplete 整体事务包裹）、幂等守卫完整（已 firmed 行重复释放抛 `ERR_MRP_LINE_ALREADY_FIRMED`）、仿真 E2 fork 单次路径零触及（`SimulationMrpEngine` 不调 `MrpEngine.runMrp`，全 grep 仅 javadoc 引用）。

**关键裁决（计划假设证伪/确认）**：

| 计划假设 | 裁决 | 证据 |
|---|---|---|
| MRP 运算 RUNNING 中途异常状态悬挂（status 未回滚 DRAFT） | **证伪** | `ErpMfgMrpPlanBizModel.runMrp:36` 是 `@BizMutation`（Nop 平台自动事务），事务包裹整个 `mrpEngine.runMrp(planId, demandAggregator.aggregate(planId))` 调用——含 `MrpEngine.runMrp:77-100` 内的 setStatus RUNNING(:84) + updateEntity(:85) + clearLines(:88) + processMaterial 循环(:93-96) + setStatus COMPLETED(:98-99)。若 processMaterial 在 :94-96 抛异常 → 事务回滚 → RUNNING 写入(:85)一并回滚，DB 中 status 保持 DRAFT（事务开始前的值）。**无悬挂**——事务边界覆盖状态机一致性。 |
| 释放路径生成目标单失败致建议单 isFirmed 与目标单悬挂半状态（事务回滚缺口） | **证伪** | `ErpMfgMrpPlanLineBizModel.releasePurchaseRequest:29/releaseWorkRequest:39/releaseSubcontractRequest:46` 均 `@BizMutation`，事务包裹 `MrpReleaseService.releaseXxxRequest` 全链：`requireReleasable`(:115) → `releaseToXxx`（跨域 `daoFor(ErpPurOrder).saveEntity`/`daoFor(ErpMfgWorkOrder).saveEntity`/`daoFor(ErpMfgSubcontractOrder).saveEntity`）→ `markFirmed`(:129-133 isFirmed=true + convertedBillCode + updateEntity) → `advancePlanToFirmedIfComplete`(:218-236)。任一环节失败（跨域 saveEntity 抛异常）→ 事务回滚 → markFirmed 不执行 → isFirmed 保持 false + convertedBillCode 保持 null + 目标单不持久化。**无悬挂半状态**——事务边界覆盖释放原子性。 |
| SUBCONTRACT_REQUEST 释放 config-gated on 时生成 APPROVED 委外单绕审批破坏业务规则 | **config-gated 裁决 P1 治理，非 P0** | `MrpReleaseService.releaseSubcontractRequest:98-113` config-gated `erp-mfg.subcontract-release-enabled`（默认 false，:266-276 `isSubcontractReleaseEnabled` 解析）。关闭时(:99-102)抛 `ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE` 拒绝；开启时(:199-201)直接置 `docStatus=APPROVED + approveStatus=APPROVED` 绕过 `ErpMfgSubcontractOrderProcessor` 审批管道（submit→approve）。MrpReleaseService javadoc:96 已声明"对齐 MRP 自动释放不经人工审批管道的 O-4 架构豁免"。config-gate 默认 off 控制风险暴露面 → **裁决为 P1 治理**（O-4 豁免登记完整性，见 P1-MA2-038），非 P0（不破坏默认配置下业务规则）。 |
| MRP CANCELLED 是否可达 | **不可达 → P1-MA2-036** | 全 `src/main` grep `MRP_STATUS_CANCELLED` 仅 `ErpMfgConstants.java:95` 常量定义；无任何 `setStatus(MRP_STATUS_CANCELLED)` 调用；`ErpMfgMrpPlanBizModel` 仅 `runMrp` 无 `cancelMrp` mutation。dict `erp-mfg/mrp-status:77` 含 CANCELLED 项但无 writer → dict 死状态。按 finance A2.5a P1-MA2-031 + A2.6a P1-MA2-035 同型裁决。 |
| 预测 CONSUMED 是否可达 | **不可达 → P1-MA2-036（合并）** | 全 `src/main` grep `FORECAST_STATUS_CONSUMED` 仅 `ErpMfgConstants.java:161` 常量定义 + `ErpMfgForecastBizModel.java:54` 作为 cancel 守卫（拒绝已 CONSUMED 的 cancel），**无 setStatus(CONSUMED) 写入**；`DemandAggregator.collectForecastDemands:175` 只读 status=APPROVED 预测，**不回写 CONSUMED**。dict `erp-mfg/forecast-status:129` 含 CONSUMED 项但无 writer → dict 死状态。owner doc `mrp.md §实现偏离补注 line 88` 已注记 Deferred（"CONSUMED 状态值预留但本期不自动迁移"），但 `state-machine.md` 无预测独立状态机章节未声明死状态。按 P1-MA2-036 合并裁决（与 MRP CANCELLED 同根因：dict 死状态）。 |
| FIRMED 是动作完成还是等待点 | **动作完成（终态）** | `MrpReleaseService.advancePlanToFirmedIfComplete:218-236` 当所有行 isFirmed=true 时置头 status=FIRMED(:233)。FIRMED 是"所有建议单已释放"的动作结果，无再运算回 DRAFT 路径（无 unfirmMrp mutation）→ 终态语义。owner dict label "已确认(转工单)" 表达动作完成。**PASS**。 |
| 建议单无 status 列用 isFirmed 布尔（隐式状态机的清晰性） | **设计接受** | `ErpMfgMrpPlanLine` 无 status 列（ORM `orm.xml:807-853`），生命周期由 `orderType`（dict 4 态 PLANNED_ORDER/PURCHASE_REQUEST/WORK_ORDER_REQUEST/SUBCONTRACT_REQUEST）+ `isFirmed` 布尔（默认 false :824）+ `convertedBillCode`(:825) 三字段隐式承载。释放=翻转 isFirmed=true + 填 convertedBillCode。这是"条件作为状态"反模式的合法变体——建议单生命周期简单（未释放/已释放两态），用布尔比独立 status 列更清晰（避免与头级 status 语义混淆）。**PASS**（但 owner doc `mrp.md §建议单释放` 文字"释放后建议单状态标记为 RELEASED"与实现不一致 → P1-MA2-037 owner doc 漂移）。 |
| BOM isActive 布尔无状态机（是否够） | **设计接受** | `ErpMfgBom.isActive` 布尔（默认 true `orm.xml:202`）+ `isDefault`(:203)。BOM 生命周期简单（有效/无效两态），用布尔比 status 列更轻量。`ErpMfgBomBizModel`(69 行) 无 approve/activate/deactivate 方法（仅 findDefaultBom/explode/rollupCost），激活/停用经通用 CRUD update isActive。owner doc `bom-and-routing.md` 未声明 BOM 状态机（一致）。**PASS**。 |
| 仿真 promote 失败回滚 | **证伪** | `ErpMfgMrpScenarioBizModel.promoteToFormalPlan:59` `@BizMutation` 事务包裹 `SimulationMrpEngine.promoteToFormalPlan:171-230`：requireVersion 守卫(:172-181) → 新建 DRAFT plan(:188-197) → 复制计划行(:200-222) → 回写版本 promotedPlanId + ARCHIVED(:225-227)。任一环节失败 → 事务回滚 → 版本保持 COMPLETED 不被 ARCHIVED。**无悬挂**。 |
| 仿真 fork 是否触及单次 MRP 路径 | **证伪（零触及）** | `SimulationMrpEngine.java` 全文 grep `MrpEngine.runMrp` / `MrpReleaseService` 仅 javadoc 引用（:51 注释"本算法对齐 MrpEngine.runMrp，任何 MrpEngine 算法变更须同步本类"），无运行时调用。仿真 fork 自己的 processMaterial/lotSize/mfgLeadDays/purLeadDays/availableQuantity/topDemandsByMaterial(:286-443)，复用注入的 `BomExpander`/`DemandAggregator`（既有 bean）。**单次 MRP 路径零触及**——既有 200+ manufacturing 测试不受影响。 |

### 1.1 审查范围

- **MRP 计划头状态机**：`ErpMfgMrpPlan`（`orm.xml:768-804`，列 `status` 在 :777，无 approveStatus）+ `MrpEngine.java`（277 行，`runMrp:77-100` 入口守卫 + RUNNING/COMPLETED 写入 + clearLines + processMaterial 递归）+ `ErpMfgMrpPlanBizModel.java`（40 行 Facade，`runMrp:36` `@BizMutation`）+ `ErpMfgMrpPlanLineBizModel.java`（53 行 Facade，3 个释放 mutation）。
- **运营预测状态机**：`ErpMfgForecast`（`orm.xml:896-928`，列 `status` 在 :906）+ `ErpMfgForecastLine`（`orm.xml:931-973`，无 status 列）+ `ErpMfgForecastBizModel.java`（66 行，`approve:34` / `cancel:50` + CONSUMED Deferred javadoc:21）。
- **建议单隐式生命周期**：`ErpMfgMrpPlanLine`（`orm.xml:807-853`，`orderType:816` + `isFirmed:824` + `convertedBillCode:825`）+ `MrpReleaseService.java`（277 行，`releasePurchaseRequest:67` / `releaseWorkRequest:83` / `releaseSubcontractRequest:98` + `requireReleasable:115` + `markFirmed:129-133` + `advancePlanToFirmedIfComplete:218-236` + O-4 豁免 javadoc:42-53,148-149,202）。
- **需求单**：`ErpMfgMrpDemand`（`orm.xml:856-893`，无 status 列，`demandSource:865` 是分类非生命周期）+ `ErpMfgMrpDemandBizModel`（纯 CrudBizModel 无自定义方法）+ `DemandAggregator.java`（299 行，`aggregate:68` + 跨域只读 `collectSalesOrderDemands:84`/`collectSafetyStockDemands:123`/`collectForecastDemands:160` + `clearSynthesized:257`）。
- **BOM 激活**：`ErpMfgBom`（`orm.xml:193-230`，`isActive:202` + `isDefault:203`）+ `ErpMfgBomBizModel.java`（69 行，`findDefaultBom:45` / `explode:56` / `rollupCost:65`，无状态机方法）。
- **仿真状态机**：`ErpMfgMrpScenario`（`orm.xml:1534-1569`，`status:1543` dict `erp-mfg/simulation-status`）+ `ErpMfgMrpScenarioVersion`（`orm.xml:1572-1606`，`status:1581` + `promotedPlanId:1582`）+ `SimulationMrpEngine.java`（513 行 fork，`runSimulation:96-164` + `promoteToFormalPlan:171-230`）+ `ErpMfgMrpScenarioBizModel.java`（81 行，`requireSimulationEnabled:72-79` config-gate）。
- **跨域访问（释放路径）**：`MrpReleaseService` 跨域写 `ErpPurOrder:137,150` + `ErpPurOrderLine:152,161`（DRAFT/UNSUBMITTED）+ `ErpMfgSubcontractOrder:187,199-201`（APPROVED 绕审批）+ `ErpMfgSubcontractOrderLine:205,214`。`MrpEngine`/`DemandAggregator` 跨域只读 `ErpInvStockBalance`/`ErpMdMaterial`/`ErpSalOrder`（P1-MA1-022 同型）。
- **owner doc**：`docs/design/manufacturing/mrp.md`（MRP 流程/建议单释放/实现偏离补注 + CRM vs 运营预测关系 + 仿真引擎关系）+ `bom-and-routing.md`（BOM/工艺，无状态机）+ `simulation-engine.md`（仿真状态机 + 单次路径零触及声明）+ `state-machine.md`（owner doc——无 MRP/预测独立状态机章节，散落在 mrp.md §实现偏离补注）+ `docs/architecture/posting-exemptions.md §MrpReleaseService`。
- **测试**：`TestErpMfgMrpEngine`（引擎运算/净需求/批量/提前期）+ `TestErpMfgMrpEndToEnd`（聚合→引擎→释放 + 重复释放幂等 + orderType 不匹配拒绝 + 部分/全部释放 FIRMED 头级回写）+ `TestErpMfgMrpSimulation`（仿真引擎/场景/版本/promote/参数覆盖）+ `TestErpMfgForecastCrudSmoke` + `TestErpMfgForecastSource`（预测作为需求源被消费 + approve/cancel 状态机 + 非法迁移拒绝）+ `TestErpMfgBomExplosion`（BOM 多级展开）。无独立建议单释放测试类（覆盖在 MrpEndToEnd 内）+ 无 BOM 状态机测试（BOM 无状态机）。

### 1.2 可达性摘要

- **MRP 计划头 5 态中 4 态可达**：null/DRAFT→RUNNING→COMPLETED（MrpEngine.runMrp）；COMPLETED→FIRMED（MrpReleaseService.advancePlanToFirmedIfComplete 全行 isFirmed 时）；**CANCELLED 无任何 setStatus 调用 → 不可达**（P1-MA2-036）。
- **预测 4 态中 3 态可达**：DRAFT→APPROVED（approve）；DRAFT/APPROVED→CANCELLED（cancel）；**CONSUMED 无任何 setStatus 调用 → 不可达**（P1-MA2-036 合并）。DemandAggregator 只读 APPROVED 不回写 CONSUMED。
- **建议单隐式生命周期**：isFirmed false→true（释放 markFirmed）可达；已 firmed 拒绝重复释放（幂等）；**无 unfirmed 回退路径**（释放不可逆——设计接受，与工单 COMPLETED 无 reverseCompletion 同型 successor 裁定）。
- **BOM isActive**：true↔false 经通用 CRUD update 可达（无独立状态机迁移方法）。
- **仿真 4 态全部可达**：DRAFT→RUNNING→COMPLETED（runSimulation）；COMPLETED→ARCHIVED（promoteToFormalPlan 版本级）；场景头 DRAFT→RUNNING→COMPLETED（runSimulation 头级）。

### 1.3 角色/权限摘要

owner doc 未显式定义 MRP 计划/预测/建议单释放的角色矩阵（散落在 `mrp.md §关键业务规则` + `state-machine.md` 无独立章节）。状态机层面：MRP 运算（计划员）/ 预测 approve（计划主管）/ 建议单释放（采购员 releasePurchaseRequest / 计划员 releaseWorkRequest / config-gated releaseSubcontractRequest）。本审计未做权限注解运行时验证（归 A4/A6 平台合规与权限审计），状态机层面无角色漂移反模式。

**危险操作门控**：
- `runMrp`（重算清除既有计划行 + 重写）：守卫 status=DRAFT 或 null（`MrpEngine:79-83` 抛 `ERR_MRP_INVALID_PLAN_STATUS`）；
- `releasePurchaseRequest`（生成采购单骨架影响下游——单价/金额=0 须补录）：守卫 orderType=PURCHASE_REQUEST + 未 firmed；
- `releaseSubcontractRequest`（生成 APPROVED 委外单绕审批管道）：config-gated `erp-mfg.subcontract-release-enabled` 默认 false 阻断（P1-MA2-038）；
- `promoteToFormalPlan`（仿真转正式）：守卫 version.status=COMPLETED + promotedPlanId 为空（防重复转正）。

### 1.4 外部依赖摘要

- **建议单释放跨域写 ErpPurOrder/ErpPurOrderLine**（O-4 豁免已登记）：`MrpReleaseService.releaseToPurchaseOrder:135-163` 经 `daoProvider.daoFor(ErpPurOrder).saveEntity` 直接持久化采购单骨架（DRAFT/UNSUBMITTED，单价/金额=0），绕过 `IErpPurOrderBiz` 审批管道。`docs/architecture/posting-exemptions.md §MrpReleaseService` 已登记豁免 + 理由 + 风险 + 补偿机制 + 收敛条件。
- **建议单释放跨域写 ErpMfgSubcontractOrder/ErpMfgSubcontractOrderLine**（O-4 豁免**登记缺失**）：`MrpReleaseService.releaseToSubcontractOrder:185-216` 经 `daoProvider.daoFor(ErpMfgSubcontractOrder).saveEntity` 直接持久化委外单（APPROVED 绕审批，:199-201）+ 委外单行。虽 ErpMfgSubcontractOrder 同属 manufacturing 域（非跨模块），但 MrpReleaseService javadoc:202 已声明"O-4 架构豁免：MRP 自动释放不经人工审批管道"，**`posting-exemptions.md §MrpReleaseService` 未登记此委外单豁免**——与 P1-MA1-029（ErpCtInvoicePlanBizModel 跨域写半治理）同型半治理（P1-MA2-038）。
- **需求聚合跨域只读**：`DemandAggregator` 跨域读 `ErpSalOrder`/`ErpSalOrderLine`/`ErpMdMaterial`/`ErpInvStockBalance`（只读，P1-MA1-022 已登记 MR1）；`MrpEngine` 跨域读 `ErpInvStockBalance`/`ErpMdMaterial`/`ErpMfgBomOperation`（只读，P1-MA1-022 同型）。
- **APS 排程来源建卡**（config-gated）：仿真 promote 后产 DRAFT plan，单次释放路径不变（`simulation-engine.md §Decision D`）。
- **CRM 金额预测 vs 运营数量预测 disaggregation**：owner doc `mrp.md §CRM 销售预测 vs 运营需求预测的关系` 已裁定 Deferred（金额→数量分解依赖售价策略 + 多币种 + 折扣，误差大；归 successor）。
- **外部步骤失败阻断状态迁移**：@BizMutation 事务回滚保证（runMrp 异常回滚 RUNNING / 释放异常回滚 isFirmed / promote 异常回滚 ARCHIVED）。**PASS**。

### 1.5 剩余风险

- MRP CANCELLED + 预测 CONSUMED dict 死状态（P1-MA2-036，MR1）；
- `mrp.md §建议单释放` "RELEASED" 文字 vs 实现 isFirmed 布尔漂移（P1-MA2-037，MR1）；
- MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失（P1-MA2-038，MR1）；
- `state-machine.md` 无 MRP/预测独立状态机章节（P2-MA2-045）；
- 建议单释放生成采购单单价/金额=0 + 委外单加工费=0（owner doc `mrp.md §实现偏离补注` 已注记残留，须采购员补录）；
- 已 firmed 建议单不可逆（无 unfirmed 路径——设计接受，释放后改主意的回退经目标单 DRAFT 状态由采购员/计划员废弃目标单，非建议单层回退）；
- 仿真算法漂移风险（SimulationMrpEngine 头部注释 :51 已声明"任何 MrpEngine 算法变更须同步本类"，Non-Goals 限定本期不引入新算法维度）；
- 并发释放同建议单（isFirmed 无 @Version——双读双写？交接 A2.17）。

---

## 2. 状态图与转换矩阵

### 2.1 MRP 计划头状态机（`ErpMfgMrpPlan.status`，dict `erp-mfg/mrp-status` 5 态，无审批轴）

```
                    runMrp                       runMrp
[null/DRAFT] ──────────────────► [RUNNING] ──────────────────► [COMPLETED]
                                     │                              │
                                     │ (异常 → @BizMutation         │ 全部行 isFirmed=true
                                     │  事务回滚 RUNNING            │ (advancePlanToFirmedIfComplete)
                                     │  不持久化，回 DRAFT)          ▼
                                     │                          [FIRMED]  (终态)

        [CANCELLED]   ❌ 不可达（dict 死状态，无 setStatus writer）
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| null/DRAFT→RUNNING | runMrp | `MrpEngine:84`（setStatus RUNNING + :85 updateEntity） | `:79-83`（requireStatus null 或 DRAFT，否则 ERR_MRP_INVALID_PLAN_STATUS） | PASS（事务包裹，异常回滚 RUNNING 不持久化） |
| RUNNING→COMPLETED | runMrp（运算完成） | `MrpEngine:98-99`（setStatus COMPLETED + updateEntity） | processMaterial 循环完成（无显式守卫，循环正常结束即置 COMPLETED） | PASS |
| COMPLETED→FIRMED | advancePlanToFirmedIfComplete | `MrpReleaseService:218-236`（:232-234 全行 isFirmed 时 setStatus FIRMED + updateEntity） | `:225-231`（!lines.isEmpty + 全行 isFirmed=true） | PASS（部分释放时头保持 COMPLETED，全释放时升 FIRMED） |
| **任意→CANCELLED** | （未实现） | **无 setStatus(MRP_STATUS_CANCELLED) 调用** | — | **FAIL → P1-MA2-036**（dict 死状态，无 cancelMrp mutation） |

**MRP 计划头终态**：FIRMED（真终态，无再运算回 DRAFT 路径——设计接受，若需重算新建 plan 或删除重建）；CANCELLED（dict 声明但不可达）。

### 2.2 运营预测状态机（`ErpMfgForecast.status`，dict `erp-mfg/forecast-status` 4 态）

```
                  approve
[DRAFT] ──────────────────► [APPROVED] ──────────► (DemandAggregator 只读消费，不回写状态)
    │                            │
    │ cancel                     │ cancel
    ▼                            ▼
[CANCELLED]  ◄────────────────────────
(终态)

        [CONSUMED]   ❌ 不可达（dict 死状态，无 setStatus writer；owner doc mrp.md:88 已注记 Deferred）
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| DRAFT→APPROVED | approve | `ErpMfgForecastBizModel:34-46`（:43 setStatus APPROVED + updateEntity） | `:37-42`（requireStatus DRAFT，否则 ERR_FORECAST_ILLEGAL_STATUS_TRANSITION） | PASS |
| DRAFT/APPROVED→CANCELLED | cancel | `ErpMfgForecastBizModel:50-64`（:61 setStatus CANCELLED + updateEntity） | `:53-60`（拒绝已 CANCELLED 或 CONSUMED 的 cancel） | PASS（终态拒绝重复 cancel） |
| APPROVED→（被消费，无状态回写） | DemandAggregator.collectForecastDemands | `DemandAggregator:175`（filter status=APPROVED 只读） | 只读不回写 | ⚠️ **owner doc mrp.md:88 已注记 Deferred**（"CONSUMED 状态值预留但本期不自动迁移"）；预测保持 APPROVED 漂移，每次 MRP 运行都消费同一 APPROVED 预测（无去重——设计接受，预测是计划期输入非一次性消费） |
| **APPROVED→CONSUMED** | （未实现） | **无 setStatus(FORECAST_STATUS_CONSUMED) 调用** | — | **FAIL → P1-MA2-036**（dict 死状态，owner doc 已注记 Deferred 但 state-machine.md 未声明） |

**预测终态**：CANCELLED（真终态）；APPROVED（被消费后保持 APPROVED，非真终态但无出边——设计接受，预测可被多计划期重复消费）；CONSUMED（dict 声明但不可达）。

### 2.3 建议单隐式生命周期（`ErpMfgMrpPlanLine` 无 status 列，`orderType` + `isFirmed` + `convertedBillCode` 三字段隐式承载）

```
[orderType=PURCHASE_REQUEST]   isFirmed=false ──releasePurchaseRequest──► isFirmed=true + convertedBillCode=PO-MRP-*
[orderType=WORK_ORDER_REQUEST] isFirmed=false ──releaseWorkRequest──────► isFirmed=true + convertedBillCode=WO-MRP-*
[orderType=SUBCONTRACT_REQUEST] isFirmed=false ──releaseSubcontractRequest (config-gated)──► isFirmed=true + convertedBillCode=SUB-MRP-*
[orderType=PLANNED_ORDER]      isFirmed=false（无释放路径——字典项存在但 MrpEngine 永不产生此 orderType）

幂等：已 isFirmed=true 行重复释放 → ERR_MRP_LINE_ALREADY_FIRMED 拒绝
orderType 不匹配：releasePurchaseRequest 调用于 WORK_ORDER_REQUEST 行 → ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE 拒绝
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| isFirmed=false→true（PURCHASE_REQUEST） | releasePurchaseRequest | `MrpReleaseService:67-78` + `markFirmed:129-133` | `requireReleasable:115`（PURCHASE_REQUEST + 未 firmed）+ supplierId 必填（:68-71） | PASS（生成 ErpPurOrder DRAFT/UNSUBMITTED 骨架 + 头级 advancePlanToFirmedIfComplete） |
| isFirmed=false→true（WORK_ORDER_REQUEST） | releaseWorkRequest | `MrpReleaseService:83-90` + `markFirmed` | `requireReleasable:115`（WORK_ORDER_REQUEST + 未 firmed） | PASS（生成 ErpMfgWorkOrder DRAFT/UNSUBMITTED 骨架 + 头级回写） |
| isFirmed=false→true（SUBCONTRACT_REQUEST） | releaseSubcontractRequest（config-gated） | `MrpReleaseService:98-113` + `markFirmed` | `:99-102`（config `erp-mfg.subcontract-release-enabled` 默认 false 抛 ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE）+ `requireReleasable:115` + supplierId 必填 | ⚠️ config 开启时生成 ErpMfgSubcontractOrder APPROVED 绕审批（:199-201）—— O-4 豁免登记缺失（P1-MA2-038） |
| 已 firmed 行重复释放 | （幂等拒绝） | `MrpReleaseService.requireReleasable:117-120` | isFirmed=true → ERR_MRP_LINE_ALREADY_FIRMED | PASS（幂等守卫完整，TestErpMfgMrpEndToEnd:121-123 覆盖） |
| orderType 不匹配释放 | （拒绝） | `MrpReleaseService.requireReleasable:121-125` | orderType ≠ expected → ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE | PASS（TestErpMfgMrpEndToEnd:136-137 覆盖） |
| **isFirmed=true→false（取消释放）** | （未实现） | **无 unfirmed 路径** | — | 设计接受（释放不可逆；与工单 COMPLETED 无 reverseCompletion 同型 successor 裁定——目标单 DRAFT 状态由采购员/计划员废弃，非建议单层回退） |

**建议单终态**：isFirmed=true（真终态，不可回退）。

### 2.4 BOM 激活（`ErpMfgBom.isActive` 布尔，无状态机）

```
[isActive=true]  ◄──通用 CRUD update──►  [isActive=false]
（默认 true，无独立 activate/deactivate mutation）
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| isActive=true↔false | 通用 CRUD update | `ErpMfgBomBizModel`（继承 CrudBizModel，无自定义方法） | 无独立守卫（通用 CRUD 权限） | PASS（BOM 生命周期简单，布尔比 status 列轻量；`findDefaultBomOrNull` 查询 isDefault=true AND isActive=true 保证默认 BOM 有效性） |

**BOM 终态**：无（isActive 是可逆布尔，无终态语义）。

### 2.5 仿真状态机（`ErpMfgMrpScenario.status` + `ErpMfgMrpScenarioVersion.status`，dict `erp-mfg/simulation-status` 4 态，config-gated `erp-mfg.simulation-enabled` 默认 false）

```
场景头：
[DRAFT] ──runSimulation──► [RUNNING] ──(fork 计算 + 写版本)──► [COMPLETED]
                                                              （无 ARCHIVED 头级迁移；版本级 ARCHIVED）

版本：
（runSimulation 创建）→ [COMPLETED] ──promoteToFormalPlan──► [ARCHIVED]  (终态)
                                │
                                └─ promotedPlanId 防重复转正守卫
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| 场景 DRAFT→RUNNING | runSimulation | `SimulationMrpEngine:114-115`（setStatus RUNNING + saveOrUpdateEntity） | `:98-102`（requireStatus DRAFT）+ `requireSimulationEnabled` config-gate | PASS |
| 场景 RUNNING→COMPLETED | runSimulation（完成） | `SimulationMrpEngine:160-161`（setStatus COMPLETED + saveOrUpdateEntity） | fork 计算完成（无显式守卫，正常结束即置 COMPLETED） | PASS |
| 版本（创建）→COMPLETED | runSimulation | `SimulationMrpEngine:151-157`（version setStatus COMPLETED + saveEntity） | 场景 RUNNING + fork 计算 + computed plan COMPLETED | PASS |
| 版本 COMPLETED→ARCHIVED | promoteToFormalPlan | `SimulationMrpEngine:225-227`（version setStatus ARCHIVED + setPromotedPlanId + saveOrUpdateEntity） | `:173-176`（promotedPlanId 为空防重复转正）+ `:177-181`（requireStatus COMPLETED） | PASS |
| 仿真入口 config-gate | requireSimulationEnabled | `ErpMfgMrpScenarioBizModel:72-79`（config false 时抛 ERR_MFG_SIMULATION_DISABLED） | config `erp-mfg.simulation-enabled` 默认 false | PASS（config-gate 门控仿真入口，不保护单次 MRP 路径——E2 零触及已保证） |

**仿真终态**：场景头 COMPLETED（无 ARCHIVED 头级迁移，版本 ARCHIVED 是版本级终态）；版本 ARCHIVED（真终态，promotedPlanId 防重复转正）。

---

## 3. 10 维度审查裁决

### 3.1 维度「状态定义」 — ⚠️ FAIL（MRP CANCELLED + 预测 CONSUMED 死状态）

**MRP 计划头 5 态语义清晰性**：DRAFT（等待运算）/ RUNNING（运算中，等待完成）/ COMPLETED（运算完成，等待释放决策）/ FIRMED（所有建议单已释放，终态）/ CANCELLED（dict 声明但**无任何 writer**——按"状态存在但代码无实现"反模式裁决）。FIRMED 是"所有行释放完成"的动作结果（终态），非等待点——语义清晰但 owner dict label "已确认(转工单)" 表达动作完成可接受。**CANCELLED 不可达 → P1-MA2-036**。

**预测 4 态语义清晰性**：DRAFT（等待审批）/ APPROVED（已审批，可被 MRP 消费）/ CONSUMED（dict 声明"已消费"但**无任何 writer**——DemandAggregator 只读 APPROVED 不回写 CONSUMED）/ CANCELLED（终态）。**CONSUMED 不可达 → P1-MA2-036**（owner doc mrp.md:88 已注记 Deferred，但 state-machine.md 无预测独立章节未声明死状态）。

**建议单无 status 列用 isFirmed 布尔**：隐式状态机的清晰性——`orderType`（4 态字典）+ `isFirmed`（布尔）+ `convertedBillCode`（字符串）三字段隐式承载生命周期。这是"条件作为状态"反模式的合法变体（建议单生命周期简单：未释放/已释放两态），用布尔比独立 status 列更清晰（避免与头级 status 语义混淆）。**PASS**（但 owner doc `mrp.md §建议单释放` 文字"释放后建议单状态标记为 RELEASED"与实现 isFirmed 布尔不一致 → P1-MA2-037 owner doc 漂移）。

**BOM isActive 布尔无状态机**：BOM 生命周期简单（有效/无效两态），用布尔比 status 列轻量。owner doc `bom-and-routing.md` 未声明 BOM 状态机（一致）。**PASS**。

**仿真 4 态语义清晰性**：DRAFT/RUNNING/COMPLETED/ARCHIVED 语义清晰，每个状态清楚表达"等待什么"。**PASS**。

### 3.2 维度「转换完整性」 — ⚠️ FAIL（MRP CANCELLED + 预测 CONSUMED 转换缺失）

**MRP 计划**：迁移矩阵（§2.1）覆盖 null/DRAFT→RUNNING→COMPLETED→FIRMED 主路径，每个有前置守卫。**CANCELLED 无写入路径**（无 cancelMrp mutation + 无 setStatus(CANCELLED) 调用）——迁移缺失，dict 死状态。**FAIL → P1-MA2-036**。

**预测**：DRAFT→APPROVED（approve）/ DRAFT|APPROVED→CANCELLED（cancel）覆盖；**无 SUBMITTED 审核中间态**（APPROVED 直接由 approve 从 DRAFT 迁移，设计接受——预测审批是单步决策非多级审核，与 budget 多级审核不同）；**无 CONSUMED 写入**（DemandAggregator 只读不回写）——转换缺失，dict 死状态。**FAIL → P1-MA2-036**。

**建议单**：isFirmed false→true（释放 markFirmed）/ 已 firmed 拒绝重复（幂等）/ orderType 不匹配拒绝（守卫）全覆盖；**无 unfirmed 回退**（释放不可逆——设计接受）。**PASS**。

**BOM**：isActive true↔false（通用 CRUD）覆盖；无独立状态机迁移方法（设计接受）。**PASS**。

**仿真**：场景 DRAFT→RUNNING→COMPLETED + 版本 COMPLETED→ARCHIVED 全覆盖；promotedPlanId 防重复转正守卫齐全。**PASS**。

### 3.3 维度「终端状态和恢复」 — ✅ PASS

**MRP 计划头终态**：FIRMED（真终态，无再运算回 DRAFT 路径——`MrpEngine:79-83` 守卫 requireStatus DRAFT 或 null，FIRMED 计划不可再运算。若需重算，新建 plan 或删除重建——设计接受）。CANCELLED dict 声明终态但不可达（P1-MA2-036）。**PASS**。

**预测终态**：CANCELLED（真终态，无恢复路径）；APPROVED（被消费后保持 APPROVED，非真终态但无出边——设计接受，预测可被多计划期重复消费，DemandAggregator 每次 MRP 运行都读 APPROVED 预测无去重）；CONSUMED dict 声明终态但不可达（P1-MA2-036）。**PASS**。

**建议单终态**：isFirmed=true（真终态，无 unfirmed 回退——释放不可逆。设计接受：释放后改主意的回退经目标单 DRAFT 状态由采购员/计划员废弃目标单，非建议单层回退。与工单 COMPLETED 无 reverseCompletion 同型 successor 裁定）。**PASS**。

**BOM**：无终态（isActive 可逆布尔）。**PASS**。

**仿真终态**：版本 ARCHIVED（真终态，promotedPlanId 防重复转正）；场景头 COMPLETED（无 ARCHIVED 头级迁移，但场景可经测试 resetScenarioToDraft 直写 DB 重置——`simulation-engine.md §场景状态重置机制` 已文档化此浏览器层范式）。**PASS**。

**归档与活动区分**：MRP 计划经 status（DRAFT/RUNNING/COMPLETED 活动 vs FIRMED 归档）区分；建议单经 isFirmed（false 活动 vs true 归档）+ convertedBillCode 区分；仿真版本经 status + promotedPlanId 区分。**PASS**。

### 3.4 维度「异常路径」 — ✅ PASS

逐项核验：

| 异常场景 | 处理 | 证据 | 裁决 |
|---|---|---|---|
| **MRP 运算 RUNNING 中途异常**（候选 P0） | @BizMutation 事务回滚 RUNNING 不持久化，回 DRAFT | `ErpMfgMrpPlanBizModel.runMrp:36` `@BizMutation` 包裹 `MrpEngine.runMrp:77-100`；processMaterial :94-96 抛异常 → 事务回滚 → :85 updateEntity RUNNING 一并回滚 | **PASS（候选 P0 证伪）**——事务边界覆盖状态机一致性，无悬挂 RUNNING |
| **释放路径生成目标单失败**（候选 P0） | @BizMutation 事务回滚 isFirmed + convertedBillCode + 目标单 | `ErpMfgMrpPlanLineBizModel.releaseXxxRequest:29/39/46` `@BizMutation` 包裹 `MrpReleaseService.releaseXxxRequest` 全链；跨域 saveEntity 失败 → 事务回滚 → markFirmed 不执行 → isFirmed 保持 false + 目标单不持久化 | **PASS（候选 P0 证伪）**——事务边界覆盖释放原子性，无悬挂半状态 |
| 建议单 orderType 不匹配释放 | 拒绝 ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE | `MrpReleaseService.requireReleasable:121-125` | PASS（TestErpMfgMrpEndToEnd:136-137 覆盖） |
| 已 firmed 行重复释放（幂等） | 拒绝 ERR_MRP_LINE_ALREADY_FIRMED | `MrpReleaseService.requireReleasable:117-120` | PASS（TestErpMfgMrpEndToEnd:121-123 覆盖） |
| SUBCONTRACT_REQUEST 释放 config-gated off | 拒绝 ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE | `MrpReleaseService.releaseSubcontractRequest:99-102`（config `erp-mfg.subcontract-release-enabled` 默认 false） | PASS（config-gate 阻断） |
| SUBCONTRACT_REQUEST 释放 config-gated on 生成 APPROVED 委外单 | 直接置 APPROVED 绕审批管道（O-4 豁免） | `MrpReleaseService.releaseToSubcontractOrder:199-201` | ⚠️ **P1-MA2-038**（O-4 豁免登记缺失；config-gate 默认 off 控制风险暴露面，裁决为 P1 治理非 P0） |
| 预测 cancel 已 CONSUMED/CANCELLED | 拒绝 ERR_FORECAST_ILLEGAL_STATUS_TRANSITION | `ErpMfgForecastBizModel.cancel:53-60` | PASS（TestErpMfgForecastSource:82-84 覆盖） |
| 预测 approve 非 DRAFT | 拒绝 ERR_FORECAST_ILLEGAL_STATUS_TRANSITION | `ErpMfgForecastBizModel.approve:37-42` | PASS |
| 仿真 promote 已 ARCHIVED 版本 | 拒绝 ERR_MFG_SIMULATION_VERSION_ALREADY_PROMOTED | `SimulationMrpEngine.promoteToFormalPlan:173-176`（promotedPlanId 非空守卫） | PASS |
| 仿真 promote 非 COMPLETED 版本 | 拒绝 ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT | `SimulationMrpEngine.promoteToFormalPlan:177-181` | PASS |
| 仿真 runSimulation 非 DRAFT 场景 | 拒绝 ERR_MFG_SIMULATION_SCENARIO_NOT_DRAFT | `SimulationMrpEngine.runSimulation:98-102` | PASS |
| 仿真 promote 失败回滚 | @BizMutation 事务回滚版本保持 COMPLETED | `ErpMfgMrpScenarioBizModel.promoteToFormalPlan:59` `@BizMutation` | PASS |
| 释放生成采购单单价/金额=0 | owner doc 已注记残留，须采购员补录 | `MrpReleaseService.releaseToPurchaseOrder:159-160`（unitPrice/amount=ZERO）+ `mrp.md §实现偏离补注 line 95` | PASS（owner doc 已文档化，DRAFT 状态须人工审核后提交） |

### 3.5 维度「可达性」 — ⚠️ FAIL（MRP CANCELLED + 预测 CONSUMED 死状态）

**MRP 计划头**：null/DRAFT→RUNNING→COMPLETED→FIRMED 全可达；**CANCELLED 不可达**（无 setStatus writer）——dict 死状态，按 finance A2.5a P1-MA2-031 + A2.6a P1-MA2-035 同型裁决 **P1-MA2-036**。

**预测**：DRAFT→APPROVED→CANCELLED 可达；**CONSUMED 不可达**（无 setStatus writer，DemandAggregator 只读不回写）——dict 死状态，owner doc mrp.md:88 已注记 Deferred 但 state-machine.md 未声明。**P1-MA2-036（合并）**。

**建议单**：isFirmed false→true（释放）可达；isFirmed true→false（unfirmed）**不可达**（设计接受，释放不可逆——非缺陷，设计裁定）。

**BOM**：isActive true↔false（通用 CRUD）可达。**PASS**。

**仿真**：场景 DRAFT→RUNNING→COMPLETED + 版本 COMPLETED→ARCHIVED 全可达。**PASS**。

无死循环或不可达终态路径（除上述 dict 死状态）。

### 3.6 维度「角色和权限」 — ✅ PASS

owner doc 未显式定义 MRP 计划/预测/建议单释放的角色矩阵（散落在 `mrp.md §关键业务规则` + `state-machine.md` 无独立章节）。状态机层面：MRP 运算（计划员）/ 预测 approve（计划主管）/ 建议单释放（采购员 releasePurchaseRequest / 计划员 releaseWorkRequest / config-gated releaseSubcontractRequest）。本审计未做权限注解运行时验证（归 A4/A6 平台合规与权限审计），状态机层面无角色漂移反模式（每个迁移绑定执行角色，无审核员执行最终关闭操作）。

**危险操作门控**：
- `runMrp`（重算清除既有计划行）：守卫 status=DRAFT 或 null；
- `releasePurchaseRequest`/`releaseWorkRequest`（生成目标单骨架影响下游——单价/金额=0 须补录）：守卫 orderType 匹配 + 未 firmed；
- `releaseSubcontractRequest`（生成 APPROVED 委外单绕审批）：config-gated 默认 false 阻断（P1-MA2-038）；
- `promoteToFormalPlan`（仿真转正式）：守卫 version.status=COMPLETED + promotedPlanId 为空。

多角色冲突（计划员释放 vs 采购员补录）：经目标单 DRAFT/UNSUBMITTED 状态隔离——释放生成骨架后，采购员在目标域经审批管道补录单价/金额后提交，计划员不再干预目标单。**PASS**。

### 3.7 维度「外部依赖」 — ⚠️ FAIL（MrpReleaseService 委外单 O-4 豁免登记缺失）

- **建议单释放跨域写 ErpPurOrder/ErpPurOrderLine**（O-4 豁免已登记）：`MrpReleaseService.releaseToPurchaseOrder:135-163` 经 `daoProvider.daoFor(ErpPurOrder).saveEntity` 直接持久化采购单骨架（DRAFT/UNSUBMITTED，单价/金额=0），绕过 `IErpPurOrderBiz` 审批管道。`docs/architecture/posting-exemptions.md §MrpReleaseService` 已登记豁免 + 理由 + 风险 + 补偿机制 + 收敛条件。**PASS**。
- **建议单释放跨实体写 ErpMfgSubcontractOrder/ErpMfgSubcontractOrderLine**（O-4 豁免**登记缺失**）：`MrpReleaseService.releaseToSubcontractOrder:185-216` 经 `daoProvider.daoFor(ErpMfgSubcontractOrder).saveEntity` 直接持久化委外单（APPROVED 绕审批，:199-201）+ 委外单行。虽 ErpMfgSubcontractOrder 同属 manufacturing 域（非跨模块），但 MrpReleaseService javadoc:202 已声明"O-4 架构豁免：MRP 自动释放不经人工审批管道"，**`posting-exemptions.md §MrpReleaseService` 未登记此委外单豁免**——与 P1-MA1-029（ErpCtInvoicePlanBizModel 跨域写半治理）同型半治理。**FAIL → P1-MA2-038**。
- **需求聚合跨域只读**：`DemandAggregator` 跨域读 `ErpSalOrder`/`ErpSalOrderLine`/`ErpMdMaterial`/`ErpInvStockBalance`（只读，P1-MA1-022 已登记 MR1）；`MrpEngine` 跨域读 `ErpInvStockBalance`/`ErpMdMaterial`/`ErpMfgBomOperation`（只读，P1-MA1-022 同型）。状态机角度：跨域只读是状态迁移的副作用（需求聚合 + 库存可用量计算），不破坏状态机裁决。异常路径：@BizMutation 事务回滚覆盖跨域读一致性（只读无副作用，事务回滚无影响）。**PASS（仅治理缺陷，状态机角度无悬挂升级）**。
- **APS 排程来源建卡**（config-gated）：仿真 promote 后产 DRAFT plan，单次释放路径不变（`simulation-engine.md §Decision D`）。**PASS**。
- **CRM 金额预测 vs 运营数量预测 disaggregation**：owner doc `mrp.md §CRM 销售预测 vs 运营需求预测的关系` 已裁定 Deferred（金额→数量分解依赖售价策略 + 多币种 + 折扣，误差大；归 successor）。**PASS（owner doc 已裁定 Non-Goal）**。
- **外部步骤失败阻断状态迁移**：@BizMutation 事务回滚保证。**PASS**。

### 3.8 维度「TODO/任务策略」 — ✅ PASS（设计接受无自动 TODO）

owner doc 未定义 MRP 计划/预测/建议单的 TODO 矩阵（`mrp.md` 无 §8 等价章节）。当前实现**无自动 TODO 生成**——这与 nop-app-erp 整体设计一致（无统一 TODO 子系统，运营经仪表板/列表筛选发现滞留单据）。

**残留观察**（非状态机业务正确性问题，归运营成熟度后续阶段）：
- MRP COMPLETED 状态产生"待释放建议单"待办（计划员决策释放采购/工单）——当前经 MRP 建议单列表 isFirmed=false 筛选发现，无自动 TODO；
- 长期 COMPLETED 未释放计划——建议单滞留经计划列表 status=COMPLETED 筛选发现，无自动 TODO；
- CONSUMED 预测回写缺失致预测长期 APPROVED 静默下沉——设计接受（预测可被多计划期重复消费，非一次性消费；DemandAggregator 每次 MRP 运行都读 APPROVED 预测无去重是设计语义，非缺陷）。

**裁决**：设计接受（与全域一致 + A2.6a 同型裁决），不构成本审计 scope 内的状态机缺陷。**PASS**。

### 3.9 维度「场景演练」 — ✅ PASS（含 9 个场景）

#### 场景 A：MRP 快乐路径（DRAFT→聚合需求→RUNNING→COMPLETED→释放采购建议→释放工单建议→全行 isFirmed→头 FIRMED）

证据：`TestErpMfgMrpEndToEnd` happy path。流程：创建 DRAFT plan + 销售订单需求 → runMrp（DRAFT→RUNNING→COMPLETED + clearLines + processMaterial 写 ErpMfgMrpPlanLine isFirmed=false）→ releasePurchaseRequest（PURCHASE_REQUEST 行 isFirmed=true + 生成 ErpPurOrder DRAFT 骨架）→ releaseWorkRequest（WORK_ORDER_REQUEST 行 isFirmed=true + 生成 ErpMfgWorkOrder DRAFT 骨架）→ advancePlanToFirmedIfComplete（全行 isFirmed → 头 FIRMED）。**PASS**。

#### 场景 B：部分释放（仅部分行 firmed，头保持 COMPLETED）

证据：`TestErpMfgMrpEndToEnd:99`（计划尚未全部释放 → 仍 COMPLETED 未 FIRMED）。流程：releasePurchaseRequest（部分行 isFirmed=true）→ advancePlanToFirmedIfComplete 检查全行 isFirmed（仍有未 firmed 行）→ 头保持 COMPLETED。**PASS**。

#### 场景 C：MRP 运算失败回滚（RUNNING 中途异常→status 一致性）

证据：`TestErpMfgMrpEngine`（引擎运算）+ 事务边界分析（`ErpMfgMrpPlanBizModel.runMrp:36` `@BizMutation`）。流程：runMrp setStatus RUNNING + updateEntity → processMaterial 抛异常（如物料缺失/BOM 环检测）→ @BizMutation 事务回滚 → RUNNING 写入一并回滚 → DB status 保持 DRAFT。**PASS（候选 P0 证伪）**——事务边界覆盖状态机一致性，无悬挂 RUNNING。注：无显式"MRP 运算失败回滚"独立测试（异常路径经 @BizMutation 平台机制保证），归 A4.2b 代码质量审计补测试。

#### 场景 D：已 firmed 行重复释放拒绝（幂等 ERR_MRP_LINE_ALREADY_FIRMED）

证据：`TestErpMfgMrpEndToEnd:121-123`。流程：releaseWorkRequest（已 firmed 行）→ requireReleasable 检测 isFirmed=true → 抛 ERR_MRP_LINE_ALREADY_FIRMED。**PASS**。

#### 场景 E：预测生命周期（DRAFT→approve→APPROVED→DemandAggregator 消费→CONSUMED 未回写漂移）

证据：`TestErpMfgForecastSource`（预测作为需求源被消费 + approve/cancel 状态机）。流程：创建 DRAFT forecast → approve（DRAFT→APPROVED）→ runMrp（DemandAggregator.collectForecastDemands:175 filter status=APPROVED 只读消费）→ **CONSUMED 未回写——预测保持 APPROVED 漂移**（owner doc mrp.md:88 已注记 Deferred）。下次 MRP 运行仍消费同一 APPROVED 预测（无去重——设计接受，预测是计划期输入非一次性消费）。**PASS（主路径）+ FAIL（CONSUMED 死状态 → P1-MA2-036）**。

#### 场景 F：预测 cancel（APPROVED→CANCELLED）

证据：`TestErpMfgForecastSource:77-84`。流程：approve（DRAFT→APPROVED）→ cancel（APPROVED→CANCELLED）→ cancel 已 CANCELLED 拒绝（ERR_FORECAST_ILLEGAL_STATUS_TRANSITION）。**PASS**。

#### 场景 G：SUBCONTRACT_REQUEST 释放 config-gated（off 拒绝 / on 生成 APPROVED 委外单绕审批）

证据：`TestErpMfgSubcontracting:171-185`（config 开启时 releaseSubcontractRequest 生成 APPROVED 委外单 + 重复释放幂等拒绝）+ `MrpReleaseService.releaseSubcontractRequest:99-102`（config 关闭时拒绝）。流程：config `erp-mfg.subcontract-release-enabled=false` → releaseSubcontractRequest 抛 ERR_MRP_RELEASE_UNSUPPORTED_ORDER_TYPE；config=true → releaseToSubcontractOrder 生成 ErpMfgSubcontractOrder docStatus=APPROVED + approveStatus=APPROVED 绕审批管道（:199-201）→ isFirmed=true + convertedBillCode=SUB-MRP-*。**PASS（config-gate 门控）+ FAIL（O-4 豁免登记缺失 → P1-MA2-038）**。

#### 场景 H：仿真 promote（场景版本 COMPLETED→promoteToFormalPlan→新 DRAFT plan→版本 ARCHIVED，单次路径零触及）

证据：`TestErpMfgMrpSimulation`（仿真引擎/场景/版本/promote/参数覆盖）。流程：runSimulation（场景 DRAFT→RUNNING→COMPLETED + fork 计算 + 新建 computed plan DRAFT→RUNNING→COMPLETED + 版本 COMPLETED）→ promoteToFormalPlan（版本 COMPLETED→ARCHIVED + 新建 promoted DRAFT plan + 复制计划行重置 isFirmed=false/convertedBillCode=null + promotedPlanId 防重复转正）。**单次 MRP 路径零触及**（SimulationMrpEngine 不调 MrpEngine.runMrp，全 grep 仅 javadoc 引用）。**PASS**。

#### 场景 I：并发释放同建议单（isFirmed 无 @Version——双读双写？交接 A2.17）

证据：`ErpMfgMrpPlanLine` ORM `orm.xml:807-853` 列定义无 `versionProp`（仅 `ErpMfgMrpPlan` 头 `versionProp="version"` :780，建议单行无 versionProp）。并发释放同建议单：两事务同读 isFirmed=false → 都通过 requireReleasable 守卫 → 都执行 markFirmed → 后提交事务覆盖先提交事务的 convertedBillCode（silent lost-update，可能生成两个目标单但建议单只记录最后一个 convertedBillCode）。**交接 A2.17**（系统性并发审计）。注：头级 versionProp 不保护行级并发（行级无独立 versionProp）。

### 3.10 维度「与设计文档一致性」 — ⚠️ FAIL（4 处漂移）

| # | 漂移 | owner doc | 代码 | 裁决 |
|---|---|---|---|---|
| 1 | **MRP CANCELLED dict 含但无 writer** | `mrp.md` 无独立 MRP 状态机章节（散落在 §实现偏离补注）；`state-machine.md` 无 MRP 独立章节 | dict `erp-mfg/mrp-status:77` 含 CANCELLED；`ErpMfgConstants.java:95` 常量定义零使用；`ErpMfgMrpPlanBizModel` 无 cancelMrp mutation | **P1-MA2-036**（dict 死状态 + owner doc 未注记死状态——漂移） |
| 2 | **预测 CONSUMED dict 含但无 writer** | `mrp.md §实现偏离补注 line 88` 已注记 Deferred（"CONSUMED 状态值预留但本期不自动迁移"）；`state-machine.md` 无预测独立章节 | dict `erp-mfg/forecast-status:129` 含 CONSUMED；`ErpMfgConstants.java:161` 常量定义零使用（仅 ForecastBizModel:54 作为 cancel 守卫）；DemandAggregator:175 只读 APPROVED 不回写 CONSUMED | **P1-MA2-036（合并）**（dict 死状态；owner doc 部分注记 Deferred 但 state-machine.md 未声明——漂移较轻） |
| 3 | **`mrp.md §建议单释放` "RELEASED" 文字 vs 实现 isFirmed 布尔** | `mrp.md §建议单释放 line 69`："释放后建议单状态标记为 RELEASED，不再参与下次 MRP" | 实现无 RELEASED 状态值——`ErpMfgMrpPlanLine` 无 status 列，用 isFirmed 布尔 + convertedBillCode 承载 | **P1-MA2-037**（owner doc 文字 vs 实现偏离——重点漂移） |
| 4 | **`mrp.md §MRP 流程` lot sizing 物料级列 + 在途在制实时汇总** | `mrp.md §MRP 流程 line 42-44`（fixedLotSize/minOrderQty/maxOrderQty）+ `§可用量 line 29-32`（在途采购/在制工单实时汇总） | 物料级 fixedLotSize/minOrderQty/maxOrderQty 列不存在；可用量仅 ErpInvStockBalance.availableQuantity（在途在制未实时汇总） | **PASS**（`mrp.md §实现偏离补注 line 89-91` 已注记 Deferred/简化，触发条件明确） |
| 5 | BOM 无状态机 | `bom-and-routing.md`（无 BOM 状态机声明）+ `state-machine.md`（无 BOM 章节） | `ErpMfgBom.isActive` 布尔无状态机 | **PASS**（owner doc 未声明 BOM 状态机——一致） |
| 6 | 建议单释放生成采购单单价/金额=0 | `mrp.md §实现偏离补注 line 95` 已注记残留（"须计划员/采购员后续完善"） | `MrpReleaseService.releaseToPurchaseOrder:159-160`（unitPrice/amount=ZERO） | **PASS**（owner doc 已文档化残留） |
| 7 | 委外建议释放 SUBCONTRACT_REQUEST | `mrp.md §实现偏离补注 line 93`："orderType=SUBCONTRACT_REQUEST 字典存在但委外流程独立面，本期不支持释放" | 实现 `MrpReleaseService.releaseSubcontractRequest:98-113` config-gated 已落地（plan 2026-07-13-0455-1 §Phase 4） | **PASS**（owner doc mrp.md:93 注记已过时——委外释放已 config-gated 落地；`subcontracting.md §MRP 释放` 已更新引用，mrp.md 未同步——属 P2-MA2-045 owner doc 组织问题） |
| 8 | `state-machine.md` 无 MRP/预测独立状态机章节 | `state-machine.md`（适用对象一工单 + 二作业卡 + 三委外，无 MRP/预测） | MRP 计划/预测状态机实现完整（散落在 mrp.md §实现偏离补注） | **P2-MA2-045**（owner doc 组织问题，与 P2-MA2-043 同型） |

---

## 4. 已登记 finding 运行时影响复核

| Finding ID | 原描述 | 状态机角度复核 | 终态裁决 |
|---|---|---|---|
| `P1-MA1-022` | posting dispatcher 跨域只读 `daoFor(ErpInv*)`/`daoFor(ErpMd*)`（9 域合并） | manufacturing 计划规划路径的跨域只读：`MrpEngine` 跨域读 `ErpInvStockBalance`/`ErpMdMaterial`/`ErpMfgBomOperation`（可用量/物料/BOM 工序）+ `DemandAggregator` 跨域读 `ErpSalOrder`/`ErpMdMaterial`/`ErpInvStockBalance`（销售订单/物料/库存）。状态机角度：跨域只读是状态迁移的副作用（需求聚合 + 库存可用量计算），不破坏状态机裁决本身。异常路径：@BizMutation 事务回滚覆盖（只读无副作用，事务回滚无影响——读不到数据时返回空集合，不抛异常致状态悬挂）。 | **仅治理缺陷**，状态机角度**无悬挂升级**，维持 P1 |
| `P1-MA1-029` | `ErpCtInvoicePlanBizModel`（contract→pur/sal）跨域写半治理——`posting-exemptions.md` 未登记该豁免 | 同型裁决参考：`MrpReleaseService.releaseToSubcontractOrder:185-216` 跨实体写 `ErpMfgSubcontractOrder` APPROVED 绕审批管道——`posting-exemptions.md §MrpReleaseService` 同样未登记此委外单豁免（仅登记采购单）。与 P1-MA1-029 同型半治理（bypass rationale 已在 javadoc :202 但未登记 posting-exemptions.md）。**新登记 P1-MA2-038**。 | **同型裁决**：P1-MA1-029 维持仅治理缺陷（生成 UNSUBMITTED DRAFT 不破坏业务正确性）；P1-MA2-038 新登记（生成 APPROVED 委外单绕审批，config-gated 默认 off 控制风险——裁决为 P1 治理非 P0） |

> 复核结论：已登记 finding 在计划规划状态机运行时**无行为升级**。P1-MA1-022 跨域只读是状态迁移副作用（异常路径经事务回滚覆盖）；P1-MA1-029 同型裁决产生**新 finding P1-MA2-038**（MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失）。

---

## 5. 新发现汇总

### P1-MA2-036 — MRP CANCELLED + 预测 CONSUMED dict 死状态（不可达 + owner doc 漂移）

- **严重性**：P1（major）
- **位置**：
  - MRP CANCELLED：`module-manufacturing/model/app-erp-manufacturing.orm.xml:77`（dict `erp-mfg/mrp-status` 含 CANCELLED）+ `ErpMfgConstants.java:95`（常量定义零使用）+ `ErpMfgMrpPlanBizModel.java`（无 cancelMrp mutation）
  - 预测 CONSUMED：`module-manufacturing/model/app-erp-manufacturing.orm.xml:129`（dict `erp-mfg/forecast-status` 含 CONSUMED）+ `ErpMfgConstants.java:161`（常量定义零使用，仅 ForecastBizModel:54 作为 cancel 守卫）+ `DemandAggregator.java:175`（只读 APPROVED 不回写 CONSUMED）
- **问题**：MRP 计划头 dict 5 态中 CANCELLED **无任何代码路径可达**（无 cancelMrp mutation + 无 setStatus(MRP_STATUS_CANCELLED) 调用）；预测 dict 4 态中 CONSUMED **无任何代码路径可达**（无 setStatus(FORECAST_STATUS_CONSUMED) 调用；DemandAggregator 只读 APPROVED 不回写）。按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）+ A2.6a P1-MA2-035（作业卡 TRANSFERRED dict 死状态）同型裁决：dict 项不可达 + owner doc 声明但代码无实现（或代码有但 owner doc 未声明死状态）。
- **重要性原因**：dict 项不可达致查询筛选语义混乱（UI 按 dict 渲染状态选项包含永不到达的状态）+ owner doc 状态机章节与代码漂移（MRP CANCELLED owner doc 未注记死状态；预测 CONSUMED owner doc mrp.md:88 已注记 Deferred 但 state-machine.md 无独立章节未声明）。不破坏主路径（MRP DRAFT→RUNNING→COMPLETED→FIRMED 完整覆盖计划生命周期；预测 DRAFT→APPROVED→CANCELLED 完整覆盖预测生命周期——CONSUMED 是预测消费后状态回写的预留，本期 DemandAggregator 设计为只读不回写，预测保持 APPROVED 可被多计划期重复消费）。
- **处置**：MR1 裁决——
  - MRP CANCELLED：方案 A（推荐）删除 dict `erp-mfg/mrp-status` CANCELLED 项 + 删除 `ErpMfgConstants.java:95` 常量 + owner doc 注记「MRP 计划废弃走 useLogicalDelete 而非状态迁移」（ErpMfgMrpPlan `useLogicalDelete=true` 已在 orm.xml:770）；方案 B 实现 cancelMrp mutation（DRAFT/COMPLETED→CANCELLED 守卫 + 释放路径守卫）
  - 预测 CONSUMED：方案 A（推荐）删除 dict `erp-mfg/forecast-status` CONSUMED 项 + 删除 `ErpMfgConstants.java:161` 常量 + ForecastBizModel:54 cancel 守卫移除 CONSUMED 检查 + owner doc mrp.md:88 注记更新「预测消费不回写状态，APPROVED 预测可被多计划期重复消费」；方案 B 实现 CONSUMED 自动回写（DemandAggregator 消费后置 CONSUMED + 预测消费去重机制——owner doc 已裁定 Deferred 触发条件：预测消费后状态回写需求落地）

### P1-MA2-037 — `mrp.md §建议单释放` "RELEASED" 文字 vs 实现 isFirmed 布尔 owner doc 漂移

- **严重性**：P1（major，owner doc 文字 vs 实现偏离——重点漂移）
- **位置**：`docs/design/manufacturing/mrp.md:69`（§建议单释放："释放后建议单状态标记为 RELEASED，不再参与下次 MRP"）
- **问题**：owner doc `mrp.md §建议单释放 line 69` 明示"释放后建议单状态标记为 RELEASED，不再参与下次 MRP"，但实现 `ErpMfgMrpPlanLine`（`orm.xml:807-853`）**无 status 列**——生命周期由 `orderType`（dict 4 态）+ `isFirmed` 布尔（默认 false :824）+ `convertedBillCode`(:825) 三字段隐式承载。释放=翻转 isFirmed=true + 填 convertedBillCode，**无 RELEASED 状态值**。owner doc 文字 vs 实现偏离（与 `mrp.md §实现偏离补注 line 95 §建议单释放耦合度` 已注记的"释放直接持久化目标域实体"不同——前者是状态值漂移，后者是耦合度注记）。
- **重要性原因**：owner doc 文字误导审查者/开发者期望建议单有 RELEASED 状态值（按 status 筛选已释放建议单），实际须按 isFirmed 布尔筛选。不破坏运行时正确性（isFirmed 布尔承载语义等价，MrpEngine:140 永远写 isFirmed=FALSE + MrpReleaseService:130 写 isFirmed=TRUE），但破坏 owner doc 与实现的一致性契约。
- **处置**：MR1 裁决——方案 A（推荐）owner doc `mrp.md §建议单释放 line 69` 更新为「释放后建议单 `isFirmed=true` + 填 `convertedBillCode`，不再参与下次 MRP 重算（MrpEngine.clearLines 清除既有行重写，但已 firmed 行的 convertedBillCode 在重算前已生成目标单）」；方案 B 实现建议单 status 列（加 RELEASED 状态值——须 ask-first ORM 加列，与本期 isFirmed 布尔设计偏离，不推荐）。

### P1-MA2-038 — MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失（与 P1-MA1-029 同型半治理）

- **严重性**：P1（major，O-4 豁免登记完整性 + 跨实体写绕审批管道）
- **位置**：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpReleaseService.java:185-216`（releaseToSubcontractOrder）+ `:199-201`（setDocStatus APPROVED + setApproveStatus APPROVED 绕审批管道）+ `:202` javadoc（O-4 架构豁免声明）；`docs/architecture/posting-exemptions.md §MrpReleaseService`（仅登记 ErpPurOrder/ErpPurOrderLine，未登记 ErpMfgSubcontractOrder/ErpMfgSubcontractOrderLine）
- **问题**：`MrpReleaseService.releaseToSubcontractOrder:185-216` 经 `daoProvider.daoFor(ErpMfgSubcontractOrder).saveEntity` 直接持久化委外单（APPROVED 绕审批，:199-201）+ 委外单行（:205-214）。虽 ErpMfgSubcontractOrder 同属 manufacturing 域（非跨模块），但 MrpReleaseService javadoc:202 已声明"O-4 架构豁免：MRP 自动释放不经人工审批管道，跨模块直接持久化委外单骨架（加工费 0 待采购员补录）"，**`docs/architecture/posting-exemptions.md §MrpReleaseService` 未登记此委外单豁免**——与 P1-MA1-029（ErpCtInvoicePlanBizModel 跨域写半治理）同型半治理（bypass rationale 已在 javadoc 但未登记 posting-exemptions.md）。config-gated `erp-mfg.subcontract-release-enabled` 默认 false 控制风险暴露面。
- **重要性原因**：豁免登记缺失致架构治理盲区——审查者/开发者无法从 `posting-exemptions.md` 完整了解 MrpReleaseService 的跨实体写范围（仅见采购单，不见委外单）。config-gated 默认 off 控制运行时风险（生产环境默认不触发 APPROVED 绕审批），但开启时生成的 APPROVED 委外单绕过 `ErpMfgSubcontractOrderProcessor` 审批管道（submit→approve）+ 加工费=0 须采购员补录。与 P1-MA1-029 同型裁决（治理层 finding 不升级为运行时 P0——config-gate 默认 off + 加工费=0 须补录 + 状态正确不破坏业务规则）。
- **处置**：MR1 在 `docs/architecture/posting-exemptions.md §MrpReleaseService` 补登委外单豁免条目（写入目标 ErpMfgSubcontractOrder + ErpMfgSubcontractOrderLine / 触发场景 SUBCONTRACT_REQUEST 释放 config-gated / 理由 同采购单 O-4 豁免 / 风险 APPROVED 绕审批 + 加工费=0 / 补偿 config-gated 默认 false + 加工费须补录 + 权限校验在 @BizMutation 入口 / 收敛条件 待委外域提供 purpose-built createFromMrpLine 时收敛为 I*Biz 调用）。

### P2-MA2-045 — `state-machine.md` 无 MRP/预测独立状态机章节（散落在 mrp.md §实现偏离补注）

- **严重性**：P2（watch-only，文档组织问题）
- **位置**：`docs/design/manufacturing/state-machine.md`（适用对象一工单 + 二作业卡 + 三委外，无 MRP 计划/预测独立章节）
- **问题**：MRP 计划状态机（5 态）+ 预测状态机（4 态）owner doc 散落在 `mrp.md §实现偏离补注`（line 88 CONSUMED Deferred + line 93 委外释放 + line 95 建议单释放耦合度），无独立 MRP/预测状态机章节。与 P2-MA2-043（领料状态机 owner doc 无独立章节）+ P2-MA2-037（finance state-machine.md 缺 AR/AP+坏账独立章节）同型。
- **处置**：watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增"适用对象四：MRP 计划头状态机" + "适用对象五：运营预测状态机"章节（本审计 §2.1/§2.2 状态图可直接采用）；方案 B 交叉链接到 `mrp.md §实现偏离补注`。

### P2-MA2-046 — `mrp.md §实现偏离补注 line 93` 委外释放注记已过时（config-gated 已落地）

- **严重性**：P2（watch-only，owner doc 未同步实现进展）
- **位置**：`docs/design/manufacturing/mrp.md:93`（§实现偏离补注："orderType=SUBCONTRACT_REQUEST 字典存在但委外流程独立面，本期不支持释放。触发条件：委外加工落地时。"）
- **问题**：owner doc mrp.md:93 注记"本期不支持释放"，但实现 `MrpReleaseService.releaseSubcontractRequest:98-113` config-gated 已落地（plan 2026-07-13-0455-1 §Phase 4，config `erp-mfg.subcontract-release-enabled` 默认 false）。owner doc 未同步实现进展（`subcontracting.md §MRP 释放` 已更新引用，mrp.md 未同步）。
- **处置**：watch-only，MR1 顺手更新 mrp.md:93 注记为「config-gated `erp-mfg.subcontract-release-enabled`（默认 false）已落地，开启时经 releaseSubcontractRequest 生成 APPROVED 委外单骨架（O-4 豁免，见 posting-exemptions.md §MrpReleaseService）」。

---

## 6. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 现状 | 交接 |
|---|---|---|---|
| 并发释放同建议单（isFirmed 无 @Version） | `MrpReleaseService.requireReleasable:115` + `markFirmed:129-133` | `ErpMfgMrpPlanLine` ORM 列定义（`orm.xml:807-853`）**无 versionProp**（仅头级 `ErpMfgMrpPlan.versionProp="version"` :780）→ 并发释放同建议单：两事务同读 isFirmed=false → 都通过守卫 → 都执行 markFirmed → 后提交事务覆盖先提交事务的 convertedBillCode（silent lost-update，可能生成两个目标单但建议单只记录最后一个 convertedBillCode） | A2.17（owner doc `mrp.md` 未声明并发控制；建议单行无独立 versionProp 是并发缺口） |
| 并发 advancePlanToFirmedIfComplete（头级回写竞态） | `MrpReleaseService.advancePlanToFirmedIfComplete:218-236` | `ErpMfgMrpPlan.versionProp="version"` 乐观锁保护头级回写（COMPLETED→FIRMED 迁移）；并发释放不同建议单但同计划时，两事务都查全行 isFirmed + 都更新头 status → 头级 versionProp 透明乐观锁 detectable conflict | A2.17（头级 versionProp 覆盖，但行级并发缺口见上） |
| 并发 runMrp 同计划（重复运算） | `MrpEngine.runMrp:79-83`（守卫 requireStatus DRAFT 或 null） | 两并发 runMrp 同 planId：第一事务 setStatus RUNNING + updateEntity（事务内未 commit）；第二事务同读 status=DRAFT（第一事务未提交不可见）→ 也通过守卫 → 两事务都 clearLines + processMaterial → 后提交事务覆盖先提交事务的计划行（silent lost-update） | A2.17（守卫依赖事务隔离级别，RUNNING 中间态未在 flush 前对外可见——同 finance P2-MA2-025 CLOSING 中间态并发缺口同型） |
| 仿真并发 runSimulation 同场景 | `SimulationMrpEngine.runSimulation:98-102`（守卫 requireStatus DRAFT） | 两并发 runSimulation 同 scenarioId：同 runMrp 竞态范式 | A2.17 |
| 仿真并发 promoteToFormalPlan 同版本 | `SimulationMrpEngine.promoteToFormalPlan:173-176`（守卫 promotedPlanId 为空） | `ErpMfgMrpScenarioVersion.versionProp="version"`（orm.xml:1584）乐观锁保护；并发 promote 同版本：第一事务 setPromotedPlanId + ARCHIVED；第二事务同读 promotedPlanId=null → 都通过守卫 → 都创建 promoted plan → 后提交事务覆盖 promotedPlanId（可能生成两个 promoted plan 但版本只记录最后一个） | A2.17（versionProp 部分覆盖，promotedPlanId 守卫依赖事务隔离） |

> **重要降级**：本审计发现 `ErpMfgMrpPlan`（头）+ `ErpMfgMrpScenario` + `ErpMfgMrpScenarioVersion` 均声明 `versionProp="version"`，Nop ORM 透明乐观锁将头级/场景级/版本级"silent lost-update 风险"降级为"detectable optimistic conflict"。但**`ErpMfgMrpPlanLine`（建议单行）无 versionProp**，行级并发释放缺口未降级——A2.17 系统性并发审计时应纳入此事实。

---

## 7. 残留风险

1. **MRP CANCELLED + 预测 CONSUMED dict 死状态**（P1-MA2-036，MR1）——dict 项不可达需清理或实现 writer。
2. **`mrp.md §建议单释放` "RELEASED" 文字 vs isFirmed 布尔漂移**（P1-MA2-037，MR1）——owner doc 文字误导。
3. **MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失**（P1-MA2-038，MR1）——架构治理盲区（config-gated 默认 off 控制运行时风险）。
4. **`state-machine.md` 无 MRP/预测独立章节**（P2-MA2-045）+ **mrp.md:93 委外释放注记过时**（P2-MA2-046）——owner doc 治理。
5. **已 firmed 建议单不可逆**（设计接受 successor）——释放后改主意的回退经目标单 DRAFT 状态由采购员/计划员废弃目标单，非建议单层回退。与工单 COMPLETED 无 reverseCompletion 同型裁定。
6. **建议单释放生成采购单单价/金额=0 + 委外单加工费=0**（owner doc `mrp.md §实现偏离补注` 已注记残留）——须采购员补录，DRAFT/UNSUBMITTED 状态保证不进入审批流。
7. **仿真算法漂移风险**（SimulationMrpEngine 头部注释 :51 已声明"任何 MrpEngine 算法变更须同步本类"）——Non-Goals 限定本期不引入新算法维度（CRP/概率仿真/物料级批量），算法漂移风险低。
8. **并发敏感点 5 处交接 A2.17**（含 `ErpMfgMrpPlanLine` 无 versionProp 行级并发缺口未降级重要事实）。

## 8. 审计范围声明

本审计严格限定 A2.6b scope = manufacturing 计划规划状态机（MRP 计划头 + 预测 + 建议单隐式生命周期 + BOM 激活 + 仿真五组件）。以下明确排除（Non-Goal）：

- **A2.6a 生产执行类状态机**（done）——本审计只确认建议单释放生成的工单/委外单骨架进入目标域后状态正确（DRAFT 工单骨架 / APPROVED 委外单绕审批——后者在 scope 内复核 → P1-MA2-038）；
- **MRP 运算正确性**（净需求/批量/提前期/BOM 展开算法）——归 A4.2b 代码质量/业务逻辑审计；本审计只做状态机生命周期审查；
- **仿真引擎算法正确性**——config-gated 默认关 + 单次路径零触及；本审计只确认仿真状态机迁移正确 + promote 不破坏单次路径；
- **A2.17 并发与乐观锁**（并发释放同建议单 isFirmed 无 @Version）——归 A2.17；本审计只标注观察到的并发敏感点；
- **config-gated Deferred 偏离本身**（预测 CONSUMED 回写 / 物料级 lot sizing 列 / 在途在制实时汇总 / CRM disaggregation / SUBCONTRACT_RELEASE config / 仿真算法）——owner doc 已裁定的 Deferred/Non-Goal，本审计只确认其在状态机上不引入悬挂（CONSUMED/CANCELLED 死状态归状态定义清晰性维度裁决为 P1 dict 死状态清理而非实现）；
- **A4.2b manufacturing 代码质量**——MRP 引擎/释放代码质量（异常处理/N+1/索引）系统性审查归 A4.2b；
- **owner doc 已裁定的 Non-Goal 子项**（已 firmed 建议单不可逆 / MRP FIRMED 终态无再运算回退 / 仿真版本 ARCHIVED 终态无恢复）。

## 9. 结论

manufacturing 计划规划五组件状态机（MRP 计划头 5 态 + 预测 4 态 + 建议单隐式生命周期 isFirmed + BOM isActive + 仿真 4 态 config-gated）核心契约经实仓逐项证据确认：状态迁移守卫齐全（`requirePlan`/`requireReleasable`/`requireSimulationEnabled`/`requireVersion` 前置校验）、事务边界清晰（@BizMutation 自动事务覆盖 runMrp 全链 + 释放全链 + promote 全链）、幂等守卫完整（已 firmed 行重复释放拒绝）、仿真 E2 fork 单次路径零触及（SimulationMrpEngine 不调 MrpEngine.runMrp）。**零 P0**（三个候选 P0 经证据证伪：MRP 运算 RUNNING 中途异常经 @BizMutation 事务回滚覆盖 / 释放路径生成目标单失败经 @BizMutation 事务回滚覆盖 / SUBCONTRACT config-gated on 生成 APPROVED 委外单经 config-gate 默认 off 控制裁决 P1 治理）；**3 项新 P1**（P1-MA2-036 MRP CANCELLED + 预测 CONSUMED dict 死状态 + owner doc 漂移 / P1-MA2-037 mrp.md §建议单释放 "RELEASED" 文字 vs isFirmed 布尔漂移 / P1-MA2-038 MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失——与 P1-MA1-029 同型半治理）；**2 项新 P2** watch-only（P2-MA2-045 state-machine.md 无 MRP/预测独立章节 / P2-MA2-046 mrp.md:93 委外释放注记过时）；MA1 finding 运行时复核**无升级**（P1-MA1-022 跨域只读维持仅治理缺陷异常路径经事务回滚覆盖 / P1-MA1-029 同型裁决产生新 finding P1-MA2-038）；并发敏感点 5 处交接 A2.17（含 `ErpMfgMrpPlanLine` 无 versionProp 行级并发缺口未降级重要事实）。

**Verdict: pass**。A2.6b 完成，manufacturing 计划规划状态机系统性审查 done。manufacturing 状态机审查 S 级拆分 1/2（A2.6a 生产执行）+ 2/2（A2.6b 计划规划）全部 done。
