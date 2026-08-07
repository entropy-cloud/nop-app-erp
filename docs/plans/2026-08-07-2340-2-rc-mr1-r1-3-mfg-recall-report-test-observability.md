# 2026-08-07-2340-2-rc-mr1-r1-3-mfg-recall-report-test-observability RC-R1.3 — mfg 召回报告测试补强 + best-effort 基因链写失败可观测性（P1-RC-010，MR1 第一批纯预授权）

> Plan Status: active
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: RC-R1.3（MR1 第一批纯预授权：mfg 召回报告测试补强 + best-effort 可观测性增强，P1-RC-010）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.3 行 + `docs/audits/arm-index.md` P1-RC-010 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` §3.1（RC-R1.3 = 「纯测试补充 + 可观测性增强（预授权）」）
> Related: `docs/audits/2026-08-06-2025-rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability.md`（A4.2.9 residual observability gap）；`docs/audits/2026-08-06-2247-rc-ma4-a4-2-11-mfg-recall-report-degraded-business-coverage.md`（A4.2.11 业务覆盖维度）；`docs/audits/2026-08-06-2025-rc-ma4-a4-2-4-mfg-variance-posting-failure-notify-runtime.md`（A4.2.4 dispatchVarianceFailureAlert 范式）；`docs/design/manufacturing/batch-genealogy.md`（owner doc）
> Audit: required

## Current Baseline

- **finding P1-RC-010（arm-index 行）**：UC-MFG-13⑫ 召回报告测试仅冒烟（testRecallReport 弱断言）+ best-effort 基因链写失败路径零测试覆盖。测试补充义务归 MR1（A4.2.9/A4.2.11 均维持「P1-RC-010 测试补充义务不撤销」）。
- **A4.2.9 运行时确认（`2026-08-06-2025` 报告）**：`BatchGenealogyWriter.writeOnCompletion` catch 分支（`:73-75`）**仅 LOG.error**——无 notify 告警派发、无失败标记持久化、无 metrics；LOG.error 无监控采集通道；config `erp-mfg.genealogy-write-enabled` 默认 **true**（best-effort 写入路径默认活跃）；与完工差异过账失败（`dispatchVarianceFailureAlert` 双通道）**可观测性不对称**。residual observability gap 修复方向（报告 §183/§227）：「catch 分支加 notify 告警派发（对齐 A4.2.4 dispatchVarianceFailureAlert 范式），纯 BizModel 代码逻辑预授权自动执行不触 §5 ask-first；**失败标记持久化若引入新 ORM 字段则触 ORM 须 ask-first**（notify 通道增强不需 ORM 变更，为推荐路径）」。
- **A4.2.11 业务覆盖维度（`2026-08-06-2247` 报告）**：recallReport（`ErpMfgBatchGenealogyBizModel.recallReport:72-107`）反向递归（BFS + visited 防环 + 无 maxDepth 截断）**完整覆盖**所有可达产出成品批次；`degraded=true`（`:78`）结构性恒置**如实反映**位置/去向缺失现状；L1 UC-MFG-13 ⑫ 仅要求「识别」——降级版满足。**维持 degraded 语义不变**。
- **测试现状**：`TestErpMfgBatchGenealogy.testRecallReport:220-237` 仅断言 status=0 + data 非空（**未断言 sourceLotId / degraded / affectedLots 内容**——受影响的成品批次集合正是召回需求的核心）；best-effort 写失败路径（`writeOnCompletion` catch）**零测试**（既有 5 个 @Test 均成功路径）。`TestErpMfgVarianceAlert`（THRESHOLD 通道 `mfg.production-variance`）提供 notify 断言范式（findNotification + seedNotificationTemplate USER_LIST），与本 finding 的 FAILURE 通道不同 event。
- **notify 范式（A4.2.4）**：`ErpMfgWorkOrderProcessor.dispatchVarianceFailureAlert:150-167`——`notificationBiz.notify(NOTIFY_EVENT_VARIANCE_FAILURE="mfg.production-variance-posting-failure", ctx, new ServiceContextImpl())`，ctx 含 workOrderId/workOrderCode/errorCode/errorMessage/postingNo，外层 try/catch LOG.warn 降级不阻断主流程；`NOTIFY_EVENT_VARIANCE_FAILURE` 常量在 `ErpMfgWorkOrderProcessor:89`（类内 package-private 静态常量，非 private）。
- **预授权判据**（第一批纯预授权）：不触 ORM/会计核心/删除（notify 通道增强纯 BizModel 代码逻辑）；**无 ask-first checkbox**。owner doc `batch-genealogy.md` 记录 best-effort 失败语义与 config 键（写入时机/失败语义三 Decision 见 plan `2026-07-07-0305-3`）。
- **roadmap RC-R1.3 行**：`todo`，Deps（R1.0 done）已满足，第一批纯预授权（§7 A1 裁决，自动执行无人工介入）。

## Goals

- **testRecallReport 强化断言**：sourceLotId==入参、degraded==true、affectedLots 内容完整（受影响成品批次集合 = lotB/lotC，逐项断言 lotId/batchNo/materialId/lotStatus）——对齐 A4.2.11「完整覆盖」已证实行为，把弱断言升级为强断言（P1-RC-010 测试补充义务）。
- **best-effort 写失败 mock 测试**：doWrite 抛异常（失败注入）→ 完工不被阻断（writeOnCompletion 不 rethrow）+ catch 分支 notify 派发被触发 + ErpSysNotification 行落库（对齐 `TestErpMfgVarianceAlert` 断言范式）。
- **catch 分支 notify 告警派发**：`BatchGenealogyWriter` catch 分支增 `dispatchGenealogyWriteFailureAlert(wo, e)`（镜像 `dispatchVarianceFailureAlert` 范式：新 event 常量 `mfg.production-genealogy-write-failure` + ctx 含 workOrderId/workOrderCode/errorCode/errorMessage + try/catch 降级不阻断）——消除 A4.2.9 可观测性不对称。
- 回填 arm-index P1-RC-010 → `done (RC-R1.3)` + roadmap RC-R1.3 → `done` + owner doc 补注 + `docs/logs/` 日志条目。

## Non-Goals

- **不做失败标记持久化**（引入新 ORM 字段触 ask-first，越出第一批预授权边界——登记 Deferred But Adjudicated，Successor Required: yes 触发条件 = 人工裁决/第二批）。
- **不改 degraded 语义与 recallReport 返回结构**（A4.2.11 裁决维持：结构性恒置如实反映现状，非缺陷；`RecallReport` DTO 不动）。
- **不做 inventory 位置/去向查询**（A4.2.11 successor watch-only，触发条件未满足）。
- **不改 config 默认值**（`erp-mfg.genealogy-write-enabled` 默认 true 保持——写入路径活跃是既有裁决，本计划只增强失败可观测性）。
- **不改真相源**（use-cases/owner doc 需求契约段；batch-genealogy.md 仅补实现注记）。
- **不新增 ErpMfgConstants 之外的平台级变更**（event 常量可放 Writer 类内 package-private 静态常量，对齐 `NOTIFY_EVENT_VARIANCE_FAILURE` 先例；是否提升到 ErpMfgConstants 由执行时按同域约定裁决）。

## Task Route

- Type: `implementation-only change`（P1 测试补充义务 + 预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/manufacturing/batch-genealogy.md`（写入时机/失败语义）+ `docs/audits/2026-08-06-2025-rc-ma4-a4-2-9-...md`（可观测性缺口证据）+ `docs/audits/2026-08-06-2247-rc-ma4-a4-2-11-...md`（业务覆盖证据）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）
- Skill Selection Basis: 实现面 = BizModel/Processor 级代码 + notify 派发（`nop-backend-dev`：protected step 方法、错误处理、跨实体调用经 I*Biz 注入规则——`IErpSysNotificationBiz` 按规则注入）+ JUnit 测试（`nop-testing`：JunitAutoTestCase/@NopTestConfig/seed 范式 + notify 断言镜像 `TestErpMfgVarianceAlert`）。无 view.xml/xbiz 变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 无新 config/端口/外部服务（复用既有 notify 基础设施 `IErpSysNotificationBiz.notify`；通知模板 seed 由测试 seedNotificationTemplate USER_LIST 提供，镜像 `TestErpMfgVarianceAlert`）。
- 分域验证前置：`mvn install -DskipTests`（依赖模块就位）后 `mvn test -pl module-manufacturing/erp-mfg-service`。

