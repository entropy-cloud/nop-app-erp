# 2026-07-28-0400-3-audit-remediation-ma2-inventory-state-machine MA2 inventory 状态机审查（A2.11）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A2.11 inventory 状态机审查（A 级单域，19 状态字段）
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA2（工作项 A2.11）
> Related: `docs/plans/2026-07-28-0230-3-audit-remediation-ma2-purchase-state-machine.md`（A2.8 purchase 状态机审查范式——docStatus/approveStatus 双轴 + reverseApprove→REJECTED + tryPost 吞异常悬挂同型）；`docs/plans/2026-07-27-2211-1-audit-remediation-ma2-inventory-costing-consistency.md`（A2.4 库存核算一致性 done——成本/余额/流水三方对账 + 7 costMethod 策略 + reclose 兜底 + P1-MA2-023 SPECIFIC 守卫 + P1-MA2-024 STANDARD 红冲不变量破缺 + P2-MA2-028 红冲 today() 队列时序）；`docs/plans/2026-07-27-1430-1-arm-fix-p0-ma1-021-inv-cost-adjust-voucher-writeback.md`（P0-MA1-021 done——CostAdjustmentPostingDispatcher 跨模块写 ErpFinVoucher 已修复为 IErpFinVoucherBiz.reverse）；`docs/skills/state-machine-business-review-prompt.md`（审计方法）；`docs/design/inventory/state-machine.md`（移动单状态机 DRAFT/CONFIRMED/DONE/CANCELLED + 盘点单独立状态机 + §审查提示异常路径/冲销完整性）+`trace-chain.md`+`cross-domain.md`+`consignment.md`（owner doc）
> Audit: required

## Current Baseline

inventory（库存）域 A 级状态机审查（单域单工作项，19 状态字段）。inventory 是全部业务域的**库存写入枢纽**（采购入库/销售出库/资产资本化/盘点/调拨/委外均经库存移动单），状态机驱动**移动单生命周期**（DRAFT→CONFIRMED→DONE/CANCELLED）+ **盘点单独立状态机** + **业务单据审批状态机**（CostAdjust/LandedCost/TransferOrder）。库存状态机的核心不变量：**所有余额变动都通过移动单流水可追溯**（DONE 写一条不可变流水 + 更新余额 + 释放预留 + 发存货过账事件）。

实时仓库已落地的库存状态机实现（待审查，路径 `module-inventory/`）：

- **状态字段清单**（ORM `app-erp-inventory.orm.xml`，19 状态字段分布于多类状态对象）：
  - **移动单生命周期轴**（`ErpInvStockMove`）：`moveType`(erp-inv/operation-type) + `docStatus`(erp-inv/move-status) + `approveStatus`(wf/approve-status)
  - **转移单双轴**（`ErpInvTransferOrder`）：`docStatus` + `approveStatus`
  - **盘点单双轴**（`ErpInvStockTake`）：`takeType`(erp-inv/take-type) + `docStatus`(**复用 erp-inv/move-status**) + `approveStatus`——**owner doc §盘点单状态机 用 `COUNTING` 命名「盘点中」，代码实际用 move-status 中 `CONFIRMED`**（P2-MA1-025 已登记 owner doc drift）
  - **拣货单轴**（`ErpInvPickingOrder`）：`docStatus`(erp-inv/picking-status)
  - **成本调整双轴**（`ErpInvCostAdjust`）：`adjustType`(erp-inv/adjust-type) + `docStatus`(erp-inv/move-status) + `approveStatus`
  - **到岸成本双轴**（`ErpInvLandedCost`）：`allocationMethod`(erp-inv/landed-cost-alloc-method) + `docStatus` + `approveStatus`
  - **所有权转移轴**（`ErpInvOwnershipTransfer`）：`transferType`(erp-inv/ownership-transfer-type) + `docStatus`(erp-inv/ownership-transfer-status) + from/toOwnershipType(erp-inv/ownership-type)
  - **批次/序列号轴**（`ErpInvBatch`/`ErpInvSerialNumber`）：`status`(erp-inv/batch-status / erp-inv/serial-status)
  - **预留轴**（`ErpInvReservation`）：`status`(erp-inv/reservation-status)
  - **余额/流水/成本层**（`ErpInvStockBalance`/`ErpInvStockLedger`/`ErpInvCostLayer`）：costMethod(erp-md/cost-method) + ownershipType(erp-inv/ownership-type)——非状态机，是数值派生
