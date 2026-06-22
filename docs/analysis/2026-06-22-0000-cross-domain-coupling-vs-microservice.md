---
分析日期: 2026-06-22
类型: 架构权衡分析（Architecture Trade-off Analysis）
状态: 已完成（基于 erp-survey 横向证据 + 已定稿架构决策归纳）
覆盖问题:
  - 多模块 ERP 应该用直接 ORM 关联，还是按微服务分离？
  - ERP 强关联是否意味着必须单库？开源 ERP 都是单库吗？有没有分库做法？
  - 开源 ERP 模块之间的依赖关系与数据结构引用具体怎么做的？强引用还是弱指针？
相关文档:
  - docs/architecture/domain-module-split-analysis.md（已生效决策）
  - docs/architecture/module-boundaries.md（硬规则 + DAG）
  - docs/architecture/integration-and-transaction-patterns.md
  - docs/analysis/erp-survey/2026-06-22-0000-module-split-comparison.md
  - docs/analysis/erp-survey/2026-06-22-0000-metasfresh.md
  - docs/analysis/erp-survey/2026-06-22-0000-erpnext.md
  - docs/analysis/erp-survey/2026-06-22-0000-xingyun-erp.md
---

# 跨域耦合策略分析：直接 ORM 关联 vs 微服务分离

> **问题**：多模块 ERP 系统，域之间应该用直接的数据库关联（FK / ORM `refEntityName`），还是按微服务彻底分离？
>
> **结论先行**：nop-app-erp **两者都不选**。正确答案是中间态——**多 Maven 工程 + 同进程部署 + 跨工程走 `I*Biz` 接口（纯字符串外键列，不做 ORM 强引用）**。这是 nop 平台原生哲学，与 Metasfresh / 赤龙 / 星云等主流中型 ERP 的实践一致，也是 `docs/architecture/domain-module-split-analysis.md` 已定稿的决策。本文记录完整的权衡论证与横向证据，作为该决策的支撑材料。

## 1. 候选方案谱系

将"多模块系统的跨域协作方式"放在一个谱系上，从最紧到最松：

| # | 模式 | 数据库 | 跨域查询 | 事务边界 | 代表项目 |
|---|---|---|---|---|---|
| A | **跨模块 ORM 强关联** | 单库 + 跨域 FK 约束 / `refEntityName` | 直接 join | 共享本地事务（隐性耦合） | Odoo（`_inherit` 横切）、管伊佳 |
| **B** | **多 Maven 工程 + `I*Biz`（本工程选择）** | 单库 + 跨域**纯字符串外键列**，无 FK | BizModel 注入 `IErpMd*Biz` 只读查询 | 单进程本地事务 | Metasfresh、赤龙、星云、nop 内置模块 |
| C | **共享库 + 服务化边界** | 单库 + 通过 service 接口访问 | 跨服务 RPC | 本地事务 + 业务层编排 | — |
| D | **物理微服务** | 每服务独立库 | RPC / 数据冗余 / CQRS 投影 | 分布式事务 / Saga | （ERP 领域稀少） |

本文聚焦 **A vs B vs D** 的对比与选型理由。

## 2. 三模式横向对比

| 维度 | A. ORM 强关联 | **B. 多模块 + I\*Biz** | D. 物理微服务 |
|---|---|---|---|
| 数据库 | 单库，跨域 FK 约束 | 单库，跨域无 FK，仅字符串指针列 | 每服务独立库 |
| 跨域查询 | 直接 SQL join | BizModel `@Inject` 跨域 `I*Biz` 只读查询 | RPC / 反范式化 / 投影表 |
| 一致性 | 强一致 | 强一致（同库同进程） | 最终一致为主 |
| 事务 | 隐性跨域耦合 | 显式本地事务 | Saga / 补偿（复杂） |
| 模块独立演进 | 差（动一处牵一片） | 好（DAG 单向依赖） | 极好（独立部署） |
| 独立扩缩容 | 不可能 | 不可能 | 可行 |
| 运维复杂度 | 低 | 中 | 高（服务治理 / 调用链） |
| 适用规模 | <30 表 | 80–120 表、多业务域 | 多团队、地理分布式 |

## 3. 为什么不选 A：跨模块 ORM 强关联

### 3.1 平台硬规则

源自 `docs/architecture/module-boundaries.md` 与 `domain-module-split-analysis.md:138-149`：

> **硬规则**：跨工程实体**不做** ORM 层 `refEntityName` 强引用。平台所有内置模块（nop-auth / nop-sys / nop-wf）的源 orm.xml 中 `refEntityName` **全部**指向本模块包内实体，**零跨包引用**。

### 3.2 具体替代做法

- 引用方工程用**纯外键列**（如 `erp_pur_order.material_id VARCHAR`），不带 `<to-one>` 关系声明。
- 跨域只读查询和动作编排通过 `@Inject IErpMdMaterialBiz`（被引用方在 `*-dao` 暴露的 `I*Biz` 接口）。
- 凭证反查源单用三元组 `(source_bill_type, source_head_code, source_line_code)`，纯字符串。

### 3.3 反面证据：Odoo 的升级地狱

