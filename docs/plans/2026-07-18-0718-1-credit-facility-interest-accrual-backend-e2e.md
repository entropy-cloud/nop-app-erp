# 2026-07-18-0718-1-credit-facility-interest-accrual-backend-e2e Finance 银行授信额度利息计提（CREDIT_FACILITY_INTEREST）后端实现 + 浏览器层 E2E

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Mission: erp
> Work Item: finance treasury 后端设计-实现差距收口（授信额度利息计提 successor）
> Source: `docs/plans/2026-07-17-2256-1-fin-treasury-cash-forecast-credit-facility-e2e.md` Deferred But Adjudicated「授信额度利息计提（CREDIT_FACILITY_INTEREST）」(l.186-190) — Successor Required: yes，触发条件「授信利息计提后端落地时」。
> 触发条件经实时仓库核实**可由本计划主动驱动满足**：`ErpFinBusinessType.CREDIT_FACILITY_INTEREST(250)` 枚举已声明（`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java:37`）+ 字典项 `value: CREDIT_FACILITY_INTEREST` 已存在（`module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/business-type.dict.yaml:104-106`），但 (a) `ErpFinCreditFacilityBizModel` 仅 `reserveCredit`/`releaseCredit` 两 `@BizMutation`，无利息计提入口；(b) `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/` 无 `CreditFacilityInterestAcctDocProvider`；(c) `ErpFinCreditFacility` 实体无 `interestRate` 列（ORM 实测 18 列，无利率字段）；(d) 全仓 grep `accrueInterest`/`InterestRate`（src/main）零命中。设计 owner doc `docs/design/finance/treasury.md:148` 已规定 `CREDIT_FACILITY_INTEREST` = 借 财务费用-利息支出 / 贷 银行存款。本计划落地后端 @BizMutation + Provider + VoucherBuilder + bean 注册 + 配置化利率（不动 ORM）+ JUnit + 浏览器层 E2E，主动解除该 Deferred。
> Related: `2026-07-17-2256-1`（前置覆盖 reserve/release 浏览器层，本计划补其利息 Deferred）；`2026-07-12-0413-2-finance-bank-recon-bad-debt-e2e.md`（bank-recon `BankReconAdjustmentVoucherBuilder.post` 范式源 + finance 业务动作 + 种子科目补齐范式源）；`2026-07-18-0718-2-employee-advance-cash-repay-backend-e2e.md`（同批 N=2，finance 域不同 owner doc 独立推进，无依赖）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18，`read`/`grep`/`awk` 实测，非采信旧记忆）：

### 后端差距：枚举/字典/设计均已就绪，零实现

- **业务类型枚举已声明**：`ErpFinBusinessType.CREDIT_FACILITY_INTEREST(250)`（`ErpFinBusinessType.java:37`）。
- **字典项已存在**（无需新增）：`module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/business-type.dict.yaml:104-106`：
  ```yaml
  - label: 授信利息
    value: CREDIT_FACILITY_INTEREST
    description:
  ```
- **owner doc 设计已就绪**：`docs/design/finance/treasury.md:148` 明示「CREDIT_FACILITY_INTEREST | 授信利息 | 借：财务费用-利息支出 / 贷：银行存款」。`§业财过账` 段复用 `IErpFinAcctDocProvider`（`posting.md`），无新机制。
- **`ErpFinCreditFacilityBizModel` 仅占用回写无利息计提**（实测 `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinCreditFacilityBizModel.java` 全文 82 行）：
  - `reserveCredit(creditFacilityId, amount)` 强一致校验 + `usedAmount += amount` + `availableAmount = total − used` 同步重算（:32-50）。
  - `releaseCredit(creditFacilityId, amount)` 反向（:53-66）。
  - 无利息计提 `@BizMutation`；无利率读取；无凭证生成委派。
- **`ErpFinCreditFacility` 实体（实测 `module-finance/model/app-erp-finance.orm.xml:1430-1456` 18 列）**：`fundAccountId`（→ 银行账户）+ `totalAmount`/`usedAmount`/`availableAmount`（额度三值）+ `validFrom`/`validTo`（生效/到期日，计息日期区间边界）+ `facilityType`（授信类型字典 `erp-fin/credit-facility-type`）+ `orgId`。**无 `interestRate` 列**——利率须经 config 读取（避免 ORM 保护区域变更）。
- **`IErpFinAcctDocProvider` 范式已稳定**：`NotesPayableAcctDocProvider`（`module-finance/erp-fin-service/.../posting/provider/NotesPayableAcctDocProvider.java`）展示标准实现——`getSupportedBusinessTypes()` + `createFacts(PostingEvent, AcctDocContext)` 经 `VoucherFact` 列表返回。本计划镜像此范式。
- **Provider 注册机制（实测）**：`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml:14-24` 经 `<ioc:collect-beans by-type="...IErpFinAcctDocProvider" only-concrete-classes="true">` 收集**显式声明的 `<bean>`**（非 component scan）；既有 Provider 各自在同文件显式 `<bean id="...NotesPayableAcctDocProvider" class="..."/>` 声明（l.99-125）。本计划新增 Provider 须追加 `<bean>` 声明。
- **`IErpFinVoucherBiz.post` 签名与幂等性（实测 `IErpFinVoucherBiz.java:22-32`）**：`@Transactional(REQUIRES_NEW)` + `Long post(PostingEvent event, IServiceContext context)` 返回新建 voucherId；Javadoc 明示「**幂等：源单据已过账时返回 null**」+「@return 新建凭证 ID；源单据已过账（幂等命中）返回 `null`」。`ErpFinPostingProcessor.alreadyPosted`（:462-474）按 `(billCode=billHeadCode, businessType)` 反查 `ErpFinVoucherBillR`，存在非红冲 POSTED 凭证时返回 true → `process` early-return null（:123-127）。**幂等键 = (billHeadCode, businessType)**。

### 业财过账触发范式（VoucherBuilder 模式）

