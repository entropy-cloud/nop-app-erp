# rc-ma4 A4.2.63-A4.2.73 assets 折旧/处置/资本化运行时确认审计报告

> Report Status: done
> Mission: requirement-compliance（MA4 扩展域展开器运行时确认）
> Work Item: A4.2.63 / A4.2.64 / A4.2.65 / A4.2.66 / A4.2.67 / A4.2.68 / A4.2.69 / A4.2.70 / A4.2.71 / A4.2.72 / A4.2.73（11 项 A1.22/A1.23/A1.24 §7 静态存疑点运行时确认）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 四级分级判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §去重协议）
> 计划：`docs/plans/2026-08-07-2345-1-rc-ma4-a4-2-63-73-assets-depreciation-disposal-capitalization-runtime.md`
> Source Audits: `docs/audits/2026-08-03-0900-2-rc-ma1-a1-22-assets-f1-depreciation-engine.md`（A1.22 §7 SP-1..SP-4）/ `docs/audits/2026-08-03-0900-3-rc-ma1-a1-23-assets-f2-disposal.md`（A1.23 §7 SP-1..SP-3）/ `docs/audits/2026-08-03-1200-1-rc-ma1-a1-24-assets-f3-capitalization-idle-cip-inventory-maintenance-splitmerge-dashboard.md`（A1.24 §7 SP-1/SP-2/SP-3/SP-5）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 全部工作项指定）
> 审计类型：**只读运行时确认（零生产代码/ORM/api.xml/view.xml/真相源变更）**

---

## 0. 业财保护区域探针纪律声明（前置段）

本报告三项存疑点触及业财保护区域探针——**READ-ONLY 确认，不改折旧/过账/处置凭证逻辑**：

- **A4.2.64（方式B 补提；P1-RC-029）**：仅 grep census + `executeDepreciation` elapsed 语义追踪确认"补提 mutation 不存在 + 单月语义"，不改 `ExecuteDepreciationProcessor` / `DepreciationCalculator`。修复义务归 MR1（触折旧重算 + ORM 结构变更须 ask-first + 独立 plan-audit，roadmap §横切关注点 #5）。
- **A4.2.67（出售补提；reuse P1-RC-029）**：仅 grep census + `executeApprove`/`buildEvent` 路径追踪确认"处置路径从不调 executeDepreciation + 读陈旧 accumulatedDepreciation"，不改 `ErpAstDisposalProcessor`。修复义务归 MR1（与方式B 补提能力修复协同）。
- **A4.2.68（处置凭证科目腿；P1-RC-030）**：仅 `DisposalAcctDocProvider.createFacts` 行级科目追踪 + GL 借贷平衡推导，不改 `DisposalAcctDocProvider` / `VoucherFact`。修复义务归 MR1（触会计过账逻辑核心路径须 ask-first + 独立 plan-audit §5）。

本审计**维持既有分级不撤销**（A4.2.64/A4.2.67 维持 P1-RC-029 P1 / A4.2.68 维持 P1-RC-030 P1 / A4.2.69 维持 P1-MA2-060 resolved R1.16），仅记录运行时证据。运行时未发现会计错误已活跃（GL 借贷平衡未破坏），**不触发 MR0**。

---

## 1. 运行时证据与裁决（11 项逐项）

### A4.2.63 — 方式A 反结账补提 3 步链编排可达性（A1.22 SP-1）

- **静态判定**：各操作存在（`finance.reverseClose` + `ast.executeDepreciation` + `finance.closePeriod`）但 assets 域零 reverseClose 调用（编排缺口属操作便利性，能力存在非能力缺失）。
- **运行时证据**：
  - grep `reverseClose` 全 `module-assets` → **0 命中**（census 确认 assets 域无编排）。
  - 3 步各操作技术可达：`IErpFinAccountingPeriodBiz.reverseClose`（finance 侧，A1.22 §2 证实）+ `ErpAstDepreciationScheduleExecuteDepreciationProcessor.executeDepreciation:38-121`（接受任意 `period` 参数，无前置必须等于当前期间校验 `:38-41`，期间 OPEN 即可触发）+ `IErpFinAccountingPeriodBiz.closePeriod`（finance 侧）。
  - `requirePeriodOpen:76-87`（`ErpAstDepreciationScheduleProcessor`）仅拒绝非 OPEN 期间——反结账后期间回 OPEN，`executeDepreciation` 可成功。
