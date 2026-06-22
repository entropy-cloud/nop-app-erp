# 领域设计归属与协作规则

## 目的

定义项目级领域设计归属：哪个业务区域由哪个设计文档负责，跨域协作如何归属。通用产品设计规则属于上游 `../nop-entropy/docs-for-ai/02-core-guides/application-project-docs-and-domain-design.md`，本文件只记录 ERP 项目自己的领域归属映射与本地解释。

## 领域归属映射

ERP 业务按 10 个独立领域工程组织（见 `docs/architecture/domain-module-split-analysis.md`）。每个域对应一个设计目录与一个独立 Maven 工程。

### 核心业务域（进销存+财务）

| 领域 | 设计目录 | 工程 | 权威模型 | 拥有的业务术语 |
|------|----------|------|----------|----------------|
| 主数据 | `master-data/` | `app-erp-master-data` | `model/app-erp-master-data.orm.xml` | 物料、SKU、往来单位、仓库、库位、计量单位、币种、汇率、科目表、科目 |
| 库存 | `inventory/` | `app-erp-inventory` | `model/app-erp-inventory.orm.xml` | 库存移动单、库存流水、库存余额、调拨、盘点、批次、序列号、作业类型、预留量、可用量 |
| 采购 | `purchase/` | `app-erp-purchase` | `model/app-erp-purchase.orm.xml` | 采购订单、采购入库、采购发票、付款、采购退货、三单匹配、应付核销 |
| 销售 | `sales/` | `app-erp-sales` | `model/app-erp-sales.orm.xml` | 销售订单、销售出库、销售发票、收款、销售退货、应收核销 |
| 财务 | `finance/` | `app-erp-finance` | `model/app-erp-finance.orm.xml` | 凭证、凭证分录行、凭证模板、科目、过账、借贷、应付/应收、总账、会计期间、成本核算 |

### 扩展业务域（资产/项目/制造/质量/维护）

| 领域 | 设计目录 | 工程 | 权威模型 | 拥有的业务术语 |
|------|----------|------|----------|----------------|
| 固定资产 | `assets/` | `app-erp-assets` | `model/app-erp-assets.orm.xml` | 资产卡片、资产类别、折旧计划、资产移动、资产价值调整、资本化、报废/出售 |
| 项目管理 | `projects/` | `app-erp-projects` | `model/app-erp-projects.orm.xml` | 项目、任务、工时记录、活动类型、项目辅助核算 |
| 制造 | `manufacturing/` | `app-erp-manufacturing` | `model/app-erp-manufacturing.orm.xml` | BOM、工单、作业卡、工艺路线、工作中心、生产计划、齐套、领料、完工 |
| 质量管理 | `quality/` | `app-erp-quality` | `model/app-erp-quality.orm.xml` | 质检单、质检模板、不符合项（NCR）、纠正预防措施（CAPA）、让步接收 |
| 设备维护 | `maintenance/` | `app-erp-maintenance` | `model/app-erp-maintenance.orm.xml` | 设备、维护计划、维护访问、维护请求、维护团队、停机记录 |

## 跨域流程归属

跨域工作流放在触发它的主业务过程所属域，或放在 `flow-overview.md` 作为全局视图。其他文档只引用或摘要，不重复维护完整规则。

| 跨域流程 | 主 owner | 协作域 | 说明 |
|----------|----------|--------|------|
| 采购入库 → 写库存 | `purchase/` | inventory | 采购入库单审核触发库存入库移动单 |
| 采购发票 → 应付凭证 | `finance/` | purchase | 财务域监听采购发票审核事件，生成应付凭证 |
| 销售出库 → 写库存 | `sales/` | inventory | 销售出库单审核触发库存出库移动单 |
| 销售发票 → 应收凭证 | `finance/` | sales | 财务域监听销售发票审核事件，生成应收凭证 |
| 库存移动 → 存货估值凭证 | `finance/` | inventory | 财务域监听库存移动完成事件，生成存货估值凭证 |
| 资产折旧/处置 → 凭证 | `finance/` | assets | 折旧/资本化/处置触发对应凭证 |
| 工单领料/完工 → 写库存 | `manufacturing/` | inventory | 工单领料出库、完工入库 |
| 工单完工 → 成本结转凭证 | `finance/` | manufacturing | 完工触发存货估值与成本结转凭证 |
| 工单完工 → 质检 | `quality/` | manufacturing | 完工触发完工检验（若 BOM 配置） |
| 采购入库 → 来料质检 | `quality/` | purchase | 采购入库触发来料检验 |
| 销售出库 → 出货质检 | `quality/` | sales | 销售出库触发出货检验 |
| 维修消耗备件 → 出库 | `maintenance/` | inventory | 维护访问消耗备件触发出库移动单 |
| 设备停机 → 影响排产 | `manufacturing/` | maintenance | 停机记录通知制造域调整排产 |
| 工时 → 项目成本凭证 | `finance/` | projects | 工时提交触发项目成本凭证 |
| 业财打通总览 | `flow-overview.md` | 全部 | L3 跨域规则：过账触发、余量校验、快照语义 |

## 跨域归属规则

- **主数据引用**：所有业务域（inventory/purchase/sales/finance）引用主数据时，走 `IErpMd*Biz` 接口查询，不做 ORM 层跨工程 `refEntityName`。
- **库存写入**：只有 inventory 域维护库存余额与流水；其他域通过 `IErpInvStockMoveBiz` 触发库存移动单。
- **凭证生成**：只有 finance 域维护凭证；其他域的业务单据触发凭证生成（通过 finance 域暴露的 `IErpFinAcctDocProvider` 机制）。
- **往来单位余额**：应收/应付余额由 finance 域维护，master-data 域只承载往来单位主数据。
- **科目表共享**：科目表/科目主数据在 master-data 域，但凭证过账逻辑在 finance 域。

## 写作规则

- 保持 `docs/design/<domain>/` 聚焦于本域业务语义、状态机、跨域协作规则。
- 跨域流程在本域文档中只描述"本域视角的触发与结果"，完整流程编排归 `flow-overview.md`。
- 状态码、字典 code、字段名的真相归 `model/app-erp-<domain>.orm.xml`，设计文档不复制。
- 实现落位提示（Entity/BizModel/Processor/I*Biz）遵循上游 `domain-logic-and-ddd.md`，本域文档只保留简短桥接。

## 更新规则

- 领域归属映射变化时更新本文件。
- 单个功能的支持行为变化时，更新所属域的设计文档，而非本文件。
- 新增跨域流程时，在 `flow-overview.md` 登记主 owner 与协作域。
