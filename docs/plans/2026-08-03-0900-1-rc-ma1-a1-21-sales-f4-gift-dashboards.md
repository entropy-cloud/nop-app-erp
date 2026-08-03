# 2026-08-03-0900-1 rc-ma1-a1-21-sales-f4-gift-dashboards sales-F4 赠品与看板需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.21（MA1 需求追踪矩阵审计 — sales-F4 赠品与看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.21
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.21 的 0.2 依赖）、`2026-08-03-0407-1-rc-ma1-a1-18-sales-f1-mainflow-pricing.md` + `2026-08-03-0407-2-rc-ma1-a1-19-sales-f2-outbound-concurrency.md` + `2026-08-03-0407-3-rc-ma1-a1-20-sales-f3-returns-family.md`（同域 sales 审计先批 F1/F2/F3/F4；本切片含 P1-RC-022 价税分离复用、P2-RC-018 pricing 冒烟复用、P1-MA2-093 看板行级权限复用）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.21 给出 UC 清单 = `UC-SAL-08/12`（2 UC），含 `use-cases.md:180/:265` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/sales/use-cases.md`（机制见 `state-machine.md §9` + `../dashboards.md §销售看板`）：
  - UC-SAL-08 赠品行扣库存 + 价税分离（`:180`）：订单含赠品行（数量>0、单价=0），赠品也需触发出库扣库存（可用量 −= 赠品数量）；赠品成本计入销售成本（存货估值红冲按成本，非按售价 0）。折扣价税分离：折扣后金额 = 原金额 − 折扣额；税额 = 折扣后金额 / (1 + 税率) × 税率；不含税金额 = 折扣后金额 − 税额。
  - UC-SAL-12 销售看板（`:265`）：KPI 卡片值 == 对应实体的实时聚合（按期间/orgId/权限过滤），含本期销售额/订单量、应收账龄、销售趋势、客户 TOP10；预警项 == 满足阈值条件的记录（阈值来自系统配置非硬编码）；看板数据受行级权限约束（只看自己组织/部门/成本中心）。

- **L3 代码实现现状（实测）**——UC-SAL-08 赠品行扣库存 + 价税分离、UC-SAL-12 看板均已实现，存在若干缺口：
  - **赠品行生成（UC-SAL-08）**：`ErpSalPricingRuleEngine.addGiftLine:203-217`（giftLine.unitPrice=0 `:210` / quantity=giftQuantity `:211` / amount=0 `:212` / pricingSource=PROMOTION `:213` / remark="赠品行" `:214`）；赠品规则配置列在 `ErpSalPricingRule`（`app-erp-sales.orm.xml:1125-1127` giftMaterialId/giftSkuId/giftQuantity）。**ErpSalOrderLine 无显式 isGift/lineType 列**（`app-erp-sales.orm.xml:396-407` unitPrice/quantity/taxAmount/amount/discountAmount；跨模块 grep `isGift|lineType` 生产代码 0 匹配）——赠品识别为隐式（pricingSource=PROMOTION + remark）。`docs/design/sales/ui-patterns.md:11,36,116` 要求行级显式"赠品"开关（锁定单价 0），**UI 层缺口**（后端扣库存行为不受影响）。
  - **赠品出库扣库存（UC-SAL-08，已实现）**：`ErpSalDeliveryProcessor.triggerOutgoingMove:241-245` → `DeliveryStockMoveBuilder.buildLines:54-67`（`:61` 遍历**全部** delivery 行扣库存，无赠品过滤；`:62` Javadoc 明示 unitCost 由库存域 avgCost 快照，不传 unitPrice）→ `IErpInvStockMoveBiz.generateMove`（跨域，含 validateAvailable）。赠品数量计入可用量校验与扣减。MA2 sales 状态机审计 `2026-07-28-0400-arm-ma2-sales-state-machine.md:180,260-264` 场景(i)赠品库存扣减 **PASS**。
  - **赠品成本计入销售成本（UC-SAL-08，已实现）**：赠品行 unitPrice=0 不传入库存域；库存域 `InvPostingDispatcher.computeTotalCost:184-212`（avgCost×qty）→ `InvAcctDocProvider.createFacts:64-87`（SALES_OUTPUT 分支 `:81-85` 借 6401 主营业务成本 / 贷 1401 库存商品，按 inventory avgCost 非 0）→ 赠品成本按成本入销售成本（满足 `use-cases.md:188`）。销售发票过账 `SalAcctDocProvider.createFacts:73-93` 仅用头合计（`KEY_TOTAL_AMOUNT` 等 `:77-79`），赠品行对收入/销项税贡献 0（正确——赠品免费）。
  - **折扣价税分离（UC-SAL-08，缺口 #1 — 复用既有 finding）**：`ErpSalOrderBizModel.recomputeLineAmount:172-179`（`:176-178` gross−discountAmt→net→setAmount，**仅 setAmount 无 setTaxAmount**）；`recomputeOrderTotals:181-197`（`:187` 复用**促销前陈旧** line.taxAmount 求和，从不重算）。**此为 P1-RC-022**（A1.18 sales-F1 登记，`arm-index.md:132`，UC-SAL-11⑦ 价税分离缺失）。UC-SAL-08:190-193 验收标准语言与 UC-SAL-11⑦ 完全一致（"价税分离"同根因同控制点同修复点），按 §7 裁决**复用 P1-RC-022（追加 RC 交叉引用注记）**，不新建。
  - **销售看板聚合 API（UC-SAL-12，已实现）**：`ErpSalDashboardBizModel`（`module-sales/erp-sal-service/.../dashboard/ErpSalDashboardBizModel.java`）— `getDashboardKpi:61-92`（salesAmount `:85`/orderCount `:86`/invoiceCount `:87`/conversionRate `:88`/arBalance `:89`）、`getDashboardTrend:94-120`（月销售额序列）、`findCustomerTopN:122-164`（TOP-N 客户）、`findArOverdueAlert:170-209`（超期应收列表，含 partnerName/sourceBillCode/openAmount/ageDays）。全部 `@BizQuery`。KPI 经 `ErpSalInvoice`(posted)+`ErpSalOrder`(ACTIVE)+`IErpFinArApItemBiz`(RECEIVABLE OPEN/PARTIAL) 实时聚合。
  - **看板阈值配置化（UC-SAL-12，已实现）**：`findArOverdueAlert:171-180` daysThreshold/amountThreshold 均 `AppConfig.var(...)`（`ErpSalConstants.java:80-84` CONFIG_DASH_SAL_AR_OVERDUE_DAYS/AMOUNT + DEFAULT 0/ZERO 默认禁用）。满足 `use-cases.md:276`（阈值来自系统配置非硬编码）。与 mfg 看板 P2-RC-009（阈值未配置化）形成**正面对比**。
  - **看板 AMIS 接线（UC-SAL-12，已实现）**：菜单 `erp-sal.action-auth.xml:98-107`（sal-dashboard/sal-dashboard-main `useCases="UC-SAL-12"`）；页面 `erp/sal/pages/dashboard/main.page.yaml`（KPI `:36`/趋势 `:87`/TOP-N `:117`/超期 CRUD `:154` + 筛选 reload `:23-27`）。
  - **应收账龄 4 桶（UC-SAL-12，缺口 #2）**：`dashboards.md:60` 规定应收账龄 0-30/31-60/61-90/90+ 四桶结构化视图；实现 `findArOverdueAlert` 仅返回**扁平 ageDays 列表**，无四桶分桶。预警列表是更严格子集但缺失结构化账龄视图。**未登记 finding**（候选新 P2-RC-xxx watch-only）。
  - **看板行级权限（UC-SAL-12 — 复用既有 finding）**：`use-cases.md:279` 要求行级权限。`ErpSalDashboardBizModel` 用 `IServiceContext context` 参数但查询为 raw QueryBean over IDaoProvider/IOrmTemplate，无显式 orgId 注入。此为 **P1-MA2-093**（resolved R1.29 全局 `ErpOrgIsolationQueryTransformer`），按 §7 **复用**（A1.7 finance/A1.11 mfg 看板已设复用先例）。
  - **跨域 Facade**：`IErpInvStockMoveBiz`（赠品扣库存）、`IErpFinArApItemBiz`（看板应收聚合）、`IErpMdPartnerBiz`（读）。

- **L4 测试证据现状**（`module-sales/erp-sal-service/src/test/`）：
  - 赠品引擎 `TestErpSalPricingRuleEngine#testGiftLine:91-110`（**强** — result 行数=2 + giftLine unitPrice=0/quantity=ONE/pricingSource=PROMOTION + giftRuleIds）；`TestErpSalPricingEndToEnd#testScenario4_Gift:89-101`（**弱/冒烟** — 仅 status==0，未断言赠品行/折扣/合计）→ 此为 **P2-RC-018**（A1.18 登记 `arm-index.md:135`，7 场景含 testScenario4_Gift 仅冒烟）。
  - 看板 `TestErpSalDashboard`（6 方法，全部**强**：testKpiEmptyDatasetReturnsZeros / testKpiAggregationAndConversionRate 断言 salesAmount=300/orderCount=4/conversionRate=0.5/arBalance=500 / testTrendMonthlySeries / testCustomerTopN / testArOverdueAlertDisabledByDefault / testArOverdueAlertTriggers 配置 90/500 断言 1 预警）。
  - E2E `dashboards/sales.value.spec.ts`（**强** — assertDashboardKpiValues 断言 salesAmount=1000/orderCount=1/invoiceCount=1 严格相等）；`dashboards/sales.smoke.spec.ts`（**弱/冒烟**，P1-MA5-012 → R3.2 closed 已补 value 层）。
  - **缺口测试**：价税分离重算（零测试，P2-RC-018 衍生）；赠品扣库存隔离断言（"赠品数量参与扣减"无独立 JUnit 断言，仅 MA2 行为 PASS）；赠品成本按 avgCost 入 6401 无独立断言；AR 账龄四桶视图（无，因实现缺失）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`（A2.9）：§场景(i) 赠品库存扣减 **PASS**（applyPricingRules 后置追加赠品行 amount=0 quantity 计入 → 经标准 delivery approve 路径扣库存）。无看板 finding。
  - `docs/audits/2026-07-27-1949-arm-ma2-order-to-cash-e2e.md`：无赠品/看板 finding（P2-MA2-009..015 均为多币种/红字发票/核销维度，非赠品/看板）。
  - `docs/audits/2026-07-29-0430-arm-ma4-pur-sal-inv-qa-crm-code-quality.md`（A4.5）：P1-MA4-021（**resolved R2.14**）。无赠品/看板 finding。
  - **本切片须声明与上述 MA2 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（赠品行显式 UI 标记缺口 / 折扣价税分离缺口复用 P1-RC-022 / AR 账龄四桶缺口 / 看板行级权限复用 P1-MA2-093）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-RC-022`（价税分离缺失，A1.18 `arm-index.md:132`，**直接相关 UC-SAL-08**）、`P2-RC-018`（pricing 7 场景含赠品仅冒烟 `:135`）、`P1-MA2-093`（看板行级权限，resolved R1.29）、`P1-MA5-012`（dashboard smoke → R3.2 closed）、`P2-RC-009`（mfg 看板阈值未配置化 — **正面对比** sales 看板无此缺陷）。**本切片须 grep arm-index sales 赠品/看板同域同控制点后裁决复用 or 新建**：UC-SAL-08 价税分离 → 复用 P1-RC-022；UC-SAL-12 行级权限 → 复用 P1-MA2-093；pricing 冒烟 → 复用 P2-RC-018；赠品行显式 UI 标记 / AR 账龄四桶为**未登记**缺口，须新建并 grep 比对。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；触及会计过账逻辑（价税分离重算）或 ORM 结构（补 isGift 列）的修复行须 ask-first（§5 保护区域暂停协议）。

