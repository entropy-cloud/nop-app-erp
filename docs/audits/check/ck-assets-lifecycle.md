# ck-assets-lifecycle — assets「资产生命周期」切片实现代码检查报告

> 工作项：C4.4。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-assets/erp-ast-service/src/main/java` 资产生命周期切片约 60 个手写生产文件中的核心链全部深读——资产卡片链 3 类（`ErpAstAssetBizModel` + `ErpAstAssetSuspendResumeProcessor` + `ErpAstAssetStateMachine`）+ 资本化链 7 类（`ErpAstAssetCapitalizationProcessor` + 5 per-mutation + `CapitalizationPostingDispatcher`）+ CIP 链 6 类（`ErpAstCipProcessor` + Start/AddCostItem/AddProgressBilling/TransferToAsset/ReverseTransfer）+ 处置链 7 类（`ErpAstDisposalProcessor` + 5 per-mutation + `DisposalPostingDispatcher`）+ 分割/合并链 16 类（`ErpAstSplitProcessor`/`ErpAstMergeProcessor` + 各 6 per-mutation + `AssetSplitPostingDispatcher`/`AssetMergePostingDispatcher`）+ 价值调整链 7 类（`ErpAstValueAdjustmentProcessor` + 5 per-mutation + `ValueAdjustmentPostingDispatcher`）+ 盘点链 9 类（`ErpAstInventoryProcessor` + 7 per-mutation + `AssetInventoryPostingDispatcher`）+ 共享（`AssetPostingExecutor`、`DepreciationCalculator`、`ErpAstDashboardBizModel`、InventoryStateMachine）。跨域核实：`ErpFinVoucherBizModel`（post/reverse 双 REQUIRES_NEW 实证）、`ErpFinPostingProcessor`（幂等命中返回 null + 失败自记录 PostingException）、`ErpFinDeferredPostingRetryHelper`（sweep 只重建 PostingEvent 重试凭证，无业务侧回调）、`ErpFinDeferredPostingRetryHelper#doRetry`、`AcctSchemaResolver.resolvePrimarySchemaId`（null 静默）、`ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor`（IN_SERVICE-only 按资产遍历）、`ErpAstDepreciationScheduleCatchUpDepreciationProcessor`（处置补提交点）、`module-assets/model/app-erp-assets.orm.xml`（versionProp 全实体/UK_AST_ASSET_CODE_ORG/Category 无 residualRate）、`app-service.beans.xml`（101 bean 接线抽查）。D1 机械扫描覆盖 `erp-ast-service/src/main/java` 全部手写文件。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.2/B4）。七链 Facade 逐行深读 + per-mutation Processor 抽读（同构 6+5×3 组抽 2 组全读 + 其余签名核对）+ 平台/跨域源码实证 + arm-index 复用裁决。
> 切片边界：折旧引擎本体（ExecuteDepreciation/ExecuteBatch/ReverseDepreciation/Recalculate、DepreciationPostingDispatcher/Provider、depreciation.batch.xml）归 C4.5——本切片只读其与本切片的交点（处置补提链 catchUpDepreciation、Split/Merge 建卡折旧口径、批量查询过滤口径）；Maintenance 族（UC-AST-10 维修）不在 C4.4/C4.5 切片清单内，未深读。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-ast-001（D6）Split PROPORTIONAL 模式累计折旧被二次派生覆盖——最大项补差失效，Σ 新卡累计折旧 ≠ 源资产累计折旧（价值守恒破坏）

- **控制点**：`app/erp/ast/service/processor/ErpAstSplitProcessor.java#computeAllocation`（L292-315 PROPORTIONAL 分支：`dep = sourceAccumDep.multiply(prop).setScale(4, HALF_UP)` + L314 `applyRemainderForAccumDep` 最大项补差）对照 L316-326 **无条件执行的二次派生循环**（注释写着「FIXED_AMOUNT 模式下 originalCostAmount 已由用户给定，累计折旧按金额比例派生」，但代码无 `if (fixedMode)` 包裹）：`ratio = orig.divide(sourceOriginal, 6, HALF_UP); dep = sourceAccumDep.multiply(ratio).setScale(4, HALF_UP); line.setAccumulatedDepreciationAmount(dep)`——**用 ratio 派生值覆盖了上一步含补差的分配值**。L325 `totalFixed = totalFixed.add(BigDecimal.ZERO)` 为无操作死代码（复制残留自证）。
- **证据**：三等分场景（sourceAccumDep=100.0000，3 行 prop=1/3）：第一遍 dep=33.3333×3=99.9999 → 补差最大项 +0.0001 → Σ=100.0000；第二遍 ratio=333.3333/1000=0.333333 → dep=33.3333 → **Σ 被覆盖回 99.9999**。源资产 `disposeSourceAsset`（L450-453）把源卡 accumulatedDepreciation 保留但 NBV 归零、新卡各拿 33.3333 → 全账 Σ 累计折旧凭空少 0.0001；净值同步偏移（每行 NBV = orig − dep）。DOWN_UP 补差机制（owner doc `split-merge.md` §配置选项「原值与累计折旧同步补差」）被彻底旁路。
- **问题**：拆分守恒约束（owner doc §关键业务规则 1 价值平衡）在累计折旧维度失效；原值维度因 `orig` 覆盖值与第一遍相同（同一公式）不受影响。币值精度 4 位、多行多次拆分时误差累积。同时 split 行快照（`ErpAstSplitLine.accumulatedDepreciationAmount`）与 `AssetSplitAcctDocProvider` 凭证金额均消费被覆盖后的值。
- **建议修复方向**：二次派生循环用 `if (fixedMode)` 包裹（仅 FIXED_AMOUNT 模式按金额比例派生累计折旧）；PROPORTIONAL 模式保留第一遍 + 补差结果。补测试：prop=1/3 三行拆分断言 Σ line.accumulatedDepreciationAmount == source.accumulatedDepreciation。
- **arm-index 裁决**：新增（grep「二次派生/尾差覆盖/残值复制」arm-index 零命中；A1.24 拆分合并追踪未覆盖分配算法维度）。

### P1-CK-ast-002（D6/D8）Split 目标卡 residualValue 全额复制到每张新卡——Σ 残值 N 倍放大，卡片残值与折旧计划残值口径分叉，直线法折旧系统性低估

- **控制点**：`ErpAstSplitProcessor.java#createTargetAssets` L378 `asset.setResidualValue(nz(source.getResidualValue()))`（循环内每张目标卡全额复制）对照同文件 `#generateDepreciationScheduleForTarget` L408 `residual = nz(source.getResidualValue()).multiply(proportion).setScale(4, HALF_UP)`（计划侧正确按比例）+ 执行/补提侧消费点 `ErpAstDepreciationScheduleCatchUpDepreciationProcessor` L93-95 与 `ErpAstDepreciationScheduleExecuteDepreciationProcessor` L76 均调 `DepreciationCalculator.calculate(method, asset.getOriginalValue(), asset.getResidualValue(), ...)`——**卡片字段**。
- **证据**：源 1000/残值 50 拆两行各 50%：每卡 original=500、卡片 residual=**50**（应 25），计划侧 residual=25。后续每期直线折旧（calculator L65 `original.subtract(residual).divide(months)`）= (500−50)/n 而非 (500−25)/n——每期低估、终态净值停在 50（双倍残值）；`calculator` L33 残值停提约束也按 50 截断。残值约束（README §关键业务规则 4「折旧后的账面净值不低于残值」）按虚高残值提前停提。
- **问题**：拆分后全部新资产的折旧计提与净值轨迹系统性错误，且卡片/计划/执行三口径不一致；处置 gainLoss 也消费被低估折旧的 NBV。
- **建议修复方向**：`createTargetAssets` 改为 `source.getResidualValue() * proportion`（与计划侧同公式，PROPORTIONAL）；FIXED_AMOUNT 模式按金额比例派生残值；补测试断言 Σ target.residualValue == source.residualValue。
- **arm-index 裁决**：新增（grep「残值复制/residualValue 复制」零命中）。

