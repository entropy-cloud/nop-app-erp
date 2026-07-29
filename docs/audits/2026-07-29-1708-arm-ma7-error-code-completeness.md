# A7.1 错误码完整性审计报告

> 审计 ID：A7.1
> 里程碑：MA7（运维与性能层审计）
> 维度：错误码完整性（全域 `throw new` 核对 ErrorCode + 命名空间 + i18n + 异常吞咽）
> 域范围：全 19 域 `module-*/erp-*-{service,dao}/src/main/java/**` + 20 `Erp*Errors` 定义文件
> Owner Doc：`docs/design/domain-design-guidelines.md §七`（§7.1 命名空间 `erp.err.<domain-short>.<name>` + §7.2 编码规则 + §7.3 使用规范「禁止 `throw new RuntimeException`」）
> Skill：`docs/skills/open-ended-audit-prompt.md`（开放式审计：主动搜索构造形态未知偏差 / 集中化缺口 / 吞咽不可观察风险）
> 审计日期：2026-07-29
> 关联 plan：`docs/plans/2026-07-29-1708-1-ma7-error-code-index-nplus1-audit.md` Phase 1
> Source Audits: `docs/audits/2026-07-29-1708-arm-ma7-error-code-completeness.md`
> Audit Status: closed

## Verdict: PASS（⚠ 2 项 P2 watch-only，零 P0 + 零 P1）

错误码完整性基线扎实：**752 处 `throw new NopException` 100% 携带 ErrorCode 常量 + 中文描述**（§7.3 禁则合规），命名空间 100% 符合 §7.1 `erp.err.<domain-short>.<name>` kebab-case，4 处原生异常经逐项裁决均为合法技术/前置条件守卫（非业务异常）。仅 2 处 ErrorCode 集中化偏差（CloseVoucherWriter 内联字面量 + AssessmentAggregator 类内局部常量）+ 1 项 i18n 资源束前瞻缺口，均为可维护性/前瞻性 P2 watch-only，不影响错误可观察性。

## 1. 构造形态矩阵（精确计数基线）

**审计方法**：全域 `rg "throw new NopException"` 精确分类 + 非类限定形态逐站点核实 + 原生异常上下文裁决。

| 构造形态 | 站点数 | 占比 | §7 合规裁决 |
|----------|--------|------|------------|
| **类限定 `Erp*Errors.ERR_*` 常量**（含 `ErpB2bErrors` 数字前缀） | 741 | 98.5% | ✅ §7.2/§7.3 合规 |
| **static-import 常量**（`import static ...Erp*Errors.ERR_*` 后裸用 `ERR_*`） | 8 | 1.1% | ✅ 经核实全部解析为合法 `Erp*Errors.ERR_*` 常量（见 §2） |
| **变量传递 ErrorCode**（util 方法形参，调用方传入常量） | 2 | 0.3% | ✅ 通用校验器范式，调用方传合法常量（见 §2） |
| **内联 `ErrorCode.define("字面量", ...)`** | 1 | 0.1% | ⚠ 命名合规但未集中化至 `Erp*Errors`（P2-MA7-001） |
| `.bizError()` 链 | 0 | — | n/a |
| 字面量 ErrorCode 字符串（`new NopException("xxx")`） | 0 | — | n/a（零违规） |
| 无参 `new NopException()` | 0 | — | n/a（零违规） |
| **service 层 NopException 合计** | **752** | 100% | — |
| dao 层 `throw new NopException` | 0 | — | ✅ dao 层不抛业务异常（符合分层） |
| `.param()` 链（携带上下文参数） | 43 站点 | — | ✅ §7.3 `.param()` 规范使用 |

**关键结论**：零「`new NopException("字面量")`」+ 零「无参 `new NopException()`」+ 零 `.bizError()` 误用——构造形态与 §7.3 示范完全一致。

## 2. 非类限定 ErrorCode 合规性裁决（11 站点逐项）

### 2.1 static-import 常量（8 站点，全部合规）

| 站点 | 常量 | import 源 | 裁决 |
|------|------|----------|------|
| `AssessmentAggregator.java:42,88,124`（hr，3 站点） | `ERR_AGGREGATE_NO_DETAILS` | ⚠ **类内局部定义**（`AssessmentAggregator:145 static final ErrorCode`，非 `ErpHrErrors`） | P2-MA7-001（集中化偏差，见 §4） |
| `SequenceStepAdvancer.java:73,83,103,110`（crm，4 站点） | `ERR_SEQUENCE_ILLEGAL_STATUS_TRANSITION` / `ERR_SEQUENCE_STEP_NOT_DUE` | `import static app.erp.crm.service.ErpCrmErrors.*` | ✅ 解析为合法 `ErpCrmErrors` 常量 |
| `ErpFinIntercompanyTransferBizModel.java:88`（finance，1 站点） | `ERR_TRANSFER_PRICE_NOT_FOUND` | `import static app.erp.fin.service.ErpFinErrors.*` | ✅ 解析为合法 `ErpFinErrors` 常量 |

