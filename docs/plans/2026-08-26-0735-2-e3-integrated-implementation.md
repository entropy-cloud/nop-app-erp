# 2026-08-26-0735-2-e3-integrated-implementation E3 整体实现：KPI 度量目录与行级安全核实 / 库存审计快照 / 资产字段集与审计轨迹 / APS 求解器试点 / 文档摄取管道 / AI 接口层

> Plan Status: active
> Mission: erp-enhancement
> Work Item: E3.1 + E3.1b + E3.2 + E3.3 + E3.4 + E3.5 + E3.6 + E3.8（E3.7 暂缓排除，见 Non-Goals）
> Last Reviewed: 2026-08-26
> Source: `docs/backlog/erp-enhancement-roadmap.md` §5 Milestone E3 + §8 横切关注点 + §9 规则 2（单一整体计划，用户 2026-08-12 指示）
> Related: `docs/plans/2026-08-26-0735-1-e2-survey-cross-confirmation.md`（同批计划 1，先行）；E1 设计文档 7 份（owner docs）；permissions-enforcement mission（E3.1b 核实对象 = 其落地栈）
> Audit: required

## Current Baseline

> 逐工作项盘点（2026-08-26 实时仓库核验）。本计划为 roadmap §9 规则 2 强制的**单一整体计划**（E3.1-E3.8 全部非暂缓项一份计划，不逐项起草）——计划指南规则 4「一个计划一个结果表面」在此被 roadmap 显式规则覆盖：各工作项共享同一 roadmap 收口义务与结束审计，工作项间共享 ORM 授权记录与平台复用约束。

