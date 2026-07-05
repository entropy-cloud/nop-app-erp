# 2026-07-05-0540-1-finance-bad-debt-provision 坏账准备计提/核销/收回/释放 + 期末 allowance 充足性门控

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/design/finance/bad-debt.md`（完整设计，基于 `docs/analysis/erp-survey/2026-07-04-0000-ar-close-engine.md` 实测方法学）；`docs/architecture/job-scheduling.md` §3.1 `erp-fin-bad-debt-provision` 作业登记为 DESIGN（待实现）
> Related: `2026-07-02-0300-3-ar-ap-settlement-subledger.md`（AR/AP 辅助账 + 账龄，本计划复用）；`2026-07-02-1000-3-finance-period-close.md`（期末结账前置检查，本计划新增 allowance 门控 + 其 Deferred「年度结转」与本计划正交）；`2026-07-05-0115-1-finance-ar-ap-auto-reconciliation.md`（核销单范式，坏账核销/恢复经核销单反向结算表达）
> Mission: erp
> Work Item: finance 坏账准备与应收核销（owner doc `finance/bad-debt.md` DESIGN → 落地）
> Audit: required

## Current Baseline

（实时核实于 2026-07-05）

- **坏账设计完整但零实现**：`docs/design/finance/bad-debt.md` 定义五步分录（赊销确认[非本模块]/月末计提准备 BAD_DEBT_RESERVE/坏账核销 BAD_DEBT_WRITE_OFF/坏账收回 BAD_DEBT_RECOVERY/准备释放 BAD_DEBT_RELEASE）+ 账龄分桶法（5 级历史损失率配置）+ NRV 呈现 + 期末 allowance 充足性门控 + SOX 控制（C-R1/C8/C21）。全仓 grep `ErpFinBadDebt` 在 `module-finance/model/app-erp-finance.orm.xml` 命中 0——无坏账实体、无坏账 BizModel、无坏账业务类型。
- **业务类型枚举缺坏账四码值**：`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java`（枚举）+ ORM 字典 `erp-fin/business-type`（`app-erp-finance.orm.xml:57-86`）现含 30+ 码值（PURCHASE_INPUT/SALES_OUTPUT/AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT/DEPRECIATION/CAPITALIZATION/DISPOSAL/.../CREDIT_FACILITY_INTEREST/OWNERSHIP_TRANSFER/SALARY_* 等），**无** `BAD_DEBT_RESERVE`/`BAD_DEBT_WRITE_OFF`/`BAD_DEBT_RECOVERY`/`BAD_DEBT_RELEASE`。新增属保护区域契约扩展（参照 0831-2 薪酬多类型同步范式：枚举 + ORM 字典 option 同步）。
- **AR/AP 辅助账与账龄已就绪（复用基础）**：`ErpFinArApItem`（`app-erp-finance.orm.xml:473`，应收应付明细账）含 `openAmount`/`settledAmount`/`status`（propId 19，字典 `erp-fin/ar-ap-status` 现仅 OPEN/PARTIAL/SETTLED/CANCELLED，**无** WRITTEN_OFF——本计划需字典加性扩展）；账龄分级（0-30/31-60/61-90/91-180/180+）已由 0300-3 在核销/账龄查询中落地。坏账核销/恢复经 `ErpFinReconciliation` 核销单反向结算表达（参 0115-1 红冲 `docStatus=REVERSED` 模式），不新建账龄体系、不新建辅助账（设计明示复用）。
- **期末结账前置检查可扩展**：`ErpFinAccountingPeriodProcessor`（`module-finance/erp-fin-service/.../processor/`）已有 preCheck 编排（`ErpFinAccountingPeriodBizModel` 委托），现有前置项含「posted=false 单据」「未核销 AR/AP」（1000-3 Phase 1）。本计划新增 allowance 充足性检查项与之并列。
- **过账通道可扩展**：`IErpFinAcctDocProvider` + `ErpFinPostingProcessor` 范式（0811-1）；坏账四类业务类型经既有 `IErpFinVoucherBiz.post` 或新 Provider 生成凭证，业财回链关联源发票/核销单（参 0456 退货 Provider 范式）。
- **既有测试为回归门控**：finance 过账套件（93 测试，1000-3 Closure）+ AR/AP 核销套件（0115-1/0300-3）为本计划回归基线。

剩余差距：坏账五步分录零实现、四业务类型码值缺失、`ErpFinArApItem.status` 无 WRITTEN_OFF、期末 allowance 充足性门控缺失、NRV 无法呈现。

## Goals

- 新增坏账核销/收回单实体（`ErpFinBadDebt` 头，承载核销/恢复事件，关联客户 + 源 AR 辅助账项 + 审批状态机），经 ORM 加性新增 + codegen。
- 实现坏账五步分录中本模块负责的四步（计提 BAD_DEBT_RESERVE / 核销 BAD_DEBT_WRITE_OFF / 收回恢复 BAD_DEBT_RECOVERY / 释放 BAD_DEBT_RELEASE），各步经对应业务类型过账生成凭证；步骤 4b 收款复用既有 RECEIPT。
- 账龄分桶法计提引擎：按 5 级历史损失率（配置化）× 各区间应收 openAmount 求必需准备，排除争议/负余额/已核销项；与当前 Allowance GL 账面比较决定补提/释放。
- `ErpFinArApItem.status` 字典加性扩展 `WRITTEN_OFF`（保护区域契约同步）；坏账核销置 WRITTEN_OFF + openAmount→0，恢复回退正常态。
- 期末结账前置检查新增 allowance 充足性门控（必需准备 vs Allowance 账面：不足阻止结账提示补提；超额提示释放；精度内相等通过）。

## Non-Goals

- **完整 ECL 三阶段模型（CAS 22 正向减值）**：采用简便实务（账龄分桶）；ECL 属 Follow-up（设计已裁定，触发条件：金融工具准则严格合规要求时）。
- **客户风险评分体系 / 风险分类法 / 帕累托法**：依赖客户信用评分，属 Follow-up（触发条件：CRM 客户信用评分落地时）。
- **坏账准备多维分摊（按部门/产品线）**：属 GL Distribution 范畴（`posting.md` §GL Distribution Deferred）。
- **应收账款保理/质押**：属资金面（`treasury.md`）。
- **总账报表渲染（科目余额表/NRV 列报）**：属 nop-report 报表面；本计划仅计算 allowance 余额供报表消费，不渲染。
- **年度结转**：与本计划正交，归 `1000-3` Deferred（本批次 N=2 计划承接）。
- **坏账核销/恢复 cron 定时执行**：归 `job-scheduling.md` §3.1 follow-up 接线（本计划交付 BizModel 入口 + 期末门控，cron 注册归后继）。

## Task Route

- Type: `implementation-only change`（owner doc 已完整设计的落地）+ 少量 `app-layer design change`（owner doc 偏离补注收口）
- Owner Docs: `docs/design/finance/bad-debt.md`（§businessType 映射落地标记 + §状态含义 WRITTEN_OFF 落地 + §期末门控落地补注）；`docs/design/finance/period-close.md`（§结账前置检查增 allowance 项）；`docs/design/finance/ar-ap-reconciliation.md`（status 扩展补注）
- Skill Selection Basis: BizModel 方法 + 业财过账业务类型扩展 + 跨实体读 AR 辅助账 + 期末前置检查 + ORM 加性实体 + 字典保护区域同步——加载 `nop-backend-dev`（实体服务、跨实体调用、过账 Provider 范式、ErrorCode）。

### Key Decisions

- **Decision: 坏账核销/恢复的承载形态**
  - 选择：新增 `ErpFinBadDebt` 单实体（头，承载 writeOff/recovery 事件：docType dict `erp-fin/bad-debt-type` WRITE_OFF/RECOVERY + customerId + 源 ArApItemId 关联 + amount + reason + approvalStatus 三轴审批状态机 + standard 审计字段），核销/恢复动作生成 `ErpFinReconciliation` 核销单反向结算表达（复用 0115-1 范式，反向 AR 辅助账项），不新建"坏账核销单行"子表（核销对象即源 AR 辅助账项）。
  - 替代方案：(a) 复用 `ErpFinReconciliation` 直接表达坏账核销不加新实体——拒绝，坏账核销/恢复是带审批 + 原因 + SOX 控制的独立业务事件，需独立头承载审计与审批；(b) 头-行两实体（多发票一次核销）——拒绝，核销对象已是 ArApItem（行级粒度），一次核销可经多张 ErpFinBadDebt 或核销单多行表达，避免过度建模。
  - 残留风险：单实体不带行子表，多发票批量核销需多次调用或核销单多行——以核销单（ErpFinReconciliation 已支持多对多）承载批量。
- **Decision: 计提与释放的触发机制**
  - 选择：`runBadDebtProvision(periodId)` @BizMutation 作为期末计提/释放入口（计算必需准备 vs Allowance 账面：不足补提 BAD_DEBT_RESERVE；超额 BAD_DEBT_RELEASE 财务主管审批后释放；相等无动作）；期末结账 allowance 门控阻止"必需>账面"结账。释放需审批（直接 P&L 影响），计提随结账批量。
  - 替代方案：计提也强制单笔审批——拒绝，计提是期间估计批量动作（设计明示），审批成本过高；核销/恢复/释放才是高风险单据（进审批）。
  - 残留风险：批量计提无审批——以期末门控 + 规则命中日志（5.1）+ NRV 正值校验缓解。
- **Decision: Allowance 账面余额来源**
  - 选择：Allowance 科目余额经既有总账（`ErpFinVoucher` 科目聚合）查询本期间累计额（复用 1000-3 试算平衡表快照通道或直接科目聚合）；不新建 Allowance 余额实体。
  - 替代方案：新建 `ErpFinBadDebtAllowance` 余额实体物化——拒绝，重复总账余额且引入一致性维护。
- **Decision: 坏账四业务类型与字典同步**
  - 选择：`ErpFinBusinessType` 枚举 + ORM 字典 `erp-fin/business-type` 同步加性追加 4 码值（BAD_DEBT_RESERVE/WRITE_OFF/RECOVERY/RELEASE），参照 0831-2 多类型同步保护区域范式；凭证科目分解（借信用减值损失/贷坏账准备 等）经会计科目表配置（ErpMdAcctSchema）。
  - 替代方案：复用单一 BAD_DEBT 类型内部分支——拒绝，四步借贷方向与科目不同（设计 §businessType 映射），独立类型便于规则命中日志追溯与报表。

## Infrastructure And Config Prereqs

- 新增 config 项（设计 §配置点）：`erp-fin.bad-debt-method`（默认 AGING_BUCKET）、`erp-fin.bad-debt-loss-rate-0-30`(0.005)/`-31-60`(0.02)/`-61-90`(0.05)/`-91-180`(0.15)/`-180-plus`(0.40)、`erp-fin.bad-debt-write-off-require-approval`(true)、`erp-fin.bad-debt-exclude-disputed`(true)。
- 依赖既有会计科目表配置（ErpMdAcctSchema，坏账准备/信用减值损失科目）；无新基础设施。
- 无数据迁移（新实体 + 字典加性）；回滚策略：删 `ErpFinBadDebt` 实体 + 还原字典 4 码值 + 移除 allowance 门控项 + 移除 WRITTEN_OFF status。

## Execution Plan

### Phase 1 - ORM 加性（坏账实体 + 四业务类型 + WRITTEN_OFF status）+ codegen

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（`ErpFinBadDebt` 实体 + `erp-fin/business-type` 4 option + `erp-fin/bad-debt-type` 字典 + `erp-fin/ar-ap-status` WRITTEN_OFF option）；codegen 产物
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：在计划记录坏账核销/恢复承载形态 + 计提/释放触发 + Allowance 余额来源 + 四业务类型裁决（见 Task Route Key Decisions）
  - Skill: `nop-backend-dev`
- [x] `Add`：`app-erp-finance.orm.xml` 加性追加 `ErpFinBadDebt`（docType dict `erp-fin/bad-debt-type` WRITE_OFF/RECOVERY + customerId + sourceArApItemId 弱参照 + amount + reason + approvalStatus dict `erp-fin/approve-status` + standard 审计字段）；追加字典 `erp-fin/bad-debt-type`；`erp-fin/business-type` 加性追加 BAD_DEBT_RESERVE/WRITE_OFF/RECOVERY/RELEASE；`erp-fin/ar-ap-status` 加性追加 WRITTEN_OFF
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinBusinessType` 枚举同步 4 码值；重新 codegen 生成 dao/_gen/IBiz/meta/view（遵循 finance 域既有范式，不手改 _gen）
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅证明实体可生成并可标准 CRUD；分录逻辑在 Phase 2/3 验证。

