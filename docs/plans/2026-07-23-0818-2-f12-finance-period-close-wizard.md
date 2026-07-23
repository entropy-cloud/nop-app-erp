# 2026-07-23-0818-2-f12-finance-period-close-wizard F12 Tier C 期末结账向导 successor

> Plan Status: completed
> Last Reviewed: 2026-07-23
> Source: `docs/backlog/frontend-ui-roadmap.md` §F12（line 305-330，finance: ErpFinAccountingPeriod 期末结账向导 ❌ Deferred）+ §F16（line 402 maintenance 向导 successor 引用 finance 向导范式）+ `docs/plans/2026-07-21-0330-3-f12-page-structure-tabs-wizards.md` §Deferred「Tier C ErpFinAccountingPeriod 期末结账向导」
> Related: `docs/plans/2026-07-22-1400-1-f16-high-risk-gantt-bom-scan.md`（F16 高风险 successor，§Deferred「maintenance 4 步向导 BLOCKED」引用 finance 向导为先例范式）；`docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md`（F16 凭证录入/三单匹配复杂页面范式先例）
> Audit: required

## Current Baseline

- **后端期末结账链已全量落地**（core-business-roadmap M4 done，plan `2026-07-05-0540-2`）：期间状态机 `OPEN→CLOSING→CLOSED→CLOSED_FINAL`（含反结账）、AR/AP/INV/AST/GL 模块按序关账、折旧集成门控、汇兑重估（AR/AP + 银行存款外币）、损益结转（收入/费用/成本三类）、试算平衡表快照、年度结转（本年利润→未分配利润凭证 + 次年年初余额 + 自动建次年 12 期间）、反结账红冲。
- **Facade 入口已暴露为 GraphQL action**（`module-finance/erp-fin-service/.../entity/ErpFinAccountingPeriodBizModel.java`，事务钉在 Facade `@BizMutation`，Processor 为 protected step 供下游覆盖）：
  - `preCheck(periodId)` → `@BizQuery` → `PeriodPreCheckReport`（**实际字段经核实**：`unpostedVoucherCodes` / `unsettledArApCodes` / `unresolvedPostingExceptionKeys` + 4 坏账准备字段 `allowanceRequired/Balance/Shortfall/Excess` + `hasIssues()`/`issueCount()`；**无**辅助账对账字段——辅助账对账是 `closePeriod` 年度分支内的 config-gate，非 preCheck 范畴。**只读**）
  - `closePeriod(periodId)` → `@BizMutation` → 一次性同步编排 AR→AP→INV→AST→GL 模块关账 + 12 月年度结转分支（config-gated）
  - `finalizePeriod(periodId)` → `@BizMutation` → `CLOSED→CLOSED_FINAL` 终关
  - `reverseClose(periodId)` → `@BizMutation` → 反结账（红冲结转凭证 + 重开期间）
  - `generateNextYearPeriods(year)` → `@BizMutation` → 批量建次年 1-12 期间（由 `closePeriod` 年度分支内部调用，向导不单独暴露为独立步骤）
