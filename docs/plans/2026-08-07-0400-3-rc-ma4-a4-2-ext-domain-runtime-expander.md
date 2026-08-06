# 2026-08-07-0400-3 rc-ma4-a4-2-ext-domain-runtime-expander MA4 扩展域存疑点运行时确认展开器

> Plan Status: active
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A4.2（扩展域 MA1 存疑点运行时确认**展开器**）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done，§MA4 + §10 展开器范式）、`docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 done，范式参照 + 非阻塞观察「A1.8-A1.51 存疑点覆盖面交 mission driver 裁决」）、扩展域 MA1 切片报告（A1.8-A1.51 done，§7 存疑点清单已落盘，本展开器的输入）
> Audit: required

## Current Baseline

> 本计划是**展开器工作项**（expander，对齐 R1.0/A4.1 范式），结果表面 = roadmap MA4 表内追加的 A4.2.n 实体行 + 展开映射记录。展开器**不执行运行时验证本身**——验证属后续各 A4.2.n 实体行（各自独立 plan）。展开完成（全部实体行已追加）后 A4.2 标记 done（同 R1.0/A4.1 的 done 判据 = 展开完成，而非「全部验证完成」）。

- **MA1 里程碑已有效完成**：A1.1-A1.51 全部切片报告已落盘（A1.4 roadmap 状态标记 `todo` 但其 plan `2026-08-02-1815-1` 为 `completed`、audit 报告为 `closed`，属 roadmap 状态滞后，实仓证据表明 MA1 done）。这解除 A4.2 的 `Deps=MA1 done` 依赖。

- **输入就绪**：扩展域 MA1 切片报告（A1.8-A1.51）的 §7 静态存疑点清单均已落盘。A4.1 展开器已覆盖 finance 业财域（A1.1-A1.7），A4.2 覆盖全部其余切片（扩展域 = mfg/hr/purchase/sales/assets/inventory/crm/quality/projects/cs/master-data/maintenance/contract/b2b/drp/logistics/aps/notify）。展开器须**逐报告读取 §7 存疑点清单全集**，逐存疑点登记为 A4.2.n 实体行。

  > 扩展域范围 = MA1 全部切片 A1.8-A1.51（44 个切片报告）。若某报告 §7 注明「无存疑点」，则该切片不产生 A4.2.n 行（方法论 §MA4：无存疑点的切片不产生实体行）。实际 §7 存疑点条目数须逐报告提取（Phase 1 完整枚举）。

  > 业财域范围 = finance MA1 切片（A1.1-A1.7），已由 A4.1 展开器覆盖（A4.1 done ✅，25 行 A4.1.1-A4.1.25）。本 A4.2 展开器不覆盖 finance 切片存疑点（去重，不重复展开）。

- **既有证据复用**（方法论 §去重协议）：既有 arm MA2 报告（`2026-07-2*-arm-ma2-*`）已证实的行为不重复验证；A4.2.n 实体行只对「MA1 报告标注为静态存疑、需运行时确认」的点展开。无存疑点的切片不产生实体行（方法论 §MA4）。

- **保护区域**：展开器为**只读收集 + roadmap 表追加**（读 MA1 报告 §7，追加 A4.2.n 行到 roadmap MA4 表；不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。后续 A4.2.n 验证若触及会计过账/ORM/数据删除行为探针，属各 A4.2.n 实体行的保护区域门控（§5），不在本展开器范围。

- **与 A4.1 范式一致性**：A4.2 展开器语义、流程、done 判据、输出格式与 A4.1 完全同构（§MA4 + §10 R1.0 范式）。差异仅在输入范围（A4.1=finance A1.1-A1.7 业财域 / A4.2=扩展域 A1.8-A1.51 其余切片）和编号前缀（A4.1.n / A4.2.n）。

- **剩余差距**：A4.2.n 实体行未追加 = MA4 里程碑的扩展域运行时验证队列缺口。本展开器解除此缺口后，mission driver 可逐 A4.2.n 实体行 DRAFT_PLANS → 验证。

## Goals

- 读取扩展域 MA1 切片报告（A1.8-A1.51）§7 存疑点清单**全集**，**逐存疑点**登记为 roadmap MA4 表内 A4.2.n 实体行（完整枚举，禁止抽样：每个 §7 存疑点恰好一行，跨报告同根因去重时显式标注合并依据）。
- 每条 A4.2.n 行含：来源切片 + §7 存疑点原文摘要 + 运行时验证方法（复用 `tests/e2e/` + `orchestration/_helper.ts` 原语 / 既有 JUnit / 临时探针）+ 预期证实/证伪的行为 + 触及保护区域标注（是/否 + 类别）。
- 展开完成后 A4.2 标记 done（全部实体行已追加；done 判据 = 展开完成，非验证完成）。
- 产出展开映射记录 `docs/audits/<执行时间戳>-rc-ma4-a4-2-ext-domain-expander.md`（存疑点 → A4.2.n 行映射 + 去重依据 + 计数自检）。

## Non-Goals

- **不执行运行时验证**（验证属后续各 A4.2.n 实体行，各自独立 plan + 独立 done）。
- **不展开业财域存疑点**（A4.1 展开器已覆盖 finance A1.1-A1.7，done ✅；本展开器只覆盖扩展域 A1.8-A1.51）。
- **不修改真相源**（product-scope/use-cases/owner doc 需求契约；§9 冻结）。roadmap MA4 表追加 A4.2.n 行是工作项追踪更新（非冻结真相源），属展开器既定动作。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读收集 + roadmap 表追加）。
- **不重复既有 arm MA2 已证实行为**（§去重协议）。

## Task Route

- Type: `verification or audit work`（展开器：存疑点清单 → 实体行的展开与登记；非实现变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 展开器范式 + §10 R1.0 同构 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2 工作项 + Work Item Details MA4）+ 扩展域 MA1 切片报告 A1.8-A1.51 §7 存疑点清单（输入）+ `docs/design/flow-overview.md` + `tests/e2e/orchestration/_helper.ts`（验证原语参考，用于标注 A4.2.n 行的验证方法）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。展开器需对存疑点做维度归类（运行时验证方法选择），复用其维度框架。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 展开器以读 MA1 报告 §7 + 标注验证方法为主（纯分析）。标注 A4.2.n 验证方法时引用既有 `tests/e2e/` + `orchestration/_helper.ts` 原语（不执行）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本展开器无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 存疑点收集、去重、A4.2.n 行展开

Status: planned
Targets: `docs/backlog/requirement-compliance-roadmap.md`（MA4 表追加 A4.2.n 行）；`docs/audits/<执行时间戳>-rc-ma4-a4-2-ext-domain-expander.md`（展开映射记录）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Add | Decision`
- Prereqs: MA1 done（扩展域 A1.8-A1.51 §7 存疑点清单已落盘）

