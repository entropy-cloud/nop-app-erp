# ck-finance-posting — finance「过账与凭证」切片实现代码检查报告

> 工作项：C3.1。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-finance/erp-fin-service/src/main/java` 过账与凭证切片 30 个手写生产文件——`posting/` 包全 26 类（引擎 `ErpFinPostingProcessor`、sweep `ErpFinDeferredPostingRetryHelper`、异常记录器/工作台辅助、GL 映射 `ErpFinGlMappingResolver`/`ErpFinGlDistributionValidator`、Registry 3 类、`SchemaPropagator`、`ErpFinArApItemGenerator`、`FinPostingExecutor`、Dispatcher 3 类、Provider 7 类、DTO 4 类）+ `entity/ErpFinVoucherBizModel`/`ErpFinPostingExceptionBizModel`/`ErpFinGlMappingRuleBizModel`/`ErpFinVoucherBillRBizModel`/`ErpFinVoucherLineBizModel` + `processor/ErpFinPostingExceptionRetryProcessor`/`IgnoreProcessor`/`GlMappingRuleRefreshCacheProcessor` + `statemachine/ErpFinVoucherDocumentStateMachine` + `budget/CommitmentVoucherGenerator`（承付凭证生成/红冲属过账链对端）+ `ErpFinConstants` + `_vfs/nop/batch-task/fin/deferred-posting-sweep.batch.xml`（D4）。跨域核实：`AcctSchemaResolver`（master-data）、`PurReversalListener`（purchase 监听者抽样）、purchase/sales/inventory 各域 dispatcher 的 `postEvent` 返回值消费语义、`model/app-erp-finance.orm.xml`（versionProp/UK/期间粒度）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。核心链逐文件深读（引擎/sweep/异常工作台/GL 映射/Registry/Dispatcher 全读）+ 平台源码验证（`nop-entropy/nop-kernel/nop-api-core` QueryBean、`nop-service-framework/nop-biz` CrudBizModel）+ arm-index 复用裁决。
> 切片边界：AR/AP 核销（Reconciliation/AdvanceOffsetOrchestrator 辅助部分）、预算成本（budget 主链）、期间结账（close/profitloss/annualclose）归 C3.2-C3.4；本切片仅涉及其与过账链的交点。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-fin-001（D8/D2，承接 P1-CK-sal-003）Sweep 重试成功仅标记 RETRIED——源单 posted 回写通道在 finance 侧完全缺失（无正向 posted 事件）

- **控制点**：`app/erp/fin/service/posting/ErpFinDeferredPostingRetryHelper.java#doRetry`（L96-109）+ `#markRetried`（L130-136）+ `deferred-posting-sweep.batch.xml`（processor 直调 `helper.retry`）
- **证据**：`doRetry` 正向路径 `voucherBiz.post(event, ctx)` 成功后仅 `markRetried(ex)`（置 status=RETRIED/resolvedAt/resolvedBy），**无任何源单回写动作**。finance 引擎有反向闭环 SPI（`IErpFinVoucherReversedListener` + `VoucherReversedEvent`，红冲时派发），但**正向过账成功的对偶事件（VoucherPostedEvent 类）不存在**——grep module-finance 全模块零命中。类 javadoc O-16 注释（L32-33）只覆盖「幂等命中返回 null 视为补偿成功」场景，未覆盖「重试成功生成新凭证后源单 posted 仍 false」场景。
- **问题**：SYNC 失败链 = 域 dispatcher catch 吞异常（保 posted=false）→ `ErpFinPostingException` PENDING → sweep 重试成功 → 凭证存在但源单 posted 永久 false。设计 `posting.md §反写契约`「源单 posted 由域调用方在 post() 成功返回后自行置位」在 sweep 路径失效——重试时域调用方不在场，finance 又不持源实体（DAG 顶约束），形成无人承担的回写空档。下游后果链已在 sales 侧登记（`P1-CK-sal-003`：反审核/作废 posted 门控跳过红冲 → 孤儿凭证；退货暂估判定失真）——本条为该 finding 的 finance 侧控制点登记。
- **建议修复方向**：镜像反向闭环范式——引擎 post() 成功路径（或 RetryHelper 成功路径）派发 `VoucherPostedEvent`，各域实现 `IErpFinVoucherPostedListener` 回写自身 posted=true；或 sweep 重试成功后经 billType+billHeadCode 反查源域回写（需 per-domain SPI）。修复阶段与 sales `P1-CK-sal-003` 联合裁决（sales 侧备选：cancel/reverseApprove 改按凭证存在性判红冲前置）。
- **arm-index 裁决**：新增（grep arm-index「posted 回写/sweep 成功 posted/VoucherPostedEvent」零命中）。承接 P1-CK-sal-003 的 finance 侧对端。

### P1-CK-fin-002（D6）postVoucher（DRAFT→POSTED）无借贷平衡校验——不平衡的手工凭证可过账进 GL

- **控制点**：`app/erp/fin/service/entity/ErpFinVoucherBizModel.java#postVoucher`（L90-107）
- **证据**：方法体仅做 `assertPeriodNotLocked`（L93）+ 状态机边守卫 `assertCanPost`（L95）+ 状态翻转 + postedBy/postedAt 写回。无 `balanceTotals`/`assertBalanced` 调用——grep 全 service 层 `assertBalanced` 仅 `ErpFinPostingProcessor` L163（引擎自动过账路径）使用。DRAFT 凭证经标准 CRUD `save_` 创建时也无平衡校验（`ErpFinVoucherBizModel` 未覆写 `defaultPrepareSave`；`ErpFinVoucherLineBizModel` 为 20 行裸 CrudBizModel）。
- **问题**：owner doc `finance/README.md §关键业务规则` 1：「**借贷平衡**：每张凭证借方合计 = 贷方合计，否则不可过账」；`§借贷平衡规则`「必须平衡，否则不可过账」。手工补录凭证（异常工作台 manualEntry 场景 F）或直接 CRUD 建的 DRAFT 凭证行若借≠贷，`postVoucher` 照常置 POSTED → 不平衡凭证进入 GL 汇总（损益结转 `findPostedVoucherIds` 只按 docStatus/isReversed/postingType 过滤，不做平衡复检）→ 试算平衡表失衡。`previewReverseVoucher` 预览亦无平衡告警。
- **建议修复方向**：`postVoucher` 前置平衡断言（聚合行 `debitAmount`/`creditAmount` 总和比对，复用引擎 `assertBalanced` 的 `ERR_UNBALANCED` 错误码）；可选在 DRAFT save 钩子加软告警（不阻断草稿）。
- **arm-index 裁决**：新增（grep「postVoucher 平衡/手工凭证 校验」零命中；A2.5a MA2 状态机审计范围是迁移边非业务校验）。

