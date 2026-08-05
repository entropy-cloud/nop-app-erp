# A1.43 maintenance-F2 维护访问全流程(设备状态联动) + 备件消耗闭环(出库+凭证) — 需求-实现符合性五级追踪审计报告

> 报告类型：MA1(RC) 五级追踪审计（requirement-compliance mission）
> 工作项：A1.43（MA1 需求追踪矩阵审计 — maintenance-F2）
> UC 覆盖：UC-MAIN-03 / UC-MAIN-04（2 UC）
> 计划：`docs/plans/2026-08-06-0245-2-rc-ma1-a1-43-maintenance-f2-visit-sparepart.md`
> 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）
> 真相源层级：L1=`docs/design/maintenance/use-cases.md`（权威）；L2=`state-machine.md §2` + `equipment-integration.md §二/§三`（设计参考，冲突以 L1 为准）
> 本报告产出日：2026-08-05
> HEAD 复核 commit：工作树 dirty 仅文档变更，零 Java/ORM/契约变更

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/maintenance/use-cases.md`（功能契约真相源，权威性层级 2）

### UC-MAIN-03 维护访问全流程（use-cases.md:49-67）

**场景**：维护访问从排程到完成,设备状态联动。

**行为链路**（逐字引用 use-cases.md:54-58）：
```
维护访问 SCHEDULED → IN_PROGRESS(开始, 设备→MAINTENANCE)
  → 执行任务 + 消耗备件
  → COMPLETED(完成, 设备→RUNNING/IDLE)
```

**可验证断言**（逐字引用 use-cases.md:61-65）：
```
维护访问 IN_PROGRESS 时: 关联设备.状态 = MAINTENANCE
COMPLETED 时: 设备.状态 恢复(RUNNING/IDLE, 取决于排产)
设备状态由维护访问状态驱动(见 equipment-integration §三)
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-03-A**：SCHEDULED → IN_PROGRESS（开始），设备 → MAINTENANCE
- **UC-MAIN-03-B**：执行任务 + 消耗备件（IN_PROGRESS 承载任务/备件消耗）
- **UC-MAIN-03-C**：IN_PROGRESS → COMPLETED（完成），设备 → RUNNING/IDLE（取决于排产）
- **UC-MAIN-03-D**：设备状态由维护访问状态驱动

### UC-MAIN-04 备件消耗闭环（use-cases.md:71-84）

**场景**：维护访问消耗备件,触发出库移动单与维修费用凭证。

**可验证断言**（逐字引用 use-cases.md:76-82）：
```
维护访问记录备件消耗 →
  调用 IErpInvStockMoveBiz.generateConsumptionMove(出库)
  库存余额[备件] -= 消耗量
  生成凭证: 借 维修费用, 贷 存货
备件不足 → 校验失败(见 ../inventory cross-domain)
```

逐条验收标准拆解（进入 L5 判读）：
- **UC-MAIN-04-A**：维护访问记录备件消耗
- **UC-MAIN-04-B**：调用 IErpInvStockMoveBiz.generateConsumptionMove（出库）
- **UC-MAIN-04-C**：库存余额[备件] -= 消耗量
- **UC-MAIN-04-D**：生成凭证（借 维修费用，贷 存货）
- **UC-MAIN-04-E**：备件不足 → 校验失败（见 ../inventory cross-domain）

---

## 2. 实现证据（L3，代码路径 `file#method` + 关键行为断言）

> 引用格式锚点 = 文件路径 + 方法名 + 关键行为断言；行号为写时实测导航，漂移不构成引用失效。

### UC-MAIN-03 维护访问状态机 + 设备状态联动（start/complete 两侧设备联动均已实现）

调用链（全路径列全）：
- `module-maintenance/erp-mnt-service/.../entity/ErpMntVisitBizModel.java#schedule/start/complete/cancel`（:34/40/46/52，`@BizMutation` 薄 facade，R6.7 每 mutation 委派一 Processor）→
- `.../processor/ErpMntVisitStartProcessor.java#start`（:15，自包含 SCHEDULED→IN_PROGRESS 编排：状态守卫 `validateTransition(:17 要求 SCHEDULED)` + `doStart(:23 设状态翻转 + startTime 兜底 + 落库)` + **设备状态联动**）→
- `.../processor/ErpMntVisitCompleteProcessor.java#complete`（:27，自包含 IN_PROGRESS→COMPLETED 编排：状态守卫 `validateTransition(:29 要求 IN_PROGRESS)` + `doComplete(:35 设状态翻转 + endTime/totalMinutes/completedAt 计算 + 落库 + 维修工时 GL 过账 config-gated)` + **设备状态恢复**）→
- `.../processor/ErpMntVisitCancelProcessor.java#cancel`（:23，非终态守卫 `validateNotTerminal(:25)` + `doCancel(:31 设 CANCELLED + 维修工时 GL 红冲 config-gated)` + 设备状态恢复）。
- 设备状态联动器：`.../support/EquipmentStatusLinker.java#linkToUnderMaintenance(:24)` / `#restoreToRunning(:38)` / `#linkToDown(:31)`，经 config key `erp-mnt.equipment-status-link-enabled` 门控（`ErpMntConfigs.equipmentStatusLinkEnabled`，`DEFAULT_EQUIPMENT_STATUS_LINK_ENABLED=true` 默认开），`changeEquipmentStatus(:45-52)` 经 `IErpMntEquipmentBiz.get/updateEntity` 写 `ErpMntEquipment.status`。