- [x] `ErpFinBadDebt` codegen 产物存在（entity/_gen/IBiz/xmeta），finance 域编译通过
- [x] 4 业务类型 + WRITTEN_OFF status + bad-debt-type 字典在 ORM 与枚举一致（grep 双向命中）

### Phase 2 - 计提/释放引擎 + 核销/收回 BizModel + 过账

Status: completed
Targets: `ErpFinBadDebtBizModel`（CrudBizModel + 自定义动作）；`BadDebtProvisionCalculator`；坏账过账 Provider（`ErpFinBadDebtPostingProvider` 或经既有 `IErpFinVoucherBiz.post`）；`ErpFinErrors`（新 ErrorCode）；`ErpFinConstants`（config 键）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：`BadDebtProvisionCalculator`——账龄分桶法：按 5 级 config 损失率 × 各区间 ArApItem openAmount 求必需准备，排除 disputed/负余额/已核销项（config-gated `exclude-disputed`）；返回必需准备 + 各区间明细
  - Skill: `nop-backend-dev`
- [x] `Add`：`runBadDebtProvision(periodId)` @BizMutation——必需准备 vs Allowance GL 账面：不足→补提 BAD_DEBT_RESERVE 凭证；超额→登记待审批释放（不自动释放，财务主管审批后执行）；相等→无动作
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinBadDebtBizModel` 自定义动作——`writeOff`（审批后置源 ArApItem status→WRITTEN_OFF + openAmount→0 经核销单反向结算 + BAD_DEBT_WRITE_OFF 凭证 借Allowance/贷AR）；`recover`（恢复：ArApItem 回退正常态 + BAD_DEBT_RECOVERY 凭证 借AR/贷Allowance）；三轴审批状态机（write-off/recovery 强制审批 config-gated）；非法迁移 ErrorCode
  - Skill: `nop-backend-dev`
- [x] `Add`：坏账过账——四业务类型经 `IErpFinVoucherBiz.post` 或新 Provider 生成凭证，业财回链关联源发票/核销单；科目分解经 ErpMdAcctSchema 配置
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 计提引擎计算必需准备 = Σ(区间 openAmount × 损失率)，排除项正确；补提生成 BAD_DEBT_RESERVE 凭证（借信用减值损失/贷坏账准备）
- [x] 核销后 ArApItem.status=WRITTEN_OFF + openAmount=0 + BAD_DEBT_WRITE_OFF 凭证（借Allowance/贷AR，不进 P&L）；恢复回退正常态 + BAD_DEBT_RECOVERY 凭证
- [x] 释放（超额）经审批生成 BAD_DEBT_RELEASE 凭证（借Allowance/贷信用减值损失）

### Phase 3 - 期末 allowance 充足性门控 + 测试 + owner doc

Status: completed
Targets: `ErpFinAccountingPeriodProcessor`（preCheck 增项）；行为测试（fin-service）；`docs/design/finance/bad-debt.md`；`docs/design/finance/period-close.md`；`docs/logs/2026/07-05.md`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`：`ErpFinAccountingPeriodProcessor` preCheck 新增 allowance 充足性检查项（必需准备 vs Allowance 账面：必需>账面→阻止结账提示补提；必需<账面→提示释放；精度内相等→通过），与既有前置项并列
  - Skill: `nop-backend-dev`
