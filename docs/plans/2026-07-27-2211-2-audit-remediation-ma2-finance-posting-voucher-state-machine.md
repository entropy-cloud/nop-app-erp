# 2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine MA2 finance 状态机审查 — 过账与凭证（A2.5a）

> Plan Status: active
> Mission: audit-remediation
> Work Item: A2.5a finance 状态机审查 — 过账与凭证（S 级拆分 1/3）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.5a）
> Related: `docs/plans/2026-07-27-2211-1-audit-remediation-ma2-inventory-costing-consistency.md`（A2.4，库存过账通道产出凭证与三方对账金额一致性的交接）；`docs/plans/2026-07-27-1949-3-audit-remediation-ma2-period-close-e2e.md`（A2.3，期末结账业务类型映射 + 反结账红冲 + P1-MA2-021 CLOSED_FINAL 凭证锁定交接 A2.5a 凭证侧守卫）；`docs/plans/2026-07-27-1949-1-audit-remediation-ma2-procure-to-pay-e2e.md`（P1-MA2-001 GRNI 自动冲回 / P1-MA2-002 多币种 P2P 本位币凭证路径，过账引擎 scope）；`docs/plans/2026-07-27-1949-2-audit-remediation-ma2-order-to-cash-e2e.md`（P1-MA2-009 多币种 O2C + 收款核销汇兑损益，过账引擎 scope）；`docs/plans/2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier.md`（P1-MA1-018 enum↔dict 漂移 4 项含 PERIOD_CLOSE/EXCHANGE_GAIN_LOSS + P2-MA1-019 fromCode 抛 IllegalArgumentException，待 MR1）；`docs/plans/2026-07-27-1430-1-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`（P0-MA1-021 已修复，本审计复核过账引擎 reversal 路径）；`docs/plans/2026-07-27-1949-arm-fix-p0-ma2-016-fx-gain-loss-pl-closing.md`（P0-MA2-016 fix plan，EXCHANGE_GAIN_LOSS 业务类型，本审计复核过账引擎持久化 `enum.name()` 的运行时影响）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/finance/posting.md`+`posting-log.md`+`state-machine.md`+`gl-mapping-rules.md`（owner doc）
> Audit: required

## Current Baseline

过账与凭证是 ERP 业财一体的枢纽：业务单据 approve 触发过账引擎产出凭证（`ErpFinVoucher` + `ErpFinVoucherLine` + `ErpFinVoucherBillR` 回链），凭证经 `ErpFinPostingProcessor` 装配科目/方向/金额并校验借贷平衡 + 期间开放，`docStatus` 翻 POSTED；业务侧 `posted=true`。红冲经 `IErpFinVoucherBiz.reverse` 生成红字凭证 + 原凭证 `isReversed=true` + 派发 `VoucherReversedEvent` 通知业务域回写。owner doc `docs/design/finance/posting.md`（475+ 行）定义三层模型 + PostingEvent 契约 + 幂等 + businessType 映射 + 可插拔 Provider 机制 + 科目映射 + 冲销机制 + 反写契约；`state-machine.md` 定义两类状态机（会计凭证状态机 + 会计期间状态机），A2.5a 仅覆盖**会计凭证状态机**（A2.5b 覆盖期间状态机，A2.5c 覆盖 AR/AP 核销）。

实时仓库已落地的过账与凭证实现（逐项核实）：

- **凭证聚合 BizModel Facade**（`module-finance/erp-fin-service/.../service/entity/ErpFinVoucherBizModel.java`）：`post():67-74`（REQUIRES_NEW + @BizAudit）+ `reverse():76-84`（REQUIRES_NEW）+ `postVoucher():86-100`（DRAFT→POSTED UI 按钮）+ `reverseVoucher():102-114`（置 `isReversed=true`）+ `previewReverseVoucher():120-149`（只读预览）。I*Biz 接口 `IErpFinVoucherBiz`（`module-finance/erp-fin-dao/.../biz/`）。
- **过账编排器 Processor**（`ErpFinPostingProcessor.java`）：`process():125-203` 正向过账；`reverseProcess():208-257` 红冲；`alreadyPosted():472-484` 幂等（**显式排除 `isReversed=true`**，允许同 billCode 重新过账）；`resolveProvider():486-493`；`resolveOpenPeriod():495-512`（期间门控，**P1-MA2-021**：postVoucher/reverseVoucher 不校验期间状态）；`generateFacts():543-560`；`resolveSubjects():562-618`（GL mapping 钩子）；`balanceTotals():703-715` + `assertBalanced():717-723`（借贷平衡校验）；`persistVoucher():757-846`（写凭证+行+billR，置 `docStatus=POSTED`）；`buildReversalDraft():725-755`（负数行）；`markOriginalVoucherReversed():909-923`（O-8 统一引擎职责，原 P0-MA1-021 违规点）；`dispatchReversalEvent():363-388`（SYNC 默认 / ASYNC afterCommit）；`recordPostFailure():305-329`（O-6 异常捕获）。
- **Provider 注册表**（`ErpFinAcctDocRegistry.java`）：`init():45-79`（@PostConstruct；非 fallback 优先；非默认重复 fail-fast `ERR_DUPLICATE_PROVIDER`）；`getProvider():81-83`（O(1) Map）。SPI `IErpFinAcctDocProvider`（`getSupportedBusinessTypes()/createFacts()/isFallback()`）。**38 个 Provider 实现**跨 9 域（purchase/sales/inventory/manufacturing/assets/projects/quality/maintenance/hr + finance 内）。
- **凭证状态机轴**（`ErpFinVoucher`，`module-finance/model/app-erp-finance.orm.xml:411-468`）：`postingType` propId=4（NORMAL/REVERSAL/BUDGET/COMMITMENT）；`isReversed` propId=12（红字标记）；`reversalOfVoucherId` propId=13（原凭证指针）；`docStatus` propId=14（dict `erp-fin/voucher-status`：DRAFT/POSTED/CANCELLED — **凭证状态机轴**）；`postedBy`/`postedAt`。状态转移：UNPOSTED→POSTED（`persistVoucher` 置 POSTED）；POSTED→红字凭证（`reverseProcess` 建新凭证 postingType=REVERSAL+isReversed=true，原凭证 isReversed=true）；DRAFT→CANCELLED（owner doc 声明，**无显式 action 实现**，走 CrudBizModel 默认 delete）。
- **业务侧 posted 反写**（8 域）：`posted`/`postedAt`/`postedBy` 在**源业务单据**上（非凭证）；引擎返回 `voucherId`，域调用方置 `posted=true`（`posting.md §反写契约`）。
- **红冲实现**：`IErpFinVoucherBiz.reverse(billHeadCode, businessType, ctx)`→`reverseProcess` 找全部 POSTED+未红冲原凭证（`findAllPostedVouchers:866-882`）→建负数 draft→持久化新凭证（postingType=REVERSAL+isReversed=true+reversalOfVoucherId）→`markOriginalVoucherReversed`（原 isReversed=true）→派发 `VoucherReversedEvent`。简化 UI 入口 `reverseVoucher`（仅置 isReversed，**不生成红字凭证**）。
- **业务类型枚举↔字典**（P1-MA1-018）：`ErpFinBusinessType`（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java:12-68`）56 常量（int code 稳定，**持久化用 `enum.name()`** String）。dict `erp-fin/business-type`（`app-erp-finance.orm.xml:60-127`）。**4 项漂移**：`MANUFACTURING_COST_CLOSE(100)↔PRODUCTION_COST` / `PROJECT_COST_COLLECTION(110)↔PROJECT_COST` / `PERIOD_CLOSE(120)↔PERIOD_CLOSING` / `EXCHANGE_GAIN_LOSS(130)↔FX_REVALUATION`。持久化站点：`ExchangeRevaluationService:152,213`、`ProfitLossClosingService:152`、`ErpFinReportBizModel:370,372`。影响：UI dict 下拉值与 DB 存储值不符→筛选查询漏命中；**内部聚合一致**（全用 `enum.name()`），非过账运行时正确性 bug，但破坏 UI/审计筛选。
- **GL 映射解析**（`ErpFinGlMappingResolver`）：`(orgId, businessType, accountKey)` 索引 + 优先级链（priority DESC, specificity DESC）+ config-gated `erp-fin.gl-mapping.org-dimension-enabled`。
- **可观测性**（`posting-log.md`）：4 类日志（rule-hit SLF4J / change-audit 复用 `NopSysChangeLog` / `ErpFinPostingException` 持久化异常工作台 PENDING/RETRYING/RETRIED/IGNORED/MANUAL 状态机 / runtime-metrics）；延迟重试 `ErpFinDeferredPostingRetryHelper`（REQUIRES_NEW per record，MAX_RETRY=3）。
- **测试覆盖**：服务层 `TestErpFinPostingService`（happy+幂等+不平衡拒绝+期间关闭拒绝）/`TestErpFinAcctDocRegistry`（域优先于 fallback+重复 fail-fast）/`TestErpFinReversalDispatch`+`TestErpFinReversalListenerRegistry`（reversal 派发+listener 隔离）/`TestErpFinArApItemGeneration`/`TestErpFinPostingObservability`/`TestErpFinPostingExceptionNotify`/`TestErpFinMultiSchemaPosting`/`TestErpFinGlMappingResolver`/`TestErpFinAcctDocProviderAccountKey`/EmployeeAdvance·ExpenseClaim·Notes·BadDebt 过账 reverse 系列/`TestErpFinVoucherReversePreview`/模板渲染系列/`TestErpFinPostingMetrics`/`TestErpFinPostingExceptionWorkbench`（异常重试/忽略/人工解决状态机）；跨域 `TestErpPurInvoicePosting`/`TestErpPurFinanceReversalWriteback`/`TestErpSalFinanceReversalWriteback`/`TestErpInvFinanceReversalWriteback`/`TestErpMfgVarianceRecomputeReversal`/`TestErpMfgSubcontractReverse`/`TestErpMntVisitCancelReversal`。浏览器层 `finance-voucher-post`/`voucher-back-link`/`quality-ncr-reverse-voucher-line`/`inv-landed-cost-reversal`/`mfg-material-issue-reversal`/`fin-bad-debt-reverse-*`/`fin-expense-claim`/`fin-employee-advance`/`fin-credit-facility-interest`（幂等键 `(billHeadCode,businessType)`）/`fin-period-close-wizard`/`fin-intercompany-*`/`fin-gl-mapping-routing`/`fin-commitment-accounting` + 编排 `p2p-chain`/`o2c-chain`/`p2p-reverse`/`p2p-reverse-approve`/`o2c-reverse`。