- **E3.1（KPI 度量目录）**：`docs/design/dashboards.md` 有 10 域看板章节 + 通用指标类型 + 实现约定，**无「KPI 度量目录」章节**（grep 零命中）。口径现状散落在各域 `ErpXxxDashboardBizModel.getDashboardKpi` 内联聚合；value-spec 数值断言（`*.value.spec.ts`）为数据驱动口径覆盖域。平台 nop-metadata（master）语义层已存在——E3.1 只做目录登记（口径对齐映射输入），不接平台运行时（触发条件驱动）。
- **E3.1b（行级安全核实）**：enforcement 栈已大幅落地（permissions-enforcement roadmap：action 级 E1.2 done、data 级 E2.1-E2.3 done、响应层脱敏 E3.1/E4 done；其 deep-audit follow-up 3 项为 active 计划 `2026-08-25-1956-1/2/3`，属该 mission 队列）。`getDashboardKpi`/报表查询（跨域只读聚合 BizQuery）的 role-row-filter 覆盖**未核实**。nop-datav 平台全链（api/app/codegen/core/dao/meta/service/web + DataAuth/RbacAuth/审计实体）**已合入 `../nop-entropy` master**（2026-08-26 活仓核实：master HEAD 跟踪全模块，2026-08-19~23 收口提交含「datav 630 tests 全绿」；roadmap §8 的「feat 分支在途」核对记录停留在 2026-08-13，已过期）——但应用层看板（10 域 AMIS）**未挂载 nop-datav**，E3.1b 边界注记按 master 实态登记（平台能力存在 + 应用层不依赖）。
- **E3.2（库存快照查询）**：inventory 3 层模型（Move→Ledger→Balance）+ StockTake + 批次追溯已落地；**无 `getInventorySnapshot`**（grep 零命中）。设计裁决：快照 = 派生视图（期初 + 截至时点流水汇总），默认零 ORM；物化表触发条件 = 大库存量 + 查询 P95 超标（未满足）。
- **E3.3（资产型号级 ext 字段集）**：18 域 `module-*/model/*.orm.xml` **零 JsonOrmComponent/ext-json 用法**（grep 0 命中）——E3.3 为项目首次启用，实现前置须核实平台 ext 字段用法（owner doc 明示并入计划 Phase 1 Explore）。承载实体 `ErpAstAssetModel`/`ErpAstAsset` 已就位；管理界面 = view.xml 定制（flux 渲染）。
- **E3.4（APS 求解器分离 + TOC 试点）**：`module-aps/erp-aps-service/.../scheduling/ErpApsSchedulingEngine.java` 纯算法 POJO（贪心前/后向 + MAINTENANCE 单约束）；`module-manufacturing/erp-mfg-service/.../crp/CrpLoadCalculator.java` 负荷率派生链已有（crpLoadChart 已接看板）。**无 `IApsSchedulingSolver` / `scheduleToc`**（grep 零命中）。多约束/预测衔接/KPI 看板为触发条件驱动 Non-Goal。
- **E3.5（文档摄取管道）**：非结构化文档入口完全缺失（供应商发票仅人工录入）；既有基础 = b2b EDI/MFT（结构化入口）+ AP 三单匹配（M1）+ 通知链路 + nop-job。平台 `../nop-entropy/nop-file`（含 nop-file-api）存在，能力须 Phase 1 核实。**OCR 引擎未选型**（E3.5 前置调研）。ORM 变更（文档引用/解析字段/处理日志）已授权（roadmap §8.1）。
- **E3.6（AI 接口层）**：action 层即 API（@BizMutation/@BizQuery 经 IGraphQLEngine 自动暴露 GraphQL + REST 双通道）；enforcement 栈（RBAC + SoD + 数据权限 + 响应脱敏）与 `IRateLimiter`（D1）已落地 = 护栏基础。**GraphQL schema 对 AI 工具发现的可用性（introspection/类型描述完整性）未调研**（E3.6 前置）；`actorType=AI` 字段未落（ORM 已授权，仅当需区分 AI 发起操作时）。**MCP 否决**（用户 2026-08-12 裁决：应用与平台均不用 MCP）。
- **E3.8（资产审计轨迹）**：会计日志（posting-log，M5）+ E4.2 保密字段读访问审计已落地；**无 `getAssetAuditTrail`**（grep 零命中）。承载裁决开放：优先复用会计日志同型追加业务审计类型；独立实体 `ErpAstAssetActionLog` ORM 已授权（§8.1）。
- **横切约束**：① ORM 授权 §8.1 覆盖 E3.5/E3.6/E3.8/(E3.2 物化表如触发)，实施时显式列出变更清单 + dual-agent-approval（双独立子 agent 批准）；不手改生成代码，模型变更后 `mvn clean install -DskipTests` 增量重生成。② 平台优先：nop-ai/nop-wf/nop-file/nop-job/nop-metadata/nop-datav 先核实不重建；平台模块以 `../nop-entropy` **master 实态**为准（nop-datav 全链已合入 master，2026-08-26 核实；roadmap §3/§8 的 2026-08-13 平台核对记录已过期，随 Phase 8 roadmap 收口显式更新，不得静默）。③ 业务逻辑单一真相：AI/管道只编排不复制业务规则。④ 验证标准（mission 注册）：全量 `mvn test` 零回归 + 全量构建 BUILD SUCCESS + compliance checker 零漂移 + 涉前端项 E2E 全绿。

## Goals

- **E3.1**：`dashboards.md` 新增「KPI 度量目录」章节，登记 10 域全部 KPI 规范口径（名称/定义公式/数据来源表+过滤/单位/口径说明），跨域重复口径显式对齐，作为 value-spec 断言与未来平台语义层映射的单一真相（零代码）。
- **E3.1b**：核实 enforcement 数据权限（role-row-filter）对 `getDashboardKpi`/报表查询的实际覆盖（静态表征 + %test 运行时抽样实测）+ nop-datav DataAuth 边界登记（master 实态：平台能力存在、应用层未挂载不依赖）；缺口登记去向（enforcement 栈扩展候选）。
- **E3.2**：`getInventorySnapshot(warehouseId, materialIds, asOfDate)` @BizQuery 落地（派生视图，默认零 ORM）+ 每日对账机制增「账面余额 = 快照派生值」一致性校验项。
- **E3.3**：资产型号级 ext 字段键声明 + 管理界面（view.xml 定制，零 ORM）+ 实例资产 ext 字段按型号校验。
- **E3.4**：`IApsSchedulingSolver` 接口分离 + 既有贪心引擎保留为默认实现（行为不变）+ `scheduleToc` 瓶颈识别试点（复用 CrpLoadCalculator 派生链，SchedulingResult 扩展瓶颈清单）。
- **E3.5**：文档摄取管道端到端落地：上传入口 → OCR 解析 → 分类（规则优先 + 置信度 + 低置信挂人工）→ 草稿发票（UNSUBMITTED）→ 三单匹配预填衔接；分类引擎 SPI 预留 ML 注入；文件载体 nop-file；审计追溯（文档-发票-凭证回链）。
- **E3.6**：AI 工具消费约定落地（GraphQL 类型定义即 API，经调研结论最小实现；REST+GraphQL 双通道验证）+ 护栏接线（高影响 action 复用既有审批门；`actorType=AI` 审计标识如 Phase 1 裁决需要；限流复用 IRateLimiter）。不采用 MCP。
- **E3.8**：资产状态/归属变化审计记录（承载经 Phase 1 裁决）+ `getAssetAuditTrail(assetId)` 时间轴 BizQuery。
- roadmap §2/§5/§6 收口：E3 非暂缓项全部 done。