### P1-CK-fin-003（D8）post() 幂等命中返回 null 与全域 dispatcher「null=失败」语义冲突——O-16 场景重审后 posted 永久悬挂

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#process`（L139-143：幂等命中 `return null`，无任何返回态区分）+ 域侧消费点 `ExpenseClaimPostingDispatcher#tryPost`（L43 `return voucherId != null`）、`EmployeeAdvancePostingDispatcher#tryPost`（L42）、`NotesPostingDispatcher#safePost`（L60）；跨域同型实证：purchase `PurInvoice/PurPayment/PurReturnPostingDispatcher`、sales `SalInvoice/SalReceipt/SalReturnPostingDispatcher`、inventory `InvPostingDispatcher` 全部 `voucherId != null`（grep 8 处命中）
- **证据**：引擎幂等命中（源单已过账）静默返回 null；RetryHelper javadoc L32-33（O-16）明确把 null 语义定义为「幂等命中=补偿成功」——**该语义仅在 sweep 路径成立**（RetryHelper 据此标 RETRIED）；域 Processor 消费 `tryPost==false` 时不置 posted=true（`ErpFinExpenseClaimProcessor` L284-289 等实证）。
- **问题**：时序 = approve 事务内 doPosting（REQUIRES_NEW 凭证已提交）→ 主事务后续失败回滚（乐观锁/SoD 后置守卫等，即 purchase `P1-CK-pur-002` 场景）→ 单据回到未审态 + 凭证孤立存在 → 用户重新 approve → tryPost 幂等命中返回 null → **posted 永远无法置 true**。此后单据 APPROVED + posted=false + 凭证存在：反审核/作废的 posted 门控跳过红冲 → 孤儿凭证滞留 GL（与 P1-CK-pur-002 构成同一孤儿凭证闭环的两个缺口）。调用方在既有 API 契约下无法区分「已过账（应视为成功）」与「真失败（posted=false 留存）」。
- **建议修复方向**：引擎 API 三态化——幂等命中时返回已有 voucherId（而非 null）或提供 `postResult` 结构；或幂等命中路径经 traceId 记录 INFO 之外向调用方传递可判定信号。最小改动：幂等命中分支返回 `findPostedVoucher` 的 id（alreadyPosted 已持有该查询能力）。修复时同步核全域 8 个 dispatcher 消费语义。
- **arm-index 裁决**：新增（grep「幂等命中 null/返回值语义」零命中；P0-MA2-018 是并发双 INSERT 维度，非返回值契约维度）。

### P1-CK-fin-004（D8/D6）SchemaPropagator 查询账套缺 ACTIVE 状态过滤——停用账套被传播过账、主账套可选中停用账套

- **控制点**：`app/erp/fin/service/posting/SchemaPropagator.java#findActiveSchemasByOrg`（L111-121：`q.addFilter(eq("orgId", orgId))` 后**无 status/isActive 过滤**，排序 `statusScore = "ACTIVE".equals(s.getStatus()) ? 0 : 100` 只把非 ACTIVE 排后不剔除）；消费点 `#resolveTargetSchemas`（L73-80：`for (ErpMdAcctSchema schema : allActive) targets.add(schema.getId())`）与 `#findPrimarySchemaId`（L94-104：`schemas.isEmpty() ? null : schemas.get(0)`）
- **证据**：方法名/注释声称「返回同组织下所有 ACTIVE 账套」（类 javadoc L24-25、`findPrimarySchemaId` javadoc「按 nature 优先级选取组织的 ACTIVE 账套」），实现无 ACTIVE filter——INACTIVE 账套照常进入 targets。对照组：模板 Provider `findTemplate` 显式 `eq("isActive", Boolean.TRUE)`（ErpFinTemplateAcctDocProvider L106）。
- **问题**：`erp-fin.multi-schema-enabled=true` 时，已停用账套（status≠ACTIVE）仍按传播列表逐账套生成凭证——owner doc `multiple-accounting-schemas.md §并行核算机制`「查询所有**启用的**账套」不满足；停用账套产生不应存在的凭证（若其期间/科目已停用则重试悬挂）。`findPrimarySchemaId` 在 org 仅存停用账套时把停用账套选为主账套（`AcctSchemaResolver.resolvePrimarySchemaId` 同型：`schemaPriority` 仅降权不剔除，注释「无 ACTIVE 账套时返回 null」与实现不符——只有空集才 null）。默认 `multi-schema-enabled=false` 时不触发主链（降 P1 非 P0 的依据），但 `findPrimarySchemaId` 的停用选中在关闭态同样可达（Dispatcher `resolveAcctSchemaId` 全量调用它）。
- **建议修复方向**：`findActiveSchemasByOrg` 增加 `eq("status", "ACTIVE")` filter（或 xmeta 管道过滤）；`AcctSchemaResolver.resolvePrimarySchemaId` 同步收紧（属 master-data，跨域修复项）。status 字段取值域需先与 `ErpMdAcctSchema` 模型核对（ACTIVE/INACTIVE 字面量）。
- **arm-index 裁决**：新增（带复用注记）。与 `P1-MA2-095`（多账套**读路径**双计，已 resolved）同族但不同控制点（写路径账套选取）；grep「findActiveSchemasByOrg/ACTIVE 过滤」零命中。