经实时仓库核实 `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/bankrecon/`：
- **`BankReconAdjustmentVoucherBuilder.post`**（`BankReconAdjustmentVoucherBuilder.java:80-94`）展示「BizModel 委派 Builder 构造 PostingEvent + 调 `IErpFinVoucherBiz.post`」范式（**非 BizModel 直调**）：
  - `ErpFinBankReconciliationBizModel.post`（:34）→ `BankReconciliationBuilder.post`（:127）→ `BankReconAdjustmentVoucherBuilder.post`（:80-94）→ `voucherBiz.post`。
  - Builder 内经 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)`（:148）解析 acctSchemaId。
- **本计划场景**：授信利息计提**无源单据状态机迁移**（facility 实体本身不变更 status），更像「定期/手动触发的一次性计提」。故采用 **VoucherBuilder 模式**（对齐 `BankReconAdjustmentVoucherBuilder` 范式），新建 `CreditFacilityInterestVoucherBuilder.post(facility, fromDate, toDate, interestAmount, context)`。BizModel `accrueInterest` 计算金额 + 委派 Builder。利于单测（Builder 可独立测试 PostingEvent 构造与 `voucherBiz.post` 调用，BizModel 单测金额计算）。

### 浏览器层 E2E 范式（复用，零范式新增）

- `_helper.ts` 三原语 `createViaSave`/`callMutation`/`verifyState` + `findFirst`/`deleteByFilter`（0814-2 / 1249-1 起）。凭证行数值断言范式 `findVoucherIdByBillCode(billCode, postingType)` + `assertVoucherLines(voucherId, expectedLines)`（`tests/e2e/orchestration/_helper.ts:92,151`，0704-1/0742-1 起稳定）。本计划直接复用，零 helper 扩展。
- 自包含 setup（建测试专用 CreditFacility 隔离）+ finally 兜底 cleanup（凭证 + facility 逐域删，保护 finance 看板/报表数值断言基线）。
- GraphQL 入参序列化：`BigDecimal` 经 String scalar 接受（对齐 2256-1 `fin-credit-facility.action.spec.ts` reserveCredit(amount: "300") 范式）；`LocalDate` 同样 String scalar ISO 日期（对齐 fin-cash-forecast refreshForecast 范式）；返回 Long 标量无选择集，对齐 fin-voucher-post `gql.raw` 长标量返回范式。

### 剩余差距

1. `ErpFinCreditFacilityBizModel.accrueInterest` `@BizMutation` 缺失（利息计算 + 委派 Builder + 返回 voucherId）。
2. `CreditFacilityInterestVoucherBuilder` 缺失（PostingEvent 构造 + 调 voucherBiz.post）。
3. `CreditFacilityInterestAcctDocProvider` 缺失（Dr 6603 / Cr 1002 凭证行生成）+ bean 注册声明缺失。
4. 利率读取机制缺失（config 键 `erp-fin.credit-facility-default-interest-rate` + `ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE` 常量 + BizModel 内 `AppConfig.var(...)` 内联读取，对齐既有 `CONFIG_AUTO_RECONCILE`/`CONFIG_BAD_DEBT_*` 范式）。
5. 种子 COA：`erp_md_subject.csv` 已含 `6603`（财务费用-利息支出，1430-1 补齐）+ `1002`（银行存款，1234-1 基础科目）—— **无需新科目**（实测两科目已在种子）。
6. 浏览器层 E2E 缺失（无 `fin-credit-facility-interest*` spec，实测 grep NONE）。

## Goals

- 后端落地 `accrueInterest(@Name creditFacilityId, @Name fromDate, @Name toDate, IServiceContext)` `@BizMutation` 返回 `Long`（生成的 voucherId；幂等命中返回 null）：
  - 计息公式：`interest = usedAmount × annualRate × days / 360`（360 天惯例对齐票据贴现 `discountInterest = 票面 × 贴现率 × 剩余天数 / 360` 范式 `ErpFinNotesReceivableProcessor.java:231-234`；`usedAmount` 取计息开始时点 facility.`usedAmount`——决策门 Phase 1）。
  - 经新建 `CreditFacilityInterestVoucherBuilder.post(...)` 构造 PostingEvent（businessType=CREDIT_FACILITY_INTEREST + billHeadCode + billData 携带科目码集 + 利息金额 + orgId/currencyId/acctSchemaId 派生）+ 调 `IErpFinVoucherBiz.post` → 经 `ErpFinAcctDocRegistry` 路由到新增的 `CreditFacilityInterestAcctDocProvider`。
  - billHeadCode = `"CFI-INT-" + facilityId + "-" + fromDate + "_" + toDate`——**幂等键组成**（同 facility + 同区间二次调用经 `IErpFinVoucherBiz.post` 内置 `alreadyPosted` 命中返回 null，无第二张凭证）。
  - `partnerId=null`（Dr 6603 财务费用无 partner 要求；Cr 1002 银行存款 partner 字段不适用——`ErpFinFundAccount` ORM 实测无 `partnerId` 列，bank counterpart 不在 partner 表中）。
- 新建 `CreditFacilityInterestVoucherBuilder`（`module-finance/erp-fin-service/.../entity/` 或 `treasury/` 子包，对齐 `BankReconAdjustmentVoucherBuilder` 包路径）：`post(facility, fromDate, toDate, interestAmount, context) → Long`。注入 `IDaoProvider`/`IErpFinVoucherBiz`；经 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, facility.getOrgId())` 解析 acctSchemaId（**不**经不存在的 `IErpFinAcctSchemaBiz`）。
- 新增 `CreditFacilityInterestAcctDocProvider implements IErpFinAcctDocProvider`：`getSupportedBusinessTypes()={CREDIT_FACILITY_INTEREST}` + `createFacts` 返回 2 行 VoucherFact（Dr 6603 财务费用-利息支出 + Cr 1002 银行存款，amount 来自 `billData.TOTAL`）。
- 显式 bean 注册：`_vfs/erp/fin/beans/app-service.beans.xml` 追加 `<bean id="...CreditFacilityInterestAcctDocProvider" class="...CreditFacilityInterestAcctDocProvider"/>`（l.99-125 既有 Provider 声明段），对齐 `NotesPayableAcctDocProvider` 范式。
- 利率机制：新增 `ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE = "erp-fin.credit-facility-default-interest-rate"`（treasury config 块，l.223+ 起）+ BizModel 内 `AppConfig.var(ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE, BigDecimal.ZERO)` 内联读取（**不**新增 `ErpFinConfigs` reader 方法——该接口为空，既有范式为内联 `AppConfig.var`）。**不**加 ORM 列（保护区域）。
- **JUnit**：`TestErpFinCreditFacilityInterest` 覆盖正路径（计息 + 凭证生成 + facility 不变）+ 幂等命中（同参数二次调用返回 null + 无第二张凭证）+ 边界（fromDate>toDate / usedAmount=0 跳过返回 null / rate=0 抛守卫）+ Provider 单元测试。
- **浏览器层 E2E**（1 新 spec `fin-credit-facility-interest.action.spec.ts`）：自包含 setup CreditFacility → reserve(300) 占用（或直 `__save` 置 usedAmount=300）→ `accrueInterest(facilityId, "2026-08-01", "2026-08-31")` → 断言 facility 不变 + 经 `findVoucherIdByBillCode("CFI-INT-<id>-2026-08-01_2026-08-31", "NORMAL")` 反查凭证存在 + `assertVoucherLines` Dr 6603 / Cr 1002 精确数值（按 config rate × usedAmount × days/360 派生，5% × 300 × 31 / 360 = 1.2917）。
- **owner doc 收口**：解除 2256-1 Deferred「授信额度利息计提（CREDIT_FACILITY_INTEREST）」（补 `**RELEASED by 2026-07-18-0718-1**`）；`treasury.md §业财过账` 表格行下补实现注记（rate 来源 config / 计息公式 / 360 天基准 / 内置幂等机制）；`e2e-runbook` 业务动作表 + finance 利息计提行 + 凭证行断言表 +1 行 + 套件计数对齐；当日日志聚合条目。

