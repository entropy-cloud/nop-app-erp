# RC MA1 A1.6 — finance-F6 期间与结账（期末结账前置门禁 + 反结账）需求-实现符合性审计

> Audit Status: closed
> 里程碑：MA1（需求-实现符合性层 / 五级追踪矩阵维度）
> 工作项：A1.6（MA1 需求追踪矩阵审计 — finance-F6 期间与结账：期末结账前置门禁 + 反结账）
> 审计 plan：`docs/plans/2026-08-02-1815-3-rc-ma1-a1-6-finance-f6-period-close.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级矩阵 / §2 分级判据 / §3 完整枚举 / §4 Q1 真相源层级 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 报告 9 段骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 机制 / §去重协议）
> L1 真相源：`docs/design/finance/use-cases.md`（UC-FIN-06/07，2 UC，12 条验收标准）
> L1 锚点清单：`docs/audits/rc-requirement-baseline-inventory.md` §finance + §切片索引 A1.6（`:110` / `:129`）
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源；方法论 §5 保护区域，roadmap 预授权类目）
> 审计日期：2026-08-02
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`c1b775491`

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（活跃数据破坏 / 会计过账正确性破坏） | **0** | 无 MR0 即时通道触发 |
| **P1**（新登记） | **1** | P1-RC-006（UC-FIN-07⑫ 反结账审计轨迹缺失——`reverseClose(periodId, context)` 无 reason 参数 + ORM 无 `reversedBy`/`reverseCloseReason` 列 + 无 `ReverseCloseLog` 实体，全仓 grep 0 命中，未登记 finding）→ MR1（触及 ORM 结构变更，须 ask-first） |
| **P1**（复用） | **1** | P1-MA3-046（UC-FIN-07⑥ 高权限无运行时角色强制——reverseClose 仅 kill-switch 无 `@BizAuth`/`@RolesAllowed`，全域敏感动作零运行时权限保护 finding 的 finance 反结账投影，追加 RC 交叉引用） |
| **P2**（新登记） | **2** | P2-RC-006（UC-FIN-06③ AR/AP reminder 偏离 L1 字面"拒绝"——L2 owner doc `period-close.md:42-43` 已记录有意设计 + P1-MA2-017 resolved，按 §4 L1 为准仍为分歧，倾向接受）/ P2-RC-007（UC-FIN-07⑨ 反结账"成本凭证冲销"缺失——INV costing 无 finance 侧期间凭证可冲，owner doc `:9` Non-Goal）→ successor watch-only |
| **接受**（符合需求契约） | **9 验收标准** | UC-FIN-06 ①未过账凭证 hard block / ②未审核凭证（归①）/ ④资产未折旧（间接 auto-execute + 悬挂阻断）/ ⑤成本未算（间接 + 悬挂阻断）/ UC-FIN-07 ⑧状态迁移 CLOSED_FINAL→OPEN / ⑨结转/FX/折旧凭证冲销 / ⑩解锁单据（间接）/ ⑪重新结账（幂等）/ + 坏账缺口 hard block（owner doc 增量要求，已实现） |
| resolved finding HEAD 复核 | **11/11 已落地** | P0-MA2-016 + P1-MA2-017/018/019/020/021/022/033 + P1-MA3-036 + P1-MA4-004/005 在当前 HEAD 实际落地（R6.1 per-mutation Processor 拆分后按逻辑核验） |
| MA2 既有行为证据复用 | A2.3 period-close E2E + A2.5b period-budget 状态机 | 无升级（详见 §4 / §9） |
| 报告校正项 | **1** | plan Current Baseline 称"反结账审批 kill-switch 默认阻断路径无测试"——**过时**，`TestErpFinModuleCloseOrder#testReverseCloseApprovalBlocked:78-90` 显式覆盖默认 `reverse-close-approval-required=true` 阻断路径 |

**整体裁决**：A1.6 切片 2 UC 五级追踪矩阵填齐，12 条验收标准（UC-FIN-06 五前置条件 + UC-FIN-07 反结账七要求）经 L3-L5 四级证据逐条核对。**1 项新 P1**（反结账审计轨迹缺失）+ **1 项复用 P1**（高权限无角色强制，归 P1-MA3-046）+ **2 项新 P2**（AR/AP reminder 偏离 L1 / 反结账成本凭证冲销缺失）。期末结账前置门禁主路径（未过账凭证/坏账缺口 hard block + 资产折旧/成本重算 auto-execute + 跨域悬挂兜底阻断）+ 反结账主路径（CLOSED_FINAL→OPEN + 结转/FX/年末结转/折旧凭证红冲 + 模块回开 + 重新结账幂等）均经单测/E2E 强断言覆盖。**零 P0**——无活跃数据破坏、无会计过账正确性破坏、无核心业务循环断裂。本审计**不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。验收标准逐字引用，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。

### UC-FIN-06 期末结账前置门禁（`use-cases.md:110`）

**场景**：期末结账时,前置检查拦截未过账/未核销/未折旧。

```
可验证断言(见 period-close.md §前置检查):
期间.结账(→CLOSING) 前置检查:
  若 存在 posted=false 的单据 → 拒绝(列出)
  若 存在未审核凭证 → 拒绝
  若 存在未核销应收应付(强制核销模式) → 拒绝
  若 资产未折旧 → 拒绝
  若 成本未算 → 拒绝
全部通过 → 进入结账步骤(见 §结账8步)
```

**涉及机制**：`period-close.md §前置检查`、`state-machine.md`（期间状态）

> **验收标准完整枚举**（§3 完整枚举纪律，5 条逐一进入 L5 判读，不抽样）：
> - **PC-1**：若 存在 `posted=false` 的单据 → 拒绝（列出）
> - **PC-2**：若 存在未审核凭证 → 拒绝
> - **PC-3**：若 存在未核销应收应付（强制核销模式） → 拒绝
> - **PC-4**：若 资产未折旧 → 拒绝
> - **PC-5**：若 成本未算 → 拒绝
> - **PC-6**（隐式）：全部通过 → 进入结账步骤

### UC-FIN-07 反结账（`use-cases.md:129`）

**场景**：已 CLOSED_FINAL 的期间需要反结账(更正错误)。

```
可验证断言(见 period-close.md §反结账):
反结账需: 高权限 + 审批
CLOSED_FINAL → OPEN
冲销: 结转凭证/折旧凭证/成本凭证
解锁期间内单据(可修改)
重新结账 → CLOSED_FINAL
全程审计(记录反结账操作人/原因)
```

**涉及机制**：`period-close.md §反结账`、`state-machine.md`

> **验收标准完整枚举**（§3 完整枚举纪律，7 条逐一进入 L5 判读，不抽样）：
> - **RC-1**：反结账需 高权限
> - **RC-2**：反结账需 审批
> - **RC-3**：`CLOSED_FINAL → OPEN`
> - **RC-4**：冲销 结转凭证
> - **RC-5**：冲销 折旧凭证
> - **RC-6**：冲销 成本凭证
> - **RC-7**：解锁期间内单据（可修改）
> - **RC-8**：重新结账 → `CLOSED_FINAL`
> - **RC-9**：全程审计（记录反结账操作人 / 原因）

> **L1↔L2 冲突注记**（§4 真相源层级）：L2 owner doc `period-close.md:42-43` 对 PC-3 未核销 AR/AP 处理为"提示"非"拒绝"；L2 `:321-325` 对 RC-1/RC-2 高权限/审批为"config kill-switch，审批流 successor"。按 §4 Q1 裁决=(c) 逐项对照分歧处裁决，**冲突以 L1 为准，L2 推定已向实现妥协**。分歧记入报告（§5 候选缺口），不直改真相源（§9 冻结条款）。

---

## 2. 实现证据（L3，`file:line`，跨域调用链列全）

> 审计对象实仓逐项核实（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinAccountingPeriodBizModel.java` Facade + `.../service/processor/ErpFinAccountingPeriod*.java` 6 per-mutation Processor + 共享编排 helper `ErpFinAccountingPeriodProcessor.java` + `ErpFinVoucherBizModel.assertPeriodNotLocked`）。L3 引用格式遵循 §1 L3 规范（含行号）。R6.1 per-mutation 拆分后行号按当前 HEAD `c1b775491` 实测。

### 2.1 Facade（`ErpFinAccountingPeriodBizModel.java`，85 行）

| mutation | 文件:行 | 委托 Processor | 审计状态 |
|---|---|---|---|
| `preCheck` | `:51-54`（`@BizQuery` 只读） | `ErpFinAccountingPeriodPreCheckProcessor` | ✅ |
| `closePeriod` | `:57-60`（`@BizMutation`） | `ErpFinAccountingPeriodClosePeriodProcessor` | ✅ |
| `finalizePeriod` | `:63-66`（`@BizMutation`） | `ErpFinAccountingPeriodFinalizePeriodProcessor` | ✅ |
| `reverseClose` | `:69-72`（`@BizMutation`，**无 reason 参数**） | `ErpFinAccountingPeriodReverseCloseProcessor` | ✅（审计轨迹缺口见 §5 候选缺口 RC-9） |
| `openPeriod` | `:75-78`（`@BizMutation`） | `ErpFinAccountingPeriodOpenPeriodProcessor` | ✅ |
| `generateNextYearPeriods` | `:81-84`（`@BizMutation`） | `ErpFinAccountingPeriodGenerateNextYearPeriodsProcessor` | ✅ |

> **权限注解核查**：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/entity/ErpFinAccountingPeriodBizModel.java` + `.../processor/ErpFinAccountingPeriod*Processor.java` 全 7 文件 `@BizAuth`/`@RolesAllowed`/`@PreAuthorize`/`@Secured`/`@AuthIn` grep **0 命中**——RC-1 高权限仅 kill-switch（`ErpFinAccountingPeriodProcessor.isReverseCloseApprovalRequired:653-656`），无运行时角色强制（§5 候选缺口 RC-1，复用 P1-MA3-046）。

