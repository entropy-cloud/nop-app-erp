# 制造域用例规格(Manufacturing Use Cases)

> 从使用场景出发组织制造域可验证用例。机制细节引用不重复(指向 state-machine / bom-and-routing / material-reservation)。
> 制造核心是工单全流程 + BOM 展开 + 齐套校验 + 物料预留 + 完工成本结转。

## 状态轴速查(详见 state-machine.md)

```
工单状态(10态):  DRAFT / SUBMITTED / NOT_STARTED / STOCK_PARTIAL / STOCK_RESERVED /
                  IN_PROCESS / COMPLETED / STOPPED / CLOSED / CANCELLED
作业卡(8态):     OPEN / WORK_IN_PROGRESS / PARTIALLY_TRANSFERRED / MATERIAL_TRANSFERRED /
                  ON_HOLD / SUBMITTED / COMPLETED / CANCELLED
预留状态:         UNRESERVED / PARTIAL_RESERVED / RESERVED / PARTIAL_PICKED / PICKED / RELEASED
```

---

## UC-MFG-01 工单正常生产全流程

**场景**:工单从创建到完工入库的完整流程。

**行为链路**:
```
创建工单(产成品/数量/BOM) → 提交 → 审核
→ 齐套校验(BOM 展开 vs 可用量)
→ 预留子件(更新库存预留量)
→ 开工(IN_PROCESS) → 领料 → 报工(作业卡)
→ 完工入库(COMPLETED) → 成本结转
```

**可验证断言**:
```
工单.状态 流转: DRAFT→SUBMITTED→(STOCK_RESERVED|STOCK_PARTIAL)→IN_PROCESS→COMPLETED
齐套: BOM 展开子件需求 × 产出量, 与 可用量比较
完工入库: 库存余额[产成品] += 完工数量
成本结转: 材料成本 + 人工成本 + 制造费用 → 产成品存货估值凭证(见 bom-and-routing §成本计算)
```

**涉及机制**:state-machine.md §1/§2、bom-and-routing.md、material-reservation.md

---

## UC-MFG-02 多级 BOM 展开(phantom 虚拟件)

**场景**:BOM 含虚拟件(phantom),展开时虚拟件不建生产订单,其子件直接展开。

**可验证断言**(见 bom-and-routing.md §展开):
```
BOM.is_phantom == true 的组件 →
  不生成该组件的生产订单
  其子件直接展开到当前工单的物料需求
齐套校验基于展开后的全部子件(含虚拟件子件)
```

**涉及机制**:bom-and-routing.md §多级 BOM 展开

---

## UC-MFG-03 齐套校验

**场景**:工单审核时校验子件是否齐套,决定后续状态。

**可验证断言**(见 material-reservation.md §齐套、state-machine.md §2):
```
齐套校验 = BOM 展开子件需求 vs 可用量(物料×仓库)
全部满足 → 工单.状态 = STOCK_RESERVED
部分满足 → 工单.状态 = STOCK_PARTIAL
齐套状态决定可否生产(配置 kitting-required)
```

**涉及机制**:material-reservation.md §齐套、state-machine.md

---

## UC-MFG-04 部分齐套强制开工

**场景**:配置允许且主管权限下,部分齐套(STOCK_PARTIAL)可强制开工。

**可验证断言**(见 state-machine.md §2/§6):
```
配置 erp-mfg.kitting-required == false 或 主管权限
STOCK_PARTIAL → 可迁移到 IN_PROCESS(强制开工)
缺件部分后续补料
```

**涉及机制**:state-machine.md §2/§6

---

## UC-MFG-05 工单审核触发物料预留

**场景**:工单审核时,按 BOM 展开子件创建预留,更新库存预留量。

**可验证断言**(见 material-reservation.md §预留流程):
```
工单.审核通过 →
  BOM 展开 → 创建 MaterialReservation(每个子件一条)
  执行预留: 预留量 = min(需求量, 可用量)
  库存余额.预留量 += 预留量
  工单.reservationStatus = RESERVED(或 PARTIAL_RESERVED)
```

**涉及机制**:material-reservation.md §预留流程、../inventory(预留量 owner)

---

## UC-MFG-06 领料扣减预留

**场景**:工单领料,扣减预留量与现有量。

**可验证断言**(见 material-reservation.md §领料):
```
领料单.数量 <= 预留剩余量(超预留拒绝或警告 erp-mfg.over-pick-warning)
领料后:
  MaterialReservation.pickedQty += 领料量
  MaterialReservation.reservedQty -= 领料量(预留转消耗)
  库存余额.现有量 -= 领料量
  库存余额.预留量 -= 领料量
```

**涉及机制**:material-reservation.md §领料

---

## UC-MFG-07 工单完工入库与成本结转

**场景**:完工入库,结转材料+人工+制造费用到产成品成本。

**可验证断言**(见 bom-and-routing.md §成本计算):
```
完工入库 →
  库存余额[产成品] += 完工数量
  产成品单位成本 = (材料 + 人工 + 制造费用) / 完工数量
  生成存货估值凭证: 借 产成品存货, 贷 在制品(WIP)/各成本要素
材料成本 = Σ 领料单成本
人工成本 = Σ JobCard.工时 × 费率
制造费用 = Σ 工序.工时 × 费率(见 bom-and-routing §BomOperation)
```

**涉及机制**:bom-and-routing.md §成本计算、../finance/costing-methods.md

---

## UC-MFG-08 工单取消/完工释放预留

**场景**:工单取消或完工后,释放未消耗的预留。

**可验证断言**(见 material-reservation.md §预留释放):
```
工单.CANCELLED 或 COMPLETED →
  释放未领料的预留(reservedQty - pickedQty)
  库存余额.预留量 -= 释放量
  MaterialReservation.状态 = RELEASED
```

**涉及机制**:material-reservation.md §预留释放

---

## UC-MFG-09 完工质检不合格→返工工单

**场景**:完工检验不合格,新建返工工单。

**可验证断言**(见 state-machine.md §4):
```
完工触发质检(若 BOM.inspection_required) →
  质检 REJECTED → 不合格
  原工单不可恢复(终态), 新建返工工单(关联原工单)
返工工单走标准流程, 产出合格品
```

**涉及机制**:state-machine.md §4、../quality

---

## UC-MFG-10 BOM 变更不影响已开工工单(快照原则)

**场景**:BOM 修改后,已开工的工单仍按原 BOM 执行。

**可验证断言**(见 state-machine.md §4):
```
工单审核时快照 BOM(工单行记录当时 BOM 内容)
BOM 后续修改 → 不影响已审核工单的物料需求/成本
新建工单才用新 BOM
```

**涉及机制**:state-machine.md §4

---

## 用例与测试的衔接

- 齐套/预留(U03/U05)→ 跨域测试(mfg→inventory 预留量)
- 成本结转(U07)→ 材料/人工/制造费用归集 + 凭证
- 领料扣减(U06)→ 预留与现有量双扣
- 快照(U10)→ BOM 变更隔离

## 参考机制文档

- `state-machine.md` — 工单/作业卡状态/迁移/异常
- `bom-and-routing.md` — BOM 模型/展开/齐套/成本/工艺/联副产
- `material-reservation.md` — 预留模型/流程/齐套/领料/释放
- `../inventory/cross-domain.md` — 预留量 owner
- `../finance/costing-methods.md` — 成本方法
- `../quality/inspection-integration.md` — 完工质检
