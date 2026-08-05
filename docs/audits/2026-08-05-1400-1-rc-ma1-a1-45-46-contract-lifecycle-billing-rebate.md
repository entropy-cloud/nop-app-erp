# A1.45+A1.46 contract 全域（F1 生命周期与签署 + F2 计费与返利）— 需求-实现符合性五级追踪审计报告

> 报告类型：MA1(RC) 五级追踪审计（requirement-compliance mission）
> 工作项：A1.45（MA1 — contract-F1 生命周期与签署 UC-CT-01/02/05/06/07/09）+ A1.46（contract-F2 计费与返利 UC-CT-03/04/08/10）
> UC 覆盖：UC-CT-01 / UC-CT-02 / UC-CT-03 / UC-CT-04 / UC-CT-05 / UC-CT-06 / UC-CT-07 / UC-CT-08 / UC-CT-09 / UC-CT-10（10 UC，contract 域全覆盖）
> 计划：`docs/plans/2026-08-05-1400-1-rc-ma1-a1-45-46-contract-lifecycle-billing-rebate.md`
> 方法论：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> 审计性质：**只读审计**（读代码/测试/报告，不改代码/ORM/api.xml/真相源）
> 真相源层级：L1=`docs/design/contract/use-cases.md`（权威）；L2=`state-machine.md`/`volume-discount.md`/`contract-repository.md`/`e-signature.md`/`approval-workflow.md`（设计参考，冲突以 L1 为准）
> 本报告产出日：2026-08-05
> HEAD 复核 commit：工作树 dirty 仅文档变更，零 Java/ORM/契约变更

---

## 1. 需求契约原文（L1，逐字引用）

> 来源：`docs/design/contract/use-cases.md`（功能契约真相源，权威性层级 2）。逐字引用验收标准原文，不转述。

### UC-CT-01 合同创建与签署（use-cases.md:3-21）

**流程**逐字（:9-18）：
```
1. 经办人选择合同模板（ErpCtTemplate）→ 自动填充合同类型、部分条款。
2. 填写合同头 contractName、partnerId、totalAmount、startDate/endDate。
3. 录入合同行 物料/产品、数量、单价、金额。
4. 系统校验 总金额 = ∑行金额；startDate < endDate。
5. 合同状态 → DRAFT。
6. 提交审批 → NEGOTIATION（创建 v1 版本，isCurrent=true）。
7. 双方确认条款，签署合同文件上传（attachmentId）。
8. 合同管理员确认签署 → ACTIVE（signDate=now）。
```
**异常**逐字（:21）：`金额超预算 → 拦截提交，触发特批流程。`

逐条验收标准拆解（进入 L5 判读）：
- **UC-CT-01-A**：系统校验 总金额 = ∑行金额；startDate < endDate
- **UC-CT-01-B**：提交审批 → NEGOTIATION（创建 v1 版本，isCurrent=true）
- **UC-CT-01-C**：确认签署 → ACTIVE（signDate=now）
- **UC-CT-01-D**（异常）：金额超预算 → 拦截提交，触发特批流程

### UC-CT-02 合同变更与版本管理（use-cases.md:25-43）

**流程**逐字（:31-39）：
```
1. 经办人基于原合同创建变更单（新 DRAFT 合同，parentContractId = 原合同）。
2. 系统复制原合同所有行到变更单，允许增删改。
3. 变更单走标准流程 DRAFT → NEGOTIATION → 签署后 ACTIVE。
4. 变更单生效时，系统
   - 原合同当前版本 isCurrent=false。
   - 创建新版本（versionNo 递增），isCurrent=true。
   - 新版本内容记录变更说明。
5. 后续订单/开票引用新版本条款。
```
**异常**逐字（:43）：`变更单被驳回 → 原合同保持 ACTIVE 不变。`

逐条验收标准（进入 L5 判读）：
- **UC-CT-02-A**：系统复制原合同所有行到变更单，允许增删改
- **UC-CT-02-B**：变更单生效时 原版本 isCurrent=false + 新版本 versionNo 递增 isCurrent=true
- **UC-CT-02-C**（异常）：变更单被驳回 → 原合同保持 ACTIVE 不变

### UC-CT-03 开票计划生成与执行（use-cases.md:47-65）

**流程**逐字（:53-62）：
```
1. 系统按合同行的 invoiceTerm 批量生成 InvoicePlan
   - 预付款（ADVANCE） 签署后 N 天生成。
   - 里程碑（MILESTONE） 按合同约定的里程碑日期生成。
   - 月结（MONTHLY） 每月固定日期生成。
   - 完工（COMPLETION） endDate 生成。
2. InvoicePlan 记录 planDate、amount、invoiceTerm、isInvoiced=false。
3. 到达 planDate 时，系统自动生成 AP/AR Invoice 草稿（调用 finance 域 API）。
4. 财务人员审核发票 → isInvoiced=true、invoiceDate=now、invoiceBillCode 回写。
```
**后置条件**逐字（:63）：`已开票的 InvoicePlan 不允许修改金额。`
**异常**逐字（:65）：`合同处于 SUSPENDED 状态 → 拦截 InvoicePlan 执行，标记为待恢复。`

逐条验收标准（进入 L5 判读）：
- **UC-CT-03-A**：按 invoiceTerm(ADVANCE/MILESTONE/MONTHLY/COMPLETION) 批量生成 InvoicePlan
- **UC-CT-03-B**：到达 planDate 自动生成 AP/AR Invoice 草稿（调 finance 域）
- **UC-CT-03-C**（后置）：已开票的 InvoicePlan 不允许修改金额
- **UC-CT-03-D**（异常）：SUSPENDED → 拦截执行，标记待恢复

### UC-CT-04 消耗计费与用量结算（use-cases.md:69-85）

**流程**逐字（:75-82）：
```
1. 系统/人工按周期（日/周/月）记录 ErpCtConsumptionLine
   - consumptionDate、quantity、unitPrice、amount。
   - sourceBillType/sourceBillCode 记录来源（如 API 调用日志）。
2. 周期结束时，汇总 ConsumptionLine 总量与合同行预估总量对比。
3. 超量部分生成额外计费 InvoicePlan。
4. 系统生成 AP/AR Invoice 草稿给财务。
```
**异常**逐字（:85）：`消耗量超过合同行预估总量 120% → 触发超量审批通知。`

逐条验收标准（进入 L5 判读）：
- **UC-CT-04-A**：按周期记录 ErpCtConsumptionLine（实体载体）
- **UC-CT-04-B**：周期末汇总 ConsumptionLine 总量与合同行预估总量对比
- **UC-CT-04-C**：超量部分生成额外计费 InvoicePlan + AP/AR Invoice 草稿
- **UC-CT-04-D**（异常）：消耗量超预估总量 120% → 触发超量审批通知

### UC-CT-05 合同到期提醒与续期（use-cases.md:89-107）

**流程**逐字（:95-103）：
```
1. nop-job 扫描 erp_ct_contract，条件 `status=ACTIVE AND endDate BETWEEN now() AND now()+30d`。
2. 到期前 30 天 → 发送邮件/站内通知给经办人。
3. 到期前 15 天 → 再次通知，标记为"即将到期"。
4. 到期前 7 天 → 升级通知经办人上级。
5. 经办人决策
   - 续期 按配置 auto-create-renewal-draft → 自动创建续期草稿（parentContractId 关联原合同），走 DRAFT 流程。
   - 不续期 endDate 到达时自动 EXPIRED。
6. endDate 到达 → 系统将合同状态设为 EXPIRED。
```
**异常**逐字（:107）：`endDate 到达仍有未完成的开票计划 → 先完成开票再 EXPIRED。`

逐条验收标准（进入 L5 判读）：
- **UC-CT-05-A**：nop-job 每日扫描 `status=ACTIVE AND endDate BETWEEN now() AND now()+30d`
- **UC-CT-05-B**：到期前 30/15/7 天分级通知（经办人/上级）
- **UC-CT-05-C**：续期 auto-create-renewal-draft（parentContractId 关联）
- **UC-CT-05-D**：endDate 到达 → 自动 EXPIRED
- **UC-CT-05-E**（异常）：endDate 到达仍有未完成开票 → 先完成开票再 EXPIRED

### UC-CT-06 合同提前终止（use-cases.md:111-132）

**流程**逐字（:117-128）：
```
1. 合同管理员发起终止申请，填写 终止原因（违约/协商/其他）+ 上传终止协议附件。
2. 提交法务审批。
3. 法务审批通过后，系统执行终止操作
   - 合同状态 → TERMINATED。
   - 当前版本 isCurrent=false（归档）。
   - 截停所有未执行 InvoicePlan。
   - 生成 TODO 善后结算处理。
4. 财务根据已消耗/已收货数量生成最终结算发票。
5. 经办人确认尾款结清，TODO 完成。
```
**异常**逐字（:132）：`法务驳回 → 合同保持原状态（ACTIVE/SUSPENDED）。`

逐条验收标准（进入 L5 判读）：
- **UC-CT-06-A**：提交法务审批（门控）
- **UC-CT-06-B**：法务通过后 合同状态 → TERMINATED
- **UC-CT-06-C**：当前版本 isCurrent=false（归档）+ 截停未执行 InvoicePlan + 生成善后 TODO
- **UC-CT-06-D**（异常）：法务驳回 → 保持原状态

