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

## CRM 词汇（crm）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Lead | 线索/商机 | crm | 单实体 + leadType 判别（LEAD 线索 / OPPORTUNITY 商机）；线索经漏斗阶段推进，商机转化触发报价单 |
| Opportunity | 商机 | crm | 已验证的潜在成交机会（leadType=OPPORTUNITY），含预期收入与成交概率 |
| Stage | 漏斗阶段 | crm | 可配置的漏斗阶段记录，驱动线索前移；含团队作用域、默认成交概率、是否赢单阶段 |
| Funnel | 销售漏斗 | crm | 由 Stage 顺序构成的线索推进管道 |
| Lead Status | 线索状态 | crm | 线索生命周期状态字典（NEW/QUALIFIED/CONVERTED/LOST/CANCELLED），持久化值见 orm.xml |
| Source | 线索来源 | crm | 线索来源字典记录 |
| Lost Reason | 丢单原因 | crm | 丢单原因字典记录，LOST 时必填 |
| Event | 活动/事件 | crm | CRM 活动时间线核心（通话/邮件/会议/任务），含时长、日历排程与提醒 |
| Campaign | 营销活动 | crm | UTM 归因的营销活动记录 |
| Team | 销售团队 | crm | 销售团队（负责人与成员），用于活动归属与阶段作用域 |

## 客户服务词汇（customer-service）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Ticket | 客服工单 | cs | 客户发起的服务请求主记录，绑定优先级、SLA、处理人 |
| Ticket Type | 工单类型 | cs | 工单分类，绑定默认优先级与默认 SLA 策略 |
| SLA (Service Level Agreement) | 服务时效协议 | cs | 服务时效规则：解决时限、是否仅计工作日、超时升级通知人 |
| SLA Policy | SLA 策略 | cs | 按工单类型/优先级/团队配置的 SLA 规则记录 |
| Ticket Action | 工单操作日志 | cs | 工单状态变更与操作审计（分派/备注/附件/升级/关闭/取消） |
| Knowledge Base | 知识库 | cs | 可发布知识/FAQ 文章，按工单主题智能推荐 |
| CSAT / NPS / CES | 客户满意度 / 净推荐值 / 客户费力指数 | cs | 满意度回访三类度量指标 |

## 人力资源词汇（human-resource）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Employee | 员工 | hr | 员工主记录（工号、用工状态、部门/职位、银行账户、社保/个税档案）；与系统用户（nop-auth User）分离 |
| Department | 部门 | hr | 组织单元，含上级部门、负责人、默认成本中心 |
| Position | 职位 | hr | 岗位编制（所属部门、职级、职位类别） |
| Employment Contract | 劳动合同 | hr | 合同记录（固定期限/无固定期限/项目制），含试用期、薪酬基数 |
| Leave Request | 休假申请 | hr | 请假单（假别、起止日期、审批） |
| Timesheet | 工时表 | hr | 周期工时汇总，作为项目成本归集来源（与 projects 域共享语义） |
| Attendance | 考勤记录 | hr | 每日出勤记录（签到/签退、迟到早退、旷工、数据来源） |
| Salary | 薪酬记录 | hr | 月度薪资核算结果（应发/扣款/实发），敏感数据，承载审批流 |
| Recruitment | 招聘记录 | hr | 招聘流程（发布→筛选→面试→录用→入职），入职后关联员工 |

## 高级排程词汇（aps）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Operation Order | 工序工单 | aps | APS 排产核心数据结构：从属主工单（WorkOrder）的一道工序，绑定工作中心/设备、计划时间；与执行层 JobCard 分离 |
| Schedule | 排产方案 | aps | 一次排产计算的方案版本（前向/后向模式、展望期区间、状态） |
| Constraint | 排产约束 | aps | 工作中心层面的不可用时段（维护停机、刀具寿命、人员约束） |
| Forward / Backward Scheduling | 前向排产 / 后向排产 | aps | 从计划开工正向填充 / 从交期倒推 |
| ATP / CTP | 可承诺量 / 可承诺产能 | aps | 销售订单审核时经 APS 模拟排产获得可承诺交期 |

