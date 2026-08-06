# 2026-08-06-2025-3 rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability best-effort 基因链写失败运行时缺口可观测性确认（A1.11 SP-2，P1-RC-010 协同）

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.9（A1.11 SP-2：MA4 运行时行为验证 — best-effort 基因链写失败运行时缺口可观测性，`BatchGenealogyWriter.writeOnCompletion` try/catch 不阻断完工的运行时可观测性 + 基因链缺口业务影响；与 P1-RC-010[UC-MFG-13 ⑫召回报告测试仅冒烟 + best-effort 写失败路径无测试]测试补充协同）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.9；存疑点来源 `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-2
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.9 实体行）、`docs/audits/arm-index.md`（P1-RC-010 finding 行）、`docs/plans/2026-08-06-1926-2-rc-ma4-a4-2-6-7-8-mfg-bom-edit-impact-runtime.md`（范式参照：mfg 完工 best-effort 路径运行时探针 + config-gated 裁决）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-2025-rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：BatchGenealogyWriter catch 分支 census + LOG 监控采集现状 + 基因链缺口业务影响 + config-gated 行为）。范式对齐 A4.2.6-8（done — mfg 完工 best-effort 路径运行时探针先例）。

- **存疑点原文**（A1.11 §7 SP-2，`2026-08-02-2245-...-a1-11-...md` §7:312）：
  - 「**best-effort 基因链写失败运行时缺口可观测性**：Decision 3 try/catch 不阻断完工，但实际运营中 `BatchGenealogyWriter.writeOnCompletion:71-75` catch 分支被触发频率 + LOG.error 是否被监控采集 + 基因链缺口的业务影响（部分完工无追溯行）需运行时确认。」触发条件：领料单带批次 + 完工入库 + 写入异常（如 ErpInvBatch 锁冲突/数据不一致）。与 P1-RC-010 测试补充协同。

- **关联既有 finding**：
  - **P1-RC-010**（arm-index）：UC-MFG-13 ⑫召回报告测试仅冒烟——testRecallReport 只断言 status=0+data 非空，未断言 affectedLots 含 lotB/lotC 内容/degraded=true/sourceLotId；验收标准⑫"识别受影响成品批次"零内容断言 + best-effort 写失败路径[BatchGenealogyWriter.writeOnCompletion:71-75 catch]无测试；功能本身实现正确仅测试断言强度不足。**本验证确认 P1-RC-010 best-effort 写失败路径的运行时可观测性现状**——catch 分支触发后的可观测性（LOG.error 采集 / 告警 / 失败标记）+ 基因链缺口业务影响。若运行时确认 catch 分支无监控采集 + 基因链缺口致召回报告降级无运营感知 → 维持 P1-RC-010（测试补充）+ 登记 residual observability gap watch-only（归 MR1）；若 catch 分支已有告警派发/失败标记 → 闭合可观测性，P1-RC-010 维持测试补充义务。

- **需求契约（L1 权威）**：`docs/design/manufacturing/use-cases.md` UC-MFG-13 批次追溯与召回（⑧完工记录 input→output / ⑨前向 / ⑩反向 / ⑪多级递归 / ⑫召回报告识别受影响成品批次）。L1 要求完工建立批次基因链供召回追溯；best-effort 语义为 owner doc 显式设计（写失败不阻断完工），但缺口须可观测。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **best-effort 写入链**（已 live 核实）：
    - `ErpMfgWorkOrderProcessor.java:326` `batchGenealogyWriter.writeOnCompletion(wo, completedQty, context)`
    - `ErpMfgWorkOrderReportCompletionProcessor.java:67` 注释「best-effort（BatchGenealogyWriter 内部 try/catch，不阻断完工入库）；config-gated `erp-mfg.genealogy-write-enabled`」
    - `BatchGenealogyWriter.java:64` `public void writeOnCompletion(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context)`
    - `BatchGenealogyWriter.java:51` `private static final Logger LOG = LoggerFactory.getLogger(BatchGenealogyWriter.class)`
  - **catch 分支**（已 live 核实，`BatchGenealogyWriter.java` 约 :74-76）：`} catch (Exception e) { LOG.error("工单 {} 完工写入批次基因链失败（best-effort，不阻断完工入库）", wo.getCode(), e); }`——**仅 LOG.error，无 notify 告警派发 + 无失败标记持久化**（待 Phase 1 census 确认无其他可观测通道）。
  - **config**：`erp-mfg.genealogy-write-enabled`（待 Phase 1 census 默认值 + 部署 override，决定 best-effort 写入路径默认活跃性）。
  - **召回报告**（P1-RC-010 协同）：RecallReport.degraded=true 仅返回受影响成品批次集合（位置/去向查询缺失，归 inventory 域能力演进 successor）。

- **既有证据（复用输入）**：
  - A1.11 §4（UC-MFG-13 接受 on 功能 ⑧⑨⑩⑪⑫，⑫测试有效性 P1-RC-010）
  - A1.11 §5（P1-RC-010 新建：best-effort 写失败路径无测试 + 召回报告测试仅冒烟）
  - A1.11 §6.1（P1-RC-010 与 P1-MA4-009/011 不同控制点）

- **剩余差距**：(a) `BatchGenealogyWriter` catch 分支的**可观测性现状**（LOG.error 是否被监控采集 / 是否有 notify 告警派发 / 是否有失败标记持久化供运营感知）未做运行时 census；(b) 基因链缺口（部分完工无追溯行）对 UC-MFG-13 召回报告的业务影响（降级无运营感知）未确认；(c) config `erp-mfg.genealogy-write-enabled` 默认值未确认。本验证闭合 P1-RC-010 best-effort 写失败路径的运行时可观测性裁决。

- **保护区域**：只读评估（grep BatchGenealogyWriter catch 分支 + LOG/notify/失败标记 census + 监控采集现状 + config 默认值 + 召回报告业务影响），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若登记 residual observability gap 归 MR1；修复增强 catch 分支可观测性[加 notify 告警/失败标记]属 BizModel 代码逻辑预授权不触 ask-first）。

## Goals

- **catch 分支可观测性 census**（SP-2 核心）：核验 `BatchGenealogyWriter.writeOnCompletion` catch 分支（`BatchGenealogyWriter.java` 约 :74-76）的可观测性现状——LOG.error 级别确认 + 是否有 notify 告警派发（`IErpSysNotificationBiz.notify`）/ 是否有失败标记持久化（基因链缺口标记字段）/ 是否有其他可观测通道。给出 file:line 证据，判定可观测性"仅日志/日志+告警/日志+告警+失败标记"。
- **LOG.error 监控采集现状 census**：核验 `LOG.error("工单 {} 完工写入批次基因链失败...")` 是否被监控采集——是否有结构化日志/metrics/日志告警通道（运营实际感知路径）+ 与 A4.2.4 notify 通道家族对比（完工 best-effort 失败可观测性家族）。
- **基因链缺口业务影响 census**（SP-2 核心）：核验部分完工无追溯行（基因链缺口）对 UC-MFG-13 召回报告的业务影响——RecallReport.degraded=true 降级 + 运营感知现状（降级是否被运营感知 / 召回场景下缺口致漏报受影响成品批次的合规风险）。
- **config 默认值复核**：核验 `erp-mfg.genealogy-write-enabled` 默认值（决定 best-effort 写入路径默认活跃性）+ 全 application.yaml 部署 override 普查。
- **裁决**（方法论 §2 判据 + 三源对照）：①catch 分支仅 LOG.error + 无监控采集 + 基因链缺口致召回报告降级无运营感知 → **维持 P1-RC-010**（测试补充义务）+ **登记 residual observability gap watch-only**（归 MR1，增强 catch 分支可观测性[notify 告警/失败标记]纯 BizModel 预授权不触 ask-first）；②catch 分支已有告警派发/失败标记 → 闭合可观测性，P1-RC-010 维持测试补充义务；③config 默认 off → best-effort 写入路径非默认活跃，运行时风险限 config=true 部署（登记 config-enable 运营注意）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.11 §5 P1-RC-010 裁决分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 catch 分支可观测性**（增强 notify 告警/失败标记，归 MR1；本计划仅登记不实施修复）。
- **不修改代码/ORM/api.xml/BizModel/Processor/真相源**（只读评估）。
- **不重新核实 UC-MFG-13 全部验收标准**（A1.11 §5 已判接受 on 功能 + ⑫测试 P1-RC-010；本验证只评 best-effort 写失败路径运行时可观测性差异）。
- **不重审 P1-RC-010 的 P1 定级本身**（A1.11 §5 已裁决；本验证只评可观测性现状，不撤销/不降级 P1-RC-010 测试补充义务）。
- **不展开 A1.11 SP-1**（完工差异过账失败告警通道，归 A4.2.4 独立工作项）。
- **不展开 A1.11 SP-3**（看板行级权限，归 A4.2.10 done）。
- **不展开 A1.11 SP-4**（召回报告 degraded 模式业务覆盖，归 A4.2.11 独立工作项；本验证只评 best-effort 写失败可观测性，召回报告业务覆盖属独立维度）。
- **不实际注入基因链写入失败重现**（只读 catch 分支 census + 可观测性推理 + config/监控普查；真实失败注入重现属 MR1 修复验证范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（best-effort 基因链写失败运行时缺口可观测性确认 + P1-RC-010 best-effort 写失败路径运行时可观测性裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.9 行）+ `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-2 + §5 P1-RC-010 裁决（输入）+ `docs/design/manufacturing/`（批次追溯/召回 owner doc）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。best-effort 写失败可观测性评估需多维度归类（catch 分支可观测性 / LOG 监控采集 / 基因链缺口业务影响 / config 默认值 / P1-RC-010 协同 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep BatchGenealogyWriter catch 分支 + LOG/notify/失败标记 census + 监控采集现状 + config 默认值）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - catch 分支可观测性 census + LOG 监控采集现状 + 基因链缺口业务影响