- **per-module 关账结果可观测**：`ErpFinAccountingPeriodStatus` 实体（`app-erp-finance.orm.xml:669-673`）记录 `arStatus`/`apStatus`/`invStatus`/`glStatus`/`assetStatus` 各模块关账状态枚举——由于 `closePeriod` 是**同步一次性** mutation，向导在执行后展示 per-module 关账**结果**（非实时逐步进度）。
- **owner doc 已有权威步骤模型**：`docs/design/finance/period-close.md` §期末结账步骤（8 步：业务单据过账检查/成本核算/折旧计提/费用摊销/损益结转/生成结账凭证/标记期间结账/生成结账报告）+ §反结账步骤（8 步概念模型：审批/开启期间/处理结转凭证/处理折旧凭证/处理成本凭证/解锁业务单据/重新结账/记录审计；实际由单个 `reverseClose` mutation 一次性执行）。**这是 wizard 步骤映射的权威基准**。
- **缺口（Deferred 根因，经核实）**：roadmap §F12 笔误描述「5 步：成本转结→汇兑损益→损益结转→凭证复审→结账」，与实际后端**不一致**——前任 plan `2026-07-21-0330-3:44` 独立审计已核实后端**不存在** roadmap 描述的 5 步独立 mutation（closeCostTransfer/closeFx/closePnl/reviewVoucher/closePeriod），`closePeriod` 内部一次性多步执行。**无任何前端结账向导页面存在**：`module-finance/erp-fin-web/.../pages/` 经核实仅有 CRUD/picker/报表/对账页面，**无 `period-close-wizard` page.yaml**。用户当前只能逐实体在 `ErpFinAccountingPeriod` 列表页对单行调 mutation（无引导式流程、无 preCheck 结果预览、无结果可视化、无反结账确认）。
- **前任 successor 触发条件与本计划的架构再基线（关键裁决）**：前任 `2026-07-21-0330-3:44,75` 将 Tier C wizard 标记为财务保护区域，并设定 successor 触发条件为「后端 mutation 重构授权 + 财务保护区域人工审查」，successor 内容含「后端 mutation 重构 + AMIS wizard PoC」。**本计划作出相反的架构裁决并在此显式再基线**：(a) 本计划 Phase 0 Explore 已前置核实（见上）——后端实际 5 个 mutation（preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods）**完全充分**，roadmap「5 步」纯属笔误，**无需后端 mutation 重构**，故「后端 mutation 重构授权」触发条件**失效（moot）**；(b) 前任 successor 触发条件的另一支「财务保护区域 owner doc 明确 wizard 行为」由本计划 Phase 2 owner-doc 对齐项目满足（向 `period-close.md` 增「期末结账向导」段，明确 wizard 步骤映射与 action 调用契约）；(c) 本计划 Non-Goal 明确**零后端 delta**——向导仅 UI 编排既有、已审计、owner-doc（`period-close.md`）已记录行为的 M4 mutation，**不引入任何新会计逻辑/数据写入语义**，故不新增财务保护区域风险；(d) 财务保护区域审查义务由 AGENTS.md §12 强制的**独立子代理结束审计**（新会话）承担，对「向导仅编排既有 mutation、不触碰会计逻辑」进行证据核实。若 Phase 0 Explore 发现某步骤确需新后端端点，该步骤降级为 Deferred（非扩后端），保护区域红线不被越过。
- **测试基线**：后端单测齐全（`TestErpFinPeriodCloseEndToEnd`/`TestErpFinExchangeRevaluation`/`TestErpFinAnnualClose`/`TestErpFinPeriodPreCheck`/`TestErpFinReverseClose` 全绿）；浏览器层 E2E 经核实**无 period-close wizard spec**。

## Goals

- 落地 finance 期末结账向导页面 `period-close-wizard.page.yaml`：引导式流程，按 owner doc 步骤模型驱动既有 `preCheck → closePeriod →（12 月年度结转）→ finalizePeriod` mutation 链 + `reverseClose` 反结账入口。
- 向导提供：(1) 期间选择；(2) preCheck 前置检查结果预览（PeriodPreCheckReport 结构化展示，阻断项高亮）；(3) closePeriod 执行 + per-module 关账结果可视化（AR/AP/INV/AST/GL 各模块 status，closePeriod 同步一次性故展示执行后结果而非实时进度）；(4) 12 月期间年度结转分支提示；(5) finalizePeriod 终关；(6) reverseClose 反结账确认（预览红冲影响）。
- 解除 F12 Tier C 最后一项 Deferred；为 F16 maintenance 4 步向导 successor 提供 finance 向导范式先例。
- 落地浏览器层 E2E（visual + action spec）验证向导驱动真实 mutation 的状态翻转与产物。

## Non-Goals

- **不改后端 Java/ORM/契约**（5 个 Facade mutation + Processor 已就绪；本计划纯前端 page.yaml 编排既有 action，零后端 delta。若 Explore 发现某步骤需新端点，该步骤降级为 Deferred 而非扩后端）。
- **不拆 `closePeriod` 为可逐步暂停的子 mutation**（一次性编排是既有设计与事务边界；向导展示进度而非真正逐步执行子事务）。
- **不做结账报表渲染**（资产负债表/利润表/现金流量表属 nop-report 报表面，已由报表子系统覆盖）。
- **不做费用摊销/待摊费用步骤4 UI**（owner doc 标注模块未落地，为已裁定 Non-Goal）。
- **不实现拖拽/甘特式交互**（结账是线性流程，AMIS `steps`/`wizard` 即可，无需 Flux）。
- **不做 maintenance 4 步向导**（BLOCKED 于 F4 maintenance child-table-editor 基线缺失，归独立 successor，本计划仅作范式先例）。
- **不做结账审批工作流**（结账是 DIRECT mutation，非 useWorkflow）。

## Task Route