### P1-CK-ast-003（D5）盘点 reconcile 无实盘数量完整性校验——漏录行按实盘 0 判盘亏，processVariance 将在用资产静默 SCRAPPED

- **控制点**：`app/erp/ast/service/processor/ErpAstInventoryProcessor.java#calculateVariance`（L168-171：`int book = nzInt(line.getBookQuantity()); int actual = nzInt(line.getActualQuantity()); int variance = actual - book;`——`nzInt(null)=0`，**未录入实盘 = 实盘 0 = variance −1 = 盘亏**）+ `ErpAstInventoryReconcileProcessor.java#reconcile`（L23-35：仅状态守卫 `assertCanReconcile(COUNTING)` + calculateVariance，**无「全部行 actualQuantity 已录入」完整性校验**）+ `ErpAstInventoryProcessor#handleShortageTriggerDisposal`（L256-287：disposition 缺省自动置 DISPOSAL → `assetStateMachine.assertCanShortageDispose` 通过 → 资产置 SCRAPPED）。
- **证据**：盘点单 100 行，录完 99 行误触 reconcile（或前端部分提交）→ 第 100 行 varianceType=SHORTAGE、varianceAmount=NBV → processVariance 将该在用资产 SCRAPPED（无确认、无二次校验）；`validateAllVarianceProcessed` 只查 disposition!=NONE，盘亏行缺省 disposition 已被置 DISPOSAL，post 畅通——盘亏差异凭证照常出账。UC-AST-09 断言「盘点单 → 录入实盘数量」的完整性前提在后端无守卫。
- **问题**：数据破坏级用户可见缺陷——漏录即报废资产 + 出盘亏凭证；恢复需处置冲销（reverse 无资产侧回滚，见 P2-CK-ast-012）+ 人工重建。
- **建议修复方向**：reconcile 前置完整性校验（存在 actualQuantity==null 的行抛业务错误码，如 `ERR_AST_INVENTORY_ACTUAL_QUANTITY_MISSING`）；或区分「未盘」与「实盘 0」（如 actualQuantity null 时跳过差异计算并阻止 processVariance）。
- **arm-index 裁决**：新增（grep「漏录/实盘 完整性」零命中；P2-RC-028 裁决的是盘盈/盘亏处置链收窄，非录入完整性维度）。

### P1-CK-ast-004（D8/D2）ValueAdjustment 资产净值联动仅在过账同步成功时执行——悬挂窗口 sweep 重试成功后 GL 有凭证但资产 NBV 永不联动，且无重试入口

- **控制点**：`app/erp/ast/service/processor/ErpAstValueAdjustmentProcessor.java#executeApprove`（L83-87：`String voucherId = postingDispatcher.tryPost(...); if (voucherId != null) { applyAssetValueChange(adjustment, asset); }`——**凭证失败（返回 null）时跳过资产净值调整**）+ `#doAutoApprove`（L295-299 同型）对照跨域实证 `app/erp/fin/service/posting/ErpFinDeferredPostingRetryHelper.java#doRetry`（L96-109：sweep 只 `rebuildEvent` + `voucherBiz.post` + `markRetried`——**无任何业务侧回调**）+ `ErpAstValueAdjustmentApproveProcessor`（幂等短路 `adjustment.isApproved()` 直接 return，approve 无重试入口）。
- **证据**：时序：approve → tryPost 失败（如科目缺失/期间锁定）→ 单据 APPROVED+ACTIVE+posted=false、资产 NBV 未动 → fin 侧 `ErpFinPostingProcessor` 已记录 PostingException（跨域实证 L342 `exceptionRecorder.record`）→ sweep 重试成功 → GL 落下减值凭证 + PostingException 标 RETRIED——**但 `applyAssetValueChange` 永不执行**（sweep 不回调 ast，approve 幂等短路）。终态：GL 减值已入账、资产卡片 NBV/currentValue 原值未动——业账两面永久分叉。对照 Capitalization（建卡在 tryPost 前主事务内，悬挂重试后业务/凭证自然闭环）与 Disposal（终态推进在 tryPost 前）——VA 把「业务回写」放在了「过账成功」之后，成为唯一不自愈的链。
- **问题**：减值/重估凭证与卡片账面净值静默分叉；后续折旧（消费 NBV）、处置 gainLoss（消费净值）全部基于未调整的旧值。
- **建议修复方向**：对齐 Cap/Disposal 范式——`applyAssetValueChange` 移到 tryPost 之前主事务内（凭证悬挂经 sweep 重试后业务面已就位）；或 sweep 成功路径增加业务回调钩子（侵入大，不推荐）。
- **arm-index 裁决**：新增（grep「NBV 不联动/applyAssetValueChange」零命中；P1-MA4-013 折旧 dispatcher posted=false 悬挂是同族不同控制点——那是凭证侧悬挂无告警已修，本条是业务回写永不到位）。

### P1-CK-ast-005（D5/D6）ValueAdjustment 减值金额无上限校验——调整额可超过账面净值使 NBV/CurrentValue 为负

- **控制点**：`ErpAstValueAdjustmentProcessor.java#validateForApproval`（L196-201：仅 `adjustmentAmount == null || signum() <= 0` 拒绝——**无「金额 ≤ 资产 NBV（或 NBV−残值）」上限**）+ `#applyAssetValueChange`（L228-233：`newNbv = currentNbv.subtract(amount)` 后**直接 set**，L239-241 的 `<0 → ZERO` clamp 只作用于从未使用的局部变量 `newDepreciableBase`，NBV 本身无下限）。
- **证据**：NBV=4500 的资产录减值 10000 → newNbv=−5500 落库；`DepreciationCalculator.calculate` L33 `nbv.compareTo(residual) <= 0 → ZERO`（负 NBV 停提，尚不崩）；处置 gainLoss = 收入 − (−5500) = 虚高收益 5500 进 DISPOSAL 凭证（`DisposalPostingDispatcher#buildEvent` L110-114 同公式）；看板净值为负。
- **问题**：负净值资产 + 处置收益虚高直接进 GL。会计语义：减值应以可收回金额为限（不使账面为负）。
- **建议修复方向**：validateForApproval 增加上限校验（IMPAIRMENT/REVALUATION_DOWN：`amount <= NBV − residual`，REVALUATION_UP 可按需放开）；applyAssetValueChange 对 newNbv 加 `max(residual, 0)` 下限兜底。
- **arm-index 裁决**：新增（A1.24 价值调整维度未覆盖金额上限）。

### P1-CK-ast-006（D6）Split/Merge 新卡折旧口径三重错位——Merge 残值归零+全年限加权致执行期过度折旧；两链计划从源购置次月重排（覆盖历史期间）+ 计划基数不减已提

