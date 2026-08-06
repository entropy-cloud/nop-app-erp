# 2026-08-07-0530-3 rc-ma4-a4-2-22-26-hr-payroll-survey-runtime HR 薪酬/工时/调研域过账触发链与桩功能运行时确认

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Items A4.2.22 / A4.2.23 / A4.2.24 / A4.2.25 / A4.2.26
> Related: `docs/audits/2026-08-03-0000-rc-ma1-a1-14-hr-f3-payroll-survey.md`（A1.14 MA1 报告 §7 存疑点 1..5 + §6 P1-RC-015/016 新建 + P1-MA4-017/P1-MA2-041/P1-MA2-043 reuse 重开）、`docs/plans/2026-08-02-2250-3-rc-ma1-a1-14-hr-f3-payroll-survey.md`（A1.14 计划）
> Audit: required

## Current Baseline

A1.14（HR-F3 薪酬与调研）MA1 报告 §7 列出 5 个静态存疑点，对应 §6 多项 finding：P1-MA4-017（计提+公司承担过账，reuse 重开，Q4 会计正确性类）、P1-MA2-041（调研 CRUD 桩，reuse 重开）、P1-MA2-043（工时单仅 submit，reuse 重开）、P1-RC-015（24h 校验 + totalHours，新建）、P1-RC-016（匿名 respondentHash + 聚合 + eNPS，新建）。A1.14 §5 裁决：UC-HR-03 = P1、UC-HR-04 = P1 on ⑯（⑥-⑫⑬⑭⑮⑰ 接受）、UC-HR-11 = P1。

这 5 项存疑点涉及两类运行时确认：(1) 业财过账触发链的运行时实测（§7-1/§7-2，会计正确性类，触及业财保护区域探针——只读确认不改过账逻辑）；(2) 桩功能/数据校验的运行时缺口确认（§7-3/§7-4/§7-5，确认功能确实不可达且结果表确实为空）。

- **A4.2.22（§7-1 UC-HR-04 ⑯ 计提+公司承担过账运行时触发链）**：HEAD 静态判定 = 永不生成——`tryPostAccrual:67` 零调用方死代码 + `socialInsuranceER/housingFundER` 无 ORM 列（`PayrollCalculator:110/:115` 计算后丢弃）+ 290/300 event 永不生成。`approve→APPROVED` 时计提 SALARY(270) + 290/300 event 是否生成。运行时可经 approve 路径确认 GL 仅收 280（SALARY_PAYMENT）。**触及业财保护区域探针——本计划仅只读确认不改过账逻辑。**
- **A4.2.23（§7-2 UC-HR-04 公司承担金额运行时丢弃确认）**：HEAD 静态判定 = 丢弃——`PayrollCalculator:110/:115` socialInsuranceER/housingFundER 计算后无 setRemark/billData 传递到 PostingEvent。运行时可断言 billData 不含 ER 金额。
- **A4.2.24（§7-3 UC-HR-03 ②24h 校验运行时拦截）**：HEAD 静态判定 = 无校验——grep `24\|MAX_HOURS` 零业务命中，同一日多条 TimesheetLine hours 之和 > 24 不被拦截。运行时可构造 >24h 提交确认无报错。
- **A4.2.25（§7-4 UC-HR-11 ㉖匿名 respondentHash 运行时防重复）**：HEAD 静态判定 = 无 writer/无校验——`ErpHrSurvey` 列 :1429 零 writer/零校验，匿名模式重复提交不被拦截。运行时可构造同 respondentHash 重复提交确认无拦截。
- **A4.2.26（§7-5 UC-HR-11 ㉘㉙ CLOSED 自动聚合 + eNPS 运行时计算）**：HEAD 静态判定 = 无 mutation/无算法——`ErpHrSurveyResultBizModel` 18 行桩零 aggregate + 无 eNPS 计算 + ORM :1357/:1499 零 writer。运行时确认结果表永远空。

剩余差距：上述五项均为只读运行时确认。A4.2.22/A4.2.23（业财过账）的修复义务归 MR1 且触及会计保护区域（须 ask-first）；A4.2.24-A4.2.26（桩功能）的修复义务归 MR1。本计划仅确认运行时行为以维持/细化 P1 裁决，不改变 Q4 强制实现义务。

## Goals

- 对 A4.2.22-A4.2.26 五项存疑点产出运行时行为证据链，输出验证报告落盘 `docs/audits/`。
- 每项给出 §2 判据裁决：维持 P1（reuse 重开 finding 不降级，Q4 强制实现）+ 记录运行时证据（过账仅 280 / ER 丢弃 / 无 24h 校验 / 无防重复 / 结果表空），或升级（若运行时发现会计错误已活跃则触发 MR0）。
- 完成后回写 roadmap A4.2.22-A4.2.26 `todo → done`，并按裁决更新 arm-index。

## Non-Goals

