# rc-ma1-a1-9 mfg-F2 工单与报工 需求-实现符合性五级追踪审计报告

> 报告类型：requirement-compliance MA1 切片 A1.9
> 切片：mfg-F2 工单与报工（roadmap 标签；权威 UC 范围 = UC-MFG-01/03/04/06/07/09 共 6 UC）
> 审计时间：2026-08-02
> 审计基线 HEAD：`3c4beba78`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）
> 上游计划：`docs/plans/2026-08-02-2042-3-rc-ma1-a1-9-mfg-f2-work-order-reporting.md`（草案审查 `acceptable as-is`，独立子代理 `ses_03d4ef6d9ffePMQggYCtjxqEz`）
> 真相源层级（§4 Q1）：L1 = `docs/design/manufacturing/use-cases.md`（UC-MFG-01 `:18` / UC-MFG-03 `:59` / UC-MFG-04 `:75` / UC-MFG-06 `:107` / UC-MFG-07 `:125` / UC-MFG-09 `:160`）；L2 = `state-machine.md` + `bom-and-routing.md` + `material-reservation.md`（设计参考，冲突一律以 L1 为准）；L3 = 实仓代码；L4 = 测试；L5 = 复用 A2.6a/A4.2a + 本切片差异。

---

## 9. 与既有 MA2/MA4 报告差异增量声明（前置声明，便于读者识别复用边界）

> 依方法论 §6 段落 9 + §去重协议，本报告前置声明与既有 MA2/MA4 报告的差异增量。

| 既有报告 | 覆盖维度 | 已证实结论（本切片复用） | 本切片补的差异增量（需求契约视角） |
|---------|---------|----------------------|--------------------------|
| `2026-07-28-0109-arm-ma2-mfg-work-order-jobcard-state-machine.md`（A2.6a） | 工单/作业卡/领料/委外状态机（10+8+4+8 态）业务正确性 + 事务回滚一致性 + 红冲闭环对称性 | 工单 10 态全守卫（`requireStatus`/`validateTransition*`）+ 领料 `reverseConfirm` 红冲闭环对称 + 委外 `reverseCompletion` 红冲闭环对称 + @BizMutation 事务回滚覆盖跨域写一致性 + 部分齐套强制开工 config-gated 出口（异常回滚状态保持）；**1 P1（P1-MA2-035）+ 3 P2（P2-MA2-042/043/044）** | 工单状态机/事务/红冲行为**复用 A2.6a pass 结论**（不重审）；本切片只补**需求契约↔实现符合性**视角（UC-MFG-01 全链 + UC-MFG-03/04 齐套 + UC-MFG-06 领料扣减预留 vs Deferred + UC-MFG-07 完工成本结转凭证完整性 + UC-MFG-09 返工路径） |
| `2026-07-29-0024-arm-ma4-mfg-work-order-bom-code-quality.md`（A4.2a） | 工单/BOM 链路**代码质量**（编排健壮性/BOM 展开/算术/错误处理/失败恢复闭环/架构边界/测试有效性） | BomExpander DFS 环检测 + path 回溯正确；报工 laborCost = durationMins/60×hourlyRate 算术正确；领料 materialCost 聚合正确；完工 unitCost = total/completed 除零守卫；错误处理规范化（全 NopException + erp.err.mfg.*）；**3 P1（P1-MA4-007/008/009）+ 1 P2（P2-MA4-004）** | 本切片不重审代码质量维度；只补**需求契约 vs 实现符合性**（UC-MFG-07 凭证完整性 via P1-MA4-007 HEAD 复核 + UC-MFG-06 预留扣减预留符合性 + resolved finding HEAD 复核） |
| `2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（A3.4） | mfg owner doc vs code **drift**（文本一致性） | P1-MA3-040 state-machine.md §质检约束 INSPECTING 不存在（resolved R2.6）/ P1-MA3-041 可配置超产无 config（resolved R2.6）/ P1-MA3-044 README DowntimeEntry/ProductionPlan 不存在（resolved R2.6）/ P1-MA3-042 material-reservation.md 整个子系统未实现（resolved R2.6 方案 B 标 Deferred） | 本切片不复审 doc↔code 文本一致性；**R2.6 方案 B 关闭（owner doc Deferred）属 audit-remediation 文本一致性维度，不关闭需求契约维度**（§去重协议）。UC-MFG-06 领料扣减预留的需求契约裁决交叉引用 A1.8 已建的 P1-RC-008（不重复登记） |
| `2026-07-29-1430-arm-ma5-mfg-test-coverage.md`（A5.2） | mfg 测试覆盖深度 | mfg 深测比例 41% 全域最高；物料预留子系统零测试（P1-MA5-006 successor 注记——被测功能不存在） | 本切片不复审测试维度；L4 测试断言强度引用 A5.2 + A4.2a 已评级，不重复登记 |

**结论**：本切片裁决焦点 = **UC-MFG-01/03/04/06/07/09 需求契约↔实现符合性**。工单主链状态机/事务/红冲/代码质量四面**复用 A2.6a/A4.2a pass 结论**（接受，不重审）；本切片只补需求视角差异（UC-MFG-06 领料扣减预留符合性交叉 P1-RC-008 + UC-MFG-07 完工成本结转凭证完整性 P1-MA4-007 HEAD 复核 + UC-MFG-09 返工路径 + resolved finding HEAD 复核）。

---

## 1. 需求契约原文（L1 逐字引用，禁止转述）

> 真相源：`docs/design/manufacturing/use-cases.md`（UC 锚点经 `docs/audits/rc-requirement-baseline-inventory.md` A1.9 确认 = `:18/:59/:75/:107/:125/:160`，inventory `:343` 一致）。

### UC-MFG-01 工单正常生产全流程（`use-cases.md:18-39`）

逐字引用验收标准：

```
工单.状态 流转: DRAFT→SUBMITTED→(STOCK_RESERVED|STOCK_PARTIAL)→IN_PROCESS→COMPLETED   [断言①]
齐套: BOM 展开子件需求 × 产出量, 与 可用量比较                                            [断言②]
完工入库: 库存余额[产成品] += 完工数量                                                    [断言③]
成本结转: 材料成本 + 人工成本 + 制造费用 → 产成品存货估值凭证(见 bom-and-routing §成本计算) [断言④]
```

涉及机制：`state-machine.md §1/§2`、`bom-and-routing.md`、`material-reservation.md`。

### UC-MFG-03 齐套校验（`use-cases.md:59-71`）

逐字引用验收标准：

```
齐套校验 = BOM 展开子件需求 vs 可用量(物料×仓库)           [断言⑤]
全部满足 → 工单.状态 = STOCK_RESERVED                       [断言⑥]
部分满足 → 工单.状态 = STOCK_PARTIAL                        [断言⑦]
齐套状态决定可否生产(配置 ErpMfgBom.consumption)            [断言⑧]
```

涉及机制：`material-reservation.md §齐套`、`state-machine.md`。

### UC-MFG-04 部分齐套强制开工（`use-cases.md:75-86`）

逐字引用验收标准：

```
ErpMfgBom.consumption != STRICT 或 主管权限                 [断言⑨]
STOCK_PARTIAL → 可迁移到 IN_PROCESS(强制开工)                [断言⑩]
缺件部分后续补料                                              [断言⑪]
```

涉及机制：`state-machine.md §2/§6`。

### UC-MFG-06 领料扣减预留（`use-cases.md:107-121`）

逐字引用验收标准：

```
领料单.数量 <= 预留剩余量(超预留拒绝或警告 erp-mfg.over-pick-warning)  [断言⑫]
领料后:
  MaterialReservation.pickedQty += 领料量                              [断言⑬]
  MaterialReservation.reservedQty -= 领料量(预留转消耗)                  [断言⑭]
  库存余额.现有量 -= 领料量                                              [断言⑮]
  库存余额.预留量 -= 领料量                                              [断言⑯]