Odoo 用 `_inherit` / `_inherits` 模型继承横切注入定制，**每个大版本升级时定制模块必须重新移植，视图继承链和 XMLID 经常断裂**，社区充斥升级失败案例（见 `docs/architecture/competitive-comparison.md:53`）。这是"ORM 强关联"思路在更大尺度上的症状——模块边界被穿透后，演进性崩塌。

### 3.4 不选 A 的工程理由

1. **破坏 DAG**：跨域 `refEntityName` 一旦双向，即形成循环依赖，无法独立 codegen。
2. **升级耦合**：被引用方实体改字段，引用方 ORM 元数据同步失效。
3. **测试不可孤立**：单域单测必须拖起他域 ORM 上下文。
4. **与平台哲学冲突**：nop 内置 3 个模块（auth/sys/wf）零反例，本工程应遵循同一约束。

## 4. 为什么不选 D：物理微服务

### 4.1 ERP 的领域特征与微服务代价

| ERP 领域特征 | 与微服务的冲突 |
|---|---|
| 业财一体要求强一致 | 凭证与库存**绝不能最终一致**，Saga/补偿成本极高 |
| 跨域查询高频（采购单带物料名、凭证反查源单） | RPC 慢且要熔断；冗余同步成本高 |
| 单据链路长（采购→入库→应付→凭证） | 一次业务跨多服务，分布式事务几乎不可避免 |
| 团队规模通常不大 | 微服务运维（治理 / 调用链 / 配置中心）超出收益 |

### 4.2 业财一体的事务边界：Metasfresh 范式

Metasfresh 的业财一体已是**单体内部的事务边界优化**，而非微服务（见 `erp-survey/2026-06-22-0000-metasfresh.md:99-113`）：

- 业务表带 `Posted CHAR(1)` 标志（`X_C_Order:98`、`X_C_Invoice:82`）。
- 主事务只落业务单据（`Posted=N`）。
- 过账通过 `sendAfterCommit` → `IEventBus` 在**事务提交后**异步执行（`DocumentPostingBusService.java:104`）。
- 失败可重试，且不阻塞主单据事务。

这已经是把"过账"从事务中剥离的极致优化——但它**仍在单进程内**。一旦跨物理服务，连这种优化都要额外引入分布式消息可靠性保障。

### 4.3 调研样本的部署形态分布

来自 `erp-survey/2026-06-22-0000-module-split-comparison.md` "部署形态"表（13 项目）：

| 部署形态 | 项目 |
|---|---|
| 单体 | Odoo、ERPNext、Metasfresh、管伊佳、若依、Dolibarr |
| OSGi 可分布式 | iDempiere（仍以单体为主） |
| **单体 + 微服务双形态** | **星云**（唯一真微服务可选，且业务代码零改动） |
| 纯微服务 | **0 个** |

> 关键观察：13 个调研项目里，**没有一个**把 ERP 默认做成物理微服务。最接近的星云也是"同代码双入口"——业务模块只写一次，单体入口加 `rabbitmq-starter` 内嵌 MQ，微服务入口加 `cloud-starter` 走云端 broker。

### 4.4 不选 D 的判断框架

遇到"是否微服务"的纠结，依次问：

1. **是否需要独立扩缩容**？（如采购流量是销售 10 倍）—— 否 → 不要微服务。
2. **是否多团队地理分布式开发**？—— 否 → 不要微服务。
3. **是否需要独立数据所有权与发布节奏**？—— 需要但同库可满足 → 多 Maven 工程 + `I*Biz` 足够。

ERP 三个问题基本都是"否"，微服务是错配。

## 5. 为什么选 B：多 Maven 工程 + I*Biz

### 5.1 三重对齐

| 对齐维度 | 证据 |
|---|---|
| 平台原生 | nop 内置 nop-auth / nop-sys / nop-wf 各自独立工程，跨模块走 `I*Biz`，零跨包 ORM 引用 |
| 主流中型 ERP 实践 | Metasfresh `IAcctDocProvider` SPI、赤龙 6 模块、星云 9 模块 |
| 本工程规模 | 预估 80–120 表、10 个业务域，单文件不可维护，需按域拆分 |

### 5.2 业财打通：SPI 注册模式（Metasfresh 范式落地）

财务域定义接口 + 注册中心，各业务域提供 Provider Bean：

```
app-erp-finance：定义 IErpFinAcctDocProvider + ErpFinAcctDocRegistry
                  （注入 List<IErpFinAcctDocProvider>）
app-erp-purchase / sales / inventory：各自实现 IErpFinAcctDocProvider（@Component）
```

新增单据类型 = 新增一个 Provider Bean，**零改动财务核心**。这是 Metasfresh `AcctDocRegistry.java:25-33` + `StandardAcctDocProvider.java:56-79` 的类型安全注册模式（见 `erp-survey/2026-06-22-0000-metasfresh.md:77-84`）。

### 5.3 凭证反查：纯字符串指针（领域共识）

| 项目 | 凭证反查源单的指针字段 | 证据 |
|---|---|---|
| Metasfresh | `Fact_Acct.AD_Table_ID + Record_ID` | `metasfresh.md:54` |
| ERPNext | `gl_entry.voucher_type + voucher_no + voucher_detail_no` | `erpnext.md:53,104` |
| **nop-app-erp（本设计）** | `(source_bill_type, source_head_code, source_line_code)` 三元组 | `domain-module-split-analysis.md:149` |

