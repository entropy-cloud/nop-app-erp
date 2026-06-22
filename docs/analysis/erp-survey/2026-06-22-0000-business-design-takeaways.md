---
调研日期: 2026-06-22
来源: 13 个 ERP 项目调研（见 erp-survey/ 同目录）
状态: 已完成（基于源码实测归纳）
---

# 进销存+财务一体化业务设计参考

> 本文档从 13 个 ERP 项目中提炼对 nop-app-erp **进销存+财务一体化**最有借鉴价值的业务设计点，按主题归纳。每条给出**源项目 + 源码证据路径 + 对 nop 的落地建议**。
>
> 配套文档：模块拆分对比见 `2026-06-22-0000-module-split-comparison.md`；架构结论见 `docs/architecture/domain-module-split-analysis.md`。

## 主题 1：业财一体过账机制（最核心）

业务单据如何自动生成会计凭证，是业财一体的核心。三个层次的设计：

### 1.1 统一钩子触发（ERPNext 范式）
- **源项目**：ERPNext
- **证据**：`erpnext/accounts/general_ledger.py:34`（`make_gl_entries` 入口）+ `purchase_receipt.py:379`（`on_submit`）+ `:452`（`on_cancel` 冲销）
- **机制**：业务单据 `on_submit` 调 `make_gl_entries()`，`on_cancel` 调 `make_reverse_gl_entries()`；**所有业务单据（含库存单）过账走同一入口**
- **对 nop 建议**：建立统一 `on_post/on_reverse` 钩子，所有业务单据过账走同一入口，业务取消即冲销 GL。业务模型独立，财务是横切关注点（通过 on_submit 钩子注入）

### 1.2 审批触发 + 同一事务落地库存与凭证（赤龙范式）
- **源项目**：赤龙 ERP
- **证据**：`InvInputHeadServiceImpl.java:145`（审批 `APPROVE` 分支）→ `autoCreateVoucher("INPUT")` 同时写 `InvStock` 库存；`FinVoucherModelHeadServiceImpl.java:143`（`autoCreateVoucher` 核心）
- **机制**：审批通过即触发凭证生成，**库存与凭证在同一事务同一审批节点落地**，保证账实一致
- **对 nop 建议**：出入库审批节点同时写库存与凭证（同事务）；业务取消（反审批）按回链表反查并删凭证

### 1.3 异步过账 + EventBus（Metasfresh 范式，工程最佳实践）
- **源项目**：Metasfresh
- **证据**：`DocumentPostingBusService.java:104-110`（事务提交后投事件到 `IEventBus`）+ `PostingService.java:78-112`（`postNow`）
- **机制**：主事务只落业务单据 + `Posted=N`，过账在 post-commit 异步执行，失败可重试不阻塞主单据
- **对比**：iDempiere 用 30 秒 DB 轮询（`AcctProcessor.java:60`），延迟高
- **对 nop 建议**：主事务落单据 + `posted=false`，过账走 nop 消息总线/事件队列（nop-stream/nop-message）异步执行；保留 `posted` 字段做兜底扫描 job

### 1.4 凭证生成引擎：模板方法 + 类型安全注册（Metasfresh 演进）
- **源项目**：Metasfresh（演进自 iDempiere 的反射命名约定）
- **证据**：
  - `Doc<DocLineType>` 抽象基类（`Doc.java:148`）暴露 `loadDocumentDetails()` + `createFacts(acctSchema)` 两个钩子
  - `StandardAcctDocProvider.java:56-79` 用 `ImmutableMap.<String,AcctDocFactory>builder().put(I_C_Invoice.Table_Name, Doc_Invoice::new)`（**编译期类型安全，无反射字符串**）
  - `AcctDocRegistry.java:25-33` 注入 `List<IAcctDocProvider>` 跨模块自动聚合
- **对比**：iDempiere `DefaultDocumentFactory.java:87-101` 靠 `Class.forName("Doc_" + table)` 反射（脆弱，重命名即失效）
- **对 nop 建议**：用 nop IoC 的 `@Inject Map<String, IDocFactory>`（key 用 entityName），凭证生成规则做成可插拔模块；借贷规则用 nop 规则引擎/DSL 表达，而非硬编码 Java

## 主题 2：凭证与 AR/AP 字段设计

### 2.1 凭证三件套（赤龙，中式复式记账完整骨架）
- **源项目**：赤龙 ERP
- **证据**：
  - `FinVoucherHead`（凭证头）：`voucherType`(收/付/转)、`voucherNumber`(按凭证字 Redis 流水)、`voucherDate`、`billNum`
  - `FinVoucherLine`（借贷分录行）：`subjectCode`、`drAmount`/`crAmount`（**每行只填一侧，另一侧为 0**）、`memo`
  - `FinVoucherBillR`（**业财回链表**）：`voucherHeadCode` + `billType`(PAY/RECEIPT/INPUT/OUTPUT/AP_INVOICE) + `billHeadCode`
