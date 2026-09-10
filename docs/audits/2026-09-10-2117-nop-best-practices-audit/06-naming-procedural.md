# 06 — 命名违规 / God class / 死代码 / 重复 helper

> 规则依据：平台 `service-layer.md` + owner doc `processor-extension-pattern.md` 硬规则 4（不创建 `*Service`/`*Controller` 类；编排类用 `<Entity><Method>Processor`，复步用 `*Step`，纯计算用具体角色名）+ `domain-logic-and-ddd.md`（勿因「架构好看」新增无稳定职责的类）

## 1. `*Service` 命名违规（13 个 + 1 个 Impl + 1 个接口）

| # | 文件 | 行数 | 职责（实测） | 应改命名 |
|---|------|------|-------------|---------|
| 1 | `module-manufacturing/.../mrp/MrpReleaseService.java:58` | — | MRP 释放编排（requireReleasable→生成下游单→markFirmed→推进计划），被 3 个薄 Processor 委托，实为 Processor 主体 | 并入 `ErpMfgMrpPlanLineRelease*Processor` 或去 Service 后缀；**注意 posting-exemptions.md:10 按此类名登记豁免，改名须同步文档** |
| 2 | `module-drp/.../drp/DrpReleaseService.java:51` | — | DRP 行释放→生成调拨/采购单→状态推进，被 Processor 双层委托（层级冗余） | 合并入 Processor（同时治理 `04` §1.3 跨域写） |
| 3 | `module-finance/.../annualclose/AnnualCloseService.java:58` | 432 | 年度结转编排（利润结转凭证+次年余额 populate+对账门控） | `ErpFinAnnualCloseProcessor` |
| 4 | `module-finance/.../profitloss/ProfitLossClosingService.java:48` | 250 | 损益结转编排 | `ErpFinProfitLossClosingProcessor` |
| 5 | `module-finance/.../baddebt/BadDebtProvisionService.java:64` | 378 | 坏账计提/释放/反向红冲编排 | `ErpFinBadDebtProvisionProcessor` |
| 6 | `module-finance/.../fx/ExchangeRevaluationService.java:58` | 341 | 汇兑重估编排 | `ErpFinFxRevaluationProcessor` |
| 7 | `module-quality/.../entity/NcrLifecycleService.java:36` | — | 混两职责：autoCreateNcrFromInspection（多步写编排）+ requireResolveGate（只读门控） | 门控改 `NcrResolveGateChecker`；autoCreate 拆 `ErpQaNonConformanceAutoCreateNcrProcessor` |
| 8 | `module-quality/.../spc/SpcSamplingService.java:53` | 544 | SPC 计量/计数采样引擎（有状态编排+写） | `SpcSampleCollector` 或收敛 `ErpQaSpcChartCollectSamplesProcessor`；**注意 qa/spc-sampling.batch.xml processor 段按类名引用，改名须同步** |
| 9 | `module-maintenance/.../support/SparePartIssueService.java:23` | — | 构造 StockMoveRequest 调 IErpInvStockMoveBiz（跨域出库请求构造器） | `SparePartIssueRequestBuilder` |
| 10 | `module-inventory/.../costing/CostAdjustmentService.java:52` | — | 成本调整引擎（跨 inv/mfg/md 读写——见 `04` §1.2） | `CostAdjustmentEngine` 或 `CostAdjustmentBookkeeper`（对齐 StockMoveBookkeeper 先例） |
| 11 | `module-manufacturing/.../costing/CostRollupService.java:71` | 387 | 卷算 helper（javadoc 自认「服务助手」） | `CostRollupCalculator`（对齐 ProductionVarianceCalculator 先例） |
| 12 | `module-aps/.../atpctp/ErpApsAtpCtpServiceImpl.java:46` + `module-aps/erp-aps-dao/.../biz/IErpApsAtpCtpService.java:19` | — | `@BizModel("ErpApsAtpCtpService")` + Impl/接口拆分 = Spring-ism，违反 Nop BizModel 直类约定（全仓其余 BizModel 无接口） | 改 `ErpApsAtpCtpBizModel` 去接口（daoProvider 只读豁免 javadoc :37-42 独立成立） |

crm/cs/b2b/ct/md/notify/common/app-erp-all 范围内 0 个 `*Service`/`*Controller` 命名违规 ✅；无 `*Controller` 类 ✅。

## 2. God class / 超长方法

