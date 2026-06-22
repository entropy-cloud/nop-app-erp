---
调研日期: 2026-06-22
来源: 13 个 ERP 项目调研（见 erp-survey/ 同目录）
状态: 已完成（基于源码实测归纳）
---

# ERP 模块拆分粒度横向对比

> 横向对比 13 个项目的模块化策略，为 nop-app-erp 的"是否拆多模块/多 model"决策提供参照系。
>
> **核心结论先行**：nop-app-erp 应采用**业务域粗粒度拆分（5-7 个领域 Maven 模块）+ 每域独立 orm.xml**，而非单 `app-erp.orm.xml`。理由见 `docs/architecture/domain-module-split-analysis.md`。本文提供横向证据。

## 模块化策略谱系

13 个项目按模块化粒度从细到粗排列：

| 粒度 | 项目 | 模块化机制 | 模块数 | 评价 |
|---|---|---|---|---|
| **极细（功能维度）** | Odoo | addon + 模型继承（`_inherit`） | **625 addon** | 适合超大规模社区协作；单一团队维护成本极高 |
| **细（功能/技术 OSGi）** | iDempiere | OSGi Equinox bundle（66 个） | 66 plugin | 技术关注点拆分细，但**业务模型集中在单 base plugin** |
| **细（业务域+垂直行业）** | Metasfresh | Maven 多模块 | **~140 模块** | 业务边界清晰，但过度拆分；Spring DI 聚合 |
| **中（业务域）** | 赤龙 ERP | Maven 多模块（erp-parent） | ~6 模块（common/masterData/inv/finance/order + CasServer） | **业务域拆分合理，适合中型团队** |
| **中（业务域+部署形态）** | 星云 ERP | Maven 多模块（业务域 + 单体/Cloud 双入口） | 9 模块（core/basedata/sc/settle/chart/comp + api/cloud-api/gateway） | **业务域拆分 + 双形态部署，演进友好** |
| **中（业务域）** | Tryton | 框架 + 业务模块独立发布（PyPI egg） | 0（仓库内）/ ~100（PyPI） | 纯框架，模块独立版本化 |
| **粗（业务域 + doctype）** | ERPNext | 顶层业务域 + doctype + purpose | **~15 域** | 粗模块 + 元数据驱动单据，单一团队友好 |
| **粗（目录式）** | Dolibarr | 目录式模块（无元数据） | ~77 目录 | 扁平堆砌，无领域边界 |
| **粗（单应用 + 包分层）** | 管伊佳 | 单 SpringBoot + 包分层 | 1 应用 | 无模块化，部署简单 |
| **粗（单 admin 模块）** | 若依 ERP ×2 | 若依脚手架 + 单 admin-erp 模块 | 1 业务模块 | 业务全塞 admin，无领域边界 |
| **粗（4 层 + 双 Web）** | WMES | 经典 4 层（Code/Data/Domain/Service/Web） | 7 工程 | 技术分层而非业务域拆分 |

## 五种典型模式详解

### 模式 A：极细功能维度 addon（Odoo）
- **机制**：每个功能独立 addon，靠模型继承（`_inherit`）和字段反向关联组合。`stock_account`（存货会计）从 `stock` 独立；`sale_stock_margin`（销售+库存+毛利）三维交叉独立
- **家族数**：mrp 11 / sale 29 / purchase 9 / stock 9 addon
- **优势**：超大社区协作、功能可插拔、垂直行业易扩展
- **劣势**：模块爆炸、依赖复杂、单一团队维护成本极高
- **对 nop 启示**：**不照搬**。nop 用 Delta + xmeta 扩展点达到类似灵活性，不靠模块数量

### 模式 B：业务域 Maven 多模块（赤龙/星云，最贴近 nop）
- **机制**：按业务域切 Maven 模块，每域独立包结构；星云额外提供单体/Cloud 双形态部署
- **赤龙**：`erp-common`/`erp-masterData`/`erp-inv`/`erp-finance`/`erp-order` + `CasServer`
- **星云**：`xingyun-core`/`xingyun-basedata`/`xingyun-sc`/`xingyun-settle`/`xingyun-chart`/`xingyun-comp` + `xingyun-api`(单体)/`cloud-xingyun-cloud-api`(微服务)/`cloud-xingyun-cloud-gateway`
- **优势**：业务边界清晰、可独立演进、部署形态可切换
- **劣势**：模块数需控制（赤龙 6 / 星云 9 合理；Metasfresh 140 过度）
- **对 nop 启示**：**主推模式**。nop-app-erp 拆 `app-erp-master-data`/`app-erp-inventory`/`app-erp-purchase`/`app-erp-sales`/`app-erp-finance`(+`app-erp-manufacturing` 视范围) + 聚合 `app-erp-app`

### 模式 C：粗模块 + 元数据驱动单据（ERPNext）
- **机制**：顶层 ~15 业务域，每域内用 doctype + 子表 + purpose 表达复杂度（`Stock Entry` 一单 13 purpose）
- **优势**：单一团队友好、单据类型扩展灵活
- **劣势**：doctype 元数据系统是 Frappe 特性，nop 有自己的 xmeta/orm
- **对 nop 启示**：借鉴"粗模块 + 单据类型参数化"思路，但用 nop 的字典 + 作业类型模板实现，非复刻 doctype