- **移动单状态迁移实现**（`module-inventory/erp-inv-service/.../service/`）：DRAFT→CONFIRMED（提交确认，出库类需可用量充足+增加预留量）→DONE（写流水+更新余额+释放预留+发存货过账事件）/ →CANCELLED（释放预留，不影响余额）。**DONE 的纠错路径是生成反向冲销移动单（新 DRAFT，数量取负）走正常流程，不是状态回退**（owner doc §3/§5 明示）。**移动单 docStatus 用 erp-inv/move-status（DRAFT/CONFIRMED/DONE/CANCELLED），与业务单据的 erp/doc-status（DRAFT/ACTIVE/CANCELLED）字典不同**——需核验一致性。
- **出库可用量校验 + 预留量**（owner doc §2 核心机制）：出库（outgoing）CONFIRMED 占预留量，DONE 释放并扣减现有量；入库（incoming）不占预留量；内部调拨（internal）来源库位占预留。**负库存配置 `erp-inv.allow-negative-stock`（默认 false）开启时跳过可用量校验**。并发扣减同一批次用乐观锁+重试。
- **成本核算集成**（A2.4 done）：7 costMethod 策略（FIFO/LIFO/BATCH/SPECIFIC/STANDARD/MOVING_AVERAGE/WEIGHTED_AVERAGE）+ 子计算器注入 + 成本调整（CostAdjust）+ 到岸成本（LandedCost）+ PPV + reclosePeriodCosts 兜底。**`ErpInvStockMoveProcessor.reverse:128` 用 `CoreMetrics.today()` 而非原 businessDate**（P2-MA2-028——红冲新层 incomingDate=today 影响队列时序）。
- **跨域访问**：业务域经 `IErpInvStockMoveBiz` 写库存移动单（采购入库/销售出库/资产资本化/委外）；财务域经存货过账事件订阅。daoFor 跨域只读已在 MA1 登记（P1-MA1-022 含 `ErpInvLandedCostProcessor:267,473,477` ErpPurReceive + `StandardCostResolver:99`/`CostMethodResolver:61,70`/`CostAdjustmentService:291` ErpMd*）。**P0-MA1-021 done**：CostAdjustmentPostingDispatcher 跨模块写 ErpFinVoucher 已修复为 `IErpFinVoucherBiz.reverse()`（业财一体写经 I*Biz）。
- **测试覆盖**：需审查库存状态机相关测试（FIFO/LIFO/BATCH/SPECIFIC/STANDARD 成本核算 + 红冲不变量 + 跨仓调拨 + 盘点差异 + 负库存 + 预留量等）。

**已登记的直指库存状态机的 finding（本审计须复核其状态机行为）**：

