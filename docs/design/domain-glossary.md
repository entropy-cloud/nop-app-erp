# 领域词汇表

## 目的

统一 `nop-app-erp` 设计文档中的核心领域概念、标准中文译法和中英文对应关系。

本词汇表只收录跨多个设计文档重复出现、且容易产生歧义的核心术语。

## 使用规则

- 设计文档出现术语冲突时，以本词汇表为准。
- 如果某个概念已经有稳定中文译法，不要在不同文档中随意改写成新的近义词。
- 如果某个英文术语需要保留，应优先在首次出现时同时给出中文含义，再按文档风格决定后续是否继续保留英文。
- 本词汇表用于统一业务概念，不替代 `model/app-erp-*.orm.xml` 中的字段、字典或状态码真相。

## 组织与主数据词汇

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Material | 物料 | master-data | ERP 管理对象的最小主数据单元：商品、原材料、产成品、服务等 |
| SKU | SKU / 库存单位 | master-data | 物料 × 包装单位 × 条码 的唯一可销售/可库存单元 |
| Partner | 往来单位 | master-data | 客户与供应商的统一主数据，一个单位可同时是客户和供应商 |
| Warehouse | 仓库 | master-data | 物理或逻辑库存地点 |
| Location | 库位 | master-data | 仓库内的细分储位 |
| UoM | 计量单位 | master-data | 物料的计量单位，分属不同单位组 |
| Currency | 币种 | master-data | 结算货币主数据 |
| Exchange Rate | 汇率 | master-data | 币种间在某日的换算比率 |
| Chart of Accounts | 会计科目表 | master-data（共享） | 某账套的科目体系；凭证过账属于 finance |
| Account | 会计科目 | master-data（共享） | 树形结构，支持父子层级与段值编码 |

## 库存词汇

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Stock Move | 库存移动单 | inventory | 计划层"一次移动意图"，承载来源/目的库位、数量、状态 |
| Stock Ledger | 库存流水 | inventory | 不可变的库存变动记录，含移动后余量与成本 |
| Stock Balance | 库存余额 | inventory | 按物料×仓库×库位×批次的当前存量快照 |
| Transfer Order | 调拨单 | inventory | 内部仓库间或库位间的库存移动 |
| Stock Take | 盘点单 | inventory | 账面与实物差异的调整 |
| Batch | 批次 | inventory | 物料的批次/效期管理单元 |
| Serial Number | 序列号 | inventory | 单品序列号追踪 |
| Operation Type | 作业类型 | inventory | 参数化的库存作业分类（收/发/内/制） |
| Reserved Quantity | 预留量 | inventory | 被未完成移动单占用的量 |
| Available Quantity | 可用量 | inventory | 现有量 − 预留量 |
| VMI（Vendor-Managed Inventory） | 供应商寄售 | inventory | 货在己方仓但所有权属供应商，消耗时才转移。见 `inventory/consignment.md` |
| Consignment（寄售/受托代销） | 受托代销 | inventory | 货在代销方仓但所有权属委托方，售出才结算。见 `inventory/consignment.md` |
| Ownership | 所有权维度 | inventory | 库存正交维度（与产品/批次/库位并列），区分自有/VMI/寄售/客供 |

## 采购与销售词汇

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Purchase Order | 采购订单 | purchase | 与供应商签订的采购意向 |
| Purchase Receive | 采购入库单 | purchase | 实际收货的单据 |
| Purchase Invoice | 采购发票 | purchase | 供应商开出的发票，应付凭证来源 |
| Payment | 付款单 | purchase | 向供应商付款的单据 |
| Purchase Return | 采购退货单 | purchase | 向供应商退货 |
| Sales Order | 销售订单 | sales | 与客户确认的销售意向 |
| Sales Delivery | 销售出库单 | sales | 实际发货的单据 |
| Sales Invoice | 销售发票 | sales | 向客户开出的发票，应收凭证来源 |
| Receipt | 收款单 | sales | 从客户收款的单据 |
| Sales Return | 销售退货单 | sales | 客户退货 |
| Three-way Match | 三单匹配 | purchase | 采购订单 → 入库 → 发票 的数量与金额一致性校验 |
| AVL（Approved Vendor List） | 合格供应商名录 | master-data | 经准入审批可参与的供应商资格清单。见 `purchase/supplier-evaluation.md` |
| Settlement | 核销 | purchase/sales | 收付款与发票的多对多核销关系 |

## 财务词汇

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Voucher | 会计凭证 | finance | 一次复式记账的完整记录 |
| Voucher Line | 凭证分录行 | finance | 凭证内的一条借贷分录，只填借方或贷方一侧 |
| Voucher Template | 凭证模板 | finance | 预定义借贷模板，业务单据触发时填充金额 |
| Posting | 过账 | finance | 业务单据生成会计凭证的动作 |
| Debit | 借 | finance | 借方分录 |
| Credit | 贷 | finance | 贷方分录 |
| AP (Accounts Payable) | 应付 | finance | 供应商应付账款 |
| AR (Accounts Receivable) | 应收 | finance | 客户应收账款 |
| Voucher Type | 凭证字 | finance | 凭证分类：收/付/转 |
| Business Type | 业务类型 | finance | 触发凭证的业务类型（采购入库/销售出库/应付发票/应收发票/付款/收款） |
| GL (General Ledger) | 总账 | finance | 按科目汇总的账簿 |
| Accounting Period | 会计期间 | finance | 财务结账的时间区间 |
| Costing Method | 成本核算方法 | finance | 移动加权平均/FIFO/批次等多种方法 |
| Acceptance（Notes） | 承兑汇票 | finance | 银行承兑（银承）/商业承兑（商承），中式票据。见 `finance/treasury.md` |
| Notes Discount | 票据贴现 | finance | 未到期票据向银行兑取现金，贴现息走财务费用 |

