# assets 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.2c（plan `docs/plans/2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md`，2026-09-02）。此前 3 张 assets 表 seed（asset_category / asset / depreciation_schedule，plan `2026-07-08-2210-1`）见 `docs/architecture/seed-data.md` 历史批次段。
> **业务语义 owner docs**：`docs/design/assets/` 各域文档（cip.md / disposal / state-machine.md / depreciation-and-posting.md / inventory.md / maintenance.md / split-merge.md / use-cases.md）；本文件只登记种子数据面，不重复业务语义。

## 种子数据范围（M1.2c 批次 17 表）

assets 域 20 规格实体中，3 表已由历史批次 seed；本批补齐其余 **17 规格表（26 行）**，达成 assets 域全量 seed 覆盖（20 / 20）。

### 17 规格表 CSV

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpAstAssetModel（资产型号） | `erp_ast_asset_model.csv` | 1 | P | —（无必填 FK）；CATEGORY_ID 可选→ErpAstAssetCategory〔已seed 1〕 |
| ErpAstAssetActionLog（资产操作审计） | `erp_ast_asset_action_log.csv` | 1 | P | ASSET_ID→ErpAstAsset〔已seed 1〕；EVENT_TYPE/FROM_STATUS/TO_STATUS ∈ `erp-ast/*` 字典 |
| ErpAstMovement（资产移动） | `erp_ast_movement.csv` | 2 | P+N-TERM(CANCELLED) | ASSET_ID→ErpAstAsset〔已seed 1/2〕；HANDLER_ID 可选→ErpMdEmployee〔跨域:md·已seed 3〕；DOC_STATUS/APPROVE_STATUS ∈ `erp/doc-status`/`wf/approve-status` |
| ErpAstValueAdjustment（资产价值调整） | `erp_ast_value_adjustment.csv` | 2 | P+N-TERM(CANCELLED) | ASSET_ID→ErpAstAsset〔已seed 2/3〕；ADJUSTMENT_TYPE ∈ `erp-ast/adjustment-type` |
| ErpAstDisposal（资产处置） | `erp_ast_disposal.csv` | 2 | P+N-TERM(CANCELLED) | ASSET_ID→ErpAstAsset〔已seed 3/1〕；DISPOSAL_TYPE/REASON ∈ `erp-ast/disposal-*` |
| ErpAstAssetCapitalization（资产资本化） | `erp_ast_asset_capitalization.csv` | 2 | P+N-TERM(CANCELLED) | —（无必填 FK）；CATEGORY_ID 可选→ErpAstAssetCategory〔已seed 2/1〕；SOURCE_CODE 软引用本批 CIP-2026-001 |
| ErpAstCip（在建工程） | `erp_ast_cip.csv` | 2 | P+N-TERM（全转固终态，见「零漂移设计」） | COMPLETED_ASSET_ID 可选→ErpAstAsset〔已seed 2/3〕；PROJECT_ID 可选→ErpPrjProject〔跨域:prj·已seed 1〕；STATUS ∈ `erp-ast/cip-status` |
| ErpAstCipCostItem（CIP 成本归集行） | `erp_ast_cip_cost_item.csv` | 2 | P（挂 cip 1） | CIP_ID→ErpAstCip〔本批 1〕；CAPITALIZATION_ID 可选→ErpAstAssetCapitalization〔本批 1〕；COST_TYPE ∈ `erp-ast/cip-cost-type` |
| ErpAstCipProgressBilling（CIP 进度付款） | `erp_ast_cip_progress_billing.csv` | 2 | P（挂 cip 1） | CIP_ID→ErpAstCip〔本批 1〕 |
| ErpAstSplit（资产拆分单） | `erp_ast_split.csv` | 2 | P+N-TERM(CANCELLED) | SOURCE_ASSET_ID→ErpAstAsset〔已seed 2〕 |
| ErpAstSplitLine（资产拆分行） | `erp_ast_split_line.csv` | 2 | P（挂 split 1） | SPLIT_ID→ErpAstSplit〔本批 1〕；CATEGORY_ID 可选→ErpAstAssetCategory〔已seed 2〕；TARGET_ASSET_ID 留空（目标卡未生成语义） |
| ErpAstMerge（资产合并单） | `erp_ast_merge.csv` | 2 | P+N-TERM(CANCELLED) | TARGET_ASSET_ID→ErpAstAsset〔已seed 2〕 |
| ErpAstMergeLine（资产合并行） | `erp_ast_merge_line.csv` | 2 | P（挂 merge 1） | MERGE_ID→ErpAstMerge〔本批 1〕；SOURCE_ASSET_ID→ErpAstAsset〔已seed 1/3〕 |
| ErpAstInventory（资产盘点单） | `erp_ast_inventory.csv` | 2 | P(COUNTING)+N-TERM(CANCELLED) | —（无必填 FK）；RANGE_DEPARTMENT_ID/RESPONSIBLE_BY_ID 可选→ErpMdOrganization/ErpMdEmployee〔跨域:md·已seed 2/3〕 |
| ErpAstInventoryLine（资产盘点行） | `erp_ast_inventory_line.csv` | 1 | P（挂 inventory 1） | INVENTORY_ID→ErpAstInventory〔本批 1〕；ASSET_ID 可选→ErpAstAsset〔已seed 1〕；DISPOSITION ∈ `erp-ast/inventory-line-disposition` |
| ErpAstMaintenance（资产维修工单） | `erp_ast_maintenance.csv` | 2 | P(COMPLETED+EXPENSE)+N-TERM(CANCELLED) | ASSET_ID→ErpAstAsset〔已seed 1/3〕；STATUS/TREATMENT ∈ `erp-ast/maintenance-*` |
| ErpAstMaintenanceCost（维修费用行） | `erp_ast_maintenance_cost.csv` | 2 | P（挂 maintenance 1） | MAINTENANCE_ID→ErpAstMaintenance〔本批 1〕；COST_TYPE ∈ `erp-ast/maintenance-cost-type` |

