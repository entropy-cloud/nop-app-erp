# 2026-08-15-1838-3-finance-trial-balance-commitment-exclusion RC-R1.46 — finance 试算平衡 COMMITMENT 排除（MR1 第二批 B 类预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.46（P1-RC-091 finance 试算平衡快照 + 4 GL 重分类/重估服务 exclude BUDGET only 未排 COMMITMENT）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.46 行 + `docs/audits/arm-index.md` P1-RC-091 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 B 类：findPostedVoucherIds 加 COMMITMENT 过滤，降级为预授权自动执行**）
> Related: `docs/design/finance/use-cases.md`（L1 UC-FIN-13 ③）；`docs/design/finance/budget.md`（§规则 4/6/8 + 三通道分离）；`docs/audits/2026-08-06-0847-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`（A4.1.5 运行时证据）；`docs/plans/2026-08-07-1932-2-rc-mr1-r1-1-finance-budget-report-three-channel.md`（RC-R1.1 三通道化先例——本行姊妹站点）
> Audit: required

## Current Baseline

- **finding P1-RC-091（arm-index 行，UC-FIN-13 ③ 试算平衡+GL 重分类聚合维度）**：5 GL 聚合路径共享 BUDGET-only 过滤 `or(isNull("postingType"), ne("postingType", POSTING_TYPE_BUDGET))`——①试算平衡快照 `ErpFinAccountingPeriodProcessor.findPostedVoucherIds:385-394`（:392）+ `populateTrialBalanceForAllSchemas:331-382`（Σdebit→closingDebit/Σcredit→closingCredit :357-378，**Dr==Cr 平衡聚合**）；②年度结转 `AnnualCloseService.java:339`；③损益结转 `ProfitLossClosingService.java:192`；④坏账准备 `BadDebtProvisionService.java:310`（+ :265-269 聚合 credit−debit）；⑤汇兑重估 `ExchangeRevaluationService.java:227`。承付凭证 `CommitmentVoucherGenerator.writeCommitmentVoucher:119-182` 写**单行单边**（header totalDebit=absAmount/totalCredit=0 :143-144 + 单行 debitAmount=absAmount/creditAmount=0 :161-162）+ `docStatus=POSTED`（:149）+ `isReversed=false`（:145）→ 满足 findPostedVoucherIds 全部条件 → 单边 Dr 行累入 Σdebit → **config 开启承付（`budget-commitment-enabled=true`）+ PO approve 时试算平衡快照 ΣclosingDebit > ΣclosingCredit，Dr==Cr 恒等式破坏**。§2 P1④（跨组件口径不一致）。
- **对照（已正确，本行对齐基准）**：控制引擎 `ErpFinBudgetControlBiz.applyPostingTypeFilter:162-164` ACTUAL 通道 `notIn(BUDGET,COMMITMENT)` 正确排两者（P1-MA2-084 fix）+ 预算结转 `ErpFinBudgetScenarioCarryForwardProcessor:221-222` 排两者——5 GL 路径未同步三通道分离修正 = 跨组件口径不一致。
- **非 P0**：config 默认关闭（`isCommitmentEnabled` 默认 Boolean.FALSE）默认部署无承付凭证故默认路径不破坏；L1 UC-FIN-13 ③ 仅要求生成+红冲未规定试算平衡过滤；试算平衡是只读快照不破坏活跃数据/GL 写入/预算硬拦截。**承付运行时覆盖现状（HEAD 核查，非 A4.1.5 旧口径）**：5 GL 路径测试类（`TestErpFinPeriodClose*`/`TestErpFinProfitLossClosing`/`TestErpFinBadDebt`/`TestErpFinAnnualClose`/`TestErpFinExchangeRevaluation`/`TestErpFinBudgetIsolation:64`）**零承付启用**；但全仓存在 **8 个承付启用测试类**（finance：`TestErpFinBudgetCommitment`/`TestErpFinBudgetRollForward`/`TestErpFinBudgetCarryForward` 经 `budget-a2-test.yaml:8` `budget-commitment-enabled: true`；purchase：`TestErpPurOrderCommitment`/`TestErpPurInvoiceCommitmentRestore:164-172`/`TestErpPurReturnCommitmentRestore:179-199`/`TestErpPurReturnCommitmentRelease` 经 `budget-commitment-test.yaml:3`/`return-commitment-test.yaml:3`；sales：`TestErpSalOrderCommitment` 经 `budget-commitment-sales-test.yaml:3`）+ `TestErpFinBudgetEndToEnd:449-451` 直接 seed `postingType=COMMITMENT` 单边 POSTED 凭证。**零回归论证（结构性成立但须精确）**：8 承付类消费的是控制引擎/三通道路径（非 5 GL 聚合）；5 GL 路径被零承付测试类执行——过滤条件 `ne(BUDGET)` → `notIn(BUDGET,COMMITMENT)` 在无 COMMITMENT 凭证的查询中语义等价，结构性零回归成立，但须（a）Phase 3 回归范围含 pur/sal 承付类（确保改动未波及控制引擎路径），（b）Phase 1 测试入口 Explore 确认承付凭证构造载体（见下）。
- **实仓（HEAD 核查，5 过滤点全集确认）**：
  - `ErpFinAccountingPeriodProcessor.java:392`（`findPostedVoucherIds:385-394`，试算平衡快照前置）——`or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET))`
  - `AnnualCloseService.java:339`（年度结转，vq 查询）
  - `ProfitLossClosingService.java:192`（损益结转，q 查询）
  - `BadDebtProvisionService.java:310`（坏账准备聚合，q 查询）
  - `ExchangeRevaluationService.java:227`（汇兑重估，vq 查询）
  - 常量：`ErpFinConstants.POSTING_TYPE_COMMITMENT = "COMMITMENT"`（:169）+ `POSTING_TYPE_BUDGET = "BUDGET"`（:167）——**常量已存在，零新增**。
  - **排除 ErpFinBudgetLineBizModel:73-74**：该处 `or(eq("postingType", BUDGET), or(isNull, ne(BUDGET)))` 是**恒真式**（显式表达「不过滤通道」语义——三通道分类在下方内存内 per-voucher 谓词完成，`channelOf` 三通道分流已在 R1.1 落地）——**非 5 GL 路径，不在本行范围**（roadmap 行「5 GL 路径」口径）。
  - `ErpFinPostingProcessor` 的 `prepareContext`/`persistVoucher` 侧无 postingType 过滤（过账写入侧不涉及——本行仅读侧过滤）。