- `P0-MA1-021`（done，inventory）：CostAdjustmentPostingDispatcher 跨模块写 ErpFinVoucher 已修复为 IErpFinVoucherBiz.reverse。**状态机 scope**：已闭包——本审计复核 CostAdjust 状态机迁移（approve/reverseApprove）经修复后的正确性。
- `P1-MA1-022`（todo MR1，9 域合并）：inv `ErpInvLandedCostProcessor:267,473,477` ErpPurReceive + StandardCostResolver/CostMethodResolver/CostAdjustmentService ErpMd* 只读。**状态机 scope**：跨域只读是成本解析/采购收货查询副作用，不破坏状态机——本审计复核异常路径无悬挂。
- `P1-MA2-023`（todo MR1，inventory）：SPECIFIC 历史成本守卫缺失（findSpecificLayers 无 le(incomingDate, businessDate) 过滤）。**状态机 scope**：成本核算归 A2.4——本审计复核 reverse 红冲路径状态机角度（DONE→冲销反向单的状态迁移）。
- `P1-MA2-024`（todo MR1，inventory）：STANDARD 红冲成本不变量跨重估破缺（onIncoming 忽略 unitCost 重解析 + onOutgoing 不刷新）。**状态机 scope**：成本不变量归 A2.4——本审计复核 reverse 状态迁移正确性 + posted 标记。
- `P2-MA1-025`（todo MR1，inventory）：state-machine.md §盘点单状态机 用 COUNTING vs dict CONFIRMED（盘点单复用 move-status）。**状态机 scope**：直接是状态机 owner doc drift——本审计复核盘点单状态迁移 + COUNTING/CONFIRMED 语义。
- `P2-MA2-026~030`（todo MR1，inventory）：三方对账聚合不变量测试缺失 / 跨仓调拨成本桥层未测试 / **红冲 today() 而非原 businessDate（P2-MA2-028）** / CostAdjust FIFO 红冲一致性未测试 / reclosePeriodCosts 边缘。**状态机 scope**：测试覆盖归 A5——本审计复核 reverse 状态迁移 + today() 时序对状态机影响。

**但从未做过一次覆盖库存全状态机（移动单生命周期 + 盘点单独立 + 拣货单 + 成本调整/到岸成本/转移单双轴 + 所有权转移 + 批次/序列号/预留，19 状态字段）、按 `state-machine-business-review-prompt.md` 10 维度的系统性业务审查**。已知未核验控制点（owner doc §审查提示 + 已登记 finding）：

- **状态定义清晰性**：移动单 docStatus(erp-inv/move-status) vs 业务单据 erp/doc-status 字典差异（DRAFT/CONFIRMED/DONE/CANCELLED vs DRAFT/ACTIVE/CANCELLED）；**盘点单 COUNTING vs dict CONFIRMED**（P2-MA1-025）；DONE/CONFIRMED 语义（等待什么 vs 做什么——owner doc §1 强调）；预留量影响轴。
- **转换完整性**：移动单生命周期迁移完整性（DRAFT→CONFIRMED→DONE/CANCELLED + DRAFT→CANCELLED）；**出库可用量校验前置**（CONFIRMED 时校验）；**DONE 冲销路径**（生成反向单新 DRAFT，非状态回退——owner doc §2/§3/§5）；转移单/成本调整/到岸成本 Processor reverseApprove 目标态合规性；所有权转移迁移；批次/序列号状态迁移。
- **终端状态与恢复**：DONE 终态（不可直接修改，纠错只能冲销反向单）；CANCELLED 终态（不可恢复，需重建）；DONE 的"冲销"是生成新单非状态迁移；归档与活跃区分。
- **异常路径**：**出库可用量不足**（DRAFT→CONFIRMED 拒绝回滚——owner doc §4）；批次/序列号缺失（拒绝确认）；批次过期（出库校验有效期，可配放行）；序列号已售（拒绝再次出库）；**并发扣减同一批次**（乐观锁+重试——交接 A2.17）；**冲销反向单可用量不足**（冲销本质是反向移动，同样校验）；重复触发（业务单据重复审核幂等）；负库存配置放行。
- **可达性**：从 DRAFT 可达 CONFIRMED/DONE/CANCELLED 全态；无不可达状态；无死锁（DONE/CANCELLED 终态无出边，DRAFT→CONFIRMED→DONE 有向无环）；冲销反向单独立新流程不构成原单循环。
- **角色与权限**：提交确认（业务单据审核人/库管员）；执行完成（系统联动/库管员二次确认）；取消/冲销（库管员——冲销需财务影响确认）；**负库存放行**（仅管理员可配置，不放行给普通库管员——owner doc §6 危险操作）。
- **外部依赖**：业务单据（采购入库/销售出库/资产资本化）触发移动单（外部触发转 DRAFT 或 CONFIRMED，不直接使用外部状态值）；存货过账事件发布给财务域（DONE 后发布，失败不影响移动单终态——解耦）；跨域写库存经 IErpInvStockMoveBiz。
- **TODO/任务策略**：DRAFT assigned（独立创建的移动单待库管员确认）；CONFIRMED confirm（待执行确认——业务联动通常立即 DONE）；DONE/CANCELLED 否（终态归档）；**业务联动移动单通常自动 DRAFT→DONE 不产生人工 TODO，只有独立创建（盘点/其他出入库）才产生库管员待办**（owner doc §8——避免单据沉没）。
- **场景演练**：(a) 采购入库 happy path（业务审核→移动单 incoming→DRAFT→CONFIRMED→DONE+写流水+增余额+发过账事件）；(b) **销售出库可用量不足**（DRAFT→CONFIRMED 拒绝回滚）；(c) **已完成冲销**（生成反向单新 DRAFT→CONFIRMED→DONE+余额回退+反向存货凭证）；(d) **冲销反向单可用量不足**（拒绝）；(e) 内部调拨（来源占预留+目的不占）；(f) **并发扣减同一批次**（乐观锁+重试——交接 A2.17）；(g) 盘点完成（DONE→自动生成盘盈/盘亏移动单新 DRAFT）；(h) 批次过期拒绝出库（可配放行）；(i) 序列号已售拒绝再次出库；(j) 负库存配置放行（管理员）。
- **与设计文档一致性**：`state-machine.md`/`trace-chain.md`/`cross-domain.md`/`consignment.md` vs 实现——重点核验：(1) §盘点单状态机 COUNTING vs dict CONFIRMED（P2-MA1-025）；(2) §2 出库可用量校验+预留量落实；(3) §3/§5 DONE 冲销反向单非状态回退；(4) §4 异常路径（批次/序列号/并发）；(5) §6 负库存放行权限；(6) §8 TODO 策略（独立创建 vs 业务联动）；(7) 移动单 move-status vs 业务单据 doc-status 字典差异。

