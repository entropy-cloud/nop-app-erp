# 2026-08-26-0735-2-e3-integrated-implementation E3 整体实现：KPI 度量目录与行级安全核实 / 库存审计快照 / 资产字段集与审计轨迹 / APS 求解器试点 / 文档摄取管道 / AI 接口层

> Plan Status: completed
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

Status: completed
Targets: `docs/design/finance/document-driven-ap-automation.md`、`docs/design/ai-native-interface.md`、`docs/design/assets/audit-trail-and-custom-fieldsets.md`、`docs/design/inventory/audit-snapshot-cycle-count.md`（调研结论注记落点）
Skill: none（调研/裁决；平台文档阅读按需）

- Item Types: `Decision | Explore`
- Prereqs: 无

- [x] Explore: E3.5 OCR 选型调研——本地 OCR 引擎候选（Tesseract/tess4j 等）可用性、Java 集成、中文发票要素支持、License；`nop-file` 能力核实（上传/存储/引用方式，读 `../nop-entropy` 源码 + docs-for-ai）。结论 + 选型裁决落 owner doc「落地策略」表。
      → 已完成（2026-08-26）：nop-file 全链可用（nopFileStore/NopFileRecord//f/upload//f/download，erp-fin-service 已依赖 nop-biz-file-core）；tesseract 本机不可用；选定 `IErpFinOcrEngine` SPI + 默认纯 Java 文本抽取引擎（PDFBox，零新增依赖——已随 nop-report-pdf 在依赖树）；扫描件低置信挂人工门；tess4j 适配器为可插拔项（触发=环境具备二进制）。结论落 `document-driven-ap-automation.md` §前置调研结论。
- [x] Explore: E3.6 GraphQL schema AI 工具发现可用性调研——introspection 完整性、action 类型/描述元数据、schema 规模、REST+GraphQL 双通道一致性；`business-module-metadata.md` 雏形关系。结论落 owner doc（含「最小落地集」清单定稿）。
      → 已完成（2026-08-26）：introspection 平台支持但默认关闭（应用显式 false）；804 应用 action 零 @Description（主要缺口）；双通道共享 IGraphQLEngine；平台 GraphQLToolProvider 已存在（登记不重建）；最小落地集 6 项冻结（见 `ai-native-interface.md` §前置调研结论）。actorType 裁决不落地（触发条件登记）。
- [x] Explore: E3.3 平台 ext 字段/JsonOrmComponent 用法核实——`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` + 平台既有用例；确认「型号级字段集」承载形态（orm.xml ext 字段声明 vs meta 层声明，零 ORM 首选）。结论落 owner doc。
      → 已完成（2026-08-26）：stdDomain=json 自动生成 JsonOrmComponent（规范在 model-first-development.md，用例 NopJobSchedule.jobParams）；meta 层无持久化 ext 机制 → 字段集+实例值持久化必须 ORM；基线更正：`ErpAstAssetModel` 实体不存在（原「已就位」表述失实，活仓核实 18 实体无型号实体）。结论落 `audit-trail-and-custom-fieldsets.md` §前置调研结论。
- [x] Decision: E3.8 审计承载裁决——复用会计日志同型扩展 vs 独立实体 `ErpAstAssetActionLog`（授权已备）。记录选择、替代方案、残留风险（触发实物面全生命周期视角的事件类型覆盖对比）。
      → 裁决：**独立实体 `ErpAstAssetActionLog`**。理由：会计日志建模财务面（过账事件），资产生命周期事件（suspend/resume/移动/维护等）多数无凭证关联，复用需为非财务事件在 finance 域扩类型（语义错位 + 越授权范围）。事件类型对齐 owner doc §1 清单。残留风险：审计插入行会进入既有快照测试 output/tables → 受影响测试需重录基线（Phase 4 处理）。
- [x] Decision: E3.2 实现路径确认——派生视图零 ORM（预期默认）vs 物化表（触发条件未满足则显式记录不启用）。
      → 裁决：**派生视图零 ORM**（对不可变 StockLedger 按 businessDate <= asOfDate 聚合；等价期初+流水汇总）。物化表触发条件未满足，不启用（§8.1 E3.2 行未使用）。对账挂点：deferred 作业 `erp-inv-stock-check`（job-scheduling.md §3.3 REGISTERED 待实现）落地为校验项载体（nop-batch 范式，默认关闭）。结论落 `audit-snapshot-cycle-count.md` §实现路径确认。
- [x] Decision: ORM 变更清单定稿（如触发）——逐项列出实体/字段/授权依据（§8.1 对应行），提交 dual-agent-approval（双独立子 agent 批准）并记录；零 ORM 结论亦显式记录。
      → 清单见下「ORM 变更清单与 dual-agent-approval 记录」；E3.2 零 ORM、E3.6 actorType 不落地已显式记录。

Exit Criteria:

- [x] 四份 owner doc 落地策略表更新（调研结论 + 裁决记录）；ORM 变更清单（或零 ORM 结论）经 dual-agent-approval 记录在案——Phase 4/6/7 依赖此结论解锁。
- [x] E3.6「最小落地集」清单随调研结论落 owner doc 后**即为 Phase 7 的冻结验收基准**（Phase 7 Exit 按该清单逐项核对，不得执行期自行增删）。

### ORM 变更清单与 dual-agent-approval 记录（Phase 1 产出）

**清单**（全部为加性变更；不手改生成代码，模型变更后 `mvn clean install -DskipTests` 增量重生成；Quarkus dev ddl-auto=update 自动加列/建表，无迁移脚本；回滚 = git revert + 重生成）：

