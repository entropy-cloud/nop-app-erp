# 2026-08-06-1708-2 rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence RC-9 反结账审计轨迹缺失实际合规影响与降级审计证据评估

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.20（MA4 运行时行为验证 — A1.6 §7 存疑点 3：UC-FIN-07 RC-9 反结账全程审计[操作人/原因]缺失——无操作人/原因/时间记录在外部审计/税务/SOX 合规场景下的可追溯性破坏程度；当前 `ErpFinAccountingPeriod` 通用 `updatedBy`/`updateTime` 审计列是否被反结账操作覆盖以提供降级审计证据，闭合 P1-RC-006 修复方案优先级裁决）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.20；存疑点来源 `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 3
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/plans/2026-08-06-1708-1-rc-ma4-a4-1-19-pc4-depreciation-autoexecute-vs-dangling-block-interaction.md`（A4.1.19 同批次 period-close 同切片 A1.6）、`docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md`（A1.6 报告 §2.11 RC-9 完全缺失 + §5.2 RC-9 P1 + §5.3 新建 P1-RC-006 + §6.1 P1-RC-006 新建裁决 + §7 存疑点 3 + §6.3 P1-RC-006 → MR1[ORM 结构变更 ask-first]）、`docs/design/finance/period-close.md §反结账约束 :223-228`（L2 设计参考）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.20 验证报告（落盘 `docs/audits/2026-08-06-1708-rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `reverseClose` 审计字段写入 + `ErpFinAccountingPeriod` ORM `updateTimeProp/updaterProp` 平台自动填充机制 + 通用 `updatedBy`/`updateTime` 是否被反结账覆盖提供降级证据 + 既有测试普查）。范式对齐 A4.1.18（done — period-close 运行时行为评估同型工作项）。

- **存疑点原文**（A1.6 报告 §7 存疑点 3，`2026-08-02-2100-...-a1-6-period-close.md` §7）：「RC-9 反结账审计缺失的实际合规影响——无操作人/原因/时间记录在外部审计/税务/SOX 合规场景下的可追溯性破坏程度；当前 `ErpFinAccountingPeriod` 通用 `updatedBy`/`updateTime` 审计列是否被反结账操作覆盖可作部分证据」。触发条件 = 实际反结账操作发生时（生产环境）。交 A4.1 运行时验证（确认 `updatedBy`/`updateTime` 是否被覆盖以提供降级审计证据，闭合 P1-RC-006 修复方案的优先级裁决）。

- **关联既有 finding**：
  - **P1-RC-006**（arm-index，A1.6 §5.3 新建）：UC-FIN-07 RC-9 反结账全程审计[操作人/原因]完全缺失——`reverseClose(periodId, context)` 无 reason 参数 + ORM 无 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 + 无 `ReverseCloseLog` 实体，全仓 grep 0 命中。§2 P1①（功能完全缺失）+ §2 P1⑤（验收标准无断言）。目标 MR1（修复触及 ORM 结构变更须 ask-first + 独立 plan-audit）。本验证评估**实际合规影响 + 是否存在降级审计证据**（通用 `updatedBy`/`updateTime` 经平台自动填充），闭合 P1-RC-006 修复方案的**优先级裁决**（不修改 P1 分级——RC-9 缺失已确认 P1；本验证只确认降级证据存在性 + 合规影响程度以指导 MR1 优先级，**不降级 P1**）。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:129`（UC-FIN-07 heading）/ `:135`（RC-9 逐字）「全程审计(记录反结账操作人/原因)」。L2 `period-close.md §反结账约束 :223-228`（管理员+审批要求，设计参考）。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实，HEAD 经独立子代理实测，全在 module-finance）**：
  - `reverseClose`（`ErpFinAccountingPeriodReverseCloseProcessor.java:22-59`）：**无 reason 参数**（签名 `reverseClose(Long periodId, IServiceContext context)`，接口 `IErpFinPeriodCloseBiz.java:45` + Facade `ErpFinAccountingPeriodBizModel.java:70-71` 一致）；方法内**无** `setReversedBy`/`setReverseCloseReason`/`setReverseCloseAt` 调用（字段不存在）；**无** `ReverseCloseLog` 实体写入。方法对 period 实体的写入 = `:39 setStatus(OPEN)` + `:55-56 findOrCreatePeriodStatus + reopenModules` + `:57 flushSession()`。
  - `ErpFinAccountingPeriod` ORM（`app-erp-finance.orm.xml:655-694`）：entity 声明 `updateTimeProp="updateTime" updaterProp="updatedBy"`（:657）→ 平台 `IDaoEntityListener`/审计拦截器在**每次 entity update/flush** 时**自动填充** `updatedBy`（from `IUserContext`）+ `updateTime`（from `CoreMetrics`）。审计列实际存在：`closedBy`(:670)/`closedAt`(:671)/`updatedBy`(:676)/`updateTime`(:677) + std `createdBy`/`createTime`/`version`。**`reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列不存在**（:659-677 列清单无此字段）。
  - 全仓 grep `reversedBy|reverseCloseReason|reverseCloseAt|ReverseCloseLog|reverseCloseLog`（module-finance 全量，排除 docs/）= **0 命中**（A1.6 §2.11 已确认，本计划 live 复核维持）。
  - **降级审计证据（本存疑点关键变量）**：reverseClose 的 `setStatus(OPEN)` + `reopenModules`（修改 period + PeriodStatus 行）+ `flushSession()` 触发平台自动填充 `updatedBy`/`updateTime`——即 `erp_fin_accounting_period.UPDATED_BY`/`UPDATE_TIME` **会被反结账操作覆盖**（记录操作人 + 时间戳）。但这是**通用更新追踪**（generic update-tracking），非反结账专属审计：①无 reason；②无专属反结账时间戳（`updateTime` = 任意更新的时间，非反结账时间）；③被任何后续无关更新覆盖（不可靠的反结账审计轨迹）。`ErpFinAccountingPeriod` entity tagSet（:657）= `gid,erp.finance`，**无 `audit,audit-save` tagSet** → 不被平台 audit-log writer 覆盖（无结构化操作日志）。

- **既有证据（复用输入）**：
  - A1.6 §2.11 + §5.2 + §5.3 + §6.1：RC-9 完全缺失已静态确认（无 reason 参数 + ORM 无审计列 + 无 ReverseCloseLog）；P1-RC-006 新建 P1（§2 P1① 功能完全缺失 + §2 P1⑤ 验收标准无断言）。本验证补「降级审计证据[updatedBy/updateTime 平台自动填充]存在性 + 实际合规影响程度」差异。
  - A2.3 period-close E2E（`2026-07-27-1949-arm-ma2-period-close-e2e.md`）：反结账行为（余额还原 + 模块回开 + PL 红冲）已证实，未审审计轨迹（RC-9 不属 MA2 行为轴）。

- **剩余差距**：RC-9 的实际合规影响未验证——①通用 `updatedBy`/`updateTime` 经平台自动填充是否被反结账覆盖提供降级证据；②降级证据的可靠性边界（无 reason + 被后续更新覆盖 + 非专属时间戳）；③外部审计/税务/SOX 合规场景下反结账无专属审计轨迹的可追溯性破坏程度。本验证闭合 P1-RC-006 修复方案优先级裁决（不降级 P1——RC-9 缺失已确认；只确认降级证据以指导 MR1 优先级排序）。

- **保护区域**：只读评估（读 reverseClose 审计字段写入 + ORM 自动填充机制 + 既有测试普查），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（P1-RC-006 修复触及 ORM 结构变更，归 MR1，须 ask-first + 独立 plan-audit §5）。

## Goals

- reverseClose 审计字段写入核验：核验 `ReverseCloseProcessor.reverseClose:22-59` 对 period 实体的写入（`:39 setStatus(OPEN)` + `:55-56 reopenModules` + `:57 flushSession`）——确认无 reason 参数 + 无 reversedBy/Reason/At setter + 无 ReverseCloseLog 写入（RC-9 专属审计完全缺失）。
- 通用 `updatedBy`/`updateTime` 降级审计证据评估（本存疑点核心）：核验 `ErpFinAccountingPeriod` ORM `updateTimeProp="updateTime" updaterProp="updatedBy"`（orm.xml:657）→ 平台自动填充机制——确认 reverseClose 的实体修改 + flushSession **是否**触发 `updatedBy`/`updateTime` 被反结账操作覆盖（提供降级证据：操作人 + 时间戳）；评估降级证据的可靠性边界（①无 reason；②`updateTime` = 任意更新时间非反结账专属；③被后续无关更新覆盖；④entity 无 `audit,audit-save` tagSet 故无结构化操作日志）。
- RC-9 实际合规影响程度评估：评估反结账无专属审计轨迹[操作人/原因/时间]在外部审计/税务/SOX 合规场景下的可追溯性破坏程度——降级证据[通用 updatedBy/updateTime]是否部分满足合规最低要求（操作人可追 + 时间可追但 reason 不可追 + 易被覆盖），还是完全不可追。
- 既有测试覆盖边界普查：grep 反结账审计相关测试（`updatedBy`/`updateTime` 被反结账覆盖的断言 + ReverseCloseLog 测试）全集，产出测试覆盖边界清单 + 标注降级证据断言缺口（零覆盖）。
- 闭合 P1-RC-006 修复方案优先级裁决（方法论 §2 判据 + 三源对照）：①确认 RC-9 专属审计完全缺失（P1-RC-006 P1 分级维持，不降级——降级证据是**通用**追踪非 RC-9 专属审计，§2 P1① 功能完全缺失成立）；②评估降级证据存在性以指导 MR1 优先级（有降级证据 → P1 优先级可排在数据破坏类 P0 之后但仍须实现，因 reason 不可追是合规硬伤）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.6 §5.3 P1-RC-006 新建 P1 + §6.3 MR1[ORM ask-first] 一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 P1-RC-006**（修复 = ORM 增 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 或 新增 `ErpFinReverseCloseLog` 实体 + BizModel/Processor/契约 reason 参数；触及 ORM 结构变更 + 会计过账逻辑[反结账] 须 ask-first + 独立 plan-audit §5，归 MR1）。
- **不降级 P1-RC-006 为 P2**（RC-9 专属审计完全缺失已确认 P1；降级证据是通用追踪非专属审计，不满足 RC-9 验收标准「全程审计[操作人/原因]」字面要求——reason 不可追是合规硬伤）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-FIN-07 全部验收标准**（A1.6 §5 已判 RC-9 P1 + RC-1 复用 P1-MA3-046 + 其余接受；本验证只评 RC-9 降级证据 + 合规影响差异）。
- **不展开 A1.6 §7-2/§7-4**（A4.1.19 PC-4 折旧交互 / A4.1.21 年末反结账边界）。
- **不重新裁决 RC-1 高权限 kill-switch**（复用 P1-MA3-046，A1.6 §5.3 已裁决）。

## Task Route

- Type: `verification or audit work`（RC-9 降级审计证据评估 + 合规影响程度评估 + P1-RC-006 优先级裁决闭合）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.20 行）+ `docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 3 + §2.11 RC-9 缺失 + §5.2 RC-9 P1 + §5.3 P1-RC-006 + §6.1/§6.3 MR1[ORM ask-first]（输入）+ `docs/design/finance/period-close.md §反结账约束 :223-228`（L2 设计参考）+ nop-entropy 平台 `updateTimeProp/updaterProp` 自动填充机制文档（`../nop-entropy/docs-for-ai/`）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。RC-9 降级证据评估需多维度归类（reverseClose 审计字段写入 / 平台 updateTimeProp/updaterProp 自动填充机制 / 降级证据可靠性边界[无 reason + 易覆盖 + 非专属时间] / 合规影响程度[外部审计/税务/SOX] / 测试覆盖边界 / P1-RC-006 优先级裁决[维持 P1 不降级] / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 reverseClose 审计字段写入 + ORM 自动填充机制 + 平台文档 + 既有测试普查）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - RC-9 降级审计证据与合规影响评估

