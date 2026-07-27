# MA1 平台合规审计 — purchase + sales + assets + inventory（A 级核心四域，A1.12）

> 报告日期：2026-07-27
> 来源 plan：`docs/plans/2026-07-27-1227-3-audit-remediation-ma1-platform-conformance-a-tier-core.md`
> Skill：`docs/skills/nop-platform-conformance-audit-prompt.md`（15 维度语义审计 + 自动化 grep）
> 范围：`module-purchase/`、`module-sales/`、`module-assets/`、`module-inventory/` 四域 `{domain}-{service,dao,web,meta,app}` Java + `*.orm.xml` + `*.xbiz.xml` + `docs/design/{purchase,sales,assets,inventory}/`
> 锚点：`compliance-baseline.md §M0 锚点注记`（HEAD=0e963531d，落锚日 compliance checker 全 19 规则 actual ≤ baseline，0 漂移）。本次审计执行时 HEAD=77d892442（无业务代码层变更，仅文档/审计推进），基线保持。

## 0. 执行摘要

| 域 | 维度合规率（15/15） | 反模式实例（grep 命中） | P0 | P1 新增 | P2 新增 |
|----|---------------------|-----------|-----|---------|---------|
| purchase | 15/15 | 0 真实命中（17 javadoc-only 假阳） | 0 | 0（合并至 P1-MA1-022 跨域通用项） | 1（D1 残留，watch-only） |
| sales | 15/15 | 0 真实命中（同 javadoc-only） | 0 | 0（合并至 P1-MA1-022） | 0 |
| assets | 15/15（含 2 处 Minor owner-doc 漂移登记） | 0 真实命中 | 0 | 0（合并至 P1-MA1-022） | 2（P2-MA1-023 / P2-MA1-024） |
| inventory | 14/15（含 1 项 P0 业财一体写绕过 I\*Biz） | **1 真实命中**（cross-module 写） | **1**（P0-MA1-021） | 0（合并至 P1-MA1-022） | 1（P2-MA1-025） |
| **合计** | **59/60** | 1 真实 + 17 javadoc 假阳 | **1** | **1**（跨域合并） | **4** |

**裁决：失败（条件性）**——四域 60 维度中 59 维度合规；1 项 P0 阻塞：inventory `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed:127-141` 通过 `voucherDao.updateEntity(voucher)` 直接跨模块写 `ErpFinVoucher`（finance 保护区域实体），绕过 `IErpFinVoucherBiz`（业财一体写绕过 I\*Biz，plan P0 类别）。该修复触及 finance 凭证保护区域（plan §保护区域门控），需 owner doc 描述预期行为 + 授权 + 子切片独立审计，故**异步注入 fix plan**而非即时通道（plan 明示「异步注入 fix plan」为 P0 合法处置路径）。

四域其他平台合规基线良好：
- 机械化规则全绿：0 `extends RuntimeException`（main 代码）、0 `@Inject private`、0 `System.currentTimeMillis`、0 `LocalDate.now()`、0 真实 `@Transactional+@BizMutation` 共存（17 个 grep 命中全部为 javadoc 文本，已逐文件核实）、0 `_gen/` 手改、0 `__XGEN_FORCE_OVERRIDE__` 手改、0 跨模块 `refEntityName` 缺失 `notGenCode` 声明。
- 跨模块外部实体引用全部经机制 B 声明（与 A1.10 / A1.11 一致）。
- 标准服务模式良好：4 域共 82 个 BizModel 中 75 个 `extends CrudBizModel<T>`，7 个非 CRUD（Dashboard / Report / Costing 等 facade）合规。
- 异常处理集中：4 个 `ErpXxxErrors.java` 集中管理 ErrorCode；共 194 处 `throw new NopException`，0 `IllegalArgumentException` / `RuntimeException`（main 代码，programmer-error 路径无残留）。

## 1. 自动化 grep 扫描结果

### 1.1 grep 反模式扫描（机械化规则）

| 规则 | purchase | sales | assets | inventory | 处置 |
|------|----------|-------|--------|----------|------|
| `extends RuntimeException`（main） | 0 | 0 | 0 | 0 | ✅ R4 基线 0 |
| `@Inject private`（IoC 失败） | 0 | 0 | 0 | 0 | ✅ R5 基线 0 |
| `@Transactional` 与 `@BizMutation` 真实共存 | 0 | 0 | 0 | 0 | ✅ 全部 17 个 grep 命中（pur 6 + sal 7 + ast 0 + inv 4 文件）经逐文件核实为 javadoc/类注释文本（"事务边界：跟随 xbiz mutation..."），无真实注解共存。processor 文件全部继承 `Abstract*Processor<T>` 抽象基类，事务由外层 xbiz mutation 保护。|
| `System.currentTimeMillis()` | 0 | 0 | 0 | 0 | ✅ R7 基线 0（plan 2026-07-24-0941-2 已收敛） |
| `LocalDate.now()` / `LocalDateTime.now()` | 0 | 0 | 0 | 0 | ✅ 全域 0（无 R7 同性质残留，与 S 级 finance `LocalDate.now()` 残留不同） |
| `IllegalArgumentException`（main 代码） | 0 | 0 | 0 | 0 | ✅ 全域 0（5 个 grep 命中均在 `/test/` 路径下的测试辅助代码，非 main） |
| `_gen/` 手改（git diff） | 0（工作树 clean） | 0 | 0 | 0 | ✅ |
| `__XGEN_FORCE_OVERRIDE__` 手改 | 0（仅出现在 `*-api/.../beans/*.java` codegen 输出，git clean 验证未手改） | 0 | 0 | 0 | ✅ |
| 跨模块 `refEntityName` 无 notGenCode 声明 | 0（12 处全部声明） | 0（11 处全部声明） | 0（6 处全部声明） | 0（10 处全部声明） | ✅ 与 A1.10 结论一致 |
| `IDaoProvider` 直接注入（应 I\*Biz） | 7 文件（Dashboard + 多个 Processor/Dispatcher Helper） | 9 文件（同） | 9 文件（同） | 8 文件（同） | ⚠️ **1 项 P0 + 1 项 P1 合并登记**：① `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed` 跨模块 **写** ErpFinVoucher → P0-MA1-021；② 其余全部为**域内 Helper 的跨模块只读访问**（Dashboard 聚合 / Subject 解析 / Period 查询 / Material 读取），同 P1-MA1-016 根因 → P1-MA1-022 合并登记 |

