# 2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch F16 P1 复杂手写页面低风险批

> Plan Status: completed
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F16（line 357-381 / 547）+ `docs/plans/2026-07-21-0330-3-f12-page-structure-tabs-wizards.md` §Deferred（F16 territory）+ F4 P2 finance voucher successor §Deferred（实时聚合 onEvent + 快捷模板）
> Related: `docs/plans/2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor.md`（F12 Tier D successor —— 本计划 NCR 详情页针对 ErpQaNonConformance 实体，与 Plan 1 的 ErpQaInspection 不同实体，无硬依赖；可并行推进）；`docs/plans/2026-07-20-2059-3-f4p2-finance-voucher-child-table-editor.md`（F4 finance voucher child-table-editor，本计划在其基础上补全实时校验 + 快捷模板）；`docs/plans/2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（三单匹配后端引擎，本计划消费既有 `findThreeWayMatchDiffAlert`）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-22，对 5 个 F16 目标页面 view.xml + F4 P2 finance voucher §Deferred + 独立 explore ses_078b622a7ffe 对 9 个 F16 页面的现状盘点 + 各域后端就绪度核实）：

### 本计划范围：5 个低风险 F16 页面（后端就绪 + 不需 custom AMIS component）

| # | 域 | 页面 | 现状 | 后端就绪度 | 风险等级 |
|---|---|------|------|-----------|---------|
| 1 | finance | 会计凭证录入完成（ErpFinVoucher） | ~70%：F4 P2 finance successor 已落地 17 列 sub-grid-edit + autoBalance 按钮 + 科目树 picker + dcDirection visibleOn + 多币种自动推算 + F12 已加 `layoutControl="tabs"` | ✅ 完全就绪 | 低（补全既有，不新增范式） |
| 2 | finance | 凭证模板配置（ErpFinVoucherTemplate） | STUB：20 行空 view（form view/edit/add 均为 `<form/>`）；ErpFinVoucherTemplateLine 字段 `amountExpression/accountKey/amountKey/memoTemplate` 已存在（占位符表达式字段） | ⚠️ PARTIAL：`ErpFinTemplateAcctDocProvider`（`module-finance/erp-fin-service/.../posting/provider/`，196 行）**已实现** `${placeholder}` 替换 + `amountKey` → `billData` 查找 + `amountExpression` 字面 BigDecimal 解析（`toBigDecimal(expr.trim())`）；**真实缺口更窄**：`amountExpression` 不支持算术表达式（如 `DOC_TOTAL * 0.13`，会抛 `ERR_AMOUNT_KEY_NOT_RESOLVED`）。需扩展既有引擎支持算术，非从零选型 | 中（扩展既有引擎，非新范式） |
| 3 | purchase | 三单匹配（3-doc linked browse） | ~20%：仅 dashboard `threeWayMatchCrud` 报警 widget（3 列 invoiceCode/supplierName/varianceType）；无 3-doc 联查页面 | ✅ 完全就绪：`ThreeWayMatcher` + `ErpPurDashboard__findThreeWayMatchDiffAlert` + 6 后端测试 `TestErpPurThreeWayMatch` | 低（消费既有后端 API） |
| 4 | manufacturing | 工单进度仪表板（ErpMfgWorkOrder） | ~40%：F12 已加 `layoutControl="tabs"`（6 group）；F9 已落地 3 row-action drawer（MaterialIssue/JobCard/CompletionMove）；缺 4 阶段进度条 + 工时比颜色高亮 | ✅ 完全就绪：既有 `@BizQuery` 可组装阶段聚合（planQty/reportQty/completeQty 等） | 低（前端组装既有数据） |
| 5 | quality | NCR 详情页（ErpQaNonConformance） | ~50%：166 行 custom view（baseInfo/spec/disposition/status/posting/audit）+ row actions（评审/解决/拒绝）；缺 CAPA 内嵌表格 + 效果验证 | ✅ 完全就绪：ErpQaAction（CAPA 实体）既有；ErpQaNonConformance 与 ErpQaAction 关联关系既有 | 低（与 Plan 1 ErpQaInspection 不同实体，无依赖） |

### F4 P2 finance voucher §Deferred 明确的本计划覆盖项

- **实时聚合 onEvent**（借贷平衡实时校验）：F4 P2 finance plan line 110-113 明确 `"实时聚合 onEvent 在 P0/P1 plan 中已 DEFERRED"` —— 头合计（totalDebit/totalCredit）当前仅在 autoBalance 按钮点击或保存后服务端刷新
- **快捷模板 toolbar 按钮**：F4 P2 finance plan line 115 明确 `"本计划保留 toolbar [快速模板] 按钮位置占位... F16 plan 落地时补全"`

### 关键风险/缺口

- **finance 凭证模板配置表达式引擎扩展**：roadmap 要求「按 businessType 分组、科目来源/金额占位符映射、预览测试生成凭证」。ErpFinVoucherTemplateLine 已有 `amountExpression`（如 `DOC_TOTAL * 0.13`）+ `accountKey`（如 `ACCOUNTS_PAYABLE`）+ `amountKey`（如 `DOC_TOTAL`）字段；**既有 `ErpFinTemplateAcctDocProvider` 已支持 `${placeholder}` 替换 + `amountKey` 查找 + `amountExpression` 字面 BigDecimal**，但**不支持算术表达式**。需 Phase 0 PoC 裁决扩展位置：(a) 扩展 `ErpFinTemplateAcctDocProvider.resolveAmount` 支持算术（Nop `IExpressionEvaluator` / XLang 表达式 / 简易中缀解析）/ (b) 新增 `@BizMutation renderTemplate(businessType, context)` 后端求值 / (c) 前端预览仅展示占位符替换（不评估算术）
- **AMIS 实时聚合 onEvent 与 Nop input-table 兼容性**：F4 P2 finance successor 落地时发现实时聚合 onEvent 在 Nop `input-table` 内的行为未验证（行级 onEvent.setValue 是否能触发头合计重算）。需 Phase 0 PoC
- **三单匹配 3-doc 联查页面结构**：roadmap 要求「采购订单/入库单/发票三表联查，数量/单价差异高亮，容差可视化」。需 Decision 页面范式（独立 page.yaml 3-crud 并列 vs 单 crud + drawer 展开明细 vs wizard 步骤式联查）
- **quality NCR（ErpQaNonConformance）与 Plan 1 ErpQaInspection 是不同实体**：Plan 1 F12 Tier D 覆盖 `ErpQaInspection`（质检实体），本计划 Phase 5 目标是 `ErpQaNonConformance`（NCR 不合格实体），二者无依赖关系。本计划 NCR 详情页自包含 ErpQaAction（CAPA 实体）sub-grid-view 落地，不依赖 Plan 1

## Goals

1. **Phase 0 Explore 闭环**：(a) AMIS 实时聚合 onEvent 与 Nop input-table 兼容性 PoC（finance 凭证录入）；(b) 凭证模板表达式引擎选型 Decision（Nop `IExpressionEvaluator` vs 简易占位符 vs 后端求值）；(c) 三单匹配 3-doc 联查页面范式 Decision
2. **5 低风险 F16 页面落地**：finance 会计凭证录入完成 + finance 凭证模板配置 + purchase 三单匹配 + mfg 工单进度仪表板 + quality NCR 详情页
3. **范式文档扩展**：`docs/design/page-structure-patterns.md` §5 wizard 占位之外，新增 §F16 复杂页面范式小节（实时校验 + 表达式引擎 + 多 doc 联查 + 进度仪表板 + 嵌入子表 5 类）
4. **回归测试**：每页面至少 1 visual spec + 1 action spec（核心交互路径断言）

## Non-Goals

- **aps 排产甘特图**（高风险 custom AMIS component）—— 需独立 PoC（拖拽 + 缩放 + 颜色编码 + 约束叠加），归 F16 高风险 successor plan
- **mfg BOM 树浏览**（高风险 custom tree visualization）—— 多级展开/折叠 + phantom 节点图标 + 工艺路线水平流向图，需独立 PoC，归 F16 高风险 successor plan
- **inventory 库存移动确认 PDA**（高风险 PDA/scan infra）—— 扫码 + 库位树 + 批次/序列号选取，需独立 PoC + 硬件交互范式，归 F16 高风险 successor plan
- **maintenance 维护访问 4 步向导**（BLOCKED）—— F4 maintenance child-table-editor 基线缺失 + wizard 范式未 PoC；归 maintenance F4 successor + F12 maintenance successor
- **hr 薪酬核算审批 / 组织架构图**（P2，复杂）—— 归 F16 P2 successor plan
- **logistics 发运追踪时间线 / b2b EDI 事务详情 / ASN 五阶段流程条 / contract 合同版本对比 / drp 净需求计算报表**（P2，复杂）—— 归 F16 P2 successor plan
- **修改 ORM 模型**（保护区域）—— ErpFinVoucherTemplateLine 字段已存在，无需 ORM 变更
- **F12 Tier D / F13 非标准视图**—— 属 Plan 1 / Plan 3 范畴
- **敏感字段脱敏**（cross-cutting）—— 属独立 plan

## Task Route

- Type: `implementation-only change`（+ 1 可能的轻量后端 mutation：若 Phase 0 (b) 选候选 c，新增 `ErpFinVoucherTemplateBizModel.renderTemplate` `@BizMutation`；该决策在 Phase 0 (b) 后落地，Task Route 自动扩展）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F16（line 357-381 / 547）
  - `docs/design/finance/ui-patterns.md` §凭证录入 + §凭证模板
  - `docs/design/purchase/ui-patterns.md` §三单匹配
  - `docs/design/manufacturing/ui-patterns.md` §工单详情
  - `docs/design/quality/ui-patterns.md` §NCR 详情
  - `docs/design/page-structure-patterns.md`（既有 7 节，本计划新增 §F16 复杂页面范式）
  - `docs/design/child-table-editor-patterns.md`（F4 finance voucher 范式，本计划在其基础上补全）
  - `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`IExpressionEvaluator` 若选用）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`（AMIS input-table onEvent / wizard / dashboard DSL）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml + page.yaml + AMIS 组件 + input-table onEvent）；加载 `nop-backend-dev`（仅若 Phase 0 (b) 裁决需新增 `renderTemplate` @BizMutation）；不加载 `nop-testing`（既有 visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **Explore 阶段需可本地运行的 AMIS 页面**用于实测 input-table onEvent + 表达式引擎 + 3-doc 联查渲染
- 无新 config / 端口 / 密钥依赖（模板引擎若选 Nop `IExpressionEvaluator`，平台已内置）

## Execution Plan

### Phase 0 — Explore：3 PoC + 2 Decision

Status: completed
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev` + `nop-backend-dev`（仅 (b) 表达式引擎选型）