## 通用单据状态词汇

> **取值归属**：下表只统一**跨域通用的业务状态语义**。各域 `docStatus`/`approveStatus` 的具体取值集合因域而异（如 purchase/sales 初始态用 DRAFT、inventory 用 CONFIRMED、finance 凭证用 POSTED、assets 用 IN_SERVICE/SCRAPPED 等），以 `domain-design-guidelines.md` §16.2/§16.3 为准；状态码的持久化值（字典 option code/value）归各域 `model/app-erp-<domain>.orm.xml` 的字典定义。本表不重复这些域专属状态，避免与 §16 及 orm.xml 形成第二个真相源。

| 英文 | 中文标准译法 | 说明 |
|------|--------------|------|
| DRAFT | 草稿 | 单据已创建但未生效（多域通用初始态） |
| SUBMITTED | 已提交 | 单据已提交审核 |
| APPROVED | 已审核 | 单据已审核通过 |
| REJECTED | 已驳回 | 单据审核被驳回；**反审核的目标态也是 REJECTED**（见 `domain-design-guidelines.md` §16.4，非初始态 UNSUBMITTED） |
| CANCELLED | 已作废 | 单据作废（多域通用作废态） |
| DONE | 已完成 | 单据已执行完成（如 inventory 移动单终态） |
| OPEN | 待处理/待开始 | 等待处理或开始（如项目进行中、维护请求待受理） |
| IN_PROGRESS | 进行中/执行中 | 正在执行 |
| COMPLETED | 已完成 | 终态：正常完成（如项目/任务/质检/维护访问终态） |
| ON_HOLD | 暂停 | 项目域使用：项目暂停态（可恢复），见 `projects/state-machine.md` |

### 域专属状态（非通用，仅指针）

以下状态因域而异，业务含义见对应域 owner doc，本表不展开以避免重复维护：

- 采购/销售收付款进度（UNPAID/PARTIAL/PAID、UNRECEIVED/PARTIAL/RECEIVED）：见 `purchase/state-machine.md`、`sales/state-machine.md`。
- 库存（CONFIRMED）、盘点（COUNTING）：见 `inventory/state-machine.md`。
- 财务凭证（POSTED）、会计期间（CLOSING/CLOSED_FINAL）：见 `finance/state-machine.md`。
- 资产（IN_SERVICE/IDLE/SCRAPPED/SOLD）、折旧计划（PENDING/EXECUTED/REVERSED）：见 `assets/state-machine.md`。
- 工单（NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/STOPPED/CLOSED 等）、作业卡、预留（UNRESERVED/RESERVED/PICKED/RELEASED）：见 `manufacturing/state-machine.md`、`manufacturing/material-reservation.md`。
- 质检（PENDING/ACCEPTED/CONDITIONAL）、NCR（OPEN/IN_REVIEW/RESOLVED）：见 `quality/state-machine.md`。
- 维护访问（SCHEDULED）、维护请求（ACCEPTED）：见 `maintenance/state-machine.md`。

## 资产/项目/制造/质量/维护词汇

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Asset | 固定资产卡片 | assets | 一项固定资产的价值主记录 |
| Asset Category | 资产类别 | assets | 资产分类，绑定折旧方法与科目映射 |
| Depreciation | 折旧 | assets | 资产价值按期分摊 |
| Capitalization | 资本化 | assets | 在建工程/库存转固定资产 |
| Disposal | 处置 | assets | 资产报废或出售退出使用 |
| Project | 项目 | projects | 业务项目总记录，可作为辅助核算维度 |
| Task | 任务 | projects | 项目下的任务分解 |
| Timesheet | 工时记录 | projects | 成员投入项目的时间记录 |
| BOM | 物料清单 | manufacturing | 产出物料的子件构成与工艺 |
| Work Order | 工单 | manufacturing | 生产订单 |
| Job Card | 作业卡 | manufacturing | 工单下的工序执行卡 |
| Routing | 工艺路线 | manufacturing | 工序序列定义 |
| Workcenter | 工作中心 | manufacturing | 生产单元（产能/费率） |
| Kit Availability | 齐套 | manufacturing | 工单所需子件库存校验 |
| Inspection | 质检单 | quality | 一次质量检验记录 |
| Inspection Template | 质检模板 | quality | 按物料配置的检验标准 |
| Non-Conformance (NCR) | 不符合项报告 | quality | 不合格事件记录与追踪 |
| Corrective Action (CAPA) | 纠正预防措施 | quality | 针对 NCR 的纠正/预防 |
| Conditional Accept | 让步接收 | quality | 不合格经审批降级接收 |
| Equipment | 设备 | maintenance | 需维护的设备实物记录 |
| Maintenance Schedule | 维护计划 | maintenance | 周期性预防维护计划 |
| Maintenance Visit | 维护访问 | maintenance | 一次实际维护执行 |
| Maintenance Request | 维护请求 | maintenance | 报修请求 |
| Downtime | 停机 | maintenance | 设备停机记录 |

## 邻接说明

- 如果某个术语的业务语义发生变化，应同时更新相关 design owner doc 和本词汇表。
- 如果只是字段名、状态码或 API 标识变化，应优先更新模型或接口 owner，而不是把词汇表扩写成字段清单。