### 2.2 PC-1 / PC-2 前置检查（未过账凭证 hard block）

`ErpFinAccountingPeriodProcessor.findUnpostedVoucherCodes:432-440`（按 periodId 查 ErpFinVoucher，filter `docStatus != POSTED`，列号清单）→ `PeriodPreCheckReport.unpostedVoucherCodes` → `hasIssues():PeriodPreCheckReport` 返回 true（`!unpostedVoucherCodes.isEmpty() || !unresolvedPostingExceptionKeys.isEmpty()`）→ `ErpFinAccountingPeriodClosePeriodProcessor.closePeriod:59-63` 当 `!isAutoPostOnClose() && report.hasIssues()` 抛 `ERR_PRE_CHECK_BLOCKED`（**hard block**，config `erp-fin.auto-post-on-close` 默认 false）。

> **PC-2（未审核凭证）归 PC-1**：ErpFinVoucher 只有 DRAFT/CANCELLED 非终态审核未通过会落入 `docStatus != POSTED`，PC-1 的 `findUnpostedVoucherCodes` 一并覆盖。

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **PC-1**（未过账凭证 hard block） | `ErpFinAccountingPeriodProcessor.findUnpostedVoucherCodes:432-440` + `ClosePeriodProcessor:59-63`（`!isAutoPostOnClose() && report.hasIssues() → ERR_PRE_CHECK_BLOCKED`） | hard block（默认 config） | ✅ |
| **PC-2**（未审核凭证） | 归 PC-1（docStatus != POSTED 含 DRAFT/CANCELLED） | 同 PC-1 | ✅ |

### 2.3 PC-3 未核销 AR/AP（实现为 reminder，偏离 L1）

`ErpFinAccountingPeriodProcessor.findUnsettledArApCodes:442-462`（按 `businessDate ∈ [start,end]` + orgId + 主账套 acctSchemaId 过滤，filter `status != SETTLED/CANCELLED/WRITTEN_OFF`，多公司/多账套读路径隔离 P1-MA2-095）→ `PeriodPreCheckReport.unsettledArApCodes` → `hasReminders():PeriodPreCheckReport`（`!unsettledArApCodes.isEmpty() || allowanceExcess>0`，**非 hasIssues**）→ `ClosePeriodProcessor` **不阻断**（owner doc `period-close.md:42-43` 记录为"提示"有意设计，P1-MA2-017 resolved）。

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **PC-3**（未核销 AR/AP → 拒绝） | `ErpFinAccountingPeriodProcessor.findUnsettledArApCodes:442-462` → `hasReminders()`（**非 hasIssues**）→ `ClosePeriodProcessor` 不阻断 | **偏离 L1**（实现为 reminder，L1 字面"拒绝"，L2 已记录有意设计） | ⚠ P2-RC-006 |

### 2.4 PC-4 资产未折旧 / PC-5 成本未算（间接 auto-execute + 悬挂阻断）

`ErpFinAccountingPeriodClosePeriodProcessor.closePeriod:69-71` 编排 AR→AP→INV→AST→GL 模块关账：
- **AST 模块**（`ErpFinAccountingPeriodProcessor.closeAssetModule:151-155`）→ `runDepreciation:179-200`（config `erp-fin.auto-depreciation-on-close` 默认 true → `IErpAstDepreciationScheduleBiz.executeBatchDepreciation`，G3 错误传播：impl 未就绪容错跳过 / 配置错误或真实故障 NopException rethrow 阻断）。
- **INV 模块**（`ErpFinAccountingPeriodProcessor.closeInvModule:144-148`）→ `recloseInvCosts:208-232`（config `erp-fin.inv-costing-reclose-on-close` 默认 true → `IErpInvCostingBiz.reclosePeriodCosts`，G3 同前）。

跨域悬挂兜底：`ErpFinAccountingPeriodProcessor.findUnresolvedPostingExceptionKeys:471-477` 三层扫描（finance PENDING/RETRYING/MANUAL 未补录 voucherId + assets 折旧 posted=false EXECUTED + inventory 到岸成本 posted=false APPROVED）→ `hasIssues()` → `ClosePeriodProcessor:59-63` hard block（auto-post-on-close=false 时）。

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **PC-4**（资产未折旧 → 拒绝） | `closeAssetModule:151-155` → `runDepreciation:179-200`（auto-execute）+ `findUnresolvedDepreciationSchedules:506-527`（悬挂兜底，EXECUTED+posted=false） | 间接实现（auto-execute 步骤 + 悬挂阻断；无显式 preCheck 字段） | ✅（行为达成） |
| **PC-5**（成本未算 → 拒绝） | `closeInvModule:144-148` → `recloseInvCosts:208-232`（auto-execute）+ `findUnresolvedLandedCosts:530-549`（悬挂兜底，APPROVED+posted=false） | 间接实现（auto-execute + 悬挂阻断；无显式 preCheck 字段） | ✅（行为达成） |

### 2.5 owner doc 增量（坏账准备充足性 hard block）

> L1 UC-FIN-06 **未要求**坏账准备充足性；L2 owner doc `period-close.md:11,45-49` 增量要求"必需准备 > 账面 → shortfall 阻止结账"（NRV 应收 #1 审计断言）。本节为 L2 增量，按 §4 真相源层级（L2 设计参考）核实实现。

`ErpFinAccountingPeriodProcessor.populateAllowanceCheck:99-118`（账龄分桶法 Σ openAmount×损失率 vs GL Allowance 账面，shortfall/excess）→ `PeriodPreCheckReport.hasAllowanceShortfall()`（**独立硬阻断，不受 auto-post-on-close 影响**）→ `ClosePeriodProcessor:52-56`（`if (report.hasAllowanceShortfall()) throw ERR_PRE_CHECK_BLOCKED`）。config `erp-fin.bad-debt-allowance-gate-enabled` 默认 true。

| 验收标准（L2 增量） | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| 坏账缺口 hard block | `populateAllowanceCheck:99-118` + `ClosePeriodProcessor:52-56` | hard block（独立于 auto-post-on-close） | ✅ |

### 2.6 PC-6 进入结账步骤（owner doc §结账8步概念，实现坍缩为同步 closePeriod）

`ClosePeriodProcessor.doClosePeriod:46-87`：
- `:48` assertPeriodStatus(OPEN) → `:50` preCheck → `:52-63` 阻断门控 → `:65` findOrCreatePeriodStatus → `:67-71` 模块按序关账（AR→AP→INV→AST→GL，`Module` enum `:708-725` `predecessor()` 守卫，乱序抛 `ERR_MODULE_OUT_OF_ORDER`）→ `:76-78` 年度分支（12月）→ `:81-82` 状态 OPEN→CLOSING→CLOSED → `:83-84` closedAt/closedBy → flush。

owner doc §结账8步（`:60-111`）映射：
1. 业务单据过账检查 → PC-1/PC-2 ✅
2. 成本核算 → `recloseInvCosts:208-232`（config-gated，BATCH/LIFO/STANDARD 为 inventory successor）✅
3. 折旧计提 → `runDepreciation:179-200`（config-gated，G3）✅
4. 费用摊销 → **缺失（Non-Goal，模块未落地）**
5. 损益结转 → `closeGlModule:163-171` → `profitLossClosingService.close`（FX 损益已含 P0-MA2-016 resolved）✅
6. 结账凭证 + 试算平衡 → `populateTrialBalanceForAllSchemas:331-383`（无独立结账汇总凭证，试算快照）✅
7. 标记 CLOSED + 锁 + 开下期 → `ClosePeriodProcessor:81-84`（CLOSED + closedBy/closedAt）+ `ErpFinVoucherBizModel.assertPeriodNotLocked:177-195`（P1-MA2-021 凭证锁）+ 次年期间由 `generateNextYearPeriods`（年度分支）✅
8. 结账报表 → nop-report Non-Goal（owner doc `:9`）

### 2.7 RC-1 / RC-2 反结账高权限 + 审批（kill-switch，无审批流）

`ErpFinAccountingPeriodReverseCloseProcessor.reverseClose:22-58`：
- `:24` assertPeriodStatus(CLOSED_FINAL)
- `:26-29` **kill-switch 门控**：`if (isReverseCloseApprovalRequired()) throw ERR_REVERSE_CLOSE_APPROVAL_REQUIRED`（config `erp-fin.reverse-close-approval-required` 默认 true → 直接拒绝；false → 无条件放行）