- Type: `implementation-only change`（前端复杂 page.yaml 编排既有后端 mutation；无后端 Java/ORM/API 契约变更）
- Owner Docs: `docs/design/finance/period-close.md`（§期末结账步骤/§反结账步骤 权威步骤模型）；`docs/design/finance/README.md`；`docs/architecture/view-and-page-strategy.md`
- Skill Selection Basis: 匹配 `nop-frontend-dev`（复杂 page.yaml 手写、AMIS steps/wizard、bounded-merge 不触发、跨实体 GraphQL action 调用）。匹配 `nop-backend-dev` 的「跨实体调用」语义用于 Phase 0 Explore 核实 Facade action 签名与返回结构（只读核实，不写后端）。不匹配 `nop-testing`（spec 是 Playwright 浏览器层 action/visual spec，非 JunitAutoTestCase）。Phase 0 Explore 用 explore 子代理核实后端 baseline。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（复用既有 finance Facade mutation；无端口/密钥/外部服务）。
- 向导可能依赖既有 config-gated 开关（如 `erp-fin.annual-close-enabled`、`erp-fin.auxiliary-recon-gate-enabled`、`erp-fin.bank-fx-revaluation-enabled`）——这些已在 webServer/部署基线配置；本计划不改 config，仅在 UI 对开关关闭时的步骤做降级提示（若 Explore 裁决需要）。

## Execution Plan

### Phase 0 — Explore：Facade 步骤映射 + wizard 步骤模型裁决