**已登记的直指过账与凭证链路的 MA1/MA2 finding（本审计须复核其运行时行为）**：

- `P0-MA1-021`（**已 done**）：`CostAdjustmentPostingDispatcher.markOriginalVoucherReversed` 曾跨模块直写 `ErpFinVoucher.isReversed` 绕过 `IErpFinVoucherBiz.reverse()`，现 `CostAdjustmentPostingDispatcher.reverse():64-67` 已改走引擎。本审计复核**过账引擎 reversal 统一路径**是否被所有 38 Provider 一致遵守（O-8 公共流），是否有其他域仍绕过引擎直写凭证。
- `P0-MA2-016`（fix-plan-injected）：`ProfitLossClosingService:88-92` 损益结转聚合排除 `EXCHANGE_GAIN_LOSS` 致汇兑损益不结转。**scope note**：表面是期末结账，实际是过账引擎持久化 `enum.name()` 与业务类型过滤的交互——本审计复核过账引擎持久化 `ErpFinBusinessType.PERIOD_CLOSE.name()`/`EXCHANGE_GAIN_LOSS.name()` 的运行时一致性，确认 fix 保留 `PERIOD_CLOSE` 排除（正确，防自结转）仅移除 `EXCHANGE_GAIN_LOSS`。
- `P1-MA1-018`（todo MR1）：enum↔dict 漂移 4 项。本审计复核漂移是否导致过账引擎内部聚合（如损益结转汇总凭证 by businessType）漏算——代码全用 `enum.name()` 内部一致，**非运行时正确性 bug**，但确认 UI/审计筛选漏命中范围。
- `P1-MA1-022`（todo MR1）：mnt `MaintenanceLaborPostingDispatcher:127 daoFor(ErpFinVoucherBillR)` 只读幂等检查。本审计复核该跨域只读在过账幂等路径的运行时正确性。
- `P1-MA2-001`（todo MR1）：GRNI 自动冲回缺失——PURCHASE_INPUT（receive）与 AP_INVOICE（invoice）在先入库后开票黄金路径无自动冲回。**过账引擎 scope**：receive→invoice 不自动红冲前凭证。
- `P1-MA2-002`+`P1-MA2-009`（todo MR1）：多币种 P2P/O2C 本位币凭证路径未验证/未实现。**过账引擎 scope**：`ErpFinPostingProcessor.persistVoucher:818-819` 写 `amountSource=amt` 且 `amountFunctional=amt`（同值）——line 级无 FX 折算；`SalAcctDocProvider.RECEIPT` 无 6051 汇兑损益 plug。
- `P1-MA2-021`（todo MR1）：CLOSED_FINAL 凭证锁定未实现——`ErpFinVoucherBizModel.postVoucher/reverseVoucher:88-114` 仅校验凭证 docStatus，**不校验期间状态**。**scope**：属 A2.5b 期间但守卫站点在凭证 BizModel（A2.5a）。
- `P1-MA2-022`（todo MR1）：FX 重估无前期 reversal——`ExchangeRevaluationService.revalueArAp:106-108` 无期间过滤 + 不 reversal 前期 FX 凭证（累计漂移）。**过账引擎 reversal 机制 scope**：无 FX 凭证自动 reversal。
- `P2-MA1-019`（watch-only）：`ErpFinBusinessType.fromCode:86` 抛 `IllegalArgumentException`（应 `NopException`）+ `ErpFinVoucherTemplateBizModel:95` 用 `LocalDate.now()`。
- `P2-MA2-025`（watch-only，A2.17）：`closePeriod:157-158` 连续 setStatus 无 flush，CLOSING 永不对外可见——`resolveOpenPeriod:507` 期间门控的并发敏感点。

