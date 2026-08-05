# rc-ma3-a3-2-mfg-inv-pur-successor-review mfg+inventory+purchase MA3 successor 追踪完整性与回队复查报告（A3.2）

> Plan Status: completed
> 产出时间：2026-08-06
> 来源 Plan：`docs/plans/2026-08-06-0442-2-rc-ma3-a3-2-mfg-inv-pur-successor-review.md`（Work Item A3.2）
> Mission：requirement-compliance（MA3 successor 触发条件复查）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§4 三判据 / §5 Q4 + 保护区域 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 + §MA2↔MA3 协作）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（A3.2 mfg+inv+pur 域 successor 复查 + Work Item Details MA3）
> 复查全集：`docs/audits/rc-existing-inventory.md`（§successor 三源对账清单 mfg+inv+pur 域分组 — 7 项 + §对账差异登记 #3/#6）
> Skill：`docs/skills/open-ended-audit-prompt.md`
> 审计性质：**只读审计**——读 arm-index / owner doc / backlog README / 实仓代码 / config / SPI 裁决 successor 触发条件，**不修改任何代码/ORM/api.xml/真相源**

---

## §复查口径与 Q4 修复义务边界

本报告复查对象 = M0.3（`rc-existing-inventory.md` §successor 三源对账清单）导出的 mfg+inventory+purchase 域 design-level successor 去重并集 **7 项**。逐项完成方法论 §MA3 四任务：① 触发条件是否已满足（grep 实仓代码/config/SPI 验证）；② 是否该回队（已满足→回队 MR1 R1.0；未满足→维持 backlog successor）；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

**Q4 修复义务边界（§5）**：successor 触发条件**已满足**者须回队 MR1（R1.0 展开为 RC-R1.n，Q4 强制实现禁方案 B）；触发条件**未满足**者维持 backlog successor 登记（不强制实现，待触发）。本复查 7 项 successor 触发条件**全部未满足**（经 grep 实仓代码/config/SPI 逐项核实），故 7 项**全部维持 backlog successor**，无回队 MR1 项。

**finding 路由 vs successor 触发条件路由（防执行者混淆）**：本 A3.x 只裁决 **successor 触发条件**是否回队，不重审方案 B 关闭裁决本身（属 A2.x，已由 `2026-08-06-0442-1-rc-ma2-a2-3-8-9-governance-exemption-simplification-review.md` 收口 A2.3/A2.8/A2.9）。successor 回队与否（A3.x）≠ finding 是否修复（A2.x→MR1），两者各自裁决、交叉引用不冲突。两项两面关系：#2（委外收敛）↔ `P1-MA2-038`[A2.3] 同一 finding 两面——关闭裁决归 A2.3，successor 触发条件归本 A3.2；#6/#7（md 迁移）↔ `P1-MA1-022`[A2.9] 同一 finding 两面——同理。

**A3.1 跨域 deferred 项接入**：A3.1 Follow-up「#1 GRNI 自动冲回跨域依赖 inventory `repostPurchaseInput` SPI（归 A3.2 复查范畴）」——本复查已 grep `module-inventory` `repostPurchaseInput` SPI，**仍零命中**（SPI 缺失），结论记入 §9 与 A3.1 交叉引用（GRNI successor=未满足→维持 backlog；inventory SPI 落地是 GRNI 回队的前置，归本 A3.2 inventory 侧判定）。

---

## 1. successor 三源对账清单（mfg+inv+pur 域，段 1，§6 MA3 适配）

> 三源：S1 = `docs/audits/arm-index.md` 行内 successor/触发条件声明 / S2 = owner doc 内嵌 successor / Deferred 段落 / S3 = `docs/backlog/README.md` 既有追踪行。

| # | successor 项 | 三源覆盖 | 触发条件摘要 | 复杂度 | A2.x 关闭裁决交叉（two-faces） |
|---|-------------|---------|-------------|--------|------------------------------|
| 1 | 物料预留子系统完整写路径（mfg 侧） | S1+S2 | 库存域 `ErpInvReservation*` 写接口先行落地后，mfg 经 `IErpInvReservationBiz` 接线 | S | `P1-MA3-042`（A2.3 范围外——该 finding 为 owner-doc drift 已 R2.6 方案 B 关闭；successor 触发条件归本 A3.2） |
| 2 | 委外单 MRP 释放收敛为 I*Biz 调用 | S1 | 委外域提供 purpose-built `createFromMrpLine`/`createFromMrpLineForSubcontract` 时 | S | `P1-MA2-038`（A2.3：有意设计，§4(i) 成立，保留 P2 successor） |
| 3 | STANDARD 红冲成本不变量（FIFO 调整层物理删除边界） | S2 | 实际启用 FIFO 物料的成本调整红冲遇此场景时 | A | 无独立 A2.x finding（`P2-MA2-029` watch-only 承接，红冲闭环功能完整） |
| 4 | 盘点自动生成盘盈/盘亏移动单 | S1 | owner doc §盘点单状态机 自动生成语义恢复 | A | `P1-MA2-062`（A2.6 空集——该 finding 经 R1.19 实现修复关闭非方案 B；A1.27 RC 复查「倾向重开 P1 入 MR1」属 A1.x→MR1 通道，successor 残留归本 A3.2） |
| 5 | 拣货单状态机（WMS） | S1 | WMS 上线时 | A | `P1-MA2-063`（A2.6 空集——该 finding 经 R1.19 实现修复关闭非方案 B；successor 触发条件归本 A3.2） |
| 6 | master-data 跨域只读迁移（md 目标域子集） | S1 | master-data I*Biz 补便捷只读方法后迁移 | B | `P1-MA1-022`（A2.9：有意设计，§4(i) 成立，保留 P2 successor） |
| 7 | md 目标域子集=可迁移（P1-MA1-022 子集） | S1 | md I*Biz 只读方法补齐 | B | `P1-MA1-022`（A2.9：有意设计，§4(i) 成立，保留 P2 successor；#6/#7 同一 finding 的 successor 子集两面） |

