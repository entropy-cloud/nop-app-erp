# 集成测试用例设计（系统级黄金路径回归套件）

> **来源**：`docs/backlog/integration-test-roadmap.md` M0.1 工作项规格（v3）
> **状态**：M0.1 定稿（经独立子代理审查 ≥2 轮收敛，审查记录见 `docs/plans/2026-08-23-1835-1-m01-integration-test-case-design.md` Draft Review Record 与本文件 §10）
> **最后更新**：2026-08-23
> **适用**：M0.2 基建试点（机制裁决逐项实证）→ M0.3 分批展开（B1-Bn）→ V.1/V.2 全量验证与收尾

---

## 1. 目的与范围

本设计文档为 `nop-app-erp` 的**系统级黄金路径集成测试套件**提供全量用例设计：

- 在 **app-erp-all 单模块**统一装配，覆盖全 **19 子系统**（18 业务域 + notify 跨域通知派发）。
- 每用例跨 **3+ 域**，涉及**审批 / 过账 / 状态机**关键路径至少其一。
- 测试基于 **IGraphQLEngine + nop-autotest 录制回放**（三层全比对：response 快照 + DB 状态快照 + JUnit 关键断言）。
- 与既有资产分工：不重复 12 个 `*EndToEnd` 集成测试类的内部细节（业务参照）；不与 Playwright E2E（260+ spec，浏览器层）重复页面层验证。
- **以当前实现为准**：用例设计不含 requirement-compliance RC-R1.44+ 未落地项与 entity-state-machine M5 未落地项；不含未实现功能的用例（roadmap 已裁决 out-of-scope）。

**Non-Goals**（本设计文档）：

- 不写任何测试代码、不动 app-erp-all 构建配置（归 M0.2）。
- 不向 M1-Bn 里程碑追加工作项（归 M0.3，本文件 §8 给出分批建议供 M0.3 引用）。
- 不改 seed CSV、不改生产代码、不接 CI。

---

## 2. 当前基线盘点（机制实证素材）

以下事实全部经 2026-08-23 实仓核实（证据行号为本设计文档的机制论证引用锚点）：

| # | 事实 | 实证 | 证据 |
|---|------|------|------|
| B1 | app-erp-all 单模块 **12 个测试类**全为基建类（auth 4 / web 5 / seed 1 / meta 1 / job 1），**零业务集成测试** | `app-erp-all/src/test/java/` 文件清单（2026-08-23 实仓 find） | roadmap 计数「11 个」为漂移，本设计以实仓为准 |
| B2 | 部署期 seed：`app-erp-all/src/main/resources/_vfs/_init-data/` **94 CSV + 1 SQL**（`zz-sequence-advance.sql`） | 2026-08-23 实仓计数 95 文件 = 94 csv + 1 sql | seed-data.md |
| B3 | 既有业务参照：**12 个 `*EndToEnd` 集成测试类**（fin 3 / inv 2 / mfg 3 / pur 1 / qa 1 / sal 2） | 实仓 grep `extends JunitAutoTestCase` 类名 `TestErp*EndToEnd` | 单用例步骤与断言的业务参照，不重复实现其内部细节 |
| B4 | `JunitAutoTestCase` 硬编码 `@ExtendWith({NopJunitExtension.class, NopJunitParameterResolver.class})` | `../nop-entropy/nop-autotest/nop-autotest-junit/src/main/java/io/nop/autotest/junit/JunitAutoTestCase.java:29` | 机制风险 ① 的源码锚点 |
| B5 | 缺 `@NopTestConfig` 抛 `IllegalArgumentException`（"Classes inheriting from JunitAutoTestCase must be annotated with @NopTestConfig"） | 同上 `:83-85` | JunitAutoTestCase 类级注解强制 |
| B6 | CHECKING 分支硬编码 `setLocalDb(true)` + `setTableInit(true)` + `setSqlInput(true)` + `setSqlInit(true)` | 同上 `:117-126` | CHECKING 态 DB 组成（风险 ②）源码锚点 |
| B7 | RECORDING 分支 `setLocalDb(testConfig.localDb())` + `setTableInit(false)`（不自动装载 input/tables） | 同上 `:110-116` | 录制态 DB 由注解 `localDb` 决定 |
| B8 | `configLocalDb()`：`localDb=true` → 强制 in-memory H2（`jdbc:h2:mem:` + 随机 UUID） | `../nop-entropy/nop-autotest/nop-autotest-core/src/main/java/io/nop/autotest/core/AutoTestCase.java:190-197` | per-method 独立内存库 |
| B9 | 每测试方法 `container.restart()`（"每个单元测试函数都要使用单独的数据库和bean环境"） | 同上 `:211-225`（initBeans） | per-method 隔离（风险 ③）源码锚点 |
| B10 | `AutoTestCaseDataBaseInitializer.createTables()` **只建 input/output 文件涉及的表**（"输入和输出所涉及到的表都需要新建"） | `../nop-entropy/nop-autotest/nop-autotest-core/src/main/java/io/nop/autotest/core/execute/AutoTestCaseDataBaseInitializer.java:90-109` | 回放态 DB ≠ 全量 94 seed（风险 ② 核心机制） |
| B11 | 快照规模澄清：`output/tables` 只写**变更行**（非全库）；`input/tables` 只写 **ORM 装载行** | `AutoTestCaseDataSaver` / `OrmModelHelper` / `TagVarCollector`（nop-autotest-core，同目录源码）；testing.md「快照测试的默认工作流」 | 「三层全比对快照巨大」担忧不成立，规模受用例触碰数据量约束 |
| B12 | 父 POM surefire：`forkCount=4 + reuseForks=true + parallel=classes + threadCount=1` | `../nop-entropy/pom.xml` surefire 段（2026-08-23 实仓） | 文件型 H2 并行 fork 竞态（BLOCKER 级，M0.2 必须解决） |
| B13 | app-erp-all 无模块级 surefire 覆盖（pom 仅 `systemPropertyVariables`） | `app-erp-all/pom.xml` build 段 surefire 插件配置 | 父 POM 并行配置对 app-erp-all 生效 |
| B14 | 宿主模式先例：`TestAuthSeedLoadingProof`（BaseTestCase + 手动 `CoreInitialization.initialize()` + 文件 H2 fresh 清理 + `setTestConfig` schema/data 开关） | `app-erp-all/src/test/java/io/nop/app/all/auth/TestAuthSeedLoadingProof.java:37-56` | 全量 94 seed 装载的宿主模式机制（绕开 NopJunitExtension ALL_LAZY 时序） |
| B15 | xwf 审批流限制：Payment/Receipt `submitForApproval` 被 wf 步骤参与者 `user:$0`（SYS id=0）拒绝 | `docs/plans/2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility.md`（权威裁决 NOT FEASIBLE，浏览器层） | 用例设计必须以当前实现为准，只选用已验证可执行的审批流（见 §5） |
| B16 | 全仓测试基线：3808 tests / 0 failures / 0 errors / 1 skipped（唯一 skipped = `ErpAllWebPagesCollectTest` `@Disabled` 预存 JDK26/ANTLR H-2）；156 reactor 模块 BUILD SUCCESS | `docs/testing/known-good-baselines.md` 2026-08-23 全量基线条目 | 权威计数源（不在本文复制内联计数，引用指针） |
| B17 | 后端 xwf 链可用 `setUserId("0")` 绕过 user:$0 拦截（既有后端测试实证） | `TestErpHrSalaryWorkflowApproval`（submit→3 级 agree→APPROVED）；`TestErpPurPaymentWorkflowApproval`（pur-service） | xwf 规避清单的判定依据（§5） |

---

## 3. 测试机制选型论证

### 3.1 三个选项

**选项 (a) 标准 input/tables 范式** —— 完全沿用 `JunitAutoTestCase` 标准生命周期：

- 录制期：`@NopTestConfig(localDb=true, snapshotTest=RECORDING)` → in-memory H2（per-method UUID 库，B8/B9）；94 seed 在**测试方法内**经 DataInitInitializer 镜像装载器（拓扑序，镜像 `DataInitInitializer.loadCsvData` 逻辑，先例 `TestErpSeedDataIntegrity`）装载入内存库作**录制期富集**；录制产物 = `input/tables`（ORM 装载行）+ `output/tables`（变更行）+ `response.json5`。
- 回放态（CHECKING）：in-memory H2 + 只建 input/output 涉及表（B10）+ `input/tables` 快照恢复（B6）。
- 优点：零平台对抗；391 测试类既有先例；per-method 天然隔离（B8/B9）；快照机制/tooling（force-save-output、@var、diff）全复用。
- 缺点：**回放态 DB ≠ 全量 94 seed**（风险 ② 常态化）——用例依赖的部署 seed 行若未被 ORM 装载则不会进入 `input/tables`，回放可能缺行；缺口须逐用例补录 `input/tables`（自包含追加），形成「部署 seed ↔ 用例快照」双份数据源，seed 变更时双面漂移（风险 ④ 放大）。

**选项 (b) 自研基类** —— 宿主模式（B14）+ 自实现 AutoTestOrmHook 级 DB 采集 + 自实现三层比对：

- 优点：完全控制（全量 94 seed 文件 H2 前置态；无 NopJunitExtension；无 CHECKING DB 组成问题）。
- 缺点：重复实现平台机制（OrmHook 采集 / @var 替换 / force-save-output / diff 比对），数百行自研代码 + 长期维护；与平台快照语义分歧风险高；丢失既有 391 先例的工具链与心智模型。**成本与风险不成比例于收益（收益被选项 (c) 以更低成本获得）→ 被拒。**

**选项 (c) 抑制 tableInit 的文件 H2 双模方案** —— app-erp-all 新增测试基类 `ErpIntegrationTestCase extends JunitAutoTestCase`：

