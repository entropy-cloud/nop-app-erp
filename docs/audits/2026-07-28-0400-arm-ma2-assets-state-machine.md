# ARM-MA2 assets 状态机系统业务审查报告（A2.10）

> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> Roadmap 工作项：A2.10（A 级单域，18 状态字段）
> Plan：`docs/plans/2026-07-28-0400-2-audit-remediation-ma2-assets-state-machine.md`
> 行为基线：`docs/design/assets/{state-machine,depreciation-and-posting,split-merge,cip,inventory}.md` + `docs/architecture/{processor-extension-pattern,posting-exemptions}.md`
> Skill：`docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
> 实仓快照：2026-07-28（HEAD 经 `mvn install -DskipTests -pl module-assets/erp-ast-service -am` BUILD SUCCESS + `mvn test -pl module-assets/erp-ast-service` 90/90 全绿验证一致）
> 裁决：**Verdict = ⚠️(P1)**——资产状态机核心契约经证据确认（资产卡片生命周期迁移经资本化/处置/拆分/合并 Processor 副作用齐全、7 业务单据 6 PROC + 1 INLINE 模式、@BizMutation 事务回滚、reverseApprove 红冲闭环经大 Processor 路径强一致 [Capitalization/Disposal/ValueAdjustment →REJECTED 合规]、跨域写经 I*Biz Facade）；零 P0；**新增 4 项 P1**（P1-MA2-058 Movement reverseApprove→SUBMITTED INLINE 违反 owner doc §2 强制 REJECTED 契约漂移 / P1-MA2-059 Movement 全 5 INLINE 动作缺 isCancelled 守卫致 CANCELLED 单据 approveStatus 副轴漂移 / P1-MA2-060 Capitalization/Disposal tryPost 吞异常悬挂 + reverseApprove 仅 posted=true 时回滚资产状态致资产侧状态悬挂[reverseApprove 在 posted=false 窗口期不回滚资产] / P1-MA2-061 ErpAstAsset IDLE 状态机迁移完全未实现[IN_SERVICE↔IDLE 无任何 writer]+ 联动缺失[折旧引擎只查 IN_SERVICE/闲置超期提醒无 job]）；**新增 3 项 P2** watch-only（P2-MA2-059 state-machine.md 缺 7 业务单据独立章节 / P2-MA2-060 PROC vs INLINE 模式 owner doc 未声明 / P2-MA2-061 6 业务单据 Processor cancel 方法死代码未接线）；**5 项已登记 MA1 finding 运行时复核全部澄清——DISPOSED/CANCELLED 经证伪非死状态**（P1-MA1-008 propId 无状态机影响 / P1-MA1-016 finance→assets 跨域 daoFor 只读，状态写经 I*Biz / P1-MA1-022 跨域只读无升级 / P2-MA1-023 DISPOSED 经 Split/Merge **可达**，仅 owner doc drift / P2-MA1-024 CANCELLED 经 Capitalization.reverseApprove + Disposal.approve **可达**，仅 owner doc drift）；并发敏感点 5 处交接 A2.17。

---

## 1. 范围与基线

### 1.1 在范围

资产域（`module-assets/model/app-erp-assets.orm.xml`，18 状态字段分布于两类状态对象 × 多组件）：

| 实体 | 状态轴 | posted | 备注 |
|------|--------|--------|------|
| `ErpAstAsset` | status(erp-ast/asset-status) 6 态 | —（资产卡片无 posted 列，由资本化单承载） | 资产卡片生命周期轴；迁移经资本化/处置/拆分/合并/盘点 Processor 副作用 |
| `ErpAstDepreciationSchedule` | status(erp-ast/depreciation-schedule-status) 4 态 | ✅ | 折旧计划条目轴；PENDING/EXECUTED/REVERSED/CANCELLED 全态可达 |
| `ErpAstMovement` | docStatus(erp/doc-status) + approveStatus(wf/approve-status) 双轴 | —（不过账） | **唯一 INLINE 实体**——5 动作全 INLINE xbiz 脚本；无 Processor |
| `ErpAstValueAdjustment` | docStatus + approveStatus + adjustmentType(erp-ast/adjustment-type) | ✅ | 价值调整/重估；PROC 全守卫；reverse→REJECTED 合规 |
| `ErpAstDisposal` | docStatus + approveStatus + disposalType(erp-ast/disposal-type) + reason(erp-ast/disposal-reason) | ✅ | useWorkflow=true（xwf 浏览器层不可达，DIRECT 三轴可达——owner doc 已知限制） |
| `ErpAstAssetCapitalization` | docStatus + approveStatus + sourceType(erp-ast/capitalization-source-type) | ✅ | PROC 全守卫；reverse→REJECTED 合规 |
| `ErpAstSplit` | docStatus + approveStatus + allocationMethod(erp-ast/allocation-method) | ✅ | 拆分；PROC 全守卫；reverseApprove 抛 ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED（不可逆契约） |
| `ErpAstMerge` | docStatus + approveStatus | ✅ | 合并；PROC 全守卫；reverseApprove 抛 ERR_AST_MERGE_REVERSE_NOT_SUPPORTED（不可逆契约） |
| `ErpAstCip` | status(erp-ast/cip-status) | — | CIP 单轴（DRAFT/IN_CONSTRUCTION/TRANSFERRED）；transferred 经 IErpAstAssetCapitalizationBiz 链 |
| `ErpAstInventory` | status(erp-ast/inventory-status) | ✅ | 资产盘点单头（5 态）；差异处置在 RECONCILING 态进行；reverse 红冲回 RECONCILING |
| `ErpAstInventoryLine` | varianceType + disposition | — | 盘点行差异轴（不迁移头状态） |
| `ErpAstMaintenance` | status(erp-ast/maintenance-status) + treatment(erp-ast/maintenance-treatment) | ✅ | 维修工单（6 态）；POSTED 终态经 post 触发 |

### 1.2 不在范围（Non-Goals 见 plan）

- A4.3 assets 折旧引擎与 Processor 链路专属审计（48 Processor 全域最高密度归 A4.3；本审计只做状态机业务正确性）
- A2.3 期末结账端到端 GL 正确性（done）
- A2.5 finance 期间/预算状态机（done）
- A4.6/A4.7 view.xml drift
- config-gated Deferred 偏离本身（IDLE 停提配置 / 多级审批链 / 闲置超期提醒 job）
- A2.17 并发与乐观锁（并发折旧同一资产）

---

## 2. 状态图与转换矩阵

### 2.1 资产卡片生命周期状态图（`ErpAstAsset.status`）

```
草稿 (DRAFT)
  └─ 资本化 approve → 使用中 (IN_SERVICE) [ErpAstAssetCapitalizationProcessor.createAndActivateAsset:206]
                     ├─ 暂停使用 → 闲置 (IDLE) ⚠️⚠️ 无任何 writer [P1-MA2-061]
                     │              └─ 恢复使用 → 使用中 (IN_SERVICE) ⚠️⚠️ 无任何 writer
                     ├─ 报废处置 approve → 已报废 (SCRAPPED) [ErpAstDisposalProcessor.approve:74-76]
                     ├─ 出售处置 approve → 已出售 (SOLD)    [ErpAstDisposalProcessor.approve:74-76]
                     ├─ 拆分 approve → 已内部处置 (DISPOSED) [ErpAstSplitProcessor.disposeSourceAsset:449]
                     └─ 合并 approve → 已内部处置 (DISPOSED) [ErpAstMergeProcessor:389]

