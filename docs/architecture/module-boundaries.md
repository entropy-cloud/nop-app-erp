## Module Boundaries

## Purpose

Define the main code ownership boundaries for `nop-app-erp`.

## 领域工程依赖方向（DAG）

nop-app-erp 按 5 个业务域拆成独立 Maven 工程，依赖方向严格单向（DAG）。详细决策见 `domain-module-split-analysis.md`。

```
app-erp-master-data（基础，无业务依赖）
        ↑
app-erp-inventory（依赖 master-data：物料/仓库）
        ↑
app-erp-purchase / app-erp-sales（依赖 master-data + inventory）
        ↑
app-erp-finance（依赖 master-data + 各业务域的 I*Biz）
```

| 领域工程 | 允许依赖 | 禁止依赖 |
|----------|----------|----------|
| `app-erp-master-data` | （无业务依赖，仅平台） | 任何业务域 |
| `app-erp-inventory` | master-data | purchase / sales / finance |
| `app-erp-purchase` | master-data / inventory | sales / finance |
| `app-erp-sales` | master-data / inventory | purchase / finance |
| `app-erp-finance` | master-data + 各域 `I*Biz`（只读查询） | （处于 DAG 顶层） |
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
| 模块拆分决策与命名 | `docs/architecture/domain-module-split-analysis.md` |

## Rule

If a recurring design argument depends on module ownership, write the answer here instead of re-litigating it in chat.
