# MA1 平台合规审计 — finance + manufacturing + hr（S 级三域，A1.11）

> 报告日期：2026-07-27
> 来源 plan：`docs/plans/2026-07-27-1227-2-audit-remediation-ma1-platform-conformance-s-tier.md`
> Skill：`docs/skills/nop-platform-conformance-audit-prompt.md`（15 维度语义审计 + 自动化 grep）
> 范围：`module-finance/`、`module-manufacturing/`、`module-hr/` 三域 `{domain}-{service,dao,web,meta,app}` Java + `*.orm.xml` + `*.xbiz.xml` + `docs/design/{finance,manufacturing,human-resource}/`
> 锚点：`compliance-baseline.md §M0 锚点注记`（HEAD=0e963531d，落锚日 compliance checker 全 19 规则 actual ≤ baseline，0 漂移）

## 0. 执行摘要

| 域 | 维度合规率（15/15） | 反模式实例 | P0 | P1 新增 | P2 新增 |
|----|---------------------|-----------|-----|---------|---------|
| finance | 15/15（含 1 处 Major 漂移登记） | 1 javadoc-only 假命中 + 1 LocalDate.now + 1 IllegalArgumentException | 0 | **1**（P1-MA1-018） | 1（P2-MA1-019） |
| manufacturing | 15/15 | 0 | 0 | 0 | 0 |
| hr | 15/15（含 1 处 Minor 漂移登记） | 0 | 0 | 0 | 1（P2-MA1-020） |
| **合计** | **45/45** | 3 | **0** | **1** | **2** |

**裁决：通过**（三域平台合规基线良好；前序 D2/D5 系统性偏差修复 + compliance checker 19 规则基线锚定效果显著；仅 1 项 P1 + 2 项 P2 待 MR1/watch-only）。三域所有跨模块外部实体引用均经机制 B（notGenCode）正确声明（与 A1.10 跨模块依赖审计结论一致）。无 P0 即时通道触发。

## 1. 自动化 grep 扫描结果

### 1.1 grep 反模式扫描（机械化规则）

| 规则 | finance | mfg | hr | 处置 |
|------|---------|-----|----|------|
| `extends RuntimeException` | 0 | 0 | 0 | ✅ R4 基线 0 |
| `@Inject private`（IoC 失败） | 0 | 0 | 0 | ✅ R5 基线 0 |
| `@Transactional` 与 `@BizMutation` 真实共存 | 2（post/reverse Facade） | 0 | 0 | ✅ 已登记 R6/R10 基线（plan 2026-07-27-0823-1），owner doc `processor-extension-pattern.md` 硬规则 1 背书（跨域失败隔离独立事务），代码注释 `nop-check: allow @Transactional(REQUIRES_NEW)` |
| `System.currentTimeMillis()` | 0 | 0 | 0 | ✅ R7 基线 0（plan 2026-07-24-0941-2 已收敛） |
| `LocalDate\.now\(\)` / `LocalDateTime\.now\(\)` | **1**（`ErpFinVoucherTemplateBizModel:95`） | 0 | 0 | ⚠️ P2-MA1-019 关联（非 R7 基线范围，但同性质——应 `CoreMetrics.currentDate()`） |
| `_gen/` 手改（git diff） | 0（工作树 clean） | 0 | 0 | ✅ |
| `__XGEN_FORCE_OVERRIDE__` 手改 | 0（仅出现在 codegen 输出 `_vfs/dict/*.dict.yaml`，git clean 验证未手改） | 0 | 0 | ✅ |
| 跨模块 `refEntityName` 无 notGenCode 声明 | 0（finance 12 处全部声明） | 0（mfg 9 处全部声明） | 0（hr 7 处全部声明） | ✅ 与 A1.10 结论一致 |
| `IDaoProvider` 直接注入（应 I\*Biz） | 多处（fin 银行对账/期末结账/坏账/汇兑等内部 Helper） | 0 处违规 | 0 | ✅ 全部为**域内** Helper（非 BizModel）的合法 DAO 访问；跨域 daoFor 反向写已由 A1.10 + R2c 基线统一治理，本审计未发现新增违规 |

