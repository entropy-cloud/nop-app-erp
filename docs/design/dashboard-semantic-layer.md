# 看板 KPI 语义层设计（Dashboard Semantic Layer）

## 定位

本文基于 Apache Superset（现代 BI）调研，设计 nop-app-erp 看板子系统的**语义层**：把 KPI/报表的**度量定义**从业务查询逻辑中解耦出来，统一口径、支持嵌入式与协议化暴露。**当前阶段只产出设计与分析，不进入编码状态**（2026-08-12 用户指示）；既有看板（10 域 `getDashboardKpi` + 24 报表 + flux 渲染）保持不变。

> **2026-08-26 平台能力核对（master 实态）**：Nop 平台已有 **nop-metadata**（联邦式元数据 + BI 语义层，master 已合入完整模块链）与 **nop-datav**（可视化平台，**全模块链已合入 master**——api/app/chart/codegen/core/dao/meta/service/web + model，2026-08-26 活仓核实；2026-08-13「master 仅 chart 单模块」的记录已过期）——本文设计须与二者对齐而非重复建设（详见 §1.1）。**应用层未挂载 nop-datav**（app 各模块零依赖引用），应用看板不依赖平台可视化能力。

## 来源与背景

- 参考报告：`docs/analysis/erp-survey/2026-08-12-0000-apache-superset.md`（semantic_layers 轻量语义层 + embedded-sdk + mcp_service + row_level_security）。
- 现状基线：`docs/design/dashboards.md` —— 各域 `ErpXxxDashboardBizModel.getDashboardKpi` 内联聚合 + 报表（nop-report）+ flux 页面 + value-spec 数值断言（value-spec 机制归属测试层，权威见 `docs/testing/e2e-runbook.md`）+ **KPI 度量目录**（`dashboards.md` §KPI 度量目录，E3.1 落地的口径单一真相）。
- **平台能力基线（2026-08-26 核实，master）**：
  - **nop-metadata**（master）：39 实体联邦式元数据中心，含 **BI 语义层**（`NopMetaTableMeasure` 指标 / `NopMetaTableDimension` 维度 / `NopMetaTableJoin` 关联 / `NopMetaTableFilter` 过滤）+ `queryAggregation`/`queryJoinData`/`queryTableData` 查询入口 + 血缘（`NopMetaLineageEdge`）+ 质量（`NopMetaQualityRule/Checkpoint/Result/Score`）+ 对账 + 联邦查询（entity/external/sql 三型表）；`NopMetaModule`（业务模块命名空间）/`NopMetaManifest`（模块清单）与 D2 `business-module-metadata.md` 主题相关。平台文档：`../nop-entropy/docs-for-ai/03-modules/nop-metadata.md`。
  - **nop-datav**（master 已合入全链）：完整可视化平台——看板/报表（`dashboard-type` DASHBOARD/REPORT）、面板（Panel：CHART/TABLE/METRIC/PIVOT_TABLE/MAP 等）、大屏（Screen）、分享管理、导出（CSV/XLSX）、定时报告（D5-1）、轻量告警（D5-2 面板阈值+冷静期+通知）、**ChatBI**（NL→数据集查询 D6-1 / NL→看板生成 D6-1b / NL→大屏生成 D6-2）、数据权限（DataAuth/RbacAuth，经平台 `DefaultDataAuthChecker` → `CrudBizModel` 管道同型机制，`TestNopDatavDataAuth`/`TestNopDatavRbacAuth` 测试在 master）、审计日志。
- 缺口：应用层各域 KPI 口径散落在 BizModel 内联逻辑，未接入平台语义层（`NopMetaTableMeasure/Dimension`）；无嵌入式暴露模式；应用层看板未挂载 nop-datav（应用层看板与平台可视化能力的关系见 §0 边界）。

## 现状 vs Superset 对照