## Non-Goals

- **不加 `ErpFinCreditFacility.interestRate` ORM 列**：利率属配置面（同 `CONFIG_BAD_DEBT_LOSS_RATE_*` config 范式），加列需 ORM 模型变更（保护区域，需人工批准）。facility 级利率覆盖归 successor（触发条件：不同 facility 不同利率的业务需求落地时）。
- **不做 facility 实体字段扩展（`lastAccrualDate`/`accruedInterestAmount`）**：利息计提记录经 `ErpFinVoucher`（billHeadCode 含 facilityId + 区间）+ `ErpFinVoucherBillR` 业财回链反查，无需在 facility 实体物化累计字段。owner doc `expense-claim.md §借款金额维度建议` 范式提示「ORM 字段建议属保护区域」，本计划对齐保守路径——查询经凭证反查。
- **不做 nop-job 定时自动计提**：cron 触发非浏览器面 mutation 入口（对齐 `ErpFinCashForecastJob` 范式由后端单测覆盖）；本计划仅手动 `accrueInterest` mutation 入口；定时执行归 successor（触发条件：产品要求按月自动计提时）。
- **不做多 facility 批量计提入口**：本计划单 facility 计提；批量入口（`accrueAllFacilities(fromDate, toDate)`）归 successor（触发条件：批量定期计提业务需求落地时）。
- **不做外币利息汇兑损益**：本位币计息（facility.`currencyId` 与账套本位币一致）；外币利息的汇兑损益分解归 successor（对齐 EXCHANGE_GAIN_LOSS 范式，触发条件：外币授信业务落地时）。
- **不做利息资本化（资本化到资产/Inventory）路径**：本计划利息全部费用化（Dr 财务费用）；资本化（如购建固定资产期间的利息资本化）属不同结果面 successor。
- **不做 `reverse` 红冲浏览器层 E2E**：通用 `ErpFinVoucher__reverse(billHeadCode, businessType)` 已由 2004-2 orchestration 覆盖 DIRECT 红冲全路径；本计划利息凭证生成后红冲复用既有路径（红字凭证行同向取负断言对齐 0742-1 范式可作 spec 内 1 assertion，不专设 spec）。
- **不做 2256-1 两项 Non-Goal 的解除**：现金预测多账户分摊 / 现金预测定时执行浏览器层观测仍归原 Deferred（不同结果面，触发条件未满足）。
- **不做幂等键组成变体**：默认 billHeadCode = `CFI-INT-{facilityId}-{fromDate}_{toDate}`（区间级幂等）；分期间累计/按月计提的幂等键变体（如按 calendar month 而非任意 fromDate/toDate 区间）归 successor（触发条件：财务制度要求按月固定计提时）。

## Task Route

- Type: `implementation-only change`（finance service 层 BizModel 新增 `@BizMutation` + 新增 VoucherBuilder + 新增 AcctDocProvider + bean 注册 + config 常量 + ErrorCode + 测试 + 浏览器层 E2E；ORM/契约/codegen/字典无变更——字典项已存在）
- Owner Docs: `docs/design/finance/treasury.md`（§业财过账 + §关键业务规则 1 强一致校验，既有）、`docs/design/finance/posting.md`（`IErpFinAcctDocProvider` 机制 + 红冲机制方向二，既有）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表 + 套件结构，既有）。
- Skill Selection Basis: finance service 层 BizModel 新方法（手写 Java 非 `_gen/`）+ 跨实体访问（facility → orgId/currencyId 解析）+ 业财过账（IErpFinVoucherBiz.post + AcctDocProvider 注册 + VoucherBuilder 范式）+ 错误处理（NopException + ErrorCode）→ **必须加载 `nop-backend-dev`**（决策门 / xbiz 动作声明 / 跨实体访问自检 / 异常处理规范 / 产品化可定制性自检）；JUnit 测试经 `JunitAutoTestCase` harness → 加载 `nop-testing`；浏览器层 E2E 本体 `Skill: none`（Playwright 浏览器层非 `nop-testing` 后端快照范畴，对齐 2256-1 范式裁决）。需阅读 `../nop-entropy/docs-for-ai/04-reference/common-java-helpers.md`（`StringHelper`/`CoreMetrics`/`AppConfig`）+ `04-reference/safe-api-reference.md`（`IErpFinVoucherBiz.post` 安全 API 签名 + 幂等契约）。
- Protected Areas: finance service 层 BizModel + VoucherBuilder + AcctDocProvider 属应用层非会计保护区域（业务方法新增 + bean 注册加性 + config 常量加性，不动既有 GL/凭证/辅助账机制）；字典项已存在不动；config 键默认 0=关闭门控（向后兼容）；不改 ORM/契约/`_gen/`；任何 finance 生产缺陷须 ask-first / 开 successor。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移。
- 复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures）。
- **种子 COA 经实测已就绪**：`app-erp-all/src/main/resources/_vfs/_init-data/erp_md_subject.csv` 含 `6603`（财务费用-利息支出，1430-1 补齐）+ `1002`（银行存款，1234-1 基础科目）—— **无需新科目补齐**（区别于 2256-2 补 2502 维修中转清算）。
- **配置键**：`erp-fin.credit-facility-default-interest-rate`（BigDecimal，默认 0=关闭门控；非 0 时作年化利率）。webServer JVM arg 须追加 `-Derp-fin.credit-facility-default-interest-rate=0.05`（5% 年化）使利息计提 mutation 可达（rate=0 时 accrueInterest 抛 `ERR_CREDIT_FACILITY_INTEREST_RATE_NOT_CONFIGURED`）。
- **回滚策略**：全部改动为应用层 Java + bean 注册 + config 常量 + ErrorCode + 测试 + 文档，git 可逆；config 默认 0=关闭门控保持现有行为（仅新增 mutation 入口，不动既有 reserve/release 路径）；自包含 setup（测试专用 facility 隔离）+ finally cleanup（凭证+facility 逐域删，不污染 finance 看板/报表数值断言基线）。