> **RC-1 高权限**：仅 kill-switch（管理员设 config=false 才能 reverseClose），**无 @BizAuth/@RolesAllowed 角色强制**（全仓 grep 0 命中）。**RC-2 审批**：完全缺失，无独立审批 action（documented simplification，`period-close.md:321-325`，P1-MA2-020/P1-MA3-036 resolved 为 kill-switch successor）。

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **RC-1**（高权限） | `ReverseCloseProcessor:26-29` kill-switch + `ErpFinAccountingPeriodProcessor.isReverseCloseApprovalRequired:653-656` | **仅 kill-switch**，无运行时角色强制 | ⚠ 复用 P1-MA3-046 |
| **RC-2**（审批） | 缺失（无审批 action） | **缺失**，documented simplification successor（P1-MA2-020/P1-MA3-036） | ⚠ 接受（owner doc 已登记 successor） |

### 2.8 RC-3 状态迁移 CLOSED_FINAL→OPEN

`ReverseCloseProcessor:39`（`period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN)`，一步直接迁移；owner doc §反结账流程 `:189`"CLOSED_FINAL → CLOSING → OPEN"概念三步，实现坍缩为同步一步，CLOSING 瞬态不刷出，P2-MA2-025 watch-only）。年末反结账阻断：`:32-36`（`isYearEnd(period) && hasNextYearPeriods(year+1)` → `ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`）。

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **RC-3**（CLOSED_FINAL→OPEN） | `ReverseCloseProcessor:39` | ✅（一步直接迁移） | ✅ |

### 2.9 RC-4 / RC-5 / RC-6 反结账冲销凭证

`ReverseCloseProcessor:42-52`：
- **RC-4 结转凭证**：`:42-43` `reverseCloseVoucher(period, PL_BILL_CODE_PREFIX + code, PERIOD_CLOSE, context)` → `ErpFinAccountingPeriodProcessor.reverseCloseVoucher:553-562`（按 billCode+businessType 反查 VoucherBillR，存在则 `voucherBiz.reverse`）✅
- **FX 重估凭证**（owner doc 反结账步骤4 扩展）：`:44-45` `FX-REVAL-` + `EXCHANGE_GAIN_LOSS` ✅
- **年末结转凭证**：`:46-49` `isYearEnd(period)` → `ANNUAL-CLOSE-` + `PROFIT_TO_RETAINED_EARNINGS` ✅
- **RC-5 折旧凭证**：`:50-52` `isAutoDepreciationOnClose()` → `ErpFinAccountingPeriodProcessor.reverseDepreciation:238-263`（仅按 period + posted=true 过滤，逐 schedule 调 `IErpAstDepreciationScheduleBiz.reverseDepreciation`，G3 错误传播）✅
- **RC-6 成本凭证**：**缺失**（Non-Goal——INV costing 无 finance 侧期间凭证可冲，owner doc `:9` 显式登记）⚠

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **RC-4**（冲销结转凭证） | `ReverseCloseProcessor:42-43` + `reverseCloseVoucher:553-562` | ✅ | ✅ |
| **RC-5**（冲销折旧凭证） | `ReverseCloseProcessor:50-52` + `reverseDepreciation:238-263` | ✅（config-gated） | ✅ |
| **RC-6**（冲销成本凭证） | **缺失** | Non-Goal（INV costing 无 finance 侧期间凭证可冲） | ⚠ P2-RC-007 |

### 2.10 RC-7 / RC-8 解锁单据 + 重新结账

- **RC-7 解锁单据**：`ReverseCloseProcessor:55-56`（`findOrCreatePeriodStatus` + `reopenModules:278-284` 重置 per-module 状态为 OPEN）；期间状态 OPEN 解除 `ErpFinVoucherBizModel.assertPeriodNotLocked:177-195` 凭证锁，间接解锁期间内单据（无显式单据解锁）。
- **RC-8 重新结账**：经 `reverseClose → OPEN` 后再次 `closePeriod → CLOSED` + `finalizePeriod → CLOSED_FINAL` 幂等（`TestErpFinReverseClose:49-50` + `TestErpFinPeriodStateMachine#testForwardAndReverse:66-67` 验证）。

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **RC-7**（解锁单据） | `ReverseCloseProcessor:55-56` + `reopenModules:278-284` + 期间 OPEN 解凭证锁 | 间接实现（无显式单据解锁） | ✅ |
| **RC-8**（重新结账→CLOSED_FINAL） | 幂等链 reverseClose→closePeriod→finalizePeriod | ✅ | ✅ |

### 2.11 RC-9 反结账全程审计（操作人 / 原因）—— **缺失**

> **关键事实**（grep 实测 HEAD `c1b775491`）：
> - `IErpFinPeriodCloseBiz.reverseClose` 契约（`module-finance/erp-fin-dao/.../IErpFinPeriodCloseBiz.java`）签名 `reverseClose(@Name("periodId") Long periodId, IServiceContext context)`——**无 reason 参数**。
> - `ErpFinAccountingPeriod` ORM（`module-finance/model/app-erp-finance.orm.xml:655-694`）字段仅 `closedBy:670` / `closedAt:671`（**无 `reversedBy` / `reverseCloseReason` / `reverseCloseAt` 列**）。
> - 全仓 `rg "reversedBy|reverseCloseReason|reverseCloseAt|ReverseCloseLog|reverseCloseLog"` Java/XML（排除 docs/）= **0 命中**。
> - arm-index 全分区无对应控制点 finding。

| 验收标准 | 文件:行 | 实现证据 | 审计状态 |
|---|---|---|---|
| **RC-9**（全程审计：操作人/原因） | **完全缺失**（无 reason 参数 / 无 ORM 审计列 / 无 ReverseCloseLog 实体） | **未实现且未登记 finding** | ⚠ P1-RC-006 |

### 2.12 跨模块调用链（finance → assets / inventory）

| 调用 | 文件:行 | 性质 |
|---|---|---|
| finance → assets（折旧） | `runDepreciation:184-200` → `IErpAstDepreciationScheduleBiz.executeBatchDepreciation` | 跨域 command 编排（合法，`data-dependency-matrix.md §4.4` 例外） |
| finance → assets（反结账折旧冲销） | `reverseDepreciation:241-256` → `IErpAstDepreciationScheduleBiz.reverseDepreciation` | 跨域 command 编排（合法） |
| finance → assets（折旧悬挂扫描） | `findUnresolvedDepreciationSchedules:506-527` `daoProvider.daoFor(ErpAstDepreciationSchedule.class)` | 跨域只读 DAO（P1-MA1-016/P1-MA1-022 登记的合法豁免，读侧统一裁决登记于 `data-dependency-matrix.md §9`） |
| finance → inventory（成本兜底重算） | `recloseInvCosts:212-225` → `IErpInvCostingBiz.reclosePeriodCosts` | 跨域 command 编排（合法） |
| finance → inventory（到岸成本悬挂扫描） | `findUnresolvedLandedCosts:530-549` `daoProvider.daoFor(ErpInvLandedCost.class)` | 跨域只读 DAO（合法豁免） |

---

## 3. 测试证据（L4，注明断言强度）

> 测试位于 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/entity/`。断言强度引用 MA5 评级（深/中/浅）。期间结账/反结账测试套件共 7 文件（不含 perf），覆盖前置检查 + 状态机 + 模块关账顺序 + 端到端 + 反结账 + 年度结转 + 凭证锁。

### 3.1 前置检查（`TestErpFinPeriodPreCheck.java`，3 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testPreCheckListsIssues` | `:47-67` | PC-1（1 张未过账凭证 `V-DRAFT-002` 检出）+ PC-3（1 笔未核销 AR `ARI-OPEN-001` 检出） | **深**（精确断言 size=1 + code 字符串） |
| `testPreCheckCleanPeriod` | `:69-78` | 干净期间 `hasIssues()` = false | **深** |
| `testBlockingCloseRejectsWithIssues` | `:80-97` | **PC-1 hard block**（默认 auto-post-on-close=false：未过账凭证 → `assertThrows(NopException.class, ... closePeriod)` + 状态保持 OPEN） | **深**（异常 + 状态守卫） |

### 3.2 期间状态机（`TestErpFinPeriodStateMachine.java`，5 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testForwardAndReverse` | `:47-68` | OPEN→CLOSED→CLOSED_FINAL→OPEN（反结账，审批门控已关闭）+ RC-8 重新结账 CLOSED | **深**（4 态正向 + 反向 + 重新结账） |
| `testIllegalTransitionsRejected` | `:70-93` | 非法迁移拒绝：OPEN→finalizePeriod / OPEN→reverseClose / CLOSED→closePeriod / CLOSED→reverseClose | **深**（4 个 assertThrows） |
| `testNonBlockingCloseWithIssues` | `:95-110` | auto-post-on-close=true 提示模式：未过账凭证不阻断结账 | **深**（PC-1 反向 config 行为） |
| `testOpenPeriodFromNeverOpened` | `:112-122` | P1-MA2-033 NEVER_OPENED→OPEN | **深** |
| `testOpenPeriodRejectsNonNeverOpened` | `:124-134` | 非 NEVER_OPENED 调 openPeriod 拒绝 | **深** |

