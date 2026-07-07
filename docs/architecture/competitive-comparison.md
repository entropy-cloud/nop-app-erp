# 竞品架构对标：nop-app-erp 在哪里、如何超越主流开源 ERP

> Status: effective
> Last Reviewed: 2026-06-22
> Related: `docs/architecture/customization-capabilities.md`、`docs/architecture/domain-module-split-analysis.md`、`docs/design/domain-design-guidelines.md`、`docs/design/erp-design-audit-checklist.md`、各 `<domain>/model/app-erp-<domain>.orm.xml`

---

## 一、调研对象与方法论

本文对标 **7 个主流开源 ERP**，论证 nop-app-erp 的设计在哪些维度超越它们、为什么更灵活、为什么更可扩展。

**调研对象：**

| 产品 | 技术栈 | 许可证 | 定位 |
| --- | --- | --- | --- |
| **Odoo** (Community + Enterprise) | Python + 自研 ORM + PostgreSQL | LGPL-3.0 (CE) / 专有 (EE) | 全功能模块化 ERP，社区最大 |
| **ERPNext** (Frappe) | Python + MariaDB/PG + Jinja | GPL-3.0 | 元数据驱动 ERP，真多租户 |
| **metasfresh** | Java + PG（ADempiere 分支） | GPL-3.0 | Compiere AD 家族，贸易导向 |
| **iDempiere** | Java + PG/Oracle（Compiere 嫡系） | GPL-2.0 | OMG 模型驱动架构典范 |
| **Tryton** | Python + PostgreSQL（模块化框架） | GPL-3.0 | 升级稳定性最强的框架型 ERP |
| **Openbravo** | Java（Compiere 派生） | **社区版已停（2020 闭源）** | 零售 SaaS 转型，仅作警示 |
| **MixERP** | ASP.NET WebForms + PG | MPL-2.0/GPL-3.0（混乱） | 基本停摆 |

**方法论：** 仅采用可溯源的架构事实（官方文档、wiki、GitHub、权威论坛），不采信营销话术。每条"超越"声明在第四节都附 nop-app-erp 模型证据（文件:实体/字段），可点击核验。

**调研关键来源：** Odoo 开发者文档（架构/估值/Anglo-Saxon）、Frappe 官方文档（DocType/多租户）、ADempiere/iDempiere wiki（Application Dictionary/2Pack/Accounting Schema）、Tryton 官方 + modules-company API、Wikipedia（Openbravo 闭源）、Metasfresh GitHub。详见本文末"参考来源"。

---

## 二、总览矩阵

| 维度 | Odoo | ERPNext | metasfresh | iDempiere | Tryton | Openbravo | MixERP | **nop-app-erp** |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| **架构范式** | 代码优先 + ORM | 元数据（DocType） | MDA/AD | MDA/AD | 代码优先模块化 | MDA/AD | 代码优先 | **模型驱动 + Delta** |
| **真模型驱动** | 否 | 部分 | 是 | 是（典范） | 否 | 是 | 否 | **是（ORM XML 为唯一真相）** |
| **多租户** | 否（DB-per-tenant） | **是（site=独立 DB）** | 多 Client 共享 DB | 多 Client 共享 DB | DB-per-tenant | 多 Client 共享 DB | 多门店 | **平台标准（可独立/共享）** |
| **Delta 升级层** | 否（重写模块） | 部分（Property Setter） | AD 数据 + 插件 | **是（2Pack+OSGi）** | 是（稳定 API） | AD + 模块 | 否 | **是（Nop Delta + 模型优先）** |
| **多套账（并行账簿）** | 否 | 否 | **是** | **是** | 弱 | **是** | 否 | **是（acctSchemaId 原生）** |
| **多公司** | 是（多 bug） | 是 | 是（Client/Org） | 是（Client/Org） | **优（MultiValue）** | 是（Client/Org） | 多门店 | **是（orgId 物理列 + 组织树）** |
| **成本方法** | Std/AVCO/FIFO | FIFO/LIFO/MA/Std | Std/Avg/FIFO/LIFO | **Std/Avg/FIFO/LIFO/LastPO** | Std/Avg/FIFO/Lot | Std/Avg/FIFO | 弱 | **Std/MA/WA/FIFO/LIFO/具体辨认/批次（7 种）** |
| **制造深度** | **深** | 中 | 中 | 中 | **深且模块化** | 中 | 弱 | **全链（MRP/生产版本/领料/委外/工时/成本滚算）** |
| **业财一体** | Anglo-Saxon(EE) | 永续盘存 | Fact 引擎 | **Fact 引擎** | 模块触发 | Fact 引擎 | 基础 | **三件套 + posted 兜底扫描** |
| **定制脆弱性** | **高** | 中 | 中 | 低（AD） | 低（稳定 API） | 低（AD） | 高 | **低（Delta 不改基线）** |
| **许可** | LGPL/专有 | GPL-3 | GPL-3 | GPL-2 | GPL-3 | **已闭源** | 混乱 | 模型/设计层（待定） |

