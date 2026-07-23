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

## 维护

- 新增业务页面用到本表未收录的中文 label 时，**先扩充本表**，再用一致译法补 `i18n-en:`。
- 本表覆盖 F15 执行时盘点的全部 414 个 view.xml 手写层唯一中文 token。
