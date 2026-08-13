# 固定资产域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 资产域有两类状态对象：**资产卡片**（生命周期状态机）与**折旧计划条目**（简单执行状态）。本文件主要覆盖资产卡片状态机（§适用对象），折旧计划条目状态较简单（见末节）。资产移动单（ErpAstMovement）双轴状态机见 §适用对象二。

## 适用对象

资产卡片（Asset）：一项固定资产从登记到处置的完整生命周期。

## 1. 状态定义

每个状态表达"资产当前处于什么阶段"：

| 状态 | 业务含义 | 计提折旧 | 参与报表 |
|------|----------|----------|----------|
| 草稿（DRAFT） | 卡片已创建未生效，等待资本化入账 | 否 | 否 |
| 使用中（IN_SERVICE） | 已入账，正常使用并计提折旧 | 是 | 是（固定资产原值/累计折旧） |
| 闲置（IDLE） | 暂停使用（如设备停用但仍保留）⚠ **预留状态** | 可配（默认停提） | 是（⚠ 本期无 writer / 无迁移实现，见下方 Deferred 补注） |
| 已报废（SCRAPPED） | 终态：报废处置完成 | 否 | 否（已清理） |
| 已出售（SOLD） | 终态：出售处置完成 | 否 | 否（已清理） |

持久化状态码字典以 `model/app-erp-assets.orm.xml` 为准。

## 2. 迁移完整性

```
草稿 (DRAFT)
  └─ 资本化入账 → 使用中 (IN_SERVICE)
                    ├─ ⚠ 预留(Deferred) → 暂停使用 → 闲置 (IDLE)         ← 本期无 writer，不可达
                    │              └─ ⚠ 预留(Deferred) → 恢复使用 → 使用中 (IN_SERVICE)  ← 本期无 writer，不可达
                    ├─ 报废处置 → 已报废 (SCRAPPED)
                    └─ 出售处置 → 已出售 (SOLD)
```

每条迁移的触发、前置、结果：

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT → IN_SERVICE | 资产管理员 | 卡片完整、取得日期有效、折旧方法与年限已配置 | 触发资本化入账凭证（借固定资产/贷在建工程或存货） |
| IN_SERVICE → IDLE ⚠ **Deferred** | 资产管理员 | 使用中状态 | **本期 Deferred**（无 suspend/toIdle BizMutation writer）。目标语义：可配置是否停提折旧（默认停）。**Successor**：资产暂停/恢复业务上线时实现 |
| IDLE → IN_SERVICE ⚠ **Deferred** | 资产管理员 | 闲置状态 | **本期 Deferred**（无 resume/fromIdle BizMutation writer）。目标语义：恢复折旧计提。**Successor**：同上 |
| IN_SERVICE/IDLE → SCRAPPED | 资产管理员/管理员 | 资产存在、未处置 | 触发报废清理凭证（结转原值/累计折旧/清理损失） |
| IN_SERVICE/IDLE → SOLD | 资产管理员/管理员 | 资产存在、未处置、出售金额已定 | 触发出售清理凭证（结转原值/累计折旧/出售收入/清理损益） |

> **Deferred**：资产卡片 `IN_SERVICE↔IDLE` 暂停/恢复迁移为**预留死状态**——`ErpAstConstants.ASSET_STATUS_IDLE = "IDLE"` 常量与 `erp-ast/asset-status` dict 值保留，但本期全 `module-assets` **零 `setStatus(...IDLE)` / `setAssetStatus(ASSET_STATUS_IDLE)` writer**，`ErpAstAssetBizModel` 为 CrudBizModel 桩（17 行，零状态机 mutation），亦无 suspend/resume/toIdle/fromIdle BizMutation。IDLE 当前仅出现在 3 处只读守卫（`ErpAstValueAdjustmentProcessor:204` + `ErpAstDisposalProcessor:200` 读取排除 + `ErpAstInventoryProcessor:193` 盘点范围过滤）。**折旧默认停提语义已等价满足**：折旧引擎 `ErpAstDepreciationScheduleProcessor` 与 KPI（`ErpAstDashboardBizModel:179`）仅查询 `IN_SERVICE`，等价于 owner doc §1「IDLE 默认停提」设计意图，无悬挂数据。IN_SERVICE 折旧主路径（资本化建卡→IN_SERVICE→期末批量折旧→处置终态）完整不受影响。处置：采纳 Decision Deferred（保留 dict 值为预留语义入口 + owner doc 标注，对齐「保留为预留 + 文档 Deferred」先例），不从 ORM 删除。**Successor 触发条件**：PM 要求正式资产闲置/恢复工作流时实现 `suspend`/`resume` BizMutation（IN_SERVICE↔IDLE 迁移 + setStatus writer）+ 折旧引擎扩展查询 IN_SERVICE+IDLE + 闲置超期 TODO cron（经 `IErpSysNotificationBiz`）。