- **控制点**（三组）：
  1. `ErpAstMergeProcessor.java#createTargetAsset` L339 `target.setResidualValue(BigDecimal.ZERO)` + L342 `target.setAccumulatedDepreciation(totalAccumDep)` + `#resolveUsefulLifeMonths` L292-309（**全年限** `src.getUsefulLifeMonths()` 按 NBV 加权——owner doc `split-merge.md` §合并流程明写「剩余折旧期间 = 按加权平均**剩余**期间取整」）+ `#generateDepreciationScheduleForTarget` L364 `depBase = original`（不减残值不减已提）。执行侧消费点 `DepreciationCalculator.calculate` L65 直线法 `(original − residual)/months` = **全额原值/全年限**：合并卡 NBV 起点=totalNbv（已含已提折旧），此后每月按全额基数计提直到 NBV=0——**已折旧部分被再提一遍（过度折旧）**。
  2. `ErpAstSplitProcessor.java#generateDepreciationScheduleForTarget` L429-431 `start = baseDate.plusMonths(1)`（源购置次月）+ remainingMonths 期——期间覆盖**历史**（源已执行 inheritedMonths 期）；Merge 同构（L366-367）。
  3. Split L423 `depBase = original − residual*prop`（不减已提 accumDep）→ 计划总额虚高。
- **证据**：跨域实证 `ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor` L40-48——批量按**资产**遍历（IN_SERVICE-only）逐个调 executeDepreciation(assetId, 当前 period)，**不按 schedule.period 选行**——历史期间 PENDING 计划行不会被误执行（不重复折旧的兜底），但它们永久残留为死行（`executeDepreciation` 落行按 findSchedule(assetId, 当前期) 新建，与历史行并存）；预警/计划报表把历史期间行计入待提。Merge 的残值/年限错误则直接进入**执行金额**（calculator 消费卡片字段，见 P1-CK-ast-002 控制点）。
- **问题**：合并后资产折旧过度计提（GL 层面金额错误）；拆分/合并计划骨架期间错位+死行堆积；两链残值口径互不一致（Split 全额复制见 002 / Merge 归零）。
- **建议修复方向**：Merge 目标卡 residual = Σ 源残值、usefulLifeMonths 按剩余期间加权（owner doc 字面）、计划基数 = remaining NBV − residual；两链计划起点改 `max(当前期, 源继承点)+1`；与 001/002 联合修复（同一建卡口径重构）。
- **arm-index 裁决**：新增（grep「剩余期间 加权/计划 起点错位」零命中；A1.24 拆分合并维度未覆盖折旧口径）。

### P2-CK-ast-007（D5/D3，同型 P1-CK-pur-003 族）通用 CRUD update/delete 无单据状态守卫——资产卡片可删可改，且 delete 直接传导资本化 reverseApprove 静默跳过回滚

- **控制点**：`app/erp/ast/service/entity/` 下 `ErpAstAssetBizModel`（41 行裸 `CrudBizModel` + suspend/resume，零 `defaultPrepareSave/Update/Delete` 覆写）、`ErpAstAssetCapitalizationBizModel`、`ErpAstDisposalBizModel`、`ErpAstCipBizModel`、`ErpAstCipCostItemBizModel`、`ErpAstSplit(BizModel|LineBizModel)`、`ErpAstMerge(BizModel|LineBizModel)`、`ErpAstValueAdjustmentBizModel`、`ErpAstInventory(BizModel|LineBizModel)`、`ErpAstInventoryLineBizModel`、`ErpAstMovementBizModel`、`ErpAstCipProgressBillingBizModel` 全部裸 CRUD——对照 owner doc `state-machine.md §3`「使用中/闲置不可删除，只能处置」与「终态不可恢复」。传导链：`ErpAstAssetCapitalizationProcessor#executeReverseApprove` L116-125 `findAssetByCode(...)` 返回 null（资产被 CRUD 删除）时 `if (asset != null)` **静默跳过**资产回滚与 cancelSchedules——凭证已红冲（L115 REQUIRES_NEW 已提交）而资产侧零回滚、无告警。
- **证据**：IN_SERVICE 资产经 `ErpAstAsset__delete_` 物理删除后，其资本化单仍 APPROVED+posted=true；reverseApprove 该单 → 红冲凭证提交 → findAssetByCode=null → 跳过 → 单据翻 REJECTED——GL 与资产台账分叉无信号。SCRAPPED/SOLD 终态资产可经 `__update_` 直接改回 IN_SERVICE（终态复活绕过状态机）。
- **问题**：同 P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005/P2-CK-mfg-006/P2-CK-mfg2-012 全域同型——ast 站点按约定登记同型；资产卡片实体的额外高危点是 delete 传导 reverseApprove 静默断链。
- **建议修复方向**：终态/已过账实体 update/delete 守卫（`defaultPrepareUpdate/Delete` 校验 status 非终态 + posted=false）；资产卡片至少禁止删除非 DRAFT 卡。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，ast 站点新增计数）。

### P2-CK-ast-008（D7/D8）Cap/Disposal executeReverseApprove 红冲凭证（REQUIRES_NEW 已提交）先于资产状态守卫——assert 抛错时 GL 已红冲而主事务回滚，两面失配无信号

- **控制点**：`ErpAstAssetCapitalizationProcessor#executeReverseApprove` L114-125：L115 `postingDispatcher.reverse(cap)`（跨域实证 `ErpFinVoucherBizModel` L82 `@Transactional(REQUIRES_NEW)`——红冲已独立提交）**先于** L119 `assetStateMachine.assertCanReverseCapitalize(asset.getStatus())`（资产非 IN_SERVICE 时抛 `ERR_AST_ASSET_ILLEGAL_STATUS_TRANSITION`）+ `ErpAstDisposalProcessor#executeReverseApprove` L155-164 同构（L157 reverse 先于 L161 `assertCanReverseDispose`）。
- **证据**：触发面：资本化 approve 后资产已处置（SCRAPPED）再 reverseApprove 资本化单（validateTransition 只查单据 approveStatus=APPROVED，通过）；或资产状态被通用 CRUD 改写（见 007）。时序：红冲凭证提交 → assert 抛错 → `@BizMutation` 主事务回滚（单据保持 APPROVED+posted=true、资产保持 SCRAPPED）→ **GL 入账凭证已被冲掉而业务侧一切未动**。再次调用 reverseApprove 将再次红冲（reverse 幂等性依赖 fin 引擎 `alreadyPosted` 语义——已冲销凭证不视为幂等命中、允许重新过账，正向不对称）。ValueAdjustment `#executeReverseApprove` L110-112 同序但无 assert（污染面见 P3-CK-ast-027）。
- **问题**：编排顺序缺陷——跨域不可回滚步骤（REQUIRES_NEW 红冲）位于可抛错的本地守卫之前，违反「先校验后副作用」；与 mfg-005（红冲失败吞异常推进终态）互为镜像。
- **建议修复方向**：`assertCanReverseCapitalize/Dispose` 上提到 `postingDispatcher.reverse` 之前（executeReverseApprove 开头先 load 资产校验状态再红冲）。
- **arm-index 裁决**：新增（grep「红冲 先于/顺序倒置」零命中）。

### P2-CK-ast-009（D8，复用注记 P2-RC-086 + 同型 P2-CK-fin2-007/P2-CK-mfg-007 族）Dashboard 全部查询无 orgId 过滤——跨组织 KPI 混算

