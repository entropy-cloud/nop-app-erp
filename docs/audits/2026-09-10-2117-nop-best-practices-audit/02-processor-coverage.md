# 02 — per-mutation Processor 覆盖缺口

> 规则依据：`docs/architecture/processor-extension-pattern.md`（每多步骤 @BizMutation 对应独立 `<Entity><Method>Processor`；例外 = ≤2 步查询 / 单步状态翻转 / 标准 CRUD）+ 豁免登记册 `docs/architecture/processor-per-mutation-exemption-registry.md`
> 全仓基线：397 个 BizModel / 496 个 Processor / 494 个 @BizMutation——大面已薄 Facade 化，以下为**多步编排滞留 BizModel** 的缺口清单（12 处 major + 边缘 minor），另有 1 个反向 God Processor。

## 1. [major] 多步编排滞留 BizModel（应拆 Processor）

### contract（缺口最重：ErpCtDocumentBizModel 零 Processor）

- `module-contract/erp-ct-service/.../entity/ErpCtContractBizModel.java:215-291` — `terminate`/`approveTermination`/`rejectTermination` 三 mutation 未委托 Processor；approveTermination 单方法 6 步（TERMINATED 置态→版本归档→InvoicePlan 截停→winddown 通知→审批记录→update）→ 拆 `ErpCtContractApproveTerminationProcessor` / `TerminateProcessor`（amend/activate 已有先例）
- `ErpCtContractBizModel.java:335-448` — `expireOverdueContracts` + `triggerDueInvoicesBeforeExpire` + `createRenewalDraftIfEnabled` + `renewalDraftCode` 约 115 行批量编排内联；代码注释自引 hr 域 `ErpHrEmploymentContractExpireOverdueContractsProcessor` 范式却未提取 → `ErpCtContractExpireOverdueProcessor`
- `module-contract/erp-ct-service/.../entity/ErpCtDocumentBizModel.java:151-269` — 文档模块 0 Processor：`purge`（5 守卫+审计+通知+删除）、`startOcr`（外部引擎调用+状态机）、`archiveOverdue`/`purgeOverdue` 批量循环全部内联 → `ErpCtDocumentPurgeProcessor` / `StartOcrProcessor`（OCR 是外部副作用，尤其应隔离）
- `module-contract/erp-ct-service/.../entity/ErpCtApprovalRecordBizModel.java:100-140` — `resubmit` 40 行（守卫+latestRejected+矩阵匹配+循环重建链节点+通知），approve/reject 亦 4-5 步内联

### hr（Survey 族 + Simulation 残留）

- `module-hr/erp-hr-service/.../entity/ErpHrSurveyResponseBizModel.java:59-119` — `submitResponse` 61 行 7 步（require survey→状态守卫→匿名哈希→查重→题目校验→构建 response+answers→save→回写计数）→ `ErpHrSurveyResponseSubmitResponseProcessor`
- `module-hr/erp-hr-service/.../entity/ErpHrSurveyResultBizModel.java:81-123` — `aggregateResult` 44 行 ≥6 步（loadQuestions→loadResponses→loadAnswers→按部门分组→逐组 upsert→回写汇总）→ `ErpHrSurveyResultAggregateResultProcessor`
- （对照正面）`ErpHrSalarySimulationBizModel`（945 行）4 个重 mutation 已正确委托 Processor——长文件本身不等于违规

### logistics（4 域子代理扫描的唯一豁免册空白域）

- `module-logistics/erp-log-service/.../entity/ErpLogDeliveryBookingBizModel.java:59-111` — `book()` 49 行 6 步（状态白名单守卫→重复预约检查→窗口加载校验→容量校验→newEntity+save→窗口计数+1 跨实体写），豁免登记册 logistics 段为空 → `ErpLogDeliveryBookingBookProcessor`
- 同文件 `:146-166` `markMissed()`（状态翻转+爽约费+优先分加分 3 步）、`:113-128` `releaseForShipment()`（预约取消+窗口计数回减 2 写）——随 book 一并治理或补登记

### sales / finance