## 3. 终态与恢复

- **终态**：`已报废（SCRAPPED）`、`已出售（SOLD）`。
- **终态不可恢复**：已处置的资产不可重新激活；若处置错误，需通过"处置冲销"（反向清理凭证）+ 重新处置，而非状态回退。
- **草稿可删除**：未入账的草稿卡片可物理删除（无凭证影响）。
- **使用中/闲置不可删除**：只能处置（报废/出售），保留审计轨迹。

## 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 折旧计提时已结账 | 拒绝计提，需反结账期间或计入当前开启期间 |
| 折旧后账面净值低于残值 | 直线法按残值预计算，不会出现；其他方法需校验并截断 |
| 处置时累计折旧与原值不符 | 拒绝处置，提示先核对折旧记录 |
| 资本化凭证生成失败 | 资产保持草稿，`posted=false`，异步重试 + 派发 `ast.capitalization-posting-failure` 告警（G4 错误传播分级） |
| 资产类别科目映射缺失 | 折旧/处置凭证报错，等待人工配置科目映射；派发告警 |
| 期间结账后才发现折旧漏提 | 反结账补提，或在当前期间补提（补提凭证注明归属期间） |
| 并发折旧同一资产 | 乐观锁 |
| 重复折旧（幂等） | 同期已执行的折旧再次触发为空操作 |

### 实现约定：reverseApprove posted=false 窗口不对称

资本化/处置 `reverseApprove` 仅在 `posted=true` 时回滚资产行为（资本化：资产→DRAFT + cancelSchedules；处置：资产→IN_SERVICE + restoreSchedules）。**posted=false 窗口期** reverseApprove 仅设业务单据 REJECTED，资产保持终态（IN_SERVICE/SCRAPPED/SOLD）+ schedules 保持 PENDING/CANCELLED。这是 deliberate 不对称（Phase 3 方案 B）：posted=false 悬挂经 `DeferredPostingSweepJob`（Cap/Disposal）兜底重试 + `IErpSysNotificationBiz` 告警闭环；运营在悬挂窗口期需先触发 sweep 重试或手工 reverse 凭证再 reverseApprove。错误传播分级见 `posting-log.md §错误传播分级策略` G2/G4。

## 5. 可达性

> **⚠ 本期可达性修正（Deferred）**：下方为**目标状态机的可达性**。本期 IDLE 因零 writer **不可达**——`IN_SERVICE→IDLE` / `IDLE→IN_SERVICE` 迁移未就绪（见 §2 Deferred 补注）。IDLE 本期保留为预留语义入口（不删除 dict 值）。本期实际可达集为 `DRAFT → IN_SERVICE → {SCRAPPED, SOLD}`；IDLE 及其出边为 successor。

- 从 DRAFT 可达 IN_SERVICE；从 IN_SERVICE 可达 IDLE、SCRAPPED、SOLD。
- IDLE 可回到 IN_SERVICE，也可直接处置。
- 无不可达状态，无死锁。终态（SCRAPPED/SOLD）无出边。
- 无循环（IDLE→IN_SERVICE→IDLE 是合法往复，退出条件为处置到终态）。

