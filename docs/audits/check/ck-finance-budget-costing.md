# ck-finance-budget-costing — finance「预算与成本」切片实现代码检查报告

> 工作项：C3.3。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-finance/erp-fin-service/src/main/java` 预算与成本切片 19 个手写生产文件——`budget/` 包全 5 类（`ErpFinBudgetControlBiz` 预算控制引擎、`ErpFinBudgetCommitmentBizModel` 承付 SPI、`BudgetVoucherGenerator`/`CommitmentVoucherGenerator` 影子凭证生成器、`ErpFinBudgetScenarioProcessor` facade）+ `entity/ErpFinBudget{Scenario,Line,ControlLog,RollforwardLog,CarryForwardLog}BizModel` 5 类 + `processor/ErpFinBudgetScenario{SubmitForApproval,Approve,Reject,Cancel,RollForward,CarryForward}Processor` 6 类 + `statemachine/ErpFinBudgetScenario{Document,Approval}StateMachine` 2 类 + `posting/CommitmentAcctDocProvider`。边界核对：`ErpFinExpenseClaimProcessor#runBudgetCheckHook`（check 消费侧）、purchase `ErpPurOrderProcessor:223`/`ErpPurPaymentProcessor:189` check 调用、sales `ErpSalOrderProcessor` 承付 hook（跨域关联裁决）、`ErpFinAccountingPeriodProcessor#recloseInvCosts`（finance→inventory costing 调用边界）、`close/CloseVoucherWriter`（结账凭证 postingType/金额符号实证）、`ErpFinReportBizModel#buildBudgetVsActualDataset`（报表接线）、`_vfs/erp/fin/beans/app-service.beans.xml`（bean 注册）、`model/app-erp-finance.orm.xml`（versionProp/UK/dict）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。19 文件全量逐行深读 + 平台源码验证（`nop-entropy/nop-persistence/nop-dao/.../IEntityDao.java` getEntityById null 语义）+ 凭证行金额符号链实证（CloseVoucherWriter/ErpFinPostingProcessor persistVoucher 写行代码）+ arm-index 复用裁决。
> 切片边界：过账引擎/凭证状态机归 C3.1（`CommitmentVoucherGenerator` 红冲行 dcDirection 语义已登记 P3-CK-fin-018 不重复）、AR/AP 归 C3.2、期间结账归 C3.4（`recloseInvCosts` 仅核调用边界）、inventory 域 StockMoveBookkeeper/CostingStrategy 归 C2.3（已查）；成本中心 GL 分摊（`ErpFinGlDistributionValidator`）在 C3.1 已验证（Σpercent 守卫 + scale 4 HALF_UP + 末行补差），本切片从成本分摊视角复核一致不重复登记。costing-methods finance 侧服务（StandardCostResolver 在 inventory、CostRollupService 在 manufacturing）不在本切片，仅核 finance 调用它们的唯一边界 `recloseInvCosts`。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-fin3-001（D6/D8）carryForward 结转预算额度传递链三重断裂——结转行科目指向 Scenario id、结转凭证 Dr/Cr 双行同科目净额恒 0、期间挂源年度，结转额度永不参与目标方案预算

- **控制点**：`app/erp/fin/service/processor/ErpFinBudgetScenarioCarryForwardProcessor.java#appendCarryForwardLines`（L277-289）+ `#writeCarryForwardVoucher`（L293-363）+ `#resolveFirstPeriodId`（L366-374）
- **证据**：
  1. 结转预算行：`cl.setSubjectId(source.getId()); cl.setSubjectCode("CARRY-FORWARD-" + source.getCode());`（L282-283）——**subjectId 写入的是源 Scenario 实体的 id（非 `ErpMdSubject` 科目 id）**，subjectCode 是编造串而非真实科目编码；且不设 `periodId`（L277-289 全段无 setPeriodId）。
  2. 结转凭证：Dr 行与 Cr 行**同一 subjectId（同为 `source.getId()`）同金额**（L319-355：`d.setSubjectId(source.getId())` + `d.setDebitAmount(carriedAmount)`；`c.setSubjectId(source.getId())` + `c.setCreditAmount(carriedAmount)`）。
  3. 凭证期间：`resolveFirstPeriodId(source)`（L298）取**源方案**第一个 BudgetLine 的 periodId（源年度期间），非目标年度。
- **问题**：owner doc `budget.md §结转算法` 步骤 5/6：「在目标 Scenario 增补 BudgetLine（按源 Scenario 的 subjectId × costCenterId 维度合并）」「生成结转凭证（postingType=BUDGET）写入目标 Scenario 关联的 VoucherLine」。实现的三重失效：
  - (a) 结转行 subjectId 指向 `erp_fin_budget_scenario` 表 id，预算控制 `ErpFinBudgetControlBiz#findMatchingBudgetLine`（`eq("subjectId", subjectId)` 匹配真实科目）与预算报表 `getBudgetVsActual`（按凭证行 subjectId 聚合）**永不命中**该行；目标方案 approve 时 `BudgetVoucherGenerator#toFact` 的 `loadSubject(scenarioId)` 在科目表查不到 → `subject == null` → 行被跳过（`BudgetVoucherGenerator.java:186-188`），结转行**不生成 BUDGET 凭证**；
  - (b) 即使科目命中，结转 BUDGET 凭证 Dr/Cr 同科目同金额，`ErpFinBudgetControlBiz#aggregateAmount` BUDGET 通道按 `debit − credit`（DEBIT 方向科目，L147）净额**恒为 0**——凭证无预算语义；
  - (c) 凭证 periodId 挂源年度期间，与目标年度预算控制期间不匹配。
  综合：**carryForward 的预算额度传递完全失效**——结转后目标方案的可用预算不含结转金额，仅 CarryForwardLog 记录了 carriedAmount 数字。E2E 未覆盖：`budget.md §浏览器层验证（A2）`（L257）自述断言仅「carriedAmount 派生数值 + docStatus=CLOSED + closedAt + Log 写入」，未断言结转行科目/目标方案 approve 后预算可用性，测试绿与断链不矛盾。config-gated（`budget-carry-forward-enabled` 默认 false）为非 P0 依据。
