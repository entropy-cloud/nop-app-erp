# 2026-07-30-0512-1-r1-18-assets-idle-state-machine-deferred assets IDLE 状态机迁移未实现裁决

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.18（P1-MA2-061，源自 A2.10 assets 状态机审查）
> Related: `docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-061`；plan `2026-07-30-0143-3-r1-14-mfg-dict-dead-state-owner-doc-drift.md`、plan `2026-07-30-0341-1-r1-15-hr-state-machine-dict-dead-state.md`（同型裁决先例）、plan `2026-07-30-0341-3-r1-17-pur-sal-ast-reverse-approve-state-machine.md`（assets 域 Movement 守卫，已闭包）
> Audit: required

## Current Baseline

P1-MA2-061 单项 finding 经实仓逐项确认：assets 卡片状态机的「闲置（IDLE）」状态是**事实上的死状态**——dict `erp-ast/asset-status` 含 IDLE 项 + owner doc `state-machine.md §1/§2/§8` 声明 `IN_SERVICE↔IDLE` 迁移，但代码完全未实现。**不破坏主路径**（IN_SERVICE 折旧主路径完整：资本化建卡→IN_SERVICE→期末批量折旧→处置/拆分/合并终态转移）。

**实仓证据：**
- `ErpAstConstants.java:67` 定义 `ASSET_STATUS_IDLE = "IDLE"`；全 `module-assets` grep `setStatus(...IDLE)` / `setAssetStatus(ASSET_STATUS_IDLE)` = **零 writer**（IDLE 仅出现在 3 处只读守卫：`ErpAstValueAdjustmentProcessor:204` + `ErpAstDisposalProcessor:200` 读取排除 + `ErpAstInventoryProcessor:193` 盘点范围过滤）。
- `ErpAstAssetBizModel.java` = CRUD 桩（`extends CrudBizModel<ErpAstAsset>`，零状态机 mutation）；无 suspend/resume/setIdle/toIdle/fromIdle 方法（全模块 grep 零匹配）。
- 折旧批量 `ErpAstDepreciationScheduleProcessor` 仅查 IN_SERVICE（KPI `ErpAstDashboardBizModel:179` `eq("status", ASSET_STATUS_IN_SERVICE)`）——等价于「IDLE 默认停提折旧」业务语义，与 owner doc §1「闲置（IDLE）—可配（默认停提）」设计意图吻合。
- owner doc §2 迁移表声明 `IN_SERVICE→IDLE` / `IDLE→IN_SERVICE`；§8「闲置超期提醒」声明 Deferred。

**保护区域：** 不触及会计/数据删除保护区域（无凭证/折旧/删除写路径变更）。本计划为纯 owner-doc 行为契约对齐（方案B Deferred 标注），无代码/无 ORM 变更。

## Goals

- 消除 assets 域 owner doc 与代码间 IDLE 状态悬空：state-machine.md §1/§2/§5/§8 明确「IDLE 为预留状态，本期无 setStatus writer / 无 suspend/resume mutation / 无闲置超期 cron，dict 值保留为语义入口 successor」。
- owner doc 与代码零 writer 一致，无「dict 含值 + 文档声明迁移但代码无实现」的悬空。

## Non-Goals

- 不实现资产 suspend/resume BizMutation（IN_SERVICE↔IDLE 迁移）——裁决 Deferred，successor 命名触发条件。
- 不扩展折旧引擎查询 IN_SERVICE+IDLE（当前仅查 IN_SERVICE 已满足「IDLE 默认停提」语义；扩展留 successor）。
- 不实现闲置超期 TODO cron job（owner doc §8 已声明 Deferred）。
- 不从 ORM 删除 IDLE dict 值（采纳「保留为预留 + 文档 Deferred」对齐 R1.13/R1.14/R1.15 既有先例）。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐，纯文档）
- Owner Docs: `docs/design/assets/state-machine.md`
- Skill Selection Basis: 纯 owner doc Deferred 标注，无代码/ORM → `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：P1-MA2-061 处置方案裁决（同型裁决范式，对齐 R1.13/R1.14/R1.15 先例）。
      - IDLE 状态机迁移未实现：**Deferred（与 arm-index 推荐方向一致）**。**arm-index 推荐对齐声明**：arm-index §P1-MA2-061 方案A（推荐）即「owner doc Deferred 标注 + 维持 dict IDLE 项」——本计划采纳该推荐方向（不偏离）。理由：(1) IDLE 是低频资产管理场景（设备停用/恢复），IN_SERVICE 折旧主路径完整不受影响；(2) 折旧引擎仅查 IN_SERVICE 等价于 owner doc §1「IDLE 默认停提」设计意图，无悬挂数据；(3) arm-index 方案B（实现 suspend/resume mutation + 折旧引擎扩展查 IN_SERVICE+IDLE + 闲置超期 cron）触及折旧计提业务逻辑且工作量大，与危害不成比例；(4) 与 R1.13/R1.14/R1.15「保留 dict 死状态为预留 + owner doc Deferred 标注」先例一致。残留风险：PM 若要求正式资产闲置工作流时需补实现 → successor 命名触发条件。dict IDLE 项保留为预留语义入口（不删除）。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 记录选择 + 理由 + 残留风险 + successor 触发条件，Phase 2 严格遵循。

### Phase 2 - assets IDLE owner doc Deferred 标注（P1-MA2-061）

Status: completed
Targets: `docs/design/assets/state-machine.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] state-machine.md 标注 IDLE Deferred：§1 状态定义表 IDLE 行补「（预留状态，本期无 writer / 无迁移实现）」；§2 迁移完整性 `IN_SERVICE→IDLE` / `IDLE→IN_SERVICE` 标注「Deferred——资产暂停/恢复业务上线时实现 suspend/resume BizMutation」；§5 可达性补注「IDLE 本期不可达（无 writer），保留为预留」；§8 TODO 策略补注「闲置超期 cron Deferred」。每处命名 successor 触发条件（PM 要求正式资产闲置工作流时）。
- [x] 核对 §实现模式节 + §审查提示「闲置资产的折旧停提/恢复配置是否明确」措辞对齐「IDLE Deferred——折旧引擎仅查 IN_SERVICE 等价于默认停提」。
      - Skill: `none`

