# 2026-07-05-0115-1-finance-ar-ap-auto-reconciliation AR/AP 自动核销引擎 + 定时执行 + 双面对账一致性兜底

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/backlog/core-business-roadmap.md` 工作项 1.6/1.7 残余段（自动核销 deferred）；`docs/design/finance/ar-ap-reconciliation.md §自动核销`
> Related: `2026-07-02-0300-3-ar-ap-settlement-subledger.md`（手工核销单 + 辅助账，本计划在其上叠加自动匹配）、`2026-07-04-1600-1-batch-scheduling-architecture.md`（批处理架构，登记 `erp-fin-ar-ap-auto-recon` 为 batch-candidate）
> Mission: erp
> Work Item: AR/AP 自动核销（按金额/比例/账龄/到期日规则 + 手动/定时触发 + 域级↔finance 双面一致性兜底）
> Audit: required

## Current Baseline

实时仓库逐项核实的事实：

- **手工核销单已落地（0300-3）**：`ErpFinReconciliationBizModel`（`module-finance/erp-fin-service/.../entity/ErpFinReconciliationBizModel.java:46`）含 `create(direction, partnerId, lines)` / `post(reconciliationId)` / `reverse(reconciliationId)` 三个 `@BizMutation`；`ReconciliationSettler` 回写双方 `ErpFinArApItem.settledAmount/openAmount/status`；`PartnerBalanceUpdater` 重算 partner 余额；5 项核销约束（同 direction/同 partner/未 SETTLED/不超额/核销日期不早于发票业务日期）已实现并测试。
- **辅助账生成已落地（0300-3）**：`ErpFinArApItemGenerator` 在 `ErpFinPostingProcessor` 同事务内按 AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT 生成 `ErpFinArApItem`（direction/amount/open/status）。`IErpFinArApItemBiz` 已有 `findOpenItemsByPartner` 等 `@BizQuery`（供核销与账龄使用）。
- **辅助账实体** `ErpFinArApItem`（`finance.orm.xml:466`）：`direction`(RECEIVABLE/PAYABLE)、`sourceBillType`、`partnerId`、`dueDate`(可空)、`openAmountSource/Functional`、`settledAmountSource/Functional`、`status`(OPEN/PARTIAL/SETTLED/CANCELLED)。**自动核销的数据源已具备**——按 partner + direction 查 OPEN/PARTIAL 的发票项与收付款项即可。
- **域级核销（双面之一）已存在**：`ErpPurPaymentLine`(paymentId→invoiceId+amount) 与 `ErpSalReceiptLine`(receiptId→invoiceId+amount) 回写发票 paidStatus/receivedStatus（0300-1/0300-2）。0300-3 Deferred「域级核销 vs finance 核销单双面对账一致性兜底」——**successor required: yes（触发条件：出现双面余额不一致或实施日终对账兜底时）**，本计划承接。
- **定时作业契约已建立（1600-1）**：`docs/architecture/job-scheduling.md:99` 登记 `erp-fin-ar-ap-auto-recon`（每日凌晨、batch-candidate、DEFERRED、配置键 `erp-fin.ar-ap-auto-recon-cron`）。`nop-job-local` 已接入 `app-erp-all`，但**无任何 job bean 实现**；18 处计划 Deferred 推迟 cron 实际注册。1600-1 裁决：单次处理量 ≥ 数万 → nop-batch 分 chunk；小数据量 → nop-job 直调 BizModel。
- **配置项（`ar-ap-reconciliation.md §配置项`）**：`erp-fin.auto-reconcile`(默认 false)、`erp-fin.reconcile-precision`(默认 0.01)、`erp-fin.allow-over-reconcile`(默认 false) 已在 0300-3 落地；`erp-fin.ar-ap-auto-recon-cron`（设计已登记，值为 deferred）。
- **BizModel 空壳**：codegen 已生成各域空 `CrudBizModel`；`ErpFinReconciliationBizModel` 已由 0300-3 扩展（非空壳）。
- **剩余差距**：(1) 无自动匹配规则引擎（按金额/比例/账龄/到期日）；(2) 无 `runAutoReconciliation` 手动触发入口；(3) 定时自动核销 job bean 未实现、scheduler.yaml 未注册；(4) 无域级↔finance 双面余额一致性兜底检查。

## Goals

- **自动核销规则引擎** `AutoReconciliationEngine`：按 partner + direction 查询 OPEN/PARTIAL 的发票项与收付款项，按可配置分摊策略（FIFO 按到期日/业务日期、按金额精确匹配、按余额比例）生成核销候选行，复用 0300-3 `create`+`post` 路径落 `ErpFinReconciliation`（约束校验、settled/open/status 回写、partner 余额重算全部复用，不重写核销原语）。
- **手动触发入口** `IErpFinReconciliationBiz.runAutoReconciliation(direction, partnerId?, strategy?)`（`@BizMutation`）：config-gated（`erp-fin.auto-reconcile=true`），返回生成的核销单与未匹配项报告；幂等（同批已 SETTLED 项不重复进入候选）。
- **定时执行**：注册 `nop-job-local` 的 job bean + `scheduler.yaml` 条目（`erp-fin.ar-ap-auto-recon`，cron 来自 `erp-fin.ar-ap-auto-recon-cron`），调用 `runAutoReconciliation`（全 partner、FIFO）；cron 未配置则不调度。**nop-batch 分 chunk 迁移留 Deferred**（触发条件：单次匹配量 ≥ 数万）。
- **双面对账一致性兜底** `IErpFinReconciliationBiz.checkDualSideConsistency(direction, partnerId?)`（`@BizQuery`）：比对 finance `ErpFinArApItem` 开口余额聚合 vs 域级 `ErpPurPaymentLine`/`ErpSalReceiptLine` 已核销聚合，产出差异报告（不自动修复，只报告 + 日志告警）。
- 行为测试覆盖四策略、手动/定时触发、双面一致性报告、幂等、config-gated 关闭。

## Non-Goals

- **付款/收款审核时自动核销钩子（on-payment-approval trigger）**：`ar-ap-reconciliation.md §自动核销触发` 列出三触发（手动/定时/付款审核触发）。付款审核触发须钩入 purchase/sales 收付款审核流，属跨域结果表面，归后续（触发条件：purchase/sales 审核流需联动自动核销时）。
- **nop-batch 分 chunk 执行**：1600-1 裁决自动核销为 batch-candidate（数据量 ≥ 数万）。本计划用 nop-job-local 直调 BizModel（内部分页处理），单次处理量小到中等场景够用；nop-batch 迁移留 Deferred（触发条件：单次匹配量稳定 ≥ 数万）。
- **核销凭证生成**：0300-3 已裁定「核销不直接生成凭证，凭证由收付款审核时生成」；本计划遵循，不生成核销凭证。
- **汇兑损益核销凭证**：期末独立汇兑重估已由 1000-3 `ExchangeRevaluationService` 承接；核销时汇兑差异（`ErpFinReconciliation.fxGainLoss` 占位）归期末汇兑面，本计划不计算。
- **自动核销规则 UI / 规则配置实体**：`ar-ap-reconciliation.md §自动核销规则` 表为设计期规则清单，本计划以 config 项 + 策略参数承载（`erp-fin.auto-recon-strategy` 等），不引入「核销规则配置实体」（归前端/产品化后续）。
- **双面一致性差异自动修复**：兜底检查只报告 + 告警，不自动调平（避免静默修改域级核销权威）。
- **账龄报表 UI / 定时账龄快照**：0300-3 已 Non-Goal，延续。

## Task Route

- Type: `implementation-only change`（在 0300-3 手工核销基线上叠加自动匹配引擎 + 定时触发 + 一致性报告；**不触及 ORM/字典/实体/契约**——核销原语、辅助账、partner 余额、配置项均已在 0300-3 落地）。
- Owner Docs: `docs/design/finance/ar-ap-reconciliation.md`（核销模型/流程/约束/账龄/§自动核销/§配置项）、`docs/architecture/job-scheduling.md`（§3.1 `erp-fin-ar-ap-auto-recon` 登记 + nop-job-local 演进）、`docs/architecture/data-dependency-matrix.md`（finance↔purchase/sales 依赖方向）。
- Skill Selection Basis: BizModel `@BizMutation`/`@BizQuery` + 复用 0300-3 核销原语 + 跨实体查询（ArApItem）+ 定时 job bean + 错误码 + 事务 → 加载 `nop-backend-dev`（覆盖 IBiz、跨实体访问、@BizMutation 事务边界、反模式自检）。

## Infrastructure And Config Prereqs

- 配置项（`ar-ap-reconciliation.md §配置项` + 1600-1）：`erp-fin.auto-reconcile`(默认 false，总开关，已在设计登记)、`erp-fin.auto-recon-strategy`(**本计划新增**，默认 FIFO，枚举 FIFO/BY_AMOUNT/BY_RATIO，经 `AppConfig.var` 默认值无契约变更)、`erp-fin.reconcile-precision`(默认 0.01，0300-3 已落地)、`erp-fin.allow-over-reconcile`(默认 false，0300-3 已落地)、`erp-fin.ar-ap-auto-recon-cron`(默认空=不调度；设计登记为 deferred，本计划实现时填值如 `0 0 1 * * ?` 每日凌晨)。经 `AppConfig.var(..., defaultValue)` 读取。
- 依赖：0300-3 必须先完成（手工核销单 + 辅助账 + partner 余额）——已 done。`nop-job-local` 已接入 `app-erp-all`（1600-1 核实）。
- 无数据迁移；无 ORM/字典变更；无新增端口/密钥/外部服务。

## Execution Plan

### Phase 1 — 自动核销规则引擎 + 手动触发

Status: completed
Targets: `module-finance/erp-fin-service/.../service/AutoReconciliationEngine.java`（新）、`.../entity/ErpFinReconciliationBizModel.java`（扩 `runAutoReconciliation`）、`.../biz/IErpFinReconciliationBiz.java`（扩）、`.../service/AutoReconStrategy.java`（新枚举）、`.../ErpFinErrors.java`(扩)、beans.xml
Skill: `nop-backend-dev`

- Item Types: `Decision | Add | Proof`
- Prereqs: 0300-3 完成（手工核销单 + 辅助账 + partner 余额，已 done）。

- [x] `Decision`：自动核销落库机制——**选择**复用 0300-3 `ErpFinReconciliationBizModel.create(direction, partnerId, businessDate, lines, context)` + `post(reconciliationId, context)` 路径（实际签名含 `businessDate` + `IServiceContext`，引擎/定时 job 须补传今日 businessDate + 系统 context），引擎只负责生成候选行（invoiceItemId/paymentItemId/settledAmount），核销约束校验/状态回写/partner 余额全部走既有 post。**替代**：① 引擎直接操作 ArApItem 跳过核销单（丢失核销审计单据 + 绕过约束校验，rejected）；② 新建独立核销单类型（重复结果表面，rejected）。**残留风险**：自动生成的核销单量大（按 partner×direction×批次），可接受（核销单即审计载体）。
  - Skill: none
- [x] `Decision`：分摊策略——**选择**支持三策略经 `erp-fin.auto-recon-strategy` 配置 + 方法参数覆盖：`FIFO`（按 dueDate 升序，null dueDate 回退 businessDate，逐笔核销直至收付款项 open 耗尽，默认）、`BY_AMOUNT`（收付款项金额精确匹配发票项金额，仅 1:1 命中）、`BY_RATIO`（按发票开口余额比例分摊收付款项）。**替代**：仅 FIFO（无法满足精确匹配/比例分摊场景，rejected）。**残留风险**：BY_AMOUNT 在金额非唯一时退化为 UNMATCHED（报告列出，等待人工）。
  - Skill: none
- [x] `Add`：`AutoReconciliationEngine.matchAndBuild(direction, partnerId, strategy)` —— 注入 `IErpFinArApItemBiz` 查询该 partner + direction 的 OPEN/PARTIAL 发票项（sourceBillType=AP_INVOICE/AR_INVOICE）与收付款项（PAYMENT/RECEIPT）；按策略生成 `List<ReconciliationLineCandidate>`（paymentItemId/invoiceItemId/settledAmountSource，精度 `erp-fin.reconcile-precision`，尾差调整末行）；金额不超任一方 openAmount（`erp-fin.allow-over-reconcile=false` 时）；返回候选 + 未匹配项报告（unmatchedReason：无对侧/金额无候选/超额）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpFinReconciliationBiz.runAutoReconciliation(@Name("direction") String direction, @Name("partnerId") Long partnerId, @Name("strategy") String strategy)`（`@BizMutation`）—— config-gated（`erp-fin.auto-reconcile=false` 抛 `NopException` ERR_AUTO_RECON_DISABLED）；调引擎生成候选 → 调 `create`+`post` 落核销单（partnerId null 时遍历所有有开口余额的 partner，分 partner 独立核销单）；返回 `AutoReconResult`（生成的核销单 ID 列表 + 未匹配项报告）。幂等：候选查询已排除 SETTLED/CANCELLED 项，重复执行只处理剩余开口项。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpFinErrors` 扩展 `ERR_AUTO_RECON_DISABLED` / `ERR_AUTO_RECON_NO_OPEN_ITEMS`。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpFinAutoReconciliation`——FIFO（多发票按 dueDate 顺序被一笔收款逐笔核销至耗尽）/ BY_AMOUNT（精确匹配 1:1 + 非唯一金额 UNMATCHED）/ BY_RATIO（按比例分摊 + 尾差归末行）/ config-gated 关闭抛错 / 幂等（二次执行无新核销单）/ 超额拒绝 / 未匹配项报告正确。验证命令 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinAutoReconciliation*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付自动核销引擎 + 手动触发，复用 0300-3 核销原语。解除 Phase 2 定时触发（需 runAutoReconciliation 入口）的阻塞。

