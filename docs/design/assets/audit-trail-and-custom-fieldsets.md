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
  - **UPDATE 事件覆盖面登记（P2-4 定稿，2026-08-28 plan 2026-08-28-0219-2）**：触发字段白名单 = 信息字段（name/brandModel/remark/extFieldValues/modelId）+ 财务敏感字段（acquisitionDate/originalValue/residualValue/depreciationMethod/depreciationRate/usefulLifeMonths/categoryId——决定折旧计提口径或记账路由的卡片参数，双源圈定自本节事件语义 + orm 资产表字段语义）；事件 remark 携带变更字段名清单（如「资产信息更新（变更字段：depreciationMethod, residualValue）」）。**与 VALUATION 的边界**：currentValue 变更归 VALUATION 事件（减值/重估语义），不入 UPDATE 白名单（避免单字段双类型）；code/orgId/currencyId/staffId 为标识/组织属性、accumulatedDepreciation/netBookValue 为处理器回写汇总列，均不入清单。
  - 承载：优先复用既有审计基础设施（`posting-log.md` 会计日志同型追加业务审计类型）；若需独立资产审计实体（`ErpAstAssetActionLog`），ORM 变更已获授权（roadmap §8.1，E3 门控）。
  - 查询：`getAssetAuditTrail(assetId)` BizQuery 返回时间轴（对齐 `getDashboardKpi` 只读聚合范式）。
- **与既有审计的关系**：不重复记录业务 action（BizModel 动作审计已有），聚焦**资产状态与归属变化**这一特定视角。

### 2. 自定义字段集（CustomFieldset）

- **目标**：资产型号（ErpAstAssetModel）级扩展字段（如 IT 资产的 CPU/内存/序列号格式），避免为每类资产加列。
- **设计**：
  - 首选平台能力：ext 字段/JsonOrmComponent（平台文档 `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`，`stdDomain=json`/`tagSet=json` 自动生成 JsonOrmComponent）——字段集定义 = 型号记录上声明 ext 字段键集合，实例资产 ext 字段按型号校验。**注：项目 18 域 orm.xml 当前零使用该模式，E3.3 将是首次启用，实现前置须核实平台 ext 字段用法（并入 E3.3 计划 Phase 1 Explore）**。
  - **逻辑删除型号守卫语义（P2-5 定稿，2026-08-28 plan 2026-08-28-0219-2）**：①新绑定拒绝——modelId 设置/变更时目标型号已逻辑删除（delVersion != 0）则拒绝（域内码 `erp.err.ast.asset-model.deleted` 钩子层兜底；标准 Map/GraphQL 入口另由平台 `ObjMetaBasedValidator` 的 deleted-ref 预检先行以通用码拒绝——双层守卫）；②存量豁免——「绑定后型号被删」的存量资产后续保存不按已删型号 extFieldDefs 强制校验，既有 extFieldValues 不触发任何拒绝（含原必填键缺失场景），保证存量保存零可见回归。
  - 型号级字段集管理界面（view.xml 定制）作为实现项（roadmap E3.3，plan-first）。
- **否决**：新建通用「字段集元数据表 + 动态表单引擎」（Baserow/NocoBase 式）——与平台 JsonOrmComponent 重复，运维成本高。

### 3. 身份集成（SCIM，远期）

- 对照 Snipe-IT laravel-scim-server：企业身份目录（LDAP/SSO）接入 ERP 用户/员工。
- **设计仅记录触发条件**：出现企业级身份目录需求时评估（平台安全层 + `docs/architecture/multi-company.md` org 维度配合）；当前不立项。

## 前置调研结论（2026-08-26，E3 整体计划 Phase 1）