- **裁决**：**倾向 watch-only 不单列 finding**（与 A1.22 §5 一致）。3 步链各步技术可达（A2.10 §场景(e) PASS "两条路径技术上可达"），手动编排可达闭合；编排缺口属 P2 watch-only 操作便利性，**能力存在非能力缺失**。无运行时升级证据。

### A4.2.64 — 方式B 补提多漏提期累计折旧偏差（A1.22 SP-2；P1-RC-029）⚠️ READ-ONLY

- **静态判定**：完全缺失（grep catchUp/backfill 生产代码 0 匹配[仅 ErpAstErrors:96 消息串]；executeDepreciation 每次 Calculator 单月计算 `elapsed=countExecuted`）。
- **运行时证据**：
  - grep `catchUp|backfill|catchUpDepreciation|补提|depreciateTo|preDisposal` 全 `module-assets` → **仅 2 命中**：`ErpAstErrors.java:96`（错误消息串"折旧期间 {period} 已结账，不允许补提折旧"——仅拒绝消息无逻辑）+ `TestErpAstDepreciation.java:177`（注释串）。**零生产逻辑匹配**——catchUpDepreciation mutation 不存在。
  - `ErpAstDepreciationScheduleExecuteDepreciationProcessor:67` `int elapsed = facade.countExecuted(assetId) - (wasExecuted ? 1 : 0);`——**单月语义确认**：elapsed 来自 EXECUTED 行计数（`ErpAstDepreciationScheduleProcessor.countExecuted:126-131`），每次 Calculator 调用（`:72-73`）仅计算单月折旧额，无多月补提循环。
  - 数值推导：2026-05 漏提 + 2026-07 调用 `executeDepreciation(assetId, "2026-07")` → elapsed = 已 EXECUTED 月数（不含 2026-05），仅产生 2026-07 单月折旧额，**不产生 2026-05+06 漏提额累计**。
- **裁决**：**维持 P1-RC-029 P1**（§2 P1① 功能完全缺失 + §5 Q4 会计正确性类无例外——漏提额不补致累计折旧低估/净值高估/折旧费用低估；修复归 MR1 触折旧重算 + ORM 结构变更须 ask-first + 独立 plan-audit）。运行时确认补提 mutation 缺失 + 单月语义，**维持分级**。

### A4.2.65 — 批量隔离 GL 科目缺失场景跳过行为（A1.22 SP-3）

- **静态判定**：已实现（`ExecuteBatchDepreciationProcessor:37-55` per-asset try/catch 隔离 + 失败告警 `dispatchFailureAlert`）。
- **运行时证据**：
  - `ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor:46-53` per-asset try/catch：`for (asset : assets) { try { executeDepreciationProcessor.executeDepreciation(...); processed++; } catch (Exception e) { LOG.warn("批量折旧：资产 {} 期间 {} 计提失败，跳过", ...); } }`——GL 科目缺失资产（`DepreciationAcctDocProvider` 解析失败 → `postingDispatcher.tryPost` 抛 NopException → `executeDepreciation:102-108` 非唯一约束异常向上抛）被独立 try/catch 捕获跳过，`processed` 不递增，循环继续处理其他资产。
  - `DepreciationPostingDispatcher.dispatchFailureAlert:74-92` 失败告警派发（`IErpSysNotificationBiz.notify("ast.depreciation-posting-failure", ...)` `:87`，event key `ast.depreciation-posting-failure` `:46`）。