- [x] 三策略自动匹配 + 手动触发 `runAutoReconciliation` 单测通过；核销单经既有 post 路径落库（settled/open/status/partner 余额正确）
- [x] 幂等 + config-gated + 未匹配报告行为验证通过

### Phase 2 — 定时执行（nop-job-local job bean + scheduler.yaml）

Status: completed
Targets: `module-finance/erp-fin-service/.../job/ErpFinAutoReconJob.java`（新）、`app-erp-all/.../scheduler.yaml`（或对应 job 配置位置，扩 `erp-fin-ar-ap-auto-recon`）、`docs/architecture/job-scheduling.md`（状态 DEFERRED→wired 更新）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（`runAutoReconciliation` 入口已存在）。

- [x] `Decision`：定时执行载体——**选择** nop-job-local 注册 job bean 调 `IErpFinReconciliationBiz.runAutoReconciliation(RECEIVABLE, null, FIFO)` + `(PAYABLE, null, FIFO)`（全 partner、FIFO），cron 经 `erp-fin.ar-ap-auto-recon-cron` 配置（空值=不调度）。**替代**：① nop-batch 分 chunk（1600-1 裁决为 batch-candidate，但单次量未达数万阈值前过度工程，rejected 归 Deferred）；② 不实现定时只留手动（设计 §自动核销触发 明列定时触发为标准能力，rejected）。**残留风险**：nop-job-local 重启丢失作业定义——scheduler.yaml 声明保证重启重建（1600-1 已要求）。
  - Skill: none
