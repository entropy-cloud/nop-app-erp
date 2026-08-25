# ck-finance-arap — finance「AR/AP 与核销」切片实现代码检查报告

> 工作项：C3.2。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-finance/erp-fin-service/src/main/java` AR/AP 辅助账与核销切片 25 个手写生产文件——核销链（`entity/ErpFinReconciliationBizModel`、`ErpFinReconciliationLineBizModel`、`processor/AbstractErpFinReconciliationProcessor` + Create/Post/Reverse/RunAutoReconciliation 四 per-mutation Processor、`reconciliation/` 包 AutoReconciliationEngine/ReconciliationSettler/DualSideConsistencyChecker/PartnerBalanceUpdater、`statemachine/ErpFinReconciliationDocumentStateMachine`）；辅助账（`entity/ErpFinArApItemBizModel` aging/findOpenItems、`posting/ErpFinArApItemGenerator` 生命周期侧、`posting/AdvanceOffsetOrchestrator`）；坏账链（`entity/ErpFinBadDebtBizModel`、`processor/ErpFinBadDebtProcessor` + WriteOff/Recover/Approve/Reject/Submit/ReverseApprove 六 Processor、`baddebt/BadDebtProvisionCalculator`/`BadDebtProvisionService`、`statemachine/ErpFinBadDebtApprovalStateMachine`）+ `_vfs/nop/batch-task/fin/ar-ap-auto-recon.batch.xml` + `app-erp-all/.../erp-fin-ar-ap-auto-recon.job.yaml`（D4）。跨域核实：`ErpFinPostingProcessor#reverseProcess`（cancelOnReverse 唯一调用点）、sales `ReturnRefundOrchestrator`/`ReceiptSettler`、`model/app-erp-finance.orm.xml`（ArApItem/Reconciliation/Line/BadDebt 四实体 versionProp/UK/id）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。核心链逐文件深读 + 平台源码验证（`nop-entropy` QueryBean.addOrderField:433、CrudBizModel.findList:1575 javadoc「缺省不分页」）+ arm-index 复用裁决（A4.1b 曾覆盖本切片但本文 4 条 P1 控制点均为其未触及的新控制点，grep 逐条零命中）。
> 切片边界：凭证/过账引擎归 C3.1（已查，`ck-finance-posting.md`）；期间结账/费用/银行对账归 C3.4；`ErpFinArApItemGenerator` 的多账套幂等问题已由 C3.1 登记为 P2-CK-fin-011，本切片只查其辅助账生命周期侧。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-fin2-001（D6）BY_RATIO 分摊分母不随收付款迭代刷新 + 尾差守卫方向写反——多笔收付款下生成超开分摊行，整批自动核销失败或比例语义失真

- **控制点**：`app/erp/fin/service/reconciliation/AutoReconciliationEngine.java#matchByRatio`（L174 `BigDecimal totalInvoiceOpen = sumOpen(sortedInvoices);` 在 payment 循环**外**一次性计算，循环内 L201 `invoiceOpen.put(...)` 递减后分母不刷新；L209 尾差守卫 `invOpen.add(tail).compareTo(precision.negate()) >= 0`——向末行结算 `tail` 后剩余应为 `invOpen.subtract(tail)`，`add` 方向反了）
- **证据/推演**：三笔付款 500×3 对一张发票 1000：
  - P1：share=500×1000/1000=500，invOpen→500；
  - P2：share=500×**500/1000**=250（分母仍为原始 1000），tail=250，守卫 `500+250=750 ≥ -precision` 通过，invOpen→250；
  - P3：share=500×**250/1000**=125，tail=375，守卫 `250+375=625 ≥ 0` 通过 → 对 open=250 的发票生成 375 结算行——超开 125。
  - 下游二选一恶果：(a) `ErpFinReconciliationPostProcessor` 逐行 `assertNotOver`（`AbstractErpFinReconciliationProcessor.java#assertNotOver` L102-110）拒绝 → `runAutoReconciliation` 单 `@BizMutation` 事务整体回滚（**所有 partner** 的核销全部失败）；(b) tail 碰巧落在容量内时静默落库——分配结果与「按余额比例」语义完全背离。
- **测试缺口佐证**：`TestErpFinAutoReconciliation#testByRatioProportionalAllocation`（L100-121）只覆盖 **1 笔付款 × 2 张发票**（该场景分母恰好正确），多笔付款场景零覆盖。
- **影响面**：`erp-fin.auto-recon-strategy=BY_RATIO`（文档化三策略之一，UI/API 可选）+ 同 partner 同方向 ≥2 笔未清收付款即触发。
- **建议修复方向**：(1) 每笔付款迭代前重算 `totalInvoiceOpen = Σ invoiceOpen 剩余值`；(2) 尾差守卫改为 `invOpen.subtract(tail).compareTo(precision.negate()) >= 0`；(3) 补多笔付款 BY_RATIO 单测。
- **arm-index 裁决**：新增。A4.1b 审计（2026-07-28）曾将「核销三策略 FIFO/BY_AMOUNT/BY_RATIO + 尾差归末行」评估为算术扎实，但为静态判断且未覆盖多付款分支；grep arm-index「matchByRatio/分母/尾差 守卫」零命中。**建议主 agent 重点复核本条**（见剩余风险）。

### P1-CK-fin2-002（D6/D8）settleWithFx 双侧按各自汇率不对称结算，reverseSettle 却按行金额对称回滚——FX 核销红冲后付款项辅助账残留 |fxGainLoss| 偏差