- **裁决**：**主路径行为正确闭合**（与 A1.22 §5 接受一致）。GL 科目缺失资产独立 try/catch 跳过不影响其他资产 processed，失败告警派发闭环（R1.16 resolved P1-MA4-013 告警部分）。

### A4.2.66 — 补提凭证显式"补提"marker（A1.22 SP-4）

- **静态判定**：部分满足（`buildEvent:119-121` voucherDate=businessDate + billHeadCode=assetCode#period 期间携带可追溯，无显式"补提"marker）。
- **运行时证据**：
  - `DepreciationPostingDispatcher.buildEvent:119-121` `voucherDate = schedule.getBusinessDate() != null ? schedule.getBusinessDate() : CoreMetrics.today()`——voucherDate = schedule.businessDate（= `periodFirstDay(period)` `ErpAstDepreciationScheduleExecuteDepreciationProcessor:85`）。
  - `buildEvent:114` `event.setBillHeadCode(billHeadCode(asset.getCode(), schedule.getPeriod()))` + `billHeadCode:137-139` `return assetCode + "#" + period`——期间标签携带。
  - 无显式"补提"marker：grep voucherType/remark/"补提" 标注于 `DepreciationPostingDispatcher` → **零命中**。审计维度仅能凭 voucherDate（= schedule.businessDate）+ billHeadCode（= assetCode#period）按期间反查识别补提凭证。
- **裁决**：**倾向 watch-only**（与 A1.22 §5 接受一致——期间归属可审计）。补提凭证期间归属可追溯（voucherDate + billHeadCode period 标签），满足 L1 `use-cases.md:124` "补提凭证标注所属期间（审计）"基本语义；显式"补提"marker 缺失属审计维度弱化，记入观察，不单列 finding。

### A4.2.67 — 出售补提缺失运行时会计影响量化（A1.23 SP-1；reuse P1-RC-029）⚠️ READ-ONLY

- **静态判定**：完全缺失（`executeApprove:62-101` 从不调 executeDepreciation + grep catchUp/补提/depreciateTo 0 命中 + buildEvent 读陈旧 accumulatedDepreciation）。
- **运行时证据**：
  - grep `catchUp|补提|depreciateTo|preDisposal|saleDate` 于处置 4 文件（`ErpAstDisposalProcessor` / `ErpAstDisposalApproveProcessor` / `DisposalPostingDispatcher` / `DisposalAcctDocProvider`）→ **0 生产匹配**（仅 `ErpAstErrors.java:96` 拒绝消息串）。
  - `ErpAstDisposalProcessor.executeApprove:62-101`：`:66-67` `accumDep = nz(asset.getAccumulatedDepreciation())`（陈旧上期末账面值）→ `:68` `nbv = original.subtract(accumDep)` → `:70` `gainLoss = disposalAmount.subtract(nbv)`；`:79` `cancelPendingSchedules(asset.getId())` 仅取消 PENDING 未来计划（`cancelPendingSchedules:202-211` 仅 setStatus CANCELLED），**从不跑当期折旧**；全程**从不调用 `IErpAstDepreciationScheduleBiz.executeDepreciation`**。
  - `DisposalPostingDispatcher.buildEvent:110-114`：`accumDep = nz(asset.getAccumulatedDepreciation())`（同陈旧值）→ `nbv = original.subtract(accumDep)` → `gainLoss = disposalAmount.subtract(nbv)`——读陈旧 accumulatedDepreciation 重算入 BILL_DATA_GAIN_LOSS。
  - 数值推导（月中出售）：资产 original=12000/residual=0/10 月寿命/直线法月折旧 100，2026-06 末 accumDep=600（6 月），2026-07-15 出售（disposalAmount=5000）。当前实现 gainLoss = 5000 − (12000 − 600) = −6500；若补提至 7 月 15 日（半月 50），accumDep=650，gainLoss = 5000 − (12000 − 650) = −6350。**偏差 = 150**（半月折旧 50 × 3）。GL 6301/6711 金额错报，但 GL 借贷平衡**不破坏**（Dr Σ = Cr Σ，仅金额错报）。