- **测试基线**：5 GL 路径执行测试类（`TestErpFinPeriodClose*`/`TestErpFinProfitLossClosing`/`TestErpFinBadDebt`/`TestErpFinAnnualClose`/`TestErpFinExchangeRevaluation`/`TestErpFinBudgetIsolation`）**零承付启用**（config 默认 false）→ 过滤条件从 `ne(BUDGET)` 改 `notIn(BUDGET,COMMITMENT)` 后这些查询结果**不变**（无承付凭证时 notIn 语义 == ne 语义）→ 结构性零回归成立，但须以测试证明（新增承付启用场景 + pur/sal 承付类回归，见基线承付覆盖现状段）。
- **预授权判据（2026-08-12 裁决 B 类）**：纯读侧查询过滤条件修改（5 处 `or(isNull, ne(BUDGET))` → `or(isNull, notIn(BUDGET,COMMITMENT))`），**不触 ORM 结构/不触 VoucherFact/PostingProcessor 写入/校验/删除**（属 Voucher 查询过滤，A4.1.5 裁决「倾向预授权自动执行」+ 2026-08-12 B 类正式降级）；roadmap RC-R1.46 行 `todo`，Deps（R1.0 done）已满足。**仍需独立 plan-audit**（触及损益结转/试算平衡核心路径读侧，独立草案审查 + 独立结束审计为本计划标准义务）。
- **涉及文件**：`ErpFinAccountingPeriodProcessor.java`（:392）；`AnnualCloseService.java`（:339）；`ProfitLossClosingService.java`（:192）；`BadDebtProvisionService.java`（:310）；`ExchangeRevaluationService.java`（:227）；`ErpFinConstants.java`（仅确认常量，零改动）；测试（新增承付启用场景 + 既有零回归）；owner doc `budget.md` + arm-index/roadmap/`docs/logs/`（回填）。

## Goals