资本化 reverseApprove (if posted) → DRAFT [ErpAstAssetCapitalizationProcessor.reverseApprove:107]
处置 reverseApprove (if posted) → IN_SERVICE [ErpAstDisposalProcessor.reverseApprove:122]
盘点盘盈 → 新建 IN_SERVICE [ErpAstInventoryProcessor:317]
盘点盘亏 → 账面 SCRAPPED [ErpAstInventoryProcessor:350]
```

**裁决**：
- DRAFT/IN_SERVICE/SCRAPPED/SOLD/DISPOSED 5 态**经证据确认可达**（5 writer 点齐全）
- **IDLE 经证据确认无任何 writer**——grep 全 `module-assets/erp-ast-service/src/main` `setStatus(ASSET_STATUS_IDLE)` 零匹配；IDLE 常量仅在 `ErpAstInventoryProcessor:193`（盘点范围查询过滤）+ `ErpAstDisposalProcessor:200` + `ErpAstValueAdjustmentProcessor:204`（处置/调整守卫读取）三处作**只读引用**——IDLE 是**事实上的死状态**（dict 项 + owner doc §1/§2 声明迁移但代码完全未实现）。**P1-MA2-061**。
- 终态 SCRAPPED/SOLD/DISPOSED 经证据确认**无出边**——无任何 setStatus 从这三态迁出。**owner doc §3 终态不可恢复约束落实 ✓**。
- 处置错误的纠正路径：经 `reverseApprove`（仅 posted=true 时回滚资产至 IN_SERVICE）+ 重新处置。**但 posted=false 窗口期 reverseApprove 不回滚资产——P1-MA2-060**。

### 2.2 折旧计划条目状态机（`ErpAstDepreciationSchedule.status`）

```
PENDING ─executeDepreciation─► EXECUTED ─reverseDepreciation─► REVERSED
   ▲                               │
   │                               │
   ├─ capitalization.reverseApprove (if posted) ──┐
   ├─ disposal.approve (cancelPendingSchedules) ──┤
   └─ capitalization.approve / split / merge ─────┘ CANCELLED
                                                   (写者齐全)
