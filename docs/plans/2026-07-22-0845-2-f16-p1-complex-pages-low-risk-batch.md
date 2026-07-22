# 2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch F16 P1 复杂手写页面低风险批

> Plan Status: active
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

Status: planned
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev` + `nop-backend-dev`（仅 (b) 表达式引擎选型）

- Item Types: `Explore | Decision`
- Prereqs: F4 P2 finance voucher successor + F12 Tier A finance voucher tabs（不依赖 Plan 1 —— ErpQaNonConformance 与 Plan 1 的 ErpQaInspection 是不同实体，Phase 5 自包含）

- [ ] `Explore` (a)：AMIS 实时聚合 onEvent 与 Nop input-table 兼容性 PoC。
  - PoC 目标：以 ErpFinVoucher 现有 17 列 sub-grid-edit 为基础，在 ErpFinVoucherLine `<cell id="debitAmount">` / `<cell id="creditAmount">` 加 AMIS `onEvent.change` 触发头合计重算（`doAction(setValue, {totalDebit: SUM(lines.debitAmount)})`）
  - 验证：(i) 行级 onEvent.change 是否在 Nop input-table 内触发；(ii) 头合计 setValue 是否实时更新；(iii) 行删除/新增时是否重算
  - 降级方案：若 onEvent 不兼容，改为 autoBalance 按钮扩展（已有按钮 + 用户点击触发）+ 保存前服务端校验（既有）
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (b)：凭证模板表达式引擎**扩展**选型 PoC（既有 `ErpFinTemplateAcctDocProvider` 已支持 `${placeholder}` + `amountKey` 查找 + 字面 BigDecimal；本 PoC 仅评估算术表达式扩展）。
  - 候选：(a) 扩展 `ErpFinTemplateAcctDocProvider.resolveAmount` 使用 Nop `IExpressionEvaluator`（XLang 表达式，平台内置，安全沙箱）评估算术；(b) 简易中缀解析器（仅支持 `+ - * /` 与变量引用，无表达式库依赖）；(c) 新增 `@BizMutation renderTemplate(businessType, context)` 后端求值（BizModel 内绑定变量 + eval，前端预览调用此 mutation）
  - PoC 目标：以 ErpFinVoucherTemplateLine `amountExpression="DOC_TOTAL * 0.13"` 为例，验证 3 候选的求值正确性 + 错误处理（除零/未定义变量/恶意表达式）+ 性能；不破坏既有 `${placeholder}` 替换 + 字面 BigDecimal 路径
  - Skill: `nop-frontend-dev` + `nop-backend-dev`
- [ ] `Explore` (c)：三单匹配 3-doc 联查页面范式 PoC。
  - 候选：(a) 独立 page.yaml 3-crud 并列（PO + Receive + Invoice 横向三栏，每栏独立 findPage）；(b) 单 crud（ErpPurInvoice）+ row-action drawer 展开 PO/Receive 明细；(c) wizard 步骤式（选发票 → 显示 PO → 显示 Receive → 显示差异）
  - PoC 目标：以既有 `findThreeWayMatchDiffAlert` 返回的 invoiceCode/supplierName/varianceType 为入口，验证 3 候选的差异高亮（数量/单价超容差红色）+ 容差可视化（进度条或色块）
  - Skill: `nop-frontend-dev`
- [ ] `Decision`：基于 Explore (a)+(b)+(c) 结果，确定 5 页面实现方式。
  - **finance 凭证录入完成**：若 Explore (a) PoC 通过 → 行级 onEvent.change 实时聚合；否则 autoBalance 按钮扩展。快捷模板 toolbar 按钮接入 Explore (b) 表达式引擎
  - **finance 凭证模板配置**：采用 Explore (b) 裁决的引擎；form 配置 amountExpression/accountKey/amountKey 字段 + 预览测试按钮（触发 `renderTemplate` 若选候选 c）
  - **purchase 三单匹配**：采用 Explore (c) 裁决的页面范式
  - **mfg 工单进度仪表板**：前端组装既有 `@BizQuery` 阶段聚合（planQty/reportQty/completeQty）→ AMIS progress bar + 工时比颜色（绿/黄/红阈值）
  - **quality NCR 详情页**：在 ErpQaNonConformance view form 既有 6 group（baseInfo/spec/disposition/status/posting/audit）基础上，新增 CAPA 内嵌 sub-grid-view（ErpQaAction `findPage` filter_nonConformanceId）+ 效果验证 section。**不依赖 Plan 1**（ErpQaNonConformance 与 Plan 1 的 ErpQaInspection 是不同实体；Plan 1 Tier D 不触及 ErpQaNonConformance.view.xml）
  - Skill: none

Exit Criteria:

- [ ] 3 Explore 结论已记录；对应 Decision 已落地
- [ ] 5 页面实现方式明确

### Phase 1 — finance 会计凭证录入完成（实时校验 + 快捷模板）

Status: planned
Targets: `module-finance/erp-fin-web/.../pages/ErpFinVoucher/ErpFinVoucher.view.xml` + ErpFinVoucherLine view
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（2/2 items tagged Add）
- Prereqs: Phase 0 Explore (a)+(b) 完成

- [ ] `Add`：实时借贷平衡校验（onEvent 或 autoBalance 扩展）
  - 实现：按 Phase 0 (a) Decision —— 若 onEvent 兼容，在 ErpFinVoucherLine debitAmount/creditAmount cell 加 `onEvent.change` 触发头合计 setValue；若不兼容，扩展 autoBalance 按钮（已有）+ 加「未平衡」红色警示 badge 在头合计旁
  - 验证：dump 输出含 onEvent 或 badge；本地浏览器输入金额 → 头合计实时更新或 badge 显示
  - Skill: `nop-frontend-dev`
- [ ] `Add`：快捷模板 toolbar 按钮
  - 实现：ErpFinVoucher view.xml toolbar 新增 `quickTemplate` 按钮（dialog 弹出 ErpFinVoucherTemplate 选择器 → 选中后触发 `renderTemplate` 填充分录行）；接入 Phase 0 (b) 表达式引擎
  - 验证：dump 输出含 quickTemplate button + dialog；本地浏览器点击 → 弹出模板选择 → 选中 → 分录行自动填充
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 实时校验落地（onEvent 或 autoBalance 扩展）
- [ ] 快捷模板 toolbar 按钮落地并接入表达式引擎

### Phase 2 — finance 凭证模板配置

Status: planned
Targets: `module-finance/erp-fin-web/.../pages/ErpFinVoucherTemplate/ErpFinVoucherTemplate.view.xml`（重写 stub）
Skill: `nop-frontend-dev` + `nop-backend-dev`（若选候选 c）

- Item Types: `Add-heavy | Decision`（若需后端 mutation）
- Prereqs: Phase 0 Explore (b) 完成 + Phase 1 快捷模板按钮（消费模板配置）

- [ ] `Add`：ErpFinVoucherTemplate form 配置
  - 实现：重写 20 行 stub，落地 form view/edit/add 含 businessType 分组 + amountExpression/accountKey/amountKey/memoTemplate 字段编辑 + 行级 sub-grid-edit（ErpFinVoucherTemplateLine）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：预览测试生成凭证按钮
  - 实现：toolbar 新增 `previewTemplate` 按钮（dialog 收集测试 context如 DOC_TOTAL=1000 → 触发表达式引擎求值 → 预览生成的分录行 debit/credit/memo）
  - 若 Phase 0 (b) 选候选 c（后端求值）：新增 `@BizMutation renderTemplate(businessType, context)` @BizMutation 到 ErpFinVoucherTemplateBizModel + 测试
  - Skill: `nop-frontend-dev` + `nop-backend-dev`

Exit Criteria:

- [ ] ErpFinVoucherTemplate form 完整落地（businessType 分组 + 4 字段 + sub-grid-edit）
- [ ] 预览测试按钮落地并接入表达式引擎

### Phase 3 — purchase 三单匹配联查页面

Status: planned
Targets: `module-purchase/erp-pur-web/.../pages/dashboard/three-way-match.page.yaml`（**NEW** 独立页面）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`
- Prereqs: Phase 0 Explore (c) 完成

