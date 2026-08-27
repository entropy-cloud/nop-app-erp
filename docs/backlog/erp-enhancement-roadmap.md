# ERP 增强路线图（ERP Enhancement Roadmap）

> **最后更新**: 2026-08-27
> **来源**: `docs/analysis/erp-survey/2026-08-12-0000-innovation-trends.md`（创新趋势总览）+ 同批次 14 份项目调研报告（erpclaw/twenty/frappe/baserow/frepple/openboxes/fleetbase/inventree/beancount/paperless-ngx/n8n/superset/snipe-it/medusa）
> **前置条件**: `deepening-roadmap.md` ✅ done（11/11，07-20 批次缺口已闭环）；`core-business-roadmap.md` ✅ done；`extended-roadmap.md` ✅ done

## 1. 目的

本路线图覆盖 2026-08-12 erp-survey 批次（14 项目 + 创新趋势）识别的**整个 ERP 的补充设计 + 实现**工作项——涵盖 AI 原生接口、APS 约束排产深化、文档驱动 AP 自动化、库存审计快照/周期盘点、资产审计轨迹/自定义字段集、看板 KPI 语义层、跨域流程编排。

核心原则（用户 2026-08-12 方向澄清）：

- **设计先行**：先补充设计文档（本批次已产出，Milestone E1），再逐项评估实现（E3 门控）。
- **流程编排暂不编码**：跨域流程编排（`cross-domain-flow-orchestration.md`）只保留分析 + 设计文档，不进入编码状态，实现触发条件驱动。
- **不改变平台核心**：所有工作项均为应用层设计与实现。
- **ORM 变更已获批准（2026-08-12）**：本 roadmap E3 工作项涉及的 ORM 变更已获人工授权，授权记录见 §8.1；实施时不再逐项人工确认（双独立子 agent 批准），在整体计划中显式列出即可。
- **平台优先**：n8n/Superset 与平台功能重复（nop-wf/nop-report/nop-datav/nop-ai），仅作设计参考不作为能力缺口。

## 2. Work Item Status

| State | Count |
|-------|-------|
| todo | 1 |
| ready | 0 |
| done | 18 |

## 3. 框架/平台复用

| 能力 | 提供方式 |
|------|----------|
| AI 接入 | `nop-ai`（LLM 接入）+ **nop-datav ChatBI**（NL→查询/看板/大屏生成，平台全链**已合入 master**（2026-08-26 活仓核实）；平台能力，应用层只做业务面 AI 消费/暴露设计） |
| BI 语义层/元数据 | **nop-metadata**（master 已合入：`NopMetaTableMeasure/Dimension/Join/Filter` + queryAggregation + 血缘/质量/对账/联邦查询，平台文档 `03-modules/nop-metadata.md`） |
| 可视化平台 | **nop-datav**（全链已合入 master（2026-08-26 活仓核实）：看板/大屏/面板/分享/导出/定时报告/告警/DataAuth；应用层看板未挂载，触发条件见 `dashboard-semantic-layer.md` §0） |
| 工作流/审批 | `nop-wf`（含人工节点/human-approval 门） |
| 报表/看板 | `nop-report` + 各域 `getDashboardKpi`（AMIS 渲染） |
| 扩展字段 | `JsonOrmComponent` / ext 字段模式（资产自定义字段集首选） |
| 排产引擎 | 既有 `ErpApsSchedulingEngine`（贪心）+ `CrpLoadCalculator`（负荷率派生链） |
| 文档存储 | `nop-file` 模块（文档摄取管道载体） |
| 定时/队列 | `nop-job` + `NopSysEvent`（管道异步步骤） |
| 限流 | `IRateLimiter`（D1 已落地，AI 频控复用） |

## 4. 当前基线

