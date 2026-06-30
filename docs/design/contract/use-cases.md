# 合同全生命周期管理域 — 典型用例

## UC-CT-01 合同创建与签署（Contract Creation & Signing）

**触发条件** 采购员/销售员需要创建新合同。

**前置条件** 合作方已在 ERP 中注册（ErpMdPartner），合同模板已配置。

**流程** 
1. 经办人选择合同模板（ErpCtTemplate）→ 自动填充合同类型、部分条款。
2. 填写合同头 contractName、partnerId、totalAmount、startDate/endDate。
3. 录入合同行 物料/产品、数量、单价、金额。
4. 系统校验 总金额 = ∑行金额；startDate < endDate。
5. 合同状态 → DRAFT。
6. 提交审批 → NEGOTIATION（创建 v1 版本，isCurrent=true）。
7. 双方确认条款，签署合同文件上传（attachmentId）。
8. 合同管理员确认签署 → ACTIVE（signDate=now）。

**后置条件** 合同进入 ACTIVE 状态，可创建采购/销售订单引用。

**异常** 金额超预算 → 拦截提交，触发特批流程。

---

## UC-CT-02 合同变更与版本管理（Contract Amendment）

**触发条件** 执行中的合同需要修改条款或价格。

**前置条件** 合同处于 ACTIVE 状态。

**流程** 
1. 经办人基于原合同创建变更单（新 DRAFT 合同，parentContractId = 原合同）。
2. 系统复制原合同所有行到变更单，允许增删改。
3. 变更单走标准流程 DRAFT → NEGOTIATION → 签署后 ACTIVE。
4. 变更单生效时，系统 
   - 原合同当前版本 isCurrent=false。
   - 创建新版本（versionNo 递增），isCurrent=true。
   - 新版本内容记录变更说明。
5. 后续订单/开票引用新版本条款。

**后置条件** 旧版本保留可追溯，新版本生效。

**异常** 变更单被驳回 → 原合同保持 ACTIVE 不变。

---

## UC-CT-03 开票计划生成与执行（Invoice Plan Generation）

**触发条件** 合同签署生效（→ACTIVE）时自动生成。

**前置条件** 合同行已配置 invoiceTerm（ADVANCE/MILESTONE/MONTHLY/COMPLETION）。

**流程** 
1. 系统按合同行的 invoiceTerm 批量生成 InvoicePlan 
   - 预付款（ADVANCE） 签署后 N 天生成。
   - 里程碑（MILESTONE） 按合同约定的里程碑日期生成。
   - 月结（MONTHLY） 每月固定日期生成。
   - 完工（COMPLETION） endDate 生成。
2. InvoicePlan 记录 planDate、amount、invoiceTerm、isInvoiced=false。
3. 到达 planDate 时，系统自动生成 AP/AR Invoice 草稿（调用 finance 域 API）。
4. 财务人员审核发票 → isInvoiced=true、invoiceDate=now、invoiceBillCode 回写。

**后置条件** 已开票的 InvoicePlan 不允许修改金额。

**异常** 合同处于 SUSPENDED 状态 → 拦截 InvoicePlan 执行，标记为待恢复。

---

## UC-CT-04 消耗计费与用量结算（Consumption Billing）

**触发条件** 服务型合同（SaaS/运维）按实际用量计费。

**前置条件** 合同行允许消耗计费（quantity 为预估总量）。

**流程** 
1. 系统/人工按周期（日/周/月）记录 ErpCtConsumptionLine 
   - consumptionDate、quantity、unitPrice、amount。
   - sourceBillType/sourceBillCode 记录来源（如 API 调用日志）。
2. 周期结束时，汇总 ConsumptionLine 总量与合同行预估总量对比。
3. 超量部分生成额外计费 InvoicePlan。
4. 系统生成 AP/AR Invoice 草稿给财务。

**后置条件** 消耗计费记录作为财务凭证附件。

**异常** 消耗量超过合同行预估总量 120% → 触发超量审批通知。

---

## UC-CT-05 合同到期提醒与续期（Expiry Reminder & Renewal）

**触发条件** nop-job 每日扫描合同到期日期。

**前置条件** 合同处于 ACTIVE 状态且 endDate 在可预见的未来。

**流程** 
1. nop-job 扫描 erp_ct_contract，条件 `status=ACTIVE AND endDate BETWEEN now() AND now()+30d`。
2. 到期前 30 天 → 发送邮件/站内通知给经办人。
3. 到期前 15 天 → 再次通知，标记为"即将到期"。
4. 到期前 7 天 → 升级通知经办人上级。
5. 经办人决策 
   - **续期** 按配置 auto-create-renewal-draft → 自动创建续期草稿（parentContractId 关联原合同），走 DRAFT 流程。
   - **不续期** endDate 到达时自动 EXPIRED。
6. endDate 到达 → 系统将合同状态设为 EXPIRED。

**后置条件** 到期提醒记录在日志中，续期草稿创建后可编辑。

**异常** endDate 到达仍有未完成的开票计划 → 先完成开票再 EXPIRED。

---

## UC-CT-06 合同提前终止（Contract Termination）

**触发条件** 一方违约或双方协商决定提前终止合同。

**前置条件** 合同处于 ACTIVE 或 SUSPENDED 状态。

**流程** 
1. 合同管理员发起终止申请，填写 
   - 终止原因（违约/协商/其他）。
   - 上传终止协议附件。
2. 提交法务审批。
3. 法务审批通过后，系统执行终止操作 
   - 合同状态 → TERMINATED。
   - 当前版本 isCurrent=false（归档）。
   - 截停所有未执行 InvoicePlan。
   - 生成 TODO 善后结算处理。
4. 财务根据已消耗/已收货数量生成最终结算发票。
5. 经办人确认尾款结清，TODO 完成。