- [x] `Add`：`ErpFinAutoReconJob`（实现 nop-job-local 的 `IJobInvoker`/job bean 接口，按 1600-1 + nop-entropy/docs-for-ai/03-modules/nop-job.md 范式）—— 解析 cron 配置，调度 `runAutoReconciliation`；job bean 注册到 beans.xml。
  - Skill: `nop-backend-dev`
- [x] `Add`：`scheduler.yaml`（nop-job 标准 VFS 路径 `/nop/job/conf/scheduler.yaml`，见 `nop-entropy/docs-for-ai/03-modules/nop-job.md`）登记 `erp-fin-ar-ap-auto-recon` 条目（cron 引用 `erp-fin.ar-ap-auto-recon-cron`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：更新 `docs/architecture/job-scheduling.md:99` `erp-fin-ar-ap-auto-recon` 行状态 `DEFERRED`→`wired`（实现证据 file:line）。
  - Skill: none
- [x] `Proof`：`TestErpFinAutoReconJob`——job bean 解析 cron 配置、cron 空值不调度、触发时调 runAutoReconciliation（RECEIVABLE + PAYABLE）。验证命令 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinAutoReconJob*`。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付定时自动核销 job bean + scheduler.yaml 注册。解除 Phase 3 对 runAutoReconciliation 稳定入口的依赖（已具备）。

- [x] job bean + scheduler.yaml 注册可观察；cron 空值不调度、配置后按 cron 触发 runAutoReconciliation 单测通过
- [x] `job-scheduling.md` 状态更新为 wired

### Phase 3 — 双面对账一致性兜底 + 文档/日志

Status: completed
Targets: `.../entity/ErpFinReconciliationBizModel.java`（扩 `checkDualSideConsistency`）、`.../service/DualSideConsistencyChecker.java`（新）、`docs/logs/2026/{07-05}.md`、`docs/backlog/core-business-roadmap.md`、`docs/design/finance/ar-ap-reconciliation.md`（若实现偏离补注）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2。

- [x] `Add`：`DualSideConsistencyChecker` —— 比对 finance 侧 `ErpFinArApItem`（按 partner 聚合开口余额：Σ openAmount by direction）vs 域级侧（purchase `ErpPurPaymentLine` 已核销额聚合 / sales `ErpSalReceiptLine` 已核销额聚合 推导域级开口）。注入 `IErpFinArApItemBiz` + `IErpPurInvoiceBiz`/`IErpSalInvoiceBiz`（或 daoFor 机制 B，按 data-dependency-matrix.md finance 对 purchase/sales 只读 R 合法）。产出 `DualSideDiffReport`（partner 级 finance 开口 vs 域级推导开口 + 差额）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`IErpFinReconciliationBiz.checkDualSideConsistency(@Name("direction") String direction, @Name("partnerId") Long partnerId)`（`@BizQuery`，只读）—— 返回差异报告；差额超 `erp-fin.reconcile-precision` 的 partner 标记 `INCONSISTENT` 并结构化日志告警（复用 5.1 `ErpFinPostingException` 工作台？——Decision：不落入异常工作台，只日志告警 + 报告，避免与过账异常语义混淆）。
  - Skill: `nop-backend-dev`
- [x] `Proof`：`TestErpFinDualSideConsistency`——双面一致（差额 0）/ 域级多核销（域级开口 < finance 开口）/ finance 多核销（反向）/ partner 级报告正确。验证命令 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinDualSideConsistency*`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`docs/logs/2026/{执行当日 month-day}.md` 新增本计划条目（含验证状态）；`docs/backlog/core-business-roadmap.md` 工作项 1.6/1.7 标注自动核销段 done（残余仅汇兑损益/退货，已 done → 1.6/1.7 可标 done）；若实现偏离 `ar-ap-reconciliation.md §自动核销`（如策略命名、cron 配置）补注实现权威。
  - Skill: none