## Execution Plan

### Phase 1 - 后端 accrueInterest + VoucherBuilder + Provider + bean 注册 + config 常量

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinCreditFacilityBizModel.java`（新增 `accrueInterest` `@BizMutation`）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/treasury/CreditFacilityInterestVoucherBuilder.java`（新建，对齐 `bankrecon/BankReconAdjustmentVoucherBuilder` 包路径范式）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/CreditFacilityInterestAcctDocProvider.java`（新建）、`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（追加 `<bean>` 注册）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinConstants.java`（新增 CONFIG 常量）、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/ErpFinErrors.java`（新增 ErrorCode）
Skill: `nop-backend-dev`

- Item Types: `Decision | Add`
- Prereqs: 无

- [x] `Decision | Explore`: 裁决 3 项关键实现选择（须在编码前定）：
  - **(a) 计息基数选择**：`usedAmount`（当前已用额度，对齐「实际占用资金计息」会计语义）vs `totalAmount`（总额度，对齐「承诺费」语义）。**裁决：`usedAmount`**——会计实质是「占用资金成本」非「承诺费」，且与 reserve/release 同步重算链一致。承诺费（未用部分）归 successor。
  - **(b) 计息天数基准**：`fromDate` ~ `toDate` 闭区间天数（days = ChronoUnit.DAYS.between(from, to) + 1）vs 开区间（days = between(from, to)）。**裁决：闭区间**（对齐 accrual accounting「含起止日」惯例，如 8 月计息 31 天非 30 天）。年化基准用 360 天（对齐 `ErpFinNotesReceivableProcessor.java:231-234` 贴现 `discountInterest = 票面 × 贴现率 × 剩余天数 / 360` 范式）。
  - **(c) `usedAmount` 取数时点**：开始时点（fromDate 当时的 usedAmount）vs 区间结束时的 usedAmount vs 区间日均。**裁决：开始时点（fromDate 当时的 usedAmount）**——保守口径，避免区间内 reserve/release 波动引入复杂日均计算（日均归 successor）；若区间内有 reserve/release 变更，本期利息按开始时点计，下期计提自动反映新 usedAmount。
  - **幂等键约束（非 Decision，已知约束）**：`IErpFinVoucherBiz.post` 经 `ErpFinPostingProcessor.alreadyPosted` 按 `(billHeadCode, businessType)` 反查实现幂等（实测 `IErpFinVoucherBiz.java:22-32` + `ErpFinPostingProcessor.java:123-127,462-474`）。billHeadCode 格式 `CFI-INT-{facilityId}-{fromDate}_{toDate}` 使同 facility + 同区间二次调用返回 null（无第二张凭证）。**这是平台契约非可选设计**——本计划 billHeadCode 格式即定义幂等粒度（区间级），分期间累计/按月计提的幂等键变体归 successor。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpFinCreditFacilityBizModel.accrueInterest(@Name("creditFacilityId") Long, @Name("fromDate") LocalDate, @Name("toDate") LocalDate, IServiceContext)` `@BizMutation` 返回 `Long`（生成的 voucherId；幂等命中返回 null）：
  - 守卫：facility 存在（`requireFacility` 复用既有 helper）；fromDate ≤ toDate（否则 `ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE`）；`usedAmount > 0`（=0 跳过，返回 null，不抛错——干净空操作）。
  - 读 rate：`BigDecimal rate = AppConfig.var(ErpFinConstants.CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE, BigDecimal.ZERO)`；`rate > 0`（=0 抛 `ERR_CREDIT_FACILITY_INTEREST_RATE_NOT_CONFIGURED`）。
  - 计息：`days = ChronoUnit.DAYS.between(fromDate, toDate) + 1`；`interest = usedAmount × rate × days / 360`（HALF_UP，scale=4 对齐 `amount` domain precision=20 scale=4）。
  - 委派 `CreditFacilityInterestVoucherBuilder.post(facility, fromDate, toDate, interest, context)` 返回 voucherId（可能 null=幂等命中）。
  - 返回 voucherId（含 null 情况，让调用方区分新建 vs 幂等）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 新建 `CreditFacilityInterestVoucherBuilder`（注入 `IDaoProvider` + `IErpFinVoucherBiz`，对齐 `BankReconAdjustmentVoucherBuilder` 包注入范式）：`Long post(ErpFinCreditFacility facility, LocalDate fromDate, LocalDate toDate, BigDecimal interestAmount, IServiceContext context)`：
  - 构造 PostingEvent：businessType=CREDIT_FACILITY_INTEREST；billHeadCode=`"CFI-INT-" + facility.getId() + "-" + fromDate + "_" + toDate`；orgId=facility.getOrgId()；acctSchemaId=`AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, facility.getOrgId())`（**不**经不存在的 `IErpFinAcctSchemaBiz`）；currencyId=facility.getCurrencyId()（或账套本位币兜底）；exchangeRate=BigDecimal.ONE；voucherDate=toDate（计息区间末日）。
  - billData 携带：`TOTAL=interestAmount`（Provider 读此键，对齐 `EmployeeAdvanceAcctDocProvider.readDecimal(event, "TOTAL")` 既有范式）+ `FUND_ACCOUNT_ID=facility.fundAccountId`（仅信息性，Provider 不强读）。
  - 调 `voucherBiz.post(event, context)` 返回 voucherId（可能 null）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 新建 `CreditFacilityInterestAcctDocProvider implements IErpFinAcctDocProvider`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/provider/CreditFacilityInterestAcctDocProvider.java`）：
  - `getSupportedBusinessTypes() = EnumSet.of(CREDIT_FACILITY_INTEREST)`。
  - `createFacts(event, ctx)`：读 `TOTAL` 经既有 `readDecimal` helper（镜像 `EmployeeAdvanceAcctDocProvider:75-84` 范式）；返回 2 行 VoucherFact：Dr `6603` 财务费用-利息支出（amount=interest）+ Cr `1002` 银行存款（amount=interest，partnerId=null——Dr/Cr 均不设 partnerId）。
  - 镜像 `NotesPayableAcctDocProvider.createFacts` 范式（fact helper / readDecimal）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 显式 bean 注册——`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml` 追加（l.99-125 既有 Provider 声明段尾）：
  ```xml
  <bean id="app.erp.fin.service.posting.provider.CreditFacilityInterestAcctDocProvider"
        class="app.erp.fin.service.posting.provider.CreditFacilityInterestAcctDocProvider"/>
  ```
  （`<ioc:collect-beans by-type="...IErpFinAcctDocProvider" only-concrete-classes="true">` 经此显式声明收集，对齐 `NotesPayableAcctDocProvider` l.124-125 范式；**非** component scan）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpFinConstants` 新增常量（treasury config 块，l.223+ 起）：`String CONFIG_CREDIT_FACILITY_DEFAULT_INTEREST_RATE = "erp-fin.credit-facility-default-interest-rate";`（对齐 `CONFIG_AUTO_RECONCILE`/`CONFIG_BAD_DEBT_*` 既有范式，**不**新增 `ErpFinConfigs` reader 方法——该接口为空，范式为 BizModel 内 `AppConfig.var(...)` 内联读取）。
  - Skill: `nop-backend-dev`
- [x] `Add`: `ErpFinErrors` 新增 ErrorCode：`ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE`（fromDate > toDate）+ `ERR_CREDIT_FACILITY_INTEREST_RATE_NOT_CONFIGURED`（rate=0）。描述用中文（i18n 处理翻译），对齐 `ERR_CREDIT_FACILITY_INSUFFICIENT` 既有范式（l.226-231）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `accrueInterest` 方法签名稳定（Long creditFacilityId, LocalDate fromDate, LocalDate toDate → Long voucherId 含 null 幂等）；本地化 `mvn compile -pl module-finance/erp-fin-service -am` 通过（解除 Phase 2/3 测试与 E2E 阻塞）。
- [x] `CreditFacilityInterestAcctDocProvider` 经 `<bean>` 声明被 `<ioc:collect-beans>` 收集到 `ErpFinAcctDocRegistry`（实测 `ErpFinAcctDocRegistry.getProvider(CREDIT_FACILITY_INTEREST)` 返回非 null）。

---

### Phase 2 - JUnit 后端单元/集成测试

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/treasury/TestErpFinCreditFacilityInterest.java`（新建，包路径对齐 VoucherBuilder）
Skill: `nop-testing`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] `Decision`: 测试 harness 选择——`JunitAutoTestCase` + `@NopTestConfig(localDb=true, initDatabaseSchema=TRUE)`（经真实 ORM/DB 走完 `IErpFinVoucherBiz.post` 链路端到端，对齐 `TestErpFinBankReconciliation`/`TestErpFinBadDebt` 范式）。**不**用纯 POJO unit（AcctDocProvider/VoucherBuilder createFacts/PostingEvent 构造可单测，但 BizModel 端到端凭证生成须真实 DB 验证 voucher 行持久化 + 幂等命中）。
  - Skill: `nop-testing`