- [ ] `Add` 读取扩展域 MA1 报告 A1.8-A1.51 §7 存疑点清单全集，逐存疑点提取（原文摘要 + 来源切片 + 来源报告锚点）。无存疑点的切片显式标注「无」（不产生行）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 跨报告去重：同根因同控制点的存疑点合并为一行（显式标注合并依据 + 涉及切片）；不同根因/不同控制点各一行。与 A4.1 已展开的 finance 存疑点（A4.1.1-A4.1.25）不重复（扩展域存疑点不归 finance 切片，天然不重叠；若跨域存疑点引用 finance 行为，以其来源扩展域报告为锚）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 每条存疑点展开为 roadmap MA4 表内 A4.2.n 实体行（编号 A4.2.1, A4.2.2…），每行含：来源切片 + §7 存疑点摘要 + 运行时验证方法（复用 E2E/_helper 原语 / JUnit / 临时探针）+ 预期证实/证伪行为 + 触及保护区域标注（是/否 + 类别）+ Skill（`multi-dimensional-audit-prompt`）。
      - Skill: none

Exit Criteria:

- [ ] 扩展域 A1.8-A1.51 §7 存疑点全集已提取，无遗漏（逐报告核对 §7 段落存在性，含「无」标注切片）
- [ ] A4.2.n 实体行已追加到 roadmap MA4 表，每行含完整字段；去重合并有显式依据

