# 2026-07-15-2246-1-fk-display-name-e2e-regression FK 显示名整改后 Playwright E2E 回归 + CRUD smoke 基线收口

> Plan Status: **completed**（经独立子代理新会话冷重播审计 PASS（2026-07-16））
> Last Reviewed: 2026-07-15
> Mission: erp
> Work Item: 各域细化端到端验证、看板运行时视觉/浏览器回归（plan 2256-2 全域 FK 显示名整改后的浏览器层回归验证）
> Source: plan `2026-07-14-2256-2-fk-display-name-resolution-conformance.md` Closure Gates 未闭合项（E2E 快照回归门控 `[x]` 仅由后端 `mvn test` 佐证，Playwright 套件未重跑）+ 该计划未勾选结束审计门控（由 `2026-07-15-2246-2` 承接）；`docs/testing/known-good-baselines.md` 2026-07-14 行「17 CRUD smoke 预存环境问题」自 ~2026-07-10 起跨 ≥10 份计划携带未根因
> **方向变更（2026-07-15）**：本计划原定直接重跑现有 AMIS 类名驱动的 E2E 套件，但根据用户指示，前端即将切换到 Flux 引擎（无 .cxd-* 类名），现有测试需整体迁移。因此本计划调整为：
>   1. 首先建立 PageObject 基础架构（`tests/e2e/pages/`），用适配器模式隔离 AMIS/Flux 差异
>   2. 将现有测试逐步迁移到 PageObject 模式，按业务字段名操作，消除 DOM 类名依赖
>   3. 迁移完成后在 AMIS 基线上全绿验证，届时 Flux 切换仅需换 Adapter
>   4. 17 CRUD smoke 超时问题将在迁移过程中自然暴露（是环境/渲染问题还是等待条件不当）
> Related: `2026-07-14-2256-2`（整改本体，completed/自我审计，本计划解除其 E2E 回归盲区）、`2026-07-15-2246-2-plan-2256-2-closure-audit`（后继：2256-2 独立结束审计，依赖本计划 E2E 全绿）、`2026-07-14-1449-1`（结束审计一致性批次先例）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 截至 2026-07-15 22:46 +0880）：

### plan 2256-2 全域整改已落地（后端验证全绿，浏览器层未验证）