### P1-CK-fin-005（D10/D2）acctSchemaId 解析为 null 时 post() 静默「成功」零凭证——无异常记录、无告警、指标误报 success

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#process`（L137 `resolveTargetSchemas` → L167 for 空列表零次 → L199-204 `recordResult(true)` 记成功 + L201 打「过账成功」日志）+ `SchemaPropagator#resolveTargetSchemas`（L45-55：`primarySchemaId == null` 时关闭态返回空 `single` 列表、开启态返回 `new ArrayList<>()`）
- **证据**：`AcctSchemaResolver.resolvePrimarySchemaId`（master-data，`AcctSchemaResolver.java:26`）注释明示「无 ACTIVE 账套时返回 **null**」；全部 3 个 finance Dispatcher 的 `resolveAcctSchemaId` 直通该返回值入 `event.setAcctSchemaId`（ExpenseClaim L65、EmployeeAdvance L67/L102/L128、Notes L76/L105）。process() 对 `targetSchemas.isEmpty()` 无守卫——既不抛 `NopException` 也不记 `ErpFinPostingException`。
- **问题**：组织未配账套（上线配置遗漏/新组织漏初始化）时：域 dispatcher tryPost 返回 false → 单据 posted=false 留存，但 finance 侧**零异常记录**（recordPostFailure 只在 catch 里）→ sweep 无 PENDING 可扫 → 期末结账前置检查（扫 PENDING/RETRYING/MANUAL）**不拦截** → 静默断链且不可观测（`postingMetrics.recordResult(true)` 还把失败计入成功率分母失真，G1/G2 分级全部落空）。这是比「失败有记录」更糟的第三态：失败被伪装成成功。
- **建议修复方向**：`process()` 前置守卫——`targetSchemas.isEmpty()` 时抛 `NopException`（新增 `ERR_NO_ACCT_SCHEMA` 类错误码，含 orgId 参数），使该配置缺陷进入异常工作台 + G2 告警链。与 P1-CK-fin-004 修复联动（ACTIVE 过滤收紧后 null 概率上升，守卫更必要）。
- **arm-index 裁决**：新增（grep「targetSchemas 空/空账套/静默成功」零命中）。

### P2-CK-fin-006（D2/D7，承接 P2-CK-sal-019）Sweep 重试无源单有效性校验通道——PENDING 异常可为由 post() 重建的已作废单生成凭证

- **控制点**：`app/erp/fin/service/posting/ErpFinDeferredPostingRetryHelper.java#doRetry`（L96-109：`rebuildEvent(ex)` 直接 `voucherBiz.post`，仅依赖异常记录自身字段）+ `deferred-posting-sweep.batch.xml` loader（filter 仅 `status=PENDING + retryCount<3 + occurrenceTime≥now-24h`，无源单状态联查）
- **证据**：doRetry 与 batch loader 均无源单状态前置检查；finance 不持源实体（DAG 顶），亦无「源单存活性探针」SPI（grep `IErpFinPostedProbe`/`sourceValid` 零命中——posting-log.md §运行监控落地路径残留风险 (ii) 已预告该探针为可选 Follow-up，但未覆盖本竞态面）。eventData 为 t1 失败时刻的 billData 快照，重试时不刷新。
- **问题**：时序（sales 侧已登记 P2-CK-sal-019，本条为 finance 侧控制点）= 单据审核 → 过账失败 PENDING → 单据作废/反审核（posted=false 门控放行、无红冲、**不取消 finance 侧 PENDING 记录**）→ sweep 24h 窗口内重试成功 → 为已作废单据生成有效凭证（AR/AP 入账），此后 posted=false 使全部红冲门控跳过 → 孤儿凭证。REVERSAL 路径同型（reverse 重试无源单校验）。
- **建议修复方向**：与 P1-CK-fin-001 共用事件/SPI 通道——(a) 源域 cancel/reverseApprove 联动作废对应 PENDING 记录（需按 billHeadCode+businessType 反查 mutation，域侧实现）；或 (b) finance 定义「重试前源单探针」SPI 由各域注册校验器；或 (c) 最小兜底：sweep 重试成功派发 posted 事件时附带源单状态回查（与 001 的 listener 机制合并落地）。修复阶段 sales/finance 联合。
- **arm-index 裁决**：新增（承接 P2-CK-sal-019 的 finance 侧对端登记；arm-index grep「重试 源单状态/已作废 凭证」零命中）。

### P2-CK-fin-007（D1）translateFactsForSchema 内死变量 + 跨域实体 daoFor 直查越权（同方法两种范式并存）

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#translateFactsForSchema`（L714：`IErpMdSubjectBiz mdSubjectBiz = bizObjectManager.getBizObject(ErpMdSubject.class.getSimpleName()).asProxy();` **声明后零使用**；L742：`daoProvider.daoFor(ErpMdSubject.class).getEntityById(mappedId)` 直查 master-data 实体）
- **证据**：同类 `resolveSubjects`（L645-648）对同一实体走 `IBizObjectManager` 按名解析 `IErpMdSubjectBiz` 的 I*Biz 管道并注释说明理由（「finance→erp-md-service 仅 test 作用域，故非 BizModel 编排 bean 经 IBizObjectManager 按名解析」）；translateFactsForSchema 却绕开该管道用 daoFor 直查且**无注释豁免说明**（AGENTS.md「仅当 I*Biz 无法满足需求时才使用 IDaoProvider 并在代码注释中记录原因」）。
- **问题**：已知失败模式 3（`docs/skills/README.md §已知失败模式` 3：跨实体访问应通过 I*Biz 接口；直接 IDaoProvider 调用须在注释中说明原因）双违反：死变量证明本意走 I*Biz、实现退化为 daoFor；绕过数据权限管道（同进程下行为等价，但架构纪律漂移，且 daoFor 直查在权限管道启用后行为分歧）。
- **建议修复方向**：删除死变量，L742 改用已解析的 `mdSubjectBiz` 查目标科目（或补注释豁免说明走 daoFor 的原因）。
- **arm-index 裁决**：新增（R2c 基线条目内的真实缺陷维度——compliance checker R2 只查 daoFor 计数不查豁免注释完整性，per-site 未覆盖此处）。

### P2-CK-fin-008（D10）process() catch 块 no-op 自赋值——多账套循环失败后 event.acctSchemaId 停留在最后迭代值

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#process`（L206：`event.setAcctSchemaId(event.getAcctSchemaId());`——自赋值无效语句）
- **证据**：成功路径 L195 `event.setAcctSchemaId(originalSchemaId)` 恢复原值；`originalSchemaId` 声明于 try 块内（L166），catch 块作用域不可见——L206 显然是「恢复 originalSchemaId」意图的残留写法（编译通过但无效果）。
- **问题**：多账套循环中非原账套迭代失败（如 L188 `arApItemGenerator.generate` 抛出）时，event.acctSchemaId 停留在当前迭代的 schemaId；随后 `recordPostFailure(run, event, e)`（L212）把**错误的 acctSchemaId** 写入 ErpFinPostingException（重试 rebuildEvent 将带错账套重过账，可能落错账套或幂等判断失真）；同时 event 突变泄漏给持有该 event 引用的调用方（域 dispatcher 通常不复用，实际面窄）。单账套（默认）下 originalSchemaId==迭代值，无实际影响。
- **建议修复方向**：将 `originalSchemaId` 提升到 try 外（L137 前声明），catch 内 `event.setAcctSchemaId(originalSchemaId)`；顺带消除 L206 无效语句。
- **arm-index 裁决**：新增。

