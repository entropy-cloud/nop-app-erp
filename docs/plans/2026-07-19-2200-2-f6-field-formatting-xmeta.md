# 2026-07-19-2200-2-f6-field-formatting-xmeta F6 — 金额/数量/日期字段格式化（xmeta 层统一）

> Plan Status: active
> Last Reviewed: 2026-07-19
> Source: `docs/backlog/frontend-ui-roadmap.md` F6（字段格式化）
> Related: `docs/plans/2026-07-19-1818-3-f5-status-tag-coloring.md`（F5 状态标签已完成，同样采用 view.xml gen-control inline 模式）；`docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（F4 Phase 2 同期落地，子表行金额字段格式化依赖本计划）；`docs/plans/2026-07-19-1818-2-f3-core-line-and-remaining-main-form-layout.md`（form 分组保留现有 `ui:number="true"` 不动，本计划补强格式化层）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-19）：

- **当前格式化状态**：抽样 `module-purchase/erp-pur-meta/.../ErpPurOrderLine/_ErpPurOrderLine.xmeta:46-72` 与 `ErpPurOrder/_ErpPurOrder.xmeta:68-86`：
  - `<schema domain="amount" type="java.math.BigDecimal" precision="20" scale="4"/>` — 仅声明 BigDecimal + 精度，**无 `ui:format` 或 AMIS format 字符串**
  - `domain` 属性支持 `amount`/`quantity`/`unitPrice`/`taxRate`/`taxAmount`/`exchangeRate` 等（见 `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md §domain 表` line 452-470）
- **view.xml 层**：抽样 `module-purchase/erp-pur-web/.../ErpPurOrder/ErpPurOrder.view.xml:7` `<col id="id" ui:number="true"/>` 与 `:15` `<col id="totalAmountWithTax"/>` — 仅 id 列加 `ui:number="true"`，金额/数量列无任何格式化属性；列表显示原始 BigDecimal 字符串（如 `1234.5` 而非 `1,234.50`）
- **既无统一金额/数量格式化基础设施**：金额/数量/单价/税率列 grep `<col.*format=` / `input-number.*format=` 在 `module-*/erp-*-web/` view.xml 中**零命中**；F5 plan（`2026-07-19-1818-3-f5-status-tag-coloring.md`）采用 view.xml `<gen-control><c:script>` inline AMIS tpl 模式落地状态标签，**未涉及金额/数量格式化**
- **既有 `format=` 属性先例**：日期字段已有 3 处 `<input-date format="YYYY-MM-DD">` 用例（`module-hr/erp-hr-web/.../ErpHrEmployee.view.xml:123` + `module-hr/erp-hr-web/.../ErpHrRecruitment.view.xml:67,95`），证实 AMIS `format=` 属性经 view.xml 透传机制可用；本计划 Phase 1 Explore (a) 复核金额列是否同样透传。
- **AMIS 原生支持**：AMIS `input-number` / `number` column 控件支持 `format` / `sprintf` / `kilometer`（千分位）/ `prefix` / `suffix` 属性（见 AMIS 文档，本计划 Phase 1 Explore 验证 nop-entropy codegen 是否将这些属性从 view.xml/xmeta 透传到 AMIS）
- **xmeta 层无 `ui:format` 标准属性**：grep nop-entropy `docs-for-ai/` + `app-erp-erp` xmeta 文件均无 `ui:format` 用例；可能机制：
  - **方案 A（首选）**：在 xmeta `<prop>` 上加 `ui:format="#,##0.00"` 属性 + codegen 模板支持透传到 view.xml col → AMIS column
  - **方案 B**：直接在 view.xml `<col format="..."/>` 配置（绕过 xmeta 层），需手动改 50+ 实体 view.xml
  - **方案 C**：经 codegen `domain → format` 全局映射（修改 codegen 模板的 default-control 推导逻辑），改动面广但一改全改
- **影响范围**：roadmap F6 标注「~50+ 实体 × 5-15 金额/数量列」（约 250-750 个 column 配置点）；加日期/百分比/税率列后总配置点约 800-1200
- **前置已就绪**：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`mvn test` 全绿；codegen 增量链可用。

## Goals

1. **建立 xmeta 层统一格式化机制**：经 Phase 1 Explore 在三种方案中裁决一种，使金额/数量/日期/百分比/税率字段一次配置全域生效，避免逐 view.xml 改造
2. **金额字段**（`domain="amount"`）显示 `#,##0.00` 千分位 + 右对齐 + 必要时附币种符号
3. **数量字段**（`domain="quantity"`）显示 `#,##0.####` 千分位（保留 4 位小数）+ 右对齐
4. **单价字段**（`domain="unitPrice"`）显示 `#,##0.0000` 千分位（4 位小数）+ 右对齐
5. **税率字段**（`domain="taxRate"`）显示 `#,##0.0000` 千分位（4 位小数）
6. **百分比字段**（如 `discountRate`）显示 `0.00%`
7. **日期字段**（`domain="date"`）统一 `YYYY-MM-DD`；日期时间字段（`domain="dateTime"`）统一 `YYYY-MM-DD HH:mm:ss`
8. **汇率字段**（`domain="exchangeRate"`）显示 `#,##0.00000000` 千分位（8 位小数，匹配 ORM `scale="8"`）
9. **抽样浏览器 E2E**：金额/数量/日期列千分位断言（`tests/e2e/visual/field-format.value.spec.ts` 新建）
10. 在 `docs/design/field-formatting-patterns.md` 固化格式化范式，含映射表 + 机制选择 + 反模式自检表（≥150 行）