### 3.3 模块关账顺序（`TestErpFinModuleCloseOrder.java`，3 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testModuleCloseOrder` | `:47-61` | AR→AP→INV→AST→GL 全 CLOSED | **深**（5 模块逐个 assertEquals） |
| `testModuleOutOfOrderRejected` | `:63-76` | 跨序关账拒绝（AR=OPEN 直接关 AP → ERR_MODULE_OUT_OF_ORDER） | **深** |
| **`testReverseCloseApprovalBlocked`** | `:78-90` | **RC-1/RC-2 kill-switch 默认 required=true 阻断**（直接 CLOSED_FINAL 调 reverseClose → assertThrows + 状态保持 CLOSED_FINAL） | **深** |

> **报告校正项**（plan Current Baseline `:30` 称"反结账审批 kill-switch 默认阻断路径**无测试**"——**过时**）：`TestErpFinModuleCloseOrder#testReverseCloseApprovalBlocked:78-90` 显式覆盖默认 `reverse-close-approval-required=true` kill-switch 阻断路径（`@NopTestConfig` 无 `testConfigFile`，使用 yaml 默认值 true）。其余反结账行为测试（`TestErpFinReverseClose`/`TestErpFinPeriodCloseEndToEnd`/`TestErpFinPeriodStateMachine#testForwardAndReverse`）用 `period-close-end-to-end-test.yaml`/`period-close-state-machine-test.yaml` 显式置 false 跳过 kill-switch 测算账行为。

### 3.4 端到端（`TestErpFinPeriodCloseEndToEnd.java`，1 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testFullChain` | `:25-66` | 全链 preCheck（ unsettledArApCodes 列出）→ closePeriod（CLOSED + GL/AST 模块 CLOSED）→ FX-REVAL + PERIOD-CLOSE 凭证生成 → finalizePeriod（CLOSED_FINAL）→ reverseClose（OPEN）→ 重新 closePeriod（CLOSED + 新 PERIOD-CLOSE 凭证 count>=2） | **深**（状态 + 凭证 + 模块 status + 计数多重断言） |

### 3.5 反结账（`TestErpFinReverseClose.java`，1 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testReverseCloseRestoresBalance` | `:26-51` | RC-3 CLOSED_FINAL→OPEN + RC-4 结转凭证红冲（`hasReversalVoucher` count>=2）+ 收入科目余额恢复 + GL/AST 模块回开 + RC-8 重新结账 CLOSED | **深**（余额精确比较 + 凭证计数 + 模块状态） |

### 3.6 年度结转（`TestErpFinAnnualClose.java`，6 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testAnnualCloseTransferProfitToRetainedEarnings` | `:45-73` | 12月结账 → 本年利润清零 + 未分配利润累计 + PROFIT_TO_RETAINED_EARNINGS 凭证 + 次年 12 期间 + yearOpening | **深**（数值精确 + 期间计数 + 状态） |
| `testReverseCloseBlockedWhenNextYearExists` | `:76-85` | 年末反结账阻断（次年期间已存在 → assertThrows） | **深** |
| `testReverseCloseReversesAnnualVoucherWhenNoNextYear` | `:87-96` | 年度结转凭证生成（无次年场景） | **中** |
| `testGenerateNextYearPeriodsIdempotentThrows` | `:98-105` | 次年期间幂等：同年已存在抛错 | **深** |
| `testBankFxRevaluationForeignAccount` | `:107-133` | 银行存款外币重估（FX-REVAL 凭证） | **深** |
| `testBankFxRevaluationFunctionalAccountSkipped` | `:135-159` | 本位币账户不重估（FX-REVAL count=0） | **深** |

### 3.7 凭证锁（`TestErpFinVoucherPeriodLock.java`，4 @Test，深断言）

| 测试方法 | 文件:行 | 覆盖验收标准 | 断言强度 |
|---|---|---|---|
| `testPostVoucherBlockedWhenPeriodClosedFinal` | `:38-49` | CLOSED_FINAL 凭证锁（postVoucher → ERR_FIN_VOUCHER_PERIOD_LOCKED + 消息含 CLOSED_FINAL） | **深**（P1-MA2-021 修复验证） |
| `testPostVoucherBlockedWhenPeriodClosed` | `:51-60` | CLOSED 凭证锁 | **深** |
| `testReverseVoucherBlockedWhenPeriodClosedFinal` | `:62-71` | CLOSED_FINAL 红冲锁 | **深** |
| `testPostVoucherAllowedWhenPeriodOpen` | `:73-83` | OPEN 期间正常过账 | **深** |

> **⚠️ 测试覆盖缺口**：
> - **PC-3 AR/AP reminder 路径**（hasReminders 非 hasIssues）：`TestErpFinPeriodCloseEndToEnd#testFullChain:30-33` 仅断言 `unsettledArApCodes.size() >= 1`（检出），未断言 reminder 不阻断 closePeriod（reminder 路径无显式 hard-block-非触发断言）。
> - **坏账准备 shortfall hard block**：`populateAllowanceCheck` 无单测覆盖（`period-close-end-to-end-test.yaml` 显式 `bad-debt-allowance-gate-enabled: false`）。
> - **跨域悬挂阻断**（assets 折旧 / inventory 到岸成本 posted=false 悬挂）：`findUnresolvedDepreciationSchedules` / `findUnresolvedLandedCosts` 无单测（单域 finance 测试无 ast/inv dao impl，扫描 try/catch 跳过）。
> - **折旧/成本重算失败传播**（G3 rethrow）：`runDepreciation` / `recloseInvCosts` rethrow NopException 路径无单测（P1-MA4-004 resolved MR1，测试 follow-up P1-MA4-005 MR2）。
> - **RC-9 反结账审计轨迹**：功能缺失故零测试。

---

## 4. 运行时行为证据（L5，复用 MA2 + 本切片差异）

> 方法论 §去重协议：既有 MA2 报告已证实行为作为既有证据输入，本审计**不重新核实行为本身**，只补"需求契约↔行为"差异。

### 4.1 复用 A2.3 period-close E2E 审计（`docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md`，217 行，**closed**）

A2.3 多维 E2E 审计已证实：
- 期间结账全链路（preCheck → closePeriod → FX/PL 凭证 → finalizePeriod → reverseClose → re-close）行为正确
- 模块关账顺序 AR→AP→INV→AST→GL 守卫（乱序抛 ERR_MODULE_OUT_OF_ORDER）
- P&L 结转 + 逐凭证 FX 平衡 PASS
- **1 项 P0**（P0-MA2-016 FX 损益未结转，**resolved**）+ **6 项 P1**（P1-MA2-017/018/019/020/021/022 **全 resolved**）+ 3 项 P2 watch-only（P2-MA2-023/024/025）
- 建立 7 控制点

**本切片复核结论**：A2.3 已证实的 period-close 行为经 HEAD 代码复核**无回退、无升级**。R6.1 per-mutation Processor 拆分（pre-R6.1 单体 `ErpFinAccountingPeriodProcessor` 时代 → 6 per-mutation Processor + 共享 helper）后，行为语义保持一致（按逻辑核验，行号偏移见 §9.3）。

### 4.2 复用 A2.5b 期间/预算状态机审计（`docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`，529+ 行，**closed**）

A2.5b 状态机审查已证实：
- 5 态期间状态机（NEVER_OPENED/OPEN/CLOSING/CLOSED/CLOSED_FINAL）迁移守卫
- @BizMutation 事务回滚一致性
- **2 项 P1**（P1-MA2-033 NEVER_OPENED→OPEN 缺失，resolved / P1-MA2-034 carryForward 年初前置 budget 侧）+ 2 项 P2

### 4.3 复用 A4.1b finance 代码质量（`docs/audits/2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`，**closed**）

A4.1b 已审 `ErpFinAccountingPeriodProcessor` + `ProfitLossClosingService` + `AnnualCloseService`：
- **P1-MA4-004**（跨域异常吞噬，resolved R1.16）+ **P1-MA4-005**（测试有效性，resolved R2.10）+ P2-MA4-003
- 银行存款外币重估性能（P2-MA4-003a）watch-only

### 4.4 本切片需求视角差异（不重审行为，只补需求↔行为对照）

本审计相对 A2.3/A2.5b/A4.1b 的**需求视角增量**：
1. **UC-FIN-06 五前置条件 + UC-FIN-07 七反结账要求逐条 L1↔L3↔L5 对照**（A2.3/A2.5b 是行为/状态机视角，未做需求验收标准逐条对照）——§5.2 逐条裁决
2. **RC-9 反结账审计轨迹（操作人/原因）缺失**（A2.3/A2.5b/A4.1b 均未识别此缺口）——§5 候选缺口 P1-RC-006
3. **RC-1 高权限仅 kill-switch 无角色强制**（A2.3 P1-MA2-020 标 kill-switch 但未做需求契约对照；A3.6/A6.1/A6.2 P1-MA3-046 全域覆盖未单独点名 finance 反结账）——§5 候选缺口 RC-1 复用 P1-MA3-046
4. **PC-3 AR/AP reminder 偏离 L1 字面"拒绝"**（A2.3 P1-MA2-017 resolved 为分级重构 + owner doc 登记，本切片需求契约视角仍记分歧）——§5 候选缺口 P2-RC-006
5. **resolved finding HEAD 复核**（防"已 resolved 但代码回退"）——§6.2

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 的符合性结论）