- **建议修复方向**：`appendCarryForwardLines` 按源方案 BudgetLine 的（subjectId, costCenterId）维度逐行增补（真实科目 id + 目标年度 periodId 经 year+1 重映射）；`writeCarryForwardVoucher` 改单边 BUDGET 凭证（对齐 `BudgetVoucherGenerator` 范式：按科目 direction 记 Dr 或 Cr 一侧）挂目标年度期间；或直接复用 `BudgetVoucherGenerator` 写凭证。修复时补「结转后目标方案 approve → BUDGET 凭证含结转金额」断言。
- **arm-index 裁决**：新增（grep arm-index `appendCarryForwardLines`/`writeCarryForwardVoucher`/`CARRY-FORWARD-`/`setSubjectId(source.getId` 全部 **0 命中**；A2.16 承付审计与 A1.2 预算 UC 审计均未覆盖 carryForward 内部实现）。

### P1-CK-fin3-002（D6）carryForward 余量公式漏减 commitment——`sourceRemaining = budget − actual` 两量口径，owner doc 与控制引擎均为三量 `budget − actual − commitment`

- **控制点**：`app/erp/fin/service/processor/ErpFinBudgetScenarioCarryForwardProcessor.java#carryForward`（L62-65）+ `#aggregateSourceAmounts`（L179-191）
- **证据**：`BigDecimal sourceRemaining = sourceBudget.subtract(sourceActual);`（L65）——`aggregation` map 仅含 `budget`/`actual` 两键（L187-190），`aggregateActualForLine` 排除 COMMITMENT（L221-222，A2.16 §4.8(5) 确认「正确排除」），但**没有任何代码聚合 COMMITMENT 通道参与余量计算**。
- **问题**：owner doc `budget.md §结转算法` 步骤 4 明确「REMAINING_FULL：carriedAmount = budget − actual − **commitment**」「REMAINING_RATIO：carriedAmount = (budget − actual − **commitment**) × ratio」；§commitment 与结转（L253）：commitment「与 actualAmount 合并记录在源 Scenario 的**余量计算**中」。控制引擎 `ErpFinBudgetControlBiz#check` 已是三通道显式三项式（`available = budgetBalance − actualBalance − commitmentBalance`，P1-MA2-084 fix 后 L78-81）。carryForward 两量口径下，未释放的采购承付（PO 已 approve 未开票）占用不减少余量 → **结转金额虚高**（REMAINING_FULL/RATIO 两规则均受影响），下年度预算被高估。与 P1-MA2-084 同族（该 finding 修了控制引擎、carryForward 姊妹站点未同步修，同 RC-R1.1/P1-RC-003 范式）。
- **建议修复方向**：`aggregateSourceAmounts` 增加第三键 `commitment`（复用 `ErpFinBudgetControlBiz#aggregateAmount` COMMITMENT 通道的过滤口径：`eq("postingType", COMMITMENT)` + POSTED + isReversed=false），`sourceRemaining = budget − actual − commitment`；REMAINING_FULL/RATIO 分支同步。
- **arm-index 裁决**：新增（带同族注记）——P1-MA2-084（已 fix）修的是**控制引擎** actual 通道语义；本条是 **carryForward 余量公式**漏第三量，arm-index grep `sourceRemaining`/`aggregateActualForScenario` 0 命中，A2.16 §4.8(5) 仅核对了「actual 排除 COMMITMENT」未核对余量公式含 commitment。

### P1-CK-fin3-003（D6）getBudgetVsActual 实际数通道用 ΣamountFunctional 方向不敏感——期末结转凭证（postingType=NORMAL）贷方结平行计入后 actual 翻倍、availableAmount 大幅低估

- **控制点**：`app/erp/fin/service/entity/ErpFinBudgetLineBizModel.java#getBudgetVsActual`（L94-116：actual 通道 `row.setActualAmount(row.getActualAmount().add(amount))` 其中 `amount = nz(l.getAmountFunctional())`）
- **证据**：金额符号链实证——贷方行的 `amountFunctional` 为**正数**：`close/CloseVoucherWriter.java` L117-124（期末结账凭证写行）：`boolean isCredit = Objects.equals(l.dcDirection, DC_CREDIT); line.setCreditAmount(isCredit ? l.amount : ZERO); ... line.setAmountFunctional(l.amount);`（正数不分符号）；引擎 `posting/ErpFinPostingProcessor.java` L874-879 同型（按 dcDirection 分侧、amountFunctional 恒正）。且期末结账凭证 `postingType=NORMAL`（CloseVoucherWriter L96 `voucher.setPostingType(POSTING_TYPE_NORMAL)`）→ `channelOf`（L121-129）归入 actual 通道。
- **问题**：`getBudgetVsActual` 对 actual 通道行**不分借贷直接累加 amountFunctional**（L105-112），而控制引擎 `ErpFinBudgetControlBiz#aggregateAmount` 是方向感知净额（L147：DEBIT 方向科目 `debit − credit` / CREDIT 方向 `credit − debit`）。后果：期末结账后损益类科目（费用 6xxx/收入 6xxx）被结转凭证（贷方结平/借方结转，NORMAL）反向冲平，净额应为 0，但报表 actual = 正向发生额 + 结转额 = **2×发生额**；`availableAmount = budget − actual − commitment`（L115）随之大幅低估。期初余额凭证（OPENING_BALANCE）同样混入 actual。两个聚合路径（控制 vs 报表）口径不一致——RC-R1.1（P1-RC-003 fix）修的是**通道分类**（COMMITMENT 独立三列），未覆盖**方向/符号语义**。
- **建议修复方向**：actual 通道对齐控制引擎方向感知：按 `ErpMdSubject.direction` 取 `debit − credit` 或 `credit − debit`（行内已有 `subjectCache` 可复用）；或按行 `dcDirection` 分解为有符号量再累加。修复时补「结账后 actual 净额归零/期末不受结转凭证影响」断言。
- **arm-index 裁决**：新增（带同族注记）——P1-RC-003/RC-R1.1（已 fix）修的是 `getBudgetVsActual` 三通道分类 + DTO commitmentAmount 字段，同一方法不同缺陷维度（方向语义）；arm-index grep 该方法 5 处命中均为通道分类维度，无方向语义登记。

### P1-CK-fin3-004（D7）预算控制 check-then-act TOCTOU 无并发防护——HARD 控制下并发单据共享预算余量双双通过

