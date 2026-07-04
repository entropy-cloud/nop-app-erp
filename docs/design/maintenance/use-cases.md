# 维护域用例规格(Maintenance Use Cases)

> 从使用场景出发组织维护域可验证用例。机制细节引用不重复(指向 state-machine / equipment-integration)。
> 维护核心是预防性维护调度(时间/运行时长/产量触发)+ 响应性维护(报修)+ 备件消耗闭环 + 停机影响排产 + 设备资产联动。

## 状态轴速查(详见 state-machine.md)

```
维护访问(5态):  DRAFT / SCHEDULED / IN_PROGRESS / COMPLETED / CANCELLED
维护请求(6态):  OPEN / ACCEPTED / COMPLETED / REJECTED / CANCELLED
设备状态(5态):  RUNNING / IDLE / MAINTENANCE / DOWN / DECOMMISSIONED
```

---

## UC-MAIN-01 预防性维护自动调度(时间周期)

**场景**:nop-job 按时间周期(如每月)自动生成维护访问 DRAFT。

**可验证断言**(见 equipment-integration.md §五):
```
nop-job 定时任务 →
  按维护计划(周期类型=时间, 频率) → 生成维护访问(DRAFT)
  套用维护任务模板(标准工时/标准备件)
维护访问.设备/计划时间/任务清单 填充
```

**涉及机制**:equipment-integration.md §五、state-machine.md

**定时作业接线**:UC-MAIN-01 的 nop-job 触发已接线（plan 2026-07-05-0306-1，SCHEDULED）：`ErpMntDueVisitJob` + `app-service.beans.xml` `<bean>` + `scheduler.yaml` 条目。cron 门控键 `erp-mnt.due-visit-cron`（默认空=跳过；非空时以 `LocalDate.now()` 为基准调 `IErpMntScheduleBiz.generateDueVisits()`）。登记于 `docs/architecture/job-scheduling.md` §3.13 `erp-mnt-due-visit-generation`。

---

## UC-MAIN-02 运行时长触发维护

**场景**:设备累计运行时长达到阈值(如每 5000 小时),触发维护。

**可验证断言**(见 equipment-integration.md §五):
```
周期类型=运行时长
设备累计运行时长 >= 阈值 → 生成维护访问(DRAFT)
运行时长来源: 设备状态记录(RUNNING 时长累计)
```

**涉及机制**:equipment-integration.md §五

---

## UC-MAIN-03 维护访问全流程

**场景**:维护访问从排程到完成,设备状态联动。

**行为链路**:
```
维护访问 SCHEDULED → IN_PROGRESS(开始, 设备→MAINTENANCE)
  → 执行任务 + 消耗备件
  → COMPLETED(完成, 设备→RUNNING/IDLE)
```

**可验证断言**(见 state-machine.md §2):
```
维护访问 IN_PROGRESS 时: 关联设备.状态 = MAINTENANCE
COMPLETED 时: 设备.状态 恢复(RUNNING/IDLE, 取决于排产)
设备状态由维护访问状态驱动(见 equipment-integration §三)
```

**涉及机制**:state-machine.md §2、equipment-integration.md §三

---

## UC-MAIN-04 备件消耗闭环

**场景**:维护访问消耗备件,触发出库移动单与维修费用凭证。

**可验证断言**(见 equipment-integration.md §二):
```
维护访问记录备件消耗 →
  调用 IErpInvStockMoveBiz.generateConsumptionMove(出库)
  库存余额[备件] -= 消耗量
  生成凭证: 借 维修费用, 贷 存货
备件不足 → 校验失败(见 ../inventory cross-domain)
```

**涉及机制**:equipment-integration.md §二、../inventory/cross-domain.md、../finance/posting.md

---

## UC-MAIN-05 报修响应性维护

**场景**:报修请求受理后生成维护访问。

**可验证断言**(见 state-machine.md §维护请求):
```
维护请求 OPEN → 受理 ACCEPTED → 生成维护访问(关联请求)
维护访问 COMPLETED → 请求 COMPLETED
请求 REJECTED/CANCELLED → 不生成维护访问
```

**涉及机制**:state-machine.md §维护请求

---

## UC-MAIN-06 设备故障停机影响排产

**场景**:设备故障停机,发布事件影响制造域工单排产。

**可验证断言**(见 equipment-integration.md §四):
```
设备.状态 = DOWN(故障停机) →
  创建停机记录(DowntimeEntry)
  发布事件(设备停机) →
    制造域接收 → 暂停该设备的工单排产
设备恢复(RUNNING) →
  发布事件(设备恢复) → 恢复排产
```

**涉及机制**:equipment-integration.md §四、../manufacturing

---

## UC-MAIN-07 维护中发现额外故障

**场景**:维护执行中发现额外故障,本次记录并另开请求。

**可验证断言**(见 state-machine.md §4):
```
维护访问 IN_PROGRESS → 发现额外故障
本次访问记录(备注/工时), 不中断本次维护
另开新维护请求(OPEN)处理额外故障
```

**涉及机制**:state-machine.md §4

---

## UC-MAIN-08 设备资产处置联动

**场景**:关联的资产被处置(SCRAPPED/SOLD),设备联动停用。

**可验证断言**(见 equipment-integration.md §一):
```
设备.资产(asset_id 关联) →
  资产 SCRAPPED/SOLD → 设备.状态 = DECOMMISSIONED(停用)
  设备不可再被新维护计划/工单引用
```

**涉及机制**:equipment-integration.md §一、../assets

---

## UC-MAIN-09 排程冲突检测

**场景**:同一设备/人员同时段排程冲突。

**可验证断言**(见 state-machine.md §4):
```
维护访问 SCHEDULED →
  校验: 设备/人员 同时段是否已有排程
  冲突 → 警告或拒绝(配置决定)
```

**涉及机制**:state-machine.md §4

---

## UC-MAIN-10 OEE 计算

**场景**:计算设备 OEE(可用率×性能×质量)。

**可验证断言**(见 equipment-integration.md §六):
```
可用率 = 实际运行时长 / 计划生产时长 (排除停机)
性能效率 = 实际产量 / 理论产量
质量合格率 = 合格品 / 总产量
OEE = 可用率 × 性能效率 × 质量合格率
数据来源: 设备状态记录/工单报工/质检(见 §六)
```

**涉及机制**:equipment-integration.md §六、../manufacturing、../quality

---



---

## UC-MAIN-11 维护看板

**场景**:维护看板的指标展示与异常预警。见 ../dashboards.md §维护看板。

**可验证断言**:
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  设备总数/运行数/待处理请求, OEE, 状态分布, 停机/维护逾期预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**涉及机制**:../dashboards.md、各域 state-machine.md、roles-and-permissions.md(行级权限)

## 用例与测试的衔接

- 调度(U01/U02)→ nop-job 触发 + 模板套用
- 状态联动(U03/U08)→ 维护访问↔设备状态,设备↔资产
- 跨域事件(U06)→ 停机影响排产(事件契约)
- 备件(U04)→ 库存出库 + 凭证
- OEE(U10)→ 多源数据聚合

## 参考机制文档

- `state-machine.md` — 维护访问/维护请求状态/异常
- `equipment-integration.md` — 设备资产关联/备件/设备状态/停机/调度/OEE/事件契约
- `../inventory/cross-domain.md` — 备件出库
- `../assets/depreciation-and-posting.md` — 资产处置联动
- `../manufacturing/state-machine.md` — 工单排产影响
