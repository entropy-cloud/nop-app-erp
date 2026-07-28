# 2026-07-28-1249-1-audit-remediation-ma2-aps-logistics-state-machine MA2 aps+logistics 状态机审查（A2.15）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.15）
> Related: `docs/plans/2026-07-28-1020-3-audit-remediation-ma2-ext-domains-state-machine.md`（A2.14 crm+cs+contract+b2b+maintenance 状态机——logistics FREIGHT 过账 + b2b EDI 异步同型，tryPost 容错/异步回调幂等同型范式）；`docs/plans/2026-07-28-0400-3-audit-remediation-ma2-inventory-state-machine.md`（A2.11 inventory——logistics 关联出库单锁定释放 + aps ErpInvReservation/ErpInvStockBalance 跨域只读复核）；`docs/plans/2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a mfg——aps OperationOrder 由 WorkOrder 下达触发创建，级联取消联动）；`docs/plans/2026-07-04-0831-1-aps-operation-order-scheduling-engine.md`（aps 排产引擎 owner doc §实现偏离补注来源）；`docs/plans/2026-07-04-1115-3-logistics-carrier-gateway-spi-freight-posting.md`（logistics 承运商网关 SPI + FREIGHT 过账 owner doc §实现偏离补注来源）；`docs/plans/2026-07-14-0508-1-aps-b2b-logistics-direct-action-e2e.md`（aps/logistics DIRECT 业务动作 E2E 已落地——排产/插单/发运/网关回调 happy path 已验证）；`docs/plans/2026-07-14-0941-2-b2b-aps-logistics-cross-domain-e2e.md`（aps 插单 + logistics DELIVERED 运费过账跨域编排 E2E 已落地）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/aps/state-machine.md` + `docs/design/logistics/state-machine.md`（owner docs）
> Audit: required

## Current Baseline

aps + logistics（排产/物流）两域合并 C 级状态机审查。两域是 ERP **生产计划排产 / 运输配送**的执行层，状态机驱动工序工单与发运单的**业务对象生命周期**。两域状态机核心契约经 MA1 平台合规审计 A1.13（owner-doc 抽样核查 OperationOrder/Shipment 状态定义与 dict `erp-aps/operation-order-status`/`erp-log/shipment-status` 一致性 ✅）确认基础一致，但从未做系统性状态机业务审查。两域状态机的保护区域边界：logistics DELIVERED 经 `IErpFinVoucherBiz.post(PostingEvent{FREIGHT})` Facade 过账（参 inventory InvPostingExecutor 范式，REQUIRES_NEW 跨域失败隔离）；logistics path-2 到岸成本 config-gated（默认 off）经 `IErpInvLandedCostBiz.generateFreightLandedCost` Facade；aps 排产纯内存算法（ErpApsSchedulingEngine POJO capacity=1 无工作中心日历配置依赖）+ 跨域只读 ErpInvReservation/ErpInvStockBalance/ErpMfgBom/ErpMfgBomOperation（P1-MA1-022 aps 扩展）。

实时仓库已落地的两域状态机实现（待审查）：