- [x] `Add`: `TestErpFinCreditFacilityInterest`：
  - **测试 1（正路径计息 + 凭证生成）**：setup facility(total=1000, used=300, available=700) + `@NopTestConfig` config `erp-fin.credit-facility-default-interest-rate=0.05` → `accrueInterest(facilityId, 2026-08-01, 2026-08-31)` → 断言返回 voucherId 非空 + 经 `IErpFinVoucherBiz.findById` 反查凭证 `postingType=NORMAL`（注：`voucherType` 是 RECEIPT/PAYMENT/TRANSFER 字典，NORMAL 是 `postingType` 字典值）+ businessType=CREDIT_FACILITY_INTEREST + billHeadCode 格式 + 经 `IErpFinVoucherLineBiz` 反查凭证行 Dr 6603 = 300 × 0.05 × 31 / 360 = 1.2917（HALF_UP scale=4）+ Cr 1002 = 同额。
  - **测试 2（幂等命中）**：同 setup → 二次 `accrueInterest(facilityId, 2026-08-01, 2026-08-31)` → 断言返回 null + 经反查无第二张凭证生成（`ErpFinVoucherBillR` 仅 1 行 (billCode, businessType) 组合）+ facility 字段不变。
  - **测试 3（usedAmount=0 空操作）**：setup facility(used=0) → accrueInterest → 返回 null + 无凭证生成（facility.usedAmount=0 业务前置不满足）。
  - **测试 4（rate=0 抛守卫）**：不设 webServer JVM arg / config 默认 0 → accrueInterest 抛 `ERR_CREDIT_FACILITY_INTEREST_RATE_NOT_CONFIGURED` + 无凭证生成。
  - **测试 5（非法日期区间守卫）**：fromDate=2026-08-31, toDate=2026-08-01 → 抛 `ERR_CREDIT_FACILITY_INTEREST_INVALID_DATE_RANGE` + 无凭证生成。
  - **测试 6（provider 单元测试）**：直接 `new CreditFacilityInterestAcctDocProvider().createFacts(event, ctx)` 单测 facts 列表结构（Dr 6603 / Cr 1002 + amount 来自 billData.TOTAL），覆盖 businessType 不在 supportedTypes 时返回空列表。
  - Skill: `nop-testing`
- [x] `Proof`: `mvn test -pl module-finance/erp-fin-service -am` 全绿（既有 finance-service tests + 6 新增 0 failures/0 errors）+ 既有 finance 既有 spec 零回归。
  - Skill: `nop-testing`

Exit Criteria:

- [x] 6 测试用例全绿（正路径 + 幂等命中 + 3 守卫 + provider 单元）；凭证行精确数值断言对齐 Phase 1 Decision（usedAmount × rate × days/360）。
- [x] 既有 `fin-credit-facility.action.spec.ts`（2256-1 落地 reserve/release）+ finance 抽样回归零回归。

---

### Phase 3 - 浏览器层 E2E spec + 文档对齐 + 日志

Status: completed
Targets: `tests/e2e/business-actions/fin-credit-facility-interest.action.spec.ts`（新建）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表 + 套件计数）、`docs/plans/2026-07-17-2256-1-fin-treasury-cash-forecast-credit-facility-e2e.md`（Deferred RELEASED）、`docs/design/finance/treasury.md`（§业财过账 实现注记）、`docs/logs/2026/07-18.md`、`docs/backlog/README.md`、`playwright.config.ts`（webServer JVM arg 追加）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `Add`: `playwright.config.ts` webServer JVM args 追加 `-Derp-fin.credit-facility-default-interest-rate=0.05`（5% 年化使 mutation 可达；对齐既有 webServer config-gated 范式如 `erp-fin.auto-reconcile=true`）。
  - Skill: none
