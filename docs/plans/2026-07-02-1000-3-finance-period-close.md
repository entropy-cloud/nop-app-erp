# 2026-07-02-1000-3-finance-period-close 期末结账全流程（损益结转/期间状态/汇兑重估/反结账）

> Plan Status: active
> > Last Reviewed: 2026-07-02
> Source: `docs/backlog/core-business-roadmap.md` 工作项 4.3（期末结账全流程：成本核算→汇兑重估→结转损益→关账）；`docs/design/finance/period-close.md`
> Related: `2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（N=2 折旧能力，本计划 step3 调用其 executeBatchDepreciation）、`2026-07-02-0300-3-ar-ap-settlement-subledger.md`（辅助账/核销 + 显式 deferred 汇兑损益，本计划汇兑重估承接）、`2026-07-01-0811-1-finance-posting-engine-foundation.md`（过账引擎）、`2026-07-02-0900-1-audit-remediation.md`（active 审计整改，无冲突）
> Mission: erp
> Work Item: 期末结账全流程（M4 · 4.3）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **会计期间** `ErpFinAccountingPeriod`（`finance.orm.xml:348`）：code/year/month/startDate/endDate/quarter/isAdjustment/status(dict erp-fin/period-status)/closedBy/closedAt。**期间状态机字段存在但无迁移逻辑**（codegen 空）。
- **期间状态字典缺口（草案审查核实，契约漂移）**：`erp-fin/period-status`（finance.orm.xml:75）现仅 `OPEN(10)`/`CLOSING(20)`/`CLOSED(30)`/`NEVER_OPENED(40)`，**缺 `CLOSED_FINAL`**。而 owner-doc（`period-close.md:148,155,176`、`state-machine.md`）假设四态关账机 OPEN→CLOSING→CLOSED→CLOSED_FINAL。**此为 owner-doc↔dict 契约漂移**——本计划须在 Phase 1 经 ask-first 向 `erp-fin/period-status` 加 `CLOSED_FINAL(50)`（或裁决用 CLOSED 作终态并修订 owner-doc，见 Task Route Decision）。
- **期间模块状态** `ErpFinAccountingPeriodStatus`（:381）：periodId/acctSchemaId/totalVouchers/postedVouchers/unpostedVouchers + **五模块关账状态** arStatus/apStatus/invStatus/glStatus/assetStatus（dict erp-fin/module-close-status）。**无模块关账编排逻辑**（codegen 空）——这正是期末结账按模块推进的载体。
- **总账余额** `ErpFinGlBalance`（:526）：periodId/subjectId/currencyId/openingDebit/Credit + periodDebit/Credit + closingDebit/Credit + yearOpeningDebit/Credit + 辅助维度(partner/department/project/warehouse)。**损益结转的数据源已具备**（按 subjectId + subjectClass 聚合收入/费用/成本余额）。
- **试算平衡表** `ErpFinTrialBalance`（:580）：实体存在（periodId + subjectId + 余额族），关账快照载体，**无生成逻辑**。
- **科目分类** `ErpMdSubject.subjectClass`（dict `erp-md/subject-class`，master-data.orm.xml:687）+ `direction`（借贷方向）。**草案审查核实字典码值**：资产(10)/负债(20)/权益(30)/收入(40)/费用(50)/**成本(60)**。损益结转须按 subjectClass 识别**收入类(40)+费用类(50)+成本类(60)**（中国《企业会计准则》损益类含收入/费用/成本三类，如主营业务成本属成本类须一并结转本年利润）。
- **业财业务类型已存在**：`ErpFinBusinessType` 枚举已含 `PERIOD_CLOSE(120)`/`EXCHANGE_GAIN_LOSS(130)`（`ErpFinBusinessType.java:24-25`，`.getCode()` 返回 int 120/130，过账经 `setBusinessType(businessType.getCode())` 存数值）。**注**：字典 `erp-fin/business-type` 的 `code` 字符串为 `PERIOD_CLOSING(120)`/`FX_REVALUATION(130)`（仅元数据/UI，过账路径不用 int code）；Java 实现引用枚举常量 `ErpFinBusinessType.PERIOD_CLOSE`/`EXCHANGE_GAIN_LOSS`。**本计划无需新增业务类型**——损益结转走 PERIOD_CLOSE(120)，汇兑重估走 EXCHANGE_GAIN_LOSS(130)。
- **凭证** `ErpFinVoucher`(+Line+BillR) 存在；过账入口 `IErpFinVoucherBiz.post`/`reverse`（0811-1）。结转凭证/汇兑重估凭证经此入口生成。
- **辅助账多币种** `ErpFinArApItem`（0300-3）：amountSource(源币种)/amountFunctional(本位币)/openAmountSource/openAmountFunctional + currencyId/exchangeRate。**汇兑重估数据源已具备**——外币 ArApItem 未核销项按期末汇率重估差额 = 汇兑损益。0300-3 显式将「汇兑损益计算 + 凭证」deferred 至期末汇兑面（本计划承接）。
- **资产折旧能力**：assets 域（extended 2.5）当前 BizModel 空，计划 1000-2 拟落地 `executeBatchDepreciation`（发布到 erp-ast-api）。本计划 step3 折旧经配置门控调用（`erp-ast.auto-depreciation-on-close=true` 时注入 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation`）。
- **DAG 依赖方向（草案审查核实）**：`docs/architecture/data-dependency-matrix.md:81` 明确 finance 处于 DAG 顶，且 finance 对 assets 为合法 **R 只读依赖**（经 I*Biz 只读查源单）。故 finance-service 可加 `erp-ast-api` compile 依赖直接注入 `IErpAstDepreciationScheduleBiz`，**无需 SPI 反向调用**。
- **成本核算（存货成本）逻辑未落地**：`costing-methods.md` 设计未实现；但 `ErpInvCostLayer`（inventory.orm.xml:338）/`ErpInvBatch`（:569）**表已存在**，仅成本计算服务未实现。`period-close.md §步骤2` 成本核算依赖 inventory 成本计算——属 inventory 域，**本计划 Non-Goal**（config-gated 跳过）。
- **费用摊销（待摊费用）不存在**：全仓 `rg "PrepaidExpense|待摊"` 零命中；`period-close.md §步骤4` 费用摊销属未落地模块，Non-Goal。
- **报表（nop-report）不存在**：`period-close.md §步骤8` 结账报告（科目余额表/试算平衡表/账龄）属报表面，Non-Goal。
- **剩余差距**：(1) 无期间状态机（OPEN→CLOSING→CLOSED→CLOSED_FINAL，且 CLOSED_FINAL 字典值缺失）；(2) 无模块关账编排（AR/AP/INV/AST/GL 五模块按序推进 AccountingPeriodStatus）；(3) 无损益结转（收入/费用/**成本**余额→本年利润 PERIOD_CLOSE 凭证）；(4) 无汇兑重估（外币余额重估 EXCHANGE_GAIN_LOSS 凭证，承接 0300-3 deferred）；(5) 无反结账（冲销结转/汇兑/折旧凭证 + 期间回开）；(6) 无前置检查。

## Goals

- ORM 小幅字典增量：`erp-fin/period-status` 加 `CLOSED_FINAL(50)`（ask-first，补齐 owner-doc 四态关账机契约漂移；不加列、不加实体）。
- `IErpFinPeriodCloseBiz`（新 BizModel）期末结账编排：`preCheck(periodId)` → `closePeriod(periodId)` → `finalizePeriod(periodId)`，按 `period-close.md §期末结账步骤` 推进。
- **前置检查**（`§结账前置检查`）：扫描本期 posted=false 业务单据、未审核凭证、未核销 AR/AP（建议非阻断提示）；折旧/成本核算状态检查（config-gated）。
- **期间状态机**：`ErpFinAccountingPeriod.status` OPEN→CLOSING→CLOSED→CLOSED_FINAL（dict erp-fin/period-status，含新增 CLOSED_FINAL(50)），对齐 `§期间状态`；`ErpFinAccountingPeriodStatus` 五模块（AR/AP/INV/AST/GL）按序关账。
- **损益结转**（`§步骤5`）：按 `subjectClass` 识别**收入类(40)+费用类(50)+成本类(60)**科目（损益三类），聚合 `ErpFinGlBalance` 本期余额，生成 **PERIOD_CLOSE(120)** 结转凭证（收入→本年利润贷方；费用+成本→本年利润借方），结转后损益类科目余额清零。
- **汇兑重估**（承接 0300-3 deferred）：查询外币 `ErpFinArApItem` 未核销项，按期末汇率重估，差额生成 **EXCHANGE_GAIN_LOSS(130)** 凭证（`§汇兑重估`）；回写 0300-3 `ErpFinReconciliation.fxGainLoss` 占位（核销时已记录，此处完成独立期末重估）。
- **折旧集成**（`§步骤3`）：`erp-ast.auto-depreciation-on-close=true` 时注入 `IErpAstDepreciationScheduleBiz`（finance-service 加 erp-ast-api compile 依赖，DAG 合法 R）调 `executeBatchDepreciation(period)`（依赖 1000-2）；未落地时 config-gated 跳过并告警，不阻断结账。
- **反结账**（`§反结账流程`）：`reverseClose(periodId)` CLOSED_FINAL→OPEN，冲销结转凭证 + 汇兑重估凭证（+ 条件冲销折旧凭证，若 auto-depreciation 已生成）（红字），解锁凭证编辑，记录审计。
- 行为测试覆盖前置检查/损益结转（含成本类）/汇兑重估/期间状态迁移/反结账/折旧集成门控。

## Non-Goals

- **存货成本核算**（`§步骤2`）：inventory 域成本计算（移动加权/FIFO/批次）逻辑未落地（`costing-methods.md` 设计未实现；`ErpInvCostLayer`/`ErpInvBatch` 表已存在但计算服务未实现）；本计划 config-gated 跳过，归 inventory 域后续计划（触发条件：inventory 成本核算落地时接线）。
- **费用摊销/待摊费用**（`§步骤4`）：待摊费用模块未落地；config-gated 跳过，归后续（触发条件：待摊费用模块上线）。
- **结账报告（科目余额表/试算平衡表/账龄/库存台账/折旧明细，`§步骤8`）**：属 nop-report 报表面；本计划 populate `ErpFinTrialBalance` 快照数据，但不做报表渲染 UI（归前端 roadmap）。
- **年度结转（`§年度结转规则`）**：年末结转（本年利润→未分配利润 + 辅助账跨年结转 + 次年 12 期间自动创建）属独立结果表面，归后续计划（触发条件：跨年运营需求）。
- **nop-wf 反结账审批流**：反结账 = 直接状态迁移 + `@BizMutation` + 配置门控 `erp-fin.reverse-close-approval-required`（对齐全仓基线），不接入 nop-wf。
- **自动核销（nop-job）**：前置检查的「未核销 AR/AP」为非阻断提示，不自动核销（0300-3 Non-Goal 延续）。
- **结账兜底重新过账扫描（`§步骤1` 触发兜底扫描重新过账）**：仅扫描列出 posted=false 单据清单（阻断或提示），不自动批量重过账（避免与各域审核边界冲突）。

## Task Route

- Type: `architecture change`（新增跨模块关账编排机制 + 期间状态机 + 损益结转/汇兑重估凭证生成）。**注**：服务层 + BizModel 为主，**仅一处 ask-first ORM 字典增量**——向 `erp-fin/period-status` 字典加 `CLOSED_FINAL(50)` 选项（`app-erp-finance.orm.xml:75` 的 `<dict>` 段，补齐 owner-doc 契约漂移）。不新增实体、不加列。
- Owner Docs: `docs/design/finance/period-close.md`（流程/前置检查/期间控制/反结账/年度结转/配置）、`docs/design/finance/posting.md`（凭证生成）、`docs/design/finance/ar-ap-reconciliation.md`（汇兑损益 deferred 承接）、`docs/design/finance/costing-methods.md`（成本核算 Non-Goal 依据）。
- Skill Selection Basis: BizModel 编排 + 跨实体（期间/余额/科目/辅助账/资产折旧）+ 凭证生成 + 错误码 + 事务 → 加载 `nop-backend-dev`（覆盖 IBiz、跨实体访问、凭证生成、反模式自检）。
- **Decision（CLOSED_FINAL 契约漂移处理）**：**选择**经 ask-first 向 `erp-fin/period-status` 加 `CLOSED_FINAL(50)`，对齐 owner-doc 四态关账机（`§期间状态` CLOSED=结账完成待复核 / CLOSED_FINAL=最终锁定，二者语义有别，反结账自 CLOSED_FINAL）。**替代**：① 用既有 CLOSED(30) 作终态、删去 CLOSED_FINAL 并修订 owner-doc（丢失「待复核 vs 最终锁定」语义区分，rejected）；② 新增实体承载复核态（超范围，rejected）。**残留风险**：字典加值需 codegen 重新生成 period-status dict 产物。
- **Decision（收入/费用/成本科目识别）**：**选择**按 `ErpMdSubject.subjectClass` 识别**收入(40)+费用(50)+成本(60)**三类（损益类全集，草案审查确认字典含独立的 成本(60) 类）。**替代**：① 仅收入(40)+费用(50)（遗漏主营业务成本等成本类，本年利润不平，rejected）；② 按 subject code 前缀匹配（脆弱，rejected）。**残留风险**：无。
- **Decision（汇兑重估范围）**：**选择**仅重估外币 `ErpFinArApItem` 未核销项（AR/AP 往来），不重估货币性资产/负债科目余额（GlBalance 按 subject 重估需 subject 级币种标记，复杂度高）。**替代**：全货币性科目余额重估（需科目级币种属性，超范围，rejected）。**残留风险**：仅往来重估，银行存款外币重估归后续（触发条件：多币种银行对账落地时）。
- **Decision（结转科目「本年利润」）**：**选择**配置项 `erp-fin.retained-earnings-subject-id`/`erp-fin.current-year-profit-subject-id` 指定本年利润/未分配利润科目（不硬编码）。**替代**：硬编码科目（不灵活，rejected）。**残留风险**：未配置时结转失败抛 NopException 提示配置（前置检查覆盖）。
- **Decision（折旧集成耦合度）**：**选择** finance-service 加 `erp-ast-api` compile 依赖直接注入 `IErpAstDepreciationScheduleBiz`（DAG 合法 R，`data-dependency-matrix.md:81` finance 对 assets 只读依赖），配置门控可选调用（`erp-ast.auto-depreciation-on-close`）。**替代**：SPI 反向调用（finance 已是 DAG 顶可正向依赖 assets，无需反向，rejected）；完全不集成折旧（`§步骤3` 缺失，rejected）。**残留风险**：1000-2 未落地时跳过折旧致结账不完整（告警明示，运营知悉）。

## Infrastructure And Config Prereqs

- 配置项（`period-close.md §配置项` + 现仓约定）：`erp-fin.auto-post-on-close`(默认 false，前置检查列出 posted=false 清单，不自动重过账)、`erp-fin.auto-depreciation`(引用 `erp-ast.auto-depreciation-on-close`，默认 true，调 assets 批量折旧)、`erp-fin.closing-reminder-days`(默认 3)、`erp-fin.reverse-close-approval-required`(默认 true)、`erp-fin.current-year-profit-subject-id`(本年利润科目，必配)、`erp-fin.retained-earnings-subject-id`(未分配利润科目，年度结转用，本计划预留)、`erp-fin.exchange-revaluation-enabled`(默认 true，启用期末汇兑重估)、`erp-fin.period-end-exchange-rate-source`(期末汇率来源，默认手动配置)。经 `AppConfig.var(..., defaultValue)` 读取，无 .env。
- 模块依赖：`erp-fin-service` 已 compile 依赖 master-data-dao（科目/币种）；**新增** `erp-ast-api` compile 依赖（注入 `IErpAstDepreciationScheduleBiz` 调折旧，DAG 合法 R，`data-dependency-matrix.md:81`）。
- **保护区域门控**：向 `erp-fin/period-status` 字典加 `CLOSED_FINAL(50)` 触及 `module-finance/model/app-erp-finance.orm.xml`（ask-first）。Phase 1 实施前须：人工批准 + 本计划草案审查通过。
- 无数据迁移（不加实体/列，仅加字典选项）；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — CLOSED_FINAL 字典增量 + 期间状态机 + 前置检查 + 模块关账编排

Status: planned
Targets: `module-finance/model/app-erp-finance.orm.xml`（erp-fin/period-status 加 CLOSED_FINAL(50) 选项）、`module-finance/erp-fin-service/.../entity/ErpFinAccountingPeriodBizModel.java`(新/扩)、`.../biz/IErpFinPeriodCloseBiz.java`(新)、`.../biz/IErpFinAccountingPeriodStatusBiz.java`(新)、`.../ErpFinErrors.java`(扩)、`.../ErpFinConstants.java`(扩 PERIOD_STATUS_*/MODULE_CLOSE_*)
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Decision | Proof`
- Prereqs: **人工批准**（model/*.orm.xml ask-first，字典加值）+ 本计划草案审查通过。

- [ ] `Fix`：向 `erp-fin/period-status` 字典加 `CLOSED_FINAL(50)`（label「已复核」），补齐 owner-doc 四态关账机契约漂移；重新 codegen 生成 period-status dict 产物。
  - Skill: none
- [ ] `Explore`：确认 `erp-fin/period-status`/`erp-fin/module-close-status` 码值（OPEN/CLOSING/CLOSED/CLOSED_FINAL/NEVER_OPENED + 模块关账态）与 subject-class 损益三类集合 {收入(40)/费用(50)/成本(60)}（已草案审查核实，此处固化映射供 Phase 2/3）。
  - Skill: none
- [ ] `Add`：`IErpFinPeriodCloseBiz.preCheck(periodId)` —— 扫描本期 posted=false 业务单据（跨域查 purchase/sales/inventory/assets/finance 的 posted 字段）、未审核凭证（ErpFinVoucher）、未核销 AR/AP（ErpFinArApItem status!=SETTLED）；产出检查报告。配置 `erp-fin.auto-post-on-close` 决定 posted=false 为阻断(false→阻断)或提示。
  - Skill: `nop-backend-dev`
- [ ] `Add`：期间状态机——`ErpFinAccountingPeriodBizModel` 实现 OPEN→CLOSING（closePeriod 开始锁定）→CLOSED（结账完成）→CLOSED_FINAL（finalizePeriod 最终锁定）；每迁移校验前置态，违例抛 `NopException`+`ErpFinErrors`。CLOSING/CLOSED/CLOSED_FINAL 期间禁止新增/修改凭证（`§期间控制`）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：模块关账编排——`closePeriod` 按 AR→AP→INV→AST→GL 顺序推进 `ErpFinAccountingPeriodStatus` 各模块 status（dict erp-fin/module-close-status），每模块关账前置（上一模块已关账）。**GL 模块内部运行顺序**：先汇兑重估（FX_REVALUATION）再损益结转（PERIOD_CLOSING），使汇兑损益进入当期结转（`§期末结账步骤` 模块顺序 + Phase 3 Decision 时序）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：`TestErpFinPeriodStateMachine`（OPEN→CLOSING→CLOSED→CLOSED_FINAL 正向 + 非法迁移拒绝 + 凭证锁定）、`TestErpFinPeriodPreCheck`（posted=false/未审核/未核销清单 + 阻断/提示模式）、`TestErpFinModuleCloseOrder`（AR→AP→INV→AST→GL 顺序 + 跨序拒绝）。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinPeriod*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付期间状态机 + 前置检查 + 模块关账编排。解除 Phase 2/3 对期间锁定/模块顺序的阻塞。

- [ ] 期间状态迁移 + 前置检查 + 模块关账顺序单测通过；subjectClass/period-status 码值映射已确认

### Phase 2 — 损益结转（PERIOD_CLOSE 凭证）+ TrialBalance 快照

Status: planned
Targets: `module-finance/erp-fin-service/.../entity/ErpFinPeriodCloseBizModel.java`(扩 closePeriod 损益结转段)、`.../service/ProfitLossClosingService.java`(新)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（GL 模块关账阶段 + CLOSED_FINAL 字典 + subjectClass 损益三类映射确认）。

- [ ] `Add`：`ProfitLossClosingService` —— 按 subjectClass 识别**收入类(40)**科目，聚合 `ErpFinGlBalance`（periodId + acctSchemaId）本期 periodCredit(收入贷方发生)，生成 PERIOD_CLOSE(120) 结转凭证（借各收入科目/贷本年利润科目），结转后收入科目余额清零。
  - Skill: `nop-backend-dev`
- [ ] `Add`：按 subjectClass 识别**费用类(50)+成本类(60)**科目（损益支出两类），聚合 periodDebit(借方发生)，生成 PERIOD_CLOSE(120) 结转凭证（借本年利润/贷各费用+成本科目），结转后费用+成本科目余额清零。
  - Skill: `nop-backend-dev`
- [ ] `Add`：结转凭证经 `IErpFinVoucherBiz.post`（businessType=PERIOD_CLOSE）入账；populate `ErpFinTrialBalance` 快照（结转后各科目期末余额）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：结转粒度——**选择**按科目逐行结转（保留科目级明细可追溯），汇总到本年利润单科目。**替代**：合并单行结转（丢失明细，rejected）。**残留风险**：凭证行数多（按科目数，期末一次可接受）。
  - Skill: none
- [ ] `Proof`：`TestErpFinProfitLossClosing`（收入/费用/成本余额→本年利润 PERIOD_CLOSE 凭证 + 借贷平衡 + 结转后收入/费用/成本余额清零 + TrialBalance 快照 + 本年利润净额=收入−费用−成本）。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinProfitLoss*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付损益结转（收入/费用/成本三类）PERIOD_CLOSE 凭证 + TrialBalance 快照。解除 Phase 3 汇兑重估（同在 GL 关账段）的数据基础。

- [ ] 损益结转凭证（含成本类）+ 收入/费用/成本清零 + TrialBalance 快照单测通过

### Phase 3 — 汇兑重估（EXCHANGE_GAIN_LOSS 凭证，承接 0300-3 deferred）+ 折旧集成门控

Status: planned
Targets: `module-finance/erp-fin-service/.../service/ExchangeRevaluationService.java`(新)、`.../entity/ErpFinPeriodCloseBizModel.java`(扩汇兑段 + 折旧门控)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（期间锁定 + 折旧门控）。汇兑重估须在损益结转**之前**执行（GL 关账段运行顺序：FX 重估 → P&L 结转，使汇兑损益进入当期损益结转，见 Decision 时序）。

- [ ] `Add`：`ExchangeRevaluationService` —— 查询外币 `ErpFinArApItem` 未核销项（status!=SETTLED/CANCELLED + currencyId!=本位币），按期末汇率重估：差额 = openAmountFunctional − (openAmountSource × 期末汇率)；正差额=汇兑收益，负=汇兑损失。生成 EXCHANGE_GAIN_LOSS(130) 凭证（借/贷往来科目 + 借/贷汇兑损益科目）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：折旧集成门控——GL 关账段前（AST 模块关账），`erp-ast.auto-depreciation-on-close=true` 时调折旧（SPI `IErpAstDepreciationTrigger.executeBatch(period)` 或直接 `IErpAstDepreciationScheduleBiz`，按 Phase 1 Decision）；1000-2 未落地时跳过并告警（记录 AccountingPeriodStatus.assetStatus 但不阻断）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：汇兑重估时序——**选择**结转损益**前**重估（重估产生的汇兑损益参与当期损益结转）。**替代**：结转后重估（汇兑损益留至下期，不符合配比，rejected）。**残留风险**：重估依赖期末汇率配置（缺失抛 NopException 提示）。
  - Skill: none
- [ ] `Proof`：`TestErpFinExchangeRevaluation`（外币 AR/AP 重估 + 正/负差额 + EXCHANGE_GAIN_LOSS 凭证 + 本位币项不重估）、`TestErpFinDepreciationIntegration`（auto-depreciation=true 调折旧 / 1000-2 未落地跳过告警不阻断）。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinExchange*,TestErpFinDepreciationIntegration*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 3 交付汇兑重估 EXCHANGE_GAIN_LOSS 凭证（承接 0300-3 deferred）+ 折旧集成门控。解除 Phase 4 反结账的凭证冲销对象。

- [ ] 汇兑重估凭证 + 折旧门控单测通过

### Phase 4 — 反结账（reverseClose）+ 端到端 + 红字冲减

Status: planned
Targets: `module-finance/erp-fin-service/.../entity/ErpFinPeriodCloseBizModel.java`(扩 reverseClose)
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1/2/3（结转/汇兑凭证已生成）。

- [ ] `Add`：`reverseClose(periodId)` —— 校验 CLOSED_FINAL + 配置门控 `erp-fin.reverse-close-approval-required`；状态 CLOSED_FINAL→CLOSING→OPEN；调 `IErpFinVoucherBiz.reverse` 冲销本期 PERIOD_CLOSE(120) 结转凭证 + EXCHANGE_GAIN_LOSS(130) 汇兑凭证（+ 条件冲销 DEPRECIATION(70) 折旧凭证，若 `erp-ast.auto-depreciation-on-close=true` 已生成折旧凭证，`§反结账步骤4`）（红字）；恢复收入/费用/成本科目余额；解锁凭证编辑；记录审计（`§反结账步骤`）。
  - Skill: `nop-backend-dev`
- [ ] `Add`：反结账后允许重新 closePeriod（幂等——重新结转生成新凭证，`§反结账 §步骤7`）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：反结账粒度——**选择**整体反结账（冲销全部结转/汇兑凭证 + 回开期间）。**替代**：按凭证逐笔选择冲销（复杂度高，运营低频，rejected）。**残留风险**：反结账影响已出具报表（`§反结账约束`，由配置门控审批兜底）。
  - Skill: none
- [ ] `Proof`：`TestErpFinReverseClose`（CLOSED_FINAL→OPEN + 结转/汇兑/折旧凭证红字冲销 + 收入/费用/成本余额恢复 + 凭证解锁 + 重新结账）、端到端 `TestErpFinPeriodCloseEndToEnd`（前置检查→模块关账→汇兑重估→损益结转→finalize→反结账→重新结账全链）。`mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinReverseClose*,TestErpFinPeriodCloseEndToEnd*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 4 交付反结账 + 端到端全链。完整仓库验证属 Closure Gates。

