# 日期范围有效性模式（Date-Ranged Validity Pattern）

> Owner docs: `docs/backlog/deepening-roadmap.md` §Milestone C / C3、`docs/design/master-data/README.md`、`docs/design/sales/`（价格/促销）、`docs/design/finance/budget.md`、`docs/design/human-resource/README.md`、`docs/architecture/l10n-strategy.md`（汇率有效期）
> 落地计划：`docs/plans/2026-07-21-2225-1-date-ranged-validity-pattern.md`
> 平台权威源：`../nop-entropy/docs-for-ai/02-core-guides/service-layer.md`（CrudBizModel + defaultPrepareSave/Update 钩子）、`../nop-entropy/docs-for-ai/03-runbooks/extend-crud-with-hooks.md`

## 1. 目的与范围

固化「ERP 实体使用 `validFrom/validTo` 表达记录有效期」的统一约定：规范字段命名、区间查询语义、重叠策略分类、可复用查询 helper、重叠校验规则。供已落地的 10+ 域实体 + 后续新增实体按图施工。

**适用范围（记录有效期类）**：实体使用「生效日期 + 失效日期」表达**单条记录的业务有效期**（在区间内对该实体值有效，区间外无效）。典型场景：价格清单、促销规则、汇率、税率、薪酬档、社保配置、工作中心日历、客户分级、交付时间窗、报价单/合同有效期。

**不适用**（归不同模式）：

- **期间语义**：`ErpFinBudgetScenario.validFrom/validTo` 表达「预算期间」（按会计期切分 + status 状态机），并非「单条记录的有效期」→ `docs/design/finance/budget.md`
- **会计期间状态机**：`ErpMdAcctSchema` 的会计期开闭 → `docs/design/finance/period-close.md`
- **生效时点（单值非区间）**：`ErpCsAgentRate.effectiveDate` 仅一个时点字段，非区间 → 归 cs 域独立约定
- **业务单据的业务日期**：`ErpPurOrder.orderDate` / `ErpSalOrder.businessDate` 是「业务发生日」（点），不是「记录有效期」（区间）

## 2. 规范字段命名（Decision A）+ 迁移策略（Decision B）

### Decision A — 规范字段命名

**裁决**：**`validFrom` / `validTo` 为规范字段命名**。

**选择理由**：

1. **数量占优**：4 核心域（finance/master-data/sales/purchase）共 12 实体已用 `validFrom/validTo`，覆盖最广（详见 §8.1 实体清单）
2. **语义对称**：`valid` 表达「记录是否生效」，与 `status=ACTIVE/INACTIVE` 同义链；`effective` 在英语中更接近「生效时点」单值语义（cs/effectiveDate 即是单值）
3. **平台/字典对齐**：`active-status` 字典 `ACTIVE/INACTIVE` 与 `valid` 语义同源

**替代方案**：

- (b) `effectiveFrom/effectiveTo` 为规范 — 拒绝。仅 hr/aps/manufacturing(workcenter)/crm/logistics 5 域使用，数量劣势；且 `effective` 与 cs 的单值 `effectiveDate` 在英语阅读上更接近「时点」语义
- (c) 双轨接受 + helper 归一化 — 拒绝作为规范。helper 内部可对历史命名做归一化适配（见 §5），但**新实体必须采用 `validFrom/validTo`**，避免双轨扩散

**残留风险**：hr/aps/crm/logistics 等扩展域历史命名 `effectiveFrom/effectiveTo` 仍存在；按 Decision B 不重命名，仅文档化为「历史变体」。helper 通过 `IDateRange` 抽象（见 §5）兼容历史命名，调用方无需感知底层字段名。

**类型选择规则**：

| 业务精度 | stdSqlType | 示例 |
|---------|-----------|------|
| 仅日期 | `DATE` | 价格清单、汇率、税率（绝大多数） |
| 含时刻 | `TIMESTAMP` + `tagSet="clock"` | 促销规则（生效时刻精确到分秒） |

新实体按业务精度择一；同一实体 `validFrom/validTo` 类型必须一致。

### Decision B — 既有字段迁移策略

**裁决**：**不重命名既有非规范字段**。仅：

