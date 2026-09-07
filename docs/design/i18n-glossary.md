# i18n 业务术语英译对照表（Glossary）

> Owner Doc: `docs/design/i18n-glossary.md`
> Source Plan: `docs/plans/2026-07-23-0818-1-f15-i18n-label-internationalization.md`（F15）
> Purpose: 作为 19 业务域手写 `*.view.xml` / `*.action-auth.xml` 文本属性 `i18n-en:` 英文翻译的一致性基准与未来 successor/新页面参考。
> Mechanism: Nop 通过 XML 命名空间 `xmlns:i18n-en="i18n-en"` 声明，元素属性 `i18n-en:label='...'` / `i18n-en:title='...'` / `i18n-en:displayName='...'` 提供英文覆盖；运行时按 locale 选择默认中文 `label` 或英文 `i18n-en:label`。

## 翻译原则

1. **业务术语优先于字面直译**：ERP 概念沿用主流英文 ERP（Odoo / ERPNext / SAP）惯用译法。
2. **缩写一致性**：UoM（计量单位）、CIP（在建工程）、NCP（不合格品）、QC（质检）、SLA、RFQ、ASN、DRP、MRP、CRP、BOM、EDI、CAPA、NCR 保持原缩写。
3. **动词按钮用祈使句首字母大写**（Title Case）：`Approve` / `Submit` / `Carry-Forward`。
4. **名词标签 Title Case**：`Purchase Order` / `Work Center`。
5. **括号说明保留语义**：`(空=全局)` → `(blank=global)`；`(可空)` → `(optional)`；`(必填)` → `(required)`。
6. **冲突解决规则**：当既有 `i18n-en:` 值与本表冲突时，以本表为准修正（本表是冻结基准）。

## 高频核心术语（≥30 项）

| 中文 | English | 语义边界 |
|----|---------|--------|
| 业务组织 | Business Org | 核算/业务归属的组织实体 |
| 所属组织 | Owning Org | 数据所属组织（与 Business Org 同义，区分语义时 Owning 强调归属） |
| 查询条件 | Query Condition | 列表筛选表单标题 |
| 币种 | Currency | 交易币种 |
| 本位币 | Functional Currency | 记账本位币 |
| 核算币种 | Accounting Currency | 核算用币种 |
| 计量单位 | UoM | Unit of Measure |
| 物料 | Material | 物料主数据 |
| 客户 | Customer | 销售客户 |
| 供应商 | Supplier | 采购供应商 |
| 往来单位 | Business Partner | 业务伙伴（客户/供应商统称） |
| 员工 | Employee | HR 员工 |
| 部门 | Department | HR 部门 |
| 职位 | Position | HR 职位 |
| 直接上级 | Direct Supervisor | HR 直属上级 |
| 仓库 | Warehouse | 库存仓库 |
| 库位 | Location | 仓库内库位 |
| 月台 | Dock | 收发货月台 |
| 科目 | Account | 会计科目 |
| 成本中心 | Cost Center | 成本归集中心 |
| 账套 | Accounting Set | 独立核算账套 |
| 会计期间 | Accounting Period | 会计核算期间 |
| 工作中心 | Work Center | 制造工作中心 |
| 工单 | Work Order | 生产工单 |
| 工艺路线 | Routing | 工序路线 |
| BOM | BOM | 物料清单 |
| 项目 | Project | 项目管理 |
| 合同 | Contract | 合同管理 |
| 任务 | Task | 项目/CS 任务 |
| 商机 | Opportunity | CRM 商机 |
| 线索 | Lead | CRM 线索 |
| 资产 | Asset | 固定资产 |
| 设备 | Equipment | 维护设备 |
| 承运商 | Carrier | 物流承运商 |

## 单据生命周期动词

| 中文 | English |
|----|---------|
| 提交 | Submit |
| 撤回提交 | Withdraw Submit |
| 批准 / 批量审批 | Approve / Batch Approve |
| 驳回 | Reject |
| 拒绝 | Refuse |
| 审核 | Audit |
| 反审批 | Un-approve |
| 作废 | Void |
| 过账 | Post |
| 红冲 / 红字冲销 | Red Reverse |
| 结账 | Close Account |
| 反结账 | Un-close |
| 结转 / 确认结转 | Carry-Forward / Confirm Carry-Forward |
| 结案 | Close Case |
| 归档 | Archive |
| 定稿 | Finalize |
| 发布 | Publish |
| 入库 / 出库 | Inbound / Outbound |
| 收货 | Goods Receipt |
| 发料 / 领料 | Issue / Pick |
| 发运 | Shipment |
| 调动 | Transfer |
| 复核 / 复检 | Review / Re-inspect |
| 核销 | Settle |
| 中止 / 暂停 / 终止 | Suspend / Pause / Terminate |
| 阻塞 / 解除阻塞 | Block / Unblock |
| 完成 | Complete |

## 完整机器可读映射

完整 414 个去重 token 的 zh→en 映射见代码生成配套脚本使用的 `i18n_map.json`（本计划执行时由扫描脚本产出，落地后归档）。本表为人工审校基准；新增业务术语须先入本表再用于代码。

