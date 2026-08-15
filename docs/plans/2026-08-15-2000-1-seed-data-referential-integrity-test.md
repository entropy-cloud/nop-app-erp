# 2026-08-15-2000-1-seed-data-referential-integrity-test 通用种子数据引用完整性测试（全表可加载 + 非空关联键指向合法数据）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Source: 用户请求（2026-08-15）——此前曾要求编写"枚举所有表、经 entityModel 获取全部关联对象、校验所有非空关联键指向合法数据"的通用用例；经实时仓库 + nop-entropy 双仓核实：**当前不存在该用例**（最接近的 `TestAuthSeedLoadingProof` 仅覆盖 auth 表 3 项断言）
> Related: `docs/plans/2026-07-08-1234-1-demo-seed-data-init.md`（seed 机制落地）、`2026-07-08-1445-1-p2p-o2c-transaction-seed-data.md`（交易种子）、`2026-08-09-2107-1-auth-table-csv-seeds.md`（auth 种子）、`docs/architecture/seed-data.md`（seed 真相源文档）、`app-erp-all/src/test/java/io/nop/app/all/auth/TestAuthSeedLoadingProof.java`（宿主模式先例）
> Audit: required

## Current Baseline（执行期实证追加）

- **执行期新增发现（先决阻塞解除）**：宿主先例 `TestAuthSeedLoadingProof` 在 2026-08-13 后因 nop-entropy `e5ee02b40`（2026-08-11）回归红测（`OrmTransactionListener.onBeforeCommit` NPE——`nopDefaultTransactionListener.ormTemplate` 改 `ioc:lazy-property` 后，`DataInitInitializer.executeSqlFiles` 在 bean 创建期提交事务时 lazy 属性未赋值）。本计划 Phase 3 退出标准要求该宿主测试全绿，故执行期**先决修复两个已知回归**（均已在 `docs/bugs/` 登记）：
  - **nop-entropy 修复（外部仓库，auto + dual-agent-approval）**：`OrmTransactionListener.onBeforeCommit/onAfterCompletion` 增 null-guard（bean 创建窗口期事务为纯 JDBC 原始 SQL，无 ORM session 绑定，跳过 flush 与已装配后行为等价；容器启动后 lazy 属性必已赋值，正常语义不变）。**双独立子代理批准已落盘（见「Cross-Repo Fix Approvals」节）**。修复后 `TestAuthSeedLoadingProof` 3/0/0 绿。
  - **mfg view.xml 修复（app 层，bug `2026-08-14-0930-mfg-...` 方案 A）**：`ErpMfgCostRollupLine.view.xml` 4 档位 grid cols + form cells 增 `custom="true"`（对齐 ErpMfgSubcontractOrder/ErpMfgWorkOrder 先例）——E4.1（`452e418d0`）代理字段 cell-not-prop 页面验证回归。修复后容器 init 全通过。
- **执行期实证（Phase 1 Explore，宿主初始化模式跑通 95 CSV 全量加载）**：
  - `IDaoProvider.getEntityNames()` 全量 **418 实体** = `app.erp.*` 352 + 平台 66（含 `NopAuthUser`/`NopSysDict` 等）；全实体 `findAll()` 零异常。
  - **非主键 join 全集 = 0**（全仓 1057 app to-one + 平台 to-one 全部 `rightProp` = ref 实体主键列；运行时 `isJoinOnNonPkColumn()` 零命中）。
  - **悬空引用初扫 = 0**：722 个非空 FK 值全部指向合法数据；`spc_chart.parameterId` 无 to-one（天然跳过）、`crp_load.workOrderId=1→WO-2026-001` 存在、`erp_mnt_*.csv` equipmentId 跨域引用全部合法。
  - 白名单豁免表 = 空（当前无弱指针豁免项；机制保留供未来 seed 追加）。

## Cross-Repo Fix Approvals（nop-entropy `OrmTransactionListener` null-guard，auto + dual-agent-approval）