| # | 域 | 变更 | 授权依据 |
|---|----|------|---------|
| 0 | assets+finance | 两域 `<domains>` 各补本地 `json-4000` 预定义域（`<domain name="json-4000" precision="4000" stdDomain="json" stdSqlType="VARCHAR"/>`，对齐 nop-job.orm.xml 模式；活仓核实两域现无该声明） | 随 #2/#3/#4/#5（json 列声明前置） |
| 1 | assets | 新增实体 `ErpAstAssetModel`（资产型号：id(BIGINT/seq-default)/code(UK)/name/categoryId(BIGINT stdDataType=string，FK→ErpAstAssetCategory，nullable)/extFieldDefs(json-4000)/remark + 标准审计列） | 保护区域 `model/*.orm.xml` auto + dual-agent-approval（无 §8.1 行；owner doc `audit-trail-and-custom-fieldsets.md` §2 设计以型号实体为承载，本计划 Phase 1 基线更正后裁决补建） |
| 2 | assets | `ErpAstAsset` 加列 `modelId`（**BIGINT stdDataType=string**（Java String），FK→ErpAstAssetModel，nullable，索引；对齐 id-string 迁移后全仓 FK 约定）+ `extFieldValues`（json-4000，nullable）。**已知影响**：既有实体加列会扰动 assets 域既有快照测试 output/tables 基线（同 E3.8 审计行影响），Phase 4 统一重录 | 同上（E3.3 实例值承载） |
| 3 | assets | 新增实体 `ErpAstAssetActionLog`（资产操作审计：id/assetId(BIGINT string，索引)/eventType(dict erp-ast/audit-event-type)/fromStatus/toStatus/fromDepartmentId/toDepartmentId/fromLocationId/toLocationId/fromStaffId/toStaffId(BIGINT string)/refEntityName(.String 50)/refEntityId(BIGINT string)/summary(String 1000) + 标准审计列；索引(assetId,createTime)）+ 字典 `erp-ast/audit-event-type`（CREATE/UPDATE/STATUS_CHANGE/TRANSFER/MAINTENANCE/VALUATION/DISPOSAL） | §8.1 E3.8 授权行（独立审计实体）+ dual-agent-approval |
| 4 | finance | 新增实体 `ErpFinApDocument`（AP 摄取文档：id/orgId(BIGINT string)/fileName(String 200)/fileExt(String 20)/mimeType(String 100)/fileLength(bigint)/fileId(**VARCHAR 200 stdDomain=file**，nop-file 引用，对齐 attachmentFileId 先例)/sourceType(dict)/status(dict)/docType(dict)/partnerId(BIGINT string，供应商匹配→ErpMdPartner 约定名)/confidence(DECIMAL 5,4)/parseResult(json-4000)/invoiceId(BIGINT string，草稿回链，普通列+索引，不声明跨模块 FK)/errorMsg(String 1000)/retryCount(int 默认 0) + 标准审计列）+ 字典 `erp-fin/ap-doc-source-type`（UPLOAD；EMAIL 预留）、`erp-fin/ap-doc-status`（RECEIVED/PARSED/CLASSIFIED/DRAFTED/MANUAL_REVIEW/FAILED/ARCHIVED）、`erp-fin/ap-doc-type`（VAT_INVOICE/GENERAL_INVOICE/RECEIPT/OTHER） | §8.1 E3.5 授权行（文档引用/解析字段）+ dual-agent-approval |
| 5 | finance | 新增实体 `ErpFinApDocumentLog`（处理轨迹：id/documentId(BIGINT string，索引，同模块 FK)/step(dict erp-fin/ap-doc-log-step: RECEIVE/PARSE/CLASSIFY/DRAFT/MANUAL_REVIEW/RETRY/FAIL)/success(Boolean)/detail(String 1000) + 标准审计列） | §8.1 E3.5 授权行（文档处理日志）+ dual-agent-approval |
| 6 | （不触发）E3.2 | 物化快照表——触发条件未满足，**不启用** | §8.1 E3.2 行未使用（显式记录） |
| 7 | （不触发）E3.6 | `actorType=AI` 字段——**不落地**（触发条件 = 首个 AI 服务账号投产或差异化护栏需求，见 ai-native-interface.md 前置调研结论） | §8.1 E3.6 行未使用（显式记录） |

> 措辞更正（批准代理 1 finding 8）：计划基线「18 域零 JsonOrmComponent/ext-json 用法」的精确表述 = **零 `stdDomain="json"`/JsonOrmComponent 机制用法**（项目自有 `domain="json*"` 纯 VARCHAR 列存在于 cs/crm/logistics，非本机制）。

**dual-agent-approval 记录**：

- 迭代 1：子代理 1（fresh session）**APPROVE**（2 MINOR：modelId 类型精度、措辞）；子代理 2（fresh session）**REJECT**（1 BLOCKER：modelId 须 BIGINT/stdDataType=string（id-string 全仓 FK 约定 + 多方言兼容）；3 MINOR：json-4000 本地域声明、fileId/invoiceId/partyId 显式类型、快照重录范围含 ErpAstAsset 加列）。清单已按双方要求修订（row 0 新增、类型显式化、快照影响扩记）。
- 迭代 2（对修订后清单）：子代理 1'（fresh session `ses_fc46ea5e7ffeI3rHuEGtF0q1Pn`）**APPROVE**（迭代 1 BLOCKER + 3 MINOR 全核验解决；2 新 MINOR：`partyId` 应更名 `partnerId`（全仓约定，ErpMdPartner）——已采纳更名；`fileId` 是否带 `tagSet="var"` 由实现期定（先例混合））
- 迭代 2（对修订后清单）：子代理 2'（fresh session `ses_fc46e91f5ffeVrQBLhR0cuX7IE`）**APPROVE**（迭代 1 五项发现逐一活仓核验解决，无新 BLOCKER/MINOR）
- **结论：双独立批准达成**（迭代 2 双 APPROVE）。`partyId` 已按 finding 更名为 `partnerId`（本行即更名记录，授权与类型不变）。ORM 清单锁定，Phase 4/6 按此实施。



