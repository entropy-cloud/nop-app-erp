# 2026-08-06-0442-3 rc-ma3-a3-3-sal-ast-prj-qa-successor-review MA3 successor 追踪完整性与回队复查（sales+assets+projects+quality）

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A3.3（sales + assets + projects + quality 域 successor 复查）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A3.3
> Related: `docs/plans/2026-08-02-1530-2-existing-inventory-export.md`（M0.3 done，导出 successor 三源对账清单 sal+ast+prj+qa 分组 + §对账差异登记 #5 实现修复项 successor 残留注记）、`docs/plans/2026-08-07-0300-2-rc-ma3-a3-1-finance-successor-review.md`（A3.1 done，范式参照）、`docs/plans/2026-08-06-0442-2-rc-ma3-a3-2-mfg-inv-pur-successor-review.md`（A3.2 同批，MA3 域分组连续复查）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份 MA3 复查报告 + arm-index/backlog 登记更新。基线盘点的是 successor 触发条件的现状，**不修改任何代码/真相源**。

- **方法论契约就绪**：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）已落盘（M0.1 done）。

- **successor 三源对账全集已导出**（M0.3 done，`docs/audits/rc-existing-inventory.md` §successor 三源对账清单 — sal+ast+prj+qa 分组）：design-level successor 去重并集 = **5 项**，逐项含三源覆盖标记（S1=arm-index 行内 / S2=owner doc 内嵌 / S3=backlog README）、触发条件摘要、已满足?、当前归属、复杂度：

  | # | successor 项 | 域 | 三源覆盖 | 触发条件摘要 | 已满足? | 复杂度 |
  |---|-------------|----|---------|-------------|---------|--------|
  | 1 | 订单维度核销（receipt prepayment against order） | sales | S1 | owner doc §L3 订单+发票双维度语义恢复（P2-MA2-013 owner doc 标注本期仅发票维度） | ❌ 未满足 | A |
  | 2 | 资产 IDLE 闲置状态机迁移 + 折旧扩展 | assets | S1 | 资产暂停/恢复业务上线时（P1-MA2-061 owner doc Deferred：IN_SERVICE↔IDLE 无 writer + 折旧引擎只查 IN_SERVICE） | ❌ 未满足 | A |
  | 3 | 工时单 approve/reject + 工时归集（projects/cost-collection） | projects | S1 | projects 工时归集 successor（P1-MA2-043 owner doc Deferred：工时单 APPROVED/REJECTED 死状态 + 工时成本凭证跨域过账悬挂同型） | ❌ 未满足 | S |
  | 4 | 业务单据作废联动取消质检单 | quality | S1 | quality 域 cancel 回调接线（P1-MA2-064 owner doc §4 Deferred） | ❌ 未满足 | A |
  | 5 | employee-id 行过滤（quality inspectorId / maintenance assignedTo） | quality(+mnt 跨域) | S1 | ErpMdEmployee 增 userId 列 + 解析器（P1-MA6-002 R3.4 successor，**ask-first ORM**） | ❌ 未满足 | A |

  > 注：#5 跨 quality + maintenance 两域（M0.3 将其归 sal+ast+prj+qa 分组即本 A3.3，因主投影 quality inspectorId；maintenance assignedTo 为同根因同控制点合并）。本计划须覆盖两域投影（不重复登记，grep arm-index 同控制点后裁决）。

- **§对账差异登记 #5（实现修复项 successor 残留）**：多项 finding 经 `resolved (R*.n done)` 实现修复关闭，但其 owner doc/arm-index 注记仍保留 successor 触发条件（如 P1-MA2-061 IDLE / P1-MA2-064 质检联动）。这些**不属 MA2 方案 B 复查**（已实现修复），但其 successor **属 MA3 复查**——本计划已按 M0.3 正确分流纳入上表。

- **复查四项任务**（roadmap MA3 Work Item Details）：逐项核对 ① 触发条件是否已满足（已满足 → 回队 MA1/R1.0）；② 是否该回队（回到审计 / R1.0 修复 / backlog README）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核（防「已登记但从未触发」）。

