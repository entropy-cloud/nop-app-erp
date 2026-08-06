# 2026-08-06-2247-1 rc-ma4-a4-2-11-mfg-recall-report-degraded-business-coverage 召回报告 degraded 模式运行时业务覆盖确认（A1.11 SP-4）

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.11（A1.11 SP-4：MA4 运行时行为验证 — 召回报告 degraded 模式运行时业务覆盖，`ErpMfgBatchGenealogyBizModel.recallReport` degraded=true 仅返回受影响成品批次集合，实际召回场景下"受影响成品批次集合"是否满足运营召回需求 + 位置/去向查询缺失的业务影响）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.11；存疑点来源 `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-4
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.11 实体行）、`docs/plans/2026-08-06-2025-3-rc-ma4-a4-2-9-mfg-batch-genealogy-write-failure-observability.md`（范式参照 + 同源 slice A1.11，SP-2 可观测性维度 done；本计划评 SP-4 业务覆盖维度，与 SP-2 不同控制点不重复）、`docs/audits/arm-index.md`（P1-RC-010 finding 行）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-2247-rc-ma4-a4-2-11-mfg-recall-report-degraded-business-coverage.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：recallReport 实现路径 census + backwardTrace/forwardTrace 受影响成品批次集合完整性 + degraded 标记语义 + 位置/去向查询缺失的业务影响 + inventory 域是否已暴露按批次位置/去向查询方法集）。范式对齐 A4.2.9（done — 同源 slice A1.11 best-effort 路径运行时探针先例，不同维度）。

- **存疑点原文**（A1.11 报告 §7 SP-4，`2026-08-02-2245-...-a1-11-...md` §7:314）：
  - 「**召回报告 degraded 模式运行时业务覆盖**：RecallReport.degraded=true 仅返回受影响成品批次集合，实际召回场景下"受影响成品批次集合"是否满足运营召回需求（位置/去向查询缺失的业务影响）需运行时确认。」触发条件：实际召回事件触发 + inventory 域暴露按批次的位置/去向查询方法集时（successor 触发条件）。

- **关联既有 finding**：
  - **P1-RC-010**（arm-index）：UC-MFG-13 ⑫召回报告测试仅冒烟（testRecallReport 仅断言 status=0+data 非空，未断言 affectedLots 含 lotB/lotC 内容/degraded=true/sourceLotId）+ best-effort 写失败路径无测试。**功能本身实现正确**（recalledReport 反向递归 + collectAffectedIfFinishedGood 识别产出成品批次），仅测试断言强度不足。A4.2.9（done）已确认 best-effort 写失败路径的**运行时可观测性**维度（catch 分支仅 LOG.error 无监控采集 → 维持 P1-RC-010 + 登记 residual observability gap）。**本验证确认 P1-RC-010 的业务覆盖维度**——recallReport degraded=true 返回的受影响成品批次集合是否完整满足 L1"识别所有受影响成品批次"需求 + 位置/去向缺失是 successor 还是缺口。与 A4.2.9 不同维度（可观测性 vs 业务覆盖），不重复。

- **需求契约（L1 权威）**：`docs/design/manufacturing/use-cases.md` UC-MFG-13 批次追溯与召回 ⑫「召回报告：从问题批次出发识别所有受影响成品批次」。L1 仅要求"识别"（identify），未要求位置/去向（position/whereabouts）。A1.12 切片 §5 裁决：UC-MFG-13 召回报告降级满足 L1"识别"，位置/去向为增强 successor 归 inventory 域演进。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **recallReport 入口**（已 live 核实）：`ErpMfgBatchGenealogyBizModel.recallReport:72`（`@BizQuery`，参数 lotId）。
  - **degraded 标记 + 注释**（已 live 核实）：`:76-78` 注释「降级标记：当前 inventory 域未暴露按批次的库存位置/已售去向查询方法集，仅返回受影响成品批次集合（位置/去向归 inventory successor）」+ `report.setDegraded(true)` 结构性恒置。
  - **反向递归 + 受影响成品批次识别**（已 live 核实）：`recallReport:84` `collectAffectedIfFinishedGood(lotId, report)` + `:92-99` BFS 反向递归 `batchGenealogyTracer.backwardTrace(currentLot)` + 对每条 outputLot 调 `collectAffectedIfFinishedGood:109`（仅当 lot 为成品[finished good]时加入 affectedLots 集合）。
  - **forwardTrace / backwardTrace**（已 live 核实）：`ErpMfgBatchGenealogyBizModel.forwardTrace:46-49` + `backwardTrace:54-57`，委托 `batchGenealogyTracer`（多级递归实现）。
  - **inventory 域位置/去向查询**（已 live 核实）：grep `position|whereabouts|locationOf|whereIs` 全 module-inventory/erp-inv-service 零命中 → inventory 域当前**未暴露**按批次的位置/已售去向查询方法集，degraded 标记语义如实反映现状。

- **既有证据（复用输入）**：
  - A1.11 §5（UC-MFG-13 接受 on 功能 ⑧⑨⑩⑪⑫，⑫测试有效性 P1-RC-010；降级满足 L1"识别"，位置/去向为增强 successor）
  - A1.11 §7 SP-4（静态存疑点）
  - A4.2.9（done — best-effort 写失败可观测性维度，与 SP-4 业务覆盖维度不同控制点）

- **剩余差距**：(a) recallReport 反向递归（backwardTrace BFS + collectAffectedIfFinishedGood）返回的受影响成品批次集合是否**完整覆盖**所有可达产出成品批次（多级递归 + 多分支链下是否漏召回）未做运行时 census；(b) degraded=true 恒置是否如实反映"位置/去向缺失"现状（vs 掩盖其他缺口）未确认；(c) 位置/去向缺失对运营召回需求的实际业务影响（L1 仅要求"识别"，位置/去向为 successor 是否成立）未做运行时裁决。本验证闭合 P1-RC-010 业务覆盖维度裁决。

- **保护区域**：只读评估（grep recallReport/collectAffectedIfFinishedGood/backwardTrace + degraded 标记语义 + inventory 域位置/去向查询方法集 census），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若登记位置/去向 successor 归 inventory 域演进；修复增强 recallReport 位置/去向属跨域 inventory Facade 扩展，纯 BizModel 读侧 Facade 预授权不触 ask-first）。

## Goals

- **recallReport 反向递归完整性 census**（SP-4 核心）：核验 `ErpMfgBatchGenealogyBizModel.recallReport`（`:72-107`）反向递归（backwardTrace BFS + collectAffectedIfFinishedGood）返回的受影响成品批次集合是否**完整覆盖**所有可达产出成品批次——多级递归（问题批次→中间半成品→产出成品）+ 多分支链（一个 inputLot 经多道工序产出多个 outputLot）下是否漏召回。给出 file:line 证据（backwardTrace BFS 实现 + collectAffectedIfFinishedGood finished-good 判定 + visited 集合防环）。
- **degraded 标记语义核验**：核验 `report.setDegraded(true)`（`:78`）结构性恒置是否如实反映"位置/去向缺失"现状——是否掩盖其他潜在缺口（如反向递归不完整）。
- **位置/去向缺失业务影响裁决**（SP-4 核心）：核验位置/去向查询缺失对运营召回需求的实际业务影响——L1 UC-MFG-13 ⑫仅要求"识别"，recallReport 降级版（返回受影响成品批次集合 + 无位置/去向）是否满足 L1"识别"需求；位置/去向为增强 successor 归 inventory 域演进是否成立（inventory 域是否已暴露按批次位置/去向查询方法集）。
- **裁决**（方法论 §2 判据 + 三源对照）：①受影响成品批次集合完整（backwardTrace BFS + visited 防环覆盖所有可达产出成品）+ degraded 如实反映位置/去向缺失 + L1 仅要求"识别" → **维持 P1-RC-010**（测试补充义务）+ **业务覆盖维度接受**（位置/去向 = inventory 域 successor，登记 successor watch-only）；②反向递归不完整（漏召回分支）→ 登记 finding（按 §2 判据分级，可能升 P1/P0[若致活跃召回漏报]）；③degraded 恒置掩盖其他缺口 → 登记 finding。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.11 §5 裁决分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 recallReport 位置/去向查询**（归 inventory 域演进 successor，本计划仅登记不实施修复）。
- **不修改代码/ORM/api.xml/BizModel/Processor/真相源**（只读评估）。
- **不重新核实 UC-MFG-13 全部验收标准**（A1.11 §5 已判接受 on 功能 ⑧⑨⑩⑪⑫；本验证只评召回报告业务覆盖维度差异）。
- **不重审 P1-RC-010 的 P1 定级本身**（A1.11 §5 已裁决；本验证只评业务覆盖现状，不撤销/不降级 P1-RC-010 测试补充义务）。
- **不展开 A1.11 SP-1**（完工差异过账失败告警通道，归 A4.2.4 done）。
- **不展开 A1.11 SP-2**（best-effort 写失败可观测性，归 A4.2.9 done）。
- **不展开 A1.11 SP-3**（看板行级权限，归 A4.2.10 done）。
- **不实施 P1-RC-010 测试补充**（testRecallReport 强化断言属 MR1 修复，纯测试补充预授权，归 R1.0 展开器）。
- **不实际触发召回事件重现**（只读 recallReport/backwardTrace/collectAffectedIfFinishedGood census + 完整性推理 + inventory 域方法集普查；真实召回事件重现属运营范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（召回报告 degraded 模式运行时业务覆盖确认 + P1-RC-010 业务覆盖维度裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.11 行）+ `docs/audits/2026-08-02-2245-rc-ma1-a1-11-mfg-f4-variance-batch-kanban.md` §7 SP-4 + §5 P1-RC-010 裁决（输入）+ `docs/design/manufacturing/`（批次追溯/召回 owner doc）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。召回报告业务覆盖评估需多维度归类（反向递归完整性 / degraded 标记语义 / 位置/去向缺失业务影响 / inventory 域 successor 触发条件 / P1-RC-010 协同 / A4.2.9 维度边界 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep recallReport/collectAffectedIfFinishedGood/backwardTrace + degraded 标记语义 + inventory 域位置/去向查询方法集 census）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - recallReport 反向递归完整性 census + degraded 标记语义 + inventory 域位置/去向方法集

Status: completed
Targets: `docs/audits/2026-08-06-2247-rc-ma4-a4-2-11-mfg-recall-report-degraded-business-coverage.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（Phase 1 全 Proof）
- Prereqs: A4.2 done（展开器已追加 A4.2.11 行）；A1.11 done（§7 SP-4 已落盘 + §5 P1-RC-010 裁决已登记）

