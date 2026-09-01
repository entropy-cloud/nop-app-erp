# manufacturing 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（测试共享夹具见 `app-erp-test-data`，边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.2a2（plan `docs/plans/2026-09-01-1245-2-m12a2-manufacturing-seed-expansion.md`，2026-09-01）。此前 8 张 manufacturing 表 seed（work_order / workcenter(+calendar,+capacity) / crp_load / forecast(+line) / cost_variance，plan `2026-07-09-0930-1`）见 `docs/architecture/seed-data.md` 历史批次段。
> **业务语义 owner docs**：`docs/design/manufacturing/bom-and-routing.md`（BOM/工艺路线）、`mrp.md` + `simulation-engine.md`（MRP 与仿真）、`crp.md`（产能负荷）、`variance-analysis.md`（生产差异）、`subcontracting.md`（委外）、`batch-genealogy.md`（批次追溯）；本文件只登记种子数据面，不重复业务语义。

## 种子数据范围（M1.2a2 批次 26 表）

manufacturing 域 34 规格实体中，8 表已由历史批次 seed；本批补齐其余 **26 规格表（63 行）**，达成 manufacturing 域全量 seed 覆盖（34 / 34）。

### 26 规格表 CSV

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpMfgBom（BOM 头） | `erp_mfg_bom.csv` | 2 | P+N-DIS | PRODUCT_ID→ErpMdMaterial〔跨域:md·已seed〕；**两行 `IS_DEFAULT=false`**（干扰面裁决，见下节） |
| ErpMfgBomLine（BOM 行） | `erp_mfg_bom_line.csv` | 3 | P | BOM_ID→ErpMfgBom〔本批〕、MATERIAL_ID→ErpMdMaterial〔已seed〕、UO_M_ID→ErpMdUoM〔已seed〕；OPERATION_ID/WAREHOUSE_ID 可选→〔本批〕/〔已seed〕 |
| ErpMfgBomOperation（BOM 工艺） | `erp_mfg_bom_operation.csv` | 3 | P | BOM_ID→ErpMfgBom〔本批〕、OPERATION_ID→**ErpMfgRoutingOperation〔本批〕跨族边**、WORKCENTER_ID 可选→ErpMfgWorkcenter〔已seed〕 |
| ErpMfgBomByproduct（BOM 联副产品） | `erp_mfg_bom_byproduct.csv` | 1 | P | BOM_ID→ErpMfgBom〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕 |
| ErpMfgRouting（工艺路线） | `erp_mfg_routing.csv` | 2 | P+N-DIS | —（无必填 FK）；行 2 `IS_ACTIVE=false` 停用负例 |
| ErpMfgRoutingOperation（工艺路线工序） | `erp_mfg_routing_operation.csv` | 3 | P | ROUTING_ID→ErpMfgRouting〔本批〕、WORKCENTER_ID 可选→ErpMfgWorkcenter〔已seed〕 |
| ErpMfgProductionVersion（生产版本） | `erp_mfg_production_version.csv` | 2 | P+N-DIS | PRODUCT_ID→ErpMdMaterial〔已seed〕、BOM_ID→ErpMfgBom〔本批〕、ROUTING_ID→ErpMfgRouting〔本批〕 |
| ErpMfgWorkOrderLine（工单产出/投入行） | `erp_mfg_work_order_line.csv` | 3 | P | WORK_ORDER_ID→ErpMfgWorkOrder〔已seed〕（WO-2026-001/003）、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕 |
| ErpMfgJobCard（工序作业卡） | `erp_mfg_job_card.csv` | 3 | P×2+N-TERM(CANCELLED) | WORK_ORDER_ID→ErpMfgWorkOrder〔已seed〕、OPERATION_ID 可选→ErpMfgBomOperation〔本批〕、WORKCENTER_ID 可选→〔已seed〕 |
| ErpMfgJobCardTimeLog（作业工时记录） | `erp_mfg_job_card_time_log.csv` | 2 | P | JOB_CARD_ID→ErpMfgJobCard〔本批〕、WORK_ORDER_ID→ErpMfgWorkOrder〔已seed〕、OPERATOR_ID→ErpMdEmployee〔跨域:md·已seed〕 |
| ErpMfgMaterialIssue（领料单头） | `erp_mfg_material_issue.csv` | 3 | P×2+N-TERM(CANCELLED) | WORK_ORDER_ID→ErpMfgWorkOrder〔已seed〕、WAREHOUSE_ID→ErpMdWarehouse〔跨域:md·已seed〕、JOB_CARD_ID/CURRENCY_ID 可选 |
| ErpMfgMaterialIssueLine（领料单行） | `erp_mfg_material_issue_line.csv` | 2 | P | ISSUE_ID→ErpMfgMaterialIssue〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕、WORK_ORDER_LINE_ID 可选→ErpMfgWorkOrderLine〔本批〕 |
| ErpMfgMrpPlan（MRP 计划头） | `erp_mfg_mrp_plan.csv` | 3 | P×2+N-TERM(FIRMED) | —（无必填 FK；ORG_ID 可选→ErpMdOrganization〔已seed〕） |
| ErpMfgMrpPlanLine（MRP 计划行） | `erp_mfg_mrp_plan_line.csv` | 4 | P | MRP_PLAN_ID→ErpMfgMrpPlan〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕；行 4 FIRMED 回写 CONVERTED_BILL_CODE=WO-2026-001（软引用演示） |
| ErpMfgMrpDemand（MRP 独立需求） | `erp_mfg_mrp_demand.csv` | 2 | P | MRP_PLAN_ID→ErpMfgMrpPlan〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕；行 1 SOURCE_BILL_CODE=FC-2026-001（挂既有 forecast seed） |
| ErpMfgMrpScenario（MRP 仿真场景） | `erp_mfg_mrp_scenario.csv` | 2 | P(DRAFT)+N-TERM(ARCHIVED) | —（无必填 FK；BASE_MRP_PLAN_ID 可选→ErpMfgMrpPlan〔本批〕、ORG_ID〔已seed〕）；状态 ∈ `erp-mfg/simulation-status` |
| ErpMfgMrpScenarioVersion（仿真版本） | `erp_mfg_mrp_scenario_version.csv` | 2 | P(COMPLETED)+N-TERM(ARCHIVED) | SCENARIO_ID→ErpMfgMrpScenario〔本批〕、COMPUTED/PROMOTED_PLAN_ID 可选→ErpMfgMrpPlan〔本批〕 |
| ErpMfgMrpScenarioParam（仿真参数覆盖） | `erp_mfg_mrp_scenario_param.csv` | 2 | P | SCENARIO_ID→ErpMfgMrpScenario〔本批〕、MATERIAL_ID 可选→ErpMdMaterial〔已seed〕；PARAM_TYPE ∈ `erp-mfg/simulation-param-type` |
| ErpMfgCostRollup（标准成本滚算头） | `erp_mfg_cost_rollup.csv` | 3 | P×2+N-TERM(CANCELLED) | —（无必填 FK；ORG_ID〔已seed〕）；**零 FIRMED 行**（干扰面裁决，见下节） |
| ErpMfgCostRollupLine（滚算行） | `erp_mfg_cost_rollup_line.csv` | 4 | P | COST_ROLLUP_ID→ErpMfgCostRollup〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕、CURRENCY_ID 可选→〔已seed〕 |
| ErpMfgSubcontractOrder（委外单头） | `erp_mfg_subcontract_order.csv` | 3 | P×2+N-TERM(CANCELLED) | SUPPLIER_ID→ErpMdPartner〔跨域:md·已seed〕（3/4 供应商）、PRODUCT_ID→ErpMdMaterial〔已seed〕、CURRENCY_ID→ErpMdCurrency〔已seed〕；ROUTING_ID/PRODUCTION_VERSION_ID 可选→〔本批〕 |
| ErpMfgSubcontractOrderLine（委外单行） | `erp_mfg_subcontract_order_line.csv` | 3 | P | SUBCONTRACT_ORDER_ID→ErpMfgSubcontractOrder〔本批〕、MATERIAL_ID/UO_M_ID→ErpMd*〔已seed〕 |
| ErpMfgBatchGenealogy（批次基因） | `erp_mfg_batch_genealogy.csv` | 1 | P | WORK_ORDER_ID→ErpMfgWorkOrder〔已seed〕、INPUT/OUTPUT_LOT_ID→**ErpInvBatch〔跨域:inv·本批=M1.1b 已落地集〕**、INPUT/OUTPUT_MATERIAL_ID 与 UO_M_ID→ErpMd*〔已seed〕 |
| ErpMfgWorkOrderBomSnapshot（工单 BOM 快照头） | `erp_mfg_work_order_bom_snapshot.csv` | 1 | P | —（快照族无必填 FK）；WORK_ORDER_ID→ErpMfgWorkOrder〔已seed〕、BOM_ID→ErpMfgBom〔本批〕、PRODUCT_ID→〔已seed〕全填闭环 |
| ErpMfgWorkOrderBomLineSnapshot（快照行） | `erp_mfg_work_order_bom_line_snapshot.csv` | 2 | P | SNAPSHOT_ID→ErpMfgWorkOrderBomSnapshot〔本批〕；其余可选列全填〔本批〕/〔已seed〕 |
| ErpMfgWorkOrderBomOperationSnapshot（快照工序） | `erp_mfg_work_order_bom_operation_snapshot.csv` | 2 | P | SNAPSHOT_ID→ErpMfgWorkOrderBomSnapshot〔本批〕、OPERATION_ID→ErpMfgRoutingOperation〔本批〕、WORKCENTER_ID→〔已seed〕 |