> §对账差异登记覆盖：#3 仅 S2 覆盖（owner doc `costing-methods.md §FIFO 红冲`[写时实测 :37-40，原注记 :66 行漂移非引用失效] 内嵌，arm-index 无独立 successor 行——`P2-MA2-029` watch-only 承接），本复查已纳入避免遗漏（§对账差异 #3）。#1 物料预留单源遗漏风险（§对账差异 #6：S1 `P1-MA3-042` R2.6 修复 owner doc 标注 + S2 `material-reservation.md §9/§14` 整节 Deferred 双源覆盖，但实现侧仅库存域 reservedQty 承载）已纳入完整性核实。

---

## 2. 逐项四任务核证（段 2，§6 MA3 适配）

> 四任务：① 触发条件是否已满足（grep 实仓代码/config/SPI）；② 是否该回队；③ 无触发条件的补登记；④ `docs/backlog/README.md` 既有行覆盖与正确性复核。

### 2.1 #1 物料预留子系统完整写路径（mfg 侧）

- **① 触发条件状态**：**未满足**。实仓 grep 核实：
  - 库存域 `IErpInvReservationBiz`/`IErpInvReservationLineBiz` 接口存在（`module-inventory/erp-inv-dao/src/main/java/app/erp/inv/biz/`，均 `extends ICrudBiz<ErpInvReservation*>` 通用 CRUD），`ErpInvReservationBizModel`/`ErpInvReservationLineBizModel` 存在——但均为 **generic CrudBizModel 骨架**，无 purpose-built 写方法承载「审核触发预留 / 领料扣减预留 / 预留释放」语义。
  - mfg 侧 `IErpInvReservationBiz` 跨 `module-manufacturing` **零命中**（未接线）。mfg 仍由 `KitAvailabilityChecker` 做**只读齐套校验**（读 `ErpInvStockBalance.availableQuantity`），**不写预留**。
  - owner doc `manufacturing/material-reservation.md §Deferred 实现说明:9-16` 显式标注「完整物料预留子系统（`ErpMfgMaterialReservation` 实体 + 工单 `reservationStatus` 6 态 + 审核触发预留/领料扣减/预留释放写路径 + 5 个 `erp-mfg.reservation-*` config key）当前**均未落地**」+「Successor 触发条件：完整预留写路径 = 跨域 substantial slice，须库存域 `ErpInvReservation*` 写接口先行落地后，再于制造域接线」。
  - **结论**：successor 触发条件（库存域 purpose-built 写接口 + mfg 接线）**未满足**——仅 generic CRUD 骨架 + entity 落地，purpose-built 预留写路径（审核触发/扣减/释放 flow）+ mfg `IErpInvReservationBiz` 接线均未实现。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；补 purpose-built 预留写路径触及 ORM 结构变更[库存域写接口语义扩展]+ 跨域架构变更，属 ask-first 保护区域，修复归 MR1 而非本审计）。
- **③ 补登记**：无需补登记（S1+S2 双源覆盖，arm-index `P1-MA3-042` 行含 Deferred plan + owner doc `material-reservation.md §9/§14` 整节 Deferred）。
- **④ README 覆盖复核**：`docs/backlog/README.md` 无独立物料预留 design successor 行（其 81 行经 M0.3 §对账差异登记 #4 核实为 E2E 测试 successor，非 design successor）。design successor 经 S1（arm-index `P1-MA3-042` 行）+ S2（owner doc 整节 Deferred）双源覆盖，**无「已登记但从未触发」风险**（触发条件 purpose-built 写接口未落地明确，未误标 done）。
- **结构性约束标注**：本项是 mfg 多项前置（owner doc `material-reservation.md §14` 明示）；§对账差异 #6 单源遗漏风险已核实——S1+S2 双源覆盖充分，实现侧仅库存域 reservedQty 承载属实（mfg 侧 KitAvailabilityChecker 只读），successor 仍需回队（待库存域写接口先行落地）。

### 2.2 #2 委外单 MRP 释放收敛为 I*Biz 调用