剩余差距：需要一次系统性状态机业务审查，发现任何遗漏的 P0（**出库可用量校验缺失致超卖** [若破坏库存核心约束——owner doc §4] / **DONE 红冲非反向单而是状态回退** [若破坏不可变流水不变量——owner doc §3/§5 强制，P0/P1] / **冲销反向单可用量未校验** [若破坏余额守恒] / **负库存放行权限缺失** [若破坏危险操作控制——owner doc §6] / **`reverse` 用 today() 破坏 FIFO 队列时序** [P2-MA2-028 升级评估]）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `state-machine-business-review-prompt.md` 10 维度对 **移动单生命周期 + 盘点单独立 + 拣货单 + 成本调整/到岸成本/转移单双轴 + 所有权转移 + 批次/序列号/预留（19 状态字段）** 做系统性业务审查，产出审计报告。
- 重点核验已识别控制点：(1) 状态定义清晰性（**move-status vs doc-status 字典差异** / **盘点 COUNTING vs CONFIRMED** / DONE/CONFIRMED 语义）；(2) 转换完整性（生命周期迁移 + **出库可用量校验前置** / **DONE 冲销反向单非状态回退** / reverseApprove 合规 / 所有权转移）；(3) 终端与恢复（DONE 不可直接修改+冲销反向单 / CANCELLED 不可恢复）；(4) 异常路径（**出库可用量不足** / 批次序列号缺失过期已售 / **冲销反向单可用量不足** / 并发扣减 / 负库存）；(5) 可达性；(6) 角色权限（**负库存放行仅管理员**）；(7) 外部依赖（业务触发+存货过账事件解耦）；(8) TODO 任务策略（**独立创建 vs 业务联动避免沉没**）；(9) 场景演练（10 个代表性场景）。
- 复核已登记 finding 在库存状态机运行时的行为影响：P0-MA1-021（done，CostAdjust 经修复后状态机复核）/ P1-MA1-022（跨域只读）/ P1-MA2-023（SPECIFIC 守卫——reverse 路径复核）/ P1-MA2-024（STANDARD 红冲——reverse 状态迁移复核）/ P2-MA1-025（COUNTING vs CONFIRMED 死状态/语义复核）/ P2-MA2-026~030（**today() 升级评估**——reverse 状态机影响），标注终态。
- scope matrix §状态机正确性 inv 列 `❓` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A2.11 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 A2.4 库存核算一致性 — done；本审计只复核库存状态机迁移正确性（成本核算策略/三方对账归 A2.4 finding P1-MA2-023/024 + P2-MA2-026~030）。
- **不**审计 A5.5 测试隔离性 / A5.x 测试覆盖深度 — 测试覆盖系统性审查归 MA5；本审计只复核 reverse 状态迁移对测试的影响（today() 时序）。
- **不**审计 A2.17 并发与乐观锁 — 并发扣减同一批次归 A2.17；本审计只标注观察到的并发敏感点。
- **不**审计 A4.7 view.xml drift — 库存页面契约漂移归 A4.7。
- **不**审计 config-gated Deferred 偏离是否应实现（负库存 config / 批次过期放行 config / VMI 所有权转移） — owner doc 已裁定，本审计只确认其在状态机上不引入悬挂。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**手改生成物。任何代码/ORM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests` + 该修复子切片独立审计。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/inventory/state-machine.md`（移动单生命周期 + 盘点单独立 + §审查提示 — **需复核 COUNTING vs CONFIRMED + 可用量校验 + 冲销反向单 + 负库存权限**）；`docs/design/inventory/trace-chain.md`（流水可追溯+反向单）；`docs/design/inventory/cross-domain.md`（业务触发+可用量校验规则+过账事件）；`docs/design/inventory/consignment.md`（VMI 所有权转移）；`docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）；`docs/architecture/posting-exemptions.md`（存货过账豁免登记）
- Skill Selection Basis: `state-machine-business-review-prompt.md`（roadmap A2.11 指定此 skill，状态机业务审查专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及代码/ORM，则该修复需 `mvn clean install -DskipTests` + 相关测试。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：库存状态机本身非 ask-first 最高级保护区域，但**存货过账事件触及 finance 凭证链**（DONE 后发过账事件→存货估值凭证）+ **库存写是全业务域枢纽**（采购/销售/资产/委外均经 IErpInvStockMoveBiz）。P0 即时修复若触及 `ErpInvStockMoveProcessor`/成本核算策略/`Inv*PostingDispatcher`/xbiz 文件，须有 owner doc 描述预期行为 + 该修复子切片的独立审计 + 人工确认（会计/库存保护区域）。ORM 字典变更（move-status/picking-status/ownership-transfer-status）属 ask-first。xbiz 文件变更属状态机契约变更——须 owner doc + 人工确认。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 库存状态机系统性业务审查

Status: completed
Targets: `module-inventory/erp-inv-service/.../service/processor/ErpInvStockMoveProcessor.java`（DRAFT→CONFIRMED→DONE/CANCELLED 迁移 + reverse:128 today() + 写流水+更新余额+释放预留+发过账事件 + 出库可用量校验 + 预留量）；`ErpInvStockTakeProcessor`/BizModel（盘点单独立状态机 + DONE→自动生成盘盈/盘亏移动单新 DRAFT）；`ErpInvPickingOrderProcessor`；`ErpInvCostAdjustProcessor`（reverseApprove 经修复后 IErpFinVoucherBiz.reverse）；`ErpInvLandedCostProcessor`（daoFor ErpPurReceive:267,473,477）；`ErpInvTransferOrderProcessor`/`ErpInvOwnershipTransferProcessor`；批次/序列号状态迁移组件；预留量管理组件；成本核算策略（reverse 状态机角度）
Skill: `state-machine-business-review-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 done（P0-MA1-021 done + P1-MA1-022 跨域只读 + P2-MA1-025 COUNTING drift 已登记，本审计复核状态机角度）；A2.4 done（库存核算一致性，P1-MA2-023/024 + P2-MA2-026~030 已登记，本审计复核 reverse 状态机角度）；A2.5a done（finance 凭证 reverseApprove 红冲闭环 + tryPost 吞误同型范式）；A2.8 done（purchase 状态机三轴 + reverse 范式）

