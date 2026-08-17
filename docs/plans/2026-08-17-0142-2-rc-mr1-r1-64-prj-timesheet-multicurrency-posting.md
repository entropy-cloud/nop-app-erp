# 2026-08-17-0142-2-rc-mr1-r1-64-prj-timesheet-multicurrency-posting RC-R1.64 — projects 工时过账多币种（P1-MA1-010 reuse 重开：TimesheetPostingDispatcher.buildEvent 汇率解析替代 setExchangeRate(ONE)）

> Plan Status: completed
> Last Reviewed: 2026-08-17
> Mission: requirement-compliance
> Work Item: RC-R1.64（P1-MA1-010 reuse 重开，A4.2.116 多币种工时过账折算失真）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.64 行（:456）+ `docs/audits/arm-index.md` P1-MA1-010 行（:503）+ A4.2.116 运行时确认（`docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md` :77-90）
> Related: `docs/design/projects/use-cases.md`（L1 UC-PRJ-02 :38 / UC-PRJ-06 :108 多币种折算到统一币种）；`docs/design/projects/cost-collection.md`（§2 工时成本计算 + §2.4 凭证示例 + §六 跨域协作）；`docs/design/finance/posting.md`（汇率语义 + R1.42 守卫）
> Audit: required

## Current Baseline

- **finding P1-MA1-010（arm-index:503，audit-remediation MA1 A 级）**：projects 多币种四件套 propId 缺失——**ORM 元数据层已 fixed**（2026-07-29-2005-1 Phase 1 propId 重编号为连续序列）。**R1.0 展开归属 RC-R1.64**：本项目复用重开的是**业务逻辑层缺口**——A4.2.116 投影确认的 `TimesheetPostingDispatcher.buildEvent` 汇率硬编码（同根因多币种维度，ORM 载体已就绪）。
- **A4.2.116 运行时确认（2026-08-07，维持 P1-MA1-010 投影）**（live code 实测）：
  - `TimesheetPostingDispatcher.buildEvent:130`（`module-projects/erp-prj-service/.../posting/TimesheetPostingDispatcher.java`）：**`event.setExchangeRate(BigDecimal.ONE)` 硬编码**——不解析 timesheet.currencyId 与项目本位币（或法人本位账套）的汇率。
  - `:129` `event.setCurrencyId(timesheet.getCurrencyId())`——currencyId 传入但 exchangeRate 固定 ONE；`:128` `event.setAcctSchemaId(resolveAcctSchemaId(timesheet.getOrgId()))`——经 `AcctSchemaResolver.resolvePrimarySchemaId` 按法人解析本位账套，但汇率仍 ONE。
  - **`ProjectCostCollectionProvider.createFacts:66-72`（`.../posting/ProjectCostCollectionProvider.java`）只设 `fact.setAmount(amount)`（:94）——不设 amountSource/amountFunctional**：finance 引擎 `ErpFinPostingProcessor.persistVoucher:862-864` 未设置时回退 `source==functional==amount`（P1-MA3-039 方案 A 语义，posting.md:450-452），GL 借贷按 amountFunctional 记账（:874-875）→ **仅 buildEvent 传真实 rate 仍不折算**，须 Provider 显式填双字段（镜像 `PurAcctDocProvider:129-131`：`setAmount(functional)` + `setAmountSource(sourceAmount)` + `setAmountFunctional(functional)`——**`amount` 保持本位币（功能金额）语义**，posting.md 实现契约 + `VoucherFact` javadoc 要求 `assertBalanced`/试算平衡以 `fact.getAmount()` 为准）。
  - **`tryPost` 结构**：`TimesheetPostingDispatcher.tryPost:60-74`——`buildEvent(timesheet)` 在 :61 **try/catch 之外**（try :62 起）。**buildEvent 抛错（含汇率缺失）→ 传播出 tryPost → `ErpPrjTimesheetApproveProcessor.approve:49` → @BizMutation 事务回滚 → 单据保持 SUBMITTED（无告警派发）**——与既有 buildEvent 校验错误先例一致（`ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED`/`ERR_PAYROLL_SUBJECT_NOT_CONFIGURED` :113-122 同样中止审批）；过账引擎内部抛错（executor.postEvent 在 try 内）才走告警+posted=false 分支。**汇率缺失错误路径语义在 D1 显式裁决**（见 Phase 1）。
  - **erp-prj-service 主代码 5 处 `setExchangeRate(BigDecimal.ONE)`**（grep 实证）：`TimesheetPostingDispatcher:130`（**voucher 面，本行核心**）+ `ProjectCostAggregator:82`/`MaterialCostAggregator:73`/`ExpenseCostAggregator:134`（**归集头 metadata 面**——LABOR/物料/费用归集头 exchangeRate，消费侧=PnL 聚合/辅助核算，非 voucher 路径）+ `ProjectPnlCalculator:105`（PnL 快照，P2-RC-050）。归集头 metadata 面与 P2-RC-050 协同归 successor（见 Deferred But Adjudicated），本行不覆盖。
  - 折算失真量化：USD 项目 + CNY 本位账套场景下工时过账凭证 amountFunctional 按 exchangeRate=ONE 折算 → **GL 借贷平衡不破坏仅折算失真，不触发 MR0**。