- **① 触发条件状态**：**未满足**。实仓 grep 核实：
  - `createFromMrpLine` / `createFromMrpLineForSubcontract` 跨 `module-manufacturing`/`module-purchase` **仅 javadoc 命中**（`MrpReleaseService.java:45,53` 注释「通用 CRUD（save(Map)），无 purpose-built `createFromMrpLine` 方法」+「待采购域提供 purpose-built `createFromMrpLine` 时可收敛为 I*Biz 调用（successor）」），**无实际方法实现**。
  - `MrpReleaseService.releaseToSubcontractOrder:189-216` 仍经 `daoProvider.daoFor(ErpMfgSubcontractOrder.class)` 直接持久化委外单（APPROVED 绕审批 :199-201）+ 委外单行（:210-214），**未收敛为 I*Biz 调用**。
  - owner doc `architecture/posting-exemptions.md §MrpReleaseService 收敛条件(Successor):29-31` 显式标注「跨域写：待采购域提供 purpose-built `createFromMrpLine` 时收敛 / 同域委外（P1-MA2-038）：待 manufacturing 域提供 purpose-built `createFromMrpLineForSubcontract` I*Biz（或委外单审批管道支持「MRP 自动批准」配置门控）时收敛」。
  - **结论**：successor 触发条件（委外域/pur 域提供 purpose-built createFromMrpLine I*Biz）**未满足**——purpose-built 方法未实现，MrpReleaseService 仍走 daoFor 直写路径。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；收敛为 I*Biz 调用属代码逻辑类修复，归 MR1 R1.0 展开，不触 §5 ask-first 保护区域[纯 Facade 新增非 ORM/会计/删除]）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-038` 行含 successor 声明 + owner doc `posting-exemptions.md §MrpReleaseService` 收敛条件双锚点）。
- **④ README 覆盖复核**：无独立 design successor 行（README mfg 委外行均为 E2E 测试 successor，如 `2026-07-14-1934-1` 委外红冲浏览器层 E2E，非 design 收敛 successor）。S1+S2 覆盖充分，无悬空。
- **两面关系标注**：本 #2 successor 触发条件归 A3.2；其 finding `P1-MA2-038` 关闭裁决（O-4 豁免登记）归 A2.3（已收口「有意设计 §4(i) 成立」）。successor 维持 backlog ≠ finding 重开（finding 经 A2.3 裁决有意设计亦不重开，两者一致不冲突）。

### 2.3 #3 STANDARD 红冲成本不变量（FIFO 调整层物理删除边界）

- **① 触发条件状态**：**未满足**。实仓 grep 核实：
  - `CostAdjustmentService.removeFifoAdjustLayer` 仍存在（`module-inventory/erp-inv-service/.../costing/CostAdjustmentService.java:188 调用 / :194 方法定义`）——FIFO 物料成本调整的 delta 调整层（`incomingMoveId=-lineId` 哨兵）经物理删除，若该调整层已部分被后续出库消耗，物理删除可能破坏已扣减层（不变量风险窗口仍在）。
  - owner doc `finance/costing-methods.md §FIFO 红冲:37-40`（原注记 :66 行漂移）显式标注 successor + Non-Goal（Deferred But Adjudicated）；arm-index `P2-MA2-029` watch-only 承接「触发条件：实际启用 FIFO 物料的成本调整红冲遇此场景时」。
  - **结论**：successor 触发条件（实际启用 FIFO 物料的成本调整红冲遇此场景）**未满足**——风险窗口存在但未在实际 FIFO 业务路径触发强制修复。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；属 watch-only P2，触发条件未强制；修复触及数据删除边界属 ask-first 保护区域，归 MR1 而非本审计）。
- **③ 补登记**：**需补登记（M0.3 §对账差异登记 #3）**。本项仅 S2 覆盖（owner doc `costing-methods.md §FIFO 红冲` 内嵌），arm-index 无独立 successor 行（`P2-MA2-029` watch-only 承接但非独立 successor 行）。按 §7 纪律，补登记以实仓 grep 为准——本复查经 grep 核实 owner doc 内嵌 successor 描述与实仓一致（`removeFifoAdjustLayer` 物理删除路径属实），**补登记入 arm-index inventory successor 分区**（段 6 衔接），不松弛。
- **④ README 覆盖复核**：无独立 design successor 行。补登记后 S1+S2 双源覆盖。

### 2.4 #4 盘点自动生成盘盈/盘亏移动单

- **① 触发条件状态**：**未满足**。实仓 grep 核实：
  - `ErpInvStockTakeBizModel.completeTake:40-50`（HEAD 实测全文）仅 `setDocStatus(DONE)` + `updateEntity`，**无 generateMove / generateGain / generateLoss 调用**。grep 全 `module-inventory/erp-inv-service/src/main/` `StockTake.*generateMove|fromStockTake|completeTake` 仅 BizModel 方法声明命中，**零自动生成移动单逻辑**。
  - owner doc `inventory/state-machine.md §盘点单状态机`（arm-index `P1-MA2-062` 行引用 L153）声明「盘点完成（DONE）→ 自动生成盘盈/盘亏移动单」+「所有余额变动都通过移动单流水可追溯」——自动生成语义**未恢复**（completeTake 仅置 DONE，盘点差异经库管员手工 generateMove 处置）。
  - arm-index `P1-MA2-062` 标 `✅ resolved (R1.19 done)`，但 A1.27 RC 复查（`2026-08-05-0900-rc-ma1-a1-27-inventory-f3-...md §6.2`）「§4 三判据在「人工批准」意义上不满足 → 按 Q4=(a) **倾向重开 P1 入 MR1**」+ M0.3 §对账差异 #5「P1-MA2-062 经 R1.19 实现修复，但方案 B 路径未走；当前 Deferred 手工」——R1.19 未恢复自动生成语义，successor 残留属实。
  - **结论**：successor 触发条件（owner doc §盘点单状态机 自动生成语义恢复）**未满足**——completeTake 仍未自动生成盘盈/盘亏移动单，自动生成语义未恢复。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——自动生成语义未恢复）。**关键边界**（methodology §MA2↔MA3 协作）：successor 维持 backlog ≠ finding 不修复。finding `P1-MA2-062` 的重开决策属 A1.27 RC（A1.x→MR1 通道，「倾向重开 P1 入 MR1」），与本 A3.2 successor 裁决各自独立、交叉引用不冲突。本 A3.2 只裁决 successor 触发条件（未满足→维持 backlog），不重审 finding 关闭裁决（R1.19 实现修复 vs A1.27 倾向重开属 A1.x/A2.x 通道）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-062` 行含 successor 残留注记 + owner doc §盘点单状态机 锚点）。
- **④ README 覆盖复核**：无独立 design successor 行（README inventory 行均为 E2E 测试 successor）。S1 覆盖充分，无悬空。

### 2.5 #5 拣货单状态机（WMS）

