# nop-fixture-bundle Skill：测试夹具可移植包编排（构造 → 导出 → 校验 → 喂测试）

> 来源：`docs/backlog/fixture-bundle-test-data-roadmap.md` M2.2（plan 2026-10-02-0030-1）
> 主文档（必读）：`../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md`（定稿：包格式/manifest schema/导出导入 API/约束边界）

## 触发词

测试夹具跨类共享、夹具跨环境搬运、fixture bundle、夹具包、可移植测试数据、多个测试用同一份底数、base/payload 分层夹具、一批构造数据喂多个测试类。

## 适用场景

- 同一份基础数据（base）+ 多个业务快照（payload）需要被 **≥2 个测试类**复用；
- 构造一次、多环境（同为 H2 的测试实例间）回放；
- 现有 `_cases/<TestClass>/<method>/` 方法私有快照无法满足的跨类共享（框架与 `_cases` 是旁路关系，互不改动）。

## 不使用场景

- 单测试类私有的数据快照——继续用 CHECKING 模式（`_cases` 布局零改动）；
- 部署 seed 内容覆盖——归 `comprehensive-test-data-and-visual-coverage-roadmap.md`（bundle base 层与部署 seed 是 requires 依赖而非复制）；
- 集成用例设计与执行——归 `integration-test-roadmap.md`；
- **喂应用 / app init 装载**——按 roadmap §Non-Goals「内存库 + init 配置测试应用」触发条件门控，**不在本 skill 编排范围**（bundle 装载仅面向测试/演示环境，生产装载更在门控之外）。

## 必读路由（按序）

1. `../nop-entropy/docs-for-ai/03-runbooks/fixture-bundle.md` — 包格式、manifest schema、导出/导入 API、约束边界（权威）；
2. `docs/backlog/fixture-bundle-test-data-roadmap.md` — 横切关注点（敏感数据纪律/seed 联动/导出会话独占）与 Non-Goals 边界；
3. `docs/architecture/testing-strategy.md` — 四类测试资产边界（夹具包资产类别由 roadmap M2.3 扩列）；
4. 消费参考：`app-erp-all/src/test/java/io/nop/app/all/it/TestErpFixtureBundleDirtyImport.java`（验收②）与 `TestErpFixtureBundleBatchEntry.java`（批量入口 round-trip）。

## 五阶段编排

### ① bundle 配置推断

- 据目标 ORM 模型建议 **base 表与业务键**（base 表必须声明业务键列——M1.2 对账契约）与 **payload 表**（剥 PK 重映射）；
- 每表声明 `maskedColumns`（敏感列纪律：PASSWORD/SALT 形列强制 masked；`MASKED-BUNDLE-SEED` 占位）；
- 复合主键表不可作 payload（剥 PK 依赖平台单列生成语义，导入侧具名拒绝）。

### ② 构造脚本编排

- 构造必走 **`@BizMutation` / GraphQL 业务逻辑入口**（JSON 请求序列落盘）——JDBC 直操不经 `AutoTestOrmHook` 不可见（导不出）；
- 构造体在 `FixtureBundleRecordingSession.run(...)` 窗口内执行；窗口内同时只允许一个录制会话（JVM 独占 tryLock）。

### ③ 录制会话导出

- `FixtureBundleExporter.export(bundleDir, config, session, orm)` —— 按收集 ID 于 fresh session 重载全行、分层落 `base/` 与 `snapshots/<name>/`、masked 占位、SHA-256、manifest 写出；
- **系统表（`io.nop.sys.*`/`nop_sys_*`）自动跳过**——不配置、不导出；
- 会话内已持久化后被删除的行进 `captureGaps`（同会话 save+delete 抵消的行从未入库、不产生 gap）。

### ④ manifest/校验器独立校验

- `FixtureBundleValidator.validate(bundleDir)` 只读磁盘态：行数↔CSV、SHA-256（用途 = 检测手改/截断，非防篡改）、captureGaps 对账、列指纹漂移（fail-fast/tolerant）、敏感列扫描；
- **bundle 入 git 前的 repo 级敏感门控是消费方 plan 义务**（successor = 首个真实 bundle 入库的 plan，已登记 M1.1 Deferred）；本 skill 编排的练习产物只落 `target/`。

### ⑤ 喂测试

- 导入 API：`new FixtureBundleImporter().importBundle(bundleDir, orm)`——base 业务键对账不覆盖、payload 剥 PK 重映射 + to-one 重写 + 悬空引用具名拒绝、`version/delVersion` 置初值、系统表拒、`includeLogicalDeleted=true` 拒；
- 批量入口（M2.1，测试/演示 profile）：`IBatchTaskRunner.execute` 触发 `fixture-bundle.batch.xml`（消费侧 processor bean 范式见 `TestErpFixtureBundleBatchEntry`）；
- **幂等消费纪律**：base 对账天然幂等；payload 幂等仅同 Importer 实例内有效——**跨进程重复导入 payload 会产生重复行**，bundle 一次性导入 + 测试 fresh 库语义；
- **app 装载边界声明**：喂应用 / app init 装载按 roadmap §Non-Goals 门控，本 skill 不编排该场景。

## 反模式表

| 反模式 | 后果 | 正解 |
|--------|------|------|
| 测试 beans 文件命名为 `app.beans.xml`/`app-*.beans.xml` | 同 JVM 全部测试隐式装载（M1.3 实证 isAppBeans 扫描规则） | 显式装载：`@NopTestConfig(testBeansFile=...)`；命名避开 `app*` |
| 以「既有测试仍绿」论证 beans 装载生效 | 静默 no-op（M1.3 v2 证伪） | 正向判别断言（容器解析 bean `instanceof` 预期类型） |
| 同 id 覆盖平台 bean 缺 `ioc:allow-override="true"` 或缺 `xmlns:ioc` | duplicate-bean-definition 启动失败 / 解析错误（orm-defaults 先注册） | 三件齐备（键名点分 `nop.ioc.app-beans.files`） |
| 构造走 JDBC 直插 | hook 不可见 → 导不出 | `@BizMutation`/GraphQL 业务逻辑入口 |
| 信任直插的引用完整性 | 悬空引用静默成功（平台不校验直插） | 导入器具名拒绝已内建；勿绕过 |
| 敏感列不标 masked | 校验器拦截；真实密钥列入库风险 | maskedColumns 声明 + repo 级门控 successor 义务 |
| 依赖导入器做跨进程幂等 | payload 重复行 | 一次性导入 + fresh 库语义 |
| 把 bundle 提交进生产 classpath / 生产调度目录 | 违背生产装载 Non-Goal | 测试/演示 profile 边界（job-scheduling.md 登记） |
| 手工改 bundle CSV 后不重导出 | sha256 漂移被校验器拦截（设计如此） | 重新导出或按计划修订流程 |
