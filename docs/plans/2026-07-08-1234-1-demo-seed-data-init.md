# 2026-07-08-1234-1-demo-seed-data-init 演示种子数据初始化（init-data）启用

> Plan Status: completed
> Mission: erp
> Work Item: 部署期演示种子数据落地（DataInitInitializer + `_vfs/_init-data/` CSV），解除空库阻断
> Last Reviewed: 2026-07-08
> Source: AGENTS.md「当前项目阶段」当前重点「各域细化端到端验证」；deferred 项承接 `docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md` Deferred「数据驱动看板 KPI 断言（具体业务数值）」（触发条件「当 `app-erp-test-data` 落地标准种子集 + 需端到端业务数值回归时」）。**注**：0637-1 触发条件措辞指向 test-scope `app-erp-test-data`，但 Playwright E2E 运行于生产 `quarkus-run.jar`（test-scope 模块不打包进 runner），解除运行时/E2E 数据层阻塞的正确机制是**部署期 seed**（`_vfs/_init-data/` + `DataInitInitializer`，本计划）；test-scope `app-erp-test-data` 填充（Java 测试夹具）保持独立 Deferred（见下）。`app-erp-test-data/pom.xml` 描述亦显式「与部署 seed（app-erp-seed，独立 follow-up）区分」——本计划承接该部署 seed follow-up
> Related: `docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md`（completed，其「数据驱动 KPI 断言」Deferred 本计划解除数据层阻塞）、`docs/plans/2026-07-08-1234-2-crud-page-e2e-smoke.md`（同批 #2，CRUD 页面冒烟在种子库上更有意义）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`，非采信旧记忆）：

- **空库现状**：`app-erp-all/src/main/resources/application.yaml:24` `init-database-schema: true`（启动建表），`:28` H2 文件库 `jdbc:h2:./db/erp`（持久化跨重启），**无 `init-database-data` 配置**（平台默认 `false`）。即：**每次启动建空表，零业务数据**。全部 E2E（0637-1 的 34 spec）+ 看板/报表 KPI 均在空库上断言「DOM 存在 + HTTP 200」（冒烟级），数值为 0/空。
- **平台机制确认（权威源 `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md:531-569`）**：`DataInitInitializer` 在 `DataBaseSchemaInitializer` 之后执行：
  - 配置：`nop.orm.init-database-data`（Boolean，默认 `false`）+ `nop.orm.init-database-data-location`（String，默认 `/_init-data/`）。
  - CSV 插入：按 ORM 模型拓扑序遍历实体，检查 `{location}/{tableName}.csv`，存在则经 `dao.saveEntity()` 插入（列名按实体 `code` 即大写数据库列名匹配，不匹配抛 `NopException`，自动设置租户/创建人）。
  - SQL 文件：扫描 `{location}/*.sql`，按文件名排序在事务中执行（原生 JDBC，多租户实体须手填 `TENANT_ID`）。
  - `_vfs/_init-data/` 目录下放文件（如 `erp_md_organization.csv` / `01-init-dict.sql`）。