## Non-Goals

- **E3.7 跨域流程编排试点**——暂缓（用户 2026-08-12 指示 + roadmap 规则 6：触发条件 = 真实 nop-wf 跨域编排需求出现，人工门控/超时/整链追溯）。保持 todo，不纳入本计划。
- 周期盘点 CycleCountTask 实体化/视图（E1.4 触发条件驱动：真实周期盘点需求 + ABC 分类数据就绪；E3.2 仅快照查询）。
- 平台语义层运行时映射（nop-metadata Measure/Dimension 注册）、外部嵌入式访问、nop-datav 平台挂载/ChatBI（**触发条件重裁 2026-08-26**：原触发条件「平台合入 master + 运行时配置化 KPI/嵌入需求」的前半已满足——nop-datav 全链已合入 master；维持移出的依据 = 后半触发条件未满足：应用层无运行时配置化 KPI / 外部嵌入 / nop-datav 挂载的真实需求。重裁登记于本计划，owner doc `dashboard-semantic-layer.md` §0 对应表述随 Phase 2 一并对齐）。
- APS 多约束扩展（PERSONNEL/TOOL/capacity>1）、预测→排产衔接 SPI、排产 KPI 看板（触发条件驱动，E3.4 仅求解器分离 + TOC 试点）。
- E3.5 分类引擎 Phase 2 ML 分类器（触发条件：规则分类准确率不满足业务需求；Phase 1 仅 SPI 预留）。
- E3.5 邮件摄取入口（设计已备，触发条件：真实供应商邮件发票入口需求；本计划仅上传入口）。
- SCIM/LDAP 身份集成（远期触发条件）。
- E3.1b 缺口的实施修复（属 enforcement 栈扩展，本计划只核实 + 登记）。
- 不改 E1 设计文档已裁决内容（MCP 否决 / 外挂化 / 平台边界等）。

## Task Route

- Type: `implementation-only change`（含 E3.1b `verification or audit work` 子项；ORM 变更经 §8.1 预授权 + dual-agent-approval 执行）
- Owner Docs: `docs/design/dashboard-semantic-layer.md`、`docs/design/dashboards.md`、`docs/design/inventory/audit-snapshot-cycle-count.md`、`docs/design/assets/audit-trail-and-custom-fieldsets.md`、`docs/design/aps/constraint-based-planning.md`、`docs/design/finance/document-driven-ap-automation.md`、`docs/design/ai-native-interface.md`、`docs/design/roles-and-permissions.md`（E3.1b 注记）
- Skill Selection Basis: `nop-backend-dev`（BizModel/@BizQuery/IBiz/ErrorCode/SPI/事务边界——Phase 3/4/5/6/7 主体）；`nop-frontend-dev`（E3.3 view.xml 定制 + 涉前端页面项）；`nop-testing`（JUnit + 浏览器层 E2E——各实现 Phase 的 Proof 项）；E3.1/E3.1b 文档与核实子项 Skill none。平台交互（nop-file/ext 字段/GraphQL introspection）先读 `../nop-entropy/docs-for-ai/`（AGENTS.md 平台文档规则）。

