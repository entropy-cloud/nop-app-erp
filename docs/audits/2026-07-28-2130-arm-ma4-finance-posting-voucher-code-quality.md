# MA4 finance 代码质量审计 — 过账与凭证链路（A4.1a — 代码实现质量）

> Audit Status: closed
> 里程碑：MA4（代码与前端质量层 / 代码实现质量维度）
> 域/功能模块：finance / 过账引擎与凭证链路（A4.1a S 级拆分 1/2）
> 审计 plan：`docs/plans/2026-07-28-2130-2-audit-remediation-ma4-finance-posting-voucher-code-quality.md`
> 来源 finding（运行时复核）：P0-MA2-018 / P1-MA1-016 / P1-MA1-018 / P1-MA1-022 / P1-MA2-002 / P1-MA2-009 / P1-MA2-031 / P1-MA2-032 / P1-MA3-026 / P1-MA3-030 / P1-MA3-031 / P1-MA3-039
> Skill：`docs/skills/code-quality-audit-prompt.md`（7 重点领域 + 严重性指南 P0-P3）
> 审计日期：2026-07-28
> 审计者：主代理（独立子代理已完成草案审查 + 结束审计，见 plan §Draft Review Record / §Closure）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏路径） | **0** | 无即时通道修复（TOCTOU 已登记 P0-MA2-018 deferred，本审计不重复） |
| **P1**（新登记） | **3** | P1-MA4-001（重试耗尽 RETRYING 死状态，业财悬挂）/ P1-MA4-002（测试有效性不足）/ P1-MA4-003（过账链路跨域 daoFor 绕 I\*Biz） |
| **P2**（watch-only） | **2** | P2-MA4-001（可维护性：no-op/常量重复/命名误导/inline ErrorCode）/ P2-MA4-002（自动化防护：R2d 未覆盖 Resolver/Propagator/Helper） |
| MA1/MA2/MA3 finding 运行时复核 | 12 项 | 全部「如登记」无升级；其中 P1-MA2-032 复核时**发现相邻代码路径新缺陷** P1-MA4-001（详见 §6） |

**整体裁决**：**FAIL（有代码实现质量缺陷）**——零 P0（无活跃数据破坏路径；alreadyPosted TOCTOU 是 P0-MA2-018 已登记 deferred，不在本审计重复）。过账链路的核心实现质量在**编排健壮性 / Provider SPI 一致性 / 错误处理规范化**三个面基本扎实（ErpFinPostingProcessor 步骤化 protected 方法 + Registry fail-fast 冲突检测 + 全链路 NopException+ErrorCode + 双 REQUIRES_NEW 异常落库隔离 + O-6 未预期异常归一化），但**失败恢复闭环**存在 1 项 P1 代码缺陷（重试耗尽后死状态无升级）、**测试有效性**存在 1 项 P1 缺陷（断言弱 + 异常/重试路径零覆盖，致多币种 bug 对测试不可见）、**架构边界**存在 1 项 P1 缺陷（6 站点跨域 daoFor 绕 I\*Biz，同 P1-MA1-022 根因在 finance posting 代码投影）。MA2 已知 finding 运行时复核 12 项全部「如 owner doc 声明」无升级，其中 P1-MA2-032（IGNORED 悬挂）复核时发现其相邻代码路径（重试耗尽 RETRYING）有新缺陷 P1-MA4-001。

**裁决分布**：P1-MA4-001 → **MR1**（业财悬挂闭环，与 P1-MA2-032 同型根因）/ P1-MA4-002 → **MR2**（测试质量，MA4 维度）/ P1-MA4-003 → **MR1**（同 P1-MA1-022 一并裁决，不重复计入 MR2）。

---

## 1. 审计范围与方法覆盖矩阵

### 1.1 审计对象（实仓逐项核实，77 源文件中核心组件抽样）

