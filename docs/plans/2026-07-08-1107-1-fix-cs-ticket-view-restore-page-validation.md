# 2026-07-08-1107-1-fix-cs-ticket-view-restore-page-validation 修复 ErpCsTicket.view.xml 缺陷并恢复启动期页面模型校验

> Plan Status: completed
> Mission: erp
> Work Item: 修复 ErpCsTicket.view.xml 缺陷（0056-2 回归）并恢复 `nop.web.validate-page-model` 启动校验
> Last Reviewed: 2026-07-08
> Source: 计划 `2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md` 执行期间发现的预存缺陷——`ErpCsTicket.view.xml` layout 语法 bug 导致 `validate-page-model` 启动校验失败，当时以 JVM 参数 `-Dnop.web.validate-page-model=false` **全局绕过**（记录于 `docs/testing/e2e-runbook.md:38,49,96` + `docs/testing/known-good-baselines.md:15`，**未记入 `docs/bugs/`、未修复根因**）；缺陷由计划 `2026-07-08-0056-2-cs-knowledge-base-search-suggestion` 引入（其 `__kbSuggestion` custom cell + `<layout>` 行 + `/api/GenericApi` ajax），0056-2 验证仅为 Java 测试 + `mvn install`，未触达 web 启动校验层故未暴露
> Related: `docs/plans/2026-07-08-0637-1-playwright-e2e-dashboard-report-smoke.md`（completed，发现并绕过本缺陷）、`docs/plans/2026-07-08-0056-2-cs-knowledge-base-search-suggestion.md`（completed，引入本回归）、`docs/plans/2026-07-06-0504-2-report-rendering-subsystem.md`（0637-1 确立的 `/api/GenericApi`→`/graphql` + Map 字段选择移除范式）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`，非采信旧记忆）：

- **配置层**：全部 20 个 `application.yaml`（含 `app-erp-all/src/main/resources/application.yaml:21`）均声明 `nop.web.validate-page-model: true`（默认开）。即：**正常启动会执行页面模型校验**。
- **运行时绕过**：`docs/testing/e2e-runbook.md:38,49,96` 与 `docs/testing/known-good-baselines.md:15` 记录 e2e 启动 quarkus-run.jar 必须附加 `-Dnop.web.validate-page-model=false`，否则「预存 ErpCsTicket.view.xml layout 校验 bug」导致启动失败。该 JVM 覆盖**全局关闭**整个应用的启动期页面校验——这是一处安全网退化（此后任何页面模型缺陷都不会在启动期被捕获）。
- **缺陷文件**：`module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml`：
  - `<form id="add">`/`<form id="edit">` 的 `<layout>` 末尾含 `=__kbSuggestion[相关知识库文章]` 行（0056-2 新增 custom cell），为启动校验失败的可疑根因——该 `=` 前缀行仅出现在 ErpCsTicket.view.xml（即本缺陷本身，行 26/91），其他自定义 view 零命中（`rg '^\s*=\w' --glob '**/_vfs/**/*.view.xml' --glob '!**/_gen/**' --glob '!**/ErpCsTicket.view.xml'` 空）；0637-1 Phase 1 已初步诊断该行为「缺少 group 闭合 `=}`」（`0637-1:75`），精确语法错误待本计划 Phase 1 复现确认）。
  - 同文件 4 处 `<api ... url="/api/GenericApi" dataType="raw">`（行 34/55/99/120，KB suggestion + adoptKnowledge 两动作 × add/edit 两表单）——与 0637-1 在 34 个 page.yaml 中修复的同类缺陷一致：`/api/GenericApi` 端点不存在（正确为 `/graphql`），且对 GraphQL Map 返回类型使用了字段选择（Nop 引擎报错）。0637-1 **仅修 page.yaml**，未覆盖 view.xml，故此处仍残留。
- **来源归属**：上述 layout 行 + 4 处 ajax 均为 `2026-07-08-0056-2` 新增（`__kbSuggestion` custom cell + KB suggestion/adopt 服务）。0056-2 验证手段为 `mvn test`（Java）+ `mvn install`，二者均不启动 web 页面校验层，故回归未被捕获。
- **同类 gen-control 范围核查**：自定义 view 中含 `gen-control` 的仅 2 文件——`ErpCsTicket.view.xml`（本计划）与 `ErpHrEmployee.view.xml`（0517-2 调动 drawer）。0637-1 探索仅报告 ErpCsTicket 触发校验失败；ErpHrEmployee 是否在 `validate-page-model=true` 下通过待 Phase 1 复核。
- **`/api/GenericApi` 残留面**：`rg '/api/GenericApi' --glob '**/_vfs/**/*.view.xml' --glob '!**/_gen/**'` 仅命中 ErpCsTicket.view.xml（4 处）——范围已收敛至单文件。
- **非保护区域**：view.xml 属前端定制面（非 `model/*.orm.xml` ask-first），AI 自主权为 `implement`/`plan-first`，无需 ORM 人工批准。
- **范式指针**：0637-1 已确立 `/api/GenericApi`→`/graphql` + 移除 Map 字段选择 + `adaptor` 转换的正确写法（见 finance/sal/pur/inv 等 10 看板 page.yaml）。

剩余差距：(1) ErpCsTicket.view.xml layout 语法使 `validate-page-model=true` 启动失败；(2) 同文件 4 处 `/api/GenericApi` ajax 使 KB suggestion/adopt 功能运行时不可用；(3) 全局 JVM 覆盖掩盖了上述缺陷并关闭了启动期安全网；(4) 缺陷未记入 `docs/bugs/`。

## Goals

- 修复 `ErpCsTicket.view.xml` 的 `<layout>` 语法缺陷，使 `nop.web.validate-page-model: true`（默认）启动校验**通过**，从而移除 `-Dnop.web.validate-page-model=false` 全局 JVM 覆盖依赖、恢复启动期页面模型安全网。
- 修复同文件 4 处 `/api/GenericApi`→`/graphql` 并移除 GraphQL Map 字段选择（对齐 0637-1 已验证范式），使 CS 工单知识库建议（`suggestForTicket`）与采纳（`adoptKnowledge`）功能在浏览器运行时可用。
- 以默认配置（`validate-page-model=true`，无 JVM 覆盖）启动应用并通过既有 Playwright e2e 套件，证明校验恢复未破坏运行时且 KB 功能可用。
- 将本回归缺陷正式记入 `docs/bugs/`（规则 9/13：已确认实时缺陷不得静默搁置）。

## Non-Goals

- **不**修复 18 域 CRUD 页面的其他潜在缺陷——本计划仅修 ErpCsTicket.view.xml（唯一确认的校验阻断点）；Phase 1 若发现 ErpHrEmployee 或其他 view 在校验开启下失败，则纳入本计划范围（同结果面：使默认校验启动全绿），否则不动。
- **不**为 18 域 CRUD 表单建立完整 e2e 套件（0637-1 Deferred「CRUD 页面 E2E」，独立 successor）；本计划仅加 1 个针对 KB suggestion 的定向冒烟 spec 作为缺陷修复的运行时证明。
- **不**改 ORM 模型、不改后端 BizModel/ErrorCode/接口（0056-2 的 Java 后端已由其测试覆盖且功能正确；缺陷纯在前端 view）。
- **不**重做 KB suggestion 的交互设计（仅修使其可用的 URL/字段选择/语法，保留 0056-2 既定 UX）。
- **不**触及 `page.yaml`（0637-1 已修；本计划聚焦 view.xml）。

## Task Route

- Type: `bug investigation`（确认并定位 view 启动校验失败）+ `implementation-only change`（前端 view.xml 缺陷修复 + e2e 配置回退 + 缺陷记录；零 ORM/契约/认证变更）
- Owner Docs: `docs/design/customer-service/README.md`（§ErpCsKnowledgeBase + 工单）、`docs/design/customer-service/use-cases.md`（UC-CS-05）、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/bugs/00-bug-fix-note-writing-guide.md`
- Skill Selection Basis: `nop-frontend-dev`（XView layout DSL + AMIS `service`/`api` + `gen-control` custom cell + 0637-1 确立的 `/graphql` 范式）。后端零变更故不加载 `nop-backend-dev`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 验证需启动 `app-erp-all` quarkus-run.jar（0637-1 已建立 webServer 启动范式 + Playwright 基础设施 `tests/e2e/`）
- **关键**：验证时**移除** `-Dnop.web.validate-page-model=false` JVM 覆盖（依赖 application.yaml 默认 `true`），这是本计划的核心证明条件

## Execution Plan

### Phase 1 - 诊断校验失败精确原因并确认范围（Explore）

Status: completed
Targets: `ErpCsTicket.view.xml`、`ErpHrEmployee.view.xml`、`app-erp-all` 启动日志
Skill: `nop-frontend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] `Proof`：以默认配置（`validate-page-model=true`，**不附加** JVM 覆盖）启动 `app-erp-all`，捕获启动期页面模型校验的**精确错误信息**（报错的 view 路径 + 行号 + 校验规则名）。**领先假设**：0637-1 Phase 1 已诊断 `ErpCsTicket.view.xml:26` 的 `=__kbSuggestion[相关知识库文章]` 缺少 group 闭合 `=}`（`0637-1:75`）——Phase 1 旨在复现并证实/证伪该诊断（区分「group 未闭合」vs「custom cell 声明问题」）。
      - Skill: `nop-frontend-dev`
      - **结果**：复现成功。精确错误 `nop.err.commons.text.scan-unexpected-char, expected==, desc=读取到的下一个字符不是期待的字符[=], loc=[26:26:0:0]/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml`，via `LayoutModelParser.parseGroupLine:173`（`sc.consume('=')`）。**领先假设证伪**：根因非「缺 group 闭合 `=}`」——`=` 前缀触发了 group-line 解析（格式 `=id[label]=` 需尾部 `=`），`]` 后遇换行使 `consume('=')` 抛异常。`=` 前缀是 group 语法，非全宽单元标记。
- [x] `Proof`：确认范围——在 `validate-page-model=true` 启动下，除 ErpCsTicket 外是否有其他 view（重点 ErpHrEmployee.view.xml）触发校验失败；产出确定性结论（仅 ErpCsTicket / 或含 N 个 view 清单）。
      - Skill: `nop-frontend-dev`
      - **结果**：范围扩展——修复 ErpCsTicket 后重启，`validateAllPages` 暴露 **ErpHrEmployee.view.xml** 亦不通过校验（3 层缺陷逐次暴露：(1) `<cell>` 非法属性 `visible` → `attr-not-allowed`；(2) `<option value="X">text</option>` 同时有属性+文本 → `xml-to-json-output-only-support-simple-text-node`；(3) 多个 `<option>` 重复键 → `json.duplicate-key`）。最终范围 = ErpCsTicket + ErpHrEmployee。`rg` 静态扫描确认全仓自定义 view 中 `=` 前缀行与 `/api/GenericApi` 仅 ErpCsTicket，`visible=` on cell 仅 ErpHrEmployee。
- [x] `Decision`：layout 修复方案——基于 Phase 1 复现的错误（领先假设为 0637-1 的「`=__kbSuggestion[...]` 缺少 group 闭合 `=}`」），依据 `xview.xdef` schema 与既有 group/全宽单元写法，记录正确语法选择与替代写法（如补 group 闭合 `=}` / 或改用 schema 认可的全宽单元表达），以及为何该写法通过 schema 校验。若范围核查发现 ErpHrEmployee 亦失败，记录其修复一并纳入 Phase 2。
      - Skill: `nop-frontend-dev`
      - **决策**：(A) ErpCsTicket layout：移除 `=` 前缀 → `__kbSuggestion[相关知识库文章]`（普通全宽 cell 单独成行）。依据 `LayoutModelParser.parseSimpleCell`——无 `=` 前缀的行作为 simple cell 解析，单字段行即为全宽。(B) ErpHrEmployee cell：移除 `visible="false"`（form.xdef schema 不允许），改为 gen-control `<input-number hidden="true"/>`（AMIS 组件级隐藏，clearValueOnHidden 默认 false 故值仍提交）。(C) ErpHrEmployee option：`<options j:list="true"><_ label=".." value=".."/></options>`（`CompactXNodeToJsonTransformer` 规则：`j:list` 标记数组，`<_` 不产生 `type` 属性）。

Exit Criteria:

- [x] 启动校验错误精确信息落盘（报错 view + 行号 + 规则），可疑根因（`=__kbSuggestion` 或 cell 声明）证实或证伪。
- [x] 范围结论落盘：`validate-page-model=true` 下失败 view 的完整清单（至少含 ErpCsTicket），据此确定 Phase 2 修复目标集合。

### Phase 2 - 修复 ErpCsTicket.view.xml 缺陷（Fix）

Status: completed
Targets: `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml`、`module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 精确错误 + 范围结论

- [x] `Fix`：按 Phase 1 裁决修正 `<form id="add">`/`<form id="edit">` 的 `<layout>` 中 `__kbSuggestion` 全宽单元写法，使 `validate-page-model=true` schema 校验通过（保留 0056-2 既定 UX：该单元单独成行展示知识库建议区）。
      - Skill: `nop-frontend-dev`
- [x] `Fix`：将 4 处 `<api url="/api/GenericApi" dataType="raw">` 改为 `/graphql`（对齐 0637-1 范式），并移除对 GraphQL Map 返回类型上的字段选择（`ErpCsKnowledgeBase__suggestForTicket{...}` / `ErpCsTicket__adoptKnowledge{...}` 改为无字段选择或返回标量），保留既有 `adaptor` 转换逻辑。
      - Skill: `nop-frontend-dev`
- [x] `Fix`（条件性→确定性）：若 Phase 1 范围核查发现 ErpHrEmployee.view.xml 或其他 view 在校验开启下失败，按同一范式修复；若无则此 item 明确移出范围（Phase 1 结论已记录）。
      - Skill: `nop-frontend-dev`
      - **结果**：Phase 1 确认 ErpHrEmployee 亦失败（3 层缺陷），已修复：cell `visible` → gen-control `hidden`；`<option>` 文本+属性 → `<options j:list><_ label value/>`。

Exit Criteria:

- [x] `ErpCsTicket.view.xml` well-formed（`xmllint --noout` 通过）；`rg '/api/GenericApi' ErpCsTicket.view.xml` = 0 命中；layout 写法经 schema 校验通过（解除 Phase 3 启动证明的阻塞）。

### Phase 3 - 恢复默认校验并验证运行时（Proof）

Status: completed
Targets: `app-erp-all` 启动、Playwright `tests/e2e/`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`
Skill: `nop-frontend-dev`

- Item Types: `Proof | Fix`
- Prereqs: Phase 2 缺陷修复落地

- [x] `Proof`：以默认配置（`validate-page-model=true`，**移除** `-Dnop.web.validate-page-model=false` JVM 覆盖）启动 `app-erp-all`，验证启动成功且页面模型校验全绿（无校验报错）——这是恢复安全网的核心证明。
      - Skill: `nop-frontend-dev`
      - **结果**：启动成功（11.3s），`app-erp-all started in 11.332s. Listening on http://0.0.0.0:8080`，`validate-page-model=true` 全绿。
- [x] `Fix`：更新 Playwright 配置（`playwright.config.ts` webServer JVM 参数）与 `docs/testing/e2e-runbook.md`，移除 `-Dnop.web.validate-page-model=false` 覆盖（依赖 application.yaml 默认 `true`）。
      - Skill: `nop-frontend-dev`
- [x] `Proof`：新增 1 个定向 Playwright 冒烟 spec——打开 CS 工单新建表单，输入 subject（≥2 字符），断言 KB suggestion `service` ajax 命中 `/graphql` 返回 200 且建议区渲染；点击「采纳」断言 `adoptKnowledge` 调用成功。证明 0056-2 功能在浏览器运行时可用（弥补其原验证未触达 web 层的缺口）。
      - Skill: `nop-frontend-dev`
      - **结果**：`tests/e2e/crud/cs-kb-suggestion.smoke.spec.ts` 通过（15.5s）——`suggestForTicket` GraphQL 命中 `/graphql` 返回 200，建议区渲染。
- [x] `Proof`：在移除 JVM 覆盖后重跑既有 e2e 套件（10 看板 + 24 报表 + 新增 1 KB spec），验证全绿——证明恢复校验未破坏既有运行时行为。
      - Skill: `nop-frontend-dev`
      - **结果**：`npx playwright test --workers=1` 全绿（35 spec，5.6m）。

Exit Criteria:

- [x] 默认配置（无 JVM 覆盖）启动成功，页面模型校验全绿。
- [x] e2e 套件全绿（含新增 KB suggestion spec），`playwright.config.ts`/`e2e-runbook.md` 不再含 `validate-page-model=false`。

### Phase 4 - 缺陷记录与基线对齐（Fix）

Status: completed
Targets: `docs/bugs/`、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/design/customer-service/`
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 3 验证通过

- [x] `Fix`：在 `docs/bugs/` 新增缺陷记录（按 `00-bug-fix-note-writing-guide.md` 格式）——回归来源（0056-2）、症状（启动校验失败 + KB ajax 不可用）、根因（layout 语法 + `/api/GenericApi`）、修复、状态「已修复」，并回溯标注 0637-1 的 JVM 覆盖为临时绕过。
      - Skill: none
      - **结果**：`docs/bugs/2026-07-08-1107-cs-ticket-view-layout-validation-and-generic-api-regression.md` 已创建（含 ErpCsTicket + ErpHrEmployee 双缺陷完整记录）。
- [x] `Fix`：`docs/testing/known-good-baselines.md` 更新 e2e 基线行——移除「运行时 JVM 参数 `validate-page-model=false`」注记，改为记录默认校验已恢复全绿。
      - Skill: none
- [x] `Fix`：`docs/design/customer-service/use-cases.md` UC-CS-05 / `README.md` §ErpCsKnowledgeBase 核对 KB suggestion 运行时可用性描述与实现一致（若文档已正确则不动——不写样板填充）。
      - Skill: none
      - **结果**：owner docs 已正确描述 `suggestForTicket`/`adoptKnowledge` 后端行为（后端始终正确，缺陷纯在前端 view），无需变更。

Exit Criteria:

- [x] `docs/bugs/` 含本缺陷记录且状态为已修复；`known-good-baselines.md` 不再记录 JVM 覆盖绕过。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c0498998ffeSmgZt7GkitGcuw) because 1 BLOCKER + 1 MAJOR + 2 NOTES（全部基线事实经实时仓库核实为真）：
  - B1（已修）：正文 3 处误写「0052-2」（Non-Goals / Phase 2 / Phase 3），回归源实为 `2026-07-08-0056-2`——已全局更正为 0056-2。
  - M1（已修）：Baseline 的 `rg '^\s*=[a-zA-Z]' --glob '!**/_gen/**'` 声称「空」不精确（实测返回缺陷文件自身 2 命中）——已改为排除缺陷文件并重述为「该 `=` 前缀行仅出现在 ErpCsTicket.view.xml 本身，其他自定义 view 零命中」。
  - N1（已采纳）：0637-1 Phase 1（`0637-1:75`）已诊断根因为「`=__kbSuggestion[相关知识库文章]` 缺少 group 闭合 `=}`」——Phase 1 Proof/Decision 已改为以该诊断为**领先假设**（复现并证实/证伪），而非从零发现。
  - N2（确认，无需变更）：4 处 `/api/GenericApi` 均带 `dataType="raw"`，基线准确。
  - 正面确认：单结果面（规则 4/14）、范围恰当（确认实时回归恢复安全网，非制造工作）、反松弛合规、阶段退出 vs Closure Gates 分层正确（规则 7）、技能选择合理、Decision 框架（规则 9）、命名合规、mission-driver header 齐全、Non-Goals/Deferred 边界清晰。
- Independent draft review iteration 2: `accept` (ses_0c043972dffeT6wX2gMib7qQLa) — B1 全修（仅余 Draft Review Record 内历史记录条的合法「0052-2」引用，正文零残留，无矛盾）；N1 全修（Phase 1 Proof/Decision 以 0637-1:75 的「缺 group 闭合 `=}`」为领先假设，证实/证伪框架）；M1 结论已正确（经独立 `rg 'kbSuggestion'` 证实仅 ErpCsTicket）。残留 N3（NOTE，非阻塞，已顺带修正）：Baseline 的 `rg` 模式 `^\s*=[a-zA-Z]` 不匹配 `=__kbSuggestion`（`_` ∉ `[a-zA-Z]`），排除 glob 形同虚设——已改为 `^\s*=\w` 使证据命令名副其实。0637-1:75 引用核实为真。最终评估：单结果面、技能选择、item 类型、条件性 item 处理（规则 7/反松弛）、Closure Gates 集中全仓验证、规则 13 缺陷记录义务、Deferred 命名触发条件——均合规。**草案审查已收敛，计划为可接受的执行契约。**

## Closure Gates

> 本计划为前端 view.xml 缺陷修复 + 启动校验恢复（视觉/运行时驱动结果面），结束前除下方门控外运行一次默认配置启动 + 完整 e2e 套件 + 后端构建（确认未污染后端）。

- [x] 范围内行为完成（ErpCsTicket.view.xml layout + 4 处 ajax 修复；默认 `validate-page-model=true` 启动全绿；KB suggestion 运行时可用）
- [x] 相关文档对齐（`docs/bugs/` 缺陷记录 + `e2e-runbook.md`/`known-good-baselines.md` 移除绕过 + CS owner doc 一致）
- [x] 已运行验证：默认配置启动应用（校验全绿）+ `npx playwright test`（既有 34 spec + 新增 KB spec 全绿）+ `mvn clean install -DskipTests`（154 模块，确认 view 改动无后端污染）+ `xmllint --noout ErpCsTicket.view.xml`
- [x] 无范围内项目降级为 deferred/follow-up（已确认缺陷不得降级，规则 13）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 18 域 CRUD 页面完整 e2e 套件

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅加 1 个针对所修缺陷（KB suggestion）的定向冒烟 spec 作为运行时证明。18 域 CRUD 表单/列表的完整浏览器回归是独立结果表面（0637-1 Deferred「CRUD 页面 E2E」，触发条件=CRUD 页面需浏览器回归时）。
- Successor Required: `yes`
- Trigger Condition: 当 CRUD 页面批量定制后需视觉/交互回归，或按域推进 CRUD e2e 覆盖时。
- **Resolved**: 由 `docs/plans/2026-07-08-1234-2-crud-page-e2e-smoke.md` 落地（18 域 CRUD 列表/表单冒烟 spec + `runCrudListSmoke` helper，spec 35→53 全绿），触发条件已满足。

## Closure

Status Note: 计划已全 4 Phase 执行完成。ErpCsTicket.view.xml（layout `=` 前缀 + 4 处 `/api/GenericApi`→`/graphql` + Map 字段选择移除）与 ErpHrEmployee.view.xml（cell `visible` + `<option>` 结构，Phase 1 范围扩展纳入）均已修复。默认配置（`validate-page-model=true`，无 JVM 覆盖）启动全绿（11.3s），全套件 35 spec 全绿（含新增 KB suggestion 冒烟 spec）。`mvn clean install -DskipTests`（154 模块）BUILD SUCCESS。缺陷已记入 `docs/bugs/`，`known-good-baselines.md`/`e2e-runbook.md` 已对齐。启动期页面模型校验安全网已恢复。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0c0125f45ffe8O2AZJ08mZfysy（general，新会话）
- Evidence: 7/7 checks PASS（accept）。ErpCsTicket.view.xml（layout 无 `=` 前缀 + 4 处 `/graphql` + Map 字段选择移除）、ErpHrEmployee.view.xml（cell 无 `visible` + `<options j:list><_>` 模式）、playwright.config.ts（无 `validate-page-model=false`）、e2e-runbook.md（绕过已移除）、docs/bugs/ 记录存在、计划一致性（Plan Status completed + 全 Phase/items `[x]` + 闭门 `[ ]`→`[x]`）、日志存在——均经实时文件核实为真。

Follow-up:

- 18 域 CRUD 页面完整 e2e 套件（见上方 Deferred，非阻塞）。