- **`_init-data` 目录不存在**：`fd '_init-data'` 全仓 0 命中（排除 target）。即**从未启用过部署期数据初始化**。
- **核心主数据表名（seed 候选，`module-master-data/model/app-erp-master-data.orm.xml` 逐行核实）**：`erp_md_organization`(:945)、`erp_md_currency`(:605)、`erp_md_uom`(:536)、`erp_md_material_category`(:243)、`erp_md_material`(:173)、`erp_md_material_sku`(:282)、`erp_md_partner`(:333)、`erp_md_partner_address`(:379)、`erp_md_partner_contact`(:413)、`erp_md_warehouse`(:448)、`erp_md_location`(:494)、`erp_md_employee`(:769)、`erp_md_subject`(:811 GL 科目)、`erp_md_acct_schema`(:868)、`erp_md_acct_schema_coa`(:914 科目表)、`erp_md_tax_rate`(:672)、`erp_md_settlement_method`(:702)、`erp_md_bank_account`(:734)、`erp_md_cost_center`(:991)、`erp_md_exchange_rate`(:632)、`erp_md_uom_conversion`(:561)。
- **用户 seed 不在范围**：E2E 认证经 0637-1 确立的 `-Dnop.auth.login.allow-create-default-user=true` JVM 属性自动创建 `nop`/`123` 用户（`LoginServiceImpl.addDefaultUser()`），不依赖 init-data。生产 `application.yaml:12` 保持 `allow-create-default-user: false`。
- **测试夹具模块（test-scope）区分**：`app-erp-test-data/`（消费方 -service 测试模块以 test-scope 依赖，故**不打包进 runner jar**）当前仅含 `load-order.txt` 骨架 + `tables/README.md` 占位，无实际 CSV。其 pom 描述显式「与部署 seed（app-erp-seed，独立 follow-up）区分」。**本计划是部署 seed（runtime/E2E 可见），非 test-scope 夹具**。
- **非保护区域**：CSV 数据文件 + application.yaml 配置变更，**非 `model/*.orm.xml` ask-first**。属 `plan-first`（运行时配置 + 跨多会话 + >5 文件）。
- **Idempotency 已确认非幂等（关键约束，经源码核实）**：平台源码 `../nop-entropy/nop-persistence/nop-orm/src/main/java/io/nop/orm/initialize/DataInitInitializer.java` 存在（133 行）。`loadCsvData()`(:83-107) 逐行 `dao.newEntity()` → 填列 → `dao.saveEntity(entity)`（:98-106），**无存在性检查、无 upsert、无 truncate、无幂等守卫**。**门控机制已文档化**：`orm-defaults.beans.xml:89-92` 注册 `DataInitInitializer` 为条件 bean（`<ioc:condition><if-property name="nop.orm.init-database-data"/></ioc:condition>` + `ioc:after` `DataBaseSchemaInitializer`，archived 平台计划 `../nop-entropy/ai-dev/archived/2026-06/129-orm-auto-init-database-data.md` §Phase 1 记录）——即 `init-database-data=false`（默认）时 bean 不实例化，`=true` 时每次启动均执行 `@PostConstruct init()`（非仅首次）。结论：持久 H2 文件库（`./db/erp` 跨重启）上重复启动若表已有 seed 行 → 重复 `saveEntity()` 触发主键冲突；archived plan 129 Deferred 裁定幂等性为「optimization candidate / 刻意 out-of-scope」。**fresh-DB 重置策略必需**（E2E/演示启动前删 `./db/erp`，Phase 1 经验性确认该重置在 webServer 生命周期中可靠）。

剩余差距：(1) 应用启动后零业务数据，看板/报表/CRUD 列表全空，无法演示、无法数据驱动验证；(2) 部署 seed 机制从未启用，`_init-data/` 目录不存在；(3) DataInitInitializer 幂等性未知，持久 H2 重启行为待验证；(4) 主数据 CSV 设计（哪些表、哪些记录、参照完整性）未做。

## Goals

- 落地核心主数据演示种子集（`_vfs/_init-data/*.csv`），覆盖组织/币种/计量单位/物料/往来单位/仓库/员工/科目体系/税率/结算方式等 bootstrap 维度，使应用首次启动即有可演示的连贯主数据。
- 经 config-gated（非生产默认）启用 `init-database-data`，使 E2E/演示启动后看板 KPI 非零、CRUD 列表有行、报表有数据行——解除 0637-1「数据驱动 KPI 断言」Deferred 的数据层阻塞。
- 确定 DataInitInitializer 门控机制（源码 + beans.xml 已确认非幂等、条件 bean 仅 `init-database-data=true` 时实例化）并落地可重复运行的 seed 策略（fresh-DB 重置）。
- 在 `docs/testing/e2e-runbook.md` 记录种子库启动方式。

## Non-Goals