### 1.2 误报澄清

`@Transactional + @BizMutation` 共存的 grep 命中文件（fin 7 + mfg 4 = 11 个文件）经逐文件核实，**真实代码命中仅 2 处**（`ErpFinVoucherBizModel.java:71,79` 的 post/reverse Facade），其余 9 个文件均为 javadoc/类注释中描述事务边界策略的文本（如"事务边界：跟随 Facade `@BizMutation` 事务，本类不带 `@Transactional`"），属 grep 机械化匹配的固有假阳性。checker 已通过 plan 2026-07-27-0823-1 的注释排除校准（R6/R10 grep 管道追加 `grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*|\*/)'`）吸收。

## 2. finance 15 维度审计

### 维度合规表

| # | 维度 | 结论 | 备注 |
|---|------|------|------|
| 1 | 决策顺序（Model→Delta→Java） | ✅ | 源模型 `module-finance/model/app-erp-finance.orm.xml`（331→438 java 文件，含 `_gen/`）；保留层定制文件存在（`ErpFinAccountingPeriod.xmeta` 等）；Java 仅承载不可模型化行为（GL Mapping 缓存/过账引擎） |
| 2 | 跨实体访问规则 | ✅ | `ReconciliationSettler`/`PartnerBalanceUpdater`/`BankRecon*Builder` 等均为**域内 Helper**（非 BizModel），DAO 访问合法；已知 P1-MA1-016（`ErpFinAccountingPeriodProcessor.reverseDepreciation` 跨域 daoFor assets）已登记 MR1，本审计不重复裁决 |
| 3 | 异常处理 | ✅ | 138 处 `throw new NopException`；ErrorCode 集中于 `ErpFinErrors` + `ErpFinPostingErrors`；**1 处 `IllegalArgumentException`**（`ErpFinBusinessType.fromCode:86`，programmer-error 路径）→ P2-MA1-019 |
| 4 | IoC 与事务 | ✅ | 0 `@Inject private`；2 处 `@Transactional(REQUIRES_NEW)` 已登记 R6/R10 基线（owner doc 背书）；`@BizMutation` 不冗余 `@Transactional` |
| 5 | 平台辅助工具 | ✅ | `CoreMetrics.currentTimeMillis()` 全域采用；Jackson 仅出现在 `*-api/beans/` 自动生成 Bean 类的 `@JsonInclude` 注解（codegen 产物，无直接 JSON 序列化调用）；**1 处 `LocalDate.now()`** → P2-MA1-019 |
| 6 | 标准服务模式 | ✅ | 36/40 BizModel `extends CrudBizModel<T>`；4 个非 CRUD 为 facade（`ErpFinDashboardBizModel`/`ErpFinReportBizModel`/`ErpFinBudgetCommitmentBizModel`/`ErpFinIntercompanyTransferBizModel`） |
| 7 | 机制 B（跨模块外部实体引用） | ✅ | 12 处跨模块 refEntityName 全部 `<entity notGenCode="true">` 声明（subject/acct_schema/currency/partner/employee/cost_center/organization/warehouse/material/material_category/project/asset） |
| 8 | 状态机与规则引擎 | ✅ | 凭证状态机声明式（dict + 常量，3 态 DRAFT/POSTED/CANCELLED）；GL 映射采用**自定义优先级链 Resolver**（`IErpFinGlMappingResolver` + 进程内缓存 + TTL 降级），是 owner doc `gl-mapping-rules.md §3` 显式设计的 nop-rule 替代方案（设计要点 README.md:125「规则引擎或元数据驱动」二者择一），非遗漏；预算结转规则引擎采用 dict 驱动的 4 规则选择（REMAINING_FULL/REMAINING_RATIO/USED_FULL/NONE） |
| 9 | 审批流与作业 | ✅ | pom 依赖 `nop-wf-core` + `nop-wf-meta`；`ErpFinCashForecastBizModel` javadoc 明示「nop-job 定时调度归 Follow-up」；nop-message 跨域事件经 `IErpFinAcctDocProvider` 派发 |
| 10 | 定制能力顺序 | ✅ | Delta 保留层文件齐全（`ErpFin*.{xmeta,view.xml,main.page.yaml,picker.page.yaml,lib.xjs}` + `_ErpFin*.xmeta` + `_ErpFin*.xbiz` + `ErpFin*.xbiz`） |
| 11 | 多租户与本地化 | ✅ | 源 orm.xml 无 `tenantId` 列（平台标准管理）；中国本地化金税模块独立为 `module-l10n-cn`（设计阶段，app.action-auth.xml 已预留 `erp-l10n-cn` TOPM） |
| 12 | 测试 | ✅ | 64 test 文件，57 个 `JunitAutoTestCase`（覆盖 GL 映射 8 场景 + PurInvoice 过账 3 场景 + 业财端到端） |
| 13 | codegen 产物安全 | ✅ | 工作树 clean；`__XGEN_FORCE_OVERRIDE__` 标记的 `_vfs/dict/*.dict.yaml` 经 git status 验证未手改；`_app.orm.xml`/`_service.beans.xml` 由 `mvn clean install -DskipTests` 增量再生（不手写） |
| 14 | 聚合完整性 | ✅ | `app-erp-all/.../auth/app.action-auth.xml:9` 聚合 `/erp/fin/auth/erp-fin.action-auth.xml`（保留层文件，无 `_` 前缀）；聚合器 POM 含 `erp-fin-app` 依赖 |
| 15 | owner-doc → 代码漂移抽样 | ⚠️ 1 Major | 见下表 |

