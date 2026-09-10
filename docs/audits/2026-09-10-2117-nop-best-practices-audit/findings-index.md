# findings-index — 全量发现索引（146 项）

> 详细描述、证据与修复方向见对应分文件（`01`~`06`）。缩写：PD=Processor 覆盖（`02`）、EM=实体方法（`03`）、CD=跨域/DAO（`04`）、PC=平台能力（`05`）、NP=命名/过程式（`06`）、BL=基线漂移（`01`）。

## Blocker（2）

| ID | 域 | 位置 | 问题 | 分文件 |
|----|----|------|------|--------|
| B-1 | notify | `NotificationDispatcher.java:177-195` | 邮件/短信消息从不设置收件人地址，EMAIL/SMS 通道不可达 | PC §2.1 |
| B-2 | projects→assets | `ErpPrjProjectSettlementProcessor.java:223-232` | 结算回退 daoFor 直写资产状态 + 硬编码 ASSET_STATUS_DRAFT，绕过资产域生命周期守卫 | CD §1.1 |

## Major（66）

### 基线漂移与硬规则（4，详见 `01`）

| ID | 规则 | 位置 | 问题 |
|----|------|------|------|
| M-BL-1 | R1b | `ErpInvSerialNumberBizModel.java:56` | dao().updateEntity 裸调（源=48b57cd06/M2.5） |
| M-BL-2 | R1d | `ErpInvSerialNumberBizModel.java:65` | dao().findAllByQuery 裸调（同源） |
| M-BL-3 | R2c | `PaymentSettler.java`（pur） | daoFor(ErpPurInvoice) 未裁决漂移（同源） |
| M-BL-4 | 时钟 | `ErpB2bOnboardingMonitorJob.java:220` | LocalDateTime.now()（ai-defaults 硬规则） |

### 跨域写 / daoFor / 裸 dao / 吞异常（12，详见 `04`）

| ID | 域 | 位置 | 问题 |
|----|----|------|------|
| M-CD-1 | inv→mfg | `CostAdjustmentService.java:207-241` | daoFor 直写/删 ErpMfgCostRollup（IErpMfgCostRollupBiz 已存在未用） |
| M-CD-2 | drp→inv/pur | `DrpReleaseService.java:188-228` | 跨域直建调拨/采购单，无豁免登记 |
| M-CD-3 | ct→pur/sal | `ErpCtInvoicePlanTriggerInvoiceProcessor.java:75-150` | daoFor 直建四实体发票，无豁免登记 |
| M-CD-4 | mfg→inv | `BatchGenealogyWriter.java:198,210` | 直写 ErpInvBatch，无豁免登记 |
| M-CD-5 | pur | `ThreeWayMatcher.java:196-204` | daoFor(FK) 而 ORM to-one 已声明（×2 站点） |
| M-CD-6 | inv | `CostMethodResolver.java:57-64` | daoFor(ErpMdMaterial FK)，to-one 已声明 |
| M-CD-7 | inv | `CostAdjustmentService.java:297` | 同文件 :208 已用 getter，:297 daoFor 不一致 |
| M-CD-8 | qa | `SpcSamplingService.java:157/247` | 循环 daoFor(inspection FK)，to-one 已声明（兼 N+1） |
| M-CD-9 | hr | `ErpHrShiftBizModel.java:141-157` | 类体内 dao.saveEntity/saveOrUpdateEntity/updateEntity 裸写 |
| M-CD-10 | hr | `ErpHrSalarySimulationBizModel.java` ×5 站点 | 裸 daoFor(ErpHrSalary) 而 salaryBiz 已注入在用 |
| M-CD-11 | sal | `ErpSalOrderBizModel.java:176-203` | daoFor findAllByQuery + lineDao 循环裸写 |
| M-CD-12 | fin | `EmployeeAdvancePostingDispatcher.java:112-118` + `ErpFinEmployeeAdvanceBizModel.java:94-104` | 过账吞异常半状态悬挂（B1 复发变体） |