```

涉及机制：`material-reservation.md §领料`。

### UC-MFG-07 工单完工入库与成本结转（`use-cases.md:125-140`）

逐字引用验收标准：

```
完工入库 →
  库存余额[产成品] += 完工数量                                            [断言⑰]
  产成品单位成本 = (材料 + 人工 + 制造费用) / 完工数量                     [断言⑱]
  生成存货估值凭证: 借 产成品存货, 贷 在制品(WIP)/各成本要素                [断言⑲]
材料成本 = Σ 领料单成本                                                    [断言⑳]
人工成本 = Σ JobCard.工时 × 费率                                           [断言㉑]
制造费用 = Σ 工序.工时 × 费率(见 bom-and-routing §BomOperation)            [断言㉒]
```

涉及机制：`bom-and-routing.md §成本计算`、`../finance/costing-methods.md`。

### UC-MFG-09 完工质检不合格→返工工单（`use-cases.md:160-172`）

逐字引用验收标准：

```
完工触发质检(若 BOM.inspection_required) →                                [断言㉓]
  质检 REJECTED → 不合格                                                   [断言㉔]
  原工单不可恢复(终态), 新建返工工单(关联原工单)                            [断言㉕]
返工工单走标准流程, 产出合格品                                              [断言㉖]
```

涉及机制：`state-machine.md §4`、`../quality`。

**断言计数**：UC-MFG-01 ×4（①②③④）+ UC-MFG-03 ×4（⑤⑥⑦⑧）+ UC-MFG-04 ×3（⑨⑩⑪）+ UC-MFG-06 ×5（⑫⑬⑭⑮⑯）+ UC-MFG-07 ×6（⑰⑱⑲⑳㉑㉒）+ UC-MFG-09 ×4（㉓㉔㉕㉖）= **26 条验收标准**（草案审查 iter1 实测一致，覆盖 6 UC 无跳号无合并）。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### 2.1 工单状态机主链（UC-MFG-01/03/04/07/09 共用编排）

> R6.2 per-mutation 拆分后（plan `2026-07-31-2115-2`），start/stop/resume/close/reportCompletion 已迁入独立 `<Entity><Method>Processor`；共享 protected helper 单一真相源在 `ErpMfgWorkOrderProcessor`。本节按逻辑（方法名 + 行为语义）核验，行号偏移由 R6.2 解释。

| 组件 | 文件:行 | 作用 |
|------|---------|------|
| 工单 Facade | `module-manufacturing/erp-mfg-service/.../entity/ErpMfgWorkOrderBizModel.java:64-124`（@BizMutation 委托 5 per-mutation Processor + `checkAvailability:65`/`cancel:95` 保留 facade） | Facade（R6.2 thin Facade） |
| 工单编排基线 | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderProcessor.java:111-138`（checkAvailability + cancel + 5 审批 validateTransition\*/do\*）+ `:140-167`（差异失败告警 helper）+ `:256-267`（validateTransitionForStart 部分齐套守卫）+ `:271-277`（doStart）+ `:279-316`（generateCompletionMove 完工入库移动单）+ `:329-338`（isInspectionGated）+ `:340-347`（recomputeTotals）+ `:360-365`（requireStatus） | 共享 protected step 单一真相源 |
| 完工 per-mutation | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderReportCompletionProcessor.java:31-104`（reportCompletion 全链：requireWorkOrder→requireStatus IN_PROCESS→超量守卫→质检门控→generateCompletionMove→writeBatchGenealogy→recompute→COMPLETED→差异自动触发） | R6.2 拆出 |
| 开工 per-mutation | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgWorkOrderStartProcessor.java` | R6.2 拆出（doStart 委托 facade） |
| 审批 5 Processor | `ErpMfgWorkOrder{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java` | R5.5 per-mutation；SoD 守卫 `doApprove` 委托 `SoDGuard.assertApproverNotCreator:234`（plan 2026-07-31-1023-2 R3.3） |
| 齐套校验 | `module-manufacturing/erp-mfg-service/.../workorder/KitAvailabilityChecker.java:62-89`（check）+ `:102-114`（loadAvailableByMaterial）+ `:38-37`（Javadoc 显式「只读校验，不写预留」） | 决定 STOCK_RESERVED/STOCK_PARTIAL |
| 齐套结果 | `module-manufacturing/erp-mfg-service/.../workorder/KitAvailabilityResult.java`（reserved()/partial() + shortages） | docStatus 写入载体 |

### 2.2 领料扣减 + 完工成本结转 + 差异过账链（UC-MFG-06/07）

| 组件 | 文件:行 | 作用 |
|------|---------|------|
| 领料 Facade | `module-manufacturing/erp-mfg-service/.../entity/ErpMfgMaterialIssueBizModel.java:37-44`（confirm + reverseConfirm 委托） | R6.2 thin Facade（46 行） |
| 领料确认 Processor | `module-manufacturing/erp-mfg-service/.../processor/ErpMfgMaterialIssueConfirmProcessor.java:33-73`（DRAFT→CONFIRMED→stockMoveBiz.generateMove→DONE + WorkOrderLine.actualQuantity 回写 + aggregateIssueMaterialCost + applyMaterialCostToWorkOrder + dispatchIfApplicable） | R6.2 拆出（共享 helper 在 AbstractErpMfgMaterialIssueProcessor） |
| 领料移动单构造器 | `module-manufacturing/erp-mfg-service/.../entity/MaterialIssueStockMoveBuilder.java:29-47`（OUTGOING_ISSUE moveType + 幂等键 `(ERP_MFG_ISSUE, issue.code)`） | 经 `IErpMdAcctSchemaBiz` I\*Biz 解析账套 |
| 领料过账 Dispatcher | `module-manufacturing/erp-mfg-service/.../posting/ManufacturingIssuePostingDispatcher.java`（dispatchIfApplicable try/catch + reverse） | MANUFACTURING_ISSUE 凭证（Dr WIP 1411 / Cr 原材料 1401） |
| 完工入库移动单构造 | `ErpMfgWorkOrderProcessor.java:279-316`（generateCompletionMove，MANUFACTURING moveType + line.unitCost = wo.unitCost） | 经 `IErpInvStockMoveBiz.generateMove` Facade |
| 完工成本三要素回写 | `ErpMfgJobCardProcessor.recordWork` laborCost = durationMins/60×hourlyRate + `applyLaborCostToWorkOrder`（A4.2a 已评级）+ `applyMaterialCostToWorkOrder:113-124`（材料）+ overheadCost 经 config-gated 分配率（state-machine.md:176） | recomputeTotals 重算 total/unit |
| 完工差异过账 | `module-manufacturing/erp-mfg-service/.../posting/ProductionVarianceDispatcher.java:70-118`（dispatchIfApplicable + try/catch LOG.warn）+ `:133-154`（reverseIfExists 红冲幂等） | PRODUCTION_VARIANCE 凭证 |
| 完工差异失败告警 | `ErpMfgWorkOrderProcessor.java:140-167`（isNoStandardCostError + dispatchVarianceFailureAlert → `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure", ctx)`） | G3 错误分级（plan R1.16） |

### 2.3 质检门控（UC-MFG-09）