### P2-CK-fin-009（D6）红冲草稿丢失 amountSource 按行数据——多币种红冲凭证源币金额失真、汇率仅取首行

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#buildReversalDraft`（L780-810：fact 只 setAmount（取负的 functional）+ subjectId/dcDirection/维度，**不复制 `ol.getAmountSource()`/`ol.getAmountFunctional()`**）+ `#prepareReversalContext`（L588-592：currencyId/exchangeRate 仅取 `originalLines.get(0)`）
- **证据**：`persistVoucher` L863-864 对 null 的 amountSource/amountFunctional 回退到 `fact.getAmount()`——红冲路径 fact 未设置，落库的 `line.amountSource` = 取负后的本位币金额，而非原行的源币种金额取负。对照正向路径：P2P/O2C Provider 已显式传双字段（`posting.md §实现契约`「P2P/O2C 已迁移双字段」），多币种为活跃路径。exchangeRate 取首行——凭证模型是单币种（ctx 单值）尚可接受，但首行 exchangeRate 为 null 时红冲凭证行汇率落 `EXCHANGE_RATE_DEFAULT=1`（L854-856 回退），多币种原凭证（rate≠1）的红冲行汇率失真。
- **问题**：多币种凭证红冲后：红字凭证行的 `amountSource` ≠ −原行.amountSource（被本位币金额顶替）、`exchangeRate` 可能回退 1 → 源币口径的辅助对账/多币种报表（现金流量表双币列、AR/AP 源币余额）失真。GL 本位币净额正确（totalDebit/totalCredit 取负平衡保持），故降 P2 非 P1。
- **建议修复方向**：`buildReversalDraft` 逐行复制 `ol.getAmountSource().negate()`/`ol.getAmountFunctional().negate()`（null 保持回退语义）；`prepareReversalContext` 对首行 rate null 时回退取任一非 null 行或原凭证头字段。
- **arm-index 裁决**：新增（P1-MA3-039/R1.9 登记的是正向双字段迁移，红冲路径未覆盖）。

### P2-CK-fin-010（D6/D8）resolveAcctSchemaIdFromContext 恒返回 null——GL 映射的账套精确规则永不命中

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#resolveAcctSchemaIdFromContext`（L686-688：`return null; // 多账套通配匹配`）+ 消费点 `#resolveSubjects`（L631-632：`glMappingResolver.resolveSubjectCode(fact.getBusinessType(), fact.getAccountKey(), dims, resolveAcctSchemaIdFromContext())`）
- **证据**：`ErpFinGlMappingResolver#matches` L163：`rule.getAcctSchemaId() != null && !Objects.equals(rule.getAcctSchemaId(), acctSchemaId)` → 不匹配——传入 null 时**所有 acctSchemaId 非 NULL 的规则全部被跳过**。owner doc `gl-mapping-rules.md §3.3 决策表示例` 的 R2（acctSchemaId=7 账套精确）/R4（双精确）即此类规则。而 `process()` 调用链上明明有 `primaryCtx.getAcctSchemaId()`（L153-154）与循环内 `ctx.getAcctSchemaId()`（L173-174）可用——`resolveSubjects(facts, context)` 签名未携带 ctx，方法是纯 stub。
- **问题**：运维按 owner doc §3 配置的「税务账套用 5001、财务账套用 1403」类账套精确规则（priority≥100 + acctSchemaId 非 NULL）**静默永不生效**（非 strict-mode 下连日志都只有 miss fallback INFO）；§3.3 示例场景直接失效。doc/code 漂移：§3.1 算法第 2 步「对 acctSchemaId 做相等匹配」被调用侧废置。
- **建议修复方向**：`resolveSubjects` 签名增加 `AcctDocContext`（或 acctSchemaId 参数），透传当前账套；注意与「跨账套传播时 resolver 仅运行一次 pre-translation」语义（§5.2）兼容——用原账套 id 解析即可（facts 在源账套上下文生成）。
- **arm-index 裁决**：新增（grep「resolveAcctSchemaIdFromContext/acctSchemaId 规则不命中」零命中；A1 计划只审了 resolver 本体算法）。

### P2-CK-fin-011（D8/D6）多套账传播下辅助账幂等去重缺账套维度——仅首个账套生成 ErpFinArApItem

- **控制点**：`app/erp/fin/service/posting/ErpFinArApItemGenerator.java#generate`（L75：`existsItem(profile.sourceBillType, event.getBillHeadCode(), context)` 仅按 `(sourceBillType, sourceBillCode)` 查重，**无 acctSchemaId 条件**；对照 L104 `item.setAcctSchemaId(event.getAcctSchemaId())` 辅助账本身带账套维度）+ 调用点 `ErpFinPostingProcessor#process`（L187-189：循环内 `event.setAcctSchemaId(currentSchemaId)` 后逐账套调 `arApItemGenerator.generate`）
- **证据**：`findItems`（L199-207）filter 仅 `and(eq("sourceBillType", ...), eq("sourceBillCode", ...))`。多账套第二次迭代 existsItem 命中第一账套已建项 → 跳过 → 第二账套无辅助账。
- **问题**：`multi-schema-enabled=true` 且源账套 isPropagate 时：AR/AP/收付款/退货等带辅助账的业务类型只生成第一账套的辅助账项，其余账套的核销视图（按 acctSchemaId 过滤的 open items）为空——`multiple-accounting-schemas.md §并行核算机制`「逐账套生成」在辅助账层断裂。GL 凭证层逐账套完整（persistVoucher 在循环内），形成「有凭证无辅助账」的账套。
- **建议修复方向**：`existsItem`/`findItems`/`cancelOnReverse` 的查重与反查 filter 增加 `eq("acctSchemaId", event.getAcctSchemaId())`（与 cancelOnReverse 联动改，防红冲只取消首账套项——cancelOnReverse L129-140 同型无账套过滤，多账套下取消语义同样只覆盖首套，修复时一并）。
- **arm-index 裁决**：新增（带复用注记）——P1-MA2-002/009（R1.9 双金额字段）未覆盖账套维度幂等；与 P1-MA2-095 多账套族同因不同控制点。

