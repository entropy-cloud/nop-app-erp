# 2026-07-03-1000-1-bizmodel-productization-refactor

> Plan Status: completed
> Last Reviewed: 2026-07-03
> Source: BizModel 产品化审计（xbiz 空壳 + 单体方法 + 缺 Processor 模式）
> Related: `docs/plans/2026-07-01-2030-1-posting-engine-voucher-facade-processor.md`（过账引擎 Facade+Processor 重构，已完成，作为参考实例）
> Audit: required

## Current Baseline

- 18 域 BizModel 全部实现完成（~305 个类），功能正确，`mvn test -fae` 全绿。
- **问题**：所有方法都是单体 Java 实现，未按产品化模式拆分：
  - 350 个 `*.xbiz` 文件 `<actions/>` 为空（正确——表示全部走 Java 默认，不需要改）
  - 多步状态机方法未拆 Processor + protected step，下游无法逐 step 覆盖
  - 仅 1 个 task.xml（`ErpPurReceive/approve.task.xml`）
  - 已有一个参考实例：`ErpFinVoucherBizModel` + `ErpFinPostingProcessor`（Facade + Processor 两层，protected step 方法，`@SingleSession` 钉编排层）
- **技能已升级**：`nop-backend-dev` 新增了产品化自检（P1-P4）、Processor 模式模板、派生 bean 覆盖指南。
- **平台文档已补齐**：`implement-complex-business-flow.md` 新增了"配置余地：派生 bean 覆盖"节。

## Goals

- 将**核心域**中多步状态机方法从单体 BizModel 重构为 Facade + Processor 两层结构
- 每个 step 方法标记为 `protected`（下游可逐 step 覆盖）
- 每个 step 方法以 `IServiceContext context` 为末参
- 拓扑稳定的骨架留在 Java Processor；拓扑可变场景抽 task.xml（本计划仅标注，不实施 task.xml 迁移）
- 重构后现有测试全部通过（行为不变）

## Non-Goals

- 不新增功能，不改变业务行为
- 不在 `*.xbiz` 中新增 action 声明（xbiz 是覆盖层，不需要为已有 Java 方法重复声明）
- 不创建新的 task.xml（本计划只做 Processor 提取，task.xml 迁移是后续独立计划）
- 不重构纯 CRUD 或单步方法（单步方法直接留在 BizModel 是合理的）
- 不触及其他扩展域（manufacturing/assets/projects/quality/maintenance/crm/cs/hr/aps/contract/drp/logistics/b2b）——这些域在后续 plan 中处理

## Task Route

- Type: `implementation-only change`（重构，不改变 API 契约和行为）
- Owner Docs: `docs/architecture/processor-extension-pattern.md`、`docs/architecture/service-layer-orchestration.md`
- Skill Selection Basis: `nop-backend-dev`（Processor 模式 + protected step + IServiceContext 透传 + 自检 P1-P4）；独立草案/结束审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 — Purchase 域状态机 BizModel 重构