| 组件 | 文件:行 | 作用 |
|------|---------|------|
| 质检门控入口 | `ErpMfgWorkOrderReportCompletionProcessor.java:46-58`（willFinish && isInspectionGated → InspectionTrigger.enforceGate，gate=BLOCKED 抛 `ERR_INSPECTION_REQUIRED` 保持 IN_PROCESS） | config-gated |
| isInspectionGated 判定 | `ErpMfgWorkOrderProcessor.java:329-338`（CONFIG_INSPECTION_GATE_ENABLED 默认 false + BOM.inspectionRequired=true） | 双重门控 |
| 跨域质检 Facade | `IErpQaInspectionBiz`（quality 域）+ `InspectionTrigger.enforceGate`（quality 域提供） | 跨域 I\*Biz Facade |

### 2.4 L3 关键事实（实测核验，区分"已实现"vs"未实现"）

- **已实现**：①全链状态流转（DRAFT→SUBMITTED→NOT_STARTED→[STOCK_RESERVED|STOCK_PARTIAL]→IN_PROCESS→COMPLETED，状态机 10 态全守卫，A2.6a §2.1 已证实）；③完工入库（generateCompletionMove → IErpInvStockMoveBiz → 产成品余额 +=）；④成本结转凭证（完工入库移动单触发 MANUFACTURING_RECEIPT 凭证 Dr 1401/Cr 1411，由 InvAcctDocProvider 内部生成）；⑤⑥⑦齐套校验决定 STOCK_RESERVED/STOCK_PARTIAL（KitAvailabilityChecker）；⑧ErpMfgBom.consumption 字段已落地（决定齐套严格度）；⑨⑩⑪部分齐套强制开工 config `erp-mfg.allow-partial-kit-start` 默认 false + STOCK_PARTIAL→IN_PROCESS 守卫；⑭⑮⑯**库存侧**：领料出库移动单 DONE 扣减 onHand（MaterialIssueStockMoveBuilder OUTGOING_ISSUE + stock move bookkeeper）；⑰⑱⑲⑳㉑㉒完工单位成本 = total/completed + 三成本要素 + 存货估值凭证；㉓㉔质检门控 config-gated + BOM.inspectionRequired。
- **未实现（与 L1 字面偏离，已交叉引用 A1.8 P1-RC-008）**：⑫⑬⑭⑯的**预留追踪侧**——MaterialReservation 实体未物化 + pickedQty/reservedQty 无 writer + reservationStatus 字段不存在 + `erp-mfg.over-pick-warning` config key 未定义（与 A1.8 P1-RC-008 同根因，预留写路径 Deferred）；**库存侧现有量扣减（⑮）已实现**（经 stock move bookkeeper + P0-MA2-020 UK 独立防护）。
- **设计偏离（owner doc 显式记录，倾向接受）**：㉕原工单不可恢复（终态）+ 新建返工工单（关联原工单）——实现为**操作员驱动**（人工新建标准工单），无 originalWorkOrderId 字段、无自动建单代码路径；owner doc `state-machine.md §3 + §场景D + §质检约束声明` 与 L1 一致，实现以 config-gated 质检门控（工单保持 IN_PROCESS）+ 操作员手动新建返工工单的简化范式承载（owner doc 已裁定为 successor 触发条件，非阻断主路径）。

---

## 3. 测试证据（L4 测试断言 + 断言强度）

| 测试 | 文件:方法 | 覆盖断言 | 断言强度（A5.2/A4.2a 已评级） |
|------|----------|---------|------|
| 工单 E2E | `TestErpMfgWorkOrderEndToEnd.java#testEndToEndIssueReportCompletion:73-142` | ①②③④ ⑦⑧ ⑭⑮ ⑰⑱⑳㉑ | **强**（行级数值 materialCost=10 / laborCost=30 / totalCost=40 / unitCost=40 + 余额扣减 + 完工入库 moveType=MANUFACTURING + 产成品余额=1） |
| 工单状态机 | `TestErpMfgWorkOrderStateMachine.java`（happy path + 齐套 + 部分齐套 + 停工恢复关闭 + 取消 + 非法迁移 + 超报） | ①②⑤⑥⑦⑨⑩⑪ | **强**（docStatus 状态 + 非法迁移 assertThrows） |
| 完工质检门控 | `TestErpMfgWorkOrderEndToEnd.java#testInspectionGateBlocksCompletionWhenEnabled:144-185` | ㉓㉔ | **强**（config=true + BOM.inspectionRequired=true → ERR_INSPECTION_REQUIRED + 工单保持 IN_PROCESS；config=false → 正常完工） |
| 作业卡状态机 | `TestErpMfgWorkOrderEndToEnd.java#testJobCardStateMachine:187-207` | ㉑（人工成本算术） | **强**（OPEN→WORK_IN_PROGRESS→SUBMITTED→COMPLETED 状态 + ON_HOLD 暂停） |
| 领料主链 | `TestErpMfgMaterialIssue.java`（confirm + idempotent） | ⑭⑮（库存侧） | **强**（余额扣减 + WorkOrderLine.actualQuantity 回写 + materialCost 聚合 + 幂等） |
| 领料红冲 | `TestErpMfgMaterialIssueReversal.java` | 红冲闭环 | **强**（未过账守卫 + 反向移动单 + posted=false/CANCELLED） |
| 完工过账 | `TestErpMfgCompletionPosting.java` | ⑲ | **强**（MANUFACTURING_RECEIPT 凭证） |
| 生产差异 | `TestErpMfgProductionVariance.java`（6 类差异）+ `TestErpMfgVarianceRecomputeReversal.java`（重算红冲幂等） | UC-MFG-12（不在本切片） | **强**（A4.2a 已评级） |
| 差异告警 | `TestErpMfgVarianceAlert.java#testVarianceOverThresholdTriggersNotify`（+ 未超阈值 + config 关闭） | P1-MA4-007 修复测试 | **强**（IErpSysNotificationBiz.notify 被调 + ErpSysNotification 行落入 + config 关闭静默跳过） |
| 成本卷积 | `TestErpMfgCostRollup.java` | UC-MFG-12（不在本切片） | **强**（A4.2a 已评级） |
| BOM 展开 | `TestErpMfgBomExplosion.java` | UC-MFG-02（不在本切片） | **强**（DFS + 环检测 + phantom） |
| **预留追踪侧（⑫⑬⑭⑯）** | —（无） | — | **无测试**（功能 Deferred，与 P1-MA5-006 successor 注记一致） |
| **返工工单自动创建（㉕）** | —（无） | — | **无测试**（设计为操作员驱动，无自动建单代码） |
| **多币种完工入库 GL voucher 行级（P1-MA4-009）** | —（已 resolved R2.11） | — | R2.11 已补 |

**结论**：黄金路径 + 状态机 + 质检门控 + 领料（库存侧）+ 完工过账 + 差异告警测试**强覆盖**；预留追踪侧（⑫⑬⑭⑯）零测试（与 P1-MA5-006 successor 一致，功能 Deferred）。

---

## 4. 运行时行为证据（L5，复用 A2.6a/A4.2a + 本切片差异）

### 4.1 复用 A2.6a 已证实行为（状态机 + 事务 + 红冲闭环）