| 域/主题 | 已实现 | 本批次补充设计 |
|---------|--------|----------------|
| AI 接口层 | action 层即 API（BizModel/GraphQL 自动暴露）；enforcement 栈；nop-ai；**平台 ChatBI（nop-datav master）** | `ai-native-interface.md`（GraphQL 类型定义即 API + REST/GraphQL 双通道 + AI 护栏 + human-approval 门 + 原语化裁决；**否决 MCP**；ChatBI 平台归属） |
| aps 排产 | 贪心前/后向排产 + MAINTENANCE 单约束 | `aps/constraint-based-planning.md`（求解器分离/TOC 瓶颈/多约束/预测衔接/KPI） |
| finance 文档入口 | b2b EDI/MFT；AP 三单匹配 | `finance/document-driven-ap-automation.md`（OCR→分类→草稿→三单匹配管道） |
| inventory | 3 层模型 + 一次性 StockTake + 批次追溯 | `inventory/audit-snapshot-cycle-count.md`（快照语义/周期盘点/对账整合） |
| assets | 折旧/CIP/维护/盘点/价值调整 + 会计日志 | `assets/audit-trail-and-custom-fieldsets.md`（Actionlog 轨迹/型号级字段集/SCIM 触发） |
| 看板 | 10 域 KPI + 24 报表 + AMIS + value-spec；**平台语义层 nop-metadata（master）+ 平台可视化 nop-datav（master，应用层未挂载）** | `dashboard-semantic-layer.md`（KPI 度量目录对齐平台语义层/嵌入式 API-first/行级安全核实；**否决 MCP**） |
| 跨域流程编排 | Processor 链 + 事件链 + 审批 wf 链 | `cross-domain-flow-orchestration.md`（必要性分析 + 补充 wf 关联形态，**暂不编码**） |
| 记账内核 | 3 层过账 + 红字冲销 + 平衡校验 | Beancount/ERPClaw 对照确认（E2.1，零代码） |
| 低代码/扩展机制 | Delta + SPI + D4 研究 | Frappe/Baserow/InvenTree/Fleetbase 对照确认（E2.2/E2.3，零代码） |

### 4.1 对照确认与不立项声明（08-12 批次其余借鉴点显式归类）

以下报告借鉴点经核实**已由既有架构覆盖或不属于本期**，显式声明去向（不新增工作项）：

| 借鉴点 | 报告来源 | 归类 |
|--------|---------|------|
| 对象以代码定义/模型版本化 | twenty | 对照确认：nop orm.xml 模型驱动更彻底（`domain-design-guidelines.md`），不立项 |
| GraphQL-first 统一 API | twenty | 对照确认：Nop `IGraphQLEngine` 自动暴露已具备，不立项 |
| 乐观更新 SPA 体验 | twenty | 对照确认：AMIS/flux 渲染交互范式（`frontend-ui-roadmap.md`），不立项 |
| 元数据驱动/自动 REST API/Report Builder | frappe | 对照确认：Nop codegen + nop-report 已具备，不立项 |
| 公式语言（BaserowFormula ANTLR） | baserow | 对照确认：Nop XLang 已覆盖表达式需求；Excel 风格公式字段触发条件=明确需求（E3 之外，不立项） |
| 插件注册/生命周期 | inventree | 对照确认：D4 已裁决 Delta+SPI（`plugin-hot-management-research.md`），不立项 |
| 机器学习集成（模型注册/推理） | inventree | 按用途分派：**ML 分类**并入 E3.5 文档管道分类引擎 SPI（Phase 2）；**ML 需求预测**并入 E1.3 APS 预测衔接（§4）；**ML 通用推理框架**触发条件=预测/识别业务需求（不立项） |
| 报告模板化生成（标签/表单） | inventree | 对照确认：nop-report 打印/标签能力已具备，不立项 |
| 部件参数体系（part 参数/替代件） | inventree | 触发条件驱动：替代料/多参数主数据需求出现时评估（与 C1 Party 抽象/C2 跨境扩展同类主数据扩展，ORM 授权范围外须单独授权），不立项 |
| 扩展索引器/订单规则自动化引擎 | fleetbase | 对照确认：Delta 定制 + 规则设计已覆盖；自动化配置层触发条件=复杂物流自动化需求 |
| 查询引擎（类 SQL 账本查询） | beancount | 对照确认：Nop EQL/BizQuery 已具备，不立项 |
| 金额非零校验（parser 约束清单项） | beancount | 对照确认（E2.1，部分覆盖）：借贷平衡/科目存在校验已覆盖；零金额分录无引擎级显式拒绝（免税单据 TAX_AMOUNT=0 模板行为合法来源）。触发条件=零金额分录造成 GL 噪声/报表失真时经 `IErpFinFactsValidator` 补非零校验，不立项（注记见 `docs/design/finance/posting.md §记账内核审计性对照` #3） |
| 标签打印/导入工具 | snipe-it | 对照确认：nop-report 打印能力，不立项 |
| 模块注册表/ed25519 签名 | erpclaw | 对照确认：Maven 模块制 + `module-meta.json`（D2）已覆盖；签名机制触发条件=插件分发需求 |
| AI 工具注册/可观测性 | n8n | 并入 E1.1（AI 接口层）与既有运行监控（`posting-log.md`），不立项 |
| AI 建表/建页面助手（Kuma 形态） | baserow | 触发条件驱动：AI 生成元数据/页面需求出现时评估（E1.1 只管业务面 AI 消费/暴露，元数据生成不在本期范围），不立项 |
| MCP 服务（ERPClaw mcp/tool_router、Superset mcp_service） | erpclaw / superset | **否决采用**（用户 2026-08-12 裁决：应用与平台均不使用 MCP）——API 由 GraphQL 类型定义描述，经 REST + GraphQL 双通道调用 |
| 轻量语义层（指标/维度规范化） | superset | **对照确认（平台已实现）**：nop-metadata（master 已合入）提供 `NopMetaTableMeasure/Dimension/Join/Filter` + queryAggregation——语义层运行时已有，应用层度量目录为口径对齐映射（`dashboard-semantic-layer.md` §0/§1），不立项 |
| 嵌入式 SDK / ChatBI / 定时报告 / 告警 | superset / n8n | **对照确认（平台已实现，master 已合入）**：nop-datav 平台提供嵌入式看板、ChatBI（NL→查询/看板/大屏）、定时报告、轻量告警、DataAuth——全链已合入 master（2026-08-26 活仓核实）；应用层不重复实现，挂载触发条件已重裁（应用层运行时配置化 KPI/外部嵌入/挂载真实需求，`dashboard-semantic-layer.md` §0） |
| 事件总线（redis/local） | medusa | 对照确认：`NopSysEvent` 主题路由已具备，不立项 |
| 30+ 模块微服务化拆分 | medusa | 对照确认：18 域 DAG 独立部署路线（`domain-module-split-analysis.md`），不立项 |
| 物流核心对象/ledger 同仓 | fleetbase | 对照确认：nop logistics 域状态机 + 独立 finance 域更彻底，不立项 |
| 批次/序列全链路 | openboxes | 对照确认：`trace-chain.md` 已覆盖，不立项 |
| WMS 特征服务（补货/拣货/上架闭环） | openboxes | 对照确认（部分具备）：预留量 + DRP 建议已覆盖补货面；拣货/上架为 WMS 特征非本期（`audit-snapshot-cycle-count.md` 对照表已声明） |
| 跨系统计划同步 SPI（erpconnection 形态） | frepple | 触发条件驱动：跨系统计划同步需求出现时评估（`constraint-based-planning.md` §对照表差距行），并入 E3.4 深化清单候选 |
| 工具链对照（bench vs build.sh/nop-cli） | frappe | 对照确认：工具链对照注记已产出（E2.2 done，`docs/analysis/erp-survey/2026-08-26-0000-lowcode-boundary-and-toolchain-notes.md` §2），不立项 |