- [x] `Proof`：行为测试——账龄分桶计提（5 区间 + 排除 disputed/负余额/已核销）+ 补提凭证 + 核销（status→WRITTEN_OFF + openAmount→0 + 不进P&L）+ 恢复回退 + 释放（超额审批）+ 期末门控（必需>账面阻止/相等通过）+ NRV 正值；策略 `JunitAutoTestCase` + GraphQL 快照，`mvn test -pl module-finance/erp-fin-service -am`
  - Skill: `nop-backend-dev`
- [x] `Add`：owner doc 对齐——`bad-debt.md` §businessType 映射标记落地 + §状态含义 WRITTEN_OFF 落地 + §期末门控落地补注；`period-close.md` §结账前置检查增 allowance 项；`ar-ap-reconciliation.md` status 扩展补注
  - Skill: none

Exit Criteria:

- [x] 新增坏账行为测试全绿；finance 过账套件（93）+ AR/AP 核销套件零回归
- [x] owner doc 偏离补注收口（bad-debt DESIGN 项标记落地）

## Draft Review Record

- Independent draft review iteration 1: accept（ses_0d0e5d636ffe5xzy37jgb8QKJK，general 独立子代理新会话）— 无 BLOCKER。逐项实时仓库核实：`ErpFinBadDebt` 在 ORM 命中 0 属实；`ErpFinBusinessType` 32 码值（止于 BANK_RECON_ADJ(320)）无 BAD_DEBT_* 属实；`erp-fin/business-type` 字典（orm.xml:56-88）无 BAD_DEBT_* 属实；`ErpFinArApItem`（:473）status（:495 propId 19 dict ar-ap-status）OPEN/PARTIAL/SETTLED/CANCELLED 无 WRITTEN_OFF 属实；`ErpFinAccountingPeriodProcessor.preCheck`（:80）属实；`bad-debt.md` 五步分录/AGING_BUCKET/4 businessType/allowance gate 属实；0300-3/1000-3/0115-1 均 completed。14 最低规则全过、反松弛无违例、模板完整、坏账核算逻辑正确（WRITE_OFF 仅 BS 不进 P&L；RESERVE/RELEASE 进 P&L；RECOVERY 反转+复用 RECEIPT）。非阻塞 minor：Phase 1 Item 1 Decision 自指（决策已落 Key Decisions）、Decision 3 缺残留风险行（框架强制选择可省）。共识达成，转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（五步分录四步 + 计提引擎 + 核销/恢复审批 + allowance 门控）
- [x] 相关文档对齐（bad-debt.md / period-close.md / ar-ap-reconciliation.md / 当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 完整 ECL 三阶段模型（CAS 22 正向减值）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 采用简便实务（账龄分桶）；ECL 属独立结果面（需三阶段减值矩阵 + 前瞻性调整）。
- Successor Required: yes（触发条件：金融工具准则严格合规要求时）