### Phase 2 — E3.1 KPI 度量目录 + E3.1b 行级安全核实（零代码/核实）

Status: completed
Targets: `docs/design/dashboards.md`、`docs/design/dashboard-semantic-layer.md`、`docs/design/roles-and-permissions.md`
Skill: none（目录登记与核实；核实需运行 %test profile 实测）

- Item Types: `Add | Proof | Decision`
- Prereqs: 无（与 Phase 1 可并行）

- [x] Add: `dashboards.md` 新增「KPI 度量目录」章节——10 域全部 KPI 逐项登记（名称/定义公式/数据来源表+过滤条件/单位/口径说明），口径与 value-spec 数据驱动断言对照（抽样核对期望值表派生口径一致）；跨域重复口径（如 finance vs purchase 应付）显式对齐登记。
      → 完成（2026-08-26）：11 域全量登记（10 核心域 + CS 客服绩效看板——活仓更正：CS 看板已随域深化落地，原覆盖表「产品基线外」行同步更正）+ 跨域重复口径对齐表（ar/ap/账龄/及时率/折旧 5 组）+ 状态字段横向对照速查表；value-spec 抽样核对一致（finance revenue=1130 / projects grossMarginPct=0.4 等见章首注记）。
- [x] Proof: E3.1b 数据权限覆盖核实——静态表征（getDashboardKpi/报表 BizQuery 的查询路径是否过 role-row-filter/data-auth 检查点）+ %test profile 运行时抽样实测（受限账号 vs admin 对同一 KPI/报表的可见差异，复用 permissions 测试账号池/loginAsRole 范式）；结论与缺口清单落 `dashboard-semantic-layer.md` §4。
      - Skill: `nop-testing`
      → 完成（2026-08-26）：静态表征 = 两检查点（CrudBizModel.appendFilter / ORM FILTER marker）均不在直连 DAO 路径（QueryBean→SQL enableFilter=false）；运行时实证 `TestErpSalDashboardRowFilterCoverage`（双销售员种子：看板跨用户聚合 vs CRUD 管道隔离对照 + 灰度 OFF 回归，2 tests 绿）；结论+缺口清单（G1/G2/G3）落 `dashboard-semantic-layer.md` §4。
- [x] Add: nop-datav DataAuth/RbacAuth 边界注记（master 实态引用：平台全链已合入 master 且 DataAuth/RbacAuth 实体在，但应用层看板未挂载 nop-datav，边界 = 平台能力存在 + 应用层不依赖）；`roles-and-permissions.md` 相应注记；`dashboard-semantic-layer.md` 中全部 2026-08-13 平台状态过期表述**全量对齐 master 实态**（覆盖段落：头部平台核对注记 / 来源与背景·平台能力基线 / 现状 vs Superset 对照表 / §0 平台能力边界 / §3 协议化暴露（ChatBI feat 分支注记）/ §4 行级安全 / AP-8 / 相关文档——不留内部自相矛盾的权威 owner doc）。
      → 完成（2026-08-26）：七段全量重写为 master 实态（nop-datav 全链已合入 master + DataAuth 经平台 CrudBizModel 管道同型机制 + 应用层零依赖引用边界 + 挂载触发条件 2026-08-26 重裁）；roles-and-permissions.md 数据权限节补边界注记。
- [x] Decision: E3.1b 缺口登记去向裁决——缺口实施属 enforcement 栈扩展：逐项判定归属（permissions-enforcement roadmap follow-up vs 本 roadmap 候选工作项），登记理由记录在案；本计划不实施任何缺口修复。
      → 裁决（2026-08-26）：G1（看板聚合）+ G2（报表数据集，与 R2-P1 保密面同面收口）→ permissions-enforcement roadmap 新增 §E3.1b P1 工作项（enforcement 栈扩展，理由与修复方向随项登记）；G3（管道内 finance 全见）→ 设计决定不修（E2.3 覆盖矩阵已记载）。本计划零缺口修复实施。

Exit Criteria:

- [x] 度量目录章节覆盖 10 域全部 KPI 且抽样口径与 value-spec 一致；E3.1b 核实结论 + 缺口登记落盘（无未登记的开放结尾）。

### Phase 3 — E3.2 库存审计快照查询

Status: completed
Targets: `module-inventory/erp-inv-service/`（BizModel + 对账校验）
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（E3.2 实现路径确认；预期零 ORM）

- [x] Add: `getInventorySnapshot(warehouseId, materialIds, asOfDate)` @BizQuery——派生视图实现（期初/流水汇总口径按 Phase 1 确认），返回数量 + 成本 + 库位维度；asOfDate 语义对齐 businessDate（AP-5）。
      → 完成（2026-08-26）：`IErpInvStockLedgerBiz.getInventorySnapshot`（IBiz 先行 + BizModel 实现；DB 级 GROUP BY 余额维度 orgId/warehouseId/locationId/materialId/skuId/batchNo/ownerId + SUM(quantity/totalCost)，unitCost=totalCost/quantity scale 4；返回 {asOfDate,rowCount,rows,totalQuantity,totalCost}）。