- **5 GL 路径 COMMITMENT 排除运行时成立（P1-RC-091 核心）**：5 处过滤条件从 `or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET))` 改为 `or(isNull("postingType"), notIn("postingType", Arrays.asList(POSTING_TYPE_BUDGET, POSTING_TYPE_COMMITMENT)))`（对齐 `ErpFinBudgetControlBiz.applyPostingTypeFilter:162-164` 已 fix 模式）——承付开启 + PO approve 场景下试算平衡快照/年度结转/损益结转/坏账准备/汇兑重估不再混入单边 COMMITMENT 行，**Dr==Cr 恒等式恢复**。
- **实现方式统一**：5 处改动共享同一过滤模式（`FilterBeans.notIn` + 常量列表），保持既有 `or(isNull, ...)` 结构（BUDGET/COMMITMENT 均为影子凭证，postingType 为 null 的实际凭证必须保留）。
- **测试**：新增承付启用场景——① 试算平衡快照（`populateTrialBalanceForAllSchemas` 或等价入口）在 `budget-commitment-enabled=true` + 承付凭证存在时 ΣclosingDebit==ΣclosingCredit（Dr==Cr 恒等式）；② 年度结转/损益结转/坏账准备/汇兑重估 4 服务过滤验证（承付凭证不参与聚合）；③ 既有零承付测试全绿（零回归证明——无承付凭证时 notIn 语义 == ne 语义）。
- **零回归**：既有 finance 测试全绿（`TestErpFinBankReconciliation`/`TestErpFinPeriodClose`/`TestErpFinBadDebt` 等）+ 全仓构建 + compliance checker 零漂移（纯查询过滤条件修改，零新增 daoFor/import 面——不触 R2c/R10）。
- **owner doc 收敛**：`budget.md` 补 5 GL 路径 COMMITMENT 排除实现注记（对齐控制引擎三通道分离口径）；不修改需求契约段（use-cases L1 不动）。
- **回填**：arm-index P1-RC-091 → `done (RC-R1.46)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-003 报表三列化**（独立 finding，属 RC-R1.1 已 done）。
- **不实现 P2-RC-085（现金流量表 postingType 过滤）**（A4.1.22 登记 P2 watch-only，`ErpFinReportBizModel.loadPostedVoucherLines:424-439` 是 CF 读路径——不同控制点，非本行 5 GL 路径范围；修复属读侧过滤预授权可后续单独处理）。
- **不改 ErpFinBudgetLineBizModel:73-74 恒真式**（三通道分流在内存 per-voucher 谓词完成，非过滤缺口，不在「5 GL 路径」口径内）。
- **不改承付凭证结构**（CommitmentVoucherGenerator 单边写入是 by design 影子凭证语义，caveat ③ 维持接受——本行只修聚合读侧过滤）。
- **不触 ORM 结构/不触 VoucherFact/PostingProcessor 写入/校验路径**（纯读侧查询过滤）。
- **不改真相源契约段落**（use-cases L1 不动；budget.md 规则段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权读侧过滤修复，Q4=(a) 强制实现禁止方案 B；2026-08-12 裁决 B 类预授权）
- Owner Docs: `docs/design/finance/use-cases.md`（L1 UC-FIN-13 ③）+ `docs/design/finance/budget.md`（§规则 4/6/8 三通道分离）
- Skill Selection Basis: 实现面 = 5 处查询过滤条件修改（`nop-backend-dev`：QueryBean 过滤构造 + FilterBeans 平台工具）；测试（`nop-testing`：JunitBaseTestCase 直断言 + config 启用场景）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务（承付启用 config `erp-fin.budget-commitment-enabled` 已存在，测试内 `assignConfigValue` 启用）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-finance/erp-fin-service`。

## Execution Plan

### Phase 1 - 5 过滤点全集确认 + 测试入口 Explore（Proof）

Status: completed
Targets: 5 文件过滤点；`ErpFinConstants`；测试类（承付启用场景入口）
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无（既有基线）

- [x] `Proof` **5 过滤点全集核对**：grep 全 module-finance/erp-fin-service 确认 `or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET))`（**带 `ErpFinConstants.` 限定符**）恰 5 处（AccountingPeriodProcessor:392 / AnnualCloseService:339 / ProfitLossClosingService:192 / BadDebtProvisionService:310 / ExchangeRevaluationService:227），`ErpFinBudgetLineBizModel:73-74` 恒真式排除确认（**不在本行范围**），无遗漏站点（完整性证据落盘计划）。
      - Skill: `nop-backend-dev`