- **aps 域**（`module-aps/`，7 实体 / 35 Java / 2 状态机实体，全域最小业务域之一）：OperationOrder 5 态（DRAFT/PLANNED/IN_PROGRESS/FINISHED/CANCELLED，dict `erp-aps/operation-order-status`）+ Schedule 排产方案生命周期（DRAFT→PUBLISHED→ARCHIVED）+ 排产引擎 scheduleForward/scheduleBackward（正/逆向填充 plannedStart/EndDateT）+ 插单 insertRushOrder 区间重排（受影响区间工序回退 DRAFT）+ ErpApsSchedulingProcessor/ErpApsAtpCtpServiceImpl 跨域只读（P1-MA1-022 aps 扩展：ErpInvReservation/ErpInvStockBalance/ErpMfgBom/ErpMfgBomOperation）。aps 域 DIRECT 业务动作 E2E 已落地（2026-07-14-0508-1：schedule 状态机 + scheduleForward/scheduleBackward；2026-07-14-0941-2：insertRushOrder 区间重排）。`P1-MA1-022`（aps 扩展跨域只读 daoFor）。
- **logistics 域**（`module-logistics/`，12 实体 / 44 Java / 11 mutation）：Shipment 发运单 6 态（DRAFT/ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED，dict `erp-log/shipment-status`）+ 承运商网关 SPI（MockClientFactory 默认 SUCCESS）+ 网关异常重试（最多 3 次指数退避，耗尽保留 ADVISED + 人工干预）+ 部分签收（保持 IN_TRANSIT）+ DELIVERED 触发 FREIGHT 运费过账（path-1 SALES_DELIVERY 经 IErpFinVoucherBiz.post Facade）+ path-2 到岸成本 config-gated（path2-landed-cost-auto-create 默认 false）+ 轮询兜底 scanForPolling + ErpLogShipmentLog 追踪日志。logistics DIRECT 业务动作 + 跨域编排 E2E 已落地（2026-07-14-0508-1：advise/completeShipment/cancelShipment 网关全链；2026-07-14-0941-2：handleTrackingWebhook DELIVERED→onDelivered→FREIGHT 过账 + 幂等守卫）。`P1-MA1-022`（logistics Dashboard facade read-only 永久接受；log 无跨域写 daoFor 站点）。

**已登记的直指两域状态机的 finding（本审计须复核其状态机行为）**：

- `P1-MA1-022`（todo MR1，aps 扩展）：aps 跨域只读 daoFor（ErpApsAtpCtpServiceImpl/ErpApsSchedulingProcessor ErpInvReservation/ErpInvStockBalance/ErpMfgBom/ErpMfgBomOperation）。**状态机 scope**：跨域只读是排产输入/ATP-CTP 查询副作用，不破坏 OperationOrder 状态机——本审计复核异常路径无悬挂。

**但从未做过一次覆盖两域全状态机（aps OperationOrder + Schedule 排产方案 / logistics Shipment 发运单）、按 `state-machine-business-review-prompt.md` 10 维度的系统性业务审查**。已知未核验控制点（各域 state-machine.md §审查提示 + 已登记 finding）：