## 5. Milestones

### Milestone E1 — 设计补充（2026-08-12 erp-survey 批次，文档已产出）

| Work Item | 状态 | Owner Doc | 依赖 | 复用 |
|-----------|------|-----------|------|------|
| E1.1: AI 原生接口层设计 | done | `docs/design/ai-native-interface.md` (**NEW**) | innovation-trends §1.1 | nop-ai / enforcement 栈 |
| E1.2: 文档驱动 AP 自动化管道设计 | done | `docs/design/finance/document-driven-ap-automation.md` (**NEW**) | paperless-ngx 报告 | nop-file / nop-job / 三单匹配 |
| E1.3: APS 约束排产深化设计 | done | `docs/design/aps/constraint-based-planning.md` (**NEW**) | frepple 报告 | ErpApsSchedulingEngine / CrpLoadCalculator |
| E1.4: 库存审计快照与周期盘点设计 | done | `docs/design/inventory/audit-snapshot-cycle-count.md` (**NEW**) | openboxes 报告 | 3 层模型 / StockTake 链 |
| E1.5: 资产审计轨迹与自定义字段集设计 | done | `docs/design/assets/audit-trail-and-custom-fieldsets.md` (**NEW**) | snipe-it 报告 | JsonOrmComponent / 会计日志 |
| E1.6: 看板 KPI 语义层设计 | done | `docs/design/dashboard-semantic-layer.md` (**NEW**) | superset 报告 | nop-report / dashboards.md |
| E1.7: 跨域流程编排设计（分析+设计，暂不编码） | done | `docs/architecture/cross-domain-flow-orchestration.md` (**NEW**) | medusa/n8n 报告 + 用户澄清 | nop-wf / 三层桥接 |

### Milestone E2 — 对照确认与边界声明（零代码）