- **Fix**：`/Users/abc/app/nop-entropy/nop-persistence/nop-orm/src/main/java/io/nop/orm/txn/OrmTransactionListener.java`——`onBeforeCommit` 与 `onAfterCompletion` 增 `ormTemplate == null` 守卫；beans.xml 的 `ioc:lazy-property` 循环依赖解耦保持不变。触发背景：`docs/bugs/2026-08-14-0930-authseed-loading-npe-ormtransactionlistener.md`。
- **Subagent 1（独立 fresh session，批准）**：`ses_ffa1d76e4ffekJHb02D8qHPnuL` —— 守卫最小且正确；root cause 复核属实（`@PostConstruct` 在 `startBean` 循环内执行，`isStarted()` 为 false，lazy prop 由容器级 `runLazyProperties()` 在全部 bean 创建后赋值）；init 窗口内事务为 raw JDBC 无 ORM session，`flushSession` 无 session 时本身是 no-op，跳过等价；启动后语义不变；无其他受影响调用方；循环解耦保留。Approved。
- **Subagent 2（独立 fresh session，批准）**：`ses_ffa1d6174ffeJbtQmJSLocXXp3` —— 守卫在位且 diff 精确；失败路径复核属实（`DataInitInitializer.java:122` `runInTransaction` 提交触发监听器）；窗口期仅 `executeSqlFiles` 提交事务（DataBaseSchemaInitializer/AddTenantColInitializer 用直连 execute，不触发监听器）；无数据丢失风险（理论第三方 @PostConstruct 带脏 session 提交属反模式且修复前是崩溃而非正确 flush）；备选方案（eager 注入/ delay-method）更激进。Approved。
- **验证**：`./mvnw install -pl nop-persistence/nop-orm -am -DskipTests`（.m2 jar 重建）→ `mvn test -pl app-erp-all -Dtest=TestAuthSeedLoadingProof` 3/0/0 绿。

## Current Baseline（计划原始基线，保留备查）

- **缺口（实时核实）**：当前项目 336+ 自有实体 + 平台实体**无任何通用引用完整性测试**。全部测试类搜索（`daoProvider`/`getEntityModels`/`getRelationModels`/`EntityModel`）零命中通用遍历用例；nop-entropy 亦无（仅 `TestCaseJsonDataSplitter` 用 `daoForTable` 做快照拆分，非校验）。`DataInitInitializer` 仅按拓扑序插入，**不做引用校验**——悬空引用不会被加载期捕获。
- **已有宿主先例**：`TestAuthSeedLoadingProof`（`app-erp-all/src/test/java/io/nop/app/all/auth/`）——`BaseTestCase` + 手动 `CoreInitialization.initialize()` + `setTestConfig("nop.orm.init-database-schema/data", true)` + 文件型 H2 fresh-DB 清理（`db/erp.mv.db`）+ `daoProvider.daoForTable()` 全量加载。**该注释明确记载**：不用 `JunitBaseTestCase` 因 NopJunitExtension ALL_LAZY 模式下 `DataBaseSchemaInitializer` 的 `@PostConstruct` 不先于 DB 访问 bean 运行（pre-existing 仓库行为）——新测试类沿用此模式避免踩坑。
- **seed 数据面**：95 CSV + 1 SQL（`app-erp-all/src/main/resources/_vfs/_init-data/`，覆盖 18+1 域 + auth 表），`zz-sequence-advance.sql` 最后执行。含**已知弱指针风险点**（Explore 必须验证）：
  - `erp_qa_spc_chart.csv` parameterId=0——**ORM 无 `<to-one>` 无目标实体**（自由 BIGINT），关联校验天然跳过，但需确认无 to-one；
  - `erp_mfg_crp_load.csv` workOrderId=1→`WO-2026-001` 弱指针（注释明示，work_order.csv 含该 code），需核实 work_order 表存在该行；
  - equipmentId 跨域引用位于 `erp_mnt_*.csv`（downtime_entry/request/schedule/spare_part_usage/visit，指向 mfg equipment），需全量核实；workcenter_calendar/crp_load 无 EQUIPMENT_ID 列。
- **平台 API（已实证存在）**：
  - `IDaoProvider.getEntityNames()` + `dao(String entityName)` / `daoForTable(String tableName)` → `IEntityDao<T>`（`io.nop.dao.api.IDaoProvider`）;
  - `IOrmEntityDao.getEntityModel()` → `IEntityModel`（`io.nop.orm.dao.IOrmEntityDao`）;
  - `IEntityModel.getRelations()` → `List<IEntityRelationModel>`，`getToOneRelations()` / `getToManyRelations()` 默认过滤;
  - `IEntityRelationModel.getJoin()` → `List<IEntityJoinConditionModel>`（`getLeftProp()`/`getRightProp()`），`getRefEntityModel()`，`isJoinOnNonPkColumn()`（非主键 join 需按 refProp 查询而非主键 getById），`getKind().isToOneRelation()`；
  - `IEntityDao.findAll()`（逻辑删除自动过滤 del_version=0）、`getEntityById(id)`。