### UC-CT-07 合同审批工作流（use-cases.md:134-144）

**基本流程**逐字（:141）：
```
1. 经办人提交合同
2. 系统读取 ErpCtApprovalMatrix，按 totalAmount 匹配适用的审批节点列表
3. 生成 ErpCtApprovalRecord（每节点一条），第一个节点激活（PENDING），其余 WAITING
4. 审批人逐节点审批，通过后激活下一节点
5. 所有节点通过后合同可进入 ACTIVE 状态
6. 驳回时经办人修改后可重新提交（仅重新激活驳回节点及其后续节点）
```
**异常**逐字（:143）：`驳回超限（默认 3 次）后锁定需强制升级；超时未处理（默认 72h）升级通知上一级`

逐条验收标准（进入 L5 判读）：
- **UC-CT-07-A**：读 ErpCtApprovalMatrix 按 totalAmount 匹配审批节点列表
- **UC-CT-07-B**：生成 ErpCtApprovalRecord（首 PENDING，其余 WAITING）+ 逐节点审批激活下一节点
- **UC-CT-07-C**：所有节点通过后合同可 ACTIVE
- **UC-CT-07-D**（异常）：驳回超限 3 次锁定强制升级 + 超时 72h 升级上一级

### UC-CT-08 批量折扣与返利（use-cases.md:146-156）

**基本流程**逐字（:153）：
```
批量折扣：1. 合同行配置数量→折扣率映射
2. 订单引用合同时按实际数量匹配折扣率，计算折后价

返利：1. 签订年度返利协议（ErpCtRebateAgreement），配置阶梯（ErpCtRebateTier）
2. 发票过账时记录返利计提（ErpCtRebateAccrual）
3. 累计金额跨越层级时自动追溯调整补差
4. 协议到期触发结算（ErpCtRebateSettlement），生成 AP/AR 信用单
```
**异常**逐字（:155）：`退货冲销导致层级回落时冲销多计提返利`

逐条验收标准（进入 L5 判读）：
- **UC-CT-08-A**（折扣）：订单引用合同时按实际数量匹配折扣率，计算折后价
- **UC-CT-08-B**（返利）：发票过账记录 RebateAccrual + 累计跨越层级自动追溯补差
- **UC-CT-08-C**（返利）：协议到期触发 Settlement 生成 AP/AR 信用单
- **UC-CT-08-D**（异常）：退货冲销致层级回落 → 冲销多计提返利

### UC-CT-09 电子签章（use-cases.md:158-168）

**基本流程**逐字（:165）：
```
1. 经办人选择合同版本，发起签署请求
2. 系统创建 ErpCtSignatureRequest（status=PENDING_SIGNATURE），指定签署人列表
3. 调用 Provider.initSignature() 上传承签文档，配置签署顺序
4. 签署人收到签署链接（邮件/短信/小程序）
5. 签署人在线签署：签署人 1 完成 → status=PARTIALLY_SIGNED
6. 全部签署人完成 → status=FULLY_SIGNED
7. 系统接收 webhook 回调，下载已签署文件
8. 更新 ErpCtContractVersion.status=SIGNED，记录 certificateUrl
9. 签署人拒签 → status=REJECTED，经办人修改后可重新发起
```
**异常**逐字（:167）：`签署超时（signingDeadline 到达）→ status=EXPIRED；提供商调用失败 → status=PENDING_SIGNATURE，记录 errorMsg 后重试`

逐条验收标准（进入 L5 判读）：
- **UC-CT-09-A**：创建 PENDING_SIGNATURE 请求 + 调 Provider.initSignature()
- **UC-CT-09-B**：签署人逐个签署 PENDING→PARTIALLY_SIGNED→FULLY_SIGNED
- **UC-CT-09-C**：webhook 回调下载文件 + 更新 ContractVersion.status=SIGNED + certificateUrl
- **UC-CT-09-D**（异常）：超时 EXPIRED + 拒签 REJECTED + 提供商失败重试

### UC-CT-10 合同仓库与全文检索（use-cases.md:170-180）

**基本流程**逐字（:177）：
```
1. 经办人上传合同文档（扫描件/电子PDF/图片/OFD）
2. 系统创建 ErpCtDocument，识别文档类型和物元数据
3. OCR 引擎自动识别文本（扫描件/图片）/ 提取文字（电子PDF）
4. ocrText 写入，fullTextSearch 构建（含 docName + ocrText + metadataTags）
5. 用户设置元数据标签（party、region、自定义字段）
6. 用户通过全文搜索或高级过滤器（文档类型、日期范围、金额范围、标签）检索文档
7. 保留策略按 retentionDate 自动归档（isArchived=true），purgeDate 到达时系统删除
8. Legal Hold 阻止归档/销毁操作
```
**异常**逐字（:179）：`OCR 识别失败时手动设置或跳过；legalHold=true 时禁止所有归档/销毁`

逐条验收标准（进入 L5 判读）：
- **UC-CT-10-A**：OCR 引擎识别/提取文字 → ocrText 写入 + fullTextSearch 构建（docName+ocrText+metadataTags）
- **UC-CT-10-B**：全文搜索 + 高级过滤（文档类型/日期范围/金额范围/标签）
- **UC-CT-10-C**：保留策略 retentionDate 自动归档（isArchived=true）+ purgeDate 自动销毁
- **UC-CT-10-D**：Legal Hold 阻止归档/销毁

---

## 2. 实现证据（L3，代码路径 `file#method` + 关键行为断言）

> 引用格式锚点 = 文件路径 + 方法名 + 关键行为断言；行号为写时实测导航，漂移不构成引用失效。跨域调用链列全。

### UC-CT-01 创建与签署（签署状态机完整 + 创建校验/版本编排缺失）

调用链（全路径列全）：
- `module-contract/erp-ct-service/.../entity/ErpCtContractBizModel.java#activate:67`（`@BizMutation` 委派 activateProcessor）→ `.../processor/ErpCtContractActivateProcessor.java#activate:33`（NEGOTIATION→ACTIVE 守卫 + type/direction 校验 + 当前版本 FINALIZED 则 signVersion + setStatus(ACTIVE) + setSignDate(today)）。
- `#defaultPrepareSave:57`（仅 setBusinessDate 兜底，**零金额/日期校验**）。

关键行为断言（写时实测）：
- **签署状态机 ✅**：`ErpCtContractActivateProcessor#activate:35-37` 守卫 `status==NEGOTIATION` 非法则抛 `ERR_CT_ILLEGAL_STATUS_TRANSITION`；`:46-48` `setStatus(ACTIVE)+setSignDate(CoreMetrics.today())`。✅ UC-CT-01-C。
- **总金额校验缺失 ❌**：grep `validateTotal|sumLineAmount|totalAmount.*equals|ERR_CT_AMOUNT` 跨 `module-contract/erp-ct-service/src/main` **零命中**。`defaultPrepareSave:57-63` 仅兜底 businessDate，`activate` 仅校验 type/direction，**无 totalAmount=∑行金额 + startDate<endDate 校验**。❌ UC-CT-01-A。
- **NEGOTIATION 提交 + v1 版本创建缺失 ❌**：`ErpCtContractBizModel` 无 `submit`/`negotiate` mutation（grep `submit|negotiate` 跨 BizModel 仅 javadoc 状态机注释命中，无 @BizMutation 方法）。合同到 NEGOTIATION 经通用 CRUD save 直设 status。v1 版本创建：测试（`TestErpCtContractTerminate#createVersion:183`、`TestErpCtContractPosting#createVersion:210`）与 E2E 均**手工 seed 版本**；`amend` 创建版本但无"提交时创建 v1"逻辑。❌ UC-CT-01-B。
- **金额超预算特批缺失 ❌**：grep `budget|超预算|特批|specialApproval` 跨 contract service 零命中。❌ UC-CT-01-D。

### UC-CT-02 变更与版本（版本翻转完整 + 行复制/驳回恢复缺失）

调用链：`ErpCtContractBizModel#amend:130` → `.../processor/ErpCtContractAmendProcessor.java#amend:35`（ACTIVE 守卫 + 遍历版本 max+1 + 旧版本 isCurrent=false + 新版本 isCurrent=true versionNo=max+1 + 合同→DRAFT）。

关键行为断言（写时实测）：
- **版本翻转 ✅**：`ErpCtContractAmendProcessor#amend:42-60` 遍历版本，旧 isCurrent=true 翻 false，新建版本 `setVersionNo(maxVersionNo+1)+setIsCurrent(true)+setStatus(DRAFT)`。✅ UC-CT-02-B。
- **复制原合同行缺失 ❌**：`amend:54-60` 仅 `newEntity()` 版本头（contractId/versionNo/versionDate/isCurrent/status），**零 ErpCtContractLine 复制操作**。`ErpCtContractLineBizModel` 为 17 行 CRUD 桩无 copy 方法。grep `copyLine|cloneLine|复制行` 跨 contract 零命中。❌ UC-CT-02-A。
- **驳回恢复缺失 ❌**：amend 将合同头 setStatus(DRAFT) + 旧版本 isCurrent=false；若变更单被驳回，**无 restore-to-ACTIVE 逻辑**（grep `rejectAmend|restoreActive|驳回.*恢复` 零命中），合同卡在 DRAFT + 无 current 版本。❌ UC-CT-02-C。

