# 2026-08-06-2025-1 rc-ma4-a4-2-4-mfg-variance-posting-failure-notify-runtime 完工差异过账失败告警通道 notify 投递 + 运营响应闭环运行时确认（A1.9 SP-1 + A1.11 SP-1 合并，P1-MA4-007 resolved R1.16 运行时落地核验）

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.4（A1.9 SP-1 + A1.11 SP-1 合并：MA4 运行时行为验证 — 完工触发差异过账失败运行时悬挂可见性，同根因[P1-MA4-007 完工编排层差异吞咽致业财悬挂，已 resolved R1.16 G3 错误分级 + 告警派发]同控制点[`IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure")` 告警投递 + `ErpMfgCostVariance__calculateVariances` 手动重算入口]；SP-1 = notify 实际运行时投递成功率（best-effort 降级不阻断主流程）+ 运营对告警的响应闭环（手动重算入口是否被实际使用）需运行时确认）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.4；存疑点来源 `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md` §7 SP-1 + `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-1（两报告 §7 SP-1 明确标注「与 A1.9 SP-1 同根因」）
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.4 实体行）、`docs/audits/arm-index.md`（P1-MA4-007 finding 行，resolved R1.16）、`docs/plans/2026-08-06-1926-2-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`（范式参照：mfg 成本过账运行时探针 + config-gated 路径裁决）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-2025-rc-ma4-a4-2-4-mfg-variance-posting-failure-notify-runtime.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：notify 投递机制 census + dispatchVarianceFailureAlert 调用链 + 手动重算入口可达性 + config 默认值 + 监控/告警采集现状）。范式对齐 A4.2.6-8（done — mfg 成本过账 config-gated 路径运行时探针先例）。

- **存疑点原文**（A1.9 §7 SP-1 `2026-08-02-2042-3-...-a1-9-...md` §7:310 + A1.11 §7 SP-1 `2026-08-02-2245-...-a1-11-...md` §7:311，两报告逐字标注同根因）：
  - 「**完工触发差异过账失败运行时悬挂可见性**：P1-MA4-007 已 resolved（R1.16 G3 错误分级 + 告警派发落地），但告警通道 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure")` 的实际运行时投递成功率（notify best-effort 降级不阻断主流程）+ 运营对告警的响应闭环（手动重算入口 `calculateVariances` 是否被实际使用）需运行时确认。」触发条件：config=true（业务要求完工自动算差异）+ 永久性失败（标准成本未发布/卷算 base cost 缺失）。

- **关联既有 finding**：
  - **P1-MA4-007**（arm-index，resolved R1.16 done）：完工编排层差异吞咽致业财悬挂。R1.16 实施 G3 错误分级 + 告警派发闭环落地。**本验证确认 R1.16 修复的运行时可见性是否真正达成**——告警通道投递可靠性 + 运营响应闭环可达性。若运行时确认告警通道 best-effort 降级致投递不可靠且无运营响应闭环 → 维持 P1-MA4-007 resolved 但登记 residual observability gap watch-only（归 MR1）；若告警投递可靠（同步落 ErpSysNotification 持久化 + 运营经工作台可见）+ 手动重算入口可达 → 闭合运行时可见性，无新 finding。