- **对 nop 建议**：财务核心采用凭证头 + 借贷分录行 + 业财回链表三件套；凭证号按凭证字分类连续编号（符合中国"字第号"规范）；借贷分录行只填一侧

### 2.2 凭证模板 + 占位符（赤龙 + Dolibarr 国际通用范式）
- **源项目**：赤龙 ERP（`FinVoucherModelHead/Line`）+ Dolibarr（`bookkeepingtemplate.class.php`）
- **证据**：赤龙 `FinVoucherModelLine.drAmount/crAmount` 填占位符字符串 `"AMOUNT"`，`autoCreateVoucher(billHeadCode, Double[]{档1,档2,...}, businessType)` 按模板行顺序填充
- **机制**：一套模板覆盖一类业务，应付发票可同时生成"库存/进项税/应付"三条分录
- **对 nop 建议**：凭证模板用 nop 规则引擎/DSL 表达，占位符按金额档位填充；按 `businessType`（INPUT/OUTPUT/AP_INVOICE/AR_INVOICE/PAY/RECEIPT）区分模板

### 2.3 AR/AP 发票头行 + 来源单据回链（赤龙）
- **源项目**：赤龙 ERP
- **证据**：`ApInvoiceHead.invoiceSourceType`/`invoiceSourceHeadCode` + `ApInvoiceLine.invoiceSourceLineCode`
- **对 nop 建议**：发票行保留 `source_bill_type`/`source_head_code`/`source_line_code`，实现"三单匹配"（PO-入库-发票）

### 2.4 付款核销多对多关系表（赤龙）
- **源项目**：赤龙 ERP
- **证据**：`ApPayLine(pay_head_code, invoice_head_code, invoice_pay_amount)`——一行付款可核销多张发票；头级 `paidStatus`(N/Y/PART) 聚合
- **对 nop 建议**：一张付款单核销多张发票、一张发票被多次部分付款；头级聚合 `paidStatus`

### 2.5 发票头四类状态分离（赤龙）
- **源项目**：赤龙 ERP
- **证据**：`status`(NEW/CONFIRM/CANCEL) + `approveStatus`(UNSUBMIT/SUBMIT/APPROVE/REJECT) + `paidStatus`(N/Y/PART)
- **对 nop 建议**：单据状态设计三轴独立——业务状态、审批状态、财务状态，避免状态混乱

### 2.6 行级税额独立列（价税分离，国产通用）
- **源项目**：管伊佳、赤龙、若依（均如此）
- **证据**：管伊佳 `DepotItem.taxRate/taxMoney/taxLastMoney`；若依 `sales_order_detail.price_without_tax/tax_amount/tax_rate/price_with_tax`
- **对 nop 建议**：明细表内建税字段三件套（`taxRate`/`taxAmount`/`amount` 分离），便于增值税核算和价税分离凭证

### 2.7 GL 行多币种 + 多维度（ERPNext + iDempiere）
- **源项目**：ERPNext、iDempiere
- **证据**：ERPNext `gl_entry.json:35-43`（debit/credit × 三币种）+ `cost_center`/`project`；iDempiere `FactLine.AmtSourceDr/Cr`（源币种）+ `AmtAcctDr/Cr`（本位币）
- **对 nop 建议**：凭证行原生支持多币种（源币种 + 本位币双字段）与可扩展会计维度（成本中心/项目），避免后期重构

## 主题 3：库存三层模型

### 3.1 计划/调度/执行/现状四层（Odoo 行业标杆）
- **源项目**：Odoo
- **证据**：
  - `stock.move`（计划层"一次移动意图"，状态 7 态 `draft→...→done→cancel`）`stock/models/stock_move.py:19`
  - `stock.picking`（操作层"一次交接/调拨单"）`stock_picking.py:539`
  - `stock.move.line`（执行层"实际搬动的批次行"）`stock_move_line.py:16`
  - `stock.quant`（现状层"某库位某产品的现存量"）`stock_quant.py:20`
- **对 nop 建议**：用"库存移动单（计划/执行合一）+ 库存流水（不可变）+ 库存余额（按 item×warehouse×lot 快照）"三层；业务单据只生成移动单→确认后写流水→更新余额