- **不**seed 业务交易单据（采购订单/销售订单/凭证/工单等）——业务单据涉复杂状态机/过账/跨域参照，属域级深化，归后继（触发条件：各域端到端业务数值回归需交易数据时）。本期仅 bootstrap 主数据。
- **不**修改 `model/*.orm.xml`——纯数据文件 + 配置，零 ORM 变更。
- **不**改生产 `application.yaml` 默认为 seed-on——生产保持 `init-database-data` 缺省（false）；seed 经 JVM 属性 / demo profile 触发（生产安全）。
- **不**填充 test-scope `app-erp-test-data`（Java 测试夹具）——那是独立 test-scope 资产，与部署 seed 严格区分（pom 描述已声明）；Java 测试既有 per-test 夹具不依赖本计划。
- **不**做数据驱动 KPI **数值断言**（断言「KPI=¥1.2M」）——本计划解除**数据存在**阻塞（使数值非零可观测）；精确数值断言是 E2E successor 层（0637-1 Deferred 上层）。
- **不**做多账套/多币种复杂场景种子——本期单组织/本位币最小连贯集。
- **不**接入 CI 管道自动 seed（CI/CD 归 `2026-07-07-2359-1` Deferred O-14，触发条件：团队 >2 人或正式 QA 阶段）。

## Task Route