- **已知结构性约束**：
  - #3（工时单/工时归集）：复杂度 S（projects 工时成本凭证跨域过账），与 finance/hr/mfg/assets/qa 同型 tryPost 吞异常悬挂根因（P1-MA2-068 等），MA3 须核实 successor 是否独立于已修复的过账悬挂路径。
  - #5（employee-id 行过滤）：触及 ORM（ErpMdEmployee 增 userId 列），ask-first 保护区域；MA3 只裁决 successor 触发条件是否回队，修复实施时的 ORM 变更须 ask-first。
  - #1（订单维度核销）：P2-MA2-013 watch-only，owner doc 标注「本期仅发票维度」——MA3 须核实「订单+发票双维度」successor 触发条件（报表/核销需求驱动）是否已满足。

- **Q4 修复义务边界**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。
- **finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x）。即：successor 回队与否（A3.x）≠ finding 是否修复（A2.x→MR1），两者各自裁决、交叉引用不冲突。

- **保护区域**：本复查为**只读审计**（读 arm-index/owner doc/backlog README/git log，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。回队项的修复**不在本计划实施**——经 MR1（R1.0 展开）；触及 ORM（如 #5 ErpMdEmployee userId 列）的修复行须 ask-first（§5）。

- **剩余差距**：A3.3 复查报告缺失 = MR1（R1.0）该域 successor 回队决策证据缺口来源。本计划产出 A3.3 复查报告并登记回队决策，解除其在 MR1 链路的该域证据缺口。

## Goals

- 产出 MA3 复查报告 `docs/audits/<执行时间戳>-rc-ma3-a3-3-sal-ast-prj-qa-successor-review.md`，含方法论 §6 **9 段全部内容**（MA3 适配：段 1=successor 三源对账清单 sal+ast+prj+qa 分组；段 2=逐项四任务核证[触发条件已满足?/是否回队/补登记/README 覆盖]；段 3/4=既有行为证据；段 5=复查结论「回队 MR1 / 维持 backlog successor / 补登记」；段 6=arm-index 衔接；段 7=静态存疑点；段 8=过程纪律自检；段 9=与既有审计差异增量）。
- 对 5 项 successor **逐项**完成四任务核证（完整枚举，禁止抽样）：每项给出触发条件状态（已满足/未满足 + 证据，grep 实仓代码/config/ORM 字段）+ 回队决策（回队 MR1 / 维持 backlog / 补登记）+ README 既有行覆盖复核结果。
- 核实 §对账差异登记 #5（实现修复项 successor 残留：P1-MA2-061/P1-MA2-064 等）纳入完整性，区分「已实现修复的 finding」与「仍待触发的 successor」。
- 对回队项登记双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）。
- 报告产出即更新 `docs/audits/arm-index.md`（successor 回队注记）。

## Non-Goals

- **不实施修复**（修复属 MR1 R1.0 展开的 RC-R1.n；本计划是审计）。
- **不复查方案 B 关闭裁决本身**（属 A2.x；本计划只复查 successor 触发条件；与 A2.x 的两面关系按 §MA2↔MA3 协作交叉引用）。
- **不修改真相源**（product-scope / sal/ast/prj/qa owner doc / backlog README 的既有 successor 登记；§9 冻结——回队决策记入报告 + arm-index，不直改 backlog README；README 覆盖差异记入报告交后续 backlog 维护）。
- **不复查其他域 successor**（A3.1 finance done / A3.2 同批 mfg+inv+pur / A3.4 hr+crm+cs / A3.5 扩展域+跨域 各自独立 plan）。
- **不重跑既有 arm 审计**（§去重协议：既有 `2026-07-2*-arm-ma2-*-state-machine.md` sales/assets/projects/quality 已证实行为直接引用）。

## Task Route