### 1.2 误报澄清：`@Transactional + @BizMutation` 的 17 个 grep 命中

逐文件核实结论：17 个文件（purchase 6 / sales 7 / assets 0 / inventory 4）全部为 **javadoc / 类注释中的事务边界策略文本**，如：

- `ErpSalInvoiceProcessor.java:43`：`* <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。`
- `ErpPurOrderProcessor.java:47`：同上
- `PurPostingExecutor.java` / `SalPostingExecutor.java` / `InvPostingExecutor.java` / `AssetPostingExecutor.java`：posting executor 类 javadoc 描述「事务边界跟随 Facade `IErpFinVoucherBiz.post` 的 `REQUIRES_NEW`」，本类不带 `@Transactional`
- `StockMoveBookkeeper.java`：注释中描述事务策略
- `SalInvoicePostingDispatcher.java` / `SalReturnPostingDispatcher.java` 等 dispatcher 类：同 posting executor

checker R6/R10 grep 管道（plan 2026-07-27-0823-1）的 `grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*|\*/)'` 注释排除校准已吸收这些假阳性。**真实 `^\s*@Transactional` 注解行数：0**（已用 `rg '^\s*@Transactional'` 独立验证）。

## 2. purchase 15 维度审计

### 维度合规表

| # | 维度 | 结论 | 备注 |
|---|------|------|------|
| 1 | 决策顺序（Model→Delta→Java） | ✅ | 源模型 `module-purchase/model/app-erp-purchase.orm.xml`（187 java 文件，含 `_gen/`）；保留层定制文件存在；Java 仅承载不可模型化行为（过账 dispatcher / Processor 审批三段） |
| 2 | 跨实体访问规则 | ⚠️ P1（合并） | `ErpPurOrderProcessor`/`ErpPurPaymentProcessor` 直接 `daoProvider.daoFor(ErpMdSubject/ErpFinAccountingPeriod).findAllByQuery/getEntityById`——同 P1-MA1-016 跨域只读未走 I\*Biz 根因，合并至 P1-MA1-022。无跨模块**写**。 |
| 3 | 异常处理 | ✅ | 43 处 `throw new NopException`（main）；`ErpPurErrors` 集中管理 ErrorCode；0 `IllegalArgumentException` / `RuntimeException`（main） |
| 4 | IoC 与事务 | ✅ | 0 `@Inject private`；0 真实 `@Transactional + @BizMutation` 共存（6 个 grep 命中全部 javadoc）；`@BizMutation` 不冗余 `@Transactional` |
| 5 | 平台辅助工具 | ✅ | 0 `System.currentTimeMillis`；0 `LocalDate.now`；时间获取全域采用 `CoreMetrics` |
| 6 | 标准服务模式 | ✅ | 20/21 BizModel `extends CrudBizModel<T>`；1 个非 CRUD（`ErpPurDashboardBizModel` facade，javadoc 显式声明「注入 IDaoProvider 经 QueryBean 过滤后内存聚合」） |
| 7 | 机制 B（跨模块外部实体引用） | ✅ | 12 处跨模块 refEntityName 全部 `<entity notGenCode="true">` 声明（material/material_sku/partner/warehouse/uom/currency/organization/tax_rate/settlement_method/bank_account/project/employee） |
| 8 | 状态机与规则引擎 | ✅ | 三轴状态分离（docStatus + approveStatus + paidStatus/receiveStatus）声明式实现：approveStatus 用平台标准 `wf/approve-status` 4 态字典（非硬编码 if-else）；付款进度 / 收货进度派生状态由核销明细驱动（owner doc `state-machine.md §付款状态机` 设计要点）；三单匹配规则配置化（owner doc `three-way-match.md`） |
| 9 | 审批流与作业 | ✅ | pom 依赖 `nop-wf-core` + `nop-wf-meta`（erp-pur-dao / erp-pur-service）；审批三段（提交-审核-过账）经 per-mutation Processor + approval-support.xbiz 标准源；nop-message 跨域事件经 `IErpFinAcctDocProvider` 派发 |
| 10 | 定制能力顺序 | ✅ | Delta 保留层文件齐全（`ErpPur*.{xmeta,view.xml,main.page.yaml,picker.page.yaml,lib.xjs}` + `_ErpPur*.xmeta` + `_ErpPur*.xbiz` + `ErpPur*.xbiz`） |
| 11 | 多租户与本地化 | ✅ | 源 orm.xml 无 `tenantId` 列；无内建本地化（金税集成走 `module-l10n-cn`，已预留） |
| 12 | 测试 | ✅ | 33 test 文件；覆盖审批三段 + 三单匹配异常路径 + 业财端到端（PO→Receive→Invoice→Pay） |
| 13 | codegen 产物安全 | ✅ | 工作树 clean；`__XGEN_FORCE_OVERRIDE__` 仅出现在 `*-api/.../beans/*.java`（codegen 输出），git status 验证未手改；`_app.orm.xml` / `_service.beans.xml` 由 `mvn clean install -DskipTests` 增量再生 |
| 14 | 聚合完整性 | ✅ | `app-erp-all/.../auth/app.action-auth.xml:6` 聚合 `/erp/pur/auth/erp-pur.action-auth.xml`（保留层文件，无 `_` 前缀）；聚合器 POM 含 `erp-pur-app` 依赖 |
| 15 | owner-doc → 代码漂移抽样 | ✅ | 见下表（4 核查点全部一致） |