- **ORM 面**：全仓 1057 个 `<to-one>`（跨域引用含 notGenCode 外部实体，如 drp→master-data），全为主键 join（`leftProp=xxxId rightProp=id` 模式），极少数非主键 join 需 Explore 定位。
- **测试基线与验证**：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS 基线 + E2E 套件经 seed 库运行（`docs/testing/known-good-baselines.md` 2026-08-05 条目）；单测 ~2000+。

## Goals

- **通用测试类落地**：`TestErpSeedDataIntegrity`（宿主 `app-erp-all/src/test/java/io/nop/app/all/seed/`，沿用 TestAuthSeedLoadingProof 手动初始化模式）实现两个通用校验：
  1. **全表可加载**：枚举 `IDaoProvider.getEntityNames()`（含平台实体），逐实体 `findAll()` 不抛异常且返回非空结果（有 seed 的表行数 > 0）；
  2. **非空关联键指向合法数据**：逐实体经 `getEntityModel().getRelations()` 取全部 **to-one** 关系，逐行取 join leftProp 值，非空时校验 refEntity 主键存在（refEntity 主键集一次性加载为 Set，内存比对）；`isJoinOnNonPkColumn()` 的关系按 refProp 值查询 refEntity 对应列（Explore 裁决具体实现）；to-many 为反向关系不重复校验（对端 to-one 已覆盖）。
- **悬空引用裁决闭环**：Explore 阶段产出全部悬空引用清单，逐项分类——(a) 真实缺口 → Fix seed 或修复 ORM 关联；(b) 弱指针/占位值（如 parameterId=0 无 to-one）→ 测试白名单豁免并登记 owner doc `seed-data.md` 注记。
- **回归可重复**：测试可独立重复运行（fresh-DB 每次重建），加入全仓验证命令。

## Non-Goals

- **不修改 seed CSV 数据本身**（除非 Explore 证实真实悬空引用——属 Fix，经本计划记录理由后最小化修改，遵守「种子只能追加」纪律）。
- **不修改 `model/*.orm.xml`**（ORM 保护区域 auto + dual-agent-approval——双独立子 agent 分别批准；若 Explore 发现 ORM 关联缺失/错误属越界，须经双独立子 agent 批准或登记 successor，不静默修改）。
- **不校验业务语义**（如状态机合法性、金额平衡）——仅引用完整性 + 可加载性。
- **不校验 to-many 反向关联**（对端 to-one 校验已覆盖单向一致性）。
- **不做性能基线**（seed 数据量小，全量内存比对即可）。
- **不接入 CI**（对齐 1234-1 Deferred O-14，CI 接入归 successor）。

## Task Route

- Type: `verification or audit work`（新增通用测试用例 + 引用完整性审计，纯测试层 + 可能的种子 Fix）
- Owner Docs: `docs/architecture/seed-data.md`（seed 真相源，实现注记落此）
- Skill Selection Basis: `nop-testing`（测试基类选择、@NopTestConfig、种子纪律、清理协议——本计划全部适用）；宿主模式 `TestAuthSeedLoadingProof` 为既有先例直接镜像。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新基础设施。测试沿用既有文件型 H2 + `nop.orm.init-database-schema/data=true` + `/_init-data/` 位置配置（`TestAuthSeedLoadingProof` 已实证）。
- 验证命令：`mvn clean install -DskipTests`（全仓）→ `mvn test -pl app-erp-all`（新测试类 + 回归）。

## Execution Plan

