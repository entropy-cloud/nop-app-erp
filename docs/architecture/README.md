# Architecture Docs Index

## Purpose

`docs/architecture/` defines the stable cross-cutting technical baseline for `nop-app-erp`.

Use `docs/design/` for app-layer feature and business design. Use `docs/architecture/` for technical structure that spans multiple features.

## Suggested Reading Order

1. `project-vision.md` — 产品定位（产品化通用 ERP）
2. `customization-capabilities.md` — 定制开发能力总览
3. `system-baseline.md` — 技术基线与 10 域模块结构
4. `module-boundaries.md` — 模块依赖方向（DAG）
5. `data-dependency-matrix.md` — 数据依赖矩阵（模块间只读/同步写/弱指针）
6. `domain-module-split-analysis.md` — 10 域拆分决策与命名方案
7. `competitive-comparison.md` — 竞品架构对标（vs Odoo/ERPNext/iDempiere/Tryton 等，超越点论证）
8. more specific owner docs as the project grows

## Owner-Doc Rules

- keep one document responsible for one stable topic
- explain current rationale and constraints, not step-by-step history
- when implementation changes supported architecture, update the owner doc in the same change
- move rejected options and exploration notes to `docs/analysis/`
- cite the relevant app-layer owner doc under `docs/design/` when the technical rule exists to support a concrete product behavior

## Precedence Boundary

- `docs/design/` owns app behavior and feature semantics
- `docs/architecture/` owns technical structure and cross-cutting implementation rules
- if a question is about persistence or schema truth, the model/schema files themselves are authoritative

## Initial Owner Docs

- `project-vision.md` - 产品定位（产品化通用 ERP）、主要用户、不变约束、非目标、里程碑
- `customization-capabilities.md` - 定制开发能力总览（Delta/扩展字段/nop-dyn/模块组装/扩展层/BizLoader + 决策矩阵 + 升级路径）
- `system-baseline.md` - 技术基线、10 域模块结构、多租户策略、Stable Rules
- `module-boundaries.md` - 模块依赖方向（DAG）、跨工程实体关系硬规则
- `data-dependency-matrix.md` - 数据依赖矩阵（模块间只读 R / 同步写 S / 弱指针 P 三类依赖、跨域字段目录、业财一体事务边界、billType 枚举）
- `domain-module-split-analysis.md` - 10 域拆分决策（方案B）、命名与前缀方案、codegen 边界
- `document-engine.md` - DocumentEngine 统一状态机设计（三轴状态、声明式转换规则、异步过账机制）
- `doc-model-design.md` - 单据模型设计（双维度类型、进销存三单链、单据编号规则）
- `l10n-strategy.md` - 本地化策略设计（中国本地化模块、金税接口、增值税发票）
- `api-response-conventions.md` - API 响应约定
- `integration-and-transaction-patterns.md` - 集成与事务模式
- `integration-pattern.md` - 外部 API 集成模式（Webhook 出站/入站）
- `b2b-integration.md` - B2B 集成 / EDI / ASN（集成层，可选 `module-b2b` 工程；EDI 格式 SPI 适用性派发 + 信封状态机 + ASN 入站）
- `competitive-comparison.md` - 竞品架构对标（Odoo/ERPNext/metasfresh/iDempiere/Tryton/Openbravo/MixERP），8 个"超越点"杠杆与汇总表、诚实声明
