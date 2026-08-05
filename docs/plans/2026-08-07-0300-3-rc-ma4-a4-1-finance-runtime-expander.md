# 2026-08-07-0300-3 rc-ma4-a4-1-finance-runtime-expander MA4 业财域存疑点运行时确认展开器

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.1（业财域 MA1 存疑点运行时确认**展开器**）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done，§MA4 + §10 展开器范式）、finance MA1 切片报告（A1.1-A1.7 done，§7 存疑点清单已落盘，本展开器的输入）
> Audit: required

## Current Baseline

> 本计划是**展开器工作项**（expander，对齐 R1.0/A4.2 范式），结果表面 = roadmap MA4 表内追加的 A4.1.n 实体行 + 展开映射记录。展开器**不执行运行时验证本身**——验证属后续各 A4.1.n 实体行（各自独立 plan）。展开完成（全部实体行已追加）后 A4.1 标记 done（同 R1.0 的 done 判据 = 展开完成，而非「全部验证完成」）。

- **MA1 里程碑已有效完成**：A1.1-A1.51 全部切片报告已落盘（A1.4 roadmap 状态标记 `todo` 但其 plan `2026-08-02-1815-1` 为 `completed`、audit 报告为 `closed`，属 roadmap 状态滞后，实仓证据表明 MA1 done）。这解除 A4.1 的 `Deps=MA1 done` 依赖。

- **输入就绪**：finance 域 MA1 切片报告（业财域）A1.1-A1.7 的 §7 静态存疑点清单均已落盘：

  | 切片 | 报告 | §7 存疑点（mentions）* |
  |------|------|---------------------|
  | A1.1 finance-F1 过账引擎 | `2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` | 5 |
  | A1.2 finance-F2 预算与承付 | `2026-08-02-1700-rc-ma1-a1-2-finance-f2-budget.md` | 6 |
  | A1.3 finance-F3 AR/AP 核销与坏账 | `2026-08-02-1715-rc-ma1-a1-3-finance-f3-arap.md` | 12 |
  | A1.4 finance-F4 银行对账 | `2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md` | 7 |
  | A1.5 finance-F5 成本核算 | `2026-08-02-2045-rc-ma1-a1-5-finance-f5-costing.md` | 4 |
  | A1.6 finance-F6 期间与结账 | `2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` | 4 |
  | A1.7 finance-F7 报表/看板/多账套 | `2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` | 6 |

  > 业财域范围 = finance MA1 切片（A1.1-A1.7），finance 是业财核心域；扩展域存疑点归 A4.2 展开器。若某 finance 报告 §7 存疑点引用了跨域（purchase/sales/inventory）行为，仍以其来源 finance 报告为锚（业财视角）。
  > *「mentions」= grep 词频（含段头/引用），**≠ 实际存疑点条目数**；A4.1.n 行数以各报告 §7 编号清单的逐条提取 + 跨报告去重为准（Phase 1 提取 + Phase 2 逐报告核对均为条目制，非词频制）。

- **既有证据复用**（方法论 §去重协议）：既有 arm MA2 报告（`2026-07-2*-arm-ma2-*`）已证实的行为不重复验证；A4.1.n 实体行只对「MA1 报告标注为静态存疑、需运行时确认」的点展开。无存疑点的切片不产生实体行（方法论 §MA4）。

- **保护区域**：展开器为**只读收集 + roadmap 表追加**（读 MA1 报告 §7，追加 A4.1.n 行到 roadmap MA4 表；不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。后续 A4.1.n 验证若触及会计过账/ORM 行为探针，属各 A4.1.n 实体行的保护区域门控（§5），不在本展开器范围。

- **剩余差距**：A4.1.n 实体行未追加 = MA4 里程碑的运行时验证队列缺口。本展开器解除此缺口后，mission driver 可逐 A4.1.n 实体行 DRAFT_PLANS → 验证。

## Goals

- 读取 finance MA1 切片报告（A1.1-A1.7）§7 存疑点清单**全集**，**逐存疑点**登记为 roadmap MA4 表内 A4.1.n 实体行（完整枚举，禁止抽样：每个 §7 存疑点恰好一行，跨报告同根因去重时显式标注合并依据）。
- 每条 A4.1.n 行含：来源切片 + §7 存疑点原文摘要 + 运行时验证方法（复用 `tests/e2e/` + `orchestration/_helper.ts` 原语 / 既有 JUnit / 临时探针）+ 预期证实/证伪的行为 + 触及保护区域标注（是/否 + 类别）。
- 展开完成后 A4.1 标记 done（全部实体行已追加；done 判据 = 展开完成，非验证完成）。
- 产出展开映射记录 `docs/audits/<执行时间戳>-rc-ma4-a4-1-finance-expander.md`（存疑点 → A4.1.n 行映射 + 去重依据 + 计数自检）。

