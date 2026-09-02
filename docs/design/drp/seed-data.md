# DRP 域种子数据（Seed Data）

> Owner: 本文件（drp 域「种子数据」owner doc；plan `2026-09-02-1415-2-m15-b2b-ct-drp-seed-expansion.md` Phase 2 落地）
> 上游规格: `docs/architecture/seed-data.md` 规格表 drp 节（11 实体逐行规格权威源）+ 对账表（计数权威源）
> 业务语义 owner docs: `docs/design/drp/`（cross-dock / lead-time-tracking / safety-stock-optimization / state-machine 等）；本文件只登记种子数据面，不重复业务语义

## 1. 范围与文件清单

M1.5 批次（2026-09-02）为 drp 域补齐 11 个 seed CSV（此前 drp 为零 seed 域）。文件位于 `app-erp-all/src/main/resources/_vfs/_init-data/`，命名 = `<tableName>.csv`。至此 drp 域 11 规格实体全量 seed 覆盖（11 / 11；6 × `erp_drp_*` + 5 × `erp_inv_drp_*`）。

| CSV | 行数 | 主子表 | 用例指示 | 说明 |
|---|---|---|---|---|
| erp_drp_plan.csv | 2 | 头（plan 组头） | P+N-TERM | DRP-PLAN-SEED-001 DRAFT 8 月期（P）+ DRP-PLAN-SEED-002 EXECUTED 7 月期（终态，TOTAL_REPLENISHMENT_QTY=140 与其 line 自洽） |
| erp_drp_line.csv | 3 | 子表（plan） | P+N-TERM | plan1 两行 SUGGESTED（TRANSFER wh1←wh2 / PURCHASE，P）+ plan2 一行 CANCELLED（N-TERM，ORDER_BILL 弱指针 SEED-REF 码） |
| erp_drp_parameter.csv | 2 | 独立（FK→md warehouse/material/partner） | P | (wh1,mat1) MIN_MAX + (wh2,mat3) LOT_FOR_LOT；**ORG_ID 两行均 = "1"（GROUP-HQ）——载重避让判据** |
| erp_drp_scenario.csv | 2 | 头（scenario 组头） | P+N-TERM | DRP-SCN-SEED-001 DRAFT（base→plan1，P）+ DRP-SCN-SEED-002 ARCHIVED（base→plan2，终态） |
| erp_drp_scenario_param.csv | 2 | 子表（scenario） | P | scn1 SAFETY_STOCK=80 (mat1,wh1) + scn2 LEAD_TIME=21 (mat3,wh2)；UK(scenarioId,materialId,warehouseId,paramType) 两行独立 |
| erp_drp_scenario_version.csv | 2 | 子表（scenario） | P+N-TERM | scn1 V1 DRAFT 待计算（P）+ scn2 V1 ARCHIVED（终态，COMPUTED_DRP_PLAN_ID→plan2） |
| erp_inv_drp_cross_dock.csv | 2 | 独立（FK→md material） | P+N-TERM | XDK-SEED-2026-001 COMPLETED/PRE_ALLOCATED（P，静态 DOCK_SLOT_TIME 2026-08-05）+ XDK-SEED-2026-002 CANCELLED/ON_RECEIPT（终态）；**状态避 `STAGING`（staging 超时 job 扫描键）** |
| erp_inv_drp_dock_appointment.csv | 2 | 独立（FK→md warehouse + cross_dock） | P+N-TERM | dock1→cross_dock1 COMPLETED（P）+ dock2→cross_dock2 CANCELLED（N-TERM）；DOCK_ID 必填 to-one→erp_inv_drp_cross_dock（ORM 建模：月台记录挂越库单） |
| erp_inv_drp_lead_time_record.csv | 2 | 独立（FK→md partner/material） | P | supplier3/mat1 准时 7 天 + supplier4/mat3 迟到 17 天（EARLY_LATE_FLAG ON_TIME/LATE） |
| erp_inv_drp_safety_stock_calc.csv | 2 | 独立（FK→md material/warehouse） | P | (mat1,wh1) STATISTICAL/PCT95 SS=58 ROP=90 + (mat3,wh2) SIMPLE/PCT99 SS=25 ROP=45（LAST_CALCULATED_AT 静态） |
| erp_inv_drp_supplier_score.csv | 2 | 独立（FK→md partner/material） | P | supplier3/mat1 A 级 94.51 + supplier4/mat3 B 级 87.13（四维分数自洽 TOTAL_SCORE 加权演示值）；UK(supplierId,materialId) 两行独立 |