> 这是 ERP 领域的**共识模式**：业务单与凭证之间通过"源单指针"反查，而非外键强约束。三家独立演进的系统殊途同归。

### 5.4 部署演进路径：保留可选项

默认 `app-erp-app` 单进程 Quarkus 启动（聚合所有领域工程 `-service`/`-web`）。若将来真要微服务化，仿星云——**业务模块不动，只换装配入口和 starter**。不为未发生的需求提前付出分布式事务代价。

## 6. 开源 ERP 耦合策略归类

来自 `erp-survey/2026-06-22-0000-module-split-comparison.md` 的横向证据，按耦合方式归三类：

### 6.1 单体紧耦合（多数，反面教材）

- **Odoo**：625 addon，模型继承 `_inherit` 横切组合；灵活但升级地狱。
- **ERPNext**：~15 域单体，业务单 `on_submit` 钩子驱动 GL 过账，GL 行用三元组反查源单（**反查用字符串指针，值得借鉴**）。
- **管伊佳 / 若依 / Dolibarr**：几乎无模块化，业务全塞一个应用。

### 6.2 业务域 Maven 多模块 + 注册机制（最贴近本工程，正面教材）

- **Metasfresh**：~140 模块（过度），但 `IAcctDocProvider` SPI 跨模块自动聚合是核心范式。`competitive-comparison.md:84` 指出"新增单据类型 = 新建模块 + 一个 Bean，零改动核心"。
- **赤龙 ERP**：6 个业务域模块（common / masterData / inv / finance / order），**合理粒度**。
- **星云 ERP**：9 模块 + 单体/Cloud 双入口，**唯一真正"演进友好"的样本**。

### 6.3 技术分层而非业务域（不推荐）

- **iDempiere**：66 个 OSGi bundle 按**技术层**拆（UI/DB/报表/业务），但业务模型集中在单 base plugin——与 nop"按业务域拆"哲学相反。
- **WMES**：经典 4 层（Code/Data/Domain/Service/Web），同样是技术分层。

### 6.4 极细功能粒度（不推荐）

- **Odoo** 625 addon：单团队维护成本极高。
- **Metasfresh** 140 模块：同病。

### 6.5 关键规律

> **业财打通这块，几乎所有成熟系统都不用"外键强关联"，而用"业务表 + Posted 标志 + 凭证用源单指针反查"。** Metasfresh `Fact_Acct.AD_Table_ID+Record_ID`、ERPNext `gl_entry.voucher_type+voucher_no`、本设计 `ErpFinVoucherBillR` 三元组——本质同构。

## 7. 决策矩阵

按加权评分（1=差，5=好），权重反映 ERP 中型应用的优先级：

| 维度 | 权重 | A. ORM 强关联 | B. 多模块 + I\*Biz | D. 微服务 |
|---|---|---|---|---|
| 业财强一致 | 高 (×3) | 5 | 5 | 2 |
| 模块独立演进 | 高 (×3) | 2 | 4 | 5 |
| 跨域查询效率 | 中 (×2) | 5 | 4 | 2 |
| 运维复杂度（越低越好） | 中 (×2) | 5 | 4 | 1 |
| 与 nop 平台一致性 | 高 (×3) | 1 | 5 | 3 |
| 团队规模适配 | 中 (×2) | 4 | 5 | 2 |
| **加权合计** | | **41** | **71** | **42** |

> B 显著领先。值得注意的是 D（微服务）总分仅与 A 相当——它的"独立演进"优势被"一致性 / 运维 / 跨域查询"三项劣势抵消殆尽。

## 8. 数据库架构实测：开源 ERP 都是单库吗？

> 本节回应一个常见推论："ERP 强关联 → 必须单库保证事务"。**校正**：单库是结果，不是原因。即便像 nop 这样跨域不做 ORM 强引用，仍选单库——业财强一致的领域本质才是驱动因素。

### 8.1 13 个调研项目的数据库架构

来自 `erp-survey/` 各项目报告的技术栈与多租户机制实测：

| 项目 | 数据库 | 业务库形态 | 多租户/多公司机制 | 按模块分库? |
|---|---|---|---|---|
| Odoo | PostgreSQL | ✅ 单库 | `res.company` + `company_id` 行级 | ❌ |
| Odoo (multi-DB) | PostgreSQL | ⚠️ 每租户一库 | `--dbfilter` 按域名路由 | ❌（仅 SaaS 租户隔离，租户内仍单库） |
| ERPNext | MariaDB | ✅ 单库 | `Company` doctype 行级 | ❌ |
| Metasfresh | PostgreSQL | ✅ 单库 | `AD_Client_ID`/`AD_Org_ID` 行级 | ❌ |
| iDempiere | PG / Oracle | ✅ 单库 | `AD_Client_ID`/`AD_Org_ID` 行级（见 `idempiere.md:86-89`） | ❌ |
| 星云 ERP | MySQL | ✅ 单库 | 单公司；Cloud 形态同库多实例 | ❌（分进程不分库） |
| 赤龙 ERP | MySQL | ✅ 单库 | 单公司 | ❌ |
| 若依 ERP ×2 | MySQL | ✅ 单库 | 单公司 | ❌ |
| 管伊佳 | MySQL | ✅ 单库 | 单公司（业务全塞 `DepotHead`） | ❌ |
| Tryton | PG / SQLite | ✅ 单库 | 同库行级 company | ❌ |
| Dolibarr | MySQL | ✅ 单库 | 单公司 | ❌ |
| Carbon | Supabase (PG) | ✅ 单库 | — | ❌ |
| assetsmgr | MySQL | ✅ 单库 | 单公司 | ❌ |