- **控制点**：`app/erp/fin/service/budget/ErpFinBudgetControlBiz.java#check`（L71-104：`findMatchingBudgetLine` 读 → `aggregateAmount`×3 只读聚合 → 比较 → 放行）+ 消费侧同事务占用点：purchase `ErpPurOrderProcessor:223`（check 后 L223-237 同事务 commit hook 生成 COMMITMENT 凭证）、`ErpPurPaymentProcessor:189`（check 后过账 ACTUAL）、finance `ErpFinExpenseClaimProcessor#runBudgetCheckHook:241`（check 后 approve 过账 ACTUAL）
- **证据**：`aggregateAmount`（L117-148）全部为 `findAllByQuery` 只读查询，无 `FOR UPDATE`/无版本检查/无占用预扣；check 通过后不落任何占用记录（ControlLog 仅审计 BLOCKED/WARNED）；实际占用（COMMITMENT/ACTUAL 凭证）在调用方 approve 事务后置才写库——并发事务 T1/T2 各自 check 时对方的占用凭证**未提交不可见**，两事务均读得相同 available 并通过。
- **问题**：`budget.md §业务规则8`「控制级别 HARD 下：预算余量 < 0 时采购订单/付款单审核抛 NopException」——TOCTOU 窗口内 HARD 语义失效：两张 PO 各 800、余量 1000，并发 approve 双双通过 → 合计占用 1600 超支 600，无任何拦截与告警。A2.16 §7 交接 A2.17 的 4 处并发点（并发 commit 无幂等/并发 release 双红冲/部分开票并发/Voucher versionProp）均为**承付写侧**；A2.17 已审 finding 清单（P0-MA2-020、P1-MA2-085~092、P2-MA2-074/075）不含预算 check 读侧 TOCTOU——本控制点未覆盖。P2-MA2-074（全域无悲观锁 watch-only）是通用观察，非本控制点登记。
- **建议修复方向**：任一：(a) check 时对命中预算行 `SELECT ... FOR UPDATE` 串行化同维度并发（预算行是低频写实体，锁代价可控）；(b) 占用预扣表（check 通过即落 PENDING 占用记录，占用唯一性靠 UK）；(c) HARD 模式下对 BudgetLine 乐观锁 version 抖动重读二次校验。修复阶段与 purchase/sales/expense 调用侧联合裁决。
- **arm-index 裁决**：新增（grep arm-index「check 竞态/预算 check TOCTOU/预算余量 并发」0 命中；A2.17 并发审计范围清单核对不含此控制点）。

### P1-CK-fin3-005（D8）承付凭证 orgId/acctSchemaId/currencyId 硬编码 + 预算控制聚合无账套维度——多账套下承付凭证全部落账套 1、预算控制跨账套混算

- **控制点**：`app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel.java#resolveOrgAndSchema`（L164-173）+ `#resolveCurrencyId`（L153-162）+ `ErpFinBudgetControlBiz#aggregateAmount`（L117-148）+ `#findMatchingBudgetLine`（L180-197）
- **证据**：
  1. `resolveOrgAndSchema`：`return new String[]{p.getOrgId(), "1"};`（L172）——**acctSchemaId 恒 "1"**；periodId 为 null 或期间不存在时 `return new String[]{"1", "1"}`（L166/L170）——orgId 也兜底硬编码 "1"。commit 主流程 L69-74 将其直通 `commitmentVoucherGenerator.generateCommitment(..., orgId, acctSchemaId, ...)` → 凭证头 `voucher.setAcctSchemaId("1")`（CommitmentVoucherGenerator L138）。
  2. `resolveCurrencyId`：两分支均 `return "1"`（L159/L161）——承付凭证币种恒 "1" + `line.setExchangeRate(BigDecimal.ONE)`（L164），前置的期间查询（L157）是死代码。
  3. `ErpFinBudgetControlBiz#check` 签名无 acctSchemaId，`aggregateAmount` 凭证查询（L119-125）与 `findMatchingBudgetLine`（L182-189）**均无 acctSchemaId 过滤**。
- **问题**：`budget.md §业务规则7`「多账套独立：管理账有预算、税务账通常无预算——通过 ErpFinBudgetScenario.acctSchemaId 隔离」失效：(a) 承付启用 + 多账套部署时，承付凭证不分订单所属账套全部写 acctSchemaId="1"（订单金额若属账套 2，承付影子凭证挂错账套；预算报表按 acctSchemaId 过滤时承付列在账套 1 下失真）；(b) 控制聚合跨账套混算——账套 1 的 BUDGET/ACTUAL 与账套 2 的同名科目+期间+成本中心凭证互相污染余量；(c) `IErpFinBudgetCommitmentBiz.commit` SPI 契约本身不携带 acctSchemaId（budget.md §SPI 契约 L316-317 同样只有 periodId），账套维度在契约层缺席。对照 `BudgetVoucherGenerator`（BUDGET 凭证用 `scenario.getAcctSchemaId()` 正确透传）与 `getBudgetVsActual`（查询参数有 acctSchemaId）——承付链是唯一断点。跨域关联：本切片核对 `P1-CK-fin-005`（AcctSchemaResolver null → 静默成功）**不同型**——承付链根本不消费 AcctSchemaResolver（grep 零命中），是更直接的硬编码缺失。
- **建议修复方向**：`commit` SPI 契约增加 acctSchemaId/orgId/currencyId 入参（或经 periodId 解析真实账套：期间→org→`AcctSchemaResolver`）；`check` 契约增加 acctSchemaId 并在 `aggregateAmount`/`findMatchingBudgetLine` 增 `eq("acctSchemaId", ...)` 过滤；修复须与 purchase/sales 调用侧（组装 SPI 参数处）联动。
- **arm-index 裁决**：新增（grep `resolveOrgAndSchema` 0 命中；P1-MA2-095 多账套读路径双计（已修）与 P1-CK-fin-004/011 写路径账套选取为同族不同控制点——本条是承付/控制链账套维度整体缺席，未登记过）。

### P2-CK-fin3-006（D3）rollForward 目标方案 approveStatus 写入字典外值 "DRAFT"——wf/approve-status 值域仅 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED

- **控制点**：`app/erp/fin/service/processor/ErpFinBudgetScenarioRollForwardProcessor.java#createRollForwardScenario`（L119：`target.setApproveStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);`）
- **证据**：`ErpFinDocStatus.java` L14-17 定义 `APPROVE_STATUS_*` 仅 4 值（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）；orm `app-erp-finance.orm.xml` L1771 `approveStatus` 列 `ext:dict="wf/approve-status" mandatory="true"`；`ErpFinBudgetScenarioApprovalStateMachine` javadoc（L13-14）明确值域 4 值。`BUDGET_STATUS_DRAFT = "DRAFT"`（ErpFinConstants L406）是 docStatus 轴字典值。
- **问题**：滚动复制出的目标方案 approveStatus="DRAFT" 落库为 dict 外值——前端字典翻译显示空/原始码、按 approveStatus 过滤的列表查询漏掉该方案、后续 submit 时 `assertCanSubmit("DRAFT")`（若 facade 接入 approveStatus 轴守卫）将抛非法迁移（当前 validateTransition 只读 docStatus 故未拦截，行为「碰巧」可用）。docStatus=DRAFT 正确，仅审批轴镜像值错位（正确值应为 UNSUBMITTED）。
- **建议修复方向**：L119 改 `ErpFinConstants.APPROVE_STATUS_UNSUBMITTED`；补 rollForward 目标方案 approveStatus 断言。
- **arm-index 裁决**：新增（dict 死状态 lesson 10 的变体：不是死值而是**字典外值写入**；grep arm-index「approveStatus DRAFT/rollForward approveStatus」0 命中）。

