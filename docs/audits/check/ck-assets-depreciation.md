# ck-assets-depreciation — assets「折旧与过账」切片实现代码检查报告

> 工作项：C4.5。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-assets/erp-ast-service/src/main/java` 折旧与过账切片 24 个手写生产文件——折旧引擎（`service/DepreciationCalculator` + `ErpAstDepreciationScheduleProcessor` helper facade + 5 个 per-mutation Processor：Execute/ExecuteBatch/Reverse/CatchUp/RecalculateForCapitalizationMaintenance）+ `entity/ErpAstDepreciationScheduleBizModel`（Facade）+ `statemachine/ErpAstDepreciationScheduleStateMachine` + 过账链（`posting/AssetPostingExecutor`/`DepreciationPostingDispatcher`/`DepreciationAcctDocProvider`/`MaintenanceExpensePostingDispatcher`+`Provider`/`MaintenanceCapitalizationPostingDispatcher`+`Provider`/`ValueAdjustmentPostingDispatcher`+`Provider`）+ `ErpAstMaintenancePostProcessor`/`ErpAstMaintenanceReverseProcessor`/`ErpAstMaintenanceProcessor`（过账相关 step）+ `ErpAstValueAdjustmentProcessor`（过账/净值联动面）+ `_vfs/nop/batch-task/ast/depreciation.batch.xml` + `app-erp-all/_vfs/nop/job/conf/erp-ast-depreciation.job.yaml`（D4）。跨域核实：`ErpFinPostingProcessor.process/reverseProcess`（幂等 null 语义 + 异常记录）、`ErpFinVoucherBizModel.post`（REQUIRES_NEW）、`ErpFinDeferredPostingRetryHelper`（sweep 重试无域回调）、`ErpFinAccountingPeriodProcessor.runDepreciation/reverseDepreciation`（期末结账接线 + 反结账红冲）、`IErpFinVoucherReversedListener` 全仓实现清单（pur/sal/inv/mfg 四域有、assets 无）、`ErpAstAssetStateMachine`/`ErpAstAssetCapitalizationProcessor`（逆资本化与折旧交点）、`ErpAstDisposalProcessor#catchUpDepreciationToDisposalPeriod`（处置传导面）、`DisposalAcctDocProvider`（净值口径）、`module-assets/model/app-erp-assets.orm.xml`（UK/versionProp/列集）、`erp/ast/beans/app-service.beans.xml`（101 bean 接线）、`TestErpAstMaintenance`（资本化维修断言面）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。核心链逐文件深读 + 平台与跨域源码实证（post() 幂等返回 null、REQUIRES_NEW 边界、sweep 重试语义、UK 实存）+ arm-index 复用裁决。
> 切片边界：资产生命周期/购置/CIP/分割合并/处置本体归 C4.4（`ck-assets-lifecycle`）——本切片仅覆盖其与折旧主链的交点（逆资本化对折旧凭证/计划行的处置、处置补提接线、处置净值口径）。资产盘点（`ErpAstInventory*`）按路线图名义属 C4.5「折旧与过账/盘点」，本次按主 agent 指令范围未深查（见剩余风险）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。行号以当前 HEAD 为准。

### P1-CK-ast2-001（D6）工作量法（UNITS）折旧恒为 0——两个调用点均传 null 工作量参数，且 ORM 无任何工作量数据列，dict 可选方法静默失效

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteDepreciationProcessor.java#executeDepreciation`（L76-77 `DepreciationCalculator.calculate(method, ..., months, elapsed, null, null)`——`periodUnits`/`estimatedTotalUnits` 恒 null）+ `ErpAstDepreciationScheduleCatchUpDepreciationProcessor.java#catchUpDepreciation`（L93-95 同型 `calculate(..., elapsed, null, null)`）+ `app/erp/ast/service/service/DepreciationCalculator.java`（L54-57：`if (estimatedTotalUnits == null || estimatedTotalUnits.signum() <= 0 || periodUnits == null) { return BigDecimal.ZERO; }`）
- **证据**：dict `erp-ast/depreciation-method` 含 `UNITS 工作量法`（`app-erp-assets.orm.xml` L82），owner doc `depreciation-and-posting.md §1.3` 明列「工作量法：(原值 - 残值) / 预计总工作量 × 本期工作量」。grep 全 `module-assets` 生产代码 `periodUnits|estimatedTotalUnits` 仅命中 `DepreciationCalculator` 自身形参；ORM（asset/category/schedule 三实体）无工作量列（grep `work|unit` 仅命中 dict 行）。即：选择了 UNITS 方法的资产，每月批量折旧/单资产折旧/补提全部得到 `amount=0` 的 EXECUTED 计划行，累计折旧永不增长，无任何警告。
- **问题**：声明支持的折旧方法（dict 可选 + 设计 §1.3）运行时静默零产出——工作量法资产的折旧费用/累计折旧永久低估，且计划行已标 EXECUTED 掩盖了漏提事实（补提也补 0）。
- **建议修复方向**：补工作量数据面（asset 或 schedule 增期间工作量录入）并在 executeDepreciation/catchUp 传入真实值；或短期在方法解析处对 UNITS 抛「工作量数据未配置」业务错误（避免静默零），并在 owner doc §十 登记 Deferred。
- **arm-index 裁决**：新增（grep「工作量法/UNITS」arm-index 零命中；A1.22 折旧引擎 RC 审计只覆盖 UC-AST-02 直线法场景）。

### P1-CK-ast2-002（D6）资本化维修折旧基数增量双计——applyTreatmentCapitalize 先把原值 +=X 再调 recalculateForCapitalizationMaintenance(assetId, X)，recalc 内 `original.add(increment)` 再加一次 X

- **控制点**：`app/erp/ast/service/processor/ErpAstMaintenanceProcessor.java#applyTreatmentCapitalize`（L92-96 `asset.setOriginalValue(nz(...).add(increment)); ... saveOrUpdateEntity(asset);` → L101-103 `depreciationScheduleBiz.recalculateForCapitalizationMaintenance(asset.getId(), increment, context)`）对照 `ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor.java#recalculateForCapitalizationMaintenance`（L37 `BigDecimal original = nz(asset.getOriginalValue());`——同一 session 取回**已加过 X** 的受管实体；L58 `BigDecimal depreciableBase = original.add(nz(increment)).subtract(residual).subtract(accumulated);`——再加一次 X；L83 计划行 `setNetBookValue(original.add(nz(increment)).subtract(accumulated))` 同样虚高）
- **证据**：数值代入（`TestErpAstMaintenance.testCapitalizePathWithDepreciationRecalc` 场景）：原值 100000、残值 5000、24 个月、资本化 X=25000 → 正确基数 = 125000−5000 = 120000；实际计算 = 125000+25000−5000 = **145000**（每月 6041.6667 vs 正确 5000，剩余寿命多提 25000）。测试仅断言 `pending` 非空（TestErpAstMaintenance.java L106-107），未断言计划总额/月额——双计未被测试钉住。镜像路径 `rollbackCapitalization`（L113-116 先 `subtract(increment)` 落库，L120-121 再传 `increment.negate()`）同样把基数多减一次 X（回退后基数 70000 vs 正确 95000）。config `erp-ast.maintenance-cap-adjust-depreciation-base` 默认 true（L125-127），即默认路径即触发。
- **问题**：资本化维修后剩余寿命期内系统性多提折旧 X（回退方向少提 X）——净值/累计折旧/GL 折旧费用全部失真；与资产卡片原值（单次加 X，正确）自相矛盾。
- **建议修复方向**：`recalculateForCapitalizationMaintenance` 语义二选一：改为「读增量前原值」（调用方先传参后改值），或删除 `.add(nz(increment))`（原值已含增量）——推荐后者（recalc 只按当前卡片状态重算），同步修 L83 计划行 NBV；补断言计划总额 = 原值−残值−已提 的测试。
- **arm-index 裁决**：新增（grep「recalculateForCapitalizationMaintenance/资本化维修 基数」arm-index 仅 P1-RC-029 行提到该方法名但主题是补提缺失，未覆盖基数双计维度）。