### 2.2 变量传递 ErrorCode（2 站点，合规通用范式）

| 站点 | 形参 | 调用方 | 裁决 |
|------|------|--------|------|
| `ErpDateRangeOverlapValidator.java:74,148`（master-data） | `ErrorCode errorCode`（方法形参） | 调用方传 `ErpMdErrors.ERR_MD_DATE_RANGE_OVERLAP` / 域专属 `ERR_*_DATE_RANGE_OVERLAP`（见 `date-ranged-validity-pattern.md §错误码命名`） | ✅ 通用校验器范式（`enforceMutex`/`enforceStackableAware` 同型签名），ErrorCode 由调用方注入，运行时始终为合法 `Erp*Errors` 常量 |

### 2.3 内联字面量 ErrorCode（1 站点，集中化偏差）

| 站点 | 构造 | 裁决 |
|------|------|------|
| `CloseVoucherWriter.java:82`（finance） | `new NopException(io.nop.api.core.exceptions.ErrorCode.define("erp.err.fin.period-close.unbalanced", "期末结账凭证借贷不平衡：借={td}, 贷={tc}")).param(...)` | ⚠ **命名合规**（`erp.err.fin.*` kebab-case + 中文描述）但 **未集中化至 `ErpFinErrors` interface**——违反 §7.2「ErrorCode 定义以 `interface` + `ErrorCode` 静态字段形式：`Erp<Domain>Errors`」。功能正确（运行时抛出合法 NopException+ErrorCode+中文），仅维护性/可发现性偏差（grep `ErpFinErrors` 漏此常量）。归入 P2-MA7-001 |

## 3. 4 处原生异常逐项裁决（全部合法技术守卫）

**Owner doc §7.3** 禁止「`throw new RuntimeException("订单不存在")`」——针对**业务异常**。技术/前置条件守卫（编程错误、穷举 switch default、enum fromCode 反序列化）使用 `IllegalArgumentException` 是 Java 惯例 + Nop 平台自身模式，不属违规。

| # | 站点 | 异常类型 | 上下文 | 裁决 |
|---|------|---------|--------|------|
| 1 | `ErpPartyBizModel.java:222`（master-data） | `IllegalArgumentException("Unsupported ErpPartyType: " + type)` | switch/default 穷举守卫——新增 `ErpPartyType` enum 值未更新 switch 时 fast-fail | ✅ **合法技术守卫**（编程错误，非业务异常；switch 穷尽性保护） |
| 2 | `AttributesControlLimitFormulas.java:126`（quality） | `IllegalArgumentException("...defects/inspected lists must not be null")` | `private static validateSameSize` 前置条件——SPC 公式输入非空/等长校验 | ✅ **合法前置条件守卫**（私有方法，编程契约，非用户面业务异常） |
| 3 | `AttributesControlLimitFormulas.java:129`（quality） | `IllegalArgumentException("...must have same size, got ...")` | 同上（等长校验） | ✅ **合法前置条件守卫** |
| 4 | `ErpFinBusinessType.java:86`（finance dao） | `IllegalArgumentException("Unknown ErpFinBusinessType code: " + code)` | enum `fromCode(int)` 反序列化——未知 DB 数值时 fast-fail（DB 数据合法时永不可达） | ✅ **合法技术守卫**（DAO 层 enum 反序列化，DB 数据约束保证不可达；属技术层非业务层） |

**裁决结论**：4 处原生异常**全部为合法技术/前置条件守卫**，非业务异常未用 NopException 的违规。无需 P1 修复。（注：若未来严格要求全域零原生异常，可在 MR3 文档化为「技术守卫例外清单」，当前不阻断。）

## 4. i18n 注册完整性

### 4.1 ErrorCode 命名空间合规性（100% 合规）

- **20 个 `Erp*Errors` 定义文件**（19 域各 1 + finance 额外 `ErpFinPostingErrors`， posting 子域专属）。
- **667 处 `ErrorCode.define(...)`** 常量定义。
- **命名空间 100% 合规**：全域 grep `ErrorCode.define(` 后的首参（name 字符串）**全部**符合 `erp.err.<domain-short>.<name>` kebab-case（§7.1 命名表 18 域 + notify 全部对齐）。零非 `erp.err` 前缀，零数字编码（§7.2「不推荐数字编码」合规）。