### P2-CK-fin3-007（D9）预算聚合三路径全量实体加载 + 内存过滤——check 每 3 通道全量载入当期 POSTED 凭证、carryForward 逐行 N×M 载入、getBudgetVsActual 全量载入

- **控制点**：`app/erp/fin/service/budget/ErpFinBudgetControlBiz.java#aggregateAmount`（L126-127：`voucherDao.findAllByQuery(vq)` 载入全期凭证实体仅取 id，再 `in("voucherId", voucherIds)` 载行——每次 check ×3 通道）+ `ErpFinBudgetScenarioCarryForwardProcessor#aggregateActualForLine`（L215/L235：**每个 BudgetLine** 全量载入该期间 POSTED 凭证 + 按 subjectId+costCenterId 载入**全期间**凭证行后内存 `voucherIds.contains` 过滤）+ `ErpFinBudgetLineBizModel#getBudgetVsActual`（L75/L90：全量载入 POSTED 凭证 + 全量载入凭证行）
- **证据**：`aggregateActualForLine` 的行查询 `lq`（L230-234）只有 subjectId/costCenterId 条件**无 voucherId/periodId 过滤**，`for (ErpFinVoucherLine vl : vlines) if (!voucherIds.contains(vl.getVoucherId())) continue;`（L237-238）——行级全量 + O(n×m) List.contains；`aggregateSourceAmounts`/`aggregateActualForScenario` 各调一次 `facade.loadBudgetLines`（L180/L194，同事务重复查询）。
- **问题**：预算行 × 期间凭证数的乘积级加载：carryForward 一个 12 行预算方案 × 当期 2000 张凭证 = 2.4 万凭证实体 + 同量级行实体全列载入；check 在每次订单/付款/报销 approve 事务内同步执行（HARD 控制延迟直接加到审核耗时）。`in(数千 id)` SQL 也逼近数据库参数上限。数据量增长后是 N+1 级性能缺陷（对照 P2-MA4-003 dashboard/budget 载入后内存聚合归并族）。
- **建议修复方向**：聚合下推 DB——`ErpFinVoucherLine` 直接 join 凭证条件（orm 查询或 `QueryBean` 子查询）做 `SUM(debitAmount)/SUM(creditAmount)` 分组聚合；`aggregateActualForLine` 至少把 voucherId 集合下推为 `in` 过滤并消除 contains；carryForward 复用一次 loadBudgetLines。
- **arm-index 裁决**：新增（带同族注记）——P2-MA4-003 归并了「dashboard/budget 载入后内存聚合」观察，但 arm-index grep `aggregateActualForLine`/`aggregateAmount` 0 命中，check 引擎 3 通道×全量、carryForward N×M 两个具体控制点未登记；P2-CK-fin-012（C3.1，findAllByQuery().size()）同型不同站点。

### P2-CK-fin3-008（D5 跨域）预算 check 调用侧 costCenterId 恒传 null——成本中心维度预算行不可达，控制只对「无成本中心」预算行生效

- **控制点**：finance SPI `ErpFinBudgetControlBiz#findMatchingBudgetLine`（L185-189：costCenterId null → `isNull("costCenterId")` 精确匹配）+ 调用侧：purchase `ErpPurOrderProcessor.java:223`（`budgetControlBiz.check(subjectId, null, periodId, ...)`）、`ErpPurPaymentProcessor.java:189`（同型 null）、finance `ErpFinExpenseClaimProcessor.java:241`（同型 null）
- **证据**：三处消费点均硬编码 `null` 占位 costCenterId；`budget.md §ErpFinBudgetLine`（L54）成本中心是预算行的标准切分维度（「按 科目 × 期间 × 维度 切分」+ cost-center.md 凭证行辅助核算维度）。
- **问题**：预算行若按成本中心编制（如「差旅费-华东中心」行），采购/付款/报销 check 匹配 `costCenterId IS NULL` 的行 → **永不命中** → 返回 PASS 放行——成本中心维度预算控制整体不可达（静默失效，非报错）。业务单据行（采购订单行/报销行）本身可携带部门/成本中心信息但未传入。归属裁决：finance SPI 契约支持 costCenterId（签名有参、null 语义明确），**主控制点在调用侧参数装配**（purchase 域 ×2 + expense 域 ×1）；ck-purchase 已收官未登记该点，本报告代为登记并标注跨域修复归属。
- **建议修复方向**：调用侧从单据行/头解析成本中心维度传入（订单行 departmentId/costCenterId）；或 finance SPI 提供「costCenterId null 时匹配任意成本中心预算行（含维度聚合）」的显式语义并写入 owner doc。修复阶段与 purchase 域联合。
- **arm-index 裁决**：新增（grep「check costCenter null/成本中心 维度 控制失效」0 命中；P1-RC-051 是 projects 域 BudgetChecker 调用时机缺失，不同控制点）。

### P2-CK-fin3-009（D10/D2）预算控制期间解析失败静默 fail-open——periodId 为 null 时 check 匹配 IS NULL 预算行必然落空直接 PASS，无告警无日志