- **裁决**：**维持 reuse P1-RC-029 P1**（同根因[补提 mutation catchUpDepreciation 不存在]下游投影，§2 P1① + §5 Q4 会计正确性类无例外；修复归 MR1 与方式B 补提能力修复协同：catchUpDepreciation mutation 实现后 `executeApprove:62-101` 须在损益计算 `:66-70` 前调用它重算 accumulatedDepreciation 至出售日）。运行时确认处置路径从不调补提 + 读陈旧值，**维持分级**。

### A4.2.68 — 处置凭证不同 disposalType 行级结构（A1.23 SP-2；P1-RC-030）⚠️ READ-ONLY

- **静态判定**：完全缺失（`DisposalAcctDocProvider:69-85` 仅 1602/1002/6711\|6301/1601 四类科目无 1606"固定资产清理"中间科目腿；§十:346 Deferred 仅覆盖类型命名未覆盖科目腿合并）。
- **运行时证据**：
  - `DisposalAcctDocProvider:34-38` 常量定义：`SUBJECT_FIXED_ASSET="1601"` / `SUBJECT_ACCUM_DEPRE="1602"` / `SUBJECT_BANK_DEPOSIT="1002"` / `SUBJECT_DISPOSAL_LOSS="6711"` / `SUBJECT_DISPOSAL_GAIN="6301"`——**无 1606"固定资产清理"常量**。
  - `createFacts:55-86` 行级结构（SCRAPPED/SOLD ±gainLoss 四组合）：
    - 借 1602 累计折旧（accumDep≠0）`:70-72`
    - 借 1002 银行存款（disposalAmount>0）`:74-76`
    - gainLoss>0 贷 6301 营业外收入 `:78-79` / gainLoss<0 借 6711 营业外支出 negate `:80-82`
    - 贷 1601 固定资产（恒定）`:84`
  - **GL 借贷平衡推导**（四组合均 Dr Σ = Cr Σ）：
    - SCRAPPED（disposalAmount=0，loss）：Dr = accumDep + |gainLoss| = accumDep + nbv = original = Cr ✓
    - SOLD gain（disposalAmount>nbv）：Dr = accumDep + disposalAmount；Cr = gainLoss + original = (disposalAmount − nbv) + original = disposalAmount + accumDep = Dr ✓
    - SOLD loss（0<disposalAmount<nbv）：Dr = accumDep + disposalAmount + |gainLoss| = accumDep + nbv = original = Cr ✓
  - **关键澄清**：GL 借贷平衡**不破坏**（合并凭证与两步流凭证对 1601/1602/1002/6711/6301 的最终余额影响完全相同——固定资产清理腿在两步流中网为零），仅凭证**结构/审计轨迹**偏离 L1:72-74 字面（缺失 1606 过渡科目中间审计轨迹）。
- **裁决**：**维持 P1-RC-030 P1**（§4 三判据不满足→重开：§十:346 Deferred 仅覆盖类型命名不覆盖科目腿合并 + owner doc §三含腿 vs §一/实现无内部不一致 + 无独立 plan-audit + product-scope 未裁剪 → 非 documented simplification → 重开 P1；修复归 MR1 触 DisposalAcctDocProvider/VoucherFact 核心路径须 ask-first + 独立 plan-audit §5）。运行时确认无 1606 中间科目腿 + GL 平衡不破坏仅凭证结构偏离，**维持分级**。

### A4.2.69 — posted=false 窗口 reverseApprove 实际行为（A1.23 SP-3；P1-MA2-060 resolved R1.16）