关键行为断言：
- **UC-MAIN-03-A（start 侧 设备→MAINTENANCE）**：✅ `ErpMntVisitStartProcessor#start:19` 在状态翻转后调 `equipmentStatusLinker.linkToUnderMaintenance(visit.getEquipmentId(), context)` → `EquipmentStatusLinker#linkToUnderMaintenance:24-29`（config-gated + equipmentId 非空守卫）→ `changeEquipmentStatus(equipmentId, EQUIPMENT_STATUS_UNDER_MAINTENANCE)`。设备写入 dict 值 `UNDER_MAINTENANCE`（`_ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE="UNDER_MAINTENANCE"`，`app-erp-maintenance.orm.xml:56` dict `erp-mnt/equipment-status` option code=`UNDER_MAINTENANCE` label=`维护中`）。**语义对齐 L1「设备→MAINTENANCE（维护中）」**——dict 值 `UNDER_MAINTENANCE` 与 L1/L2 文本 `MAINTENANCE` 的命名差异见 §9（cosmetic，语义一致均表「维护中」）。
- **UC-MAIN-03-B（执行任务 + 消耗备件）**：✅ `ErpMntVisit` 实体承载 status(5 态)/visitDate/assignedTo/equipmentId/startTime/endTime/totalMinutes/completedAt；任务经 `ErpMntVisitTask`（独立实体）承载；备件消耗经 `ErpMntSparePartUsage`+`ErpMntSparePartUsageLine`（UC-MAIN-04 链路）。IN_PROGRESS 态可承载任务执行与备件消耗（confirm 在 IN_PROGRESS 期间触发，见 UC-MAIN-04）。
- **UC-MAIN-03-C（complete 侧 设备→RUNNING/IDLE）**：✅ `ErpMntVisitCompleteProcessor#complete:31` 在 `doComplete` 后调 `equipmentStatusLinker.restoreToRunning(visit.getEquipmentId(), context)` → `EquipmentStatusLinker#restoreToRunning:38-43` → `changeEquipmentStatus(equipmentId, EQUIPMENT_STATUS_RUNNING)`。**complete 侧设备状态恢复已实现**（恢复至 RUNNING）。⚠️ **IDLE 分支缺失**：L1 明确「恢复(RUNNING/IDLE, 取决于排产)」，但 `restoreToRunning` 恒恢复至 `RUNNING`——`EquipmentStatusLinker` javadoc:16-17 显式自承「设备无独立持久化的前置状态快照列，故以 RUNNING 作为标准运行态恢复；IDLE 设备恢复为 RUNNING 为已知的简化偏差，乐观锁保护并发覆盖」。即「取决于排产」的 IDLE 恢复分支未实现（无前置状态快照 + 无与排产协调判定），归 **P2-RC-061**（见 §5）。
- **UC-MAIN-03-D（设备状态由维护访问状态驱动）**：✅ 设备状态写入唯一站点 = `EquipmentStatusLinker.changeEquipmentStatus:45-52`，仅由维护访问 mutation（start→UNDER_MAINTENANCE / complete+cancel→RUNNING）与停机记录（DowntimeEntry record→DOWN / complete→RUNNING，`linkToDown`/`restoreToRunning`，A1.44 范围）驱动；config-gated 默认开（`DEFAULT_EQUIPMENT_STATUS_LINK_ENABLED=true`）。设备状态确由维护访问状态驱动。
- **状态机守卫完整性**（A2.14 已证实，复用）：`AbstractErpMntVisitProcessor#validateTransition(:45 要求精确前态)` / `#validateNotTerminal(:52 禁 COMPLETED/CANCELLED 再迁移)`，DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + 任意非终态→CANCELLED 全迁移守卫（A2.14 maintenance visit 5 态 PASS，本切片不重审状态机迁移维度，只补需求契约视角差异）。

### UC-MAIN-04 备件消耗出库 + 凭证过账（出库+过账主路径均已实现）

调用链（全路径列全，跨域 IErpInvStockMoveBiz 列全）：
- `.../entity/ErpMntSparePartUsageBizModel.java#confirm:29` / `#reverseConfirm:35`（`@BizMutation` 薄 facade，R6.7）→
- `.../processor/ErpMntSparePartUsageConfirmProcessor.java#confirm`（:21，自包含守卫 + 行加载 + 行非空校验 + 跨域出库 + session-reload + applyIssueResult + 落库 + 备件消耗 GL 过账）→
- `.../support/SparePartIssueService.java#issue`（:28，按行构造 `StockMoveRequest`[OUTGOING, relatedBillType=`RELATED_BILL_TYPE_MNT_SPARE_PART`, relatedBillCode=usage.code] 调 **跨域** `IErpInvStockMoveBiz.generateMove(request, context)`，与 purchase/sales 跨域模式一致）→
- 跨域 inventory：`module-inventory/erp-inv-service/.../entity/ErpInvStockMoveBizModel.java#generateMove` → `.../processor/ErpInvStockMoveGenerateMoveProcessor.java` → `ErpInvStockMoveProcessor`（OUTGOING + relatedBillType 非空 → 自动推进 DONE + `bookkeeper.bookCompletion` 扣余额）。
- 过账：`.../posting/MaintenanceIssuePostingDispatcher.java#dispatchIfApplicable`（:90，config-gated `erp-mnt.spare-part-posting-enabled` 默认关；billHeadCode=`usage.code+"-MI"` 幂等判重 `voucherAlreadyExists:210`；按出库流水 `loadLedgers:229` 汇总成本 `buildEvent:152` 装配 `PostingEvent` 经 `MntPostingExecutor.postEvent`）→ `.../posting/MaintenanceIssueAcctDocProvider.java#createFacts`（:62，Dr 维修费用 6602 汇总 / Cr 存货 1403 按物料分列）。
- 红冲：`.../processor/ErpMntSparePartUsageReverseConfirmProcessor.java#reverseConfirm`（:23，已过账守卫 `validateCanReverse` + GL 凭证红冲 `issuePostingDispatcher.reverseIssue:30` try-catch 吞异常幂等 + 反向 OUTGOING 移动单 `stockMoveBiz.reverse:46` try-catch 吞异常幂等 + session-reload + posted=false/docStatus=CANCELLED）。