### purchase 维度 15 owner-doc 抽样核查

| # | owner doc : section | 断言 | 代码位置 | 结论 |
|---|---------------------|------|----------|------|
| 1 | `state-machine.md` §1.1 审核轴状态定义 | 4 态：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | `app-erp-purchase.orm.xml:114` column `approveStatus ext:dict="wf/approve-status"` + `ErpPurDocStatus.java:16-19`（`APPROVE_STATUS_*` 全 4 项逐一对应） | ✅ 一致 |
| 2 | `state-machine.md` §付款状态机 | 3 态：UNPAID/PARTIAL/PAID（派生状态） | `app-erp-purchase.orm.xml:32-35` dict `erp-pur/paid-status` option value 与 code 合一 + `_ErpPurDaoConstants.java:9-19`（`PAID_STATUS_*`） | ✅ 一致 |
| 3 | `state-machine.md` §三轴状态分离 docStatus | 草稿/已生效/已作废（DRAFT/ACTIVE/CANCELLED） | `ErpPurDocStatus.java:22-24`（`DOC_STATUS_DRAFT/ACTIVE/CANCELLED`） | ✅ 一致 |
| 4 | `state-machine.md` §2 SUBMITTED→APPROVED 触发的后续业务 | 采购入库 → `IErpInvStockMoveBiz` 生成入库移动单（incoming） | `ErpPurReceiveProcessor` 注入 `IErpInvStockMoveBiz`，approve 钩子调用（grep 实测） | ✅ 一致 |
| 5（扩）| `three-way-match.md` | 发票审核三单匹配校验 | `ErpPurInvoiceProcessor.validateThreeWayMatch` 实现 | ✅ 一致 |
| 备注 | `app-erp-purchase.orm.xml:451` column `status ext:dict="erp-pur/scorecard-status" defaultValue="10"` | defaultValue="10" 与 dict value DRAFT/FINALIZED 不符 | D1（int→string）已知 deferred 残留 | ⚠️ 已知 deferred（D1，watch-only，本审计不重复裁决，见 §7） |

## 3. sales 15 维度审计

### 维度合规表

| # | 维度 | 结论 | 备注 |
|---|------|------|------|
| 1 | 决策顺序 | ✅ | 源模型 + 保留层定制 + Java 仅承载行为（162 java 文件） |
| 2 | 跨实体访问规则 | ⚠️ P1（合并） | `ErpSalOrderProcessor:377,389` 直接 `daoProvider.daoFor(ErpMdSubject/ErpFinAccountingPeriod).findAllByQuery`——同 P1-MA1-016 根因，合并至 P1-MA1-022。无跨模块**写**。 |
| 3 | 异常处理 | ✅ | 33 处 `throw new NopException`（main）；`ErpSalErrors` 集中管理；0 `IllegalArgumentException` / `RuntimeException`（main） |
| 4 | IoC 与事务 | ✅ | 0 `@Inject private`；0 真实 `@Transactional + @BizMutation` 共存（7 个 grep 命中全部 javadoc） |
| 5 | 平台辅助工具 | ✅ | 0 `System.currentTimeMillis`；0 `LocalDate.now` |
| 6 | 标准服务模式 | ✅ | 16/17 BizModel `extends CrudBizModel<T>`；1 个非 CRUD（`ErpSalDashboardBizModel` facade，javadoc 显式声明 IDaoProvider 用途） |
| 7 | 机制 B | ✅ | 11 处跨模块 refEntityName 全部 `<entity notGenCode="true">`（material/material_sku/partner/warehouse/uom/currency/organization/tax_rate/settlement_method/bank_account/project） |
| 8 | 状态机与规则引擎 | ✅ | 三轴状态分离（docStatus + approveStatus + receivedStatus/deliveryStatus）声明式实现；收款进度 / 发货进度派生状态由核销 / 出库明细驱动；UC-SAL-10 并发扣批次缺口归 MA2 A2.17（本审计仅核验 `@Version` 注解存在性——实测 `ErpInvStockBalance` `version` 列存在，乐观锁基础具备；并发正确性归 MA2） |
| 9 | 审批流与作业 | ✅ | pom 依赖 `nop-wf-core` + `nop-wf-meta`；审批三段经 per-mutation Processor；预算 commitment 经 `IErpFinBudgetCommitmentBiz.release`（I\*Biz 跨域调用，平台规范） |
| 10 | 定制能力 | ✅ | Delta 保留层齐全 |
| 11 | 多租户与本地化 | ✅ | 无 tenantId；无内建本地化 |
| 12 | 测试 | ✅ | 35 test 文件（覆盖审批三段 + 业财端到端 SO→Delivery→Invoice→Receipt + 退货退款路径） |
| 13 | codegen 产物安全 | ✅ | 工作树 clean |
| 14 | 聚合完整性 | ✅ | `app.action-auth.xml:7` 聚合 `/erp/sal/auth/erp-sal.action-auth.xml` |
| 15 | owner-doc → 代码漂移 | ✅ | 见下表（4 核查点全部一致） |

### sales 维度 15 owner-doc 抽样核查