| 维度 | nop 现状 | Superset | 差距 |
|------|---------|----------|------|
| 度量定义 | 各域 `getDashboardKpi` 内联；**口径已目录化**（`dashboards.md` §KPI 度量目录，E3.1）；**平台语义层已具备**（`NopMetaTableMeasure/Dimension` + queryAggregation，master） | semantic_layers（指标/维度/度量规范化） | 应用层 KPI 未接入平台语义层（目录=未来映射输入，触发条件驱动） |
| 报表/可视化 | nop-report（24 报表）+ **nop-datav 平台（master 全链：看板/大屏/ChatBI）** | charts/dashboards + SQL Lab | 应用层未挂载 nop-datav（无依赖引用，见 §0 边界与 Non-Goal 重裁） |
| 嵌入式 | flux 页面内嵌（框架内） | embedded-sdk + websocket | 外部嵌入/门户场景未设计 |
| 协议化暴露 | GraphQL BizQuery（`getDashboardKpi` 已可查；**不采用 MCP**） | mcp_service | 无缺口（GraphQL 类型定义即 API） |
| 行级安全 | enforcement 数据权限（`roles-and-permissions.md`）；**nop-datav 含 DataAuth/RbacAuth**（master） | row_level_security | 看板/报表聚合路径覆盖缺口已核实（§4，E3.1b）+ 缺口已登记 enforcement 栈扩展 |
| 血缘/质量/对账 | 无（应用层） | 无 | **平台 nop-metadata 已提供**（应用层可复用，不在本文范围） |

## 设计要点

### 0. 与平台能力边界（2026-08-26 核对结论，master 实态）

- **不重复建设语义层**：Superset semantic_layers 的对应物**平台已实现**（nop-metadata `NopMetaTableMeasure/Dimension/Join/Filter`）。本文的度量目录设计 = 应用层 KPI 口径与平台语义层的**对齐映射**，而非另建运行时。度量目录已落地为 `dashboards.md` §KPI 度量目录（E3.1）。
- **nop-datav 是可视化载体候选**：nop-datav 平台（master 全链）提供看板/大屏/ChatBI/数据权限——应用层看板长期可迁移/挂载到平台可视化。**应用层当前未挂载 nop-datav**（app 各模块零依赖引用，边界 = 平台能力存在 + 应用层不依赖）；挂载/迁移的触发条件 = 应用层出现运行时配置化 KPI / 外部嵌入 / 平台挂载的真实需求（2026-08-26 重裁，原「平台合入 master + 迁移需求」双触发条件的前半已满足、后半未满足——维持移出本 roadmap 深化项）。当前 10 域 flux 看板保持。
- **ChatBI 归属平台**：NL→查询/看板生成已由 nop-datav 实现（master），归属 `ai-native-interface.md` 的 AI 消费面，应用层不重复实现（见该文档 §1）。

### 1. 度量目录（Metric Catalog）——对齐平台语义层的口径单一定义源

- **目标**：把「一个 KPI = 口径定义（指标 + 维度 + 过滤 + 单位 + 口径说明）」外化为可审计的目录，BizModel 聚合从「内联魔法数字」改为「引用目录定义」。
- **设计**：
  - **文档层（已落地，E3.1）**：`dashboards.md` §KPI 度量目录，登记 10 域 + CS 全部 KPI 的规范口径（名称 / 定义公式 / 数据来源（表+过滤）/ 单位 / 口径说明）+ 跨域重复口径对齐表 + 状态字段横向对照，作为 value-spec 断言与平台语义层映射的单一真相。
  - **平台映射（触发条件驱动）**：运行时配置化 KPI 需求出现时，将目录口径**映射为 nop-metadata 语义层**（`NopMetaTableMeasure`/`NopMetaTableDimension` + queryAggregation），经平台联邦查询承载——**不新建语义层运行时**。
- **价值**：value-spec 数值断言可对照目录审计；新增 KPI 有既定口径书写范式；跨域重复口径（如 finance 应付 vs purchase 应付）显式对齐；未来接入平台语义层时目录即映射输入。

### 2. 嵌入式暴露模式（对照 embedded-sdk）

- **设计**：看板/报表经 flux 页面已内嵌（框架内）；**外部嵌入**（客户门户/第三方）场景：以 GraphQL BizQuery（`getDashboardKpi`/报表 `__download`）作为数据契约，外部系统自行渲染（API-first）；不引入嵌入式 SDK 运行时。
- **触发条件**：出现真实的外部嵌入需求（roadmap E3 门控）时补充访问令牌/跨域方案设计。