## 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| 资本化入账（DRAFT→IN_SERVICE） | 资产管理员 |
| 暂停/恢复（IN_SERVICE↔IDLE） | 资产管理员 |
| 报废处置 | 资产管理员 + 审批（因影响资产清理与报表） |
| 出售处置 | 资产管理员 + 审批 + 财务确认（因涉及出售收入） |
| 折旧执行 | 财务员 / 系统（期末自动） |

危险操作：
- **报废/出售处置**：需审批，因结转资产价值影响报表。
- **期末批量折旧**：高影响（影响所有资产），需财务员权限 + 确认。

## 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 折旧/处置凭证生成 | 通过 `IErpFinAcctDocProvider` 跨工程聚合，财务域按 businessType（DEPRECIATION/CAPITALIZATION/DISPOSAL）生成凭证 |
| 期末批量折旧 | 财务域期末结账触发，或资产域定时任务 |
| 资本化的库存出库 | 调用 `IErpInvStockMoveBiz` 生成出库移动单（库存转固场景） |

## 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 是 | assigned（资产管理员）—— 草稿待完善入账 |
| IN_SERVICE | 否（正常折旧自动） | — |
| IDLE ⚠ **Deferred** | 是（目标） | monitor（监控）—— 闲置资产待决策（恢复/处置）。⚠ **本期 IDLE 不可达**（无 writer，见 §2/§5 Deferred 补注），故本期不产生 IDLE TODO |
| SCRAPPED/SOLD | 否 | — |

**待处置资产提醒**：闲置超期的资产可产生 TODO 提醒决策（恢复使用或处置），避免资产长期闲置无人处理。

> **⚠ 闲置超期 TODO cron Deferred**：本提醒依赖 IDLE 状态写入 + 闲置时长扫描 cron，两者本期均未就绪（IDLE 零 writer，见 §2 Deferred 补注）。处置：Deferred。**Successor 触发条件**：PM 要求正式资产闲置/恢复工作流时，随 `suspend`/`resume` BizMutation 一并实现闲置超期 TODO 经 `IErpSysNotificationBiz` 派发。

## 9. 场景演练

### 场景 A：设备购置与折旧 happy path

1. 购置设备 → 创建资产卡片（DRAFT）。
2. 资本化入账 → DRAFT → IN_SERVICE → 生成入账凭证（借固定资产 10 万 / 贷银行存款 10 万）。
3. 每月期末折旧 → 生成折旧凭证（借折旧费用 / 贷累计折旧），折旧计划条目状态更新。
4. 5 年后账面净值 = 残值，折旧停止。

### 场景 B：资产闲置与恢复

1. 设备停用 → IN_SERVICE → IDLE → 停止折旧计提（配置默认停）。
2. 半年后重新启用 → IDLE → IN_SERVICE → 恢复折旧（剩余折旧期内继续计提）。

### 场景 C：资产报废

1. 设备报废 → IN_SERVICE → SCRAPPED。
2. 生成清理凭证：借累计折旧（已提部分）/ 借清理损失 / 贷固定资产原值。
3. 资产退出报表。

### 场景 D：折旧漏提补提

1. 6 月资产折旧漏提，7 月发现。
2. 选项一：反结账 6 月 → 补提折旧 → 重新结账。
3. 选项二：在 7 月补提（补提凭证注明"补提 6 月折旧"）。

## 10. 与设计文档一致性

- 资产与折旧模型见 `assets/README.md`。
- 状态码持久化值归 `model/app-erp-assets.orm.xml`。
- 业财打通机制见 `finance/posting.md`（businessType 含 DEPRECIATION/CAPITALIZATION/DISPOSAL）。
- 期末折旧与会计期间结账的关系见 `finance/state-machine.md`。

## 适用对象二：资产移动单（ErpAstMovement）

