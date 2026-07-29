# 2026-07-30-0143-3-r1-14-mfg-dict-dead-state-owner-doc-drift manufacturing 死状态 + owner doc 漂移修复

> Plan Status: active
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.14（P1-MA2-035 + P1-MA2-036 + P1-MA2-037，源自 A2.6a/A2.6b manufacturing 状态机审查）
> Related: `docs/audits/2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`、`docs/audits/2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`、`docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（MA3 复核确认 035/036/037 分类维持）
> Audit: required

## Current Baseline

三项 finding 经实仓逐项确认：均为「dict 死状态 + owner doc 漂移」，**不破坏已实现主路径**（工单/作业卡主生命周期 + MRP RUNNING/COMPLETED/FIRMED + 预测 DRAFT/APPROVED/CANCELLED 完整覆盖）。

**P1-MA2-035（作业卡 TRANSFERRED 死状态）— 确认：**
- dict `erp-mfg/job-card-status`（`module-manufacturing/model/app-erp-manufacturing.orm.xml:47-56`）8 值含 `PARTIALLY_TRANSFERRED`（:50）+ `MATERIAL_TRANSFERRED`（:51）；常量 `ErpMfgConstants.java:52-53`；镜像 `_ErpMfgDaoConstants.java:69,74`、dict YAML、AMIS successVals 列表。
- `ErpMfgJobCardProcessor.java` setStatus 仅写 OPEN/WIP/SUBMITTED/COMPLETED/ON_HOLD/CANCELLED（:40/80/88/96/104/117）；`ErpMfgJobCardBizModel` 7 mutation（startJob/recordWork/submitJob/completeJob/holdJob/resumeJob/cancelJob），**无 transfer/setTransferred**。
- 全 `module-manufacturing` grep `setStatus(JOB_CARD_STATUS_PARTIALLY_TRANSFERRED|MATERIAL_TRANSFERRED)` = 零 writer。
- owner doc `docs/design/manufacturing/state-machine.md:188-198` 作业卡图将两态列为 WORK_IN_PROGRESS 的可达活态；`:167-178` 实现偏离补注块**未标注**两态 deferred/死。

**P1-MA2-036（MRP CANCELLED + 预测 CONSUMED 死状态）— 确认：**
- `mrp-status` dict（orm:72-78）5 值含 `CANCELLED`（:77）；常量 `ErpMfgConstants.java:95`。
- `MrpEngine.java` 仅 setStatus RUNNING(:84)/COMPLETED(:98)；`MrpReleaseService.java:233` 仅 FIRMED；`ErpMfgMrpPlanBizModel` 仅 runMrp，**无 cancel**。grep `MRP_STATUS_CANCELLED|cancelPlan|cancelMrp` = 仅常量声明。**MRP CANCELLED 死状态确认，且 owner doc（mrp.md / state-machine.md）零提及、零 Deferred 标注。**
- `forecast-status` dict（orm:126-131）含 `CONSUMED`（:129）；`ErpMfgForecastBizModel.java` 仅 setStatus APPROVED(:43)/CANCELLED(:61)，CONSUMED 仅作 cancel 守卫只读引用(:54)。**关键：forecast CONSUMED 已在 mrp.md:88 + 代码 Javadoc（`ErpMfgForecastBizModel.java:21`）自声明 Deferred（plan 2026-07-05-0427-1）**——本项主要是核对 owner doc 标注完整、补 state-machine.md 缺失的 MRP 段落注记。

**P1-MA2-037（mrp.md RELEASED vs isFirmed 漂移）— 确认：**
- `docs/design/manufacturing/mrp.md:69` ASCII 图声明「释放后建议单状态标记为 RELEASED」。
- 实现：`ErpMfgMrpPlanLine.isFirmed` 布尔（orm:824）经 `MrpReleaseService.markFirmed:129-133`（line.setIsFirmed(true)）+ `advancePlanToFirmedIfComplete:218-236`（plan head → `MRP_STATUS_FIRMED`:233）。mrp-status dict **无 RELEASED 值**。mrp.md `:84-95` 实现偏离补注块覆盖释放耦合残留但**未纠正 :69 RELEASED 措辞、未提及 isFirmed/FIRMED**。

**保护区域：** 本计划为**纯文档**修复（不改代码、不改 ORM、不 regen），不触及会计/数据删除保护区域。

## Goals

- 作业卡 PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED 在 owner doc 明确为预留死状态（转序功能 successor），与代码实际行为对齐。
- MRP CANCELLED + 预测 CONSUMED 在 owner doc 明确为预留/Deferred，消除「dict 含值但无迁移」的契约悬空。
- mrp.md 释放措辞由 RELEASED 修正为实际的 FIRMED（plan 状态）/ isFirmed（行布尔）。

## Non-Goals

- 不从 ORM 删除任何 dict 值（见 Decision，采纳「保留为预留 + 文档 Deferred」对齐 Forecast CONSUMED 既有先例）。
- 不实现转序（transfer）/ MRP 取消 / 预测消费回写业务功能（后续 successor）。
- 不新增 state-machine.md 的完整 MRP 状态机章节（仅补 Deferred 注记；完整章节归 P2-MA2-052 watch-only）。
- 不改 Java 常量、不改 AMIS view successVals 列表。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐，纯文档）
- Owner Docs: `docs/design/manufacturing/state-machine.md`、`docs/design/manufacturing/mrp.md`
- Skill Selection Basis: 纯 owner doc 编辑，无 BizModel/视图/测试代码 → `Skill: none`（无匹配技能；本工作为文档真相源对齐，非平台开发模式）。

## Infrastructure And Config Prereqs

- No infra prereqs（纯文档计划）。

## Execution Plan

### Phase 1 - 作业卡 TRANSFERRED 死状态 owner doc 对齐（P1-MA2-035）

Status: planned
Targets: `docs/design/manufacturing/state-machine.md`
Skill: `none`

- Item Types: `Decision | Add`
- Prereqs: none

- [ ] **Decision**：作业卡 PARTIALLY_TRANSFERRED + MATERIAL_TRANSFERRED 死状态处置。
      - 选择 A（推荐）：owner doc 标注 Deferred——保留 dict 两值为预留语义入口（转序/工序转移功能 successor），state-machine.md 作业卡图 + 补注块明确「两态本期无 setStatus writer，不可达；预留待转序功能上线」。
      - 选择 B：从 ORM 删除两 dict 值 + 常量 + AMIS successVals（ORM 变更 + regen + 多文件）。
      - 理由：对齐仓库既有先例（forecast CONSUMED 已按「保留 dict + mrp.md:88 Deferred 标注」处理）；转序是合理未来特性，删后重加是 churn；零数据（无 writer 即无行持此状态），保留为预留零运行时风险。采纳 A，B 作为替代记入。
      - Skill: `none`
- [ ] state-machine.md `:188-198` 作业卡图两态标注「预留（Deferred，无 writer）」；补注块 `:167-178` 增一条转序死状态 Deferred 说明。
      - Skill: `none`

Exit Criteria:

- [ ] state-machine.md 作业卡段明确 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 为预留死状态（不可达），与代码零 writer 一致。

### Phase 2 - MRP CANCELLED + 预测 CONSUMED owner doc 对齐（P1-MA2-036）

Status: planned
Targets: `docs/design/manufacturing/mrp.md`、`docs/design/manufacturing/state-machine.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: none