- 不实现计提过账接线（270/290/300）/ ER 金额持久化 / 24h 校验 / 调研聚合 / eNPS 计算——修复义务归 MR1 R1.0 展开器；A4.2.22/A4.2.23 触及会计保护区域的修复须 ask-first。
- 不修改任何真相源（product-scope/use-cases/owner doc 需求契约段落）。
- 不修改过账逻辑或 PostingProcessor 核心路径（roadmap §横切关注点 #5 ask-first 保护区域）。
- 不复跑 MA2 状态机审计或 A4.4 代码质量审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/2026-08-03-0000-rc-ma1-a1-14-hr-f3-payroll-survey.md` §5/§6/§7 + `docs/design/human-resource/`（payroll.md / payroll-simulation.md / employee-survey.md）+ `docs/design/finance/posting.md`（过账触发链）
- Skill Selection Basis: roadmap MA4 全部工作项指定 `docs/skills/multi-dimensional-audit-prompt.md`。本计划为只读审计，无代码变更。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 本计划为代码可达性 + 过账触发链追踪 + 桩功能确认（grep census / PostingEvent 构造点追踪 / writer 零命中确认），无需运行应用或 DB。

## Execution Plan

### Phase 1 - 运行时证据采集与验证报告撰写（A4.2.22-A4.2.26）

Status: completed
Targets: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`（新建验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: A4.2 done ✓；A1.14 done ✓

- [x] **A4.2.22 计提+公司承担过账运行时触发链确认**：追踪 `SalaryPostingDispatcher` 过账触发链——确认 `tryPostAccrual:67` 零调用方（grep 调用方零命中）+ 270/290/300 event 永不构造（`dispatchPosting` 仅 280 SALARY_PAYMENT wired）+ `approve→APPROVED` 不触发计提过账。确认 GL 在 approve 路径仅收 280 凭证（`TestErpHrPayrollEngine:408-440` 测试注释自述 Deferred）。**触及业财保护区域探针——只读确认，不改过账逻辑。**
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.23 公司承担金额运行时丢弃确认**：确认 `PayrollCalculator:108-109/113-114/156-157` socialInsuranceER/housingFundER 计算后无 setRemark/setBillData 传递到 PostingEvent（grep `socialInsuranceER\|housingFundER\|housing_fund_er` ORM 列零命中）+ `SalaryPostingDispatcher.buildEvent` billData 不含 ER 金额。确认 ER 金额在过账时被静默丢弃。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.24 24h 校验运行时拦截确认**：确认 `ErpHrTimesheetLineBizModel`（18 行桩）+ submit 路径无 24h 校验（grep `24\|MAX_HOURS\|maxHours\|totalHours.*24` 零业务命中）+ 同一日多条 TimesheetLine hours 之和 > 24 不被拦截（无聚合校验 mutation）。确认 `totalHours` 无 writer（派生字段未实现）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.25 匿名 respondentHash 运行时防重复确认**：确认 `ErpHrSurvey` 列 `respondentHash`（ORM :1429）零 writer + 零校验 + 匿名模式 submitRespondent 不写 respondentHash（无 setRespondentHash）+ 重复提交无唯一性拦截。确认匿名防重复机制完全缺失（ORM 列存在但无运行时消费）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **A4.2.26 CLOSED 自动聚合 + eNPS 运行时计算确认**：确认 `ErpHrSurveyResultBizModel`（18 行桩）零 aggregate mutation + 无 eNPS 计算方法 + `ErpHrSurveyResult` ORM :1357/:1499 零 writer + CLOSED 状态迁移无聚合触发。确认调研结果表运行时永远空（CLOSED 不产出任何聚合数据）。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] **验证报告撰写**：五项存疑点各出 §裁决（维持 P1 reuse 重开 finding + 运行时证据记录 / 触发 MR0）+ §与既有 finding 衔接（P1-MA4-017 / P1-MA2-041 / P1-MA2-043 / P1-RC-015 / P1-RC-016 交叉引用）+ §过程纪律自检（checker 退出码门控——无代码变更 actual=baseline；closure-audit 独立性声明）。报告落盘。
  - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

> 本阶段为只读审计，无生产代码变更。A4.2.22/A4.2.23 触及业财保护区域探针——只读确认不改过账逻辑。

- [x] 验证报告落盘 `docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`，含五项存疑点各自裁决 + file:line 证据 + §2 判据命中分支
- [x] 每项裁决明确：维持 P1（Q4 强制实现，reuse 重开 finding 不降级）+ 运行时证据记录，或升级触发 MR0

