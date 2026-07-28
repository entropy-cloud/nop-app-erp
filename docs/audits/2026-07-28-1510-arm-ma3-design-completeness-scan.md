# MA3 设计完整性扫描（A3.2）

> Report ID: `2026-07-28-1510-arm-ma3-design-completeness-scan`
> 里程碑：MA3（文档-实现一致性层）/ 工作项 A3.2
> 审计维度：设计完整性扫描（`design-completeness-scan-prompt.md` 7 维度，含维度 0 开源功能对标）
> 审计日期：2026-07-28
> Skill：`docs/skills/design-completeness-scan-prompt.md`（前瞻性设计缺口扫描专用方法——找"缺失的内容"）
> 来源计划：`docs/plans/2026-07-28-1510-3-audit-remediation-ma3-design-completeness-scan.md`
> 外部基准：`docs/analysis/erp-survey/`（16 开源 ERP + 7 补充项目实测）+ `docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`
> 需求基准：`docs/requirements/product-scope.md`
> 审查对象：`docs/design/` 全树（7 全局文档 + 18 业务域目录 + portal(future)/notify/l10n 跨切面子系统 + 18 跨域模式文档）vs `product-scope.md` + `erp-survey/`
> 互补关系：本扫描（A3.2）**找缺失的内容**；sibling `2026-07-28-1510-arm-ma3-design-doc-baseline.md`（A3.1）**审存在的内容质量**。两者维度互补不重叠。
> 审计性质：纯文档审查（不改应用代码；产出为本报告 + arm-index P1 登记 + scope matrix §2.3 终态标记）。文档补建在 MR2 批量进行。

## 0. 裁决

**Verdict: 有差距（GAPS）**

理由：本扫描**未发现 BLOCKER**——产品范围隐含的全部 18 业务域均有 `docs/design/<domain>/` 设计所有者（维度 1 PASS）；无核心标配功能（开源 ERP 普遍存在的能力）在设计中**完全缺失**且无显式「产品基线外」声明（维度 0 PASS——`feature-coverage-matrix.md` 113/118=95.8% 覆盖率经交叉验证有效，5 未覆盖项均显式分级）；无范围要求域已有代码但零设计的「实现无设计真相」情况需升级交接 A4（维度 1 PASS）。**但发现 2 项 MAJOR**——`flow-overview.md §3` L2 状态映射仅覆盖核心 5 + 制造共 6 域，缺其余 12 个扩展域状态机引用（维度 4）；`domain-glossary.md` 缺 8 第二批扩展域（crm/cs/hr/aps/contract/drp/logistics/b2b）专属词汇节（维度 5）。按 `design-completeness-scan-prompt.md` 严重性指南「跨域流程不完整 / 术语表角色覆盖滞后新域」= major，裁决 GAPS。

**关键区分**：本扫描的 GAPS 是**完整性层 GAPS**（设计文档覆盖广度的横向缺口），与 A3.1 的「文档质量层 FAIL」（已存在文档的自洽/边界/重复）性质不同。A3.1 找"已写的写得不好"；A3.2 找"该写的没写"。两者发现经交叉去重不重复登记（重叠项以交叉引用方式互相印证，不重复占 P1 编号）。

**与 A3.1 的去重裁定**：
- A3.1 P1-MA3-009（8 第二批扩展域从导航/矩阵/看板沉默遗漏，dim 6）覆盖 `app-overview.md`/`domain-design-guidelines.md §1.1`/`dashboards.md` 三个全局文档；本扫描 P1-MA3-022（flow-overview §3）是同一"扩展域覆盖滞后"模式在 `flow-overview.md` 的具体投影——但 A3.1 dim 6 通用发现未点名 flow-overview §3 状态映射，本扫描作为独立 P1 登记（dim 4，跨域流程覆盖）。
- A3.1 P1-MA3-010（8 第二批扩展域无角色基线，dim 9）覆盖 `roles-and-permissions.md`；本扫描 P1-MA3-023（domain-glossary.md 缺 8 域词汇）是同一模式在 `domain-glossary.md` 的具体投影——A3.1 dim 9 角色侧已登记，本扫描补 dim 5 术语侧。两者正交不重复。

**功能覆盖度（维度 0）单独裁决：PASS**（详见 §1）。维度 0 PASS 不抵消完整性层 GAPS——两者性质不同、不可互抵。

---

## 1. 维度 0 — 开源功能对标（核心维度，此前多轮审计遗漏的根本原因）

### 1.1 外部基准

以 `feature-coverage-matrix.md`（113/118 = **95.8%** 总覆盖率，遍历 16 开源 ERP + 7 补充项目逐功能对照）+ `survey-index.md` 速查导航（24 项目 + 18 选型决策场景）+ `business-design-takeaways.md` 9 大业务设计主题为基准。A3.1 §1 已对该矩阵做"声称能力 → 实质设计支撑"交叉验证（PASS）；本扫描在 A3.1 基础上做**第二轮纵深核查**——重点核验开源 ERP 中**普遍存在**的功能点是否在 `docs/design/` 有对应 owner doc（**功能深度而非仅域存在**）。

### 1.2 功能深度核查（开源标配逐项）

按 plan 维度 0 重点核验点抽查开源 ERP 普遍存在的差异化功能：

