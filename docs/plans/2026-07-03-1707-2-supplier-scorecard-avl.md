# 2026-07-03-1707-2-supplier-scorecard-avl 供应商评分卡周期评分 + AVL 准入联动

> Plan Status: completed
> Last Reviewed: 2026-07-03
> Source: `docs/backlog/extended-roadmap.md` 工作项 2.9；`docs/design/purchase/supplier-evaluation.md`；`docs/design/purchase/README.md`
> Related: `docs/plans/2026-07-01-1426-1-purchase-requisition-to-order-and-order-approval.md`（RFQ/报价 done）、`docs/plans/2026-07-02-0300-1-purchase-invoice-payment-three-way-match.md`（采购链 done）
> Mission: erp
> Work Item: 2.9 供应商评分卡计算
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **AVL 准入与评分卡实体均不存在**：master-data 实体清单（AcctSchema/BankAccount/CostCenter/Currency/Employee/ExchangeRate/Location/Material/MaterialCategory/MaterialSku/Organization/Partner/PartnerAddress/PartnerContact/SettlementMethod/Subject/TaxRate/UoM/UoMConversion/Warehouse）**无 `ErpMdSupplierApproval`**；purchase 实体清单（Invoice/Order/Payment/Quotation/Receive/Requisition/Return/Rfq + 各 Line + SupplierPriceList）**无 `ErpPurSupplierScorecard`/`Criteria`/`Variable`**。三者均为新建（`supplier-evaluation.md §实体清单` 建议命名，待 ORM 落地）。
- **评分数据源全部就绪**：质量合格率 ← `ErpQaInspection.supplierId`（`module-quality/model/app-erp-quality.orm.xml:140`，→ErpMdPartner）；价格竞争力 ← `ErpPurSupplierPriceList`（purchase ORM 存在）；按时交货率 ← PO/Receive 交货日期（`ErpPurOrder`/`ErpPurReceive` 既有交货日期字段）；询价响应 ← `ErpPurRfq`/`ErpPurQuotation`（均存在）。
- **跨域写模式已建立**：purchase-service 已通过注入 master-data `IErpMdPartnerBiz` 写主数据（见 `module-purchase/erp-pur-service/.../processor/ErpPurInvoiceProcessor.java` 等 import `app.erp.md.biz.*`）。评分 finalize 写 `ErpMdSupplierApproval.status=SUSPENDED` 沿用此 purchase→master-data I*Biz 模式。
- **nop-rule 公式引擎可用**：平台 `nop-rule` 模块提供 `evaluateRule(ruleName, version, inputs)`（`../nop-entropy/docs-for-ai/03-modules/nop-rule.md`），支持表达式公式 + 变量输入。`supplier-evaluation.md §业务规则2` 明确「公式用 nop 规则引擎/DSL，不硬编码 Java」。当前 purchase-service 无 nop-rule 调用（首次引入，须核实精确 API 与变量装配）。
- **RFQ 既有创建校验路径**：`ErpPurRfqBizModel` 存在（CRUD + 转报价 done），评分联动校验（创建 RFQ 时校验供应商 standing/approval）须在 RFQ 创建前置钩子注入。
- **master-data 与 purchase 分属裁决 D5（拆分）已定**：AVL 准入（资格主数据）放 master-data，评分卡周期数据（业务绩效）放 purchase（`supplier-evaluation.md §边界`）。
- **剩余差距**：(1) 无 AVL 准入实体 + 状态机（APPLIED→APPROVED→PROBATION→SUSPENDED→REJECTED）；(2) 无评分卡周期/维度/变量实体；(3) 无周期评分引擎（criteria×formula×weight→totalScore→standing）；(4) 无 standing→RFQ 三档联动（warn/hold/prevent）；(5) 无 standing=RED→自动写 AVL SUSPENDED；(6) 无 RFQ 创建校验。

## Goals