- **finance 侧守卫已落地（R1.42，plan 2026-08-15-1838-1）**：`ErpFinPostingProcessor.guardExchangeRate:554-568`——事件币种为非本位币（`ErpMdCurrency.isFunctional=false`）且未显式传 exchangeRate → 抛 `ERR_EXCHANGE_RATE_REQUIRED` 拒绝过账；本位币/currencyId=null/币种不存在 → 保留 rate=1 回退。**当前 buildEvent 恒传 ONE（非 null）→ 守卫对工时路径结构性失效**（永远走 rate=1 分支），本行修复后守卫语义才真正生效。
- **汇率数据载体**：`ErpMdExchangeRate`（`module-master-data/model/app-erp-master-data.orm.xml:722-759`，列 729-741 + IDX 索引 :751-758）——fromCurrencyId/toCurrencyId/rateType(SPOT)/rate/validFrom/validTo 字段齐备；`IErpMdExchangeRateBiz extends ICrudBiz<ErpMdExchangeRate>`（module-master-data/erp-md-dao）；跨域只读经 IBizObjectManager 按名解析范式已有先例（`ErpFinPostingProcessor.findCurrencyById:571-577`）。
- **错误码**：`ErpFinErrors.ERR_EXCHANGE_RATE_REQUIRED`（ErpFinErrors.java:498-500，erp.err.fin 前缀，含 {currencyCode} 文案）——erp-prj-service 编译依赖 finance-service（pom 既有，`AcctSchemaResolver`/`ErpFinBusinessType`/`PostingEvent` 均已 import 实证跨模块依赖成立）；projects 侧 `ErpPrjErrors` 为备选——归属裁决落在 Phase 1 D1。
- **2026-08-12 批量裁决（roadmap 头 :49 B 类清单）**：**RC-R1.64 在 B 类**——「buildEvent 汇率解析替代 BigDecimal.ONE」经核实不需要 ORM 结构变更，纯代码逻辑可解决，从"越界项 ask-first"**降级为预授权自动执行**。roadmap 行旧「越界项…双独立子 agent 批准 checkbox」字样按 B 类裁决执行期改写消除歧义（对齐 RC-R1.41/42/50/52-54/59/61/62 先例）。
- **测试基线**：erp-prj-service **158 tests 全绿**（R1.60 基线）；`TestErpPrjTimesheetCost` 7 组为既有工时过账测试范本（含 `TestErpPrjTimesheetCost` 单币种凭证断言）；**零多币种工时过账测试**（无汇率 seed + 无 amountFunctional 折算断言）。
- **compliance 基线**：R2b=235 / R2c=1433 / R2d=35（R1.60 登记后基线）；本计划预期经既有 daoProvider 站点/IBiz 注入实现 → 零漂移或 +N baseline-raise 登记（per-site 证据）。

## Goals