Status: planned
Targets: `docs/audits/2026-08-06-2025-rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（Phase 1 全 Proof）
- Prereqs: A4.2 done（展开器已追加 A4.2.9 行）；A1.11 done（§7 SP-2 已落盘 + §5 P1-RC-010 裁决已登记）

- [ ] `Proof` catch 分支可观测性 census：核验 `BatchGenealogyWriter.writeOnCompletion` catch 分支（约 :74-76）的可观测性现状——LOG.error 级别确认 + 是否有 notify 告警派发（`IErpSysNotificationBiz.notify`）/ 是否有失败标记持久化（基因链缺口标记字段）/ 是否有其他可观测通道。给出 file:line 证据，判定可观测性"仅日志/日志+告警/日志+告警+失败标记"。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` LOG.error 监控采集现状 census：核验 `LOG.error("工单 {} 完工写入批次基因链失败...")` 是否被监控采集——是否有结构化日志/metrics/日志告警通道（运营实际感知路径）+ 与 A4.2.4 notify 通道家族对比（完工 best-effort 失败可观测性家族）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 基因链缺口业务影响 census：核验部分完工无追溯行（基因链缺口）对 UC-MFG-13 召回报告的业务影响——RecallReport.degraded=true 降级 + 运营感知现状（降级是否被运营感知 / 召回场景下缺口致漏报受影响成品批次的合规风险）。给出 file:line 证据（RecallReport 构造 + degraded 标记 + affectedLots 聚合）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` config `erp-mfg.genealogy-write-enabled` 默认值复核：核验默认值（`ErpMfgConstants` / AppConfig.var 读取点）+ 全 application.yaml 部署 override 普查（决定 best-effort 写入路径默认活跃性）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` P1-RC-010 测试补充协同声明：核验 P1-RC-010（best-effort 写失败路径无测试）与本验证（运行时可观测性）的协同关系——P1-RC-010 测试补充属 MR1 修复（纯测试补充预授权），本验证评估运行时可观测性现状，二者不同控制点不同维度互补不重复。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（best-effort 写失败可观测性），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] catch 分支可观测性 census 有明确结论（仅日志/日志+告警/日志+告警+失败标记），每条有证据（file:line）
- [ ] 基因链缺口业务影响 census 有明确结论（降级运营感知 / 召回漏报合规风险），每条有证据（file:line）