- **控制点**：`app/erp/fin/service/reconciliation/ReconciliationSettler.java#settleWithFx`（L63-74：`paymentFunctional = settledSource × paymentItem.exchangeRate`、`invoiceFunctional = settledSource × invoiceItem.exchangeRate` **分别**回写双方，差额入 `head.fxGainLoss`，**两侧实际生效金额从不写回 line**）对照 `#reverseSettle`（L97-105：对双方一律 `applySettlement(item, line.getSettledAmountFunctional(), line.getSettledAmountSource(), true)`——用 create 时输入的行金额对称回滚）
- **证据**：`line.settledAmountFunctional` 只在 `ErpFinReconciliationCreateProcessor#create` L60 写入（输入值；自动引擎路径 `AutoReconciliationEngine#line` L289-297 用**发票侧汇率**折算）。汇率差场景：发票 rate=7.0、收款 rate=7.1、settledSource=1000 → post 时收款项 settled += 7100、发票项 settled += 7000；reverse 时**双方各 -= 7000**（行值）→ 收款项残留 settled=+100、open 比真实少 100、status 落 PARTIAL 而非 OPEN。FX 凭证本身经 `reverseReconFxVoucher` 正确红冲（`AbstractErpFinReconciliationProcessor` L217-222），但辅助账层不对称。
- **测试缺口佐证**：`TestErpSalMultiCurrencyReconFx`（sales 侧）只断言正向 `fxGainLoss=113`（L125-127），无 reverse 断言。
- **影响面**：`erp-fin.recon-fx-gain-loss-enabled=true`（默认 false）时，任何 FX 核销单的红冲。config 默认关闭是降 P1 非 P0 的依据；开启即真实辅助账数据损坏。
- **建议修复方向**：`settleWithFx` 将 per-side 实际生效金额持久化到行（如新增行级 payment/invoice functional 侧字段，或 reverse 时按 `settledSource × item.rate` 现算重演 settleWithFx 的逆运算）；补 FX settle→reverse→双侧 open 归位断言测试。
- **arm-index 裁决**：新增（P1-MA2-009 resolved 的是 FX plug 正向实现，reverse 对称性未覆盖；grep「settleWithFx reverse/不对称回滚」零命中）。

### P1-CK-fin2-003（D5/D6）post 校验不聚合同一辅助账项的多行累计——手工核销单多行共享同一 item 时可静默超核销（settled>amount、open 为负、状态 SETTLED）

- **控制点**：`app/erp/fin/service/processor/ErpFinReconciliationPostProcessor.java#post`（L36-39 逐行独立 `validateLine(head, line, precision)`）+ `AbstractErpFinReconciliationProcessor#validateLine`（L80-84 `assertNotOver(amt, item, ...)` 只对**单行**金额 vs item **当前** open）+ `ReconciliationSettler#applySettlement`（L107-126 无累计守卫，settled 累加后 open = amount − settled 可为负）
- **证据**：发票项 open=100，手工 `create` 两行（paymentA→invoice 60、paymentB→invoice 60）：两行各自 60 ≤ 100 校验通过 → settle 后 settled=120、open=−20、`resolveStatus`（L128-138）判 `settled>=total` → SETTLED。同一付款项出现在两行（100 open 分两行各 60）同型。自动引擎不受影响（`matchFifo` L104-131 用本地 `invoiceOpen`/`paymentOpen` map 维护累计），**手工 create+post 路径完全可达**（BizModel `create` L73-79 直收 `List<ReconciliationLineInput>`，`create` 阶段零校验——`ErpFinReconciliationCreateProcessor` L25-64 无 validateLine 调用）。
- **后果链**：settledAmountFunctional 永久大于 amountFunctional；item 被 sumOpen 排除（SETTLED）使 partner 余额丢掉全部 open；`DualSideConsistencyChecker` 恒报 INCONSISTENT。
- **建议修复方向**：post 前按 paymentItemId/invoiceItemId 分组聚合行金额，对累计值做 assertNotOver（等价于引擎的本地 map 逻辑下沉到校验层）；或 settle() 内逐行应用后即时断言 open ≥ -precision。
- **arm-index 裁决**：新增（grep「validateLine 聚合/多行 累计 校验/超核销 同一 item」零命中；P2-MA2-039 是 WRITTEN_OFF 单状态维度，不同控制点）。

### P1-CK-fin2-004（D3/D8）坏账执行体在审批/反审核时点不校验辅助账当前状态——交错时序下 settled/open 可被写穿（负 settled、虚增 open、部分核销残留）

- **控制点**：`app/erp/fin/service/processor/ErpFinBadDebtProcessor.java#approveInternal`（L154-163：按 docType 直接派发执行，**不加载/不校验 item 当前状态**）；`#executeRecovery`（L194-201：**零校验**——无 validateAmount、无 `status==WRITTEN_OFF` 断言，`settled-=amount` 可为负、`open+=amount` 可超原额）；`#executeReverseApprove`（L116-143：不校验 item 当前 status 即回写，writeOff 反向固定置 OPEN、recovery 反向固定置 WRITTEN_OFF）；`#executeWriteOff`（L169-177：无「回写后 open==0」后置断言）
- **证据（三条交错时序）**：
  1. **recovery 单悬空审批**：`recover()` 创建 RECOVERY 单（金额=`debtAmountOf`，`ErpFinBadDebtRecoverProcessor` L19-29）→ 先对原 WRITE_OFF 单 `reverseApprove`（item 回到 OPEN，settled-=amount/open+=amount）→ 再 approve 悬空的 RECOVERY 单：`executeRecovery` 无守卫照常执行 settled-=amount（此时 settled 已被反审核扣过一次 → **负 settled**）、open+=amount 虚增、status=OPEN。`ErpFinBadDebtApproveProcessor#approve` L22-30 只查 `debt.isApproved()` 与审批轴迁移，不查 item 态。
  2. **writeOff 反审核遇已恢复项**：recovery 已 approve（item OPEN）后再 reverseApprove 原 WRITE_OFF 单：executeReverseApprove 不看 item 当前态，再次 settled-=amount/open+=amount → 双重回滚污染。
  3. **部分核销残留**：writeOff 创建时 amount=当时 open（`ErpFinBadDebtWriteOffProcessor` L21-22），审批前 open 增大（如某核销单 reverse）→ approve 时 `validateAmount`（L295-304）通过（amount<open）→ `executeWriteOff` 置 WRITTEN_OFF 但 open 残留 >0——该残额永久退出账龄/计提基础（aging 与 `findReceivableOpenItems` 均只取 OPEN/PARTIAL）且不可再核销/计提（recover 只恢复 `amount` 不恢复残额）。
