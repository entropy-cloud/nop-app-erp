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
| 闲置（IDLE） | 暂停使用（如设备停用但仍保留）✅ **已实现**（RC-R1.54 suspend/resume） | 停提（引擎 IN_SERVICE-only） | 是 |
| 已报废（SCRAPPED） | 终态：报废处置完成 | 否 | 否（已清理） |
| 已出售（SOLD） | 终态：出售处置完成 | 否 | 否（已清理） |

持久化状态码字典以 `model/app-erp-assets.orm.xml` 为准。

## 2. 迁移完整性

```
草稿 (DRAFT)
  └─ 资本化入账 → 使用中 (IN_SERVICE)
                    ├─ ✅ suspend → 闲置 (IDLE)
                    │              └─ ✅ resume → 使用中 (IN_SERVICE)
                    ├─ 报废处置 → 已报废 (SCRAPPED)
                    └─ 出售处置 → 已出售 (SOLD)
```

每条迁移的触发、前置、结果：

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| DRAFT → IN_SERVICE | 资产管理员 | 卡片完整、取得日期有效、折旧方法与年限已配置 | 触发资本化入账凭证（借固定资产/贷在建工程或存货） |
| IN_SERVICE → IDLE ✅ **已实现**（RC-R1.54） | 资产管理员 | 使用中状态 | `suspend` BizMutation（`ErpAstAssetSuspendResumeProcessor`）→ IDLE，停提折旧（引擎仅查 IN_SERVICE + `validateAssetInService` 拒绝 IDLE）；暂停时点经 remark「闲置自 {date}」强制记录（闲置时长派生的时间基准，idleSince 列不落 ORM） |
| IDLE → IN_SERVICE ✅ **已实现**（RC-R1.54） | 资产管理员 | 闲置状态 | `resume` BizMutation → IN_SERVICE，恢复计提（PENDING 计划保留，后续执行自然恢复——Decision A） |
| IN_SERVICE/IDLE → SCRAPPED | 资产管理员/管理员 | 资产存在、未处置 | 触发报废清理凭证（结转原值/累计折旧/清理损失） |
| IN_SERVICE/IDLE → SOLD | 资产管理员/管理员 | 资产存在、未处置、出售金额已定 | 触发出售清理凭证（结转原值/累计折旧/出售收入/清理损益） |

> **✅ 已实现（RC-R1.54，P1-MA2-061 收敛）**：资产卡片 `IN_SERVICE↔IDLE` 暂停/恢复迁移落地——`ErpAstAssetBizModel` 增 `suspend(assetId)`/`resume(assetId)` @BizMutation（`ErpAstAssetSuspendResumeProcessor` per-mutation Processor），固定来源/目标态判断委托 `ErpAstAssetStateMachine` Bean（契约 §4/§7：`assertCanSuspend` IN_SERVICE-only / `assertCanResume` IDLE-only / `suspendTargetStatus`=IDLE / `resumeTargetStatus`=IN_SERVICE，非法边 common 码作 cause → 领域码 `ERR_AST_ASSET_ILLEGAL_STATUS_TRANSITION`）。**折旧行为语义**：批量仅查 IN_SERVICE（`ExecuteBatchDepreciationProcessor`）+ `validateAssetInService` 拒绝 IDLE + catchUp 拒绝 IDLE（Phase 1 守卫）——「IDLE 期间不计提」天然成立（UC-AST-03 ③）；PENDING 计划 suspend 期间保留（Decision A，不 cancel 不重建）。**处置路径**：`assertCanDispose` 扩展接受 IDLE（对齐本表「IN_SERVICE/IDLE → SCRAPPED/SOLD」契约），IDLE 资产可经处置直接退出（无补提——闲置期无折旧义务，损益按卡片账面计提为准）。**闲置超期提醒 cron**：仍 Deferred → successor（触发条件：运营要求闲置资产自动提醒决策时，按 R1.4 job 范式立项）。

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