- **静态判定**：resolved 落地（`DisposalPostingDispatcher.dispatchFailureAlert:67-83` 告警派发 + DeferredPostingSweepJob 兜底；reverseApprove 不对称 deliberate Phase 3 方案 B documented）。
- **运行时证据**：
  - `ErpAstDisposalProcessor.executeReverseApprove:111-129`：`:112` `if (Boolean.TRUE.equals(disposal.getPosted()))` gated by posted==true——仅 posted=true 时 `:113` postingDispatcher.reverse + `:116` asset→IN_SERVICE + `:119` restoreCancelledSchedules + `:121-124` 清 posted/gainLoss；posted=false 窗口仅 `:126` `disposal.setApproveStatus(REJECTED)`，**资产保持 SCRAPPED/SOLD 终态 + schedules 保持 CANCELLED**（不对称 deliberate）。
  - `DisposalPostingDispatcher.dispatchFailureAlert:67-83`：tryPost 失败时（`:61` 调用）派发 `IErpSysNotificationBiz.notify("ast.disposal-posting-failure", ctx, serviceCtx)`（`:78`，event key `ast.disposal-posting-failure` `:45`），通知失败降级 warn 不阻断（`:79-82`）——**告警派发已落地**（R1.16 resolved P1-MA4-013/060 告警部分）。
  - DeferredPostingSweepJob（`ErpFinDeferredPostingRetryHelper`，finance 域）兜底重试存在（A2.10/A4.3 证实）。
- **裁决**：**维持 P1-MA2-060 resolved R1.16**（RC 视角不重开，§去重协议——MA2 已裁决行为维度；§4 (i)/(ii) 满足：R1.16 plan-audit 存在 + state-machine.md §实现约定:68-70 显式 deliberate 不对称 Phase 3 方案 B documented）。运行时确认告警+sweep+reverseApprove deliberate 不对称 documented，**维持 resolved**。

### A4.2.70 — UC-AST-01 资本化折旧计划末期残值修正取值行为（A1.24 SP-1）

- **静态判定**：直线法末期补差非整除月数边界（Calculator 末期补差逻辑）。
- **运行时证据**：
  - `ErpAstAssetCapitalizationProcessor.generateDepreciationSchedule:206-241` + `plannedAmount:243-253`：直线法末期（`monthIndex == months - 1` `:246`）`return original.subtract(residual).subtract(beforeLast)`（`:247-248`，`beforeLast = straightMonthly × (months-1)`）——**末期补差**：末期折旧额 = (original − residual) − 前 N-1 月累计，确保 Σ planned = original − residual 精确（非整除月数边界收敛）。
  - `DepreciationCalculator.calculate:63-74`：直线法 `:65` `(original − residual) / months` SCALE=4 HALF_UP；残值约束双重兜底 `:71-73`（`if (nbv − amount < residual) amount = nbv − residual`——末期补差不破坏余额守恒）+ `:74` 负 clamp ZERO。
  - A1.22 §3 强测试 `testStraightLinePerPeriodEqualAndLastToResidual`（12 期 each=1000 + Σ=12000 + 末期净值=ZERO）+ `testNonZeroResidualStraightLineClampsToResidual`（残值=2000 截断 + 末期净值精确=残值 2000）+ `PropertyErpAstDepreciationResidual`（100 tries 属性测试）三重覆盖。
- **裁决**：**主路径行为正确闭合**。直线法末期残值修正经 schedule 生成末期补差 + Calculator 残值约束双重兜底，末期净值精确收敛到残值（非整除月数边界余额守恒），测试强覆盖。

### A4.2.71 — UC-AST-10 维修资本化重算后折旧计划 PENDING→EXECUTED 迁移（A1.24 SP-2）