| 组件 | 文件 | 行号 | 审计状态 |
|---|---|---|---|
| 过账编排 Processor | `module-finance/erp-fin-service/.../posting/ErpFinPostingProcessor.java` | process:126-203 / reverseProcess:209-257 / alreadyPosted:472-484 / resolveOpenPeriod:495-512 / persistVoucher:764-846 / buildReversalDraft:725-755 / markOriginalVoucherReversed:909-923 / dispatchReversalEvent:363-388 / recordPostFailure:305-329 / translateFactsForSchema:648-701 / resolveSubjects:562-618 | ✅ |
| 凭证聚合 Facade | `.../service/entity/ErpFinVoucherBizModel.java` | post:67-74（REQUIRES_NEW）/ reverse:76-84（REQUIRES_NEW）/ postVoucher:86-100 / reverseVoucher:102-114 | ✅ |
| Provider SPI | `.../posting/IErpFinAcctDocProvider.java`（getSupportedBusinessTypes / createFacts / isFallback） | 全文 35 行 | ✅ |
| Provider 注册表 | `.../posting/ErpFinAcctDocRegistry.java`（init:45-79 fail-fast / getProvider:81-83） | — | ✅ |
| 默认模板 Provider | `.../posting/provider/ErpFinTemplateAcctDocProvider.java`（createFacts:70-97 / findTemplate:99-129） | — | ✅ |
| VoucherFact 类型 | `.../posting/VoucherFact.java`（单一 amount 字段 :16） | 全文 157 行 | ✅ |
| 兜底重试 | `.../posting/ErpFinDeferredPostingRetryHelper.java`（retry:67-85 / doRetry:87-100 / incrementRetryAndRethrow:129-145） | — | ✅ |
| 兜底重试 batch | `.../_vfs/nop/batch-task/fin/deferred-posting-sweep.batch.xml`（loader filter status=PENDING AND retryCount<3） | 全文 29 行 | ✅ |
| 异常记录器 | `.../posting/ErpFinPostingExceptionRecorder.java`（record:89-128 双 REQUIRES_NEW / dispatchNotify:136-169） | — | ✅ |
| 期末直写路径 | `.../close/CloseVoucherWriter.java`（writeVoucher:61-139 / amountSource=amountFunctional:122-123 / inline ErrorCode.define:82） | — | ✅ |
| ErrorCode 集中表 | `.../posting/ErpFinPostingErrors.java`（18 个 ErrorCode.define erp.err.fin.*） | — | ✅ |
| 状态/配置常量 | `.../service/ErpFinConstants.java`（POSTING_TYPE_*:164-169 / POSTING_EXCEPTION_STATUS_*:199-203） | — | ✅ |
| 测试套件 | `.../posting/TestErpFinPostingService.java`（6 @Test：happy/idempotent/unbalanced/periodClosed/reverse/reverseNotFound） | 全文 353 行 | ✅ |

### 1.2 Skill 维度覆盖（`code-quality-audit-prompt.md` 7 重点领域）

