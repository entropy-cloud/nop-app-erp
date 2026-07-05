# 平台最佳实践合规审计报告（扩展轮）

> 日期：2026-07-05
> 范围：M2/M3 新增 8 域（crm/cs/hr/contract/drp/logistics/b2b/aps）+ 扩展 5 域（manufacturing/quality/assets/maintenance/projects）的服务层、ORM 跨域引用、异常处理、GraphQL 安全、生成物纯净度
> 方法：以 `nop-entropy/docs-for-ai/` 为基线，针对前序审计（`2026-07-01-1900-platform-best-practices-compliance-audit.md`，覆盖 S1-S8 + O1-O4 + M1-M4 + V1-V4）的覆盖盲区定向扫描
> 配套基线：`00-start-here/ai-defaults.md`、`02-core-guides/concurrency-and-transactions.md`、`02-core-guides/error-handling.md`、`02-core-guides/cross-module-entity-reference.md`

## 目的

回答一个问题：**前序审计聚焦 purchase/sales/inventory/finance 四域，M2/M3 新增及扩展域中是否存在前序未发现的平台反模式？**

结论先行：**前序四域的服务层整改路线（S1-S8）应同步推广到全部扩展域，否则同样的问题会在新域再次固化。同时发现 4 类前序完全未触及的新问题域：生成物被手改、敏感凭证未做 Meta 过滤、跨业务域 ORM 反向引用扩展、异常吞咽。**

## 与前序审计的关系

本报告**只记录新发现或对前序发现的明确扩展**。前序已覆盖的 S1-S8 + O1-O4 + M1-M4 + V1-V4 不再重复，仅在"扩展"类条目中说明扩散范围。

| 编号 | 类型 | 风险 | 一句话 |
| --- | --- | --- | --- |
| N1 | S5 扩展 | 🔴 | 扩展域 60+ 处 `LocalDateTime.now()/LocalDate.now()/System.nanoTime()`，前序只发现 1 处 |
| N2 | M1 扩展 | 🔴 | M2/M3 引入 3 条新跨业务域 ORM 边，其中 assets→finance 是明确禁止的反向边 |
| N3 | 全新 | 🔴 | `_app.orm.xml`（`_` 前缀生成文件）被手改，违反硬停止规则 |
| N4 | 全新 | 🔴 | logistics/b2b 域 API 密钥/凭证字段明文存储 + XMeta 全开 queryable |
| N5 | S2 扩展 | 🟡 | 扩展域跨域写（如 MrpReleaseService 直构 ErpPurOrder）绕过 I*Biz |
| N6 | 全新 | 🟡 | quality 域抛 `IllegalStateException` 绕过 NopException 体系 |
| N7 | 全新 | 🟡 | inventory/purchase 域存在吞异常（`catch (Exception ignored)` / `catch → return null`） |
| N8 | 全新 | 🟢 | hr/crm 域用 `Map<String,Object>` 作为复杂返回 DTO，未定义 `@DataBean` |

**新发现总计 8 项**：🔴 严重 4，🟡 中等 3，🟢 轻微 1。分布：扩展域服务层（N1/N5/N6/N7/N8）、ORM 跨域（N2）、生成物纪律（N3）、GraphQL 安全（N4）。

---

## 🔴 严重发现

### N1. 扩展域大规模时间获取违规（前序 S5 的盲区扩散）

**违反规则**：`ai-defaults.md` 反模式表「`System.currentTimeMillis()` / `LocalDateTime.now()` / `LocalDate.now()` / `new Date()` / `new Timestamp(...)` → 一律走 `CoreMetrics`」。原文强制约束：**"所有获取当前时间的写法一律走 `CoreMetrics`"**，理由是混用会读不同时钟，导致 autotest `TestClock` 与 ORM 自动 `createTime`/`updateTime` 不同源。

**前序覆盖盲区**：前序 S5 仅在 inventory 域发现 1 处遗漏（`ErpInvStockMoveBizModel.java:251`），结论是"其余 99% 正确"。本轮在 **main 代码（非测试）** 中发现 **60+ 处违规**，集中在 M2/M3 新增/扩展域。