> **✅ 已实现（RC-R1.54）**：IDLE 经 `suspend` 从 IN_SERVICE 可达，经 `resume` 回 IN_SERVICE、经处置至 SCRAPPED/SOLD。本期实际可达集为 `DRAFT → IN_SERVICE → {IDLE, SCRAPPED, SOLD}`。

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
| IDLE ✅ **已实现**（RC-R1.54） | 否（提醒 cron Deferred → successor） | monitor（监控）—— 闲置资产待决策（恢复/处置）。⚠ **闲置超期 TODO cron Deferred**，见下方补注 |
| SCRAPPED/SOLD | 否 | — |

**待处置资产提醒**：闲置超期的资产可产生 TODO 提醒决策（恢复使用或处置），避免资产长期闲置无人处理。

> **⚠ 闲置超期 TODO cron Deferred → successor**：IDLE 状态写入已实现（RC-R1.54 suspend/resume），但闲置时长扫描 cron（config-gated job.yaml 注册 + 扫描 IDLE + 闲置时长超阈值经 `IErpSysNotificationBiz` 派发）为运营便利性，非 L1 UC-AST-03 字面断言（断言①②③均已被 suspend/resume + 引擎语义覆盖），本期登记 successor。**Successor 触发条件**：运营要求闲置资产自动提醒决策时，按 R1.4 job 范式立项（闲置时长可经 remark「闲置自 {date}」+ 查询派生）。

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

## 适用对象三：资产价值调整文档双轴（Disposal / Capitalization / ValueAdjustment）

> 本节由 plan `2026-08-14-1931-2`（M4.42–M4.47）补章节落地——owner doc 原仅在「实现模式与守卫边界」散文段提及三文档实体 PROC 路径，无矩阵化 §适用对象章节。本节集中建立三实体的双轴（`approveStatus` 审批轴 + `docStatus` 业务生命周期轴）权威迁移语义与 layer-2 四方对照裁定登记（以代码为权威；dict `wf/approve-status` + `erp/doc-status`）。
>
> 实体级状态机 Bean（6 个，双轴各自独立 Bean，契约 §1/§3 双轴分离 + `Approval`/`Document` 后缀命名）：
> - `ErpAstDisposalApprovalStateMachine`（M4.45）+ `ErpAstDisposalDocumentStateMachine`（M4.44）
> - `ErpAstAssetCapitalizationApprovalStateMachine`（M4.47）+ `ErpAstAssetCapitalizationDocumentStateMachine`（M4.46）
> - `ErpAstValueAdjustmentApprovalStateMachine`（M4.43）+ `ErpAstValueAdjustmentDocumentStateMachine`（M4.42）
>
> 接线范式 = 1950-1 采购 facade 先例（assets 版）：facade `validateTransitionForXxx` 改调 Bean `assertCanXxx`（try/catch common 码作 cause → 领域码 `ERR_*_ILLEGAL_{STATUS,DOC}_TRANSITION`，契约 §7）；`executeApprove`/`executeReverseApprove` 目标态改调 Bean `*TargetStatus()`；per-mutation Processor 经 facade 透传自动生效。**动态业务守卫与副作用保留原位**（Asset 来源态校验、gain/loss 计算、schedule cancel/restore、过账、posted 置位、折旧计划生成）。Asset.status side-effect（→IN_SERVICE/SCRAPPED/SOLD/DRAFT）由计划 1 M4.40 `ErpAstAssetStateMachine` 守卫——两计划在 `asset.setStatus(...)` 行交汇，接线互不冲突。
>
> **注（层 2 四方对照漂移登记，plan 1931-2 结束审计 MINOR M1）**：「资本化库存转固 stock move」（owner doc §7 外部依赖行 `IErpInvStockMoveBiz`）在 `module-assets` 全仓代码中**零引用**（grep 实证，本计划未删任何代码）——为**继承性 owner-doc 漂移**（资本化 Processor 实际无库存出库调用），本计划不传播此声明。Successor：库存转固业务上线时补实现 + 补 owner doc 对齐。

### 1. approveStatus 审批轴（三实体同构，dict `wf/approve-status`）