- **需求契约（L1 权威）**：`docs/design/manufacturing/use-cases.md` UC-MFG-07 工单完工入库与成本结转（差异过账链）+ UC-MFG-12 完工触发差异过账。L1 要求差异过账失败可被运营感知并可干预（R1.16 resolved P1-MA4-007 即据此）。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **告警派发链**（R1.16 landed，已 live 核实）：
    - `ErpMfgWorkOrderProcessor.java:89` `static final String NOTIFY_EVENT_VARIANCE_FAILURE = "mfg.production-variance-posting-failure"`
    - `ErpMfgWorkOrderProcessor.java:150` `protected void dispatchVarianceFailureAlert(ErpMfgWorkOrder wo, Exception cause)`
    - `ErpMfgWorkOrderReportCompletionProcessor.java:99` `facade.dispatchVarianceFailureAlert(wo, e)`（完工差异过账失败时调用）
  - **notify 投递实现**（待 Phase 1 census 其投递语义——同步/异步/持久化/降级）：
    - `ErpSysNotificationBizModel.java:54` `public List<ErpSysNotification> notify(@Name("eventType") String eventType, ...)`（module-notify）→ `:57` 委托 `notifyProcessor.notify(...)`
    - `ErpSysNotificationNotifyProcessor.java:40` `public List<ErpSysNotification> notify(String eventType, Map<String,Object> context, IServiceContext ctx)`
  - **手动重算入口**（运营响应闭环，待 Phase 1 census 其可达性）：
    - `ProductionVarianceCalculator.java:106` `public List<ErpMfgCostVariance> calculateVariances(Long workOrderId)`
    - GraphQL mutation `ErpMfgCostVariance__calculateVariances`（`TestErpMfgVarianceRecomputeReversal` 经此 RPC 调用证实可达——Phase 1 复核 xbiz 注册 + reverseIfExists 红冲 + 重算语义）
  - **config**（已知 baseline，Phase 1 复核部署 override）：
    - `ErpMfgConstants.java:172` `erp-mfg.variance-auto-calc-enabled` 默认 **false**（注释「true=工单完工（willFinish）时自动算」）。默认 false 意味着完工自动算差异路径**非默认活跃**，告警通道仅在 config=true 时被触发。

- **既有证据（复用输入）**：
  - A1.9 §5.4（P1-MA4-007 resolved R1.16 HEAD 复核：`dispatchVarianceFailureAlert` + `IErpSysNotificationBiz.notify` 派发闭环落地）
  - A1.11 §5（同 P1-MA4-007 复核 + R2.11 TestErpMfgVarianceAlert 3 场景强断言）
  - `TestErpMfgVarianceRecomputeReversal`（手动重算 reverseIfExists 红冲 + 重算语义强测）

- **剩余差距**：(a) `IErpSysNotificationBiz.notify` 的**运行时投递语义**（同步落 ErpSysNotification 持久化 vs best-effort 降级吞异常）未做运行时 census；(b) notify 投递失败时是否有兜底（retry/工作台/告警）未确认；(c) 运营响应闭环——手动重算入口 `calculateVariances` 是否被运营实际使用（xbiz 注册可达性 + 文档/运维流程是否引导）未确认；(d) LOG.error/告警是否被监控采集未确认。本验证闭合 P1-MA4-007 的运行时可见性裁决。

- **保护区域**：只读评估（grep notify 实现投递语义 + dispatchVarianceFailureAlert 调用链 + 手动重算入口 xbiz 注册 + config 默认值 + 监控采集现状），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若登记 residual observability gap，归 MR1 R1.0→RC-R1.n；修复触及 notify 通道可靠性属 BizModel 代码逻辑预授权不触 ask-first，但若触及会计过账核心路径须 ask-first）。

## Goals