### Processor 覆盖缺口（12，详见 `02`）

| ID | 域 | 位置 | 方法（行数/步骤） |
|----|----|------|------------------|
| M-PD-1 | ct | `ErpCtContractBizModel.java:215-291` | terminate/approveTermination/rejectTermination（6 步） |
| M-PD-2 | ct | `ErpCtContractBizModel.java:335-448` | expireOverdue 批量族 ~115 行 |
| M-PD-3 | ct | `ErpCtDocumentBizModel.java:151-269` | purge/startOcr/archiveOverdue（模块 0 Processor） |
| M-PD-4 | ct | `ErpCtApprovalRecordBizModel.java:100-140` | resubmit 40 行 |
| M-PD-5 | hr | `ErpHrSurveyResponseBizModel.java:59-119` | submitResponse 61 行 7 步 |
| M-PD-6 | hr | `ErpHrSurveyResultBizModel.java:81-123` | aggregateResult 44 行 6 步 |
| M-PD-7 | log | `ErpLogDeliveryBookingBizModel.java:59-111` | book() 49 行 6 步 |
| M-PD-8 | sal | `ErpSalOrderBizModel.java:139` | applyPricingRules（编排+5 helper 滞留） |
| M-PD-9 | fin | `ErpFinEmployeeAdvanceBizModel.java:66-107` | cashRepay 42 行（待复核：有 plan 登记） |
| M-PD-10 | fin | 同文件 `:121-147` | reverseCashRepay 27 行 |
| M-PD-11 | cs | `ErpCsTicketBizModel.java:121-213` | enrichAfterCreate/autoAssignOnCreate 5 步 |
| M-PD-12 | cs | `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java`（873 行） | 反向 God Processor，continueChain ~290 行 |

### 实体方法 / 双源矩阵（7 组，详见 `03`）

| ID | 范围 | 摘要 |
|----|------|------|
| M-EM-1 | 全仓 | ~400 处内联状态比较（分域计数见 `03` §1），22/369 实体有谓词方法 |
| M-EM-2 | mfg/aps/log/drp | 4 域实体 100% 空壳 + 引擎 >5000 行（贫血失衡） |
| M-EM-3 | pur | ErpPurInvoiceProcessor 同类混用 stateMachine + 内联 |
| M-EM-4 | sal | 根 Processor 迁移矩阵整块内联而子类已走 Bean（漂移面） |
| M-EM-5 | ct | SignatureRequest 迁移矩阵双份实现（Processor vs BizModel） |
| M-EM-6 | aps/log/hr | 缺 StateMachine Bean：ApsSchedule / DeliveryBooking / hr 尾部 5 实体 |
| M-EM-7 | pur/sal | 13 实体已有 isApproved 三件套却被绕过（零成本修复项） |

### 平台能力替代（9，详见 `05`）

| ID | 域 | 位置 | 问题 |
|----|----|------|------|
| M-PC-1 | crm | `LeadScoringEngine.java:148-231` | 自创公式 DSL 417 行（nop-rule 候选 #1） |
| M-PC-2 | fin | `ErpFinGlMappingResolver.java:99-183` | GL 科目映射自研优先级链+自管缓存（nop-rule 候选 #2）；TransferPriceResolver 同型 |
| M-PC-3 | md | `ErpMdMaterialSkuBizModel.java:387-422` | 价档映射+最低价派生硬编码 |
| M-PC-4 | notify | `NotificationDispatcher.java:76-99` + `NotificationMergeCoordinator.java:56-76` | 同步派发 N+1 写放大（应迁 nop-message） |
| M-PC-5 | notify | `NotificationDispatcher.java:43,206-237` | 自建 ${var} 正则模板（应 XLang TemplateStringExpression） |
| M-PC-6 | cs | `ErpCsReportBizModel.java:249-251` | 无 limit 全量物化内存聚合（应 DB 下推） |
| M-PC-7 | cs | `ErpCsQualityDashboardBizModel.java:256-269` | 无界 loadClosedTickets 全历史内存 KPI |
| M-PC-8 | mfg/mnt/qa/pur/sal/inv | `05` §4 表 #1-#7 | N+1 查询 7 组 major 站点 |
| M-PC-9 | fin | `ErpFinReportBizModel.java`（741 行） | Java 聚合 God class + 硬编码现金科目前缀 1001/1002/1012/1031 与 "160" |