**核心实测结论：13 个项目里，没有一个把 ERP 业务内部按模块拆成多库。** 即便是 iDempiere 这种 66 个 OSGi bundle 的极致模块化系统，所有业务表也都共享同一个物理数据库。

### 8.2 三种"看起来像分库"的情况（实际都不是模块分库）

#### (a) Odoo multi-DB 模式（SaaS 场景）
Odoo `--dbfilter` 按请求域名路由到不同 PG 库，Odoo.sh / SaaS 用它给每个客户独立库。但这**不是 ERP 内部分库**——每个租户拿到的是一个**完整的 ERP 实例**，租户之间是独立部署的业务关系。租户内部业务模块仍在同一个库。

#### (b) iDempiere / Metasfresh 的 `AD_Client_ID` / `AD_Org_ID`
看起来像分库，实际是**同库行级隔离**：所有租户、所有公司的数据混在同一张 `C_Order` 表里，靠 `AD_Client_ID` 列区分。租户隔离由应用层 + 行级安全策略实现，不是物理分库。iDempiere 的凭证生成甚至显式校验 `p_po.getAD_Client_ID() == m_as.getAD_Client_ID()`（`Doc.java:498-504`）保证凭证与单据同租户——这只有在单库内才有意义。

#### (c) 星云"单体 / Cloud 双形态"
Cloud 形态部署多个服务实例，但每个实例指向**同一个 MySQL 库**。它分的是**进程**，不是**数据库**。业务代码零改动，仅替换 starter（`rabbitmq-starter` ↔ `cloud-starter`）。

### 8.3 为什么 ERP 几乎都不分库

| # | 原因 | 证据 |
|---|---|---|
| 1 | **业财一体是杀手约束**：一张采购单过账，要在同一事务里写 `采购单 → 入库单 → 应付凭证 → 库存余额`，跨 4 个业务域。分库必须引入 Saga/TCC/本地消息表，而**凭证与库存绝不允许最终一致** | Metasfresh `DocumentPostingBusService.java:104` 已把过账异步化到 `post-commit` EventBus，但仍**在单进程单库内**（`metasfresh.md:71-74`） |
| 2 | **跨域查询高频**：采购单列表带物料名、凭证反查源单、销售查库存可用量——这些 join 跨库就只能 RPC（慢）或冗余（同步成本） | ERPNext GL 行 `(voucher_type, voucher_no, voucher_detail_no)` 三元组（`erpnext.md:53`） |
| 3 | **规模没到**：中型 ERP 80–500 表、单表千万级，PG/MySQL 单库 + 读写分离 + 分表足以承载 | 本工程预估 80–120 表（`domain-module-split-analysis.md:35`） |
| 4 | **运维成本**：分库需解决 ID 分配、跨库查询、数据同步、备份恢复一致性、报表汇总——内部团队代价远超收益 | Metasfresh 140 模块仍单库（`metasfresh.md:22`） |

### 8.4 真有分库的 ERP 吗？

**SaaS 多租户 ERP** 是分库的主要场景，但分的是**租户**，不是**模块**：

- **Odoo SaaS / Odoo.sh**：每客户一个独立 PG 库
- **SAP Business ByDesign / Oracle NetSuite**：大型 SaaS ERP，租户级隔离（内部架构未公开，推测 schema 级或库级）

这种"分库"解决**租户隔离**问题（数据主权、性能隔离、合规），**不解决**"模块强关联"问题。同一租户内部，业务模块仍在同一个库。

**真正按业务模块分库的开源 ERP 实证为 0**。按模块分库通常只见于大厂内部自研、走事件驱动 + 最终一致的系统，代价极高，不在开源 ERP 主流范式内。

### 8.5 关键区分：模块解耦 ≠ 数据库分离

本工程选 B（多 Maven 工程 + `I*Biz`）容易引发一个误解："既然模块解耦了，为什么不顺便分库？"

> **"代码模块解耦" ≠ "数据库分离"。**

nop 的 DAG + `I*Biz` 解的是**编译期/工程期**的耦合：独立 codegen、独立演进、独立替换某域、避免循环依赖。但它**故意保留**了运行期单库，以换取：

- 跨域事务仍是廉价的**本地事务**（同一 `EntityManager` / 同一 DB 连接）
- 跨域查询仍是廉价的**单 SQL**（即便走 `I*Biz`，底层也是同库 join）
- 业财强一致无需分布式协议

这是 ERP 领域被 13 个调研项目反复验证的最优权衡点。把"模块解耦"误读为"应该分库"，会同时丢掉模块演进性和事务廉价性两头的好处。

## 9. 跨模块数据引用机制实测：各开源 ERP 具体怎么做？

> 本节回应："现有开源 ERP 模块之间的依赖关系、数据结构之间的引用关系到底是怎么做的？" 逐项目列出**模块依赖声明方式**与**跨模块数据引用字段**两层证据。

### 9.0 谱系总览：两种引用范式

13 个项目把"跨模块引用"做成两种截然不同的范式，差异在**强引用 vs 弱指针**：

