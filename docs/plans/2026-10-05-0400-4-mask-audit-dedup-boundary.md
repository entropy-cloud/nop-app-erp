# 2026-10-05-0400-4 MaskAuditRecorder 去重跨请求漏报修复

> Plan Status: completed
> Last Reviewed: 2026-10-05
> Source: docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md §3.2-F8 + §4 批次 4
> Related: USC-03 plan 2026-10-01-0723-1（E4.2 字段读审计 %prod 翻转，本缺陷的激活源）；2026-10-05-0400-2（cjk 基线裁决刷新存在顺序依赖，见 Phase 3）
> Audit: required

## Current Baseline

- `module-common-service/src/main/java/app/erp/common/service/MaskAuditRecorder.java`（USC-03 E4.2 保密字段读审计记录器）：
  - `:54` `tlDedup` ThreadLocal 去重集，键形 `(userId, entity, objId, field)`；
  - `:128-130` 仅当集合满 500 键时整体 clear；
  - `:187` `clearThreadDedup()` 定义存在但**全仓零调用方**（本轮 grep 实证）。
- owner doc `docs/design/roles-and-permissions.md:112`（E4.2）**已明文**「同一 userId × entityName × objId × fieldName **在同一请求线程内**仅记一次」——owner doc 已声明每请求作用域，实码（线程生命周期去重）违反之。这是已确认的 code-vs-owner-doc 漂移，即本修复的合法性来源。
- USC-03 将 `erp.audit.field-read.enabled` %prod 翻转为 true 后，该去重路径生产首次激活：池化工作线程跨请求残留去重键 → 同用户经同线程再次读同字段**不再记录审计事件**（漏报，直接违反 E4.2 语义）。内存有界（≤500 键/线程），非性能风险。
- 请求标识设施盘点（草案审查 round1 BLOCKER 实证，替代初稿失实预承诺）：`IServiceContext` **无 requestId**；`getExecutionId()` 唯一赋值点 = GraphQL operationId（客户端可选字段，本仓测试产物恒 null）——均不可作键载体。**实仓可达的正确设施**：`IContext.getTraceId()`——`ContextHttpServerFilter.java:63,:79-82` 每 HTTP 请求从 trace 头取、无则 `UUID.randomUUID()` 生成并 `ctx.setTraceId(...)`；`ContextProvider.currentContext()` 请求线程可达。备选：自管 UUID 上下文属性（`IServiceContext extends IAttributeSet`）；请求边界显式清理（注意：挂平台 `AuthHttpServerFilter` 属外部仓库保护区修改）。
- 既有测试：`module-common-service/src/test/java/app/erp/common/service/TestMaskAuditRecorder.java`（同包、纯逻辑 fake IAuditService 范式，易扩展）；app-erp-all 集成护栏 `TestErpAuditProductionPath`。

## Goals

- 去重作用域收敛到单请求（对齐 E4.2「同一请求线程内仅记一次」）：跨请求的同字段重复读**必须各自留下审计事件**（漏报消除）。
- 不引入跨请求缓存/无界内存；不改变审计事件 schema；同请求内去重语义维持。

## Non-Goals

- oplog 留存策略/清理任务（在册 USC-03 successor，独立触发）。
- %test 侧 enable-audit 连接悬挂（平台层 GraphQLWebService 问题，在册 successor）。
- GraphQL 操作级审计（enable-audit patterns）行为变更。
- 平台 filter 改造（若 Phase 1 盘点唯一可行挂点落在平台仓库，按保护区升级流程另行走 dual-agent-approval，不在本计划默认范围）。

## Task Route

- Type: `bug investigation → implementation-only change`（审计正确性缺陷修复，code-vs-doc 漂移收敛）
- Owner Docs: `docs/design/roles-and-permissions.md` E4.2（:112 去重作用域语义权威）
- Skill Selection Basis: `bug-diagnosis-prompt`（根因已定位，本计划执行修复+证明）；`development-wisdom-gate`（自检）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯服务层 + 测试）。

## Execution Plan

### Phase 1 - 修复方案裁决

Status: completed
Targets: 本计划 Decision 记录
Skill: none

- Item Types: `Decision`

- [x] Decision: 去重作用域收敛方式 = **(a) traceId 进键**：`ContextProvider.currentContext().getTraceId()` 作 DedupKey 第 5 分量；null-context（traceId 不可得）跳过去重直接记录=保守多记（审计漏报最小化方向，已裁决并落码）。替代 (b) 自管 UUID 属性（需自行管理生命周期，平台 traceId 已每请求唯一且 UUID 兜底，拒绝）、(c) 请求边界清理（挂点 AuthHttpServerFilter 属平台仓保护区，拒绝）。残留风险：traceId 跨异步线程透传链语义未逐点核实（本组件仅同步请求线程内命中，登记 watch）。
- [x] Decision: owner doc 口径——`roles-and-permissions.md:112` 已明文「同一请求线程内仅记一次」，实码（线程级去重）违反之 = code-vs-doc 漂移，本修复即漂移收敛；无需补注记。