### finance 维度 15 owner-doc 抽样核查

| # | owner doc : section | 断言 | 代码位置 | 结论 |
|---|---------------------|------|----------|------|
| 1 | `state-machine.md` §1.1 凭证状态定义 | DRAFT / POSTED / CANCELLED 三态 | `ErpFinConstants.java:160,161,296`（`VOUCHER_STATUS_DRAFT="DRAFT"` / `_POSTED="POSTED"` / `_CANCELLED="CANCELLED"`） | ✅ 一致 |
| 2 | `state-machine.md` §3 终态 | POSTED / CANCELLED 终态不可恢复 | 同上常量集 + BizModel 无 reverse 状态迁移 | ✅ 一致 |
| 3 | `posting.md` §业务类型映射 | enum `code` 与 dict `erp-fin/business-type` 数值**逐一一致** | `ErpFinBusinessType.java` enum 名 vs `app-erp-finance.orm.xml` dict value：**4 项 enum 名 ≠ dict value**（详见 P1-MA1-018） | ❌ **Major 漂移**（扩大抽样触发） |
| 4 | `posting.md` §总体架构 | 默认 SYNC 模式（方式 A） | 全域 AcctDocProvider 经 SYNC 过账（无 ASYNC 切换配置） | ✅ 一致 |
| 5（扩） | `posting.md` §PostingEvent 契约 | `businessType` 类型为 `ErpFinBusinessType` enum | `IErpFinVoucherBiz.post(PostingEvent, ...)` 强类型 + `ErpFinBusinessType.X ==` 比较 | ✅ 一致 |
| 6（扩） | `gl-mapping-rules.md` §3 优先级链 | (priority DESC, 维度具体度 DESC) 排序匹配 | `ErpFinGlMappingResolver.java:100-` 候选排序实现 | ✅ 一致 |

## 3. manufacturing 15 维度审计

### 维度合规表

