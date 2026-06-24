# 资产域用例规格(Assets Use Cases)

> 从使用场景出发组织资产域可验证用例。机制细节引用不重复(指向 state-machine / depreciation-and-posting)。
> 资产核心是卡片生命周期 + 折旧计提 + 资本化 + 处置 + 价值调整,全部联动 finance 凭证。

## 状态轴速查(详见 state-machine.md §1)

```
卡片状态:  DRAFT / IN_SERVICE / IDLE / SCRAPPED / SOLD
折旧计划条目:  PENDING / EXECUTED / REVERSED
```

---

## UC-AST-01 设备购置资本化入账

**场景**:购入固定资产,资本化入账(DRAFT→IN_SERVICE),生成入账凭证与折旧计划。

**可验证断言**(见 depreciation-and-posting.md §二、state-machine.md §2):
```
卡片: DRAFT → IN_SERVICE(资本化)
生成入账凭证: 借 固定资产, 贷 在建工程/银行存款/应付
自动生成折旧计划(按折旧方法/年限/残值)
卡片.已过账 == true
```

**涉及机制**:depreciation-and-posting.md §二、state-machine.md

---

## UC-AST-02 期末直线法折旧

**场景**:期末批量计提折旧,生成折旧凭证。

**可验证断言**(见 depreciation-and-posting.md §1):
```
期末折旧任务 →
  查所有 IN_SERVICE 资产
  计算月折旧额 = (原值 - 残值) / 使用月数   (直线法)
  残值约束: 计提后净值不低于残值(低于则截断为0)
生成凭证: 借 折旧费用, 贷 累计折旧
折旧计划条目: PENDING → EXECUTED
卡片.累计折旧 += 月折旧额, 卡片.净值 -= 月折旧额
```

**涉及机制**:depreciation-and-posting.md §1.2/§1.3

---

## UC-AST-03 资产闲置停提与恢复

**场景**:资产暂时闲置,停提折旧;恢复使用后继续计提。

**可验证断言**(见 state-machine.md §2):
```
IN_SERVICE → IDLE(闲置): 期间不参与折旧计提
IDLE → IN_SERVICE(恢复): 恢复计提
闲置期间不计提(折旧计划跳过)
```

**涉及机制**:state-machine.md §2

---

## UC-AST-04 资产报废处置

**场景**:资产报废,结转原值/累计折旧,记录清理损失。

**可验证断言**(见 depreciation-and-posting.md §三、state-machine.md):
```
IN_SERVICE → SCRAPPED(报废)
生成处置凭证:
  借 累计折旧(结转), 固定资产清理
  贷 固定资产(原值结转)
清理损失 = 账面净值 - 处置收入(报废收入常为0)
损失计入营业外支出
卡片状态终态(不可恢复)
```

**涉及机制**:depreciation-and-posting.md §三、state-machine.md §3

---

## UC-AST-05 资产出售处置

**场景**:资产出售,计算清理收益/损失。

**可验证断言**(见 depreciation-and-posting.md §三):
```
IN_SERVICE → SOLD(出售)
清理损益 = 处置收入 - 账面净值
若 > 0: 收益(贷 营业外收入)
若 < 0: 损失(借 营业外支出)
先补提当期折旧至出售日(确保累计折旧准确)
```

**涉及机制**:depreciation-and-posting.md §三

---

## UC-AST-06 在建工程转固

**场景**:在建工程竣工,结转余额到固定资产。

**可验证断言**(见 depreciation-and-posting.md §二):
```
在建工程余额 → 结转到 固定资产
生成转固凭证: 借 固定资产, 贷 在建工程
转固后开始折旧(生成折旧计划)
```

**涉及机制**:depreciation-and-posting.md §二

---

## UC-AST-07 折旧漏提补提

**场景**:前期漏提折旧,需补提(反结账 vs 当期补提)。

**可验证断言**(见 state-machine.md §4):
```
方式A(反结账): 反结账漏提期间 → 补提 → 重新结账(严格,影响已结账数据)
方式B(当期补提): 当期一次性补提前期漏提额(简化,不追溯)
补提凭证标注所属期间(审计)
```

**涉及机制**:state-machine.md §4、../finance/period-close.md

---

## UC-AST-08 期末批量折旧容错

**场景**:期末批量折旧时,单资产失败不影响其他资产。

**可验证断言**(见 depreciation-and-posting.md §五):
```
批量折旧 → 并行处理各资产
单资产失败(如科目缺失) → 隔离, 不影响其他资产
汇总成功凭证, 失败资产标记待处理
失败资产可单独重试
```

**涉及机制**:depreciation-and-posting.md §五

---

## UC-AST-09 资产盘点

**场景**:定期盘点固定资产,生成盘点单,差异生成调整。

**可验证断言**:
```
盘点单(范围:部门/类别) → 录入实盘数量
差异 = 实盘 - 账面(卡片.数量)
盘盈 → 生成资产卡片增加(价值评估入账)
盘亏 → 触发处置流程(UC-AST-04 报废)或调查
盘点差异生成调整凭证(借/贷固定资产, 差额)
```

**涉及机制**:depreciation-and-posting.md §四(价值调整)、state-machine.md

---

## UC-AST-10 资产维修

**场景**:固定资产维修,费用归集(可资本化或费用化)。

**可验证断言**:
```
维修单(关联资产卡片) → 记录维修费用
若 维修延长寿命/提升效能 → 资本化(增加原值, 重算折旧计划)
否则 → 费用化(借 维修费用, 贷 存货/银行)
资本化维修: 卡片.原值 += 资本化金额, 折旧计划调整
维修费用可关联维护域(ErpMntVisit, 若设备资产)
```

**涉及机制**:depreciation-and-posting.md §二(资本化)/§四、../maintenance

---

## UC-AST-11 资产拆分与合并

**场景**:一张资产卡片拆分为多张,或多张合并为一张。

**可验证断言**:
```
// 拆分
原卡片 → 拆为 N 张新卡片
新卡片.原值/累计折旧/净值 按 proportion 分配
Σ 新卡片.原值 == 原卡片.原值(平衡)
原卡片状态 → SCRAPPED 或保留(按配置), 生成拆分凭证

// 合并
N 张卡片 → 合并为 1 张
新卡片.原值 = Σ 原卡片.原值
新卡片.累计折旧 = Σ 原卡片.累计折旧
原卡片状态 → SCRAPPED, 生成合并凭证
拆分/合并不影响总账平衡(总资产不变)
```

**涉及机制**:state-machine.md、depreciation-and-posting.md

---



---

## UC-AST-12 资产看板

**场景**:资产看板的指标展示与异常预警。见 ../dashboards.md §资产看板。

**可验证断言**:
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  原值/累计折旧/净值, 本期折旧, 类别分布, 折旧未计提预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**涉及机制**:../dashboards.md、各域 state-machine.md、roles-and-permissions.md(行级权限)

## 用例与测试的衔接

- 折旧(U02)→ 核心:计算公式 + 残值约束 + 凭证方向
- 处置(U04/U05)→ 清理损益计算 + 终态不可恢复
- 业财联动(U01/U06)→ 资本化/转固凭证
- 容错(U08)→ 批量隔离

## 参考机制文档

- `state-machine.md` — 卡片状态/迁移/终态/异常
- `depreciation-and-posting.md` — 折旧/资本化/处置/价值调整/批量/科目映射
- `../finance/posting.md` — 业财过账
- `../finance/period-close.md` — 期间控制(反结账补提)