## Non-Goals

- **修改 ORM 模型**（`*.orm.xml`）——保护区域，仅在 xmeta / view.xml / codegen delta 层
- **修改 nop-entropy 平台 codegen 模板**——平台保护区域；本计划在应用层 delta 覆盖（若需要）
- **币种符号本地化（CNY ¥ / USD $）**——按域级硬编码或 xmeta `currencyRef` 字段配置；本计划仅做千分位 + 小数位，币种符号归 i18n + l10n 域（roadmap F15 / `docs/design/l10n/`）
- **负数红字显示（会计专用）**——F5/cross-cutting 范畴；本计划仅做千分位格式，颜色归 F5 状态色继承
- **F7 敏感字段脱敏**（hr 证件号/手机/银行账户、logistics API Key/Secret）——F6 是脱敏前置基础设施但脱敏本身属 F7 范畴；本计划仅交付格式化机制
- **修改报表 / 看板金额字段格式化**——nop-report 走 `formatExpr` 独立机制（见 `2026-07-12-1321-3-report-date-param-fix` 类似范式）；本计划仅覆盖 view.xml 列表/表单
- **数据库存储精度调整**——`precision/scale` 由 ORM 定义，本计划不改存储精度，仅改显示格式
- **i18n 数字格式（部分欧洲国家 `.`/`,` 颠倒）**——F15 i18n 范畴；本计划使用 zh-CN 默认千分位 `,` + 小数点 `.`
- **拖拽列调整 + 列冻结**——UI 增强范畴，roadmap 明确 Non-Goal
- **Form 字段编辑态格式化（仅列表）**——本计划仅做列表列与子表行内字段格式化；form 控件金额字段编辑态保留原始输入（避免千分位干扰输入），查看态格式化归 successor（依赖 Phase 1 Explore 验证 AMIS form 字段 format 支持）
- **测试：视觉像素 diff**——DOM 文本断言（如断言列文本含 `,`），像素 diff 归独立计划（`2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md` 已覆盖基线）

## Task Route