三实体 approveStatus 均为 4 态 + 5 命名动作 6 条边（与 Movement/采购同构）：

| approveStatus | 业务含义 | 可达性 |
|---------------|----------|--------|
| 未提交（UNSUBMITTED） | 单据新建，等待提交审批 | 初始态（CRUD 创建写入）；withdrawApproval 可回到此态 |
| 已提交（SUBMITTED） | 已提交待审核 | submitForApproval 从 UNSUBMITTED/REJECTED 进入 |
| 已审核（APPROVED） | 审核通过（业务终态，可逆） | approve 从 SUBMITTED 进入；触发业财过账 + 资产联动 |
| 已驳回（REJECTED） | 审核驳回 | reject 从 SUBMITTED 进入；reverseApprove 从 APPROVED 进入 |

| 动作 | 来源态 | 目标态 |
|------|--------|--------|
| submitForApproval | UNSUBMITTED / null / REJECTED | SUBMITTED |
| approve | SUBMITTED | APPROVED |
| reject | SUBMITTED | REJECTED |
| reverseApprove | APPROVED | REJECTED |
| withdrawApproval | SUBMITTED | UNSUBMITTED |

> **reverseApprove 目标态裁定（plan Phase 1 Decision (A)）**：reverseApprove→REJECTED（非 SUBMITTED），对齐 `domain-design-guidelines.md §16.4` + assets 域 R1.x 既有行为（三 facade `executeReverseApprove` 现状均写 REJECTED，实仓核实）。Bean `reverseApproveTargetStatus()`=REJECTED。
>
> **ValueAdjustment 动态守卫（不迁移，plan Phase 3 Decision）**：`ERR_ADJUSTMENT_ALREADY_REVERSED`（已红冲不可二次红冲）、`ERR_ADJUSTMENT_APPROVAL_REQUIRED`（强制审批配置 config-gated）、`ERR_ADJUSTMENT_TYPE_INVALID`/`ERR_ADJUSTMENT_AMOUNT_INVALID`（调整类型/金额业务校验）为 posted/docStatus/config/业务值**动态守卫**，非固定状态迁移边，保留原位（Bean 只接管固定 approveStatus 5 动作矩阵 + docStatus 轴）。

### 2. docStatus 业务生命周期轴（三实体分化，dict `erp/doc-status`）

dict 3 值（DRAFT/ACTIVE/CANCELLED）。三实体 docStatus writer 分化如下（layer-2 四方对照以代码为权威）：

| 实体 | 命名动作 writer | transitions() 边 | 终态集合 | 特例登记 |
|------|-----------------|------------------|----------|----------|
| **Disposal** | approve→ACTIVE（`executeApprove:93`）唯一命名 writer | `approve(DRAFT→ACTIVE)` 1 边 | {ACTIVE, CANCELLED} | CANCELLED 经 `useLogicalDelete` 可达（实体实仓核实）；reverseApprove **不写** docStatus（保持 ACTIVE） |
| **Capitalization** | approve→ACTIVE（`executeApprove:86`）+ reverseApprove→CANCELLED（`executeReverseApprove:122`） | `approve(DRAFT→ACTIVE)` + `reverseApprove(ACTIVE→CANCELLED)` 2 边 | {CANCELLED} | **特例边**：reverseApprove 额外写 docStatus=CANCELLED（Disposal/ValueAdjustment 无此 docStatus 写——不与 Disposal Document Bean 同构，draft review M2 登记）；ACTIVE 为「可逆中间态」（经 reverseApprove 有出边，不适用「终态无出边」强断言） |
| **ValueAdjustment** | approve→ACTIVE（`executeApprove:68` + `doAutoApprove:270` 双 writer）+ cancel→CANCELLED（`ErpAstValueAdjustmentCancelProcessor:26`，唯一有独立 cancel mutation 的实体） | `approve(DRAFT→ACTIVE)` + `cancel(DRAFT→CANCELLED)` 2 边 | {ACTIVE, CANCELLED} | cancel 守卫动态部分（posted=true 拒绝）保留原位（`validateTransitionForCancel` posted 守卫）；ACTIVE 禁 cancel（「非已生效」领域码，Bean `assertCanCancel` 承载） |

