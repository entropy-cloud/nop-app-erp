# 2026-08-07-2359-1 rc-ma4-a4-2-97-100-crm-f3-cpq-funnel-runtime 漏斗等值边界/CPQ configSnapshot 落库/弱指针回写/规则表达式运行时确认

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.97 / A4.2.98 / A4.2.99 / A4.2.100
> Related: `docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md`（A1.30 §7 存疑点 SP-1..SP-4 + §6 新建 P2-RC-036/037/038/039 + reuse P1-MA2-075 resolved R1.24）
> Audit: required

## Current Baseline

CRM 域 A1.30 切片（CRM-F3 CPQ/漏斗推进，UC-CRM-06 漏斗阶段推进 + UC-CRM-13 CPQ 配置-定价-报价）MA1 报告 §7 共列出 4 个静态存疑点（SP-1..SP-4）。CRM 域不直接产生会计凭证，故本批存疑点**均不触及业财保护区域探针**（区别于 assets/inventory/projects 业财批）。四项均为边界/config-gate/契约一致性运行时确认（A1.30 §6 全部 finding 为 P2，无 P0/P1 finding）。

- **A4.2.97（A1.30 SP-1 UC-CRM-06 ④ 等值边界运行时触发面；P2-RC-036）**：HEAD 静态判定 = `erp-crm.allow-stage-backward=true` 放行回退时，等值 stage 移动（toSeq==fromSeq）实际行为——`validateStageDirection:99` 严格 `<` 不拦等值（allow-backward=true 仅控制 `<` 分支），等值 stage 移动经 GraphQL `ErpCrmLead__moveStage` 放行；L1 `:122` 字面 `<=`（等值拒绝）与代码 `<`（等值放行）边界差异。运行时确认等值 stage 移动是否成功 + 对 FunnelAggregationEngine sequence 排序假设的实际影响。
- **A4.2.98（A1.30 SP-2 UC-CRM-13 ⑩ configSnapshot JSON 实际落库字段与 quotation 关联；P2-RC-038）**：HEAD 静态判定 = `buildQuotationData:246` 字面 `remark="CPQ pricingSource=...; snapshot=" + truncate(configSnapshot, 500)`——configSnapshot 超过 500 字符时**截断**。运行时确认大型配置（多特征/多规则）的 snapshot 截断是否丢失关键配置信息 + `quotation.remark` 列实际长度上限（ORM 字段精度）是否足够承载典型配置。
- **A4.2.99（A1.30 SP-3 UC-CRM-13 ⑫ generateQuote 弱指针回写 relatedBillType 枚举值与 sales 域契约一致）**：HEAD 静态判定 = `generateQuote:127` 字面 `setRelatedBillType(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION)`。运行时确认该枚举常量实际字面值与 sales 域 ErpSalQuotation 回链契约一致（与 A1.28 UC-CRM-03 转化路径 relatedBillType 同型回写交叉确认）+ 与 sales 域 quotation.code 命名空间无冲突。
- **A4.2.100（A1.30 SP-4 UC-CRM-13 ② conditionExpression XLang 评估的失败模式）**：HEAD 静态判定 = `ProductConfigRuleEngine.evalCondition:85-98` 编译失败抛 NopException 含 conditionExpression param。运行时确认复杂表达式（如 `selectedFeatures.CPU_TYPE == 'INTEL_XEON' && selectedFeatures.MEMORY == '64GB'`）经 XLang `allowUnregisteredScopeVar(true).compileFullExpr` 实际评估行为是否符合预期（L4 测试覆盖简单 source 匹配为主，复杂表达式运行时探查）。

剩余差距：四项均为只读运行时确认，CRM 域不直接产生会计凭证故不触及业财保护区域探针。A1.30 §6 全部 finding 为 P2（P2-RC-036 等值边界 / P2-RC-037 前端 wizard successor / P2-RC-038 createFromConfig→save 方法名漂移 + configSnapshot 截断 / P2-RC-039 configSnapshot 落库断言弱），修复归 MR1 R1.0 展开器（P2 登记不强制，全部纯 Processor/BizModel/测试补充预授权不触 ask-first）；P1-MA2-075（stageId 单向递增守卫）维持 resolved R1.24。本计划仅确认运行时行为以维持/细化裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.97-A4.2.100 四项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：A4.2.97 P2-RC-036（等值边界）维持 P2 + 运行时证据；A4.2.98 P2-RC-038（configSnapshot 截断）维持 P2 + 运行时证据（确认截断是否丢关键配置）；A4.2.99（弱指针枚举值契约一致性）确认主路径闭合或登记 watch-only；A4.2.100（conditionExpression 评估）确认主路径行为正确或登记 watch-only；若运行时发现活跃数据破坏则触发 MR0。
- 完成后回写 roadmap A4.2.97-A4.2.100 `todo → done`，并按裁决更新 arm-index（维持注记，无未经比对新建）。