**证据（按域分组，仅列代表性位置）**：

| 域 | 文件:行号 | 违规写法 |
| --- | --- | --- |
| cs | `ErpCsSurveyBizModel.java:73,110,121,134` | `LocalDateTime.now()` x4 |
| cs | `ErpCsTicketBizModel.java:98,115,156,221,234,257` | `LocalDateTime.now()` x6（含 SLA 截止时间计算） |
| hr | `ErpHrSalaryBizModel.java:165,182,196,202` | `LocalDate.now()` + `System.nanoTime()` 用作单号 |
| hr | `ErpHrSalarySimulationBizModel.java:122,412,523,725,873` | `LocalDateTime.now()` x4 + `System.nanoTime()` 单号 |
| hr | `ErpHrShiftSwapRequestBizModel.java:54` | `System.nanoTime()` 单号 |
| quality | `ErpQaActionBizModel.java:49`、`ErpQaNonConformanceBizModel.java:77,113`、`ErpQaRecallBizModel.java:97,128,177,262`、`ErpQaInspectionBizModel.java:164,165`、`NcrLifecycleService.java:54` | `LocalDateTime.now()` / `LocalDate.now()` x9 |
| drp | `SafetyStockEngine.java:135`、`DrpEngine.java:112`、`DrpReleaseService.java:73,80` | `LocalDateTime.now()` / `LocalDate.now()` x4 |
| mfg | `ErpMfgCrpRunJob.java:45`、`DemandAggregator.java:73`、`MrpEngine.java:89`、`ErpMfgJobCardProcessor.java:146`、`ErpMfgWorkOrderProcessor.java:109,175,215,244` | `LocalDate.now()` x8 |
| mnt | `ErpMntDueVisitJob.java:41`、`SparePartIssueService.java:38` | `LocalDate.now()` x2 |
| aps | `ErpApsSchedulingEngine.java:288` | `java.time.LocalDateTime.now()` |
| b2b | `ErpB2bEdiDocBizModel.java:80,195`、`ErpB2bAsnBizModel.java:287` | `System.currentTimeMillis()` 用作单号 x3 |
| b2b | `UblInvoiceEdiProvider.java:71`、`TransportManager.java:68,75,91` | `LocalDate.now()` / `System.nanoTime()`（计耗时） |
| logistics | `MockCarrierGatewayClientFactory.java:123` | `LocalDateTime.now()` |
| finance | `EmployeeAdvancePostingDispatcher.java:71`、`ErpFinCashForecastJob.java:42`、`ErpFinBadDebtProcessor.java:277`、`BankStatementImporter.java:71`、`ErpFinPostingExceptionBizModel.java:170` | `LocalDate.now()` x4 + `new Timestamp(...)` |

**根因**：M2/M3 域编写时未执行 `ai-defaults.md` 完成前自检第 5/6 项；Job/Processor/Engine 类代码（非 BizModel）被默认当作"边界场景"未受约束，但 `ai-defaults.md` 例外场景只豁免 store/infra，不豁免应用层 Job/Processor。

**风险**：
1. autotest 注入 `TestClock` 后，扩展域的时间敏感查询（SLA 截止、到期访问、MPS 计划日、银行对账导入时间）会读到与 ORM 自动字段不同的时钟，导致 `count=0` 或断言失败。
2. 单号生成用 `System.nanoTime()` / `System.currentTimeMillis()` 在测试中不可控，无法稳定回放。

**修复**：全域替换为 `CoreMetrics.currentDateTime()` / `CoreMetrics.today()` / `CoreMetrics.nanoTime()`；单号生成改用平台序列或 `CoreMetrics` + 序列号组合。

---

### N2. M2/M3 新增 3 条跨业务域 ORM 边（前序 M1 的扩散 + 1 条明确违规）

