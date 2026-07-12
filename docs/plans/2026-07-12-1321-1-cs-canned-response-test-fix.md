# 2026-07-12-1321-1-cs-canned-response-test-fix CS 预设应答业务动作 E2E 测试修复

> Plan Status: completed
> Last Reviewed: 2026-07-12
> Mission: erp
> Work Item: business-actions E2E 套件唯一红色用例 `cs-canned-response.action.spec.ts` 修复（`callQuery` 原语缺 selection set + `callMutationOk` 误传 selection 给标量返回）
> Source: 已确认实时缺陷（Fix，规则 13 不可降级）。`cs-canned-response.action.spec.ts` 是 business-actions 全套件（48 用例）唯一失败用例，经 `2026-07-12-0204-2`（:95/:113/:147）+ `2026-07-12-0413-2`（:112/:141/:147）两次独立结束审计共 5 处引用登记为「预存 schema-level 失败，与本计划无关」。`2026-07-11-1234-2-cs-canned-response-agent-performance.md` 使 CS 预设应答后端生产完成（renderTemplate/suggestForTicket/applyCannedResponse 三 `@BizQuery`/`@BizMutation` 已落地并经 20 单元/集成测试覆盖），该红色用例从「待实现占位」变为「真实回归信号」。AGENTS.md §验证基线要求「当验证完全通过（全绿）时记录验证状态」，红色用例阻塞全绿基线声明。
> Related: `2026-07-11-1234-2`（CS 预设应答后端落地源，使本缺陷暴露为回归信号）、`2026-07-12-0204-2`（finance 核销 E2E，首次以 inline GraphQL 绕过 `callQuery` 限制并注释根因 `fin-reconciliation.action.spec.ts:232`）、`2026-07-09-0814-2`（business-actions 三原语 helper 范式源）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **`callQuery` 原语不支持 selection set（helper 级根因）**：`tests/e2e/business-actions/_helper.ts:132` 定义 `callQuery(page, entityName, action, args)`，其 GraphQL 模板（:151-153）为 `query(${varDecls}){ ${entityName}__${action}(${parts}) }`——调用后**无 `{ selection }`**。对比同文件 `callMutation`（:65，模板 :85-87 `mutation(${varDecls}){ ${entityName}__${action}(${parts}){ ${selection} } }`）有 selection。`callQuery` 仅适用于标量返回（如 `String`），对复杂对象/列表返回（需 selection set）经 GraphQL 校验直接拒绝（"Field must have a selection of subfields"）。`verifyState`（:112，模板 :119 含 `{ ${selection} }`）支持 selection。
- **cs-canned-response spec 三处缺陷（spec 级症状）**：`tests/e2e/business-actions/cs-canned-response.action.spec.ts` 单 `test(...)` 块（:16）含 4 处 GraphQL 调用，3 处结构性错误：
  - **缺陷 A（首发，:62）**：`callQuery(page, 'ErpCsCannedResponse', 'suggestForTicket', { ticketId })`——`suggestForTicket` 返回 `List<ErpCsCannedResponse>`（复杂列表，需 selection），`callQuery` 不产 selection → GraphQL 拒绝 → `expect(suggestResp.errors).toBeNull()`（:63）失败 → 测试中止。
  - **缺陷 B（:81，A 修复后暴露）**：`callMutationOk(page, 'ErpCsCannedResponse', 'applyCannedResponse', {...}, 'id')`——`applyCannedResponse` 返回 `String`（标量），`callMutationOk` 产 `mutation{ ...applyCannedResponse(...){ id } }` 对标量选 `id` → GraphQL 拒绝。
  - **缺陷 C（:89，A/B 修复后暴露）**：`callQuery(page, 'ErpCsCannedResponse', '__get', { id })`——`__get` 返回实体（复杂），`callQuery` 不产 selection → GraphQL 拒绝 → `after.data?.usageCount`（:91）不可达。
  - `renderTemplate`（:71，`callQuery`）返回 `String`，是 `callQuery` 唯一正确用法。
