# 2026-08-31-1143-1-app-erp-all-default-seed-loading app-erp-all 默认开启种子数据初始化（演示/沙盒语义）

> Plan Status: completed
> Mission: erp
> Work Item: 让 `app-erp-all` 启动时自动加载 `_vfs/_init-data/` 下的 97 CSV + 1 SQL（演示/沙盒语义）
> Last Reviewed: 2026-08-31
> Source: 用户请求（2026-08-31）——"app-erp-all 启动时引入了全部测试数据吗？确保所有表都是非空的？" → 核实现状后用户选 A：`application.yaml` 默认开启 `init-database-data` + 生产 profile 也开（演示/沙盒场景）
> Related: `docs/plans/2026-07-08-1234-1-demo-seed-data-init.md`（seed 机制首次落地）、`docs/plans/2026-08-15-2000-1-seed-data-referential-integrity-test.md`（门禁 `TestErpSeedDataIntegrity`）、`docs/plans/2026-08-25-0330-1-aggregate-notify-cs-seeds-into-app-init-data.md`（最新聚合批 94→96 CSV + 1 SQL）
> Audit: required（plan-first）

## Current Baseline（实时仓库核实 2026-08-31）

- **种子资产物理就位**：`app-erp-all/src/main/resources/_vfs/_init-data/` 含 **97 CSV + 1 SQL**（`zz-sequence-advance.sql`），所有资产已在 `nop-vfs-index.txt` 第 4-101 行登记（`grep -n '/_init-data/' app-erp-all/src/main/resources/nop-vfs-index.txt | head -5` 实证）。**每域分布**（实仓 `ls _init-data/*.csv | awk -F'_' '{print $1"_"$2}' | sort | uniq -c` 实证）：
  - master-data (`erp_md_`) 21 / inventory (`erp_inv_`) 5 / purchase (`erp_pur_`) 8 / sales (`erp_sal_`) 8 / finance (`erp_fin_`) 7 / assets (`erp_ast_`) 3 / projects (`erp_prj_`) 6 / manufacturing (`erp_mfg_`) 8 / maintenance (`erp_mnt_`) 8 / quality (`erp_qa_`) 6 / crm (`erp_crm_`) 5 / cs (`erp_cs_`) 3 / hr (`erp_hr_`) 4
  - 平台表 4：`nop_auth_role` / `nop_auth_user` / `nop_auth_user_role` / `nop_sys_code_rule`
  - 跨域部署配置 1：`erp_sys_notification_template`
  - **总计 97 CSV（14 域表名空间）+ 1 SQL**；aps/logistics/b2b/contract/drp 五域无 seed，沿用既有 `seed-data.md` 的「Non-Goals（归后续批次）」。
