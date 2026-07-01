# 功能清单

## 目的

跟踪当前稳定支持的功能地图。每个功能对应一个 owner doc。

本文件不是 backlog。只记录已设计/已支持的功能，不记录实施顺序与状态（那些归 backlog 与 plan）。

## 核心业务功能

| 功能 | 所属域 | Owner Doc | 说明 |
|------|--------|-----------|------|
| 物料与 SKU 管理 | master-data | `master-data/README.md` | 物料主数据、SKU 多单位多 barcode、四档价格 |
| 往来单位管理 | master-data | `master-data/README.md` | 客户/供应商统一主数据 |
| 仓库与库位 | master-data | `master-data/README.md` | 仓库、库位、库位类型 |
| 计量单位与换算 | master-data | `master-data/README.md` | 单位组、换算系数 |
| 币种与汇率 | master-data | `master-data/README.md` | 多币种支持 |
| 会计科目表 | master-data | `master-data/README.md` | 树形科目、弹性段值（共享主数据） |
| 采购订单 | purchase | `purchase/README.md` | 采购意向、部分收货、三单匹配起点 |
| 采购入库 | purchase | `purchase/README.md` | 收货写库存 |
| 采购发票 | purchase | `purchase/README.md` | 应付凭证来源、三单匹配 |
| 付款与核销 | purchase/finance | `finance/ar-ap-reconciliation.md` | 多对多核销（open-item 明细账）、付款进度见 `purchase/state-machine.md` |
| 采购退货 | purchase | `purchase/README.md` | 反向出库 |
| 三单匹配 | purchase | `purchase/three-way-match.md` | 订单-入库-发票一致性校验 |
| 销售订单 | sales | `sales/README.md` | 销售意向、部分发货、赠品/折扣 |
| 销售出库 | sales | `sales/README.md` | 发货扣库存、可用量校验 |
| 销售发票 | sales | `sales/README.md` | 应收凭证来源 |
| 收款与核销 | sales/finance | `finance/ar-ap-reconciliation.md` | 多对多核销（open-item 明细账）、收款进度见 `sales/state-machine.md` |
| 销售退货 | sales | `sales/README.md` | 反向入库、退款 |
| 库存移动单 | inventory | `inventory/state-machine.md` | 三层模型：移动单/流水/余额 |
| 库存调拨 | inventory | `inventory/README.md` | 同仓/跨仓调拨 |
| 库存盘点 | inventory | `inventory/state-machine.md` | 盘盈/盘亏、差异生成移动单 |
| 批次管理 | inventory | `inventory/README.md` | 批次台账、FIFO、效期 |
| 序列号管理 | inventory | `inventory/README.md` | 单品追踪、双向回链 |
| 会计凭证 | finance | `finance/state-machine.md` | 中式复式记账、借贷分录行 |
| 凭证模板 | finance | `finance/posting.md` | 业务类型→凭证模板映射 |
| 业财打通（自动过账） | finance | `finance/posting.md` | 业务单据审核触发凭证生成 |
| 收付款核销 | finance | `finance/state-machine.md` | 应收应付核销闭环 |
| 期末结账 | finance | `finance/state-machine.md` | 会计期间、成本核算、结转损益 |
| 成本核算 | finance | `finance/posting.md` | 移动加权平均/FIFO/标准成本 |

## 扩展业务功能

