# 2026-07-30-0720-2-r1-25-aps-logistics-op-order-guards-cancel-approval-partial-receipt aps OperationOrder 状态守卫实现 + aps/logistics cancel 审批门控 + logistics 部分签收 Deferred

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.25（P1-MA2-077 + P1-MA2-078 + P1-MA2-079，源自 A2.15 aps+logistics 状态机审查）
> Related: `docs/audits/2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-077/078/079`；plan `2026-07-30-0341-3-r1-17-pur-sal-ast-reverse-approve-state-machine.md`（状态机守卫实现先例）、plan `2026-07-30-0631-2-r1-22-contract-negotiation-terminate-expiry-job-deferred.md`（missing-automation Deferred 先例）、plan `2026-07-30-0631-3-r1-23-b2b-edi-outbound-automation-deferred.md`（owner doc Deferred 标注先例）
> Audit: required

## Current Baseline

三项 finding 经实仓逐项确认：均为「owner doc 声明状态守卫/审批门控/部分签收但代码未实现」类型，**不破坏已实现主路径**（aps OperationOrder 5 态 DRAFT→PLANNED→IN_PROGRESS→FINISHED + PLANNED|IN_PROGRESS→CANCELLED + insertRushOrder 区间重排 + Schedule 3 态守卫齐全 + logistics Shipment 6 态 advise/completeShipment/cancelShipment/advanceTracking 全 src 守卫 + 网关重试 + deadLetter + path-1/path-2 config-gated）。

**P1-MA2-077（aps OperationOrder start/complete/cancel 完全缺状态守卫）— 确认：**
- `ErpApsOperationOrderBizModel.start:108-117` if/else 两分支逻辑完全相同（:110-114 死代码——`if status==PLANNED setStatus(IN_PROGRESS) else setStatus(IN_PROGRESS)`），**无任何前置 status 校验**；`complete:121-126` 直接 `setStatus(FINISHED)` 无校验；`cancel:129-135` 直接 `setStatus(CANCELLED)` 无校验。FINISHED→CANCELLED / FINISHED→IN_PROGRESS / CANCELLED→IN_PROGRESS 等非法迁移可达。
- owner doc `docs/design/aps/state-machine.md §2 L26-33` 迁移图「PLANNED→IN_PROGRESS→FINISHED 单向链 + PLANNED|IN_PROGRESS→CANCELLED 限定源态」+ L46「终态：FINISHED、CANCELLED」+ 迁移表 L39-41。
- `ErpApsErrors` 已有 `ERR_APS_SCHEDULE_ILLEGAL_STATUS` + `ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE`（含 ARG_CURRENT_STATUS）；需新增 OperationOrder 级非法迁移码。`ErpApsConstants` OP_STATUS_* 常量存在（PLANNED/IN_PROGRESS/FINISHED/CANCELLED）。

**P1-MA2-078（aps+logistics cancel 缺审批门控，合并）— 确认：**
- owner doc aps `state-machine.md §6 L77`「取消执行中的工序：需生产主管审批，因已产生实际报工数据」+ logistics `state-machine.md §4 L51`「IN_TRANSIT→CANCELLED | 发货员+审批」。
- 代码 `ErpApsOperationOrderBizModel.cancel:129-135` + `GatewayDispatcher.cancelShipment:131-156` **均无任何审批流或角色校验**——任意角色可执行危险操作。logistics 侧网关 cancel 已落实（DISPATCHED+ 经 `client.cancelShipment` 防止承运商侧双发，:147-151），仅缺内部审批流。
- 全域审批 SPI：reverse-approve 模式经 R1.17 落地（pur/sal/ast），但 cancel-approve 是新模式；A2.18 多公司隔离审查发现 19 模块 data-auth.xml 全 `<objs/>` 空规则 + 0 自定义 IDataAuthChecker——审批/角色基础设施全域不成熟。

