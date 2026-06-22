# 模块边界

## 目的

定义 `nop-app-erp` 的主要代码所有权边界。

## 领域工程依赖方向（DAG）

nop-app-erp 按 10 个业务域拆成独立 Maven 工程，依赖方向严格单向（DAG）。详细决策见 `domain-module-split-analysis.md`。

```
app-erp-master-data（基础，无业务依赖）
        ↑
app-erp-inventory（依赖 master-data：物料/仓库）
        ↑
app-erp-purchase / app-erp-sales（依赖 master-data + inventory）
        ↑
app-erp-finance（依赖 master-data + 各业务域的 I*Biz）

扩展域：
app-erp-assets（依赖 master-data + inventory；被 finance 引用）
app-erp-projects（依赖 master-data；被 finance 引用）
app-erp-manufacturing（依赖 master-data + inventory；被 finance/quality 引用）
app-erp-quality（依赖 master-data；被 purchase/sales/manufacturing 引用）
app-erp-maintenance（依赖 master-data + inventory + assets；被 manufacturing 引用）
```

### 核心业务域

| 领域工程 | 允许依赖 | 禁止依赖 |
|----------|----------|----------|
| `app-erp-master-data` | （无业务依赖，仅平台） | 任何业务域 |
| `app-erp-inventory` | master-data | purchase / sales / finance / 扩展域 |
| `app-erp-purchase` | master-data / inventory | sales / finance / 扩展域 |
| `app-erp-sales` | master-data / inventory | purchase / finance / 扩展域 |
| `app-erp-finance` | master-data + 各域 `I*Biz`（只读查询） | （处于核心 DAG 顶层） |

### 扩展业务域

| 领域工程 | 允许依赖 | 禁止依赖 |
|----------|----------|----------|
| `app-erp-assets` | master-data / inventory | finance（finance 引用 assets，不反向） |
| `app-erp-projects` | master-data | finance / 其他扩展域 |
| `app-erp-manufacturing` | master-data / inventory | finance / quality / maintenance |
| `app-erp-quality` | master-data | 任何业务域（quality 被业务域引用，不反向依赖） |
| `app-erp-maintenance` | master-data / inventory / assets | manufacturing / finance |
| `app-erp-app`（聚合） | 所有领域工程的 `-service`/`-web` | — |

## 跨工程实体关系规则

**硬规则**（源自 `../nop-entropy/docs-for-ai/02-core-guides/architecture-principles.md`）：跨工程实体**不做** ORM 层 `refEntityName` 强引用。

- 引用方工程用**纯外键列**（如 `erp_pur_order.material_id VARCHAR`），不带 `<to-one>` 关系声明。
- 在 BizModel/Processor 层通过 `@Inject IErpMd*Biz`（被引用方在 `*-dao` 暴露的 `I*Biz` 接口）做只读查询和跨工程动作编排。
- 平台所有内置模块（nop-auth/nop-sys/nop-wf）的源 orm.xml 中 `refEntityName` 全部指向本模块包内实体，零跨包引用——本工程遵循同一约束。

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
| 模块拆分决策与命名 | `docs/architecture/domain-module-split-analysis.md` |

## 规则

如果重复出现的设计争论依赖于模块所有权，在此处写下答案，而非在聊天中重新辩论。