- **DB 恒为文件 H2**（application.yaml `jdbc:h2:./db/erp`）：子类 override `configExecutionMode`（protected，B5/B6 所在类的同文件方法）强制两模式均 `setLocalDb(false)` + `setTableInit(false)`——CHECKING 不再强制 in-memory（B6 被覆盖），RECORDING 不再走内存库（B7 被覆盖）。
- **前置态**：`@BeforeAll` 宿主式初始化（B14 先例）：fresh-DB 清理（`rm db/erp.mv.db` + `.trace.db`）+ `setTestConfig("nop.orm.init-database-schema", true)` + `setTestConfig("nop.orm.init-database-data", true)` + `CoreInitialization.initialize()` → **全量 94 seed 为两种模式的统一前置态**。
- **录制/校验机制保持平台原生**：response.json5 + output/tables + @var 替换 + force-save-output 全复用；`input/tables` 仅承载**用例自包含追加行**（用例新建的数据，非部署 seed 复制）。
- **每类 1 测试方法约定**（§4）：`container.restart()` 每类恰 1 次（B9），文件 H2 每类 fresh 重建，类间零污染。
- **surefire 串行化**（M0.2 承诺）：app-erp-all 模块级 `forkCount=1`/`parallel=none`，消除并行 fork 对共享文件 H2 的竞态（B12/B13）。
- 优点：**回放态 DB = 部署 seed 真相源**——「测试跑的就是部署态」，系统级黄金路径语义成立；用例快照仅承载自包含追加（规模最小化，B11）；三层比对机制零自研；种子变更敏感性收敛为 output/tables 重录单面（风险 ④ 缓释）。
- 缺点/残留风险：override 平台 protected 方法（nop-entropy 版本冻结缓解兼容风险）；宿主式初始化 × NopJunitExtension 类继承共存**无既有证据**（风险 ①，M0.2 判据）；文件 H2 与 E2E live server 运行时互斥（V.2 登记纪律，roadmap 已知约束 5）。

### 3.2 选定方案与回退路线

**选定方案 (c) 抑制 tableInit 的文件 H2 双模方案**（首选）：

- 选择理由：系统级黄金路径的本质 = 「在部署态数据上跑跨域业务闭环」——(c) 使回放态与部署 seed 同一真相源，语义最忠实；(c) 以最小自研（一个基类 override）获得 (b) 的全部收益；(c) 的快照规模与维护面收敛（input/tables 仅自包含追加行），种子变更敏感性单面化。
- 考虑的替代方案：(a) 标准范式——回放态 = input/tables 快照 ≠ 部署 seed，黄金路径语义打折 + 双份数据源漂移，作为**失败回退**保留（若 M0.2 试点证伪 (c)）；(b) 自研基类——重复实现平台机制，成本不成比例，**被拒**。
- 残留风险：风险 ①（共存无先例）与风险 ③（文件 H2 × 并行）必须在 M0.2 试点逐项实证；证伪即降级 (a) 并调整用户裁决口径（roadmap M0.2 失败回退路线，须人工裁决登记）。

### 3.3 四风险判定方法（M0.2 可直接逐项实证）

**风险 ① NopJunitExtension 共存**（宿主式初始化 × JunitAutoTestCase 继承）：

- 判定方法（M0.2 试点 1）：创建 `extends ErpIntegrationTestCase` 的试点类，`@BeforeAll` 宿主式初始化（B14 完整路径），随后走 JunitAutoTestCase per-method 生命周期（B9 restart）。三个通过判据：(1) 无 ALL_LAZY schema 时序问题复现（`DataBaseSchemaInitializer` 的 @PostConstruct 先于 DB 访问 bean 运行）；(2) `container.restart()` 后 seed 数据仍可经 DAO 访问（不触发重新初始化/不丢文件 H2 连接）；(3) RECORDING→CHECKING 往返绿。
- 失败判定：任一判据不成立 → (c) 方案证伪，降级 (a)（录制期改 in-memory + 方法内 seed 装载，绕开宿主式初始化）。

**风险 ② CHECKING 态 DB 组成**（回放态 ≠ 全量 seed）：

- 判定方法（M0.2 试点 2）：RECORDING 完成后检查录制产物——逐用例核验「依赖的部署 seed 行是否全部在位」：(1) 对每个用例的 FK 依赖图（from 用例前置规格），在回放态（文件 H2 全量 seed，(c) 方案下天然成立）或 input/tables（(a) 方案下须核验）中确认目标行存在；(2) CHECKING 回放跑绿 = 组成自洽证明；`output-row-not-exists`/FK 缺失失败 = 组成缺口，逐表补录（(a) 方案下走 input/tables 自包含追加，不动部署 seed——种子只能追加纪律）。
- (c) 方案下的附带判据：回放态 = 部署 seed 全量（DataInitInitializer 装载），**该风险结构性降级**，仅需验证 seed 装载在测试上下文（非 Quarkus 启动上下文）下与部署启动等价（同 loader、同拓扑序、同 94 CSV + 1 SQL）。
- 快照规模判据（两方案通用）：`output/tables` 只写变更行 + `input/tables` 只写 ORM 装载行（B11）——试点 2 实测 1 个 3+ 域用例的快照体积与文件数，登记「单用例快照规模参考值」。

**风险 ③ per-method `container.restart()` × 文件 H2**：

- 判定方法（M0.2 试点 3）：(1) 验证 app-erp-all surefire 串行化配置落地（模块级 `forkCount=1` + `parallel=none`，覆盖父 POM B12/B13）——`mvn test -pl app-erp-all` 两个文件 H2 测试类并行执行竞态归零（观测点：删除/初始化/写库竞态、`Table not found`/`Table already exists` 类错误）；(2) 每类 1 方法约定下 `container.restart()` 每类恰 1 次，文件 H2 状态生命周期 = @BeforeAll 清理 → initialize → 1 方法执行 → @AfterAll destroy，无跨方法残留；(3) 若未来类内多方法，须验证 restart 后文件 H2 是否保留（预期保留，JVM 级文件库），并登记 per-method fresh 清理义务。
- 失败判定：串行化后仍竞态 → 检查是否残留非文件 H2 的其他共享状态；若 (c) 方案被证伪，此风险在 (a) 方案下结构性消失（in-memory per-method）。

**风险 ④ seed 变更敏感性**：

- 判定方法（M0.2 试点 4）：(1) 敏感性度量——对任一 seed 行，grep 该行主键值于 `_cases` 快照（input/tables + output/tables + response.json5），统计依赖用例数；(2) 变更影响预算——试点执行 1 次「seed 追加一行」与「seed 修改一行」，分别登记波及的 _cases 目录数与重录耗时，作为 B1-Bn/V.1 执行期预算；(3) 变更义务——任何 seed CSV 变更须同步重录受影响用例快照（roadmap 横切关注点 1 双面义务：既有各域测试快照 + 新集成用例快照）；(4) 缓释设计——用例对部署 seed 的引用尽量经业务标识（partner code/material code）设计前置规格；快照层按 id 不可避免，重录义务为最终兜底；(5) V.2 将义务登记 `docs/architecture/seed-data.md`。
- 失败判定：无——本风险为义务性风险（可管理），不以证伪论；若波及面超出可维护阈值（试点实测登记），升级为设计评审（考虑 (a) 方案的 seed 复制隔离或缩小 seed 依赖面）。

---

## 3.4 M0.2 试点机制实证结论（2026-08-23，机制裁决）

> 试点载体：`app-erp-all` 新增基类 `ErpIntegrationTestCase` + 试点用例 `TestErpP2pPilot`（P2P 简化链
> PO→Receive→Invoice，12 个 GraphQL 动作 + 三层全比对），执行 RECORDING→CHECKING 往返；逐项实证
> §3.3 待证风险清单。详细证据（命令/文件计数/时间）见 plan `docs/plans/2026-08-23-1835-2-m02-infra-pilot-mechanism-adjudication.md` Phase 3。

**机制 (c) 成立（Decision，无回退）**：试点往返全绿（RECORDING 录制 → CHECKING 复跑 0 失败），
三层全比对（response 快照 + output/tables 变更行 + JUnit 关键断言）在文件 H2 全量部署 seed 前置态下
完全可用。回退路线（降级标准范式 (a)）未触发。

**五风险逐项结论**：

| 风险 | 实证方式 | 结论 | 影响 |
|------|---------|------|------|
| ① NopJunitExtension 共存 | 试点类 extends 基类 + NopJunitExtension 生命周期全走通；判据 3 条全过 | **成立**。ALL_LAZY 下 schema 由 force-init `DataBaseSchemaInitializer` 幂等创建（`ioc:force-init="true"` + `ioc:after="nopOrmSessionFactory"`）；`DataInitInitializer` 为惰性 bean（无 force-init，ALL_LAZY 不自动跑），基类在 per-method `container.restart()` 后显式 `BeanContainer.getBeanByType(DataInitInitializer.class)` 触发装载 | 基类装配固定为该模式；「宿主式 @BeforeAll init」字面配方不适用（NopJunitExtension.beforeAll 先启动容器），以显式触发惰性 bean 等价实现 |
| ② CHECKING 态 DB 组成 | 回放态 = 文件 H2 全量 94 seed（部署同源 loader）；CHECKING 复跑零 `output-row-not-exists`/FK 缺失 | **结构性降级确认**：回放态 = 部署 seed 真相源，(c) 方案下该风险不再构成缺口；input/tables 仅承载用例自包含追加（试点未用，表装载被双模抑制） | B1-Bn 用例前置一律按「seed 引用 + 自包含 GraphQL 建数」设计，无需补录 input/tables |
| ③ per-method restart × 文件 H2 | 试点类 + 全量 13 测试类同一 fork 顺序执行全绿；restart 后 seed 仍可经 DAO 访问 | **成立**。`container.restart()` 保留文件库（JVM 级文件 H2），seed 落库后跨 restart 存活；fresh-DB 每类 1 次 = initBeans 删除 `.mv.db/.trace.db` + restart 重建 + seed 重灌（1 类 1 方法约定下每类恰 1 次） | 类内多方法时须登记 per-method fresh 清理义务（B1-Bn 默认 1 类 1 方法不触发） |
| ④ 快照构成与体积 | 试点 4 域链实测：output/tables 16 表 CSV（全变更行，`_chgType` A/D 标记；时间戳列 `*` 通配 + 时序列 `@var:` 引用），response 12 文件，用例目录合计 252K | **构成 = 变更行非全库确认**（`erp_fin_voucher` 仅 2 行新增凭证、`erp_fin_ar_ap_item` 仅 1 行等）；体积受用例触碰数据量约束，量级 KB 级，无噪声不可比问题 | 规模担忧不成立；seed 变更敏感性收敛为 output/tables 单面重录（§3.3 风险 ④ 判定方法 (2) 执行期预算：试点无 seed 修改，未测重录耗时——V.1 前以单用例 252K/类量级登记） |
| ⑤ 单用例耗时 | 试点 CHECKING 实测：suite 6.07s（容器启动 + fresh seed 装载 ~4s + 用例体 1.09s + 校验）；既有全量 13 测试类同一 fork 全绿 | **远低于 §4 模型**（模型 10-60s/类）：12 动作链 1.09s，含 seed 装载 6s/类量级 | 22 用例套件总耗时模型可下调（V.1 实测登记预期总耗时），串行执行上界收窄 |