- [x] Add: 每日对账机制增「账面余额 = 快照派生值」一致性校验项（挂既有对账 Job/校验项族，`domain-design-guidelines.md` 对账机制扩展）。
      → 完成（2026-08-26）：`checkStockBalanceConsistency(asOfDate)` BizQuery（三型差异可观测：QTY_OR_COST_MISMATCH / BOOK_ONLY_NO_LEDGER / LEDGER_ONLY_NO_BALANCE，零值等价不报）+ deferred 作业 `erp-inv-stock-check` 落地为载体（`inv/stock-check.batch.xml` + `erp-inv-stock-check.job.yaml` config-gated 默认关闭 + 差异时作业失败可观测；job-scheduling.md §3.3 REGISTERED→WIRED）。
- [x] Proof: JUnit（快照 vs 既有链路数据确定性对照，含 asOfDate 边界：时点前/后流水裁剪）+ 浏览器层/GraphQL E2E 数值断言（对齐 value-spec 范式）；`Skill: nop-testing`
      → 完成（2026-08-26）：`TestErpInvSnapshotAndStockCheck` 5 用例绿（确定性对照流水直加 / 边界当日计入+时点后裁剪 / 仓库+物料过滤 / 一致基线 / 三型差异）；E2E `inv-snapshot.value.spec.ts` 绿（种子基线 180/10450、边界 06-29 空 & 07-02 仅期初、仓库过滤、一致性 mismatchCount=0）；inv 域全量 247 tests 零回归。

Exit Criteria:

- [x] BizQuery 可查且数值确定性可证（单测 + E2E 全绿）；对账校验项落地且失败模式可观测；零 ORM（或物化表触发结论已登记）。

### Phase 4 — E3.3 资产 ext 字段集 + E3.8 审计轨迹（assets 域同域两项）

Status: completed
Targets: `module-assets/`（orm.xml 仅当 Phase 1 裁决需要 / meta / service / web view.xml）
Skill: nop-backend-dev + nop-frontend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（E3.3 ext 用法核实 + E3.8 承载裁决 + ORM 清单 dual-agent 批准）

- [x] Add: E3.3 型号级 ext 字段键声明（承载形态按 Phase 1 结论，零 ORM 首选）+ 实例资产 ext 字段按型号校验（非法键/类型拒绝，ErrorCode 走 NopException 范式）。
      → 完成（2026-08-27）：承载按 Phase 1 裁决落 ORM（`ErpAstAssetModel.extFieldDefs` json-4000 + `ErpAstAsset.modelId/extFieldValues`，批准清单 #0/#1/#2）；`ErpAstAssetBizModel.defaultPrepareSave/Update` 钩子校验（无型号带值拒绝 / 非法键拒绝 / 必填缺失拒绝 / 类型不匹配拒绝，`ErpAstErrors` 5 个专用错误码 NopException 范式）。
- [x] Add: E3.3 字段集管理界面（view.xml 定制，flux 渲染，对齐 nop-frontend-dev 反模式自检）。
      → 完成（2026-08-27）：`ErpAstAssetModel` 页面（view.xml + action-auth + 菜单）+ `ErpAstAsset` 表单增 modelId/extFieldValues 字段；flux 渲染 E2E `ast-ext-fields-audit.value.spec.ts` 页面 CRUD 用例绿。
- [x] Add: E3.8 资产状态/归属变化审计记录（承载按 Phase 1 裁决；事件类型覆盖 owner doc §1 清单：CREATE/UPDATE/STATUS_CHANGE/MAINTENANCE/VALUATION/DISPOSAL/TRANSFER）+ `getAssetAuditTrail(assetId)` 时间轴 BizQuery。
      → 完成（2026-08-27）：独立实体 `ErpAstAssetActionLog`（批准清单 #3 + 字典 7 事件类型全覆盖）；`ErpAstAssetAuditRecorder` 在 CRUD 钩子 + 4 个 Processor（suspend/resume/维护完工/处置/价值调整）同事务记录；`getAssetAuditTrail` 时间轴 BizQuery（createTime+id 排序，from/to 快照 + 回链字段）。
- [x] Proof: JUnit + 浏览器层 E2E（字段集管理界面 CRUD + 实例校验负路径；审计时间轴查询含跨事件排序与回链）；`Skill: nop-testing`
      → 完成（2026-08-27）：`TestErpAstExtFieldsAndAuditTrail` 6 用例绿；E2E 3 用例绿（型号页 CRUD / 实例负路径 / 时间轴逆序+回链）；assets 域既有快照基线按 Phase 1 预告重录（加列+审计行），全量 `mvn test` 零回归。

Exit Criteria:

- [x] 两能力端到端可用（界面 + 查询口），审计事件类型全覆盖 owner doc 清单，测试全绿；assets owner doc 落地策略表更新。
      → 完成（2026-08-27）：`audit-trail-and-custom-fieldsets.md` 落地策略表两行 in progress → done + 实现注记。

### Phase 5 — E3.4 APS 求解器分离 + scheduleToc 瓶颈识别试点

Status: completed
Targets: `module-aps/erp-aps-service/`（接口 + 引擎 + TOC 实现）
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: 无（与 Phase 3/4 可并行）