```

**裁决**：PENDING/EXECUTED/REVERSED/CANCELLED 4 态**全部经证据确认可达**——`P2-MA1-024 折旧计划 CANCELLED 死状态` 假设**经证伪不成立**（CANCELLED 经 `ErpAstAssetCapitalizationProcessor.cancelSchedules:291` + `ErpAstDisposalProcessor.cancelPendingSchedules:214` 写入，是合法的"折旧因资产资本化冲销/处置而作废"语义）。owner doc §折旧计划条目状态 列 3 态缺 CANCELLED 是 owner doc drift（P2-MA1-024 维持）。

### 2.3 7 业务单据审批轴 × 实现模式矩阵

> 实现路径列：**PROC** = 经大 Processor 全守卫（validateNotCancelled/validateTransition*/validateBusinessRules* + doApprove/doReject/doReverseApprove/doCancel 四动作齐全）；**INLINE** = xbiz 脚本直设 approveStatus，仅校验 src 状态；**THROW** = 不允许 reverse（不可逆契约）

| 实体 | submitForApproval | approve | reject | reverseApprove | withdrawApproval | cancel |
|------|------|--------|--------|----------------|------------------|--------|
| Movement | **INLINE**→SUBMITTED | **INLINE**→APPROVED | **INLINE**→REJECTED | **INLINE**→**SUBMITTED** ⚠️⚠️ [P1-MA2-058] | **INLINE**→UNSUBMITTED | 无 cancel 暴露 |
| ValueAdjustment | PROC→SUBMITTED | PROC→APPROVED + tryPost | PROC→REJECTED | PROC→**REJECTED** ✓ + posted=false + 凭证 reverse | PROC→UNSUBMITTED | Processor.cancel 存在但 xbiz 未暴露 |
| Disposal | PROC+xwf→SUBMITTED | PROC→APPROVED + 资产 SCRAPPED/SOLD + tryPost | PROC→REJECTED | PROC→**REJECTED** ✓ + (if posted) 资产回 IN_SERVICE ⚠️ [P1-MA2-060] | PROC→UNSUBMITTED | Processor.cancel 存在但 xbiz 未暴露 |
| Capitalization | PROC→SUBMITTED | PROC→APPROVED + 建卡 IN_SERVICE + 折旧计划 + tryPost | PROC→REJECTED | PROC→**REJECTED** ✓ + (if posted) 资产回 DRAFT + cancelSchedules ⚠️ [P1-MA2-060] | PROC→UNSUBMITTED | Processor.cancel 存在但 xbiz 未暴露 |
| Split | PROC→SUBMITTED | PROC→APPROVED + 源 DISPOSED + N 新卡 + tryPost | PROC→REJECTED | **THROW** ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED ✓（不可逆契约） | PROC→UNSUBMITTED | Processor.cancel 存在但 xbiz 未暴露 |
| Merge | PROC→SUBMITTED | PROC→APPROVED + 源 DISPOSED + 1 新卡 + tryPost | PROC→REJECTED | **THROW** ERR_AST_MERGE_REVERSE_NOT_SUPPORTED ✓（不可逆契约） | PROC→UNSUBMITTED | Processor.cancel 存在但 xbiz 未暴露 |

**裁决**：
- 6 PROC 实体的 submitForApproval/approve/reject/withdrawApproval 经 `validateNotCancelled`（docStatus != CANCELLED）+ `validateTransition*`（src 状态匹配）+ 业务规则校验三段守卫齐全 ✓
- 6 PROC 实体的 reverseApprove：4 个合规（Capitalization/Disposal/ValueAdjustment →REJECTED ✓ + Split/Merge THROW 不可逆契约 ✓）；**0 个违反 owner doc §2 强制 REJECTED**
- **Movement 是唯一全 INLINE 实体**，reverseApprove xbiz 直设 SUBMITTED（`ErpAstMovement.xbiz:85-99` 反编译源），违反 owner doc §2 + `domain-design-guidelines.md §16.4` 强制 REJECTED 规则。**P1-MA2-058**（与 purchase P1-MA2-049 / sales P1-MA2-056 同型裁决）
- **Movement 全 5 INLINE 动作**缺 `validateNotCancelled`（无 docStatus 守卫）——但 Movement 无 cancel 暴露，CANCELLED docStatus 不可经服务层达，**实际危害进一步收窄**（仅"已逻辑删除"Movement 仍可经 INLINE 迁移 approveStatus）。**P1-MA2-059**（与 purchase P1-MA2-050 / sales P1-MA2-057 同型）
- 6 业务单据 Processor 均含 `cancel` 方法（如 `ErpAstSplitProcessor.cancel` + `ErpAstMergeProcessor.cancel` + `ErpAstValueAdjustmentProcessor.cancel` + `ErpAstAssetCapitalizationProcessor.validateTransitionForCancel`），但 xbiz **未暴露 cancel mutation**——docStatus=CANCELLED 经服务层不可达。**死代码 P2-MA2-061**（与 purchase P2-MA2-054 同型）。CANCELLED 经 `useLogicalDelete=true`（delVersion）承载废弃语义。

### 2.4 CIP / Inventory / Maintenance 三状态机

| 实体 | 主链 | 终态 | 反向 | 守卫 |
|------|------|------|------|------|
| `ErpAstCip` | DRAFT→startConstruction→IN_CONSTRUCTION→transferToAsset→TRANSFERRED | TRANSFERRED | reverseTransfer (TRANSFERRED→IN_CONSTRUCTION) | 守卫齐全 |
| `ErpAstInventory` | DRAFT→submitForCount→COUNTING→reconcile→RECONCILING→post→POSTED | POSTED + CANCELLED | reverse (POSTED→RECONCILING，红字凭证) | 守卫齐全（范围非空/行防重/差异冻结/过账门控） |
| `ErpAstMaintenance` | DRAFT→submit→SUBMITTED→startWork→IN_PROGRESS→completeWork→COMPLETED→post→POSTED | POSTED + CANCELLED | reverse (POSTED→COMPLETED，红字凭证) | 守卫齐全（资产非终态） |

**裁决**：三状态机主链迁移守卫齐全，POSTED 经 reverse 红字凭证回退（非真终态可再 post），CANCELLED 经 cancel 终态。**PASS**。

---

## 3. 10 维度审查裁决

> 维度编号对齐 `state-machine-business-review-prompt.md`。

### 维度 1：状态定义（裁决：**FAIL**——一处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 资产卡片 status 5 态 vs dict 6 态（DISPOSED） | ⚠️ | owner doc §1 列 5 态，dict 含 6 态（DISPOSED 由 split-merge 引入）。兄弟 owner doc `split-merge.md:103-104` 已文档化 DISPOSED 语义；`state-machine.md §1` 漏更新。DISPOSED **经证据确认可达**（Split/Merge Processor 写入），**非死状态**——P2-MA1-023 维持（owner doc drift） |
| 折旧计划 status 3 态 vs dict 4 态（CANCELLED） | ⚠️ | owner doc §折旧计划条目状态列 3 态，dict 含 4 态。CANCELLED **经证据确认可达**（`ErpAstAssetCapitalizationProcessor.cancelSchedules:291` + `ErpAstDisposalProcessor.cancelPendingSchedules:214`），**非死状态**——P2-MA1-024 维持（owner doc drift） |
| **IDLE 折旧停提/恢复配置落实** | ❌ FAIL | owner doc §1「闲置（IDLE）—可配（默认停提）」+ §2「IN_SERVICE→IDLE / IDLE→IN_SERVICE」迁移表声明；实现：(1) `ASSET_STATUS_IDLE` 常量**零 writer**（grep 全 src/main 零 setStatus 匹配）；(2) 折旧引擎 `ErpAstDepreciationScheduleProcessor:138` 仅查询 IN_SERVICE，IDLE 不参与折旧批量；(3) 无 suspend/resume/setIdle BizMutation 方法；(4) IDLE 仅作 3 处只读引用（盘点范围过滤 + 处置/调整守卫）。**P1-MA2-061**（按 hr P1-MA2-039 + mfg P1-MA2-035 同型裁决——状态 dict 项 + owner doc 声明迁移但代码完全未实现，不破坏主路径 IN_SERVICE 折旧闭环） |
| 处置终态 SCRAPPED/SOLD 与 DISPOSED 关系 | PASS | 三态经证据确认均无出边（终态不可恢复约束 ✓）。语义边界清晰：SCRAPPED/SOLD=对外处置（有损益），DISPOSED=内部结构重组无损（拆分/合并） |
| CIP/盘点/维修状态轴清晰性 | PASS | CIP 3 态 + Inventory 5 态 + Maintenance 6 态各自 dict 绑定清晰，无重叠语义 |

### 维度 2：转换完整性（裁决：**FAIL**——两处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 资产卡片生命周期迁移完整性（DRAFT→IN_SERVICE→IDLE/SCRAPPED/SOLD） | ❌ FAIL | IDLE 入边/出边**完全未实现**（同维度 1 P1-MA2-061）。其余 4 个迁移点（DRAFT→IN_SERVICE 经资本化 / IN_SERVICE/IDLE→SCRAPPED|SOLD 经处置 / IN_SERVICE→DISPOSED 经拆分/合并）齐全 ✓ |
| 业务单据 Processor reverseApprove 目标态合规性（→REJECTED 统一） | ❌ FAIL | 6 PROC 实体合规（Capitalization/Disposal/ValueAdjustment →REJECTED + Split/Merge THROW 不可逆契约）；**Movement INLINE →SUBMITTED** 违反 owner doc §2。**P1-MA2-058** |
| INLINE 动作守卫（vs PROC） | ❌ FAIL | Movement 全 5 INLINE 动作仅校验 approveStatus src，缺 `validateNotCancelled`（无 docStatus 守卫）。**P1-MA2-059** |
| 终态不可恢复约束（SCRAPPED/SOLD 无出边） | PASS | grep 全 src/main 无任何 setStatus 从 SCRAPPED/SOLD/DISPOSED 迁出。owner doc §3 落实 ✓ |
| 拆分/合并特殊迁移（IN_SERVICE→DISPOSED + 新卡 DRAFT/IN_SERVICE） | PASS | `ErpAstSplitProcessor:449` + `ErpAstMergeProcessor:389` 设源 DISPOSED + 新资产 IN_SERVICE（非 DRAFT，直接进入折旧——`createTargetAssets:381`）；`split-merge.md §实现注记` 已文档化 |
| 资本化/处置前置（资产状态守卫） | PASS | `ErpAstDisposalProcessor.validateAssetDisposable:190-204` 拒绝已处置 + 仅允许 IN_SERVICE/IDLE；`ErpAstAssetCapitalizationProcessor.validateForApproval:168-185` 校验类别/原值/年限；`ErpAstSplitProcessor.validateSourceAsset:204-221` 仅允许 IN_SERVICE + 净值 > 0 |

### 维度 3：终端状态和恢复（裁决：**FAIL**——一处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| SCRAPPED/SOLD/DISPOSED 终态不可恢复 | PASS | 无出边（见维度 2） |
| **处置错误经"处置冲销"反向清理凭证路径**（owner doc §3） | ❌ FAIL | `ErpAstDisposalProcessor.reverseApprove:118` 仅在 `disposal.posted==true` 时回滚资产 IN_SERVICE + restoreCancelledSchedules；**posted=false 窗口期**（资本化/处置过账失败的悬挂态）reverseApprove 仅设 disposal=REJECTED，**资产保持 SCRAPPED/SOLD 终态 + schedules 保持 CANCELLED**——无恢复路径。**P1-MA2-060**（与 finance P1-MA2-032 / hr P1-MA2-048 同型，但资产侧更严重——终态不可逆 + reverseApprove 不对称） |
| 资本化 reverseApprove 资产回 DRAFT | ⚠️ | `ErpAstAssetCapitalizationProcessor.reverseApprove:103-115` 仅在 posted=true 时回滚资产 DRAFT + cancelSchedules；**posted=false 窗口期** reverseApprove 资产保持 IN_SERVICE + schedules 保持 PENDING——后续折旧会继续生成 DEPRECIATION 凭证，但 GL 缺 CAPITALIZATION 入账分录（GL 与资产注册表不一致）。同 P1-MA2-060 |
| DRAFT 可物理删除（未入账） | PASS | `ErpAstAsset` 配 `useLogicalDelete=true deleteFlagProp="delVersion"`，资产 DRAFT 与 IN_SERVICE 同走逻辑删除（无资本化单的 DRAFT 资产物理影响为零） |
| 折旧计划 REVERSED 终态（红字凭证） | PASS | `ErpAstDepreciationScheduleProcessor.reverseDepreciation:154-177` 守卫 EXECUTED 前置 + 回滚资产累计折旧/净值 + 设 REVERSED + posted=false |
| POSTED 经 reverse 红字凭证恢复（Inventory/Maintenance 非真终态） | PASS | `ErpAstInventoryProcessor.reverse:163` POSTED→RECONCILING + 红字凭证；`ErpAstMaintenanceProcessor.reverse` POSTED→COMPLETED + 红字凭证 |

### 维度 4：异常路径（裁决：**FAIL**——一处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 折旧计提时已结账（拒绝） | PASS | `ErpAstDepreciationScheduleProcessor.requirePeriodOpen:265-276` 守卫 `ERR_DEPRECIATION_PERIOD_CLOSED` 拒绝非 OPEN 期间 |
| 折旧后账面净值低于残值（直线法预计算/其他校验截断） | PASS | `DepreciationCalculator.calculate` 直线法预计算残值；折旧计划 last 期调整（`ErpAstAssetCapitalizationProcessor.plannedAmount:251-253`）保证不破残值约束 |
| 处置时累计折旧与原值不符（拒绝） | PASS | `ErpAstDisposalProcessor.approve:66-70` 计算账面净值 `nbv=original-accumDep`——若累计折旧异常（accumDep > original）则 nbv 负数，gainLoss 异常。**未显式拒绝 accumDep > original**——但 gainLoss 计算后会传给过账引擎，引擎层科目平衡校验兜底。属隐式守卫，未升 P1 |
| **资本化凭证生成失败资产悬挂** | ❌ FAIL | `CapitalizationPostingDispatcher.tryPost:45-58` try/catch 吞所有异常返回 false + LOG.warn/error；`ErpAstAssetCapitalizationProcessor.approve:74-85` 失败时 posted=false 但 cap=APPROVED+ACTIVE，**资产已建 IN_SERVICE + 折旧计划 PENDING**。后续 reverseApprove 仅 posted=true 时回滚资产（同维度 3 P1-MA2-060）。javadoc 标注「由 DeferredPostingSweepJob (app.erp.fin.service.job) 兜底扫描重试」——`ErpFinDeferredPostingRetryHelper` 存在于 finance 域，但 reverseApprove 在 sweep 兜底前触发会留悬挂数据 |
| **处置凭证生成失败资产悬挂（终态侧）** | ❌ FAIL | `DisposalPostingDispatcher.tryPost:39-51` 同型吞异常返回 null + LOG；`ErpAstDisposalProcessor.approve:90-100` 失败时 posted=false 但 disposal=APPROVED + 资产 SCRAPPED/SOLD（终态）。reverseApprove 仅 posted=true 时回滚。同 P1-MA2-060 |
| 科目映射缺失（报错） | PASS | `DisposalPostingDispatcher.resolveSubjectCode:105-115` + `CapitalizationPostingDispatcher.resolveSubjectCode:114-124` 兜底默认 1601/1602/1603/6711/1002，避免直接报错；若类别 subjectId 引用不存在的 subject，回落默认 |
| **期间结账后折旧漏提（反结账补提 vs 当期补提）** | PASS | owner doc §4/§9 场景 D 描述两条路径：(a) 反结账 6 月→补提→重新结账；(b) 7 月补提（补提凭证注明归属期间）。实现：`ErpAstDepreciationScheduleProcessor.executeDepreciation:52-130` 接受任意 period 参数（无前置必须等于当前期间校验），用户可在任何 OPEN 期间触发折旧——两条路径技术上可达 ✓ |
| 并发折旧同一资产（乐观锁） | ⚠️ | `ErpAstDepreciationScheduleProcessor.executeDepreciation` 无悲观/乐观锁，并发触发同一 assetId+period 折旧可双写 schedule。**ErpAstDepreciationSchedule 声明 versionProp（透明乐观锁）→ silent lost-update → detectable conflict**——已降级。归 A2.17 |
| 重复折旧幂等 | PASS | `executeDepreciation:65-72` 若 wasExecuted + posted 则先 reverse 旧凭证再重算；幂等键 (assetId, period) 经 findSchedule 自然去重 |

### 维度 5：可达性（裁决：**FAIL**——一处 P1）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **DISPOSED dict 项可达性**（P2-MA1-023） | ✅ PASS | **经证伪非死状态**——Split Processor `disposeSourceAsset:449` + Merge Processor `:389` 写入；DISPOSED 是合法的"内部结构重组处置"语义 |
| **折旧计划 CANCELLED 可达性**（P2-MA1-024） | ✅ PASS | **经证伪非死状态**——`ErpAstAssetCapitalizationProcessor.cancelSchedules:291` + `ErpAstDisposalProcessor.cancelPendingSchedules:214` 写入 |
| **IDLE 可达性** | ❌ FAIL | dict 项存在但零 writer——**死状态**。**P1-MA2-061** |
| 从 DRAFT 可达 IN_SERVICE→SCRAPPED/SOLD/DISPOSED 全态 | PASS | 资本化→处置/拆分/合并全链迁移可达 |
| IDLE↔IN_SERVICE 合法往复 | ❌ FAIL | 同 P1-MA2-061——往复迁移完全未实现 |
| 无不可达状态/死锁/无限循环 | PASS | 折旧计划 PENDING→EXECUTED→REVERSED 合法循环（reverseDepreciation 后可再 executeDepreciation 重算）+ 退出条件是资产处置到终态 |

### 维度 6：角色和权限（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 资本化/暂停/恢复（资产管理员） | PASS | `ErpAstAssetCapitalizationProcessor` 各 @BizMutation 经 nop-auth 权限模型绑定资产管理员角色（不在本审计范围——A4.x 平台合规已覆盖） |
| 报废/出售处置（资产管理员+审批） | PASS | Disposal useWorkflow=true（xwf 三轴）+ DIRECT 三轴审批浏览器层可达（owner doc §已知限制 已声明 xwf 浏览器层不可达，DIRECT 替代路径） |
| 折旧执行（财务员/系统） | PASS | `ErpAstDepreciationScheduleProcessor.executeDepreciation/executeBatchDepreciation` 经 @BizMutation 入口绑定财务员角色 |
| **期末批量折旧高影响**（owner doc §6 危险操作） | PASS | `executeBatchDepreciation:132-152` 经 @BizMutation 入口 + 财务员权限 + 单失败隔离（不影响他资产）。无 config-gate，但 @BizMutation 权限门控已生效 |
| 多角色冲突 | PASS | 资产管理员（资本化/暂停/恢复）vs 财务员（折旧/过账）职责分离经 @BizMutation 入口 + 权限模型保证（不在本审计范围） |

### 维度 7：外部依赖（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| 折旧/处置/资本化凭证生成（IErpFinAcctDocProvider 聚合→IErpFinVoucherBiz 跨域写会计保护区域） | PASS | 9 个 posting dispatcher（Depreciation/Capitalization/Disposal/ValueAdjustment/AssetSplit/AssetMerge/AssetInventory/MaintenanceCapitalization/MaintenanceExpense）经 `AssetPostingExecutor` → `IErpFinVoucherBiz.post()` REQUIRES_NEW Facade——经 I*Biz Facade，无 daoFor(ErpFin*) 直写 |
| 期末批量折旧（财务域期末结账触发/资产域定时任务） | PASS | `ErpAstDepreciationJob` + `app-service.beans.xml` + `scheduler.yaml` 三件套已接线（`erp-ast.depreciation-cron` 默认空=跳过门控，cron 配置键默认空=跳过门控，运维启用配置键即按设计 cronExpr 自动执行） |
| 资本化库存转固（IErpInvStockMoveBiz 出库） | ⚠️ | owner doc `depreciation-and-posting.md §十 实现偏离补注`：`sourceType` 仅支持 `DIRECT_PURCHASE(30) + CIP(20)`，**INVENTORY 库存转固未实现**（IErpInvStockMoveBiz 调用未落地）。Deferred——非状态机问题 |
| **finance 反向 reverseDepreciation**（P1-MA1-016） | PASS | `ErpFinAccountingPeriodProcessor.reverseDepreciation:389` 跨域 daoFor ErpAstDepreciationSchedule **只读查询**（findAllByQuery），状态写经 `IErpAstDepreciationScheduleBiz.reverseDepreciation` I*Biz——`ErpAstDepreciationScheduleProcessor.reverseDepreciation:154-177` 状态迁移 EXECUTED→REVERSED 正确。**P1-MA1-016 维持**（finance→assets 跨域只读 DAO 查询违规，非状态写） |
| 外部步骤失败是否阻断状态迁移 | PASS | @BizMutation 事务回滚保证 approve 触发的跨域写失败时业务单据回滚；过账 tryPost 吞异常路径（posted=false 悬挂）是设计容错（与 finance P1-MA2-032 + hr P1-MA2-048 同型 P1-MA2-060） |

### 维度 8：TODO/任务策略（裁决：**PASS**）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| DRAFT assigned（资产管理员完善入账） | PASS | 资产 DRAFT 经 `ErpAstAssetCapitalizationBizModel` 资本化审批 TODO（不在本审计范围） |
| IN_SERVICE 否（正常折旧自动） | PASS | 折旧引擎自动查询 IN_SERVICE 计提 |
| **IDLE monitor（闲置超期提醒——owner doc §8）** | ⚠️ | owner doc §8 声明「闲置超期的资产可产生 TODO 提醒决策」，实现：IDLE 不可达（P1-MA2-061）+ 无闲置超期 cron job。**Deferred 联动**——IDLE 实现时一并补 |
| SCRAPPED/SOLD/DISPOSED 否（终态） | PASS | 终态无 TODO |
| posted=false 悬挂 TODO（资本化/处置过账失败） | ⚠️ | `ErpFinDeferredPostingRetryHelper` 兜底重试；期末结账前置检查不覆盖资产侧（与 hr P1-MA2-048 同型缺口）——P1-MA2-060 涵盖 |

### 维度 9：场景演练（最重要，裁决：**FAIL**——四处 P1 在场景中暴露）

#### 场景 (a) 设备购置折旧 happy path（裁决：**PASS**）

购置设备→创建资本化单 DRAFT→submit→approve→建卡 IN_SERVICE+折旧计划 PENDING→期末折旧→DEPRECIATION 凭证→5 年后净值=残值停止：
- Capitalization SUBMITTED→PROC approve→APPROVED + 建卡 IN_SERVICE + 折旧计划生成 + tryPost CAPITALIZATION 凭证
- DepreciationScheduleProcessor.executeDepreciation→schedule EXECUTED + 资产累计折旧回写 + tryPost DEPRECIATION 凭证
- **全链状态迁移守卫齐全 ✓**——资本化建卡/折旧计提/跨域过账经 I*Biz Facade

#### 场景 (b) 资产闲置与恢复（裁决：**FAIL**——P1-MA2-061）

设备停用→IN_SERVICE→IDLE→停提折旧→恢复使用→IDLE→IN_SERVICE→恢复折旧：
- **代码无 suspend/resume BizMutation**——IDLE 状态不可达
- 折旧引擎只查 IN_SERVICE，IDLE 不参与折旧是设计默认（owner doc §1「默认停提」）
- 但**无任何路径让资产进入 IDLE**——owner doc §场景 B 描述的"停用→恢复"全链不可执行

#### 场景 (c) 资产报废（清理凭证结转）（裁决：**PARTIAL PASS**——P1-MA2-060）

设备报废→IN_SERVICE→SCRAPPED→清理凭证结转原值/累计折旧/清理损失：
- Disposal SUBMITTED→PROC approve→APPROVED + 资产 SCRAPPED + cancelPendingSchedules + tryPost DISPOSAL 凭证
- 清理凭证：借累计折旧 + 借清理损失 / 贷固定资产原值（`DisposalAcctDocProvider` 内部分支 by disposalType）
- **场景 (c1) posted=true 路径**：✓ 全链状态正确
- **场景 (c2) posted=false 窗口期**：资产 SCRAPPED + disposal APPROVED + posted=false；后续 reverseApprove 仅设 disposal=REJECTED，资产**保持 SCRAPPED 终态 + schedules 保持 CANCELLED**——**P1-MA2-060**

#### 场景 (d) 资产出售（清理凭证+损益）（裁决：**PARTIAL PASS**——同 P1-MA2-060）

同 (c) 但 disposalType=SOLD → 资产 SOLD + 出售清理凭证（借累计折旧 + 借银行存款 / 贷固定资产 / 贷清理收益）。

#### 场景 (e) 折旧漏提补提（反结账 vs 当期）（裁决：**PASS**）

6 月资产折旧漏提，7 月发现：
- 选项一：反结账 6 月→补提（executeDepreciation(assetId, "2026-06")）→重新结账
- 选项二：7 月补提（executeDepreciation(assetId, "2026-07")，凭证注明补提期间）
- **executeDepreciation 接受任意 period 参数**——两条路径技术上可达 ✓

#### 场景 (f) 拆分/合并（IN_SERVICE→DISPOSED+新资产）（裁决：**PASS**）

拆分：源 IN_SERVICE→DISPOSED + 创建 N 新资产 IN_SERVICE + ASSET_SPLIT 凭证。合并：N 源 IN_SERVICE→DISPOSED + 创建 1 新资产 IN_SERVICE + ASSET_MERGE 凭证。**DISPOSED 经证据可达**（证伪 P2-MA1-023 死状态假设）。

#### 场景 (g) reverseApprove 红冲（业务单据→REJECTED+posted=false+凭证 reverse）（裁决：**FAIL**——P1-MA2-058 + P1-MA2-060）

PROC 路径（Capitalization/Disposal/ValueAdjustment）：
- doReverseApprove 设 REJECTED + 清 approvedBy/At + posted=false + 凭证 reverse（posted=true 时）
- 与 owner doc §2 强制 REJECTED 规则一致 ✓

INLINE 路径（Movement）：
- 设 SUBMITTED + 清 approvedBy/At
- **违反 owner doc §2 强制 REJECTED 规则** ⚠️——**P1-MA2-058**

资本化/处置 posted=false 窗口期 reverseApprove：
- 仅设业务单据 REJECTED，**不回滚资产行为**——P1-MA2-060

Split/Merge：THROW ERR_AST_SPLIT/MERGE_REVERSE_NOT_SUPPORTED ✓（不可逆契约）

#### 场景 (h) DIRECT 审批驱动的处置过账（浏览器层可达——xwf 限制替代路径）（裁决：**PASS**）

Disposal `useWorkflow=true` xwf 审批轴浏览器层不可达（owner doc §已知限制）；DIRECT 三轴审批（submit→approve）浏览器层可达：
- `ErpAstDisposalSubmitForApprovalProcessor.submitForApproval` 经 xbiz 调用 + 条件启动 nopFlowId（仅当 objMeta 配置 `wf:wfName`）
- DIRECT 审批驱动的过账触发状态迁移：approve→资产 SCRAPPED/SOLD + tryPost DISPOSAL 凭证 ✓
- `TestErpAstDisposalWorkflowApproval`（3 测试）覆盖工作流审批路径

#### 场景 (i) 资本化库存转固（IErpInvStockMoveBiz 出库）（裁决：**DEFERRED**）

owner doc §十 实现偏离补注：`sourceType=INVENTORY` 库存转固未实现，资本化仅支持 DIRECT_PURCHASE + CIP。`IErpInvStockMoveBiz` 跨域调用未落地——Deferred（非状态机问题）。

#### 场景 (j) 并发折旧同一资产（乐观锁）（裁决：**PASS**——交接 A2.17）

`executeDepreciation` 无悲观/乐观锁，并发触发同一 assetId+period 折旧可双写 schedule。**ErpAstDepreciationSchedule `versionProp="version"` 透明乐观锁将 silent lost-update 降级为 detectable conflict**——归 A2.17。

### 维度 10：与设计文档一致性（裁决：**FAIL**——四处 owner doc 漂移）

| 控制点 | 裁决 | 证据 |
|--------|------|------|
| **§1 资产 5 态 vs dict 6 态（DISPOSED）** | ⚠️ | owner doc state-machine.md §1 列 5 态（DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD），dict 含 6 态（多 DISPOSED）。兄弟 owner doc split-merge.md:103-104 已文档化 DISPOSED。**P2-MA1-023 维持**（owner doc drift） |
| **§折旧计划 3 态 vs dict 4 态（CANCELLED）** | ⚠️ | owner doc 列 3 态（PENDING/EXECUTED/REVERSED），dict 含 4 态（多 CANCELLED）。**P2-MA1-024 维持**（owner doc drift） |
| **§2 IDLE 迁移声明 vs 代码完全未实现** | ❌ FAIL | owner doc §2「IN_SERVICE→IDLE / IDLE→IN_SERVICE」迁移表 + §1「可配默认停提」+ §8「闲置超期提醒」均声明，代码零实现。**P1-MA2-061** 涵盖 |
| **§3 终态不可恢复约束落实** | PASS | SCRAPPED/SOLD/DISPOSED 无出边（见维度 2） |
| **§4 异常路径（折旧漏提补提）** | PASS | 两条路径技术上可达（见场景 e） |
| **§6 危险操作权限（期末批量折旧高影响）** | PASS | @BizMutation + 财务员权限门控 + 单失败隔离（见维度 6） |
| **§实现模式 PROC vs INLINE owner doc 未声明** | ❌ FAIL | owner doc state-machine.md 假设单一审批状态机，未声明 6 PROC + 1 INLINE（Movement）两模式并存——审查者期望单一模式行为一致，实际 Movement INLINE 缺守卫。**P2-MA2-060**（与 purchase P2-MA2-053 / sales P2-MA2-056 同型） |
| **state-machine.md 缺 7 业务单据独立章节** | ❌ FAIL | owner doc state-machine.md 仅覆盖资产卡片生命周期 + 折旧计划条目；7 业务单据（Movement/ValueAdjustment/Disposal/Capitalization/Split/Merge + CIP/Inventory/Maintenance）状态机散落在 depreciation-and-posting.md / split-merge.md / cip.md / inventory.md。**P2-MA2-059**（与 purchase/sales/mfg/hr 同型） |
| **§处置 DIRECT 审批 xwf 限制 owner doc 一致性** | PASS | owner doc state-machine.md §已知限制 已声明 xwf 审批轴浏览器层不可达 + DIRECT 替代路径 |
| **§关键业务规则 5 拆分/合并不可逆契约（Split/Merge reverseApprove THROW）** | PASS | owner doc split-merge.md §实现注记 §不可逆契约遵守声明 已文档化 THROW 路径；代码 `ErpAstSplitProcessor.reverseApprove:144-148` + `ErpAstMergeProcessor.reverseApprove:146` 抛 ERR_AST_SPLIT/MERGE_REVERSE_NOT_SUPPORTED ✓ |

---

## 4. 已登记 finding 资产状态机角度运行时复核

| Finding ID | 原描述 | 状态机角度复核 | 终态 |
|-----------|--------|--------------|------|
| `P1-MA1-008`（todo MR1，assets） | `ErpAstDepreciationSchedule`/Movement/Revaluation/Split/Merge/Disposal/Capitalization/Transfer 共 29 列 propId 缺失 | propId 是 ORM 元数据治理，非状态迁移——本审计确认**无运行时状态机影响**（状态字段值经 dict + ErpAstConstants 常量承载，与 propId 编号无关） | **不升级**（维持 P1 治理待 MR1） |
| `P1-MA1-016`（todo MR1，finance→assets） | `ErpFinAccountingPeriodProcessor.reverseDepreciation:389` 跨域 daoFor ErpAstDepreciationSchedule | finance→assets 跨域 **只读** DAO 查询（findAllByQuery），状态写经 `IErpAstDepreciationScheduleBiz.reverseDepreciation` I*Biz；assets 侧状态迁移 EXECUTED→REVERSED 正确（`ErpAstDepreciationScheduleProcessor.reverseDepreciation:154-177`） | **不升级**（维持 P1 治理待 MR1——跨域只读 DAO 治理缺陷，状态写经 I*Biz） |
| `P1-MA1-022`（todo MR1，9 域合并） | ast `ErpAstDepreciationScheduleProcessor:290` ErpFinAccountingPeriod + 9 个 posting dispatcher ErpMdSubject 只读 | **本审计复核确认**：`ErpAstDepreciationScheduleProcessor:290` daoFor(ErpFinAccountingPeriod).findAllByQuery 只读查询期间状态（OPEN/CLOSED），不破坏状态机；9 个 posting dispatcher 的 daoFor(ErpMdSubject) 是科目代码兜底解析，异常路径经 @BizMutation 事务回滚覆盖 | **不升级**（维持 P1 治理待 MR1） |
| `P2-MA1-023`（todo MR1，assets） | state-machine.md §1 列 5 态 vs dict 6 态（多 DISPOSED） | **本审计证伪死状态假设**——DISPOSED 经 `ErpAstSplitProcessor.disposeSourceAsset:449` + `ErpAstMergeProcessor:389` 写入**可达**，是合法"内部结构重组处置"语义；仅 owner doc §1 漏更新 | **维持 P2**（owner doc drift，DISPOSED 非死状态——P2-MA1-023 描述应澄清） |
| `P2-MA1-024`（todo MR1，assets） | 折旧计划条目状态 owner doc 列 3 态 vs dict 4 态（多 CANCELLED） | **本审计证伪死状态假设**——CANCELLED 经 `ErpAstAssetCapitalizationProcessor.cancelSchedules:291` + `ErpAstDisposalProcessor.cancelPendingSchedules:214` 写入**可达**，是合法"折旧因资产资本化冲销/处置而作废"语义；仅 owner doc §折旧计划条目状态 漏更新 | **维持 P2**（owner doc drift，CANCELLED 非死状态——P2-MA1-024 描述应澄清） |

---

## 5. 新登记 finding

### 5.1 P1 finding（4 项，目标 MR1）

| Finding ID | 描述 | 严重性 | 修复方式 |
|-----------|------|-------|---------|
| `P1-MA2-058` | **ErpAstMovement reverseApprove→SUBMITTED INLINE 违反 owner doc §2 强制 REJECTED 契约漂移**：`ErpAstMovement.xbiz` reverseApprove mutation 设 `entity.approveStatus = 'SUBMITTED'`，违反 owner doc `state-machine.md §3 L49` + `domain-design-guidelines.md §16.4` 强制 REJECTED 规则。6 大 Processor 合规（Capitalization/Disposal/ValueAdjustment →REJECTED + Split/Merge THROW 不可逆契约），但 Movement 无大 Processor，xbiz 全 5 动作 INLINE 直设。**不破坏红冲闭环一致性**——Movement 是资产位置/组织转移单据，**无 posted 副作用**（不过账，无凭证需 reverse），approve 也无下游业务副作用（不触发建卡/折旧/库存写）；仅 approveStatus 审计轨迹漂移（"重新提交中" vs 期望"曾审核过"）。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + hr A2.7a P1-MA2-039~042 + purchase A2.8 P1-MA2-049 + sales A2.9 P1-MA2-056 同型裁决（owner doc 强制规则 + xbiz 实现漂移，不破坏主路径）。 | major（契约漂移，不破坏业务路径） | MR1 裁决——方案 A（推荐）xbiz `reverseApprove` 改设 `entity.approveStatus = 'REJECTED'`（与 6 大 Processor 对齐 + owner doc §2 合规）；方案 B 引入 Movement 大 Processor 全守卫（与 P1-MA2-059 一并裁决） |
| `P1-MA2-059` | **ErpAstMovement 全 5 INLINE 动作缺 isCancelled/customer active/asset active 守卫致 CANCELLED 单据 approveStatus 副轴漂移**：`ErpAstMovement.xbiz` 全 5 动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）均仅校验 `approveStatus` src 后设新状态，**缺失** PROC 路径的 `validateNotCancelled`（docStatus != CANCELLED）/资产启用/到目标位置有效守卫。CANCELLED 单据（docStatus=CANCELLED）的 SUBMITTED approveStatus 可被 reject 设为 REJECTED（副轴漂移）。**实际危害进一步收窄**：(1) Movement **无 cancel mutation 暴露**——docStatus=CANCELLED 经服务层不可达，仅经 useLogicalDelete（delVersion）承载废弃；(2) docStatus 在 Movement 上仅作 codegen 默认值（DRAFT/ACTIVE），无业务路径写入 CANCELLED；(3) 不产生脏数据（仅审计轨迹混淆）。故裁决 P1 非 P0（危害比 purchase P1-MA2-050 / sales P1-MA2-057 更轻——后两者 Processor cancel 方法存在仅 xbiz 未暴露）。 | major（安全缺口，但危害有限——主终态经 useLogicalDelete 持有，副轴漂移不破坏业务路径） | MR1 裁决——方案 A（推荐）引入 Movement 大 Processor（与 6 业务单体同型），xbiz 改 `inject('...Processor').<action>(id, svcCtx)` 接线 + Delta beans.xml 注册，全守卫对齐；方案 B INLINE 路径补 `isCancelled` 守卫（最小变更：xbiz 脚本前加 `if (entity.docStatus === 'CANCELLED') throw ...`）；方案 C owner doc 标注「Movement 全 INLINE 无 cancel，CANCELLED 经 useLogicalDelete」（永久接受） |
| `P1-MA2-060` | **Capitalization/Disposal tryPost 吞异常悬挂 + reverseApprove 仅 posted=true 时回滚资产状态致资产侧状态悬挂**：(a) `CapitalizationPostingDispatcher.tryPost:45-58` + `DisposalPostingDispatcher.tryPost:39-51` try/catch 吞所有异常返回 false/null + LOG.warn/error；(b) `ErpAstAssetCapitalizationProcessor.approve:74-85` 失败时 posted=false 但 cap=APPROVED+ACTIVE，**资产已建 IN_SERVICE + 折旧计划 PENDING**——后续折旧会继续生成 DEPRECIATION 凭证但 GL 缺 CAPITALIZATION 入账分录（GL 与资产注册表不一致）；(c) `ErpAstDisposalProcessor.approve:90-100` 失败时 posted=false 但 disposal=APPROVED + 资产 SCRAPPED/SOLD（终态）；(d) **reverseApprove 仅在 posted=true 时回滚资产**——`ErpAstAssetCapitalizationProcessor.reverseApprove:103-115` 仅 posted=true 时资产→DRAFT + cancelSchedules；`ErpAstDisposalProcessor.reverseApprove:118-131` 仅 posted=true 时资产→IN_SERVICE + restoreCancelledSchedules。**posted=false 窗口期 reverseApprove 仅设业务单据 REJECTED，资产保持 IN_SERVICE/SCRAPPED/SOLD + schedules 保持 PENDING/CANCELLED**——无恢复路径，需运营人工 DB 干预。**DeferredPostingSweepJob (`ErpFinDeferredPostingRetryHelper`) 兜底重试**，但 reverseApprove 在 sweep 兜底前触发会留悬挂数据。**比 finance P1-MA2-032（IGNORED 悬挂）+ hr P1-MA2-048（salary 悬挂）更严重**——资产侧状态不可逆（终态 SCRAPPED/SOLD）+ reverseApprove 不对称；但**非 P0**：(1) 失败模式需过账引擎异常（基础设施故障/配置错误，非正常路径）；(2) LOG.warn/error 提供运维可见性；(3) DeferredPostingSweepJob 兜底；(4) 业财不一致可经期末试算平衡人工发现。 | major（功能性悬挂数据需运营介入，非数据破坏——DeferredPostingSweepJob + LOG + 期末试算兜底） | MR1 裁决——方案 A（推荐）`reverseApprove` 在 posted=false 窗口期也回滚资产行为（资本化：资产→DRAFT + cancelSchedules；处置：资产→IN_SERVICE + restoreCancelledSchedules），保证状态机对称；方案 B owner doc `depreciation-and-posting.md §七 错误处理` 标注「过账失败悬挂经 DeferredPostingSweepJob 兜底，reverseApprove 在悬挂窗口期需运营先触发 sweep 重试或手工 reverse 凭证再 reverseApprove」+ posted=false 悬挂派发 `IErpSysNotificationBiz` 告警（与 hr P1-MA2-048 方案 A 一并裁决）。触及会计保护区域，修复须独立 plan-audit + 人工确认 |
| `P1-MA2-061` | **ErpAstAsset IDLE 状态机迁移完全未实现 + 联动缺失**：dict `erp-ast/asset-status` 含 IDLE 项，owner doc `state-machine.md §1` 列「闲置（IDLE）—暂停使用，可配（默认停提）」+ §2 迁移表声明「IN_SERVICE→IDLE / IDLE→IN_SERVICE」+ §8「闲置超期提醒」，但实现：(a) `ASSET_STATUS_IDLE` 常量**零 writer**——grep 全 `module-assets/erp-ast-service/src/main` `setStatus(ASSET_STATUS_IDLE)` 零匹配（5 处仅作只读引用：`ErpAstInventoryProcessor:193` 盘点范围过滤 + `ErpAstDisposalProcessor:200` + `ErpAstValueAdjustmentProcessor:204` 守卫读取）；(b) 折旧引擎 `ErpAstDepreciationScheduleProcessor.executeBatchDepreciation:138` 仅查询 IN_SERVICE，IDLE 不参与折旧批量；(c) 无 suspend/resume/setIdle/toIdle/fromIdle BizMutation 方法（`ErpAstAssetBizModel` 17 行 CRUD 桩 + `ErpAstAsset.xbiz` `<actions/>` 空）；(d) 无闲置超期 cron job（owner doc §8 声明 Deferred）。IDLE 是**事实上的死状态**——dict 项 + owner doc 声明迁移但代码完全未实现。按 finance A2.5a P1-MA2-031 + mfg A2.6a P1-MA2-035 + mfg A2.6b P1-MA2-036 + hr A2.7a P1-MA2-039~042 同型裁决。**不破坏主路径**——IN_SERVICE 折旧主路径完整（资本化建卡 IN_SERVICE + 期末批量折旧 + 处置/拆分/合并终态转移），IDLE 仅作为预留状态；折旧引擎只查 IN_SERVICE 等价于"IDLE 默认停提"业务语义（owner doc §1 设计意图）。 | major（死状态 + 联动缺失，不破坏主路径——IN_SERVICE 折旧闭环完整） | MR1 裁决——方案 A（推荐）owner doc `state-machine.md §1/§2/§8` 标注「IDLE Deferred——资产暂停/恢复业务上线时实现 suspend/resume BizMutation + 闲置超期 cron job + 折旧引擎扩展查询 IN_SERVICE+IDLE」+ 维持 dict IDLE 项（保留语义入口）；方案 B 实现 suspend/resume BizMutation（资产 IN_SERVICE↔IDLE 迁移 + 折旧引擎扩展 + 闲置超期 TODO 经 `IErpSysNotificationBiz`） |

### 5.2 P2 finding（3 项 watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA2-059` | **state-machine.md 缺 7 业务单据状态机独立章节**：owner doc state-machine.md 仅覆盖资产卡片生命周期（§1-§9）+ 折旧计划条目（§折旧计划条目状态）；7 业务单据（Movement/ValueAdjustment/Disposal/Capitalization/Split/Merge + CIP/Inventory/Maintenance）状态机散落在 depreciation-and-posting.md / split-merge.md / cip.md / inventory.md。与 P2-MA2-037/043/045/047/052/053/056 同型（owner doc 缺独立章节）。 | watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增「对象三：业务单据双轴审批状态机」+「对象四：CIP 状态机」+「对象五：盘点单状态机」+「对象六：维修工单状态机」章节（本审计 §2.3-2.4 状态图可直接采用）；方案 B 交叉链接到各 owner doc |
| `P2-MA2-060` | **6 PROC + 1 INLINE（Movement）实现模式 owner doc 未声明**：owner doc `state-machine.md` 假设单一审批状态机，未声明 6 PROC（Capitalization/Disposal/ValueAdjustment/Split/Merge 全守卫 + Split/Merge reverseApprove THROW 不可逆契约）+ 1 INLINE（Movement 全 5 动作 xbiz 脚本仅基础 src 状态校验）两模式并存——审查者/开发者期望单一模式行为一致，实际 Movement INLINE 缺守卫（见 P1-MA2-058/059）。与 P2-MA2-053/056 同型。 | watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增「§实现模式」章节声明两模式 + INLINE 模式的守卫边界（仅 src 状态校验，无 isCancelled/业务规则守卫）+ Movement 全 INLINE 注记；方案 B 交叉链接到 `processor-extension-pattern.md` |
| `P2-MA2-061` | **6 业务单据 Processor cancel 方法死代码未接线**：`ErpAstSplitProcessor.cancel` + `ErpAstMergeProcessor.cancel` + `ErpAstValueAdjustmentProcessor.cancel` + `ErpAstAssetCapitalizationProcessor.validateTransitionForCancel` + `ErpAstDisposalProcessor.validateTransitionForCancel` + `ErpAstInventoryProcessor.cancel` + `ErpAstMaintenanceProcessor.cancel` 等 cancel 方法在 Processor 类中存在，但 xbiz **未暴露 cancel mutation**——docStatus=CANCELLED 经服务层不可达（仅经 useLogicalDelete delVersion 承载废弃）。与 purchase P2-MA2-054 同型（死代码 Processor 类）。 | watch-only，MR1 裁决 P1-MA2-059 时一并处置——若选方案 A（Movement 引入大 Processor），6 业务单据 cancel 也可一并经 xbiz 暴露接线；若选方案 C（owner doc 标注），cancel Processor 方法应删除或标注「保留作为 future cancel 接线备用，本期 docStatus=CANCELLED 经 useLogicalDelete」 |