关键行为断言：
- **UC-MAIN-04-A（记录备件消耗）**：✅ `ErpMntSparePartUsage`（消耗单头：code/equipmentId/warehouseId/orgId/businessDate/docStatus/posted/totalAmount）+ `ErpMntSparePartUsageLine`（行：materialId/uoMId/quantity/unitCost/amount）承载消耗记录；`confirm` Processor 加载行 `loadLines(:24)` + 行非空校验 `validateLinesNonEmpty`。
- **UC-MAIN-04-B（调用 generateConsumptionMove 出库）**：✅ **行为满足，方法名差异（见 §9）**：`SparePartIssueService#issue:55` 调 `IErpInvStockMoveBiz.generateMove(request, context)`，request 设 `moveType=MOVE_TYPE_OUTGOING` + `relatedBillType=RELATED_BILL_TYPE_MNT_SPARE_PART`。inventory 侧 `IErpInvStockMoveBiz` 接口**无 `generateConsumptionMove` 方法**（仅有 `generateMove`/`confirm`/`complete`/`cancel`/`reverse`/`findByRelatedBill`）——L1/L2 文本 `generateConsumptionMove` 为设计期命名，实现统一走 `generateMove(OUTGOING + relatedBillType)` 跨域范式（与 purchase/sales/mfg 一致），relatedBillType 非空自动 DONE 扣余额。命名差异记入 §9（cosmetic，行为等价）。
- **UC-MAIN-04-C（库存余额 -= 消耗量）**：✅ 跨域 inventory 行为：`ErpInvStockMoveProcessor` OUTGOING + relatedBillType 非空 → 自动 `complete` → `bookkeeper.bookCompletion(move, lines, acctSchemaId)` 更新 `ErpInvStockBalance`（现有量 -= 出库量）。本切片测试在 move 层断言 `DOC_STATUS_DONE`（出库完成即扣余额），余额直查属 inventory 域（A1.25-A1.27）行为，登记 SP-2 运行时确认。
- **UC-MAIN-04-D（凭证借维修费用贷存货）**：✅ **config-gated 默认关**（`DEFAULT_SPARE_PART_POSTING_ENABLED=false`，开启时生成凭证）：`MaintenanceIssueAcctDocProvider#createFacts:62-91` 贷方按物料分列 `fact(invSubject="1403", "存货", DC_CREDIT, lineAmount)`(:82) + 借方汇总 `fact(expenseSubject="6602", "维修费用", DC_DEBIT, totalAmount)`(:87)。科目映射经 config `erp-mnt.expense-subject-code`(默认 6602) / `erp-mnt.inventory-subject-code`(默认 1403) 可配。**P1-MA2-074 resolved R1.16**：过账失败 `dispatchIfApplicable:118-129` try-catch 吞异常告警 + `dispatchFailureAlert:133` 派发 `IErpSysNotificationBiz` 告警（`mnt.spare-part-posting-failure`），使 GL 缺凭证悬挂可被运营感知（过账吞异常悬挂维度已 resolved，本切片不重审该维度，§去重协议）。
- **UC-MAIN-04-E（备件不足 → 校验失败）**：✅ **跨域 inventory 行为**（L1 明确「见 ../inventory cross-domain」）：`ErpInvStockMoveProcessor#validateAvailable:116-136` 当 `available.compareTo(required) < 0` 抛 `ERR_AVAILABLE_INSUFFICIENT`（携带 materialId/warehouseId/available/required），经 `isNegativeStockAllowed:285`（config `erp-inv.allow-negative-stock` 默认 FALSE）门控——默认禁负库存即备件不足校验失败。本切片 maintenance 侧测试未直测不足路径（跨域 guard 属 inventory 域），登记 SP-3 运行时确认 OUTGOING auto-DONE 路径触发 validateAvailable。

### 三源对照小结（L1 ↔ L2 ↔ L3）

| 控制点 | L1（权威） | L2 设计参考 | L3 实仓 | 一致性 |
|--------|-----------|------------|--------|--------|
| start 设备→维护中 | use-cases.md:55「设备→MAINTENANCE」 | state-machine.md §2:34「设备进入维护中」+ equipment-integration.md §3.3:140「IN_PROGRESS→MAINTENANCE」 | ErpMntVisitStartProcessor#start:19 → linkToUnderMaintenance → UNDER_MAINTENANCE | ✅ 一致（语义；命名 UNDER_MAINTENANCE↔MAINTENANCE cosmetic 见 §9） |
| complete 设备→RUNNING/IDLE | use-cases.md:57「设备→RUNNING/IDLE」 | state-machine.md §2:35「设备恢复」+ equipment-integration.md §3.3:141「恢复为 RUNNING 或 IDLE」 | ErpMntVisitCompleteProcessor#complete:31 → restoreToRunning → 恒 RUNNING | ⚠️ 部分一致（RUNNING✅ / IDLE 分支缺失 P2-RC-061） |
| 设备状态由访问状态驱动 | use-cases.md:64 | equipment-integration.md §3.3:138-142 | EquipmentStatusLinker 唯一写站点，config-gated 默认开 | ✅ 一致 |
| 备件出库 | use-cases.md:78「generateConsumptionMove」 | equipment-integration.md §2.2:64「generateConsumptionMove()→CONSUMPTION 类型」 | SparePartIssueService#issue:55 → generateMove(OUTGOING) | ✅ 一致（行为；方法名/类型名 cosmetic 见 §9） |
| 库存余额-=消耗量 | use-cases.md:79 | equipment-integration.md §2.2:67「更新库存余额」 | inventory bookkeeper.bookCompletion（跨域） | ✅ 一致（跨域，SP-2 运行时确认） |
| 凭证借维修费用贷存货 | use-cases.md:80 | equipment-integration.md §2.3:85-87「借维修费用/贷存货」 | MaintenanceIssueAcctDocProvider#createFacts Dr6602/Cr1403 | ✅ 一致（config-gated 默认关，开启时生成） |
| 备件不足校验失败 | use-cases.md:81「见 cross-domain」 | equipment-integration.md §7.2:282「库存不足预警」 | inventory validateAvailable ERR_AVAILABLE_INSUFFICIENT 默认禁负库存 | ✅ 一致（跨域，SP-3 运行时确认） |

> L2 owner doc（state-machine.md §2 + equipment-integration.md §二/§三）与 L1 **一致要求**设备状态联动（start→维护中 / complete→恢复）+ 备件出库+凭证 + 备件不足校验，**无任何 Deferred / documented simplification 标注**显式裁剪这些验收标准。state-machine.md §实现约定与 Non-Goal（:156-167）显式延后项清单（停机通知制造 / 维修费用过账 / 预测性维护 / 校准 / 设备-资产价值联动 / 多级审批）**不含** UC-MAIN-03 设备状态联动与 UC-MAIN-04 备件出库+凭证（维修费用过账 Non-Goal 条目指「工时费率物化 successor」，备件消耗 MAINTENANCE_ISSUE 凭证已实现 config-gated）。`EquipmentStatusLinker` javadoc:16-17 自承 IDLE→RUNNING 简化属 **AI 代码层自标**，非 owner doc Deferred 段落，按 methodology §4 line 172「AI 自标 ≠ 人工批准」判据(ii)不成立（见 §5 P2-RC-061 三判据复核）。

