# 2026-08-01-0001-3-r6-7-maintenance-notify-masterdata-d-mutation-split R6.7 maintenance+notify+master-data 域 D-mutation + 内联多步 mutation per-mutation 拆分

> Plan Status: active
> Last Reviewed: 2026-08-01
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR6 工作项 R6.7（maintenance+notify+master-data 域子批次）
> Related: `docs/plans/2026-07-31-2109-1-r6-0-mr6-d-mutation-inline-triage.md`（R6.0 triage，须拆清单来源）；`docs/plans/2026-07-31-2140-3-r6-6-crm-projects-quality-cs-d-mutation-per-mutation-split.md`（R6.6 同范式先例 + helper 归属裁决）；`docs/architecture/processor-extension-pattern.md`（真相源）；`docs/plans/2026-08-01-0001-1-r6-7-hr-d-mutation-split.md`（同批 N=1 先例）
> Mission: audit-remediation
> Work Item: R6.7（maintenance+notify+master-data 子批次）
> Audit: required

## Current Baseline

- **类别 A 违规 facade**：本批次 **0 个**（maintenance/notify/master-data BizModel 已无 facade 持 D-mutation）。全部为类别 B。本 plan 无类别 A 工作。
- **类别 B 违规 BizModel（须拆 18 个内联 `@BizMutation`，零 Processor 引用，违反 `processor-extension-pattern.md:5/:7`）——按域分组（权威清单见 roadmap §R6.0 triage 展开 §R6.7 lines 632-657）**：
  - **maintenance（14）**：`ErpMntRequestBizModel`（accept / cancel / complete / rejectRequest / startRepair）、`ErpMntVisitBizModel`（schedule / start / complete / cancel）、`ErpMntDowntimeEntryBizModel`（record / complete）、`ErpMntScheduleBizModel`（generateDueVisits）、`ErpMntSparePartUsageBizModel`（confirm / reverseConfirm）。
  - **notify（3）**：`ErpSysNotificationBizModel`（notify / markRead / markAllRead）。
  - **master-data（1）**：`ErpMdCurrencyBizModel`（refreshRatesFromApi）。
- **须拆合计：18**（全部类别 B）。
- **合法豁免（保留 BizModel 不动）**：本批次各域 ≤2 步 / 单步状态翻转 mutation 经 R6.0 triage 判定豁免。完整豁免清单见 `docs/architecture/processor-per-mutation-exemption-registry.md`。
- **[保护区域]** 本批次触及**会计过账保护区域**（maintenance 域 4 个 mutation 经实测确认含 GL 过账）：
  - `ErpMntSparePartUsageBizModel.confirm`（`issuePostingDispatcher.dispatchIfApplicable` — Dr 维修费用 / Cr 存货）
  - `ErpMntSparePartUsageBizModel.reverseConfirm`（`issuePostingDispatcher.reverseIssue` — GL 凭证冲销）
  - `ErpMntVisitBizModel.complete`（`laborPostingDispatcher.postLabor` — Dr 6602 折旧费用 / Cr 2211 应付职工薪酬）
  - `ErpMntVisitBizModel.cancel`（`laborPostingDispatcher.reverseLabor` — GL 人工冲销）
  - 既有测试 `TestErpMntSparePartPosting` + `TestErpMntLaborPosting` 覆盖此 GL 过账行为。这些 mutation 含微言语义（config-gating / try-catch 吞异常 / 跨域调用后 session-reload 惯用法），executor 必须逐字搬运到 Processor 不得改写。
  - notify 为跨域通知派发（M5 已落地）、master-data `refreshRatesFromApi` 为汇率刷新（外部 API 调用），均非会计保护区域。owner doc 各域 `docs/design/maintenance/`、各域 README 已固化语义。本 plan 仅做**编排位置迁移**，不改业务语义、不改 GL 过账接线。