- **建议修复方向**：approveInternal 前按 docType 断言 item 现态（WRITE_OFF 需 OPEN/PARTIAL、RECOVERY 需 WRITTEN_OFF）；executeRecovery 补 validateAmount 对称校验（amount ≤ settled）；executeWriteOff 补 `open−amount==0` 后置断言（或残额回退 PARTIAL 前置拒绝）。
- **arm-index 裁决**：新增（A1.3 RC（2026-08-02）审的是单链 happy-path「核销/收回/反审核红冲闭环」命题，交错时序未覆盖；grep「approveInternal 状态校验/executeRecovery 无守卫」零命中）。

### P2-CK-fin2-005（D3/D8，佐证 P2-CK-pur-005 / P2-CK-sal-012）源单红冲的辅助账回滚通道（cancelOnReverse）不守卫已核销项、不级联已过账核销单——且核销单 reverse 会把 CANCELLED 项复活为 OPEN

- **控制点**：`app/erp/fin/service/posting/ErpFinArApItemGenerator.java#cancelOnReverse`（L129-140：无条件 `status=CANCELLED、openAmount=0`，**不检查 settledAmount>0**、不反查引用该 item 的核销单行）+ 唯一调用点 `ErpFinPostingProcessor` L246（reverseProcess）+ `ReconciliationSettler#applySettlement/resolveStatus`（L107-138：reverse 路径无 assertOpen 对偶——`ErpFinReconciliationReverseProcessor#reverse` L23-35 直接 `settler.reverseSettle(lines)`，resolveStatus 无条件覆写 status）
- **证据（时序）**：付款单过账生成 PAYMENT 辅助账项 → finance 核销单 A post（该项 settled=100）→ 付款单反审核（purchase 侧调 `IErpFinVoucherBiz.reverse` → engine L246 cancelOnReverse）→ item 变 CANCELLED 但 settled=100 残留、核销单 A 仍 POSTED 悬挂（其发票侧 settled 仍含这 100）。此后核销单 A reverse → reverseSettle 对 CANCELLED 项回写 → `resolveStatus` 把它**复活为 OPEN**（settled 减回后 >0 且 <total）——作废源单的辅助账重新进入核销/账龄池。
- **跨域裁决承接**：这正是 `P2-CK-pur-005`（Payment cancel/reverseApprove 不守卫不回滚核销）与 `P2-CK-sal-012`（收款反审核/作废不反向核销）在 finance 侧的控制点——finance 侧回滚通道**存在但不完整**（仅取消 item 自身，无已核销守卫、无核销单级联）。另注：sales/pales 域级核销（ReceiptSettler/PaymentSettler）完全不触 finance 侧（grep 零引用），双轨漂移面即 P2-MA2-038（watch-only）已登记的系统性问题，本条是其 finance 回滚通道的具体缺口。
- **建议修复方向**：cancelOnReverse 前置检查 `settledAmount>0` 时抛错（提示先 reverse 相关核销单）或级联自动反核销；reverseSettle 对目标 item 为 CANCELLED 时拒绝/跳过并告警（与 P2-MA2-039 修复联动把 assertOpen 镜像到 reverse 侧）。
- **arm-index 裁决**：新增（带复用注记）——P2-MA2-038（双路径无对账守卫，watch-only）是系统面；本条是回滚通道的具体控制点；P2-MA2-039 只覆盖 post 侧 assertOpen，reverse 侧复活是不同控制点。

### P2-CK-fin2-006（D8）坏账核销/收回/反审核与报销抵扣借款改写辅助账 open 后不刷新 ErpMdPartner 余额缓存——receivableBalance/payableBalance 陈旧直至该伙伴下次核销

- **控制点**：grep 全 erp-fin-service，`PartnerBalanceUpdater.refresh` 仅 2 个调用点（`ErpFinReconciliationPostProcessor` L52、`ErpFinReconciliationReverseProcessor` L33）；`ErpFinBadDebtProcessor#executeWriteOff` L172-177 / `#executeRecovery` L196-201 / `#executeReverseApprove` L129-141 与 `AdvanceOffsetOrchestrator#offset` L86-87 / `#reverseOffset` L123 均改写 open/settled 后**无 refresh**
- **证据**：坏账核销 open 100→0 后 `ErpMdPartner.receivableBalance` 保持 100（sumOpen 会算 0，但没人触发重算）；credit-limit 检查（sales `CreditLimitChecker` 引用 finance 辅助账/partner 余额）与伙伴余额页面读到陈旧值，直至该 partner 恰好发生一次 finance 核销 post/reverse。
- **关联**：`P2-RC-082`（watch-only）提议的边界测试假设「writeOff → 触发 partnerBalanceUpdater.refresh → 断言」——生产代码根本不在坏账路径触发 refresh，该测试若照写会失败；本条与 P2-RC-082 相关但控制点不同（缺 refresh 调用 vs 缺边界测试）。
- **建议修复方向**：executeWriteOff/executeRecovery/executeReverseApprove 与 AdvanceOffsetOrchestrator 写后补 `partnerBalanceUpdater.refresh(item.getPartnerId())`（注意 flush 时序，对齐核销 Processor 的 `flushBeforeBalance` 范式）。
- **arm-index 裁决**：新增（带交叉引用 P2-RC-082）。

### P2-CK-fin2-007（D8/D6，同族 P1-CK-fin-004/011）AR/AP 聚合读路径缺 acctSchemaId/orgId 隔离；多账套计提逐 schema 循环却用全局 Allowance 与全局应收基础

