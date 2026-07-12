# 2026-07-12-1321-1-cs-canned-response-test-fix CS 预设应答业务动作 E2E 测试修复

> Plan Status: draft
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
- **CS BizModel 返回类型已确认（权威源 Java，无 xbiz 覆盖）**：`module-cs/erp-cs-service/.../entity/ErpCsCannedResponseBizModel.java`：`renderTemplate` `@BizQuery` :83-91 返回 `String`；`suggestForTicket` `@BizQuery` :95-132 返回 `List<ErpCsCannedResponse>`；`applyCannedResponse` `@BizMutation` :138-156 返回 `String`。无 `.xbiz.xml` 覆盖（grep 确认），Java 签名即 GraphQL 契约。
- **既有绕过范式已验证（fin-reconciliation）**：`tests/e2e/business-actions/fin-reconciliation.action.spec.ts:232` 注释明示「`checkDualSideConsistency` 返回 DualSideDiffReport（复杂对象，需 selection set，非 callQuery 原语可表达）」，:235-239 以 inline `page.request.post('/graphql', { data: { query: 'query{ ...{ direction partnerId consistent rows{ ... } } }' } })` 带 selection 直调。cs-canned-response spec 未遵循此范式。
- **`callQuery` 调用面仅 1 spec（修复无涟漪）**：grep 确认 `callQuery` 仅被 `cs-canned-response.action.spec.ts` import + 3 处调用（:62/:71/:89）；`fin-reconciliation` 仅在注释（:232）提及。修复该 spec 的 3 处调用不波及其他 spec。

剩余差距：business-actions 套件 48 用例中 47 绿、1 红（cs-canned-response），红色根因为 spec 误用 `callQuery`（缺 selection）+ 误传 selection 给标量 mutation，非后端缺陷。修复需重写 3 处调用为 inline GraphQL（带 selection）/ `verifyState`。

## Goals

- 修复 `cs-canned-response.action.spec.ts` 的 3 处结构性缺陷，使该用例全绿，解除 business-actions 套件唯一红色用例，恢复全绿基线声明能力。
- 验证 CS 预设应答三动作（`renderTemplate` 标量 / `suggestForTicket` 复杂列表 / `applyCannedResponse` 标量 mutation）浏览器层全栈可达 + 业务断言（建议列表非空 + 精确匹配 + 渲染占位符替换 + usageCount 递增 + TicketAction NOTE 审计）。
- 可选：扩展 `callQuery` 原语支持可选 `selection` 参数（镜像 `callMutation` 范式），防止后续 spec 作者重复此类错误。

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

Status: planned
Targets: `tests/e2e/business-actions/cs-canned-response.action.spec.ts`（重写 3 处调用）、`tests/e2e/business-actions/_helper.ts`（可选扩展 `callQuery`）
Skill: `nop-testing`

- Item Types: `Fix | Decision | Proof`
- Prereqs: 无

- [ ] `Decision`：`callQuery` 原语是否扩展可选 `selection` 参数。**候选 A（最小）**：仅重写 cs-canned-response spec 的 3 处调用为 inline GraphQL / `verifyState`，不改 `_helper.ts`——`callQuery` 保持标量专用，复杂返回经 inline query（镜像 fin-reconciliation:235-239）。**候选 B（防未来）**：扩展 `callQuery` 签名为 `(page, entityName, action, args, selection?)`，`selection` 提供时内联 `{ ${selection} }`（镜像 `callMutation` :85-87），默认不提供时保持标量行为向后兼容。**Decision 标准**：候选 A 是最小变更（1 文件），候选 B 增加防未来性（2 文件）。若 `callQuery` 在可见未来无其他 spec 计划使用复杂返回（grep 确认仅 1 spec），选 A；若团队偏好原语对称性，选 B。记录选择与理由。
      - Skill: `nop-testing`
- [ ] `Fix`：重写 `cs-canned-response.action.spec.ts` 缺陷 A（:62 `suggestForTicket`）——替换 `callQuery(...)` 为 inline `page.request.post('/graphql', { data: { query: 'query{ ErpCsCannedResponse__suggestForTicket(ticketId:N){ id title content macroTicketTypeId macroPriority sequence usageCount } }' } })`，镜像 fin-reconciliation:235-239 范式。断言 `data` 为数组 + 非空 + 含精确匹配项。
      - Skill: `nop-testing`
- [ ] `Fix`：重写缺陷 B（:81 `applyCannedResponse`）——`callMutationOk(..., 'id')` 误对标量返回选 `id`。替换为 inline `page.request.post('/graphql', { data: { query: 'mutation{ ErpCsCannedResponse__applyCannedResponse(cannedResponseId:N, ticketId:N) }' } })`（标量 mutation 无 selection）。断言返回字符串 truthy + 渲染内容含占位符替换。
      - Skill: `nop-testing`
- [ ] `Fix`：重写缺陷 C（:89 `__get`）——替换 `callQuery(page, 'ErpCsCannedResponse', '__get', { id })` 为既有 `verifyState(page, 'ErpCsCannedResponse', canned.id, 'usageCount')`（:112，已支持 selection）。断言 `usageCount` 递增。
      - Skill: `nop-testing`
- [ ] `Proof`：指定验证命令 `npx playwright test tests/e2e/business-actions/cs-canned-response.action.spec.ts --workers=1` 全绿（renderTemplate 标量 + suggestForTicket 复杂列表 + applyCannedResponse 标量 mutation + usageCount 递增 + TicketAction NOTE 审计断言全通过）；`npx playwright test tests/e2e/business-actions/ --workers=1` 全套件 48/48 全绿（无新增回归）。
      - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付 cs-canned-response spec 三处结构性缺陷修复 + business-actions 全套件全绿（解除唯一红色用例）。

- [ ] cs-canned-response spec 全绿（renderTemplate / suggestForTicket / applyCannedResponse / usageCount 递增 / TicketAction NOTE 审计断言全通过）。
- [ ] business-actions 全套件 48/48 全绿（无新增回归——`callQuery` 仅本 spec 使用，其他 spec 不受影响）。

## Draft Review Record

- Independent draft review iteration 1: <pending>

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处。

- [ ] 范围内行为完成（cs-canned-response spec 全绿 + business-actions 全套件 48/48 全绿）
- [ ] 相关文档对齐（`e2e-runbook.md` business-actions 套件计数 + 已知失败用例段移除「cs-canned-response 预存失败」免责声明）
- [ ] 已运行验证：`npx playwright test tests/e2e/business-actions/ --workers=1`（全套件 48/48 全绿）+ `npx playwright test tests/e2e/business-actions/cs-canned-response.action.spec.ts --workers=1`（单 spec 全绿）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### CS 预设应答业务路径边界覆盖扩展

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 三级宏匹配边界（精确>类型>全局兜底）/ `renderTemplate` JSON variableDefs 必填校验负路径 / `suggestForTicket` limit 钳制边界经 1234-2 后端单元测试覆盖。本计划仅修复既有 spec 结构缺陷使其断言可达，不扩展业务路径覆盖面。
- Successor Required: `no`

## Closure

Status Note: <pending>

Closure Audit Evidence:

- Auditor / Agent: <pending>

Follow-up:

- 无（CS 预设应答业务路径边界覆盖经 1234-2 后端单测覆盖，非阻塞）