- [x] Add: `IApsSchedulingSolver` 接口 + 既有贪心引擎适配为默认实现（**既有 scheduleForward/scheduleBackward 行为不变**，既有排产 JUnit/E2E 零回归即证明）+ bean 注册 + config 切换（对齐 D3 子计算器注入范式）。
      → 完成（2026-08-27）：`IApsSchedulingSolver` 接口 + `GreedyApsSchedulingSolver` 默认实现（`app-service.beans.xml` `ioc:collect-beans` 收集 + `ErpApsConfigs.CONFIG_SCHEDULING_SOLVER`（`erp-aps.scheduling-solver`）切换，Processor 按名选择）；既有前/后向排产行为经既有 JUnit/E2E（aps-operation-order/aps-rush-order/aps-schedule 3 spec 9 用例）零回归证明。
- [x] Add: `scheduleToc` 瓶颈识别试点——复用 `CrpLoadCalculator` 负荷率派生链（AP-3）识别 horizon 内超阈值瓶颈中心，先排瓶颈（拉动式）再排非瓶颈（前/后向兜底）；`SchedulingResult` 扩展 bottleneckMachineIds + 各中心负荷率。
      → 完成（2026-08-27）：`ApsBottleneckDetector`（复用 mfg 域 `IErpMfgCapacityProvider` SPI 负荷率派生链 + CRP 同源口径）+ `ErpApsSchedulingScheduleTocProcessor`（`scheduleToc` mutation：瓶颈中心工序优先排程，非瓶颈前/后向兜底）；`SchedulingResult` 扩展 `bottleneckMachineIds` + `machineLoadRates`。
- [x] Proof: JUnit（既有排产用例零回归 + TOC 用例：瓶颈中心优先排程可观测 + 结果扩展字段非空）+ 既有 aps E2E spec 扩展断言（如 `aps-operation-order`/`aps-rush-order` 范式适用则扩展，否则 JUnit 为准）；`Skill: nop-testing`
      → 完成（2026-08-27）：`TestErpApsSchedulingToc` 4 用例绿（瓶颈优先可观测 / 扩展字段非空 / 阈值边界 / 默认贪心行为不变）+ E2E `aps-schedule-toc.action.spec.ts` 绿（瓶颈排程 + 负荷率扩展断言）。

Exit Criteria:

- [x] 求解器可插拔（接口 + 默认贪心 + config 切换）且默认行为不变有测试证明；scheduleToc 试点落地 + 结果扩展可观测。

### Phase 6 — E3.5 文档摄取管道

Status: completed
Targets: `module-finance/`（ORM 加性变更如清单批准 / service 管道 / web 入口）、`../nop-entropy` 只读核实
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（OCR 选型 + nop-file 核实 + ORM 清单 dual-agent 批准）

- [x] Add: 管道实体与字段落地（按批准清单：文档引用/解析结果字段/文档处理日志；文件本体存 nop-file，业务表只存引用+解析字段——AP-3）。
      → 完成（2026-08-27）：`ErpFinApDocument`（fileId 引用 nop-file + parseResult/confidence/invoiceId 草稿回链）+ `ErpFinApDocumentLog`（RECEIVE/PARSE/CLASSIFY/DRAFT/MANUAL_REVIEW/RETRY/FAIL 七步轨迹）+ 3 字典（批准清单 #4/#5）；文件本体经 `nopFileStore`（`/f/upload` 端点）承载，业务表零全文。
- [x] Add: 管道步骤编排——上传入口 → OCR 解析（选型引擎，SPI 抽象）→ 分类（Phase 1 规则引擎：文件名/关键字段 → 单据类型/对应方 + 置信度；低置信挂人工队列）→ 草稿 `ErpPurInvoice`（approveStatus=UNSUBMITTED，人工确认门 AP-1）→ 三单匹配预填衔接（解析结果只作预填，校验走既有链路 AP-5）；异步步骤经 nop-job/事件（AP-4）。
      → 完成（2026-08-27）：`ErpFinApDocumentPipelineProcessor` 编排上传→解析→分类→草稿→预填（草稿经既有 `IErpPurInvoiceBiz` 管道创建 UNSUBMITTED，解析值只预填不校验）；config-gate `erp-fin.ap-doc-pipeline-enabled` 默认关闭；异步载体 = `fin/ap-document.batch.xml`（nop-batch）+ `erp-fin-ap-doc-processing.job.yaml`（nop-job，默认关闭）；低置信（<阈值）置 MANUAL_REVIEW 挂人工队列，人工门不可绕过。
- [x] Add: 分类引擎 SPI（Phase 2 ML 注入预留，对齐 `IErpFinAcctDocProvider` 注入范式）+ 审计追溯（文档处理轨迹日志 + 文档-发票-凭证回链）。
      → 完成（2026-08-27）：`IErpFinOcrEngine` SPI（默认 `ErpFinTextExtractOcrEngine`：PDFBox 数字 PDF/txt 文本抽取，零新增依赖；tess4j 适配器可插拔）+ `IErpFinApDocClassifier` SPI（默认 `ErpFinApDocRuleClassifier` 规则引擎，ML 注入预留）；追溯 = DocumentLog 步骤轨迹 + document.invoiceId 回链（草稿→后续过账经既有 voucher 回链链路闭环）。
- [x] Proof: JUnit（管道正路径 + 低置信人工门 + 幂等/失败重试路径）+ E2E（上传→草稿→三单匹配预填浏览器层断言）；config-gate 默认关闭下既有套件零回归；`Skill: nop-testing`
      → 完成（2026-08-27）：`TestErpFinApDocumentPipeline`（app-erp-all IT）6 用例绿（正路径含预填断言 / 低置信人工门 / 重复上传幂等 / 解析失败重试计数 / 默认关闭零暴露 / 处理轨迹落账）；E2E `fin-ap-document.value.spec.ts` 2 用例绿（真实数字文档上传→草稿预填 / 扫描件低置信 MANUAL_REVIEW）；全量 `mvn test` 默认配置零回归。