### P2-CK-fin-012（D9）异常工作台计数全量加载 findAllByQuery().size()——注释宣称聚合 COUNT 与实现不符，5 分钟周期任务 + 期末门控反复全表载入

- **控制点**：`app/erp/fin/service/entity/ErpFinPostingExceptionBizModel.java#countUnresolved`（L209 `List<ErpFinPostingException> all = dao.findAllByQuery(q); return all.size();`）+ `#countVouchersSince`（L296）+ `#countExceptionsSince`（L303）+ `#countManualResolutionsSince`（L310）同型；调用面 `refreshPostingExceptionBacklog`（L124-131，`GlobalExecutors.globalTimer()` 每 5 分钟）+ `getRuntimeMetrics`（@BizQuery）
- **证据**：方法注释（L216-218）明示「读路径直接用 daoProvider().daoFor() **聚合 COUNT**」——实现却是 findAllByQuery 全量实体载入后内存 `.size()`。未决异常堆积（如科目长期未配置的 PENDING/MANUAL 累积）与凭证表增长（时间窗内全列载入）使每次刷新成本线性上升。
- **问题**：与注释意图相反的实现；backlog gauge 周期任务固定 5 分钟一次全量载入 `ErpFinVoucher` 时间窗行（含全部行字段）。属性能缺陷（N+1 级）而非正确性错误——计数结果正确。
- **建议修复方向**：改 `dao.countByQuery(q)`（平台 IEntityDao 提供 count 能力）或 QueryBean `queryForLong`；4 处同修。
- **arm-index 裁决**：新增（A4.1a 代码质量审计未覆盖 metrics 查询路径）。

### P2-CK-fin-013（D6/D2）手动重试 rebuildEvent 将缺失汇率回退为 1——绕过 RC-R1.42 外币汇率守卫，与 sweep 路径行为分裂

- **控制点**：`app/erp/fin/service/processor/ErpFinPostingExceptionRetryProcessor.java#rebuildEvent`（L98：`event.setExchangeRate(entity.getExchangeRate() != null ? entity.getExchangeRate() : BigDecimal.ONE);`）
- **证据**：对照 sweep 路径 `ErpFinDeferredPostingRetryHelper#rebuildEvent`（L124：`event.setExchangeRate(ex.getExchangeRate())`——不设回退，null 交给引擎 `guardExchangeRate`（ErpFinPostingProcessor L554-568）判定非本位币缺汇率时抛 `ERR_EXCHANGE_RATE_REQUIRED` 拒绝）。手动路径写入非 null 的 ONE → `guardExchangeRate` L555 `event.getExchangeRate() != null` 短路放行 → **外币单据以 rate=1 过账**（本位币金额=源币面额，折算失真）。另 `ErpFinPostingExceptionBizModel#rebuildEvent`（L337-353，同型含 L346 同缺陷）为 private 零调用死代码（retry 已委托 RetryProcessor）。
- **问题**：同一条 ErpFinPostingException，经 sweep 重试（守卫拒绝、继续 PENDING）与经 UI 手动 retry（rate=1 错误过账）产生**相反结果**——处置通道行为分裂；RC-R1.42「汇率缺失报错拒绝过账」（UC-FIN-12 断言②）在手动通道失效。
- **建议修复方向**：RetryProcessor L98 改为透传 null（与 RetryHelper L124 对齐，交给守卫）；删除 BizModel 内死代码 `rebuildEvent`。
- **arm-index 裁决**：新增（RC-R1.42 修复登记的是引擎守卫本体，重试重建路径的旁路未覆盖）。

### P2-CK-fin-014（D2/D7）手动 retry 失败既回滚计数又增生重复 PENDING 记录——同单多记录扩大 sweep 并发面

- **控制点**：`app/erp/fin/service/processor/ErpFinPostingExceptionRetryProcessor.java#retry`（L35-67：L43 `updateEntity(entity)` 翻 RETRYING+retryCount+1 在**外层 @BizMutation 事务**内；L48 `voucherBiz.post` 失败抛出 → 外层回滚 → RETRYING 翻转与 retryCount 递增一并回滚，实体回到 PENDING）+ 引擎侧 `ErpFinPostingProcessor#process` catch（L205-214 `recordPostFailure` 经 REQUIRES_NEW 落**新** PENDING）
- **证据**：与 sweep 路径对照：`ErpFinDeferredPostingRetryHelper#retry` 的 `incrementRetryAndRethrow`（L138-168）用独立 REQUIRES_NEW 重新加载受管实体递增（注释明示「避免外层已回滚 session 的游离实体更新丢失」）——手动路径无此处理。
- **问题**：(a) 手动重试失败时 retryCount 不递增（回滚），MAX_RETRY 升级 MANUAL 只能靠 sweep 路径累积——手动反复重试永不升级、无告警；(b) 每次失败经引擎 recordPostFailure 新增一条 PENDING（同 billHeadCode+businessType 多条），sweep 后续对多条并发抓取，直接扩大 `P0-MA2-018`（alreadyPosted TOCTOU 并发双凭证，deferred）的触发面；(c) UI 表现：重试按钮每次失败后记录列表 +1，工作台数据膨胀。
- **建议修复方向**：retry 编排对齐 RetryHelper——翻 RETRYING 与递增计数改独立事务提交（或失败 catch 内以 REQUIRES_NEW 补递增）；recordPostFailure 增加同 (traceId,businessType,billHeadCode,errorCode) PENDING 去重/合并。
- **arm-index 裁决**：新增（带复用注记）——P0-MA2-018 登记的是 alreadyPosted 并发本身（deferred），本条是其**触发面扩张**通道，不同控制点。

### P2-CK-fin-015（D8/D2）reverseVoucher（UI 红冲入口）对业务凭证只置 isReversed 标记——源单不回退、辅助账不取消、无 VoucherReversedEvent，业账失配