| # | owner doc : section | 断言 | 代码位置 | 结论 |
|---|---------------------|------|----------|------|
| 1 | `state-machine.md` §三轴状态分离 receivedStatus | 3 态：UNRECEIVED/PARTIAL/RECEIVED（销售发票收款进度） | `app-erp-sales.orm.xml:58-61` dict `erp-sal/received-status` option value 与 code 合一 + `app-erp-sales.orm.xml:313,632` column `receivedStatus ext:dict="erp-sal/received-status"` | ✅ 一致 |
| 2 | `state-machine.md` §与采购域差异（出库校验可用量） | 出库 SUBMITTED→APPROVED 时校验可用量，不足拒绝 | `ErpSalDeliveryProcessor.enforceAvailability` 实现（grep 实测，UC-SAL-10 并发缺口归 MA2） | ✅ 一致（平台规范） |
| 3 | `state-machine.md` §审核轴 approveStatus | 与采购域镜像：UNSUBMITTED/SUBMITTED/APPROVED/REJECTED | `app-erp-sales.orm.xml` 多 column `approveStatus ext:dict="wf/approve-status"` | ✅ 一致 |
| 4 | `state-machine.md` §7 销售出库触发库存 | 通过 `IErpInvStockMoveBiz` 同步调用，**可用量校验在调用前** | `ErpSalDeliveryProcessor` 注入 `IErpInvStockMoveBiz`，先校验后调用（grep 实测） | ✅ 一致 |

## 4. assets 15 维度审计

### 维度合规表

| # | 维度 | 结论 | 备注 |
|---|------|------|------|
| 1 | 决策顺序 | ✅ | 源模型 + 保留层 + Java 行为（176 java 文件，含 48 Processor 折旧引擎） |
| 2 | 跨实体访问规则 | ⚠️ P1（合并） | `ErpAstDepreciationScheduleProcessor:290` 跨域只读 `daoFor(ErpFinAccountingPeriod)`；多个 posting dispatcher 跨域只读 `daoFor(ErpMdSubject)`——同 P1-MA1-016 根因，合并至 P1-MA1-022。无跨模块**写**。 |
| 3 | 异常处理 | ✅ | 68 处 `throw new NopException`（main）；`ErpAstErrors` 集中管理（`ERR_DISPOSAL_ASSET_ALREADY_DISPOSED` / `ERR_ADJUSTMENT_ASSET_ALREADY_DISPOSED` 等）；0 `IllegalArgumentException` / `RuntimeException`（main） |
| 4 | IoC 与事务 | ✅ | 0 `@Inject private`；0 真实 `@Transactional + @BizMutation` 共存 |
| 5 | 平台辅助工具 | ✅ | 0 `System.currentTimeMillis`；0 `LocalDate.now` |
| 6 | 标准服务模式 | ✅ | 18/20 BizModel `extends CrudBizModel<T>`；2 个非 CRUD（`ErpAstDashboardBizModel` + `ErpAstReportBizModel` facade） |
| 7 | 机制 B | ✅ | 6 处跨模块 refEntityName 全部 `<entity notGenCode="true">`（organization/currency/employee/location/material_category/subject） |
| 8 | 状态机与规则引擎 | ✅ | 资产卡片状态机声明式（dict + 常量，6 态）；折旧引擎 48 Processor 经 `Abstract*Processor<T>` 抽象基类派生（plan 2026-07-25-1057-2 校准后的合规模式，per-mutation Processor 文件）；折旧方法（直线法/双倍余额递减/工作量法）经 dict `erp-ast/depreciation-method` 配置化 |
| 9 | 审批流与作业 | ✅ | pom 依赖 `nop-wf-core` + `nop-wf-meta`；折旧批量经 nop-job 调度（owner doc `depreciation-and-posting.md`）；处置审批 DIRECT 三轴（不依赖 xwf，浏览器层可达——owner doc §已知限制记录） |
| 10 | 定制能力 | ✅ | Delta 保留层齐全 |
| 11 | 多租户与本地化 | ✅ | 无 tenantId；无内建本地化 |
| 12 | 测试 | ✅ | 18 test 文件（覆盖折旧计提 / 拆分合并 / 处置 / 价值调整 / 资本化） |
| 13 | codegen 产物安全 | ✅ | 工作树 clean |
| 14 | 聚合完整性 | ✅ | `app.action-auth.xml:10` 聚合 `/erp/ast/auth/erp-ast.action-auth.xml` |
| 15 | owner-doc → 代码漂移 | ⚠️ 2 Minor | 见下表（4 核查点中 2 处漂移；按 skill 规则「单域 ≥2 漂移扩大抽样」执行扩大，详见 §4.1） |

### assets 维度 15 owner-doc 抽样核查

| # | owner doc : section | 断言 | 代码位置 | 结论 |
|---|---------------------|------|----------|------|
| 1 | `state-machine.md` §1 资产状态定义 | 5 态：DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD | `app-erp-assets.orm.xml:66-72` dict `erp-ast/asset-status` 含 **6 态**（多 `DISPOSED`） + `_ErpAstDaoConstants.java:9-34`（`ASSET_STATUS_*` 全 6 项） | ❌ **Minor 漂移**（owner doc 缺 DISPOSED 态，详见 P2-MA1-023） |
| 2 | `state-machine.md` §折旧计划条目状态 | 3 态：PENDING/EXECUTED/REVERSED | `app-erp-assets.orm.xml:84-88` dict `erp-ast/depreciation-schedule-status` 含 **4 态**（多 `CANCELLED`） | ❌ **Minor 漂移**（owner doc 缺 CANCELLED 态，详见 P2-MA1-024） |
| 3 | `state-machine.md` §2 资产迁移完整性 | 资本化入账 → 生成资本化凭证 | `CapitalizationPostingDispatcher` + `ErpFinBusinessType.CAPITALIZATION` 过账 Provider 实现 | ✅ 一致 |
| 4 | `state-machine.md` §3 终态不可恢复 | SCRAPPED/SOLD 终态无出边 | `ErpAstDisposalProcessor` + `ErpAstErrors.ERR_DISPOSAL_ASSET_ALREADY_DISPOSED`（已处置资产拒绝再次处置） | ✅ 一致 |
| 5（扩）| `split-merge.md` §资产状态字典 DISPOSED | DISPOSED = 内部处置（拆分/合并），与 SCRAPPED/SOLD 并列终态 | `app-erp-assets.orm.xml:72` dict option + `_ErpAstDaoConstants.ASSET_STATUS_DISPOSED="DISPOSED"` + `ErpAstMergeProcessor:389` `setStatus(DISPOSED)` + `ErpAstSplitProcessor:449` `setStatus(DISPOSED)` | ✅ `split-merge.md` 与代码一致；`state-machine.md §1` 漏写 DISPOSED（漂移源在 `state-machine.md` 过时，非代码缺陷） |
| 6（扩）| `depreciation-and-posting.md` | 折旧计划按资产生成，每期一条 | `ErpAstDepreciationSchedule` 实体 + `ErpAstDepreciationScheduleProcessor` 实现 | ✅ 一致 |
| 7（扩）| `depreciation-and-posting.md` §折旧方法 | 直线法 / 双倍余额递减 / 工作量法 | dict `erp-ast/depreciation-method` option 3 项 + CostingStrategy 子计算器注入 | ✅ 一致 |