- **UC-PRJ-06 「多币种折算到统一币种」工时侧运行时成立（P1-MA1-010 业务逻辑层闭合）**：`TimesheetPostingDispatcher.buildEvent` 汇率解析替代 `BigDecimal.ONE` 硬编码——非本位币（`ErpMdCurrency.isFunctional=false`）经 `ErpMdExchangeRate` 按 currencyId + voucherDate 解析 rate；本位币/currencyId=null → 保留 rate=1 回退；非本位币汇率缺失 → 抛 `ERR_EXCHANGE_RATE_REQUIRED`（复用 R1.42 既有 ErrorCode，对齐 UC-FIN-12 断言②「汇率缺失 → 报错拒绝过账」语义，工时路径守卫不再结构性失效）。
- **凭证折算正确性（buildEvent + Provider 双点协同）**：`ProjectCostCollectionProvider.createFacts` 增 `setAmount(amountFunctional)` + `setAmountSource(amount)` + `setAmountFunctional(amount×ctx.exchangeRate)` 三字段（镜像 `PurAcctDocProvider:129-131` 范式，amount 保持本位币/功能金额语义）——工时过账凭证 amountFunctional = amountSource × rate（USD 项目 + CNY 本位账套场景折算失真消除）；GL 借贷平衡保持（引擎按 amountFunctional 记账，试算平衡以本位币为准）。
- **汇率缺失错误路径**：非本位币汇率缺失 → 抛 `ERR_EXCHANGE_RATE_REQUIRED` 拒绝过账（对齐 UC-FIN-12 断言② / R1.42 守卫语义）；错误路径按 Phase 1 D1(ii) 裁决（选项 α：buildEvent 抛错传播 → approve 事务回滚 → 单据保持 SUBMITTED，与既有 buildEvent 校验错误先例一致；选项 β 为备选）。
- **测试**：新增多币种工时过账测试组（汇率 seed + amountSource/amountFunctional 行级折算断言 + 缺失拒绝 + 本位币回退 + 零回归）；既有 158 tests 零回归。
- **零回归**：erp-prj-service 全量测试全绿 + 全量 `mvn test` + `mvn clean install -DskipTests` + compliance checker 零漂移或基线登记。
- **owner doc 收敛**：`cost-collection.md` §2.4/§六 补多币种工时过账实现注记；arm-index P1-MA1-010 行追加 RC-R1.64 闭合注记；roadmap RC-R1.64 → done ✅（行标签按 B 类裁决改写）+ 本日志条目。

## Non-Goals

- **不覆盖费用归集头/PnL 汇率路径**（`ExpenseCostAggregator:134` + `ProjectPnlCalculator:105` 属 P2-RC-050 watch-only successor，本行不实现；A4.2.120 维持 P2 分级不动；`ProjectCostAggregator:82`/`MaterialCostAggregator:73` 归集头 metadata 面同样归 successor，见 Deferred But Adjudicated）。
- **不触 ORM 结构变更**（B 类裁决：多币种四件套 propId 已 fixed；若 Phase 1 Explore 证实必须加列 → 暂停回落越界流程）。
- **不重写 finance 过账引擎汇率守卫**（`ErpFinPostingProcessor.guardExchangeRate` 语义不动，本行仅让 buildEvent 传入真实 rate + Provider 显式双字段使折算与守卫生效）。
- **不实现跨币种 Billing 侧折算**（收入侧 Billing 已由 finance 过账折算 amountFunctional——既有机制，非本行缺口）。
- **不改真相源契约段落**（use-cases L1 不动；cost-collection.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：纯 BizModel/Dispatcher 代码逻辑，2026-08-12 B 类裁决降级预授权；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/projects/use-cases.md`（L1 UC-PRJ-02/06）+ `docs/design/projects/cost-collection.md`（§2/§六）+ `docs/design/finance/posting.md`（汇率语义 + R1.42 守卫）
- Skill Selection Basis: 跨实体 Facade + 跨域只读（`nop-backend-dev`：IBiz 注入 + 决策门 + 错误码复用 + 事务边界）；测试（`nop-testing`：JunitBaseTestCase + 多币种 seed + 凭证行级断言范式对齐 R1.46 试算平衡/R1.42 汇率守卫先例）。

## Infrastructure And Config Prereqs

- 无新 config key（汇率解析经 `ErpMdExchangeRate` 数据载体，非 config 门控；若 Phase 1 裁决需汇率回退策略 config 再按 `erp-prj.*` 引入）。
- 无 ORM 变更（B 类裁决）。
- 跨域只读：master-data 已有 `IErpMdExchangeRateBiz`/`IErpMdCurrencyBiz` 接口（erp-md-dao）——需确认 erp-prj-service pom 依赖 erp-md-dao 既有（`AcctSchemaResolver` 已 import 自 `app.erp.md.dao` 实证跨域只读既有）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-projects/erp-prj-service`。