## 2. FK 闭环图

- **〔已 seed〕锚点**：`erp_md_organization`（ORG_ID=1 = GROUP-HQ / 2 = ERP-CO）、`erp_md_warehouse`（WAREHOUSE_ID=1/2）、`erp_md_material`（MATERIAL_ID=1/2/3）、`erp_md_partner`（PARTNER_ID=3 = SUP-001 / 4 = SUP-002）。
- **〔本批〕链路**：
  - plan → drp_line（PLAN_ID 必填 to-one→plan 1/2，MATERIAL_ID/WAREHOUSE_ID/SOURCE_WAREHOUSE_ID→md）；
  - scenario（BASE_DRP_PLAN_ID 可选→plan 1/2）→ scenario_param（SCENARIO_ID 必填→scenario 1/2，material/warehouse→md）+ scenario_version（SCENARIO_ID 必填→scenario 1/2，COMPUTED_DRP_PLAN_ID/PROMOTED_PLAN_ID 可选→plan 2）；
  - cross_dock（MATERIAL_ID 必填→md；DRP_LINE_ID/STAGING_LOCATION_ID/INBOUND_MOVE_ID/OUTBOUND_MOVE_ID 可选留空）← dock_appointment（WAREHOUSE_ID 必填→md + DOCK_ID 必填→cross_dock 1/2 + CROSS_DOCK_ID 可选→cross_dock 1/2）；
  - lead_time_record / supplier_score / safety_stock_calc（supplier/material/warehouse→md，org→md_organization）。
- 全部必填 FK 落在〔已 seed〕∪〔本批〕集合内（`TestErpSeedDataIntegrity` 门禁；本批 drp 段 66 边程序化验证全解析（`erp_drp_*` 36 + `erp_inv_drp_*` 30），白名单零增量）。

## 3. 用例指示编码与 negative 行语义

- **P（最小正例行）**：每表主行，数量/库存 `.0000` 四位小数、评分 `.00`/比率四位、静态日期 2026-02 ~ 2026-08。
- **N-TERM（终态行）**：plan 2（EXECUTED——plan-status 字典终态）、drp_line 3（CANCELLED）、scenario 2（ARCHIVED——simulation-status 运行时词汇终态）、scenario_version 2（ARCHIVED）、cross_dock 2（CANCELLED）、dock_appointment 2（CANCELLED）。
- **N-DIS**：规格表 drp 节无 N-DIS 指示行（parameter 无停用列，逐实体程序化核验口径）；本批零 N-DIS 承载。
- 行级用途以 REMARK 后缀（P）/（N-TERM）/（N-DIS）显式标注（沿 M1.4a/M1.4b 批先例）；列名例外：`erp_drp_scenario` / `erp_drp_scenario_param` / `erp_drp_scenario_version` 三表 ORM 无 REMARK 列，其 P/N-TERM 语义以 STATUS 状态列承载（DRAFT→P / ARCHIVED→N-TERM），见 §1 表格逐行登记。

## 4. 干扰面零漂移设计（本批核验结论——ORG_ID 载重避让为第一判据）