### 4.1 扩大抽样触发（assets ≥2 漂移）

按 skill 维度 15 规则「若发现 ≥2 处漂移，扩大抽样至全部 owner doc」，本审计扩大抽样 3 个核查点（上表 #5/#6/#7）。结论：
- 漂移源全部在 `state-machine.md`（过时未更新），非代码缺陷
- 兄弟 owner doc `split-merge.md` / `depreciation-and-posting.md` 与代码一致，是稳定设计真相
- 故漂移严重性降级为 Minor（owner doc 内部不一致），登记为 P2 watch-only（MR1 顺手更新 `state-machine.md`），非 Major 业务影响

## 5. inventory 15 维度审计

### 维度合规表

| # | 维度 | 结论 | 备注 |
|---|------|------|------|
| 1 | 决策顺序 | ✅ | 源模型 + 保留层 + Java 行为（179 java 文件） |
| 2 | 跨实体访问规则 | ❌ **P0** | **`CostAdjustmentPostingDispatcher.markOriginalVoucherReversed:127-141` 跨模块写**：`daoProvider.daoFor(ErpFinVoucher.class).updateEntity(voucher)` 直接更新 finance 凭证 `isReversed=true`，绕过 `IErpFinVoucherBiz.reverse()`（业财一体写绕过 I\*Biz，plan P0 类别）。详见 P0-MA1-021。 |
| 3 | 异常处理 | ✅ | 50 处 `throw new NopException`（main）；`ErpInvErrors` 集中管理（`ERR_LANDED_COST_DRAFT_EXISTS` 等）；0 `IllegalArgumentException` / `RuntimeException`（main） |
| 4 | IoC 与事务 | ✅ | 0 `@Inject private`；0 真实 `@Transactional + @BizMutation` 共存（4 个 grep 命中全部 javadoc） |
| 5 | 平台辅助工具 | ✅ | 0 `System.currentTimeMillis`；0 `LocalDate.now` |
| 6 | 标准服务模式 | ✅ | 21/24 BizModel `extends CrudBizModel<T>`；3 个非 CRUD（`ErpInvDashboardBizModel` + `ErpInvReportBizModel` + `ErpInvCostingBizModel implements IErpInvCostingBiz` facade） |
| 7 | 机制 B | ✅ | 10 处跨模块 refEntityName 全部 `<entity notGenCode="true">`（material/material_sku/warehouse/location/uom/currency/organization/acct_schema/partner/employee） |
| 8 | 状态机与规则引擎 | ✅ | 库存移动状态机声明式（dict `erp-inv/move-status` 4 态 + 常量）；7 种 costMethod 经 dict `erp-inv` 配置化 + CostingStrategy 子计算器注入（StandardCostResolver / CostMethodResolver）；UC-INV-08 乐观锁缺口归 MA2 A2.17（本审计仅核验 `ErpInvStockBalance.version` 列存在——实测具备乐观锁基础） |
| 9 | 审批流与作业 | ✅ | 业务单据审批触发库存移动（采购入库 / 销售出库经 `IErpInvStockMoveBiz` 同步调用）；inventory 自身无独立审批流（库存移动不需审批），无 nop-wf 直接依赖（合理设计）；nop-message 经 `IErpFinAcctDocProvider` 派发存货过账事件 |
| 10 | 定制能力 | ✅ | Delta 保留层齐全 |
| 11 | 多租户与本地化 | ✅ | 无 tenantId；负库存配置 `erp-inv.allow-negative-stock`（默认 false）经平台标准配置 |
| 12 | 测试 | ✅ | 28 test 文件（覆盖移动单 DRAFT→CONFIRMED→DONE / 业财一体写库存 / 成本调整 / landed cost 端到端） |
| 13 | codegen 产物安全 | ✅ | 工作树 clean |
| 14 | 聚合完整性 | ✅ | `app.action-auth.xml:8` 聚合 `/erp/inv/auth/erp-inv.action-auth.xml` |
| 15 | owner-doc → 代码漂移 | ⚠️ 1 Minor | 见下表（4 核查点中 1 处漂移） |

### inventory 维度 15 owner-doc 抽样核查