## Infrastructure And Config Prereqs

- OCR 引擎（Phase 1 选型裁决）：候选 = 本地 Tesseract 系（tess4j 集成）/ SPI 抽象 + 可插拔默认实现；**不引入外部云 OCR 服务与密钥**；License 兼容性纳入选型判据。OCR 属**系统级依赖**（引擎二进制/语言数据包）——选型裁决须同时评估本地安装与 CI/E2E 环境可用性；若引擎二进制在 E2E 环境不可用，E3.5 的 E2E Proof 按「SPI 默认 stub（E2E 层）+ 真引擎 JUnit（本机层）」分层验证，该裁决随 Phase 1 记录在案。
- 新增 config 开关一律默认关闭、向后兼容（E3.5 管道启用、E3.6 护栏、E3.4 求解器切换等，对齐既有 config-gate 范式；lesson 14：非硬契约才可 config 化）。涉 config-gate 的 E2E 所需 webServer JVM args 随对应 Phase 落地时固定进 `playwright.config.ts`（不允许测试运行时临时注入）。
- ORM 变更（如触发）：加性列/实体，Quarkus dev ddl-auto=update 自动加列，无数据迁移脚本；回滚 = git revert 模型变更 + `mvn clean install -DskipTests` 重生成（对齐 id-string 迁移既定路径）。
- E2E：涉前端/GraphQL 面项按 e2e-runbook flux 渲染模式。

## Execution Plan

### Phase 1 — 前置调研与变更清单定稿（Explore | Decision，零代码，解锁 Phase 4/6/7）

Status: planned
Targets: `docs/design/finance/document-driven-ap-automation.md`、`docs/design/ai-native-interface.md`、`docs/design/assets/audit-trail-and-custom-fieldsets.md`、`docs/design/inventory/audit-snapshot-cycle-count.md`（调研结论注记落点）
Skill: none（调研/裁决；平台文档阅读按需）

- Item Types: `Decision | Explore`
- Prereqs: 无

- [ ] Explore: E3.5 OCR 选型调研——本地 OCR 引擎候选（Tesseract/tess4j 等）可用性、Java 集成、中文发票要素支持、License；`nop-file` 能力核实（上传/存储/引用方式，读 `../nop-entropy` 源码 + docs-for-ai）。结论 + 选型裁决落 owner doc「落地策略」表。
- [ ] Explore: E3.6 GraphQL schema AI 工具发现可用性调研——introspection 完整性、action 类型/描述元数据、schema 规模、REST+GraphQL 双通道一致性；`business-module-metadata.md` 雏形关系。结论落 owner doc（含「最小落地集」清单定稿）。
- [ ] Explore: E3.3 平台 ext 字段/JsonOrmComponent 用法核实——`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` + 平台既有用例；确认「型号级字段集」承载形态（orm.xml ext 字段声明 vs meta 层声明，零 ORM 首选）。结论落 owner doc。
- [ ] Decision: E3.8 审计承载裁决——复用会计日志同型扩展 vs 独立实体 `ErpAstAssetActionLog`（授权已备）。记录选择、替代方案、残留风险（触发实物面全生命周期视角的事件类型覆盖对比）。
- [ ] Decision: E3.2 实现路径确认——派生视图零 ORM（预期默认）vs 物化表（触发条件未满足则显式记录不启用）。
- [ ] Decision: ORM 变更清单定稿（如触发）——逐项列出实体/字段/授权依据（§8.1 对应行），提交 dual-agent-approval（双独立子 agent 批准）并记录；零 ORM 结论亦显式记录。

Exit Criteria:

- [ ] 四份 owner doc 落地策略表更新（调研结论 + 裁决记录）；ORM 变更清单（或零 ORM 结论）经 dual-agent-approval 记录在案——Phase 4/6/7 依赖此结论解锁。
- [ ] E3.6「最小落地集」清单随调研结论落 owner doc 后**即为 Phase 7 的冻结验收基准**（Phase 7 Exit 按该清单逐项核对，不得执行期自行增删）。

### Phase 2 — E3.1 KPI 度量目录 + E3.1b 行级安全核实（零代码/核实）