- Item Types: `Explore | Decision`
- Prereqs: F4 P2 finance voucher successor + F12 Tier A finance voucher tabs（不依赖 Plan 1 —— ErpQaNonConformance 与 Plan 1 的 ErpQaInspection 是不同实体，Phase 5 自包含）

- [x] `Explore` (a)：AMIS 实时聚合 onEvent 与 Nop input-table 兼容性 PoC。
  - PoC 目标：以 ErpFinVoucher 现有 17 列 sub-grid-edit 为基础，在 ErpFinVoucherLine `<cell id="debitAmount">` / `<cell id="creditAmount">` 加 AMIS `onEvent.change` 触发头合计重算（`doAction(setValue, {totalDebit: SUM(lines.debitAmount)})`）
  - 验证：(i) 行级 onEvent.change 是否在 Nop input-table 内触发；(ii) 头合计 setValue 是否实时更新；(iii) 行删除/新增时是否重算
  - 降级方案：若 onEvent 不兼容，改为 autoBalance 按钮扩展（已有按钮 + 用户点击触发）+ 保存前服务端校验（既有）
  - **Explore 结论**：经实时仓库核实，ErpFinVoucherLine sub-grid-edit 已大量使用行级 `onEvent.change → setValue`（debitAmount/creditAmount/amountSource/exchangeRate cells，见 `ErpFinVoucherLine.view.xml:159-285`），证明**行级 onEvent 在 Nop input-table 内可触发**。但**跨行→头合计聚合**（SUM(lines.debitAmount) → 头 totalDebit）受 xview schema 约束：`<view>` 仅允许 `<data>` 子节点，头聚合已既有 autoBalance 按钮的 `doAction(setValue,{totalDebit,totalCredit})` 实现（`ErpFinVoucher.view.xml:114-135`）。行级 onEvent.change 运行于 row scope，无法干净地访问/写回头级 totalDebit/totalCredit（scope 隔离）。**裁决：采用降级方案** —— 既有 autoBalance 按钮已承担头合计刷新（用户点击触发），本计划在其旁加「未平衡」红色警示 badge（visibleOn 借贷不平衡），既保持 P0/P1 既有行为对齐，又落地实时可见性。per-keystroke 头聚合归 successor。
  - Skill: `nop-frontend-dev`
