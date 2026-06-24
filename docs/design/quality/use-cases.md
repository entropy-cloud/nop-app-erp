# 质量域用例规格(Quality Use Cases)

> 从使用场景出发组织质量域可验证用例。机制细节引用不重复(指向 state-machine / inspection-integration / spc)。
> 质量核心是质检触发(来料/制程/完工/出货)+ 结果反馈(合格/让步/不合格)+ NCR-CAPA 闭环 + SPC 过程控制。

## 状态轴速查(详见 state-machine.md)

```
质检单(4态):  PENDING / ACCEPTED / CONDITIONAL / REJECTED
NCR(4态):     OPEN / IN_REVIEW / RESOLVED / CANCELLED
```

---

## UC-QA-01 来料强制质检阻塞流转

**场景**:采购入库的物料标记为强制质检,质检 PENDING 时阻塞入库流转。

**可验证断言**(见 inspection-integration.md §一/§二、state-machine.md):
```
物料.inspection_required == 强制 →
  采购入库审核 → 发布事件(INCOMING) → 创建质检单(PENDING)
  入库流程阻塞(等待质检结果)
质检单 PENDING 期间: 入库单不可继续后续(过账/可用)
质检单 ACCEPTED → 入库继续流转
质检单 REJECTED → 触发退货(见 inspection §二)
```

**涉及机制**:inspection-integration.md §一/§二、state-machine.md

---

## UC-QA-02 质检不合格触发退货

**场景**:来料质检不合格,触发采购退货。

**可验证断言**(见 inspection-integration.md §二/§四):
```
质检单 REJECTED(关键项不合格) →
  创建 NCR(OPEN)
  触发退货: 关联入库单生成退货单
NCR 记录不合格详情
退货走 ../purchase/returns.md 流程
```

**涉及机制**:inspection-integration.md §二/§四、../purchase/returns.md

---

## UC-QA-03 让步接收

**场景**:非关键项不合格,经审批让步接收(CONDITIONAL)。

**可验证断言**(见 inspection-integration.md §三、state-machine.md):
```
质检单(非关键项不合格) →
  让步流程: 建议 → 审批 → CONDITIONAL
  记录让步原因/审批人
CONDITIONAL: 业务单据继续流转, 但标记让步
关键项不合格 → 不可让步(直接 REJECTED)
```

**涉及机制**:inspection-integration.md §三、state-machine.md

---

## UC-QA-04 完工检验不合格→返工

**场景**:制造完工检验不合格,触发返工工单。

**可验证断言**(见 inspection-integration.md §二):
```
完工质检(FINAL) REJECTED →
  不合格处理路径: 返工(见 §二 路径表)
  触发 ../manufacturing 新建返工工单(关联原工单)
```

**涉及机制**:inspection-integration.md §二、../manufacturing

---

## UC-QA-05 NCR-CAPA 闭环

**场景**:不合格品报告(NCR)的纠正预防措施(CAPA)闭环,需效果验证才能关闭。

**可验证断言**(见 state-machine.md §NCR、inspection-integration.md §四):
```
NCR: OPEN → IN_REVIEW → 制定 CAPA(纠正+预防)
  → 执行 CAPA
  → 效果验证:
      验证通过 → RESOLVED
      验证失败 → 返回 IN_REVIEW(重新制定)
未通过效果验证 → NCR 不可 RESOLVED
全程记录纠正/预防措施/验证结果
```

**涉及机制**:state-machine.md §NCR、inspection-integration.md §四

---

## UC-QA-06 关键项否决

**场景**:质检中关键检验项不合格,整体判为不合格。

**可验证断言**(见 inspection-integration.md §五):
```
质检模板行.是否关键项 == true 且 该行不合格 →
  整体质检单 = REJECTED(关键项否决,无论其他项)
非关键项不合格 → 可让步(CONDITIONAL)
```

**涉及机制**:inspection-integration.md §五

---

## UC-QA-07 质检模板优先级解析

