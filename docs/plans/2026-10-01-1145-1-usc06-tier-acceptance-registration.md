# 2026-10-01-1145-1-usc06-tier-acceptance-registration 制造链与 B2B 档位验收登记

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-06（M3 档位能力；触发条款降级路径）
> Related: `docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`（USC-01 矩阵 §四分流表 #8/#9）、`docs/design/manufacturing/`、`docs/design/b2b/`、`docs/architecture/customization-capabilities.md`
> Audit: required（纯文档登记，无代码/测试变更——验证门控相应收窄）

## Current Baseline

- **触发条款已满足（降级路径成立）**：roadmap USC-05... 更正——**USC-06** 原文：「仅当 USC-01 矩阵显示 US-MF-* / US-B2-01 存在设计或测试缺口时执行实现类补洞；若矩阵为『已覆盖』，本项降级为**档位验收登记**（在矩阵中勾选档位 Must + 指向既有 E2E，如 mfg-chain / ASN 匹配 spec）」。USC-01 矩阵实测判定：US-MF-01..06 = ✅、US-B2-01 = ✅——**设计/实现/测试三维全覆盖，无实现类补洞需求**。档位口径（M1 订正）：US-MF-01..06 = 制造档 Must（完整档继承；纯商贸/轻量档不组装 manufacturing/quality 模块——customization-capabilities.md 能力五明文；roadmap :176 裁定晚于 gap analysis 的 Should 记载，以 roadmap 为准）。
- **既有 E2E/JUnit 证据锚点（矩阵各行已登记）**：制造链 mfg-chain（三聚合根协作全链）/mfg-variance（PRODUCTION_VARIANCE 凭证数值）/mfg-mrp-simulation/C08 释放集成；B2B b2b-asn-match-receive/b2b-edi-doc/line-level-receive-fill + C19 集成 + TestErpC18CtLifecycleRebate 旁证；JUnit TestErpMfgBomExplosion/MrpEngine/WorkOrderStateMachine/ProductionVariance + TestErpC19B2bAsnAutoReceiveLandedCost。
- **档位语义**：product-scope 组装档位——纯商贸（核心 5 域）/制造（核心+第一批扩展）/完整（全部 18 域）；US-MF-* = 制造档 Must、非制造客户 Won't；US-B2-01 = 完整档/大客户 Should；US-MF-05/06（质量）= 制造档 Must（M1 订正：quality 不在纯商贸档组装面，customization-capabilities 能力五为准）。
- **剩余差距**：档位 Must 验收登记未落盘（矩阵分流表 #8/#9 标注「USC-06 承接」待回写）；US-B2-01 真实 EDI 网关=集成延迟段边界登记散点。

## Goals

- 档位验收登记落盘：矩阵 §四分流表 #8/#9 行与 §三 Epic F/H 主表注记回写「USC-06 档位验收登记完成」+ 指向既有 E2E 证据锚点（mfg-chain/C08/asn-match-receive/C19）。
- owner doc 档位段登记：manufacturing 域与 b2b 域 README（或 state-machine）增档位验收段（US-MF-* 制造档 Must 证据锚点清单；US-B2-01 完整档 Should + 真实 EDI 网关延迟边界）。
- USC-01 矩阵分流表 #8/#9 处置状态从「USC-06 承接」翻转为「已登记（USC-06 完成）」。

## Non-Goals

- 实现类补洞（矩阵判已覆盖，触发条款不满足）。
- 新增 E2E/JUnit 测试（既有证据链已覆盖——USC-01 矩阵逐行登记）。
- 档位组装机制的机制变更（customization-capabilities.md 仅引用不修改）。

## Task Route

- Type: `requirement clarification`（验收登记/文档对齐，零代码零测试）
- Owner Docs: `docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`（分流表+主表注记）、`docs/design/manufacturing/README.md`、`docs/design/b2b/README.md`
- Skill Selection Basis: `Skill: none`——纯验收登记文档工作，无匹配技能；roadmap §2 Skill 列 nop-testing 按 §8.7 限实现类工作项不触发（USC-02a 先例：表列与 plan 裁决分离）。

## Infrastructure And Config Prereqs

- 无（纯文档）。

## Execution Plan

### Phase 1 — 档位验收登记落盘

Status: completed
Targets: 矩阵、manufacturing/README.md、b2b/README.md
Skill: none

- Item Types: `Add`

- [x] Add: 矩阵 §四分流表 #8/#9 行处置改「已登记（USC-06 完成，plan 2026-10-01-1145-1）」+ §三主表 US-MF-01..06 与 US-B2-01 行缺口列注记追加「档位验收登记 USC-06 完成」（7 处）。
      Skill: none
