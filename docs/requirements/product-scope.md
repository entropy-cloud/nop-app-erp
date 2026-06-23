# 产品范围

## 产品定位

nop-app-erp 是基于 Nop 平台架构的**产品化通用 ERP 产品**，可快速定制适配各个领域的业务 ERP 系统。完整定位见 `docs/architecture/project-vision.md`，定制能力见 `docs/architecture/customization-capabilities.md`。

## 业务域范围（已确定）

产品内置 10 个业务域，覆盖中等规模 ERP 的进销存+财务一体化+制造全链：

### 核心业务域（进销存+财务，5 个）

| 域 | 工程 | 核心能力 |
|----|------|----------|
| 主数据 | `app-erp-master-data` | 物料/SKU、往来单位、仓库/库位、币种/汇率、科目表、计量单位 |
| 库存 | `app-erp-inventory` | 库存移动单、库存流水、库存余额、调拨、盘点、批次/序列号 |
| 采购 | `app-erp-purchase` | 采购订单、采购入库、采购发票、付款、采购退货、三单匹配 |
| 销售 | `app-erp-sales` | 销售订单、销售出库、销售发票、收款、销售退货 |
| 财务 | `app-erp-finance` | 会计凭证、科目、业财打通、核销、期末结账、成本核算 |

### 扩展业务域（资产/项目/制造/质量/维护，5 个）

| 域 | 工程 | 核心能力 |
|----|------|----------|
| 固定资产 | `app-erp-assets` | 资产卡片、折旧、资本化、处置、价值调整 |
| 项目管理 | `app-erp-projects` | 项目、任务、工时、项目辅助核算 |
| 制造 | `app-erp-manufacturing` | BOM、工单、作业卡、工艺路线、工作中心 |
| 质量管理 | `app-erp-quality` | 质检、NCR 不符合项、CAPA 纠正预防 |
| 设备维护 | `app-erp-maintenance` | 设备、维护计划、维护访问、维护请求、停机记录 |

**模块组装**：交付时可按需裁剪——纯商贸客户只组装核心 5 域，制造客户组装全部 10 域。详见 `customization-capabilities.md` 的"模块化组装"。

## 当前里程碑（ORM 模型设计 + 定制能力规划）

- 产品摘要：10 域 ORM 模型已填充（共 145 实体），目录式设计文档已建立，定制能力文档已就位，准备进入 codegen 与端到端验证阶段。
- 用户：实施方（基于基线定制各领域 ERP）、开发人员（完善模型与生成链路）
- 当前已完成：
  - 10 份 `<domain>/model/app-erp-<domain>.orm.xml` 权威源模型（145 实体）
  - 10 域目录式设计文档（README + state-machine + 跨域协作等）
  - 全局设计文档（app-overview/flow-overview/domain-glossary/roles-and-permissions/feature-inventory）
  - 架构文档（system-baseline/module-boundaries/domain-module-split-analysis/customization-capabilities）
  - 多租户策略明确（按平台标准，不在 orm.xml 预置 tenantId）
- 下一步范围：
  - 首域（建议 master-data）通过 `nop-cli gen` 生成工程骨架，验证生成链路
  - 端到端业务循环验证（采购→入库→应付→凭证）
  - 定制能力落地样例（Delta 定制 + 扩展字段各一个）
- 延迟范围：
  - SaaS 多租户启用（待业务确认）
  - 垂直行业扩展工程（待具体客户需求）
  - 外部集成（税控/银行/物流/电商）
- 成功指标：
  - 10 份 orm.xml 的 appName/entityPackageName/maven 坐标互不冲突
  - 首域 codegen 成功生成可构建的工程
  - 第一个业务循环端到端测试通过
- 约束：
  - `model/*.orm.xml` 是 ask-first 保护区域
  - `nop-entropy` 父 POM 必须在 codegen 前构建
  - 跨工程实体引用：业务域 → master-data 通过 `notGenCode="true"` 外部实体引用建立 ORM `<to-one>`（机制 B，单向 DAG）；业务域之间走纯外键 + 弱指针 + `I*Biz`。详见 `docs/architecture/data-dependency-matrix.md §5.6`

## 规则

本文件拥有当前里程碑范围与业务域范围。

不要在此处重复稳定的应用表面和工作流。将当前支持的行为放入 `docs/design/app-overview.md`。

不要在此处重复定制能力细节。放入 `docs/architecture/customization-capabilities.md`。

将实现顺序放入计划中，而非此处。