### UC-CT-03 开票计划（触发逻辑完整 + invoiceTerm 生成/已开票锁缺失）

调用链：`ErpCtInvoicePlanBizModel#triggerInvoice:65` → `.../processor/ErpCtInvoicePlanTriggerInvoiceProcessor.java#triggerInvoice:35`（已开票守卫 + SUSPENDED 守卫 + ACTIVE 守卫 + INBOUND→createApInvoiceDraft/OUTBOUND→createArInvoiceDraft + 回写 isInvoiced/invoiceBillCode/invoiceDate）；`#triggerDuePlans:71` → `.../processor/ErpCtInvoicePlanTriggerDuePlansProcessor.java#triggerDuePlans:33`（config-gated `erp-ct.invoiceplan-auto-trigger` + 批量 planDate<=asOfDate + isInvoiced=false + 逐 plan 委派 triggerInvoice）。

关键行为断言（写时实测）：
- **触发到 AP/AR 发票草稿 ✅**：`ErpCtInvoicePlanTriggerInvoiceProcessor#triggerInvoice:46-54` SUSPENDED 抛 `ERR_CT_CONTRACT_SUSPENDED` + 非 ACTIVE 抛 `ERR_CT_CONTRACT_NOT_ACTIVE`；`:58-62` INBOUND→AP/OUTBOUND→AR 发票草稿（经 `daoProvider.daoFor(ErpPurInvoice/ErpSalInvoice)` 直接持久化，posted=false/DRAFT——O-4 豁免见 `posting-exemptions.md §ErpCtInvoicePlanBizModel`，P1-MA1-029 resolved）；`:65-67` 回写 isInvoiced/invoiceBillCode/invoiceDate。✅ UC-CT-03-B + UC-CT-03-D。
- **invoiceTerm 批量生成缺失 ❌**：grep `generatePlan|generateInvoicePlan|byInvoiceTerm|ADVANCE|MILESTONE|MONTHLY|COMPLETION` 跨 contract service **仅命中 ErpCtContractLine 实体字段 + 测试种子**，无"按 invoiceTerm 批量生成 InvoicePlan"编排方法。InvoicePlan 须经手工 seed（测试 `createInvoicePlan:231` 证实）。❌ UC-CT-03-A。
- **已开票禁改金额缺失 ❌**：`ErpCtInvoicePlanBizModel` 无 defaultPrepareUpdate 守卫（grep `isInvoiced.*amount|alreadyInvoiced.*lock` 零命中）。❌ UC-CT-03-C。

### UC-CT-04 消耗计费（实体载体存在 + 周期汇总/超量/通知完全缺失）

关键行为断言（写时实测）：
- **实体载体 ✅**：`ErpCtConsumptionLineBizModel`（17 行 CRUD 桩）+ `_ErpCtConsumptionLine` 实体含 consumptionDate/quantity/unitPrice/amount/sourceBillType/sourceBillCode 字段。✅ UC-CT-04-A（载体）。
- **周期汇总对比缺失 ❌**：grep `sumConsumption|overage|consumeSummary|periodSum|estimatedTotal|120` 跨 `module-contract/erp-ct-service/src/main` 零命中（仅测试 rebate 文件命中无关）。无"周期末汇总 ConsumptionLine 总量对比预估总量"逻辑。❌ UC-CT-04-B。
- **超量 InvoicePlan + 发票草稿缺失 ❌**：无超量生成额外 InvoicePlan 编排。❌ UC-CT-04-C。
- **超 120% 审批通知缺失 ❌**：无超量审批通知逻辑。❌ UC-CT-04-D。

### UC-CT-05 到期提醒与续期（手工 expire 存在 + job/分级通知/续期/异常完全缺失）

调用链：`ErpCtContractBizModel#expire:118`（ACTIVE 守卫 + setStatus(EXPIRED) + updateEntity）。

关键行为断言（写时实测）：
- **手工 expire ✅**：`ErpCtContractBizModel#expire:120-124` 守卫 ACTIVE + setStatus(EXPIRED)。✅ UC-CT-05-D（手工路径）。
- **nop-job 扫描缺失 ❌**：grep `nop-job|@Cron|@Scheduled|cron|scheduler|endDate.*BETWEEN|ErpCtContractExpiryJob` 跨 `module-contract` 全模块——**零 Job 类、零 scheduler 注册、零 @CronProvider**（对比 hr 域有 `ErpHrContractExpiryJob`）。❌ UC-CT-05-A。
- **30/15/7 天分级通知缺失 ❌**：grep `notify.*expiry|reminder|30.*15.*7|escalationUserId` 跨 contract service 零命中。无 `IErpSysNotificationBiz` 调用站点。❌ UC-CT-05-B。
- **续期草稿缺失 ❌**：`parentContractId` 字段存在但 grep `renewal|续期|auto-create-renewal-draft` 跨 contract 零业务 Java 使用；`ErpCtConfigs` 无 `erp-ct.auto-create-renewal-draft` 键。❌ UC-CT-05-C。
- **未完成开票先完成再 EXPIRED 缺失 ❌**：`expire:118-126` 不检查未完成 InvoicePlan 直接 setStatus(EXPIRED)。❌ UC-CT-05-E。

### UC-CT-06 提前终止（terminate mutation 存在 + 法务门控/截停/TODO/归档缺失）

调用链：`ErpCtContractBizModel#terminate:97`（守卫 status∈{ACTIVE,NEGOTIATION} + setStatus(TERMINATED) + updateEntity）。

关键行为断言（写时实测）：
- **terminate 状态翻转 ✅**：`#terminate:102-106` 守卫接受 ACTIVE（提前终止）+ NEGOTIATION（谈判破裂）；`:111-112` setStatus(TERMINATED)+updateEntity。✅ UC-CT-06-B（NEGOTIATION→TERMINATED 已落地，P1-MA2-072 resolved-via-implementation 确认）。
- **法务审批门控缺失 ❌**：terminate 无任何审批流/角色校验（直接 @BizMutation 翻转）。grep `legalApproval|法务.*审批|approveTerminate` 零命中。❌ UC-CT-06-A。
- **截停 InvoicePlan + 善后 TODO + 版本归档缺失 ❌**：`terminate:107-110` 注释自承"InvoicePlan 无独立状态列，合同头 TERMINATED 后未开票计划经合同头隐式失效"——但**无显式截停/标记**；**零 TODO 生成**（grep `generateTodo|善后|settlement.*todo` 零命中）；**零版本 isCurrent=false 归档**（terminate 不触碰 ErpCtContractVersion）。❌ UC-CT-06-C。
- **法务驳回路径缺失 ❌**：无 reject-restore 逻辑（terminate 是单向翻转，无"法务驳回→保持原状态"门控）。❌ UC-CT-06-D。

### UC-CT-07 审批工作流（实体 + CRUD 桩存在 + 工作流引擎完全缺失）

调用链：`ErpCtApprovalMatrixBizModel`（17 行 CRUD 桩）+ `ErpCtApprovalRecordBizModel`（17 行 CRUD 桩）。

关键行为断言（写时实测）：
- **实体 + 配置字段 ✅**：`_ErpCtApprovalMatrix` 含 minAmount/maxAmount/approverRole/approvalOrder/contractType/allowSkip/isActive 字段（金额阈值 + 审批角色 + 顺序配置载体完整）；`_ErpCtApprovalRecord` 实体存在。
- **按 totalAmount 匹配节点 + 生成 Record 缺失 ❌**：grep `matchMatrix|generateApprovalRecord|totalAmount.*match|minAmount.*maxAmount` 跨 contract service 零命中。无"读 ApprovalMatrix 按 totalAmount 匹配 + 生成 Record（首 PENDING 其余 WAITING）"逻辑。❌ UC-CT-07-A/B。
- **逐节点审批激活下一节点缺失 ❌**：无 approve/reject BizMutation（ApprovalRecordBizModel 仅 CRUD）。❌ UC-CT-07-B/C。
- **驳回超限 3 次锁定 + 超时 72h 升级缺失 ❌**：grep `rejectCount|rejectLimit|3.*次|72h|timeout.*escalat|escalate.*superior` 跨 contract 零命中。❌ UC-CT-07-D。

### UC-CT-08 批量折扣与返利（返利引擎完整 + 折扣订单侧应用缺失）