- [ ] `Add`：三单匹配联查页面
  - 实现：按 Phase 0 (c) Decision 落地独立 page.yaml（候选 a 3-crud 并列 / 候选 b 单 crud + drawer / 候选 c wizard）。消费既有 `ErpPurDashboard__findThreeWayMatchDiffAlert`。差异高亮（数量/单价超容差红色，容差阈值 configurable）+ 容差可视化（进度条或色块）
  - 菜单接入：`erp-pur.action-auth.xml` 新增 `pur-three-way-match` 菜单项（归既有 `pur-dashboard` 分组或新增 `pur-match` 分组）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 三单匹配联查页面落地 + 菜单可达
- [ ] 差异高亮 + 容差可视化生效

### Phase 4 — manufacturing 工单进度仪表板增强

Status: planned
Targets: `module-manufacturing/erp-mfg-web/.../pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml`（扩展既有 tabs）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`
- Prereqs: F12 Tier A ErpMfgWorkOrder tabs 已落地 + 既有 F9 row-action drawer

- [ ] `Add`：4 阶段进度条 + 工时比颜色高亮
  - 实现：在 ErpMfgWorkOrder view form 既有 tabs 内（F12 Tier A 6 tab：baseInfo/bom/plan/lines/cost/audit），baseInfo tab 顶部或新增 `progress` tab 插入 AMIS progress bar 组件（4 阶段：plan/pick/report/complete，每阶段百分比 = stageQty/totalQty）+ 工时比颜色（绿≥90% / 黄 70-90% / 红<70%，阈值 configurable）
  - 数据源：前端组装既有 ErpMfgWorkLine/ErpMfgJobCard `findPage` 聚合，或既有 `@BizQuery` 若已暴露阶段聚合
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 4 阶段进度条 + 工时比颜色高亮落地
- [ ] 既有 F9 row-action drawer（MaterialIssue/JobCard/CompletionMove）不破坏

### Phase 5 — quality NCR 详情页增强

Status: planned
Targets: `module-quality/erp-qa-web/.../pages/ErpQaNonConformance/ErpQaNonConformance.view.xml`（扩展既有）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`
- Prereqs: 既有 ErpQaAction CAPA 实体（不依赖 Plan 1 —— ErpQaNonConformance 与 Plan 1 的 ErpQaInspection 是不同实体）

