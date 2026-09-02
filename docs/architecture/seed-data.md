# 种子数据模块

> **资产类型**：本文描述的是**部署资产**（系统初始化/基础配置数据，随部署一次性导入），**非测试资产**。测试共享夹具见 `app-erp-test-data` 模块与 `testing-strategy.md §四类测试资产边界`。两者不可混淆。
>
> **当前状态**：部署期种子数据**已落地**（2026-07-08，plan `2026-07-08-1234-1`）——经平台 `DataInitInitializer` + `_vfs/_init-data/*.csv`（21 张核心主数据表），config-gated 由 `-Dnop.orm.init-database-data=true` + fresh-DB 重置触发（E2E/演示），生产 `application.yaml` 默认关闭。机制/列映射/门控见 `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md`。下方描述的独立 `app-erp-seed` 模块（版本化/增量导入/按租户账套）仍为**独立 follow-up**（未实现），用于更结构化的种子管理场景。
>
> **默认加载语义已切换（2026-08-31，plan `2026-08-31-1143-1-app-erp-all-default-seed-loading`）**——用户裁决（演示/沙盒场景）推翻 plan `2026-07-08-1234-1` 的「生产默认关」姿态：`app-erp-all/src/main/resources/application.yaml` **默认开启** `nop.orm.init-database-data: true`（位于 `init-database-schema: true` 后一行，所有 profile 共享）。**fresh-DB 重置成为运行前置条件**（`DataInitInitializer` 非幂等，重复启动主键冲突）；标准启动入口 = `scripts/start-app.sh`（含 `rm -f db/erp.mv.db` 重置 + `allow-create-default-user` + flux 渲染，演示语义）；生产真实部署应改用 MySQL/PostgreSQL + 数据集隔离 + 业务动作闸门配置，不应复用演示脚本。
>
> **交易单据种子（P2P+O2C）已落地**（2026-07-08，plan `2026-07-08-1445-1`）——在 21 张主数据 CSV 之上新增 **23 张交易单据 CSV**（共 44 张），覆盖采购到付款（PO→Receive→Invoice→Payment）+ 销售到收款（SO→Delivery→Invoice→Receipt）各 1 条端到端最小连通链，含对应**已过账财务产物**（凭证/凭证行/业财回链/AR-AP 辅助账/GL 余额/会计期间 OPEN）。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-08-1445-1-transaction-seed-table-map.md`。
>
> **运营域交易单据种子（库存/资产/项目）已落地**（2026-07-08，plan `2026-07-08-2210-1`）——在 44 张 CSV 之上新增 **13 张运营域表 CSV**（共 57 张），覆盖库存（stock_move+line/stock_balance/cost_layer）+ 资产（asset_category/asset/depreciation_schedule）+ 项目（project_type/project/cost_collection/timesheet/budget/project_pnl）三域最小连通集。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-08-2210-1-operational-domain-seed-table-map.md`。
>
> **制造域交易单据种子已落地**（2026-07-09，plan `2026-07-09-0930-1`）——在 57 张 CSV 之上新增 **4 张制造域表 CSV**（共 61 张）：work_order（4 行覆盖 IN_PROCESS/STOCK_PARTIAL/COMPLETED 三态）+ cost_variance（1 行）+ forecast（1 行 APPROVED）+ forecast_line（1 行）。使制造域看板 4 `@BizQuery`（getDashboardKpi/getWorkOrderStatusDistribution/getDashboardTrend/findDelayedWorkOrderAlert）+ 2 报表（production-variance/forecast-variance）数值转非空可观测。crp_load + crp-load 报表因 mandatory workcenterId FK + workcenter/calendar/capacity 配置链依赖归 Deferred。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-09-0930-1-manufacturing-seed-table-map.md`。
>
> **维护+质量域交易单据种子已落地**（2026-07-09，plan `2026-07-09-0930-2`）——在 61 张 CSV 之上新增 **11 张维护+质量域表 CSV**（共 72 张）：维护域 8 表（equipment_category/equipment/schedule/request/downtime_entry/visit/visit_task/spare_part_usage）+ 质量域 3 表（inspection/non_conformance/action）。使维护域看板 `getDashboardKpi`（equipmentTotal/runningCount/openRequestCount/periodVisitCount）+ 3 预警（findEquipmentDowntimeAlert/findMaintenanceOverdueAlert + 质量域 findCapaOverdueAlert）+ 2 报表（maintenance-history/downtime-summary）+ 质量域看板 `getDashboardKpi`（inspectionCount/passRate/rejectedCount/openNcrCount）+ 2 报表（inspection-summary/ncr-capa-summary）数值转非空可观测。SPC 三表因 spc_chart.parameterId 配置链依赖归 Deferred。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-09-0930-2-maintenance-quality-seed-table-map.md`。
>
> **CRM/客服/人力域交易单据种子已落地**（2026-07-09，plan `2026-07-09-1045-1`）——在 72 张 CSV 之上新增 **12 张 CRM/CS/HR 域表 CSV**（共 84 张）+ **2 处既有 CSV 加性追加**（erp_md_partner +1 行 EMPLOYEE 类型 / erp_fin_ar_ap_item +2 行 EMPLOYEE_ADVANCE/EXPENSE_CLAIM·OPEN）：CRM 5 表（stage/lead/forecast_period/forecast/forecast_line）+ CS 3 表（ticket_type/ticket/survey）+ HR 4 表（department/employee/salary_simulation/salary_simulation_item_adj）。使三域 **5 张报表**（CRM lead-conversion-funnel/forecast-accuracy、CS ticket-sla-csat-summary、HR payroll-simulation-comparison/employee-net-balance）数值转非空可观测。HR employee-net-balance 经跨域 finance/master-data 扩展（追加员工型 partner + ar_ap_item OPEN 行）驱动。三域为纯报表域（无看板 BizModel）。列映射/拓扑序/范围裁决见 `docs/analysis/2026-07-09-1045-1-crm-cs-hr-seed-table-map.md`。
>
> **质量域 SPC 种子已落地**（2026-07-09，plan `2026-07-09-1145-2`）——在 84 张 CSV 之上新增 **3 张质量域 SPC 表 CSV**（共 87 张）+ **1 处既有 CSV 加性追加**（erp_qa_non_conformance +1 行 sourceType=SPC·status=OPEN）：spc_chart（1 行，parameterId=0 占位软引用）+ spc_sample（1 行 isOutOfControl=true）+ spc_capability（1 行 capabilityLevel=INADEQUATE）。使质量看板 `getSpcOutOfControlWarning` 三计数器（outOfControlChartCount/inadequateCapabilityCount/openSpcNcrCount）由确定性 0 转非空可观测（解除 0930-2 Deferred「SPC 三表 seed」+ 0930-3「确定性 0」状态）。Strategy C 完整参照完整性（sample/capability.chartId 指向真实 chart 行）；SPC 引擎双层门控默认关，seed 静态结果行不被重算覆盖。列映射/拓扑序/范围裁决/期望值派生见 `docs/analysis/2026-07-09-1145-2-quality-spc-seed-table-map.md`。
>
> **制造域工作中心配置链 + crp_load 种子已落地**（2026-07-09，plan `2026-07-09-0628-1`）——在 87 张 CSV 之上新增 **4 张制造域工作中心配置链 + crp_load 表 CSV**（共 91 张）：workcenter（1 行 WC-001 主装配线）+ workcenter_calendar（1 行 单班 08:00~16:00 ALL_WEEK）+ workcenter_capacity（1 行 efficiencyFactor=1）+ crp_load（1 行 loadDate=2026-07-15 loadHours=4）。使 CRP 负荷报表（crp-load-report）经 `CrpLoadCalculator.getLoadReport` 由空转非空可观测（capacityHours=8.00 / loadRate=0.50 确定性派生），叠加 `mfg-crp-load.value.spec.ts` 数据驱动数值断言，完成全报表域数值断言覆盖里程碑（crp-load 为最后一个缺口）。解除 0930-1 Deferred「crp_load 表 + crp-load 报表 seed」。Strategy C 完整参照完整性（calendar/capacity/crp_load.workcenterId 指向真实 workcenter 行）；CRP 重算链经 nop-job 双层门控默认关，seed 静态 crp_load 行不被重算覆盖。列映射/拓扑序/范围裁决/期望值派生见 `docs/analysis/2026-07-09-0628-1-crp-load-seed-table-map.md`。
>
> **部署期序列推进修复已落地**（2026-07-09，plan `2026-07-09-0814-1`）——新增 `_vfs/_init-data/zz-sequence-advance.sql`（唯一 `.sql`，按文件名排序在所有 CSV 加载后执行）。经平台 `DataInitInitializer.executeSqlFiles()`（`jdbcTemplate.executeMultiSql` 原始 SQL）在种子 CSV 加载后 `MERGE INTO NOP_SYS_SEQUENCE ... KEY(SEQ_NAME)` 创建 default 序列行（`NEXT_VALUE=100000`，远超种子显式 id 上限 8）。**关键时序**：`DataInitInitializer` 是常规 bean（`@PostConstruct init()`，bean 启动期跑 CSV 加载 → SQL 执行）；`SysSequenceGenerator.lazyInit()` 经 `ioc:delay-method="lazyInit"`（`app-dao.beans.xml:11`）仅在**所有 bean 启动完成后**才运行——故 `.sql` 执行时 `nop_sys_sequence` 表为空（default 行尚未插入），必须 `MERGE`/`INSERT` 创建行（`UPDATE` 是 no-op 不可用）。随后 `addDefaultSequence()` 的 `if(!exists)` 守卫发现行已存在而跳过，advanced 值 `100000` 保留。效果：消除 GraphQL/AMIS 表单 create 首次主键碰撞（首 save id=100000），解除 0628-2 Deferred「AMIS 表单写路径」（触发条件即本修复）；写路径 helper 的 30 次 warm-up 重试简化为单次容错。不改 nop-entropy 平台代码，纯 app 层 `_init-data/*.sql` 解决。MERGE 满足全部 10 mandatory 列（SEQ_NAME/IS_UUID/NEXT_VALUE/STEP_SIZE/DEL_FLAG/VERSION/CREATED_BY/CREATE_TIME/UPDATED_BY/UPDATE_TIME）+ SEQ_TYPE/CACHE_SIZE。
>
> **通用引用完整性校验已落地**（2026-08-15，plan `2026-08-15-2000-1`）——新增通用测试类 `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`，对 `_init-data/` 全量种子建立**两层通用校验**（宿主 `TestAuthSeedLoadingProof` 初始化模式：BaseTestCase + 手动 `CoreInitialization.initialize()` + fresh-DB）：
>   1. **全表可加载**：枚举 `IDaoProvider.getEntityNames()`（**动态全量含平台实体，随 ORM 演进自动扩展**，含 `NopAuthUser`/`NopSysDict` 等；2026-09-01 快照 = app.erp.\* 363 + 已知声明缺 className 5 + 平台 66——计数口径权威登记处 = 下方「全量化裁决」段对账表，漂移 &gt; 0 时先更新对账表再消费），逐实体 `findAll()` 零异常；存在 seed CSV 的表行数 > 0（CSV 查找镜像 `DataInitInitializer.loadCsvData` 逻辑，97 CSV 全覆盖）。
>   2. **非空关联键指向合法数据**：逐实体经 `getEntityModel().getRelations()` 取全部 **to-one** 关系，逐行校验 join leftProp 非空值存在于 refEntity（主键 join 用 refEntity 主键集 Set 内存比对；非主键 join 按 refProp 值 `existsByQuery` 语义精确查询——实证当前全仓 0 命中）。
> - **覆盖范围与实证结论**：全量实体（2000-1 时点历史口径 418 = app.erp.\* 352 + 平台 66，当前动态口径见上方修正与「全量化裁决」段对账表）、全量 to-one 关系（app 1057 + 平台）、722 个非空 FK 值**零悬空引用**；`spc_chart.parameterId=0` 无 `<to-one>` 天然跳过；`crp_load.workOrderId=1`→`WO-2026-001`、`workcenterId=1`→`WC-001`、`erp_mnt_*.csv` equipmentId 跨域引用全部合法。白名单豁免机制（`WHITELIST_KEYS` 三元组）当前为空，供未来 seed 追加登记。
> - **门禁强化已落地**（2026-09-01，plan `2026-09-01-0527-1-m02-seed-gate-hardening`）——在两层通用校验之上新增 M0.2 裁决口径断言：scope-pinning（app.erp.* className 唯一计数 363 快照常量 + 已知声明缺 className 实体集 5 + sys_* 语义实体在集 + 平台实体语义保留，双向漂移显式失败） + seed 资产清单（零孤儿 CSV：`_init-data/` 每个 `.csv` ↔ 实体 tableName 精确匹配；app.erp.\*/平台 CSV 基线常量 93 + 4，M1.1a 批次起 app.erp.* 分项按随批更新协议推进至 314（M1.4a 批次，经 M1.1a/M1.1b/M1.1c/M1.2a1/M1.2a2/M1.2b/M1.2c/M1.3/M1.4a 各批随批推进），见下方对账表 M1.1a/M1.1b/M1.1c/M1.2a1/M1.2a2/M1.2b/M1.2c/M1.3/M1.4a 行）；快照重录双面义务前置评估 = 本计划零 seed CSV 变更，义务未触发（评估记录见该 plan 执行注记）。
> - **后续 seed 追加义务**：任何 `_init-data/*.csv` 新增/修改必须保持引用完整性（本测试为门禁）；确需弱指针/占位引用时在 `WHITELIST_KEYS` 登记并注明证据来源（seed CSV 注释 / 本文档注记 / bug 记录）。
> - **notify/cs 种子聚合已落地**（2026-08-25，plan `2026-08-25-0330-1`）——新增 **2 张部署配置表 CSV**（聚合前少 2 张，现共 **97 CSV**）：`erp_sys_notification_template.csv`（27 行模板族，转制自 `module-notify/deploy/sql/{三方言}/_seed_erp-notify.sql`，CSV 列头按既有约定省略审计列；三方言 diff 零差异）+ `nop_sys_code_rule.csv`（1 行 `cs-ticket-code` TK 编号规则，转制自 `module-cs/deploy/sql/{三方言}/_seed_erp-cs.sql`；String PK CSV 先例 = `nop_auth_role.csv`）。效果：聚合 app fresh-DB 启动后通知子系统与 CS TK 编号在无手工导入下即工作（修复 OA-01 产品级种子契约漂移——此前模板驱动通知静默丢弃 + TK 编号缺失回退）。联动：面 2 集成用例快照重录（C01/C04/C16/C17，见「快照重录义务」节）+ owner-doc 勘误修正（`integration-testing.md` §C16 勘误(4) 根因改「种子未聚合」）。
> - **执行期先决修复**（本计划 Phase 3 绿化必要，均已在 `docs/bugs/` 登记）：nop-entropy `OrmTransactionListener` NPE（`e5ee02b40` lazy-property 回归，null-guard 修复，双独立子代理批准见计划文件「Cross-Repo Fix Approvals」）；mfg `ErpMfgCostRollupLine.view.xml` 档位 cells `custom="true"`（E4.1 代理字段 cell-not-prop 回归，bug `2026-08-14-0930-mfg-...` 方案 A）。

## 目的

定义 nop-app-erp 的种子数据（基础配置数据）管理机制，使用独立模块 `app-erp-seed` 管理。

## 模块职责

- 管理系统初始化所需的基础数据
- 支持按租户/账套导入种子数据
- 种子数据版本管理

## 种子数据范围

| 数据类型 | 示例 |
|----------|------|
| 字典数据 | 状态枚举、作业类型、审批模式 |
| 主数据模板 | 科目表模板、仓库模板 |
| 系统配置 | 过账模式、审批流配置 |
| 示例数据 | 演示用物料/客户/供应商 |