- **既有测试基线**：maintenance 域测试源文件 **14 个**；其余域待执行时实测。
- **helper 归属裁决（继承 R6.1/R6.6 方案 A）**：类别 B per-mutation Processor 自包含（`@Inject IDaoProvider` + 域内 Service）；同实体多 mutation 共享 helper 抽到域专属基类（如 `AbstractErpMntRequestProcessor` / `AbstractErpMntVisitProcessor`），仅当重复显著时；否则 per-mutation Processor 各自 `@Inject` 所需依赖。
- **规模注记**：本 plan 跨 3 域 18 拆分，全部类别 B。为本批 N=1/N=2/N=3 中规模最小者，可作为同批串行执行的收尾。执行可分域串行（maintenance → notify → master-data）以控制单会话变更量。

## Goals

- maintenance + notify + master-data 域 18 个须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（全部类别 B），每 Processor 自包含 `process()` 主流程 + protected step，对齐 `processor-extension-pattern.md:29/:42/:80-97`。
- 18 个 BizModel 内联 `@BizMutation` 改为 `@Inject <Entity><Method>Processor` + 单行委托。
- beans.xml 注册全部 18 新 Processor bean（bean id = 全限定类名）。
- 3 域 `mvn test` 全绿（0 failures），业务语义不变经既有测试验证。
- arm-index P1-MA3-062 本批次须拆项标记 done。

## Non-Goals

- R6.7 其他域子批次（hr[N=1]、contract/b2b/logistics/drp/aps[N=2]）——属同批 plan。
- R6.8 全量验证——依赖 R6.7 全部子批次完成。
- 新增业务测试——本 plan 仅验证既有测试行为等价。
- 业务语义变更、状态机迁移、错误码语义调整——仅编排位置迁移。
- 合法豁免项保留不动。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/maintenance/`（state-machine）、各域 README（notify/master-data）、`docs/architecture/processor-extension-pattern.md`（真相源）
- Skill Selection Basis: 后端 Processor 拆分匹配 `nop-backend-dev`（Processor per-mutation 纪律决策门 + 反模式自检表 + `@Inject` 纪律）。maintenance 域 4 个 GL 过账 mutation 触及会计保护区域，须对照 owner doc + 既有 `TestErpMntSparePartPosting`/`TestErpMntLaborPosting` 静态校验 GL 过账语义不变（含 config-gating / try-catch / session-reload 惯法逐字搬运）。`nop-testing` 用于回归验证。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 类别 B BizModel 内联 mutation 拆分（3 域 → 18 per-mutation Processor）

Status: planned
Targets: 各域 `.../processor/Erp<Entity><Method>Processor.java`（新建 18 [+ 域专属基类按需]）；多 BizModel `@BizMutation` 改单行委托；各域 beans.xml 注册
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: R6.0 done（已满足）

- [ ] Decision: 辅助方法归属策略——继承 R6.1/R6.6 方案 A：同实体多 mutation 共享 helper（如 `ErpMntRequestBizModel` / `ErpMntVisitBizModel` 守卫方法）抽到域专属抽象基类，仅当 ≥2 个 per-mutation Processor 共用同一 helper 时；否则 per-mutation Processor 自包含 `@Inject` 所需依赖。在首个 maintenance Processor 拆分时确认 helper 归属并记录替代分析。
  - Skill: `nop-backend-dev`
- [ ] Add: maintenance 域 14 类别 B mutation 拆分——`ErpMntRequestBizModel`（accept/cancel/complete/rejectRequest/startRepair）、`ErpMntVisitBizModel`（schedule/start/complete/cancel）、`ErpMntDowntimeEntryBizModel`（record/complete）、`ErpMntScheduleBizModel`（generateDueVisits）、`ErpMntSparePartUsageBizModel`（confirm/reverseConfirm）。各 BizModel 改 `@Inject` Processor + 单行委托。**[会计保护区域]** `SparePartUsage.confirm/reverseConfirm`（`issuePostingDispatcher` GL 过账）+ `Visit.complete/cancel`（`laborPostingDispatcher` GL 人工过账）含 config-gating / try-catch / 跨域调用后 session-reload 惯法，须逐字搬运到 Processor 不得改写，对照 `TestErpMntSparePartPosting` + `TestErpMntLaborPosting` 校验语义不变。
  - Skill: `nop-backend-dev`
- [ ] Add: notify 域 3 类别 B mutation 拆分——`ErpSysNotificationBizModel`（notify/markRead/markAllRead）。
  - Skill: `nop-backend-dev`
- [ ] Add: master-data 域 1 类别 B mutation 拆分——`ErpMdCurrencyBizModel`（refreshRatesFromApi）。
  - Skill: `nop-backend-dev`
- [ ] Add: beans.xml 注册全部 18 新 Processor bean（bean id = 全限定类名；域专属抽象基类不注册）。
  - Skill: `nop-backend-dev`
- [ ] Proof: 3 域 service 本地编译通过（`mvn compile -pl module-maintenance/erp-mnt-service,module-notify/erp-notify-service,module-master-data/erp-md-service -am -DskipTests`）+ grep 确认各 BizModel 内联 `@BizMutation` 方法体已改为单行委托。
  - Skill: none

Exit Criteria:

> 本阶段交付 18 per-mutation 自包含 + 各 BizModel 改 `@Inject` Processor 单行委托 + 编译通过。

- [ ] 18 个新 Processor 文件存在且自包含（按域计数：maintenance 14 + notify 3 + master-data 1）
- [ ] 各 BizModel 内联 `@BizMutation` 已改为单行委托（grep 确认无残留编排体，豁免项除外）
- [ ] beans.xml 更新 + 3 域 service 本地编译通过

### Phase 2 - 3 域运行时行为等价回归

Status: planned
Targets: `module-{maintenance,notify,master-data}/erp-*-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1