### P1-CK-ast2-003（D6/D4）批量折旧缺「当月增加下月提」守卫——期末结账常规路径对资本化当月资产照常计提，且与 nop-batch job 路径（按次月起的 PENDING 计划行）语义分裂

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor.java#executeBatchDepreciation`（L41-48：`q.addFilter(eq("status", ASSET_STATUS_IN_SERVICE))` 遍历**全部在用资产**，逐个 `executeDepreciation(asset.getId(), period, ...)`——不看 `acquisitionDate`/计划起始月）对照 `ErpAstAssetCapitalizationProcessor#generateDepreciationSchedule`（L246 `LocalDate start = baseDate.plusMonths(1);`——计划行从**次月**起生成，落地「当月增加下月提」）+ `ErpAstDepreciationScheduleExecuteDepreciationProcessor#executeDepreciation`（L54 `findSchedule(assetId, period)` 返回 null 时 L83-90 **ad-hoc 新建**当期行——无「期间必须 ≥ 资本化次月」守卫）
- **证据**：期末结账常规路径（finance 侧）`ErpFinAccountingPeriodProcessor#closeAssetModule → runDepreciation`（L181-202，config `erp-ast.auto-depreciation-on-close` 默认开）每月对关闭期间调 `executeBatchDepreciation(period.getCode())`。资产 2026-08-15 资本化（IN_SERVICE、计划行自 2026-09 起）→ 关闭 2026-08 期间时批量折旧对它 `executeDepreciation(assetId, "2026-08")` → findSchedule null → 新建 2026-08 行并计提。owner doc §折旧调度规则 明文「折旧起始月：资本化入账的次月开始计提折旧（当月增加下月提）」。对照另一入口 `depreciation.batch.xml`（nop-batch job）：loader 按 `ErpAstDepreciationSchedule(PENDING) + period=当前月` 迭代——计划行次月起才有 → job 路径语义正确。双入口对同一规则口径不一致。
- **问题**：主自动化路径（期末结账）违反声明的折旧起始规则：资本化当月即计提，全部生命周期多提一个月（提前一个月折完）；同时制造计划外第 13+ 条 schedule 行（与 `months` 条计划并存）。
- **建议修复方向**：`executeDepreciation` 增加「period 必须晚于 `YearMonth.from(acquisitionDate)`」守卫（批量循环自然短路）；或 `executeBatchDepreciation` 查询加 `acquisitionDate < period 首日` 过滤。同步裁决双入口（job vs 结账）语义统一。
- **arm-index 裁决**：新增（grep「当月增加/次月/起始月」arm-index 零命中）。

### P1-CK-ast2-004（D8）逆资本化红冲回退闭环断裂——reverseApprove 只红冲 CAPITALIZATION 凭证，已执行的 DEPRECIATION 凭证滞留 GL，资产累计却清零；cancelSchedules 无状态过滤直接把 EXECUTED 行写 CANCELLED（绕过 assertCanCancel）

- **控制点**：`app/erp/ast/service/processor/ErpAstAssetCapitalizationProcessor.java#executeReverseApprove`（L114-125：`postingDispatcher.reverse(cap)` 仅红冲资本化凭证 → `asset.setAccumulatedDepreciation(BigDecimal.ZERO)`/`setNetBookValue(originalValue)` → `cancelSchedules(asset.getId())`）+ `#cancelSchedules`（L310-318：`q.addFilter(eq("assetId", assetId))` **无 status 过滤**，逐行 `s.setStatus(scheduleStateMachine.cancelTargetStatus())`——不调用 `assertCanCancel`，EXECUTED/REVERSED 行一并改写 CANCELLED）对照状态机契约 `ErpAstDepreciationScheduleStateMachine#assertCanCancel`（L62-68：仅 PENDING 合法）与 `ErpAstAssetStateMachine#assertCanReverseCapitalize`（L59-65：来源态 IN_SERVICE 合法——已折旧数月的资产正是 IN_SERVICE，**无折旧已执行的阻断守卫**）
- **证据**：时序：资本化（APPROVED+posted）→ 3 个月折旧执行（3 张 DEPRECIATION 凭证已入 GL，资产累计折旧>0）→ 资本化 reverseApprove → GL 只冲 CAPITALIZATION 一张，3 张折旧凭证滞留（折旧费用/累计折旧科目余额残留、无有效资产对应）→ 资产累计清零 → 全部 schedule 行（含 3 条 EXECUTED）直接写 CANCELLED（posted/voucherId 不清，状态机 EXECUTED→CANCELLED 非法边）。业务/账务两面失配且无告警。
- **问题**：mission D8「折旧凭证↔累计折旧↔净值三方同步/红冲回退闭环」在逆资本化方向断裂；同时是状态机 Bean 契约（cancel 仅 PENDING）被同域 Processor 绕过的 writer 覆盖实例（D3）。
- **建议修复方向**：`executeReverseApprove` 前置守卫「存在 EXECUTED 折旧行则拒绝逆资本化（提示先逐期 reverseDepreciation）」；`cancelSchedules` 按 `assertCanCancel` 过滤仅取消 PENDING 行。控制点在 C4.4 实体但对折旧闭环的破坏归本切片，修复需两切片协调。
- **arm-index 裁决**：新增（P1-MA2-060 覆盖 Cap tryPost 吞异常悬挂，未覆盖「逆资本化 vs 已执行折旧」维度；grep「cancelSchedules/逆资本化 折旧凭证」零命中）。

### P1-CK-ast2-005（D8/D2）assets 域无凭证红冲反写监听 + 引擎层 REVERSAL 失败被 sweep 异步重试红冲——异步红冲成功后资产侧 posted/累计折旧不回退，事后手动红冲报 ERR_REVERSE_SOURCE_NOT_FOUND 死锁

- **控制点**：全仓 `IErpFinVoucherReversedListener` 实现清单（grep 实证：`PurReversalListener`/`SalReversalListener`/`InvReversalListener`/`MfgSubcontractReversalListener` 四域——**module-assets 零实现**）+ finance 侧 `ErpFinPostingProcessor#reverseProcess`（L265-272：`catch (RuntimeException e) { ... recordReverseFailure(run, e); throw e; }`——红冲失败**无条件**记 `ErpFinPostingException(POSTING_TYPE_REVERSAL)`，不区分业务类型）+ `ErpFinDeferredPostingRetryHelper#doRetry`（L96-100：`if (POSTING_TYPE_REVERSAL.equals(postingType)) { voucherBiz.reverse(...); }`——sweep 自动重试红冲，**无域回调**置回 `schedule.posted`/回退资产累计折旧）
- **证据**：时序：`reverseDepreciation(assetId, period)` → `postingDispatcher.reverse` 抛瞬时错误（如 `resolveOpenPeriod` 期间锁）→ 引擎已落 REVERSAL 异常记录（含告警 notify）→ 外层 @BizMutation 回滚，schedule 仍 EXECUTED+posted=true（此刻一致）→ sweep（`deferred-posting-sweep.batch.xml`，24h 窗口内）重试红冲成功 → **GL 折旧凭证被冲销，但资产侧 posted=true、累计折旧未回退** → 运营按 G4 文档手动 `reverseDepreciation` 自愈 → `ERR_REVERSE_SOURCE_NOT_FOUND`（源凭证已 isReversed）硬失败 → 死锁（GL 已冲、业务未冲、且无法再冲）。反向入口 `executeDepreciation` 幂等重执行的前置红冲（L58-59）同理撞墙。owner doc §7.2「折旧 G4 无 sweep 覆盖」的声明与引擎实际行为（全类型记录 + 全类型 sweep）矛盾。
- **问题**：业财红冲反写闭环（M5 能力）在 assets 域缺位：pur/sal/inv/mfg 四域都有监听器承接引擎红冲事件，assets 的 posted/累计折旧/净值只能靠同步 mutation 维护——一旦红冲经异步通道完成即产生不可自愈的悬挂。
- **建议修复方向**：assets 域实现 `IErpFinVoucherReversedListener`（按 billHeadCode 反查 schedule 行 → 置 posted=false/voucherId=null 并回退资产累计/净值——与 `reverseDepreciation` 同语义幂等）；短期替代：红冲失败时避免引擎记录 REVERSAL 异常（dispatcher 捕获后不外抛给引擎记录通道需平台侧裁决，须 plan-first）。关联 `P2-CK-fin-006`（sweep 无源单有效性校验）。
- **arm-index 裁决**：新增（grep「ReversalListener/反写监听 assets」arm-index 零命中；mfg 报告 P1-CK-mfg-005 为同步红冲吞异常不同控制点）。