- [x] `Proof` **测试入口识别**：承付启用场景测试载体确认——既有 8 承付启用测试类的承付凭证构造路径（`TestErpFinBudgetCommitment` 经 yaml 启用 vs `TestErpPurInvoiceCommitmentRestore:164-172` 经 `assignConfigValue` 显式赋值 vs `TestErpFinBudgetEndToEnd:449-451` 直接 seed COMMITMENT 单边凭证），`CommitmentVoucherGenerator` 直接调用 or 经 `ErpFinBudgetCommitmentBizModel` 编排——新测试类选择与隔离策略（对齐 `TestErpFinBudgetIsolation` 范式）。
      - Skill: `nop-testing`
- [x] `Decision` **测试形态裁决（T1）**：**选项 A（倾向）** = 新增独立测试类 `TestErpFinTrialBalanceCommitmentExclusion`（承付凭证构造 + 5 路径断言，避免污染既有测试）；**选项 B（否决）** = 在既有测试类内追加（承付 config 是类级环境态——既有 yaml 装载类 `TestErpFinBudgetCommitment` 等证明，混合进既有零承付类会污染其类级环境基线）；**选项 C（备选实现细节）** = 独立类内用 per-test `assignConfigValue` + try/finally 恢复（对齐 `TestErpPurInvoiceCommitmentRestore:164-172` 范式）而非类级 yaml——记录于 Decision，两者皆可接受，执行时选一。**理由**：承付 config 是类级环境态，混合进既有类产生顺序耦合；独立类隔离 config 影响面，对齐 R1.1 三通道化测试独立类先例。
      - Skill: `nop-testing`

**Phase 1 执行记录（2026-08-15，落盘证据）：**

