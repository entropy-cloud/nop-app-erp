# Product Baseline Requirements

> **本文定位**：产品基线需求文档——定义"产品是什么、做到什么程度算交付"，不定义"怎么实现"。实现决策（posting 机制、I*Biz 跨域、单向 DAG、状态机、平台组件选型等）见各自域 `docs/design/` 与 `docs/architecture/` owner doc。

## Purpose

Define the product baseline that guides implementation slices. This project may start from small complete loops, but each loop is implemented as formal product behavior rather than temporary or demo-only behavior.

> **开发策略**：本项目采用**完整产品的渐进式演化**——通过 `docs/backlog/` 路线图规划实现顺序，每个切片都是正式产品行为，不是一次性原型。

## 产品基线摘要

nop-app-erp 是基于 Nop Platform 构建的产品化通用 ERP，覆盖 **18 个业务域**（核心 5 域：主数据/库存/采购/销售/财务 + 第一批扩展 5 域：资产/项目/制造/质量/维护 + 第二批扩展 8 域：CRM/CS/HR/APS/合同/DRP/物流/B2B），共 **279 实体**。完整域清单与核心能力见 `product-scope.md`。

核心业务流程覆盖采购到付款（P2P）、销售到收款（O2C）、生产全链路（BOM→工单→领料→完工→质检）。交付时可按需裁剪模块组装（纯商贸/制造/完整产品三档）。

## Product Capabilities

产品能力边界由 18 个业务域承载，每个域的核心能力见 `product-scope.md` 的业务域范围表。本文不重复罗列，以 `product-scope.md` 为唯一权威。

## First Complete Loop

第一个完整循环应证明正式的端到端路径（采购到付款 P2P）：

1. 采购订单创建与审核
2. 采购入库（写库存）
3. 采购发票登记
4. 业财过账（生成应付凭证）
5. 付款与核销

此首循环不是一次性原型。未支持的能力仍是产品领域，其实现顺序在 `docs/backlog/` 路线图跟踪，不在稳定设计文档中。

## Manual Operations Allowed During Early Slices

- 首次启动时 schema 创建（由 Nop ORM 自动建表）
- 无自助配置时默认管理员账号 provisioning

## Development Or Local Integration Substitutes

- 外部依赖（税控/银行/物流/电商）在开发/测试期可用本地模拟路径，仅作为非生产模式，需显式标注。

## Completion Criteria For The First Loop

- 所有必须能力已实现且可测试
- 应用构建运行无错误
- 第一个业务循环端到端测试通过

## Rule

This file owns the implementation-ready product baseline and first complete loop.

Do not duplicate long-term vision from `docs/architecture/project-vision.md` or stable app behavior from `docs/design/app-overview.md`. Put implementation sequencing into `docs/backlog/` or a roadmap, not into every design doc.
