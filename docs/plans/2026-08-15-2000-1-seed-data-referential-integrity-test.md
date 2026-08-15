# 2026-08-15-2000-1-seed-data-referential-integrity-test 通用种子数据引用完整性测试（全表可加载 + 非空关联键指向合法数据）

> Plan Status: draft
> Last Reviewed: 2026-08-15
> Source: 用户请求（2026-08-15）——此前曾要求编写"枚举所有表、经 entityModel 获取全部关联对象、校验所有非空关联键指向合法数据"的通用用例；经实时仓库 + nop-entropy 双仓核实：**当前不存在该用例**（最接近的 `TestAuthSeedLoadingProof` 仅覆盖 auth 表 3 项断言）
> Related: `docs/plans/2026-07-08-1234-1-demo-seed-data-init.md`（seed 机制落地）、`2026-07-08-1445-1-p2p-o2c-transaction-seed-data.md`（交易种子）、`2026-08-09-2107-1-auth-table-csv-seeds.md`（auth 种子）、`docs/architecture/seed-data.md`（seed 真相源文档）、`app-erp-all/src/test/java/io/nop/app/all/auth/TestAuthSeedLoadingProof.java`（宿主模式先例）
> Audit: required

## Current Baseline

- **缺口（实时核实）**：当前项目 336+ 自有实体 + 平台实体**无任何通用引用完整性测试**。全部测试类搜索（`daoProvider`/`getEntityModels`/`getRelationModels`/`EntityModel`）零命中通用遍历用例；nop-entropy 亦无（仅 `TestCaseJsonDataSplitter` 用 `daoForTable` 做快照拆分，非校验）。`DataInitInitializer` 仅按拓扑序插入，**不做引用校验**——悬空引用不会被加载期捕获。
- **已有宿主先例**：`TestAuthSeedLoadingProof`（`app-erp-all/src/test/java/io/nop/app/all/auth/`）——`BaseTestCase` + 手动 `CoreInitialization.initialize()` + `setTestConfig("nop.orm.init-database-schema/data", true)` + 文件型 H2 fresh-DB 清理（`db/erp.mv.db`）+ `daoProvider.daoForTable()` 全量加载。**该注释明确记载**：不用 `JunitBaseTestCase` 因 NopJunitExtension ALL_LAZY 模式下 `DataBaseSchemaInitializer` 的 `@PostConstruct` 不先于 DB 访问 bean 运行（pre-existing 仓库行为）——新测试类沿用此模式避免踩坑。
- **seed 数据面**：95 CSV + 1 SQL（`app-erp-all/src/main/resources/_vfs/_init-data/`，覆盖 18+1 域 + auth 表），`zz-sequence-advance.sql` 最后执行。含**已知弱指针风险点**（Explore 必须验证）：
  - `erp_qa_spc_chart.csv` parameterId=0——**ORM 无 `<to-one>` 无目标实体**（自由 BIGINT），关联校验天然跳过，但需确认无 to-one；
  - `erp_mfg_crp_load.csv` workOrderId→`WO-2026-001` 弱指针（注释明示），需核实 work_order 表存在该行；
  - `erp_mfg_workcenter_calendar.csv`/`erp_mfg_crp_load.csv` 中 equipmentId 等跨域引用需全量核实。
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

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/`（新测试类初稿）、`docs/architecture/seed-data.md`（注记）
Skill: `nop-testing`

- Item Types: `Decision | Proof | Add`（Proof-heavy）
- Prereqs: 无

- [ ] `Explore`：镜像 `TestAuthSeedLoadingProof` 初始化模式跑通全量 seed 加载（95 CSV），实证 `getEntityNames()` 枚举范围（平台实体如 `NopAuthUser`/`NopSysDict` 是否含入、app.erp 前缀实体全集计数）
      - Skill: `nop-testing`
- [ ] `Proof`：实体引用面盘点——全仓 1057 `<to-one>` 中定位非主键 join 关系（`isJoinOnNonPkColumn`）全集；确认 `erp_qa_spc_chart.parameterId` 无 to-one（天然跳过）；确认 `erp_mfg_crp_load.workOrderId` 指向 `WO-2026-001` 存在性
      - Skill: `nop-testing`
- [ ] `Decision`：**枚举范围裁决**——(a) 全量含平台实体（推荐：auth 种子 user_role→user 也应校验，风险=平台实体引用系统表）vs (b) 仅 `app.erp.*` 前缀；记录替代方案与残留风险（若选 (a) 且平台实体引用平台系统表存在悬空，白名单豁免）
      - Skill: `nop-testing`
- [ ] `Decision`：**非主键 join 校验实现**——(a) 按 refProp 值经 refEntity 列查询（语义精确）vs (b) 非主键 join 一律白名单跳过（保守）；记录替代方案
      - Skill: `nop-testing`
- [ ] `Proof`：初扫全量悬空引用清单（真实缺口 vs 弱指针分类），落盘计划 Closure 证据 + `seed-data.md` 注记
      - Skill: `nop-testing`

Exit Criteria:

- [x] 全量 seed 可加载跑通，实体枚举范围与计数确定
- [x] 非主键 join 全集清单 + spc_chart/crp_load 两弱指针点结论明确
- [x] 悬空引用分类清单（Fix 候选 vs 白名单豁免候选）产出

### Phase 2 - 实现：通用引用完整性测试类

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 1 全部 Decision 已决

- [ ] `Add`：`TestErpSeedDataIntegrity`（BaseTestCase + 手动 CoreInitialization，镜像 TestAuthSeedLoadingProof 初始化块）——`testAllSeedTablesLoadable()`（枚举全部实体 findAll 非空）+ `testNonNullRelationKeysPointToExistingRows()`（to-one 全量遍历，refEntity 主键集 Set 比对；白名单三元组 (ownerEntity, relationName, key) 豁免机制常量表；非主键 join 按 Phase 1 Decision 实现）
      - Skill: `nop-testing`
- [ ] `Add`：白名单常量表（Phase 1 裁决的弱指针/占位豁免，每项注释引用证据来源）
      - Skill: `nop-testing`
- [ ] `Fix`（条件触发）：Explore 证实为真实悬空引用的 seed 缺口——最小化修正（追加缺失行优先，遵守种子追加纪律），每处记录理由
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新测试类两测试方法全绿（全量表可加载 + 零未豁免悬空引用）
- [ ] 白名单豁免表仅含 Phase 1 裁决项

### Phase 3 - 验证与文档对齐

Status: planned
Targets: `docs/architecture/seed-data.md`、`docs/logs/`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [ ] `Proof`：`mvn clean install -DskipTests` 全仓 BUILD SUCCESS + `mvn test -pl app-erp-all` 新测试 + 既有回归全绿（重点 TestAuthSeedLoadingProof、TestErpAllJobYamlLoading、TestModuleMetaReader）
      - Skill: `nop-testing`
- [ ] `Add`：`seed-data.md` 增「引用完整性校验」注记（测试类位置 + 覆盖范围 + 白名单机制 + 后续 seed 追加须过此测试的义务）；`docs/logs/2026/08-15.md` 日志条目
      - Skill: `none`

Exit Criteria:

- [ ] 全仓构建 + app-erp-all 测试全绿
- [ ] owner doc + 日志更新完成

## Draft Review Record

- Independent draft review iteration 1: pending（计划创建即待独立子代理审查）

## Closure Gates

- [ ] 范围内行为完成（两测试方法全绿 + 白名单闭环）
- [ ] 相关文档对齐（seed-data.md 注记 + 日志）
- [ ] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl app-erp-all`）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <待执行完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <待填写>
- Evidence: <待填写>

Follow-up:

- <待填写>