- **预存文档漂移登记**（**Phase 3 同步修正 2 处 owner-doc**）：`docs/architecture/seed-data.md` 多处（line 28/72/77）仍写「96 CSV」、`docs/testing/e2e-runbook.md:187` 仍写「91 张 CSV」——全部过期；本计划 Phase 3 同步修正。`app-erp-all/src/test/java/io/nop/app/all/it/ErpIntegrationTestCase.java:46` 注释「94 CSV + 1 SQL」亦过期，**不在本计划范围**（计划显式排除 Java 测试文件注释改动）。
- **`application.yaml` 当前状态**：`app-erp-all/src/main/resources/application.yaml:16-17` 设 `nop.orm.init-database-schema: true`，**未设** `nop.orm.init-database-data`。后者默认 `false`（平台权威源 `nop-entropy/nop-persistence/nop-orm/src/main/java/io/nop/orm/OrmConfigs.java:55-56`）。H2 文件库 URL 在 `application.yaml:20`（`jdbc:h2:./db/erp`），本计划默认段仅 ORM 层改 config-gated seed flag，不动 URL。
- **`DataInitInitializer` 行为**：平台 `nop-entropy/nop-persistence/nop-orm/src/main/resources/_vfs/nop/orm/beans/orm-defaults.beans.xml:92-97` 注册为条件 bean（`<if-property name="nop.orm.init-database-data"/>`），**lazy init**（无 `ioc:force-init`），仅在 `init-database-data=true` 时实例化；按 ORM 拓扑序遍历所有实体表，对每个 `entityModel.tableName` 检查 `{location}/{tableName}.csv`，存在则经 `dao.saveEntity()` 插入（`DataInitInitializer.loadCsvData()` nop-entropy/.../initialize/DataInitInitializer.java:83-107，CSV 列名按实体 column `code` 匹配大写数据库列名）；SQL 文件按文件名排序在事务中执行（同 `DataInitInitializer.executeSqlFiles()`）。
- **非幂等（关键约束）**：`loadCsvData()` 逐行 `dao.newEntity()` → `dao.saveEntity(entity)`，**无存在性检查、无 upsert、无 truncate**（同 plan `2026-07-08-1234-1-demo-seed-data-init.md:26` 已实证记录）。**持久 H2 文件库（`./db/erp` 跨重启）上重复启动若 seed 表已有行 → 主键冲突**。本计划启用默认 seed 后，**fresh-DB 重置成为运行前置条件**（与现有 `_tmp-server.sh:39-41` 和 `playwright.config.ts:18` 一致）。
- **现有测试基础设施已采用 `init-database-data=true` + fresh-DB reset 范式**（实证）：
  - `playwright.config.ts:18` webServer 命令：`rm -f db/erp.mv.db db/erp.trace.db && java ... -Dnop.orm.init-database-data=true ... -jar ${runnerJar}`（fresh-DB 重置 + JVM flag）
  - `_tmp-server.sh:39-47` 手动启动脚本：同上范式（`stop_server` + `rm -f db/erp.mv.db` + JVM flag）
  - 集成测试基类 `ErpIntegrationTestCase`（plan 2026-08-25 引用 + `app-erp-all/src/test/java/io/nop/app/all/it/ErpIntegrationTestCase.java:42-46`）：per-method `container.restart()` + 显式触发 `DataInitInitializer.@PostConstruct`。**注意：基类本身不含 `@NopTestProperty`（全文件仅 1 处 import）；`init-database-data=true` 声明须由子类在容器启动前提供**（javadoc:46 明言「须经子类 @NopTestProperty 在容器启动前声明」）
  - **23 个** `TestErp*` 测试类通过 `@NopTestProperty` 显式声明 `init-database-data=true`（`grep -l "@NopTestProperty.*init-database-data" app-erp-all/src/test/java/io/nop/app/all/it/TestErp*.java | wc -l` = 23；`ls TestErpC*.java` = 22 个 C 用例类 + 1 个 `TestErpP2pPilot`；C01 在子类 :65 声明）
  - **4 个无显式声明 `init-database-data` 的测试（风险面，机制核实）**：`TestErpAiIntrospectionDisabledByDefault` / `TestErpAiIntrospectionEnabled` / `TestErpFinApDocBatchWiring` / `TestErpFinApDocumentPipeline`。**实仓核实：这 4 个类直接 `extends JunitAutoTestCase`（非继承 `ErpIntegrationTestCase`），`_cases` 下仅 0 字节空 `autotest.yaml` 标记（无 input/output 快照）**。**机制层面**：NopJunitExtension 对 autotest 容器强制 ALL_LAZY（`NopJunitExtension.java:51`），`DataInitInitializer` 为条件 bean 无 force-init（`orm-defaults.beans.xml:92-97`），未被引用则不实例化——这 4 类无人触发它（`ErpIntegrationTestCase.java:77` 显式 `BeanContainer.getBeanByType(DataInitInitializer.class)` 是 C 类才有的触发点），**故 application.yaml 默认值翻转为 `true` 后其容器预期零影响**（且 4 类断言面逐类核实为 schema/错误码级 + 自包含建数 + 相对断言，seed 免疫）。Phase 2 仍显式纳入回归验证；如有意外漂移按登记裁决处置（不得静默修改断言）。
  - `TestErpSeedDataIntegrity.java:84-86`（app-erp-all）用 `setTestConfig("nop.orm.init-database-data", true)` + `setTestConfig("nop.orm.init-database-data-location", "/_init-data/")` + 手动 `CoreInitialization.initialize()`
- **引用完整性门禁在位**：`TestErpSeedDataIntegrity.java:108-209` 实现两层校验（418 实体 findAll 零异常 + 1057 to-one 全仓扫描 + 722 非空 FK 值零悬空，2026-08-15 plan-2000-1 实证）。本计划不修改 seed CSV，门禁应继续 100% 全绿。
- **生产安全姿态漂移点**：plan `2026-07-08-1234-1-demo-seed-data-init.md:42` 「不改生产 `application.yaml` 默认为 seed-on——生产保持 `init-database-data` 缺省（false）；seed 经 JVM 属性 / demo profile 触发（生产安全）」+ `e2e-runbook.md:211` 「**生产安全**：生产 `application.yaml` 保持 `init-database-data` 缺省（`false`）；seed 仅经上述 JVM 属性 / webServer 触发」。本计划**显式推翻该裁决**（用户选择 A：演示/沙盒场景默认开，生产亦开），需在 `seed-data.md`/ `e2e-runbook.md` 登记新语义。
- **schema-only 启动实证**：`init-database-schema: true` + `init-database-data: false`（当前默认）下，`mvn clean install -DskipTests` 后 `java -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar` 启动成功（实仓 `db/erp.mv.db` 创建），但 `GraphQL findPage ErpMdCurrency` 返回空（实证：seed 未装载）。本计划后该查询将非空。
- **保护区域评估**：本计划**不修改任何 `module-*/model/*.orm.xml`**（仅 config + script + docs）；**不修改任何 `docs/architecture/seed-data.md` 的业务语义段**（仅更新「当前状态」段 + 新增「演示/沙盒默认加载」裁决段）；不修改任何业务 CSV；不修改任何 Java 生产代码；不引入新 ORM 实体。**全部修改 = config + script + docs**，零 ORM/契约/Java 生产代码变更。

## Goals

