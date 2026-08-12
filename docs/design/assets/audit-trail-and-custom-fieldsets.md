# 资产操作审计轨迹与自定义字段集设计（Asset Audit Trail & Custom Fieldsets）

## 定位

本文基于 Snipe-IT（开源 IT 资产管理系统）调研，设计 nop-app-erp assets 域的**操作审计轨迹（Actionlog 模式）**与**自定义字段集（CustomFieldset）**深化。**当前阶段只产出设计与分析，不进入编码状态**（2026-08-12 用户指示）。

## 来源与背景

- 参考报告：`docs/analysis/erp-survey/2026-08-12-0000-snipe-it.md`（Asset/Depreciation/AssetModel + Actionlog 操作审计 + CustomFieldset 按型号绑定字段 + SCIM 身份集成）。
- 现状基线：`docs/design/assets/` —— 折旧（直线/工作量法 + 折旧计划→凭证，`depreciation-and-posting.md`）、CIP、维护、盘点、价值调整、state-machine；跨域会计日志（`posting-log.md`）。
- 缺口：资产**全生命周期操作轨迹**（谁/何时/从哪到哪）无统一审计视图；资产型号级**扩展字段**无轻量承载（依赖通用 CRUD 字段）。

## 现状 vs Snipe-IT 对照

| 维度 | nop 现状 | Snipe-IT | 差距 |
|------|---------|----------|------|
| 折旧 | 直线/工作量 + 折旧计划→凭证（更完整） | 直线 + 按月结转 | 已覆盖（对照确认） |
| 审计轨迹 | 会计日志（过账/业务动作）+ 通用审计 | Actionlog（签出/签入/借出/维修全记录） | 资产全生命周期审计视图缺失 |
| 扩展字段 | ext 字段/JsonOrmComponent（平台能力） | CustomFieldset（按型号绑定字段集） | 型号级字段集的轻量设计缺失 |
| 身份集成 | 无 SCIM/LDAP | SCIM/LDAP/SAML | 企业身份目录接入（远期） |

## 设计要点

### 1. 资产操作审计轨迹（Actionlog 模式）

- **目标**：资产卡片（ErpAstAsset）的「谁/何时/做了什么/从哪到哪」完整轨迹，与会计日志（财务面）互补——本设计覆盖**实物/生命周期面**。
- **设计**：
  - 审计事件类型：CREATE / UPDATE（字段级变更摘要）/ STATUS_CHANGE（IN_SERVICE→IDLE→SCRAPPED 等）/ MAINTENANCE（维修关联）/ VALUATION（减值/重估）/ DISPOSAL / TRANSFER（组织/位置变更）。
  - 承载：优先复用既有审计基础设施（`posting-log.md` 会计日志同型追加业务审计类型）；若需独立资产审计实体（`ErpAstAssetActionLog`），ORM 变更已获授权（roadmap §8.1，E3 门控）。
  - 查询：`getAssetAuditTrail(assetId)` BizQuery 返回时间轴（对齐 `getDashboardKpi` 只读聚合范式）。
- **与既有审计的关系**：不重复记录业务 action（BizModel 动作审计已有），聚焦**资产状态与归属变化**这一特定视角。

### 2. 自定义字段集（CustomFieldset）

- **目标**：资产型号（ErpAstAssetModel）级扩展字段（如 IT 资产的 CPU/内存/序列号格式），避免为每类资产加列。
- **设计**：
  - 首选平台能力：ext 字段/JsonOrmComponent（平台文档 `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`，`stdDomain=json`/`tagSet=json` 自动生成 JsonOrmComponent）——字段集定义 = 型号记录上声明 ext 字段键集合，实例资产 ext 字段按型号校验。**注：项目 18 域 orm.xml 当前零使用该模式，E3.3 将是首次启用，实现前置须核实平台 ext 字段用法（并入 E3.3 计划 Phase 1 Explore）**。
  - 型号级字段集管理界面（view.xml 定制）作为实现项（roadmap E3.3，plan-first）。
- **否决**：新建通用「字段集元数据表 + 动态表单引擎」（Baserow/NocoBase 式）——与平台 JsonOrmComponent 重复，运维成本高。

### 3. 身份集成（SCIM，远期）

- 对照 Snipe-IT laravel-scim-server：企业身份目录（LDAP/SSO）接入 ERP 用户/员工。
- **设计仅记录触发条件**：出现企业级身份目录需求时评估（平台安全层 + `docs/architecture/multi-company.md` org 维度配合）；当前不立项。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（审计轨迹 + 字段集 + 身份集成触发条件） | ✅ 已完成（本批次） |
| 实现（字段集） | 型号级 ext 字段声明 + 管理界面（零 ORM 变更） | todo（roadmap E3.3，plan-first） |
| 实现（审计轨迹） | `getAssetAuditTrail` + 资产状态/归属变化记录 | todo（roadmap E3.8，独立审计实体 ORM 已授权） |
| 身份集成 | SCIM/LDAP | todo（触发条件驱动） |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | 重复记录业务 action（与既有审计重叠） | 聚焦资产状态/归属变化视角，复用既有基础设施 |
| AP-2 | 新建通用动态表单/字段集引擎 | 用平台 JsonOrmComponent/ext 字段 |
| AP-3 | 为每类资产盲目加 ORM 列 | 型号级 ext 字段集 |
| AP-4 | 审计轨迹无查询入口 | `getAssetAuditTrail` BizQuery 时间轴 |
| AP-5 | 未经需求直接上 SCIM/LDAP | 触发条件门控（远期） |

## 相关文档

- `docs/analysis/erp-survey/2026-08-12-0000-snipe-it.md` — 参考报告
- `docs/design/assets/`（depreciation-and-posting/state-machine/README）— 既有 assets 设计
- `docs/design/finance/posting-log.md` — 会计日志（审计基础）
- `docs/backlog/erp-enhancement-roadmap.md` — 本主题 roadmap