| 标配能力 | 来源（开源 ERP） | 设计支撑 | 结论 |
|---------|------|---------|------|
| CRM Lead Scoring | ERPNext/Axelor | `crm/lead-scoring.md` | ✅ dedicated owner doc |
| CRM Lead Waterfall（漏斗阶段） | Odoo/ERPNext | `crm/lead-waterfall.md` | ✅ dedicated owner doc |
| CRM 销售预测 | ERPNext | `crm/sales-forecast.md` | ✅ dedicated owner doc |
| CRM 销售序列（sales sequence） | Axelor/odoo | `crm/sales-sequence.md` | ✅ dedicated owner doc |
| CRM 营销活动（UTM 归因） | Odoo | `crm/marketing.md` | ✅ dedicated owner doc |
| CRM 领地/分区（territory） | Axelor | `crm/territory.md` | ✅ dedicated owner doc |
| CPQ（配置定价） | Axelor | `crm/cpq.md` | ✅ dedicated owner doc |
| APS 替代工艺路线 | Axelor | `aps/alternative-routing.md` | ✅ dedicated owner doc |
| APS 自动调度（auto-dispatch） | Axelor | `aps/auto-dispatch.md` | ✅ dedicated owner doc |
| APS 排产（前向/后向） | Odoo/Axelor | `aps/scheduling.md` | ✅ dedicated owner doc |
| MRP 仿真引擎 | Axelor | `manufacturing/simulation-engine.md` | ✅ dedicated owner doc |
| 差异分析（variance analysis） | OFBiz/iDempiere | `manufacturing/variance-analysis.md` | ✅ dedicated owner doc |
| 委外加工（subcontracting） | Odoo `mrp_subcontracting` | `manufacturing/subcontracting.md` | ✅ dedicated owner doc |
| 批次谱系（batch genealogy） | Odoo/ERPNext | `manufacturing/batch-genealogy.md` | ✅ dedicated owner doc |
| 物料预留（material reservation） | Odoo | `manufacturing/material-reservation.md` | ✅ dedicated owner doc |
| SPC 失控预警 | Carbon | `quality/spc.md` | ✅ dedicated owner doc |
| 质检集成（inspection integration） | Carbon/OFBiz | `quality/inspection-integration.md` | ✅ dedicated owner doc |
| 召回事件 | Carbon | `quality/recall.md` | ✅ dedicated owner doc |
| 供应商评分卡 | ERPNext（8-doctype） | `purchase/supplier-evaluation.md` | ✅ dedicated owner doc |
| 三单匹配 | ERPNext | `purchase/three-way-match.md` | ✅ dedicated owner doc |
| 采购申请（requisition） | Odoo/ERPNext | `purchase/requisition.md` | ✅ dedicated owner doc |
| VMI/寄售/受托代销 | Odoo | `inventory/consignment.md` | ✅ dedicated owner doc |
| 库存追踪链 | Odoo/ERPNext | `inventory/trace-chain.md` | ✅ dedicated owner doc |
| 条码/PDA 集成 | AureusERP/WMES | `inventory/barcode-integration.md` | ✅ dedicated owner doc |
| 跨境贸易（cross-border） | 国产 ERP | `master-data/cross-border-trade.md` | ✅ dedicated owner doc |
| SKU 多单位多 barcode | 管伊佳 | `master-data/sku-multi-unit.md` | ✅ dedicated owner doc |
| 统一往来单位 | 管伊佳 | `master-data/unified-party-identity.md` | ✅ dedicated owner doc |
| 汇率管理 | iDempiere | `master-data/exchange-rate-management.md` | ✅ dedicated owner doc |
| 资金/票据/承兑 | Metasfresh | `finance/treasury.md` | ✅ dedicated owner doc |
| 银行对账 | Metasfresh/OCA | `finance/bank-reconciliation.md` | ✅ dedicated owner doc |
| 坏账准备（Allowance 法） | ar-close-engine | `finance/bad-debt.md` | ✅ dedicated owner doc |
| 费用报销 + 员工借款 | Odoo/frappe-hrms | `finance/expense-claim.md` | ✅ dedicated owner doc |
| 多账套多科目表 | iDempiere | `finance/multiple-accounting-schemas.md` | ✅ dedicated owner doc |
| 公司间合并抵消 | iDempiere | `finance/intercompany-consolidation.md` | ✅ dedicated owner doc |
| 成本中心 | Metasfresh | `finance/cost-center.md` | ✅ dedicated owner doc |
| 期初余额 | ERPNext | `finance/opening-balance.md` | ✅ dedicated owner doc |
| GL 映射规则 | OFBiz AcctgTrans | `finance/gl-mapping-rules.md` | ✅ dedicated owner doc |
| 项目盈亏（profitability） | OFBiz | `projects/profitability.md` | ✅ dedicated owner doc |
| 任务 DAG | OFBiz/Carbon | `projects/task-dag.md` | ✅ dedicated owner doc |
| 项目成本归集 | OFBiz | `projects/cost-collection.md` | ✅ dedicated owner doc |
| 设备集成（equipment integration） | Atlas CMMS | `maintenance/equipment-integration.md` | ✅ dedicated owner doc |
| 合同审批工作流 | Axelor | `contract/approval-workflow.md` | ✅ dedicated owner doc |
| 合同 e-signature | Axelor | `contract/e-signature.md` | ✅ dedicated owner doc |
| 合同 volume-discount | Axelor | `contract/volume-discount.md` | ✅ dedicated owner doc |
| 合同版本库（repository） | Axelor | `contract/contract-repository.md` | ✅ dedicated owner doc |
| B2B ASN 处理 | Axelor/odoo | `b2b/asn-processing.md` | ✅ dedicated owner doc |
| B2B EDI 格式 | Axelor | `b2b/edi-formats.md` | ✅ dedicated owner doc |
| B2B 文件传输（MFT） | Axelor | `b2b/managed-file-transfer.md` | ✅ dedicated owner doc |
| B2B 伙伴上线 | Axelor | `b2b/partner-onboarding.md` | ✅ dedicated owner doc |
| 物流承运商集成 | Metasfresh 三层 SPI | `logistics/carrier-integration.md` | ✅ dedicated owner doc |
| 物流配送窗口 | Metasfresh | `logistics/delivery-window.md` | ✅ dedicated owner doc |
| HR 薪酬仿真 | Axelor | `human-resource/payroll-simulation.md` | ✅ dedicated owner doc |
| HR 排班调度 | Axelor | `human-resource/shift-scheduling.md` | ✅ dedicated owner doc |
| HR 能力管理 | Axelor talent | `human-resource/competency-management.md` | ✅ dedicated owner doc |
| HR 员工调查 | Axelor | `human-resource/employee-survey.md` | ✅ dedicated owner doc |
| HR 招聘 | Axelor/AureusERP | `human-resource/recruitment.md` | ✅ dedicated owner doc |
| CS SLA 策略 | Axelor helpdesk | `customer-service/sla.md` | ✅ dedicated owner doc |
| CS 权利（entitlement） | Axelor | `customer-service/entitlement.md` | ✅ dedicated owner doc |
| CS 服务目录 | Axelor | `customer-service/service-catalog.md` | ✅ dedicated owner doc |
| CSAT 客户满意度 | Axelor | `customer-service/csat.md` | ✅ dedicated owner doc |
| CS 时间追踪 | Axelor | `customer-service/time-tracking.md` | ✅ dedicated owner doc |
| CS 预设回复 | Axelor | `customer-service/canned-response.md` | ✅ dedicated owner doc |
| DRP 安全库存优化 | Axelor supplychain | `drp/safety-stock-optimization.md` | ✅ dedicated owner doc |
| DRP cross-dock | Axelor | `drp/cross-dock.md` | ✅ dedicated owner doc |
| DRP 提前期跟踪 | Axelor | `drp/lead-time-tracking.md` | ✅ dedicated owner doc |
| 资产折旧过账 | Yu-FAMS | `assets/depreciation-and-posting.md` | ✅ dedicated owner doc |
| 资产盘点 | OFBiz | `assets/inventory.md` | ✅ dedicated owner doc |
| 资产拆分合并 | OFBiz | `assets/split-merge.md` | ✅ dedicated owner doc |
| 资产 CIP 在建工程 | OFBiz | `assets/cip.md` | ✅ dedicated owner doc |
| 资产-维护联动 | OFBiz/Atlas | `assets/maintenance.md` | ✅ dedicated owner doc |
| 多账套/多公司隔离 | iDempiere | `architecture/multi-company.md` | ✅ dedicated owner doc |
| 跨模块依赖矩阵 | — | `architecture/data-dependency-matrix.md` | ✅ dedicated owner doc |