### P1-CK-ast2-006（D8）价值调整后资产账面与 GL/处置三方失同步——净值只改 netBookValue/currentValue（originalValue/累计折旧不动），处置凭证净值 = 原值−累计折旧 忽略调整额，减值准备科目处置时不清

- **控制点**：`app/erp/ast/service/processor/ErpAstValueAdjustmentProcessor.java#applyAssetValueChange`（L222-245：`asset.setNetBookValue(newNbv); asset.setCurrentValue(newNbv);`——originalValue 与 accumulatedDepreciation 均不动）+ `#rollbackAssetValue`（L247-265 同口径）对照 GL 侧 `ValueAdjustmentAcctDocProvider#createFacts`（L69-78：REVALUATION_UP 借 1601 固定资产 / IMPAIRMENT 贷 1604 减值准备——GL 固定资产/减值准备科目余额已变）+ 消费侧 `DisposalAcctDocProvider`（L64-76：`BigDecimal original = readDecimal(event, BILL_DATA_ORIGINAL_VALUE); ... BigDecimal net = original.subtract(accumDep);`——**处置净值用原值−累计折旧**，不读资产 netBookValue，也无 1604 减值准备结转腿）
- **证据**：减值 20000 后处置：资产卡片 NBV 已减 20000（VA 联动），GL 1604 有 20000 贷方余额；处置凭证按 original−accum 计算清理损益（多计损失 20000）且 1604 余额永不清（资产已 SCRAPPED）。重估增值同理：GL 1601 增加而资产 originalValue 不增——资产登记簿与 GL 固定资产控制科目余额漂移，`NBV = original − accumulated` 不变量被破坏（后续依赖该不变式的阅读面全部失真）。
- **问题**：mission D8 三方同步（凭证↔累计折旧↔净值）在价值调整方向断裂：调整后任何处置/报废的清理损益错误，减值准备科目残留。处置腿的科目分解归 C4.4，但「调整写入口径」与「净值不变量破坏」的控制点在本切片。
- **建议修复方向**：VA 联动改为同时维护不变量：减值走 accumulated 侧（或独立 impairment 累计列）+ originalValue 对重估增值同步上调 + 处置侧净值口径改读资产净账面（或 original−accum−impairment）并补 1604 结转腿。涉及会计科目口径，修复走 plan-first（保护区域）。
- **arm-index 裁决**：新增（grep「netBookValue originalValue 失同步/减值准备 处置」零命中；A2.10 状态机审计未覆盖数值联动维度）。

### P2-CK-ast2-007（D6）elapsed 口径 = 全部 EXECUTED 行数而非「早于目标期的行数」——补提/重执行非最新期间时双倍余额递减的剩余期数与直线切换点错位

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteDepreciationProcessor.java#executeDepreciation`（L71 `int elapsed = facade.countExecuted(assetId) - (wasExecuted ? 1 : 0);`——对**目标期之后**已执行的期间照计不误）+ `ErpAstDepreciationScheduleCatchUpDepreciationProcessor.java#catchUpDepreciation`（L82 `int elapsed = facade.countExecuted(assetId);` 同型）+ `facade.countExecuted`（`ErpAstDepreciationScheduleProcessor` L126-131：`eq("status", EXECUTED)` 计数，无 period 比较）+ `DepreciationCalculator` DECLINING 分支（L41-51：`remaining = months - elapsed`，`remaining <= 24` 切直线）
- **证据**：DECLINING 资产 60 个月寿命，2026-01..06 已执行（elapsed=6）；补提 2026-03（漏提期）：elapsed=6 而非真实 2 → remaining=54 而非 58 → 直线切换点与剩余期除数错位，补提额与当月原始计提额不一致（同为 2026-03 两个时点算出不同金额）。重执行旧期同型。STRAIGHT_LINE 不受影响（elapsed 不参与），故降 P2。
- **建议修复方向**：`countExecuted` 增加 `lt("period", targetPeriod)` 过滤（或 QueryBean count），两调用点统一传目标期。
- **arm-index 裁决**：新增（P1-RC-029 行引用过 `countExecuted` 但主题是补提缺失，未覆盖口径维度）。

### P2-CK-ast2-008（D2/D7，同型 P3-CK-pur-013/P3-CK-mfg-017 族·站点升级）executeBatchDepreciation 单事务逐资产吞异常——失败资产 session 脏写残留可致后续资产连锁失败/整批回滚，而每资产凭证已 REQUIRES_NEW 独立提交

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor.java#executeBatchDepreciation`（L45-53：`for (asset : assets) { try { executeDepreciationProcessor.executeDepreciation(...); } catch (Exception e) { LOG.warn(...e.getMessage()); } }`——整个批在 Facade `@BizMutation` 单事务内；`executeDepreciation` 内部 `facade.orm().flushSession()`（ExecuteDepreciationProcessor L106）逐资产强刷）+ 凭证侧 `ErpFinVoucherBizModel#post`（L74-75 `@Transactional(REQUIRES_NEW)` 实证——每资产凭证独立提交）
- **证据**：失败注入点：并发双跑（job 02:00 与期末结账同时触发同一期间——UK 冲突路径正是为此设计的，ExecuteDepreciationProcessor L107-112 UK catch 转 `ERR_AST_DEPRECIATION_ALREADY_EXECUTED`）→ 该资产 flush 失败后脏实体残留 session → 后续每资产 `flushSession` 重试失败插入连锁抛错 → 全批 warn 后返回低计数，或最终提交时再抛 → **整批资产回写回滚而已生成的凭证（REQUIRES_NEW）滞留 GL 形成孤儿凭证群**。catch 仅 `LOG.warn(e.getMessage())` 无告警派发（对照 dispatcher 有 notify），owner doc §5.1「失败资产记录错误等待人工处理」仅落到日志。设计声明的「错误隔离/每组独立事务/并行」均未落地（§十已登记并行 Deferred，但**错误隔离**的声明仍在 §5.1）。
- **问题**：同型家族（pur batchApprove/mfg generatePendingJobCards）在折旧主链的站点，因双入口并发现实存在（UK 守卫的存在自证预期并发）且失败后果放大（孤儿凭证群 + 期末结账 G3 阻断），按站点升级 P2。
- **建议修复方向**：逐资产独立事务（per-asset REQUIRES_NEW 编排或 nop-batch chunk 语义承接——job 路径已是 chunk 事务，可考虑收敛到单入口）；catch 内补告警派发。
- **arm-index 裁决**：同型登记（P3-CK-pur-013/P3-CK-mfg-017 族，ast 站点升级计数）。

### P2-CK-ast2-009（D2/D3）维修过账失败后重试入口被封死且无告警——post() 无论凭证成败都翻 status=POSTED；CAPITALIZE 路径失败时资产原值已增/计划已重算而 GL 缺凭证