---

## 6. 并发敏感点（交接 A2.17）

| 序号 | 位置 | 描述 | 处置 |
|-----|------|------|------|
| 1 | `ErpAstDepreciationScheduleProcessor.executeDepreciation:52-130` | 同一 (assetId, period) 并发触发折旧可双写 schedule（无悲观锁）；`ErpAstDepreciationSchedule` 声明 `versionProp="version"` 透明乐观锁 → silent lost-update → detectable conflict | 已降级（versionProp 透明乐观锁），归 A2.17 |
| 2 | `ErpAstDepreciationScheduleProcessor.executeBatchDepreciation:132-152` | 批量折旧期间资产被处置（IN_SERVICE→SCRAPPED）：批量查询快照 vs 单资产状态迁移竞态——批量内已读 IN_SERVICE 的资产可能在折旧计提时已被处置，处置 cancelPendingSchedules 与折旧 setStatus(EXECUTED) 竞态 | 归 A2.17 |
| 3 | `ErpAstDisposalProcessor.approve` 与 `ErpAstAssetCapitalizationProcessor.approve` 并发 | 极端场景：同一资产在资本化建卡后处置前被并发处置——versionProp 透明乐观锁降级为 detectable conflict | 已降级（versionProp），归 A2.17 |
| 4 | `ErpAstSplitProcessor.approve` 与 `ErpAstMergeProcessor.approve` 并发 | 同一源资产被并发拆分+合并——versionProp 透明乐观锁降级 | 已降级（versionProp），归 A2.17 |
| 5 | `ErpAstInventoryProcessor.post` 与折旧/处置并发 | 盘点 post 时盘亏资产置 SCRAPPED vs 折旧计提读取 IN_SERVICE 竞态——versionProp 透明乐观锁降级 | 已降级（versionProp），归 A2.17 |

