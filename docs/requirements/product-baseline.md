# Product Baseline Requirements

## Purpose

Define the product baseline that guides implementation slices. This project may start from small complete loops, but each loop is implemented as formal product behavior rather than temporary or demo-only behavior.

## 产品基线摘要

nop-app-erp 是基于 Nop Platform 构建的产品化通用 ERP，覆盖 10 个业务域（主数据、库存、采购、销售、财务、固定资产、项目管理、制造、质量管理、设备维护），共 145 实体、82 模块、1096 Java 文件，已完成全部代码生成进入 post-codegen 阶段。核心业务流程覆盖采购到付款（P2P）、销售到收款（O2C）、生产全链路（BOM→工单→领料→完工→质检），通过事件驱动实现业财一体化（posting event + posted flag 兜底扫描）。采用双轴状态机（docStatus + approveStatus）统一跨域状态语义，跨域协作优先走 I*Biz 接口而非 ORM 硬引用，遵循单向 DAG 依赖约束。项目基于 Nop Platform 标准组件（nop-wf、nop-report、nop-rule、nop-message 等）构建，支持按需裁剪模块组装。

## Product Capabilities

- <capability>
- <capability>

## First Complete Loop

The first complete loop should prove the formal end-to-end path:

- <step>
- <step>

This first loop is not a disposable prototype. Unsupported capabilities remain product areas whose implementation order is tracked outside stable design docs.

## Manual Operations Allowed During Early Slices

- <manual operation, e.g. schema creation on first start>
- <manual operation, e.g. default admin provisioning when no self-service provisioning exists>

## Development Or Local Integration Substitutes

- A local or simulated path for an external dependency may exist only as development/test support or as an explicitly documented non-production mode.
- <other substitute>

## Completion Criteria For The First Loop

- All must-have features implemented and testable
- Application builds and runs without errors
- <other criterion>

## Rule

This file owns the implementation-ready product baseline and first complete loop.

Do not duplicate long-term vision from `docs/architecture/project-vision.md` or stable app behavior from `docs/design/app-overview.md`. Put implementation sequencing into `docs/backlog/` or a roadmap, not into every design doc.