- Type: `verification or audit work`（successor 触发条件完整性 + 回队复查；非实现变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§4/§5/§6/§7/§8/§9 + §MA2↔MA3 协作 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A3.3 工作项 + Work Item Details MA3）+ `docs/audits/rc-existing-inventory.md`（successor 三源对账清单 sal+ast+prj+qa 分组 + §对账差异登记 #5）+ `docs/audits/arm-index.md`（successor 行内声明）+ sal/ast/prj/qa owner docs（`sales/`[订单维度核销 §L3]、`assets/state-machine.md`[IDLE §已知限制]、`projects/cost-collection.md` + `projects/state-machine.md`[工时归集]、`quality/inspection-integration.md`[质检联动 §4]、master-data ErpMdEmployee[employee-id]，successor 内嵌段落 S2）+ `docs/backlog/README.md`（S3 既有追踪行）。
- Skill Selection Basis: `Skill: docs/skills/open-ended-audit-prompt.md`（roadmap MA3 全部 A3.x 指定）。该技能适合「触发条件是否已满足 + 是否该回队 + 实现修复项 successor 残留核实」的开放式逐项裁决。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 复查以读 arm-index/owner doc/backlog README/git log + grep 实仓代码（验证状态机 writer/回调接线/ORM 字段是否存在）为主（纯分析）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本审计无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 5 项 successor 四任务逐项核证

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-3-sal-ast-prj-qa-successor-review.md`（新建，先填段 1-5）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.3 done（方法论契约 + successor 三源对账导出就绪）

- [x] `Proof` 对 5 项 successor 逐项核证任务①②③：① 触发条件是否已满足（grep 实仓代码/config/ORM 验证，如 #1 订单维度核销查 sales receipt prepayment 是否支持订单维度、#2 资产 IDLE 查 IN_SERVICE↔IDLE writer + 折旧引擎查询条件、#3 工时单查 approve/reject 状态机 + 工时成本凭证跨域过账路径、#4 质检联动查 quality cancel 回调是否接线、#5 employee-id 查 ErpMdEmployee 是否有 userId 列 + inspectorId/assignedTo 解析器）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog）；③ 无触发条件的补登记。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Proof` 核实 §对账差异登记 #5（实现修复项 successor 残留）：区分 P1-MA2-061（IDLE，已实现修复的 finding vs 仍待触发 successor）/ P1-MA2-064（质检联动）等项的「finding 已修复」与「successor 仍有效」，避免误将已修复 finding 重新纳入 MR1。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Proof` 任务④ `docs/backlog/README.md` 既有行覆盖与正确性复核：grep backlog README sales/assets/projects/quality successor 行，逐项核实覆盖（防「已登记但从未触发」）+ 正确性（触发条件描述与实仓一致）。差异记入报告（不回写 README）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Decision` 对 5 项逐项给出复查结论（`回队 MR1` / `维持 backlog successor` / `补登记`），列明触发条件状态证据 + 三源覆盖。#3 工时归集与同型过账悬挂根因的关系 + #5 ORM ask-first 保护区域 + #5 跨 quality/maintenance 两域投影去重须在结论中显式标注。
      - Skill: `docs/skills/open-ended-audit-prompt.md`

Exit Criteria:

- [x] 报告段 1（5 项三源对账清单）+ 段 2（逐项四任务核证）已落盘，每项含触发条件状态 + 回队决策 + README 覆盖复核（非悬空「待查」）
- [x] §对账差异 #5（实现修复项 successor 残留）已核实；#5 跨域投影去重已裁决