| 功能 | 所属域 | Owner Doc | 说明 |
|------|--------|-----------|------|
| 固定资产卡片 | assets | `assets/README.md` | 一物一卡、资产类别 |
| 资产折旧 | assets | `assets/state-machine.md` | 直线法/双倍余额递减/工作量法 |
| 资本化入账 | assets | `assets/state-machine.md` | 在建工程/库存转固 |
| 资产处置 | assets | `assets/state-machine.md` | 报废/出售、清理损益 |
| 资产价值调整 | assets | `assets/README.md` | 减值/重估 |
| 项目管理 | projects | `projects/state-machine.md` | 项目生命周期、辅助核算维度 |
| 任务管理 | projects | `projects/state-machine.md` | 任务依赖（DAG）、工时 |
| 工时记录 | projects | `projects/README.md` | 工时成本计入项目 |
| BOM 管理 | manufacturing | `manufacturing/bom-and-routing.md` | 多版本、虚拟件、联副产品 |
| 工艺路线 | manufacturing | `manufacturing/bom-and-routing.md` | 工序、工作中心、标准工时 |
| 工单管理 | manufacturing | `manufacturing/state-machine.md` | 齐套、领料、完工、10 态状态机 |
| 作业卡与报工 | manufacturing | `manufacturing/state-machine.md` | 工序执行、工时记录 |
| 质检管理 | quality | `quality/state-machine.md` | 来料/制程/完工/出货检验 |
| 质检模板 | quality | `quality/README.md` | 按物料配置检验标准 |
| 不符合项（NCR） | quality | `quality/state-machine.md` | 不合格事件追踪 |
| 纠正预防措施（CAPA） | quality | `quality/state-machine.md` | 纠正/预防、效果验证 |
| 让步接收 | quality | `quality/state-machine.md` | 降级使用审批 |
| 设备管理 | maintenance | `maintenance/README.md` | 设备实物记录、状态联动 |
| 维护计划 | maintenance | `maintenance/README.md` | 周期性预防维护 |
| 维护访问 | maintenance | `maintenance/state-machine.md` | 实际维护执行、备件消耗 |
| 维护请求（报修） | maintenance | `maintenance/state-machine.md` | 响应性维护 |
| 停机记录 | maintenance | `maintenance/README.md` | 设备停机、影响排产 |
| 费用报销与员工借款 | finance | `finance/expense-claim.md` | 员工费用报销（价税分离、项目归集）、员工借款/备用金 |
| 资金管理与票据 | finance | `finance/treasury.md` | 中式承兑汇票（应收/应付）、贴现、授信额度、现金预测 |
| 产能需求计划（CRP） | manufacturing | `manufacturing/crp.md` | 工作中心产能建模（日历/按产品/换模）、负荷报表（只读分析，APS 排产在独立 `aps/` 模块） |
| 供应商评分卡 | purchase/master-data | `purchase/supplier-evaluation.md` | 周期评分（维度×公式×权重）、评级→RFQ 联动；AVL 准入归 master-data |
| VMI/寄售/受托代销 | inventory | `inventory/consignment.md` | owner 库存维度、所有权转移单（物权变更，物理不变） |
| 批次召回事件 | quality | `quality/recall.md` | 召回事件聚合（复用 trace-chain/NCR/退货）、NCR 升级 ESCALATED_TO_RECALL |

## 第二批扩展业务域（独立模块）

> 以下域为独立扩展模块（开源调研 Axelor/AureusERP/IDURAR 均有成熟实现），覆盖全部开源 ERP 中存在的业务功能。

| 功能 | 所属域 | Owner Doc | 说明 |
|------|--------|-----------|------|
| 客户关系管理（CRM） | crm | `crm/README.md` | 线索→商机→转化报价单，活动历史（Event/Meeting）、日历、查重、团队分配、丢单原因 |
| 售后服务/客服工单 | customer-service | `customer-service/README.md` | 客服工单（Ticket） + SLA 策略 + 团队分派 + 知识库 + 升级机制 |
| 人力资源管理（HRMS） | human-resource | `human-resource/README.md` | 员工主数据、劳动合同、薪酬核算、考勤、休假/请假、招聘管理、社保/个税 |
| 运输管理（TMS） | logistics | `logistics/README.md` | 发运单 + 三层承运商 SPI（Client/Factory/Registry） |
| 工序级排产（APS） | aps | `aps/README.md` | OperationOrder 工序工单 + 有限产能排产 + 前向/后向排产 |
| 合同全生命周期 | contract | `contract/README.md` | 合同版本、开票计划、用量计费、到期提醒 |
| 分销需求计划（DRP） | drp | `drp/README.md` | 多仓库净需求计算、补货建议（仓间调拨/采购） |
| B2B 集成（EDI/ASN） | b2b | `b2b/README.md`（业务语义）+ 辅助 `architecture/b2b-integration.md`（集成契约） | EDI 格式 SPI + 信封状态机 + ASN 入站 |
| 条码/PDA 扫描 | inventory | `inventory/barcode-integration.md` | PDA 收发货/盘点/领料/质检扫码，条码规则配置 |
| 客户/供应商门户 | portal | `portal/README.md`（**future extension**，非当前基线） | 客户自助查订单/发票/工单；供应商确认PO/提交ASN/发票 |

## 平台复用功能（非 ERP 自建）

| 功能 | 来源 | 说明 |
|------|------|------|
| 用户与认证 | nop-auth | 注册/登录/密码重置、SSO、OAuth2 |
| 角色与权限 | nop-auth | RBAC、数据权限、菜单/按钮级权限 |
| 多租户 | nop-auth | 租户隔离 |
| 字典与序列号 | nop-sys | 字典管理、序列号生成 |
| 工作流 | nop-wf | 单据审批流（按需引入） |
| 报表 | nop-report | 财务报表、库存报表 |
| 文件存储 | nop-file | 附件、单据影像 |
| 定时任务 | nop-job | 维护计划周期触发、期末折旧、兜底扫描 |
| 规则引擎 | nop-rule | 科目映射决策表（可选用） |

## 规则

- 本文件只记录已设计/已支持的功能；新增功能时更新。
- 不记录实施顺序与状态（归 `docs/backlog/`）。
- 不复制字段清单/状态码（归 `model/*.orm.xml`）。
- 每个功能必须指向一个 owner doc。