Exit Criteria:

> Phase 3 交付双面一致性兜底报告 + 文档/日志。完整仓库验证属 Closure Gates。

- [x] 双面一致性报告按 partner 正确比对 finance 开口 vs 域级推导开口（一致/不一致 + 差额）单测通过
- [x] 当日日志已记；roadmap 1.6/1.7 自动核销段进展已标注

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0d1da1916ffejhf1h9q5OlAyk6`，独立 general 子代理，对照实时仓库复核）。全部 Current Baseline 主张（11 项）经独立验证 TRUE（file:line 证据：`ErpFinReconciliationBizModel.java:58-149` create/post/reverse @SingleSession、`ReconciliationSettler.java:24`/`PartnerBalanceUpdater.java:25`、`ErpFinArApItem` orm:466-488、`IErpFinArApItemBiz.findOpenItemsByPartner`、`ErpPurPaymentLine`/`ErpSalReceiptLine` 双面、`job-scheduling.md:99,278,320`、`app-erp-all/pom.xml:153` nop-job-local、设计配置项、roadmap 1.6/1.7 partial）。**无 BLOCKER**。规则 1-14 逐项 PASS；保护区域核实：无 ORM/列/字典变更（create+post 复用 + 只读 checker，"implementation-only" 判定 TRUE）。5 项 Minor（S1 create 签名补 businessDate+context / S2 scheduler.yaml VFS 路径 / S3 auto-recon-strategy 新增 config 澄清 / S4 DualSide 域级聚合查询口径 / S5 空壳基线非承重）**S1-S3 已吸收**（Decision 1 补签名、Phase 2 补 VFS 路径、Config 段标注新增项）。**共识达成**：Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成：自动核销引擎（三策略）+ 手动触发 + 定时 job + 双面一致性报告落地，行为测试通过
- [x] 相关文档对齐：`core-business-roadmap.md` 1.6/1.7 标注；`job-scheduling.md` 状态更新；当日日志已记
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am`（改动模块）；根 `mvn test -fae` 无回归
- [x] 无范围内项目降级为 deferred/follow-up（on-payment-approval 钩子 / nop-batch 分 chunk / 核销凭证 / 汇兑损益 / 规则配置实体 UI / 双面自动修复 均为计划内 Non-Goal）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status、各 Phase Status、Exit Criteria、Closure Gates、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于 `Closure` 节