- [ ] `Add`：CAPA 内嵌表格 + 效果验证 section
  - 实现：ErpQaNonConformance view form 新增 `capa` section（或 tab，若该 view 已有 tabs）含 `<cell id="capaActions">` 引用 ErpQaAction sub-grid-view（`findPage` filter_nonConformanceId）；新增 `verification` section 含效果验证字段（verificationResult/verifiedAt/verifiedBy）
  - 本计划自行落地 ErpQaAction sub-grid-view（轻量只读 grid），不依赖 Plan 1
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] CAPA 内嵌表格落地（ErpQaAction sub-grid-view）
- [ ] 效果验证 section 落地

### Phase 6 — 范式文档扩展 + 回归测试

Status: planned
Targets: `docs/design/page-structure-patterns.md`（新增 §F16 复杂页面范式）+ `tests/e2e/visual/` + `tests/e2e/business-actions/`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 1-5 完成

- [ ] `Add`：范式文档扩展 `docs/design/page-structure-patterns.md`
  - 新增 §8 F16 复杂页面范式（5 类）：实时聚合 onEvent + 表达式引擎 + 多 doc 联查 + 进度仪表板 + 嵌入子表；每类含 PoC 结论 + 反模式自检表条目
  - §4 Deferred 表更新：移除本计划完成的 5 项；保留高风险 successor（甘特图/BOM 树/PDA/维护向导）+ P2 successor（hr/logistics/b2b/contract/drp）
  - Skill: none
- [ ] `Proof`：visual spec + action spec
  - 落地：`tests/e2e/visual/f16-complex-pages.visual.spec.ts`（**NEW**）覆盖 5 页面（每页面 1 用例：渲染断言 + 核心交互 DOM 断言）；`tests/e2e/business-actions/f16-template-preview.action.spec.ts`（**NEW**）覆盖凭证模板预览交互
  - 验证：全 PASS（base_url=http://127.0.0.1:8080, SKIP_WEBSERVER=1）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 范式文档 §8 新增 + §4 更新
- [ ] visual spec + action spec 通过

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_078adff16ffe) — 1 blocker + 2 majors：
  1. **BLOCKER**：Finance 凭证模板 baseline 虚称「表达式引擎未实现」，但 `ErpFinTemplateAcctDocProvider`（196 行）已实现 `${placeholder}` 替换 + `amountKey` 查找 + 字面 BigDecimal；真实缺口仅是算术表达式支持
  2. **MAJOR**：Plan 1 依赖误述 —— Plan 1 覆盖 ErpQaInspection，本计划 Phase 5 目标是 ErpQaNonConformance（不同实体，无依赖）
  3. **MAJOR**：Backend mutation scope 内部矛盾（Non-Goals 排除 BizModel 变更但 Phase 2 可能新增 `renderTemplate`）
- Independent draft review iteration 2: needs revision (ses_078a802c6ffe) — 0 blockers，1 major（Plan 1 依赖修正仅部分传播，4 处剩余位置仍隐含阻塞：Related / Current Baseline row #5 / Phase 0 Prereqs / Decision）
- Independent draft review iteration 3: accept (ses_078a47419ffe) — 0 blockers, 0 majors, 0 minors。Plan 1 依赖修正已传播至全部 8 处引用（全部一致声明「不同实体 / 无硬依赖 / 可并行」）。Task Route / Skill / Closure Gates 内部一致。

## Closure Gates

- [ ] 范围内行为完成（Phase 0-6 全部 `[x]`）
- [ ] 相关文档对齐（`page-structure-patterns.md` §8 新增 + 各域 ui-patterns.md 详情页章节实施记录）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/visual/f16-complex-pages.visual.spec.ts` + `tests/e2e/business-actions/f16-template-preview.action.spec.ts` 全 PASS + 既有核心域 E2E 无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（高风险 4 项 + P2 5 项是合法 Deferred，已在 §Deferred But Adjudicated 登记）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待执行后填写独立结束审计证据>

Follow-up:

- F16 高风险 successor plan（aps 甘特图 + mfg BOM 树 + inventory PDA + maintenance 向导）—— 触发：各项 PoC 通过 + 业务需求明确
- F16 P2 successor plan（hr 薪酬/组织 + logistics 时间线 + b2b EDI/ASN + contract diff + drp 报表）—— 触发：F16 P2 启动
- 凭证模板表达式引擎高级特性 successor —— 触发：复杂模板需求 > 10 种场景
- maintenance F4 successor + F12 maintenance successor（解除 ErpMntVisit 向导 BLOCKED）—— 触发：maintenance child-table-editor 基线就绪