- Type: `implementation-only change`
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F6（字段类型 → 格式要求表）
  - `docs/design/purchase/ui-patterns.md` + 各域 ui-patterns（金额字段语义来源）
  - `docs/architecture/view-and-page-strategy.md`（view.xml → AMIS 列展开管线）
  - `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §domain 表（`amount`/`quantity`/`unitPrice`/`taxRate` 等 domain 定义）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md` §7 `gen-control`（自定义控件机制）
  - `../nop-entropy/docs-for-ai/02-core-guides/frontend-rendering-pipeline.md`（xmeta → view.xml → AMIS 列展开链路）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml 列格式化 / xmeta 属性透传 / codegen delta）；不涉及 BizModel 改动（仅展示层），故不加载 `nop-backend-dev`；浏览器层 E2E 千分位断言需 `nop-testing` 辅助 Phase 4 编写新 spec。

## Infrastructure And Config Prereqs

- xmeta 文件路径：`module-<domain>/erp-<short>-meta/src/main/resources/_vfs/erp/<short>/model/<Entity>/<Entity>.xmeta`（保留层）或 `_gen/_<Entity>.xmeta`（生成层，**不改**）
- view.xml 文件路径：`module-<domain>/erp-<short>-web/src/main/resources/_vfs/erp/<short>/pages/<Entity>/<Entity>.view.xml`
- 修改后运行 `mvn clean install -DskipTests` 触发 codegen 增量重新生成 view.xml（若方案是 xmeta 属性透传）或仅重启应用（若方案是 view.xml 直接配置）
- 本地运行验证：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Playwright webServer 已配置

## Execution Plan

### Phase 1 — 范式探索与方案裁决

Status: planned
Targets: `docs/design/field-formatting-patterns.md`（新建）+ 方案裁决记录
Skill: `nop-frontend-dev`

- Item Types: `Decision | Add | Explore`
- Prereqs: none

- [ ] `Explore`: 验证 4 项并记录证据 file:line（**阻塞门控**：方案选定决定后续 Phase 实施路径）：
  - (a) **AMIS column/input-number format 透传**：抽样 `ErpPurOrder.view.xml` 在 `<col id="totalAmountWithTax">` 上加 `format="#,##0.00"` 测试属性，运行 `mvn clean install -DskipTests`，检查 `_dump/` 下展开后 page.yaml 的 `columns[]` 是否含 `format: "#,##0.00"`。若透传失败，验证 AMIS `tpl` 列类型 + `c:script` 内 `Number(...).toLocaleString()` 是否生效。
  - (b) **xmeta `<prop>` 上 `ui:format` 属性支持**：抽样 `ErpPurOrderLine.xmeta` 在 `<prop name="amount">` 上加 `ui:format="#,##0.00"` 测试属性，运行 `mvn clean install -DskipTests`，检查生成 view.xml 的 `<col id="amount">` 是否含 `format` 属性。若不支持，核实是否存在 `ui:schema` / `ui:mask` / `ui:transformer` 等替代属性。
  - (c) **codegen `domain → AMIS column default` 推导逻辑**：grep nop-entropy 源码（`nop-web/src/main/resources/_vfs/nop/web/xlib/web/` 下 `view-gen.xlib` / `grid_crud.xpl`）查找 `<domain>` → AMIS column type + format 推导逻辑；核实是否存在应用层 delta 注册自定义 domain → format 映射的扩展点（如 `app-erp-web/.../_delta/nop/web/...`）。同时核实 ORM `domains` 定义表（`module-finance/model/app-erp-finance.orm.xml:285-295` 等）与 nop-entropy `orm-model-design.md §domain 表` 文档定义的偏差（实际 codebase 含 `unitPrice`/`taxAmount` 等 domain，文档仅列 `price`/`amount`），裁决 codegen 方案 C 能否依赖规范 domain 集。
  - (d) **F5 inline gen-control 范式适用性 + 既有 `format=` 先例**：F5 plan（`2026-07-19-1818-3`）采用 `<gen-control><c:script>` inline AMIS tpl 渲染状态标签，本计划是否可复用此模式落地金额格式化（如 `<col id="amount"><gen-control><c:script>return {type:'tpl', tpl:'${amount | toFixed:2 | thousand}'}</c:script></gen-control></col>`）；同时核实既有 `<input-date format="YYYY-MM-DD">` 3 处用例（`module-hr/erp-hr-web/.../ErpHrEmployee.view.xml:123` + `ErpHrRecruitment.view.xml:67,95`）的透传机制是否能直接推广到金额/数量列。
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 基于 Explore (a)-(d) 证据裁决格式化机制（按优先级排序）：
  - **方案 A（首选）**：xmeta `<prop ui:format="...">` 属性 + codegen 透传到 view.xml `<col format="...">` + AMIS 渲染。优点：一次配置，列表+表单+子表行均生效；改动面集中。
  - **方案 B**：view.xml `<col format="...">` 直接配置（仅在 xmeta 透传失败时降级）。缺点：需改 50+ view.xml；form 字段不受益。
  - **方案 C**：codegen `domain → format` 全局映射 delta（应用层 `_delta/nop/web/.../`）。优点：一改全改，无需逐实体配置；缺点：影响 nop-entropy 平台行为，违反「应用层优先」原则，仅当 A/B 均不可行时降级。
  - **方案 D**：F5 inline `gen-control` + `c:script` 模式落地格式化。优点：已被 F5 验证可行；缺点：每实体每列需手写 view.xml，工作量与方案 B 相当。
  - 记录替代方案与残留风险到 plan
  - Skill: `nop-frontend-dev`