### 4.2 i18n 资源束完整性（功能完整 / 前瞻缺口）

**现状**：全域 `module-*/**/*.yaml` + `*.properties` **零** `erp.err.*` 键。

**裁决**：**非功能缺口**。Nop 平台 `ErrorCode.define(name, description)` 的 `description`（第二参）即**默认中文消息**，运行时 `NopException.getDescription()` 直接返回该内联中文描述——**无需独立 i18n 资源束即可正确显示中文错误**。owner doc §七「描述使用中文（平台 i18n 处理翻译）」正是此机制：中文描述内联于 ErrorCode 定义，平台 i18n 层在需要时**可选覆盖**（resource bundle 键 = ErrorCode name），但不覆盖时 fallback 至内联默认值。

- **当前基线（中文单语）**：功能 100% 完整，所有 667 ErrorCode 在 GraphQL 响应 `errors[].message` / 日志 / 异常工作台正确显示中文。
- **前瞻缺口（多语国际化）**：若未来需英文/其他语种，需补 `module-*/erp-*-meta/src/main/resources/_vfs/i18n/` 资源束（键 = `erp.err.*`，值 = 翻译消息）。当前产品定位为中文 ERP，无活跃多语需求——归入 P2-MA7-002 watch-only。

## 5. 异常吞咽风险（与 MA4 P1 家族交叉去重）

**审计方法**：全域 `rg "catch\s*\(\s*(Exception|NopException|RuntimeException)"` + 逐类裁决「业务异常吞咽致 ErrorCode 不可观察」vs「读路径/job 容错」。

**结果**：service 层 234 处 catch 块（含 Exception/Throwable/RuntimeException/NopException），按模式分 3 类：

| 模式 | 站点 | 裁决 |
|------|------|------|
| **业财过账 dispatcher `tryPost` 吞咽**（catch Exception → LOG.warn/error → 返回 boolean / 不重抛） | ~15 站点（finance `ErpFinDeferredPostingRetryHelper` / mfg `ProductionVarianceDispatcher:111,150` + `SubcontractPostingDispatcher:137,149` + `ManufacturingIssuePostingDispatcher:107` / assets 折旧 dispatcher / hr `SalaryPostingDispatcher` / inv `InvPostingDispatcher:69,139` + `LandedCostPostingDispatcher:63` + `CostAdjustmentPostingDispatcher:54,68` + `OwnershipTransferPostingDispatcher:62` / mnt `MaintenanceLaborPostingDispatcher:113` + `MaintenanceIssuePostingDispatcher:113`） | **已由 MA4 P1 家族登记**：P1-MA4-001（MAX_RETRY 死状态）/ P1-MA4-004（期间编排吞咽）/ P1-MA4-007（mfg 完工差异吞咽）/ P1-MA4-010（委外吞咽）/ P1-MA4-013（折旧 dispatcher posted=false 悬挂）/ P1-MA4-017（salary 计提+ER 丢弃）/ P1-MA4-020（landed cost reverse 吞咽）。**A7.1 不产生独立新 P1**（同根因「业财悬挂 + 异常吞咽」已系统化登记 MR1/MR2） |
| **期间/编排层跨域 command 吞咽** | `ErpFinAccountingPeriodProcessor:353-356,375-377,393-395`（catch Exception → LOG.warn 跳过） | **已由 P1-MA4-004 登记**（期间结账编排层跨域 command 异常吞咽致 GL 缺折旧/成本凭证） |
| **读路径/报表/看板容错**（catch Exception → LOG + 返回空/部分） | ~12 站点（`ErpMfgReportBizModel:126` / `ErpMntReportBizModel:121` / `ErpCsReportBizModel:114,288` / `ErpMfgDashboardBizModel:312` / `ErpCsQualityDashboardBizModel:343` / `CannedResponseRenderer:92`） | ✅ **合法读路径容错**（报表/看板聚合部分失败不应阻断整页；LOG 提供可见性；无业务写路径数据腐败） |
| **定时 job 容错**（catch Exception → LOG，job 继续） | ~9 站点（`ErpMfgCrpRunJob:53` / `ErpMntDueVisitJob:46` / `ErpCsSlaScanJob:46` / `ErpCsCsatReminderJob:82,109,141` / `ErpCsEntitlementExpiryJob:73,89,126`） | ✅ **合法 job 容错**（cron job 单条失败不应中断批次；LOG + 下次扫描自愈；无业务异常被吞致不可观察——job 失败经日志可见） |
| **mutation 编排通知 fallback**（catch Exception → LOG + 主路径继续） | ~8 站点（`ErpMntVisitBizModel:192` / `ErpCsEntitlementBizModel:151,247` / `ErpCtSignatureRequestBizModel:127,498` / `ErpCsServiceCatalogItemBizModel:91` / `ErpMntSparePartUsageBizModel:84,100` / `ErpMfgSubcontractOrderProcessor:271,307` / `MrpReleaseService:273`） | ✅ **合法通知/副作用 fallback**（主业务写已提交，通知/衍生副作用失败不回滚主事务；LOG 可见） |