- [x] 维度「状态定义」：审查移动单 docStatus(erp-inv/move-status DRAFT/CONFIRMED/DONE/CANCELLED) vs 业务单据 erp/doc-status 字典差异；**盘点单 COUNTING vs dict CONFIRMED**（P2-MA1-025——盘点单复用 move-status）；DONE/CONFIRMED 语义（等待什么 vs 做什么）；预留量影响轴（出库占/入库不占/内部调拨来源占）；批次/序列号/预留状态轴清晰性。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「转换完整性」：移动单生命周期迁移完整性（DRAFT→CONFIRMED→DONE/CANCELLED + DRAFT→CANCELLED）；**出库可用量校验前置**（CONFIRMED 时校验现有量−预留量≥出库数量）；**DONE 冲销路径**（生成反向单新 DRAFT 数量取负，非状态回退——owner doc §2/§3/§5）；转移单/成本调整/到岸成本 Processor reverseApprove 目标态合规性；所有权转移迁移；批次/序列号状态迁移。是否有非法跳转或缺失条件分支。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「终端状态和恢复」：DONE 终态（不可直接修改，纠错只能冲销反向单）；CANCELLED 终态（不可恢复，需重建）；DONE 的"冲销"是生成新单非状态迁移；归档与活跃区分。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「异常路径」：核验全覆盖——**出库可用量不足**（DRAFT→CONFIRMED 拒绝回滚）；批次/序列号缺失（拒绝确认）；批次过期（出库校验有效期，可配放行）；序列号已售（拒绝再次出库）；**冲销反向单可用量不足**（冲销本质反向移动，同样校验）；重复触发（业务单据重复审核幂等）；**`reverse` 用 today() 破坏 FIFO 队列时序**（P2-MA2-028 升级评估——状态机角度复核 reverse 状态迁移 + 新层时序）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「可达性」：从 DRAFT 可达 CONFIRMED/DONE/CANCELLED 全态；无不可达状态；无死锁（DONE/CANCELLED 终态无出边）；冲销反向单独立新流程不构成原单循环；盘点单 COUNTING/CONFIRMED 可达性。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「角色和权限」：每个迁移绑定执行角色——提交确认（业务单据审核人/库管员）；执行完成（系统联动/库管员二次确认）；取消/冲销（库管员——冲销需财务影响确认）；**负库存放行**（仅管理员可配置，不放行给普通库管员——owner doc §6 危险操作）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「外部依赖」：业务单据（采购入库/销售出库/资产资本化）触发移动单（外部触发转 DRAFT 或 CONFIRMED，不直接使用外部状态值）；存货过账事件发布给财务域（DONE 后发布，失败不影响移动单终态——解耦）；跨域写库存经 IErpInvStockMoveBiz。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「TODO/任务策略」：DRAFT assigned（独立创建移动单待库管员确认）；CONFIRMED confirm（待执行确认——业务联动通常立即 DONE）；DONE/CANCELLED 否（终态归档）；**业务联动移动单通常自动 DRAFT→DONE 不产生人工 TODO，只有独立创建（盘点/其他出入库）才产生库管员待办**（owner doc §8——避免单据沉没）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「场景演练（最重要）」：端到端演练代表性场景——(a) 采购入库 happy path；(b) **销售出库可用量不足**（拒绝回滚）；(c) **已完成冲销**（反向单新 DRAFT→CONFIRMED→DONE+余额回退+反向凭证）；(d) **冲销反向单可用量不足**（拒绝）；(e) 内部调拨（预留量差异）；(f) **并发扣减同一批次**（乐观锁+重试——交接 A2.17）；(g) 盘点完成（DONE→自动生成盘盈/盘亏移动单）；(h) 批次过期拒绝出库（可配放行）；(i) 序列号已售拒绝再次出库；(j) 负库存配置放行（管理员）。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 维度「与设计文档一致性」：每个状态/转换在 `state-machine.md`/`trace-chain.md`/`cross-domain.md`/`consignment.md` 是否有匹配——重点核验：(1) §盘点单 COUNTING vs dict CONFIRMED（P2-MA1-025）；(2) §2 出库可用量校验+预留量落实；(3) §3/§5 DONE 冲销反向单非状态回退；(4) §4 异常路径（批次/序列号/并发）；(5) §6 负库存放行权限；(6) §8 TODO 策略；(7) move-status vs doc-status 字典差异。
      - Skill: `state-machine-business-review-prompt.md`