---

## 3. 测试证据（L4，测试断言 + 断言强度）

> 来源：`module-maintenance/erp-mnt-service/src/test`。断言强度分级：强断言（断言验收标准语义）/ 弱断言 / 仅冒烟。

### UC-MAIN-03
- `TestErpMntVisitRequestStateMachine.java#testVisitHappyPathWithEquipmentLink`（:66）——**强断言**：seed 设备=RUNNING → schedule（断言设备仍 RUNNING「排程阶段设备状态不变」:75）→ start（断言 visit=IN_PROGRESS + **设备=UNDER_MAINTENANCE**「执行中设备置 UNDER_MAINTENANCE」:80）→ complete（断言 visit=COMPLETED + completedAt 已设 + totalMinutes 已计算 + **设备=RUNNING**「完成恢复设备 RUNNING」:88）。**直接覆盖 UC-MAIN-03-A（start 设备→维护中）+ UC-MAIN-03-C（complete 设备恢复 RUNNING）+ UC-MAIN-03-D（设备由访问状态驱动）**。
- `TestErpMntVisitRequestStateMachine.java#testVisitCancelRestoresEquipment`（:108）——**强断言**：start 后设备=UNDER_MAINTENANCE → cancel → 断言 visit=CANCELLED + **设备=RUNNING**「取消恢复设备 RUNNING」:122。覆盖 cancel 侧设备恢复。
- `TestErpMntVisitRequestStateMachine.java#testVisitTerminalCannotTransition`（:126）/ `#testVisitIllegalTransition`（:143）——**强断言**：终态不可再迁移（COMPLETED cancel/start 拒绝，抛 ERR_INVALID_VISIT_STATUS_TRANSITION）+ DRAFT 不可直接 start（须先 schedule）。覆盖状态机守卫（A2.14 复用）。
- **❌ IDLE 输入分支零测试**：上述测试均 seed 设备=RUNNING（`seedEquipment(EQUIPMENT_ID, EQUIPMENT_STATUS_RUNNING)`），**未 seed 设备=IDLE 验证 complete 后是否恢复 IDLE**——与实现同步偏离 L1「RUNNING/IDLE 取决于排产」的 IDLE 分支（P2-RC-061）。无测试覆盖 IDLE 设备经维护后状态。

### UC-MAIN-04
- `TestErpMntSparePartPosting.java#testSparePartPostingBasic`（:100）——**强断言**：seed 库存(20@5) + confirm → 断言 posted=true（库存已出库）+ move 存在且 DOC_STATUS_DONE + MAINTENANCE_ISSUE 凭证存在且 POSTED + **借方维修费用 6602 行 DEBIT 金额=50（10×5）** + **贷方存货 1403 行 CREDIT** + **借贷平衡**（drLine.debit==crLine.credit）。**直接覆盖 UC-MAIN-04-D（凭证借维修费用贷存货）行级断言**。
- `TestErpMntSparePartPosting.java#testSparePartPostingMultiMaterial`（:142）——**强断言**：2 物料（M1 2@5 + M2 1@8）→ 断言贷方 2 行（各物料存货科目）+ 贷方合计=18（10+8）+ 借方汇总=贷方合计 + 凭证行数=3（2 贷+1 借）。覆盖多物料分列凭证。
- `TestErpMntSparePartPosting.java#testSparePartPostingConfigDisabled`（:182）——**强断言**：config 关 → 断言 posted=true（库存已出库，向后兼容语义不变）+ move 存在 + **config 关闭不生成 GL 凭证**（findVoucher=null）。覆盖 config-gate 门控。
- `TestErpMntSparePartPosting.java#testSparePartPostingIdempotentNoDoubleDeduction`（:211）——**强断言**：confirm 产凭证后再次 dispatch → 断言**仅 1 张 MAINTENANCE_ISSUE 凭证**（billHeadCode 幂等判重）+ 贷方存货扣减=50（未翻倍）。覆盖幂等防双重扣减（与 assets MAINTENANCE_EXPENSE 防双重由不同业务类型保证）。
- `TestErpMntSparePartUsageReversal.java#testReverseConfirmRedReversesVoucherAndMove`（:98）——**强断言**：confirm 产凭证+移动单后 reverseConfirm → 断言原凭证 isReversed + 红字凭证行 + 反向 REVERSAL 移动单 DOC_STATUS_DONE + 原单保持 DONE + posted=false + docStatus=CANCELLED。覆盖红冲闭环（UC-MAIN-04 反向路径）。
- **❌ 库存余额直查零测试**：上述测试在 move 层断言 DOC_STATUS_DONE（出库完成），**未直查 `ErpInvStockBalance.totalQuantity` 验证余额 -= 消耗量**（余额扣减属 inventory 域 bookkeeper 行为，本切片在 move 层间接覆盖）。登记 SP-2。
- **❌ 备件不足路径零测试**：上述测试均 seed 充足库存（20>消耗 10），**未 seed 不足库存验证 confirm 抛 ERR_AVAILABLE_INSUFFICIENT**（跨域 inventory guard 属 inventory 域）。登记 SP-3。

### 测试运行结果（行为基线确认）
`mvn test -pl module-maintenance/erp-mnt-service -Dtest='TestErpMntVisitRequestStateMachine,TestErpMntVisitCancelReversal,TestErpMntSparePartPosting,TestErpMntSparePartUsageReversal'` → **Tests run: 18, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS**（2026-08-05 实测；VisitRequestStateMachine=9 / SparePartPosting=4 / VisitCancelReversal=3 / SparePartUsageReversal=2）。E2E：`tests/e2e/dashboards/maintenance.{smoke,value}.spec.ts`（看板域，不覆盖访问/备件链路）。

---

## 4. 运行时行为证据（L5）

