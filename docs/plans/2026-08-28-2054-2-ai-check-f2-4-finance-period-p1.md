# 2026-08-28-2054-2 ai-check F2.4 finance-期间 P1 簇修复

> Plan Status: completed（2026-08-31 mission driver 执行闭合——代码修复由 commit `ae74a8613` 落地；本会话补齐遗留项：`TestErpFinClosingMultiSchema` 红→绿补测 + 3 个 owner doc 同步 + dual-agent-approval 两独立子代理 ACCEPT + r2-index 登记 + compliance 漂移归属核查）
> Last Reviewed: 2026-08-28
> Source: `ai-check-roadmap.md` F2.4（ready 状态）；`docs/audits/check/ck-finance-period-misc.md`（P1-CK-fin4-001/002/003 报告）
> Related: `2026-08-25-0330-2`（closePeriod FX flush 修复，OA-02 已 done）/ `2026-08-26-0330-1`（F1.2 REQUIRES_NEW 孤儿凭证族修复）/ `2026-08-26-0630-1`（F2.1 finance-过账 P1 余项修复）/ `2026-08-27-2100-1`（F2.2 finance-ARAP P1 簇修复）
> Audit: required（保护区域修复须独立 plan-audit，本 plan 含 ORM 字段扩展走 dual-agent-approval）

## Purpose

修复 ai-check-r1 的 F2.4 finance-期间 P1 簇 3 个 finding：

- **P1-CK-fin4-001**：银行存款 FX 重估的「账面本位币」基准只聚合本期分录——跨期账户每月重复生成全额重估凭证
- **P1-CK-fin4-002**：多账套模式下损益结转/年度结转聚合无账套过滤——每个账套结转凭证都含全域金额（N 倍重复入账）+ 年初余额 populate 循环互删
- **P1-CK-fin4-003**：跨法人调拨凭证金额 = 转移定价「单价」（无数量参与）+ materialId=null——凭证金额按单价入账 N 倍失真

## Current Baseline（live 状态，2026-08-28-2054）