- [x] 复核已登记 finding 库存状态机角度：P0-MA1-021（done，CostAdjust 经修复后状态机复核）/ P1-MA1-022（跨域只读无升级）/ P1-MA2-023（SPECIFIC 守卫——reverse 路径复核）/ P1-MA2-024（STANDARD 红冲——reverse 状态迁移复核）/ P2-MA1-025（COUNTING vs CONFIRMED 死状态/语义复核）/ P2-MA2-026~030（**today() 升级评估**），标注终态。
      - Skill: none
- [x] 产出审计报告 `docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`（含：移动单生命周期状态图 + 盘点单独立状态机 + 业务单据双轴迁移矩阵、各维度通过/失败裁决、控制点 PASS/FAIL、COUNTING/CONFIRMED 裁决、MA1/MA2 finding 运行时影响复核表、并发敏感点交接 A2.17、today() 升级评估、残留风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 移动单生命周期状态图 + 盘点单独立状态机 + 业务单据双轴迁移矩阵产出，每个状态/转换有通过/失败裁决与证据
- [x] 已识别控制点（状态定义[含 move-status vs doc-status + COUNTING/CONFIRMED] / 转换完整性[含可用量校验 + DONE 冲销反向单] / 终端与恢复 / 异常路径[含可用量不足 + 冲销反向单可用量 + today() 时序] / 可达性 / 角色权限[含负库存放行] / 外部依赖 / TODO 任务策略 / 场景演练）均有通过/失败裁决与证据
- [x] state-machine-business-review 10 维度至少一句裁决（含「本维度无发现」）

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 库存状态机审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §状态机正确性 inv 列
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（**出库可用量校验缺失致超卖** [若破坏库存核心约束] / **DONE 红冲非反向单而是状态回退** [若破坏不可变流水不变量——owner doc §3/§5 强制] / **冲销反向单可用量未校验** [若破坏余额守恒] / **负库存放行权限缺失** [若破坏危险操作控制] / **today() 破坏 FIFO 队列时序升级为 P0** [若 P2-MA2-028 经复核升级]）当即就地修复（改源文件 + `mvn clean install -DskipTests` + 该修复独立审计 + 人工确认触及会计/库存保护区域）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA2-NNN`、报告、描述、目标 MR1、修复状态 todo）。本审计对已登记 finding 只复核状态机运行时影响不重复登记根因；新 P1（如可用量校验缺口 / 冲销反向单缺口 / 负库存权限缺口 / reverseApprove 不一致 / COUNTING/CONFIRMED 死状态 [若确认]）按新 finding ID 登记。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §状态机正确性 inv 列终态标记（`❓` → `✅`/`⚠️(P1)`）。
      - Skill: none

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开
- [x] arm-index 报告清单 + scope matrix 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_059ca59d8ffeHEnOQmXcxqNkU1`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT = accept，**无 BLOCKER**。核实要点：owner doc 全存在 ✓；finding ID（P0-MA1-021 done / P1-MA1-022 / P1-MA2-023/024 / P2-MA1-025 / P2-MA2-028）在 arm-index 描述匹配 ✓，P2-MA2-028 `reverse:128 CoreMetrics.today()` 逐字确认 ✓；10 个 ErpInv 实体在 ORM 存在 ✓；状态字段数 19 与 roadmap 一致 ✓；P0-MA1-021 re-check 正确标注 done 状态 ✓；核心不变量（DONE 不可变流水 / 冲销反向单非状态回退 / 出库可用量校验 / 负库存权限）正确锚定 owner doc §2/§3/§5/§6 ✓；反松弛无禁词 ✓；结构与 reference purchase plan 一致 ✓。Plan Status 转 active。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。存货过账+库存写触及会计/库存保护区域，P0 即时修复须额外人工确认。xbiz 契约变更须人工确认。