### 1.3 未覆盖项分级（功能广度边界）

`feature-coverage-matrix.md §未覆盖项清单`（5 项 + 3 已覆盖/排除）逐项分级（与 A3.1 §1.3 一致，本扫描复核确认）：

| 功能 | 状态 | 分级 | 说明 |
|------|------|------|------|
| 现场服务（Intervention） | 🕒 暂缓 | **note**（建议显式声明产品基线外） | 与 maintenance/cs 重叠，有承接域 owner doc |
| POS 零售 | 🕒 待调研 | **note** | 随零售客户触发；非 ERP 核心标配 |
| 移动端 | 🔵 平台能力 | **note** | Nop Platform 原生支持 |
| GDPR 合规 | 🔵 排除 | **note** | 非中国市场需求，已显式排除 |
| 敏捷 Scrum | 🔵 排除 | **note** | 非 ERP 核心，已显式排除 |
| 条码/PDA | ✅ 已设计 | — | `inventory/barcode-integration.md` |
| 电商/网站 | ✅ nop-app-mall | — | 配套产品 |
| 客户/供应商门户 | ✅ 已设计 | — | `portal/README.md`（future extension placeholder） |

### 1.4 功能深度新增观察（minor）

| 标配能力 | 来源 | 当前覆盖 | 分级 |
|---------|------|---------|------|
| Subscription / recurring billing（订阅周期计费） | Odoo `subscription` | `contract/README.md` InvoicePlan 周期开票覆盖订阅式计费语义，但无 dedicated `contract/subscription-billing.md` 拆解续费/续约/到期续费/中途升级退款等订阅专用规则 | **minor**（功能广度通过，深度未拆解；按需补建） |
| AcctgTrans 统一入账枢纽（服务 ECA 自动过账） | OFBiz Framework | `finance/posting.md` IErpFinAcctDocProvider 已是等价统一枢纽（Provider 注册 + businessType 路由），机制对齐 | **note**（机制已等价，无需补建） |

### 1.5 维度 0 结论

**PASS**：113/118 = 95.8% 总覆盖率经第二轮纵深核查有效；71 个开源标配差异化功能逐项均有对应 `docs/design/` owner doc；5 未覆盖项均显式分级（暂缓/待调研/排除/平台能力）；无核心标配功能在设计中完全缺失且无显式「产品基线外」声明。1 项 minor（subscription billing 深度未拆解， InvoicePlan 已覆盖广度）。

---

## 2. 维度 1 — 域覆盖

### 2.1 设计 vs 范围对照

`product-scope.md` 声明 18 业务域 + 跨域通知派发子系统（共 19 个 `module-*/`）。逐域核验：

| 域（product-scope 声明） | `docs/design/<domain>/` | README | 状态 |
|------|------|------|------|
| master-data（核心） | ✅ `master-data/` | ✅ | 设计所有者齐备 |
| inventory（核心） | ✅ `inventory/` | ✅ | 设计所有者齐备 |
| purchase（核心） | ✅ `purchase/` | ✅ | 设计所有者齐备 |
| sales（核心） | ✅ `sales/` | ✅ | 设计所有者齐备 |
| finance（核心） | ✅ `finance/` | ✅ | 设计所有者齐备 |
| assets（第一批扩展） | ✅ `assets/` | ✅ | 设计所有者齐备 |
| projects（第一批扩展） | ✅ `projects/` | ✅ | 设计所有者齐备 |
| manufacturing（第一批扩展） | ✅ `manufacturing/` | ✅ | 设计所有者齐备 |
| quality（第一批扩展） | ✅ `quality/` | ✅ | 设计所有者齐备 |
| maintenance（第一批扩展） | ✅ `maintenance/` | ✅ | 设计所有者齐备 |
| crm（第二批扩展） | ✅ `crm/` | ✅ | 设计所有者齐备 |
| customer-service（第二批扩展） | ✅ `customer-service/` | ✅ | 设计所有者齐备 |
| human-resource（第二批扩展） | ✅ `human-resource/` | ✅ | 设计所有者齐备 |
| aps（第二批扩展） | ✅ `aps/` | ✅ | 设计所有者齐备 |
| contract（第二批扩展） | ✅ `contract/` | ✅ | 设计所有者齐备 |
| drp（第二批扩展） | ✅ `drp/` | ✅ | 设计所有者齐备 |
| logistics（第二批扩展） | ✅ `logistics/` | ✅ | 设计所有者齐备 |
| b2b（第二批扩展） | ✅ `b2b/` | ✅ | 设计所有者齐备 |
| notify（跨域通知派发，AGENTS.md 声明） | ✅ `notify/` | ✅ | 设计所有者齐备 |

