# 2026-08-06-0442-2 rc-ma3-a3-2-mfg-inv-pur-successor-review MA3 successor 追踪完整性与回队复查（mfg+inventory+purchase）

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A3.2（mfg + inventory + purchase 域 successor 复查）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A3.2
> Related: `docs/plans/2026-08-02-1530-2-existing-inventory-export.md`（M0.3 done，导出 successor 三源对账清单 mfg+inv+pur 分组 + §对账差异登记 #6 单源遗漏风险）、`docs/plans/2026-08-07-0300-2-rc-ma3-a3-1-finance-successor-review.md`（A3.1 done，范式参照 + A3.1 Follow-up「#1 GRNI 自动冲回跨域依赖 inventory `repostPurchaseInput` SPI 归 A3.2 复查范畴」）、`docs/plans/2026-08-06-0442-1-rc-ma2-a2-3-8-9-governance-exemption-simplification-review.md`（A2.3/A2.8/A2.9 关闭裁决复查，successor 两面关系——关闭裁决归 A2.x、successor 触发条件归本 A3.x）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份 MA3 复查报告 + arm-index/backlog 登记更新。基线盘点的是 successor 触发条件的现状，**不修改任何代码/真相源**。

- **方法论契约就绪**：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）已落盘（M0.1 done）。

- **successor 三源对账全集已导出**（M0.3 done，`docs/audits/rc-existing-inventory.md` §successor 三源对账清单 — mfg+inv+pur 分组）：design-level successor 去重并集 = **7 项**，逐项含三源覆盖标记（S1=arm-index 行内 / S2=owner doc 内嵌 / S3=backlog README）、触发条件摘要、已满足?、当前归属、复杂度：

  | # | successor 项 | 三源覆盖 | 触发条件摘要 | 已满足? | 复杂度 |
  |---|-------------|---------|-------------|---------|--------|
  | 1 | 物料预留子系统完整写路径（mfg 侧） | S1+S2 | 库存域 `ErpInvReservation*` 写接口先行落地后，mfg 经 `IErpInvReservationBiz` 接线 | ❌ 未满足 | S |
  | 2 | 委外单 MRP 释放收敛为 I*Biz 调用 | S1 | 委外域提供 purpose-built `createFromMrpLine` 时 | ❌ 未满足 | S |
  | 3 | STANDARD 红冲成本不变量（FIFO 调整层物理删除边界） | S2 | 实际启用 FIFO 物料的成本调整红冲遇此场景时 | ❌ 未满足 | A |
  | 4 | 盘点自动生成盘盈/盘亏移动单 | S1 | owner doc §盘点单状态机 自动生成语义恢复 | ❌ 未满足（P1-MA2-062 经 R1.19 实现修复，但方案 B 路径未走；当前 Deferred 手工） | A |
  | 5 | 拣货单状态机（WMS） | S1 | WMS 上线时 | ❌ 未满足 | A |
  | 6 | master-data 跨域只读迁移（md 目标域子集，P1-MA1-022 子集） | S1 | master-data I*Biz 补便捷只读方法后迁移 | ❌ 未满足 | B |
  | 7 | md 目标域子集=可迁移（P1-MA1-022 子集） | S1 | md I*Biz 只读方法补齐 | ❌ 未满足 | B |

  > 注：#2（委外收敛）与 A2.3 复查的 `P1-MA2-038` 同一 finding 两面——关闭裁决本身归 A2.x 复查，successor 触发条件归本 A3.x 复查，交叉引用不重复。#6/#7 是 `P1-MA1-022`（A2.9 复查）的 successor 子集，同样两面关系。

- **A3.1 跨域 deferred 项接入**：A3.1 Follow-up 记录「#1 GRNI 自动冲回跨域依赖 inventory 域 `repostPurchaseInput` SPI（A3.1 §successor #1），归 A3.2 复查范畴」。本计划须在报告中核实该 inventory SPI 是否仍缺失（grep `module-inventory` `repostPurchaseInput`），并在 §9 差异增量声明中交叉引用 A3.1 结论（GRNI successor 触发条件=未满足，维持 backlog；inventory SPI 落地是 GRNI 回队的前置，归本 A3.2 inventory 侧判定）。

