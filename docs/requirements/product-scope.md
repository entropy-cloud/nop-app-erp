# 产品范围

## 产品定位

nop-app-erp 是基于 Nop 平台架构的**产品化通用 ERP 产品**，可快速定制适配各个领域的业务 ERP 系统。完整定位见 `docs/architecture/project-vision.md`，定制能力见 `docs/architecture/customization-capabilities.md`。

## 业务域范围（已确定）

产品内置 18 个业务域，覆盖中等规模 ERP 的进销存+财务一体化+制造全链及外围协作域：

### 核心业务域（进销存+财务，5 个）

| 域 | 工程 | 核心能力 |
|----|------|----------|
| 主数据 | `app-erp-master-data` | 物料/SKU、往来单位、仓库/库位、币种/汇率、科目表、计量单位 |
| 库存 | `app-erp-inventory` | 库存移动单、库存流水、库存余额、调拨、盘点、批次/序列号 |
| 采购 | `app-erp-purchase` | 采购订单、采购入库、采购发票、付款、采购退货、三单匹配 |
| 销售 | `app-erp-sales` | 销售订单、销售出库、销售发票、收款、销售退货 |
| 财务 | `app-erp-finance` | 会计凭证、科目、业财打通、核销、期末结账、成本核算 |

### 扩展业务域（13 个）

#### 第一批扩展（资产/项目/制造/质量/维护，5 个）

| 域 | 工程 | 核心能力 |
|----|------|----------|
| 固定资产 | `app-erp-assets` | 资产卡片、折旧、资本化、处置、价值调整 |
| 项目管理 | `app-erp-projects` | 项目、任务、工时、项目辅助核算 |
| 制造 | `app-erp-manufacturing` | BOM、工单、作业卡、工艺路线、工作中心 |
| 质量管理 | `app-erp-quality` | 质检、NCR 不符合项、CAPA 纠正预防 |
| 设备维护 | `app-erp-maintenance` | 设备、维护计划、维护访问、维护请求、停机记录 |

#### 第二批扩展（外围协作域，8 个）

| 域 | 工程 | 核心能力 |
|----|------|----------|
| 客户关系 | `app-erp-crm` | 线索、商机、客户、CPQ 配置定价 |
| 客户服务 | `app-erp-cs` | 服务工单、售后、客户反馈 |
| 人力资源 | `app-erp-hr` | 员工、薪酬、考勤、招聘 |
| 高级排程 | `app-erp-aps` | 排程优化、产能平衡、替代工艺 |
| 合同 | `app-erp-contract` | 合同起草、审批、电子签、履约 |
| 分销资源 | `app-erp-drp` | 分销网络、补货、跨仓调拨 |
| 物流 | `app-erp-logistics` | 承运、运输、配送、签收 |
| B2B | `app-erp-b2b` | EDI、订单协同、ASN、对账 |

**模块组装**：交付时可按需裁剪——纯商贸客户只组装核心 5 域，制造客户组装核心 + 第一批扩展，完整产品组装全部 18 域。详见 `customization-capabilities.md` 的"模块化组装"。

> **工程命名映射**：物理目录 `module-<domain>/` ↔ 逻辑工程名 `app-erp-<domain>` 的完整映射见 `docs/architecture/domain-module-split-analysis.md §2.0`。

## 当前里程碑（业务逻辑深化与运营成熟度收尾阶段）

> **里程碑对齐说明**：本节由 R2.2（P1-MA3-011）于 2026-07-31 与 `AGENTS.md §当前项目阶段` 对齐——AGENTS.md 已由人工确立当前阶段描述为权威，本节更新是对齐（reconciliation）而非新决策。若审查者认为里程碑框架需重新设计，可降级为 deferred（见 `docs/plans/2026-07-31-0010-2-r2-2-...md` Closure Gates 人工确认门控）。

- 产品摘要：18 业务域 + 跨域通知派发子系统（共 19 个 `module-*/`）ORM 模型已设计完成，Maven 多模块结构由 `nop-cli gen` 生成。项目处于「业务逻辑深化与运营成熟度收尾」阶段。
- 用户：实施方（基于基线定制各领域 ERP）、开发人员（完善模型与生成链路、BizModel/xbiz 与端到端验证）
- 当前已完成：
  - 18 份 `module-<domain>/model/app-erp-<domain>.orm.xml` 权威源模型（447 实体）+ notify 跨域通知派发子系统
  - 18 域完整的代码生成工程骨架（model → codegen → dao → service → web → app → api），156 reactor 模块全绿
  - CRUD 全 18 域（含冒烟测试）
  - 核心业务逻辑：采购订单/销售订单 BizModel 审批-触发-过账三段（M1 全 done）
  - 扩展 13 域业务逻辑（M2/M3 全 done）
  - 业财一体端到端（M4 全 done：采购到付款/销售到收款/期末结账/成本核算/年度结转/坏账准备）
  - 运营成熟度（M5 全 done：会计日志与可观测性/冲销反写闭环/运行监控/通知派发/审批抄送）
  - 报表子系统（nop-report 接线 + 各域种子报表 + AMIS 菜单/页面）
  - 看板子系统（各域后端聚合 API + AMIS 前端）
  - 18 域目录式设计文档 + 全局设计文档 + 架构文档（多次审计验证）
  - 多租户策略明确（按平台标准，不在 orm.xml 预置 tenantId）
- 下一步范围（当前重点）：
  - 看板运行时视觉/浏览器回归
  - 各域细化端到端验证
  - 运行时权限注解落地（R2.7 + MA6，操作级拦截灰度启用）
- 延迟范围：
  - SaaS 多租户启用（待业务确认）
  - 垂直行业扩展工程（待具体客户需求）
  - 外部集成（税控/银行/物流/电商）
- 成功指标（已达成）：
  - 所有 156 模块可独立编译通过（`mvn clean install -DskipTests` 全绿）
  - 全 18 域 CRUD 流程测试通过（含冒烟测试）
  - 业财一体端到端业务循环测试通过（P2P / O2C / 期末结账 / 成本核算 / 年度结转 / 坏账准备）
  - 1903 单元测试 0 failures（与 `docs/backlog/requirement-compliance-roadmap.md §当前基线` 对齐，0.2 基线提取时校正陈旧计数）
- 约束：
  - `model/*.orm.xml` 是 ask-first 保护区域
  - `nop-entropy` 父 POM 必须在 codegen 前构建
  - 跨工程实体引用：业务域 → master-data 通过 `notGenCode="true"` 外部实体引用建立 ORM `<to-one>`（机制 B，单向 DAG）；业务域之间走纯外键 + 弱指针 + `I*Biz`。详见 `docs/architecture/data-dependency-matrix.md §5.6`

## 规则

本文件拥有当前里程碑范围与业务域范围。

不要在此处重复稳定的应用表面和工作流。将当前支持的行为放入 `docs/design/app-overview.md`。

不要在此处重复定制能力细节。放入 `docs/architecture/customization-capabilities.md`。

将实现顺序放入计划中，而非此处。