三实体 docStatus 初始态均为 DRAFT（CRUD 创建路径写入，§9.1 排除矩阵运行时强制）。

### 3. 终态与恢复

- **approveStatus 终态**：APPROVED 为业务终态（「可逆终态」——经 reverseApprove 有出边）。REJECTED 非终态（经 submitForApproval 可重新提交）。
- **docStatus 终态**：Disposal/ValueAdjustment {ACTIVE, CANCELLED}；Capitalization {CANCELLED}（ACTIVE 为可逆中间态）。
- **资产终态联动（非本计划接管）**：Disposal approve→Asset SCRAPPED/SOLD（M4.40 AssetStateMachine `disposeScrapTargetStatus`/`disposeSellTargetStatus`）；Capitalization approve→Asset IN_SERVICE（`capitalizeTargetStatus`）+ reverseApprove posted=true 窗口→DRAFT（`reverseCapitalizeTargetStatus`）；ValueAdjustment 不改 Asset.status。

### 4. 异常路径与 posted=false 不对称窗口

| 异常场景 | 处理 |
|----------|------|
| 非来源态执行审批动作 | Bean `assertCanXxx` 报告 common 层非法迁移码（契约 §7），facade 映射领域码 `ERR_*_ILLEGAL_STATUS_TRANSITION`（common 作 cause；错误码值/参数对外不变） |
| CANCELLED 单据执行审批动作 | doc-cancelled 守卫（facade `validateTransitionForCancel`/`validateNotCancelled` 委托 Document Bean `isCancelled()`）阻断，抛 `ERR_*_ILLEGAL_DOC_TRANSITION` |
| **reverseApprove posted=false 不对称窗口** | 三实体 `executeReverseApprove` 仅 posted=true 时回滚资产行为 + 红冲凭证 + schedule cancel/restore；posted=false 窗口仅设 approveStatus=REJECTED（Capitalization 额外 docStatus=CANCELLED），资产保持终态。deliberate 不对称（owner doc §4 实现约定），悬挂经 `DeferredPostingSweepJob` 兜底重试 + `IErpSysNotificationBiz` 告警——本计划不改此行为 |
| ValueAdjustment 已红冲二次红冲 / 强制审批配置 / 类型金额非法 | 动态守卫保留原位（§1 动态守卫登记） |

### 5. 角色与权限

approve/reverseApprove 权限声明沿用各实体 xbiz 既有 `<auth permissions>`（本计划零改动，仅迁移固定状态判断）。

## 适用对象四：资产拆分/合并文档双轴（Split / Merge）