| Work Item | 状态 | Owner Doc | 依赖 | 复用 |
|-----------|------|-----------|------|------|
| E2.1: 记账内核审计性对照确认（Beancount 平衡校验清单 vs 既有凭证引擎） | done | `docs/design/finance/posting.md`（补对照段 ✅ §记账内核审计性对照） | E1 批次报告 | 既有凭证引擎 |
| E2.2: 低代码平台边界对照（Frappe/Baserow vs Nop 模型驱动；含工具链对照 bench vs build.sh/nop-cli） | done | `docs/analysis/erp-survey/2026-08-26-0000-lowcode-boundary-and-toolchain-notes.md`（对照注记 ✅） | E1 批次报告 | — |
| E2.3: 扩展机制三方对照（InvenTree/Fleetbase vs NocoBase vs Delta+SPI） | done | `docs/analysis/plugin-hot-management-research.md`（补对照段 ✅ §11） | D4 研究 | — |

### Milestone E3 — 实现（单一整体计划实施，plan-first）

| Work Item | 状态 | Owner Doc | 依赖 | 复用 |
|-----------|------|-----------|------|------|
| E3.1: 看板 KPI 度量目录登记（dashboards.md 章节，口径对齐 nop-metadata 语义层，零代码） | done | `dashboard-semantic-layer.md` §1 | E1.6 | nop-metadata（语义层映射输入） |
| E3.1b: 看板/报表行级安全核实（数据权限覆盖核实 + nop-datav DataAuth 边界 + 缺口登记） | done | `dashboard-semantic-layer.md` §4 | E1.6 | enforcement 栈 / nop-datav DataAuth |
| E3.2: 库存审计快照查询 `getInventorySnapshot` + 对账校验项 | done | `inventory/audit-snapshot-cycle-count.md` §1 | E1.4 | 3 层模型派生 |
| E3.3: 资产型号级 ext 字段集管理界面（零 ORM；前置核实平台 JsonOrmComponent 用法） | done | `assets/audit-trail-and-custom-fieldsets.md` §2 | E1.5 | JsonOrmComponent |
| E3.4: APS 求解器分离 + 瓶颈识别试点（默认贪心保持） | done | `aps/constraint-based-planning.md` §1-2 | E1.3 | IApsSchedulingSolver |
| E3.5: 文档摄取管道（前置 OCR/nop-file 调研；ORM 已授权） | done | `finance/document-driven-ap-automation.md` | E1.2 | nop-file / nop-job |
| E3.6: AI 接口层（前置 GraphQL schema 工具发现可用性调研；ORM 已授权） | done | `ai-native-interface.md` | E1.1 | nop-ai / IGraphQLEngine |
| E3.7: 跨域流程编排试点（**暂缓**，触发条件驱动） | todo | `cross-domain-flow-orchestration.md` | E1.7 + 触发条件 | nop-wf |
| E3.8: 资产操作审计轨迹实现（`getAssetAuditTrail`；ORM 已授权） | done | `assets/audit-trail-and-custom-fieldsets.md` §1 | E1.5 | 会计日志 |

## 6. Work Item Details