### Phase 1 - Explore：平台 API 实证 + 全量 seed 加载 + 悬空引用初扫

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/`（新测试类初稿）、`docs/architecture/seed-data.md`（注记）
Skill: `nop-testing`

- Item Types: `Explore | Decision | Proof | Add`（Proof-heavy）
- Prereqs: 无

- [x] `Explore`：镜像 `TestAuthSeedLoadingProof` 初始化模式跑通全量 seed 加载（95 CSV），实证 `getEntityNames()` 枚举范围（平台实体如 `NopAuthUser`/`NopSysDict` 是否含入、app.erp 前缀实体全集计数）
      - Skill: `nop-testing`
      - Evidence: `TestErpSeedIntegrityExplore` 探针测试（临时，跑通后转正为 `TestErpSeedDataIntegrity`）——`getEntityNames()` 全量 418 = app.erp.* 352 + 平台 66（含 NopAuthUser/NopSysDict 等）；全实体 `findAll()` 零异常。95 CSV 全量加载在宿主修复（见「Current Baseline 执行期新增发现」）后跑通。
- [x] `Proof`：实体引用面盘点——全仓 1057 `<to-one>` 中定位非主键 join 关系（`isJoinOnNonPkColumn`）全集；确认 `erp_qa_spc_chart.parameterId` 无 to-one（天然跳过）；确认 `erp_mfg_crp_load.workOrderId` 指向 `WO-2026-001` 存在性
      - Skill: `nop-testing`
      - Evidence: (1) 静态盘点全仓 19 ORM XML 全部 1057 `<to-one>` join 均为 `rightProp="id"`（ref 实体主键）；平台 auth/sys ORM 的 to-one 亦全部 join 到 ref 主键列（userId/deptId/sid 等）——**运行时 `isJoinOnNonPkColumn()` 全量 0 命中**；(2) `erp_qa_spc_chart.csv` parameterId=0 且 ORM 无 `<to-one>` 无目标实体（quality ORM 仅 BIGINT 列）→ 天然跳过；(3) `erp_mfg_crp_load.csv` workOrderId=1 ↔ `erp_mfg_work_order.csv` ID=1 `WO-2026-001` 存在；workcenterId=1 ↔ workcenter.csv `WC-001` 存在；`erp_mnt_*.csv` equipmentId 跨域引用全量核实合法。
- [x] `Decision`：**枚举范围裁决**——(a) 全量含平台实体（推荐：auth 种子 user_role→user 也应校验，风险=平台实体引用系统表）vs (b) 仅 `app.erp.*` 前缀；记录替代方案与残留风险（若选 (a) 且平台实体引用平台系统表存在悬空，白名单豁免）
      - Skill: `nop-testing`
      - **裁决：(a) 全量 418 实体**。实证：全量遍历 722 个非空 FK 值零悬空（含平台实体，auth 种子 user_role→user/role 绑定均合法）；风险（平台实体引用未 seed 系统表）实证未发生。替代 (b) 拒绝——遗漏 auth 种子 user_role→user 校验面且无收益。
- [x] `Decision`：**非主键 join 校验实现**——(a) 按 refProp 值经 refEntity 列查询（语义精确）vs (b) 非主键 join 一律白名单跳过（保守）；记录替代方案
      - Skill: `nop-testing`
      - **裁决：(a) 语义精确查询**。实证：全仓运行时 `isJoinOnNonPkColumn()` 零命中（无实际触发），实现保留按 refProp 查询分支以对未来模型变更免疫；替代 (b) 拒绝——零命中场景下两者等价，但 (a) 不埋「非主键 join 被静默跳过」的未来陷阱。
- [x] `Proof`：初扫全量悬空引用清单（真实缺口 vs 弱指针分类），落盘计划 Closure 证据 + `seed-data.md` 注记
      - Skill: `nop-testing`
      - Evidence: 初扫 722 非空 FK 全合法，**悬空引用 = 0**（无真实缺口、无弱指针豁免需求）。`spc_chart.parameterId=0` 无 to-one 天然跳过；`crp_load.workOrderId=1` 为合法指针（非悬空）。`seed-data.md` 注记已落（Phase 3）。

Exit Criteria:

- [x] 全量 seed 可加载跑通，实体枚举范围与计数确定
- [x] 非主键 join 全集清单 + spc_chart/crp_load 两弱指针点结论明确
- [x] 悬空引用分类清单（Fix 候选 vs 白名单豁免候选）产出

### Phase 2 - 实现：通用引用完整性测试类

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 1 全部 Decision 已决

- [x] `Add`：`TestErpSeedDataIntegrity`（BaseTestCase + 手动 CoreInitialization，镜像 TestAuthSeedLoadingProof 初始化块）——`testAllSeedTablesLoadable()`（枚举全部实体 findAll 非空）+ `testNonNullRelationKeysPointToExistingRows()`（to-one 全量遍历，refEntity 主键集 Set 比对；白名单三元组 (ownerEntity, relationName, key) 豁免机制常量表；非主键 join 按 Phase 1 Decision 实现）
      - Skill: `nop-testing`
      - Evidence: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`——`testAllSeedTablesLoadable`（getEntityNames 全量 418 枚举 + findAll 零异常 + 存在 seed CSV 的表行数>0，CSV 查找镜像 `DataInitInitializer.loadCsvData` 逻辑）+ `testNonNullRelationKeysPointToExistingRows`（to-one 全量遍历；主键 join 用 refEntity 主键集 Set 内存比对；`isJoinOnNonPkColumn` 分支按 refProp 值 `existsByQuery` 查询（Phase 1 Decision (a)）；`isDynamicJoin`/复合 join 防御性跳过并记 failure（实证 0 命中））。`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 2/0/0 绿。
- [x] `Add`：白名单常量表（Phase 1 裁决的弱指针/占位豁免，每项注释引用证据来源）
      - Skill: `nop-testing`
      - Evidence: `WHITELIST_KEYS` 常量表 + `whitelistKey(ownerEntity, relationName, key)` 三元组机制 + Javadoc 注明豁免证据来源义务。**Phase 1 实证 722 非空 FK 零悬空 → 当前表为空**（无豁免项；机制保留供未来 seed 追加登记）。
- [x] `Fix`（条件触发）：Explore 证实为真实悬空引用的 seed 缺口——最小化修正（追加缺失行优先，遵守种子追加纪律），每处记录理由
      - Skill: `nop-testing`
      - **未触发**：Phase 1 初扫零悬空引用，无 seed 缺口需 Fix。
      - 注：执行期另有**两个已知回归先决修复**（非本计划引入，见「Current Baseline 执行期新增发现」与「Cross-Repo Fix Approvals」）：nop-entropy `OrmTransactionListener` NPE null-guard（外部仓库，双独立子代理批准）+ mfg `ErpMfgCostRollupLine.view.xml` 档位 cells custom="true"（bug `2026-08-14-0930-mfg-...` 方案 A）。二者是宿主 `TestAuthSeedLoadingProof` 与容器 init 绿化的必要条件。

Exit Criteria:

- [x] 新测试类两测试方法全绿（全量表可加载 + 零未豁免悬空引用）
- [x] 白名单豁免表仅含 Phase 1 裁决项

### Phase 3 - 验证与文档对齐

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/logs/`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] `Proof`：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS + `mvn test -pl app-erp-all` 新测试 + 既有回归全绿（重点 TestAuthSeedLoadingProof、TestErpAllJobYamlLoading、TestModuleMetaReader）
      - Skill: `nop-testing`
      - Evidence: `mvn clean install -DskipTests` 全仓 BUILD SUCCESS（156 reactor 模块）；`mvn test -pl app-erp-all` **28/28 全绿**（1 skipped = 既有 `@Disabled` ErpAllWebPagesCollectTest）——`TestErpSeedDataIntegrity` 2/0/0 + `TestAuthSeedLoadingProof` 3/0/0 + `TestErpAllJobYamlLoading` 1/0/0 + `TestModuleMetaReader` 7/0/0 + `ErpAllFluxPagesTest`/`ErpAllWebPagesTest` 0 errors（两个 known-good-baselines 2026-08-14 已知失败均已解除：NPE 平台回归 + mfg 页面回归）；全 reactor `mvn test` BUILD SUCCESS（`TestErpMdSkuServices` 单次 reactor 运行时 9 unknown-operation 为既有跨模块隔离 flake——plan `2026-08-13-0810-1` 已登记，独立运行与复跑全绿，与本计划无关）。
      - 注：`mvn test` 在 `module-master-data/erp-md-service` 出现一次 `TestErpMdSkuServices` 9 错误（`nop.err.graphql.unknown-operation`）；经核实为 plan `2026-08-13-0810-1` 已记录的既有 reactor 跨模块隔离/时序问题（零触碰 master-data，独立运行 9/9 全绿），非本计划引入；复跑全 reactor 全绿。