1. **新实体**强制采用 `validFrom/validTo`（Decision A）
2. **helper** 通过 `IDateRange` 接口归一化历史命名（`effectiveFrom/effectiveTo` 调用方适配后传入 helper）
3. **owner doc** 显式记录历史变体（见 §8.1）

**预期裁决=a**（已确认）。理由：

- 跨 10+ 源 ORM 文件 + 全量 codegen 再生 = 高风险数据迁移（涉及 DAO/Entity/Mapper/XMeta/i18n/View/Page 全套生成产物）
- 既有命名变体不影响业务正确性（语义清晰）
- 重命名收益（命名一致性）≈ 0 业务价值，成本（迁移风险）高

**授权门控**：若后续业务驱动确需重命名（如客户要求统一字段名），必须按 `project-context.md §AI 阻塞条件` 取得明确人工批准 + 独立 ORM 变更计划。本计划不触发。

## 3. 区间查询语义

### 3.1 边界含否约定

**[validFrom, validTo] 双侧闭区间**（含起止日）。

- `validFrom = 2026-01-01, validTo = 2026-12-31` → 2026-01-01 与 2026-12-31 均视为有效
- `validTo IS NULL` → 表示「无失效日」（开放右侧），仅 `validFrom <= date` 校验
- `validFrom IS NULL` → 表示「无生效日」（开放左侧），仅 `validTo == null || validTo >= date` 校验；新实体应避免（强制 `validFrom mandatory`）

** TIMESTAMP 变体**（如 `ErpSalPricingRule`）：闭区间语义不变，但比较粒度到毫秒。

### 3.2 原语定义

| 原语 | 语义 | 公式 |
|------|------|------|
| `contains(range, date)` | date 是否在 [validFrom, validTo] 内 | `fromLE(date) && toGE(date)`（含 NULL 侧开放） |
| `overlaps(r1, r2)` | 两区间是否有交集 | `from1 <= to2 && to1 >= from2`（含 NULL 视为负/正无穷） |
| `effectiveOn(ranges, date)` | 在区间集合中找出在 date 当天生效的记录 | `filter(r -> contains(r, date))` |
| `longestOverlap(ranges)` | 返回最大重叠数（同维度最多 N 条同时生效） | `sweep line on endpoints` |

**实现位置**：见 Decision D（§5）。

## 4. 重叠策略分类（Decision C 裁决矩阵）

### Decision C — 三类语义分类

| 策略 | 含义 | 查询语义 | 典型实体 |
|------|------|----------|----------|
| **互斥（MUTEX）** | 同维度同一时刻至多 1 条有效 | `effectiveOn(date)` 必须返回 0 或 1 条 | 汇率（同 from→to 同 rateType 同日仅一档）/ 默认税率（同 taxType 同日仅一档） |
| **优先级（PRIORITY）** | 允许重叠，但按 `priority` 或时间戳取一 | `effectiveOn(date)` 多条时按 priority ASC 取首 | 销售价格清单（会员价 vs 标准价）/ 报价单（同客户重叠按最新优先） |
| **允许重叠（STACKABLE）** | 允许多条同时生效，业务上叠加计算 | `effectiveOn(date)` 返回所有命中，调用方自行合并 | 促销规则（`stackable=true` 的多条并行） |

### 试点实体策略映射

| 试点实体 | 维度键 | 重叠策略 | 接入点 |
|---------|--------|---------|--------|
| `ErpMdExchangeRate` | `fromCurrencyId, toCurrencyId, rateType` | **MUTEX** | `defaultPrepareSave` / `defaultPrepareUpdate` 钩子前置校验 |
| `ErpMdTaxRate` | `taxType` | **MUTEX** | 同上 |
| `ErpMdSupplierApproval` | `partnerId` | **MUTEX**（既有 AVL 状态机已隐式约束，本计划补 validFrom/validTo 维度） | 同上（status != REJECTED 时校验） |

**`ErpSalPriceList`** 作为非试点 follow-up（PRIORITY 策略 + 跨域接入，留 successor）：同 partnerId/customerGroupCode 维度允许重叠，按 `priority` 取首。本计划不接入以避免跨域 service 测试扩散。