**违反规则**：`module-boundaries.md:44-51` DAG 约束 + `cross-module-entity-reference.md §7` 主数据引用策略。前序 M1 仅识别 purchase/sales → projects 的 2 条边；本轮发现 M2/M3 新增 3 条边，其中 **assets → finance 是 owner docs 明确禁止的反向边**。

**证据**：

| 边 | 证据 | owner docs 立场 | 判定 |
| --- | --- | --- | --- |
| **assets → finance**（`ErpFinVoucher`） | `module-assets/model/app-erp-assets.orm.xml:267` `<to-one name="voucher" refEntityName="app.erp.fin.dao.entity.ErpFinVoucher">` + `:713` 外部实体声明 + `erp-ast-dao/pom.xml:38` 依赖 `app-erp-finance-dao` | `module-boundaries.md:44`「app-erp-assets \| master-data / inventory \| **finance（finance 引用 assets，不反向）**」 + `data-dependency-matrix.md:66`「assets: R: master-data + inventory；**S 写 finance**」 | 🔴 **明确违规**。S 写允许（经 I*Biz），ORM 反向 to-one 禁止。分层独立校验原则（`data-dependency-matrix.md:41`）只豁免"ORM 单向 + S 写反向"，不豁免"ORM 反向"。 |
| hr → projects（`ErpPrjProject`/`ErpPrjTask`） | `module-hr/model/app-erp-hr.orm.xml:496-497` 两个 `<to-one>` + `:1374,1383` 外部实体声明 + `erp-hr-dao/pom.xml:42` 依赖 `app-erp-projects-dao` | `module-boundaries.md:45` projects 允许列表只列「finance ORM 引用 + purchase/sales ORM 只读」，**未列 hr** | 🟡 **未合法化**。需补 owner docs 白名单或改机制 D。 |
| drp → inventory（`ErpInvStockMove`） | `module-drp/model/app-erp-drp.orm.xml:240-241` `<to-one name="inboundMove/outboundMove" refEntityName="app.erp.inv.dao.entity.ErpInvStockMove">` + `:318` 外部实体声明 + `erp-drp-dao/pom.xml` 依赖 `app-erp-inventory-dao` | `module-boundaries.md` 未列 drp 依赖列表；`data-dependency-matrix.md:54` 仅将 drp 列为顶层域 | 🟡 **未合法化**。同上。 |

**正面佐证**：M2/M3 中以下边已在 owner docs 合法化，不算违规——`maintenance → assets`（`module-boundaries.md:27`）、`manufacturing → inventory`（`:25`）、`finance → projects/assets`（`:45` 单向合法）。

**风险**：
1. assets → finance 反向边在 DAG 中形成潜在环（finance→assets 已是合法边），codegen 拓扑排序可能失败或产生非预期生成顺序。
2. 未合法化的边导致 owner docs 与实现再次脱节（前序 M1 的同一类问题）。

**修复**：
- assets → finance：改用机制 D（纯外键 `voucherId` + `@BizLoader` 懒加载），删除 ORM to-one 与 pom 依赖。assets 过账写入仍走 `IErpFinVoucherBiz`（S 写合法）。
- hr → projects、drp → inventory：在 `module-boundaries.md` 补充裁决（若业务确需 ORM 关联），或同样改机制 D。

---

### N3. `_app.orm.xml`（`_` 前缀生成文件）被手改

**违反规则**：`ai-defaults.md` 顶部「硬停止规则」原文：**"不允许手工修改任何生成物。包括所有以下划线开头的文件（如 `_*.xml`、`_*.java`、`_*.xmeta`、`_app.orm.xml`、`_service.beans.xml`）以及 `_gen/` 目录下的所有文件。"** 反模式表再次强调："这些文件由 codegen 管线从 ORM 模型自动生成，改了会在 `mvn install` 时被覆盖。"

**证据**（`git diff` working tree）：