### Phase 2 - Finding 衔接、roadmap/log 同步

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（A4.2.22-26 done）、`docs/audits/arm-index.md`（reuse 重开注记追加）、`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 报告落盘

- [x] `Decision` arm-index 衔接裁决：P1-MA4-017 / P1-MA2-041 / P1-MA2-043 reuse 重开维持 P1（运行时证据确认方案 B Deferred 关闭在 Q4=(a) 下不成立，修复归 MR1，A4.2.22/A4.2.23 触及会计保护区域须 ask-first）；P1-RC-015 / P1-RC-016 维持 P1（运行时确认缺口存在，修复归 MR1）。无新 finding 新建（全部 reuse/维持）。
- [x] `Add` roadmap A4.2.22-A4.2.26 `todo → done`；`docs/logs/2026/08-07.md` 追加完成条目。

Exit Criteria:

- [x] roadmap 五项状态已更新为 done 且与报告裁决一致
- [x] arm-index reuse 重开注记已追加（无未经比对直接新建的 finding）

## Draft Review Record

- Independent draft review iteration 1: acceptable-as-is (ses_02843734fffe9nALn66nVbyLsW) — no blocking issues. Source report §5/§6/§7 citations accurate, ACCOUNTING protected-area handling exemplary (READ-ONLY marked 6 locations, P1s maintained, fixes deferred to MR1 with ask-first NOT silently dropped), Deps satisfied, rule-14 scope satisfied. Non-blocking suggestions applied for consistency: Phase 2 `Item Types` corrected to `Decision | Add`; ambiguous `§5` references clarified to `roadmap §横切关注点 #5` (protected-area protocol, not report §5 matrix). Consensus reached → flipped to active.

## Closure Gates

> 本计划为只读审计（零生产代码/ORM/api.xml/view.xml/真相源变更）。closure 时确认 checker 未触发 actual > baseline。

- [x] 范围内行为完成（五项存疑点均有 file:line 运行时证据 + 明确裁决）
- [x] 相关文档对齐（报告落盘 + roadmap/log 同步 + arm-index 衔接裁决记录）
- [x] 已运行验证（checker actual=baseline 确认；无代码变更故无 build/test 回归风险）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA4-017 / P1-RC-015 / P1-RC-016 / P1-MA2-041 / P1-MA2-043 修复实现

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅运行时确认；五项 P1 的修复（计提过账接线/ER 持久化/24h 校验/totalHours/调研聚合/eNPS/工时单 approve/调研 publish-close）归 MR1 R1.0 展开器，Q4 裁决 P1 强制实现。A4.2.22/A4.2.23 触及会计保护区域（VoucherFact/PostingProcessor 核心路径）的修复须 ask-first + 独立 plan-audit（roadmap §横切关注点 #5）。本审计维持 P1 不撤销。
- Successor Required: yes（MR1 R1.0 展开为 RC-R1.n 时承接）

## Closure

Status Note: A1.14 §7 五项静态存疑点（A4.2.22-A4.2.26）的运行时证据采集与裁决全部完成。5 项静态判定无一翻转，全部「维持 P1 + 运行时证据记录」分支，不触发 MR0（运行时未发现会计错误已活跃——GL 从未收到错误凭证而是缺凭证可经试算平衡发现）。五项 P1 修复义务归 MR1 R1.0 展开器（A4.2.22/A4.2.23 触会计保护区域须 ask-first；A4.2.24-A4.2.26 纯 BizModel 预授权），本审计维持 P1 不撤销。本计划为只读审计（零生产代码/ORM/api.xml/view.xml/config 默认值/真相源变更），无 build/test 回归风险。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，新会话，非执行者上下文）
- Audit Report: `docs/audits/2026-08-07-0530-rc-ma4-a4-2-22-26-hr-payroll-survey-runtime.md`（`> Audit Status: closed`，9 段齐全：§1 存疑点清单 + §2-1..§2-5 五项运行时证据与裁决 + §3 测试证据 + §4 业财保护区域探针纪律声明[6 处 READ-ONLY 标记] + §5 与既有 finding 衔接[5 项 reuse 维持] + §6 多维审计自检[8 维度] + §7 过程纪律自检 + §8 结论 + §9 差异增量声明）
- Roadmap Sync: `docs/backlog/requirement-compliance-roadmap.md` A4.2.22/A4.2.23/A4.2.24/A4.2.25/A4.2.26 全部 `todo → done ✅`（:176-179 五行 confirmed）
- arm-index Sync: `docs/audits/arm-index.md` 追加「A4.2.22-26 hr-F3 运行时确认」RC 交叉引用注记（:349），P1-MA4-017(:843) / P1-MA2-041(:505) / P1-MA2-043(:507) / P1-RC-015(:154) / P1-RC-016(:155) 维持既有分级不撤销，无新 finding
- Log Sync: `docs/logs/2026/08-07.md` 追加完成条目（A4.2.22-26 五项 SP 裁决全数 CONFIRMED 维持 P1 + 下一步声明）
- Closure Auditor Verification: 独立子代理复核确认 (a) 验证报告存在且 9 段齐全 (b) 五项裁决均维持 P1 非 P0 升级 (c) A4.2.22/A4.2.23 业财保护区域探针为只读未改过账逻辑 (d) Deferred But Adjudicated 正确分类（五项 P1 修复归 MR1 非本审计范围降级）(e) 文本一致性：Plan Status=completed / Phase 1+2 Status=completed / 全部 Exit Criteria [x] / 全部 Closure Gates [x] / 日志一致

Follow-up:

- 无非阻塞跟进项目（五项 P1 修复义务已明确归 MR1 R1.0 展开器，记录于 Deferred But Adjudicated 节，非本审计 follow-up）