### 5.1 UC-FIN-06 / UC-FIN-07 五级追踪矩阵（2 UC 各一行，12 验收标准逐条进入判读）

| UC 编号 | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---|---|---|---|---|---|---|
| **UC-FIN-06** | `use-cases.md:110` 期末结账前置门禁（验收标准逐字见 §1：PC-1 未过账凭证→拒绝 / PC-2 未审核凭证→拒绝 / PC-3 未核销 AR/AP[强制核销模式]→拒绝 / PC-4 资产未折旧→拒绝 / PC-5 成本未算→拒绝 / PC-6 全部通过→进入结账） | `period-close.md` §结账前置检查 `:25-58`（**设计参考**：PC-3 AR/AP 处理为"提示"非"拒绝" `:42-43`，与 L1 冲突；**以 L1 为准**，L2 推定已向实现妥协）/ §坏账准备充足性 `:11,45-49`（L2 增量）/ §期末结账步骤8步 `:60-111`（费用摊销 Non-Goal `:9`）/ §期间状态机 `:147-168` | `ErpFinAccountingPeriodProcessor.findUnpostedVoucherCodes:432-440`(PC-1) + `findUnsettledArApCodes:442-462`(PC-3 reminder) + `findUnresolvedPostingExceptionKeys:471-477`(悬挂兜底) + `findUnresolvedDepreciationSchedules:506-527`(PC-4) + `findUnresolvedLandedCosts:530-549`(PC-5) + `populateAllowanceCheck:99-118`(L2 增量) + `ClosePeriodProcessor.closePeriod:46-87`(PC-6 + 8步编排 + 模块顺序 AR→AP→INV→AST→GL `:67-71`) | `TestErpFinPeriodPreCheck#testPreCheckListsIssues:47-67`(PC-1+PC-3 检出深断言) / `#testBlockingCloseRejectsWithIssues:80-97`(PC-1 hard block assertThrows) / `TestErpFinPeriodStateMachine#testNonBlockingCloseWithIssues:95-110`(PC-1 config 反向) / `TestErpFinModuleCloseOrder#testModuleCloseOrder:47-61`(模块顺序 5 模块逐断言) | A2.3 period-close E2E（行为 PASS，FX/PL/模块顺序控制点已证实）+ A2.5b 状态机（5 态迁移守卫 PASS）；本切片差异见 §4.4（PC-3 偏离 L1） | **整体 P2**（PC-1/2/4/5/6 + L2 增量 接受；PC-3 偏离 L1 字面"拒绝"，P2-RC-006 watch-only，L2 已记录有意设计） |
| **UC-FIN-07** | `use-cases.md:129` 反结账（验收标准逐字见 §1：RC-1 高权限 / RC-2 审批 / RC-3 CLOSED_FINAL→OPEN / RC-4 冲销结转凭证 / RC-5 冲销折旧凭证 / RC-6 冲销成本凭证 / RC-7 解锁期间内单据 / RC-8 重新结账→CLOSED_FINAL / RC-9 全程审计[操作人/原因]） | `period-close.md` §反结账流程 `:170-228`（**设计参考**）/ §反结账约束 `:223-228`（"管理员+审批"要求）/ §已知简化—reverse-close approval `:321-325`（config kill-switch successor，documented simplification，P1-MA2-020/P1-MA3-036 resolved）/ §期间控制 `:147-168`（CLOSED_FINAL→OPEN config kill-switch 门控） | `ReverseCloseProcessor.reverseClose:22-58`(RC-1/2/3 + 凭证冲销编排) + `ErpFinAccountingPeriodProcessor.isReverseCloseApprovalRequired:653-656`(RC-1 kill-switch) + `ReverseCloseProcessor:42-43`(RC-4) + `:50-52`+`reverseDepreciation:238-263`(RC-5) + [RC-6 缺失] + `:55-56`+`reopenModules:278-284`(RC-7) + 幂等链(RC-8) + [RC-9 完全缺失：`IErpFinPeriodCloseBiz.reverseClose` 无 reason 参数 + ORM `:670-671` 仅 closedBy/closedAt 无反结账审计列 + 无 ReverseCloseLog 实体] | `TestErpFinModuleCloseOrder#testReverseCloseApprovalBlocked:78-90`(RC-1/RC-2 kill-switch 默认 true 阻断深断言) / `TestErpFinReverseClose#testReverseCloseRestoresBalance:26-51`(RC-3/4/7/8 余额恢复 + 凭证红冲 + 模块回开 + 重新结账深断言) / `TestErpFinPeriodCloseEndToEnd#testFullChain:25-66`(全链反结账 + re-close 深断言) / `TestErpFinPeriodStateMachine#testForwardAndReverse:47-68`(4 态正反向 + 重新结账) / `TestErpFinAnnualClose#testReverseCloseBlockedWhenNextYearExists:76-85`(年末反结账阻断) / [RC-9 功能缺失零测试] | A2.3 反结账行为（余额还原 + GL/AST 模块重开 + PL 红冲凭证存在 + re-close 已证实）+ A2.5b 状态机（CLOSED_FINAL→OPEN 迁移）；本切片差异见 §4.4（RC-1 仅 kill-switch / RC-2 缺失 / RC-6 缺失 / RC-9 缺失） | **整体 P1**（RC-1 复用 P1-MA3-046 + RC-2 接受 successor + RC-3/4/5/7/8 接受 + RC-6 P2-RC-007 Non-Goal + RC-9 缺失 P1-RC-006） |

### 5.2 逐验收标准符合性裁决

#### UC-FIN-06 期末结账前置门禁（6 验收标准）

| 验收标准 | L3 证据 | L4 证据 | 裁决 | 命中判据 |
|---|---|---|---|---|
| **PC-1**（未过账凭证 hard block） | `findUnpostedVoucherCodes:432-440` + `ClosePeriodProcessor:59-63` | `testPreCheckListsIssues:47-67` + `testBlockingCloseRejectsWithIssues:80-97` | **接受** | §2 接受 |
| **PC-2**（未审核凭证） | 归 PC-1（docStatus != POSTED 含 DRAFT/CANCELLED） | 同 PC-1 | **接受** | §2 接受 |
| **PC-3**（未核销 AR/AP → 拒绝） | `findUnsettledArApCodes:442-462` → `hasReminders()`（**非 hasIssues**）→ 不阻断 | `testPreCheckListsIssues:65-66`（仅检出断言，无 reminder-不阻断断言） | **P2**（偏离 L1 字面"拒绝"，L2 已记录有意设计） | §2 P2①（次要验收标准未完全满足，主路径[阻断模式 config=false 默认]OK 边界[reminder 模式]弱） |
| **PC-4**（资产未折旧） | `closeAssetModule:151-155` → `runDepreciation:179-200` + `findUnresolvedDepreciationSchedules:506-527` | `testModuleCloseOrder:47-61`（模块关账）+ 跨域悬挂扫描无单测 | **接受**（间接实现：auto-execute + 悬挂阻断） | §2 接受 |
| **PC-5**（成本未算） | `closeInvModule:144-148` → `recloseInvCosts:208-232` + `findUnresolvedLandedCosts:530-549` | 同 PC-4 | **接受**（间接实现：auto-execute + 悬挂阻断） | §2 接受 |
| **PC-6**（全部通过→进入结账） | `ClosePeriodProcessor.doClosePeriod:46-87`（preCheck 通过则按序关账 + 状态迁移） | `testForwardAndReverse:47-68` + `testFullChain:25-66` | **接受** | §2 接受 |

#### UC-FIN-07 反结账（7 验收标准，UC-FIN-07 拆分为 RC-1..RC-9 九条）