**但从未做过一次覆盖会计凭证状态机（DRAFT/POSTED/CANCELLED + isReversed 红字）、按 `state-machine-business-review-prompt.md` 维度的系统性业务审查**。已知未核验控制点：

- **状态定义清晰性**：`docStatus`（DRAFT/POSTED/CANCELLED）+ `isReversed`（布尔红字标记）+ `postingType`（NORMAL/REVERSAL/BUDGET/COMMITMENT）三轴组合的语义清晰性——红字凭证自身 `isReversed=true` 是否为语义混淆（红字凭证是"已红冲的产物"还是"被红冲的原凭证"？owner doc `state-machine.md:37-42` 终端 POSTED/CANCELLED 未明确红字凭证的终态归属）。
- **转换完整性**：DRAFT→POSTED（postVoucher，前置：借贷平衡+期间开放+科目有效+汇率存在）/ POSTED→红字凭证（reverseProcess）/ DRAFT→CANCELLED（owner doc 声明**无显式 action 实现**，走默认 delete——是否为"动作作为状态"反模式或缺失状态）。每个状态的所有传入/传出转换是否完整列出。
- **终端状态与恢复**：POSTED（终态？可被红冲→非终态）/ CANCELLED（终态）。红冲后原凭证 `isReversed=true` 但 `docStatus` 仍 POSTED——是否可"撤销红冲"（恢复），路径是否明确。
- **异常路径**：不平衡拒绝 / 期间关闭拒绝 / 模板缺失 / 汇率缺失 / 异步失败 / 红冲失败 / 并发 / 幂等重发——是否全覆盖；幂等键 `(billHeadCode, businessType)` 经 `ErpFinVoucherBillR` 的重复回调幂等性。
- **可达性**：从 DRAFT 每个状态是否可达；是否有不可达状态（如 CANCELLED 是否有显式入口）；红字凭证（postingType=REVERSAL）是否可再被红冲（无限循环风险）。
- **角色与权限**：每个转换（postVoucher/reverseVoucher/post/reverse）是否绑定执行角色；危险操作（最终红冲 CLOSED_FINAL 期间凭证——P1-MA2-021 未实现守卫）是否对任何角色开放；多角色冲突（制单员 vs 审核员 vs 会计）。
- **外部依赖**：业务域状态（8 域 posted 反写）在成为凭证状态前是否映射/包装；外部过账回调（异步）的入站通道；外部系统超时/不可用时的回退（`ErpFinDeferredPostingRetryHelper` MAX_RETRY=3 后 IGNORED/MANUAL）。
- **TODO/任务策略**：每个非终端凭证状态是否产生正确类型待办（`ErpFinPostingException` PENDING/RETRYING/RETRIED/IGNORED/MANUAL 状态机是否覆盖"需要人工决策=分配/池任务"、"只是等待=监控任务"）；是否存在期望有人行动但不产生待办的状态（异常 IGNORED 后案例静默下沉）。
- **场景演练**：快乐路径（approve→post→POSTED）/ 拒绝-返回（不平衡/期间关闭拒绝）/ 异常终止（异步失败→重试→IGNORED）/ 外部触发（业务域 reverse→VoucherReversedEvent 回写）/ 超时（重试上限）。
- **与设计文档一致性**：每个状态在 `posting.md`/`state-machine.md`/`posting-log.md` 是否有匹配的页面/API/权限/业务注释；`state-machine.md:249-265` 已记录浏览器层 ErpFinPayment/Receipt `useWorkflow="true"` xwf 审批路径不可达（仅 DIRECT 三轴审批可达）。