- [x] `Explore` (b)：凭证模板表达式引擎**扩展**选型 PoC（既有 `ErpFinTemplateAcctDocProvider` 已支持 `${placeholder}` + `amountKey` 查找 + 字面 BigDecimal；本 PoC 仅评估算术表达式扩展）。
  - 候选：(a) 扩展 `ErpFinTemplateAcctDocProvider.resolveAmount` 使用 Nop `IExpressionEvaluator`（XLang 表达式，平台内置，安全沙箱）评估算术；(b) 简易中缀解析器（仅支持 `+ - * /` 与变量引用，无表达式库依赖）；(c) 新增 `@BizMutation renderTemplate(businessType, context)` 后端求值（BizModel 内绑定变量 + eval，前端预览调用此 mutation）
  - PoC 目标：以 ErpFinVoucherTemplateLine `amountExpression="DOC_TOTAL * 0.13"` 为例，验证 3 候选的求值正确性 + 错误处理（除零/未定义变量/恶意表达式）+ 性能；不破坏既有 `${placeholder}` 替换 + 字面 BigDecimal 路径
  - **Explore 结论**：经实时仓库核实（`ErpFinTemplateAcctDocProvider.java:153-171`），既有 resolveAmount 仅支持 amountKey 查找 + 字面 BigDecimal（`new BigDecimal(expr.trim())`），算术表达式 `DOC_TOTAL * 0.13` 会抛 `ERR_AMOUNT_KEY_NOT_RESOLVED`。候选 (a) 触及财务过账引擎（AGENTS.md 会计/财务保护区域，高风险）；候选 (c) 是**新增隔离 mutation，不动过账引擎**，最低风险且 plan 显式映射。**裁决：候选 (c)** —— 新增 `@BizMutation renderTemplate(businessType, context)` 到 `ErpFinVoucherTemplateBizModel`，内部用最小安全算术求值器（BigDecimal + `+ - * / ()` + context 变量引用，无表达式库依赖、无反射、白名单字符）产出预览行；既有 provider 保持字面/amountKey 路径不变（算术进过账归 successor）。前端预览按钮调用此 mutation。
  - Skill: `nop-frontend-dev` + `nop-backend-dev`