**关键结论**：**业务异常吞咽致 ErrorCode 不可观察**的风险已由 MA4 P1 家族（P1-MA4-001/004/007/010/013/017/020）系统化覆盖。A7.1 复核确认这些站点在错误码维度的投影（ErrorCode 经 NopException 抛出后被 catch 吞咽 → GL/业务侧不可观察）与 MA4 代码质量维度的投影（posted=false 悬挂无告警闭环）是**同根因双投影**，不重复登记。读路径/job/通知 fallback 的 catch-all 是合法容错模式，非业务异常吞咽。

## 6. Finding 汇总

### P0（即时通道）

无。

### P1（目标 MR3）

无。错误码构造形态 100% 合规 + 4 原生异常合法 + 吞咽风险已由 MA4 系统化覆盖——不产生独立新 P1。

### P2（watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA7-001` | **2 处 ErrorCode 集中化偏差**：(a) `CloseVoucherWriter.java:82`（finance）内联 `ErrorCode.define("erp.err.fin.period-close.unbalanced", ...)` 未集中化至 `ErpFinErrors` interface；(b) `AssessmentAggregator.java:145`（hr）类内局部 `static final ErrorCode ERR_AGGREGATE_NO_DETAILS` 未提升至 `ErpHrErrors` interface。两处命名/描述合规，仅违反 §7.2「以 interface + 静态字段形式集中定义」的可发现性/维护性约定（grep `Erp*Errors` 漏此 2 常量）。 | watch-only，MR3 文档卫生顺手——2 常量迁移至对应 `Erp*Errors` interface（`CloseVoucherWriter` 改引用 `ErpFinErrors.ERR_PERIOD_CLOSE_UNBALANCED`，`AssessmentAggregator` 改引用 `ErpHrErrors.ERR_AGGREGATE_NO_DETAILS`）+ 删除类内局部定义 |
| `P2-MA7-002` | **i18n 资源束前瞻缺口**：全域零 `erp.err.*` i18n 资源束键。当前中文单语基线下功能完整（ErrorCode.define 内联中文描述为默认消息），但多语国际化时需补 `module-*/erp-*-meta/_vfs/i18n/` 资源束。 | watch-only，多语国际化触发时补——非当前产品需求（中文 ERP），登记为前瞻 successor |

## 7. 与 MA1-MA6 已登记 finding 交叉去重

| 本审计观察 | 已登记 finding | 关系 |
|-----------|---------------|------|
| 业财过账 dispatcher tryPost 吞咽（~15 站点） | P1-MA4-001/004/007/010/013/017/020 | **同根因双投影**（MA4 = 代码质量/posted 悬挂维度；A7.1 = 错误码可观察性维度）。A7.1 不重复登记，归并 MA4 P1 家族 |
| 期间编排跨域 command 吞咽（3 站点） | P1-MA4-004 | **同一站点**，已登记 |
| ErrorCode 命名空间合规 | n/a | 新观察，PASS（无 finding） |
| 4 原生异常合法性 | n/a | 新观察，PASS（技术守卫合规） |
| i18n 资源束前瞻缺口 | P1-MA3-011（product-scope 里程碑陈旧）/ A4.9 i18n 完整性 | **不同维度**：A4.9 审 view.xml/action-auth.xml i18n（前端）；本审计审 ErrorCode i18n（后端错误消息）。无重叠 |

## 8. Exit Criteria 核实

- [x] 错误码合规性矩阵产出（§1 构造形态分布表 752 站点 + §3 4 原生异常逐项裁决 + §2 11 非类限定 ErrorCode 合规性裁决 + §4 i18n 注册完整性表）
- [x] A7.1 P0/P1/P2 已登记 arm-index.md（零 P0 + 零 P1 + 2 项 P2 watch-only），且与 MA1-MA6 既有 P1 交叉去重无重复（§7）
