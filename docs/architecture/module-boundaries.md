# 模块边界

## 目的

定义 `nop-app-erp` 的主要代码所有权边界。

## 领域工程依赖方向（DAG）

nop-app-erp 按 18 个业务域拆成独立 Maven 工程，依赖方向严格单向（DAG）。详细决策见 `domain-module-split-analysis.md`。

> **物理目录映射**：`module-<domain>/` 是物理顶层目录，对应逻辑工程名 `app-erp-<domain>`；聚合启动工程逻辑名 = 物理目录 = `app-erp-all`。完整 18 域 × 命名维度映射见 `domain-module-split-analysis.md §2.0`。

```
app-erp-master-data（基础，无业务依赖）
        ↑
app-erp-inventory（依赖 master-data：物料/仓库）
        ↑
app-erp-purchase / app-erp-sales（依赖 master-data + inventory）
        ↑
app-erp-finance（依赖 master-data + 各业务域的 I*Biz）

扩展域：
app-erp-assets（依赖 master-data + inventory；被 finance ORM 引用；→finance 凭证经 voucherId 弱指针）
app-erp-projects（依赖 master-data；被 finance ORM 引用 + 被 purchase/sales/hr ORM 只读引用；S 写 finance 凭证）
app-erp-manufacturing（依赖 master-data + inventory；被 finance ORM 引用；事件触发 quality 检验）
app-erp-quality（依赖 master-data；被 purchase/sales/manufacturing 事件触发检验，零业务域 ORM 引用）
app-erp-maintenance（依赖 master-data + inventory + assets；被 manufacturing 引用）
app-erp-hr（依赖 master-data + projects ORM 只读引用：员工项目分配/工时归集）
```

### 核心业务域

| 领域工程 | 允许依赖 | 禁止依赖 |
|----------|----------|----------|
| `app-erp-master-data` | （无业务依赖，仅平台） | 任何业务域 |
| `app-erp-inventory` | master-data | purchase / sales / finance / 扩展域 |
| `app-erp-purchase` | master-data / inventory / projects（只读 ORM 引用） | sales / finance / 其他扩展域（assets/manufacturing/quality/maintenance） |
| `app-erp-sales` | master-data / inventory / projects（只读 ORM 引用） | purchase / finance / 其他扩展域（assets/manufacturing/quality/maintenance） |
| `app-erp-finance` | master-data + 各域 `I*Biz`（只读查询） | （处于核心 DAG 顶层） |

### 扩展业务域

| 领域工程 | 允许依赖 | 禁止依赖 |
|----------|----------|----------|
| `app-erp-assets` | master-data / inventory | finance（finance 引用 assets，不反向；assets→finance 凭证经 `voucherId` 弱指针，无 ORM to-one，应用层按需查 `IErpFinVoucherBiz`） |
| `app-erp-projects` | master-data | finance ORM 引用（finance→projects 单向合法，projects 不反向 ORM 引用 finance）；**S 写 finance 允许**（成本归集过账，见数据矩阵 §4.2）；**purchase/sales ORM 只读引用允许**（项目采购/销售单按 `projectId` 对 `ErpPrjProject` 建 to-one，归集项目成本，机制 B 只读，projects 不反向） |
| `app-erp-manufacturing` | master-data / inventory | finance / maintenance（ORM + S 写反向）；**quality 仅事件触发检验**（ORM 零引用，manufacturing→quality 单向业务触发允许） |
| `app-erp-quality` | master-data | 任何业务域 ORM 引用（quality 被业务域事件触发检验，不反向 ORM 依赖） |
| `app-erp-maintenance` | master-data / inventory / assets | manufacturing / finance（反向） |
| `app-erp-hr` | master-data / projects（员工项目分配/工时归集，按 `projectId`/`taskId` 对 `ErpPrjProject`/`ErpPrjTask` 建 to-one 只读引用，机制 B，projects 不反向 ORM 引用 hr，无环） | 任何业务域 ORM 反向引用（hr 为叶域，不被业务域 ORM 引用） |
| `app-erp-all`（聚合） | 所有领域工程的 `-service`/`-web` | — |

> **关于 projects ↔ finance 与 manufacturing → quality 的分层说明**（源自 `data-dependency-matrix.md §2.0` 裁决原则 5）：ORM 层 finance→projects 单向、S 写层 projects→finance 单向，两层各自无环即合法，不构成循环。manufacturing→quality 仅事件/I*Biz 触发检验，ORM 零引用，同样合法。

## 跨工程实体关系规则

**规则**（源自 `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`）：在**共享单库 + 单向 DAG** 前提下，业务域通过 `notGenCode="true"` 外部实体引用建立到 master-data 的 ORM `<to-one>` 关联。