- **控制点**：`entity/ErpFinArApItemBizModel`（`findOpenItemsByPartner` L38-48 / `findOpenItems` L52-60 / `aging` L64-88——filter 仅 direction/partner/status，无 acctSchemaId、无 orgId）+ `baddebt/BadDebtProvisionService#runBadDebtProvisionForSchema`（L193-235：外层逐 schemaId 循环写凭证，但 `calculateRequiredProvision` L240-243 与 `getAllowanceBalance` L251-270 均为**全账套混合**——`findReceivableOpenItems` L288-297 无 schema 过滤、`findPostedVoucherIds` L304-314 汇总全部账套凭证）+ `AbstractErpFinReconciliationProcessor#validateLine`（L63-91：不校验行 item 的 orgId/acctSchemaId 与 head 一致）
- **证据**：多账套（`multi-schema-enabled=true`）下：账套 2 的计提比较「账套 2 应收基础 vs 全局 Allowance」→ 补提/释放金额错配；账套 1 已提的 allowance 被账套 2 的比较吞掉。GL 凭证层逐账套完整（C3.1 已证），计提层混合。`getAllowanceBalance` 的混合独立于 P2-CK-fin-011 成立（GL 凭证天然逐账套）。aging/validateLine 面当前受 fin-011（辅助账仅首账套生成）遮蔽，fin-011 修复后完全显形；跨 org 核销（同 partner 不同 org 的 item 混入同一核销单）手工路径即时可达（validateLine 无 org 断言，head.orgId 取首个 invoice item——`ErpFinReconciliationCreateProcessor` L38）。
- **建议修复方向**：聚合查询补 acctSchemaId/orgId 维度（aging 按调用方传入账套/组织）；validateLine 补 orgId（至少）一致性断言；ProvisionService per-schema 循环内按 schemaId 过滤基础与余额。
- **arm-index 裁决**：新增（带同族注记）——P1-CK-fin-004（账套 ACTIVE 过滤）/P2-CK-fin-011（辅助账多账套幂等）是写路径控制点，本条是计提/账龄/核销校验读路径；P1-MA2-095（读路径双计，resolved）是报表层。`getAllowanceBalance` 全量 voucherId 载入的性能面归 **P2-MA4-003**（watch-only，复用不重复登记）。

### P2-CK-fin2-008（D4/D7）定时自动核销单事务全量原子 + 业务开关关闭时整批抛错——与声明的「记录级重试（单条失败不阻断）」失败接力模型不符

- **控制点**：`_vfs/nop/batch-task/fin/ar-ap-auto-recon.batch.xml`（L3 `transactionScope="process"`；L18-22 processor 对每 direction 调 `runAutoReconciliation(direction, null, 'FIFO', ...)`——partnerId=null 触发**全 partner** 循环）+ `processor/ErpFinReconciliationRunAutoReconciliationProcessor#runAutoReconciliation`（L38-40 `erp-fin.auto-reconcile=false` 时直接抛 `ERR_AUTO_RECON_DISABLED`；L50-62 单 `@BizMutation` 事务内逐 partner create+post，任一 partner 校验异常整体回滚）
- **证据**：`docs/architecture/job-scheduling.md` L337 为 `erp-fin-ar-ap-auto-recon` 声明的重试模型是「记录级重试（单条匹配失败不阻断）」，实现是全或无。触发面举例：夜间批处理运行中某 item 恰被并发核销至 SETTLED（引擎查询与 post 校验之间）→ `assertOpen` 抛错 → 当晚**所有 partner** 的自动核销回滚归零。双层门控错配：job 有自己的 `nop.job.erp-fin-ar-ap-auto-recon.enabled`（默认 false），运维只开 job 门而未开 `erp-fin.auto-reconcile`（默认 false）时每日凌晨首个 item 即抛错、整批失败且无降级。
- **建议修复方向**：batch processor 或 RunAutoReconciliation 循环改 per-partner 容错（catch NopException → 记录该 partner 失败并继续，结果 DTO 已有 unmatched 承载面）；disabled 场景改为 skip+warn 而非抛错（或 job 层校验业务开关）。
- **arm-index 裁决**：新增（grep「auto-recon 批 事务/记录级重试」零命中；P1-MA3-038 是配置键命名漂移已 resolved，不同控制点）。

### P2-CK-fin2-009（D5）坏账 approve 无 SoD 守卫——坏账单创建人可自审核销/收回（R3.3 SoD 铺开未覆盖 ErpFinBadDebt）

- **控制点**：`processor/ErpFinBadDebtApproveProcessor.java#approve`（L22-30：仅 `debt.isApproved()` 幂等早返 + 审批轴迁移校验 + 派发，无 `SoDGuard.assertApproverNotCreator`）对照同域已铺守卫的 `ErpFinEmployeeAdvanceProcessor` L195 / `ErpFinExpenseClaimProcessor` L283（grep 全模块 SoDGuard 命中仅此两处 + common 定义）
- **证据**：arm-index `P1-MA6-001`（resolved R3.3）的 SoD 修复覆盖清单为 4 域 16 个 approve 实体（PurOrder/Requisition、SalOrder/Quotation、PurInvoice/Payment、FinExpenseClaim/EmployeeAdvance、MfgWorkOrder/SubcontractOrder、PurReceive/Return、SalInvoice/Receipt/Return/Delivery）——**ErpFinBadDebt 不在列**。owner doc `bad-debt.md §SOX 控制启示` C21「坏账核销/恢复的审批人与发起人分离」+ §步骤3「经财务主管审批」；writeOff 默认 `require-approval=true` 时 approve 即执行凭证+辅助账变异，自审窗口直接触及 BS。
- **建议修复方向**：approve/submit 路径补 `SoDGuard.assertApproverNotCreator(debt.getCreatedBy(), currentUserId(), ...)`（模式同 EmployeeAdvance Pattern B）；顺带补「财务主管」角色维度（当前无任何角色检查，依赖全局 action-auth 基线 P1-MA3-046）。
- **arm-index 裁决**：新增（带同族注记）——与 P1-MA6-001（SoD 模式，resolved）同型不同实体控制点；action-level RBAC 基线归 P1-MA3-046 不重复。

### P3-CK-fin2-010（D4/D8）定时核销 job 硬编码 'FIFO' 绕过 `erp-fin.auto-recon-strategy` 配置 + 设计文档 cron 键漂移（「deferred」过期）