| # | owner doc : section | 断言 | 代码位置 | 结论 |
|---|---------------------|------|----------|------|
| 1 | `state-machine.md` §1 库存移动状态 | 4 态：DRAFT/CONFIRMED/DONE/CANCELLED | `app-erp-inventory.orm.xml:37-41` dict `erp-inv/move-status` option value 与 code 合一 + `ErpInvDocStatus.java:20-23`（`DOC_STATUS_*` 全 4 项逐一对应） | ✅ 一致 |
| 2 | `state-machine.md` §盘点单状态机（独立） | 4 态：DRAFT/**COUNTING**/DONE/CANCELLED | `app-erp-inventory.orm.xml:695` column `docStatus ext:dict="erp-inv/move-status"`（盘点单复用 move-status dict，4 态 DRAFT/**CONFIRMED**/DONE/CANCELLED） | ❌ **Minor 漂移**（owner doc `state-machine.md §盘点单状态机` 用 `COUNTING` 命名「盘点中」，但代码实际用 `CONFIRMED`，详见 P2-MA1-025） |
| 3 | `state-machine.md` §1 状态定义影响 | DONE 时写不可变流水 + 更新余额 + 释放预留 | `StockMoveBookkeeper.bookCompletion` 实现（grep 实测） | ✅ 一致 |
| 4 | `state-machine.md` §冲销路径 | 已完成单的纠错只能冲销（生成反向新单），不能反审核 | `ErpInvStockMoveBizModel` 无 reverseApprove 到 CONFIRMED 的迁移；冲销经 `ErpInvConstants.MOVE_DIRECTION` 反向新单 | ✅ 一致 |

## 6. finding 分级汇总

### 6.1 P0（即时通道 / 异步注入 fix plan）：1 项

#### `P0-MA1-021` inventory — `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed` 跨模块写 ErpFinVoucher 绕过 I\*Biz（业财一体写绕过 I\*Biz）

- **位置**：`module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/posting/CostAdjustmentPostingDispatcher.java:121-141`
- **现象**：成本调整 reversal 时，inventory 直接跨模块**写** finance 凭证：

  ```java
  private void markOriginalVoucherReversed(String billHeadCode) {
      IEntityDao<ErpFinVoucherBillR> linkDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
      // ... findAllByQuery 查 link ...
      IEntityDao<ErpFinVoucher> voucherDao = daoProvider.daoFor(ErpFinVoucher.class);
      for (ErpFinVoucherBillR link : links) {
          ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());
          if (... POSTED && !isReversed && postingType==NORMAL ...) {
              voucher.setIsReversed(true);
              voucherDao.updateEntity(voucher);   // ← 跨模块写！绕过 IErpFinVoucherBiz
          }
      }
  }
  ```

- **违反规则**：
  1. skill 维度 2「实体不做跨模块写操作（写走 BizModel 的 @BizMutation）」
  2. skill 维度 2「跨实体访问通过 @Inject I\*Biz 接口」
  3. AGENTS.md「跨实体访问」+ plan §保护区域门控「inventory 库存（业财一体写）」
  4. plan Phase 2 P0 类别「业财一体写绕过 I\*Biz」
- **功能风险**（高于平台合规）：仅设置 `isReversed=true` 标志，**未生成红字冲销凭证**，破坏 finance 凭证审计轨迹——`IErpFinVoucherBiz.reverse(billHeadCode, businessType)` 会同时生成 posted 红字凭证并原子更新原凭证 `isReversed`，本路径绕过了此业务逻辑
- **DAG 方向分析**：inventory→finance 是业财一体写正确方向（非"跨模块写反向"），但绕过 I\*Biz 仍属 P0（plan 明示「业财一体写绕过 I\*Biz」为 P0 类别，独立于 DAG 方向判定）
- **修复路径**（fix plan 内执行）：
  - 选项 A（推荐）：替换为 `IErpFinVoucherBiz.reverse(billHeadCode, ErpFinBusinessType.COST_ADJUSTMENT)`，由 finance 走完整红冲流程（生成红字凭证 + 原凭证置 `isReversed`）
  - 选项 B：若仅需置标志不生成红字，则在 `IErpFinVoucherBiz` 新增 `markReversed(voucherId, ...)` I\*Biz 写方法，inventory 调用此 I\*Biz 而非 daoFor
- **保护区域门控**：触及 finance 凭证（accounting 保护区域）→ 需 owner doc `docs/design/finance/posting.md` 描述预期行为 + 人工/任务驱动授权 + 该修复子切片独立 plan-audit + closure-audit
- **处置**：**异步注入 fix plan**（plan Phase 2 明示合法路径）；P0 永不进入 MR。建议 fix plan 命名 `docs/plans/YYYY-MM-DD-HHmm-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`，先于 MR1 执行。
- **状态**：`fix-plan-required (protected area gate)`

### 6.2 P1（MR1 批量）：1 项（跨域合并，接续 MA1 序号 → P1-MA1-022）

#### `P1-MA1-022` pur/sal/ast/inv — 跨模块只读访问经 `IDaoProvider.daoFor(Other*)` 而非 I\*Biz（Major，跨域通用模式）

- **位置**（共 ~14 个文件，本审计四域合并登记；与 P1-MA1-016 同根因）：
  - **purchase**：`ErpPurOrderProcessor.java:302 daoFor(ErpMdSubject)` / `:314 daoFor(ErpFinAccountingPeriod)`；`ErpPurPaymentProcessor.java:228 daoFor(ErpMdSubject)` / `:240 daoFor(ErpFinAccountingPeriod)`；`ErpPurDashboardBizModel` 7 处 `daoFor(ErpMdPartner)`（facade 聚合，javadoc 声明设计选择）
  - **sales**：`ErpSalOrderProcessor.java:377 daoFor(ErpMdSubject)` / `:389 daoFor(ErpFinAccountingPeriod)`；`ErpSalDashboardBizModel` 2 处 `daoFor(ErpMdPartner)`（facade 聚合）
  - **assets**：`ErpAstDepreciationScheduleProcessor.java:290 daoFor(ErpFinAccountingPeriod)`；9 个 posting dispatcher（DisposalPostingDispatcher / CapitalizationPostingDispatcher / DepreciationPostingDispatcher / ValueAdjustmentPostingDispatcher / MaintenanceCapitalizationPostingDispatcher / MaintenanceExpensePostingDispatcher / AssetInventoryPostingDispatcher / AssetSplitPostingDispatcher / AssetMergePostingDispatcher）`daoFor(ErpMdSubject)` 解析科目；`ErpAstDashboardBizModel` facade
  - **inventory**：`ErpInvLandedCostProcessor.java:267,473,477 daoFor(ErpPurReceive/ErpPurReceiveLine)`；`StandardCostResolver.java:99 daoFor(ErpMdMaterial)`；`CostMethodResolver.java:61,70 daoFor(ErpMdMaterial/MdAcctSchema)`；`CostAdjustmentService.java:291 daoFor(ErpMdMaterial)`；`ErpInvDashboardBizModel` facade