| # | 维度 | 结论 | 备注 |
|---|------|------|------|
| 1 | 决策顺序 | ✅ | 源模型 + 保留层定制 + Java 仅承载行为 |
| 2 | 跨实体访问 | ✅ | 无新增跨域 daoFor 违规 |
| 3 | 异常处理 | ✅ | 67 处 NopException + 0 IllegalArgumentException；`ErpMfgErrors` 集中管理（ERR_BOM_NOT_FOUND/ERR_DEFAULT_BOM_NOT_FOUND/ERR_BOM_CYCLE 等） |
| 4 | IoC 与事务 | ✅ | 0 `@Inject private`；grep 命中的 4 个 Processor 文件全部为类注释 javadoc 文本（无真实 `@Transactional`） |
| 5 | 平台辅助工具 | ✅ | 0 `System.currentTimeMillis`；0 `LocalDate.now`；Jackson 仅限 codegen Bean 注解 |
| 6 | 标准服务模式 | ✅ | 31/33 BizModel `extends CrudBizModel<T>`；2 个非 CRUD 为 facade（`ErpMfgDashboardBizModel`/`ErpMfgReportBizModel`） |
| 7 | 机制 B | ✅ | 9 处跨模块 refEntityName 全部 `notGenCode="true"`（material/material_sku/uom/warehouse/location/organization/currency/partner/batch/employee） |
| 8 | 状态机与规则引擎 | ✅ | 工单状态机采用**声明式 AbstractApproveProcessor 派生模式**（per-mutation Processor 文件 plan 2026-07-25-1057-2），非 if-else；工单 10 态 + JobCard 生命周期与 `state-machine.md` 一致；MRP/CRP/BOM 计算引擎使用平台辅助工具 + dict 驱动参数 |
| 9 | 审批流与作业 | ✅ | pom 依赖 nop-wf；`ErpMfgCrpRunJob` 经 nop-job-local scheduler.yaml 调度；`ErpMfgConstants.CONFIG_JOBCARD_AUTO_GENERATE_*` 配置项 |
| 10 | 定制能力 | ✅ | Delta 保留层 xmeta/xbiz/view 文件齐全 |
| 11 | 多租户与本地化 | ✅ | 无 tenantId 列；无内建本地化 |
| 12 | 测试 | ✅ | 30 test 文件（28 JunitAutoTestCase） |
| 13 | codegen 产物安全 | ✅ | 工作树 clean |
| 14 | 聚合完整性 | ✅ | app.action-auth.xml:12 聚合 `/erp/mfg/auth/erp-mfg.action-auth.xml` |
| 15 | owner-doc → 代码漂移 | ✅ | 见下表 |

### manufacturing 维度 15 owner-doc 抽样核查

| # | owner doc : section | 断言 | 代码位置 | 结论 |
|---|---------------------|------|----------|------|
| 1 | `state-machine.md` §1.1 工单状态定义 | 10 态：DRAFT/SUBMITTED/NOT_STARTED/STOCK_PARTIAL/STOCK_RESERVED/IN_PROCESS/COMPLETED/STOPPED/CLOSED/CANCELLED | `ErpMfgConstants.java:30-39`（`WORK_ORDER_STATUS_*` 全部 10 项逐一对应） | ✅ 一致 |
| 2 | `mrp.md` §状态 | MRP 计划状态：DRAFT/RUNNING/COMPLETED/FIRMED/CANCELLED | `app-erp-manufacturing.orm.xml:72-77` dict `erp-mfg/mrp-status` + `ErpMfgConstants.java:91-95`（`MRP_STATUS_*`） | ✅ 一致 |
| 3 | `state-machine.md` §3 工单终态 | COMPLETED / CLOSED / CANCELLED 终态 | `ErpMfgWorkOrderApproveProcessor` 等 per-mutation Processor 通过 `AbstractApproveProcessor<T>` 派生实现 | ✅ 一致 |
| 4 | `bom-and-routing.md` | BOM 展开经标准 Processor 链 | `ErpMfgBomLine`/`ErpMfgRoutingOperation` 实体 + Processor 实现 | ✅ 一致 |

## 4. hr 15 维度审计

### 维度合规表