- **控制点**：`app/erp/ast/service/dashboard/ErpAstDashboardBizModel.java`——`loadInServiceAssets` L176-181（仅 `eq("status", IN_SERVICE)`）、`sumPeriodDepreciation` L183-193、`sumCipBalance` L195-204、`loadAssetIdsWithExecutedDepreciationInPeriod` L213-223、`loadExecutedSchedulesInRange` L206-211 均无 orgId filter，对照 UC-AST-12 断言「看板数据受行级权限约束(只看自己组织)」与 orm `UK_AST_ASSET_CODE_ORG (code, orgId)` 的多组织设计。
- **问题**：同型族新站点。arm-index `P2-RC-086`（全域看板 orgId 行级权限，A1.24 §7 等复用）已登记 watch-only/todo——本条按同型登记，修复随族统一裁决。
- **arm-index 裁决**：复用注记（P2-RC-086 + ai-check 族 fin2-007/mfg-007；ast 站点计数）。

### P2-CK-ast-010（D8）CIP 余额 KPI 部分转固后不扣减——accumulatedCost 保留归集全额，已转固成本在固定资产与在建工程余额双计

- **控制点**：`ErpAstCipProcessor#addCostItem`（`ErpAstCipAddCostItemProcessor.java` L60-61 `cip.accumulatedCost += amountFunctional`）对照 `#postProcess` L176-198（转固后仅更新 CostItem `postedTransferFlag`/`capitalizationId`、CIP status/isCompleted/completedAssetId——**不扣减 accumulatedCost**）+ `ErpAstDashboardBizModel#sumCipBalance` L195-204（`eq("isCompleted", false)` + `Σ accumulatedCost`——部分转固的 CIP（isCompleted=false）余额含已转固部分）。
- **证据**：CIP 归集 100 万，部分转固 60 万（Cap 单建卡 60 万固定资产 + 凭证）→ CIP 仍 IN_CONSTRUCTION、accumulatedCost=100 万 → 看板 `cipBalance` 计 100 万 + 固定资产原值计 60 万——同一笔成本双计 160 万视角。owner doc `cip.md` §部分转固「剩余未转固部分继续留在 CIP 卡片中」——余额语义应为未转固 CostItem 汇总。
- **问题**：KPI 口径错误（部分转固场景）；即使全部转固（isCompleted=true 被过滤）语义也是「过滤掉」而非「扣减」，行为上凑对但字段语义失真（TRANSFERRED 卡片仍挂全额成本）。
- **建议修复方向**：postProcess 扣减 `cip.accumulatedCost -= Σ 本次转固 CostItem.amountFunctional`；或 sumCipBalance 改按 `CostItem.postedTransferFlag=false` 汇总（后者不动字段，只修读侧——推荐，字段保留审计语义需 owner doc 裁决）。
- **arm-index 裁决**：新增（grep「accumulatedCost 扣减/CIP 余额 双计」零命中）。

### P2-CK-ast-011（D5/D8）CIP reverseTransfer 无 capitalizationId 归属校验——传入他 CIP 资本化单可错误红冲并回退错误 CostItem

- **控制点**：`ErpAstCipReverseTransferProcessor.java#reverseTransfer` L34-41：`findCostItemsByCapitalization(capitalizationId)`（跨域实证 `ErpAstCipProcessor` L267-271：查询仅 `eq("capitalizationId", ...)` **无 cipId 过滤**，也不校验 `cap.sourceCode == cip.code`）→ 仅以 `capCostItems.size() < allCipCostItems.size()` 判部分红冲 → 直接 `capitalizationProcessor.reverseApprove(capitalizationId, context)` + 回退 capCostItems + 当前 CIP 回 IN_CONSTRUCTION。
- **证据**：CIP-A（TRANSFERRED，5 CostItem）传入 CIP-B 的资本化单（B 有 10 CostItem）：10 ≥ 5 通过校验 → B 的资本化被红冲（B 的资产回 DRAFT、B 的凭证被冲）→ 回退的是 B 的 10 个 CostItem → **A 回 IN_CONSTRUCTION 但 A 的 5 个 CostItem 仍 postedTransferFlag=true**（A 自身转固态被破坏且无自愈路径，B 的状态也被污染）。正常 UI 传对 id 时不触发——API 层零防御。
- **问题**：入参边界缺失致跨实体数据损坏链。
- **建议修复方向**：reverseTransfer 前置归属校验（`capCostItems` 全部 `cipId == cipId` 或查 Cap 单 `sourceType=CIP && sourceCode == cip.code`，不满足抛业务错误码）。
- **arm-index 裁决**：新增（A1.24 CIP 维度未覆盖反向入参校验）。

### P2-CK-ast-012（D8）盘点 reverse 只红冲凭证回退单据——不回滚盘盈新卡/盘亏 SCRAPPED，单据轴可逆与资产轴不可逆不对称

- **控制点**：`ErpAstInventoryReverseProcessor.java#reverse` L40-47（红冲凭证 + posted 三件套清空 + status 回 RECONCILING——**零资产侧回滚**）对照正向 `ErpAstInventoryProcessor#handleSurplusCreateCard` L231-253（新卡 IN_SERVICE + line.newAssetId）与 `#handleShortageTriggerDisposal` L285-287（资产置 SCRAPPED，终态无出边——`ErpAstAssetStateMachine` 无 reverseShortage 动作）。
- **证据**：reverse 后：GL 差异凭证已冲、盘点单回 RECONCILING 可再 processVariance——但盘盈新卡仍在（无删除路径，新卡也无处置关联）；盘亏资产仍 SCRAPPED（`handleShortageTriggerDisposal` L279-283 对终态资产直接 return，无法重新处置）；行 disposition 已固化。RECONCILING 态改行 disposition=INVESTIGATE 后再 post，凭证按行金额重出——与新卡/终态资产的实际状态脱节。
- **问题**：P2-RC-028 已裁决「盘盈/盘亏直接建卡/SCRAPPED（避免双重过账）」为 documented simplification，但其 **reverse 对称性**未被裁决——资产轴不可逆使单据轴的 RECONCILING 可逆语义失效（回退后无法真正重新处理差异）。
- **建议修复方向**：reverse 时联动回滚（盘盈卡逻辑删除/置 DRAFT；盘亏资产恢复 IN_SERVICE——需 AssetStateMachine 增 shortage 逆动作，触及保护区域须 dual-agent）；或 owner doc 显式裁决「盘点 reverse 后资产侧动作不可逆，重处理仅限金额维度」并收紧 reverse 前置（禁止存在已执行资产侧动作时 reverse）。
- **arm-index 裁决**：新增（P2-RC-028 裁决范围不含 reverse 对称性）。

### P2-CK-ast-013（D10）Merge 悬空源引用 null 被 validateSources continue 放行——executeApprove sum() 对 null 实体 NPE（500 而非业务错误码）

- **控制点**：`ErpAstMergeProcessor.java#validateSources` L219-222（`for (...) { ErpAstAsset src = sources.get(i); if (src == null) { continue; } ... }`——悬空引用静默放行）对照 `#executeApprove` L93-95 `sum(sources, ErpAstAsset::getOriginalValue)` + 静态 `#sum` L473-479（`total.add(nz(f.apply(src)))`——**src=null 时方法引用调用 NPE**）+ `#createTargetAsset` L328 `sources.get(0)` 解引用。
- **证据**：行 `sourceAssetId` 指向已删除资产（CRUD delete 无守卫，见 007）→ `loadSources`（L438-444 惰性 `line.getSourceAsset()`）返回含 null 列表 → validateSources continue → executeApprove L93 NPE——未捕获 NullPointerException 从 @BizMutation 冒泡（非 NopException 业务码）。
- **问题**：D10 + D2 混合：悬空引用应抛业务错误（如 `ERR_AST_MERGE_SOURCE_NOT_FOUND`）而非 NPE；validate 与执行对 null 的语义不一致（校验放行、执行崩溃）。
- **建议修复方向**：validateSources 对 null 源抛业务错误码（行号入参）；loadSources 后前置非空断言。
- **arm-index 裁决**：新增。

