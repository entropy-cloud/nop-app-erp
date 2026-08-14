# TestAuthSeedLoadingProof NPE：OrmTransactionListener.ormTemplate 为 null（nop-entropy e5ee02b40 lazy-property 回归）

## 问题

- 什么坏了：`TestAuthSeedLoadingProof`（app-erp-all，auth CSV 种子加载 Proof，plan 2026-08-09-2107 P1.5b）在 `@BeforeAll CoreInitialization.initialize()` 阶段 NPE：`Cannot invoke "io.nop.orm.IOrmTemplate.flushSession()" because "this.ormTemplate" is null`，栈在 `OrmTransactionListener.onBeforeCommit` ← `DataInitInitializer.executeSqlFiles` 事务提交。
- 在哪里坏了：平台层 nop-entropy `io.nop.orm.txn.OrmTransactionListener:30` + `DataInitInitializer.executeSqlFiles`（`nop-orm-2.0.0-SNAPSHOT.jar`，.m2 重建于 2026-08-13 06:03）。
- 最小可见症状：全 reactor `mvn test` 时 app-erp-all FAILURE；`mvn test -pl app-erp-all -Dtest=TestAuthSeedLoadingProof` 单测同样失败（与 reactor 顺序无关）。
- 影响或严重性：auth 种子加载 Proof 不可运行（P1.5b 的 Proof 测试被打红）；阻塞全 reactor `mvn test` 绿基线。非本计划（`2026-08-13-1146-1` finance notes 状态机）引入——clean HEAD 重编译 module-finance 后复现不变。

## 复现

- 环境和前提条件：.m2 中 nop-orm/nop-ioc/nop-dao 2.0.0-SNAPSHOT jar 为 2026-08-13 06:03 重建（包含 nop-entropy `e5ee02b40` 2026-08-11 20:58 的 `ioc:lazy-property` 改动）。
- 触发步骤：
  1. `mvn test`（全 reactor）或 `mvn test -pl app-erp-all -Dtest=TestAuthSeedLoadingProof`
  2. 观察 `DataInitInitializer.executeSqlFiles` → 事务提交 → `OrmTransactionListener.onBeforeCommit` NPE
- 最小复现：`mvn test -pl app-erp-all -Dtest=TestAuthSeedLoadingProof -Dsurefire.failIfNoSpecifiedTests=false`

## 诊断方法

- 诊断难度：中等——NPE 在平台层，初看可能与本计划（finance beans 注册）有关；需排除。
- 调查路径：
  1. 全 reactor 失败 → 读 surefire 报告，NPE 栈为平台 `OrmTransactionListener`（nop-orm jar），非 app 代码。
  2. **排除本计划**：`git stash` 回退全部未提交变更 → `mvn clean install -DskipTests -pl module-finance/erp-fin-service -am`（确保 .m2 与本计划变更无关的 jar 重建）→ `mvn test -pl app-erp-all -Dtest=TestAuthSeedLoadingProof` **仍同样 NPE**。证明 clean HEAD 复现。
  3. 时间线归因：`TestAuthSeedLoadingProof` 在 2026-08-10/08-11 日志中均绿（3/0/0）；nop-entropy jar 于 08-13 06:03 重建；`git log` 显示 `e5ee02b40`（08-11 20:58）将 `nopDefaultTransactionListener.ormTemplate` 从无 property 改为 `ioc:lazy-property="true"`——lazy-property 下 setOrmTemplate 推迟到 `runLazyPropActions`（容器 flushActions 阶段），而 `DataInitInitializer` 的 `@PostConstruct init()` 在 bean 创建阶段即执行 SQL 事务并提交 → 事务监听器 ormTemplate 尚未注入 → NPE。
  4. 平台自身补丁历史佐证：`652c4f09e`（08-08）回退 `ensureOrmTemplateSessionFactory` 补丁，`1edf193be`（08-10）为 ALL_LAZY 测试模式加 DataBaseSchemaInitializer force-init——平台近期在反复调整初始化顺序，本 NPE 属该系列改动的运行时暴露。
- 被拒绝的假设：~~finance app-service.beans.xml 新增 2 Bean 破坏容器初始化顺序~~——stash 后 clean HEAD 复现；且新增 Bean 为纯无状态（零 @Inject），不参与事务监听器装配。

## 根本原因

- nop-entropy `e5ee02b40`（2026-08-11）把 `orm-defaults.beans.xml` 中 `nopDefaultTransactionListener` 的 `ormTemplate` 注入改为 `ioc:lazy-property="true"`（规避循环依赖初始化顺序问题）。
- `ioc:lazy-property` 语义：属性赋值延迟到容器 `flushActions` 的 `runLazyPropActions` 阶段；但 `DataInitInitializer`（`@PostConstruct init()`，`nop.orm.init-database-data=true` 时装配）在 bean 创建阶段执行 `executeSqlFiles` → `jdbcTemplate.txn().runInTransaction` → 提交 → `beforeCommit` 触发 `OrmTransactionListener.onBeforeCommit` → `ormTemplate` 尚为 null → NPE。
- 变更位于 nop-entropy（外部仓库），触发于 .m2 jar 重建（08-13 06:03）。属外部仓库代码保护区域（ask first）。

## 修复

- 未修复（本 VERIFY 运行仅记录，外部仓库需 ask-first）。候选方向：
  - nop-entropy 侧：恢复 `nopDefaultTransactionListener.ormTemplate` 为 eager 注入（若循环依赖允许），或给 `DataInitInitializer` 增加 force-init/ioc:after 顺序保证；需 nop-entropy 跨仓库 plan + audit。
  - 备选：`DataInitInitializer.executeSqlFiles` 内事务改为在 lazy-prop 阶段之后执行（平台初始化时序调整）。
- 修复后验证：`mvn test -pl app-erp-all -Dtest=TestAuthSeedLoadingProof` 3/0/0 + 全 reactor `mvn test` 绿。

## 测试

- 回归覆盖：`TestAuthSeedLoadingProof` 本身即回归测试（3 @Test：24 角色 / userId=1 ACTIVE / admin 绑定 + 密码往返）。平台侧可参考 `TestDataInitInitializerContainer`（nop-orm-geo，容器级）扩展 lazy-property 时序断言。

## 受影响的工件

- 外部仓库：nop-entropy `nop-persistence/nop-orm/src/main/resources/_vfs/nop/orm/beans/orm-defaults.beans.xml`（`nopDefaultTransactionListener`）+ `DataInitInitializer.java`（init/executeSqlFiles）
- 触发 jar：`.m2/repository/io/github/entropy-cloud/nop-orm/2.0.0-SNAPSHOT/nop-orm-2.0.0-SNAPSHOT.jar`（08-13 06:03 重建）

## 未来重构注意事项

- 任何把平台 bean 属性注入改为 `ioc:lazy-property` 的改动，必须检查 @PostConstruct 阶段是否触发事务提交（DataInitInitializer 模式）——lazy-property 属性在 bean 创建阶段不可用。
- 全 reactor `mvn test` 是此类初始化时序回归的唯一防线（app-erp-all 测试在 reactor 末尾）；模块级 `-pl` 验证测不到。

## 预防差距

- nop-entropy 平台初始化时序改动未跑 app-erp-all 全量（含数据初始化测试）验证；`TestAuthSeedLoadingProof` 是唯一覆盖 `nop.orm.init-database-data=true` 容器启动路径的 app 侧测试。