## Execution Plan

### Phase 1 - Explore + Decision（汇率解析载体）

Status: completed
Targets: `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/TimesheetPostingDispatcher.java`（只读核查）、`module-master-data/erp-md-dao/.../biz/IErpMdExchangeRateBiz.java`（只读核查）、`docs/design/finance/posting.md`（只读复核）
Skill: `nop-backend-dev`
Item Types: `Decision`
Prereqs: 无

- [x] Explore（只读）：① 复核 `TimesheetPostingDispatcher.buildEvent:124-150` 现状（exchangeRate=ONE 硬编码 + currencyId 传入 + 本位账套解析）；② `ProjectCostCollectionProvider.createFacts:49-75` + `fact():88-98` 现状（仅 setAmount，无 amountSource/amountFunctional）——确认双字段迁移落点与 `PurAcctDocProvider:130-131` 镜像范式；③ `ErpMdExchangeRate` 查询面——按 fromCurrencyId+toCurrencyId+validFrom<=date<=validTo 的查找范式（findFirstByQuery + 日期边界排序裁决：最近生效优先 vs 无边界匹配）；④ 跨域只读接入范式——IBizObjectManager 按名解析（对齐 `ErpFinPostingProcessor.findCurrencyById:571-577`）vs 直接注入 `IErpMdExchangeRateBiz`（pom 依赖核查）；⑤ 本位币判定——`IErpMdCurrencyBiz` isFunctional 查询（对齐 guardExchangeRate 语义）。
       - Skill: `nop-backend-dev`
- [x] Decision D1（汇率解析载体 + Provider 双字段 + 缺失错误路径语义）：A. `TimesheetPostingDispatcher` 内联解析——currencyId=null → rate=1；本位币 → rate=1；非本位币 → `ErpMdExchangeRate` 按 currencyId+本位币 toCurrency + voucherDate 解析，命中 → rate，未命中 → 抛 `ERR_EXCHANGE_RATE_REQUIRED`（复用 finance ErpFinErrors 或 projects 新 ErrorCode——裁决错误码归属）；B. 抽共享汇率解析 helper（cost 包，供后续 P2-RC-050 协同复用）——若选 B 记录复用触发条件。**D1 必须同时裁决**：(i) **Provider 双字段迁移范围**（`ProjectCostCollectionProvider.fact()` 增 `setAmount(amountFunctional)` + `setAmountSource(amount)` + `setAmountFunctional(amount×ctx.exchangeRate)`，镜像 PurAcctDocProvider 范式——amountFunctional 折算需 buildEvent 真实 rate + Provider 双字段两点协同，缺一不可）；(ii) **汇率缺失错误路径语义**——选项 α：buildEvent 抛错传播（tryPost 外 → approve 事务回滚 → 单据保持 **SUBMITTED**，无告警，与既有 buildEvent 校验错误先例 `ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED` 同型）；选项 β：把 buildEvent 移入 tryPost 的 try 块（→ dispatchFailureAlert + return false → APPROVED+posted=false）。裁决标准：与既有 buildEvent 校验错误语义一致（α）+ L1 UC-FIN-12「汇率缺失 → 报错拒绝过账」（α 命中）；残留风险（汇率缺失时审批被拒，操作员需补录汇率后重提——无悬挂，显式可恢复）。
   - Skill: `nop-backend-dev`

Exit Criteria:

- [x] D1 落地（理由 + 替代方案 + 残留风险）；汇率查询范式/本位币判定/错误码归属明确，解除 Phase 2 阻塞

#### D1 裁决记录（2026-08-17，实仓核查）