> 本节由 plan `2026-08-13-0805-2`（M3.15 docStatus + M3.16 approveStatus）补章节落地——owner doc 原仅在「实现模式与守卫边界」散文段提及 Movement INLINE 路径，无矩阵化 §适用对象章节。本节集中建立 `ErpAstMovement` 双轴（`approveStatus` 审批轴 + `docStatus` 退化分类轴）的权威迁移语义与退化轴裁定登记。
>
> 实体级状态机 Bean：`ErpAstMovementApprovalStateMachine`（approveStatus 5 动作矩阵）+ `ErpAstMovementDocumentStateMachine`（docStatus 退化分类 Bean——`transitions()` 空 + 集中化 `isCancelled(status)` 只读守卫）。双轴各自独立 Bean（契约 §1/§3 双轴分离 + `Approval`/`Document` 后缀命名）。

### 1. 状态定义

#### approveStatus 审批轴（dict `wf/approve-status`）

| approveStatus | 业务含义 | 可达性 |
|---------------|----------|--------|
| 未提交（UNSUBMITTED） | 移动单新建，等待提交审批 | 初始态（CRUD 创建写入）；withdrawApproval 可回到此态 |
| 已提交（SUBMITTED） | 已提交待审核 | submitForApproval 从 UNSUBMITTED/REJECTED 进入 |
| 已审核（APPROVED） | 审核通过（业务终态，可逆） | approve 从 SUBMITTED 进入 |
| 已驳回（REJECTED） | 审核驳回 | reject 从 SUBMITTED 进入；reverseApprove 从 APPROVED 进入 |

#### docStatus 退化分类轴（dict `erp/doc-status`）

| docStatus | 业务含义 | 可达性 |
|-----------|----------|--------|
| 草稿（DRAFT） | 单据初始生命周期态 | 初始态（CRUD 创建写入） |
| 已生效（ACTIVE） | **预留死状态** | dict 内有值，Movement 零命名动作 writer 可达（保留为预留语义入口） |
| 已作废（CANCELLED） | 终态：逻辑删除 | 经 `useLogicalDelete` 承载（无独立 cancel mutation） |

### 2. 迁移完整性

#### approveStatus 审批轴迁移矩阵（5 命名动作，6 条边）

```
未提交 (UNSUBMITTED) / 已驳回 (REJECTED)
  └─ submitForApproval → 已提交 (SUBMITTED)
                          ├─ approve → 已审核 (APPROVED)
                          │             └─ reverseApprove → 已驳回 (REJECTED)
                          ├─ reject → 已驳回 (REJECTED)
                          └─ withdrawApproval → 未提交 (UNSUBMITTED)
```

| 动作 | 来源态 | 目标态 | 审计字段 |
|------|--------|--------|----------|
| submitForApproval | UNSUBMITTED / null / REJECTED | SUBMITTED | — |
| approve | SUBMITTED | APPROVED | approvedBy + approvedAt |
| reject | SUBMITTED | REJECTED | approvedBy + approvedAt |
| reverseApprove | APPROVED | REJECTED | approvedBy + approvedAt 置空 |
| withdrawApproval | SUBMITTED | UNSUBMITTED | — |

5 动作均前置 `isCancelled(docStatus)` 防御守卫（CANCELLED 单据禁止审批操作）。

> **reverseApprove 目标态裁定**：reverseApprove→REJECTED（非 SUBMITTED），对齐 `domain-design-guidelines.md §16.4` + assets 域 R1.x 既有行为。Bean `reverseApproveTargetStatus()`=REJECTED。

#### docStatus 退化轴声明（layer-2 四方对照裁定）

本轴为**退化分类轴**：

- **零命名动作迁移 writer**：全仓无 `setDocStatus(...)` 生产 writer（grep 零命中），无 cancel/activate BizMutation。
- **CANCELLED 经 `useLogicalDelete` 承载**：Movement 实体配 `useLogicalDelete="true"`（`app-erp-assets.orm.xml:396`），逻辑删除时置 CANCELLED，无独立 cancel mutation。
- **ACTIVE = 预留死状态（intentional reserved）**：dict 含值但零 writer 可达。Bean `transitions()` 返回**空列表**（零迁移边），ACTIVE 不在 `initialStatuses`/`terminalStatuses`/`transitions` 任一集合。
- **裁定（Decision）**：分类 = `intentional reserved`。dict 值保留（**不从 ORM 删除**——对齐资产卡片 IDLE + Contract CANCELLED + hr SUSPENDED 先例：保留优于删除）。
- **Successor**：资产移动单独立 cancel/activate 工作流需求时，开独立 plan 实现命名动作 mutation + 填充 Bean `transitions()` 边。