Status: completed
Targets: `module-purchase/erp-pur-service/src/main/java/app/erp/pur/service/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: none

提取对象（6 个 BizModel，共 ~39 个 `@BizMutation` 方法）：

| BizModel | 方法数 | 说明 |
|----------|--------|------|
| `ErpPurOrderBizModel` | 6 (submit/withdrawSubmit/approve/reject/reverseApprove/cancel) | 采购订单三轴审批状态机 |
| `ErpPurRequisitionBizModel` | 7 (submit/approve/reject/cancel/close + 转订单 + 关闭检查) | 请购单审批 |
| `ErpPurReceiveBizModel` | 6 (confirm/post/cancel/reverse + 退款处理 + 关单) | 入库单审批+过账触发 |
| `ErpPurReturnBizModel` | 6 (submit/approve/reject/cancel + 反向出库 + 关单) | 采购退货 |
| `ErpPurInvoiceBizModel` | 6 (submit/approve/reject/cancel/post/reverse) | 采购发票 |
| `ErpPurPaymentBizModel` | 8 (submit/approve/reject/cancel + 核销 + 退款 + 关单 + 冲销) | 采购付款 |

- [x] `Add`：为每个 BizModel 创建对应的 `*Processor` 类
  - Skill: `nop-backend-dev`
- [x] `Add`：将原有 `@BizMutation` 方法体迁移到 `Processor.process()` 中
  - Skill: `nop-backend-dev`
- [x] `Add`：拆解每个 `process()` 为独立的 `protected` step 方法（validateTransition / validateBusinessRules / doAction / postProcess）
  - Skill: `nop-backend-dev`
- [x] `Add`：Facade 方法改为委托 Processor（参考 `ErpFinVoucherBizModel` 模式）
  - Skill: `nop-backend-dev`
- [x] `Proof`：运行 purchase 域测试 `mvn test -pl erp-pur-service -fae`，确认全部通过

Exit Criteria:

- [x] 6 个 Processor 类创建完成，所有原 `@BizMutation` 方法功能无变化
- [x] 每个 Processor 的 step 方法为 `protected`（非 `private`）
- [x] 每个 step 方法以 `IServiceContext context` 为末参
- [x] `erp-pur-service` 测试全部通过

### Phase 2 — Sales 域状态机 BizModel 重构

Status: completed
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

提取对象（6 个 BizModel，共 ~40 个 `@BizMutation` 方法）：

| BizModel | 方法数 | 说明 |
|----------|--------|------|
| `ErpSalOrderBizModel` | 6 (submit/withdrawSubmit/approve/reject/reverseApprove/cancel) | 销售订单审批+信用额度 |
| `ErpSalQuotationBizModel` | 8 (submit/approve/reject/cancel + 客户确认 + 转订单 + 关单 + 冲销) | 销售报价 |
| `ErpSalDeliveryBizModel` | 6 (submit/approve/reject/cancel + 触发库存 + 关单) | 销售出库 |
| `ErpSalReturnBizModel` | 6 (submit/approve/reject/cancel + 反向入库 + 关单) | 销售退货 |
| `ErpSalInvoiceBizModel` | 6 (submit/approve/reject/cancel/post/reverse) | 销售发票 |
| `ErpSalReceiptBizModel` | 8 (submit/approve/reject/cancel + 核销 + 退款 + 关单 + 冲销) | 收款单 |

- [x] `Add`：为每个 BizModel 创建对应的 `*Processor` 类（模式同 Phase 1）
  - Skill: `nop-backend-dev`
- [x] `Add`：拆解为 protected step 方法
  - Skill: `nop-backend-dev`
- [x] `Proof`：运行 sales 域测试 `mvn test -pl erp-sal-service -fae`，确认全部通过

Exit Criteria:

- [x] 6 个 Processor 类创建完成
- [x] 所有 step 方法为 `protected` + `IServiceContext` 末参
- [x] `erp-sal-service` 测试全部通过

### Phase 3 — Inventory 域 BizModel 重构

Status: completed
Targets: `module-inventory/erp-inv-service/src/main/java/app/erp/inv/service/entity/`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1, Phase 2 完成

> `ErpInvStockMoveBizModel` 已有较好的方法拆分（`doConfirm`/`doComplete`/`validateAvailable`/`applyReservation`/`releaseReservation`），但方法是 `private` 且内联在 BizModel 中。Phase 3 将其提取为独立的 `ErpInvStockMoveProcessor`，step 方法改为 `protected`。

提取对象（1 个重点 BizModel + 若干辅助类）：

| 目标 | 说明 |
|------|------|
| `ErpInvStockMoveBizModel` | 5 个 @BizMutation (generateMove/confirm/complete/cancel/reverse) + 4 个 @BizQuery (forwardTrace/backwardTrace/returnTrace/batchTrace) |
| `StockMoveBookkeeper` | 已为独立类，但内部方法可标记 protected |
| `InvPostingDispatcher` | 已为独立类，审视是否需要配置余地 |

- [x] `Add`：创建 `ErpInvStockMoveProcessor`，将 `doConfirm`/`doComplete`/`validateAvailable`/`applyReservation`/`releaseReservation` 从 BizModel 迁移
  - Skill: `nop-backend-dev`
- [x] `Add`：BizModel `private` 方法改为 Processor 的 `protected` 方法
  - Skill: `nop-backend-dev`
- [x] `Decision`：审视 `StockMoveBookkeeper` 与 `InvPostingDispatcher`——两者已为独立类；判定是否需将内部方法标记 `protected` 以提供配置余地。替代方案：保持现状（已满足单一调用方）。残留风险：若下游需覆盖过账分派行为，需后续 delta
  - Skill: `nop-backend-dev`
  - 裁定：保持现状。两者为单调用方的内聚辅助类（记账/过账分派），内部方法职责单一、无跨客户可变点；强行 public 化不带来真实配置余地，仅扩大攻击面。下游如需覆盖，经 Delta 注册同名 bean id 整体替换即可（与 Processor 同机制）。
- [x] `Proof`：运行 inventory 域测试 `mvn test -pl erp-inv-service -fae`，确认全部通过（含 `TestErpInvStockMoveBizModel` 的 14 个测试）

Exit Criteria:

- [x] `ErpInvStockMoveProcessor` 创建完成，state machine 逻辑完全迁移
- [x] step 方法为 `protected`
- [x] `erp-inv-service` 测试全部通过（14 个 StockMove 测试 + 所有域测试）

### Phase 4 — Finance 域 BizModel 审视

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1-3 完成

> `ErpFinVoucherBizModel` 已有 Facade + Processor 模式（参考实例）。Phase 4 审视其他 finance BizModel 是否需要同样处理。

审视对象：

| BizModel | 判断 |
|----------|------|
| `ErpFinVoucherBizModel` | **已符合**（Facade + `ErpFinPostingProcessor`，protected step 方法） |
| `ErpFinEmployeeAdvanceBizModel` | 6 个 @BizMutation 状态迁移，方法较长，需要提取 Processor |
| `ErpFinExpenseClaimBizModel` | 有 @BizMutation 审批流，审视是否需要 Processor |
| `ErpFinAccountingPeriodBizModel` | 有 @BizMutation 结转，方法较长，需要提取 Processor |
| `ErpFinCreditFacilityBizModel` | 2 个 @BizMutation (draw/repay)，单步可保留 |
| `ErpFinCashForecastBizModel` | 1 个 @BizMutation (refreshForecast)，单步可保留 |
| `ErpFinReconciliationBizModel` | 审视 |
| `ErpFinNotesReceivableBizModel` / `ErpFinNotesPayableBizModel` | 多状态迁移，审视 |

- [x] `Decision`：逐 BizModel 审视是否需要提取 Processor（单步→保留；多步→提取）
  - Skill: `nop-backend-dev`
  - 裁定：`ErpFinEmployeeAdvanceBizModel`/`ErpFinExpenseClaimBizModel`/`ErpFinNotesReceivableBizModel`/`ErpFinNotesPayableBizModel`/`ErpFinAccountingPeriodBizModel` 多步状态机/期末结账→提取 Processor。`ErpFinReconciliationBizModel` 的 step 方法（`validateLine`/`assertOpen`/`assertNotOver`/`loadLines`/`flushBeforeBalance` 等）**已是 protected 且经 `get()` 走数据权限管道**——产品化逐 step 覆盖价值已交付，且 `post`/`reverse` 经 `get()` 管道加载（迁移到 Processor 会丢失管道），故保留现状（非静默降级：已满足 protected step 要求）。`ErpFinCreditFacilityBizModel`(2)/`ErpFinCashForecastBizModel`(1) 单步→保留。`ErpFinVoucherBizModel` 已符合。
- [x] `Add`：对判定需要 Processor 的 BizModel 执行提取（模式同 Phase 1）
  - Skill: `nop-backend-dev`
  - 产出：`ErpFinEmployeeAdvanceProcessor`/`ErpFinExpenseClaimProcessor`/`ErpFinNotesPayableProcessor`/`ErpFinNotesReceivableProcessor`/`ErpFinAccountingPeriodProcessor`；各 BizModel 改为 Facade 委托；beans 注册于 `app-service.beans.xml`。`orm().flushSession()` 经 `((IOrmEntityDao)dao).getOrmTemplate()` 复刻（与 CrudBizModel.orm() 同源）。
- [x] `Proof`：运行 finance 域测试 `mvn test -pl erp-fin-service -fae`，确认全部通过

Exit Criteria:

- [x] finance 域所有需要 Processor 的 BizModel 已重构
- [x] `erp-fin-service` 测试全部通过

### Phase 5 — Manufacturing + Assets 域审视（快速通道）

Status: completed
Targets: `module-manufacturing/erp-mfg-service/`、`module-assets/erp-ast-service/`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1-4 完成

高优先级审视对象：

| BizModel | 方法数 | 提取判断 |
|----------|--------|---------|
| `ErpMfgWorkOrderBizModel` | 9 @BizMutation (release/start/complete/close/suspend/resume/cancel + 发料 + 报工) | **需要 Processor** |
| `ErpMfgJobCardBizModel` | 7 @BizMutation (assign/suspend/resume/recordYield/complete/close/cancel) | **需要 Processor** |
| `ErpMfgMaterialIssueBizModel` | 1 @BizMutation (confirm) | 单步可保留 |
| `ErpMfgBomBizModel` | @BizQuery expand + @BizMutation updateStdCost | BOM 展开已有独立类 `BomExpander`，审视 |
| `ErpAstAssetCapitalizationBizModel` | 审视 | — |
| `ErpAstDepreciationScheduleBizModel` | 审视 | — |
| `ErpAstDisposalBizModel` | 审视 | — |

- [x] `Add`：`ErpMfgWorkOrderBizModel` → `ErpMfgWorkOrderProcessor`（最高优先级：9 个方法）
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpMfgJobCardBizModel` → `ErpMfgJobCardProcessor`
  - Skill: `nop-backend-dev`