**残留风险**：试点仅 3 个 MUTEX 实体；PRIORITY / STACKABLE 策略的运行时取值 helper（`pickHighestPriority`）属 Deferred successor，触发条件：业务方明确需求 + PRIORITY 实体接入。

## 5. 查询 helper 契约（Decision D）

### Decision D — helper 实现位置

**裁决**：**Java 静态工具类 + 实体内 BizModel 钩子调用 DAO 查询**。位置：

- **纯区间运算原语**（`contains/overlaps/effectiveOn/longestOverlap`）：`module-master-data/erp-md-service/src/main/java/app/erp/md/service/daterange/ErpDateRanges.java`，**纯函数无 IoC 依赖**，跨域 service 经 `app-erp-master-data-service` 依赖即可调用（hr/crm/pur/sal/mfg/fin/log 已传递依赖）
- **重叠校验器**（`ErpDateRangeOverlapValidator`）：同包，提供 `enforceMutex` 方法接受 `IDateRange` + 维度键 + 同维度已存在记录 List，由试点 BizModel 在 `defaultPrepareSave/Update` 中调用
- **`IDateRange` 接口**：同包，归一化 `validFrom/validTo` 与 `effectiveFrom/effectiveTo` 历史变体，调用方实现 2 个 getter 即可

**替代方案**：

- (b) `nop-commons` 风格静态工具类 — 拒绝。需向 nop-entropy 提 PR，跨仓库依赖；本仓库范围足够
- (c) SQL-lib 跨域 mapper — 拒绝。SQL-lib 适合复杂 SQL 模板；区间运算是纯 Java 逻辑，SQL filter 仅需简单 `le/ge` 由 BizModel 现拼装，无需 mapper

**选择理由**：

1. 纯 Java 函数 = 零运行时开销 + 单元测试简单（无需容器）
2. master-data-service 已是跨域枢纽（sales/purchase/finance 等均依赖），可达性最佳
3. 调用方自带 DAO 查询（按维度键 `findAllByQuery`），无需 helper 注入 IDaoProvider（避免 IoC 耦合）
4. aps 域不依赖 master-data-service，aps 实体接入时通过 `app-erp-master-data-dao` 间接可达（dao 模块同样可达；后续 follow-up 触发时再评估是否下沉到 dao 层）

### helper 契约

```java
// 区间运算原语（纯函数）
public final class ErpDateRanges {
    public static boolean contains(IDateRange range, LocalDate date);
    public static boolean contains(IDateRange range, java.util.Date date); // TIMESTAMP 适配
    public static boolean overlaps(IDateRange r1, IDateRange r2);
    public static <T extends IDateRange> List<T> effectiveOn(List<T> ranges, LocalDate date);
    public static int longestOverlap(List<? extends IDateRange> ranges);
}

// 归一化接口（消除 validFrom/effectiveFrom 历史命名差异）
public interface IDateRange {
    LocalDate getValidFrom();
    LocalDate getValidTo();
}

// 互斥校验器（friendly pre-save check）
public final class ErpDateRangeOverlapValidator {
    public static <T extends IDateRange> void enforceMutex(
        T candidate,                       // 待保存的实体
        List<T> existing,                  // 同维度已存在记录（由调用方查询）
        ErrorCode errorCode,               // 抛出时使用的错误码
        Long selfId                        // 排除自身（更新场景）；新增传 null
    );
}
```

## 6. 重叠校验规则 + 错误码约定

### 校验流程

`defaultPrepareSave` / `defaultPrepareUpdate` 钩子：

1. 从 `entityData.getEntity()` 取候选记录 `candidate`
2. 用维度键构造 `QueryBean`，`dao().findAllByQuery(query)` 取同维度已存在记录
3. 排除 `candidate.id` 自身（更新场景）
4. 调用 `ErpDateRangeOverlapValidator.enforceMutex(candidate, existing, ERR_XXX, candidate.id)`
5. 若有重叠 → 抛 `NopException` + 中文 ErrorCode（不暴露 stack trace）
6. 若无重叠 → 继续 `super.defaultPrepareSave`

### 错误码命名

