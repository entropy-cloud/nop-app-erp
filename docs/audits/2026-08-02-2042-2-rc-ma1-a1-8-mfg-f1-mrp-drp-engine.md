# rc-ma1-a1-8 mfg-F1 MRP/DRP 引擎 需求-实现符合性五级追踪审计报告

> 报告类型：requirement-compliance MA1 切片 A1.8
> 切片：mfg-F1 MRP/DRP 引擎（roadmap 标签；权威 UC 范围 = UC-MFG-05/08 工单审核触发/释放物料预留，非 MRP 净需求计算本身）
> 审计时间：2026-08-02
> 审计基线 HEAD：`5953f07c1`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 上游计划：`docs/plans/2026-08-02-2042-2-rc-ma1-a1-8-mfg-f1-mrp-drp-engine.md`（草案审查 `acceptable as-is`，独立子代理 `ses_03d4f1becffeGSyHogCRyQ0233`）
> 真相源层级（§4 Q1）：L1 = `docs/design/manufacturing/use-cases.md`（UC-MFG-05 `:90-104` / UC-MFG-08 `:144-156`）；L2 = `material-reservation.md` + `mrp.md`（设计参考，整节 Deferred，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.6b/A4.2b + 本切片差异。

---

## 9. 与既有 MA2/MA4 报告差异增量声明（前置声明，便于读者识别复用边界）

> 依方法论 §6 段落 9 + §去重协议，本报告前置声明与既有 MA2/MA4 报告的差异增量。

| 既有报告 | 覆盖维度 | 已证实结论（本切片复用） | 本切片补的差异增量（需求契约视角） |
|---------|---------|----------------------|--------------------------|
| `2026-07-28-0109-arm-ma2-mfg-mrp-bom-state-machine.md`（A2.6b） | MRP 计划规划状态机（MRP 计划头 5 态 + 预测 4 态 + 建议单隐式生命周期 isFirmed + BOM isActive + 仿真 4 态）+ 事务原子性 | MRP 运算 `RUNNING→COMPLETED` + 释放路径跨域 saveEntity + `markFirmed` + `advancePlanToFirmedIfComplete` 全链经 @BizMutation 事务回滚保证；幂等守卫完整（已 firmed 行重复释放拒绝）；仿真 E2 fork 单次路径零触及；**3 P1 全 resolved**：P1-MA2-036 MRP CANCELLED/预测 CONSUMED dict 死状态（R1.14 方案 A 保留为预留）+ P1-MA2-037 mrp.md RELEASED→isFirmed 措辞修正（R1.14）+ P1-MA2-038 委外 APPROVED O-4 豁免登记（plan 2026-07-29-2225-1） | MRP 引擎**本身**（runMrp/释放/事务原子性）的**需求契约符合性 = 接受**（复用 A2.6b pass 结论，不重审）；本切片只补 **UC-MFG-05/08 物料预留写路径** 的需求契约裁决（A2.6b 未从需求视角核，归 owner-doc drift A3.4 + Deferred） |
| `2026-07-29-0024-arm-ma4-mfg-mrp-quality-code-quality.md`（A4.2b） | MRP/成本/基因/委外链路**代码质量** | MrpEngine/MrpReleaseService/DemandAggregator/CostRollupService/ProductionVarianceCalculator/BatchGenealogy/CRP 的代码质量已审；跨域 daoFor 经 P1-MA4-012 统一裁决登记（读侧豁免/写侧 O-4 豁免） | 本切片不重审代码质量维度；只补**需求契约 vs 实现符合性**（预留写路径 Deferred 的 Q4 裁决） |
| `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（A3.4） | owner doc vs code **drift**（文本一致性） | P1-MA3-042 material-reservation.md 整个子系统未实现（doc↔code drift blocker）→ **resolved R2.6**（owner doc 整节标 Deferred + 持久化真相源指向库存域 ErpInvReservation*） | 本切片不复审 doc↔code 文本一致性；**R2.6 的 方案 B 关闭（owner doc Deferred 标注）属 audit-remediation 文本一致性维度，按方法论 §去重协议 + §5 Q4，不关闭需求契约维度的 P0/P1**。本切片正是补此差异：从 L1 字面 + Q4 修复义务裁决预留写路径未实现的需求契约定级 |
| `2026-07-29-1430-arm-ma5-mfg-test-coverage.md`（A5.2） | 测试覆盖深度 | P1-MA5-006 mfg 物料预留子系统零测试 → R3.0 裁决为 successor 注记（被测功能 P1-MA3-042 未实现→无测试可补） | 本切片不复审测试维度；预留写路径 L4 = 无测试（与 P1-MA5-006 一致），属同一事实不同审计轴投影 |

**结论**：本切片裁决焦点 = **UC-MFG-05/08 预留写路径的需求契约符合性**。MRP 引擎本身复用 A2.6b pass 结论（接受），预留写路径字面未实现按 §4 Q1（L1 为准）+ §5 Q4（P0/P1 必须实现禁方案 B）+ §2 判据裁决（详见 §5）。

---

## 1. 需求契约原文（L1 逐字引用，禁止转述）

> 真相源：`docs/design/manufacturing/use-cases.md`（UC 锚点经 `docs/audits/rc-requirement-baseline-inventory.md` A1.8 确认 = `:90/:144`，inventory `:342` 一致）。

### UC-MFG-05 工单审核触发物料预留（`use-cases.md:90-104`）

逐字引用验收标准：

```
工单.审核通过 →
  BOM 展开 → 创建 MaterialReservation(每个子件一条)        [断言①]
  执行预留: 预留量 = min(需求量, 可用量)                    [断言②]
  库存余额.预留量 += 预留量                                  [断言③]
  工单.reservationStatus = RESERVED(或 PARTIAL_RESERVED)    [断言④]