> **重要事实**：`ErpAstAsset` / `ErpAstDepreciationSchedule` / `ErpAstAssetCapitalization` / `ErpAstDisposal` / `ErpAstValueAdjustment` / `ErpAstSplit` / `ErpAstMerge` / `ErpAstCip` / `ErpAstInventory` / `ErpAstMaintenance` 均声明 `versionProp="version"`（透明乐观锁），将 silent lost-update 降级为 detectable conflict。`ErpAstSplitLine` / `ErpAstMergeLine` / `ErpAstInventoryLine` 行级实体也声明 versionProp。

---

## 7. 综合裁决

### 7.1 Verdict

**⚠️(P1)**——资产状态机核心契约经实仓逐项证据确认（资产卡片生命周期迁移经资本化/处置/拆分/合并/盘点 Processor 副作用齐全、7 业务单据 6 PROC + 1 INLINE 模式、@BizMutation 事务回滚、reverseApprove 红冲闭环经大 Processor 路径强一致 [Capitalization/Disposal/ValueAdjustment →REJECTED 合规 + Split/Merge THROW 不可逆契约合规]、跨域写经 I*Biz Facade [production 代码无 daoFor(ErpFin*) 直写已确认]）；**零 P0**（四个候选 P0 经证据证伪或降级：(1) Movement reverseApprove→SUBMITTED 违反 owner doc §2 但不破坏红冲闭环一致性 [Movement 无 posted 副作用]，按 finance P1-MA2-031 + mfg P1-MA2-035 + hr P1-MA2-039~042 + purchase P1-MA2-049 + sales P1-MA2-056 同型裁决 P1；(2) Movement 全 INLINE 缺 isCancelled 守卫但不破坏主终态 [docStatus=CANCELLED 经服务层不可达，仅经 useLogicalDelete 承载]，按危害进一步收窄 P1；(3) Capitalization/Disposal tryPost 吞异常悬挂 + reverseApprove 不对称但 DeferredPostingSweepJob 兜底 + LOG 可见性 + 期末试算人工兜底，按功能性悬挂 P1；(4) IDLE 死状态 + 联动缺失按 finance P1-MA2-031 + mfg P1-MA2-035 + hr P1-MA2-039 同型裁决 P1，不破坏 IN_SERVICE 主路径折旧闭环）；**新增 4 项 P1**（P1-MA2-058/059/060/061）+ **新增 3 项 P2** watch-only（P2-MA2-059/060/061）；**5 项已登记 MA1 finding 运行时复核——DISPOSED/CANCELLED 经证伪非死状态**（P1-MA1-008 propId 无状态机影响 / P1-MA1-016 finance→assets 跨域只读无升级 / P1-MA1-022 跨域只读无升级 / **P2-MA1-023 DISPOSED 经 Split/Merge 可达非死状态，仅 owner doc drift** / **P2-MA1-024 CANCELLED 经 Capitalization.reverseApprove + Disposal.approve 可达非死状态，仅 owner doc drift**）；并发敏感点 5 处交接 A2.17（含 versionProp 透明乐观锁降级重要事实——10 个资产状态机实体全部声明 versionProp）。