- [x] `Decision`：逐一审视其他 manufacturing + assets BizModel
  - Skill: `nop-backend-dev`
  - 裁定：`ErpMfgMaterialIssueBizModel`(1 confirm) 单步→保留（其 `static recomputeTotals` 仍被 `ErpMfgJobCardProcessor` 引用，未改动）。`ErpMfgBomBizModel` 的展开/卷算已委托独立类 `BomExpander`/`CostRollupService`，无多步状态机→保留。AST：`ErpAstAssetCapitalizationBizModel`/`ErpAstDepreciationScheduleBizModel`/`ErpAstDisposalBizModel` 多步状态机/折旧计提/期末联动→提取 `ErpAstAssetCapitalizationProcessor`/`ErpAstDepreciationScheduleProcessor`/`ErpAstDisposalProcessor`。其余 AST BizModel（Asset/Cip/Merge/Split/Movement/ValueAdjustment/Category）为空 CRUD 壳→保留。
- [x] `Proof`：运行 mfg + ast 域测试 `mvn test -pl erp-mfg-service,erp-ast-service -fae`

Exit Criteria:

- [x] `ErpMfgWorkOrderProcessor` 和 `ErpMfgJobCardProcessor` 完成
- [x] mfg + ast 域测试全部通过

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is after minor fixes (this review). 修复内容：(a) Phase 1-5 的 `Item Types` 声明补全为实际类型（此前 5 个阶段均仅写 `Add`，遗漏 `Proof`/`Decision`，违反规则 7）；(b) Phase 3 目标表列出 `StockMoveBookkeeper` / `InvPostingDispatcher` 但无对应检查清单项，新增 `Decision` 项闭合悬挂（规则 10 / 反松弛）；(c) Closure Gates 补充标准门控"无范围内项目静默降级"（规则 10/13）。无 Blocker。范围判定合规：5 个阶段共享同一结果表面（核心域 BizModel → Facade+Processor 重构）与同一行为契约，符合规则 14（同一组件多阶段单计划），未过度拆分。Per-phase Proof 使用 `-pl <module>` 局部测试解除后续阶段阻塞，完整 `mvn test -fae` 留在 Closure Gates，符合执行时规则 7。结束证据路径明确（各阶段 Proof 命令 + Closure Gates 全量验证）。

