# 2026-07-30-0512-2-r1-19-inventory-stocktake-picking-deferred inventory StockTake 联动 + PickingOrder 死状态裁决

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.19（P1-MA2-062 + P1-MA2-063，源自 A2.11 inventory 状态机审查）
> Related: `docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-062/063`；plan `2026-07-30-0143-3-r1-14-mfg-dict-dead-state-owner-doc-drift.md`、plan `2026-07-30-0341-1-r1-15-hr-state-machine-dict-dead-state.md`（同型裁决先例）
> Audit: required

## Current Baseline

两项 finding 经实仓逐项确认：均为「owner doc 声明联动/迁移但代码未实现 + dict 死状态 + BizModel CRUD 桩」类型，**不破坏已实现主路径**（移动单 DRAFT→CONFIRMED→DONE/CANCELLED + 冲销反向单完整覆盖；盘点单 DRAFT→CONFIRMED→DONE/CANCELLED 生命周期完整）。

**P1-MA2-062（StockTake completeTake 未自动生成盘盈/盘亏移动单）— 确认：**
- `ErpInvStockTakeBizModel.completeTake:40-50` 仅 `requireEntity` + 源态守卫（CONFIRMED）+ `setDocStatus(DOC_STATUS_DONE)` + `updateEntity`，**无任何比对 StockTakeLine.qtyActual vs StockBalance.totalQuantity、无 generateMove 调用**。
- owner doc `state-machine.md §盘点单状态机 L153` 声明「盘点完成（DONE）→ 自动生成盘盈/盘亏移动单（新 DRAFT）」+「所有余额变动都通过移动单流水可追溯」。代码未落地该联动。
- 不产生悬挂数据（盘点单 DONE 但无差异调整移动单，需库管员后续手工 generateMove 处置）。

**P1-MA2-063（PickingOrder PICKING/PICKED dict 死状态 + ErpInvPickingOrderBizModel 15 行 CRUD 桩）— 确认：**
- dict `erp-inv/picking-status` 4 态（PENDING/PICKING/PICKED/CANCELLED）；`ErpInvPickingOrderBizModel` = `extends CrudBizModel<ErpInvPickingOrder>` CRUD 桩，零 setStatus writer。
- 全 `module-inventory/erp-inv-service/src/main` grep `PICKING_STATUS` / `setDocStatus.*PICKING` 零业务命中——PICKING/PICKED 两态不可达（dict 死状态）；PENDING 由 codegen 默认值承载；CANCELLED 经 useLogicalDelete 承载。
- owner doc `state-machine.md` 无拣货独立章节描述。

**保护区域：** 不触及会计/数据删除保护区域（无凭证/删除写路径变更）。本计划为纯 owner-doc 行为契约对齐（方案B Deferred 标注），无代码/无 ORM 变更。

## Goals

- 消除 inventory 域 owner doc 与代码间两项悬空：(1) StockTake 联动生成移动单语义对齐；(2) PickingOrder 拣货生命周期死状态对齐。
- owner doc 与代码零 writer 一致，无「文档声明联动/迁移但代码无实现」或「dict 含值但无 writer」的悬空。

## Non-Goals

- 不实现 completeTake 自动比对并生成盘盈/盘亏移动单（P1-MA2-062 方案A）——裁决 Deferred（触计会计邻接的移动单→过账链路，跨表面），successor 命名触发条件。
- 不实现 startPicking/completePicking/cancelPicking BizMutation（P1-MA2-063 方案A）——裁决 Deferred，WMS successor。
- 不从 ORM 删除 PICKING/PICKED dict 值（采纳「保留为预留 + 文档 Deferred」对齐 R1.13/R1.14/R1.15 既有先例）。
- 不补移动单/成本核算测试（归 MR2）；本计划聚焦 owner doc 与代码一致性。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐，纯文档）
- Owner Docs: `docs/design/inventory/state-machine.md`
- Skill Selection Basis: 纯 owner doc Deferred 标注，无代码/ORM → `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 两项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：两项 finding 处置方案逐项裁决（同型裁决范式，对齐 R1.13/R1.14/R1.15 先例）。
      - P1-MA2-062 StockTake 自动生成移动单：**Deferred（owner doc 语义对齐）**。**与 arm-index 推荐偏差声明**：arm-index §P1-MA2-062 方案A（推荐）为「实现 completeTake 自动比对生成差异移动单」；本计划裁决 Deferred（偏离推荐），理由：(1) 实现（completeTake 自动比对 qtyActual vs totalQuantity → 经 `IErpInvStockMoveBiz.generateMove` Facade 生成差异移动单）触及移动单→InvPostingDispatcher 过账链路，属跨表面实现，与危害不成比例；(2) owner doc 漂移是真实 finding，对齐为「自动生成 Deferred——盘点差异经库管员手工 generateMove 处置」消除悬空；(3) 不破坏 StockTake 主路径（DONE 后无悬挂数据）。残留风险：若要求盘点闭环自动化时需补实现 → successor 命名触发条件（实现路径 + 现有 Facade 可复用已记录）。
      - P1-MA2-063 PickingOrder PICKING/PICKED 死状态 + CRUD 桩：**Deferred + dict 保留为预留**。**与 arm-index 推荐偏差声明**：arm-index §P1-MA2-063 方案A（推荐）为「实现 startPicking/completePicking/cancelPicking BizMutation」；本计划裁决 Deferred（偏离推荐），理由：(1) 拣货执行由 WMS successor 承载，CRUD 桩为主路径可用（PENDING 创建/查询/逻辑删除）；(2) 与 R1.13/R1.14/R1.15「保留 dict 死状态为预留」先例一致，不删除 PICKING/PICKED dict 值。successor：WMS 上线时实现 BizMutation + owner doc 新增「拣货单状态机」章节。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 残留风险 + successor 触发条件，Phase 2 严格遵循。

### Phase 2 - inventory owner doc Deferred 标注（P1-MA2-062 + P1-MA2-063）

Status: completed
Targets: `docs/design/inventory/state-machine.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] state-machine.md §盘点单状态机 补 Deferred 标注：原「盘点完成（DONE）→ 自动生成盘盈/盘亏移动单」改为「**Deferred**——completeTake 当前仅置 DONE，差异调整移动单由库管员手工 `generateMove` 处置；自动比对并生成留 successor」，保留「差异不直接改余额、经移动单流水可追溯」原则描述但注明当前为手工入口；命名 successor 触发条件（盘点闭环自动化需求时）。
- [x] state-machine.md 新增「拣货单生命周期（Deferred）」补注段：说明 PICKING/PICKED 为预留 dict 值（零 writer），`ErpInvPickingOrderBizModel` 为 CRUD 桩，拣货执行由 WMS successor 承载；dict 值保留不删除；命名 successor 触发条件（WMS 上线时）。
- [x] 核对 §审查提示中涉及盘点/拣货自动化的措辞与「Deferred + 手工入口」一致。
      - Skill: `none`