### 3.2 库存移动单自追溯链（Odoo）
- **源项目**：Odoo
- **证据**：`stock_move.py:98-158`（`move_orig_ids`/`move_dest_ids` M2M 自关联 + `origin_returned_move_id`）
- **对 nop 建议**：库存移动单内建"上下游移动关联"，支持采购到货→生产领料→销售出库全链路追溯

### 3.3 不可变库存流水 + FIFO 队列（ERPNext）
- **源项目**：ERPNext
- **证据**：`stock_ledger_entry.json:10-46`（`actual_qty`/`qty_after_transaction`/`valuation_rate`/`stock_value`/`stock_queue` FIFO）
- **对 nop 建议**：库存流水不可变，含 `valuation_rate`/`stock_value`/FIFO 队列；靠 `voucher_type`+`voucher_no` 反查源单

### 3.4 库存台账两种实现（赤龙 vs 管伊佳对比）
- **赤龙**（流水台账）：`InvStock.stockNumber`（入库正/出库负），汇总=余额——审计性强
- **管伊佳**（余额汇总表）：`MaterialCurrentStock` + 重算 upsert——性能好但并发风险
- **对 nop 建议**：用流水明细为主、余额表为物化缓存（流水驱动更新，非重算）

### 3.5 作业类型参数化（Odoo）
- **源项目**：Odoo
- **证据**：`stock.picking.type`（code=incoming/outgoing/internal/mrp_operation）`stock_picking.py:21`
- **对 nop 建议**：库存作业类型参数化（收/发/内/制），绑定默认 location 与科目映射

## 主题 4：单据模型设计

### 4.1 出入库单据双维度类型（管伊佳，最简洁）
- **源项目**：管伊佳 ERP
- **证据**：`DepotHead.type`(入库/出库/其它) + `subType`(采购订单/采购/采购退货/零售/销售/销售退货/调拨/盘点录入/盘点复盘/组装/拆卸/请购单)——见 `BusinessConstants.java:40-99`；**采购和销售靠同一张表 + subType 区分**
- **对 nop 建议**：进销存单据用 `docType`(入库/出库/其它) + `bizType`(采购/销售/调拨/盘点...) 双维度字典，所有出入库单据共用头行表

### 4.2 进销存三单链（若依）
- **源项目**：若依 ERP（zccbbg）
- **证据**：`*_order`(订单) → `*_trade`(成交/出入库) → `*_refund`(退货)，每单 head+detail
- **对 nop 建议**：进销存单据链复用此模式（订单→成交/出入库→退货）

### 4.3 单据四级拆分（星云）
- **源项目**：星云 ERP
- **证据**：`SaleOutSheet` + `Detail` + `DetailBundle` + `DetailLot`（明细-组合-批次）
- **对 nop 建议**：销售出库可参照做"明细-组合-批次"三层子表（仅批次管理场景需要）

### 4.4 状态双维度（若依）
- **源项目**：若依 ERP
- **证据**：`sales_order` 的 `checked_status`(审核) + `stock_status`(库存) 分离
- **对 nop 建议**：用两个 int 字段 + `erp/doc-status`/`erp/stock-status` 字典，比单一 status 灵活

### 4.5 声明式状态机（Tryton）
- **源项目**：Tryton
- **证据**：`workflow.py`（`_transitions = {(from_state, to_state), ...}` + `@transition(state)` 装饰器自动校验）
- **对 nop 建议**：单据状态机用 DSL/字典声明转换规则，集中校验，避免散落 if-else
- **专题深入**：见 `2026-06-22-0000-workflow-vs-state-machine.md`（13 项目流程实现横向对比）

### 4.6 单据唯一编号约束（若依）
- **源项目**：若依 ERP
- **证据**：`sales_order` 的 `UNIQUE INDEX idx_bill_no(doc_no)`
- **对 nop 建议**：实体对单据号加 unique 约束

## 主题 5：主数据设计

### 5.1 物料 + SKU 多单位多 barcode 分离（管伊佳，国产标准）
- **源项目**：管伊佳 ERP
- **证据**：`Material`(基础属性) + `MaterialExtend`(每行=一个 SKU：单位+条码+四档价格 `purchaseDecimal`/`commodityDecimal`/`wholesaleDecimal`/`lowDecimal` + `defaultFlag`)
- **对 nop 建议**：物料主数据拆 `Material` + `MaterialSku`，单据引用 SKU；多单位换算和分级定价的标准国产方案

### 5.2 供应商/客户一体表 + 应收应付余额（管伊佳）
- **源项目**：管伊佳 ERP
- **证据**：`Supplier.type`(供需区分) + `beginNeedGet`/`beginNeedPay`(期初) + `allNeedGet`/`allNeedPay`(累计) + `advanceIn`(预收预付余额)
- **对 nop 建议**：往来单位主数据直接承载应收应付余额，配合核销子表形成欠款闭环