Status: planned
Targets: `docs/design/dashboards.md`、`docs/design/dashboard-semantic-layer.md`、`docs/design/roles-and-permissions.md`
Skill: none（目录登记与核实；核实需运行 %test profile 实测）

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（与 Phase 1 可并行）

- [ ] Add: `dashboards.md` 新增「KPI 度量目录」章节——10 域全部 KPI 逐项登记（名称/定义公式/数据来源表+过滤条件/单位/口径说明），口径与 value-spec 数据驱动断言对照（抽样核对期望值表派生口径一致）；跨域重复口径（如 finance vs purchase 应付）显式对齐登记。
- [ ] Proof: E3.1b 数据权限覆盖核实——静态表征（getDashboardKpi/报表 BizQuery 的查询路径是否过 role-row-filter/data-auth 检查点）+ %test profile 运行时抽样实测（受限账号 vs admin 对同一 KPI/报表的可见差异，复用 permissions 测试账号池/loginAsRole 范式）；结论与缺口清单落 `dashboard-semantic-layer.md` §4。
      - Skill: `nop-testing`
- [ ] Add: nop-datav DataAuth/RbacAuth 边界注记（master 实态引用：平台全链已合入 master 且 DataAuth/RbacAuth 实体在，但应用层看板未挂载 nop-datav，边界 = 平台能力存在 + 应用层不依赖）；`roles-and-permissions.md` 相应注记；`dashboard-semantic-layer.md` 中全部 2026-08-13 平台状态过期表述**全量对齐 master 实态**（覆盖段落：头部平台核对注记 / 来源与背景·平台能力基线 / 现状 vs Superset 对照表 / §0 平台能力边界 / §3 协议化暴露（ChatBI feat 分支注记）/ §4 行级安全 / AP-8 / 相关文档——不留内部自相矛盾的权威 owner doc）。
- [ ] Decision: E3.1b 缺口登记去向裁决——缺口实施属 enforcement 栈扩展：逐项判定归属（permissions-enforcement roadmap follow-up vs 本 roadmap 候选工作项），登记理由记录在案；本计划不实施任何缺口修复。

Exit Criteria:

- [ ] 度量目录章节覆盖 10 域全部 KPI 且抽样口径与 value-spec 一致；E3.1b 核实结论 + 缺口登记落盘（无未登记的开放结尾）。

### Phase 3 — E3.2 库存审计快照查询

Status: planned
Targets: `module-inventory/erp-inv-service/`（BizModel + 对账校验）
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（E3.2 实现路径确认；预期零 ORM）

- [ ] Add: `getInventorySnapshot(warehouseId, materialIds, asOfDate)` @BizQuery——派生视图实现（期初/流水汇总口径按 Phase 1 确认），返回数量 + 成本 + 库位维度；asOfDate 语义对齐 businessDate（AP-5）。
- [ ] Add: 每日对账机制增「账面余额 = 快照派生值」一致性校验项（挂既有对账 Job/校验项族，`domain-design-guidelines.md` 对账机制扩展）。
- [ ] Proof: JUnit（快照 vs 既有链路数据确定性对照，含 asOfDate 边界：时点前/后流水裁剪）+ 浏览器层/GraphQL E2E 数值断言（对齐 value-spec 范式）；`Skill: nop-testing`

Exit Criteria:

- [ ] BizQuery 可查且数值确定性可证（单测 + E2E 全绿）；对账校验项落地且失败模式可观测；零 ORM（或物化表触发结论已登记）。

### Phase 4 — E3.3 资产 ext 字段集 + E3.8 审计轨迹（assets 域同域两项）

Status: planned
Targets: `module-assets/`（orm.xml 仅当 Phase 1 裁决需要 / meta / service / web view.xml）
Skill: nop-backend-dev + nop-frontend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（E3.3 ext 用法核实 + E3.8 承载裁决 + ORM 清单 dual-agent 批准）