### 3. 协议化暴露（不采用 MCP）

- 看板/报表数据能力作为 AI 工具暴露：**应用层与平台均不使用 MCP**（用户 2026-08-12 裁决）——API 由 GraphQL 类型定义描述，经 REST + GraphQL 双通道调用；看板 `getDashboardKpi`/报表查询本就是 BizQuery，自动暴露于 GraphQL schema；nop-datav ChatBI（master）为平台侧 AI 消费形态，同样不依赖 MCP。
- 设计细节归属 `docs/design/ai-native-interface.md` §1（同批姊妹主题），本文不重复——仅登记依赖关系。

### 4. 行级安全在报表场景的落实

- **E3.1b 核实结论（2026-08-26，静态表征 + %test 运行时抽样实测）**：
  - **机制表征**：role-row-filter 的两个检查点均不在看板/报表聚合路径上——(a) `CrudBizModel.prepareFindPageQuery` → `AuthHelper.appendFilter` 仅覆盖实体 CRUD 管道（findPage/findList 族）；(b) ORM SQL 层 FILTER marker（`DataAuthEntityFilterProvider`）仅在 EQL `@enable_filter` 装饰或 SQL 对象显式 `enableFilter` 时生成，而 QueryBean→SQL（`DaoQueryHelper.queryToSelectObjectSql`）默认 `enableFilter=false`。
  - **看板/报表查询路径实态**：10 域 `getDashboardKpi` 及各域报表 BizModel（如 `ErpFinReportBizModel`）经 `daoProvider.daoFor(...).findAllByQuery/countByQuery` 与 `ormTemplate` 直连查询 → **两检查点均未经过** → 受限角色（如销售员）在看板 KPI 中聚合到跨用户数据（与 admin 同值）。
  - **例外（管道内路径）**：跨域经 `I*Biz` 实体方法（如 sales/purchase 看板 `arBalance` → `IErpFinArApItemBiz.findOpenItems`）走 CrudBizModel 管道 → 过检查点；finance 域 data-auth 规则为全见设计决定（无 filter），实际无收敛。
  - **运行时抽样实证**：`TestErpSalDashboardRowFilterCoverage`（module-sales）——双销售员种子下，销售员上下文 `getDashboardKpi.orderCount/salesAmount` 聚合他人数据（=admin 同值，缺口实证）；**对照面**同一上下文 `IErpSalOrderBiz.findList`（CRUD 管道）仅见自己单据（检查点在 CRUD 路径生效，证明差异来自查询路径而非规则失效）；灰度 OFF 回归正常。
  - **nop-datav DataAuth 边界**：平台 nop-datav（master 全链）的 DataAuth/RbacAuth 经平台 `DefaultDataAuthChecker` → `CrudBizModel` 管道（与 app 同型机制）——**平台能力存在（master）**，但其保护面是 nop-datav 自身实体（看板/面板/分享等资源），**不覆盖应用层看板 BizQuery**；且应用层未挂载 nop-datav（零依赖引用）→ 应用层看板行级安全不依赖平台能力（边界 = 平台能力存在 + 应用层不依赖）。
- **缺口清单与登记去向（Decision，2026-08-26）**：
  | # | 缺口 | 判定 | 登记去向 | 理由 |
  |---|------|------|---------|------|
  | G1 | 看板 `getDashboardKpi`/trend/topN/alerts 全族直连 DAO 聚合，role-row-filter 不生效（跨用户聚合泄漏） | enforcement 栈扩展 | `docs/backlog/permissions-enforcement-roadmap.md` Follow-up（P1） | 修复形态 = 查询路径改造走管道或 EQL `@enable_filter`，属 data-auth enforcement 覆盖面扩展，与 E2.x 同栈同范式（loginAsRole 账号池/负向 Proof 复用） |
  | G2 | 各域报表 BizModel（`ErpXxxReportBizModel` 数据集/renderHtml）同样直连 DAO，行级过滤不生效 | enforcement 栈扩展 | 同上（并入 G1 项） | 与 G1 同根因同修复形态；报表面另涉 R2-P1 保密字段脱敏缺口（已登记于该 roadmap E-stack），行级与字段级须一并收口 |
  | G3 | 管道内路径（`I*Biz` findOpenItems 族）已过检查点但 finance 全见设计 → 无实际收敛（设计决定，非缺陷） | 登记不修 | `roles-and-permissions.md` E2.3 覆盖矩阵（已有「finance 全见设计决定」记载） | 全见为 E2.3 审计裁决，多组织/orgId 维隔离归独立开关（`erp.multi-company.org-isolation-enabled`，E2.1 Non-Goal） |