### Phase 2 - 展开映射记录 + 计数自检 + A4.2 done

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma4-a4-2-ext-domain-expander.md`（定稿）；roadmap A4.2 状态 → done
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成（A4.2.n 行已追加）

- [ ] `Add` 产出展开映射记录：存疑点 → A4.2.n 行映射表 + 去重依据 + 来源报告锚点 + 触及保护区域标注汇总。
      - Skill: none
- [ ] `Proof` 计数自检：A4.2.n 行数 = 扩展域 §7 存疑点去重并集数；逐报告核对（A1.8-A1.51 各报告的 §7 存疑点均已纳入或显式标注「无」）。
      - Skill: none
- [ ] `Proof` 过程纪律自检（§8 模板）：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline（本展开器无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（展开器不新建 finding，A4.2.n 验证结论才新建/复用）。
      - Skill: none
- [ ] `Add` 展开完成即 roadmap A4.2 状态标记 `done`（done 判据 = 全部 A4.2.n 行已追加，非验证完成）。
      - Skill: none

Exit Criteria:

- [ ] 展开映射记录已落盘，含存疑点 → A4.2.n 映射 + 去重依据 + 计数自检通过
- [ ] roadmap MA4 表 A4.2.n 行完整；A4.2 状态标记 done（展开完成）

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 `ses_02ba94965ffeWmbz2adx3ULlSG`，fresh session，未起草本计划）。全部 baseline 主张经实时仓库逐项核实**零伪**：A1.4 roadmap `todo` vs plan `completed` 证实 MA1 有效 done（解除 A4.2 Deps）；A4.1 done ✅ + 25 行 A4.1.1-A4.1.25 + 展开映射记录核实；A4.2 roadmap 条目逐字匹配；扩展域 MA1 报告 A1.8-A1.51 存在（43 份报告覆盖 44 切片，A1.45-46 合并）；§7 存疑点段落实测存在（A1.8/A1.28/A1.45-46/A1.51 抽查）；方法论 §MA4+§8+§10+§去重协议 + supporting refs 全在位。Rule 1/4/7/8/9/12 + anti-slack 全 PASS。展开器语义正确（done=展开完成非验证完成，对齐 A4.1/§10 R1.0）；与 A4.1 结构同构（Phase/Closure Gates/Deferred 同形）；A4.1 非重叠（finance vs 扩展域）显式。无 BLOCKER / 无 MAJOR。1 non-blocking MINOR（「44 切片报告」实际 43 份文件因 A1.45-46 合并，Phase 2 计数自检隐式处理，不影响正确性）。共识达成，转 active。

## Closure Gates

> 本计划为**展开器**（只读收集 + roadmap 表追加，无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = A4.2.n 行完整性（扩展域 §7 全集覆盖）+ 展开映射记录 + 计数自检 + 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.2.n 实体行已追加到 roadmap MA4 表（扩展域 A1.8-A1.51 §7 全集覆盖）；A4.2 状态 done（展开完成）
- [ ] 相关文档对齐：展开映射记录与方法论 §MA4 + §去重协议一致；A4.2.n 行字段与 roadmap MA4 工作项规范一致；与 A4.1 范式同构
- [ ] 已运行验证：计数自检（A4.2.n 行数 = 扩展域 §7 去重并集）+ §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A4.2.n 实体行的运行时验证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 展开器结果表面 = A4.2.n 行追加（展开完成即 done）。各 A4.2.n 的运行时验证属后续独立 plan + 独立 done（mission driver 逐项 DRAFT_PLANS → 验证）。触及会计过账/ORM/数据删除行为探针的 A4.2.n 行须 ask-first（§5 保护区域暂停协议）。本展开器闭环不阻塞于验证落地。
- Successor Required: yes（各 A4.2.n 实体行）

## Closure

Status Note: <关闭时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（fresh session）>
- Evidence: <关闭时填写>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
