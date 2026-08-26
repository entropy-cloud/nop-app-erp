# ck-contract — contract（合同域）实现代码检查报告

> 工作项：C7.2。执行日期：2026-08-26。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-contract/erp-ct-service/src/main/java` 全部 51 个手写生产文件（约 6,209 行）——合同生命周期（`entity/ErpCtContractBizModel` 778 行 + `ErpCtContractActivateProcessor`/`ErpCtContractAmendProcessor` + `ErpCtContractStateMachine` 224 行 9 边）、版本轴（`ErpCtContractVersionBizModel` + `ErpCtContractVersionSignVersionProcessor` + `ErpCtContractVersionStateMachine`）、开票计划（`ErpCtInvoicePlanBizModel` + TriggerInvoice/TriggerDuePlans/GenerateByTerm 三 Processor）、消耗计费（`ErpCtConsumptionPeriodSummarizeProcessor` 210 行）、返利链（`RebateEngine` 173 行 + `ErpCtRebateAgreementRunAccrualProcessor` + `ErpCtRebateSettlementPostSettlementProcessor` + 两退化/实现轴状态机 Bean）、量折扣（`ErpCtVolumeDiscountBizModel`）、审批工作流（`ErpCtApprovalWorkflowEngine` 275 行 + `ErpCtApprovalRecordBizModel` + 超时升级 Job）、终止两段化（terminate/approveTermination/rejectTermination）、到期自动化（`ErpCtContractExpiryJob`）、文档仓库（`ErpCtDocumentBizModel` 571 行 + `ErpCtDocRetentionJob` + OCR SPI）、电子签章（`ErpCtSignatureRequestBizModel` 441 行 + Abstract 基类 + 3 Processor + Provider SPI/Mock）+ 配置/常量/错误码 + 裸 stub BizModel 5 个。跨文件核实：`module-contract/model/app-erp-contract.orm.xml`（versionProp/UK/列 mandatory）、`erp-ct-meta` xmeta 覆盖层（ErpCtDocument/ErpCtInvoicePlan/ErpCtRebateAgreement）与 14 个 dict、`_vfs/erp/ct/beans/app-service.beans.xml`（25 bean）、`app-erp-all/_vfs/nop/job/conf/erp-ct-*.job.yaml`（3 个）、`module-purchase|sales/model/*.orm.xml` 发票 UK/mandatory 列、平台源码 `BeanMethodJobInvoker`/`FilterBeans`。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。51 文件核心链逐行深读 + 平台源码实证 4 处（`BeanMethodJobInvoker` 零 `IUserContext` 建立 → 008 依据；`UK_PUR/SAL_INVOICE_CODE_ORG` 存在 → 并发双触发证伪；`ErpPurInvoice.currencyId`/`ErpPurInvoiceLine.materialId/uoMId` mandatory → 004 依据；`FilterBeans.compareOp` 直传语义）+ orm/xmeta/dict/beans/job-yaml 接线核对 + arm-index contract 域 10 条 finding（P1-RC-072..080、P1-MA1-029、P1-MA2-071/072）逐条裁决。
> 切片边界：`erp-ct-web` AMIS 页面契约 drift 归 C8.2；`erp-ct-api` 骨架 beans 不深查；测试代码仅用于行为语义交叉验证；pur/sal 侧折扣消费组件（`ErpPurCtDiscountApplier`/`ErpSalCtDiscountApplier`，RC-R1.79）归 purchase/sales 域检查范围，本报告只查 ct 侧 `resolveDiscount` 供给面。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-ct-001（D6/D8）PERIOD_END runAccrual 非幂等——重复运行把期间发票全额重复累加进返利基数并重复计提

- **控制点 A**：`app/erp/ct/service/processor/ErpCtRebateAgreementRunAccrualProcessor.java#runAccrual`（L58-65：`Set<String> alreadyAccruedCodes = loadAccruedBillCodes(agreementId); ... BigDecimal periodTotal = sumPeriodInvoices(agreement, periodStart, periodEnd, alreadyAccruedCodes); if (periodTotal.signum() != 0) { rebateEngine.accruePeriodEnd(agreement, periodTotal, context); }`）
- **控制点 B**：`app/erp/ct/service/rebate/RebateEngine.java#accrue`（L66-70：`BigDecimal newCumulative = nz(agreement.getTotalAccumulatedAmount()).add(amount); ... BigDecimal delta = expectedRebate.subtract(alreadyAccrued);`）+ `#accruePeriodEnd`（L103-107：sourceBillCode = `"PERIOD-" + today`）
- **证据**：PERIOD_END 去重集合 `alreadyAccruedCodes` 只含历次计提写入的伪单据码 `"PERIOD-<date>"`（`loadAccruedBillCodes` L92-103 读 `ErpCtRebateAccrual.sourceBillCode`），而 `sumPeriodInvoices`（L120-131）跳过的条件是**发票 code** 落在该集合——发票 code（如 `AP-xxx`）永远不等于 `PERIOD-xxx`，**去重永不命中**。第二次运行 runAccrual（同日或跨日，PERIOD_END 法）：`periodTotal` 再次聚合**期间全部**已过账发票（不是「上次计提之后的新增」——无增量水位载体）→ `accrue` 把同一批发票金额**再次**加进 `totalAccumulatedAmount`（基数翻倍）→ `delta = expected(翻倍基数) − Σ已计提` 产出第二笔计提行。数值例：阶梯 0~1M→0%、1M~5M→2%；期间发票 Σ=1.2M，首次运行计提 24K、累计 1.2M；同日重跑 → 累计 2.4M、expected 48K、delta 24K → **计提总额 48K、结算按 48K 付返利，实际基数只有 1.2M**。`totalAccumulatedAmount` 被永久污染（后续每次运行再 +periodTotal）。PROGRESSIVE 法有 per-invoice code 去重不受影响（对照证成该缺陷为 PERIOD_END 独有）。
- **触发条件/影响面**：任何 `accrualMethod=PERIOD_END` 协议（owner doc `volume-discount.md §配置点` 默认计提方法即 PERIOD_END）被重复 runAccrual——手工重复调用、或未来接线 job 后每日运行——返利基数与应付返利按运行次数线性膨胀；结算（postSettlement 汇总未结算计提）直接按虚增金额开负额发票。
- **建议修复方向**：为 PERIOD_END 建立增量水位（如计提时记录已聚合到的 max(businessDate) 或逐发票落 code 明细行使 `sumPeriodInvoices` 去重生效）；或 PERIOD_END 计提行落「期间窗口 + 聚合集指纹」重演拒绝。
- **arm-index 裁决**：新增（grep arm-index「PERIOD_END / runAccrual」零命中；P1-RC-078 done 注记只覆盖订单侧折扣接线维度）。

### P1-CK-ct-002（D8）返利贷项发票被后续 runAccrual 当作负数发票重复计入——已结算返利反噬累计基数（双重扣减）

- **控制点**：`app/erp/ct/service/processor/ErpCtRebateAgreementRunAccrualProcessor.java#findPeriodInvoices`（L106-118：`q.addFilter(eq("posted", true)); q.addFilter(ge("businessDate", from)); q.addFilter(le("businessDate", to)); ... q.addFilter(eq("supplierId"/"customerId", agreement.getPartnerId()));`——**无任何对结算贷项发票的排除**）对照 `ErpCtRebateSettlementPostSettlementProcessor#postSettlement`（L72-81：贷项发票 code=`"CT-REBATE-" + settlement.getId()`，supplier/customer = agreement.partnerId，businessDate=结算日，`docStatus="DRAFT" + posted=false`）
- **证据**：贷项发票按设计（volume-discount.md §结算流程实现约定 + C18 勘误）后续经 purchase/sales 标准管道审批过账 → `posted=true`。此后任何一次 runAccrual：`findPeriodInvoices` 按 posted + partner + 日期窗命中该贷项（settlement 日在 [startDate, asOf] 内即命中）→ `invoiceAmount` 取 `totalAmountWithTax` = **负的结算总额** → `alreadyAccruedCodes` 不含 `CT-REBATE-xxx`（从未计提）→ 作为「退货扣减」参与累计：`totalAccumulatedAmount` 被已付返利等额冲减、`delta` 产生负计提行 → 下一轮结算再按负 delta 反向开票。返利支付本身被当成退货从基数中扣除——**同一笔返利先付出又从未来返利中扣回**。PROGRESSIVE 与 PERIOD_END 两法均受影响。
- **触发条件/影响面**：任何「先结算、后继续 runAccrual」的协议（多次结算/跨期结算场景）；返利财务口径系统性失真。
- **建议修复方向**：`findPeriodInvoices` 排除 `code like 'CT-REBATE-%'`（与生成侧 `creditMemoCode` 前缀约定对偶）；或为发票增加来源标记列（跨域 ORM 变更须 dual-agent-approval）；owner doc 同步登记「贷项不参与计提基数」规则。
- **arm-index 裁决**：新增（grep「CT-REBATE / 贷项再计提」零命中；C18 勘误只裁决「负额发票表达贷项」为设计，未覆盖计提侧回吸维度）。

### P1-CK-ct-003（D6）tier 区间上界排他 vs 设计「截止数量/金额（含）」——边界命中落空：返利整档归零、量折扣静默回退原价

- **控制点 A**：`app/erp/ct/service/rebate/RebateEngine.java#matchTier`（L126-132：`t -> (t.getFromAmount() == null || amount.compareTo(t.getFromAmount()) >= 0) && (t.getToAmount() == null || amount.compareTo(t.getToAmount()) < 0)`——**toAmount 排他**；`computeRebate` L112-115：`matched == null → return BigDecimal.ZERO`）
- **控制点 B**：`app/erp/ct/service/entity/ErpCtVolumeDiscountBizModel.java#matchBand`（L86-99：`if (to != null && qty.compareTo(to) >= 0) { continue; }`——**toQty 排他**；无命中 L53-56 回退原价 `discountApplied=false`）
- **证据**：owner doc `volume-discount.md` §ErpCtVolumeDiscount/§ErpCtRebateTier 字段表逐字「fromQty 起始数量（**含**）」「toQty 截止数量（**含**，null 无上限）」「toAmount 截止金额（**含**，null 无上限）」，§折扣应用逻辑示例区间 `0~100 / 101~500 / 501+`。实现按 `[from, to)` 半开匹配：按示例配置（toQty=100 / fromQty=101），**下单数量恰为 100** 时 band1 `100>=100` 排除、band2 `100<101` 排除 → 无命中 → 回退原价零折扣——设计语义下 100 应享 band1（0% 档）且更重要的是任何「整千/整百」边界订单在带间隙处静默丢折扣；返利侧同理：累计金额恰等于某档 toAmount（如 ¥1,000,000）且次档 fromAmount=1,000,000.01 时**落空整档 → 返利 0**。量折扣内部自洽（`validateNoOverlap` L121-131 同用半开区间，javadoc L28 显式声明 `[fromQty, toQty)`），属「代码自定义语义 vs owner doc 权威语义」漂移而非实现笔误；边界整数数量（ERP 常见）命中概率不低。
- **问题**：D6 业务计算边界——量折扣/返利在档上界精确命中时系统性少算（折扣丢失/返利归零），purchase/sal 侧消费组件（`ErpPurCtDiscountApplier`/`ErpSalCtDiscountApplier`，RC-R1.79）调用本 API，错误随订单定价扩散。
- **建议修复方向**：与 owner doc 联合裁决二选一：(a) 实现改 `<=`（含上界，重叠校验同步改闭开混合 `[from, to]` + 次档 from=to+最小步长约定）；(b) owner doc 改半开语义并显式声明边界值归属。修复必须两控制点（RebateEngine + VolumeDiscountBizModel）+ 重叠校验同步，防修一处漏一处。
- **arm-index 裁决**：新增（grep「toAmount 含/边界等号/matchTier」arm-index 零命中；P1-RC-078 done 注记声明「API 存在且正确」只验证了区间中段行为，未覆盖边界等号维度）。

### P1-CK-ct-004（D10/D5）发票生成路径对 nullable 合同数据无守卫——独立协议结算/无物料合同行触发时撞 NOT NULL 列裸崩

- **控制点 A**：`app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java#resolveCurrencyId`（L205-212：`agreement.getContractId() == null → return null`）+ `#resolveMaterialId`（L218-227：无关联合同/首行无 materialId → return null）→ `#createNegativeApInvoice`（L105-135：`invoice.setCurrencyId(currencyId)` 可 null、`line.setMaterialId(materialId)/setUoMId(uomId)` 可 null）
- **控制点 B**：`app/erp/ct/service/processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java#createApInvoiceDraft`（L96-108：`if (line.getMaterialId() != null) { invLine.setMaterialId(...); ... }`——materialId null 时行字段留空）+ `createArInvoiceDraft`（L133-145 同构）
- **证据**：平台 ORM 实证——`module-purchase/model/app-erp-purchase.orm.xml`：`ErpPurInvoice.currencyId` mandatory（L294）、`ErpPurInvoiceLine.materialId/uoMId` mandatory（invoice line 列 12-13）；sales 侧同构。而 ct 侧合法数据面：① `volume-discount.md §ErpCtRebateAgreement` 逐字「contractId | 关联合同（**可选，独立协议可无合同**）」——独立协议 postSettlement → currencyId=null → 发票头 NOT NULL 违约，整笔结算 mutation 以平台 SQL 异常失败（无领域错误码）；② `docs/design/contract/README.md §核心业务对象` 逐字「合同行……物料/产品（**框架合同可不指定**）」+ ORM 实证 `ErpCtContractLine.materialId` 可空——首行无物料的合同 postSettlement 与无物料行的 triggerInvoice → 发票行 MATERIAL_ID/UO_M_ID 违约裸崩。注释自承前提（「由调用方确保关联存在」L168-169）与设计合法面冲突。
- **触发条件/影响面**：设计明示合法的两类数据（独立返利协议、框架合同无物料行）上，返利结算与开票触发两大核心链路功能性断点，且错误形态为原始约束异常（无 `ERR_CT_*` 领域码，排障困难）。
- **建议修复方向**：入口前置守卫（独立协议/无物料 → 领域错误码显式拒绝并要求补合同关联或占位物料），或 resolve 层取缺省币种（本位币）+ 占位物料配置键；与 owner doc 裁决「独立协议结算币种来源」。
- **arm-index 裁决**：新增（grep「独立协议 settlement/材料 NOT NULL」零命中；P1-RC-078 done 注记的结算验收只覆盖有关联合同+有物料行主路径）。

### P2-CK-ct-005（D3）approveTermination 无合同状态再守卫——PENDING 期间合同经 amend/expire 漂移后，法务批准可把任意状态（含 EXPIRED 终态）改成 TERMINATED

- **控制点**：`app/erp/ct/service/entity/ErpCtContractBizModel.java#approveTermination`（L252-272：`requireTerminationRecord` + `guardTerminationRecord`（仅 PENDING + 审批人）→ L259 `contract.setStatus(stateMachine.terminateTargetStatus()); updateEntity(...)`——**全函数无 `stateMachine.assertCanTerminate(contract.getStatus())`**）对照发起侧 `#terminate` L223（有 assertCanTerminate）
- **证据**：时序洞：terminate 发起（合同 ACTIVE，记录 PENDING）→ 期间合同被 amend（ACTIVE→DRAFT）或被 expire（ACTIVE→EXPIRED，如 expiry job 自动推进）→ 法务批准 → `setStatus(TERMINATED)` 无条件写入：DRAFT→TERMINATED（状态机 9 边之外的非法边）、**EXPIRED→TERMINATED（终态改写）**均可达。副作用（版本归档/InvoicePlan 截停/善后通知）也在错误状态下执行。双请求窗口（发起与批准是两个独立 mutation）使竞态真实存在。
- **建议修复方向**：approveTermination 在执行前重放 `assertCanTerminate`（状态已漂移 → 领域码拒绝 + 记录作废/通知发起人）；或发起时在合同行打终止中标记并让 amend/expire 拒绝。
- **arm-index 裁决**：新增（RC-R1.34 done 注记声明「守卫 PENDING + 审批人匹配」，未覆盖状态再守卫维度）。

### P2-CK-ct-006（D3）rejectAmend 无「amend 来源」判别——全新 DRAFT 合同可直接一键 ACTIVE，绕过审批链/签署/法务全部门控

- **控制点**：`app/erp/ct/service/entity/ErpCtContractBizModel.java#rejectAmend`（L162-176：仅 `stateMachine.assertCanRejectAmend(contract.getStatus())`——`statemachine/ErpCtContractStateMachine.java#assertCanRejectAmend` 只查 `status==DRAFT`）+ `#restoreCurrentVersion`（L680-707：零版本 early-return / 无 SIGNED+FINALIZED 候选时仅清空 isCurrent）
- **证据**：`rejectAmend(DRAFT→ACTIVE)` 边为 amend 生命周期设计（RC-R1.32），但实现无法区分「amend 产生的 DRAFT」（曾有 SIGNED 版本、合同曾 ACTIVE）与「**全新草稿**」。新建合同（DRAFT，未 submit，零版本）直接调 `rejectAmend`：守卫通过 → restoreCurrentVersion 空转 → `setStatus(ACTIVE)`——**DRAFT→ACTIVE 直达**，跳过 submit 审批链（`generateApprovalRecordsIfEnabled`/`isChainComplete` 只在 submit/activate 路径）、跳过 signVersion 签署级联、跳过 NEGOTIATION。状态机 `transitions()` 元数据中 DRAFT 的出边仅 submit/rejectAmend，该洞使 rejectAmend 成为事实上的免检激活后门。amend 场景合同（有 SIGNED 前任版本）不受影响——判别依据（存在 SIGNED/FINALIZED 版本）在 `restoreCurrentVersion` 内已经算出但未用于守卫。
- **建议修复方向**：rejectAmend 增加来源判别守卫——无 SIGNED/FINALIZED 前任版本（非 amend 产物）时抛 `ERR_CT_ILLEGAL_STATUS_TRANSITION`；Bean 边语义不变。
- **arm-index 裁决**：新增（RC-R1.32 done 注记覆盖「amend 场景驳回恢复」正路径，未覆盖 fresh-DRAFT 洞；grep「rejectAmend 守卫」零命中）。

### P2-CK-ct-007（D3/D5）手工 expire 无 endDate 到达守卫——未到期合同可提前 EXPIRED，成为绕过 terminate 法务门控的捷径

- **控制点**：`app/erp/ct/service/entity/ErpCtContractBizModel.java#expire`（L293-305：仅 `stateMachine.assertCanExpire(contract.getStatus())`——status==ACTIVE 即放行，**无 endDate ≤ today 校验**）对照批量路径 `#expireOverdueContracts` L337-342（查询侧以 `dateBetween("endDate", epoch, today-1)` 约束到期）与 state-machine.md §2 迁移表「ACTIVE→EXPIRED | 系统自动 | **endDate < now()**」
- **证据**：终止一份合同有两条路：terminate（两段化法务审批，RC-R1.34/P1-RC-076 修复的强制义务）与 expire（无任何日期守卫的公开 @BizMutation）。想绕过法务审批的操作者对生效中合同直接 `expire` → 未到期合同即刻 EXPIRED（终态），无审批记录、无版本归档差异、无善后 TODO。job 路径有日期约束而手工单点没有，形成守卫不对称；owner doc 迁移表的触发条件是「endDate 到达」而非「任意时刻」。
- **建议修复方向**：expire 增 `endDate <= today` 守卫（提前结束合同须走 terminate 法务通道）；或 owner doc 显式登记「手工 expire = 强制到期管理员动作」并配权限注解收紧。
- **arm-index 裁决**：新增（RC-R1.35 done 注记声明「手工 expire() 单点语义不变」，该「不变」正包含无日期守卫——本 finding 从守卫完备性维度重新登记，属设计未裁量的守卫缺口）。

### P2-CK-ct-008（D2/D4）doc-auto-purge 批量销毁路径必死——job 上下文无用户，purgeOverdueDocuments 逐条撞 admin 角色守卫全部隔离跳过

- **控制点 A**：`app/erp/ct/service/entity/ErpCtDocumentBizModel.java#purge`（L150-152：`checkPurgeRole();` 首守卫）+ `#checkRole`（L529-535：`IUserContext userContext = IUserContext.get(); if (userContext == null || !userContext.isUserInRole(roleId)) throw ERR_CT_DOCUMENT_ROLE_REQUIRED`）
- **控制点 B**：`app/erp/ct/service/job/ErpCtDocRetentionJob.java#runPurgeScan`（L87-89：`ormTemplate.runInSession(session -> documentBiz.purgeOverdueDocuments(ctx))`，ctx = `new ServiceContextImpl()` L56）
- **证据**：平台源码实证——`nop-entropy/nop-job/nop-job-local/.../BeanMethodJobInvoker.java` grep `IUserContext|loginUser|UserContext` **零命中**：job 调度线程不建立登录用户上下文。开启 `erp-ct.doc-auto-purge=true` 后 job 每次运行 → `purgeOverdueDocuments` 逐条调 `purge` → `checkPurgeRole` 中 `IUserContext.get()==null` → **每条**抛 ROLE_REQUIRED → 外层 catch `LOG.warn("单条文档销毁失败（隔离继续）")`（L228-235）→ 计数恒 0。**自动销毁功能整体死路**，且只有 WARN 日志（B1 形态：失败无告警闭环、功能静默失效）；批量归档不受影响（archive 无角色守卫）。对照：同文件 `resolveOperator`（L537-540）对无用户上下文返回 "system"，证明作者已知 job 无用户，但未把角色守卫做同样的系统身份处理。
- **建议修复方向**：purge 增系统通道判定（job 上下文/服务账号放行或以专用 system 角色判定）；或 purgeOverdueDocuments 批量入口绕过交互角色守卫、保留其余四守卫；owner doc 裁决自动销毁的操作者语义。
- **arm-index 裁决**：新增（RC-R1.80 done 注记声明 job 接线 + 五守卫，未覆盖「角色守卫 × job 无用户上下文」互斥维度）。

### P2-CK-ct-009（D7/D8）webhook eventId 幂等只记最后一个且复用 remark 列——旧事件重放穿透 + completed 重放 500

- **控制点**：`app/erp/ct/service/processor/ErpCtSignatureRequestHandleSignatureCallbackProcessor.java#handleSignatureCallback`（L44-51：`if (eventId != null && Objects.equals(eventId, request.getRemark())) { throw ERR_CT_SIGNATURE_CALLBACK_DUPLICATE_EVENT; } if (eventId != null) { request.setRemark(eventId); }`）
- **证据**：① 幂等载体 = `remark` 列单槽只保留**最后一个** eventId：重放任何非最后事件（provider 重试常发不同 eventId 的重复通知）→ 穿透去重 → `applyEventTransition` 对已终态请求重放 signer.signed → `transitionTo(PARTIALLY)` from=FULLY → `isValidTransition` false → 抛 `ERR_CT_SIGNATURE_ILLEGAL_TRANSITION`（webhook 500，provider 按失败重试造成循环）；重放 completed（新 eventId）→ `completeFullySigned` 首行 `ALREADY_COMPLETED` 抛错 → 同样 500 且**未到达**的 signVersion 级联事务回滚副作用需逐事件分析。② remark 是通用备注列被劫持为幂等标记（语义污染，运营侧改 remark 即破坏幂等）。③ 幂等更新与状态推进同 `dao().updateEntity(request)`（L54），并发回调由 versionProp 乐观锁兜底一胜一败（✓），但败者异常同样向 provider 返回 500。
- **建议修复方向**：独立已处理事件表（eventId 唯一键）或签章请求表加 lastEventId 专用列；重放事件幂等返回 200/no-op 而非抛错（对齐 webhook 重试语义）。
- **arm-index 裁决**：新增（grep「eventId 幂等/remark webhook」零命中）。

### P2-CK-ct-010（D5）RebateTier 零边界/零重叠校验（裸 stub）——负返利率、>100% 返利、倒挂带、重叠带全部静默入账；协议日期倒序无校验

- **控制点 A**：`app/erp/ct/service/entity/ErpCtRebateTierBizModel.java`（全文 14 行裸 CrudBizModel——无 defaultPrepareSave/Update）对照 `ErpCtVolumeDiscountBizModel#validateNoOverlap`（L101-121，同域已有范式）
- **控制点 B**：`app/erp/ct/service/rebate/RebateEngine.java#computeRebate`（L112-124：`cumulative.multiply(percent).divide(100)`——**percent 无 0~100 钳位**；`rebateAmount.signum() > 0` 才走固定额，负固定额静默跌回比例 0）
- **证据**：tier CRUD 可写入：`rebatePercent=-5`（负返利：累计越多负计提越多）、`rebatePercent=200`（返利 2 倍累计金额）、`fromAmount > toAmount` 倒挂带（永不匹配，静默死带）、区间重叠（`matchTier` L130 `max(fromAmount)` 静默取其一——同域 VolumeDiscount 明确抛 `ERR_CT_DISCOUNT_BAND_OVERLAP` 而 tier 无）。`ErpCtRebateAgreementBizModel#defaultPrepareSave`（L63-70）只兜底 businessDate——**startDate < endDate 不校验**（合同侧有 `validateDateRange`，协议侧没有；两列 ORM mandatory 但可倒序）。任务点名「负返利率/负 tier 数量/日期倒序」核查：三处全缺。
- **建议修复方向**：RebateTierBizModel 补 defaultPrepareSave/Update：percent ∈ [0,100]、fromAmount ≤ toAmount、同协议区间不重叠（镜像 VolumeDiscount 范式）；RebateAgreementBizModel 补 startDate<endDate。
- **arm-index 裁决**：新增（arm-index 无 tier 校验维度条目；本域 stub 桩面未在 P1-RC-072..080 覆盖）。

### P2-CK-ct-011（D9）查询面三处全量加载 + N+1——triggerDuePlans 全表扫后 Java 过滤；PROGRESSIVE 计提 O(N²) 重载；搜索/到期查询无界

- **控制点 A**：`app/erp/ct/service/processor/ErpCtInvoicePlanTriggerDuePlansProcessor.java#triggerDuePlans`（L38-55：query 仅 `le("planDate", asOfDate)` + `eq("isInvoiced", false)`——**无 contractId 下推**，加载全域全部到期未开票计划后 L48-51 逐条 `line.getContract()` 懒加载 + Java 判 `line.getContractId().equals(contractId)` 过滤——单合同批量触发退化为全域扫描 + N 次关系懒加载）
- **控制点 B**：`app/erp/ct/service/rebate/RebateEngine.java#accrue`（L69-71：每张发票 `loadTiers` + `sumAccrued`→`loadAccruals` 全量重查）× `ErpCtRebateAgreementRunAccrualProcessor#runAccrual` PROGRESSIVE 循环（L68-76 逐张喂入）——N 张发票 O(N) 次全表聚合，计提明细增长后 O(N²) 查询量
- **证据**：控制点 A 正确写法应为先查合同行 id 集合（`ErpCtContractBizModel#triggerDueInvoicesBeforeExpire` L371-385 正是先 findLines 再 `in("contractLineId", lineIds)` 的下推范式——同模块内自相矛盾）；控制点 B tiers/accruals 在一次 runAccrual 内不变量可提升至循环外/增量累计。`ErpCtDocumentBizModel#searchDocuments`（L306-334 无 limit）、`ErpCtSignatureRequestBizModel#findExpiringRequests`（L161-174 dateBetween epoch 起全表）同属无界族。
- **建议修复方向**：triggerDuePlans 下推 contractId（经 line in 集合或 join）；RebateEngine 循环外缓存 tiers + 增量维护 alreadyAccrued；搜索/到期查询补分页 limit。
- **arm-index 裁决**：新增（N+1/全量加载族 cs-011 同型形态，ct 站点独立登记——orgId 维度另归 012）。

### P2-CK-ct-012（D8，同型 orgId 隔离族）计提明细/签章请求 orgId 不透传 + 返利发票聚合无 orgId 过滤 + orgId=null 削弱发票 UK 并发兜底

- **控制点 A**：`app/erp/ct/service/rebate/RebateEngine.java#accrue`（L75-85：`newEntity()` 后仅设 agreementId/billType/code/amount/date/isSettled——**不复制 agreement.getOrgId()**，`ErpCtRebateAccrual.orgId` 列存在 ORM propId 3）+ `ErpCtSignatureRequestInitSignatureRequestProcessor#initSignatureRequest`（L42-47：不设 orgId，列存在 propId 2）
- **控制点 B**：`ErpCtRebateAgreementRunAccrualProcessor#findPeriodInvoices`（L106-118：仅 posted/partner/日期——**无 orgId**，多组织部署下别组织同 partner 发票混入计提）+ `ErpCtDocumentBizModel#searchDocuments`（无 orgId 过滤）
- **证据**：对照正向范式：同文件链的发票生成处处显式复制 orgId（`createApInvoiceDraft` L141-143 `if (contract.getOrgId() != null) invoice.setOrgId(...)`）。附带削弱：`UK_PUR/SAL_INVOICE_CODE_ORG` 以 `(code, orgId)` 防并发双触发，`contract.orgId` 可空时 NULL 在唯一索引中互不相等（MySQL 语义）→ orgId=null 合同的并发双触发失去 DB 兜底（仅剩 versionProp 单层）。单组织部署无影响——同型族边界声明一致。
- **建议修复方向**：accrue/签名请求保存前透传 orgId；聚合查询下推 orgId；合同 orgId 缺省化（xmeta fill-when-absent）。
- **arm-index 裁决**：同型登记（P2-CK-fin2-007 / P2-CK-mfg-007 / P2-CK-hr-003④ / P2-CK-cs-011 orgId 隔离族，ct 站点计数）。

### P2-CK-ct-013（D5/D3，同型 P1-CK-pur-003 族）CRUD update 无已审守卫——全实体裸面：合同终态复活、审批记录直改 APPROVED、计提 isSettled/金额直改、协议 status 直改

- **控制点**：`ErpCtContractBizModel`（defaultPrepareSave 仅兜底 businessDate + 创建校验，**defaultPrepareUpdate 未 override**——status 可 update_ 直改：TERMINATED→ACTIVE 终态复活、绕过 9 边状态机/法务门控/审批链）、`ErpCtApprovalRecordBizModel`（**approvalStatus/approverId 可直改**——`ErpCtApprovalRecord__update_` 把记录改 APPROVED 绕过 approve 的 PENDING/审批人/锁定三守卫，进而 isChainComplete 放行 activate）、`ErpCtRebateAccrualBizModel`（accruedRebate/isSettled 可直改——结算汇总口径可被手改）、`ErpCtRebateSettlementBizModel`（status 可直改 POSTED 绕过 assertCanPostSettlement/贷项生成）、`ErpCtSignatureRequestBizModel`（status 可直改绕过 isValidTransition）、`ErpCtRebateAgreementBizModel`（**status 可直改 ACTIVE——由于 ACTIVE 是 owner doc 登记的预留死状态（零命名动作 writer），未守卫的 CRUD 直改是当前激活协议的唯一通道**，同型洞同时也是功能通道）、其余 stub（Template/ApprovalMatrix/ContractLine/ConsumptionLine/VolumeDiscount 后者已有重叠校验）。
- **证据**：与本域命名 mutation 守卫面（stateMachine.assertCan* / guardPending / assertCanPostSettlement / defaultPrepareUpdate isInvoiced 锁 + Document 归档只读锁——后者是本域**唯一**有 update 守卫的实体）形成旁路面。InvoicePlan 有 R1.33 字段级锁（✓ 复用不展开）。
- **建议修复方向**：按 Document/InvoicePlan 范式补 defaultPrepareUpdate 字段级守卫（status/approvalStatus/isSettled/posted 族字段拒绝 CRUD 写入）；协议 ACTIVE 激活通道与 owner doc successor（activate mutation）联合裁决。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，ct 站点计数；「RebateAgreement ACTIVE 依赖 CRUD 直改」维度为 owner doc §适用对象三 successor 已知面，不另开条目）。

### P2-CK-ct-014（D4，同型 cron 键漂移家族）erp-ct-approval-timeout job 双键漂移——启用需配外层 cron-expr + enabled + 内层 bean-cron 三键；另 1 死配置常量

- **控制点 A**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-ct-approval-timeout.job.yaml`（`enabled: "@cfg:nop.job.erp-ct-approval-timeout.enabled|false"` + `cronExpr: "@cfg:nop.job.erp-ct-approval-timeout.cron-expr|0 0 1 * * ?"`——**外层调度 cron 键 = `nop.job.erp-ct-approval-timeout.cron-expr`**）对照 bean 执行门控 `ErpCtApprovalTimeoutEscalationJob#execute`（L87-92：`AppConfig.var(ErpCtConfigs.CFG_APPROVAL_TIMEOUT_CRON, "")`——**内层执行键 = `erp-ct.approval-timeout-cron`，默认空恒跳过**）
- **控制点 B**：`ErpCtConfigs.java` L119 `String DEFAULT_APPROVAL_TIMEOUT_CRON = "0 0 1 * * ?";`——零消费死常量（bean 侧硬编码 `""` 默认而非引用）
- **证据**：与已裁决家族同构：只配 description 引导的 `erp-ct.approval-timeout-cron` → job 因 enabled=false 不被调度；只配 enabled=true → job 触发但每次 `LOG.info("erp-ct-approval-timeout-skipped")` 空转。**对照正面形态**：同域 `erp-ct-contract-expiry.job.yaml` 与 `erp-ct-doc-retention.job.yaml` 均为单键模式（cronExpr 直接消费 `erp-ct.contract-expiry-cron`/`erp-ct.doc-retention-cron`，与 bean 共键）——ct 域 3 个 job 中 2 个已是修复形态，仅 approval-timeout 残留漂移。
- **建议修复方向**：approval-timeout job.yaml cronExpr 改消费 `erp-ct.approval-timeout-cron`（对齐域内另两 job 单键形态）；删除或接线 DEFAULT_APPROVAL_TIMEOUT_CRON 死常量。
- **arm-index 裁决**：同型登记（P3-CK-crm-014 / P3-CK-mfg-013 / P3-CK-inv-021 / P3-CK-sal-023 / P3-CK-qa-019 / P3-CK-hr-012 / P2-CK-cs-014 家族，ct 站点 1 处）。

### P2-CK-ct-015（D4/D2）到期分级提醒与审批超时升级无去重载体——同档窗口内每日重复派发

- **控制点 A**：`app/erp/ct/service/job/ErpCtContractExpiryJob.java#notifyByTier`（L137-150：`remaining <= days7 → escalation; remaining <= days15 → warning-15; else → warning-30`——**无任何 sentAt 标记**；job 每日 01:00 运行）对照 use-cases.md UC-CT-05 step 2-4（L97-99）：「到期前 30 天 → 发送…；到期前 15 天 → **再次通知**；到期前 7 天 → 升级通知」——「再次」语义为每档一次
- **控制点 B**：`ErpCtApprovalTimeoutEscalationJob#runTimeoutEscalation`（L110-134：PENDING 且 updateTime < cutoff 的记录**每次扫描都重新升级通知**——无 escalatedAt 标记）对照 UC-CT-07「超时未处理（默认 72h）升级通知上一级」
- **证据**：每日 cron 下：剩 16~30 天的合同每天收一条 30 档通知（最多 ~15 条）、剩 8~15 天每天收 15 档、剩 ≤7 天每天升级通知上级；超时 PENDING 审批记录每个扫描周期重复升级。合同/审批记录实体无 reminderSentAt/escalatedAt 列（grep 零命中），与 cs 域 P2-CK-cs-007 同型（无终态/去重载体）。缓解：job 默认不调度（enabled=false + cron 空）。
- **建议修复方向**：档位去重载体（如按 contractId+档位 通知记录表或合同行 sentAt30/15/7 布尔）；升级通知加 escalatedAt。
- **arm-index 裁决**：新增（RC-R1.35/R1.34 done 注记只声明 job 接线与分档逻辑，未覆盖去重语义）。

### P2-CK-ct-016（D6/D8）返利链多币种零折算——exchangeRate 硬编码 ONE，源币种金额直接混合累加，owner doc 业务规则 3 未实现

- **控制点**：`app/erp/ct/service/processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java#createApInvoiceDraft`（L147：`invoice.setExchangeRate(BigDecimal.ONE);`）+ `ErpCtRebateSettlementPostSettlementProcessor#createNegativeApInvoice`（L116 同构）+ `ErpCtRebateAgreementRunAccrualProcessor#invoiceAmount`（L133-141：直接取 `getTotalAmountWithTax()` 源币种额）
- **证据**：volume-discount.md §业务规则 3 逐字「多币种场景下，返利累计金额统一折算为本位币计算，汇率取每笔过账单据的业务日期汇率」；§跨域协作「master-data（Currency）| 汇率用于返利跨币种累计折算」。实现三站点汇率恒 ONE、amountFunctional=amountSource：多币种供应商/客户（USD+CNY 混合）协议的 tier 命中基数跨币种直接相加（1M USD + 1M CNY = 2M 命中 2M 档），返利金额与贷项发票币种（取合同币种）同下入账——财务口径失真。单币种部署无影响（与全项目 FX 缺口 P1-MA2-002/009 同根因族，本条为 ct 站点登记）。
- **建议修复方向**：与全局 FX 修复（arm-index P1-MA2-002/009 族）联合裁决；短期至少在 owner doc 登记单币种限制并加协议币种一致性校验。
- **arm-index 裁决**：部分复用——FX 全局缺口归 P1-MA2-002/P1-MA2-009（本报告不重复展开其跨域修复方案）；ct 站点返利累计混合币种维度为新增登记。

### P2-CK-ct-017（D6）runAccrual 期间上界不钳制 agreement.endDate——协议期外发票计入本期，连续协议同一发票双计提

- **控制点**：`app/erp/ct/service/processor/ErpCtRebateAgreementRunAccrualProcessor.java#runAccrual`（L56-57：`LocalDate periodStart = agreement.getStartDate(); LocalDate periodEnd = asOfDate == null ? CoreMetrics.today() : asOfDate;`——**periodEnd 不与 `agreement.getEndDate()` 取 min**）
- **证据**：协议（startDate/endDate ORM mandatory，通常一财年）过期后以 asOf=null（或晚于 endDate 的 asOf）再运行 runAccrual：`le("businessDate", periodEnd)` 把 endDate 之后、属**下一协议期**的该 partner 发票全部计入本期累计与计提。volume-discount.md §业务规则 5「协议续签：到期后新协议重新开始累计」+ §ErpCtRebateAgreement「startDate/endDate 协议有效期」隐含计提窗钳制于有效期。连续两协议（2025 / 2026）+ 各自 runAccrual → 交界期同一张发票在两协议各计提一次（per-agreement 去重互不可见）。PROGRESSIVE/PERIOD_END 均受影响。
- **建议修复方向**：`periodEnd = min(asOfDate ?? today, agreement.getEndDate())`；owner doc 显式声明到期后补计提边界。
- **arm-index 裁决**：新增（grep「endDate 钳制/协议期外计提」零命中）。

### P3-CK-ct-018（D1 代码质量）BizModel 层大块死代码复制——R6.7 Processor 迁移后遗留双份实现，漂移风险

- **控制点**：`ErpCtInvoicePlanBizModel`（L136-219：`createApInvoiceDraft`/`createArInvoiceDraft`/`requirePlan` 三段与 `ErpCtInvoicePlanTriggerInvoiceProcessor` L74-157 逐字双份——BizModel 侧零调用方）；`ErpCtRebateAgreementBizModel`（L91-159：`loadAccruedBillCodes`/`findPeriodInvoices`/`sumPeriodInvoices`/`invoiceAmount`/`invoiceCode`/`billTypeFor` 六段与 RunAccrualProcessor 双份）；`ErpCtRebateSettlementBizModel`（L82-217：贷项生成两方法 + findUnsettledAccruals + resolve 三方法与 PostSettlementProcessor 双份）；`ErpCtContractVersionBizModel`（L79-85 `findSiblings` 死复制）；`ErpCtSignatureRequestBizModel`（L177-439 状态机核心/解析/HMAC 整套与 `AbstractErpCtSignatureRequestProcessor` 双份——`WEBHOOK_SECRET` 常量亦双定义 L76/L49）
- **证据**：grep 各 BizModel 方法调用方为零（mutations 全部委托 Processor）。行为影响：零（死代码不执行）；风险：修复阶段改 Processor 活副本而漏 BizModel 死副本（或反向）——本报告 001/004 等修复时须先清理否则双站点修复义务。`WEBHOOK_SECRET` 双定义还会导致配置化改造只改一处。
- **建议修复方向**：删除 BizModel 侧死复制（保留 @BizLoader 脱敏与 mutation 委托）。
- **arm-index 裁决**：新增（arm-index 无死代码维度条目；R6.7 迁移计划未登记清理义务）。

### P3-CK-ct-019（D5）not-found 复用错误码族——实体不存在抛「非法迁移/已开票/未激活」语义错误码

- **控制点**：`ErpCtContractBizModel#requireContract`（L452-459：null → `ERR_CT_ILLEGAL_STATUS_TRANSITION`）、`ErpCtInvoicePlanTriggerInvoiceProcessor#requirePlan`（L150-157：null → `ERR_CT_INVOICE_PLAN_ALREADY_INVOICED`）、`ErpCtRebateAgreementRunAccrualProcessor#requireAgreement`（L83-90：null → `ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE`）、`ErpCtSignatureRequestQueryAndUpdateStatusProcessor`（L16-20：null → `ERR_CT_SIGNATURE_ILLEGAL_TRANSITION`）对照正向范式 `ErpCtDocumentBizModel#requireDocument`（L563-570 专属 `ERR_CT_DOCUMENT_NOT_FOUND`）
- **证据**：调用方按错误码分流（重试/告警/提示）时把「单据不存在」误判为「状态冲突」；日志排障误导。零行为损坏（均 fail-fast）。
- **建议修复方向**：补各实体 NOT_FOUND 错误码（`ErpCtErrors` 已有 document 先例）。
- **arm-index 裁决**：新增（低危质量）。

### P3-CK-ct-020（D5/D7）generateInvoicePlansByTerm 幂等查重 check-then-insert 无 UK 兜底 + amount 无非负校验

- **控制点**：`app/erp/ct/service/processor/ErpCtInvoicePlanGenerateByTermProcessor.java#assertNotDuplicate`（L85-96：先查后插）+ `#createPlan`（L98-107：`plan.setAmount(item.getAmount())` 无 ≥0 校验）对照 ORM 实证 `erp_ct_invoice_plan` **无 `(contractLineId, invoiceTerm, planDate)` UK**（unique-keys 节缺失，仅 contractLineId 普通索引）
- **证据**：并发双提交同 (line,term,date) → 两查都空 → 双计划落库（后触发 triggerInvoice 时第二条因合同状态守卫/invoice UK 间接暴露，但脏计划行残留）；负 amount 计划可入库 → triggerInvoice 生成**负额正向发票草稿**（与返利贷项负额同形但语义为冲减，未经任何审批通道）。owner doc R1.33 注记声明「幂等查重抛 ERR_CT_INVOICE_PLAN_DUPLICATE」——服务端查重在位，仅缺 DB 兜底与值域校验。
- **建议修复方向**：补 UK（纯加性，dual-agent-approval）或 createPlan 前置 amount 非负 + planDate 非空校验。
- **arm-index 裁决**：新增。

### P3-CK-ct-021（D10/D8）settlement 空集/负净额边界——0 额贷项照开、负 total 生成正向「贷项」、悬挂 agreement 无发票却 POSTED

- **控制点**：`app/erp/ct/service/processor/ErpCtRebateSettlementPostSettlementProcessor.java#postSettlement`（L56-81：`unsettled` 空集或 Σ=0 时 `creditAmount = total.negate() = 0` 照常 `createNegative*Invoice`；`total<0`（退货冲销超计提）→ `creditAmount>0` 生成**正额**「贷项」= 方向反转的应收/应付发票；`agreement == null`（悬挂 rebateAgreementId）→ 两分支都不进 → **不生成任何发票**但 L81 照设 `setCreditMemoBillCode("CT-REBATE-"+id)` + L94 置 POSTED——结算单指向不存在的发票）
- **证据**：D10 边界三处：零额贷项发票入库（noise）、负净额结算方向反转（应先冲销原结算而非反向开票——owner doc「跨越后累计回落……冲销多计提的返利」的结算载体未定义）、悬挂引用无守卫。均低频边界。
- **建议修复方向**：`total.signum() <= 0` 时拒绝过账（领域码）；agreement null 前置拒绝；owner doc 裁决负净额结算语义。
- **arm-index 裁决**：新增。

### P3-CK-ct-022（D5 安全）webhook HMAC 密钥硬编码仓库常量且不可配置——签章链（可触发合同 signVersion 生效）鉴别基础弱

- **控制点**：`AbstractErpCtSignatureRequestProcessor.java` L48-49：`public static final String WEBHOOK_SECRET = "erp-ct-signature-callback-secret";`（`ErpCtSignatureRequestBizModel` L76 双定义）+ `#verifySignature`（L294-312 用该常量做 HMAC-SHA256 比对）+ `#completeFullySigned`（L146-147：`certificateUrl = "https://mock.sign/cert/..."` / `evidenceNo = "EVID-..."` 占位值入正式列）
- **证据**：`handleSignatureCallback` 是公开 @BizMutation（webhook 入口），签名校验默认开启（`DEFAULT_SIGNATURE_CALLBACK_SIGNATURE_REQUIRED=true` ✓）但密钥是提交在源码里的公开常量——任何读源者可伪造 completed 事件 → `completeFullySigned` → `signVersion` 把合同版本置 SIGNED（合同生效链）。缓解：`erp-ct.e-signature-enabled` 默认 false（init 侧拒绝）+ 真实 provider 归 successor（owner doc 声明本期 MOCK stub）。`MessageDigest.isEqual` 常量时间比对正确 ✓。
- **建议修复方向**：密钥 config 化（`@cfg` 注入）+ 部署文档强制改默认；真实 provider 接入时一并收敛占位 certificateUrl/evidenceNo。
- **arm-index 裁决**：新增（P1-MA3-006 只裁决 MOCK provider 入 dict 维度）。

### P3-CK-ct-023（D4/D9）approval-timeout 扫描窗无排序饿死——SCAN_LIMIT 200 无 addOrderField，超量 PENDING 时子集不确定

- **控制点**：`app/erp/ct/service/job/ErpCtApprovalTimeoutEscalationJob.java#runTimeoutEscalation`（L110-118：`q.addFilter(eq("approvalStatus", PENDING)); q.addFilter(dateTimeBetween("updateTime", epoch, cutoff)); q.setLimit(SCAN_LIMIT=200);`——**无排序**）
- **证据**：超时 PENDING > 200 时 DB 返回子集不确定，老记录可能被持续挤出（同型 P3-CK-cs-017 弱化形态——cs 是排序+Java 过滤挤占，此处是无排序任意子集）；无「超限放弃」终态。缓解：审批记录量级小、每条隔离。
- **建议修复方向**：`addOrderField("updateTime", false)`（最旧优先，C1.2 校准：第二参 desc=false 升序）。
- **arm-index 裁决**：新增。

### P3-CK-ct-024（D4）5 个死配置键——rebate-enabled/rebate-auto-settle（owner doc 承诺默认 true 自动结算）/rebate-accrual-method/settlement-mode/progressive-retro-topup 零消费

- **控制点**：`app/erp/ct/service/ErpCtConfigs.java` L17-37（`CFG_VOLUME_DISCOUNT_ENABLED`/`CFG_REBATE_ENABLED`/`CFG_REBATE_AUTO_SETTLE`/`CFG_REBATE_ACCRUAL_METHOD`/`CFG_SETTLEMENT_MODE`/`CFG_REBATE_PROGRESSIVE_RETRO_TOPUP`）——grep 全 erp-ct-service 消费点：仅 `CFG_VOLUME_DISCOUNT_ENABLED` 零消费已由 owner doc 裁决（「ct 侧聚合键不叠加消费」，RC-R1.79 注记 ✓ 复用不登记），其余 5 键同样零消费但无裁决
- **证据**：① `erp-ct.rebate-enabled` 默认 false 承诺「年度返利协议是否启用」——runAccrual/postSettlement 无任何 config 门控，功能恒可用（配置面撒谎）；② `erp-ct.rebate-auto-settle` 默认 **true** 承诺「协议到期是否自动触发结算」——**全仓无任何自动结算调度/代码**（settlement DRAFT 仅手工创建），需求缺口（volume-discount.md §结算流程首行「返利协议到期或手动触发结算」）；③ `erp-ct.rebate-accrual-method`「默认计提方法」——`ErpCtRebateAgreementBizModel#defaultPrepareSave` 不消费（accrualMethod 全靠调用方显式传入，ORM mandatory 兜底报错）；④ `erp-ct.settlement-mode`（AUTO=计提后自动生成结算单草稿）无实现；⑤ `erp-ct.rebate-progressive-retro-topup` 无实现（delta 追溯恒开）。
- **建议修复方向**：与 owner doc 联合裁决：实现 auto-settle/settlement-mode（或删除键 + owner doc 标 Deferred）；rebate-enabled 接线为 runAccrual 门控（或删键）；accrual-method 接线为 defaultPrepareSave 缺省。
- **arm-index 裁决**：新增（死配置常量形态同 P3-CK-mfg-013/ast2-015 族一部分，但本条主维度是「承诺行为未实现」）。

## 跨域/同型关联影响面注记（不新建 finding）

| 已登记 finding / arm 条目 | ct 受影响面 | 结论 |
| --- | --- | --- |
| `P1-CK-pur-003` 族（CRUD update 无守卫全域族） | Contract status / ApprovalRecord approvalStatus / RebateAccrual / RebateSettlement status / SignatureRequest status / RebateAgreement status | 同型登记为 P2-CK-ct-013 |
| cron 键漂移家族（crm-014/mfg-013/inv-021/sal-023/qa-019/hr-012/cs-014） | erp-ct-approval-timeout.job.yaml 双键 + DEFAULT_APPROVAL_TIMEOUT_CRON 死常量；expiry/doc-retention 两 job 已是单键修复形态 | 同型登记为 P2-CK-ct-014 |
| orgId 隔离族（fin2-007/mfg-007/hr-003④/cs-011） | RebateEngine accrual orgId / findPeriodInvoices / initSignatureRequest / searchDocuments | 同型登记为 P2-CK-ct-012 |
| `P1-MA2-002`/`P1-MA2-009`（FX 全局缺口） | ct 三站点 exchangeRate=ONE + 源币种混合累计 | 站点登记为 P2-CK-ct-016（修复方案归全局条目） |
| Contract CANCELLED dict 缺值 + 零 writer | `ErpCtContractStateMachine` 据实不纳入终态集 | 复用不登记（owner doc state-machine.md §1 实现漂移注记 + successor 已裁定） |
| RebateAgreement ACTIVE/EXPIRED/SETTLED + Settlement CANCELLED 预留死状态 | 退化分类轴 Bean transitions() 空 | 复用不登记（owner doc §适用对象三/四 intentional reserved 裁决 + successor） |
| activate 对非 FINALIZED current 版本静默跳过 | `ErpCtContractActivateProcessor:66-69` | 复用 watch-only（owner doc §适用对象二 RC-R1.32 D6 注记已登记） |
| triggerInvoice 仅守 ACTIVE——过期未 expire 合同可开 unposted DRAFT 发票 | `ErpCtInvoicePlanTriggerInvoiceProcessor:50-54` | 复用 watch-only residual（owner doc state-machine.md §4 残留风险注记：expiry job 收敛路径已裁决） |
| `P1-MA1-029`（InvoicePlan 跨域写 O-4 豁免，resolved） | triggerInvoice/settlement 贷项经 IDaoProvider 直接持久化 | 复用不登记（posting-exemptions.md §ErpCtRebateSettlementBizModel 已登记 + C18 勘误裁决为设计） |
| `P1-MA2-086`（10 cron job 并发副作用全局裁决） | ct 3 个 job 并发重复运行 | 全局已裁决，不重复 |
| mfg-002 族（双轴 doReject 死锁）同型核查 | ct 无 docStatus/approveStatus 双轴——审批在独立 ErpCtApprovalRecord 实体，合同头单轴 status | **核查不适用**（无双轴联动，无死锁形态） |
| mfg3-003 族（Pattern B custom override 绕过 validateNotCancelled）同型核查 | ct 无 validateNotCancelled 骨架/custom override 审批族（approve/reject 走 ApprovalRecordBizModel 单实现） | **核查不适用**（ct 近似形态 = P2-CK-ct-013 CRUD 旁路，已同型登记） |

## 验证为正确（显式排除，防误报）

- **D1 机械扫描全零**：51 文件 `@Inject private`=0、`System.currentTimeMillis()/LocalDateTime.now()/new Date()`=0（时间一律 `CoreMetrics.*`）、`extends RuntimeException/Exception`=0、`printStackTrace`=0、字典字符串 `==`=0（grep 命中均为 null 判断）。16 处 `catch (Exception` 逐一核验：全部为 best-effort 通知隔离 / 逐条失败隔离 / 包装为 NopException 再抛 / metadataTag 解析降级——无吞咽致业务标志悬挂形态（B1 核查通过；计提/结算主链异常均向上抛由 @BizMutation 事务回滚）。
- **任务点名「triggerDuePlans le(planDate) 白名单修复」——验证在位**：`ErpCtInvoicePlanTriggerDuePlansProcessor:38-44` 经 `daoProvider.daoFor(ErpCtInvoicePlan.class).findAllByQuery(query)` 直查（绕过 XMeta 算子白名单）+ L41-43 注记说明；`ErpCtInvoicePlan.xmeta` 覆盖层为空 props（默认白名单确不含 le）——修复必要且在位 ✓。同域 `ErpCtDocument.xmeta` 显式开放 `fullTextSearch contains/like/startsWith` + `createTime ge/le`——`searchDocuments` 的 ge/le/contains 过滤合法（非白名单违规）✓。
- **SUSPENDED 拦截三站点一致**：triggerInvoice（`ErpCtInvoicePlanTriggerInvoiceProcessor:46-49` 专属 `ERR_CT_CONTRACT_SUSPENDED` + 通用非 ACTIVE `ERR_CT_CONTRACT_NOT_ACTIVE`）、generateInvoicePlansByTerm（`assertContractActive:63-74` 同双码）、periodSummarize（`ErpCtConsumptionPeriodSummarizeProcessor:152-163` 同双码）——state-machine.md §4「SUSPENDED 期间有开票计划到期 → 拦截」落实（消耗**记录**录入不拦、计费面拦——按「不可开票/消耗计费」语义可接受，billing 入口全覆盖）。
- **isInvoiced 防重三层**：① triggerInvoice 入口 `Boolean.TRUE.equals(plan.getIsInvoiced()) → ERR_CT_INVOICE_PLAN_ALREADY_INVOICED`（L38-41）；② R1.33 `defaultPrepareUpdate` 字段锁在位（`ErpCtInvoicePlanBizModel:107-132`：{amount,planDate,invoiceTerm} 触碰 + ORM 脏值追踪取更新前值防同请求解锁绕过，remark 放行，Processor dao 直写回写不受阻——与 owner doc R1.33 注记逐条一致）；③ 并发双触发由 `UK_PUR_INVOICE_CODE_ORG`/`UK_SAL_INVOICE_CODE_ORG`（平台 ORM 实证 L858/L671）+ InvoicePlan `versionProp="version"` 乐观锁双兜底（orgId=null 的 UK 空值盲区归 P2-CK-ct-012 注记）。
- **版本轴核心正确**：`finalizeVersion`（DRAFT→FINALIZED，Bean `assertCanFinalize`）→ `signVersion`（isCurrent 动态守卫 + FINALIZED 来源态 + 同合同 sibling isCurrent 原子翻转 + approvedAt；activate 级联经 IBiz 调用守卫统一在 signVersion 内）与 owner doc §适用对象二逐条一致；`amend` max+1 版本号 + 旧 current 翻转 + 合同头回 DRAFT ✓；`rejectAmend` 恢复目标 D5 选项 B（SIGNED 优先/FINALIZED 回落/无候选清空恢复「无 current」不变量）与裁决一致 ✓（fresh-DRAFT 洞另登 006）。
- **PROGRESSIVE 逐张聚合 + delta 追溯数学正确**：`expectedRebate(新累计所在档整额×档率) − Σ已计提` 天然实现跨档全量补差（volume-discount.md §追溯调整示例 1.2M×2%=24K 复算吻合）、退货负额自然冲销、无复利（每次只补差不再对补差计返利）✓；BigDecimal scale 2/4 HALF_UP 无 double 污染 ✓；`sumAccrued` 含已结算计提行——结算后续新发票 delta 连续性正确（不重复计提已结算部分）✓（PERIOD_END 非幂等另登 001、贷项回吸另登 002、期间上界另登 017——三缺陷均为聚合面，单张数学面正确）。
- **负额发票同向取负正确（C18 勘误裁决为设计）**：postSettlement 贷项 AP/AR 头四金额字段 + 行 unitPrice/amount 全部负额（`total.negate()`），PURCHASE→AP / SALES→AR 方向正确；postSettlement 时点 posted=false 不产凭证、红字凭证在贷项发票后续标准审批过账时产生——与 volume-discount.md §结算流程实现约定 + 时点语义注记（2026-08-25）逐字一致；O-4 豁免登记在 posting-exemptions.md §ErpCtRebateSettlementBizModel（P1-MA1-029 resolved）✓。
- **expireOverdueContracts 批量范式正确**：`IErpCtContractBiz#expireOverdueContracts` 接口 `@SingleSession` 在位（IErpCtContractBiz L102-103）；job 侧 `runInSession` 包裹（@SingleSession 经 IBiz 代理不生效的补偿，hr 先例）；逐合同 try/catch 失败隔离；`dateBetween("endDate", epoch, today-1)` 正确表达「endDate < today」（平台 dateBetween 闭区间）；D3 先开票（planDate ≤ today 逐条 triggerInvoice 失败隔离）→ D4 续期草稿（config-gated + parentContractId 幂等守卫 + code "-RN" 截断保后缀 + UK 防撞）→ assertCanExpire 同手工守卫 ✓（RC-R1.35 修复在位验证）。
- **审批链编排正确（RC-R1.34 修复在位验证）**：approve（PENDING/审批人匹配/锁定守卫 + activateNext 仅激活 WAITING 最新记录）、reject（驳回计数 ≥ max-retries 锁定 + 通知）、resubmit（仅 NEGOTIANCE + latestRejected 基准 + 追加行不原地翻转 + 首节点 PENDING 余 WAITING + notifyTask）；`isChainComplete` 消费「每 order 最新记录均 APPROVED」语义正确（resubmit 后旧 APPROVED 被新 PENDING 取代→链回到未完整，正确）；terminate 法务记录与链记录双轨经 approvalMatrixId null/非 null 双向判别互斥 ✓；`resolveApproverId` roleName→roleId→userId min 确定性（D2 裁决）✓；法务门控不受 approval-enabled 门控（D1 选项 B）✓（approveTermination 状态再守卫缺失另登 005）。
- **消耗计费边界正确（RC-R1.33 修复在位验证）**：`assertPeriodValid` 日期倒序拒绝、ACTIVE 双码守卫、120% 除零守卫（预估 ≤0 特判「任何消耗即超限」，`ratioOf` 返回 null 不除零）、overAmount scale 4 HALF_UP、同事务 NEW 态 flush+evict 重载后触发、notify 返回值驱动 notificationSent、幂等查重复用生成面（D3 裁决抛错语义）✓。
- **文档仓库守卫面正确（RC-R1.80 修复在位验证）**：Legal Hold fail-closed 角色守卫 + generic 管道携带 legalHold 字段同守卫（防绕过 setLegalHold 专用入口）；归档只读（defaultPrepareUpdate 脏值追踪取更新前 isArchived + defaultPrepareDelete 双守卫）；purge 五守卫（角色/legalHold/已归档/合同非 ACTIVE/purgeDate 到达禁提前）+ 逻辑删除 + remark 耐久审计 + best-effort 通知；fullTextSearch 4000 截断 + metadataTags JSON 解析失败降级原文；retentionDate/purgeDate fill-when-absent 手工可覆盖 ✓（job 无用户上下文 × 角色守卫互斥另登 008）。
- **dict 死状态核查（B2）结论**：`erp-ct/contract-status` 6 值全有 writer（DRAFT=save/amend/rejectAmend 目标、NEGOTIATION=submit、ACTIVE=activate/resume/rejectAmend、SUSPENDED=suspend、EXPIRED=expire、TERMINATED=approveTermination）；CANCELLED 缺值为 owner doc 已裁定漂移（复用注记）；`version-status` 3 值全可达；`rebate-agreement-status` ACTIVE/EXPIRED/SETTLED 与 `settlement-status` CANCELLED 为 owner doc intentional reserved（复用注记）；`ocr-status` 4 值（PENDING=save 缺省/PROCESSING=startOcr/COMPLETED=成功或补录/FAILED=失败）、`sign-status` 6 值全有 writer。**dashboard 消费死状态核查 N/A**——ct 域无 dashboard/report BizModel（grep 零命中）。
- **beans/job 接线完整（D4）**：`app-service.beans.xml` 25 bean 全注册（12 Processor + 4 状态机 + 2 引擎 + 2 注册中心 + 2 SPI 实现 + 3 job）；3 个 job.yaml invoker bean id（erpCtContractExpiryJob/erpCtApprovalTimeoutEscalationJob/erpCtDocRetentionJob）与 beans.xml 逐一对应——无孤立声明/漏调（键漂移仅 approval-timeout 一处，归 014）。
- **平台实证 3 处沿用**：`CrudBizModel.get(id, false, ctx)` 不存在返回 null（ck-crm-lead 先例）——各 require* null 检查在位；`FilterBeans.compareOp` 直传不变形（contains/ge/le 由 xmeta 白名单把关，ct 侧已开放）；`BeanMethodJobInvoker` 无 IUserContext（本报告新实证，008 依据）。

## arm-index 复用 or 新增裁决（汇总）

- **新增关键符号（arm-index 零命中）**：`PERIOD_END 重复累加`/`CT-REBATE 贷项回吸`/`matchTier toAmount 排他`/`matchBand toQty 排他`/`独立协议 currencyId null`/`approveTermination 状态再守卫`/`rejectAmend fresh-DRAFT`/`expire endDate 守卫`/`purgeOverdueDocuments 角色死路`/`eventId remark 幂等`/`RebateTier 无校验`/`triggerDuePlans 全表过滤`/`accrue O(N²)`/`orgId accrual`/`approval-timeout cron 双键`/`expiry 提醒去重`/`返利汇率 ONE`/`runAccrual endDate 钳制`/`BizModel 死代码`/`not-found 错误码族`/`InvoicePlan UK`/`settlement 负净额`/`WEBHOOK_SECRET`/`SCAN_LIMIT 排序`/`死配置 5 键` → **新增**。
- **复用（不重复登记，报告中注记）**：P1-MA1-029（O-4 跨域写豁免 resolved）、P1-MA2-071/072（expiry job / NEGOTIATION→TERMINATED resolved）、P1-RC-072..080（本域十大 UC 修复 done 注记——本报告全部 finding 均为对**已实现代码**的质量检查，与「功能缺失」类 arm 条目维度正交）、P1-MA2-086（job 并发全局裁决）、owner doc 已裁定的 CANCELLED/预留死状态/activate 静默跳过/EXPIRED watch-only residual。
- **同型登记**：P2-CK-ct-013（pur-003 族）、P2-CK-ct-014（cron 键漂移家族 ×1 站点）、P2-CK-ct-012（orgId 族）、P2-CK-ct-016（FX 族 ct 站点，修复方案归全局）。
- **同型核查不适用**：mfg-002 双轴死锁族（ct 无双轴）、mfg3-003 Pattern B 族（ct 无 validateNotCancelled 骨架 override 面）。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 4 | P1-CK-ct-001/002/003/004 |
| P2 | 13 | P2-CK-ct-005..017 |
| P3 | 7 | P3-CK-ct-018..024 |

按主维度（每条 finding 计主维度一次）：D3×3（005/006/007）、D4×5（008/014/015/023/024）、D5×5（010/013/019/020/022）、D6×4（001/003/016/017）、D7×1（009）、D8×2（002/012）、D9×1（011）、D10×2（004/021）、D1×1（018）。逐条精确归属：001 D6、002 D8、003 D6、004 D10、005 D3、006 D3、007 D3、008 D4、009 D7、010 D5、011 D9、012 D8、013 D5、014 D4、015 D4、016 D6、017 D6、018 D1、019 D5、020 D5、021 D10、022 D5、023 D4、024 D4。

同型/复用裁决：同型登记 4（013 pur-003 族 / 014 cron 键漂移家族×1 / 012 orgId 族 / 016 FX 族 ct 站点）+ arm-index/owner-doc 复用不登记 7 项（P1-MA1-029、P1-MA2-071/072、P1-MA2-086、CANCELLED 漂移、预留死状态裁定、activate 静默跳过 + EXPIRED watch-only）+ 同型核查不适用 2（mfg-002 双轴族 / mfg3-003 Pattern B 族）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-ct-service 51 文件核心链逐行深读（ContractBizModel 778 / DocumentBizModel 571 / SignatureRequestBizModel 441 + Abstract 326 / ApprovalEngine 275 / ApprovalRecordBizModel 277 / SettlementProcessor 257 / RebateEngine 173 / RunAccrualProcessor 165 / TriggerInvoiceProcessor 166 / ExpiryJob 239 / ApprovalTimeoutJob 199 / 其余 Processor/状态机/SPI/job/configs）；平台源码实证 4 处（BeanMethodJobInvoker 零 IUserContext、UK_PUR/SAL_INVOICE_CODE_ORG、ErpPurInvoice/Line mandatory 列、FilterBeans.compareOp）；ORM 核对（16 实体 versionProp 全在位 / UK 清单 / contract-line 与 invoice-plan 列集——后者实证无 orgId 列 / rebate 族列 mandatory）；接线核对（app-service.beans.xml 25 bean / 3 job.yaml 键形态 / ErpCtDocument+ErpCtInvoicePlan+ErpCtRebateAgreement xmeta 覆盖层 / dict 14 个）；owner docs（README/state-machine/volume-discount/use-cases 相关节）；arm-index contract 域条目（P1-RC-072..080、P1-MA1-029、P1-MA2-071/072 + R1.32-35/79/80 修复注记）逐条裁决。
- **未深查**：`erp-ct-web` AMIS/flux 页面契约 drift（归 C8.2）；`erp-ct-api` 骨架 beans；`erp-ct-dao` 生成物正确性（问题应回溯模型）；测试代码正确性（仅用于行为交叉验证）；pur/sal 侧 `ErpPurCtDiscountApplier`/`ErpSalCtDiscountApplier` 消费实现（归 purchase/sales 检查范围，本报告只查 ct 供给面）；e-signature.md/contract-repository.md/approval-workflow.md 全文逐字对照（只节选与被检行为相关段落）；notify 模板种子 CSV 对 8 个 ct.* 事件的覆盖核对（同型 cs-006 形态，ct 侧未逐一验证——如需登记归修复阶段补充核查）；`like`/`contains` 通配符未转义（keyword 含 `%`/`_` 的过滤放宽——全域同型未裁决，ct 不单列）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-ct-001**——PERIOD_END 重复累加的判定依赖「`sumPeriodInvoices` 的 code 去重对 `PERIOD-日期` 伪码永不命中」推演。建议集成测试实证：PERIOD_END 协议 + 2 张已过账发票，连续两次 runAccrual，断言 `totalAccumulatedAmount` 与 ΣaccruedRebate 是否翻倍（我判定翻倍）。若产品语义是「PERIOD_END 全期重算而非增量」（重算需先冲销旧计提——现实现无冲销，仍属缺陷但修复方向不同），需 owner doc 裁决。
  2. **P1-CK-ct-002**——贷项回吸依赖「贷项发票经标准管道 posted=true 后再次 runAccrual」。若部署上结算后永不重跑 runAccrual（一次性结算），影响面缩小为「跨期多次结算」场景；修复方向（排除 CT-REBATE 前缀 vs 发票来源标记列）需与 pur/sal 域联合裁决。
  3. **P1-CK-ct-003**——「设计含上界 vs 实现半开」若产品已默认半开（javadoc 显式声明 `[fromQty, toQty)`，重叠校验同语义），则降级 not-a-problem 需 owner doc 改文而非改码；建议先与 volume-discount.md 字段表（「含」）逐字比对后裁决——两控制点（rebate/discount）须同裁决。
  4. **P2-CK-ct-008**——job 上下文无用户的判定基于 BeanMethodJobInvoker 源码零 IUserContext 引用；若 nop-job-local 另有调度包装层注入系统用户（本次 grep 未发现），则该 finding 证伪。建议以 `doc-auto-purge=true` 集成运行一次实证（预期 WARN 全跳过、count=0）。
  5. **P2-CK-ct-015/024**——「每档一次」与「auto-settle 默认 true」均为 owner doc 语义解读（「再次通知」「是否自动触发结算」），修复阶段须先 owner doc 裁决再动代码（涉及 dict/列新增的走 dual-agent-approval）。