**Explore 实证**（live code）：
- ① `buildEvent:124-133` 现状核实：`setAcctSchemaId(resolveAcctSchemaId(orgId)):128` + `setCurrencyId:129` + `setExchangeRate(BigDecimal.ONE):130`；voucherDate=`workDate ?: today`:131-132。
- ② `ProjectCostCollectionProvider.fact():88-98` 仅 `setAmount(amount):94`，无 amountSource/amountFunctional；镜像范式 `PurAcctDocProvider:113-135`（`resolveRate(ctx)`=ctx.getExchangeRate()?:ONE + `fact()` functional=source×rate，setAmount(functional)+setAmountSource(source)+setAmountFunctional(functional)）。引擎 `persistVoucher:853-879`：ctx.exchangeRate→line.exchangeRate，fact 双字段未设时 fallback amount，GL debit/credit 按 amtFunctional。
- ③ `ErpMdExchangeRate` 字段齐备（fromCurrencyId/toCurrencyId/rateType[default SPOT]/rate/validFrom/validTo）；objMeta `fromCurrencyId/toCurrencyId/validFrom/validTo/rateType` 全部 `queryable=true`（IBiz findFirst 管道可用）；md 域内既有查询先例 `ErpMdCurrencyRefreshRatesFromApiProcessor.findExistingRate:99-108`（eq from/to + findAllByQuery）。
- ④ pom 核查：erp-prj-service 对 md-**service** 仅 test-scope（BizModel bean 测试注册），md-**dao** 经 finance-service 链 transitive compile（`AcctSchemaResolver`/`ErpMdSubject` 已 import 实证）——直接 `@Inject IErpMdExchangeRateBiz` 编译可行但单域测试 IoC 解析依赖 test-scope bean；`IBizObjectManager` 按名解析（`io.nop.biz.api`，nop-biz compile dep）为 finance 已验证先例（`findCurrencyById:571-577` + `resolveSubjects:646-648` 注释明示「finance→erp-md-service 仅 test 作用域，故非 BizModel 编排 bean 经 IBizObjectManager 按名解析」）。
- ⑤ 本位币判定：`ErpMdCurrency.isFunctional`（objMeta queryable=true）；owner doc `posting.md:445` R1.42 注记明示「本位币判定载体 = `ErpMdCurrency.isFunctional` 字段（`eq("isFunctional",TRUE)` 查询范式，`ErpMdAcctSchema.functionalCurrencyId` 作 schema 级细分兜底载体，归 successor）」。

**裁决 A：TimesheetPostingDispatcher 内联解析（Option A）+ IBizObjectManager 按名解析跨域只读（Option ④之 IBizObjectManager 分支）**：
- `@Inject IBizObjectManager bizObjectManager`（包级可见，镜像 `ErpFinPostingProcessor:90`）；`IErpMdCurrencyBiz`/`IErpMdExchangeRateBiz` 均经 `getBizObject(<Entity>.class.getSimpleName()).asProxy()` 解析——**零新增 daoFor 站点（R2c/R2d 零漂移）**，且与 R1.42 守卫同一解析范式（语义同源）。
- 三态：currencyId=null → rate=1（行为保持）；币种按 id 解析（findFirst eq("id")）——记录不存在 → rate=1 保守放行（镜像 guardExchangeRate D2 裁决语义，既有 158 测试不 seed ErpMdCurrency 零回归的前提）；`isFunctional=TRUE` → rate=1（本位币折算恒等式）。
- 非本位币折算目标币种 = `eq("isFunctional",TRUE)` findFirst 解析的本位币（owner doc :445 主载体；schema.functionalCurrencyId 细分归 successor）。本位币缺失（无非本位币可折算目标）与汇率行未命中同归「汇率缺失」→ 抛错。
- 汇率行查询：`eq(fromCurrencyId)+eq(toCurrencyId)` + `le(validFrom,voucherDate)+ge(validTo,voucherDate)` 边界匹配 + `addOrderField(validFrom,true)` 最近生效优先 + limit 1（**裁决：边界匹配 + 最近生效优先**，无边界匹配行不回退更早汇率——避免静默用错期汇率；rateType 不作过滤条件——ORM default SPOT 与 refresh API 写入 MIDDLE 并存，按类型过滤会漏另一类数据行，rateType 为信息性维度）。
- 替代方案 B（抽共享 helper 到 cost 包）否决：当前 voucher 面仅此一处消费；P2-RC-050 回队时按 Deferred But Adjudicated 触发条件抽取（复用触发条件已登记）。

