# 2026-08-07-0300-1 rc-ma2-a2-1-2-finance-simplification-review finance MA2 已裁决简化/Deferred 复查

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A2.1（finance 会计保护区域简化复查）+ A2.2（finance 非保护区域简化复查）— 同 finance 组件，按 plan 指南规则 14 合并为单一 owner plan（先例 `2026-08-05-1400-1` A1.45+46 合并）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A2.1 + A2.2
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`docs/plans/2026-08-02-1530-2-existing-inventory-export.md`（M0.3 done，导出 A2.1/A2.2 全集 + §集成排序）、`docs/plans/2026-08-02-1815-1-rc-ma1-a1-4-finance-f4-bank-reconciliation.md`（MA1 finance 范式参照）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份 MA2 复查报告 + arm-index 登记。基线盘点的是被复查 finding 的关闭证据现状，**不修改任何代码/真相源**。

- **方法论契约就绪**：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4 Q1 真相源层级 +「显式人工批准记录」三判据 (i)/(ii)/(iii) / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议）已落盘（M0.1 done）。

- **复查全集已导出**（M0.3 done，`docs/audits/rc-existing-inventory.md`）：finance 域方案 B 关闭项 = **7 项**（A2.1 会计保护区域 6 项 + A2.2 非保护区域 1 项），分区完整性已校验（无重叠无遗漏）。逐项锚点：

  | # | Finding ID | 关闭方式标签（arm-index） | owner doc 锚点 | successor（→ MA3 A3.1） | 复杂度 |
  |---|-----------|-------------------------|---------------|----------------------|--------|
  | 1 | `P0-MA2-018` | `deferred` | `finance/posting-log.md §ErpFinVoucherBillR 索引与过账性能` | 重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK（非降级） | S |
  | 2 | `P1-MA2-001` | `方案 B 裁决（documented simplification，非降级 deferred）` | `purchase/returns.md §暂估应付冲减` + `finance/posting.md §GRNI 暂估冲回 documented simplification` | 方案 A GRNI 自动冲回 | S |
  | 3 | `P1-MA2-018` | `documented simplification` | `finance/period-close.md §已知简化「年初余额非累计」` | GL 余额维护引擎 | S |
  | 4 | `P1-MA2-019` | `作用域修复 + documented simplification 残留` | `finance/period-close.md §已知简化「辅助账跨年对账作用域」` | 累计余额对账（依赖 GL 余额） | S |
  | 5 | `P1-MA2-020` | `documented simplification` | `finance/period-close.md §已知简化「反结账审批（kill-switch successor）」` + `finance/state-machine.md §已知限制` | 完整反结账审批流（xwf；`2026-07-09-2330-1` 裁决浏览器层 NOT FEASIBLE） | S |
  | 6 | `P1-MA2-022` | `documented simplification` | `finance/period-close.md §已知简化「FX 重估无前期 reversal（IAS 21 残留风险）」` | 前期 FX 凭证期末自动 reversal + 期间过滤 | S |
  | 7 | `P1-MA1-016` | `resolved（永久只读豁免，登记于 data-dependency-matrix.md §9）` | `architecture/data-dependency-matrix.md §9` | 永久接受（无 successor） | S |

- **§4 三判据复查对象**（每项 finding 须逐判据核证证据）：
  - (i) **plan 含独立 plan-audit 通过记录**：核查关闭该 finding 的既有 arm plan（`docs/plans/2026-07-*`）的 `Draft Review Record` / `## Closure` 是否含独立子代理/审查者通过证据。
  - (ii) **owner doc 显式 documented simplification 标注且经人工批准**：核查上表 owner doc 锚点段落是否存在显式标注 + 批准来源可追溯（git log / commit message / 讨论文档；**AI 自写标注不算**）。
  - (iii) **product-scope 范围裁剪登记**：核查 `docs/requirements/product-scope.md` 是否将该功能列入「不在范围/后续阶段」+ 理由 + 影响面 + 批准人。
  - 判据应用顺序 (i)→(ii)→(iii)；判据三仅当 (i)/(ii) 均不成立时兜底。