调用链：
- 折扣：`ErpCtVolumeDiscountBizModel#resolveDiscount:44`（@BizQuery，band 匹配 fromQty<=qty<toQty + discountPercent/覆盖价 + 回退原价 + lineAmount 计算）+ `#validateNoOverlap:101`（保存时区间带无重叠校验，重叠抛 `ERR_CT_DISCOUNT_BAND_OVERLAP`）。
- 返利：`ErpCtRebateAgreementBizModel#runAccrual:71` → `.../processor/ErpCtRebateAgreementRunAccrualProcessor.java#runAccrual:44`（ACTIVE 守卫 + 聚合期间已过账 AP/AR 发票只读 + PERIOD_END/PROGRESSIVE 分支 + 逐张喂 `RebateEngine`）；`.../rebate/RebateEngine.java#accrue:59`（累计金额 + 命中档 + delta=expected-alreadyAccrued 追溯补差 + 负额捕获退货回落）；`ErpCtRebateSettlementBizModel#postSettlement:70` → `.../processor/ErpCtRebateSettlementPostSettlementProcessor.java#postSettlement:43`（DRAFT→POSTED + 汇总未结算计提 + 生成负额贷项发票 AP/AR + 标记计提 isSettled=true）。

关键行为断言（写时实测）：
- **返利计提 + 层级追溯 ✅**：`RebateEngine#accrue:65-71` newCumulative + computeRebate(命中档整额×率) + delta=expected-alreadyAccrued 自然捕获跨档追溯补差；退货负额经 `accrue` 同路径 delta 为负自动冲销多计提。✅ UC-CT-08-B + UC-CT-08-D。
- **结算信用单生成 ✅**：`ErpCtRebateSettlementPostSettlementProcessor#postSettlement:67-77` creditAmount=total.negate() + PURCHASE→createNegativeApInvoice/SALES→createNegativeArInvoice（O-4 豁免见 `posting-exemptions.md §ErpCtRebateSettlementBizModel`）+ settlement.setCreditMemoBillCode。✅ UC-CT-08-C。
- **折扣订单侧应用缺失 ❌**：`resolveDiscount` API 存在且正确（band 匹配 + 无重叠校验强实现），但 grep `resolveDiscount|IErpCtVolumeDiscountBiz` 跨 `module-purchase`+`module-sales` **零命中**——采购/销售订单行无调用合同折扣 API 站点。L1"订单引用合同时按实际数量匹配折扣率"无消费方。❌ UC-CT-08-A。

### UC-CT-09 电子签章（签章生命周期完整 + Mock provider 可测试实现）

调用链：
- `ErpCtSignatureRequestBizModel#initSignatureRequest:99` → `.../processor/ErpCtSignatureRequestInitSignatureRequestProcessor.java#initSignatureRequest:23`（config-gated `erp-ct.e-signature-enabled` + FINALIZED 守卫 + 建 PENDING_SIGNATURE 请求 + 调 `providerRegistry.getProvider().initSignature()` 回填 providerRequestId）。
- `#handleSignatureCallback:108` → `.../processor/ErpCtSignatureRequestHandleSignatureCallbackProcessor.java`（HMAC 校验 + eventId 幂等 + 按 event 推进 `applyEventTransition`）。
- `#queryAndUpdateStatus:118` → `.../processor/ErpCtSignatureRequestQueryAndUpdateStatusProcessor.java`（轮询兜底，共用 `applyStatusTransition`）。
- `#completeFullySigned:245`（幂等守门 + `provider.retrieveCertificate()` + attachmentFileId/certificateUrl/evidenceNo + 调 `contractVersionBiz.signVersion`）。
- Provider：`.../spi/mock/MockSignatureProvider.java`（initSignature→providerRequestId + queryStatus 确定性推进 PARTIALLY→COMPLETED + retrieveCertificate 占位字节）。

关键行为断言（写时实测）：
- **init + Provider 调用 ✅**：`ErpCtSignatureRequestInitSignatureRequestProcessor#initSignatureRequest:53-55` 调 `providerRegistry.getProvider(effectiveProvider).initSignature(initReq)` 回填 providerRequestId。✅ UC-CT-09-A。
- **逐个签署状态迁移 ✅**：`applyEventTransition:185-198` signer.signed→PARTIALLY / completed→FULLY（调 completeFullySigned）/ rejected·declined→REJECTED / expired→EXPIRED；`isValidTransition:281-295` PENDING→{PARTIALLY/FULLY/REJECTED/EXPIRED/CANCELLED} + PARTIALLY→{FULLY/REJECTED/EXPIRED/CANCELLED}。✅ UC-CT-09-B。
- **webhook 回调 + SIGNED + certificateUrl ✅**：`completeFullySigned:255-267` retrieveCertificate→storeCertificateArtifact→setAttachmentFileId + setCertificateUrl("https://mock.sign/cert/"+providerRequestId) + setEvidenceNo + 调 signVersion（FINALIZED→SIGNED + isCurrent 翻转）。✅ UC-CT-09-C。
- **超时/拒签/失败 ✅**：applyEventTransition EXPIRED/REJECTED 分支 + `ErpCtSignatureRequestInitSignatureRequestProcessor:60-64` 提供商失败抛 `ERR_CT_SIGNATURE_INIT_FAILED`。✅ UC-CT-09-D。
- **真实三方 provider（e签宝/DocuSign/Tsign）= 文档化 successor**：`MockSignatureProvider` 为 SPI 测试桩，config-gated `erp-ct.e-signature-enabled` 默认 false（关则走线下签署附件上传 + signVersion 直接接入）。真实 HTTP 集成归 follow-up（e-signature.md + ErpCtConfigs 注释）。

### UC-CT-10 合同仓库与全文检索（实体字段完整 + OCR/全文/保留/Legal Hold 引擎完全缺失）

调用链：`ErpCtDocumentBizModel`（17 行 CRUD 桩）。

关键行为断言（写时实测）：
- **实体字段完整 ✅**：`_ErpCtDocument` 含 ocrText/ocrStatus/fullTextSearch/metadataTags/retentionDate/archiveDate/purgeDate/isArchived 字段（结构载体完整）。
- **OCR 引擎 + fullTextSearch 构建缺失 ❌**：grep `ocr|fullText|extractText|tesseract` 跨 `module-contract/erp-ct-service/src/main` **零业务逻辑命中**（仅 `_gen` 实体 + API bean 字段定义）。`ErpCtDocumentBizModel` 为 CRUD 桩，无"OCR 识别→ocrText 写入→fullTextSearch 构建"编排。❌ UC-CT-10-A。
- **全文搜索 + 高级过滤缺失 ❌**：无 fullText 搜索 @BizQuery 方法。❌ UC-CT-10-B。
- **保留策略归档/销毁缺失 ❌**：无 retentionDate→isArchived 自动归档 + purgeDate→delete 自动销毁逻辑（grep `retention|purge|archive` 零业务逻辑命中）。❌ UC-CT-10-C。
- **Legal Hold 字段不存在 + 逻辑缺失 ❌**：`_ErpCtDocument` 属性集**无 legalHold 字段**（grep `legalHold|legal_hold` 跨 erp-ct-dao 零命中），L1 UC-CT-10-D + owner doc `contract-repository.md:238` 声明的 Legal Hold **无 ORM 结构载体**。❌ UC-CT-10-D（结构 + 逻辑双缺）。

---

## 3. 测试证据（L4，测试断言引用 + 断言强度）

> 断言强度：强断言 = 断言验收标准语义；弱断言 = 仅断言非空/状态码；仅冒烟 = 仅通过无值断言。

### UC-CT-01 测试证据
- `TestErpCtContractTerminate.java#createContract:179` + `TestErpCtContractPosting.java#activateRpc:127`：**强断言** activate 后 status=ACTIVE。**未断言** totalAmount=∑行金额（无该断言）+ **手工 seed 版本**（`createVersion:183/210`，证实 v1 非自动创建）。
- E2E `tests/e2e/business-actions/ct-contract-lifecycle.action.spec.ts:57`：**强断言** new contract status=NEGOTIATION（经 save 直设）+ activate→ACTIVE:62。**零金额校验断言 + 零 v1 自动创建断言**。

### UC-CT-02 测试证据
- E2E `ct-contract-lifecycle.action.spec.ts:108-112`：**强断言** amend→DRAFT + versionCount>0。**未断言** 行复制（变更单行数 == 原合同行数）+ 未断言驳回恢复 ACTIVE。

### UC-CT-03 测试证据
- `TestErpCtContractPosting.java#testTriggerInvoiceGeneratesApInvoice:49` + `#testTriggerInvoiceGeneratesArInvoice:71`：**强断言** trigger→isInvoiced=true + invoiceBillCode 非空 + invoice.docStatus=DRAFT + posted=false。**强断言** SUSPENDED 拒绝（`testTriggerInvoiceRejectedForSuspendedContract:93` assertNotEquals(0)）。**未断言** invoiceTerm 批量生成（InvoicePlan 手工 seed:231 证实）+ 未断言已开票禁改金额。

### UC-CT-04 测试证据
- **零 dedicated 测试**：grep `consumption|overage|120|periodSum` 跨 contract 测试目录零命中。UC-CT-04-B/C/D 无测试覆盖。

### UC-CT-05 测试证据
- `TestErpCtContractTerminate.java:120`（expire 经 executeRpc ErpCtContract__expire）：**强断言** ACTIVE→EXPIRED。**未断言** job 扫描 + 分级通知 + 续期草稿 + 未完成开票先完成（均无实现故无测试）。

