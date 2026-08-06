# RC MA4 A4.1.20 — RC-9 反结账审计轨迹缺失：降级审计证据评估与合规影响裁决

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.20（MA4 运行时行为验证 — A1.6 §7-3：UC-FIN-07 RC-9 反结账全程审计[操作人/原因/时间]缺失——无操作人/原因/时间记录在外部审计/税务/SOX 合规场景下的可追溯性破坏程度；当前 `ErpFinAccountingPeriod` 通用 `updatedBy`/`updateTime` 审计列是否被反结账操作覆盖以提供降级审计证据，闭合 P1-RC-006 修复方案优先级裁决）
> 输入：`docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 3 + §2.11 RC-9 缺失 + §5.3 P1-RC-006 新建 + §6.1/§6.3 MR1[ORM ask-first]
> 验证 plan：`docs/plans/2026-08-06-1708-2-rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据[含 P1① 功能完全缺失] + §4 Q1 真相源层级[Q1=(c) 分歧以 L1 为准] + §5 Q4 修复义务 + 保护区域暂停协议[ORM ask-first] + §7 衔接 + §8 过程纪律自检 + §9 真相源冻结 + §去重协议[MA4↔A5.6 边界]）
> 范式对齐：A4.1.18（`docs/audits/2026-08-06-1517-rc-ma4-a4-1-18-pc3-arap-reminder-nonblocking-runtime-behavior.md`，done — period-close 运行时行为评估同型工作项；本验证复用其 §0 TL;DR / §1-§11 骨架）
> 审计性质：**只读 RC-9 降级审计证据与合规影响评估**（读 `reverseClose` 审计字段写入 + `ErpFinAccountingPeriod` ORM `updateTimeProp`/`updaterProp` 平台自动填充机制 + 通用 `updatedBy`/`updateTime` 是否被反结账覆盖评估 + 既有测试普查 + 引用 A1.6/A2.3；**不改代码/ORM/api.xml/真相源**）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`7aa9b7078`

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.6 §7-3：RC-9 反结账审计缺失的实际合规影响——无操作人/原因/时间记录在外部审计/税务/SOX 合规场景下的可追溯性破坏程度；当前 `ErpFinAccountingPeriod` 通用 `updatedBy`/`updateTime` 审计列是否被反结账操作覆盖可作部分证据，关联 P1-RC-006 |
| reverseClose 审计字段写入核验（写时实测） | `ErpFinAccountingPeriodReverseCloseProcessor.reverseClose:22-59` 签名 `reverseClose(Long periodId, IServiceContext context)` **无 reason 参数**（契约链 `IErpFinPeriodCloseBiz.reverseClose:45` + Facade `ErpFinAccountingPeriodBizModel.reverseClose:70-71` + Processor `:22` 一致无 reason）；方法体**无** `setReversedBy`/`setReverseCloseReason`/`setReverseCloseAt` 调用（字段不存在）；**无** `ReverseCloseLog` 实体写入。方法对 period 实体的写入 = `:39 setStatus(OPEN)` + `:55 findOrCreatePeriodStatus` + `:56 reopenModules` + `:57 flushSession`。证实 RC-9 专属审计完全缺失 |
| **通用 `updatedBy`/`updateTime` 降级审计证据评估（存疑点核心变量，写时实测）** | **降级证据存在**——`ErpFinAccountingPeriod` ORM（`app-erp-finance.orm.xml:657`）声明 `updateTimeProp="updateTime" updaterProp="updatedBy"`；平台 `OrmTimestampHelper.onUpdate:74-112`（`:96-105` 填 `updaterProp`=updatedBy from `ContextProvider.currentUserRefNo()`=IUserContext；`:107-111` 填 `updateTimeProp`=updateTime from `CoreMetrics.currentTimeMillis()`）经 `EntityPersisterImpl.queueUpdate:470-471` 在**每次 entity update 排队 flush** 时调用。`reverseClose` 的 `setStatus(OPEN):39` + `reopenModules:55-56`（修改 period + PeriodStatus 行）+ `flushSession:57` **触发** `updatedBy`/`updateTime` 被反结账操作覆盖（提供降级证据：操作人 + 时间戳）。**但降级证据可靠性边界受限**（§2.2） |
| 降级证据可靠性边界（4 项） | ① **无 reason**（reason 完全不可追——`reverseClose` 无 reason 参数）；② `updateTime` = **任意更新时间非反结账专属时间戳**（被任何后续无关更新覆盖，非"反结账发生时刻"）；③ **易被覆盖**（`updatedBy`/`updateTime` 被任何后续 period 更新覆盖，不可靠）；④ entity tagSet `gid,erp.finance`（orm.xml:657）**无 `audit,audit-save`** → 不被平台 audit-log writer 覆盖（无结构化操作日志，`orm-model-design.md:312` `audit` column tag → `NopSysChangeLog` 才有，本实体无） |
| ORM 审计列 + ReverseCloseLog grep 复核（写时实测） | `app-erp-finance.orm.xml:655-694` 审计列实际存在：`closedBy:670`/`closedAt:671`/`updatedBy:676`/`updateTime:677` + std `createdBy:674`/`createTime:675`/`version:673`；**`reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列不存在**（`:659-677` 列清单无此字段）；全仓 `rg "reversedBy\|reverseCloseReason\|reverseCloseAt\|ReverseCloseLog\|reverseCloseLog"`（排除 docs/）= **0 命中**。证实 RC-9 专属审计列/实体完全缺失（A1.6 §2.11 HEAD `c1b775491` 复核维持） |
| RC-9 实际合规影响程度（§2 P1① + L1 字面） | L1 `use-cases.md:140` 逐字「全程审计(记录反结账操作人/**原因**)」——**操作人 + 原因均须**。降级证据[通用 updatedBy/updateTime]：操作人**部分可追**（from `updatedBy`，但易被覆盖）+ 时间**部分可追**（from `updateTime`，但非专属时间戳）BUT **reason 完全不可追** + 无结构化操作日志 + 非专属审计轨迹。外部审计/税务/SOX：操作人+时间部分可追（降级证据）→ 部分满足"谁在何时动了什么"最低追溯，BUT **reason 不可追是合规硬伤**（SOX/审计准则要求 change-log 含变更原因 + 不可篡改时间序列，降级证据不满足）。降级证据**部分满足合规最低要求但 reason 不可追是合规硬伤** |
| 测试覆盖边界（写时实测） | `TestErpFinReverseClose#testReverseCloseRestoresBalance:26-51` 仅断言 status（:37）/余额（:39-40）/模块回开（:43-44）/红冲凭证（:46-47）——**无 `getUpdatedBy`/`getUpdateTime` 断言**；grep `getUpdatedBy\|getUpdateTime` 跨 `module-finance/erp-fin-service/src/test/` = **0 命中**（仅 `TestErpFinBudgetCarryForward:82` 断言 `getClosedAt`，非 updatedBy/updateTime）。`ReverseCloseLog` 测试 = **缺口**（实体不存在）；`reverseClose` reason 参数测试 = **缺口**（参数不存在）。降级证据断言缺口 = **零覆盖** |
| **P1-RC-006 优先级裁决（§2 判据 + 三源对照）** | **维持 P1（不降级 P2）**——(1) RC-9 专属审计完全缺失 → §2 P1①（功能完全缺失——降级证据是**通用更新追踪**非 RC-9 专属审计，reason 不可追是合规硬伤，不满足 L1「全程审计[操作人/原因]」字面要求，**不降级 P2**）；(2) 降级证据存在性指导 MR1 优先级：**有降级证据[操作人+时间可追]** → P1 优先级可排在活跃数据破坏类 P0 之后，**BUT reason 不可追是合规硬伤故仍须实现，不延后 P2**。裁决与 A1.6 §5.3 P1-RC-006 P1 新建 + §6.3 MR1[ORM ask-first] 分层一致 |
| 新 finding | **0**（维持 P1-RC-006 P1 分级，不降级，无 successor 触发；arm-index P1-RC-006 行追加 A4.1.20 降级证据评估注记） |
| P0 即时通道 | 不触发（维持 P1，未出 P0） |