- **状态定义清晰性**：aps OperationOrder 5 态（PLANNED 等待车间确认 / IN_PROGRESS 报工中）；logistics Shipment 6 态（ADVISED 等待承运商接单 / DISPATCHED 等待运输更新 / IN_TRANSIT 等待签收 / DELIVERED 触发运费过账）。
- **转换完整性**：aps OperationOrder 迁移（DRAFT→PLANNED + PLANNED→IN_PROGRESS→FINISHED + PLANNED|IN_PROGRESS→CANCELLED + **PLANNED→DRAFT 重排回退** + **insertRushOrder 区间重排**）；logistics Shipment 迁移（DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED + DRAFT/ADVISED→CANCELLED + IN_TRANSIT→CANCELLED 货物退回）+ **网关异常重试幂等**（ADVISED 重试 3 次耗尽）+ **部分签收**（保持 IN_TRANSIT）。是否有非法跳转或缺失条件分支。
- **终端状态与恢复**：aps FINISHED/CANCELLED 终态（不可直接恢复，需新建）；logistics DELIVERED/CANCELLED 终态（DELIVERED 后退货走 sales 标准退货；CANCELLED 可新建发运单）。
- **异常路径**：aps **并发排产同一工作中心产能双倍占用**（owner doc §4 明列"乐观锁或资源锁防止产能双倍占用"——是否落地，交接 A2.17）/ 工作中心故障停机 / **PLANNED→DRAFT 重排未限定区间致全局重排**（牛顿效应）；logistics **网关异常重试耗尽无告警闭环**（ADVISED 保留 + 人工干预是否产 TODO）/ **DELIVERED 运费过账失败悬挂**（tryPost 容错，与 finance P1-MA2-032 + mfg P1-MA2-035/036 + hr P1-MA2-048 + assets P1-MA2-060 + qa A2.12 + projects P1-MA2-068 + maintenance P1-MA2-074 + ext A2.14 同型——升级评估）/ **关联出库单取消发运后锁定未释放**（DRAFT→CANCELLED / ADVISED→CANCELLED 释放）/ 追踪长时间无更新。
- **可达性**：各域状态可达性；aps PLANNED→DRAFT 回退路径合法性；logistics 无不可达状态。无死锁。
- **角色与权限**：aps 排产（APS 引擎/计划员）+ 取消执行中工序（**生产主管审批** owner doc §6 危险操作）+ 重排（APS 引擎/计划员）；logistics 发运（发货员）+ 取消在途（**物流主管审批** owner doc §6 危险操作）+ 网关回调（系统自动）。是否有危险操作对任何角色开放。
- **外部依赖**：aps WorkOrder 下达触发创建 OperationOrder（main→aps）+ WorkOrder 取消级联取消 + ErpApsConstraint 排产输入 + 跨域只读 ErpInvReservation/ErpMfgBom；logistics sales 出库事件触发自动创建草稿 + 承运商网关回调（异步追踪）+ DELIVERED→IErpFinVoucherBiz.post FREIGHT 过账（REQUIRES_NEW）+ path-2 config-gated IErpInvLandedCostBiz.generateFreightLandedCost + 轮询兜底 scanForPolling。
- **TODO/任务策略**：aps PLANNED pool（车间调度员待执行工序）+ IN_PROGRESS assigned（操作工）+ "PLANNED 超 24h 未开工催办"；logistics DRAFT assigned（发货员待确认发运）+ ADVISED assigned（等待承运商接单/网关异常人工处理）+ "DRAFT 超 24h 升级" + "网关异常 4h 升级"。是否存在期望有人行动但不产生待办的状态（ADVISED 网关异常重试耗尽是否产 TODO）。
- **场景演练**：(a) aps 前向排产 happy path（DRAFT→PLANNED）；(b) aps 后向排产（产能不足告警）；(c) **aps 插单区间重排**（受影响工序回退 DRAFT 重排）；(d) **aps 并发排产同一工作中心**（乐观锁——交接 A2.17）；(e) **aps 取消执行中工序审批**；(f) logistics 正常发运送达（DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED + FREIGHT 过账）；(g) **logistics 网关异常重试 3 次耗尽 + 人工干预**；(h) **logistics 部分签收**（保持 IN_TRANSIT）；(i) **logistics 货物退回审批**（IN_TRANSIT→CANCELLED 物流主管审批）；(j) **logistics 关联出库单取消发运锁定释放**；(k) **logistics DELIVERED 过账失败悬挂**（tryPost——升级评估）。
- **与设计文档一致性**：各域 `state-machine.md` vs 实现——重点核验：(1) aps §2 PLANNED→DRAFT 重排回退 + §4 并发排产乐观锁 + §6 取消执行中工序审批；(2) logistics §2 网关异常重试 + 部分签收 + §4 网关异常人工干预 + §6 取消在途审批 + §7 DELIVERED FREIGHT 过账 Facade（实现裁决补注 plan 2026-07-04-1115-3 + 2026-07-11-2329-1 path-1/path-2）。