### 7.2 状态机正确性维度 ast 列推进

`❓` → **`⚠️(P1)`**（资产状态机迁移正确性经审计确认 + 4 项 P1 待 MR1：P1-MA2-058 Movement reverseApprove→SUBMITTED 契约漂移 / P1-MA2-059 Movement 全 INLINE 缺守卫 / P1-MA2-060 Capitalization/Disposal tryPost 吞异常悬挂 + reverseApprove 不对称 / P1-MA2-061 IDLE 状态机迁移完全未实现；3 项 P2 watch-only；5 项 MA1 finding 运行时复核 [DISPOSED/CANCELLED 经证伪非死状态] 无升级；并发敏感点 5 处交接 A2.17）。

### 7.3 残留风险

- **Movement 全 INLINE 无 Processor 守卫**（P1-MA2-058/059）：CANCELLED 单据的 approveStatus 副轴漂移——若未来 Movement 添加按 approveStatus 过滤的业务查询，可能产生意外结果。MR1 修复时建议方案 A（引入大 Processor）。
- **Capitalization/Disposal tryPost 悬挂 + reverseApprove 不对称**（P1-MA2-060）：posted=false 窗口期 reverseApprove 不回滚资产，资产保持 IN_SERVICE/SCRAPPED/SOLD 状态——若运营不熟悉该路径，资产可能长期悬挂。MR1 修复时建议方案 A（reverseApprove 在 posted=false 窗口期也回滚资产，状态机对称）。
- **IDLE 死状态**（P1-MA2-061）：dict 项存在但零 writer——若业务方依赖 IDLE 表达"暂停使用"，需手动 DB update 或新建资产分类实现等效语义。MR1 修复时建议方案 A（owner doc 标注 Deferred）。
- **A2.17 并发审计未覆盖**：本审计仅标注并发敏感点，系统性并发正确性裁决归 A2.17。
- **A4.3 assets Processor 链路专属审计未覆盖**：48 Processor 代码质量（异常处理/N+1/索引/辅助方法）归 A4.3。
- **A4.6/A4.7 view.xml drift 未覆盖**：资产页面契约漂移归 A4.6/A4.7。