## 模块结构

```
app-erp-seed/
    └── src/main/resources/
        ├── seed-data/
        │   ├── dict/          # 字典数据
        │   ├── master-data/   # 主数据模板
        │   └── config/        # 系统配置
        └── import.sql         # 初始化导入脚本
```

## 导入策略

- 首次部署时自动导入
- 升级时增量导入（新增数据）
- 不覆盖用户已修改的数据

## 快照重录义务（seed 变更联动，强制）

> 2026-08-25 登记（roadmap V.2，plan `docs/plans/2026-08-25-0232-1-v2-closure-alignment-docs-registration.md` Phase 1；roadmap 横切关注点 1「快照重录义务」的强制规则落点）。

部署期 seed 资产（`app-erp-all/src/main/resources/_vfs/_init-data/`，**97 CSV 基点 + M1.x 新增 CSV + 1 SQL**（`zz-sequence-advance.sql`；M1.1b 批次后 = 125 CSV + 1 SQL，M1.1c 批次后 = 137 CSV + 1 SQL，M1.2a1 批次后 = 168 CSV + 1 SQL，M1.2a2 批次后 = 194 CSV + 1 SQL，M1.2b 批次后 = 222 CSV + 1 SQL，M1.2c 批次后 = 241 CSV + 1 SQL，M1.3 批次后 = 273 CSV + 1 SQL，M1.4a 批次后 = 318 CSV + 1 SQL，M1.4b 批次后 = 333 CSV + 1 SQL，见对账表 M1.1a/M1.1b/M1.1c/M1.2a1/M1.2a2/M1.2b/M1.2c/M1.3/M1.4a/M1.4b 行），`DataInitInitializer` 拓扑序加载）是下述**双面测试快照的输入源**：三层全比对（response 快照 + DB 状态快照 + JUnit 关键断言）下，任何 seed CSV/SQL 变更（含未来计划追加种子）都会破坏受影响快照的录制口径。

### 双面资产盘点

- **面 1 — 既有各域测试快照**：各域 `module-<domain>/erp-*-service/_cases/`（19 域全在位，录制回放范式，391 测试类先例；快照种子 input/tables + 比对 output/response + output/tables）。
- **面 2 — app-erp-all 集成用例快照**：`app-erp-all/_cases/io/nop/app/all/it/`（22 用例类（C01-C21 含 C20a/C20b）+ 试点 `TestErpP2pPilot` 共 **23 类**；CHECKING 态 = 全量 97 seed 装载后三层全比对）。**2026-08-25 履行先例**：notify/cs 种子聚合（plan `2026-08-25-0330-1`）触发面 2 重录 C01/C04/C16/C17 四用例（新激活通知行落 `output/tables/erp_sys_notification.csv` + 全局序列 ID 漂移），面 1 零影响（域模块测试不声明 `nop.orm.init-database-data`）、E2E 零影响（notify-inbox spec 全相对断言自包含）。

### 义务规则（强制）

1. **触发条件**：任何部署期 seed 资产（97 CSV 基点 + M1.x 新增 CSV + `zz-sequence-advance.sql`；M1.1b 后 = 125 CSV + 1 SQL，M1.1c 后 = 137 CSV + 1 SQL，M1.2a1 后 = 168 CSV + 1 SQL，M1.2a2 后 = 194 CSV + 1 SQL，M1.2b 后 = 222 CSV + 1 SQL，M1.2c 后 = 241 CSV + 1 SQL，M1.3 后 = 273 CSV + 1 SQL，M1.4a 后 = 318 CSV + 1 SQL，M1.4b 后 = 333 CSV + 1 SQL）新增/修改/删除。
2. **重录义务（双面）**：变更方**同步重录受影响快照**——
   - 面 1：受影响各域 `_cases` 快照（force-save 重录；delVersion 列语义与响应快照 `*` 通配恢复口径见 e2e-runbook「JUnit 快照 delVersion 列语义」节）；
   - 面 2：`app-erp-all` 集成用例快照（基类 fresh-DB 重灌全量 seed 后按用例重录）。
3. **提交说明义务**：在变更提交说明中**登记重录范围**（哪些 seed 文件变更 → 双面中哪些用例/域快照重录；roadmap V.2 行文「含变更 PR 说明」口径）。
4. **E2E 数值断言联动**：同步评估既有 E2E 数值断言/期望值基线影响并同步（先例：SPC 追加 NCR 行曾联动更新 `quality.value.spec.ts` 的 openNcrCount）。

### 交叉引用

- `docs/backlog/integration-test-roadmap.md`：横切关注点 1（快照重录义务）/ 横切关注点 3（seed 修正授权 + E2E 数值断言期望值联动评估）/ 规则 6（seed 修正纪律）。
- `docs/testing/e2e-runbook.md`「集成测试」节 fresh-DB 纪律（seed 只追加不修改 + 部署期 seed 变更走修正授权流程——与本文节**双向互指**）。
- 同一资产的引用完整性门禁义务见上方「通用引用完整性校验」段「后续 seed 追加义务」（本文节为快照重录面，二者互补）。

## 模块 deploy 种子 ↔ 聚合 `_init-data` 种子同步义务（强制）

> 2026-08-25 立法（plan `2026-08-25-0330-1`，开放审计 OA-01 修复；此前 module-notify 27 模板行 + module-cs 1 编号规则行只存在于 deploy SQL、从未聚合进聚合 app，导致 fresh-DB 通知子系统静默失活 + CS TK 编号缺失——产品级种子契约漂移六天无人发现）。

**规则**：任何模块新增/修改 `deploy/sql/{mysql,oracle,postgresql}/_seed_*.sql`（模块级业务种子 deploy SQL）时，**必须**同步聚合进 `app-erp-all/src/main/resources/_vfs/_init-data/`（转制为 `<table>.csv`，列头对齐既有约定——省略审计列；`DataInitInitializer.loadCsvData` 按表名.csv + 列 CODE 装载、拓扑序自动排序、参与 `TestErpSeedDataIntegrity` 引用完整性门禁），**或**在本节显式登记 Non-Goal 裁决（含理由与触发条件）。

**登记处**：

| 模块 deploy 种子 | 聚合状态 | 裁决 |
| --- | --- | --- |
| `module-notify/deploy/sql/{三方言}/_seed_erp-notify.sql`（27 模板行） | ✅ 已聚合（2026-08-25，`erp_sys_notification_template.csv`） | 三方言 diff 零差异；CSV 省略审计列（对齐既有 97 CSV 约定） |
| `module-cs/deploy/sql/{三方言}/_seed_erp-cs.sql`（`cs-ticket-code` 1 行） | ✅ 已聚合（2026-08-25，`nop_sys_code_rule.csv`） | String PK CSV 先例 = `nop_auth_role.csv` |
| 其余模块 | — | 全仓无其他 `_seed_*.sql` deploy SQL（grep 实证）；新增时按本节规则同步聚合或登记 Non-Goal |

**联动义务**：聚合/修改聚合种子时，同步履行「快照重录义务」节（面 2 受影响用例重录 + 面 1/E2E 评估落盘）与「通用引用完整性校验」段「后续 seed 追加义务」（`TestErpSeedDataIntegrity` 门禁全绿），并在提交说明登记重录范围（义务规则 3）。

## 交易单据种子（P2P+O2C，已落地）

### 核心范式：源单据 + 下游财务产物「直 seed」

业财过账（凭证生成）是 **action 驱动**（BizModel 的 `@BizMutation` 动作触发），**原始 CSV 插入源单据不会自动产生下游凭证/辅助账/核销**。因此要 seed 一个**连贯的已过账端到端态**，必须**同时直 seed**：

1. 源单据头/行（PO/Receive/Invoice/Payment、SO/Delivery/Invoice/Receipt 各头+行）
2. 下游财务产物：`erp_fin_voucher` + `erp_fin_voucher_line`（借贷平衡）+ `erp_fin_voucher_bill_r`（凭证-单据反查）+ `erp_fin_ar_ap_item`（AR/AP 辅助账）+ `erp_fin_gl_balance`（期间科目余额）
3. 期间状态：`erp_fin_accounting_period` + `erp_fin_accounting_period_status`（当前期间 OPEN）

全部以一致 FK 串联，并引用 1234-1 已 seed 的主数据固定 ID（org/acctSchema/currency/partner/material/subject/...）。

### 加载拓扑序（跨域）

```
accounting_period → accounting_period_status
  → pur_order → pur_order_line → pur_receive → pur_receive_line
    → pur_invoice → pur_invoice_line → pur_payment → pur_payment_line
  → sal_order → sal_order_line → sal_delivery → sal_delivery_line
    → sal_invoice → sal_invoice_line → sal_receipt → sal_receipt_line
  → fin_voucher → fin_voucher_line → fin_voucher_bill_r
    → fin_ar_ap_item → fin_gl_balance
```

> `DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序，确保 FK 上游先于下游。

### posted 一致性裁决

`posted=true` **当且仅当**该源单据有对应凭证（经 `voucher_bill_r` 串联）：
- PO/SO（订单不直接过账 GL）、采购入库/销售出库（其过账产物是库存移动，属 inventory 域，未 seed 库存表）→ `posted=false`
- 采购发票/付款/销售发票/收款 → `posted=true`

### 已知简化

1234-1 seed 的科目表未含进/销项税科目，故凭证将税额并入相邻科目（AP 发票税额并入存货借方；AR 发票税额并入收入贷方），保证「凭证合计 = 发票价税合计」金额自洽且借贷平衡。精确税金科目分拆是主数据扩展 successor。

### Non-Goals（归后续批次）

- 扩展域交易单据（manufacturing/HR/quality/maintenance/CRM/CS/logistics/b2b/contract/drp/aps）——按域逐批补充（1234-1/1445-1 Deferred 既定策略）。**inventory/assets/projects 已于 2210-1 落地**；**manufacturing 已于 2026-07-09-0930-1 落地**；**maintenance/quality 已于 2026-07-09-0930-2 落地**；**CRM/CS/HR 已于 2026-07-09-1045-1 落地**（见下方「CRM/客服/人力域交易单据种子」段）。
- 运营域 GL 凭证/业财一体 seed（库存估值凭证/资产取得+折旧凭证/项目成本凭证）——三域看板读域表非 GL，且种子科目表无运营域专用科目；触发条件：运营域业财一体端到端数值回归需 GL 串联时。
- 退货链（采购/销售退货 + 红字凭证 + 反向辅助账）
- 核销单文档 `erp_fin_reconciliation`(+line)——本批 ar_ap_item 直表达 SETTLED 态，核销单文档归后续
- 精确 KPI/报表数值断言——归 `2026-07-08-1445-2` 数据驱动 successor（运营域数值断言由 `2026-07-08-2210-2` 承接）

## 运营域交易单据种子（库存/资产/项目，已落地）

### 核心范式：域表「直 seed」（区别于 P2P/O2C「源单据 + 下游财务产物直 seed」）

库存/资产/项目三域看板**读域表而非 GL 凭证**（经 `ErpInvDashboardBizModel`/`ErpAstDashboardBizModel`/`ErpPrjDashboardBizModel.getDashboardKpi` 核实）：

- **库存看板**：库存总值 = Σ `ErpInvStockBalance.totalCost`；本期出入库量 = Σ `ErpInvStockMove`（DONE 期内）关联 `ErpInvStockMoveLine`。
- **资产看板**：资产原值 = Σ `ErpAstAsset.originalValue`（IN_SERVICE）；累计折旧 = Σ `accumulatedDepreciation`；本期折旧 = Σ `ErpAstDepreciationSchedule.actualAmount`（EXECUTED 期内）。
- **项目看板**：在手项目数 = count `ErpPrjProject`（OPEN）；已发生成本 = Σ `ErpPrjCostCollection.totalAmount`；项目毛利率 = `ErpPrjProjectPnl` Σ grossProfit / Σ revenueAmount。

故 seed 域表（stock_balance/asset/depreciation_schedule/project/cost_collection/project_pnl）即令三域看板 KPI **非空**，**无需 seed GL 凭证**。这是运营域 seed 相对 P2P+O2C 的**复杂度减负**。

### 加载拓扑序（跨域）

```
[1234-1 主数据] → [上游域配置] ast_asset_category / prj_project_type
  → [域头] ast_asset / prj_project
    → [域行/计算产物]
      inv_stock_move → inv_stock_move_line
      inv_stock_balance / inv_cost_layer        （引用 material/warehouse，独立于 move）
      ast_depreciation_schedule                  （引用 asset）
      prj_cost_collection / prj_timesheet /
      prj_budget / prj_project_pnl               （引用 project）
```

### posted 一致性裁决（统一 posted=false）

本批所有运营域源单据/计算产物统一 `posted=false`。依据：
1. 三域看板读**域表**非 GL，`posted` 标志不被看板消费；
2. 1234-1 seed 的科目表无库存估值/资产/折旧费用/项目成本专用科目，seed GL 凭证徒增参照复杂度且不解除额外阻塞；
3. 运营域过账 → GL 凭证 seed 归后续（Deferred）。

### 域内金额自洽约束

seed 设计保持三组计算产物金额自洽（启动加载不校验，但 GraphQL 抽样/数值断言可观测）：
- `stock_balance.totalCost` ↔ `cost_layer.totalCost`（同物料+仓库对）
- `asset.accumulatedDepreciation`/`netBookValue` ↔ 最新 `depreciation_schedule` 同名字段
- `project_pnl.totalCost` ↔ Σ `cost_collection.totalAmount`（同项目）

### Non-Goals（归后续批次）

- 运营域 GL 凭证/业财一体 seed（库存估值凭证/资产取得+折旧凭证/项目成本凭证）——三域看板读域表非 GL；触发条件：运营域业财一体端到端数值回归需 GL 串联时。
- 其他扩展域交易种子（manufacturing/quality/maintenance/CRM/CS/HR/logistics/b2b/contract/drp/aps）——按域逐批补充（1445-1 Deferred 既定策略）。**manufacturing 已于 2026-07-09-0930-1 落地（见下方「制造域交易单据种子」段）**；**maintenance/quality 已于 2026-07-09-0930-2 落地（见下方「维护+质量域交易单据种子」段）**。
- 精确运营域 KPI/报表数值断言——本计划解除「运营域交易数据存在」阻塞（数值非零可观测）；精确断言由 `2026-07-08-2210-2` 承接。

## 制造域交易单据种子（已落地）

### 核心范式：域表「直 seed」（镜像运营域范式）

制造域看板/报表**读域表而非 GL 凭证**（经 `ErpMfgDashboardBizModel`/`ErpMfgReportBizModel` 核实）：

- **制造看板**（`ErpMfgDashboardBizModel`，4 `@BizQuery` 均查 `ErpMfgWorkOrder`）：在制工单数 = count(docStatus IN [IN_PROCESS, STOCK_RESERVED])；本期完工量 = Σ completedQuantity（COMPLETED 期内 actualEndDate）；齐套待产 = count(STOCK_PARTIAL)；工单准时率 = count(COMPLETED 且 actualEndDate ≤ plannedEndDate) / count(COMPLETED)。
- **生产差异报表**（`buildProductionVarianceDataset`）：读 `ErpMfgCostVariance`（workOrderId FK→work_order）。
- **预测差异报表**（`buildForecastVarianceDataset`）：读 `ErpMfgForecast`(APPROVED) + `ErpMfgForecastLine` + `ErpMfgWorkOrder`(COMPLETED 实际量，按 productId 聚合、区间重叠用 plannedStartDate/plannedEndDate)。

故 seed 4 表（work_order/cost_variance/forecast/forecast_line）即令看板 + 2 报表 KPI **非空**，**无需 seed GL 凭证**（与运营域范式一致，复杂度减负）。

### 加载拓扑序（跨域）

```
[1234-1 主数据] → [域头] erp_mfg_work_order / erp_mfg_forecast
                    → [域行/计算产物] erp_mfg_cost_variance（workOrderId→work_order）
                                    / erp_mfg_forecast_line（forecastId→forecast）
