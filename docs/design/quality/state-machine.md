# 质量管理域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 质量域有两类状态对象：**质检单**（Inspection）与**不符合项报告**（NonConformance）。

## 适用对象一：质检单（Inspection）

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 业务单据影响 |
|------|----------------------|--------------|
| 待检（PENDING） | 已生成质检单，等待检验 | 关联业务单据暂停流转（强制质检时） |
| 合格（ACCEPTED） | 终态：检验合格 | 关联业务单据继续流转 |
| 让步接收（CONDITIONAL） | 终态：不合格但经审批降级接收 | 关联业务单据继续（附让步记录） |
| 不合格（REJECTED） | 终态：检验不合格 | 触发退货/返工/报废 |

### 2. 迁移完整性

```
待检 (PENDING)
  ├─ 录入结果 → 合格 (ACCEPTED)
  ├─ 录入结果 + 让步审批 → 让步接收 (CONDITIONAL)
  └─ 录入结果 → 不合格 (REJECTED) → 触发 NCR + 退货/返工
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| PENDING→ACCEPTED | 质检员 | 所有检验项实测值在规格内 | 关联业务单据继续流转 |
| PENDING→CONDITIONAL | 质检员 + 审批人 | 部分项不合格但经审批降级使用 | 继续流转，记录让步理由 |
| PENDING→REJECTED | 质检员 | 关键项不合格或整体不达标 | 触发 NCR，反馈业务域 |

> **silent flip 守卫**：所有 PENDING→终态迁移（`recordResult` / `passInspection` / `failInspection`）均守卫 `result==PENDING` 单一源态 + 设 `posted=true`；`failInspection` 同 `recordResult` REJECTED 分支触发 NCR 自动生成。终态（ACCEPTED/CONDITIONAL/REJECTED）直接调用上述方法抛 `ERR_INVALID_INSPECTION_STATUS_TRANSITION`，禁止 silent flip 绕过强制质检门控。

### 3. 终态与恢复

- 终态：`合格（ACCEPTED）`、`让步接收（CONDITIONAL）`、`不合格（REJECTED）`。
- 终态不可直接恢复；若需复检，**新建质检单**（经 `IErpQaInspectionBiz.createForBusinessBill` 关联原单与业务单据），原质检单 result 保持不变作为审计记录。`reInspect` 方法已废弃删除——禁止将终态直接翻回 PENDING。
- 不合格（REJECTED）触发 NCR 流程，NCR 闭环是独立状态机。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 强制质检的业务单据未经质检就流转 | 系统拦截，拒绝流转 |
| 质检模板缺失（物料未配置检验标准） | 按全局默认模板或提示先配置 |
| 复检结果与原检冲突 | 以复检结果为准，原检记录保留（审计） |
| 让步接收未经审批 | 拒绝迁移到 CONDITIONAL |
| 并发录入同一质检单 | 乐观锁 |
| 业务单据作废联动 | 关联业务单据作废时，未完成的质检单自动取消（**已实现**——`IErpQaInspectionBiz.cancelForBusinessBill` Facade + purchase/sales/mfg cancel Processor config-gated wiring，RC-R1.59；见 §实现约定） |

### 5. 可达性

- 从 PENDING 可达三个终态（ACCEPTED/CONDITIONAL/REJECTED）。
- 无不可达状态，无死锁。终态无出边。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| 录入结果（→ACCEPTED/REJECTED） | 质检员 |
| 让步审批（→CONDITIONAL） | 质检员 + 质量主管审批 |

危险操作：
- **让步接收**：需质量主管审批，因降级使用有质量风险。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 业务单据（采购入库/销售出库/工单）触发质检 | 业务域发布事件，本域订阅生成质检单 |
| 质检结果反馈业务域 | 本域发布结果事件，业务域订阅（合格则继续，不合格则退货/返工） |

外部触发渠道：
- 业务单据审核联动（主要渠道）。
- 质检员手工创建（抽检场景）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| PENDING | 是 | assigned（质检员）—— 待检任务 |
| ACCEPTED/CONDITIONAL | 否 | — |
| REJECTED | 是 | assigned（质量主管）—— 不合格处理决策（退货/返工/报废） |

避免"待检质检单长期滞留"：PENDING 产生 TODO，强制质检的业务单据在质检完成前阻塞流转。

### 9. 场景演练

#### 场景 A：来料检验合格

1. 采购入库审核 → 触发来料质检单（PENDING）。
2. 质检员录入实测值，全部合格 → ACCEPTED。
3. 采购入库单继续流转（写库存、生成凭证）。

#### 场景 B：来料检验不合格退货

1. 采购入库审核 → 触发来料质检单（PENDING）。
2. 检验不合格 → REJECTED → 开 NCR。
3. 反馈采购域 → 触发采购退货。

#### 场景 C：让步接收

1. 来料部分项不合格（非关键项）。
2. 质检员提让步建议 → 质量主管审批 → CONDITIONAL。
3. 记录让步理由（降级使用），入库继续。

#### 场景 D：完工检验返工

1. 工单完工 → 触发完工质检（PENDING）。
2. 不合格 → REJECTED → 反馈制造域。
3. 制造域新建返工工单（关联原工单）。

### 10. 与设计文档一致性

- 质检与业务单据关联见 `quality/README.md`。
- 状态码归 `model/app-erp-quality.orm.xml`。

---

## 适用对象二：不符合项报告（NonConformance）

NCR 状态机 5 态（含召回升级）：

```
待处理 (OPEN)
  ├─ 评审 → 评审中 (IN_REVIEW)
  │           ├─ 制定 CAPA 并验证有效 → 已解决 (RESOLVED)
  │           ├─ 升级为召回事件 → 已升级为召回 (ESCALATED_TO_RECALL)
  │           │       └─ 召回事件独立处理（ErpQaRecall），见 recall.md
  │           └─ 取消（误开或无效）→ 已取消 (CANCELLED)
  └─ 取消 → 已取消 (CANCELLED)