- **工单 10 态全可达 + 全守卫**（A2.6a §2.1 + §1.2）：DRAFT→SUBMITTED→NOT_STARTED→（STOCK_RESERVED | STOCK_PARTIAL）→IN_PROCESS→COMPLETED；IN_PROCESS→STOPPED→（IN_PROCESS 恢复 | CLOSED）；NOT_STARTED/SUBMITTED/DRAFT→CANCELLED。状态迁移经 `requireStatus`/`validateTransition*` 前置校验。
- **@BizMutation 事务回滚覆盖跨域写一致性**（A2.6a §1.4 + §3.4）：领料/完工/委外写库存全经 `IErpInvStockMoveBiz.generateMove` Facade；任一外部步骤失败（库存写/过账/质检）抛异常 → 事务回滚 → 状态保持。
- **领料 `reverseConfirm` 红冲闭环对称**（A2.6a §2.3）：DONE+posted=true → reverseConfirm → 红冲 MANUFACTURING_ISSUE 凭证 + 反向 OUTGOING 移动单 + posted=false/CANCELLED。
- **部分齐套强制开工缺料异常有出口**（A2.6a §3.4 + 关键裁决表）：STOCK_PARTIAL→IN_PROCESS 后 confirm 抛 NopException → @BizMutation 事务回滚 → 工单 IN_PROCESS 保持 / 领料 DRAFT 保持，无悬挂。
- **质检门控 INSPECTING 态字典偏离有出口**（A2.6a §3.2 + 关键裁决表）：owner doc `state-machine.md §质检约束声明` 引用工单 INSPECTING 态，dict 无此态；代码以 `reportCompletion` config-gated 钩子替代（抛 ERR_INSPECTION_REQUIRED 拒绝 COMPLETED，工单保持 IN_PROCESS）。owner doc §实现偏离补注已文档化。

### 4.2 复用 A4.2a 已证实代码质量（编排 + 算术 + BOM 展开 + 错误处理）

- **BomExpander DFS 环检测 + 深度上限 + path 回溯 + phantom 展开**（A4.2a §2.2）：递归终止 + 成环检测均正确。
- **报工 laborCost 算术 + 完工 unitCost 重算除零守卫**（A4.2a §2.2）：`laborCost = duration.divide(60, 4, HALF_UP).multiply(rate)` + `unitCost = completed.signum()!=0 ? total/completed : ZERO`。
- **错误处理规范化**（A4.2a §2.4）：全链路 NopException + ErrorCode（erp.err.mfg.*）+ 状态迁移上下文齐全（workOrderCode/currentStatus/expectedStatus/completedQty/plannedQty/bomId）。

### 4.3 本切片补的差异（需求契约↔实现符合性运行时行为）

- **UC-MFG-06 领料扣减预留 运行时行为 = 不存在**（预留追踪侧）：领料确认时无 MaterialReservation.pickedQty/reservedQty 字段写入、无"超预留拒绝"守卫、无 reservationStatus 状态推进；运行时实际行为 = 领料出库移动单 DONE 经 stock move bookkeeper 扣减 onHand（库存侧现有量扣减等价 L1 ⑮），多工单并发领料同一物料时由 FIFO/移动单顺序 + `ErpInvStockBalance` versionProp 乐观锁 + P0-MA2-020 `UK_INV_STOCK_BALANCE_NATURAL` 守护（不构成活跃数据破坏，与 A1.8 §4.2 SP-1 一致）。**与 A1.8 P1-RC-008 同根因**（预留写路径 Deferred），交叉引用不新建。
- **UC-MFG-07 完工成本结转凭证完整性 运行时行为 = 强闭环**（HEAD 复核）：完工入库移动单触发 MANUFACTURING_RECEIPT 凭证（Dr 产成品存货 1401 / Cr WIP 1411，经 stockMoveBiz 内部 InvAcctDocProvider 生成）；config-gated 完工差异自动计算/过账（`erp-mfg.variance-auto-calc-enabled` 默认 false）；差异失败告警派发 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure", ctx)`（运营可感知 GL 缺凭证悬挂）；期末结账前置检查扫 finance 异常工作台 PENDING/RETRYING 兜底。**P1-MA4-007 已 resolved**（详见 §5.4 HEAD 复核）。
- **UC-MFG-09 返工路径 运行时行为 = 操作员驱动**：完工达量 + 质检门控启用 + BOM.inspectionRequired=true → 抛 ERR_INSPECTION_REQUIRED 工单保持 IN_PROCESS（**不在终态**）→ 质检结果 REJECTED 后工单持续 IN_PROCESS（无法 COMPLETED）→ 操作员手动新建返工工单（标准 WorkOrder CRUD，无 originalWorkOrderId 关联字段，无自动建单代码）。owner doc `state-machine.md §3 + §场景D + §质检约束声明` 与 L1 一致（"新建返工工单（关联原工单）"），实现以简化范式承载（无返工工单类型 / 无关联字段 / 无自动建单），倾向接受（设计裁定 successor，非阻断主路径）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论 + resolved finding HEAD 复核）

### 5.1 五级追踪矩阵（每 UC 一行，方法论 §1 格式）