- [ ] `Decision`: 决策格式化映射表（在 plan 内固化，作为后续 Phase 实施依据）。**Phase 1 Explore 须逐行核实下表与 ORM 实际 scale + roadmap F6 行的偏差并记录裁决**：

  | domain | ORM 实际 scale | 显示格式 | AMIS format 字符串 | 对齐 | 与 roadmap F6 偏差裁决 |
  |--------|---------------|---------|-------------------|------|----------------------|
  | amount | 2-4（多 scale） | 千分位 + 2 位小数 | `#,##0.00` | 右对齐 | 与 roadmap F6 一致 |
  | quantity | 4 | 千分位 + 4 位小数 | `#,##0.0000` | 右对齐 | 偏差：roadmap F6 给 `#,##0.###`（3 位）；本计划按 ORM scale=4 选 4 位，保留存储精度，避免显示丢精度 |
  | unitPrice | 4 | 千分位 + 4 位小数 | `#,##0.0000` | 右对齐 | 与 roadmap F6 一致 |
  | taxRate | 4 | 千分位 + 4 位小数 | `0.0000` | 右对齐 | 偏差：roadmap F6 给 `0.0000`（无千分位）；税率值通常 < 1（如 0.13）千分位无意义，**采纳 roadmap 写法 `0.0000` 不带千分位** |
  | taxAmount | 2 | 千分位 + 2 位小数 | `#,##0.00` | 右对齐 | 与 roadmap F6 一致 |
  | exchangeRate | 8 | 千分位 + 8 位小数 | `#,##0.00000000` | 右对齐 | 偏差：roadmap F6 未明确；ORM scale=8 本计划保留 8 位避免丢精度 |
  | percentage | 2 | 百分比 + 2 位小数 | `0.00%` | 右对齐 | 与 roadmap F6 一致 |
  | date | - | 短日期 | `YYYY-MM-DD` | 居中 | 与 roadmap F6 + 既有 HR 3 处用例一致 |
  | dateTime | - | 长日期时间 | `YYYY-MM-DD HH:mm:ss` | 居中 | 与 roadmap F6 一致 |

  - Skill: `none`
- [ ] `Add`: 在 `docs/design/field-formatting-patterns.md` 新建文档，固化方案裁决 + 格式化映射表 + 范式说明 + 反模式自检表（≥150 行）。
  - Skill: `none`

Exit Criteria:

- [ ] Phase 1 Explore (a)-(d) 四项门控证据落地（含 file:line），若裁决替代机制则记录残留风险
- [ ] 格式化机制决策在 plan 表格化记录（含替代方案与残留风险）
- [ ] 格式化映射表（domain → format 字符串）在 plan 内固化
- [ ] `docs/design/field-formatting-patterns.md` 文件落地（≥150 行）