```

### posted 一致性裁决（统一 posted=false）

本批所有制造域源单据/计算产物统一 `posted=false`。依据（镜像运营域裁决）：
1. 看板/报表读**域表**非 GL，`posted` 标志不被消费；
2. 1234-1 seed 的科目表无制造费用/差异/在产品专用科目，seed GL 凭证徒增参照复杂度；
3. 制造域过账 → GL 凭证 seed 归后续（Deferred）。

### crp_load 移出范围（Deferred → 已于 0628-1 落地）

`erp_mfg_crp_load.workcenterId` 是 mandatory FK→ErpMfgWorkcenter，且 crp-load 报表经 `CrpLoadCalculator` 依赖 workcenter/workcenter_calendar/workcenter_capacity 配置链（均未 seed）算 capacityHours/loadRate。seed crp_load 需先 seed 整条配置链，超出「域表直 seed」范式。0930-1 据此将 crp_load + crp-load 报表移出范围（Deferred，触发条件「workcenter 配置链 seed 落地后」）。**此阻塞已于 `2026-07-09-0628-1` 解除**：seed 完整工作中心配置链（workcenter 1 行 + workcenter_calendar 1 行 单班 08:00~16:00 ALL_WEEK + workcenter_capacity 1 行 efficiencyFactor=1）+ crp_load 1 行（loadDate=2026-07-15 loadHours=4），使 crp-load 报表经 `CrpLoadCalculator.getLoadReport` 由空转非空可观测（capacityHours=8.00 / loadRate=0.50 确定性派生），叠加 `mfg-crp-load.value.spec.ts` 数据驱动数值断言。详见下方「制造域工作中心配置链 + crp_load 种子」段。

### 域内金额自洽约束

- `cost_variance.standardAmount` ↔ `work_order.materialCost`（同工单材料成本标准）
- `cost_variance`：`varianceAmount = actualAmount − standardAmount`、`variancePercent = varianceAmount / standardAmount`（MATERIAL_USAGE：standardPrice = actualPrice，差异纯由用量差驱动）
- `forecast_line.materialId` 对齐 `work_order.productId`（forecast-vs-actual 对比有意义）

### Non-Goals（归后续批次）

- 制造域 GL 凭证/业财一体 seed（制造费用/差异过账凭证）——看板/报表读域表非 GL，种子科目表无制造域专用科目；触发条件：制造域业财一体端到端数值回归需 GL 串联时。
- ~~crp_load + crp-load 报表 seed——mandatory workcenterId FK + workcenter/calendar/capacity 配置链依赖；触发条件：workcenter 配置链 seed 落地后。~~ **已于 2026-07-09-0628-1 落地**（见下方「制造域工作中心配置链 + crp_load 种子」段；workcenter 配置链 seed 后 crp_load + crp-load 报表 seed 阻塞解除）。
- 制造域配置/执行链 seed（BOM/Routing/Workcenter/MRP/JobCard/MaterialIssue/Subcontract/CostRollup/BatchGenealogy/work_order_line）——这些表不被看板/报表 `QueryBean` 直接读，work_order.bomId/routingId 非强制可留 null；触发条件：制造域配置/执行链端到端回归需这些数据时。
- 精确制造域 KPI/报表数值断言——本计划解除「制造域交易数据存在」阻塞（数值非零可观测）；精确断言由 `2026-07-09-0930-3` 承接。
- 其他扩展域交易种子（maintenance/quality 同批 N=2；CRM/CS/HR/logistics/b2b/contract/drp/aps 后续批次）——1445-1 Deferred 既定策略。**maintenance/quality 已于 2026-07-09-0930-2 落地（见下方「维护+质量域交易单据种子」段）**；**CRM/CS/HR 已于 2026-07-09-1045-1 落地（见下方「CRM/客服/人力域交易单据种子」段）**。

## 维护+质量域交易单据种子（已落地）

### 核心范式：域表「直 seed」（镜像运营域/制造域范式）

维护/质量域看板/报表**读域表而非 GL 凭证**（经 `ErpMntDashboardBizModel`/`ErpQaDashboardBizModel`/`ErpMntReportBizModel`/`ErpQaReportBizModel` 核实，零 GL/Voucher 引用）：

- **维护看板**（`ErpMntDashboardBizModel`）：设备总数 = count `ErpMntEquipment`（status≠DECOMMISSIONED）；运行中 = count(RUNNING)；待处理请求 = count `ErpMntRequest`(OPEN)；本期维护访问 = count `ErpMntVisit`(COMPLETED + businessDate 区间)。
- **维护预警**：`findEquipmentDowntimeAlert`（equipment DOWN + `ErpMntDowntimeEntry` endTime=null）；`findMaintenanceOverdueAlert`（`ErpMntSchedule` isActive=1 + nextDueDate<today + 无 visit 关联）。
- **维护报表**：maintenance-history 读 `ErpMntVisit`（visitDate 区间 + taskCount via visit_task + sparePartUsageCount via spare_part_usage）；downtime-summary 读 `ErpMntDowntimeEntry`（startTime 区间，按设备/原因聚合 totalMinutes）。
- **质量看板**（`ErpQaDashboardBizModel`）：本期质检数 = count `ErpQaInspection`（inspectionDate 区间）；合格率 = ACCEPTED/total；不合格数 = count(REJECTED)；开放 NCR = count `ErpQaNonConformance`(status IN [OPEN,IN_REVIEW])。
- **质量预警/报表**：`findCapaOverdueAlert`（`ErpQaAction` status≠COMPLETED + dueDate<today）；inspection-summary 读 inspection（按 materialId 聚合）；ncr-capa-summary 读 non_conformance（ncrDate 区间 + action 计数 by ncrId）。

故 seed 域表（equipment/visit/inspection/non_conformance 等）即令两域看板 KPI + 4 报表 + 3 预警**非空**，**无需 seed GL 凭证**（与运营域/制造域范式一致，复杂度减负）。

### 加载拓扑序（跨域）

```
[1234-1/2210-1 主数据] md_organization/md_material/md_uom/md_warehouse/md_employee/md_partner
  /ast_asset(AST-2026-002，mnt equipment.assetId 跨域可选复用)
  → [维护域配置] mnt_equipment_category
    → [维护域头] mnt_equipment
      → [维护域单据/记录]
        mnt_schedule / mnt_request / mnt_downtime_entry / mnt_visit
          → mnt_visit_task / mnt_spare_part_usage
  → [质量域单据] qa_inspection → qa_non_conformance → qa_action
```

### posted 一致性裁决（统一 posted=false）

本批所有维护/质量域源单据统一 `posted=false`。依据（镜像运营域/制造域裁决）：
1. 两域看板/报表读**域表**非 GL，`posted` 标志不被消费；
2. 1234-1 seed 的科目表无维护费用/备件消耗/质量损失/报废处置专用科目，seed GL 凭证徒增参照复杂度；
3. 两域过账 → GL 凭证 seed 归后续（Deferred）。

### SPC 三表移出范围（Deferred → 已于 1145-2 落地）

`getSpcOutOfControlWarning` 读 `ErpQaSpcSample`（isOutOfControl）+ `ErpQaSpcCapability`（capabilityLevel=INADEQUATE），二者 chartId mandatory FK→`ErpQaSpcChart`。0930-2 阶段 spc_chart.parameterId 是 mandatory BIGINT 但 quality ORM 无独立 ErpQaParameter 实体（检验参数仅以 inspection_template_line.parameterName 自由文本存在），曾据此将 SPC 三表移出范围（Deferred）。**此阻塞已于 `2026-07-09-1145-2` 解除**：经核实 parameterId 为自由 BIGINT 软引用（ORM 无 `<to-one>` 无目标实体，可填占位值 0），SPC 引擎双层门控默认关闭（seed 静态结果行安全），采用 Strategy C 完整参照完整性 seed spc_chart(1) + spc_sample(1 isOutOfControl=true) + spc_capability(1 INADEQUATE) + non_conformance 追加 SPC 行，使该预警三计数器由 0 转非空。详见下方「质量域 SPC 种子」段。

### 域内金额/计数自洽约束

- maintenance-history 报表：visit 的 taskCount（visit_task.visitId 计数）+ sparePartUsageCount（spare_part_usage.visitId 计数）自洽。
- downtime-summary 报表：downtime_entry 已恢复行（endTime≠null）的 totalMinutes 聚合自洽（ongoing 行 endTime=null + totalMinutes=null 不计入聚合）。
- ncr-capa 报表：ncr 的 capaActionCount（action.ncrId 计数）+ completedActionCount（action.status=COMPLETED 计数）自洽。
- inspection-summary 报表：inspection 按 materialId 聚合 totalInspections/acceptedCount(ACCEPTED+CONDITIONAL)/rejectedCount(REJECTED) 自洽。

### Non-Goals（归后续批次）

- 维护/质量域 GL 凭证/业财一体 seed（维护费用/备件消耗/质量损失/报废处置过账凭证）——看板/报表读域表非 GL，种子科目表无两域专用科目；触发条件：维护/质量域业财一体端到端数值回归需 GL 串联时。
- ~~维护域 calibration / 质量域 risk_register/quality_goal/review/calibration/recall(+target)/sampling_plan/inspection_template(+line) seed~~ **已于 M1.2b 批次（plan `2026-09-01-1245-3`，2026-09-01）全量落地**（按 270 实体规格表 maintenance/quality 两节逐行消费；触发条件由 roadmap M1 全量覆盖目标取代）。
- ~~质量域 SPC 三表 seed（spc_chart/spc_sample/spc_capability）——spc_chart.parameterId 配置链依赖；触发条件：SPC 配置链 seed 落地后（见上方「SPC 三表移出范围」）。~~ **已于 2026-07-09-1145-2 落地**（见下方「质量域 SPC 种子」段；parameterId 为自由 BIGINT 软引用，占位值 0 即可加载，阻塞解除）。
- ~~备件消耗行 `erp_mnt_spare_part_usage_line` seed~~ **已于 M1.2b 批次（plan `2026-09-01-1245-3`，2026-09-01）落地**（2 行挂 seed usage id=1，行合计 170.00 与头表 TOTAL_AMOUNT 对账；UoM 列名 `UO_M_ID` 分叉拼写陷阱在列头显式落实；触发条件由 roadmap M1 全量覆盖目标取代）。
- 精确维护/质量域 KPI/报表数值断言——本计划解除「维护/质量域交易数据存在」阻塞（数值非零可观测）；精确断言由 `2026-07-09-0930-3` 承接。
- 其他扩展域交易种子（CRM/CS/HR 已于 2026-07-09-1045-1 落地，见下方「CRM/客服/人力域交易单据种子」段；logistics/b2b/contract/drp/aps 后续批次）——1445-1 Deferred 既定策略。

## CRM/客服/人力域交易单据种子（已落地）

### 核心范式：域表「直 seed」+ 跨域加性追加（镜像运营/制造/维护+质量域范式）

CRM/CS/HR 三域为**纯报表域（无看板 BizModel）**，各 1 个 `ErpXxxReportBizModel` 共 5 张报表，**读域表而非 GL 凭证**（逐方法 `findAll`/`findAllByQuery` 核实）：

- **CRM 线索转化漏斗**（`buildLeadConversionFunnelDataset`）：读 `ErpCrmLead`（按 stageId 非 null 聚合 leadCount/expectedRevenue）+ `ErpCrmStage`（解析 stageName）。
- **CRM 销售预测准确率**（`buildForecastAccuracyDataset`）：读 `ErpCrmForecast`（periodId mandatory FK→forecast_period）+ `ErpCrmForecastLine`（forecastId+leadId mandatory FK，按 forecastId 聚合 lineCount/lineWeightedRevenue）。
- **CS 工单 SLA/CSAT**（`buildTicketSlaCsatSummaryDataset`）：读 `ErpCsTicket`（ticketTypeId mandatory FK、isSlaCompleted 布尔列内存派生）+ `ErpCsSurvey`（ticketId mandatory FK，csatScore/npsScore 经 `orm_propValueByName` 读取）+ `ErpCsTicketType`（解析 ticketTypeName）。
- **HR 薪酬模拟对比**（`buildPayrollSimulationComparisonDataset`）：simulationId 为强制入参；读 `ErpHrSalarySimulationItemAdjustment`（simulationId+employeeId mandatory FK，employee.departmentId 驱动 DEPT_SUBTOTAL 小计行）+ `ErpHrEmployee`。
- **HR 员工净余额**（`buildEmployeeNetBalanceDataset`）：**唯一跨域读取**——经注入 biz `IErpFinArApItemBiz.findOpenItems(direction)`（过滤 direction + status IN [OPEN,PARTIAL]）读 finance `erp_fin_ar_ap_item`，再内存按 sourceBillType 二次过滤（预支余额=RECEIVABLE+EMPLOYEE_ADVANCE、报销余额=PAYABLE+EXPENSE_CLAIM），按 partnerId 汇总 openAmountFunctional；再 `findAllByQuery` 读 `erp_md_partner` 解析姓名。

故 seed 域表 + HR 跨域 finance/master-data 加性追加即令 5 报表 KPI **非空**，**无需 seed GL 凭证**。

### 加载拓扑序（跨域）

```
[1234-1 主数据(已 seed)] md_organization(2) / md_currency(1) / md_partner(1-4)
  → [本批跨域追加] md_partner +1 行(id=5 EMPLOYEE 类型)              ← 早于 fin_ar_ap_item 追加行