| # | 维度 | 结论 | 备注 |
|---|------|------|------|
| 1 | 决策顺序 | ✅ | 源模型 + 保留层 + Java 行为 |
| 2 | 跨实体访问 | ✅ | 无违规 |
| 3 | 异常处理 | ✅ | 68 处 NopException + 0 IllegalArgumentException；`ErpHrErrors` 集中管理（ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND 等） |
| 4 | IoC 与事务 | ✅ | 0 `@Inject private`；0 `@Transactional + @BizMutation` 真实共存 |
| 5 | 平台辅助工具 | ✅ | 0 `System.currentTimeMillis`；0 `LocalDate.now`；Jackson 仅限 codegen Bean 注解 |
| 6 | 标准服务模式 | ✅ | 36/37 BizModel `extends CrudBizModel<T>`；1 个非 CRUD（`ErpHrReportBizModel` facade） |
| 7 | 机制 B | ✅ | 7 处跨模块 refEntityName 全部 `notGenCode="true"`（organization/bank_account/cost_center/currency/project/task） |
| 8 | 状态机与规则引擎 | ✅ | LeaveRequest 5 态 + Timesheet 3 态声明式；薪酬扣税采用**累计预扣法**（owner doc `payroll.md §4` 7 级超额累进税率表）配置化（`ErpHrTaxConfig.taxBrackets` JSON），是 dict+config 驱动的 nop-rule 替代方案（owner doc 明示）；排班引擎经 `ErpHrShift*` 实体 + Processor 实现 |
| 9 | 审批流与作业 | ✅ | pom 依赖 nop-wf + nop-wf-service；`ErpHrContractExpiryJob` 经 nop-job-local 调度 |
| 10 | 定制能力 | ✅ | Delta 保留层齐全 |
| 11 | 多租户与本地化 | ✅ | 无 tenantId；薪酬本地化经 `ErpHrTaxConfig` 配置（中国累计预扣法） |
| 12 | 测试 | ✅ | 15 test 文件（15 JunitAutoTestCase） |
| 13 | codegen 产物安全 | ✅ | 工作树 clean；`_vfs/dict/erp-hr/*.dict.yaml` 含 `__XGEN_FORCE_OVERRIDE__` 标记但 git clean 验证未手改 |
| 14 | 聚合完整性 | ✅ | app.action-auth.xml:17 聚合 `/erp/hr/auth/erp-hr.action-auth.xml` |
| 15 | owner-doc → 代码漂移 | ⚠️ 1 Minor | 见下表 |

### hr 维度 15 owner-doc 抽样核查

| # | owner doc : section | 断言 | 代码位置 | 结论 |
|---|---------------------|------|----------|------|
| 1 | `state-machine.md` §1.1 LeaveRequest 状态定义 | 5 态：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED | `app-erp-hr.orm.xml:50-56` dict `erp-hr/leave-status` + `ErpHrConstants.java:115,116,226,227,228`（`LEAVE_STATUS_*`） | ✅ 一致 |
| 2 | `payroll.md` §审批状态标准化 | approvalStatus 拆为标准 `wf/approve-status` 4 态（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED） | `app-erp-hr.orm.xml:736` column `approveStatus ext:dict="wf/approve-status" defaultValue="UNSUBMITTED"` | ✅ 一致（column 正确） |
| 3 | `payroll.md` §审批状态标准化 | 原 6 态 approvalStatus（PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID）**已废弃** | `app-erp-hr.orm.xml:77-83` 仍定义 dict `erp-hr/salary-approval-status` 含废弃的 6 态（orphan dict，无 column 引用） | ❌ **Minor 漂移**（orphan dict 残留，详见 P2-MA1-020） |
| 4 | `state-machine.md` §3 LeaveRequest 终态 | APPROVED / REJECTED / CANCELLED 终态 | `ErpHrConstants.LEAVE_STATUS_*` 常量集 + BizModel 无逆向迁移 | ✅ 一致 |

## 5. finding 分级汇总

### 5.1 P0（即时通道）：0 项

无手改生成代码、无跨模块写反向、无业务异常不扩展 NopException、无 `@Inject private` 致 IoC 失败。三域无需即时修复。