**附加实证发现（B1-Bn 编写纪律）**：

- **@var 自动注册对 ERP 实体主键不成立**：ERP `id` 列 `tagSet="seq-default"`（非平台 `seq`），不会被快照框架
  自动注册为 `@var:Entity@id`；仅 `var` 标签列（如 `code`）与 `clock` 列自动注册（实测 `@var:ErpPurOrder@code`
  / `@var:ErpPurOrder@businessDate`）。多步 id 传递 = 响应提取 + `addVar` 供 request 文件 `@var:xxx` 引用
  （对齐 `TestErpMdPartnerCrudSmoke` 先例；设计文档 §4「ORM 主键自动注册 @var」表述修正为上述口径）。
- **request 文件静态载荷 + 动态 id 分离**：save/submit/approve 等动作的静态载荷放 `input/N_step.json5`，
  动态 id 经 `addVar` + `@var:xxx` 引用（与 CHECKING 确定性兼容：fresh-DB + 序列推进 `zz-sequence-advance.sql`
  使测试产物 id 每轮确定性一致）。
- **串行化既有测试回归修复（surefire 单 fork 暴露的两类宿主模式跨类污染）**：NopJunitExtension 测试类遗留
  动态配置（in-memory datasource URL / ALL_LAZY 容器启动模式——其 `AppConfig.getConfigProvider().reset()`
  仅自身 beforeAll 执行）会令后续宿主模式（`TestAuthSeedLoadingProof`/`TestErpSeedDataIntegrity`）seed 不装载；
  `ConfigStarter.doStop` 销毁 VFS 但不 unregister 会令手动初始化类（`TestAppActionAuthMerge`/`TestErpDataAuthStructure`）
  资源缺失。4 个既有测试类已加最小修复（reset + ALL_EAGER 回置 / VFS 残留注销），串行 fork 下全量 29/0/0/1 绿。

---

## 4. 用例粒度与规模约定

**约定（Decision）**：

- **1 用例 = 1 测试类 1 测试方法**；`@BeforeAll` 类级初始化（fresh-DB 清理 + 宿主式初始化 + 全量 seed 装载，每类 1 次）；`@AfterAll` 销毁。
- 多步动作经 `request("N_step.json5")`/`output("N_step_response.json5")` 编号文件 + @var 变量机制（M0.2 实证：ERP 实体 `id` 列 `tagSet="seq-default"` 不被自动注册，仅 `var`/`clock` 标签列自动注册为 `@var:Entity@prop`——多步 id 传递 = 响应提取 id + `addVar` 供后续 request 文件 `@var:xxx` 引用，见 §3.4 附加实证发现；禁止直接内联动态 id 字面量）。
- JUnit 关键断言层 = 显式锚点（状态翻转 / 金额与借贷平衡 / 成功失败 / 余额正确性），叠加快照两层（response + DB 状态），构成三层验证。

**预计耗时模型**：

- 纯启动：22 类 × ~12s/类 ≈ **4-5 分钟**（roadmap M0.2 规格口径「15-25 类 × ~12s 启动 ≈ 3-6 分钟纯启动」为同一模型的区间表述）。
- 单用例执行：每类 10-60s（跨域链 20-60 GraphQL 动作 + seed 装载（含 schema 创建，参考部署期 fresh 启动 11-13s 实测）+ 快照录制/比对）。
- 22 用例套件总模型 ≈ **8-26 分钟**（22 × (12s 启动 + 10-60s 执行)，中位 ~15 分钟）；V.1 实测登记预期总耗时（roadmap M0.2 规格同口径）。
- 串行化后（B12/B13 覆盖）无并行加速收益，该模型即执行时上界。

---

## 5. xwf 限制规避清单（已验证可执行审批流集合）

**背景**：`useWorkflow="true"` 仅 4 实体（`ErpPurPayment` / `ErpSalReceipt` / `ErpAstDisposal` / `ErpHrSalary`，ORM 实测）；其 xwf 审批轴浏览器层 **NOT FEASIBLE**（`user:$0` 步骤参与者拦截，2330-1 权威裁决，B15）。

**已验证可执行审批流集合**（用例设计引用）：

| 审批流 | 机制 | 执行性证据 | 用例采用 |
|--------|------|-----------|---------|
| **DIRECT use-approval 轴**（`approval-support.xbiz` 标准 5 动作 `submitForApproval`/`approve`/`reject`/`reverseApprove`/`withdrawApproval`） | 平台 approval-support 状态机，无 wf 参与者限制 | E2E orchestration/business-actions 全套实证（P2P/O2C/Return/WorkOrder/Recall/ASN/Contract/DRP 等用例直接审批可达）；JUnit 大量先例（`submitForApproval` 实仓分布于 8 模块 79 文件，use-approval 实体直接审批可达） | **默认采用**（用例审批步骤首选） |
| **xwf 轴（后端 `setUserId("0")`）**：ErpHrSalary（submit→3 级 agree→APPROVED）、ErpPurPayment、ErpSalReceipt、ErpAstDisposal | wf 参与者 `user:$0` = SYS(id=0)，后端测试显式设调用者 user 为 "0" 即匹配 | `TestErpHrSalaryWorkflowApproval`（submit→3 级 agree→APPROVED）、`TestErpPurPaymentWorkflowApproval`（pur-service）实证 | **条件采用**：用例设计默认**排除 Payment/Receipt `submitForApproval` 的 `user:$0` 被拒路径**；需触及 Payment/Receipt 状态推进的用例优先以 seed 已过账态为前置；确需驱动审批轴时显式标注 `setUserId("0")` 规避依据，且须经 M0.2 试点复核（app-erp-all 装配下与 (c) 方案共存）后方可启用 |

**用例设计规则**：

1. 审批步骤默认走 DIRECT 轴，标注 `[审批轴: DIRECT]`。
2. 涉及 xwf 实体（Payment/Receipt/Disposal/Salary）的步骤标注 `[xwf 规避依据: ...]`——或为「seed 已过账态前置」（posted=true + voucher 回链，roadmap 横切关注点 3 授权路径），或为「setUserId("0") 后端驱动（M0.2 复核）」。
3. **排除路径**（用例不得依赖）：非 SYS 用户调用 Payment/Receipt `submitForApproval`（user:$0 被拒，B15）——用例前置/步骤设计若隐含该路径即不合格。
4. 若 M0.2 试点证伪 setUserId("0") 在 app-erp-all 装配下不可用：Payment/Receipt 审批轴从用例移除，改 seed 直置 posted 态（横切关注点 3 授权），并在用例规格登记修订。

---

## 6. 用例全量设计（22 用例）

> **用例编号**：C01-C22（C20 拆分为 C20a/C20b 两独立用例；C21 为批 B4 的 aps 用例，编号置于 C09 之后由批序决定，非严格单调递增）。每用例六要素：**业务目标 / 前置 seed 或自包含数据 / 关键路径步骤（GraphQL 动作序列）/ 三层断言 / 主导域（批次归属）/ 涉及域**。复杂度判据（跨 3+ 域 + 审批/过账/状态机至少其一）逐用例标注。
> **图例**：`[审批轴: DIRECT]` = 平台 approval-support 直接审批；`[xwf 规避依据: …]` = xwf 实体步骤的规避说明（§5）；`[自包含]` = 用例自建数据（input/tables 承载）；`[seed]` = 依赖部署 94 seed 前置态。
> **动作名以当前实现为准**：步骤中动作名均经实仓 BizModel `@BizMutation`/E2E 先例核实（如 `ErpPurOrder__submitForApproval`、`ErpFinAccountingPeriod__closePeriod`、`ErpB2bAsn__createReceiveFromAsn`、`ErpDrpPlan__runDrp` 等）；实施时以 M0.2 试点 GraphQL schema 校验为准。

### 批 B1（主导域 purchase）

#### C01 P2P 采购到付款黄金路径