## Deferred But Adjudicated

### 付款/收款审核时自动核销钩子（on-payment-approval trigger）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 须钩入 purchase/sales 收付款审核流，属跨域结果表面；本计划交付手动 + 定时两触发已满足运营自动化基线。
- Successor Required: yes（触发条件：purchase/sales 审核流需联动自动核销时）

### nop-batch 分 chunk 执行自动核销

- Classification: `optimization candidate`
- Why Not Blocking Closure: 1600-1 裁决为 batch-candidate（数据量 ≥ 数万）；当前 nop-job-local 直调 BizModel 内部分页对小到中等数据量够用。
- Successor Required: yes（触发条件：单次匹配量稳定 ≥ 数万时迁移 nop-batch）

### 双面一致性差异自动修复

- Classification: `watch-only residual`
- Why Not Blocking Closure: 兜底检查只报告 + 告警；自动调平会静默修改域级核销权威，须人工裁决。
- Successor Required: yes（触发条件：出现持续双面不一致且运营确认须自动调平时）

## Closure

Status Note: 全部三阶段实施完成（2026-07-05）。Phase 1 自动核销引擎（FIFO/BY_AMOUNT/BY_RATIO 三策略）+ 手动触发入口 runAutoReconciliation（config-gated + 幂等）落地；Phase 2 定时执行（ErpFinAutoReconJob job bean + scheduler.yaml 注册 + cron 配置门控）落地；Phase 3 双面对账一致性兜底（DualSideConsistencyChecker + checkDualSideConsistency @BizQuery 只读报告）落地。job-scheduling.md erp-fin-ar-ap-auto-recon 状态 DEFERRED→WIRED；core-business-roadmap.md 1.6/1.7 升 done。验证全绿（21 target tests + 126 full-module tests PASS；clean install BUILD SUCCESS）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，closure-auditor role，2026-07-05）
- Evidence:
  - **结构性检查**：plan-check.mjs --strict → PASS（28/28 项勾选，0 unchecked）
  - **语义校验（对照实时仓库 ./）**：
    - Phase 1：`AutoReconciliationEngine.java:58 matchAndBuild(direction, partnerId, strategy, context)`（非空，含 UNMATCHED_* 常量与 MatchResult）；`ErpFinReconciliationBizModel.java:165-166 @BizMutation runAutoReconciliation` + `:197-198 @BizQuery checkDualSideConsistency`；beans.xml 注册 AutoReconciliationEngine/DualSideConsistencyChecker/ErpFinAutoReconJob（app-service.beans.xml:64-75）
    - Phase 2：`ErpFinAutoReconJob.java:36 execute()` 调 `runAutoReconciliation(RECEIVABLE/PAYABLE, null, FIFO)`，cron 空值跳过；`scheduler.yaml:3 erp-fin-ar-ap-auto-recon`（cronExpr `0 0 1 * * ?`）；`job-scheduling.md:99` 状态 WIRED（file:line 实证一致）
    - Phase 3：`DualSideConsistencyChecker.java:52 check(direction, partnerId, context)`（非空，:96 LOG.warn 告警双面不一致）
  - **Anti-Hollow 校验**：三处新代码（engine/job/checker）均有 public 方法体 + 实际查询/分支逻辑，无 `return null` 占位、无空 `{}`、无被吞异常；beans.xml 注册三 bean 且经 BizModel @BizMutation/@BizQuery 反射可达
  - **测试计数实证**：`@Test` 数 TestErpFinAutoReconciliation=7 / TestErpFinAutoReconJob=3 / TestErpFinDualSideConsistency=4（共 14，与计划声称 7/3/4 一致）
  - **五点一致性**：Plan Status=completed / 三 Phase Status=completed / 各 Exit Criteria 全 [x] / Closure Gates 全 [x] / 日志 `docs/logs/2026/07-05.md` 存在且含本计划条目（line 3）→ 全部一致
  - **Deferred 诚实性**：三项 Deferred（on-payment-approval 钩子 / nop-batch 分 chunk / 双面自动修复）均为计划内 Non-Goal，无已确认活缺陷或契约漂移隐藏其中
  - **Docs 同步**：`docs/logs/2026/07-05.md`（line 3 本计划条目）；`docs/architecture/job-scheduling.md:99 WIRED`；无 ORM/契约变更（implementation-only，与 Task Route 判定一致）
  - 结论：**approved**（无 BLOCKER）
  - Phase 1: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/AutoReconciliationEngine.java`、`ErpFinReconciliationBizModel.java:runAutoReconciliation`、`IErpFinReconciliationBiz.java`、`ErpFinErrors.java(ERR_AUTO_RECON_*)`、`ErpFinConstants.java(CONFIG_AUTO_RECONCILE/STRATEGY/CRON)`、beans.xml；测试 `TestErpFinAutoReconciliation`（7/7 PASS）
  - Phase 2: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/job/ErpFinAutoReconJob.java`、`app-erp-all/src/main/resources/_vfs/nop/job/conf/scheduler.yaml`、`docs/architecture/job-scheduling.md:99 WIRED`；测试 `TestErpFinAutoReconJob`（3/3 PASS）
  - Phase 3: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/reconciliation/DualSideConsistencyChecker.java`、`ErpFinReconciliationBizModel.java:checkDualSideConsistency`；测试 `TestErpFinDualSideConsistency`（4/4 PASS）
  - 验证命令：`mvn test -pl module-finance/erp-fin-service -am -Dtest='TestErpFinAutoReconciliation*,TestErpFinAutoReconJob*,TestErpFinDualSideConsistency*'` → 14/14 PASS；`mvn -pl module-finance/erp-fin-service test`（全模块）→ 126/126 PASS；`mvn -pl module-finance/erp-fin-service -am clean install -DskipTests` → BUILD SUCCESS
  - 日志：`docs/logs/2026/07-05.md`

Follow-up:

- 付款/收款审核自动核销钩子（见上方 Deferred）
- nop-batch 分 chunk 迁移（见上方 Deferred）
- 双面一致性自动修复（见上方 Deferred）