## FK 闭环图

```
[跨域:md·已seed] erp_md_organization(1/2)、erp_md_employee(1/2/3)、erp_md_currency(1/2)、erp_md_location(1/2)
[跨域:prj·已seed] erp_prj_project(1)
[已seed·ast] erp_ast_asset_category(1/2)、erp_ast_asset(1 笔记本 12000/2 机床 120000/3 打印机 3000)、erp_ast_depreciation_schedule
   ──ASSET_ID/SOURCE_ASSET_ID/TARGET_ASSET_ID/COMPLETED_ASSET_ID──▶ action_log / movement / value_adjustment / disposal
                                                                    / split / split_line / merge / merge_line / inventory_line / maintenance / cip
[本批] erp_ast_cip(1..2) ──CIP_ID──▶ cip_cost_item（Σ=80000=cip1.accumulated_cost）/ cip_progress_billing
[本批] erp_ast_split(1..2) ──SPLIT_ID──▶ split_line（Σ 原值 120000 / 折旧 6000 / 净值 114000 = 源资产 2）
[本批] erp_ast_merge(1..2) ──MERGE_ID──▶ merge_line（Σ 原值/净值 15000 = 源资产 1+3，比例 0.8/0.2）
[本批] erp_ast_inventory(1..2) ──INVENTORY_ID──▶ inventory_line（book_value 12000 = 资产 1 净值）
[本批] erp_ast_maintenance(1..2) ──MAINTENANCE_ID──▶ maintenance_cost（Σ=800=maintenance1.total_cost_amount）
[本批] erp_ast_asset_capitalization(1) ◀──CAPITALIZATION_ID── cip_cost_item（转固结转链：cip 1 → cap 1 → 资产 2）
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空、白名单零增量（`TestErpSeedDataIntegrity` 全绿背书）。19 实体 ORM 零 UoM 引用列（`UO_M_ID` 分叉拼写陷阱不适用，已按 ORM `code=` 全量核对）。

## 干扰面零漂移设计（本批核验结论）

- **看板 KPI 零漂移（cip 裁决）**：`ErpAstDashboardBizModel.sumCipBalance()` 消费 `ErpAstCip` 过滤 `isCompleted=false` 求 Σ `accumulatedCost`。本批 2 行 Cip 全部落**转固完成终态**（`STATUS=TRANSFERRED` + `IS_COMPLETED=true` + `ACCUMULATED_COST>0` 与子表 Σ 自洽），过滤集为空 → `cipBalance` 恒 0，`/ast-dashboard-main` 像素快照零漂移。其余 16 表零进入看板 KPI 读取面（Asset/AssetCategory/DepreciationSchedule 均为既有行）。
- **报表面零漂移**：`ast-disposal.value.spec`（title-token 子集断言）/ `reports.visual.spec` `ast-asset-disposal-detail`（静态标题/列头 token）/ `reports.download.spec` 均与数据行无关；`ast-depreciation.value.spec` 数值 token 来自既有 depreciation_schedule 行（本批不改）。disposal 种子行 businessDate 2026-07 落 `ErpAstReportBizModel` 默认查询区间，assetId 指向已 seed 资产 1/3，关系解析安全。
- **E2E 业务动作 spec 零干扰**：`ast-cip-capitalization / ast-inventory-count / ast-maintenance / ast-value-adjustment.action.spec` 均自建自清理、断言自建行状态迁移；`assets.value`/`assets.smoke` 断言键不含 `cipBalance`。
- 快照机制 `_chgType` 增量记录，显式种子 id < 100000 不入既有快照、不耗 default 序列；全批 posted=false（统一先例）。

## 域内金额/状态自洽约束

- 主状态语义沿 M1.2b 先例：P 行文档 = `DRAFT/UNSUBMITTED` 或业务完成态 + `posted=false`；N-TERM 行 = `CANCELLED/APPROVED` 终态（取消前曾审批通过）。
- **CIP 转固链**：cip 1（车间生产线改造，成本 80000 = 采购 65000 + 人工 15000）转固至资产 2（机床 120000 原值，资本化单 CAP-2026-001 SOURCE_TYPE=CIP 溯源）——成本归集行 `POSTED_TRANSFER_FLAG=true` + `CAPITALIZATION_ID=1` 结转闭环。
- **拆分行守恒**：split_line 2 行 Σ = 源资产 2 原值 120000 / 累计折旧 6000 / 净值 114000（PROPORTIONAL 0.6/0.4）。
- **合并行守恒**：merge_line 2 行 Σ 原值/净值 15000 = 源资产 1 + 3（比例按净值 12000:3000 = 0.8/0.2）。
- **盘点一致**：inventory_line book_value 12000 = 资产 1 净值；VARIANCE_TYPE=MATCHED + DISPOSITION=NONE（COUNTING 进行中不汇总结数）。
- **维修费用闭环**：maintenance_cost Σ=800 = maintenance 1 `TOTAL_COST_AMOUNT`（EXPENSE 费用化）。
- 维度值复用既有值域：组织 2；员工 1/2/3；币种 1；类别 1/2；项目 1；全部静态日期落在 2026-05~07 参考期（冻结时钟纪律，禁 `now()`/滚动期间语义）。

## 用例指示编码与 negative 行语义

- **P（最小正例行）**：全部 17 表均有。
- **N-TERM（终态行）**：movement id=2 / value_adjustment id=2 / disposal id=2 / asset_capitalization id=2 / split id=2 / merge id=2 / maintenance id=2 均 `CANCELLED`（取消终态，供非法迁移守卫负路径）；cip id=2 为「已转固工程」正例终态（`TRANSFERRED`，非取消语义——P/N-TERM 编码按规格表，转固终态即 N-TERM 分支）。

## 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列（含 posted/postedAt/postedBy/approvedBy/approvedAt 等默认值列）；ISO 日期（时间戳 ISO-T）；小写布尔；ID < 100000；字典码 ∈ `erp-ast/*` + `erp/doc-status` + `wf/approve-status` 字典（逐值核对）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议 218→237）。