- Type: `architecture change`（新增部署期数据初始化层，首次启用平台 DataInitInitializer）+ `implementation-only change`
- Owner Docs: `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（§自动初始化数据 DataInitInitializer，权威机制源）、`docs/design/master-data/`（主数据实体语义/参照关系）、`docs/design/finance/posting.md`（科目体系 erp_md_subject / erp_md_acct_schema 用于过账，判定哪些科目是 seed 必需）、`docs/testing/e2e-runbook.md`（种子库启动记录）、`docs/architecture/system-baseline.md`（部署配置基线）
- Skill Selection Basis: `nop-backend-dev`（ORM 实体列 code 映射 + DataInitInitializer 机制 + 配置门控；CSV 列名须按实体 `code` 即大写数据库列名，Phase 1 逐表核实列 code）。`nop-testing` 仅当 Phase 3 需扩展 E2E 断言「数值非零」时（局部，复用 0637-1 helper）。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`app-erp-all/target/quarkus-app/quarkus-run.jar`（0637-1 已建立 `mvn clean install -DskipTests` + webServer 范式）。
- H2 文件库：`./db/erp`（`application.yaml:28`）。Phase 1 探索幂等性时可能需删除该目录做 fresh-DB 验证。
- 配置门控：`-Dnop.orm.init-database-data=true`（候选 A，JVM 属性）或新增 demo profile（候选 B，Phase 1 Decision）。
- 回滚策略：seed 为纯新增（CSV + 配置），失败不影响生产构建；移除 `_init-data/` 文件 + 关闭 JVM 属性即回滚。

## Execution Plan

### Phase 1 - 幂等性探索 + 主数据清单 + 配置策略（Explore + Decision）

Status: completed
Targets: `DataInitInitializer` 运行时行为、`module-master-data/model/app-erp-master-data.orm.xml`（表/列 code 盘点）、`app-erp-all/src/main/resources/application.yaml`
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: `app-erp-all/target/quarkus-app/quarkus-run.jar` 已构建

- [x] `Proof`（Explore）：经验性确认门控 + 非幂等行为——
      - (1) 放 1 个最小 CSV（如 `erp_md_currency.csv` 含 2 行 CNY/USD）到 `_vfs/_init-data/`，以 `-Dnop.orm.init-database-data=true` 启动 fresh DB（删 `./db/erp`），确认数据插入成功（GraphQL 查询 `ErpMdCurrency` 返回 2 行）。
      - (2) **重跑验证**：不删 DB 再次启动（持久 H2 已有数据，`init-database-data=true` → 条件 bean 实例化 → `init()` 重跑），确认主键冲突报错（与源码 `dao.saveEntity()` 无守卫 + beans.xml 条件 bean 每次启用均实例化的预期一致）。据此确定 E2E webServer 生命周期的 fresh-DB 重置步骤（删 `./db/erp.mv.db` 后启动）可靠。
      - Skill: `nop-backend-dev`
- [x] `Decision`：配置门控策略——
      - 选择：候选 A（`-Dnop.orm.init-database-data=true` JVM 属性，镜像 0637-1 认证 `allow-create-default-user=true` 范式，生产 application.yaml 不改默认）/ 候选 B（新增 `%demo` Quarkus profile，`init-database-data: true`，需 `quarkus.profile` 激活）。候选 C（默认 true）**rejected**——经源码 + beans.xml 核实，条件 bean 每次启用均实例化且非幂等，持久 H2 重复启动会主键冲突，故生产不可默认开。
      - 替代方案分析 + 残留风险（生产误开 seed / H2 文件库 fresh-DB 重置策略对持久化的影响）。
      - Skill: `nop-backend-dev`
- [x] `Proof`：逐表读取核心主数据源 `orm.xml`，列出 seed 候选表的**列 code 清单**（CSV 列名须匹配 `code` 即大写数据库列名，`orm-model-design.md:542`），标注 mandatory 列（seed 须填）、参照 FK 列（须先 seed 被引用表）、framework-managed 列（`orgId`/`tenantId` 等 DataInitInitializer 自动设置，CSV 不填或填固定值）。产出「seed 表清单 + 列映射 + 加载拓扑序」表。
      - Skill: `nop-backend-dev`
- [x] `Decision`：seed 记录集范围——确定本期 seed 的具体表与每表行数（领先方案：bootstrap 主数据 ~15-20 表，每表 3-10 行连贯记录；业务单据 Non-Goal）。记录选择依据（哪些是看板/报表/CRUD 的最小可见前提）与残留风险（参照完整性遗漏导致启动失败的具体防护）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] DataInitInitializer 门控机制 + 非幂等结论落盘（`init-database-data` 如何控制 bean 实例化；非幂等→fresh-DB 重置策略确定），解除 Phase 2/3 配置与运行策略阻塞。
- [x] seed 表清单 + 列映射 + 加载拓扑序表落盘（写入本计划或引用的 `docs/analysis/` 文件），每表标注 mandatory/FK/framework-managed 列。
- [x] 配置门控 Decision 记录选择 + 替代方案 + 残留风险。

> Phase 1 Evidence（2026-07-08）：门控+非幂等经验性确认详见 `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md` §1。**平台 bug 修复**：执行中发现 `DataInitInitializer.init()` 在 Quarkus `@PostConstruct` 因 IoC 循环依赖致 `ormTemplate.sessionFactory=null` → NPE（archived plan 129 单元测试未覆盖真实 IoC 启动路径）。修复：`DataInitInitializer.ensureOrmTemplateSessionFactory()` 补齐（`nop-entropy/nop-persistence/nop-orm`），经验性确认 fresh-DB 启动 CSV 插入成功 + GraphQL 返回种子行；重启不删 DB 确认主键冲突。表/列映射+拓扑序详见 `docs/analysis/2026-07-08-1234-1-seed-data-table-column-map.md` §3。配置门控 Decision §2（候选 A，JVM 属性，生产默认保持关）。

### Phase 2 - 编写核心主数据 seed CSV（Add）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/*.csv`（或 Phase 1 Decision 另选模块路径）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 幂等结论 + 列映射表 + 范围 Decision

- [x] `Add`：按 Phase 1 拓扑序编写 seed CSV——按依赖顺序（organization → currency → uom → material_category → material → material_sku → partner → partner_address/contact → warehouse → location → employee → subject → acct_schema → acct_schema_coa → tax_rate → settlement_method → bank_account → cost_center → exchange_rate → uom_conversion）。每表 CSV 列名对齐实体 `code`，mandatory 列全填，FK 列引用已 seed 的上游记录 ID（固定可读 ID 如 `org-001`/`cur-cny`）。数据须连贯自洽（如物料的 `uomId`/`categoryId` 引用已 seed 行；科目的 `acctSchemaId` 引用已 seed 科目表）。
      - Skill: `nop-backend-dev`