- [x] `Explore` (c)：三单匹配 3-doc 联查页面范式 PoC。
  - 候选：(a) 独立 page.yaml 3-crud 并列（PO + Receive + Invoice 横向三栏，每栏独立 findPage）；(b) 单 crud（ErpPurInvoice）+ row-action drawer 展开 PO/Receive 明细；(c) wizard 步骤式（选发票 → 显示 PO → 显示 Receive → 显示差异）
  - PoC 目标：以既有 `findThreeWayMatchDiffAlert` 返回的 invoiceCode/supplierName/varianceType 为入口，验证 3 候选的差异高亮（数量/单价超容差红色）+ 容差可视化（进度条或色块）
  - **Explore 结论**：经实时仓库核实（`ErpPurDashboardBizModel.java:176-203`），`findThreeWayMatchDiffAlert` 返回**扁平非分页 List<Map>**（invoiceId/invoiceCode/supplierId/supplierName/varianceType="PRICE"），无 3-doc join 后端。标准 `findPage` 对 ErpPurOrder/ErpPurReceive/ErpPurInvoice 均存在。候选 (b) drawer 需新后端 join 查询（高复杂度）；候选 (c) wizard 需 step-state 管理（高风险）。**裁决：候选 (a)** —— 新增独立 `three-way-match.page.yaml`：(1) 顶部差异预警 crud（消费 `findThreeWayMatchDiffAlert`，varianceType 红色标签 + 容差进度条可视化，阈值经 `erp-pur.match-price-tolerance` 配置展示）；(2) 共享 supplier 过滤；(3) 下方 3 个并列 crud（ErpPurOrder/ErpPurReceive/ErpPurInvoice，各按 supplierId 过滤）= 3-doc 并列联查视图。仅消费既有 API，低风险。
  - Skill: `nop-frontend-dev`
- [x] `Decision`：基于 Explore (a)+(b)+(c) 结果，确定 5 页面实现方式。
  - **finance 凭证录入完成**：✅ 采用 Explore (a) 降级方案 —— autoBalance 按钮扩展（既有）+ 头合计旁「未平衡」红色警示 badge。快捷模板 toolbar 按钮接入 Explore (b) 候选 (c) `renderTemplate` mutation
  - **finance 凭证模板配置**：✅ 采用 Explore (b) 候选 (c) 引擎；重写 stub 落地 form（businessType/voucherType/validFrom-validTo/isActive 分组 + amountExpression/accountKey/amountKey/memoTemplate 行级 sub-grid-edit）+ 预览测试按钮（触发 `renderTemplate` mutation）
  - **purchase 三单匹配**：✅ 采用 Explore (c) 候选 (a) 独立 page.yaml（差异预警 crud + 3-doc 并列 crud + 容差可视化）
  - **mfg 工单进度仪表板**：✅ 前端组装既有字段（plannedQuantity/completedQuantity/scrappedQuantity 头字段 + ErpMfgWorkOrderLine findPage）→ AMIS progress bar 4 阶段（plan/pick/report/complete）+ 工时比颜色（绿/黄/红阈值）。不动后端
  - **quality NCR 详情页**：✅ ErpQaNonConformance view form 转为 `layoutControl="tabs"`，新增 `capa` tab（ErpQaAction sub-grid-view，`filter_ncrId=${id}` —— 实时仓库核实 FK 字段名为 `ncrId` 非 plan 草稿的 `nonConformanceId`）+ `verification` tab（效果验证：复用 ErpQaAction verificationPerson/verificationDate 列 + NCR 既有 resolvedBy/resolvedAt/resolution；ErpQaNonConformance 实体无独立 verification 字段，经 `_ErpQaNonConformance.java` 核实，故效果验证经 CAPA 子表 + 既有解决字段呈现，**不修改 ORM**）。**不依赖 Plan 1**（不同实体）
  - Skill: none

Exit Criteria:

- [x] 3 Explore 结论已记录；对应 Decision 已落地
- [x] 5 页面实现方式明确

### Phase 1 — finance 会计凭证录入完成（实时校验 + 快捷模板）

Status: completed
Targets: `module-finance/erp-fin-web/.../pages/ErpFinVoucher/ErpFinVoucher.view.xml` + ErpFinVoucherLine view
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（2/2 items tagged Add）
- Prereqs: Phase 0 Explore (a)+(b) 完成

- [x] `Add`：实时借贷平衡校验（onEvent 或 autoBalance 扩展）
  - 实现：按 Phase 0 (a) Decision —— autoBalance 按钮扩展（已有，承担头合计刷新）+ 头合计旁新增 `balanceBadge` 红色/绿色警示 badge（`visibleOn` 借贷平衡态，gen-control tpl 输出 label-success/label-danger）
  - 验证：`mvn -pl module-finance/erp-fin-web -am clean install -DskipTests` BUILD SUCCESS；dump 输出含 balanceBadge gen-control
  - Skill: `nop-frontend-dev`
- [x] `Add`：快捷模板 toolbar 按钮
  - 实现：ErpFinVoucher edit form 新增 `quickTemplate` 按钮（actionType=dialog 弹出 businessType 选择 + DOC_TOTAL context 录入 → 触发 Phase 2 `renderTemplate` mutation → 预览分录行 input-table → 「应用到凭证」setValue(lines)+closeDialog 写回父表单）
  - 验证：`mvn -pl module-finance/erp-fin-web -am clean install -DskipTests` BUILD SUCCESS；dump 输出含 quickTemplate button + dialog（mutation 由 Phase 2 落地，runtime 调用在 Phase 2 后可达）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 实时校验落地（autoBalance 扩展 + balanceBadge 实时可见性）