- **① 触发条件状态**：**未满足**。实仓 grep 核实：
  - `ErpInvPickingOrderBizModel` 仍为 **15 行 CRUD 桩**（`extends CrudBizModel<ErpInvPickingOrder> implements IErpInvPickingOrderBiz`，无任何 setStatus writer）。grep 全 `module-inventory/erp-inv-service/` `startPicking|completePicking|cancelPicking|PICKING_STATUS` **零业务命中**。
  - dict `erp-inv/picking-status` 4 态（PENDING/PICKING/PICKED/CANCELLED）中 PICKING/PICKED 仍为 dict 死状态（PENDING 仅 codegen 默认值承载，CANCELLED 经 useLogicalDelete 承载）。
  - owner doc `inventory/state-machine.md` 无拣货独立章节；arm-index `P1-MA2-063` 标 `✅ resolved (R1.19 done)` 但拣货执行仍由 Warehouse Management System (WMS) successor 承载。
  - **结论**：successor 触发条件（WMS 上线时）**未满足**——WMS 未上线，拣货状态机生命周期未实现。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足——WMS 未上线；实现拣货状态机属代码逻辑类修复，归 MR1 R1.0 展开，不触 §5 ask-first 保护区域[纯 BizModel 状态迁移非 ORM/会计/删除]）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA2-063` 行含 WMS successor 声明）。
- **④ README 覆盖复核**：无独立 design successor 行。S1 覆盖充分，无悬空。

### 2.6 #6 master-data 跨域只读迁移（md 目标域子集）

- **① 触发条件状态**：**未满足**。实仓 grep 核实：
  - `module-master-data/erp-md-api` **零 `IErpMd*Biz.java` 接口**（find 全模块无命中）；`erp-md-service` BizModel 均为 generic CrudBizModel（`ErpMdMaterialBizModel` 等），**无 purpose-built 便捷只读方法**（grep `resolveMaterial|readMaterial|findMaterial|resolveCostMethod|readAcctSchema` 跨 md-api 零业务命中）。
  - owner doc `architecture/data-dependency-matrix.md §9:867,877` 显式标注「md 子集=可迁移（13 域 md-service compile-scope）」+「md 迁移 successor：开按域分批计划替换 `daoFor(ErpMd*)` → `@Inject IErpMd*Biz`」——迁移**未执行**（9 域 daoFor(ErpMd*) 站点仍直访）。
  - arm-index `P1-MA1-022` 标 `✅ resolved (plan 2026-07-29-2225-1: 读侧统一裁决——md 目标域子集=可迁移[successor 已命名])`——裁决落地但**迁移 successor 未实施**（md I*Biz 只读方法未补齐）。
  - **结论**：successor 触发条件（master-data I*Biz 补便捷只读方法后迁移）**未满足**——md I*Biz 只读方法未补齐，9 域 daoFor(ErpMd*) 站点未迁移。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；补 md I*Biz 只读方法 + 迁移属代码逻辑类修复，归 MR1 R1.0 展开，不触 §5 ask-first 保护区域[纯 Facade 新增 + 调用点迁移非 ORM/会计/删除]）。
- **③ 补登记**：无需补登记（S1 覆盖，arm-index `P1-MA1-022` 行含 md 可迁移 successor 声明 + owner doc `data-dependency-matrix.md §9` 双锚点）。
- **④ README 覆盖复核**：无独立 design successor 行。S1+S2 覆盖充分，无悬空。
- **两面关系标注**：本 #6 successor 触发条件归 A3.2；其 finding `P1-MA1-022` 关闭裁决（读侧统一裁决）归 A2.9（已收口「有意设计 §4(i) 成立」）。

### 2.7 #7 md 目标域子集=可迁移（P1-MA1-022 子集）

- **① 触发条件状态**：**未满足**（与 #6 同一根因同一控制点子集）。实仓 grep 同 #6——md I*Biz 只读方法未补齐，可迁移条件未达成。owner doc `data-dependency-matrix.md §9:877` 明示「md 子集=可迁移（successor 已命名，触发=master-data I*Biz 补便捷只读方法）」。
- **② 回队决策**：**维持 backlog successor**（触发条件未满足；与 #6 同根因，MR1 R1.0 展开时 #6/#7 合并同一修复行[md I*Biz 只读方法补齐 + 按域分批迁移]）。
- **③ 补登记**：无需补登记（S1 覆盖，与 #6 同一 arm-index `P1-MA1-022` 行）。
- **④ README 覆盖复核**：无独立 design successor 行。S1 覆盖充分。
- **两面关系标注**：本 #7 与 #6 是 `P1-MA1-022`（A2.9）的 successor 子集两面——#6 = 「跨域只读迁移」动作面，#7 = 「md 目标域子集=可迁移」状态面，同一 finding 同一 successor，交叉引用不重复。MR1 R1.0 展开时归一为同一 RC-R1.n 修复行。

---

## 3. 既有行为证据（段 3，复用既有 arm 审计，§去重协议）

> 本复查为 successor 触发条件复查（需求契约视角），不重做 doc↔code 文本一致性 / 状态机行为 / 代码质量。实现证据复用既有 arm MA2/MA4 报告 + A2.x RC 复查报告已证实的代码路径，仅列锚点供四任务核证溯源。

| # | successor 项 | 代码锚点（复用 arm MA2/MA4 + A2.x RC 已证实） | 既有证实报告 |
|---|-------------|----------------------------------------------|-------------|
| 1 | 物料预留写路径 | `KitAvailabilityChecker:30-33`（只读不写预留）+ inventory `IErpInvReservationBiz`/`IErpInvReservationLineBiz`（generic CRUD 骨架，无 purpose-built 写方法）+ mfg `IErpInvReservationBiz` 零命中 | `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（P1-MA3-042 material-reservation.md 整子系统未实现 R2.6 关闭）；A2.3 RC §3 |
| 2 | 委外收敛 createFromMrpLine | `MrpReleaseService.releaseToSubcontractOrder:189-216`（daoFor 直写 ErpMfgSubcontractOrder APPROVED 绕审批）+ `:45,53` javadoc（无 purpose-built createFromMrpLine successor） | `2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（P1-MA2-038）；A2.3 RC §3 |
| 3 | STANDARD 红冲 FIFO 边界 | `CostAdjustmentService.removeFifoAdjustLayer:188,194`（物理删除 delta 调整层）+ `TestErpInvCostAdjust.testReverseRollsBackBalanceAndVoucher`（仅 MA 路径，FIFO 边界未覆盖） | `2026-07-27-2211-arm-ma2-inventory-costing-consistency.md §P2-MA2-029`；A1.5 RC §3 |
| 4 | 盘点自动生成移动单 | `ErpInvStockTakeBizModel.completeTake:40-50`（仅 setDocStatus DONE，无 generateMove） | `2026-07-28-0400-arm-ma2-inventory-state-machine.md §P1-MA2-062`；A1.27 RC §6.2 |
| 5 | 拣货单 WMS 状态机 | `ErpInvPickingOrderBizModel`（15 行 CRUD 桩，无 setStatus writer）+ dict picking-status PICKING/PICKED 死状态 | `2026-07-28-0400-arm-ma2-inventory-state-machine.md §P1-MA2-063` |
| 6 | md 跨域只读迁移 | 9 域 `daoFor(ErpMd*)` 站点（pur/sal/ast/inv/mnt/prj/qa/drp/aps）+ md-api 零 `IErpMd*Biz` + md-service generic CrudBizModel | `2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md` + `2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md`（P1-MA1-022 9 域）；A2.9 RC §3 |
| 7 | md 子集可迁移 | 同 #6（data-dependency-matrix.md §9 :877 明示 md 子集=可迁移 successor） | 同 #6 |

---

## 4. 运行时行为证据（段 4，复用既有 arm MA2/MA3，§去重协议）

> 本 mission MA3 = successor 触发条件复查（需求契约视角），与 audit-remediation MA2（状态机/链路行为视角）/ MA3（doc↔code drift）/ MA4（代码质量）维度不重叠（methodology §去重协议 §MA2(本)↔MA3(audit-remediation) 边界）。既有 arm 报告 + A2.x RC 报告已证实的运行时行为直接引用：

- **#1 物料预留**：mfg 齐套校验只读 `ErpInvStockBalance.availableQuantity` 决定 STOCK_RESERVED/STOCK_PARTIAL，不写预留；实际扣减由开工后领料出库移动单 DONE 完成——经 `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（P1-MA3-042）+ A2.3 RC §4 证实。库存域 reservedQty 字段承载预留量（generic CRUD 可读写），但 purpose-built 预留写路径（审核触发/扣减/释放 flow）未落地。
- **#2 委外收敛**：MrpReleaseService 委外释放 config-gated `erp-mfg.subcontract-release-enabled` 默认 false 阻断；开启时生成 APPROVED 委外单绕审批管道（O-4 豁免登记于 posting-exemptions.md §MrpReleaseService）——经 `2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md` + A2.3 RC §4 证实。
- **#3 STANDARD 红冲 FIFO 边界**：MA 物料成本调整红冲三方一致性经 TestErpInvCostAdjust 覆盖；FIFO 物料 delta 调整层物理删除边界（removeFifoAdjustLayer）未测试，风险窗口存在但未在实际 FIFO 业务路径触发——经 `2026-07-27-2211-arm-ma2-inventory-costing-consistency.md §P2-MA2-029` + A1.5 RC §4 证实。
- **#4 盘点自动生成**：StockTake 状态机 DRAFT→CONFIRMED→DONE/CANCELLED 完整覆盖生命周期；DONE 不自动生成移动单不产生悬挂数据（盘点单 DONE 但无差异调整移动单，需库管员后续手工处理）——经 `2026-07-28-0400-arm-ma2-inventory-state-machine.md §P1-MA2-062` + A1.27 RC §4 证实。
- **#5 拣货单 WMS**：PickingOrder CRUD 完整可用（PENDING 创建/查询/更新/逻辑删除），拣货执行由 WMS successor 承载，缺失状态机不产生悬挂数据——经 `2026-07-28-0400-arm-ma2-inventory-state-machine.md §P1-MA2-063` 证实。
- **#6/#7 md 跨域只读**：9 域 daoFor(ErpMd*) 跨域只读是成本解析/科目解析/物料查询副作用，不破坏状态机迁移（异常路径经 @BizMutation 事务回滚覆盖）——经 `2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md` + `2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md` + 多域 MA2 状态机报告复核无升级证实。

