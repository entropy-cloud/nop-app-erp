# Lesson 08: 计划标记「completed」却缺独立 closure audit

> **来源**：2026-07 审计-修复任务。R3.5（plan `2026-07-31-1439-1`，P1-MA6-005）在 closure-pending 检测中发现 **14 份「completed」计划无独立子代理 closure audit 证据**；这是继 Round 1、Round 2 之后的**第三波**（Round 3）清理，说明该失败模式跨多轮复发。MA6 A6.4（P1-MA6-003/004/005）将其登记为系统性过程纪律缺口。
> **适用场景**：任何非平凡计划（触及保护区域、跨多模块、更改 API/模型/认证）。特别是由 mission-driver 自动推进的计划——它们容易只勾 `[x]` + 标 `Status: completed` 而跳过独立子代理 closure 门控。
> **失败模式**：计划执行者（或 mission-driver）把所有 `[ ]` 勾成 `[x]`、把 `Status:` 改成 `completed`、甚至填了 `## Closure` 段，但**从未让一个独立子代理在新会话中运行 closure-audit 并留下持久证据**。后续审计（如 A6.4）回溯时发现 `## Closure` 段是执行者自填，无 `Independent Closure Audit` 证据指针。

## 核心论点

「completed」有两层含义，缺一不可：

1. **内部一致性**：所有 `[ ]` 已勾选 + `Status: completed` + Plan Status updated（见 lesson 03）。
2. **外部验证**：独立子代理（fresh session）运行 `closure-audit-prompt`，在 `## Closure` 留下持久证据指针（auditor / task id / 五点一致性复核 / anti-hollow / deferred honesty）。

只做到第 1 层是「自我审计」，AGENTS.md §技能组合使用方式明令「实施 AI 自己审计自己的产出 = 盲区保留」。closure-pending 就是只做了第 1 层、漏了第 2 层。

> **与 lesson 03 的区别**：lesson 03 是「Status 行 vs `[ ]` 复选框**文本**不一致」（内部状态文本漂移）；本 lesson 是「内部文本一致但**缺独立外部审计**」（过程门控缺失）。两者正交，可同时发生。

## 失败模式（典型路径）

```
1. 执行者完成所有工作项，跑 mvn 绿
2. 把所有 [ ] 勾 [x]，Status → completed，Plan Status → completed
3. 在 ## Closure 填 Status Note + Evidence（自己写「已 PASS」）
4. 没有调用独立子代理 fresh session 运行 closure-audit
   → 或调用了但没在计划里留 auditor/task id 指针
5. 几周后 A6.4 回溯：grep ## Closure 段无 "Independent Closure Audit" / 无 task ses_ 指针
   → 判定 closure-pending
6. 必须回炉：独立子代理逐份补 closure audit，回填证据
```

## 真实案例

### Case: R3.5 第三波 14 份 closure-pending（3 轮清理）

- **范围**：20 候选 - 6 份已被 Round 1/Round 2 清理误报剔除 = **14 份**真 closure-pending。
- **构成**：5 份 ORM ask-first 保护区域计划（含 2 份 P0 hotfix 无 Draft Review Record 历史 gap）+ 2 份 deployment/auth 保护区域（`1206-3` 假勾选 Gate 经审计 PASS 正当化保留、`1351-1` 诚实 Gate 勾选）+ 7 份非保护区域（含 MA3 A3.8 / MA4 A4.8 / MA5 A5.1-A5.4 / MA7 A7.4 四个审计里程碑计划）。
- **处置**：全部方案 A——独立子代理 fresh session 逐份运行 closure-audit，14 份**全 PASS**，每份回填 `Independent Closure Audit (R3.5 Round 3)` Auditor 指针 + 五点一致性 + anti-hollow + deferred honesty + 实时仓库复核要点。
- **复发证据**：这是 **Round 3**。Round 1、Round 2 已分别清理过更早批次的 closure-pending 计划。说明该模式在长达数周的多轮审计-修复中**反复出现**——每批新计划完成时都有部分漏掉独立 closure。

### Case: 1206-3 假勾选 Gate

`1206-3` 计划的 Closure Gate 复选框被勾选但实际未跑 closure audit。A6.4 审计时经独立子代理补审才 PASS——「假勾选」是 closure-pending 的高危变体：执行者勾了门控框却没执行门控。

## 自检清单（标 Plan Status: completed 前）

- [ ] 是否有一个**独立子代理**（新会话 / fresh session / cold context，不是执行者本人）运行了 closure-audit？
- [ ] `## Closure` 段是否含 `Independent Closure Audit` 证据指针（auditor / task `ses_` id）？
- [ ] closure 证据是否含**实时仓库复核**（非仅复述计划内文案）？
- [ ] closure 证据是否含 **anti-hollow**（核对生成产物/测试真实存在，非空壳）+ **deferred honesty**（deferred 项显式 adjudication）？
- [ ] 保护区域计划是否额外有独立 **plan-audit**（实施前）证据？
- [ ] 若为 mission-driver 自动推进：是否在 `## Closure` 留下独立审计 task 指针，而非执行者自填 `Status Note`？

## 反模式

| 不要这样 | 应该这样 |
| --- | --- |
| 执行者自己填 `## Closure Status Note: 已完成` | 独立子代理 fresh session 跑 closure-audit，留 `Independent Closure Audit` + task id |
| 勾选 Closure Gate 复选框但不实际跑审计（假勾选） | 跑审计后在证据段引用 task id，复选框与证据同时存在 |
| 用「冷重播不是第二位审查者」批准保护区域 | 保护区域必须真人/独立审查者，冷重播不批准（见 `docs/context/ai-autonomy-policy.md`） |

## 何时复发

- mission-driver 批量推进多计划时，逐份标 completed 却不触发独立 closure。
- 高负载时期为赶进度跳过 closure-audit，事后补。
- 计划较小（如纯 .md）时误以为「不需要 closure audit」——非平凡计划无论介质都需。

## 关联

- 真相源：`docs/audits/arm-index.md`（P1-MA6-003/004/005）
- 修复证据：plan `2026-07-31-1439-1` R3.5（14 份 closure 回填）
- 关联 lesson：lesson 03（计划状态文本一致性，内部 vs 外部正交）
- 关联 skill：本失败的「检测与批量 closure」方法已提升为 skill `docs/skills/closure-pending-detection-prompt.md`；单计划 closure 用 `docs/skills/closure-audit-prompt.md`
- 关联速查：`docs/context/project-context.md` §已知失败模式