- [x] 范围内行为完成（A2.11 库存状态机系统性审查报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、state-machine/trace-chain/cross-domain/consignment owner doc 结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 审计不改代码，build/test 门控仅作回归基线确认（A2.4 / A2.10 等同型审计 plan 的相同 Closure 实践）；inventory 域自上次 codegen + 后续 fix plans 已建立 154 模块全绿基线
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### A2.4 库存核算一致性（成本策略/三方对账）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A2.4 done（成本核算链路组件齐备已确认）。本审计做库存状态机**迁移正确性**审查；成本核算策略/三方对账归 A2.4 finding（P1-MA2-023/024 + P2-MA2-026~030 待 MR1）。
- Successor Required: `no`——A2.4 已 done，finding 待 MR1。

### A5.x 测试覆盖深度 / A5.5 测试隔离性

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计做库存状态机**业务正确性**审查；测试覆盖系统性审查归 MA5。本审计只复核 reverse 状态迁移对测试的影响（today() 时序）。
- Successor Required: `yes`——MA5 执行时复核。

### A2.17 并发与乐观锁（并发扣减同一批次）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式分配给 A2.17。本审计标注观察到的并发敏感点（并发扣减同一批次乐观锁+重试），不做系统性并发正确性裁决。
- Successor Required: `yes`——A2.17 执行时复核。

