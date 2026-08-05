# 2026-08-07-0300-2 rc-ma3-a3-1-finance-successor-review finance MA3 successor 追踪完整性与回队复查

> Plan Status: active
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: A3.1（finance 域 successor 复查）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A3.1
> Related: `docs/plans/2026-08-02-1530-2-existing-inventory-export.md`（M0.3 done，导出 successor 三源对账清单 finance 分组）、`2026-08-07-0300-1-rc-ma2-a2-1-2-finance-simplification-review.md`（A2.1/A2.2 关闭裁决复查，successor 两面关系——关闭裁决归 A2.x、successor 触发条件归 A3.x）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份 MA3 复查报告 + arm-index/backlog 登记更新。基线盘点的是 successor 触发条件的现状，**不修改任何代码/真相源**。

- **方法论契约就绪**：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）已落盘（M0.1 done）。

- **successor 三源对账全集已导出**（M0.3 done，`docs/audits/rc-existing-inventory.md` §successor 三源对账清单 — finance 域分组）：finance 域 design-level successor 去重并集 = **8 项**，逐项含三源覆盖标记（S1=arm-index 行内 / S2=owner doc 内嵌 / S3=backlog README）、触发条件摘要、已满足?、当前归属、复杂度：

  | # | successor 项 | 三源覆盖 | 触发条件摘要 | 已满足? | 复杂度 |
  |---|-------------|---------|-------------|---------|--------|
  | 1 | GRNI 正向 receive→invoice 自动冲回（方案 A） | S1+S2 | 双向钩子[approve 红冲+reverseApprove 反冲回]+部分开票覆盖判定+跨期语义；inventory 域 `repostPurchaseInput` SPI 缺失 | ❌ 未满足（SPI 缺失） | S |
  | 2 | GL 余额维护引擎（opening/closing） | S1+S2 | 补过账引擎 postVoucher 时维护 opening/closing 余额 | ❌ 未满足 | S |
  | 3 | 累计余额对账（辅助账跨年） | S1+S2 | GL 余额维护 successor 落地后 | ❌ 未满足（依赖 #2） | S |
  | 4 | 反结账完整审批流（xwf） | S1+S2 | 浏览器层 xwf 审批路径落地 | ❌ 未满足（`2026-07-09-2330-1` 裁决 xwf 浏览器层 NOT FEASIBLE） | S |
  | 5 | FX 重估前期 reversal + 期间过滤（IAS 21 完整语义） | S1+S2 | IAS 21 完整语义需求 + config-gated 关闭默认 | ❌ 未满足 | S |
  | 6 | 凭证幂等键字面 UK 方向 A/B/C/D | S1 | 重构 billR 加判别列（acctSchemaId/postingType/isReversed）+ 对应 UK | ❌ 未满足（须 ask-first ORM） | S |
  | 7 | 多币种全域源币金额迁移（其余域 Provider） | S2 | 各域启用多币种业务路径时 | ❌ 未满足 | S |
  | 8 | 凭证 `reversedVoucherId` 双向回链 | S2 | 报表需求驱动时 | ❌ 未满足 | S |

  > 注：#6（P0-MA2-018 successor）与 A2.1 复查的同一 finding 两面——关闭裁决本身归 A2.x 复查，successor 触发条件归本 A3.x 复查，交叉引用不重复。

- **复查四项任务**（roadmap MA3 Work Item Details）：逐项核对 ① 触发条件是否已满足（已满足 → 回队 MA1/R1.0）；② 是否该回队（回到审计 / R1.0 修复 / backlog README）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核（防「已登记但从未触发」）。

- **已知结构性约束**：successor #2（GL 余额维护引擎）是 #3（累计余额对账）的共同前置；successor #1（GRNI）依赖 inventory 域 `repostPurchaseInput` SPI（跨域，A3.2 范畴）；successor #4（反结账审批）有已知 NOT FEASIBLE 阻塞（xwf）。owner doc 内嵌但 arm-index 无独立行的 successor（#7/#8，仅 S2 覆盖）须纳入避免遗漏（M0.3 §对账差异登记 #3）。

- **Q4 修复义务边界**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。`P0-MA2-018` successor（#6）经 Q4 强制——其触发条件实质为「须更深设计变更」，Q4 无技术不可行例外，故**应回队 MR1**（与 A2.1 复查结论交叉一致）。
- **finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x）。故 #4（反结账审批 successor）successor 触发条件（xwf 落地）未满足 → successor 维持 backlog；但若 A2.1 裁决其关闭为「静默降级」，则该 **finding** 经 A2.1 重开入 MR1（Q4 强制，与 successor 是否回队独立）。即：successor 回队与否（A3.x）≠ finding 是否修复（A2.x→MR1），两者各自裁决、交叉引用不冲突。

