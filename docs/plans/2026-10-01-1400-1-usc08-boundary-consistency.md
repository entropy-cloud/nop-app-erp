# 2026-10-01-1400-1-usc08-boundary-consistency 延迟/出界故事边界核对与 Non-Goal 登记一致性

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-08（M5 边界固化；零代码零测试）
> Related: `docs/requirements/product-scope.md`（延迟段 :72-75）、`docs/design/portal/README.md`（future STATUS）、`docs/analysis/2026-09-22-erp-user-story-gap-analysis.md`（§3.3 延迟表）
> Audit: required（纯文档一致性核对）

## Current Baseline

- **核对对象（roadmap §6 USC-08 原文；M2/m3 订正后）**：核对 gap analysis §3.3 延迟表（7 行）与 `product-scope.md` 延迟段、`docs/design/portal/README.md` future 状态、`docs/analysis/2026-06-30-1200-feature-coverage-matrix.md` POS 🕒 行、**US-PL-07 SSO/多租户分界两半**是否一致；不一致时以 product-scope 为权威并修正 gap analysis 用词（「索引」=docs/index.md/backlog/README 引用处；两个矩阵为只读输入——矩阵内偏差只登记不改写[USC-01 §七章程]）。
- **USC-01 矩阵裁定（已固化）**：US-PL-06 门户 🕒（声明内无缺口）；US-PL-07 🔶（边界登记完整：SSO 走平台能力 Could-Should、多租户/外部集成维持延迟）；延迟/出界故事（门户/POS/电商/SaaS 多租户/原生 App/外部集成/AI 分析）仅 USC-08 做边界一致性核对，不设实现项。
- **核对预读（起草前实读；M2 订正后口径）**：product-scope 延迟段 :72-75（SaaS 多租户/垂直行业扩展/外部集成）；portal/README.md :3 STATUS future + :7 支付/SSO future 占位（「/Users/abc/app/nop-app-mall 实测不存在」为路径限定事实——`~/app/nop-app-mall-wt/nop-app-mall-master/` 887 行 ORM 实存，矩阵引用即此）；coverage 矩阵（`docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`）:39-40 POS 🕒、电子商务/商城 ✅（配套产品覆盖）；gap analysis §3.3 延迟表 **7 行**（门户/POS/电商/SaaS 多租户/原生 App/AI 预测/预测性维护 IoT）。
- **剩余差距**：四方（product-scope/gap analysis/portal README/coverage 矩阵）边界登记逐条一致性未做正式核对记录；EC（ecommerce）出界登记（矩阵 🕒「配套 nop-app-mall」与 portal README「复用 nop-app-mall 占位（实测不存在）」两处表述待归一）。

## Goals

- 边界一致性核对记录落盘（dated 分析文档）：四方逐条核对矩阵（product-scope 延迟段 × gap analysis §3.3 × portal README STATUS × coverage 矩阵 🕒 行），逐项判定一致/偏差及权威口径。
- 不一致处修正：以 product-scope 为权威修正分析/索引用词（预期候选：EC/商城表述归一、SSO Could-Should 分界对齐 portal README future 措辞）。
- roadmap §9 Non-Goals 与 USC-01 矩阵 🕒/🔶 行交叉确认（延迟项无回流）。

## Non-Goals

- 门户/POS/多租户/外部集成的实现立项（触发条件不变，product-scope 延迟段为准）。
- product-scope.md 本体修改（四方核对中 product-scope 为权威，不改）。
- roadmap 工作项增删（AI 不发明工作项——发现回流风险时按 roadmap 规则 7 停止升级人工）。

## Task Route

- Type: `requirement clarification`（边界一致性核对，纯文档）
- Owner Docs: `docs/requirements/product-scope.md`（权威，不改）、`docs/analysis/2026-09-22-erp-user-story-gap-analysis.md`（用词修正载体）、`docs/design/portal/README.md`（引用口径保持）
- Skill Selection Basis: `Skill: none`——纯边界核对，无匹配技能。

## Infrastructure And Config Prereqs

- 无（纯文档）。

## Execution Plan

### Phase 1 — 四方核对与不一致修正

Status: completed
Targets: `docs/analysis/2026-10-01-1400-1-usc08-boundary-check.md`（核对记录）、gap analysis（如需用词修正）
Skill: none

- Item Types: `Decision | Proof`

- [x] Decision: 四方逐条核对——product-scope 延迟段 vs gap analysis §3.3 vs portal README STATUS vs coverage 矩阵 POS 🕒 行；**EC/商城裁决按三处表述+双路径事实裁**（gap analysis「配套不进本仓」/矩阵「✅ 配套产品覆盖[~/app/nop-app-mall-wt 实存]」/portal README「复用占位[/Users/abc/app/nop-app-mall 不存在]」——表述差异非语义冲突，裁决是否归一用词而非预设单侧口径）；SSO Could-Should 分界（SSO+多租户两半[US-PL-07 分界两半为独立核对行]）与 portal README future 措辞对齐裁决。
      Skill: none