### P2-CK-ast-014（D5）Merge 源资产行无去重——同一资产出现在多行时价值双计、贡献比例失真

- **控制点**：`ErpAstMergeProcessor.java#validateSources` L209-245（校验状态/类别/币种——**无 sourceAssetId 重复检查**）对照 Split 侧 `#validateLines` L231-241（`codes.add(targetAssetCode)` 去重守卫在位——镜像实现有、Merge 缺）+ `#executeApprove` L93-95（`sum()` 对 sources 直接求和——同资产两行则 totalOriginal/totalAccumDep/totalNbv 全部双计）。
- **证据**：配置错误（同 sourceAssetId 两行）→ 目标卡 originalValue=2×实际、凭证借贷按虚增金额出账；`disposeSourceAssets` 对同一实体 DISPOSED 两次（无害）；`contributionProportion` 按虚增基数计算。
- **问题**：入参边界缺失直接进 GL 金额；Split/Merge 镜像实现守卫不对称自证遗漏。
- **建议修复方向**：validateSources 增加 sourceAssetId Set 去重（对齐 Split validateLines 范式，抛 `ERR_AST_MERGE_SOURCE_DUPLICATE`）。
- **arm-index 裁决**：新增。

### P2-CK-ast-015（D7，同型 P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011 族）REQUIRES_NEW 凭证先于主事务末步提交——ast 六链 approve 末段 updateEntity 仍可失败，中途失败留孤儿凭证

- **控制点**：`ErpAstAssetCapitalizationProcessor#executeApprove` L90-101（L90 `tryPost`（REQUIRES_NEW 已提交）→ L100 `capDao().updateEntity(cap)` 乐观锁可抛）+ `ErpAstDisposalProcessor#executeApprove` L134-143 同构 + `ErpAstSplitProcessor#executeApprove` L120-134 + `ErpAstMergeProcessor#executeApprove` L122-137 + `ErpAstValueAdjustmentProcessor#executeApprove` L83-96 + `ErpAstInventoryPostProcessor#post` L48-57；处置链额外第二张前置凭证：`ErpAstDisposalProcessor#catchUpDepreciationToDisposalPeriod` L101 → `ErpAstDepreciationScheduleCatchUpDepreciationProcessor` L143 `tryPostCatchUp`（#CATCHUP 凭证 REQUIRES_NEW）。
- **证据**：主事务回滚（乐观锁冲突/其他行冲突）时：资产/单据/计划回滚，但 CAP/DISPOSAL/ASSET_SPLIT/ASSET_MERGE/VALUE_ADJUSTMENT/盘点/补提凭证已提交——凭证无对应业务状态；重试 approve 时 post() 幂等命中已提交凭证返回 null → dispatcher 判定失败（P1-CK-fin-003 同型面）或重建资产再出第二张凭证（前一版孤儿滞留 GL）。
- **问题**：同型族 ast 站点登记（6 链 7 张凭证位）。范式本身是平台裁决（processor-extension-pattern 硬规则 1），缺陷在「REQUIRES_NEW 之后仍有可失败步骤」的编排顺序——处置链双凭证位风险最高。
- **建议修复方向**：随族统一裁决（与 inv-012/pur-002/mfg-011 联合）：GL 过账移 afterCommit 或配对账告警。
- **arm-index 裁决**：同型登记（P2-CK-inv-012/P1-CK-pur-002 族，ast 站点新增计数）。

### P2-CK-ast-016（D6）ValueAdjustment 折旧基数调整为死代码——newDepreciableBase 计算后从未使用，减值后未来折旧不降

- **控制点**：`ErpAstValueAdjustmentProcessor.java#applyAssetValueChange` L236-242：`if (shouldAdjustDepreciationBase(type)) { BigDecimal residual = ...; BigDecimal newDepreciableBase = newNbv.subtract(residual); if (newDepreciableBase.signum() < 0) { newDepreciableBase = BigDecimal.ZERO; } }`——**局部变量计算后无任何消费**（无字段接收、无返回）+ `#shouldAdjustDepreciationBase` L267-275（IMPAIRMENT true / REVALUATION_UP 按 config）成为纯谓词空转。
- **证据**：减值 1000 后 NBV=4500，但卡片 originalValue/residualValue 不变 → `DepreciationCalculator` 直线法 L65 `(original − residual)/months` 仍按原值计提——每期折旧不受减值影响；净值被折旧推低至残值的过程比减值后的合理轨迹多提（减值效果被后续折旧「吃回」GL）。资产无「减值后折旧基数」字段（orm 实证 Asset 无 impairmentBase 类列）——声明行为（README §核心业务对象「资产价值调整：影响账面净值」+ 减值语义）无落地点。
- **问题**：声明的折旧基数联动未落地（半成品代码残留）；减值后折旧费用虚高。
- **建议修复方向**：短期在 calculator 消费点取 `min(original, NBV)` 作折旧基数（减值后按净值轨迹）或 owner doc 裁决 Deferred 移除死代码；根治需 Asset 增折旧基数字段（触及 ORM 保护区域，dual-agent）。
- **arm-index 裁决**：新增。

### P3-CK-ast-017（D6）资本化/盘盈建卡 residualValue 硬编码 ZERO——cip.md「预计净残值按资产类别比例计算」无字段支撑

- **控制点**：`ErpAstAssetCapitalizationProcessor#createAndActivateAsset` L223 `asset.setResidualValue(BigDecimal.ZERO)` + `ErpAstInventoryProcessor#handleSurplusCreateCard` L241 同型 + `ErpAstMergeProcessor#createTargetAsset` L339（见 006）对照 owner doc `cip.md` §流程「预计净残值 = 按资产类别比例计算」与 orm 实证 `ErpAstAssetCategory` 列集（code/name/depreciationMethod/usefulLifeMonths/3 科目——**无 residualRate 字段**）。
- **问题**：设计声明无落地字段——所有新建资产残值恒 0，直线法全额计提；与 Split 侧「继承源残值」（002 的错误复制）口径又不一致。属设计层缺口（字段缺失），修复需 ORM 变更（保护区域）。
- **建议修复方向**：Category 增 `residualRate` 列（dual-agent 批准）或 owner doc 显式裁决「残值仅手工维护、建卡恒 0」。
- **arm-index 裁决**：新增。

### P3-CK-ast-018（D6）非直线法建卡折旧计划 plannedAmount 全 0——DECLINING/UNITS 资产计划骨架空转

- **控制点**：`ErpAstAssetCapitalizationProcessor#plannedAmount` L272-282（仅 STRAIGHT_LINE 返回计算值，**其他方法 `return BigDecimal.ZERO`**——months 期全 0 计划行照常落库）+ `ErpAstSplitProcessor#generateDepreciationScheduleForTarget` L403-405 / `ErpAstMergeProcessor#generateDepreciationScheduleForTarget` L360-362（非直线法直接 return——连 0 计划都不生成，三处行为还不一致）。
- **证据**：跨域实证 `ErpAstDepreciationScheduleExecuteDepreciationProcessor` L76 执行金额经 `DepreciationCalculator.calculate` 现算（支持三种方法）——**实际计提不受 planned=0 影响**；但计划报表/`findDepreciationMissingAlert` 语义失真（计划金额列恒 0 / 无计划行）。
- **问题**：计划展示层缺陷，执行兜底在位。资本化（生成 0 行）与 Split/Merge（不生成行）行为不一致。
- **建议修复方向**：plannedAmount 按方法预计算（DECLINING/UNITS 首期可算）；三处行为统一；或 owner doc 裁决「计划仅直线法支持」。
- **arm-index 裁决**：新增。