- **无 maintenance 专属 MA2 报告**：`docs/audits/` 下 MA2 状态机/业财链路报告覆盖 finance/inventory/mfg/hr/purchase/sales/assets/quality/projects/crm/cs/contract/b2b/aps/logistics，**零 maintenance 访问/备件行为证据**。A2.14（`2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）覆盖 maintenance visit 5 态 + request 6 态状态机迁移 + EquipmentStatusLinker config-gated + DowntimeEntry 时间驱动，**证实状态机迁移守卫完整 + 设备状态联动 config-gated 机制存在**，但 A2.14 维度 = 状态机/链路行为视角，**不含 UC-MAIN-03「设备状态联动 start/complete 两侧语义」+ UC-MAIN-04「备件出库+凭证+不足校验」需求契约视角**。**本切片为 maintenance 访问+备件需求契约符合性的首份证据**（与 A1.42 并列 maintenance 域 RC 切片）。
- **P1-MA2-074 复用**（备件过账吞异常悬挂维度，resolved R1.16）：`MaintenanceIssuePostingDispatcher.dispatchIfApplicable` 曾 try/catch 吞异常致 posted=false 悬挂无告警，经 R1.16 resolved（complete 检查 postLabor 返回值 + 失败告警派发 `IErpSysNotificationBiz` + dispatcher 失败派发 `mnt.spare-part-posting-failure` 告警）。本审计**不重审过账吞异常悬挂维度**（§去重协议），仅引用其已修复现状作为 UC-MAIN-04-D 过账失败处理行为证据。
- **A2.14 复用**（visit 5 态状态机迁移守卫 + EquipmentStatusLinker config-gated 机制）：状态机 DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + 任意非终态→CANCELLED 全迁移守卫 + 设备状态联动 config-gated 默认开，已由 A2.14 证实，本切片不重审状态机迁移维度，只补需求契约视角差异（设备状态联动 start/complete 两侧语义 + IDLE 分支 + 备件出库+凭证+不足校验）。
- **存疑点交 MA4**（§7）：SP-1 complete 设备恢复 RUNNING 已静态+测试证实（IDLE 分支缺失归 P2-RC-061，低优先级运行时确认）/ SP-2 generateMove 出库运行时是否真实触发库存余额扣减（跨域 inventory bookkeeper）/ SP-3 备件不足校验失败路径运行时是否抛 inventory ERR（跨域 guard）。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC P0/P1/P2/接受）

### 五级追踪矩阵（逐验收标准进入 L5）

| UC | 验收标准 | L1（权威） | L2（设计参考） | L3（代码） | L4（测试） | L5（运行时/结论） |
|----|---------|-----------|---------------|-----------|-----------|------------------|
| UC-MAIN-03 | A start 设备→MAINTENANCE | use-cases.md:55 | state-machine.md §2:34 + equipment-integration.md §3.3:140 | ErpMntVisitStartProcessor#start:19 → linkToUnderMaintenance → UNDER_MAINTENANCE | testVisitHappyPathWithEquipmentLink 强（设备=UNDER_MAINTENANCE） | ✅ 接受（语义一致；命名 cosmetic 见 §9） |
| UC-MAIN-03 | B 执行任务+消耗备件 | use-cases.md:56 | state-machine.md §2:35 | ErpMntVisit 承载 + VisitTask + SparePartUsage（UC-MAIN-04） | testVisitHappyPathWithEquipmentLink 强（IN_PROGRESS 承载） | ✅ 接受 |
| UC-MAIN-03 | C complete 设备→RUNNING/IDLE | use-cases.md:57 | state-machine.md §2:35 + equipment-integration.md §3.3:141 | ErpMntVisitCompleteProcessor#complete:31 → restoreToRunning → 恒 RUNNING（无 IDLE 分支） | RUNNING 强 / IDLE 零 | RUNNING✅接受；IDLE 分支❌**P2**（P2-RC-061） |
| UC-MAIN-03 | D 设备状态由访问状态驱动 | use-cases.md:64 | equipment-integration.md §3.3:138-142 | EquipmentStatusLinker 唯一写站点，config-gated 默认开 | testVisitHappyPathWithEquipmentLink + testVisitCancelRestoresEquipment 强 | ✅ 接受 |
| UC-MAIN-04 | A 记录备件消耗 | use-cases.md:77 | equipment-integration.md §2.2:62 | ErpMntSparePartUsage + Line + confirm Processor | testSparePartPostingBasic 强（posted=true） | ✅ 接受 |
| UC-MAIN-04 | B generateConsumptionMove 出库 | use-cases.md:78 | equipment-integration.md §2.2:64 | SparePartIssueService#issue:55 → generateMove(OUTGOING)（方法名差异 §9） | testSparePartPostingBasic 强（move DONE） | ✅ 接受（行为；命名 cosmetic 见 §9） |
| UC-MAIN-04 | C 库存余额-=消耗量 | use-cases.md:79 | equipment-integration.md §2.2:67 | inventory bookkeeper.bookCompletion（跨域 auto-DONE） | move DONE 强 / 余额直查零（SP-2） | ✅ 接受（跨域，SP-2 运行时确认） |
| UC-MAIN-04 | D 凭证借维修费用贷存货 | use-cases.md:80 | equipment-integration.md §2.3:85-87 | MaintenanceIssueAcctDocProvider#createFacts Dr6602/Cr1403（config-gated 默认关） | testSparePartPostingBasic/MultiMaterial 强（行级+借贷平衡） | ✅ 接受（config-gated，开启时生成；P1-MA2-074 resolved） |
| UC-MAIN-04 | E 备件不足→校验失败 | use-cases.md:81 | equipment-integration.md §7.2:282 | inventory validateAvailable ERR_AVAILABLE_INSUFFICIENT 默认禁负库存（跨域） | 不足路径零（SP-3） | ✅ 接受（跨域 guard，SP-3 运行时确认） |

### 每 UC 符合性结论（取最高，§2 判据）

#### UC-MAIN-03 = **P2**（start/complete/驱动 主路径接受 + IDLE 恢复分支 P2）
- **接受部分**：UC-MAIN-03-A（start 设备→UNDER_MAINTENANCE）/ B（执行任务+消耗备件）/ C-RUNNING 分支（complete 设备恢复 RUNNING）/ D（设备状态由访问状态驱动）。维护访问 5 态状态机完整（A2.14 复用）+ start 侧 `linkToUnderMaintenance:19` + complete 侧 `restoreToRunning:31` + cancel 侧 `restoreToRunning:27`，config-gated 默认开，`testVisitHappyPathWithEquipmentLink` 强断言 RUNNING→UNDER_MAINTENANCE→RUNNING 全链路。
- **P2 部分**（**P2-RC-061**）：UC-MAIN-03-C **IDLE 恢复分支缺失**。命中判据 **§2 P2①（次要验收标准未完全满足——主路径 RUNNING→MAINTENANCE→RUNNING OK，边界场景 IDLE 弱）**——L1 明确「恢复(RUNNING/IDLE, 取决于排产)」，实仓 `restoreToRunning:38-43` 恒恢复至 RUNNING（无前置状态快照列 + 无与排产协调判定），IDLE 设备经维护后变为 RUNNING。`EquipmentStatusLinker` javadoc:16-17 自承此简化但属 AI 代码层自标。
  - **§4 三判据复核**（P2 项须核 owner doc Deferred 人工批准痕迹）：(i) 无独立 plan-audit 裁决 IDLE 恢复裁剪；(ii) owner doc equipment-integration.md §3.3:141「恢复为 RUNNING 或 IDLE（根据之前状态）」**活跃要求**双分支，state-machine.md §实现约定与 Non-Goal（:156-167）**未列设备状态恢复目标简化为延后项**，`EquipmentStatusLinker` javadoc 自标非 owner doc Deferred 段落无人工批准痕迹（git log 全 AI commits）；(iii) product-scope（:31 maintenance 在范围）未裁剪。**三判据均不成立** → 该简化不可作为「合法 documented simplification」关闭，但影响限于 IDLE 边界场景（RUNNING 是「更可用」态，非数据破坏），定 P2 作为 successor 登记（Q4=(a) 张力声明：P2 登记不强制，但三判据不成立意味不可用方案 B 永久关闭，须择期实现 IDLE 恢复或前置状态快照）。
  - **触及保护区域**：修复 = `EquipmentStatusLinker.restoreToRunning` 增前置状态快照逻辑（或 `ErpMntEquipment` 加 preMaintenanceStatus 快照列）+ complete 时按快照恢复 RUNNING/IDLE。若加 ORM 快照列 → **§5 ORM 类 ask-first**；若纯 BizModel 内存/查询前态恢复逻辑 → 预授权。当前倾向纯逻辑修复（complete 时查询 visit start 前设备状态需快照载体，可能触及 ORM），**择期实现时须裁决修复形态**。
- **计划基线勘误**：本计划 Current Baseline 称「complete 侧是否恢复设备状态至 RUNNING/IDLE（待核 ErpMntVisitCompleteProcessor——若 complete 不恢复则候选缺口）」——**执行时核验澄清**：`ErpMntVisitCompleteProcessor#complete:31` 实际调用 `equipmentStatusLinker.restoreToRunning`（complete 侧设备恢复 RUNNING **已实现**，与独立草案审查 INFO 注记一致）；候选缺口收窄为 **IDLE 分支缺失**（非「complete 不恢复」），定级相应从倾向-P1 降为 **P2**（主路径接受 + IDLE 边界弱）。