## FK 闭环图

```
[跨域:md·已seed] erp_md_material(1 产品甲/2 产品乙/3 原料X/4 包装箱)、erp_md_uom(1 PCS/2 KG/4 BOX)、
                 erp_md_warehouse(1/2)、erp_md_employee(1)、erp_md_partner(3 SUP-001/4 SUP-002)、
                 erp_md_currency(1 CNY)、erp_md_organization(2 ERP-CO)
   ──PRODUCT_ID/MATERIAL_ID/UO_M_ID(列名 UO_M_ID 分叉拼写)/OPERATOR_ID/SUPPLIER_ID/ORG_ID/CURRENCY_ID──▶ 全部 26 表
[已seed·mfg] erp_mfg_work_order(1 WO-2026-001/3 WO-2026-003/4 WO-2026-004)、erp_mfg_workcenter(1 WC-001)
   ──WORK_ORDER_ID/WORKCENTER_ID──▶ 工单行/作业卡/报工/领料/快照/基因
[本批] erp_mfg_routing(1..2) ──ROUTING_ID──▶ routing_operation / production_version / subcontract_order
[本批] erp_mfg_routing_operation(1..3) ──OPERATION_ID──▶ bom_operation（跨族边）/ bom_line / 快照工序
[本批] erp_mfg_bom(1..2) ──BOM_ID/BOM_ID──▶ bom_line / bom_operation / bom_byproduct / production_version / 快照头
[本批] erp_mfg_bom_operation(1..3) ──OPERATION_ID──▶ bom_line / job_card / 批次基因
[本批] erp_mfg_production_version(1..2) ──PRODUCTION_VERSION_ID──▶ subcontract_order
[本批] erp_mfg_work_order_line(1..3) ──WORK_ORDER_LINE_ID──▶ material_issue_line
[本批] erp_mfg_job_card(1..3) ──JOB_CARD_ID──▶ job_card_time_log / material_issue / 批次基因
[本批] erp_mfg_material_issue(1..3) ──ISSUE_ID──▶ material_issue_line
[本批] erp_mfg_mrp_plan(1..3) ──MRP_PLAN_ID/BASE_MRP_PLAN_ID/COMPUTED/PROMOTED_PLAN_ID──▶ mrp_plan_line / mrp_demand / mrp_scenario / mrp_scenario_version
[本批] erp_mfg_mrp_scenario(1..2) ──SCENARIO_ID──▶ mrp_scenario_version / mrp_scenario_param
[本批] erp_mfg_cost_rollup(1..3) ──COST_ROLLUP_ID──▶ cost_rollup_line
[本批] erp_mfg_subcontract_order(1..3) ──SUBCONTRACT_ORDER_ID──▶ subcontract_order_line
[本批] erp_mfg_work_order_bom_snapshot(1) ──SNAPSHOT_ID──▶ 快照行 / 快照工序
[跨域:inv·本批=M1.1b 已落地集] erp_inv_batch(1 LOT-20260703-001 原料X/2 LOT-20260630-002 产品甲)
   ──INPUT_LOT_ID/OUTPUT_LOT_ID──▶ 批次基因（批次1 钢材 200KG → 批次2 产品甲 100PCS）
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空、白名单零增量（`TestErpSeedDataIntegrity` 4/4 全绿背书；程序化核验 120 条 to-one 边全闭）。

## 干扰面零漂移设计裁决（本批核心约束）

- **零默认 BOM**：全部 `erp_mfg_bom` 行 `IS_DEFAULT=false`——C08 `TestErpC08MrpApsRelease` 的 `runSimulation` 经 `BomExpander.findDefaultBomOrNull`（过滤 `isDefault=true AND isActive=true`）判定物料自制品/采购件并展开子件需求；种子物料 MAT-001..004 无默认 BOM 时按采购件语义运算，与既有录制口径一致。若种子引入默认 BOM，转正计划将出现种子物料子件需求行 → C08 层 1/层 3 快照漂移。
- **零 FIRMED 滚算**：全部 `erp_mfg_cost_rollup` 行 status ∈ {DRAFT, CALCULATED, CANCELLED}——`ProductionVarianceCalculator.calculateVariances` 经 `findFirmedRollupLine(productId)` 取标准成本；E2E `mfg-chain.spec.ts`（MAT-001 链）依赖「无 FIRMED rollup → 差异计算抛 ERR_VARIANCE_NO_STANDARD_COST 被吞」的预存语义。若种子引入 FIRMED 行，链路完工将触发差异行 + PRODUCTION_VARIANCE 凭证 → 行为破坏。
- **MRP 种子链自封闭**：仿真引擎 `loadDemands(planId)` 按 planId 取需求——种子 mrp_demand/mrp_plan 行（id 1..3）不进入 C08 自建计划的运算输入；场景版本 v2 转正链（ARCHIVED → PROMOTED 计划 3 FIRMED）为静态演示数据。
- 其余读取面核验：mfg 看板（getDashboardKpi/状态分布/趋势/延期告警）与报表（production-variance/forecast-variance/crp-load）只读既有 8 表；`ErpMfgSkuReferenceChecker` 按 skuId 查询（种子行 skuId 留空）；CRP 重算按 routingId 定位（种子工单无 routingId）；快照机制 `_chgType` 增量记录，纯加性插入不入既有快照。

## 与既有 8 表 seed 的衔接（语义一致性约束）

- **工单族衔接**：工单行/作业卡/报工/领料全部挂既有 WO-2026-001（COMPLETED，实际 06-05 开工 06-28 完工）/ WO-2026-003（IN_PROCESS）/ WO-2026-004（STOCK_PARTIAL，取 N-TERM 负例载体）。MI-2026-001 领料 200 KG×30=6000 与 WO-2026-001 材料成本 6000 一致；JC-2026-001 报工 8h×120/h=960 为其 laborCost 1500 的部分记录（差额归未 seed 的其他报工行）。
- **成本衔接**：CR-2026-001（CALCULATED）产品甲单位标准成本 80 = WO-2026-001 单位成本 80（60 材料+15 人工+5 制费）。
- **批次衔接（静态日期简化登记）**：基因链批次 1（LOT-20260703-001，入库 2026-07-01）→ 批次 2（LOT-20260630-002，即 WO-2026-001 产出批次）在入库日期与工单完工日（2026-06-28）间存在静态种子日期张力——冻结时钟纪律（禁止滚动日期）下不重构 M1.1b 既有批次日期，登记为 documented simplification（roadmap Non-Goal「不做逐字段回放测试」口径内）。MI-2026-002（CONFIRMED 待出库）业务日期取 2026-07-03（批次入库后），MI-2026-001（DONE）出库早于批次化追溯启用，BATCH_NO 留空。
- **维度值复用既有值域**：组织 2（ERP-CO）；供应商 3/4；物料 1..4；UoM 1/2/4；仓库 1/2；员工 1（报工操作工）；币种 1；工作中心 1；全部静态日期落在 2026-06~2026-07 参考期（生产版本有效期 2026 全年）。

## 用例指示编码与 negative 行语义

编码定义见 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」通用约定 5：

- **P（最小正例行）**：全部 26 表均有。
- **N-TERM（终态行）**：job_card id=3 `CANCELLED`（作业卡取消终态）；material_issue id=3 `CANCELLED`（领料单取消终态）；mrp_plan id=3 `FIRMED`（计划确认终态，含 CONVERTED_BILL_CODE 转单回写演示）；mrp_scenario id=2 `ARCHIVED` + mrp_scenario_version id=2 `ARCHIVED`（仿真归档终态，promotedPlanId 串联）；cost_rollup id=3 `CANCELLED`（滚算作废终态）；subcontract_order id=3 `CANCELLED`（委外取消终态）。
- **N-DIS（禁用行）**：bom id=2 `IS_ACTIVE=false`（停用 BOM）；routing id=2 `IS_ACTIVE=false`（停用工艺路线）；production_version id=2 `IS_ACTIVE=false`（停用生产版本）。

## 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准，含 `UO_M_ID` 分叉拼写逐表核对）；省略审计列（delVersion/version/createdBy/createTime/updatedBy/updateTime）；ISO 日期与 `yyyy-MM-dd HH:mm:ss` 时间戳；小写布尔；ID < 100000（`zz-sequence-advance.sql` 序列推进值域约束）；字典码 ∈ `erp-mfg/*` + `wf/approve-status` + `erp-md/posted-status` 字典（simulation-status / simulation-param-type 定义于 `ErpMfgConstants`，逐值核对）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议 164→190），并保持本文件「干扰面零漂移设计裁决」两条件（零默认 BOM / 零 FIRMED 滚算）不被破坏。