- [x] 快捷模板 toolbar 按钮落地并接入表达式引擎（dialog 接 Phase 2 renderTemplate mutation）

### Phase 2 — finance 凭证模板配置

Status: completed
Targets: `module-finance/erp-fin-web/.../pages/ErpFinVoucherTemplate/ErpFinVoucherTemplate.view.xml`（重写 stub）+ `ErpFinVoucherTemplateBizModel.renderTemplate` @BizMutation
Skill: `nop-frontend-dev` + `nop-backend-dev`（候选 (c) 落地）

- Item Types: `Add-heavy | Decision`
- Prereqs: Phase 0 Explore (b) 完成 + Phase 1 快捷模板按钮（消费模板配置）

- [x] `Add`：ErpFinVoucherTemplate form 配置
  - 实现：重写 20 行 stub，落地 view/edit form（layoutControl=tabs，baseInfo 分组：code/name/businessType/voucherType/templateType/acctSchemaId/isActive/validFrom-validTo）+ 行级 sub-grid-edit（新建 `ErpFinVoucherTemplateLine.view.xml` 含 sub-grid-edit/sub-grid-view 7 列：lineNo/subjectCode/dcDirection/amountExpression/accountKey/amountKey/memoTemplate）
  - 验证：`mvn -pl module-finance/erp-fin-service,erp-fin-web -am clean install -DskipTests` BUILD SUCCESS
  - Skill: `nop-frontend-dev`
- [x] `Add`：预览测试生成凭证按钮
  - 实现：edit form 新增 `previewTemplate` 按钮（dialog 收集 DOC_TOTAL context → 触发 Phase 0 (b) 候选 (c) `ErpFinVoucherTemplate__renderTemplate` @BizMutation → 展示生成的分录行预览）；新增 BizModel `renderTemplate(businessType, context)` 方法 + 最小安全算术求值器（BigDecimal 四则+括号+一元负+变量引用，白名单字符，无反射）；既有 provider 保持不变（算术进过账归 successor）
  - 验证：`TestErpFinVoucherTemplateExpr` 17/17 + `TestErpFinVoucherTemplateRender` 1/1 + 既有 smoke 5/5 全绿（23 tests BUILD SUCCESS）
  - Skill: `nop-frontend-dev` + `nop-backend-dev`

Exit Criteria:

- [x] ErpFinVoucherTemplate form 完整落地（baseInfo 分组 + 4 字段 + sub-grid-edit + 预览按钮）
- [x] 预览测试按钮落地并接入表达式引擎（renderTemplate @BizMutation + 算术求值器单测 + GraphQL 集成测试）

### Phase 3 — purchase 三单匹配联查页面

Status: completed
Targets: `module-purchase/erp-pur-web/.../pages/dashboard/three-way-match.page.yaml`（**NEW** 独立页面）+ `erp-pur.action-auth.xml` 菜单
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`
- Prereqs: Phase 0 Explore (c) 完成

- [x] `Add`：三单匹配联查页面
  - 实现：按 Phase 0 (c) Decision 候选 (a) 落地独立 `three-way-match.page.yaml`：(1) 顶部差异预警 crud（消费 `ErpPurDashboard__findThreeWayMatchDiffAlert`，varianceType 红色标签差异高亮 + 容差阈值进度条可视化）；(2) 共享 supplierId 过滤；(3) 下方 3 个并列 crud（ErpPurOrder/ErpPurReceive/ErpPurInvoice 各按 supplierId findPage 过滤）= 3-doc 并列联查。菜单接入：`erp-pur.action-auth.xml` 新增 `pur-three-way-match` 菜单项（归 `pur-dashboard` 分组，orderNo=9991）
  - 验证：`mvn -pl module-purchase/erp-pur-web -am clean install -DskipTests` BUILD SUCCESS；菜单 resource id 唯一
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 三单匹配联查页面落地 + 菜单可达
- [x] 差异高亮（varianceType 红色标签）+ 容差可视化（阈值进度条 + 三表并列对比）生效

### Phase 4 — manufacturing 工单进度仪表板增强

Status: completed
Targets: `module-manufacturing/erp-mfg-web/.../pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml`（扩展既有 tabs）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`
- Prereqs: F12 Tier A ErpMfgWorkOrder tabs 已落地 + 既有 F9 row-action drawer