| 验收标准 | L3 证据 | L4 证据 | 裁决 | 命中判据 |
|---|---|---|---|---|
| **RC-1**（高权限） | `ReverseCloseProcessor:26-29` kill-switch + `isReverseCloseApprovalRequired:653-656` | `testReverseCloseApprovalBlocked:78-90`（kill-switch 默认 true 阻断） | **P1**（仅 kill-switch 无运行时角色强制） | §2 P1④（跨组件契约行为不一致：L1 要求"高权限"语义 = 角色守卫，实现 = config 切换）→ 复用 P1-MA3-046 |
| **RC-2**（审批） | 缺失（无审批 action） | 无（功能缺失） | **接受**（documented simplification successor） | §2 P2③（owner doc `period-close.md:321-325` 显式 documented simplification + successor 触发条件，P1-MA2-020/P1-MA3-036 resolved）|
| **RC-3**（CLOSED_FINAL→OPEN） | `ReverseCloseProcessor:39` | `testForwardAndReverse:47-68` + `testFullChain:55-58` | **接受** | §2 接受 |
| **RC-4**（冲销结转凭证） | `ReverseCloseProcessor:42-43` + `reverseCloseVoucher:553-562` | `testReverseCloseRestoresBalance:46-47`（PERIOD-CLOSE 凭证 count>=2）+ `testFullChain:64-65` | **接受** | §2 接受 |
| **RC-5**（冲销折旧凭证） | `ReverseCloseProcessor:50-52` + `reverseDepreciation:238-263` | 间接（`reverseCloseRestoresBalance` AST 模块回开断言） | **接受**（config-gated） | §2 接受 |
| **RC-6**（冲销成本凭证） | **缺失** | 无 | **P2**（Non-Goal） | §2 P2③（owner doc `period-close.md:9` 显式 Non-Goal——INV costing 无 finance 侧期间凭证可冲） |
| **RC-7**（解锁期间内单据） | `ReverseCloseProcessor:55-56` + `reopenModules:278-284` + 期间 OPEN 解凭证锁 | 间接（`testReverseCloseRestoresBalance:42-44` GL/AST 模块回开） | **接受**（间接实现） | §2 接受 |
| **RC-8**（重新结账→CLOSED_FINAL） | 幂等链 reverseClose→closePeriod→finalizePeriod | `testForwardAndReverse:66-67` + `testReverseCloseRestoresBalance:49-50` + `testFullChain:60-63` | **接受** | §2 接受 |
| **RC-9**（全程审计：操作人/原因） | **完全缺失**（无 reason 参数 / ORM `:670-671` 仅 closedBy/closedAt / 无 ReverseCloseLog 实体） | 无（功能缺失） | **P1** | §2 P1①（功能完全缺失——验收标准要求的"全程审计[操作人/原因]"零实现） |

### 5.3 候选缺口分级裁决

| 候选缺口 | 分级 | 命中判据 | 处置 |
|---|---|---|---|
| **RC-9 反结账审计轨迹缺失**（reverseClose 无 reason 参数 + ORM 无审计列 + 无 ReverseCloseLog 实体 + arm-index 无对应 finding） | **P1** | §2 P1①（功能完全缺失）+ §2 P1⑤（验收标准无断言） | 新建 **P1-RC-006**（与既有 finding 不同控制点——arm-index period-close 分区无"反结账审计轨迹"同控制点 finding）→ MR1（R1.0 展开为 RC-R1.n）；修复触及 ORM 结构变更（`ErpFinAccountingPeriod` 增 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 或 新增 `ErpFinReverseCloseLog` 实体）+ BizModel/Processor/契约面 reason 参数，**须 ask-first + 独立 plan-audit**（§5 ORM 结构变更类） |
| **RC-1 高权限无运行时角色强制**（仅 kill-switch，无 @BizAuth/@RolesAllowed） | **P1** | §2 P1④（跨组件契约行为不一致） | **复用 P1-MA3-046**（全域敏感动作零运行时权限保护 finding 的 finance 反结账投影，同根因同控制点——action-level RBAC 缺失，A6.1/A6.2 已确认）→ 追加 RC 交叉引用注记，不新建 |
| **PC-3 AR/AP reminder 偏离 L1 字面"拒绝"**（实现 reminder 非 hard block） | **P2** | §2 P2①（次要验收标准未完全满足，主路径[阻断模式]OK 边界[reminder 模式]弱；L2 owner doc 已记录有意设计 P1-MA2-017 resolved） | 新建 **P2-RC-006**（与 P1-MA2-017 不同控制点：P1-MA2-017 = 阻断分级重构+默认值对齐 doc↔code，本 finding = L1↔L2 字面冲突[L1"拒绝"vs L2"提示"]，按 §4 Q1 L1 为准仍记分歧；倾向接受——L2 已记录有意设计 + 强制核销模式 config 默认未启用）→ successor watch-only |
| **RC-6 反结账"成本凭证冲销"缺失**（INV costing 无 finance 侧期间凭证可冲） | **P2** | §2 P2③（owner doc 显式 Non-Goal 登记于 `period-close.md:9`） | 新建 **P2-RC-007**（与既有 finding 不同控制点：arm-index 无"反结账成本凭证冲销"同控制点 finding）→ successor watch-only |

**UC-FIN-06 整体裁决**：**P2**（PC-3 AR/AP reminder 偏离 L1 字面"拒绝"，L2 已记录有意设计，倾向接受 P2 watch-only；其余 5 验收标准 + L2 增量坏账缺口 hard block 接受）。

**UC-FIN-07 整体裁决**：**P1**（RC-9 反结账审计轨迹完全缺失 P1-RC-006 + RC-1 高权限无角色强制复用 P1-MA3-046；RC-2/RC-6 documented simplification/Non-Goal 接受；RC-3/4/5/7/8 接受）。

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

> 方法论 §7：产出 finding 前 grep arm-index 同域同控制点后裁决（禁止未经比对直接新建）。

### 6.1 finding 复用/新增裁决

| 候选 finding | grep 比对 | 裁决 | 依据 |
|---|---|---|---|
| **RC-9 反结账审计轨迹缺失** | 比对 arm-index period-close 分区（P0-MA2-016 FX 损益 / P1-MA2-017 阻断分级 / P1-MA2-018 年初余额 / P1-MA2-019 辅助账对账 / P1-MA2-020 反结账 kill-switch / P1-MA2-021 凭证锁 / P1-MA2-022 FX 重估 / P1-MA2-033 NEVER_OPENED / P1-MA3-036 doc↔code / P1-MA4-004/005）+ 全域 RC 分区（P1-RC-001..005 + P2-RC-001..005） | **新建 P1-RC-006** | 新控制点：反结账操作的审计轨迹（操作人/原因/时间）记录机制完全缺失。P1-MA2-020/P1-MA3-036 = 审批门控实现形态（kill-switch vs 审批流），P1-MA3-046 = action-level RBAC，均非审计轨迹记录机制；P1-MA4-004/005 = 异常吞咽/测试有效性。无同控制点 finding |
| **RC-1 高权限无角色强制** | 比对 P1-MA3-046（全域敏感动作零运行时权限保护，arm-index `:295` 显式列 finance reverseClose 为示例）+ P1-MA2-020（反结账 kill-switch doc↔code） | **复用 P1-MA3-046** | 同根因同控制点：action-level RBAC 缺失。P1-MA3-046 arm-index 描述已显式列「finance ... reverseClose[反结账 kill-switch] ... 仅 HTTP 登录屏障」，本切片为其 finance 反结账投影，追加 RC 交叉引用注记。P1-MA2-020 是审批流实现形态（kill-switch successor），不同控制点 |
| **PC-3 AR/AP reminder 偏离 L1** | 比对 P1-MA2-017（auto-post-on-close 阻断分级不一致，arm-index `:183` resolved 阻断分级重构——`hasIssues()` 排除未核销 AR-AP + 新增 `hasReminders()`） | **新建 P2-RC-006** | 不同控制点/不同维度：P1-MA2-017 = doc↔code 阻断分级 + 默认值 + allowance shortfall 独立硬阻断（audit-remediation 文本一致性视角），已 resolved；本 finding = L1↔L2 字面契约冲突（L1"强制核销模式→拒绝" vs L2"未核销=提示"），按 §4 Q1 L1 为准仍记分歧（需求契约视角）。同一代码站点但不同审计轴投影，倾向接受——L2 已记录有意设计 + 强制核销模式 config 默认未启用 |
| **RC-6 反结账成本凭证冲销缺失** | 比对 arm-index period-close 分区（无"反结账成本凭证冲销"同控制点 finding）+ P2-MA4-003a（银行存款重估全表扫描性能，不同控制点） | **新建 P2-RC-007** | 新控制点：反结账步骤5"成本凭证冲销"缺失。owner doc `period-close.md:9` 显式 Non-Goal（"BATCH/INDIVIDUAL/STANDARD/全月一次/LIFO 计价[inventory 域 successor]"——INV costing 无 finance 侧期间凭证可冲）。无同控制点 finding |

### 6.2 resolved finding HEAD 复核（防"已 resolved 但代码回退"）

> 方法论增强（plan Phase 1 item）：对 arm-index 标记 resolved 的 period-close finding 在当前 HEAD（`c1b775491`）代码实际落地逐条复核。**R6.1 per-mutation 拆分后行号偏移**，按逻辑而非行号核验。