> 本节由 plan `2026-08-14-1931-3`（M4.48 docStatus + M4.49 approveStatus Split、M4.50 docStatus + M4.51 approveStatus Merge）补章节落地——owner doc 原仅在「实现模式与守卫边界」散文段提及拆分/合并 PROC 路径，无矩阵化 §适用对象章节。本节集中建立 `ErpAstSplit` / `ErpAstMerge` 双轴（`approveStatus` 审批轴 + `docStatus` 业务生命周期轴）权威迁移语义与 layer-2 四方对照裁定登记（以代码为权威；dict `wf/approve-status` + `erp/doc-status`；业务语义见 `split-merge.md`）。
>
> 实体级状态机 Bean（4 个，双轴各自独立 Bean，契约 §1/§3 双轴分离 + `Approval`/`Document` 后缀命名）：
> - `ErpAstSplitApprovalStateMachine`（M4.49）+ `ErpAstSplitDocumentStateMachine`（M4.48）
> - `ErpAstMergeApprovalStateMachine`（M4.51）+ `ErpAstMergeDocumentStateMachine`（M4.50）
>
> 接线范式 = 1931-2 Disposal facade 先例（同域文档双轴直接范本）：facade `validateTransitionForXxx` 改调 Bean `assertCanXxx`（try/catch common 码作 cause → 领域码 `ERR_AST_{SPLIT,MERGE}_ILLEGAL_{STATUS,DOC}_TRANSITION`，契约 §7）；`executeApprove` 目标态改调 Bean `*TargetStatus()`；per-mutation 6 Processor 经 facade 透传自动生效。**动态业务守卫与副作用保留原位**（比例/金额平衡、跨类别/币种、源 IN_SERVICE、净值充足、目标编码唯一、已过账守卫、资产卡片结构性重组、`AssetSplit/MergePostingDispatcher.tryPost`、posted 置位）。
>
> **不可逆契约（关键差异 vs 适用对象三，owner doc `split-merge.md` §关键业务规则 5）**：Split/Merge approve 触发**资产卡片结构性重组**（不可物理回退），`AssetSplit/MergePostingDispatcher` **仅 post 路径、无 reverse**。reverseApprove Mutation 存在但**无条件抛错**——per-mutation `ErpAst{Split,Merge}ReverseApproveProcessor` 在 `requireXxx` 后直接 `throw ERR_AST_{SPLIT,MERGE}_REVERSE_NOT_SUPPORTED`（无 posted 判定、无 executeReverseApprove 方法体、短路在 facade validateTransition 之前）。与适用对象三（reverseApprove 有真实 posted=true 红冲 + posted=false 不对称窗口）形成对比——Split/Merge 的 reverseApprove 连 posted=false 窗口都没有。错误更正路径 = 资产处置 + 新建流程。**Bean reverseApprove(APPROVED→REJECTED) 边为名义边（nominal edge，运行时不可达）**——仅供矩阵完备性/可达性元数据（M5.1）+ §16.4 约定对齐消费，`assertCanReverseApprove` 存在但**不被接线**。

### 1. approveStatus 审批轴（二实体同构，dict `wf/approve-status`）

二实体 approveStatus 均为 4 态 + 5 命名动作 6 条边（与 Movement/采购/适用对象三同构）：

| approveStatus | 业务含义 | 可达性 |
|---------------|----------|--------|
| 未提交（UNSUBMITTED） | 单据新建，等待提交审批 | 初始态（CRUD 创建写入）；withdrawApproval 可回到此态 |
| 已提交（SUBMITTED） | 已提交待审核 | submitForApproval 从 UNSUBMITTED/REJECTED 进入 |
| 已审核（APPROVED） | 审核通过（业务终态，可逆） | approve 从 SUBMITTED 进入；触发结构性资产过账 + 卡片重组 |
| 已驳回（REJECTED） | 审核驳回 | reject 从 SUBMITTED 进入；reverseApprove 名义边从 APPROVED 进入（运行时不可达） |

| 动作 | 来源态 | 目标态 |
|------|--------|--------|
| submitForApproval | UNSUBMITTED / null / REJECTED | SUBMITTED |
| approve | SUBMITTED | APPROVED |
| reject | SUBMITTED | REJECTED |
| reverseApprove | APPROVED | REJECTED（**名义边，运行时不可达**——无条件抛 `ERR_AST_{SPLIT,MERGE}_REVERSE_NOT_SUPPORTED`） |
| withdrawApproval | SUBMITTED | UNSUBMITTED |

> **reverseApprove 名义边裁定（plan Phase 1 Decision (A)）**：目标态=REJECTED（对齐 `domain-design-guidelines.md §16.4`），但 **Split/Merge reverseApprove 是无条件抛错动作**（per-mutation Processor require 后直接抛，无 posted 判定、短路在 facade `validateTransitionForReverseApprove` 之前），运行时从不产生状态迁移。Bean 将 reverseApprove APPROVED→REJECTED 声明为**名义边**（javadoc 显式标注「运行时不可达」），`assertCanReverseApprove` 存在但**不被接线**；层 1 矩阵测试仅断言该边在 `transitions()` 元数据中存在（元数据完备性），运行时不可达由层 3 既有集成测试断言（`TestErpAstSplitMerge.testApproveThenReverseNotSupported` 等）。

### 2. docStatus 业务生命周期轴（二实体同构，dict `erp/doc-status`）