---

## 5. 复查结论（段 5，§6 MA3 适配：触发条件状态 + 回队决策）

> 复查结论三分：`回队 MR1`（触发条件已满足 / Q4 强制）/ `维持 backlog successor`（触发条件未满足）/ `补登记`（owner doc 内嵌但 arm-index 无行）。

### 5.1 逐项复查结论

| # | successor 项 | 触发条件状态 | 证据 | 回队决策 | 与 A2.x 关闭裁决交叉 |
|---|-------------|-------------|------|---------|---------------------|
| 1 | 物料预留完整写路径 | ❌ 未满足 | inventory `IErpInvReservationBiz` 仅 generic CRUD 骨架 + mfg 零接线 + KitAvailabilityChecker 只读 | **维持 backlog successor** | #1 ↔ `P1-MA3-042`（R2.6 owner-doc drift 方案 B 关闭）；successor 触发条件归 A3.2；**mfg 多项前置 + §对账差异 #6 单源遗漏风险** |
| 2 | 委外收敛 createFromMrpLine | ❌ 未满足 | `MrpReleaseService:45,53` javadoc 无 purpose-built 方法 + `:189-216` daoFor 直写未收敛 | **维持 backlog successor** | #2 ↔ `P1-MA2-038`（A2.3：有意设计）一致 |
| 3 | STANDARD 红冲 FIFO 边界 | ❌ 未满足 | `CostAdjustmentService.removeFifoAdjustLayer:188,194` 物理删除路径仍在 + FIFO 边界未测试 | **维持 backlog successor** + **补登记** | 无独立 A2.x finding（`P2-MA2-029` watch-only）；§对账差异 #3 |
| 4 | 盘点自动生成移动单 | ❌ 未满足 | `ErpInvStockTakeBizModel.completeTake:40-50` 仅 setDocStatus DONE 无 generateMove | **维持 backlog successor** | #4 ↔ `P1-MA2-062`（R1.19 实现修复关闭，非方案 B；A1.27 RC「倾向重开 P1 入 MR1」属 A1.x→MR1 通道，successor 维持 ≠ finding 不修复，两者各自裁决） |
| 5 | 拣货单 WMS 状态机 | ❌ 未满足 | `ErpInvPickingOrderBizModel` 15 行 CRUD 桩 + PICKING/PICKED dict 死状态 | **维持 backlog successor** | #5 ↔ `P1-MA2-063`（R1.19 实现修复关闭，非方案 B） |
| 6 | md 跨域只读迁移 | ❌ 未满足 | md-api 零 `IErpMd*Biz` + md-service generic CrudBizModel + 9 域 daoFor(ErpMd*) 未迁移 | **维持 backlog successor** | #6 ↔ `P1-MA1-022`（A2.9：有意设计）一致 |
| 7 | md 子集可迁移 | ❌ 未满足 | 同 #6（data-dependency-matrix.md §9 :877 md 子集=可迁移 successor 未实施） | **维持 backlog successor** | #7 ↔ `P1-MA1-022`（A2.9）；**#6/#7 同一 finding successor 子集两面，MR1 归一** |