- [ ] Add: E3.3 型号级 ext 字段键声明（承载形态按 Phase 1 结论，零 ORM 首选）+ 实例资产 ext 字段按型号校验（非法键/类型拒绝，ErrorCode 走 NopException 范式）。
- [ ] Add: E3.3 字段集管理界面（view.xml 定制，flux 渲染，对齐 nop-frontend-dev 反模式自检）。
- [ ] Add: E3.8 资产状态/归属变化审计记录（承载按 Phase 1 裁决；事件类型覆盖 owner doc §1 清单：CREATE/UPDATE/STATUS_CHANGE/MAINTENANCE/VALUATION/DISPOSAL/TRANSFER）+ `getAssetAuditTrail(assetId)` 时间轴 BizQuery。
- [ ] Proof: JUnit + 浏览器层 E2E（字段集管理界面 CRUD + 实例校验负路径；审计时间轴查询含跨事件排序与回链）；`Skill: nop-testing`

Exit Criteria:

- [ ] 两能力端到端可用（界面 + 查询口），审计事件类型全覆盖 owner doc 清单，测试全绿；assets owner doc 落地策略表更新。

### Phase 5 — E3.4 APS 求解器分离 + scheduleToc 瓶颈识别试点

Status: planned
Targets: `module-aps/erp-aps-service/`（接口 + 引擎 + TOC 实现）
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: 无（与 Phase 3/4 可并行）

- [ ] Add: `IApsSchedulingSolver` 接口 + 既有贪心引擎适配为默认实现（**既有 scheduleForward/scheduleBackward 行为不变**，既有排产 JUnit/E2E 零回归即证明）+ bean 注册 + config 切换（对齐 D3 子计算器注入范式）。
- [ ] Add: `scheduleToc` 瓶颈识别试点——复用 `CrpLoadCalculator` 负荷率派生链（AP-3）识别 horizon 内超阈值瓶颈中心，先排瓶颈（拉动式）再排非瓶颈（前/后向兜底）；`SchedulingResult` 扩展 bottleneckMachineIds + 各中心负荷率。
- [ ] Proof: JUnit（既有排产用例零回归 + TOC 用例：瓶颈中心优先排程可观测 + 结果扩展字段非空）+ 既有 aps E2E spec 扩展断言（如 `aps-operation-order`/`aps-rush-order` 范式适用则扩展，否则 JUnit 为准）；`Skill: nop-testing`

Exit Criteria:

- [ ] 求解器可插拔（接口 + 默认贪心 + config 切换）且默认行为不变有测试证明；scheduleToc 试点落地 + 结果扩展可观测。

### Phase 6 — E3.5 文档摄取管道

Status: planned
Targets: `module-finance/`（ORM 加性变更如清单批准 / service 管道 / web 入口）、`../nop-entropy` 只读核实
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（OCR 选型 + nop-file 核实 + ORM 清单 dual-agent 批准）

- [ ] Add: 管道实体与字段落地（按批准清单：文档引用/解析结果字段/文档处理日志；文件本体存 nop-file，业务表只存引用+解析字段——AP-3）。
- [ ] Add: 管道步骤编排——上传入口 → OCR 解析（选型引擎，SPI 抽象）→ 分类（Phase 1 规则引擎：文件名/关键字段 → 单据类型/对应方 + 置信度；低置信挂人工队列）→ 草稿 `ErpPurInvoice`（approveStatus=UNSUBMITTED，人工确认门 AP-1）→ 三单匹配预填衔接（解析结果只作预填，校验走既有链路 AP-5）；异步步骤经 nop-job/事件（AP-4）。
- [ ] Add: 分类引擎 SPI（Phase 2 ML 注入预留，对齐 `IErpFinAcctDocProvider` 注入范式）+ 审计追溯（文档处理轨迹日志 + 文档-发票-凭证回链）。
- [ ] Proof: JUnit（管道正路径 + 低置信人工门 + 幂等/失败重试路径）+ E2E（上传→草稿→三单匹配预填浏览器层断言）；config-gate 默认关闭下既有套件零回归；`Skill: nop-testing`

Exit Criteria:

- [ ] 管道端到端落地（上传→OCR→分类→草稿→预填）且人工门不可绕过；测试全绿；OCR 引擎无外部云依赖。

### Phase 7 — E3.6 AI 接口层