- **复查四项任务**（roadmap MA3 Work Item Details）：逐项核对 ① 触发条件是否已满足（已满足 → 回队 MA1/R1.0）；② 是否该回队（回到审计 / R1.0 修复 / backlog README）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核（防「已登记但从未触发」）。

- **已知结构性约束与单源遗漏风险**：
  - #1（物料预留）是 mfg 多项前置；M0.3 §对账差异登记 #6 标注「单源遗漏风险」——S1（P1-MA3-042 经 R2.6 修复 owner doc 标注）+ S2（`material-reservation.md §9/§14` 整节 Deferred）双源覆盖，但实现侧仅库存域 reservedQty 承载，MA3 A3.2 须核实「完整预留写路径」successor 是否仍需回队。
  - #4（盘点自动移动单）：P1-MA2-062 经 R1.19 实现修复，但「方案 B 路径未走」（手工 Deferred），MA3 须核实自动生成语义 successor 是否仍有效。
  - #3（STANDARD 红冲 FIFO 边界）：仅 S2（`costing-methods.md §66`），arm-index 无独立行（P2-MA2-029 watch-only 承接），属 §对账差异登记 #3「owner doc 内嵌但 arm-index 无行」——MA3 须纳入避免遗漏。

- **Q4 修复义务边界**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。
- **finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x）。即：successor 回队与否（A3.x）≠ finding 是否修复（A2.x→MR1），两者各自裁决、交叉引用不冲突。

- **保护区域**：本复查为**只读审计**（读 arm-index/owner doc/backlog README/git log，不改代码/ORM/api.xml/真相源）。属 roadmap 预授权类目。回队项的修复**不在本计划实施**——经 MR1（R1.0 展开）；触及 ORM（如物料预留写接口、拣货单状态机字段）的修复行须 ask-first（§5）。

- **剩余差距**：A3.2 复查报告缺失 = MR1（R1.0）该域 successor 回队决策证据缺口来源。本计划产出 A3.2 复查报告并登记回队决策，解除其在 MR1 链路的该域证据缺口。

## Goals

- 产出 MA3 复查报告 `docs/audits/<执行时间戳>-rc-ma3-a3-2-mfg-inv-pur-successor-review.md`，含方法论 §6 **9 段全部内容**（MA3 适配：段 1=successor 三源对账清单 mfg+inv+pur 分组 + A3.1 GRNI 跨域项接入；段 2=逐项四任务核证[触发条件已满足?/是否回队/补登记/README 覆盖]；段 3/4=既有行为证据；段 5=复查结论「回队 MR1 / 维持 backlog successor / 补登记」；段 6=arm-index 衔接；段 7=静态存疑点；段 8=过程纪律自检；段 9=与既有审计差异增量）。
- 对 7 项 successor **逐项**完成四任务核证（完整枚举，禁止抽样）：每项给出触发条件状态（已满足/未满足 + 证据，grep 实仓代码/config/SPI）+ 回队决策（回队 MR1 / 维持 backlog / 补登记）+ README 既有行覆盖复核结果。
- 核实 A3.1 deferred 跨域项（GRNI 依赖 inventory `repostPurchaseInput` SPI）的当前状态，§9 交叉引用 A3.1。
- 核实 §对账差异登记 #6（物料预留单源遗漏风险）+ #3（#3 STANDARD 红冲仅 S2 owner doc 内嵌）纳入完整性。
- 对回队项登记双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）；#2/#6/#7 回队决策与 A2.3/A2.9 复查结论交叉一致。
- 报告产出即更新 `docs/audits/arm-index.md`（successor 回队注记）。

## Non-Goals

- **不实施修复**（修复属 MR1 R1.0 展开的 RC-R1.n；本计划是审计）。
- **不复查方案 B 关闭裁决本身**（属 A2.x；本计划只复查 successor 触发条件；与 A2.x 的两面关系按 §MA2↔MA3 协作交叉引用）。
- **不修改真相源**（product-scope / mfg/inv/pur owner doc / backlog README 的既有 successor 登记；§9 冻结——回队决策记入报告 + arm-index，不直改 backlog README；README 覆盖差异记入报告交后续 backlog 维护）。
- **不复查其他域 successor**（A3.1 finance done / A3.3-A3.5 各自独立 plan）。
- **不重跑既有 arm 审计**（§去重协议：既有 `2026-07-2*-arm-ma2-inventory-costing-consistency.md` / `-mfg-*-state-machine.md` / `-inventory-state-machine.md` 等已证实行为直接引用）。