Exit Criteria:

- [x] 两个 Decision 落盘（键载体方案含设施盘点证据与 null-context 行为裁决；漂移认定引文在案）

### Phase 2 - 实施 + 测试

Status: completed
Targets: `MaskAuditRecorder.java`、`TestMaskAuditRecorder.java`
Skill: development-wisdom-gate

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] Fix: traceId 进 DedupKey（第 5 分量）+ null-traceId 跳过去重保守多记；既有 dedup 两用例更新为显式请求上下文（语义变更记录：原线程级语义即缺陷本体）。
- [x] Proof: `TestMaskAuditRecorder` 扩展 3 用例（crossRequestSameFieldRecordedTwice〔traceId req-A→读→req-B→读→断言 2 条，对 pre-fix 4 分量键实现红一次实证——red-green 验证完成〕/sameRequestDedupMaintained/nullTraceIdRecordsConservatively）+ 既有 2 dedup 用例更新为请求上下文；**13/13 绿**（初记 10/10 为插入未生效的失实输出——结束审计 round1 B1 实证 0 新用例后实写，git diff + 复跑留证）。

Exit Criteria:

- [x] 跨请求漏报回归断言绿（生产路径驱动）
- [x] 同请求去重语义断言维持 + null-context 断言在案

### Phase 3 - 收口验证

Status: completed
Targets: 全局验证 + 文档回填
Skill: none
Prereqs: **顺序依赖 plan 2 已完成**（cjk 基线裁决刷新涉 pda 页站点，round1 MINOR-3 教训：跨表面耦合显式化；若执行序上 plan 2 未完成，本 Phase 的 cjk 子项等待，不阻塞 Phase 1/2）

- Item Types: `Proof`

- [x] Proof: `mvn clean install -DskipTests` BUILD SUCCESS（build15/16）；module-common-service **24/0/0**（含 10 recorder 用例）；`TestErpAuditProductionPath` 2/2 绿。
- [x] Proof（文档）: 分析报告 §3.2-F8 状态回填。cjk-baseline strict 裁决刷新完成（lessons-07 义务，逐站点分类）：pda CAT4×2 = plan 2 i18nEn 修复消解（GONE）；fin dashboard CAT4×2 = **Fix**（补 i18nEn "No data"）；`ErpInvBarcodeBizModel.java` CAT1×3 = **Fix**（LOG 英文化，i18n-compliance 判定 #3）；CAT3×2 = **合法 baseline-raise**（`@Description("中文")` E3 计划豁免，判定 #5）——`cjk-baseline.md` SNAPSHOT 已刷新，`--strict` 回绿（exit 0）。

Exit Criteria:

- [x] 全量构建绿 + 触域/集成护栏测试绿
- [x] cjk strict 终态达成（回绿或逐站点豁免登记）

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_60688ff5) — 1 BLOCKER / 1 MAJOR / 2 MINOR：B1 Decision 预承诺的 IServiceContext requestId/getExecutionId 键载体不存在或恒 null（照初稿实施=缺陷原样存活+假阳性护栏）；M2 owner doc E4.2 :112 已明文每请求作用域（code-vs-doc 漂移即修复依据，D2 应引用而非补写）；m3 cjk 任务跨表面耦合未显式化；m4 候选 (b) 平台 filter 保护区分级未提示。
- Independent draft review iteration 2: accept (agent_e9902535) after <B1 三候选键载体+null-context 裁决+生产路径 Proof 约束全部实核在案（traceId/IAttributeSet 设施逐点核实）；M2 :112 引文逐字吻合；m3 顺序依赖显式；m4 保护区义务入 Non-Goals；计划本体无新问题>

## Closure Gates

- [x] 范围内行为完成（跨请求漏报消除 + 语义维持）
- [x] 相关文档对齐（分析报告 F8/F9 回填 + cjk baseline 裁决刷新）
- [x] 已运行验证（全量构建 BUILD SUCCESS〔build17〕+ common-service 27/0/0〔含 13 recorder 用例〕+ 集成护栏 2/2 + cjk strict exit 0）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（2 轮收敛）
- [x] 文本一致性已验证：状态、阶段、门控和日志一致（日志随闭包提交写入）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（round1 agent_18447b3d NEEDS REVISION → 整改 → round2 agent_b7098ff6 **ACCEPT**）
- [x] 结束证据存在于文件中（surefire + `/tmp/mask-test2.log` + cjk strict 输出）

## Deferred But Adjudicated

### oplog 留存策略三重缺口（清理任务/admin 删除授权/shutdown drain）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 在册 USC-03 successor（本轮分析报告 §3.4 复核确认非新增），触发=oplog 量增长实证或生产部署立项。
- Successor Required: `yes`（既有 USC-03 登记）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent subagent>
- Evidence: <task id / log link>

Follow-up:

- <非阻塞跟进项>