- [x] `Add`：`seed-data.md` 增「引用完整性校验」注记（测试类位置 + 覆盖范围 + 白名单机制 + 后续 seed 追加须过此测试的义务）；`docs/logs/2026/08-15.md` 日志条目
      - Skill: `none`
      - Evidence: `docs/architecture/seed-data.md` 头部新增「通用引用完整性校验已落地」（2026-08-15）注记段（测试类位置/覆盖范围 418 实体·722 FK 零悬空/白名单机制/后续 seed 追加义务/执行期两先决修复引用）；`docs/logs/2026/08-15.md` 顶部新增本计划日志条目（含验证基线全绿记录）。

Exit Criteria:

- [x] 全仓构建 + app-erp-all 测试全绿
- [x] owner doc + 日志更新完成

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is`（mission-driver review `2026-08-14-070716-mission-driver`）— 0 Blocker / 0 Major / 3 Minor（全部已修订）。实时仓库零信任核实全部 load-bearing 主张属实：`TestAuthSeedLoadingProof` 存在且模式如实（BaseTestCase + 手动 CoreInitialization + setTestConfig + 文件型 H2 fresh-DB 清理）；`_init-data/` 恰 95 CSV + 1 SQL；全仓 `<to-one>` 计数恰 1057；`erp_qa_spc_chart.csv` parameterId=0（REMARK 证实无 to-one）；`erp_mfg_crp_load.csv` workOrderId=1 弱指针且 `WO-2026-001` 存在；`known-good-baselines.md` 2026-08-05 条目存在；related 三计划 + `seed-data.md` 均在位。3 Minor 已修订：(1) 基线 equipmentId 归属订正——EQUIPMENT_ID 仅存于 `erp_mnt_*.csv`（5 文件），workcenter_calendar/crp_load 无此列；(2) Phase 1 Exit Criteria 预勾 `[x]` 订正为 `[ ]`（text consistency，规则 10）；(3) Phase 1 Item Types 声明补 `Explore`（规则 9 允许临时 Explore 项，声明与项匹配）。模板合规 pass（规则 1/2/4/7/8/9/14 + anti-slack + 命名规范）。草案可接受执行 → `Plan Status: active`。

## Closure Gates

- [x] 范围内行为完成（两测试方法全绿 + 白名单闭环）
- [x] 相关文档对齐（seed-data.md 注记 + 日志）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl app-erp-all`）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CI 自动接线

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划目标是测试用例落地 + 一次性裁决闭环；CI 持续门控属部署/CI 范围（1234-1 Deferred O-14 同型），且 fresh-DB + 全量 seed 加载在 CI 运行成本需单独评估
- Successor Required: `yes`（触发条件：本测试连续通过且 CI 已有 seed 装载机制时）