- **静态判定**：已实现（`recalculateForCapitalizationMaintenance:96` 重算折旧计划）。
- **运行时证据**：
  - `ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor:35-91`：`:45` `executedMonths = facade.countExecuted(assetId)` + `:46` `remainingMonths = totalMonths - executedMonths`；`:50-54` 删除 PENDING 计划（仅删 PENDING，**EXECUTED 保留**）；`:57-89` 重生成 remainingMonths 个 PENDING 计划（`:84` setStatus PENDING）；末期（`i == remainingMonths - 1` `:72`）`planned = depreciableBase − monthly × (remainingMonths − 1)`（`:72-73`，末期补差确保 Σ planned = depreciableBase）。
  - 重算后状态一致性：已 EXECUTED 计划累计折旧结转保留 + 新 PENDING 计划按剩余月数重生成，后续 `executeDepreciation`（`findSchedule:117-124` + `countExecuted:126-131`）正确消费新 PENDING → EXECUTED。
  - `depreciableBase = original + increment − residual − accumulated`（`:58`）——加性扩展（资本化金额增量入折旧基数），15 @Test 强覆盖（A1.24 §3）。
- **裁决**：**主路径行为正确闭合**。PENDING→EXECUTED 迁移语义正确（EXECUTED 保留 + PENDING 重生成），重算后 schedule 状态一致，末期补差余额守恒。

### A4.2.72 — UC-AST-11 拆分 proportion tolerance 极端比例平衡行为（A1.24 SP-3）

- **静态判定**：已实现（`SplitProcessor` PROPORTION_TOLERANCE + max-item residual fix + reverse 抛 NOT_SUPPORTED）。
- **运行时证据**：
  - `ErpAstSplitProcessor:48-49` `PROPORTION_TOLERANCE = new BigDecimal("0.000001")` + `AMOUNT_TOLERANCE = new BigDecimal("0.01")`。
  - `:238-244` Σ 比例平衡校验：`if (sumProportion != 0 && |sumProportion − 1| > PROPORTION_TOLERANCE) throw ERR_AST_SPLIT_PROPORTION_NOT_BALANCED`——3+ 目标比例和=1.000001 时 |1.000001−1|=0.000001 **不大于** 0.000001 → 通过容差（边界值合法）。
  - `computeAllocation:267-316` PROPORTIONAL 模式：逐行 `orig = sourceOriginal × prop` setScale HALF_UP（`:290`）+ `applyRemainder:322-333` max-item 补差（DOWN_UP 默认模式 `:328-331` maxPropIndex 行 += remainder）——确保 Σ 新卡片原值 == 源原值精确（残差归最大项）；累计折旧同理（`applyRemainderForAccumDep:335-345`）。
  - `createTargetAssets:349+` 新卡片原值 = `line.getOriginalCostAmount()`（`:361`，经补差后），Σ 精确平衡。
- **裁决**：**主路径行为正确闭合**（与 A1.24 §5 接受一致）。3+ 目标比例和=1.000001 边界通过容差校验 + max-item residual fix 确保 Σ 新卡片原值 == 源原值（AMOUNT_TOLERANCE 内，DOWN_UP 模式精确），8 @Test 强覆盖（A1.24 §3）。

### A4.2.73 — UC-AST-09 盘亏 SCRAPPED 资产折旧计划 CANCELLED 同步触发（A1.24 SP-5）