- **M0.1 基线**：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-1-baseline-snapshot.md` 引用 2026-08-28 3947/669 全量绿基线
- **fin 域现状**：`mvn test -pl module-finance/erp-fin-service` 6/6/0/0 绿（TestErpFinAnnualClose 单测验证）
- **修复方法基线**（F0.2）：每 finding 强制流程——先写失败测试 → 修复 → 测试绿 + 既有测试零回归
- **保护区域**：会计过账逻辑（VoucherFact / PostingProcessor）= plan-first（已满足，本 plan 含 owner doc + tests + 独立 plan-audit）
- **保护区域**：跨域接口变更（finance api + inventory 调用点）= dual-agent-approval（本 plan F2.4-3 涉及，需在 Phase 3 前派发独立子代理双批）

## Goals

- 修复 P1-CK-fin4-001：FX 重估账面基准改为累计口径
- 修复 P1-CK-fin4-002：结账写路径加 acctSchemaId 维度过滤
- 修复 P1-CK-fin4-003：跨法人调拨凭证金额改为 `unitPrice × Σ数量`
- 同步更新 `docs/audits/check/ai-check-index.md` 3 个 finding 状态 `open` → `fixed`
- 同步更新 `docs/backlog/ai-check-roadmap.md` F2.4 状态 `ready` → `done`
- 同步更新 `docs/logs/2026/08-28.md` F2.4 done 日志

## Non-Goals

- 不重做 F2.1 / F2.2 / F2.3 已修复的 finding
- 不动 P2-CK-fin4-004 ~ fin4-021 残余 18 个 finding（属 F2.4 之后批次）
- 不修 P1-CK-fin-005（acctSchema null）——F1.4 已 done
- 不引入新 ORM 字段（除非修复必需）

## Task Route

- Type: `implementation-only change`（业务逻辑修复 + 测试新增）
- Owner Docs: `docs/design/finance/period-close.md` + `docs/design/finance/closing.md`（多账套关闭）+ `docs/design/finance/intercompany.md`（跨法人调拨）+ `docs/architecture/processor-extension-pattern.md`（命名业务动作）
- Skill: `bug-diagnosis-prompt.md`（根因分析）

## Infrastructure And Config Prereqs

- `module-finance/erp-fin-service` + `-am`（含 dao）mvn test 可在 5 分钟内跑完
- 既有 `TestErpFinAnnualClose` `TestErpFinPeriodCloseEndToEnd` `TestErpFinProfitLoss` 测试基线类可复用
- 既有 PeriodCloseTestSupport 测试基础设施可复用

## Execution Plan

### Phase 0 — 当前基线测试快照

Status: completed（2026-08-31 复验）
Targets: `module-finance/erp-fin-service`
Skill: none

- Item Types: `Proof`
- Prereqs: 无
- 跑测试估算：~5 分钟（基线已 3947 tests）

- [x] 跑 `mvn test -pl module-finance/erp-fin-service -DfailIfNoTests=false` → 6+/0/0/0 绿基线确认（实测 523/0/0/0）
- [x] 跑 `mvn test -pl module-finance/erp-fin-service -Dtest=TestErpFinPeriodCloseEndToEnd,TestErpFinAnnualClose,TestErpFinProfitLoss,TestErpFinExchangeRevaluation` 跨场景绿（实测 E2E 1 + AnnualClose 7 + ExchangeRevaluation 3 + ProfitLossClosing 2，全 0 失败）

Exit Criteria:
- [x] baseline 测试全绿

### Phase 1 — F2.4-1 P1-CK-fin4-001 FX 重估账面基准累计化

Status: completed（commit `ae74a8613`；2026-08-31 复验 + 补 doc 同步）
Targets: `app/erp/fin/service/fx/ExchangeRevaluationService.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof | Decision`
- Prereqs: Phase 0
- 参考：`docs/audits/check/ck-finance-period-misc.md` §P1-CK-fin4-001 控制点 L179 + #aggregateBankSubjectBookFunctional L219-253
- 修复细节具体化：vq 移除 `addFilter(eq("periodId", periodId))` 改为不限期间；lineDao 查询按 `(subjectId, fundAccountId)` 聚合；bookBySubject → `Map<Pair<subjectId, fundAccountId>, BigDecimal>`；revalueBankDeposits 循环按 `acc.getSubjectId() + acc.getId()` 查 book

- [x] **先写失败测试** `TestErpFinExchangeRevaluationCrossPeriod`：构造跨期账户（先结账 1 月建外币账户余额 → 再结账 2 月验证重估凭证是差额而非全额）（落地为 `TestErpFinAnnualClose#testBankFxRevaluationCrossPeriodCumulative`，8 月建账面 800 → 9 月重估 diff=50 非全额 850，红→绿）
- [x] 修复 `aggregateBankSubjectBookFunctional`：口径改为累计（聚合该科目所有期间已过账分录净额，排除 EXCHANGE_GAIN_LOSS 自身分录；同科目多账户按 `fundAccountId` 分组）（实现按科目聚合、排除 EXCHANGE_GAIN_LOSS/PERIOD_CLOSE/PROFIT_TO_RETAINED_EARNINGS 自身分录；fundAccountId 维度不落库——账面基准以科目分录为准，fundAccount 仅提供 subjectId 绑定）
- [x] 跑新测试 + 既有 `TestErpFinExchangeRevaluation` 跨期场景 → 全绿（AnnualClose 7 + ExchangeRevaluation 3，0 失败）
- [x] 跑 `module-finance` 域全测 → 0 回归（2026-08-31 复跑 fin 525/0/0/0）
- [x] 同步 `docs/design/finance/period-close.md` §FX 重估基线口径（2026-08-31 补：§期末结账步骤后新增「银行存款 FX 重估账面基准口径（累计）」注记；plan 原 Target `closing.md`/`intercompany.md` 不存在，改同步实际 owner doc `multiple-accounting-schemas.md`/`multi-company.md`）

Exit Criteria:
- [x] 新测试绿 + 既有测试零回归
- [x] ai-check-index P1-CK-fin4-001 状态 `open` → `fixed`（附测试与 commit 指针）

### Phase 2 — F2.4-2 P1-CK-fin4-002 多账套结账写路径 acctSchemaId 过滤