| Work Item | Deliverables |
|-----------|-------------|
| E1.1 | `ai-native-interface.md`（AI 工具暴露形态裁决 + 安全护栏表 + human-approval 门 + 原语化裁决）✅ 已产出 |
| E1.2 | `document-driven-ap-automation.md`（摄取管道图 + 分类引擎两阶段 + 邮件摄取 + 审计追溯）✅ 已产出 |
| E1.3 | `constraint-based-planning.md`（求解器分离接口 + TOC 瓶颈识别 + 多约束扩展 + 预测衔接 SPI + KPI）✅ 已产出 |
| E1.4 | `audit-snapshot-cycle-count.md`（快照=派生视图 + 周期盘点任务 + 对账整合）✅ 已产出 |
| E1.5 | `audit-trail-and-custom-fieldsets.md`（Actionlog 式审计事件类型 + 型号级字段集 + SCIM 触发）✅ 已产出 |
| E1.6 | `dashboard-semantic-layer.md`（KPI 度量目录 + 嵌入式 API-first + 行级安全核实项）✅ 已产出 |
| E1.7 | `cross-domain-flow-orchestration.md`（必要性判定矩阵 N1-N6 + 补充 wf 关联形态 + 全局协调器 + 反模式）✅ 已产出 |
| E2.1 | posting.md 补「记账内核审计性对照」段（平衡校验约束清单核对 + 结论登记）✅ 已产出（9 项清单：7 覆盖 / 1 不适用 / 1 部分覆盖登记触发条件） |
| E2.2 | erp-survey 对照注记（Frappe/Baserow 与 Nop 模型驱动路径差异结论，引用既有报告）✅ 已产出（`2026-08-26-0000-lowcode-boundary-and-toolchain-notes.md`，含工具链对照 §2） |
| E2.3 | plugin-hot-management-research.md 补三方对照段（InvenTree registry / Fleetbase extensions 与既有裁决）✅ 已产出（§11，结论：佐证无分歧） |
| E3.1 | dashboards.md 增 KPI 度量目录章节（10 域 KPI 口径/数据来源/单位登记，口径=数据驱动数值断言覆盖域）；目录作为未来 nop-metadata 语义层（Measure/Dimension）映射输入 ✅ 已落地（11 域全量登记 + 跨域对齐表 + 状态字段对照，2026-08-26） |
| E3.1b | 核实 enforcement 数据权限（role-row-filter）对 `getDashboardKpi`/报表查询的覆盖 + nop-datav DataAuth/RbacAuth 边界（master 实态），缺口登记（实施属 enforcement 栈扩展）✅ 已落地（静态表征 + 运行时实测 + G1/G2/G3 缺口登记，2026-08-26） |
| E3.2 | `getInventorySnapshot` BizQuery + 期末对账一致性校验项（零 ORM）✅ 已落地（派生视图 + `erp-inv-stock-check` 作业，2026-08-26） |
| E3.3 | 型号级 ext 字段键声明 + 管理界面（view.xml 定制，零 ORM）✅ 已落地（ORM 承载经 dual-agent 批准（meta 层无持久化 ext 机制）；型号实体 + 校验 + 页面，2026-08-27） |
| E3.4 | `IApsSchedulingSolver` 接口 + 贪心默认实现保留 + `scheduleToc` 瓶颈识别试点 ✅ 已落地（config 切换 + SchedulingResult 扩展，2026-08-27） |
| E3.5 | OCR 引擎选型调研 → 管道实现（文档 → 解析 → 草稿发票 → 三单匹配）；分类引擎 Phase 1 规则优先 + Phase 2 ML 经 SPI 注入（`IErpFinAcctDocProvider` 注入范式）；ORM 变更（文档引用/解析字段）已授权 ✅ 已落地（SPI 双引擎 + 端到端管道 + 人工门，config-gate 默认关闭，2026-08-27；**P1 修复加固**（multi-audit P1-1/2/3，plan `2026-08-27-2006-1`，2026-08-27）：步骤失败 FAILED+FAIL 轨迹 REQUIRES_NEW 独立事务落账（外层回滚后存活，retry 守卫同步路径可达）+ `processPending` 逐文档独立事务失败隔离（毒文档不回滚先行成功/不阻断后续/FAILED 终态退出扫描）+ upload 门补齐「默认关闭零暴露」成立） |
| E3.6 | GraphQL schema 对 AI 工具发现的可用性调研（introspection/类型描述）→ AI 工具消费约定 + 护栏（ORM actorType 字段已授权）。**不采用 MCP**（用户 2026-08-12 裁决：GraphQL 类型定义即 API，REST/GraphQL 双通道）✅ 已落地（最小落地集 6 项冻结清单逐项，actorType 裁决不落地，2026-08-27；**P1 修复收口**（multi-audit P1-6，plan `2026-08-27-2006-3`，2026-08-28）：19 分域 app `schema-introspection.enabled` 翻 `false`，全部 20 应用工件默认关闭，owner doc「显式关闭」断言对齐为真） |
| E3.7 | **暂缓**：触发条件 = 真实 nop-wf 跨域编排需求（人工门控/超时/整链追溯）出现后按 E1.7 设计实现 |
| E3.8 | 资产状态/归属变化审计记录 + `getAssetAuditTrail` 时间轴查询（优先复用会计日志；独立审计实体 ORM 已授权）✅ 已落地（独立实体裁决 + 7 事件类型全覆盖，2026-08-27） |

## 7. 依赖图

```mermaid
graph LR
    subgraph E1[E1 设计补充]
        E11[E1.1 AI 接口层]
        E12[E1.2 文档 AP 管道]
        E13[E1.3 APS 约束排产]
        E14[E1.4 库存快照/盘点]
        E15[E1.5 资产审计/字段集]
        E16[E1.6 看板语义层]
        E17[E1.7 跨域流程编排-设计]
    end
    E11 --> E36[E3.6 AI 接口层实现]
    E12 --> E35[E3.5 文档管道实现]
    E13 --> E34[E3.4 APS 求解器试点]
    E14 --> E32[E3.2 库存快照查询]
    E15 --> E33[E3.3 资产字段集]
    E15 --> E38[E3.8 资产审计轨迹]
    E16 --> E31[E3.1 KPI 度量目录]
    E16 --> E31B[E3.1b 行级安全核实]
    E17 -.触发条件.-> E37[E3.7 流程编排试点-暂缓]
```

## 8. 横切关注点