- [x] Proof: 核对记录落盘（dated 分析文档，含逐条判定表[判定/权威口径引用/修正清单/替代方案/残留风险三字段] + 「roadmap 完成标准三句语义确认」节：①门户触发=协同需求立项（plan-first+人工批准）②多租户待业务确认③外部集成维持延迟——三句逐条对照 product-scope/portal README 裁决）+ **backlog/README 确认节**（核查无 🕒 项误登记为近期 P 项）。
      Skill: none

Exit Criteria:

- [x] 核对记录落盘；不一致处已修正或登记（EC 归一裁决=用词归一为双路径事实口径，gap analysis :152 已补路径限定）。

### Phase 2 — 延迟项回流交叉确认（折并入核对记录；title 依据=USC-08 标题「Non-Goal 登记一致性」+M5 防 🕒 回流意图）

Status: completed
Targets: roadmap §9 Non-Goals
Skill: none

- [x] Proof: roadmap §9 Non-Goals 与 USC-01 矩阵 🕒/🔶 行交叉确认（延迟项无回流；发现回流风险按 roadmap 规则 7 停止升级人工）——结论并入核对记录「三句语义确认」节。
      Skill: none

Exit Criteria:

- [x] 交叉确认结论并入核对记录落盘（核对记录 §五）。

## Draft Review Record

- Independent draft review iteration 2: accept（同一独立子代理 agent_9157e095；「修订后可直接转 acceptable，无需再审一轮」授权——执行者自检通过后凭授权回填本行；三处口径归一=「1 轮 needs revision[2M+5m]→整改后 acceptable」）
- Independent draft review iteration 1: needs revision（独立子代理 agent_9157e095，2M+5m；五对象结构成立）——M1 roadmap 完成标准两义务缺失（backlog/README 🕒 误登记确认+三句语义确认未进 Exit）；M2 基线三处实仓误述（矩阵 🕒 行实为 POS/商城 ✅ 配套覆盖、gap §3.3 实为 7 行、mall 双路径事实）；m1 Phase 2 折并入核对记录三句确认节；m2 EC/SSO 修正改条件式去预置结论；m3 矩阵只读声明+索引钉死+coverage 矩阵全路径；m4 Decision 补最低规则 9 三字段；m5 US-PL-07 两半显式。全部整改落盘。

## Closure Gates

- [x] 范围内行为完成（核对记录 dated 文档 + gap analysis :152 用词订正）
- [x] 相关文档对齐（roadmap USC-08 行回写 + gap analysis :152 引用对齐）
- [x] 已运行验证（grep 边界关键词一致性核对通过；纯文档零行为变更无构建门）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（1 轮 needs revision[2M+5m]→整改后 acceptable）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + 核对记录 dated 文档 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

（无——边界回流风险按 roadmap 规则 7 停止升级人工，不设 successor。）

## Closure

Status Note: 独立结束审计 ACCEPT（0B/0M/3m 随本回填一并落盘：核对记录 ORM 行数改引实测+计划口径归一+roadmap 预写注记自洽）。九项四方判定全经实仓独立复验成立，纯文档零行为变更，文本一致性闭环（roadmap M1-M5 九项全 done、状态块归零）。USC-08 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，只读结束审计，2026-10-01）——ACCEPT（0B/0M/3m，3m 随本回填一并归一）
- Evidence: ①核对记录五节齐备（九项四方判定表/EC 双路径裁决/三句语义确认/backlog 🕒 零误登记/§9 无回流/修正清单）；②gap analysis :152 diff 实测单行补「（实存于 nop-app-mall-wt 工作树）」；③独立复跑全命中：矩阵 :39 POS 🕒（:258/:271/:301 同口径）、portal README :3 STATUS future、双路径实测（mall-wt 实存 2380 行 57 实体源模型[/Users/abc/app/nop-app-mall 不存在]）、check-plan-gates --strict PASS（0 新违规/82 基线）、git 面纯 5 文档零代码；④文本一致性：Phase 1/2 completed+全 [x]（仅 2 审计门控）、roadmap :68 done+计数块 todo=0/ready=0/done=9、日志 10-01 落盘。3m 整改：[m2] 核对记录「887 行 ORM」实测 2380 行/57 实体（承只读矩阵承袭，改引实测或注明承袭）；[m1] iteration-2 占位与门控措辞归一；[m3] roadmap 预写注记随 ACCEPT 自洽。

Follow-up:

- （无。）