- **CS BizModel 返回类型已确认（权威源 Java，无 xbiz 覆盖）**：`module-cs/erp-cs-service/.../entity/ErpCsCannedResponseBizModel.java`：`renderTemplate` `@BizQuery` :82-91 返回 `String`；`suggestForTicket` `@BizQuery` :94-132 返回 `List<ErpCsCannedResponse>`；`applyCannedResponse` `@BizMutation` :137-156 返回 `String`。无 `.xbiz.xml` 覆盖（grep 确认），Java 签名即 GraphQL 契约。
- **既有绕过范式已验证（fin-reconciliation）**：`tests/e2e/business-actions/fin-reconciliation.action.spec.ts:232` 注释明示「`checkDualSideConsistency` 返回 DualSideDiffReport（复杂对象，需 selection set，非 callQuery 原语可表达）」，:235-239 以 inline `page.request.post('/graphql', { data: { query: 'query{ ...{ direction partnerId consistent rows{ ... } } }' } })` 带 selection 直调。cs-canned-response spec 未遵循此范式。
- **`callQuery` 调用面仅 1 spec（修复无涟漪）**：grep 确认 `callQuery` 仅被 `cs-canned-response.action.spec.ts` import + 3 处调用（:62/:71/:89）；`fin-reconciliation` 仅在注释（:232）提及。修复该 spec 的 3 处调用不波及其他 spec。

剩余差距：business-actions 套件 48 用例中 47 绿、1 红（cs-canned-response），红色根因为 spec 误用 `callQuery`（缺 selection）+ 误传 selection 给标量 mutation，非后端缺陷。修复需重写 3 处调用为 inline GraphQL（带 selection）/ `verifyState`。

## Goals

- 修复 `cs-canned-response.action.spec.ts` 的 3 处结构性缺陷，使该用例全绿，解除 business-actions 套件唯一红色用例，恢复全绿基线声明能力。
- 验证 CS 预设应答三动作（`renderTemplate` 标量 / `suggestForTicket` 复杂列表 / `applyCannedResponse` 标量 mutation）浏览器层全栈可达 + 业务断言（建议列表非空 + 精确匹配 + 渲染占位符替换 + usageCount 递增 + TicketAction NOTE 审计）。
- `callQuery` 原语是否扩展 `selection` 参数经 Phase 1 Decision 裁决——候选 A（仅重写 spec，不改 helper）或候选 B（扩展 helper 防未来），选中 B 时作为 Execution Plan in-scope 项执行。

## Non-Goals

- **不**改后端 BizModel / Processor / ORM / xbiz——后端经 1234-2 已落地并经 20 单元/集成测试覆盖，本计划纯测试层修复（零生产代码/契约/模型变更）。
- **不**改其他 business-actions spec——`callQuery` 仅 cs-canned-response 使用，其他 spec 的 `callMutation`/`verifyState`/`createViaSave` 用法正确（grep 确认）。
- **不**新增 CS 预设应答业务路径覆盖（如 `suggestForTicket` 三级宏匹配边界 / `renderTemplate` JSON variableDefs 校验负路径）——1234-2 后端单测已覆盖，本计划仅修复既有 spec 的结构缺陷使其可达断言。

## Task Route

- Type: `bug investigation`（已确认测试层缺陷修复，纯 spec/helper TypeScript 变更，零生产代码/契约/模型变更）
- Owner Docs: `docs/testing/e2e-runbook.md`（business-actions 套件运行手册 + 三原语 helper 范式 + 套件计数）
- Skill Selection Basis: `nop-testing`（Playwright 浏览器层 E2E、GraphQL selection set 语义、三原语 helper 范式、inline query 构造）。无后端/前端开发技能匹配（零生产代码）。既有 fin-reconciliation inline query 范式已验证可复用。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 依赖既有 webServer 启动参数（e2e-runbook.md 方式 A），无新增 JVM 属性
- 依赖 `app-erp-all/target/quarkus-app/quarkus-run.jar` 预构建（既有前置）
- 无 ORM/契约变更，故无 ask-first 保护区域门控；纯测试层改动

## Execution Plan

### Phase 1 - cs-canned-response spec 三处结构性缺陷修复

Status: completed
Targets: `tests/e2e/business-actions/cs-canned-response.action.spec.ts`（重写 3 处调用）、`tests/e2e/business-actions/_helper.ts`（可选扩展 `callQuery`）
Skill: `nop-testing`

- Item Types: `Fix | Decision | Proof`
- Prereqs: 无

- [x] `Decision`：`callQuery` 原语是否扩展可选 `selection` 参数。**选定候选 A（仅重写 spec，不改 `_helper.ts`）**。理由：(1) grep 确认 `callQuery` 仅被 cs-canned-response spec 使用，且其中 `renderTemplate`（:77，返回 String 标量）是其唯一正确用法，保留即可；(2) fin-reconciliation.action.spec.ts:235-239 已验证「复杂返回经 inline `page.request.post('/graphql')` 带 selection 直调」范式，cs-canned-response 直接复用，无新原语需求；(3) 候选 B 扩展 helper 会增加当前无消费者的代码路径，违反最小变更原则。`_helper.ts` 不变。
      - Skill: `nop-testing`