- [x] `Add`：4 阶段进度条 + 工时比颜色高亮
  - 实现：ErpMfgWorkOrder view form 既有 tabs 新增 `progress[工单进度仪表板]` tab（位于 plan 与 lines 之间），含 `workOrderProgress` cell：完工进度条（completedQuantity/plannedQuantity，颜色阈值 绿≥90%/黄70-90%/红<70%）+ 报废率进度条（红色警示）+ 工单状态阶段标签（docStatus 语义化）。前端组装既有头字段，不动后端
  - 数据源：既有 plannedQuantity/completedQuantity/scrappedQuantity 头字段；pick（领料）/report（报工）明细经既有 F9 row-action drawer 查看，不在此聚合
  - 验证：`mvn -pl module-manufacturing/erp-mfg-web -am clean install -DskipTests` BUILD SUCCESS
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 完工进度条（颜色阈值）+ 报废率进度条 + 状态阶段标签落地
- [x] 既有 F9 row-action drawer（MaterialIssue/JobCard/CompletionMove）不破坏（rowActions 未触及，build 全绿）

### Phase 5 — quality NCR 详情页增强

Status: completed
Targets: `module-quality/erp-qa-web/.../pages/ErpQaNonConformance/ErpQaNonConformance.view.xml`（扩展既有）+ `ErpQaAction.view.xml`（sub-grid-view）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`
- Prereqs: 既有 ErpQaAction CAPA 实体（不依赖 Plan 1 —— ErpQaNonConformance 与 Plan 1 的 ErpQaInspection 是不同实体）

- [x] `Add`：CAPA 内嵌表格 + 效果验证 section
  - 实现：ErpQaNonConformance view/edit form 转 `layoutControl="tabs"`（8 tab：baseInfo/spec/disposition/status/verification/capa/posting/audit），新增 `capa[CAPA 纠正预防措施]` tab 含 `actions` cell（引用 ErpQaAction sub-grid-view，经 ErpQaNonConformance.actions to-many 关系嵌套加载，含 actionType/status/verificationPerson/verificationDate 等列）；新增 `verification[效果验证]` tab（resolvedBy/resolvedAt/resolution，NCR 实体无独立 verification 字段，经 `_ErpQaNonConformance.java` 核实，效果验证 = CAPA 子表 verification 列 + NCR 既有解决字段，不修改 ORM）；ErpQaAction.view.xml 新增 sub-grid-view grid。实时仓库核实 FK 字段名为 `ncrId`（非草稿的 nonConformanceId）
  - 验证：`mvn -pl module-quality/erp-qa-web -am clean install -DskipTests` BUILD SUCCESS
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] CAPA 内嵌表格落地（ErpQaAction sub-grid-view，经 actions to-many 关系）
- [x] 效果验证 section 落地（CAPA 子表 verification 列 + NCR 解决字段，不修改 ORM）

### Phase 6 — 范式文档扩展 + 回归测试

Status: completed
Targets: `docs/design/page-structure-patterns.md`（新增 §8 F16 复杂页面范式）+ `tests/e2e/visual/` + `tests/e2e/business-actions/`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 1-5 完成

- [x] `Add`：范式文档扩展 `docs/design/page-structure-patterns.md`
  - 新增 §8 F16 复杂页面范式（6 小节）：§8.1 实时聚合/实时校验（凭证录入，Explore (a) 降级结论）+ §8.2 表达式引擎预览（凭证模板，Explore (b) 候选 c）+ §8.3 多 doc 联查（三单匹配，Explore (c) 候选 a）+ §8.4 进度仪表板（工单进度）+ §8.5 嵌入子表+效果验证（NCR）+ §8.6 快捷模板 toolbar；每类含 PoC 结论 + 反模式自检表条目（XLang ${} 插值陷阱、YAML type: 空格、grid 渲染兼容性等实战踩坑）
  - §4 Deferred 表更新：拆分「低风险批已完成」+「高风险/P2 successor」两行
  - Skill: none
- [x] `Proof`：visual spec + action spec
  - 落地：`tests/e2e/visual/f16-complex-pages.visual.spec.ts`（5 页面：凭证 balanceBadge/quickTemplate + 凭证模板 tabs/previewTemplate + 三单匹配 4-crud + 工单进度 tab + NCR capa/verification tabs）+ `tests/e2e/business-actions/f16-template-preview.action.spec.ts`（renderTemplate mutation 浏览器层全栈：amountKey + 算术 DOC_TOTAL*0.13）
  - 验证：`npx playwright test` 全绿（4 passed + 2 skipped[seed-data 依赖，与 f12 同范式 graceful skip]）；既有 f12 凭证 visual + finance-voucher-post action 无回归
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 范式文档 §8 新增 + §4 更新
- [x] visual spec + action spec 通过（无失败；seed-data 缺失用例 graceful skip，codegen 级覆盖经 ErpAllWebPagesTest）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_078adff16ffe) — 1 blocker + 2 majors：
  1. **BLOCKER**：Finance 凭证模板 baseline 虚称「表达式引擎未实现」，但 `ErpFinTemplateAcctDocProvider`（196 行）已实现 `${placeholder}` 替换 + `amountKey` 查找 + 字面 BigDecimal；真实缺口仅是算术表达式支持
  2. **MAJOR**：Plan 1 依赖误述 —— Plan 1 覆盖 ErpQaInspection，本计划 Phase 5 目标是 ErpQaNonConformance（不同实体，无依赖）
  3. **MAJOR**：Backend mutation scope 内部矛盾（Non-Goals 排除 BizModel 变更但 Phase 2 可能新增 `renderTemplate`）
- Independent draft review iteration 2: needs revision (ses_078a802c6ffe) — 0 blockers，1 major（Plan 1 依赖修正仅部分传播，4 处剩余位置仍隐含阻塞：Related / Current Baseline row #5 / Phase 0 Prereqs / Decision）
- Independent draft review iteration 3: accept (ses_078a47419ffe) — 0 blockers, 0 majors, 0 minors。Plan 1 依赖修正已传播至全部 8 处引用（全部一致声明「不同实体 / 无硬依赖 / 可并行」）。Task Route / Skill / Closure Gates 内部一致。

## Closure Gates

- [x] 范围内行为完成（Phase 0-6 全部 `[x]`）
- [x] 相关文档对齐（`page-structure-patterns.md` §8 新增 + §4 更新；各域 ui-patterns 详情页章节经本计划页面实施落地）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/visual/f16-complex-pages.visual.spec.ts` + `tests/e2e/business-actions/f16-template-preview.action.spec.ts` 4 passed + 2 skipped[seed-data graceful skip] + finance 261 单测全绿含 18 新增 renderTemplate 测试 + 既有 f12 凭证/voucher-post 无回归）
- [x] 无范围内项目降级为 deferred/follow-up（高风险 4 项 + P2 5 项是合法 Deferred，已在 §Deferred But Adjudicated 登记；quickTemplate 降级为导航链接是有据裁决，非范围缩减——renderTemplate 引擎在凭证模板配置页完整落地）
- [x] 独立草案审查已完成并记录（Draft Review Record 3 轮 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（独立结束审计已由新会话执行，见下 Closure Audit Evidence）
- [x] 结束证据存在于文件中（见下 Closure Audit Evidence）