- **本计划不实施任何缺口修复**（E3.1b 范围 = 核实 + 登记）。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（度量目录 + 平台能力边界 + 嵌入式 + MCP 否决登记 + 行级安全核实项） | ✅ 已完成（本批次） |
| 实现（度量目录） | `dashboards.md` 增 KPI 度量目录章节（登记现有 KPI 口径） | ✅ done（E3.1，2026-08-26：10 域 + CS 全量登记 + 跨域对齐 + 状态字段对照） |
| 实现（行级安全核实） | 数据权限对看板/报表查询覆盖核实 + nop-datav DataAuth 边界 | ✅ done（E3.1b，2026-08-26：静态表征 + 运行时抽样实测（`TestErpSalDashboardRowFilterCoverage`）+ 缺口清单与登记（G1/G2 → enforcement roadmap Follow-up，G3 → 设计决定）） |
| 深化 | 平台语义层映射（nop-metadata Measure/Dimension）/ 外部嵌入 / nop-datav 平台挂载（协议化暴露否决 MCP，GraphQL 即 API） | todo（触发条件驱动；nop-datav 挂载触发条件 2026-08-26 重裁见 §0） |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | 新建 BI 引擎/语义层运行时（与平台重复） | **平台语义层已存在（nop-metadata）**：度量目录=口径文档 + 触发条件映射平台；可视化=既有 flux/nop-report（nop-datav 平台 master 已合入，挂载触发条件见 §0） |
| AP-2 | KPI 口径继续散落内联无登记 | 度量目录登记为单一真相，value-spec 对照 |
| AP-3 | 未经需求引入嵌入式 SDK 运行时 | API-first 数据契约，触发条件驱动 |
| AP-4 | 报表查询绕过数据权限 | 行级安全核实完成（§4）：缺口已登记 enforcement 栈扩展（G1/G2），修复后须负向 Proof |
| AP-5 | 跨域重复口径各自定义 | 度量目录显式对齐（如 finance/purchase 应付口径） |
| AP-6 | 引入 MCP 暴露 BI 能力（Superset mcp_service 形态） | **否决**：不采用 MCP；GraphQL 类型定义即 API，REST/GraphQL 双通道（用户 2026-08-12 裁决） |
| AP-7 | 无视平台 nop-metadata 语义层自建度量运行时 | 复用平台 Measure/Dimension + queryAggregation（触发条件驱动映射） |
| AP-8 | 把 nop-datav 平台当作应用层已挂载能力依赖 | 依赖应用层实际依赖状态（当前零依赖引用）：平台能力在 master 存在 ≠ 应用层挂载；引用平台 API 前须先裁决挂载触发条件（§0） |

## 相关文档

- `docs/analysis/erp-survey/2026-08-12-0000-apache-superset.md` — 参考报告
- `docs/design/dashboards.md` — 看板子系统（既有 KPI/报表/渲染 + **§KPI 度量目录**）
- `docs/design/ai-native-interface.md` — AI 接口层（协议化暴露否决 MCP + ChatBI 平台归属，同批姊妹主题）
- `docs/design/roles-and-permissions.md` — 数据权限（行级安全 + nop-datav DataAuth 边界注记）
- `../nop-entropy/docs-for-ai/03-modules/nop-metadata.md` — 平台语义层/元数据中心（master 已合入）
- `../nop-entropy` master `nop-datav/`（可视化平台全链，2026-08-26 已合入 master；`nop-datav/model/nop-datav.orm.xml` + `nop-datav-service` DataAuth/RbacAuth 测试）
- `docs/backlog/erp-enhancement-roadmap.md` — 本主题 roadmap；`docs/backlog/permissions-enforcement-roadmap.md` — E3.1b 缺口 G1/G2 登记去向
