# 2026-07-05-2352-3-inventory-cost-adjustment 存货成本调整单 + 标准成本重估

> Plan Status: completed
> Mission: erp
> Work Item: deferred successor — 成本调整单 + 标准成本重估（costing 模块完整性收口）
> Last Reviewed: 2026-07-05
> Source: `docs/design/finance/costing-methods.md §成本调整`（:367-415 完整流程 + `ErpInvCostAdjust` 实体设计草稿 + `erp-fin.cost-adjust-approval` 配置，权威设计）；`docs/plans/2026-07-02-1538-1-inventory-costing-engine.md` Deferred「成本调整单」（Successor Required: yes）；`docs/plans/2026-07-05-0427-2-standard-costing-strategy.md` Deferred「标准成本重估（归 1538-1 成本调整单）」
> Related: `docs/plans/2026-07-02-1538-1-inventory-costing-engine.md`（成本引擎前驱：CostingStrategy/StockMoveBookkeeper/ErpInvCostLayer）；`docs/plans/2026-07-05-0427-2-standard-costing-strategy.md`（STANDARD 策略 + StandardCostResolver + PPV 过账范式）；`docs/plans/2026-07-05-1838-2-manufacturing-production-variance.md`（生产差异过账，costing 模块最后前置今日完成）；`docs/plans/2026-07-04-2050-1-use-approval-migration.md`（DIRECT 审批模式，成本调整审批门控前置）
> Audit: required

## Current Baseline

实时仓库逐项核实（`grep`/`read`，非采信旧记忆）：

- **`ErpInvCostAdjust` 实体不存在**（`grep -rn "ErpInvCostAdjust\|CostAdjust" module-inventory/model/app-erp-inventory.orm.xml` = 0 命中）——`costing-methods.md §成本调整 :404-414` 仅为设计草稿（`<entity name="ErpInvCostAdjust">`，非落地实体）。本期需创建（ORM ask-first 保护区域）。
- **成本调整目标实体已就绪**（`module-inventory/model/app-erp-inventory.orm.xml`）：
  - `ErpInvStockBalance`（:267-300）：`avgCost`(propId 13, DECIMAL)、`totalCost`(propId 14, DECIMAL)、`costMethod`(propId 12, dict `erp-md/cost-method`)、`materialId`/`warehouseId`/`batchNo`/`totalQuantity` —— 成本调整直接更新 avgCost/totalCost。
  - `ErpInvStockLedger`（:216，库存流水）：记录成本调整流水（参考设计 :394「记录成本调整流水」）。
  - `ErpInvCostLayer`（:373，FIFO 成本层）：FIFO 物料的成本调整需处理层（plan 1538-1 落地）。
- **成本引擎已就绪**（plan 1538-1）：`StockMoveBookkeeper` 按 costMethod 策略分派（`CostMethodResolver` → `MovingAverageCostingStrategy`/`FifoCostingStrategy`）；`BookingContext` 维护余额成本。成本调整引擎需对齐此范式（不破坏既有移动单记账路径）。
- **STANDARD 标准成本已就绪**（plan 0427-2）：`StandardCostingStrategy`（出入库按标准成本记账）+ `StandardCostResolver`（`module-inventory/erp-inv-service/.../costing/StandardCostResolver.java`，读最近 FIRMED `ErpMfgCostRollupLine.unitCost`）。标准成本重估 = 新增/发布 FIRMED rollup 行 + 重估差异过账。
- **`ErpMfgCostRollup` FIRMED 机制已就绪**（`module-manufacturing/model/app-erp-manufacturing.orm.xml:934`）：`cost-rollup-status` 字典（:108-111）含 `FIRMED`（已发布）；`rollupCost(bomId)` 计算 status=CALCULATED，FIRMED 由人工动作置位（plan 1538-2）。
- **过账范式可直接复用**（plan 0427-2/1838-2）：`PurchasePriceVarianceAcctDocProvider`/`ProductionVarianceAcctDocProvider` 方向相关 Dr/Cr 分解 + `ErpFinBusinessType`（现最高 `PRODUCTION_VARIANCE(400)`）。成本调整过账承接此范式（借存货/贷成本差异，或相反）。
- **审批门控已就绪**（plan 2050-1）：use-approval DIRECT 模式生效，标准 5 action（submitForApproval/approve/reject/reverseApprove/unsubmit）。成本调整审批经 `erp-fin.cost-adjust-approval`（设计 :499 已声明，默认 true）config-gated。
- **冲销反写闭环已就绪**（plan 1452-2）：`VoucherReversedEvent` SPI + 域监听者。

