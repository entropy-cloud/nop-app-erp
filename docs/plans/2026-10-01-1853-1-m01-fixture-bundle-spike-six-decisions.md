# 2026-10-01-1853-1 M0.1 fixture-bundle Spike 六裁决

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/fixture-bundle-test-data-roadmap.md` §Work Item Status M0.1（Spike 六裁决）
> Related: `docs/plans/00-plan-authoring-and-execution-guide.md`；后续 M1.1/M1.2/M1.3 依赖本计划 6 条 Decision
> Audit: required

## Current Baseline

- roadmap §当前基线 与 §框架/平台复用 已于 2026-10-01 经 iteration 2 可执行性审查在活码中逐行实核（复用表 12 行路径/签名全命中、基线数字复算一致、三耦合点源码级证实），本计划直接引用该实核结论作为事实底座，关键锚点：
  - `AutoTestOrmHook`（nop-autotest-core `.../execute/AutoTestOrmHook.java`）实现 `IOrmDaoListener + IOrmInterceptor`；`AutoTestCase.java:179` `new AutoTestOrmHook()`（非 IoC bean）、:208 经 `BeanContainer.tryGetBean("nopOrmSessionFactory")` 取**容器级共享** `IOrmSessionFactory`、:180-181 addDaoListener/addInterceptor、:234-237 `complete()` removeInterceptor 注销；`SessionFactoryImpl.java:243-256` addInterceptor 直接改 factory 级可变 interceptor 列表（copy-on-write ArrayList）——**全局可变、非 session 级、非 ThreadLocal**。
  - `AutoTestCase.initDao` 硬耦合：`caseData == null` 时 `:164 caseData.getInitVars()` NPE（`useSnapshot=false` 时 `initCaseDataDir()` :134-135 直接 return）；`initBeans()` :219 每方法 `container.restart()` 使已注入引用失效；hook 注册无关闭收集配置项。
  - `EntityRow` `orm_forEachInitedProp` 仅已初始化列；`AutoTestCaseDataSaver` output 行带 `_chgType` 仅变更行——**两者都不是全行导出**；`AutoTestCaseDataBaseInitializer` public、单事务（`txn.runInTransaction(REQUIRED)` :140）、`getFiles` 末尾 `Collections.sort` 纯字母序、只为 case 自身 CSV 表建表。
  - `OrmModel.java:78` `sortEntityModelInTopoOrder(Collection<IEntityModel>)` 存在可用于子集；平台 DDL 九方言 xlib 零 FK（`<to-one>` 为逻辑 join，引用完整性由 `ObjMetaBasedValidator.validateRefValue` 保障）。
  - `ExportDbTool` 自建 DataSource 不走 ORM（nop-batch-exp，本仓 0 依赖）；`DefaultBizEntityExporter.exportByQuery` 已实现返回 `CompletionStage<WebContentBean>`；`IBizEntityImporter` 占位实现调用即抛（禁依赖）。
  - 本仓 default 序列已推 `NEXT_VALUE=100000`（`_init-data/zz-sequence-advance.sql`）；手工插自定义 ID 不消耗不推进 `nop_sys_sequence`；无 `syncCurrentSeq`/`NOP_SEQ` API，序列修正唯一手段 `NopSysSequence` CRUD。
- 剩余差距：六项架构级不确定项未裁决（见 Goals），每项都是 M1.x plan 起草的前置输入；roadmap §当前基线/§框架/平台复用 尚无 spike 结论增补。

## Goals

- 对 M0.1 六裁决各产出一条落盘 Decision（选择 / 替代方案 / 残留风险三要素齐备）：
  1. **① 独立录制会话复用**：在非测试类中复刻 hook 注册/注销，跑构造并提取收集结果；解决 `caseData` 非空前置、`container.restart()` 引用失效、注销 try/finally 三个耦合点的可行路径。
  2. **② 并发隔离与注销保证**：hook 实例字段 + 容器级全局注册 → 导出会话独占/排队口径与注销兜底。
  3. **③ 导入 ID 策略**：(a) 剥 PK 交平台生成 + `oldId→newId` 映射 vs (b) preserve 原 ID；(a) 分支冲突面由原型 B 实测、(b) 分支按已实核序列机制事实 + 源码判定；在 `NEXT_VALUE=100000` 前提下定死「后续取号无冲突」断言口径。
  4. **④ 全行导出选型**：ORM + `CsvHelper` 自写 / `ExportDbTool` / `IBizEntityExporter.exportByQuery` 三选一，裁决 nop-autotest 的依赖方向代价。
  5. **⑤ 拓扑序与引用完整性边界**：直插是否触发 `validateRefValue`、拓扑序的收益定位（引用完整性 vs 业务前置态可读性）、子集排序 API 适用性。
  6. **⑥ 载体位置与打包可见性**：bundle canonical 位置与发现规则 + 可见性矩阵（JUnit ✓ / runner.jar E2E ✓/✗）。
- roadmap 增补：§当前基线 与 §框架/平台复用 各增补 spike 结论与证据（仅增补，不得增删或重排工作项）+ §审查记录 一行。
- 原型代码可丢弃（spike 性质）；上述文档改动为必需交付物。

## Non-Goals

- 不实现 M1.1/M1.2 的正式框架代码（包格式/导出器/导入器的生产化属于后续工作项）。
- 不修改本仓 `module-*/model/*.orm.xml`（ORM 保护区域零触碰）。
- 不触碰 `_init-data/**`（seed 联动义务不触发）。
- 不修复 `AutoTestCaseDataSaver.removeInputTable` variant 缺陷（横切 7：未触碰该路径则按「不适用」在 Closure 段记录）。
- 不改变既有 `_cases/` 布局与 CHECKING 模式行为。

## Task Route

- Type: `verification or audit work`（spike：事实勘验 + 原型验证 + Decision 落盘；原型代码可丢弃）
- Owner Docs: `../nop-entropy/docs-for-ai/02-core-guides/testing.md`（回写目标为 roadmap 增补段，非 owner doc 正文——M0.1 完成判据限定）
- Skill Selection Basis: `Skill: nop-testing`（测试基础设施方法的单一入口；其路由的 testing 文档为本任务核心阅读面）。保护区域纪律：nop-entropy 为外部仓库，本计划跨仓读 + 原型适用 `auto + dual-agent-approval`——草案审查通过后、实施开始前，须两名独立子代理分别批准（批准记录落盘本计划）。

## Infrastructure And Config Prereqs

- 跨仓验证命令：`cd ../nop-entropy && ./mvnw test -pl nop-autotest/nop-autotest-core -am`（模块 pom 实存，含 1 个既有测试类）。
- 原型运行环境：JUnit + H2 内存库（沿 nop-autotest-core 既有测试形态）；无新增端口/密钥/外部服务。
- 长输出按 `docs/context/conventions.md` 落盘 `_tmp/2026-10-01-1853-1-*.log` 后摘要。
- 回滚策略：原型代码不进入 git（如临时落盘于 `_tmp/` 或明确标注 spike 的测试文件，裁决后按「原型处置」小节处理）；roadmap 增补为纯文本可 git revert。

## Execution Plan

### Phase 1 - 六裁决事实勘验与 Decision 草案

Status: completed
Targets: 本计划 `## Decisions` 节（Phase 3 摘录落盘 roadmap）；nop-entropy 源码只读
Skill: `nop-testing`

- Item Types: `Decision`（6 条，每条含选择/替代方案/残留风险）
- Prereqs: **dual-agent-approval 已获两名独立子代理批准（见 Task Route 与 Approval Record——Phase 1 即含跨仓读，属 Guard 范围，批准先于 Phase 1 开始）**

- [x] Decision ①②：读 `AutoTestOrmHook` / `AutoTestCase` / `SessionFactoryImpl` / `BeanContainer` 相关源码，起草「独立录制会话复用」与「并发隔离/注销保证」裁决——注册/注销复刻路径、三耦合点解法、导出会话独占口径、try/finally 兜底形态，回填 `## Decisions` ①②
      - Skill: `nop-testing`
- [x] Decision ③：基于已实核的序列机制事实（`NEXT_VALUE=100000`、手工 ID 不消耗序列、无 `syncCurrentSeq` API）+ 源码判定 `seq-default` tagSet / 业务码列 / 审计列 / 逻辑删除列语义，裁决剥 PK 生成 vs preserve 冲突面，定死「后续取号无冲突」断言口径，回填 `## Decisions` ③
      - Skill: `nop-testing`
- [x] Decision ④：对三候选（自写 ORM 全行导出 / `ExportDbTool` / `exportByQuery`）从「构造会话末态自洽性、依赖方向代价（nop-autotest → nop-batch-exp 是否引入新依赖边）、全行 vs 已初始化列」三轴裁决，回填 `## Decisions` ④
      - Skill: `nop-testing`
- [x] Decision ⑤：直插路径是否触发 `ObjMetaBasedValidator.validateRefValue` 的行为观测由原型 B 顺带承载（to-one 引用行导入时的拦截器链观测），结合源码判定裁决拓扑序收益定位与子集排序 API 适用性，回填 `## Decisions` ⑤
      - Skill: `nop-testing`
- [x] Decision ⑥：盘点 nop-entropy 资源装载/VFS 打包可见性机制（test-scope jar、runner.jar 装载面、`_vfs` 资源发现），起草 bundle canonical 位置与发现规则 + 可见性矩阵，回填 `## Decisions` ⑥
      - Skill: `nop-testing`

Exit Criteria:

- [x] 6 条 Decision 草案各含选择/替代方案/残留风险三要素，且每条至少有一个可执行证据（源码行号引用或实测命令输出）——已回填 `## Decisions` 节
- [x] （本地化检查）Decision ③ 的「后续取号无冲突」断言口径已定死三件套（M1.3 验收①前置）

### Phase 2 - 原型验证（可丢弃）

Status: completed
Targets: nop-entropy `nop-autotest-core` test scope（spike 原型，标注处置方式）；`_tmp/` 证据文件
Skill: `nop-testing`

- Item Types: `Proof`（原型实测）、`Add`（临时原型代码，裁决后按处置小节收口）
- Prereqs: Phase 1 Decision 草案成立；**dual-agent-approval 已获两名独立子代理批准（批准记录落盘 Draft Review Record 之后、本阶段开始之前）**

- [x] （Proof | Add）原型 A：非测试类复刻 hook 注册/注销 + 跑构造会话 + 提取收集结果——**实测通过**（`TestM01SpikeRecordingSession` 2/2 绿：独立会话收集构造行 + 会话隔离不串扰）；spike 修正发现：onSave 行数据落 `EntityRow.changedData`（onLoad 才落 initData），全行导出须导出前按 ID 重载
      - Skill: `nop-testing`
- [x] （Proof | Add）原型 B：小数据集剥 PK 导入实测——**实测通过**（`TestM01SpikeIdRemap` 2/2 绿：剥 PK 平台生成新 ID + oldId→newId 映射可编码 + 再取号无冲突；悬空 to-one 直插成功且可回读 = Decision ⑤ 行为佐证）
      - Skill: `nop-testing`
- [x] （Proof）原型处置：文件清单与处置方式已登记于「原型处置」节（保留为 M1.1 参考实现）
      - Skill: `none`

Exit Criteria:

- [x] 原型 A/B 实测运行留证（4/4 测试绿 + 既有 `TestTestClockAnchoredSim` 5/5 零回归；输出摘要已按 conventions 落盘 `_tmp/2026-10-01-1853-1-spike-run4.log`、`_tmp/2026-10-01-1853-1-spike-run5.log`）
- [x] 原型与 Decision 草案冲突时以实测为准——已修订：Decision ④ 证据补「onSave 落 changedData」收集粒度事实；Decision ⑤ 增行为佐证；其余无冲突

### Phase 3 - Decision 落盘 roadmap 与收口

Status: completed
Targets: `docs/backlog/fixture-bundle-test-data-roadmap.md`（§当前基线/§框架/平台复用/§审查记录 仅增补）
Skill: `none`

- Item Types: `Add`（文档增补）、`Proof`（收口核验）
- Prereqs: Phase 2 完成（或 Phase 1 Decision 在无原型必要处直接成立——须逐条注明「原型未覆盖/不需要」理由）

- [x] roadmap §当前基线 与 §框架/平台复用 增补 6 条结论与证据（仅增补结论与证据，不增删/重排工作项）；§审查记录 追加 M0.1 完成行
      - Skill: `none`
- [x] 失败出路核对：6 条 Decision 若触发情景 A/A′/B/C 任一，按 roadmap M0.1 失败出路表执行对应收口动作并登记。**情景 A/A′ 涉及工作项结构变更的收口动作（M1.1 重定义 / M1.x-M2.x 移出状态块），本计划仅做标记与登记，结构变更本身留待人工审查（roadmap 规则 8：M0.1 不得增删或重排工作项）**——实测结果：情景均未触发
      - Skill: `none`

Exit Criteria:

- [x] roadmap 三段增补落盘且工作项结构零变化（§当前基线 新增「M0.1 spike 结论增补」5 条 + §框架/平台复用 新增 `UuidSequenceGenerator` 行 + §审查记录 M0.1 完成行 + 最后更新行同步；工作项表零触碰）
- [x] 状态流转：roadmap M0.1 `ready` → `done` 仅在独立结束审计通过后执行（审计通过后随提交翻转）
- [x] 失败出路核对：6 条 Decision 全部为正向裁决，情景 A/A′/B/C 均未触发（Decision ③ preserve 冲突面实证确认不可调和，但属 (a)/(b) 比选结论而非情景 B 触发——remap 主策略成立）

## Decisions

> 六条 Decision 的计划内落盘锚点；Phase 1 逐条回填（三要素 + 证据锚点），Phase 3 摘录增补至 roadmap。回填前保持占位形态即代表该 Decision 未裁决。

### Decision ① 独立录制会话复用

- 选择：**可行**。非测试类复刻 `initDao` 本质行——`BeanContainer.tryGetBean("nopOrmSessionFactory")` + `addDaoListener/addInterceptor` + `ContextProvider` 用户戳——完全不继承 `AutoTestCase`、不触碰 `caseData`/`initDao` 耦合点；`container.restart()` 引用失效通过「会话内按名取 bean、不持有跨 restart 的注入引用」规避；注销以 try/finally 兜底（原型 `M01SpikeRecordingSession.run`）。bootstrap 复刻 `NopJunitExtension.beforeAll` + `initBeans`：`beginTest` → 测试配置（ALL_LAZY / H2 内存库 / `nop.orm.init-database-schema=true`）→ `CoreInitialization.initialize()` → `container.restart()`。
- 替代方案：继承 `AutoTestCase`/`JunitAutoTestCase`（拒绝——被 `_cases` 目录归属与 caseData NPE 锁死，正是要脱离的形态）；修改平台 `AutoTestCase` 增加无 caseData 模式（拒绝——平台侵入过大，正式 API 形态留给 M1.1 评估）。
- 残留风险：模块测试 classpath 必须含 IoC 实现（nop-ioc）——本仓通过 nop-autotest-junit 传递获得，纯 nop-autotest-core 消费者需自行补 test-scope 依赖（本原型已按增量批准处理）；正式 API 需重审 restart 语义与并发 restart 兼容性。
- 证据锚点：`AutoTestCase.java:164/:179-181/:208/:219`；`NopJunitExtension.java:30-47`；`M01SpikeRecordingSession.java`；`TestM01SpikeRecordingSession` 2 测试。

### Decision ② 并发隔离与注销保证

- 选择：**导出会话 JVM 级独占（单锁 fail-fast）**，不排队不并发；注销保证 = try/finally + 注册/注销对称 API（`addDaoListener/addInterceptor` ↔ `removeDaoListener/removeInterceptor`）。
- 替代方案：ThreadLocal 隔离（拒绝——需改 `SessionFactoryImpl` 拦截器分发机制，平台侵入）；引用计数并发多会话（拒绝——收集结果跨会话混淆，违背包自洽）。
- 残留风险：M2.1 批量入口若同 JVM 调度多个导出需串行队列（登记为 M2.1 Decision 输入）；正式实现应将 synchronized 替换为 tryLock + 语义化 ErrorCode。
- 证据锚点：`SessionFactoryImpl.java:243-256`（factory 级可变列表、非 session 级非 ThreadLocal）；`M01SpikeRecordingSession.java` EXCLUSIVE_LOCK。

### Decision ③ 导入 ID 策略（含「后续取号无冲突」断言口径）

- 选择：**(a) 剥 PK 交平台生成 + `oldId→newId` 映射**。冲突面分析（已实核事实 + 源码判定）：`SysSequenceGenerator.generateLong(key, useDefault)`——`seq-default`（useDefault=true）回落 default 序列行（本仓 `NEXT_VALUE=100000`、cacheSize 批量单调取号）；纯 `seq` tag（useDefault=false）无对应序列行则 `ERR_SYS_NO_SEQ` fail-fast；`isUuid`/snowflake 行为随机/雪花 ID（碰撞概率可忽略但非单调）。preserve (b) 的冲突面：导入 ID 落在目标序列未来取号区间（`NEXT_VALUE ≤ max(importedId)`）时必然冲突，且手工 ID 不消耗不推进 `nop_sys_sequence`——不可调和（roadmap 情景 B 判据证实）。
- **「后续取号无冲突」断言口径（M1.3 验收①使用）**：导入完成后 (i) 逐表断言新 ID ∉ 包内 oldId 集；(ii) 若目标序列为数值单调型（seq 行存在且非 uuid/snowflake），断言 `nop_sys_sequence.NEXT_VALUE > max(新 ID)`；(iii) 通用口径：导入后再取一个新号，断言 ∉ 已导入 ID 集（对 uuid/snowflake 型为经验性断言）。
- 替代方案：(b) preserve（降级预案保留于 roadmap 情景 B：manifest 增 `idStrategy: preserve|remap`，冲突即失败不静默覆盖）。
- 残留风险：业务码列（UK）跨环境冲突由 M1.2 base 对账 / fail-fast 承担，非 ID 策略问题；uuid 型序列「无冲突」为概率性，正式验收以 (iii) 经验断言为准。
- 证据锚点：`SysSequenceGenerator.java:165-235`（generateLong/syncFromDb）、`OrmEntityIdGenerator.java:45-59/:81-103`（genSeq）、`zz-sequence-advance.sql`（NEXT_VALUE=100000）；原型 B 实测。

### Decision ④ 全行导出选型

- 选择：**ORM + `CsvHelper` 自写全行导出器**。理由：① 与构造会话末态自洽——hook 收集触及行，导出前按 ID 重载取全行（`EntityRow.initData` 仅已初始化列、onSave 仅含已设列；重载后 `orm_forEachInitedProp` 覆盖全部映射列）；② 零新增依赖边（nop-autotest-core 已依赖 nop-orm + nop-core/CsvHelper）；③ `ExportDbTool` 自建 DataSource 不走 ORM（无模型元数据、与会话状态脱节）且引入 nop-batch-exp 新依赖边（本仓 0 依赖）；④ `exportByQuery` 走 BizModel 层返回 `CompletionStage<WebContentBean>`（HTTP 面），层级与返回形态均不适合框架库内嵌行收集。
- 替代方案：`ExportDbTool`（依赖方向不可接受）；`exportByQuery`（层级不对）。
- 残留风险：导出重载对「构造会话中删除的行」需跳过（postDelete 行重载 get 不到——按 `_chgType=D` 语义处理）；「全行」= 触及行的完整行内容，非全表全量（范围口径锁定）。
- 证据锚点：`EntityRow.java:58-93`、`ExportDbTool.java:121`（自建 DataSource）、`DefaultBizEntityExporter.java:36`、nop-autotest-core pom 依赖检视。

### Decision ⑤ 拓扑序与引用完整性边界

- 选择：**直插不触发 `validateRefValue`**（源码主证据：该 validator 仅由 `CrudToolProvider.newValidator` 构造、仅被 `CrudBizModel.java:709/:987` 写入口使用；行为佐证：原型 B 悬空 to-one 直插成功且可回读）。拓扑序的收益定位 = **业务前置态可读性 + 确定性装载序**（与 `DataInitInitializer` 拓扑序语义一致），非 DB 约束需要（平台 DDL 九方言零 FK 已成事实）；导入器采用 `sortEntityModelInTopoOrder(Collection)` 子集排序（无参版只返回全模型不可用）。
- 替代方案：按 manifest 表清单字母序不排序（拒绝——与平台装载语义不一致、业务前置态不可读）；依赖 DB FK（拒绝——平台不生成 FK）。
- 残留风险：悬空引用在直插路径**静默成功**（本原型实证）→ M1.2 导入器必须自带引用完整性断言（导入前校验 to-one 目标在包内或目标库已存在），不能依赖平台兜底。
- 证据锚点：`ObjMetaBasedValidator.java:431/:437`、`CrudBizModel.java:709/:987`、`CrudToolProvider.newValidator`、`ddl*.xlib` 九方言零 FK、`TestM01SpikeIdRemap.testDirectInsertWithDanglingToOneSucceeds`。

### Decision ⑥ 载体位置与打包可见性

- 选择：**bundle 载体 = 消费方模块的 test-scope classpath 资源**（VFS 目录约定 `_vfs/<module-short>/fixture-bundle/<bundle-name>/`，manifest 为发现入口——精确布局由 M1.1 Decision 定稿，本裁决只定 scope 与可见性）。可见性矩阵：JUnit/集成测试（test classpath）✓；runner.jar E2E **✗**（test-scope 资源不进 main 打包，且生产打包不应携带测试夹具——与 Non-Goals「生产环境夹具装载」一致）；同 JVM 其他模块测试 ✓（classpath 合并）。
- 替代方案：main scope 载体（拒绝——测试夹具进生产打包，违背敏感数据纪律与生产装载 Non-Goal）；外部文件系统路径（拒绝——脱离 VFS 资源发现体系，跨环境搬运性更差）。
- 残留风险：若未来触发 Non-Goal「内存库 + init 配置测试应用」（E2E 需要 bundle），须另裁决 runner.jar 侧载体（main scope 或外挂目录）——登记为该 Non-Goal 的前置输入。
- 证据锚点：`_vfs/main/orm/app.orm.xml` 测试资源被 `OrmModelLoader` 主合并点发现（原型 A 实证）、`_vfs/nop/spike/beans` 覆盖文件仅测试 classpath 可见、`playwright.config.ts` runner.jar 启动契约。

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_67d0b74d, fresh session) — 1 MAJOR（Phase 1 Targets 悬空引用「Decision 段」，补搭 `## Decisions` 脚手架 ①-⑥）+ 6 MINOR（原型条目逐条标型 / 补 Deferred 节 / ⑤ 行为观测改由原型 B 顺带承载 + preserve 分支改「已实核事实+源码判定」/ Phase 1 Prereqs 补 dual-agent-approval 先行注记 / 原型保留路径交接口径 / 情景 A/A′ 结构变更仅标记不执行）；基线事实 9 条活码抽查全命中。
- Independent draft review iteration 2: acceptable as-is (agent_67d0b74d 增量复审) — 七处修订六处完全落实、一处 Goals ③ 摘要残留「实测」一词（R-1 非阻塞）已顺手收紧为「(a) 分支由原型 B 实测 + preserve 分支按已实核事实判定」；反松弛词全档扫描零命中、无新引入缺陷；复审备注 R-2（active 预置时序）以本记录落盘即自洽。计划转为可执行契约，dual-agent-approval 批准先行于 Phase 1。

## Dual-Agent Approval Record（保护区：nop-entropy 跨仓读 + 原型）

- Approver A: **APPROVED with conditions**（agent_a682ad46，fresh session，risk & boundary 视角，2026-10-01）。条件与执行口径：A1 期间不在 nop-entropy 执行任何 git add/commit、原型文件名含 plan-id 前缀、原型处置文件清单为硬性交付——执行者接受；A2 `nop-testing` 为 docs/skills/README.md 注册表外的工具原生技能（实存于 `.opencode/skills/nop-testing/SKILL.md`，草案审查者已实核）——执行者直接读该 SKILL.md 并在计划内如实标注来源，不虚构注册表加载记录，roadmap 修订时同步斟酌技能名口径；A3 范围冻结为「跨仓只读 + nop-autotest-core test scope 新增原型文件 + 本仓 roadmap 增补」，越面即重新走 dual-agent-approval——执行者接受。
- Approver B: **APPROVED**（agent_4a59c48a，fresh session，技术方案正确性与 roadmap 忠实度视角，2026-10-01）。附注与执行口径：B1 nop-autotest-core 无现成容器引导测试形态，引导代码参照兄弟模块复刻，**引导成本畸重不得误判为失败出路情景 A**（A 判据是隔离/耦合点不可解，非搭建成本）——执行者接受；B2 Decision ⑤ 源码级预判：`validateRefValue` 仅经 `CrudBizModel` 写路径可达（`CrudBizModel.java:709/987` → `CrudToolProvider.newValidator` → `ObjMetaBasedValidator.java:431→:437`），直插不经过——回填时以源码行号为主证据、原型 B 负向观测为辅证；B3 批准时序以 Phase 1 Prereqs 与 Task Route 为权威（批准先于 Phase 1）——已按此执行。
- 增量批准（原型实跑发现的范围缺口）：nop-autotest-core 不依赖 `nop-ioc`（IoC 实现由 nop-autotest-junit 带入），`CoreInitialization.initialize()` 在该模块测试 classpath 停于 level 2900（IoC priority=4900 不运行），`BeanContainer.instance()` 抛 bean-container-not-initialized。最小修法 = pom.xml 新增单行 test-scope `nop-ioc` 依赖。Approver A **APPROVE**（agent_a682ad46 续会话，附条件：① 同轮跑既有测试类 `TestTestClockAnchoredSim` 确认 classpath 变化零回归；② 在「原型处置」节登记该 pom 单行 diff——原型删除时同步 revert 该行，超此单行的 pom 改动重新触发 dual-agent-approval）。Approver B **APPROVE**（agent_4a59c48a 续会话，实测核验 pom 依赖面与 IocCoreInitializer/错误码实存；随批登记：pom 修改属文件写非只读，须同批登记原型处置——原型删除时依赖行一并 revert 或留作 M1.1 使能项，二选一须写明；Decision ⑤ 证据采纳无需再批）。

## Closure Gates

- [x] 范围内行为完成（6 条 Decision 落盘 + roadmap 三段增补）
- [x] 相关文档对齐（roadmap 增补为计划级 owner-doc 义务；nop-entropy owner doc 正文不改动——M0.1 判据限定）
- [x] 已运行验证（跨仓 `./mvnw test -pl nop-autotest/nop-autotest-core -am` 原型相关测试通过；本仓 compliance checker 零新增漂移）
- [x] 无范围内项目降级为 deferred/follow-up（失败出路触发的收口动作按 roadmap 执行，不算降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## 原型处置

- **处置方式：保留为 M1.1 参考实现**（未提交文件顺延至 M1.1 工作区，由 M1.1 计划在其自身 dual-agent-approval 范围内决定入库形态或废弃；pom 依赖行按 Approver B 口径选「留作 M1.1 使能项」）。
- 原型文件清单（全部位于 `../nop-entropy/nop-autotest/nop-autotest-core/`，未提交）：
  - `src/test/java/io/nop/autotest/core/spike/M01SpikeBoot.java`（bootstrap 复刻）
  - `src/test/java/io/nop/autotest/core/spike/M01SpikeRecordingSession.java`（独立录制会话原型，Decision ①② 载体）
  - `src/test/java/io/nop/autotest/core/spike/IocSpikeConfigs.java`（IoC 配置名镜像）
  - `src/test/java/io/nop/autotest/core/spike/TestM01SpikeRecordingSession.java`（原型 A 驱动，2 测试）
  - `src/test/java/io/nop/autotest/core/spike/TestM01SpikeIdRemap.java`（原型 B 驱动，2 测试）
  - `src/test/resources/_vfs/main/orm/app.orm.xml`（spike 双实体模型，Decision ⑥ VFS 可见性载体）
  - `src/test/resources/_vfs/nop/spike/beans/m01-fixture-spike.beans.xml`（UuidSequenceGenerator 覆盖，Decision ③ 载体）
  - `pom.xml`：单条 test-scope `nop-ioc` 依赖（含 plan-id 注释共 8 行 diff——XML 格式下「单行」实为单条 `<dependency>`，与批准口径一致；删除时同步 revert——Approver A 条件② / B 登记项）
- 保留即视为 M0.1 范围外的状态延续，已在 Closure Status Note 登记；M0.1 提交足迹不含任何 nop-entropy 文件（Approver A 条件①遵守：期间未在 nop-entropy 执行任何 git add/commit）。

## Deferred But Adjudicated

- 起草时无 deferred 项；失败出路触发的收口按 roadmap M0.1 失败出路表执行，不计 deferred。

## Closure

Status Note: 六条 Decision 全部正向裁决并经独立结束审计抽验（6/6 证据锚点命中活码）；roadmap 事实增补严格限定为增补、工作项结构零变化；nop-entropy 零提交、原型 7 文件 + pom 单条依赖保留为 M1.1 参考实现（登记于「原型处置」节）；原型 A/B 4/4 绿 + 既有测试 5/5 零回归；失败出路 A/A′/B/C 均未触发。审计 3 Minor（/tmp 证据易失 / pom 措辞 / checker 复跑记录）已随关闭整改。

Closure Audit Evidence:

- Auditor / Agent: independent subagent agent_9821fb53（fresh session，2026-10-01）
- Evidence: 审计 ACCEPT（0 BLOCKER / 0 MAJOR / 3 MINOR 已整改）；审计方独立复跑 `node tools/check-plan-gates.mjs --strict` PASS（exit 0，0 new violations，82 baseline files）；实仓核验 nop-entropy HEAD 未动（`86ad2c389d`）+ 原型 7 文件实存 + surefire 报告 `TestM01SpikeIdRemap` 2/2、`TestM01SpikeRecordingSession` 2/2、`TestTestClockAnchoredSim` 5/5；证据日志已复制入仓 `_tmp/2026-10-01-1853-1-spike-run4.log`、`_tmp/2026-10-01-1853-1-spike-run5.log`（compliance checker 本批零新增漂移执行者自证于提交前复跑）。

Follow-up:

- M1.1 起草时以「原型处置」节 7 文件为参考实现起点，并裁决 pom test-scope `nop-ioc` 依赖行的去留（登记项，非缺陷）。