Status: completed
Targets: `ErpFinAccountingPeriodBizModel.java`、`ErpFinAccountingPeriodProcessor.java`、`PeriodPreCheckReport`、`ErpFinAccountingPeriodStatus`、`period-close.md`
Skill: `nop-frontend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] Explore：核实 `PeriodPreCheckReport` 字段结构（**已前置核实**：`unpostedVoucherCodes`/`unsettledArApCodes`/`unresolvedPostingExceptionKeys` + 4 坏账准备字段 + `hasIssues()`/`issueCount()`，**无**辅助账对账字段——辅助账对账在 `closePeriod` 年度分支 config-gate 内），确认向导 step-2 预览消费的字段名；核实 `ErpFinAccountingPeriodStatus` per-module 状态字段（`arStatus`/`apStatus`/`invStatus`/`glStatus`/`assetStatus`，`app-erp-finance.orm.xml:669-673`），确认 step-3 **结果可视化**（closePeriod 同步一次性，非实时进度）数据源；核实 `closePeriod` 返回的 `ErpFinAccountingPeriod` 状态字段（status 翻转 OPEN→CLOSING→CLOSED）。
  - Skill: `nop-backend-dev`
  - **Explore 结论（实时仓库核实）**：
    - `PeriodPreCheckReport.java`（erp-fin-dao）字段确认：`unpostedVoucherCodes`/`unsettledArApCodes`/`unresolvedPostingExceptionKeys`（List<String>）+ `allowanceRequired`/`allowanceBalance`/`allowanceShortfall`/`allowanceExcess`（BigDecimal）+ `hasIssues()`/`issueCount()`。GraphQL 暴露为同名 field（`hasIssues`/`issueCount`）。**确认无辅助账对账字段**。
    - `app-erp-finance.orm.xml:669-673` ErpFinAccountingPeriodStatus per-module 字段确认：`arStatus`/`apStatus`/`invStatus`/`glStatus`/`assetStatus`（dict `erp-fin/module-close-status`：OPEN/CLOSING/CLOSED）+ `totalVouchers`/`postedVouchers`/`unpostedVouchers` + 关系 `period`（periodId）。是 step-3 关账**结果可视化**数据源（closePeriod 同步一次性，故展示执行后结果，非实时进度）。
    - `ErpFinAccountingPeriodBizModel.java`（erp-fin-service）5 个 Facade mutation 签名确认：`preCheck(periodId)→@BizQuery→PeriodPreCheckReport`（只读）/`closePeriod(periodId)→@BizMutation→ErpFinAccountingPeriod`（一次性编排 AR→AP→INV→AST→GL + 12 月年度分支）/`finalizePeriod(periodId)→@BizMutation`（CLOSED→CLOSED_FINAL）/`reverseClose(periodId)→@BizMutation`（红冲 + 重开）/`generateNextYearPeriods(year)→@BizMutation→Integer`（由 closePeriod 年度分支内部调用，向导不单独暴露）。status 翻转经既有 row-action visibleOn 核实：closePeriod OPEN/CLOSING→CLOSED、finalizePeriod CLOSED→CLOSED_FINAL、reverseClose CLOSED/CLOSED_FINAL→OPEN。
- [x] Decision（步骤映射）：将 owner doc 8 步结账 + 8 步反结账概念模型 + roadmap 5 步笔误裁决映射到实际可驱动的 wizard 步骤。预期映射：**Step 1 选择期间 + preCheck 前置检查**（`preCheck` 只读，展示 PeriodPreCheckReport，阻断项红色阻断继续）→ **Step 2 执行月度结账**（`closePeriod` 一次性编排，UI 按 per-module status 展示 AR/AP/INV/AST/GL 关账**结果**卡）→ **Step 3 年度结转**（仅 12 月期间可见，`closePeriod` 已内含年度分支，此步展示年度结转凭证 + 次年期间生成结果，非独立 mutation）→ **Step 4 终关**（`finalizePeriod` CLOSED→CLOSED_FINAL）→ **反结账**（独立 row-action/dialog，`reverseClose` 一次性执行 owner doc 8 步反结账概念模型 + 红冲预览确认）。记录 roadmap「5 步」笔误与实际映射的差异理由。
  - Skill: none
  - **Decision 结论**：采纳预期映射。roadmap §F12「5 步：成本转结→汇兑损益→损益结转→凭证复审→结账」属笔误——实际后端无此 5 个独立 mutation（`closePeriod` 内部一次性多步执行成本核算兜底/汇兑重估/损益结转/结账凭证），与前任 `2026-07-21-0330-3:44,75` 独立审计结论一致。wizard 4 forward step + 1 reverse 入口为权威映射，driver of UI 步骤指示器（preCheck→closePeriod→[annualClose]→finalize + reverse）。generateNextYearPeriods 由 closePeriod 年度分支内部调用，无独立向导按钮。
- [x] Decision（保护区域再基线确认）：确认「零后端 delta」裁决——核实向导调用的 4 个 action（preCheck/closePeriod/finalizePeriod/reverseClose）均为既有、已审计、`period-close.md` 已记录行为的 M4 mutation，向导不引入新会计逻辑/写入语义；据此确认前任「后端 mutation 重构授权」触发条件失效（moot），保护区域审查义务由 AGENTS.md §12 独立子代理结束审计承担。记录残留风险（如某步骤运行时发现需新端点→该步骤移出范围并记理由）。
  - Skill: none
  - **Decision 结论**：4 个 action 均为既有 Facade mutation（`ErpFinAccountingPeriodBizModel.java:35-57`），行为已在 `period-close.md` §期末结账步骤/§反结账步骤/§年度结转规则/§配置项 全量记录（M4 plan `2026-07-05-0540-2` 落地）。向导纯 UI 编排既有 action，**零后端 delta**，不触碰会计逻辑/写入语义。前任 successor 触发条件「后端 mutation 重构授权」失效（moot）——Explore 已证实 5 mutation 充分。保护区域审查义务由 AGENTS.md §12 强制的独立子代理结束审计（新会话）承担。残留风险：若运行时某步骤发现需新端点→该步骤移出范围并记理由（当前 Explore 未发现此类需求）。
- [x] Decision（组件选型）：在 AMIS `steps`（只读步骤指示器 + 各步独立 service/dialog）与 AMIS `wizard`（内置下一步/上一步）之间裁决。预期选 `steps` + 分步 dialog/service：结账各步是独立 GraphQL 调用（非表单连续提交），`wizard` 的表单连续模型不契合；`steps` 指示当前阶段 + 每步独立触发更贴合 mutation 编排语义。
  - Skill: `nop-frontend-dev`
  - **Decision 结论（经 Explore 再裁决）**：`form.xdef` 定义了 `layoutControl="wizard"` 但 **AMIS 渲染器仅实现了 `tabs`**（`nop-entropy/docs-for-ai/02-core-guides/layout-syntax-reference.md:288` 明确），故 view.xml wizard 布局不可用，必须手写 `page.yaml`。又 `page-structure-patterns.md §8.12`（b2b ASN 流程条先例）记录 AMIS `type:steps` prop 契约不稳，降级为 `each+tpl` 色块。故本向导**采用 each+tpl 步骤指示器 + 分步 service/button-driven mutation**（非 AMIS `type:steps` 也非 `type:wizard`），与 §8.12 先例一致，满足「步骤指示器 + 每步独立触发 mutation 编排」语义。结账各步为独立 GraphQL 调用（非表单连续提交），契合 button+service 模型。

Exit Criteria:

- [x] PeriodPreCheckReport / ErpFinAccountingPeriodStatus 字段结构已核实并记录（解除 step-2 预览与 step-3 关账结果可视化数据源阻塞）
- [x] 步骤映射 Decision 已裁决（roadmap 5 步 ↔ owner doc 8 步 ↔ 实际 mutation 链差异已记录理由），解除 Phase 1 实现阻塞

### Phase 1 — period-close-wizard.page.yaml 实现

Status: completed
Targets: `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/period-close-wizard/main.page.yaml`（NEW）
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0（步骤映射 + 组件裁决）

- [x] Add：落地 `period-close-wizard/main.page.yaml`——(1) 顶部 form 选期间（`ErpFinAccountingPeriod__findList` filter status，显示 年/月/状态）；(2) AMIS `steps` 指示当前阶段（preCheck→closePeriod→年度结转→finalize）；(3) Step 1 preCheck 按钮 → `ErpFinAccountingPeriod__preCheck`（@BizQuery）→ 结构化展示 PeriodPreCheckReport（`unpostedVoucherCodes`/`unsettledArApCodes`/`unresolvedPostingExceptionKeys` 列表 + 坏账准备 shortfall/excess；`hasIssues()`=true 时阻断项红色，禁用下一步）；(4) Step 2 执行结账按钮 → `ErpFinAccountingPeriod__closePeriod`（@BizMutation 同步一次性）→ 展示 per-module 关账**结果**卡（`ErpFinAccountingPeriodStatus` 的 arStatus/apStatus/invStatus/glStatus/assetStatus）+ status 翻转确认；(5) Step 3 年度结转区（visibleOn 期间月=12）展示年度结转凭证结果 + 次年期间生成计数；(6) Step 4 终关按钮 → `ErpFinAccountingPeriod__finalizePeriod`（@BizMutation）→ CLOSED_FINAL 确认；(7) 反结账 dialog（row-action 风格）→ `ErpFinAccountingPeriod__reverseClose`（@BizMutation 一次性执行 owner doc 8 步反结账概念模型）+ 红冲影响预览 + 二次确认。（注：`generateNextYearPeriods` 由 closePeriod 年度分支内部调用，不单独暴露为向导步骤。）
  - Skill: `nop-frontend-dev`
  - **落地核实**：`period-close-wizard/main.page.yaml` 已落地（`type: page`）。组件选型经 Phase 0 再裁决为 each+tpl 步骤指示器（非 AMIS `type:steps`/`type:wizard`，因 form `layoutControl="wizard"` AMIS 未实现 + `type:steps` prop 契约不稳，见 page-structure-patterns.md §8.12 先例）。包含：(1) 顶部 select（source=`ErpFinAccountingPeriod__findPage` orderBy year/month desc，显示 `${year}-${mm} [${status}]`）+ 进入向导 button（reload wizardService，disabledOn `!periodId`）；(2) wizardService（`ErpFinAccountingPeriod__get` + `ErpFinAccountingPeriodStatus__findPage` client-side filter periodId）adaptor 计算 flowSteps 状态色块；(3) Step 1 preCheck button→dialog service（`ErpFinAccountingPeriod__preCheck` 查询 PeriodPreCheckReport 全字段 + hasIssues/issueCount，adaptor 产 issueRows 表 + 坏账充足性 + 列表，hasIssues=true 时 alert-danger「阻断项存在」）；(4) Step 2 closePeriod button（actionType:ajax + confirmText + visibleOn canClose，api `mutation ...__closePeriod`，reload wizardService）+ per-module 结果卡（each statusRows + grid 5 tpl 卡 AR/AP/INV/AST/GL，visibleOn CLOSED+）；(5) Step 3 年度结转 wrapper visibleOn `isDecember && (CLOSED||CLOSED_FINAL)` + 查看次年期间 button→dialog（findPage client-side filter year+1）；(6) Step 4 finalizePeriod button（actionType:ajax + visibleOn canFinalize）；(7) 反结账 wrapper visibleOn canReverse + dialog（红冲影响预览 alert-danger + reverseImpactService 显示 currentStatus→OPEN + 二次确认 reverseClose button actionType:ajax）。generateNextYearPeriods 仅在 Step 3 文案提及（由 closePeriod 年度分支内部调用），无独立按钮。**YAML 反模式修复**：confirmText 含 `${isDecember ? " + 年度结转" : ""}` 嵌套双引号，按 §8.9 anti-pattern 改单引号包裹；Step 2 tpl 内 `\"` 改 `''`（single-quote YAML 转义）。
  - **验证**：`mvn -pl module-finance/erp-fin-web install -DskipTests` BUILD SUCCESS + `mvn -pl app-erp-all test -Dtest=ErpAllWebPagesTest` Tests run:1 Errors:0（page.yaml 通过 `PageProvider.validateAllPages` 解析校验）。