- **设计先行纪律**：E1 全部为设计文档（已产出，独立审查后保持 done）；E2 为零代码对照确认；E3 以**单一整体计划**实施（§9 规则 2），**不允许直接从设计文档跳到编码**。
- **流程编排暂缓**：E3.7 不进入编码状态（用户 2026-08-12 指示）；触发条件未满足前保持 todo。
- **ORM 授权**：E3.5（文档引用/解析字段）、E3.6（actorType 字段）、E3.2（如物化快照表）、E3.8（独立审计实体）涉及的 ORM 变更**已获人工批准**（§8.1 授权记录），实施时在整体计划中显式列出变更清单，按 dual-agent-approval 规则执行（双独立子 agent 批准）；E3.3 首选 ext 字段规避 ORM。
- **平台优先**：任何实现项先核实 Nop 平台能力（nop-ai/nop-wf/nop-file/nop-job/nop-metadata/nop-datav），不重建。
- **平台能力跟踪（2026-08-26 活仓复核，替换 2026-08-13 过期核对记录）**：nop-metadata（BI 语义层/元数据中心）**master 已合入**——应用层语义层需求复用平台（E3.1 度量目录=口径对齐映射，已落地）；nop-datav（可视化平台含 ChatBI/DataAuth）**全链已合入 master**（2026-08-19~23 收口提交含「datav 630 tests 全绿」，2026-08-26 活仓核实 master HEAD 跟踪全模块链——2026-08-13 记录「完整链在 feat 分支、master 仅 chart 单模块」已过期）；应用层看板**未挂载 nop-datav、零依赖**，挂载触发条件已重裁（应用层运行时配置化 KPI / 外部嵌入 / 挂载真实需求出现，`dashboard-semantic-layer.md` §0）。
- **AI 接口裁决（用户 2026-08-12）**：应用系统提供给 AI 的接口**不通过 MCP**，整个 Nop 平台均不使用 MCP；API 由 GraphQL 类型定义描述，经 REST 与 GraphQL 两种方式调用。所有 E1/E3 设计与实现项遵守此裁决。
- **业务逻辑单一真相**：AI/管道/协调器只编排，不复制业务规则（对齐 processor-per-mutation 契约）。
- **与既有 roadmap 边界**：deepening-roadmap（07-20 批次）已 done 不重复；frontend-ui-roadmap 保持独立；本 roadmap 只登记 08-12 批次识别项。

### 8.1 ORM 变更授权记录（2026-08-12 人工批准）

| 涉及工作项 | 授权范围 | 说明 |
|-----------|---------|------|
| E3.5 | 文档摄取管道相关字段/实体（文档引用、解析结果字段、文档处理日志） | 实施时在整体计划中列出具体 schema |
| E3.6 | AI 操作审计标识字段（`actorType=AI`） | 仅当需区分 AI 发起的操作时 |
| E3.8 | 资产审计实体 `ErpAstAssetActionLog`（如复用会计日志不满足时） | 首选零变更路径 |
| E3.2 | 库存快照物化表（如触发条件满足时） | 默认派生视图零 ORM |
| E3.7 | 业务实体 `flowInstanceId` 引用（如触发条件满足时） | **暂缓**，解除暂缓后生效 |
| E1.4（周期盘点） | CycleCountTask 实体化或 StockTake 扩展（如触发条件满足时） | 触发条件驱动 |

> **授权约束**：变更仍须遵守「ORM 源模型唯一真相 → codegen 增量重新生成（`mvn clean install -DskipTests`）」纪律；不手改生成代码；schema 细节在整体计划 Phase 1 Explore 中定稿并显式列出。

## 9. Rules

1. 遵循 `00-roadmap-authoring-guide.md` 状态跟踪；状态只存在于工作项。
2. **E3 以单一整体计划实施**（覆盖 E3.1-E3.8 的全部非暂缓项）：一份 `docs/plans/` 计划，含独立草案审查与独立结束审计；E3.7 触发条件满足后并入或另立计划。**不逐项起草独立计划**（用户 2026-08-12 指示）。
3. 涉及 ORM 变更的字段/实体在整体计划中显式列出（授权见 §8.1），按 dual-agent-approval 规则执行（双独立子 agent 批准）。
4. 发现新的平台能力复用机会时更新 §3。
5. 不将 roadmap 编写为实施规格；细节在计划与 owner doc 中定义。
6. 流程编排（E1.7/E3.7）的编码须等待用户显式解除暂缓。

## Follow-up Backlog

> 2026-08-27 由 erp-enhancement mission 审计轮登记（multi/open 两审计 P2 项分流，`docs/plans/00-plan-authoring-and-execution-guide.md` 反松弛规则：P2 不单独驱动 remediation plan）。每项带来源审计路径保持可追溯；标注「随 plan 顺带」的项在对应 P1 修复计划执行时可顺带收口（须在计划中补记范围变更理由），不构成该计划的完成义务。