- **控制点**：`app/erp/fin/service/budget/ErpFinBudgetControlBiz.java#check`（L67-74：仅守卫 subjectId/amount，periodId null 不早退也无日志）+ `#findMatchingBudgetLine`（L184：`lq.addFilter(eq("periodId", periodId))`——null 时生成 `periodId IS NULL`）+ 调用侧解析 `ErpFinExpenseClaimProcessor#resolvePeriodId:257-268`（businessDate null 或越出全部期间 → return null 静默）
- **证据**：平台先例（`FilterBeans.eq(name, null)` 生成 IS NULL）已由 C1.2 校准确认；预算行 periodId 普遍非空（`BudgetVoucherGenerator` 对 periodId null 行跳过凭证生成，budget.md 设计期间为必填维度）→ `findMatchingBudgetLine` 以 `periodId IS NULL` 查询几乎必然空集 → L72-74 `return PASS`。
- **问题**：业务日期为 null / 日期不在任何会计期间（上线初期期间未建全、跨年空档期）时，**预算控制静默放行**（fail-open）——与 `P1-CK-fin-005`「失败被伪装成成功」同性质的观测缺失面：无 ControlLog、无 LOG.warn、指标不可见。`budget.md §业务规则6`「预算凭证同样受期间 glStatus 约束」隐含期间完备前提，但控制路径对期间缺失无防御。对照：承付 commit 的 periodId null 走 orgId/acctSchemaId 兜底 "1" 仍写凭证（凭证 periodId=null 落库，参与后续聚合时同样匹配不上）。
- **建议修复方向**：check 入口对 `periodId == null` 增加显式分支：HARD 模式抛 `NopException`（新增 `ERR_BUDGET_PERIOD_NOT_RESOLVED` 类错误码，含 sourceBillType/Code/businessDate 参数）或至少 LOG.warn + ControlLog 记录 SKIPPED；调用侧 resolvePeriodId 失败不再静默。
- **arm-index 裁决**：新增（grep「periodId null 静默/fail-open 预算」0 命中；与 P1-CK-fin-005 同性质不同链路——该条是过账引擎账套解析，本条是预算控制期间解析）。

### P2-CK-fin3-010（D6）预算/承付/滚动复制多币种字段失真——amountSource 被本位币金额顶替、汇率与币种恒定，源币口径不自洽

- **控制点**：`app/erp/fin/service/budget/BudgetVoucherGenerator.java#writeBudgetVoucher`（L154-158：`line.setCurrencyId(scenario.getCurrencyId()); BigDecimal rate = scenario.getExchangeRate() != null ? ... : ONE; line.setExchangeRate(rate); line.setAmountSource(f.amount); line.setAmountFunctional(f.amount);`——f.amount 取自 `budgetAmountFunctional`）+ `CommitmentVoucherGenerator#writeCommitmentVoucher`（L163-166：currencyId 恒来自 resolveCurrencyId 的 "1"、exchangeRate 恒 ONE、amountSource=amountFunctional=absAmount）+ `ErpFinBudgetScenarioRollForwardProcessor#copyBudgetLinesForRollForward`（L153-156：`tl.setBudgetAmountSource(targetAmt); tl.setBudgetAmountFunctional(targetAmt); tl.setExchangeRate(sl.getExchangeRate());`——targetAmt 是 functional 口径）
- **证据**：`budget.md §ErpFinBudgetLine`：`budgetAmountSource/budgetAmountFunctional` 双字段 + 「本位币 = source × rate」；BudgetVsActualRow 等消费侧以 amountFunctional 为主（本位币净额不受影响），但源币辅助对账（多币种预算台账、承付源币占用）依赖 amountSource×rate == amountFunctional 恒等式。
- **问题**：预算凭证行（BUDGET postingType）`amountSource` 落本位币金额而 `exchangeRate` 落编制汇率——rate≠1 时 `amountSource × rate ≠ amountFunctional` 恒等式破坏，源币列失真；承付凭证币种恒 "1"/汇率恒 1（叠加 P1-CK-fin3-005）；rollForward 复制行 source 币种金额被 functional 顶替 + 继承源行汇率（源行 rate×source=functional 的关系在复制行变为 rate×functional=functional，矛盾）。GL 本位币净额与预算控制数值均正确（都走 functional/debit/credit），故降 P2 非 P1——影响面为多币种源币对账/报表。
- **建议修复方向**：BUDGET 凭证行 `amountSource = f.amount.divide(rate)` 或直接取 BudgetLine.budgetAmountSource；承付凭证币种/汇率随 P1-CK-fin3-005 修复一并透传；rollForward 复制行分别携带 sourceAmt 与 functional（INCREMENTAL 策略对两字段同乘）。
- **arm-index 裁决**：新增（带同族注记）——P1-MA3-039/R1.9（已修）登记的是**正向过账 Provider** 双字段迁移，预算/承付/滚动三条影子凭证与复制路径未覆盖；P2-CK-fin-009（C3.1）是引擎红冲路径同型，不同站点。

### P3-CK-fin3-011（D6）BudgetVoucherGenerator.toFact 死三元——isReversal 参数在 BudgetLine 分支无效，未来按预算行红冲将不取负

- **控制点**：`app/erp/fin/service/budget/BudgetVoucherGenerator.java#toFact`（L189：`BigDecimal amount = isReversal ? l.getBudgetAmountFunctional() : l.getBudgetAmountFunctional();`——两分支相同）
- **问题**：三元表达式两分支一致，`isReversal` 在 `ErpFinBudgetLine` 分支被静默忽略。当前 `reverse()` 走 `ErpFinVoucherLine` 分支（L193-201 `amount.negate()`）行为正确，故无现行缺陷；但该死代码是「红冲应取负」意图的残留，若未来调用方以 BudgetLine 列表 + isReversal=true 走 writeBudgetVoucher（方法签名允许），红冲凭证金额将为正数（红冲变增计）。
- **建议修复方向**：删除三元直接 `l.getBudgetAmountFunctional()`，并在 `ErpFinBudgetLine` 分支头部断言 `!isReversal` 或实现取负语义。
- **arm-index 裁决**：新增。

### P3-CK-fin3-012（D10）预算链期间解析查询无排序且无 orgId 过滤——setLimit(1)/get(0) 在重叠期间与多 org 期间下归属不确定（与 P3-CK-fin-017 同型 3 新站点）

- **控制点**：`app/erp/fin/service/budget/ErpFinBudgetCommitmentBizModel#resolvePeriodId`（L145-150：`le(startDate)+ge(endDate)+setLimit(1)` 无 orderBy 无 orgId）+ `ErpFinBudgetScenarioRollForwardProcessor#remapPeriodId`（L189-196：`eq("year")+eq("month")+setLimit(1)` 无 orderBy 无 orgId）+ `ErpFinBudgetScenarioCarryForwardProcessor#isSourceFiscalYearFullyClosed`（L154-156：`eq("year", fiscalYear)` 无 orgId——多 org 期间混入 CLOSED 判定）
- **问题**：与 C3.1 `P3-CK-fin-017`（resolveOpenPeriod 无排序）同型：期间表唯一键 (code, orgId) 不排除日期重叠/多 org 同期——重叠期或第二组织的同期期间存在时，承付期间解析、滚动期间重映射结果不确定（承付凭证可能挂错期间→预算控制期间错配）；carryForward 的年度全 CLOSED 判定混入其他 org 未关账期间 → 误拒结转。
- **建议修复方向**：三站点补 `eq("orgId", ...)`（承付/结转链有 org 上下文）+ 确定性排序（startDate DESC 或 isAdjustment=false 优先）；与 P3-CK-fin-017 修复合并为「期间解析统一 helper」。
- **arm-index 裁决**：新增（同型注记 P3-CK-fin-017，新增 3 站点）。