Status: planned
Targets: `docs/audits/2026-08-06-1708-rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.20 行）；A1.6 done（§7 存疑点 3 已落盘 + §2.11 RC-9 缺失 + §5.2 RC-9 P1 + §5.3 P1-RC-006 新建 + §6.1/§6.3 MR1[ORM ask-first]）

- [ ] `Proof` reverseClose 审计字段写入核验：给出 `ErpFinAccountingPeriodReverseCloseProcessor.reverseClose:22-59` 证据（file:line）——无 reason 参数（签名）+ 无 `setReversedBy`/`setReverseCloseReason`/`setReverseCloseAt`（字段不存在）+ 无 `ReverseCloseLog` 写入 + 方法对 period 实体写入 = `:39 setStatus(OPEN)` + `:55-56 reopenModules` + `:57 flushSession`。证实 RC-9 专属审计完全缺失。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 通用 `updatedBy`/`updateTime` 降级审计证据评估（本存疑点核心）：核验 `ErpFinAccountingPeriod` ORM `updateTimeProp="updateTime" updaterProp="updatedBy"`（orm.xml:657）→ 平台 `IDaoEntityListener`/审计拦截器自动填充机制——追踪 reverseClose 的 `setStatus(OPEN)` + `reopenModules` + `flushSession` **是否**触发 `updatedBy`/`updateTime` 被反结账操作覆盖（提供降级证据：操作人 from `IUserContext` + 时间戳 from `CoreMetrics`）。评估降级证据可靠性边界：①无 reason（reason 不可追）；②`updateTime` = 任意更新时间非反结账专属时间戳；③被任何后续无关更新覆盖（不可靠）；④entity tagSet `gid,erp.finance` 无 `audit,audit-save`（无结构化操作日志）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` ORM 审计列 + ReverseCloseLog grep 复核：核验 `app-erp-finance.orm.xml:655-694` 审计列实际存在（`closedBy`/`closedAt`/`updatedBy`/`updateTime`）+ `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列**不存在**（:659-677 列清单）+ 全仓 grep `reversedBy|reverseCloseReason|reverseCloseAt|ReverseCloseLog|reverseCloseLog` = 0 命中（live 复核 A1.6 §2.11）。证实 RC-9 专属审计列/实体完全缺失。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` RC-9 实际合规影响程度评估：评估反结账无专属审计轨迹[操作人/原因/时间]在外部审计/税务/SOX 合规场景下的可追溯性破坏程度——降级证据[通用 updatedBy/updateTime]是否部分满足合规最低要求（操作人可追[from updatedBy] + 时间可追[from updateTime]但 reason 不可追 + 易被覆盖）vs 完全不可追。结合 L1 RC-9「全程审计[操作人/原因]」字面要求[操作人 + 原因均须]评估降级证据的合规缺口。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 既有测试覆盖边界普查：grep 反结账审计相关测试（`updatedBy`/`updateTime` 被反结账覆盖断言 + ReverseCloseLog 测试 + reverseClose reason 参数测试）全集，产出测试覆盖边界清单 + 标注降级证据断言缺口（零覆盖，功能缺失故零测试）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（RC-9 审计轨迹是否符合 L1「全程审计[操作人/原因]」），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` P1-RC-006 优先级裁决闭合（方法论 §2 判据 + 三源对照）：①确认 RC-9 专属审计完全缺失 → P1-RC-006 **P1 分级维持**（§2 P1① 功能完全缺失——降级证据是通用追踪非 RC-9 专属审计，不满足「全程审计[操作人/原因]」字面[reason 不可追是合规硬伤]，不降级 P2）；②评估降级证据存在性以指导 MR1 优先级（有降级证据[操作人+时间可追]→ P1 优先级可排在活跃数据破坏类 P0 之后，但 reason 不可追是合规硬伤故仍须实现，不延后至 P2）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.6 §5.3 P1-RC-006 P1 新建 + §6.3 MR1[ORM ask-first] 分层一致。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] reverseClose 审计写入 + 降级证据评估 + ORM grep 复核 + 合规影响 + 测试覆盖边界证据落盘（全集，无遗漏），每条有证据（file:line）
- [ ] P1-RC-006 优先级裁决有明确结论（维持 P1 不降级 + 降级证据存在性指导 MR1 优先级），与 A1.6 §5.3 + §6.3 分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: planned
Targets: `docs/audits/2026-08-06-1708-rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence.md`（定稿）；`docs/audits/arm-index.md`（P1-RC-006 优先级注记更新）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 RC-9 降级证据评估 + 优先级裁决闭合完成

- [ ] `Add` P1-RC-006 优先级注记更新：在 arm-index P1-RC-006 行追加「A4.1.20 降级审计证据评估：通用 updatedBy/updateTime 经平台自动填充被反结账覆盖提供降级证据[操作人+时间可追]，但 reason 不可追 + 易被覆盖 + 非专属审计 → RC-9 验收标准仍功能完全缺失 → P1 维持不降级；降级证据指导 MR1 优先级（排活跃数据破坏 P0 之后，reason 不可追合规硬伤故仍须实现）」注记。P1-RC-006 分级/修复通道[MR1 ORM ask-first]不变。禁止未经比对新建重复 finding（P1-RC-006 已登记，本验证只更新注记）。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.6 §2.11/§5.3 P1-RC-006 / §6.3 MR1 / A2.3 period-close E2E 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（reverseClose 审计写入 + 降级证据评估 + ORM grep + 合规影响 + 测试覆盖边界 + 优先级裁决 + finding 衔接 + §8 自检齐全）
- [ ] P1-RC-006 优先级注记已更新入 arm-index（维持 P1 + 降级证据指导 MR1 优先级）并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: accept (mission-driver 独立子代理 ses_029a5c84affeInq03xrOJ56PHS，新会话不重用执行者上下文) — 全 9 checklist 项 PASS，零信任核对 live code（reverseClose:22-59 无 reason 参数 + 无 reversedBy/Reason/At setter + setStatus:39 + reopenModules:55-56 + flushSession:57 / 契约链 IErpFinPeriodCloseBiz:45 + BizModel:70-71 + Processor:22 一致无 reason 参数 / ORM orm.xml:657 updateTimeProp/updaterProp + 审计列 closedBy/closedAt/updatedBy/updateTime 在 + reversedBy/Reason/At 缺 + tagSet=gid,erp.finance 无 audit,audit-save / grep reversedBy|ReverseCloseLog = 0 命中）零漂移；格式合规；单一结果表面；anti-slack 零命中；item typing 合规（Proof/Decision/Add 无 Fix）；Deps 门控满足（A4.1 expander done + A1.6 done）；保护区域纪律（只读 + P1-RC-006 修复归 MR1 ask-first）；P1 不降级纪律正确（显式声明 P1 维持，仅评估降级证据指导 MR1 优先级，"排 P0 之后"指 MR1 排序非 P→P2 降级）；逻辑健全（updateTimeProp/updaterProp 平台自动填充 → updatedBy/updateTime 被反结账覆盖提供降级证据[操作人+时间可追] 但 reason 不可追 + 易覆盖 + 非专属 → RC-9 验收标准功能缺失成立 P1 维持）；Closure Gates 删除全仓 typecheck/build（只读）对齐 guide + A4.1.18。无 Blocker/Major。2 non-blocking Minors（执行报告引用 nop-entropy 自动填充机制需精确 file:line — 执行时补；reopenModules:278-284 行锚未独立 spot-check — 主张 setStatus 修改实体已独立证实，降级证据逻辑成立）。promote to active。

## Closure Gates

> 本计划为**只读 RC-9 降级审计证据与合规影响评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = reverseClose 审计写入 + 降级证据评估 + ORM grep + 合规影响 + 测试覆盖边界 + 优先级裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.1.20 验证报告 reverseClose 审计写入 + 降级证据评估 + ORM grep + 合规影响 + 测试覆盖边界 + 优先级裁决齐全 + P1-RC-006 优先级注记更新
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.6 §7-3 + §2.11 RC-9 缺失 + §5.3 P1-RC-006 + §6.3 MR1 一致
- [ ] 已运行验证：reverseClose 审计写入 + 降级证据评估 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（P1-RC-006 修复归 MR1 在 §Deferred But Adjudicated 预声明，非范围内项目降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-RC-006 RC-9 反结账审计轨迹实现（MR1 修复归口）

- Classification: `out-of-scope improvement`（本验证是降级证据 + 合规影响评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是降级证据评估，结果表面 = 验证报告 + P1-RC-006 优先级注记更新。修复（ORM 增 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 或 新增 `ErpFinReverseCloseLog` 实体 + `IErpFinPeriodCloseBiz.reverseClose` 增 reason 参数 + `ReverseCloseProcessor` 落库审计）归 MR1（R1.0→RC-R1.n），**触及 ORM 结构变更 + 会计过账逻辑[反结账] 须 ask-first + 独立 plan-audit**（§5）。本验证闭环不阻塞于修复落地。
- Successor Required: yes（MR1 R1.0 展开器读取 P1-RC-006 → RC-R1.n 修复；本报告降级证据评估 + reason 不可追合规硬伤指导 MR1 优先级[排活跃数据破坏 P0 之后但须实现]）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- MR1 修复 P1-RC-006（RC-9 反结账审计轨迹）：触及 ORM 结构变更 + 会计过账逻辑须 ask-first + 独立 plan-audit。R1.0 展开器读取 P1-RC-006 → RC-R1.n。