Status: planned
Targets: `module-*/`（如需 actorType 字段按批准清单）、GraphQL/REST 通道验证、护栏接线
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（GraphQL 可用性调研 + 最小落地集定稿 + ORM 清单裁决）

- [ ] Add: AI 工具消费约定最小落地集（按 Phase 1 冻结清单逐项落地：action 元数据/描述补全等——不新建 AI 专用业务 API，AP-1 外挂化裁决）+ REST 与 GraphQL 双通道调用一致性验证。
- [ ] Add: 护栏接线——高影响 action 确认门复用既有审批流（use-approval）；`actorType=AI` 审计标识（仅当 Phase 1 裁决需要，ORM 授权 §8.1）；限流复用 `IRateLimiter`。**不采用 MCP**（任何形态——用户裁决）。
- [ ] Proof: JUnit + GraphQL/浏览器层 E2E（AI 通道调用正路径 + 未授权/超限负路径 + 审计标识落账断言）；`Skill: nop-testing`

Exit Criteria:

- [ ] AI 消费面按 Phase 1 冻结的最小落地集**逐项**落地且双通道一致（清单项零增删）；护栏负路径可证；`ai-native-interface.md` 落地策略表更新。

### Phase 8 — 收尾：全量验证 + owner docs + roadmap 收口

Status: planned
Targets: `docs/backlog/erp-enhancement-roadmap.md`、各 owner docs、`docs/logs/`
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 2-7 全部

