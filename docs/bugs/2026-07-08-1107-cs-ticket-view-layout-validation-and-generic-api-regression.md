# 2026-07-08-1107-cs-ticket-view-layout-validation-and-generic-api-regression

## 问题

- 以默认配置（`nop.web.validate-page-model=true`）启动 `app-erp-all` 时，启动期页面模型校验（`PageModelValidator.validate`）失败导致应用崩溃，无法启动。
- `ErpCsTicket.view.xml` 的 KB 建议（`suggestForTicket`）与采纳（`adoptKnowledge`）功能运行时不可用：4 处 ajax 使用不存在的 `/api/GenericApi` 端点（正确为 `/graphql`），且对 GraphQL Map 返回类型使用了字段选择。
- `ErpHrEmployee.view.xml` 调动表单同样不通过页面校验（`<cell>` 非法属性 `visible`；`<option>` 元素同时有属性与文本内容；重复子节点产生 JSON 重复键）。
- 影响/严重性：**安全网退化**——0637-1 计划执行期间以 `-Dnop.web.validate-page-model=false` 全局关闭启动校验作为临时绕过，此后任何页面模型缺陷都不会在启动期被捕获。

## 复现

- 环境：`app-erp-all` quarkus-run.jar（构建后），JDK 25。
- 触发：`java -jar app-erp-all/target/quarkus-app/quarkus-run.jar`（不附加任何 `-Dnop.web.validate-page-model=false` 覆盖，依赖 application.yaml 默认 `true`）。
- 最小行为：启动 ~6s 后崩溃，日志报 `Failed to start application`，根因 `LayoutModelParser.parseGroupLine:173` `scan-unexpected-char expected '=='`。

## 诊断方法

- 诊断难度：中等——缺陷被 JVM 全局覆盖掩盖（`validate-page-model=false`），运行时不可见，需主动移除覆盖才能暴露。
- 调查路径：(1) 阅读计划基线得知 0637-1 以 JVM 覆盖绕过；(2) 阅读 `LayoutModelParser.java` 源码追踪 `=` 前缀行的解析逻辑；(3) 以默认配置启动复现，捕获精确错误。
- 被拒绝的假设：0637-1 的领先假设为「`=__kbSuggestion[...]` 缺少 group 闭合 `=}`」。**证伪**：实际根因是 `=` 前缀触发了 **group-line** 解析（格式 `=id[label]=` 需尾部 `=`），`parseGroupLine:173` 的 `sc.consume('=')` 在 `]` 后遇到换行符而抛异常——这不是"缺闭合 `=}`"，而是 `=` 前缀本身就是错误语法（应为普通全宽单元，无 `=` 前缀）。
- 决定性证据：运行时错误 `nop.err.commons.text.scan-unexpected-char, expected==, loc=[26:26:0:0]/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml` + 源码 `LayoutModelParser.java:102-110,173`（group 行的 `=` 消费逻辑）。
- 范围确认：修复 ErpCsTicket 后重启，发现 ErpHrEmployee.view.xml 暴露第二处缺陷（`attr-not-allowed:visible`）；继续修复后发现第三处（`xml-to-json-output-only-support-simple-text-node`，`<option>` 同时有属性与文本）；再修复后发现第四处（`json.duplicate-key`，多个 `<option>` 产生重复键）。逐层暴露是因为 `validateAllPages` 在首个致命错误即崩溃。

## 根本原因

- **ErpCsTicket.layout**：`=__kbSuggestion[相关知识库文章]` 使用了 group-line 语法（`=` 前缀），但 group 行要求尾部 `=`（`=id[label]=`），缺少尾部 `=` 导致 `sc.consume('=')` 抛异常。`=` 前缀被误用为"全宽单元"标记，实际应为普通 cell（无 `=` 前缀）。
- **ErpCsTicket.ajax（4 处）**：`/api/GenericApi` 端点在 Nop 平台中不存在（正确为 `/graphql`）；且对 GraphQL Map 返回类型使用字段选择 `{ id code ... }`，引擎报错。与 0637-1 修复的 34 个 page.yaml 同类缺陷（`/api/GenericApi`→`/graphql` + Map 字段选择移除），但 0637-1 未覆盖 view.xml。
- **ErpHrEmployee.cell**：`<cell>` 的 `visible` 属性不在 xview.xdef schema 允许列表内（form.xdef 仅允许 `notSubmit`/`custom`/`readonly`/`clearValueOnHidden` 等）。
- **ErpHrEmployee.option**：`<option value="X">text</option>` 同时有属性与文本内容，违反 Nop XML→JSON 规则（有属性的节点不允许直接文本）；且多个同名 `<option>` 子节点在 JSON 对象中产生重复键。
- **回归来源**：ErpCsTicket 缺陷由计划 `2026-07-08-0056-2`（CS 知识库建议与采纳功能）引入；ErpHrEmployee 缺陷由计划 `2026-07-08-0517-2`（HR 调动 drawer）引入。二者验证手段均为 Java 测试 + `mvn install`，均不启动 web 页面校验层，故回归未被捕获。