`ERP_MD_DATE_RANGE_OVERLAP`（master-data 域试点通用错误码，参数 `entityName, validFrom, validTo, conflictId`）。其余域接入时按域前缀定义（如 `ERP_SAL_DATE_RANGE_OVERLAP`），不复用 master-data 错误码（对齐 `ErpMdErrors` 既有「每域独立错误码」约定）。

### NULL 处理

- `validFrom == null && validTo == null` → 跳过校验（视为「永久无区间」，业务上视为总是有效；新实体应在 ORM 层 `validFrom mandatory=true` 规避）
- `validFrom == null && validTo != null` 或反之 → 仅按单侧校验（半开区间）
- 两记录完全相同端点 → 视为重叠（端点闭区间）

## 7. 试点实施记录

3 试点实体均已落地（plan 2026-07-21-2225-1 Phase 3，2026-07-21）：

| 实体 | 模块 | 策略 | 维度键 | 接入点 | 错误码 | 测试 |
|------|------|------|--------|--------|--------|------|
| `ErpMdExchangeRate` | `erp-md-service` | MUTEX | `fromCurrencyId, toCurrencyId, rateType` | `ErpMdExchangeRateBizModel.defaultPrepareSave/Update` | `ERR_MD_DATE_RANGE_OVERLAP` | `TestErpMdDateRangePilots.exchangeRate_*`（4 场景） |
| `ErpMdTaxRate` | `erp-md-service` | MUTEX | `taxType` | `ErpMdTaxRateBizModel.defaultPrepareSave/Update` | 同上（master-data 域通用错误码） | `TestErpMdDateRangePilots.taxRate_*`（3 场景） |
| `ErpMdSupplierApproval` | `erp-md-service` | MUTEX（status != REJECTED 时） | `partnerId` | `ErpMdSupplierApprovalBizModel.defaultPrepareSave/Update` | 同上 | `TestErpMdDateRangePilots.supplierApproval_*`（3 场景） |

### 字段映射

3 试点实体均原生使用规范字段 `validFrom` / `validTo`（DATE 类型），直接 `implements IDateRange`，无需适配器。

### 关键设计决策记录

- **`updateEntity` 绕过钩子**：Nop Platform `CrudBizModel.updateEntity(entity, action, context)` 不触发 `defaultPrepareUpdate`（仅 GraphQL `__save` / `__update` Map 入口走 EntityData 管道才触发）。因此 `ErpMdSupplierApprovalBizModel` 的 6 态状态机内部 `updateEntity` 调用不受 overlap 钩子干扰；仅用户经标准 CRUD API 创建/更新 AVL 资格时才校验。这是正确语义：状态机内部转换是同一业务操作的一部分，不应被区间校验阻断。
- **REJECTED 语义豁免**：status=REJECTED 的 AVL 记录视为「已废弃」不参与互斥（业务上不生效）。校验器在查询同 partnerId 既有记录后内存过滤 REJECTED，再传入 `enforceMutex`。
- **错误码单点定义**：3 试点复用 `ERR_MD_DATE_RANGE_OVERLAP`（master-data 域通用错误码），不按实体拆分（`{entityName}` 参数已区分实体）。其他域接入时按域前缀独立定义（如 `ERP_SAL_DATE_RANGE_OVERLAP`），不复用此码。

### 测试基线

- helper 单测：`TestErpDateRanges`（29 场景，纯函数无容器）
- 试点集成测试：`TestErpMdDateRangePilots`（10 场景，经 GraphQL RPC 触发 `__save` 走完整管道）
- master-data service 全 108 测试全绿（98 既有 + 10 新增 pilot 集成测试）