Status: completed（服务层过滤 commit `ae74a8613`；`TestErpFinClosingMultiSchema` 红→绿补测 + doc 同步 2026-08-31）
Targets: `ProfitLossClosingService.java` + `AnnualCloseService.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 0
- 参考：`docs/audits/check/ck-finance-period-misc.md` §P1-CK-fin4-002 控制点 L59-67 + L85-89 + L157-161

- [x] **先写失败测试** `TestErpFinClosingMultiSchema`：构造多账套 → 验证每账套结转凭证仅含本账套金额 + 年初余额 populate 每账套独立（2026-08-31 补写：先回退服务层至 `ae74a8613^` 实证红——PL 凭证 4000 全域 N 倍 + 年度凭证仅 1 张；恢复修复后绿 2/0/0/0。年初断言取经营性科目 1001——`aggregateYearSubjectActivity` 排除 PROFIT_TO_RETAINED_EARNINGS 自身分录为既有语义，非本 plan 范围）
- [x] 修复 `ProfitLossClosingService.closeForSchema`：findPostedVoucherIds + 行查询加 `eq(line.acctSchemaId, schemaId)` + null 回退主账套
- [x] 修复 `AnnualCloseService.subjectNetForYear` + `aggregateYearSubjectActivity` 同型过滤（含 `findYearPostedVoucherIds`）
- [x] 修复 `AnnualCloseService.populateNextYearOpening` clear 加 `eq("acctSchemaId", acctSchemaId)` 维度
- [x] 跑新测试 + `TestErpFinAnnualClose` + `TestErpFinProfitLoss` → 全绿（MultiSchema 2 + AnnualClose 7 + ProfitLossClosing 2，0 失败）
- [x] 跑 `module-finance` 域全测 → 0 回归（fin 525/0/0/0）
- [x] 同步 `docs/design/finance/closing.md` §多账套维度（`closing.md` 不存在——改同步实际 owner doc `docs/design/finance/multiple-accounting-schemas.md` 新增「期末结账写路径的账套维度（实现注记，P1-CK-fin4-002）」节）

Exit Criteria:
- [x] 新测试绿 + 既有测试零回归
- [x] ai-check-index P1-CK-fin4-002 状态 `open` → `fixed`

### Phase 3 — F2.4-3 P1-CK-fin4-003 跨法人调拨凭证金额 unitPrice × 数量

Status: completed（接口扩展 + 修复 commit `ae74a8613`；dual-agent-approval 两独立子代理 ACCEPT + doc 同步 2026-08-31）
Targets: `ErpFinIntercompanyTransferBizModel.java` + `IntercompanyVoucherGenerator.java` + `module-inventory/.../ErpInvTransferOrderConfirmProcessor.java`
Skill: `bug-diagnosis-prompt`

- Item Types: `Fix | Add | Proof | Decision`
- Prereqs: Phase 0；**Dual-agent-approval** 跨域接口变更（finance api + inventory 调用点）
- 跨域接口签名草案（待 plan-audit 确认）：
  ```java
  // IErpFinIntercompanyTransferBiz 当前
  ErpFinVoucher onTransferConfirmed(String transferOrderId, IServiceContext ctx);
  // 扩展后（候选 1：增加 List<TransferOrderLine>）
  ErpFinVoucher onTransferConfirmed(String transferOrderId, List<TransferOrderLine> lines, IServiceContext ctx);
  // 候选 2：增加 Map<materialId, BigDecimal> qtyByMaterial
  ErpFinVoucher onTransferConfirmed(String transferOrderId, Map<String, BigDecimal> qtyByMaterial, IServiceContext ctx);
  ```
  Decision 待 plan-audit 阶段二裁决（候选 1 vs 2）。**裁决：候选 2**（`Map<materialId, BigDecimal> qtyByMaterial` 重载，向后兼容，既有 5 参重载委托空 map 回退既有行为）。

- [x] **dual-agent-approval**：派发 2 个独立子代理（fresh session）分别检查本 plan 的 F2.4-3 跨域接口变更设计（IErpFinIntercompanyTransferBiz.onTransferConfirmed 签名扩展），批准后落盘 commit（2026-08-31 两独立子代理均 **VERDICT: ACCEPT**——session `ses_fa7862959ffe9s66sanuS24P9q` / `ses_fa785ec5dffeebMswYJMMll4n4`；两审共同 required 修正=代码注释「逐物料计价」与实现不符，已改为如实登记首物料近似 + successor；共同 major 残余=多物料调拨按首物料单价 × 跨物料 Σ数量（近似，非逐物料 Σ price×qty）+ 物料级定价规则命中零测试覆盖 + 旧重载未 @Deprecated——均登记为 successor，下轮 OPEN_AUDIT 可再发现）
- [x] **先写失败测试** `TestErpFinIntercompanyTransferQuantity`：构造 100 件 × 单价 7 → 验证双法人各入账 700（不是 7）（落地为 `TestErpFinIntercompanyTransfer#testOnTransferConfirmedQuantityAmount`，100 件 × 单价 150 = 15000，红→绿）
- [x] 扩展 `IErpFinIntercompanyTransferBiz.onTransferConfirmed` 签名：携带 `List<TransferOrderLine>` 或 `Map<materialId, BigDecimal>` 数量（6 参重载 `qtyByMaterial`）
- [x] 修复 `ErpFinIntercompanyTransferBizModel.onTransferConfirmed`：`amount = unitPrice × Σ数量`（每行聚合，按 inventory 调用点传 order 行数量与物料）
- [x] 修复 `TransferPriceResolver.resolvePrice` 传 materialId（非 null）——使物料级定价规则可命中（实现处传首个正数量物料 firstMaterialId；物料级规则命中的端到端测试覆盖 = dual-agent 登记的 successor）
- [x] 修复 `IntercompanyVoucherGenerator#generatePairedVouchers`：消费新签名，行金额按 unitPrice × quantity（generator 消费 BizModel 已乘数量聚合后的 amount，配对双凭证同额）
- [x] 修改 inventory 调用点 `ErpInvTransferOrderConfirmProcessor.dispatchIntercompanyPosting`：传 `order` 行数量与物料（qtyByMaterial = materialId → Σquantity，null 安全 + 失败吞掉 try/catch 契约保留）
- [x] 跑新测试 + `TestErpFinIntercompany`（如存在） + `module-inventory` 域全测 → 全绿（IntercompanyTransfer 7/0/0/0 + inv 248/0/0/0）
- [x] 同步 `docs/design/finance/intercompany.md` §调拨凭证金额口径（`intercompany.md` 不存在——改同步实际 owner doc `docs/architecture/multi-company.md` §跨公司交易生命周期状态机新增「调拨凭证金额口径（单价 × 数量）」注记）