### 2.2 范围外但已设计

| 域 | 状态 | 说明 |
|----|------|------|
| `portal/` | future extension placeholder | `portal/README.md` STATUS 横幅明确标注非当前基线；范围外但显式标记 deferred，非设计蔓延 |
| `l10n/` | 跨切面子系统 | 仅 `cn-golden-tax.md`（中国本地化金税）；非独立域，作为 master-data/finance 的本地化扩展 |

### 2.3 维度 1 结论

**PASS**：18 product-scope 域 + notify 跨域子系统全部有 `docs/design/<domain>/` 设计所有者；无范围要求域缺设计；无代码/待办存在但无设计文档的「实现无设计真相」情况（无需升级交接 A4）；portal future extension 显式标记。

---

## 3. 维度 2 — 每域内文档覆盖

### 3.1 文档结构矩阵

| 域 | README | state-machine | cross-domain | use-cases | ui-patterns | 域专属文档 |
|----|----|----|----|----|----|----|
| master-data | ✅ | N/A（启停二态，规则内嵌 README） | — | ✅ | ✅ | cross-border-trade / unified-party-identity / sku-multi-unit / exchange-rate-management / data-migration |
| inventory | ✅ | ✅（19 状态字段，A2.11 已审） | ✅ | ✅ | ✅ | barcode-integration / trace-chain / consignment |
| purchase | ✅ | ✅（29 状态字段，A2.8 已审） | —（路由 flow-overview） | ✅ | ✅ | three-way-match / returns / requisition / supplier-evaluation |
| sales | ✅ | ✅（25 状态字段，A2.9 已审） | — | ✅ | ✅ | returns / contract / quotation |
| finance | ✅ | ✅（24 状态字段，A2.5a/b/c 已审） | —（跨域规则在 posting/period-close） | ✅ | ✅ | posting / posting-log / period-close / ar-ap-reconciliation / bad-debt / bank-reconciliation / budget / costing-methods / cost-center / expense-claim / gl-mapping-rules / intercompany-consolidation / multiple-accounting-schemas / opening-balance / treasury（15 域专属） |
| assets | ✅ | ✅（18 状态字段，A2.10 已审） | — | ✅ | ✅ | depreciation-and-posting / inventory / maintenance / split-merge / cip |
| projects | ✅ | ✅（16 状态字段，A2.13 已审） | — | ✅ | ✅ | cost-collection / profitability / task-dag |
| manufacturing | ✅ | ✅（11 状态字段，A2.6a/b 已审） | — | ✅ | ✅ | bom-and-routing / crp / material-reservation / mrp / simulation-engine / subcontracting / variance-analysis / batch-genealogy |
| quality | ✅ | ✅（16 状态字段，A2.12 已审） | — | ✅ | ✅ | inspection-integration / recall / spc |
| maintenance | ✅ | ✅（6 状态字段，A2.14 已审） | — | ✅ | ✅ | equipment-integration |
| crm | ✅ | ✅（A2.14 已审） | — | ✅ | ✅ | lead-scoring / lead-waterfall / sales-forecast / sales-sequence / marketing / territory / cpq |
| customer-service | ✅ | ✅（A2.14 已审） | — | ✅ | ✅ | sla / entitlement / service-catalog / csat / time-tracking / canned-response |
| human-resource | ✅ | ✅（A2.7a/b 已审） | — | ✅ | ✅ | payroll / payroll-simulation / shift-scheduling / competency-management / employee-survey / recruitment |
| aps | ✅ | ✅（A2.15 已审） | — | ✅ | ✅ | scheduling / auto-dispatch / alternative-routing |
| contract | ✅ | ✅（A2.14 已审） | — | ✅ | ✅ | approval-workflow / e-signature / contract-repository / volume-discount |
| drp | ✅ | ✅（A2.14 已审） | — | ✅ | ✅ | safety-stock-optimization / cross-dock / lead-time-tracking |
| logistics | ✅ | ✅（A2.15 已审） | — | ✅ | ✅ | carrier-integration / delivery-window |
| b2b | ✅ | ✅（A2.14 已审） | —（业务语义）+ 辅助 `architecture/b2b-integration.md`（集成契约） | ✅ | ✅ | asn-processing / edi-formats / managed-file-transfer / partner-onboarding |
| notify | ✅ | N/A（派发子系统，无独立状态机） | — | — | — | inbox-patterns |

### 3.2 跨域耦合密集域 cross-domain.md 核验

`docs/design/README.md` 设计选择：仅 inventory 有独立 `cross-domain.md`（"跨域协作复杂的域才有独立 cross-domain.md，如 inventory"）。其他跨域耦合密集域（finance 49 跨域 daoFor / manufacturing 47 / purchase 34 / sales 28）通过**等效专属文档**承载跨域协作规则——finance 的跨域规则在 `posting.md`（业财 Provider 注册机制）+ `period-close.md`（期间结账跨域 command 编排）+ `ar-ap-reconciliation.md`（跨域核销）；manufacturing 在 `subcontracting.md` + `material-reservation.md`；purchase 在 `three-way-match.md` + `returns.md`；sales 在 `returns.md`。维度 2 prompt 接受"等效文件"。**结论：通过**。