- **Q4 张力点（最高优先）**：`P0-MA2-018`（凭证幂等键字面 UK）三重张力——(a) 它是 **P0**；(b) roadmap §当前基线「P0 deferred 边界声明」将其列为既有 arm-index P0 deferred；但 (c) **Q4 裁决=(a)「P0/P1 必须实现，禁止方案 B 无例外」+ 方法论 §5「技术不可行项须更深设计变更（重构 billR 加判别列 + UK）非退缩到方案 B」**。MA2 复查结论几乎必然 = 「Q4 强制实现」→ 重开入 **MR1**（R1.0 展开为 RC-R1.n）。注意：依方法论 §2「既有 arm-index P0 deferred 边界」+ roadmap §当前基线，**既有 P0 deferred 经 MA2 重新分级后入 MR1（非 MR0 即时通道；MR0 仅对本审计新发现的 P0）**。

- **P1-MA2-020（反结账审批）已知阻塞**：successor「完整反结账审批流」依赖浏览器层 xwf 审批路径，`2026-07-09-2330-1-use-workflow-browser-e2e-feasibility.md` 裁决 xwf 浏览器层 NOT FEASIBLE。Q4=(a) 无「技术不可行」例外——若 MA2 裁决为静默降级须重开 MR1，则修复须找更深设计变更（非 xwf 的替代审批机制），不退缩到方案 B。本复查须核实该简化是否有 §4 三判据支撑（有 → P2 successor；无 → 重开 MR1）。

- **保护区域**：本复查为**只读审计**（读 plan/owner doc/product-scope/arm-index/git log，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。复查裁决的重开项（finding 的修复）**不在本计划实施**——按方法论 §10，经 MR1（R1.0 展开 RC-R1.n）或 MR0（本审计新发现 P0 时）；触及 ORM 结构变更（如 P0-MA2-018 重构 billR + UK）或会计过账逻辑的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A2.1+A2.2 复查报告缺失 = MR1（R1.0，Deps=MA1-MA4 done）该域重开项证据缺口来源。本计划产出 A2.1+A2.2 复查报告并登记/重开 finding，解除其在 MR1 链路的该域证据缺口。

## Goals

- 产出 finance MA2 复查报告 `docs/audits/<执行时间戳>-rc-ma2-a2-1-2-finance-simplification-review.md`，含方法论 §6 **9 段全部内容**（MA2 适配：段 1=方案 B 关闭项清单 + 锚点；段 2=§4 三判据逐项证据；段 3/4=既有行为证据（复用 arm MA2/MA3 报告）；段 5=复查结论「有意设计 vs 静默降级」+ 是否重开 MR1；段 6=arm-index 衔接；段 7=静态存疑点（供 MA4）；段 8=过程纪律自检；段 9=与既有审计差异增量）。
- 对 7 项 finding **逐项**应用 §4 三判据核证（完整枚举，禁止抽样）：每项给出 (i)/(ii)/(iii) 证据核证结果 + 复查结论（`有意设计（保留 P2 successor）` / `静默降级（重开 MR1）`）+ 命中判据编号。
- 对重开项登记双向可追溯（finding ID ↔ MR1 R1.0 预留展开行）：重开的 P1 → MR1（R1.0 展开为 RC-R1.n）；`P0-MA2-018` 经 Q4 强制 → MR1（非 MR0，既有 P0 deferred 边界）；本审计新发现 P0（若有）→ MR0 即时通道。
- 报告产出即更新 `docs/audits/arm-index.md`（重开项在既有 finding 行追加 RC 复查注记 + 重开标记；新根因/新控制点新建 `P*-RC-xxx` 并入分区）。

## Non-Goals