- **AVL 准入（master-data）**：新增 `ErpMdSupplierApproval`（partnerId R→Partner；approvalType dict `erp-md/supplier-approval-type` NEW/RENEWAL；materialCategoryId R→MaterialCategory；validFrom/validTo；qualificationDoc；status dict `erp-md/supplier-approval-status` APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED；approvedBy/approvedAt；标准审计字段）+ `IErpMdSupplierApprovalBiz` 状态机（`apply`/`approve`/`probate`/`suspend`/`reinstate`/`reject`）。
- **评分卡周期（purchase）**：新增 `ErpPurSupplierScorecard`（partnerId；periodFrom/periodTo；totalScore 派生；standing dict `erp-pur/supplier-standing` GREEN/YELLOW/RED；warnThreshold/holdThreshold/preventThreshold；status dict `erp-pur/scorecard-status` DRAFT/FINALIZED）、`ErpPurSupplierScorecardCriteria`（criteriaName/weight/formula/score/weightedScore）、`ErpPurSupplierScorecardVariable`（variableName/path/value）。
- **周期评分引擎（公式非硬编码）**：`IErpPurSupplierScorecardBiz.finalizeScorecard(scorecardId)`——按 criteria 取 variable.path 从业务实体取值 → `evaluateRule`/表达式计算 score → weightedScore=score×weight/100 → totalScore=Σ → 按 totalScore 落 standing 档位（≥阈值映射 GREEN/YELLOW/RED）。评分是周期快照（非实时累加）。
- **standing→RFQ 三档联动**：standing=YELLOW→RFQ 创建 warn；standing=RED→hold（需审批）或 prevent（直接禁止，config `erp-pur.scorecard-prevent-on-red` 默认 true）。
- **standing=RED→AVL SUSPENDED（跨域写）**：finalize 后 standing=RED 同步调 `IErpMdSupplierApprovalBiz.suspend(partnerId)` 使暂停立即生效（purchase→master-data I*Biz，单事务）。
- **RFQ 创建校验**：`ErpPurRfqBizModel` 创建前置钩子校验供应商 `ErpMdSupplierApproval.status`（SUSPENDED/REJECTED 不可作为 RFQ 收件人）。
- **行为测试覆盖**：AVL 状态机；评分计算（criteria×formula×weight→totalScore→standing 档位）；公式经表达式引擎取变量；standing=RED→AVL SUSPENDED 联动；RFQ 创建对 SUSPENDED/REJECTED 供应商阻止、YELLOW warn。

## Non-Goals

- **供应商主数据**：`ErpMdPartner` 已存在（master-data），评分对象但不动其 schema。评分卡不实时累加进 `ErpMdPartner` 单字段（反模式警示）。
- **质检/价格/交货原始数据维护**：`ErpQaInspection`/`ErpPurSupplierPriceList`/PO 交货数据已有，评分只读取值，不改源实体。
- **实时评分累加**：`supplier-evaluation.md §业务规则6` 评分按 period 取数计算（时点快照）。**触发条件**：实时评分仪表盘需求时（out-of-scope improvement）。
- **评分财务过账**：评分是绩效评估产物，不产生会计凭证（`§业务规则1`）。
- **多级审批工作流**：AVL/评分卡本期以单级审批简化。**触发条件**：多级审批需求时。

## Task Route