### Phase 2 — amount / quantity / unitPrice / taxRate 格式化实施

Status: planned
Targets: 按 Phase 1 裁决的机制，覆盖 4 核心域（purchase/sales/inventory/finance）+ master-data 主实体的金额/数量/单价/税率字段
Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1

- [ ] `Add`: 按方案 A/B/C/D 之一（Phase 1 裁决），覆盖金额/数量/单价/税率 4 类字段：
  - **核心 4 域头实体 + Line 实体**（~30 实体 × 5-10 字段/实体 = ~150-300 字段配置点）：purchase 14 + sales 13 + inventory 16 + finance 30
  - **master-data 主实体价格/汇率字段**（~10 字段）：物料 defaultPurchasePrice、币种 exchangeRate、汇率表 rate 等
  - 若方案 A（xmeta 属性）：在保留层 `ErpPurOrderLine.xmeta` 加 `ui:format` 属性，codegen 自动透传；保留层只改 entity 主键相关字段，列字段继承 `_gen/_<Entity>.xmeta`
  - 若方案 B（view.xml 直接）：在 `<col id="amount" format="#,##0.00"/>` 直接配置
  - 若方案 D（inline gen-control）：参考 F5 范式，每金额列加 `<gen-control><c:script>` 渲染
  - Skill: `nop-frontend-dev`
- [ ] `Fix`: 若 Explore (a) 透传链路存在精度漂移（如 AMIS tpl 内 `${amount | toFixed:2}` 与 xmeta `scale="4"` 不一致），调整为 AMIS `input-number` 控件 `precision` + `step` 配置而非 `format` 字符串
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，抽样 4 域列表页 + 子表行字段：
  - 抽样 purchase：`ErpPurOrder` 列表 totalAmountWithTax 显示 `123,456.78` 而非 `123456.78`；ErpPurOrderLine 子表行 amount 显示 `12.3456`（4 位小数）
  - 抽样 sales：同 purchase
  - 抽样 inventory：StockMove 数量字段 + LandedCost 金额字段
  - 抽样 finance：Voucher 行 debitAmount/creditAmount + Voucher 头 totalAmount
  - 每抽样记录证据到 plan（file:line + 实际渲染截图描述）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 核心域金额/数量/单价/税率字段格式化生效（千分位 + 正确小数位）
- [ ] 4 抽样域列表 + 子表行格式化正确（含 file:line 证据）
- [ ] 本地化验证：4 域 view.xml/xmeta 经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 3 — exchangeRate / percentage / date / dateTime 格式化实施