#### UC-MAIN-04 = **接受**（出库+余额+凭证+不足校验主路径全满足，跨域 guard 存在）
- **接受部分**：UC-MAIN-04-A（记录备件消耗）/ B（generateConsumptionMove 出库，行为满足方法名差异 §9）/ C（库存余额-=消耗量，跨域 inventory auto-DONE 扣余额）/ D（凭证借维修费用贷存货，config-gated 默认关开启时生成 Dr6602/Cr1403 行级强断言）/ E（备件不足校验失败，跨域 inventory validateAvailable 默认禁负库存）。
- **复用 P1-MA2-074**（备件过账吞异常悬挂维度 resolved R1.16）：UC-MAIN-04-D 过账失败处理（吞异常告警 + 告警派发）经 R1.16 resolved，本切片不重审该维度（§去重协议），仅引用其已修复现状作为过账失败处理行为证据。
- **命名差异（§9 cosmetic，非 finding）**：①方法名 `generateMove` vs L1/L2 `generateConsumptionMove`（接口无此方法，统一走 generateMove(OUTGOING+relatedBillType) 跨域范式）；②移动单类型 `OUTGOING` vs L2「CONSUMPTION 类型」（实现用 OUTGOING，relatedBillType 非空自动 DONE）；③凭证科目命名对齐（Dr 维修费用 6602 / Cr 存货 1403 与 L1 一致）。均为设计期命名 vs 实现命名差异，**行为完全满足 L1 验收标准**，按 §9 真相源冻结条款记入报告不直改真相源。
- **跨域协调声明**：UC-MAIN-04-B/C/E 出库+余额扣减+不足校验依赖 inventory 域 `IErpInvStockMoveBiz.generateMove`（跨域 Facade，A1.25-A1.27 覆盖 inventory 侧行为）。本切片证实 maintenance 侧调用链正确接入跨域 Facade，inventory 侧余额扣减/不足校验行为由其本域审计覆盖（SP-2/SP-3 运行时确认跨域集成路径）。

### P0 即时通道评估
**本切片无 P0**。UC-MAIN-03 IDLE 恢复分支缺失属**状态一致性分歧**（IDLE 设备变 RUNNING，RUNNING 是「更可用」态，非数据破坏——不致库存负数/凭证重复/会计错误）；UC-MAIN-04 出库+过账主路径完整 + config-gated + 幂等（billHeadCode 判重）+ 失败告警（P1-MA2-074 resolved R1.16）；备件不足校验跨域 guard 存在（inventory 默认禁负库存）。三者均非 §2 P0①②③④（活跃数据破坏/安全/核心循环断裂/会计过账正确性破坏）。结论：**不触发 MR0，P2-RC-061 经 successor（P2 登记不强制）**。

---

## 6. 与 arm-index 衔接（复用 or 新增裁决，§7）

执行时 grep arm-index maintenance/mnt/UC-MAIN/visit/spare-part/备件/consumption/equipment status/MAINTENANCE_ISSUE/restoreToRunning：**无 UC-MAIN-03/04 需求符合性 finding**。非 UC 的 maintenance tag 既有 finding（P1-MA2-074 过账吞异常 / P1-MA1-011/013 propId / P1-MA1-022 跨域 daoFor / P1-MA2-086 并发幂等 / A2.14 状态机）均**不同控制点/不同维度**，不构成复用（P1-MA2-074 经复用裁决不重审过账吞异常维度）。