| 范式 | 机制 | 代表项目 | 特点 |
|---|---|---|---|
| **强引用范式** | ORM 字段直接声明 `Many2one`/`One2many`/`Many2many` + DB 物理外键，模块间显式 `depends` | Odoo、Tryton、ERPNext | 查询方便（直接 join/导航），但模块耦合强、删除需级联 |
| **弱指针范式** | 表里只存"源单类型 + 源单号"字符串/ID，无 FK，靠应用层反查 | iDempiere、Metasfresh、赤龙 | 模块解耦、可跨"表族"反查，但查询要应用层组装 |
| **混合范式** | 业务内部强引用、业财之间弱指针 | Metasfresh（业务强 + 凭证弱）、赤龙（业务回链 + 凭证弱） | 兼顾两者——业务域内高内聚用强引用，业财边界用弱指针 |

> **关键发现**：业财打通的边界上，**几乎所有成熟系统都用弱指针**（`AD_Table_ID+Record_ID` / `voucher_type+voucher_no` / `billType+billHeadCode`），不在业务表写外键到凭证表。这与 nop 平台"跨模块不做 ORM 强引用"的硬规则**殊途同归**。

### 9.1 Odoo：模型继承 + 字段强引用（强引用范式极版）

**模块依赖声明**：每个 addon 的 `__manifest__.py` 写 `depends` 列表（`odoo.md:24`）。
```
sale_stock  depends: ['sale', 'stock', 'sales_team']
mrp_account depends: ['mrp', 'account']
```

**跨模块数据引用**：全用 ORM 关系字段（`odoo.md:62-75`）：

| 引用场景 | 字段 | 类型 | 证据 |
|---|---|---|---|
| 销售→库存 | `sale.order.picking_ids` | `One2many('stock.picking','sale_id')` | `sale_stock/models/sale_order.py:31` |
| 采购→库存 | `purchase.order` → `_create_picking()` 生成 `stock.picking` | 程序生成 | `purchase_stock/models/purchase_order.py:375` |
| 制造→库存 | `mrp.production.move_raw_ids` / `move_finished_ids` | `One2many('stock.move')` | `mrp_production.py:203,208` |
| 移动链追溯 | `stock.move.move_orig_ids` / `move_dest_ids` | `Many2many` 自关联 | `stock_move.py:98-105` |
| 退货反查 | `stock.move.origin_returned_move_id` | `Many2one` 自关联 | `stock_move.py:155-158` |
| 销售→发票 | `sale.order.invoice_ids` | `Many2many('account.move')` | `sale_order.py:239` |
| 业务→凭证 | `stock_move._create_account_move()` 程序生成 `account.move` | 程序生成，无反查 FK | `stock_account/models/stock_move.py:184` |

**业财边界的关键技巧**：业务→凭证虽然程序生成，但**凭证侧不写回到业务表的 FK**，而是凭证行用 `account.move.line` 的 `move_id` 归属凭证头，业务侧通过 `invoice_ids` M2M 反查。Odoo 的存货过账甚至不存源单指针——靠 `stock.move` 自身的 `account_move_line_ids` O2M 反向关联（`stock_account`）。这是 Odoo 把"业务强引用"推到极致的表现。

### 9.2 ERPNext：Link 字段 + on_submit 钩子（强引用范式）

**模块依赖声明**：Frappe 框架的 `hooks.py` + doctype 间的 `Link` 字段类型。

**跨模块数据引用**：doctype 字段用 `Link` 类型表达外键（`erpnext.md:50-55`）：

| 引用场景 | 字段 | 证据 |
|---|---|---|
| 凭证→科目 | `gl_entry.account` | `Link→Account`（`gl_entry.json:24`） |
| 凭证→业务单（反查） | `voucher_type` + `voucher_no` + `voucher_detail_no` | **字符串三元组**（`:24-31`） |
| 核销关联 | `against_voucher_type` + `against_voucher` | 字符串二元组（`:54`） |
| BOM→物料 | `bom.item` | `Link→Item`（`bom.json`） |
| 核算维度 | `cost_center` / `project` | `Link` 字段（`:55`） |

**业财边界的关键技巧**：业务单据 `on_submit` 钩子触发 `make_gl_entries()`（`purchase_receipt.py:379`、`delivery_note.py:492`、`sales_invoice.py:458`），生成的 GL 行用**字符串三元组 `(voucher_type, voucher_no, voucher_detail_no)` 反查源单**——不在 GL 表写 FK 到业务表。这是 ERPNext 的"业务内 Link 强引用、业财边界弱指针"混合范式。

### 9.3 iDempiere / Metasfresh：AD_Table_ID 通用反查（弱指针范式）

**模块依赖声明**：OSGi `MANIFEST.MF` 的 `Require-Bundle`（iDempiere）/ Maven 父 POM（Metasfresh）。

**跨模块数据引用**：业务表用 `_ID` 后缀的 FK 列；**凭证表用通用反查指针**（`idempiere.md:59`、`metasfresh.md:54`）：