Status: planned
Targets: 全域汇率/百分比/日期字段
Skill: `nop-frontend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 2（沿用 Phase 2 机制）

- [ ] `Add`: 按 Phase 1 裁决机制覆盖：
  - **汇率字段**（`exchangeRate`）：~5-10 字段（master-data ErpMdExchangeRate + 各单据头 exchangeRate + finance 票据汇率）
  - **百分比字段**（`discountRate` 等）：~5-10 字段（purchase/sales 头表 discountRate + 制造完成率 + 资产折旧率）
  - **日期字段**（`businessDate`/`deliveryDate`/`invoiceDate` 等）：~50+ 字段（每域头实体含至少 2-3 日期字段）；统一 `YYYY-MM-DD`
  - **日期时间字段**（`createTime`/`updateTime`/`approvedAt`/`postedAt`）：~30+ 字段；统一 `YYYY-MM-DD HH:mm:ss`
  - Skill: `nop-frontend-dev`
- [ ] `Fix`: 若日期字段已在 view.xml codegen 默认显示 ISO 时间戳（如 `2026-07-19T10:30:00Z`），统一改为 `YYYY-MM-DD` 友好格式；保留 audit 时间戳原值（不格式化，便于追溯）
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，抽样 4 域：
  - 抽样 master-data：ErpMdExchangeRate rate 显示 `0.123456`（6 位千分位小数）
  - 抽样 finance：ErpFinVoucher businessDate 显示 `2026-07-19` 而非 ISO 时间戳
  - 抽样 purchase：ErpPurOrder discountRate 显示 `1.50%`
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 汇率/百分比/日期/日期时间字段格式化生效
- [ ] 4 抽样域字段格式化正确（含 file:line 证据）
- [ ] 本地化验证：全域经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 4 — 扩展域全覆盖（mfg/assets/projects/quality/maintenance/crm/cs/hr/aps/logistics/b2b/contract/drp）

Status: planned
Targets: 13 扩展域剩余金额/数量/日期字段
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [ ] `Add`: 按 Phase 1 裁决机制覆盖 13 扩展域：
  - 每域抽样核实金额/数量/日期字段是否在 `domain` 属性覆盖范围；未在 domain 列表内的字段（如 ext 域自定义精度字段）需手动加 `ui:format` 属性（若方案 A）或 view.xml col `format`（若方案 B）
  - 优先覆盖：manufacturing（cost/variance）+ assets（netValue/depreciation）+ projects（cost/revenue）+ quality（measurement）+ maintenance（cost/sparePart）+ logistics（freight）+ b2b（amount）+ contract（amount/rebate）+ drp（quantity）+ crm（amount）+ cs（sla）+ hr（salary）+ aps（load/capacity）
  - Skill: `nop-frontend-dev`
- [ ] `Proof`: 启动 app，抽样每扩展域 1 实体列表页验证格式化生效
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 13 扩展域金额/数量/日期字段格式化生效
- [ ] 每域 ≥1 实体抽样证据落地
- [ ] 本地化验证：全域经 `mvn clean install -DskipTests` + `ErpAllWebPagesCollectTest` 全绿

### Phase 5 — Playwright E2E 千分位 DOM 断言

Status: planned
Targets: `tests/e2e/visual/field-format.value.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 4

- [ ] `Add`: 新建 `tests/e2e/visual/field-format.value.spec.ts`，覆盖 4 核心域列表页金额/数量/日期列 DOM 文本断言：
  - 抽样 purchase：ErpPurOrder 列表 totalAmountWithTax 列文本含 `,`（千分位）+ `.`（小数点）+ 2 位小数
  - 抽样 sales：同 purchase
  - 抽样 inventory：StockBalance 数量列文本含 `,` + 4 位小数
  - 抽样 finance：ErpFinVoucher businessDate 列文本格式 `YYYY-MM-DD`
  - 经 GraphQL `__findPage` 取值（避免 page.route 复杂化），与既有 `*.value.spec.ts` 范式一致；helper 原语经 Phase 1 Explore (d) 核实 `tests/e2e/visual/_helper.ts` 是否已有 `assertColumnFormat`，存在则复用，不存在则在本 Phase 内一并新建（落到 `tests/e2e/visual/_helper.ts`）
  - Skill: `nop-testing`
- [ ] `Proof`: `npx playwright test tests/e2e/visual/field-format.value.spec.ts` 全绿；不影响其他 spec（0 回归）
  - Skill: `nop-testing`

Exit Criteria:

- [ ] `tests/e2e/visual/field-format.value.spec.ts` 落地，4 测试全绿
- [ ] 全套件回归：`npx playwright test` 0 新增失败

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_084ffaedcffeteiOEBNQhc8DaZ`) — 2 blockers (B1: "零命中" grep claim inaccurate — 3 HR `<input-date format=>` precedents exist; B2: format mapping table diverged from roadmap F6 + ORM scale) + 4 major concerns (M1: 反松弛 hedges; M2: helper primitive hedge; M3: domain citation mismatch; M4: Goal #10 positioning).
- Independent draft review iteration 2: **accept** (`ses_084f88ca8ffenSuIlyotN9rdvz`) — all blockers resolved: B1 narrowed grep + acknowledged HR precedents with file:line; B2 added "与 roadmap F6 偏差裁决" column with recorded justifications + matched ORM scale (exchangeRate `#,##0.00000000` matching scale=8); M1-M4 resolved. One minor decimal-count inconsistency (Goal #8 + table description) fixed in same revision pass after iteration 2 caught it. Plan acceptable for active status.