- **保护区域**：本复查为**只读审计**（读 arm-index/owner doc/backlog README/git log，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。回队项的修复**不在本计划实施**——经 MR1（R1.0 展开）；触及 ORM/会计过账的修复行须 ask-first（§5）。

- **剩余差距**：A3.1 复查报告缺失 = MR1（R1.0）该域 successor 回队决策证据缺口来源。本计划产出 A3.1 复查报告并登记回队决策，解除其在 MR1 链路的该域证据缺口。

## Goals

- 产出 finance MA3 复查报告 `docs/audits/<执行时间戳>-rc-ma3-a3-1-finance-successor-review.md`，含方法论 §6 **9 段全部内容**（MA3 适配：段 1=successor 三源对账清单 finance 分组；段 2=逐项四任务核证[触发条件已满足?/是否回队/补登记/README 覆盖]；段 3/4=既有行为证据；段 5=复查结论「回队 MR1 / 维持 backlog successor / 补登记」；段 6=arm-index 衔接；段 7=静态存疑点；段 8=过程纪律自检；段 9=与既有审计差异增量）。
- 对 8 项 successor **逐项**完成四任务核证（完整枚举，禁止抽样）：每项给出触发条件状态（已满足/未满足 + 证据）+ 回队决策（回队 MR1 / 维持 backlog / 补登记）+ README 既有行覆盖复核结果。
- 对回队项登记双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）；`P0-MA2-018` successor（#6）回队决策与 A2.1 复查结论交叉一致。
- 报告产出即更新 `docs/audits/arm-index.md`（successor 回队注记）。

## Non-Goals

- **不实施修复**（修复属 MR1 R1.0 展开的 RC-R1.n；本计划是审计）。
- **不复查方案 B 关闭裁决本身**（属 A2.x；本计划只复查 successor 触发条件；与 A2.x 的两面关系按 §MA2↔MA3 协作交叉引用）。
- **不修改真相源**（product-scope / finance owner doc / backlog README 的既有 successor 登记；§9 冻结——回队决策记入报告 + arm-index，不直改 backlog README；README 覆盖差异记入报告交后续 backlog 维护）。
- **不复查其他域 successor**（A3.2-A3.5 各自独立 plan）。
- **不重跑既有 arm 审计**（§去重协议：既有行为证据直接引用）。

## Task Route

- Type: `verification or audit work`（successor 触发条件完整性 + 回队复查；非实现变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§4/§5/§6/§7/§8/§9 + §MA2↔MA3 协作 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A3.1 工作项 + Work Item Details MA3）+ `docs/audits/rc-existing-inventory.md`（successor 三源对账清单 finance 分组 + §对账差异登记 #3）+ `docs/audits/arm-index.md`（successor 行内声明）+ finance owner docs（`posting.md`/`period-close.md`/`state-machine.md`/`costing-methods.md`，successor 内嵌段落 S2）+ `docs/backlog/README.md`（S3 既有追踪行）。
- Skill Selection Basis: `Skill: docs/skills/open-ended-audit-prompt.md`（roadmap MA3 全部 A3.x 指定）。该技能适合「触发条件是否已满足 + 是否该回队」的开放式逐项裁决。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 复查以读 arm-index/owner doc/backlog README/git log 为主（纯分析）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本审计无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 8 项 successor 四任务逐项核证

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-1-finance-successor-review.md`（新建，先填段 1-5）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.3 done（方法论契约 + successor 三源对账导出就绪）

- [ ] `Proof` 对 8 项 successor 逐项核证任务①②③：① 触发条件是否已满足（grep 实仓代码/config/SPI 验证，如 #2 GL 余额引擎查 postVoucher 路径是否维护 opening/closing、#1 GRNI 查 inventory `repostPurchaseInput` SPI 是否存在、#6 查 billR 是否已加判别列）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog；#6 经 Q4 强制回队 MR1）；③ 无触发条件的补登记（owner doc 内嵌 successor #7/#8 若 arm-index 无独立行须补登记）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [ ] `Proof` 任务④ `docs/backlog/README.md` 既有行覆盖与正确性复核：grep backlog README finance successor 行，逐项核实覆盖（防「已登记但从未触发」）+ 正确性（触发条件描述与实仓一致）。差异记入报告（不回写 README）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [ ] `Decision` 对 8 项逐项给出复查结论（`回队 MR1` / `维持 backlog successor` / `补登记`），列明触发条件状态证据 + 三源覆盖 + 与 A2.x 关闭裁决的交叉关系（#1 GRNI ↔ P1-MA2-001 / #2↔P1-MA2-018 / #3↔P1-MA2-019 / #4↔P1-MA2-020 / #5↔P1-MA2-022 / #6↔P0-MA2-018）。#2/#3 结构性约束（#2 是 #3 前置）须在结论中显式标注回队顺序依赖。
      - Skill: `docs/skills/open-ended-audit-prompt.md`

Exit Criteria:

- [ ] 报告段 1（8 项三源对账清单）+ 段 2（逐项四任务核证）已落盘，每项含触发条件状态 + 回队决策 + README 覆盖复核（非悬空「待查」）
- [ ] #6 回队决策与 A2.1 复查结论交叉一致（Q4 强制回队 MR1）

### Phase 2 - 报告定稿 / arm-index / 回队登记

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-1-finance-successor-review.md`（补段 6-9，报告定稿）；`docs/audits/arm-index.md`（successor 回队注记）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（8 项结论已出）