**后置条件** 合同归档，不可再修改；所有未执行 InvoicePlan 标记为作废。

**异常** 法务驳回 → 合同保持原状态（ACTIVE/SUSPENDED）。

## UC-CT-07 合同审批工作流

| 项目 | 内容 |
|------|------|
| **概述** | 合同提交后按金额阈值路由到不同审批层级（部门经理→财务→法务→高管），支持审批链配置、驳回答复循环 |
| **触发条件** | 经办人提交合同（DRAFT→NEGOTIATION） |
| **前置条件** | ErpCtApprovalMatrix 已配置金额阈值与审批角色映射 |
| **基本流程** | 1. 经办人提交合同<br>2. 系统读取 ErpCtApprovalMatrix，按 totalAmount 匹配适用的审批节点列表<br>3. 生成 ErpCtApprovalRecord（每节点一条），第一个节点激活（PENDING），其余 WAITING<br>4. 审批人逐节点审批，通过后激活下一节点<br>5. 所有节点通过后合同可进入 ACTIVE 状态<br>6. 驳回时经办人修改后可重新提交（仅重新激活驳回节点及其后续节点） |
| **后置条件** | 全部审批节点通过后合同可转为 ACTIVE |
| **异常** | 驳回超限（默认 3 次）后锁定需强制升级；超时未处理（默认 72h）升级通知上一级 |
| **跨域协作** | master-data（Role/User 确定审批人）；notification（审批待办通知） |

## UC-CT-08 批量折扣与返利

| 项目 | 内容 |
|------|------|
| **概述** | 合同行按数量区间映射折扣率，以及年度累计阶梯返利协议，到期生成信用单结算 |
| **触发条件** | 采购/销售订单引用合同行时自动应用折扣；返利协议到期或手动触发结算 |
| **前置条件** | ErpCtVolumeDiscount 已配置（行级折扣）；ErpCtRebateAgreement 已配置（返利协议） |
| **基本流程** | 批量折扣：1. 合同行配置数量→折扣率映射<br>2. 订单引用合同时按实际数量匹配折扣率，计算折后价<br><br>返利：1. 签订年度返利协议（ErpCtRebateAgreement），配置阶梯（ErpCtRebateTier）<br>2. 发票过账时记录返利计提（ErpCtRebateAccrual）<br>3. 累计金额跨越层级时自动追溯调整补差<br>4. 协议到期触发结算（ErpCtRebateSettlement），生成 AP/AR 信用单 |
| **后置条件** | 折扣已应用到订单行；返利已计算并生成信用单 |
| **异常** | 退货冲销导致层级回落时冲销多计提返利 |
| **跨域协作** | purchase/sales（订单引用折扣）；finance（返利结算生成 AP/AR Credit Memo） |

## UC-CT-09 电子签章

| 项目 | 内容 |
|------|------|
| **概述** | 合同版本定稿后通过电子签章提供商（e签宝/DocuSign/Tsign）发送签署请求，跟踪签署状态，签署完成后存储证书证据 |
| **触发条件** | 合同版本进入 FINALIZED 状态，经办人发起电子签署 |
| **前置条件** | 合同版本已定稿；合同已配置电子签章提供商 |
| **基本流程** | 1. 经办人选择合同版本，发起签署请求<br>2. 系统创建 ErpCtSignatureRequest（status=PENDING_SIGNATURE），指定签署人列表<br>3. 调用 Provider.initSignature() 上传承签文档，配置签署顺序<br>4. 签署人收到签署链接（邮件/短信/小程序）<br>5. 签署人在线签署：签署人 1 完成 → status=PARTIALLY_SIGNED<br>6. 全部签署人完成 → status=FULLY_SIGNED<br>7. 系统接收 webhook 回调，下载已签署文件<br>8. 更新 ErpCtContractVersion.status=SIGNED，记录 certificateUrl<br>9. 签署人拒签 → status=REJECTED，经办人修改后可重新发起 |
| **后置条件** | 已签署的合同版本进入 SIGNED 状态；签署证据已归档 |
| **异常** | 签署超时（signingDeadline 到达）→ status=EXPIRED；提供商调用失败 → status=PENDING_SIGNATURE，记录 errorMsg 后重试 |
| **跨域协作** | master-data（合作伙伴联系人信息）；notification（发送签署通知） |

## UC-CT-10 合同仓库与全文检索

| 项目 | 内容 |
|------|------|
| **概述** | 合同文档集中存储管理，支持 OCR 识别、全文检索、元数据标签和高级搜索。按保留策略自动归档和销毁 |
| **触发条件** | 经办人上传合同文档 / OCR 处理完成 / 搜索查询触发 |
| **前置条件** | 文档存储已配置；OCR 引擎可用（可选） |
| **基本流程** | 1. 经办人上传合同文档（扫描件/电子PDF/图片/OFD）<br>2. 系统创建 ErpCtDocument，识别文档类型和物元数据<br>3. OCR 引擎自动识别文本（扫描件/图片）/ 提取文字（电子PDF）<br>4. ocrText 写入，fullTextSearch 构建（含 docName + ocrText + metadataTags）<br>5. 用户设置元数据标签（party、region、自定义字段）<br>6. 用户通过全文搜索或高级过滤器（文档类型、日期范围、金额范围、标签）检索文档<br>7. 保留策略按 retentionDate 自动归档（isArchived=true），purgeDate 到达时系统删除<br>8. Legal Hold 阻止归档/销毁操作 |
| **后置条件** | 文档已索引可检索；归档/销毁按策略执行 |
| **异常** | OCR 识别失败时手动设置或跳过；legalHold=true 时禁止所有归档/销毁 |
| **跨域协作** | master-data（固定分类和标签配置） |