### 5.2 P1（MR1 批量）：新增 1 项（接续 MA1 ORM 批次序号 → P1-MA1-018）

#### `P1-MA1-018` finance — `ErpFinBusinessType` enum 名 ↔ dict value 漂移（Major）

- **位置**：`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java:22-26,39` + `module-finance/model/app-erp-finance.orm.xml` dict `erp-fin/business-type`
- **现象**：4 项 enum 名与 dict value 不一致：

  | enum 名 (`ErpFinBusinessType.X.name()`) | dict value (`erp-fin/business-type` option) | 标签 |
  |---|---|---|
  | `MANUFACTURING_COST_CLOSE(100)` | `PRODUCTION_COST` | 生产成本结转 |
  | `PROJECT_COST_COLLECTION(110)` | `PROJECT_COST` | 项目成本归集 |
  | `PERIOD_CLOSE(120)` | `PERIOD_CLOSING` | 期末结转 |
  | `EXCHANGE_GAIN_LOSS(130)` | `FX_REVALUATION` | 汇兑损益 |

- **影响**：代码以 `ErpFinBusinessType.X.name()` 持久化到 `voucher_bill_r.businessType` 列（实测：`ExchangeRevaluationService.java:152,213`、`ProfitLossClosingService.java:152`、`ErpFinReportBizModel.java:370,372` 等），但 UI 字典下拉值显示 dict value。结果：
  1. UI 用户筛选「FX_REVALUATION」时，SQL `WHERE businessType='FX_REVALUATION'` 命中 0 行（实际存储为「EXCHANGE_GAIN_LOSS」）
  2. owner doc `posting.md §业务类型映射` 明示「常量的 `code` 与字典 `erp-fin/business-type` 的数值逐一一致」——文字断言与代码实现不符，长期累积会让 owner doc 失去参考价值
- **修复建议**（MR1 裁决二选一）：
  - **方案 A（推荐）**：把 enum 4 项重命名为 dict 当前 value（`MANUFACTURING_COST_CLOSE→PRODUCTION_COST` 等），保留旧名为 `@Deprecated` 别名兼容期。优点：UI/查询一致性优先，dict 是稳定持久层真相。需配套全仓 import 替换 + 测试。
  - **方案 B**：把 dict 4 项 value 改为 enum 当前名（`PRODUCTION_COST→MANUFACTURING_COST_CLOSE` 等），同时迁移已持久化的列值（数据迁移脚本）。优点：enum 编译期稳定。缺点：破坏既有数据的字典语义。
- **裁决状态**：MR1 待裁决（与 P1-MA1-016 finance IDaoProvider 同域同批次）
- **是否扩大抽样**：本审计已按 skill 规则「单域 ≥2 处漂移扩大抽样」执行扩大（finance 共抽样 6 个核查点），未发现额外 Major 漂移

### 5.3 P2（Minor / watch-only）：新增 2 项

#### `P2-MA1-019` finance — `ErpFinBusinessType.fromCode` 抛 `IllegalArgumentException`（Minor）

- **位置**：`module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java:86`
- **现象**：`throw new IllegalArgumentException("Unknown ErpFinBusinessType code: " + code)` 违反 skill 异常处理规则「所有业务异常扩展 `NopException`，不用 `IllegalArgumentException`」
- **严重性降级理由**：programmer-error 路径（无效 int code 传入静态 lookup），非 GraphQL 面向错误；R4 基线聚焦 `extends RuntimeException`，`IllegalArgumentException extends RuntimeException` 但本场景不构成业务异常
- **关联**：同一文件 `ErpFinVoucherTemplateBizModel.java:95` 的 `LocalDate.now()` 是 R7 同性质残留（应 `CoreMetrics.currentDate()`），合并登记
- **处置**：watch-only，MR1 顺手收敛或永久接受（非阻塞）

#### `P2-MA1-020` hr — 残留 orphan dict `erp-hr/salary-approval-status`（Minor）