- **ORG_ID 载重判据（草案审查 iteration 1 裁决 + 预分析 (c) 复证）**：`DrpDemandAggregator.loadParametersInScope` 仅按 `plan.orgId` eq 过滤（plan.orgId 为 null 时零过滤），`DrpEngine` 对每条 in-scope parameter **无条件**生成 `ErpDrpLine`（net=0 clamp 不跳过）挂到本 planId；C20a fixture plan/param orgId 全 "2" 且断言 `lines.size()==2` + `totalReplenishmentQty==140`——**种子 `erp_drp_parameter` 两行 ORG_ID 均 = "1"**（GROUP-HQ；org CSV 仅 1/2 两行，"1" 为唯一 FK 合法替代），对 C20a/C20b 及 5 个 drp e2e action spec（全部 orgId="2"）恒不可见；禁 null 同样满足。
- **(warehouseId, materialId) 组合 belt-and-braces**：种子 (wh1,mat1) / (wh2,mat3) 与 C20a fixture (2,1)/(1,2) 及 C20b/E2E 常用 (1,4)/(2,4) 全不相交——若未来载重判据扩展到组合维度仍有余量。
- **C20b ParamResolver 零泄漏（预分析 (c) 复证）**：`ErpDrpSimulationParamResolver.loadParams` scenarioId-scoped（scenarioId 键缓存 + 测试内 invalidateCache），种子 scenario/param 行携带自有 scenarioId 恒不命中 C20b 场景；`SimulationDrpEngine` 仅按显式 scenarioId 装载（DRAFT 守卫），无场景枚举面。
- **staging 超时扫描面避让（预分析 (d) 复证）**：`ErpDrpCrossDockStagingTimeoutJob` 扫描 = status=STAGING AND updateTime>24h（job yaml enabled 缺省 false 双门控）——种子 cross_dock 取 COMPLETED/CANCELLED + dock_appointment 自由串 COMPLETED/CANCELLED + 静态过期日期，三重不可见；dock_appointment 无任何扫描器。模块级回归 `TestErpDrpCrossDock#testStagingTimeoutFallbackJob` 位于 `module-drp/erp-drp-service`（pom 不依赖 app-erp-all、测试资源无 `_init-data`，localDb 自建 fixture）——classpath 不含本批种子，零交互。
- **E2E/视觉消费面**：drp 5 个 action spec（plan-engine / release-line / release-approved / simulation / safety-stock）全部 orgId="2" + 自建唯一物料/参数 + planId/(mat,wh) 维度精确断言——org="1" 种子零交集；`drp.smoke.spec.ts` 渲染型零计数断言；`ext-domains-child-table.visual.spec.ts`（/ErpDrpPlan-main view drawer lines sub-grid-view）此前 no-row skip，种子后激活真实行路径（结构断言，Phase 3 实跑核验）；drp 无 dashboard/report spec（grep 实证），零 KPI 漂移面。
- **预存 ORM quirk 登记**：`erp-drp/simulation-status` / `erp-drp/simulation-param-type` 字典被 ORM 列引用但未在任何 ORM `<dicts>` 定义——种子取 `ErpDrpConstants`（:84-92）运行时词汇 `DRAFT`/`ARCHIVED`/`SAFETY_STOCK`/`LEAD_TIME`（零校验影响，字典补定义归 successor）。
- **集成快照零漂移**：`app-erp-all/_cases` drp 相关用例仅 C20a/C20b（均无种子计数断言，断言全部 own planId/material 维度）；纯加性插入不入既有快照。
- **UK/数值自洽**：plan/scenario/cross_dock/safety_stock_calc UK(code,orgId)、scenario_version UK(scenarioId,versionNo)、scenario_param UK(scenarioId,materialId,warehouseId,paramType)、supplier_score UK(supplierId,materialId) 逐行独立；plan2 total 140 = 其 line suggested 140。

## 5. 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准，脚本化全等校验 11/11）；省略审计列；日期 ISO、时间戳空格分隔；小写布尔逐列对齐；ID < 100000（各表独立自 1 起段）；文本列取无逗号演示值；字典码 ∈ `module-drp/model/app-erp-drp.orm.xml` `<dicts>`（erp-drp/ + erp-inv/ 命名空间；simulation 双字典按运行时词汇，见 quirk 登记）。
- 拓扑序由 `DataInitInitializer` 自动排序（line 晚于 plan；param/version 晚于 scenario；dock_appointment 晚于 cross_dock），无需手工排序文件名。