剩余差距：需要一次系统性状态机业务审查，将上述已落地组件与未核验控制点整合为通过/失败裁决，发现任何遗漏的 P0（红字凭证可再红冲致无限循环 / 幂等键破缺致重复过账 / 异常 IGNORED 后凭证悬挂 / CLOSED_FINAL 凭证可被修改/红冲——P1-MA2-021 升级评估）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对**会计凭证状态机**（DRAFT/POSTED/CANCELLED + `isReversed` 红字 + `postingType` NORMAL/REVERSAL/BUDGET/COMMITMENT 三轴组合）做系统性业务审查，产出审计报告。**严格限定 A2.5a scope = 凭证状态机**；期间状态机归 A2.5b、AR/AP 核销状态机归 A2.5c。
- 重点核验 9 个已识别控制点：(1) 状态定义清晰性（三轴组合语义，红字凭证终态归属）；(2) 转换完整性（DRAFT→POSTED/POSTED→红字/DRAFT→CANCELLED 无显式 action）；(3) 终端与恢复（POSTED 可被红冲→非终态；红冲可否撤销）；(4) 异常路径（不平衡/期间关闭/模板/汇率/异步失败/红冲失败/并发/幂等）；(5) 可达性（CANCELLED 入口；红字凭证可再红冲的循环风险）；(6) 角色权限（postVoucher/reverseVoucher/post/reverse 绑定角色；CLOSED_FINAL 守卫缺失 P1-MA2-021）；(7) 外部依赖（8 域 posted 反写映射；异步回调入站；重试上限后 IGNORED/MANUAL）；(8) TODO/任务策略（`ErpFinPostingException` 状态机待办类型；IGNORED 静默下沉）；(9) 场景演练（快乐/拒绝/异常终止/外部触发/超时）。
- 复核已登记 finding 在过账与凭证运行时的行为影响：P0-MA1-021（引擎 reversal 统一路径，38 Provider 一致性复核）/ P0-MA2-016（持久化 `enum.name()` 与业务类型过滤交互，fix 保留 PERIOD_CLOSE 排除）/ P1-MA1-018（enum↔dict 漂移 UI/审计筛选漏命中范围）/ P1-MA1-022（mnt 幂等只读）/ P1-MA2-001（GRNI 自动冲回）/ P1-MA2-002+P1-MA2-009（多币种 line 级无 FX 折算）/ P1-MA2-021（CLOSED_FINAL 凭证锁定升级评估）/ P1-MA2-022（FX 凭证无前期 reversal）/ P2-MA1-019（fromCode 异常类型）/ P2-MA2-025（期间门控并发敏感点交接 A2.17），标注终态（仅治理缺陷 / 产生运行时缺陷升级）。
- scope matrix §2.x finance/凭证状态机 相关列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.5a 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.5b 期间状态机（OPEN/CLOSING/CLOSED/CLOSED_FINAL + 反结账）— 期间状态机系统性可达性审查归 A2.5b。本审计覆盖**凭证侧**期间守卫（P1-MA2-021 CLOSED_FINAL 凭证锁定是否在凭证 BizModel 实现），但不做期间状态机本身审查。
- **不**审计 A2.5c AR/AP 核销状态机 — AR/AP 辅助账核销路径归 A2.5c。本审计只确认过账引擎产出的 AR/AP 凭证与辅助账生成的金额一致性（`ErpFinArApItemGenerator`），不做核销状态机审查。
- **不**审计 A2.3 期末结账链路本身 — 损益结转/汇兑重估/年度结转的**编排正确性**归 A2.3（done）；本审计只确认这些链路产出的凭证（PERIOD_CLOSE/EXCHANGE_GAIN_LOSS/PROFIT_TO_RETAINED_EARNINGS）的**状态机行为**（post→POSTED→反结账红冲）正确性。
- **不**审计 A4.1a finance 代码质量 — 过账引擎代码质量（异常处理/N+1/索引）系统性审查归 A4.1a；本审计只做凭证状态机业务正确性审查。
- **不**审计 A2.17 并发与乐观锁 — 并发过账/并发红冲的 lost-update 风险归 A2.17；本审计只标注观察到的并发敏感点（P2-MA2-025 期间门控并发、幂等键竞态）。
- **不**审计 A2.4 库存核算三方对账 — 库存过账通道产出凭证与三方对账金额一致性归 A2.4；本审计只确认凭证状态机行为。
- **不**审计 Non-Goal 子项（owner doc 已裁定）：多 schema 并行凭证（`SchemaPropagator` 已落地但不做系统性审查）、commitment/intercompany 凭证生成器（dormant Provider，`CommitmentVoucherGenerator`/`IntercompanyVoucherGenerator` 直写——归各自 owner doc）、预算凭证（BUDGET postingType，归 A2.5b）、浏览器层 useWorkflow xwf 审批路径（`state-machine.md:249-265` 已记录不可达）。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/posting.md`（三层模型 + PostingEvent 契约 + 幂等 + businessType 映射 + Provider 机制 + 科目映射 + 冲销机制 + 反写契约 — 权威）；`docs/design/finance/state-machine.md`（§对象一 会计凭证状态机 DRAFT/POSTED/CANCELLED + isReversed `:12-121`，**A2.5a scope**；§对象二 期间状态机归 A2.5b）；`docs/design/finance/posting-log.md`（4 类日志 + `ErpFinPostingException` 状态机）；`docs/design/finance/gl-mapping-rules.md`（科目映射优先级链 + accountKey 矩阵）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层，事务边界在 Facade）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.5a 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：会计/财务（凭证状态机/过账引擎/reversal 路径/期间守卫）与 ORM 模型（`module-finance/model/*.orm.xml` 凭证字段/业务类型字典）是 ask-first **最高级别**保护区域。过账与凭证直接触及会计保护区域。P0 即时修复若触及 `ErpFinVoucherBizModel`/`ErpFinPostingProcessor`/`ErpFinBusinessType`/凭证状态机字典/期间守卫/Provider，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（`project-context.md §AI 阻塞条件`）。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 会计凭证状态机系统性业务审查

Status: planned
Targets: `module-finance/erp-fin-service/.../service/entity/ErpFinVoucherBizModel.java`（post/reverse/postVoucher/reverseVoucher/previewReverseVoucher）；`.../service/posting/ErpFinPostingProcessor.java`（process/reverseProcess/alreadyPosted/resolveOpenPeriod/persistVoucher/buildReversalDraft/markOriginalVoucherReversed/dispatchReversalEvent/recordPostFailure）；`.../service/posting/ErpFinAcctDocRegistry.java`+`IErpFinAcctDocProvider.java`（38 Provider）；`.../service/posting/ErpFinReversalListenerRegistry.java`+`VoucherReversedEvent.java`+`IErpFinVoucherReversedListener.java`；`.../service/posting/ErpFinPostingExceptionRecorder.java`+`ErpFinDeferredPostingRetryHelper.java`（异常状态机）；`.../service/posting/ErpFinGlMappingResolver.java`；`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`（56 常量+fromCode）；`module-finance/model/app-erp-finance.orm.xml`（ErpFinVoucher 字段+voucher-status/business-type 字典）；`docs/design/finance/posting.md`+`state-machine.md §对象一`+`posting-log.md`+`gl-mapping-rules.md`；服务层 `TestErpFinPostingService`+`TestErpFinAcctDocRegistry`+`TestErpFinReversalDispatch`+`TestErpFinReversalListenerRegistry`+`TestErpFinPostingExceptionWorkbench`+跨域 reversal writeback 系列；浏览器层 `finance-voucher-post`+`voucher-back-link`+`*-reversal`+编排 `p2p-reverse`/`o2c-reverse`
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-018 enum↔dict 漂移 + P1-MA1-022 mnt 只读 + P2-MA1-019 fromCode 异常 已登记待 MR1 供本审计复核运行时影响）；A2.1-A2.3 done（P1-MA2-001 GRNI + P1-MA2-002/009 多币种 + P1-MA2-021 CLOSED_FINAL 凭证锁定 + P1-MA2-022 FX reversal 已登记，供本审计从凭证状态机角度复核）；P0-MA1-021 done（引擎 reversal 统一路径，供本审计复核 38 Provider 一致性）；P0-MA2-016 fix-plan-injected（EXCHANGE_GAIN_LOSS 业务类型，供本审计复核持久化 enum.name() 运行时影响）

- [ ] 维度「状态定义」：审查 `docStatus`（DRAFT/POSTED/CANCELLED）+ `isReversed`（布尔）+ `postingType`（NORMAL/REVERSAL/BUDGET/COMMITMENT）三轴组合的语义清晰性——每个状态名是否清楚表达业务等待点（状态是"等待 X"而非"做 X"）；红字凭证自身 `isReversed=true`+`postingType=REVERSAL` 是否为语义混淆（红字凭证是"已红冲的产物"还是"被红冲的原凭证"？`state-machine.md:37-42` 终端 POSTED/CANCELLED 未明确红字凭证终态归属）；是否有两状态语义相同或可由业务字段表达（`isReversed` 是否可由 `postingType=REVERSAL`+`reversalOfVoucherId!=null` 推导，冗余轴？）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「转换完整性」：列出每个状态（DRAFT/POSTED/CANCELLED × isReversed true/false × postingType）的所有传入/传出转换；遍历每个转换问业务合法性——DRAFT→POSTED（postVoucher 前置：借贷平衡 `assertBalanced:717`+期间开放 `resolveOpenPeriod:507`+科目有效+汇率存在）/ POSTED→红字凭证（reverseProcess，找 POSTED+未红冲原凭证）/ DRAFT→CANCELLED（owner doc `state-machine.md:35` 声明**无显式 action 实现**，走 CrudBizModel 默认 delete——是否缺失状态或"动作作为状态"反模式）；是否有非法向前/向后跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「终端状态和恢复」：列出所有终端状态（无出边）；确认每个是合法业务结束——POSTED 是否真终态（可被红冲→非终态？）/ CANCELLED 是否终态；红冲后原凭证 `isReversed=true` 但 `docStatus` 仍 POSTED 是否可"撤销红冲"（恢复），路径是否明确（当前无撤销红冲 action，红字凭证是否可再红冲——见可达性维度）；归档与活动凭证是否可区分并正确存储。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「异常路径」：核验全覆盖——不平衡拒绝（`ERR_UNBALANCED`）/ 期间关闭拒绝（`ERR_PERIOD_CLOSED`）/ 模板缺失 / 汇率缺失 / 异步失败（`erp-fin.reversal-dispatch-mode=ASYNC` afterCommit）/ 红冲失败（`ERR_REVERSE_SOURCE_NOT_FOUND`/`ERR_REVERSAL_LISTENER_FAILED`）/ 并发（同 billCode 并发过账）/ 幂等重发（`alreadyPosted:472-484` 排除 isReversed 允许重发）；重复触发器幂等性——幂等键 `(billHeadCode, businessType)` 经 `ErpFinVoucherBillR` 的重复回调是否幂等；`ErpFinDeferredPostingRetryHelper` MAX_RETRY=3 后转 IGNORED/MANUAL 的状态机完整性。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「可达性」：从 DRAFT 每个状态是否可达；是否有不可达状态（CANCELLED 是否有显式入口，或仅经默认 delete 隐式到达——若 owner doc 声明 DRAFT→CANCELLED 但无 action，CANCELLED 是否不可达）；是否有永远无法到终端的路径——**重点：红字凭证（postingType=REVERSAL+isReversed=true）是否可再被红冲**（reverseProcess 找 POSTED+未红冲原凭证，红字凭证 isReversed=true 应被排除→不可再红冲，但需核实是否真排除，防无限循环）；合法循环（commitment 凭证 commit→release）是否有退出条件。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「角色和权限」：每个转换（postVoucher/reverseVoucher/post/reverse）是否绑定执行角色（`@BizMutation` 权限注解——A6.1 全域 grep 的 finance 子集）；危险操作是否对任何角色开放——**重点 P1-MA2-021**：CLOSED_FINAL 期间凭证锁定未实现，`postVoucher/reverseVoucher:88-114` 不校验期间状态，任何有权角色可修改/红冲 CLOSED_FINAL 凭证（升级评估：是否从 P1 升 P0——破坏期末结账不可变性）；多角色冲突（制单员 DRAFT vs 审核员 POSTED vs 会计 reverse）；角色名与状态名是否同业务词汇表。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「外部依赖」：8 域业务侧 `posted` 反写在成为凭证状态前是否映射/包装（域 dispatcher 调 `IErpFinVoucherBiz.post` 返回 voucherId 后置 `posted=true`——反写契约一致性）；外部过账回调（异步 `erp-fin.reversal-dispatch-mode=ASYNC`）的入站通道；外部系统超时/不可用时的回退——`ErpFinDeferredPostingRetryHelper` MAX_RETRY=3 后 IGNORED/MANUAL，凭证是否悬挂（POSTED 但业务侧未反写，或业务侧 posted=true 但凭证失败）；commitment/intercompany dormant Provider 直写凭证（不经引擎 process）是否绕过状态机。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「TODO/任务策略」：每个非终端凭证状态是否产生正确类型待办——`ErpFinPostingException` 状态机（PENDING/RETRYING/RETRIED/IGNORED/MANUAL）是否覆盖"需要人工决策=MANUAL 分配/池任务"、"只是等待=RETRYING 监控任务"、"准备好需确认=RETRIED 确认任务"；是否存在期望有人行动但不产生待办的状态（异常 **IGNORED** 后凭证悬挂，案例静默下沉——重点核验 IGNORED 是否有告警 `TestErpFinPostingExceptionNotify`）；归档凭证是否产生监控任务。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 快乐路径（业务 approve→post→POSTED→业务侧 posted=true）；(b) 拒绝/返回（不平衡/期间关闭拒绝，凭证留 DRAFT 或不创建）；(c) 异常终止（异步 reversal 失败→`ErpFinPostingException` RETRYING→MAX_RETRY→IGNORED/MANUAL）；(d) 外部触发（业务域 reverse→`IErpFinVoucherBiz.reverse`→`VoucherReversedEvent`→域 listener 回写 `posted=false`）；(e) 超时（重试上限）；(f) 幂等（重复 approve 同 billCode→`alreadyPosted` 跳过）；(g) 红冲链（post→reverse→原 isReversed=true+红字凭证）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「与设计文档一致性」：每个状态/转换在 `posting.md`/`state-machine.md §对象一`/`posting-log.md` 是否有匹配的页面/API/权限/业务注释；是否存在"设计了但不在状态机中"或"在状态机中但未描述"的不一致——**重点**：`state-machine.md:35` 声明 DRAFT→CANCELLED 但代码无显式 action（设计与实现不一致？）；`state-machine.md:249-265` 已记录浏览器层 ErpFinPayment/Receipt useWorkflow xwf 审批路径不可达（仅 DIRECT 三轴审批可达）——复核该限制对凭证状态机的影响。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「businessType enum↔dict 一致性（P1-MA1-018 运行时复核）」：核验 4 项漂移（MANUFACTURING_COST_CLOSE↔PRODUCTION_COST / PROJECT_COST_COLLECTION↔PROJECT_COST / PERIOD_CLOSE↔PERIOD_CLOSING / EXCHANGE_GAIN_LOSS↔FX_REVALUATION）在过账引擎持久化 `enum.name()` 与 UI dict 筛选值的交互——确认内部聚合（如损益结转汇总凭证 by businessType、`ErpFinReportBizModel` 报表筛选）全用 `enum.name()` 内部一致（非运行时正确性 bug），但 UI/审计筛选用 dict 值漏命中；复核 P0-MA2-016 fix 是否仅移除 EXCHANGE_GAIN_LOSS 排除保留 PERIOD_CLOSE 排除（持久化值一致）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 维度「多币种凭证路径（P1-MA2-002/009 运行时复核）」：核验 `ErpFinPostingProcessor.persistVoucher:818-819` 写 `amountSource=amt` 且 `amountFunctional=amt`（同值）——line 级无 FX 折算；多币种下凭证状态机行为是否正确（DRAFT→POSTED 不因币种失败），但本位币金额与源币金额同值是否为状态机正确性缺陷（凭证"借贷平衡"在本位币维度是否真平衡）；复核 `SalAcctDocProvider.RECEIPT` 无 6051 汇兑损益 plug 对凭证状态机的影响（凭证技术上平衡但业务上漏汇兑损益）。
      - Skill: `state-machine-business-review-prompt.md`
- [ ] 复核已登记 MA1/MA2 finding 运行时影响（凭证状态机角度）：P0-MA1-021（引擎 reversal 统一路径，38 Provider 一致性——是否有其他域仍绕过引擎直写凭证）/ P0-MA2-016（持久化 enum.name() 一致性）/ P1-MA1-018（enum↔dict 漂移 UI 筛选漏命中）/ P1-MA1-022（mnt 幂等只读 daoFor(ErpFinVoucherBillR)）/ P1-MA2-001（GRNI 自动冲回——receive→invoice 凭证状态机交互）/ P1-MA2-002+009（多币种 line 级无 FX）/ P1-MA2-021（CLOSED_FINAL 凭证锁定升级评估）/ P1-MA2-022（FX 凭证无前期 reversal 累计漂移）/ P2-MA1-019（fromCode IllegalArgumentException + LocalDate.now）/ P2-MA2-025（期间门控并发交接 A2.17）。标注每项终态。
      - Skill: none
- [ ] 产出审计报告 `docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（含：凭证状态机三轴组合状态图、各维度通过/失败裁决、9 控制点 PASS/FAIL、MA1/MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

- [ ] 凭证状态机三轴组合（docStatus × isReversed × postingType）的状态图与转换矩阵产出，每个状态/转换有通过/失败裁决与证据
- [ ] 9 个已识别控制点（状态定义 / 转换完整性 / 终端与恢复 / 异常路径 / 可达性 / 角色权限 / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [ ] state-machine-business-review 10 维度 + 2 项目特定维度（businessType enum↔dict / 多币种凭证路径）至少一句裁决（含「本维度无发现」）
- [ ] MA1/MA2 finding 运行时影响复核结论已记录（含 P1-MA2-021 CLOSED_FINAL 凭证锁定的升级评估裁决）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: planned
Targets: 过账与凭证审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.x finance/凭证状态机行
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [ ] P0 finding 即时处理：每个 P0（红字凭证可再红冲致无限循环 / 幂等键破缺致重复过账 / 异常 IGNORED 后凭证悬挂 / CLOSED_FINAL 凭证可被修改红冲 [若 P1-MA2-021 升级] / reversal 统一路径有域仍绕过引擎直写凭证）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [ ] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。注意：本审计对已登记 finding（P1-MA1-018/022 + P1-MA2-001/002/009/021/022）只复核运行时影响不重复登记根因；若发现新 P1（如 DRAFT→CANCELLED 缺失 action / 红字凭证终态归属未定义 / IGNORED 静默下沉）按新 finding ID 登记。
      - Skill: none
- [ ] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.x finance/凭证状态机 相关列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [ ] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [ ] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [ ] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_05c0a1fb1ffeVJIA3tcF82Uj85`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：`ErpFinVoucherBizModel` post/reverse/postVoucher/reverseVoucher/previewReverseVoucher 在 67-149 精确行号；`ErpFinPostingProcessor` process/reverseProcess/alreadyPosted/resolveOpenPeriod/persistVoucher/buildReversalDraft/markOriginalVoucherReversed/dispatchReversalEvent/recordPostFailure 全部存在（126/209/472/495/757/725/909/363/305）；4 项 enum↔dict 漂移（MANUFACTURING_COST_CLOSE(100)/PROJECT_COST_COLLECTION(110)/PERIOD_CLOSE(120)/EXCHANGE_GAIN_LOSS(130) ↔ PRODUCTION_COST/PROJECT_COST/PERIOD_CLOSING/FX_REVALUATION）+ `enum.name()` 持久化（PostingProcessor:822,839,841,888）核实为真；`ErpFinVoucher` postingType/isReversed/reversalOfVoucherId/docStatus propId 4/12/13/14（orm.xml:418/426/427/428）；**关键 finding 核实**——`persistVoucher:818-819` 确实写 `amountSource=amt` 且 `amountFunctional=amt`（line 级无 FX 折算）+ `postVoucher/reverseVoucher` 确实不校验期间状态（P1-MA2-021）+ `alreadyPosted:472` 确实排除 `isReversed=true`；11 个 finding ID 全部在 arm-index.md（P0-MA1-021=done / P0-MA2-016=fix-plan-injected）。11 项检查清单全部 PASS（格式/结果表面/Item 类型/技能/反松弛/不可降级——P1-MA2-021 升级评估留待审计裁决非预判/范围——rule-14 不违反，A2.5a/b/c 为 roadmap 强制 S 级拆分/基线准确性/10 维度覆盖/结束门控/退出标准）。非阻塞注记：Goals「9 控制点」与 Exit Criteria「10+2 维度」框架可更清晰但退出义务无歧义（9=重点子集，Exit Criteria 3 独立要求全维度裁决）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。过账与凭证触及会计保护区域，P0 即时修复须额外人工确认。

- [ ] 范围内行为完成（A2.5a 会计凭证状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [ ] 相关文档对齐（审计报告、arm-index、scope matrix、posting/state-machine/posting-log/gl-mapping-rules owner doc 结论已反映）
- [ ] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am` 作回归基线确认；若有 P0 即时修复，该修复模块测试全绿
- [ ] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 注入即时通道 fix plan，不降级为 MR）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.5b 期间状态机系统性审查

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计覆盖**凭证侧**期间守卫（P1-MA2-021 CLOSED_FINAL 凭证锁定是否在凭证 BizModel 实现的升级评估），但期间状态机（OPEN/CLOSING/CLOSED/CLOSED_FINAL + 反结账）系统性可达性审查归 A2.5b。
- Successor Required: `yes`——A2.5b 执行时复核。

### A2.5c AR/AP 核销状态机

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计只确认过账引擎产出 AR/AP 凭证与辅助账生成的金额一致性（`ErpFinArApItemGenerator`），AR/AP 辅助账核销状态机归 A2.5c。
- Successor Required: `yes`——A2.5c 执行时复核。

### A4.1a finance 代码质量审计 — 过账与凭证链路

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做凭证状态机**业务正确性**审查；过账引擎代码质量（异常处理类型/N+1/索引/辅助方法）系统性审查归 A4.1a。
- Successor Required: `yes`——A4.1a 执行时复核。

### A2.17 并发与乐观锁（并发过账/并发红冲/幂等键竞态/期间门控并发）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（P2-MA2-025 期间门控并发、`(billHeadCode,businessType)` 幂等键竞态、并发 reverse 同原凭证），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### 多 schema 并行凭证 + commitment/intercompany dormant Provider + 预算凭证 + useWorkflow xwf 审批

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定——`SchemaPropagator` 多 schema 已落地但不做系统性审查；commitment/intercompany dormant Provider 直写凭证（`CommitmentVoucherGenerator`/`IntercompanyVoucherGenerator`）归各自 owner doc；预算凭证（BUDGET postingType）归 A2.5b；浏览器层 useWorkflow xwf 审批路径 `state-machine.md:249-265` 已记录不可达（仅 DIRECT 三轴审批可达）。
- Successor Required: `yes`——各 successor 触发条件满足时（如多公司合并报表上线/commitment 会计启用/预算控制深化/xwf 审批浏览器层落地）。

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- <待独立结束审计填写>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
