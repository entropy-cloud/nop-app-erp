# 看板 KPI 语义层设计（Dashboard Semantic Layer）

## 定位

本文基于 Apache Superset（现代 BI）调研，设计 nop-app-erp 看板子系统的**语义层**：把 KPI/报表的**度量定义**从业务查询逻辑中解耦出来，统一口径、支持嵌入式与协议化暴露。**当前阶段只产出设计与分析，不进入编码状态**（2026-08-12 用户指示）；既有看板（10 域 `getDashboardKpi` + 24 报表 + AMIS 渲染）保持不变。

> **2026-08-13 平台能力核对**：Nop 平台已有 **nop-metadata（联邦式元数据 + BI 语义层，master 已合入完整模块链）** 与 **nop-datav（可视化平台，完整链在 `feat-nop-datav` 分支开发中，master 仅 chart 单模块）**——本文设计须与二者对齐而非重复建设（详见 §1.1）。

## 来源与背景

- 参考报告：`docs/analysis/erp-survey/2026-08-12-0000-apache-superset.md`（semantic_layers 轻量语义层 + embedded-sdk + mcp_service + row_level_security）。
- 现状基线：`docs/design/dashboards.md` —— 各域 `ErpXxxDashboardBizModel.getDashboardKpi` 内联聚合 + 报表（nop-report）+ AMIS 页面 + value-spec 数值断言（value-spec 机制归属测试层，权威见 `docs/testing/e2e-runbook.md`）。
- **平台能力基线（2026-08-13 核实）**：
  - **nop-metadata**（master）：39 实体联邦式元数据中心，含 **BI 语义层**（`NopMetaTableMeasure` 指标 / `NopMetaTableDimension` 维度 / `NopMetaTableJoin` 关联 / `NopMetaTableFilter` 过滤）+ `queryAggregation`/`queryJoinData`/`queryTableData` 查询入口 + 血缘（`NopMetaLineageEdge`）+ 质量（`NopMetaQualityRule/Checkpoint/Result/Score`）+ 对账 + 联邦查询（entity/external/sql 三型表）；`NopMetaModule`（业务模块命名空间）/`NopMetaManifest`（模块清单）与 D2 `business-module-metadata.md` 主题相关。平台文档：`../nop-entropy/docs-for-ai/03-modules/nop-metadata.md`。
  - **nop-datav**（feat 分支）：完整可视化平台——看板/报表（`dashboard-type` DASHBOARD/REPORT）、面板（Panel：CHART/TABLE/METRIC/PIVOT_TABLE/MAP 等）、大屏（Screen）、分享管理、导出（CSV/XLSX）、定时报告（D5-1）、轻量告警（D5-2 面板阈值+冷静期+通知）、**ChatBI**（NL→数据集查询 D6-1 / NL→看板生成 D6-1b / NL→大屏生成 D6-2）、数据权限（DataAuth/RbacAuth）、审计日志。master 当前仅合入 `nop-datav-chart` 前端组件单模块，平台模块链（api/app/codegen/core/dao/meta/service/web）在 `feat-nop-datav` 分支。
- 缺口：应用层各域 KPI 口径散落在 BizModel 内联逻辑，未接入平台语义层（`NopMetaTableMeasure/Dimension`）；无嵌入式暴露模式；nop-datav 平台合入 master 后应用层看板与平台可视化能力的关系未定义。

## 现状 vs Superset 对照

| 维度 | nop 现状 | Superset | 差距 |
|------|---------|----------|------|
| 度量定义 | 各域 `getDashboardKpi` 内联；**平台语义层已具备**（`NopMetaTableMeasure/Dimension` + queryAggregation） | semantic_layers（指标/维度/度量规范化） | 应用层 KPI 未接入平台语义层（口径散落） |
| 报表/可视化 | nop-report（24 报表）+ **nop-datav 平台（feat 分支：看板/大屏/ChatBI）** | charts/dashboards + SQL Lab | 平台可视化能力未合入 master（feat 分支在途） |
| 嵌入式 | AMIS 页面内嵌（框架内） | embedded-sdk + websocket | 外部嵌入/门户场景未设计 |
| 协议化暴露 | GraphQL BizQuery（`getDashboardKpi` 已可查；**不采用 MCP**） | mcp_service | 无缺口（GraphQL 类型定义即 API） |
| 行级安全 | enforcement 数据权限（`roles-and-permissions.md`）；**nop-datav 含 DataAuth/RbacAuth** | row_level_security | 报表场景落实（对照确认/缺口待核） |
| 血缘/质量/对账 | 无（应用层） | 无 | **平台 nop-metadata 已提供**（应用层可复用，不在本文范围） |

## 设计要点

### 0. 与平台能力边界（2026-08-13 核对结论）

- **不重复建设语义层**：Superset semantic_layers 的对应物**平台已实现**（nop-metadata `NopMetaTableMeasure/Dimension/Join/Filter`）。本文的度量目录设计 = 应用层 KPI 口径与平台语义层的**对齐映射**，而非另建运行时。
- **nop-datav 是可视化载体候选**：nop-datav 平台（feat 分支）提供看板/大屏/ChatBI/数据权限——应用层看板长期可迁移/挂载到平台可视化（触发条件：平台合入 master + 应用层迁移需求）；当前 10 域 AMIS 看板保持。
- **ChatBI 归属平台**：NL→查询/看板生成已由 nop-datav 实现（feat 分支），归属 `ai-native-interface.md` 的 AI 消费面，应用层不重复实现（见该文档 §1）。

### 1. 度量目录（Metric Catalog）——对齐平台语义层的口径单一定义源

