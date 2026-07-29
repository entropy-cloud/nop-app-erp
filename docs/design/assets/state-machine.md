# 固定资产域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 资产域有两类状态对象：**资产卡片**（生命周期状态机）与**折旧计划条目**（简单执行状态）。本文件主要覆盖资产卡片状态机，折旧计划条目状态较简单（见末节）。

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
| IN_SERVICE → IDLE ⚠ **Deferred（audit P1-MA2-061）** | 资产管理员 | 使用中状态 | **本期未实现**（无 suspend/toIdle BizMutation writer）。目标语义：可配置是否停提折旧（默认停）。**Successor**：资产暂停/恢复业务上线时实现 |
| IDLE → IN_SERVICE ⚠ **Deferred（audit P1-MA2-061）** | 资产管理员 | 闲置状态 | **本期未实现**（无 resume/fromIdle BizMutation writer）。目标语义：恢复折旧计提。**Successor**：同上 |
| IN_SERVICE/IDLE → SCRAPPED | 资产管理员/管理员 | 资产存在、未处置 | 触发报废清理凭证（结转原值/累计折旧/清理损失） |
| IN_SERVICE/IDLE → SOLD | 资产管理员/管理员 | 资产存在、未处置、出售金额已定 | 触发出售清理凭证（结转原值/累计折旧/出售收入/清理损益） |

> **实现状态（Deferred，audit P1-MA2-061）**：资产卡片 `IN_SERVICE↔IDLE` 暂停/恢复迁移为**预留死状态**——`ErpAstConstants.ASSET_STATUS_IDLE = "IDLE"` 常量与 `erp-ast/asset-status` dict 值保留，但本期全 `module-assets` **零 `setStatus(...IDLE)` / `setAssetStatus(ASSET_STATUS_IDLE)` writer**，`ErpAstAssetBizModel` 为 CrudBizModel 桩（17 行，零状态机 mutation），亦无 suspend/resume/toIdle/fromIdle BizMutation。IDLE 当前仅出现在 3 处只读守卫（`ErpAstValueAdjustmentProcessor:204` + `ErpAstDisposalProcessor:200` 读取排除 + `ErpAstInventoryProcessor:193` 盘点范围过滤）。**折旧默认停提语义已等价满足**：折旧引擎 `ErpAstDepreciationScheduleProcessor` 与 KPI（`ErpAstDashboardBizModel:179`）仅查询 `IN_SERVICE`，等价于 owner doc §1「IDLE 默认停提」设计意图，无悬挂数据。IN_SERVICE 折旧主路径（资本化建卡→IN_SERVICE→期末批量折旧→处置终态）完整不受影响。处置：采纳 Decision Deferred（保留 dict 值为预留语义入口 + owner doc 标注，对齐 finance R1.13 / mfg R1.14 / hr R1.15「保留为预留 + 文档 Deferred」裁决先例），不从 ORM 删除。**Successor 触发条件**：PM 要求正式资产闲置/恢复工作流时实现 `suspend`/`resume` BizMutation（IN_SERVICE↔IDLE 迁移 + setStatus writer）+ 折旧引擎扩展查询 IN_SERVICE+IDLE + 闲置超期 TODO cron（经 `IErpSysNotificationBiz`）。

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

### 实现偏离补注：reverseApprove posted=false 窗口不对称（P1-MA2-060）

资本化/处置 `reverseApprove` 仅在 `posted=true` 时回滚资产行为（资本化：资产→DRAFT + cancelSchedules；处置：资产→IN_SERVICE + restoreSchedules）。**posted=false 窗口期** reverseApprove 仅设业务单据 REJECTED，资产保持终态（IN_SERVICE/SCRAPPED/SOLD）+ schedules 保持 PENDING/CANCELLED。这是 deliberate 不对称（plan `2026-07-30-0341-2` Phase 3 方案 B 裁决）：posted=false 悬挂经 `DeferredPostingSweepJob`（Cap/Disposal）兜底重试 + `IErpSysNotificationBiz` 告警闭环；运营在悬挂窗口期需先触发 sweep 重试或手工 reverse 凭证再 reverseApprove。错误传播分级见 `posting-log.md §错误传播分级策略` G2/G4。

## 5. 可达性