- **notify 投递语义 census**（SP-1 核心）：核验 `IErpSysNotificationBiz.notify`（`ErpSysNotificationBizModel:54` → `ErpSysNotificationNotifyProcessor:40`）的实际投递语义——是否同步持久化 ErpSysNotification 行（运营经工作台可见）/ 是否 best-effort 降级吞异常 / 投递失败时是否有兜底（retry/告警升级）。给出 file:line 证据。
- **dispatchVarianceFailureAlert 调用链 census**：核验 `ErpMfgWorkOrderReportCompletionProcessor:99` → `dispatchVarianceFailureAlert:150` → `notify(NOTIFY_EVENT_VARIANCE_FAILURE, ...)` 完整调用链 + 异常上下文传递（workOrder code/cause 是否进 context 供运营定位）。
- **手动重算入口可达性核验**（运营响应闭环）：核验 `ErpMfgCostVariance__calculateVariances` xbiz mutation 注册（GraphQL 可达）+ 其 reverseIfExists 红冲 + 重算语义（复用 TestErpMfgVarianceRecomputeReversal 证据）+ 运营文档/owner doc 是否引导该入口（运营响应闭环是否"可达且被引导"）。
- **config 默认值复核**：确认 `erp-mfg.variance-auto-calc-enabled` 默认 false（`ErpMfgConstants:172`）+ application.yaml 部署 override 是否存在（决定告警通道默认活跃性）。
- **监控/告警采集现状 census**：核验 `dispatchVarianceFailureAlert` 内 LOG 级别 + 是否有结构化日志/metrics/外部告警通道（运营实际感知路径）。
- **裁决**（方法论 §2 判据 + 三源对照）：①若 notify 同步持久化 ErpSysNotification + 运营经工作台可见 + 手动重算入口可达且被引导 → **闭合 P1-MA4-007 运行时可见性**，无新 finding；②若 notify best-effort 降级吞异常 + 无兜底 + 手动重算入口虽可达但无运营引导 → **维持 P1-MA4-007 resolved**（R1.16 已落地告警派发基础设施）但**登记 residual observability gap watch-only**（归 MR1，纯 BizModel/notify 通道可靠性预授权不触 ask-first）；③若 config 默认 off → 告警通道非默认活跃，运行时风险限 config=true 部署（登记 config-enable 运营注意）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.9/A1.11 §5 P1-MA4-007 resolved 裁决分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 notify 通道可靠性/告警升级**（若登记 residual observability gap 归 MR1；本计划仅登记不实施修复）。
- **不修改代码/ORM/api.xml/BizModel/Processor/真相源**（只读评估）。
- **不重新核实 UC-MFG-07/UC-MFG-12 全部验收标准**（A1.9/A1.11 §5 已判接受；本验证只评 P1-MA4-007 运行时可见性差异）。
- **不重审 P1-MA4-007 的 resolved 裁决本身**（R1.16 已 done；本验证只评运行时可见性是否真正达成，不撤销 resolved）。
- **不展开 A1.9 SP-2**（返工工单工作流，归 A4.2.5 独立工作项）。
- **不展开 A1.9 SP-3 / A1.11 SP-3**（预留实现后一致性 = MR1 修复落地后 successor，Deps 不满足，归 A4.2.3；SP-3 看板行级权限归 A4.2.10 done）。
- **不展开 A1.11 SP-2/SP-4**（基因链写失败归 A4.2.9 独立工作项；召回报告 degraded 归 A4.2.11 独立工作项）。
- **不实际注入永久性失败重现投递**（只读 notify 投递语义 census + 调用链推理 + config/监控普查；真实失败注入重现属 MR1 修复验证范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（完工差异过账失败告警通道 notify 投递 + 运营响应闭环运行时确认 + P1-MA4-007 resolved 运行时可见性裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.4 行）+ `docs/audits/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md` §7 SP-1 + §5.4 P1-MA4-007 复核（输入）+ `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-1 + §5（输入）+ `docs/design/manufacturing/`（完工差异过账 owner doc）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。告警通道投递可靠性评估需多维度归类（notify 投递语义 / dispatchVarianceFailureAlert 调用链 / 手动重算入口可达性 / config 默认值 / 监控采集现状 / P1-MA4-007 resolved 运行时可见性裁决 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep notify 投递实现 + dispatchVarianceFailureAlert 调用链 + 手动重算入口 xbiz 注册 + config 默认值 + 监控采集现状）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - notify 投递语义 census + dispatchVarianceFailureAlert 调用链 + 手动重算入口可达性

Status: completed
Targets: `docs/audits/2026-08-06-2025-rc-ma4-a4-2-4-mfg-variance-posting-failure-notify-runtime.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（Phase 1 全 Proof）
- Prereqs: A4.2 done（展开器已追加 A4.2.4 行）；A1.9 + A1.11 done（§7 SP-1 已落盘 + §5 P1-MA4-007 resolved 复核已登记）