**裁决 (i)：Provider 双字段迁移**——`ProjectCostCollectionProvider.createFacts` 增 `resolveRate(ctx)`（镜像 PurAcctDocProvider:114-116），`fact()` 增 rate 参数：`functional = sourceAmount.multiply(rate)`，`setAmount(functional)` + `setAmountSource(sourceAmount)` + `setAmountFunctional(functional)`；source=`BILL_DATA_COST_AMOUNT`（timesheet.costAmount，源币种）。单币种 rate=1 → source==functional==amount 三者相等（向后兼容，既有断言 `amountFunctional==8000.00` 不变）。expense 路径（`SOURCE_BILL_TYPE=EXPENSE`）同 Provider 双字段同享折算——`ErpMdCurrency.isFunctional=TRUE` 单币种主路径行为不变，多币种 EXPENSE 消费方向无既有断言（非回归面）。

**裁决 (ii)：选项 α（buildEvent 抛错传播 → 单据保持 SUBMITTED）**——buildEvent 在 tryPost:61 try 块外（实仓 :61-62 核实），抛 `ERR_EXCHANGE_RATE_REQUIRED` 传播出 tryPost → `ErpPrjTimesheetApproveProcessor.approve:49` → @BizMutation 事务回滚 → 单据保持 SUBMITTED（无告警派发），与既有 buildEvent 校验错误先例（`ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED`:113-116 / `ERR_PAYROLL_SUBJECT_NOT_CONFIGURED`:118-122 同型）。选项 β（移入 try 块 → APPROVED+posted=false+告警）否决：与同类配置缺失校验错误分级不一致。残留风险：汇率缺失时审批被拒，操作员补录 `ErpMdExchangeRate` 后重提 approve 显式可恢复（无悬挂）。

**错误码归属：复用 finance `ErpFinErrors.ERR_EXCHANGE_RATE_REQUIRED`**（`erp.err.fin.exchange-rate-required`，含 `{currencyCode}` 参数）——该错误语义由 R1.42 守卫定义且 owner doc（posting.md:444-445）归 finance「汇率缺失→报错拒绝过账」；工时侧抛同一码使跨域语义统一（调用方单码处理），erp-prj-service 已有 finance-service compile 依赖（`ErpFinBusinessType`/`PostingEvent` import 实证）；projects 侧新建 `ErpPrjErrors` 码会分裂同一语义错误面。

### Phase 2 - 工时过账汇率解析 + Provider 双字段实现（Add）

Status: completed
Targets: `module-projects/erp-prj-service/.../posting/TimesheetPostingDispatcher.java`（buildEvent 改造）、`module-projects/erp-prj-service/.../posting/ProjectCostCollectionProvider.java`（双字段迁移）、`ErpPrjConstants`/`ErpPrjErrors`（按需）
Skill: `nop-backend-dev`
Item Types: `Add`
Prereqs: Phase 1

- [x] Add：`buildEvent` 汇率解析——按 D1 载体替代 `setExchangeRate(BigDecimal.ONE)`：currencyId=null/本位币 → rate=1 回退（行为保持）；非本位币 → 汇率解析（currencyId + 本位币 + voucherDate 边界），缺失抛 `ERR_EXCHANGE_RATE_REQUIRED`（守卫语义对齐 R1.42）；**缺失错误路径按 D1 (ii) 裁决落地**（选项 α：buildEvent 抛错传播 → approve 事务回滚 → 单据保持 SUBMITTED）。
       - Skill: `nop-backend-dev`
- [x] Add：`ProjectCostCollectionProvider.fact()` 双字段迁移——`setAmount(amountFunctional)` + `setAmountSource(amount)` + `setAmountFunctional(amount×ctx.exchangeRate)`（amount 保持本位币/功能金额语义，镜像 `PurAcctDocProvider:129-131` + P1-MA3-039 方案 A）；单币种路径（rate=1）行为保持（source==functional==amount 向后兼容）。
       - Skill: `nop-backend-dev`