## 修复

- **ErpCsTicket.layout**：移除 `=` 前缀，`__kbSuggestion[相关知识库文章]` 作为普通全宽 cell 单独成行（保留 0056-2 既定 UX）。
- **ErpCsTicket.ajax（4 处）**：`url="/api/GenericApi"` → `url="/graphql"`；移除 Map 返回类型的字段选择（`{ id code title contentSummary categoryId }` × 2 + `{ id }` × 2），保留既有 `adaptor` 转换逻辑（adaptor 从 Map 全字段中提取所需字段）。
- **ErpHrEmployee.cell**：移除非法 `visible="false"` 属性，改为 gen-control 内 `<input-number hidden="true"/>`（AMIS 组件级隐藏，`clearValueOnHidden` 默认 false 故值仍提交）。
- **ErpHrEmployee.option**：`<option value="X">text</option>` × 3 改为 `<options j:list="true"><_ label="text" value="X"/></options>`——`j:list="true"` 标记 JSON 数组，`<_` 标签名不产生 `type` 属性（CompactXNodeToJsonTransformer 规则 5），产出正确的 AMIS `options: [{label, value}]` 结构。
- **恢复安全网**：移除 `playwright.config.ts` webServer 与 `e2e-runbook.md` 中的 `-Dnop.web.validate-page-model=false` 覆盖，依赖 application.yaml 默认 `true`。

## 测试

- `tests/e2e/crud/cs-kb-suggestion.smoke.spec.ts` — 验证 CS 工单新建表单输入 subject(≥2 字符) 后 `suggestForTicket` GraphQL 命中 `/graphql` 返回 200（级别：e2e）。仅 e2e 可行：KB 建议是浏览器 AMIS `service` 组件运行时行为，Java 测试无法触达 web 渲染层。
- 全套件回归：`npx playwright test --workers=1`（35 spec：10 看板 + 24 报表 + 1 KB）全绿（5.6m），证明恢复校验未破坏既有运行时。
- 启动验证：默认配置（无 JVM 覆盖）启动成功（11.3s），`validate-page-model=true` 页面校验全绿。

## 受影响的工件

- `module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml:26,91,34,55,99,120` — layout `=` 前缀移除 + 4 处 ajax URL/字段选择修复
- `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml:22,49-53` — cell `visible` 移除 + option 结构修复
- `playwright.config.ts:18` — 移除 `-Dnop.web.validate-page-model=false`
- `docs/testing/e2e-runbook.md:38,49,96` — 移除绕过注记，改为校验已恢复
- `tests/e2e/crud/cs-kb-suggestion.smoke.spec.ts` — 新增 KB 建议冒烟 spec

## 未来重构注意事项

- **view.xml layout DSL**：`=` 前缀是 group-line 语法（需尾部 `=`），不是全宽单元标记。全宽单元直接写 `field[label]` 单独成行。若有人复制 group 语法做全宽单元，将再次触发 `scan-unexpected-char`。
- **view.xml gen-control 中的 select options**：必须用 `<options j:list="true"><_ label=".." value=".."/></options>`，不能用 `<option value="X">text</option>`（违反 XML→JSON 文本规则 + 重复键）。`CompactXNodeToJsonTransformer` 规则：`<_` 标签名不产生 `type` 属性，其他标签名成为 `type` 属性。
- **`validate-page-model` 安全网**：启动期页面校验是防止 view.xml 缺陷进入运行时的关键防线。任何 `-Dnop.web.validate-page-model=false` 覆盖都应是临时的，必须在同一计划内修复根因并移除覆盖，否则后续页面缺陷将不被捕获（本 bug 即如此：0056-2/0517-2 的缺陷在被 0637-1 全局绕过后持续存在）。
- **view.xml 验证手段差距**：Java 测试 + `mvn install` 不启动 web 页面校验层。view.xml 变更后，如需验证 schema 合规性，必须以默认配置（`validate-page-model=true`）实际启动应用。

## 预防差距

- 0056-2/0517-2 引入 view.xml 变更但仅以 Java 测试 + `mvn install` 验证，缺少启动期页面校验验证（`validate-page-model=true` 启动）。
- 0637-1 发现缺陷后以全局 JVM 覆盖绕过而非修复根因，并将绕过记录为"已知限制"而非 bug——导致安全网退化持续存在直到本计划修复。