- **业务目标**：采购到付款（P2P）核心闭环——订单审批 → 收货 → 发票 → 付款，验证审批状态机翻转、过账凭证生成、AP 核销与 GL 余额联动。复杂度判据：跨 4 域 + 审批 + 过账 ✓。
- **前置**：`[seed]` 主数据（SUP-001 供应商、MAT-001 物料、CNY 币种、科目表、期间 OPEN）+ `[自包含]` 新采购订单（引用 seed 供应商/物料）。
- **关键路径步骤**：
  1. `ErpPurOrder__save`（自包含 PO，2 行物料）→ `ErpPurOrder__submitForApproval` → `ErpPurOrder__approve` `[审批轴: DIRECT]`
  2. `ErpPurReceive__save`（引用 PO）→ `ErpPurReceive__submitForApproval` → `ErpPurReceive__approve` `[审批轴: DIRECT]`（触发入库移动）
  3. `ErpPurInvoice__save`（引用 PO/收货）→ `ErpPurInvoice__submitForApproval` → `ErpPurInvoice__approve` `[审批轴: DIRECT]`（触发 AP_INVOICE 凭证 + AP 辅助账）
  4. `ErpPurPayment__save` → 付款审批推进 `[xwf 规避依据: 默认 setUserId("0") 后端驱动（TestErpPurPaymentWorkflowApproval 实证，M0.2 复核）；备选 = seed 已过账付款前置（posted=true + voucher 回链）——两路径二选一，实施前在用例规格定一]` → 付款核销 AP（openAmount → 0）
- **三层断言**：
  - 层 1（JUnit 关键断言）：PO/Receive/Invoice `approveStatus=APPROVED`；Invoice `posted=true`；AP_INVOICE 凭证借贷平衡（总借 = 总贷）；AP 辅助账核销后 `openAmount=0`；GL 余额（应付账款科目）与 AP 总额一致。
  - 层 2（response 快照）：每步 `output("N_step_response.json5")`。
  - 层 3（DB 状态快照）：自动录制 output/tables 变更行（PO/Receive/Invoice/Payment + voucher + voucher_line + voucher_bill_r + ar_ap_item + gl_balance）。
- **主导域 / 涉及域**：purchase / purchase, inventory, finance, master-data。

#### C02 采购退货与退款闭环

- **业务目标**：采购退货闭环——退货审批触发反向出库 + 红字凭证 + 负 AP 辅助账，退款核销。复杂度判据：跨 4 域 + 审批 + 过账 ✓。
- **前置**：`[seed]` 既有已过账采购链（erp_pur_invoice posted + AP 项 OPEN）+ `[seed]` 采购合同（退货条款依据，erp_ct_contract）+ `[自包含]` 退货单。
- **关键路径步骤**：
  1. `ErpCtContract__get`（读取合同退货条款）→ 前置核对
  2. `ErpPurReturn__save`（自包含，引用原收货/发票）→ `ErpPurReturn__submitForApproval` → `ErpPurReturn__approve` `[审批轴: DIRECT]`（触发反向出库 + PURCHASE_RETURN 红字凭证 + 负 AP）
  3. 退款：红字 AP 项 + 收款方往来核对（`IErpFinArApItemBiz.findOpenItems` 反查）
- **三层断言**：
  - 层 1：Return `approveStatus=APPROVED` + `posted=true`；反向出库移动存在（relatedBill 反查）；PURCHASE_RETURN 凭证借贷平衡且为红字方向（红字凭证行同向取负，先例 TestErpPurReturnRefundEndToEnd）；AP 项负向登记。
  - 层 2/层 3：每步快照 + 变更行（return + reverse stock move + 红字 voucher + ar_ap_item）。
- **主导域 / 涉及域**：purchase / purchase, inventory, finance, contract。

### 批 B2（主导域 sales）

#### C03 O2C 销售到收款黄金路径（EDI 订单发起）

- **业务目标**：销售到收款（O2C）核心闭环 + B2B EDI 订单发起——EDI 消息 → 销售订单 → 审批 → 出库 → 发票 → 收款，验证 AR 核销与 COGS。复杂度判据：跨 6 域 + 审批 + 过账 ✓。
- **前置**：`[seed]` 主数据（CUST-001 客户、FINISHED 物料、仓库）+ `[自包含]` EDI 订单消息（b2b）+ CRM 机会（转化来源）+ 销售订单。
- **关键路径步骤**：
  1. B2B EDI 订单接收 → 经 EDI 映射生成 `ErpSalOrder__save` `[涉及域 b2b]`
  2. `ErpCrmOpportunity__get`（机会→报价单转化来源核对）`[涉及域 crm]`
  3. `ErpSalOrder__submitForApproval` → `ErpSalOrder__approve` `[审批轴: DIRECT]`（承付会计钩子若启用则生成 COMMITMENT 影子凭证——config 门控，默认关闭不纳入断言）
  4. `ErpSalDelivery__save` → `ErpSalDelivery__submitForApproval` → `ErpSalDelivery__approve` `[审批轴: DIRECT]`（触发库存出库）
  5. `ErpSalInvoice__save` → `ErpSalInvoice__submitForApproval` → `ErpSalInvoice__approve` `[审批轴: DIRECT]`（AR_INVOICE 凭证 + COGS 凭证 + AR 辅助账）
  6. `ErpSalReceipt__save` → 收款推进 `[xwf 规避依据: 同 C01 Payment——默认 setUserId("0") 后端驱动，备选 seed 已过账收款前置，实施前定一]` → AR 核销
- **三层断言**：
  - 层 1：Order/Delivery/Invoice `approveStatus=APPROVED`；Invoice `posted=true`；AR_INVOICE 凭证借贷平衡；COGS 凭证金额 = 出库成本（FIFO/MovingAverage 口径）；AR 核销后 `openAmount=0`。
  - 层 2/层 3：每步快照 + 变更行（order/delivery/invoice/receipt + stock_move + voucher + ar_ap_item + gl_balance）。
- **主导域 / 涉及域**：sales / sales, inventory, finance, master-data, crm, b2b。

#### C04 销售退货与客服联动

> **前置勘误登记（2026-08-23，B2 实施期）**：本用例前置原述「[seed] 既有已过账销售链（erp_sal_invoice posted + AR 项 OPEN）」为 **stale 表述**——实仓核验 `erp_fin_ar_ap_item` 全部 SETTLED（OPEN 仅 HR 行 EMPLOYEE_ADVANCE/EXPENSE_CLAIM），部署 seed 无 OPEN AR 项。§3.4 风险②「自包含建数」纪律覆盖该前置：用例以**自包含 GraphQL 建数**实现（先建 posted 销售链得 OPEN AR 项再退货），保证 closure 门控「设计文档 ↔ 用例实现无矛盾」可满足。B2 实施证据：`TestErpC04SalReturnWithCs`。

- **业务目标**：客户投诉 → 客服工单 → 销售退货 → 反向入库 + 红字凭证 + 负 AR，退款核销。复杂度判据：跨 4 域 + 审批 + 过账 + 状态机 ✓。
- **前置**：`[seed]` 主数据（CUST-001 客户、MAT-001 物料、WH-MAIN 仓库、TT-COMPLAINT 工单类型）+ `[自包含]` 已过账销售链（Delivery→Invoice posted，得 OPEN AR 项——**勘误见上，实仓无 seed OPEN AR 项**）+ 客服工单（关联客户）+ 退货单。
- **关键路径步骤**：
  1. `ErpCsTicket__save`（客户投诉工单）→ 六态状态机推进（`assign`/`respond`…）`[涉及域 cs]`
  2. `ErpSalReturn__save`（引用原发票/工单）→ `ErpSalReturn__submitForApproval` → `ErpSalReturn__approve` `[审批轴: DIRECT]`（反向入库 + SALES_RETURN 凭证 + 负 AR）
  3. 退款：红字 AR + 往来核对
- **三层断言**：
  - 层 1：工单终态（RESOLVED/CLOSED）；Return `approveStatus=APPROVED` + `posted=true`；反向入库移动存在；SALES_RETURN 凭证借贷平衡红字（先例 TestErpSalReturnRefundEndToEnd）；AR 项负向登记。
  - 层 2/层 3：每步快照 + 变更行。
- **主导域 / 涉及域**：sales / sales, inventory, finance, cs。

### 批 B3（主导域 inventory）

#### C05 库存到岸成本分摊过账

> **实施期勘误登记（2026-08-23，B3）**：(1) **审批轴动作名漂移**——原述「save → submitForApproval → approve」为漂移，实仓 `ErpInvLandedCostBizModel` 无 `submitForApproval`（仅 approve/reverseApprove/allocate/generateFreightLandedCost），用例按 **approve-only** 落地；(2) **数据来源**——收货链 + 物流运费（`ErpLogCarrier`/`ErpLogShipment`，无 erp_log_* seed）全部自包含建数，未触发 seed 修正授权；(3) **BY_AMOUNT 分摊基数**——`LandedCostAllocationEngine` 基数 = 入库行 `amount`（0 即抛 NO_LINES），收货行须显式置 amount（C01/C02 收货行无 amount 不影响其断言）；(4) **成本层语义**——MOVING_AVERAGE 成本调整只更新 balance 不动 cost_layer（`CostAdjustmentService.applyAverageLike`），C05 物料取 **FIFO**（分摊 delta 调整层追加，Σ cost_layer totalCost = 原成本 + 分摊运费成立）。B3 实施证据：`TestErpC05InvLandedCost`。

- **业务目标**：到岸成本闭环——采购运费（logistics）归集 → 到岸成本单审批 → LANDED_COST 凭证 + 成本层更新。复杂度判据：跨 4 域 + 审批 + 过账 ✓。
- **前置**：`[seed]` 主数据（组织 2 / SUP-001 供应商 3 / WH-RAW 仓库 2 / CNY 币种 1 / 2026-07 OPEN 期间）+ `[自包含]` 已收货采购链（PO→Receive approve posted，FIFO 物料 + 原成本层 10@5=50）+ `[自包含]` 承运商/物流运费（`ErpLogCarrier__save` + `ErpLogShipment__save`，来源核对 `ErpLogShipment__get` freightAmount=15）+ `[自包含]` 到岸成本单（FREIGHT 费用行 15）。
- **关键路径步骤**：
  1. `ErpLogShipment__get`（运费来源核对）`[涉及域 logistics]`
  2. `ErpInvLandedCost__save`（自包含，引用收货单 + 运费行）→ **`ErpInvLandedCost__approve`（approve-only——实仓无 submitForApproval，勘误见上）** `[审批轴: DIRECT]`（LANDED_COST 凭证 + cost_layer Σ 更新，先例 TestErpInvLandedCostEndToEnd）
  3. `ErpInvStockBalance__findPage`（总成本断言）
