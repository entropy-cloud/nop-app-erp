# Wimoor ERP vs nop-app-erp 深度对比分析

> 调研日期: 2026-07-20
> 基于: Wimoor 源码 (~/sources/erp/wimoor/) 与 nop-app-erp 设计文档/ORM 模型/BizModel 实现

---

## 目录

1. [架构风格对比](#1-架构风格对比)
2. [SaaS/多租户](#2-saas多租户)
3. [第三方 API 集成](#3-第三方-api-集成)
4. [策略模式与可插拔业务逻辑](#4-策略模式与可插拔业务逻辑)
5. [财务域深度对比](#5-财务域深度对比)
6. [库存模型对比](#6-库存模型对比)
7. [通知模型对比](#7-通知模型对比)
8. [网关与认证](#8-网关与认证)
9. [nop-app-erp 可从 Wimoor 借鉴之处](#9-nop-app-erp-可从-wimoor-借鉴之处)
10. [Wimoor 可从 nop-app-erp 借鉴之处](#10-wimoor-可从-nop-app-erp-借鉴之处)
11. [总体评估](#11-总体评估)

---

## 1. 架构风格对比

### Wimoor: Spring Cloud 微服务

Wimoor 采用标准 Spring Cloud 微服务架构：

| 模块 | 路径 | 技术栈 |
|------|------|--------|
| `wimoor-gateway` | `wimoor-gateway/` | Spring Cloud Gateway + Redis |
| `wimoor-amazon` | `wimoor-amazon/` | Amazon SP-API 集成 |
| `wimoor-erp` | `wimoor-erp/` | 核心 ERP：物料/库存/采购 |
| `wimoor-modules/finance` | `wimoor-modules/wimoor-finance/` | 财务模块 |
| `wimoor-admin` | `wimoor-admin/` | 用户/SaaS 租户套餐 |
| `wimoor-common` | `wimoor-common/` | 共享基础设施 |

特征：
- 物理服务拆分，独立部署单元（`wimoor-gateway/pom.xml`，`SecurityGlobalFilter.java:40-43` 作为独立 Gateway 进程）
- 服务间通信通过 Feign + Nacos 服务发现（依赖 `spring-cloud-starter-openfeign`）
- 每个服务独立数据库或共享数据库（多租户场景）
- Spring Cloud Gateway 作为统一入口
- Nacos 配置中心管理多服务配置

### nop-app-erp: 多 Maven 模块 + 单一部署

nop-app-erp 采用 Nop Platform 的"多领域工程模型"模式：

| 域 | 逻辑工程名 | 物理目录 | 代码生成骨架 |
|----|-----------|---------|------------|
| 主数据 | `app-erp-master-data` | `module-master-data/` | `erp-md-{dao,service,web,...}` |
| 库存 | `app-erp-inventory` | `module-inventory/` | `erp-inv-{dao,service,web,...}` |
| 采购 | `app-erp-purchase` | `module-purchase/` | `erp-pur-{dao,service,web,...}` |
| 销售 | `app-erp-sales` | `module-sales/` | `erp-sal-{dao,service,web,...}` |
| 财务 | `app-erp-finance` | `module-finance/` | `erp-fin-{dao,service,web,...}` |
| 聚合启动 | `app-erp-all` | `app-erp-all/` | Quarkus main 聚合 |

特征（证据：`docs/architecture/domain-module-split-analysis.md:7-11`）：
- **一套代码库、一个部署单元**：所有域工程通过 Maven 依赖聚合到 `app-erp-all`（`domain-module-split-analysis.md:31`：`最后由 app-erp-all 聚合依赖启动`）
- **模型驱动代码生成**：每个域有独立 `model/app-erp-<domain>.orm.xml` → `nop-cli gen` 生成 `-dao/-service/-web` 模块集（`domain-module-split-analysis.md:24-26`）
- **物理模块 = 逻辑边界，非部署边界**：模块拆分是编译期依赖 DAG（`domain-module-split-analysis.md:186`：`Maven 依赖方向严格单向（DAG）`），运行时仍是单一 Quarkus 应用
- **跨域调用通过 `I*Biz` 接口**（`domain-design-guidelines.md:35`：`跨域调用只通过接口：使用 I*Biz 接口`）

### 对比分析

| 维度 | Wimoor | nop-app-erp | 评价 |
|------|--------|-------------|------|
| 部署模型 | 多进程独立部署 | 单进程聚合部署 | nop 单进程开发/运维简单；Wimoor 微服务独立扩缩容但运维复杂 |
| 模块内聚 | 物理服务边界=业务边界 | 编译模块边界=业务边界 | nop 代码组织更贴近 DDD bounded context 而不引入分布式复杂度 |
| 服务通信 | Feign + Nacos（网络 RPC） | `@Inject I*Biz` + Maven（进程内接口） | nop 无网络开销，编译期安全；Wimoor 需处理服务发现/熔断/超时 |
| 代码生成 | MyBatis-Plus + 手写 | ORM 模型 → 代码生成全骨架 | nop 模型驱动减少大量重复代码 |
| 开发效率 | 每个新模块需手写全部 Controller/Service/DAO | 改 orm.xml → `mvn install` 增量生成 | nop 更适合声明式开发 |
| 可扩展性 | 水平扩缩容，但对 Nacos/Redis 有 infra 依赖 | 垂直扩展，启动实例数取决于 Quarkus | Wimoor 适合多租户 SaaS 高并发；nop 适合中大型企业内部部署 |

**关键差异**：Wimoor 的微服务是**运行时拆分**，nop 的多模块是**编译时/逻辑拆分**。两者都实现了领域解耦，但 nop 避免了分布式事务和网络开销——这对业财一体（如采购入库同事务写库存+写凭证）有明显优势（`domain-design-guidelines.md:52-55` 事务边界原则）。

---

## 2. SaaS/多租户

### Wimoor: shopid 逻辑隔离 + 套餐计费

Wimoor 的 SaaS 模型证据（`wimoor/` 源码）：

- **租户标识**：`shopid`（公司ID）作为业务表的租户列——`Material.java:44` (`private String shopid`)、`Warehouse.java:59` (`private String shopid`)
- **用户类型**：三档用户——admin / manager / member（`SecurityGlobalFilter.java:80-81`）
- **套餐计费系统**：
  - `SysTariffPackages` — 订阅套餐定义（Wimoor 调研报告 §4.3）
  - `SysTariffPackagesAppend` — 增值功能
  - `ManagerLimit` — 租户资源限制
  - `SysCustomerOrder` / `SysCustomerInvoice` — 客户订阅订单/发票
- **权限规则**：URL 级 RBAC 缓存 Redis `system:perm_roles_rule:url:`（`SecurityGlobalFilter.java:96`）

### nop-app-erp: 多组织模型，无显式 SaaS

nop-app-erp 多组织设计证据：

- **orgId 作为标准字段**：所有业务单据头携带 `orgId`（引用 `ErpMdOrganization`），`domain-design-guidelines.md:433-435`：`组织与时间维度` — `orgId` 是查询过滤与权限隔离的依据
- **单据编号作用域**：`在 orgId 内唯一`（`domain-design-guidelines.md:441-445`）
- **配置优先级链**：三级覆盖：按实体 → 按组织 → 全局默认（`domain-design-guidelines.md:629-638`）
- **Master-data 的 `ErpMdOrganization`**（`module-master-data/model/app-erp-master-data.orm.xml:108-115`）：组织类型字典包含 GROUP/COMPANY/BRANCH/DEPARTMENT/WORKSHOP/STORE
- **没有显式的 SaaS 套餐/计费模型**：nop-app-erp 当前设计文档未提及 `SysTariffPackages` 类模型或 SaaS 订阅

### 对比分析

| 维度 | Wimoor | nop-app-erp |
|------|--------|-------------|
| 租户隔离 | `shopid` 逻辑隔离，共享数据库 | `orgId` 组织隔离，无显式租户概念 |
| 计费模型 | 完整套餐+增值+订单+发票体系 | 无（产品定位为单租户/私有部署） |
| 资源限制 | `ManagerLimit` 按租户限制用量 | 无 |
| 用户分层 | admin/manager/member 三级 | `domain-design-guidelines.md:207-214` 角色职责分离（采购员/销售员/审核人/财务员/管理员） |
| 数据权限 | URL 级 + `UserLimitDataType` 字段级 | `domain-design-guidelines.md:217-219` 行级+字段级设计（未落地实现） |

**结论**：Wimoor 是完整的多租户 SaaS ERP，具备计费闭环；nop-app-erp 当前定位为私有部署/单租户 ERP，`orgId` 为多组织维度（集团→公司→部门层级），非多租户。若 nop 未来需 SaaS 化，可直接借鉴 Wimoor 的 `SysTariffPackages` + `ManagerLimit` 套餐模型。

---

## 3. 第三方 API 集成

### Wimoor: ApiBuildService 工厂 + Swagger Codegen SDK

Wimoor 围绕 Amazon SP-API（Selling Partner API）构建完整集成层：

**ApiBuildService 工厂**（`ApiBuildService.java:35-507`）：
- 单一 `@Component`，注册为 Spring Bean
- 构造所有 SP-API 客户端实例：`getOrdersV0Api()`、`getReportsApi()`、`getFeedsApi()`、`getFbaInventoryApi()`、`getFinancesApi()` 等 20+ API 客户端
- 每个方法通用 Builder 模式：设置 LWA 凭证 → Region/Endpoint → Rate Limit → build
- 认证使用 Swagger Codegen 生成的 Java SDK（`com.amazon.spapi.api.*`）
- Rate Limiting 通过 `AmazonAuthority implements RateLimitConfiguration`（Wimoor 调研报告 §6.2 指此为反模式——领域实体耦合基础设施）

**已集成 API**（调研报告 §2.2）：
| SP-API | 用途 |
|--------|------|
| Orders API | 订单检索 |
| Reports API | 报告请求/结果 |
| Feeds API | Feed 提交 |
| FBA Inbound API | 入仓计划/货件 |
| FBA Inventory API | FBA 库存查询 |
| Finances API | 财务事件 |
| Catalog API | 产品目录 |
| Product Pricing API | 产品定价 |
| Notifications API | 通知订阅 |

**广告 API**（`wimoor-amazon-adv` 模块）：对接 Amazon Advertising API，含活动管理、报表体系、出价优化规则。

### nop-app-erp: SPI Provider + 模型驱动

nop-app-erp 当前的第三方集成模式：

- **无显式外部 API 对接**：18 业务域均为 ERP 内部模型，未设计电商/物流/支付平台集成层
- **凭证生成 SPI**（`/docs/design/finance/posting.md:184-195`）：`IErpFinAcctDocProvider` 接口 + `@Inject Map<BusinessType, IErpFinAcctDocProvider>` 是可插拔扩展的典型 SPI 模式
- **设计指引**（`domain-design-guidelines.md:701`）：`第三方系统对接（TMS/EDI）` 推荐使用 `SPI Provider（@Inject Map）`，禁止硬编码 switch/case

### 对比分析

| 维度 | Wimoor | nop-app-erp |
|------|--------|-------------|
| 现有集成 | 亚马逊 SP-API 全栈（订单/报告/库存/财务/通知/广告） | 无（纯内部 ERP） |
| 认证体系 | LWA Token Exchange + Restricted Data Token + Region Mapping | 尚无 |
| SDK 策略 | Swagger Codegen 生成 Java SDK | Nop Platform delta 模式，可类似生成 |
| 工厂模式 | `ApiBuildService` 统一构建 20+ API 客户端 | `ErpFinAcctDocRegistry` 类似 SPI 注册中心（`posting.md:211-219`） |
| 速率限制 | `RateLimitConfiguration` + 代理支持 | 无 |
| 接口风格 | 每个 API 独立 Builder 方法（方法级重复代码） | 类型安全 Map 注册（`posting.md:197`） |

**评价**：Wimoor 的 `ApiBuildService` 虽然代码重复度高（每个 API 客户端 10+ 行几乎相同的 Builder 代码），但作为单一工厂可维护。nop 的 `ErpFinAcctDocRegistry` 使用 `@Inject Map` 自动注册更优雅。nop 若未来集成电商 API，应借鉴 Wimoor 的"工厂+SDK+认证引擎"三层架构，但可采用 SPI Map 注册替代方法级工厂。

---

## 4. 策略模式与可插拔业务逻辑

### Wimoor: 利润计算 + 结账策略模式

**利润计算策略**（调研报告 §2.4）：

```java
// Wimoor: 策略接口 + 10 个站点实现
USProfitServiceImpl implements IProfitService  // 美国
UKProfitServiceImpl                            // 英国
EUProfitServiceImpl                            // 欧洲
JPProfitServiceImpl                            // 日本
CAProfitServiceImpl                            // 加拿大
AUProfitServiceImpl                            // 澳大利亚
MXProfitServiceImpl                            // 墨西哥
...

// 子计算器注入到站点策略中
ReferralFeeServiceImpl       // 推荐费
VariableClosingFeeServiceImpl // 可变结算费
FbaFormatServiceImpl         // FBA 配送费
FbaLabelingFeeServiceImpl    // 贴标费
InventoryStorageFeeServiceImpl // 仓储费
```

**结账策略**（`IFinClosingTemplateStrategy.java:9-23`）：

```java
public interface IFinClosingTemplateStrategy {
    String getFtype();
    void generateVoucher(UserInfo userInfo, String templateId, String periodCode);
    void initTemplateItem(FinClosingTemplate template);
}
```

**策略工厂**（`FinClosingTemplateStrategyFactory.java:13-43`）：

```java
@PostConstruct
public void init() {
    for (IFinClosingTemplateStrategy strategy : strategyList) {
        strategyMap.put(strategy.getFtype(), strategy);
    }
}
```

模式特征：`@Autowired List<Strategy>` → `@PostConstruct` 手工构建 `Map<ftype, Strategy>` → 按 `ftype` 路由。

### nop-app-erp: Processor 模式 + `@Inject Map`

nop-app-erp 有多个可插拔模式：

1. **凭证生成 SPI**（`posting.md:184-195`）：

```
ErpFinAcctDocRegistry
  ├─ @Inject List<IErpFinAcctDocProvider> providers  // 启动时收集
  └─ @Inject List<IErpFinFactsValidator> validators   // 校验/改写
```

2. **Processor 两层结构**（`ErpMfgWorkOrderBizModel.java:34-37`）：

```java
@Inject ErpMfgWorkOrderProcessor workOrderProcessor;
@Inject ErpMfgScheduleToJobCardProcessor scheduleToJobCardProcessor;
```

BizModel 是 Facade，Processor 是 `protected step` 方法实现——下游可逐 step 覆盖（`ErpMfgWorkOrderBizModel.java:21-26` 注释）。

3. **审批钩子**：审批动作通过 xbiz `<observes>` / `.xwf` 注入（`notification-strategy.md:60-62`），非硬编码。

### 对比分析

| 维度 | Wimoor | nop-app-erp |
|------|--------|-------------|
| 策略注册 | `@PostConstruct` 手工构建 Map | `@Inject Map<K, V>` 自动注入（平台 SPI） |
| 策略接口 | 每个域自定接口（`IFinClosingTemplateStrategy`, `IProfitService`） | 统一 `IErpFinAcctDocProvider` SPI |
| 扩展方式 | 新增 impl + 工厂自动发现（List 注入） | 新增 impl + @Inject Map 自动发现 |
| 业务编排 | 策略方法内直接写完整逻辑 | Facade + Processor 两层，Processor 可逐 step 覆盖 |
| 声明式钩子 | 无 | xbiz `<observes>` 钩子 |
| 策略粒度 | 按站点/按 ftype 粗粒度 | 按 businessType 细粒度 |

**评价**：两者都采用经典的策略模式 + Spring `@Inject List` 自动发现，设计思路高度一致。nop 的创新是**Processor 两层结构**——BizModel 只做请求路由和事务管理，Processor 提供可被子类覆盖的 `protected step` 方法，这是对传统 Strategy + Template Method 的融合。Wimoor 的利润计算子计算器注入模式（如 `ReferralFeeServiceImpl` 注入到 `USProfitServiceImpl`）对 nop 的 GL Distribution 场景（`posting.md:260`）有参考价值。

---

## 5. 财务域深度对比

### Wimoor: 传统 MVC 财务

**会计科目**（`FinAccountingSubjects.java:19-399`）：
- 继承 `TreeEntity`，天然树形结构
- 层级编码 `subjectCode`（支持 `parentCode` 自引用）
- 布尔标记字段丰富：`isCash`, `isForeignCurrency`, `isExchange`, `isParallel`, `isAuxiliary`
- 无显式多账套概念

**凭证**（`FinVouchers.java:19-278`）：
- 整数状态码：1-草稿，2-已审核，3-已过账，4-已作废（`FinVouchers.java:52-53`）
- `dataSource` 区分手动/导入/结账模板/单据同步（`FinVouchers.java:55-56`）
- 辅助核算通过独立表 `FinVoucherEntriesAuxiliary`
- 无 `businessType` / `posted` 标准字段体系

**结账**（`IFinClosingTemplateStrategy.java` + `FinClosingTemplateStrategyFactory.java`）：
- 策略模式按 `ftype` 路由：成本摊销、汇兑损益、损益结转
- `FinClosingTemplate` / `FinClosingTemplateItem` 模板定义
- `FinClosingTemplateVouchers` 结账生成凭证
- 策略接口包含 `initTemplateItem` 初始化 + `generateVoucher` 生成凭证

### nop-app-erp: 三层建模 + 业财一体

**会计科目**（`module-master-data/model/app-erp-master-data.orm.xml:91-107` + 完整 orm.xml 中的 `ErpMdSubject`）：
- 科目表归 master-data 域（共享主数据），财务域只用不维护
- 多账套（`acctSchemaId`）：财务账/管理账/税务账/合并账/预算账（`app-erp-finance.orm.xml:11-12`）
- 标准字典 `erp-md/subject-class`：ASSET/LIABILITY/EQUITY/INCOME/EXPENSE/COST
- 借贷方向 `erp-md/subject-direction`：DEBIT/CREDIT

**凭证模型**（`docs/design/finance/README.md:39-55`）：

```
会计凭证（Voucher）
  ├─ 凭证字（收/付/转）+ 凭证号（按字连续编号）+ 凭证日期 + 状态
  └─ 凭证分录行（VoucherLine）[]
       ├─ 科目（subjectCode）
       ├─ 每行只填借方或贷方一侧
       ├─ 业务维度辅助核算
       └─ 源币种金额 + 本位币金额
```

**业财一体三件套**（`docs/design/finance/README.md:93-96`）：
1. `posted` 标志（业务单据上，标识已过账）
2. `ErpFinVoucherBillR` 业财回链表（凭证 ↔ 源单据双向关联）
3. 兜底扫描 + 冲销双向闭环

**三层过账引擎**（`posting.md:11-42`）：

```
第①层 底座：业务单据+库存（SYNC 强一致，不可配置）
第②层 凭证生成时序（按 billType 可配 SYNC/ASYNC）
第③层 兜底（posted 幂等 + 业财回链 + 物理锁定 + 红字冲销）
```

**凭证模板**（`posting.md:142-177`）：
- `VoucherTemplate`（头/行）按 `businessType` 路由
- 占位符填充：`"AMOUNT"` / `"TAX_AMOUNT"` / `"TOTAL"`
- 科目映射多维决策：物料类别/往来单位组/仓库/部门

**会计期间状态机**（`period-close.md:150-158`）：`OPEN → CLOSING → CLOSED → CLOSED_FINAL` + 反结账

**年度结转**（`period-close.md:241-271`）：本年利润→未分配利润 + 辅助账结转 + 次年期间自动创建

**坏账准备**（`period-close.md:11-12`）：Allowance 法 + 账龄分桶 + NRV 门控

**businessType 字典**（`app-erp-finance.orm.xml:60-127`）：40+ 业务类型枚举，覆盖进销存/资产/制造/物流/质量/人力/票据等全域

### 对比分析

| 维度 | Wimoor | nop-app-erp |
|------|--------|-------------|
| 凭证状态 | 整数码（1/2/3/4），无业务语义 | 字典码（DRAFT/POSTED/CANCELLED），语义编码 |
| 业财一体 | 无显式 `posted` 标志；FinVouchers 有 `dataSource=4-单据同步` 但非标准化 | `posted` 标准字段全体系统一（`domain-design-guidelines.md:466-472`） |
| 业务类型枚举 | 无标准字典，结账模板按 `ftype` 路由 | 40+ businessType 字典统一全域（`app-erp-finance.orm.xml:60-127`） |
| 回链机制 | 无显式 `VoucherBillR`（仅在 entries 层有 voucherId 关联源单） | `ErpFinVoucherBillR` 标准双向回链（`posting.md:404-413`） |
| 多账套 | 无（`isParallel` 是科目属性，非账套维度） | `acctSchemaId` 多账套并行（5 套）（`README.md:97`） |
| 多币种 | `currency` + `exchangeRate` 在 entries 层 | `currencyId` + `exchangeRate` + `amountSource` + `amountFunctional` 四件套（`domain-design-guidelines.md:488-493`） |
| 结账策略 | 策略模式 + 模板方法 | 同策略模式 + 但多模块关账（AR/AP/INV/AST/GL）按序 + config-gated |
| 科目模型 | `FinAccountingSubjects` 树形继承 | `ErpMdSubject` 树形 + 多账套独立科目体系 |
| 成本核算 | 无（Wimoor 是跨境电商 ERP，成本主要来自 FBA 费用） | 完整成本核算（移动加权平均/FIFO/批次）+ 到岸成本（`period-close.md:114-145`） |
| 过账时序 | 无 | SYNC/ASYNC 可配（`posting.md:44`） |
| 冲销双向闭环 | 无 | `VoucherReversedEvent` → 各域监听回退（`posting.md:347-365`） |
| 坏账准备 | 无 | Allowance 法 + 账龄分桶 + 期末门控（`period-close.md:11-12`） |
| 期间状态 | 简化（periodCode 字符串） | 5 态：OPEN/CLOSING/CLOSED/CLOSED_FINAL/NEVER_OPENED |

**评价**：这是两项目差距最大的维度。nop-app-erp 的财务域深度远超 Wimoor——nop 的 3 层过账引擎、businessType 字典体系、多账套并行、业财一体 posted 标志、双向冲销闭环、坏账准备 Allowance 法，均是 Wimoor 不具备或仅简略实现的能力。Wimoor 的财务模块更像"记账工具"而非"业财一体引擎"。

---

## 6. 库存模型对比

### Wimoor: 简化仓库-库存

**仓库**（`Warehouse.java:25-147`）：
- `ftype` 类型：self/self_test/usable/unusable
- `parentid` 父子层级（`children` 自引用）
- `fbawareid` 关联 FBA 海外仓
- `ishungry` 允许负库存
- `isstocktaking` 盘点锁定
- 无 Location（库位）概念

**库存**（调研报告 §3.2）：
- `Inventory`：`warehouseid` + `materialid` + `quantity` + `status`——简单的存量化表
- `InventoryRecord`：`formid` + `materialid` + `warehouseid` + `delta` + `type`——流水记录
- `InventoryMonthSummary`：月度库存汇总
- 无成本层、无批次/序列号管理（Material 有 `issfg` 判断是否组合产品，但无批次追踪）

### nop-app-erp: 三层库存模型

**三层模型**（`docs/design/inventory/README.md:26-35`）：

| 层 | 可变性 | 业务含义 |
|----|--------|----------|
| StockMove（库存移动单） | 可确认/取消 | 计划层"一次移动意图" |
| StockLedger（库存流水） | 不可变 | 变动记录，含成本、FIFO 队列 |
| StockBalance（库存余额） | 由流水驱动 | 按物料×仓库×库位×批次的快照 |

**ORM 模型**（`module-inventory/model/app-erp-inventory.orm.xml:154-200`）：
- `ErpInvStockMove` 头字段：`sourceWarehouseId` / `sourceLocationId` / `destWarehouseId` / `destLocationId` / `relatedBillType` / `relatedBillCode`
- `originMoveId` / `originReturnedMoveId` 上下游追溯链
- 关系：`to-one` 仓库/库位/组织 + `to-many lines` 行
- 跨模块引用 master-data 的仓库库位（`inventory.orm.xml:185-190`）

**库存能力**（`docs/design/inventory/README.md:38-49`）：
- 批次台账（Batch）：保质期管理、FIFO/FEFO 选择策略
- 序列号台账（SerialNumber）：单品追踪
- 调拨单（TransferOrder）、盘点单（StockTake）、拣货单
- 作业类型（OperationType）参数化
- 预留量（Reserved Quantity）与可用量（Available Quantity）
- 所有权维度：自有/VMI/寄售/客供（`inventory.orm.xml:91-96`）

**批次策略配置**（`docs/design/inventory/README.md:92-108`）：FIFO/FEFO/手工指定三级优先级。

### 对比分析

| 维度 | Wimoor | nop-app-erp |
|------|--------|-------------|
| 模型层数 | 2 层（Inventory + InventoryRecord） | 3 层（StockMove + StockLedger + StockBalance） |
| 成本追踪 | 无（Material 有 priceWavg 但无成本层） | 完整成本层（CostLayer）+ FIFO 队列 + 移动加权平均 |
| 批次/序列号 | 无 | `ErpInvBatch` / `ErpInvSerialNumber` |
| 库位管理 | 无（Warehouse 父子结构粗粒度） | `ErpMdLocation` 库位（master-data） |
| 预留机制 | 无 | 预留量/可用量/预留状态机（`inventory.orm.xml:72-78`） |
| 追溯链 | 无 | `originMoveId` + `originReturnedMoveId` |
| 盘点 | 无 | `StockTake` + 盘点类型（FULL/SAMPLE/CYCLE） |
| 拣货 | 无 | `Picking` + 拣货状态（PENDING/PICKING/PICKED） |
| 所有权追踪 | 无 | `ownership-type` 4 态（自有/VMI/寄售/客供） |
| 到岸成本 | 无 | landed cost allocation（`inventory.orm.xml:118-131`） |

**结论**：nop-app-erp 的三层库存模型更接近商业 ERP 标准（类似 iDempiere `M_InOut` → `M_Transaction` → `M_StorageOnHand`），而 Wimoor 的 `Inventory` + `InventoryRecord` 简化模型满足跨境电商 FBA 场景（库存量主要从 Amazon SP-API 同步，非内部精细化管控）。

---

## 7. 通知模型对比

### Wimoor: 无显式通知子系统

Wimoor 没有独立的通知模块（调查源码未发现 `notify` 相关模块或实体）。通知机制通过以下方式实现：

- 站内消息可能依赖通用基础设施（`wimoor-common`）
- 未发现独立 `INotificationBiz` 接口或 `*Notification*` 实体

### nop-app-erp: 跨域事件驱动通知派发

**架构**（`docs/architecture/notification-strategy.md:7-33`）：

```
notify(eventType, context) → 解析模板 → 解析接收人 → 频控合并
  → 落站内消息表 → config-gated 派发外发通道（邮件/短信）
```

**核心组件**：

| 组件 | 表/实体 | 职责 |
|------|---------|------|
| 通知模板 | `ErpSysNotificationTemplate` | `${var}` 插值模板 |
| 通知实例 | `ErpSysNotification` | 站内消息实例 |
| 已读记录 | `ErpSysNotificationRead` | 已读状态（唯一键防重复） |
| 接收人解析 | `recipientResolver`（ROLE/ORG/PARTNER/USER_LIST） | 按角色/组织/用户列表解析 |

**业务接线**（`notification-strategy.md:37-48`）：

| 事件类型 | 触发点 | 接收人 |
|----------|--------|--------|
| `cs.sla-overdue` | `ErpCsTicketBizModel.scanOverdueTickets` | ROLE 客服主管 |
| `fin.posting-exception` | `ErpFinPostingExceptionRecorder.record` | ROLE 财务员 |
| `sal.credit-over-limit` | `CreditLimitChecker.check` | ROLE 销售员 |
| `crm.event-reminder` | `ErpCrmEventReminderJob.execute` | USER_LIST ownerUserId |
| `mfg.production-variance` | `ProductionVarianceCalculator.calculateVariances` | ROLE 生产主管 |

**审批通知**（`notification-strategy.md:52-62`）：4 实体 WORKFLOW 审批接入，通过 `.xwf` listener 和 `on-enter` 步骤触发 `wf.<entity>.result` / `wf.<entity>.task-assigned` / `wf.<entity>.cc`。

**前端收件箱**（`inbox-patterns.md:9-18`）：三 tab（未读/已读/全部）+ 未读计数 + 全部标记已读 + 详情 drawer。

### 对比分析

| 维度 | Wimoor | nop-app-erp |
|------|--------|-------------|
| 通知子系统 | 无独立模块 | `module-notify` 完整子系统 |
| 模板引擎 | 无 | `${var}` 插值模板 + 按事件类型查找 |
| 接收人解析 | 无 | ROLE/ORG/PARTNER/USER_LIST 多策略 |
| 频控合并 | 无 | 按类型+时间窗口合并（`notification-strategy.md:17-22`） |
| 审批通知 | 无 | `.xwf` listener 全生命周期通知 |
| 前端收件箱 | 无 | AMIS 三 tab inbox 页面 |
| 外发通道 | 可能有通用邮件 | `IEmailSender` / `ISmsSender` SPI（config-gated） |
| 通知失败降级 | 无 | try/catch warn-and-continue（`notification-strategy.md:48`） |

**结论**：nop-app-erp 的通知子系统是 Wimoor 完全不覆盖的能力。基于 Nop Platform 的事件驱动 + 模板 + 接收人解析 + 频控 + 收件箱，构成了完整的跨域通知基础设施。这是 nop-app-erp 作为"设计先行"方法论的最佳体现。

---

## 8. 网关与认证

### Wimoor: Spring Cloud Gateway + Redis Token + URL RBAC

**SecurityGlobalFilter**（`SecurityGlobalFilter.java:43-191`）：

1. **Token 验证**：从 HTTP Header `jsessionid` 取 token → Redis `hasKey(CacheConstants.LOGIN_TOKEN_KEY + token)` 验证 → 刷新过期时间（3小时）（`SecurityGlobalFilter.java:134-150`）
2. **URL 白名单**：`ignoreUrls` 配置 → AntPathMatcher 匹配（`SecurityGlobalFilter.java:54-72`）
3. **URL 级 RBAC**：`system:perm_roles_rule:url:` Redis Hash → `"GET:/api/v1/users/*" → "['ADMIN','TEST']"`（`SecurityGlobalFilter.java:96-119`）
4. **用户类型**：`user.getUsertype()` — admin/manager/member 三层（`SecurityGlobalFilter.java:80-81`）
5. **X-USERINFO Header**：将 JSON 序列化用户信息透传到下游微服务（`SecurityGlobalFilter.java:155-157`）

### nop-app-erp: Nop Platform 内置认证

nop-app-erp 基于 Nop Platform 的 nop-auth 模块：

- 复用平台 `nop-auth` 认证机制（`notification-strategy.md:30`：`ROLE 复用平台 nop-auth（角色名 → NopAuthUserRole → userId）`）
- 角色职责分离设计（`domain-design-guidelines.md:207-214`）：采购员/销售员/审核人/财务员/管理员
- 危险操作审计（`domain-design-guidelines.md:306-311`）：反审核/反结账/管理员操作
- 无独立 Gateway 层（单进程部署，无需 Gateway）

### 对比分析

| 维度 | Wimoor | nop-app-erp |
|------|--------|-------------|
| Gateway | Spring Cloud Gateway 独立进程 | 无（单进程 Quarkus） |
| Token 存储 | Redis（3h 过期） | Nop Platform JWT（`nop-auth`） |
| 权限模型 | URL 级 RBAC + 用户类型三级 | 角色级 + 操作级（nop-auth 标准） |
| 白名单 | `ignoreUrls` AntPathMatcher | `nop-auth` 内置白名单配置 |
| 数据透传 | `X-USERINFO` Header 服务间 | 单进程内 IServiceContext |
| 审计 | 无显式设计 | 平台内置审计 + 危险操作业务审计（`domain-design-guidelines.md:306-311`） |

**结论**：Wimoor 的 Gateway 认证模式是标准微服务网关（Token 验证 → RBAC → Header 透传），而 nop 的单进程部署无需网关层。nop 的认证更深层（`nop-auth` 内置角色/权限/审计），Wimoor 更注重 URL 路由级安全。两者的架构差异决定了不同的认证模式。

---

## 9. nop-app-erp 可从 Wimoor 借鉴之处

### 9.1 第三方 API 集成模式

**借鉴点**：Wimoor 的 ApiBuildService + Swagger Codegen SDK + Region 映射模式。

**nop 落地建议**：
- nop 若需对接电商平台（淘宝/京东/Amazon）或物流平台（顺丰/菜鸟），可参照 Wimoor：
  - `ApiBuildService.java:102-127` 的 LWA 凭证管理 → nop 可使用 `IErp*Biz` + `@Inject` SPI 管理第三方认证
  - `ApiBuildService.java:156-178` 的 Region/Endpoint 映射 → nop 可在 master-data 建 `ErpSysExternalSystem` 表管理
  - `ApiBuildService.java:192-198` 的 API 客户端 Builder → nop 可使用 `@Inject Map<String, IApiClientBuilder>` 自动注册
- 但 nop 应**避免 Wimoor 的反模式**：`AmazonAuthority implements RateLimitConfiguration`（Wimoor 调研报告 §6.2）

### 9.2 利润计算策略模式的垂直扩展

**借鉴点**：Wimoor 的 10 站点实现 + 子计算器注入（调研报告 §2.4）。

**nop 落地建议**：
- nop 的成本核算（`period-close.md:114-145`）已支持移动加权平均/FIFO/批次，但计价方法的"子计算器注入"可借鉴
- 例如 `IErpInvCostCalculator` SPI：`MovingAverageCalculator` / `FifoCalculator` / `BatchSpecificCalculator`，按物料 `costMethod` 字段路由
- 当前 nop 的设计（`ErpFinAcctDocRegistry` + `@Inject Map`）已具备类似基础，只需扩展具体计算器

### 9.3 SaaS 计费模型

**借鉴点**：Wimoor 的 `SysTariffPackages` + `ManagerLimit`（调研报告 §4.3）。

**nop 落地建议**：
- nop 当前定位私有部署，若未来 SaaS 化：
  - master-data 域新增 `ErpSysSubscriptionPlan` / `ErpSysTenantResourceLimit` 实体
  - 沿用 `orgId` 作为租户标识（当前 `orgId` 仅是多组织维度，SaaS 化后作为租户隔离维度的天然基础）
  - 业务功能开关与套餐绑定（nop 的 `config-gated` 模式天然适合套餐功能开关）

### 9.4 跨境贸易字段补充

**借鉴点**：Wimoor `Material.java` 中的跨境特有字段：

```java
// Wimoor Material 的跨境字段
private Float vatrate;          // VAT 税率（`Material.java:98-99`）
private Float drawbackRate;     // 退税率（`Material.java:102-103`）
private Integer deliveryCycle;  // 采购周期（`Material.java:111-112`）
private Integer assemblyTime;   // 组装周期（`Material.java:115-116`）
```

**nop 落地建议**：
- nop 的 `ErpMdMaterial`（`master-data.orm.xml:171-200`）已有 `leadTimeDays` 和重量/体积，但缺少 `vatRate`/`drawbackRate` 等跨境财务字段
- 可通过 master-data 的 delta 扩展（`domain-design-guidelines.md:113`：`ext:baseClass Delta 扩展`）补充
- MaterialCustoms（报关数据）可新增为关联实体

---

## 10. Wimoor 可从 nop-app-erp 借鉴之处

### 10.1 模型驱动开发

**借鉴点**：nop 的 `orm.xml` → 代码生成全链（`domain-module-split-analysis.md:24-26`）。

**对 Wimoor 的价值**：
- Wimoor 目前使用 MyBatis-Plus，每个实体需手写 `Entity` + `Mapper` + `Service` + `Controller`
- 引入模型驱动可大幅减少 CRUD 重复代码
- Wimoor 的 `Material.java:27-206` 手写了 40+ 字段 + getter/setter + annotation，通过 ORM 模型生成可消除 70% 重复代码

### 10.2 领域模块化拆分

**借鉴点**：nop 的 18 域 DAG 依赖 + `I*Biz` 跨域接口（`domain-module-split-analysis.md:186-194`）。

**对 Wimoor 的价值**：
- Wimoor 当前模块是物理微服务边界（amazon/erp/finance/admin），非领域驱动拆分
- `wimoor-erp` 单一模块包含了 Material/Warehouse/Purchase/Inventory 等不同领域
- 借鉴 nop 的 master-data → inventory → purchase/sales → finance 四层 DAG，可优化 Wimoor 的模块内聚性
- 引入 `I*Biz` 接口解耦跨域依赖

### 10.3 三层过账引擎

**借鉴点**：nop 的 3 层过账模型 + businessType 字典 + posted 标志体系（`posting.md:11-42`）。

**对 Wimoor 的价值**：
- Wimoor 的 `FinVouchers.dataSource=4-单据同步` 仅记录"来源"，缺乏标准化业财一体机制
- 引入 `posted` 标志 + 业财回链 `VoucherBillR` + 兜底扫描，可解决 Wimoor 的"业务已审核但未过账"的不一致问题
- `businessType` 统一字典（40+ 类型）替代 Wimoor 的 `ftype` 字符串路由

### 10.4 三层库存模型

**借鉴点**：nop 的 StockMove → StockLedger → StockBalance（`docs/design/inventory/README.md:28-35`）。

**对 Wimoor 的价值**：
- Wimoor 的 `Inventory` + `InventoryRecord` 两层模型缺少成本层和追溯链
- 对跨境电商而言，FBA 库存多节点（可售/在途/入库中），nop 的 3 层模型 + 所有权维度（自有/VMI/寄售）更适合多节点库存追踪
- 批次/序列号管理可满足 Wimoor 的 FBA 货件追踪需求

### 10.5 通知子系统

**借鉴点**：nop 的 `module-notify` + 模板引擎 + 接收人解析 + 频控合并（`notification-strategy.md`）。

**对 Wimoor 的价值**：
- Wimoor 当前缺少标准通知系统
- 对于跨境电商场景，通知至关重要：FBA 库存预警、广告预算超限、订单异常等
- `notify(eventType, context)` 统一入口，业务域只需调用即可派发站内/邮件/短信

### 10.6 会计期间与多账套

**借鉴点**：nop 的 5 态期间状态机 + `acctSchemaId` 多账套并行（`period-close.md:150-158` / `finance.orm.xml:11-12`）。

**对 Wimoor 的价值**：
- Wimoor 的 `FinVouchers.periodId` 简化关联期间，缺乏状态机和约束
- 多账套（管理账/税务账/合并账）是国际电子商务的刚需——Wimoor 的跨境电商用户通常需要管理多国会计准则
- 汇兑损益期末重估（`domain-design-guidelines.md:388-406`）对跨境电商多币种场景至关重要

---

## 11. 总体评估

```
┌──────────────────────────────────────────────────────────────────┐
│                    能力覆盖矩阵                                    │
├───────────────────────┬──────────────┬──────────────┬─────────────┤
│      能力维度          │   Wimoor     │ nop-app-erp  │  差距方向    │
├───────────────────────┼──────────────┼──────────────┼─────────────┤
│ 电商 API 集成          │ ████████    │ ░░░░░░░░    │ Wimoor 强   │
│ SaaS 多租户计费        │ ██████░░    │ ██░░░░░░    │ Wimoor 强   │
│ 策略模式利润计算       │ ████████    │ ░░░░░░░░    │ Wimoor 强   │
│ 跨境字段支持           │ ████████    │ ██░░░░░░    │ Wimoor 强   │
│ 库存精细化（3层模型）  │ ██░░░░░░    │ ████████    │ nop 强      │
│ 业财一体过账引擎       │ ██░░░░░░    │ ████████    │ nop 强      │
│ 会计期间+多账套        │ ██░░░░░░    │ ████████    │ nop 强      │
│ 成本核算               │ █░░░░░░░    │ ████████    │ nop 强      │
│ 通知子系统             │ ░░░░░░░░    │ ████████    │ nop 强      │
│ 坏账准备 Allowance 法  │ ░░░░░░░░    │ ████████    │ nop 强      │
│ 批次/序列号追踪        │ ░░░░░░░░    │ ████████    │ nop 强      │
│ 模型驱动代码生成        │ ░░░░░░░░    │ ████████    │ nop 强      │
│ 领域模块化 DAG         │ ██░░░░░░    │ ████████    │ nop 强      │
│ 审计追溯/危险操作审计   │ ██░░░░░░    │ ████████    │ nop 强      │
│ 冲销双向闭环           │ ░░░░░░░░    │ ████████    │ nop 强      │
│ 外汇汇兑损益重估       │ ░░░░░░░░    │ ████████    │ nop 强      │
│ 年度结转               │ ░░░░░░░░    │ ████████    │ nop 强      │
│ 账单/流水/余额 3层模型 │ ██░░░░░░    │ ████████    │ nop 强      │
└───────────────────────┴──────────────┴──────────────┴─────────────┘
```

### 核心结论

1. **项目定位根本不同**：Wimoor 是跨境电商（Amazon 卖家）ERP，核心价值在 Amazon SP-API 集成和利润计算；nop-app-erp 是通用制造业/进销存 ERP，核心价值在业财一体和精细化库存。

2. **架构哲学对立**：Wimoor 采用微服务物理拆分，适合弹性扩展的多租户 SaaS 部署；nop-app-erp 采用多模块逻辑拆分 + 单进程聚合，模型驱动生成代码，适合 DevOps 简化的中大型企业部署。

3. **财务域差距最大**：nop-app-erp 的财务域（3 层过账引擎 + businessType 字典 + 多账套 + 期间管理 + 坏账准备 + 双向冲销）深度远超 Wimoor。nop 的财务设计对标 iDempiere/Metasfresh 等专业 ERP，而 Wimoor 的财务功能类似于"跨境电商记账辅助"。

4. **外部集成 Wimoor 领先**：Wimoor 的 SP-API 全栈集成（订单/报告/库存/广告/财务/通知）+ 利润计算引擎是 nop-app-erp 完全不覆盖的能力。如果 nop-app-erp 未来需要电商集成，Wimoor 是最直接的参考。

5. **通知系统 nop 独占**：nop 的 `module-notify` 是 Wimoor 完全没有的完整能力——模板引擎、接收人解析、频控合并、审批通知、收件箱。这是 nop 事件驱动架构的产物。

6. **双方完全互补**：Wimoor 的核心能力（电商集成、SaaS 计费、利润计算）正是 nop 缺失的；nop 的核心能力（业财一体、模型驱动、精细库存、通知系统）正是 Wimoor 缺失的。建议 nop 项目保留 Wimoor 作为外部集成设计的前置参考。

### 对 nop-app-erp 的优先行动建议

按优先级排序：

| 优先级 | 行动 | 参考 Wimoor 证据 |
|--------|------|------------------|
| P0 | 设计第三方 API 集成 SPI（工厂+认证+限流） | `ApiBuildService.java:35-507` |
| P1 | 利润/成本计算策略模式（按物料 costMethod 分发计算器） | `USProfitServiceImpl` + 子计算器注入 |
| P2 | 跨境贸易字段补充（vatRate/drawbackRate 等） | `Material.java:98-103` |
| P3 | 若 SaaS 化，参考套餐计费模型 | `SysTariffPackages` + `ManagerLimit` |

---

*本报告基于 2026-07-20 日实测源码与设计文档编写。*