## Execution Plan

### Phase 1 - catch 分支 notify 告警派发实现

Status: planned
Targets: `module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/genealogy/BatchGenealogyWriter.java`；`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/ErpMfgConstants.java`（按执行时裁决是否登记）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Fix`
- Prereqs: 无（既有基线）

- [ ] `Decision` 告警 event 命名与派发形态：镜像 `dispatchVarianceFailureAlert`（`ErpMfgWorkOrderProcessor:150-167`）——event = `mfg.production-genealogy-write-failure`（同域命名族：`mfg.production-variance` / `mfg.production-variance-posting-failure`），ctx = workOrderId/workOrderCode/errorCode/errorMessage，外层 try/catch LOG.warn 降级不阻断完工；事件模板不预置（无 ACTIVE 模板时 notify config-gated 静默跳过，对齐 `IErpSysNotificationBiz.notify` 契约——文档说明「运营侧配置模板后生效」）。备选（否决）：复用 `mfg.production-variance-posting-failure` event——否决理由：事件语义不同（差异过账失败 vs 基因链写失败），复用将致模板接收人/上下文混同，且 A4.2.4/A4.2.9 报告均建议独立通道。
      - Skill: `nop-backend-dev`
- [ ] `Decision` 失败注入测试形态（Phase 2 前置契约）：mock 方式 = 构造派生 Writer（同包子类覆盖 `doWrite` 直接抛 NopException）注入被测 `writeOnCompletion`（`doWrite` 为 protected 可覆盖，`BatchGenealogyWriter:80`）——避免改生产代码加测试钩子；或 seed 数据致 doWrite 内部抛错（如 outputLine 缺 destWarehouseId 时返回不抛——不可用，须显式抛错的派生 Writer）。备选（否决）：反射/字节码 mock 框架——否决理由：项目测试栈以 seed 数据 + 派生覆盖为主，无 mock 框架先例。
      - Skill: `nop-backend-dev`
- [ ] `Fix` `BatchGenealogyWriter` catch 分支（`:73-75`）增 `dispatchGenealogyWriteFailureAlert(wo, e)` 调用（protected 方法，镜像 dispatchVarianceFailureAlert 形态：@Inject IErpSysNotificationBiz + null 守卫 + try/catch 降级 + ctx 构造）；`LOG.error` 保留（失败详情仍须可查，消息含 workOrderCode 为既有行为 `:74`）。**`@Inject` 字段不可 private（Nop IoC 规则）且 mock 测试需跨包手工装配——增 `setNotificationBiz` public setter（镜像 `setDaoProvider:56-58` 先例），供测试装配 + IoC 注入双通道**。
      - Skill: `nop-backend-dev`
- [ ] `Fix` 常量登记：`NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE`（对齐先例：`ErpMfgWorkOrderProcessor:89` 类内 package-private 静态常量 vs `ErpMfgConstants` 全局常量——按同域约定裁决；仅一处定义，单一真相源）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 失败注入下 `writeOnCompletion` 不 rethrow（完工不阻断）+ notify 调用可达（成功与失败模式：派生 Writer 抛错 → catch 内派发；无 ACTIVE 模板 → 静默跳过不抛）
- [ ] 无 ORM/契约变更（本阶段产物仅 Java 代码 + 常量）

### Phase 2 - 测试补强（recallReport 强断言 + 写失败 mock + notify 断言）

Status: planned
Targets: `module-manufacturing/erp-mfg-service/src/test/java/app/erp/mfg/service/TestErpMfgBatchGenealogy.java`（强化 + 追加）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Add` testRecallReport 强化断言：`sourceLotId` == 入参 lotA + `degraded` == true + `affectedLots` 大小 == 2 且逐项断言 lotId（lotB/lotC）/ batchNo / materialId / lotStatus（`RecallReport.AffectedLot` 字段集，`RecallReport.java:51-91`）+ REJECTED 批次排除分支（seed 一个 REJECTED 状态产出批次 → 不出现在 affectedLots，对齐 `collectAffectedIfFinishedGood:115-117` 行为）。
      - Skill: `nop-testing`