dict 3 值（DRAFT/ACTIVE/CANCELLED）。二实体 docStatus writer 分化如下（layer-2 四方对照以代码为权威）：

| 实体 | 命名动作 writer | transitions() 边 | 终态集合 | 特例登记 |
|------|-----------------|------------------|----------|----------|
| **Split** | approve→ACTIVE（`executeApprove:117`）唯一命名 writer + cancel→CANCELLED（`ErpAstSplitCancelProcessor:26`，独立 cancel mutation） | `approve(DRAFT→ACTIVE)` 1 边 | {ACTIVE, CANCELLED} | cancel 守卫动态条件（ACTIVE「非已生效」+ posted「非已过账」）保留原位，仅 CANCELLED 判定委托 Document Bean `isCancelled()`（Phase 1 Decision (C)）；reverseApprove 不写 docStatus（无条件抛错） |
| **Merge** | approve→ACTIVE（`executeApprove:120`）唯一命名 writer + cancel→CANCELLED（`ErpAstMergeCancelProcessor:26`，独立 cancel mutation） | `approve(DRAFT→ACTIVE)` 1 边 | {ACTIVE, CANCELLED} | 同上（Phase 2 镜像） |

二实体 docStatus 初始态均为 DRAFT（CRUD 创建路径写入，§9.1 排除矩阵运行时强制）。

### 3. 终态与恢复

- **approveStatus 终态**：APPROVED 为业务终态（「可逆终态」——经 reverseApprove 有出边（名义），运行时该边不可达）。REJECTED 非终态（经 submitForApproval 可重新提交）。
- **docStatus 终态**：{ACTIVE, CANCELLED}。
- **资产终态联动（非本计划接管）**：Split/Merge approve→源资产 Asset.status=DISPOSED（内部结构重组无损终态，`split-merge.md` 实现注记 Decision 1）+ 新卡片 IN_SERVICE；**不可逆**——无 reverse 路径恢复源资产。

### 4. 异常路径与不可逆契约

| 异常场景 | 处理 |
|----------|------|
| 非来源态执行审批动作 | Bean `assertCanXxx` 报告 common 层非法迁移码（契约 §7），facade 映射领域码 `ERR_AST_{SPLIT,MERGE}_ILLEGAL_STATUS_TRANSITION`（common 作 cause；错误码值/参数对外不变） |
| CANCELLED 单据执行审批动作 | doc-cancelled 守卫（facade `validateTransitionForCancel` 委托 Document Bean `isCancelled()`，ACTIVE/posted 动态条件保留原位）阻断，抛 `ERR_AST_{SPLIT,MERGE}_ILLEGAL_DOC_TRANSITION` |
| **reverseApprove（不可逆契约）** | per-mutation `ErpAst{Split,Merge}ReverseApproveProcessor` require 后**无条件抛 `ERR_AST_{SPLIT,MERGE}_REVERSE_NOT_SUPPORTED`**（无 posted 判定、无窗口期、短路在 facade validateTransition 之前）。错误更正走资产处置 + 新建流程（owner doc `split-merge.md` §关键业务规则 5） |
| 比例/金额不平衡、跨类别/币种、源非 IN_SERVICE、净值不足、目标编码重复、无行/无源、已过账 | 动态业务守卫保留原位（`ERR_AST_{SPLIT,MERGE}_*`，非固定状态迁移边） |

### 5. 角色与权限

approve/reverseApprove 权限声明沿用各实体 xbiz 既有 `<auth permissions>`（本计划零改动，仅迁移固定状态判断）。

## 实现模式与守卫边界

> 资产域移动单（ErpAstMovement）审批轴动作的实现模式补注。完整双轴矩阵见 §适用对象二。

**PROC 路径**（资本化/处置/拆分/合并/价值调整 等 `ErpAst*Processor`）：含完整业务守卫 + 过账联动。

