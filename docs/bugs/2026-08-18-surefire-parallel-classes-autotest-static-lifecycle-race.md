# 2026-08-18 surefire parallel=classes 与 nop-autotest 全局静态生命周期竞争（unknown-operation 类级失效）

## 现象

- `module-cs/erp-cs-service` 全量 `mvn test`（20 个容器测试类，144 tests）时 `TestErpCsTicketSlaCsat` **全部 13 测试**以 `nop.err.graphql.unknown-operation`（`ErpCsTicket__assign`/`ErpCsTicket__scanOverdueTickets`/`ErpCsSurvey__createSurvey` 等**该类用到的所有自定义 GraphQL 操作**）失败，类耗时异常短（≈6s，每方法 ≈0.5s 快速失败）。
- **调度时序相关**：单类运行、2 类（ServiceCatalog+SlaCsat / DeltaOverride+SlaCsat）、9 类、18 类（无 `TestErpCsTicketStateMachineDeltaOverride`）子集全绿；19 类及以上（DeltaOverride 在场）**确定性复现**（forkCount=4 与 forkCount=1 均复现——CLI `-DforkCount`/`-Dparallel` 无法覆盖父 POM 硬编码 `<configuration>`）。
- 同根因形态先例：2026-08-18 R1.66 全仓首跑 `TestErpMdSkuServices` 9 errors `unknown-operation`（module-master-data，日志 `docs/logs/2026/08-18.md`「首跑偶发注记」，当时存疑链，经本次 cs 确定性复现实证确认）。

## 根因

- nop-entropy 父 POM（`pom.xml` surefire `<configuration>`）：`forkCount=4 + reuseForks=true + parallel=classes + threadCount=1` —— 多个测试类**共享 JVM 并行交错执行**。
- nop-autotest 生命周期为**全局静态**：`NopJunitExtension.beforeAll → CoreInitialization.initialize()` / `afterAll → CoreInitialization.destroy()` + `AutoTestCase.initBeans() → BeanContainer.instance().restart()`（每方法）。类 A 的 afterAll 销毁与类 B 的初始化/执行交错时，B 绑定到部分初始化/已销毁重建的容器——biz bean 已注册但 `BizObjectManager` 的 GraphQL schema 未含对应操作 → 全类 unknown-operation。
- 证据：失败类所在 JVM 内其他类全绿；`TestErpCsTicketStateMachineDeltaOverride`（带 `_delta` VFS 层 + `@NopTestProperty`）改变了类完成时序使坏交错稳定命中（其自身不调 GraphQL 故不受影响）；无该类时调度避开了坏交错。

## 修复（本仓 2026-08-18 已落地：module-cs/erp-cs-service）

- `module-cs/erp-cs-service/pom.xml` 覆写 surefire：`forkCount=4 + reuseForks=false + parallel=classes + threadCount=1`（保留 jacoco argLine）——每测试类独立 JVM，消除共享静态竞争；全量 144/0/0 全绿。
- 不修改 nop-entropy 父 POM（外部仓库代码，保护区域 `auto + dual-agent-approval`；且其他模块未再现不强制统一变更）。

## successor（触发条件）

- 其他模块（首证 module-master-data `TestErpMdSkuServices`）再现同类 unknown-operation 类级失效时，按同款 per-module `reuseForks=false` 覆写处理；若多模块频发，考虑跨仓库计划升级父 POM 默认（须双独立子 agent 批准）。

## 关联

- plan `docs/plans/2026-08-17-2125-3-rc-mr1-r1-67-cs-multi-level-escalation.md`（执行期发现修复记录）
- 日志 `docs/logs/2026/08-18.md`（RC-R1.67 条目 + R1.66 首跑偶发注记）