**P1-MA2-079（logistics 部分签收完全未实现）— 确认：**
- owner doc logistics `state-machine.md §2 L40` ASCII 图「部分签收 → 记录部分签收，状态保持 IN_TRANSIT（等待剩余）」+ §4 L66「部分签收：记录签收明细，状态保持 IN_TRANSIT，等待剩余货物签收」。
- 代码 `GatewayDispatcher.advanceTracking:162-185` 仅处理完整 `TRACKING_EVENT_DELIVERED`（ErpLogConstants）+ TRACKING_EVENT_IN_TRANSIT/PICKED_UP，**无 TRACKING_EVENT_PARTIAL 常量 + 无部分签收字段（receivedQuantity/partialSignedQty）+ 无部分签收记录路径**。grep 全 `module-logistics/erp-log-service/src/main` `partial|Partial|PARTIAL|部分签收` 零业务命中。
- 完整签收主路径 DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED 完整覆盖；部分签收是 owner doc Deferred 业务场景（承运商回调暂只发完整 DELIVERED 事件）。实现部分签收须 ORM ask-first 加列（receivedQuantity 等）。

**保护区域：** 不触及会计/数据删除保护区域（无凭证/删除写路径变更）。077 涉及 BizModel 行为变更 + ErrorCode，按 roadmap 规则走标准 plan-audit + closure-audit（不触及 ORM ask-first）。078/079 为 owner doc Deferred 标注（纯文档）。

## Goals

- 消除 aps+logistics 域 owner doc 与代码间三项悬空：(1) **实现** aps OperationOrder start/complete/cancel 状态守卫（对齐 owner doc §2 迁移图）；(2) aps+logistics cancel 审批门控对齐（owner doc Deferred 标注，successor 命名触发条件）；(3) logistics 部分签收对齐（owner doc Deferred 标注，successor 命名触发条件）。
- owner doc 与代码一致；OperationOrder 终态不可恢复不变量经显式守卫落地。

## Non-Goals

- 不实现 aps+logistics cancel 审批工作流（P1-MA2-078）——裁决 Deferred（owner doc 正式化）。理由：(1) owner doc §6 声明的「生产主管/物流主管审批」是工作流审批（非纯角色检查），实现需审批 SPI + 角色-resource 种子，属 missing-feature；(2) 审计确认「操作本身业务正确」——cancel 操作不直接破坏库存/GL（aps 纯排产，副作用经下游 mfg 工单级联归 A2.6a；logistics 网关 cancel 已防承运商侧双发）；(3) A2.18 确认审批/角色基础设施全域不成熟（19 模块 data-auth.xml 全空）；(4) 与 R1.18/R1.22/R1.23 missing-automation Deferred 范式一致。successor：审批工作流 SPI 落地时实现 cancel-approve 动作 + config-gated 角色门控。
- 不实现 logistics 部分签收（P1-MA2-079）——裁决 Deferred（arm-index 推荐方向方案A）。理由：(1) 实现须 ORM ask-first 加列（receivedQuantity/partialSignedQty）+ TRACKING_EVENT_PARTIAL 常量 + 累计签收判定；(2) 承运商回调暂只发完整 DELIVERED 事件——部分签收是 owner doc Deferred 业务场景；(3) 完整签收主路径完整覆盖。successor：承运商支持部分签收回调时实现。
- 不改 GatewayDispatcher.cancelShipment 的网关取消逻辑（已落实防双发，仅缺内部审批流——归 078 Deferred）。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐）+ `implementation-only change`（077 BizModel 守卫 + ErrorCode）
- Owner Docs: `docs/design/aps/state-machine.md`、`docs/design/logistics/state-machine.md`
- Skill Selection Basis: P1-MA2-077 涉及 BizModel 方法行为变更 + 状态机守卫 + ErrorCode → `Skill: nop-backend-dev`；P1-MA2-078/079 owner doc Deferred 标注为纯文档 → 该部分 `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 三项 finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：三项 finding 处置方案逐项裁决（**选择性裁决**——077 实现状态守卫[对齐 R1.17 先例] + 078/079 owner doc Deferred[对齐 R1.22/R1.23 missing-automation 先例]）。
      - P1-MA2-077 aps OperationOrder 状态守卫缺失：**实现（arm-index 推荐方向方案A）**。理由：(1) owner doc §2 迁移图是强制契约（PLANNED→IN_PROGRESS→FINISHED 单向链 + PLANNED|IN_PROGRESS→CANCELLED 限定源态）；(2) containment 友好（start/complete/cancel 各加单点 status 守卫 + 1 ErrorCode，消除 if/else 死代码）；(3) 终态 FINISHED/CANCELLED 不可恢复不变量落地。残留风险：既有测试若依赖「任意态可 start/complete/cancel」须迁移（补前置态 seed）。
      - P1-MA2-078 aps+logistics cancel 审批门控缺失：**Deferred（owner doc 正式化，方案B）**。**与 arm-index 推荐偏差声明**：arm-index §P1-MA2-078 方案A（推荐）实现审批门控；本计划裁决 Deferred，理由：(1) owner doc §6 声明的工作流审批属 missing-feature（需审批 SPI + 角色-resource 种子）；(2) 审计确认 cancel 操作本身业务正确（不破坏库存/GL）；(3) A2.18 确认审批/角色基础设施全域不成熟；(4) 与 R1.18/R1.22/R1.23 missing-automation Deferred 范式一致。successor：审批工作流 SPI 落地时实现 cancel-approve 动作 + config-gated 角色门控。
      - P1-MA2-079 logistics 部分签收未实现：**Deferred（arm-index 推荐方向方案A）**。理由：(1) 实现须 ORM ask-first 加列 + 累计签收判定；(2) 承运商回调暂只发完整 DELIVERED 事件——属 owner doc Deferred 业务场景；(3) 完整签收主路径完整覆盖。successor：承运商支持部分签收回调时实现 TRACKING_EVENT_PARTIAL + receivedQuantity 字段 + 累计签收判定。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 逐项记录选择 + 理由 + 与 arm-index 推荐偏差声明 + successor 触发条件；077 进 Phase 2（实现），078/079 进 Phase 3（Deferred 标注）。

### Phase 2 - aps OperationOrder 状态守卫实现（P1-MA2-077）

Status: completed
Targets: `module-aps/erp-aps-service/.../entity/ErpApsOperationOrderBizModel.java`、`IErpApsOperationOrderBiz`、`ErpApsErrors.java`、`docs/design/aps/state-machine.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [x] **Add（ErrorCode）**：`ErpApsErrors` 增 `ERR_APS_OP_ILLEGAL_TRANSITION`（`erp.err.aps.op-illegal-transition`，描述「工序 {operationOrderCode} 非法状态迁移：当前={currentStatus}，期望={expectedStatus}」，复用 ARG_OP_CODE + ARG_CURRENT_STATUS + 新增 ARG_EXPECTED_STATUS）。
      - Skill: `nop-backend-dev`