- **控制点**：`app/erp/fin/service/entity/ErpFinVoucherBizModel.java#reverseVoucher`（L109-123：仅 `assertPeriodNotLocked` + isPosted 分类守卫 + `setIsReversed(true)` + updateEntity）对照引擎路径 `IErpFinVoucherBiz#reverse` → `reverseProcess`（红字凭证 + `cancelOnReverse` L246 + `markOriginalVoucherReversed` L255 + `dispatchReversalEvent` L256）
- **证据**：GL 生效机制 = 排除式汇总（`ProfitLossClosingService#findPostedVoucherIds` L190 `eq("isReversed", Boolean.FALSE)`）——reverseVoucher 标记后该凭证确实退出 GL 聚合。但三条反向闭环动作全部缺席：源单 posted 不回退（无 listener 派发）、`ErpFinArApItemGenerator.cancelOnReverse` 不调用（辅助账 openAmount 残留、仍可核销）、无红字凭证留痕（`reversalOfVoucherId` 链空）。owner doc `state-machine.md` L41/L48 将 reverseVoucher 定位为「单边标记（已知简化）」且声称「红冲闭环功能完整」——doc 已裁决标记式本身，但**未覆盖其对业务凭证（经业财回链生成、有源单）使用的失配面**。
- **问题**：财务员在凭证列表对业务凭证（如 AP_INVOICE 产物）执行 reverseVoucher：GL 移除该笔、但 purchase 侧发票 posted=true/APPROVED 原样、辅助账开放。后续：(a) 业务侧反审核走 posted=true 分支调 `reverse()` → `findAllPostedVouchers` 找不到未冲销 NORMAL 凭证 → 抛 `ERR_REVERSE_SOURCE_NOT_FOUND` → **反审核被永久阻断**；(b) 辅助账仍开放可核销 → 已出 GL 的应付被核销。UI 无「业务凭证禁用 reverseVoucher」拦截（入口未区分凭证来源）。
- **建议修复方向**：reverseVoucher 前置检查 `ErpFinVoucherBillR` 存在回链（业务凭证）时拒绝并提示走源单反审核（或内部转调引擎 `reverse(billCode, businessType)`）；手工凭证（无回链）保留现行为。
- **arm-index 裁决**：新增（带复用注记）——P1-MA2-031(b)（红字凭证 isReversed 终态语义混淆，resolved）是凭证侧语义；本条是「源单/辅助账失配」控制点，未被覆盖。

### P3-CK-fin-016（D10）findBillLinks 对 null businessType 直接 NPE——sweep 重建事件类型解析失败时以 NPE 而非业务错误码失败

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#findBillLinks`（L945-951：L949 `eq("businessType", businessType.name())` 无 null 守卫）；上游 `ErpFinDeferredPostingRetryHelper#parseBusinessType`（L201-211：未知枚举名 `LOG.warn → return null`）→ `doRetry` L100/L102 以 null businessType 调 `reverse`/`post`
- **问题**：`ErpFinPostingException.businessType` 为脏值（未知枚举名/空）时，重试路径在 findBillLinks 抛裸 NPE——被 recordPostFailure 归类 `ERR_POSTING_UNEXPECTED_FAILURE`（记录闭环不断），但错误信息为 NPE 无定位价值，且正向路径 NPE 发生在 alreadyPosted（L139）早于 resolveProvider 的 `ERR_NO_PROVIDER` 友好错误。
- **建议修复方向**：`reverseProcess`/`process` 入口对 null businessType 抛 `ERR_NO_PROVIDER`（或专用 invalid-business-type 错误码）；parseBusinessType 失败时直接以业务错误终止而非传 null。
- **arm-index 裁决**：新增。

### P3-CK-fin-017（D10）resolveOpenPeriod 期间命中集无排序——重叠期间配置下 get(0) 不确定

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java#resolveOpenPeriod`（L511-532：filter `le(startDate)+ge(endDate)+eq(orgId)`，无 orderBy，L526 `periods.get(0)`）
- **证据**：`ErpFinAccountingPeriod` 唯一键为 `(code, orgId)`（orm L688-690），无日期区间唯一/互斥约束——手工建跨月/重叠期间（如调整期 isAdjustment 与常规月重叠）时同一 voucherDate 命中多行，DB 返回序不定。
- **问题**：正常月度运营（每月一期不重叠）无影响；期间配置错误或启用调整期重叠时，凭证 periodId 归属随机（可能落入 CLOSED 的调整期而误抛 ERR_PERIOD_CLOSED，或 OPEN 判定漂移）。防御缺失级。
- **建议修复方向**：命中多期时确定性选择（如 `isAdjustment=false` 优先 + startDate DESC）或命中 >1 抛配置错误提示。
- **arm-index 裁决**：新增。

### P3-CK-fin-018（D6）CommitmentVoucherGenerator 红冲行 dcDirection 保留原方向但借贷互换——与引擎红冲范式字段语义分裂

- **控制点**：`app/erp/fin/service/budget/CommitmentVoucherGenerator.java#writeReversalFromLines`（L232-234：`line.setDcDirection(ol.getDcDirection()); line.setDebitAmount(origCredit); line.setCreditAmount(origDebit);`）对照引擎 `buildReversalDraft`（L797-798：dcDirection 保留 + `fact.setAmount(...negate())` 同向负数语义）
- **问题**：两套红冲并存：引擎 = 同方向负金额（debit=-100）；承付 = 借贷互换正金额（credit=100）+ dcDirection 未换。GL 净额等效（按 debitAmount/creditAmount 汇总均正确），但按 `dcDirection` 过滤的消费方（凭证行展示、报表分类）会把承付红冲行误判为借方行；且 reversal 行 amountSource=origDebit+origCredit（正数，L237）与引擎负数语义不一致。凭证生成路径（`ErpFinVoucherDocumentStateMachine` 注释列 7 生成路径）内部范式不统一。
- **建议修复方向**：承付红冲对齐引擎（dcDirection 保持 + 金额取负）或显式交换 dcDirection；至少在类 javadoc 登记差异语义。
- **arm-index 裁决**：新增（A2 承付审计未覆盖红冲行字段级语义）。

### P3-CK-fin-019（D2/D9）PostingRun.captureTemplate 恒置 null——成功日志与失败记录的模板描述观测点死置