- [x] `Proof` notify 投递语义 census：核验 `ErpSysNotificationBizModel.notify:54` → `ErpSysNotificationNotifyProcessor.notify:40` 实际投递语义（同步持久化 ErpSysNotification 行 / best-effort 降级吞异常 / 投递失败兜底[retry/告警升级]）。给出 file:line 证据 + 异常处理路径。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` dispatchVarianceFailureAlert 调用链 census：核验 `ErpMfgWorkOrderReportCompletionProcessor:99` → `ErpMfgWorkOrderProcessor.dispatchVarianceFailureAlert:150` → `notify(NOTIFY_EVENT_VARIANCE_FAILURE, context)` 完整调用链 + context 字段（workOrder code/cause/errorMsg 是否进 context 供运营定位）+ 异常分级（G3 错误分级 R1.16 落地确认）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 手动重算入口可达性核验（运营响应闭环）：核验 `ErpMfgCostVariance__calculateVariances` xbiz mutation 注册（`@BizMutation` GraphQL 可达）+ reverseIfExists 红冲 + 重算语义（复用 TestErpMfgVarianceRecomputeReversal 证据）+ owner doc/运维文档是否引导该入口。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` config `erp-mfg.variance-auto-calc-enabled` 默认值复核：确认默认 false（`ErpMfgConstants:172`）+ 全 application.yaml 部署 override 普查（决定告警通道默认活跃性）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 监控/告警采集现状 census：核验 dispatchVarianceFailureAlert 内 LOG 级别（LOG.warn/error）+ 是否有结构化日志/metrics/外部告警通道（运营实际感知路径）+ ErpSysNotification 工作台查询入口。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（差异过账失败告警通道运行时可见性），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] notify 投递语义 census 有明确结论（同步持久化 / best-effort 降级 / 兜底），每条有证据（file:line）
- [x] dispatchVarianceFailureAlert 调用链 + 手动重算入口可达性核验有明确结论，每条有证据（file:line）

### Phase 2 - 运行时可见性裁决 + finding 衔接 + §8 自检

Status: completed
Targets: `docs/audits/2026-08-06-2025-rc-ma4-a4-2-4-mfg-variance-posting-failure-notify-runtime.md`（定稿）；`docs/audits/arm-index.md`（P1-MA4-007 注记更新或 residual gap 登记，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 notify 投递语义 census + 调用链 + 手动重算入口可达性核验完成

- [x] `Decision` P1-MA4-007 resolved 运行时可见性裁决（方法论 §2 判据 + 三源对照）：①notify 同步持久化 + 工作台可见 + 重算入口可达且被引导 → 闭合运行时可见性，无新 finding；②notify best-effort 降级吞异常 + 无兜底 + 重算入口可达但无引导 → 维持 resolved + 登记 residual observability gap watch-only（归 MR1）；③config 默认 off → 运行时风险限 config=true 部署，登记 config-enable 运营注意。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.9/A1.11 §5 P1-MA4-007 resolved 分层一致。
      - Skill: none
- [x] `Add` finding/注记更新：若登记 residual observability gap → arm-index P1-MA4-007 行追加运行时可见性确认注记 + 触发 MR1 R1.0 展开器读取（本计划记录「归 MR1」）；若闭合 → arm-index P1-MA4-007 行追加运行时可见性闭合注记。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P1-MA4-007 / A1.9 §5/§7 / A1.11 §5/§7 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（notify 投递语义 census + 调用链 + 手动重算入口可达性 + 运行时可见性裁决 + finding 衔接 + §8 自检齐全）
- [x] P1-MA4-007 注记更新或 residual gap 登记 + 若归 MR1 已记录

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_028f1f893ffeWOeuLL6rcPXg3A，fresh session，未起草本计划）— 全 10 checklist 项 PASS（A 格式完整 / B Deps 满足[A4.2 展开器 completed + R1.16 landed arm-index P1-MA4-007 resolved] / C 规则14 合并成立[A1.11 §7:311 逐字「与 A1.9 SP-1 同根因」+ 同 finding P1-MA4-007 + 同 notify 通道] / D 单一结果表面 / E baseline 零信任核验全 VERIFIED[NOTIFY_EVENT_VARIANCE_FAILURE:89 + dispatchVarianceFailureAlert:150 + ReportCompletionProcessor:99 调用 + ErpSysNotificationBizModel.notify:54 + ProductionVarianceCalculator.calculateVariances:106 + ErpMfgCostVarianceBizModel @BizMutation:48-50 + config 默认 false] / F 反松弛 / G item typing / H Skill / I 保护区域[只读，notify 通道可靠性修复归 MR1 BizModel 预授权不触 ask-first] / J 无矛盾）。零 Blocker。Non-blocking 已记录：①ErpMfgConstants 常量在 :173（计划引 :172 为注释尾，off-by-one 语义正确，Phase 1 grep 落地 :173）；②ErpSysNotificationNotifyProcessor:40 行号 Phase 1 census 精化。共识达成，转 active。