- [x] `Fix`：重写 `cs-canned-response.action.spec.ts` 缺陷 A（:62 `suggestForTicket`）——替换 `callQuery(...)` 为 inline `page.request.post('/graphql', { data: { query: 'query{ ErpCsCannedResponse__suggestForTicket(ticketId:N){ id title content macroTicketTypeId macroPriority sequence usageCount } }' } })`，镜像 fin-reconciliation:235-239 范式。断言 `data` 为数组 + 非空 + 含精确匹配项。（**实现备注**：inline 响应在成功时省略 `errors` 字段（→ undefined），故 errors 断言采用 `toBeFalsy()` 而非 `toBeNull()`，对齐 fin-reconciliation:241 已验证范式——`callQuery` helper 的 `extract` 会把 undefined 归一化为 null，但 inline 直读不经 helper。）
      - Skill: `nop-testing`
- [x] `Fix`：重写缺陷 B（:81 `applyCannedResponse`）——`callMutationOk(..., 'id')` 误对标量返回选 `id`。替换为 inline `page.request.post('/graphql', { data: { query: 'mutation{ ErpCsCannedResponse__applyCannedResponse(cannedResponseId:N, ticketId:N) }' } })`（标量 mutation 无 selection）。断言返回字符串 truthy + 渲染内容含占位符替换（新增 `not.toContain('{customer_name}')` 断言强化 apply 路径覆盖）。errors 断言用 `toBeFalsy()`（同 Fix A 理由）。
      - Skill: `nop-testing`
- [x] `Fix`：重写缺陷 C（:89 `__get`）——替换 `callQuery(page, 'ErpCsCannedResponse', '__get', { id })` 为既有 `verifyState(page, 'ErpCsCannedResponse', canned.id, 'usageCount')`（:112，已支持 selection）。断言 `usageCount` 递增。
      - Skill: `nop-testing`
- [x] `Proof`：指定验证命令 `npx playwright test tests/e2e/business-actions/cs-canned-response.action.spec.ts --workers=1` 全绿 + `npx playwright test tests/e2e/business-actions/ --workers=1` 全套件全绿。**实测结果**：单 spec 1/1 passed（8.1s，BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 复用已启动 Quarkus 实例）；business-actions 全套件 **54/54 passed**（6.4m，无新增回归——`callQuery` 仅本 spec 使用）。注：计划「48 用例」基线为起草期计数，实测 54（套件经 0413-2/0204-2 后续新增 fin-bank-recon/fin-bad-debt 等 spec 增长），「唯一红色用例解除 + 全套件全绿」核心退出标准达成。`mvn install -DskipTests` BUILD SUCCESS（零 Java/ORM 变更，基线确认）。
      - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付 cs-canned-response spec 三处结构性缺陷修复 + business-actions 全套件全绿（解除唯一红色用例）。

- [x] cs-canned-response spec 全绿（renderTemplate / suggestForTicket / applyCannedResponse / usageCount 递增 / TicketAction NOTE 审计断言全通过）。
- [x] business-actions 全套件 54/54 全绿（无新增回归——`callQuery` 仅本 spec 使用，其他 spec 不受影响）。

## Draft Review Record