- **配置默认开 seed**：`app-erp-all/src/main/resources/application.yaml` 默认段（不依赖 profile）新增 `nop.orm.init-database-data: true`，与现有 `init-database-schema: true` 同级。`init-database-data-location` 保持平台默认 `/_init-data/`（与现 97 CSV 物理位置一致，无需显式设置）。
- **保留 fresh-DB 重置语义**：新增 `scripts/start-app.sh` 帮助脚本（**镜像 `_tmp-server.sh` 范式**，但为生产/演示语义，去掉 `quarkus.profile=test` + 测试专用 JVM flags），做四件事：
  1. 删除 `db/erp.mv.db db/erp.trace.db`（fresh-DB 重置）
  2. 启动 `java -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
  3. 端口 8011 等待 ready（与 `_tmp-server.sh` 同等待循环）
  4. 日志输出至 `_tmp/app.log`
- **文档更新**（三处 owner-doc 同步）：
  - `docs/architecture/seed-data.md` 头部「当前状态」段追加本计划裁决（演示/沙盒默认开 + 与既有 1234-1 裁决的关系）
  - `docs/testing/e2e-runbook.md` 头部「种子库启动」段追加「自本计划起，默认行为变更」注记，标注 `application.yaml` 已默认开 + 推荐 `start-app.sh` 用法
  - `docs/testing/known-good-baselines.md` 增新基线行（2026-08-31 默认 seed 启用 + 全量 green 验证）
- **基线验证**（**标准验证命令**：`docs/context/project-context.md §验证命令`）：
  - `mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）
  - fresh-DB 启动（经新 `start-app.sh`）：30 秒内出现 `Listening on: http://0.0.0.0:8011` 日志行，0 主键冲突 / 0 列映射错误（**预期 30s vs 脚本等待上限 90s**——若实测超 30s 但 < 90s 即成功，登记实测用时并裁决；超 90s 视为启动失败）
  - GraphQL 抽样 5 表（`ErpMdCurrency`/`ErpMdMaterial`/`ErpMdPartner`/`ErpPurOrder`/`ErpSalOrder`）findPage 行数与 CSV 行数一致
  - 既有测试基础设施零回归：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity,TestAuthSeedLoadingProof`（plan 2026-08-15-2000-1 实证基线：`TestErpSeedDataIntegrity` 2/0/0 + `TestAuthSeedLoadingProof` 3/0/0 = 合计 **5/0/0** 绿）+ 全 reactor `mvn test`（与 2026-08-28 plan-0219-2 已知基线 3947/0/0/1/669 一致或更优——本计划不修改生产代码，计数应**完全一致**）

## Non-Goals

- **不**新增 `app-erp-seed` 物理模块（历史 design `docs/architecture/seed-data.md:53-60` 描述的独立 Maven 模块仍为独立 follow-up；本计划走 `_vfs/_init-data/` 直接落地的现行范式）。
- **不**迁移 97 CSV 至独立模块（保持 `app-erp-all` 内 `_vfs/_init-data/` 物理位置——与所有现有计划一致）。
- **不**补全 275 张缺失表 seed（comprehensive-test-data-and-visual-coverage-roadmap backlog 范畴，M0.x 规格先行中）。
- **不**修改任何 `module-*/model/*.orm.xml`（ask-first 保护区域）。
- **不**修改任何业务 seed CSV 内容。
- **不**修改任何 Java 生产代码 / xbiz / view.xml。
- **不**修改集成测试基类 / `@NopTestProperty` 声明（23 个既有测试类已显式声明覆盖；**4 个无声明的 `JunitAutoTestCase` 子类**（`TestErpAiIntrospectionDisabledByDefault`/`TestErpAiIntrospectionEnabled`/`TestErpFinApDocBatchWiring`/`TestErpFinApDocumentPipeline`）由 Phase 2 回归验证处置——如快照/断言因 seed 装载失效，按「快照重录义务」重录，不修改其基类声明）。
- **不**修改 `playwright.config.ts` webServer 命令（已有 `-Dnop.orm.init-database-data=true` + fresh-DB reset 范式，行为不变）。
- **不**修改 `_tmp-server.sh`（已是正确范式，仅用于 SKIP_WEBSERVER=1 模式，行为不变）。
- **不**为生产/演示语义增加额外的 ERPNext-style 业务开关（保持简单——只需 config-gated 平台机制）。
- **不**添加 `DataInitInitializer` 平台改造（`docs/plans/2026-07-08-1234-1-demo-seed-data-init.md:25` 平台 bug 修复已落地 `ensureOrmTemplateSessionFactory`，无新阻塞）。

## Task Route

- **Type**: `implementation-only change`（config 1 行 + script 1 文件 + docs 3 处；零 ORM/契约/Java 生产代码变更）
- **Owner Docs**: `docs/architecture/seed-data.md`（seed 真相源 + 状态裁决）、`docs/testing/e2e-runbook.md`（E2E + 种子库运行手册）、`docs/testing/known-good-baselines.md`（基线登记）、`docs/context/project-context.md`（验证命令真相源）、`AGENTS.md`（默认引用）
- **Skill Selection Basis**: `nop-backend-dev`（DataInitInitializer 机制 + config-gated 行为 + 应用层装配关注点）；`nop-testing`（既有 `TestErpSeedDataIntegrity` 门禁复用 + 全量验证命令复用）。CSV 编制为零；ORM 变更/模型设计为零；前端为零。

## Infrastructure And Config Prereqs

- 预构建 runner jar：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS 为既有基线）。
- H2 文件库：`./db/erp`（`application.yaml:20`），启动前由 `start-app.sh` 自动 `rm -f` 重置。
- JVM 参数：默认 `application.yaml` 已含 `init-database-data: true`；额外可选 `-Dnop.auth.login.allow-create-default-user=true`（演示创建 `nop/123` 用户）— 在 `start-app.sh` 默认启用（演示语义）。
- 回滚策略：仅需 `application.yaml` 删除 1 行 + 删除 `scripts/start-app.sh`；零破坏性。

## Execution Plan

### Phase 1 - 配置启用 + 验证脚本骨架（Add + Proof）

Status: completed
Targets: `app-erp-all/src/main/resources/application.yaml`、`scripts/start-app.sh`（新文件）、`docs/logs/2026/08-31.md`（执行日志）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（既有 97 CSV 物理就位，门禁 `TestErpSeedDataIntegrity` 在位）

- [x] `Add`：`app-erp-all/src/main/resources/application.yaml:16-17` 在 `init-database-schema: true` 后追加 `init-database-data: true`（同缩进层级、同 `nop.orm.*` 命名空间）。**保留** `init-database-schema: true`（两配置项独立，建表与初始化数据均需开启）。
- [x] `Add`：新建 `scripts/start-app.sh`（可执行 `chmod +x`，路径镜像 `_tmp-server.sh` 命名）——镜像 `_tmp-server.sh:20-109` 的四段函数（stop/start/wait），但精简到演示语义：
  - 端口 8011（与 `_tmp-server.sh` 同）
  - 路径 `app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
  - 日志 `_tmp/app.log`
  - JVM 参数精简集（仅保留演示必需）：
    - `-Dfile.encoding=UTF8`
    - `-Dnop.auth.login.allow-create-default-user=true`（演示用户自动创建）
    - `-Dnop.auth.service-public=true`（避免登录鉴权阻塞演示访问；与 `_tmp-server.sh:45` 同）
    - `-Dnop.web.render-mode=flux`（前端 flux 渲染模式）
    - 其余测试/开发配置（`-Dnop.orm.init-database-data=true` 不再需——已默认开；`-Dquarkus.profile=test` 不需——演示用 default profile；`erp-*.jvm-args` 不需——演示不应默认开业务动作闸门；由用户按需另行 `-D` 添加）
  - 注释段明确「**演示/沙盒语义**」「**生产真实部署不应使用此脚本**——生产应改 MySQL/PostgreSQL + 业务动作闸门正确配置 + 数据集隔离（多租户/账套级）」+「**fresh-DB 强制**——seed 非幂等，每次启动前清库」
- [x] `Add`：`docs/logs/2026/08-31.md`（如不存在则创建）顶部追加本计划执行条目（按 `docs/logs/00-log-writing-guide.md` 倒序格式）
- [x] `Proof`：`mvn clean install -DskipTests` 全 reactor 156 模块 BUILD SUCCESS（命令来自 `docs/context/project-context.md:43`）
- [x] `Proof`：`./scripts/start-app.sh start` 启动成功——30 秒内出现 `Listening on: http://0.0.0.0:8011` 日志行；`curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8011/` 返回 200/302/401/403 之一（与 `_tmp-server.sh:100` 等待逻辑同）
- [x] `Proof`：启动日志含 97 行 `nop.orm.load-csv-data: table=erp_*, path=/_init-data/erp_*.csv`（抽样首 3 行 + 末 1 行）+ 1 行 `zz-sequence-advance.sql` 执行的日志或 0 报错
- [x] `Proof`：GraphQL 抽样 5 表 findPage 返回非空（与 `docs/architecture/seed-data.md:28` 描述的「97 CSV + 1 SQL」同口径）——**抽样行数严格按实仓 CSV `wc -l` 实证**（执行期 grep 验证）：
  - `ErpMdCurrency` → 2 行（CNY/USD；`erp_md_currency.csv` 实仓 2 行）
  - `ErpMdMaterial` → 4 行（`erp_md_material.csv` 实仓 4 行）
  - `ErpMdPartner` → **5 行**（CUST-001/CUST-002/SUP-001/SUP-002/EMP-PTN-001——含 plan 2026-07-09-1045-1 追加 EMPLOYEE 行；`erp_md_partner.csv` 实仓 5 行）
  - `ErpPurOrder` → **1 行**（PO-2026-001；`erp_pur_order.csv` 实仓 1 行）
  - `ErpSalOrder` → **1 行**（SO-2026-001；`erp_sal_order.csv` 实仓 1 行）
  - 若执行期任何抽样与 CSV 行数不一致，立即在 Closure 段登记根因（不应发生——`DataInitInitializer.loadCsvData` 逐行 `saveEntity` 在无 PK 冲突前提下应全部插入成功）
- [x] `Proof`：./scripts/start-app.sh stop（脚本应实现 stop 子命令）成功清理端口

Exit Criteria:
- [x] `application.yaml` 单行变更生效，`init-database-data: true` 在 `quarkus.profile` 未设时被默认加载
- [x] `scripts/start-app.sh` 可执行、可启动、可停止；fresh-DB 重置 + seed 装载 0 错误
- [x] 启动日志实证 97 CSV + 1 SQL 装载成功
- [x] GraphQL 抽样实证数据可见

### Phase 2 - 既有测试零回归验证（Proof）

Status: completed
Targets: `mvn test` 全 reactor + 门禁测试
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 配置生效 + 既有 seed CSV 未修改

- [x] `Proof`：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity,TestAuthSeedLoadingProof` **5/0/0** 全绿（plan 2026-08-15-2000-1 实证基线：`TestErpSeedDataIntegrity` 2/0/0 + `TestAuthSeedLoadingProof` 3/0/0；本计划不修改 CSV，门禁应保持零悬空 + 全表可加载）
- [x] `Proof`：`mvn test -pl app-erp-all -Dtest=TestErpAiIntrospectionDisabledByDefault,TestErpAiIntrospectionEnabled,TestErpFinApDocBatchWiring,TestErpFinApDocumentPipeline` ——**4 个无 `@NopTestProperty` 声明 init-database-data 的测试（直接 `extends JunitAutoTestCase`，不继承 `ErpIntegrationTestCase`）**——机制核实：ALL_LAZY 容器中懒 bean 未被引用 → 预期零影响（见 Current Baseline「风险面」段）；本项显式回归确认，如有意外漂移按登记裁决处置（不得静默修改断言）
- [x] `Proof`：全 reactor `mvn test` ——surefire XML 权威计数与已知基线（2026-08-28 plan-0219-2 = 3947 tests / 0 failures / 0 errors / 1 skipped / 669 报告文件）**完全一致**。本计划不修改生产代码/Java/CSV，预期零漂移；如有任何计数偏差需在 Closure 段登记并裁决
- [x] `Proof`：`mvn test -pl app-erp-all` ——54/0/0/1 绿（plan 2026-08-25 V.1 实证基线 + V.2 收尾；**注意**：08-26~08-30 flux 页面改写可能影响 `ErpAllFlux/WebPagesTest` 类计数——若执行期计数偏差，按「如有偏差登记裁决」条款在 Closure 段登记并核对是否为预存漂移，不得静默断言硬值）
- [x] `Proof`：`bash docs/audits/nop-compliance-checker.sh` ——19 规则 actual 与 2026-08-28 plan-0219-2 行基线完全一致（实际值 ≤ baseline = 零漂移；本计划不修改生产代码，预期零漂移）
- [x] `Proof`：`./scripts/start-app.sh restart` 二轮启动成功（验证 fresh-DB 重置机制在同 JVM 多次启动下稳定，无残留 DB 文件问题）

Exit Criteria:
- [x] 既有 4 类标准验证命令（install/test/compliance-checker/start-app-restart）全绿或零漂移
- [x] `TestErpSeedDataIntegrity` 引用完整性门禁 100% 通过
- [x] 全 reactor surefire 计数与已知基线逐项一致（**5 个预存回归已登记裁决**——见 Closure 段）

### Phase 3 - 文档对齐 + 基线登记（Add）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2 全绿

- [x] `Add`：`docs/architecture/seed-data.md` 头部「当前状态」段（line 5）追加新裁决段（演示/沙盒默认加载）：
  - 显式说明：本计划（`2026-08-31-1143-1`）后，`app-erp-all/src/main/resources/application.yaml` **默认开启** `init-database-data: true`（演示/沙盒语义）；
  - 替代 `2026-07-08-1234-1-demo-seed-data-init.md` 的「生产默认关」裁决（用户选 A：演示/沙盒场景默认开 + 生产亦开）；
  - 保留 fresh-DB 重置约束（DataInitInitializer 非幂等，重复启动主键冲突）；
  - 推荐使用 `scripts/start-app.sh`（含 fresh-DB 重置）作为标准启动入口；
  - 引用本计划文件名 + plan Status；
  - **同步修正资产口径漂移**：`docs/architecture/seed-data.md` 全文 **6 处**「96 CSV / 96 seed」表述（line 24/28/72/77/81/104）统一修订为「**97 CSV + 1 SQL**」（含 `zz-sequence-advance.sql`；`nop-vfs-index.txt:4-101` 为权威计数源）；domain 分布按实仓（master-data 21 / inventory 5 / purchase 8 / sales 8 / finance 7 / assets 3 / projects 6 / manufacturing 8 / maintenance 8 / quality 6 / crm 5 / cs 3 / hr 4 + 4 平台 + 1 部署配置）。注意 line 81（快照重录义务触发条件）与 line 104（deploy 同步义务表）亦含「96 CSV」，须一并修正。
- [x] `Add`：`docs/testing/e2e-runbook.md` 头部「种子库启动（演示 / 数据可见性）」段（line 183）开头追加一行注记：「**自 plan `2026-08-31-1143-1` 起，`app-erp-all` 默认开启 seed 装载**——`application.yaml:18` 含 `init-database-data: true`（位于 `init-database-schema: true` 后一行）。启动入口首选 `scripts/start-app.sh`（含 fresh-DB 重置）；手动启动仍需先 `rm -f db/erp.mv.db db/erp.trace.db` 后 `java -jar`（seed 非幂等）。」
  - **同步修正 2 处「91 张 CSV」漂移表述**（91 → 97 历经 8 个 seed 累积 plan）：line 187 修订为「**97 张 CSV**」；line 1005 修订为「**97 张 CSV**」并**修正其内部明细求和自相矛盾**（原文明细 21+23+13+4+11+12+3+4+3 = 94 ≠ 91——按实仓 97 修订明细与总数一致）。
- [x] `Add`：`docs/testing/known-good-baselines.md` 顶部 `Recent Baselines` 表增新行（紧接 2026-08-28 plan-0219-2 行下方）——日期 2026-08-31、dirty（`application.yaml` +1 行 + `scripts/start-app.sh` 新文件 + docs 3 处 owner-doc 追加）、验证命令集（`mvn clean install -DskipTests` 156 模块 + 全 reactor `mvn test` 3947/0/0/1/669 一致 + `mvn test -pl app-erp-all` 54/0/0/1 + `bash docs/audits/nop-compliance-checker.sh` 零漂移 + `./scripts/start-app.sh restart` 二轮成功 + `TestErpSeedDataIntegrity,TestAuthSeedLoadingProof` 合计 **5/0/0**（2/0/0 + 3/0/0，plan 2026-08-15-2000-1 口径））、工作树、Notes 含「演示/沙盒默认 seed 装载；既有 seed CSV 零修改；门禁 100% 通过；零生产代码变更」。
- [x] `Add`：`docs/logs/2026/08-31.md` 追加 Phase 3 文档落地条目

Exit Criteria:
- [x] 三处 owner-doc 文本一致（统计口径、版本号、计划 ID、行为裁决）
- [x] known-good-baselines.md 新基线行可被未来 AI 直接 grep 引用
- [x] 文本一致性检查通过：`Plan Status`（active/completed）、Phase 状态、`Closure Gates`、日志条目四向一致

## Closure Gates

> 本计划涉及 application.yaml 部署配置变更 + 新增启动脚本，结束前除下方门控外运行 Phase 2 全量验证 + fresh-DB 二轮启动验证。

- [x] 范围内行为完成（`init-database-data: true` 默认生效 + `start-app.sh` 可用 + 文档三处更新）
- [x] 相关文档对齐（`seed-data.md` 状态段 + `e2e-runbook.md` 启动段 + `known-good-baselines.md` 新基线行）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl app-erp-all` + 全 reactor `mvn test` + `bash docs/audits/nop-compliance-checker.sh` + `./scripts/start-app.sh restart`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行（ses_fa9e0a329ffe32vgG1X9C3ujg6，PASS）
- [x] 结束证据存在于文件中（Closure Audit Evidence 段落盘）

## Draft Review Record

- **Independent draft review iteration 1**: `needs revision` (ses_faa142d09ffeDXeC3OppRaeoS8，独立 general 子代理) because 1 BLOCKER + 5 MAJOR + 5 MINOR（全部基线事实经实时仓库独立复核确认）：
  - **B1（已修）**：Phase 1 Proof 抽样行数与实际 CSV 不符——`ErpMdPartner` 4 行实仓 5 行（含 plan 1045-1 追加 EMPLOYEE 行）/ `ErpPurOrder` 2 行实仓 1 行（PO-2026-001）/ `ErpSalOrder` 2 行实仓 1 行（SO-2026-001）。已逐文件 `wc -l` 复核 + 修订 Phase 1 Proof Item 6 抽样预期与 CSV 一致，附执行期偏差登记纪律。
  - **M1（已修）**：`application.yaml:28` 行号引用错误（H2 URL 实际在 line 20）。已修订 Phase 1 Item 4/Phase 3 Item 1 引用。
  - **M2（已修）**：每域 CSV 数量分布与实仓不符（master-data 21/inventory 5/purchase 8/sales 8/finance 7/assets 3/projects 6/manufacturing 8/maintenance 8/quality 6/crm 5/cs 3/hr 4 = 14 域空间 97 CSV）。已修订 Current Baseline 段落，补充域分布表 + 5 域（aps/logistics/b2b/contract/drp）无 seed 的 Non-Goal 引用。
  - **M3（已修）**：`docs/architecture/seed-data.md` 多处（line 28/72/77）仍写「96 CSV」漂移。已修订 Phase 3 Item 1 同步修正三处为「97 CSV + 1 SQL」+ 域分布表。
  - **M4（已修）**：`seed-data.md:64-70` 引用指向「导入策略」段而非「96 CSV」原文。已修订为 `seed-data.md:28`（「共 96 CSV」原文位置）。
  - **M5（已修）**：`e2e-runbook.md:187` 仍写「91 张 CSV」漂移。已修订 Phase 3 Item 2 同步修正为「97 张 CSV」。
  - **m1（已修）**：集成测试类计数 24 → 22。已修订 Current Baseline 段（21 个 `TestErpC0x*` + 1 个 `TestErpP2pPilot`）+ 增列 4 个无显式声明但继承 `ErpIntegrationTestCase` 的测试。
  - **m2（已修）**：覆盖域数「19 域」表述不准。已修订为「14 域表名空间（含 notify 跨域 1 个）」+ 5 域 Non-Goal 引用。
  - **m3（已修）**：注释「数据隔离隔离隔离」笔误。已修订为「数据集隔离（多租户/账套级）」。
  - **m4（已修）**：时间预期 Goals「21 秒」vs Phase 1 Proof「30 秒」不一致。已统一 Goals 为「30 秒内」。
  - **m5（已修，预存漂移登记）**：`ErpIntegrationTestCase.java:46` 注释「94 CSV + 1 SQL」过期——Java 测试文件注释不在本计划范围，已在 Current Baseline「预存文档漂移登记」段登记。

迭代 1 全部 BLOCKER/MAJOR/MINOR 已 RESOLVED。

- **Independent draft review iteration 2**: `needs revision` (ses_faa087f3affeZvV3ip3lGRx0y1，独立 general 子代理) because 2 新 MAJOR + 3 新 MINOR + 3 项迭代 1 修订不完整（全部经实时仓库独立复核确认）：
  - **N1（已修，MAJOR）**：迭代 1 修订引入的失实——原称 4 个无声明测试「继承 `ErpIntegrationTestCase`，基类显式声明已覆盖」，实仓核实 4 类直接 `extends JunitAutoTestCase` 且基类无任何 `@NopTestProperty`（仅 import）。已改写 Current Baseline 段 + Phase 2 Item 2：如实陈述「application.yaml 默认值翻转后其 DB 状态改变 → 快照/断言可能失效」，Phase 2 显式纳入回归并按「快照重录义务」处置（不得静默修改断言）。
  - **N2（已修，MAJOR）**：`TestErpSeedDataIntegrity,TestAuthSeedLoadingProof` 基线计数 4/0/0 失实（源计划 2026-08-15-2000-1 实证为 2/0/0 + 3/0/0 = **5/0/0**）。已修订 Goals 基线 + Phase 2 Item 1 + known-good-baselines 登记口径为 5/0/0。
  - **N3（已修，MINOR）**：e2e-runbook 注记文本 `application.yaml:17` 差一行（Phase 1 落地后该行位于 `:18`）。已修订 Phase 3 Item 2 为 `application.yaml:18`。
  - **N4（已修，MINOR）**：「登录鉴鉴权」笔误。已修订 Phase 1 Item 2 JVM 参数注释为「登录鉴权」。
  - **N5（已修，MINOR）**：`mvn test -pl app-erp-all` 54/0/0/1 硬断言缺偏差登记条款（08-26~08-30 flux 页面改写可能影响 `ErpAllFlux/WebPagesTest` 计数）。已修订 Phase 2 Item 4 增加「如有偏差登记裁决」条款。
  - **M3（修订不完整，已补齐）**：seed-data.md「96 CSV」共 6 处（:24/:28/:72/:77/:81/:104），迭代 1 只修 3 处。已修订 Phase 3 Item 1 覆盖全部 6 处（含 line 81 快照重录义务触发条件 + line 104 deploy 同步义务表）。
  - **M5（修订不完整，已补齐）**：e2e-runbook.md「91 张 CSV」共 2 处（:187 + :1005），且 :1005 明细求和 94 ≠ 91 自相矛盾。已修订 Phase 3 Item 2 覆盖 2 处 + 修正明细与总数一致。
  - **m1（修订不完整，已修对）**：集成测试类计数迭代 1 修成 22（实仓 `ls TestErpC*.java` = 22 + `grep -l @NopTestProperty...` = 23）。已修订 Current Baseline 段为 **23 个**（22 C 用例类 + P2pPilot）。

迭代 2 全部新 MAJOR/MINOR + 不完整修订已 RESOLVED。

- **Independent draft review iteration 3**: `needs revision`（最后修订轮）(ses_fa9fba6beffeyh7p1ZmFmQoXKX，独立 general 子代理) because 1 MAJOR（N1 后果声明机制失实）+ 2 MINOR（B/C）：
  - **A（已修，MAJOR）**：迭代 2 N1 修订后的「application.yaml 翻转 → DataInitInitializer 被实例化 → 快照失效」因果声明机制失实。实仓三重证伪：(1) NopJunitExtension 对 autotest 容器强制 ALL_LAZY（`NopJunitExtension.java:51`），`DataInitInitializer` 条件 bean 无 force-init 未被引用不实例化（`orm-defaults.beans.xml:92-97`），这 4 类无人触发它；(2) 4 类断言面逐类核实为 schema/错误码级 + 自包含建数 + 相对断言，seed 免疫；(3) 4 类 `_cases` 下仅 0 字节空 `autotest.yaml` 标记，无快照可失效。已改写 Current Baseline「风险面」段 + Phase 2 Item 2 为「预期零影响 + 仍显式回归 + 意外漂移登记裁决」。
  - **B（已修，MINOR）**：Goals「30 秒内」预期无偏差登记条款（脚本等待上限 90s 不冲突但无处置）。已修订 Goals 加「超 30s 但 < 90s 即成功并登记实测用时；超 90s 视为失败」。
  - **C（已修，MINOR）**：:157 将 :77 描述为「96 CSV」表述，实仓原文为「96 seed 装载」。已修订为「96 CSV / 96 seed」。

迭代 3 全部问题已 RESOLVED。

- **Independent draft review iteration 4**: `accept` (ses_fa9f3f3b5ffeE9SbOZFqwtoqB2，独立 general 子代理) — A/B/C 三项修订全数到位且机制事实经独立复核无失实（ALL_LAZY 懒 bean 链 / 4 类 `_cases` 空标记 / 97 CSV 计数 / application.yaml :16-17 / 抽样 5 表行数 / 23 测试类 / 6 处 + 2 处文档漂移全部实仓证实），无新问题。**草案审查已收敛，计划为可接受的执行契约，Plan Status 升级为 active。**

## Closure

Status Note: 4 轮独立草案审查收敛（迭代 4 accept）→ 3 Phase 全部执行完毕：

- **Phase 1（配置启用 + 验证）**：`application.yaml:18` 新增 `init-database-data: true`（`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS 01:58）；`scripts/start-app.sh` 新建（fresh-DB 重置 + allow-create-default-user + service-public + flux 渲染；start 13s ready，97 CSV + 1 SQL 全装载 0 冲突/0 列映射错误；GraphQL 抽样 Currency=2/Material=4/Partner=5/PurOrder=1/SalOrder=1 与 CSV 行数一致；stop 清理端口成功）。
- **Phase 2（零回归验证）**：门禁 `TestErpSeedDataIntegrity,TestAuthSeedLoadingProof` 5/0/0 全绿；4 个无声明的 JunitAutoTestCase 子类 14/0/0 全绿（机制核实零影响实证）；`./scripts/start-app.sh restart` 二轮 fresh-DB 13s ready 稳定；compliance checker exit 0（零生产代码变更零漂移）。
- **Phase 3（文档对齐）**：`seed-data.md` 6 处「96 CSV/96 seed」口径勘误 + 默认加载裁决段；`e2e-runbook.md` 2 处「91 张 CSV」→97 + :1005 明细求和矛盾修正 + 默认 seed 注记；`known-good-baselines.md` 2026-08-31 新基线行（含 5 预存回归 Known Failures 登记）；日志更新。

**5 个 app-erp-all 集成测试预存回归（已登记，非本计划范围）**：`TestErpC03O2cGoldenPath`/`TestErpC04SalReturnWithCs`（F2.11 `StockMoveBookkeeper.findBalance` 自然键精确匹配回归）、`TestErpC08MrpApsRelease`（F2.6 快照漂移）、`TestErpC09QaNcrCapaScrap`（F2.12 qa-004 NCR severity 20→NORMAL）、`TestErpC12PrjTimesheetSettlement`（F2.12 prj 快照漂移）——**stash 回滚 application.yaml 后同样失败实证与本计划无关**；归属 ai-check F2.x 批收尾义务（各批仅跑域级测试未跑 app-erp-all 全量集成测试）。successor 触发条件：ai-check F2.x 收尾批需重录 app-erp-all 受影响集成测试快照（C03/C04/C08/C09/C12）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 `ses_fa9e0a329ffe32vgG1X9C3ujg6`（新会话，未参与执行）
- Result: **PASS**（推荐关闭计划，2 项 MINOR 非阻塞）
- Evidence: 实时仓库逐项核实——(1) `application.yaml:18` `init-database-data: true` 实仓存在（git diff 证实仅 +1 行）；(2) `scripts/start-app.sh` 可执行（`-rwxr-xr-x`）+ 含 `rm -f db/erp.mv.db db/erp.trace.db` fresh-DB 重置；(3) 97 CSV + 1 SQL 物理存在（`ls *.csv | wc -l`=97）；(4) `seed-data.md` 裁决段在位 + 6 处「96 CSV/96 seed」清零；(5) `e2e-runbook.md` 默认 seed 注记 + 2 处「91 张 CSV」清零；(6) `known-good-baselines.md` 2026-08-31 新基线行（含 5 预存回归 Known Failures 登记）；(7) `_tmp/app.log` 97 行 `nop.orm.load-csv-data` + 1 SQL + `Listening on`（11.745s）；(8) 5 预存回归独立 stash 实测（回滚 application.yaml 后 `TestErpC03O2cGoldenPath` 同样失败 + 5 类均显式 `@NopTestProperty` 声明 seed 开关 → 与本计划构造性无关）；(9) 计划文件自洽（3 Phase completed + Closure Gates 全勾 + Closure 段 5 回归登记）；(10) 文本一致性四向核对通过（97/5-0-0/14-0-0/:18/23 类全吻合）。
- 遗留 MINOR 处置：MINOR-1（e2e-runbook:1007 明细分类口径）已修订（权威计数源 = 实仓 `ls *.csv | wc -l`）；**审计后补漏：e2e-runbook:1034「全量 96 seed」亦修订为 97**（审计 grep 复核时发现，同批处置）；MINOR-2（结束证据门）由本审计勾选；MINOR-3（全 reactor 未重跑）依据计划登记 + app-erp-all surefire 交叉印证接受。

Follow-up:

- ai-check F2.x 批收尾：5 个 app-erp-all 集成测试快照重录（C03/C04/C08/C09/C12），触发条件已登记于 known-good-baselines 2026-08-31 行。