- **控制点**：`ar-ap-auto-recon.batch.xml` L21（`biz.runAutoReconciliation(item.direction, null, 'FIFO', ...)` 字面量）+ `ErpFinReconciliationRunAutoReconciliationProcessor#resolveStrategy` L71-78（参数非空即短路 config）+ `docs/design/finance/ar-ap-reconciliation.md` L325（`erp-fin.ar-ap-auto-recon-cron`—deferred）对照 `job-scheduling.md` L96/L109/L352（SCHEDULED，实际键 `nop.job.erp-fin-ar-ap-auto-recon.cron-expr` 默认 `0 0 1 * * ?`，job yaml 实证）
- **问题**：配置 BY_RATIO/BY_AMOUNT 对定时路径无效（job-scheduling.md L109 描述「定时自动核销（按比例/账龄/到期日）」名不符实——顺带使 P1-CK-fin2-001 的爆炸面限于手动路径，属少数幸运对齐）；设计文档仍称定时调度 deferred 且配置键名与实现不一致。
- **建议修复方向**：batch processor 传 `null` strategy（让 resolveStrategy 落 config）；设计文档配置表更新为实际 job 键与 SCHEDULED 状态。
- **arm-index 裁决**：新增（P1-MA3-038 resolved 的是 4 幻影规则键的 doc 漂移；batch 硬编码 FIFO 与 cron 键现状是新控制点，同族 P3-CK-pur-012/P3-CK-sal-023）。

### P3-CK-fin2-011（D9）findPartnersWithOpenItems 全量载入实体 + O(n²) contains 收集 distinct partner；DualSideConsistencyChecker 逐发票 N+1 反查域级发票

- **控制点**：`reconciliation/AutoReconciliationEngine#findPartnersWithOpenItems`（L323-337：`arApItemBiz.findList` 载入全部 OPEN/PARTIAL 实体后 `!partners.contains(id)` 线性去重）+ `reconciliation/DualSideConsistencyChecker#resolveDomainSettled/findPurInvoiceByCode/findSalInvoiceByCode`（L119-146：每个发票项一次 `eq("code", code)` 查询——item 数量级 N+1）
- **问题**：夜间全 partner 自动核销（partnerId=null）与双面对账均为「大」量级入口（job-scheduling.md 自评「大」），实体全载 + 平方去重 + N+1 放大。aging/findOpenItems 全量载入（平台 `findList` 缺省不分页，见「验证为正确」）同属此类但不截断、计数正确，归性能不归正确性。
- **建议修复方向**：findPartnersWithOpenItems 改 `QueryBean` 投影/去重查询或 `groupBy partnerId`；Checker 先按 code 批量 `in` 查域级发票再内存 join。
- **arm-index 裁决**：新增（P2-MA4-003 watch-only 六项热点不含此两站点；P2-MA7-005 N+1 族是 posting alreadyposted 链）。

### P3-CK-fin2-012（D2/D10）currentUserId 宽 catch 返回 null 无日志（跨域同型）+ approve 路径 loadArApItem 无 null 守卫

- **控制点**：`processor/ErpFinBadDebtProcessor#currentUserId`（L414-421：`catch (Exception e) { return null; }` 零日志——同型 P3-CK-pur-010/P3-CK-sal-024/P3-CK-inv-018）+ `ErpFinBadDebtApproveProcessor#approve` L29（`processor.loadArApItem(debt.getSourceArApItemId())` 无 null 检查，`loadArApItem` L314-316 返回裸 `getEntityById`——item 被删时 `executeWriteOff#validateAmount` L299 `nz(item.getOpenAmountFunctional())` 直接 NPE 而非 `ERR_AR_AP_ITEM_NOT_FOUND`）
- **建议修复方向**：同型修复（log.warn + 返回 null）；approve 复用 `requireOpenArApItem`/`requireWrittenOffArApItem` 语义加载。
- **arm-index 裁决**：新增（同型跨域已按各域登记，本站点按域登记惯例）。

### P3-CK-fin2-013（D6/D10）AdvanceOffsetOrchestrator 将本位币值直写 source 侧字段——外币借款/报销项源币口径污染；引擎金额 scale 2 硬编码 + matchFifo 死守卫

- **控制点**：`posting/AdvanceOffsetOrchestrator#applySettlement/reverseSettlement`（L194-212：`item.setSettledAmountSource(settled)` / `setOpenAmountSource(open)`——settled/open 为 **functional** 计算值，源币字段应按 item.exchangeRate 折算；rate≠1 的外币借款/报销项 source 侧账面失真，`ReconciliationSettler#applySettlement` L116-124 的双轨维护在此被绕过）+ `AutoReconciliationEngine#line` L296 与 `#matchByRatio` L192（`divide(..., 2, HALF_UP)` 硬编码 scale 2，`erp-fin.reconcile-precision` 配 0.0001 时精度丢失）+ `#matchFifo` L115-117（`settle = min(invoiceOpen, remain)` 后的 `if (!allowOver && settle > invoiceOpen) settle = invoiceOpen` 恒假死代码）
- **建议修复方向**：offset 算术复用 `ReconciliationSettler`（或按 rate 折算 source 侧）；divide scale 取 `precision.scale()`；删除死守卫。
- **arm-index 裁决**：新增（P1-MA2-009 的双字段修复覆盖 posting/VoucherFact 链，AdvanceOffset 的 source 污染未覆盖；grep 零命中）。

### P3-CK-fin2-014（D5/D8）核销冲销无原因记录（设计承诺「核销冲销：财务员 + 原因记录」）+ create 入参校验复用方向不匹配错误码