## 8. 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|---------|
| AP1 | 在 BizModel 内手写 `le("validTo", date).ge("validFrom", date)` 拼装 | 用 `ErpDateRanges.contains/effectiveOn` |
| AP2 | 新实体使用 `effectiveFrom/effectiveTo` 命名 | 用 `validFrom/validTo`（Decision A） |
| AP3 | 互斥策略实体未接入 `defaultPrepareSave/Update` 钩子 | 必须接入前置校验（DB 无唯一约束兜底） |
| AP4 | helper 注入 `IDaoProvider` 让纯函数变 IoC bean | helper 保持纯静态；调用方查 DAO 后传 List |
| AP5 | 错误码复用其他域前缀（如 sal 实体用 `ERP_MD_DATE_RANGE_OVERLAP`） | 按域前缀独立定义 |
| AP6 | helper 放 `nop-commons` 或 nop-entropy 提 PR | 放本仓库 `erp-md-service` 内（同仓库可演进） |
| AP7 | 互斥校验遗漏 `selfId` 排除（更新场景下与自身重叠） | 必传 `selfId`（新增传 null） |
| AP8 | 把「业务单据业务日期」当「记录有效期」套本模式 | 见 §1 不适用清单 |
| AP9 | 把「预算期间」「会计期间状态机」当「记录有效期」套本模式 | 见 §1 不适用清单 |
| AP10 | TIMESTAMP 变体（如 ErpSalPricingRule）与 DATE 混用 helper 不适配 | helper 提供 `java.util.Date` 重载或调用方截断到 LocalDate |

## 9. 与既有 owner doc 关系

- `docs/design/master-data/README.md` — 增「日期范围有效性（C3）」段，交叉回链本文件
- `docs/design/master-data/exchange-rate-management.md` — 「汇率有效期重叠校验」段回链本文件 §6
- `docs/design/sales/`（价格/促销相关 owner doc）— follow-up 接入 `ErpSalPriceList` 时回链本文件
- `docs/design/finance/budget.md` — 已在 §1 显式排除「预算期间」（不同模式）
- `docs/design/human-resource/README.md` — 增交叉引用段，标记 hr 历史 `effectiveFrom/effectiveTo` 命名变体（Decision B 不重命名）
- `docs/architecture/l10n-strategy.md` — 汇率有效期段落回链本文件

## 10. Follow-up 实体清单（Deferred successor）

以下实体未在本计划试点，按业务驱动逐域接入。接入时按本文件 §3-6 范式施工。

| 实体 | 域 | 命名变体 | 策略 | 接入触发条件 |
|------|-----|---------|------|-------------|
| `ErpSalPriceList` | sales | validFrom/validTo | PRIORITY | 销售定价引擎细化 + sales service 接入 |
| `ErpSalPriceListLine` | sales | validFrom/validTo | MUTEX（同 priceListId + materialId 维度） | 同上 |
| `ErpSalPricingRule` | sales | validFrom/validTo (TIMESTAMP) | STACKABLE | 同上 |
| `ErpSalQuotation` | sales | validFrom/validTo | PRIORITY | 报价转订单流程细化 |
| `ErpSalContract` | sales | validFrom/validTo | MUTEX | 合同续签/废止流程 |
| `ErpPurQuotation` | purchase | validFrom/validTo | PRIORITY | 采购定价引擎 |
| `ErpPurSupplierPriceList` | purchase | validFrom/validTo | MUTEX | 同上 |
| `ErpHrSocialInsuranceConfig` | hr | effectiveFrom/effectiveTo | MUTEX | 薪酬档调整流程 |
| `ErpHrSocialInsuranceBase` | hr | effectiveFrom/effectiveTo | MUTEX | 同上 |
| `ErpCrmLeadScoreConfig` | crm | effectiveFrom/effectiveTo | MUTEX | CRM 评分引擎细化 |
| `ErpCrmProductConfigurator` | crm | effectiveFrom/effectiveTo | MUTEX | CRM 配置器细化 |
| `ErpCrmBundlePricing` | crm | effectiveFrom/effectiveTo | STACKABLE | CRM 套餐定价 |
| `ErpCrmPriceRule` | crm | effectiveFrom/effectiveTo | PRIORITY | CRM 定价规则 |
| `ErpApsOpRouting` | aps | effectiveFrom/effectiveTo | MUTEX | 工艺路线版本管理 |
| `ErpMfgWorkcenterCalendar` | manufacturing | effectiveFrom/effectiveTo | MUTEX | 工作中心日历细化 |
| `ErpMfgProductionVersion` | manufacturing | validFrom/validTo | MUTEX | 生产版本管理 |
| `ErpLogDeliveryWindow` | logistics | effectiveFrom/effectiveTo | MUTEX | 交付时间窗细化 |