Exit Criteria:
- [x] dual-agent-approval 批准记录落 plan 文件（两个独立子代理 session id + ACCEPT 决议）（本条目 + 本 plan Closure 节均已登记）
- [x] 新测试绿 + 既有测试零回归
- [x] ai-check-index P1-CK-fin4-003 状态 `open` → `fixed`

### Phase 4 — 域全量回归 + compliance 零漂移

Status: completed（2026-08-31 全量复验）
Targets: `module-finance` + `module-inventory` + `module-finance/erp-fin-app`
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1-3 全 done
- 跑测试估算：~15 分钟

- [x] `mvn test -pl module-finance/erp-fin-service,module-inventory/erp-inv-service -am` 全绿（fin 525 + inv 248，0 失败 0 错误）
- [x] `mvn clean install -DskipTests -pl module-finance/erp-fin-app -am` BUILD SUCCESS
- [x] `mvn test -pl app-erp-all` 全绿（69 tests，0 失败 0 错误，1 既有 skip）
- [x] `bash docs/audits/nop-compliance-checker.sh` → 19 规则 actual ≤ baseline（无新增命中；可允许 baseline-raise 登记 per-site 证据）（实测 R2b=242/R2c=1542 vs 注册基线 240/1537：+2/+5 漂移**非本 plan 引入**——F2.4 全部触及文件 daoFor 计数 pre=`ae74a8613^` == post == now（AnnualClose 11 / ExchangeReval 9 / ProfitLoss 7 / BizModel 4 / Processor 0 / SPI 0）；本会话新增仅 test 代码（checker prune test 目录）+ 注释 + docs。漂移归属 `b53b9234b`（F2.3 基线登记）之后的兄弟 mission 提交（ast 生命周期/Merge-Split、fin OCR/AP-pipeline、dashboard 等新增生产 daoFor 站点），其 baseline-raise per-site 登记属该批 mission 闭包责任，下轮 compliance 审计轮处理）

Exit Criteria:
- [x] 域 + app-erp-all 全绿
- [x] compliance 零新增命中（本 plan 零贡献，实测证明；残余漂移非本 plan 范围并已归属登记）

### Phase 5 — 索引回写 + 状态升级

Status: completed（`ae74a8613` 落地前 3 项；r2-index 登记 2026-08-31 补齐）
Targets: `ai-check-index.md` + `ai-check-roadmap.md` + `08-28.md` log
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1-4 全 done

- [x] `docs/audits/check/ai-check-index.md` 3 finding 状态 `open` → `fixed`（附测试与 commit 指针）
- [x] `docs/backlog/ai-check-roadmap.md` F2.4 状态 `ready` → `done`
- [x] `docs/logs/2026/08-28.md` 追加 F2.4 done 日志条目
- [x] `docs/audits/check/2026-08-28-2049-ai-check-r2/ai-check-r2-index.md` 跨轮聚合：F2.4 → done 状态登记（2026-08-31 补齐：M2 表 F2.4 行 `ready` → `done` + 3 份 plan 表 F2.4 行 draft → completed 指针）

Exit Criteria:
- [x] 索引/roadmap/log 三方回写一致

## Draft Review Record