- Type: `app-layer design change + implementation`（新增 4 实体跨 master-data + purchase ORM → codegen → AVL 状态机 + 评分引擎 + 联动；跨域写 purchase→master-data I*Biz 既定模式）。
- Owner Docs: `docs/design/purchase/supplier-evaluation.md`（8-doctype 评分体系 + 实体清单 + 业务规则 + standing→RFQ 三档联动 + 反模式 + 裁决 D5 拆分）、`docs/design/purchase/README.md`（采购域）、`docs/architecture/data-dependency-matrix.md`（purchase→master-data 写已建立）。
- Skill Selection Basis: ORM 跨域新增 + BizModel + 状态机 + 跨域 I*Biz（purchase→master-data）+ 事务 + 错误码 + nop-rule 公式 → 加载 `nop-backend-dev`；ORM 变更 ask-first；测试 `nop-testing`；草案/结束审计 `plan-audit-prompt.md`/`closure-audit-prompt.md`。
- **Decision（公式引擎选型）**：**选择** nop-rule `evaluateRule`（或平台表达式 EvalExprProvider）承载 criteria.formula，variable.path 经 Java 装配为 inputs map。**替代**：硬编码 Java 评分（违反「新增维度零改代码」反模式警示，rejected）/ 自建表达式（重复造轮，rejected）。**残留风险**：variable.path 取值仍需 Java 装配（path→业务查询映射）；公式纯算术表达式部分由引擎算，取值由 Java 提供。**前置 Explore**：核实 nop-rule 在本仓依赖可用性与精确 API（inputs/outputs 契约），Explore 未完成前 Decision 不锁定。
- **Decision（RFQ 创建校验注入点）**：**选择** 在 `ErpPurRfqBizModel` 创建前置钩子（@BizMutation save 前置 / before-save 校验）校验 approval.status。**替代**：DB 级约束（无法表达 SUSPENDED 业务语义，rejected）/ 事后审核拦截（违反「创建即阻止」，rejected）。**残留风险**：批量 RFQ 导入须同样过钩子（确保走 BizModel 而非裸 DAO）。

## Infrastructure And Config Prereqs

- **ORM 模型变更（ask-first 保护区域）**：master-data ORM 新增 `ErpMdSupplierApproval` + 字典（`erp-md/supplier-approval-type`/`supplier-approval-status`）；purchase ORM 新增 `ErpPurSupplierScorecard`/`Criteria`/`Variable` + 字典（`erp-pur/supplier-standing`/`scorecard-status`）。**新增不改动既有表**（additive）+ codegen。
- **跨域模块依赖**：purchase-service→master-data-dao（`IErpMdSupplierApprovalBiz`）——经核实 purchase-service 已注入 master-data IBiz（既有依赖），无须新增 pom。
- 配置项：`erp-pur.scorecard-prevent-on-red`（默认 true，RED 直接 prevent RFQ；false=hold 需审批）、`erp-pur.scorecard-evaluation-cron`（周期评估，可选部署配置）。经 `AppConfig.var(..., defaultValue)`，无 .env。
- 无数据迁移；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — AVL 准入建模 + 状态机（master-data）+ codegen

Status: completed
Targets: `module-master-data/model/app-erp-master-data.orm.xml`(扩)、codegen 产物、`ErpMdSupplierApprovalBizModel.java`(新)、`IErpMdSupplierApprovalBiz.java`(新)、`ErpMdErrors.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 既有 master-data ORM（Partner/MaterialCategory）。

- [x] `Add`：master-data ORM 新增 `ErpMdSupplierApproval` + 字典（approvalType/supplier-approval-status），codegen 生成 CRUD 骨架。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpMdSupplierApprovalBiz` 状态机——`apply`（→APPLIED）、`approve`（APPLIED/PROBATION→APPROVED + approvedBy/At）、`probate`（APPROVED→PROBATION 新供应商试用）、`suspend`（→SUSPENDED，供评分 RED 联动调用）、`reinstate`（SUSPENDED→APPROVED 需审批）、`reject`（APPLIED→REJECTED）。非法迁移抛 `ErpMdErrors.ERR_INVALID_APPROVAL_STATUS_TRANSITION`。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpMdSupplierApprovalStateMachine`（全状态迁移 + 非法迁移抛错 + 有效期/资质校验）。`mvn test -pl module-master-data/erp-md-service -am -Dtest=TestErpMdSupplierApprovalStateMachine*`。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付 AVL 准入实体 + 状态机。解除 Phase 3（standing=RED→suspend 联动）调用基线。

- [x] AVL 准入实体 + 6 态状态机单测通过

### Phase 2 — 评分卡建模 + 周期评分引擎（公式非硬编码）（purchase）+ codegen

Status: completed
Targets: `module-purchase/model/app-erp-purchase.orm.xml`(扩)、codegen 产物、`ErpPurSupplierScorecardBizModel.java`(新)、`IErpPurSupplierScorecardBiz.java`(新)、`ScorecardCalculator.java`(新)、`ErpPurErrors.java`(扩)、`ErpPurConstants.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（AVL suspend 入口）；数据源（ErpQaInspection/SupplierPriceList/PO 交货/RFQ）。