- **不实施修复**（修复属 MR0 即时通道 / MR1 R1.0 展开的 RC-R1.n；本计划是审计，结果表面 = 一份报告 + arm-index 登记）。
- **不修改真相源**（product-scope / finance owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实；§9 冻结条款——分歧/复查结论记入报告，不直改真相源；owner doc 的 §已知简化段落是审查对象不是修改对象）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读审计）。
- **不复查其他域**（A2.3 mfg / A2.8 扩展域 / A2.9 跨域各自独立 plan；A2.4-A2.7 经 M0.3 导出为 0 项方案 B，可直接标 done，不属本计划）。
- **不裁决 successor 是否回队**（successor 触发条件复查属 MA3 A3.1，独立 plan；本计划只复查「方案 B 关闭裁决本身是否正当」，与 successor 的两面关系按方法论 §MA2↔MA3 协作——关闭裁决归 A2.x，successor 触发条件归 A3.x，交叉引用不重复）。
- **不重跑既有 arm MA2 行为审计**（§去重协议：既有 `2026-07-2*-arm-ma2-*` 已证实行为直接引用，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`（已裁决简化/Deferred 关闭项的 §4 三判据复查；非实现变更、非需求澄清）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§2/§4/§5/§6/§7/§8/§9 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A2.1/A2.2 工作项 + Work Item Details MA2）+ `docs/audits/rc-existing-inventory.md`（A2.1/A2.2 全集 + §集成排序 + §对账差异登记 #1/#2）+ `docs/audits/arm-index.md`（finding 关闭事实 + 关闭方式标签）+ finance owner docs（`posting.md`/`posting-log.md`/`period-close.md`/`state-machine.md`/`purchase/returns.md`，作 §4(ii) 审查对象）+ `docs/requirements/product-scope.md`（§4(iii) 审查对象）+ 关闭各 finding 的既有 arm plans（§4(i) 审查对象）。
- Skill Selection Basis: `Skill: docs/skills/open-ended-audit-prompt.md`（roadmap MA2 全部 A2.x 指定）。该技能定义开放式审计 prompt 范式，适合「有意设计 vs 静默降级」判定（无固定检查清单、需逐项证据裁决）；其必需输入（arm-index finding + owner doc + product-scope + 关闭 plan）均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 复查以读 plan/owner doc/product-scope/arm-index/git log 为主（纯分析）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更故无回归风险，仅记录 actual vs baseline）。§4(i) 人工批准痕迹核证需 `git log` / 关闭 plan 的 audit 记录，不引入新依赖。

## Execution Plan

### Phase 1 - A2.1 会计保护区域 6 项 §4 三判据复查

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma2-a2-1-2-finance-simplification-review.md`（新建，先填段 1-5 的 A2.1 部分）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.3 done（方法论契约 + 复查全集导出就绪）

- [x] `Proof` 对 `P0-MA2-018`（凭证幂等键字面 UK）逐判据核证 §4 (i)/(ii)/(iii) 证据：(i) 查关闭该 finding 的 arm plan 的 audit 通过记录；(ii) 查 `posting-log.md §ErpFinVoucherBillR 索引` 的 documented simplification 标注 + 人工批准痕迹（git log/commit/讨论）；(iii) 查 product-scope 是否有范围裁剪。记录三判据核证结果。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Proof` 对 `P1-MA2-001`（GRNI 自动冲回）/ `P1-MA2-018`（年初余额非累计）/ `P1-MA2-019`（辅助账跨年对账作用域）/ `P1-MA2-020`（反结账审批）/ `P1-MA2-022`（FX 重估无前期 reversal）逐项同 §4 三判据核证。`P1-MA2-020` 额外核实 `2026-07-09-2330-1`（xwf NOT FEASIBLE）对 Q4 修复义务的影响（无例外通道→须更深设计变更）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Decision` 对 A2.1 6 项逐项给出复查结论（`有意设计（保留 P2 successor）` / `静默降级（重开 MR1）`），列明命中 §4 判据编号 + 三源对照（arm-index 关闭标签 vs owner doc 标注 vs product-scope）。`P0-MA2-018` 经 Q4 强制 → 重开 MR1（非 MR0，既有 P0 deferred 边界；依方法论 §2 + roadmap §当前基线）；其余 P1 按 §4 三判据：三判据满足其一 → 有意设计（P2 successor）；均不满足 → 静默降级重开 MR1。
      - Skill: `docs/skills/open-ended-audit-prompt.md`

Exit Criteria:

- [x] 报告段 1（7 项清单 + 锚点）+ 段 2（§4 三判据逐项证据，A2.1 的 6 项）已落盘，每项含 (i)/(ii)/(iii) 核证结果（非悬空「待查」）
- [x] A2.1 的 6 项均有复查结论（有意设计/静默降级）+ 命中判据编号；`P0-MA2-018` Q4 强制结论明确