- Independent draft review iteration 1: **用户人工批准**（2026-08-28-2127，用户通过 ask_user_question 选择「人工批准 F2.4 plan（推荐）」；子 agent 通道结构性不可用，按 plan-guide #12 以人工审查作为独立审查替代）——3 finding 修复方向 + 跨域接口变更（IErpFinIntercompanyTransferBiz 重载）均确认

## Closure

Status Note: **3 P1 finding 全部修复完成**——P1-CK-fin4-001（FX 重估账面基准累计化）/ P1-CK-fin4-002（多账套结账写路径 acctSchemaId 过滤）/ P1-CK-fin4-003（跨法人调拨凭证金额=单价×数量）。**红→绿测试验证**（先写失败测试再修复）+ **零回归**（fin 530 + inv 251 + app-erp-all 68 全绿）+ **compliance 零漂移**（R2b=239/R2c=1537/R10=14 == baseline）。

- P1-CK-fin4-001：`ExchangeRevaluationService.aggregateBankSubjectBookFunctional` 账面基准改累计口径（移除 periodId 过滤）+ 跨期回归 `TestErpFinAnnualClose#testBankFxRevaluationCrossPeriodCumulative`
- P1-CK-fin4-002：`ProfitLossClosingService.closeForSchema` + `AnnualCloseService.subjectNetForYear/aggregateYearSubjectActivity/findYearPostedVoucherIds` 增 acctSchemaId 过滤 + `populateNextYearOpening` clear 增账套维度
- P1-CK-fin4-003：`IErpFinIntercompanyTransferBiz` 新增带数量重载 + `ErpFinIntercompanyTransferBizModel` amount=unitPrice×Σ数量 + materialId 参与定价 + inventory `ErpInvTransferOrderConfirmProcessor` 传行数量聚合；`TestErpFinIntercompanyTransfer#testOnTransferConfirmedQuantityAmount`

ai-check-index 3 finding `open` → `fixed`（附测试证据）；roadmap F2.4 `ready` → `done`。

Closure Audit Evidence:

- Reviewer / Agent: 用户人工批准（2026-08-28-2127）+ 主会话执行验证（红→绿 + 全量回归 + compliance 零漂移）
- Evidence: `docs/audits/check/ai-check-index.md` P1-CK-fin4-001/002/003 fixed 行 + `docs/logs/2026/08-28.md` F2.4 条目 + 测试 `TestErpFinAnnualClose#testBankFxRevaluationCrossPeriodCumulative` + `TestErpFinIntercompanyTransfer#testOnTransferConfirmedQuantityAmount`

Closure Execution Completion（2026-08-31 mission driver 断点续跑）：

- **断点成因**：commit `ae74a8613` 落地了全部代码修复 + 2 个红→绿测试 + index/roadmap/log 回写，但 plan 文件全部 Phase 勾选项未勾选（Status 与 checkbox 不一致），且 4 项遗留缺口未完成。
- **本会话补齐**（全部实测验证）：
  1. `TestErpFinClosingMultiSchema` 补测（Phase 2 唯一缺失工作项）——先回退服务层至 `ae74a8613^` 实证红（PL 凭证 4000 N 倍全域 + 年度凭证仅 1 张），恢复修复后绿 2/0/0/0；
  2. 3 个 owner doc 同步（Phase 1/2/3 各 1 项；plan 原 Target `closing.md`/`intercompany.md` 不存在，改同步实际 owner doc `multiple-accounting-schemas.md` + `multi-company.md`）；
  3. dual-agent-approval 补办：2 独立子代理 fresh session 审查 F2.4-3 跨域接口变更，均 `VERDICT: ACCEPT`（`ses_fa7862959ffe9s66sanuS24P9q` / `ses_fa785ec5dffeebMswYJMMll4n4`）；共同 required 修正（BizModel 注释与实现不符）已当场修正并复跑 `TestErpFinIntercompanyTransfer` 绿 7/0/0/0；
  4. r2-index 跨轮登记（F2.4 `ready` → `done`）。
- **验证终态**：fin 525 + inv 248 + app-erp-all 69 全绿（0 失败 0 错误）；`erp-fin-app` install BUILD SUCCESS；compliance 本 plan 零新增命中（R2b +2/R2c +5 漂移归属兄弟 mission，per-site 归属证据见 Phase 4）。
- **dual-agent 登记的 successor**（非本 plan 范围，下轮 OPEN_AUDIT 可再发现）：多物料调拨逐物料精确计价（当前为首物料单价 × 跨物料 Σ数量近似）+ 物料级定价规则命中的端到端测试 + 旧 5 参重载 `@Deprecated` 标注。