**场景**:按优先级解析适用的质检模板。

**可验证断言**(见 inspection-integration.md §五):
```
优先级: 物料级模板 > 物料类别级 > 全局默认
物料有专属模板 → 用专属
否则 查类别模板 → 用类别
否则 用全局默认
模板缺失且强制质检 → 报错或用最小默认
```

**涉及机制**:inspection-integration.md §五

---

## UC-QA-08 业务单据作废联动取消质检

**场景**:业务单据(如入库单)作废,联动取消关联的未完成质检单。

**可验证断言**(见 state-machine.md §4):
```
入库单.作废 →
  关联质检单(若 PENDING) → 取消(CANCELLED)
  不影响已 ACCEPTED/REJECTED 的质检单(历史完整)
```

**涉及机制**:state-machine.md §4

---

## UC-QA-09 SPC 失控预警

**场景**:SPC 控制图子组违反判异规则,事件驱动创建 NCR + CAPA。

**可验证断言**(见 spc.md §关键流程):
```
SPC 采样(nop-job) → 聚合 InspectionLine.measuredValue → ErpQaSpcSample
控制图计算(≥20 子组) → 检查 violatedRules
子组.isOutOfControl == true →
  事件驱动创建 NCR(sourceType=SPC)
  按 ruleSet 创建 CAPA(Action)
  severity 按 violatedRules 映射
```

**涉及机制**:spc.md §关键流程

---

## UC-QA-10 SPC 过程能力分析

**场景**:周期性计算 Cpk,能力不足触发风险登记与质量目标回写。

**可验证断言**(见 spc.md §ErpQaSpcCapability):
```
周期任务 → 计算 ErpQaSpcCapability:
  Cp = (USL - LSL) / (6 × σ̂)        // σ̂ = R̄/d2
  Cpk = min((USL - X̄̄), (X̄̄ - LSL)) / (3 × σ̂)
capabilityLevel:
  Cpk < 1.0 → INADEQUATE
  1.0-1.33 → ACCEPTABLE
  1.33-1.67 → CAPABLE
  > 1.67 → EXCELLENT
若 < ACCEPTABLE → 回写 QualityGoal.currentValue + 创建 RiskRegister
```

**涉及机制**:spc.md §ErpQaSpcCapability

---

## UC-QA-11 SPC 数据从 InspectionLine 聚合

**场景**:SPC 不重复存储原始读数,从质检明细聚合。

**可验证断言**(见 spc.md §关键决策):
```
SPC 采样任务 → 从 ErpQaInspectionLine.measuredValue 聚合(不重复存)
按 chart.subgroupSize + samplingFrequency 聚合成 SpcSample
原始读数仍在 InspectionLine(单一真相源)
SpcSample.measuredValues 是聚合后的子组统计
```

**涉及机制**:spc.md §关键决策

---



---

## UC-QA-12 质量看板

**场景**:质量看板的指标展示与异常预警。见 ../dashboards.md §质量看板。

**可验证断言**:
```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  质检数/合格率/不合格数/开放NCR, 合格率趋势, 不合格原因TOP, SPC失控/CAPA逾期预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**涉及机制**:../dashboards.md、各域 state-machine.md、roles-and-permissions.md(行级权限)

## 用例与测试的衔接

- 质检阻塞(U01)→ 强制质检 + 业务流转阻塞
- 让步/否决(U03/U06)→ 关键项判定逻辑
- NCR-CAPA(U05)→ 效果验证门禁(失败返回)
- SPC(U09/U10/U11)→ 从 InspectionLine 聚合 + 失控预警 + 能力分析

## 参考机制文档

- `state-machine.md` — 质检单/NCR 状态/异常
- `inspection-integration.md` — 触发机制/结果反馈/让步/NCR-CAPA/模板/事件契约
- `spc.md` — 控制图/样本/能力分析/失控预警
- `../purchase/returns.md` — 来料不合格退货
- `../manufacturing/state-machine.md` — 完工不合格返工