### 5.3 序列号台账独立表 + 双向回链（管伊佳）
- **源项目**：管伊佳 ERP
- **证据**：`SerialNumber.isSell`(0未售1已售) + `inBillNo`/`outBillNo`(入库单号/出库单号双向回链)
- **对 nop 建议**：序列号管理做独立台账表，出入库时带 snList，双向回链源单

### 5.4 含税/不含税双价字段内建于商品（Dolibarr）
- **源项目**：Dolibarr
- **证据**：`product.class.php`: `price`/`price_ttc`(含税)/`price_base_type`(HT/TTC)
- **对 nop 建议**：商品主数据内建含税价/不含税价/计价基准类型

## 主题 6：科目表（COA）与成本核算

### 6.1 父子树形科目 + 段值编码（赤龙）
- **源项目**：赤龙 ERP
- **证据**：`MdFinanceSubject.parentSubjectCode` + `segmentCode`/`segmentDesc`（多段式弹性科目，模仿 Oracle EBS 国产化简化）
- **对 nop 建议**：科目表支持多级科目（一级编码+明细）和弹性段，便于汇总查询和报表

### 6.2 多会计科目表并行（iDempiere）
- **源项目**：iDempiere
- **证据**：`C_AcctSchema` 按 `AD_Client_ID` + `AD_Org_ID` 配多套，单据在多套下各出一组 Fact_Acct；`Fact.java:78-84` 四种 PostingType（Actual/Budget/Commitment/Reservation）
- **对 nop 建议**：凭证表带 `acctSchemaId` + `postingType` 维度，支持管理账/税务账/预算账并行

### 6.3 科目解析分层：按业务对象维度（Metasfresh）
- **源项目**：Metasfresh
- **证据**：`de.metas.acct.base/.../accounts/` 13 类（BPartner/Product/Tax/Charge/Project/Warehouse/CostElement）；`InvoiceAcct.java:41-56` 按 `(acctSchemaId, accountConceptualName, invoiceAndLineId)` 三元组"specific→generic"匹配覆盖
- **对 nop 建议**：科目映射做成多维决策表（伙伴组/产品类别/项目/仓库 → 科目），用 nop 规则引擎或元数据驱动，而非 if-else

### 6.4 成本核算多方法（iDempiere）
- **源项目**：iDempiere
- **证据**：`C_AcctSchema.CostingMethod`（`A`/`F`/`I`=`AveragePO`/`Fifo`/`AverageInvoice`）+ `M_Cost`（`CurrentCostPrice`/`CurrentCostPriceLL`(landed cost)/`CumulatedAmt`）
- **对 nop 建议**：成本核算支持移动加权平均/FIFO/批次多种方法；landed cost（到岸成本）独立字段

## 主题 7：多公司/多币种/多租户

### 7.1 AD_Client/AD_Org 双层租户（iDempiere）
- **源项目**：iDempiere
- **证据**：`AD_Client_ID`(租户) + `AD_Org_ID`(公司/组织)，所有业务表+凭证表都带；`Doc.post` 校验凭证与单据同租户
- **对 nop 建议**：用 `nop-auth` 多租户 + 业务表加 `orgId` 字段表达公司/组织维度

### 7.2 源币种+本位币双字段（iDempiere + ERPNext）
- **源项目**：iDempiere、ERPNext
- **证据**：iDempiere `FactLine.AmtSourceDr/Cr`（源）+ `AmtAcctDr/Cr`（本位币）+ `MConversionRate`；ERPNext GL 行三币种
- **对 nop 建议**：凭证行内建双币种字段（源币种 + 本位币），汇率转换独立配置

## 主题 8：应收应付核销与欠款闭环

### 8.1 欠款闭环公式（管伊佳）
- **源项目**：管伊佳 ERP
- **证据**：`DepotHeadService.java:185`（`debt = discountLastMoney + otherMoney − deposit − changeAmount`）
- **对 nop 建议**：应付/应收计算明确化：折后+其他费用−订金−已结算

### 8.2 多账户多金额规范化（管伊佳反例）
- **源项目**：管伊佳 ERP（**反例参考**）
- **证据**：`DepotHead.accountIdList`+`accountMoneyList`（逗号分隔字符串，反范式）
- **对 nop 建议**：改为 `depot_account_allocation(head_id, account_id, amount)` 子表，不要用逗号字符串

