# 库存域用例规格(Inventory Use Cases)

> 从使用场景出发组织库存域可验证用例。机制细节引用不重复(指向 state-machine / trace-chain / cross-domain)。
> 库存核心是移动单(Move)的流转 + 不可变流水 + 追溯链 + 余量校验(库存是余量校验的全局 owner)。

## 状态轴速查(详见 state-machine.md §1)

```
moveStatus:  DRAFT / CONFIRMED / DONE / CANCELLED
// DONE 的三维语义: 写不可变流水 / 影响余额 / 影响预留量(按入库/出库/调拨不同)
```

---

## UC-INV-01 采购入库移动单全链

**场景**:采购入库单审核触发生成入库移动单,完成库存增加与流水写入。

**行为链路**:见 cross-domain.md §与采购协作
```
采购入库单.审核通过 →
  库存域 generateMove(incoming) → 移动单(DRAFT → CONFIRMED → DONE)
```

**可验证断言**:
```
移动单.状态 == DONE
库存余额[物料, 仓库, 批次].现有量 += 移动数量
存在不可变流水: 关联移动单, 记录 数量/单位成本/余额快照
移动单 DONE 发布事件 → 触发存货估值凭证异步生成(见 cross-domain §与财务协作)
```

**涉及机制**:state-machine.md §1/§2、cross-domain.md

---

## UC-INV-02 销售出库可用量不足拒绝

**场景**:销售出库时库存可用量不足,移动单创建/确认失败,审核回滚。

**可验证断言**(见 state-machine.md §4、cross-domain.md §余量校验):
```
校验维度 = 物料 × 仓库 × 库位 × 批次
若 可用量 < 出库数量:
  generateMove(outgoing) 拒绝
  销售订单审核回滚(approveStatus 保持 SUBMITTED)
  库存余额不变

// 可用量 = 现有量 - 预留量(见 cross-domain §余量校验)
// 负库存配置 erp-inv.allow-negative-stock 决定是否放行
```

**涉及机制**:cross-domain.md §余量校验、state-machine.md §4

---

## UC-INV-03 已完成移动单冲销

**场景**:已 DONE 的移动单需要冲销(如入库错误),生成反向移动单。

**可验证断言**(见 state-machine.md §2/§3):
```
原移动单.冲销 →
  生成反向移动单(新 DRAFT, 数量取负)
  反向单走 DRAFT → CONFIRMED → DONE 流程
DONE 后:
  库存余额按反向数量调整(原+的反-)
  原流水不删除(不可变), 新增反向流水
  追溯链: 反向单.originReturnedMoveId 指向原单
```

**涉及机制**:state-machine.md §2、trace-chain.md §追溯链维护

---

## UC-INV-04 全链路正向追溯

**场景**:从采购入库到销售出库的全链路追溯(物料来源与去向)。

**可验证断言**(见 trace-chain.md §追溯链场景):
```
链路: 采购入库 → 生产领料 → 完工入库 → 销售出库
每环移动单通过 originMoveId / destMoveIds 关联
从销售出库单 可反向追溯到 采购入库单(经中间环节)
从采购入库单 可正向追踪到 所有去向(领料/完工/销售)
```

**涉及机制**:trace-chain.md §追溯链模型/§追溯链场景

---

## UC-INV-05 退货反查原移动单

**场景**:采购退货/销售退货,反查原入库/出库移动单。

**可验证断言**(见 trace-chain.md §追溯链模型):
```
退货移动单.originReturnedMoveId == 原入库/出库移动单.id
原单.returnedMoveIds 包含 退货单.id  (双向)
```

**涉及机制**:trace-chain.md §追溯链模型

---

## UC-INV-06 批次追溯与效期拦截

**场景**:领料继承入库批次;批次过期则拦截出库。

**可验证断言**(见 trace-chain.md §追溯链与批次、state-machine.md §4):
```
领料移动单.批次 == 入库移动单.批次  (批次继承)
若 批次.效期 < 当前日期 且 物料.批次管控 == 强制:
  出库移动单确认失败(批次过期拦截)
完工入库生成新批次(不继承)
```

**涉及机制**:trace-chain.md §追溯链与批次、state-machine.md §4

---

## UC-INV-07 盘点差异生成移动单

**场景**:盘点产生差异,不直接改余额,而是生成盘盈/盘亏移动单走标准流程。

**可验证断言**(见 state-machine.md §盘点单状态机):
```
盘点单.确认 →
  计算差异: 差异 = 实盘数量 - 账面数量
  若 差异 > 0: 生成盘盈移动单(incoming)
  若 差异 < 0: 生成盘亏移动单(outgoing)
  盘点单本身不改余额
盘盈/盘亏移动单走 DRAFT→DONE 后才影响余额
```

**涉及机制**:state-machine.md §盘点单状态机

---

## UC-INV-08 并发扣减乐观锁

**场景**:多个出库移动单并发扣减同一批次,乐观锁保证不超扣。

**可验证断言**(见 state-machine.md §4):
```
并发移动单A、B 扣同一批次:
  一个 DONE 成功, 另一个乐观锁冲突 → 重试或失败
最终 批次.现有量 == 初始 - A - B (不出现负数,除非允许负库存)
```

**涉及机制**:state-machine.md §4

---

## UC-INV-09 负库存放行

**场景**:先发货后入库的场景(配置允许负库存)。

**可验证断言**(见 state-machine.md §4、cross-domain.md §余量校验):
```
配置 erp-inv.allow-negative-stock == true
出库时 可用量 < 出库数量 → 放行(现有量变负)
后续入库移动单补回
若 配置 == false: 拒绝(回到 UC-INV-02)
```

**涉及机制**:state-machine.md §4、cross-domain.md

---

## UC-INV-10 移动单触发存货估值凭证

**场景**:移动单 DONE 后异步生成存货估值凭证(成本来自流水)。

**可验证断言**(见 cross-domain.md §与财务协作):
```
移动单.DONE → 发布事件(post-commit 异步)
→ 生成存货估值凭证(STOCK_MOVE 业务类型)
凭证.金额 来自 移动单.单位成本 × 数量
单位成本由库存流水维护(基线约定,见 cross-domain §数据契约)
入库: 借存货 / 贷暂估应付(GR/IR)
出库: 借销售成本 / 贷存货
```

**涉及机制**:cross-domain.md §与财务协作、../finance/posting.md

---

## 用例与测试的衔接

- 余量校验(U02)→ 核心测试,验证 owner 责任(库存是全局余量校验 owner)
- 追溯链(U04/U05)→ trace-chain 集成测试(正反向遍历)
- 不可变流水(U01/U03)→ 验证流水不可删、冲销新增反向流水
- 业财数据契约(U10)→ 验证事件驱动 + 成本来源

## 参考机制文档

- `state-machine.md` — 移动单状态/三维语义/盘点单/异常路径
- `trace-chain.md` — 追溯链模型/批次追溯/维护规则
- `cross-domain.md` — 跨域调用契约/余量校验 owner/业财数据契约
- `../finance/posting.md` — 存货估值凭证
- `../finance/costing-methods.md` — 单位成本来源