| # | 维度 | 裁决 | 发现 |
|---|------|------|------|
| 1 | 架构和边界完整性 | ⚠️(P1) | 过账链路 6 站点跨域 `daoFor(ErpMd*)` 绕 I\*Biz（P1-MA4-003，同 P1-MA1-022 根因）；CloseVoucherWriter 绕 Facade 为文档化设计选择非违规；生成物零手编 ✅ |
| 2 | 核心实现正确性 | ⚠️(P1) | 重试耗尽 → RETRYING 死状态无升级（P1-MA4-001）；alreadyPosted TOCTOU 如 P0-MA2-018 登记；事务边界（@BizMutation + REQUIRES_NEW Facade + @SingleSession 编排）正确 ✅ |
| 3 | 类型和契约质量 | ⚠️(P1 维持) | VoucherFact 单一 amount 字段如 P1-MA2-002/009 登记；postingType 三源局部常量重复如 P1-MA3-026 登记（P2-MA4-001）；Provider SPI 契约一致 ✅ |
| 4 | 错误处理和操作安全 | ✅(P3) | 全链路 NopException+ErrorCode（erp.err.fin.*）+ O-6 未预期异常归一化 + 上下文齐全（traceId/billHeadCode/businessType/failedStage）；CloseVoucherWriter inline ErrorCode.define 未集中（P3，P2-MA4-001） |
| 5 | 测试有效性 | ⚠️(P1) | 断言弱（仅 count lines + total debit/credit）+ 异常/重试路径零覆盖 + 无多币种/IGNORED-悬挂/重试耗尽/红冲红字凭证负向测试（P1-MA4-002） |
| 6 | 可维护性和未来变更风险 | ⚠️(P2) | ErpFinPostingProcessor 944 行（步骤化良好但体量上限）；no-op setAcctSchemaId catch 块 / 局部常量重复 / 命名误导 incrementRetryAndRethrow（P2-MA4-001） |
| 7 | 自动化和防护覆盖 | ⚠️(P2) | compliance checker R2d 未覆盖 *Resolver/*Propagator/*Helper；无 TOCTOU/重试耗尽静态规则（P2-MA4-002） |

---

## 2. 重点领域逐项审查结果

### 2.1 领域「架构和边界完整性」— ⚠️(P1)

**核查项**：Provider SPI 实现是否经 I\*Biz 接口（非 daoFor 直访）/ 凭证写入是否经 Facade / 生成物是否手编 / P1-MA1-016 运行时状态。

**证据**：
- **跨域 daoFor(ErpMd\*) 绕 I\*Biz（6 站点 + 1 内部不一致）**：grep `daoFor(Erp` 于 `posting/` + `close/` 命中 6 处 finance→master-data 只读直访：
  - `SchemaPropagator.java:107,112` → `daoFor(ErpMdAcctSchema.class)`（账套传播只读）
  - `ExpenseClaimPostingDispatcher.java:95` → `daoFor(ErpMdEmployee.class)`（员工解析只读）
  - `ErpFinGlMappingResolver.java:219` → `daoFor(ErpMdMaterial.class).getEntityById`（GL 映射维度只读）
  - `EmployeeAdvancePostingDispatcher.java:151` → `daoFor(ErpMdEmployee.class)`（员工解析只读）
  - `ErpFinTransferPriceResolver.java:167` → `daoFor(ErpMdMaterial.class).getEntityById`（转移定价只读）
  - `ErpFinPostingProcessor.java:687` → `daoFor(ErpMdSubject.class).getEntityById`（translateFactsForSchema 跨账套科目翻译只读）
- **内部不一致**：`ErpFinPostingProcessor` 同一类内 `resolveSubjects:595` 经 `bizObjectManager.getBizObject(...).asProxy()`（IErpMdSubjectBiz I\*Biz 管道）查科目，但 `translateFactsForSchema:687` 改用 `daoFor(ErpMdSubject.class).getEntityById` 直访——同类操作两种模式，可维护性缺陷。
- **P1-MA1-016 运行时状态**：`ErpFinAccountingPeriodProcessor.reverseDepreciation` 跨域 DAO（finance→assets）如 MA1 登记，无新代码层缺陷（该文件实现质量归 A4.1b 复核）。
- **凭证写入经 Facade**：8+ 域过账调用方经 `IErpFinVoucherBiz.post/reverse` Facade（A3.6 已确认 9 域 11 调用方签名一致）✅。
- **CloseVoucherWriter 绕 Facade**：期末结账凭证（损益结转/汇兑重估）直接持久化 ErpFinVoucher+Line+BillR，绕 `IErpFinVoucherBiz.post`——经 Javadoc:18-24 文档化为**设计选择**（无 PERIOD_CLOSE/FX Provider + post 会触发 ArApItem 生成），结构与引擎产出一致（供 `voucherBiz.reverse` 红冲）。非违规，登记为文档化例外 ✅。
- **生成物零手编**：posting/ 与 close/ 下全部为手写非 `_gen` 文件；未发现 `_` 前缀文件手编 ✅。

**裁决**：⚠️(P1) — 6 站点跨域 daoFor 绕 I\*Biz 是 **P1-MA1-022 根因在 finance posting 代码的投影**（P1-MA1-022 原列举 pur/sal/ast/inv/mnt/prj/qa/drp/aps 9 域，未显式枚举 finance posting helpers）。登记 P1-MA4-003 交叉引用 P1-MA1-022，MR1 一并裁决（不重复计入 MR2）。

### 2.2 领域「核心实现正确性」— ⚠️(P1)

**核查项**：PostingProcessor 事务边界健壮性 / alreadyPosted 幂等 TOCTOU / 兜底重试异常吞咽与悬挂 / persistVoucher 多币种折算 / CloseVoucherWriter 是否绕过编排。

**证据**：
- **【新缺陷 P1-MA4-001】重试耗尽 RETRYING 死状态无升级**：`ErpFinDeferredPostingRetryHelper.incrementRetryAndRethrow:133-136` 当 `retryCount >= MAX_RETRY(3)` 时 `ex.setStatus(POSTING_EXCEPTION_STATUS_RETRYING)`；但兜底扫描 loader（`deferred-posting-sweep.batch.xml:13-17`）filter 为 `status=PENDING AND retryCount<3`——**RETRYING 状态记录永不被 loader 重新选中**。结果：永久性失败（如科目/模板配置缺失、Provider 抛固定错误）3 次重试后搁浅于 RETRYING，既不重试也不升级 MANUAL/IGNORED，也无告警（`ErpFinPostingExceptionRecorder.dispatchNotify` 仅在首次 record 时派发，重试耗尽无二次告警）。状态名 "RETRYING" 双重误导（实为「耗尽终态」）。与 P1-MA2-032（IGNORED 悬挂）是**相邻代码路径同型根因**（业财不一致悬挂），但 P1-MA2-032 审的是 IGNORED 显式放弃态，本缺陷审的是 MAX_RETRY 隐式耗尽态——MA2 未覆盖。
- **alreadyPosted TOCTOU**（`:472-484`）：pre-check query（按 billR 反查 POSTED+未冲销凭证）非 SELECT FOR UPDATE，配合 `IErpFinVoucherBiz.post` REQUIRES_NEW 独立事务——并发 post/重试/人工可双 INSERT。如 **P0-MA2-018 登记**（deferred plan 方向 A/B/C/D 维持，A2.18 多公司维度复核不重新打开）。无新代码层缺陷。
- **事务边界健壮性**：`ErpFinVoucherBizModel.post:71 / reverse:79` 显式 `@Transactional(REQUIRES_NEW)` 钉 Facade（跨域失败隔离，processor-extension-pattern.md 硬规则 1）；编排层 `@SingleSession` 承接 ORM Session（作用域精确覆盖编排方法）；`reverseProcess` 不叠加 REQUIRES_NEW（跟随调用方 @BizMutation）——对齐 owner doc `posting.md §裁决3` + 解决 P1-MA3-030 doc↔code 冲突 ✅。
- **persistVoucher 多币种折算**（`:818-819`）：`line.setAmountSource(amt)` + `line.setAmountFunctional(amt)`——两字段同值（源币种金额），未按 `ctx.exchangeRate` 折算本位币。如 **P1-MA3-039 登记**（MR1 代码侧）。`CloseVoucherWriter:122-123` 同型。运行时确认 drift 活跃。
- **CloseVoucherWriter 是否绕过编排**：期末凭证不经 PostingProcessor 编排（无 FactsValidator 链 / 无 alreadyPosted 幂等 / 无 Provider 路由），但经文档化设计选择（见 §2.1）+ 借贷平衡校验（`:81-85`）+ 结构与引擎一致 ✅。非缺陷。
- **incrementRetryAndRethrow 命名误导**（`:129-145`）：方法名含 "Rethrow" 但**不 rethrow**（仅 catch 持久化异常 + LOG.warn，原异常 `e` 被吞）——batch 语义下可接受（retry() 返回 false，batch skipPolicy 继续其他记录），但命名误导维护者。P2-MA4-001。
- **doRetry null-event 静默 RETRIED**（`:87-100`）：`rebuildEvent` 返回 null（无 eventData）时 doRetry no-op，随后 `markRetried` 标 RETRIED——无实际重试却标成功。边缘场景（NORMAL posting 总有 eventData，recordPostFailure:324-325 序列化 billData），实际触发面窄。P2-MA4-001。

**裁决**：⚠️(P1) — P1-MA4-001（重试耗尽死状态）是新发现的核心实现正确性 + 错误处理闭环缺陷；alreadyPosted TOCTOU / persistVoucher 多币种如既有 P0/P1 登记，运行时确认无升级。

### 2.3 领域「类型和契约质量」— ⚠️(P1 维持)

**核查项**：VoucherFact 类型安全 / Provider SPI 参数返回契约一致性 / postingType 三源不一致。

**证据**：
- **VoucherFact 单一 amount 字段**（`VoucherFact.java:16`）：无 amountSource/amountFunctional 分离，Provider 写入源币种金额，引擎 persistVoucher 无法据此折算本位币（两字段同值，§2.2）。如 **P1-MA2-002/009 登记**（MR1）。无新代码层缺陷。
- **Provider SPI 契约一致性**：`IErpFinAcctDocProvider` 三方法（getSupportedBusinessTypes / createFacts / isFallback）契约清晰；`CommitmentAcctDocProvider` / `IntercompanyAcctDocProvider` 返回空 `getSupportedBusinessTypes`（文档化：不走 Provider 路由，凭证由专属 Generator 写入）——如 **P1-MA3-031 登记**（doc-side 矛盾 budget.md vs posting.md，MR2 文档类）。Registry fail-fast 检测重复非默认 Provider（`ErpFinAcctDocRegistry.init:54-60`）✅。
- **postingType 三源不一致**：`ErpFinPostingProcessor:73-74` 局部声明 `POSTING_TYPE_NORMAL="NORMAL" / POSTING_TYPE_REVERSAL="REVERSAL"`，与 `ErpFinConstants:164-165` 同值常量重复（两处真相源）。如 **P1-MA3-026 登记**（三源：局部常量 / ErpFinConstants / dict）。无升级；局部常量重复归 P2-MA4-001 可维护性。

**裁决**：⚠️(P1 维持) — VoucherFact 单 amount + postingType 三源如 MA2/MA3 登记，运行时确认；无新类型/契约缺陷。

### 2.4 领域「错误处理和操作安全」— ✅(P3)

**核查项**：过账链路异常是否全扩展 NopException + ErrorCode（erp.err.fin.*）/ 异常上下文是否齐全 / MANUAL_POST 与 AUTO_POST 错误传播差异。

**证据**：
- **全链路 NopException + ErrorCode**：grep `extends RuntimeException` 于 `posting/`+`close/` = **0 命中**（R4 合规）。`ErpFinPostingErrors` 集中定义 18 个 `ErrorCode.define("erp.err.fin.*")`；所有 throw 点使用 NopException + .param() 携带上下文 ✅。
- **O-6 未预期异常归一化**：`ErpFinPostingProcessor.recordPostFailure:314-318` / `recordReverseFailure:340-344` 对非 NopException 统一记录为 `ERR_POSTING_UNEXPECTED_FAILURE`，保证 NPE/IllegalState 等不丢失于 posted=false 盲区 ✅。
- **异常上下文齐全**：record 携带 traceId/billHeadCode/businessType/postingType/errorCode/errorMessage/failedStage/voucherDate/orgId/acctSchemaId/currencyId/exchangeRate/eventData——重试可完整重建 PostingEvent ✅。
- **MANUAL_POST vs AUTO_POST 错误传播**：AUTO 路径（PostingProcessor.process）失败经 catch → recordPostFailure 落异常工作台 + 原异常上抛（跨域调用方 try/catch 可见）；CloseVoucherWriter（期末半自动）失败直接 throw NopException（无异常工作台记录，但期末结账流程捕获并中止结账）。差异为设计（期末凭证失败应中止结账而非进重试队列）✅。
- **CloseVoucherWriter:82 inline ErrorCode.define**：`ErrorCode.define("erp.err.fin.period-close.unbalanced", ...)` 内联定义而非集中登记于 ErpFinErrors/ErpFinPostingErrors——错误码本身合规（erp.err.fin.* 前缀 + 中文描述），仅集中性缺陷。P3（归 P2-MA4-001）。

**裁决**：✅(P3) — 错误处理规范化扎实（全 NopException + O-6 归一化 + 上下文齐全）；仅 CloseVoucherWriter inline ErrorCode 集中性 P3。

### 2.5 领域「测试有效性」— ⚠️(P1)

**核查项**：异常路径覆盖（非仅黄金路径）+ 断言强度（是否仅断言 posted=true 还是校验凭证行数值/业财回链）/ P1-MA2-032 测试覆盖复核。

**证据**（`TestErpFinPostingService.java` 6 @Test + `TestErpFinPostingExceptionNotify` / `TestErpFinPostingExceptionWorkbench` 抽样）：
- **黄金路径覆盖良好**：happy（凭证+3 行+回链+POSTED+借贷平衡）/ idempotent（重复过账返回 null）/ unbalanced（拒绝）/ periodClosed（拒绝）/ reverse（红字凭证+原凭证 isReversed+净额为 0+双向回链）/ reverseNotFound（拒绝）✅。
- **【新缺陷 P1-MA4-002】断言强度弱**：happy/reverse 仅断言 `countLines`（行数）+ `totalDebit/totalCredit`（凭证头合计），**未校验行级字段**（`ErpFinVoucherLine.amountSource` / `amountFunctional` / `exchangeRate` / `debitAmount` / `creditAmount`）。后果：persistVoucher 多币种 bug（amountSource=amountFunctional=amt，P1-MA3-039）**对测试不可见**——即便交换率≠ONE，测试仍因 totalDebit==totalCredit 通过。无任何断言验证行级金额正确性。
- **异常/重试路径零覆盖**：
  - 无多币种过账测试（所有 event `exchangeRate=BigDecimal.ONE`）——P1-MA2-002/009/MA3-039 不可见。
  - 无 IGNORED-悬挂测试（P1-MA2-032）+ 无重试耗尽 RETRYING 死状态测试（P1-MA4-001）——`TestErpFinPostingExceptionWorkbench` 覆盖工作台 CRUD 但不覆盖重试状态机闭环。
  - 无「红冲红字凭证」负向测试（P2-MA2-033 已登记）——`findAllPostedVouchers` 过滤 postingType=REVERSAL 阻断无限循环，但无回归保护测试。
  - 无 deferred-retry 成功/失败路径测试（`ErpFinDeferredPostingRetryHelper.retry` 零直接测试）。
- **P1-MA2-032 测试覆盖复核**：如 MA2 登记（告警通道仅日志/通知，无强制处置门控；期末结账前置检查 PENDING 间接兜底），无升级；本审计补充发现重试耗尽路径（P1-MA4-001）同样零测试覆盖。

**裁决**：⚠️(P1) — P1-MA4-002（断言弱 + 异常/重试路径零覆盖，致多币种/悬挂 bug 对测试不可见）。测试存在性合格但**有效性不足**——黄金路径覆盖良好，但断言强度与异常路径覆盖存在系统性空洞。

### 2.6 领域「可维护性和未来变更风险」— ⚠️(P2)

**核查项**：过账链路复杂度热点 / Provider 实现重复模式 / 凭证模板配置可维护性。

**证据**：
- **ErpFinPostingProcessor 944 行**：单类体量在上限，但已步骤化为 protected 单一职责方法（process/reverseProcess 编排 + 各 step protected）——派生覆盖友好，可维护性可接受，但持续增长需关注。
- **【P2-MA4-001】可维护性热点合并**：
  - `ErpFinPostingProcessor:198` catch 块 `event.setAcctSchemaId(event.getAcctSchemaId())` 是 **no-op**（设为自身）——应恢复 `originalSchemaId`（对齐成功路径 :189）。失败时 event 残留循环内最后 schemaId。影响低（event 是瞬态 DTO + 异常上抛），但代码明显错误/冗余。
  - 局部常量重复：`ErpFinPostingProcessor:68-77` 声明 `VOUCHER_STATUS_DRAFT/POSTED` + `POSTING_TYPE_NORMAL/REVERSAL` 重复 `ErpFinConstants:160-165`（两处真相源，P1-MA3-026 三源之一）。
  - `incrementRetryAndRethrow` 命名误导（不 rethrow，§2.2）。
  - `CloseVoucherWriter:82` inline ErrorCode.define 未集中（§2.4）。
- **Provider 实现重复模式**：~20+ Provider/Dispatcher（Notes/ExpenseClaim/EmployeeAdvance/CreditFacility/BankRecon 等）各自重复金额提取/科目解析逻辑；`ErpFinTemplateAcctDocProvider` 提供良好共享基线但域 Dispatcher 未充分复用。P2 可维护性。
- **凭证模板配置可维护性**：DB 驱动 + 占位符 + accountKey + 版本管理（validFrom/validTo）+ 多账套（acctSchemaId 或 null 通配）——可维护性良好 ✅。

**裁决**：⚠️(P2) — P2-MA4-001（4 项可维护性热点合并）watch-only，MR2 顺手收敛。

### 2.7 领域「自动化和防护覆盖」— ⚠️(P2)

**核查项**：过账链路是否有 compliance checker 规则守护（R8 Processor 无 xbiz / R2 daoFor 跨域）/ 是否有测试门控防止回归。

**证据**：
- **【P2-MA4-002】compliance checker 覆盖缺口**：`nop-compliance-checker.sh` R2（跨域 daoFor）的 R2a/R2b 扫描 BizModel，R2d 扫描 `*Processor.java`/`*Dispatcher.java`/`*Engine.java`——**未覆盖 `*Resolver` / `*Propagator` / `*Helper`**。故 `ErpFinGlMappingResolver:219`（daoFor ErpMdMaterial）/ `ErpFinTransferPriceResolver:167`（daoFor ErpMdMaterial）/ `SchemaPropagator:107,112`（daoFor ErpMdAcctSchema）的 finance 内部跨域 daoFor **无静态守卫**。R2d 命中的 *Dispatcher（ExpenseClaim/EmployeeAdvance/Notes）有覆盖。R8（Processor 无 xbiz）不适用（posting Provider/Dispatcher 非 xbiz 路由实体）。
- **TOCTOU / 重试耗尽可能无静态规则**：alreadyPosted TOCTOU（P0-MA2-018）与重试耗尽死状态（P1-MA4-001）是运行时行为模式，静态 checker 难以捕获——属测试门控职责。
- **测试门控缺口**：多币种回归（P1-MA3-039）无测试门控（P1-MA4-002）；重试耗尽死状态（P1-MA4-001）无测试门控。

**裁决**：⚠️(P2) — P2-MA4-002（checker R2d 未覆盖 Resolver/Propagator/Helper + 测试门控缺口）watch-only，MR2 顺手扩展 R2d 文件名模式或补测试。

---

## 3. P1 finding 清单（按严重性 + 目标 MR 排序）

### P1-MA4-001 兜底重试 MAX_RETRY 耗尽 → RETRYING 死状态无升级（业财悬挂闭环缺失）

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——业财不一致悬挂，但需永久性失败前置 + 非正常路径） |
| 目标 MR | **MR1**（业务正确性：业财悬挂，与 P1-MA2-032 同型根因；R4.1 可裁决） |
| 文件 / 行 | `module-finance/erp-fin-service/.../posting/ErpFinDeferredPostingRetryHelper.java:129-145`（incrementRetryAndRethrow）+ `.../_vfs/nop/batch-task/fin/deferred-posting-sweep.batch.xml:13-17`（loader filter） |
| 缺陷描述 | `incrementRetryAndRethrow:134-136` 当 `retryCount >= MAX_RETRY(3)` 设 `status=RETRYING`；但 sweep loader filter 为 `status=PENDING AND retryCount<3`——RETRYING 记录永不被重新选中。3 次重试耗尽后记录搁浅于 RETRYING：不重试、不升级 MANUAL/IGNORED、无二次告警（dispatchNotify 仅首次 record 时派发）。状态名 "RETRYING" 误导（实为耗尽终态）。 |
| 影响 | 永久性过账失败（科目/模板配置缺失、Provider 固定抛错、data 序列化损坏）→ 业务侧 `posted=false` 永久悬挂 + GL 缺凭证 + 无人工处置入口（不同于 P1-MA2-032 的显式 IGNORED，本缺陷是隐式 MAX_RETRY 耗尽）。期末结账前置检查仅扫 PENDING（不扫 RETRYING），间接兜底失效。 |
| 修复方向 | MR1 裁决——方案 A（推荐）：retryCount>=MAX_RETRY 时设 `status=MANUAL`（或新增 FAILED 终态）+ 派发 `IErpSysNotificationBiz` 告警 + owner doc `posting-log.md §过账异常处置` 标注「MAX_RETRY 耗尽 → MANUAL + 告警」；方案 B：sweep loader 扩展至 RETRYING+retryCount>=MAX_RETRY 并设上限退出（仍需告警）。触及会计保护区域，修复须独立 plan-audit + 人工确认。 |

### P1-MA4-002 过账链路测试有效性不足（断言弱 + 异常/重试路径零覆盖）

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——测试空洞致既有 bug 不可见 + 无回归防护） |
| 目标 MR | **MR2**（测试质量，MA4「测试有效性」维度；与 A5.1 测试覆盖深度统计互补不重叠——本项审断言强度+异常路径，A5.1 审覆盖深度数值） |
| 文件 / 行 | `module-finance/erp-fin-service/.../posting/TestErpFinPostingService.java:71-103,174-226`（happy/reverse 断言）+ 全文件（缺异常/重试/多币种测试） |
| 缺陷描述 | (a) 断言强度弱：happy/reverse 仅断言 `countLines` + `totalDebit/totalCredit`，未校验行级 `amountSource/amountFunctional/exchangeRate/debitAmount/creditAmount`——persistVoucher 多币种 bug（P1-MA3-039 amountSource=amountFunctional）对测试不可见。(b) 异常/重试路径零覆盖：无多币种（exchangeRate 恒 ONE）/ 无 IGNORED-悬挂（P1-MA2-032）/ 无重试耗尽 RETRYING 死状态（P1-MA4-001）/ 无红冲红字凭证负向（P2-MA2-033）/ 无 deferred-retry 成功失败路径（ErpFinDeferredPostingRetryHelper.retry 零直接测试）。 |
| 影响 | 多币种 bug P1-MA3-039 + 重试耗尽死状态 P1-MA4-001 + IGNORED 悬挂 P1-MA2-032 三类缺陷均无测试门控；未来结构性变更（如 VoucherFact 增双金额字段、persistVoucher 折算逻辑）无回归保护。 |
| 修复方向 | MR2——补：(1) 多币种过账 E2E（exchangeRate≠ONE + 行级 amountSource≠amountFunctional 断言）；(2) 重试耗尽 → MANUAL/告警断言（依赖 P1-MA4-001 修复）；(3) 红冲红字凭证负向（assertThrows ERR_REVERSE_SOURCE_NOT_FOUND，闭合 P2-MA2-033）；(4) deferred-retry 成功（rebuildEvent→post→RETRIED）+ 失败（retryCount 递增）路径测试。 |

### P1-MA4-003 过账链路跨域 daoFor(ErpMd*) 绕 I*Biz（同 P1-MA1-022 根因在 finance posting 投影）

| 属性 | 值 |
|---|---|
| 严重性 | **P1**（major——架构边界违规，read-only 跨域直访绕 I\*Biz 管道） |
| 目标 MR | **MR1**（同 P1-MA1-022 一并裁决，**不重复计入 MR2**——同根因 master-data/finance/inventory I\*Biz 补便捷只读方法后迁移） |
| 文件 / 行 | `posting/SchemaPropagator.java:107,112` / `posting/ExpenseClaimPostingDispatcher.java:95` / `posting/ErpFinGlMappingResolver.java:219` / `posting/EmployeeAdvancePostingDispatcher.java:151` / `posting/ErpFinTransferPriceResolver.java:167` / `posting/ErpFinPostingProcessor.java:687`（+ 内部不一致 :595 用 I\*Biz vs :687 用 daoFor） |
| 缺陷描述 | 6 站点 finance→master-data 只读 `daoFor(ErpMdAcctSchema/ErpMdEmployee/ErpMdMaterial/ErpMdSubject)` 直访，违反 AGENTS.md「跨实体访问应通过 I\*Biz 接口」+ data-dependency-matrix.md §5.3。与 P1-MA1-022（pur/sal/ast/inv/mnt/prj/qa/drp/aps 9 域同型）同根因，本批是其在 finance posting helpers 的投影（P1-MA1-022 未显式枚举 finance posting）。ErpFinPostingProcessor 同类内 :595（IErpMdSubjectBiz）vs :687（daoFor ErpMdSubject）不一致加剧可维护性风险。 |
| 影响 | 架构边界侵蚀（read-only，无活跃数据破坏）；master-data 实体变更时 finance posting 直访点不受 I\*Biz 契约保护。 |
| 修复方向 | MR1——同 P1-MA1-022 方案 A（master-data I\*Biz 补便捷只读方法后迁移 6 站点）或方案 B（永久接受为 Helper 合法模式，登记 posting-exemptions.md）。ErpFinPostingProcessor:687 改用 :595 同型 bizObjectManager I\*Biz 管道消除内部不一致。 |

---

## 4. P2 finding 清单（watch-only）

| Finding ID | 描述 | 处置 |
|---|---|---|
| `P2-MA4-001` | 可维护性热点合并（4 项）：(a) `ErpFinPostingProcessor:198` catch 块 no-op `event.setAcctSchemaId(event.getAcctSchemaId())`（应恢复 originalSchemaId）；(b) `ErpFinPostingProcessor:68-77` 局部常量重复 `ErpFinConstants:160-165`（VOUCHER_STATUS_*/POSTING_TYPE_*，P1-MA3-026 三源之一）；(c) `ErpFinDeferredPostingRetryHelper:129` `incrementRetryAndRethrow` 命名误导（不 rethrow）；(d) `CloseVoucherWriter:82` inline `ErrorCode.define("erp.err.fin.period-close.unbalanced")` 未集中登记于 ErpFinErrors/ErpFinPostingErrors。 | watch-only，MR2 顺手收敛（与 P1-MA3-026 / P1-MA4-001 修复时一并） |
| `P2-MA4-002` | 自动化防护缺口：compliance checker R2d 覆盖 `*Processor/*Dispatcher/*Engine` 但未覆盖 `*Resolver/*Propagator/*Helper` → finance 内部跨域 daoFor(ErpMd*) 在 GlMappingResolver/TransferPriceResolver/SchemaPropagator 无静态守卫（R2d 命中的 *Dispatcher 有覆盖）；TOCTOU/重试耗尽为运行时模式无静态规则（归测试门控）。 | watch-only，MR2 顺手扩展 R2d 文件名模式或补测试门控 |