> **⚠ 本期可达性修正（Deferred，audit P1-MA2-061）**：下方为**目标状态机的可达性**。本期 IDLE 因零 writer **不可达**——`IN_SERVICE→IDLE` / `IDLE→IN_SERVICE` 迁移未实现（见 §2 Deferred 补注）。IDLE 本期保留为预留语义入口（不删除 dict 值）。本期实际可达集为 `DRAFT → IN_SERVICE → {SCRAPPED, SOLD}`；IDLE 及其出边为 successor。

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
| IDLE ⚠ **Deferred（audit P1-MA2-061）** | 是（目标） | monitor（监控）—— 闲置资产待决策（恢复/处置）。⚠ **本期 IDLE 不可达**（无 writer，见 §2/§5 Deferred 补注），故本期不产生 IDLE TODO |
| SCRAPPED/SOLD | 否 | — |

**待处置资产提醒**：闲置超期的资产可产生 TODO 提醒决策（恢复使用或处置），避免资产长期闲置无人处理。

> **⚠ 闲置超期 TODO cron Deferred（audit P1-MA2-061）**：本提醒依赖 IDLE 状态写入 + 闲置时长扫描 cron，两者本期均未实现（IDLE 零 writer，见 §2 Deferred 补注）。处置：Deferred。**Successor 触发条件**：PM 要求正式资产闲置/恢复工作流时，随 `suspend`/`resume` BizMutation 一并实现闲置超期 TODO 经 `IErpSysNotificationBiz` 派发。

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

## 实现模式与守卫边界

> 计划 `2026-07-30-0341-3-r1-17`（P1-MA2-058/059）补注：资产域移动单（ErpAstMovement）审批轴动作的实现模式。

**PROC 路径**（资本化/处置/拆分/合并/价值调整 等 `ErpAst*Processor`）：含完整业务守卫 + 过账联动。

**INLINE 路径**（Movement 全 5 动作：submitForApproval/approve/reject/reverseApprove/withdrawApproval）：直接在 xbiz `<source>` 脚本中实现，守卫边界为 **isCancelled + src 状态校验**（`entity.docStatus === 'CANCELLED'` 阻断 + `approveStatus` 源态校验）。Movement 无独立 cancel mutation，docStatus=CANCELLED 经 `useLogicalDelete` 承载，isCancelled 守卫为防御性（阻止逻辑删除单据的 approveStatus 副轴漂移）。

### IDLE 暂停/恢复：本期 Deferred（audit P1-MA2-061）

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
- 闲置资产的折旧停提/恢复配置是否明确。（⚠ audit P1-MA2-061：IDLE 本期 Deferred——`IN_SERVICE↔IDLE` 迁移未实现、零 writer；折旧引擎仅查 `IN_SERVICE` 等价于「IDLE 默认停提」。审查时应确认文档未声称已实现 IDLE 暂停/恢复，successor = 资产闲置工作流上线时。）
- 终态（报废/出售）是否真正无出边，处置错误的纠正路径是否清晰。
- 折旧漏提补提路径是否覆盖（反结账 vs 当期补提）。
- 资本化入账与库存出库的协作（库存转固场景）。

## 已知限制：浏览器层 xwf 审批路径（ErpAstDisposal）

> M-2（plan `2026-07-20-2200-1`）补充；权威裁决见 plan `2026-07-09-2330-1`。

资产处置单 **ErpAstDisposal** 的 `useWorkflow="true"` xwf 审批轴在浏览器层 E2E 不可达：

- 根因：nop-wf `WorkflowEngineImpl.newSteps` 在浏览器层 `submitForApproval` 时 fallback `sysUser(0)` 作 step owner，但 `NopAuthUser.userId` 因 `tagSet="seq"` 覆盖显式 "0" 为 UUID，致 `allowCallByUser:1053` 拒绝。
- **替代路径**：处置单的 DIRECT 三轴审批（`approveStatus` DIRECT，`docs/plans/2026-07-05-0540-3` 范式）不依赖 xwf，浏览器层 E2E 可达。
- **影响范围**：本状态机的 `IN_SERVICE → SCRAPPED` / `IN_SERVICE → SOLD` 迁移由 DIRECT 审批驱动的过账触发，浏览器层可达；多级审批链业务场景仅在浏览器层外覆盖。
- **解除条件**：见 `docs/design/roles-and-permissions.md §浏览器层审批路径已知限制`。