### 客户风险评分体系 / 风险分类法 / 帕累托法

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖 CRM 客户信用评分落地。
- Successor Required: yes（触发条件：CRM 客户信用评分体系落地时）

### 坏账准备多维分摊（部门/产品线）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 GL Distribution 范畴（`posting.md` Deferred）。
- Successor Required: yes（触发条件：GL Distribution 落地时）

### 坏账核销/恢复 cron 定时执行

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划交付 BizModel 入口 + 期末门控；cron 注册归 job-scheduling follow-up。
- Successor Required: yes（触发条件：坏账周期自动核销/计提定时需求落地时）

## Closure

Status Note: 坏账五步分录四步（RESERVE/WRITE_OFF/RECOVERY/RELEASE）+ 账龄分桶计提引擎 + 核销/收回审批状态机 + 期末 allowance 充足性门控全部落地。验证全绿：根 `mvn clean install -DskipTests`（19 reactor 模块全 SUCCESS）+ `mvn test -pl module-finance/erp-fin-service -am`（155 测试，含新增 7 坏账行为测试，0 失败/0 错误，原 148 测试零回归）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，closure-audit role；不重用执行者上下文）
- Evidence: 独立语义核实于 2026-07-05——逐项 grep/read 实时仓库验证：(1) `ErpFinBadDebt` 实体在 `module-finance/model/app-erp-finance.orm.xml:1178` + codegen 产物全链（entity/_gen/IBiz/xmeta/xbiz/view/beans/dict.yaml）；(2) `ErpFinBusinessType` 枚举 4 码值（340/350/360/370）+ ORM 字典 + 重生成 `business-type.dict.yaml`/`_app.orm.xml` 双侧一致；(3) `erp-fin/ar-ap-status` 含 `WRITTEN_OFF`（ORM:121 + dict.yaml + i18n）；(4) `BadDebtProvisionCalculator`（账龄分桶法，排除 SETTLED/WRITTEN_OFF/CANCELLED + 负余额）+ `BadDebtProvisionService.runBadDebtProvision`（补提/释放/相等三分支，经 `CloseVoucherWriter` 持久化凭证）落地且 bean 注册；(5) `ErpFinBadDebtProcessor` writeOff/recover/submit/approve/reject 三轴审批状态机方法体非空（executeWriteOff 置 WRITTEN_OFF+openAmount→0+借Allowance/贷AR；executeRecovery 回退 OPEN+借AR/贷Allowance），非 `return null` 占位；(6) 期末门控 `ErpFinAccountingPeriodProcessor.populateAllowanceCheck`（:98-115，由 preCheck :89 实际调用，shortfall>0 阻断 / excess>0 提示，config-gated）真实接线；(7) `TestErpFinBadDebt` 7 case 覆盖账龄分桶/补提/排除/核销/恢复/释放/门控；(8) owner docs `bad-debt.md`/`period-close.md`/`ar-ap-reconciliation.md` 均含「已落地 plan 2026-07-05-0540-1」标记；(9) `docs/logs/2026/07-05.md` 计划条目存在含 full-green 验证状态。反空心检查通过。执行者验证于 2026-07-05——`module-finance/erp-fin-service` 155 测试全绿（含 TestErpFinBadDebt 7）；根构建 19 模块 SUCCESS；roadmap item 1.8 已追加坏账 done 标记。

Follow-up:

- ECL 三阶段模型（见上方 Deferred）
- 客户风险评分体系（见上方 Deferred）
- 坏账 cron 接线（见上方 Deferred）