### Phase 2 - 报告定稿 / arm-index / 回队登记

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-3-sal-ast-prj-qa-successor-review.md`（补段 6-9，报告定稿）；`docs/audits/arm-index.md`（successor 回队注记）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（5 项结论已出）

- [x] `Decision` **复用 or 新增 裁决**（§7）：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC 注记；仅当发现新 successor（owner doc 内嵌但 arm-index 无行）才新建/补登记，须 grep 后裁决。#5 跨 quality/maintenance 按 §去重协议合并裁决（不同切片不同域投影，同一控制点）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [x] `Add` 报告段 6 与 arm-index 衔接段：列明每项的复用/补登记裁决 + 双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）。
      - Skill: none
- [x] `Add` 报告段 7 静态存疑点清单（供 MA4 A4.1/A4.2 展开）：登记复查中需运行时确认的点（无则注明「无」）。
      - Skill: none
- [x] `Proof` 报告段 8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**。
      - Skill: none
- [x] `Add` 报告段 9 与既有审计差异增量声明：声明复用既有 arm 审计（successor 声明源自 arm-index + sales/assets/projects/quality 状态机报告）+ 本复查只补的「触发条件是否已满足 + 回队决策」差异。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：回队项在既有 finding/successor 行追加「RC MA3 复查（A3.3）：触发条件已满足→回队 MR1」注记；补登记项（若有）入对应分区。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查段 1-9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告段 6-9 已落盘，9 段齐全；successor 复用/补登记裁决均有 arm-index grep 依据
- [x] 回队项已写入 `arm-index.md`；静态存疑点清单已登记（供 MA4 展开）
- [x] 段 8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02c52bc0bffeSfjX6i2Z2dQQON，fresh session，未起草本计划）。10 项检查 A-J 全 PASS：格式完整、Deps 正确（A3.3 Deps=0.3 done）、单一结果表面（A3.3 = sal+ast+prj+qa successor 组 = 一份报告）、Baseline 准确（5 项 successor 逐行对 rc-existing-inventory §sal+ast+prj+qa 分组 cell-by-cell 核证全匹配；#5 跨 quality+maintenance 投影合并显式标注）、范围清晰（只读审计，修复→MR1，真相源冻结，不重审关闭裁决→A2.x，他域分离，不重跑 arm）、方法论对齐（§MA3 四任务 + §MA2↔MA3 两面分离 + Q4 边界 + §对账差异 #5 纳入[finding-fixed vs successor-pending 区分] + #5 ORM ask-first 标为修复期非裁决期）、反松弛合规、Skill 就绪、item typing 合规、无矛盾（不重审 A2.x；#5 跨域投影按 §去重协议合并裁决非双重登记）。特别核实点全确认：§对账差异 #5（P1-MA2-061/P1-MA2-064 finding 已修复 vs successor 仍有效区分）真实处理——避免误将已 R*.n-fixed finding 重纳 MR1；#5 ORM ask-first 保护区域正确框定（MA3 仅裁决 successor 触发条件，ORM 变更属 MR1 修复期 ask-first）。无阻塞。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 5 项逐项四任务核证 + §对账差异 #5 核实 + successor arm-index 衔接 + 段 8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A3.3 报告 9 段齐全 + 5 项逐项四任务结论 + 回队项登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §4/§5/§6/§7 + §MA2↔MA3 协作 + §去重协议一致；与 rc-existing-inventory sal+ast+prj+qa successor 分组 + §对账差异登记 #5 一致
- [x] 已运行验证：报告 9 段完整性自检 + 段 8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 回队项的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。回队项的修复按方法论 §10 经 MR1（R1.0 展开为 RC-R1.n）实施；触及 ORM（如 #5 ErpMdEmployee userId 列）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本复查闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告回队项交叉引用展开修复行）

## Closure

Status Note: A3.3 sales+assets+projects+quality successor 复查闭环完成——5 项 successor 逐项四任务核证齐全，全部维持 backlog（0 回队 MR1），5 项 RC MA3 注记已写入 arm-index 既有行（复用 P2-MA2-013/P1-MA2-061/P1-MA2-043/P1-MA2-064/P1-MA6-002，无新建 P*-RC-xxx）；§对账差异 #5 finding-fixed vs successor-pending 区分正确。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，ses_02bb0b3d9ffeh56zyUWigyaMev）
- Evidence: 独立结束审计 10 项核查全 PASS（对照实仓 + arm-index + 方法论 §6-§9 逐项核验）：① 报告 9 段齐全（§1-§9）；② 5 项 successor 四任务（①触发条件状态 grep 实仓 / ②回队决策 / ③补登记 / ④README 覆盖）完整无抽样无悬空待查；③ §对账差异 #5 区分正确（#2 R1.18/#3 R1.15/#4 R1.20 resolved-via-deferral + #5 R3.4 部分实现，successor 触发条件均未满足，0 误纳 MR1）；④ grep 实仓验证三处全确认（#2 ASSET_STATUS_IDLE 零 writer 仅 3 只读引用 + #4 cancelForBusinessBill 跨 quality/purchase/sales/mfg 零命中 + #5 ErpMdEmployee 实测 15 列无 userId 列）；⑤ #5 跨 quality+maintenance 两域投影按 §去重协议合并裁决一次 + ORM ask-first 正确框定为 MR1 修复期非裁决期；⑥ arm-index 5 既有行 RC MA3 注记实测全部存在（:489/:503/:506/:598/:699）；⑦ Q4 边界正确（5 项触发条件全未满足→全维持 backlog，0 回队 MR1）；⑧ §8 checker actual vs baseline 表实测值与 compliance-baseline.md §BASELINE machine-readable :296-316 精确匹配 + 独立性声明 + 交叉去重声明齐全 + 不以 checker 退出码 0 为门控；⑨ §9 真相源冻结遵守（仅 arm-index 追加 successor 注记，未触 product-scope/use-cases/owner doc 需求契约段/backlog README/已关闭 finding 关闭事实）；⑩ 文本一致（Plan/Phase Status=completed + 全部 item/exit/gate [x]）。范式对齐 A3.1（finance）done report 结构一致。

Follow-up:

- 无阻塞跟进项。#5 maintenance 域投影已由本报告合并裁决并标注 A3.5 交叉引用（非阻塞，A3.5 执行时引用本 §2.5 结论即可）。