- `module-sales/erp-sal-service/.../entity/ErpSalOrderBizModel.java:139` — `applyPricingRules`（17 行入口但编排 ≥5 步：加载订单/行→跨实体客户组→活跃规则查询→引擎求值→`persistPricingResult` 落盘）；整套业务 helper（`persistPricingResult`:187-208、`recomputeLineAmount`:220-239、`recomputeOrderTotals`:241-257、`validatePromotionPrices`:274-299）滞留 BizModel → 拆 `ErpSalOrderApplyPricingRulesProcessor`；同方法逻辑与 `ErpSalOrderProcessor.recomputeOrderTotals(:202-212)` 近同构双份维护
- `module-finance/erp-fin-service/.../entity/ErpFinEmployeeAdvanceBizModel.java:66-107` — `cashRepay` 42 行多步（3 守卫+字段翻转+updateEntity+过账派发+失败 warn）内联 Facade；javadoc 引 plan 2026-07-18-0718-2 裁决「BizModel Facade 直落」，与 owner doc ≥3 步规则正面冲突（**待复核**：有 plan 登记但非豁免体系）→ `ErpFinEmployeeAdvanceCashRepayProcessor`
- 同文件 `:121-147` — `reverseCashRepay` 27 行（凭证链反查 :155-177 + 红冲 + 字段回退）未 Processor 化，且与 cashRepay「字段先于凭证」弱一致范式相反

### cs

- `module-cs/erp-cs-service/.../entity/ErpCsTicketBizModel.java:121-213` — `doSaveEntity` 后置富化 `enrichAfterCreate`（SLA 挂载+自动分配+通知 3 步）与 `autoAssignOnCreate`（团队解析→候选池→ROUND_ROBIN/LEAST_OPEN 挑人→置态→审计 5 步）内联 → `ErpCsTicketAutoAssignOnCreateProcessor`

## 2. [minor] 边缘情形（守卫已超单步翻转字面豁免）

- `ErpFinVoucherBizModel.java:92-108` `postVoucher`（期间锁守卫+状态机断言+借贷平衡校验+翻转+簿记 5 步，**待复核**）
- `ErpFinIntercompanyTransferBizModel.java:68-135,138-207` — `onTransferConfirmed`/`onTradeDocumentApproved` 各 ~70 行跨域回调入口（非 @BizMutation，前段解析/判定未 Step 化）
- `ErpHrSurveyBizModel.java:48-69` `publish` 22 行 3 步守卫；`ErpHrTimesheetBizModel.java:52-68` `submit` 保留 2 个动态副作用
- `ErpInvReservationBizModel.java:80/160/219/343` — 4 个 reservation mutation 有 plan 2026-08-15-2119-3 D4「不升 Processor」裁决，**但未回填豁免登记册 §A**（登记漂移，待复核：补登记或复议）
- `ErpInvSerialNumberBizModel.java:34` `markOutbound` 有当日 plan 裁决三要素但同样未入登记册 §A
- `ErpApsOperationOrderBizModel.java:122-141` `batchScheduleForward` 循环逐行委托既有 Processor（符合 R6.7 批量先例，仅披露）
- `ErpCtContractBizModel.java:119-159` `submit` 5 步已到阈值，terminate 族重构时一并评估

## 3. 反向问题：God Processor

- [major] `module-cs/erp-cs-service/.../processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java`（873 行）— `continueChain` 单方法约 290 行（链推进+审批+通知+重试全内联）；`:812-836` `resolveTypeDefaultPolicy`/`findLastAssigned`/`countOpenTickets` 与 `ErpCsTicketBizModel.java:181-213` 逐行重复 — 按步骤类型拆子 Processor + 抽公共 AssignResolver

## 4. 正面基线（不构成发现，修复时的范式参照）

- purchase/sales `batchApprove` 循环内逐行委托 per-mutation Processor（Phase 0 决策注记在案）
- assets/finance 149+ per-mutation Processor 全部薄委托；期间结账链（`ErpFinAccountingPeriodClosePeriodProcessor` + PreCheck/GenerateNextYear 伴生 Processor）为标准范式
- `ErpAstMaintenanceBizModel.java:65-131`、`ErpFinBadDebtBizModel` 为 Facade 薄委托范例
