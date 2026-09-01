# projects 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.2b（plan `docs/plans/2026-09-01-1245-3-m12b-mnt-qa-prj-seed-expansion.md`，2026-09-01）。此前 6 张 projects 表 seed（project / project_type / project_pnl / budget / cost_collection / timesheet，plan `2026-08-31-2210-1`）见 `docs/architecture/seed-data.md` 历史批次段。
> **业务语义 owner docs**：`docs/design/projects/` 各域文档（profitability.md 结算语义等）；本文件只登记种子数据面，不重复业务语义。

## 种子数据范围（M1.2b 批次 11 表）

projects 域 17 规格实体中，6 表已由历史批次 seed；本批补齐其余 **11 规格表（26 行）**，达成 projects 域全量 seed 覆盖（17 / 17）。

### 11 规格表 CSV

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpPrjActivityType（活动类型） | `erp_prj_activity_type.csv` | 2 | P | —（无必填 FK）；SUBJECT_ID 可选→ErpMdSubject〔跨域:md·已seed 32=5101 项目成本〕 |
| ErpPrjBilling（项目账单） | `erp_prj_billing.csv` | 2 | **全批 CANCELLED（N-TERM，零漂移裁决 A′）** | PROJECT_ID→ErpPrjProject〔已seed 1〕、CUSTOMER_ID→ErpMdPartner〔跨域:md·已seed 1/2〕 |
| ErpPrjBillingLine（账单行） | `erp_prj_billing_line.csv` | 3 | P（2+1 按头） | BILLING_ID→ErpPrjBilling〔本批〕；行合计 12000+8000=20000 / 30000 与头表 TOTAL_AMOUNT 对账 |
| ErpPrjBudgetLine（预算行） | `erp_prj_budget_line.csv` | 3 | P | BUDGET_ID→ErpPrjBudget〔已seed 1〕；行合计 planned 30000+15000+5000=50000 与头表对账，committed/actual 30000 与项目 COMMITTED/ACTUAL_COST 对账 |
| ErpPrjCostCollectionLine（成本归集行） | `erp_prj_cost_collection_line.csv` | 2 | P | COST_COLLECTION_ID→ErpPrjCostCollection〔已seed 1〕；行合计 20000+10000=30000 与头表对账；SOURCE_BILL_TYPE/CODE 留空（手工归集语义，避免 C12 TIMESHEET 反查交集） |
| ErpPrjMilestone（里程碑） | `erp_prj_milestone.csv` | 3 | P×2+N-TERM(DONE) | PROJECT_ID→ErpPrjProject〔已seed 1〕；STATUS ∈ `erp-prj/task-status` |
| ErpPrjProjectSettlement（项目结算） | `erp_prj_project_settlement.csv` | 2 | P(INTERIM/DRAFT)+N-TERM(FINAL/CANCELLED) | PROJECT_ID→ErpPrjProject〔已seed 1〕、PNL_SNAPSHOT_ID→ErpPrjProjectPnl〔已seed 1〕、CUSTOMER_ID 可选→〔已seed 1〕；**INTERIM/CANCELLED 类型-状态组合 = 零漂移裁决 A**（见下节） |
| ErpPrjProjectSettlementLine（结算行） | `erp_prj_project_settlement_line.csv` | 2 | P（1+1 按头） | SETTLEMENT_ID→ErpPrjProjectSettlement〔本批〕；LINE_TYPE ∈ {INCOME, COST}（结算行类型常量）；来源软引用既有归集单 PRJ-CC-2026-001 / 同批账单 BL-2026-002 |
| ErpPrjProjectUser（项目成员） | `erp_prj_project_user.csv` | 2 | P | PROJECT_ID→ErpPrjProject〔已seed 1〕、USER_ID→ErpMdEmployee〔跨域:md·已seed 2/1〕 |
| ErpPrjRole（项目角色） | `erp_prj_role.csv` | 2 | P | —（无必填 FK） |
| ErpPrjTask（项目任务） | `erp_prj_task.csv` | 3 | P×2+N-TERM(DONE) | PROJECT_ID→ErpPrjProject〔已seed 1〕；PARENT_TASK_ID 可选自引用→ErpPrjTask〔本批 1〕、ASSIGNEE_ID 可选→ErpMdEmployee〔已seed 2/1〕 |

## FK 闭环图