### P3-CK-fin3-013（D2）resolveUserId 宽 catch 吞咽无日志——与 P3-CK-pur-010 同型 finance 站点

- **控制点**：`app/erp/fin/service/budget/ErpFinBudgetScenarioProcessor.java#resolveUserId`（L158-166：`try { ... context.getUserContext().getUserId(); } catch (Exception ignored) {} return null;`）
- **问题**：获取 userId 的任意异常（含非预期 RuntimeException）被静默吞掉，RollforwardLog/CarryForwardLog 的 rolledBy/carriedBy 落 null 且无诊断线索。与 purchase 报告 P3-CK-pur-010（5 处 Processor 同型）完全同型。
- **建议修复方向**：catch 收窄到预期异常类型并 LOG.debug；与 pur-010 族合并修复。
- **arm-index 裁决**：新增（同型注记 P3-CK-pur-010）。

### P3-CK-fin3-014（D1 文档）CommitmentVoucherGenerator 类 javadoc 描述与实现矛盾——注释称「Cr 行使用同一科目反向（保持平衡）」，实现是单行单边无 Cr 行

- **控制点**：`app/erp/fin/service/budget/CommitmentVoucherGenerator.java` 类 javadoc L28-30（「单边凭证（Dr 配置的承付占用科目 / Cr 配置的应付-承付科目或对侧科目）。简化实现：仅写 Dr 行（承付占用科目……），**Cr 行使用同一科目反向（保持平衡）**」）对照 `#writeCommitmentVoucher` 实际（L132-172：仅 1 行，`debit = isCredit ? ZERO : absAmount; credit = isCredit ? absAmount : ZERO`——单侧填写，凭证 totalDebit≠totalCredit）
- **问题**：单边不平衡凭证是 **by design**（budget.md §5 GL 路径排除注记 L112「承付凭证单边或平行入账」+ RC-R1.46 caveat 维持接受 + P1-RC-091 修复后 5 GL 聚合已排除 COMMITMENT），实现本身无需改；但 javadoc「Cr 行反向保持平衡」的描述与实现矛盾，误导维护者以为凭证应平衡/存在双行——与 C3.1 P3-CK-fin-018（红冲行 dcDirection 语义分裂）同属承付凭证文档/语义漂移族。
- **建议修复方向**：修正类 javadoc 为「单行单边影子凭证（凭证本身借贷不平衡 by design，靠 GL 聚合排除 COMMITMENT 保证恒等式，见 budget.md §5 GL 路径排除）」；与 P3-CK-fin-018 修复时一并统一承付凭证语义文档。
- **arm-index 裁决**：新增（A2.16 审计确认单边 by design 但未覆盖 javadoc 漂移）。

### P3-CK-fin3-015（D5/D10）rollForward 无幂等守卫 + validateNewFiscalYear 自动拆箱 NPE 风险

- **控制点**：`app/erp/fin/service/processor/ErpFinBudgetScenarioRollForwardProcessor.java#createRollForwardScenario`（L103：`target.setCode(source.getCode() + "-" + newFiscalYear)`，无「目标 code 已存在」预检）+ `#validateNewFiscalYear`（L79：`newFiscalYear <= source.getFiscalYear()`——`source.getFiscalYear()` 为 Integer 时自动拆箱）
- **证据**：`erp_fin_budget_scenario` UK `(code, orgId)`（orm L1798-1799）；重复对同一 source rollForward 同一年 → UK 冲突以 DB 原生约束异常上抛（非友好 `NopException`、错误信息无 scenario 上下文）。`fiscalYear` 列非 mandatory 语义上可能为 null（源方案未填年度）→ 拆箱 NPE。
- **问题**：边界缺失级——无数据损坏（UK 兜底防重复），但错误通道不友好 + null 年度源方案触发裸 NPE。
- **建议修复方向**：前置查重（同 orgId 下 code 已存在抛 `ERP_FIN_BUDGET_SCENARIO_NOT_APPROVED` 类语义化错误或专用码）；`validateNewFiscalYear` 对 `source.getFiscalYear() == null` 显式抛业务错误。
- **arm-index 裁决**：新增。

### P3-CK-fin3-016（D3）findMatchingBudgetLine 多 APPROVED 方案同维度命中时无排序取首个——controlLevel 取值取决于 DB 返回序

- **控制点**：`app/erp/fin/service/budget/ErpFinBudgetControlBiz.java#findMatchingBudgetLine`（L190-196：`lineDao.findAllByQuery(lq)` 无 orderBy，`for` 循环返回第一个所属方案 APPROVED 的行）
- **问题**：同 (subjectId, periodId, costCenterId) 存在于多个 APPROVED 方案（原始 ANNUAL + ADJUSTMENT 调整方案并存是 budget.md 版本链设计内的场景）时，命中哪个方案的行（及其 controlLevel：HARD/WARN/NONE）由数据库返回顺序决定——同一维度在不同时刻的 check 可能取不同控制级别（NONE 方案被选中时 HARD 失效）。budgetBalance 聚合是全部 BUDGET 凭证累计（不受影响），仅 controlLevel 与 ControlLog 关联行不确定。
- **建议修复方向**：命中多方案时确定性选择（如按 scenario id 升序 + scenarioType 优先级 ANNUAL>ADJUSTMENT>ROLLING，或 controlLevel 就严原则取 HARD 优先）；owner doc 补多方案并存语义。
- **arm-index 裁决**：新增。

## 移交项裁决结论（跨域关联，回填主 agent）