- **控制点**：`app/erp/ast/service/processor/ErpAstMaintenancePostProcessor.java#post`（L72-89：`voucherId = expenseDispatcher.tryPost(...)`（或 capitalization）吞异常返回 null → `m.setStatus(stateMachine.postTargetStatus())` **无条件翻 POSTED**，仅 `voucherId != null` 时置 posted 三元组）+ `ErpAstMaintenanceStateMachine#assertCanPost`（L90-93：仅 COMPLETED 合法——翻成 POSTED 后重 post 被拦）+ `MaintenanceExpensePostingDispatcher#tryPost`（L40-52：catch → LOG → return null，**无 dispatchFailureAlert**，对照 DepreciationPostingDispatcher L75-93 有 notify）+ CAPITALIZE 顺序（L73-76：`applyTreatmentCapitalize`（改资产+重算计划）**先于** tryPost）
- **证据**：CAPITALIZE 失败时序：资产原值 +=X、currentValue/NBV +=X、PENDING 计划删除重算（L89-104 facade）→ tryPost 失败 → m=POSTED+posted=false → 重 post 被 `assertCanPost(POSTED)` 拦死 → reverse 被 `!posted` 守卫拦死（ReverseProcessor L42-43）→ 资产增值与计划已变而 GL 无凭证。引擎层 `recordPostFailure`（ErpFinPostingProcessor L205-213）会记 PostingException 并经 sweep 补凭证（billData 快照可重建），GL 最终可自愈，但 `m.posted` 永不回 true（sweep 无域回调，见 P1-CK-ast2-005 同根因）→ posted 标志与 GL 状态长期分裂 + 无告警通知（与 G 分级范式不一致：折旧/Cap/Disposal 均有告警或 sweep 语义文档，MAINTENANCE_* 无）。
- **问题**：lesson 09（业财过账吞异常悬挂）在维修过账的站点：无告警 + 状态机封死重试 + 业务侧已变更与 GL 缺凭证窗口。
- **建议修复方向**：posting 失败时保持 COMPLETED 可重试（成功才翻 POSTED），对齐 VA 范式（`ErpAstValueAdjustmentProcessor#executeApprove` L83-96 同样先翻状态但 asset 变更成功路径有 voucherId 门控——维修的资产变更应同样后置于凭证成功或提供补偿）；tryPost 补 dispatchFailureAlert。
- **arm-index 裁决**：新增（带同族注记）——P1-MA2-060 覆盖 Cap/Disposal dispatcher 吞异常（resolved，Cap/Disposal 有 sweep 兜底语义），MAINTENANCE_*/VA 站点未覆盖；grep「maintenance post 失败 死锁」零命中。

### P2-CK-ast2-010（D4）减值/重估「折旧基数调整」为死代码——newDepreciableBase 计算后丢弃，config `erp-ast.revaluation-adjust-depreciation-base` 声明默认 true 但零效果

- **控制点**：`app/erp/ast/service/processor/ErpAstValueAdjustmentProcessor.java#applyAssetValueChange`（L236-242：`if (shouldAdjustDepreciationBase(type)) { BigDecimal residual = ...; BigDecimal newDepreciableBase = newNbv.subtract(residual); if (newDepreciableBase.signum() < 0) { newDepreciableBase = BigDecimal.ZERO; } }`——**结果从未使用**：不重算折旧计划、不更新残值、不触发任何后续）+ `#shouldAdjustDepreciationBase`（L267-275：IMPAIRMENT 恒 true / REVALUATION_UP 读 config 默认 true）
- **证据**：owner doc §4.1「减值：减少账面净值，不影响折旧（**减值后折旧基数调整**）」「重估增值：可调整折旧基数」。实现：调整后折旧继续按 `DepreciationCalculator`（SL 用 originalValue−residual，完全不受 NBV 调整影响；DECLINING 用 NBV 部分受影响）——声明逻辑（无论 config 开关）对计划/后续计提零作用。对照资本化维修路径有真实的 `recalculateForCapitalizationMaintenance` 重算范式可复用。
- **问题**：D4「声明配置/特性未落地」家族 + 死代码（局部变量赋值后丢弃）。减值后折旧不按新基数 → 减值资产后续折旧费用错报（SL 完全无感）。
- **建议修复方向**：落地重算（复用 recalc 范式按新基数重生成 PENDING 计划）或删除死代码块并在 owner doc §十 登记 Deferred。
- **arm-index 裁决**：新增（带同族注记，家族 = P2-CK-mfg-008 声明配置零消费）。

### P2-CK-ast2-011（D5/D3，同型 P1-CK-pur-003 族）assets 全实体裸 CrudBizModel 无状态守卫——折旧计划行/资产卡片汇总列可直接手改；executeDepreciation 不调 assertCanExecute，REVERSED/CANCELLED 行可被覆盖复活

- **控制点**：`app/erp/ast/service/entity/` 下 `ErpAstAssetBizModel`（L20 裸 `CrudBizModel<ErpAstAsset>`，仅 suspend/resume 委托）/ `ErpAstDepreciationScheduleBizModel`（L29 同）/ Disposal/Capitalization/Cip/Maintenance/ValueAdjustment/Split/Merge 等——grep `defaultPrepareSave|defaultPrepareUpdate|defaultPrepareDelete` 全 entity 目录**零覆盖** + `ErpAstDepreciationScheduleExecuteDepreciationProcessor#executeDepreciation`（L96 `schedule.setStatus(scheduleStateMachine.executeTargetStatus())` **不调用** `assertCanExecute`——REVERSED（状态机声明无出边终态）/CANCELLED 行直接被重写为 EXECUTED；注释 L95 自认「重执行/幂等路径为动态编排逻辑保留原位」）
- **证据**：`ErpAstDepreciationSchedule__update_` 可直接改 `actualAmount`/`accumulatedDepreciation`/`posted` 而不联动资产卡片——三方（凭证↔计划行↔资产汇总）任一可被旁路改写。CANCELLED 行复活路径受 `validateAssetInService` 间接保护（处置后资产非 IN_SERVICE），但 REVERSED 行复活是现实操作路径（reverseDepreciation 后重跑）——数值上自洽（elapsed 不计 REVERSED 行），但状态机 Bean 契约（REVERSED 终态无出边）被绕过，`transitions()` 元数据与实际行为失配（下游 Delta 覆盖/审计分析据此误判）。
- **问题**：同 P1-CK-pur-003/P1-CK-sal-004/P2-CK-mfg-006 全域同型（ast 站点登记）+ 本切片特有的「来源态断言缺失」子项。
- **建议修复方向**：EXECUTED/REVERSED/CANCELLED 计划行与终态/已过账资产禁用通用 update/delete（`defaultPrepareUpdate` 校验）；executeDepreciation 增加来源态白名单断言（PENDING/null + 显式 REVERSED 重提边——若业务确认，同步修订状态机矩阵与 owner doc）。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，ast 站点计数）。

### P2-CK-ast2-012（D9）批量折旧 N+1——每资产 4+ 次查询（期间重复查/计划行/EXECUTED 全实体加载/类别懒加载），countExecuted 用 findAllByQuery().size()

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteDepreciationProcessor.java#executeDepreciation`（每资产：L45 `requirePeriodOpen` → `findPeriod` 1 查；L54 `findSchedule` 1 查；L71 `countExecuted` **全实体加载**；L47 `asset.getCategory()` 懒加载 1 查）+ `ErpAstDepreciationScheduleProcessor#countExecuted`（L126-131：`dao.findAllByQuery(q).size()`——加载全部 EXECUTED 行实体仅为计数）+ `#findSchedule`（L117-124 limit 1）
- **证据**：期末结账批量路径（数千资产）每资产 ≥4 查询 + 已执行行随寿命增长线性加载（60 月寿命资产末期每次执行加载 59 行实体）。期间查询在同一批内重复 N 次（period 不变）。同型 `findLastExecutedPeriod`（L56-64）已用 `setLimit(1)` 正确示范。
- **问题**：D9 N+1 家族——折旧批跑窗口（月末）放大 DB 压力；正确性无影响。
- **建议修复方向**：批量入口预取（期间查一次传入；count 用 QueryBean count 或 `countByQuery`；类别按 categoryId 缓存/批量取）。
- **arm-index 裁决**：新增（同型家族 P3-CK-pur-011/sal-026/inv-020/mfg-014，因月末结账关键路径升 P2 站点）。