## Closure Gates

> 全部 Phase 完成且退出标准 `[x]` 后关闭。完整仓库验证在此处运行。

- [ ] 范围内行为完成（Phase 1–5 全部 done；金额/数量/日期字段格式化全域生效）
- [ ] 相关文档对齐：`docs/design/field-formatting-patterns.md` 落地；4 域 `ui-patterns.md` 各补「字段格式化」段落（如适用）；`docs/backlog/frontend-ui-roadmap.md` F6 行标 completed
- [ ] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ `mvn test`（含 `ErpAllWebPagesCollectTest` PAGE_ERROR_COUNT=0）+ `npx playwright test tests/e2e/visual/field-format.value.spec.ts`（4 测试全绿）+ 全套件回归 0 新增失败
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 币种符号本地化（CNY ¥ / USD $）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 币种符号需配合 i18n + l10n 域配置（roadmap F15 + `docs/design/l10n/`）；本计划仅做千分位 + 小数位，币种符号显示需用户 locale + 币种字典联动。
- Successor Required: `yes`（触发条件：F15 i18n + l10n 域 plan 启动时）

### 负数红字显示（会计专用借贷方向色）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 负数红字属会计专用显示语义（借方金额红字、贷方金额黑字等），需在 F5 状态色基础上扩展「方向色」机制。本计划仅做千分位格式。
- Successor Required: `yes`（触发条件：F5 状态色继承 / finance 域专用借/贷方向色 plan 启动时）

### F7 敏感字段脱敏（hr 证件号/手机/银行账户、logistics API Key/Secret）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 脱敏机制（`**************`、`138****0000`、`工行****1234`）属 F7 cross-cutting 范畴；F6 是脱敏前置基础设施（统一格式化机制可被脱敏复用），但脱敏本身需独立设计 mask 规则 + 二次验证流程。
- Successor Required: `yes`（触发条件：F7 cross-cutting 敏感字段脱敏 plan 启动时）

### 报表 / 看板金额字段格式化（nop-report 走 formatExpr 独立机制）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: nop-report 走 `formatExpr` 表达式独立机制（见 `2026-07-12-1321-3-report-date-param-fix` 范式），与 view.xml AMIS format 不同；本计划仅覆盖 view.xml 列表/表单层。
- Successor Required: `yes`（触发条件：报表格式化增强 plan 启动时，或 nop-report 模板统一审计时）

### 修改 nop-entropy 平台 codegen 模板（方案 C 全局映射）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 Phase 1 Explore 裁决方案 C（codegen 全局 domain → format delta）不可行（如 nop-entropy 不支持应用层 delta 注册），归平台扩展提案 successor。
- Successor Required: `yes`（触发条件：nop-entropy 平台 codegen 扩展点落地时）

### Form 字段编辑态格式化

- Classification: `optimization candidate`
- Why Not Blocking Closure: Form 编辑态金额字段保留原始输入（避免千分位干扰输入）是当前设计；查看态格式化属 successor，依赖 Phase 1 Explore (a) 验证 AMIS form 字段 `format` 支持，若不支持则需独立 form 控件改造。
- Successor Required: `yes`（触发条件：Phase 1 Explore (a) 裁决 form 字段 format 支持时）

## Closure

Status Note: 待 Phase 1–5 全部 done 后填写。

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计子代理（新会话，2026-07-XX）>
- Evidence: <待填>

Follow-up:

- F7 敏感字段脱敏（hr/logistics）独立 plan
- 币种符号本地化（F15 i18n + l10n）
- 负数红字（会计专用借/贷方向色）独立 plan
- 报表金额字段格式化（nop-report formatExpr 统一审计）
- nop-entropy 平台 codegen 全局 domain → format 映射扩展提案（若 Phase 1 裁决方案 A 不可行）