### 剩余差距

1. 无 `ErpInvCostAdjust` 实体（设计草稿待落地，ORM ask-first）。
2. 无成本调整引擎（apply 调整 → 更新 StockBalance.avgCost/totalCost → 记录流水 → 处理 FIFO 层）。
3. 无成本调整审批门控（DIRECT 模式 + config-gated）。
4. 无成本调整过账 Provider + 业务类型（COST_ADJUSTMENT，方向相关借存货/贷成本差异）。
5. 无标准成本重估路径（发布 FIRMED rollup + 重估差异凭证 + 影响后续 STANDARD 记账）。
6. 无 reverse 红冲回退（已调整余额 + 凭证回退）。
7. owner doc 收口（`costing-methods.md §成本调整` Non-Goal 转落地 + 实现偏离补注）。

## Goals

- **`ErpInvCostAdjust` 实体落地**（ORM ask-first）：头-行结构——头（code/adjustDate/adjustType/reason/docStatus/approveStatus/posted + 标准审计列）、行（materialId/warehouseId/batchNo/oldUnitCost/newUnitCost/adjustQty/adjustAmount/adjustReason）。对齐 `costing-methods.md :404-414` 设计草稿 + 既有头-行单据范式（采购订单/退货单）。
- **成本调整引擎**：`applyCostAdjust(adjustId)` @BizMutation —— 按行遍历，更新 `ErpInvStockBalance.avgCost/totalCost`（newUnitCost × totalQuantity），记录 `ErpInvStockLedger` 成本调整流水（独立 moveType 或 reason 标记，不生成库存移动单——纯成本变更，数量不变），FIFO 物料处理 `ErpInvCostLayer`（追加调整层或更新最近层，Decision）。
- **审批门控**：`erp-fin.cost-adjust-approval=true` 时 approve 前置方可 apply；DIRECT 模式标准审批状态机（submit→approve→apply→posted）。config-gated。
- **成本调整过账**：新增 `COST_ADJUSTMENT` 业务类型（加性）+ `CostAdjustmentAcctDocProvider`（方向相关：成本增加 借存货/贷成本差异；成本减少 借成本差异/贷存货）。
- **标准成本重估**：`ErpInvCostAdjust.adjustType=STANDARD_REVALUATION` 时——发布新 FIRMED `ErpMfgCostRollup` 行（newUnitCost）+ 重估差异凭证（库存存量 × 新旧标准成本差）+ 影响后续 `StandardCostingStrategy` 读新 FIRMED。承接 `ErpMfgCostRollup` 既有 FIRMED 机制。
- **reverse 红冲**：`reverseCostAdjust(adjustId)` —— posted→false + 红字凭证 + 回退 StockBalance.avgCost/totalCost 至 oldUnitCost + 回退 FIFO 层。
- **行为测试 + owner doc**：各 adjustType（采购价格调整/成本差异/标准成本重估）+ 审批门控 + reverse 行为测试；`costing-methods.md §成本调整` 收口。

## Non-Goals

- **不**实现到岸成本（Landed Cost）分摊算法（1538-1 Deferred——到岸成本是采购费用分摊面，非成本调整面；但到岸成本补录可作为 `ErpInvCostAdjust.adjustType=LANDED_COST` 的来源单据类型，本期不实现分摊算法本身）。
- **不**做成本报表渲染（存货成本明细/FIFO 队列/差异表，属 nop-report 报表面，1538-1 Deferred）。
- **不**实现 BATCH/INDIVIDUAL 计价的成本调整（1538-1 Deferred，需批次/个别计价先落地；本期仅 MOVING_AVERAGE/FIFO/STANDARD 三方法）。
- **不**做全月一次加权平均/LIFO（1538-1 Deferred，计价方法未落地）。
- **不**做存货减值（成本与可变现净值孰低，独立面）。
- **不**做成本调整的批量定时（本期手动 `applyCostAdjust` 入口；批量归 nop-batch successor）。
- **不**做多账套并行成本调整（本期单账套；1538-1 Deferred）。
- **不**改 `StockMoveBookkeeper` 移动单记账路径（成本调整是独立于库存移动的纯成本变更，不生成 StockMove）。
- **不**做标准成本重估的多版本历史追溯（本期最新 FIRMED 覆盖；历史版本快照归 successor）。