- [x] `Add`: `fin-credit-facility-interest.action.spec.ts` —— 自包含 setup（finally 兜底 cleanup）：
  - **测试 1（正路径计息 + 凭证行精确数值）**：经 `__save` 建 ErpFinCreditFacility(total=1000, used=300, available=700, orgId=2, currencyId=1)（直接置三值避免 reserveCredit 二次调用，对齐 2256-1 直 `__save` 范式）→ `accrueInterest(facilityId, "2026-08-01", "2026-08-31")` → GraphQL 返回 voucherId（String scalar，对齐 fin-voucher-post `gql.raw` 长标量返回范式）→ 经 `findVoucherIdByBillCode("CFI-INT-{id}-2026-08-01_2026-08-31", "NORMAL")` 反查（或直接用返回 voucherId）→ `assertVoucherLines` Dr 6603 = 1.2917 / Cr 1002 = 1.2917。
  - **测试 2（usedAmount=0 空操作）**：setup facility(used=0) → accrueInterest → GraphQL data null + errors null（返回 null 标量）→ 经 findFirst 反查无 `CFI-INT-{id}-` 凭证生成。
  - **测试 3（非法日期区间守卫）**：fromDate=2026-08-31, toDate=2026-08-01 → GraphQL errors 含中文 message token（对齐 2256-1 fin-credit-facility 不足守卫范式，i18n 描述含「日期」语义 token）+ 无凭证生成。
  - **清理**：finally 删 voucher（经 cleanupVoucherByBillCode 删 `CFI-INT-{id}-*` 凭证）+ facility。
  - Skill: none
- [x] `Proof`: spec 独立运行全绿（`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/business-actions/fin-credit-facility-interest.action.spec.ts --workers=1` 3 passed）+ finance 既有 spec 抽样回归（fin-credit-facility + fin-cash-forecast + fin-notes-* + fin-reconciliation 抽样）0 新增失败。
  - Skill: none
- [x] `Add`: 文档对齐——`e2e-runbook` 业务动作表 +1 finance 利息计提行 + 凭证行断言表 +1 行（CREDIT_FACILITY_INTEREST Dr 6603 / Cr 1002 公式派生 `usedAmount × rate × days/360`）+ 套件计数对齐（实测后增）；`2026-07-17-2256-1` Deferred 段补 `**RELEASED by 2026-07-18-0718-1**` 行 + 实施摘要；`treasury.md §业财过账` 表格 CREDIT_FACILITY_INTEREST 行下补实现注记（rate 来源 config / 公式 / 360 天基准 / billHeadCode 区间级幂等机制内置）；`docs/logs/2026/07-18.md` 增聚合条目（背景 / Phase 1-3 / 验证状态 / 范围纪律）；`docs/backlog/README.md` +1 done 行。
  - Skill: none

Exit Criteria:

- [x] 1 spec 全绿（3 用例：正路径凭证行精确数值 + 空操作 + 守卫 token）；voucherId 返回与凭证行数值断言均经 `findVoucherIdByBillCode` + `assertVoucherLines` 独立反查（非仅 mutation 返回值）。
- [x] finally cleanup 保护共享 DB（finance 看板/报表数值断言基线无漂移，对齐 2256-1 自包含 facility 隔离范式）。
- [x] e2e-runbook 业务动作表 + 凭证行断言表 + 套件计数对齐；2256-1 Deferred RELEASED 登记落地；treasury.md 实现注记就位；日志 + backlog 条目在位。

## Draft Review Record

- Independent draft review iteration 1: needs-revision (`ses_08d9c4a92ffev89WG1pNruBb2Q`，general agent 新会话冷审计) — 4 BLOCKERS / 4 MAJORS / 5 MINORS。10+ load-bearing 事实主张经实时仓库核实，发现 4 处事实伪 + 4 处机制引用错误：B1 字典项 `value: CREDIT_FACILITY_INTEREST` 已存在于 `_vfs/dict/erp-fin/business-type.dict.yaml:104-106`（plan 误称缺失）；B2 字典路径/格式错（应为 `dict/erp-fin/business-type.dict.yaml` 非 `erp/fin/meta/finance/business-type.dict.json`）；B3 `IErpFinVoucherBiz.post` 经 `ErpFinPostingProcessor.alreadyPosted` 按 `(billHeadCode, businessType)` **内置幂等**（plan 误称「静默生成第二张凭证」）；B4 `ErpFinFundAccount` 实测无 `partnerId` 列（plan 误称经此反查派生）；M1 Provider 注册需显式 `<bean>` 声明（非 component scan）；M2 `ErpFinConfigs` 为空接口，范式为 `ErpFinConstants.CONFIG_*` + 内联 `AppConfig.var`；M3 `IErpFinAcctSchemaBiz` 不存在，应使用 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)`；M4 Decision (d) 基于错误前提须重写为约束声明。legitimately warranted PASS（2256-1 Deferred 真实存在）。
- 修订落地（iteration 1 → 2）：(1) B1 删除「字典加性追加」Phase 1 deliverable + 改 Current Baseline 明示字典项已存在；(2) B2 删除错误路径引用；(3) B3 重写 Decision (d) 为「幂等键约束（非 Decision，已知约束）」+ 调整 JUnit 测试 2 为「幂等命中返回 null」+ 调整 Phase 3 spec 不二次调用同参数；(4) B4 删除 partnerId 反查派生 + 明示 partnerId=null；(5) M1 新增「显式 bean 注册」Phase 1 deliverable；(6) M2 改 config 机制为 `ErpFinConstants.CONFIG_*` 常量 + BizModel 内联 `AppConfig.var`，删除 `ErpFinConfigs` reader；(7) M3 改 acctSchema 解析为 `AcctSchemaResolver.resolvePrimarySchemaId`；(8) M4 Decision 重组（4 项 Decision 改为 3 项 Decision + 1 项约束声明）；(9) 采纳 m1 提取独立 `CreditFacilityInterestVoucherBuilder`（镜像 `BankReconAdjustmentVoucherBuilder` 范式，非 BizModel 直调 PostingEvent 构造）；(10) m4 修正 1430-1 → 0413-2 范式引用；(11) m2/m3/m5 文档订正。
- Independent draft review iteration 2: **accept** (`ses_08d93a2bdffeHwrtvvBIDxIQaT`，general agent 新会话冷审计) — 0 BLOCKERS / 0 MAJORS / 2 non-blocking MINORS。iter-1 全部 4 BLOCKERS + 4 MAJORS + 5 MINORS 经实时仓库核实 FIXED：B1（字典项 `value: CREDIT_FACILITY_INTEREST` live 实测 `business-type.dict.yaml:105` 存在 ✓ + Phase 1 dict-add deliverable 已删除 ✓）/ B2（YAML 路径订正 ✓）/ B3（Decision (d) 重写为幂等键约束 + JUnit Test 2 断言 null 返回 + Deferred 重分类 watch-only ✓ + live 实测 `IErpFinVoucherBiz.java:27` + `ErpFinPostingProcessor.alreadyPosted:462-474` 幂等机制 ✓）/ B4（partnerId=null + live 实测 `ErpFinFundAccount` 19 列无 partnerId ✓）/ M1（`<bean>` 注册 deliverable ✓ + live 实测 `<ioc:collect-beans by-type>` l.14-16 + 既有 Provider 显式 bean 声明 ✓）/ M2（`ErpFinConstants.CONFIG_*` + inline `AppConfig.var` ✓ + live 实测 `ErpFinConfigs.java` 空 interface）/ M3（`AcctSchemaResolver.resolvePrimarySchemaId` ✓ + live 实测 5 处使用点）/ M4（3 Decision + 1 约束声明 ✓）。m1-m5 cross-ref 全部订正。采纳 2 非阻塞 minor 微调落地：(n1) JUnit Test 1 `voucherType=NORMAL` 改为 `postingType=NORMAL`（`voucherType` 是 RECEIPT/PAYMENT/TRANSFER 字典；NORMAL 是 `postingType` 字典值——live `posting-type.dict.yaml` 实测）；(n2) `/360` 范式 attribution 由 `NotesReceivableAcctDocProvider` 改为 `ErpFinNotesReceivableProcessor.java:231-234`（live 实测 360 天惯例实际位置）。R1-R13 + anti-slack + scope 全 PASS。共识达成 → `Plan Status: active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划触及 finance service 层生产代码（1 BizModel 新方法 + 1 新 VoucherBuilder 类 + 1 新 Provider 类 + bean 注册 + config 常量 + 2 ErrorCode）+ 测试 + 文档；Closure Gates 须含完整 `mvn` 验证 + Playwright 全套件回归。