### 模式 D：技术分层 OSGi（iDempiere）
- **机制**：66 个 OSGi bundle，按技术关注点（数据库/UI/报表/调度/业务）拆，**但业务模型集中在单 `org.adempiere.base` plugin**
- **关键观察**：OSGi 拆的是"技术层"而非"业务域"——这与 nop-entropy"业务领域各成独立模块"哲学**相反**
- **对 nop 启示**：**不照搬**。nop 按**业务域**拆模块（nop-auth/nop-sys/nop-wf 各自独立），不按技术层拆

### 模式 E：单应用无模块化（管伊佳/若依/Dolibarr）
- **机制**：业务全塞单一应用或 admin 模块，靠包/目录分层
- **优势**：部署简单、上手快
- **劣势**：无领域边界、业务膨胀后难维护、无法独立演进
- **对 nop 启示**：**不照搬**。bootstrap 阶段可单 orm.xml 起步，但进销存+财务一体化预估 80-120 表，远超合理单文件规模，应按域拆分

## 关键决策维度对比

### 单据类型扩展机制

| 项目 | 机制 | 灵活性 | 复杂度 |
|---|---|---|---|
| Odoo | 新增 addon + `_inherit` 扩展模型 | 极高 | 极高 |
| ERPNext | 新增 doctype + purpose | 高 | 中 |
| Metasfresh | 新增 Maven 模块 + `@Component IAcctDocProvider` | 高 | 中 |
| 管伊佳 | `subType` 字典扩展 | 中 | 低 |
| 若依 | 新增表 + 五件套代码 | 低 | 低 |
| **nop 推荐** | 字典 + 作业类型模板 + Delta + xmeta 扩展点 | 高 | 中 |

### 业财耦合度

| 项目 | 业务与财务的耦合方式 |
|---|---|
| Odoo | 业务模型独立，财务是独立 addon（`stock_account`/`sale_management`）横切注入 |
| ERPNext | 业务模型独立，财务通过 `on_submit` 钩子横切注入 |
| Metasfresh | 业务模型独立，凭证引擎通过 `IAcctDocProvider` 跨模块聚合 |
| 赤龙 | 业务模块（inv/order）与财务模块（finance）分离，凭证模板驱动 |
| 管伊佳 | 业务与财务耦合在同一单据（`DepotHead` 直接挂 `accountId`） |
| **nop 推荐** | 业务模块独立，财务模块通过 `on_post` 钩子 + `IAcctDocProvider` 跨模块聚合（Metasfresh 范式） |

### 部署形态

| 项目 | 单体 | 微服务 | 双形态 |
|---|---|---|---|
| Odoo | ✅ | — | — |
| ERPNext | ✅ | — | — |
| Metasfresh | ✅ | — | — |
| iDempiere | ✅（OSGi 可分布式） | ⚠️ | — |
| 星云 | ✅ | ✅ | ✅（同代码双入口） |
| 管伊佳/若依/Dolibarr | ✅ | — | — |
| **nop 推荐** | ✅（默认 Quarkus 单体起步） | 可演进 | 按需（仿星云双入口） |

## 对 nop-app-erp 的模块化建议（综合结论）

### 推荐拆分（业务域粗粒度，5-7 个领域模块）

```
nop-app-erp/
├── model/
│   ├── app-erp-master-data.orm.xml      # 物料/SKU/往来单位/仓库/科目表/币种
│   ├── app-erp-inventory.orm.xml        # 库存移动/流水/余额/调拨/盘点
│   ├── app-erp-purchase.orm.xml         # 采购订单/入库/采购发票/付款
│   ├── app-erp-sales.orm.xml            # 销售订单/出库/销售发票/收款
│   ├── app-erp-finance.orm.xml          # 凭证/科目/核销/总账
│   └── app-erp-manufacturing.orm.xml    # BOM/工单（视范围，可选）
├── app-erp-master-data-{codegen,dao,meta,service,web}/
├── app-erp-inventory-{codegen,dao,meta,service,web}/
├── app-erp-purchase-{codegen,dao,meta,service,web}/
├── app-erp-sales-{codegen,dao,meta,service,web}/
├── app-erp-finance-{codegen,dao,meta,service,web}/
├── app-erp-l10n-cn/                     # 中国本地化（独立可拔）
├── app-erp-delta/                       # 对 nop-auth/nop-sys 的 Delta 扩展
└── app-erp-app/                         # 聚合启动
```

### 不推荐的模式（基于 13 项目对比）

1. **Odoo 625 addon 极细粒度**——单一团队维护成本极高
2. **iDempiere OSGi 66 plugin 按技术层拆**——违背"业务域拆模块"原则
3. **Metasfresh 140 Maven 模块**——过度拆分
4. **管伊佳/若依单应用无模块化**——业务膨胀后难维护
5. **Dolibarr 77 目录扁平堆砌**——无领域边界

### 关键工程约束（来自 nop 平台）

- **跨领域实体不做 ORM 层 `refEntityName` 强引用**（平台所有内置模块零反例）——改走 `I*Biz` 接口 + Processor
- **每域独立 VFS moduleId**（如 `app/erp-inventory`、`app/erp-finance`），运行时 `OrmModelLoader` 自动合并
- **本地化做成独立可拔模块**（`app-erp-l10n-cn`），不内建核心实体

详见架构结论 `docs/architecture/domain-module-split-analysis.md`。