## Non-Goals

- **不执行运行时验证**（验证属后续各 A4.1.n 实体行，各自独立 plan + 独立 done）。
- **不展开扩展域存疑点**（A4.2 展开器负责；本展开器只覆盖 finance A1.1-A1.7）。
- **不修改真相源**（product-scope/use-cases/owner doc 需求契约；§9 冻结）。roadmap MA4 表追加 A4.1.n 行是工作项追踪更新（非冻结真相源），属展开器既定动作。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读收集 + roadmap 表追加）。
- **不重复既有 arm MA2 已证实行为**（§去重协议）。

## Task Route

- Type: `verification or audit work`（展开器：存疑点清单 → 实体行的展开与登记；非实现变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 展开器范式 + §10 R1.0 同构 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1 工作项 + Work Item Details MA4）+ finance MA1 切片报告 A1.1-A1.7 §7 存疑点清单（输入）+ `docs/design/flow-overview.md` + `tests/e2e/orchestration/_helper.ts`（验证原语参考，用于标注 A4.1.n 行的验证方法）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。展开器需对存疑点做维度归类（运行时验证方法选择），复用其维度框架。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 展开器以读 MA1 报告 §7 + 标注验证方法为主（纯分析）。标注 A4.1.n 验证方法时引用既有 `tests/e2e/` + `orchestration/_helper.ts` 原语（不执行）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本展开器无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 存疑点收集、去重、A4.1.n 行展开

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md`（MA4 表追加 A4.1.n 行）；`docs/audits/<执行时间戳>-rc-ma4-a4-1-finance-expander.md`（展开映射记录）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Add | Decision`
- Prereqs: MA1 done（finance A1.1-A1.7 §7 存疑点清单已落盘）

- [x] `Add` 读取 finance MA1 报告 A1.1-A1.7 §7 存疑点清单全集，逐存疑点提取（原文摘要 + 来源切片 + 来源报告锚点）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 跨报告去重：同根因同控制点的存疑点合并为一行（显式标注合并依据 + 涉及切片）；不同根因/不同控制点各一行。无存疑点的切片不产生行（方法论 §MA4）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 每条存疑点展开为 roadmap MA4 表内 A4.1.n 实体行（编号 A4.1.1, A4.1.2…），每行含：来源切片 + §7 存疑点摘要 + 运行时验证方法（复用 E2E/_helper 原语 / JUnit / 临时探针）+ 预期证实/证伪行为 + 触及保护区域标注（是/否 + 类别）+ Skill（`multi-dimensional-audit-prompt`）。
      - Skill: none

Exit Criteria:

- [x] finance A1.1-A1.7 §7 存疑点全集已提取，无遗漏（逐报告核对 §7 段落存在性）
- [x] A4.1.n 实体行已追加到 roadmap MA4 表，每行含完整字段；去重合并有显式依据

### Phase 2 - 展开映射记录 + 计数自检 + A4.1 done

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma4-a4-1-finance-expander.md`（定稿）；roadmap A4.1 状态 → done
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成（A4.1.n 行已追加）

- [x] `Add` 产出展开映射记录：存疑点 → A4.1.n 行映射表 + 去重依据 + 来源报告锚点 + 触及保护区域标注汇总。
      - Skill: none
- [x] `Proof` 计数自检：A4.1.n 行数 = finance §7 存疑点去重并集数；逐报告核对（A1.1-A1.7 各报告的 §7 存疑点均已纳入或显式标注「无」）。
      - Skill: none
- [x] `Proof` 过程纪律自检（§8 模板）：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline（本展开器无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（展开器不新建 finding，A4.1.n 验证结论才新建/复用）。
      - Skill: none
- [x] `Add` 展开完成即 roadmap A4.1 状态标记 `done`（done 判据 = 全部 A4.1.n 行已追加，非验证完成）。
      - Skill: none

Exit Criteria:

- [x] 展开映射记录已落盘，含存疑点 → A4.1.n 映射 + 去重依据 + 计数自检通过
- [x] roadmap MA4 表 A4.1.n 行完整；A4.1 状态标记 done（展开完成）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02ca829caffeTEpaGJPwC7Oflk，fresh session，未起草本计划）。展开器语义正确（done=展开完成非验证完成，对齐 §MA4 + §10 R1.0 同构）；**MA1-done 依赖经实测核证**（A1.4 plan `Plan Status: completed` + audit 报告 `Audit Status: closed`，roadmap `todo` 为状态滞后，MA1 有效 done）；完整枚举纪律（每 §7 存疑点→一行，去重须显式依据）；baseline 输入准确（7 份 finance §7 段落实测存在，mentions 计数核验）；anti-slack 合规（Closure Gates 移除 build/test 有据，验证=A4.1.n 完整性+计数自检）；A4.1.n 行字段可操作（验证方法+证实/证伪+保护区域标注）。采纳非阻塞建议：baseline 表「mentions ≠ 实际存疑点条目数」澄清已修订入表注（提取/自检为条目制非词频制）。`## Closure` 段关闭时补。一处 roadmap 覆盖面提示（非本计划范围）：非 finance 核心域 A1.8-A1.51 的存疑点既不在 A4.1 也不明确在 A4.2「扩展域」，交 mission driver 后续裁决覆盖。共识达成，转 active。