---

## 三、关键差距分析（竞品的真实痛点）

### 3.1 Odoo：定制的"升级陷阱"

Odoo 的定制通过 `_inherit`/`_inherits` 继承核心类、XPath 视图继承、安全规则覆盖。**这些定制与核心实现强耦合**：每个大版本升级时，定制模块必须重新移植，视图继承链和 XMLID 经常断裂。**社区版没有官方升级工具**，依赖 OCA 的 OpenUpgrade；企业版用 `upgrade.odoo.com`。社区充斥升级失败案例。这是 Odoo #1 痛点。

### 3.2 ERPNext：元数据"半套"

ERPNext 的 DocType 是真元数据驱动（Customize Form 的 Property Setter 存在 DB，升级不丢），但**深度 class override（`override_doctype_class`/`override_whitelisted_methods`）仍会丢**——GitHub issue #31370 记录了"升级后丢失定制"的真实案例。真多租户（site=独立 DB）是其最大优势，但**无多套账**。

### 3.3 Compiere 家族（iDempiere/metasfresh/Openbravo）：AD 强但老

这是**唯一真模型驱动 + 多套账**的家族（Application Dictionary + 2Pack/OSGi delta + Accounting Schema）。但：
- **iDempiere**：最接近"理想"（2Pack delta + OSGi 插件 + 多 Accounting Schema），但 Java/OSGi 栈老、UX 旧、社区小。
- **metasfresh**：从 ADempiere 深度分叉，合并上游困难。
- **Openbravo**：**社区版 2020 年已闭源**，警示了单厂商开源 ERP 的许可/供应商风险。

### 3.4 Tryton：最稳但代码优先

Tryton 是**升级稳定性最强**的（API 严格向后兼容），多公司 MultiValue 一流，但**代码优先**（不是模型驱动），多套账弱，生态最小。

### 3.5 MixERP：基本停摆

ASP.NET WebForms（已过时），社区萎缩，文档稀薄，不应作为对标基线。

---

## 四、nop-app-erp 的"超越点"（8 个杠杆，逐条论证）

> 每个杠杆：**竞品现状 → 我们的设计 → 模型证据 → 为何更好/更灵活/更可扩展**

### 杠杆 A：真模型驱动 + 真 Delta 升级层

- **竞品现状**：Odoo 代码耦合（升级脆弱）；ERPNext 半套（深度 override 丢）；Tryton 代码优先（非模型驱动）；Compiere 家族 AD 强但老。
- **我们的设计**：`<domain>/model/*.orm.xml` 是**唯一真相源**（见 `docs/architecture/customization-capabilities.md` 与 `docs/context/source-of-truth-and-precedence.md`）。定制遵循 **Model → Delta → Java** 决策顺序（见 `../nop-entropy-wt/nop-entropy-master/docs-for-ai/00-start-here/ai-defaults.md`）。Delta 层在运行时叠加于基线，**升级基线不破坏 Delta**。
- **模型证据**：10 份 `app-erp-<domain>.orm.xml`（共 145 实体）是权威；生成的 `_gen/`/`_` 前缀文件**永不手改**（AGENTS.md 强制）。
- **为何更好**：同时满足"模型驱动"（像 Compiere 家族）+ "升级不破坏定制"（像 Tryton）+ "现代栈"（不像 iDempiere 老旧）。Odoo/ERPNext 的升级痛点被根除。