### P3-CK-ast-019（D5）Disposal 处置收入负数无校验——负 disposalAmount 直接进 gainLoss 与凭证

- **控制点**：`ErpAstDisposalProcessor#validateForApproval` L235-240（仅校验 assetId/disposalType 非空）对照 L106-107 `disposalAmount = nz(disposal.getDisposalAmount()); gainLoss = disposalAmount.subtract(nbv)`——负收入（如 −1000）产生 gainLoss=−1000−nbv 的巨额损失 + DISPOSAL 凭证负收入行。
- **问题**：入参符号边界缺失（误输负号静默生效）。对照 CIP 侧 `validateAmountPositive`（L229-235）有正数校验——同域守卫不对称。
- **建议修复方向**：validateForApproval 增加 `disposalAmount != null && signum() < 0` 拒绝（报废允许 0/null）。
- **arm-index 裁决**：新增。

### P3-CK-ast-020（D10）处置补提链 YearMonth.parse(lastExecuted) 无守卫——脏 period 数据触发非业务异常

- **控制点**：`ErpAstDisposalProcessor#catchUpDepreciationToDisposalPeriod` L297-303（`lastExecuted = findLastExecutedPeriod(...)` 后 `java.time.YearMonth.parse(lastExecuted).plusMonths(1)` **无 try/catch**——对照 L291-296 对 disposalPeriod 的解析有 catch 兜底返回）。
- **问题**：schedule.period 脏数据（手工 CRUD 改坏，见 007）时 DateTimeParseException 冒泡为系统异常而非业务码。低频边界。
- **建议修复方向**：parse 包 try/catch 对齐 disposalPeriod 分支（异常时跳过补提并 warn）。
- **arm-index 裁决**：新增。

### P3-CK-ast-021（D2）Split/Merge/VA/Inventory 过账失败即时告警缺失——对照 Cap/Disposal 的 dispatchFailureAlert 不对称

- **控制点**：`AssetSplitPostingDispatcher#tryPost` L53-60 / `AssetMergePostingDispatcher`（同构）/ `ValueAdjustmentPostingDispatcher#tryPost` L43-51 / `AssetInventoryPostingDispatcher#tryPost` L43-50——catch 内仅 LOG.warn/error 返回 false/null；对照 `CapitalizationPostingDispatcher#dispatchFailureAlert` L72-88 与 `DisposalPostingDispatcher` L67-83（`IErpSysNotificationBiz` G4 告警派发在位）。
- **证据**：跨域实证 `ErpFinPostingProcessor` 失败时自记录 PostingException（L342）→ sweep 兜底重试覆盖全业务类型——**可恢复性不受影响**，缺的仅是运营即时感知（sweep 周期前的静默窗口）。
- **问题**：同域告警覆盖不对称（G4 分级只落了 2/6 链）。
- **建议修复方向**：四 dispatcher 补 dispatchFailureAlert（复用 Cap 范式）。
- **arm-index 裁决**：新增（P1-MA4-013 折旧侧告警已修，资产域其余 dispatcher 未覆盖）。

### P3-CK-ast-022（D2，同型全域族）currentUserId 宽 catch 返回 null 无日志——审计字段 approvedBy/postedBy 写 null 无信号

- **控制点**：`ErpAstAssetCapitalizationProcessor#currentUserId` L358-365 / `ErpAstDisposalProcessor` L367-374 / `ErpAstSplitProcessor` L517-524 / `ErpAstMergeProcessor` L460-467 / `ErpAstValueAdjustmentProcessor` L348-355 / `ErpAstInventoryProcessor` L402-409（六处同型 `catch (Exception e) { return null; }` 零日志）。
- **问题**：同 P3-CK-md-008/pur-010/sal-024/inv-018/fin2-012/fin3-013/mfg-016 全域族——ast 六站点登记。
- **建议修复方向**：catch 补 LOG.warn；同型修复统一裁决。
- **arm-index 裁决**：同型登记（全域族，ast 站点）。

### P3-CK-ast-023（D3/D8）Split/Merge/盘亏终态资产不取消 PENDING 折旧计划、盘亏 NBV 不清零——与 Disposal 的 cancelPendingSchedules/NBV 归零不一致

- **控制点**：`ErpAstSplitProcessor#disposeSourceAsset` L450-454（DISPOSED + NBV=0，**无 cancelPendingSchedules**）+ `ErpAstMergeProcessor#disposeSourceAssets` L389-396 同型 + `ErpAstInventoryProcessor#handleShortageTriggerDisposal` L285-287（SCRAPPED，**既不 cancel 计划也不清 NBV**）对照 `ErpAstDisposalProcessor#cancelPendingSchedules` L311-320（处置链有取消）。
- **证据**：跨域实证批量折旧 IN_SERVICE-only（`ExecuteBatchDepreciationProcessor` L42）——DISPOSED/SCRAPPED 资产不会被误提（正确性兜底在位）；但 PENDING 死行残留使计划查询/`findDepreciationMissingAlert`（若范围扩展）语义脏化，盘亏资产 NBV 保留原值（对照 Split DISPOSED 置 0——同域终态语义又不一致）。
- **问题**：终态资产善后不一致（三链 vs 处置链）；纯残留数据/展示面。
- **建议修复方向**：三处置入对齐 Disposal 的 cancelPendingSchedules + 统一终态 NBV 归零口径。
- **arm-index 裁决**：新增。

### P3-CK-ast-024（D9）Dashboard 趋势全表载入（loadExecutedSchedulesInRange 名不符实）+ 本月新购资产折旧假预警

- **控制点**：`ErpAstDashboardBizModel#loadExecutedSchedulesInRange` L206-211（签名收 from/to，查询**只有 status=EXECUTED 无日期过滤**——全表载入后内存分桶，窗口外数据白载）+ `#findDepreciationMissingAlert` L141-160（IN_SERVICE 资产当期无 EXECUTED 行即预警——本月资本化资产计划从次月起（`ErpAstAssetCapitalizationProcessor` L246 `start = baseDate.plusMonths(1)`，符合「转固次月开始计提」），当期必然无 EXECUTED → **每月新建资产全部假预警**）。
- **问题**：同型 P3-CK-pur-011/sal-026/inv-020/mfg-014 性能家族 + 预警口径缺 acquisitionDate 豁免。
- **建议修复方向**：查询下推 period 范围过滤；预警豁免 `acquisitionDate 当月` 资产。
- **arm-index 裁决**：新增（同型家族性能站点 + 假预警新维度）。

### P3-CK-ast-025（D3）suspend 备注标记无限追加——多轮闲置/恢复循环后 remark 堆叠，resume 不清理

- **控制点**：`ErpAstAssetSuspendResumeProcessor#suspend` L42-44（`remark == null/空 ? idleMark : remark + "；" + idleMark`——每次 suspend 追加「闲置自 {date}」）+ `#resume` L49-59（不清理标记）。owner doc `state-machine.md §2`「暂停时点经 remark『闲置自 {date}』强制记录（idleSince 列不落 ORM）」——设计无清理约定，但多轮循环 remark 无限增长（remark 列 precision 1000 撑满后 DB 截断异常风险）。
- **建议修复方向**：resume 时移除末次标记；或每轮覆盖式记录（保留最近 N 轮）。
- **arm-index 裁决**：新增。