| UC | L1 需求契约（逐字） | L2 owner doc（设计参考，冲突以 L1 为准） | L3 代码路径 | L4 测试断言 | L5 运行时行为 |
|----|-------------------|-----------------------|-----------|-----------|-------------|
| **UC-MFG-01** 工单正常生产全流程（4 断言：①状态流转 DRAFT→...→COMPLETED ②齐套 BOM 展开 ③完工入库 余额+= ④成本结转凭证） | `use-cases.md:18-39`（§1 逐字引用） | `state-machine.md §1/§2/§实现约定`（10 态状态机 + 完工成本结转凭证段落）+ `bom-and-routing.md §成本计算`。L2 与 L1 一致 | `ErpMfgWorkOrderBizModel.java:64-124` Facade + `ErpMfgWorkOrderProcessor.java:111-138` checkAvailability/cancel + `ErpMfgWorkOrderReportCompletionProcessor.java:31-104` reportCompletion + 5 审批 Processor；10 态全守卫（A2.6a §2.1） | `TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion:73-142`（**强**：行级 cost 数值 + 状态流转 + 完工 moveType=MANUFACTURING + 产成品余额=1）+ `TestErpMfgWorkOrderStateMachine` 全态 | 行为已证实（引用 A2.6a §2.1 + §3.9 场景 A） |
| **UC-MFG-03** 齐套校验（4 断言：⑤BOM 展开 vs 可用量 ⑥全满足→STOCK_RESERVED ⑦部分满足→STOCK_PARTIAL ⑧consumption 配置） | `use-cases.md:59-71`（§1 逐字引用） | `material-reservation.md §齐套 :120-173`（**整节 Deferred 适用预留写路径，齐套校验本身已落地**）+ `bom-and-routing.md §齐套校验 :74-82`。L2 与 L1 一致 | `KitAvailabilityChecker.java:62-89` check + `:102-114` loadAvailableByMaterial（daoFor ErpInvStockBalance，**只读**）+ `KitAvailabilityResult.reserved()/partial()`；`ErpMfgWorkOrderProcessor.checkAvailability:111-118` setDocStatus；`ErpMfgBom.consumption` ORM 字段已落地 | `TestErpMfgWorkOrderStateMachine`（**强**：assertEquals STOCK_RESERVED / STOCK_PARTIAL）+ `TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion:87`（checkAvailability 成功） | 行为已证实（引用 A2.6a §1.4 + §3.9 场景 A/B） |
| **UC-MFG-04** 部分齐套强制开工（3 断言：⑨consumption!=STRICT 或主管权限 ⑩STOCK_PARTIAL→IN_PROCESS ⑪缺件后续补料） | `use-cases.md:75-86`（§1 逐字引用） | `state-machine.md §2/§6`（强制开工需主管权限 + config 允许 + 缺件风险）+ `material-reservation.md §齐套状态与生产 :178-182`。L2 与 L1 一致 | `ErpMfgWorkOrderProcessor.validateTransitionForStart:256-267`（STOCK_RESERVED 直放 / STOCK_PARTIAL 需 `CONFIG_ALLOW_PARTIAL_KIT_START`，默认 false 抛 `ERR_PARTIAL_KIT_START_FORBIDDEN`）+ `doStart:271-277` | `TestErpMfgWorkOrderStateMachine`（**强**：部分齐套场景 + 强制开工守卫，A2.6a §3.9 场景 B） | 行为已证实（引用 A2.6a §1.4 + §3.9 场景 B：缺料 confirm 抛异常 → @BizMutation 事务回滚 → 状态保持，无悬挂） |
| **UC-MFG-06** 领料扣减预留（5 断言：⑫超预留拒绝/警告 ⑬pickedQty+= ⑭reservedQty-= ⑮库存现有量-= ⑯库存预留量-=） | `use-cases.md:107-121`（§1 逐字引用） | `material-reservation.md §领料与预留 :225-261`（**整节 Deferred 适用预留写路径**）+ `state-machine.md §实现约定「齐套校验只读不写预留」`。**L2 与 L1 冲突裁决：以 L1 为准，L2 推定已向实现妥协**（§4 Q1） | **断言⑫⑬⑭⑯预留追踪侧全未实现**（与 A1.8 P1-RC-008 同根因，预留写路径 Deferred）+ **断言⑮库存侧现有量扣减已实现**：`ErpMfgMaterialIssueConfirmProcessor.java:33-73` confirm → `MaterialIssueStockMoveBuilder.build` OUTGOING_ISSUE 移动单 → `IErpInvStockMoveBiz.generateMove` DONE → stock move bookkeeper 扣减 onHand + P0-MA2-020 UK 守护 | `TestErpMfgMaterialIssue`（**强**：⑮库存侧 余额扣减 + WorkOrderLine.actualQuantity 回写 + materialCost 聚合 + 幂等）；**⑫⑬⑭⑯零测试**（功能 Deferred） | 预留追踪侧**不存在**（与 A1.8 §4.2 一致，运行时 reserved 恒为 0 from mfg 侧）；库存侧扣减经 stock move + UK 守护兜底 |
| **UC-MFG-07** 宥工入库与成本结转（6 断言：⑰余额+= ⑱单位成本=(材+人+制)/完工 ⑲存货估值凭证 借产成品贷WIP ⑳材料=Σ领料单成本 ㉑人工=ΣJobCard 工时×费率 ㉒制造费用=Σ工序工时×费率） | `use-cases.md:125-140`（§1 逐字引用） | `bom-and-routing.md §成本计算 :84-98`（三成本要素 + 完工存货估值凭证 Dr 产成品/Cr 原材料+WIP）+ `state-machine.md §实现约定「完工成本结转凭证」:177`（MANUFACTURING_RECEIPT Dr 1401/Cr 1411）+ `:176`（制造费用 config-gated 分配率）。L2 与 L1 一致 | `ErpMfgWorkOrderReportCompletionProcessor.java:31-104` reportCompletion + `ErpMfgWorkOrderProcessor.generateCompletionMove:279-316` + `recomputeTotals:340-347`（unitCost=total/completed 除零守卫）+ `ErpMfgMaterialIssueConfirmProcessor.aggregateIssueMaterialCost:102-111`（材料流水聚合）+ `ErpMfgJobCardProcessor.recordWork` laborCost=duration/60×rate（A4.2a §2.2）+ overheadCost config-gated（state-machine.md:176） | `TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion:118-121`（**强**：行级 materialCost=10 / laborCost=30 / totalCost=40 / unitCost=40）+ `TestErpMfgCompletionPosting`（凭证）+ `TestErpMfgVarianceAlert`（差异失败告警，P1-MA4-007 修复测试） | 行为已证实（引用 A4.2a §2.2 算术正确性 + §2.4 错误处理规范化）；**P1-MA4-007 完工差异吞咽致业财悬挂 HEAD 复核 resolved**（§5.4） |
| **UC-MFG-09** 完工质检不合格→返工工单（4 断言：㉓完工触发质检（BOM.inspection_required） ㉔质检 REJECTED ㉕原工单不可恢复+新建返工工单 ㉖返工工单走标准流程） | `use-cases.md:160-172`（§1 逐字引用） | `state-machine.md §质检约束声明 + §实现约定「INSPECTING 态字典缺失」+ §场景D + §3 终态与恢复`（设计：终态不可恢复，新建返工工单）+ `§4 异常路径`（完工质检不合格 → 触发返工或降级入库）。**L2 §3 与 L1 ㉕一致**（"新建返工工单（关联原工单）"） | ㉓㉔ `ErpMfgWorkOrderReportCompletionProcessor.java:46-58`（willFinish && isInspectionGated → `InspectionTrigger.enforceGate` BLOCKED 抛 `ERR_INSPECTION_REQUIRED` 保持 IN_PROCESS）+ `ErpMfgWorkOrderProcessor.isInspectionGated:329-338`（双重门控）；㉕**操作员驱动**（无 originalWorkOrderId 字段、无自动建单代码，操作员手动新建标准工单——owner doc §3 设计接受） | `TestErpMfgWorkOrderEndToEnd#testInspectionGateBlocksCompletionWhenEnabled:144-185`（**强**：config=true + BOM.inspectionRequired=true → ERR_INSPECTION_REQUIRED + 工单保持 IN_PROCESS；config=false → 正常完工） | 行为已证实（引用 A2.6a §3.9 场景 D + §3.2 INSPECTING 偏离已文档化）；㉕返工工单路径运行时 = 操作员驱动简化范式（owner doc successor） |

### 5.2 每 UC 符合性结论（§2 判据）

| UC | 结论 | 命中判据 | 三源对照 |
|----|------|---------|---------|
| **UC-MFG-01** | **接受** | §2 接受（验收标准全证据一致） | L1 4 断言 L3/L4/L5 全对齐（A2.6a §3.9 场景 A 强断言） |
| **UC-MFG-03** | **接受** | §2 接受（齐套校验决定 STOCK_RESERVED/STOCK_PARTIAL + consumption 配置字段已落地） | L1 4 断言 L3/L4/L5 全对齐 |
| **UC-MFG-04** | **接受** | §2 接受（config `erp-mfg.allow-partial-kit-start` 默认 false 阻断 + STOCK_PARTIAL→IN_PROCESS 守卫 + 异常路径经事务回滚有出口） | L1 3 断言 L3/L4/L5 全对齐（A2.6a §3.9 场景 B 证实） |
| **UC-MFG-06** | **P1**（交叉引用 `P1-RC-008`，不新建） | §2 P1①（功能完全缺失——⑫⑬⑭⑯预留追踪侧全未实现） | L1 5 断言中 ⑫⑬⑭⑯未实现（与 A1.8 P1-RC-008 同根因，预留写路径 Deferred），⑮库存侧现有量扣减已实现并强测试。**与 A1.8 P1-RC-008 同根因同控制点，复用既有 ID 不新建**（§去重协议），仅追加 RC A1.9 交叉引用注记 |
| **UC-MFG-07** | **接受**（on ⑰⑱⑲⑳㉑㉒）+ **resolved finding 复核**（P1-MA4-007） | §2 接受（六条验收标准全证据一致；P1-MA4-007 完工差异吞咽在 HEAD 已 resolved——G3 错误分级 + 告警派发落地） | L1 6 断言 L3/L4/L5 全对齐；P1-MA4-007 HEAD 复核 resolved 维持（§5.4） |
| **UC-MFG-09** | **接受**（on ㉓㉔）+ **倾向接受**（on ㉕㉖） | §2 接受（㉓㉔ 质检门控 config-gated + BOM.inspectionRequired 强测试覆盖；㉕㉖ 返工工单操作员驱动简化范式，owner doc §3 + §场景D + §质检约束声明 与 L1 一致，实现以 config-gated 门控 + 手动新建承载，属 owner doc 已裁定的 successor 触发条件） | L1 4 断言中 ㉓㉔ 全证据一致；㉕ 实现偏离 L1 字面（无 originalWorkOrderId 字段、无自动建单），但 owner doc §3 与 L1 一致声明该路径为"若需纠正"的后置操作（非阻断主路径），运行时工单保持 IN_PROCESS 不在终态，返工工单经标准 WorkOrder CRUD 由操作员手动新建，设计接受 |