### 7.4 范围内已覆盖 / 范围外已交接

| 范围 | 状态 |
|------|------|
| 资产卡片生命周期状态机迁移正确性 | ✅ 已审计 |
| 折旧计划条目状态机 | ✅ 已审计（PENDING/EXECUTED/REVERSED/CANCELLED 全态可达，证伪 P2-MA1-024 死状态） |
| 7 业务单据双轴审批状态机 | ✅ 已审计（6 PROC + 1 INLINE） |
| CIP/Inventory/Maintenance 三状态机 | ✅ 已审计 |
| 终态不可恢复约束（SCRAPPED/SOLD/DISPOSED） | ✅ 已审计 |
| reverseApprove 目标态合规性 | ✅ 已审计（5/6 合规 + Movement 漂移 P1-MA2-058） |
| 资本化/处置 tryPost 悬挂 + reverseApprove 不对称 | ✅ 已审计（P1-MA2-060） |
| IDLE 状态机迁移缺失 | ✅ 已审计（P1-MA2-061） |
| DISPOSED/CANCELLED 可达性 | ✅ 已审计（证伪死状态假设，仅 owner doc drift） |
| MA1 finding 状态机角度复核 | ✅ 已审计（5 项无升级，DISPOSED/CANCELLED 经证伪非死状态） |
| 并发敏感点 | ⚠️ 标注，交 A2.17 |
| 折旧引擎/Processor 代码质量 | ❌ 交 A4.3 |
| view.xml drift | ❌ 交 A4.6/A4.7 |
| 期末结账 GL 正确性 | ❌ 交 A2.3 finding（已 done） |
| config-gated Deferred（IDLE 停提配置 / 多级审批链 / 闲置超期提醒 job） | ❌ 各 successor 触发条件满足时 |