- [x] **Fix（start 守卫）**：`ErpApsOperationOrderBizModel.start:108-117` 删除 if/else 死代码，改为守卫 `status==PLANNED`——非 PLANNED 抛 `ERR_APS_OP_ILLEGAL_TRANSITION`（expected=PLANNED）；通过后 `setStatus(IN_PROGRESS)`。**Containment 核实**：start/complete/cancel 均为手工 `@BizMutation` 入口点——grep 确认零内部/跨域调用方（mfg 仅为注释引用 IErpApsOperationOrderBiz；owner doc aps §7「WorkOrder 取消级联取消 OperationOrder」当前未落地代码实现），故新增守卫不会阻断既有级联路径。
      - Skill: `nop-backend-dev`
- [x] **Fix（complete 守卫）**：`complete:121-126` 增守卫 `status==IN_PROGRESS`——非 IN_PROGRESS 抛 `ERR_APS_OP_ILLEGAL_TRANSITION`（expected=IN_PROGRESS）；通过后 `setStatus(FINISHED)`。
      - Skill: `nop-backend-dev`
- [x] **Fix（cancel 守卫）**：`cancel:129-135` 增守卫 `status∈{DRAFT,PLANNED,IN_PROGRESS}`（对齐 owner doc §2 L41 PLANNED|IN_PROGRESS→CANCELLED + DRAFT 未排程可取消）——非三态抛 `ERR_APS_OP_ILLEGAL_TRANSITION`（expected=DRAFT/PLANNED/IN_PROGRESS）；通过后 `setStatus(CANCELLED)`。
      - Skill: `nop-backend-dev`
- [x] **Proof**：测试——start：PLANNED→IN_PROGRESS 成功 / FINISHED start assertThrows / CANCELLED start assertThrows；complete：IN_PROGRESS→FINISHED 成功 / PLANNED complete assertThrows / FINISHED complete assertThrows；cancel：DRAFT|PLANNED|IN_PROGRESS→CANCELLED 成功 / FINISHED cancel assertThrows / CANCELLED cancel assertThrows。迁移既有依赖「任意态可迁移」的测试（补前置态 seed）。
      - Skill: `nop-backend-dev`