- **允许**：业务域 orm.xml 声明 `<entity notGenCode="true" tableName="erp_md_*">` 引用 master-data 表，建立 `<to-one>`，支持 EQL 点导航与 GraphQL 展开。
- **允许**：finance → projects/assets（finance 是 DAG 顶，单向合法）；purchase/sales → projects（项目采购/销售单按 `projectId` 建 to-one 只读引用用于成本归集，机制 B 只读，projects 不反向）；hr → projects（员工项目分配/工时归集，按 `projectId`/`taskId` 建 to-one 只读引用，projects 不反向，无环）。
- **禁止**：业务域之间的反向或循环引用（如 inventory → purchase/sales、purchase ↔ sales、projects/assets → finance → 反向回 purchase/sales）。这些走纯外键 + 弱指针 + `I*Biz`。
- **平台依据**：`nop/schema/orm/entity.xdef` 的 `@notGenCode` 注释 + `nop/orm/xlib/orm-gen.xlib:228` 平台代码生成器自身范式。
- **完整清单**：哪些业务域引用哪些 master-data 表、to-one 命名、DAG 验证，见 `data-dependency-matrix.md §5.6`。

**跨模块关联查询的两条路线**（完整机制见 `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`）：

- **路线 1（机制 B，`notGenCode="true"` 外部实体引用）**：已在本工程 17 个业务域 orm.xml 落地（约 369 个跨模块 to-one，见 `data-dependency-matrix.md §5.6.2` 矩阵）。适用于高频多维关联查询（如 finance 凭证行按 subject/partner/project/warehouse/material 筛选）。
- **路线 2（机制 D，纯外键 + `I*Biz`）**：断开 ORM 关联，用冗余显示名（列表）+ `@BizLoader`/`requireBiz`（详情）+ EQL 子查询（报表）。适用于业务表之间的引用（凭证反查源单等弱指针场景）。

> **例外**：业务模块需要给主数据实体加业务字段时（类比 nop-app-mall 扩展 `NopAuthUser`），通过 `app-erp-delta` 工程做 `ext:baseClass` Delta 扩展，不在业务域建新表或写跨模块 `<to-one>`。

> **表级数据依赖明细**（哪些模块只读 R / 同步写 S / 弱指针 P 哪些表、跨域字段目录、`billType` 枚举）：见 `data-dependency-matrix.md`。本文只规定模块级依赖方向，数据层细化由该文档承载。

> **依赖方向冲突裁决**：当本文模块级摘要与 `data-dependency-matrix.md` 数据层清单冲突时，以数据层清单为准（裁决优先级见 `data-dependency-matrix.md §2.0`）。

## 业财打通跨工程协作

财务域定义 `IErpFinAcctDocProvider` 接口 + `ErpFinAcctDocRegistry`（注入 `List<IErpFinAcctDocProvider>`）；各业务域（purchase/sales/inventory）实现自己的凭证生成 Provider（`@Component`）。新增单据类型 = 新增 Provider Bean，零改动财务核心。详见 `docs/design/finance/posting.md`。

## Owner Docs

| 边界 | Owner Doc |
|------|-----------|
| 领域归属与跨域协作规则 | `docs/design/domain-design-guidelines.md` |
| 全局业务流程编排 | `docs/design/flow-overview.md` |
| 主数据域业务规则 | `docs/design/master-data/README.md` |
| 库存域业务规则 | `docs/design/inventory/README.md` |
| 采购域业务规则 | `docs/design/purchase/README.md` |
| 销售域业务规则 | `docs/design/sales/README.md` |
| 财务域业务规则 | `docs/design/finance/README.md` |
| 固定资产域业务规则 | `docs/design/assets/README.md` |
| 项目管理域业务规则 | `docs/design/projects/README.md` |
| 制造域业务规则 | `docs/design/manufacturing/README.md` |
| 质量管理域业务规则 | `docs/design/quality/README.md` |
| 设备维护域业务规则 | `docs/design/maintenance/README.md` |
| 客户关系域业务规则 | `docs/design/crm/README.md` |
| 客户服务域业务规则 | `docs/design/customer-service/README.md` |
| 人力资源域业务规则 | `docs/design/human-resource/README.md` |
| 高级排程域业务规则 | `docs/design/aps/README.md` |
| 合同域业务规则 | `docs/design/contract/README.md` |
| 分销资源域业务规则 | `docs/design/drp/README.md` |
| 物流域业务规则 | `docs/design/logistics/README.md` |
| B2B 域业务规则 | `docs/design/b2b/README.md` |
| 模块拆分决策与命名 | `docs/architecture/domain-module-split-analysis.md` |
| 数据依赖矩阵（表级只读/同步写/弱指针） | `docs/architecture/data-dependency-matrix.md` |

## 规则

如果重复出现的设计争论依赖于模块所有权，在此处写下答案，而非在聊天中重新辩论。