| 文件 | 改动 | 源模型是否同步 |
| --- | --- | --- |
| `module-assets/erp-ast-dao/src/main/resources/_vfs/erp/ast/orm/_app.orm.xml` | 在 `erp-ast/adjustment-type` 字典将 `REVALUATION` 拆为 `REVALUATION_UP` + `REVALUATION_DOWN` | ✅ `module-assets/model/app-erp-assets.orm.xml:86-87` 已有相同改动 |
| `module-finance/erp-fin-dao/src/main/resources/_vfs/erp/fin/orm/_app.orm.xml` | 在 `erp-fin/voucher-source-bill-type` 字典新增 `VALUE_ADJUSTMENT`（资产减值/重估） | ❌ `module-finance/model/app-erp-finance.orm.xml` 无对应改动 |

**根因**：开发者直接手改生成物添加字典项，而非修改源模型后触发 `mvn install` 重新生成。assets 域虽同步了源模型，但仍手改了生成物（应只改源模型）；finance 域连源模型都没改，下次 codegen 会丢失此项。

**风险**：下次 `mvn install` 触发 codegen 时，finance 域的 `VALUE_ADJUSTMENT` 字典项会被静默覆盖，导致资产价值调整过账时找不到凭证来源类型。

**修复**：
1. 还原两个 `_app.orm.xml`。
2. 在 `module-finance/model/app-erp-finance.orm.xml` 对应字典补 `VALUE_ADJUSTMENT`。
3. 触发 `mvn install` 让 codegen 重新生成。

---

### N4. 敏感凭证字段明文存储 + XMeta 全开（GraphQL 安全）

**违反规则**：`ai-defaults.md` 反模式表「DTO 日期时间字段使用 String 类型」隐含的字段安全管控原则 + `error-handling.md`「敏感信息不要直接传入参数；如果业务必须包含，先掩码」的精神。XMeta 应通过 `queryable="false"` 或 `ext:mask` 控制敏感字段不通过 GraphQL 返回。

**证据**：

| 实体 | 字段 | ORM 定义 | XMeta 控制 |
| --- | --- | --- | --- |
| `ErpLogCarrierConfig`（承运商配置） | `apiKey` VARCHAR(500) | `module-logistics/model/app-erp-logistics.orm.xml:132` | `_ErpLogCarrierConfig.xmeta:46-49` `queryable="true" insertable="true" updatable="true"`，无 mask |
| 同上 | `apiSecret` VARCHAR(500) | `:133` | `:50-53` 同上，全开 |
| 同上 | `credentials` JSON VARCHAR | `:134` | `:54-57` 同上，全开 |
| `ErpB2bPartnerCredential`（伙伴凭证） | `credentialValue` VARCHAR(2000) | `module-b2b/model/app-erp-b2b.orm.xml`（propId 5） | `_ErpB2bPartnerCredential.xmeta:39-42` `queryable="true" insertable="true" updatable="true"`，无 mask |
| `ErpB2bPartnerProfile` | `webhookSecret` VARCHAR(500) | `module-b2b/model/app-erp-b2b.orm.xml:320` | 同实体 xmeta 同样全开（未抽查到 mask 配置） |

**风险**：任何拥有 `ErpLogCarrierConfig:find_list` 或 `ErpB2bPartnerCredential:find_list` 权限的用户，可通过 GraphQL 直接获取承运商 API 密钥、伙伴 OAuth token、webhook 签名密钥明文。这些凭证通常用于对接外部系统（如物流承运商 API、EDI 合作方），泄露后可被冒用下单或篡改货运状态。

**修复**：
1. XMeta 将上述字段 `queryable="false"`（详情页按需 `@BizLoader` 返回掩码值，如 `StringHelper.maskMiddle(apiSecret, 3, 4)`）。
2. 评估存储层加密（平台 `IEncryption` 机制），至少 `apiSecret`/`credentialValue`/`webhookSecret` 应加密存储。
3. 审计 `ErpB2bPartnerCredential` 的访问权限矩阵，确认仅限集成管理员角色。

---

## 🟡 中等发现

### N5. 扩展域跨域写绕过 I*Biz（前序 S2 的扩散）