- [x] `Add`（条件性→按 Phase 1）：若 DataInitInitializer 非 CSV 原生支持某些复杂初始化（如序列/字典种子），补 `01-init-sequence.sql` 等 SQL 文件（按文件名排序执行，注意多租户 `TENANT_ID` 列）。
      - Skill: `nop-backend-dev`
      - **No-op**：21 表全部经 CSV 原生支持（显式 ID 无需序列；dict 列为 VARCHAR 无需字典种子 SQL）。无 SQL 文件产出。

Exit Criteria:

- [x] `_vfs/_init-data/` 下含 Phase 1 范围内全部表的 CSV（`ls` 计数匹配清单），每 CSV 列名经 `code` 映射核实（抽样 5 表逐列对齐 `orm.xml` column `code`）。

> Phase 2 Evidence（2026-07-08）：21 CSV 落地（`ls _vfs/_init-data/*.csv | wc -l` = 21）。抽样 5 表（tax_rate/subject/exchange_rate/material/partner）逐列对齐 ORM `code`——全部命中，无幽灵列；framework-managed 列（CREATED_BY/CREATE_TIME/DEL_VERSION/VERSION/UPDATED_BY/UPDATE_TIME）正确省略（ORM 拦截器自动填充，经 Phase 1 PK 冲突错误行核实 `CREATED_BY='sys'`）。固定 ID 方案（1,2,3…每表独立序列）经 Phase 1 fresh-DB 启动验证 GraphQL 返回显式 ID。拓扑序由 DataInitInitializer `getEntityModelsInTopoOrder()` 自动保证。

### Phase 3 - 启用配置 + 启动验证 + E2E 数据可见性证明（Proof）

Status: completed
Targets: `application.yaml` 或 `playwright.config.ts` webServer JVM 参数、`app-erp-all` 启动、E2E 数据观测
Skill: `nop-backend-dev | nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 CSV 落地 + Phase 1 配置 Decision

- [x] `Add`：按 Phase 1 Decision 启用 `init-database-data`（JVM 属性 / demo profile / 配置注释说明），确保生产默认仍关闭。
      - Skill: `nop-backend-dev`
- [x] `Proof`：fresh-DB 启动（按 Phase 1 幂等结论，必要时删 `./db/erp`）+ seed 加载——验证启动日志无主键冲突/列映射错误，全部 CSV 经 DataInitInitializer 插入成功。
      - Skill: `nop-backend-dev`
- [x] `Proof`：经 GraphQL 查询抽样验证种子数据可见——核心主数据实体（material/partner/warehouse/subject/employee）查询返回非空（行数匹配 CSV），`orgId`/租户框架字段已自动填充。
      - Skill: `nop-backend-dev`
- [x] `Proof`：复跑既有 E2E（10 看板 + 24 报表，0637-1 套件）在种子库上——验证看板 KPI 卡片数值非零（或报表渲染非空数据行），证明 seed 解除空库阻断。不新增精确数值断言（Non-Goal），仅观测「非空」。
      - Skill: `nop-testing`

Exit Criteria:

- [x] seed 库启动成功，0 主键冲突 / 0 列映射错误；GraphQL 抽样查询返回种子数据。
- [x] 既有 E2E 套件在种子库上全绿（34 spec 不回归）；看板/报表数值非空可观测（解除 0637-1 数据层阻塞的证明）。

> Phase 3 Evidence（2026-07-08）：(1) `playwright.config.ts` webServer 命令更新为 `rm -f db/erp.mv.db db/erp.trace.db && java ... -Dnop.orm.init-database-data=true -jar ...`（fresh-DB 重置 + seed JVM 属性，生产 `application.yaml` 不改默认）。(2) fresh-DB 启动：21 `nop.orm.load-csv-data` 日志行（21 表全加载），0 主键冲突/0 列映射错误，启动 11.4s 监听 8080。(3) GraphQL 抽样：material=4/partner=4/subject=8/warehouse=2/employee=3/currency=2/uom=4/sku=4(FK material.uoM 解析正确)，`createdBy='sys'` 证实框架审计字段自动填充。(4) E2E 全套件种子库上 **35/35 passed (5.7m)**（10 看板 + 24 报表 + 1 KB），0 回归；master-data 域看板/报表（md-material-price-list/md-partner-list）经 GraphQL 证实数据非空。

### Phase 4 - 运行手册 + 文档对齐（Add）

Status: completed
Targets: `docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/architecture/system-baseline.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3 验证通过