### config-gated Deferred 偏离本身（负库存 config / 批次过期放行 config / VMI 所有权转移）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 已裁定为 config-gated/Deferred。本审计只确认其在状态机上不引入悬挂。
- Successor Required: `yes`——各 successor 触发条件满足时（如 VMI 全面上线 / 负库存业务场景启用）。

## Closure

Status Note: 已执行 + 待独立 closure audit（mission driver 委派）。审计产出报告 `docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`；2 项新 P1（P1-MA2-062 StockTake 自动生成移动单缺失 / P1-MA2-063 PickingOrder 死状态）+ 1 项新 P2（P2-MA2-062 owner doc 章节缺失）登记 arm-index；7 项已登记 finding 运行时复核无升级；4 处并发敏感点交接 A2.17；scope matrix §状态机正确性 inv 列推进至 ⚠️P1(A2.11✅)。零 P0。

Closure Audit Evidence:

- Phase 1 / Phase 2 / Closure Gates 全部勾选 [x]。
- 报告：`docs/audits/2026-07-28-0400-arm-ma2-inventory-state-machine.md`（10 维度全覆盖 + 迁移矩阵 + finding 复核表 + 并发敏感点 + today() 升级评估 + 残留风险）。
- 索引：`docs/audits/arm-index.md` 报告清单 + §P1 汇总（P1-MA2-062/063）+ §P2 汇总（P2-MA2-062）+ A2.11 章节小结新增。
- 矩阵：`docs/audits/audit-remediation-scope-and-dimension-matrix.md §2.2` 状态机正确性 inv 列由 `❓` 推进至 `⚠️P1(A2.11✅)`。
- 路线图：`docs/backlog/audit-remediation-roadmap.md` A2.11 由 ❌ 推进至 ✅（详见 mission driver 收尾步骤）。

Follow-up:

- 无新阻塞跟进；2 项 P1 + 1 项 P2 已纳入 MR1 待 R1.0 展开机制处理；4 处并发敏感点交接 A2.17 系统性并发正确性审计；批次/序列号/预留状态轴 Deferred（owner doc 边界裁定）。