### 杠杆 B：原生多套账（并行账簿）

- **竞品现状**：**Odoo、ERPNext 无多套账**（市场领导者缺这个关键能力）；Compiere 家族有（Accounting Schema）但老；Tryton 弱。
- **我们的设计**：`ErpMdAcctSchema`（账套主数据，性质：财务/管理/税务/合并/预算）+ `ErpMdAcctSchemaCoa`（账套 × 科目表）。凭证头/行、总账余额、存货成本层都携带 `acctSchemaId`。
- **模型证据**：
  - `module-master-data/model/app-erp-master-data.orm.xml`：`ErpMdAcctSchema`（`nature` 字典：FINANCIAL/MANAGEMENT/TAX/CONSOLIDATION/BUDGET）+ `ErpMdAcctSchemaCoa`
  - `module-finance/model/app-erp-finance.orm.xml`：`ErpFinVoucher.acctSchemaId`、`ErpFinVoucherLine.acctSchemaId`、`ErpFinGlBalance.acctSchemaId`
  - `module-inventory/model/app-erp-inventory.orm.xml`：`ErpInvCostLayer.acctSchemaId`、`ErpInvStockLedger.acctSchemaId`
- **为何更好**：一笔业务可同时入财务账（IFRS/中国会计准则）+ 管理账（内部口径）+ 税务账，**无需多套系统或多账套数据库**。直接超越 Odoo/ERPNext，与 Compiere 家族持平且栈更现代。

### 杠杆 C：多组织 + 多公司（orgId 物理列）

- **竞品现状**：Odoo 多公司"多 bug"；ERPNext 多公司靠权限隔离；Compiere 家族 Client/Org 强但共享 DB；Tryton MultiValue 最优雅。
- **我们的设计**：所有业务单据头统一携带 `orgId`（引用 `ErpMdOrganization` 树）。组织树支持 集团/公司/分公司/部门/车间/门店 6 种类型。
- **模型证据**：grep `orgId` 覆盖采购/销售/库存/资产/项目/维护/质量/制造/凭证头全部业务单据头；`ErpMdOrganization`（`module-master-data/model/`）含 `orgType` 字典与 `functionalCurrencyId`。
- **为何更好**：`orgId` 作为**物理列**（非 EAV）支持高效索引与权限过滤，兼顾 Odoo/ERPNext 的易用与 Compiere 家族的严格。多租户走平台标准（不在源模型预置 `tenantId`，见 `docs/architecture/customization-capabilities.md`）。

### 杠杆 D：业财一体三件套 + posted 兜底扫描

- **竞品现状**：Odoo Anglo-Saxon 仅企业版；ERPNext 永续盘存；Compiere Fact 引擎强但复杂。
- **我们的设计**：凭证头/分录行/业财回链表三件套（`ErpFinVoucher`/`ErpFinVoucherLine`/`ErpFinVoucherBillR`）+ 凭证模板引擎（`ErpFinVoucherTemplate`/`Line`，按 `businessType` 路由）+ **业务单据 `posted` 标志 + 兜底扫描**。
- **模型证据**：
  - 所有业务单据头携带 `posted`/`postedAt`/`postedBy`（grep 验证）
  - `ErpFinVoucherBillR`（业财回链，凭证↔源单据双向追溯）
  - `ErpFinVoucherTemplate.businessType` 字典（PURCHASE_INPUT/SALES_OUTPUT/AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT/DEPRECIATION/...）