- **5 过滤点全集 grep 证据**：`grep 'postingType", ErpFinConstants\.POSTING_TYPE_BUDGET'` 全仓主代码恰 5 处 `or(isNull, ne(BUDGET))` —— `ErpFinAccountingPeriodProcessor.java:392`（`findPostedVoucherIds:385-394`，试算平衡快照前置）、`AnnualCloseService.java:339`（年度结转 vq）、`ProfitLossClosingService.java:192`（损益结转 q）、`BadDebtProvisionService.java:310`（坏账准备 `getAllowanceBalance:256` 聚合前置，credit−debit）、`ExchangeRevaluationService.java:227`（汇兑重估 `aggregateBankSubjectBookFunctional:219`）。其余 grep 命中均为非本行站点：`ErpFinBudgetControlBiz:154`/`BudgetVoucherGenerator:253` 为 `eq(BUDGET)` 通道过滤（正确语义，不动）；`ErpFinBudgetLineBizModel:73-74` 恒真式（`or(eq(BUDGET), or(isNull, ne(BUDGET)))`）排除确认——三通道分流在内存 per-voucher 谓词完成（R1.1 落地），不在本行范围；测试类内命中为既有 helper（`TestErpFinBudgetIsolation:232` ne、`TestErpFinBudgetEndToEnd:500` eq）。
- **常量确认**：`ErpFinConstants.POSTING_TYPE_BUDGET="BUDGET"`（:167）+ `POSTING_TYPE_COMMITMENT="COMMITMENT"`（:169）已存在，零新增。参考模式 `ErpFinBudgetControlBiz.applyPostingTypeFilter:162-164` ACTUAL 通道 `or(isNull, notIn(BUDGET, COMMITMENT))`（`notIn` 静态导入 + 全限定 `java.util.Arrays.asList`）。
- **导入现状（Phase 2 编译前置）**：`ProfitLossClosingService`/`BadDebtProvisionService`/`ExchangeRevaluationService` 已静态导入 `notIn`（+ `java.util.Arrays`）；`ErpFinAccountingPeriodProcessor`/`AnnualCloseService` **缺 `notIn` 导入**（需补）。统一按参考模式用全限定 `java.util.Arrays.asList` 构造常量列表。
- **测试入口确认（Phase 1 Proof 2）**：① 试算平衡快照 + 损益结转经 `periodBiz.closePeriod(periodId, CTX)`（`closeGlModule` → `profitLossClosingService.close` → `populateTrialBalanceForAllSchemas:169`，快照行入 `ErpFinTrialBalance`，按 periodId 查询）；② 年度结转经 `closePeriod` 12 月分支（`ErpFinAccountingPeriodClosePeriodProcessor.closeAnnual:83-117`，config `annual-close-enabled` 默认 TRUE）；③ 坏账准备经 `BadDebtProvisionService.getAllowanceBalance()`（public，`TestErpFinBadDebt:71` 同注入范式）；④ 汇兑重估经 `ExchangeRevaluationService.revalue(period, CTX)` 直调（`TestErpFinAnnualClose:128-132` 同范式）。承付凭证构造载体：`TestErpFinBudgetEndToEnd.seedCommitmentVoucher:450-452`（直接 seed 单边 Dr + `totalDebit=amount/totalCredit=0` + `postingType=COMMITMENT` + `docStatus=POSTED` + `isReversed=false`，镜像 `CommitmentVoucherGenerator.writeCommitmentVoucher:119-182` 语义）；config 切换经 `assignConfigValue` + try/finally（`TestErpPurInvoiceCommitmentRestore:164-172` 范式，`ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED="erp-fin.budget-commitment-enabled"` :419）。
- **T1 裁决（落盘）**：**选 A + C 混合** —— 新增独立测试类 `TestErpFinTrialBalanceCommitmentExclusion`（Option A 理由成立：承付 config 类级环境态，独立类隔离影响面，对齐 R1.1 三通道化独立类先例；Option B 否决：混合进既有零承付类会污染类级环境基线）；config 策略选 Option C —— 类级 `trial-balance-commitment-test.yaml` 提供基座 config（profit-loss 型，`exchange-revaluation-enabled=false` 保持 close 简单），承付开关按 per-test `assignConfigValue(CONFIG_BUDGET_COMMITMENT_ENABLED, TRUE)` + try/finally 恢复（对齐 `TestErpPurInvoiceCommitmentRestore:164-172` 实证范式），避免类级常开承付影响测试顺序耦合。

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] 5 过滤点全集核对完成（grep 证据 + 恒真式排除裁决），测试入口/T1 裁决记录落盘计划
- [x] 无语法/引用错误风险点识别完成（notIn 构造 + 常量引用）

### Phase 2 - 5 处过滤条件修改（P1-RC-091 核心）

Status: completed
Targets: `ErpFinAccountingPeriodProcessor.java`（:392）；`AnnualCloseService.java`（:339）；`ProfitLossClosingService.java`（:192）；`BadDebtProvisionService.java`（:310）；`ExchangeRevaluationService.java`（:227）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 完成

- [x] `Fix` 5 处过滤条件统一修改：`or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET))` → `or(isNull("postingType"), notIn("postingType", java.util.Arrays.asList(ErpFinConstants.POSTING_TYPE_BUDGET, ErpFinConstants.POSTING_TYPE_COMMITMENT)))`（或等价 import 静态化，对齐 `ErpFinBudgetControlBiz.applyPostingTypeFilter:162-164` 已 fix 模式）——保持 `or(isNull, ...)` 结构（postingType=null 的实际凭证必须保留）。
      - Skill: `nop-backend-dev`
- [x] `Proof` 编译验证：`mvn compile -pl module-finance/erp-fin-service -am` 通过（5 文件改动零编译错误）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 5 处过滤条件全部修改（grep 显示零残留 `ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET)` 于 5 站点 + `notIn(...BUDGET, COMMITMENT)` 落地），编译通过

### Phase 3 - 测试 + 零回归验证

Status: completed
Targets: 新增 `TestErpFinTrialBalanceCommitmentExclusion`；既有 finance 测试回归；owner doc `budget.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` `TestErpFinTrialBalanceCommitmentExclusion`（按 T1 裁决独立类）：① 承付启用 + 承付凭证存在 → 试算平衡快照 ΣclosingDebit==ΣclosingCredit（Dr==Cr 恒等式断言，对照修复前失败场景）；② 年度结转/损益结转/坏账准备/汇兑重估 4 服务过滤断言（承付凭证不参与聚合）；③ 承付关闭（默认）→ 既有行为不变断言。
      - Skill: `nop-testing`