- **控制点**：`entity/ErpFinReconciliationBizModel#reverse`（L89-91：仅 `reconciliationId` 参数，无 reason 入参、不写 remark）对照 `docs/design/finance/ar-ap-reconciliation.md §核销冲销`（步骤 2「记录冲销原因」+ §核销权限表「核销冲销 | 财务员 + 原因记录」）+ `ErpFinReconciliationCreateProcessor#create` L27-31（direction/partnerId/businessDate/lines 任一为 null 时抛 `ERR_RECONCILIATION_DIRECTION_MISMATCH`——参数缺失报「方向不匹配」语义漂移，同型 P3-CK-md-014）
- **建议修复方向**：reverse 增可选 `reason` 参数写 head.remark；create 入参校验换专用错误码。
- **arm-index 裁决**：新增（P2-MA2-036 doc 漂移清单不含此项）。

### P3-CK-fin2-015（D2）DualSideConsistencyChecker 把坏账核销的合法单侧变异报为 INCONSISTENT——告警噪音侵蚀检查器信任

- **控制点**：`reconciliation/DualSideConsistencyChecker#check`（L68-99：finance 侧聚合 `item.settledAmountFunctional` **全量**（无 sourceBillType=发票过滤外的状态区分、不剔除坏账核销贡献），对照域侧 `ErpPurInvoice.paidAmount`/`ErpSalInvoice.receivedAmount`——`ErpFinBadDebtProcessor#executeWriteOff` L172-173 将坏账额计入 settled 而域级侧无对应回写）
- **问题**：启用坏账的账套每发生一次 writeOff/recover，该 partner 即恒定 INCONSISTENT（diff=坏账额）+ WARN 告警——检查器无法区分「合法单侧变异（坏账、域级反向核销）」与「真实漂移」，`P2-MA2-038`（watch-only）设想的 dual-side 守卫若照此实现会持续误报。
- **建议修复方向**：finance 侧聚合剔除坏账贡献（按 `ErpFinVoucherBillR` 反查 BAD_DEBT_* 凭证金额）或 checker 报告维度拆分 badDebtSettled 字段。
- **arm-index 裁决**：新增（带同族注记 P2-MA2-038 watch-only——本条是其落地面上的具体噪音控制点）。

### P3-CK-fin2-016（D4）声明配置零消费：`erp-fin.bad-debt-exclude-disputed` 无 disputed 字段支撑，计提范围排除争议项的 doc 承诺不生效

- **控制点**：`service/ErpFinConstants.java` L350（`CONFIG_BAD_DEBT_EXCLUDE_DISPUTED` 定义）+ `baddebt/BadDebtProvisionCalculator` 类 javadoc L20-21（自认「当前无 disputed 字段，预留门控」）+ grep 全模块该常量**零消费**；owner doc `bad-debt.md §配置点`（默认 true）与 `§计提范围排除`（争议发票排除出准备基础）承诺未落地
- **建议修复方向**：短期 doc 标注「字段未落地，配置无效」；长期 ArApItem 增 disputed 标记（ORM 保护区域，dual-agent）。
- **arm-index 裁决**：新增（同族 P3-CK-pur-012/P3-CK-inv-021 声明未接线）。

### P3-CK-fin2-017（D10）resolvePeriodId 无 orgId 过滤无排序 setLimit(1)（同族 P3-CK-fin-017 新站点）+ BadDebtProvisionService 账套解析魔法默认 "1"

- **控制点**：`processor/AbstractErpFinReconciliationProcessor#resolvePeriodId`（L253-264：`le(startDate)+ge(endDate)+setLimit(1)`，无 orgId、无 orderBy——重叠期间下 FX 凭证 periodId 归属不定；与 C3.1 登记的 `ErpFinPostingProcessor#resolveOpenPeriod` 同型不同站点）+ `baddebt/BadDebtProvisionService#resolveAcctSchemaId`（L346-364：解析链尽头的兜底返回字面量 `"1"`——无账套数据时 BDR/BDL 凭证挂到 id="1" 的存疑账套）
- **建议修复方向**：resolvePeriodId 补 orgId + 确定性排序；resolveAcctSchemaId 兜底改抛 `ERR_CLOSE_SUBJECT_NOT_CONFIGURED` 类配置错误。
- **arm-index 裁决**：新增（P3-CK-fin-017 是引擎站点，本条是新站点按惯例登记）。

## 移交项裁决结论（回填 purchase/sales 检查）

| 移交 finding | 裁决 | 结论与依据 |
| --- | --- | --- |
| `P2-CK-pur-005` Payment cancel/reverseApprove 不守卫也不回滚核销 | **证实（finance 侧控制点登记为 P2-CK-fin2-005）** | finance 侧付款项回滚通道仅一条：源单凭证红冲 → `ErpFinPostingProcessor` L246 → `cancelOnReverse` 置 item CANCELLED/open=0——但不守卫 `settledAmount>0`（已核销项被强制作废、settled 残留）、不级联引用该 item 的已过账核销单（悬挂 POSTED）、后续核销单 reverse 还会把 CANCELLED 项复活为 OPEN。purchase 域侧不触 finance（grep pur 模块对 IErpFinReconciliationBiz/ArApItemBiz 写引用仅 dashboard 只读）。purchase 侧缺陷维持其归属，finance 侧缺口由 fin2-005 承接。 |
| `P2-CK-sal-010` sales 收款核销不拒绝已作废单据（仅校验 approveStatus） | **finance 侧证实有守卫（无新 finding）** | finance 核销链对辅助账项状态有独立守卫：`AbstractErpFinReconciliationProcessor#assertOpen`（L93-100）拒绝 SETTLED/CANCELLED + `assertNotOver` 拒超额 + direction/partner/date 校验——已作废收款项若经凭证红冲会落 CANCELLED 从而被 finance 核销拒绝。WRITTEN_OFF 未列入拒绝集是既有登记 **P2-MA2-039**（watch-only，复用不重复）。sales 侧「只查 approveStatus 不查 docStatus」缺陷维持 sales 归属。 |
| `P2-CK-sal-012` sales 收款反审核/作废不反向核销（RECEIVED 残留） | **证实（finance 侧通道存在但不完整，并入 P2-CK-fin2-005）** | 同 pur-005：RECEIPT 凭证红冲 → cancelOnReverse 取消收款辅助账项，但已核销项守卫与核销单级联缺失（fin2-005 单点承接 pur-005/sal-012 双方）。sales ReceiptSettler 不触 finance 侧（grep 零引用），域级 receivedAmount 残留维持 sales 归属。 |
| `P1-CK-sal-001` ReturnRefundOrchestrator 客户级全量反转核销 | **佐证成立（维持 sales 归属，finance 侧无新 finding）** | 细粒度反转原语存在：(a) finance `IErpFinReconciliationBiz.reverse(reconciliationId)` 行级绑定 (paymentItemId, invoiceItemId)（`ErpFinReconciliationLine` 列定义实证）；(b) sales 自身 `ReceiptSettler.reverseSettlement(receipt, invoice.getId())` 即发票粒度签名（`ReturnRefundOrchestrator` L97 调用处实证）。orchestrator 仍选择 `findReceivedInvoicesOfCustomer`（L69-77，customerId+receivedAmount>0 全量扫描，不关联退货单/发票）——发票粒度能力在手下却做客户级全量，用法错误佐证 P1。该类完全不触 finance 实体（grep 零 ErpFin 引用）。 |

