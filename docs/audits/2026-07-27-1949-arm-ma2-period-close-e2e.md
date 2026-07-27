# MA2 期末结账端到端审计报告（A2.3）

> 审计 plan：`docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`
> 里程碑：MA2（业务正确性层）— 工作项 A2.3
> 维度：业财端到端（多维上下文审计）
> 域/功能模块：finance / 期间结账（前置检查 → AR/AP/INV/AST/GL 模块关账 → 损益结转 → 汇兑重估 → 终关；反结账；年度结转）
> 审计方法：`docs/skills/multi-dimensional-audit-prompt.md`（项目定制化层已注入：会计保护区域、`mvn clean install -DskipTests` 验证、`ErpFin*` 命名、`erp.err.fin` ErrorCode、已知失败模式）
> 审计日期：2026-07-27
> 审计对象：实时仓库（HEAD 同 `docs/audits/compliance-baseline.md §M0 锚点` 一线，审计不改代码）

## 0. 综合裁决

**needs revision（P0 须走即时通道）**。期末结账全链组件齐备、模块关账顺序与状态机推进正确、损益结转借贷平衡且收入/费用/成本三类余额归零（单币种无 FX 场景）、汇兑重估每张凭证自平衡（`CloseVoucherWriter:81-85` 强制平衡校验）、年度结转结构完整。但多维审计发现 **1 项 P0 实时业务正确性缺陷**（汇兑重估生成的汇兑损益费用类科目余额未结转至本年利润，违反 owner doc「结转后损益类科目余额归零」）+ 6 项 P1 + 3 项 P2 watch-only。P0 已异步注入独立 fix plan（触及 finance 损益结账保护区域，按 `project-context.md §AI 阻塞条件` 须人工确认 + 独立审计，不在本审计 plan 内就地修复）。

**已登记 MA1 finding 运行时影响复核结论**：P1-MA1-016（reverseDepreciation 跨域 DAO）/ P1-MA1-017（owner doc 跨域编排规则不完整）/ P1-MA1-018（enum↔dict 漂移）**均不升级**——期末结账内部查询一致使用 `enum.name()`，无对账漏算；跨域 command 编排在 I*Biz 层合法。**C-11 已自然消解**——`flow-overview.md:499` 现文本为「期末结账 | 单库事务（REQUIRED） | 强一致性」，已无「分布式事务」措辞。

## 1. 链路覆盖矩阵