- [x] E3.5 重复上传幂等缺失（无 fileId/内容哈希去重，同文件重传 → 第二 RECEIVED → 第二草稿；去重责任是否后移三单匹配未落文档）（来源：`docs/audits/2026-08-26-2226-multi-audit-erp-enhancement.md` P2-1；建议随 plan 2026-08-27-2006-1 顺带评估） ✅ plan `2026-08-28-0219-1`（方案 A 应用层守卫：fileName+非终态+SHA-256 内容比对，`ERR_AP_DOC_DUPLICATE_UPLOAD` 指向既有文档；并发残留风险 + 最终权威去重 = 三单匹配边界登记 owner doc §P2 加固裁决登记）
- [ ] E3.5 `getInventorySnapshot` 绕过 role-row-filter 未登记入 G1 缺口族（`dashboard-semantic-layer.md` §4 清单补录）（来源：multi-audit P2-2；归 permissions-enforcement G1 族同机制收口）
- [x] dict 死状态 `ARCHIVED`（`erp-fin/ap-doc-status`）全仓无 writer（来源：multi-audit P2-3；lesson-10 家族） ✅ plan `2026-08-28-0219-1`（裁决方案 A：保留 dict 值 + writer 归属 = 邮件摄取落地时自动归档，触发条件显式登记 owner doc §P2 加固裁决登记）
- [ ] E3.3 资产 UPDATE 审计白名单过窄：`depreciationMethod/depreciationRate/acquisitionDate/categoryId` 等财务敏感字段变更零审计事件（来源：multi-audit P2-4）
- [ ] 逻辑删除资产型号可被绑定：`ErpAstAssetModel` `useLogicalDelete=true` 但校验钩子不拒已删型号（来源：multi-audit P2-5）
- [x] E3.5 分类引擎确定性/上界瑕疵：≤2000 伙伴加载无排序首匹配不确定 + 超量静默落人工门；`String.valueOf(null)` → `"null"` 幻影路径（来源：multi-audit P2-6） ✅ plan `2026-08-28-0219-1`（id 升序确定性排序 + `findPageByQuery` 使上界真实生效【执行发现 `findAllByQuery` 忽略 QueryBean.limit，原 setLimit(2000) 实为无界加载】+ 超限 WARN 可观测 + null 幻影收口；Proof = `TestErpFinApDocRuleClassifierDeterminism` 4 用例）
- [x] E3.5 OCR 默认引擎吞异常零日志 + PDFBox 全内存无页数上限（来源：multi-audit P2-7） ✅ plan `2026-08-28-0219-1`（catch WARN `erp-fin-ap-doc-pdf-extract-failed`（fileName/mimeType/reason，损坏件与扫描件可区分）+ 页数上限 `erp-fin.ap-doc-pdf-max-pages` 默认 50 config-gate 超限 WARN 落人工门；Proof = `TestErpFinTextExtractOcrEngineObservability` 3 用例）
- [x] E3.5 上传入口输入契约松：无 MIME/扩展名白名单；`fileName` 未按列精度 200 截断；非法 base64 抛裸 `IllegalArgumentException`（来源：multi-audit P2-8；建议随 plan 2026-08-27-2006-1 顺带） ✅ plan `2026-08-28-0219-1`（白名单 pdf/txt/csv/json/xml/png/jpg/jpeg + MIME 对齐；超长拒绝 `file-type-not-allowed`/`file-name-too-long`/`invalid-base64` 三专属错误码；Proof = `TestErpFinApDocumentPipeline.testUploadContractNegativePaths`）
- [x] E3.5 错误码契约错位：`ERR_AP_DOC_PARSE_FAILED` 定义未用，PARSE 失败以 `ERR_AP_DOC_DRAFT_FAILED` 面客（来源：multi-audit P2-9；建议随 plan 2026-08-27-2006-1 顺带） ✅ plan `2026-08-28-0219-1`（`fail()` 按 step 映射：PARSE → parse-failed / DRAFT 与 CLASSIFY（复用裁决登记）→ draft-failed）
- [ ] E3 批新增 5 个对外 action 零 `@Description`（违反 `ai-native-interface.md:72` 自登记约定）（来源：multi-audit P2-10）
- [x] E3.5 限流默认 10rps 对所有调用方生效 vs owner doc「人类用户面默认无限流」措辞失实（来源：multi-audit P2-11） ✅ plan `2026-08-28-0219-1`（`ai-native-interface.md` 最小落地集 item 4 措辞对齐实际语义：限流落点 = 管道 upload 单入口、全部调用方默认 10rps、0=不限流；默认值未改）
- [ ] 悬空引用 ×2：`TestErpAiIntrospectionEnabled` 注释指 `ai-native-interface.md` 不存在的实现注记；E3 计划「IBiz 同步」空指（来源：multi-audit P2-12）
- [ ] `data-dependency-matrix.md` 缺登记：fin→md `ErpMdPartner` 只读边 + aps→mfg `IErpMfgCapacityProvider` SPI 边（§2.4 矩阵行）（来源：multi-audit P2-13；与开放审计 P2-C fin→pur 行合并一次收口）
- [ ] E3 批测试充分性缺口：`ap-document.batch.xml` + job yaml 接线零覆盖；MAINTENANCE/DISPOSAL 审计事件零 JUnit；TOC 阈值等值边界未测；`TestErpInvSnapshotAndStockCheck` 零值等价断言空转；`TestErpFinApDocumentPipeline` `enableActionAuth=FALSE`（来源：multi-audit P2-14）——**fin 部分已收口** ✅ plan `2026-08-28-0219-1`（`TestErpFinApDocBatchWiring` 落地时发现并修复 batch.xml 两处运行时缺陷：接口名 inject unknown-bean + 恒非空 loader 无限 chunk 循环，改 orm-reader 触发扫描 + bean id 注入；job yaml 注册形态断言；enableActionAuth=FALSE 理由注记 + 鉴权拒绝路径由 P2-E E2E 承载）；非 fin 部分归 plan 2（assets 等）
- [x] `ErpFinBankReconAutoReverseHelper.java:117` 生产代码 `LocalDate.now()` 违反 CoreMetrics 约定（checker R7 不覆盖该形态）（来源：multi-audit P2-15；随下次 fin 域触碰修复） ✅ plan `2026-08-28-0219-1`（`CoreMetrics.currentDate().withDayOfMonth(1)` 等价替换，行为零变化，`TestErpFinBankReconAutoReverseJob` 5 用例零回归）
- [ ] 工作树遗留：`module-projects/erp-prj-service/_cases/.../TestErpPrjDummyProbe/` + `cases-retention.bak/` 未跟踪脚手架（08-27 日志已登记故意遗留）；5 个 ai-check docs 修改未提交属另一 mission 流（来源：multi-audit P2-16；watch-only）
- [ ] `job-scheduling.md` 漏登记生产 nop-job `erp-fin-ap-doc-processing`（含 `nop.job.erp-fin-ap-doc-processing.enabled/.cron-expr` 两部署键；对齐 :131 inv 行格式补 §3.x）（来源：`docs/audits/2026-08-26-2226-open-audit-erp-enhancement.md` P2-A；文档注册批）
- [ ] 设计路由层 README「暂不编码」标注陈旧 ×6：`docs/design/README.md:40-41` + finance/aps/inventory/assets 各 README（→「已实现（E3.x，2026-08-26/27）」）（来源：open-audit P2-B；文档注册批）
- [ ] `data-dependency-matrix.md` finance 行 R-only 特征化失实：E3.5 fin→pur 经 `IErpPurInvoiceBiz.save()` command 写边未登记（§2.4 补边 + 行特征修正）（来源：open-audit P2-C；与 multi P2-13 合并收口）
- [ ] `known-good-baselines.md` 未落 E3 批全绿行（3889 锚落后于 +23 新测试；P1 修复批全量重跑时落新行并做差量归因）（来源：open-audit P2-D；随 mission VERIFY 批收口）
- [x] 六个新 mutation（`uploadApDocument` 等）鉴权面零有效覆盖 + E3 计划 Phase 7 证据归因漂移（负路径实际测 BadDebt 既有 mutation；建议补 uploadApDocument 无权限拒绝路径 E2E）（来源：open-audit P2-E；建议随 plan 2026-08-27-2006-1 顺带评估） ✅ plan `2026-08-28-0219-1`（E2E `ai-interface.value.spec.ts` 新增 `uploadApDocument denied for unauthorized role` 用例——role-restricted 经 `/r/` 被 FNPT enforcement 拒；E3 计划 Phase 7 ④ post-hoc 归因注记落盘；`TestErpFinApDocumentPipeline` 保持 FALSE 的理由补注记）
- [ ] `AGENTS.md` BizModel 包名示例 `io.github.nop.app.erp.<domain>.service` 与全仓实态 `app.erp.<short>.service` 不符（存量漂移）（来源：open-audit P2-F；随下次 AGENTS.md 人工维护窗口）