- [x] **Add（owner doc）**：aps/state-machine.md §2 迁移图 + 迁移表核对一致——start/complete/cancel 经 status 守卫落地（start 仅 PLANNED / complete 仅 IN_PROGRESS / cancel 仅 DRAFT|PLANNED|IN_PROGRESS）；§残留风险补注「非法迁移（FINISHED/CANCELLED 终态→他态）经 ERR_APS_OP_ILLEGAL_TRANSITION 拦截」。
      - Skill: `none`

Exit Criteria:

- [x] start/complete/cancel 守卫落地（grep 确认 ERR_APS_OP_ILLEGAL_TRANSITION 抛出点）；if/else 死代码消除；新增/迁移测试全绿（Closure Gates 跑全量 mvn）；owner doc §2 与代码一致。

### Phase 3 - aps+logistics cancel 审批门控 + logistics 部分签收 owner doc Deferred 标注（P1-MA2-078 + P1-MA2-079）

Status: completed
Targets: `docs/design/aps/state-machine.md`、`docs/design/logistics/state-machine.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] aps/state-machine.md §6 L77「取消执行中的工序：需生产主管审批」正式化为「**Deferred**——当前 cancel @BizMutation 经 @BizMutation 入口权限覆盖（任意授权角色可执行），生产主管审批工作流（cancel-approve 动作 + 角色-resource 种子 + 审批 SPI）留 successor；cancel 操作本身业务正确（不破坏库存/GL，副作用经下游 mfg 工单级联）」；命名 successor 触发条件。
      - Skill: `none`
- [x] logistics/state-machine.md §4 L51「IN_TRANSIT→CANCELLED | 发货员+审批」+ §4 L66 正式化为「**Deferred**——当前 cancelShipment 经状态守卫 + 网关 client.cancelShipment（防承运商侧双发）覆盖，物流主管审批工作流（cancel-approve 动作 + 角色-resource 种子）留 successor；网关 cancel 已落实防双发」；命名 successor 触发条件。
      - Skill: `none`
- [x] logistics/state-machine.md §2 L40「部分签收」+ §4 L66 正式化为「**Deferred**——当前 advanceTracking 仅处理完整 TRACKING_EVENT_DELIVERED；承运商支持部分签收回调时实现 TRACKING_EVENT_PARTIAL 常量 + receivedQuantity/partialSignedQty 字段（须 ORM ask-first 加列）+ 累计签收判定（状态保持 IN_TRANSIT 直至全部签收）」；命名 successor 触发条件。
      - Skill: `none`

Exit Criteria:

- [x] aps/state-machine.md + logistics/state-machine.md 明确 078/079 Deferred，owner doc 与代码（cancel 无审批 + advanceTracking 仅完整签收）一致；successor 触发事件已命名。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04fcfddb7ffehWVhIeJdtZ2ygs, fresh session) because 全部基线声明经实仓 file:line 验证 TRUE（start:110-114 if/else 死代码两分支同设 IN_PROGRESS 无守卫 / complete:123 + cancel:132 直接 setStatus 无守卫 / cancelShipment:131-156 仅状态检查 + 网关 client.cancelShipment 零审批门控 / advanceTracking:162-185 仅 DELIVERED/IN_TRANSIT/PICKED_UP 无部分签收 / logistics 全域零 partial/PARTIAL 业务代码 + 无 TRACKING_EVENT_PARTIAL / ErpApsErrors + ErpApsConstants 存在且 ERR_APS_OP_ILLEGAL_TRANSITION 不存在 / OP_STATUS_* 存在）；077 实现=arm-index 方案A 推荐；078/079 经 arm-index 列举的方案B owner-doc 标注解决 drift（对齐代码现实 + 命名 successor）= 合法 drift 解决非缺陷隐瞒（审计确认 cancel 操作业务正确无数据破坏 + 显式声明 078 偏离推荐方案）；Closure Gates 含 mvn 门控（077 含代码变更）；规则 4/14（aps+logistics 状态机同 A2.15 批次）+ 7/9/10/13 + anti-slack 全满足。采纳非阻塞修订：Phase 2 start 守卫补 Containment 核实（start/complete/cancel 均为手工 @BizMutation 入口点，grep 零内部/跨域调用方，mfg 仅注释引用，故新增守卫不阻断级联路径）。

## Closure Gates

> 本计划含代码变更（P1-MA2-077），故 Closure Gates 含全量 `mvn` 验证（见执行时规则 7）。

- [x] 范围内行为/文档完成（077 状态守卫实现 + 078/079 Deferred 标注）
- [x] 相关文档对齐（aps/state-machine.md + logistics/state-machine.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 全绿 + aps 域 `mvn test` 全绿[36 tests, 0 failures] + compliance checker 本计划零新增命中；grep 验证 077 守卫落地[3 抛出点]）
- [x] 无范围内项目降级为 deferred/follow-up（077 为范围内存活实现项；078/079 Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### aps+logistics cancel 审批工作流（P1-MA2-078 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc §6 声明的工作流审批属 missing-feature（需审批 SPI + 角色-resource 种子）；审计确认 cancel 操作本身业务正确（aps 纯排产不破坏库存/GL；logistics 网关 cancel 已防承运商侧双发）；A2.18 确认审批/角色基础设施全域不成熟（19 模块 data-auth.xml 全空）。
- Successor Required: `yes`（审批工作流 SPI 落地时实现 cancel-approve 动作（IN_PROGRESS/IN_TRANSIT 源态需审批令牌）+ config-gated 角色-resource 门控 + _erp-*.action-auth.xml 种子）

### logistics 部分签收（P1-MA2-079 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 实现须 ORM ask-first 加列（receivedQuantity/partialSignedQty）+ 累计签收判定；承运商回调暂只发完整 DELIVERED 事件——属 owner doc Deferred 业务场景；完整签收主路径完整覆盖。
- Successor Required: `yes`（承运商支持部分签收回调时实现 TRACKING_EVENT_PARTIAL 常量 + receivedQuantity 字段 + 累计签收判定，状态保持 IN_TRANSIT 直至全部签收）

## Closure

Status Note: 完成（三项 finding 处置落地：P1-MA2-077 状态守卫实现 + P1-MA2-078/079 owner doc Deferred 标注；独立结束审计 PASS）。

Closure Audit Evidence:

- 独立结束审计（fresh session `ses_04fa2617fffecLGMA7nupYCAg1`）逐项实仓验证 PASS。**P1-MA2-077**：`ErpApsErrors.java:81-84` 定义 `ERR_APS_OP_ILLEGAL_TRANSITION`（`erp.err.aps.op-illegal-transition`，ARG_OP_CODE/ARG_CURRENT_STATUS/ARG_EXPECTED_STATUS，后者声明于 `:21`）；`ErpApsOperationOrderBizModel.java` 三守卫落地且使用 `NopException`+`ErrorCode`+`.param()`——start `:111-117`（仅 PLANNED，if/else 死代码已消除）、complete `:126-132`（仅 IN_PROGRESS）、cancel `:142-153`（仅 DRAFT/PLANNED/IN_PROGRESS）；三处抛出点 `:112/:127/:145`。containment 经 grep 确认零内部/跨域调用方（mfg `ErpMfgScheduleToJobCardProcessor.java:40` 仅为注释引用），仅 GraphQL 入口可达，守卫不阻断级联路径。测试 `TestErpApsOperationOrderStateGuards.java` 11 用例覆盖 start/complete/cancel 全成功+终态失败路径，独立实跑 `Tests run: 11, Failures: 0`；全 aps-service 套件 `Tests run: 36, Failures: 0, BUILD SUCCESS` 验证全绿基线。`mvn clean install -DskipTests` 全绿。
- **P1-MA2-078/079**：aps `state-machine.md:49`(§3 守卫注)/`:78`(§6 cancel Deferred+successor) + logistics `:40`(§2 部分签收 Deferred)/`:51`(IN_TRANSIT→CANCELLED Deferred+successor)/`:66`(§4 部分签收 Deferred) 均含明确 Deferred 标注与命名 successor 触发条件。
- **保护区域**：`git diff --stat` 仅触及 BizModel/Errors/IBiz javadoc + owner doc + plan + 测试，无 `*.orm.xml` schema / 会计过账 / 数据删除 / auth / 部署变更。三项 finding 处置（077 实现 + 078/079 owner doc Deferred 裁决）与 arm-index 推荐偏差已在计划显式声明。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件。