### 命名 / 过程式 / 死代码（12，详见 `06`）

| ID | 范围 | 摘要 |
|----|------|------|
| M-NP-1 | 全仓 | `*Service` 命名 11 + Impl 1 + 接口 1（逐类应改名见 `06` §1 表） |
| M-NP-2 | qa | SpcSamplingService 544 行双百行同构方法 |
| M-NP-3 | hr | PayrollCalculator.calculate 106 行 11 步 |
| M-NP-4 | hr | SalarySimulationBizModel ~150 行死代码（与 Processor 100% 重复，含裸 dao 写） |
| M-NP-5 | ct | ErpCtInvoicePlanBizModel 跨域写死代码副本 |
| M-NP-6 | qa | NcrLifecycleService 混两职责 |
| M-NP-7 | aps | AtpCtp Service+Impl Spring-ism |
| M-NP-8 | mfg/drp | MrpReleaseService/DrpReleaseService Processor 主体却 Service 命名（Drp 兼跨域写） |
| M-NP-9 | fin | 结账 4 *Service 重复 helper（resolveAcctSchemaId×3/findSubjectByCode×3/resolveFunctionalCurrencyId×4） |
| M-NP-10 | sal | recomputeOrderTotals BizModel/Processor 双份维护 |
| M-NP-11 | cs | AssignResolver 逐行重复（Processor vs BizModel） |
| M-NP-12 | fin | CloseVoucherWriter 绕过过账引擎 + 常量/ErrorCode 重复（待复核） |

## Minor（78）

按分文件分布：`01`（checker 盲区建议 4 项，计入演进建议不占 finding 位）；`02` §2 边缘情形 7 项；`03` §2 各域上提候选 minor 部分 + §3 缺 Bean minor；`04` §4.5-4.9 裸 dao minor 5 项 + §5.2-5.3 吞异常收窄 2 项；`05` §1 规则化候选 minor 9 项 + §3 报表/看板 minor 5 项 + §4 N+1 minor 3 项 + §2.4-2.5 notify minor 2 项；`06` §2.5-2.7 超长方法 3 项 + §3.3 死注释 1 项 + §4 重复 helper 6 项。逐条 file:line 见各分文件。

## 修复批次建议（供 successor roadmap 参考）

1. **P0 批**：B-1 notify 收件人、B-2 资产回退 Facade 化、M-BL-1~3 漂移裁决（Fix 优先）、M-BL-4 时钟
2. **P1 跨域写批**：M-CD-1~4 补登记或 I*Biz 化（单计划可含 4 站点 + M-NP-5 死代码删除）
3. **P1 Processor 批**：M-PD-3（ct 文档模块）→ M-PD-1/2（ct 合同族）→ M-PD-5/6（hr survey）→ M-PD-7/8/9/10/11 → M-PD-12 拆分
4. **P1 N+1 批**：M-PC-8 七组站点（to-one 替换 3 处与 CD-5~8 联动）
5. **P2 结构批**：实体方法上提（M-EM-1 按域分计划，L-6 follow-up 立项证据已足）+ 双源矩阵收敛（M-EM-3~6）+ nop-rule 迁移（M-PC-1/2 试点）
6. **P2 清理批**：命名 13 项（注意 batch.xml/posting-exemptions.md 联动改名）+ 死代码 2 项 + 重复 helper 6 项