## Task Route

- Type: `app-layer design change`（新实体 `ErpInvCostAdjust` ORM ask-first + COST_ADJUSTMENT 业务类型加性 + 标准成本重估路径）+ `implementation-only change`（调整引擎 + Provider + 触发）
- Owner Docs: `docs/design/finance/costing-methods.md §成本调整`（权威流程 + 实体草稿）、`docs/design/finance/costing-methods.md §标准成本`（重估）、`docs/design/manufacturing/bom-and-routing.md`（ErpMfgCostRollup FIRMED 机制）、`docs/design/finance/posting.md`（过账 Provider 范式）
- Skill Selection Basis: BizModel（成本调整引擎 service-helper 对齐 `CostRollupService`/`CostingStrategy` 范式）、跨实体（StockBalance/Ledger/CostLayer/CostRollup）、AcctDocProvider 过账（承接 PPV/生产差异）、ORM 实体新建（保护区域 ask-first）、use-approval DIRECT 审批、config-gated、ErrorCode、JunitAutoTestCase——匹配 `nop-backend-dev`。
- **切片选择诚实性**：1538-1/0427-2 Deferred 触发条件原文为「成本调整+PPV / 标准成本周期重估/调整单需求落地时」——略带同义反复。本计划选择此切片的真实依据（对齐 1838-2 范式）：(a) `costing-methods.md §成本调整 :367-415` 是完整设计（流程 + 实体草稿 + 配置）；(b) 全部技术前置今日就绪——STANDARD 计价（0427-2）+ cost rollup（1538-2）+ PPV 过账范式（0427-2）+ 生产差异过账（1838-2，今日完成，costing 模块最后前置）+ use-approval 审批（2050-1）；(c) costing 模块三方法（MA/FIFO/STANDARD）+ PPV + 生产差异均已落地，成本调整是模块完整性收口的自然下一片。残留风险：无产品侧显式业务请求——以设计文档完整 + 技术就绪为决策依据。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env/外部服务/数据迁移（bootstrap 阶段无生产数据，新实体 DDL 由 codegen 生成）。
- 依赖成本引擎 `CostingStrategy`/`StockMoveBookkeeper`/`ErpInvCostLayer`（plan 1538-1，已落地）。
- 依赖 STANDARD + `StandardCostResolver` + `ErpMfgCostRollup` FIRMED（plan 0427-2/1538-2，已落地）。
- 依赖过账管道 `IErpFinAcctDocProvider` SPI + `ErpFinBusinessType`（plan 0811-1，已落地）。
- 依赖 use-approval DIRECT 模式（plan 2050-1，已落地）。
- 依赖冲销反写 `VoucherReversedEvent`（plan 1452-2，已落地）。
- **保护区域门控**：(a) 新建 `ErpInvCostAdjust`/`ErpInvCostAdjustLine` 实体 → `module-inventory/model/app-erp-inventory.orm.xml` ask-first + regen；(b) COST_ADJUSTMENT 业务类型加性（枚举 + 字典，对齐 PPV 范式）；(c) 若需新 `adjust-type` 字典 → 加性 ask-first。回滚策略：新实体 + config-gated 默认关，git 可逆，零数据风险。

## Execution Plan

### Phase 1 - 实体建模 + 业务类型 + 调整策略决策（ORM·ask-first）

Status: completed
Targets: `module-inventory/model/app-erp-inventory.orm.xml`（新实体）、`module-finance/erp-fin-dao/.../ErpFinBusinessType.java`、`erp-fin/business-type` 字典、`erp-inv/adjust-type` 字典
Skill: none