[CRM 域] stage → lead(stageId) ; forecast_period → forecast(periodId) → forecast_line(forecastId+leadId)
[CS 域]  ticket_type → ticket(ticketTypeId+customerId) → survey(ticketId)
[HR 域]  department → employee(departmentId) ; salary_simulation → simulation_item_adj(simulationId+employeeId)
[HR 跨域 finance 扩展] erp_fin_ar_ap_item +2 行(partnerId=5；引用 1234-1 已 seed org=2/acctSchema=1/currency=1/period=1)
```

> `md_partner` 追加行（id=5）属 1234-1 主数据批，先于 finance `fin_ar_ap_item`（1445-1 批）加载，FK 天然满足。

### posted 一致性裁决（统一无 posted 列）

本批 CRM/CS/HR 新增域表 + 跨域追加表（fin_ar_ap_item / md_partner）实体**本身均无 `posted` 列**（逐表 ORM 核实），CSV 不含 posted。依据（镜像前序批次裁决）：
1. 三域 5 报表读域表/状态列非 posted（lead.stageId / forecast·forecast_line / ticket.isSlaCompleted / ar_ap_item.status·sourceBillType·direction / simulation_item_adj），`posted` 非任何报表过滤列；
2. 1234-1 seed 的科目表无 CRM/CS/HR 域专用科目，seed GL 凭证徒增参照复杂度；
3. 三域过账 → GL 凭证 seed 归后续（Deferred）。

### HR 跨域 finance/master-data 扩展裁决（方案 A）

`buildEmployeeNetBalanceDataset` 需 `erp_fin_ar_ap_item` 含 EMPLOYEE_ADVANCE(RECEIVABLE)/EXPENSE_CLAIM(PAYABLE)+status=OPEN 行。选择**加性追加**（方案 A）：`erp_md_partner` 追加 1 行 PARTNER_TYPE=EMPLOYEE（对齐 `docs/design/finance/expense-claim.md` 员工-as-partner 设计）+ `erp_fin_ar_ap_item` 追加 2 行 OPEN（partnerId 指向员工型 partner）。

**对既有 finance 报表/看板无回归核证**：(1) finance 看板 `getDashboardKpi` 的 revenue/netProfit/expense 读 GL，arBalance/apBalance 虽读 ar_ap_item open 但非 `finance.value.spec.ts` 断言字段；(2) `fin-ar-ap-aging.value.spec.ts` 仅断言报表标题 + 合计行标签 token 存在，不断言具体数值；(3) `findOpenItems` 仅取 status IN [OPEN,PARTIAL]，既有 4 行全 SETTLED 不被取。E2E 全套 74 spec 0 回归实证。

### 域内金额/计数自洽约束

- CRM funnel：lead 按 stageId 聚合 leadCount + ΣexpectedRevenue（每 stage ≥1 行）。
- CRM forecast-accuracy：forecast_line.weightedRevenue = expectedRevenue × probability/100（行自洽）；forecast.commitAmount/lineCount/lineWeightedRevenue 跨表可观测。
- CS ticket-sla-csat：ticket 按 ticketTypeId 聚合 totalTickets/slaCompleted(isSlaCompleted=true)/slaBreached(false)；survey 按 ticketId 摊回 ticketType 桶驱动 avgCsat/avgNps。
- HR payroll-sim：difference = adjustedAmount − originalAmount；DEPT_SUBTOTAL = Σ difference by departmentId。
- HR employee-net-balance：OPEN 态 openAmountFunctional = amount（全额未核销）；netBalance = advanceBalance(ΣRECEIVABLE·EMPLOYEE_ADVANCE) − expenseBalance(ΣPAYABLE·EXPENSE_CLAIM)。

### Non-Goals（归后续批次）

- CRM/CS/HR 域 GL 凭证/业财一体 seed（凭证↔源单据↔辅助账串联）——三域报表读域表/ar_ap_item 状态列非 GL；HR ar_ap_item 追加行作为可观测独立行（无凭证回链）；触发条件：三域业财一体端到端数值回归需 GL 串联时。
- CRM/CS/HR 域配置/执行链 seed（CRM product_config_rule/price_rule/bundle_pricing/territory/team/campaign；CS knowledge_base/sla_policy/entitlement/catalog；HR salary/salary_item/leave/attendance/shift/competency/social_insurance）——这些表不被范围内 5 报表 `QueryBean` 直接读（lead/ticket/simulation 的配置 FK 非强制可留 null）；触发条件：对应域配置/执行链端到端回归需这些数据时。**HR 子集消费登记（2026-09-02）**：HR 配置/执行链子集（salary / salary_item / leave / attendance / shift / competency / social_insurance 族）已由 M1.3 批次（plan `2026-09-01-2255-2`）按 roadmap M1 全量覆盖口径全量消费落地。**CRM/CS 子集消费登记（2026-09-02）**：CRM 子集（config_rule / price_rule / bundle_pricing / territory / team / campaign）+ CS 子集（knowledge_base / sla_policy / entitlement / catalog 族 = catalog_category / service_catalog_item / catalog_fulfillment）已由 M1.4a 批次（plan `2026-09-01-2255-3`）按 roadmap M1 全量覆盖口径全量消费落地；GL 凭证/业财一体子集（上条）维持原 Deferred。
- 精确 CRM/CS/HR 域报表数值断言——本计划解除「数据存在」阻塞（报表非空可观测）；精确断言由 `2026-07-09-1045-2-crm-cs-hr-report-value-assertions.md` 承接。
- 其他扩展域交易种子（logistics/b2b/contract/drp/aps 后续批次）——无看板无报表（seed 不解除额外阻塞）；触发条件：对应域端到端数值回归需交易数据时。**aps/logistics 子集消费登记（2026-09-02）**：aps 子集（operation_order / schedule / constraint / op_routing / dispatch_rule / dispatch_log / capacity_reservation）+ logistics 子集（carrier / carrier_config / shipment / shipment_line / shipment_log / shipment_parcel / delivery_window / delivery_booking）已由 M1.4b 批次（plan `2026-09-02-1415-1-m14b-aps-logistics-seed-expansion`）按 roadmap M1 全量覆盖口径全量消费落地（owner doc：`docs/design/aps/seed-data.md` + `docs/design/logistics/seed-data.md`）；b2b/contract/drp 子集维持原 Deferred（归 M1.5）。

## 质量域 SPC 种子（已落地）

### 核心范式：域表「直 seed」+ 完整参照完整性（Strategy C）

承接 0930-2 Deferred「SPC 三表 seed」。质量看板 `getSpcOutOfControlWarning` 三计数器**读 SPC 域表而非 GL 凭证**（经 `ErpQaDashboardBizModel.getSpcOutOfControlWarning` + 3 helper 核实）：

- **outOfControlChartCount**：distinct `ErpQaSpcSample.chartId` where `isOutOfControl=true`。
- **inadequateCapabilityCount**：distinct `ErpQaSpcCapability.chartId` where `capabilityLevel=INADEQUATE`（config-gated `erp-dash.qa-spc-include-inadequate`，默认 true）。
- **openSpcNcrCount**：count `ErpQaNonConformance` where `sourceType=SPC` AND `status IN [OPEN, IN_REVIEW]`（config-gated `erp-dash.qa-spc-include-ncr`，默认 true）。

三 helper 仅迭代 sample/capability/non_conformance 表收集 distinct `chartId` 或行数，**从不 join 或 load `erp_qa_spc_chart`**（Nop ORM `<to-one>` 为逻辑 join 非 DB 物理外键）。故 seed 域表即令该预警三计数器**非空**，**无需 seed GL 凭证**（与运营域/制造域/维护+质量域范式一致，复杂度减负）。

### Strategy C 完整参照完整性裁决（vs Strategy B）

- **Strategy C（selected）**：seed spc_chart（1 行，parameterId=0 占位）+ spc_sample（1 行 isOutOfControl=true，chartId 引用已 seed chart）+ spc_capability（1 行 INADEQUATE，chartId 引用同 chart）+ non_conformance 追加 SPC 行。sample/capability.chartId 指向真实 chart 行（完整参照），与 0930-2 范式一致。
- **Strategy B（rejected）**：仅 sample+capability 不建 chart，chartId 悬空指向不存在的行（种子数据完整性差，虽看板不读 chart）。

### parameterId=0 占位软引用文档化

`spc_chart.parameterId` 是 mandatory BIGINT **但 ORM 无 `<to-one>` 无目标实体**（仓库无 `ErpQaParameter`/`ErpQaInspectionParameter`，检验参数仅以 `inspection_template_line.parameterName` 自由文本存在）。本批 `parameterId=0` 为占位软引用：
- 加载层：BIGINT 列接受任意值（无 FK 约束 / 无 dict 校验），0 安全。
- 看板层：`getSpcOutOfControlWarning` 三 helper 不读 chart 表，0 不影响预警计数。
- 物化 `ErpQaParameter` 实体属 schema 扩展（ask-first），超出 seed 范畴 → Deferred（触发条件：SPC 控制图需绑定真实检验参数维度时）。

### SPC 引擎重算覆盖防护

SPC 引擎双层门控默认关闭：`erp-qa.spc-enabled` 默认 `false`（`ErpQaConfigs.isSpcEnabled`）+ `ErpQaSpcSamplingJob`/`ErpQaSpcCapabilityJob` cron 表达式默认空（`getSpcSamplingCron`/`getSpcCapabilityCron`）。fresh-DB 启动（`init-database-data=true`）不触发重算，seed 静态结果行（isOutOfControl=true / capabilityLevel=INADEQUATE）安全不被覆盖。SPC 引擎重算链端到端回归属 Deferred。

### 加载拓扑序（跨域）

```
[1234-1/2210-1 主数据(已 seed)] md_organization(2) / md_material(1) / md_employee(1)
  → [SPC 配置头] qa_spc_chart                          （orgId/materialId 逻辑 to-one，非强制）
      → [SPC 结果行]
        qa_spc_sample                                   （chartId mandatory 逻辑 to-one→chart）
        qa_spc_capability                               （chartId mandatory 逻辑 to-one→chart）
  → [既有 quality 单据加性追加] qa_non_conformance +1 行 （materialId FK→md_material=1；inspectionId 留空）
```

### posted 一致性裁决（三表无 posted 列；non_conformance 追加行 posted=false）

镜像 0930-2 裁决。SPC 三表（spc_chart/spc_sample/spc_capability）ORM 均**无 `posted` 列**（逐表 ORM 核实），看板读域表非 GL，CSV 不含 posted；non_conformance 追加行 `posted=false`（镜像 0930-2 既有 2 行）。

### getDashboardKpi.openNcrCount 联动裁决

`getDashboardKpi.openNcrCount` 经 `countOpenNcrs` 计数**所有** NCR status IN [OPEN, IN_REVIEW]（不按 sourceType 过滤）。追加 SPC NCR（id=3, status=OPEN）→ openNcrCount 由 2 → 3。须同步更新 `quality.value.spec.ts` 的 getDashboardKpi 断言（已落），其余 getDashboardKpi 字段（inspectionCount/passRate/rejectedCount）不受影响（本批不 seed inspection）。

### 域内计数自洽约束（期望值派生）

config 门控两段默认 true（`ErpQaConfigs.isDashQaSpcIncludeInadequate`/`isDashQaSpcIncludeNcr`）→ 三计数器全计入：

- `outOfControlChartCount=1`：spc_sample 1 行（chartId=1, isOutOfControl=true）→ distinct chartId={1} → size=1。
- `inadequateCapabilityCount=1`：spc_capability 1 行（chartId=1, capabilityLevel=INADEQUATE）→ distinct chartId={1} → size=1。
- `openSpcNcrCount=1`：non_conformance 追加 1 行（id=3, sourceType=SPC, status=OPEN）→ count=1。

### Non-Goals（归后续批次）

- 质量域 SPC GL 凭证/业财一体 seed——SPC 预警读域表非 GL，种子科目表无 SPC 专用科目；触发条件：SPC 业财一体端到端数值回归需 GL 串联时。
- SPC 控制图完整可视化（echarts UCL/LCL + 违规点高亮）——0930-3 既定 Deferred（前端可视化面）；触发条件：前端 SPC 控制图可视化需求时。
- ErpQaParameter 实体物化 / 检验参数 seed——parameterId 为自由 BIGINT 软引用，占位值 0 即可；触发条件：SPC 控制图需绑定真实检验参数维度时。
- SPC 引擎重算链 seed / rule engine 触发——本批 seed 静态结果行令看板可观测，不触发 SpcRuleEngine/SpcControlLimitCalculator/SpcCapabilityCalculator 重算；触发条件：需验证 SPC 引擎端到端计算正确性时。
- 质量域其他配置表 seed（risk_register/quality_goal/review/calibration/recall/sampling_plan/inspection_template）——0930-2 既定 Deferred，触发条件不变。

## 制造域工作中心配置链 + crp_load 种子（已落地）

### 核心范式：域表「直 seed」+ 完整参照完整性（Strategy C，镜像 SPC 范式）

承接 0930-1 Deferred「crp_load 表 + crp-load 报表 seed」。CRP 负荷报表（crp-load-report）**读 crp_load + workcenter 配置链而非 GL 凭证**（经 `ErpMfgReportBizModel.buildCrpLoadDataset` → `CrpLoadCalculator.getLoadReport` 核实）：

- **resolveReportWorkcenters**：workcenterIds（若提供）否则区间内有 CrpLoad 行或有 Calendar 的工作中心。
- **indexLoads**：聚合 `erp_mfg_crp_load` 行 → workcenter×date 累加 loadHours/setupHours（驱动 loadHours）。
- **efficiencyByWorkcenter**：读 `erp_mfg_workcenter_capacity.efficiencyFactor`（isActive=true，缺省回退 1）。
- **calendarsByWorkcenter**：读 `erp_mfg_workcenter_calendar`（isActive=true）→ `availableHours(calendars, date)` = Σ shiftHours(startTime,endTime)（受 effectiveFrom/effectiveTo 边界 + workDatePattern 周几模式过滤）。
- **capacityHours** = availableHours × efficiency；**loadRate** = loadHours / capacityHours；**overloaded** = loadRate > erp-mfg.crp-overload-threshold（默认 1.0）。

**关键**：报表非空需 crp_load 行（驱动 loadHours）+ workcenter（驱动 code）+ workcenter_calendar（驱动 capacityHours）。workcenter_capacity 提供 efficiencyFactor（缺省 1 仍可工作，但 seed 一行保证参照完整）。故 seed 4 表完整配置链（镜像 1145-2 Strategy C），**无需 seed GL 凭证**。

### Strategy C 完整参照完整性裁决

- **Strategy C（selected）**：seed workcenter（1 行）+ workcenter_calendar（1 行 IS_ACTIVE=true）+ workcenter_capacity（1 行 EFFICIENCY_FACTOR=1）+ crp_load（1 行 LOAD_HOURS=4），以一致 workcenterId=1 串联；calendar/capacity/crp_load.workcenterId 指向真实 workcenter 行（完整参照）。
- **Strategy B（rejected）**：仅 seed workcenter + crp_load（不 seed calendar/capacity）——`getLoadReport` 需 calendar 算 capacityHours（缺 calendar 则 availableHours=0 → capacityHours=0 → loadRate 除零兜底 9999），报表数值不可观测/不稳定。

### capacityHours 口径（确定性派生）

seed workcenter_calendar: START_TIME=`08:00`/END_TIME=`16:00`/WORK_DATE_PATTERN=`ALL_WEEK`/EFFECTIVE=`2026-07-01~2026-07-31`/IS_ACTIVE=true；crp_load.LOAD_DATE=`2026-07-15`（周三，ALL_WEEK 恒命中）：
- `shiftHours("08:00","16:00")` = Duration 480 min / 60 = 8.0000（SCALE=4）
- capacityHours = 8.0000 × efficiencyFactor(1.0000) = **8.0000**（渲染 `8.00`）
- loadRate = loadHours(4.00) / capacityHours(8.0000) = **0.5000**（渲染 `0.50`）
- overloaded = 0.5 ≤ 1.0 → false（50% 负荷，业务合理）

### CRP 重算覆盖防护

CRP 重算链经 nop-job 双层门控默认关：`erp-mfg.crp-run-cron` 默认空（`ErpMfgCrpRunJob` 不调度）+ `erp-mfg.crp-load-source`（默认 WORK_ORDER）不影响 `getLoadReport` 读既有行。fresh-DB 启动（`init-database-data=true`）不触发 `CrpLoadCalculator.calculateLoad` 重算，seed 静态 crp_load 行安全不被覆盖。重算链端到端回归属 Deferred。

### 加载拓扑序（跨域）

```
[1234-1 主数据(已 seed)] md_organization(2) / md_material(1)
[0930-1 制造域(已 seed)] erp_mfg_work_order(1 = WO-2026-001)
  → [工作中心配置头] erp_mfg_workcenter                            （无 mandatory FK，独立）
      → [工作中心配置行]
        erp_mfg_workcenter_calendar                                （workcenterId mandatory FK→workcenter）
        erp_mfg_workcenter_capacity                                （workcenterId mandatory FK + materialId FK→md_material=1）
      → [CRP 负荷快照行]
        erp_mfg_crp_load                                           （workcenterId mandatory FK→workcenter + workOrderId 弱指针→work_order=1）
