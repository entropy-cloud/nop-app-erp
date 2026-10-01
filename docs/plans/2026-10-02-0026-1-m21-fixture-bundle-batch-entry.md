# 2026-10-02-0026-1 M2.1 fixture-bundle 批量/CLI 导出导入入口

> Plan Status: completed
> Last Reviewed: 2026-10-02
> Source: `docs/backlog/fixture-bundle-test-data-roadmap.md` §Work Item Status M2.1（批量/CLI 导出导入入口：形态裁定 + 可调度调用测试）
> Related: M1.1/M1.2/M1.3（框架与验收已 done）；M2.3（消费落地）依赖本计划入口
> Audit: required

## Current Baseline

- **框架已 done**：`io.nop.autotest.bundle`（导出器/导入器/校验器/录制会话/manifest）入库 nop-entropy（`9b0a704f79`+`bb2edc774c`+`2f7630edbe`）；验收①②③全过；runbook 定稿。框架类为**普通类非 IoC bean**——M1.1 批准条件明令「不为新包添加自动装配入口（IoC beans.xml/ServiceLoader/静态 hook 注册）」，任何入口形态的消费方必须**显式定义 bean 或显式 new**。
- **依赖方向事实**：`app-erp-all` 对 `nop-autotest-junit` 仅 test scope（pom :224-228）；main scope 无 nop-autotest 依赖。batch-dsl 任务在 app 运行时调度（main classpath）——main-scope 入口 = 生产 classpath 引入测试框架 jar（与 Non-Goal「生产环境夹具装载」张力）。
- **范式先例**：`TestErpCrmLeadScoringRecalcJob`（module-crm）/`TestErpQaSpcSamplingEvaluateBatch`（module-quality）/`TestErpFinApDocBatchWiring`（app-erp-all）——`IBatchTaskRunner.execute` 测试范式成熟；nop-batch-dsl `orm-reader→processor` 模式成熟（本仓 12 个 batch.xml/35 个 job.yaml）。
- **约束**（roadmap M2.1）：不得依赖 `IBizEntityImporter` 占位实现；playwright runner.jar 启动契约（50+ `-D` 开关）列为 Decision 输入——其裁答的边界事实：E2E/生产运行时面看不见 test 资源（恰是 route a 与 Non-Goal 自洽、route b 生产可达张力的分界）；CLI 路径将改动 `nop-runner/nop-cli-core`（外部仓库更重保护面）。
- 剩余差距：无批量/CLI 入口；job-scheduling.md 未登记该资产类别。

## Goals

- **入口形态裁定**（Decision I）：三条路线裁决——(a) batch-dsl 任务 + processor（test-scope 落位）/ (b) 独立 CLI 子命令（nop-cli-core 改动）/ (c) main-scope 编程入口。产出入「可被调度调用的入口 + 一个调用测试」。
- **调用测试**：沿 `IBatchTaskRunner.execute` 或所选范式的可调度调用证明。
- **owner doc 登记**：`docs/architecture/job-scheduling.md` 增 fixture-bundle 入口资产段。

## Non-Goals

- 不做生产环境夹具装载（roadmap §Non-Goals 门控）；不做 M2.2 skill/M2.3 消费示例。
- 不改 `IBizEntityImporter`；不改既有 12 个 batch.xml/35 个 job.yaml 的任何语义。
- 不触碰 `module-*/model/*.orm.xml`/`_init-data/**`。

## Task Route

- Type: `architecture change`（入口形态 + 资产类别登记）
- Owner Docs: `docs/architecture/job-scheduling.md`
- Skill Selection Basis: `Skill: nop-backend-dev`（批次任务/调度面路由）。保护区：视 Decision I 裁决而定——(a) 本仓 test scope 写入（无保护区）；(b) 触碰 nop-cli-core（外部仓库，`auto + dual-agent-approval` 加重保护面）。Guard 列为 dual-agent-approval——**无论裁决为何均走双批准**（roadmap M2.1 Guard 列无差别标注）。

## Infrastructure And Config Prereqs

- 验证命令：route a/c 取本仓 `mvn test -pl app-erp-all -Dtest=…` 形状；route b 按 roadmap 横切 8 惯例就地固化跨仓命令。日志落 `_tmp/2026-10-02-0026-1-*.log`。
- 回滚策略：新增文件为主——revert 单提交即可。

## Execution Plan

### Phase 1 - Decision I 裁决与入口实现

Status: completed
Targets: 按 Decision I 裁决落位
Skill: `nop-backend-dev`

