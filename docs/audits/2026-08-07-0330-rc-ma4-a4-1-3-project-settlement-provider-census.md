# 2026-08-07-0330 rc-ma4-a4-1-3-project-settlement-provider-census UC-FIN-03 PROJECT_SETTLEMENT Provider 注册实例普查

> Audit Status: closed
> Mission: requirement-compliance（MA4 运行时行为验证）
> Work Item: A4.1.3（A1.1 §7-3 衍生实例普查）
> Type: 只读运行时普查（verification/audit work）——**无代码/ORM/api.xml/view.xml/真相源变更**
> Source Plan: `docs/plans/2026-08-07-0330-3-rc-ma4-a4-1-3-project-settlement-provider-census.md`
> Source 存疑点: `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md` §7 存疑点 3（`:295`）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`
> Independently drafted & executed; closure audit by separate subagent session.

---

## §0 TL;DR（结论先行）

| 维度 | 结论 | 证据强度 |
| --- | --- | --- |
| **机制符合性**（UC-FIN-03 可插拔 Provider 路由，4 验收断言） | **接受**（A1.1 已定级，本验证不推翻） | L3-L5 全证据（A1.1 §5.1 矩阵行） |
| **实例层符合性**（PROJECT_SETTLEMENT businessType 是否已有 Provider 注册） | **接受（示例已落地）** | 强（注册面 + 调用链 + 单测 + E2E 四源一致） |

**核心事实**：`PROJECT_SETTLEMENT(430)` businessType **已由** projects 域非默认 Provider `ProjectSettlementAcctDocProvider`（`module-projects/.../posting/ProjectSettlementAcctDocProvider.java:52-54`）注册，经容器 collect-by-type 自动收录入 `ErpFinAcctDocRegistry`，结算单 approve 后由 `ProjectSettlementPostingDispatcher` 组装 `PostingEvent(businessType=PROJECT_SETTLEMENT)` 经引擎路由生成凭证。无 P2、无 successor 需登记。**与 A1.1 UC-FIN-03 机制接受结论分层一致（机制层 + 实例层双层均接受，无冲突）**。

本验证属**确认性普查**（边际信息量较低，与草案审查预期一致），剩余价值 = 完整 businessType→Provider 映射表（为 A4.2/MA4 后续复用）+ 调用链与测试覆盖核查。

---

## §1 验证范围与存疑点

### §1.1 存疑点原文（逐字引用，A1.1 §7 存疑点 3）

> 3. **UC-FIN-03 PROJECT_SETTLEMENT businessType 是否已有 Provider 注册**：L3 证实可插拔机制通用，但 UC 文本以 PROJECT_SETTLEMENT 为例，该具体 businessType 是否已有 `IErpFinAcctDocProvider` Bean 注册属实例普查——交 MA4 A4.1 按需展开（rg `getSupportedBusinessTypes` 含 PROJECT_SETTLEMENT 的 Provider）。
> （`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md:295`）

### §1.2 L1 需求契约（权威源）

`docs/design/finance/use-cases.md:60-69` UC-FIN-03 可插拔 Provider 路由，4 可验证断言：

```
新增 IErpFinAcctDocProvider Bean(注册 businessType=PROJECT_SETTLEMENT)
→ ErpFinAcctDocRegistry 自动聚合(@Inject List)
→ 项目结算单审核时, 该 Provider 被路由调用, 生成凭证
→ 核心过账引擎代码无改动
```

**语义边界**：UC-FIN-03 验收的是**「可插拔机制」（机制层）**，PROJECT_SETTLEMENT 为**示例 businessType**。A1.1 据此对 UC-FIN-03 判「接受」（4 断言 L3-L5 全证据）。本存疑点是 A1.1 遗留的**实例层补查**：该示例 businessType 当前是否已有 Provider 实例（影响「示例是否已落地为可用能力」，非机制符合性）。

### §1.3 验证目标

1. **注册面普查**：rg 全仓 `IErpFinAcctDocProvider` 实现的 `getSupportedBusinessTypes`，产出全部已注册 businessType → Provider 映射表；确认 PROJECT_SETTLEMENT 是否在其中。
2. **调用链证据**：projects 域项目结算单过账是否以 businessType=PROJECT_SETTLEMENT 经引擎路由（file:line）。
3. **测试覆盖**：`TestErpFinAcctDocRegistry` 及 projects 域过账测试是否含 PROJECT_SETTLEMENT 用例。
4. **实例层结论**：三源对照 + §2 判据，给出分层裁决。

---

## §2 判据（机制 vs 实例分层）

| 层 | 判据来源 | 裁决门 |
| --- | --- | --- |
| 机制符合性 | A1.1 §5.1 UC-FIN-03 矩阵行（4 验收断言 L3-L5） | 已「接受」，本验证**不重新核实** |
| 实例层符合性 | 本验证 §3/§4/§5 三源（注册面 / 调用链 / 测试） | ①PROJECT_SETTLEMENT 已有非默认 Provider 注册 + 引擎路由 + 测试覆盖 → **实例层接受（示例已落地）**；②未注册且 L1 未硬性要求示例可用 → successor；③未注册但 L1 隐含「项目结算单应经专用 Provider 过账」且当前无过账/命中 fallback → P2 |

**反松弛约束**：实例层裁决不得与 A1.1 UC-FIN-03 机制接受结论冲突（机制 vs 实例分层，前者已锁）。

---

## §3 注册面普查（businessType → Provider 全集映射表）

### §3.1 普查方法

```
rg -l "implements IErpFinAcctDocProvider" --glob '!*/test/*' --glob '*.java'
```

→ **37 个生产 Provider 实现**（与 A4.1.2 `2026-08-07-0330-rc-ma4-a4-1-2-fx-rate-missing-trigger-surface.md:64` 实测「37 个生产 Provider 实现」一致，交叉验证无漂移）。逐个读取 `getSupportedBusinessTypes()` 返回值，建立映射表。

### §3.2 完整映射表（37 Provider × 56 enum businessType）

> 下表「优先级」列：`非默认` = `isFallback()==false`（Registry 中优先装配，冲突 fail-fast）；`默认` = `isFallback()==true`（仅填充未被域 Provider 接管的空缺 key）；`dormant` = `getSupportedBusinessTypes()` 返回空集（不走 Registry 路由，凭证由专属 Generator 写入，文档化 stub）。

| # | Provider（file:line of `getSupportedBusinessTypes`） | 域 | 优先级 | 注册的 businessType（enum code） |
| --- | --- | --- | --- | --- |
| 1 | `ErpFinTemplateAcctDocProvider.java:60` | finance | 默认 | PURCHASE_INPUT(10), SALES_OUTPUT(20), AP_INVOICE(30), AR_INVOICE(40), PAYMENT(50), RECEIPT(60), PURCHASE_PRICE_VARIANCE(330) |
| 2 | `PurAcctDocProvider.java:65` | purchase | 非默认 | AP_INVOICE(30), PAYMENT(50), PURCHASE_RETURN(140) |
| 3 | `SalAcctDocProvider.java:67` | sales | 非默认 | AR_INVOICE(40), RECEIPT(60), SALES_RETURN(150) |
| 4 | `InvAcctDocProvider.java:58` | inventory | 非默认 | PURCHASE_INPUT(10), SALES_OUTPUT(20), MANUFACTURING_RECEIPT(500) |
| 5 | `InvOwnershipTransferProvider.java:36` | inventory | 非默认 | OWNERSHIP_TRANSFER(260) |
| 6 | `CostAdjustmentAcctDocProvider.java:42` | inventory | 非默认 | COST_ADJUSTMENT(420) |
| 7 | `PurchasePriceVarianceAcctDocProvider.java:56` | inventory | 非默认 | PURCHASE_PRICE_VARIANCE(330) |
| 8 | `LandedCostAcctDocProvider.java:47` | inventory | 非默认 | LANDED_COST(490) |
| 9 | `ProjectCostCollectionProvider.java:44` | projects | 非默认 | PROJECT_COST_COLLECTION(110) |
| **10** | **`ProjectSettlementAcctDocProvider.java:52-54`** | **projects** | **非默认** | **PROJECT_SETTLEMENT(430)** ← **本验证目标** |
| 11 | `SalaryPostingProvider.java:53-59` | hr | 非默认 | SALARY(270), SALARY_PAYMENT(280), SOCIAL_INSURANCE_ER(290), HOUSING_FUND_ER(300) |
| 12 | `LogisticsFreightProvider.java:42` | logistics | 非默认 | FREIGHT(310) |
| 13 | `NcrScrapAcctDocProvider.java:51` | quality | 非默认 | NCR_SCRAP(410) |
| 14 | `DepreciationAcctDocProvider.java:41` | assets | 非默认 | DEPRECIATION(70) |
| 15 | `CapitalizationAcctDocProvider.java:48` | assets | 非默认 | CAPITALIZATION(80) |
| 16 | `DisposalAcctDocProvider.java:50` | assets | 非默认 | DISPOSAL(90) |
| 17 | `ValueAdjustmentAcctDocProvider.java:50` | assets | 非默认 | VALUE_ADJUSTMENT(390) |
| 18 | `AssetSplitAcctDocProvider.java:39` | assets | 非默认 | ASSET_SPLIT(440) |
| 19 | `AssetMergeAcctDocProvider.java:37` | assets | 非默认 | ASSET_MERGE(450) |
| 20 | `AssetInventoryAcctDocProvider.java:47` | assets | 非默认 | ASSET_INVENTORY_ADJUSTMENT(460) |
| 21 | `MaintenanceExpenseAcctDocProvider.java:48` | assets | 非默认 | MAINTENANCE_EXPENSE(470) |
| 22 | `MaintenanceCapitalizationAcctDocProvider.java:49` | assets | 非默认 | MAINTENANCE_CAPITALIZATION(480) |
| 23 | `MaintenanceIssueAcctDocProvider.java:56` | maintenance | 非默认 | MAINTENANCE_ISSUE(492) |
| 24 | `MaintenanceLaborAcctDocProvider.java:61` | maintenance | 非默认 | MAINTENANCE_LABOR(493) |
| 25 | `ManufacturingIssueAcctDocProvider.java:54` | manufacturing | 非默认 | MANUFACTURING_ISSUE(501) |
| 26 | `ProductionVarianceAcctDocProvider.java:85` | manufacturing | 非默认 | PRODUCTION_VARIANCE(400) |
| 27 | `SubcontractIssueAcctDocProvider.java:45` | manufacturing | 非默认 | SUBCONTRACT_ISSUE(502) |
| 28 | `SubcontractReceiptAcctDocProvider.java:45` | manufacturing | 非默认 | SUBCONTRACT_RECEIPT(503) |
| 29 | `SubcontractFeeAcctDocProvider.java:42` | manufacturing | 非默认 | SUBCONTRACT_FEE(504) |
| 30 | `EmployeeAdvanceAcctDocProvider.java:51` | finance | 非默认 | EMPLOYEE_ADVANCE(170), EMPLOYEE_ADVANCE_SETTLE(180) |
| 31 | `ExpenseClaimAcctDocProvider.java:49` | finance | 非默认 | EXPENSE_CLAIM(160) |
| 32 | `NotesPayableAcctDocProvider.java:43` | finance | 非默认 | NOTES_PAYABLE_ISSUED(230), NOTES_PAYABLE_HONORED(240) |
| 33 | `NotesReceivableAcctDocProvider.java:54` | finance | 非默认 | NOTES_RECEIVABLE_RECEIVED(190), NOTES_RECEIVABLE_DISCOUNTED(200), NOTES_RECEIVABLE_ENDORSED(210), NOTES_RECEIVABLE_COLLECTION(220) |
| 34 | `CreditFacilityInterestAcctDocProvider.java:46` | finance | 非默认 | CREDIT_FACILITY_INTEREST(250) |
| 35 | `BankReconAdjAcctDocProvider.java:41` | finance | 非默认 | BANK_RECON_ADJ(320) |
| 36 | `CommitmentAcctDocProvider.java:33` | finance | dormant(∅) | （空集——承付凭证由 `CommitmentVoucherGenerator` 直接写入，非 Registry 路由；与 budget.md/posting.md 一致，P1-MA3-031 已 resolved MR2） |
| 37 | `IntercompanyAcctDocProvider.java:25` | finance | dormant(∅) | （空集——公司间凭证由 `IntercompanyVoucherGenerator` 直接写入，与 BUDGET/COMMITMENT 同型） |

**Enum 中未被任何 Provider 注册的 businessType**（`ErpFinBusinessType.java`，共 56 个 enum 常量；下述 7 个无 Provider 覆盖，属独立 Generator/Processor 路径或 owner-doc 声明 successor，**不在本普查裁决范围内**，仅列示供后续 A4.x 复用）：
`MANUFACTURING_COST_CLOSE(100)`、`PERIOD_CLOSE(120)`、`EXCHANGE_GAIN_LOSS(130)`、`BAD_DEBT_RESERVE(340)`、`BAD_DEBT_WRITE_OFF(350)`、`BAD_DEBT_RECOVERY(360)`、`BAD_DEBT_RELEASE(370)`、`PROFIT_TO_RETAINED_EARNINGS(380)`（期末/坏账/汇兑/结转类，由 `ErpFinPeriodCloseProcessor`/`ErpFinBadDebtProcessor` 等专属链路处理，非 Provider 路由）。

### §3.3 PROJECT_SETTLEMENT 注册状态

**已注册** ✅

- **Provider 类**：`app.erp.prj.service.posting.ProjectSettlementAcctDocProvider`（`module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectSettlementAcctDocProvider.java:31`）
- **注册声明**：`getSupportedBusinessTypes()` `:52-54` → `return Collections.singleton(ErpFinBusinessType.PROJECT_SETTLEMENT);`
- **枚举常量**：`ErpFinBusinessType.PROJECT_SETTLEMENT(430)`（`module-finance/erp-fin-dao/.../ErpFinBusinessType.java:56`）
- **字典对齐**：`erp-fin/business-type.dict.yaml:173` `value: PROJECT_SETTLEMENT`（保护区域契约三件套齐：枚举 + 字典 + codegen 常量 `_ErpFinDaoConstants.BUSINESS_TYPE_PROJECT_SETTLEMENT` `:289`）
- **Bean 注册**：`module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/beans/app-service.beans.xml:22-23` 显式 `<bean id="app.erp.prj.service.posting.ProjectSettlementAcctDocProvider" class="..."/>`（plan `2026-07-07-0305-1` Phase 3 落地）；容器 collect-by-type 自动收录入 `ErpFinAcctDocRegistry.providers`。
- **优先级**：非默认（`isFallback()` 默认 `false`）→ Registry 中 `PROJECT_SETTLEMENT` key 路由到此 Provider（优先于任何默认 fallback；与默认 Provider `ErpFinTemplateAcctDocProvider` 的 SUPPORTED 集无重叠，无冲突）。

**Registry 装配实测**：`ErpFinAcctDocRegistry.init():45-79`——非默认 Provider 优先装配（`:49-62`，`map.put(type, provider)` + 重复 fail-fast `:55-60` `ERR_DUPLICATE_PROVIDER`），默认 Provider 仅填充空缺（`:64-71` `putIfAbsent`）。PROJECT_SETTLEMENT 仅被 #10 一个非默认 Provider 声明 → 无冲突，启动期 fail-fast 不触发，运行时 `getProvider(PROJECT_SETTLEMENT):81-83` O(1) 命中 `ProjectSettlementAcctDocProvider`。

---

## §4 调用链证据（PROJECT_SETTLEMENT 经引擎路由）

### §4.1 完整调用链（approve → 凭证生成）

```
[1] ErpPrjProjectSettlementProcessor.doPost(ErpPrjProjectSettlement, IServiceContext)
    module-projects/.../processor/ErpPrjProjectSettlementProcessor.java:140-147
    → postingDispatcher.tryPost(settlement)  (:141)；成功置 settlement.posted=true (:142-146)