### UC-CT-06 测试证据
- `TestErpCtContractTerminate.java#testTerminateFromActiveSucceeds:48` + `#testTerminateFromNegotiationSucceeds:60`：**强断言** ACTIVE/NEGOTIATION→TERMINATED + 非法源态拒绝（`assertTerminateRejected:92` DRAFT/SUSPENDED/EXPIRED/TERMINATED）。**未断言** 法务审批门控 + InvoicePlan 截停 + TODO 生成 + 版本归档（均无实现）。

### UC-CT-07 测试证据
- **零 dedicated 测试**：grep `ApprovalMatrix|ApprovalRecord|approval.*match|rejectCount|72h` 跨 contract 测试目录零命中。UC-CT-07-A/B/C/D 无测试覆盖（ApprovalMatrix/Record 仅 CRUD 桩）。

### UC-CT-08 测试证据
- `TestErpCtContractRebate.java`：**强断言** 返利计提（PERIOD_END/PROGRESSIVE + 跨档追溯补差 + 层级命中）。
- `TestErpCtRebateSettlementEnd.java`：**强断言** 结算端到端（计提→结算→负额贷项发票→isSettled=true）。
- `TestErpCtContractPosting.java`：**强断言** trigger 发票草稿（UC-CT-03 复用）。
- **未断言** 折扣订单侧应用（purchase/sales 无 resolveDiscount 调用，无跨域测试）。

### UC-CT-09 测试证据
- `TestErpCtESignature.java`：**强断言** init→PENDING + callback completed→FULLY + signVersion SIGNED + certificateUrl + 拒签 REJECTED + 超时 EXPIRED（覆盖 UC-CT-09-A/B/C/D 四验收标准，MA5 评级强断言）。

### UC-CT-10 测试证据
- **零 dedicated 测试**：grep `ocr|fullText|legalHold|retention|purge|isArchived` 跨 contract 测试目录零命中。UC-CT-10-A/B/C/D 无测试覆盖。

---

## 4. 运行时行为证据（L5）

> 本切片为 contract 域全域需求契约符合性首份证据（contract 域 MA1 首审）。无 contract 专属 MA2 状态机报告（A2.14 覆盖 contract 合同/InvoicePlan 状态机迁移 + EquipmentStatusLinker，但不含创建校验/审批工作流/计费/返利/仓库需求契约维度）。L5 存疑点登记 §7 静态存疑点清单交 MA4。

| UC | L5 行为证据 | 来源 |
|----|------------|------|
| UC-CT-01 | 签署状态机 NEGOTIATION→ACTIVE + signDate 已证实（TestErpCtContractPosting 强测 + E2E ct-contract-lifecycle 强测）；总金额校验 + v1 自动创建 + 预算特批为本切片新发现（零实现） | 本切片 JUnit + E2E + grep |
| UC-CT-02 | 版本翻转 isCurrent + versionNo 递增已证实（E2E ct-contract-lifecycle amend 强测）；行复制 + 驳回恢复为本切片新发现（零实现） | E2E + grep |
| UC-CT-03 | trigger→AP/AR 草稿 + SUSPENDED 拦截已证实（TestErpCtContractPosting 强测）；invoiceTerm 批量生成 + 已开票锁为本切片新发现（零实现） | TestErpCtContractPosting + grep |
| UC-CT-04 | 实体载体存在；周期汇总/超量/通知为本切片新发现（零实现） | 本切片 grep |
| UC-CT-05 | 手工 expire 已证实（TestErpCtContractTerminate 强测）；job/分级通知/续期/异常为本切片新发现（零实现，owner doc Deferred R1.22） | TestErpCtContractTerminate + grep + state-machine.md §2/§4/§7 Deferred 注 |
| UC-CT-06 | terminate ACTIVE/NEGOTIATION→TERMINATED 已证实（TestErpCtContractTerminate 强测）；法务门控/截停/TODO/归档为本切片新发现（零实现） | TestErpCtContractTerminate + grep |
| UC-CT-07 | 实体 + 配置字段存在；工作流引擎为本切片新发现（零实现，仅 CRUD 桩） | 本切片 grep |
| UC-CT-08 | 返利计提/追溯/结算信用单已证实（TestErpCtContractRebate + TestErpCtRebateSettlementEnd 强测）；折扣订单侧应用为本切片新发现（purchase/sales 零调用） | JUnit + grep |
| UC-CT-09 | 签章生命周期 PENDING→PARTIALLY→FULLY + webhook + certificateUrl + REJECTED/EXPIRED 已证实（TestErpCtESignature 强测）；Mock provider 为 SPI 测试桩，真实 provider 归 successor | TestErpCtESignature |
| UC-CT-10 | 实体字段完整；OCR/全文/保留/Legal Hold 引擎为本切片新发现（零实现，legalHold 字段不存在） | 本切片 grep |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论，§2 判据）

### 五级追踪矩阵

| UC 编号 | L1 需求契约 | L2 owner doc（设计参考） | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|---------|------------|------------------------|------------|------------|--------------|-----------|
| UC-CT-01 | use-cases.md:9-18（逐字见 §1） | state-machine.md §2/§9 场景 A（设计参考，冲突以 L1 为准） | `ErpCtContractActivateProcessor#activate:33`（签署 ✅）+ defaultPrepareSave:57 无金额校验 ❌ + 无 submit/v1 创建 ❌ | TestErpCtContractPosting activate（强，未断言金额/v1）+ E2E ct-contract-lifecycle（强状态机） | 签署状态机已证实；创建校验/v1/预算特批缺失 | **P1**（P1-RC-072） |
| UC-CT-02 | use-cases.md:31-39（逐字见 §1） | state-machine.md §2/§9 场景 B（设计参考） | `ErpCtContractAmendProcessor#amend:35`（版本翻转 ✅）+ 零行复制 ❌ + 零驳回恢复 ❌ | E2E ct-contract-lifecycle amend（强版本计数，未断言行复制/驳回恢复） | 版本翻转已证实；行复制/驳回恢复缺失 | **P1**（P1-RC-073） |
| UC-CT-03 | use-cases.md:53-63（逐字见 §1） | state-machine.md §InvoicePlan 触发（设计参考） | `ErpCtInvoicePlanTriggerInvoiceProcessor#triggerInvoice:35`（触发+SUSPENDED ✅）+ 零 invoiceTerm 生成 ❌ + 零已开票锁 ❌ | TestErpCtContractPosting trigger（强 AP/AR+SUSPENDED，未断言生成/锁） | 触发链已证实；invoiceTerm 生成/已开票锁缺失 | **P1**（P1-RC-074） |
| UC-CT-04 | use-cases.md:75-82（逐字见 §1） | volume-discount.md §消耗计费（设计参考，活跃要求周期汇总+超量） | `ErpCtConsumptionLineBizModel` 17 行 CRUD 桩 + grep 周期汇总/超量/120 零命中 ❌ | 零 dedicated 测试 | 实体载体存在；汇总/超量/通知完全缺失 | **P1**（P1-RC-075） |
| UC-CT-05 | use-cases.md:95-103（逐字见 §1） | state-machine.md §2/§4/§7 **Deferred 注**（R1.22 deferral，§4 三判据复核见下） | `ErpCtContractBizModel#expire:118`（手工 ✅）+ 零 Job/通知/续期 ❌ | TestErpCtContractTerminate expire（强手工，未断言 job/通知/续期） | 手工 expire 已证实；job/分级通知/续期/异常缺失 | **P1**（reuse P1-MA2-071） |
| UC-CT-06 | use-cases.md:117-128（逐字见 §1） | state-machine.md §2/§6（活跃要求法务审批门控，设计参考） | `ErpCtContractBizModel#terminate:97`（状态翻转 ✅）+ 零法务门控/截停/TODO/归档 ❌ | TestErpCtContractTerminate（强状态翻转，未断言门控/截停/TODO/归档） | terminate 翻转已证实；法务门控/截停/TODO/归档缺失 | **P1**（P1-RC-076） |
| UC-CT-07 | use-cases.md:141-143（逐字见 §1） | approval-workflow.md（活跃要求按金额匹配+逐节点+驳回超限+超时升级，无 Deferred 标注） | `ErpCtApprovalMatrixBizModel`/`ErpCtApprovalRecordBizModel` 17 行 CRUD 桩 + grep match/rejectCount/72h 零命中 ❌ | 零 dedicated 测试 | 实体/配置字段存在；工作流引擎完全缺失 | **P1**（P1-RC-077） |
| UC-CT-08 | use-cases.md:153-155（逐字见 §1） | volume-discount.md §折扣应用/§返利（活跃要求订单引用折扣，无 Deferred） | `RebateEngine#accrue:59`+`ErpCtRebateSettlementPostSettlementProcessor#postSettlement:43`（返利 ✅）+ `resolveDiscount:44`（API ✅）+ grep purchase/sales 零调用 ❌ | TestErpCtContractRebate + TestErpCtRebateSettlementEnd（强返利，未断言订单侧折扣） | 返利引擎已证实；折扣订单侧应用缺失 | **接受 on B/C/D 返利** + **P1** A（P1-RC-078） |
| UC-CT-09 | use-cases.md:165-167（逐字见 §1） | e-signature.md §状态机/§Webhook（设计参考，行为已实现） | `ErpCtSignatureRequestInitSignatureRequestProcessor#initSignatureRequest:23`+`completeFullySigned:245`+`applyEventTransition:183`（签章生命周期 ✅）+ MockSignatureProvider | TestErpCtESignature（强，覆盖 A/B/C/D 四验收标准） | 签章生命周期已证实（Mock provider 可测试实现） | **接受**（真实 provider 归 successor） |
| UC-CT-10 | use-cases.md:177-179（逐字见 §1） | contract-repository.md §OCR/§全文检索/§保留策略/§Legal Hold（活跃要求，无 Deferred） | `ErpCtDocumentBizModel` 17 行 CRUD 桩 + grep ocr/fullText/retention/legalHold 零业务命中 ❌ + legalHold 字段不存在 ❌ | 零 dedicated 测试 | 实体字段完整；OCR/全文/保留/Legal Hold 引擎完全缺失 | **P1**（P1-RC-079） |