**违反规则**：`ai-defaults.md` 反模式表「`IDaoProvider.daoFor(Xxx.class).*` 在 BizModel 中访问其他实体 → 业务代码：注入 `I*Biz` 接口」+ `cross-module-entity-reference.md §7`「业务表之间引用走纯外键 + 弱指针 + I*Biz」。

**证据**（代表性）：

- `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/mrp/MrpReleaseService.java:108-130` — 跨域**写**构造 `ErpPurOrder` + `ErpPurOrderLine`（purchase 域实体）：
  ```java
  IEntityDao<ErpPurOrder> orderDao = daoProvider.daoFor(ErpPurOrder.class);
  ErpPurOrder order = orderDao.newEntity();
  // ... 直接 saveEntity 跨域生成采购单
  ```
  Javadoc（`:38-42`）明确承认："**偏离计划 Task Route「调 I*Biz」Decision**…IErpPurOrderBiz 仅提供订单头级通用 CRUD（save(Map)），无 purpose-built createFromMrpLine 方法…故释放直接持久化目标域实体（service-helper 范式）"。

- `module-projects/erp-prj-service/.../cost/ExpenseCostAggregator.java:101,115,124,165,172,185,198,205,215` — 9 处 `daoProvider.daoFor(...)` 跨聚合访问 `ErpFinExpenseClaimLine`（finance）/`ErpPrjCostCollection`（自身聚合写）。同文件 `ProjectCostAggregator.java` 共 17 处。

- `module-manufacturing/.../mrp/DemandAggregator.java:85-86,237` — 跨域读 `ErpSalOrder`/`ErpSalOrderLine`/`ErpInvStockBalance`。

**与前序 S2 的区别**：前序 S2 主要发现 purchase/sales/inventory/finance 四域的跨实体访问；本轮发现扩展域（manufacturing/projects）的跨域写更密集，且开发者已在 Javadoc 中"承认违规但选择 service-helper 范式"——这是更危险的信号，说明反模式正在被当作"另一种合法风格"传播。

**修复**：在 `IErpPurOrderBiz`/`IErpMfgWorkOrderBiz` 补 `createFromMrpLine(...)` 等 purpose-built 方法，让 MrpReleaseService 改走 I*Biz。或按 ai-defaults 例外注释规范，在代码中明确记录"为何必须绕过"。

---

### N6. quality 域抛 `IllegalStateException` 绕过 NopException 体系

**违反规则**：`error-handling.md` 反模式表「自定义异常类不继承 `NopException`（如 `extends RuntimeException`）→ 绕过框架异常体系，丢失 ErrorCode、i18n、结构化错误响应等能力。所有业务异常必须直接或间接继承 `NopException`」+「`throw new RuntimeException("some message")` → 绕过框架异常体系，上层无法统一处理」。

**证据**：

- `module-quality/erp-qa-service/src/main/java/app/erp/qa/service/entity/InspectionResultEvaluator.java:71`
  ```java
  if (lines == null || lines.isEmpty()) {
      throw new IllegalStateException("质检单无质检行，无法汇总结果");
  }
  ```
  `IllegalStateException` 继承 `RuntimeException`，不继承 `NopException`，中文消息未走 i18n，无 ErrorCode，前端无法按错误码匹配处理。

- `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java:69`
  ```java
  throw new IllegalArgumentException("Unknown ErpFinBusinessType code: " + code);
  ```
  这是枚举 `fromCode` 静态工厂方法，属边界场景（解析外部传入的枚举码），但同样绕过 NopException。前序审计"服务层亮点 1"称"全部 `new NopException(ERR_...)`"，此例证伪了该结论的覆盖范围。

**修复**：quality 域补 `ErpQaErrors.ERR_INSPECTION_NO_LINES` 并抛 `new NopException(ERR_INSPECTION_NO_LINES)`；finance 枚举方法可保留 `IllegalArgumentException`（属 JDK 枚举解析惯例）但应在调用方 catch 后包装为 `NopException`。

---

### N7. 吞异常（`catch (Exception ignored)` / `catch → return null`）