**INLINE→Bean 路径**（Movement 全 5 动作：submitForApproval/approve/reject/reverseApprove/withdrawApproval）：原直接在 xbiz `<source>` 脚本中实现，现**已完成 INLINE→Bean 迁移**（plan `2026-08-13-0805-2`）——xbiz source 经 `inject` 取得 `ErpAstMovementApprovalStateMachine` + `ErpAstMovementDocumentStateMachine` 双 Bean，固定守卫/目标态委托 Bean，动态守卫（isCancelled 防御 + approvedBy/At 审计）保留原位。Movement 无独立 cancel mutation，docStatus=CANCELLED 经 `useLogicalDelete` 承载，isCancelled 守卫为防御性（阻止逻辑删除单据的 approveStatus 副轴漂移）。

### IDLE 暂停/恢复：✅ 已实现（RC-R1.54）

资产卡片 `IN_SERVICE↔IDLE` 暂停/恢复迁移已落地：`suspend`/`resume` BizMutation（`ErpAstAssetSuspendResumeProcessor`）+ `ErpAstAssetStateMachine` Bean 扩展（suspend/resume 守卫与目标态 + `assertCanDispose` 接受 IDLE 对齐 §2「IN_SERVICE/IDLE → SCRAPPED/SOLD」）。「IDLE 停提折旧」由引擎仅查询 `IN_SERVICE` + `validateAssetInService` 拒绝 IDLE 天然满足（owner doc §1 设计意图），PENDING 计划 suspend 期间保留（Decision A），暂停时点经 remark「闲置自 {date}」强制记录。闲置超期 cron → successor（见 §8 补注）。

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
- 闲置资产的折旧停提/恢复配置是否明确。（✅ IDLE 已实现（RC-R1.54）——`suspend`/`resume` BizMutation + 引擎 IN_SERVICE-only 天然停提 + PENDING 保留；闲置超期 cron = successor，审查时应确认文档未声称 cron 已就绪。）
- 终态（报废/出售）是否真正无出边，处置错误的纠正路径是否清晰。
- 折旧漏提补提路径是否覆盖（反结账 vs 当期补提）。
- 资本化入账与库存出库的协作（库存转固场景）。
- 资产移动单（适用对象二）：approveStatus 5 动作矩阵 + docStatus 退化轴（ACTIVE 死状态 reserved，CANCELLED 经 useLogicalDelete）；reverseApprove→REJECTED 目标态（§16.4）；INLINE→Bean 迁移（xbiz source inject 双 Bean）。
- 资产拆分/合并（适用对象四）：二实体 approveStatus 5 动作矩阵 + docStatus 1 边（approve→ACTIVE）+ cancel mutation（CANCELLED）；**不可逆契约**——reverseApprove 无条件抛 `ERR_AST_{SPLIT,MERGE}_REVERSE_NOT_SUPPORTED`，Bean reverseApprove 边为名义边（运行时不可达）；approve 触发结构性资产过账 + 卡片重组（`AssetSplit/MergePostingDispatcher` 仅 post 无 reverse）。

## 已知限制：浏览器层 xwf 审批路径（ErpAstDisposal）

> M-2 补充。

资产处置单 **ErpAstDisposal** 的 `useWorkflow="true"` xwf 审批轴在浏览器层 E2E 不可达：

- 根因：nop-wf `WorkflowEngineImpl.newSteps` 在浏览器层 `submitForApproval` 时 fallback `sysUser(0)` 作 step owner，但 `NopAuthUser.userId` 因 `tagSet="seq"` 覆盖显式 "0" 为 UUID，致 `allowCallByUser:1053` 拒绝。
- **替代路径**：处置单的 DIRECT 三轴审批（`approveStatus` DIRECT，`docs/plans/2026-07-05-0540-3` 范式）不依赖 xwf，浏览器层 E2E 可达。
- **影响范围**：本状态机的 `IN_SERVICE → SCRAPPED` / `IN_SERVICE → SOLD` 迁移由 DIRECT 审批驱动的过账触发，浏览器层可达；多级审批链业务场景仅在浏览器层外覆盖。
- **解除条件**：见 `docs/design/roles-and-permissions.md §浏览器层审批路径已知限制`。