- [x] `Proof` 零回归证明：既有 finance 测试全绿（`mvn test -pl module-finance/erp-fin-service`，含执行 5 个改动站点的测试类 `TestErpFinPeriodClose*`/`TestErpFinProfitLossClosing`/`TestErpFinBadDebt`/`TestErpFinAnnualClose`/`TestErpFinExchangeRevaluation`/`TestErpFinBudgetIsolation`——无承付凭证时 notIn==ne 语义的结构性零回归以实测证明）+ **跨域承付类回归**（`mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service` 的承付启用类 `TestErpPurOrderCommitment`/`TestErpPurInvoiceCommitmentRestore`/`TestErpPurReturnCommitmentRestore`/`TestErpPurReturnCommitmentRelease`/`TestErpSalOrderCommitment` 全绿——证明改动未波及控制引擎/三通道路径）。
      - Skill: `nop-testing`
- [x] `Add` owner doc 注记：`budget.md` 补 5 GL 路径 COMMITMENT 排除实现注记（对齐控制引擎三通道分离口径 + 与 A4.1.5 裁决关联）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `TestErpFinTrialBalanceCommitmentExclusion` 全绿（①-③ 通过，含恒等式断言）+ 既有 finance 测试零回归 + 跨域承付类零回归（finance 3 类 + pur/sal 5 类全绿）
- [x] owner doc 注记落地 + compliance checker 零漂移

**Phase 2/3 执行记录（2026-08-15，落盘证据）：**