### 每 UC 符合性结论（§2 判据 + §4 三判据复核）

#### UC-CT-01 = **P1**（new P1-RC-072）
- **命中判据**：§2 ①（行为实质偏离验收标准——UC-CT-01-A 总金额=∑行金额 + startDate<endDate 校验缺失 + UC-CT-01-B NEGOTIATION 提交创建 v1 版本缺失 + UC-CT-01-D 金额超预算特批缺失）。
- **三源对照**：L1 use-cases.md:13/15/21 明确 step 4 校验 + step 6 v1 + 异常特批为系统行为；L3 `defaultPrepareSave:57` 仅兜底 businessDate + 无 submit mutation + grep 金额校验零命中；L4 测试手工 seed 版本证实 v1 非自动。
- **§4 三判据**：不适用（state-machine.md 无 UC-CT-01 创建校验/v1/预算的 Deferred 标注，L1 明确要求）。
- **触及保护区域**：纯 BizModel 代码逻辑（新增 validateTotalAmount + submit mutation + v1 创建编排 + 预算校验），预授权不触 §5 ask-first。

#### UC-CT-02 = **P1**（new P1-RC-073）
- **命中判据**：§2 ①（UC-CT-02-A 系统复制原合同所有行到变更单缺失 + UC-CT-02-C 变更单驳回→原合同保持 ACTIVE 缺失）。
- **三源对照**：L1 use-cases.md:33/43 明确"系统复制原合同所有行"+"驳回→原合同保持 ACTIVE"；L3 `ErpCtContractAmendProcessor#amend:54-60` 仅建版本头零行复制 + 零驳回恢复；L4 E2E 未断言行复制/驳回恢复。
- **§4 三判据**：不适用（无 Deferred 标注）。
- **触及保护区域**：纯 BizModel/Processor 代码逻辑（amend 增 copyLines + reject-restore 编排），预授权不触 §5 ask-first。

#### UC-CT-03 = **P1**（new P1-RC-074）
- **命中判据**：§2 ①（UC-CT-03-A 按 invoiceTerm 批量生成 InvoicePlan 缺失 + UC-CT-03-C 已开票禁改金额缺失）；接受 on UC-CT-03-B（触发 AP/AR 草稿 ✅）+ UC-CT-03-D（SUSPENDED 拦截 ✅）。
- **三源对照**：L1 use-cases.md:54-58 明确 invoiceTerm 四类批量生成 + :63 后置"已开票不允许修改金额"；L3 grep generatePlan/byInvoiceTerm 零命中 + 无 defaultPrepareUpdate isInvoiced 守卫；L4 测试手工 seed InvoicePlan 证实。
- **§4 三判据**：不适用（无 Deferred 标注）。
- **触及保护区域**：纯 BizModel 代码逻辑（新增 generateInvoicePlansByTerm + 已开票金额守卫），预授权不触 §5 ask-first。

#### UC-CT-04 = **P1**（new P1-RC-075）
- **命中判据**：§2 ①（UC-CT-04-B/C/D 周期汇总对比 + 超量 InvoicePlan + 超 120% 审批通知完全缺失）；接受 on UC-CT-04-A（实体载体存在 ✅）。
- **三源对照**：L1 use-cases.md:79-85 明确周期汇总 + 超量生成 + 120% 通知；L3 `ErpCtConsumptionLineBizModel` 17 行 CRUD 桩 + grep sumConsumption/overage/120 零命中；L4 零 dedicated 测试。
- **§4 三判据**：不适用（volume-discount.md §消耗计费活跃要求，无 Deferred 标注）。
- **触及保护区域**：纯 BizModel 代码逻辑（新增 periodSummarize + overage InvoicePlan + 通知编排），预授权不触 §5 ask-first。

#### UC-CT-05 = **P1**（reuse P1-MA2-071）—— §4 三判据关键裁决
- **命中判据**：§2 ①（UC-CT-05-A nop-job 扫描 + UC-CT-05-B 30/15/7 分级通知 + UC-CT-05-C 续期草稿 + UC-CT-05-E 未完成开票先完成均缺失）；接受 on UC-CT-05-D（手工 expire ✅）。
- **三源对照**：L1 use-cases.md:96-107 明确 nop-job 扫描 + 30/15/7 分级通知 + auto-create-renewal-draft + 异常先完成开票；L3 零 Job/通知/续期；L4 仅手工 expire 断言。
- **§4 三判据关键裁决**（expiry job/renewal/notification Deferred 标注是否经人工批准）：
  - **(i) plan 含独立 plan-audit 通过记录**：R1.22 plan 存在且 Plan Status=completed（P1-MA2-071 resolved），但其裁决为 AI 子代理执行（git log 全 AI commits），按 methodology §4 line 177「代理独立审计通过 = 审计裁决质量证据，不算人工批准」→ **(i) 在"人工批准"意义上不成立**。
  - **(ii) owner doc 显式 documented simplification 标注且经人工批准**：L2 `state-machine.md §2:47-49`+`§4:69-71`+`§7:107` 含显式 Deferred 注记（EXPIRED 自动化 Job + 续期草稿 + 30/15/7 通知均标 Deferred），但 git log 全 AI commits 无人工批准痕迹（§4 line 172「AI 自标 ≠ 人工批准」）→ **(ii) 不成立**。
  - **(iii) product-scope 范围裁剪登记**：product-scope.md:41 列合同为域能力（含起草/审批/电子签/履约），未将到期自动化列入"不在范围"→ **(iii) 不成立**。
  - **裁决**：三判据均不成立 → L2 Deferred 标注不构成合法范围裁剪。L1 use-cases.md:96-107 为活跃需求契约，权威性高于 L2（§4 冲突以 L1 为准）。**UC-CT-05 到期自动化/分级通知/续期为 P1 强制实现**（Q4=(a) 无例外通道），对齐 A1.44 UC-MAIN-10（P1-RC-071 OEE）§4 三判据先例。
- **复用裁决**：本切片缺口与既有 **P1-MA2-071**（contract EXPIRED 自动到期 Job 缺失 + 续期草稿缺失，resolved R1.22 via deferral）**同根因同控制点**（contract 到期自动化维度）——按 §7 复用规则追加 RC A1.45 交叉引用注记**不新建**。但声明 R1.22 resolution 实为 resolved-via-deferral（owner doc Deferred 标注）而非 resolved-via-implementation，从 Q4=(a) 需求契约视角 L1 仍活跃，MR1（R1.0 展开为 RC-R1.n）须实现 `ErpCtContractExpiryJob`（cron-gated 扫描 ACTIVE 且 endDate<now 批量 expire）+ config-gated `erp-ct.auto-create-renewal-draft` + 30/15/7 分级通知经 `IErpSysNotificationBiz`。本复用**不重开 P1-MA2-072**（NEGOTIATION→TERMINATED 已 resolved-via-implementation，terminate 守卫已扩展，HEAD 复核确认）。
- **触及保护区域**：新增 Job 类 + scheduler + job.yaml 注册属代码逻辑预授权；30/15/7 通知经 IErpSysNotificationBiz 预授权；不触 §5 ask-first。

#### UC-CT-06 = **P1**（new P1-RC-076）
- **命中判据**：§2 ①/③（UC-CT-06-A 法务审批门控缺失 + UC-CT-06-C 截停 InvoicePlan + 善后 TODO + 版本归档缺失 + UC-CT-06-D 法务驳回路径缺失）；接受 on UC-CT-06-B（terminate 状态翻转 ✅）。
- **三源对照**：L1 use-cases.md:121-128 明确法务审批门控 + 截停 + TODO + 归档；L3 `terminate:97-113` 单向翻转零门控/截停/TODO/归档；L4 仅断言状态翻转。
- **§4 三判据**：不适用（state-machine.md §6 活跃要求"需法务审批"，无 Deferred 标注）。
- **触及保护区域**：纯 BizModel 代码逻辑（新增 legal-approval gate + InvoicePlan 截停 + TODO 生成 + 版本归档编排），预授权不触 §5 ask-first。