- **静态判定**：已实现（`ErpAstInventoryProcessor` 直接 SCRAPPED + 折旧计划 CANCELLED 联动）。
- **运行时证据**：
  - `ErpAstInventoryProcessor.handleShortageTriggerDisposal:242-272`：盘亏资产 `:270` `asset.setStatus(ErpAstDepreciationScheduleProcessor... ASSET_STATUS_SCRAPPED)` 直接置 SCRAPPED 终态——**不显式调 `cancelPendingSchedules`**（区别于处置链 `ErpAstDisposalProcessor.cancelPendingSchedules:79,202-211`）。grep `cancelPendingSchedules|cancelSchedule|setStatus(...CANCELLED)` 于 `ErpAstInventoryProcessor` → **0 CANCELLED 联动调用**（仅 `:83` inv CANCELLED 是盘点单状态非折旧计划）。
  - **净效果实现（不重复计提）**：`ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor:42` `q.addFilter(eq("status", ASSET_STATUS_IN_SERVICE))` 批量折旧仅查 IN_SERVICE（SCRAPPED 排除）+ `ErpAstDepreciationScheduleProcessor.validateAssetInService:68-74` executeDepreciation 拒绝非 IN_SERVICE——盘亏 SCRAPPED 资产后续 `executeBatchDepreciation`/`executeDepreciation` **被跳过/拒绝**，不重复计提折旧。
  - PENDING 折旧计划行未显式 CANCELLED（orphaned PENDING rows）——属 P2-RC-028（盘点盘盈/盘亏完整处置链复用收窄 owner doc §四/§八 documented simplification）watch-only 已登记范围。
- **裁决**：**主路径行为正确闭合**（与 A1.24 §5 接受 on ②⑤ + P2-RC-028 净效果实现一致）。盘亏 SCRAPPED 资产后续折旧不重复计提（折旧引擎 IN_SERVICE 过滤兜底），PENDING 行 orphaned 属 P2-RC-028 watch-only documented simplification 范围，不单列新 finding。

---

## 2. 与既有 finding 衔接（维持注记，无未经比对新建）

| Finding ID | 域 | 运行时确认结论 | 维持/变更 |
|-----------|---|--------------|----------|
| `P1-RC-029`（方式B 补提 + 出售补提 reuse） | assets | A4.2.64 确认补提 mutation 缺失 + elapsed 单月语义；A4.2.67 确认处置路径从不调补提 + 读陈旧 accumulatedDepreciation。GL 借贷平衡不破坏仅金额错报。 | **维持 P1**（Q4 会计正确性类无例外；修复归 MR1 触折旧重算 + ORM 结构变更 + 处置路径接线须 ask-first + 独立 plan-audit §5） |
| `P1-RC-030`（处置凭证科目腿） | assets | A4.2.68 确认无 1606 中间科目腿（仅 1602/1002/6711\|6301/1601）+ GL 平衡不破坏仅凭证结构偏离 L1:72-74。 | **维持 P1**（§4 三判据不满足→重开；修复归 MR1 触 DisposalAcctDocProvider/VoucherFact 须 ask-first + 独立 plan-audit §5） |
| `P1-MA2-060`（Cap/Disposal tryPost 吞异常 + reverseApprove 不对称） | assets | A4.2.69 确认 `dispatchFailureAlert:67-83` 告警派发已落地（R1.16）+ DeferredPostingSweepJob 兜底 + reverseApprove 仅 posted=true 回滚资产（deliberate Phase 3 方案 B documented）。 | **维持 resolved R1.16**（RC 视角不重开，§去重协议——MA2 已裁决行为维度；§4 (i)/(ii) 满足） |
| `P2-RC-025`（失败资产独立重试 API） | assets | A4.2.65 间接确认手动幂等可达（批量隔离+告警闭环），dedicated retry API 仍缺失。 | **维持 P2**（登记不强制） |
| `P2-RC-026`（汇总成功凭证 Deferred） | assets | A4.2.65 间接确认每资产单张凭证，无聚合。 | **维持 P2**（owner doc §十:348 documented simplification） |
| `P2-RC-027`（DISPOSAL 无浏览器层 E2E） | assets | 本切片非 E2E 维度，无增量。 | **维持 P2**（successor watch-only） |
| `P2-RC-028`（盘点盘盈/盘亏完整处置链复用收窄） | assets | A4.2.73 确认盘亏直接 SCRAPPED + PENDING 折旧计划行 orphaned（折旧引擎 IN_SERVICE 过滤兜底不重复计提），属本 finding documented simplification 范围。 | **维持 P2**（owner doc §四/§八 documented simplification） |