### 3. 终态与恢复

- **approveStatus 终态**：APPROVED 为业务终态（「可逆终态」——经 reverseApprove 有出边，故不适用「终态无出边」强断言）。REJECTED 非终态（经 submitForApproval 可重新提交）。
- **docStatus 终态**：CANCELLED 为经 useLogicalDelete 可达的终态。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| CANCELLED 单据执行审批动作 | `isCancelled` 防御守卫阻断（抛 `nop.err.wf.approve.doc-cancelled`） |
| 非来源态执行审批动作 | Bean `assertCan<Action>` 报告 common 层非法迁移码（`nop.err.erp.common.illegal-status-transition`，契约 §7） |
| 资产移动单无 posted 副作用 | 不过账、无凭证需 reverse；reverseApprove 仅清空 approvedBy/At + 置 REJECTED |

### 5. 可达性

- approveStatus：从 UNSUBMITTED 可达全部 3 非初始态（SUBMITTED/APPROVED/REJECTED）；REJECTED 经 submitForApproval 可回到 SUBMITTED（驳回后重新提交循环合法）。
- docStatus：DRAFT 为初始态；CANCELLED 经 useLogicalDelete 可达（终态）；ACTIVE 死状态不可达。

### 6. 角色与权限

| 动作 | `<auth permissions>` 声明 |
|------|---------------------------|
| approve | `ErpAstMovement:approve` |
| reverseApprove | `ErpAstMovement:reverseApprove` |
| submitForApproval / reject / withdrawApproval | 无额外 auth 声明（默认） |

### 7. 实现模式：INLINE→Bean 迁移（首个范式）

Movement 审批状态逻辑原 100% 内联在 `ErpAstMovement.xbiz` 的 `<source>` 脚本（无 `ErpAstMovement*Processor` Java 类）。本节落地后：

- **xbiz source 经 `inject('FQN')` 取得双 Bean**（验证 xbiz source 可注入并调用 Bean，首个 INLINE→Bean 迁移范式）。
- approveStatus 守卫 → `approvalStateMachine.assertCan<Action>(entity.approveStatus)`；目标态 → `entity.approveStatus = approvalStateMachine.<action>TargetStatus()`。
- docStatus 防御守卫 → `documentStateMachine.isCancelled(entity.docStatus)`（boolean，xbiz 保留领域码 `nop.err.wf.approve.doc-cancelled` 抛出）。
- **保留**：`<auth permissions>` 声明、approvedBy/approvedAt 置位与置空、`<x:extends>` 继承结构。

> **错误码迁移说明（契约 §7）**：approveStatus 非法边由 Bean 抛 common 层码（`nop.err.erp.common.illegal-status-transition`，携带 action/currentStatus 元数据）；Movement 无 Processor 层做领域码映射（XScript 不支持 try-catch），故 common 码直接传播。doc-cancelled 守卫经 `isCancelled()` boolean helper 委托，xbiz 保留领域码 `nop.err.wf.approve.doc-cancelled`（错误码对外不变）。

## 实现模式与守卫边界

> 资产域移动单（ErpAstMovement）审批轴动作的实现模式补注。完整双轴矩阵见 §适用对象二。

**PROC 路径**（资本化/处置/拆分/合并/价值调整 等 `ErpAst*Processor`）：含完整业务守卫 + 过账联动。