#### UC-CT-07 = **P1**（new P1-RC-077）
- **命中判据**：§2 ①/⑤（UC-CT-07-A/B/C/D 按 totalAmount 匹配节点 + 生成 Record + 逐节点审批 + 驳回超限 3 次锁定 + 超时 72h 升级完全缺失 + 测试断言完全缺失）。
- **三源对照**：L1 use-cases.md:141-143 明确匹配 + Record 生成 + 逐节点 + 驳回超限 + 超时升级；L3 ApprovalMatrix/Record BizModel 仅 17 行 CRUD 桩 + grep match/rejectCount/72h 零命中；L4 零 dedicated 测试。
- **§4 三判据**：不适用（approval-workflow.md 活跃要求完整工作流，无 Deferred 标注）。
- **触及保护区域**：纯 BizModel 代码逻辑（新增 ApprovalWorkflowEngine + matchByAmount + generateRecords + approve/reject + rejectCount/timeout 编排），预授权不触 §5 ask-first。

#### UC-CT-08 = **接受 on B/C/D 返利** + **P1** A（new P1-RC-078）
- **UC-CT-08-B/C/D（返利）= 接受**：`RebateEngine#accrue` + `ErpCtRebateSettlementPostSettlementProcessor#postSettlement` 返利计提 + 层级追溯 + 退货冲销 + 结算信用单完整，TestErpCtContractRebate/TestErpCtRebateSettlementEnd 强断言。命中"接受"。
- **UC-CT-08-A（折扣订单侧应用）= P1**：§2 ④（跨域契约行为不一致——L1 要求"订单引用合同时按实际数量匹配折扣率"，purchase/sales 域零 resolveDiscount 调用站点）。
- **三源对照**：L1 use-cases.md:153 明确"订单引用合同时按实际数量匹配折扣率"；L3 `resolveDiscount` API 存在且正确但 grep `resolveDiscount|IErpCtVolumeDiscountBiz` 跨 module-purchase+module-sales 零命中；L4 无跨域折扣测试。
- **§4 三判据**：不适用（volume-discount.md §折扣应用活跃要求，无 Deferred 标注）。
- **触及保护区域**：修复须与 purchase/sales 订单行联动（跨域契约 ask-first）+ contract 侧 API 已就绪无须改；折扣应用逻辑属代码逻辑预授权。

#### UC-CT-09 = **接受**
- **命中判据**：§2"接受"——UC-CT-09-A/B/C/D 四验收标准全部实现（init+Provider + 逐个签署状态迁移 + webhook+SIGNED+certificateUrl + 超时/拒签/失败），TestErpCtESignature 强断言覆盖。
- **三源对照**：L1 use-cases.md:165-167 与 L3 完全对齐；L4 TestErpCtESignature 强测。
- **真实 provider successor**：MockSignatureProvider 是 SPI 测试桩（config-gated，覆盖全链行为），真实三方 HTTP 集成（e签宝/DocuSign/Tsign）归 e-signature.md 文档化 successor（非 P1——核心签章生命周期已实现，provider 切换是部署集成细节，SPI 契约 IErpCtSignatureProvider 已就绪）。

#### UC-CT-10 = **P1**（new P1-RC-079）
- **命中判据**：§2 ①（UC-CT-10-A OCR 引擎 + fullTextSearch 构建 + UC-CT-10-B 全文搜索 + UC-CT-10-C 保留策略归档/销毁 + UC-CT-10-D Legal Hold 完全缺失）；实体字段结构载体 ✅（ocrText/ocrStatus/fullTextSearch/metadataTags/retentionDate/archiveDate/purgeDate/isArchived 均存在）。
- **三源对照**：L1 use-cases.md:177-179 明确 OCR + fullText + 保留策略 + Legal Hold；L3 `ErpCtDocumentBizModel` 17 行 CRUD 桩 + grep ocr/fullText/retention/legalHold 零业务命中 + **legalHold 字段不存在**（`_ErpCtDocument` 无此属性）；L4 零 dedicated 测试。
- **§4 三判据**：不适用（contract-repository.md §OCR/§全文检索/§保留策略/§Legal Hold 活跃要求，无 Deferred 标注）。
- **触及保护区域**：**Legal Hold 须为 ErpCtDocument 加 legalHold 字段（ORM 结构变更须 ask-first + 独立 plan-audit §5 ORM 类）**；OCR 引擎 + fullTextSearch 构建 + 全文搜索属代码逻辑预授权；**保留策略 purgeDate→delete 触及数据删除（§5 数据删除类须 ask-first + 独立 plan-audit）**。

### P0 即时通道评估

**本切片无 P0**。最高级 = P1（8 新 P1 + 1 reuse）。理由：
- 创建校验/行复制/审批工作流/消耗计费/OCR/折扣订单侧缺失均属功能缺失类（无活跃数据破坏 / GL 不平衡 / 安全漏洞 / 核心循环断裂）。
- 到期自动化缺失属 missing-automation（手工 expire 路径存在 + InvoicePlan 生成 unposted DRAFT 经人工审批兜底 + 不破坏业财一致——state-machine.md §4 残留风险注记确认）。
- 终止门控/截停缺失属流程编排缺失（terminate 状态翻转正确 + InvoicePlan 无独立状态列经合同头隐式失效——triggerInvoice ACTIVE 守卫拒绝 TERMINATED 合同）。
- 返利结算信用单（负额发票）经 O-4 豁免 documented（posting-exemptions.md），不破坏 GL 平衡。
- UC-CT-10 purgeDate→delete 触及数据删除但**逻辑未实现**（无活跃删除路径），属功能缺失非活跃数据破坏。

---

## 6. 与 arm-index 衔接（finding 复用/新增裁决，§7 规则）

> grep arm-index contract/lifecycle/amend/rebate/invoicePlan/signature/approval/document/OCR/expiry/renewal/consumption/discount 同域同控制点后裁决。既有 contract finding：`P1-MA1-029`（InvoicePlan 跨域写 O-4 豁免 resolved）、`P1-MA2-071`（EXPIRED Job+续期 resolved R1.22 deferral）、`P1-MA2-072`（NEGOTIATION→TERMINATED resolved R1.22 implementation）、`P1-MA3-006`（MOCK provider 入正式 dict，doc 维度 fixed R2.1）。**无 UC-CT-01/02/03/04/06/07/08/10 需求符合性 finding**。

### 新建 finding（8 新 P1，续编 P1-RC-072~079）

| Finding ID | UC | 根因 | 与既有 finding 差异 | 触及保护区域 |
|-----------|-----|------|-------------------|-------------|
| **P1-RC-072** | UC-CT-01 | 创建校验（totalAmount=∑行 + startDate<endDate）+ NEGOTIATION 提交创建 v1 版本 + 金额超预算特批完全缺失 | 新根因（创建编排维度），无既有同控制点 | 纯 BizModel 代码逻辑预授权不触 §5 ask-first |
| **P1-RC-073** | UC-CT-02 | amend 复制原合同行缺失 + 变更单驳回→原合同恢复 ACTIVE 缺失 | 新根因（变更行复制 + 驳回恢复维度），非 P1-MA2-072（NEGOTIATION→TERMINATED 迁移，不同控制点） | 纯 BizModel/Processor 代码逻辑预授权不触 §5 ask-first |
| **P1-RC-074** | UC-CT-03 | 按 invoiceTerm 批量生成 InvoicePlan 缺失 + 已开票 InvoicePlan 禁改金额缺失 | 新根因（计划生成 + 已开票锁维度），非 P1-MA1-029（跨域写 O-4 豁免，平台治理维度） | 纯 BizModel 代码逻辑预授权不触 §5 ask-first |
| **P1-RC-075** | UC-CT-04 | 消耗计费周期汇总 + 超量 InvoicePlan + 超 120% 审批通知完全缺失 | 新根因（消耗计费引擎维度），无既有同控制点 | 纯 BizModel 代码逻辑预授权不触 §5 ask-first |
| **P1-RC-076** | UC-CT-06 | terminate 法务审批门控 + 截停 InvoicePlan + 善后 TODO + 版本归档完全缺失 | 新根因（终止编排维度），非 P1-MA2-072（NEGOTIATION→TERMINATED 迁移已实现） | 纯 BizModel 代码逻辑预授权不触 §5 ask-first |
| **P1-RC-077** | UC-CT-07 | 审批工作流引擎（按 totalAmount 匹配 + Record 生成 + 逐节点 + 驳回超限 3 次 + 超时 72h 升级）完全缺失 | 新根因（审批工作流维度），无既有同控制点 | 纯 BizModel 代码逻辑预授权不触 §5 ask-first |
| **P1-RC-078** | UC-CT-08 | 批量折扣订单侧应用缺失（purchase/sales 零 resolveDiscount 调用） | 新根因（折扣跨域消费维度），非返利引擎（已实现） | 跨域契约须与 purchase/sales 订单行协调（ask-first）+ contract 侧 API 已就绪 |
| **P1-RC-079** | UC-CT-10 | OCR 引擎 + fullTextSearch 构建 + 全文搜索 + 保留策略归档/销毁 + Legal Hold 完全缺失 + legalHold 字段不存在 | 新根因（文档仓库引擎维度），无既有同控制点 | **ORM 结构[ErpCtDocument 加 legalHold 字段]须 ask-first + 独立 plan-audit §5 ORM 类** + OCR/全文引擎属代码逻辑预授权 + **保留策略 purgeDate→delete 触及数据删除 §5 数据删除类须 ask-first + 独立 plan-audit** |