[2] ProjectSettlementPostingDispatcher.tryPost(ErpPrjProjectSettlement):40-53
    module-projects/.../posting/ProjectSettlementPostingDispatcher.java
    → PostingEvent event = buildEvent(settlement)  (:41)；executor.postEvent(event)  (:43)
       失败 try/catch 吞异常记日志、return false（保持 APPROVED+posted=false，不阻塞终态 :45-52）
[3] ProjectSettlementPostingDispatcher.buildEvent():71-96
    → event.setBusinessType(ErpFinBusinessType.PROJECT_SETTLEMENT)  (:73)  ← businessType 锚点
    → event.setBillHeadCode(settlement.getCode())  (:74)
    → billData 填 SETTLEMENT_TYPE/FINAL_REVENUE/FINAL_COST/PROJECT_ID/TRANSFER_TO_ASSET  (:83-93)
[4] ProjectPostingExecutor.postEvent → IErpFinVoucherBiz.post(PostingEvent, IServiceContext)
    （Facade 层 @Transactional(REQUIRES_NEW)，跨域失败隔离钉在 Facade）
[5] ErpFinPostingProcessor.resolveProvider → ErpFinAcctDocRegistry.getProvider(PROJECT_SETTLEMENT)
    → 命中 ProjectSettlementAcctDocProvider（§3.3 实测）