```
[跨域:md·已seed] erp_md_employee(2 李四/1 张三)、erp_md_partner(1 华东科技/2 华南贸易)、erp_md_subject(32=5101 项目成本)
   ──USER_ID/ASSIGNEE_ID/CUSTOMER_ID/SUBJECT_ID──▶ 成员/任务/账单与结算/活动类型
[已seed·prj] erp_prj_project(1 PRJ-2026-001)、erp_prj_budget(1 PRJ-BD-2026-001)、erp_prj_cost_collection(1 PRJ-CC-2026-001)、
             erp_prj_project_pnl(1 PRJ-PNL-2026-001 CALCULATED 50000/30000/20000)
   ──PROJECT_ID/BUDGET_ID/COST_COLLECTION_ID/PNL_SNAPSHOT_ID──▶ 全部项目侧子表
[本批] erp_prj_billing(1..2) ──BILLING_ID──▶ billing_line（软引用：settlement_line 行 2 INCOME 来源）
[本批] erp_prj_project_settlement(1..2) ──SETTLEMENT_ID──▶ settlement_line
[本批] erp_prj_task(1..3) ──PARENT_TASK_ID（自引用：任务 2 → 任务 1）──▶ task
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空、白名单零增量（`TestErpSeedDataIntegrity` 全绿背书）。

## 干扰面零漂移设计裁决（本批核心约束，镜像 M1.2a2 双裁决范式）

- **裁决 A（结算类型-状态组合）**：`createSettlement` 守卫 `findActiveSettlementOfType(projectId, FINAL/CLOSE)` 拒绝同项目同类型未取消结算单（`docStatus != CANCELLED` 即命中）——C12 `TestErpC12PrjTimesheetSettlement` 对 seed 项目 1 依次创建 CLOSE + FINAL 结算。种子 2 行取 **INTERIM/DRAFT**（P 行，INTERIM 不在守卫类型集）+ **FINAL/CANCELLED**（N-TERM 行，CANCELLED 被守卫排除）→ 守卫双侧零触发。
- **裁决 A′（账单全批取消）**：`buildLines` 迭代 `findBillings(projectId)`（仅排除 `docStatus=CANCELLED`）为每张新结算单生成 INCOME 行——种子任何非取消账单都会为 C12 自建结算单追加 INCOME 行并使既有归集行 lineNo 移位（output-row 快照失配）→ **种子 billing 2 行全批 `DOC_STATUS=CANCELLED`**。规格表 P 指示由「取消终态 + 行合计对账」承载（反松弛登记于 plan Deferred But Adjudicated 注记，属零漂移设计裁决非范围裁剪）。
- **结算金额派生源不可触**：C12 结算金额断言（30000/50000/20000）派生自 seed PNL 快照行（`getProjectPnl` → `erp_prj_project_pnl`，不在本批 11 表）；种子结算行金额为静态演示值，不被任何断言消费。
- **归集行反查零交集**：C12 `findCollectionLine` 按 `sourceBillType=TIMESHEET + sourceBillCode=IT-C12-TS-001` 反查——种子归集行 SOURCE_BILL_TYPE/CODE 留空（手工归集语义）。
- **其余读取面核验**：prj 看板（getDashboardKpi/getProjectGrossMargin）只读 project/budget/cost_collection/project_pnl（既有 4 表）；`refreshPnl`（聚合 billing + cost_collection 头表）C12 显式不调用且不在任何集成用例路径；快照机制 `_chgType` 增量记录，显式种子 id < 100000 不入既有快照、不耗 default 序列。

## 与既有 6 表 seed 的衔接（语义一致性约束）

- **金额对账链**：budget_line planned 合计 50000 = 预算头 TOTAL_AMOUNT = 项目 BUDGET；committed/actual 合计 30000 = 项目 COMMITTED/ACTUAL_COST；cost_collection_line 合计 30000 = 归集头 TOTAL_AMOUNT（人工 20000 + 材料 10000，归集头备注「人工+材料」一致）；billing_line 合计与账单头两两对账。
- **结算行来源软引用**：结算行 1（COST）引用既有归集单 PRJ-CC-2026-001（非取消，语义合法）；结算行 2（INCOME）引用同批已取消账单 BL-2026-002（已取消结算引用已取消账单，语义一致）。
- **维度值复用既有值域**：组织 2；员工 2/1；往来单位 1/2；科目 32（5101 项目成本）；币种 1；全部静态日期落在 2026-06~2026-07 参考期（冻结时钟纪律；里程碑 plannedDate 最远 2026-12-20 为项目 END_DATE 内计划值）。

## 用例指示编码与 negative 行语义

- **P（最小正例行）**：全部 11 表均有。
- **N-TERM（终态行）**：milestone id=1 `DONE`（已完成里程碑终态）/ task id=3 `DONE`（任务完成终态）/ settlement id=2 `FINAL+CANCELLED`（结算取消终态，兼零漂移裁决 A 载体）/ billing id=1/2 `CANCELLED`（零漂移裁决 A′ 载体）。
- **N-DIS（禁用行）**：本批 projects 11 表无 enabled/isActive 类列，无 N-DIS 行。

## 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列；ISO 日期；小写布尔（IS_BILLING_TRIGGER）；ID < 100000；字典码 ∈ `erp-prj/*` + `erp/doc-status` + `wf/approve-status` 字典（逐值核对；结算行 LINE_TYPE 取 `ErpPrjConstants.SETTLEMENT_LINE_TYPE_*` 常量值域 INCOME/COST）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议 190→218），并保持本文件「干扰面零漂移设计裁决」两条件（结算类型-状态组合 / 账单全批取消）不被破坏。