- [ ] `Decision` **复用 or 新增 裁决**（§7）：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC 注记；仅当发现新 successor（owner doc 内嵌但 arm-index 无行，如 #7/#8）才新建/补登记，须 grep 后裁决。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [ ] `Add` 报告段 6 与 arm-index 衔接段：列明每项的复用/补登记裁决 + 双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）。
      - Skill: none
- [ ] `Add` 报告段 7 静态存疑点清单（供 MA4 A4.1 展开）：登记复查中需运行时确认的点（无则注明「无」）。
      - Skill: none
- [ ] `Proof` 报告段 8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告段 9 与既有审计差异增量声明：声明复用既有 arm 审计（successor 声明源自 arm-index）+ 本复查只补的「触发条件是否已满足 + 回队决策」差异。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：回队项在既有 finding/successor 行追加「RC MA3 复查：触发条件已满足→回队 MR1」注记；补登记项（#7/#8 若适用）入对应分区。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查段 1-9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告段 6-9 已落盘，9 段齐全；successor 复用/补登记裁决均有 arm-index grep 依据
- [ ] 回队项已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [ ] 段 8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02ca8607bffecnDtd7VtZc8NYS，fresh session，未起草本计划）。8 项 finance successor 逐行对 rc-existing-inventory §finance successor 分组核证（GRNI/GL 余额/累计余额/反结账/FX/UK/多币种/reversedVoucherId 全匹配，含三源覆盖 S1/S2 标记 + 结构性约束 #2→#3 前置 + #1 跨域 SPI 依赖 A3.2 范畴）；6 个 two-faces 交叉引用（#1↔P1-MA2-001 … #6↔P0-MA2-018）全匹配且未重审关闭裁决（边界正确）；#6 Q4-forced MR1 路由独立成立；四任务复查 + 9 段 MA3 适配正确；item typing/skill/deps(0.3 done)/anti-slack 全 PASS。采纳非阻塞建议：补充「finding 路由（A2.x→MR1）vs successor 触发条件路由（A3.x→backlog）」区分（已修订入 Current Baseline），明确 #4 successor 维持 backlog 不与其 finding 经 A2.1 重开冲突。#7/#8 补登记以实仓 grep 为准（§7 纪律，非松弛）。`## Closure` 段关闭时补（与 A1.4 范式一致）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 8 项逐项四任务核证 + successor arm-index 衔接 + 段 8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A3.1 报告 9 段齐全 + 8 项逐项四任务结论 + 回队项登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §4/§5/§6/§7 + §MA2↔MA3 协作 + §去重协议一致；与 rc-existing-inventory finance successor 分组 + §对账差异登记 #3 一致；#6 与 A2.1 结论交叉一致
- [ ] 已运行验证：报告 9 段完整性自检 + 段 8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 回队项的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。回队项的修复按方法论 §10 经 MR1（R1.0 展开为 RC-R1.n）实施；触及 ORM（如 #6 billR 重构 + UK）/ 会计过账逻辑（如 #2 GL 余额引擎 / #5 FX reversal）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本复查闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告回队项交叉引用展开修复行）