| Finding | arm-index 状态 | HEAD 复核 | 结论 |
|---|---|---|---|
| **P0-MA2-016**（FX 损益未结转） | ✅ resolved (即时通道 plan 2026-07-27-1949) | `ProfitLossClosingService.java:88-91` 排除条件仅 `Objects.equals(bt, ErpFinBusinessType.PERIOD_CLOSE.name())`，**不排除 EXCHANGE_GAIN_LOSS**（汇兑损益正常结转至本年利润）+ 注释 `:75,88` 显式说明"汇兑重估凭证[EXCHANGE_GAIN_LOSS]的汇兑损益须结转" | **已落地** |
| **P1-MA2-017**（auto-post-on-close 阻断分级） | ✅ resolved (plan 2026-07-29-2322-3) | `PeriodPreCheckReport.hasIssues()` 排除未核销 AR-AP（仅 `!unpostedVoucherCodes.isEmpty() || !unresolvedPostingExceptionKeys.isEmpty()`）+ 新增 `hasReminders()`（`!unsettledArApCodes.isEmpty() || allowanceExcess>0`）+ 新增 `hasAllowanceShortfall()` + `ClosePeriodProcessor:52-56` allowance shortfall 独立硬阻断 + `:59-63` 未过账凭证/异常阻断（auto-post-on-close=false 默认） | **已落地** |
| **P1-MA2-018**（年初余额非累计） | ✅ resolved documented simplification | `period-close.md:309-314` §已知简化「年初余额非累计」标注 + successor 触发条件（GL 余额引擎） | **已落地**（documented simplification 仍 open successor） |
| **P1-MA2-019**（辅助账对账作用域） | ✅ resolved (plan 2026-07-29-2322-3) | `AnnualCloseService.sumArApOpenFunctional` 增年度过滤（businessDate 落在结账年度内）+ owner doc `period-close.md:316-319` 标注单年作用域对齐 + 累计余额对账 successor | **已落地**（documented simplification 残留） |
| **P1-MA2-020**（反结账 approval kill-switch） | ✅ resolved documented simplification | `period-close.md:321-325` §已知简化「反结账审批」标注 config kill-switch + 审批流 successor + `state-machine.md §已知限制：浏览器层 xwf 审批路径` | **已落地**（documented simplification，successor 仍 open） |
| **P1-MA2-021**（CLOSED_FINAL 凭证锁） | ✅ resolved (plan 2026-07-29-2322-3) | `ErpFinVoucherBizModel.assertPeriodNotLocked:177-195`（CLOSED/CLOSED_FINAL 抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED`）+ `postVoucher:90`/`reverseVoucher:107` 调用守卫 + `TestErpFinVoucherPeriodLock` 4 测试 | **已落地** |
| **P1-MA2-022**（FX 重估无前期 reversal） | ✅ resolved documented simplification | `period-close.md:331-336` §已知简化「FX 重估无前期 reversal」标注 + IAS 21 残留风险 + config-gated + successor 触发条件 | **已落地**（documented simplification 仍 open successor） |
| **P1-MA2-033**（NEVER_OPENED→OPEN） | ✅ resolved | `ErpFinAccountingPeriodOpenPeriodProcessor.openPeriod:18-24`（assertPeriodStatus[NEVER_OPENED] + setStatus[OPEN] + flush）+ `TestErpFinPeriodStateMachine#testOpenPeriodFromNeverOpened:112-122` + `#testOpenPeriodRejectsNonNeverOpened:124-134` | **已落地** |
| **P1-MA3-036**（反结账审批 doc↔code） | ✅ done (R2.5, plan 2026-07-31-0010-3) | `period-close.md:321-325` + `state-machine.md §已知限制：浏览器层 xwf 审批路径` 显式标注「当前 config kill-switch，审批流 successor」（doc↔code 对齐） | **已落地**（doc 对齐） |
| **P1-MA4-004**（跨域异常吞噬） | ✅ resolved (R1.16) | `ErpFinAccountingPeriodProcessor.runDepreciation:195-200`（catch NopException → LOG.error → **rethrow**）+ `recloseInvCosts:226-231`（同 rethrow）+ `reverseDepreciation:257-262`（同 rethrow）—— G3 配置错误/真实故障阻断结账，impl 未就绪 try/catch 容错跳过 | **已落地** |
| **P1-MA4-005**（测试有效性） | ✅ resolved (R2.10) | 测试套件齐备：`TestErpFinPeriodPreCheck`(3) + `TestErpFinPeriodStateMachine`(5) + `TestErpFinModuleCloseOrder`(3) + `TestErpFinPeriodCloseEndToEnd`(1) + `TestErpFinReverseClose`(1) + `TestErpFinAnnualClose`(6) + `TestErpFinVoucherPeriodLock`(4) + `TestErpFinPeriodClosePerf` | **已落地**（period-close 子范围） |

**resolved finding 复核结论**：11/11 已落地，无代码回退。3 项为 documented simplification（P1-MA2-018/020/022），successor 仍 open 但 doc 显式标注合规。

### 6.3 finding 双向可追溯

| Finding ID | 类型 | 目标 MR | 修复状态 |
|---|---|---|---|
| **P1-RC-006** | 新建（UC-FIN-07⑫ 反结账审计轨迹缺失） | MR1（R1.0 展开为 RC-R1.n） | todo（本审计仅登记，不实施修复；修复 = `IErpFinPeriodCloseBiz.reverseClose` 增 reason 参数 + `ErpFinAccountingPeriod` ORM 增 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列 或 新增 `ErpFinReverseCloseLog` 实体 + `ReverseCloseProcessor` 落库审计；**触及 ORM 结构变更 + 会计过账逻辑[反结账] 须 ask-first + 独立 plan-audit** §5） |
| **P1-MA3-046**（复用） | 既有（全域敏感动作零运行时权限保护） | MR2（done R2.7） | done（R2.7）—— 追加 RC 交叉引用注记（finance 反结账 RC-1 投影），状态不变 |
| **P2-RC-006** | 新建（PC-3 AR/AP reminder 偏离 L1） | successor watch-only（P2 登记不强制） | todo（修复 = 补充 owner doc `period-close.md §结账前置检查` 显式标注"L1 字面'强制核销模式→拒绝'当前实现为 reminder，强制核销模式 config 默认未启用" + 或实现 config-gated 强制核销模式 hard block；前者纯文档修复可自动执行，后者触及 BizModel 代码逻辑修复按 roadmap 预授权类目可自动执行，**不触发 §5 ask-first**） |
| **P2-RC-007** | 新建（RC-6 反结账成本凭证冲销缺失） | successor watch-only（P2 登记不强制） | todo（修复 = owner doc `period-close.md §反结账步骤5` 显式补 Non-Goal 注记"INV costing 无 finance 侧期间凭证可冲"——已在 `:9` 实现范围注记中登记，可在 §反结账流程补交叉引用；纯文档修复可自动执行，**不触发 §5 ask-first**） |

---

## 7. 静态存疑点清单（供 MA4 A4.1 运行时展开）

> 方法论 §6 段落 7：L5 无法静态定论、需运行时确认的点，每存疑点一行；无则注明"无"。

| # | 存疑点 | 触发条件 | 交 MA4 |
|---|---|---|---|
| 1 | **PC-3 AR/AP reminder 模式运行时行为**（auto-post-on-close=true 提示模式下，未核销 AR/AP 经 `hasReminders()` 列出但 closePeriod 不阻断——是否实际符合用户对"前置门禁"的期望） | 实际启用强制核销模式（未文档化 config） + 月末存在大额未核销 AR/AP 时结账 | A4.1 运行时验证（闭合 P2-RC-006 决策：保留 reminder 或升级 hard block） |
| 2 | **PC-4 资产折旧 auto-execute + 悬挂阻断交互**（`runDepreciation` G3 容错跳过[impl 未就绪]与 `findUnresolvedDepreciationSchedules` 悬挂阻断的交互——若 assets 域部署但折旧因配置错误失败，rethrow 阻断 vs 悬挂扫描是否双重报告） | assets 域部署 + 折旧配置错误（如 ERR_DEPRECIATION_RATE_MISSING） | A4.1 运行时探针 |
| 3 | **RC-9 反结账审计缺失的实际合规影响**（无操作人/原因/时间记录——外部审计/税务/SOX 合规场景下的可追溯性破坏程度；当前 `ErpFinAccountingPeriod` 通用 `updatedBy`/`updateTime` 审计列是否被反结账操作覆盖可作部分证据） | 实际反结账操作发生时（生产环境） | A4.1 运行时验证（确认 `updatedBy`/`updateTime` 是否被覆盖以提供降级审计证据，闭合 P1-RC-006 修复方案的优先级裁决） |
| 4 | **年末反结账阻断边界**（`ReverseCloseProcessor:32-36` 年末反结账阻断——若次年期间已手动删除但 ErpFinGlBalance/yearOpening 残留，反结账红冲年度结转凭证是否致次年年初余额与凭证不一致） | 12 月期间反结账（手动删次年期间后） | A4.1 运行时探针 |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`c1b775491`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本审计无生产代码变更**（纯审计报告），checker 无回归风险（actual 反映既有 HEAD 状态，非本审计引入）。

  | 规则 | 描述 | baseline（machine-readable） | actual（HEAD c1b775491） | delta | 说明 |
  |---|---|---|---|---|---|
  | R1a | dao().saveEntity (BizModel) | 0 | 0 | 0 | — |
  | R1b | dao().updateEntity (BizModel) | 0 | 0 | 0 | — |
  | R1c | dao().getEntityById (BizModel) | 0 | 0 | 0 | — |
  | R1d | dao().findAllByQuery (BizModel) | 14 | 14 | 0 | — |
  | R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 | — |
  | R2b | BizModel daoFor(Erp*) 跨域 | 240 | 229 | -11 | 既有改进（非本审计引入） |
  | R2c | 全生产代码 daoFor() 总量 | 1380 | 1382 | +2 | 既有漂移（非本审计引入；本审计无代码变更） |
  | R2d | Processor daoFor(ErpMd*) | 32 | 34 | +2 | 既有漂移（非本审计引入） |
  | R3 | new Erp*() 构造实体 | 5 | 5 | 0 | — |
  | R4 | extends RuntimeException | 0 | 0 | 0 | — |
  | R5 | @Inject private | 0 | 0 | 0 | — |
  | R6 | @Transactional in BizModel | 2 | 2 | 0 | — |
  | R7 | System.currentTimeMillis() | 0 | 0 | 0 | — |
  | R8 | Processor 无 xbiz 接线 | 0 | 0 | 0 | — |
  | R10 | REQUIRES_NEW 事务 | 6 | 6 | 0 | — |
  | R11 | Processor 重复状态判断方法 | 0 | 0 | 0 | — |
  | R12a | 共享内核 import ErpFinBusinessType | 69 | 69 | 0 | — |
  | R12b | 共享内核 import PostingEvent | 66 | 66 | 0 | — |
  | R12c | 共享内核 import AcctSchemaResolver | 40 | 40 | 0 | — |

  > **注**：R2b/R2c/R2d 的 actual vs top-table baseline 差异反映既有 HEAD 与 `compliance-baseline.md` 顶部人工表之间已知的漂移历史（多轮 MR1 重构的累计结果，已记录于 baseline 文件的增量注记段），**与本审计无关**（本审计无生产代码变更）。CI 门控以 `## BASELINE (machine-readable)` 块为准；本审计不触发 CI（无代码变更）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-006 + 复用 P1-MA3-046 + P2-RC-006 + P2-RC-007）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1），无未经比对直接新建的 finding。