### P2-CK-ast2-013（D8）维修独立维修贷方科目分支死代码——Provider 声明「贷存货（备件）/银行（外协）」但 linkedVisit=false 一律贷 1002 银行，inventorySubject 读了不用；科目全部硬编码不读类别配置

- **控制点**：`app/erp/ast/service/posting/MaintenanceExpenseAcctDocProvider.java#createFacts`（L62-63 `String inventorySubject = readCode(event, BILL_DATA_MAINTENANCE_INVENTORY_SUBJECT_CODE, SUBJECT_INVENTORY);`——**局部变量从未被引用**；L67-71 `if (linkedVisit) { ...clearing... } else { facts.add(fact(bankSubject, "银行存款", DC_CREDIT, ...)); }`）+ `MaintenanceExpensePostingDispatcher#buildEvent`（L84-87：`billData.put(...EXPENSE_SUBJECT_CODE, "6602"); ... "2502"/"1002"/"1403"` 硬编码常量，不读类别维护科目配置）
- **证据**：javadoc（L20-24）明示「独立维修（linkedVisit=false）：贷存货（备件消耗）/银行存款（外协/人工）」。实际：消耗了库存备件的独立维修单（备件已从库存出库）贷记银行存款——GL 存货不减、银行虚减（Dr Cr 平衡，科目误分类）。`ErpAstMaintenanceCost` 有 cost type（LABOR/SPARE_PART，测试 L84-85 可证）但 dispatcher 只传 `totalCostAmount` 合计，无成本类型拆分——存货分支无数据可依，死代码是「声明意图未落地」的直接证据。
- **问题**：GL 科目误分类（用户可见报表错误）+ 死配置键。
- **建议修复方向**：按成本类型拆分贷方（SPARE_PART → 存货科目 / 其余 → 银行），billData 传分项金额；或删除存货分支声明并登记 Deferred。
- **arm-index 裁决**：新增（grep「inventorySubject/维修 存货 贷方」零命中）。

### P2-CK-ast2-014（D5）资产 CRUD 直接建卡无折旧配置校验——0/null 年限静默按 1 个月折完、残值>原值静默零提、UNITS 可选；ERR_DEPRECIATION_USEFUL_LIFE_INVALID 错误码零消费

- **控制点**：`app/erp/ast/service/entity/ErpAstAssetBizModel.java`（裸 CrudBizModel——直接 `ErpAstAsset__save_` 可建 IN_SERVICE + usefulLifeMonths=null + method=UNITS 的资产，无校验）+ `app/erp/ast/service/service/DepreciationCalculator.java#calculate`（L36 `int months = usefulLifeMonths <= 0 ? 1 : usefulLifeMonths;`——**静默**把 0/负年限当 1 个月 → SL 一个月全额折完；L33 `nbv.compareTo(residual) <= 0 → ZERO`——残值≥原值静默零折旧）+ `ErpAstErrors.java` L107 `ERR_DEPRECIATION_USEFUL_LIFE_INVALID`（grep 全 service **零消费**——错误码声明未接线）
- **证据**：资本化路径有校验（`ErpAstAssetCapitalizationProcessor#validateForApproval` L201-206 拒绝 null/≤0 年限与缺方法），但资产 CRUD 直建/改卡路径完全绕过。批量折旧对这类资产每月产生错误金额（1 个月折完→后续月 0）且无任何错误码提示。
- **问题**：D5 入参边界——折旧配置三要素（年限/残值/方法）在主数据入口无校验，错误静默传播到折旧引擎。mission D5 点名「零残值率≥1/负年限」。
- **建议修复方向**：资产 BizModel `defaultPrepareSave/Update` 校验（年限>0、0≤残值<原值、方法在 dict 内）或折旧执行前消费 `ERR_DEPRECIATION_USEFUL_LIFE_INVALID` 硬拒。
- **arm-index 裁决**：新增（grep「useful life 校验 asset CRUD」零命中）。

### P3-CK-ast2-015（D4）折旧三个死配置常量 + cron 键漂移 + owner doc 声称的 `ErpAstDepreciationJob` 类不存在

- **控制点**：`app/erp/ast/service/ErpAstConstants.java`（L20 `CONFIG_DEPRECIATION_PARALLEL_BY_CATEGORY`「默认 true」/L22 `CONFIG_RESIDUAL_VALUE_ENFORCED`「默认 true」/L24 `CONFIG_DEPRECIATION_CRON`「空=不调度」——grep 全 service 生产代码**三个常量零消费**）对照实际接线 `app-erp-all/_vfs/nop/job/conf/erp-ast-depreciation.job.yaml`（L2 `enabled: "@cfg:nop.job.erp-ast-depreciation.enabled|false"` + L7 `cronExpr: "@cfg:nop.job.erp-ast-depreciation.cron-expr|0 0 2 1 * ?"`——实际键前缀 `nop.job.*` 且 cron 默认**非空**）+ owner doc `depreciation-and-posting.md §5.1 定时作业登记`（声称「`ErpAstDepreciationJob` + `app-service.beans.xml` `<bean>` + `scheduler.yaml` 三件套接线，cron 配置键默认空=跳过门控」——grep 全仓 `ErpAstDepreciationJob` **零命中**，实际为 nop-batch job yaml + `depreciation.batch.xml` 接线，门控语义为 enabled=false 双层门控）
- **问题**：同型 `P3-CK-mfg-013`/`P3-CK-inv-021`/`P3-CK-sal-023` 家族（cron 键漂移 + 死常量）+ owner doc 类名/门控语义漂移。运维按文档键配置不生效；`residual-value-enforced`/`parallel-by-category` 声明行为不存在。
- **建议修复方向**：删死常量或接线；owner doc 修正为实际 nop-batch 三件套 + `nop.job.*` 键描述。
- **arm-index 裁决**：新增（同型家族 mfg-013/inv-021/sal-023，ast 站点）。

### P3-CK-ast2-016（D1）`java.time.YearMonth.now()` 直读系统时钟——CoreMetrics 时间可控约定违例（checker R7 盲区变体）

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor.java#recalculateForCapitalizationMaintenance`（L64-67：`java.time.YearMonth baseMonth = lastExecutedPeriod != null ? java.time.YearMonth.parse(...).plusMonths(1) : java.time.YearMonth.now();`）+ `depreciation.batch.xml` loader（`value="${java.time.YearMonth.now().toString()}"`——XML 层同型）
- **问题**：已知失败模式 #4 家族（`System.currentTimeMillis()/LocalDateTime.now()` 须 CoreMetrics）——`nop-compliance-checker.sh` R7 仅 grep `System\.currentTimeMillis`，`YearMonth.now()` 是基线外新变体。影响：时间可控测试无法固定重算基线月；job 路径跑批时点决定期间归属（凌晨 1 日 02:00 边界）。
- **建议修复方向**：Java 侧改 `CoreMetrics.today()` 推导 YearMonth；batch.xml 表达式改注入可配置参数。
- **arm-index 裁决**：新增（R7 基线内无此变体）。

### P3-CK-ast2-017（D8/D6，同型 P2-CK-inv-010 族）折旧多币种面零填充——schedule 的 currencyId/exchangeRate/amountSource/amountFunctional 四列从不写入；PostingEvent 汇率恒 ONE

