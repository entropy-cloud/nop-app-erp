---
分析日期: 2026-06-30
类型: 高级业务场景缺口分析（对照 erp-survey）
状态: 已完成（经独立子代理两轮审查修正）
对照基准:
  - docs/analysis/erp-survey/ 下 18 份调研（13 + 横向分析 + 子域覆盖）
  - docs/design/ 全部设计文档（10 域 + 横切）
  - docs/architecture/ 关键架构文档
  - **各域 `model/*.orm.xml`（持久化实体存在性的权威真相源）**
前置审计（含本报告不可矛盾的前序结论）:
  - 2026-06-23-0000-design-doc-comprehensive-audit.md（核心闭环完备）
  - 2026-06-23-0001-business-flow-coverage-audit.md（业务流程覆盖 100%）
  - 2026-06-23-0003-menu-and-feature-completeness.md（10 域 51 分组 105 菜单项功能完整，**含采购寻源 RFQ/报价/价格清单已覆盖结论**）
  - 2026-06-23-0004-configuration-comparison.md（配置机制对照）
  - docs/architecture/competitive-comparison.md（8 杠杆已论证）
---

# 高级业务场景缺口分析：对照 erp-survey 13 个开源 ERP

> **本报告定位**：前序四份审计（含 0003 菜单功能完整性）已确认**核心进销存+财务+制造+资产+质量+维护+项目**的设计与实体完备性。本报告**不重复**核心闭环审计，而是聚焦于 erp-survey 中存在、但本项目设计中**未深入展开**的**高级/边缘业务场景**，识别真正的功能盲区与扩展机会。
>
> **方法学（关键）**：对每条候选场景，做三步验证——① erp-survey 中是否有可溯源的证据（**每条证据必须能指回 `erp-survey/*.md` 的 file:line；不能指回的标注"领域常识/非调研来源"**）；② **本项目覆盖以 `<domain>/model/*.orm.xml` 实体存在性为权威**（design/*.md 描述仅为辅证，避免"md 未提=不存在"的误判）；③ 是否属于 `docs/requirements/product-scope.md` 声明范围。

## TL;DR

本项目**核心 ERP 闭环设计已达到并多处超越主流开源 ERP**（见前序四份审计）。对照 erp-survey，识别出 **13 个**设计中**未深入展开**的高级场景：

- **P0（2 个，建议纳入产品基线扩展）**：费用报销/员工借款（**与 projects 文档存在内部不一致**）、资金管理/票据。
- **P1（7 个，按行业/客户需求触发）**：CRP/APS（**项目自承认延迟项**）、VMI/寄售（调研证据弱，已如实标注）、CRM、供应商评分卡/AVL、运输/TMS、EDI/ASN、批次召回事件。
- **P2（4 个，明确不纳入核心）**：DRP、HRMS/薪酬、POS/电商、售后服务（另有库存 ABC 分类作为 P2 备选记录）。

所有缺口**均不影响当前核心进销存+财务一体化基线**，属于产品范围（scope）决策，非设计缺陷。

---

## 一、对照基准与覆盖判断方法

### 1.1 调研对象（18 份报告，详见 `erp-survey/2026-06-22-0000-survey-index.md`）

| 分类 | 项目数 | 代表 |
|---|---|---|
| 国际开源 ERP | 7 | Odoo、ERPNext、Metasfresh、iDempiere、Dolibarr、Tryton、OCA/l10n-china |
| 国产开源 ERP | 5 | 管伊佳、赤龙、星云、若依 v1/v2 |
| MES 专项 | 1 | WMES |
| 子域补充 | 7 | OFBiz、Carbon、Yu-FAMS、Atlas CMMS、MES-SpringBoot 等 |

### 1.2 本项目已覆盖范围（前序审计结论，本报告不复述、不矛盾）

下列场景在前序审计中已确认**实体存在 + 设计深入**，本报告**不再列为缺口**（核实依据：ORM 实体存在性 + design owner doc）：

- 业财一体过账（SPI Provider + posted 兜底 + FactsValidator）
- 凭证三件套 + 模板 + 业财回链
- 库存三层 + 追溯链 + 批次/效期/FEFO + 序列号
- 三单匹配 + 数量/价格容差
- AR/AP open-item 核销 + 汇兑损益 + 期末外币重估（`domain-design-guidelines.md §十二`）
- 多公司/多币种/多账套/多组织
- 成本核算 7 法 + Landed Cost 分摊 + 联副产品分摊规则
- 预算管理（PostingType=BUDGET/COMMITMENT）
- 银行对账（余额调节表 + 未达账项）
- 多公司合并抵消
- 期间关账/反结账/年度结转
- 信用额度三级控制、销售合同
- **采购寻源链（请购→询价→报价→订单，含供应商价格清单）**——`ErpPurRequisition`/`ErpPurRfq`/`ErpPurQuotation`/`ErpPurSupplierPriceList` 均已存在且已建模多供应商比价（`ErpPurRfq.quotations` to-many），见 `0003-menu-audit.md` §2.2 与 `module-purchase/model`。
- MRP + 生产版本 + BOM 快照 + 委外 + 工时成本归集
- 跨法人调拨/内部交易凭证
- 金税接口（l10n-cn 独立模块）
- 三轴状态 + 声明式状态机
- 质量检验/NCR/CAPA/让步/SPC
- 资产折旧/资本化/处置/拆并/价值调整
- 设备维护/预防维护/OEE/停机

> **特别说明**：本次审查纠正了初稿中将"采购 RFQ/报价"误判为缺口的错误——上述实体早已存在。真实子缺口仅剩"供应商评分卡 + AVL 准入流程"，见 §四.4。

---

## 二、缺口判定矩阵

下表为本报告识别的 **13 个候选缺口**，按"业务深度+调研证据强度（可溯源）+本项目 ORM/设计验证"三维评定优先级。

| # | 高级业务场景 | erp-survey 证据（可溯源） | 本项目覆盖（ORM/design 核查） | 优先级 |
|---|---|---|---|---|
| 1 | 费用报销 / 员工借款 / 备用金 | 领域常识（ERPNext `expense_claim` / Odoo `hr_expense` / iDempiere 均有，**erp-survey 未单列实测**） | ORM 无报销实体；`projects/use-cases.md:57`、`cost-collection.md:37,119,153,165` 引用但无 owner | **P0** |
| 2 | 资金管理 / 票据（承兑汇票/贴现/授信） | Metasfresh banking provider（`metasfresh.md:83` BankingAcctDocProvider）+ 赤龙银行三件套字段（`redragon-erp.md:41`） | 仅 `ErpFinFundAccount` + 银行对账；ORM 无票据/授信/融资实体 | **P0** |
| 3 | CRP / APS（产能计划与车间排产） | 调研 matrix（`survey-index.md:93-99` 排产列：Odoo/ERPNext/iDempiere/Metasfresh 有）+ **项目自承认延迟**（`manufacturing/mrp.md:10` 明文"不负责 CRP"） | ORM 无产能/排产实体；MRP 仅解物料需求 | **P1** |
| 4 | VMI / 寄售 / 受托代销（库存所有权分离） | **证据弱**：仅 Odoo `stock.quant` 唯一约束含 `owner` 元素（`odoo.md:45`，quant 层支持 owner，odoo.md 本身未讨论 VMI/寄售）；其余项目调研未记载 | ORM `ErpInvStockBalance` 维度无 `ownerId` | **P1**（证据弱，见 §四.3） |
| 5 | CRM（线索/商机/漏斗/营销） | ERPNext crm 顶层域（`erpnext.md:33`）；Odoo crm addon 族（领域常识）*非调研实测* | ORM 无 CRM 实体；sales 仅从报价单起 | P1 |
| 6 | 供应商评分卡 / AVL 准入 | ERPNext `Supplier Scorecard`（`erpnext.md:28` buying 域列出） | **RFQ/报价/价格清单已存在**；ORM 无评分/AVL 实体 | P1 |
| 7 | 运输 / 物流（TMS：发运单/路线/运费） | Odoo `delivery`/`stock_delivery`/`stock_fleet` addon（`odoo.md:31,32,33`） | 仅 sales UI 承运商字段；ORM 无发运单实体 | P1 |
| 8 | EDI / ASN / B2B 接口 / Webhook | Odoo `sale_edi_ubl`/`purchase_edi_ubl_bis3`（`odoo.md:31,32`）；ERPNext edi 顶层域（`erpnext.md:33`） | 仅金税（l10n-cn）；ORM 无 EDI/ASN 实体 | P1 |
| 9 | 批次召回事件管理（质量升级） | **领域常识**：食品/医药/汽车行业标准能力；*erp-survey 中 recall/召回 零命中，非调研实测* | 有批次追溯（`inventory/trace-chain.md`）+ NCR/CAPA + 退货；ORM 无召回事件实体 | P1 |
| 10 | DRP（分销需求计划，跨仓补货） | ERPNext `Material Request`（`erpnext.md:30` stock 域列出） | 仅 MRP（制造端）；ORM 无 DRP 实体 | **P2（不纳入核心）** |
| 11 | HRMS / 薪酬 / 考勤 | Metasfresh liberoHR + PayrollAcctDocProvider（`metasfresh.md:31,83`）；*iDempiere/Odoo/ERPNext hr/payroll 属领域常识，非调研实测* | ORM 无 HR 实体 | **P2（不纳入核心）** |
| 12 | POS / 门店零售 / 多渠道 | Odoo `point_of_sale`、ERPNext POS Invoice（领域常识）*非调研实测* | ORM 无 POS 实体 | **P2（不纳入核心）** |
| 13 | 售后服务 / 保修 / 客服工单 | ERPNext support 顶层域（`erpnext.md:33`） | 与 NCR/维护/退货重叠；ORM 无客服工单实体 | **P2（不纳入核心）** |

> 另有**库存 ABC 分类**（核查 `module-inventory` ORM 15 实体无 `abcClass` 字段）：erp-survey 未专门记载，属低优先级通用能力，作为 P2 备选记录，不展开。

---

## 三、P0 缺口详述（建议纳入产品基线扩展）

### 3.1 费用报销 / 员工借款 / 备用金

**场景描述**：员工差旅/招待/办公费用 → 报销单 → 主管审批 → 财务复核 → 付款；员工借款（预支）→ 发票核销 → 退余款；备用金定额补足；费用归集到项目/成本中心。

**调研证据（领域常识，非 erp-survey 实测）**：
- ERPNext `expense_claim`、Odoo `hr_expense`、iDempiere 均有员工费用报销能力，是成熟 ERP 公认特性。
- ⚠️ **erp-survey 未对 hr/expense 做单列实测**（`erpnext.md:33` 顶层域列表含 crm/support/edi 但**不含 hr/expense**，初稿误引该行号，已撤回）。本条缺口的真实性来自"项目侧 ORM 无实体 + projects 文档已引用"的内部不一致，**不依赖调研证据**。

**本项目当前状态**：
- ✅ 已有：项目成本归集机制、付款单（`ErpFinPayment`）、审核流
- ❌ 缺失：报销单/员工借款实体
- ⚠️ **内部不一致**：`projects/use-cases.md:57`、`projects/cost-collection.md:37,119,153,165` 与 `state-machine.md:88` **多处引用"费用报销"作为项目成本归集来源并生成凭证（借：项目费用 / 贷：应付职工薪酬/现金）**，但**无任何域拥有该实体**——`feature-inventory.md` 扩展业务功能表也无报销条目。这是"被引用无 owner"的真实不一致。

**建议落位**：
- **关键决策**：新建独立域 `module-expense`（参考 Odoo 独立 addon）vs 放入 `module-finance`（赤龙把 AP 含义扩展）？
- 推荐方案：**放入 `module-finance`**（报销对方科目是现金/银行/应付员工，本质是财务域单据；新建独立域过度拆分）
- 新增：`ErpFinExpenseClaim`（报销单头）、`ErpFinExpenseClaimLine`（明细，含 projectId/costCenterId 归集维度）、`ErpFinEmployeeAdvance`（员工借款头/行）
- 走现有审核流 + `IErpFinAcctDocProvider`（新增 businessType=EXPENSE_CLAIM/EMPLOYEE_ADVANCE 字典值），**无需新增过账引擎**
- 报销付款复用现有 `ErpFinPayment`（付款人=员工）
- 同步修正 `feature-inventory.md` 补报销条目，消除内部不一致

**优先级理由**：**projects 文档已引用但无对应实体**——这是设计内部不一致，必须优先补齐；任何真实企业运营都需要员工费用报销；落地成本低（复用审核+付款+过账）。

---

### 3.2 资金管理 / 票据管理（Treasury）

**场景描述**：银行对账只解决"钱已到账"的勾对，但企业日常资金运作还包括：
- **应收/应付票据**（银行承兑汇票/商业承兑汇票）：开票、背书、贴现、到期托收
- **短期融资**：银行授信额度管理、短期借款合同、利息计提
- **资金计划/现金预测**：基于 AR/AP 到期日 + 历史回款率预测未来现金流
- **票据到期调度**：托收日、贴现息计算

**调研证据（可溯源）**：
- **Metasfresh** `metasfresh.md:83` 明文 `BankingAcctDocProvider`（`de.metas.banking`）是独立 `@Component` 会计凭证 Provider——**证明票据/银行业务在成熟 ERP 中是业财一等的过账对象**
- **赤龙** `redragon-erp.md:41` `ApInvoiceHead` 含 `bankCode`/`subBankCode`/`bankAccount`/`payMode` 银行三件套字段（票据支付场景基础）
- ⚠️ 初稿曾误引"星云 SettleSheet = 资金管理模块"——`xingyun-erp.md:85` 的 `SettleSheet` 实为"对账-费用-预收付-结算单"结算流，**非票据/授信模块**，本报告已撤回该引用。

**本项目当前状态**：
- ✅ 已有：`ErpFinFundAccount`（accountType=BANK/CASH）、银行对账、汇兑损益重估
- ❌ 缺失：票据实体、贴现息计算、授信额度、资金计划/现金预测、票据到期托收调度
- 证据：`grep "票据|承兑|贴现|授信|融资"` 在 design/ 下仅在 `master-data/ui-patterns.md:134` 出现"2001 短期借款"科目名，无业务实体；各域 ORM 无对应实体

**建议落位**：
- 新增 `docs/design/finance/treasury.md`（资金管理专题）
- `module-finance/model` 新增：`ErpFinNotesReceivable`（应收票据）、`ErpFinNotesPayable`（应付票据）、`ErpFinCreditFacility`（授信额度）、`ErpFinCashForecast`（现金预测头/行）
- 票据过账走现有 `IErpFinAcctDocProvider` SPI（新增 businessType=NOTES_RECEIVABLE/NOTES_PAYABLE/DISCOUNT/LOAN_INTEREST），**无需新增过账引擎**
- 票据贴现息作为"财务费用-贴现息"科目，复用现有凭证模板

**优先级理由**：中国大陆 B2B 结算中**银行承兑汇票占比 30%+**，是中型以上企业刚需；本项目已有银行对账/资金账户基础，扩展成本低；Metasfresh 已验证票据独立 AcctDocProvider 的可行性。

---

## 四、P1 缺口详述（按行业/客户需求触发再启动）

### 4.1 CRP / APS（产能计划与车间排产）—— 项目自承认延迟项

**场景描述**：MRP 解决"物料需求"（要什么、要多少、何时要）；CRP（Capacity Requirements Planning）解决"产能需求"——根据计划生产单 × 工艺路线 × 工作中心产能，计算各工作中心负荷，识别瓶颈；APS（Advanced Planning & Scheduling）进一步做有限产能排产。

**调研证据（可溯源）**：
- `survey-index.md:93-99` 制造业能力矩阵"排产"列：Odoo ✅、ERPNext ✅、Metasfresh ✅、iDempiere ✅
- **项目自承认**：`manufacturing/mrp.md:10` 明文"本模块不负责：**产能计划（CRP，后续版本）**"

**本项目状态**：
- ✅ 已有：MRP（`manufacturing/mrp.md`）、工艺路线、工作中心、作业卡工时
- ❌ 缺失：产能计划实体（工作中心产能模型、负荷计算、有限产能排产）
- 这是项目设计文档**自己标注**的延迟项，是 P1 中最确定的缺口

**建议**：**P1，制造客户必选**。新增 `ErpMfgWorkCenterCapacity`（工作中心日历/班次/产能）、`ErpMfgCrpLoad`（负荷计算结果），MRP 运行后触发 CRP，超负荷时给出加班/外协/调整排程建议。属于 MRP 的天然延伸，复用现有工作中心/工艺路线数据。

---

### 4.2 CRM（线索/商机/漏斗/营销活动）

**场景描述**：销售订单**之前**的营销转化：线索→商机→报价→预测销售额→营销活动 ROI。

**调研证据（可溯源）**：**ERPNext** crm 顶层域（`erpnext.md:33`）；Odoo crm addon 族为领域常识（**非 erp-survey 实测细节**）。

**本项目状态**：sales 域从报价单（Quotation）开始，无 CRM。`grep "CRM|商机|线索|漏斗|opportunity|lead"` 在 design/ 下仅命中"lead time"无关项。

**建议**：**P1，按客户行业启动**。本项目定位为 ERP（非 CRM），标准 ERP 边界一般从报价单起。如目标客户群体为 B2B 制造业/分销业，CRM 可后置；如含零售/B2C，应提前。建议作为可选扩展模块 `module-crm` 设计，**不污染 sales 核心实体**。

---

### 4.3 VMI / 寄售 / 受托代销（库存所有权分离）—— 调研证据弱

**场景描述**：
- **VMI（供应商管理库存）**：供应商把货放在我方仓库，消耗时才结算所有权转移
- **寄售（我方货在客户仓）**：客户卖出才确认收入
- **受托代销**：代销他人商品，卖出后与委托方结算
- **客户提供的料**：OEM/代工场景客户免费提供的物料

**调研证据（弱，已如实标注）**：
- **Odoo** `odoo.md:45` `stock.quant` 的唯一约束含 `owner` 元素 `(product, location, lot, package, owner)`——**quant 层支持 owner 字段**。但 `odoo.md` 本身**未讨论 VMI/寄售场景**，本报告不夸大为"Odoo 完整 VMI 方案"。
- ⚠️ 初稿曾引用"管伊佳代销 subType"、"ERPNext Customer Provided Stock doctype"、"Metasfresh M_ProductStock_Owned"——**核查 erp-survey 全目录零命中，属编造，已全部撤回**。
- 业务场景本身（OEM 代工/寄售）真实存在，属通用 ERP 领域常识。

**本项目状态**：
- ✅ 已有：库存三层、批次/序列号、跨法人调拨（视同买卖）
- ❌ 缺失：库存的**所有权维度**（owner）。`ErpInvStockBalance` 维度是 `(item × warehouse × lot × ...)`，无 `ownerId`
- `grep "VMI|寄存|寄售|代销|owner|consignment"` 在 design/ 下零命中

**建议**：**P1（非 P0）**。鉴于调研证据弱，不主张定为"中型企业刚需 P0"，而是"OEM/分销行业客户的 P1 触发项"。若启动：
- 新增 `docs/design/inventory/consignment.md`
- 给 `ErpInvStockBalance`/`ErpInvStockLedger` 加 `ownerId`（引用 `ErpMdPartner`，默认=仓库归属法人）
- 新增 `ErpInvOwnershipTransfer`（所有权转移单）：VMI 消耗触发，生成结算依据→AP_INVOICE
- 新增字典 `erp-inv/stock-ownership`（OWNED/VMI_OWNED_BY_SUPPLIER/CONSIGNMENT_OUT/CUSTOMER_PROVIDED）
- 业财打通：所有权转移单走 `IErpFinAcctDocProvider`（businessType=OWNERSHIP_TRANSFER）

---

### 4.4 供应商评分卡 / AVL 准入（RFQ 已存在，仅评分缺失）

**场景描述**：RFQ/比价/价格清单**已存在**（见 §一.2）；剩余缺口是：
- **供应商评分卡**：按时交货率、质量合格率、价格竞争力 → 综合评分
- **AVL（Approved Vendor List）**：合格供应商目录 + 物料-供应商关联 + 准入/淘汰流程

**调研证据（可溯源）**：**ERPNext** `erpnext.md:28` buying 域 doctype 表明确列出 `Supplier Scorecard`。

**本项目状态**：
- ✅ 已有：`ErpPurRequisition`（请购）、`ErpPurRfq`/`ErpPurRfqLine`（询价）、`ErpPurQuotation`/`ErpPurQuotationLine`（供应商报价，含 `isAccepted` 中标标志 + 比价关系）、`ErpPurSupplierPriceList`（供应商价格清单）、`ErpMdPartner`（往来单位）
- ❌ 缺失：评分卡实体、AVL 准入流程实体

**建议**：**P1**。落地成本低：
- 新增 `ErpMdSupplierScore`（评分卡，按周期/维度记录）+ `ErpMdSupplierApproval`（AVL 准入/淘汰流程）
- 评分数据来源可从现有"采购订单按时交货率"（order vs delivery）、"质量检验合格率"（quality 域 inspection）派生，**无需新建业务流**

> **审查修正说明**：初稿曾把整个 RFQ 链判为缺口并建议新建 `ErpPurRequestForQuotation`/`ErpPurSupplierQuotation`——这是**严重误判**（上述实体早已存在，且 `2026-06-23-0003-menu-audit.md:35` 已确认覆盖）。本节已纠正为仅评分/AVL 子缺口。同时**撤销初稿对 `erp-design-audit-checklist.md:199` 的修正建议**——该 checklist 标注"请购→询价→报价→订单链"是**正确**的，与 ORM 一致。

---

### 4.5 运输 / 物流（TMS）

**场景描述**：销售出库后的物流环节：发运单（Shipment）、运输路线规划、承运商对接、运单跟踪、运费分摊到销售单/成本。

**调研证据（可溯源）**：**Odoo** `odoo.md:31,32,33` 明列 `delivery`、`stock_delivery`、`stock_fleet` addon。

**本项目状态**：`sales/ui-patterns.md:50` 仅"发货地址、物流信息（承运商、运单号）"作为字段，无发运单实体；`finance/costing-methods.md:281` 仅把"运费"作为 Landed Cost 一项。

**建议**：**P1**。多数 ERP 把 TMS 做成可选模块（Odoo 亦如此）。短期承运商/运单号字段够基础场景；长期若需第三方物流对接（顺丰/京东/DHL），新增 `module-tms` 独立工程 + SPI 承运商网关（参考 Metasfresh `de.metas.shipper.gateway.{spi,dhl,dpd,...}` 模式，`metasfresh.md:30`）。

---

### 4.6 EDI / ASN / B2B 接口 / Webhook

**场景描述**：EDI（电子数据交换）：B2B 大客户用 EDI 850（订单）/810（发票）/856（ASN 提前发货通知）；ASN：供应商发货前提前通知；Webhook：事件驱动对外推送。

**调研证据（可溯源）**：
- **Odoo** `odoo.md:31,32` 明列 `sale_edi_ubl`、`purchase_edi_ubl_bis3`
- **ERPNext** `erpnext.md:33` 顶层域列出 `edi`

**本项目状态**：仅 `l10n/cn-golden-tax.md` 有金税导入导出。`grep "EDI|ASN|webhook"` 在 design/ 下零命中。

**建议**：**P1，按大客户需求触发**。EDI 是大型零售商（沃尔玛/家乐福）的硬要求。不内建 EDI 引擎（避免标准复杂度），设计 `IErpB2BIntegrationProvider` SPI（参考 Metasfresh），按客户启用。优先做 ASN，后做 EDI 订单/发票。

---

### 4.7 批次召回事件管理（质量升级）

**场景描述**：批次追溯（已有）回答"这个批次去了哪里"；召回事件回答"**这个批次发现质量缺陷后如何系统化处理**"——定位全部已售产品 + 通知客户 + 集中退回 + 与 CAPA 联动。

**调研证据**：
- ⚠️ **领域常识**：食品/医药/汽车行业标准 QMS 能力
- *初稿曾引用"Odoo mrp_recall"、"ERPNext quality_management recall 流程"、"Carbon QMS 召回事件实体"——核查 erp-survey 全目录 `recall|召回` 零命中，属编造，已撤回*

**本项目状态**：
- ✅ 已有：批次全链路追溯（`inventory/trace-chain.md` 正向/反向递归）、NCR/CAPA/让步（`quality/state-machine.md`）、销售/采购退货
- ❌ 缺失：**召回事件**作为聚合实体（统一管理"某批次→N 个客户→通知/退回/CAPA 关联"）

**建议**：**P1**。本项目已具备所有底层能力（追溯 + NCR + 退货），缺的是事件层聚合。**修正初稿的乐观估计**：召回不是"从已有 NCR 状态机升级路径扩展"——核查 `quality/state-machine.md`，NCR 状态机**无现成"升级到召回"路径**，需新建召回事件实体与触发路径。具体：
- `module-quality/model` 新增 `ErpQaRecall`（召回事件头）+ `ErpQaRecallTarget`（受影响客户/批次明细）
- 召回触发：手动创建或从 NCR 升级（NCR 新增"升级为召回"动作，非状态机现成路径）
- 召回执行：批量生成销售退货单（已有），关联 NCR→CAPA（已有）
- 工作量属"中等扩展"（非初稿所称"轻量"），但成本低收益明显（食品/医药/汽车行业刚需）。

---

## 五、P2 缺口（明确不纳入核心，避免功能蔓延）

### 5.1 DRP（分销需求计划）

**调研证据（可溯源）**：**ERPNext** `erpnext.md:30` stock 域列出 `Material Request`。

**本项目状态**：MRP（`manufacturing/mrp.md`）的"需求来源"已含"安全库存补货"，**部分覆盖 DRP 场景**；缺独立的多级分销网络层级补货引擎。

**决策**：**P2，明确不纳入核心**。完整 DRP（多级网络、级际时间）仅在大型分销网络（>5 分仓）才有价值。短期在 MRP 文档中明确"分销场景的覆盖范围"，长期按需扩展。**本报告统一口径：DRP 列为 P2（不纳入核心），不再出现 P1/P2 模糊表述**。

### 5.2 HRMS / 薪酬 / 考勤

**调研证据（可溯源）**：**Metasfresh** `metasfresh.md:31,83` 明文 `de.metas.adempiere.libero.liberoHR` + `PayrollAcctDocProvider`（liberoHR）为 `@Component`。*初稿曾把此证据归于 iDempiere——核查 `idempiere.md` 全文未提 HR/liberoHR/Payroll，属张冠李戴，已修正为 Metasfresh。iDempiere/Odoo/ERPNext 的 hr/payroll 属领域常识，非调研实测。*

**决策**：**不纳入核心 ERP 基线**。理由：
- 产品定位（`product-scope.md:5`）为"产品化通用 ERP 产品，可快速定制适配各个领域"——愿景比"进销存+财务"宽，但 10 域基线**不含 HR**，且 `product-scope.md:49-52`"延迟范围：外部集成"覆盖此类
- HR/薪酬本地化极重（社保/个税/用工法规地区差异大），不适合通用底座
- 建议接入第三方 HR（北森/用友 DHR），通过 `IErpB2BIntegrationProvider` 同步员工主数据 + 薪酬凭证（businessType=PAYROLL）
- 仅在 projects 域保留"工时→应付职工薪酬"成本归集（已有）

### 5.3 POS / 门店零售 / 多渠道电商

**调研证据**：领域常识（Odoo point_of_sale、ERPNext POS Invoice、Dolibarr），*非 erp-survey 实测*。

**决策**：**不纳入**。POS 是零售业专用，B2B 制造/分销 ERP 不需要；电商集成属渠道层，应独立模块 + API 对接（参考 EDI 模式）。sales 域"销售订单"已是 POS 后的聚合结果。

### 5.4 售后服务 / 保修 / 客服工单

**调研证据（可溯源）**：**ERPNext** `erpnext.md:33` 顶层域列出 `support`。

**决策**：**不纳入核心，保留扩展点**。售后服务流程与本项目"质量+维护+退货"高度重叠（已有：NCR/CAPA、维护请求、销售/采购退货）；缺的仅是客服工单（customer-facing ticketing），属 CRM/Helpdesk 范畴。如客户需要，作为 `module-cs` 可选扩展，复用现有能力。

---

## 六、与现有架构原则的契合性

本报告建议的所有 P0/P1 扩展，均**复用现有架构契约**，无需引入新范式：

| 契约 | 复用方式 |
|---|---|
| 业财一体（posted 兜底） | 票据/报销/所有权转移均走 `IErpFinAcctDocProvider` SPI，新增 businessType 字典值 |
| 凭证三件套 + 业财回链 | 所有新单据过账后写 `ErpFinVoucherBillR` 反查 |
| 状态机三轴分离 | 票据/报销单/召回均用 docStatus + approveStatus + posted 三轴 |
| 跨域 DAG + I\*Biz | VMI 跨 inventory/purchase/sales 通过 `I*Biz` 调用，无 ORM 强引用 |
| Model → Delta → Java | 所有新实体先建 orm.xml（唯一真相） |
| 配置化（NopSysConfig） | 召回/EDI/CRP 开关通过配置项启用，不硬编码 |
| 独立模块化（VMI/CRM/TMS/CS） | 参考 l10n-cn 模式，按需新增独立 Maven 工程 |

**结论**：本报告识别的缺口不挑战现有任何架构决策，可作为渐进式扩展路线图。

---

## 七、优先级建议与 roadmap 输入

建议将以下条目输入 `docs/backlog/implementation-roadmap.md` 评审：

| 优先级 | 场景 | 触发条件 | 预估新增实体数 |
|---|---|---|---|
| **P0** | 费用报销/员工借款 | 首批客户上线前（**已与 projects 文档矛盾，必须补齐**） | 3-4 |
| **P0** | 资金管理/票据 | 目标客户含年营收>1 亿的中型企业 | 5-6 |
| P1 | CRP/APS | 制造客户（**项目自承认延迟项**） | 2-3 |
| P1 | 供应商评分卡/AVL | 客户采购合规要求 | 2 |
| P1 | 批次召回事件 | 食品/医药/汽车行业 | 2（中等扩展） |
| P1 | CRM | 目标客户含 B2C 或主动销售场景 | 5-8（独立工程） |
| P1 | VMI/寄售/受托代销 | OEM 代工或零售分销客户 | 2-3 + StockBalance 加 ownerId |
| P1 | 运输/TMS | 客户需对接第三方物流 | 3-5（独立工程 + SPI） |
| P1 | EDI/ASN | 客户含大型零售商或跨国 B2B | 1-2 + SPI 接口 |
| P2 | DRP/HRMS/POS/售后/ABC | **明确不纳入核心**，按需独立扩展 | — |

---

## 八、对现有文档的修正建议

审计过程中发现一处文档与实际设计不一致，建议修正：

| 文档 | 位置 | 问题 | 建议修正 |
|---|---|---|---|
| `feature-inventory.md` | 扩展业务功能表 | 缺"费用报销"条目 | 但 `projects/use-cases.md:57`、`projects/cost-collection.md:37,119,153,165` 已引用"费用报销 → 借：项目费用 / 贷：现金"作为成本归集来源——**无对应实体**。建议在 P0 设计时于 finance 域补报销实体，并在本表补条目 |

> **审查修正说明**：初稿曾建议修正 `erp-design-audit-checklist.md:199`（"询价→报价"标注）——核查后该 checklist 是**正确**的（`ErpPurRfq`/`ErpPurQuotation` 实体存在），**撤销该修正建议**。

---

## 九、结论

1. **核心 ERP 闭环无设计缺陷**（前序四份审计结论继续成立；本次纠正了初稿对 RFQ 的误判）。
2. **13 个高级业务场景**未深入展开（矩阵 13 行 + ABC 分类脚注）：
   - **2 个 P0** 应纳入产品基线扩展（费用报销是已暴露的内部不一致，资金/票据是中型企业刚需）
   - **7 个 P1** 按客户行业触发（CRP 为项目自承认延迟项；VMI 证据弱已如实标注；供应商评分/CRM/TMS/EDI/召回均可复用现有架构）
   - **4 个 P2** 明确不纳入核心（DRP/HRMS/POS/售后，另有 ABC 分类作为 P2 备选），避免功能蔓延
3. **所有扩展均不挑战现有架构决策**，是渐进式、可插拔的，符合 `competitive-comparison.md` 的"模型驱动 + Delta + 跨域 DAG"哲学。
4. **建议先修正 `feature-inventory.md` 的报销条目不一致**，再启动 P0 扩展设计。

---

## 十、参考证据索引

### erp-survey（业务场景来源，均已核查可溯源）
- `erp-survey/2026-06-22-0000-survey-index.md:93-99`（制造业能力矩阵排产列 → CRP 证据）
- `erp-survey/2026-06-22-0000-odoo.md:31,32,33`（delivery/stock_delivery/stock_fleet/edi_ubl addon → TMS/EDI 证据）
- `erp-survey/2026-06-22-0000-odoo.md:45`（stock.quant owner 元素 → VMI 弱证据）
- `erp-survey/2026-06-22-0000-erpnext.md:28,30,33`（Supplier Scorecard / Material Request / crm/edi/support 顶层域 → 供应商评分/DRP/CRM/EDI/售后证据）
- `erp-survey/2026-06-22-0000-metasfresh.md:30,31,83`（shipper.gateway SPI / liberoHR+Payroll / Banking provider → TMS/HRMS/票据证据）
- `erp-survey/2026-06-22-0000-redragon-erp.md:41`（ApInvoiceHead 银行三件套字段 → 票据支付基础）
- `erp-survey/2026-06-22-0000-xingyun-erp.md:85`（SettleSheet 结算单，**非授信模块**）
- `erp-survey/2026-06-22-0000-business-design-takeaways.md`（9 大主题业务设计参考）
- `erp-survey/2026-06-22-0000-subdomain-opensource-coverage.md`（子域覆盖与缺口）

### 本项目（覆盖验证，含 ORM 权威）
- `module-purchase/model/app-erp-purchase.orm.xml:160,188,214`（**ErpPurRfq/ErpPurRfqLine/ErpPurQuotation 实体存在**，纠正初稿 RFQ 误判）
- `module-inventory/model/app-erp-inventory.orm.xml`（15 实体，无 ownerId/abcClass → VMI/ABC 缺口）
- `docs/design/manufacturing/mrp.md:10`（**项目自承认 CRP 延迟**）
- `docs/design/finance/bank-reconciliation.md`（已有银行对账，对比 §3.2 票据缺口）
- `docs/design/projects/use-cases.md:57`、`projects/cost-collection.md:37,119,153,165`（引用报销但无实体，§3.1）
- `docs/design/quality/state-machine.md`（NCR 状态机无召回升级路径，修正 §4.7 工作量评估）
- `docs/design/feature-inventory.md`（功能清单无报销条目）
- `docs/analysis/2026-06-23-0003-menu-and-feature-completeness.md:35`（**前序审计已确认采购寻源 RFQ/报价覆盖**）
- `docs/architecture/competitive-comparison.md`（8 杠杆 + 诚实声明 §六）
- `docs/requirements/product-scope.md:5,49-52`（产品范围边界与延迟范围）

### 审查过程（本报告经独立子代理两轮审查修正）
- **第一轮独立审查**发现的关键问题（已全部修正）：① RFQ 实体误判（实已存在）；② 多处调研证据编造（VMI 代销/recall/hr_expense 等 erp-survey 零命中）；③ liberoHR 归属错误（Metasfresh 非 iDempiere）；④ 漏列 CRP；⑤ 召回工作量低估；⑥ 交叉引用/DRP 优先级/product-scope 引用措辞问题。
- **第二轮独立复核**（fresh context）结论：9 个前轮问题**全部真正解决**（逐条经源文件核查）；新发现 2 处呈现层瑕疵——① P1 计数矛盾（TL;DR/§九 写"6"但矩阵实为 7）；② `erpnext.md:33` 不含 hr/expense 的误引——均已修正。第二轮判定"实质达成共识，修正后可发布"。