## Closure Gates

> 本计划为**展开器**（只读收集 + roadmap 表追加，无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = A4.1.n 行完整性（finance §7 全集覆盖）+ 展开映射记录 + 计数自检 + 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.n 实体行已追加到 roadmap MA4 表（finance A1.1-A1.7 §7 全集覆盖）；A4.1 状态 done（展开完成）
- [x] 相关文档对齐：展开映射记录与方法论 §MA4 + §去重协议一致；A4.1.n 行字段与 roadmap MA4 工作项规范一致
- [x] 已运行验证：计数自检（A4.1.n 行数 = finance §7 去重并集）+ §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Closure

> 独立结束审计（fresh-session 子代理 `ses_02c862159ffeS8OuPaEKEkF0AD`，未执行本工作）于 2026-08-07 完成，verdict = **passes closure audit**。

审计逐项裁决（A-H 全 PASS）：
- **A 完整枚举**：独立复核 7 报告 §7，A1.1=3/A1.2=4/A1.3=3/A1.4=4/A1.5=3/A1.6=4/A1.7=4 = 25，与展开映射记录 §1/§4 完全一致，零遗漏零多余。
- **B 去重**：0 合并；A4.1.5↔A4.1.22 主题相关对正确保留独立行（不同根因 + 不同控制点 + 不同数据源 GlBalance vs VoucherLine）。
- **C 计数自检**：A4.1.n 行数(25)=§7 去重并集(25)，逐报告 7/7。
- **D 行字段完整性**：25 行抽查齐备（来源切片 + §7 锚点 + 摘要 + 验证方法 + 预期证实/证伪 + 保护区域标注）。
- **E roadmap 一致性**：A4.1=`done ✅` + A4.1.1-A4.1.25 追加（各 `todo`，Deps=A4.1 done）+ A4.2 不变。
- **F 范围合规**：`git status` 仅 3 文件（roadmap 改 / plan 改 / 映射记录新增），零代码/ORM/api.xml/真相源变更。
- **G §8 自检**：checker actual vs baseline 表 + 纯 reporter 注 + 无代码变更无回归风险 + closure-audit 独立性声明 + arm-index 交叉去重声明（展开器不新建 finding）齐备。
- **H 保护区域标注**：A4.1.15（成本过账探针）+ A4.1.16（数据删除探针）正确标 ask-first，23 行正确标不触及，可辩护且保守。

非阻塞观察（已采纳，不影响闭环）：A4.1.8（JUnit 扩展 writeOff 至 PARTIAL→WRITTEN_OFF 边界）边界评级「否」可辩护（辅助账余额断言，非 FIFO 过账核心/删除逻辑新探针）。

共识达成，A4.1 展开器闭环。

## Deferred But Adjudicated

### A4.1.n 实体行的运行时验证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 展开器结果表面 = A4.1.n 行追加（展开完成即 done）。各 A4.1.n 的运行时验证属后续独立 plan + 独立 done（mission driver 逐项 DRAFT_PLANS → 验证）。触及会计过账/ORM 行为探针的 A4.1.n 行须 ask-first（§5 保护区域暂停协议）。本展开器闭环不阻塞于验证落地。
- Successor Required: yes（各 A4.1.n 实体行）