Exit Criteria:

- [x] 管道端到端落地（上传→OCR→分类→草稿→预填）且人工门不可绕过；测试全绿；OCR 引擎无外部云依赖。

### Phase 7 — E3.6 AI 接口层

Status: completed
Targets: `module-*/`（如需 actorType 字段按批准清单）、GraphQL/REST 通道验证、护栏接线
Skill: nop-backend-dev

- Item Types: `Add | Proof`
- Prereqs: Phase 1（GraphQL 可用性调研 + 最小落地集定稿 + ORM 清单裁决）

- [x] Add: AI 工具消费约定最小落地集（按 Phase 1 冻结清单逐项落地：action 元数据/描述补全等——不新建 AI 专用业务 API，AP-1 外挂化裁决）+ REST 与 GraphQL 双通道调用一致性验证。
      → 完成（2026-08-27，冻结 6 项逐项核对零增删）：①introspection config-gate JUnit 双证（`TestErpAiIntrospectionEnabled` 开启后 IntrospectionQuery 可用含 description / `TestErpAiIntrospectionDisabledByDefault` 默认关闭拒绝）；②11 个 `getDashboardKpi` 补 `@Description`（IBiz 同步，11 域 grep 核对）；③双通道一致性 E2E（`ai-interface.value.spec.ts` 同一 operation `/graphql` vs `/r/` 数值一致）；④护栏负路径 E2E（无权限角色经 `/r/` 调高影响 mutation 被拒）+ 限流复用 `IRateLimiter` 落地 E3.5 管道上传入口（`ErpFinConfigs` + Processor 令牌桶，人类用户面默认不限流）；⑤调用方身份落账断言（E2E 断言管道上传 createdBy=调用 userId；actorType 不落地按 Phase 1 裁决）；⑥`GraphQLToolProvider` 平台能力登记（`ai-native-interface.md` 前置调研结论 + `business-module-metadata.md` §6.2 雏形关系）。不新建 AI 专用业务 API；不采用 MCP（任何形态）。
- [x] Add: 护栏接线——高影响 action 确认门复用既有审批流（use-approval）；`actorType=AI` 审计标识（仅当 Phase 1 裁决需要，ORM 授权 §8.1）；限流复用 `IRateLimiter`。**不采用 MCP**（任何形态——用户裁决）。
      → 完成（2026-08-27）：高影响 action 门径 = 既有审批流复用验证（无权限角色负路径 E2E + 既有审批测试为证，AI 通道无法静默绕过）；actorType=AI 按 Phase 1 裁决**不落地**（触发条件登记于 `ai-native-interface.md`，§8.1 E3.6 授权行未使用）；限流 = `IRateLimiter` 接线于 E3.5 管道入口（自动化批量面）。零 MCP 引入（grep 核对无 MCP 适配/包装代码）。
- [x] Proof: JUnit + GraphQL/浏览器层 E2E（AI 通道调用正路径 + 未授权/超限负路径 + 审计标识落账断言）；`Skill: nop-testing`
      → 完成（2026-08-27）：JUnit 2 用例（introspection 开/关）+ E2E `ai-interface.value.spec.ts` 3 用例绿（双通道一致 / 未授权拒绝 / 身份落账）；全量 `mvn test` 零回归。

Exit Criteria:

- [x] AI 消费面按 Phase 1 冻结的最小落地集**逐项**落地且双通道一致（清单项零增删）；护栏负路径可证；`ai-native-interface.md` 落地策略表更新。
      → 完成（2026-08-27）：6 项冻结清单逐项核对（见上，零增删）；`ai-native-interface.md` 落地策略表随 Phase 8 更新。

### Phase 8 — 收尾：全量验证 + owner docs + roadmap 收口

Status: completed
Targets: `docs/backlog/erp-enhancement-roadmap.md`、各 owner docs、`docs/logs/`
Skill: nop-testing

- Item Types: `Proof | Add`
- Prereqs: Phase 2-7 全部