- **5 处修改落地**：`ErpFinAccountingPeriodProcessor.java:392`（+ `notIn` 静态导入）、`AnnualCloseService.java:339`（+ `notIn` 静态导入）、`ProfitLossClosingService.java:192`、`BadDebtProvisionService.java:310`、`ExchangeRevaluationService.java:227`（后三者 `notIn` 已导入）——统一 `or(isNull("postingType"), notIn("postingType", java.util.Arrays.asList(BUDGET, COMMITMENT)))` + 注释同步「预算/承付凭证」。grep 验证：主代码 `or(isNull, ne(BUDGET))` 零残留（仅排除项 `ErpFinBudgetLineBizModel:74` 恒真式保留）。
- **编译**：`mvn compile -pl module-finance/erp-fin-service -am` BUILD SUCCESS。
- **新增测试**：`TestErpFinTrialBalanceCommitmentExclusion`（`app.erp.fin.service.entity`，5 方法）+ 类级 yaml `trial-balance-commitment-test.yaml`（profit-loss 型基座 + `annual-close-enabled=true` + `auto-generate-next-year-periods=true`）。① `testTrialBalanceIdentityWithCommitmentExcluded`（实际 200/80 + 单边承付 Dr6601 500 → closePeriod 后 ΣclosingDebit==ΣclosingCredit + 本年利润=120）；②a `testAnnualCloseExcludesCommitment`（12 月 1000/400 + 承付 Dr6601 500 → 未分配利润=600）；②b `testBadDebtAllowanceExcludesCommitment`（Allowance 实际 Cr1000 + 承付 Dr1231 500 → `getAllowanceBalance()`=1000）；②c `testExchangeRevaluationExcludesCommitment`（账面 800 + 承付 Dr1002 500 + EUR 账户 100@8.5 → FX 凭证 50）；③ `testCommitmentDisabledBehaviorUnchanged`（默认关闭无承付 → 恒等式 + 本年利润=120 不变）。承付开关 per-test `assignConfigValue` + try/finally 恢复（T1 裁决 C）。
- **测试执行**：新类 5/5 绿；`mvn test -pl module-finance/erp-fin-service` 全量 490 测试 0 失败；跨域承付类 pur 4 类 21 测试 + sal 1 类 3 测试全绿（控制引擎/三通道路径零波及）。
- **执行期发现（非本计划缺陷，记录备查）**：`auto-generate-next-year-periods=false` 时年度结转分支不产出结转凭证——根因是 Nop ORM 查询不自动 flush 未提交的 P&L 结转凭证，而 `generateNextYearPeriods` 内部 `flushSession()` 恰好消除该间隙；既有 `TestErpFinAnnualClose` 恒用 `auto-generate-next-year-periods=true`，生产默认亦为 true，故为既有行为（非本计划 5 站点改动引入）。本计划测试按既有惯例配置 `auto-generate-next-year-periods=true` 规避。
- **owner doc**：`budget.md` §与现有实体的关系 下新增「5 GL 路径 COMMITMENT 排除（读侧过滤，RC-R1.46 / P1-RC-091）」实现注记（5 站点清单 + 语义 + 回归证明引用），契约段未动。
- **compliance checker**：`bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline 见 Closure Gates（纯查询过滤 + 静态导入，零新增 daoFor/import 面）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_ffaf7d434ffe9PiRvZiGF7dYTE`) — 1 MAJOR + 2 MINOR。MAJOR-1 已修正：基线「零承付运行时覆盖」陈旧（A4.1.5 旧口径）→ HEAD 全仓普查 8 个承付启用测试类（finance 3 + pur 4 + sal 1，yaml/assignConfigValue 启用）+ `TestErpFinBudgetEndToEnd:449-451` 直接 seed COMMITMENT 单边凭证——零回归论证重构为「5 GL 路径测试类零承付 + 8 承付类消费控制引擎路径（非 5 GL 聚合）+ notIn==ne 结构性等价」，Phase 3 回归范围补 pur/sal 承付类（Closure Gates 同步），Phase 1 测试入口 Explore 补既有承付类构造路径。2 MINOR 已修正：(1) 过滤条件字面统一带 `ErpFinConstants.` 限定符（Phase 1 census grep 与 Phase 2 退出 grep 可执行）；(2) 零回归测试清单补 5 站点执行类全名（`TestErpFinProfitLossClosing`/`TestErpFinAnnualClose`/`TestErpFinExchangeRevaluation`/`TestErpFinBudgetIsolation:64`）。T1 Decision 补选项 C（per-test assignConfigValue 备选）。其余 baseline 声明实仓核实 PASS（5 站点全集恰 5 处/恒真式排除/常量存在/控制引擎对齐基线/承付凭证单边 Dr 构造/08-12 裁决 B 类降级 + 超期 framing/独立 plan-audit 保留）。
- Independent draft review iteration 2: `acceptable` (`ses_ffaefb03cffe6SVLi4DNXDXp0R`) — 逐项复核 4 项修正全部正确落地（8 承付类全数实证——finance 3 经 budget-a2-test.yaml:8 + pur 4 经 budget-commitment-test.yaml:3/return-commitment-test.yaml:3 + sal 1 经 budget-commitment-sales-test.yaml:3 + `TestErpFinBudgetEndToEnd:449-451` seedCommitmentVoucher 实证；过滤字面限定符双站点；回归清单含 5 站点执行类 + 跨域承付类；T1 选项 C 锚定 `TestErpPurInvoiceCommitmentRestore:164-172`）。1 项残留数字标注已就地修正（「pur/sal 8 类」→「finance 3 类 + pur/sal 5 类」，总数 8 不变、两 run 覆盖口径精确化）。共识达成，计划可转 active。

## Closure Gates