| 链路段 | 实现位置（实仓核实） | owner doc 锚点 | 覆盖裁决 |
|--------|---------------------|---------------|---------|
| 期间状态机 OPEN→CLOSING→CLOSED→CLOSED_FINAL | `ErpFinAccountingPeriodProcessor.assertPeriodStatus` / `closePeriod` / `finalizePeriod` | `period-close.md §期间控制` | ⚠️（CLOSING 中间态不可观测，见 §3.10/P2-MA2-025） |
| 前置检查（未过账/未核销/未处置异常/坏账 allowance） | `preCheck` + `populateAllowanceCheck` + `findUnpostedVoucherCodes`/`findUnsettledArApCodes`/`findUnresolvedPostingExceptionKeys` | `period-close.md §结账前置检查`（6 项） | ⚠️（缺「折旧已执行」「成本核算完成」2 项硬检查实现；AR-AP 未核销阻断分级与 owner doc 不一致，见 P1-MA2-017） |
| 模块关账顺序 AR→AP→INV→AST→GL | `closePeriod:143-147` + `Module.predecessor()` 前置依赖守卫 | `period-close.md §期末结账步骤` + 向导 §步骤映射 | ✅ |
| INV 成本兜底重算（finance→inventory command） | `closeInvModule`/`recloseInvCosts` 调 `IErpInvCostingBiz.reclosePeriodCosts` | `period-close.md §步骤2` + `data-dependency-matrix.md`（DAG 合法） | ✅（编排正确；成本算法归 A2.4） |
| AST 折旧（finance→assets command） | `closeAssetModule`/`runDepreciation` 调 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation` | `period-close.md §步骤3` | ✅（编排正确；折旧算法归 A4.3） |
| GL 汇兑重估 | `closeGlModule`/`ExchangeRevaluationService.revalue`（AR/AP + 银行存款，config-gated） | `period-close.md`（0300-3 deferred + 0540-2 扩展） | ⚠️（凭证自平衡 ✅；但汇兑损益余额未结转 P0-MA2-016；无前期 reversal P1-MA2-022） |
| GL 损益结转 | `ProfitLossClosingService.close`（收入/费用/成本三类 → 本年利润） | `period-close.md §步骤5` | ⚠️（单币种余额归零 ✅；FX 汇兑损益被过度排除 P0-MA2-016） |
| 试算平衡表快照 | `populateTrialBalanceForAllSchemas`（多账套分组聚合） | `period-close.md §步骤8` | ✅ |
| 终关 CLOSED_FINAL | `finalizePeriod` | `period-close.md §期间控制` | ✅ |
| 反结账（红冲 PL/FX/ANNUAL + 条件折旧 + 重开模块） | `reverseClose` + `reverseCloseVoucher` + `reopenModules` | `period-close.md §反结账流程`（8 步概念） | ⚠️（approval 为 kill-switch P1-MA2-020；TB 快照残留 P2-MA2-023；成本凭证红冲为 Non-Goal 不覆盖） |
| 年度结转（本年利润→未分配利润 + 年初余额 populate + 次年期间） | `closeAnnual` + `AnnualCloseService.executeAnnualClose`/`populateNextYearOpening` + `generateNextYearPeriods` | `period-close.md §年度结转规则` | ⚠️（年初余额非累计 P1-MA2-018；辅助账对账作用域不匹配 P1-MA2-019；幂等 ✅） |
| 期间状态机 CLOSED_FINAL 凭证锁定 | （无实现——`ErpFinVoucherBizModel` 不检查期间状态） | `period-close.md §期间控制`（CLOSED_FINAL 不可修改） | ⚠️（承诺但无证据 P1-MA2-021） |

## 2. 已识别 7 控制点裁决汇总

| # | 控制点 | 裁决 | 证据/Finding |
|---|--------|------|-------------|
| 1 | 前置检查完整性（6 项 + 阻断/提示分级） | ⚠️ FAIL | 实现 4/6 项（未过账/未核销/未处置异常/坏账 allowance）；缺「折旧已执行」「成本核算完成」2 项硬检查（由 config-gated 自动执行间接覆盖，但 preCheck 报告未体现）；阻断分级不一致 → P1-MA2-017 |
| 2 | 模块关账顺序与隔离性（AR→AP→INV→AST→GL + 中途失败事务边界） | ✅ PASS | `Module.predecessor()` 守卫乱序；单库事务 REQUIRED（`flow-overview.md:499`），中途失败整体回滚；per-module status 转移正确 |
| 3 | 损益结转余额归零（收入/费用/成本三类 → 本年利润） | ⚠️ FAIL（FX 场景） | 单币种场景三类归零 ✅（`TestErpFinProfitLossClosing` 证实）；FX 场景汇兑损益（费用类）被排除未结转 → **P0-MA2-016** |
| 4 | 汇兑重估借贷平衡 | ✅ PASS（per-voucher） | `CloseVoucherWriter:81-85` 强制 totalDebit==totalCredit 否则抛错；每张 FX 凭证自平衡 |
| 5 | 反结账完整性（8 步 + P1-MA1-016 运行时复核） | ⚠️ PARTIAL | PL/FX/ANNUAL 红冲 + 条件折旧 + 重开模块 ✅；approval 为 kill-switch P1-MA2-020；TB 残留 P2-MA2-023；P1-MA1-016 跨域 DAO **仅治理问题**（只读查询语义正确，无数据错误）✅ |
| 6 | 年度结转（本年利润→未分配利润 + 年初余额 + 辅助账对账 + 次年期间幂等） | ⚠️ FAIL | 本年利润清零 ✅；次年期间幂等 ✅；但年初余额非累计 P1-MA2-018；辅助账对账作用域不匹配 P1-MA2-019 |
| 7 | 期间状态机纪律（CLOSED_FINAL 锁定 + 反结账阻止） | ⚠️ FAIL | 状态转移合法性 ✅；次年期间已创建阻止反结账 ✅；但 CLOSED_FINAL 凭证锁定未实现 P1-MA2-021 |

## 3. 多维审计逐维裁决

> 反窄化自检：已对下方全部维度各给出至少一句裁决（含「本维度无发现」）。每维结论以实仓代码/测试/owner doc 三方对齐为依据。

### 3.1 维度「需求正确性」— ⚠️

对照 `period-close.md` 前置检查 6 项 + 8 步结账 + 反结账 8 步 + 年度结转，实现声明的流程与范围基本不偏离。**「承诺但无证据」控制点**：

- **CLOSED_FINAL 期间凭证锁定（不可修改）**：owner doc `§期间控制` 明示 CLOSED_FINAL「可修改凭证=否」。但 `ErpFinVoucherBizModel.postVoucher`/`reverseVoucher` 仅校验凭证自身 `docStatus`（DRAFT/POSTED），不校验期间状态；继承的 `CrudBizModel` 默认 `update`/`delete` 同样不检查期间状态。故 CLOSED_FINAL 期间凭证可被修改/红冲，锁定承诺未落实 → **P1-MA2-021**。
- **前置检查「折旧已执行」「成本核算完成」**：owner doc `§结账前置检查` 列为阻断项，实现 `preCheck` 未产出这两项的结构化检查（折旧/成本由 config-gated 自动执行 `runDepreciation`/`recloseInvCosts` 间接保证，失败告警不阻断）。前置检查报告 `PeriodPreCheckReport` 无对应字段。属实现缩窄，登记 P2（与 P1-MA2-017 一并 MR1 裁决：补结构化字段或更新 owner doc 标注「由 config-gated 自动执行保证」）。

### 3.2 维度「owner-doc 对齐」— ⚠️

逐条核对 `period-close.md` 期间状态机/反结账 8 步/年度结转步骤映射 + `bad-debt.md` allowance 门控 + `budget.md` 结转与 commitment：

- **allowance 门控阻断条件不一致**：owner doc `bad-debt.md §期末 allowance 充足性门控` + `period-close.md §结账前置检查` 明示「必需 > 账面 → shortfall **阻止**结账」。实现 `closePeriod:135-139` 将**所有**前置检查（含 allowance shortfall）的阻断条件化为 `!isAutoPostOnClose()`——即 `auto-post-on-close=true` 时 allowance shortfall 也不阻断。与 owner doc「shortfall 阻断」不一致（绑定到 P1-MA2-017 的 auto-post-on-close 语义）。
- **P1-MA1-017 复核（owner doc §3.2/§4.4 finance 跨域编排规则不完整）**：期末结账 `runDepreciation`/`reverseDepreciation`/`recloseInvCosts` 经 `IBizObjectManager.getBizObject(...).asProxy()` 调 assets/inventory I*Biz mutation。**裁决：command 编排在 I*Biz 层合法**（业务域自管实体的写），owner doc「finance 对业务域纯读不回写」指 ORM 层无反向 to-one，未覆盖 I*Biz 层 command 编排。**P1-MA1-017 终态 = owner doc 文字缺陷，MR1 补注，无运行时影响**。

### 3.3 维度「业务正确性 — 前置检查完整性」— ⚠️（控制点 1）

`preCheck` 覆盖 4/6 项（未过账凭证 / 未核销 AR-AP / 未处置过账异常 / 坏账 allowance shortfall-excess）。`PeriodPreCheckReport` 结构完整（字段 + hasIssues + issueCount）。

**阻断 vs 提示分级不一致**：
- owner doc `§结账前置检查`：AR-AP 未核销 = **提示**（建议结账前完成核销）；坏账 shortfall = **阻断**。
- 实现 `PeriodPreCheckReport.hasIssues():92-96` 将 `unsettledArApCodes` 计入阻断性 issues；`closePeriod` 在 `!isAutoPostOnClose()` 时对**全部** issues（含未核销 AR-AP）抛 `ERR_PRE_CHECK_BLOCKED`。
- **故默认配置（`auto-post-on-close=false`）下，未核销 AR-AP 会阻断结账，与 owner doc「提示」不一致** → **P1-MA2-017**。

### 3.4 维度「业务正确性 — 模块关账顺序与隔离性」— ✅（控制点 2）

`closePeriod:143-147` 按 AR→AP→INV→AST→GL 调用各模块关账；`Module.predecessor():759-772` 对每个模块校验前序模块已达 CLOSED（`advanceModule:400-409` 乱序抛 `ERR_MODULE_OUT_OF_ORDER`）。事务边界：跟随 Facade `@BizMutation`（单库 REQUIRED，`flow-overview.md:499`），中途任一模块失败整体回滚，无脏状态残留（per-module status 的 CLOSING 中间态 `setModuleStatus:407` 与 CLOSED `:408` 连续设置，仅在 flush 后落库，失败回滚不留痕）。per-module status（arStatus/apStatus/invStatus/glStatus/assetStatus）状态转移正确。**本维度无新发现**（CLOSING 不可观测见 §3.10）。

### 3.5 维度「业务正确性 — 损益结转」— ⚠️ FAIL（控制点 3）

`ProfitLossClosingService.close` 按 `erp-md/subject-class` 识别收入(INCOME)/费用(EXPENSE)/成本(COST) 三类，聚合本期发生额，结转方向正确（收入贷方余额→借收入/贷本年利润；费用+成本借方余额→贷费用成本/借本年利润），结转凭证经 `CloseVoucherWriter` 强制借贷平衡。单币种无 FX 场景：`TestErpFinProfitLossClosing.testProfitLossClosing` 证实三类科目结转后净额归零、本年利润=收入−费用−成本。

**FX 场景缺陷（P0）**：`ProfitLossClosingService:88-92` 聚合时排除 `businessType == PERIOD_CLOSE || EXCHANGE_GAIN_LOSS` 的分录。排除 `PERIOD_CLOSE` 正确（防结转凭证自身分录重复结转——其收入/费用科目行若再聚合会重复结转）。但排除 `EXCHANGE_GAIN_LOSS` **过度**：汇兑重估凭证（`ExchangeRevaluationService`）的汇兑损益分录（`CONFIG_FX_GAIN_LOSS_SUBJECT_CODE`，测试 seed `6603` 为 `SUBJECT_CLASS_EXPENSE`，`CloseVoucherWriter:125` 写入 `businessType=EXCHANGE_GAIN_LOSS`）被排除后，**汇兑损益（费用类）余额永不结转至本年利润**。结账后汇兑损益科目残留非零余额，本年利润少计该汇兑净额，违反 owner doc `§步骤5`「结转后收入/费用科目余额归零」+ 控制点 3 → **P0-MA2-016**。

证据链（`TestErpFinPeriodCloseEndToEnd` seed + `period-close-end-to-end-test.yaml`）：AR 外币项 openSource=100/openFunctional=800、periodEndRate=8.5 → revaluedFunctional=850、diff=800−850=−50 → 应收 diff<0=收益 → 借 AR 50 / 贷汇兑损益 50。汇兑损益产生贷方余额 50（收益），被 P&L 结转排除 → 残留。该测试未断言汇兑损益净额归零，故缺陷未被发现。

### 3.6 维度「业务正确性 — 汇兑重估」— ⚠️（控制点 4）

`ExchangeRevaluationService` 重估 AR/AP 外币未核销项 + 银行存款外币余额（config-gated `erp-fin.bank-fx-revaluation-enabled`），差额公式 `diff = openAmountFunctional − (openAmountSource × 期末汇率)`，方向映射（应收资产升值=收益 / 应付负债升值=损失）正确。每张 FX 凭证 `CloseVoucherWriter:81-85` 强制借贷平衡 ✅。

**P1-MA1-018 enum 漂移运行时复核**：代码以 `ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name()`（="EXCHANGE_GAIN_LOSS"）持久化（`ExchangeRevaluationService:152,213`）与内部查询（`reverseCloseVoucher:610`、`ProfitLossClosingService:88-92`）一致。UI dict `erp-fin/business-type` value 为 `FX_REVALUATION`，仅影响 **UI 筛选下拉值与 DB 存储值不符**（用户按 dict 筛选凭证漏命中），**期末结账内部对账（凭证汇总 vs 总账）查询一致使用 enum.name()，无漏算**。**P1-MA1-018 终态 = 不升级，维持 P1（UI/查询一致性），MR1 修复**。

**新发现 P1-MA2-022（FX 无前期 reversal + 无期间过滤）**：`revalueArAp:106-108` 查询所有未核销外币项**不按期间过滤**（重估所有开放项），且重估后**不更新** `openAmountFunctional`、**不 reversal 前期 FX 凭证**。故每月结账对同一批开放项按新汇率重估，前期已入账的汇兑损益不冲回，累计漂移（非 IAS 21 spot-rate 标准的「前期重估期末自动 reversal」语义）。config-gated 且 owner doc 未显式规定 reversal 策略，登记 P1。

### 3.7 维度「业务正确性 — 反结账完整性」— ⚠️ PARTIAL（控制点 5）

`reverseClose` 实现 §反结账 8 步概念模型的核心：红冲 PL/FX 凭证（`reverseCloseVoucher:294-295`）+ 年末条件红冲 ANNUAL（:296-299）+ 条件折旧红冲（:300-302，config-gated）+ 重开各模块 status（`reopenModules:306`）+ 回开期间 OPEN（:291）。年末次年期间已创建时阻止反结账（:284-288）✅。

**P1-MA1-016 运行时正确性复核**：`reverseDepreciation:381-396` 经 `daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q)`（:385,389）跨域 DAO 查询 assets 实体。**裁决：仅治理违规**（跨域查询应经 `IErpAstDepreciationScheduleBiz.findList()`），**只读查询语义正确，不产生数据错误**——查询条件（period + posted=true）正确识别冲销对象，逐项调 `depreciationBiz.reverseDepreciation`。**P1-MA1-016 终态 = 维持 P1（治理），MR1 修复，无运行时数据正确性升级**。

**新发现**：
- **P1-MA2-020（approval kill-switch）**：`reverseClose:278-281` 在 `isReverseCloseApprovalRequired()`（默认 true）时直接 `throw ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`。即默认配置下反结账**完全不可用**；将 config 置 false 则**无条件放行无审批**。owner doc `§反结账约束` 要求「管理员 + 审批」，实现无审批流（仅 kill-switch），与 owner doc「高权限+审批门控」不符。
- **P2-MA2-023（TB 快照残留）**：`reverseClose` 未清除 `populateTrialBalanceForAllSchemas` 写入的 `ErpFinTrialBalance` 快照行，反结账后期间残留 CLOSED 态试算平衡快照（重新结账时 `populateTrialBalanceForAllSchemas:467-471` 先清后写会覆盖，故影响有限）。
- **成本凭证红冲**：owner doc `§反结账步骤5` 处理成本凭证；实现不覆盖（INV 成本兜底重算不产生 finance 侧可红冲的期间凭证，归 owner doc 已裁定的 Non-Goal「成本核算方法」范围，不作为 finding）。

### 3.8 维度「业务正确性 — 年度结转」— ⚠️ FAIL（控制点 6）

`closeAnnual` + `AnnualCloseService`：本年利润→未分配利润结转（`transferProfitToRetainedEarnings`，本年利润清零，PROFIT_TO_RETAINED_EARNINGS）✅；次年期间创建 `generateNextYearPeriods` 幂等（`period-generate-skip-existing` config）✅；反结账覆盖年度结转凭证红冲（`reverseClose:296-299`）✅。

**新发现**：
- **P1-MA2-018（年初余额非累计）**：`populateNextYearOpening:148` 经 `aggregateYearSubjectActivity(year)` 聚合**本年度**已过账分录净额写入次年 `ErpFinGlBalance.yearOpeningDebit/Credit`。对资产负债类科目，此为「本年净变动」而非「累计期末余额」——**第 2 年及以后年度结转的年初余额缺失上年结转额**。根因：`ErpFinGlBalance` 当前阶段未由过账引擎维护（`ProfitLossClosingService:42-43`、`AnnualCloseService:49-50` 注释明示），无上年年初可承接。首年结转（期初为零）正确；多年结转错误。登记 P1（受 GlBalance 未维护架构限制约束，MR1 裁决：补 GL 余额维护或文档化为已知简化）。
- **P1-MA2-019（辅助账对账作用域不匹配）**：`assertAuxiliaryReconciles:199-200` 的 AR/AP 辅助账合计经 `sumArApOpenFunctional:232-247` 汇总**全部**（无年度过滤）开放项 `openAmountFunctional`；而 GL 侧 `subjectNetForYear:266-281` 仅聚合**本年度**分录。辅助账=全历史开放项 vs GL=本年发生，作用域不一致，跨年场景假阳性/假阴性。登记 P1。

### 3.9 维度「业务正确性 — 期间状态机纪律」— ⚠️（控制点 7）

OPEN→CLOSING→CLOSED→CLOSED_FINAL 转移经 `assertPeriodStatus` 守卫合法性 ✅；次年期间已创建阻止年末反结账 ✅。**CLOSED_FINAL 凭证锁定未实现**（见 §3.1 P1-MA2-021）。**已上报税务等下游引用阻止反结账**：owner doc `§异常处理` 提及，实现无对应检查（无「已上报税务」状态字段），属 owner doc 已裁定的 Non-Goal 范围（无下游引用追踪），不作为 finding。

### 3.10 维度「架构或边界影响」— ✅

期末结账跨域编排（finance→assets `executeBatchDepreciation`/`reverseDepreciation` + finance→inventory `reclosePeriodCosts`）经 I*Biz command，DAG 合法（finance R→ assets/inventory，对齐 `data-dependency-matrix.md`）。事务隔离：期末结账跟随 Facade `@BizMutation` 单库 REQUIRED（`flow-overview.md:499`），与 `§6.1` 业财过账 Facade `REQUIRES_NEW`（`ErpFinVoucherBizModel.post/reverse`）分层清晰——期末结账内部生成的 PL/FX/ANNUAL 凭证经 `CloseVoucherWriter` 直接持久化（非经 `IErpFinVoucherBiz.post` Provider 模型，`CloseVoucherWriter:18-22` 注释说明：期末凭证来自余额聚合无源单据），红冲经 `voucherBiz.reverse`（REQUIRES_NEW）。

**C-11 复核（flow-overview:499 分布式事务描述过时）**：实仓 `flow-overview.md:499` 现文本为「期末结账 | 单库事务（REQUIRED） | 强一致性」，:498 为「单据审核 + 凭证生成 | 同步调用（REQUIRES_NEW 独立事务隔离）」。**已无「分布式事务」措辞**（grep `分布式事务` 在 `flow-overview.md` 零命中；唯一命中在 `inventory/cross-domain.md:174`，非 C-11 范围）。**C-11 终态 = 已自然消解/closed**。

**P2-MA2-025（CLOSING 中间态不可观测）**：`closePeriod:157-158` 连续 `setStatus(CLOSING)` 再 `setStatus(CLOSED)`，CLOSING 态永不在 flush 前对外可见 → 并发结账同一期间无法靠 CLOSING 态互斥（两个并发 closePeriod 都看到 OPEN）。交接 **A2.17 并发与乐观锁审计**（owner doc `§期间约束` 定义 CLOSING 为真实态，实现未体现其并发互斥语义）。

### 3.11 维度「验证充分性」— ⚠️

期末结账 E2E（`TestErpFinPeriodPreCheck` + `TestErpFinProfitLossClosing` + `TestErpFinPeriodCloseEndToEnd` + `fin-period-close-wizard.action.spec.ts`）覆盖黄金路径 + 反结账 + 重新结账。对每个关键断言问「如果它假了，我怎么知道？」：

- 「收入/费用/成本结转后净额为 0」（`TestErpFinProfitLossClosing:88-90`）— 单币种无 FX 场景，**未覆盖汇兑损益归零** → P0-MA2-016 未被捕。
- 「汇兑重估凭证已生成」（`TestErpFinPeriodCloseEndToEnd:41-42`）— 仅断言凭证存在，**未断言汇兑损益科目结转后净额归零**也未断言 FX 凭证借贷平衡（虽 `CloseVoucherWriter` 强制平衡）。
- **年初余额 populate 一致性** — 无 12 月年度结转 E2E（`TestErpFinPeriodCloseEndToEnd` 用 6 月），P1-MA2-018/019 未被覆盖。
- **辅助账对账门控** — 无对账失败场景测试，P1-MA2-019 未被覆盖。
- **allowance shortfall 阻断** — 无坏账 shortfall 场景测试（`populateAllowanceCheck` 在 allowance 科目未配时 catch 跳过）。
- **反结账红冲完整性** — `TestErpFinPeriodCloseEndToEnd:56-58` 断言反结账后 OPEN + 重新结账生成新凭证，但**未断言旧 PL/FX 凭证 isReversed=true**。

裁决：验证覆盖黄金路径但**关键控制点（FX 汇兑损益归零、年初余额一致性、辅助账对账、allowance 阻断、反结账红冲完整性）断言不足**，是 P0-MA2-016 漏网的主因。MR1 补强测试。

### 3.12 维度「回归风险」— ⚠️

寻找「仅偶然通过狭窄验证」的期末结账代码：

- **年度结转仅在非 12 月测试**：`TestErpFinPeriodCloseEndToEnd` 用 6 月，年度结转分支（`isYearEnd`）从未被单测触发 → P1-MA2-018/019 漏网。
- **汇兑重估仅在单一外币 + 单一 AR 项场景**：`revalueArAp` 多外币/多项聚合、银行存款重估、AR+AP 混合未覆盖。
- **反结账仅在无次年期间 + approval=false 场景**：年末次年期间已创建阻止反结账（:284-288）未测；approval kill-switch 默认行为未测。
- **模块关账顺序失败回滚仅在第一模块失败隐式覆盖**：中途（如 AST）失败的事务回滚无显式测试。
- **`aggregateBankSubjectBookFunctional:236` 全表扫描**（`findAllByQuery(new QueryBean())` 后 Java 过滤）— 数据量大时性能崩溃，应 `in("voucherId", voucherIds)` → **P2-MA2-024**。

### 3.13 维度「路由和技能选择正确性」— ✅

期末结账向导（纯 UI 编排既有 Facade mutation，`period-close.md §期末结账向导` 明示零后端 delta）+ Facade mutation（preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods）+ 跨域 command 编排（executeBatchDepreciation/reclosePeriodCosts）。任务路由与技能选择（`multi-dimensional-audit-prompt.md`）与工作类型（业财端到端多维审计）匹配。`ErpFinAccountingPeriodBizModel` Facade + `ErpFinAccountingPeriodProcessor` 两层结构符合 `processor-extension-pattern.md`。**本维度无发现**。

### 3.14 维度「待办或自主权策略漂移」— ✅

`period-close.md` 实现范围注记声明的 Non-Goal 裁定（BATCH/INDIVIDUAL/LIFO 计价、费用摊销/待摊费用、年度报表渲染、利润分配明细、多账套/合并报表年度结转、历史年度追溯结转）在代码中无声扩大或缩窄范围检查：

- 费用摊销（步骤4）：owner doc 标注「模块未落地」，实现 `closeGlModule` 无费用摊销步骤 — 一致 ✅。
- 年度报表（步骤6）：owner doc 标注「nop-report 报表面」，实现不覆盖 — 一致 ✅。
- 多账套年度结转：owner doc Non-Goal，`AnnualCloseService` 经 `SchemaPropagator.resolveTargetSchemas` 逐账套循环（多账套并行传播），但年度结转仍按单账套语义聚合 — 在多账套 config 关闭时（默认）等价单账套，未无声扩大 ✅。

**本维度无发现**（实现范围与 owner doc Non-Goal 裁定一致）。

## 4. Finding 清单（按严重性排序）

### P0（即时通道）

| Finding ID | 描述 | 证据 | 修复路径 | 修复 plan | 修复状态 |
|-----------|------|------|---------|----------|---------|
| **P0-MA2-016** | **汇兑损益（费用类）余额未结转至本年利润**：`ProfitLossClosingService:88-92` 聚合排除 `businessType==EXCHANGE_GAIN_LOSS` 分录，致汇兑重估凭证的汇兑损益科目（`CONFIG_FX_GAIN_LOSS_SUBJECT_CODE`，seed `6603` EXPENSE 类）余额永不结转。结账后汇兑损益残留非零余额、本年利润少计汇兑净额，违反 owner doc `period-close.md §步骤5`「结转后收入/费用科目余额归零」+ 控制点 3。排除 `PERIOD_CLOSE` 正确（防结转凭证自身重复结转），排除 `EXCHANGE_GAIN_LOSS` 过度。 | `ProfitLossClosingService.java:88-92`；`ExchangeRevaluationService.java:143-145,205-207`（FX 凭证命中汇兑损益）；`CloseVoucherWriter.java:125`（line.businessType=EXCHANGE_GAIN_LOSS）；`PeriodCloseTestSupport.java:52`（6603 seeded EXPENSE）；`period-close-end-to-end-test.yaml:4` periodEndRate=8.5 触发 FX；`TestErpFinPeriodCloseEndToEnd` 未断言汇兑损益归零 | 触及 finance 损益结转保护区域 → **异步注入独立 fix plan**（按 `project-context.md §AI 阻塞条件` 须人工确认 + 独立 plan-audit + closure-audit；不在本审计 plan 就地修复）。修复方向：从 `ProfitLossClosingService:89-90` 排除条件移除 `EXCHANGE_GAIN_LOSS`（保留 `PERIOD_CLOSE`），补「FX 场景汇兑损益结转后归零」测试 | `docs/plans/2026-07-27-1949-arm-fix-p0-ma2-016-fx-gain-loss-pl-closing.md` | `fix-plan-injected (protected area gate)` |

### P1（待 MR1）

| Finding ID | 描述 | 修复方式 |
|-----------|------|---------|
| **P1-MA2-017** | **auto-post-on-close doc/code 默认值 + 语义双重偏离 + AR-AP/allowance 阻断分级不一致**：(a) `ErpFinConstants:113-114` + `ErpFinAccountingPeriodProcessor:681-684` 代码默认 `false`（阻断），`period-close.md:285` owner doc 默认 `true`；(b) 语义偏离——owner doc 描述为「结账时自动触发未过账单据过账」（动作），代码用作「阻断(false)/提示(true)」门控；(c) `PeriodPreCheckReport.hasIssues():92-96` 将未核销 AR-AP 计入阻断 issues，默认 config 下未核销 AR-AP 阻断结账，与 owner doc `§结账前置检查`「未核销=提示」不一致；(d) allowance shortfall 阻断被绑到 `!isAutoPostOnClose()`，`auto-post-on-close=true` 时 shortfall 不阻断，与 `bad-debt.md`「shortfall 阻断」不一致。 | MR1 统一：裁决 owner doc 默认值（推荐代码 false=阻断更安全，改 owner doc 默认为 false）+ 拆分语义（auto-post 动作 vs pre-check 阻断门控应解耦为两 config）+ 未核销 AR-AP 移出 `hasIssues` 阻断集（改为结构化提示）+ allowance shortfall 独立硬阻断（不受 auto-post-on-close 影响） |
| **P1-MA2-018** | **年初余额 populate 非累计（多年结转错误）**：`AnnualCloseService.populateNextYearOpening:148` 经 `aggregateYearSubjectActivity(year)` 仅聚合本年度分录净额写入次年 `yearOpeningDebit/Credit`。资产负债类科目缺上年结转额，第 2 年及以后年度结转年初余额错误（首年期初为零故正确）。受 `ErpFinGlBalance` 未由过账引擎维护的架构限制约束。 | MR1 裁决：补 GL 余额维护（过账引擎写入 opening/closing）使年初余额=累计期末，或文档化为已知简化 + owner doc 标注「当前仅首年精确」 |
| **P1-MA2-019** | **辅助账跨年对账作用域不匹配**：`AnnualCloseService.assertAuxiliaryReconciles:199-200` AR/AP 辅助账经 `sumArApOpenFunctional:232-247` 汇总全历史开放项（无年度过滤），GL 侧 `subjectNetForYear:266-281` 仅本年发生，作用域不一致，跨年场景假阳性/假阴性。 | MR1 统一作用域：辅助账按年度过滤或 GL 改累计余额（与 P1-MA2-018 一并裁决） |
| **P1-MA2-020** | **反结账 approval 为 kill-switch 无审批流**：`ErpFinAccountingPeriodProcessor.reverseClose:278-281` 默认 config `reverse-close-approval-required=true` 时直接 throw，反结账完全不可用；置 false 则无条件放行无审批。owner doc `§反结账约束` 要求「管理员+审批」，实现无审批流。 | MR1 裁决：实现审批流（反结账申请→审批→执行）或 owner doc 标注「当前 config kill-switch，审批流 successor」 |
| **P1-MA2-021** | **CLOSED_FINAL 期间凭证锁定未实现**：owner doc `§期间控制` CLOSED_FINAL「可修改凭证=否」，`ErpFinVoucherBizModel.postVoucher/reverseVoucher` + CrudBizModel update/delete 不检查期间状态，CLOSED_FINAL 凭证可被修改/红冲。「承诺但无证据」控制点。 | MR1 补期间状态守卫（postVoucher/reverseVoucher/update/delete 前校验 period.status != CLOSED/CLOSED_FINAL）或 owner doc 标注「锁定语义经期间状态机 + 操作权限间接保证」 |
| **P1-MA2-022** | **FX 重估无前期 reversal + 无期间过滤（累计漂移）**：`ExchangeRevaluationService.revalueArAp:106-108` 查询所有未核销外币项不按期间过滤，重估后不更新 `openAmountFunctional`、不 reversal 前期 FX 凭证。每月结账对同一批开放项按新汇率重估，前期汇兑损益不冲回，累计漂移（非 IAS 21 spot-rate「前期重估期末自动 reversal」语义）。config-gated。 | MR1 裁决：实现前期 FX 凭证期末自动 reversal + 期间过滤，或 owner doc 标注「当期 spot-rate 重估，无前期 reversal」为已知简化 |

### P2（watch-only / 待 MR 顺手收敛）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| **P2-MA2-023** | **反结账 TB 快照残留**：`reverseClose` 未清除 `ErpFinTrialBalance` 快照行，反结账后期间残留 CLOSED 态试算平衡快照。重新结账时 `populateTrialBalanceForAllSchemas:467-471` 先清后写覆盖，影响有限。 | watch-only，MR1 顺手在 reverseClose 补 TB 清理 |
| **P2-MA2-024** | **`aggregateBankSubjectBookFunctional` 全表扫描**：`ExchangeRevaluationService:236` `findAllByQuery(new QueryBean())` 加载全部 VoucherLine 后 Java 过滤 `voucherIds.contains`，大数据量性能崩溃。应 `in("voucherId", voucherIds)` 下推。 | watch-only，MR1 顺手改查询过滤下推 |
| **P2-MA2-025** | **CLOSING 中间态不可观测（并发敏感点）**：`ErpFinAccountingPeriodProcessor.closePeriod:157-158` 连续 setStatus(CLOSING) 再 setStatus(CLOSED)，CLOSING 永不在 flush 前对外可见，并发结账同一期间无法靠 CLOSING 互斥。 | watch-only，归 **A2.17 并发与乐观锁审计**（owner doc 定义 CLOSING 为真实态，实现未体现并发互斥） |

## 5. 已登记 MA1 finding + C-11 运行时影响复核表

| Finding | 原描述 | 运行时影响复核（本审计） | 终态 |
|---------|--------|------------------------|------|
| **P1-MA1-016** | `ErpFinAccountingPeriodProcessor.reverseDepreciation:385,389` 跨域 DAO 查询 assets 实体 | 实仓核实精确（`daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q)`，line 385/389）。**仅治理违规**：只读查询语义正确（period + posted=true 过滤），逐项调 `depreciationBiz.reverseDepreciation`，不产生数据错误 | **维持 P1（治理），MR1 修复，无运行时升级** |
| **P1-MA1-017** | owner doc `data-dependency-matrix.md §3.2/§4.4` finance 跨域编排规则不完整 | 期末结账 `runDepreciation`/`reverseDepreciation`/`recloseInvCosts` 经 I*Biz command（`IBizObjectManager.getBizObject(...).asProxy()`）调 assets/inventory mutation。**command 编排在 I*Biz 层合法**（业务域自管实体的写），「纯读」指 ORM 层无反向 to-one | **维持 P1（owner doc 文字缺陷），MR1 补注，无运行时影响** |
| **P1-MA1-018** | finance enum 名 ↔ dict 漂移 4 项含 PERIOD_CLOSE↔PERIOD_CLOSING / EXCHANGE_GAIN_LOSS↔FX_REVALUATION | 期末结账内部查询（`reverseCloseVoucher:610`、`ProfitLossClosingService:88-92`）一致使用 `enum.name()`，凭证汇总 vs 总账对账无漏算。仅影响 UI dict 筛选下拉值与 DB 存储值不符 | **维持 P1（UI/查询一致性），MR1 修复，不升级为运行时对账缺陷** |
| **C-11** | `flow-overview.md:499`「单据审核+凭证生成 = 分布式事务（REQUIRES_NEW）」描述过时 | 实仓 `flow-overview.md:499` 现为「期末结账 \| 单库事务（REQUIRED）」，:498 为「同步调用（REQUIRES_NEW 独立事务隔离）」，**已无「分布式事务」措辞**（flow-overview.md grep 零命中） | **已自然消解/closed**（无需行动；`inventory/cross-domain.md:174` 残留「Nop 分布式事务」措辞非 C-11 范围，归 A2.4/A3.5 inventory owner doc drift） |

## 6. 并发敏感点交接（A2.17）

| 敏感点 | 位置 | 风险 | 交接 |
|--------|------|------|------|
| 并发结账同一期间 | `closePeriod:157-158`（CLOSING 不可观测） | 两并发 closePeriod 都见 OPEN，重复生成 PL/FX 凭证 | A2.17（P2-MA2-025） |
| 并发反结账 | `reverseClose` 无乐观锁 | 两并发 reverseClose 重复红冲 | A2.17 |
| 并发 generateNextYearPeriods | `generateNextYearPeriods:219-267` 无锁 | 两并发调用重复创建（skip-existing=false 时一抛错一成功，非数据损坏） | A2.17 |

## 7. 残留风险

- **P0-MA2-016 未就地修复**：触及 finance 损益结转保护区域，已注入独立 fix plan `2026-07-27-1949-arm-fix-p0-ma2-016-fx-gain-loss-pl-closing.md`，待人工确认 + 独立 plan-audit + closure-audit 后执行。在该 fix plan 闭合前，启用 FX 重估（默认 true）且有外币项的账套结账后汇兑损益科目残留余额。
- **年度结转多年正确性受 GlBalance 架构限制**（P1-MA2-018/019）：当前仅首年年度结转精确，多年需 GL 余额维护支撑。
- **CLOSED_FINAL 凭证锁定 + 反结账审批流**（P1-MA2-020/021）：为控制点「承诺但无证据」，依赖操作权限 + 期间状态机间接保证，无硬守卫。

## 8. 验证基线确认

本审计 plan 不改代码（仅产出报告 + 索引/矩阵 doc 更新 + P0 fix plan 注入）。BUILD_VERIFY 的 `mvn clean install -DskipTests` 作回归基线确认（审计期间无 P0 即时修复触及代码）。详见审计 plan Closure Gates。

## 9. 范围矩阵映射更新

`audit-remediation-scope-and-dimension-matrix.md §2.2`「业财端到端」行 finance/期间结账 相关列：finance 列由 `⚠️P1`（P2P/O2C 共担）推进至 `⚠️(P0→fix-plan + P1)`——期间结账链路组件齐备、模块关账顺序/状态机/损益结转（单币种）/汇兑重估（per-voucher 平衡）经审计确认，但发现 1 项 P0（已注入 fix plan）+ 6 项 P1 + 3 项 P2 watch-only。MA1 finding 运行时复核无升级；C-11 已自然消解。并发敏感点交接 A2.17。