---

## 8. 参考

- `docs/design/assets/state-machine.md`（owner doc，资产卡片生命周期 + 折旧计划条目状态）
- `docs/design/assets/depreciation-and-posting.md`（折旧/处置/资本化凭证 + 期末批量折旧 + reverseDepreciation 冲销 + §十 实现偏离补注）
- `docs/design/assets/split-merge.md`（拆分/合并 DISPOSED 状态引入 + §关键业务规则 5 不可逆契约 + §实现注记）
- `docs/design/assets/cip.md`（在建工程资本化 + §实现注记）
- `docs/design/assets/inventory.md`（资产盘点差异 → 移动单 + 反向红冲）
- `docs/architecture/processor-extension-pattern.md`（Facade+Processor 两层）
- `docs/architecture/posting-exemptions.md`（资产过账跨域写豁免登记）
- `docs/skills/state-machine-business-review-prompt.md`（10 维度审查方法）
- `docs/plans/2026-07-28-0400-2-audit-remediation-ma2-assets-state-machine.md`（本审计 plan）
- 关联审计：`docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`（A2.8 purchase 状态机审查范式——P1-MA2-049/050/051 同型）/ `2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9 sales——P1-MA2-056/057 同型）/ `2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（A2.5a 凭证——P1-MA2-032 tryPost 吞异常悬挂同型根因）/ `2026-07-28-0230-arm-ma2-hr-attendance-payroll-state-machine.md`（A2.7b hr——P1-MA2-048 salary 过账吞异常悬挂同型）