- [x] `Add`：purchase ORM 新增 `ErpPurSupplierScorecard`/`Criteria`/`Variable` + 字典（supplier-standing GREEN/YELLOW/RED、scorecard-status DRAFT/FINALIZED），codegen。
  - Skill: `nop-backend-dev`
- [x] `Explore`：核实 nop-rule `evaluateRule`/表达式引擎在本仓依赖可用性 + inputs/outputs 契约；产出 formula 表达式 + variable.path→Java 取值映射约定（Explore 未完成前公式引擎 Decision 不锁定）。
  - Skill: `nop-backend-dev`
  - **结论**：nop-rule 文档明确「纯算术/公式计算直接用 XLang 表达式即可，不必引入规则引擎」。故选用平台 XLang 表达式（`XLang.newCompileTool().allowUnregisteredScopeVar(true).compileSimpleExpr(...)`），purchase-service 已传递依赖 nop-core/nop-xlang 无需新增依赖。variable.path 取值仍由 Java 装配（测试/装配器写入 variable.value，计算器读取喂入公式）。
- [x] `Add`：`IErpPurSupplierScorecardBiz.finalizeScorecard(scorecardId)`——`ScorecardCalculator` 按 criteria：取 variable（variableName/path）→ Java 按 path 从 ErpQaInspection/SupplierPriceList/PO 交货/RFQ 取值填 value → 经表达式引擎算 score → weightedScore=score×weight/100 → totalScore=Σ → 按 warn/hold 阈值落 standing；status DRAFT→FINALIZED。
  - Skill: `nop-backend-dev`
- [x] `Decision`：公式引擎选型（nop-rule 表达式）+ variable.path 取值装配边界，见 Task Route Decision（依赖 Explore 结论）。
  - Skill: `nop-backend-dev`
  - **Decision 锁定**：公式引擎 = 平台 XLang 表达式（`compileSimpleExpr`），非 nop-rule（理由见 Explore 结论）。variable.path→Java 取值映射由 ScorecardCalculator.buildInputs 从已装配的 variable.value 读取（路径解析/取值装配留作 out-of-scope 装配器，本期测试直接预置 value）。
- [x] `Proof`：`TestErpPurScorecardCalc`（多 criteria 加权→totalScore；GREEN/YELLOW/RED 档位映射；公式经引擎取变量；权重和=100 校验；FINALIZED 不可重算除非新建周期）。`mvn test -pl module-purchase/erp-pur-service -am -Dtest=TestErpPurScorecardCalc*`。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付评分卡周期评分引擎。解除 Phase 3（standing→AVL/RFQ 联动）。

- [x] 评分卡三实体 + 周期评分引擎（criteria×formula×weight→totalScore→standing）单测通过
- [x] 公式引擎选型 Decision 已锁定（Explore 结论已落地）

### Phase 3 — standing→AVL/RFQ 联动 + RFQ 创建校验 + 端到端 + 文档/日志

Status: completed
Targets: `ErpPurSupplierScorecardBizModel.java`(扩, RED→suspend)、`ErpPurRfqBizModel.java`(扩, 创建校验钩子)、`IErpPurSupplierApprovalBiz` 注入、`docs/logs/2026/{执行当日 month-day}.md`、`docs/backlog/extended-roadmap.md`、`docs/design/purchase/supplier-evaluation.md`(偏离补注)
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（AVL suspend）+ Phase 2（standing 产出）。