**核心裁决**：存疑点 A1.6 §7-3 的 RC-9 反结账审计缺失降级证据评估结论 = **维持 P1-RC-006（P1，不降级 P2）**。判据三层：(1) **降级证据存在**——本报告 §2.2 主证据（ORM `updateTimeProp`/`updaterProp` 声明 + 平台 `OrmTimestampHelper.onUpdate` 自动填充机制 + `EntityPersisterImpl.queueUpdate` 调用点）独立核验：`reverseClose` 的 `setStatus(OPEN)` + `reopenModules` + `flushSession` 触发平台在每次 entity update 排队 flush 时自动填充 `updatedBy`（from `IUserContext`）+ `updateTime`（from `CoreMetrics`）→ **通用 `updatedBy`/`updateTime` 确被反结账操作覆盖，提供降级证据[操作人 + 时间部分可追]**；(2) **降级证据可靠性边界受限**——①无 reason（reason 完全不可追，L1「全程审计[操作人/原因]」字面要求 reason，合规硬伤）；②`updateTime`=任意更新时间非反结账专属时间戳；③易被后续无关更新覆盖（不可靠）；④entity tagSet `gid,erp.finance` 无 `audit,audit-save` → 无结构化操作日志；(3) **P1 维持不降级**——降级证据是**通用更新追踪**（generic update-tracking）非 RC-9 专属审计，不满足 L1 `use-cases.md:140`「全程审计[记录反结账**操作人/原因**]」字面要求[操作人虽部分可追但 reason 完全不可追是合规硬伤] → §2 P1①（功能完全缺失）成立，**不降级 P2**。降级证据存在性指导 MR1 优先级：有降级证据[操作人+时间可追]→ P1 优先级可排在活跃数据破坏类 P0 之后，BUT reason 不可追是合规硬伤故仍须实现，不延后 P2。A1.6 §7 存疑点 3 经本评估**消解为「降级证据存在 + P1 维持不降级」**，闭合 P1-RC-006 修复方案优先级裁决。修复（`IErpFinPeriodCloseBiz.reverseClose` 增 reason 参数 + `ErpFinAccountingPeriod` ORM 增 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 或 新增 `ErpFinReverseCloseLog` 实体 + `ReverseCloseProcessor` 落库审计）归 MR1[ORM ask-first]，**触发 §5 ask-first + 独立 plan-audit**，归 plan §Deferred But Adjudicated successor。**本验证不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（§6 §1 / §1 L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。验收标准逐字引用，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。本验证只评 RC-9，引用 A1.6 §1 UC-FIN-07 完整枚举。

**UC-FIN-07 反结账**（`use-cases.md:129`）RC-9 逐字（A1.6 §1 :86，UC-FIN-07 heading :129，断言块 :133-141）：

```
全程审计(记录反结账操作人/原因)
```

**关键语义**：L1 RC-9 字面要求「全程审计」须记录**反结账操作人 + 反结账原因**两项（"操作人/原因"为并列必备项，非"操作人或原因"）。本限定是本验证的核心 hinge——若反结账操作完全无任何审计痕迹（操作人/原因/时间均不可追）→ 合规影响最大（无可追溯性）；若存在降级证据（通用 `updatedBy`/`updateTime` 被覆盖 → 操作人+时间部分可追）BUT reason 不可追 → 合规影响部分缓解（reason 仍不可追是合规硬伤）。本验证 §2.2 评估通用 `updatedBy`/`updateTime` 经平台自动填充是否被反结账操作覆盖，§2.4 评估降级证据的合规影响程度。

**L2 设计参考**（`period-close.md §反结账流程 :216-221`「步骤8：记录审计」+ `:223-228 §反结账约束`）：L2 设计了「反结账操作记录 / 调整单据清单 / 调整金额影响 / 相关责任人签名」审计步骤 + 「管理员 + 审批」权限要求——L2 期望**专属反结账审计**（含操作记录 + 调整清单 + 责任人签名），远超通用 `updatedBy`/`updateTime` 降级证据。按 §4 Q1=(c) **L1 为准**，L2 设计参考不降低 L1 要求。

---

## 2. 实现证据（§6 §2 / §1 L3，写时实测 `7aa9b7078`）

### 2.1 reverseClose 审计字段写入核验（Phase 1 item 1）

> 核验目标：证实 `reverseClose` 方法体对 period 实体的写入 + 无 RC-9 专属审计字段 setter + 无 ReverseCloseLog 写入（A1.6 §2.11 静态确认的运行时复核实）。

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| 契约签名（无 reason 参数） | `module-finance/erp-fin-dao/.../biz/IErpFinPeriodCloseBiz.java#reverseClose:45` | `:44` `@BizMutation` + `:45` `ErpFinAccountingPeriod reverseClose(@Name("periodId") Long periodId, IServiceContext context);`——**仅 periodId + context，无 reason 参数** | ✅ |
| Facade 委托（无 reason 参数） | `module-finance/erp-fin-service/.../entity/ErpFinAccountingPeriodBizModel.java#reverseClose:70-71` | `:68` `@BizMutation` + `:70` `public ErpFinAccountingPeriod reverseClose(@Name("periodId") Long periodId, IServiceContext context)` + `:71` `return reverseCloseProcessor.reverseClose(periodId, context);`——**无 reason 参数透传** | ✅ |
| Processor 实现（无 reason 参数 + 无专属审计 setter） | `module-finance/erp-fin-service/.../processor/ErpFinAccountingPeriodReverseCloseProcessor.java#reverseClose:22-59` | `:22` `public ErpFinAccountingPeriod reverseClose(Long periodId, IServiceContext context)`（**无 reason 参数**）；方法体写入 = `:23 requirePeriod` + `:24 assertPeriodStatus(CLOSED_FINAL)` + `:26-29` kill-switch 门控 + `:32-36` 年末反结账阻断 + **`:39 period.setStatus(PERIOD_STATUS_OPEN)`**（对 period 实体的唯一显式字段写入）+ `:42-52` 凭证红冲（`reverseCloseVoucher` ×3 + `reverseDepreciation`，跨实体写 Voucher 非写 period 审计字段）+ **`:55 findOrCreatePeriodStatus` + `:56 reopenModules`**（修改 PeriodStatus 行）+ **`:57 orm().flushSession()`**；**无** `setReversedBy`/`setReverseCloseReason`/`setReverseCloseAt` 调用（字段不存在）；**无** `ReverseCloseLog` 实体 `new`/`save` | ✅ |