| 本切片 finding | 裁决 | 与既有 finding 差异依据（§7「复用 or 新增」） |
|---------------|------|---------------------------------------------|
| **P2-RC-061** UC-MAIN-03 设备状态恢复目标固定 RUNNING（IDLE 分支缺失） | **新建** | 新根因（设备状态恢复目标简化，无前置状态快照）+ 新控制点（EquipmentStatusLinker.restoreToRunning IDLE 分支）+ 需求符合性维度。P1-MA2-074=过账吞异常维度（resolved R1.16），P1-MA1-011/013=propId 维度，P1-MA1-022=跨域 daoFor 维度，P1-MA2-086=并发幂等维度（resolved R1.28），A2.14=状态机迁移守卫维度（不含恢复目标语义），均不同。 |
| （reuse）P1-MA2-074 maintenance Labor/Issue 过账吞异常悬挂 | **复用**（不重审过账吞异常维度） | §去重协议：resolved R1.16 过账吞异常悬挂维度裁决不重审；本切片仅引用其已修复现状（dispatchFailureAlert 告警派发）作为 UC-MAIN-04-D 过账失败处理行为证据。**不在本切片新增过账吞异常维度 finding**。 |
| （reuse）A2.14 maintenance visit 5 态状态机迁移守卫 | **复用**（不重审状态机迁移维度） | §去重协议：A2.14 已证实 visit 5 态 + request 6 态全迁移守卫 + EquipmentStatusLinker config-gated 机制；本切片不重审状态机迁移维度，只补需求契约视角差异（设备状态联动 start/complete 两侧语义 + IDLE 分支 + 备件出库+凭证+不足校验）。 |

**双向可追溯**：新 finding 已写入下方 §finding 汇总 + arm-index「RC 发现追踪」分区；P2 登记不强制修复，作为 successor 由后续 roadmap 处理（修复行预留，须含 finding ID 交叉引用）。

**触及保护区域标注**：
- P2-RC-061（IDLE 恢复分支）：修复 = `EquipmentStatusLinker.restoreToRunning` 增前置状态恢复逻辑。**若纯 BizModel 内存/查询前态逻辑** → 预授权，不触发 §5 ask-first；**若须 `ErpMntEquipment` 加 preMaintenanceStatus 快照列** → **§5 ORM 类 ask-first + 独立 plan-audit**。择期实现时须裁决修复形态（前置状态快照载体需求）。
- UC-MAIN-04 备件消耗过账凭证科目映射/VoucherFact **已实现**（MaintenanceIssueAcctDocProvider.createFacts），本切片**无新增会计过账变更**。若未来 P2-RC-061 或其他修复触及 MAINTENANCE_ISSUE 凭证科目映射/VoucherFact，须 **ask-first + 独立 plan-audit**（沿用 P1-MA2-074 ask-first 先例）。
- UC-MAIN-04 出库须与 inventory 域 `IErpInvStockMoveBiz` **跨域协调**（出库+余额扣减+不足校验依赖 inventory 域行为，A1.25-A1.27 覆盖）。

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

| SP | 存疑点 | 静态结论 | MA4 展开 |
|----|--------|---------|---------|
| SP-1 | complete 时 IDLE 设备运行时是否变 RUNNING（IDLE 分支缺失确认） | 静态已确认**是缺口**：`restoreToRunning:38-43` 恒恢复 RUNNING，无 IDLE 分支，javadoc:16-17 自承简化。测试均 seed RUNNING 未覆盖 IDLE 输入。归 P2-RC-061。 | 低优先级运行时确认（静态+代码阅读已明确，补 IDLE 输入测试即可证伪/证实） |
| SP-2 | `generateMove(OUTGOING+relatedBillType)` 运行时是否真实触发库存余额扣减 | 静态已澄清**机制存在**：inventory `ErpInvStockMoveProcessor` OUTGOING+relatedBillType 非空 → 自动 complete → `bookkeeper.bookCompletion` 更新 ErpInvStockBalance。maintenance 测试在 move 层断言 DONE（间接），未直查余额。 | 运行时确认：confirm 后直查 `ErpInvStockBalance.totalQuantity` == seed-消耗量（跨域 inventory 行为，A1.25-A1.27 覆盖） |
| SP-3 | 备件不足校验失败路径运行时是否抛 inventory ERR | 静态已澄清**guard 存在**：inventory `validateAvailable:128` available<required 抛 ERR_AVAILABLE_INSUFFICIENT，`isNegativeStockAllowed` 默认 FALSE。maintenance slice 未测不足路径（跨域 guard）。 | 运行时确认：seed 不足库存（如库存 5<消耗 10）→ confirm → 断言抛 ERR_AVAILABLE_INSUFFICIENT（跨域集成路径） |

> 本切片 L5 存疑点经静态分析多数已澄清（SP-1 确认缺口归 P2-RC-061 / SP-2/SP-3 机制存在留运行时确认跨域集成路径）。仅 SP-2/SP-3 留运行时确认。

**P0 即时通道评估**（本切片无活跃数据破坏候选）：
- 备件过账悬挂经 P1-MA2-074 已 resolved R1.16（吞异常 + 告警派发闭环）。
- UC-MAIN-03 设备状态联动 IDLE 分支缺失属状态一致性分歧（RUNNING 是「更可用」态，非数据破坏——不致库存负数/凭证重复/会计错误；设备维度经乐观锁 @Version 保护并发覆盖）。
- 结论：**本切片无 P0**，不触发 MR0。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（2026-08-05 实测）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter，真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码作为门控通过依据。**本审计为只读审计，零生产代码变更，checker 无回归风险**。本次实测 checker 在 R3（`new Erp*()` 规则）处输出截断退出（exit=1，预存工具态，非本审计引入——本审计零 Java 变更）；R1/R2 actual 实测如下，R3+ 引用同日兄弟切片 A1.42 记录值（零代码变更故不变）：

  | 规则 | Baseline（compliance-baseline.md） | Actual（2026-08-05 实测） | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 240 | 229 | ✅（actual<baseline） |
  | R2c | 1380 | 1382 | ⚠️ actual 微超 baseline（+2，预存态，非本审计引入——零代码变更；baseline 文件值与同日 A1.42 记录值{R2b=229/R2c=1382/R2d=34}不一致，疑 baseline 文件未同步 R6.8 后实际值，建议择期重同步 baseline） |
  | R2d | 32 | 34 | ⚠️ actual 微超 baseline（+2，同上预存态） |
  | R3 | 5 | 5（A1.42 同日记录，本审计零变更） | ✅ |
  | R4/R5 | 0/0 | 0/0 | ✅ |
  | R6 | 2 | 2 | ✅ |
  | R7 | 0 | 0 | ✅ |
  | R8 | 0 | 0 | ✅ |
  | R10 | 6 | 6 | ✅ |
  | R11 | 0 | 0 | ✅ |
  | R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

  > **基线漂移注记**：`compliance-baseline.md` 当前 R2b=240/R2c=1380/R2d=32 与同日兄弟切片 A1.42 记录的 actual=baseline{229/1382/34}不一致，疑 baseline 文件未同步近期实际值（本审计零代码变更，故 actual 沿用 A1.42 同日实测）。此为预存 baseline 同步问题，**非本审计引入**，建议择期独立校准 baseline 文件。本审计无生产代码变更，checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P2-RC-061）已按 §7 规则 grep arm-index 同域同控制点后给出「复用 or 新增」裁决（见 §6 裁决表），无未经比对直接新建的 finding。P1-MA2-074 经复用裁决（不重审过账吞异常维度），A2.14 经复用裁决（不重审状态机迁移维度）。