Exit Criteria:

- [x] state-machine.md 明确 IDLE 为预留/Deferred，owner doc 与代码零 writer 一致；successor 触发事件已命名。

## Draft Review Record

- Independent draft review iteration 1: accept (ses_05030be58ffeFgem8okz3j5sGt) because 单项 finding（P1-MA2-061）基线实仓验证 TRUE（ASSET_STATUS_IDLE 零 writer / ErpAstAssetBizModel CRUD 桩 / 折旧引擎仅查 IN_SERVICE），Deferred 裁决与 arm-index §P1-MA2-061 推荐方向一致（无偏离），单一结果表面（assets owner-doc 契约对齐，规则 4/14），mvn 门控因纯文档正确删除（执行时规则 7 例外），无禁用词，successor 已命名。非阻塞：补「arm-index 推荐对齐声明」消除方案A/B 标签歧义（已修订）。

## Closure Gates

> 本计划无代码/ORM 变更（纯 owner doc Deferred 标注），故删除 `mvn` 构建验证门控（见执行时规则 7 例外）。验证聚焦 owner doc 与代码一致性。

- [x] 范围内文档对齐完成（P1-MA2-061 裁决落地为 owner doc Deferred 标注）
- [x] 相关文档对齐（assets/state-machine.md）
- [x] 已运行验证（grep 确认 IDLE 零 writer 基线不变 + owner doc Deferred 标注落地；compliance checker 本计划零新增命中）
- [x] 无范围内项目降级为 deferred/follow-up（方案B Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 资产 suspend/resume BizMutation + 折旧引擎扩展 + 闲置超期 cron（P1-MA2-061 方案A successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: IDLE 为零 writer 死状态；折旧引擎仅查 IN_SERVICE 等价于「IDLE 默认停提」语义；IN_SERVICE 折旧主路径完整。方案A 触及折旧计提业务逻辑且工作量大。
- Successor Required: `yes`（PM 要求正式资产闲置/恢复工作流时实现 suspend/resume BizMutation [IN_SERVICE↔IDLE 迁移] + 折旧引擎扩展查询 IN_SERVICE+IDLE + 闲置超期 TODO 经 `IErpSysNotificationBiz`）

## Closure

Status Note: 全部 2 个 Phase 执行完成，单项 finding P1-MA2-061 裁决落地（方案 Deferred：IDLE 状态机迁移为预留死状态，dict 值保留为语义入口，owner doc state-machine.md Deferred 标注覆盖 §1/§2/§5/§8 + §实现模式 + §审查提示，successor 命名触发条件）。纯文档变更（无代码/无 ORM），mvn 门控按规则 7 例外删除；验证聚焦 grep 确认零 writer 基线不变 + owner doc Deferred 落地 + compliance checker 零新增命中。独立结束审计 PASS。

Closure Audit Evidence:

- 独立结束审计（ses_05026887effe2xh4KPe9r17nDY，新会话，read-only）：**VERDICT: PASS**，无阻塞缺陷。逐项验证：
  - 基线事实 TRUE：`ErpAstConstants.java:67` ASSET_STATUS_IDLE="IDLE"；全 `module-assets` grep `set(Asset)?Status(...IDLE...)` = 零 writer；`ErpAstAssetBizModel.java` 17 行 CrudBizModel 桩（无 suspend/resume/toIdle/fromIdle mutation）；IDLE 仅出现在 3 处只读守卫（`ErpAstValueAdjustmentProcessor:204` + `ErpAstDisposalProcessor:200` + `ErpAstInventoryProcessor:193`）+ 1 测试读；折旧引擎 `ErpAstDepreciationScheduleProcessor:138` + KPI `ErpAstDashboardBizModel:179` 均仅查 `ASSET_STATUS_IN_SERVICE`。
  - owner doc Deferred 标注全部落地（state-machine.md §1 line 19 / §2 diagram lines 30-31 + table lines 41-42 + callout line 46 / §5 line 74 / §8 lines 109,114 / §实现模式 lines 157-159 / §审查提示 line 181），每处命名 successor。
  - 无范围蔓延：`git status` 仅 `docs/design/assets/state-machine.md`（+17/-7）+ 本计划文件变更，无代码/ORM/会计写路径触及。
  - successor 已命名（Deferred But Adjudicated：PM 要求正式资产闲置/恢复工作流时实现 suspend/resume BizMutation + 折旧引擎扩展查 IN_SERVICE+IDLE + 闲置超期 TODO cron）。
- 验证执行：grep 确认零 IDLE writer（退出码 1）；`bash docs/audits/nop-compliance-checker.sh` 本计划零新增命中（R1a/b/c/R4/R5/R7/R11=0；R1d=17/R8=42/R2a=38/R2c=1238 均为既有基线不变，与 R1.15 闭包基线一致）。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件（PM 要求正式资产闲置/恢复工作流时）。