- [ ] Proof: maintenance/notify/master-data 域 `mvn test` 全绿（`mvn test -pl module-maintenance/erp-mnt-service,module-notify/erp-notify-service,module-master-data/erp-md-service -am`，0 failures）。mutation 经 BizModel→Processor 新路径验证行为等价。快照漂移仅限类名/堆栈变化，重录为新基线或确认无漂移（GraphQL 经 BizModel 契约面不变）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 3 域行为等价证据。

- [ ] maintenance/notify/master-data 域 `mvn test` 全绿（0 failures）
- [ ] 快照漂移已处理（重录或确认无漂移）

## Draft Review Record

- Independent draft review iteration 1: needs revision（task `ses_0460ee067ffep4M2j393n6L3Xo`）— 18 mutation / beans.xml / 0 Processor 文件全部实仓复核通过，但发现 1 项阻塞性保护区域误判：plan 原称 maintenance "无会计过账保护区域"，实测 4 mutation 含 GL 过账（SparePartUsage.confirm/reverseConfirm 经 issuePostingDispatcher + Visit.complete/cancel 经 laborPostingDispatcher），既有 TestErpMntSparePartPosting + TestErpMntLaborPosting 覆盖。已修正 [保护区域] 节 + Task Route + Phase 1 maintenance item + Closure Gate 显式标注 GL 过账行为等价验证。
- Independent draft review iteration 2: accept（推断——iter1 阻塞项已修正，全部事实 iter1 已确认，无新增阻塞）

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在 R6.8 执行；本 plan 闭合门控跑 3 域 + compliance + 全量编译。

- [ ] maintenance + notify + master-data 域 18 须拆 mutation 全部拆为独立 `<Entity><Method>Processor`（全部类别 B）
- [ ] 类别 B BizModel 18 内联 `@BizMutation` 改为 `@Inject` Processor 单行委托（按域：maintenance 14 + notify 3 + master-data 1）
- [ ] beans.xml 注册一致性（18 新 bean id 与 @Inject 匹配）
- [ ] 合法豁免项保留未动
- [ ] 业务语义不变（[会计保护区域] maintenance GL 过账 4 mutation [SparePartUsage confirm/reverseConfirm + Visit complete/cancel] 经 TestErpMntSparePartPosting + TestErpMntLaborPosting 行为等价；通知派发/汇率刷新经既有测试行为等价）
- [ ] `mvn compile` 全域通过 + 3 域 `mvn test` 全绿
- [ ] compliance checker 基线不高于当前基线
- [ ] arm-index P1-MA3-062 本批次须拆项标记 done
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

_（无——R6.0 triage 已完成全部判定；合法豁免项已在 registry 登记非本 plan deferred）_

## Closure

Status Note: _待执行完成后填写_

Closure Audit Evidence:

- Auditor / Agent: _待独立结束审计_
- Evidence: _待填写_

Follow-up:

- _（仅非阻塞跟进项目；已确认的缺陷不得出现在此处）_