- **控制点**：`app-erp-assets.orm.xml` schedule 实体（propId 16-19 `currencyId`/`exchangeRate`(默认 1)/`amountSource`/`amountFunctional` 列存在）对照 `ErpAstDepreciationScheduleExecuteDepreciationProcessor#executeDepreciation` L83-99 与 `CatchUpDepreciationProcessor` L99-113 建行/更新路径——**set 这四列的代码零处**（grep 实证仅 PostingEvent 侧 set currencyId/exchangeRate）+ `DepreciationPostingDispatcher#buildEvent` L197 `event.setExchangeRate(BigDecimal.ONE)` / `#buildCatchUpEvent` L151 同（对照 `MaintenanceExpensePostingDispatcher` L74 透传 `maintenance.getExchangeRate()`——同域已有正确范式）
- **问题**：外币资产折旧凭证源币金额失真（同 inv-010/mfg-021 家族 ast 站点）；schedule 多币种列为死列（恒默认值）。维护链已正确透传，唯折旧/VA 链硬编码。
- **建议修复方向**：`event.setExchangeRate(asset.getExchangeRate() != null ? ... : ONE)` + schedule 行落 currencyId/金额双语列（或删列登记 Non-Goal）。
- **arm-index 裁决**：同型登记（P2-CK-inv-010 汇率恒 1 家族，ast 折旧站点）。

### P3-CK-ast2-018（D6）非直线法 plannedAmount 恒 0 + 维修重算固定直线均摊不读折旧方法

- **控制点**：`app/erp/ast/service/processor/ErpAstAssetCapitalizationProcessor#plannedAmount`（L272-282：仅 `STRAIGHT_LINE` 分支返回计算值，DECLINING/UNITS 一律 `return BigDecimal.ZERO`）+ `ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor`（L62 `monthly = depreciableBase.divide(remainingMonths, 4, HALF_UP)`——不读 method，DECLINING 资产维修重算后计划行按直线展示）
- **问题**：计划额（计划折旧额列）对非 SL 方法全 0/直线口径——报表 `asset-depreciation-detail` 与看板若消费 plannedAmount 则失真（实际计提额 actualAmount 正确，故 P3）。
- **建议修复方向**：plannedAmount 补 DECLINING 递减公式或登记「planned 仅 SL 有效」约定到 owner doc。
- **arm-index 裁决**：新增。

### P3-CK-ast2-019（D5，疑似设计分歧只登记不裁决）残值配置面缺失——资本化强制 residual=0，类别/资本化单均无残值输入列，设计 §1.4 残值约束在主路径形同虚设

- **控制点**：`app/erp/ast/service/processor/ErpAstAssetCapitalizationProcessor#createAndActivateAsset`（L223 `asset.setResidualValue(BigDecimal.ZERO);`——硬编码）+ `app-erp-assets.orm.xml`：`ErpAstAssetCapitalization` 实体无 residualValue 列（L645-715 列集实证）、`ErpAstAssetCategory` 无残值率列（L257-322 列集实证）
- **证据**：owner doc §1.3 全部公式含残值、§1.4「折旧后的账面净值不得低于残值」、§6.1 科目映射表外的设计输入面；实现上资本化路径资产残值恒 0（除非事后手改资产卡）——全部资产折至 0 而非保留残值。§十 实现约定未登记此简化。
- **建议修复方向**：资本化单/类别补残值输入（ORM 变更走 dual-agent-approval）或 owner doc §十 登记 Non-Goal。
- **arm-index 裁决**：新增（grep「残值 强制 0/残值率」零命中）。

### P3-CK-ast2-020（D8）currentValue 字段语义漂移（折旧从不维护、5 处 writer 3 种语义）+ depreciationRate 死列

- **控制点**：`setCurrentValue` 全 writer 清单（grep 实证）：资本化=原值（CapitalizationProcessor L222）/VA=新净值（ValueAdjustmentProcessor L234、L263）/维修=原值±增量（MaintenanceProcessor L96/L114）/盘点=评估值（InventoryProcessor L240）/分割=行成本（SplitProcessor L377）——**折旧执行/红冲/补提三链从不维护 currentValue**（累计折旧与净值增减时 currentValue 停留旧值）；`depreciationRate` 列（asset propId 12）grep 全 service 零消费
- **问题**：字段语义未定义导致消费方无从判断口径（当前无 GL 消费故 P3）；死列。
- **建议修复方向**：裁决 currentValue 语义（建议=净值镜像并在折旧链同步维护）或删除；depreciationRate 删列（dual-agent）或接线。
- **arm-index 裁决**：新增。

### P3-CK-ast2-021（D7，同型 P2-CK-inv-012/P1-CK-pur-002 族）折旧凭证 REQUIRES_NEW 先于主事务剩余步骤提交——乐观锁/回滚窗口留孤儿凭证

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleExecuteDepreciationProcessor.java#executeDepreciation`（L116 `tryPost`（凭证 REQUIRES_NEW 已提交）→ L117-124 `findSchedule` + `saveOrUpdateEntity(schedule)`（乐观锁可抛）仍在主事务内）对照 `ErpFinVoucherBizModel` L74-75（post `@Transactional(REQUIRES_NEW)` 实证）
- **问题**：主事务回滚（如 markPosted 阶段乐观锁冲突）时：schedule/资产回滚、凭证滞留 GL（billHeadCode asset#period）；重跑时 tryPost 幂等命中返回 null（`ErpFinPostingProcessor#process` L139-143 实证）→ posted 永不置 true（fin-003 受影响面）且金额若重算不同则 GL 与 schedule 分叉。同型家族 ast 站点登记。
- **建议修复方向**：随 fin-003 三态化 + 编排顺序调整（markPosted 移入 tryPost 前/后置对账告警）联合裁决。
- **arm-index 裁决**：同型登记（P2-CK-inv-012/P1-CK-pur-002 族，ast 站点）。

### P3-CK-ast2-022（D2）catchUpDepreciation 补提凭证失败后重跑同参数静默 no-op——全 EXECUTED 跳过导致凭证重试通道不存在；total=0 时行留 posted=false

- **控制点**：`app/erp/ast/service/processor/ErpAstDepreciationScheduleCatchUpDepreciationProcessor.java#catchUpDepreciation`（L87-92 已 EXECUTED 期 `continue` 跳过 → L132-134 `if (created.isEmpty()) return created;`——**不再尝试补发凭证**；L142 `if (total.signum() != 0)` 才 tryPostCatchUp——全零金额行留 EXECUTED+posted=false）
- **证据**：首次补提成功落行但凭证失败（posted=false，告警已发）→ 运营按 G4 文档重跑 catchUpDepreciation 同参数 → 全部跳过 → 返回空列表无错无凭证——重试预期落空。可自愈路径存在但非直觉：逐期 `reverseDepreciation`（posted=false 跳过 GL 红冲、回退资产）后再重跑 catchUp（owner doc §7.2 已文档化 reverseDepreciation 自愈，故 P3）。对已结账漏提期，`executeDepreciation` 自愈被 `requirePeriodOpen` 拦（红冲后期间 CLOSED 时该路径不可达）。
- **建议修复方向**：created 为空但存在 posted=false 的 CATCHUP 目标行时重发汇总凭证（或返回值携带提示）。
- **arm-index 裁决**：新增（RC-R1.52 实现注记未覆盖失败重试通道）。

### P3-CK-ast2-023（D9/D8）折旧明细报表与看板数据面全表加载 + 无 orgId 过滤

- **控制点**：`app/erp/ast/service/report/ErpAstReportBizModel.java#loadAssets`（L289-294：仅 categoryId 过滤 + `findAllByQuery` 全实体加载，无 orgId 无分页）+ `#aggregatePeriodDepreciation`（L305-316 全 schedule 行载入内存聚合）对照 asset/schedule 均有 orgId 列（ORM 实证）；看板 `ErpAstDashboardBizModel` L62-75 同型（Σ 全资产，净值 = 原值−累计折旧 口径与 VA 调整后净值失配——见 006 关联）
- **问题**：同型 `P2-CK-mfg-007/014` 家族 ast 站点（报表/看板读路径，单组织部署无影响故 P3）。
- **建议修复方向**：查询下推 orgId + 聚合列；多组织部署前必须补。
- **arm-index 裁决**：新增（同型家族 mfg-007/014）。