## 合同词汇（contract）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Contract | 合同头 | contract | 一份合同主记录（采购/销售/劳动/服务），框架协议层 |
| Contract Line | 合同行 | contract | 合同下的产品/物料行（框架合同为预估总量） |
| Contract Version | 合同版本 | contract | 合同的版本快照（版本号、当前版本标记 isCurrent、版本状态草稿/定稿/签署），审计可追溯 |
| Invoice Plan | 开票计划 | contract | 按合同条款生成的开票安排（预付款/里程碑/月结/完工），自动生成 AP/AR 发票草稿 |
| Consumption Line | 消耗计费行 | contract | 用量计费场景的消耗记录（SaaS 订阅按实际用量结算） |
| Amendment | 合同变更单 | contract | 合同变更，每次变更生成新版本 |

## 分销需求计划词汇（drp）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| DRP Plan | DRP 计划头 | drp | 一次 DRP 运行的主记录（覆盖区间、状态 DRAFT/COMPUTED/APPROVED/EXECUTED） |
| DRP Line | DRP 明细行 | drp | 按物料×目标仓库的净需求计算结果（含建议补货量、补货类型仓间调拨/采购） |
| Reorder Parameter | 仓库补货参数 | drp | 按仓库×物料配置的补货策略（安全库存、提前期、MIN_MAX/PERIODIC/按需） |
| Net Requirement | 净需求 | drp | 当前库存 + 在途在单 − 已分配 − 预测需求 推导的补货需求 |
| Reorder Point | 订货点 | drp | 触发补货的库存阈值 |
| Cross Dock | 越库 | drp | 到货不经入库直接分拨发运的物流策略 |

## 物流词汇（logistics）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| Carrier | 承运商 | logistics | 承运商主数据（顺丰/DHL/京东物流等），绑定网关标识与往来单位 |
| Carrier Config | 承运商配置 | logistics | 每个承运商的参数化配置（服务类型、接口地址、加密凭证、面单格式） |
| Shipment | 发运单 | logistics | 发运订单（关联出/入库单弱指针、承运商、运单号、面单、运费结算） |
| Shipment Parcel | 包裹 | logistics | 物理包裹拆分（一单多包裹，含重量尺寸、独立运单号、面单 URL） |
| Shipment Log | 网关日志 | logistics | 承运商网关交互记录（下单/取面单/追踪/取消的请求响应报文） |
| Carrier Gateway SPI | 承运商网关 SPI | logistics | 三层 SPI（Client/ClientFactory/Registry），新增承运商 = 1 个 bean；技术契约见 `architecture/logistics-integration.md` |

## B2B 集成词汇（b2b）

| 英文 | 中文标准译法 | 所属域 | 说明 |
|------|--------------|--------|------|
| EDI (Electronic Data Interchange) | 电子数据交换 | b2b | 业务单据的标准化电子交换（UBL/X12/EDIFACT/CUSTOM） |
| EDI Format | EDI 格式 | b2b | EDI 格式配置（标准、方向、是否异步），对应可插拔 Provider |
| EDI Doc | EDI 事务/信封 | b2b | 一条 EDI 事务的状态跟踪（方向、关联业务单弱指针、状态机、报文附件） |
| ASN (Advance Shipping Notice) | 提前发货通知 | b2b | 供应商发货前的电子通知（ASN 不直接写库存，入库由 purchase 决定） |
| Code Mapping | 代码映射 | b2b | 内外系统代码映射（物料/伙伴/单位的内部编码 ↔ 外部编码） |
| EDI Log | EDI 日志 | b2b | EDI 交互日志（请求/响应报文、状态、错误），供审计排错 |

## 邻接说明

- 如果某个术语的业务语义发生变化，应同时更新相关 design owner doc 和本词汇表。
- 如果只是字段名、状态码或 API 标识变化，应优先更新模型或接口 owner，而不是把词汇表扩写成字段清单。