- **三层断言**：
  - 层 1：LandedCost `approveStatus=APPROVED` + `posted=true`；LANDED_COST 凭证借贷平衡（1401 借 15 / 2202 贷 15）；Σ cost_layer totalCost = 原成本 50 + 分摊运费 15 = 65（FIFO delta 层 unitCost=Δ 追加）；stock_balance.totalCost 联动（65）。
  - 层 2/层 3：每步快照 + 变更行（landed_cost + voucher + cost_adjust + cost_layer + stock_balance + log_carrier/shipment）。
- **主导域 / 涉及域**：inventory / inventory, purchase, finance, logistics。

#### C06 库存成本流转与 COGS 核算（含质检门控）

> **实施期勘误登记（2026-08-23，B3）**：(1) **门控配置值修正**——计划原文 `@NopTestProperty(name="erp-qua.mandatory-inspection-bill-types", value="PUR_RECEIPT")` 中 `PUR_RECEIPT` 为 QA 域自引用常量（`ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT`），而门控点 `ErpPurReceiveProcessor.enforceInspectionGate` 使用 `ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE = "ERP_PUR_RECEIVE"` → 用例配置值落地为 **`ERP_PUR_RECEIVE`**；(2) 负路径 = REJECTED 检验先行保存 → 审批阻断（`erp.err.pur.receive-inspection-blocked`）→ 单据保持 SUBMITTED 且无移动单；(3) 正路径 = ACCEPTED 检验先行保存 → 审批放行 + 内建 DONE 入库移动 + FIFO cost_layer（无显式 generateMove/confirm，见计划 Baseline）。B3 实施证据：`TestErpC06InvCostFlowCogs`。

- **业务目标**：库存成本流转闭环——来料质检 → 入库 → 销售出库 COGS 核算（FIFO/MovingAverage），验证成本层与账本一致性。复杂度判据：跨 5 域 + 状态机 + 过账 ✓。
- **前置**：`[seed]` 主数据（组织 2 / CUST-001 客户 1 / WH-MAIN 仓库 1 / CNY 币种 1 / 2026-07 OPEN 期间）+ `[自包含]` FIFO 物料 + `[自包含]` QA 来料检验（ACCEPTED 正路径 / REJECTED 负路径）+ `[自包含]` 采购入库 + 销售出库。
- **关键路径步骤**：
  1. `ErpQaInspection__save`（来料检验 ACCEPTED/REJECTED，relatedBillType=ERP_PUR_RECEIVE）→ 门控通过/阻断 `[涉及域 quality]`
  2. `ErpPurReceive__save` → `ErpPurReceive__approve`（门控放行后内建触发 DONE 入库移动 + FIFO cost_layer 创建，先例 TestErpInvFifoCostingEndToEnd；**无显式 generateMove/confirm**——显式 confirm 对 DONE 移动单非法 ERR_ILLEGAL_STATUS_TRANSITION）
  3. `ErpSalDelivery__approve`（出库，引用 C03 同型链）→ COGS 凭证
  4. `ErpInvStockBalance__findPage` + `ErpInvCostLayer__findPage`（余额/层断言）
- **三层断言**：
  - 层 1：入库后 stock_balance.totalQty/totalCost 正确（10/85）；出库 COGS = 层成本（FIFO 先入先出口径 6×8.5=51，先例 TestErpInvFifoCostingEndToEnd）；质检 ACCEPTED 门控通过后方可入库（REJECTED 阻断断言——负路径，错误码 `erp.err.pur.receive-inspection-blocked`）；账本（ledger.totalCost=-51）与余额一致（出库后 4/34）。
  - 层 2/层 3：每步快照 + 变更行（inspection + stock_move/line + stock_balance + cost_layer + voucher）。
- **主导域 / 涉及域**：inventory / inventory, sales, finance, manufacturing（成品出库），quality。

### 批 B4（主导域 manufacturing + aps）

#### C07 制造工单全生命周期与完工过账

- **业务目标**：制造工单闭环——开工 → 领料 → 报工 → 完工 → 完工入库 + COGS/差异凭证 + 成本卷算。复杂度判据：跨 3 域 + 状态机 + 过账 ✓。
- **前置**：`[seed]` 物料/BOM/工作中心 + `[自包含]` 工单（DIRECT 审批轴，先例 TestErpMfgWorkOrderEndToEnd）。
- **关键路径步骤**：
  1. `ErpMfgWorkOrder__save` → `ErpMfgWorkOrder__submitForApproval` → `ErpMfgWorkOrder__approve` `[审批轴: DIRECT]`
  2. `ErpMfgWorkOrder__start`（开工）→ `ErpMfgWorkOrder__reportCompletion`（报工，触发领料出库）→ `ErpMfgWorkOrder__close`（完工：完工入库 + 完工过账凭证）
  3. `ErpMfgCostVariance__findPage`（差异断言）
- **三层断言**：
  - 层 1：状态机翻转（APPROVED→IN_PROCESS→COMPLETED）；完工入库 stock_move 生成；完工过账凭证借贷平衡（COGS 借方 + 存货/差异）；cost_variance.varianceAmount = actual − standard。
  - 层 2/层 3：每步快照 + 变更行（work_order + stock_move + voucher + cost_variance）。
- **主导域 / 涉及域**：manufacturing / manufacturing, inventory, finance。

#### C08 MRP 计划 → APS 排程 → 工单释放

- **业务目标**：计划域闭环——MRP 净需求计算 → 计划订单 → APS 排程 → 工单/采购建议释放。复杂度判据：跨 4 域 + 状态机 ✓。
- **前置**：`[seed]` 物料/BOM/库存 + `[自包含]` MRP 方案（demand 数据）。
- **关键路径步骤**：
  1. `ErpMfgMrpScenario__runSimulation` → `promoteToFormalPlan`（MRP 仿真→正式计划，config 门控 `erp-mfg.simulation-enabled=true`，先例 TestErpMfgMrpEndToEnd/TestErpMfgMrpSimulation）
  2. `ErpMfgMrpPlan__release`（计划释放 → 工单/采购建议）`[涉及域 aps: MRP 输出进入 APS 排程输入]`
  3. `ErpApsSchedule__save` → `ErpApsSchedule__publish`（排程发布，先例 ErpApsScheduleBizModel `@BizMutation`）`[涉及域 aps]`
  4. `ErpApsOperationOrder__*`（工序订单状态机推进）
- **三层断言**：
  - 层 1：MRP 计划净需求 = 毛需求 − 在途 − 在手；计划释放生成工单/采购建议（下游单据存在）；APS 排程发布后状态 PUBLISHED；工序订单状态机翻转。
  - 层 2/层 3：每步快照 + 变更行（mrp_plan/line + work_order/pur_order + aps_schedule + operation_order）。
- **主导域 / 涉及域**：manufacturing / manufacturing, aps, inventory, purchase。

#### C21 APS 排程发布与产能负荷

- **业务目标**：APS 排程闭环——工序订单排程 → 发布 → 产能负荷（CRP）计算可观测。复杂度判据：跨 4 域 + 状态机 ✓。
- **前置**：`[seed]` 工作中心配置链（workcenter/calendar/capacity，0628-1 已 seed）+ `[自包含]` 工序订单 + 排程。
- **关键路径步骤**：
  1. `ErpApsOperationOrder__save` → 状态机推进（排程候选）`[涉及域 aps]`
  2. `ErpApsSchedule__save` → `ErpApsSchedule__publish` `[涉及域 aps]`
  3. `ErpMfgReport__renderHtml(reportName="crp-load-report")`（负荷率断言，seed crp_load 行 loadRate=0.50 确定性派生）
  4. 负荷重算链（若 config 门控开启则经 `ErpMfgCrpRunJob`——默认关闭，断言静态行不被覆盖）
- **三层断言**：
  - 层 1：排程发布状态；crp-load 报表 HTML 含 WC-001/loadHours/capacityHours/loadRate 确定性 token（8.00/0.50，0628-1 期望值）。
  - 层 2/层 3：每步快照 + 变更行（operation_order + schedule + crp_load 若重算）。
- **主导域 / 涉及域**：aps / aps, manufacturing, inventory, finance（负荷率报表侧）。

### 批 B5（主导域 quality + maintenance）

#### C09 质检门控与 NCR/CAPA/SCRAP 闭环

- **业务目标**：质量闭环——完工质检 REJECTED 阻断完工 → NCR → CAPA 处置 → SCRAP 过账 + 通知派发。复杂度判据：跨 5 域 + 状态机 + 过账 ✓。
- **前置**：`[seed]` 工单（IN_PROCESS，0930-1 seed）+ `[自包含]` 检验单（REJECTED）+ NCR + CAPA 动作。
- **关键路径步骤**：
  1. `ErpQaInspection__save`（REJECTED）→ 完工门控阻断（`ErpMfgWorkOrder__close` 被拒，先例 TestErpQaInspectionTrigger/quality-ncr E2E）
  2. `ErpQaNonConformance__save`（从检验生成 NCR）→ `resolve` 门控（无 CAPA 须显式 noCapaReason；CAPA 三步闭包 startAction/completeAction/verifyAction）
  3. `ErpQaNonConformance__postNcr`（SCRAP 处置过账：disposition=SCRAP 驱动 NcrPostingDispatcher 生成凭证行 + 库存扣减，先例 quality-ncr-scrap-posting E2E）
  4. 通知：NCR 状态变更触发 `ErpSysNotification` 生成（`markRead` 断言）`[涉及域 notify]`