---

## 9. 与 MA2 报告差异增量声明（§去重协议）

- **无 maintenance 专属 MA2 报告**：`docs/audits/` 下 MA2 报告（`2026-07-2*-arm-ma2-*`）覆盖 finance/inventory/mfg/hr/purchase/sales/assets/quality/projects/crm/cs/contract/b2b/aps/logistics，**零 maintenance 访问/备件行为证据**。A2.14（`2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）覆盖 maintenance visit/request 状态机迁移 + EquipmentStatusLinker config-gated + DowntimeEntry，**不含 UC-MAIN-03 设备状态联动 start/complete 两侧语义 + UC-MAIN-04 备件出库+凭证+不足校验需求契约维度**。**本切片为 maintenance 访问+备件需求契约符合性的首份证据**（与 A1.42 并列 maintenance 域 RC 切片）。
- **P1-MA2-074 引作过账吞异常悬挂已 resolved 证据**（不重审过账吞异常维度）：`MaintenanceIssuePostingDispatcher.dispatchIfApplicable` 过账失败吞异常悬挂经 R1.16 resolved（失败告警派发 `mnt.spare-part-posting-failure`）作为 UC-MAIN-04-D 过账失败处理行为证据复用，本切片**不重审过账吞异常悬挂维度**。
- **A2.14 引作状态机迁移守卫已证实证据**（不重审状态机迁移维度）：visit 5 态全迁移守卫 + EquipmentStatusLinker config-gated 机制经 A2.14 证实，本切片**不重审状态机迁移维度**。
- **只补需求视角差异**：本切片从需求契约（use-cases.md UC-MAIN-03/04）视角补既有 MA2 未覆盖的差异——①UC-MAIN-03 complete 侧设备状态恢复 RUNNING **已实现**（纠正计划基线「待核」假设，与草案审查 INFO 一致），候选缺口收窄为 **IDLE 恢复分支缺失**（P2-RC-061）；②UC-MAIN-04 出库+余额+凭证+不足校验主路径**全满足**（命名差异 cosmetic 记入下条）；③跨域 inventory 余额扣减/不足校验集成路径登记 SP-2/SP-3 运行时确认。
- **命名差异（cosmetic，记入报告不直改真相源）**：①设备状态 dict 值 `UNDER_MAINTENANCE`（`app-erp-maintenance.orm.xml:56`）vs L1/L2 文本 `MAINTENANCE`（use-cases.md:11/55/62 + equipment-integration.md §3.1:112）——语义一致均表「维护中」，命名差异；②备件出库方法 `IErpInvStockMoveBiz.generateMove(OUTGOING)` vs L1/L2 文本 `generateConsumptionMove`（use-cases.md:78 + equipment-integration.md §2.2:64）——接口无 generateConsumptionMove，统一走 generateMove(OUTGOING+relatedBillType) 跨域范式，行为等价；③移动单类型 `OUTGOING` vs L2「CONSUMPTION 类型」（equipment-integration.md §2.2:66）——实现用 OUTGOING，relatedBillType 非空自动 DONE 扣余额。三者均**行为完全满足 L1 验收标准**，按 §9 真相源冻结条款记入报告不直改真相源（L1/L2 命名修订须经人工批准）。
- **§9 真相源冻结条款遵守**：本审计未修改任何真相源（product-scope / use-cases / owner doc 需求契约段落）。发现的 doc↔impl 命名差异（UNDER_MAINTENANCE/MAINTENANCE、generateMove/generateConsumptionMove、OUTGOING/CONSUMPTION）+ 计划基线对 complete 侧「待核」假设的澄清，均记入本报告，不直改真相源。

---

## 报告 9 段完整性自检

- [x] §1 需求契约原文（UC-MAIN-03/04 逐字引用 + 逐条验收标准拆解）
- [x] §2 实现证据（L3 file#method + 行为断言 + 跨域 IErpInvStockMoveBiz 调用链 + 三源对照表）
- [x] §3 测试证据（L4 + 断言强度 + 测试运行结果）
- [x] §4 运行时行为证据（L5 + MA2/A2.14 复用 + 存疑点交接）
- [x] §5 符合性结论（五级矩阵逐验收标准 + 每 UC P0/P1/P2/接受 + §4 三判据复核 + 触及保护区域标注 + P0 评估）
- [x] §6 与 arm-index 衔接（复用/新增裁决表 + 双向可追溯 + ORM/会计 ask-first 标注）
- [x] §7 静态存疑点清单（SP-1~SP-3 + P0 评估）
- [x] §8 过程纪律自检（checker actual vs baseline 表 + 基线漂移注记 + 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（首份证据 + 不重审过账吞异常/状态机迁移维度 + 只补需求视角差异 + 命名差异 cosmetic + 真相源冻结遵守）

## finding 汇总

| Finding ID | UC | 分级 | 判据 | 触及保护区域 | 目标 MR |
|-----------|-----|------|------|-------------|--------|
| P2-RC-061 | UC-MAIN-03 C-IDLE 分支 | P2 | §2 P2①（次要验收标准未完全满足——主路径 RUNNING OK，IDLE 边界弱） | 视修复形态：纯 BizModel 逻辑预授权 / **若加 ErpMntEquipment preMaintenanceStatus 快照列则 ORM ask-first** | successor（P2 登记不强制） |