1. [major] `module-finance/.../report/ErpFinReportBizModel.java`（741 行）— 6 个 buildXxxDataset + ~20 个私有 helper + 科目分类/账龄规则全内联；finance 6 个 .xpt.xml 模板仅做展示（模板内无数据集声明），数据集 100% Java 构造 → 拆 per-report DatasetBuilder 或数据集声明下沉 xpt；**硬编码现金科目前缀 `1001/1002/1012/1031`（:639-643）与非流动资产前缀 "160"（:689）**——会计科目属性判定应走科目主数据字段/dict
2. [major] `module-cs/.../processor/ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.java`（873 行）— 见 `02` §3
3. [major] `module-quality/.../spc/SpcSamplingService.java:105-218`（collectSamples 114 行 7 段）+ `:230-334`（collectAttributesSamples 105 行，与前者同构重复）→ 拆计量/幂等/聚合三段共享骨架
4. [major] `module-hr/.../payroll/PayrollCalculator.java:54-159`（calculate 106 行 11 步薪资流水线）→ 拆 attendance/allowance/netting 三段
5. 超长方法清单（mfg/drp/aps）：`ProductionVarianceCalculator.java:109`（111 行）、`SimulationDrpEngine.java:65`（102 行）、`ErpApsSchedulingInsertRushOrderProcessor.java:38`（97 行）、`SimulationMrpEngine.java:99`（90 行）、`ErpMfgWorkOrderReportCompletionProcessor.java:31`（83 行）——多为算法密集引擎主流程，优先级低于上述 4 项
6. [minor] pur/sal Settler 双子（`ThreeWayMatcher.java:50` match 66 行、`PaymentSettler.java:67` settle 61 行、`ReceiptSettler.java:55` settle 58 行）——边界可接受，建议抽 per-allocation 步骤
7. [minor] `module-mnt/.../support/OeeCalculator.java:79-167`（computeOee 88 行三分量）→ 按 availability/performance/quality 拆段

## 3. 死代码（与 Processor 双份维护漂移）

1. [major] `module-hr/.../entity/ErpHrSalarySimulationBizModel.java:374/542/617/647/672/714/842/854/868` — `findSourceSalaries`/`buildSimulationCode`/`filterByScope`/`resolveBatchAdjustment`/`recordAdjustment`/`loadEmployeeJobGrades`/`hasPaidSalary`/`hasNonVoidSalary`/`conflictEntry` 共 9 个方法 ~150 行**文件内零调用点**，与 `AbstractErpHrSalarySimulationProcessor.java:69/83/211/349/374/315/425/437/451` 100% 重复维护——删除死副本可 945→~790 行并消除漂移（死方法 `recordAdjustment`:678-693 含裸 dao 写）
2. [major] `module-contract/.../entity/ErpCtInvoicePlanBizModel.java:137-212` — `createApInvoiceDraft`/`createArInvoiceDraft` 死代码副本（triggerInvoice 已委托 Processor）——同时消除未登记跨域写文本（`04` §1.4）
3. [minor] `module-hr/.../entity/ErpHrShiftBizModel.java:42` 类头豁免注释指向已不存在的 findExistingByDate（豁免-现实漂移）

## 4. 重复 helper（同型多份维护）

1. [minor] finance 结账 4 *Service 重复私有 helper：`resolveAcctSchemaId` ×3（ExchangeRevaluationService/AnnualCloseService/ProfitLossClosingService）、`findSubjectByCode` ×3（另含 IntercompanyVoucherGenerator）、`resolveFunctionalCurrencyId` ×4（另含 ErpFinNotesReceivableProcessor）→ 抽共享 `CloseContextResolver` Bean
2. [minor] `module-finance/.../close/CloseVoucherWriter.java:27,67-148` — 静态工具承载多步凭证持久化（newEntity×3+平衡断言+flush），绕过过账引擎（无 traceId/metrics/异常工作台）；`:29-30` 自定义 POSTING_TYPE_NORMAL/VOUCHER_TYPE_TRANSFER 与 ErpFinConstants 重复、`:32-34` 内联 ErrorCode——文档化决策（避免 ArApItem 生成）**待复核**，至少去重常量/错误码
3. [minor] `MrpEngine.java:113` 与 `SimulationMrpEngine.java:312` 各自 65 行 `processMaterial` 近同构（净需求−可用量−在途双份维护）——**待复核**（可能有意快照隔离）
4. [minor] `ErpSalOrderBizModel.recomputeOrderTotals(:241-257)` 与 `ErpSalOrderProcessor.recomputeOrderTotals(:202-212)` 近同构（随 `02` §1 拆分归一）
5. [minor] cs `ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor:812-836` 与 `ErpCsTicketBizModel:181-213` 逐行重复（findLastAssigned/countOpenTickets）→ 抽公共 AssignResolver
6. [minor] `module-cs/.../entity/SlaPolicyMatcher.java:36` — static match(IDaoProvider, ticket) 静态方法携带 DAO 查询编排 → 改实例 Bean（同包引擎均实例注入，范式不一致）

## 5. 正面确认（过程式治理已做对的）

- 3 域 43 个非 Processor 编排类命名全部为具体角色名（*Settler/*Bookkeeper/*Engine/*Orchestrator/*Matcher/*Checker/*Resolver/*Builder/*Dispatcher/*Strategy），无静态 God-helper
- hr/prj/qa/mnt 9 个 Helper/Calculator 中 7 个评估良好（ShiftAttendanceCalculator 纯函数、EquipmentRuntimeCalculator 查询时聚合裁决合理、cost/ 四聚合器、posting/ Dispatcher 全部只读）
- 最长业务方法中 `ErpPurReceiveProcessor.validateOverReceiptTolerance`(:203, 50 行)、`ErpHrSalarySimulationConvertToFormalProcessor`(:23, 89 行) 为单一职责 protected step / 线性 Processor——长而不违例