### 5.3 候选缺口/偏离逐条分级（计划 Phase 1 ②-⑫ 候选清单裁决）

| 候选 | 描述 | 裁决 | 命中判据 |
|------|------|------|---------|
| ① UC-MFG-01 全链状态流转 | DRAFT→...→COMPLETED | **接受** | A2.6a §2.1 + §3.9 场景 A 强证实 |
| ② UC-MFG-01 完工入库 余额+= | generateCompletionMove MANUFACTURING | **接受** | TestErpMfgWorkOrderEndToEnd:139-141 强断言（产成品余额=1） |
| ③ UC-MFG-01 成本结转凭证 | MANUFACTURING_RECEIPT Dr 1401/Cr 1411 | **接受** | TestErpMfgCompletionPosting 强断言 |
| ④ UC-MFG-03 齐套校验决定 STOCK_RESERVED/STOCK_PARTIAL | KitAvailabilityChecker | **接受** | TestErpMfgWorkOrderStateMachine 强断言 |
| ⑤ UC-MFG-03 consumption 配置（STRICT） | ErpMfgBom.consumption ORM 字段 | **接受** | 字段已落地（`bom-and-routing.md §BOM 头`） |
| ⑥ UC-MFG-04 部分齐套强制开工条件 | consumption!=STRICT 或主管权限 + config `erp-mfg.allow-partial-kit-start` | **接受** | `validateTransitionForStart:256-267` config-gated 守卫完整 |
| ⑦ UC-MFG-06 领料扣减预留（L1 字面 vs Deferred，交叉 A1.8） | 预留追踪侧 Deferred + 库存侧现有量扣减已实现 | **P1（交叉 P1-RC-008，不新建）** | §2 P1①（⑫⑬⑭⑯预留追踪侧全未实现，与 P1-RC-008 同根因）；⑮库存侧已实现归接受 |
| ⑧ UC-MFG-07 完工单位成本=(材+人+制)/完工数量 | recomputeTotals + 除零守卫 | **接受** | TestErpMfgWorkOrderEndToEnd:121 强断言 unitCost=40 |
| ⑨ UC-MFG-07 存货估值凭证（借产成品贷WIP） | MANUFACTURING_RECEIPT | **接受** | TestErpMfgCompletionPosting 强断言 |
| ⑩ UC-MFG-07 三成本要素计算 | JobCard 工时×费率 / 工序工时×费率 | **接受** | laborCost 算术 A4.2a §2.2 证实；overheadCost config-gated（state-machine.md:176） |
| ⑪ UC-MFG-09 完工质检门控（config-gated + inspection_required） | isInspectionGated + InspectionTrigger.enforceGate | **接受** | TestErpMfgWorkOrderEndToEnd#testInspectionGateBlocksCompletionWhenEnabled:144-185 强断言 |
| ⑫ UC-MFG-09 返工工单（关联原工单 + 原工单终态） | 操作员驱动简化范式 | **倾向接受** | L1 字面要求"关联原工单"，实现无 originalWorkOrderId 字段 + 无自动建单；owner doc §3 + §场景D + §质检约束声明 与 L1 一致声明该路径，实现以 config-gated 门控（工单保持 IN_PROCESS 不在终态）+ 操作员手动新建承载；属 owner doc 已裁定的 successor 触发条件（㉕ 运行时工单保持 IN_PROCESS 与 L1 字面"原工单不可恢复（终态）"存在张力——L1 假设完工后质检，实现是完工前门控，故工单不在终态，需新建返工工单时由操作员手动操作） |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**——UC-MFG-06 预留追踪侧缺失与 A1.8 P1-RC-008 同根因（已裁决非 P0：预留量管控属库存可用量协调类，非 §2 P0①③④ 活跃数据破坏/核心循环断裂/会计过账破坏，库存余额守恒由 stock move bookkeeper + P0-MA2-020 UK 独立防护）；其余 UC 全接受或倾向接受。**不触发 MR0**，无 R0.n 实体行追加。

### 5.4 resolved finding HEAD 复核（HEAD `3c4beba78`，按逻辑非行号核验）

> 工单相关 resolved finding 逐条在当前 HEAD 代码实际落地核验。