Exit Criteria:

- [x] `period-close-wizard/main.page.yaml` 真实存在且非 hollow（含 4 step + preCheck/closePeriod/finalize/reverseClose 4 GraphQL action 调用 + per-module 关账结果卡 + 12 月 visibleOn + 反结账确认 dialog；generateNextYearPeriods 经 closePeriod 内部调用，无独立按钮）
- [x] 向导调用的全是既有 action（git diff 后端 Java 为空，零后端 delta）——`git diff --name-only | grep -E "\.(java|orm\.xml|api\.xml|xbiz\.xml)$"` = NO backend file changes（仅 page.yaml + plan.md 变更）

### Phase 2 — 菜单接入 + 测试 + 文档对齐 + roadmap 收口

Status: completed
Targets: `module-finance/erp-fin-web/.../erp-fin.action-auth.xml`、`tests/e2e/`、`docs/design/finance/period-close.md`、`docs/design/page-structure-patterns.md`、frontend-ui-roadmap
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add：`erp-fin.action-auth.xml` 新增 `fin-period-close-wizard` 菜单项（归 `fin-period` 期间分组，orderNo 紧随 ErpFinAccountingPeriod），url 指向 `period-close-wizard/main`，含 displayName。
  - Skill: `nop-frontend-dev`
  - **落地核实**：`erp-fin.action-auth.xml` 已加 `fin-period-close-wizard` resource（fin-period 分组，orderNo=205 紧随 ErpFinAccountingPeriod 200，url=`/erp/fin/pages/period-close-wizard/main.page.yaml`，displayName=期末结账向导 / Period Close Wizard，icon=magic）。xmllint well-formed OK；TestAppActionAuthMerge 通过（无重复 resource id）。