- **现象**：跨模块**只读**查询（findAllByQuery / getEntityById / findFirstByQuery）经 `IDaoProvider.daoFor(OtherModuleEntity.class)` 而非 `I*Biz` 接口
- **与 P0-MA1-021 区分**：本项**仅读不写**，严重性 Major（与 P1-MA1-016 finance 读 assets 同根因、同 severity）；P0-MA1-021 是**写**，严重性 P0
- **Dashboard facade 处置**：4 个 `ErpXxxDashboardBizModel` 的 IDaoProvider 用途是 read-only 内存聚合（javadoc 显式声明「注入 IDaoProvider/IOrmTemplate 经 QueryBean 过滤后内存聚合」），与 S 级 finance/mfg Dashboard facade 同模式（A1.11 已接受）。本审计认定 facade read-only 聚合**可接受**（I\*Biz 不天然承载跨域聚合查询语义）；MR1 仅裁决 Processor/Dispatcher 中跨域只读站点是否迁移至 I\*Biz（如 `IErpMdSubjectBiz.findByCode` / `IErpFinAccountingPeriodBiz.findByPeriod` 等只读便捷方法）
- **修复建议**（MR1 裁决）：
  - 方案 A（推荐）：在 master-data / finance 的 I\*Biz 接口补充便捷只读方法（`IErpMdSubjectBiz.requireByCode` / `IErpFinAccountingPeriodBiz.findActiveByDate` / `IErpPurReceiveBiz.loadByCode` 等），Processor/Dispatcher 迁移调用
  - 方案 B：永久接受为域内 Helper 合法模式（与 S 级 finance Helper 处置一致），登记 §设计例外
- **裁决状态**：MR1 待裁决（与 P1-MA1-016 同批次）
- **是否扩大抽样**：本审计已枚举全部跨域 daoFor 站点（14 个文件），未发现遗漏

### 6.3 P2（Minor / watch-only）：4 项

#### `P2-MA1-023` assets — `state-machine.md §1` 缺 DISPOSED 资产状态（Minor）

- **位置**：`docs/design/assets/state-machine.md:13-22`（资产状态定义表 5 态）+ `:28-34`（迁移图 5 态）
- **现象**：owner doc `state-machine.md §1` 列出资产 5 态（DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD），但代码 `app-erp-assets.orm.xml:66-72` dict `erp-ast/asset-status` 含 6 态（多 `DISPOSED`——内部处置，与 SCRAPPED/SOLD 并列终态）。`DISPOSED` 状态在兄弟 owner doc `split-merge.md:103` 已详细文档化（「新增 DISPOSED，语义=已拆分/合并内部处置，账面净值归零」），但 `state-machine.md` 漏更新。
- **影响**：owner doc 内部不一致；审计者读 `state-machine.md` 时困惑「为何代码 6 态文档 5 态」。无运行时影响。
- **处置**：watch-only，MR1 顺手更新 `state-machine.md §1` 状态定义表（追加 DISPOSED 行 + §2 迁移图标注拆分/合并触发 DISPOSED）。引用 `split-merge.md:103-104` 的语义边界裁决（DISPOSED = 内部重组无损；SCRAPPED/SOLD = 对外处置有损益）。

#### `P2-MA1-024` assets — `state-machine.md §折旧计划条目状态` 缺 CANCELLED（Minor）

- **位置**：`docs/design/assets/state-machine.md:143-148`（折旧计划条目状态表 3 态）
- **现象**：owner doc 列出折旧计划条目 3 态（PENDING/EXECUTED/REVERSED），代码 `app-erp-assets.orm.xml:84-88` dict `erp-ast/depreciation-schedule-status` 含 4 态（多 `CANCELLED`）。
- **影响**：owner doc 内部不一致；无运行时影响。
- **处置**：watch-only，MR1 顺手更新 `state-machine.md §折旧计划条目状态` 表追加 CANCELLED 行。

#### `P2-MA1-025` inventory — `state-machine.md §盘点单状态机` 用 COUNTING 而代码用 CONFIRMED（Minor）

- **位置**：`docs/design/inventory/state-machine.md:146-156`（盘点单状态机图用 `COUNTING`）
- **现象**：owner doc `state-machine.md §盘点单状态机` 描述「开始盘点 → 盘点中 (COUNTING)」，但代码 `app-erp-inventory.orm.xml:695` `ErpInvStockTake.docStatus` 实际使用 dict `erp-inv/move-status`，其中「盘点中」对应值是 `CONFIRMED`（盘点单复用 move-status 字典）。兄弟 owner doc `ui-patterns.md:136` 表述正确：「盘点单状态基于 `erp-inv/move-status` 字典：DRAFT(草稿) → CONFIRMED(盘点中) → DONE(已完成) → CANCELLED(已取消)」。
- **影响**：owner doc 内部不一致（state-machine.md vs ui-patterns.md）；无运行时影响。
- **处置**：watch-only，MR1 顺手更新 `state-machine.md §盘点单状态机` 将 `COUNTING` 改为 `CONFIRMED`（与代码 + ui-patterns.md 对齐）。