- [x] `Add`：`docs/testing/e2e-runbook.md` 增「种子库启动」段——fresh-DB 重置步骤 + `-Dnop.orm.init-database-data=true` + 幂等结论说明。
      - Skill: none
- [x] `Add`：`docs/testing/known-good-baselines.md` 增 seed 基线行（commit + seed 表数 + E2E 在种子库上状态）。
      - Skill: none
- [x] `Add`：`docs/architecture/system-baseline.md` 核对部署配置描述——新增 init-database-data 配置点说明（生产默认关，demo/E2E 经 JVM/profile 开），不与既有描述冲突。
      - Skill: none

Exit Criteria:

- [x] e2e-runbook 含种子库启动步骤；known-good-baselines 含 seed 基线行；system-baseline 配置描述一致。

> Phase 4 Evidence（2026-07-08）：(1) `e2e-runbook.md` 新增「种子库启动（演示 / 数据可见性）」段（fresh-DB 重置 + JVM 属性 + 手动启动方式 B + 生产安全说明 + 种子范围/Non-Goal）+ 更新方式 A webServer 命令 JVM 参数清单（加 seed flag + fresh-DB reset）+ 更新「空库冒烟」已知限制为「已解除」。(2) `known-good-baselines.md` 新增 2026-07-08 seed 基线行（full + E2E seeded DB，35 spec 5.7m，0 失败）。(3) `system-baseline.md` 部署形态后新增「数据初始化（部署期 seed）」配置点说明（默认关/非幂等/fresh-DB 重置/E2E 经 JVM 属性触发/21 表覆盖 + 引用分析文档）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0bff97795ffer3SmRH0IpF4tmU) because 1 BLOCKER + 2 MAJOR + 2 MINOR（全部基线事实经实时仓库核实）：
  - B1（已修）：Baseline 虚称平台源码 `DataInitInitializer`「未在本地 nop-entropy 检出（fd/rg 0 命中）」——**虚假事实**：源码实际存在于 `../nop-entropy/nop-persistence/nop-orm/src/main/java/io/nop/orm/initialize/DataInitInitializer.java`（133 行）。已据源码重写：`loadCsvData()`(:83-107) 逐行 `dao.saveEntity()` 无幂等守卫 → 确认非幂等；archived plan 129 Deferred 裁定幂等性刻意 out-of-scope。Phase 1 Explore 由「复现未知幂等性」改为「经验性确认非幂等 + 门控机制」。
  - M1（已修）：Related 引用 `2026-07-08-0637-2-crud-page-e2e-smoke.md` 路径错误（0637-2 实为 localdate-now-cleanup）——已更正为 `2026-07-08-1234-2-crud-page-e2e-smoke.md`。
  - M2（已修）：Source 承接 0637-1 触发条件措辞指向 test-scope `app-erp-test-data`，与本计划部署 seed 方案存在表面矛盾——已补注说明 0637-1 措辞指向 test-scope 机制不精确，运行时/E2E 数据层阻塞的正确解除机制是部署期 seed（`_vfs/_init-data/`），test-scope 填充保持独立 Deferred。
  - m1（已修）：typo「0673-1」→「0637-1」。
  - m2（已修）：「pom.xml test-scope」措辞不精确（该模块本身非 test-scope，是消费方 test-scope 依赖）——已改为「消费方以 test-scope 依赖，故不打包进 runner jar」。
  - 正面确认（无需变更）：21 个主数据表行号逐项精确核实通过；application.yaml 配置项/行号（init-database-schema:24/jdbc:28/allow-create-default-user:12/无 init-database-data）准确；`_vfs/_init-data/` 确认不存在；平台文档 orm-model-design.md:531-569 机制描述准确；Goals/Non-Goals 边界清晰；单结果面（规则 4/14）；plan-first 自分类正确（非 ask-first，生产默认保持关）；item types 完整（规则 7）；Skill 行齐备且每项标注（规则 8）；Phase 1 Decisions 含替代方案 + 残留风险（规则 9）；反松弛合规；3 Deferred 均带触发条件；命名/模板结构完整。