| 引用场景 | 字段 | 证据 |
|---|---|---|
| 收发货→订单 | `M_InOut.C_Order_ID` | FK 列（`X_M_InOut.java`） |
| 凭证→源单（**通用反查**） | `Fact_Acct.AD_Table_ID` + `Record_ID` | **(表 ID, 记录 ID) 二元组**（`X_Fact_Acct.java:46-58`） |
| 凭证→科目表 | `Fact_Acct.C_AcctSchema_ID` | FK 列 |
| 凭证→科目 | `Fact_Acct.Account_ID` | FK 列 |
| 凭证→租户 | `Fact_Acct.AD_Client_ID` / `AD_Org_ID` | FK 列（行级隔离） |

**业财边界的关键技巧**：
1. **`AD_Table_ID` 是 ADempiere 的"通用表注册表"**：每张业务表在 `AD_Table` 注册一个 ID，凭证表通过 `(AD_Table_ID, Record_ID)` 反查任意业务表的任意行——这是**比字符串三元组更松的指针**（连源单类型都用 ID 而非字符串）。
2. **业务表带 `Posted` 标志**（`X_C_Order:98`、`X_C_Invoice:82`），凭证引擎扫 `Posted='N'` 单据过账，**业务表不写凭证 FK**。
3. **Metasfresh 的演进**：把 iDempiere 的反射 Doc 工厂（`Class.forName("Doc_"+table)`）升级为类型安全 `ImmutableMap<TableName, Factory>`（`StandardAcctDocProvider.java:56-79`），但**指针范式不变**——仍是 `AD_Table_ID+Record_ID`。

> **iDempiere/Metasfresh 是"弱指针范式"的源头**。它们把"凭证↔业务"做成全系统通用的反查机制，凭证引擎不知道具体业务表，只认 `AD_Table_ID`。这实现了凭证引擎与业务模块的彻底解耦——新增业务表只需在 `AD_Table` 注册 + 提供 `IAcctDocProvider` Bean，零改动凭证核心。

### 9.4 赤龙 ERP：billType + billHeadCode 字符串回链（中式弱指针）

**模块依赖声明**：Maven 多模块 `erp-parent`，业务域模块 `erp-common`/`erp-masterData`/`erp-inv`/`erp-order`/`erp-finance`（`redragon-erp.md:22-32`）。

**跨模块数据引用**：业务回链 + 凭证回链两层（`redragon-erp.md:40-60`）：

| 引用场景 | 字段 | 证据 |
|---|---|---|
| 发票→源单 | `ApInvoiceHead.invoiceSourceType` + `invoiceSourceHeadCode` | 字符串二元组（PO/INPUT） |
| 发票行→源单行 | `ApInvoiceLine.invoiceSourceLineCode` | 字符串（`:44`） |
| 付款→发票（核销） | `ApPayLine.payHeadCode` + `invoiceHeadCode` + `invoicePayAmount` | **多对多核销子表**（`:48`） |
| **凭证→业务（业财回链）** | `FinVoucherBillR.voucherHeadCode` + `billType` + `billHeadCode` | **字符串三元组**（`:60`） |
| 业务→库存 | `InvStock.billHeadCode` + `billLineCode` + `billType` | 字符串三元组（`:68`） |

**业财边界的关键技巧**：
1. **`FinVoucherBillR` 业财回链表**是赤龙的核心创新——凭证与业务单据的 **N:1 反向索引**，凭证生成时写入，反审批时按 `billHeadCode` 反查并删凭证（`redragon-erp.md:98`）。
2. **`billType` 用字符串枚举**（`PAY`/`RECEIPT`/`INPUT`/`OUTPUT`/`AP_INVOICE`），不引用业务表的物理结构——凭证引擎对业务表完全无感。
3. **审批触发 `autoCreateVoucher(billHeadCode, Double[], businessType)`**（`FinVoucherModelHeadServiceImpl.java:143`），业务 Service 在 `APPROVE` 分支调用，模板占位符 `AMOUNT` 按下标填充。

> **赤龙的 `FinVoucherBillR` 与 nop 设计的 `ErpFinVoucherBillR` 几乎同名同构**——这不是巧合，是中式复式记账业财打通的标准范式。`redragon-erp.md` 是 nop 财务模块的首要参考来源。

### 9.5 管伊佳 ERP：逗号字符串反范式（反面教材）

**模块依赖声明**：单 SpringBoot 应用，无模块拆分，按包分层（`jsh-erp.md:24-30`）。

**跨模块数据引用**（多为反范式）：

| 引用场景 | 字段 | 证据 |
|---|---|---|
| 单据→账户（**反范式**） | `DepotHead.accountIdList` + `accountMoneyList` | **逗号分隔字符串**（`jsh-erp.md:44`） |
| 账户流水→业务单 | `AccountItem.billId` | FK 列 |
| 序列号→出入库单（双向回链） | `SerialNumber.inBillNo` / `outBillNo` | 字符串（`:86`） |
| 单据明细→源单 | `DepotItem.linkId` | ID 列（`:54`） |

**业财边界**：管伊佳**无借贷凭证**，靠账户流水代数和 = 余额（`jsh-erp.md:96-100`）。`accountIdList`/`accountMoneyList` 逗号字符串是**反范式设计**，查询需解析，`jsh-erp.md:119` 明确标注为反例。nop 不应照搬，应改为 `allocation(head_id, account_id, amount)` 子表。

### 9.6 星云 ERP：BPM flowInstanceId + MQ 事件（混合范式）

**模块依赖声明**：Maven 多模块 `xingyun-core`/`basedata`/`sc`/`settle`/`chart`/`comp` + 单体/Cloud 双入口（`xingyun-erp.md:22-35`）。

