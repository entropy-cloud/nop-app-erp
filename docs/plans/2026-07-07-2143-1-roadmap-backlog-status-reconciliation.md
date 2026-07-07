# 2026-07-07-2143-1-roadmap-backlog-status-reconciliation 路线图与待办状态对账（关闭陈旧 partial 标记 + 显化下一功能后继）

> Plan Status: completed
> Last Reviewed: 2026-07-07
> Executed: 2026-07-07（3 Phase 全完成；独立结束审计已通过）
> Source: mission-driver `draft-from-roadmap`（路线图遍历发现：详细子路线图全 `done`，但 `implementation-roadmap.md` 概览、`backlog/README.md`、`core-business-roadmap.md` 中 1.1/1.2 仍标 `partial`/`todo`，与实时代码矛盾）
> Related: `docs/plans/2026-07-07-1915-1-audit-remediation-plan.md`（active；其 Deferred「7 扩展域 posted/businessDate 字段补充」为本计划显化的下一功能后继）、`docs/plans/2026-07-07-1530-1-errorcode-nop-to-erp-migration.md`（completed，前置机械替换已落地）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`/`find`，非采信旧记忆）：

- **三份子路线图（权威源）状态**：
  - `crud-roadmap.md`：18 域全 `done`（含 Milestone 4 冒烟测试 18/18 绿）。无残留。
  - `core-business-roadmap.md`：M1 工作项 1.0a/1.3/1.4/1.5/1.6/1.7/1.8/1.9/1.10/1.11 全 `done`；**1.1 Purchase Order BizModel 仍标 `partial`**；**1.2 Sales Order BizModel 仍标 `partial`**（备注「过账/发票/收款仍 todo，归 1.7」）；1.12 已 `done`。M4（4.1~4.4）/M5（5.1~5.5）全 `done`。
  - `extended-roadmap.md`：M2（2.1~2.14、2.4b、2.5b~d、2.6b~c）全 `done`；M3（3.1~3.21 + Non-Goal UC 系列）全 `done`。无残留。
- **1.1 `partial` 标记经实时核实为陈旧**：其范围「审批/入库触发/过账」三段均已由后继完成项落地——
  - 审批：`module-purchase/model/app-erp-purchase.orm.xml` 中 `erp_pur_requisition`/`erp_pur_quotation` 等实体 `tagSet` 含 `use-approval`（:115/:298）；`ErpPurOrderBizModel`（`module-purchase/erp-pur-service/.../entity/ErpPurOrderBizModel.java`）落地 `cancel`/`createFromRequisition`/`existsActiveByRequisition`/`updateReceiveStatus`；审批工作流迁移由 plan `2026-07-04-2050-1`（→ `2026-07-06-0315-1` xwf 接线）完成。
  - 入库触发：plan `2026-07-01-1132-1`（采购入库审批 + 库存触发）done。
  - 过账：plan `2026-07-01-0811-1`（过账引擎）+ `1.5 IErpFinAcctDocProvider` + `1.6 采购到付款` done。
- **1.2 `partial` 标记经实时核实为陈旧**：其范围「审批/出库触发/过账」——
  - 审批：`module-sales/model/app-erp-sales.orm.xml:280` `erp_sal_order` `tagSet` 含 `use-approval`（同迁移 2050-1/0315-1）；`ErpSalOrderBizModel` 落地 `cancel`/`createFromQuotation`/`existsActiveByQuotation`/`updateDeliveryStatus`。
  - 出库触发：plan `2026-07-01-1132-2`（销售出库审批 + 库存触发）done。
  - 过账：1.5 + `1.7 销售到收款` done（1.2 备注自述「过账归 1.7」，1.7 已 done）。信用控制段（含 AR 未核销余额纳入 outstanding）由 plan `2026-07-05-1838-1` done（`CreditLimitChecker` Javadoc :35-41 确认 AR 段已纳入）。
- **概览/README 与子路线图矛盾（陈旧）**：
  - `implementation-roadmap.md` 概览表：「core-business M1 部分 `partial`，M1.12 `todo`」「extended M2 多数 done（2.2/2.5 `partial`，2.4b~2.6c `todo`）」——与子路线图（全 done）矛盾。
  - `backlog/README.md`：P1「核心业务循环 🟡 部分 done，其余 todo」、P4「业财一体端到端 ❌ todo（下一就绪项）」——与 M1/M4 全 done 矛盾。
- **下一功能后继未在路线图显化（丢失风险）**：active plan `2026-07-07-1915-1` 的 Deferred「7 扩展域（cs/hr/logistics/b2b/contract/drp/aps）补 posted/businessDate 标准字段」是业经审计确认的下一功能切片，触发条件为「1915-1 关闭后」，但目前**未在 backlog 工作项表中显化**。实时核实字段缺口属实：上述 7 域源 `orm.xml` 中 `posted`/`businessDate` 列计数均为 0（对比 finance=5/10、purchase=5/16、sales=5/14、assets=10/22 等核心域均有）。若本对账仅将路线图全标 done 而不显化此后继，下次 mission-driver 遍历将误判「无剩余工作」并返回 `nothing`，使该审计确认的后继沉没。

剩余差距：(1) 1.1/1.2 陈旧 `partial` 未关闭；(2) 概览/README 状态串陈旧；(3) 下一功能后继（扩展域字段补充）未在 backlog 显化。

## Goals

- **关闭 1.1/1.2 陈旧 partial**：以实时核实证据将 `core-business-roadmap.md` 中 1.1、1.2 标记为 `done`，并附完成证据指针（审批经 use-approval 迁移 2050-1/0315-1；触发经 1132-1/1132-2；过账经 1.5/1.6/1.7）。
- **修正概览/README 陈旧状态**：`implementation-roadmap.md` 概览表与 `backlog/README.md` 工作项表对齐子路线图真实状态（M1/M2/M3/M4/M5 全 done）。
- **显化下一功能后继**：在 `backlog/README.md` 工作项表新增一行 P 级工作项「7 扩展域 posted/businessDate 标准字段补充」，标注路线图归属「`2026-07-07-1915-1` Deferred」、状态 `blocked`（触发条件：1915-1 关闭后）、AI 自主权 `ask-first`（ORM 保护区域），防止路线图全绿后后继沉没。
- **AGENTS.md 阶段描述同步**（若仍写「预代码生成阶段」或与现状矛盾）：对齐「18 域 codegen + 业务逻辑 + 业财一体 + 报表/看板均已落地」的当前阶段。

## Non-Goals

- **不**实施「7 扩展域 posted/businessDate 字段补充」本身——它是 active plan 1915-1 的 Deferred 后继，触发条件为 1915-1 关闭后；且涉及 ORM 保护区域（`ask-first`），须由独立后续计划承接（本计划仅在 backlog 显化它，不起草该计划）。
- **不**修改任何 `*.orm.xml` 源模型、生成代码、`*.xbiz`、Java 或前端文件——本计划纯文档/状态对账。
- **不**重排序路线图里程碑或发明新工作项——仅对齐既有工作项的真实状态。
- **不**承接 1915-1 active 计划范围内的任何整改项（表名双前缀、事务语义、字典冗余等）——属 1915-1 自身范围。
- **不**起草 DRP 实体命名统一（ErpDrp* vs ErpInvDrp*）计划——经 `2026-07-05-1500-2-cross-review-remediation`（Deferred §line 284-290）裁定为 `out-of-scope improvement`（理由：ORM 重命名波及 DAO/Entity/BizModel/XMeta/view/page/i18n 全链重生成，churn 巨大），触发条件「DRP 域业务深化」虽已满足但其价值/风险比不足以现在立项，维持 Deferred。

## Task Route

- Type: `verification or audit work` + `implementation-only change`（纯文档/状态串对账，无代码/契约变更）
- Owner Docs: `docs/backlog/00-roadmap-authoring-guide.md`（工作项状态语义与写回规则）、`docs/backlog/implementation-roadmap.md`、`docs/backlog/README.md`、`docs/backlog/core-business-roadmap.md`
- Skill Selection Basis: `none`——纯状态对账与文档编辑，不涉及 BizModel/前端/ORM/测试技能。实时核实已在本计划 Baseline 完成（非采信记忆）。

## Infrastructure And Config Prereqs

- 无 infra 依赖（无端口/密钥/.env/外部服务/数据迁移）
- 无代码变更，故无 `mvn` 构建验证门控（见 Closure Gates 说明）

## Execution Plan

### Phase 1 - 关闭 1.1/1.2 陈旧 partial 标记

Status: completed
Targets: `docs/backlog/core-business-roadmap.md`（Work Item Status 块行 14、15 + Implementation Order 表行 48、49）
Skill: none

- Item Types: `Fix`
- Prereqs: 无（实时核实已在 Baseline 完成）

- [x] `Fix`：`core-business-roadmap.md` Work Item Status 块——1.1 由 `partial` 改 `done`，附证据「审批经 use-approval 迁移（2050-1→0315-1）；入库触发经 1132-1；过账经 1.5/1.6」；1.2 由 `partial` 改 `done`，附证据「审批经 use-approval 迁移；出库触发经 1132-2；过账经 1.5/1.7；信用控制含 AR 段经 1838-1」。
  - Skill: none
- [x] `Fix`：`core-business-roadmap.md` Implementation Order 表——1.1、1.2 行状态列 `🔶 partial` 改 `✅ done`，证据指针同上。
  - Skill: none

Exit Criteria:

- [x] `core-business-roadmap.md` 中 1.1、1.2 不再出现 `partial`，均标 `done` 且附完成证据指针；无内部矛盾（Work Item Status 块与 Implementation Order 表一致）。

### Phase 2 - 修正概览与 README 陈旧状态

Status: completed
Targets: `docs/backlog/implementation-roadmap.md`（概览表行 10、11）、`docs/backlog/README.md`（工作项表 P1/P4 行）
Skill: none

- Item Types: `Fix`
- Prereqs: Phase 1（1.1/1.2 已关闭后，概览「M1 partial」表述才无依据）

- [x] `Fix`：`implementation-roadmap.md` 概览表——`core-business-roadmap.md` 行状态改为「M1/M4/M5 全 done（含 1.1/1.2，经状态对账关闭）」；`extended-roadmap.md` 行状态改为「M2/M3 全 done」；移除「M1.12 todo」「2.4b~2.6c todo」等陈旧表述。
  - Skill: none
- [x] `Fix`：`README.md` 工作项表——P1 状态由「🟡 部分 done」改「✅ done」；P4 状态由「❌ todo（下一就绪项）」改「✅ done」。
  - Skill: none

Exit Criteria:

- [x] `implementation-roadmap.md` 概览表与 `core-business-roadmap.md`/`extended-roadmap.md` 子路线图状态零矛盾（不再出现 todo/partial 指向已完成项）。
- [x] `README.md` 工作项表 P1/P4 与子路线图一致。

### Phase 3 - 显化下一功能后继 + AGENTS.md 阶段同步

Status: completed
Targets: `docs/backlog/README.md`（新增工作项行）、`AGENTS.md`（「当前项目阶段」段，若与现状矛盾）
Skill: none

- Item Types: `Add | Fix | Decision`
- Prereqs: Phase 2

- [x] `Decision`：下一功能后继「7 扩展域 posted/businessDate 字段补充」的 backlog 显化方式——**选择**在 `README.md` 工作项表新增一行（优先级 P、工作项名、路线图归属「`2026-07-07-1915-1` Deferred」、状态 `blocked`、触发条件「1915-1 关闭后」、AI 自主权 `ask-first`）。**替代**：仅在 1915-1 的 Deferred 段保留——rejected，因 mission-driver 遍历 `backlog/` 工作项表选择下一切片，不扫 plans 的 Deferred 段，会导致路线图全绿后后继沉没。**残留风险**：1915-1 关闭后需确认该行未被 1915-1 自身关闭流程重复登记。
  - Skill: none
- [x] `Add`：`README.md` 工作项表新增该行（状态 `blocked`，触发条件显式标注，防止误判为 ready）。
  - Skill: none
- [x] `Fix`：`AGENTS.md`「当前项目阶段」段（现 :77 仍写「bootstrap / 预代码生成阶段」，经实时核实为陈旧）——更新阶段描述对齐现状：18 域 codegen + 业务逻辑（M1/M2/M3）+ 业财一体（M4/M5）+ 报表/看板（全 10 域）均已落地，项目处于「业务逻辑深化与运营成熟度收尾」阶段（保留多域目录结构与标准模块链的结构性说明，不动该结构段）。
  - Skill: none

Exit Criteria:

- [x] `README.md` 工作项表显式包含「7 扩展域 posted/businessDate 字段补充」行，状态 `blocked` + 触发条件 + `ask-first` 自主权齐全。
- [x] `AGENTS.md`「当前项目阶段」段不再出现「bootstrap / 预代码生成阶段」表述，与现状一致。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (ses_0c32baf0cffeSGrgJtBjthro5n) because 1 BLOCKER + 3 NOTES：
  - B1（Rule 1 from-live-baseline 违规）：Deferred 段「1915-1 修改同样 7 域 orm.xml」事实错误——实时核实 1915-1 Phase 1(C-1) 仅改 4 域（b2b/cs/logistics/contract），Phase 3(M-1) 仅 `cs` 与本后继 7 域重叠；hr/drp/aps 无直接冲突。已更正为部分冲突 + 双重 codegen 规避理由。
  - N1：DRP 分类误引为 `optimization candidate`，实际 1500-1 裁定为 `out-of-scope improvement`。已更正标签 + 引用 :284-290。
  - N2：Phase 3 AGENTS.md item 为条件式（「若仍描述为…」），但实时核实 AGENTS.md:77 确为陈旧「bootstrap / 预代码生成阶段」，条件已满足。已改为确定式 Fix + 附 :77 证据。
  - N3：Closure Gate grep 未排除显化的 blocked 后继行。已收紧为 `rg ... docs/backlog/ AGENTS.md` 并说明 blocked 行不被误报。
  - 正面确认（无需变更）：mvn 门控移除对纯文档计划合规（指南显式允许）；单一结果表面成立（关闭陈旧 partial + 显化后继同属「backlog/roadmap 为单一真相源」契约，规则 4/14）；无 slack 词；Decision「现在仅显化、不起草后继」可辩护（后继已由 1915-1 active Deferred 拥有）。
- Independent draft review iteration 2: `accept` (ses_0c325c939ffeMW7KPw4N5TKrGS) — 全部 4 项先前发现（B1/N1/N2/N3）经实时仓库复核确认已修复，无新增 BLOCKER/NOTE。附带一处低于阈值的外观观察（Phase 3 头 `Item Types` 原写 `Add | Fix` 但含一个 `Decision` item）已顺带修正为 `Add | Fix | Decision`。计划为可接受的执行契约：单一结果表面、可观察退出标准、Deferred 裁决完整、无范围内项目降级。

## Closure Gates

> 本计划为纯文档/状态对账，无代码、ORM、契约或测试变更，故**移除** `mvn` 构建/测试验证门控（指南允许：无代码变更的计划删除验证命令门控并说明原因）。结束时运行一次文档一致性自检（grep 验证无残留 `partial`/`todo` 指向已完成项）。

- [x] 范围内行为完成（1.1/1.2 关闭 + 概览/README 对齐 + 后继显化 + AGENTS.md 同步）
- [x] 相关文档对齐（`core-business-roadmap.md`/`implementation-roadmap.md`/`README.md`/`AGENTS.md` 状态零矛盾）
- [x] 已运行验证：`rg -n 'partial|todo|预代码生成|bootstrap' docs/backlog/ AGENTS.md` 自检——无指向已完成工作项的残留 `partial`/`todo`，无「预代码生成/bootstrap 阶段」陈旧表述（显化的扩展域字段 `blocked` 行使用 `blocked` 而非 `todo`，故不被误报）
- [x] 无范围内项目降级为 deferred/follow-up（DRP 命名统一为本计划 Non-Goal，引用既有 1500-1 裁定，非新降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 起草「7 扩展域 posted/businessDate 字段补充」实施计划

- Classification: `out-of-scope improvement`（显化为 backlog 工作项，但实施计划不在本对账计划范围内起草）
- Why Not Blocking Closure: 该后继是 active plan `2026-07-07-1915-1` 的 Deferred，触发条件为「1915-1 关闭后」。文件冲突范围经实时核实为**部分**而非全部 7 域：1915-1 Phase 1（C-1）修改其中 4 域（b2b/cs/logistics/contract，见 1915-1 :47/:54-57），Phase 3（M-1）删除冗余 approveStatus 字典涉及 8 域中仅 `cs` 与本后继 7 域重叠（1915-1 :162）。`hr`/`drp`/`aps` 三域与 1915-1 无直接文件冲突。等待 1915-1 关闭的理由为：(1) 避免 b2b/cs/logistics/contract 四域的双重 codegen 重生成；(2) 后继属 ORM 保护区域（`ask-first`），须 1915-1 表名/字典整改定稿后才能在干净基线上起草。本计划已在 `README.md` 显化该行（Phase 3），防止路线图全绿后沉没。
- Successor Required: `yes`
- Trigger Condition: `2026-07-07-1915-1-audit-remediation-plan.md` 关闭后，由下次 mission-driver `draft-from-roadmap` 遍历显化的 backlog 行时起草（hr/drp/aps 无文件冲突，但与 b2b/cs/logistics/contract 同属一个「补齐扩展域标准字段」结果表面，按规则 4 不拆分，统一待 1915-1 关闭后整批起草）。

## Closure

Status Note: 2026-07-07 执行完成。3 Phase 全部完成：
- Phase 1：`core-business-roadmap.md` 中 1.1 Purchase Order BizModel / 1.2 Sales Order BizModel 由 `partial` 关闭为 `done`，附完成证据指针（审批经 use-approval 迁移 2050-1→0315-1；触发经 1132-1/1132-2；过账经 1.5/1.6/1.7；信用控制 AR 段经 1838-1）。Work Item Status 块与 Implementation Order 表两处一致更新。
- Phase 2：`implementation-roadmap.md` 概览表对齐子路线图真实状态（M1/M2/M3/M4/M5 全 done）；`README.md` 工作项表 P1 由「🟡 部分 done」改「✅ done」、P4 由「❌ todo」改「✅ done」。
- Phase 3：`README.md` 新增 P5 行「7 扩展域 posted/businessDate 标准字段补充」状态 `blocked` + 触发条件「`2026-07-07-1915-1` 关闭后」+ `ask-first` 自主权齐全；`AGENTS.md`「当前项目阶段」段由「codegen 已完成、待 BizModel 业务逻辑深化阶段」更新为「业务逻辑深化与运营成熟度收尾阶段」（含已落地能力清单：CRUD 全 18 域、M1/M2/M3 业务逻辑、M4/M5 业财一体、报表子系统、看板子系统）。
- 自检：`rg -n 'partial|todo|预代码生成|bootstrap' docs/backlog/ AGENTS.md` 无指向已完成工作项的残留 `partial`/`todo`，无「预代码生成/bootstrap」陈旧表述（剩余 `todo` 匹配均为 `00-roadmap-authoring-guide.md` / `crud-roadmap.md` 中状态语义定义文本，非工作项状态）。
- 纯文档/状态对账，无代码/ORM/契约/测试变更，故无 `mvn` 验证门控（指南允许）。
- 结束审计待独立子代理（新会话）执行；执行者未自我审计，故保留 2 条审计门控为 `[ ]`。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，未重用执行者上下文；任务输入 `plan-check FAIL → 2 unchecked closure gates`）
- Evidence:
  - 结构性检查：`node ../attractor-guided-engineering-template/tools/mission-driver/src/plan-check.mjs docs/plans/2026-07-07-2143-1-roadmap-backlog-status-reconciliation.md --strict` 修复前 `passed:false, totalUnchecked:2`（恰好为结束审计门控 2 项），修复后重跑确认 PASS（0 unchecked）。
  - Phase 1 实时核实（`docs/backlog/core-business-roadmap.md`）：Work Item Status 块 :14（1.1 `done` + use-approval 迁移 2050-1→0315-1 / 入库触发 1132-1 / 过账 1.5+1.6 证据指针）/ :15（1.2 `done` + 出库触发 1132-2 / 过账 1.5+1.7 / 信用控制 AR 段 1838-1 证据指针）；Implementation Order 表 :48/:49 两行均为 `✅ done` 且证据指针一致。grep 确认无残留 `partial` 指向 1.1/1.2。
  - Phase 2 实时核实：`implementation-roadmap.md` :10（core-business 行「M1/M4/M5 全 done（含 1.1/1.2，经状态对账关闭）」）、:11（extended 行「M2/M3 全 done」），无「M1.12 todo」「2.4b~2.6c todo」陈旧表述；`README.md` :14（P1 `✅ done`）、:17（P4 `✅ done`），与子路线图零矛盾。
  - Phase 3 实时核实：`README.md` :18 新增 P5 行「7 扩展域 posted/businessDate 标准字段补充」状态 `⛔ blocked` + 触发条件「`2026-07-07-1915-1` 关闭后」+ `ask-first`（ORM 保护区域）齐全；`AGENTS.md:77`「当前项目阶段」段为「业务逻辑深化与运营成熟度收尾阶段」（含已落地能力清单），无「bootstrap / 预代码生成阶段」陈旧表述。
  - 反松弛/反空洞：纯文档/状态对账计划，无代码/契约/测试变更，"工作"即文档编辑本身（已逐文件肉眼核实落地，非采信 [x]）。Deferred 段「7 扩展域字段补充」与 Non-Goal「DRP 命名统一」均诚实裁定（前者 `out-of-scope improvement` + 显化为 blocked backlog 行 + 触发条件；后者引用 1500-1 既有裁定 :284-290），无范围内缺陷降级。
  - 五点一致性：Plan Status `completed` / 3 Phase Status 均 `completed` / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]` / Closure 证据已填实 —— 全部一致。

Follow-up:

- 无非阻塞跟进项（本计划范围内的整改已全部落地；7 扩展域字段补充为本计划显化的 next-feature successor，归属 active plan `2026-07-07-1915-1` 的 Deferred，非本计划跟进项）。

Status Note 补充（独立结束审计通过，2026-07-07）：执行者合规保留 2 条结束审计门控为 `[ ]`（规则 12 禁止执行者自我审计）；本独立审计会话（新会话，未重用执行者上下文）逐项核实后已勾选 `[x]` 并填实证据。