```

### posted 一致性裁决（四表无 posted 列）

镜像 0930-1 裁决。四表（workcenter/workcenter_calendar/workcenter_capacity/crp_load）ORM 均**无 `posted` 列**（逐表 ORM 核实），crp_load 为负荷快照非 GL，看板/报表读域表，CSV 不含 posted。

### 域内数值自洽约束（期望值派生）

| token | 期望渲染值 | seed 行依据 |
|-------|-----------|------------|
| workcenterCode | `WC-001` | workcenter.CODE |
| loadDate | `2026-07-15` | crp_load.LOAD_DATE |
| loadHours | `4.00` | crp_load.LOAD_HOURS=4 |
| capacityHours | `8.00` | calendar 08:00~16:00 × efficiency 1 = 8.0000 |
| loadRate | `0.50` | 4.00 / 8.0000 = 0.5000 |

### Non-Goals（归后续批次）

- 制造域 GL 凭证/业财一体 seed（制造费用/差异过账凭证）——看板/报表读域表非 GL，种子科目表无制造域专用科目；触发条件：制造域业财一体端到端数值回归需 GL 串联时。（镜像 0930-1 既定 Deferred。）
- CRP 负荷前端可视化增强（echarts 负荷/产能对比图、超负荷高亮）——本计划使 crp-load 报表数值可观测（HTML token 断言），不做 echarts 可视化增强（前端能力面）；触发条件：产品要求 CRP 负荷看板可视化时。
- crp_load 重算链 seed / calculateLoad 端到端——本计划 seed 静态 crp_load 行令报表可观测，不触发 `CrpLoadCalculator.calculateLoad` 重算（重算会清区间写新行覆盖 seed）；触发条件：需在部署期种子上验证 calculateLoad 从 WorkOrder 重算到 crp_load 快照的端到端正确性时。
- 制造域配置/执行链其他表 seed（BOM/Routing/MRP/JobCard/MaterialIssue/Subcontract/CostRollup/BatchGenealogy/work_order_line）——0930-1 既定 Deferred，crp-load 报表 `getLoadReport` 不读这些表；触发条件不变。
## 全量化裁决（seed 全量覆盖口径，2026-09-01）

> 来源：plan `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（roadmap `comprehensive-test-data-and-visual-coverage` 工作项 M0.1）。本节是 app.erp.* 计数口径、seed 数据分层、270 缺 seed 实体最小数据集规格的**权威登记处**；`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` §目的 口径表中与本表冲突的表述以本表为准。视觉扩面边界裁决见 `docs/testing/e2e-runbook.md`「视觉断言扩面边界」节。

### 计数口径裁决（多档口径对账）

**裁决**：「seed 全量覆盖目标集」= `rg 'className="app\.erp\.' module-*/model/*.orm.xml` 唯一计数 **363**（2026-09-01 实仓复算）。分项裁决：

- **notGenCode 引用声明不计入**：实仓 113 处 `notGenCode="true"` 实体声明**全部仅携带 `name="app.erp.*"` 属性、无 `className` 属性**（含 drp 6 处等 19 文件），是跨域 FK join 的表引用别名；其 23 个物理表全部由 owner 域的 className 实体持有并已计入 363（程序化核验：notGenCode 引用表 ⊆ className 实体表集，零遗漏）。不生成独立 Entity 类、不产生新表 → 不进目标集、不进缺 seed 清单。roadmap §目的「363（含 notGenCode 子类）」的表述在实仓 grep 语义下精确化为：notGenCode 声明天然不在 `className="app.erp.*"` grep 口径内。
- **sys_* 跨域表计入**：`ErpSysNotificationTemplate`（已 seed）/ `ErpSysNotification` / `ErpSysNotificationRead`（缺 seed）均为 className `app.erp.notify.*` 实体 → 在 363 目标集内。
- **部署配置表 `ErpSysConfig` 计入**（className `app.erp.md.dao.entity.ErpSysConfig`，表 `erp_sys_config`）→ 在 363 内，且因无 CSV 在 270 缺 seed 清单内。
- **平台表（66 个）不计入**：非 `app.erp.*`（`NopAuthUser`/`NopSysDict` 等，平台资产管理边界）。其中 4 表（`nop_auth_user` / `nop_auth_user_role` / `nop_auth_role` / `nop_sys_code_rule`）因演示启动前置已有 seed CSV，属平台侧装载，不进入 363 覆盖目标。

**多档口径对账表**（权威登记处；roadmap §目的 口径表失实行按历史档在此登记，roadmap 本体勘误不在 M0.1 范围）：

