# @SingleSession 使用策略

> **总原则（2026-07-15 修订）**：`@SingleSession` 仅用于 **non-BizModel** 场景（Processor 编排方法、定时任务 `*Job`、独立 service bean）。BizModel 上的 `@BizMutation`/`@BizQuery` 方法**不需要** `@SingleSession`——GraphQL 引擎的 `SingleSessionFunctionInvoker` 已为每个操作建立 ORM Session（见 `service-layer.md:231` + plan `2026-07-14-2256-1` Re-Verification Record §1）。
>
> **历史背景**：本仓库原在 50 个 BizModel 文件的 175 处 `@BizMutation` 方法上叠加了 `@SingleSession`。经平台源码核实，这些注解在生产路径上完全冗余（AOP `runInSession` 命中 OrmTemplate 的「复用已开 Session」分支，零行为差异）。已于 2026-07-15 全部移除。受影响直调测试（绕过 GraphQL 引擎的测试方法）已通过 `ormTemplate.runInSession(session -> ...)` 包裹修复。

## 适用场景

1. **Processor 编排方法**：`ErpFinPostingProcessor.process`/`reverseProcess` 等 code-level `@SingleSession` 钉在编排方法上，管 Session 刷新作用域（见 `implement-complex-business-flow.md:117`）
2. **定时任务 `*Job`**：`ErpFinAutoReconJob` 等 non-BizModel bean 的入口方法
3. **独立 service bean**：非 BizModel 的 service 方法需要跨多次实体操作时

## 不适用场景

1. **BizModel `@BizMutation`/`@BizQuery` 方法**：GraphQL 引擎已提供 ORM Session，`@SingleSession` 冗余
2. **纯查询**：查询不走 Session 级联懒加载的，不需要
3. **单实体简单 CRUD**：CrudBizModel 默认 `get`/`save`/`update`/`delete` 自动管理 Session，无需额外标记

## 审查规则

新增 `@BizMutation` 方法时：
- **不要**添加 `@SingleSession`——GraphQL 引擎的 `SingleSessionFunctionInvoker` 已为每个 mutation 建立 ORM Session
- 如果测试直调 BizModel 方法报 `no-current-session`，**不要**给 BizModel 加 `@SingleSession`（见 `testing.md:49`）；改用 `IGraphQLEngine` 或在测试内 `ormTemplate.runInSession(...)` 包裹
- 只有 **non-BizModel** bean（Processor/Job/独立 service）的编排方法才考虑 `@SingleSession`