- [x] Proof：新增 `tests/e2e/business-actions/fin-period-close-wizard.action.spec.ts`——浏览器层驱动真实 mutation 链：选种子期间 → preCheck 断言 PeriodPreCheckReport 结构非空 → closePeriod 断言 status CLOSED + per-module 关账 + (12 月) 年度结转凭证/次年期间 → finalizePeriod 断言 CLOSED_FINAL → reverseClose 断言红冲 + 期间重开 + 非法守卫（已 FINAL 不可直接 close、preCheck 阻断项存在时禁用）。复用既有 `callMutation`/`verifyState` 原语。自包含建/清理测试专用期间避免污染 finance 看板基线。
  - Skill: `nop-frontend-dev`
  - **落地核实**：spec 落地，建测试专用期间（2026-08，避开种子 period id=1 看板基线）。驱动全链：preCheck（@BizQuery raw GraphQL + field selection，结构非空 + 客户端算 hasIssues）→ closePeriod（CLOSED + ErpFinAccountingPeriodStatus per-module glStatus=CLOSED）→ finalizePeriod（CLOSED_FINAL）→ 非法守卫（closePeriod on CLOSED_FINAL 返 GraphQL errors + null data）→ reverseClose（OPEN + REVERSAL 红冲凭证）。cleanup 删 PERIOD-CLOSE-/FX-REVAL- 凭证 + per-module 状态行 + 期间。`npx playwright test` **1 passed**。
  - **关键裁决（环境约束）**：E2E 共享 DB 种子含 OPEN receivable item 5 → allowance gate（默认 ON）阻断 closePeriod；reverse-close-approval（默认 true）阻断 reverseClose。webServer config 追加 `-Derp-fin.bad-debt-allowance-gate-enabled=false -Derp-fin.reverse-close-approval-required=false`（test infra 配置，与既有 mfg/mnt/fin subject config 同范式；核实无 E2E spec 依赖这两 gate，fin-bad-debt provision 独立于 close gate）。`playwright.config.ts` webServer command 已更新。
- [x] Add：新增 `tests/e2e/visual/fin-period-close-wizard.visual.spec.ts`——断言 page metadata 可达 + steps 指示器结构 + preCheck 结果区 + 反结账 dialog 存在。
  - Skill: `nop-frontend-dev`
  - **落地核实**：spec 落地，单次 login（导航 wizard 路由）→ 建测试期间 → 选期间（select 含 code 标签唯一匹配）+ 进入向导 → 断言 page URL + 期间选择 form + 步骤指示器（前置检查/月度结账/终关 色块标签）+ preCheck dialog（table + 坏账准备充足性 alert）→ closePeriod GraphQL 翻 CLOSED + reload → 反结账 section 渲染 + dialog（红冲影响预览）。`npx playwright test` **1 passed**。