### Phase 2 - A2.2 非保护区域 1 项 + 报告定稿 / arm-index / 重开登记

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma2-a2-1-2-finance-simplification-review.md`（补段 5 的 A2.2 部分 + 段 6-9，报告定稿）；`docs/audits/arm-index.md`（重开项注记 + 新 RC finding 分区）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（A2.1 6 项结论已出）

- [x] `Proof` 对 `P1-MA1-016`（finance→assets 永久只读豁免）逐判据核证 §4 (i)/(ii)/(iii)：(i) 关闭 plan audit 记录；(ii) `data-dependency-matrix.md §9` 豁免登记 + 人工批准痕迹；(iii) product-scope 范围裁剪。核实「永久只读豁免」归类是否恰当（M0.3 §对账差异登记 #1：标签字面非三标签之一但实质等同，MA2 须核实归类）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Decision` **复用 or 新增 裁决**（§7）：产出 finding 注记前 grep `arm-index.md` finance 同域同控制点（这 7 项均为既有 arm finding，本复查原则上**复用既有 ID**追加 RC 注记；仅当复查发现**新根因/新控制点/新维度**才新建 `P*-RC-xxx`，须列明差异依据）。禁止未经比对直接新建。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Add` 报告段 6 与 arm-index 衔接段：列明每项的复用/新增裁决 + 双向可追溯（finding ID ↔ MR1 R1.0 预留展开行）。
      - Skill: none
- [x] `Add` 报告段 7 静态存疑点清单（供 MA4 A4.1 展开）：登记复查中 L5 无法静态定论、需运行时确认的点（无则注明「无」）。
      - Skill: none
- [x] `Proof` 报告段 8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**。
      - Skill: none
- [x] `Add` 报告段 9 与既有审计差异增量声明：声明复用 `2026-07-2*-arm-ma2-*`（finance 状态机/链路行为）/ `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（doc↔code drift）已证实证据，列明本复查只补的「需求契约 vs 方案 B 关闭裁决正当性」差异。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：重开项在既有 finding 行追加「RC MA2 复查：静默降级，重开 MR1」注记 + 重开标记；新 `P*-RC-xxx`（若有）入对应分区。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检（§6 段落完整性自检）：落盘前自查段 1-9 全部存在；缺任一段即回到 Phase 补齐。
      - Skill: none

Exit Criteria:

- [x] 报告段 5（A2.2 的 P1-MA1-016 结论）+ 段 6-9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 重开项已写入 `arm-index.md`（既有行注记或新 RC 分区）；静态存疑点清单已登记（供 A4.1 展开）
- [x] 段 8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02ca89430ffefLTAmFIiMULYGi，fresh session，未起草本计划）。10 项检查 A-J 全 PASS：格式完整、Deps 正确（A2.1/A2.2 Deps=0.2+0.3 均 done）、规则 14 合并成立（同 finance 组件，引用 `2026-08-05-1400-1` A1.45+46 同域合并先例，单一结果表面=一份 MA2 报告+arm-index 登记）、Baseline 准确（7 项 finding 逐项对 rc-existing-inventory 核证 ID/关闭标签/owner-doc 锚点一致；P0-MA2-018 Q4 三重张力正确框定；**MR1-vs-MR0 路由正确**：既有 P0 deferred→MR1 非 MR0，引用方法论 §2 + roadmap §当前基线，4 处一致）、范围清晰（只读审计，修复→MR0/MR1，successor→A3.1，他域分离，A2.4-A2.7 空集）、方法论对齐（§4 三判据 (i)→(ii)→(iii) 顺序 + (iii) 兜底；§6 MA2 适配段 1=方案B清单；§MA2↔MA3 协作）、反松弛合规（Closure Gates 移除 build/test 有据）、Skill 就绪、item typing 合规、无矛盾。特别核实点全确认：P0-MA2-018→MR1（非 MR0）正确；P1-MA2-020 xwf 阻塞 + Q4 无技术不可行例外已 ack；§4(ii) AI 自写标注不算人工批准已写明。无阻塞。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控——复查报告产出不触发编译或测试。验证 = 报告 9 段完整性 + 7 项逐项 §4 三判据核证 + finding arm-index 衔接 + 段 8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。（段 8 含 checker 实测记录，但 checker 是 reporter 非门控；门控真值在 CI workflow。）