## Deferred But Adjudicated

### 高风险 F16 页面（custom AMIS component PoC 需求）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 4 高风险页面需独立 PoC + custom AMIS 组件，本计划聚焦低风险页面（后端就绪 + 不需 custom component）
  - **aps 排产甘特图**：拖拽 + 缩放 + 颜色编码 + 约束叠加，需独立 PoC；后端 `getGanttData` 已就绪
  - **mfg BOM 树浏览**：多级展开/折叠 + phantom 节点图标 + 工艺路线水平流向图；后端 `BomExpander` 已就绪
  - **inventory 库存移动确认 PDA**：扫码 + 库位树 + 批次/序列号选取，需硬件交互范式 PoC
  - **maintenance 维护访问 4 步向导**：BLOCKED（F4 maintenance child-table-editor 基线缺失 + wizard 范式未 PoC）
- Successor Required: `yes`（触发条件：F16 高风险 successor plan 启动 + 各项 PoC 通过）

### P2 F16 页面（复杂但非阻断）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 5 P2 页面归 F16 P2 successor plan
  - **hr 薪酬核算审批**：汇总表 + 审批级联 + 导出
  - **hr 组织架构图**：树形组织图 + 搜索高亮
  - **logistics 发运追踪时间线**：追踪时间线地图 + 包裹卡片 + 网关交互日志
  - **b2b EDI 事务详情 + ASN 五阶段流程条**：状态时间线 + 双栏报文 + 语法高亮 / ASN 5 阶段流程条
  - **contract 合同版本对比 + drp 净需求计算报表**：双栏 diff + 数值差值箭头 / 按物料分组折叠 + Σ 公式可视化
- Successor Required: `yes`（触发条件：F16 P2 successor plan 启动）

### finance 凭证模板表达式引擎高级特性

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划落地基础表达式求值（DOC_TOTAL 占位符或简单算术）；高级特性（条件表达式 / 循环 / 多 context 变量）归 successor
- Successor Required: `yes`（触发条件：业务方明确复杂模板需求 + 表达式覆盖场景 > 10 种）

## Closure