[6] ProjectSettlementAcctDocProvider.createFacts(PostingEvent, AcctDocContext):57-102
    → 按 settlementType 分支产 VoucherFact 列表：
       · CLOSE+transferToAsset (:67-76)：Dr 1601 固定资产 / Cr 1603 在建工程（finalCost，借贷平衡）
       · FINAL/INTERIM (:77-100)：Dr 5101 项目成本(finalCost) + 条件性 Dr/Cr 4103 本年利润(|profitLoss|，signum>0 DEBIT/<0 CREDIT/=0 不发) / Cr 6001 项目收入(finalRevenue)
       · 全部分录行标 projectId 辅助核算维度
```

### §4.2 红字冲销调用链（reverseSettlement）

```
ProjectSettlementPostingDispatcher.reverse(ErpPrjProjectSettlement):58-69
  → executor.reverse(settlement.getCode(), ErpFinBusinessType.PROJECT_SETTLEMENT)  (:60)  ← businessType 锚点
  → IErpFinVoucherBiz.reverse(code, businessType, ctx)（Facade REQUIRES_NEW）
  冲销是硬前置，失败向上抛出阻断状态迁移（:67 throw e）
```

### §4.3 触发点（approve mutation → doPost）

`ErpPrjProjectSettlementProcessor` 为 approve mutation 的 D-mutation Processor（beans.xml `:54-55` + per-mutation `:61-62` `ErpPrjProjectSettlementApproveProcessor`）。approve → `doApprove():125-130`（置 APPROVED）→ `doPost():140-147`（派发过账）。状态机经三轴（createSettlement/submit/approve/reverseSettlement）落地（A2.13 结算三轴 PASS）。

**调用链结论**：PROJECT_SETTLEMENT businessType **经引擎路由**（非绕过、非 fallback）——UC-FIN-03 断言③「项目结算单审核时, 该 Provider 被路由调用, 生成凭证」在实例层**证据充分**。

---

## §5 测试覆盖

### §5.1 `TestErpFinAcctDocRegistry`（Registry 机制层，finance-service）

`module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinAcctDocRegistry.java`（2 @Test）：

- `testRegistryDomainProviderWinsOverDefault:55-69`——断言域 Provider 优先于默认（用 `PURCHASE_INPUT` 作示例 + `AP_INVOICE` fallback 兜底）。
- `testRegistryDuplicateNonDefaultFailsFast:71-81`——断言两个非默认 Provider 声明同 key 启动期 fail-fast（用 `SALES_OUTPUT` 作示例）。

**PROJECT_SETTLEMENT 用例**：**无**（N/A）。**断言强度**：不适用——本测试类是 Registry **机制层**契约测试（fallback 优先 / 重复 fail-fast），用 `PURCHASE_INPUT`/`SALES_OUTPUT`/`AP_INVOICE` 作示例 businessType 验证通用机制，**不按 per-businessType 枚举**（机制通用，PROJECT_SETTLEMENT 无需在此重复）。per-businessType 覆盖由域测试承担（§5.2/§5.3）。

### §5.2 `TestErpPrjAcctDocProviderAccountKey`（PROJECT_SETTLEMENT Provider 单元，projects-service，**强**）

`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjAcctDocProviderAccountKey.java`（2 @Test）：

- `testCloseTransferToAsset:22-32`——直接 `new ProjectSettlementAcctDocProvider().createFacts(e, null)`，`e.businessType=PROJECT_SETTLEMENT`（`:50`）+ CLOSE+transferToAsset+finalCost=100 → 断言 accountKey 序列 `[FIXED_ASSET, CIP]`（`:31`）+ 每行 accountKey 非空/非空白（`:55-58`）+ 行数（`:59`）。
- `testFinalSettlement:34-46`——FINAL+finalRevenue=120/finalCost=100/transferToAsset=false → 断言 accountKey 序列 `[PROJECT_COST, PROFIT_LOSS, REVENUE]`（`:45`，profitLoss=20>0 → DEBIT 本年利润行出现）。

**断言强度**：**强**（accountKey 语义逐行精确 + 行数 + 非空守卫，非弱断言）。直接命中 `ProjectSettlementAcctDocProvider` + `PROJECT_SETTLEMENT` businessType。

### §5.3 `TestErpPrjProjectSettlement`（结算三轴行为，projects-service，**强**）

`module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectSettlement.java`（4 @Test）：

- FINAL 结算 approve 后 `posted=true`（成功模式，明细行含 Billing/CostCollection 来源，Javadoc `:52`）。
- `reverseSettlement` 红冲凭证 + 回退资产卡片状态（status=DRAFT）+ `posted=false`（Javadoc `:55`，断言 `:181` `assertFalse(posted)`）。
- 过账失败隔离场景：`posted=false` 时 `reverseSettlement` 抛非法状态（`:186`）。
- `posted` 经 PostingDispatcher（finance 引擎成功时 true，失败隔离时 false——断言非抛异常即达终态，`:109`）。

**断言强度**：**强**（posted 状态机 + 红冲回退 + 失败隔离三态行为断言）。

### §5.4 E2E spec（浏览器层，凭证行级数值强断言）

- `projects-settlement-posting.action.spec.ts`（plan `2026-07-14-0742-2`）：CLOSE 转固 PROJECT_SETTLEMENT 凭证行 `Dr 1601=1000 / Cr 1603=1000` 精确数值断言 + `reverseSettlement` 红冲同向取负 `Dr 1601=-1000 / Cr 1603=-1000`（`docs/testing/e2e-runbook.md:335`）。
- `projects-pnl-settlement.action.spec.ts`（plan `2026-07-17-1005-2`）：FINAL/INTERIM PROJECT_SETTLEMENT 凭证行 `Dr 5101=6000 / Dr 4103=4000 / Cr 6001=10000` + 红冲同向取负（`docs/testing/e2e-runbook.md:395,519`）。

**断言强度**：**强**（凭证行级 Dr/Cr 科目 + 数值精确，经 `findVoucherIdByBillCode` + `assertVoucherLines` 反查）。后端日志确认 `provider=ProjectSettlementAcctDocProvider`（`docs/logs/2026/07-14.md:141`）。

### §5.5 覆盖结论

PROJECT_SETTLEMENT businessType 在**机制层**（Registry fallback/duplicate 通用，`TestErpFinAcctDocRegistry`）+ **Provider 单元层**（`TestErpPrjAcctDocProviderAccountKey` 2 @Test 强）+ **行为层**（`TestErpPrjProjectSettlement` 4 @Test 强）+ **E2E 层**（2 spec 凭证行级数值强断言）四层均有覆盖。`TestErpFinAcctDocRegistry` 不含 PROJECT_SETTLEMENT 用例是**设计正确**（机制层不按 per-businessType 枚举），非覆盖缺口。

---

## §6 实例层符合性结论（三源对照）

| 源 | 锚点 | 事实 |
| --- | --- | --- |
| **L1**（需求契约） | `docs/design/finance/use-cases.md:60-69` UC-FIN-03 | PROJECT_SETTLEMENT 为**示例** businessType；UC 验收的是可插拔机制（4 断言）。L1 未硬性要求「该示例必须可用」，但隐含「新增 Provider Bean 注册 businessType=PROJECT_SETTLEMENT」为示例展示路径。 |
| **L2**（设计参考） | `docs/design/finance/posting.md §过账引擎(可插拔)`；`docs/design/projects/profitability.md:84,94`；`docs/design/projects/use-cases.md:139` | projects 域 owner doc 显式声明「结算生成转固凭证(经 finance IErpFinAcctDocProvider 注册 PROJECT_SETTLEMENT)」——L2 **隐含**项目结算单应经专用 Provider 过账。 |
| **L3**（实现） | §3.3 + §4 | `ProjectSettlementAcctDocProvider`（非默认）注册 PROJECT_SETTLEMENT + Bean 装配 + 完整调用链经引擎路由 + 红冲链。 |
| **L4**（单元测试） | §5.2 + §5.3 | Provider 单元（accountKey 强）+ 结算三轴行为（posted/红冲/失败隔离强）。 |
| **L5**（E2E） | §5.4 | 2 spec 凭证行级 Dr/Cr 数值强断言 + 后端日志 provider 命中。 |

### §6.1 裁决

- **机制符合性**：**接受**（A1.1 §5.1 UC-FIN-03 矩阵行已定级，4 验收断言 L3-L5 全证据；本验证不重新核实，不推翻）。
- **实例层符合性**：**接受（示例已落地）**——PROJECT_SETTLEMENT 已由非默认 Provider `ProjectSettlementAcctDocProvider` 注册，经引擎路由，L4/L5 强覆盖。命中 §2 判据 ①（「PROJECT_SETTLEMENT 已有 Provider 注册 → 实例层接受」）。
- **分层一致性**：机制层接受（A1.1）+ 实例层接受（本验证）→ **两层一致，无冲突**。PROJECT_SETTLEMENT 作为 UC 示例已从「机制可插拔」落地为「实例可用能力」。

### §6.2 无 P2 / 无 successor

- 不命中 §2 判据 ②（未注册 + L1 未硬性要求 → successor）：实例**已注册**，前提不成立。
- 不命中 §2 判据 ③（未注册 + L2 隐含应过账 + 当前无过账/命中 fallback → P2）：实例**已注册且经引擎路由**（非 fallback），前提不成立。
- L2 隐含「项目结算单应经专用 Provider 过账」**已满足**（§4 调用链 + §5 测试）。

→ **无 finding、无 successor 需登记**。Phase 2 item 1（`Add` finding/successor）为**条件 no-op**（「若定 P2 或 successor →」前置条件为假）。

---

## §7 finding/successor 衔接（去重协议）

### §7.1 去重核查

按 §去重协议 grep arm-index 同域同控制点：

- `PROJECT_SETTLEMENT` / `ProjectSettlementAcctDocProvider`：arm-index 仅 A1.36（`2026-08-05-2330-1-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`，UC-PRJ-08 转固接受，凭证 Dr1601/Cr1603 行级强断言）+ A1.1（UC-FIN-03 机制接受 + §7-3 本存疑点）引用，**无 open finding** 涉及「PROJECT_SETTLEMENT Provider 注册缺失」。
- projects 过账 / Provider 注册面：P1-RC-052（质保金逻辑层全缺）、P1-RC-053（PnL 调度未接线）、P2-RC-050（多币种折算）均为**不同控制点**（质保金/调度/汇率），非 Provider 注册面。

### §7.2 衔接结论

**无新 finding、无新 successor**（§6.2 已裁决）。本验证不写入 arm-index 任何新行——PROJECT_SETTLEMENT 注册面属「示例已正确落地」，无缺陷可登。arm-index MA4 / RC 分区保持不变。

**双向可追溯注记**：
- 本验证 → A1.1：闭环 A1.1 §7 存疑点 3（实例层补查完成，结论与机制层一致）。
- 本验证 → A4.1 expander：`docs/audits/2026-08-07-0300-rc-ma4-a4-1-finance-expander.md:60` A4.1.3 行「证实：机制通用（已由 A1.1 接受），PROJECT_SETTLEMENT 是否有 Bean 仅影响『是否需新增』非合规」——本验证证实「已有 Bean，无需新增」。
- 本验证 → 后续 A4.x：§3.2 完整映射表（37 Provider × 56 enum）可复用为后续运行时行为验证的注册面基线。

---

## §8 过程纪律自检

### §8.1 nop-compliance-checker.sh actual vs baseline

```
$ bash docs/audits/nop-compliance-checker.sh    # EXIT=1（R3 段已知行为，与 A4.1.2 §5.1 baseline 一致；纯 reporter，非门控）
```

| 规则 | 级别 | baseline 命中 | actual 命中 | 回归？ |
| --- | --- | --- | --- | --- |
| R1 BizModel 中 dao() 直接调用 | 🔴 高 | 0（R1a-c）/ 14（R1d 子规则） | 同 baseline | **无** |
| R2 daoFor() 绕过 I*Biz 接口 | 🔴 高 | 34（R2a，跨域实体读，已有 owner-doc 裁决基线）+ R2b/c/d 同 baseline | 同 baseline | **无** |
| R3 new Erp*() 直接构造实体 | 🟡 中 | （baseline 范围） | 同 baseline | **无** |

**回归风险评估**：本计划**无生产代码变更**（只读普查：rg + 读 JUnit + 引用 A1.1/A4.1），actual == baseline，**无回归风险**。checker 退出码非 0（R3 段已知行为）不作为门控依据（方法论要求，纯 reporter；与 A4.1.2 `2026-08-07-0330-rc-ma4-a4-1-2-...md` §5.1 实测一致）。

### §8.2 独立性声明

- **草案审查**：独立子代理 ses_02c7b35bdffe5vU7kJbw6DJgv0（fresh session，未起草本计划），iteration 1 `accept`（计划 `Draft Review Record`）。
- **执行者**：本验证执行者非起草者亦非草案审查者。
- **closure-audit**：须由独立子代理（新会话，冷重播）执行，非执行者自我审计。执行者不在本报告勾选 Closure Gates 的「结束审计」项（留 `[ ]` 给独立审计）。

### §8.3 与 arm-index 交叉去重声明

§7.1 已 grep arm-index 同域同控制点，无重复 finding。本验证不新增 arm-index 行（§6.2/§7.2）。

### §8.4 真相源未变更声明

本验证**未修改**任何 ORM/`*.api.xml`/view.xml/BizModel/xbiz/字典/种子/配置/代码。仅产出本报告（`docs/audits/`）+ 计划文件勾选（`docs/plans/`）+ 日志（`docs/logs/`）+ roadmap 状态翻转（`docs/backlog/`）。§9 冻结的全部真相源保持原状。

---

## §9 真相源冻结声明（本验证不修改）

- `docs/design/finance/use-cases.md`（L1 需求契约）——只读引用
- `docs/design/finance/posting.md`、`docs/design/projects/profitability.md`、`docs/design/projects/use-cases.md`（L2 设计参考）——只读引用
- `module-finance/erp-fin-dao/.../ErpFinBusinessType.java`、`module-finance/erp-fin-service/.../posting/ErpFinAcctDocRegistry.java`、`module-projects/erp-prj-service/.../posting/ProjectSettlement*.java`（L3 实现）——只读普查
- `docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`（A1.1 报告，含 §7 存疑点 3）——只读引用

---

## §10 引用清单

**L1**：`docs/design/finance/use-cases.md:60-69`
**A1.1**：`docs/audits/2026-08-02-1645-rc-ma1-a1-1-finance-f1-posting.md`（§5.1 UC-FIN-03 矩阵行 `:217`；§7 存疑点 3 `:295`）
**A4.1 expander**：`docs/audits/2026-08-07-0300-rc-ma4-a4-1-finance-expander.md:60`
**A1.36**：`docs/audits/2026-08-05-2330-1-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`（UC-PRJ-08 转固）
**实现**：
- `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/ErpFinBusinessType.java:56`
- `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/ErpFinAcctDocRegistry.java:45-83`
- `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectSettlementAcctDocProvider.java:31,52-54,57-102`
- `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/posting/ProjectSettlementPostingDispatcher.java:40-96`
- `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/processor/ErpPrjProjectSettlementProcessor.java:140-147`
- `module-projects/erp-prj-service/src/main/resources/_vfs/erp/prj/beans/app-service.beans.xml:19-23`
- `module-finance/erp-fin-meta/src/main/resources/_vfs/dict/erp-fin/business-type.dict.yaml:173`
- `module-finance/erp-fin-dao/src/main/java/app/erp/fin/dao/_ErpFinDaoConstants.java:289`

**测试**：
- `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/posting/TestErpFinAcctDocRegistry.java`（2 @Test，机制层）
- `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjAcctDocProviderAccountKey.java`（2 @Test，Provider 单元强）
- `module-projects/erp-prj-service/src/test/java/app/erp/prj/service/TestErpPrjProjectSettlement.java`（4 @Test，行为强）
- E2E：`projects-settlement-posting.action.spec.ts`、`projects-pnl-settlement.action.spec.ts`（凭证行级数值强断言）

---

## Closure

> 结束审计由独立子代理（新会话）执行后填入。