- **控制点**：`app/erp/fin/service/posting/ErpFinPostingProcessor.java` 内部类 `PostingRun#captureTemplate`（L463-465：`templateDesc = null;` 无任何赋值逻辑）+ 调用点 L158 `run.captureTemplate(facts)`（参数未用）
- **问题**：`posting-log.md §规则命中日志`「路由结果：命中的模板（code + version）」——实现里 `templateDesc` 永远为 null，成功日志 L201-203 的 `template={}` 恒空，排障场景「这笔为什么记到错误科目 → 看命中模板」缺关键信号。观测设计意图未落地（属可观测性缺口，非行为错误）。
- **建议修复方向**：captureTemplate 从 facts 提取 accountKey/amountKey 指纹（或 ctx 扩展携带模板 code/version——需 TemplateAcctDocProvider 在 ctx 回填），至少移除死参数避免误导。
- **arm-index 裁决**：新增（posting-log 落地计划登记了结构化日志骨架，模板字段死置未覆盖）。

## 移交项裁决结论（回填 sales 检查）

| 移交 finding | 裁决 | 结论与依据 |
| --- | --- | --- |
| `P1-CK-sal-003` sweep 重试成功不回写源单 posted | **证实** | finance 侧控制点登记为 **P1-CK-fin-001**：`ErpFinDeferredPostingRetryHelper#doRetry/markRetried` 成功路径仅标 RETRIED，正向 posted 事件/SPI 全域不存在（grep 零命中）；sales 侧后果链（红冲门控跳过、暂估失真）成立。修复联合裁决。 |
| `P2-CK-sal-018` OFFSET_ESTIMATED_RECEIVABLE 硬编码 TRUE | **证伪（finance 侧）** | grep 全仓 `OFFSET_ESTIMATED_RECEIVABLE` 仅命中 `SalReturnPostingDispatcher.java`（L51 常量定义 + L121 硬编码 TRUE 构造点）——**全仓（含 finance 引擎、sales Provider、ErpFinArApItemGenerator）无任何消费者读取该标记**。finance 引擎只透传 billData 快照（PostingEvent→Provider），不解释业务标记；「双路径区分失效」的根因（构造点硬编码 + 消费端缺失）全部在 sales 域。维持 sales finding 归属（P2-CK-sal-018），finance 侧无控制点、不新建 finding。 |
| `P2-CK-sal-019` PENDING 异常与源单作废竞态 | **证实** | finance 侧控制点登记为 **P2-CK-fin-006**：`doRetry` 与 `deferred-posting-sweep.batch.xml` loader 均无源单状态前置检查，且 finance 无源单探针 SPI（DAG 顶约束）；sales cancel/reverseApprove 不联动作废 PENDING 记录。sweep 24h 窗口内为已作废单生成凭证的时序成立。 |

## 验证为正确（显式排除，防误报）