### 复用 finding（1 reuse）

| 复用既有 ID | UC | 投影 | 裁决 |
|-----------|-----|------|------|
| **P1-MA2-071** | UC-CT-05 A/B/C/E | nop-job 扫描 + 30/15/7 分级通知 + 续期草稿 + 未完成开票先完成缺失 | resolved R1.22 via **deferral**（owner doc state-machine.md §2/§4/§7 Deferred 注记，非 implementation）。从 Q4=(a) 需求契约视角 L1 use-cases.md:96-107 仍活跃，§4 三判据复核 deferral 为 AI 自标无人工批准痕迹不成立。追加 RC A1.45 交叉引用注记**不新建**（同根因同控制点 contract 到期自动化维度）；MR1（R1.0→RC-R1.n）须实现 ErpCtContractExpiryJob + 续期 + 分级通知 successor。本复用不重开 P1-MA2-072（已 resolved-via-implementation） |

### 双向可追溯

- 8 新 P1-RC-072~079 入 arm-index RC 发现追踪分区（§arm-index 更新）。
- MR1 修复行须含 finding ID 交叉引用：P1-RC-072（创建校验+submit/v1+预算）/ P1-RC-073（amend 行复制+驳回恢复）/ P1-RC-074（invoiceTerm 生成+已开票锁）/ P1-RC-075（消耗计费引擎）/ P1-RC-076（终止编排）/ P1-RC-077（审批工作流引擎）/ P1-RC-078（折扣订单侧 wiring）/ P1-RC-079（文档仓库引擎+legalHold ORM）。
- **ORM 结构类修复（P1-RC-079 ErpCtDocument 加 legalHold 字段）须 ask-first + 独立 plan-audit**。
- **数据删除类修复（P1-RC-079 purgeDate→delete 保留策略）须 ask-first + 独立 plan-audit**。
- **跨域协调**：UC-CT-08 折扣订单侧须与 purchase/sales 订单行协调（contract API 已就绪，须消费方 wiring）。
- **UC-CT-05 到期自动化随 P1-MA2-071/R1.22 deferral successor 方向**（MR1 实现而非重开 MA2 行为裁决）。

---

## 7. 静态存疑点清单（供 MA4 展开）

> 登记 L5 无法静态定论、需运行时确认的点。每存疑点一行。

- **SP-1**（UC-CT-01）：运行时合同创建是否经前端/XMeta 层隐式校验 totalAmount=∑行金额（静态 grep BizModel 零校验，XMeta mandatory 单字段非跨字段——倾向确认无跨字段校验），运行时确认 save 是否拒绝金额不一致合同。
- **SP-2**（UC-CT-01）：运行时合同到 NEGOTIATION 是否经其他隐式机制（如前端编排）创建 v1 版本（静态无 submit mutation + 测试手工 seed——倾向确认无自动 v1），运行时确认 GraphQL save 后是否自动生成版本。
- **SP-3**（UC-CT-02）：运行时 amend 后变更单是否经前端编排半自动复制原合同行（静态 amend Processor 零行复制——倾向确认无），运行时确认前端是否补全行复制。
- **SP-4**（UC-CT-05）：运行时是否有其他全局 job（非 contract 专属）扫描合同到期（静态 grep 跨 module-contract 零 Job——倾向确认无），运行时确认 scheduler.yaml 是否注册 contract 到期 job。
- **SP-5**（UC-CT-06）：运行时 terminate 后 InvoicePlan 是否经合同头 TERMINATED 状态隐式失效（triggerInvoice ACTIVE 守卫拒绝——倾向确认隐式失效但无显式截停标记），运行时确认未开票 InvoicePlan 在 TERMINATED 合同上的可达性。
- **SP-6**（UC-CT-07）：运行时是否有其他全局审批引擎（非 contract 专属 ApprovalMatrix 匹配）驱动合同审批（静态 ApprovalMatrix/Record 仅 CRUD 桩——倾向确认无工作流引擎），运行时确认合同提交是否触发任何审批流。
- **SP-7**（UC-CT-08）：运行时 purchase/sales 订单行是否经其他隐式机制（如前端选合同行时调用 resolveDiscount）应用折扣（静态 grep 零调用——倾向确认无自动应用），运行时确认订单引用合同行时是否应用折扣。
- **SP-8**（UC-CT-10）：运行时是否有外部 OCR 服务/异步任务处理 ErpCtDocument（静态 grep 零 OCR 业务逻辑——倾向确认无），运行时确认文档上传后 ocrText/fullTextSearch 是否被任何机制填充。

**P0 即时通道评估结论**：本切片无活跃数据破坏候选——创建校验/行复制/审批/消耗/OCR/折扣缺失属功能缺失类；到期自动化缺失属 missing-automation（手工路径存在 + unposted DRAFT 兜底）；终止门控/截停缺失属流程编排缺失（状态翻转正确 + InvoicePlan 隐式失效）；返利信用单经 O-4 豁免 documented；UC-CT-10 purgeDate→delete 逻辑未实现无活跃删除路径。**无 P0**。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，退出码 0（纯 reporter，退出码恒 0）。本报告**无生产代码变更**（纯审计报告 + arm-index 文档更新），checker 无回归风险。**不以 checker 脚本退出码 0 作为门控通过依据**——区分门控退出码 vs 纯 reporter 退出码（checker 是纯 reporter，真正门控在 CI workflow `.github/workflows/compliance.yml` 解析 actual > baseline => sys.exit(1)）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index contract/lifecycle/amend/rebate/invoicePlan/signature/approval/document/OCR/expiry/renewal/consumption/discount 同域同控制点后给出"复用 or 新增"裁决（8 新建 P1-RC-072~079 + 1 reuse P1-MA2-071），无未经比对直接新建的 finding。

---

## 9. 与 MA2 报告差异增量声明

> 对齐 methodology §去重协议。本切片为 contract 域全域需求契约符合性首份证据。

- **无 contract 专属 MA2 状态机/业财链路行为审计报告**。A2.14（`2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`）覆盖 contract 合同/InvoicePlan 状态机迁移 + EquipmentStatusLinker（状态机行为维度），但**不含创建校验/审批工作流/计费/返利/仓库/到期自动化需求契约维度**。本切片为 contract 域全域需求契约符合性首份证据。
- **P1-MA2-071**（EXPIRED Job+续期缺失，resolved R1.22 via **deferral**）引作 UC-CT-05 到期自动化同型现状——**reuse**（追加 RC A1.45 交叉引用注记不新建），声明 R1.22 resolution 为 deferral 非 implementation，从 Q4=(a) 视角 L1 仍活跃，MR1 须实现 successor。
- **P1-MA2-072**（NEGOTIATION→TERMINATED 缺失，resolved R1.22 via **implementation**）HEAD 复核确认——`terminate:102-106` 守卫已扩展接受 NEGOTIATION，**resolved-via-implementation 确认**，本切片不重开。
- **P1-MA1-029**（InvoicePlan 跨域写 O-4 豁免，resolved）非本切片维度（平台治理维度不重审）；UC-CT-03 触发到 AP/AR 草稿经此豁免 documented，跨域生成行为已证实。
- **P1-MA3-006**（MOCK provider 入正式 dict，doc 维度 fixed R2.1）非本切片维度（文档基线维度）；UC-CT-09 签章生命周期行为已实现，Mock provider 为 SPI 测试桩非本切片功能缺口。
- **本切片只补需求视角差异**（use-case 验收标准视角）：
  - UC-CT-01 创建校验/v1/预算特批缺失（P1-RC-072，新根因）。
  - UC-CT-02 amend 行复制/驳回恢复缺失（P1-RC-073，新根因）。
  - UC-CT-03 invoiceTerm 生成/已开票锁缺失（P1-RC-074，新根因）。
  - UC-CT-04 消耗计费引擎完全缺失（P1-RC-075，新根因）。
  - UC-CT-05 到期自动化/分级通知/续期缺失（reuse P1-MA2-071，§4 三判据裁决 deferral 不成立）。
  - UC-CT-06 终止编排（法务门控/截停/TODO/归档）缺失（P1-RC-076，新根因）。
  - UC-CT-07 审批工作流引擎完全缺失（P1-RC-077，新根因）。
  - UC-CT-08 折扣订单侧应用缺失（P1-RC-078，新根因跨域）。
  - UC-CT-09 签章生命周期接受（核心实现完整，真实 provider 归 successor）。
  - UC-CT-10 文档仓库引擎 + Legal Hold ORM 完全缺失（P1-RC-079，新根因 + ORM ask-first）。
- **真相源冻结声明**：本审计**未修改**任何真相源（use-cases.md / state-machine.md / volume-discount.md / contract-repository.md / e-signature.md / approval-workflow.md / product-scope.md），分歧记入本报告（§5/§6）。

---

> 本审计解除 A1.45+A1.46 在 MA4（A4.1/A4.2 展开器）及 MR1（R1.0）链路的 contract 域全域证据缺口。**contract 域 MA1 全覆盖（A1.45+A1.46 合并切片 done，contract 域 10 UC 全覆盖收尾）**。