- **平台 ext 字段用法核实（活仓核验）**：`stdDomain="json"`（或预定义 domain `json-1000/json-4000`）在 orm.xml 声明后自动生成 `JsonOrmComponent`（实体懒加载组件，GraphQL 序列化为 JSON 值，meta 自动带 `{prop}Component` 属性）——规范文档在 `../nop-entropy/docs-for-ai/02-core-guides/model-first-development.md` §「stdDomain=json 自动生成 JsonOrmComponent」，平台生产级用例 = `NopJobSchedule.jobParams`（`nop-job.orm.xml`）。**meta 层无持久化 ext 机制**（`@BizLoader(autoCreateField)` 只加非持久化计算字段；kv-table 关系型扩展表本身也是 ORM 声明）——「字段集定义 + 实例值持久化」无法零 ORM 落地，E3.3 须 ORM 加性变更（按保护区域 dual-agent-approval 执行，见下）。
- **基线更正**：计划基线中「承载实体 `ErpAstAssetModel` 已就位」**不准确**——assets 域 18 实体无型号实体（活仓核实；最接近的是 `ErpAstAssetCategory` 类别实体）。本计划按 owner doc §2 设计补建 `ErpAstAssetModel` 型号实体作为字段集声明载体。
- **E3.3 承载裁决**：`ErpAstAssetModel.extFieldDefs`（json，字段集键声明：`[{key,label,type,required}]`）+ `ErpAstAsset.modelId`（FK）+ `ErpAstAsset.extFieldValues`（json，实例值）；实例保存时按型号声明校验（非法键/类型拒绝，ErrorCode 范式）。替代方案（否决）：① 类别级承载（语义漂移，类别≠型号）；② kv-table 关系型扩展表（ORM 变更更大、超出试点需要）。残留风险：extFieldDefs JSON 结构无 schema 级约束（由 BizModel 校验兜底）。
- **E3.8 审计承载裁决**：**独立实体 `ErpAstAssetActionLog`**（§8.1 E3.8 授权行）。理由：会计日志建模财务面（过账事件），而资产生命周期事件（suspend/resume/移动/维护/盘点等）多数无凭证关联，复用需在 finance 域为非财务事件扩类型——语义错位。替代方案（否决）：会计日志同型追加业务审计类型（跨域语义污染 + 授权范围不含 finance 侧变更）。事件类型对齐 §1 清单（CREATE/UPDATE/STATUS_CHANGE/MAINTENANCE/VALUATION/DISPOSAL/TRANSFER）。
- **ORM 变更清单**（已随计划 dual-agent-approval）：assets 域新增 `ErpAstAssetModel`、`ErpAstAssetActionLog` 两实体 + `ErpAstAsset` 加 `modelId`/`extFieldValues` 两列 + `erp-ast/audit-event-type` 字典。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（审计轨迹 + 字段集 + 身份集成触发条件） | ✅ 已完成（本批次） |
| 实现（字段集） | 型号级 ext 字段声明 + 管理界面（`ErpAstAssetModel` 新实体 + json 列，经 dual-agent-approval） | ✅ done（E3.3，2026-08-27：`ErpAstAssetModel.extFieldDefs` + `ErpAstAsset.modelId/extFieldValues`（json-4000，dual-agent 批准清单 #0/#1/#2）；BizModel 保存/更新钩子按型号校验（非法键/缺必填/类型不匹配/无型号带值 4 类拒绝，专用 ErrorCode）；型号管理页 view.xml + flux E2E） |
| 实现（审计轨迹） | `getAssetAuditTrail` + 资产状态/归属变化记录（独立审计实体） | ✅ done（E3.8，2026-08-27：`ErpAstAssetActionLog`（dual-agent 批准清单 #3）+ `ErpAstAssetAuditRecorder` 同事务记录，事件类型 7 类全覆盖（CRUD 钩子 CREATE/UPDATE/STATUS_CHANGE/TRANSFER/VALUATION + Processor MAINTENANCE/DISPOSAL）；`getAssetAuditTrail` 时间轴 BizQuery（createTime+id 逆序，from/to 快照 + 回链）） |
| P2 加固 | UPDATE 审计覆盖面扩展（§1 白名单登记）+ 逻辑删除型号双路径守卫（§2 守卫语义） | ✅ done（2026-08-28，plan `2026-08-28-0219-2`：财务敏感 7 字段入 UPDATE 事件 + remark 字段名清单；新绑定拒绝 + 存量豁免；MAINTENANCE/DISPOSAL 事件 JUnit 收口 7 类型全覆盖） |
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