### 3.3 维度 2 结论

**PASS**：18 域 + notify 全部有 README.md；17 个状态机重域有 state-machine.md（master-data 是启停二态非状态机，按设计规则内嵌 README，正确）；inventory 是唯一有独立 cross-domain.md 的域（其他跨域密集域通过等效专属文档承载，符合 README 设计选择）；每域有 use-cases.md + ui-patterns.md；域专属文档覆盖充分。

---

## 4. 维度 3 — 每文档内功能点覆盖（结构存在性核验）

### 4.1 核心业务对象业务含义

每域 README 抽样（master-data/inventory/purchase/sales/finance/assets/projects/manufacturing/quality/maintenance/crm/cs/hr/aps/contract/drp/logistics/b2b）首章均含「核心业务对象」表或对应散文段。A3.1 §1 已做交叉验证（功能覆盖度 PASS）。

### 4.2 状态机 10 审查维度结构

抽样 `crm/state-machine.md`（第二批扩展域代表）：文档首行明确引用 `docs/skills/state-machine-business-review-prompt.md`（"本状态机按 ... 的 10 个审查维度组织。审查本状态机时使用该提示词"）；§1 状态定义 / §2 迁移完整性（含触发人/前置/结果）/ §3-§10 各维度章节齐全。

> **范围声明**：维度 3 只核验状态机文档**结构存在**（10 维度章节是否齐全 + 是否引用审查提示），不重做 MA2 正确性裁决（迁移完整性/守卫/并发/可达性正确性归 MA2 A2.5-A2.15 已 done）。本扫描确认：18 域状态机文档（17 个独立 + 1 个 inventory 内嵌 README）结构上**全部**遵循 10 维度组织。

### 4.3 跨域流程描述或路由

每域 README 的「跨域协作」/「业务规则」节描述或路由到 flow-overview.md / 域专属跨域文档。无孤立功能。

### 4.4 保护区域行为

| 保护区域 | owner doc | 状态 |
|---------|-----------|------|
| 支付（payment） | `finance/ar-ap-reconciliation.md` + `purchase/README.md` | ✅ 定义 |
| 退款（refund） | `purchase/returns.md` + `sales/returns.md` | ✅ 定义 |
| 数据删除 | N/A（项目策略：useLogicalDelete 软删除，无 hard delete 业务路径） | ✅ 路由到平台机制 |
| 会计过账 | `finance/posting.md` + `finance/posting-log.md` | ✅ 定义 |
| 权限更改 | `roles-and-permissions.md §高危操作权限` | ✅ 定义（A3.1 P1-MA3-012 指出 4 处重复，但都有定义） |

### 4.5 维度 3 结论

**PASS**：核心业务对象业务含义齐备；状态机 10 维度结构全覆盖；跨域流程描述或路由齐备；保护区域行为定义或路由齐备。

---

## 5. 维度 4 — 跨域流程覆盖

### 5.1 L1 宏观流程

`flow-overview.md §2` 核心业务流程覆盖：
- §2.1 采购到付款（P2P）
- §2.2 销售到收款（O2C）
- §2.3 生产流程
- §2.4 库存管理流程
- §2.5 委外加工流程
- §2.6 生产异常处理流程
- §2.7 两步调拨流程（在途场景）
- §4.4 多套科目表并行

L1 覆盖了产品范围要求的核心端到端业务路径。**PASS**。

### 5.2 L2 状态机映射 — **GAP（finding P1-MA3-022）**

`flow-overview.md §3 状态映射总览` 实际覆盖：
- §3.1 审批状态映射（approveStatus）：仅 8 类采购/销售单据
- §3.2 业务生命周期映射（docStatus）：核心 5 域 + assets 折旧计划
- §3.3 制造域状态映射：manufacturing 工单

**缺口**：§3 仅引用核心 5 + manufacturing 共 6 域状态机。**缺其余 12 个扩展域状态机引用**：
- 第一批扩展（5）：assets / projects / quality / maintenance 状态机
- 第二批扩展（8）：crm / customer-service / human-resource / aps / contract / drp / logistics / b2b 状态机

维度 4 prompt 要求"L2 状态机映射是否引用所有存在的域状态机"。当前 §3 是核心域偏重的状态映射目录，未引用扩展域。`domain-glossary.md §域专属状态` 节有 5 域状态指针（purchase/sales/inventory/finance/assets/manufacturing/quality/maintenance），但仍缺 projects/crm/cs/hr/aps/contract/drp/logistics/b2b 共 9 域指针——与本 finding 同根因（扩展域状态机未纳入全局状态视图）。

**不破坏运行时正确性**——各域状态机自身完整（MA2 A2.5-A2.15 已审），flow-overview §3 是全局视图聚合，缺引用是文档导航/可见性缺口非状态机正确性缺口。

### 5.3 L3 跨域规则

`flow-overview.md §4 业财打通详细流程` 覆盖：
- §4.1 凭证生成机制（PostingEvent → Provider → Voucher）
- §4.2 科目映射解析（specific→generic 优先级）
- §4.3 多币种处理（amountSource/exchangeRate/amountFunctional）
- §4.4 多套科目表并行
- §5 异常处理与恢复（过账失败/冲销/反审核）
- §6 数据一致性保障（事务边界/兜底/对账）

L3 跨域规则覆盖了"过账触发/库存可用性/快照语义/多货币/对账"五类核心规则。**PASS**。

### 5.4 维度 4 结论

**GAPS（1 MAJOR）**：L1 宏流程 PASS / L2 状态机映射 GAP（P1-MA3-022，缺 12 扩展域状态机引用）/ L3 跨域规则 PASS。