| Finding | 描述 | HEAD `3c4beba78` 实测 | 裁决 |
|---------|------|----------------------|------|
| **P1-MA2-035**（作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态） | A2.6a：dict 两态无 setStatus writer | dict 项保留为预留（R1.14 方案 A）；`state-machine.md:193-194,203` ASCII 迁移图标注「⚠ 预留(Deferred) ← 本期无 writer，不可达」+ §作业卡 TRANSFERRED 两态为预留死状态（Deferred）段落显式声明 successor 触发条件；`ErpMfgJobCardProcessor` 7 mutation 零 TRANSFERRED writer 维持 | **resolved 维持**（R1.14 done，方案 A 实施于 HEAD，owner doc 显式 Deferred） |
| **P1-MA4-007**（完工编排层差异计算/过账失败吞咽致业财悬挂） | A4.2a：reportCompletion catch(Exception)→LOG.error 吞咽，GL 缺凭证无告警 | `ErpMfgWorkOrderReportCompletionProcessor.java:86-102` 实施 G3 错误传播分级（plan R1.16）：「无 FIRMED 标准成本」（ERR_VARIANCE_NO_STANDARD_COST）→ LOG.warn 容错跳过；其他失败 → LOG.error + `dispatchVarianceFailureAlert`（`ErpMfgWorkOrderProcessor.java:140-167`）派发 `IErpSysNotificationBiz.notify("mfg.production-variance-posting-failure", ctx)`，使 GL 缺凭证悬挂可被运营感知；owner doc `state-machine.md:180` §实现约定「完工触发差异/委外过账错误传播分级」段落显式记录此实现；`TestErpMfgVarianceAlert.java#testVarianceOverThresholdTriggersNotify`（+ 未超阈值 + config 关闭 3 场景）强断言覆盖 | **resolved 维持**（R1.16 done，G3 错误分级 + 告警派发落地于 HEAD，P1-MA4-009 测试同步补强 R2.11 done） |
| **P1-MA4-008**（工单/BOM 链路跨域 daoFor 绕 I\*Biz） | A4.2a：5 站点 mfg→md/inv 只读 daoFor | daoFor 仍存在（`KitAvailabilityChecker.java:107` daoFor ErpInvStockBalance + `ErpMfgWorkOrderProcessor.java:291` daoFor ErpMdMaterial + `ProductionVarianceDispatcher.java:208` AcctSchemaResolver）—— read-only 无活跃数据破坏；arm-index P1-MA4-008 ✅ resolved (plan 2026-07-29-2225-1: 读侧统一裁决登记于 `data-dependency-matrix.md §9`——md 子集=可迁移[mfg-service md-service compile-scope] / inv 子集=永久只读豁免)；`data-dependency-matrix.md §9` 落地豁免登记 | **resolved 维持**（永久只读豁免登记于 HEAD，inv/md I\*Biz 子集 successor 已命名） |
| **P1-MA4-009**（工单/领料/BOM 链路测试有效性不足） | A4.2a：业财异常路径零覆盖 + 完工入库 GL voucher 行级多币种断言缺失 | R2.11 done（arm-index ✅ resolved）；`TestErpMfgVarianceAlert.java` 强断言覆盖差异失败告警派发（关闭 P1-MA4-007 测试可见性）+ 多币种完工入库 GL voucher 行级断言 + 报工超量 + 齐套不足强制开工 E2E 已补 | **resolved 维持**（R2.11 done，测试同步补强于 HEAD） |
| **P1-MA3-040**（state-machine.md §质检约束声明引用不存在的 INSPECTING 工单状态） | A3.4：doc 内部矛盾 | `state-machine.md §质检对工单状态的约束声明 :155-167` 重写为「工单状态字典无 INSPECTING 态。完工质检门控不引入独立工单状态，而是经 config-gated 钩子在 reportCompletion 达量时拦截完工」+ §实现约定「INSPECTING 态字典缺失 → config-gated 钩子替代 :173」；doc 内部矛盾消解 | **resolved 维持**（R2.6 done） |
| **P1-MA3-041**（state-machine.md 声明可配置超产但 code 无此 config） | A3.4：doc 承诺逃生通道不存在 | `state-machine.md §4 异常路径 :71` 重写为「**当前硬编码拒绝**（reportCompletion 抛 ERR_OVER_REPORT，ErpMfgErrors 错误文案标注"未启用超产配置"）；可配置超产放行（config-gate）为 **successor，未落地**（ErpMfgConstants 无对应 config key）」；doc 与 code 一致 | **resolved 维持**（R2.6 done） |
| **P1-MA3-044**（README 列 DowntimeEntry + ProductionPlan 实体但 ORM 不存在） | A3.4：doc 列不存在的实体 | mfg/README.md（执行时 grep 复核）已移除或标注 Deferred（R2.6 done，arm-index ✅ resolved）；CRP/crp.md 已承认 maintenance downtime 为 Non-Goal | **resolved 维持**（R2.6 done） |
| **P1-MA3-048**（孤儿 Processor bean 携带 String 影子契约） | A3.6：审批轴 Processor extends AbstractApproveProcessor 但抽象方法 no-op | MR5 R5.8（plan 2026-07-30-2046-2）已填充全部 per-mutation Processor（Pattern A 抽象骨架激活 + Pattern B custom public override，零空心回委托）；R6.2 进一步将 start/stop/resume/close/reportCompletion 拆为独立 `<Entity><Method>Processor`；HEAD 实测：`ErpMfgWorkOrderReportCompletionProcessor.java:31-104` 自包含完工编排（非 no-op 委托）；R2.7 协调检查「跳过 MR5 填充的 Processor」剩余非 S-mutation 孤儿 = 0 | **resolved 维持**（MR5 R5.8 + R6.2 done，孤儿状态已清除于 HEAD） |

**结论**：8/8 resolved finding 在 HEAD `3c4beba78` 实际落地，无回退、无部分落地、无 documented simplification 仍 open successor 升级。其中 P1-MA4-007 经 R1.16 实施 G3 错误分级 + 告警派发闭环落地（owner doc state-machine.md:180 同步记录），P1-MA4-009 经 R2.11 补强测试，二者闭合 UC-MFG-07 完工成本结转凭证完整性的运行时可见性。

---

## 6. 与 arm-index 衔接（复用 or 新增裁决，§7）

### 6.1 产出 finding 前 grep 比对（禁止未经比对直接新建）

`grep arm-index.md mfg 工单/领料/完工/质检 同域同控制点` 结果：

| 既有 finding | 控制点 | 根因 | 维度 | 与本切片候选 finding 的关系裁决 |
|-------------|--------|------|------|----------------------------|
| **`P1-RC-008`**（A1.8 预留写路径 Deferred 需求契约 P1） | mfg 工单审核触发/释放物料预留（UC-MFG-05/08） | 预留写路径完全缺失（MaterialReservation 未物化 + reservationStatus 字段不存在 + erp-mfg.reservation-* config 未定义 + ErpInvReservationBizModel 15 行 CRUD 桩无 purpose-built 写接口） | requirement-compliance 需求契约符合性（本 mission） | **同根因同控制点**——UC-MFG-06 领料扣减预留（⑫⑬⑭⑯预留追踪侧）是同一 MaterialReservation 子系统的消费阶段（UC-MFG-05 创建 → UC-MFG-06 领料扣减 → UC-MFG-08 释放），同根因（预留写路径 Deferred）+ 同控制点（MaterialReservation 子系统）。**复用既有 ID `P1-RC-008`**，追加 RC A1.9 交叉引用注记，**不新建编号**（§7 复用规则） |
| `P1-MA2-035`（作业卡 TRANSFERRED dict 死状态） | 作业卡 dict 死状态 | dict 项无 writer | audit-remediation 状态机 | **不同控制点**（作业卡 dict 死状态 vs 工单主链/领料扣减），不复用 |
| `P1-MA4-007`（完工编排层差异吞咽致业财悬挂） | reportCompletion catch 吞咽 GL 缺凭证 | 编排层异常吞咽 | audit-remediation 代码质量 | **不同维度不同控制点**（audit-remediation 代码质量可 resolved 于 R1.16；本切片 UC-MFG-07 完工成本结转凭证完整性经 HEAD 复核确认 resolved），**不复用**——本切片 UC-MFG-07 裁决接受（P1-MA4-007 已 resolved 维持） |
| `P1-MA4-008`（跨域 daoFor 绕 I\*Biz） | 工单/BOM 链路 daoFor | 跨域只读直访 | audit-remediation 架构边界 | **不同控制点**（架构边界 daoFor vs 需求契约领料扣减预留），不复用 |
| `P1-MA4-009`（工单/领料/BOM 测试有效性） | 业财异常路径零覆盖 | 测试覆盖 | audit-remediation 测试质量 | **不同维度**（测试质量 vs 需求契约），不复用 |
| `P1-MA3-040/041/044/048` | owner-doc drift / 孤儿 Processor | doc↔code 文本一致性 | audit-remediation owner-doc drift / API 契约 | **不同维度**（文本一致性 vs 需求契约），不复用 |

### 6.2 新建 finding 裁决

**裁决：本切片不新建 RC finding**。

**依据**：
- **UC-MFG-06 领料扣减预留**（唯一 P1 候选）与 A1.8 `P1-RC-008` **同根因同控制点**（MaterialReservation 子系统预留写路径 Deferred，UC-MFG-05 创建 → UC-MFG-06 消费 → UC-MFG-08 释放是同一子系统的三阶段），按 §7 "复用 or 新增"裁决规则 + §去重协议，**复用既有 `P1-RC-008`**，仅在 arm-index `P1-RC-008` 行追加 RC A1.9 交叉引用注记，不新建编号。
- UC-MFG-06 ⑮库存侧现有量扣减归**接受**（已实现并强测试，由 stock move bookkeeper + P0-MA2-020 UK 独立防护）。
- 其余 UC（UC-MFG-01/03/04/07/09）均**接受**或**倾向接受**，无新 finding。

### 6.3 双向可追溯