- [ ] `Add` best-effort 写失败 mock 测试：派生 Writer 覆盖 `doWrite` 抛 NopException → `writeOnCompletion` 调用不抛（断言完工不被阻断语义）+ catch 分支 notify 派发（seed USER_LIST 通知模板后断言 ErpSysNotification 行落库，镜像 `TestErpMfgVarianceAlert.findNotification:160-166`）**+ 无 ACTIVE 模板 → 静默跳过不抛断言**（对齐 `IErpSysNotificationBiz.notify` 契约，Phase 1 Exit 成功/失败模式全覆盖）；config `erp-mfg.genealogy-write-enabled=false` 时跳过（不派发）回归断言。
      - Skill: `nop-testing`
- [ ] `Proof` 断言强度：拒绝/失败路径断言错误码 + 参数 + 事务回滚后状态（对齐既有拒绝范式）；通知断言含 eventType + recipientUserId（USER_LIST 模板接收人）+ status=SENT。**LOG 断言说明**：arm-index 修复面含「LOG.error 含 workOrderCode」——module-mfg 无日志捕获 harness（grep captureLog/OutputCapture 零命中），以 notify 落库断言 + `:74` 既有 LOG 消息文本（含 wo.getCode()）实仓引用替代，偏离记录于报告。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 新/强化测试全部落地并绿：`mvn test -pl module-manufacturing/erp-mfg-service` 全绿（既有 tests 零回归；erp-mfg-service 无 `_cases/` 快照先例，以既有测试类形态为准）

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: planned
Targets: `docs/design/manufacturing/batch-genealogy.md`（实现注记）；`docs/audits/arm-index.md`（P1-RC-010 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.3 done + Owner Doc 列修正）；`docs/logs/2026/08-08.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [ ] `Add` owner doc 补注：`batch-genealogy.md` 记录 catch 分支 notify 告警派发（event + 形态 + 配置模板说明）；不修改需求契约段（真相源冻结条款遵守）。
      - Skill: none
- [ ] `Add` arm-index P1-RC-010 行「修复状态」→ `done (RC-R1.3)` + 修复落地摘要（测试补强 + notify 通道）；roadmap RC-R1.3 → done **+ roadmap 行 Owner Doc 列修正**（原指向 `variance-analysis.md` 与修复面不符 → `batch-genealogy.md`，执行时同步修正）；`docs/logs/2026/08-08.md` 日志条目（当日实际日期为 2026-08-08，对齐 RC-R1.20 日志路径修正先例）。
      - Skill: none

Exit Criteria:

- [ ] arm-index/roadmap 状态回填 + owner doc 补注落盘；日志条目写入

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 `ses_02317535effeKHGfiexBbhSoh7`，fresh session）——0 Blocker 0 Major；6 Minor advisory 全量落地：M1（LOG.error 含 workOrderCode 断言——无日志捕获 harness，以 notify 落库断言 + `:74` 实仓引用替代并记录偏离）✓ / M2（派生 Writer 跨包装配不到 package-private @Inject——Fix 项增 `setNotificationBiz` public setter 镜像 `setDaoProvider:56-58`）✓ / M3（`:89` 为 package-private 非 private 措辞）✓ / M4（无 ACTIVE 模板静默跳过缺测试项——mock 测试增无模板断言）✓ / M5（erp-mfg-service 无 `_cases/` 先例——快照措辞改为按既有测试类形态）✓ / M6（roadmap 行 Owner Doc 列 `variance-analysis.md` 与修复面不符——Phase 3 增列修正）✓。共识达成，转 active。
- Independent draft review iteration 2: `accept`（独立子代理 `ses_0230f88d1ffe00s7KkqX22gH7U`，fresh session）——6 Minor 逐项实仓核验 RESOLVED（M1 无 harness grep 零命中 + `:74` 含 wo.getCode()；M2 setDaoProvider:56-58 实仓 + `ProductionVarianceCalculator:96` setNotificationBiz 先例 + `TestErpMfgVarianceRecomputeReversal:264-267` new Dispatcher + 手工 setter 失败注入先例；M3 `:89` 无修饰符 = package-private；M4 `ErpSysNotificationNotifyProcessor:43-46` 无模板 LOG.warn + 空表契约实仓；M5 mfg 无 `_cases/`；M6 roadmap `:371` Owner Doc 列实仓确认）+ 全量复检 PASS（格式/Deps/预授权/范围/反松弛/规则 13/行号引用）。2 cosmetic advisory 已修订（`类内私有常量` 措辞 → package-private，两处）。共识达成，转 active。

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐
- [ ] 已运行验证（`mvn test -pl module-manufacturing/erp-mfg-service` + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline——notify 派发经 `IErpSysNotificationBiz` 注入不引 daoFor 漂移，防基线漂移）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 失败标记持久化（best-effort 写失败落库标记，如 lastFailureAt/retryCount 列）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 引入新 ORM 字段触 §5 ask-first，越出第一批纯预授权边界；A4.2.9 报告明确「notify 通道增强不需 ORM 变更，为推荐路径」。notify 告警已消除运营不可感知缺口；持久化标记为深化增强。
- Successor Required: yes（触发条件 = 第二批启动或人工裁决 ORM 授权）

### inventory 位置/去向查询（recallReport 位置/去向维度）

- Classification: `watch-only residual`
- Why Not Blocking Closure: A4.2.11 裁决维持——inventory 域查询方法集零暴露，触发条件未满足；degraded=true 结构性如实反映，L1 仅要求「识别」。
- Successor Required: no（watch-only；触发条件 = inventory 域暴露位置/去向查询）

### degraded 语义变更

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A4.2.11 裁决 degraded=true 结构性恒置如实反映现状（位置/去向缺失），非缺陷；本计划只补测试断言该语义，不改实现。
- Successor Required: no

## Closure

Status Note: 待执行。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话）
- Evidence: 待执行

Follow-up:

- 无（范围内项目全落地后关闭；失败标记持久化 successor 触发条件见 Deferred But Adjudicated）