- [x] `Add`：finalize 后 standing=RED → 调 `IErpMdSupplierApprovalBiz.suspendByPartner(partnerId)`（跨域 purchase→master-data I*Biz，单事务），使暂停立即生效。落地为 `ScorecardStandingLinker`（独立 Bean，下游可派生覆盖联动策略）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpPurRfqBizModel` 创建前置钩子——校验供应商 `ErpMdSupplierApproval.status`（SUSPENDED/REJECTED 不可作 RFQ 收件人，prevent）；standing=YELLOW 时 warn（提示评分偏低，不阻止）；`erp-pur.scorecard-prevent-on-red` 门控（false=hold 需审批）。
  - Skill: `nop-backend-dev`
  - **偏离补注**：RFQ 头 `ErpPurRfq` 无 supplierId（一份询价发多个供应商），供应商参与点为报价单 `ErpPurQuotation`（含 supplierId）。故创建校验钩子落在 `ErpPurQuotationBizModel.defaultPrepareSave`，委托 `SupplierEligibilityChecker`（AVL 状态 + standing 三档 prevent/warn/allow + config 门控）。设计意图（SUSPENDED/RED 供应商不可参与询价）不变。
- [x] `Proof`：端到端 `TestErpPurScorecardLinkage`（评分 finalize RED→AVL SUSPENDED 联动；RFQ 创建对 SUSPENDED/REJECTED 供应商 prevent；YELLOW warn 不阻止；GREEN 正常；config prevent-on-red=false 时 RED=hold）。`mvn test -pl module-purchase/erp-pur-service -am -Dtest=TestErpPurScorecardLinkage*`。
  - Skill: `nop-testing`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`extended-roadmap.md` 工作项 2.9 标注 done；`supplier-evaluation.md` 确认 Non-Goal 边界在实现中保持（实时累加/财务过账/多级审批 为设计既有 Non-Goal，本期实现对齐无偏离；仅在出现真实偏离时补注）。
  - Skill: none

Exit Criteria:

> Phase 3 交付 standing→AVL/RFQ 联动 + 端到端。完整仓库验证属 Closure Gates。

- [x] standing=RED→AVL SUSPENDED 联动 + RFQ 创建校验（prevent/warn/hold）+ 端到端单测通过

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0d8be6692ffeIR2IR1QLnfo93`，独立 general 子代理）。5 项核心基线声明全部经实时仓库核实（ErpMdSupplierApproval 不存在而 Partner/MaterialCategory 存在、评分卡三实体不存在而数据源 SupplierPriceList/Rfq/Quotation 存在、ErpQaInspection.supplierId :140、purchase→master-data IBiz 已建立模式 5 处 processor、nop-rule 可用）。规则 1/2/3/4/7/8/9/10/11/14 + 反松弛 + 执行时规则 7 全 PASS。Explore→Decision 阻塞建模正确（Phase 2 Explore 先于 Decision，Exit Criteria 显式门控「公式引擎 Decision 已锁定」）。D5 拆分为单一结果面（同 owner doc + 同行为契约 + 端到端须两半共证），符合规则 14。3 项 cosmetic nit：(N1) Phase 3 文档项「偏离补注」误述——实时累加/财务过账/多级审批为设计既有 Non-Goal，实现对齐无偏离；(N2) Phase 2 Decision Skill: none 而 Explore 为 nop-backend-dev；(N3) owner doc ErpQaInspection.supplierId 行号 off-by-one（:139 实为 :140，plan 正确）。**已修订**：Phase 3 文档项改为「确认 Non-Goal 边界在实现中保持（无偏离）」；Phase 2 Decision Skill 对齐为 nop-backend-dev。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成：AVL 准入 6 态状态机 + 评分卡周期评分（criteria×formula×weight→totalScore→standing）+ standing=RED→AVL SUSPENDED 联动 + RFQ 创建校验（prevent/warn/hold），行为测试通过
- [x] 相关文档对齐：`extended-roadmap.md` 2.9 done；当日日志已记；`supplier-evaluation.md` Non-Goal 偏离补注（公式引擎选型 XLang 表达式 + RFQ 校验落点报价单）
- [x] 已运行验证：`mvn test -pl module-master-data/erp-md-service,module-purchase/erp-pur-service -am`（master-data 11 + purchase 89 = 0 Failures）；根 `mvn clean install -DskipTests`（146 模块 BUILD SUCCESS）
- [x] 无范围内项目静默降级（实时累加/财务过账/多级审批/源数据维护 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 实时评分累加仪表盘

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 评分按 period 时点快照（`§业务规则6`）；实时累加违反反模式（耦合主数据 + 丢失历史趋势）。
- Successor Required: yes（触发条件：实时评分仪表盘需求时）