- **平台 API `QueryBean.addOrderField(name, desc)` 第二参为 desc**（nop-entropy `nop-kernel/nop-api-core/.../QueryBean.java:433` `OrderFieldBean.forField(name, desc)` 实证）——`ErpFinGlMappingRuleBizModel#findApplicableRules` L81 `addOrderField("priority", true)` + 注释 DESC **正确**（运维调试查询确为优先级降序）。
- **GL 映射缓存失效钩子覆盖完整**：`defaultPrepareSave/Update/Delete` 覆写的 afterCommit 失效（ErpFinGlMappingRuleBizModel L46-67）覆盖 deleteById_/deleteByQuery_——平台 `CrudBizModel.delete`（nop-biz L1059）与 `deleteByQuery`（L1505-1507）均经 `invokeDefaultPrepareDelete`；afterCommit 仅提交后失效语义正确（回滚不失效）。
- **B1 吞异常悬挂修复（lesson 09 / R1.16）在位验证**：引擎 `process`/`reverseProcess` catch 均「先落异常记录（REQUIRES_NEW 独立事务 `ErpFinPostingExceptionRecorder`）再 rethrow」，非 NopException 经 O-6 泛化记录；sweep `incrementRetryAndRethrow` 独立事务递增 + MAX_RETRY→MANUAL + `fin.posting-exception` 告警（G2 分级落地）；域 dispatcher tryPost 吞异常是显式设计语义（有 sweep 兜底承接），与 sales/purchase 报告裁决一致，不重复登记。
- **REQUIRES_NEW + @SingleSession 事务/Session 分层**：`ErpFinVoucherBizModel#post/#reverse` REQUIRES_NEW（L74/L82，nop-check 豁免注释在位）+ 编排层 `@SingleSession`（process/reverseProcess L129/L220）——对齐 `processor-extension-pattern.md` 硬规则 1 与 ORM Session 作用域注记。
- **乐观锁在位**：`ErpFinVoucher` `versionProp="version"`（orm 实证）——`markOriginalVoucherReversed`/`reverseVoucher`/`postVoucher` 的 updateEntity 走版本检查；`markOriginalVoucherReversed` 仅标记 NORMAL+POSTED+未冲销凭证（REVERSAL 跳过防连环红冲）。
- **Registry 聚合**：`ErpFinAcctDocRegistry` 非默认 Provider 同 businessType 冲突启动期 fail-fast（ERR_DUPLICATE_PROVIDER）、fallback 仅填空缺——对齐 posting.md §注册方式；`ErpFinReversalListenerRegistry.dispatch` 逐监听者 try/catch 隔离 + 失败收集落工作台（posting.md 裁决 3 逐条落地），启动零监听者 warn（O-19 注释在位）。
- **汇率守卫（RC-R1.42）在位**：`prepareContext` L543 经 `guardExchangeRate`（L554-568）——非本位币 + 缺汇率抛 `ERR_EXCHANGE_RATE_REQUIRED`；本位币/币种不存在保守放行 rate=1（D2 裁决语义）。（重试旁路缺陷另登记 P2-CK-fin-013，守卫本体正确。）
- **GlDistributionValidator**：Σpercent ≠100 抛 `ERR_GL_DISTRIBUTION_PERCENT_SUM`（PERCENT_EPSILON 容忍）；拆行 scale 4 HALF_UP + 末行补差保 Σ==原行；amountSource/amountFunctional 同比拆分且 null 保持回退；getOrder=100 高序——UC-FIN-04/15 断言满足。
- **承付红冲不双冲**：COMMITMENT 凭证被 `findAllPostedVouchers` 的 postingType 过滤（L935-936）排除，红冲由 `CommitmentVoucherGenerator#reverseCommitment` 独立承担（billType 三路径同派发保 lookup 对称）——引擎/生成器分工自洽，无「reverse() 找不到 COMMITMENT 凭证」问题。
- **辅助账 credit memo 语义**：PUR_RETURN/SAL_RETURN 负 openAmount（resolveAmountFunctional 取负）+ EXPENSE_CLAIM 公司直付不挂员工应付（paymentMode 判断）——对齐 javadoc 声明的 credit memo/直付契约。
- **D1 机械扫描全零**：切片 30 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now`=0、`extends RuntimeException/Exception`=0、字符串 `==` 比较仅枚举/null 判等（字典值比较均 Objects.equals）、无手编生成产物。
- **CANCELLED 凭证死状态**：`ErpFinVoucherDocumentStateMachine` L33-36 已登记为 intentional reserved（零 writer、不纳入任一集合）——P1-MA2-031 resolved 裁决在位，不重复登记。
- **凭证编码长度**：`buildVoucherCode` 用业务类型 int code（L988 注释「PST-{code}-{32hex} ≤ 40 适配 CODE VARCHAR(50)」）数学正确。
- **ErpFinVoucherBillR (billCode, businessType) 非唯一索引**：posting-log.md 索引裁决（不触 P0-MA2-018 UK 冲突）已在 orm 落地（posting-log.md §索引裁决实证），过账/红冲热反查有索引支撑。
- **红冲后 isReversed 排除式 GL 汇总自洽**：原凭证与红字凭证（均 isReversed=true）同时退出 `findPostedVoucherIds` 聚合——数学上等效「原 − 原 = 0」，两种红冲语义（标记式/红字式）在排除式汇总下 GL 净额一致（业务面失配另见 P2-CK-fin-015）。
- **advance cashRepay billHeadCode 含时间戳防碰撞**：`postCashRepay` L100 `"EA-CASH-REPAY-{code}-{millis}"` 用 `CoreMetrics.currentTimeMillis()`（平台时钟非 System.），幂等经 ErpFinPostingException 重建同 code 事件保持，重试不自增生凭证。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `postVoucher 平衡`/`幂等命中 null`/`targetSchemas 空`/`findActiveSchemasByOrg`/`translateFactsForSchema daoFor`/`resolveAcctSchemaIdFromContext`/`reverseVoucher 源单`/`retry 汇率 ONE`/`countUnresolved size`/`captureTemplate` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - alreadyPosted TOCTOU 并发双凭证（sweep+手动+SYNC 并发）→ **P0-MA2-018**（deferred：字面 UK 三重冲突不可实施，方向 A-D 维持）——本切片 P2-CK-fin-014 登记的仅是其触发面扩张通道（新增控制点）。
  - alreadyPosted/findBillLinks 循环 getEntityById 有界 N+1 → **P2-MA7-005**（watch-only，5 站点同族）——不重复登记。
  - 多账套读路径双计 → **P1-MA2-095**（resolved R1.29）——P1-CK-fin-004/011 为写路径不同控制点（新增，带同族注记）。
  - CANCELLED 死状态 / 红字凭证 isReversed 终态语义 → **P1-MA2-031**（resolved）——修复在位验证通过，不登记。
  - IGNORED 告警闭环（P1-MA2-032）、MAX_RETRY 升级告警（P1-MA4-001）——代码在位（dispatchAbandonmentAlert/dispatchMaxRetryAlert），不登记。
- OA-02（CloseVoucherWriter flush）属期间结账切片（C3.4），仅作背景未复核。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 5 | P1-CK-fin-001..005 |
| P2 | 10 | P2-CK-fin-006..015 |
| P3 | 4 | P3-CK-fin-016..019 |

按维度（主维度计）：D8×6（001/003/004/011/015 及 008 计 D10）、D6×4（002/009/010/018）、D2×3（005/006/013）、D10×2（016/017）、D1×1（007）、D9×2（012/019）、D7×1（014，跨 D2/D7 计 D7 触发面）。（001 跨 D8/D2 计 D8；004 跨 D8/D6 计 D8；005 跨 D10/D2 计 D10。）

移交项：证实 2（P1-CK-sal-003→P1-CK-fin-001、P2-CK-sal-019→P2-CK-fin-006）、finance 侧证伪 1（P2-CK-sal-018，维持 sales 归属）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 30 文件中 26 个逐行深读（引擎 1005 行全读、sweep/异常工作台/GL 映射三链全读、Dispatcher/Provider/Registry/Generator 全读）；平台源码实证 3 处（QueryBean.addOrderField、CrudBizModel 删除钩子链、ITransactionTemplate.afterCommit javadoc）；orm.xml 核对（Voucher versionProp/UK、AccountingPeriod 月度粒度与唯一键、VoucherBillR 索引）；跨域核实 4 点（AcctSchemaResolver null 语义、PurReversalListener 回退目标态、purchase/sales/inventory dispatcher null 消费语义、ProfitLossClosing isReversed 排除式汇总）。
- **未深查**：`erp-fin-web` AMIS view.xml 与后端契约 drift（归 C8.2）；`ErpFinBusinessType` 枚举全值与字典逐项比对（voucher-back-link-patterns 已声明字典与枚举刻意分歧）；`ErpFinPostingMetrics` 环形采样的并发正确性（仅结构扫描）；`IntercompanyVoucherGenerator`/`BudgetVoucherGenerator`（intercompany/budget 切片边缘，仅核对与引擎交点 postingType 语义）；测试代码（103 个测试文件仅用于交叉验证行为语义）；`ErpFinVoucherTemplateBizModel/TemplateLineBizModel`（变更审计 tagSet 裁决在位抽查，未逐钩子核）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-fin-002**（postVoucher 缺平衡校验）——若 AMIS 前端或 xmeta 层存在本代理未发现的保存前平衡拦截（如 voucher save_ 钩子在 xbiz/前端校验器中），降级为「校验位置不当」；后端无守卫的事实本身已确证。
  2. **P1-CK-fin-003**（幂等 null 语义）——影响面依赖「主事务在 doPosting 后失败」的实际频率（purchase P1-CK-pur-002 的 SoD 场景是其确定触发器之一）；若全域 Processor 实际无 doPosting 后置守卫抛错路径（除 pur-002 已登记两处），则触发面收窄至乐观锁冲突，严重性可议。
  3. **P2-CK-fin-004**（ACTIVE 过滤）——`ErpMdAcctSchema.status` 取值域未逐字核对（代码用 `"ACTIVE".equals(s.getStatus())` 字面量；若模型实际以 isActive boolean 承载启用态则过滤条件需相应调整，缺陷本身仍成立——排序逻辑已证 status 字段存在且语义被消费）。
