# 2026-07-29-2005-2 data-dependency-matrix owner-doc 准确性修复（数值核验 + finance 纯读规则注记）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MR1 工作项 R1.4（MA1 跨模块依赖审计 P1 findings）
> Related: `docs/plans/00-plan-authoring-and-execution-guide.md`；`docs/audits/arm-index.md`（P1-MA1-015 / P1-MA1-017）
> Audit: required

## Current Baseline

A1.10 跨模块依赖与 DAG 审计（done）登记 2 项 owner-doc 准确性 P1 findings，均指向 `docs/architecture/data-dependency-matrix.md`：

- **P1-MA1-015（§5.6.2 数值偏低）**：owner doc 自述数值偏低——声称 ~369 to-one / ~68 external，实测 **625 to-one + 0 to-many / 111 external**（+69% / +63%）。owner doc 文字已自声明"待 codegen 后跑脚本精确统一"（§5.6.2 line 38 "以本文 §5.6.2 实测清单为最高权威"），A1.10 审计即该脚本。权威提取脚本 `docs/audits/scripts/cross-module-dep-extract.py` 已存在并在 A1.10 中闭合提供权威值。需用机器核验值更新 §5.6.2 汇总数值与每域明细表。
- **P1-MA1-017（§3.2/§4.4 finance 纯读规则不完整）**：owner doc §3.2（line 160）"关键规则：finance 对业务域是纯读——从不写业务表"与 §4.4（line 258）"禁止反向 S 写：finance 不回写业务表"未覆盖期末结账期间的跨域 command/request 编排：finance 调 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation/reverseDepreciation` + `IErpInvCostingBiz.reclosePeriodCosts`。需补注澄清："纯读"指 ORM 层无反向 to-one；期间结账的 command 编排在 I*Biz 层合法（业务域自管实体的写，finance 仅发起 command 不持引用）。

剩余差距：上述 2 项 P1 在 roadmap 标记 `todo`，无活跃修复 plan。文档当前数值与规则表述与实测/实现不一致。

## Goals

- P1-MA1-015：用 `cross-module-dep-extract.py` 产出权威值，更新 `data-dependency-matrix.md §5.6.2` 汇总数值（625 to-one / 111 external）及每域明细表，使 owner doc 数值与实仓一致。
- P1-MA1-017：在 `§3.2` 与 `§4.4` 补注 finance 纯读规则的精确边界——区分"ORM 层无反向 to-one"（纯读）与"I*Biz 层 command 编排"（合法，业务域自管写），消除"finance 调业务域 I*Biz 方法"与"纯读"规则的表面矛盾。

## Non-Goals

- 不改任何代码（纯 owner-doc 准确性修复）。
- 不重新审计 DAG（A1.10 已确认零循环零禁止方向）。
- 不处理 R1.5（跨域只读 daoFor 裁决）及之后工作项——R1.5 涉及代码架构决策，属不同结果表面，后续 plan。
- 不处理 R1.7（posting-exemptions 登记）——不同 finding、不同文档，且按文档顺序在 R1.5/R1.6 之后，不在本 plan 范围。

## Task Route

- Type: `implementation-only change`（owner-doc 数值/规则准确性修复，finding 已明确）
- Owner Docs: `docs/architecture/data-dependency-matrix.md`（§5.6.2 + §3.2 + §4.4）；权威数据源 `docs/audits/scripts/cross-module-dep-extract.py`；finding 证据 `docs/audits/arm-index.md`
- Skill Selection Basis: roadmap R1.4 Skill 列 = `none`（文档数值核验 + 规则注记，非审计/行为维度）。无 opencode 技能匹配。

## Infrastructure And Config Prereqs

- 无代码/端口/密钥依赖。需 `docs/audits/scripts/cross-module-dep-extract.py` 可执行（Python 脚本，已在 A1.10 验证可用）。

## Execution Plan

### Phase 1 - P1-MA1-015 §5.6.2 权威数值回填

Status: completed
Targets: `docs/architecture/data-dependency-matrix.md`（§5.6.2 汇总 + 每域明细表）
Skill: none

- Item Types: `Fix`
- Prereqs: 无

- [x] Fix：运行 `python3 docs/audits/scripts/cross-module-dep-extract.py`（脚本硬编码 REPO 路径，直接执行即可）产出权威 to-one/external 计数；将 §5.6.2 汇总数值（625 to-one / 111 external）及每域明细表更新为脚本输出值。保留 owner doc 现有"以脚本为权威"的声明文字。
  - Skill: none
- [x] Proof：将脚本每域输出块粘贴入计划的 closure 证据，人工对照确认脚本输出值与文档更新后的数值逐项一致（机器可复现，非目测）。
  - Skill: none

Exit Criteria:

- [x] §5.6.2 汇总数与每域明细表均与脚本权威输出一致。

### Phase 2 - P1-MA1-017 finance 纯读规则边界注记

Status: completed
Targets: `docs/architecture/data-dependency-matrix.md`（§3.2 line 160 关键规则 + §4.4 line 258 禁止反向 S 写）
Skill: none

- Item Types: `Fix`
- Prereqs: 无（与 Phase 1 独立）

- [x] Fix：在 §3.2"关键规则"与 §4.4"禁止反向 S 写"处补注精确边界——明确"纯读/不回写"指 ORM 层无 finance→业务域的反向 `<to-one>` 与无 finance 直接 saveEntity/updateEntity 业务表；期间结账 finance 经 `IErpAstDepreciationScheduleBiz.executeBatchDepreciation` / `IErpInvCostingBiz.reclosePeriodCosts` 等 command 编排是 I*Biz 层合法调用（业务域自管实体写，finance 仅发起 command），不违反纯读规则。引用具体方法名作为实证。
  - Skill: none
- [x] Proof：注记文字准确反映实仓实现（方法名与 §3.2 取数清单交叉一致）。
  - Skill: none

Exit Criteria:

- [x] §3.2 与 §4.4 规则表述与实仓 command 编排实现一致，表面矛盾消除。

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_0523a0986ffe4OF8P37W9ywjcG）— Phase 2 Targets 字段误写 `data-dependency.md`（正确为 `data-dependency-matrix.md`），自洽性缺陷；Phase 1 Proof 宜粘贴脚本输出块入 closure 证据；脚本调用宜固定命令去除"或…既定"软措辞。
- Independent draft review iteration 2: accept（ses_05235c2b9ffeGcVOYcXHIqTJAc）— Phase 2 Targets 文件名已修正为 `data-dependency-matrix.md`（与全文 6 处引用自洽）；Phase 1 Proof 已改为粘贴脚本输出块入 closure 证据；脚本命令已固定。无残留 blocking。

## Closure Gates

> 本 plan 为纯文档变更（无代码改动）。按指南"对于无代码更改的计划（仅文档），删除验证命令门控并说明原因"——移除 build/test 验证命令门控，原因：仅编辑 `docs/architecture/data-dependency-matrix.md` 文本与数值，不触及任何生成代码或运行时行为。

- [x] 范围内完成：P1-MA1-015（§5.6.2 数值回填）+ P1-MA1-017（§3.2/§4.4 规则注记）。
- [x] 文档数值与脚本权威输出一致（Phase 1 Proof）。
- [x] 规则注记与实仓 command 编排实现一致（Phase 2 Proof）。
- [x] arm-index 中 P1-MA1-015 / P1-MA1-017 状态回填为已修复。
- [x] 无范围内项目降级为 deferred/follow-up。
- [x] 独立草案审查已完成并记录。
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致。
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计。
- [x] 结束证据存在于文件中。

## Deferred But Adjudicated

_（暂无）_

## Closure

Status Note: 两 Phase 已执行完成（纯 owner-doc 文本/数值变更，无代码改动，build/test 门控已按指南移除）。Phase 1 用 `cross-module-dep-extract.py` 权威输出回填 §5.6.2；Phase 2 在 §3.2/§4.4 补注 finance 纯读规则边界。arm-index 两项 finding 已置 ✅ fixed，roadmap R1.4 已置 done。结束审计待独立子代理执行后回填下方证据。

Closure Audit Evidence:

- Auditor / Agent: _独立结束审计（新会话，执行者未自我审计）。结果=PASS。证据：① Phase 1 跑 `cross-module-dep-extract.py`（§7 + §5 + §2），17 域逐行对齐 §5.6.2（inv 85/10 … b2b 15/3 全 match），汇总 625/0/111 + 核心 9=468/扩展 8=157 + external 79+32 算术闭合，跨业务边 fin→prj 6 / pur→prj 2 / sal→prj 1 / hr→prj 2 与 §2 DAG 一致；② Phase 2 三方法名实测存在（`IErpAstDepreciationScheduleBiz.executeBatchDepreciation` ast-dao:28 / `reverseDepreciation` ast-dao:35 / `IErpInvCostingBiz.reclosePeriodCosts` inv-dao:30），均由 `ErpFinAccountingPeriodProcessor` 期末结账调用（runDepreciation:351 / recloseInvCosts:370 / reverseDepreciation:391），ORM 层 fin→md 97+fin→prj 6=103 零 fin→inv/fin→ast，§2.0 裁决原则 4 引用准确（line 41）；③ arm-index P1-MA1-015/017 均 ✅ fixed、roadmap R1.4=done；④ 纯文档变更、无代码改动、未越界 R1.5/R1.6/R1.7。注：§2.3/§6.1/§6.5 残留陈旧每域计数，按计划 Non-Goals 属 §5.6.2 外、不构成本 plan 缺陷（留待未来 finding）。_
- Phase 1 Proof（脚本每域输出块，机器可复现，对照文档更新值逐项一致）：

  ```
  脚本 §7 Per-domain cross-module to-one counts（权威）：
  | src domain | to-one count | to-many count | external decl count |
  | aps  | 6  | 0 | 1  |
  | ast  | 50 | 0 | 6  |
  | b2b  | 15 | 0 | 3  |
  | crm  | 38 | 0 | 5  |
  | cs   | 15 | 0 | 2  |
  | ct   | 13 | 0 | 4  |
  | drp  | 26 | 0 | 6  |
  | fin  | 103| 0 | 12 |
  | hr   | 34 | 0 | 6  |
  | inv  | 85 | 0 | 10 |
  | log  | 10 | 0 | 5  |
  | md   | 0  | 0 | 0  |
  | mfg  | 58 | 0 | 10 |
  | mnt  | 13 | 0 | 8  |
  | notify | 0 | 0 | 0 |
  | prj  | 27 | 0 | 5  |
  | pur  | 58 | 0 | 12 |
  | qa   | 19 | 0 | 5  |
  | sal  | 55 | 0 | 11 |
  | TOTAL| 625| 0 | 111|

  脚本汇总：to-one=625 / to-many=0 / external=111；唯一 (src,tgt) 边 24 条；跨业务域边 7 条全 allow-list；零循环。
  文档更新点：§5.6.2 每域明细表新增"跨模块 to-one / 外部实体声明（实测）"列（17 域逐行对齐上表）；§5.6.2 终态统计块改为 625 to-one（核心 9 域 468 + 扩展 8 域 157）/ 0 to-many / 111 external（核心 79 + 扩展 32）。逐项一致 ✅。
  ```

- Phase 2 Proof（实仓方法名实证，与 §3.2 取数清单交叉一致）：

  ```
  脚本 DAG 边证实 finance ORM 层：fin→md（97）+ fin→prj（6）= 103，零 fin→inv / fin→ast ORM 边 → "纯读"在 ORM 层成立。
  I*Biz 层 command 编排实证（finance 期末结账期间发起，业务域自管实体写）：
    - IErpAstDepreciationScheduleBiz.executeBatchDepreciation(period, ctx)
        → ErpFinAccountingPeriodProcessor.runDepreciation:344-357（assets 域自管 ErpAstDepreciationSchedule 写）
    - IErpAstDepreciationScheduleBiz.reverseDepreciation(assetId, period, ctx)
        → ErpFinAccountingPeriodProcessor.reverseDepreciation:381-396（assets 域自管冲销写）
    - IErpInvCostingBiz.reclosePeriodCosts(periodId, start, end, ctx)
        → ErpFinAccountingPeriodProcessor.recloseInvCosts:364-378（inventory 域自管 ErpInvCostLayer/ledger 写）
  三方法名均出现在 §3.2 注记文字中，与 §3.2 取数清单（IErpAstDepreciationScheduleBiz 读折旧计划 / IErpInvStockMoveBiz 等）交叉一致 ✅。
  command 失败仅告警不阻断结账（catch → LOG.warn），finance 不持业务表 ORM 引用、不回写业务单据表 ✅。
  ```
- 闭环动作：arm-index `P1-MA1-015`/`P1-MA1-017` → `✅ fixed（plan 2026-07-29-2005-2）`；roadmap R1.4 → `done`。

Follow-up:

- _（无；已确认缺陷不出现于此）_