- [x] 范围内行为完成（R1.46 5 GL 路径 COMMITMENT 排除 + 测试）
- [x] 相关文档对齐（budget.md 注记 + arm-index P1-RC-091 → done (RC-R1.46) + roadmap 行 done）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service` + 跨域承付类 `mvn test -pl module-purchase/erp-pur-service,module-sales/erp-sal-service` 的承付启用类（pur 4 + sal 1）全绿 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 现金流量表 postingType 过滤（P2-RC-085）

- Classification: `watch-only residual`
- Why Not Blocking Closure: A4.1.22 已登记 P2 watch-only——当前运行时无污染（BUDGET/COMMITMENT 科目分布非现金类 + seed 零污染凭证），CF 读路径 `ErpFinReportBizModel.loadPostedVoucherLines:424-439` 是不同控制点，非本行 5 GL 路径；修复属读侧过滤预授权可后续单独处理
- Successor Required: `no`

### ErpFinBudgetLineBizModel 恒真式过滤

- Classification: `watch-only residual`
- Why Not Blocking Closure: 三通道分流在内存 per-voucher 谓词完成（R1.1 已落地），恒真式显式表达「不过滤通道」语义，非过滤缺口
- Successor Required: `no`

## Closure

Status Note: 计划可关闭——5 GL 路径（试算平衡快照/年度结转/损益结转/坏账准备/汇兑重估）过滤条件已统一为 `or(isNull("postingType"), notIn("postingType", [BUDGET, COMMITMENT]))`，承付凭证单边 Dr 不再混入聚合，Dr==Cr 恒等式在承付开启场景恢复；新增 `TestErpFinTrialBalanceCommitmentExclusion` 5 断言全绿 + 既有 finance 490 测试零回归 + 跨域承付类 pur 21/sal 3 零回归 + 全量构建通过 + compliance checker 零漂移 + 三处回填（roadmap/arm-index/budget.md/log）落盘。Deferred But Adjudicated 两项均为既有登记 watch-only/显式 Non-Goal 裁决，无范围内项目降级。

Closure Audit Evidence:

- Auditor / Agent: independent subagent (fresh session)
- Evidence: 独立结束审计（2026-08-15，fresh session 实仓复核，非执行者自审）。实跑验证命令与结果：
  - `grep notIn("postingType", java.util.Arrays.asList(` → 恰 5 目标站点（`ErpFinAccountingPeriodProcessor.java:394` / `AnnualCloseService.java:341` / `ProfitLossClosingService.java:193` / `BadDebtProvisionService.java:311` / `ExchangeRevaluationService.java:228`）+ 既有参照 `ErpFinBudgetControlBiz.java:163`（未改）；旧模式 `or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET))` 全仓主代码仅残留 `ErpFinBudgetLineBizModel.java:74`（恒真式排除项，Non-Goal）；常量 `POSTING_TYPE_COMMITMENT` `ErpFinConstants.java:169` 零新增确认
  - `mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinTrialBalanceCommitmentExclusion -o -DfailIfNoTests=false` → **Tests run: 5, Failures: 0, Errors: 0**（surefire 报告重新生成落盘：`target/surefire-reports/app.erp.fin.service.entity.TestErpFinTrialBalanceCommitmentExclusion.txt`）
  - `mvn test -pl module-finance/erp-fin-service -o` → **Tests run: 490, Failures: 0, Errors: 0** BUILD SUCCESS
  - `mvn test -pl module-purchase/erp-pur-service -Dtest='TestErpPurOrderCommitment,TestErpPurInvoiceCommitmentRestore,TestErpPurReturnCommitmentRestore,TestErpPurReturnCommitmentRelease' -o` → **21/21 绿**；`mvn test -pl module-sales/erp-sal-service -Dtest='TestErpSalOrderCommitment' -o` → **3/3 绿**
  - `mvn clean install -DskipTests -o` → **BUILD SUCCESS**（全 reactor，156 模块）
  - `bash docs/audits/nop-compliance-checker.sh` → 19 规则 actual == baseline 机器可读块逐项一致，**零漂移**（R1d=14/R2a=34/R2b=230/R2c=1399/R2d=34/R3=5/R6=2/R10=9/R12a=69/R12b=66/R12c=40）
  - 回填核验：`requirement-compliance-roadmap.md:438` RC-R1.46 → `done ✅`；`arm-index.md:297` P1-RC-091 → `done (RC-R1.46)`；`budget.md:102`「5 GL 路径 COMMITMENT 排除（读侧过滤，RC-R1.46 / P1-RC-091）」注记（5 站点方法名与实仓逐一吻合：`findPostedVoucherIds`×3 / `findYearPostedVoucherIds` / `aggregateBankSubjectBookFunctional`，0 漂移）；`docs/logs/2026/08-15.md:1` 首条目含 RC-R1.46
  - 变更面核验：`git status` 生产改动仅 5 目标 Java 文件（diff 全部为过滤条件+注释+`notIn` 静态导入，零越界）

Follow-up:

- 非阻塞：`_cases/.../TestErpFinTrialBalanceCommitmentExclusion/` 残留 2 个执行期 debug 输出目录（`debugAnnualCloseNoCommitment`/`debugExecuteAnnualCloseDirect`），测试为直断言模式不消费，属可清理的惰性工件，不影响关闭