**违反规则**：`error-handling.md` 「异常传播与捕获边界」强制约束：**"catch 后丢弃异常，既不 rethrow with cause 也没 `LOG.info` 以上 → 静默吞异常，运维不可见、排查无凭据"** + 反模式表「内部函数 catch 异常转返回值（`return null` / `return Optional.empty()`）→ 把系统故障伪装成业务结果，丢失传播链，违反 fail-fast」。

**证据**：

| 文件:行号 | 违规模式 | 问题 |
| --- | --- | --- |
| `module-inventory/.../costing/StandardCostResolver.java:106-108` | `catch (Exception ignored) { // 属性不存在时反射读取抛错，返回 null }` | 完全吞异常，仅注释解释，无 LOG.warn，原始异常链丢失。注释称"由调用方抛 ERR_STANDARD_COST_NOT_AVAILABLE"，但原始 NumberFormatException/属性不存在原因不可追溯。 |
| `module-purchase/.../entity/RequisitionToOrderConverter.java:124-126` | `catch (NumberFormatException e) { return null; }`（`parseTaxRate`） | 把格式异常伪装成"无税率"，业务上可能可接受，但违反 fail-fast；若数据脏（如税率字段被写入 "abc"），系统静默按"无税"处理，运维不可见。 |
| `module-purchase/.../entity/RequisitionToOrderConverter.java:111-115` | `catch (NumberFormatException e) { throw new NopException(...) }` | ✅ 此处正确（包装并保留上下文），作为上一条的对照。 |

**修复**：StandardCostResolver 改 `LOG.warn("read standardCost failed for material {}", materialId, e)` 后返回 null；RequisitionToOrderConverter.parseTaxRate 同样补 LOG.warn 或改为抛 `NopException(ERR_INVALID_TAX_RATE)`。

---

## 🟢 轻微发现

### N8. hr/crm 域用 `Map<String,Object>` 作为复杂返回 DTO

**违反规则**：`ai-defaults.md` 反模式表「`Map<String, Object>` 作为复杂返回 DTO → 定义 `@DataBean` DTO」。

**证据**：
- `module-hr/.../entity/ErpHrSalarySimulationBizModel.java:165,203,230,258,272,337` — 6 处方法返回 `Map<String,Object>` / `List<Map<String,Object>>`（`getComparison`/`getDepartmentSummary`/`getProjectSummary`/`getCompanySummary`/`applyBatchAdjustment`/`findAnomalies`）。
- `module-crm/.../entity/ErpCrmEventBizModel.java:104` — `List<Map<String, Object>> getLeadTimeline(...)`。
- `module-crm/.../support/EventTimelineAggregator.java:34` — 同模式。

**风险**：GraphQL schema 无法推断 Map 内部结构，前端拿不到字段元数据，IDE 无类型提示；DTO 变更无编译期保障。

**修复**：为 SalaryComparison/SalaryDepartmentSummary/LeadTimelineItem 等定义 `@DataBean` DTO。

---

## 已检查未发现新问题的区域

| 区域 | 检查方法 | 结论 |
| --- | --- | --- |
| `@Inject private` 字段 | 全项目 grep `@Inject\s+private` | ✅ 0 命中，全项目合规 |
| 第三方 JSON 库（Jackson/Gson）导入 | 全项目 grep import | ✅ main 代码 0 命中（仅在 api 模块 `*Bean.java` 用 `JsonInclude` 注解，属生成代码框架内置，非业务用法） |
| Apache Commons `StringUtils` | 全项目 grep import | ✅ 0 命中，全用平台 `StringHelper` |
| `@Inject private` + 直接注入 BizModel 实现类 | grep | ✅ 0 命中 |
| M3 新增域 dict valueType | 量化 `valueType="int"` vs `"string"` | ✅ 全部 18 域 model 文件 `int=0`，前序 O3 的"新模块优先 string"指导已被 M3 采纳（crm 全 string 23、master-data 已转 string） |
| `@BizMutation @Transactional` 冗余 | grep `@Transactional` 上下文 | ✅ 扩展域 Processor/Executor 注释明确"本类不带 @Transactional，跟随 Facade @BizMutation 事务"；唯一保留的 `ErpFinVoucherBizModel.java:42 @Transactional(REQUIRES_NEW)` 是 ai-defaults 明确允许的例外 |
| `@Inject private` 在扩展域 BizModel | 抽查 mfg/quality/assets/prj BizModel | ✅ 全部 `protected` 或 setter 注入 |
| 测试代码 throw new RuntimeException | grep | 容忍（测试边界，6 处全在 `*.java.test` 包装反射调用异常） |