**INLINE→Bean 路径**（Movement 全 5 动作：submitForApproval/approve/reject/reverseApprove/withdrawApproval）：原直接在 xbiz `<source>` 脚本中实现，现**已完成 INLINE→Bean 迁移**（plan `2026-08-13-0805-2`）——xbiz source 经 `inject` 取得 `ErpAstMovementApprovalStateMachine` + `ErpAstMovementDocumentStateMachine` 双 Bean，固定守卫/目标态委托 Bean，动态守卫（isCancelled 防御 + approvedBy/At 审计）保留原位。Movement 无独立 cancel mutation，docStatus=CANCELLED 经 `useLogicalDelete` 承载，isCancelled 守卫为防御性（阻止逻辑删除单据的 approveStatus 副轴漂移）。

### IDLE 暂停/恢复：本期 Deferred

资产卡片 `IN_SERVICE↔IDLE` 暂停/恢复迁移本期**无 Processor / 无 INLINE 实现 / 无 BizMutation writer**（`ErpAstAssetBizModel` 为 CrudBizModel 桩）。「IDLE 默认停提折旧」语义由折旧引擎仅查询 `IN_SERVICE` 等价满足（`ErpAstDepreciationScheduleProcessor:138` + `ErpAstDashboardBizModel:179` 均 `eq("status", ASSET_STATUS_IN_SERVICE)`），无需显式停提配置即达成 owner doc §1 设计意图。详细裁决与 successor 见 §2 Deferred 补注。

### reversal listener 回退目标态

资产域移动单无 posted 副作用（不过账、无凭证需 reverse），故无 reversal listener 回退；reverseApprove 目标态为 REJECTED（与其他域对齐，owner doc §16.4）。

## 折旧计划条目状态（简单）

折旧计划按资产生成，每期一条条目，状态简单：

| 状态 | 含义 |
|------|------|
| 待执行（PENDING） | 折旧计划已生成但未执行 |
| 已执行（EXECUTED） | 折旧已计提，凭证已生成 |
| 已冲销（REVERSED） | 冲销的折旧（红字凭证） |

折旧计划条目状态由折旧执行动作驱动，不是独立工作流，因此不展开完整 10 维度。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 处置（报废/出售）的清理凭证是否完整结转原值与累计折旧。
- 闲置资产的折旧停提/恢复配置是否明确。（⚠ IDLE 本期 Deferred——`IN_SERVICE↔IDLE` 迁移未就绪、零 writer；折旧引擎仅查 `IN_SERVICE` 等价于「IDLE 默认停提」。审查时应确认文档未声称已就绪 IDLE 暂停/恢复，successor = 资产闲置工作流上线时。）
- 终态（报废/出售）是否真正无出边，处置错误的纠正路径是否清晰。
- 折旧漏提补提路径是否覆盖（反结账 vs 当期补提）。
- 资本化入账与库存出库的协作（库存转固场景）。
- 资产移动单（适用对象二）：approveStatus 5 动作矩阵 + docStatus 退化轴（ACTIVE 死状态 reserved，CANCELLED 经 useLogicalDelete）；reverseApprove→REJECTED 目标态（§16.4）；INLINE→Bean 迁移（xbiz source inject 双 Bean）。

## 已知限制：浏览器层 xwf 审批路径（ErpAstDisposal）

> M-2 补充。

资产处置单 **ErpAstDisposal** 的 `useWorkflow="true"` xwf 审批轴在浏览器层 E2E 不可达：

- 根因：nop-wf `WorkflowEngineImpl.newSteps` 在浏览器层 `submitForApproval` 时 fallback `sysUser(0)` 作 step owner，但 `NopAuthUser.userId` 因 `tagSet="seq"` 覆盖显式 "0" 为 UUID，致 `allowCallByUser:1053` 拒绝。
- **替代路径**：处置单的 DIRECT 三轴审批（`approveStatus` DIRECT，`docs/plans/2026-07-05-0540-3` 范式）不依赖 xwf，浏览器层 E2E 可达。
- **影响范围**：本状态机的 `IN_SERVICE → SCRAPPED` / `IN_SERVICE → SOLD` 迁移由 DIRECT 审批驱动的过账触发，浏览器层可达；多级审批链业务场景仅在浏览器层外覆盖。
- **解除条件**：见 `docs/design/roles-and-permissions.md §浏览器层审批路径已知限制`。