## MI.8 手写页批次新增术语

> 登记义务：MI.8 手写页 CAT-4 清剿批新词「先扩本表再使用」（`docs/architecture/i18n-compliance.md` 修复模式对照表 CAT-4 行）。条目式样沿 414 基准既有表格。

### 批 1/2 finance + projects（plan 2026-09-07-0902-3，2026-09-07）

| 中文 | English | 语义边界 |
|----|---------|--------|
| 期末结账向导 | Period Close Wizard | finance 期末结账向导页（F12 Tier C） |
| 前置检查 | Pre-check | preCheck 只读结账前置检查动作 |
| 月度结账 | Monthly Close | closePeriod 月度关账动作 |
| 年度结转 | Annual Carry-Forward | 12 月年度结账分支（结转=Carry-Forward 既有） |
| 年度结账 | Annual Close | 年度结账分支/结果面 |
| 终关 | Finalize | finalizePeriod CLOSED→CLOSED_FINAL 终态锁定 |
| 凭证 | Voucher | 记账凭证（凭证号=Voucher No 沿 view.xml 既有） |
| 已过账凭证 | Posted Vouchers | 凭证计数列标签 |
| 未过账凭证 | Unposted Vouchers | 凭证计数列标签 |
| 总凭证 | Total Vouchers | 凭证计数列标签 |
| 坏账准备 | Bad-Debt Allowance | 应收坏账准备计提面（allowance 系字段） |
| 损益结转 | P&L Carry-Forward | 期末损益结转 |
| 汇兑重估 | FX Revaluation | 期末汇兑重估 |
| 本年利润 | Current Profit | 损益结转中间科目 |
| 未分配利润 | Retained Earnings | 年度结转目标科目 |
| 反结账原因 | Un-close Reason | 反结账审计必填原因（反结账=Un-close 既有） |
| 红冲影响 | Red-Reverse Impact | 反结账红冲预览面（红冲=Red Reverse 既有） |
| 项目盈亏 | Project P&L | projects 项目盈亏页 |
| 项目结算 | Project Settlement | projects 项目结算单（结算=Settle 既有） |
| 工时明细 | Timesheet Detail | projects 工时明细报表（工时=Timesheet 沿 view.xml） |

### 批 2/2 其余 17 域（plan 2026-09-07-1715-2，2026-09-08）

> 登记义务同批 1：本批 17 域 82 文件 CAT-4 清剿引入的新业务术语逐词落账；通用 UI 词（刷新=Refresh、开始/结束日期=Start/End Date、筛选=Filter、渲染报表=Render Report、下载 XLSX/PDF、操作=Actions、金额=Amount、负责人=Responsible Person/Manager、留空=全部→blank = all 等）为本批统一口径，一并登记。