## Closure Gates

> 本计划为**只读告警通道可见性评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = notify 投递语义 census + 调用链 + 手动重算入口可达性 + 运行时可见性裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.2.4 验证报告 notify 投递语义 census + 调用链 + 手动重算入口可达性 + 运行时可见性裁决齐全 + finding/注记更新
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.9/A1.11 §7 SP-1 + §5 P1-MA4-007 resolved 裁决一致
- [x] 已运行验证：notify 投递语义 census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### notify 通道可靠性/告警升级修复（若登记 residual observability gap）

- Classification: `out-of-scope improvement`（本验证是可见性评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是可见性评估，结果表面 = 验证报告 + finding/注记登记。修复归 MR1（R1.0→RC-R1.n），修复触及 notify 通道可靠性属 BizModel 代码逻辑预授权可自动执行不触 §5 ask-first；若触及会计过账核心路径须 ask-first。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取本报告 finding → RC-R1.n 修复，按报告裁决方向：①闭合则无需；②best-effort 降级无兜底→增强 notify 投递可靠性 + 运营引导）

## Closure

Status Note: completed — 全部 Phase 执行完毕（只读验证，无代码/ORM/api.xml/真相源变更）。裁决：维持 P1-MA4-007 resolved + 登记 residual observability gap watch-only（归 MR1）+ config-enable 运营注意，与 A1.9 §5.4 / A1.11 §5.4 P1-MA4-007 resolved 裁决分层一致。R1.16 告警派发基础设施层运行时可见性已达成（G3 分级 + dispatchVarianceFailureAlert 完整调用链 + notify 成功路径同步持久化 + 工作台可见 + 手动重算入口可达且被引导）；residual gap = notify best-effort 降级无兜底 + FAILURE 通道零运行时测试覆盖（仅 THRESHOLD 通道强测，不同 event）+ 监控仅 LOG/工作台无 metrics。结束审计由独立子代理执行（见 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_028e5ad74ffesnPFBesV5J4gnS（general subagent，fresh context，未执行本计划）。Verdict = **passes closure audit**。核查：Phase 1 census 全 claim 经 live source 复核 PASS（notify 链 @BizMutation + 同步持久化 + best-effort catch→LOG.error→emptyList + 无 retry/fallback；dispatch 链 G3 分级 + dispatchVarianceFailureAlert:150-167 + NOTIFY_EVENT_VARIANCE_FAILURE:89；手动重算 @BizMutation + COMPLETED 守卫 + 幂等闭环；config 默认 false + 无生产 application.yaml override；FAILURE 通道零运行时测试覆盖 + TestErpMfgVarianceAlert 强测的是 THRESHOLD 通道不同 event）；Phase 2 verdict coherent（§2 判据 + L1/L2/L3 三源 + 与 A1.9/A1.11 §5.4 分层一致 + 不重开 P1-MA4-007 §去重协议）；plan/report 一致性 PASS（全 [x] + Status completed + §6 9 段骨架 + roadmap done ✅ + arm-index 注记）；anti-hollow PASS（file:line 证据真实 + residual gap 真实[best-effort 吞 + FAILURE 通道零测] + 不撤销 resolved + git 确认零 .java/.orm.xml/.api.xml 变更）。零 Blocker。

Follow-up:

- MR1 修复 notify 通道可靠性/告警升级（若登记 residual observability gap）