- Item Types: `Decision`（1 条核心）、`Add`
- Prereqs: dual-agent-approval 双批准（批准先于实施）

- [x] （Decision）Decision I 裁决 route (a)（双批准一致确认；三路线输入+失败出路分枝 A-3+C2 IBizEntityImporter 显式化全落盘）
      - Skill: `nop-backend-dev`
- [x] （Add）route (a) 入口落地：`fixture-bundle.batch.xml`（loader done 标记排空）+ `fixture-bundle-batch.beans.xml`（testBeansFile 显式装载、非 app* 命名）+ `FixtureBundleImportBatchProcessor`（消费侧显式 bean 包裹框架类，C2 禁令承载）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 入口编译/装载通过（正向判别断言 instanceof 过——C1 判别要求）；零既有任务语义修改

### Phase 2 - 可调度调用测试

Status: completed
Targets: 测试类（沿所选范式）
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 完成

- [x] （Proof）`TestErpFixtureBundleBatchEntry` 1/1 绿：`IBatchTaskRunner.execute` 真实调度面 round-trip（正向判别+具名字段断言 baseImported=0/baseSkipped=1/payloadImported=1[脏环境语义]+回读实体 FROM 重映射——N2）；bundle 仅落 `target/`
      - Skill: `nop-backend-dev`
- [x] （Proof）既有测试零回归（app-erp-all 全模块跑法 78/0/0——A-4 硬门控留痕 ）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 新测试绿 + 全模块 78/0/0（A-4 硬门控复验留痕 `_tmp/2026-10-02-0026-1-appfull-summary.log`）

### Phase 3 - owner doc 与 roadmap 回写

Status: completed
Targets: `docs/architecture/job-scheduling.md` + roadmap
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 2 完成

- [x] job-scheduling.md 增 fixture-bundle 入口段（形态/调度边界[不进生产调度目录——N1]/禁依赖/Non-Goal 衔接）
      - Skill: `none`
- [x] roadmap：审查记录行 + 状态翻转（独立结束审计 ACCEPT 后凭授权执行）
      - Skill: `none`

Exit Criteria:

- [x] owner doc 落盘；roadmap 审查记录行落盘

## Decisions

### Decision I — 入口形态裁定

- 选择：**route (a) test-scope batch-dsl**（双批准一致确认预判）——batch 任务 `fixture-bundle.batch.xml` + 显式 bean 定义 processor，落位 `app-erp-all` test scope VFS（`/nop/test/batch-task/` + `/nop/test/beans/`）；beans 装载 = `@NopTestConfig(testBeansFile=...)`（C1，与 `nop.ioc.app-beans.files` 同键同机制）；**processor 仅调用 M1.1/M1.2 框架类（FixtureBundleExporter/Importer），绝不触及 nop-batch-biz 的 IBizEntityImporter 占位实现**（C2）；loader 采用 processor 完成标记排空形态（沿 ap-document 批注的「恒非空 = 无限 chunk」教训，避免 orm-reader 语义误用）。
- 替代方案：(b) CLI 子命令——nop-cli-core 外部仓改动重保护面 + 生产可达与 Non-Goal「生产环境夹具装载」直接张力（拒绝）；(c) main-scope 编程入口——生产 classpath 引入测试框架 jar（拒绝）。
- 残留风险：入口仅测试/演示 profile 可达（by design，与 Non-Goal 自洽；生产装载需求出现时按 roadmap Non-Goals 触发条件由人工追加工作项）；processor 单例状态标记在 forkCount=1 共享 JVM 下跨测试类存续——done 标记不可重置会导致同 JVM 第二个调用测试空转（当前仅 1 个调用测试，登记为消费纪律）。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (agent_9a0183aa, fresh session) — 0 BLOCKER/0 MAJOR/6 MINOR 单行级（H1 身份与文件名对齐[必修随转 active]/Phase 2 退出收窄归 Closure Gates/Decision I 补调度机制证据链与失败出路/runner.jar 契约实质化/bundle 落位 target/ 登记/验证命令形状按路线分支）；7 条基线抽查全命中 + route (a) 预判经活码预核技术可行（loadBatchTaskFromPath 路径式装载 + test-scope VFS 可见 + 显式 bean 两先例）。全部修订已随转 active 落实。计划转为可执行契约，dual-agent-approval 先于 Phase 1。

## Dual-Agent Approval Record