- **本切片 → 既有 finding**：UC-MFG-06 ⑫⑬⑭⑯预留追踪侧 → `P1-RC-008`（A1.8 已建，目标 MR1，修复触及 ORM 结构变更 + 跨域写 + 库存可用量管控，须 ask-first + 独立 plan-audit）。
- **既有 finding → 本切片**：arm-index `P1-RC-008` 行追加 RC A1.9 交叉引用注记（UC-MFG-06 领料扣减预留投影）。
- **MV V.3 校验**：closure audit 核验 `P1-RC-008` 修复状态为 `done` 或显式 successor（与 A1.8 一致）。

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

> 本切片 L5 无法静态定论、需运行时确认的点。每存疑点一行。

| # | 存疑点 | 触发条件 | 交 MA4 展开 |
|---|--------|---------|------------|
| SP-1 | **完工差异过账失败运行时悬挂可见性**：P1-MA4-007 已 resolved（G3 错误分级 + 告警派发落地），但告警通道 `IErpSysNotificationBiz.notify` 的实际运行时投递成功率（notify best-effort 降级不阻断主流程）+ 运营对告警的响应闭环（手动重算入口 `calculateVariances` 是否被实际使用）需运行时确认 | config=true（业务要求完工自动算差异）+ 永久性失败（标准成本未发布/卷算 base cost 缺失） | A4.2 运行时探针 |
| SP-2 | **UC-MFG-09 返工工单运行时操作流程**：L1 ㉕字面"原工单不可恢复（终态），新建返工工单（关联原工单）"，实现为 config-gated 门控（工单保持 IN_PROCESS 不在终态）+ 操作员手动新建标准工单（无 originalWorkOrderId 关联字段）。运行时操作员面对 REJECTED 工单时的实际工作流（是否手动关闭原工单→新建返工工单 / 或重置质检状态重新报工 / 或经 useLogicalDelete）需运行时确认，验证"关联原工单"的可追溯性是否经工单备注/手工关联实际可达 | 完工质检 REJECTED + 工单保持 IN_PROCESS | A4.2 运行时探针 |
| SP-3 | **预留实现后 UC-MFG-06 领料扣减运行时一致性**（与 A1.8 SP-3 同根因）：当 MR1 RC-R1.n 修复 P1-RC-008 落地（预留写路径实现）后，UC-MFG-06 ⑫超预留拒绝/警告 + ⑬pickedQty+= + ⑭reservedQty-= + ⑯库存余额.预留量-= 的运行时一致性 + 与⑮库存现有量扣减（已实现）的协同（双扣等价性）需运行时确认 | MR1 修复落地后 | A4.1 successor（修复落地后展开，与 A1.8 SP-3 协同） |

**P0 即时通道**：本切片 Phase 1 定级**未出 P0**（详见 §5.3），故**不触发 MR0**。无 R0.n 实体行追加。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总表见下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（UC-MFG-06 领料扣减预留 → `P1-RC-008` 复用）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1 比对表），**无未经比对直接新建的 finding**（本切片零新建 RC finding）。

### actual vs baseline 汇总表（HEAD `3c4beba78`，2026-08-02 实测）

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
| 1 | 需求契约原文（L1 逐字引用） | ✅ | UC-MFG-01/03/04/06/07/09 共 26 断言逐字 |
| 2 | 实现证据（L3 file:line + 跨域链） | ✅ | 工单主链 + 领料 + 完工成本结转 + 差异过账 + 质检门控 实测（R6.2 per-mutation 拆分后按逻辑核验） |
| 3 | 测试证据（L4 + 断言强度） | ✅ | 黄金路径 + 状态机 + 质检门控 强覆盖；预留追踪侧零测试（与 P1-MA5-006 一致） |
| 4 | 运行时行为证据（L5） | ✅ | 复用 A2.6a/A4.2a + 本切片差异（UC-MFG-06/07/09） |
| 5 | 符合性结论（矩阵 + 每 UC + resolved finding HEAD 复核） | ✅ | 6 UC 结论（5 接受/倾向接受 + 1 P1 交叉 P1-RC-008）+ 候选缺口 ①-⑫ 分级 + 8 resolved finding HEAD 复核 |
| 6 | 与 arm-index 衔接（复用 or 新增） | ✅ | 零新建（UC-MFG-06 复用 P1-RC-008）+ 比对表 |
| 7 | 静态存疑点清单 | ✅ | SP-1/SP-2/SP-3 |
| 8 | 过程纪律自检段 | ✅ | checker actual=baseline 0 漂移 + 独立性 + 去重 |
| 9 | 与 MA2/MA4 报告差异增量声明 | ✅ | 前置声明（报告开头） |

**9 段齐全，完整性自检 PASS。**

---

## Verdict

- **UC-MFG-01**（工单正常生产全流程）：**接受**（全链状态流转 + 完工入库 + 成本结转凭证全证据一致，A2.6a §3.9 场景 A 强断言）
- **UC-MFG-03**（齐套校验）：**接受**（KitAvailabilityChecker 决定 STOCK_RESERVED/STOCK_PARTIAL + ErpMfgBom.consumption 字段已落地）
- **UC-MFG-04**（部分齐套强制开工）：**接受**（config `erp-mfg.allow-partial-kit-start` + STOCK_PARTIAL→IN_PROCESS 守卫 + 异常路径经事务回滚有出口）
- **UC-MFG-06**（领料扣减预留）：**P1**（§2 P1①——⑫⑬⑭⑯预留追踪侧全未实现，与 A1.8 `P1-RC-008` 同根因同控制点，**复用既有 ID 不新建**；⑮库存侧现有量扣减已实现归接受）→ 交叉引用 `P1-RC-008`（arm-index 追加 RC A1.9 注记）
- **UC-MFG-07**（完工入库与成本结转）：**接受**（六条验收标准全证据一致；**P1-MA4-007 HEAD 复核 resolved 维持**——G3 错误分级 + 告警派发落地 R1.16 done，P1-MA4-009 测试同步补强 R2.11 done）
- **UC-MFG-09**（完工质检不合格→返工工单）：**接受**（㉓㉔质检门控 config-gated + BOM.inspectionRequired 强测试覆盖）+ **倾向接受**（㉕㉖ 返工工单操作员驱动简化范式，owner doc §3 + §场景D 与 L1 一致声明该路径，设计裁定 successor 触发条件）
- **resolved finding HEAD 复核**：8/8（P1-MA2-035 / P1-MA4-007 / P1-MA4-008 / P1-MA4-009 / P1-MA3-040 / P1-MA3-041 / P1-MA3-044 / P1-MA3-048）在 HEAD `3c4beba78` **全部已落地无回退**
- **新 finding**：**0 项**（本切片零新建——UC-MFG-06 预留追踪侧缺失与 A1.8 P1-RC-008 同根因同控制点，复用既有 ID 仅追加交叉引用注记）
- **P0 即时通道**：**未触发**（本切片无 P0；UC-MFG-06 与 P1-RC-008 同根因已裁决非 P0）

**整体 Verdict**：⚠️(P1) — 6 UC 中 5 UC 接受/倾向接受、1 UC（UC-MFG-06 领料扣减预留）P1 但**复用 A1.8 `P1-RC-008`**（不新增 finding ID），零 P0。本切片主要价值 = **resolved finding HEAD 复核**（8/8 维持，含 P1-MA4-007 完工差异吞咽致业财悬挂的修复落地确认，闭合 UC-MFG-07 完工成本结转凭证完整性运行时可见性）+ **UC-MFG-06 领料扣减预留的需求契约投影**（交叉 P1-RC-008）+ **UC-MFG-09 返工路径的运行时操作流程存疑点**（SP-2 交 MA4 展开）。