## 验证为正确（显式排除，防误报）

- **平台 `QueryBean.addOrderField(name, desc)` 第二参为 desc**（`nop-entropy/nop-kernel/nop-api-core/.../QueryBean.java:433-441` `OrderFieldBean.forField(name, desc)` 复核）——`ErpFinArApItemBizModel#findOpenItemsByPartner` L46 `addOrderField("businessDate", false)` = 升序 = 最旧优先，**FIFO 语义正确**；`ErpFinBadDebtProcessor#debtAmountOf` L342 `addOrderField("id", true)` = 降序 = 最新已批 WRITE_OFF 单（id 为 `seq-default` 递增序列，orm 实证）**正确**。
- **`CrudBizModel.findList` 缺省不分页**（`nop-entropy/nop-service-framework/nop-biz/.../CrudBizModel.java:1575` javadoc「返回List类型，而且缺省不分页」实证）——aging/findOpenItems/findPartnersWithOpenItems 无静默截断风险（全量载入的性能面归 fin2-011）。
- **乐观锁在位**：`ErpFinArApItem`（orm L739）/`ErpFinReconciliation`（L810）/`ErpFinReconciliationLine`（L871）/`ErpFinBadDebt` 均 `versionProp="version"`——并发核销同一辅助账项由 ORM 乐观锁兜底（后提交方 stale 失败回滚），一致性保证成立；owner doc「悲观锁」承诺漂移归 **P2-MA2-036(4)**（复用，watch-only 已登记）。
- **核销单状态机无死状态**：`ErpFinReconciliationDocumentStateMachine` 3 dict 值 2 迁移边（DRAFT→POSTED→REVERSED）全活跃，REVERSED 终态不可复活，非法边抛 common 码 + 领域码映射（契约 §7）在位。
- **坏账审批状态机全可达**：`ErpFinBadDebtApprovalStateMachine` 4 值（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）全活跃；REJECTED 无重提为登记过的 intentional（**P2-MA2-040** 复用，不重复登记）。
- **FIFO/BY_AMOUNT 策略算术正确**：matchFifo `min(invoiceOpen, remain)` 分配 + 本地 map 维护累计 + 未耗尽报告；BY_AMOUNT norm 到 precision scale 后 1:1 精确匹配 + 剩余发票 unmatched 报告——与 javadoc/设计一致（BY_RATIO 缺陷另登记 fin2-001）。
- **单币种 settle↔reverse 对称**：`ReconciliationSettler#settle/reverseSettle` 以相反数互逆、`resolveStatus` 降级回 OPEN/PARTIAL 正确；config 关闭（默认）时 FX 路径完全退化为 settle（不对称缺陷 fin2-002 仅开启态）。
- **assertOpen/assertNotOver/direction/partner/date 五项核销约束在位**（validateLine L63-91）：同方向、同往来、不超开（precision 容忍）、核销日期不早于发票日期、状态非 SETTLED/CANCELLED——与 owner doc §核销约束 1-5 对应（WRITTEN_OFF 缺口归 P2-MA2-039；累计缺口归 fin2-003）。
- **坏账五步分录方向正确**：writeOff 借 Allowance/贷 AR 不进 P&L（executeWriteOff L179-186）；recovery 借 AR/贷 Allowance；provision 补提借减值损失/贷准备、释放反向（BadDebtProvisionService L203-228）——对齐 bad-debt.md §生命周期与「核销不进 P&L」反模式禁令。
- **writeOff 全量 open 入口 + validateAmount 守卫链使 open 归零**（writeOff 单创建即取全量 open，approve 时 validateAmount 复核）——P2-RC-082 已裁决的主路径行为正确性维持（交错时序缺口另登记 fin2-004）。
- **账龄分桶边界正确**：`accumulate`/`calculate` 的 `<=30/<=60/<=90/<=180/else` 与 doc 0-30/31-60/61-90/91-180/180+ 开闭区间一致；负 open（credit memo）与 SETTLED/CANCELLED/WRITTEN_OFF 排除正确（calculator L50-59）。
- **FX 凭证方向映射正确**：AR fx>0=收益（Dr 应收/Cr 汇兑损益）、AP 相反（AbstractErpFinReconciliationProcessor L197-206）；`hasFxVoucher`+`voucherBiz.reverse` 幂等守护对齐平台；单币种差额=0 不出凭证。
- **previewReverse 只读镜像 reverse 前置校验**：assertCanReverse 一致 + `estimateStatusAfterRevert`（回退后 settled≤0→OPEN 否则 PARTIAL）与 reverseSettle 实际行为一致。
- **D1 机械面全零**：切片 25 文件 `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now`=0（post/newBadDebt 用 `CoreMetrics.currentTimestamp/today`）、`extends RuntimeException`=0、字符串比较均 `.equals/Objects.equals`、无手编生成产物；跨域 daoFor（DualSideConsistencyChecker→pur/sal、BadDebtProvisionService/ErpFinBadDebtProcessor/requireReconSubject→md、PartnerBalanceUpdater→md partner）均落在 **P1-MA4-006** resolved 豁免矩阵（`data-dependency-matrix.md §9`）或有机制 B 注释豁免——不登记。
- **ArApItemGenerator 多账套幂等缺账套维度**：已由 C3.1 登记为 **P2-CK-fin-011**（复用，不重复）；本切片 cancelOnReverse 侧新控制点已并入 fin2-005 并注明。
- **post/reverse 无 CLOSED_FINAL 期间守卫 / 悲观锁 doc 漂移 / REJECTED 无重提 / assertOpen 不拒 WRITTEN_OFF**：分别归 **P2-MA2-041 / P2-MA2-036 / P2-MA2-040 / P2-MA2-039**（均 arm-index 已登记 watch-only，复用不重复计数）。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `matchByRatio 分母`/`settleWithFx reverse 不对称`/`validateLine 累计聚合`/`approveInternal 状态校验`/`cancelOnReverse settled 守卫`/`reverseSettle 复活`/`坏账 partnerBalance refresh`/`bad-debt SoD`/`auto-recon batch 事务`/`batch FIFO 硬编码`/`exclude-disputed 零消费`/`resolvePeriodId orgId` 在 `docs/audits/arm-index.md` **零命中 → 新增**（17 条）。
- **复用（不重复登记，报告中注记）**：
  - assertOpen 不拒 WRITTEN_OFF（post 侧）→ **P2-MA2-039**（watch-only）；reverse 侧复活是新控制点（fin2-005 内）。
  - post/reverse 无期间守卫 → **P2-MA2-041**；悲观锁 doc 漂移 → **P2-MA2-036(4)**；REJECTED 无重提 → **P2-MA2-040**。
  - `getAllowanceBalance` 全量 voucherId 内存载入（性能面）→ **P2-MA4-003**（watch-only 六项之一）——多账套混合维度是新的（fin2-007）。
  - 域侧-finance 双路径核销无对账守卫（系统面）→ **P2-MA2-038**（watch-only）——fin2-005/015 是其具体控制点。
  - ArApItem 多账套幂等 → **P2-CK-fin-011**（C3.1）；多账套 ACTIVE 过滤族 → **P1-CK-fin-004/P1-MA2-095**（fin2-007 带同族注记）。
  - SoD 模式 → **P1-MA6-001**（resolved R3.3）——fin2-009 为未覆盖实体新控制点。
  - 跨域 daoFor 读豁免 → **P1-MA4-006**（resolved）——验证为正确节处理。
  - FX plug 正向实现 → **P1-MA2-009**（resolved）——fin2-002 为 reverse 对称性新控制点。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 4 | P1-CK-fin2-001..004 |