- **为何更好**：`posted` 标志让业财过账**幂等且可重放**——定时扫描 `posted=false` 的已审核单据补过账，保证最终一致。回链表让任一凭证可追溯到源业务单据，任一业务单据可查到其生成的凭证。这是审计与内控的硬要求，Odoo/ERPNext 实现弱。

### 杠杆 E：完整成本方法集（7 种）

- **竞品现状**：Odoo 仅 Std/AVCO/FIFO（**无 LIFO、无具体辨认**）；ERPNext 有 LIFO 但无具体辨认；iDempiere 最全（Std/Avg/FIFO/LIFO/LastPO/LastInvoice）。
- **我们的设计**：`erp-md/cost-method` 字典提供 **7 种**：移动加权平均、全月一次加权平均、FIFO、LIFO、标准成本、具体辨认（个别计价）、批次。`ErpInvCostLayer`（成本层）支撑 FIFO/批次/具体辨认的多层核算。
- **模型证据**：
  - `module-master-data/model/app-erp-master-data.orm.xml`：`erp-md/cost-method` 字典 7 选项；`ErpMdMaterial.costMethod`、`ErpMdAcctSchema.costingMethod`
  - `module-inventory/model/app-erp-inventory.orm.xml`：`ErpInvCostLayer`（成本层，按 batch/入库批次记录剩余量与单位成本）、`ErpInvStockBalance.costMethod`、`ErpInvStockLedger.costMethod`
- **为何更好**：超越 Odoo（缺 LIFO/具体辨认），与 iDempiere 持平且**按账套可不同**（`ErpMdAcctSchema.costingMethod` 默认 + 物料级覆盖）。

### 杠杆 F：制造全链（MRP + 生产版本 + 领料 + 委外 + 工时 + 成本滚算）