- [ ] mrp.md 增 MRP CANCELLED Deferred 标注（无 cancelPlan mutation，dict 值预留待 MRP 取消功能 successor）；与 `:88` forecast CONSUMED 既有 Deferred 标注并列，补齐注记完整。
- [ ] state-machine.md 增一行 MRP-plan/Forecast 死状态指引（指向 mrp.md Deferred 段；不展开完整章节——P2-MA2-052 watch-only）。
- [ ] 核对 `ErpMfgForecastBizModel.java:21` Javadoc + mrp.md:88 的 forecast CONSUMED Deferred 标注已完整（无需改代码；若 mrp.md 措辞不完整则补全）。

Exit Criteria:

- [ ] mrp.md 明确 MRP CANCELLED + 预测 CONSUMED 均为预留/Deferred；owner doc 与代码零 writer 一致。

### Phase 3 - mrp.md RELEASED → FIRMED/isFirmed 措辞修正（P1-MA2-037）

Status: planned
Targets: `docs/design/manufacturing/mrp.md`
Skill: `none`

- Item Types: `Fix`
- Prereqs: none

- [ ] mrp.md `:69` ASCII 图「释放后建议单状态标记为 RELEASED」修正为实际机制：行级 `isFirmed=true`（`MrpReleaseService.markFirmed`）+ 全部行 firmed 后 plan head → `FIRMED`（`advancePlanToFirmedIfComplete`）；补注块 `:84-95` 增一条 RELEASED→FIRMED/isFirmed 措辞更正说明。
      - Skill: `none`

Exit Criteria:

- [ ] mrp.md 释放段措辞与 `MrpReleaseService` 实际行为（isFirmed + FIRMED）一致，无 RELEASED 幻影状态。

## Draft Review Record

- Independent draft review iteration 1: accept (review-2026-07-30-mfg-dict-drift) because 格式合规、三项 finding 干净映射三个 Phase 且退出标准可测；单一结果表面（manufacturing owner docs，符合规则 14）；Phase 1 Decision A 记录替代 B 与仓库先例（forecast CONSUMED）理由（规则 9）；Deferred 项均命名 successor 触发事件（规则反松弛）；纯文档计划正确删除 mvn/typecheck 验证门控并说明原因（模板 L236）；文本一致性门控保留。无 Blocker/Major。Minor（留待结束/深度审计）：Phase 2/3 项目省略逐项 Skill: none（已由阶段级声明覆盖，规则 8 满足）；Phase 2 第三项（核对 forecast CONSUMED 标注）为条件性 Add，亦可读作 Proof，现状可接受。

## Closure Gates

- [ ] 范围内文档对齐完成（三项 finding 的 owner doc 漂移消除）
- [ ] 相关文档对齐（state-machine.md / mrp.md）
- [ ] 无范围内项目降级为 deferred/follow-up（Decision A 是处置裁决，已明确 successor；非范围内缺陷隐瞒）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中
- [ ] 本计划为纯文档（无代码/ORM 变更），Closure Gates 的验证命令门控删除——无 `mvn`/typecheck 适用（见执行时规则 7 + 模板说明）

## Deferred But Adjudicated

### 转序/工序转移功能（作业卡 TRANSFERRED 两态 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 两态为零 writer 死状态，不破坏作业卡主生命周期（OPEN→WIP→SUBMITTED/COMPLETED + ON_HOLD/CANCELLED 完整）；dict 值保留为预留并已文档化。
- Successor Required: `yes`（转序功能上线时实现 setStatus writer + 状态迁移守卫）

### MRP 取消 / 预测消费回写功能

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: MRP RUNNING/COMPLETED/FIRMED + 预测 DRAFT/APPROVED/CANCELLED 主路径完整；CANCELLED/CONSUMED 预留并已文档化。
- Successor Required: `yes`（MRP 取消需求 / 预测消费状态回写需求落地时）

## Closure

Status Note: _（待结束审计）_

Closure Audit Evidence:

- Auditor / Agent: _（独立子代理）_
- Evidence: _（task id / walkthrough）_

Follow-up:

- _（非阻塞跟进；已确认缺陷不得出现在此处）_