```

涉及机制：`material-reservation.md §预留流程`、`../inventory`（预留量 owner）。

### UC-MFG-08 工单取消/完工释放预留（`use-cases.md:144-156`）

逐字引用验收标准：

```
工单.CANCELLED 或 COMPLETED →
  释放未领料的预留(reservedQty - pickedQty)                 [断言⑤]
  库存余额.预留量 -= 释放量                                  [断言⑥]
  MaterialReservation.状态 = RELEASED                       [断言⑦]
```

涉及机制：`material-reservation.md §预留释放`。

**断言计数**：UC-MFG-05 ×4（①②③④）+ UC-MFG-08 ×3（⑤⑥⑦）= **7 条验收标准**（与草案审查 iter1 实测一致）。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### 2.1 MRP 引擎主链（UC-MFG-05/08 上下文，复用 A2.6b/A4.2b 结论）

| 组件 | 文件:行 | 作用 |
|------|---------|------|
| MRP 运算入口 | `module-manufacturing/erp-mfg-service/.../entity/ErpMfgMrpPlanBizModel.java:29`（`runMrp` @BizMutation，R6.2 per-mutation 拆分后委托 Processor） | 净需求/低层码递归运算，事务包裹 |
| 运算 Processor | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgMrpPlanRunMrpProcessor.java` | runMrp 实际执行（R6.2 拆分） |
| 净需求引擎 | `module-manufacturing/erp-mfg-service/.../mrp/MrpEngine.java` | BOM 多级展开 + 净需求计算 |
| 需求整合 | `module-manufacturing/erp-mfg-service/.../mrp/DemandAggregator.java` | 销售/安全库存/预测需求整合 |
| 建议单释放 | `module-manufacturing/erp-mfg-service/.../mrp/MrpReleaseService.java:69,85,112`（releasePurchaseRequest/releaseWorkRequest/releaseToSubcontractOrder）+ `markFirmed:131` + `advancePlanToFirmedIfComplete:223` | 三路径释放（采购/工单/委外）+ isFirmed + 计划头 FIRMED |

### 2.2 物料预留写路径（UC-MFG-05/08 核心 — 实测核验）