## Non-Goals

- 不实现等值边界守卫（P2-RC-036）/ 前端配置向导 wizard（P2-RC-037）/ createFromConfig→save 方法名对齐 + configSnapshot 独立字段（P2-RC-038）/ configSnapshot 落库断言补强（P2-RC-039）——修复义务归 MR1 R1.0 展开器；全部纯 Processor/BizModel/AMIS view.xml/测试补充预授权不触 ask-first（roadmap §横切关注点 #5）。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不复跑 MA2 状态机审计（A2.14 crm Lead 5 态 + Event 3 态 PASS + P1-MA2-075 resolved R1.24 作为既有证据输入，不重新核实行为本身）；不重审 P2-RC-036~039 维度（A1.30 已审，本计划仅运行时确认）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-05-1830-rc-ma1-a1-30-crm-f3-cpq-funnel-advancement.md` §5/§6/§7 + `docs/design/crm/`（use-cases.md / state-machine.md / cpq.md 衔接契约）+ `docs/design/sales/`（quotation 回链契约，弱指针枚举值交叉确认）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + validateStageDirection 等值边界追踪 + buildQuotationData remark 截断 + ORM remark 列精度 census + RELATED_BILL_TYPE_SALES_QUOTATION 枚举值与 sales 域 ErpSalQuotation 命名空间交叉确认 + ProductConfigRuleEngine.evalCondition XLang compileFullExpr 行为追踪（grep census / config-gate allow-stage-backward 默认值普查 / quotation.remark ORM 精度读 / 枚举常量字面值 grep / XLang 评估路径追踪），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.97-A4.2.100）