**跨模块数据引用**：

| 引用场景 | 字段/机制 | 证据 |
|---|---|---|
| 业务单→工作流 | `PurchaseOrder.flowInstanceId` | FK 列 + `BpmBizListener` 监听器回调（`:137`，`xingyun-erp.md:93`） |
| 业务→报表（异步） | MQ 事件 `OrderDataToChartListener` | RabbitMQ 解耦（`xingyun-erp.md:95`） |
| 单据四级拆分 | `SaleOutSheetDetailBundle` / `...Lot` | 父子表 FK |

**业财边界**：星云的报表/库存预警走 MQ 解耦，不阻塞主单据事务。BPM 只管审批不管业务流转（`xingyun-erp.md:94`）。这是"业务内强引用、跨域关注点用 MQ 事件解耦"的混合范式。

### 9.7 Tryton：PoolMeta 受控扩展（框架级解耦）

**模块依赖声明**：业务模块以 PyPI egg 独立发布，`tryton.cfg` 声明 `depends`（`tryton.md:24-28`）。

**跨模块数据引用机制**（框架级，非字段级）：
- **Pool 全局注册表**：所有 model 注册到全局 `Pool`，跨模块通过 `Pool().get('account.move')` 按名获取（`tryton.md:29-39`）。
- **PoolMeta 元类扩展**：子模块用 `__setup__`/`__register__` 钩子扩展父模块 model，类似 Odoo `_inherit` 但更受控（`:39`）。
- 字段引用：业务 model 继承 `ModelSQL + ModelView + Workflow`，字段在类属性声明，跨模块用 `Many2one`/`One2many`（与 Odoo 同）。

**业财边界**：account 业务模块用 `Workflow.transition` 在状态转换时触发 GL 分录（`tryton.md:87`）。

### 9.8 OFBiz：视图实体跨表组合（数据层 join）

**模块依赖声明**：OFBiz 插件 `ofbiz-component.xml` 声明依赖。

**跨模块数据引用**（`ofbiz-plugins.md:25-32`）：OFBiz 用**视图实体（view-entity）**在数据层跨表组合，不定义新实体：

| 视图实体 | 组合 | 证据 |
|---|---|---|
| `FixedAssetMaintWorkEffortAndPartyAssignment` | 维护记录 + 工作计划 + 人员分配 | `:41` |
| `ItemIssuanceInventoryItemAndProduct` | 备件领用 + 库存项 + 物料 | `:93` |
| `ProjectAndPhaseAndTask` | 项目 → 阶段 → 任务三级 | `:184` |

**关键技巧**：OFBiz 插件**不定义新实体**，复用框架核心的 `FixedAsset`/`WorkEffort`/`InventoryItem`，通过 `entitymodel.xml` 声明视图实体做跨表 join。这是"插件零实体定义 + 视图层组合"的解耦范式。

### 9.9 横向对比矩阵

| 项目 | 模块依赖声明 | 业务内引用 | 业财边界引用 | 凭证反查指针 | 范式 |
|---|---|---|---|---|---|
| Odoo | `__manifest__.py depends` | M2O/O2M/M2M 强引用 | 程序生成 + M2M 反查 | 无通用指针 | 强引用极版 |
| ERPNext | Frappe hooks | `Link` 字段 | `on_submit` 钩子生成 | `(voucher_type, voucher_no, voucher_detail_no)` | 混合 |
| iDempiere | OSGi `Require-Bundle` | `_ID` FK 列 | `Posted` 标志 + 异步扫 | `(AD_Table_ID, Record_ID)` | 弱指针 |
| Metasfresh | Maven 父 POM | `_ID` FK 列 | `Posted` + EventBus | `(AD_Table_ID, Record_ID)` | 弱指针 |
| 赤龙 | Maven `erp-parent` | 字符串回链 | `FinVoucherBillR` 三元组 | `(billType, billHeadCode)` | 中式弱指针 |
| 管伊佳 | 无（单应用） | 逗号字符串（反范式） | 无凭证 | — | 反面教材 |
| 星云 | Maven + Cloud starter | FK 列 | BPM/MQ 事件 | — | 混合 |
| Tryton | PyPI egg `depends` | `Pool().get()` 按名 | Workflow.transition | — | 框架级解耦 |
| OFBiz | `ofbiz-component.xml` | 视图实体 join | — | — | 数据层组合 |

### 9.10 归纳：业财边界的共识模式

剥开各项目的差异，业财边界（业务单据 ↔ 会计凭证）的引用方式呈现高度趋同：

| 项目 | 凭证反查源单的指针 | 类型 |
|---|---|---|
| Odoo | `account.move.line` 反查 `stock.move`（O2M `account_move_line_ids`） | ORM 关系 |
| ERPNext | `gl_entry.voucher_type + voucher_no + voucher_detail_no` | 字符串三元组 |
| iDempiere / Metasfresh | `Fact_Acct.AD_Table_ID + Record_ID` | (表 ID, 记录 ID) 二元组 |
| 赤龙 | `FinVoucherBillR.billType + billHeadCode` | 字符串二元组 |
| **nop-app-erp（本设计）** | `(source_bill_type, source_head_code, source_line_code)` | 字符串三元组 |