### P3-CK-ast-026（D8/D10）findAssetByCode 无 orgId 过滤——UK 为 (code, orgId)，跨组织同 code 时 get(0) 命中不确定

- **控制点**：`ErpAstAssetCapitalizationProcessor#findAssetByCode` L303-308（`eq("code", code)` + `list.get(0)`，无 orgId、无 limit）+ `ErpAstCipProcessor#findAssetByCode` L273-282（`setLimit(1)` 但同样无 orgId）对照 orm L228 `UK_AST_ASSET_CODE_ORG columns="code,orgId"`（code 仅 org 内唯一）。
- **问题**：多组织部署下 reverseApprove/postProcess 可能命中他 org 同 code 资产（reverseCapitalize 错卡回滚）。单组织部署无影响（降 P3 依据）。
- **建议修复方向**：查询补 `eq("orgId", cap.getOrgId())`。
- **arm-index 裁决**：新增（同族 orgId 隔离散点）。

### P3-CK-ast-027（D8）ValueAdjustment reverseApprove 无资产状态守卫——已处置资产的 NBV 仍被回写

- **控制点**：`ErpAstValueAdjustmentProcessor#executeReverseApprove` L108-117（`if (posted)` → reverse 凭证 → `rollbackAssetValue`——**无 asset 状态校验**，对照 approve 侧 `validateAssetAdjustable` 只在正向）。VA approve 后资产被处置（SCRAPPED）再 reverseApprove VA 单：终态（已退出报表）资产的 NBV/currentValue 被回滚调整——终态卡片数据被污染；且该资产此后无折旧/报表消费面，影响限于审计口径。
- **建议修复方向**：rollbackAssetValue 前置 `assetStateMachine.isTerminal(status)` 拒绝或跳过+warn。
- **arm-index 裁决**：新增。

## 跨域关联影响面注记（不新建 finding）

| 已登记 finding | ast 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-fin-003`（post() 幂等命中返回 null 与 dispatcher「null=失败」语义冲突） | ast 全部 6 个正向过账位同型受影响：`CapitalizationPostingDispatcher#tryPost` L58-59（`voucherId != null` 判 posted）、`DisposalPostingDispatcher#tryPost` L54（返回 null=失败 → `ErpAstDisposalProcessor` L138 `voucherId != null`）、`AssetSplitPostingDispatcher#tryPost` L51-52、`AssetMergePostingDispatcher`（同构）、`ValueAdjustmentPostingDispatcher#tryPost` L42（→ `executeApprove` L85——**叠加本域 P1-CK-ast-004 使 NBV 联动也跳过**）、`AssetInventoryPostProcessor#post` L48-50。O-16 补偿场景（REQUIRES_NEW 已提交、调用方 posted 设置前失败 → sweep 幂等命中返回 null → 标 RETRIED 但业务 posted 遗留 false）对全部 6 位成立 | **同型受影响面确认**（任务预登记 5 类型之外，ASSET_SPLIT/ASSET_MERGE/ASSET_INVENTORY_ADJUSTMENT 3 个 dispatcher 为新增计数）；随 fin-003 三态化修复联动核 |
| `P1-CK-fin-005`（AcctSchemaResolver null→静默成功） | ast 全部 6 dispatcher 的 `resolveAcctSchemaId` 均经 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)`（源码实证：orgId=null 或无账套返回 null，不抛错）→ PostingEvent.acctSchemaId=null 传导至 fin 引擎 | **同型受影响面确认**：ast 6 dispatcher + Disposal 的 orgId fallback asset.orgId 链；随 fin-005 修复联动核 |
| `P1-CK-pur-003`（CRUD update 无已审守卫全域同型） | ast 全实体裸 CrudBizModel（16 个 entity BizModel） | **同型登记为 P2-CK-ast-007**（资产卡片 delete 传导 reverseApprove 断链已在 007 内注明） |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：`erp-ast-service/src/main/java` 全部手写文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0、`extends RuntimeException/Exception`=0、字符串字面量 `==` 比较=0（命中均为 `== null` 判空）、无手编生成产物（与 compliance 基线 R4/R5/R7 一致）。
- **乐观锁在位**：orm 实证全部 18 实体 `versionProp="version"`（L174/261/328/397/513/...）——并发 addCostItem/折旧累计/Split 源处置由乐观锁检测。
- **处置后资产不可再变动/折旧（D3 核查项）**：SCRAPPED/SOLD/DISPOSED 资产的七条再入路径全有守卫——suspend/resume（`assertCanSuspend/Resume` 单态白名单）、价值调整（`validateAssetAdjustable` 拒终态+限 IN_SERVICE/IDLE）、处置（`validateAssetDisposable`）、Split 源/Merge 源（`validateSourceAsset`/`validateSources` 仅 IN_SERVICE）、盘点范围（`expandAssetsToLines` liveStatuses）、批量折旧（引擎 IN_SERVICE-only）+ catchUp `validateAssetInService` 拒 IDLE——终态无复活路径（唯一通路是通用 CRUD 旁路，见 007）。
- **处置损益方向与 UC-AST-05 一致**：`gainLoss = disposalAmount − nbv`（正=收益）与 use-cases.md「清理损益 = 处置收入 − 账面净值」字面一致；UC-AST-04 的「损失 = 净值 − 收入」是同一数量的损失视角。
- **出售补提后 gainLoss 消费补提值**：`ErpAstDisposalProcessor#executeApprove` L101 补提 → L103-104 读 `asset.getAccumulatedDepreciation()`——catchUp 内 `facade.requireAsset(assetId)` 与 `disposal.getAsset()` 在同一 @BizMutation session 内按 id 返回**同一受管实例**（ORM 一级缓存），L125-126 的累计回写对 L104 可见。依赖平台 session 缓存语义（标准行为），非缺陷。
- **IDLE 处置/补提跳过符合 owner doc**：`catchUpDepreciationToDisposalPeriod` L284-286 仅 IN_SERVICE 补提（state-machine.md Phase 1 Decision「IDLE 不允许补提，以卡片账面计提为准」）+ `assertCanDispose` 接受 IDLE（§2「IN_SERVICE/IDLE → SCRAPPED/SOLD」）——文档对齐。
- **折旧计划末月补差正确**：`ErpAstAssetCapitalization#plannedAmount` L275-277（末月 = 总额 − 前 n−1 月和）+ Split L437 同构——直线法计划守恒在位。
- **CIP 汇率换算方向自洽**：`amountSource = amountFunctional / exchangeRate`（addCostItem L40-41 / buildCapitalizationRequest L149-151 同式）——rate=源→本位币惯例下正确，rate 默认 1 时无差。
- **catchUp 幂等在位**：已 EXECUTED 期间跳过（L89-91）+ saveOrUpdate UK 冲突转 `ERR_AST_DEPRECIATION_ALREADY_EXECUTED` 业务码（L114-123）+ 空漏提列表不生成凭证（L132-134）——重复触发不双计。
- **Split/Merge 不可逆契约落地**：`ErpAstSplitReverseApproveProcessor#reverseApprove` L22-26 无条件抛 `ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED`（Merge 同构）——owner doc §关键业务规则 5 + 状态机名义边契约一致。
- **sweep 兜底覆盖 ast 全业务类型**：跨域实证 `ErpFinPostingProcessor` 失败自记录 PostingException（L342/L362/L430）——dispatcher 吞异常不影响 sweep 重试可用性（Cap/Disposal 的 javadoc「有 DeferredPostingSweepJob 兜底」声明实际覆盖 Split/Merge/VA/Inventory；即时告警缺失另见 P3-CK-ast-021）。
- **Split 目标编码冲突有 UK 兜底**：`UK_AST_ASSET_CODE_ORG` 使 targetAssetCode 撞已有资产时 saveEntity 抛唯一约束异常回滚（报错不友好但正确性守住）；行间重复有 `validateLines` 前置守卫（L237-241）。
- **per-mutation 幂等与 SoD 基线**：六链 approve 均有 `isApproved()` 短路 + `validateNotCancelled` + 状态机 Bean 固定边守卫（cause-chain 领域码映射契约 §7 全链一致）；Split/Merge submit 与 approve 双侧重复 validate（源状态/平衡校验 TOCTOU 收窄）。
- **beans 接线完整**：`app-service.beans.xml` 101 bean 全注册（抽查 7 链 Processor/Dispatcher/StateMachine/Executor bean id 全命中）；`_service.beans.xml` 为生成产物未手编。
- **D4 对本切片 N/A**：唯一 batch.xml `nop/batch-task/ast/depreciation.batch.xml` 归折旧切片（C4.5）；生命周期切片无调度接线。
- **dict 无死状态**：asset-status 6 值（DRAFT/IN_SERVICE/IDLE/SCRAPPED/SOLD/DISPOSED）全部有活跃 writer（DISPOSED 由 Split/Merge disposeSourceAsset 写入）；cip-status 3 值（startConstruction/transferToAsset/reverseTransfer 全可达）；inventory 5 态状态机迁移矩阵完整（DRAFT→COUNTING→RECONCILING→POSTED + CANCELLED + reverse 回 RECONCILING）。
- **PostingEvent 不读未提交数据**：六 dispatcher buildEvent 均消费内存实体（cap/disposal/asset/category 先存数据）+ Inventory 汇总字段（recalculate 时已更新于同 session 实体）——REQUIRES_NEW 读隔离无空读风险（已逐个核）。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `残值复制`/`二次派生`/`尾差覆盖`/`漏录`/`红冲 先于`/`accumulatedCost 扣减`/`剩余期间 加权`/`applyAssetValueChange`/`NBV 不联动` 在 `docs/audits/arm-index.md` **零命中 → 新增**（001-006、008、010、011、013、014、016-021、023-027）。
- **复用（不重复登记，报告中注记）**：
  - Dashboard orgId → **P2-RC-086**（arm-index，全域看板 orgId 行级权限 watch-only/todo）+ ai-check 族 P2-CK-fin2-007/P2-CK-mfg-007——P2-CK-ast-009 同型登记。
  - CRUD 无守卫 → **P1-CK-pur-003 族**（本 mission）——P2-CK-ast-007 同型登记。
  - REQUIRES_NEW 凭证先提交 → **P2-CK-inv-012 / P1-CK-pur-002 / P2-CK-mfg-011 族**——P2-CK-ast-015 同型登记。
  - currentUserId 宽 catch → **P3-CK-md-008 全域族**——P3-CK-ast-022 同型登记。
  - 处置补提（catchUpDepreciation）→ **P1-RC-029**（resolved RC-R1.52，处置链接线为修复成果）——主链在位验证，不登记。
  - 盘盈/盘亏处置链收窄 → **P2-RC-028**（documented simplification 已接受）——012 只登记其未覆盖的 reverse 对称性维度。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 6 | P1-CK-ast-001..006 |