### 8.3 双账本并行：GL + Payment Ledger（ERPNext）
- **源项目**：ERPNext
- **证据**：`general_ledger.py:58` `create_payment_ledger_entry()`（付款台账与 GL 并行，用于应收应付核销）
- **对 nop 建议**：若核销逻辑复杂，可考虑 GL + 独立付款台账双账本

## 主题 9：本地化策略

### 9.1 中国本地化独立可拔模块（OCA 范式）
- **源项目**：OCA/l10n-china
- **证据**：本地化模块分散在子仓，每模块独立 `__manifest__.py`，按 Odoo 版本分支隔离
- **对 nop 建议**：设独立 `app-erp-l10n-cn` orm 片段/工程，金税/增值税发票/银行对账/中国报表各自可独立装卸，**不内建在核心 ERP 实体里**——这样本地化可单独演进，不污染通用进销存模型

### 9.2 本地化最小能力集（OCA 推断）
- **金税接口**（Golden Tax）
- **增值税发票**（VAT Invoice）
- **银行对账**（Bank Reconciliation）
- **中国特色财务报表**

## 主题 10：业务流程实现——流程引擎 vs 状态变迁

> 详细分析见 `2026-06-22-0000-workflow-vs-state-machine.md`。

### 10.1 三种范式分布

13 个项目的业务流程实现分为三类：
- **完整流程引擎**（3 个）：iDempiere（OMG 规范自研）、Metasfresh（现代化演进）、WMES（JSON 可视化设计器）
- **可选/轻量流程引擎**（2 个）：星云（warm-flow BPM 可选开关）、管伊佳（插件式多级审批可选）
- **纯状态变迁**（7 个）：Odoo、ERPNext、Tryton、赤龙、若依 v1/v2、OCA
- **触发器式自动化**（1 个）：Dolibarr（报价→订单→发票→发货链式触发）

### 10.2 主流趋势：状态变迁为主，流程引擎为辅

**关键发现**: 13 个中最成熟的 ERP（Odoo、ERPNext、赤龙）都**不依赖流程引擎**，而是用状态字段 + 代码逻辑实现流转。流程引擎（iDempiere、Metasfresh）虽然功能强大，但增加了系统复杂度。

**ERPNext 的量化驱动模式**值得特别关注：状态由收货/开票百分比自动推导，而非显式"转换动作"——这在进销存场景中非常自然。

### 10.3 对 nop 建议：声明式状态机 + 可选审批流

- **核心**：用 DSL/字典声明转换规则（参考 Tryton `_transitions`），集中校验
- **审批**：双轴状态分离——`docStatus`（业务生命周期）+ `approveStatus`（审批流），参考赤龙
- **事件钩子**：统一 `onSubmit`/`onReverse` 钩子做业财联动，参考 ERPNext
- **可选 BPM**：复杂审批场景可插入流程引擎，但不作为默认必选，参考星云

## 综合推荐：nop-app-erp 进销存+财务一体化设计骨架

基于以上 9 大主题，推荐如下组合（详见架构结论 `docs/architecture/domain-module-split-analysis.md`）：

| 设计层 | 推荐范式 | 源项目 |
|---|---|---|
| **凭证三件套** | Voucher/VoucherLine/VoucherBillR + 借贷分录行只填一侧 | 赤龙 |
| **凭证模板** | Model + 占位符填充 + businessType 区分 | 赤龙 + Dolibarr |
| **过账触发** | 审批触发（赤龙）+ 统一 on_submit 钩子（ERPNext） | 赤龙 + ERPNext |
| **过账引擎** | 模板方法 Doc/Fact + 类型安全 ImmutableMap 注册 + EventBus 异步 | Metasfresh |
| **库存三层** | 移动单(计划/执行) + 不可变流水 + 余额快照 | Odoo + ERPNext |
| **进销存单据** | 双维度类型(type+bizType)单表 + order→trade→refund 链 | 管伊佳 + 若依 |
| **主数据** | Material+SKU 分离 + 供应商客户一体表 | 管伊佳 |
| **价税分离** | 明细表税字段三件套 | 管伊佳/若依 |
| **多币种/多科目表** | GL 行双币种 + 多 AcctSchema 并行 | iDempiere + ERPNext |
| **状态机** | 声明式转换规则 + 三轴状态分离(业务/审批/财务) | Tryton + 赤龙 |
| **审批流** | 声明式状态机为核心 + 可选 BPM 叠加层 | Tryton + 星云 |
| **业财事件钩子** | 统一 onSubmit/onReverse 钩子 + 触发器链 | ERPNext + Dolibarr |
| **本地化** | 独立可拔 l10n-cn 模块 | OCA |
| **模块化** | 按业务域拆多 Maven 模块（非单 orm.xml） | 见架构结论 |