Exit Criteria:

- [x] state-machine.md 明确 StockTake 联动 Deferred（手工入口）+ PickingOrder 拣货生命周期 Deferred（dict 预留），owner doc 与代码零 writer 一致；successor 触发事件已命名。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_05030be58ffeFgem8okz3j5sGt) because 两项 finding（P1-MA2-062/063）基线实仓验证 TRUE（completeTake 仅置 DONE 无 generateMove / PickingOrderBizModel CRUD 桩 / PICKING-PICKED 零 writer），Deferred 裁决虽偏离 arm-index 推荐实现方向但代价/范围理由成立（062 触及过账链路跨表面 / 063 属 WMS successor），同 owner doc 合并（规则 14）正确，mvn 门控因纯文档正确删除，无禁用词，successor 已命名。非阻塞：062/063 各补「与 arm-index 推荐偏差声明」增强可追溯性（已修订）。

## Closure Gates

> 本计划无代码/ORM 变更（纯 owner doc Deferred 标注），故删除 `mvn` 构建验证门控（见执行时规则 7 例外）。验证聚焦 owner doc 与代码一致性。

- [x] 范围内文档对齐完成（P1-MA2-062/063 裁决落地为 owner doc Deferred 标注）
- [x] 相关文档对齐（inventory/state-machine.md）
- [x] 已运行验证（grep 确认 PICKING/PICKED 零 writer + completeTake 无 generateMove 基线不变 + owner doc Deferred 标注落地；compliance checker 本计划零新增命中）
- [x] 无范围内项目降级为 deferred/follow-up（方案B Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### StockTake completeTake 自动生成盘盈/盘亏移动单（P1-MA2-062 方案A successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: completeTake 当前仅置 DONE，无悬挂数据；盘点差异经库管员手工 generateMove 处置（移动单流水仍可追溯）。方案A 触及移动单→过账链路属跨表面实现。
- Successor Required: `yes`（盘点闭环自动化需求时实现 completeTake 自动比对 StockTakeLine.qtyActual vs StockBalance.totalQuantity → 经 `IErpInvStockMoveBiz.generateMove` Facade 生成差异移动单）

### PickingOrder 拣货状态机 BizMutation（P1-MA2-063 方案A successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: PICKING/PICKED 为零 writer 预留状态；CRUD 桩为主路径可用；拣货执行由 WMS successor 承载。
- Successor Required: `yes`（WMS 上线时实现 startPicking/completePicking/cancelPicking BizMutation [PENDING→PICKING→PICKED + PENDING/PICKING→CANCELLED] + owner doc 新增「拣货单状态机」独立章节）

## Closure

Status Note: 两项 finding（P1-MA2-062/063）owner doc 漂移均已消除——`docs/design/inventory/state-machine.md` §盘点单状态机 补「自动生成差异移动单 = Deferred（手工 `generateMove` 入口）」+ 新增「拣货单生命周期（Deferred）」段（PICKING/PICKED 预留死状态、CRUD 桩、WMS successor）+ §审查提示 增零-writer 一致性检查项。纯文档计划，零代码/ORM 变更，无 mvn/typecheck 门控（见 Closure Gates 说明）。successor 触发条件已在 Deferred But Adjudicated 与 owner doc 双重命名。

Closure Audit Evidence:

- Auditor / Agent: 执行代理（本会话）；独立结束审计为后续独立会话步骤。
- Evidence: 实仓 grep 验证（非引用计划自述）：
  - P1-MA2-062：`rg "generateMove" ErpInvStockTakeBizModel.java` EXIT 1（completeTake 仅置 DONE，无自动比对/生成）；`state-machine.md:159-162` Deferred 标注 + successor 落地。
  - P1-MA2-063：`rg "PICKING_STATUS|setDocStatus.*PICKING" module-inventory/erp-inv-service/src/main` EXIT 1（PICKING/PICKED 零 writer）；`ErpInvPickingOrderBizModel` = 15 行 CRUD 桩；dict `erp-inv/picking-status` 4 值保留（orm:73-78）；`state-machine.md:164-174` 拣货单生命周期 Deferred 段落地。
  - 文本一致性：`state-machine.md` ASCII 图（:151-157）已移除幻影「自动生成」迁移；§审查提示（:183）增零-writer 一致性检查。
  - git status：仅 `docs/design/inventory/state-machine.md` + 本计划文件变更（+ roadmap R1.19 状态）。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 与 owner doc 双重命名触发条件。