- **位置**：`module-hr/model/app-erp-hr.orm.xml:77-83` dict 定义 + `module-hr/erp-hr-meta/.../i18n/en/_erp-hr.i18n.yaml:923,1008` i18n
- **现象**：dict `erp-hr/salary-approval-status` 仍含 owner doc `payroll.md §审批状态标准化` 明示已废弃的 6 态（PENDING/REVIEWED/APPROVED_FINANCE/APPROVED_MANAGER/PAID/VOID）；实际 `ErpHrSalary.approveStatus` 列正确引用 `wf/approve-status`（4 态）。该 dict 是 orphan——无任何 column 引用，仅在 i18n + `ErpHrConstants.java:5` javadoc 中残留引用
- **影响**：纯文档/代码冗余，无运行时影响；但与 owner doc 决策不一致，长期会让审计者困惑「为何 dict 还在」
- **处置**：watch-only，MR1 顺手清理（删除 orm.xml 中 orphan dict 定义 + i18n 条目 + javadoc 引用）

## 6. 残留风险

1. **GL Mapping Resolver 自定义实现 vs nop-rule**：finance 选择自定义优先级链 Resolver 而非 nop-rule DSL，是 owner doc 显式裁决（`gl-mapping-rules.md §3` + README.md:125「规则引擎或元数据驱动」二选一）。本审计认定合规，但若未来 nop-rule 平台层能力增强（如可视化规则编辑器），可能需要重新评估迁移成本。归 Follow-up。
2. **跨域 daoFor 残留**：本审计复核 R2c 基线 1228 处中的 finance/mfg/hr 域内站点，确认域内 Helper 合法；跨域 daoFor（如 P1-MA1-016 `ErpFinAccountingPeriodProcessor.reverseDepreciation`）已在 A1.10 登记 MR1。本审计不重复裁决。
3. **per-mutation Processor 抽象基类 R8 排除**：本审计期间三域 per-mutation Processor 文件均正确继承 `Abstract*Processor<T>`（plan 2026-07-25-1057-2 校准后的合规模式）。残留风险：若未来手写 per-mutation Processor 不继承抽象基类，checker R8 会漏排除——已在 baseline 注记登记 successor。
4. **NOP 业务事件（nop-message）**：三域内 nop-message 直接 import 较少（grep 0 hits），主要经 `IErpFinAcctDocProvider` 派发机制实现业财打通；本审计未深查事件契约完整性，归 MA2 业财端到端审计范围。

## 7. 范围内 deferred 复核（不重复裁决）

### D1 字典 valueType int→string

本审计复核三域全部 dict 定义，未发现新的 int valueType 偏离（前序 plan 2026-07-02-0900-1 已统一为 string）。**D1 状态确认：watch-only residual**，触发条件不变（业财一体打通前/跨系统集成启动时）。

### hr 双员工表（`erp_hr_employee` vs `erp_md_employee`）

本审计复核 hr.orm.xml，`ErpHrEmployee` 实体仍存在（设计独立的员工生命周期/组织/薪酬实体，与 master-data 的 `ErpMdEmployee` 弱关联经 `employeeId` 弱指针）。**状态确认：watch-only residual**（MA1 ORM 审计 A1.3 已登记 deferred successor）。

## 8. 结论

- **裁决**：**通过**（45/45 维度合规；P0=0；P1=1；P2=2）
- **三域 Nop 平台合规基线良好**：前序系统性偏差修复（D2 BizModel 安全 API、D5 移除多余 IOrmTemplate、R3/R7 收敛、F1 daoFor Type 1 重构）效果显著，机械化规则全绿，语义抽样仅 1 项 Major 漂移（业务类型 enum ↔ dict 命名不一致）+ 2 项 Minor 残留。
- **A1.11 完成判定**：MA2 业务正确性审计对实现层平台合规的依赖**已解除**。后续 A1.12（A 级核心域平台合规）+ A1.13（B+C 合并）按 roadmap 推进。