- **三层断言**：
  - 层 1：REJECTED 检验阻断完工（错误码断言）；NCR 门控（未闭 CAPA 被拒）；SCRAP 凭证借贷平衡；库存扣减数量正确；通知记录存在且可标记已读。
  - 层 2/层 3：每步快照 + 变更行（inspection + ncr + action + voucher + stock + notification）。
- **主导域 / 涉及域**：quality / quality, manufacturing, inventory, finance, notify。

#### C10 维护工单与备件消耗过账

- **业务目标**：维护闭环——设备维护请求 → 受理 → 访问 → 备件消耗 → MAINTENANCE_ISSUE 过账 + 设备状态联动。复杂度判据：跨 4 域 + 状态机 + 过账 ✓。
- **前置**：`[seed]` 设备（0930-2 seed：RUNNING/DOWN）+ `[自包含]` 维护请求 + 访问 + 备件消耗。
- **关键路径步骤**：
  1. `ErpMntRequest__save`（OPEN）→ `accept`（生成 DRAFT Visit，响应式副作用，先例 mnt-request E2E）→ `startRepair` → `complete`（状态机 5 态）
  2. `ErpMntVisit__complete`（访问完成 + visit_task）
  3. 备件消耗 `confirm` → MAINTENANCE_ISSUE 过账（config 门控 `erp-mnt.spare-part-posting-enabled=true` → GL 凭证，先例 spare-part-posting E2E）
  4. `ErpAstAsset__get`（设备关联资产卡片核对）`[涉及域 assets]`
- **三层断言**：
  - 层 1：Request 状态机终态 COMPLETED；MAINTENANCE_ISSUE 凭证借贷平衡；备件库存扣减；设备状态/资产字段联动；非法迁移被拒（守卫断言）。
  - 层 2/层 3：每步快照 + 变更行（request + visit + spare_part_usage + voucher + stock_move）。
- **主导域 / 涉及域**：maintenance / maintenance, inventory, finance, assets。

### 批 B6（主导域 projects + assets）

#### C11 项目成本归集 → CIP → 资产资本化 → 维护设备

- **业务目标**：项目转资闭环——项目成本归集 → CIP 在建工程 → 资本化转固建卡 → 设备维护计划挂接。复杂度判据：跨 5 域 + 审批 + 过账 ✓。
- **前置**：`[seed]` 项目（OPEN，2210-1 seed）+ `[自包含]` 成本归集单 + CIP + 资本化单。
- **关键路径步骤**：
  1. `ErpPrjCostCollection__save`（项目成本归集）`[涉及域 projects]`
  2. `ErpAstCip__save`（在建工程，sourceType=CIP(20)）→ 完工审批
  3. `ErpAstAssetCapitalization__save` → `submit` → `approve` `[审批轴: DIRECT]`（建卡 + CAPITALIZATION 凭证，先例 TestErpAstPostingReverse/TestErpAstCapitalization）
  4. `ErpMntEquipment__save`（资产卡片 → 设备卡，assetId 跨域引用——seed-data.md equipment.assetId 先例）`[涉及域 maintenance]`
- **三层断言**：
  - 层 1：CIP 状态迁移（IN_CONSTRUCTION→TRANSFERRED）；资本化审批后资产卡片生成（IN_SERVICE）+ CAPITALIZATION 凭证借贷平衡；CIP 成本 = 资本化原值；设备卡 assetId 指向新资产。
  - 层 2/层 3：每步快照 + 变更行（cost_collection + cip + asset + depreciation_schedule + voucher + equipment）。
- **主导域 / 涉及域**：projects / projects, assets, finance, master-data, maintenance。

#### C12 项目工时过账与结算损益

- **业务目标**：项目业财闭环——员工工时 → 审批 → PROJECT_COST_COLLECTION 凭证 → 项目结算 CLOSE → 损益结转。复杂度判据：跨 3 域 + 审批 + 过账 ✓。
- **前置**：`[seed]` 员工（1234-1 seed）+ `[自包含]` 工时单 + 项目结算单。
- **关键路径步骤**：
  1. `ErpPrjTimesheet__save` → `submit` → `approve` `[审批轴: DIRECT]`（PROJECT_COST_COLLECTION 凭证：Dr 5101/Cr 2211，config `erp-prj.default-payroll-subject-id=2211`）`[涉及域 hr: 员工工时归属]`
  2. `ErpPrjSettlement__createSettlement` → `approve` → CLOSE（Dr 1601/Cr 1603 + 资产卡片回退，先例 projects-settlement E2E）
  3. 损益结转：`FINAL/INTERIM approve`（Dr 5101/Cr 6001 + 条件性 4103 本年利润，先例 projects E2E）
- **三层断言**：
  - 层 1：工时审批后凭证借贷平衡 + 金额正确（hours × rate）；结算 CLOSE 后凭证 + 资产卡片回退；损益结转凭证 + 期间模块状态。
  - 层 2/层 3：每步快照 + 变更行（timesheet + settlement + voucher + asset + project_pnl）。
- **主导域 / 涉及域**：projects / projects, finance, hr。

### 批 B7（主导域 finance）

#### C13 期末结账全链与反结账

- **业务目标**：期末结账闭环——前置检查 → 模块关账（AR/AP/INV/AST/PRJ/GL）→ 汇兑重估 → 损益结转 → 最终锁定 → 反结账 → 重新结账。复杂度判据：跨 6 域 + 过账 + 状态机 ✓。
- **前置**：`[seed]` 会计期间 OPEN（1445-1 seed）+ 已过账业务链（P2P/O2C 凭证）+ `[seed]` 1045-1 追加的 OPEN 往来项（EMPLOYEE_ADVANCE/EXPENSE_CLAIM 两行，preCheck 期望列出的未核销项来源）+ `[自包含]` 新期间（若测试跨期）。
- **关键路径步骤**：
  1. `ErpFinAccountingPeriod__preCheck`（前置检查，未核销 AR/AP 列表）
  2. `ErpFinAccountingPeriod__closePeriod`（模块关账 + 汇兑重估 + 损益结转 → CLOSED；先例 TestErpFinPeriodCloseEndToEnd）
  3. `ErpFinAccountingPeriod__finalizePeriod`（CLOSED_FINAL）
  4. `ErpFinAccountingPeriod__reverseClose`（反结账 → OPEN）→ `closePeriod`（重新结账幂等）
- **三层断言**：
  - 层 1：preCheck 列出未核销项；closePeriod 后 CLOSED + 汇兑重估/损益结转凭证生成（billCode 反查）；各模块关账状态（GL/AST/PRJ…）；finalize 后 CLOSED_FINAL；reverseClose 后 OPEN；重新结账生成新结转凭证（幂等断言）。
  - 层 2/层 3：每步快照 + 变更行（period + period_status + voucher + gl_balance）。
- **主导域 / 涉及域**：finance / finance, sales, purchase, inventory, assets, projects。

#### C14 银行对账与坏账计提回收

- **业务目标**：资金闭环——银行对账单生成 → 对账 → 未达调整凭证 → 冲销；坏账核销 → 审批 → 收回 → 期末计提。复杂度判据：跨 3 域 + 状态机 + 过账 ✓。
- **前置**：`[seed]` 银行账户（1234-1 seed）+ `[自包含]` 对账单 + 基金账户 + OPEN AR 项（自包含 partner 隔离，先例 bank-recon E2E）。
- **关键路径步骤**：
  1. `ErpFinBankReconciliation__generate` → `post`（BANK_RECON_ADJ 未达调整凭证，config `erp-fin.ap-subject-code=2202` 等 4 键）→ `reverse`（红冲）
  2. `ErpFinBadDebtWriteOff__writeOff` → `submit` → `approve` `[审批轴: DIRECT]`（坏账核销凭证，config 1231/6701）→ `recover`（收回，红冲）
  3. `ErpFinBadDebtProvision__runBadDebtProvision`（期末计提，config 门控）
- **三层断言**：
  - 层 1：对账状态机（GENERATED→POSTED→REVERSED）；BANK_RECON_ADJ 凭证借贷平衡 + 红冲闭环（原凭证 isReversed）；坏账核销凭证 + 收回红冲；计提金额 = 期末 AR × 计提率。
  - 层 2/层 3：每步快照 + 变更行（bank_statement/recon + bad_debt + voucher + ar_ap_item）。
- **主导域 / 涉及域**：finance / finance, sales, master-data。

### 批 B8（主导域 crm + cs + hr）

#### C15 CRM 线索转化与销售预测

- **业务目标**：CRM 闭环——线索阶段迁移 → 机会 → 报价 → 订单转化 + 销售预测准确率聚合。复杂度判据：跨 4 域 + 状态机 ✓。
- **前置**：`[seed]` CRM 阶段（1045-1 seed：stage/lead/forecast）+ `[自包含]` 新线索 + 机会。
- **关键路径步骤**：
  1. `ErpCrmLead__save` → `moveStage`（阶段迁移，先例 crm-lead E2E）
  2. `ErpCrmOpportunity__save`（机会，转化来源）→ 报价单生成（`ErpSalQuotation` 转化，先例 ErpCrmProductConfiguratorGenerateQuoteProcessor）
  3. `ErpSalOrder__save`（订单转化）→ 审批（DIRECT）
  4. `ErpCrmReport__renderHtml(reportName="forecast-accuracy")`（预测准确率 token 断言）
- **三层断言**：
  - 层 1：Lead 阶段迁移正确（stageId 翻转）；报价→订单转化成功；forecast-accuracy 报表 commitAmount/lineCount 与 seed 派生一致（50000/45000/80000/63000 期望值，1045-2）。
  - 层 2/层 3：每步快照 + 变更行（lead + opportunity + quotation + order）。