---

## 5. 与既有 P1 交叉去重

| 本审计 Finding | 既有 Finding | 关系 | 去重裁决 |
|---|---|---|---|
| P1-MA4-001 | P1-MA2-032（IGNORED 悬挂） | 相邻代码路径同型根因（业财悬挂），但 MA2 审 IGNORED 显式放弃态、本审 MAX_RETRY 隐式耗尽态——**不重叠** | 独立登记 P1-MA4-001（MR1，与 P1-MA2-032 协同修复） |
| P1-MA4-002 | A5.1（todo，测试覆盖深度） | 互补不重叠——MA4 审断言强度+异常路径，A5.1 审覆盖深度数值 | 独立登记 P1-MA4-002（MR2） |
| P1-MA4-003 | P1-MA1-022（9 域跨域 daoFor） | **同根因在 finance posting 投影** | 独立登记但**不重复计入 MR2**（MR1 同 P1-MA1-022 一并裁决） |
| P2-MA4-001 (b) | P1-MA3-026（postingType 三源） | 子例（局部常量重复是三源之一） | P2 watch-only，MR2 与 P1-MA3-026 协同 |

---

## 6. MA1/MA2/MA3 已知 finding 运行时复核（12 项）

| Finding ID | 运行时状态 | 裁决 |
|---|---|---|
| `P0-MA2-018` | 如 MA2 登记（alreadyPosted:472-484 TOCTOU pre-check + billR 无 UK + REQUIRES_NEW 隔离；deferred plan 方向 A/B/C/D 维持，A2.18 多公司维度复核不重新打开） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA1-016` | 如 MA1 登记（ErpFinAccountingPeriodProcessor.reverseDepreciation 跨域 DAO finance→assets；实现质量归 A4.1b 复核） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA1-018` | 如 MA1 登记（ErpFinBusinessType enum↔dict 4 项漂移；代码以 enum.name() 持久化） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA1-022` | 如 MA1 登记 + **本审计补充 finance posting 6 站点投影**（P1-MA4-003） | **如 owner doc 声明 + 发现 finance posting 投影新站点**（P1-MA4-003） |
| `P1-MA2-002` | 如 MA2 登记（VoucherFact 单一 amount 字段；P2P 多币种未验证） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-009` | 如 MA2 登记（VoucherFact 单一 amount；O2C 收款核销汇兑损益未实现） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-031` | 如 MA2 登记（DRAFT→CANCELLED 无 action + 红字凭证终态归属未定义；useLogicalDelete 承载废弃） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA2-032` | 如 MA2 登记（IGNORED 显式放弃态悬挂）+ **复核时发现相邻代码路径新缺陷**：MAX_RETRY 隐式耗尽 RETRYING 死状态（P1-MA4-001） | **如 owner doc 声明 + 发现相邻路径新缺陷**（P1-MA4-001） |
| `P1-MA3-026` | 如 MA3 登记（postingType 三源：ErpFinPostingProcessor:73-74 局部 / ErpFinConstants:164-165 / dict）+ 局部常量重复归 P2-MA4-001 | **如 owner doc 声明，无新代码层缺陷**（局部重复→P2） |
| `P1-MA3-030` | 如 MA3 登记——裁决3 已落实：reverse 跟随 @BizMutation 不叠加 REQUIRES_NEW（ErpFinVoucherBizModel.reverse:79 叠加 REQUIRES_NEW 为 O-7 对齐 post 的事务边界声明，与裁决3 的「调用方 @BizMutation 承接」一致——Facade 钉 REQUIRES_NEW 是更强隔离，非冲突） | **如 owner doc 声明（裁决3 已解决 doc↔code 冲突），无新代码层缺陷** |
| `P1-MA3-031` | 如 MA3 登记（CommitmentAcctDocProvider budget.md vs posting.md 矛盾，doc-side；getSupportedBusinessTypes 返回空集文档化） | **如 owner doc 声明，无新代码层缺陷** |
| `P1-MA3-039` | 如 MA3 登记 + 运行时确认 drift 活跃：persistVoucher:818-819 + CloseVoucherWriter:122-123 `amountSource=amountFunctional=amt`（未按 exchangeRate 折算）；**对测试不可见**（P1-MA4-002） | **如 owner doc 声明，运行时确认 drift 活跃 + 测试不可见** |