| 关联项 | 裁决 | 结论与依据 |
| --- | --- | --- |
| `P3-CK-sal-025` sales 承付 commit hook 无容错（与 release hook 容错不对称） | **finance 侧证伪，维持 sales 归属，不新建 finding** | 根因裁决：不对称性来自 commit/release 的**业务语义差异**而非 finance hook 契约缺陷——(1) finance SPI 契约与 owner doc 一致：`ErpFinBudgetCommitmentBizModel#commit`（L57-67）对参数缺失/科目不存在/金额≤0 全部**静默返回 null**（SPI 内消化守卫，无显式守卫异常），`budget.md §SPI 契约`（L314-321）只对 release 声明守卫异常 `ERR_BUDGET_COMMITMENT_ALREADY_RELEASED`；(2) release 抛守卫异常是因为「已释放」是**合法业务态**（多发票场景 budget.md §全额释放语义 L278 显式容错），调用侧 try-catch 是对契约的正确响应（purchase/sales 两域 release hook 均有容错，budget.md §release hook 容错对称性 L407 记录）；(3) commit 是占用动作，其失败（DAO/约束/运行时异常）应 fail-closed 同事务回滚——静默吞掉会导致占用丢失（预算泄漏），无 try-catch 是**正确方向**，purchase 侧 commit hook（`ErpPurOrderProcessor.runCommitmentCommitHook:223-237`）同为无 try-catch，两域对称。sales finding 的实际缺陷面仅是 javadoc「不阻塞业务流」措辞与传播行为不符（P3-CK-sal-025 原文已如此定性），finance 侧无控制点。 |
| `P1-CK-fin-005` AcctSchemaResolver null → 静默成功（同型核对） | **不同型，登记为独立新 finding P1-CK-fin3-005** | 预算/成本切片代码**不消费 AcctSchemaResolver**（grep 全切片 5 文件 `AcctSchemaResolver` 0 命中——承付链凭证账套不经 resolver 而是直接硬编码，预算控制链根本无账套维度）。故不存在「resolver null → 静默成功」同型路径；但存在更直接的账套透传缺失：`ErpFinBudgetCommitmentBizModel#resolveOrgAndSchema` acctSchemaId 恒 "1" + `ErpFinBudgetControlBiz#aggregateAmount` 无 acctSchemaId 过滤（多账套预算控制/承付凭证错账套），登记为 P1-CK-fin3-005（标注关联但不同型）。附带核对：`ErpFinAccountingPeriodProcessor#recloseInvCosts`（L210-234）的 impl 未就绪容错**有 LOG.warn 告警通道**且 `costing-methods.md:38` 明示 by-design（单域测试场景）+ NopException 阻断结账分级在位——与 P1-CK-fin-005 的「零记录零告警伪装成功」不同型，写入验证为正确节。 |

## 验证为正确（显式排除，防误报）

- **平台 API `getEntityById` 不存在时返回 null**（nop-entropy `nop-persistence/nop-dao/src/main/java/io/nop/dao/api/IEntityDao.java:137-142` javadoc「如果数据库中不存在，则返回null」；`requireEntityById` L150-155 才抛 UnknownEntityException）——`ErpFinBudgetControlBiz#loadSubject`/`BudgetVoucherGenerator#loadSubject`/`remapPeriodId` 的 null 检查语义正确，无「查不到抛裸异常」问题。
- **承付凭证单边不平衡 by design**：`writeCommitmentVoucher` 单行单侧（totalDebit≠totalCredit）——RC-R1.46/P1-RC-091 修复后 5 GL 聚合路径已排除 COMMITMENT（`or(isNull, notIn(BUDGET, COMMITMENT))`），budget.md §5 GL 路径排除注记（L102-114）+「承付凭证单行单边影子凭证语义 by design，caveat 维持接受」——实现与裁决一致（javadoc 漂移另登记 P3-CK-fin3-014）。
- **承付红冲 isReversed 排除式聚合自洽**：release 后原凭证与红冲凭证均 isReversed=true → 控制引擎 COMMITMENT 通道聚合归零（占用正确释放）；未释放时仅原凭证计入——数学正确（`ErpFinBudgetControlBiz#aggregateAmount` L124 `eq("isReversed", FALSE)` + `CommitmentVoucherGenerator` L88/L215 置标链）。
- **ControlLog actionResult 字典 PASS 值无 writer 是 owner doc 语义**：`budget.md §业务规则2`（L81）「PASS → 静默」——PASS 不写日志是显式设计（只审计超预算事件），非 dict 死状态（lesson 10 模式不适用）；BLOCKED/WARNED 两值 writer（`writeControlLog` L89/L97）覆盖可达。
- **预算方案状态机无双凭证路径**：`ErpFinBudgetScenario` `versionProp="version"`（orm L1753）乐观锁在位；`assertCanApprove` 仅 SUBMITTED 来源 + APPROVED 无出边（除 cancel/carryForward）+ submit 重提链（REJECTED→SUBMITTED）不经过凭证生成 → 正常状态机下 `BudgetVoucherGenerator.generate` 不会被同一方案调用两次（并发双 approve 由乐观锁 detectable）；carryForward 重复触发被 `validateCarryForwardPreconditions`（L109 docStatus=APPROVED）+ CLOSED 终态守卫拦截。
- **check/commit SPI 早期静默守卫 by design**：`check` 对 subjectId/amount null 或 ≤0 直接 PASS（L67-69）、`commit` 对 code/subject/amount 缺失或科目不存在返回 null（L60-67）——budget.md §承付会计声明语义 + A2.16 矩阵 #1 确认（amount/signum 守卫 + subject null 守卫）；期间维度的静默面另登记 P2-CK-fin3-009（未覆盖的观测缺口）。
- **`CommitmentAcctDocProvider` 空 Provider stub**：getSupportedBusinessTypes 空集 + createFacts 空列表——owner doc budget.md §CommitmentAcctDocProvider（L296-304）显式裁决「不走 Provider 路由」，A2.16 §9 确认一致。
- **`getBudgetVsActual` 恒真式过滤**：`or(eq(postingType, BUDGET), or(isNull, ne(BUDGET)))`——budget.md L113 已裁决「三通道分流在内存 per-voucher 谓词完成，非过滤缺口」，RC-R1.1 修复产物，不登记。
- **`ErpFinBudgetScenarioProcessor#save` 与两个 Generator 的 `updateEntity`**：均为 finance 同模块实体（ErpFinBudgetScenario/ErpFinVoucher）+ Processor/Generator 非 BizModel 编排 bean 场景——同域直访合法（对照 C3.1 对 `CommitmentVoucherGenerator:89` 的既有合法 bypass 裁决），非跨实体越权。
- **`recloseInvCosts`（finance→inventory costing 唯一边界）容错分级在位**：impl 未就绪 → LOG.warn 跳过（design 文档明示单域测试场景）；NopException → rethrow 阻断结账（G3 分级，与折旧门控同范式）；非 NopException 运行时异常传播不吞——与 P1-CK-fin-005 静默成功不同型（有告警通道 + by-design + 阻断分级）。
- **D1 机械扫描全零**：切片 19 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now`=0、`extends RuntimeException/Exception`=0、字典字符串 `==` 比较=0（均为 Objects.equals/equals）、无手编生成产物、bean 注册完整（app-service.beans.xml L179-196 + L284-291 全 10 bean）。
- **D4 = N/A**：module-finance 4 个 batch.xml（deferred-posting-sweep/ar-ap-auto-recon/bank-recon-auto-reverse/cash-forecast-refresh）均属其他切片；预算（rollForward/carryForward）与成本无任何调度 job/cron 声明——无调度链可查。
- **GL Distribution（成本中心分摊）**：C3.1 已验证 Σpercent≠100 抛 `ERR_GL_DISTRIBUTION_PERCENT_SUM` + 拆行 scale 4 HALF_UP + 末行补差保 Σ==原行 + getOrder=100——本切片从成本中心分摊视角复核一致（RC-R1.41 非物化裁决在位），不重复登记。
- **预算对比报表接线**：`ErpFinReportBizModel#buildBudgetVsActualDataset`（L239-242）纯委托 `IErpFinBudgetLineBiz#getBudgetVsActual`，无二次聚合偏差（缺陷本体在 P1-CK-fin3-003）。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `appendCarryForwardLines`/`writeCarryForwardVoucher`/`sourceRemaining`/`aggregateActualForScenario`/`resolveOrgAndSchema`/`CARRY-FORWARD-`/`setSubjectId(source.getId`/`budget check 竞态` 在 `docs/audits/arm-index.md` **全部 0 命中 → 新增**。
- **复用/同族注记（不重复计数）**：
  - P1-MA2-084（控制引擎三通道分离，已 fix R1.x）——P1-CK-fin3-002 是其姊妹站点（carryForward 余量公式未同步修），同 P1-RC-003/RC-R1.1「fix 未同步姊妹站点」范式。
  - P1-RC-003/RC-R1.1（getBudgetVsActual 三通道分类，已 fix）——P1-CK-fin3-003 是同一方法的方向语义维度，未覆盖。
  - P1-RC-091/RC-R1.46（5 GL 路径排除 COMMITMENT，已 fix）——验证在位（承付单边凭证安全），本切片未新登记。
  - P1-MA2-081/082/083（部分开票/退货释放/冲销恢复语义缺口，MR1 目标）——budget.md 已补 §冲销恢复语义（RC-R1.12 修复在位，ck-purchase L130 复核确认）；本切片未重复。
  - A2.16 §7 交接 A2.17 的 4 处承付并发点——P1-CK-fin3-004 是 check 读侧 TOCTOU（不在 4 点清单），A2.17 finding 清单核对不含此控制点，故新增。
  - P2-MA4-003（dashboard/budget 载入后内存聚合归并族）——P2-CK-fin3-007 是其 check/carryForward 具体控制点展开（新增站点）。
  - P3-CK-fin-017（resolveOpenPeriod 无排序，C3.1）——P3-CK-fin3-012 同型 3 新站点。
  - P3-CK-pur-010（currentUserId 宽 catch，purchase）——P3-CK-fin3-013 同型 finance 站点。
  - P3-CK-fin-018（承付红冲行 dcDirection 语义分裂，C3.1）——P3-CK-fin3-014 是其同族 javadoc 漂移面，不同控制点（类文档 vs 红冲行字段），分别登记。
  - P0-MA2-018（alreadyPosted TOCTOU，deferred）——与本切片无新增触发面（预算/承付凭证不经 alreadyPosted 路径，直接 Generator 写入）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 5 | P1-CK-fin3-001..005 |