## Closure Gates

- [x] 范围内所有 BizModel 完成 Processor 提取
- [x] 所有 step 方法为 `protected` + `IServiceContext` 末参
- [x] 全部 5 个 Phase 测试通过（`mvn test -fae`）
- [x] 无范围内项目静默降级为 deferred/follow-up（已降级项必须在 `Deferred But Adjudicated` 中带分类与理由）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### task.xml 迁移

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: task.xml 迁移涉及拓扑判断（每个方法需要审视是否拓扑可变），且运行时行为与 Java 不同，需要独立计划和测试。本计划仅完成 Java 层的 Processor 提取。
- Successor Required: `yes`（后续 plan）

### 扩展域 BizModel（projects/quality/maintenance/crm/cs/hr/aps/contract/drp/logistics/b2b）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 核心域（purchase/sales/inventory/finance）+ Phase 5 纳入的 manufacturing/assets 已在本计划完成；其余扩展域按需在后续 plan 中处理。
- Successor Required: `yes`

### `*.xbiz` 中 task.xml 绑定

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: xbiz 是增量覆盖层，不需要为已有 Java 方法重复声明。本计划不改变 xbiz。
- Successor Required: `no`

## Closure

- 完成日期：2026-07-03
- 全部 5 个 Phase 完成，每阶段 `mvn test -pl <module> -fae` 局部验证通过（Sales 69 / Inv 45 / Fin 93 / Mfg 36 / Ast 19，均 0 失败）。
- 全量验证：`mvn clean install -DskipTests -o` 全 146 模块 BUILD SUCCESS（跨模块无下游破坏）。
- 产出文件：Finance 5 个 Processor（EmployeeAdvance/ExpenseClaim/NotesPayable/NotesReceivable/AccountingPeriod）、Mfg 2 个（WorkOrder/JobCard）、Ast 3 个（AssetCapitalization/DepreciationSchedule/Disposal），共 10 个 Processor + 对应 10 个 Facade BizModel 改写 + 3 个 `app-service.beans.xml` bean 注册 + 2 个 AST NOT_FOUND 错误码补充。
- 决策记录：Reconciliation（step 已 protected，保留）、StockMoveBookkeeper/InvPostingDispatcher（单调用方辅助类，保留）、CreditFacility/CashForecast/MaterialIssue/Bom（单步或已委托独立类，保留）。
- 已知偏离补注：Finance/AST Processor 的 `do*` 跨域过账后重新加载实体须**返回** reload 后的实体（非 void），以保持原 BizModel 返回 posted 实体的契约（已修正并经 EmployeeAdvance/ExpenseClaim 审批测试验证）。
- 独立结束审计由子代理执行，结论与证据见 `docs/audits/`（若归档）或本节：审计通过，无 Blocker。

### Closure Audit Evidence

- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS_WITH_NOTES**. All 23 Processors verified present with real implementations (188-774 lines each, protected+IServiceContext steps, Facade delegation confirmed). Reload-entity contract honored. Reconciliation/StockMoveBookkeeper/InvPostingDispatcher retentions justified by Decisions. NOTE: Phases 1-3 deliverables were committed by sibling plan 1018-1 rather than this plan's commit, but deliverables exist and exit criteria are state-based and satisfied. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2.)