---

## 7. 剩余风险与交接

- **P1-MA4-001 修复前**：永久性过账失败 3 次重试后静默搁浅 RETRYING，需运营手工扫 `status=RETRYING AND retryCount>=3` 处置（无自动告警）。MR1 修复后闭环。
- **P1-MA3-039 修复前**：多币种场景 GL 行级本位币金额错误（amountFunctional=源币种金额），单币种（exchangeRate=ONE）场景无影响。MR1 修复（VoucherFact 增双金额字段 + persistVoucher/CloseVoucherWriter 折算）。
- **交接 A4.1b**：A4.1b（预算/AR-AP/成本/期间代码质量）执行时复核——(1) `ErpFinArApItemGenerator` 实现质量（本审计仅审其在过账链路的调用点 `ErpFinPostingProcessor:182-183`）；(2) `CloseVoucherWriter` 期间结账侧调用点（ProfitLossClosingService/ExchangeRevaluationService）的错误传播；(3) P1-MA4-003 同型 daoFor 是否在 A4.1b 范围文件复现。
- **交接 A5.1**：P1-MA4-002 的测试覆盖深度数值统计（finance 64 测试 / 137 mutation 比 0.47）归 A5.1 系统化；本审计仅审断言强度+异常路径。
- **交接 A4.6**：finance view.xml vs 后端契约 drift（如 voucher 列表页字段）归 A4.6；本审计不审前端消费。

## 8. 裁决

**Verdict: FAIL（有代码实现质量缺陷）**——零 P0（无活跃数据破坏路径；TOCTOU 是 P0-MA2-018 已登记 deferred）。过账链路在编排健壮性 / Provider SPI 一致性 / 错误处理规范化三面扎实，但失败恢复闭环（P1-MA4-001）、测试有效性（P1-MA4-002）、架构边界（P1-MA4-003）三项 P1 缺陷需 MR1/MR2 修复。MA1/MA2/MA3 已知 finding 运行时复核 12 项全部「如登记」无升级，其中 P1-MA2-032 复核发现相邻代码路径新缺陷 P1-MA4-001。roadmap A4.1a 推进至 done（待独立 closure audit）。