**无新 finding 新建**（全部维持）。运行时未发现会计错误已活跃（GL 借贷平衡未破坏），**不触发 MR0**。

---

## 3. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告为只读审计，**零生产代码变更**（纯审计报告 + 文档更新），checker 无回归风险。本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（checker 脚本为纯 reporter 退出码恒 0，真正门控在 CI workflow `.github/workflows/compliance.yml` 解析 actual > baseline => sys.exit(1)；本报告**不**以 checker 退出码 0 作为门控通过依据）。**actual = baseline**（与 A1.22/A1.23/A1.24 报告基线一致——本切片仅追加文档无生产代码变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 11 项运行时确认均衔接既有 finding（P1-RC-029/P1-RC-030/P1-MA2-060/P2-RC-025~028），无未经比对直接新建的 finding。维持注记已追加至 arm-index RC 交叉引用区（§2 衔接表 + arm-index 注记行）。
- [x] **业财保护区域探针纪律**：A4.2.64/A4.2.67/A4.2.68 三项触及业财保护区域探针——**READ-ONLY 确认**（grep census + 路径追踪 + 行级科目追踪 + GL 平衡推导），不改折旧/过账/处置凭证逻辑（详见 §0 前置声明）。

---

## 4. 裁决摘要

| 工作项 | 存疑点 | 运行时裁决 |
|--------|--------|-----------|
| A4.2.63 | 方式A 3 步链编排可达性 | **watch-only**（3 步技术可达，编排缺口属操作便利性，不单列 finding） |
| A4.2.64 | 方式B 补提偏差（P1-RC-029）⚠️ READ-ONLY | **维持 P1-RC-029 P1**（补提 mutation 缺失 + 单月语义，Q4 无例外） |
| A4.2.65 | 批量隔离跳过行为 | **主路径闭合**（per-asset try/catch + 告警派发） |
| A4.2.66 | 补提凭证 marker | **watch-only**（期间归属可审计，不单列 finding） |
| A4.2.67 | 出售补提缺失（reuse P1-RC-029）⚠️ READ-ONLY | **维持 reuse P1-RC-029 P1**（处置路径从不调补提 + 读陈旧值） |
| A4.2.68 | 处置凭证科目腿（P1-RC-030）⚠️ READ-ONLY | **维持 P1-RC-030 P1**（无 1606 中间科目腿，GL 平衡不破坏仅结构偏离） |
| A4.2.69 | posted=false reverseApprove（P1-MA2-060） | **维持 resolved R1.16**（告警+sweep+deliberate 不对称 documented） |
| A4.2.70 | 资本化末期残值修正 | **主路径闭合**（末期补差 + 残值约束双重兜底） |
| A4.2.71 | 维修重算 PENDING→EXECUTED | **主路径闭合**（EXECUTED 保留 + PENDING 重生成 + 末期补差） |
| A4.2.72 | 拆分容差平衡 | **主路径闭合**（PROPORTION_TOLERANCE + max-item residual fix） |
| A4.2.73 | 盘亏 SCRAPPED 折旧同步 | **主路径闭合**（折旧引擎 IN_SERVICE 过滤兜底，PENDING orphaned 属 P2-RC-028 范围） |

**整体裁决**：11 项存疑点运行时确认完成。**零 P0**（运行时未发现会计错误已活跃——GL 借贷平衡未破坏）。**3 项维持 P1**（P1-RC-029 方式B 补提 + 出售补提 reuse / P1-RC-030 处置凭证科目腿，修复归 MR1 ask-first）。**1 项维持 resolved**（P1-MA2-060 R1.16）。**4 项 P2 维持**（P2-RC-025/026/027/028）。**7 项主路径闭合/watch-only**（A4.2.63/65/66/70/71/72/73）。**不触发 MR0**（无活跃数据破坏）。本审计维持既有分级不撤销，记录运行时证据链，解除 A4.2.63-A4.2.73 在 MA4/R1.0 链路的运行时证据缺口。