### 5.2 统计

- **回队 MR1**：0 项（7 项 successor 触发条件全部未满足，无 Q4 强制回队项）
- **维持 backlog successor**：7 项（#1-#7 全部维持 backlog）
- **补登记**：1 项（#3 STANDARD 红冲 FIFO 边界，§对账差异 #3，owner doc 内嵌但 arm-index 无独立 successor 行，经 grep 核实补登记）
- **本审计新发现 P0**：0 项（无 MR0 即时通道触发）

### 5.3 结构性约束（回队顺序依赖 + 两面归一）

- **#6/#7 同源归一**：md 跨域只读迁移（#6）与 md 子集可迁移（#7）是 `P1-MA1-022`（A2.9）的 successor 子集两面——同一 finding 同一 successor（md I*Biz 只读方法补齐 + 按域分批迁移）。MR1 R1.0 展开时 #6/#7 合并同一 RC-R1.n 修复行，不重复计入。
- **#1 跨域依赖 + mfg 多项前置**：物料预留完整写路径（#1）是 mfg 多项前置（owner doc `material-reservation.md §14` 明示）。successor 落地顺序：库存域 `ErpInvReservation*` purpose-built 写接口先行 → mfg 经 `IErpInvReservationBiz` 接线。触及 ORM 结构变更（库存域写接口语义扩展）+ 跨域架构变更，修复须 ask-first（§5 保护区域暂停协议）。
- **#4 finding 重开独立通道**：finding `P1-MA2-062` 经 A1.27 RC「倾向重开 P1 入 MR1」（A1.x→MR1 通道），与本 A3.2 successor 维持 backlog 各自独立。若 A1.27 finding 重开入 MR1，则 completeTake 自动生成移动单的实现修复与 #4 successor 登记同步消解（同 A3.1 #6 字面 UK 范式）。
- **#2 两面归一**：#2 successor（委外收敛）与 `P1-MA2-038` finding（O-4 豁免登记）两面——successor 触发条件归 A3.2（维持 backlog），finding 关闭裁决归 A2.3（有意设计 §4(i) 成立），两者交叉一致不冲突。
- **A3.1 GRNI 跨域项接入**：A3.1 #1 GRNI 依赖 inventory `repostPurchaseInput` SPI（A3.1 §successor #1 Follow-up 归 A3.2 范畴）——本复查 grep `module-inventory` `repostPurchaseInput` **仍零命中**，SPI 缺失，GRNI successor 触发条件维持未满足（与 A3.1 结论交叉一致）。inventory SPI 落地是 GRNI 回队的前置，归本 A3.2 inventory 侧判定：当前未落地 → GRNI 维持 backlog。

---

## 6. 与 arm-index 衔接（段 6，§7「复用 or 新增」裁决）

> §7 规则：successor 项均源自既有 arm finding，本复查原则上**复用既有 finding ID**追加 RC MA3 注记；仅当发现 owner doc 内嵌但 arm-index 无独立行的 successor（#3）才补登记。

### 6.1 逐项「复用 or 补登记」裁决

| # | successor 项 | arm-index grep 结果 | 裁决 | 操作 |
|---|-------------|---------------------|------|------|
| 1 | 物料预留完整写路径 | 既有 `P1-MA3-042` 行含 Deferred plan + owner doc §9/§14 整节 Deferred | **复用** | 既有行追加「RC MA3 复查（A3.2）：触发条件未满足[inventory IErpInvReservationBiz 仅 generic CRUD 骨架 + mfg 零接线 + KitAvailabilityChecker 只读]→维持 backlog successor；mfg 多项前置 + §对账差异 #6 单源遗漏风险」注记 |
| 2 | 委外收敛 createFromMrpLine | 既有 `P1-MA2-038` 行含 successor 声明 + owner doc posting-exemptions.md §MrpReleaseService 收敛条件 | **复用** | 既有行追加「RC MA3 复查（A3.2）：触发条件未满足[purpose-built createFromMrpLine/createFromMrpLineForSubcontract 未实现 + MrpReleaseService daoFor 直写未收敛]→维持 backlog successor」注记 |
| 3 | STANDARD 红冲 FIFO 边界 | arm-index 无独立 successor 行（`P2-MA2-029` watch-only 承接但非独立 successor 行） | **补登记** | `P2-MA2-029` 行追加「RC MA3 复查（A3.2，§对账差异 #3）：STANDARD 红冲 FIFO 调整层物理删除边界 successor 补登记——`removeFifoAdjustLayer` 物理删除路径仍在，触发条件=实际启用 FIFO 物料的成本调整红冲遇此场景时，未满足→维持 backlog」注记 |
| 4 | 盘点自动生成移动单 | 既有 `P1-MA2-062` 行含 successor 残留注记（R1.19 done + A1.27 RC 倾向重开） | **复用** | 既有行追加「RC MA3 复查（A3.2）：successor 触发条件未满足[completeTake:40-50 仅 setDocStatus DONE 无 generateMove，自动生成语义未恢复]→维持 backlog successor（successor 维持 ≠ finding 不修复，finding 重开归 A1.27 RC A1.x→MR1 通道）」注记 |
| 5 | 拣货单 WMS 状态机 | 既有 `P1-MA2-063` 行含 WMS successor 声明 | **复用** | 既有行追加「RC MA3 复查（A3.2）：触发条件未满足[ErpInvPickingOrderBizModel 15 行 CRUD 桩 + PICKING/PICKED dict 死状态，WMS 未上线]→维持 backlog successor」注记 |
| 6 | md 跨域只读迁移 | 既有 `P1-MA1-022` 行含 md 可迁移 successor 声明 + owner doc data-dependency-matrix.md §9 | **复用** | 既有行追加「RC MA3 复查（A3.2）：md 跨域只读迁移 successor 触发条件未满足[md-api 零 IErpMd*Biz + 9 域 daoFor(ErpMd*) 未迁移]→维持 backlog successor」注记 |
| 7 | md 子集可迁移 | 同 #6（同一 `P1-MA1-022` 行 successor 子集两面） | **复用** | 与 #6 同一注记归一（#6/#7 同源 successor，MR1 R1.0 展开时合并同一 RC-R1.n 修复行） |