- [x] 范围内行为完成（accrueInterest @BizMutation + VoucherBuilder + Provider + bean 注册 + config 常量 + ErrorCode + JUnit 6 用例 + 1 spec 3 用例）
- [x] 相关文档对齐（e2e-runbook + 2256-1 Deferred RELEASED + treasury.md 实现注记 + 当日日志 + backlog）
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service -am` 全绿（含新增 6 tests）+ 新增 spec 独立运行全绿 + finance 既有 spec 抽样回归 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（closure gate，确认零后端污染）
- [x] 无范围内项目降级为 deferred/follow-up（facility 级利率 / 累计字段 / nop-job 定时 / 多 facility 批量 / 外币利息汇兑 / 利息资本化 / 幂等键变体均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Facility 级利率覆盖（per-facility interestRate ORM 列）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同 facility 不同利率属合理业务需求（信用等级差异），但落地需 ORM 加列（保护区域）+ UI 维护面 + 配置同步。本计划用 config 默认利率（全 facility 共享）满足核心计提；per-facility 覆盖属不同结果面。
- Successor Required: `yes`（触发条件：不同 facility 不同利率的业务需求落地时）

### 累计计息字段物化（facility.lastAccrualDate / accruedInterestAmount）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 利息累计查询可经 `ErpFinVoucherBillR` 反查（billHeadCode 含 facilityId）实现，无业务阻断。物化字段优化查询性能但增加一致性维护面（红冲须同步回退）。
- Successor Required: `yes`（触发条件：facility 维度累计利息高频查询性能问题落地时）

### nop-job 定时自动计提

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: cron 触发非浏览器面 mutation 入口（对齐 `ErpFinCashForecastJob` 范式由后端单测覆盖）。本计划仅手动 `accrueInterest` mutation。
- Successor Required: `yes`（触发条件：产品要求按月自动计提时，需新建 `ErpFinCreditFacilityInterestJob` + scheduler.yaml 接线）

### 多 facility 批量计提入口

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 单 facility 计提已覆盖核心业务动作；批量入口（`accrueAllFacilities(fromDate, toDate)` 迭代所有 ACTIVE facility）属运营自动化面。
- Successor Required: `yes`（触发条件：批量定期计提业务需求落地时）

### 外币授信利息汇兑损益

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本位币计息已覆盖核心；外币利息的汇兑损益分解（Dr/Cr 银行存款本位币 × 期末汇率 vs Dr/Cr 财务费用-汇兑）属不同结果面。
- Successor Required: `yes`（触发条件：外币授信业务落地时）

### 利息资本化（购建固定资产期间利息资本化）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 会计准则允许符合条件的借款利息资本化到资产成本（如购建固定资产期间）。本计划利息全部费用化（Dr 财务费用）；资本化路径属不同结果面 successor。
- Successor Required: `yes`（触发条件：利息资本化业务需求落地时）

### 幂等键组成变体（区间级 vs 月度级）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 默认 billHeadCode = `CFI-INT-{facilityId}-{fromDate}_{toDate}`（区间级幂等键）经 `IErpFinVoucherBiz.post` 内置 `alreadyPosted` 守护（同 facility + 同区间二次调用返回 null 无第二张凭证，已实现）。如财务制度要求按月固定计提（fromDate/toDate 固定为月初/月末 vs 任意区间），billHeadCode 格式须调整为 `{facilityId}-{year}-{month}` 月度级；属配置扩展面非缺陷。
- Successor Required: `yes`（触发条件：财务制度要求按月固定计提 vs 任意区间时）

## Closure

Status Note: 执行完成（2026-07-18）。3 Phase 全绿——Phase 1（后端 accrueInterest @BizMutation + CreditFacilityInterestVoucherBuilder + CreditFacilityInterestAcctDocProvider + bean 注册 + config 常量 + 2 ErrorCode + IErpFinCreditFacilityBiz 接口声明；3 项 Decision 已裁决：计息基数=usedAmount / 闭区间天数 / fromDate 时点取数；执行期修复 1 处 latent defect：VoucherBuilder currencyId 兜底 facility.fundAccount.currencyId → acctSchema.functionalCurrencyId，对齐 plan「账套本位币兜底」原裁决）/ Phase 2（JUnit 6 用例全绿：正路径凭证行精确数值 Dr=Cr=1.2917 + 幂等命中返回 null + usedAmount=0 空操作 + rate=0 守卫经 AppConfig.getConfigProvider().assignConfigValue 临时覆盖 + 非法日期守卫 + Provider 单元；RECORDING→CHECKING 切换）/ Phase 3（1 新 spec 3 用例全绿：正路径 findVoucherIdByBillCode 反查 + assertVoucherLines Dr 6603=1.2917 / Cr 1002=1.2917 + facility 三值不变 + usedAmount=0 返回 null + fromDate>toDate「日期」语义 token 守卫；测试区间用 2026-07 而非 plan 原写 2026-08 因种子仅含 2026-07 OPEN 期间；playwright.config.ts webServer JVM args 追加 `-Derp-fin.credit-facility-default-interest-rate=0.05`；e2e-runbook 业务动作表 +1 行 + 凭证行断言表 +1 行 + 套件计数 73→74；2256-1 Deferred RELEASED；treasury.md §业财过账补实现注记；当日日志 + backlog）。验证：JUnit 6/6 + finance-service 全量 200 tests 0 failures/0 errors + 新 spec 3/3 + finance 既有 5 spec 抽样回归 25/25 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（02:46 min，零后端污染确认）。结束审计待独立子代理执行。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（opencode 新会话，不重用执行者上下文），audit task 通过 mission-driver AUDIT step 触发；冷重播自检针对计划、受影响 owner docs（treasury.md / e2e-runbook / 2256-1）、实时差异和真实验证命令执行。
- Live repo verification（审计会话内 `grep`/`read`/`mvn`/`playwright --list` 实测，非采信 [x] 标记或 plan 内部叙述）：
  - Phase 1 后端代码全部落地且非空壳：`ErpFinCreditFacilityBizModel.java:81-111` `accrueInterest` `@BizMutation` 实测守卫 fromDate/toDate/usedAmount/rate + 计息 `usedAmount × rate × days / 360`（HALF_UP scale=4）+ 委派 `interestVoucherBuilder.post(...)` 返回 voucherId（**非 `return null` 占位符**，line 110 实际调用）；`CreditFacilityInterestVoucherBuilder.java:58-81` 实测构造 PostingEvent（businessType + billHeadCode=`CFI-INT-{id}-{from}_{to}` + orgId/currencyId/acctSchemaId/exchangeRate/voucherDate + billData 携带 `TOTAL`/`FUND_ACCOUNT_ID`）+ 实调 `voucherBiz.post(event, context)`（**非吞异常**，line 80 直接 return）；`CreditFacilityInterestAcctDocProvider.java:39-54` 实测 `getSupportedBusinessTypes()=EnumSet.of(CREDIT_FACILITY_INTEREST)` + `createFacts` 返回 Dr 6603 / Cr 1002 两行 VoucherFact（amount 来自 billData.TOTAL，partnerId=null）；`app-service.beans.xml:129-134` 实测显式 `<bean>` 注册（Provider + VoucherBuilder 各 1 条，`<ioc:collect-beans by-type>` 收集）；`ErpFinConstants.java:229` + `ErpFinErrors.java:235,239` + `IErpFinCreditFacilityBiz.java:54` 实测落地。
  - Phase 2 JUnit 全绿：审计会话内 `mvn test -pl module-finance/erp-fin-service -am -Dtest=TestErpFinCreditFacilityInterest -Dsurefire.failIfNoSpecifiedTests=false` 实测 `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 3.872 s` + `BUILD SUCCESS`（正路径 + 幂等命中返回 null + usedAmount=0 空操作 + rate=0 守卫 + 非法日期守卫 + Provider 单元 6 用例非空断言）；测试体非空壳，对 voucher 行精确数值 `1.2917`（300×0.05×31/360）+ billHeadCode 区间级幂等键格式 + facility 三值不变均有独立反查断言。
  - Phase 3 浏览器层 spec 在位且语法可加载：审计会话内 `npx playwright test --list tests/e2e/business-actions/fin-credit-facility-interest.action.spec.ts` 实测 3 tests in 1 file（正路径凭证行精确数值 + usedAmount=0 空操作 + fromDate>toDate 守卫 token），文件 TS 类型检查针对 spec 本体零专属错误（既有 playwright-core types 与 spec 无关）；`playwright.config.ts:18` webServer JVM arg 实测含 `-Derp-fin.credit-facility-default-interest-rate=0.05`。
  - 文档对齐实测：`docs/testing/e2e-runbook.md:294,399,405`（业务动作表 +1 finance 行 + 凭证行断言表 +1 行 + 套件计数 73→74）；`docs/plans/2026-07-17-2256-1-...md:191` Deferred 段补 `**RELEASED by 2026-07-18-0718-1**` + 实施摘要；`docs/design/finance/treasury.md:150-153` §业财过账 CREDIT_FACILITY_INTEREST 行下补实现注记（触发 / rate 来源 / 公式 / 幂等机制 / 币种解析顺序）；`docs/logs/2026/07-18.md` 聚合条目（背景 / Phase 1-3 / 验证状态 / 范围纪律）；`docs/backlog/README.md:88` +1 done 行 + 范围纪律注。
- 反空壳 / 反吞异常：BizModel → VoucherBuilder → voucherBiz.post → AcctDocRegistry → Provider createFacts 链路全程可达（无 `return null` 占位 / 无空 `{}` 方法体 / 无 catch 静默 / 无注册但不可达组件），JUnit 端到端实测验证凭证行持久化。
- 反松弛 / Deferred honesty：6 项 Non-Goal 全部附触发条件（facility 级利率 / 累计字段 / nop-job 定时 / 多 facility 批量 / 外币利息汇兑 / 利息资本化 / 幂等键变体）；无范围内缺陷降级为 follow-up；Closure Gates 第 4 项「无范围内项目降级为 deferred/follow-up」语义对齐。
- Five-point consistency：`Plan Status: completed` ↔ Phase 1/2/3 全 `Status: completed` ↔ 所有 Phase Exit Criteria `[x]` ↔ Closure Gates 全 `[x]` ↔ Closure evidence 落地，五处一致。
- 残留风险：无范围内阻塞残留；执行期修复的 1 处 latent defect（VoucherBuilder currencyId 兜底 facility.fundAccount.currencyId → acctSchema.functionalCurrencyId）已正确落地（`CreditFacilityInterestVoucherBuilder.java:92-108`）并在当日日志记录。

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