### 未覆盖：业务语义校验（状态机/金额平衡）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 引用完整性是用户请求的唯一范围；业务语义校验由各域既有测试覆盖
- Successor Required: `no`

## Closure

Status Note: 计划全量执行完成——`TestErpSeedDataIntegrity` 两测试方法全绿（全量 418 实体可加载 + 722 非空 FK 零悬空）、白名单豁免表为空（机制保留）、owner doc + 日志更新完成、全仓构建 + app-erp-all 测试全绿。执行期另解除两个已知回归先决阻塞（nop-entropy `OrmTransactionListener` NPE 平台回归——双独立子代理批准 + mfg `ErpMfgCostRollupLine.view.xml` 档位 cells custom="true"），宿主 `TestAuthSeedLoadingProof` 由红转绿。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure-audit subagent `ses_ff9c18842ffe9Bw2KihXFTvhyW`（fresh session，read-only，审计结论 FAIL（条件性）→ 2 Major 已修复：Closure 段补齐 + nop-entropy `ai-dev/logs/2026/08-15.md` 日志条目落档；Minor-1 重复 `## Current Baseline` 头已合并标注、Minor-2 ProbeDbLock 残留 surefire 报告为构建产物已随后续构建覆盖）。复核证据链全属实：surefire `TestErpSeedDataIntegrity` 2/0/0 + `TestAuthSeedLoadingProof` 3/0/0（同一最终 run 00:23）、app-erp-all 28 执行 + 1 skipped 全绿、nop-entropy null-guard 在位 + .m2 jar 重建（mtime 22:31 > 源码 22:30）、git status 恰 4 修改文件 + 新 seed/ 目录无异常。
- Evidence: 计划各 Phase/Exit Criteria/Closure Gates 全 `[x]`；`mvn clean install -DskipTests` 全仓 BUILD SUCCESS；`mvn test -pl app-erp-all` 28/28 绿（1 skipped 既有 @Disabled）；全 reactor `mvn test` BUILD SUCCESS；`docs/architecture/seed-data.md`「通用引用完整性校验已落地」注记 + `docs/logs/2026/08-15.md` 日志条目 + nop-entropy `ai-dev/logs/2026/08-15.md` 平台修复条目；Cross-Repo Fix Approvals 双独立子代理批准记录。

Follow-up:

- CI 自动接线（Deferred O-14 同型）：本测试连续通过且 CI 已有 seed 装载机制时归 successor（触发条件见「Deferred But Adjudicated」）。
- 未来 `_init-data/*.csv` 追加须保持引用完整性（测试为门禁），弱指针/占位引用在 `WHITELIST_KEYS` 登记并注明证据来源。
- nop-entropy `OrmTransactionListener` null-guard 修复待 nop-entropy 侧随下次平台提交落库（本计划已在 nop-entropy `ai-dev/logs/2026/08-15.md` 记录，未单独 commit 外部仓库）。