| P2 | 10 | P2-CK-ast-007..016 |
| P3 | 11 | P3-CK-ast-017..027 |

按主维度：D6×6（001/002/005 主 D5 计 D5、006/016/017/018 中 001/006/016/018 计 D6，002 计 D6）、D5×4（003/005/007/011/014/019 中 003/011/014 计 D5）、D8×5（004/010/012/023/026）、D7×2（008/015）、D2×2（021/022）、D9×1（024）、D3×2（023 跨计 D8 后 D3 由 025 承担）、D10×2（013/020）。（精确主维度归属：001 D6、002 D6、003 D5、004 D8、005 D5、006 D6、007 D5、008 D7、009 D8、010 D8、011 D5、012 D8、013 D10、014 D5、015 D7、016 D6、017 D6、018 D6、019 D5、020 D10、021 D2、022 D2、023 D3、024 D9、025 D3、026 D8、027 D8。）

跨域关联注记 3 项（fin-003 ast 6 dispatcher 同型受影响面确认 + Split/Merge/Inventory 3 dispatcher 为预登记 5 类型外新增计数；fin-005 ast 全 dispatcher 传导面确认；pur-003 同型登记为 007）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片核心 7 链约 55 文件逐行深读（7 个 Facade 355-568 行全读、per-mutation Processor 抽读 2 组全读 + 其余签名/结构核对——六链 per-mutation 同构模式已由 Capitalization 全 5 个 + Split 3 个全读锚定、posting dispatcher 6 个全读、AssetStateMachine/InventoryStateMachine 全读、Dashboard 全读、CatchUp 交点全读）；跨域源码实证 8 处（voucherBiz REQUIRES_NEW 双注解、PostingProcessor 幂等 null + PostingException 自记录、DeferredPostingRetryHelper 无业务回调、AcctSchemaResolver null 静默、ExecuteBatch IN_SERVICE-only 按资产遍历、ExecuteDepreciation 消费卡片字段现算、抽象基类 AbstractApprove/SubmitForApprovalProcessor 结构）；orm 核对（versionProp 18 实体、UK_AST_ASSET_CODE_ORG/Category/Split/Merge code+orgId、Category 无 residualRate、dict 全值 writer 核对）；D1 机械扫描全量；beans 101 注册抽查。
- **未深查**：`erp-ast-web` AMIS view.xml 契约 drift（归 C8.2）；折旧引擎本体 5 Processor + DepreciationPostingDispatcher/Provider + depreciation.batch.xml（归 C4.5——本切片只读了交点）；Maintenance 族 9 类（UC-AST-10，不在 C4.4/C4.5 切片清单，其 MAINT_EXPENSE/MAINT_CAP dispatcher 未经本切片深读）；AcctDocProvider 6 个的凭证行组装细则（借贷方向核对抽样 Capitalization/Disposal/Split 三个，Provider 金额均消费 dispatcher billData 已核字段）；测试代码正确性（43 个测试文件未用于行为交叉验证之外的审查）；xbiz 层 `<auth permissions>` 声明完整性（抽查未做——SoD/权限维度归 web/xbiz 契约层）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-ast-003**（盘点漏录=盘亏→资产 SCRAPPED）——若 AMIS 前端强制「全部行录完才能提交 reconcile」，触发面收窄为 P2；但后端零守卫的事实独立成立（API 直调可绕过）。建议用「留一行 actualQuantity=null → reconcile → processVariance」集成测试实证。
  2. **P1-CK-ast-004**（VA 悬挂后 NBV 永不联动）——severity 依赖 DeferredPostingSweepJob 对 VALUE_ADJUSTMENT 的重试路径是否真的不回调业务侧（`ErpFinDeferredPostingRetryHelper#doRetry` 实证只调 voucherBiz.post/reverse；若存在其他 sweep 变体或人工工作台回写 posted 时联动业务，则降级）。建议主 agent 全量 grep `applyAssetValueChange` 调用方确认唯一。
  3. **P1-CK-ast-006**（Merge 残值 ZERO + 全年限的执行期过度折旧）——结论依赖 `DepreciationCalculator` 直线法无「剩余期数」概念（L65 全 months 除法，elapsed 仅 Declining 用）这一实证；若 C4.5 检查发现执行层另有 remaining 语义，本条降级为计划骨架错位（P2）。