- [x] `Proof` recallReport 反向递归完整性 census：核验 `ErpMfgBatchGenealogyBizModel.recallReport`（`:72-107`）反向递归（backwardTrace BFS + collectAffectedIfFinishedGood）返回的受影响成品批次集合是否完整覆盖所有可达产出成品批次——多级递归（inputLot→中间半成品→产出成品）+ 多分支链（一个 inputLot 经多道工序产出多个 outputLot）下是否漏召回。给出 file:line 证据（backwardTrace BFS 实现 + visited 集合防环 + collectAffectedIfFinishedGood finished-good 判定）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` degraded 标记语义核验：核验 `report.setDegraded(true)`（`:78`）结构性恒置是否如实反映"位置/去向缺失"现状——是否掩盖其他潜在缺口（如反向递归不完整 / 多级递归 max-depth 截断）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` inventory 域位置/去向查询方法集 census：核验 inventory 域（module-inventory/erp-inv-service）是否已暴露按批次的库存位置/已售去向查询方法集（grep position/whereabouts/locationOf/whereIs/batchLocation）。确认 degraded 标记的 successor 触发条件（inventory 域暴露方法集时）当前是否已满足。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` P1-RC-010 测试补充 + A4.2.9 可观测性协同声明：核验 P1-RC-010（测试断言强度）/ A4.2.9（best-effort 写失败可观测性）/ 本验证（业务覆盖）三者不同维度不同控制点互补不重复。P1-RC-010 测试补充属 MR1 修复（纯测试预授权），A4.2.9 评可观测性（done），本验证评业务覆盖。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（召回报告业务覆盖），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] recallReport 反向递归完整性 census 有明确结论（完整覆盖 / 漏召回分支），每条有证据（file:line）
- [x] degraded 标记语义 + inventory 域位置/去向方法集 census 有明确结论，每条有证据（file:line）

### Phase 2 - 业务覆盖裁决 + finding 衔接 + §8 自检

Status: completed
Targets: `docs/audits/2026-08-06-2247-rc-ma4-a4-2-11-mfg-recall-report-degraded-business-coverage.md`（定稿）；`docs/audits/arm-index.md`（P1-RC-010 注记更新或新 finding 登记，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 recallReport 反向递归完整性 census + degraded 标记语义 + inventory 域方法集 census 完成

- [x] `Decision` P1-RC-010 业务覆盖维度裁决（方法论 §2 判据 + 三源对照）：①受影响成品批次集合完整（backwardTrace BFS + visited 防环覆盖所有可达产出成品）+ degraded 如实反映位置/去向缺失 + L1 仅要求"识别" → **维持 P1-RC-010**（测试补充义务）+ **业务覆盖维度接受**（位置/去向 = inventory 域 successor，登记 successor watch-only）；②反向递归不完整（漏召回分支）→ 登记 finding（按 §2 判据分级，若致活跃召回漏报可能升 P1/P0）；③degraded 恒置掩盖其他缺口 → 登记 finding。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.11 §5 P1-RC-010 裁决分层一致。
      - Skill: none
- [x] `Add` finding/注记更新：若业务覆盖维度接受 → arm-index P1-RC-010 行追加运行时业务覆盖确认注记（位置/去向 = inventory successor watch-only）；若登记新 finding（漏召回 / degraded 掩盖）→ arm-index 新建 finding 行 + 触发 MR1 R1.0 展开器读取（本计划记录「归 MR1」或「触发 MR0[若升 P0]」）。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 P1-RC-010 / A1.11 §5/§7 / A4.2.9 的复用关系 + 业务覆盖维度协同 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（反向递归完整性 census + degraded 标记语义 + inventory 域方法集 + 业务覆盖裁决 + finding 衔接 + §8 自检齐全）
- [x] P1-RC-010 注记更新或新 finding 登记 + 若归 MR1/MR0 已记录

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（独立子代理 ses_028706024ffedVF8mBevGTrVW6，fresh session，未起草本计划）— 全 10 checklist 项 PASS（A 格式完整 / B Deps 满足[A4.2 展开器 done + A1.11 done] / C 规则14 单行非合并正确[sibling SP-1→A4.2.4/SP-2→A4.2.9/SP-3→A4.2.10 done 正确排除，A4.2.3 MR1-blocked 不属本切片] / D 单一结果表面 / E baseline 零信任核验全 VERIFIED[recallReport:72 @BizQuery + degraded:78 结构性恒置 + BFS 反向递归 :89-105 + collectAffectedIfFinishedGood:109 + forwardTrace/backwardTrace + inventory 域位置/去向方法集零命中 + P1-RC-010 arm-index:146]零 FALSIFIED / F 反松弛 / G item typing[Phase1 unified Proof 5/5] / H Skill / I 保护区域[只读，inventory Facade successor 出范围] / J 无矛盾[裁决 3 分支与 A1.11 §5 P1-RC-010 一致]）。零 Blocker。Non-blocking 已记录：①collectAffectedIfFinishedGood 行范围 :92-99 vs 实测核心循环 :89-105（methodology §1 行漂移豁免）；②inventory 位置/去向方法集 grep 含 batchLocation 零命中确认。共识达成，转 active。

## Closure Gates

> 本计划为**只读业务覆盖评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 反向递归完整性 census + degraded 标记语义 + inventory 域方法集 + 业务覆盖裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.2.11 验证报告反向递归完整性 census + degraded 标记语义 + inventory 域方法集 + 业务覆盖裁决齐全 + finding/注记更新
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.11 §7 SP-4 + §5 P1-RC-010 裁决一致
- [x] 已运行验证：反向递归完整性 census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（登记的 successor/finding 是验证**输出**，非范围内项目降级；Deferred But Adjudicated 正确分类）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status / Phase Status / Exit Criteria / Closure Gates / 日志条目都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### recallReport 位置/去向查询（inventory 域演进 successor）+ P1-RC-010 测试补充

- Classification: `out-of-scope improvement`（本验证是业务覆盖评估，修复归 inventory 域演进/MR1）
- Why Not Blocking Closure: 本计划是业务覆盖评估，结果表面 = 验证报告 + finding/注记登记。位置/去向查询归 inventory 域演进 successor（degraded 标记 successor 触发条件 = inventory 域暴露按批次位置/去向方法集）；P1-RC-010 测试补充归 MR1（R1.0→RC-R1.n，纯测试预授权）。本验证闭环不阻塞于修复落地（finding/successor 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（inventory 域暴露位置/去向方法集时 recallReport 增强；MR1 R1.0 展开器读取本报告 P1-RC-010 → RC-R1.n 测试补充）

## Closure

Status Note: 已完成。两 Phase 全 done：Phase 1 反向递归完整性 census（BFS backwardTrace + visited 防环 + 无 maxDepth 截断 → 完整覆盖无漏召回）+ degraded 标记语义（结构性恒置如实反映位置/去向缺失，不掩盖递归不完整）+ inventory 域位置/去向方法集 census（零暴露，successor 未触发）+ P1-RC-010/A4.2.9 协同 + MA4↔A5.6 边界；Phase 2 业务覆盖裁决命中分支①（维持 P1-RC-010 测试补充义务 + 业务覆盖维度接受，位置/去向 = inventory successor watch-only 不新建 finding）+ arm-index P1-RC-010 追加业务覆盖确认注记 + §8 checker actual=baseline（零代码变更无回归风险）。验证报告落盘 `docs/audits/2026-08-06-2247-rc-ma4-a4-2-11-mfg-recall-report-degraded-business-coverage.md`。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_02864de63ffe1XmlZqpGodR204（fresh session，未执行本计划，未参与草案审查）
- Evidence: 独立结束审计 verdict = `passes closure audit`（全 10 checklist 项 PASS）。核心项独立复核：①recallReport 反向递归完整性——独立读 `ErpMfgBatchGenealogyBizModel:89` while(!frontier.isEmpty()) 无 maxDepth + `BatchGenealogyTracer:56` eq("inputLotId") 返回全集（多分支）+ `:98` visited.add 防环 + 对比 traceChain:89-92 有 maxDepth 抛错 → recallReport 确无截断，非空壳断言；②inventory 域 census 独立 grep 零命中；③degraded 语义、裁决分支①一致性、arm-index 注记、roadmap done ✅、§8 过程纪律、范围纪律全 PASS。零 Blocker，3 项非阻塞 residual（测试维度交叉引用属正确边界/命名精度属 MR1 注记/行号属 §1 漂移豁免）。

Follow-up:

- inventory 域演进暴露按批次位置/去向查询方法集时增强 recallReport（跨域 Facade 扩展，纯读侧预授权不触 ask-first）
- MR1 修复 P1-RC-010 测试补充（testRecallReport 强化断言，纯测试预授权）