- Independent draft review iteration 1: `acceptable as-is` (ses_0ab3102ccffeB4YU23yl61LQKo) — 全部 Current Baseline 主张经实时仓库核实为真（`callQuery` :132 无 selection / `callMutation` :65 有 selection / `verifyState` :112 有 selection / cs-canned-response spec 三处缺陷 :62/:81/:89 / BizModel 三返回类型 / fin-reconciliation :232 注释 + :235-239 inline 范式 / `callQuery` 仅 1 spec 使用）。规则 4/5/7/8/10/13 全合规，命名合规，无反松弛禁词，Deferred 项带触发条件。**无 BLOCKER**。采纳 2 non-blocking：S1（Goals 第三条「可选」禁词→改为「经 Phase 1 Decision 裁决」措辞，委托 Decision 项裁定）；S2（BizModel 行号 :83→:82/:95→:94/:138→:137 注解行号对齐）。修订后草案审查已收敛 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [x] 范围内行为完成（cs-canned-response spec 全绿 1/1 + business-actions 全套件 54/54 全绿，无新增回归）
- [x] 相关文档对齐（**核实结果**：grep 确认 `e2e-runbook.md` 不含「cs-canned-response 预存失败」免责声明——该措辞仅存在于兄弟计划结束审计记录 `2026-07-12-0204-2`/`2026-07-12-0413-2` 与 append-only 日志 `07-12.md`，均属正确-as-recorded 的历史记录（AGENTS.md 规则 8 日志仅追加），不得回填改写；e2e-runbook 仅 2 处 cs-canned-response 引用——:222 业务动作表行 + :588 文件树，均准确无误。故「移除免责声明」子项为 no-op（无目标可移除）；「套件计数」子项为 no-op——本计划既不增删 spec，套件计数欠量为 0，e2e-runbook 既有计数漂移（48→54）系 0204-2/0413-2 后续新增 spec 累计所致，非本计划引入，归对应源计划。文档对齐落点：`docs/logs/2026/07-12.md` 增 1321-1 条目 + `docs/testing/known-good-baselines.md` 增全绿基线行记录 cs-canned-response 修复 + business-actions 54/54 + BUILD SUCCESS）
- [x] 已运行验证：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/cs-canned-response.action.spec.ts --workers=1`（1/1 全绿）+ `... tests/e2e/business-actions/ --workers=1`（全套件 54/54 全绿）+ `mvn install -DskipTests`（154 模块 BUILD SUCCESS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 `acceptable as-is`，ses_0ab3102ccffeB4YU23yl61LQKo）
- [x] 文本一致性已验证（Plan Status / Phase Status / Gate / 日志一致，详见结束审计）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### CS 预设应答业务路径边界覆盖扩展

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 三级宏匹配边界（精确>类型>全局兜底）/ `renderTemplate` JSON variableDefs 必填校验负路径 / `suggestForTicket` limit 钳制边界经 1234-2 后端单元测试覆盖。本计划仅修复既有 spec 结构缺陷使其断言可达，不扩展业务路径覆盖面。
- Successor Required: `no`

## Closure

Status Note: 计划完成。business-actions E2E 套件唯一红色用例 `cs-canned-response.action.spec.ts` 三处结构性 GraphQL 误用修复（suggestForTicket inline query 带 selection / applyCannedResponse inline scalar mutation / __get → verifyState），`_helper.ts` 不变（Candidate A）。cs-canned-response spec 1/1 全绿 + business-actions 全套件 54/54 全绿（无新增回归）+ `mvn install -DskipTests` BUILD SUCCESS。零生产代码/契约/ORM 模型变更（纯 1 个 TypeScript 测试文件内部 GraphQL 调用重写）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0a9211864ffetedpswy04FPCRp（general，新会话，无执行者上下文），VERDICT: PASS。
- Evidence: 逐项核实——(1) spec 修复正确（cs-canned-response.action.spec.ts:63-67 defect A inline 带 selection + :87-91 defect B inline scalar 无 selection + :99 defect C verifyState + :77 renderTemplate callQuery 保留 + :1 导入移 callMutationOk/增 verifyState/留 callQuery + :69/:93 errors 用 toBeFalsy() + 业务断言完整 :71-74/:82/:99-101/:104-113）；(2) `_helper.ts` 未改（git diff/status 空，callQuery :132 仍标量专用，Candidate A 确认）；(3) 零生产代码变更（git status 仅 4 文件：1 spec + 3 文档，无 .java/.orm.xml/.api.xml/.xbiz.xml/.view.xml/.page.yaml/CSV）；(4) 后端返回类型匹配（ErpCsCannedResponseBizModel.java:83 String / :95 List / :138 String，无 xbiz 覆盖）；(5) 验证证据一致（07-12.md:3-10 + known-good-baselines.md:13 记录 spec 1/1 + business-actions 54/54 + BUILD SUCCESS，与 Phase 1 Proof 一致）；(6) 计划内部一致性 OK（Phase 1 Status completed + 全 [x] + 8/8 Closure Gates [x] + Plan Status completed）；(7) 无范围蔓延（Deferred 项 out-of-scope 经 1234-2 后端单测覆盖，Non-Goals 显式排除）。无 BLOCKING 项。Non-blocking：计划「48 用例」起草期计数 vs 实测 54，Proof 与日志已透明 reconcile（兄弟 plan 后续新增 spec 增长，非不一致）。

Follow-up:

- 无（CS 预设应答业务路径边界覆盖经 1234-2 后端单测覆盖，非阻塞）