剩余差距：需要一次系统性状态机业务审查，发现任何遗漏的 P0（**aps 并发排产产能双倍占用** [若破坏产能预留不变量——升级评估或交接 A2.17] / **aps PLANNED→DRAFT 重排未限定区间致全局重排** [若破坏区间重排约束] / **logistics DELIVERED 运费过账失败悬挂无告警闭环** [若破坏业财一致——同型升级评估] / **logistics 网关异常重试耗尽无告警/TODO 致发运单悬挂** [若破坏生命周期] / **logistics 关联出库单取消发运后锁定未释放致重复发运阻断** [若破坏发运锁定不变量]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **aps（OperationOrder 5 态 + Schedule 排产方案 + scheduleForward/scheduleBackward + insertRushOrder 区间重排）+ logistics（Shipment 6 态 + 网关 SPI + 重试 + 部分签收 + DELIVERED FREIGHT 过账 + path-2 到岸成本 config-gated）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（aps PLANNED/IN_PROGRESS / logistics ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED）；(2) 转换完整性（各域生命周期迁移 + **aps PLANNED→DRAFT 重排回退 + insertRushOrder 区间重排** / **logistics 网关异常重试幂等 + 部分签收**）；(3) 终端与恢复（aps FINISHED 不可重排 / logistics DELIVERED 退货走 sales）；(4) 异常路径（**aps 并发排产产能双倍占用 + 重排范围** / **logistics 网关异常重试耗尽 + 过账失败悬挂 + 出库单锁定释放**）；(5) 可达性（aps PLANNED→DRAFT 回退）；(6) 角色权限（**aps 取消执行中工序生产主管审批** / **logistics 取消在途物流主管审批**）；(7) 外部依赖（aps WorkOrder 级联 / logistics 网关回调 + FREIGHT 过账 Facade）；(8) TODO 任务策略（**logistics 网关异常重试耗尽是否产 TODO**）；(9) 场景演练（11 个代表性场景）。
- 复核已登记 finding 在两域状态机运行时的行为影响：P1-MA1-022（aps 跨域只读 daoFor——异常路径复核），标注终态。
- scope matrix §状态机正确性 aps/log 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.15 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.6a manufacturing 工单状态机 — done；本审计只复核 aps OperationOrder 由 WorkOrder 下达触发创建 + WorkOrder 取消级联取消的状态机联动角度。
- **不**审计 A2.11 inventory 状态机 — done；本审计只复核 logistics 关联出库单锁定释放 + aps ErpInvReservation/ErpInvStockBalance 跨域只读（P1-MA1-022）的状态机角度。
- **不**审计 A2.1 P2P / A2.2 O2C 端到端 — done；本审计只复核 logistics path-2 到岸成本 config-gated 跨域 Facade 的状态机角度。
- **不**审计 A2.17 并发与乐观锁 — 并发排产产能双倍占用 / 并发更新同一发运单归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 A4.x view.xml drift / A4.5 两域代码质量抽样 — 归 MA4。
- **不**审计 config-gated / Deferred 偏离是否应实现（logistics path-2 到岸成本 config / 网关 SPI / aps 排产配置） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/aps/state-machine.md`（OperationOrder 5 态 + §2 PLANNED→DRAFT 重排 + §4 并发排产乐观锁 + §6 取消审批）；`docs/design/logistics/state-machine.md`（Shipment 6 态 + §2 网关异常重试 + 部分签收 + §4 人工干预 + §6 取消在途审批 + §7 DELIVERED FREIGHT 过账 Facade 实现裁决补注）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）；`docs/architecture/posting-exemptions.md`（logistics FREIGHT Facade 豁免登记复核）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.15 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：两域状态机本身非 ask-first 最高级保护区域，但 **logistics DELIVERED FREIGHT 运费过账触及 finance 凭证链**（IErpFinVoucherBiz.post REQUIRES_NEW）+ **path-2 到岸成本 config-gated 触及 inventory 成本层**（IErpInvLandedCostBiz Facade）。P0 即时修复若触及 `ErpLogShipmentBizModel`/logistics posting dispatcher/aps scheduling processor/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计保护区域）。ORM 字典变更（aps operation-order-status、logistics shipment-status）属 ask-first。xbiz 文件变更属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 两域状态机系统性业务审查

Status: completed
Targets: `module-aps/`（OperationOrder BizModel 5 态 + Schedule 排产方案 DRAFT→PUBLISHED→ARCHIVED + ErpApsSchedulingEngine scheduleForward/scheduleBackward POJO + insertRushOrder 区间重排 + ErpApsSchedulingProcessor/ErpApsAtpCtpServiceImpl 跨域只读）；`module-logistics/`（Shipment BizModel 6 态 + 承运商网关 SPI + handleTrackingWebhook 回调 + onDelivered FREIGHT 过账 Facade + path-2 config-gated + scanForPolling 轮询兜底 + ErpLogShipmentLog）
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P1-MA1-022 aps 扩展跨域只读已登记，本审计复核状态机角度）；A2.6a mfg done（WorkOrder 级联 aps OperationOrder 联动）；A2.11 inventory done（logistics 关联出库单锁定 + aps ErpInvReservation 跨域只读）；A2.14 ext-domains done（logistics FREIGHT 过账 + b2b EDI 异步回调幂等同型范式）；aps/logistics DIRECT 业务动作 + 跨域编排 E2E 已落地（2026-07-14-0508-1 + 2026-07-14-0941-2 happy path 已验证，本审计补系统性状态机正确性审查）

- [x] 维度「状态定义」：审查 aps OperationOrder 5 态（PLANNED 等待车间确认 / IN_PROGRESS 报工中）；logistics Shipment 6 态（ADVISED 等待承运商接单 / DISPATCHED 等待运输更新 / IN_TRANSIT 等待签收 / DELIVERED 触发运费过账）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：aps OperationOrder 迁移（DRAFT→PLANNED + PLANNED→IN_PROGRESS→FINISHED + PLANNED|IN_PROGRESS→CANCELLED + **PLANNED→DRAFT 重排回退** + **insertRushOrder 区间重排**）；logistics Shipment 迁移（DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED + DRAFT/ADVISED→CANCELLED + IN_TRANSIT→CANCELLED 货物退回）+ **网关异常重试幂等**（ADVISED 重试 3 次耗尽）+ **部分签收**（保持 IN_TRANSIT）。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：aps FINISHED/CANCELLED 终态（不可直接恢复，需新建 OperationOrder；FINISHED 不可重排）；logistics DELIVERED/CANCELLED 终态（DELIVERED 后退货走 sales 标准退货；CANCELLED 可新建发运单关联原出库单）。归档与活跃区分。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——aps **并发排产同一工作中心产能双倍占用**（owner doc §4 声明乐观锁/资源锁——是否落地，交接 A2.17）/ 工作中心故障停机 / **PLANNED→DRAFT 重排未限定区间致全局重排**（牛顿效应）；logistics **网关异常重试耗尽无告警闭环**（ADVISED 保留 + 人工干预是否产 TODO）/ **DELIVERED 运费过账失败悬挂**（tryPost 容错——与 finance P1-MA2-032 + ext A2.14 P1-MA2-074 同型升级评估）/ **关联出库单取消发运后锁定未释放**（DRAFT→CANCELLED / ADVISED→CANCELLED 释放）/ 追踪长时间无更新超期。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：各域状态可达性；**aps PLANNED→DRAFT 回退路径合法性**（仅 APS 引擎/计划员可执行）；logistics 无不可达状态。无死锁。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：aps 排产（APS 引擎/计划员）+ **取消执行中工序生产主管审批**（owner doc §6 危险操作）+ 重排（APS 引擎/计划员）；logistics 发运（发货员）+ **取消在途物流主管审批**（owner doc §6 危险操作）+ 网关回调（系统自动）。是否有危险操作对任何角色开放。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：aps WorkOrder 下达触发创建 OperationOrder（main→aps）+ WorkOrder 取消级联取消 + ErpApsConstraint 排产输入 + 跨域只读 ErpInvReservation/ErpInvStockBalance/ErpMfgBom/ErpMfgBomOperation；logistics sales 出库事件触发自动创建草稿 + 承运商网关回调（异步追踪）+ DELIVERED→IErpFinVoucherBiz.post FREIGHT 过账（REQUIRES_NEW）+ path-2 config-gated IErpInvLandedCostBiz.generateFreightLandedCost + 轮询兜底 scanForPolling。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：aps PLANNED pool（车间调度员待执行工序）+ IN_PROGRESS assigned（操作工）+ "PLANNED 超 24h 未开工催办"；logistics DRAFT assigned（发货员待确认发运）+ ADVISED assigned（等待承运商接单/网关异常人工处理）+ "DRAFT 超 24h 升级" + "网关异常 4h 升级"。是否存在期望有人行动但不产生待办的状态（**ADVISED 网关异常重试耗尽是否产 TODO**）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) aps 前向排产 happy path（DRAFT→PLANNED）；(b) aps 后向排产（产能不足告警）；(c) **aps 插单区间重排**（受影响工序回退 DRAFT 重排）；(d) **aps 并发排产同一工作中心**（乐观锁——交接 A2.17）；(e) **aps 取消执行中工序审批**；(f) logistics 正常发运送达（DRAFT→ADVISED→DISPATCHED→IN_TRANSIT→DELIVERED + FREIGHT 过账）；(g) **logistics 网关异常重试 3 次耗尽 + 人工干预**；(h) **logistics 部分签收**（保持 IN_TRANSIT）；(i) **logistics 货物退回审批**（IN_TRANSIT→CANCELLED 物流主管审批）；(j) **logistics 关联出库单取消发运锁定释放**；(k) **logistics DELIVERED 过账失败悬挂**（tryPost——升级评估）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在各域 `state-machine.md` 是否有匹配——重点核验：(1) aps §2 PLANNED→DRAFT 重排回退 + §4 并发排产乐观锁 + §6 取消执行中工序审批；(2) logistics §2 网关异常重试 + 部分签收 + §4 网关异常人工干预 + §6 取消在途审批 + §7 DELIVERED FREIGHT 过账 Facade（实现裁决补注 plan 2026-07-04-1115-3 + 2026-07-11-2329-1 path-1/path-2）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 两域状态机角度：P1-MA1-022（aps 跨域只读 daoFor——异常路径复核，状态机迁移不依赖跨域只读结果），标注终态。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md`（含：aps OperationOrder + logistics Shipment 状态图与迁移矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、PLANNED→DRAFT 重排范围/插单区间重排/并发排产乐观锁/网关异常重试幂等/部分签收/DELIVERED 过账悬挂/出库单锁定释放/取消审批裁决、MA1 finding 运行时影响复核表、并发敏感点交接 A2.17、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] aps/logistics 两域状态图与迁移矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 已识别控制点（状态定义 / 转换完整性[含 aps 重排 + logistics 网关重试 + 部分签收] / 终端与恢复 / 异常路径[含并发排产 + 过账悬挂 + 出库单锁定释放 + 网关异常告警] / 可达性[含 PLANNED→DRAFT] / 角色权限[含取消审批] / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 两域状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 aps/log 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**aps 并发排产产能双倍占用** [若破坏产能预留不变量] / **aps PLANNED→DRAFT 重排未限定区间致全局重排** [若破坏区间重排约束] / **logistics DELIVERED 运费过账失败悬挂无告警闭环** [若破坏业财一致——同型升级评估] / **logistics 网关异常重试耗尽无告警/TODO 致发运单悬挂** [若破坏生命周期] / **logistics 关联出库单取消发运后锁定未释放致重复发运阻断** [若破坏发运锁定不变量]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。本审计对已登记 finding 只复核状态机运行时影响不重复登记根因；新 P1（如 aps 重排范围缺口 / logistics 网关异常告警缺口 / Deferred CRUD 空壳死状态 [若确认] / 过账悬挂同型 [若确认]）按新 finding ID 登记。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 aps/log 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_058eb3603ffebvXTBu05mElOtO`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：`module-aps/`/`module-logistics/` BizModel 存在 ✓；字典 `erp-aps/operation-order-status` 5 态 + `erp-log/shipment-status` 6 态 + `schedule-status` 3 态与基线一致 ✓；`P1-MA1-022` aps 扩展描述与 arm-index:84 精确匹配 + logistics Dashboard facade read-only 永久接受标注 ✓；scope matrix §状态机正确性 aps/log 列均 `❓` ✓；A2.15 是 A2.14 done 后首个 `todo` + C 级合并单 plan 粒度正确 ✓；owner docs + skill 存在 ✓；最低规则 R1/R2/R4/R7/R8/R10-R11 全 PASS；反松弛零禁词（`[若破坏…]` 条件证伪框架可接受，与 A2.14 同型）。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。logistics FREIGHT 过账 + path-2 到岸成本触及会计保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [x] 范围内行为完成（A2.15 aps+logistics 状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、两域 state-machine owner doc 结论已反映）
- [x] 已运行验证：审计不改代码，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.6a manufacturing 工单状态机（aps OperationOrder 由 WorkOrder 级联）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.6a done（manufacturing 工单/作业卡/领料/委外状态机组件齐备已确认）。本审计做 aps 状态机**业务正确性**审查；OperationOrder 由 WorkOrder 下达触发创建 + WorkOrder 取消级联取消的联动状态机角度复核归 A2.6a。
- Successor Required: `no`——A2.6a 已 done。

### A2.11 inventory 状态机（logistics 关联出库单锁定 + aps 跨域只读）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.11 done（inventory 移动单/盘点/拣货状态机组件齐备已确认）。本审计做两域状态机**业务正确性**审查；logistics 关联出库单锁定释放 + aps ErpInvReservation/ErpInvStockBalance 跨域只读（P1-MA1-022）的状态机角度复核归 A2.11。
- Successor Required: `no`——A2.11 已 done。

### A2.17 并发与乐观锁（并发排产产能双倍占用 / 并发更新同一发运单）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（aps 并发排产同一工作中心产能双倍占用 + logistics 并发更新同一发运单乐观锁），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated / Deferred 偏离本身（logistics path-2 到岸成本 config / 承运商网关 SPI / aps 排产配置）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/SPI/Deferred（path-2 默认 off；网关 SPI 实现裁决补注已落地）。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如 path-2 全量上线 / 承运商网关真实对接）。

## Closure

Status Note: A2.15 aps+logistics 状态机系统性业务审查完成。审计报告 `docs/audits/2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md` 产出（10 维度审查 + 状态图与迁移矩阵 + 控制点裁决汇总 + 已登记 finding 复核 + 6 处并发敏感点交接 A2.17 + 剩余风险）。**零 P0**（5 个候选 P0 经证据证伪或降级）；**4 项新 P1**（P1-MA2-077 aps OperationOrder start/complete/cancel 缺状态守卫 / P1-MA2-078 aps+logistics cancel 缺审批门控合并 / P1-MA2-079 logistics 部分签收未实现 / P1-MA2-080 logistics 网关异常 + 过账失败缺告警闭环合并）登记至 arm-index §P1 汇总；**2 项新 P2** watch-only（P2-MA2-071/072）登记至 arm-index §P2 汇总；1 项 MA1 finding（P1-MA1-022 aps 跨域只读）运行时复核无升级。arm-index 报告清单 + scope matrix §状态机正确性 aps/log 列由 `❓` 推进至 `⚠️P1(A2.15✅)`（MA2 状态机正确性维度全域 13 业务列收尾，无 ❓）。本审计未改代码，build/test 门控属回归基线确认。独立结束审计由独立子代理（新会话，fresh-context）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（opencode glm-5.2，fresh-context 新会话，不重用执行者上下文）
- Evidence: `docs/audits/2026-07-28-1249-arm-ma2-aps-logistics-state-machine.md`（10 维度 + 状态图 + 4 P1 + 2 P2 + 6 并发敏感点）；`docs/audits/arm-index.md`（报告清单新增本报告行 + P1-MA2-077~080 4 行 + P2-MA2-071/072 2 行 + §A2.15 新增项段落）；`docs/audits/audit-remediation-scope-and-dimension-matrix.md`（§2.2 状态机正确性 aps/log 列 `❓` → `⚠️P1(A2.15✅)` + §A2.15 note）；`docs/backlog/audit-remediation-roadmap.md`（A2.15 `todo` → `done`）
- Independent Closure Audit Pass: 独立子代理 fresh-context 复核——审计报告存在且完整（10 维度全覆盖 + 状态图 + 迁移矩阵 + 控制点裁决汇总 §1-§10）；4 P1 + 2 P2 finding 已落地 arm-index.md（grep `P1-MA2-07[789]|P1-MA2-080|P2-MA2-07[12]` 74 处匹配跨 arm-index + scope matrix + 报告）；scope matrix §2.2 aps/log 列 `⚠️P1(A2.15✅)`；roadmap A2.15 `done`（line 69）；plan guide 规则 12 已满足（独立子代理新会话执行，非执行者自审）；Plan Status / 两 Phase Status / Exit Criteria / Closure Gates / Closure 文本一致性已验证（无 ❓/无残留 `[ ]`/无降级 P0）。

Follow-up:

- MR1 裁决 4 项新 P1（P1-MA2-077~080，与全域同型 finding 一并整体裁决）
- MR1 顺手 2 项新 P2（P2-MA2-071/072，owner doc 章节补充 + 概念性弱指针注记）
- A2.17 复核 6 处并发敏感点（含 aps 并发排产产能双倍占用 + 全域 @Version 透明乐观锁降级）
- 独立子代理结束审计（新会话）验证本 plan 一致性