- **主导域 / 涉及域**：crm / crm, sales, master-data, finance。

#### C16 CS 工单 SLA 与通知派发

- **业务目标**：客服闭环——工单六态状态机 → SLA 达标计算 → 宏响应 → 满意度调研 → 通知派发。复杂度判据：跨 3 域 + 状态机 ✓。
- **前置**：`[seed]` 工单类型（1045-1 seed）+ `[自包含]` 工单 + 调研。
- **关键路径步骤**：
  1. `ErpCsTicket__save` → 六态状态机推进（`assign`/`respond`/`resolve`/…，先例 cs-ticket E2E）
  2. `ErpCsCannedResponse__applyCannedResponse`（宏响应，usageCount 递增，先例 cs-canned-response E2E）
  3. `ErpCsSurvey__save`（CSAT/NPS）→ `ErpCsReport__renderHtml(reportName="ticket-sla-csat-summary")`（token 断言）
  4. 通知：工单事件触发 `ErpSysNotification`（跨域通知派发，`markRead`）`[涉及域 notify]`
- **三层断言**：
  - 层 1：工单终态 + isSlaCompleted 正确；宏响应 usageCount 递增；SLA/CSAT 报表 token（投诉桶/5.00/9.00 期望值，1045-2）；通知已读翻转。
  - 层 2/层 3：每步快照 + 变更行（ticket + survey + notification）。
- **主导域 / 涉及域**：cs / cs, notify, master-data。

#### C17 HR 薪酬发放闭环

- **业务目标**：薪酬闭环——薪资计算 → xwf 三级审批 → 发放 → SALARY_PAYMENT 凭证 + 员工往来 + 发放通知。复杂度判据：跨 3 域 + 审批（xwf）+ 过账 ✓。
- **前置**：`[seed]` 员工（1234-1 seed）+ `[自包含]` 薪资单（薪酬项/模拟）。
- **关键路径步骤**：
  1. `ErpHrSalary__calculateSalary`/`runPayroll`（薪资计算，先例 TestErpHrPayrollEngine）
  2. `ErpHrSalary__submitForApproval` → xwf 3 级 agree `[xwf 规避依据: setUserId("0") 后端驱动，TestErpHrSalaryWorkflowApproval 实证，M0.2 复核]` → `approve`（APPROVED）
  3. `ErpHrSalary__markPaid`（SALARY_PAYMENT 凭证：Dr 费用/Cr 2211，先例 salary-posting）
  4. 通知：发放完成触发员工通知 `[涉及域 notify]`
- **三层断言**：
  - 层 1：审批链终态 APPROVED（xwf 链 submit→agree×3）；markPaid 后 paymentStatus=PAID；SALARY_PAYMENT 凭证借贷平衡 + 金额 = 薪资合计；员工往来（ar_ap_item EMPLOYEE_ADVANCE 口径）核对；通知已读。
  - 层 2/层 3：每步快照 + 变更行（salary + voucher + notification）。
- **主导域 / 涉及域**：hr / hr, finance, notify。

### 批 B9（主导域 contract + b2b）

#### C18 合同生命周期与返利计提结算

- **业务目标**：合同闭环——合同生命周期 6 动作 → 版本/签署 → 返利计提 → 返利结算（跨域负额 credit memo + 计提翻转）。复杂度判据：跨 3 域 + 状态机 + 过账 ✓。
- **前置**：`[seed]` 客户/物料 + `[自包含]` 合同（含返利条款）+ 销售发票。
- **关键路径步骤**：
  1. `ErpCtContract__submit` → `activate` → `amend`（版本）→ `suspend`/`resume`/`terminate`（生命周期 6 动作，先例 contract E2E）
  2. `ErpCtContract__runAccrual`（返利计提，config 门控）
  3. `ErpCtContract__postSettlement`（返利结算：跨域负额 credit memo + 计提翻转，先例 contract-rebate E2E）
- **三层断言**：
  - 层 1：生命周期状态机翻转（ACTIVE→SUSPENDED→ACTIVE→TERMINATED）；计提金额 = 条款 × 销售额；结算后负额发票/凭证生成 + 计提行翻转（红字同向取负）。
  - 层 2/层 3：每步快照 + 变更行（contract/version + accrual + invoice + voucher）。
- **主导域 / 涉及域**：contract / contract, sales, finance。

#### C19 B2B ASN 自动收货与物流到岸成本

- **业务目标**：B2B 供应链闭环——ASN 状态机 → 自动创建收货 → 收货过账 → 物流送达 → 运费到岸成本自动创建。复杂度判据：跨 5 域 + 状态机 + 过账 ✓。
- **前置**：`[seed]` 供应商/物料/仓库 + `[自包含]` ASN（含行）+ 物流发货单。
- **关键路径步骤**：
  1. `ErpB2bAsn__save` → 状态机推进（RECEIVED 等，先例 b2b-asn E2E）→ `ErpB2bAsn__createReceiveFromAsn`（config 门控 `erp-b2b.asn-auto-create-receive=true` → 生成采购收货草稿）
  2. `ErpPurReceive__submitForApproval` → `approve` `[审批轴: DIRECT]`（收货过账）
  3. `ErpLogShipment__handleTrackingWebhook`（DELIVERED，config `erp-log.webhook-signature-required=false`）→ path-2 到岸成本自动创建（config `erp-log.path2-landed-cost-auto-create=true`，先例 logistics path2 E2E）
  4. `ErpInvLandedCost__findPage`（自动创建的到岸成本单断言）
- **三层断言**：
  - 层 1：ASN 状态机终态；收货单自动生成（草稿→审批→posted）；物流送达事件触发到岸成本 DRAFT 单（FREIGHT 费用行）；到岸成本单引用正确收货/运费。
  - 层 2/层 3：每步快照 + 变更行（asn + receive + shipment + landed_cost）。
- **主导域 / 涉及域**：b2b / b2b, purchase, inventory, logistics, finance。

### 批 B10（主导域 drp）

#### C20a DRP 净需求与补货释放

- **业务目标**：分销补货闭环——DRP 运行 → 净需求计算 → 计划审批 → 释放生成采购/调拨建议。复杂度判据：跨 4 域 + 状态机 ✓。
- **前置**：`[seed]` 物料/仓库/往来 + `[自包含]` DRP 计划（需求数据）。
- **关键路径步骤**：
  1. `ErpDrpPlan__save` → `ErpDrpPlan__runDrp`（净需求引擎，先例 TestErpDrpEngine）
  2. `ErpDrpPlan__approvePlan`（approvedQty 回填）
  3. `ErpDrpLine__releaseLine`（行级释放 → 生成 TransferOrder/PurchaseOrder 建议；或 `ErpDrpLine__releaseApproved` 计划级释放，先例 TestErpDrpScheduleRelease）
  4. 下游单据审批（生成的建议单走 DIRECT 审批）
- **三层断言**：
  - 层 1：净需求 = 毛需求 − 在途 − 在手 − 已分配；审批后 approvedQty 正确；释放生成下游单据（采购/调拨建议存在）；下游审批后过账（posted）。
  - 层 2/层 3：每步快照 + 变更行（drp_plan/line + pur_order/transfer + voucher）。
- **主导域 / 涉及域**：drp / drp, purchase, inventory。

#### C20b DRP 仿真与正式计划提升

- **业务目标**：DRP 仿真闭环——仿真运行 → 版本对比 → 提升为正式计划。复杂度判据：跨 3 域 + 状态机 ✓。
- **前置**：`[seed]` 物料/仓库 + `[自包含]` 仿真场景（参数，自包含隔离，先例 TestErpDrpSimulation）。
- **关键路径步骤**：
  1. `ErpDrpScenario__runSimulation`（config 门控 `erp-drp.simulation-enabled=true`）
  2. `ErpDrpScenario__compareVersions`（版本对比结构断言）
  3. `ErpDrpScenario__promoteToFormalPlan`（提升正式计划 → 生成可执行 DRP 计划）
  4. `ErpDrpPlan__runDrp`（正式计划运行）
- **三层断言**：
  - 层 1：仿真版本生成；compareVersions 两版本参数 delta 断言（或结构非空——ParamResolver 缓存约束先例，浏览器层降级口径 JUnit 层不受限）；promote 后正式计划状态。
  - 层 2/层 3：每步快照 + 变更行（scenario/version + drp_plan）。
- **主导域 / 涉及域**：drp / drp, purchase, inventory。

---

## 7. 用例覆盖矩阵（验收表）

> 全 19 子系统（18 业务域 + notify）逐域出现次数；验收标准：**每域 ≥2 次** + **每域至少 1 次全覆盖**。计数 = 用例「涉及域」出现次数（主导域同时计入）。

| # | 域（module） | 出现于用例 | 次数 | 验收（≥2） |
|---|-------------|-----------|------|-----------|
| 1 | master-data | C01, C03, C11, C14, C15, C16 | 6 | ✅ |
| 2 | inventory | C01, C02, C03, C04, C05, C06, C07, C08, C09, C10, C13, C19, C20a, C20b, C21 | 15 | ✅ |
| 3 | purchase | C01, C02, C05, C08, C13, C19, C20a, C20b | 8 | ✅ |
| 4 | sales | C03, C04, C06, C13, C14, C15, C18 | 7 | ✅ |
| 5 | finance | C01, C02, C03, C04, C05, C06, C07, C09, C10, C11, C12, C13, C14, C15, C17, C18, C19, C21 | 18 | ✅ |
| 6 | assets | C10, C11, C13 | 3 | ✅ |
| 7 | projects | C11, C12, C13 | 3 | ✅ |
| 8 | manufacturing | C06, C07, C08, C09, C21 | 5 | ✅ |
| 9 | quality | C06, C09 | 2 | ✅ |
| 10 | maintenance | C10, C11 | 2 | ✅ |
| 11 | notify | C09, C16, C17 | 3 | ✅ |
| 12 | crm | C03, C15 | 2 | ✅ |
| 13 | cs | C04, C16 | 2 | ✅ |
| 14 | hr | C12, C17 | 2 | ✅ |
| 15 | aps | C08, C21 | 2 | ✅ |
| 16 | logistics | C05, C19 | 2 | ✅ |
| 17 | b2b | C03, C19 | 2 | ✅ |
| 18 | contract | C02, C18 | 2 | ✅ |
| 19 | drp | C20a, C20b | 2 | ✅ |