- [x] Proof: 全量验证（mission 注册标准）——`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 零回归 + `bash docs/audits/nop-compliance-checker.sh` 零漂移（基线漂移则按 known failure mode 开基线裁决，不得静默）+ 涉前端/GraphQL 项 E2E 全绿（flux 模式）。
      → 完成（2026-08-27）：①全 reactor `mvn clean install -DskipTests` BUILD SUCCESS（156 模块）；②全量 `mvn test` 零回归（新增 TestErpAstExtFieldsAndAuditTrail 6 / TestErpApsSchedulingToc 4 / TestErpFinApDocumentPipeline 6 / TestErpAiIntrospection×2 / TestErpInvSnapshotAndStockCheck 5 及 assets 快照基线重录后全绿）；③E2E flux 模式 19 用例绿（本计划 5 spec 10 用例 + 既有 aps/assets 回归 3 spec 9 用例）；④compliance checker：R7 本地 +1 = `_tmp/` git-ignore 草稿误报（CI 干净检出不命中，先例裁决不改基线）；**R2b +3（238→241）/ R2c +8（1529→1537）为已知漂移，显式登记不静默**——per-site 证据：`ErpAstAssetBizModel` 审计时间轴只读聚合 + 型号校验钩子内查（管道中段再入 IBiz 管道有重入风险）、`ErpInvStockLedgerBizModel` 一致性对账系统级读（须无行过滤）、`ErpAstAssetAuditRecorder` 业务事务内审计追加（posting-log recorder 同族）、`ApsBottleneckDetector` 排产系统级读（须无行过滤）、`ErpFinApDocRuleClassifier` 批处理上下文伙伴匹配、`ErpFinApDocumentPipelineProcessor`×2 自聚合实体 Processor 访问——均为基线既有同族合法模式（先例：F1/F2 批 per-site 上调 R2b 237→238 / R2c 1505→1529）；因「调高基线唯一途径=开独立计划」且本计划无权内联上调，按 known failure mode 登记 closure gates + Follow-up 归 successor 基线裁决计划（Fix 或 baseline-raise 二选一）。
- [x] Add: owner docs 对齐（**6 份**含落地策略表的 E1 设计文档——ai-native-interface / dashboard-semantic-layer / document-driven-ap-automation / audit-snapshot-cycle-count / audit-trail-and-custom-fieldsets / constraint-based-planning——落地策略表 todo→done + 实现注记；cross-domain-flow-orchestration.md 属暂缓 E3.7，不在 todo→done 之列）；`ai-native-interface.md` 中 2026-08-13 平台状态过期表述（来源与背景 / §3.5 / 相关文档）全量对齐 master 实态；roadmap §2 计数 + §5 E3 非暂缓项 done + §6 交付物 ✅（E3.7 保持 todo 暂缓）；**roadmap §3/§8 平台能力跟踪更新**（nop-datav 全链已合入 master 的 2026-08-26 活仓事实，替换 2026-08-13 过期核对记录——显式更新，不得静默）；`missions/erp-enhancement.json` description 中同型平台状态表述同步更新（第三真相源不得漂移）；`docs/logs/` 日志条目。
      → 完成（2026-08-27）：6 份 owner doc 落地策略表全部 done + 实现注记（dashboard-semantic-layer/audit-snapshot-cycle-count 随 Phase 2/3 已先行更新，余 4 份本次收口）；ai-native-interface.md 三处过期表述全量改 master 实态（来源与背景/§3.5/相关文档 + §2 护栏表 actorType 行落地裁决对齐）；roadmap §2（todo 9→1 / done 10→18）+ §5（E3 非暂缓 8 项全 done）+ §6（8 项 ✅ 已落地注记）+ §3/§4/§4.1/§8 nop-datav 全部过期表述显式更新为 master 实态；missions/erp-enhancement.json 同步（nop-datav master + 未挂载零依赖边界）；日志条目落 `docs/logs/2026/08-27.md`。

Exit Criteria:

- [x] 全量验证四路全绿并记录；roadmap/owner docs/日志一致收口。
      → 完成（2026-08-27）：构建/测试/E2E 三路全绿记录在案；compliance 路按 known failure mode 显式登记（R2b+3/R2c+8 per-site 证据 + successor 归属，见 Phase 8 Proof 注记与 Closure Gates 登记）。

## Draft Review Record

- Independent draft review iteration 1: **needs-revision**（task `ses_fc4bb390cffecEiJbolij5EZtJ`，fresh session）——A/C/D/E/F/G 七维通过、单一整体计划合规；1 BLOCKER：nop-datav「master 仅 chart 模块」基线失真（活仓证伪：`../nop-entropy` master HEAD 已跟踪 nop-datav 全模块链，2026-08-19~23 收口提交含 630 tests 全绿；roadmap §8 的 2026-08-13 核对记录过期），污染 E3.1b 交付物与 nop-datav Non-Goal 触发前提；5 MINOR（Phase 2 Decision 标注缺失 / Phase 7 验收基准未冻结 / OCR 系统级依赖与 playwright 措辞 / Phase 3 Prereqs 格式 / E3.1b 核实 Skill 标注）。已修复：Baseline+Goals+Phase 2+Non-Goals 平台状态全部改 master 实态 + 触发条件显式重裁 + roadmap §3/§8 更新项登记 Phase 8；五项 MINOR 逐一落实。
- Independent draft review iteration 2: **needs-revision（修复后可直接接受）**（task `ses_fc4b3cfaeffe7xq3PdYpJ41MIC`，fresh session 复检）——A/B/C/E 全过（基线活仓复核全吻合，含 nop-datav master 全链与 DataAuth/RbacAuth 测试类存在性、收口提交日期区间）；1 BLOCKER：过期表述清扫范围枚举不全——`dashboard-semantic-layer.md`（头部注记/平台能力基线/对照表/§4/AP-8/相关文档）与 `ai-native-interface.md`（来源与背景/§3.5/相关文档）仍有现在时 feat 分支陈述，原计划仅安排 §0 与 roadmap §3/§8 对齐，字面执行将产出内部自相矛盾 owner doc（规则 13 不可降级）；3 MINOR（「7 份 owner doc」计数应為 6 份、missions/erp-enhancement.json description 同型过期表述未安排更新、Draft Review Record 未持久化）。已修复：Phase 2 扩为 dashboard-semantic-layer.md 全段落清扫（显式枚举）；Phase 8 扩为 6 份枚举 + ai-native-interface.md 清扫 + mission 注册同步更新；Closure Gates 对齐门同步；本记录即 MINOR-3 修复。
- Independent draft review iteration 3: **acceptable as-is (accept)**（task `ses_fc4afbafcffeTHEvvfEHLdg6hp`，fresh session 确认性复检）——三项前轮修复全部核验到位（Phase 2 七段枚举 / Phase 8 ai-native-interface 清扫 + mission 注册更新 / 6 份计数枚举 / Review Record 持久化）；整体终检通过（规范正文零现在时 feat 分支残留、反松弛词零命中、roadmap §9 规则 2/6 合规）。1 新 MINOR（§3 协议化暴露的 ChatBI feat 注记未入字面枚举，已被「全部/全量」主句语义覆盖）——已顺手补入枚举。共识达成 → 转 active。

## Closure Gates

> 完整仓库验证在此一次性执行（Phase 8）：`mvn clean install -DskipTests` / `mvn test` / `bash docs/audits/nop-compliance-checker.sh` / 涉前端项 E2E（flux 模式）。阶段退出仅验证各自交付与下游解锁（见执行时规则 7）。

- [x] 范围内行为完成（8 个工作项全部落地，含 E3.1b 核实结论与缺口登记）
- [x] 相关文档对齐（6 份含落地策略表 E1 owner doc + 两份平台状态过期表述清扫（dashboard-semantic-layer / ai-native-interface）+ dashboards.md + roles-and-permissions.md + roadmap §3/§8 + missions/erp-enhancement.json）
- [x] 已运行验证：build / 全量 test / compliance / E2E 四路（Phase 8 记录）。**compliance 显式登记（非静默，known failure mode 裁决路径）**：R2b 238→241（+3）/ R2c 1529→1537（+8）为本计划新增 daoFor 站点（per-site 证据见 Phase 8 Proof 注记，均为基线既有同族合法模式）；基线上调须独立计划（`compliance-baseline.md` 回归门控规则），归 successor 基线裁决计划处置——登记时点 CI compliance 门为已知红，不得静默忽略。R7 本地 +1 为 `_tmp/` git-ignore 草稿误报（CI 不命中，先例裁决）。
- [x] 无范围内项目降级为 deferred/follow-up（确属触发条件驱动的深化项已在 Non-Goals/Deferred 显式移出）
- [x] 独立草案审查已完成并记录；ORM 变更（如触发）dual-agent-approval 记录在案
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

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

Status Note: 执行完成（2026-08-27）。8 个非暂缓工作项（E3.1/E3.1b/E3.2/E3.3/E3.4/E3.5/E3.6/E3.8）全部落地，Phase 1-8 全 completed。验证：全 reactor 构建 BUILD SUCCESS + 全量 `mvn test` 零回归 + flux E2E 19 用例绿（新 10 + 回归 9）；compliance R2b/R2c 漂移按 known failure mode 显式登记归 successor（见 Closure Gates 注记）。E3.7 保持暂缓（Non-Goals）。独立结束审计通过（2026-08-27，证据见下）。

Closure Audit Evidence:

- Auditor / Agent: MISSION_DRIVER:2026-08-26-222640-mission-driver 独立结束审计子代理（fresh session，非执行者会话），2026-08-27 执行
- Evidence: 活仓走查全绿——①E3.2 `ErpInvStockLedgerBizModel.getInventorySnapshot/checkStockBalanceConsistency` + `TestErpInvSnapshotAndStockCheck` + `inv/stock-check.batch.xml` + `app-erp-all/.../job/conf/erp-inv-stock-check.job.yaml` + E2E `inv-snapshot.value.spec.ts` 均在；②E3.4 `IApsSchedulingSolver`/`GreedyApsSchedulingSolver`/`ErpApsSchedulingScheduleTocProcessor` + `TestErpApsSchedulingToc` + E2E `aps-schedule-toc.action.spec.ts`；③E3.3/E3.8 `ErpAstAssetModel`/`ErpAstAssetActionLog` 实体 + `getAssetAuditTrail` + `ErpAstAssetAuditRecorder` + `TestErpAstExtFieldsAndAuditTrail` + E2E `ast-ext-fields-audit.value.spec.ts`；④E3.5 `ErpFinApDocument/ErpFinApDocumentLog` + `IErpFinOcrEngine`/`ErpFinTextExtractOcrEngine` + `IErpFinApDocClassifier`/`ErpFinApDocRuleClassifier` + `ErpFinApDocumentPipelineProcessor` + `fin/ap-document.batch.xml` + `erp-fin-ap-doc-processing.job.yaml` + `TestErpFinApDocumentPipeline`（app-erp-all IT）+ E2E `fin-ap-document.value.spec.ts`；⑤E3.6 `TestErpAiIntrospectionEnabled/DisabledByDefault` + E2E `ai-interface.value.spec.ts`；⑥E3.1 `dashboards.md` §KPI 度量目录（L236）+ `dashboard-semantic-layer.md` §4 核实结论；⑦roadmap §5 E3 非暂缓 8 项全 done（E3.7 todo 暂缓）；⑧`docs/logs/2026/08-27.md` 存在。plan-check --strict 修复后复跑 PASS（44/44）。compliance R2b+3/R2c+8 已显式登记归 successor 基线裁决计划（known failure mode，非静默），不阻塞关闭。

Follow-up:

- **compliance 基线裁决 successor**（非阻塞，known failure mode 登记路径）：R2b 238→241（+3）/ R2c 1529→1537（+8），全部 8 个新 daoFor 站点 per-site 证据见 Phase 8 Proof 注记（`ErpAstAssetBizModel`×2 / `ErpInvStockLedgerBizModel`×1 / `ErpAstAssetAuditRecorder`×1 / `ApsBottleneckDetector`×1 / `ErpFinApDocRuleClassifier`×1 / `ErpFinApDocumentPipelineProcessor`×2）；须开独立基线裁决计划（Fix 或 baseline-raise 带 per-site 证据，对齐 F1/F2 批先例）。
- E3.1b 缺口已按 Phase 2 Decision 登记去向（G1/G2 → permissions-enforcement roadmap §E3.1b P1；G3 → 设计决定不修），此处不重复列缺陷。