---

## 6. 维度 5 — 术语表与角色覆盖

### 6.1 术语表覆盖 — **GAP（finding P1-MA3-023）**

`domain-glossary.md` 实际章节：
- ✅ 组织与主数据词汇（master-data）
- ✅ 库存词汇（inventory）
- ✅ 采购与销售词汇（purchase/sales）
- ✅ 财务词汇（finance）
- ✅ 通用单据状态词汇（跨域通用）
- ✅ 域专属状态（非通用，仅指针）——含 purchase/sales/inventory/finance/assets/manufacturing/quality/maintenance 8 域状态指针
- ✅ 资产/项目/制造/质量/维护词汇（5 第一批扩展域）

**缺口**：缺 8 第二批扩展域（crm/cs/hr/aps/contract/drp/logistics/b2b）专属词汇节。第二批扩展域引入大量新术语——CRM Lead/Opportunity/Stage/Funnel/Scoring/CPQ；CS Ticket/SLA/Entitlement；HR Employee/Payroll/LeaveRequest/Recruitment；APS OperationOrder/Schedule；Contract InvoicePlan/ConsumptionLine；DRP NetRequirement/ReorderPoint；Logistics Shipment/Carrier SPI；B2B EDI/ASN——均无术语表统一中文译法与歧义消解。第二批扩展域 README 自行约定术语，跨域对照易漂移。

### 6.2 角色覆盖

`roles-and-permissions.md §角色体系` 实际覆盖：
- ✅ 核心业务角色（采购员/销售员/库管员/财务员）
- ✅ 扩展业务角色（资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员）——第一批扩展 5 域
- ✅ 审核与管理角色（审核人/管理员）

**缺口**：缺 8 第二批扩展域角色映射——**A3.1 P1-MA3-010 已登记**（"8 第二批扩展域无角色/权限基线，dim 9"）。本扫描确认 A3.1 发现并交叉引用，不重复登记。

### 6.3 维度 5 结论

**GAPS（1 新 MAJOR + 1 交叉引用 A3.1）**：
- P1-MA3-023（dim 5 术语表）：`domain-glossary.md` 缺 8 第二批扩展域专属词汇节
- 交叉引用 A3.1 P1-MA3-010（dim 9 角色）：`roles-and-permissions.md` 缺 8 第二批扩展域角色基线

8 第二批扩展域同时缺术语表 + 角色基线，是系统性"扩展域未纳入全局视图"模式在 dim 5 + dim 9 双侧投影。与 P1-MA3-022（flow-overview L2 状态映射）+ A3.1 P1-MA3-009（app-overview/guidelines/dashboards 导航遗漏）同根因。

---

## 7. 维度 6 — 与范围一致性

### 7.1 设计域集 vs product-scope.md

设计的 18 域 + portal(future) + notify + l10n ↔ `product-scope.md §业务域范围` 声明 18 域：**完全对齐**。无范围隐含功能无设计所有者，无设计描述范围外行为（portal 已显式 future extension 标记）。

### 7.2 范围-设计不匹配

A3.1 P1-MA3-011 已识别：`product-scope.md §当前里程碑`（line 50-73）描述状态为「codegen skeleton done / 下一步编写核心 BizModel」，与 AGENTS.md + 设计文档反映的「业务逻辑深化与运营成熟度收尾」（M1-M5 全 done）冲突。本扫描确认 A3.1 发现并交叉引用，不重复登记。

### 7.3 维度 6 结论

**PASS**（设计-范围双向对齐；里程碑陈旧问题交叉引用 A3.1 P1-MA3-011）。

---

## 8. finding 清单

### 8.1 P1（MAJOR，目标 MR2，文档类）

| Finding ID | 维度 | 受影响区域 | 严重性 | 差距描述 | 重要性 | 建议操作 | 建议文档路径 | 目标 MR |
|------|------|------|------|------|------|------|------|------|
| `P1-MA3-022` | dim 4 跨域流程覆盖 | `docs/design/flow-overview.md §3` + 12 扩展域状态机 | major | flow-overview §3 状态映射总览仅引用核心 5 + manufacturing 共 6 域状态机，缺 12 扩展域状态机引用（assets/projects/quality/maintenance + crm/cs/hr/aps/contract/drp/logistics/b2b）。L2 状态机映射不完整，扩展域状态机未纳入全局状态视图。各域状态机自身完整（MA2 已审），此为文档导航/可见性缺口非正确性缺口 | 全局状态视图不完整，审查者/实施方无法在一处查阅全部 18 域状态机；扩展域状态变更不易被跨域影响分析捕获 | 扩展 `flow-overview.md §3` 增加"扩展域状态映射"小节（结构化指针表，每域 1-3 行核心状态摘要 + 跳转各域 state-machine.md 链接），不重复状态字面值（按 A3.1 P1-MA3-013 单一真相源规则引用 `domain-design-guidelines.md §16.2`） | `docs/design/flow-overview.md §3`（扩展）| MR2 |
| `P1-MA3-023` | dim 5 术语表覆盖 | `docs/design/domain-glossary.md` + 8 第二批扩展域 | major | domain-glossary.md 缺 8 第二批扩展域（crm/cs/hr/aps/contract/drp/logistics/b2b）专属词汇节。第二批扩展域引入大量新术语（CRM Lead/Opportunity/Stage/Funnel；CS Ticket/SLA；HR Employee/Payroll；APS OperationOrder；Contract InvoicePlan；DRP NetRequirement；Logistics Shipment；B2B EDI/ASN）无统一中文译法与歧义消解 | 8 域 README 自行约定术语，跨域对照易漂移；新增域文档时术语表机制失效 | 在 `domain-glossary.md` 增加 8 第二批扩展域专属词汇节（每域 5-10 个核心术语 + 中文标准译法 + 所属域 + 一句话说明），保持与 `domain-design-guidelines.md` 域归属映射一致；可由各第二批扩展域 README 反向提炼核心术语 | `docs/design/domain-glossary.md`（新增 8 节）| MR2 |