- [ ] 反结账（结转/汇兑/折旧凭证红冲 + 余额恢复 + 期间回开 + 重新结账）+ 端到端全链单测通过

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0dfb68c9bffeOCFOf0ksknE2ta`，独立 general 子代理，对照实时仓库复核）。2 BLOCKER：(B1) `erp-fin/period-status` 字典仅 OPEN/CLOSING/CLOSED/NEVER_OPENED，**缺 CLOSED_FINAL**（owner-doc 契约漂移），且计划 Task Route「纯服务层不触及 ORM」前提虚假——加 CLOSED_FINAL 须 ask-first ORM 字典编辑；(B2) 损益结转仅识别收入(40)/费用(50)，**漏成本(60)**（中国准则损益类含成本如主营业务成本，否则本年利润不平）。S 级 nit：SPI 逻辑反转（finance 是 DAG 顶可正向 R 依赖 assets 直接注入，`data-dependency-matrix.md:81`）、ErpInvCostLayer/Batch 表已存在、反结账漏折旧凭证冲销（`§反结账步骤4`）。**已修订**：Current Baseline 补 CLOSED_FINAL 缺失 + 契约漂移 + subject-class 三类码值 + DAG 方向 + 成本表已存在；Task Route 改 Type 承认 ask-first 字典增量 + 新增 CLOSED_FINAL Decision（选择加值对齐 owner-doc）；损益结转改收入(40)+费用(50)+成本(60) 三类（Goal/Phase2/Decision/Proof 全改）；折旧集成改直接注入（去 SPI）；Phase 1 加 Fix CLOSED_FINAL 字典项 + ask-first 门控；Phase 4 反结账加条件冲销折旧凭证；Closure Gates 加保护区域门控。
- Independent draft review iteration 2: **needs revision（部分误报，已防御性澄清）**（`ses_0dfaa2806ffel2e4DFbdCLI6VZ`，独立 general 子代理）。iteration-1 B1(CLOSED_FINAL)/B2(COST 60) **确认已解决**。新提 1 BLOCKER（B1）：称业务类型字典 code 为 PERIOD_CLOSING/FX_REVALUATION 应替换 PERIOD_CLOSE/EXCHANGE_GAIN_LOSS。**经核实为误报**：`ErpFinBusinessType.getCode()` 返回 int（120/130），过账经 `ErpFinPostingProcessor:286 setBusinessType(businessType.getCode())` 存数值；字典 code 字符串仅元数据/UI，Java 实现引用枚举常量名 PERIOD_CLOSE/EXCHANGE_GAIN_LOSS（枚举 javadoc 明示「常量 code 与字典数值逐一一致」）。替换为 PERIOD_CLOSING 反而破坏编译（枚举常量名为 PERIOD_CLOSE）。**防御性处理**：Current Baseline 补注枚举常量名 vs 字典 code 字符串区分，避免实施者混淆。S 级 nit（Phase 3 prereq「可选 Phase 2」触反松弛 + FX→P&L 运行顺序未在 Phase 1 编排骨干写明）**已修订**：去「可选」+ Phase 3 prereq 明定 FX 重估先于 P&L 结转 + Phase 1 GL 模块编排补运行顺序。
- Independent draft review iteration 3: <待独立子代理复审>

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [ ] 范围内行为完成：期间状态机 + 前置检查 + 模块关账编排 + 损益结转 + 汇兑重估 + 折旧门控 + 反结账 + 端到端，行为测试通过
- [ ] 相关文档对齐：`core-business-roadmap.md` M4.3 标注 done；当日日志已记；`period-close.md` 偏离（成本核算/费用摊销/年度结转/报表 Non-Goal）补注；`ar-ap-reconciliation.md` 汇兑 deferred 标记承接完成
- [ ] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`（改动模块）
- [ ] 无范围内项目静默降级（成本核算/费用摊销/年度结转/报表/自动核销/兜底重过账均为计划内 Non-Goal）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 保护区域（erp-fin/period-status 字典加 CLOSED_FINAL）实施前已获人工批准
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 存货成本核算集成（period-close §步骤2）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: inventory 域成本计算（移动加权/FIFO/批次）未落地（costing-methods.md 设计未实现）；本计划 config-gated 跳过。
- Successor Required: yes（触发条件：inventory 成本核算落地时接线 step2）

### 银行存款外币汇兑重估

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划仅重估外币 AR/AP 往来；银行存款外币重估需科目级币种标记。
- Successor Required: yes（触发条件：多币种银行对账/资金面落地时）

### 年度结转（本年利润→未分配利润 + 辅助账跨年 + 次年期间创建）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 年末结转属独立结果表面（涉及辅助账跨年结转 + 次年 12 期间自动创建）；本计划仅覆盖月度结账。
- Successor Required: yes（触发条件：跨年运营需求时）

### 结账报告渲染（nop-report）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划 populate TrialBalance 快照数据；科目余额表/试算平衡表/账龄报表渲染属 nop-report 报表面。
- Successor Required: yes（触发条件：nop-report 接线时）

## Closure

Status Note: <待草案审查 + 各阶段执行 + 独立结束审计后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理>
- Evidence: <task id / log link>

Follow-up:

- 存货成本核算集成（见上方 Deferred）
- 银行存款外币汇兑重估（见上方 Deferred）
- 年度结转（见上方 Deferred）
- 结账报告渲染（见上方 Deferred）