## 跨域关联影响面注记（不新建 finding）

| 已登记 finding | ast 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-fin-003`（post() 幂等命中返回 null vs dispatcher「null=失败」） | ast 域 **6 个过账站点**全部 `voucherId != null` 才置 posted：`DepreciationPostingDispatcher#tryPost/tryPostCatchUp`、`CapitalizationPostingDispatcher#tryPost`、`DisposalPostingDispatcher#tryPost`、`MaintenanceExpensePostingDispatcher#tryPost`、`MaintenanceCapitalizationPostingDispatcher#tryPost`、`ValueAdjustmentPostingDispatcher#tryPost`。幂等命中窗口（P3-CK-ast2-021 场景 + sweep 补凭证后）posted 永不回 true，reverse/markPosted 守卫连锁封死 | **同型受影响面确认**：随 fin-003 三态化修复时联动核全部 6 站点 |
| `P1-CK-fin4-001`（银行 FX 重估本期 vs 累计口径混用） | 折旧凭证金额口径检查：`DepreciationPostingDispatcher#buildEvent` L203 传 `schedule.getActualAmount()`（**单期**口径）；schedule.accumulatedDepreciation/netBookValue 为累计快照列，仅存储展示、不参与凭证金额计算 | **证伪（无同型）**：折旧链无「本期聚合 vs 累计余额」混用 |
| `P1-CK-pur-003`（CRUD update 无已审守卫全域同型） | assets 全实体裸 CrudBizModel（Asset/DepreciationSchedule/Disposal/Capitalization/Cip/Maintenance/ValueAdjustment/Split/Merge——`defaultPrepare*` 零覆盖实证） | **同型登记为 P2-CK-ast2-011**（含折旧行手改三方失同步子项） |
| lesson 09 B1（期末结账 preCheck 扫描 assets 折旧 G4 兜底面） | `ErpFinAccountingPeriodProcessor#runDepreciation`（L194-201：NopException rethrow 阻断结账 + bizObjectManager 解析失败 warn 容错）——G3 分级落地 | **验证在位**：期末结账折旧失败带病关闭被阻断，无需新登记 |
| `P2-CK-fin-006`（sweep 重试无源单有效性校验） | assets 侧无红冲反写监听使 sweep 的 REVERSAL 重试在 assets 域产生业务悬挂（见 P1-CK-ast2-005） | **关联确认**：fin-006 修 source 校验时应同时核 assets listener 缺位 |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描（本切片 Java 面）**：折旧/过账/维修/VA 切片文件 `@Inject private`=0（全部包级可见）、`System.currentTimeMillis/LocalDateTime.now/new Date()`=0（`YearMonth.now()` 一处见 P3-CK-ast2-016）、非 NopException 业务异常=0、字典字符串 `==` 比较=0（全部 `Objects.equals`）、无手编生成产物。
- **同资产同月双折旧守卫在位（D3，mission 点名）**：`UK_AST_DEPRECIATION_ASSET_PERIOD (assetId, period, delVersion)`（orm 实证）+ `UniqueConstraintHelper.isUniqueConstraintViolation` 转译 `ERR_AST_DEPRECIATION_ALREADY_EXECUTED`（ExecuteDepreciationProcessor L107-112、CatchUp L114-123）——P1-MA2-089 修复（R1.28）在位验证；资产/计划行 `versionProp="version"` 全实体实证（乐观锁 D5 在位）。
- **折旧过账失败告警在位（B1/G4，P1-MA4-013 修复验证）**：`DepreciationPostingDispatcher#tryPost` catch → `dispatchFailureAlert` → `IErpSysNotificationBiz.notify("ast.depreciation-posting-failure")` + notify 失败降级 warn（L54-93）；补提同型镜像（L102-141）。
- **期间守卫链在位**：`requirePeriodOpen`（期间不存在/CLOSED 拒绝）在 execute/executeBatch/catchUp 主路径；CLOSED 期间红冲被引擎 `resolveOpenPeriod` 间接拦截（红冲失败上抛中止 mutation，资产侧不回退——顺序正确：先红冲后回退，ReverseDepreciationProcessor L51-57）。
- **IDLE 不折旧在位（RC-R1.54）**：批量查询 `eq(status, IN_SERVICE)` + `validateAssetInService` 双保险；catchUp/处置补提同守卫。
- **幂等重执行语义正确（设计 §5.1「先冲销已执行凭证再重新生成」）**：`wasExecuted && posted → dispatcher.reverse`（硬前置失败上抛）→ 旧额回退 → 重算 → `posted=false/voucherId=null` 重置 → 重发凭证；posted=false 残留态重算路径数值自洽（旧额先减后加）。
- **残值上限截断收敛尾差（D6）**：`DepreciationCalculator` L71-74 cap 使 SL 尾月自动收敛到残值（总量恰为 原值−残值，无尾差残留）；DECLINING 最后 24 个月切直线 + `remaining<=0→1` 兜底（L41-51）符合设计 §1.3；负结果归零（L74）。维修重算计划行尾月公式（`depreciableBase − monthly×(n−1)`）在位（Recalculate L72-74）。
- **期末结账/反结账接线正确（D4/G3）**：`runDepreciation` config 门控 + impl 未就绪容错 + 真实故障阻断；反结账仅冲销 `posted=true` 行（`eq("period", code), eq("posted", true)`），REVERSED/未过账行不重复处理。
- **nop-batch job 三件套实际在位**：`erp-ast-depreciation.job.yaml`（enabled 默认 false + cron 默认 `0 0 2 1 * ?`）→ `nopBatchTaskRunner.executeAsync` → `depreciation.batch.xml`（loader PENDING+当期 → processor `executeDepreciation`，chunk 事务 batchSize 50）——功能接线完整（类名/键漂移见 P3-CK-ast2-015）；period 格式 `yyyy-MM`（`PERIOD_FMT` 实证）与 `YearMonth.toString()` 一致。
- **折旧计划起始月（job 路径）正确**：资本化 `start = capitalizationDate.plusMonths(1)` 生成 PENDING 行（L246），job 按 PENDING 行迭代自然满足「次月提」（结账批量路径缺陷见 P1-CK-ast2-003——两入口分裂正是 finding 证据）。
- **catchUp 守卫链完整**：IN_SERVICE + currentPeriod OPEN + 漏提期可解析且不晚于 currentPeriod（`ERR_DEPRECIATION_CATCHUP_PERIOD_INVALID`）+ 去重升序 + 已 EXECUTED 幂等跳过（不双计）+ 汇总凭证 `#CATCHUP` 后缀与常规凭证键无碰撞 + 行 memo「补提 {periods}」审计标注（RC-R1.52 注记逐项核对在位）。
- **VA approve 凭证门控正确**：`applyAssetValueChange` 仅 `voucherId != null` 执行（L85-87）——过账失败资产净值不动，reverseApprove（posted=false 分支）跳过 GL 红冲与回退，重提交可重试（对照维修路径 P2-CK-ast2-009 的缺陷，VA 范式正确）。
- **处置补提接线与 owner doc 一致（传导面）**：`catchUpDepreciationToDisposalPeriod`（DisposalProcessor L283-309）IN_SERVICE-only、`(lastExecuted, disposalPeriod]` 含当期、复用 catchUp mutation——与 RC-R1.52 注记一致。**注记（疑似设计分歧只登记不裁决）**：设计 §折旧调度规则「处置/报废的当月停止计提折旧（当月减少当月停）」与代码「补提至出售期**含当期**」（及 L1 UC-AST-05 ⑤「补提当期折旧至出售日」）文字矛盾——代码遵循 L1 需求，设计 §5.1 该行疑似陈旧，归 C4.4 裁决后修订 owner doc。
- **跨域 daoFor 只读豁免已裁决**：`findPeriod daoFor(ErpFinAccountingPeriod)` / `resolveSubjectCode daoFor(ErpMdSubject)` / `AcctSchemaResolver`——P1-MA4-015 经 data-dependency-matrix 裁决 resolved，不按 D1 反模式重复登记。
- **beans 接线完整**：`erp/ast/beans/app-service.beans.xml` 101 bean——折旧 5 per-mutation Processor + posting 全族（Executor/10 Dispatcher+Provider）逐 id 命中。
- **折旧凭证科目方向与金额守恒**：`DepreciationAcctDocProvider` Dr 折旧费用（类别 expenseSubjectId）/ Cr 累计折旧（类别 depreciationSubjectId），等额双腿，GL 映射键在位（A1）；科目回退 6602/1602 与 owner doc §6.1 示例一致（资产级别覆盖设计 §6.2 无 ORM 支撑，随 P3-CK-ast2-019 一并属设计输入面缺失，不单列）。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `工作量法 UNITS`/`当月增加下月提`/`recalculateForCapitalizationMaintenance 基数`/`cancelSchedules EXECUTED`/`ReversalListener assets`/`applyAssetValueChange originalValue`/`countExecuted elapsed`/`maintenance post 死锁`/`newDepreciableBase 死代码`/`inventorySubject`/`YearMonth.now`/`depreciation-cron 键`/`ERR_DEPRECIATION_USEFUL_LIFE_INVALID` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - 并发首次折旧重复 schedule 行 → **P1-MA2-089**（resolved R1.28，UK 在位验证）——不登记。
  - 折旧 dispatcher posted=false 悬挂无告警 → **P1-MA4-013**（resolved R1.16，notify 在位验证）——不登记；维修/VA 站点无告警为**新增** P2-CK-ast2-009（Cap/Disposal 已被 P1-MA2-060 覆盖）。
  - 方式B 补提缺失 → **P1-RC-029**（resolved RC-R1.52，实现+测试在位验证）——不登记；其失败重试通道缺口为新增 P3-CK-ast2-022。
  - 折旧异常路径测试空洞 → **P1-MA4-014/P1-MA5-011**（归并，随 parent 闭合）——不登记（本报告补充：TestErpAstMaintenance 未断言计划总额，作为 002 证据引用）。
  - daoFor 跨域只读 → **P1-MA4-015**（resolved，data-dependency-matrix 豁免）——不登记。
  - 失败资产独立重试 API / 汇总凭证 → **P2-RC-025 / P2-RC-026**（documented simplification / watch-only）——不登记。
  - IDLE 死状态 → **P1-MA2-061**（resolved RC-R1.54，suspend/resume + IN_SERVICE-only 折旧在位验证）——不登记。
  - CRUD 无守卫 → **P1-CK-pur-003 族**——P2-CK-ast2-011 同型登记。
  - 单事务逐单吞异常 → **P3-CK-pur-013 / P3-CK-mfg-017 族**——P2-CK-ast2-008 同型站点升级。
  - REQUIRES_NEW 凭证先提交 → **P2-CK-inv-012 / P1-CK-pur-002 族**——P3-CK-ast2-021 同型登记。
  - 汇率恒 1 → **P2-CK-inv-010 族**——P3-CK-ast2-017 同族站点。
  - cron 键漂移/死配置 → **P3-CK-mfg-013 / P3-CK-inv-021 / P3-CK-sal-023 家族**——P3-CK-ast2-015 同型站点。
  - 声明配置零消费 → **P2-CK-mfg-008 家族**——P2-CK-ast2-010 同族站点。
  - 报表/看板全表加载无 orgId → **P2-CK-mfg-007/014 家族**——P3-CK-ast2-023 同族站点。
  - sweep 无源单校验 → **P2-CK-fin-006**——关联注记（P1-CK-ast2-005 独立控制点为 assets listener 缺位）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 6 | P1-CK-ast2-001..006 |