---

## 根因分析

1. **前序审计的"四域聚焦"留下了扩散温床**：S1-S8 的整改路线只针对 purchase/sales/inventory/finance，M2/M3 新增域编写时仍沿用旧反模式（尤其时间获取、跨域写）。N1 的 60+ 处违规证明前序 S5 的"99% 正确"结论只对四域成立。
2. **owner docs 与实现的同步机制失灵**：N2 的 3 条新跨域边、N3 的手改 `_app.orm.xml`，都是开发者在实现时绕过 owner docs 而非更新 owner docs。前序 M1 已发现 projects 边的同类问题，但未推动建立"新增跨域 ORM 边必须先更新 module-boundaries.md"的门禁。
3. **安全维度完全未被纳入前序审计**：N4 的敏感凭证字段暴露表明，前序 V1-V4 聚焦菜单断链与状态机按钮，未覆盖 GraphQL 字段级安全。这是审计维度的盲区。
4. **"边界代码"标签被滥用**：N5 中开发者用 Javadoc"承认偏离"但继续违规，N7 中用注释解释吞异常，都是把 ai-defaults 的"例外场景"当作通用豁免。`ai-defaults.md` 例外场景只覆盖 store/infra/调度器底层，不覆盖应用层 Processor/Aggregator/Resolver。

## 优先修复顺序建议

1. **N4**（敏感凭证）—— 安全风险，立即修。XMeta `queryable="false"` + 评估存储加密。
2. **N3**（手改 `_app.orm.xml`）—— 还原生成物 + 改源模型 + 触发 codegen，避免下次 `mvn install` 丢失字典项。
3. **N2 assets→finance 反向边**—— 改机制 D，避免 DAG 环风险。
4. **N1**（时间获取）—— 扩展域一次性批量替换为 `CoreMetrics`，与 S5 整改合并。
5. **N5/N6/N7**（服务层平台 API 合规）—— 与前序 S2/S3 整改合并为统一切片。
6. **N8**（DTO）—— 重构机会，低优先级。

## 抽样覆盖说明

| 维度 | 覆盖 |
| --- | --- |
| BizModel | 全项目 grep 反模式（@Inject private、第三方 JSON、Commons、时间获取、RuntimeException、@Transactional、daoFor、@BizLoader、Map DTO）；main 代码 60+ 处时间获取违规逐条定位 |
| 跨域 ORM 边 | 全 18 域 `notGenCode` + `<to-one refEntityName>` + pom 依赖三角核对；对照 `module-boundaries.md` + `data-dependency-matrix.md` 逐边裁决 |
| 生成物纯净度 | `git status` working tree + `git log` 历史 `_gen`/`_app.orm.xml`/`_service.beans.xml`/`_dao.beans.xml` |
| GraphQL 安全 | 全项目 grep `password\|salt\|secretKey\|apiSecret\|token\|webhookSecret` + 抽查 XMeta queryable 配置 |
| 异常处理 | 全项目 grep `catch\s*\(` + `throw new (Runtime\|IllegalState\|IllegalArgument\|NullPointer)` |
| dict valueType | 18 域量化 `int` vs `string` 计数 |

> 本报告为只读分析，未修改任何代码。所有"违规"判定均附 `文件:行号` 证据并对照 `docs-for-ai` 具体规则出处，可逐条复核。N1-N8 与前序 S1-S8 + O1-O4 + M1-M4 + V1-V4 无重叠。