**裁决依据**：#1/#2/#4/#5/#6/#7 为既有 arm finding 的同一根因/同一控制点 successor，复用既有 ID 追加 RC MA3 注记；#3 经 grep 核实 owner doc 内嵌 successor 描述与实仓一致（§7 纪律，非松弛），补登记入 arm-index（`P2-MA2-029` 行追加注记）。**不新建 `P*-RC-xxx`**（禁止未经比对直接新建）——#3 补登记为 successor 行注记追加，非新 finding 编号。

### 6.2 双向可追溯

- **维持 backlog 项 ↔ A3.x successor 登记**：#1-#7 全部维持 backlog，交叉引用本 A3.2 报告 + arm-index successor 注记。
- **#6/#7 ↔ MR1 R1.0 预留展开行**：#6/#7（`P1-MA1-022` successor）→ MR1 R1.0 展开为 `RC-R1.n`（修复行须含 finding ID 交叉引用 `P1-MA1-022` + 触及保护区域标注「否[纯 Facade 新增 + 调用点迁移]」+ Skill）。与 A2.9 RC §6.2 finding 维持裁决**各自独立**（A2.9 裁决 finding 读侧豁免保留，A3.2 裁决 successor 维持 backlog 待触发，两者不冲突）。
- **#1 ↔ MR1 触及保护区域**：物料预留完整写路径（#1）修复触及 ORM 结构变更（库存域写接口语义扩展）+ 跨域架构变更，须 ask-first + 独立 plan-audit（§5）。
- **arm-index 回填**：§6.1 注记已写入 `arm-index.md`（既有 6 行追加 + #3 `P2-MA2-029` 行补登记注记）。

---

## 7. 静态存疑点清单（段 7，供 MA4 A4.2 展开）

> L5 无法静态定论、需运行时确认的点。本复查为 successor 触发条件复查（读 arm-index/owner doc/实仓代码/config），以下为复查中静态无法定论、建议 MA4 运行时确认的点：

1. **#1 物料预留 inventory generic CRUD 写路径运行时语义**：库存域 `IErpInvReservationBiz`/`IErpInvReservationLineBiz` 已落地为 generic CrudBizModel 骨架（entity + generic save/update/delete）——generic CRUD 写路径运行时是否可被外部调用方直接用来承载预留语义（绕过 purpose-built 写方法），需运行时确认（静态已确认 mfg 未接线，但 generic CRUD 的开放写面是否被其他域/前端直驱需运行时普查）。建议 MA4 A4.2 物料预留运行时确认项展开。
2. **#3 FIFO 调整层物理删除边界运行时触发面**：`removeFifoAdjustLayer` 物理删除 delta 调整层（`incomingMoveId=-lineId` 哨兵）若该层已部分被后续出库消耗，物理删除可能破坏已扣减层——实际启用 FIFO 物料的成本调整红冲遇此场景的触发频率依赖部署负载与业务路径，需运行时并发/序列探针确认（静态已确认风险窗口存在，与 A1.5 §7 存疑点/A4.1.16 同一）。建议 MR1 修复行（RC-R1.n）附 FIFO 红冲负向测试。
3. **#4 盘点 completeTake 手工 generateMove 运行时追溯链完整性**：completeTake 仅置 DONE，盘点差异经库管员手工 generateMove 处置——手工处置路径在实际运维中的遗漏率（盘点单 DONE 但差异移动单长期未生成）需运行时/运维数据确认（静态已确认自动生成未恢复，遗漏率依赖运维纪律）。与 A1.27 §7 存疑点同一。

> 其余 4 项（#2/#5/#6/#7）的运行时行为已由既有 arm MA2/MA4 报告充分证实（§4），无新增静态存疑点。

---

## 8. 过程纪律自检（段 8，§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（actual 见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本次实测 CHECKER_EXIT:0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以 checker 脚本退出码作为门控通过依据**。**本审计无生产代码变更（纯审计报告 + arm-index 文档注记），checker 无回归风险**——actual 计数与本审计行为正交（未触及任何生产代码），任何 actual vs baseline 差异均非本审计引入（git diff 确认仅 docs/ 变更）。

  | 规则 | 基线（compliance-baseline.md §基线表） | actual（本次实测） | 漂移 | 归因 |
  |------|-------------------------------|-------------------|------|------|
  | R1a | 0 | 0 | 0 | — |
  | R1b | 0 | 0 | 0 | — |
  | R1c | 0 | 0 | 0 | — |
  | R1d | 14 | 14 | 0 | — |
  | R2a | 34 | 34 | 0 | — |
  | R2b | 240 | 229 | -11（actual < baseline） | 改善方向；与本审计无关（纯 docs/ 变更），为既有基线文档与实仓的预存漂移 |
  | R2c | 1380 | 1382 | +2 | 既有漂移（与本审计无关，纯 docs/ 变更）；非本审计引入 |
  | R2d | 32 | 34 | +2 | 既有漂移（与本审计无关，纯 docs/ 变更）；非本审计引入 |

  > 本审计仅产出本报告 + `arm-index.md` 注记（纯文档），未触及 `module-*/` 任何生产代码。R2b/R2c/R2d 的小幅漂移为 compliance-baseline.md §基线表与实仓的**预存不一致**（A3.1 报告 §8 实测亦为 R2b=229/R2c=1382/R2d=34，与本审计一致——证实代码侧未变，漂移源自基线表文档侧），非本审计引入，不构成回归。R1a/R1b/R1c/R1d/R2a 全部 = baseline 零漂移。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见来源 plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部 7 项 successor 已按 §7 规则 grep arm-index 同域同控制点后给出「复用 or 补登记」裁决（§6.1），无未经比对直接新建的 `P*-RC-xxx` finding（#3 为 `P2-MA2-029` 行 successor 注记追加，非新 finding 编号）。