| P2 | 5 | P2-CK-fin3-006..010 |
| P3 | 6 | P3-CK-fin3-011..016 |

按维度（主维度计）：D6×5（001/002/003/010/011）、D8×1（005，001 跨 D6/D8 计 D6）、D7×1（004）、D3×2（006/016）、D9×1（007）、D5×1（008，015 跨 D5/D10 计 D5）、D10×1（009，012 跨 D10/D6 计 D10）、D2×1（013）、D1×1（014）、D10×1（012）。D4=N/A（无预算/成本调度 job）。

跨域关联：证伪 1（P3-CK-sal-025 维持 sales 归属）、不同型转独立登记 1（P1-CK-fin-005 → P1-CK-fin3-005）。

## 剩余风险（查了什么/没查什么）

- **已查**：切片 19 文件逐行全读（含 carryForward 393 行/rollForward 217 行/两 Generator 全文）；跨域消费侧 4 点（purchase order/payment check 调用、expense check hook、sales 承付 hook 双向）；凭证行金额符号链实证（CloseVoucherWriter L117-124 + ErpFinPostingProcessor L874-879 贷方行 amountFunctional 正数 + 结账凭证 postingType=NORMAL L96）；平台源码实证 1 处（IEntityDao.getEntityById null 语义）；orm 核对（BudgetScenario versionProp/UK/(code,orgId)、approveStatus dict=wf/approve-status 4 值、BudgetLine relations）；beans.xml 注册核对；arm-index 精准 grep 复用裁决（10+ 关键符号）。
- **未深查**：`erp-fin-web` 预算/承付 AMIS view 与后端契约 drift（归 C8.2）；`ErpFinBudgetScenario` xmeta 层是否有 save 钩子拦截 approveStatus 非法值（P2-CK-fin3-006 的证伪路径——若 xmeta dict 校验在保存层拒绝则实际不可落库，但 rollForward 直写 dao 不经 xmeta 管道，落库路径成立）；测试代码（budget 相关测试类仅用于交叉验证 E2E 断言范围声明）；`ErpFinExpenseClaimApproveProcessor` 的 check 时序细节（归 C3.4 expense 切片，本切片只核 hook 本体）；`IntercompanyVoucherGenerator`（C3.4）；mfg `CostRollupService`/inv `StandardCostResolver`（他域，Q4 取值豁免裁决在位且守卫测试存在，未复核）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-fin3-001**（carryForward 断链）——「subjectId=source.getId() 查科目表必为 null」依赖两表 id 序列不碰撞的判断（ErpMdSubject 与 ErpFinBudgetScenario 均为独立 BIGINT 序列，碰撞概率≈0 但非严格不可能）；语义错误本身（Scenario id 冒充科目 id + 编造 subjectCode）无歧义，若主 agent 认为存在某种 id 复用机制则影响面结论需复核。
  2. **P1-CK-fin3-003**（getBudgetVsActual actual 翻倍）——影响面依赖「期末结账后用户仍查询该期间预算对比」的实际使用时序；若预算报表按惯例只在结账前使用则降观察项。结账凭证落 NORMAL（L96 实证）与贷方行正数（L117-124 实证）两个前提均已确证，口径与控制引擎不一致的事实本身成立。
  3. **P1-CK-fin3-004**（check TOCTOU）——严重性取决于部署形态（预算控制+承付同时启用 + 并发审核频率）；单用户低频场景触发面窄，可议降 P2。