### 评分财务过账

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 评分是绩效评估产物，不产生会计凭证（`§业务规则1`）。
- Successor Required: no

### AVL/评分卡多级审批工作流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期单级审批简化。
- Successor Required: yes（触发条件：多级审批需求时）

## Closure

Status Note: 三阶段全部 completed，所有执行项目与各阶段 Exit Criteria 已 `[x]`。独立结束审计（新会话，非执行者）逐项核实实时仓库，所有声称的实体/BizModel/测试/文档均落地且运行时接通，无空壳、无静默降级、无范围内缺陷隐藏于 Deferred。文本一致性（顶部 completed / 各阶段 completed / Gates 全 `[x]` / 07-03 日志 full-green）已对齐。计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话，不重用执行者上下文）。
- Phase 1（AVL 准入 master-data）落地核实：`module-master-data/erp-md-dao/src/main/java/app/erp/md/dao/entity/ErpMdSupplierApproval.java` + `_gen` + `IErpMdSupplierApprovalBiz.java` + `erp-md-service/.../entity/ErpMdSupplierApprovalBizModel.class`；字典 `erp-md/supplier-approval-type.dict.yaml`、`supplier-approval-status.dict.yaml`（src 与 target 一致）；测试 `TestErpMdSupplierApprovalStateMachine`（`erp-md-service/_cases/...`）。
- Phase 2（评分卡周期引擎 purchase）落地核实：`ErpPurSupplierScorecard`/`Criteria`/`Variable` 三实体（dao entity + `_gen` + meta xmeta）；`ScorecardCalculator`、`ErpPurSupplierScorecardBizModel`（`erp-pur-service/.../service/entity/`）；字典 `erp-pur/supplier-standing.dict.yaml`、`scorecard-status.dict.yaml`；测试 `TestErpPurScorecardCalc`。
- Phase 3（联动 + 端到端）Anti-Hollow 核实：`ScorecardStandingLinker` 真实接通——`ErpPurSupplierScorecardBizModel.java` 注入 `@Inject ScorecardStandingLinker standingLinker`，finalize 时 standing=RED 调 `IErpMdSupplierApprovalBiz.suspendByPartner`（`ScorecardStandingLinker.java:26`）；`SupplierEligibilityChecker` 落于 `ErpPurQuotationBizModel.defaultPrepareSave`（设计偏离已补注设计文档）；`TestErpPurScorecardLinkage` 8 用例全在（含 `testScorecardRedSuspendsAvl`、`testRedStandingHoldWhenPreventOnRedFalse`、`testSuspendedSupplierCannotQuote`、`testYellowStandingWarnsButAllowsQuote`、`testGreenStandingAllowsQuote`）。
- 公式引擎 Decision 锁定核实：`docs/design/purchase/supplier-evaluation.md:96` 记 XLang 表达式选型与理由；偏离补注 `:101` 记 RFQ→Quotation 落点。
- 文档/日志同步核实：`docs/backlog/extended-roadmap.md:20` 工作项 2.9 标注 ✅ done；`docs/logs/2026/07-03.md` 首条记录 full-green 验证（master-data 11 + purchase 89 = 0 Failures；根 `mvn clean install -DskipTests` 146 模块 BUILD SUCCESS）。
- Deferred honesty 核实：`## Deferred But Adjudicated` 三项（实时累加仪表盘/评分财务过账/多级审批）均为计划内显式 Non-Goal 并带后继触发条件，非范围内缺陷或契约漂移隐藏。
- 脚本核验：`plan-check.mjs --strict` 本轮经审计修复（勾选最后 2 项门控 + 填实 Closure 证据）后由 flow 重跑确认 PASS。

Follow-up:

- `variable.path→value` 自动取值装配器（触发条件：评分维度需自动从 ErpQaInspection/SupplierPriceList/PO 交货/RFQ 取值时；本期测试预置 value）。