### Phase 2 - 可观测性裁决 + 业务影响 + finding 衔接 + §8 自检

Status: planned
Targets: `docs/audits/2026-08-06-2025-rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability.md`（定稿）；`docs/audits/arm-index.md`（P1-RC-010 注记更新或 residual gap 登记，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 catch 分支可观测性 census + 监控采集 + 业务影响 census 完成

- [ ] `Decision` P1-RC-010 best-effort 写失败路径运行时可观测性裁决（方法论 §2 判据 + 三源对照）：①catch 仅 LOG.error + 无监控采集 + 缺口致召回报告降级无运营感知 → **维持 P1-RC-010**（测试补充义务）+ **登记 residual observability gap watch-only**（归 MR1，增强 catch 可观测性[notify 告警/失败标记]纯 BizModel 预授权不触 ask-first）；②catch 已有告警派发/失败标记 → 闭合可观测性，P1-RC-010 维持测试补充义务；③config 默认 off → 运行时风险限 config=true 部署，登记 config-enable 运营注意。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.11 §5 P1-RC-010 裁决分层一致。
      - Skill: none
- [ ] `Add` finding/注记更新：若登记 residual observability gap → arm-index P1-RC-010 行追加运行时可观测性确认注记 + 触发 MR1 R1.0 展开器读取（本计划记录「归 MR1」）；若闭合 → arm-index P1-RC-010 行追加可观测性闭合注记（P1-RC-010 测试补充义务维持）。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P1-RC-010 / A1.11 §5/§7 的复用关系 + P1-RC-010 测试补充协同 + A4.2.4 notify 通道家族对比 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（catch 分支可观测性 census + 监控采集 + 业务影响 + 可观测性裁决 + finding 衔接 + §8 自检齐全）
- [ ] P1-RC-010 注记更新或 residual gap 登记 + 若归 MR1 已记录

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 ses_028f1a056ffe0ek87hABLZY8J6，fresh session，未起草本计划）— 全 10 checklist 项 PASS（A 格式完整 / B Deps 满足[A4.2 展开器 completed + P1-RC-010 arm-index 注册 + A1.11 done] / C 规则14 非合并正确[A4.2.9 单行 A1.11 SP-2，sibling SP[SP-1→A4.2.4/SP-3→A4.2.10/SP-4→A4.2.11]不同根因正确分离] / D 单一结果表面 / E baseline 零信任核验全 VERIFIED[BatchGenealogyWriter:49/51/64 + catch :73-75「仅 LOG.error 无 notify 无失败标记」**计划前提经独立读码确认成立非证伪** + ErpMfgWorkOrderProcessor:326 调用 + ReportCompletionProcessor:67 config-gate 注释 + isWriteEnabled 默认 true Phase 1 census] / F 反松弛 / G item typing / H Skill / I 保护区域[只读，catch 可观测性增强归 MR1 BizModel 预授权不触 ask-first + P1-RC-010 测试补充纯测试预授权] / J 无矛盾[P1-RC-010 测试补充正确为独立关注点归 MR1]）。零 Blocker。Non-blocking 已记录：①catch 行范围计划引「约 :74-76」实测 :73-75（行漂移非引用失效，methodology §1）；②源 §7 引 :71-75 与计划 :74-76 同 catch 区域，可对齐非必需。共识达成，转 active。

## Closure Gates

> 本计划为**只读可观测性评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = catch 分支可观测性 census + 监控采集 + 业务影响 + 可观测性裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.2.9 验证报告 catch 分支可观测性 census + 监控采集 + 业务影响 + 可观测性裁决齐全 + finding/注记更新
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.11 §7 SP-2 + §5 P1-RC-010 裁决一致
- [ ] 已运行验证：catch 分支可观测性 census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### catch 分支可观测性增强（notify 告警/失败标记）+ P1-RC-010 测试补充

- Classification: `out-of-scope improvement`（本验证是可观测性评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是可观测性评估，结果表面 = 验证报告 + finding/注记登记。修复归 MR1（R1.0→RC-R1.n）：增强 catch 分支可观测性[加 notify 告警/失败标记]属 BizModel 代码逻辑预授权可自动执行不触 §5 ask-first；P1-RC-010 测试补充属纯测试预授权。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取本报告 finding → RC-R1.n 修复，按报告裁决方向：①闭合则仅 P1-RC-010 测试补充；②residual gap→增强 catch 可观测性 + P1-RC-010 测试补充）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填写>

Follow-up:

- MR1 修复 catch 分支可观测性增强 + P1-RC-010 测试补充（若登记 finding）