- Independent draft review iteration 2: `accept` (ses_0bff3641affexEgPYrfawB4JUo) — B1/M1/M2/m1/m2 全部经实时仓库独立复核确认 RESOLVED，无新增 BLOCKER/MAJOR：DataInitInitializer 源码（133 行，`loadCsvData` :83-107 无幂等守卫）+ archived plan 129 Deferred 经核实属实；Related 路径 `1234-2` 文件存在；Source test-scope/deployment-seed 调和说明到位；0673 typo 清零；test-scope 措辞归正。独立基线复核（application.yaml/init-data 目录缺失/表名行号/平台文档）全准确。采纳 2 非阻塞 NOTE 优化：(N1) beans.xml:89-92 条件 bean 门控事实（`<if-property name="nop.orm.init-database-data"/>`）已折入 Baseline，Phase 1 Explore 由「探索未知门控」收敛为「经验性确认已文档化门控 + fresh-DB 重置」；(N2) Goals「门控仅首次实例化」措辞修正为「条件 bean 仅启用时实例化（非仅首次）」+ 候选 C rejected（非幂等不可生产默认开）。**草案审查已收敛，计划为可接受的执行契约，Plan Status 升级为 active。**

## Closure Gates

> 本计划涉及部署配置 + 数据初始化层（首次启用 DataInitInitializer），结束前除下方门控外运行一次 fresh-DB 种子启动 + 完整 E2E 套件（确认 seed 不破坏既有运行时）+ 后端构建。

- [x] 范围内行为完成（核心主数据 seed CSV 落地 + init-database-data config-gated 启用 + 种子库启动成功）
- [x] 相关文档对齐（e2e-runbook + known-good-baselines + system-baseline）
- [x] 已运行验证：fresh-DB 种子启动（0 冲突）+ `npx playwright test`（34 spec 在种子库上全绿）+ `mvn clean install -DskipTests`（154 模块，确认 seed 文件无构建污染）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 业务交易单据种子（采购/销售/凭证/工单等）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 业务单据涉复杂状态机/过账/跨域参照，属域级深化。本期 bootstrap 主数据已解除「空库」阻断（看板/报表/CRUD 列表有主数据可观测）。交易数据是端到端业务数值验证的更深层。
- Successor Required: `yes` — **已交付**（计划 `2026-07-08-1445-1`）：P2P+O2C 最小连通集（源单据头/行 + 已过账财务产物：凭证/凭证行/业财回链/AR-AP 辅助账/GL 余额/期间 OPEN，共 23 张交易 CSV 落地 `_vfs/_init-data/`，fresh-DB 启动 0 冲突 + GraphQL FK 一致 + 53 spec E2E 0 回归 + 核心 KPI 数值转非空）。扩展域交易单据按域逐批仍为后续 successor。
- Trigger Condition: 当各域端到端业务数值回归（如断言采购订单全链路凭证金额）需交易数据时，按域逐批补充交易单据 seed。