Status: completed
Targets: `docs/audits/2026-08-07-2359-rc-ma4-a4-2-97-100-crm-f3-cpq-funnel-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.30 done ✓

- [x] **A4.2.97 UC-CRM-06 ④ 等值边界运行时触发面确认（P2-RC-036）**：确认 `validateStageDirection:91-110` STRICT 默认 + config-gated allow-backward 落地；确认 `:99` 严格 `<` 不拦等值（toSeq==fromSeq 放行）+ allow-backward=true 仅控制 `<` 分支；确认等值 stage 移动经 GraphQL `ErpCrmLead__moveStage` 运行时成功；确认 config `erp-crm.allow-stage-backward` 默认值 + 全生产 application.yaml override 普查（config-gate = 部署启用决策）。裁决：维持 P2-RC-036 P2（§2 P2① 边界场景弱，修复归 MR1 纯 Processor 预授权[将 `toSeq < fromSeq` 改为 `toSeq <= fromSeq`] 不触 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.98 UC-CRM-13 ⑩ configSnapshot JSON 截断与 quotation 关联确认（P2-RC-038）**：确认 `buildQuotationData:246` 字面 `remark="CPQ pricingSource=...; snapshot=" + truncate(configSnapshot, 500)`；确认大型配置（多特征/多规则）的 snapshot 截断是否丢失关键配置信息（cosmetic 影响面）；确认 `quotation.remark` 列 ORM 字段精度（grep orm.xml remark length）是否足够承载典型配置。裁决：维持 P2-RC-038 P2（§2 P2① 边界弱，configSnapshot 落 remark 截断 cosmetic，修复归 MR1 纯 BizModel[配置快照独立字段或扩 remark 长度]预授权不触 ask-first）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.99 UC-CRM-13 ⑫ generateQuote 弱指针 relatedBillType 枚举值与 sales 域契约一致确认**：确认 `generateQuote:127` 字面 `setRelatedBillType(ErpCrmConstants.RELATED_BILL_TYPE_SALES_QUOTATION)`；确认该枚举常量实际字面值（grep 常量定义）与 sales 域 ErpSalQuotation 回链契约一致；确认与 sales 域 quotation.code 命名空间无冲突（与 A1.28 UC-CRM-03 转化路径 relatedBillType 同型回写交叉确认）。裁决：确认主路径闭合（枚举值契约一致）或登记 watch-only（弱指针 cosmetic 风险）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.100 UC-CRM-13 ② conditionExpression XLang 评估失败模式确认**：确认 `ProductConfigRuleEngine.evalCondition:85-98` 编译失败抛 NopException 含 conditionExpression param；确认复杂表达式（如 `selectedFeatures.CPU_TYPE == 'INTEL_XEON' && selectedFeatures.MEMORY == '64GB'`）经 XLang `allowUnregisteredScopeVar(true).compileFullExpr` 实际评估行为是否符合预期；确认 L4 测试覆盖（简单 source 匹配为主）与复杂表达式运行时探查的差距面。裁决：确认主路径行为正确（简单表达式强测覆盖）或登记 watch-only（复杂表达式边界）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：四项存疑点各出 §裁决（主路径闭合 / 维持 P2 + 运行时证据 / 登记 watch-only / config-gate 部署决策 / 触发 MR0）+ §与既有 finding 衔接（P2-RC-036/037/038/039 + reuse P1-MA2-075 resolved R1.24 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。CRM 域不直接产生会计凭证，不触及业财保护区域探针。

- [x] 验证报告落盘，含四项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：主路径闭合 / 维持分级（P2 Q4 登记 / watch-only / config-gate）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.97-100 done）、`docs/audits/arm-index.md`（维持注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P2-RC-036（等值边界）/ P2-RC-037（前端 wizard successor）/ P2-RC-038（createFromConfig→save 方法名漂移 + configSnapshot 截断）/ P2-RC-039（configSnapshot 落库断言弱）维持 P2（P2 登记不强制，修复归 MR1 纯 Processor/BizModel/AMIS view.xml/测试补充预授权不触 ask-first）；P1-MA2-075（stageId 单向递增守卫）维持 resolved R1.24。无新 finding 新建（全部维持）。
- [x] `Add` roadmap A4.2.97-A4.2.100 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 四项状态已更新为 done 且与报告裁决一致
- [x] arm-index 维持注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0263ed3f9ffep4QLo4VUfBxobu) — no blocking issues. Citation accuracy verified: P2-RC-036=等值边界 / P2-RC-037=前端 wizard successor / P2-RC-038=createFromConfig→save 漂移 + configSnapshot 截断 / P2-RC-039=configSnapshot 落库断言弱 全部与 A1.30 §6 逐字一致，无 shifted/misattributed IDs（参考 crm 计划曾有的错位 bug 在此不存在）。Item↔roadmap↔§7 1:1 映射正确（A4.2.97→SP-1 / A4.2.98→SP-2 / A4.2.99→SP-3 / A4.2.100→SP-4，SP-3/SP-4 watch-only 无 finding 正确仅在 Phase 2 同步）。Deps 满足（A4.2 expander done）。CRM 域不产生会计凭证→无业财保护区域探针声明到位。结构/模板/规则合规，无 anti-slack 禁词。Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [x] 范围内行为完成（四项存疑点均有 file:line 运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P2-RC-036~039 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；P2-RC-036（等值边界）/ P2-RC-038（createFromConfig→save 方法名漂移 + configSnapshot 截断）/ P2-RC-039（configSnapshot 落库断言弱）修复归 MR1 R1.0 展开器纯 Processor/BizModel/测试补充预授权不触 ask-first；P2-RC-037（前端配置向导 wizard）修复归 MR1 纯前端 AMIS view.xml 预授权（登记不强制，前端 successor）。本审计维持分级不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: 四项存疑点（A4.2.97-A4.2.100）运行时确认完毕——两项维持 P2（P2-RC-036 等值边界 / P2-RC-038 configSnapshot 截断）+ 一项主路径闭合（A4.2.99 弱指针枚举值契约一致）+ 一项主路径行为正确并登记复杂表达式 watch-only 观察（A4.2.100 conditionExpression XLang 评估）。零新 finding / 不触发 MR0 / 不归 MR1。P2-RC-036/037/038/039 维持 P2 + P1-MA2-075 维持 resolved R1.24。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure 审计子代理（新会话，MISSION_DRIVER:2026-08-04-224309-mission-driver，不重用执行者上下文）
- Evidence: 独立冷重读复核通过——(1) 验证报告 `docs/audits/2026-08-07-2359-rc-ma4-a4-2-97-100-crm-f3-cpq-funnel-runtime.md` 存在且 8 段齐全（§0-§8），四项裁决各带 file:line 实测证据；(2) `docs/audits/arm-index.md:380` RC 交叉引用注记已落（P2-RC-036/037/038/039 维持 + P1-MA2-075 维持 resolved，无新 finding）；(3) `docs/backlog/requirement-compliance-roadmap.md:250-253` A4.2.97-A4.2.100 四行均 `done ✅` 且裁决注记与报告一致；(4) `docs/logs/2026/08-07.md:3-20` 完成条目存在。`git diff --stat HEAD` 仅 3 个 .md 修改 + 报告/计划新增，零生产代码/ORM/api.xml/view.xml/config 变更 → checker actual=baseline 无回归风险。五点一致性核对：Plan Status completed / Phase 1+2 Status completed / 各 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / 日志条目一致。P2-RC-036~039 已诚实归入 Deferred But Adjudicated（非 follow-up 隐藏）。CRM 域不产生凭证故无业财探针触发。审计通过。

Follow-up:

- 无非阻塞跟进项目（P2 修复义务已明确归 MR1 R1.0 展开器，记录于 Deferred But Adjudicated 节，非本审计 follow-up）