| P2 | 5 | P2-CK-fin2-005..009 |
| P3 | 8 | P3-CK-fin2-010..017 |

按维度（主维度计）：D6×3（001/002/013）、D5×3（003/009/014）、D8×3（006/007 + 005 计 D3/D8 主 D8）、D4×3（008/010/016）、D3×1（004）、D9×1（011）、D2×2（012/015）、D10×1（017）。

移交项：证实 2.5（pur-005→fin2-005、sal-012→并入 fin2-005、sal-010 finance 侧守卫证实无新 finding）、佐证 1（sal-001 细粒度原语存在、用法错误维持 sales 归属）、复用 4（P2-MA2-039/036/040/041 watch-only 维持）。

## 剩余风险（查了什么/没查什么）

- **已查**：核销链（BizModel→4 Processor→Engine 三策略逐行推演含数值算例→Settler 双向→状态机）+ 坏账链（writeOff/recover/approve/reverseApprove 全时序组合 + Calculator 分桶边界 + ProvisionService 计提/释放/反向红冲）+ 辅助账生命周期（Generator generate/cancelOnReverse + AdvanceOffset 双向）+ aging/双面对账/伙伴余额三个辅助组件全读；平台源码实证 2 处（QueryBean.addOrderField、CrudBizModel.findList 不分页）；orm 四实体核对（versionProp/UK/id seq-default/dict）；测试交叉验证 3 处（BY_RATIO 单付款用例缺口、FX 测试无 reverse 断言、A4.1b 静态评估对照）；job 三件套（batch.xml/job.yaml/job-scheduling.md）D4 接线核验。
- **未深查**：`ErpFinNotesReceivable/PayableProcessor` 全文（仅 doWriteOff 断面——其红冲走 posting 引擎 cancelOnReverse，缺口已由 fin2-005 承接；票据 endorsement 抵应付的核销编排未逐行）；`ErpFinReportBizModel`/`ErpFinDashboardBizModel` 的账龄消费面（只读报表， drift 归 C8.2 web 层）；`ErpFinErrors` 错误码文案逐条核对（编译绿基线保证存在性）；坏账 provision 与期末结账门控（`populateAllowanceCheck`）的交互细节归 C3.4；测试代码仅用于行为交叉验证未逐文件审。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-fin2-001**（BY_RATIO 分母 + 尾差守卫反向）——A4.1b 审计曾评估三策略算术「扎实」，与本结论冲突；本条证据是逐行代码 + 数值推演（3×500 vs 1000 算例）+ 测试覆盖缺口三方印证，但建议修复阶段先写多笔付款复现测试定谳。
  2. **P1-CK-fin2-002**（FX reverse 不对称）——依赖「`line.settledAmountFunctional` 在 settleWithFx 路径从不被改写」的读码结论（settleWithFx L58-80 确无写回语句）；config 默认 false 使现行基线测试全绿自洽。建议开启 config 跑 settle→reverse→断言双侧 open 复现。
  3. **P2-CK-fin2-006**（坏账不刷新伙伴余额）——若存在本代理未发现的周期性余额重算 job（grep 未发现，但 app-erp-all 层调度未全列），严重性降为瞬态。grep `PartnerBalanceUpdater` 全模块仅 2 调用点是当前最强证据。