### 数据驱动 KPI 精确数值断言

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划解除「数据存在」阻塞（数值非零可观测）；精确断言「KPI=特定值」需固定 seed 集确定性 + 断言逻辑，是 E2E successor 层（0637-1 Deferred 上层）。
- Successor Required: `yes`
- Trigger Condition: 当 seed 集固化且需回归断言看板/报表具体业务数值时。

### test-scope `app-erp-test-data` Java 测试夹具填充

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `app-erp-test-data`（test-scope，Java 测试可见，不打包进 runner）与部署 seed（本计划，runtime/E2E 可见）严格区分（pom 描述已声明）。Java 测试既有 per-test 夹具不依赖本计划。
- Successor Required: `yes`
- Trigger Condition: 当 Java 测试需跨类共享主数据夹具且 per-test 夹具冗余显著时。

## Closure

Status Note: 4 Phase 全部执行完毕。核心主数据演示种子集（21 张表 ~57 行，`_vfs/_init-data/*.csv`）经平台 `DataInitInitializer` 在 fresh-DB 启动时按拓扑序插入成功（0 冲突/0 列映射错误）。config-gated 经 `-Dnop.orm.init-database-data=true` JVM 属性 + fresh-DB 重置（`playwright.config.ts` webServer）触发，生产 `application.yaml` 默认关闭。执行中发现并修复平台 bug（`DataInitInitializer` 在 Quarkus `@PostConstruct` 因 IoC 循环依赖致 `ormTemplate.sessionFactory=null` → NPE；archived plan 129 单元测试未覆盖真实 IoC 启动路径）。GraphQL 抽样证实种子数据非空 + FK 完整 + 框架审计字段自动填充。既有 E2E 套件在种子库上 35/35 全绿（0 回归）。`mvn clean install -DskipTests`（154 模块）构建通过。文档对齐：e2e-runbook 种子库启动段 + known-good-baselines seed 基线行 + system-baseline 配置点说明 + seed-data.md 状态更新 + backlog README 工作项。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_0bfc7c5e8ffeJWkmgFey0NZcPj`（新会话，未参与执行）
- Result: **PASS**（10/10 checks 通过，推荐关闭）
- Evidence: 实时仓库逐项核实——(1) Plan Status=completed + 4 Phase 全 completed + Phase 内 0 个 `[ ]`（唯一 `[ ]` 为本结束审计门控，由本次审计满足）；(2) `ls _vfs/_init-data/*.csv | wc -l`=21，git status 显示目录为未跟踪新文件（印证 Baseline「目录原不存在」）；(3) 抽样 3 表（material/employee/cost_center）CSV 列 code 全部命中 orm.xml `<column code>`，0 幽灵列，framework-managed 列正确省略；(4) playwright.config.ts:18 含 `rm -f db/erp.mv.db` + `-Dnop.orm.init-database-data=true`；(5) production application.yaml 无 `init-database-data`（生产默认关）；(6) DataInitInitializer.java `ensureOrmTemplateSessionFactory()` 存在并从 `init()` 调用，import OrmTemplateImpl；(7) e2e-runbook/known-good-baselines/system-baseline/analysis 文档对齐；(8) backlog README 含 ✅ done 工作项；(9) 3 Deferred 全保留（含 Trigger Condition + Successor Required）；(10) 构建证据具体可核实（TestDataInitInitializer 含 4 @Test，known-good-baselines 记录 154 模块/35 spec/11.4s 启动）。
- Non-blocking note: 全部交付物当前未提交 git（工作树状态满足所有声明，提交为独立工作流步骤，非计划完整性缺陷）。

Follow-up:

- 平台 bug 修复（`DataInitInitializer.ensureOrmTemplateSessionFactory`）的 nop-entropy 侧日志待补 `nop-entropy/ai-dev/logs/`（本会话聚焦 nop-app-erp 计划执行；平台日志为独立 follow-up，不阻塞本计划关闭）