> **五家独立演进的系统殊途同归**：凭证侧不写 FK 到业务表，业务表不写 FK 到凭证表，靠"源单指针"在应用层反查。这正是 nop 平台"跨模块不做 ORM 强引用"硬规则的领域依据，也是本工程选择弱指针范式的实证支撑。

**为什么业财边界特别需要弱指针**（区别于业务内部）：
1. **凭证引擎要通用**：新增业务单据类型时，凭证引擎不应知道新表的物理结构。`AD_Table_ID` / `billType` 把"源单类型"抽象成 ID/字符串，凭证引擎只认抽象。
2. **业务表生命周期独立**：业务单据可能被取消/冲销/重做，凭证需跟随但不应被 FK 锁死。弱指针允许"反审批时按指针反查并删凭证"（赤龙 `FinVoucherBillR` 范式）。
3. **多套科目表并行**：同一业务单要在多套 `AcctSchema` 下各出一组凭证（iDempiere/Metasfresh），FK 无法表达这种 1:N 关系，指针 + `acctSchemaId` 维度才行。

## 10. 对本工程的落地确认

本分析重申并支撑已生效决策，不引入新决策：

1. **维持 10 领域 Maven 工程 + 单库 + 跨域 `I*Biz` 纯字符串外键列**（`domain-module-split-analysis.md`）。
2. **业财打通用 SPI 注册**：`IErpFinAcctDocProvider` + 注册中心，业务域提供 Provider Bean（Metasfresh 范式，`module-boundaries.md:57-59`）。
3. **凭证反查源单用三元组字符串指针**，无 FK 约束（`ErpFinVoucherBillR.relatedBillType/Code` 等）。
4. **部署默认单进程**，微服务化保留为未来可选项（仿星云双入口）。
5. **跨域引用禁用** `refEntityName` 跨包指向——遵循 nop 平台零反例约束。

## 11. 何时需要重新评估

出现以下任一信号时，应重新审视是否向 D（微服务）演进：

- 某业务域出现**数量级的独立扩缩容需求**（如电商大促期间销售域流量是其它域 10×+）。
- **多团队地理分布式开发**，且模块所有权边界固化。
- 出现**独立的数据主权/合规要求**（如某域数据必须物理隔离）。
- 单进程启动时间或内存占用**超过可接受阈值**且无法通过瘦身解决。

当前阶段以上信号均不存在，故维持 B。

## 12. 引用证据索引

### 项目内文档
- `docs/architecture/domain-module-split-analysis.md`（已定稿决策）
- `docs/architecture/module-boundaries.md`（跨工程实体关系硬规则 + DAG）
- `docs/architecture/integration-and-transaction-patterns.md`（本地优先 + 幂等 + 外部结果所有权）
- `docs/architecture/competitive-comparison.md`（杠杆 G：跨域 DAG + I\*Biz 解耦）

### erp-survey 证据
- `2026-06-22-0000-module-split-comparison.md`（13 项目模块化谱系 + 部署形态表）
- `2026-06-22-0000-metasfresh.md`（业财一体 SPI 注册 + EventBus 异步过账）
- `2026-06-22-0000-metasfresh.md:99-113`（三层源码分层 + 主事务边界）
- `2026-06-22-0000-erpnext.md`（GL 行三元组反查源单 + on_submit 钩子过账）
- `2026-06-22-0000-xingyun-erp.md`（同代码双入口：单体 + Cloud）
- `2026-06-22-0000-odoo.md`（`_inherit` 横切与升级地狱；multi-DB 模式）
- `2026-06-22-0000-idempiere.md`（OSGi 技术层拆分反例；`AD_Client_ID`/`AD_Org_ID` 同库行级隔离 `:86-89`；`Doc.java:498-504` 租户一致性校验；`Fact_Acct.AD_Table_ID+Record_ID` 弱指针 `:59`）
- `2026-06-22-0000-metasfresh.md:71-74`（异步过账仍在单进程单库；`StandardAcctDocProvider.java:56-79` 类型安全 Map 注册）
- `2026-06-22-0000-odoo.md`（`_inherit` 横切与升级地狱；multi-DB 模式；ORM 关系字段强引用 `:62-75`）
- `2026-06-22-0000-erpnext.md`（GL 行 `(voucher_type,voucher_no,voucher_detail_no)` 三元组 `:53,104`；on_submit 钩子过账 `:46-48`）
- `2026-06-22-0000-redragon-erp.md`（`FinVoucherBillR` 业财回链 `:60,98`；`billType+billHeadCode` 中式弱指针；`autoCreateVoucher` 模板 `:143`）
- `2026-06-22-0000-jsh-erp.md`（`accountIdList`/`accountMoneyList` 逗号字符串反范式 `:44,119`；`linkId`/`SerialNumber.inBillNo` 字符串回链）
- `2026-06-22-0000-xingyun-erp.md`（同代码双入口；BPM `flowInstanceId` + MQ 事件解耦 `:93-95`）
- `2026-06-22-0000-tryton.md`（PyPI egg 独立发布；Pool/PoolMeta 框架级解耦 `:29-39`）
- `2026-06-22-0000-ofbiz-plugins.md`（视图实体跨表组合，零新实体定义 `:25-32`）
- `2026-06-22-0000-survey-index.md:122-133`（13 项目技术栈/数据库横向表）