- **剩余差距**：A1.21 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.21 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.21 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-21-sales-f4-gift-dashboards.md`，含方法论 §6 **9 段全部内容**。
- 对 2 UC（UC-SAL-08/12）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：#1 折扣价税分离缺失（**复用 P1-RC-022** 追加 RC 交叉引用）、#2 赠品行显式 UI 标记缺口（ui-patterns.md:11,36,116，候选 P2）、#3 AR 账龄四桶视图缺失（候选 P2）、#4 看板行级权限（**复用 P1-MA2-093**）、#5 pricing 赠品场景冒烟（**复用 P2-RC-018**）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；既有行追加 RC 注记）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/state-machine.md/dashboards.md/ui-patterns.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.18/1.19/1.20 各自独立 plan 已 done；A1.21 只覆盖 UC-SAL-08/12）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：赠品库存扣减行为由 A2.9 PASS 证实，只补需求视角差异）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §7 复用裁决）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.21 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.21 UC 锚点）+ `docs/design/sales/use-cases.md`（L1 真相源）+ `docs/design/sales/state-machine.md §9` + `docs/design/dashboards.md §销售看板` + `docs/design/sales/ui-patterns.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测/E2E；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-sales/erp-sal-service -Dtest=TestErpSalDashboard,TestErpSalPricingRuleEngine`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-21-sales-f4-gift-dashboards.md`（落盘 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-SAL-08/12 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:180/:265` 验收标准原文；L2 引用 `state-machine.md §9` + `dashboards.md §销售看板:48-63` + `ui-patterns.md:11,36,116`（标注"设计参考，冲突以 L1 为准"，注意 ui-patterns 行级赠品开关要求 vs 实现隐式标记 drift 注记）；L3 引用 `module-sales/.../ErpSalPricingRuleEngine.java:<line>` / `DeliveryStockMoveBuilder` / `ErpSalOrderBizModel.recomputeLineAmount` / `ErpSalDashboardBizModel` / `ErpSalConstants`（含跨域 `IErpInvStockMoveBiz`/`IErpFinArApItemBiz`/`InvAcctDocProvider`/`InvPostingDispatcher`）；L4 引用 `TestErpSal*.java#method` / E2E spec（注明断言强度）；L5 复用 MA2 A2.9 赠品 PASS + 单测/E2E。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-SAL-08 赠品扣库存（已实现，MA2 A2.9 PASS，`DeliveryStockMoveBuilder:54-67` 全行扣减）；②UC-SAL-08 赠品成本按 avgCost 入 6401（已实现，`InvAcctDocProvider:81-85` SALES_OUTPUT）；③#1 折扣价税分离缺失（`ErpSalOrderBizModel.recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount，`recomputeOrderTotals:181-197` 复用陈旧 taxAmount）——复用 P1-RC-022；④#2 赠品行显式 UI 标记缺口（无 isGift/lineType 列，ui-patterns:11,36,116 要求行级开关）；⑤UC-SAL-12 KPI 卡片实时聚合（已实现 `getDashboardKpi:61-92`）；⑥UC-SAL-12 阈值配置化（已实现 `AppConfig.var`，对比 mfg P2-RC-009）；⑦#3 AR 账龄四桶缺失（`findArOverdueAlert` 扁平 ageDays，dashboards.md:60 要求 0-30/31-60/61-90/90+ 四桶）；⑧#4 看板行级权限（复用 P1-MA2-093）；⑨#5 pricing 赠品场景冒烟（复用 P2-RC-018）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：UC-SAL-08 赠品扣库存+成本→**接受**（行为正确）；UC-SAL-08 价税分离→**复用 P1-RC-022**（同根因同控制点，§7 裁决）；UC-SAL-08 赠品行显式 UI 标记→倾向 **P2**（后端行为正确，UI 层 cosmetic 缺口）；UC-SAL-12 KPI/阈值→**接受**；UC-SAL-12 AR 账龄四桶→倾向 **P2**（预警列表是更严格子集，缺失结构化视图）；UC-SAL-12 行级权限→**复用 P1-MA2-093**（resolved R1.29）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-SAL-08/12 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.9 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#5 有明确分级（非悬空"待查"）；复用裁决（P1-RC-022/P1-MA2-093/P2-RC-018）均 grep arm-index 后记录依据

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-21-sales-f4-gift-dashboards.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` sales 赠品/看板同域同控制点（P1-RC-022 价税分离 / P1-MA2-093 看板行级权限 / P2-RC-018 pricing 冒烟）后裁决——同根因同控制点 → 复用（追加 RC 注记）；新根因 → 新建 `P2-RC-xxx` 列明差异依据。#2 赠品 UI 标记 / #3 AR 账龄四桶为**未登记**缺口，须新建并 grep 比对。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如赠品行在不同库存策略下 avgCost 实际取值 / 折扣价税分离在多档税率混合单据的实际税额偏差 / 看板 orgId 行级权限在 ErpOrgIsolationQueryTransformer 实际生效性等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-sales-state-machine.md`（赠品库存扣减 PASS）+ `2026-07-27-1949-...-order-to-cash-e2e.md`（无赠品/看板 finding）+ `2026-07-29-0430-...-code-quality.md`（P1-MA4-021 resolved），列明只补的需求视角差异（赠品 UI 标记缺口 / 价税分离缺口复用 P1-RC-022 / AR 账龄四桶缺口 / 看板行级权限复用 P1-MA2-093 / 阈值配置化正面验证）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P2-RC-xxx`（#2/#3 若成立）入对应分区；既有行（P1-RC-022/P1-MA2-093/P2-RC-018）追加 RC 复核注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding（若有）已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03ba8ed35ffewo1WBPHpkQLu2f，fresh session，未起草本计划）。规则 1-13 全 PASS：(1) Deps A1.21=0.2 done；(2) 单结果表面（A1.21 报告 UC-SAL-08/12）；(3) 格式 + 命名合规（N=1 = sales-F4，在 N=2 assets-F1/N=3 assets-F2 之前）；(4) UC 覆盖精确（baseline-inventory:355 = UC-SAL-08/12 ✅一致）；(5) Baseline 8/8 spot-check 全 CONFIRMED——`ErpSalPricingRuleEngine.addGiftLine:203-217` unitPrice=0/quantity/amount=0/pricingSource=PROMOTION/remark="赠品行" / `ErpSalDashboardBizModel.findArOverdueAlert` 用 AppConfig.var 且仅扁平 ageDays 无 4 桶 / `ErpSalOrderLine` 无 isGift/lineType 列（grep 0 匹配）/ `recomputeLineAmount:172-179` 仅 setAmount 无 setTaxAmount（P1-RC-022 同根因）/ `DeliveryStockMoveBuilder.buildLines` 全行扣减无赠品过滤 / arm-index P1-RC-022(:132)/P1-MA2-093(resolved R1.29)/P2-RC-018(:135) 均存在且特征正确 / ui-patterns.md:11,36,116 行级赠品开关 + dashboards.md:60 四桶要求均存在 / erp-sal.action-auth.xml:98-107 sal-dashboard-main useCases="UC-SAL-12"；(6) 方法论 §1-§10 + §去重 + §4 三判据 + §7 reuse 对齐；(7) 反松弛；(8) typing；(9) Closure Gates audit-only 有据；(10) Non-Goals 守约；(11) Q4 正确——复用 finding（P1-RC-022/P1-MA2-093）保留 P1 不静默降级，修复全移交 MR0/MR1。无阻塞。Non-blocking（已评估，无需修订）：类引用未带包路径（ErpSalConstants 在 service/、ErpSalDeliveryProcessor 在 processor/），不影响复核；"倾向 P2"措辞被 Phase 1 Decision + Exit Criteria "明确分级非悬空" 锁定，非 anti-slack 违规。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.21 报告 9 段齐全 + 2 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.21 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；触及会计过账逻辑（价税分离重算）或 ORM 结构（补 isGift 列）的修复行须 ask-first + 独立 plan-audit（§5）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: A1.21 sales-F4 赠品与看板五级追踪审计完成。报告 `docs/audits/2026-08-03-0900-rc-ma1-a1-21-sales-f4-gift-dashboards.md` 9 段齐全，2 UC（UC-SAL-08/12）逐 UC 一矩阵行 L1-L5 全部填齐，5 候选缺口均有明确分级（#1 价税分离 reuse P1-RC-022 [P1]/#2 赠品行 UI 显式标记 P2-RC-023 新建 [P2 watch-only]/#3 AR 账龄 4 桶 P2-RC-024 新建 [P2 watch-only]/#4 行级权限 reuse P1-MA2-093 [resolved R1.29]/#5 pricing 冒烟 reuse P2-RC-018 [P2 watch-only]）。arm-index 已更新：2 新 P2 finding + 3 reuse 交叉引用注记 + A1.21 报告清单条目 + A1.21 RC 交叉引用注记段。§8 实际运行 `nop-compliance-checker.sh`，全 16 规则 actual ≤ baseline（精确匹配 0 漂移）。roadmap A1.21 状态 ready → done。P0 即时通道未触发（最高级 = P1，GL 平衡不破坏）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，未执行本计划）
- Evidence: 独立 closure-audit walkthrough — 12 项证据全 PASS：(1) 计划状态 completed + 两 Phase completed + 全部 `[ ]`→`[x]` + 8 Closure Gates 全勾 + Closure 段已填；(2) 报告 9 段齐全（§9 前置段 + §1-§8）；(3) L1 逐字引用 UC-SAL-08（use-cases.md:180-197）+ UC-SAL-12（:265-282）与实仓一致；(4) L3 行号实测核对——ErpSalPricingRuleEngine.addGiftLine:203-217(unitPrice=ZERO:210/quantity:211/amount=ZERO:212/pricingSource=PROMOTION:213/remark="赠品行":214) / DeliveryStockMoveBuilder.buildLines:54-67(全行扣减无赠品过滤) / ErpSalOrderBizModel.recomputeLineAmount:172-179(仅setAmount无setTaxAmount)+recomputeOrderTotals:181-197(:187复用陈旧taxAmount) / ErpSalDashboardBizModel.findArOverdueAlert:170-209(扁平ageDays无4桶) / ErpSalConstants:80-84(默认0/ZERO) / InvPostingDispatcher.buildEvent:181-221(totalCost=Σledger.totalCost)+InvAcctDocProvider.createFacts:81-85(SALES_OUTPUT Dr 6401/Cr 1401) / app-erp-sales.orm.xml:1125-1127(giftMaterialId/SkuId/Quantity)+:396-408(无isGift/lineType列,grep prod 0命中) / erp-sal.action-auth.xml:98-107(useCases="UC-SAL-12")；(5) L4 断言强度核对——TestErpSalPricingRuleEngine#testGiftLine:91-110(强:2lines+unitPrice=ZERO+quantity=ONE+pricingSource=PROMOTION) / TestErpSalDashboard 6方法全强(数值精确匹配) / TestErpSalPricingEndToEnd#testScenario4_Gift:89-101(仅冒烟status==0) / E2E sales.value.spec.ts(严格相等 salesAmount=1000/orderCount=1/invoiceCount=1)；(6) §8 checker 实测全 16 规则 actual≤baseline 精确匹配(R1d=14/R2a=34/R2b=229/R2c=1382/R2d=34/R3=5/R6=2/R10=6/R12a=69/R12b=66/R12c=40/其余=0)；(7) arm-index 更新齐——A1.21报告条目:89+P2-RC-023:147+P2-RC-024:148+P1-RC-022:133/P2-RC-018:136/P1-MA2-093:338 reuse注记+A1.21 RC摘要段:164；(8) roadmap A1.21 行 ready→done；(9) §6.1 5 候选缺口 grep 基据全记录(复用3+新建2 grep 零命中)；(10) 8 Closure Gates 全勾含独立性门(本审计=独立 fresh session)；(11) git status 仅 docs/ 变更,无 ORM/Java/api.xml/view.xml/真相源改动；(12) P0 未触发(§7:248+附录:299 显式声明,最高=P1 reuse,GL平衡不破坏)。无 blocker。残留风险:§9 前置段排序(方法论允许);未触及修复实施(全 MR0/MR1 移交)。

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围（见 Deferred But Adjudicated）