## Task Route

- Type: `verification or audit work`（successor 触发条件完整性 + 回队复查；非实现变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§4/§5/§6/§7/§8/§9 + §MA2↔MA3 协作 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A3.2 工作项 + Work Item Details MA3）+ `docs/audits/rc-existing-inventory.md`（successor 三源对账清单 mfg+inv+pur 分组 + §对账差异登记 #3/#6）+ `docs/audits/arm-index.md`（successor 行内声明）+ mfg/inv/pur owner docs（`manufacturing/material-reservation.md`、`manufacturing/mrp.md`、`finance/costing-methods.md`[inv 成本 successor 内嵌]、`inventory/state-machine.md`[盘点/拣货 successor]、`architecture/posting-exemptions.md`[委外收敛]、`architecture/data-dependency-matrix.md §9`[md 迁移]，successor 内嵌段落 S2）+ `docs/backlog/README.md`（S3 既有追踪行）。
- Skill Selection Basis: `Skill: docs/skills/open-ended-audit-prompt.md`（roadmap MA3 全部 A3.x 指定）。该技能适合「触发条件是否已满足 + 是否该回队 + 单源遗漏风险核实」的开放式逐项裁决。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 复查以读 arm-index/owner doc/backlog README/git log + grep 实仓代码（验证 SPI/写接口/状态机字段是否存在）为主（纯分析）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本审计无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 7 项 successor 四任务逐项核证 + A3.1 跨域项接入

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-2-mfg-inv-pur-successor-review.md`（新建，先填段 1-5）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.3 done（方法论契约 + successor 三源对账导出就绪）

- [ ] `Proof` 对 7 项 successor 逐项核证任务①②③：① 触发条件是否已满足（grep 实仓代码/config/SPI 验证，如 #1 物料预留查 inventory `ErpInvReservation*` 写接口是否存在 + mfg `IErpInvReservationBiz` 是否接线、#2 委外收敛查委外域 `createFromMrpLine` 是否存在、#3 STANDARD 红冲查 FIFO 调整层红冲路径、#4 盘点自动移动单查 StockTake 状态机是否自动生成、#5 拣货单 WMS 状态机、#6/#7 md I*Biz 只读方法是否补齐）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog）；③ 无触发条件的补登记（#3 仅 S2 owner doc 内嵌，arm-index 无独立行须核实是否补登记）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [ ] `Proof` 核实 A3.1 deferred 跨域项：grep `module-inventory` `repostPurchaseInput` SPI 是否仍缺失（A3.1 §successor #1 GRNI 依赖）；结论记入 §9 差异增量（与 A3.1 结论交叉：GRNI successor=未满足→维持 backlog，inventory SPI 落地是前置）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [ ] `Proof` 任务④ `docs/backlog/README.md` 既有行覆盖与正确性复核：grep backlog README mfg/inv/pur successor 行，逐项核实覆盖（防「已登记但从未触发」）+ 正确性（触发条件描述与实仓一致）。差异记入报告（不回写 README）。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [ ] `Decision` 对 7 项逐项给出复查结论（`回队 MR1` / `维持 backlog successor` / `补登记`），列明触发条件状态证据 + 三源覆盖 + 与 A2.x 关闭裁决的交叉关系（#2↔P1-MA2-038[A2.3] / #6+#7↔P1-MA1-022[A2.9]）。#1 物料预留结构性约束（mfg 多项前置）+ §对账差异 #6 单源遗漏风险须在结论中显式标注。
      - Skill: `docs/skills/open-ended-audit-prompt.md`

Exit Criteria:

- [ ] 报告段 1（7 项三源对账清单 + A3.1 GRNI 跨域项接入）+ 段 2（逐项四任务核证）已落盘，每项含触发条件状态 + 回队决策 + README 覆盖复核（非悬空「待查」）
- [ ] §对账差异 #6（物料预留）+ #3（STANDARD 红冲仅 S2）已纳入核实；A3.1 GRNI 跨域项状态已交叉引用

### Phase 2 - 报告定稿 / arm-index / 回队登记

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma3-a3-2-mfg-inv-pur-successor-review.md`（补段 6-9，报告定稿）；`docs/audits/arm-index.md`（successor 回队注记）
Skill: `docs/skills/open-ended-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成（7 项结论已出）

- [ ] `Decision` **复用 or 新增 裁决**（§7）：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC 注记；仅当发现新 successor（owner doc 内嵌但 arm-index 无行，如 #3 STANDARD 红冲）才新建/补登记，须 grep 后裁决。
      - Skill: `docs/skills/open-ended-audit-prompt.md`
- [ ] `Add` 报告段 6 与 arm-index 衔接段：列明每项的复用/补登记裁决 + 双向可追溯（successor ↔ finding ID ↔ MR1 R1.0 预留展开行）。
      - Skill: none
- [ ] `Add` 报告段 7 静态存疑点清单（供 MA4 A4.1/A4.2 展开）：登记复查中需运行时确认的点（无则注明「无」）。
      - Skill: none
- [ ] `Proof` 报告段 8 过程纪律自检段（§8 模板）：实际运行 `bash docs/audits/nop-compliance-checker.sh` 并附 actual vs baseline 汇总表（本审计无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 脚本退出码 0 作为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告段 9 与既有审计差异增量声明：声明复用既有 arm 审计（successor 声明源自 arm-index + mfg/inv 状态机/成本一致性报告）+ A3.1 GRNI 跨域项交叉 + 本复查只补的「触发条件是否已满足 + 回队决策」差异。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：回队项在既有 finding/successor 行追加「RC MA3 复查（A3.2）：触发条件已满足→回队 MR1」注记；补登记项（#3 若适用）入对应分区。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查段 1-9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告段 6-9 已落盘，9 段齐全；successor 复用/补登记裁决均有 arm-index grep 依据
- [ ] 回队项已写入 `arm-index.md`；静态存疑点清单已登记（供 MA4 展开）
- [ ] 段 8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02c52def5ffe0cX48O3X1V07ds，fresh session，未起草本计划）。10 项检查 A-J 全 PASS：格式完整、Deps 正确（A3.2 Deps=0.3 done）、单一结果表面（A3.2 = mfg+inv+pur successor 组 = 一份报告）、Baseline 准确（7 项 successor 逐行对 rc-existing-inventory §mfg+inv+pur 分组 cell-by-cell 核证，三源覆盖/触发条件/已满足状态/复杂度全匹配；两面链接 #2↔P1-MA2-038[A2.3]、#6/#7↔P1-MA1-022[A2.9] 核证）、范围清晰（只读审计，修复→MR1，真相源冻结，不重审关闭裁决→A2.x，他域分离，不重跑 arm）、方法论对齐（§MA3 四任务复查 + §MA2↔MA3 两面分离 + Q4 边界 + §对账差异 #3/#6 纳入）、反松弛合规、Skill 就绪、item typing 合规、无矛盾（不重审 A2.x；#4 盘点 P1-MA2-062 R1.19-fixed 正确按 §对账差异 #5「实现修复项 successor 残留」处理=裁决 successor 触发条件非 R1.19 修复充分性）。特别核实点全确认：A3.1 deferred GRNI 跨域项（inventory repostPurchaseInput SPI）真实拾取并裁决（专设 Phase 1 checkbox + §9 交叉，非仅提及）；§对账差异 #6（物料预留单源遗漏风险）+ #3（STANDARD 红冲仅 S2 owner doc 内嵌）真实纳入完整性核实。无阻塞。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 7 项逐项四任务核证 + A3.1 跨域项交叉 + successor arm-index 衔接 + 段 8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A3.2 报告 9 段齐全 + 7 项逐项四任务结论 + 回队项登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §4/§5/§6/§7 + §MA2↔MA3 协作 + §去重协议一致；与 rc-existing-inventory mfg+inv+pur successor 分组 + §对账差异登记 #3/#6 一致；#2/#6/#7 与 A2.3/A2.9 结论交叉一致
- [ ] 已运行验证：报告 9 段完整性自检 + 段 8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 回队项的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。回队项的修复按方法论 §10 经 MR1（R1.0 展开为 RC-R1.n）实施；触及 ORM（如物料预留写接口 / 拣货单状态机字段 / md I*Biz 只读方法）的修复行须 ask-first + 独立 plan-audit（§5 保护区域暂停协议）。本复查闭环不阻塞于修复落地。
- Successor Required: yes（MR1 按本报告回队项交叉引用展开修复行）

## Closure

Status Note: <关闭时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理或独立审查者>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