- **目标**：把「一个 KPI = 口径定义（指标 + 维度 + 过滤 + 单位 + 口径说明）」外化为可审计的目录，BizModel 聚合从「内联魔法数字」改为「引用目录定义」。
- **设计**：
  - **文档层（先行）**：在 `dashboards.md` 增「KPI 度量目录」章节，登记现有 10 域全部 KPI 的规范口径（名称 / 定义公式 / 数据来源（表+过滤）/ 单位 / 口径说明），作为 value-spec 断言与平台语义层映射的单一真相。
  - **平台映射（触发条件驱动）**：运行时配置化 KPI 需求出现时，将目录口径**映射为 nop-metadata 语义层**（`NopMetaTableMeasure`/`NopMetaTableDimension` + queryAggregation），经平台联邦查询承载——**不新建语义层运行时**。
- **价值**：value-spec 数值断言可对照目录审计；新增 KPI 有既定口径书写范式；跨域重复口径（如 finance 应付 vs purchase 应付）显式对齐；未来接入平台语义层时目录即映射输入。

### 2. 嵌入式暴露模式（对照 embedded-sdk）

- **设计**：看板/报表经 AMIS 页面已内嵌（框架内）；**外部嵌入**（客户门户/第三方）场景：以 GraphQL BizQuery（`getDashboardKpi`/报表 `__download`）作为数据契约，外部系统自行渲染（API-first）；不引入嵌入式 SDK 运行时。
- **触发条件**：出现真实的外部嵌入需求（roadmap E3 门控）时补充访问令牌/跨域方案设计。

### 3. 协议化暴露（不采用 MCP）

- 看板/报表数据能力作为 AI 工具暴露：**应用层与平台均不使用 MCP**（用户 2026-08-12 裁决）——API 由 GraphQL 类型定义描述，经 REST + GraphQL 双通道调用；看板 `getDashboardKpi`/报表查询本就是 BizQuery，自动暴露于 GraphQL schema；nop-datav ChatBI（feat 分支）为平台侧 AI 消费形态，同样不依赖 MCP。
- 设计细节归属 `docs/design/ai-native-interface.md` §1（同批姊妹主题），本文不重复——仅登记依赖关系。

### 4. 行级安全在报表场景的落实

- 既有 enforcement 数据权限（role-row-filter）在报表/看板查询的落实需核实（当前 `getDashboardKpi` 为跨域只读聚合，row-filter 是否生效存疑）；nop-datav 平台自带 DataAuth/RbacAuth（feat 分支，合入 master 后应用层看板可复用）。
- **设计动作**：roadmap E3.1b「看板行级安全核实」工作项——核实现有数据权限对 `getDashboardKpi`/报表查询的覆盖 + nop-datav DataAuth 边界，缺口登记（实施属 enforcement 栈扩展，plan-first）。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（度量目录 + 平台能力边界 + 嵌入式 + MCP 否决登记 + 行级安全核实项） | ✅ 已完成（本批次） |
| 实现（度量目录） | `dashboards.md` 增 KPI 度量目录章节（登记现有 KPI 口径） | todo（roadmap E3.1，零代码文档工作项） |
| 实现（行级安全核实） | 数据权限对看板/报表查询覆盖核实 + nop-datav DataAuth 边界 | todo（roadmap E3.1b，核实+登记） |
| 深化 | 平台语义层映射（nop-metadata Measure/Dimension）/ 外部嵌入 / nop-datav 平台挂载（协议化暴露否决 MCP，GraphQL 即 API） | todo（触发条件驱动） |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | 新建 BI 引擎/语义层运行时（与平台重复） | **平台语义层已存在（nop-metadata）**：度量目录=口径文档 + 触发条件映射平台；可视化=既有 AMIS/nop-report（nop-datav 平台合入后评估挂载） |
| AP-2 | KPI 口径继续散落内联无登记 | 度量目录登记为单一真相，value-spec 对照 |
| AP-3 | 未经需求引入嵌入式 SDK 运行时 | API-first 数据契约，触发条件驱动 |
| AP-4 | 报表查询绕过数据权限 | 行级安全核实 + enforcement 栈扩展 + nop-datav DataAuth 复用评估 |
| AP-5 | 跨域重复口径各自定义 | 度量目录显式对齐（如 finance/purchase 应付口径） |
| AP-6 | 引入 MCP 暴露 BI 能力（Superset mcp_service 形态） | **否决**：不采用 MCP；GraphQL 类型定义即 API，REST/GraphQL 双通道（用户 2026-08-12 裁决） |
| AP-7 | 无视平台 nop-metadata 语义层自建度量运行时 | 复用平台 Measure/Dimension + queryAggregation（触发条件驱动映射） |
| AP-8 | 把 nop-datav 平台（feat 分支在途）当作已合入能力依赖 | 依赖 master 实际状态（当前仅 chart 单模块）；平台链合入 master 前不阻塞设计、不引用分支 API |

## 相关文档

- `docs/analysis/erp-survey/2026-08-12-0000-apache-superset.md` — 参考报告
- `docs/design/dashboards.md` — 看板子系统（既有 KPI/报表/渲染）
- `docs/design/ai-native-interface.md` — AI 接口层（协议化暴露否决 MCP + ChatBI 平台归属，同批姊妹主题）
- `docs/design/roles-and-permissions.md` — 数据权限（行级安全）
- `../nop-entropy/docs-for-ai/03-modules/nop-metadata.md` — 平台语义层/元数据中心（master 已合入）
- `../nop-entropy` `feat-nop-datav` 分支（可视化平台，master 未合入）— `nop-datav/model/nop-datav.orm.xml` + `nop-datav-service` 测试
- `docs/backlog/erp-enhancement-roadmap.md` — 本主题 roadmap