### 8.2 P2（MINOR，watch-only）

| Finding ID | 维度 | 受影响区域 | 严重性 | 差距描述 | 建议操作 |
|------|------|------|------|------|------|
| `P2-MA3-022` | dim 0 功能深度 | `docs/design/contract/` | minor | Odoo `subscription` 模块标配的订阅专用规则（续费/续约/到期续费/中途升级退款/proration）未在 contract 域拆解为 dedicated owner doc；当前由 `contract/README.md` InvoicePlan 周期开票覆盖广度。功能广度通过，订阅深度未独立拆解 | 按需（subscription 客户触发时）新建 `contract/subscription-billing.md`，拆解订阅专用规则；非当前基线必需 |

> Finding ID 复用说明：`P2-MA3-014~021` 已由 A3.1 占用（8 项 P2 watch-only），本扫描新增 P2 从 `P2-MA3-022` 起编号。

### 8.3 NOTE（观察项）

| 观察 | 维度 | 说明 |
|------|------|------|
| 现场服务（Intervention）暂缓 | dim 0 | 与 maintenance/cs 重叠，建议显式声明产品基线外（同 A3.1 §1.3） |
| POS 零售待调研 | dim 0 | 随零售客户触发；非 ERP 核心标配（同 A3.1 §1.3） |
| 移动端 / GDPR / Scrum 排除或平台能力 | dim 0 | 已显式分级（同 A3.1 §1.3） |
| AcctgTrans 统一入账枢纽（OFBiz 服务 ECA） | dim 0 | `finance/posting.md` IErpFinAcctDocProvider 已等价覆盖，无需补建 |
| 8 第二批扩展域术语+角色+导航三联缺位 | dim 4+5+9 | 系统性"扩展域未纳入全局视图"模式（本扫描 P1-MA3-022/023 + A3.1 P1-MA3-009/010 同根因）；MR2 应**协同修复**（一并扩展 flow-overview §3 + domain-glossary + roles-and-permissions + app-overview + dashboards + domain-design-guidelines §1.1，避免分散修复漂移） |

### 8.4 与 A3.1 的 finding 协同矩阵

| 维度 | A3.1（质量层） | A3.2（完整性层） | 协同关系 |
|------|------|------|------|
| dim 2 产品基线 | P1-MA3-006（占位/scaffold 泄漏） | — | A3.1 独有 |
| dim 3 稳定与时间敏感 | P1-MA3-001（系统性实现状态泄漏）+ P1-MA3-007（dashboards 三联） | — | A3.1 独有 |
| dim 4 需求对齐 | P1-MA3-011（product-scope 陈旧） | 交叉引用 A3.1 | A3.1 主登记 |
| dim 4 跨域流程 | — | **P1-MA3-022**（flow-overview §3 状态映射缺扩展域） | A3.2 独有 |
| dim 5 owner-doc 边界 | P1-MA3-002/003/004/005 | — | A3.1 独有（已写文档的边界质量） |
| dim 5 术语表 | — | **P1-MA3-023**（domain-glossary 缺扩展域词汇） | A3.2 独有（该写未写） |
| dim 6 跨设计一致 | P1-MA3-008（admin 角色冲突）+ P1-MA3-009（导航遗漏）+ P1-MA3-013（状态码目录重复） | 交叉引用 A3.1 P1-MA3-009 | dim 6 导航遗漏 A3.1 主登记；A3.2 §3 状态映射是同模式具体投影 |
| dim 9 角色权限 | P1-MA3-010（8 域无角色基线） | 交叉引用 A3.1 | A3.1 主登记；A3.2 §6.2 确认 |
| dim 12 维护成本 | P1-MA3-012（危险操作审计重复） | — | A3.1 独有 |

**去重结论**：A3.2 新增 2 项独立 P1（P1-MA3-022/023）+ 1 项独立 P2（P2-MA3-022），与 A3.1 13 项 P1 + 8 项 P2 经交叉去重无重复登记。MA3 累计 P1 = 15（A3.1 13 + A3.2 2），P2 = 9（A3.1 8 + A3.2 1）。

---

## 9. 建议的下一轮文档添加优先级列表（驱动 MR2）

> 排序按严重性 + 修复成本 + 协同效应。MR2 展开时按此优先级转化为具体补建工作项行。

| 优先级 | 工作项 | 来源 finding | 类型 | 建议路径 | 协同 |
|------|------|------|------|------|------|
| **P1-HIGH** | flow-overview §3 扩展域状态映射扩展 | P1-MA3-022 | 扩展现有文档 | `docs/design/flow-overview.md §3`（增加扩展域状态指针表） | 与 P1-MA3-009/010/023 协同 |
| **P1-HIGH** | domain-glossary 8 扩展域专属词汇节 | P1-MA3-023 | 扩展现有文档 | `docs/design/domain-glossary.md`（新增 8 节） | 与 P1-MA3-009/010/022 协同 |
| P2-MED | subscription billing 专用规则拆解 | P2-MA3-022 | 新建文档（按需） | `docs/design/contract/subscription-billing.md`（subscription 客户触发时） | 独立 |
| NOTE | 现场服务产品基线外显式声明 | NOTE | 扩展 product-scope | `docs/requirements/product-scope.md`（加 deferred 声明） | 与 P1-MA3-011 协同 |

---

## 10. 覆盖摘要

### 10.1 域覆盖摘要

- **设计域**：18（核心 5 + 第一批扩展 5 + 第二批扩展 8）+ notify 跨域 + portal(future) + l10n 跨切面
- **范围要求域**：18（product-scope 声明）+ notify（AGENTS.md 声明）
- **推迟域**：portal（future extension placeholder，显式标记）
- **范围-设计不匹配**：0 域（里程碑陈旧归 A3.1 P1-MA3-011 需求文档维护）