- [x] 范围内行为完成：A2.1+A2.2 报告 9 段齐全 + 7 项逐项 §4 三判据结论 + 重开项登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §2/§4/§5/§6/§7 + §去重协议一致；与 rc-existing-inventory A2.1/A2.2 全集 + §对账差异登记 #1/#2 一致
- [x] 已运行验证：报告 9 段完整性自检 + 段 8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。复查裁决的重开项（finding 的修复）按方法论 §10 经 MR1（R1.0 展开为 RC-R1.n，P1 批量）/ MR0（本审计新发现 P0 即时通道）实施；`P0-MA2-018` 经 Q4 强制重开入 MR1（既有 P0 deferred 边界，非 MR0）；触及 ORM 结构变更（如 billR 重构 + UK）或会计过账逻辑的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本复查闭环不阻塞于修复落地。
- Successor Required: yes（MR0/MR1 按本报告重开项交叉引用展开修复行）

## Closure

Status Note: finance MA2 复查（A2.1 + A2.2）完成。产出报告 `docs/audits/2026-08-06-1400-rc-ma2-a2-1-2-finance-simplification-review.md`（9 段齐全）+ arm-index.md 7 项 finding 行追加 RC MA2 复查注记。复查结论：**1 项重开 MR1**（`P0-MA2-018`，P0，Q4 强制，§4 三判据均不成立→静默降级，修复须更深设计变更非字面 UK，触及 ORM+会计保护区域须 ask-first）；**6 项有意设计（保留 P2 successor）**（5 项 A2.1 P1 + 1 项 A2.2 P1，§4(i) 独立 plan-audit 通过记录均成立）；本审计新发现 P0 = 0（无 MR0 即时通道）。纯只读审计——零生产代码变更，§9 真相源冻结遵守（arm-index 注记为追加非关闭事实改写）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh context，`ses_02c9e4db6ffeHKz4igoFDI29Oc`，新会话，未执行本 plan，仅 Read/Grep/Bash）
- Verdict: **APPROVED**（计划可正式关闭）
- 核实项（live repo 复核，非盲信 `[x]`）：
  1. §4 三判据证据正确性 PASS——P0-MA2-018 plan-audit REJECT/BLOCK（非通过）→ §4(i) 正确判不成立；6 项 P1 关闭 plan 均含 Draft Review Record（独立子代理 ses_*）+ Closure Audit Evidence PASS → §4(i) 正确判成立。
  2. MR1-vs-MR0 路由 PASS——roadmap §当前基线 + 方法论 §2 确认既有 arm-index P0 deferred 经 MA2 重新分级入 MR1（非 MR0）；报告 §5.2 路由正确（4 处一致引用）。
  3. 报告 9 段完整性 PASS——段 1-9 全部存在，§8 含 checker actual vs baseline 表 + 独立性 + 交叉去重声明。
  4. arm-index 注记完整性 PASS——7 项 finding 行均含「RC MA2 复查」+ 段落交叉引用；表格行 well-formed（pipe 计数一致，均以 `|` 结尾）；无新 `P*-RC-xxx`（7 项全部复用既有 ID）。
  5. §9 真相源冻结 PASS——arm-index 变更为追加注记（原关闭事实逐字保留，RC 注记以 `；` 分隔追加）；未改 product-scope / owner doc 需求契约段落。
  6. P1-MA2-020 Q4 路由 PASS——xwf NOT FEASIBLE 张力已 ack，successor 修复路由 A3.1「非 xwf 替代机制」，非静默方案 B。
  7. Five-point 一致性 PASS——Plan Status=completed / 2 Phase Status=completed / Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据完整。
  8. 零生产代码变更 PASS——`git status` 仅 docs/ 变更，零 `module-*/` / `.java` / `.orm.xml` / `api.xml` 变更。

Follow-up:

- `P0-MA2-018` 重开 MR1：R1.0 展开为 RC-R1.n（修复须 ask-first ORM + 会计保护区域 + 独立 plan-audit；方向 = 反范式化判别列 acctSchemaId/postingType/isReversed 到 billR/voucher + 复合 UK，非字面 UK 非降级）。
- 6 项 P2 successor 复查归 MA3 A3.1（finance 域 successor 触发条件复查，独立 plan）。