Exit Criteria:

- [x] buildEvent 汇率解析三态（本位币回退/非本位币解析/缺失拒绝按 D1(ii) 错误路径）+ Provider 双字段折算行为落地，amountFunctional = amount×rate 运行时成立；`mvn compile -DskipTests -pl module-projects/erp-prj-service` 通过（本地化类型检查，解除 Phase 3 阻塞）

### Phase 3 - 测试 + 文档 + 回填（Add | Proof）

Status: completed
Targets: `module-projects/erp-prj-service/src/test/`、`docs/design/projects/cost-collection.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-17.md`
Skill: `nop-testing`
Item Types: `Add | Proof`
Prereqs: Phase 2

- [x] Proof：新增多币种工时过账测试组——①非本位币（USD 项目 + CNY 本位账套）汇率 seed → approve → 凭证行断言 amountSource=源币金额 + amountFunctional=amount×rate + voucherLine.exchangeRate=rate + fact.amount==amountFunctional（折算失真消除）；②非本位币汇率缺失 → 抛 ERR_EXCHANGE_RATE_REQUIRED + **单据保持 SUBMITTED**（approve 事务回滚，D1(ii) 选项 α 语义；buildEvent 校验错误先例同型，无告警派发）；③本位币/currencyId=null → rate=1 行为保持（既有断言零回归）；④既有 158 tests 零回归。
       - Skill: `nop-testing`
- [x] Proof：`mvn test -pl module-projects/erp-prj-service` 全绿（分域测试，全量验证属 Closure Gates）。
       - Skill: `nop-testing`
- [x] Add：owner doc 回填——`cost-collection.md` §2.4/§六 补多币种工时过账实现注记 + D1 裁决记录；arm-index P1-MA1-010 行追加 RC-R1.64 闭合注记（业务逻辑层闭合 + P2-RC-050 协同边界声明）；roadmap RC-R1.64 → done ✅（行标签按 B 类裁决改写）；本日志条目。
       - Skill: `none`

Exit Criteria:

- [x] 测试组全绿 + 既有 158 tests 零回归（分域 `mvn test -pl module-projects/erp-prj-service`；全量验证 + checker 属 Closure Gates）
- [x] owner doc/arm-index/roadmap 回填完成，D1 裁决留痕

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (ses_ff452213fffeHmcOMhwY74wrgJ) because **Blocker**：`ProjectCostCollectionProvider` 仅 setAmount 不设 amountSource/amountFunctional → 引擎回退 source==functional==amount → 仅 buildEvent 传 rate 不折算，Plan 的「折算失真消除」可观察结果与范围脱节。已修订：Provider 双字段迁移纳入 Phase 2 + D1 裁决维度 + Goal ② 改「buildEvent + Provider 双点协同」。另修：5 处 setExchangeRate(ONE) 全景盘点（ProjectCostAggregator:82/MaterialCostAggregator:73 归集头 metadata 面归 successor）+ 行号漂移修正（ExpenseCostAggregator:134/guardExchangeRate:554-568/findCurrencyById:571-577/orm.xml:722-759）。
- Independent draft review iteration 2: **needs revision** (ses_ff4464861ffeM9RzljphWbFDeq) because **Major**：`buildEvent(timesheet)` 在 tryPost :61 try/catch 之外 → 汇率缺失抛错传播中止审批 → 单据保持 SUBMITTED 非 APPROVED+posted=false，D1 残差 + Phase 3 测试② 原语义不可达。已修订：tryPost 结构入 Baseline；D1(ii) 显式裁决缺失错误路径（选项 α 传播→SUBMITTED 与既有 buildEvent 校验错误先例一致 / 选项 β 备选）；Phase 3 测试② 改 SUBMITTED 语义。另修：`fact.setAmount(amountFunctional)`（amount 保持本位币语义，镜像 PurAcctDocProvider:129-131）。
- Independent draft review iteration 3: **acceptable as-is** (ses_ff42e11c6ffePigH1kFVXVaug2) because iter-2 Major 修复实仓核验通过（buildEvent :61 在 try :62 外 + D1(ii) 选项 α/β 精确描述可达语义 + Phase 2/3 一致采用 α）+ iter-1 Blocker 修复核验通过（Provider 双字段迁移镜像 PurAcctDocProvider:129-131 + 单币种路径保持既有断言）+ 5 站点盘点与 Deferred 分类相干（voucher 面 in-scope / 归集头 metadata + PnL 面归 P2-RC-050）+ 无新 Blocker/Major。1 项非阻塞措辞观察（ExpenseCostAggregator:134 跨 Deferred 两项重复列出，分类/successor 一致，可选合并）。