### 10.2 每域文档覆盖摘要

每域文档结构因域复杂性而异（per `docs/design/README.md` 设计选择）：
- **README.md**：18/18 域 + notify 全覆盖
- **state-machine.md**：17/18 域（master-data 启停二态非状态机，按设计规则内嵌 README）
- **cross-domain.md**：仅 inventory（其他跨域密集域通过等效专属文档承载）
- **use-cases.md**：18/18 域
- **ui-patterns.md**：18/18 域
- **域专属文档**：覆盖充分（finance 15 份 / manufacturing 8 份 / crm 7 份 / cs 6 份 / hr 6 份 / b2b 4 份 / contract 4 份 / purchase 4 份 / drp 3 份 / assets 5 份 / projects 3 份 / aps 3 份 / inventory 3 份 / quality 3 份 / master-data 5 份 / sales 3 份 / logistics 2 份 / maintenance 1 份）

### 10.3 状态机覆盖摘要

17 个 state-machine.md 全部按 `state-machine-business-review-prompt.md` 10 审查维度组织（首行引用提示词 + 10 维度章节齐全）；MA2 A2.5-A2.15 已完成正确性裁决（全部 done，详见各域 arm 报告）。本扫描核验结构存在性 PASS。

### 10.4 跨域流程覆盖摘要

- L1 宏观流程：8 个端到端流程（P2P / O2C / 生产 / 库存 / 委外 / 异常 / 调拨 / 多科目表）——PASS
- L2 状态机映射：核心 5 + manufacturing 共 6 域 ——**GAP**（P1-MA3-022 缺 12 扩展域）
- L3 跨域规则：业财打通 4 节 + 异常恢复 + 数据一致性 ——PASS

### 10.5 术语表角色覆盖摘要

- domain-glossary.md：5 全局节（master-data/inventory/purchase-sales/finance/通用状态）+ 域专属状态指针 8 域 + 5 第一批扩展域词汇节——**GAP**（P1-MA3-023 缺 8 第二批扩展域词汇节）
- roles-and-permissions.md：核心 4 角色 + 第一批扩展 9 角色 + 审核/管理 2 角色——**GAP**（A3.1 P1-MA3-010 缺 8 第二批扩展域角色）

---

## 11. 剩余风险与跳过区域

### 11.1 剩余风险

1. **8 第二批扩展域"全局视图"系统性缺位（dim 4+5+6+9 四联）**：本扫描 P1-MA3-022/023 + A3.1 P1-MA3-009/010 同根因。MR2 修复时应**协同扩展** 6 份全局文档（flow-overview §3 + domain-glossary + roles-and-permissions + app-overview + dashboards + domain-design-guidelines §1.1），避免分散修复漂移。当前不破坏运行时正确性（各域自身设计完整），但全仓可查阅性/可审查性受损。
2. **subscription billing 深度未拆解**：当前 InvoicePlan 覆盖广度，订阅专用规则（续费/退订/proration）深度归 successor；subscription 客户触发时按需补建（P2-MA3-022）。
3. **现场服务暂缓**：与 maintenance/cs 重叠；建议 product-scope 显式声明 deferred（NOTE）。

### 11.2 跳过区域

- **MA2 已收口的正确性裁决**：状态机迁移完整性/守卫/并发/可达性正确性归 MA2 A2.5-A2.15（全部 done）。本扫描维度 3 仅核验状态机**结构存在**（10 维度章节 + 提示词引用），不重做正确性裁决。
- **A3.1 已审的文档质量**：A3.1 `design-doc-audit-prompt.md` 12 维度 + 维度 1 功能覆盖度外部基准已审存在的内容质量。本扫描维度 0 复用 A3.1 §1 功能覆盖度结论 + 第二轮纵深核查（71 标配功能逐项），不重做文档质量裁决。
- **A3.3-A3.5 owner doc vs 代码 drift**：归 A3.3-A3.5（todo）。本扫描核验设计**覆盖广度**（哪些功能从未设计），不做设计 vs Java/ORM 逐字段比对。
- **A3.6 API 契约一致性 / A3.7 索引路由 / A3.8 可定制性**：归后续工作项（todo）。

---

## 12. 与 plan Exit Criteria 对照

| Exit Criteria | 满足 | 证据 |
|------|------|------|
| 7 维度逐项扫描结果产出（每维度至少一句结论，含"本维度无缺口"） | ✅ | §1-§7 每维度独立结论 |
| 域覆盖摘要 + 状态机覆盖摘要 + 跨域流程覆盖摘要 + 术语表角色覆盖摘要产出 | ✅ | §10.1-§10.5 |
| blocker/major/minor/note finding 清单产出，每个含严重性/维度/受影响区域/差距描述/建议操作/建议文档路径 | ✅ | §8.1（P1 2 项）/§8.2（P2 1 项）/§8.3（NOTE 5 项），每项含完整字段 |
| 建议的下一轮文档添加优先级列表产出（驱动 MR2 补建） | ✅ | §9 优先级表 |

## 13. 范围内行为完成声明

- ✅ 7 维度扫描完成，2 项新 P1（P1-MA3-022/023）+ 1 项新 P2（P2-MA3-022）登记
- ✅ 域覆盖/状态机覆盖/跨域流程覆盖/术语表角色覆盖四摘要产出
- ✅ finding 清单 + 建议下一轮文档添加优先级列表产出
- ✅ 与 A3.1 13 项 P1 + 8 项 P2 经交叉去重，无重复登记；MA3 累计 P1=15 / P2=9
- ✅ 无 P0（与 plan「文档层审计原则上无 P0」一致）
- ✅ 无范围要求域已有代码但零设计的「实现无设计真相」情况需升级交接 A4
- ✅ Verdict: GAPS（有差距，2 项 MAJOR 均目标 MR2 文档类补建）