**核验**：19/19 域 ≥2 次；每域至少 1 次全覆盖（矩阵每域非空即证）；用例涉及域/主导域与 §6 用例规格逐项一致（显式算术核验：Σ矩阵 = 6+15+8+7+18+3+3+5+2+2+3+2+2+2+2+2+2+2+2 = **88** = Σ用例涉及域数（C01 4 + C02 4 + C03 6 + C04 4 + C05 4 + C06 5 + C07 3 + C08 4 + C09 5 + C10 4 + C11 5 + C12 3 + C13 6 + C14 3 + C15 4 + C16 3 + C17 3 + C18 3 + C19 5 + C20a 3 + C20b 3 + C21 4 = 88）；22 用例全部出现在矩阵；无矩阵外用例、无用例外矩阵行）。

---

## 8. M0.3 分批建议（主导域分组，8-12 批）

> M0.3 依本表向 M1-Bn 追加工作项（每批 1 工作项 2-3 用例；行内含用例编号/涉及域/Skill）。优先 18-22 用例校准：本设计 22 用例 = 10 批。

| 批 | 主导域 | 用例 | 涉及域 | Skill |
|----|--------|------|--------|-------|
| B1 | purchase | C01, C02 | pur, inv, fin, md, ct | nop-testing |
| B2 | sales | C03, C04 | sal, inv, fin, md, b2b, crm, cs | nop-testing |
| B3 | inventory | C05, C06 | inv, pur, fin, log, sal, mfg, qa | nop-testing |
| B4 | manufacturing + aps | C07, C08, C21 | mfg, aps, inv, pur, fin | nop-testing |
| B5 | quality + maintenance | C09, C10 | qa, mnt, mfg, inv, fin, notify, ast | nop-testing |
| B6 | projects + assets | C11, C12 | prj, ast, fin, md, mnt, hr | nop-testing |
| B7 | finance | C13, C14 | fin, sal, pur, inv, ast, prj, md | nop-testing |
| B8 | crm + cs + hr | C15, C16, C17 | crm, cs, hr, sal, md, fin, notify | nop-testing |
| B9 | contract + b2b | C18, C19 | ct, b2b, sal, fin, pur, inv, log | nop-testing |
| B10 | drp | C20a, C20b | drp, pur, inv | nop-testing |

**分批原则**：同主导域合并（B1/B2/B3/B7）；扩展域与核心域联动批次（B5/B6/B8/B9/B10）；每批内用例涉及域互补，批间无强依赖（可并行推进，V.1 聚合）。

---

## 9. 与 roadmap M0.1 规格逐项对应

| roadmap M0.1 规格 | 本设计落点 |
|------------------|-----------|
| 15-25 用例全量设计（六要素） | §6：22 用例 × 六要素完整（业务目标/前置/步骤/三层断言/主导域/涉及域） |
| 覆盖全 19 子系统每域至少 1 次 | §7：19/19 全覆盖 |
| 用例覆盖矩阵（每域出现次数，目标 ≥2）验收表 | §7：19/19 ≥2 |
| 复杂度判据（跨 3+ 域 + 审批/过账/状态机至少其一） | §6 逐用例标注：22/22 跨 3+ 域；审批/过账/状态机至少其一 |
| 测试机制选型论证 (a)/(b)/(c) + 选定 | §3.1/§3.2 |
| 四风险可行性判据（NopJunitExtension 共存 / CHECKING 态 DB 组成 / per-method restart × 文件 H2 / seed 变更敏感性） | §3.3：四风险判定方法逐项（M0.2 可直接实证） |
| 用例粒度约定（1 用例 = 1 测试类 1 测试方法，@BeforeAll 类级初始化，fresh-DB 每类 1 次） | §4 |
| 预计耗时模型（15-25 类 × ~12s 启动 ≈ 3-6 分钟纯启动） | §4：22 类 ≈ 8-26 分钟总模型（中位 ~15） |
| 审批流以当前实现为准（xwf 规避） | §5：已验证可执行审批流清单 + 排除路径 + 用例规格标注 |
| 独立子代理审查收敛（≥2 轮） | §10 审查记录 |
| 主导域分组呼应 M0.3 分批（8-12 批，每批 2-3 用例，优先 18-22 用例校准） | §8：10 批，22 用例 |

---

## 10. 独立审查收敛记录

> 本节记录**设计文档本身**的独立审查收敛（fresh 会话，无执行者上下文）。计划级审查（Draft Review Record）见 `docs/plans/2026-08-23-1835-1-m01-integration-test-case-design.md`（与本记录对象不同，不混淆）。

- **Independent review iteration 1（设计文档，2026-08-23，独立 general 子代理新会话 `ses_fd1c0b042ffeUFNkSff8zPMUuu`）**：结论 = **needs revision** — 0 BLOCKER / 1 MAJOR / 5 MINOR。基线事实 12 项抽查零漂移；机制选型/四风险判定方法/用例六要素/以当前实现为准全部 PASS。发现与修订：
  - **MAJOR-1**：§7 覆盖矩阵 inventory 行误含 C12、漏 C08/C21、计数 14→15（违反计划 Phase 2 Exit Criterion 3「计数核对」）→ 已修正为 C01-C21 共 15 项，§7 核验补显式算术核验（Σ矩阵 = Σ用例涉及域数 = 88，逐域独立重算通过）。
  - MINOR-2：C01/C03 xwf 规避依据「或」语义歧义 → 明确默认 setUserId("0") 后端驱动 + seed 前置备选 + 实施前定一。
  - MINOR-3：C13 preCheck 未核销项来源悬空 → 前置补 1045-1 OPEN 往来项（EMPLOYEE_ADVANCE/EXPENSE_CLAIM）。
  - MINOR-4：§4 耗时模型算术不自洽 → 重算为 22 类 ≈ 8-26 分钟（中位 ~15），统一执行时间口径。
  - MINOR-5：§10 审查记录对象不实 → 本修订（设计文档级记录内联）。
  - MINOR-6：§5「39 实体」无出处 → 改可核验表述（8 模块 79 文件分布）。
- **Independent review iteration 2（设计文档，2026-08-23，独立 general 子代理新会话 `ses_fd1bc538dffeZ0aKucwxkpG65P`）**：结论 = **accept** — 0 BLOCKER / 0 MAJOR / 7 MINOR。MAJOR-1 与 MINOR-2/3 完全修复核验通过；覆盖矩阵逐域独立重算通过；剩余 7 MINOR 全部采纳并修订落地：
  - M-1：§9 耗时行与 §4 口径统一（8-26 分钟）。
  - M-2：§10 补真实设计文档级审查记录（本节）。
  - M-3：§5 DIRECT 轴分布计数改 8 模块 79 文件（实仓核对）。
  - M-4：C09 `ErpQaNonConformance__scrap` → `postNcr`（disposition=SCRAP 驱动，实仓 BizModel 核实）。
  - M-5：C20a `ErpDrpPlan__release` → `ErpDrpLine__releaseLine`/`releaseApproved`（实仓 BizModel 核实）。
  - M-6：C11 `ErpAstCapitalization__*` → `ErpAstAssetCapitalization__*`（实仓实体名核实）。
  - M-7：C21 编号非单调 → §6 编号说明（见上）。

**收敛结论**：两轮独立审查 0 BLOCKER / 0 MAJOR 收敛；文档定稿。roadmap M0.1 `todo` → `done`（经独立 closure audit，见计划 Closure 段）。

---

## 附录 A：机制实证素材清单（源码锚点）

| 锚点 | 文件 | 行号 |
|------|------|------|
| JunitAutoTestCase 硬编码 @ExtendWith | `nop-entropy/nop-autotest/nop-autotest-junit/.../JunitAutoTestCase.java` | :29 |
| 缺 @NopTestConfig 抛异常 | 同上 | :83-85 |
| RECORDING 分支（setLocalDb(testConfig.localDb()) + tableInit=false） | 同上 | :110-116 |
| CHECKING 分支（硬编码 localDb=true + tableInit=true） | 同上 | :117-126 |
| configLocalDb（in-memory H2 + 随机 UUID） | `nop-entropy/nop-autotest/nop-autotest-core/.../AutoTestCase.java` | :190-197 |
| per-method container.restart() | 同上 | :211-225 |
| createTables 只建 input/output 涉及表 | `.../execute/AutoTestCaseDataBaseInitializer.java` | :90-109 |
| 宿主模式（手动初始化 + 文件 H2 fresh 清理） | `app-erp-all/src/test/java/io/nop/app/all/auth/TestAuthSeedLoadingProof.java` | :37-56 |
| 父 POM surefire forkCount=4/parallel=classes | `nop-entropy/pom.xml` surefire 段 | 2026-08-23 实仓 |
| app-erp-all 无模块级 surefire 覆盖 | `app-erp-all/pom.xml` build 段 | 2026-08-23 实仓 |
| xwf user:$0 被拒（浏览器层 NOT FEASIBLE） | `docs/plans/2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility.md` | 权威裁决 |
| 后端 setUserId("0") xwf 链实证 | `TestErpHrSalaryWorkflowApproval` / `TestErpPurPaymentWorkflowApproval` | 2026-08-23 实仓 |
| 部署 seed 94 CSV + 1 SQL | `app-erp-all/src/main/resources/_vfs/_init-data/` | 2026-08-23 实仓计数 |