| 中文 | English | 语义边界 |
|----|---------|--------|
| 维护访问 | Maintenance Visit | maintenance 维护访问单/执行向导主实体面 |
| 维护访问执行向导 | Maintenance Visit Execution Wizard | maintenance F12 向导页 |
| 备件消耗 | Spare Part Consumption | maintenance 备件消耗面（记录=Spare Part Usage Records） |
| 设备联动 | Equipment Linkage | maintenance __start 设备状态联动 |
| 维护概览 | Maintenance Overview | maintenance 看板 |
| 设备 OEE | Equipment OEE (Availability × Performance × Quality) | 可用率×性能×质量 |
| 停机 | Downtime | 停机统计/预警（停机统计表=Downtime Summary Report） |
| 客服 | Agent | cs 客服坐席（客服绩效看板=Customer Service Performance Dashboard） |
| 工单（cs 域） | Ticket | cs 客服工单（区别于制造 Work Order） |
| 看板 | Kanban | cs/CRM 看板视图 |
| 分派 | Assign | cs 工单分派（处理人=Assignee） |
| SLA 超时 | SLA Breached | cs SLA 违约面（达标率=Compliance Rate） |
| 质检 | Quality Inspection (QC) | quality 检验面（本期质检数=Inspections This Period） |
| 合格率 | Pass Rate | quality 合格率/趋势 |
| 不合格品处置 | Non-Conforming Product Disposal | quality NCR 处置单 |
| 让步接收 | Concession Acceptance | quality 处置决定（退货=Return/报废=Scrap） |
| 过程能力 | Process Capability | quality SPC 能力分析（充足/尚可/不足=Excellent/Adequate/Inadequate） |
| 控制图 | Control Chart | quality SPC 图（失控=Out of Control） |
| 子组 | Subgroup | quality SPC 采样子组（子组号=Subgroup No.） |
| 极差 | Range | quality SPC 统计量（均值=Mean/标准差=Std Dev） |
| 缺陷率 | Defect Rate | quality SPC p 图 |
| 请假 | Leave | hr 请假面（年假/事假/病假/婚假/产假/丧假=Annual/Personal/Sick/Marriage/Maternity/Funeral Leave） |
| 团队假期日历 | Team Vacation Calendar | hr 部门联合休假日历 |
| 调休 | Comp Time Off | hr 休假类型 |
| 组织架构图 | Org Chart | hr 部门树可视化 |
| 薪酬审批 | Payroll Approval | hr 薪酬发放审批看板 |
| 应发/实发 | Gross / Net Pay | hr 薪酬列（社保=Social Insurance/个税=Income Tax） |
| 标记已发放 | Mark Paid | hr 发放动作（已发放/待发放=Paid/Pending） |
| 三单匹配 | Three-Way Match | purchase 订单-入库-发票匹配（价格差异=Price Variance） |
| 到货及时率 | On-Time Arrival Rate | purchase KPI |
| 应付超期 | AP Overdue | purchase/finance 预警（账龄=Aging） |
| 多级展开 | Multi-Level Explosion | manufacturing BOM 展开树 |
| 工序 | Operation | manufacturing 工艺工序（区别 cs 记录语境） |
| 齐套 | Kit-Complete | manufacturing 齐套待产 |
| 负荷 | Load | manufacturing CRP 负荷（产能=Capacity/负荷工时=Load Hours） |
| 完工 | Completion | manufacturing 完工量/入库（本期完工量=Completed Qty This Period） |
| 委外 | Subcontract | manufacturing 委外面 |
| 库存概览 | Inventory Overview | inventory 看板（库存总值=Total Inventory Value/周转率=Turnover Rate） |
| 安全库存 | Safety Stock | inventory 缺料预警面 |
| 滞销 | Slow-Moving | inventory 滞销库存预警 |
| 盘点 | Stock Take | inventory 盘点三阶段流程（盘盈/盘亏=Gain/Loss） |
| 批次 | Batch | inventory 批次/效期（到期日=Expiry Date） |
| 所有权的转移 | Ownership Transfer | inventory 库存所有权转移 |
| 提前发货通知 | Advance Ship Notice (ASN) | b2b ASN 全称 |
| 报文 | Payload | b2b EDI 报文（请求/响应=Request/Response Payload） |
| 已发足 | Shipped in Full | b2b 行匹配状态（部分发货=Partially Shipped） |
| 确认（时间线） | Acknowledged | b2b EDI 997 确认 |
| 合作伙伴 | Partner | b2b 交易伙伴（追踪号=Tracking No.） |
| 资产盘点 | Asset Stocktake | assets 盘点单面（盘盈/盘亏=Surplus/Deficit） |
| 处置向导 | Disposal Wizard | assets 处置流程（出售/报废/捐赠/毁损=Sale/Scrap/Donation/Damage） |
| 累计折旧 | Accumulated Depreciation | assets 折旧面（折旧额=Depreciation Amount） |
| 清理损益 | Disposal P&L | assets 处置清理（收益/损失=Gain/Loss，账面净值=Net Book Value） |
| 在建工程 | CIP | assets 在建工程余额（缩写沿 414 基准） |
| 活动日历 | Activity Calendar | crm 活动日历（时间线=Activity Timeline） |
| 商机看板 | Opportunity Kanban | crm 商机阶段看板（阶段=Stage/赢单=Win） |
| 线索转化 | Lead Conversion | crm 转化向导（转化=Convert） |
| 转化漏斗 | Conversion Funnel | crm 漏斗报表（归因=Attribution） |
| 成交概率 | Win Probability | crm 看板泳道（高/中概率=High/Medium Probability） |
| 跟进 | Follow-up | crm 活动类型（通话/邮件/会议=Call/Email/Meeting） |
| 主数据概览 | Master Data Overview | master-data 看板 |
| 往来单位检索 | Partner Search | master-data 联合检索 picker |
| 物料价目表 | Material Price List | master-data 报表（往来单位清单=Partner List） |
| 版本对比 | Version Diff | contract 合同版本对比页 |
| 并排对比 | Side-by-Side Diff | contract 双栏比对（已变更/一致=Changed/Unchanged） |
| 收件箱 | Inbox | notify 我的通知页（已读/未读=Read/Unread） |
| 全部标记已读 | Mark All Read | notify 批量动作（站内=In-App/渠道=Channel） |
| 净需求 | Net Requirement | drp 净需求分解报表（建议补货量=Suggested Replenishment Qty） |
| 补货 | Replenishment | drp 补货类型（采购/调拨=Purchase/Transfer） |
| 销售概览 | Sales Overview | sales 看板（销售额=Sales Amount） |
| 订单→开票转化率 | Order-to-Invoice Conversion Rate | sales KPI |
| 应收超期 | AR Overdue | sales 预警（回款面沿 view.xml 既有） |
| 排产甘特图 | Schedule Gantt | aps 排产甘特（排程=Schedule/工序条=Operation Bar） |
| 已计划/进行中 | Planned / In Progress | aps/跨域状态色标 |
| 发运追踪 | Shipment Tracking | logistics 追踪时间线（在途=In Transit/超期未送达=Overdue） |
| 签收 | Delivered | logistics 终态事件 |

## 维护

- 新增业务页面用到本表未收录的中文 label 时，**先扩充本表**，再用一致译法补 `i18n-en:`。
- 本表覆盖 F15 执行时盘点的全部 414 个 view.xml 手写层唯一中文 token。
