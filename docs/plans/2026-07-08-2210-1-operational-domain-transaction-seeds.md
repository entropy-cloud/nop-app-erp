# 2026-07-08-2210-1-operational-domain-transaction-seeds 运营域（库存/资产/项目）业务交易单据种子数据

> Plan Status: completed
> Mission: erp
> Work Item: 运营域（inventory/assets/projects）业务交易单据 + 计算产物部署期种子（最小连通集）
> Last Reviewed: 2026-07-08
> Source: deferred 项承接 `docs/plans/2026-07-08-1445-1-p2p-o2c-transaction-seed-data.md` Non-Goals「扩展域交易单据（manufacturing/HR/assets/quality/maintenance/.../projects）——按域逐批补充」（Successor Required: yes，触发条件「各域端到端业务数值回归需对应域交易数据时，按域逐批补充」——**已满足**：AGENTS.md「当前项目阶段」明示当前重点「各域细化端到端验证」，1445-1 已落地 P2P+O2C 种子范式）；同计划 Deferred「精确 KPI/报表数值断言 → 1445-2 successor」之数据层前置；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-1445-1-p2p-o2c-transaction-seed-data.md`（completed，P2P+O2C 种子范式，本计划同机制向运营域延伸）、`docs/plans/2026-07-08-2210-2-operational-domain-value-assertions.md`（同批 N=2，依赖本计划固化运营域种子才能断言具体数值）、`docs/plans/2026-07-08-1445-2-data-driven-e2e-value-assertions.md`（completed，数值断言范式 + helper，运营域断言 successor 复用）
> Audit: required

## Current Baseline

实时仓库逐项核实（`ls`/`rg`/`read`，非采信旧记忆）：

- **部署期 seed 机制已就绪（1234-1/1445-1 交付）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 现含 **44 张 CSV**（`ls` 计数=44：21 张主数据 `erp_md_*` + 23 张 P2P/O2C 交易 `erp_{pur,sal,fin}_*`）。`DataInitInitializer` 经 `-Dnop.orm.init-database-data=true` + fresh-DB 重置（`playwright.config.ts` webServer `rm -f db/erp.mv.db db/erp.trace.db`）触发，按 ORM `getEntityModelsInTopoOrder()` 拓扑序插入，**非幂等**（1234-1 已确认 + 平台 bug `ensureOrmTemplateSessionFactory()` 已修复）。「源单据 + 下游财务产物直 seed」范式已在 1445-1 落地（P2P+O2C 含凭证/辅助账/GL 余额）。
- **主数据 FK 上游已就绪（约束本计划引用）**：`erp_md_material`/`material_sku` 各 4 行、`erp_md_partner` 4 行、`erp_md_warehouse` 2 行、`erp_md_location` 若干、`erp_md_employee` 3 行、`erp_md_currency` 2 行、`erp_md_uom` 4 行、`erp_md_organization`/`acct_schema`/`cost_center` 若干、`erp_md_subject` 8 行 GL 科目（**无库存/资产/项目专用科目**——见下方 posted 裁决）。本计划运营域种子的 FK（materialId/partnerId/warehouseId/employeeId/currencyId/uomId/...）必须引用上述已 seed 固定 ID。
- **运营域看板/报表读「域表」非「GL 凭证」**（关键，决定 seed 策略）——经 `dashboards.md` §3/§5/§6 + 各域 `ErpXxxDashboardBizModel.getDashboardKpi` 核实：
  - **库存看板**（`ErpInvDashboardBizModel.getDashboardKpi`，`module-inventory/erp-inv-service/.../dashboard/ErpInvDashboardBizModel.java:62`）：库存总值 = Σ `ErpInvStockBalance.totalCost`；本期出入库量 = Σ `ErpInvStockMove`（DONE，期内）；缺料预警 = `ErpInvStockBalance.availableQuantity < 阈值`。**读 stock_balance / stock_move，非 GL**。
  - **资产看板**（`ErpAstDashboardBizModel.getDashboardKpi`，`.../ErpAstDashboardBizModel.java:51`）：资产原值 = Σ `ErpAstAsset.originalValue`（IN_SERVICE）；累计折旧 = Σ `accumulatedDepreciation`；本期折旧 = Σ `ErpAstDepreciationSchedule.actualAmount`（status=EXECUTED）。**读 asset / depreciation_schedule，非 GL**。
  - **项目看板**（`ErpPrjDashboardBizModel.getDashboardKpi`，`.../ErpPrjDashboardBizModel.java:58`）：在手项目数 = count `ErpPrjProject`（OPEN）；已发生成本 = Σ `ErpPrjCostCollection.totalAmount`；项目毛利率 = `ErpPrjProjectPnl` Σ grossProfit / Σ revenue（1100-3 已接 `getProjectGrossMargin`）。**读 project / cost_collection / project_pnl，非 GL**。
  - **结论**：seed 运营域域表（stock_balance/asset/depreciation_schedule/project/cost_collection/project_pnl）即令三域看板 KPI **非空**，**无需 seed GL 凭证**（区别于 P2P+O2C 财务看板读 GL 的范式）。这是本计划相对 1445-1 的**复杂度减负**。
- **运营域表存在（本计划 seed 候选，逐表 `rg tableName` 核实）**：
  - 库存（`module-inventory/model/app-erp-inventory.orm.xml`）：`erp_inv_stock_move`（code/moveType 字典 `erp-inv/operation-type`/businessDate/sourceWarehouseId/destWarehouseId/docStatus 字典 `erp-inv/move-status`/approveStatus/posted）+ `erp_inv_stock_move_line`（moveId/materialId/skuId/uoMId/quantity/unitCost/totalCost/currencyId）、`erp_inv_stock_balance`（materialId/skuId/warehouseId/totalQuantity/availableQuantity/avgCost/totalCost/costMethod 字典 `erp-md/cost-method`）、`erp_inv_cost_layer`（materialId/warehouseId/costMethod 字典 `erp-md/cost-method` mandatory/incomingQuantity/remainingQuantity/unitCost/totalCost/incomingDate/incomingMoveId）
  - 资产（`module-assets/model/app-erp-assets.orm.xml`）：`erp_ast_asset`（code/orgId/categoryId→`erp_ast_asset_category`/acquisitionDate/currencyId/originalValue/depreciationMethod 字典 `erp-ast/depreciation-method`/status 字典 `erp-ast/asset-status`{IN_SERVICE,...}/accumulatedDepreciation/netBookValue/departmentId/locationId）+ `erp_ast_depreciation_schedule`（assetId/orgId/period/plannedAmount/actualAmount/accumulatedDepreciation/netBookValue/status 字典 `erp-ast/depreciation-schedule-status`{EXECUTED,...}/posted/voucherId/businessDate）
  - 项目（`module-projects/model/app-erp-projects.orm.xml`）：`erp_prj_project`（code/name/orgId/projectTypeId→`erp_prj_project_type`/customerId→`erp_md_partner`/currencyId/startDate/endDate/status 字典 `erp-prj/project-status`/managerId→`erp_md_employee`）+ `erp_prj_cost_collection`（code/orgId/projectId/currencyId/status/posted）、`erp_prj_project_pnl`（code/projectId/revenueAmount/costLabor/costMaterial/costExpense/costSubcontract/totalCost/grossProfit/grossMarginPct/amountFunctional）、`erp_prj_timesheet`（projectId/...）、`erp_prj_budget`（projectId/...）
- **上游配置表缺失（须本计划补 seed）**：`erp_ast_asset_category`（code/name mandatory；subjectId/depreciationSubjectId/expenseSubjectId 可空——无 GL 科目时留空）、`erp_prj_project_type`（code/name mandatory；defaultSubjectId 可空）**均不在 1234-1 的 21 张 `erp_md_*` 主数据中**（域配置表非主数据）。本计划须将其作为运营域种子的**上游配置**先 seed（拓扑序先于 asset/project）。
- **既有 E2E 套件 59 spec 全绿**（1445-2：10 看板 + 24 报表 + 18 CRUD + 1 KB + 6 数值断言），运营域看板/报表当前在 P2P+O2C 种子库下 KPI 仍为 0/空（P2P+O2C 不 seed 库存/资产/项目表）——证明**运营域交易数据是这三域看板/报表数值非零的唯一阻塞**。
- **保护区域**：纯数据文件（CSV）。**非 `model/*.orm.xml` ask-first**（零 ORM 变更）。属 `plan-first`（跨域 seed + 跨多会话 + >5 文件 + 首次 seed 运营域参照完整性）。

剩余差距：(1) 库存/资产/项目三域零业务数据，看板/报表数值为空，无法演示/数据驱动验证运营域端到端态；(2) 上游域配置表（asset_category/project_type）未 seed，须先补；(3) 哪些运营域表/记录纳入首批最小连通集、每表列 code 映射、跨域加载拓扑序（域配置 → 域头/行 → 计算产物）待 Phase 1 盘点；(4) posted 裁决（无 GL 科目 → posted=false，看板不依赖 posted）。

## Goals

- 在 `_vfs/_init-data/` 增补运营域（inventory/assets/projects）最小连通集**业务交易 + 计算产物种子 CSV**：库存（stock_move + line + stock_balance + cost_layer）、资产（asset + depreciation_schedule，含 asset_category 上游配置）、项目（project + cost_collection + project_pnl + timesheet + budget，含 project_type 上游配置），全部以一致 FK 串联并引用 1234-1 已 seed 主数据。
- 经既有 config-gated fresh-DB 启动加载，验证全部新 CSV **0 主键冲突 / 0 列映射错误 / 0 参照完整性失败**。
- 经 GraphQL 抽样验证种子运营数据可见且域内 FK 一致（如 stock_balance.totalCost 与 cost_layer 一致；asset.accumulatedDepreciation 与最新 depreciation_schedule.accumulatedDepreciation 一致；project_pnl.totalCost 与 cost_collection Σ 一致）。
- 复跑既有 59 spec E2E 在种子库上 **0 回归**，并观测 inventory/assets/projects 三域看板 KPI **非空**（解除运营域交易数据层阻塞的证明）。
- 解除 1445-1 Non-Goals/Deferred「扩展域交易单据（assets/projects 子集）」+ 为 N=2「运营域数值断言」提供固化数据基线。

## Non-Goals

- **不**seed 其他扩展域（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps）——属后续批次（触发条件：对应域端到端业务数值回归需交易数据时，按域逐批补充）。manufacturing 因 BOM/routing/work-order/job-card/material-issue 链复杂度高，单独 successor。
- **不**seed GL 凭证/辅助账/GL 余额 for 运营域——三域看板读域表非 GL；且 1234-1 种子科目表无库存/资产/项目专用科目。`posted` 统一置 `false`（镜像 1445-1「PO/SO posted=false（无对应 GL 凭证）」裁决）；运营域过账 → GL 凭证 seed 归后续（触发条件：运营域业财一体端到端数值回归需 GL 串联时）。
- **不**做精确 KPI/报表数值断言（断言「库存总值 = ¥X」）——本计划解除**运营域交易数据存在**阻塞（数值非零可观测）；精确数值回归是 N=2 successor 层。
- **不**修改 `model/*.orm.xml`——纯数据文件，零 ORM 变更。
- **不**seed 退货/红冲/反向移动链——涉反向库存/红字凭证，复杂度高，属后续批次。
- **不**做多组织/多币种/多期间复杂场景——单组织/本位币最小连贯集（镜像 1445-1 范式）。
- **不**填充 test-scope `app-erp-test-data`（Java 测试夹具）——独立资产（1234-1 Deferred，保持）。

## Task Route

- Type: `implementation-only change`（纯数据文件 CSV + 域表列 code 映射 + 跨域 FK 参照完整性；零 ORM/契约/认证变更；首次 seed 运营域但沿用 1445-1 已确立范式，非新架构）
- Owner Docs: `docs/architecture/seed-data.md`（部署 seed 范式 + 「源单据 + 下游产物直 seed」）、`docs/design/dashboards.md` §3/§5/§6（库存/资产/项目看板数据源，判定哪些域表须 seed）、`docs/design/inventory/costing-methods.md`（库存估值/成本层语义）、`docs/design/assets/depreciation-and-posting.md`（折旧计划语义）、`docs/design/projects/cost-collection.md`+`profitability.md`（项目成本/损益语义）、`docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`（列 code 映射 + 拓扑序范式）
- Skill Selection Basis: `nop-backend-dev`（ORM 实体列 code 映射 + 跨域 FK 参照完整性 + 域计算产物一致性约束 stock_balance↔cost_layer / asset↔depreciation_schedule / project_pnl↔cost_collection；seed 是数据层工作但需深刻理解运营域计算产物结构）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`app-erp-all/target/quarkus-app/quarkus-run.jar`（1445-1 已建立 webServer 范式，无需改 `playwright.config.ts`）。
- H2 文件库：`./db/erp`；fresh-DB 重置（删 `db/erp.mv.db db/erp.trace.db`）+ `-Dnop.orm.init-database-data=true`（已就绪）。
- 回滚策略：seed 为纯新增 CSV，失败不影响生产构建；移除新增 `_init-data` 运营域 CSV 即回滚（主数据 + P2P/O2C seed 不受影响）。

## Execution Plan

### Phase 1 - 运营域表盘点 + 列 code 映射 + 拓扑序 + 范围裁决（Proof + Decision）

Status: completed
Targets: `module-{inventory,assets,projects}/model/app-erp-*.orm.xml`、`docs/design/dashboards.md` §3/§5/§6、`docs/design/{inventory,assets,projects}/*.md`
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 1445-1 seed 机制 + 范式就绪（已满足）

- [x] `Proof`：逐表读取本期范围内运营域表的**列 code 清单**（CSV 列名须匹配 `code` 即大写数据库列名），标注 mandatory 列（须填）、FK 列（引用 1234-1 已 seed 主数据或本批先 seed 的上游配置/单据）、framework-managed 列（`orgId`/审计列由 ORM 拦截器自动填，CSV 省略——按 1445-1 实测 `CREATED_BY='sys'` 范式）。范围至少含：inventory stock_move(+line)/stock_balance/cost_layer、assets asset_category(asset 上游配置)/asset/depreciation_schedule、projects project_type(project 上游配置)/project/cost_collection/project_pnl/timesheet/budget。产出「运营域表清单 + 列映射 + 加载拓扑序」分析文档（拓扑序须解决跨域排序：域配置 asset_category/project_type → 域头 asset/project → 域行/计算产物 depreciation_schedule/cost_collection/project_pnl/stock_balance/cost_layer）。
      - Skill: `nop-backend-dev`
- [x] `Decision`：首批运营域最小连通集范围裁决——确定每张表的 seed 行数（领先方案：每域 1-2 条最小连通记录，资产 2-3 台含 1 台已折旧、库存 2 物料余额 + 1 条 DONE 移动、项目 1 OPEN 项目含成本 + PnL）与具体记录设计（物料/伙伴/仓库/员工/科目引用 1234-1 固定 ID；资产类别/项目类型自建固定 ID）。记录选择依据（哪些是 inventory/assets/projects 看板/报表数值非零的最小前提）与残留风险（参照完整性遗漏致启动失败的具体防护——Phase 3 fresh-DB 加载验证兜底）。
      - 替代方案考虑：(a) 同时 seed 运营域 GL 凭证（rejected——三域看板读域表非 GL，且种子科目表无运营域专用科目，seed GL 徒增参照复杂度且不解除额外阻塞）；(b) seed 全部 13 扩展域（rejected——复杂度爆炸，按域逐批是 1445-1 Deferred 既定策略）；(c) 仅 seed 库存不 seed 资产/项目（rejected——三域看板并列缺数据，且资产/项目 seed 同样 tractable，合并一批降低 N=2 断言计划跨批碎片）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 运营域表清单 + 列 code 映射 + 跨域加载拓扑序分析文档落盘（`docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`），含每表 mandatory/FK 角色标注 + 范围 Decision（含替代方案 + 残留风险）。
- [x] 范围 Decision 明确每域 seed 行数 + posted=false 裁决依据（无 GL 科目 + 看板不依赖 posted）。

### Phase 2 - 编写运营域种子 CSV（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/erp_{inv,ast,prj}_*.csv`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 列映射 + 拓扑序 + 范围裁决落盘

- [x] `Add`：按 Phase 1 拓扑序编写运营域种子 CSV，列名对齐实体 `code`（UPPER_SNAKE_CASE），mandatory 业务列全填，FK 列引用上游固定 ID（每表独立 1,2,3…序列，延续 1445-1 ID 约定），framework-managed 列（CREATED_BY/CREATE_TIME/DEL_VERSION/VERSION/UPDATED_BY/UPDATE_TIME）省略。资产类别（asset_category）/项目类型（project_type）作为上游配置先 seed。库存 stock_balance 与 cost_layer 的 totalCost/unitCost 保持金额自洽；资产 asset.accumulatedDepreciation/netBookValue 与最新 depreciation_schedule 同名字段一致；项目 project_pnl.totalCost 与 cost_collection Σ amount 一致。
      - Skill: `nop-backend-dev`
- [x] `Proof`：脚本或逐表校验所有新 CSV 列名对齐 ORM `code`（0 错配）+ mandatory 业务列全填 + FK 引用上游存在（沿用 1445-1 校验范式）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 运营域 CSV 全部落地 `_vfs/_init-data/`，列名校验 0 错配，mandatory 业务列全填，域内金额自洽（stock_balance↔cost_layer / asset↔depreciation_schedule / project_pnl↔cost_collection）。

### Phase 3 - 启动验证 + GraphQL 抽样 + E2E 0 回归（Proof）

Status: completed
Targets: fresh-DB 启动日志、`/graphql` 抽样、既有 59 spec E2E
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 2 CSV 落地

- [x] `Proof`：`mvn clean install -DskipTests`（154 模块）确认新 CSV 打包入 app jar（沿用 1445-1 范式）。
      - Skill: none
- [x] `Proof`：fresh-DB 启动（删 `db/erp.mv.db db/erp.trace.db` + `-Dnop.init-database-data=true`）成功，日志确认全部 CSV（44 既有 + 新增运营域）`load-csv-data` 成功，**0 主键冲突 / 0 列映射错误 / 0 参照完整性失败**。
      - Skill: `nop-backend-dev`
- [x] `Proof`：GraphQL 抽样（`/graphql`）验证运营域种子可见且 FK 一致——库存 stock_balance.totalCost 非空、资产 asset.originalValue/accumulatedDepreciation 非空 + 与 depreciation_schedule 一致、项目 project(OPEN) + cost_collection.amount + project_pnl.revenueAmount/grossProfit 非空；三域看板 getDashboardKpi 由 0 转非空。
      - Skill: `nop-backend-dev`
- [x] `Proof`：`npx playwright test`（全套件 fresh-DB seed）复跑 **既有 59 spec 0 回归**（新运营域 CSV 不破坏 P2P+O2C 种子 + 既有断言）。
      - Skill: none

Exit Criteria:

- [x] fresh-DB 启动全部 CSV 0 冲突/0 列映射错误/0 参照失败；GraphQL 抽样运营域 FK 一致 + 三域看板 KPI 非空；既有 59 spec E2E 0 回归。

### Phase 4 - 文档对齐 + Deferred 解除登记（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/known-good-baselines.md`、`docs/testing/e2e-runbook.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3 全绿

- [x] `Add`：`docs/architecture/seed-data.md` 增「运营域交易单据种子（库存/资产/项目）」段（域表直 seed 范式 + 域配置上游 + posted=false 裁决 + 看板读域表非 GL 的依据 + Non-Goal 其他扩展域/GL 凭证）。
      - Skill: none
- [x] `Add`：`docs/testing/known-good-baselines.md` 增运营域种子基线行（fresh-DB CSV 总数 44→44+N + 0 冲突 + 三域 KPI 非空 + 59 spec 0 回归）；`docs/testing/e2e-runbook.md` 种子库段同步（运营域 CSV 计数 + KPI 非空域）。
      - Skill: none

Exit Criteria:

- [x] seed-data.md 含运营域种子段；known-good-baselines + e2e-runbook 种子库计数/域同步；1445-1 Deferred「扩展域交易单据（assets/projects 子集）」+ 1445-2 Deferred 触发条件（运营域 seed 后）登记解除（本计划 Closure 段登记，为 N=2 解除数据层阻塞）。

## Draft Review Record

- Independent draft review iteration 1: accept (`ses_0bdea56b5ffeRNqRTqCQ9m1Gg8`，独立 general 子代理，新会话冷重播无执行者上下文) — 全部基线主张经实时仓库逐项核实为真：CSV 计数 44（21 主数据 + 23 P2P/O2C，零运营域表）/运营域实体及列存在/三域看板 getDashboardKpi 读域表非 GL（支撑 posted=false Non-Goal 的关键假设已确认）/asset_category+project_type 未 seed（上游配置依赖确认）/主数据 FK 行数一致/59 spec 基线一致/1445-1 Deferred 触发条件满足。格式合规（命名/元数据/九大章节/四阶段/item types `Proof|Decision`/`Add`/`Proof`/`Add`/skill 逐项标注/Phase 1 Decision 含替代方案 a/b/c + 残留风险/三 Deferred 各带 Classification + 触发条件/Closure Gates 含 build + fresh-DB 0 冲突 + GraphQL FK 一致 + 59 spec 0 回归 + 独立审计）。无 BLOCKER/MAJOR。3 MINOR 基线列名勘误已修订：项目看板 `ErpPrjCostCollection.amount`→`totalAmount`；cost_layer 补 mandatory `costMethod`；project_pnl 移除不存在的 `actualCost`（属 ErpPrjProject 而非 ProjectPnl）。草案已收敛为可接受执行契约。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。本计划结果表面为部署期数据（CSV），无生产 Java 代码变更；验证门控以 fresh-DB seed 加载 + 既有 E2E 0 回归 + GraphQL FK 一致为主。

- [x] 范围内行为完成（运营域三域种子 CSV 落地 + 域配置上游 + 域内金额自洽）
- [x] 相关文档对齐（seed-data.md + known-good-baselines + e2e-runbook）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块）+ fresh-DB seed 加载（0 冲突/0 列映射错误/0 参照失败）+ GraphQL 抽样 FK 一致 + `npx playwright test`（既有 59 spec 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up（其他扩展域/GL 凭证 seed/退货链/精确数值断言均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 运营域 GL 凭证/业财一体 seed（库存估值凭证 / 资产取得+折旧凭证 / 项目成本凭证）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 三域看板读域表（stock_balance/asset/depreciation_schedule/project_pnl）非 GL；1234-1 种子科目表无运营域专用科目。seed GL 凭证不解除额外看板阻塞，徒增参照复杂度。
- Successor Required: `yes`
- Trigger Condition: 当运营域业财一体端到端数值回归需 GL 串联（凭证↔源单据↔辅助账）时，按域逐批补 GL 凭证 seed + 扩展种子科目表。

### 其他扩展域交易种子（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅覆盖 inventory/assets/projects 三域（tractability 最高的运营域）。其余扩展域按域逐批是 1445-1 Deferred 既定策略；manufacturing 因 BOM/routing/work-order 链复杂度单独 successor。
- Successor Required: `yes`
- Trigger Condition: 当对应扩展域看板/报表端到端数值回归需交易数据时，按域逐批补 seed。

### 精确运营域 KPI/报表数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划解除「运营域交易数据存在」阻塞（数值非零可观测）；精确断言「库存总值 = ¥X」需固定种子集确定性 + 断言逻辑，是 N=2 successor 层。
- Successor Required: `yes`
- Trigger Condition: 本计划固化后，由 `2026-07-08-2210-2-operational-domain-value-assertions.md` 承接。

## Closure

Status Note: 执行完成（2026-07-08，主代理执行 4 阶段全绿）。13 张运营域表 CSV 落地 `_vfs/_init-data/`（库存 4 张：stock_move/stock_move_line/stock_balance×2/cost_layer×2；资产 3 张：asset_category×2/asset×3 IN_SERVICE 含 1 已折旧/depreciation_schedule×1 EXECUTED；项目 6 张：project_type/project OPEN/cost_collection/timesheet/budget/project_pnl CALCULATED），共 18 行，引用 1234-1 主数据固定 ID，posted 统一 false（无 GL 科目 + 看板读域表非 GL）。验证全绿：`mvn clean install -DskipTests`（154 模块，1:25）BUILD SUCCESS；fresh-DB 启动（57 CSV = 21 主数据 + 23 P2P/O2C + 13 运营域）0 冲突/0 列映射错误/0 参照失败，11.3s started；`npx playwright test`（59 spec，8.6m）0 回归；GraphQL 抽样三域 KPI 由 0 转非空且 FK 一致（inv totalValue=10450/incomingQty=100、ast originalValue=135000/accumulatedDepreciation=6000/periodDepreciation=2000、prj openProjectCount=1/incurredCost=30000/grossProfit=20000）。域内金额自洽经脚本校验（stock_balance↔cost_layer / asset↔depreciation_schedule / project_pnl↔cost_collection）。文档对齐：seed-data.md 增运营域种子段、known-good-baselines + e2e-runbook 种子库计数 44→57 + 三域 KPI 非空域。Deferred 解除登记：(1) 1445-1 Non-Goals「扩展域交易单据（assets/projects 子集）」——inventory/assets/projects 子集已 seed，其余扩展域仍按域逐批（本计划 Non-Goal）；(2) 1445-2 Deferred「扩展域数值断言」触发条件「运营域 seed 后」——已满足，数据层阻塞解除，由 `2026-07-08-2210-2` 承接断言层。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0bdc93001ffeFTpoPS6RH8JNZW`（general，新会话冷重播无执行者上下文）。VERDICT: **PASS**（无 BLOCKER）。逐项核实：13 CSV 存在（目录计数 57）/ 列名对齐 ORM `code`（0 错配，含关键陷阱 `UO_M_ID` 已正确确认是 ORM 真实 code）/ FK 参照完整（material/sku/uom/org/currency/partner/employee/warehouse + 域内头行全解析）/ mandatory 业务列全填 / 金额自洽数值校验通过（stock_balance↔cost_layer 850+9600、asset 2↔schedule 6000/114000、project_pnl↔cost_collection 30000）/ posted 全 false（5 张含 POSTED 列 CSV）/ 文档对齐（seed-data.md + known-good-baselines + e2e-runbook 均 57/运营域）/ 分析文档落盘 / roadmap ✅ done / 13 CSV 已打包入 target。审计发现的 2 MAJOR（每日日志缺失 + Closure Gates 未勾）+ 1 MINOR（Plan Status header active）均为流程簿记，已由执行者修复：补 `docs/logs/2026/07-08.md` 2210-1 条目、勾选自验证 Closure Gates、Plan Status 翻 completed。无技术交付物返工。

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