```

| 状态 | 业务含义 |
|------|----------|
| 待处理（OPEN） | NCR 已记录，等待评审 |
| 评审中（IN_REVIEW） | 正在分析原因、制定纠正预防措施 |
| 已解决（RESOLVED） | 终态：CAPA 已执行且效果验证通过 |
| 已升级为召回（ESCALATED_TO_RECALL） | 终态：该 NCR 已升级为召回事件（ErpQaRecall），后续处理走 recall.md。不走 RESOLVED 路线 |
| 已取消（CANCELLED） | 终态：误开或无效 NCR |

### NCR 与 CAPA 的关系

- NCR 是不合格事件的记录。
- 针对 NCR 制定 CAPA（纠正预防措施）：纠正（即时修复）+ 预防（防止再发）。
- 有 CAPA 措施时：CAPA 需全部完成（COMPLETED）+ 效果验证（验证人/验证日期）才能关闭 NCR（闭环）。
- 无 CAPA 措施时 resolve：须显式提供 `noCapaReason`（误开/降级场景显式标注），否则抛 `ERR_NCR_RESOLVE_NO_CAPA`。

NCR 的其他维度（异常/角色/TODO）与质检单类似，不重复展开；审查时同样使用提示词。

### NCR 财务影响规则

NCR 关闭（RESOLVED）时，根据处置方式触发不同的财务处理：

| 处置方式 | 财务处理 | 凭证方向 |
|----------|----------|----------|
| 退货 | 生成红字入库凭证（冲销原入库暂估） | 借：暂估应付 / 贷：存货 |
| 返工 | 返工成本归集到原工单（直接人工+材料） | 借：制造费用 / 贷：原材料+应付职工薪酬 |
| 报废 | 报废损失凭证 | 借：营业外支出（或制造费用） / 贷：存货 |
| 让步接收 | 无额外凭证（降级入库按原价） | — |
| **升级为召回**（ESCALATED_TO_RECALL） | NCR 升级为召回事件（ErpQaRecall，见 recall.md），触发批量销售退货，过账走 sales 域标准退货流程 | 走标准销售退货过账 |

> NCR 过账模式可按 NCR 类型配置：AUTO_POST（NCR 关闭时自动触发）或 MANUAL_POST（需人工确认后触发）。配置项：`erp-qua.ncr-posting-mode`。

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 强制质检的业务单据在质检完成前是否阻塞流转。
- 让步接收的审批权限是否落实。
- 不合格 → NCR → CAPA 的闭环是否完整（效果验证才能关闭）。
- 质检结果反馈业务域的机制（事件驱动）。
- 业务单据作废时关联质检单的联动取消。

---

## 实现约定

本期范围与上述设计的偏离（均为 Non-Goal，留后继）：

- **质检结果反馈业务域**：设计 §7「事件驱动」本期改为**业务域查 quality 结果**（`IErpQaInspectionBiz.findByRelatedBill` / `isInspectionCleared`），quality 不反向依赖 business（DAG 无环）。业务域 Processor 在 confirm/DONE 前 config-gated 查询。残留风险：业务域须主动查。事件驱动留后继。
- **强制质检阻塞机制**：设计 §4「强制质检的业务单据未经质检就流转 → 系统拦截」经 `erp-qua.mandatory-inspection-bill-types` config-gated（默认空=不强制）落地；purchase/sales/mfg BizModel 经 `InspectionTrigger.enforceGate`（business→quality 同步 I*Biz 写触发）——首次流转生成 PENDING 质检单并阻塞，质检合格/让步后再次流转放行。
- **NCR 财务过账（Non-Goal）**：设计 §「NCR 财务影响规则」（退货红字/返工成本/报废损失/召回退货凭证）依赖 finance 域 NCR 驱动过账 Provider + purchase/sales 退货流程，属业财一体面，本期不落地。触发条件：NCR 驱动自动退货/报废过账 Provider 落地时（successor）。
  - **NCR 过账引擎**：SCRAP 处置 → 报废损失凭证（借 6711 营业外支出/贷 1401 库存商品，经 `NcrScrapAcctDocProvider` + `NcrPostingDispatcher`）；RETURN 处置 → 编排退货域（`IErpPurReturnBiz`/`IErpSalReturnBiz`，退货单自带红字过账，NCR 侧登记 `returnCode`）；CONCESSION/DOWNGRADE → 无凭证（`postNcr` 拒 `ERR_NCR_DISPOSITION_NOT_POSTABLE`）。`resolve` 按 `erp-qua.ncr-posting-mode`（AUTO_POST/MANUAL_POST）config-gated 分派，`postNcr`/`reverseNcr` 提供人工入口。posted 三件套（`posted`/`postedAt`/`postedBy`）+ `returnCode` 列加性新增到 `ErpQaNonConformance`。
  - **实现偏离**：(1) 返工映射 Decision(b)——不加 REWORK 字典码值/NCR_REWORK 业务类型，返工经制造域返工工单自然归集成本，NCR 侧仅状态迁移不过账；(2) 退货编排 vs 直接过账 Decision——选编排退货域（单一过账来源原则），不建 `NcrReturnAcctDocProvider`；(3) posted 机制 Decision(a)——显式 posted-flag 三件套（与 `ErpQaInspection` 一致），非 voucher 反查；(4) 报废存货物理出库简化——NCR_SCRAP 凭证贷记存货已表达会计影响，物理库存量同步扣减属 inventory 域 successor（避免与 InvPostingDispatcher SALES_OUTPUT 双计）。
- **召回事件（Non-Goal）**：NCR `ESCALATED_TO_RECALL` 为终态指向 `recall.md`；召回 `ErpQaRecall` 属工作项 2.11，本期仅状态迁移不触发召回流程。触发条件：工作项 2.11 就绪时。
- **让步接收审批流（简化）**：设计 §2 让步 CONDITIONAL 需「让步审批」；本期以 `approveStatus=APPROVED`（质量主管审核）简化，完整多级让步审批工作流 Non-Goal。触发条件：多级审批需求时。
- **抽检方案自动计算（Non-Goal）**：`ErpQaSamplingPlan` 实体存在但抽样数量自动计算（AQL/GB2828）不落地。触发条件：统计抽样需求时。
- **校准管理 / 风险登记 / 质量目标 / 评审（Non-Goal）**：`ErpQaCalibration`/`ErpQaRiskRegister`/`ErpQaQualityGoal`/`ErpQaReview` 实体存在但 BizModel 深化不落地（仅标准 CRUD 空壳）。触发条件：计量管理 / QMS 全面需求时。
- **业务单据作废联动取消（已实现）**：设计 §4「业务单据作废时关联质检单自动取消」**已实现**（RC-R1.59 / P1-RC-041，plan `2026-08-16-1634-3-rc-mr1-r1-59-qa-business-cancel-linkage.md`）：`IErpQaInspectionBiz.cancelForBusinessBill(billType, billCode)` Facade——按 relatedBillType+relatedBillCode 精确查询关联质检单，仅 `result=PENDING` 软删取消（useLogicalDelete 置 delVersion，平台逻辑删除，非物理删除），终态（ACCEPTED/CONDITIONAL/REJECTED）不动（历史完整，L1 use-cases.md:141），无匹配零副作用，幂等（重复取消零副作用）。purchase/sales/mfg cancel Processor 作废成功后置调用（`ErpPurReceiveCancelProcessor.cancel` / `ErpSalDeliveryCancelProcessor.cancel` / `ErpMfgWorkOrderProcessor.cancel`，protected step `cancelLinkedInspections`，try/catch LOG.warn 降级不阻断作废主流程——联动为辅助语义）。**D1 取消载体**：方案 A 软删（对齐 arm-index P1-MA2-064 方案 A「PENDING→cancelled via useLogicalDelete」）；方案 B（result dict 加 CANCELLED + 状态机迁移）不选——dict 追加超 B 类枚举（2026-08-08 不可类推规则），且 L1「取消(CANCELLED)」语义经软删已满足（软删后 findByRelatedBill 经平台 delVersion=0 过滤自动不可见，门控/反查语义天然闭合；审计经 delVersion/删除时间可追溯）。**D2 config 门控**：`erp-qua.business-cancel-linkage-enabled` **默认 true**（仅取消 PENDING 且关联已作废单据的质检单，零活跃数据危害；部署侧可显式关闭保持作废零联动）。**D3 接线与 billType 键值**：各域 cancel 接线使用本域创建路径同源常量（pur=`ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE`="ERP_PUR_RECEIVE"、sal=`ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY`、mfg=`ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER`），保证与强制质检触发写入的 relatedBillType 键值一致；残留风险：软删质检单无独立「已取消」状态展示（登记 Deferred But Adjudicated「软删质检单的『已取消』审计展示」）。
- **行级评测规格类型**：`specMin/specMax/measuredValue` 列域为 DECIMAL（domain measuredValue/specLimit），非数值规格（外观「合格/不合格」）由 `InspectionResultEvaluator`「无规格上下限 + 实测非空即合格」分支处理；纯非数值实测值落库受域强转限制（须人工录入行结果覆盖）。
- **passInspection/failInspection 状态守卫 + reInspect 废弃**：`passInspection`/`failInspection` 增 `result==PENDING` 单一源态守卫 + 设 `posted=true`（postedAt/postedBy）+ `failInspection` 触发 `autoCreateNcrFromInspection`（与 `recordResult` REJECTED 分支对齐），堵住 silent flip REJECTED→ACCEPTED 绕过强制质检门控。`reInspect` 方法 + `IErpQaInspectionBiz.reInspect` 接口签名删除——终态不可直接翻回 PENDING，复检走 `createForBusinessBill` 新建关联质检单（§3）。

## CRUD 桩实体状态机（Deferred）

> 以下实体的 dict 状态值为**预留语义入口（零 writer）**，CRUD 桩为主路径可用；完整状态机迁移属 QMS 全面需求 successor。dict 值保留不删除（「保留 dict 死状态为预留」先例一致）。

| 实体 / dict | 死状态值（零 writer） | 现状 | Successor 触发条件 |
|-------------|----------------------|------|-------------------|
| `ErpQaQualityGoal` / QualityGoal 状态 | 全部（BizModel 为 CRUD 桩，零 `setStatus` writer） | 标准 CRUD 可用，状态字段不参与主路径迁移判定 | 质量目标管理（KPI 设定/考核闭环）需求时 |
| `ErpQaReview` / Review 状态 | 全部（BizModel 为 CRUD 桩，零 `setStatus` writer） | 标准 CRUD 可用 | 管理评审流程需求时 |
| `ErpQaCalibration` / Calibration 状态 | 全部（BizModel 为 CRUD 桩，零 `setStatus` writer） | 标准 CRUD 可用 | 计量器具校准管理需求时 |
| `erp-qa/risk-status` | `MITIGATED`/`CLOSED` 死（仅 `SpcCapabilityCalculator` 写 `OPEN`） | `ErpQaRiskRegister` 状态字段预留 | 风险登记闭环管理需求时 |
| `erp-qa/action-status` | `OVERDUE`（零 writer） | CAPA Action 状态机主路径 COMPLETED/PENDING 等可用 | 逾期自动监控需求时 |
| `erp-qa/spc-calc-status` | `STALE`（零 writer） | SPC 计算状态主路径可用 | SPC 控制图失效自动重算需求时 |

**Successor 触发条件**：计量管理 / QMS 全面需求时，实现上述各实体 `BizMutation` 状态机迁移（含 dict 死状态 writer 落地）。CRUD 空壳实体状态字段不参与质检单/NCR/召回三大主状态机迁移判定，故本期保留为预留值不影响主路径。