- [x] Add: manufacturing/README.md 增「档位验收登记（USC-06）」段——US-MF-01..06 制造档 Must 逐条证据锚点表格（从矩阵主表转录）。
      Skill: none
- [x] Add: b2b/README.md 增「档位验收登记（USC-06）」段——US-B2-01 完整档/大客户 Should + 真实 EDI 网关=product-scope 延迟边界 + 既有 E2E 锚点表。
      Skill: none

Exit Criteria:

- [x] 三文档落盘；矩阵分流表 #8/#9 无「USC-06 承接」悬空引用（翻转「已登记」+ grep 验证）。

### Phase 2 — 一致性核对

Status: completed
Targets: 全 doc
Skill: none

- [x] Proof: grep 核对——矩阵「档位验收登记 USC-06」7 处命中（MF-01..06+B2-01 主表行）；分流表「USC-06 承接」悬空引用零残留（#8/#9 已翻转「已登记」）；两 README 段落落盘；roadmap USC-06 行回写完成。

Exit Criteria:

- [x] 引用一致性核对通过。

## Draft Review Record

- Independent draft review iteration 2（授权转 ready）: 复审方明示「修订后可直接转 ready，无需再审一轮」——M1/M2/m1-m5 整改后执行者自检通过（待 closure 审计复核 M1 口径落盘结果）。
- Independent draft review iteration 1: needs revision（独立子代理 agent_2620aae3，0B+2M+5m；判定转录/锚点存在性/降级合法性全成立）——M1 档位语义错误（「MF-05/06 全档通用」与 customization-capabilities 能力五/gap analysis Should/矩阵无注记三处冲突→统一制造档 Must 口径）；M2 证据锚点误植（TestErpC18CtLifecycleRebate 为合同返利非 B2B→删除换看板 value 2 件）；m1 开头笔误；m2 引文排版照录；m3 主表 05/06 行纳入注记；m4 US-B2-01 缺口列承接字样同步消除；m5 Skill none 补 §8.7+先例引用。全部整改落盘，复审方明示「修订后可直接转 ready，无需再审一轮」。

## Closure Gates

- [x] 范围内行为完成（矩阵 7 行注记+分流表翻转+两域 README 档位验收登记段）
- [x] 相关文档对齐（矩阵+manufacturing/README+b2b/README+roadmap USC-06 行回写）
- [x] 已运行验证（grep 引用一致性核对通过；纯文档零行为变更，无代码/测试门控）
- [x] 无范围内项目降级为 deferred/follow-up（真实 EDI 网关=product-scope 既定延迟段，非新增）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision 2M+5m → 修订后直接 ready[复审方明示免再审]）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + 矩阵登记 + 两域 README 档位验收登记段 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

（无——矩阵判已覆盖，无残量；真实 EDI 网关=product-scope 既定延迟段，非本项新增。）

## Closure

Status Note: 独立结束审计通过（round1 NEEDS REVISION 1M+1m → 定点整改 → 增量复审 RESOLVED）。矩阵 US-MF-01..06+US-B2-01 七行注记、分流表 #8/#9 翻转、两域 README 档位验收登记段、roadmap 行 done+计数块 todo=2/ready=0/done=7 全部实仓核验一致；档位口径=制造档 Must（M1 订正落盘确认）；TestErpC18 误植零残留（M2 确认）；check-plan-gates --strict PASS。USC-06 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，只读；round1 全量审计 + round2 定点增量复审）
- Evidence: round1（NEEDS REVISION，1M+1m+2c）：M-1=矩阵 :118 US-B2-01 缺口列悬空字样「USC-06 档位验收登记承接」未消除（git diff 证实仅追加注记未删旧句，m4 执行期遗漏）；m-1=计划 :13 残留「US-MF-05/06 全档通用」与 :11 M1 订正自相矛盾；c-1=日志「完成并提交」时序超前（随提交落盘豁免）；c-2=roadmap 行号引用漂移一格。round2 增量复审（RESOLVED）：①矩阵 :118 现值悬空字样删除，grep「USC-06 档位验收登记承接」docs 全域 0 命中，主表注记回归 7 处；②计划 :13 订正口径一致，交付物「全档通用」零残留；③日志/Review Record 陈述随 ①② 工件修复由失实转为属实，本证据行即权威留痕。独立复跑：矩阵注记 7 处/悬空字样 0 命中/check-plan-gates.mjs --strict PASS（0 new violations vs frozen snapshot）。

Follow-up:

- （无。）