## Closure Gates

- [x] 范围内行为完成（buildEvent 汇率解析三态 + 凭证折算正确性）
- [x] 相关文档对齐（cost-collection.md 注记 + arm-index + roadmap + logs）
- [x] 已运行验证（`mvn test -pl module-projects/erp-prj-service` 172/0/0 + 全量 `mvn test` BUILD SUCCESS 13:01 min + `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 全 19 规则 actual == baseline 零漂移[R2b=235/R2c=1434/R2d=35/R12a=70/R12b=66/R12c=40]）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 三项均为计划预裁定 residual，非范围内降级）
- [x] 独立草案审查已完成并记录（Draft Review Record iter-1 Blocker/iter-2 Major 修复 + iter-3 acceptable as-is）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 费用归集头/PnL 汇率路径（ExpenseCostAggregator:134 + ProjectPnlCalculator:105）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P2-RC-050 登记项（A4.2.120 维持 P2 分级）；PnL 非总账凭证，多币种精度偏差不破坏 GL 平衡；Q4 张力已声明
- Successor Required: `yes`（触发条件：P2-RC-050 从 watch-only 回队立项时，与 D1 解析 helper 协同复用）

### 归集头 exchangeRate metadata 面（ProjectCostAggregator:82 / MaterialCostAggregator:73 / ExpenseCostAggregator:134）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 归集头 exchangeRate 为 metadata（消费侧=PnL 聚合/辅助核算，非 voucher 路径——voucher 折算由 buildEvent 真实 rate + Provider 双字段达成）；LABOR 归集头与工时 voucher 同源但不同消费面；单币种主路径行为正确，多币种 PnL 精度归 P2-RC-050
- Successor Required: `yes`（触发条件：P2-RC-050 回队 / PnL 多币种折算立项时，一并修正归集头 metadata 与 D1 解析 helper 协同）

### 共享汇率解析 helper 抽取

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本行单点解析已满足 UC-PRJ-06 工时侧；helper 抽取在 P2-RC-050 回队时一并裁决（D1 选项 B 触发条件）
- Successor Required: `yes`（触发条件：P2-RC-050 回队 / 第二处汇率解析需求出现时）

## Closure

Status Note: 三 Phase 全部完成且经独立结束审计 PASS——buildEvent 汇率解析三态（null/币种不存在保守放行/本位币 → rate=1；非本位币边界解析缺失抛 ERR_EXCHANGE_RATE_REQUIRED）+ Provider 双字段折算（amountSource/amountFunctional，镜像 PurAcctDocProvider）落地，D1 全部裁决维度（载体/i/ii）按裁决执行；测试 4 组新增全绿 + erp-prj-service 172 tests 零回归 + 全量 mvn test/clean install BUILD SUCCESS + compliance checker 全 19 规则零漂移；owner doc/arm-index/roadmap/logs 回填完成。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话，general subagent）
- Evidence: task id `ses_feef01871ffepCeQARuY506EjF`（2026-08-18，verdict **PASS** 全 6 项：D1 代码逐条 file:line 核验[三态 :202-213/边界 :252-258/错误 :269-272/IBizObjectManager :230/:239/:259 零新增 daoFor] + 独立复跑测试[定向 4/0/0 + 全模块 172/0/0] + 文档回填四处核验 + 计划一致性 + 反模式零命中 + checker 复跑零漂移；非阻塞注记：工作树未提交属预期，闭包后由执行者勾选门控）

Follow-up:

- 无（Deferred 三项已裁定归 successor，见 Deferred But Adjudicated；P2-RC-050 协同边界已在 arm-index/owner doc 声明）