| 组件 | 文件:行 | 实测结论 |
|------|---------|---------|
| 齐套校验（只读） | `module-manufacturing/erp-mfg-service/.../workorder/KitAvailabilityChecker.java:62-89` | **只读校验**：`loadAvailableByMaterial:102-114` 读 `ErpInvStockBalance.availableQuantity`（:111），对比需求量，返回 `KitAvailabilityResult.reserved()/partial()`（决定 docStatus STOCK_RESERVED/STOCK_PARTIAL）。**Javadoc:31-33 显式声明「只读校验，不写预留（实际预留由工单开工后的领料出库移动单扣减）」** |
| 工单审核 Processor | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java` | `rg "reservation\|Reservation\|reservedQty\|reservationStatus"` 全文 **0 命中** — 审核路径无预留创建调用 |
| 制造域预留实体 | —（`rg "ErpMfgMaterialReservation" module-manufacturing` 仅命中 `ErpMfgDashboardBizModel` 代码注释 Non-Goal 引用） | `ErpMfgMaterialReservation` 实体**未物化**（仅业务语义说明） |
| `ErpMfgWorkOrder.reservationStatus` 字段 | —（`rg "reservationStatus" module-manufacturing/model/*.orm.xml` **0 命中**） | **无该字段**（齐套现以工单 docStatus STOCK_RESERVED/STOCK_PARTIAL 承载，owner doc `material-reservation.md:67` 显式标注 Deferred） |
| `erp-mfg.reservation-*` config key | —（`rg "reservation" module-manufacturing/erp-mfg-service/.../ErpMfgConstants.java` **0 命中**） | 5 个 config key（reservation-enabled/reservation-on-approve/over-pick-warning/auto-release-on-complete）**均未落地** |
| 库存域预留实体（跨域真相源） | `module-inventory/model/app-erp-inventory.orm.xml` ErpInvReservation/ErpInvReservationLine（实体存在） | 持久化真相源 = 库存域（material-reservation.md:11 声明），**非制造域独立 ErpMfgMaterialReservation** |
| 库存域预留写接口 | `module-inventory/erp-inv-service/.../entity/ErpInvReservationBizModel.java`（15 行 CRUD 桩）+ `ErpInvReservationLineBizModel.java`（同） | `extends CrudBizModel<ErpInvReservation>` 通用 CRUD，**无 purpose-built 预留写方法**（无 createReservation/releaseReservation/consumeReservation）— 制造域无可调用的预留写接口 |
| 库存余额预留量字段 | `module-inventory/.../ErpInvStockBalance`（`availableQuantity` = onHand − reserved − locked） | 字段存在，但**无任何 mfg 域 writer**（reserved 恒为 0 from mfg 侧） |

**核心裁决事实**：UC-MFG-05/08 字面要求的预留写路径在制造域**完全未实现**（owner doc `material-reservation.md:9-16` 文首 Deferred 实现说明已显式声明，执行时复核 HEAD 仍成立）。仅 `KitAvailabilityChecker` 做只读齐套校验。

---

## 3. 测试证据（L4 测试断言 + 断言强度）

| 测试 | 文件:方法 | 覆盖 | 断言强度 |
|------|----------|------|---------|
| MRP 引擎 | `TestErpMfgMrpEngine.java` + `TestErpMfgMrpEndToEnd.java` + `TestErpMfgMrpSimulation.java` | MRP 净需求/低层码/释放/仿真 | **强**（A2.6b/A4.2b 已评级，复用） |
| 齐套只读路径（docStatus 决定） | `TestErpMfgWorkOrderStateMachine.java:76,96-97,108-109,125`（`assertEquals(WORK_ORDER_STATUS_STOCK_RESERVED, ...)` / `WORK_ORDER_STATUS_STOCK_PARTIAL`） | 齐套校验只读路径 → docStatus 转换 | **强**（断言 docStatus 状态） |
| **预留写路径** | —（无） | — | **无测试**（功能 Deferred，与 P1-MA5-006 successor 注记一致） |

**结论**：MRP 引擎 + 齐套只读路径有强测试覆盖；**预留写路径（UC-MFG-05/08 核心）零测试**（被测功能不存在，R3.0 已裁决 successor 注记）。

---

## 4. 运行时行为证据（L5，复用 A2.6b/A4.2b + 本切片差异）

### 4.1 复用 A2.6b 已证实行为（MRP 引擎主链）

- MRP 运算 `RUNNING→COMPLETED` 写入 + 释放路径跨域 saveEntity + `markFirmed` + `advancePlanToFirmedIfComplete` 全链经 @BizMutation 事务回滚保证（A2.6b 零 P0 三候选证伪之一/二）。
- 委外释放 config-gated `erp-mfg.subcontract-release-enabled` 默认 false 控制风险（A2.6b 零 P0 三候选证伪之三）。
- 仿真 E2 fork 单次 MRP 路径零触及（`SimulationMrpEngine` 不调 `MrpEngine.runMrp`）。

### 4.2 本切片补的差异（预留写路径运行时行为）

- **预留写路径运行时行为 = 不存在**（功能 Deferred）。
- 当前运行时实际行为：工单审核 → `KitAvailabilityChecker` 只读齐套 → docStatus 置 STOCK_RESERVED/STOCK_PARTIAL（不写预留量）；开工 → 领料出库移动单 DONE 扣减 onHand（不经预留）；取消/完工 → 无预留释放（reserved 从未写入）。
- **运行时后果**：多工单可同时通过齐套校验（availableQuantity 含 reserved 项但 reserved 恒为 0，每工单见全量 onHand），实际领料时按 FIFO/移动单顺序扣减。若领料超出实际 onHand，由 stock move bookkeeper 的 negative-stock 防护拒绝（P0-MA2-020 UK + 余额守恒不变量兜底），故**不构成 §2 P0① 活跃数据破坏**（数据破坏由 stock move 层独立防护，预留层缺失是规划/协调信号缺失）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论 + 预留写路径 Deferred Q4 裁决）

### 5.1 五级追踪矩阵（每 UC 一行）

| UC | L1 需求契约（逐字） | L2 owner doc（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 |
|----|-------------------|-----------------------|-----------|-----------|-------------|
| **UC-MFG-05** 工单审核触发物料预留（4 断言：①BOM 展开创建 MaterialReservation ②预留量=min(需求,可用) ③库存余额.预留量+= ④reservationStatus=RESERVED） | `use-cases.md:90-104`（§1 逐字引用） | `material-reservation.md:9-16`（文首 Deferred 实现说明）+ `:71-106`（§预留流程，**整节 Deferred**）+ `:67`（reservationStatus 6 态 Deferred）。**L2 与 L1 冲突裁决：以 L1 为准，L2 推定已向实现妥协**（§4 Q1） | 制造域：**断言①②③④全未实现** — `KitAvailabilityChecker.java:62-89` 只读齐套（不写预留）+ `ErpMfgWorkOrderProcessor` 无预留创建 + `ErpMfgMaterialReservation` 未物化 + `ErpMfgWorkOrder` 无 reservationStatus 字段 + `ErpMfgConstants` 无 reservation config key。跨域：`ErpInvReservationBizModel.java`（15 行 CRUD 桩，无 purpose-built 写接口） | **零测试**（功能 Deferred，与 P1-MA5-006 successor 一致） | 预留写路径**不存在**；运行时 = 只读齐套决定 docStatus + 领料移动单扣减 onHand |
| **UC-MFG-08** 工单取消/完工释放预留（3 断言：⑤CANCELLED/COMPLETED 释放未领料预留 ⑥库存余额.预留量-= ⑦MaterialReservation.状态=RELEASED） | `use-cases.md:144-156`（§1 逐字引用） | `material-reservation.md:9-16`（文首 Deferred）+ `:184-216`（§预留释放，**整节 Deferred**）。**L2 与 L1 冲突裁决：以 L1 为准** | 制造域：**断言⑤⑥⑦全未实现** — 无释放预留写实现（CANCELLED/COMPLETED 路径无预留释放调用）+ reserved 从未写入故无可减 + MaterialReservation 实体不存在故无 RELEASED 状态 | **零测试** | 释放路径**不存在**；运行时 = 取消/完工不触发任何预留释放（因预留从未写入） |

### 5.2 每 UC 符合性结论（§2 判据）

| UC | 结论 | 命中判据 | 三源对照 |
|----|------|---------|---------|
| **UC-MFG-05** | **P1** | §2 P1①（功能完全缺失）+ §2 P1⑤（测试断言完全缺失） | L1 要求 4 断言全部写入；L3 全未实现；L4 零测试 |
| **UC-MFG-08** | **P1** | §2 P1①（功能完全缺失）+ §2 P1⑤（测试断言完全缺失） | L1 要求 3 断言全部写入；L3 全未实现；L4 零测试 |

### 5.3 核心裁决：预留写路径 Deferred 的 Q4 显式论证（非悬空"待查"）

#### (a) 预留量管控是否属 §2 "数据安全/会计正确性"强制类 vs "业务便利/优化"类

- **预留量管控属性裁决**：库存可用量/物料分配协调类。预留量用于**跨工单物料分配协调**（防止多工单同时承诺同一库存），非"会计过账正确性"（不写 GL/不涉凭证），非纯"数据便利"（涉及库存可用量信号正确性）。
- **是否命中 §2 P0① "活跃数据破坏防护未实现"**：**不命中**。预留写缺失不直接破坏活跃数据 —— 库存余额守恒由 stock move bookkeeper 独立防护（P0-MA2-020 UK + 余额守恒不变量 + negative-stock 拒绝）。预留层缺失的运行时后果 = 多工单可同时通过齐套校验（规划信号缺失），但实际领料扣减由 stock move 层顺序处理，不产生负库存或余额分裂。
- **是否命中 §2 P0③ "核心业务循环断裂"**：**不命中**。工单 → 齐套 → 开工 → 领料 → 完工循环完整（齐套只读 + 领料扣减 onHand），仅缺独立的预留追踪子循环。
- **是否命中 §2 P0④ "会计过账正确性破坏"**：**不命中**。预留不涉会计过账。
- **P0 裁决**：**非 P0**（三个 P0 判据均不命中）。
- **P1 裁决**：**命中 §2 P1①（功能完全缺失）**——UC-MFG-05/08 字面要求的预留写路径在制造域完全未实现。

#### (b) 当前只读齐套 + 领料移动单 DONE 扣减是否构成"功能等价"

- **不等价**。L1 字面要求预留**独立追踪**：
  - MaterialReservation 实体（每子件一条，含 requiredQty/reservedQty/pickedQty/releasedQty/reservationStatus 6 态字段）—— 用于跨工单协调 + pegging 追溯；
  - 工单 reservationStatus 独立维度（与 docStatus/approveStatus 三轴）—— 用于独立追踪预留进度；
  - 库存余额.预留量 += / -= —— 用于跨工单可用量信号同步。
- 当前实现：无 MaterialReservation 实体、无 reservationStatus 字段、无预留量 writer。多个工单可同时通过齐套校验（每工单见全量 onHand，reserved 恒为 0），取消/完工不释放（因从未写入）。
- **不等价裁决**：L1 要求的"独立追踪 + 跨工单协调 + 取消/完工释放"在当前实现中**完全缺失**，非"功能等价的简化实现"。

#### (c) Deferred 是否有合法人工批准（§4 三判据，裁决是否需 Q4 重开）

- **判据 (i) plan 含独立 plan-audit 通过记录**：本切片预留 Deferred 无独立 plan-audit 通过记录（material-reservation.md 的 Deferred 标注是 owner-doc 维护，非经独立 plan-audit 裁决）。
- **判据 (ii) owner doc 显式 documented simplification 标注且经人工批准**：material-reservation.md:9-16 有显式 Deferred 标注，但**无人工批准痕迹**（无 git log/commit/discussion 可追溯的人工批准记录）。**owner doc 自标 Deferred 按 §4 不算人工批准证据标准**（对齐 MA2 §"显式人工批准记录" + 方法论 §4：(ii) 须经"人工批准痕迹"，AI 自写标注不算）。R2.6（plan 2026-07-31-0310-1）的 owner-doc Deferred 正式化是 **audit-remediation 文本一致性维度**的裁决（doc↔code drift 修复），其方案 B 关闭**不适用 requirement-compliance 需求契约维度**（§去重协议：本 mission MA2 文本一致性 ≠ 需求契约符合性）。
- **判据 (iii) product-scope 范围裁剪登记**：`docs/requirements/product-scope.md` 未将物料预留列入"不在范围"或"后续阶段"（预留是 mfg 核心 UC-MFG-05/08，属产品基线内）。
- **三判据裁决**：**(i)(ii)(iii) 均不成立**。owner doc 自标 Deferred **不算合法人工批准**，按 §5 Q4 须**重开为 P1 修复义务**（P0/P1 必须实现，禁止方案 B 关闭，唯一合法出口 = 需求本身不合理经人工批准改 product-scope —— 本切片预留需求合理，不触发该出口）。

#### (d) Q4 修复义务绑定 + MR 通道

- **级别**：P1（非 P0，不触发 MR0 即时通道）。
- **目标 MR**：**MR1**（R1.0 展开为 RC-R1.n）。
- **修复触及保护区域**（§5）：
  - **ORM 结构变更**：`ErpMfgWorkOrder` 加 `reservationStatus` 字段 **或** 新增 `ErpMfgMaterialReservation` 实体（须 ask-first + 独立 plan-audit）；
  - **跨域写**：库存域 `IErpInvReservationBiz` 加 purpose-built 预留写方法（createReservation/releaseReservation/consumeReservation）+ 制造域接线（审核/取消/完工 hook）；
  - **库存可用量管控**：`ErpInvStockBalance.reservedQty` writer 注入（库存可用量是 inventory 域保护区域相邻）。
- **successor 已命名**：`requirement-compliance-roadmap.md` A3.2（mfg + inventory + purchase 域 successor 复查，含物料预留实现）已显式追踪（状态 `todo`）；P1-MA3-042（audit-remediation，doc-drift 维度 resolved R2.6）+ P1-MA5-006（测试维度 successor 注记）为不同维度的既有追踪。

### 5.4 resolved finding + Deferred 追踪 HEAD 复核（HEAD `5953f07c1`）

#### (a) A2.6b P1-MA2-036/037/038 在当前 HEAD 实际落地（按逻辑非行号核验）

| Finding | HEAD `5953f07c1` 实测 | 裁决 |
|---------|----------------------|------|
| **P1-MA2-036**（MRP CANCELLED + 预测 CONSUMED dict 死状态） | `ErpMfgConstants.java:95` `MRP_STATUS_CANCELLED` 仅常量定义零 writer（方案 A：保留 dict 为预留 + owner doc Deferred 标注）；预测 CONSUMED 同（仅 cancel 守卫只读引用） | **resolved 维持**（R1.14 done，方案 A 实施于 HEAD） |
| **P1-MA2-037**（mrp.md RELEASED vs isFirmed 措辞漂移） | `mrp.md:69` 已改述为「行级 isFirmed=true（markFirmed）；全部行 firmed 后计划头 → FIRMED（advancePlanToFirmedIfComplete）」+ `:98` 有「释放状态措辞更正：RELEASED → FIRMED/isFirmed」显式注记 | **resolved 维持**（R1.14 done） |
| **P1-MA2-038**（MrpReleaseService 委外单 APPROVED O-4 豁免登记缺失） | `docs/architecture/posting-exemptions.md §MrpReleaseService:8,13,19,28` 已补登委外单（ErpMfgSubcontractOrder/ErpMfgSubcontractOrderLine）豁免条目（含 config-gated 默认 false + 加工费=0 待补录 + postedStatus=DRAFT 不自动过账） | **resolved 维持**（plan 2026-07-29-2225-1 done） |

**结论**：3/3 resolved finding 在 HEAD 实际落地，无回退。

#### (b) 预留 Deferred 既有追踪核验（arm-index + backlog）

| 既有追踪 | 维度 | 状态 | 与本切片 RC finding 关系 |
|---------|------|------|-------------------------|
| **P1-MA3-042**（arm-index §P1 详细清单） | audit-remediation owner-doc drift（doc↔code 文本一致性） | **resolved R2.6**（owner doc 整节标 Deferred + 持久化真相源指向库存域） | **不同维度**（文本一致性 vs 需求契约符合性）；R2.6 方案 B 关闭**不关闭本切片需求契约 P1**（§去重协议） |
| **P1-MA5-006**（arm-index §P1 详细清单，归并） | audit-remediation 测试覆盖深度 | successor 注记（R3.0 裁决：被测功能不存在→无测试可补） | **不同维度**（测试覆盖 vs 需求契约符合性）；预留实现 plan 落地时须含测试义务（successor 触发条件） |
| **A3.2**（requirement-compliance-roadmap.md MA3） | requirement-compliance MA3 successor 复查 | **todo**（mfg + inventory + purchase 域 successor，含物料预留实现） | **同维度同方向**（需求契约 successor）—— 本切片 RC finding 是 A3.2 successor 的**需求契约裁决输入**（定级 P1 + Q4 修复义务） |
| `material-reservation.md:9-16` owner doc Deferred | owner doc 自标 | Deferred 标注（无人工批准痕迹） | **不算合法人工批准**（§4 三判据不满足），按 Q4 须重开 |

**结论**：预留 Deferred 在 audit-remediation（P1-MA3-042 doc-drift + P1-MA5-006 测试）+ requirement-compliance（A3.2 successor）均有追踪。本切片 RC finding 是**需求契约维度的裁决**（P1 定级 + Q4 修复义务 + MR1 通道），与既有追踪互补不重复（详见 §6 衔接裁决）。

### 5.5 MRP 引擎本身符合性（复用 A2.6b）

- MRP 引擎（runMrp/释放/事务原子性/幂等/仿真 fork）**复用 A2.6b pass 结论**：状态机迁移守卫齐全 + @BizMutation 事务边界覆盖全链 + 幂等守卫完整 + 仿真零触及单次路径。
- 本切片对 MRP 引擎本身**不重复审计**（§去重协议），符合性 = **接受**。

---


## 6. 与 arm-index 衔接（复用 or 新增裁决，§7）

### 6.1 产出 finding 前 grep 比对（禁止未经比对直接新建）

`grep arm-index.md mfg 预留/MRP 同域同控制点` 结果：

| 既有 finding | 控制点 | 根因 | 维度 | 与本切片拟新建 finding 的关系裁决 |
|-------------|--------|------|------|----------------------------|
| `P1-MA2-036`（MRP CANCELLED/预测 CONSUMED dict 死状态） | MRP 计划头/预测状态机 dict 死状态 | dict 项无 writer | audit-remediation 状态机 | **不同控制点**（MRP 状态机 dict 死状态 vs 预留写路径缺失），不复用 |
| `P1-MA2-037`（mrp.md RELEASED vs isFirmed） | 建议单释放状态措辞 | owner doc 措辞漂移 | audit-remediation owner-doc drift | **不同控制点**（建议单释放措辞 vs 预留写路径），不复用 |
| `P1-MA2-038`（委外单 APPROVED O-4 豁免登记） | MrpReleaseService 委外写豁免 | O-4 豁免半治理 | audit-remediation 跨域写治理 | **不同控制点**（委外释放豁免 vs 预留写路径），不复用 |
| `P1-MA3-042`（material-reservation.md 整个子系统未实现） | owner doc 声明预留子系统但 code 未实现 | doc↔code drift | audit-remediation owner-doc drift（文本一致性） | **同功能点不同维度**：P1-MA3-042 = doc↔code 文本一致性（resolved R2.6 方案 B 标 Deferred）；本切片 = 需求契约符合性（L1 UC-MFG-05/08 字面 vs 实现，Q4 禁方案 B）。**R2.6 方案 B 关闭不关闭需求契约维度**（§去重协议）。复用其已证实的"预留写路径未实现"事实，但**新建 RC finding** 承载需求契约维度的 Q4 修复义务 |
| `P1-MA5-006`（mfg 物料预留子系统零测试） | 预留子系统零测试 | 被测功能不存在 | audit-remediation 测试覆盖 | **不同维度**（测试覆盖 vs 需求契约符合性）。复用其"预留子系统未实现"事实，但**不承载 Q4 修复义务**（successor 注记）。新建 RC finding 与其互补 |
| `A3.2`（requirement-compliance MA3 successor） | mfg+inv+pur successor 复查（含物料预留实现） | successor 触发条件 | requirement-compliance successor | **同维度同方向**（需求契约 successor）—— A3.2 是 successor 复查工作项（todo），本切片 RC finding 是其**需求契约裁决输入**。本切片 finding 入 MR1 修复追踪区，A3.2 successor 触发条件 = 库存域 ErpInvReservation 写接口先行落地后制造域接线 |

### 6.2 新建 finding 裁决

**裁决：新建 `P1-RC-008`**（下一个序号，承接 P1-RC-001~007）。

**与既有 finding 的差异依据**：
- **vs P1-MA3-042**：根因不同点（doc↔code 文本一致性 vs 需求契约符合性）；控制点不同点（owner doc 声明 drift vs L1 验收标准字面）；维度不同点（audit-remediation 文本一致性可方案 B 关闭 vs requirement-compliance 需求契约 Q4 禁方案 B）。R2.6 方案 B 关闭（owner doc Deferred）是 audit-remediation 维度的合法关闭，但**不关闭需求契约维度**——按 §去重协议，本 mission MA2（方案 B 关闭项复查）与 audit-remediation MA3（owner-doc drift）边界不同，本 mission MA1 只补需求契约差异。
- **vs P1-MA5-006**：维度不同（测试覆盖 vs 需求契约符合性），P1-MA5-006 是 successor 注记（无修复义务），本 finding 承载 Q4 P1 修复义务。
- **vs A3.2**：A3.2 是 successor 复查工作项（容器/触发器），本 finding 是其需求契约裁决输入（具体 P1 定级 + MR1 修复行）。

### 6.3 双向可追溯

- **finding → 修复**：`P1-RC-008` 目标 MR1（R1.0 展开为 RC-R1.n 实体行），修复行须含 finding ID 交叉引用。
- **修复 → finding**：MR1 RC-R1.n 修复完成后回填 arm-index `P1-RC-008` 修复状态。
- **finding → successor**：本 finding 修复依赖 A3.2 successor 触发条件（库存域 ErpInvReservation 写接口先行落地）。
- **MV V.3 校验**：closure audit 核验 `P1-RC-008` 修复状态为 `done` 或显式 successor。

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行。

| # | 存疑点 | 触发条件 | 交 MA4 展开 |
|---|--------|---------|------------|
| SP-1 | **预留量并发扣减运行时行为**：当前 reserved 恒为 0（无 writer），多工单同时通过齐套校验后并发领料时，stock move bookkeeper 的 negative-stock 防护（P0-MA2-020 UK + 余额守恒）是否在所有并发场景下兜底（无 silent split-quantity corruption） | 多工单并发领料同一物料同一仓库 | A4.1 运行时并发探针 |
| SP-2 | **STOCK_PARTIAL 强制开工后领料可用量校验运行时**：部分齐套强制开工（UC-MFG-04，config-gated consumption != STRICT）后，缺件部分后续补料时，KitAvailabilityChecker 只读路径是否正确反映补料后的可用量（无缓存/无陈旧读） | STOCK_PARTIAL 强制开工后补料 | A4.1 运行时探针 |
| SP-3 | **预留实现后 reservedQty 与 availableQuantity 一致性运行时**：当 MR1 RC-R1.n 修复落地（预留写路径实现）后，`ErpInvStockBalance.reservedQty` 与 `availableQuantity`（= onHand − reserved − locked）的实时一致性 + 跨工单并发预留的 lost-update 防护（versionProp 是否覆盖预留写） | MR1 修复落地后 | A4.1 successor（修复落地后展开） |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（预留写路径缺失经 §2 P0①③④ 三判据均不命中裁决为 P1），故**不触发 MR0**。无 R0.n 实体行追加。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总表见下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-008）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1 比对表），无未经比对直接新建的 finding。

### actual vs baseline 汇总表（HEAD `5953f07c1`，2026-08-02 实测）

| 规则 | 描述 | actual | baseline | 漂移 |
|------|------|--------|----------|------|
| R1a | dao().saveEntity (BizModel) | 0 | 0 | 0 |
| R1b | dao().updateEntity (BizModel) | 0 | 0 | 0 |
| R1c | dao().getEntityById (BizModel) | 0 | 0 | 0 |
| R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 0 |
| R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 |
| R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | 0 |
| R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | 0 |
| R2d | Processor daoFor(ErpMd*) | 34 | 34 | 0 |
| R3 | new Erp*() 构造实体 | 5 | 5 | 0 |
| R4 | extends RuntimeException | 0 | 0 | 0 |
| R5 | @Inject private | 0 | 0 | 0 |
| R6 | @Transactional in BizModel | 2 | 2 | 0 |
| R7 | System.currentTimeMillis() | 0 | 0 | 0 |
| R8 | Processor 无 xbiz 接线 | 0 | 0 | 0 |
| R10 | REQUIRES_NEW 事务 | 6 | 6 | 0 |
| R11 | Processor 重复状态判断方法 | 0 | 0 | 0 |
| R12a | 共享内核 import ErpFinBusinessType | 69 | 69 | 0 |
| R12b | 共享内核 import PostingEvent | 66 | 66 | 0 |
| R12c | 共享内核 import AcctSchemaResolver | 40 | 40 | 0 |

**汇总**：全 19 可计数规则 actual **精确等于** baseline，**0 漂移**（0 regression + 0 improvement）。本审计为**只读审计**（无生产代码/ORM/api.xml/view.xml/真相源变更），checker **无回归风险**（纯 reporter，退出码 0；本报告不以退出码 0 作为门控通过依据，对齐 R6.9 教训）。

---

## 报告 9 段完整性自检

| # | 段落 | 存在 | 备注 |
|---|------|------|------|
| 1 | 需求契约原文（L1 逐字引用） | ✅ | UC-MFG-05/08 7 断言逐字 |
| 2 | 实现证据（L3 file:line + 跨域链） | ✅ | MRP 主链 + 预留写路径实测 |
| 3 | 测试证据（L4 + 断言强度） | ✅ | MRP 强 + 齐套只读强 + 预留零测试 |
| 4 | 运行时行为证据（L5） | ✅ | 复用 A2.6b + 本切片差异 |
| 5 | 符合性结论（矩阵 + 每 UC + Q4 裁决） | ✅ | UC-MFG-05/08 均 P1 + Q4 显式论证 |
| 6 | 与 arm-index 衔接（复用 or 新增） | ✅ | 新建 P1-RC-008 + 比对表 |
| 7 | 静态存疑点清单 | ✅ | SP-1/SP-2/SP-3 |
| 8 | 过程纪律自检段 | ✅ | checker actual=baseline 0 漂移 + 独立性 + 去重 |
| 9 | 与 MA2/MA4 报告差异增量声明 | ✅ | 前置声明（报告开头） |

**9 段齐全，完整性自检 PASS。**

---

## Verdict

- **UC-MFG-05**（工单审核触发物料预留）：**P1**（§2 P1① 功能完全缺失 + §2 P1⑤ 测试断言完全缺失）→ `P1-RC-008`
- **UC-MFG-08**（工单取消/完工释放预留）：**P1**（§2 P1① + §2 P1⑤）→ `P1-RC-008`（与 UC-MFG-05 同一 finding，同一预留写路径缺失根因）
- **MRP 引擎本身**（runMrp/释放/事务原子性）：**接受**（复用 A2.6b pass 结论）
- **resolved finding HEAD 复核**：P1-MA2-036/037/038 在 HEAD `5953f07c1` **3/3 已落地无回退**
- **新 finding**：1 项 `P1-RC-008`（预留写路径 Deferred 需求契约 P1，目标 MR1，修复触及 ORM 结构变更 + 跨域写 + 库存可用量管控，须 ask-first + 独立 plan-audit）
- **P0 即时通道**：未触发（本切片无 P0）

**整体 Verdict**：⚠️(P1) — 2 UC 均 P1（预留写路径需求契约未实现，Q4 禁方案 B 关闭，须经 MR1 实现），零 P0。