#### `P2-MA1-026` purchase — `scorecard-status` column defaultValue="10" 与 dict 不符（Minor，D1 残留）

- **位置**：`module-purchase/model/app-erp-purchase.orm.xml:451` column `status ext:dict="erp-pur/scorecard-status" defaultValue="10"`
- **现象**：dict `erp-pur/scorecard-status` 选项 value 为 `DRAFT`/`FINALIZED`（string），但 column defaultValue="10"（残留 int 数值）。属 D1（字典 valueType int→string 已知 deferred）的复现。
- **处置**：watch-only，**不重复裁决 D1**（plan Non-Goals 明示）；触发条件不变（业财一体打通前/跨系统集成启动时统一收敛）。MR1 D1 整体修复时一并处理。

## 7. 范围内 deferred 复核（不重复裁决）

### D1 字典 valueType int→string

本审计复核四域全部 dict 定义。新发现 1 处 D1 复现：`purchase scorecard-status defaultValue="10"`（P2-MA1-026，登记但不重复裁决）。**D1 状态确认：watch-only residual**，触发条件不变。

### assets propId 缺失（P1-MA1-008）

本审计复核 assets 29 列 propId 缺失对维度 1（决策顺序 Model→Delta→Java）与维度 13（codegen 产物安全）的影响：
- propId 缺失是 ORM 层机械维度问题（A1.5 已登记 MR1），不影响 service/dao/web 层平台合规
- codegen 增量再生会自动按文档顺序补全，对 `mvn clean install -DskipTests` 无破坏性影响（已实测当前 build/test 全绿）
- **状态确认：watch-only residual**（A1.5 已登记 deferred successor）

### UC-SAL-10 / UC-INV-08 并发缺口（归 MA2 A2.17）

本审计复核：
- `ErpInvStockBalance` `version` 列存在（乐观锁基础具备，dim 4 平台规范层面合规）
- UC-SAL-10 并发扣批次 / UC-INV-08 乐观锁缺口属业务正确性范畴，归 MA2 A2.17 裁决
- 本审计仅核验事务边界平台规范（@BizMutation 事务包装、@Version 注解存在性），不裁决并发正确性
- **状态确认：out-of-scope improvement**（plan §Deferred But Adjudicated 已明示）

## 8. 残留风险

1. **P0-MA1-021 修复前的运行时风险**：inventory 成本调整 reversal 路径在 fix plan 完成前仍直接写 finance 凭证 `isReversed=true` 而不生成红字凭证。建议 fix plan 优先级最高（先于 MR1）；fix plan 完成前应避免在生产环境触发成本调整 reversal（功能测试仍可运行，因测试用例断言可能容忍此行为）。
2. **跨域只读 IDaoProvider 模式（P1-MA1-022）**：四域 Processor/Dispatcher 普遍采用 daoFor 跨域只读，与 P1-MA1-016 同根因。MR1 裁决时若选择迁移至 I\*Biz，需在 master-data / finance 的 I\*Biz 补充便捷只读方法（避免 I\*Biz 接口爆炸式增长）。Dashboard facade read-only 聚合可永久接受。
3. **per-mutation Processor 抽象基类 R8 排除**：四域 per-mutation Processor 文件均正确继承 `Abstract*Processor<T>`（plan 2026-07-25-1057-2 校准后的合规模式）。残留风险：若未来手写 Processor 不继承抽象基类，checker R8 会漏排除（与 S 级同风险，已在 baseline 注记登记 successor）。
4. **nop-message 业务事件契约完整性**：四域内 nop-message 直接 import 较少（grep 0 hits），主要经 `IErpFinAcctDocProvider` 派发机制实现业财打通；事件契约完整性归 MA2 业财端到端审计范围。
5. **inventory 无 nop-wf 直接依赖**：设计合理（库存移动不需独立审批流，由业务单据触发），但若未来需要库存调整审批（如负库存审批），需引入 nop-wf。归 Follow-up。

## 9. 结论

- **裁决**：**条件性失败**（59/60 维度合规；P0=1；P1=1；P2=4）。
- **A 级核心四域平台合规基线整体良好**：前序系统性偏差修复（D2 BizModel 安全 API、D5 移除多余 IOrmTemplate、R3/R7 收敛、F1 daoFor Type 1 重构、R6/R10 javadoc 假阳性排除）效果显著，机械化规则全绿。
- **唯一 P0 阻塞**：inventory `CostAdjustmentPostingDispatcher.markOriginalVoucherReversed` 跨模块写 finance 凭证（业财一体写绕过 I\*Biz）。该修复触及 finance 保护区域，**异步注入 fix plan**（plan 合法路径），需先于 MR1 执行 + 独立 plan-audit + closure-audit。
- **A1.12 完成判定**：本审计产出已识别全部 P0/P1/P2 finding。MA2 业务正确性审计对实现层平台合规的依赖**部分解除**——P0-MA1-021 修复完成后方可完全解除（MA2 业财端到端 A2.1/A2.2/A2.4 + 库存核算 A2.4 + 折旧引擎 A4.3 触及此写路径）。后续 A1.13（B+C 合并）按 roadmap 推进。
- **scope matrix §2.1 标记**：pur=`✅`（仅 D1 P2 watch-only，无 P1）/ sal=`✅`（全维度通过）/ assets=`⚠️(P2)`（2 项 P2 owner-doc drift + P1 合并 P1-MA1-022，无 P0）/ inventory=`⚠️(P0)`（P0-MA1-021 待 fix plan + P1 合并）。