- [x] **真相源未修改声明**（§9）：本审计未修改 product-scope / use-cases / period-close.md 需求契约段落。PC-3 AR/AP reminder 偏离 + RC-9 审计轨迹缺失 + RC-6 成本凭证冲销缺失记入报告（§5.3），不直改 L1/L2（§9 冻结条款）。

---

## 9. 与 MA2 报告差异增量声明

> 方法论 §去重协议 + §6 段落 9：声明复用既有 MA2 报告已证实行为，列明本切片只补的需求视角差异。

### 9.1 复用的 MA2/MA3/MA4 既有证据

- **`docs/audits/2026-07-27-1949-arm-ma2-period-close-e2e.md`（A2.3，217 行，closed）= THE period-close 审计**：多维 E2E。1 P0（P0-MA2-016 FX 损益未结转，resolved 即时通道）+ 6 P1（P1-MA2-017/018/019/020/021/022 全 resolved）+ 3 P2 watch-only（P2-MA2-023/024/025）。建立 7 控制点；模块结账顺序 + P&L 平衡 + 逐凭证 FX 平衡 PASS。本审计复用其已证实的 period-close 行为，不重跑。
- **`docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（A2.5b，529+ 行，closed）**：期间/预算状态机审查。2 P1（P1-MA2-033/034）+ 2 P2。@BizMutation 事务回滚一致性确认。
- **`docs/audits/2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md`（A4.1b，closed）**：finance 预算+AR-AP+坏账汇兑+成本+期间结账+GL 映射代码质量。P1-MA4-004（跨域异常吞噬，resolved R1.16）+ P1-MA4-005（测试有效性，resolved R2.10）+ P2-MA4-003。
- **`docs/audits/2026-07-28-1953-arm-ma3-owner-doc-vs-code-drift.md`（A3.3，closed）**：finance owner doc vs code drift。P1-MA3-036（reverse-close approval doc↔code，done R2.5）。
- **`docs/audits/2026-07-28-1953-arm-ma3-api-contract-consistency.md`（A3.6，closed）+ 2026-07-29-1410-arm-ma6-*（A6.1/A6.2，closed）**：P1-MA3-046（全域敏感动作零运行时权限保护，done R2.7）—— 本切片 RC-1 复用。

### 9.2 本切片需求视角差异增量（只补，不重审）

1. **UC-FIN-06 五前置条件 + UC-FIN-07 七反结账要求逐条 L1↔L3↔L4↔L5 五级追踪**（A2.3 是 E2E 行为视角，A2.5b 是状态机视角，均未做需求验收标准逐条对照）——§5.2 逐条裁决
2. **RC-9 反结账审计轨迹缺失**（A2.3/A2.5b/A4.1b 均未识别此缺口，arm-index 无对应控制点 finding）——§5.3 新建 P1-RC-006
3. **RC-1 高权限仅 kill-switch 无角色强制的需求契约对照**（A2.3 P1-MA2-020 标 kill-switch 但未做 L1 契约对照；A3.6/A6.1/A6.2 P1-MA3-046 全域覆盖未单独点名 finance 反结账）——§5.3 复用 P1-MA3-046 + 追加 RC 交叉引用
4. **PC-3 AR/AP reminder 偏离 L1 字面"拒绝"**（A2.3 P1-MA2-017 resolved 为阻断分级重构 + owner doc 登记，但 L1↔L2 字面契约冲突未消除）——§5.3 新建 P2-RC-006
5. **RC-6 反结账"成本凭证冲销"缺失**（A2.3 未单独识别，owner doc `:9` 实现范围注记 Non-Goal）——§5.3 新建 P2-RC-007
6. **resolved finding HEAD 复核**（11 个 period-close resolved finding 在 HEAD `c1b775491` 实际落地复核，防"已 resolved 但代码回退"）——§6.2 结论 11/11 已落地
7. **报告校正项**：plan Current Baseline `:30` 称"反结账审批 kill-switch 默认阻断路径**无测试**"——**已过时**，`TestErpFinModuleCloseOrder#testReverseCloseApprovalBlocked:78-90` 显式覆盖默认 `reverse-close-approval-required=true` 阻断路径（深断言 assertThrows + 状态保持 CLOSED_FINAL）。本审计据实校正。

### 9.3 R6.1 行号偏移说明

A2.3/A2.5b 报告产出于 pre-R6.1 单体 `ErpFinAccountingPeriodProcessor` 时代（行号如 `:278-281`,`:681-684`,`:701-704`）。R6.1 per-mutation 拆分后：
- Facade `ErpFinAccountingPeriodBizModel`（85 行）仅入口/事务/委托
- 6 per-mutation Processor：`PreCheckProcessor`(27) / `ClosePeriodProcessor`(115) / `FinalizePeriodProcessor`(25) / `ReverseCloseProcessor`(60) / `OpenPeriodProcessor`(25) / `GenerateNextYearPeriodsProcessor`(83)
- 共享 helper `ErpFinAccountingPeriodProcessor`（726 行）保留 protected step 方法

本审计所有 L3 行号引用按当前 HEAD `c1b775491` 实测，按逻辑（方法名 + 行为语义）核验 resolved finding 落地状态，不依赖 pre-R6.1 行号。

### 9.4 不重审维度（§去重协议）

- **不重跑 A2.3 period-close E2E 行为审计**（已 closed，行为直接引用）
- **不重跑 A2.5b 期间状态机审查**（已 closed，状态机迁移守卫直接引用）
- **不重审 A4.1b 代码质量维度**（已 closed，代码质量缺陷[P1-MA4-004/005]已 resolved）
- **不重审 A3.3 owner-doc drift 文本一致性**（已 closed，P1-MA3-036 已 done）
- **不重审 A3.6/A6.1/A6.2 权限注解完整性**（已 closed，P1-MA3-046 已 done R2.7——本切片复用）
- **不复跑 MA1-MA7 架构漂移类审计**（以 audit-remediation 收口为准）

---

## §自检清单（报告产出前强制，方法论 §6 段落完整性自检）

- [x] §1 需求契约原文（UC-FIN-06 五前置条件 + UC-FIN-07 七反结账要求，12 验收标准完整枚举，逐字引用）
- [x] §2 实现证据（L3 `file:line`，含 Facade + 6 per-mutation Processor + 共享 helper 调用链 + 跨域 finance→assets/inventory 链列全）
- [x] §3 测试证据（L4，注明断言强度，7 测试文件覆盖矩阵 + 测试覆盖缺口）
- [x] §4 运行时行为证据（L5，复用 A2.3/A2.5b/A4.1b + 本切片差异）
- [x] §5 符合性结论（五级追踪矩阵 2 UC 行 + 12 验收标准逐条裁决 + 候选缺口分级）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + 11 个 resolved finding HEAD 复核 + 双向可追溯）
- [x] §7 静态存疑点清单（4 项交 MA4 A4.1）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重 + 真相源未修改声明）
- [x] §9 与 MA2 报告差异增量声明（复用证据 + 需求视角差异 + R6.1 行号偏移说明 + 不重审维度）

**9 段完整性自检结论**：§1-§9 全部存在，无缺失。