---

## 9. 与既有审计差异增量声明（段 9，§去重协议）

本报告与既有 arm 审计（`docs/audits/2026-07-2*-arm-ma2-*` / `arm-ma3-*` / `arm-ma4-*`）+ A2.x RC 复查报告（`2026-08-06-0442-1-rc-ma2-a2-3-8-9-governance-exemption-simplification-review.md`）+ A3.1 RC 复查报告（`2026-08-07-0300-rc-ma3-a3-1-finance-successor-review.md`）的差异增量：

- **复用既有证据**（不重复验证）：
  - `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（#1 P1-MA3-042 material-reservation.md 整子系统未实现已证实，R2.6 方案 B 关闭）；
  - `2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（#2 P1-MA2-038 MrpReleaseService 委外单 O-4 豁免已证实）；
  - `2026-07-27-2211-arm-ma2-inventory-costing-consistency.md §P2-MA2-029`（#3 FIFO 红冲三方一致性未测试已证实）；
  - `2026-07-28-0400-arm-ma2-inventory-state-machine.md`（#4 P1-MA2-062 盘点 + #5 P1-MA2-063 拣货状态机已证实）；
  - `2026-07-27-1227-arm-ma1-platform-conformance-a-tier-core.md` + `2026-07-27-1430-arm-ma1-platform-conformance-bc-tier.md`（#6/#7 P1-MA1-022 9 域跨域只读已证实）；
  - `2026-08-06-0442-1-rc-ma2-a2-3-8-9-governance-exemption-simplification-review.md`（A2.3/A2.8/A2.9 方案 B 关闭裁决 P1-MA2-038/P1-MA1-022 §4 三判据核证 + 交叉引用）；
  - `2026-08-07-0300-rc-ma3-a3-1-finance-successor-review.md`（A3.1 finance successor 复查范式参照 + #1 GRNI 跨域依赖 inventory `repostPurchaseInput` SPI Follow-up 归 A3.2）。

- **本复查只补的差异增量**：**successor 触发条件是否已满足 + 是否该回队**——从 methodology §MA3 四任务（① 触发条件状态 grep 实仓验证 / ② 回队决策 / ③ 补登记 / ④ README 覆盖复核）出发，逐项核证 7 项 mfg+inv+pur successor 的触发条件现状。这是既有 arm 审计（doc↔code / 状态机行为 / 代码质量维度）+ A2.x RC（方案 B 关闭裁决正当性维度）未覆盖的「successor 触发条件完整性 + 回队决策」维度（methodology §去重协议 §MA2↔MA3 协作——关闭裁决归 A2.x，successor 触发条件归 A3.x，交叉引用不重复）。

- **A3.1 GRNI 跨域项交叉**：A3.1 §successor #1 GRNI 自动冲回依赖 inventory 域 `repostPurchaseInput` SPI，A3.1 Follow-up 明示「归 A3.2 复查范畴」。本复查 grep `module-inventory` `repostPurchaseInput` **仍零命中**（SPI 缺失），结论与 A3.1 交叉一致：GRNI successor 触发条件维持未满足→维持 backlog；inventory SPI 落地是 GRNI 回队的前置，归本 A3.2 inventory 侧判定（当前未落地）。本 A3.2 不重复登记 GRNI successor（归 A3.1 finance 分组），仅交叉引用确认 inventory SPI 状态。

- **不重复**：不重做 doc↔code 文本一致性（audit-remediation MA3 已收口，含 P1-MA3-042 R2.6 关闭）、不重做状态机/链路行为（arm MA2 已收口）、不重做代码质量（arm MA4 已收口）、不重审方案 B 关闭裁决本身（A2.3/A2.8/A2.9 RC 已收口，本 A3.2 只复查 successor 触发条件，两面交叉引用）、不重审 A3.1 finance successor（A3.1 已收口，仅 GRNI 跨域项交叉）。

---

## 结论

mfg+inventory+purchase MA3 successor 复查（A3.2）完成：7 项 design-level successor 逐项经 §MA3 四任务核证。

- **回队 MR1**：0 项（7 项 successor 触发条件全部未满足，无 Q4 强制回队项）。
- **维持 backlog successor**：7 项（#1-#7 全部维持 backlog，待触发条件满足）。
- **补登记**：1 项（#3 STANDARD 红冲 FIFO 边界，§对账差异 #3，owner doc 内嵌但 arm-index 无独立 successor 行，经 grep 核实补登记入 `P2-MA2-029` 行）。
- **结构性约束**：#6/#7 同源归一（`P1-MA1-022` successor 子集两面，MR1 合并同一 RC-R1.n）；#1 mfg 多项前置 + 跨域依赖 + 触及 ORM 保护区域（ask-first）；#2 两面归一（`P1-MA2-038` A2.3）；#4 finding 重开独立通道（A1.27 RC A1.x→MR1，successor 维持 ≠ finding 不修复）；A3.1 GRNI 跨域依赖 inventory `repostPurchaseInput` SPI 仍缺失（与 A3.1 交叉一致）。
- **arm-index 衔接**：7 项全部复用既有 ID 追加 RC MA3 注记（无新 `P*-RC-xxx`）；#3 补登记为 `P2-MA2-029` 行 successor 注记追加；#6/#7 归一同一 `P1-MA1-022` 注记。
- **本审计无生产代码变更**（纯报告 + arm-index 文档注记），§9 真相源冻结条款遵守（未修改 product-scope / owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实 / backlog README）。