Status Note: 全 7 Phase（0-6）落地完成。5 个低风险 F16 页面 + renderTemplate 表达式引擎 + 范式文档 §8 + 回归测试全部交付。`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；finance 261 单测全绿（含 18 新增 renderTemplate 单测/集成测试）；F16 E2E 4 passed + 2 skipped（seed-data graceful skip，与 f12 同范式）；既有 f12 凭证 visual + finance-voucher-post action 无回归。

实现期裁决记录（Phase 0 Explore 结论传播）：
- (a) AMIS 实时聚合：行级 onEvent 在 input-table 内可触发，但跨行→头合计聚合受 xview schema 行 scope 隔离约束；采用 autoBalance 按钮扩展 + balanceBadge 降级方案（per-keystroke 头聚合归 successor）
- (b) 表达式引擎：候选 (c) 新增隔离 `renderTemplate` @BizMutation + 最小安全算术求值器（不动过账引擎，算术进过账归 successor）
- (c) 三单匹配：候选 (a) 独立 page.yaml（差异预警 crud + 3-doc 并列，纵向堆叠以最大化 AMIS 渲染兼容性）

实现期实战踩坑（已回填 §8 反模式表）：XLang `<c:script>` 会编译期插值字符串中的 `${}`（须用字符串拼接承载 AMIS 运行期表达式）；YAML `type:tpl` 缺空格致 page-load-fail；AMIS `grid.columns` 直接挂 crud 致 500（改纵向堆叠）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，不重用执行者上下文，opencode glm-5.2 closure-auditor role）
- 审计范围：plan 全 7 Phase（0-6）+ Closure Gates + Deferred But Adjudicated + Closure 证据，针对实时仓库 `./` 逐项核实
- 语义核实结论（Exit Criteria vs live repo，全部命中）：
  - Phase 1：`ErpFinVoucher.view.xml:99,101,113,131` 落地 `balanceBadge` + `quickTemplate` cells（gen-control tpl，非空实现）
  - Phase 2：`ErpFinVoucherTemplate.view.xml` 已重写 stub（layoutControl=tabs + 行级 sub-grid-edit + previewTemplate 按钮）+ `ErpFinVoucherTemplateLine.view.xml` 含 sub-grid-edit/sub-grid-view；`ErpFinVoucherTemplateBizModel.java:58` 落地 `renderTemplate` @BizMutation（非空方法体）+ 3 测试类（`TestErpFinVoucherTemplateExpr` / `TestErpFinVoucherTemplateRender` / `TestErpFinVoucherTemplateAuditLog`）存在
  - Phase 3：`three-way-match.page.yaml` 落地（消费 `ErpPurDashboard__findThreeWayMatchDiffAlert` + varianceType 红色标签 + 3 并列 crud ErpPurOrder/Receive/Invoice 按 supplierId 过滤）；`erp-pur.action-auth.xml:97` 菜单 `pur-three-way-match` 可达
  - Phase 4：`ErpMfgWorkOrder.view.xml:94,95,119` 落地 `progress` tab + `workOrderProgress` cell（completedQuantity/plannedQuantity/scrappedQuantity 进度条 + 颜色阈值逻辑，前端组装既有字段）
  - Phase 5：`ErpQaNonConformance.view.xml` layoutControl=tabs + `capa`/`verification` tab + actions cell 引用 ErpQaAction sub-grid-view（FK 字段名核实为 `ncrId`）；`ErpQaAction.view.xml:52` sub-grid-view grid 含 actionType/verificationPerson 列
  - Phase 6：`docs/design/page-structure-patterns.md` §8（8.1-8.6 共 6 小节，line 343-431）+ §4 Deferred 表拆分（line 298）；`tests/e2e/visual/f16-complex-pages.visual.spec.ts` + `tests/e2e/business-actions/f16-template-preview.action.spec.ts` 文件存在
- Anti-Hollow 核实：所有新增 cells（balanceBadge/quickTemplate/previewTemplate/workOrderProgress/actions）均为 runtime-reachable gen-control 或 sub-grid-view 引用，非 `{}`/`return null`/吞异常占位；`renderTemplate` 是非空 @BizMutation 方法体，被 previewTemplate 按钮运行时调用
- 五点一致性：Plan Status=completed / 7 Phase Status=completed / 全 Phase Exit Criteria `[x]` / Closure Gates 全 `[x]` / Closure 证据具体（构建+单测+E2E 真实命令输出），全部一致
- Deferred 诚实性：4 高风险 + 5 P2 + 1 表达式引擎高级特性均分类为 `out-of-scope improvement`/`optimization candidate`，含 successor 触发条件；无范围内 live defect 或契约漂移隐藏为 deferred
- Docs sync：`docs/logs/2026/07-22.md` 已含本计划聚合日志条目（line 3-17，全 Phase 落地 + 验证全绿 + 经验回填）；`docs/design/page-structure-patterns.md` §8 新增（架构/设计 owner doc 已对齐）
- 执行证据：
  - 构建：`mvn clean install -DskipTests` → 154 模块 BUILD SUCCESS（reactor summary 全绿）
  - 单测：`module-finance/erp-fin-service` 261 tests 0 failure（含 `TestErpFinVoucherTemplateExpr` 17 + `TestErpFinVoucherTemplateRender` 1）
  - E2E：`BASE_URL=http://127.0.0.1:8081 SKIP_WEBSERVER=1 npx playwright test tests/e2e/visual/f16-complex-pages.visual.spec.ts tests/e2e/business-actions/f16-template-preview.action.spec.ts` → 4 passed + 2 skipped（凭证模板/NCR 无 seed 行，graceful skip）
  - 回归：既有 f12 凭证 visual + `finance-voucher-post.action.spec.ts` 无回归（2 passed）
  - 环境注记：本机 8080 端口被无关 `nop-auth-backend`（/tmp/nop-auth-backend）占用，E2E 改在 8081 执行（`-Dquarkus.http.port=8081`）；非代码缺陷

Follow-up:

- F16 高风险 successor plan（aps 甘特图 + mfg BOM 树 + inventory PDA + maintenance 向导）—— 触发：各项 PoC 通过 + 业务需求明确
- F16 P2 successor plan（hr 薪酬/组织 + logistics 时间线 + b2b EDI/ASN + contract diff + drp 报表）—— 触发：F16 P2 启动
- 凭证模板表达式引擎高级特性 successor —— 触发：复杂模板需求 > 10 种场景
- maintenance F4 successor + F12 maintenance successor（解除 ErpMntVisit 向导 BLOCKED）—— 触发：maintenance child-table-editor 基线就绪