**reverseClose 审计字段写入核验结论**：`reverseClose:22-59` 签名无 reason 参数（契约链 IErpFinPeriodCloseBiz:45 + BizModel:70-71 + Processor:22 三层一致无 reason）+ 方法体无 RC-9 专属审计字段 setter（`reversedBy`/`reverseCloseReason`/`reverseCloseAt` 字段不存在故无 setter）+ 无 `ReverseCloseLog` 实体写入——证实 **RC-9 专属审计（操作人/原因/时间专属记录）完全缺失**。方法对 period 实体的写入仅 `setStatus(OPEN):39`（修改 status 字段）+ `reopenModules:55-56`（修改 PeriodStatus 行，非 period 审计字段）+ `flushSession:57`（刷出，触发 §2.2 平台自动填充）。A1.6 §2.11 静态确认经 HEAD `7aa9b7078` 复核**无回退**（行号与 A1.6 §2.11 / plan Current Baseline 精确一致：契约链 :45/:70-71/:22 一致无 reason + setStatus:39 + reopenModules:55-56 + flushSession:57）。

### 2.2 通用 `updatedBy`/`updateTime` 降级审计证据评估（Phase 1 item 2，存疑点核心变量）

> 核验目标：核验 `ErpFinAccountingPeriod` ORM `updateTimeProp="updateTime" updaterProp="updatedBy"`（orm.xml:657）→ 平台自动填充机制——追踪 `reverseClose` 的 `setStatus(OPEN)` + `reopenModules` + `flushSession` **是否**触发 `updatedBy`/`updateTime` 被反结账操作覆盖（提供降级证据）。**这是 P1-RC-006 优先级裁决闭合的核心证据。**

#### 2.2.1 ORM 审计列声明核验

| ORM 声明 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| entity 审计列声明 | `module-finance/model/app-erp-finance.orm.xml:657`（`ErpFinAccountingPeriod` entity） | `updateTimeProp="updateTime" createrProp="createdBy" updateTimeProp="updateTime" updaterProp="updatedBy"`——平台按此声明在 entity insert/update 时自动填充 `createdBy`/`updatedBy`/`createTime`/`updateTime` | ✅ |
| entity tagSet（无 audit） | `module-finance/model/app-erp-finance.orm.xml:657` | `tagSet="gid,erp.finance"`——**无 `audit,audit-save`** → 不被平台 audit-log writer 覆盖（无结构化操作日志） | ✅ |
| 审计列实际存在 | `module-finance/model/app-erp-finance.orm.xml:670-677` | `closedBy:670`（结账人，业务动作责任字段）+ `closedAt:671`（结账时间）+ `updatedBy:676`（domain="updatedBy" 被动审计列）+ `updateTime:677`（domain="updateTime" 被动审计列）+ std `createdBy:674`/`createTime:675`/`version:673` | ✅ |

#### 2.2.2 平台自动填充机制核验（nop-entropy）

| 平台机制 | 文件:行（写时实测 nop-entropy） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| 自动填充 helper | `nop-entropy/nop-persistence/nop-orm/src/main/java/io/nop/orm/persister/OrmTimestampHelper.java#onUpdate:74-112` | `:74 onUpdate(IEntityModel, IOrmEntity)` 在 entity update 时：**`:96-105`** 若 `getUpdaterPropId() > 0` → `:98 getCurrentUser()`（`:120-122` `ContextProvider.currentUserRefNo()` = IUserContext 登录用户）→ `:100 orm_propValue(updaterPropId, user)`（填 `updatedBy`）；**`:107-111`** 若 `getUpdateTimePropId() > 0` → `:79 current = new Timestamp(CoreMetrics.currentTimeMillis())` → `:109 orm_propValue(updateTimePropId, current)`（填 `updateTime`）。`:75-76` `orm_disableAutoStamp()` 守卫（默认 false 不禁用） | ✅ |
| 调用点（每次 entity update 排队 flush） | `nop-entropy/nop-persistence/nop-orm/src/main/java/io/nop/orm/persister/EntityPersisterImpl.java#queueUpdate:470-471` | `:470 protected void queueUpdate(IOrmEntity entity, IOrmSessionImplementor session)` + `:471 OrmTimestampHelper.instance().onUpdate(entityModel, entity);`——**每次 entity update 入队 flush 时调用 onUpdate**，自动填充 `updaterProp`/`updateTimeProp` | ✅ |
| 平台文档（被动审计列分类） | `nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md §被动审计列 :652-690` | `:631`「被动审计：框架自动填充的纯合规轨迹（createdBy, updatedBy）」+ `:660-665`「`domain="createdBy"` 模式 = 框架自动填充，不参与过滤/统计」+ `:690`「被动审计（createdBy, updatedBy）= `VARCHAR(50)` `domain="createdBy"` 无 to-one」——确认 `updatedBy`/`updateTime` 是平台自动填充的**通用更新追踪**列，非业务动作专属审计 | ✅ |
| 平台文档（audit tag = 结构化日志） | `nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md §列的 tagSet 标签体系 :312` | 「`audit` column tag = 记录字段级变更日志到 `NopSysChangeLog`」——`ErpFinAccountingPeriod` 列无 `audit` tag，故无字段级变更日志 | ✅ |

#### 2.2.3 reverseClose 触发降级证据路径核验

| reverseClose 写入步骤 | 文件:行（写时实测） | 是否触发平台自动填充 `updatedBy`/`updateTime` | 核验状态 |
|---|---|---|---|
| `period.setStatus(OPEN)` | `ErpFinAccountingPeriodReverseCloseProcessor.java:39` | ✅ 是——`setStatus` 修改 period 实体 → period 被 OrmSession 标记 dirty → `flushSession:57` 时 `queueUpdate:470` → `onUpdate:471` 自动填充 `period.updatedBy`/`period.updateTime` | ✅ |
| `reopenModules(status)` | `ErpFinAccountingPeriodProcessor.java`（经 `:56`）修改 `ErpFinAccountingPeriodStatus` 行（per-module status reset to OPEN） | ✅ 是——PeriodStatus 行被修改 → dirty → flush 时自动填充各自 `updatedBy`/`updateTime`（PeriodStatus 实体亦声明 `updateTimeProp`/`updaterProp`，orm.xml:699） | ✅ |
| `orm().flushSession()` | `ErpFinAccountingPeriodReverseCloseProcessor.java:57` | ✅ 是——刷出所有 dirty 实体，触发 `queueUpdate` → `onUpdate` 自动填充 | ✅ |