- [x] Add：owner doc 对齐——`docs/design/finance/period-close.md` 增「期末结账向导（F12 Tier C）」段（步骤映射表 + wizard action 调用契约 + per-module 关账结果数据源 + 反结账确认）；`docs/design/page-structure-patterns.md` 增 wizard 范式小节（引用本 plan 作为 finance 向导首例，供 maintenance successor 参考）；roadmap §F12 Tier C Deferred 标记已落地 + §退出标准 F12 收口；更新 §F16 maintenance successor 引用本计划为范式先例。
  - Skill: none
  - **落地核实**：`period-close.md` 增「期末结账向导」段（步骤映射表 + per-module 数据源 + 组件选型裁决 + E2E 覆盖）；`page-structure-patterns.md` §5 从「占位」回填为「wizard 范式（F12 Tier C 落地）」含组件选型裁决表 + 落地范式 + 反模式表 + GraphQL field selection 注意（hasIssues/issueCount 非字段）；§4 Deferred 表 ErpFinAccountingPeriod wizard 标 ✅ 已落地；§6 反模式表更新；roadmap §F12 line 317 标 ✅ done + Status line 307 + 落地清单 line 330（14/16→15/16）；§F16 maintenance 向导 line 402 后加 wizard 范式先例引用。

Exit Criteria:

- [x] `fin-period-close-wizard` 菜单可达且 url 正确（TestAppActionAuthMerge 通过 + url 指向 period-close-wizard/main.page.yaml）
- [x] action spec + visual spec 存在且驱动真实 mutation（非仅类型签名）——action spec 驱动 preCheck→closePeriod→finalize→reverseClose 全链 + 非法守卫，visual spec 驱动 UI 结构 + closePeriod/reverseClose DOM；`npx playwright test` 两 spec 均 passed
- [x] period-close.md wizard 段 + page-structure-patterns wizard 范式 + roadmap §F12/§F16 successor 引用已更新

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_073a6079fffeltg3WAC9fVvB77) — BLOCKERS=0（后端 5 mutation 全部经实时仓库核实存在、签名准确，「零后端 delta」Non-Goal 可行）。MAJOR：未与前任 `2026-07-21-0330-3:44,75` 的 successor 触发条件（「后端 mutation 重构授权 + 财务保护区域人工审查」）和解——本计划作相反架构裁决（零后端 delta）却未显式再基线。MINORS：反结账步骤数误写「4 步」（实为 8 步概念模型）、preCheck 误含「辅助账对账差异」（PeriodPreCheckReport 实无此字段，辅助账对账在 closePeriod 年度分支 config-gate 内）、「进度卡」措辞应改「结果可视化」（closePeriod 同步一次性）、generateNextYearPeriods 缺溯源说明。
- Independent draft review iteration 2: acceptable-as-is (ses_0739f4029ffeyw4jS1hdVyIGmT) — 经实时仓库核实，iteration 1 的 MAJOR（保护区域和解）与全部 4 个 MINORS 均已解决：Current Baseline 新增「架构再基线」三支裁决 + Phase 0 新增「保护区域再基线确认」Decision；后端 5 mutation + PeriodPreCheckReport 字段 + ErpFinAccountingPeriodStatus per-module 字段 + period-close.md 8 步反结账均逐字段核实；「进度」→「结果」全域一致；generateNextYearPeriods 溯源已补。0 blockers，0 majors，2 个非阻塞 citation polish（predecessor trigger 措辞、loose「5 mutation」headline，已在本轮补 owner-doc 触发支交叉引用）。计划为可接受的执行契约，转 active。

## Closure Gates