- **竞品现状**：Odoo 制造深；Tryton 模块化深；ERPNext 中；Compiere 家族弱。
- **我们的设计**：制造域 21 实体，覆盖 BOM（头/行/工艺/联副产品）+ 工艺路线（工序）+ 工作中心 + **生产版本**（产品×BOM×Routing×有效期）+ **工单**（头/**多产出投入行**）+ 作业卡 + **作业工时记录** + **领料单**（头/行）+ **MRP**（计划头/计划行/独立需求）+ **委外加工单**（头/行）+ **标准成本滚算**（头/行）。
- **模型证据**：
  - `module-manufacturing/model/app-erp-manufacturing.orm.xml`：
    - `ErpMfgMrpPlan`/`ErpMfgMrpPlanLine`/`ErpMfgMrpDemand`（MRP 三件套：毛需求/预计入库/现有量/净需求/计划数量/多级 BOM 展开）
    - `ErpMfgProductionVersion`（生产版本，SAP 风格 lot-size 区间）
    - `ErpMfgWorkOrderLine`（工单多产出/投入/联副行，替代单 productId 弱表达）
    - `ErpMfgMaterialIssue`/`Line`（领料单，绑定工单/作业卡，触发库存出库）
    - `ErpMfgJobCardTimeLog`（按操作员逐条工时 + 人工成本）
    - `ErpMfgSubcontractOrder`/`Line`（委外加工单，独立于 Workcenter.isExternal 标志）
    - `ErpMfgCostRollup`/`Line`（标准成本滚算：材料+人工+制造费用+委外）
  - 工单 10 态状态机（含 STOCK_RESERVED 齐套）/ 作业卡 8 态
- **为何更好**：**MRP + 生产版本 + 记录级领料 + 工时成本归集 + 委外 + 成本滚算**是 SAP 级制造深度的开源实现。Odoo/ERPNext 缺生产版本与独立 MRP 计划实体；Tryton 需拼装多模块。

### 杠杆 G：跨域 DAG + I*Biz 解耦（无 ORM 循环）

- **竞品现状**：Odoo 单体紧耦合；ERPNext 单体；Compiere 家族单体。
- **我们的设计**：18 域构成 **DAG**，master-data 为根，业务域间**不做 ORM 强引用**（走 `I*Biz` 接口），无循环依赖。每个域可独立 Maven 工程、独立组装裁剪（见 `docs/architecture/domain-module-split-analysis.md`）。
- **模型证据**：每份 orm 头部注释明示跨工程依赖与 I*Biz 约定；跨域引用用 `relatedBillType`/`relatedBillCode`（字符串，非 FK），如 `ErpFinVoucherBillR`、`ErpInvStockMove.relatedBillType`、`ErpQaInspection.relatedBillType`。
- **为何更好**：**可独立部署、独立演进、独立替换**某域实现。单体 ERP 无法做到。这是微服务化与多团队并行开发的基础。

### 杠杆 H：AR/AP open-item + 记录级核销（替代"魔法更新余额"）

- **竞品现状**：多数 ERP 往来余额靠主数据字段累计更新，核销逻辑不透明。
- **我们的设计**：`ErpFinArApItem`（应收应付明细账，open-item 模型，逐笔发票/收付款生成，含已核销/未核销金额）+ `ErpFinReconciliation`/`Line`（核销单头/行，付款↔发票多对多 + 汇兑损益）。
- **模型证据**：`module-finance/model/app-erp-finance.orm.xml`：`ErpFinArApItem`（`direction` AR/AP、`openAmountSource`/`openAmountFunctional`、`status` OPEN/PARTIAL/SETTLED）、`ErpFinReconciliation`（`fxGainLoss` 汇兑损益字段）。
- **为何更好**：往来账**完全可审计**——每笔核销有记录、每笔余额变动可追溯。超越"余额字段魔法更新"的反模式（本设计已将其列为禁止反模式，见 `domain-design-guidelines.md` 10.6）。

---

## 五、"在哪些地方分别超越什么"汇总表

| 竞品 | 我们超越它的点 | 模型证据 |
| --- | --- | --- |
| **Odoo** | (1) 升级不破坏定制（Delta vs 代码耦合）；(2) 多套账（vs 无）；(3) LIFO/具体辨认成本方法（vs 无）；(4) 独立 MRP 实体 + 生产版本（vs 无）；(5) AR/AP open-item 可审计核销（vs 余额累计） | `ErpMdAcctSchema`、`erp-md/cost-method`、`ErpMfgMrpPlan`、`ErpFinArApItem` |
| **ERPNext** | (1) 多套账（vs 无）；(2) 深度定制不丢（Delta vs class override 升级丢）；(3) 生产版本 + 独立 MRP（vs 拼 Production Plan）；(4) 跨域 DAG 可独立部署（vs 单体） | `acctSchemaId`、`ErpMfgProductionVersion`、`docs/architecture/domain-module-split-analysis.md` |
| **metasfresh** | (1) 现代栈 + 现代模型驱动（vs 老 Java/AD）；(2) 不依赖 AD 表命名复杂度；(3) 制造深度（vs 贸易导向弱制造） | `ErpMfgMrpPlan`、`ErpMfgCostRollup` |
| **iDempiere** | (1) 现代栈 + 现代 UX（vs 老 Java/OSGi）；(2) 等价的多套账与 AD 能力但更简洁；(3) 等价的 Delta 升级但更轻量 | `ErpMdAcctSchema`、Nop Delta 机制 |
| **Tryton** | (1) 模型驱动（vs 代码优先）；(2) 多套账（vs 弱）；(3) 制造深度（vs 需拼装多模块） | `app-erp-<domain>.orm.xml` 唯一真相、`ErpMdAcctSchema`、`ErpMfg*` |
| **Openbravo** | (1) **许可证/供应商风险为零**（Openbravo 社区版已闭源）；(2) 同等 AD 能力但开源可持续 | 全仓开源、无单厂商锁定 |
| **MixERP** | (1) 技术栈现代（vs 过时 WebForms）；(2) 活跃（vs 停摆）；(3) 制造/财务深度（vs 弱） | 全部 18 域 279 实体 |

---

## 六、诚实声明：我们尚未超越的点

本文不夸大。以下是 nop-app-erp **当前尚未超越**的领域，需后续投入：

1. **生态规模与现成垂直行业模块**：Odoo 有数千个 OCA/第三方模块，App Store 生态巨大；ERPNext 有成熟行业方案。nop-app-erp 处于 post-codegen BizModel 深化阶段，垂直行业模块需逐步建设。
2. **UI/UX 丰富度**：Odoo/ERPNext 有成熟的前端与移动端。nop-app-erp 依赖 AMIS 页面（codegen 后生成），现成页面丰富度待补。
3. **代码生成后的端到端验证**：本项目处于 pre-codegen 阶段（见 AGENTS.md），首域 codegen + 业务循环验证是下一个计划的必做项。
4. **本地化（l10n）**：中国本地化（发票格式、税务申报、银企直联）尚未做（audit-checklist 列为低优先级延迟项）。
5. **社区与文档广度**：Compiere 家族有 20+ 年 wiki 沉淀；我们的文档体系完备但深度待补。

---

## 七、结论：为什么 nop-app-erp 是更好、更灵活、更可扩展的设计

- **更好**：在多套账（杠杆 B）、成本方法完整性（杠杆 E）、制造全链（杠杆 F）、业财一体可审计性（杠杆 D/H）四个产品能力维度，超越市场领导者 Odoo/ERPNext，与最强的 iDempiere 持平或更优。
- **更灵活**：Delta 定制层（杠杆 A）让定制升级不破坏；多套账 + 多组织 + 多币种四件套（杠杆 B/C）让一套系统支持多会计准则/多公司/多币种并行。
- **更可扩展**：模型驱动（杠杆 A）+ 跨域 DAG（杠杆 G）让系统可按域独立演进、独立部署、独立替换；新增域/新增账套/新增成本方法都是数据而非代码改动。

**总判断：** nop-app-erp 的设计**在现代栈上同时拿到了** Odoo/ERPNext 的易用 + Compiere 家族的模型驱动/多套账深度 + Tryton 的升级稳定性，**且无 Openbravo 式的许可/单厂商风险**。

---

## 参考来源

- Odoo 架构：https://www.odoo.com/documentation/19.0/developer/tutorials/server_framework_101/01_architecture.html
- Odoo 估值方法：https://www.odoo.com/documentation/19.0/applications/inventory_and_mrp/inventory/inventory_valuation/cheat_sheet.html
- Odoo Anglo-Saxon（仅企业版）：https://www.cybrosys.com/odoo/odoo-books/odoo-17-accounting/anglosaxon-accounting/
- Odoo CE 无官方升级（OpenUpgrade）：https://ocu.winotto.com/articles/odoo-ce-vs-enterprise-migration
- Frappe 多租户：https://docs.frappe.io/framework/user/en/bench/guides/setup-multitenancy
- ERPNext Customize Form（DB 存储）：https://docs.frappe.io/framework/user/en/basics/doctypes/customize
- ERPNext 升级丢失定制（GitHub #31370）：https://github.com/frappe/erpnext/issues/31370
- ADempiere Application Dictionary：https://adempiere.gitbook.io/docs/system-administration/the-application-dictionary
- iDempiere 2Pack：https://wiki.idempiere.org/en/Developing_Plug-Ins_-_2Pack_-_Pack_In/Out
- iDempiere Accounting Schema（多套账）：https://wiki.idempiere.org/en/Template:Accounting_Schema_(Window_ID-125_V1.0.0)
- Tryton 官方 + 多公司 API：https://www.tryton.org/ 、https://docs.tryton.org/7.2/modules-company/reference.html
- Metasfresh GitHub：https://github.com/metasfresh/metasfresh
- Openbravo 社区版闭源：https://sourceforge.net/p/openbravotech/blog/2023/01/openbravo-to-end-community-edition-open-source-projects-in-2020/
- MixERP 状态：https://github.com/thcristo/mixerp