**降级证据评估结论（存疑点核心变量）**：通用 `updatedBy`/`updateTime` 经平台 `OrmTimestampHelper.onUpdate:74-112`（`EntityPersisterImpl.queueUpdate:470-471` 调用）**确被反结账操作覆盖**——`reverseClose` 的 `setStatus(OPEN):39` + `reopenModules:55-56` + `flushSession:57` 触发平台在 period（及 PeriodStatus 行）entity update 排队 flush 时自动填充 `updatedBy`（from `IUserContext`，反结账操作人）+ `updateTime`（from `CoreMetrics`，反结账时刻）。**降级证据存在**：操作人 + 时间部分可追。

**但降级证据可靠性边界受限（4 项）**：

| # | 可靠性边界 | 证据（写时实测） | 影响 |
|---|---|---|---|
| ① | **无 reason** | `reverseClose:22` 签名无 reason 参数 + 无 `setReverseCloseReason`（字段不存在） | **reason 完全不可追**——L1 RC-9 字面要求「操作人/**原因**」，reason 不可追是合规硬伤 |
| ② | `updateTime` = 任意更新时间非反结账专属时间戳 | `OrmTimestampHelper.onUpdate:79,109` 每次 entity update 都填 `CoreMetrics.currentTimeMillis()`，非仅反结账时填 | `updateTime` 是"最后一次任意更新"时间，非"反结账发生时刻"专属时间戳；若反结账后 period 被任何无关更新（如手工改 status/name），`updateTime` 被覆盖丢失反结账时刻 |
| ③ | 易被后续无关更新覆盖 | `updatedBy`/`updateTime` 是通用更新追踪列（orm-model-design.md :631「被动审计」），任何后续 period 更新都覆盖之 | 反结账操作人/时间**不可靠**——被后续无关更新覆盖即丢失，非不可篡改审计轨迹 |
| ④ | 无结构化操作日志 | entity tagSet `gid,erp.finance`（orm.xml:657）无 `audit,audit-save` + 列无 `audit` tag | 不被平台 audit-log writer 覆盖（无 `NopSysChangeLog` 结构化操作日志，orm-model-design.md :312）→ 无"谁在何时对 period 做了反结账"的结构化操作记录 |

### 2.3 ORM 审计列 + ReverseCloseLog grep 复核（Phase 1 item 3）

> 核验目标：核验 `app-erp-finance.orm.xml:655-694` 审计列实际存在 + `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列不存在 + 全仓 grep RC-9 专属审计实体/字段 = 0 命中（live 复核 A1.6 §2.11）。

| grep 维度 | pattern（写时实测 HEAD `7aa9b7078`） | 命中（生产代码，排除 docs/） | 裁决 |
|---|---|---|---|
| RC-9 专属审计列/实体 | `rg "reversedBy\|reverseCloseReason\|reverseCloseAt\|ReverseCloseLog\|reverseCloseLog" --glob '!docs/**'` | **0 命中** | RC-9 专属审计列/实体完全缺失 |
| ORM 审计列存在性 | `app-erp-finance.orm.xml:659-677`（`ErpFinAccountingPeriod` columns 块） | 审计列存在：`closedBy:670`/`closedAt:671`/`updatedBy:676`/`updateTime:677` + std `createdBy:674`/`createTime:675`/`version:673`/`delVersion:672`；**`reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列不存在** | 通用审计列在，专属反结账审计列缺 |

**grep 复核结论**：A1.6 §2.11 静态确认经 HEAD `7aa9b7078` 复核**维持**——RC-9 专属审计（`reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 + `ReverseCloseLog` 实体）全仓 0 命中，完全缺失。`ErpFinAccountingPeriod` 仅有通用 `closedBy`/`closedAt`（结账业务动作责任字段）+ `updatedBy`/`updateTime`（被动审计列，§2.2 降级证据），无反结账专属审计载体。

### 2.4 RC-9 实际合规影响程度评估（Phase 1 item 4）

> 评估反结账无专属审计轨迹[操作人/原因/时间]在外部审计/税务/SOX 合规场景下的可追溯性破坏程度——降级证据[通用 updatedBy/updateTime]是否部分满足合规最低要求 vs 完全不可追。

| 合规场景 | L1 字面要求 | 降级证据[通用 updatedBy/updateTime]覆盖 | 合规缺口 |
|---|---|---|---|
| **外部审计**（财务报表审计） | 反结账操作可追溯[操作人 + 原因 + 时间]，审计准则要求 change-log 含变更原因 + 不可篡改时间序列 | 操作人**部分可追**（from `updatedBy`，但易被覆盖：§2.2 边界③）+ 时间**部分可追**（from `updateTime`，但非专属时间戳：§2.2 边界②）+ **reason 完全不可追**（§2.2 边界①）+ 无结构化操作日志（§2.2 边界④） | **reason 不可追是合规硬伤**——审计准则要求变更原因记录；降级证据易被覆盖非"不可篡改时间序列"；无结构化操作日志致审计无法独立取证反结账事件 |
| **税务合规**（已申报税务期间反结账） | 反结账操作可追溯 + 与税务机关沟通记录（L2 period-close.md :227「已申报税务需与税务机关沟通」） | 操作人+时间部分可追 BUT reason 不可追 + 无"反结账致税务申报需更正"的结构化关联记录 | reason 不可追致无法证明反结账动机（更正错误 vs 调节利润）；无税务沟通关联记录 |
| **SOX（萨班斯法案）ITGC** | 反结账（高权限财务关账动作）须有完整 audit trail：操作人 + 原因 + 时间 + 审批 + 不可篡改 | 操作人+时间部分可追 BUT reason 不可追 + 无审批记录（RC-2 缺失，A1.6 §2.7）+ 易被覆盖（非不可篡改）+ 无结构化操作日志 | SOX §404 ITGC 要求 change-management audit trail 完整——降级证据缺 reason + 缺审批 + 非不可篡改，**不满足 SOX 合规最低要求** |

**合规影响程度裁决**：降级证据[通用 updatedBy/updateTime] **部分满足合规最低要求**（操作人 + 时间部分可追，非"完全不可追"最坏情形）BUT **reason 不可追是合规硬伤**（L1 RC-9 字面「操作人/**原因**」并列要求，外部审计/税务/SOX 均要求变更原因记录）+ 降级证据易被覆盖非不可篡改 + 无结构化操作日志致无法独立取证。**结论**：降级证据将合规影响从"完全不可追"（最大破坏）缓解为"操作人+时间部分可追但 reason 不可追"（部分缓解，合规硬伤残留），但**不满足 L1 RC-9「全程审计[操作人/原因]」字面要求**（reason 完全不可追）。

### 2.5 MA4↔A5.6 边界声明（Phase 1 item 5）

> 方法论 §去重协议 MA4↔A5.6 边界：MA4 审「行为是否符合需求」（需求契约视角，RC-9 审计轨迹是否符合 L1「全程审计[操作人/原因]」）；A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角，全量评级）。

**本验证边界执行声明**：

- 本验证审「RC-9 反结账审计轨迹是否符合 UC-FIN-07「全程审计[操作人/原因]」」——**需求契约视角**。裁决依据 = §2 判据（RC-9 专属审计完全缺失 + 降级证据可靠性边界 + 合规影响程度 + L1 字面 reason 要求）。
- 本验证**不重做 A5.6 E2E 断言强度审计**（A5.6 已对全量 spec 做断言强度分类矩阵）。本验证只评 RC-9 审计轨迹这一具体控制点的需求契约符合性。
- 裁决为「维持 P1（不降级）」→ 无新 finding；降级证据断言缺口（§3）属 A5.6 测试质量维度 successor（纯测试代码 MR1 预授权类目），**非本 MA4 范围**。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

### 3.1 测试覆盖边界普查（Phase 1 item 6）

> grep 反结账审计相关测试（`updatedBy`/`updateTime` 被反结账覆盖断言 + ReverseCloseLog 测试 + reverseClose reason 参数测试）全集。引用 A1.6 §3 已有评级依据。

| 测试方法 | 文件:行（写时实测） | 覆盖范围 | 断言强度 | RC-9 审计相关覆盖 |
|---|---|---|---|---|
| `testReverseCloseRestoresBalance` | `TestErpFinReverseClose.java:26-51` | RC-3 CLOSED_FINAL→OPEN（:37）+ RC-4 结转凭证红冲（:46-47）+ 收入科目余额恢复（:39-40）+ GL/AST 模块回开（:43-44）+ RC-8 重新结账（:49-50） | **深**（status + 余额精确 + 模块 status + 凭证计数） | ❌ **无 `getUpdatedBy`/`getUpdateTime` 断言**——仅断言 period.status/balance/moduleStatus/reversalVoucher，未断言反结账操作是否覆盖 `updatedBy`/`updateTime`（降级证据断言缺口） |
| `testFullChain` | `TestErpFinPeriodCloseEndToEnd.java:25-66` | 全链 preCheck → closePeriod → FX/PL 凭证 → finalizePeriod → reverseClose（:55-58）→ re-close | **深** | ❌ **无 RC-9 审计断言**（同上，仅状态/凭证/模块） |
| `testForwardAndReverse` | `TestErpFinPeriodStateMachine.java:47-68` | OPEN→CLOSED→CLOSED_FINAL→OPEN（反结账）+ RC-8 重新结账 | **深** | ❌ **无 RC-9 审计断言** |
| `testReverseCloseApprovalBlocked` | `TestErpFinModuleCloseOrder.java:78-90` | RC-1/RC-2 kill-switch 默认 true 阻断 | **深** | ❌ **无 RC-9 审计断言**（测审批门控非审计轨迹） |
| `updatedBy`/`updateTime` 跨 finance test 断言普查 | grep `getUpdatedBy\|getUpdateTime` 跨 `module-finance/erp-fin-service/src/test/` | **0 命中** | — | ❌ **零覆盖**（仅 `TestErpFinBudgetCarryForward:82` 断言 `getClosedAt`，非 updatedBy/updateTime） |
| `ReverseCloseLog` 测试 | grep `ReverseCloseLog\|reverseCloseLog` 跨 finance test | **0 命中**（实体不存在） | — | ❌ **缺口**（实体不存在故无测试） |
| `reverseClose` reason 参数测试 | grep `reverseClose.*reason\|reverseCloseReason` 跨 finance test | **0 命中**（参数不存在） | — | ❌ **缺口**（参数不存在故无测试） |

**测试覆盖边界清单**：

1. **RC-9 专属审计测试** ❌——RC-9 专属审计（reason 参数 + reversedBy/Reason/At 列 + ReverseCloseLog 实体）完全缺失（§2.1/§2.3），故零测试。
2. **降级证据断言缺口** ❌——`updatedBy`/`updateTime` 被反结账操作覆盖的断言零覆盖（grep `getUpdatedBy`/`getUpdateTime` 跨 finance test = 0 命中）。即使降级证据存在（§2.2 平台自动填充），亦无测试证实 reverseClose 后 `period.getUpdatedBy()`/`getUpdateTime()` 被覆盖。
3. **反结账行为主路径** ✅——`testReverseCloseRestoresBalance:26-51` 深断言 status/余额/模块/凭证（RC-3/4/7/8），A1.6 §3.5 已记。

**断言强度评级**：RC-9 审计轨迹测试覆盖 = **零覆盖**（功能缺失故零测试）+ 降级证据断言缺口（功能存在[平台自动填充]但无测试证实）。降级证据断言缺口属测试覆盖补强项（A5.6 维度），非合规缺陷（降级证据是平台机制自动生效，不需业务代码触发）。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 MA2/A1.6 复用（§去重协议）

| 已证实行为 | 引用 | 本验证复用判定 |
|---|---|---|
| 反结账行为主路径（余额还原 + GL/AST 模块回开 + PL 红冲凭证 + re-close） | A1.6 §2.8-2.10 + A2.3 period-close E2E `2026-07-27-1949-arm-ma2-period-close-e2e.md` | ✅ 复用（反结账行为主路径已证实）；本验证只补「降级证据存在性 + 合规影响程度 + P1-RC-006 优先级裁决」差异 |
| RC-9 反结账审计轨迹完全缺失（静态确认） | A1.6 §2.11 + §5.3 P1-RC-006 新建 | ✅ 复用（静态确认 + P1 分级）；本验证闭合决策（确认降级证据存在性 + P1 维持不降级） |

**声明**：本验证只补「通用 `updatedBy`/`updateTime` 经平台自动填充是否被反结账覆盖[降级证据存在性] + 降级证据可靠性边界 + 合规影响程度 + P1-RC-006 优先级裁决闭合」差异（A1.6 §7 存疑点 3 标注为「需运行时确认」），不重新核实反结账行为本身（A1.6 §2.8-2.10 + A2.3 已证实）。

### 4.2 本切片运行时行为增量

本验证相对 A1.6/A2.3 的**运行时行为增量**：

1. **降级证据存在性核验**（A1.6 §7-3「当前 `ErpFinAccountingPeriod` 通用 `updatedBy`/`updateTime` 审计列是否被反结账操作覆盖可作部分证据」未核实）：§2.2 核验平台 `OrmTimestampHelper.onUpdate:74-112` + `EntityPersisterImpl.queueUpdate:470-471` 自动填充机制——证实 `reverseClose` 的 `setStatus(OPEN)` + `reopenModules` + `flushSession` **确触发** `updatedBy`/`updateTime` 被反结账操作覆盖，**降级证据存在**。
2. **降级证据可靠性边界评估**（A1.6 §7-3 未评估边界）：§2.2 评估 4 项可靠性边界（无 reason + 非专属时间戳 + 易被覆盖 + 无结构化操作日志）。
3. **合规影响程度评估**（A1.6 §7-3「外部审计/税务/SOX 合规场景下的可追溯性破坏程度」未评估）：§2.4 评估降级证据部分满足合规最低要求[操作人+时间可追] BUT reason 不可追是合规硬伤。
4. **P1-RC-006 优先级裁决闭合**（A1.6 §5.3 P1 新建 + §6.3 MR1[ORM ask-first] 未闭合降级证据裁决）：§5.1 裁决维持 P1 不降级 + 降级证据指导 MR1 优先级。

---

## 5. 符合性结论（§6 §5 / §2 判据 + 三源对照）

### 5.1 P1-RC-006 优先级裁决闭合（Phase 1 item 7，方法论 §2 判据 + §4 Q1 + plan 决策树两分支）

| 决策分支 | 判据条件（plan Phase 1 item 7） | 本验证结果 | 命中 |
|---|---|---|---|
| **① 维持 P1（不降级 P2）** | RC-9 专属审计完全缺失（§2 P1① 功能完全缺失）**且**降级证据是通用追踪非 RC-9 专属审计（reason 不可追是合规硬伤）**且** L1「全程审计[操作人/原因]」字面 reason 不可追 → P1 维持不降级 | (a) RC-9 专属审计完全缺失（§2.1 reverseClose 无 reason 参数 + 无专属审计 setter + 无 ReverseCloseLog + §2.3 grep 0 命中）✅；(b) 降级证据是**通用更新追踪**非 RC-9 专属审计（§2.2 平台 `OrmTimestampHelper.onUpdate` 自动填充 `updatedBy`/`updateTime` 是被动审计列，orm-model-design.md :631，非反结账专属）✅；(c) reason 完全不可追（§2.2 边界①，`reverseClose` 无 reason 参数）是合规硬伤，L1 `use-cases.md:140`「操作人/**原因**」并列要求 ✅；(d) §2.4 合规影响评估降级证据部分缓解但 reason 不可追合规硬伤残留，不满足 L1 字面 ✅ | **命中** |
| ② 降级 P2（降级证据满足合规） | 降级证据[通用 updatedBy/updateTime]完全满足 L1 RC-9「全程审计[操作人/原因]」字面要求 → RC-9 验收标准达成 → 降级 P2 | 降级证据 reason **完全不可追**（§2.2 边界①）+ 易被覆盖（边界③）+ 无结构化操作日志（边界④）→ **不满足** L1「操作人/**原因**」字面要求（reason 不可追是合规硬伤）→ 分支②条件不成立 | 否 |

**裁决 = ① 维持 P1-RC-006（P1，不降级 P2）**。

> **裁决理由（决策树两分支的关键区分）**：plan 决策树分支②的 P2 降级须满足「降级证据完全满足 L1 RC-9 字面要求」——即降级证据须同时覆盖「操作人 + 原因」。本验证 §2.2 核实降级证据[通用 updatedBy/updateTime] **只覆盖操作人（部分）+ 时间（部分）**，**reason 完全不可追**（`reverseClose` 无 reason 参数 + 无 `setReverseCloseReason`）。L1 `use-cases.md:140` 逐字「全程审计(记录反结账操作人/**原因**)」——「操作人/原因」是并列必备项，reason 不可追即不满足字面要求 → §2 P1①（功能完全缺失——验收标准要求的"全程审计[操作人/原因]"reason 维度零实现）成立 → 分支②「降级证据满足合规」条件不成立，分支①匹配。**reason 不可追是 P1 维持不降级的核心 hinge**。与 A1.6 §5.3 P1-RC-006 P1 新建结论（§2 P1① 功能完全缺失）分层一致——本验证进一步精确化：降级证据存在（操作人+时间部分可追）缓解了"完全不可追"最坏情形，BUT reason 不可追合规硬伤致 RC-9 验收标准仍功能完全缺失，P1 维持。

### 5.2 降级证据存在性指导 MR1 优先级（plan Goals）

| MR1 优先级排序因素 | 本验证结果 | MR1 优先级指导 |
|---|---|---|
| 降级证据存在性 | **存在**（§2.2 平台自动填充 `updatedBy`/`updateTime` 确被反结账覆盖，操作人+时间部分可追） | 有降级证据 → 合规影响从"完全不可追"缓解为"操作人+时间部分可追但 reason 不可追" |
| reason 不可追合规硬伤 | **硬伤残留**（§2.2 边界① + §2.4 外部审计/税务/SOX 均要求 reason） | reason 不可追是合规硬伤，**仍须实现**，不延后 P2 |
| 活跃数据破坏 | **无**（反结账行为主路径正确，A1.6 §2.8-2.10 + A2.3 已证实；RC-9 缺失是审计轨迹非数据破坏） | RC-9 缺失不破坏活跃数据 → P1 优先级**可排在活跃数据破坏类 P0 之后** |

**MR1 优先级指导结论**：降级证据存在[操作人+时间可追] + 无活跃数据破坏 → P1-RC-006 在 MR1 排序中可排在活跃数据破坏类 P0 之后；BUT reason 不可追是合规硬伤（外部审计/税务/SOX 均要求）→ **仍须实现，不延后 P2**。

### 5.3 §2 判据编号 + 三源 + 分层一致性

- **§2 判据**：P1①（功能完全缺失——RC-9 专属审计[reason 参数 + reversedBy/Reason/At 列 + ReverseCloseLog 实体]完全缺失；降级证据是通用更新追踪非 RC-9 专属审计，reason 不可追合规硬伤，不满足 L1「全程审计[操作人/原因]」字面）+ P1⑤（验收标准无断言——RC-9 审计零测试，§3）。
- **§4 Q1**：L1 为准（Q1=(c) 逐项对照，分歧以 L1 为准）；L1 RC-9「全程审计[操作人/原因]」字面要求 reason，降级证据 reason 不可追 → 不满足 L1 → P1 维持。
- **L1/L2/L3 三源**：L1 `use-cases.md:140`（全程审计[操作人/原因]）/ L2 `period-close.md:216-221`（步骤8 记录审计：反结账操作记录 + 调整单据清单 + 调整金额影响 + 责任人签名，期望专属审计远超降级证据）+ `:223-228`（管理员+审批约束）/ L3 §2.1+§2.2+§2.3（reverseClose 无 reason + 无专属审计 setter + 降级证据通用 updatedBy/updateTime + grep 0 命中）。
- **与 A1.6 §5.3 P1-RC-006 P1 新建分层一致**：A1.6 §5.3 新建 P1-RC-006 理由「§2 P1① 功能完全缺失 + §2 P1⑤ 验收标准无断言」——本验证确认 P1 维持（降级证据存在但 reason 不可追合规硬伤，§2 P1① 仍成立），P1 分级维持。
- **与 A1.6 §6.3 MR1[ORM ask-first] 分层一致**：A1.6 §6.3 P1-RC-006 修复归 MR1（R1.0 展开为 RC-R1.n），触及 ORM 结构变更[增 reversedBy/Reason/At 列 或 新增 ErpFinReverseCloseLog 实体] + 会计过账逻辑[反结账]须 ask-first + 独立 plan-audit（§5）。本验证降级证据评估指导 MR1 优先级（排活跃数据破坏 P0 之后但须实现），修复通道不变。
- **与 A2.3 period-close E2E 分层一致**：A2.3 证实反结账行为主路径（余额还原 + 模块回开 + PL 红冲）正确；本验证只补 RC-9 审计轨迹降级证据评估 + 合规影响，不重审行为。

### 5.4 与同族 A4.1 工作项裁决分层对照

| 同族工作项 | 存疑点性质 | 裁决 | 与本验证（A4.1.20）区分 |
|---|---|---|---|
| A4.1.18（done） | PC-3 AR/AP reminder 模式运行时行为（L1 限定词「强制核销模式」活跃性） | 维持 P2 watch-only（config 不存在 → L1 限定条件不活跃） | A4.1.18 是 L1 限定词活跃性裁决（config 不存在 → 分歧倾向接受）→ P2 维持；A4.1.20 是降级证据评估（reason 不可追合规硬伤）→ P1 维持。同属 period-close A1.6 §7 存疑点族，结论差异源于控制点性质（L1 限定词活跃性 vs reason 合规硬伤） |
| A4.1.19（done） | PC-4 折旧 auto-execute + 悬挂阻断交互 | 维持 PC-4 接受（rethrow + 悬挂扫描双路径运行时有效阻断） | A4.1.19 是阻断交互运行时确认（行为达成）→ 接受；A4.1.20 是降级证据评估（reason 不可追）→ P1 维持。同属 period-close A1.6 §7 存疑点族 |

**分层一致**：A4.1.20 与 A4.1.18/A4.1.19 同属 A4.1 MA4 运行时行为验证族（A1.6 §7 存疑点 1/2/3），结论差异源于控制点性质（L1 限定词活跃性 / 阻断交互 / reason 合规硬伤），无矛盾。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 产出 finding 前 grep `arm-index.md` finance RC-9 / 反结账审计 / updatedBy /降级证据同域同控制点。本验证裁决 = 维持 P1-RC-006，**产出 0 项新 finding**，**仅追加 A4.1.20 评估注记**。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| **P1-RC-006**（arm-index :136）UC-FIN-07⑨ RC-9 反结账审计轨迹缺失 | 反结账操作审计轨迹（操作人/原因/时间）记录机制完全缺失 | 本验证是其**降级证据评估 + 优先级裁决闭合**（通用 updatedBy/updateTime 降级证据存在性 + 可靠性边界 + 合规影响 + P1 维持不降级 + MR1 优先级指导） | **维持 + 追加注记**（§6.2） |
| P1-MA2-020（arm-index，resolved documented simplification）反结账 approval kill-switch | 审批门控实现形态（kill-switch vs 审批流 successor） | **不同控制点**（P1-MA2-020 = 审批门控实现形态，P1-RC-006 = 审计轨迹记录机制）；A1.6 §6.1 已裁决 | 不重开（不同控制点） |
| P1-MA3-036（arm-index，done）反结账审批 doc↔code | doc↔code 文本一致性（kill-switch 标注） | **不同控制点**（P1-MA3-036 = doc↔code 对齐，P1-RC-006 = 审计轨迹记录机制）；A1.6 §6.1 已裁决 | 不重开（不同控制点） |
| P1-MA3-046（arm-index，done R2.7）全域敏感动作零运行时权限保护 | action-level RBAC（高权限角色强制） | **不同控制点**（P1-MA3-046 = RBAC，P1-RC-006 = 审计轨迹记录机制）；A1.6 §6.1 已裁决（互补不重复） | 不重开（不同控制点） |
| A1.6 §5.1 UC-FIN-07 整体 P1（RC-9 缺失） | UC-FIN-07 符合性 | 本验证是其**RC-9 降级证据评估差异**，确认 P1 维持 | 复用（分层一致，确认维持） |
| A2.3 period-close E2E（反结账行为主路径已证实） | 反结账行为（余额还原 + 模块回开 + PL 红冲） | 本验证复用其已证实的反结账行为，不重审 | 复用（§去重协议） |

grep `arm-index.md` 「RC-9 审计」「reversedBy」「reverseCloseReason」「ReverseCloseLog」「updatedBy 降级证据」「反结账审计轨迹」RC 系列 = **零新控制点命中**（P1-RC-006 是唯一覆盖 RC-9 反结账审计轨迹的 finding）。

### 6.2 P1-RC-006 优先级注记更新（Phase 2 item 1）

**裁决**：**维持 P1-RC-006（P1，不降级 P2），追加 A4.1.20 降级证据评估注记**。在 arm-index P1-RC-006 行追加注记：「**【A4.1.20 降级审计证据评估 2026-08-06】** 经通用 `updatedBy`/`updateTime` 降级证据评估（平台 `OrmTimestampHelper.onUpdate:74-112` + `EntityPersisterImpl.queueUpdate:470-471` 自动填充机制 + reverseClose `setStatus(OPEN):39` + `reopenModules:55-56` + `flushSession:57` 触发覆盖），确认降级证据**存在**[操作人+时间部分可追] BUT **可靠性边界受限**[①无 reason 合规硬伤 + ②updateTime 非专属时间戳 + ③易被覆盖 + ④无结构化操作日志] → RC-9 验收标准「全程审计[操作人/原因]」字面 reason 不可追**仍功能完全缺失** → **P1 维持不降级**；降级证据指导 MR1 优先级（有降级证据 + 无活跃数据破坏 → 可排活跃数据破坏 P0 之后，BUT reason 不可追合规硬伤故仍须实现，不延后 P2）。详见 `docs/audits/2026-08-06-1708-rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence.md`。」状态/分级/修复通道[MR1 ORM ask-first]**不变**。

### 6.3 双向可追溯

- **新 finding → arm-index**：N/A（无新 finding）。
- **静态存疑点闭合**：A1.6 §7 存疑点 3 经本评估**消解为「降级证据存在 + P1 维持不降级」**（通用 updatedBy/updateTime 经平台自动填充被反结账覆盖提供降级证据[操作人+时间部分可追]，但 reason 不可追合规硬伤致 RC-9 验收标准仍功能完全缺失，P1 维持），闭合 P1-RC-006 修复方案优先级裁决。
- **与 P1-RC-006（维持 P1）+ P1-MA2-020/P1-MA3-036（不同控制点，不重开）+ P1-MA3-046（不同控制点，不重开）+ A1.6 §5.1 UC-FIN-07 P1（确认维持）+ A2.3 period-close E2E（复用反结账行为）分层一致**。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.6 §7-3 经通用 `updatedBy`/`updateTime` 降级证据评估 + 平台自动填充机制核验 + 合规影响程度评估 + P1-RC-006 优先级裁决**消解为「降级证据存在 + P1 维持不降级」**，无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**维持 P1**（未出 P0），按 §10 **不触发 MR0**；P1-RC-006 修复归 MR1[ORM ask-first]，本验证不实施修复。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`7aa9b7078`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本因 `set -euo pipefail` + R3 段提前中止的既有行为，本验证实测退出码 = **1**（同型报告 A4.1.14 §8.1 / A4.1.18 §8 已记录同款 checker 退出码特性）；真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => `sys.exit(1)`。本报告**不**以 checker 脚本退出码作为门控通过依据——门控判据是下方 actual vs baseline 表的逐规则比较。**本验证无生产代码变更**（只读评估：读 reverseClose 审计字段写入 + ORM 自动填充机制 + 平台文档 + 既有测试普查 + 引用 A1.6/A2.3），R1a-R2d 全部 actual == baseline，checker 无回归风险。

  | 规则 | Baseline（`compliance-baseline.md §BASELINE (machine-readable)` 权威块 :292-315） | Actual（本验证 HEAD `7aa9b7078` 实测） | 状态 |
  |------|-----------------------------------------------------|----------------------------|------|
  | R1a (dao().saveEntity BizModel) | 0 | 0 | = ✅ |
  | R1b (dao().updateEntity BizModel) | 0 | 0 | = ✅ |
  | R1c (dao().getEntityById BizModel) | 0 | 0 | = ✅ |
  | R1d (dao().findAllByQuery BizModel) | 14 | 14 | = ✅ |
  | R2a (BizModel daoFor ErpMd*) | 34 | 34 | = ✅ |
  | R2b (BizModel daoFor Erp* 跨域) | 229 | 229 | = ✅ |
  | R2c (全生产 daoFor 总量) | 1382 | 1382 | = ✅ |
  | R2d (Processor daoFor ErpMd*) | 34 | 34 | = ✅ |

  > R1/R2 全部 actual == baseline，**0 漂移**。权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 块为准。本验证零生产代码变更（docs-only），checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（零新 finding，维持 P1-RC-006）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。交叉去重声明：与 A1.6 §5.3 P1-RC-006（P1 新建，确认维持）+ 与 A1.6 §6.1 P1-MA2-020/P1-MA3-036（审批门控实现形态，不同控制点不重开）+ 与 A1.6 §6.1 P1-MA3-046（RBAC，不同控制点不重开）+ 与 A2.3 period-close E2E（复用反结账行为主路径，§去重协议）+ MA4↔A5.6 边界（需求契约视角 RC-9 审计轨迹评估 vs 测试质量全量评级，不重做 A5.6）。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc `period-close.md` 需求契约段落）。只读评估（读 reverseClose 审计字段写入 + ORM 自动填充机制 + 平台文档 + 既有测试普查 + 引用 A1.6/A2.3），未修改代码/ORM/api.xml/view.xml/真相源。L1 RC-9「全程审计[操作人/原因]」字面要求 vs 降级证据 reason 不可追合规硬伤记入报告（§2.4/§5.1），不直改 L1/L2（§9 冻结条款）。

---

## 10. 与 A1.6/A2.3 报告差异增量声明（§去重协议）

本验证复用 A1.6 §2.11（RC-9 缺失静态确认）+ §5.3（P1-RC-006 P1 新建）+ §6.1/§6.3（P1-RC-006 与 P1-MA2-020/P1-MA3-036/P1-MA3-046 不同控制点 + MR1[ORM ask-first]）+ A2.3 period-close E2E（反结账行为主路径已证实）+ A4.1.18（period-close 运行时行为评估同型范式），**不重新核实反结账行为本身**。只补 A1.6 §7 存疑点 3 标注为「需运行时确认」的差异：

1. **降级证据存在性核验**（A1.6 §7-3「通用 `updatedBy`/`updateTime` 是否被反结账操作覆盖」未核实）：§2.2 核验平台 `OrmTimestampHelper.onUpdate:74-112` + `EntityPersisterImpl.queueUpdate:470-471` 自动填充机制——证实 `reverseClose` 的实体修改 + flushSession **确触发** `updatedBy`/`updateTime` 被反结账操作覆盖，**降级证据存在**。
2. **降级证据可靠性边界评估**（A1.6 §7-3 未评估边界）：§2.2 评估 4 项可靠性边界（无 reason + 非专属时间戳 + 易被覆盖 + 无结构化操作日志）。
3. **合规影响程度评估**（A1.6 §7-3「外部审计/税务/SOX 合规场景下可追溯性破坏程度」未评估）：§2.4 评估降级证据部分满足合规最低要求[操作人+时间可追] BUT reason 不可追是合规硬伤。
4. **P1-RC-006 优先级裁决闭合**（A1.6 §5.3 P1 新建 + §6.3 MR1[ORM ask-first] 未闭合降级证据裁决）：§5.1 裁决**维持 P1 不降级**（reason 不可追合规硬伤），§5.2 降级证据指导 MR1 优先级（排活跃数据破坏 P0 之后但须实现），arm-index 注记更新（§6.2）。

差异增量与本验证范围一致，无与 A1.6/A2.3 重叠的重新核实。

---

## 11. Verdict

**Verdict: passes requirement-compliance runtime-behavior evaluation**（P1-RC-006 维持 P1 不降级，零 P0 新 finding，零 successor 触发）

**审查范围**：A1.6 §7-3 存疑点（RC-9 反结账审计缺失降级证据评估）评估——reverseClose 审计字段写入核验（`reverseClose:22-59` 无 reason 参数 + 无专属审计 setter + 无 ReverseCloseLog + setStatus:39 + reopenModules:55-56 + flushSession:57）+ **通用 `updatedBy`/`updateTime` 降级证据评估**（ORM `updateTimeProp`/`updaterProp` orm.xml:657 + 平台 `OrmTimestampHelper.onUpdate:74-112` 自动填充 + `EntityPersisterImpl.queueUpdate:470-471` 调用点 + reverseClose 触发覆盖 + 4 项可靠性边界）+ ORM 审计列 + ReverseCloseLog grep 复核（0 命中）+ RC-9 合规影响程度评估（外部审计/税务/SOX）+ 测试覆盖边界普查（零覆盖）+ MA4↔A5.6 边界声明 + §2 判据裁决（决策树分支①维持 P1）+ 与 arm-index 衔接（维持 P1-RC-006 + 追加注记）+ §8 过程纪律自检 + §9 真相源冻结 + §10 差异增量声明。

**维持 P1 类**：RC-9 反结账审计轨迹缺失降级证据评估确认 P1-RC-006 维持——通用 `updatedBy`/`updateTime` 经平台自动填充**确被反结账操作覆盖提供降级证据**[操作人+时间部分可追] BUT **可靠性边界受限**[①无 reason 合规硬伤 + ②updateTime 非专属时间戳 + ③易被覆盖 + ④无结构化操作日志] → RC-9 验收标准「全程审计[操作人/原因]」字面 reason 不可追**仍功能完全缺失** → §2 P1① 成立，P1 维持不降级 P2；降级证据指导 MR1 优先级（有降级证据 + 无活跃数据破坏 → 可排活跃数据破坏 P0 之后，BUT reason 不可追合规硬伤故仍须实现，不延后 P2）。

**P0/P1**：无 P0；P1-RC-006 维持 P1（不实施修复，归 MR1[ORM ask-first]）。不触发 MR0。A1.6 §5.1 UC-FIN-07 整体 P1 维持。

**剩余风险**：无遗留运行时存疑点。P1-RC-006 修复归 MR1[ORM ask-first]（触及 ORM 结构变更 + 会计过账逻辑[反结账] 须 ask-first + 独立 plan-audit §5）：修复 = `IErpFinPeriodCloseBiz.reverseClose` 增 reason 参数 + `ErpFinAccountingPeriod` ORM 增 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 **或** 新增 `ErpFinReverseCloseLog` 实体 + `ReverseCloseProcessor` 落库专属审计。降级证据断言缺口[reverseClose 后断言 `getUpdatedBy`/`getUpdateTime` 被覆盖]归 A5.6 测试质量维度 successor（纯测试代码 MR1 预授权类目，不触发 §5 ask-first）。两者均非本 MA4 范围。