| 档 | 值 | 来源 | 2026-09-01 实仓复核 | 状态 |
|---|---|---|---|---|
| className 唯一计数 | **363** | `rg 'className="app\.erp\.'` module-*/model/*.orm.xml | 复算 363；19 域分布与 roadmap §当前基线 1:1 零漂移 | **权威（当前）** |
| 有 seed 的 app.erp.* 实体 | 314 | `_init-data/` 314 个 `erp_*` CSV ↔ entity tableName 精确匹配 | 复算 314（93 基线 + M1.1a 批次 12 + M1.1b 批次 16 + M1.1c 批次 12 + M1.2a1 批次 31（26 规格表 + 5 运行时补充）+ M1.2a2 批次 26 + M1.2b 批次 28 + M1.2c 批次 19 + M1.3 批次 32 + M1.4a 批次 45，2026-09-02）；M1.4b 批次后 329（+15，见下方批次行） | 权威（当前） |
| 平台 seed CSV | 4 | `nop_auth_user` / `nop_auth_user_role` / `nop_auth_role` / `nop_sys_code_rule` | 复算 4 | 权威（当前） |
| 精确缺 seed | **54** | roadmap §当前基线 + 本节重算（entity tableName ↔ CSV 精确匹配，逐域小计相加） | 复算 54（= 99 − M1.4a 批次 45：crm 30 + cs 15 已落地，规格表 crm/cs 节已由 M1.4a 消费落地）；M1.4b 批次后 39（= 54 − 15：aps 7 + logistics 8 已落地，规格表 aps/logistics 节已由 M1.4b 消费落地）；其余 1 工作项（M1.5）待 M1.x 后续批次 | **权威（当前）** |
| M1.1a 批次增量 | +12 CSV（app.erp.*） | plan `docs/plans/2026-09-01-0838-1-m11a-md-sal-seed-expansion.md`（master-data 4 + sales 8，CSV-only 路径） | 复算：有 seed 93→105 / 缺 seed 270→258 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 93→105 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.1b 批次增量 | +16 CSV（app.erp.*） | plan `docs/plans/2026-09-01-0838-2-m11b-inventory-seed-expansion.md`（inventory 16，CSV-only 路径） | 复算：有 seed 105→121 / 缺 seed 258→242 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 105→121 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.1c 批次增量 | +12 CSV（app.erp.*） | plan `docs/plans/2026-09-01-0838-3-m11c-purchase-seed-expansion.md`（purchase 12，CSV-only 路径） | 复算：有 seed 121→133 / 缺 seed 242→230 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 121→133 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.2a1 批次增量 | +31 CSV（app.erp.* = 26 规格表 + 5 运行时补充） | plan `docs/plans/2026-09-01-1245-1-m12a1-finance-seed-expansion.md`（finance 26 规格表逐行消费 + 5 缺 className 运行时补充实体按 ORM 推导，CSV-only 路径；含过账凭证扩展 16→32 行（voucher 4→8 / line 8→16 / bill_r 4→8，凭证族内追加，决策 A）） | 复算：有 seed 133→164 / 精确缺 seed 230→204 / 运行时口径缺 235→204（补充档 5 实体转入有 seed，两口径收敛）/ 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 133→164 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.2a2 批次增量 | +26 CSV（app.erp.*） | plan `docs/plans/2026-09-01-1245-2-m12a2-manufacturing-seed-expansion.md`（manufacturing 26 规格表逐行消费，CSV-only 路径；零默认 BOM（`isDefault=false`）+ 零 FIRMED cost_rollup 种子 = C08 MRP 输入面与 mfg-chain 差异计算面零漂移设计） | 复算：有 seed 164→190 / 精确缺 seed 204→178 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 164→190 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.2b 批次增量 | +28 CSV（app.erp.*） | plan `docs/plans/2026-09-01-1245-3-m12b-mnt-qa-prj-seed-expansion.md`（maintenance 7 + quality 10 + projects 11 规格表逐行消费，CSV-only 路径；billing 全批 CANCELLED + settlement INTERIM/CANCELLED = C12 `findBillings`/`findActiveSettlementOfType` 输入面零漂移设计；`erp_qa_calibration` targetValue/tolerance 留空 = pre-existing ORM decimal/VARCHAR 列型 quirk 的 CSV-only 规避，登记该 plan Deferred But Adjudicated） | 复算：有 seed 190→218 / 精确缺 seed 178→150 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 190→218 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.2c 批次增量 | +19 CSV（app.erp.*） | plan `docs/plans/2026-09-01-2255-1-m12c-ast-notify-seed-expansion.md`（assets 17 + notify 2 规格表逐行消费，CSV-only 路径；cip 全行转固终态（`isCompleted=true`）= 看板 `sumCipBalance()` 过滤集空 → `cipBalance=0` 像素零漂移设计；notify 段 plan-first 保护区独立 plan-audit 通过（recipient=demo-user 与 E2E 用户 `nop` 零交集）；disposal successor 触发条件「ast-disposal gains E2E seed data」由本批达成，数值 token 升级登记 Deferred But Adjudicated 归 M2.x） | 复算：有 seed 218→237 / 精确缺 seed 150→131 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 218→237 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.3 批次增量 | +32 CSV（app.erp.*） | plan `docs/plans/2026-09-01-2255-2-m13-hr-seed-expansion.md`（hr 32 规格表逐行消费，CSV-only 路径；子表 7（competency_level / development_plan_item / shift_assignment / survey_question / survey_response / survey_result / timesheet_line）+ survey_answer 独立表；F7 PII 集 4 字段不在批内表（仅 ErpHrEmployee）+ recruitment 候选人 PII 列确定性伪值（plan-first 保护区独立 plan-audit 通过，`ses_fa1ca7ed6ffeJjneNygHBvCCiX`）；薪酬金额列保守纳入伪值纪律；C17（`TestErpC17HrSalaryPayment`）值中性化契约——employment_contract emp1/emp2 全行镜像 15000/8000、social_insurance_base 镜像 SHENZHEN 15000/15000、social_insurance_config 仅 SHANGHAI、tax_config/tax_special_deduction/salary 仅 year=2025、attendance/leave 冻结 2026-05 窗口；seed-data.md L353 HR 配置/执行链 Deferred 子集由本批消费） | 复算：有 seed 237→269 / 精确缺 seed 131→99 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 237→269 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.4a 批次增量 | +45 CSV（app.erp.*） | plan `docs/plans/2026-09-01-2255-3-m14a-crm-cs-seed-expansion.md`（crm 30 + cs 15 规格表逐行消费，CSV-only 路径；CRM 主子表组 6 组（bundle_pricing+line / lead_score+line / lead_score_config+line / sequence+assignment+step / team+member / territory+assignment_rule）+ `erp_crm_lead_seq_progress.csv` 按 ORM tableName 缩写文件名；cs ticket 全生命周期周边表（action / fulfillment_step / timer_session / time_entry）+ catalog 链（catalog_category / service_catalog_item / catalog_fulfillment）+ sla_policy / team / entitlement / contract / knowledge_base / canned 族 / agent_rate；用例指示编码以 REMARK 后缀（P）/（N-TERM）/（N-DIS）落地；干扰面零漂移设计——既有 lead 零 CAMPAIGN_ID 列 + 零新增 lead 行 = campaignAttribution 空集短路、既有 ticket 零 SLA_POLICY_ID 列 = quality dashboard 空集短路（plan 执行证据复证）；seed-data.md L352 CRM/CS 配置/执行链 Deferred 子集由本批消费） | 复算：有 seed 269→314 / 精确缺 seed 99→54 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 269→314 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| M1.4b 批次增量 | +15 CSV（app.erp.*） | plan `docs/plans/2026-09-02-1415-1-m14b-aps-logistics-seed-expansion.md`（aps 7 + logistics 8 规格表逐行消费，CSV-only 路径；logistics 主子表组 4 组（carrier+config / shipment+line / shipment+log / shipment+parcel）+ delivery_booking→shipment+window 链路 + aps dispatch_log→operation_order 边；用例指示编码以 REMARK 后缀（P）/（N-TERM）/（N-DIS）落地；干扰面零漂移设计——aps 种子值域裁决：operation_order P 行 status=PLANNED（排程引擎 pending 集 {DRAFT,UNSCHEDULABLE} 之外）+ machineId 7001/7002 避开 fixture 值 1/C21 机器 + earliestStartDateT 静态 2026-08 避开 C08 窗口 2026-07-10~20、constraint 窗口 2026-08-10~12 与 C08 窗口不相交 + machineId 7001、op_routing machineId 7001/7002 ≠ fixture 机器（引擎仅消费 `isDefault=true && machineId=op.machineId` 默认行，复证零候选）、capacity_reservation machineId 7001（`hasOverlappingReservation` 机器维度隔离）、dispatch_rule 惰性（`erp-aps.auto-dispatch-enabled` 缺省 false + `erp-aps-auto-dispatch.job.yaml` enabled 缺省 false 双门控）；logistics 种子 shipment 取 IN_TRANSIT/CANCELLED 态避开批扫描面（`erp-log-tracking-poll.job.yaml` / `erp-log-draft-escalation.job.yaml` enabled 缺省 false 实证休眠）；seed-data.md「其他扩展域交易种子」Deferred 之 aps/logistics 子集由本批消费） | 复算：有 seed 314→329 / 精确缺 seed 54→39 / 平台 4 不变；`TestErpSeedDataIntegrity` 基线常量 314→329 随批更新（常量 javadoc 登记） | 权威（当前批次档） |
| 「backlog README 350」 | 350 | roadmap §目的 口径表「backlog README L134（过时应更新）」行 | backlog README L134 现值已是「约 363」，全文件 0 处「350」→ 该行对 README 现值的表述已失实 | **已失效历史档**（仅存于 roadmap §目的 口径表表述；以本表为登记处，roadmap 本体勘误不在 M0.1 范围） |
| 门禁注释 app.erp 计数 | 352 | `TestErpSeedDataIntegrity.java:36`「Phase 1 Decision (a)：418 = app.erp.* 352 + 平台 66」 | 实仓 363 > 352：注释早于后续 ORM 实体扩展 | 过期历史档（计数修正归 M0.2 门禁扩展消费） |
| seed-data 旧总口径 | 418 = 352 + 66 | 本文档「通用引用完整性校验」段 | findAll 全实体门禁语义仍有效，唯计数过期（app.erp 352→363） | 过期历史档（修正归 M0.2 消费） |
| 1143-1 宽口径缺 seed | 275 | plan `2026-08-31-1143-1` L56「**不**补全 275 张缺失表 seed」 | 原文未附逐表明细，无法从现行实仓 1:1 复算；与精确 270 差 +5 归因于当时实体集/口径未细分 | 历史档（被 270 精确清点取代） |
| 运行时注册实体集（`getEntityNames`） | **368 = 363 + 5** | 2026-09-01 M0.2 门禁实测（`TestErpSeedDataIntegrity#testAdjudicatedScopePinned`，plan `2026-09-01-0527-1`） | +5 = finance 域 5 个模型声明缺 `className` 属性实体（`ErpFinCashForecast` / `ErpFinCreditFacility` / `ErpFinNotesDiscount` / `ErpFinNotesPayable` / `ErpFinNotesReceivable`）——实体定义完整（物理表/生成 Java 类均在，codegen 与运行时按 entity name 默认补齐）但 className grep 口径天然遗漏、无 seed CSV、不在下方 270 规格表内 | **补充档（M1.x seed 补齐须按运行时口径消费：M1.1c 后全集缺 = 230 + 5 = 235；M1.2a1 批次（2026-09-01）已按运行时口径消费 finance 31 = 26 + 5——5 补充实体现均有 seed，运行时口径缺 = 204，与精确口径收敛；M1.2a2 批次后运行时口径缺 = 精确口径缺 = 178（manufacturing 26 均 className 实体，补充档不涉及）；其余域待 M1.x 后续批次按同口径核查；roadmap 本体勘误归 roadmap owner）** |

**精确化附注**：roadmap §目的 口径表对 270 的注「不含 sys_* 部署表」须按本表精确化——270 = 363 全集减 93 有 CSV 者，**包含** `erp_sys_config` / `erp_sys_notification` / `erp_sys_notification_read` 三个 sys_* 语义的 app.erp.* 实体（它们是 className 实体）；「不含」仅对平台 `nop_*` 表成立（本就不在 app.erp.* 口径内）。

**替代方案（rejected）与残留风险**：

- 替代 1：目标集扩为「全部物理表」（363 + 平台 66 + notGenCode 别名表）——平台表归平台资产管理边界、notGenCode 是别名非独立实体，扩集徒增门禁噪音且越权平台边界。rejected。
- 替代 2：将 `ErpSysConfig` / `ErpSysNotification` 族移出目标集（「部署配置」语义豁免）——它们是业务可查实体（GraphQL/AMIS CRUD 面在网），移出将再造「全量覆盖」与门禁 findAll 范围的口径漂移。rejected。
- 残留风险：ORM 实体集随产品演进继续增长，363 / 93 / 270 是 2026-09-01 时点快照；M0.2 / M1.x / M3.1 各门禁点须按同命令重算刷新，漂移 > 0 时先更新本表再消费，禁止沿用旧计数起草新 plan。

### seed 数据分层裁决（演示种子 / E2E 种子 / 业务动作 negative 种子）

| 层 | 定义 | 物理载体 | 装载语义 |
|---|---|---|---|
| 演示种子（demo seed） | fresh-DB 启动后演示/沙盒界面可见的最小可用业务数据 | `_vfs/_init-data/` 全部资产（97 CSV + 1 SQL + M1.x 新增 CSV） | 1143-1 裁决：`application.yaml` 默认 `init-database-data: true`（演示/沙盒默认开）；fresh-DB 重置是运行前置（`DataInitInitializer` 非幂等）；生产部署按 1143-1 裁决改用 MySQL/PostgreSQL + 数据集隔离 + 业务动作闸门配置，不复用演示脚本 |
| E2E 种子（E2E seed） | E2E「数据可见性」断言（`*.list-value.spec.ts` / `*.visual.spec.ts` / 像素层）读取的数据 | **与演示种子同物理集**（同一批 `_init-data/` 行）——E2E 不设第二 CSV 集 | 同上（E2E webServer 即 fresh-DB 全量装载）；E2E 会话级动态行由测试自建自清理，不入 seed |
| 业务动作 negative 种子（negative seed） | 供业务动作 spec 非法迁移/守卫负路径直接消费的**专用行**（终态行、禁用行等） | **并入各实体 CSV 内**（不新建独立 negative 文件、不入测试夹具模块） | 同上演示种子装载；行级用途以本文「规格表」用例指示列为准 |

**分层边界规则**：(1) 三层全部落在 1143-1「演示/沙盒默认装载」语义内，prod 语义不变（部署显式关闭或评审后开启；`TestErpSeedDataIntegrity` 门禁是 prod 适用面的完整性底线——roadmap 横切关注点 5）；(2) 命名约定唯一 = `<tableName>.csv`（`DataInitInitializer.loadCsvData` 契约，soft-shorthand 如 `erp_md_uom` 沿用既有表名，不新引入别名体系）；(3) `app-erp-test-data` 测试共享夹具是**测试资产**，与本部署资产边界不可混淆（本文档头部资产类型裁决维持）。

**替代方案（rejected）与残留风险**：

- 替代 1：E2E 独立种子目录（`init-database-data-location` 按环境切换）——形成双真相源 + E2E 与演示数据漂移风险，E2E 与演示的可见性诉求高度重叠。rejected。
- 替代 2：negative 种子入 `app-erp-test-data` 夹具——与「部署资产 vs 测试资产」边界冲突；且业务动作 E2E 运行在 fresh-DB 全量装载后，夹具行不在该装载路径上。rejected。
- 残留风险：negative 行混入演示界面可能展示「异常状态」行——以每 CSV 行数上限（≤ 20）+ 本节规格表用例指示控制；若某 negative 行造成演示语义困扰，按「快照重录义务」节流程迁移至测试夹具并在此登记（触发条件：演示验收方提出异议）。

### 270 个缺 seed 实体最小可用数据集规格表（M1.x 直接消费）

> Phase 2 产出（2026-09-01）。本规格表按域逐实体列出 270 个缺 seed 实体（精确对齐 roadmap §当前基线「缺 seed」列），供 M0.2 门禁扩展与 M1.x 11 个工作项 plan 起草时**逐行引用**；每域标题标注对应 M1.x 工作项编号（每工作项 ≤ 50 实体拆分约束天然满足：最大单域 32）。

**通用约定**：

1. **文件名** = `<tableName>.csv`（tableName 以 ORM 实体定义为唯一权威；`DataInitInitializer.loadCsvData` 按表名查找）。
2. **行数上限**：每 CSV ≤ 20 行（roadmap M1 补齐原则）；「建议行数」列给域内建议（配置/字典 1~3、单据头 2~3 跨状态、行表 1~2/头、日志 1~2）。
3. **拓扑序**：同批 CSV 无需手工排序文件名——`DataInitInitializer` 按 ORM `getEntityModelsInTopoOrder()` 自动排序。
4. **FK 闭环规则**：必填 FK 必须指向「已 seed 行」（标记〔已seed〕）或「同批 M1.x 新增行」（标记〔本批〕）；可选 FK 可留空或指向已 seed 行；**禁止悬空引用**（`TestErpSeedDataIntegrity` 引用完整性门禁，白名单豁免须登记证据）。跨域引用显式标注 `〔跨域:<域>·已seed|本批〕`。
5. **用例指示编码**：`P` = 最小正例行；`N-TERM` = 附 1 行终态行（docStatus/status 终态如 CANCELLED/REJECTED/CLOSED，供业务动作非法迁移守卫负路径）；`N-DIS` = 附 1 行禁用/停用行（enabled/isActive=false，供启用前置守卫负路径）；行表用例随头（`1~2/头`）。编码依据 = 实体 ORM 是否携带 `docStatus`/`status`/`enabled`/`isActive` 列（2026-09-01 逐实体程序化核验：124/270 实体带状态列）。
6. **审计列**：CSV 列头省略审计列（对齐既有 97 CSV 约定，见「模块 deploy 种子同步义务」节）。
7. **敏感字段**：HR 域 PII 字段按 2026-08-11 E4.2 MaskHelper 范式脱敏后再 seed（roadmap M1.3 约束沿用，owner doc F7 PII 集为准）。
8. **owner-doc 同步**：每工作项完成后按 roadmap 规则回写 `docs/design/<domain>/` 增「种子数据」段。

**CSV-only 可满足性核验（Phase 2 编制时程序化核验，2026-09-01）**：270 实体的 mandatory to-one 关系**零环**、**零指向未知实体**；必填 FK 目标全部为「已 seed」或「同批 M1.x」实体 → **全部 270 实体可在 CSV-only 路径下承载最小数据集，无需 ORM 增列**，本 plan「Deferred But Adjudicated」保持零条目。

**与 M1.x 拆分对齐小计**（逐域小计相加 = 270，零遗漏零重复）：

| 域 | 缺 seed | M1.x 工作项 |
|---|---|---|
| master-data | 4 | M1.1a |
| sales | 8 | M1.1a |
| inventory | 16 | M1.1b |
| purchase | 12 | M1.1c |
| finance | 26 | M1.2a1 |
| manufacturing | 26 | M1.2a2 |
| maintenance | 7 | M1.2b |
| quality | 10 | M1.2b |
| projects | 11 | M1.2b |
| assets | 17 | M1.2c |
| notify | 2 | M1.2c |
| hr | 32 | M1.3 |
| crm | 30 | M1.4a |
| cs | 15 | M1.4a |
| aps | 7 | M1.4b |
| logistics | 8 | M1.4b |
| b2b | 13 | M1.5 |
| contract | 15 | M1.5 |
| drp | 11 | M1.5 |
| **总计** | **270** | 11 工作项 |

逐域规格表（域序对齐小计表；域内按实体名字典序）：
#### master-data（4 缺 → M1.x 工作项 **M1.1a**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpMdMaterialCustoms | `erp_md_material_customs.csv` | 1~3 | —（独立表） | ErpMdMaterial〔已seed〕 | P |
| ErpMdSubjectMapping | `erp_md_subject_mapping.csv` | 1~3 | —（独立表） | ErpMdSubject〔已seed〕、ErpMdAcctSchema〔已seed〕 | P |
| ErpMdSupplierApproval | `erp_md_supplier_approval.csv` | 2~3 | —（独立表） | ErpMdPartner〔已seed〕、ErpMdMaterialCategory〔已seed〕 | P+N-TERM |
| ErpSysConfig | `erp_sys_config.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |

#### sales（8 缺 → M1.x 工作项 **M1.1a**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpSalContract | `erp_sal_contract.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕 | P+N-TERM |
| ErpSalPriceList | `erp_sal_price_list.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpSalPriceListLine | `erp_sal_price_list_line.csv` | 1~2/头 | ErpSalPriceList（子表） | ErpSalPriceList〔本批〕 | P |
| ErpSalPricingRule | `erp_sal_pricing_rule.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpSalQuotation | `erp_sal_quotation.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕 | P+N-TERM |
| ErpSalQuotationLine | `erp_sal_quotation_line.csv` | 1~2/头 | ErpSalQuotation（子表） | ErpSalQuotation〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpSalReturn | `erp_sal_return.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdWarehouse〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕 | P+N-TERM |
| ErpSalReturnLine | `erp_sal_return_line.csv` | 1~2/头 | ErpSalReturn（子表） | ErpSalReturn〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |

#### inventory（16 缺 → M1.x 工作项 **M1.1b**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpInvBatch | `erp_inv_batch.csv` | 2~3 | —（独立表） | ErpMdMaterial〔跨域:md·已seed〕、ErpMdWarehouse〔跨域:md·已seed〕 | P+N-TERM |
| ErpInvCostAdjust | `erp_inv_cost_adjust.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpInvCostAdjustLine | `erp_inv_cost_adjust_line.csv` | 1~2/头 | ErpInvCostAdjust（子表） | ErpInvCostAdjust〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdWarehouse〔跨域:md·已seed〕 | P |
| ErpInvLandedCost | `erp_inv_landed_cost.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpInvLandedCostLine | `erp_inv_landed_cost_line.csv` | 1~2/头 | ErpInvLandedCost（子表） | ErpInvLandedCost〔本批〕 | P |
| ErpInvOwnershipTransfer | `erp_inv_ownership_transfer.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdWarehouse〔跨域:md·已seed〕、ErpMdLocation〔跨域:md·已seed〕 | P+N-TERM |
| ErpInvOwnershipTransferLine | `erp_inv_ownership_transfer_line.csv` | 1~2/头 | ErpInvOwnershipTransfer（子表） | ErpInvOwnershipTransfer〔本批〕、ErpMdMaterial〔跨域:md·已seed〕 | P |
| ErpInvPickingOrder | `erp_inv_picking_order.csv` | 2~3 | —（独立表） | ErpMdWarehouse〔跨域:md·已seed〕 | P+N-TERM |
| ErpInvPickingOrderLine | `erp_inv_picking_order_line.csv` | 1~2/头 | ErpInvPickingOrder（子表） | ErpInvPickingOrder〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpInvReservation | `erp_inv_reservation.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpInvReservationLine | `erp_inv_reservation_line.csv` | 1~2/头 | ErpInvReservation（子表） | ErpInvReservation〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdWarehouse〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpInvSerialNumber | `erp_inv_serial_number.csv` | 2~3 | —（独立表） | ErpMdMaterial〔跨域:md·已seed〕 | P+N-TERM |
| ErpInvStockTake | `erp_inv_stock_take.csv` | 2~3 | —（独立表） | ErpMdWarehouse〔跨域:md·已seed〕 | P+N-TERM |
| ErpInvStockTakeLine | `erp_inv_stock_take_line.csv` | 1~2/头 | ErpInvStockTake（子表） | ErpInvStockTake〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpInvTransferOrder | `erp_inv_transfer_order.csv` | 2~3 | —（独立表） | ErpMdWarehouse〔跨域:md·已seed〕 | P+N-TERM |
| ErpInvTransferOrderLine | `erp_inv_transfer_order_line.csv` | 1~2/头 | ErpInvTransferOrder（子表） | ErpInvTransferOrder〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |

#### purchase（12 缺 → M1.x 工作项 **M1.1c**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpPurQuotation | `erp_pur_quotation.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕 | P+N-TERM |
| ErpPurQuotationLine | `erp_pur_quotation_line.csv` | 1~2/头 | ErpPurQuotation（子表） | ErpPurQuotation〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpPurRequisition | `erp_pur_requisition.csv` | 2~3 | —（独立表） | ErpMdEmployee〔跨域:md·已seed〕 | P+N-TERM |
| ErpPurRequisitionLine | `erp_pur_requisition_line.csv` | 1~2/头 | ErpPurRequisition（子表） | ErpPurRequisition〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpPurReturn | `erp_pur_return.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdWarehouse〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕 | P+N-TERM |
| ErpPurReturnLine | `erp_pur_return_line.csv` | 1~2/头 | ErpPurReturn（子表） | ErpPurReturn〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpPurRfq | `erp_pur_rfq.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpPurRfqLine | `erp_pur_rfq_line.csv` | 1~2/头 | ErpPurRfq（子表） | ErpPurRfq〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpPurSupplierPriceList | `erp_pur_supplier_price_list.csv` | 1~2 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕 | P+N-DIS |
| ErpPurSupplierScorecard | `erp_pur_supplier_scorecard.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕 | P+N-TERM |
| ErpPurSupplierScorecardCriteria | `erp_pur_supplier_scorecard_criteria.csv` | 1~2/头 | ErpPurSupplierScorecard（子表） | ErpPurSupplierScorecard〔本批〕 | P |
| ErpPurSupplierScorecardVariable | `erp_pur_supplier_scorecard_variable.csv` | 1~3 | —（独立表） | ErpPurSupplierScorecardCriteria〔本批〕 | P |

#### finance（26 缺 → M1.x 工作项 **M1.2a1**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpFinApDocument | `erp_fin_ap_document.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpFinApDocumentLog | `erp_fin_ap_document_log.csv` | 1~2/头 | ErpFinApDocument（子表） | ErpFinApDocument〔本批〕 | P |
| ErpFinBadDebt | `erp_fin_bad_debt.csv` | 1~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdAcctSchema〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕、ErpFinArApItem〔已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P |
| ErpFinBankReconciliation | `erp_fin_bank_reconciliation.csv` | 2~3 | —（独立表） | ErpFinFundAccount〔本批〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinBankReconciliationLine | `erp_fin_bank_reconciliation_line.csv` | 1~2/头 | ErpFinBankReconciliation（子表） | ErpFinBankReconciliation〔本批〕 | P |
| ErpFinBankStatement | `erp_fin_bank_statement.csv` | 2~3 | —（独立表） | ErpFinFundAccount〔本批〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinBankStatementLine | `erp_fin_bank_statement_line.csv` | 1~2/头 | ErpFinBankStatement（子表） | ErpFinBankStatement〔本批〕、ErpMdCurrency〔跨域:md·已seed〕 | P |
| ErpFinBudgetCarryForwardLog | `erp_fin_budget_carry_forward_log.csv` | 1~3 | —（独立表） | ErpFinBudgetScenario〔本批〕、ErpMdOrganization〔跨域:md·已seed〕 | P |
| ErpFinBudgetControlLog | `erp_fin_budget_control_log.csv` | 1~3 | —（独立表） | ErpMdOrganization〔跨域:md·已seed〕 | P |
| ErpFinBudgetLine | `erp_fin_budget_line.csv` | 1~3 | —（独立表） | ErpFinBudgetScenario〔本批〕、ErpMdAcctSchema〔跨域:md·已seed〕、ErpMdSubject〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P |
| ErpFinBudgetRollforwardLog | `erp_fin_budget_rollforward_log.csv` | 1~3 | —（独立表） | ErpFinBudgetScenario〔本批〕、ErpMdOrganization〔跨域:md·已seed〕 | P |
| ErpFinBudgetScenario | `erp_fin_budget_scenario.csv` | 2~3 | —（独立表） | ErpMdAcctSchema〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinConsolidationElimination | `erp_fin_consolidation_elimination.csv` | 2~3 | —（独立表） | ErpFinAccountingPeriod〔已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinEmployeeAdvance | `erp_fin_employee_advance.csv` | 2~3 | —（独立表） | ErpMdEmployee〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinExpenseClaim | `erp_fin_expense_claim.csv` | 2~3 | —（独立表） | ErpMdEmployee〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinExpenseClaimLine | `erp_fin_expense_claim_line.csv` | 1~2/头 | ErpFinExpenseClaim（子表） | ErpFinExpenseClaim〔本批〕 | P |
| ErpFinFundAccount | `erp_fin_fund_account.csv` | 2~3 | —（独立表） | ErpMdCurrency〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinGlMappingRule | `erp_fin_gl_mapping_rule.csv` | 1~2 | —（独立表） | ErpMdOrganization〔跨域:md·已seed〕 | P+N-DIS |
| ErpFinIntercompanyMatch | `erp_fin_intercompany_match.csv` | 2~3 | —（独立表） | ErpFinAccountingPeriod〔已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinIntercompanyTransferPrice | `erp_fin_intercompany_transfer_price.csv` | 1~2 | —（独立表） | ErpMdOrganization〔跨域:md·已seed〕 | P+N-DIS |
| ErpFinPostingException | `erp_fin_posting_exception.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpFinReconciliation | `erp_fin_reconciliation.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdAcctSchema〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P+N-TERM |
| ErpFinReconciliationLine | `erp_fin_reconciliation_line.csv` | 1~2/头 | ErpFinReconciliation（子表） | ErpFinReconciliation〔本批〕、ErpFinArApItem〔已seed〕 | P |
| ErpFinTrialBalance | `erp_fin_trial_balance.csv` | 1~3 | —（独立表） | ErpMdAcctSchema〔跨域:md·已seed〕、ErpFinAccountingPeriod〔已seed〕、ErpMdSubject〔跨域:md·已seed〕、ErpMdOrganization〔跨域:md·已seed〕 | P |
| ErpFinVoucherTemplate | `erp_fin_voucher_template.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpFinVoucherTemplateLine | `erp_fin_voucher_template_line.csv` | 1~2/头 | ErpFinVoucherTemplate（子表） | ErpFinVoucherTemplate〔本批〕 | P |

#### manufacturing（26 缺 → M1.x 工作项 **M1.2a2**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpMfgBatchGenealogy | `erp_mfg_batch_genealogy.csv` | 1~3 | —（独立表） | ErpMfgWorkOrder〔已seed〕、ErpMdMaterial〔跨域:md·已seed〕、ErpInvBatch〔跨域:inv·本批〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgBom | `erp_mfg_bom.csv` | 1~2 | —（独立表） | ErpMdMaterial〔跨域:md·已seed〕 | P+N-DIS |
| ErpMfgBomByproduct | `erp_mfg_bom_byproduct.csv` | 1~2/头 | ErpMfgBom（子表） | ErpMfgBom〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgBomLine | `erp_mfg_bom_line.csv` | 1~2/头 | ErpMfgBom（子表） | ErpMfgBom〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgBomOperation | `erp_mfg_bom_operation.csv` | 1~2/头 | ErpMfgBom（子表） | ErpMfgBom〔本批〕、ErpMfgRoutingOperation〔本批〕 | P |
| ErpMfgCostRollup | `erp_mfg_cost_rollup.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpMfgCostRollupLine | `erp_mfg_cost_rollup_line.csv` | 1~2/头 | ErpMfgCostRollup（子表） | ErpMfgCostRollup〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgJobCard | `erp_mfg_job_card.csv` | 2~3 | —（独立表） | ErpMfgWorkOrder〔已seed〕 | P+N-TERM |
| ErpMfgJobCardTimeLog | `erp_mfg_job_card_time_log.csv` | 1~2/头 | ErpMfgJobCard（子表） | ErpMfgJobCard〔本批〕、ErpMfgWorkOrder〔已seed〕、ErpMdEmployee〔跨域:md·已seed〕 | P |
| ErpMfgMaterialIssue | `erp_mfg_material_issue.csv` | 2~3 | —（独立表） | ErpMfgWorkOrder〔已seed〕、ErpMdWarehouse〔跨域:md·已seed〕 | P+N-TERM |
| ErpMfgMaterialIssueLine | `erp_mfg_material_issue_line.csv` | 1~2/头 | ErpMfgMaterialIssue（子表） | ErpMfgMaterialIssue〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgMrpDemand | `erp_mfg_mrp_demand.csv` | 1~3 | —（独立表） | ErpMfgMrpPlan〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgMrpPlan | `erp_mfg_mrp_plan.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpMfgMrpPlanLine | `erp_mfg_mrp_plan_line.csv` | 1~2/头 | ErpMfgMrpPlan（子表） | ErpMfgMrpPlan〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgMrpScenario | `erp_mfg_mrp_scenario.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpMfgMrpScenarioParam | `erp_mfg_mrp_scenario_param.csv` | 1~2/头 | ErpMfgMrpScenario（子表） | ErpMfgMrpScenario〔本批〕 | P |
| ErpMfgMrpScenarioVersion | `erp_mfg_mrp_scenario_version.csv` | 1~2/头 | ErpMfgMrpScenario（子表） | ErpMfgMrpScenario〔本批〕 | P+N-TERM |
| ErpMfgProductionVersion | `erp_mfg_production_version.csv` | 1~2 | —（独立表） | ErpMdMaterial〔跨域:md·已seed〕、ErpMfgBom〔本批〕、ErpMfgRouting〔本批〕 | P+N-DIS |
| ErpMfgRouting | `erp_mfg_routing.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpMfgRoutingOperation | `erp_mfg_routing_operation.csv` | 1~2/头 | ErpMfgRouting（子表） | ErpMfgRouting〔本批〕 | P |
| ErpMfgSubcontractOrder | `erp_mfg_subcontract_order.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdCurrency〔跨域:md·已seed〕、ErpMdMaterial〔跨域:md·已seed〕 | P+N-TERM |
| ErpMfgSubcontractOrderLine | `erp_mfg_subcontract_order_line.csv` | 1~2/头 | ErpMfgSubcontractOrder（子表） | ErpMfgSubcontractOrder〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMfgWorkOrderBomLineSnapshot | `erp_mfg_work_order_bom_line_snapshot.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpMfgWorkOrderBomOperationSnapshot | `erp_mfg_work_order_bom_operation_snapshot.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpMfgWorkOrderBomSnapshot | `erp_mfg_work_order_bom_snapshot.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpMfgWorkOrderLine | `erp_mfg_work_order_line.csv` | 1~3 | —（独立表） | ErpMfgWorkOrder〔已seed〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |

#### maintenance（7 缺 → M1.x 工作项 **M1.2b**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpMntCalibration | `erp_mnt_calibration.csv` | 2~3 | —（独立表） | ErpMntEquipment〔已seed〕 | P+N-TERM |
| ErpMntEquipmentStatusLog | `erp_mnt_equipment_status_log.csv` | 1~3 | —（独立表） | ErpMntEquipment〔已seed〕 | P |
| ErpMntMaintenanceTeam | `erp_mnt_maintenance_team.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpMntMaintenanceTeamMember | `erp_mnt_maintenance_team_member.csv` | 1~2/头 | ErpMntMaintenanceTeam（子表） | ErpMntMaintenanceTeam〔本批〕、ErpMdEmployee〔跨域:md·已seed〕 | P |
| ErpMntSparePartUsageLine | `erp_mnt_spare_part_usage_line.csv` | 1~3 | —（独立表） | ErpMntSparePartUsage〔已seed〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdUoM〔跨域:md·已seed〕 | P |
| ErpMntTaskTemplate | `erp_mnt_task_template.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpMntTaskTemplateLine | `erp_mnt_task_template_line.csv` | 1~2/头 | ErpMntTaskTemplate（子表） | ErpMntTaskTemplate〔本批〕 | P |

#### quality（10 缺 → M1.x 工作项 **M1.2b**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpQaCalibration | `erp_qa_calibration.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpQaInspectionLine | `erp_qa_inspection_line.csv` | 1~3 | —（独立表） | ErpQaInspection〔已seed〕 | P |
| ErpQaInspectionTemplate | `erp_qa_inspection_template.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpQaInspectionTemplateLine | `erp_qa_inspection_template_line.csv` | 1~2/头 | ErpQaInspectionTemplate（子表） | ErpQaInspectionTemplate〔本批〕 | P |
| ErpQaQualityGoal | `erp_qa_quality_goal.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpQaRecall | `erp_qa_recall.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpQaRecallTarget | `erp_qa_recall_target.csv` | 1~2/头 | ErpQaRecall（子表） | ErpQaRecall〔本批〕 | P |
| ErpQaReview | `erp_qa_review.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpQaRiskRegister | `erp_qa_risk_register.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpQaSamplingPlan | `erp_qa_sampling_plan.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |

#### projects（11 缺 → M1.x 工作项 **M1.2b**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpPrjActivityType | `erp_prj_activity_type.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpPrjBilling | `erp_prj_billing.csv` | 2~3 | —（独立表） | ErpPrjProject〔已seed〕、ErpMdPartner〔跨域:md·已seed〕 | P+N-TERM |
| ErpPrjBillingLine | `erp_prj_billing_line.csv` | 1~2/头 | ErpPrjBilling（子表） | ErpPrjBilling〔本批〕 | P |
| ErpPrjBudgetLine | `erp_prj_budget_line.csv` | 1~3 | —（独立表） | ErpPrjBudget〔已seed〕 | P |
| ErpPrjCostCollectionLine | `erp_prj_cost_collection_line.csv` | 1~3 | —（独立表） | ErpPrjCostCollection〔已seed〕 | P |
| ErpPrjMilestone | `erp_prj_milestone.csv` | 2~3 | —（独立表） | ErpPrjProject〔已seed〕 | P+N-TERM |
| ErpPrjProjectSettlement | `erp_prj_project_settlement.csv` | 2~3 | —（独立表） | ErpPrjProject〔已seed〕 | P+N-TERM |
| ErpPrjProjectSettlementLine | `erp_prj_project_settlement_line.csv` | 1~2/头 | ErpPrjProjectSettlement（子表） | ErpPrjProjectSettlement〔本批〕 | P |
| ErpPrjProjectUser | `erp_prj_project_user.csv` | 1~3 | —（独立表） | ErpPrjProject〔已seed〕、ErpMdEmployee〔跨域:md·已seed〕 | P |
| ErpPrjRole | `erp_prj_role.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpPrjTask | `erp_prj_task.csv` | 2~3 | —（独立表） | ErpPrjProject〔已seed〕 | P+N-TERM |

#### assets（17 缺 → M1.x 工作项 **M1.2c**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpAstAssetActionLog | `erp_ast_asset_action_log.csv` | 1~3 | —（独立表） | ErpAstAsset〔已seed〕 | P |
| ErpAstAssetCapitalization | `erp_ast_asset_capitalization.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpAstAssetModel | `erp_ast_asset_model.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpAstCip | `erp_ast_cip.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpAstCipCostItem | `erp_ast_cip_cost_item.csv` | 1~2/头 | ErpAstCip（子表） | ErpAstCip〔本批〕 | P |
| ErpAstCipProgressBilling | `erp_ast_cip_progress_billing.csv` | 1~2/头 | ErpAstCip（子表） | ErpAstCip〔本批〕 | P |
| ErpAstDisposal | `erp_ast_disposal.csv` | 2~3 | —（独立表） | ErpAstAsset〔已seed〕 | P+N-TERM |
| ErpAstInventory | `erp_ast_inventory.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpAstInventoryLine | `erp_ast_inventory_line.csv` | 1~2/头 | ErpAstInventory（子表） | ErpAstInventory〔本批〕 | P |
| ErpAstMaintenance | `erp_ast_maintenance.csv` | 2~3 | —（独立表） | ErpAstAsset〔已seed〕 | P+N-TERM |
| ErpAstMaintenanceCost | `erp_ast_maintenance_cost.csv` | 1~2/头 | ErpAstMaintenance（子表） | ErpAstMaintenance〔本批〕 | P |
| ErpAstMerge | `erp_ast_merge.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpAstMergeLine | `erp_ast_merge_line.csv` | 1~2/头 | ErpAstMerge（子表） | ErpAstMerge〔本批〕、ErpAstAsset〔已seed〕 | P |
| ErpAstMovement | `erp_ast_movement.csv` | 2~3 | —（独立表） | ErpAstAsset〔已seed〕 | P+N-TERM |
| ErpAstSplit | `erp_ast_split.csv` | 2~3 | —（独立表） | ErpAstAsset〔已seed〕 | P+N-TERM |
| ErpAstSplitLine | `erp_ast_split_line.csv` | 1~2/头 | ErpAstSplit（子表） | ErpAstSplit〔本批〕 | P |
| ErpAstValueAdjustment | `erp_ast_value_adjustment.csv` | 2~3 | —（独立表） | ErpAstAsset〔已seed〕 | P+N-TERM |

#### notify（2 缺 → M1.x 工作项 **M1.2c**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpSysNotification | `erp_sys_notification.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpSysNotificationRead | `erp_sys_notification_read.csv` | 1~2/头 | ErpSysNotification（子表） | ErpSysNotification〔本批〕 | P |

#### hr（32 缺 → M1.x 工作项 **M1.3**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpHrAssessmentDetail | `erp_hr_assessment_detail.csv` | 1~3 | —（独立表） | ErpHrEmployeeAssessment〔本批〕、ErpHrCompetency〔本批〕 | P |
| ErpHrAttendance | `erp_hr_attendance.csv` | 1~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P |
| ErpHrCompetency | `erp_hr_competency.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpHrCompetencyLevel | `erp_hr_competency_level.csv` | 1~2/头 | ErpHrCompetency（子表） | ErpHrCompetency〔本批〕 | P |
| ErpHrDevelopmentPlan | `erp_hr_development_plan.csv` | 2~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P+N-TERM |
| ErpHrDevelopmentPlanItem | `erp_hr_development_plan_item.csv` | 1~2/头 | ErpHrDevelopmentPlan（子表） | ErpHrDevelopmentPlan〔本批〕、ErpHrCompetency〔本批〕 | P+N-TERM |
| ErpHrEmployeeAssessment | `erp_hr_employee_assessment.csv` | 2~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P+N-TERM |
| ErpHrEmploymentContract | `erp_hr_employment_contract.csv` | 2~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P+N-TERM |
| ErpHrGapAnalysis | `erp_hr_gap_analysis.csv` | 1~3 | —（独立表） | ErpHrEmployee〔已seed〕、ErpHrCompetency〔本批〕 | P |
| ErpHrLeaveBalance | `erp_hr_leave_balance.csv` | 1~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P |
| ErpHrLeaveRequest | `erp_hr_leave_request.csv` | 2~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P+N-TERM |
| ErpHrPayrollBankFile | `erp_hr_payroll_bank_file.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpHrPosition | `erp_hr_position.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpHrRecruitment | `erp_hr_recruitment.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpHrRoleCompetency | `erp_hr_role_competency.csv` | 1~3 | —（独立表） | ErpHrPosition〔本批〕、ErpHrCompetency〔本批〕 | P |
| ErpHrSalary | `erp_hr_salary.csv` | 1~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P |
| ErpHrSalaryItem | `erp_hr_salary_item.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpHrShift | `erp_hr_shift.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpHrShiftAssignment | `erp_hr_shift_assignment.csv` | 1~2/头 | ErpHrShift（子表） | ErpHrEmployee〔已seed〕、ErpHrShift〔本批〕 | P+N-TERM |
| ErpHrShiftRotationPattern | `erp_hr_shift_rotation_pattern.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpHrShiftSwapRequest | `erp_hr_shift_swap_request.csv` | 2~3 | —（独立表） | ErpHrEmployee〔已seed〕、ErpHrShiftAssignment〔本批〕 | P+N-TERM |
| ErpHrSocialInsuranceBase | `erp_hr_social_insurance_base.csv` | 1~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P |
| ErpHrSocialInsuranceConfig | `erp_hr_social_insurance_config.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpHrSurvey | `erp_hr_survey.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpHrSurveyAnswer | `erp_hr_survey_answer.csv` | 1~3 | —（独立表） | ErpHrSurveyResponse〔本批〕、ErpHrSurveyQuestion〔本批〕 | P |
| ErpHrSurveyQuestion | `erp_hr_survey_question.csv` | 1~2/头 | ErpHrSurvey（子表） | ErpHrSurvey〔本批〕 | P |
| ErpHrSurveyResponse | `erp_hr_survey_response.csv` | 1~2/头 | ErpHrSurvey（子表） | ErpHrSurvey〔本批〕 | P |
| ErpHrSurveyResult | `erp_hr_survey_result.csv` | 1~2/头 | ErpHrSurvey（子表） | ErpHrSurvey〔本批〕 | P |
| ErpHrTaxConfig | `erp_hr_tax_config.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpHrTaxSpecialDeduction | `erp_hr_tax_special_deduction.csv` | 1~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P |
| ErpHrTimesheet | `erp_hr_timesheet.csv` | 2~3 | —（独立表） | ErpHrEmployee〔已seed〕 | P+N-TERM |
| ErpHrTimesheetLine | `erp_hr_timesheet_line.csv` | 1~2/头 | ErpHrTimesheet（子表） | ErpHrTimesheet〔本批〕、ErpHrEmployee〔已seed〕 | P |

#### crm（30 缺 → M1.x 工作项 **M1.4a**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpCrmActivity | `erp_crm_activity.csv` | 1~3 | —（独立表） | ErpCrmLead〔已seed〕 | P |
| ErpCrmBundlePricing | `erp_crm_bundle_pricing.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCrmBundlePricingLine | `erp_crm_bundle_pricing_line.csv` | 1~2/头 | ErpCrmBundlePricing（子表） | ErpCrmBundlePricing〔本批〕 | P |
| ErpCrmCampaign | `erp_crm_campaign.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmConfigRule | `erp_crm_config_rule.csv` | 1~3 | —（独立表） | ErpCrmProductConfigurator〔本批〕 | P |
| ErpCrmEvent | `erp_crm_event.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpCrmEventCategory | `erp_crm_event_category.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmForecastAccuracy | `erp_crm_forecast_accuracy.csv` | 1~3 | —（独立表） | ErpCrmForecastPeriod〔已seed〕 | P |
| ErpCrmFunnelStageMetrics | `erp_crm_funnel_stage_metrics.csv` | 1~3 | —（独立表） | ErpCrmLeadFunnel〔本批〕、ErpCrmStage〔已seed〕 | P |
| ErpCrmLeadConvLog | `erp_crm_lead_conv_log.csv` | 1~3 | —（独立表） | ErpCrmLead〔已seed〕 | P |
| ErpCrmLeadFunnel | `erp_crm_lead_funnel.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmLeadScore | `erp_crm_lead_score.csv` | 1~3 | —（独立表） | ErpCrmLead〔已seed〕 | P |
| ErpCrmLeadScoreConfig | `erp_crm_lead_score_config.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCrmLeadScoreConfigLine | `erp_crm_lead_score_config_line.csv` | 1~2/头 | ErpCrmLeadScoreConfig（子表） | ErpCrmLeadScoreConfig〔本批〕 | P |
| ErpCrmLeadScoreLine | `erp_crm_lead_score_line.csv` | 1~2/头 | ErpCrmLeadScore（子表） | ErpCrmLeadScore〔本批〕 | P |
| ErpCrmLeadSequenceProgress | `erp_crm_lead_seq_progress.csv` | 2~3 | —（独立表） | ErpCrmLead〔已seed〕、ErpCrmSequence〔本批〕 | P+N-TERM |
| ErpCrmLeadStatus | `erp_crm_lead_status.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmLostReason | `erp_crm_lost_reason.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmPriceRule | `erp_crm_price_rule.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCrmProductConfigurator | `erp_crm_product_configurator.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCrmQuota | `erp_crm_quota.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmQuoteTemplate | `erp_crm_quote_template.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmSequence | `erp_crm_sequence.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCrmSequenceAssignment | `erp_crm_sequence_assignment.csv` | 1~2/头 | ErpCrmSequence（子表） | ErpCrmSequence〔本批〕 | P+N-DIS |
| ErpCrmSequenceStep | `erp_crm_sequence_step.csv` | 1~2/头 | ErpCrmSequence（子表） | ErpCrmSequence〔本批〕 | P |
| ErpCrmSource | `erp_crm_source.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmTeam | `erp_crm_team.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCrmTeamMember | `erp_crm_team_member.csv` | 1~2/头 | ErpCrmTeam（子表） | ErpCrmTeam〔本批〕 | P |
| ErpCrmTerritory | `erp_crm_territory.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCrmTerritoryAssignmentRule | `erp_crm_territory_assignment_rule.csv` | 1~2/头 | ErpCrmTerritory（子表） | ErpCrmTerritory〔本批〕 | P+N-DIS |

#### cs（15 缺 → M1.x 工作项 **M1.4a**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpCsAgentRate | `erp_cs_agent_rate.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCsCannedCategory | `erp_cs_canned_category.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCsCannedResponse | `erp_cs_canned_response.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCsCatalogCategory | `erp_cs_catalog_category.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCsCatalogFulfillment | `erp_cs_catalog_fulfillment.csv` | 1~3 | —（独立表） | ErpCsServiceCatalogItem〔本批〕 | P |
| ErpCsContract | `erp_cs_contract.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpCsEntitlement | `erp_cs_entitlement.csv` | 1~2 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕 | P+N-DIS |
| ErpCsKnowledgeBase | `erp_cs_knowledge_base.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCsServiceCatalogItem | `erp_cs_service_catalog_item.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCsSlaPolicy | `erp_cs_sla_policy.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCsTeam | `erp_cs_team.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCsTicketAction | `erp_cs_ticket_action.csv` | 1~3 | —（独立表） | ErpCsTicket〔已seed〕 | P |
| ErpCsTicketFulfillmentStep | `erp_cs_ticket_fulfillment_step.csv` | 2~3 | —（独立表） | ErpCsTicket〔已seed〕、ErpCsCatalogFulfillment〔本批〕 | P+N-TERM |
| ErpCsTicketTimerSession | `erp_cs_ticket_timer_session.csv` | 2~3 | —（独立表） | ErpCsTicket〔已seed〕 | P+N-TERM |
| ErpCsTimeEntry | `erp_cs_time_entry.csv` | 1~3 | —（独立表） | ErpCsTicket〔已seed〕 | P |

#### aps（7 缺 → M1.x 工作项 **M1.4b**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpApsCapacityReservation | `erp_aps_capacity_reservation.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpApsConstraint | `erp_aps_constraint.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpApsDispatchLog | `erp_aps_dispatch_log.csv` | 1~3 | —（独立表） | ErpApsOperationOrder〔本批〕 | P |
| ErpApsDispatchRule | `erp_aps_dispatch_rule.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpApsOpRouting | `erp_aps_op_routing.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpApsOperationOrder | `erp_aps_operation_order.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpApsSchedule | `erp_aps_schedule.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |

#### logistics（8 缺 → M1.x 工作项 **M1.4b**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpLogCarrier | `erp_log_carrier.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpLogCarrierConfig | `erp_log_carrier_config.csv` | 1~2/头 | ErpLogCarrier（子表） | ErpLogCarrier〔本批〕 | P+N-DIS |
| ErpLogDeliveryBooking | `erp_log_delivery_booking.csv` | 2~3 | —（独立表） | ErpLogShipment〔本批〕、ErpLogDeliveryWindow〔本批〕 | P+N-TERM |
| ErpLogDeliveryWindow | `erp_log_delivery_window.csv` | 1~2 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕 | P+N-DIS |
| ErpLogShipment | `erp_log_shipment.csv` | 2~3 | —（独立表） | ErpLogCarrier〔本批〕 | P+N-TERM |
| ErpLogShipmentLine | `erp_log_shipment_line.csv` | 1~2/头 | ErpLogShipment（子表） | ErpLogShipment〔本批〕 | P |
| ErpLogShipmentLog | `erp_log_shipment_log.csv` | 1~2/头 | ErpLogShipment（子表） | ErpLogShipment〔本批〕 | P |
| ErpLogShipmentParcel | `erp_log_shipment_parcel.csv` | 1~2/头 | ErpLogShipment（子表） | ErpLogShipment〔本批〕 | P+N-DIS |

#### b2b（13 缺 → M1.x 工作项 **M1.5**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpB2bAsn | `erp_b2b_asn.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpB2bAsnLine | `erp_b2b_asn_line.csv` | 1~2/头 | ErpB2bAsn（子表） | ErpB2bAsn〔本批〕 | P |
| ErpB2bCertificationChecklist | `erp_b2b_certification_checklist.csv` | 1~3 | —（独立表） | ErpB2bPartnerProfile〔本批〕 | P |
| ErpB2bCodeMapping | `erp_b2b_code_mapping.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpB2bEdiDoc | `erp_b2b_edi_doc.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpB2bEdiFormat | `erp_b2b_edi_format.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpB2bEdiLog | `erp_b2b_edi_log.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpB2bMftCertificate | `erp_b2b_mft_certificate.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpB2bMftConfig | `erp_b2b_mft_config.csv` | 1~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕 | P |
| ErpB2bMftLog | `erp_b2b_mft_log.csv` | 2~3 | —（独立表） | ErpB2bMftConfig〔本批〕 | P+N-TERM |
| ErpB2bPartnerCredential | `erp_b2b_partner_credential.csv` | 1~2 | —（独立表） | ErpB2bPartnerProfile〔本批〕 | P+N-DIS |
| ErpB2bPartnerProfile | `erp_b2b_partner_profile.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpB2bTestExchange | `erp_b2b_test_exchange.csv` | 1~3 | —（独立表） | ErpB2bPartnerProfile〔本批〕 | P |

#### contract（15 缺 → M1.x 工作项 **M1.5**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpCtApprovalMatrix | `erp_ct_approval_matrix.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCtApprovalRecord | `erp_ct_approval_record.csv` | 1~3 | —（独立表） | ErpCtContract〔本批〕 | P |
| ErpCtConsumptionLine | `erp_ct_consumption_line.csv` | 1~3 | —（独立表） | ErpCtContractLine〔本批〕 | P |
| ErpCtContract | `erp_ct_contract.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕 | P+N-TERM |
| ErpCtContractLine | `erp_ct_contract_line.csv` | 1~2/头 | ErpCtContract（子表） | ErpCtContract〔本批〕 | P |
| ErpCtContractVersion | `erp_ct_contract_version.csv` | 1~2/头 | ErpCtContract（子表） | ErpCtContract〔本批〕 | P+N-TERM |
| ErpCtDocument | `erp_ct_document.csv` | 1~3 | —（独立表） | —（无必填 FK） | P |
| ErpCtInvoicePlan | `erp_ct_invoice_plan.csv` | 1~3 | —（独立表） | ErpCtContractLine〔本批〕 | P |
| ErpCtRebateAccrual | `erp_ct_rebate_accrual.csv` | 1~3 | —（独立表） | ErpCtRebateAgreement〔本批〕 | P |
| ErpCtRebateAgreement | `erp_ct_rebate_agreement.csv` | 2~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕 | P+N-TERM |
| ErpCtRebateSettlement | `erp_ct_rebate_settlement.csv` | 2~3 | —（独立表） | ErpCtRebateAgreement〔本批〕 | P+N-TERM |
| ErpCtRebateTier | `erp_ct_rebate_tier.csv` | 1~3 | —（独立表） | ErpCtRebateAgreement〔本批〕 | P |
| ErpCtSignatureRequest | `erp_ct_signature_request.csv` | 2~3 | —（独立表） | ErpCtContractVersion〔本批〕 | P+N-TERM |
| ErpCtTemplate | `erp_ct_template.csv` | 1~2 | —（独立表） | —（无必填 FK） | P+N-DIS |
| ErpCtVolumeDiscount | `erp_ct_volume_discount.csv` | 1~3 | —（独立表） | ErpCtContractLine〔本批〕 | P |

#### drp（11 缺 → M1.x 工作项 **M1.5**）

| 实体 | 建议 CSV | 建议行数 | 主子表组 | 必填 FK 闭环依赖 | 用例指示 |
|---|---|---|---|---|---|
| ErpDrpLine | `erp_drp_line.csv` | 2~3 | —（独立表） | ErpDrpPlan〔本批〕、ErpMdMaterial〔跨域:md·已seed〕、ErpMdWarehouse〔跨域:md·已seed〕 | P+N-TERM |
| ErpDrpParameter | `erp_drp_parameter.csv` | 1~3 | —（独立表） | ErpMdWarehouse〔跨域:md·已seed〕、ErpMdMaterial〔跨域:md·已seed〕 | P |
| ErpDrpPlan | `erp_drp_plan.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpDrpScenario | `erp_drp_scenario.csv` | 2~3 | —（独立表） | —（无必填 FK） | P+N-TERM |
| ErpDrpScenarioParam | `erp_drp_scenario_param.csv` | 1~2/头 | ErpDrpScenario（子表） | ErpDrpScenario〔本批〕 | P |
| ErpDrpScenarioVersion | `erp_drp_scenario_version.csv` | 1~2/头 | ErpDrpScenario（子表） | ErpDrpScenario〔本批〕 | P+N-TERM |
| ErpInvDrpCrossDock | `erp_inv_drp_cross_dock.csv` | 2~3 | —（独立表） | ErpMdMaterial〔跨域:md·已seed〕 | P+N-TERM |
| ErpInvDrpDockAppointment | `erp_inv_drp_dock_appointment.csv` | 2~3 | —（独立表） | ErpMdWarehouse〔跨域:md·已seed〕、ErpInvDrpCrossDock〔本批〕 | P+N-TERM |
| ErpInvDrpLeadTimeRecord | `erp_inv_drp_lead_time_record.csv` | 1~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdMaterial〔跨域:md·已seed〕 | P |
| ErpInvDrpSafetyStockCalc | `erp_inv_drp_safety_stock_calc.csv` | 1~3 | —（独立表） | ErpMdMaterial〔跨域:md·已seed〕 | P |
| ErpInvDrpSupplierScore | `erp_inv_drp_supplier_score.csv` | 1~3 | —（独立表） | ErpMdPartner〔跨域:md·已seed〕、ErpMdMaterial〔跨域:md·已seed〕 | P |