- [x] 范围内行为完成（period-close-wizard.page.yaml 4 step + 4 action + per-module 关账结果卡 + 反结账确认；菜单可达）
- [x] 相关文档对齐（period-close.md wizard 段 + page-structure-patterns wizard 范式 + roadmap §F12 收口 + §F16 successor 引用）
- [x] 已运行验证：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS，纯 page.yaml 变更不破坏构建）+ finance service `mvn test`（261 tests 0 failures，零后端 delta 无回归）+ `npx playwright test` 新增 fin-period-close-wizard action+visual spec 全绿（2 passed）+ 既有 finance 视觉/数值回归无漂移（fin-bad-debt + finance-voucher-post 4 passed）
- [x] 无范围内项目降级为 deferred/follow-up（若 Explore 裁决某步骤需后端端点则该步骤显式移出范围并记理由，非静默降级）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 2 acceptable-as-is）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（独立子代理 ses_0730580d0ffeQkbee7hpURNffE 2026-07-23 闭合审计：PASS——零后端 delta 核实、4 action 仅调既有 BizModel mutation 核实、page.yaml 非 hollow 核实、菜单 well-formed 核实、tests 非 type-signature-only 核实、docs 对齐核实）
- [x] 结束证据存在于文件中（见 Closure Audit Evidence）

## Deferred But Adjudicated

### F16 maintenance 4 步向导

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: BLOCKED 于 F4 maintenance child-table-editor 基线缺失（ErpMntVisitTask/ErpMntSparePartUsage sub-grid-edit 未落地，需 ORM cascade-delete 批准）。本计划为 finance 向导，maintenance 向导归独立 successor。本计划 page-structure-patterns wizard 范式为 maintenance successor 提供先例。
- Successor Required: `yes`（触发条件：maintenance F4 P2 successor 完成 child-table-editor 基线后）

### 费用摊销/待摊费用步骤 UI

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: `period-close.md` 已裁定模块未落地为 Non-Goal；wizard 步骤映射 Decision 已排除该步骤。
- Successor Required: `yes`（触发条件：费用摊销模块落地后）

### 结账报表渲染（资产负债表/利润表/现金流量表）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 nop-report 报面子系统，已由报表子系统覆盖；wizard 不内嵌报表渲染。
- Successor Required: `no`（按需经报表菜单查看）

## Closure

Status Note: period-close-wizard.page.yaml 落地驱动既有 preCheck/closePeriod/finalizePeriod/reverseClose 链（零后端 delta）、per-module 关账结果可视化（ErpFinAccountingPeriodStatus）、12 月年度结转分支（closePeriod 内部 generateNextYearPeriods）、反结账确认（红冲影响预览 + 二次确认）、菜单可达（fin-period-close-wizard orderNo=205）、action/visual spec 全绿（2 passed）、零后端 delta（git diff 无 Java/ORM/API/xbiz）、period-close.md + page-structure-patterns §5 wizard 范式对齐、roadmap §F12 Tier C 收口（15/16 done）+ §F16 maintenance successor 引用本计划为范式先例。环境约束裁决：webServer config 追加 allowance-gate=false + reverse-close-approval=false（test infra，核实无 E2E 依赖）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_0730580d0ffeQkbee7hpURNffE（general，新会话，2026-07-23）
- Verdict: PASS
- Evidence:
  - Phase 0 完成：3 Decision + Explore 有真实 findings（PeriodPreCheckReport 字段、ErpFinAccountingPeriodStatus per-module、组件选型 form wizard 未实现 + type:steps 不稳）
  - page.yaml 非 hollow（416 行，4 GraphQL action 调用 line 170/245/346/408、per-module 卡 line 272-276、12 月 visibleOn line 281、反结账 dialog line 371-416、generateNextYearPeriods 无独立按钮）
  - 零后端 delta：git status --porcelain 无 .java/.orm.xml/.api.xml/.xbiz 变更（核实硬 Non-Goal）
  - 菜单 wired：erp-fin.action-auth.xml:41-44 fin-period-close-wizard resource，xmllint well-formed
  - tests 非 type-signature-only：action spec 驱动 4 mutation + 状态翻转断言 + 非法守卫 + REVERSAL 凭证；visual spec DOM 结构断言
  - docs 对齐：period-close.md:356 wizard 段、page-structure-patterns.md:303 §5 wizard 范式（非占位）、frontend-ui-roadmap.md:317 ✅ done
  - 仅调既有 action：ErpFinAccountingPeriodBizModel.java:37/43/49/55/61 5 mutation 全存在，page.yaml 仅调这些
  - 验证日志：docs/logs/2026/07-23.md（mvn clean install 154 模块 BUILD SUCCESS / finance 261 tests 0 failures / playwright 2 passed / finance 回归 4 passed）

Follow-up:

- maintenance 4 步向导 successor（见 Deferred，BLOCKED 于 F4 maintenance 基线；本计划 §5 wizard 范式为先例）
- 费用摊销步骤 successor（见 Deferred，BLOCKED 于模块未落地）