| P2 | 8 | P2-CK-ast2-007..014 |
| P3 | 9 | P3-CK-ast2-015..023 |

按主维度：D6×5（001/002/003/007/018）、D8×6（004/005/006/013/017/020）、D2×3（008/009/022）、D4×2（010/015）、D5×3（011/014/019）、D9×2（012/023）、D1×1（016）、D7×1（021）。（跨维度：003 跨 D4、005 跨 D2、008 跨 D7、009 跨 D3、011 跨 D3、017 跨 D6、021 跨 D8、023 跨 D8。）

跨域关联注记 5 项（fin-003 六站点受影响面确认 / fin4-001 证伪无同型 / pur-003 同型登记 011 / lesson09 B1 验证在位 / fin-006 关联确认）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 24 文件全读（折旧 5 Processor + facade + calculator + 状态机 + BizModel + 10 posting 类 + 维修 post/reverse/facade step + VA processor 全文）；跨域源码实证 8 处（`ErpFinPostingProcessor.process` 幂等 null 语义与 recordPostFailure/recordReverseFailure、`ErpFinVoucherBizModel` REQUIRES_NEW、`ErpFinDeferredPostingRetryHelper` 无域回调、`ErpFinAccountingPeriodProcessor` 结账/反结账接线、`IErpFinVoucherReversedListener` 全仓实现清单、`ErpFinPostingExceptionRecorder`/sweep batch.xml、`IErpFinVoucherBiz.post` 事务注解）；orm 核对（schedule/asset/category/capitalization 列集 + UK + versionProp + dict）；beans.xml 101 条 + job yaml + batch.xml 接线；测试面抽查（TestErpAstMaintenance 断言口径、TestErpAstCatchUpDepreciation 存在性）；处置/逆资本化/盘点与折旧交点。
- **未深查**：资产盘点链（`ErpAstInventory*` 8 Processor + `AssetInventoryPostingDispatcher/Provider`——路线图名义挂 C4.5「/盘点」，本次指令范围未含，**需主 agent 裁决归入 C4.4 或补查**）；`MaintenanceCapitalizationAcctDocProvider/Dispatcher` 本体（仅读 post/reverse 编排面，科目分解未逐行——与 013 同范式推断，修复阶段顺带核）；`erp-ast-web` AMIS 页面契约 drift（归 C8.2）；报表 xpt 模板与看板 page.yaml（仅读 Java 聚合层）；测试代码正确性（43 文件仅交叉验证语义）；CIP 转固对折旧的传导（归 C4.4，`ErpAstCipTransferToAssetProcessor` 未读）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-ast2-005**（assets 无红冲反写监听 + sweep 异步红冲死锁）——触发链依赖「引擎对 REVERSAL 失败的记录 + sweep 重试成功」时序（需红冲瞬时失败后期间重开等条件）；若产品语义是「assets 红冲永不入 sweep」（引擎按 businessType 过滤的隐藏配置未发现），则降 P2。建议用一次「红冲失败→重开期间→sweep」集成测试实证。
  2. **P1-CK-ast2-003**（资本化当月即提）——若产品裁决「期末结账统一补齐口径优先于当月增加下月提」（即有意让结账路径补提当月），触发面收窄为「期间内资本化 + 当月关账」仍违反设计文字；owner doc §折旧调度规则 是当前真相源，建议按其裁决。
  3. **P2-CK-ast2-008 升级面**（session 中毒连锁/整批回滚 + 孤儿凭证群）——Nop ORM 失败 flush 后 session 行为（残留脏实体是否阻塞后续 flush / 提交是否必炸）需平台源码级确认（`nop-entropy` `orm/session` 模块），影响 P2 定级是否升 P1。