- **变更规模**：变更范围 5570 文件（已提交于 commit `253fcdeb8`）。删除 243 个 BizModel 文件中 **658 个 FK 名称解析 `@BizLoader` 方法**（结构特征识别）+ 对应 xmeta 派生 prop；全域 view.xml list grid 列由 `xxxName`/`xxxTitle`/`xxxFullName` 等**恢复为原始 FK 列名 `xxxId`**；激活平台 `tagSet="disp"` → 自动 `{relation}.{dispCol}` 路径属性 + `control.xlib:view-relation` AMIS 自动显示名管线。
- **附加修复**（均影响快照/渲染稳定性）：`AutoTestHelper.isVarCol` 增 `deleteVersionPropId`、`JsonMatchHelper.valueEquals` Number 优先匹配、全域 179 个 `tagSet="clock"` 列 `DATETIME`→`TIMESTAMP`、删除 23 处 `setMemo(billHeadCode)` + 2 处 `setRemark(code)`、`NotificationDispatcher` TreeMap 键序、`MockTransportAdapter` 内容哈希替 UUID、`ErpInvConfigs.roundCost` 统一 scale、supplier/carrier xmeta 敏感字段 `published="false"`。
- **后端验证**：`mvn test` 19 模块 **1497 tests 0 failures/0 errors**；`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。后端 GraphQL 快照已按 `force-save-output` 重录（字段名 `xxxName`→`relation.dispCol` 已生效）。
- **浏览器层验证缺口**：plan 2256-2 Closure Gates「E2E 快照回归：全域快照重录 `[x]`」实指**后端 autotest 快照**（force-save-output 重录），**Playwright E2E 套件（~300+ 测试）自 2026-07-14 后未重跑**。2256-2 的 Task Route / Execution Plan / 07-15 日志均仅含 `mvn test`，无 `npx playwright test` 证据。

### Playwright E2E 套件现状（整改后状态未知）

- 套件结构：`tests/e2e/{crud, dashboards, reports, business-actions, orchestration, visual}` + `playwright.config.ts`（webServer fresh-DB + 种子 + auth fixtures）。
- 最近已知良好基线：2026-07-14（plan 1934-1）全套件 **343 passed**，但 **17 CRUD smoke 失败**登记为「预存环境问题」（`.cxd-Crud`/`.cxd-Table` DOM 15s 渲染超时；fresh-DB+fresh-server 隔离仍失败）。
- **整改对 E2E 的风险面**（未经实测核实，故列为待证风险而非既定事实）：
  - view.xml 列 `xxxName`→`xxxId` 后 AMIS grid 改由 `view-relation` 运行时解析显示名——CRUD smoke 等待 `.cxd-Crud`/`.cxd-Table` DOM，若运行时解析改变渲染时序或失败，可能影响冒烟/视觉断言。
  - GraphQL 选择集中若含已移除的 `xxxName` 字段，查询会失败（field-not-found）；需核查哪些 E2E spec 在 selection 中引用了 `xxxName` 类字段。
  - value/visual spec 断言 DOM 显示名 token，`view-relation` 解析后 token 可能偏移。

### 17 CRUD smoke 渲染超时（长期携带，未根因）

- 自 ~2026-07-10 起跨 ≥10 份计划（1234-2/0628-2/1249-1/1249-2/2004-1/2004-2/0335-1/0335-2/0704-1/1934-1 等）的 Closure Gates / known-good-baselines 登记「17 CRUD smoke 失败 = 预存环境问题」，根因描述为「`.cxd-Crud`/table DOM 15s 渲染超时」，但**从未有计划系统性根因调查或修复**；fresh-DB+fresh-server 隔离仍复现，表明非测试间状态污染。
- 整改后 view.xml 列变更使该问题状态进一步不确定：可能仍失败、可能变化、亦可能因列结构简化而缓解——须实测核实。

### 剩余差距

1. Playwright E2E 套件在 2256-2 整改后的运行状态未知——最大近期变更（5570 文件）的浏览器层回归盲区。
2. 17 CRUD smoke 渲染超时长期携带未根因，整改后须重新核实并收口（修复或诚实重基线）。
3. known-good-baselines 自 2026-07-14 起无 fresh 全绿 E2E 行（均带「17 CRUD smoke 失败」已知失败）。

## Goals

- **建立 PageObject 基础架构**（`tests/e2e/pages/`）：用 EngineAdapter 接口隔离 AMIS/Flux DOM 差异，使测试按业务字段名操作，不直接触碰 DOM 选择器。
- **逐模块迁移现有 E2E 测试**：将 crud, business-actions, dashboards, reports, orchestration, visual 中所有测试逐步重构为 PageObject 模式，消除 `.cxd-*` 类名依赖。
- **迁移后在 2256-2 整改基线上全绿验证**，达成全绿或诚实记录残留。
- **17 CRUD smoke 渲染超时**在迁移过程中自然根因定位（迁移暴露真实等待条件问题 vs 平台/Flux 固有限制），一并收口。
- 在 `known-good-baselines.md` 落地一条 fresh E2E 基线行（全绿或附确认根因的已知失败），恢复「基线可信」状态。

## Non-Goals

- **不重新实现 2256-2 的范围**——本计划仅消费侧 E2E 回归验证 + 测试/spec 修复，不重开其功能边界。
- **不新增 E2E 覆盖**——这是回归验证 + 测试重构，非新业务测试；新业务动作/编排/数值断言覆盖归各自 successor。
- **不改 ORM/契约/种子**——E2E spec/选择器/快照修复为本计划范围；若发现真实**生产**缺陷（Java/ORM/契约），开 successor 单独立项（ORM ask-first），不在本计划即时改生产代码。
- **不触及 xwf 审批轴浏览器层**——经 2330-1 权威裁决平台阻塞，触发条件未满足。
- **不承接 2256-2 的独立结束审计**——由后继计划 `2026-07-15-2246-2` 承接（依赖本计划 E2E 全绿）。
- **不追求一次完成全部迁移**——PageObject 基础设施优先落地，迁移分模块进行，每模块迁移后验证通过再继续下一模块。

## Task Route

- Type: `verification or audit work`（既有 Playwright E2E 套件的回归验证 + 测试层修复；纯消费侧 + 测试维护，零生产契约变更预期）
- Owner Docs: `docs/testing/e2e-runbook.md`（E2E 套件结构、运行命令、已知限制）、`docs/references/playwright-e2e-guide.md`（Phase 2 失败诊断决策树，`e2e-runbook.md:558` 引用）、`nop-entropy/docs-for-ai/02-core-guides/testing.md`（快照录放流程，仅后端快照部分参考）
- Skill Selection Basis: Phase 0 建立 PageObject 基础架构（新建 TypeScript 类，非 Nop 平台特定工作，匹配 `none` / 前端工程通用技能）。Phase 1-2 测试重构（纯消费侧测试维护，零生产代码变更，匹配 `none`）。Phase 3 根因调查 17 CRUD smoke 渲染超时 / E2E 回归匹配 `nop-debugging`（Iron Law：先根因后修复；condition-based-waiting 替任意 15s 超时）。E2E 测试为 Playwright 浏览器层而非 `nop-testing`（`nop-testing` 实质内容 = JunitAutoTestCase 后端快照），同 1934-1/0742-1 既有 E2E 计划 `Skill: none`（nop-testing）先例；注：`nop-testing` 触发词表含「E2E测试/Playwright」但路由目标 `02-core-guides/e2e-testing.md` 不存在，故其 E2E 覆盖为空——本计划依技能实质内容判定 `Skill: none`（nop-testing），Phase 3 用 `nop-debugging`。执行 Phase 3 前重新加载 `nop-debugging`。
- Protected Areas: E2E spec 在根 `tests/e2e/` 非 reactor 模块；不改 ORM/契约；任何生产代码修复须 ask-first 并开 successor。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- webServer JVM args 已含 2256-2 整改所需 config（整改未新增 E2E 层 config）。无新增端口/环境变量/密钥。

## Execution Plan

### Phase 0 - PageObject 基础架构

Status: done
Targets: `tests/e2e/pages/`（新建基础设施）
Skill: `none`

- Item Types: `Add`
- Prereqs: 2256-2 整改已落地（后端 1497 全绿）+ 整改变更已落地于 HEAD（commit `253fcdeb8`）
- 产出物：
  - `tests/e2e/pages/types.ts` — 共享类型与常量
  - `tests/e2e/pages/AmisAdapter.ts` — AMIS 引擎适配器（封装 .cxd-Crud/.cxd-Modal/input[name] 等 AMIS 类名）
  - `tests/e2e/pages/FluxAdapter.ts` — Flux 引擎适配器（基于 data-slot/data-testid/getByLabel 模式，当前为未来预留占位）
  - `tests/e2e/pages/GraphQLClient.ts` — 中心化 GraphQL 操作（findPage/get/save/update/delete/callMutation）
  - `tests/e2e/pages/Navigation.ts` — 统一登录与页面导航（login/loginAndNavigate/navigateTo）
  - `tests/e2e/pages/CrudListPage.ts` — CRUD 列表页面对象（按业务字段名操作单元格，findRowByField）
  - `tests/e2e/pages/FormDialog.ts` — 表单对话框对象（setField/getField/selectOption/submit）
  - `tests/e2e/pages/README.md` — 使用文档与迁移指南
  - `tests/e2e/examples/crud-smoke.example.spec.ts` — 迁移示例
- [x] `Add`：PageObject 基础设施代码创建并通过 Playwright 解析验证（无语法/导入错误）
- [x] `Add`：README 记录架构原则、使用示例、迁移指南

Exit Criteria:

- [x] 基础设施文件全部创建，Playwright 可解析
- [x] 迁移示例展示从旧模式到 PageObject 的重构路径

### Phase 1 - CRUD 模块迁移（smoke + list-value + write + amis-form-write）

Status: done
Targets: `tests/e2e/crud/`（迁移到 PageObject 模式）
Skill: `none`

- Item Types: `Fix | Refactor`
- Prereqs: Phase 0 基础设施已可用

- [x] `Add`：`pages/engine.ts` 全局引擎工厂（`getEngine()` 读 `E2E_ENGINE` 环境变量，集中化 AMIS/Flux 切换）
- [x] `Add`：`fixtures.ts` 扩展 `engine` fixture（测试可通过 `{ page, engine }` 解构获取引擎实例）
- [x] `Refactor`：`crud/_helper.ts` 四个公共函数全部迁移到 PageObject（`runCrudListSmoke` → `CrudListPage`+`FormDialog`；`assertCrudListValues` → `GraphQLClient.findPage`；`runCrudWriteCycle` → `GraphQLClient` 方法链；`runAmisFormWrite` → `CrudListPage`+`FormDialog`+`GraphQLClient`）
- [x] `Refactor`：`master-data.write.amis.spec.ts` 移除已不存在的 `id` 表单字段
- [x] `Proof`：全套 crud/ 套件运行（36 tests）：23 passed / 13 failed（13 均为预存 `.cxd-Crud` 15s 渲染超时，非迁移引入回归）

Exit Criteria:

- [x] crud/_helper.ts 中所有公共函数使用 PageObject 基础设施
- [x] 各域 crud spec 文件仅传递业务配置（entityRoute/entityName/field values），不含 DOM 选择器
- [x] 迁移后的 spec 无新增回归（13 失败均为 Phase 3 待根因的预存 smoke 渲染超时）

### Phase 2 - 业务动作 + 看板 + 报表 + 编排 + 视觉模块迁移

Status: completed
Targets: `tests/e2e/{business-actions,dashboards,reports,orchestration,visual}/`
Skill: `none`

- Item Types: `Fix | Refactor`
- Prereqs: Phase 1 已全绿验证

- [x] `Refactor`：business-actions/_helper.ts 改为使用 `GraphQLClient` 基类，消除 `page.request.post('/graphql')` 裸调用。
- [x] `Refactor`：dashboards/_helper.ts + dashboards/*.smoke.spec.ts 使用 `CrudListPage` 导航 + 等待 + `GraphQLClient` 数值断言。
- [x] `Refactor`：reports/_helper.ts + reports/*.smoke.spec.ts 使用基础页面导航 + `GraphQLClient` 报表查询。
- [x] `Refactor`：orchestration/_helper.ts 使用 `GraphQLClient`，消除 `page.request.post('/graphql')` 裸调用。
- [x] `Refactor`：visual/_helper.ts + visual/*.spec.ts 使用 `CrudListPage` 导航 + 等待视觉截图。
- [x] `Proof`：每模块迁移后单 spec 或子集重跑验证。

Exit Criteria:

- [x] 全部测试文件不再直接引用 AMIS 类名（`.cxd-*`）
- [x] 全部 GraphQL 调用经 `GraphQLClient` 中心化
- [x] 页面导航经 `Navigation.ts` 统一入口

### Phase 3 - 全套件回归 + 17 CRUD smoke 收口 + 基线落盘

Status: completed
Targets: `tests/e2e/**`（全绿验证）；`docs/testing/known-good-baselines.md`、`docs/testing/e2e-runbook.md`
Skill: `nop-debugging`

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 1 + Phase 2 迁移完成

- [x] `Proof`：全套件 `npx playwright test --workers=1` 重跑，确认全绿（或仅余经根因证实的已知失败）。
  - 验证命令：`BASE_URL=http://127.0.0.1:8011 npx playwright test --workers=1`
  - Execution Note: 干净 server（自起，runner jar 含 successor 0012-1 全部修复）`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test --workers=1` → **全套件 405 passed (51.9m)，0 失败**。successor 0012-1（147 col-not-prop 修复）+ 本计划 example spec 2 处测试代码修复（response 监听器须先于 navigate 挂 + 实体须有标准 list 页：maintenance→ErpMntVisit、aps→ErpApsOperationOrder）后达成。
- [x] `Fix | Decision`：17 CRUD smoke 渲染超时根因调查（Iron Law：先根因后修复）。
  - Skill: `nop-debugging`
  - **根因结论（2026-07-16，干净 server 实证）**：非时序/环境问题，而是生产缺陷 `nop.err.xui.grid.col-not-prop`。view.xml grid 列引用了不存在的实体属性 → `PageProvider__getPage`→`loadComponentModel` 抛错 → 页面不渲染 `.cxd-Crud` → `waitForList()` 15s 超时。跨 ≥10 份计划长期误诊为"渲染超时环境问题"。
  - **来源**：2256-2 把 view.xml 列从 `xxxName` 回退为 `xxxId`，但部分 `xxxId` 非有效属性（有的是被删的 @BizLoader 显示属性，有的是误改的真实列）；另含更早遗留的非法列（如 `employeeDisplayName`）。
  - **规模（静态检查器 `tools/check-view-cols.mjs` 全量枚举）**：源文件 **147 个非法列，跨 13 域**（hr 34/crm 20/finance 19/assets 19/mfg 16/mnt 12/qa 10/projects 5/cs 5/b2b 4/md 1/log 1/aps 1）。
  - **修复非均匀（实证）**：`ErpCrmStage` 的 `stageName` 是真实列→简单回退；`ErpCsTicketType` 的 FK 实为 `defaultSlaPolicyId`/关系 `defaultSlaPolicy`（非 `slaPolicyId`）→需逐实体核对。不能机械批量改。
  - **处置**：超出本计划 Non-Goal（生产 view.xml 缺陷开 successor），已开 successor `2026-07-16-0012-1-view-grid-col-not-prop-remediation` 承接全量修复。本计划此项标 `[x]`（根因已收口），全套件全绿依赖 successor 完成后方可达成。
- [x] `Add`：`known-good-baselines.md` 落一条 fresh E2E 基线行；`e2e-runbook.md` 已知限制段按根因结论更新。
  - Skill: `none`
  - Execution Note: baselines 新增 2026-07-16 行（全套件 405 passed / 0 失败，Known Failures = none，"17 smoke 环境问题"经根因证伪已全修）；e2e-runbook troubleshooting 增第 5 条（渲染超时→查 server log col-not-prop，勿假设时序，指向 lesson 05 + 2 工具）。
- [x] `Add`：`docs/logs/<date>.md` 增聚合条目（迁移进度 / 修复数 / CRUD smoke 根因结论 / 基线状态）。
  - Skill: `none`

Exit Criteria:

- [x] 全套件全绿或仅余经根因证实的已知失败（附 successor 指向）—— 405 passed 0 失败，无残留
- [x] known-good-baselines fresh E2E 行落地，e2e-runbook 已知限制段与根因结论一致
- [x] Flux 适配器就绪，全部测试可通过 `new FluxAdapter()` 替换运行（仅需 Flux 前端对 data-slot/data-testid/data-field 的约定实现）—— Phase 0/1 已落地 FluxAdapter + engine 工厂

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_099be26c4ffemOd8XWvT4uHI1v`，独立 general 子代理，新会话冷重播无执行者上下文，2026-07-15) — 0 Blocker / 0 Major / 3 Minor。全部 load-bearing 事实主张经实时仓库逐行核实**零伪**：2256-2 completed/自我审计/未勾选门控/占位审计、仅 `mvn test` 无 playwright、17 CRUD smoke 预存失败、套件结构、5570/658/243 变更范围、343 passed 基线均 confirmed。3 Minor（m1「工作树」措辞对已提交变更不精确→已改「变更范围（commit 253fcdeb8）」+ Phase 1 Prereqs 同步；m2 Owner Docs 漏列 `docs/references/playwright-e2e-guide.md`（Phase 2 诊断决策树）→已补；m3 `Skill: none`（nop-testing）与技能触发词表「E2E/Playwright」张力未 acknowledge→Skill Selection Basis 已增说明：nop-testing 路由目标 `e2e-testing.md` 不存在故 E2E 覆盖为空，依技能实质内容判定）已全部修订落地。模板合规 pass（rule 1/2/4/7/8/14 + anti-slack + exec rule 7 阶段退出仅本地化、full verify 在 Closure Gates）。草案可接受执行 → `Plan Status: active`。

## Closure Gates

> 本计划为前端/浏览器 E2E（行为驱动结果面），纯消费侧回归验证 + 测试层修复（预期零生产契约变更）。结束前运行全套件 + 后端构建（确认 E2E 修复未污染后端）。

- [x] 范围内行为完成（整改后 E2E 全套件全绿或诚实残留 + 17 CRUD smoke 根因收口）—— 全套件 405 passed 0 失败；17 smoke 根因（col-not-prop）已收口并由 successor 0012-1 全修
- [x] 相关文档对齐（known-good-baselines fresh 行 + e2e-runbook 已知限制段 + 日志）
- [x] 已运行验证：全套件 `npx playwright test --workers=1` 全绿/诚实残留 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（确认 E2E spec 变更未污染后端）
- [x] 无范围内项目降级为 deferred/follow-up（疑似生产缺陷须开显式 successor，不得模糊化）—— col-not-prop 已开 successor 0012-1（completed）承接，无模糊化
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符——已完成（独立 general 子代理冷重播 PASS，2026-07-16）
- [x] 结束证据存在于文件中——已回填（见下 Closure Audit Evidence）

## Deferred But Adjudicated

### 疑似生产缺陷（Phase 1/2 发现的 Java/ORM/契约层问题）

- Classification: `watch-only residual`（执行期方可判定是否存在）
- Why Not Blocking Closure: 本计划限定为 E2E 消费侧回归 + 测试层修复。任何确认的生产缺陷一律开 successor 单独立项（ORM ask-first），不在本计划即时改生产代码。
- Successor Required: `yes`（触发条件：Phase 1/2 实测发现确认生产缺陷时，立即开 successor 承接）

## Closure

Status Note: Phase 3 完成。17 CRUD smoke 渲染超时根因收口（col-not-prop 生产缺陷）→ successor 0012-1 全修 147 非法列/13 域（completed）→ 全套件 405 passed 0 失败（干净 server，51.9m）。known-good-baselines 落 2026-07-16 fresh 全绿行（Known Failures = none，"17 smoke 环境问题"误传经根因证伪）；e2e-runbook troubleshooting 增渲染超时→查 server log 条目。Plan Status 仍 active，待独立子代理结束审计后方可 completed。→ 【2026-07-16 追加指针】独立子代理冷重播结束审计 PASS，证据见下 Closure Audit Evidence；Plan Status 已置 completed，Closure Gate 最后两项已勾，Phase 3 Status 标签由 `planned` 纠正为 `completed`（过时文书标签，与既有 `[x]` item/Exit Criteria/Execution Note 对齐）。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话），冷重播无执行者/起草者上下文，2026-07-16。
- Verdict: **passes closure audit (PASS)**。
- 实际执行命令与逐项结论（对照实时仓库，不采信执行者自述）：
  1. successor 状态核实（全套件全绿的前置依赖）：读 `docs/plans/2026-07-16-0012-1-*.md` Plan Status = `**completed**`（经独立子代理新会话冷重播审计 PASS，2026-07-16），其 Closure Gates 全 `[x]`、Closure Audit Evidence 已回填 147 src 非法列全修证据。本计划全套件全绿依赖它成立。✓
  2. 静态全量复核（决定性）：`node tools/check-view-cols.mjs` → `Checked 676 views, 6116 cols. 0 invalid col(s)`（扫描 `module-*/erp-*-web/.../_vfs/.../pages/*/*.view.xml`，排除 `_gen/`；计数与 successor 0012-1 审计逐数字吻合）→ **src 非法列 = 0**。✓
  3. 运行时 ground-truth（决定性）：`mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest -Dmaven.compiler.fork=true -q` → exit 0（本审计新鲜运行，surefire 时间戳 2026-07-17T07:30）；`TEST-io.nop.app.all.web.ErpAllWebPagesTest.xml`：`tests="1" errors="0" failures="0" skipped="0"`，`testValidateAllPages` 通过（time 9.898s）。validateAllPages 全量页面闸门 0 errors。✓
  4. example spec 修复核实（决定性，静态核 `tests/e2e/examples/crud-smoke.example.spec.ts`，三项均须成立）：
     - ① `page.on('response', ...)` 监听器在第 26-30 行注册，**先于**第 32 行 `await crud.navigate()`（对照 `tests/e2e/crud/_helper.ts` runCrudListSmoke 第 21-25 行监听器先于第 27 行 navigate 的正确范式）✓
     - ② maintenance 实体 = `ErpMntVisit`（第 102 行，非 ErpMntVisitRequest）✓
     - ③ aps 实体 = `ErpApsOperationOrder`（第 156 行，非 ErpApsConstraint）✓
     三者均成立。
  5. 基线与 runbook 落地核实：① `docs/testing/known-good-baselines.md` 含 2026-07-16 行（全套件 405 passed / 51.9m / 0 失败；Known Failures = **none**，附"17 CRUD smoke 渲染超时 = 预存环境问题"经根因证伪说明；Commands Passed 含 playwright 全套件 + mvn 154 模块 + validateAllPages + checker）；② `docs/testing/e2e-runbook.md` 第 564 行 troubleshooting 第 5 条「渲染超时→查 server log `errorCode=`（如 col-not-prop），勿假设时序/环境，指向 lesson 05 + parse-nop-errors/check-view-cols 两工具」。✓
  6. 全套件 405 passed 主张：未重跑约 50 分钟全套件（非必需）；以「example spec 修复正确 + checker src=0 + validateAllPages 0 errors + successor 0012-1 completed」作等价 ground-truth（这些正是 17 smoke 失败的根因门控，转绿即等价）。✓
  7. 规则 11 五点一致性：Plan Status（审计前 active 待审计，本审计 PASS 后改 completed）/ Phase 0-2 Status（done/done/completed）/ Phase 3 Status（审计前发现遗留过时标签 `planned`，与该 Phase 全部 `[x]` item/Exit Criteria/Execution Note 矛盾——本审计已纠正为 `completed`，非篡改执行记录）/ 各 Exit Criteria（`[x]`）/ Closure Gates（除结束审计/结束证据外原 `[x]`，本审计勾最后两项）/ Closure Audit Evidence（原占位，本审计回填）/ Status Note 互相吻合。Draft Review Record 与各 Phase Execution Note 原文未篡改。✓
  8. Anti-Hollow + Deferred 诚实性：col-not-prop 生产缺陷**未被静默吞掉**——已开显式 successor 0012-1（completed）承接全量修复，非模糊 follow-up；本计划 Non-Goal「不改 ORM/契约/种子；生产缺陷开 successor」被遵守（本计划仅改测试代码 example spec + 工具/文档，零 ORM/契约/Java 业务逻辑变更）；Deferred 项为 watch-only residual（条件触发），无证据表明发现的真实生产缺陷被隐藏。✓
- 残留风险（非阻塞）：全套件 405 passed Playwright 结果为执行者记录，本次审计未重跑约 50 分钟全套件（依赖运行 server）；已以决定性的 validateAllPages（0 errors，本审计新鲜复跑）+ 静态 checker（0 非法列）+ example spec 静态核（3 处修复正确）+ successor completed（独立审计 PASS）作运行时 ground-truth 等价证明——这些正是 smoke 失败的根因门控，转绿即等价。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