- Approver A: **APPROVED with conditions**（agent_5c135c2c，fresh session，风险与边界视角，2026-10-02）。实测：写入面声明可信（module-common-test 以 compile 持有 nop-autotest-junit 但全部消费方 test scope 引用、不达生产 classpath）；beans 隔离关键事实 = isAppBeans 仅认 `app*.beans.xml` 命名（M1.3 REJECT 轮实证）——app-erp-all surefire forkCount=1 共享 JVM 使隐式装载泄漏面具体化。条件（全部接受）：**A-1** 装载机制钉死 `@NopTestConfig(testBeansFile=...)`（优先，per-test-class 隔离；备选 M1.3 v4 三陷阱全数适用）；**禁止**测试 beans 文件命名为 `app.beans.xml`/`app-*.beans.xml`（同 JVM 全测试隐式装载，违 M1.1 条件②精神）；processor 注入沿按 bean id 先例。**A-2** 写入面冻结：本仓 test scope 新增 batch.xml + 测试 beans + 测试类 + 两处文档行；零 nop-entropy 写入、零 main pom 变更、module-common-test 零触碰。**A-3** 失败出路分枝：路线级不可行→回退重裁；route 内 bean 解析失败→route 内修复不重裁。**A-4** closure 全模块跑法复验留痕。
- Approver B: **APPROVE（3 条件 + 4 附注）**（agent_0e2bd2d9，fresh session，技术正确性与 roadmap 忠实度视角，2026-10-02）。核验：三路线枚举忠实（(c) 合法备选）；route (a) 三链机制全活码证实（loadBatchTaskFromPath:114-118 路径式装载/test-scope VFS/processor bean id 显式定义——`ModelBasedBatchTaskBuilderFactory.buildProcessor0` 强转 IBatchProcessorProvider + unknown-bean-for-name 缺陷史反证）；processor 形态不违 M1.1 框架冻结（消费方显式 bean + 显式 new，框架包零触碰）；round-trip 经真实调度面（`execute = syncGet(executeAsync)` = 生产 job.yaml invoker 同面）具有证明力。条件：**C1** bean 机制落 `@NopTestConfig(testBeansFile=...)`（与 nop.ioc.app-beans.files 同键同机制——NopTestConfigProcessor 直译）+ 携带 M1.3 三教训[静默 no-op 命名/同 id 覆盖须 xmlns:ioc+allow-override/正向判别断言]；**C2** Decision I 显式化「绝不触及 IBizEntityImporter」；**C3** 写入面 test scope 确认性重申。附注：N1 job-scheduling 登记段明写不进生产调度目录；N2 round-trip 断言对具名字段与回读实体；N3 batch.xdef 不在本仓、loader 以 12 既有范式落位；N4 pom 行号差 2 可接受。

## Closure Gates

- [x] 范围内行为完成（入口 + 调用测试 + owner doc）
- [x] 相关文档对齐（job-scheduling.md + roadmap 审查记录/状态翻转）
- [x] 已运行验证（调用测试绿 + 全模块零回归；compliance checker 零新增漂移；plan-gates --strict PASS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- 起草时无 deferred 项；生产环境装载按 roadmap §Non-Goals 触发条件门控，不属本计划 deferred。

## Closure

Status Note: route (a) 入口经真实调度面（IBatchTaskRunner.execute = 生产 invoker 同面）round-trip 证明可调度调用；Decision I 三路线裁决双批准一致确认；C1 三教训（静默 no-op 命名/正向判别/同 id 覆盖纪律）全数落地；脏环境语义断言如实呈现。审计后转 done。

Closure Audit Evidence:

- Auditor / Agent: independent subagent agent_f6c8cae0（fresh session，2026-10-02）
- Evidence: 审计 **ACCEPT**（0 BLOCKER / 0 MAJOR / 3 MINOR 闭包簿记类，全部履行：M2.1 日志条目补写[M1.3 遗留验收②测试类从未入库实锤——83a29758b 提交信息失实，本闭包收编使 78/0/0 自 HEAD 可复现]/回读断言判别力弱化登记[证明力由 payloadImported=1 具名字段承载]）；审计方独立复跑 TestErpFixtureBundleBatchEntry exit 0 + plan-gates --strict PASS；批准条件 A1-A4/B C1-C3/N1-N2 逐条实码核对。

横切关注点 7（`AutoTestCaseDataSaver.removeInputTable` 路径）：**不适用**——本计划零触碰，按 roadmap 规则在此登记一次。

Follow-up:

- （仅非阻塞跟进；已确认缺陷不得在此）