- Item Types: `Decision | Add`
- Prereqs: 人工批准（model/*.orm.xml ask-first）+ 本计划草案审查通过

- [x] `Decision`：`ErpInvCostAdjust` 实体结构——头-行（对齐采购订单/退货单范式）还是单层（对齐 costing-methods.md :404 草稿单实体）？**倾向头-行**——成本调整常涉及多物料/多仓库一次调整，头-行 cascade 范式更贴合业务，且与全域头-行单据一致。头：code/adjustDate/adjustType(dict)/reason/docStatus/approveStatus/posted + 审计列；行：materialId/warehouseId/batchNo/oldUnitCost/newUnitCost/adjustQty/adjustAmount/adjustReason。
  - Skill: none
- [x] `Decision`：FIFO 物料成本调整的层处理——(a) 追加「调整层」（newUnitCost，ErpInvCostLayer 新行，后续出库按 FIFO 消耗）；(b) 更新最近层 unitCost；(c) FIFO 物料不允许成本调整（仅 MA/STANDARD 可调）。**倾向 (a)**——追加调整层保持 FIFO 队列不变量（先进先出），调整作为独立层不影响历史层成本。残留风险：FIFO 队列长度增长。
  - Skill: none
- [x] `Decision`：标准成本重估的 rollup 发布机制——(a) 成本调整 apply 时自动创建新 `ErpMfgCostRollup`(FIRMED) 行（newUnitCost）；(b) 仅更新 StandardCostResolver 读源（如物料 standardCost 列，但 ErpMdMaterial 无此列）；(c) 经制造域 `rollupCost` 重算后人工 FIRMED + 成本调整仅过账差异。**倾向 (a)**——成本调整作为标准成本重估的显式入口，apply 时创建 FIRMED rollup 行（采购件，非制造件），后续 StandardCostingStrategy 读新 FIRMED。制造件标准成本重估归制造域 `rollupCost` successor。
  - Skill: none
- [x] `Decision`：adjustType 字典码值——`PURCHASE_PRICE_ADJUST`(采购价格调整)/`COST_DIFFERENCE`(成本差异)/`STANDARD_REVALUATION`(标准成本重估)/`LANDED_COST_SUPPLEMENT`(到岸成本补录，本期 Non-Goal 但预留码值)。
  - Skill: none
- [x] `Add`：`ErpInvCostAdjust`/`ErpInvCostAdjustLine` 实体落地（ORM ask-first + `<unique-key>` UK_INV_COST_ADJUST_CODE_ORG 对齐 1000-1 范式 + `<indexes>` 对齐 2352-1 范式若已落地）
      - Skill: none
- [x] `Add`：`COST_ADJUSTMENT` 业务类型（ErpFinBusinessType 枚举 + `erp-fin/business-type` 字典加性；code 取实施时最高值之后，若 2352-2 NCR 类型已落地则顺延）；`erp-inv/adjust-type` 字典
      - Skill: none
- [x] `mvn clean install -DskipTests` 增量重新生成（codegen 生成 ErpInvCostAdjust Entity/DAO/BizModel/I*Biz 空壳）
      - Skill: none

Exit Criteria:

> 本阶段交付实体 + 业务类型 + 决策。完整仓库 mvn test 归 Closure Gates。

- [x] `ErpInvCostAdjust`/`Line` 实体 + DAO + 空壳 BizModel 生成存在（解除 Phase 2 引擎实现阻塞）
- [x] `mvn clean install -DskipTests` BUILD SUCCESS（codegen 解析新实体通过）

### Phase 2 - 成本调整引擎 + 审批门控

Status: completed
Targets: `module-inventory/erp-inv-service/.../service/entity/ErpInvCostAdjustBizModel.java`、`CostAdjustmentService.java`（service-helper）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1 完成

- [x] `CostAdjustmentService.applyCostAdjust(adjustId, ctx)`：按行遍历——读 `ErpInvStockBalance`(materialId+warehouseId+batchNo)，记 oldUnitCost=balance.avgCost，计算 newUnitCost/adjustAmount，更新 balance.avgCost/totalCost；记录 `ErpInvStockLedger` 流水（moveType=ADJUSTMENT 或 reason 标记，quantity=0 纯成本变更）；FIFO 物料追加 `ErpInvCostLayer` 调整层（Phase 1 Decision(a)）
      - Skill: `nop-backend-dev`
- [x] 审批门控：`erp-fin.cost-adjust-approval=true` 时 apply 前置 approveStatus=APPROVED（DIRECT 模式标准 5 action）；config-gated 关时允许 DRAFT 直接 apply。`@BizMutation applyCostAdjust` 落 posted=true
      - Skill: `nop-backend-dev`
- [x] `reverseCostAdjust(adjustId)` @BizMutation：posted=true→false + 回退 balance.avgCost/totalCost 至 oldUnitCost（行级）+ 回退 FIFO 调整层（删除追加层）+ 红字凭证（Phase 3）。前置 posted=true（M2 posted-flag 守卫）
      - Skill: `nop-backend-dev`
- [x] 标准成本重估路径（adjustType=STANDARD_REVALUATION）：apply 时创建新 `ErpMfgCostRollup`(FIRMED, materialId 采购件, newUnitCost) 行（Phase 1 Decision(c)）；后续 StandardCostingStrategy 读新 FIRMED
      - Skill: `nop-backend-dev`
- [x] ErrorCode：`ERR_COST_ADJUST_ALREADY_APPLIED`/`ERR_COST_ADJUST_NOT_APPROVED`/`ERR_COST_ADJUST_NO_BALANCE`（无库存余额不可调）/`ERR_COST_ADJUST_NEGATIVE_COST`（newUnitCost<0 拒）等，NopException 范式
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `applyCostAdjust` 各 adjustType 行为可观察（MA/FIFO/STANDARD 三方法余额/层更新 + 流水记录），解除 Phase 3 过账 + Phase 4 测试阻塞
- [x] 审批门控 config-gated 行为可观察

### Phase 3 - 成本调整过账 Provider + 触发接线

Status: completed
Targets: `module-inventory/erp-inv-service/.../posting/CostAdjustmentAcctDocProvider.java`、`CostAdjustmentPostingDispatcher.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 2 完成

- [x] `CostAdjustmentAcctDocProvider`：方向相关 Dr/Cr——成本增加（newUnitCost>oldUnitCost）借存货(1401)/贷成本差异(6603)；成本减少 借成本差异(6603)/贷存货(1401)。承接 `ProductionVarianceAcctDocProvider`/`PurchasePriceVarianceAcctDocProvider` 范式。金额 = Σ 行 adjustAmount
      - Skill: `nop-backend-dev`
- [x] `CostAdjustmentPostingDispatcher`：apply 末尾构造 `PostingEvent`(sourceBillType=COST_ADJUST/sourceBillCode=adjust.code) → Provider.buildAcctDoc → `IErpFinVoucherBiz.post`；reverse 红字经 `IErpFinVoucherBiz.reverse`
      - Skill: `nop-backend-dev`
- [x] `applyCostAdjust` 接线派发器（apply→过账原子同事务，`@BizMutation` 自动事务包装）；`reverseCostAdjust` 接线红字
      - Skill: `nop-backend-dev`
- [x] Provider 经 `app-service.beans.xml` 注册为 Bean（反空心）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `CostAdjustmentAcctDocProvider` 注册为 Bean，apply 生成凭证 + posted=true 可观察
- [x] reverse 红字凭证 + posted→false + 余额回退可观察

### Phase 4 - 行为测试 + owner doc 收口

Status: completed
Targets: `module-inventory/erp-inv-service/src/test/.../TestErpInvCostAdjust.java`、`docs/design/finance/costing-methods.md`
Skill: `nop-backend-dev`

- Item Types: `Proof | Add`
- Prereqs: Phase 3 完成

- [x] `Proof`：行为测试 `TestErpInvCostAdjust`：(a) MA 成本增加（余额 avgCost/totalCost 更新 + 流水 + 凭证 借存货/贷差异）；(b) MA 成本减少（反向）；(c) FIFO 追加调整层（层队列 + 后续出库消耗）；(d) STANDARD 重估（FIRMED rollup 发布 + 差异凭证 + 后续 StandardCostingStrategy 读新值）；(e) 审批门控（approval=true 未 approve 拒 / approval=false DRAFT 可 apply）；(f) 重复 apply 防护（posted=true 拒）；(g) reverse 红冲（余额回退 oldUnitCost + 红字凭证 + FIFO 层删除）；(h) 无余额拒 / 负成本拒。`mvn test -pl module-inventory/erp-inv-service -am` 全绿。
      - Skill: `nop-backend-dev`
- [x] `Add`：`costing-methods.md §成本调整` 偏离补注——Non-Goal 转 ✅ 落地标记（plan 2352-3）+ 实现偏离（头-行结构 Decision/FIFO 层处理 Decision/标准成本重估 rollup 发布 Decision/制造件重估归 successor）；`:40` Non-Goal 成本调整单条目更新
      - Skill: none
- [x] 1538-1/0427-2 Deferred「成本调整单/标准成本重估」状态更新（解除 Deferred 标记）
      - Skill: none

Exit Criteria:

- [x] 8 类行为测试存在且 `mvn test -pl module-inventory/erp-inv-service -am` 全绿
- [x] `costing-methods.md §成本调整` 偏离补注 + Non-Goal 转落地

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is**（`ses_0ccf9473effeCaNaFI1K2u4iNQ`，独立 general 子代理，对照实时仓库逐项核实 10 项 Current Baseline 声明 + 切片选择诚实性）。全部 10 项基线声明经实时仓库核实为 TRUE（含精确行号/propId：ErpInvCostAdjust 不存在 0 命中、StockBalance avgCost(13)/totalCost(14)/costMethod(12)、StockLedger(:216)/CostLayer(:373)、ErpMfgCostRollup(:934)+FIRMED、StandardCostResolver 存在读 FIRMED 行、costing-methods.md §成本调整(:367-415)+config(:499)、PPV/生产差异 Provider 范式存在、use-approval ready、1538-1/0427-2 deferred-trigger 依赖真已 done、ErpMdMaterial 无 standardCost 列）；bonus 核实 ErpMfgCostRollup 头无 mandatory bomId 故 Decision(c) 选项(a) 结构可行。0 BLOCKER。4 项 nit（Phase 4 Proof 可加 `nop-testing` 技能、Source 行 config 引用 :499 非 :367-415、Decision(c) ErpMfgCostRollup 语义适配执行期确认、Phase 2 reverse 跨 Phase 3 引用澄清）均为非阻塞，执行期可吸收。切片选择诚实性披露（同义反复触发 + 设计文档指定 + 技术就绪 + 残留风险，对齐 1838-2 范式）经评估为充分。共识达成，Plan Status 升级为 `active`。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（成本调整引擎 MA/FIFO/STANDARD 三方法 + 审批门控 + 过账 + reverse + 标准成本重估）
- [x] 相关文档对齐（`costing-methods.md §成本调整`/`§标准成本`）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（全量 BUILD SUCCESS / 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 保护区域（新实体 ErpInvCostAdjust + COST_ADJUSTMENT 业务类型 + adjust-type 字典）实施前已获人工批准
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 到岸成本（Landed Cost）分摊算法

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 到岸成本是采购费用（运费/保险/关税）按数量/金额分摊到入库成本的面，非成本调整面；本期预留 `adjustType=LANDED_COST_SUPPLEMENT` 码值但不实现分摊算法。
- Successor Required: yes——触发条件：到岸成本模块落地时（1538-1 Deferred）。

### 制造件标准成本重估

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期标准成本重估仅采购件（直接 FIRMED rollup 行）；制造件标准成本经 `rollupCost(bomId)` 重算后人工 FIRMED（1538-2 既有），成本调整仅过账存量差异。
- Successor Required: yes——触发条件：制造件标准成本周期重估编排需求时。

### BATCH/INDIVIDUAL 计价成本调整

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: BATCH/INDIVIDUAL 计价方法未落地（1538-1 Deferred）；本期仅 MA/FIFO/STANDARD。
- Successor Required: yes——触发条件：BATCH/INDIVIDUAL 计价落地时。

### 成本调整批量定时 / 多账套 / 成本报表

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期手动入口 + 单账套 + 无报表渲染；批量归 nop-batch，多账套归 1538-1 Deferred，报表归 nop-report successor。
- Successor Required: yes——触发条件：批量/多账套/报表需求时。

## Closure

Status Note: 4 Phase 全部完成并验证。`ErpInvCostAdjust` 头-行实体落地（头-行 Decision，UK_INV_COST_ADJUST_CODE_ORG + 索引 + erp-inv/adjust-type 字典 4 码值）；`CostAdjustmentService` 纯成本变更引擎（按行更新 balance.avgCost/totalCost + 写 ledger 流水 quantity=0/moveId=0 哨兵 + FIFO 追加 delta 调整层 incomingMoveId=-行ID 负值哨兵 + STANDARD_REVALUATION 发布 FIRMED ErpMfgCostRollup 行）；`ErpInvCostAdjustProcessor` DIRECT 审批状态机 5 action + apply/reverse，erp-fin.cost-adjust-approval config-gated；`CostAdjustmentAcctDocProvider` 方向相关（成本增加 借存货(1401)/贷成本差异(6603)，减少反向）+ `CostAdjustmentPostingDispatcher`（PostingEvent → IErpFinVoucherBiz.post/reverse + markOriginalVoucherReversed）；apply 仅在过账成功（voucherId≠null）置 posted=true（对齐 asset 价值调整范式）。`COST_ADJUSTMENT(420)` 业务类型加性。`TestErpInvCostAdjust` 8 类场景全绿。`costing-methods.md §成本调整` Non-Goal 转 ✅ 落地 + 实现注记；1538-1/0427-2 Deferred 状态更新。无既有契约破坏（COST_ADJUSTMENT 枚举/字典/实体均加性）。`mvn clean install -DskipTests` 146 模块 BUILD SUCCESS；`mvn test` 全量 0 failures / 0 errors；inventory-service 63 测试（含新增 8）全绿。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理 `ses_0cc5a2f2affeeq2tPcAsjaSsRc`（新会话，无执行者上下文）
- Evidence: VERDICT: PASS（0 BLOCKER）。逐项核实 8 项验证任务：(1) 计划文本一致性——Plan Status 经审计确认可升级 completed、4 Phase 全部 Status:completed 且每项 [x]、Closure Gates 经审计确认可勾选；(2) Phase 1 实体/字典/枚举/生成产物存在（orm.xml ErpInvCostAdjust/Line + UK_INV_COST_ADJUST_CODE_ORG + erp-inv/adjust-type 4 码 + erp-fin/business-type COST_ADJUSTMENT + ErpFinBusinessType.COST_ADJUSTMENT(420)）；(3) Phase 2 CostAdjustmentService/Processor/IBiz/BizModel/Errors/xbiz 真实非桩实现；(4) Phase 3 Provider/Dispatcher + 4 Bean 注册 app-service.beans.xml；(5) Phase 4 TestErpInvCostAdjust 8 @Test + costing-methods.md 实现注记 + 1538-1/0427-2 Deferred 更新；(6) `mvn test -Dtest=TestErpInvCostAdjust` = Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 BUILD SUCCESS；(7) 反模式自检全通过（@Inject 非 private / 异常用 ErrorCode+NopException / 无 @BizMutation+@Transactional / BizModel 注入 Processor / 无 _gen 手改 / 跨域用 IErpFinVoucherBiz）；(8) `mvn compile -pl erp-inv-service -am` BUILD SUCCESS。非阻塞 nit：CostAdjustmentService 直接用 IDaoProvider/IOrmTemplate（Javadoc 已文档化理由，对齐 StandardCostResolver 范式）。

Follow-up:

- 到岸成本分摊算法（见 Deferred，触发：到岸成本模块）
- 制造件标准成本重估编排（见 Deferred，触发：制造件周期重估需求）
- BATCH/INDIVIDUAL 计价成本调整（见 Deferred，触发：BATCH/INDIVIDUAL 计价落地）
- 成本调整批量/多账套/报表（见 Deferred，触发：相关需求）