- [ ] Proof: 全量验证（mission 注册标准）——`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 零回归 + `bash docs/audits/nop-compliance-checker.sh` 零漂移（基线漂移则按 known failure mode 开基线裁决，不得静默）+ 涉前端/GraphQL 项 E2E 全绿（flux 模式）。
- [ ] Add: owner docs 对齐（**6 份**含落地策略表的 E1 设计文档——ai-native-interface / dashboard-semantic-layer / document-driven-ap-automation / audit-snapshot-cycle-count / audit-trail-and-custom-fieldsets / constraint-based-planning——落地策略表 todo→done + 实现注记；cross-domain-flow-orchestration.md 属暂缓 E3.7，不在 todo→done 之列）；`ai-native-interface.md` 中 2026-08-13 平台状态过期表述（来源与背景 / §3.5 / 相关文档）全量对齐 master 实态；roadmap §2 计数 + §5 E3 非暂缓项 done + §6 交付物 ✅（E3.7 保持 todo 暂缓）；**roadmap §3/§8 平台能力跟踪更新**（nop-datav 全链已合入 master 的 2026-08-26 活仓事实，替换 2026-08-13 过期核对记录——显式更新，不得静默）；`missions/erp-enhancement.json` description 中同型平台状态表述同步更新（第三真相源不得漂移）；`docs/logs/` 日志条目。

Exit Criteria:

- [ ] 全量验证四路全绿并记录；roadmap/owner docs/日志一致收口。

## Draft Review Record

- Independent draft review iteration 1: **needs-revision**（task `ses_fc4bb390cffecEiJbolij5EZtJ`，fresh session）——A/C/D/E/F/G 七维通过、单一整体计划合规；1 BLOCKER：nop-datav「master 仅 chart 模块」基线失真（活仓证伪：`../nop-entropy` master HEAD 已跟踪 nop-datav 全模块链，2026-08-19~23 收口提交含 630 tests 全绿；roadmap §8 的 2026-08-13 核对记录过期），污染 E3.1b 交付物与 nop-datav Non-Goal 触发前提；5 MINOR（Phase 2 Decision 标注缺失 / Phase 7 验收基准未冻结 / OCR 系统级依赖与 playwright 措辞 / Phase 3 Prereqs 格式 / E3.1b 核实 Skill 标注）。已修复：Baseline+Goals+Phase 2+Non-Goals 平台状态全部改 master 实态 + 触发条件显式重裁 + roadmap §3/§8 更新项登记 Phase 8；五项 MINOR 逐一落实。
- Independent draft review iteration 2: **needs-revision（修复后可直接接受）**（task `ses_fc4b3cfaeffe7xq3PdYpJ41MIC`，fresh session 复检）——A/B/C/E 全过（基线活仓复核全吻合，含 nop-datav master 全链与 DataAuth/RbacAuth 测试类存在性、收口提交日期区间）；1 BLOCKER：过期表述清扫范围枚举不全——`dashboard-semantic-layer.md`（头部注记/平台能力基线/对照表/§4/AP-8/相关文档）与 `ai-native-interface.md`（来源与背景/§3.5/相关文档）仍有现在时 feat 分支陈述，原计划仅安排 §0 与 roadmap §3/§8 对齐，字面执行将产出内部自相矛盾 owner doc（规则 13 不可降级）；3 MINOR（「7 份 owner doc」计数应為 6 份、missions/erp-enhancement.json description 同型过期表述未安排更新、Draft Review Record 未持久化）。已修复：Phase 2 扩为 dashboard-semantic-layer.md 全段落清扫（显式枚举）；Phase 8 扩为 6 份枚举 + ai-native-interface.md 清扫 + mission 注册同步更新；Closure Gates 对齐门同步；本记录即 MINOR-3 修复。
- Independent draft review iteration 3: **acceptable as-is (accept)**（task `ses_fc4afbafcffeTHEvvfEHLdg6hp`，fresh session 确认性复检）——三项前轮修复全部核验到位（Phase 2 七段枚举 / Phase 8 ai-native-interface 清扫 + mission 注册更新 / 6 份计数枚举 / Review Record 持久化）；整体终检通过（规范正文零现在时 feat 分支残留、反松弛词零命中、roadmap §9 规则 2/6 合规）。1 新 MINOR（§3 协议化暴露的 ChatBI feat 注记未入字面枚举，已被「全部/全量」主句语义覆盖）——已顺手补入枚举。共识达成 → 转 active。

## Closure Gates

> 完整仓库验证在此一次性执行（Phase 8）：`mvn clean install -DskipTests` / `mvn test` / `bash docs/audits/nop-compliance-checker.sh` / 涉前端项 E2E（flux 模式）。阶段退出仅验证各自交付与下游解锁（见执行时规则 7）。

- [ ] 范围内行为完成（8 个工作项全部落地，含 E3.1b 核实结论与缺口登记）
- [ ] 相关文档对齐（6 份含落地策略表 E1 owner doc + 两份平台状态过期表述清扫（dashboard-semantic-layer / ai-native-interface）+ dashboards.md + roles-and-permissions.md + roadmap §3/§8 + missions/erp-enhancement.json）
- [ ] 已运行验证：build / 全量 test / compliance / E2E 四路（Phase 8 记录）
- [ ] 无范围内项目降级为 deferred/follow-up（确属触发条件驱动的深化项已在 Non-Goals/Deferred 显式移出）
- [ ] 独立草案审查已完成并记录；ORM 变更（如触发）dual-agent-approval 记录在案
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### E3.7 跨域流程编排试点

- Classification: `out-of-scope improvement`（用户暂缓指令）
- Why Not Blocking Closure: roadmap 规则 6——触发条件 = 真实 nop-wf 跨域编排需求（人工门控/超时/整链追溯）出现；设计已完备（E1.7 owner doc）。
- Successor Required: yes（触发后并入或另立计划，roadmap §9 规则 2 末句）

### 触发条件驱动的深化项（各 owner doc 已登记）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 周期盘点 CycleCountTask / 平台语义层映射 / APS 多约束+预测衔接+KPI 看板 / ML 分类 Phase 2 / 邮件摄取 / SCIM——均为 owner doc 明示触发条件驱动的深化项，非本计划范围（Non-Goals 已显式移出）。nop-datav 平台挂载/ChatBI 的触发条件已于本计划重裁（Non-Goals 第 3 条：平台合入 master 已满足，剩余触发 = 应用层运行时配置化 KPI/嵌入/挂载真实需求）。
- Successor Required: no（触发条件出现时由 roadmap 择机立项）

## Closure

Status Note: （结束时填写）

Closure Audit Evidence:

- Auditor / Agent: （结束时填写）
- Evidence: （结束时填写）

Follow-up:

- （仅非阻塞跟进；E3.1b 缺口已按 Phase 2 Decision 登记去向，此处不重复列缺陷）
