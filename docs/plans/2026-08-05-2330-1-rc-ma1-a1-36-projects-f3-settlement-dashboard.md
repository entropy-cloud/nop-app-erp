# 2026-08-05-2330-1 rc-ma1-a1-36-projects-f3-settlement-dashboard projects 域 projects-F3 结算与看板需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Mission: requirement-compliance
> Work Item: A1.36（MA1 需求追踪矩阵审计 — projects-F3 损益汇总 / 竣工结算与质保金 / 结算转固 / 项目看板）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.36
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.36 的 0.2 依赖）、`2026-08-05-2200-2-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`（projects-F1 done，成本归集为损益汇总的输入前置）、`2026-08-05-2200-3-rc-ma1-a1-35-projects-f2-budget-dag.md`（projects-F2 done，预算控制与本切片并行）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.36 给出 UC 清单 = `UC-PRJ-06/07/08/10`（4 UC），含 `use-cases.md:96/115/131/167` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。本切片为 projects 域第三个也是最后一个 RC 切片（projects 域共 3 切片 A1.34/A1.35/A1.36，本切片完成后 projects 域 MA1 全覆盖）。

- **L1 需求契约（权威真相源）**：`docs/design/projects/use-cases.md`（机制见 `profitability.md`、`../dashboards.md`）：
  - **UC-PRJ-06 项目损益汇总**（`:96`）：定时任务(nop-job) → 聚合 收入=Σ Billing.amountFunctional / 成本=Σ CostCollection(按人工/物料/费用/分包分类) / 毛利=收入-成本 / 毛利率=毛利/收入；生成 ProjectPnl 记录；**多币种折算到统一币种**。
  - **UC-PRJ-07 竣工结算与质保金**（`:115`）：项目→COMPLETED；基于最新 ProjectPnl 生成 ProjectSettlement(FINAL)；最终结算收入/成本/损益；**质保金(retentionAmount)留存,到期返还**。
  - **UC-PRJ-08 项目结算转固**（`:131`）：ProjectSettlement(settlementType=CLOSE, transferToAsset=true) → 调用 IErpAstAssetBiz 生成资产卡片(资产域) → 生成转固凭证(经 finance IErpFinAcctDocProvider 注册 PROJECT_SETTLEMENT) → 资产卡片.来源项目 == 本项目。
  - **UC-PRJ-10 项目看板**（`:167`）：KPI 卡片值 == 实时聚合(按期间/orgId/权限过滤) 在手项目/预算/已发生成本/预算执行率/毛利率/超支·延期预警；预警触发=满足阈值条件的记录(阈值来自系统配置非硬编码)；看板数据受行级权限约束。
  - **L1 关键不变量**：① 损益汇总**定时任务**触发 + **多币种折算**；② 结算质保金**留存+到期返还**；③ 转固跨域建资产卡片 + 转固凭证；④ 看板 KPI 实时聚合非硬编码 + 阈值来自配置 + 行级权限。

- **L3 代码实现现状（实测）**——结算/转固/看板完整实现，损益聚合缺**调度接线 + 多币种**，质保金**逻辑层全缺**：
  - **UC-PRJ-06 项目损益汇总（⚠️ 聚合引擎完整但调度未接线 + 多币种未实现）**：
    - `ProjectPnlCalculator.refreshPnl:57-132`（`module-projects/erp-prj-service/.../pnl/ProjectPnlCalculator.java`）：聚合收入（读 `Billing.amountFunctional` 过滤 businessDate，`:144-161`）+ 成本四分类（LABOR/MATERIAL/EXPENSE/SUBCONTRACT，`:163-194`）+ `totalCost/grossProfit/grossMarginPct`（`:86-88`，margin helper `:262-268`）+ 承诺成本/EAC（`:90-93`）+ 幂等（posted=true 冻结重算抛 `ERR_PRJ_PNL_RECALC_FROZEN`，`:78-82,247-260`）。
    - **⚠️ 调度未接线**：config key `erp-prj.pnl-calc-cron`（`ErpPrjConstants.java:20`）+ `erp-prj.pnl-auto-calc-enabled`（默认 false，`ErpPrjConstants.java:22` + `module-projects/erp-prj-meta/module-meta.yaml:14-16`）**已声明但全 `module-projects` 零 nop-job / 零 cron 注册 / 零 scheduler config**（`find` `job*`/`*.task.xml` 无果，grep `cron`/`schedule` 仅 AMIS 注释）。batch task `prj.pnl-calc.batch.xml` 存在（`:1-25`，loader 过滤 status∈[DRAFT,OPEN,ON_HOLD]，processor 调 refreshPnl）但仅手动可调；`IErpPrjProjectPnlBiz.java:20` 注释"经 nop-job 周期触发"为**期望非实现**。**UC-PRJ-06"定时任务(nop-job)"前置未落地——损益汇总无自动触发路径**。
    - **⚠️ 多币种未实现**：`ProjectPnlCalculator.java:105` 硬编码 `exchangeRate = BigDecimal.ONE`；Javadoc `:47-48` 显式声明"多币种基线：聚合使用 amountFunctional（本位币）… exchangeRate=1。跨币种 rollup 精度归 successor"。**UC-PRJ-06"多币种折算到统一币种"未实现**。
  - **UC-PRJ-07 竣工结算与质保金（⚠️ 结算完整，质保金逻辑层全缺）**：
    - 结算引擎完整：`ErpPrjProjectSettlementBizModel.java:28-91`（createSettlement/submit/approve/reject/cancel/reverseSettlement 全状态机）+ `ErpPrjProjectSettlementCreateSettlementProcessor.java:26-56`（要求 PnL 快照抛 `ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING` + 置 finalRevenue/finalCost/finalProfit + CLOSE 置 transferToAsset）+ 行生成器 `ErpPrjProjectSettlementProcessor.java:149-172`（INCOME 行 from Billing + COST 行 from CostCollection）+ 状态迁移校验 `:91-148`。
    - **⚠️ 质保金逻辑层全缺**：`retentionAmount`(propId 16) + `retentionDueDate`(propId 17) **schema 存在**（`_ErpPrjProjectSettlement.java:340-344` + `model/app-erp-projects.orm.xml:803-804`）但 grep 全 `module-projects/erp-prj-service/src/main` `setRetention|getRetention|Retention|RETENTION` 在 `.java` 源码**零命中**（仅 `_cases/` CSV 头）。**质保金字段是惰性 schema——UC-PRJ-07"质保金(retentionAmount)留存,到期返还"在业务逻辑层完全未实现**：createSettlement 不填、无留存工作流、无到期返还路径。
  - **UC-PRJ-08 项目结算转固（✅ 完整实现）**：
    - CLOSE 分支：`ErpPrjProjectSettlementApproveProcessor.java:30-33`（settlementType=CLOSE && transferToAsset=true && assetCardId==null → createAndActivateAsset）。
    - 跨域建资产卡片：`ErpPrjProjectSettlementProcessor.java:174-194`（注入 `IErpAstAssetBiz assetBiz:59` + 构建 data map[code/name/orgId/acquisitionDate/originalValue=finalCost/currentValue=finalCost/residualValue=0/status=IN_SERVICE] `:176-184` + `assetBiz.save(data, context):186` + 回写 `settlement.assetCardId:188` + 失败抛 `ERR_SETTLEMENT_CAPITALIZATION_FAILED:191-192`）。
    - 红冲回退资产：`ErpPrjProjectSettlementProcessor.java:196-206`（资产 status 回 DRAFT）+ `ErpPrjProjectSettlementReverseSettlementProcessor.java:21-37`（要求 posted=true 否则 `ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION` + postingDispatcher.reverse + rollbackAssetIfNeeded）。
    - PROJECT_SETTLEMENT 过账：`ProjectSettlementAcctDocProvider.java:52-100`（CLOSE 分支 Dr 1601 固定资产/Cr 1603 在建工程 `:67-76`；FINAL/INTERIM 分支 Dr 5101 + Dr/Cr 4103 / Cr 6001 `:77-100`；facts 标 projectId）。
  - **UC-PRJ-10 项目看板（✅ 完整实现）**：
    - `ErpPrjDashboardBizModel.java`（bean `app-service.beans.xml:43`）：`getDashboardKpi:62-81`（openProjectCount/totalBudget/incurredCost/executionRate=incurredCost/totalBudget，源 ErpPrjBudget.totalAmount + ErpPrjCostCollection.totalAmount）+ `getProjectStatusDistribution:84-106`（DB GROUP BY）+ `findCostOverrunAlert:110-135`（cost>budget，硬上限 ALERT_MAX_ROWS=5000）+ `findDelayedProjectAlert:139-163`（endDate<today && status!=COMPLETED，算 overdueDays）+ `getProjectGrossMargin:173-205`（聚合 ErpPrjProjectPnl Σrevenue/Σcost/ΣgrossProfit/grossMarginPct=Σprofit/Σrevenue）。
  - **跨域 daoFor**：projects 跨域只读归 P1-MA1-022 todo MR1，本切片不重审。本切片范围外的新 daoFor 站点（`ErpPrjProjectSettlementProcessor.java:200` daoFor(ErpAstAsset) 回退只读 + `ExpenseCostAggregator.java:165` daoFor(ErpFinExpenseClaimLine)）归既有 finding 不重审。

- **L4 测试证据现状**（`module-projects/erp-prj-service/src/test/java/`）：
  - `TestErpPrjProjectPnl.java`（4 @Test：收入+四分类成本算术 `:67-106` / 空项目 `:109-129` / 非法期间抛错 `:132-148` / 幂等重算 `:151-172`）——**覆盖手动 refreshPnl 主路径**。
  - `TestErpPrjProjectSettlement.java`（4 @Test：FINAL approve+post `:83-112` / CLOSE 转固 `:115-140` / 非法迁移 `:143-159` / 红冲凭证+资产 `:162-191`）——**覆盖结算+转固主路径**。
  - `TestErpPrjDashboard.java`（5 @Test）+ `TestErpPrjDashboardGrossMargin.java`（4 @Test）——**覆盖看板 KPI + 预警 + 毛利率**。
  - E2E：`tests/e2e/business-actions/projects-settlement-posting.action.spec.ts`（CLOSE 链 + 凭证行 Dr1601/Cr1603 + 红冲 `:58-184`）+ `projects-pnl-settlement.action.spec.ts`（FINAL/INTERIM 转账凭证行 `:141+, :220+`）+ `tests/e2e/dashboards/projects.value.spec.ts`（getDashboardKpi + getProjectGrossMargin GraphQL 值断言）。
  - **⚠️ 测试缺口**：① 质保金**零测试**（与逻辑缺失一致）；② PnL **自动调度**零测试（与调度未接线一致）；③ 多币种折算零测试（exchangeRate=ONE 单币种路径）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`（A2.13）：项目 5 态 + 任务 4 态 + DAG + 工时审批轴 + **项目结算三轴**状态机主路径守卫齐全（startProject/closeProject/cancelProject + 结算 Processor 链标准审批三轴 + PnL ProjectPnlCalculator 写 CALCULATED）；P1-MA2-069（dict 死状态）resolved。**P1-MA2-068**（tryPost 吞异常悬挂）resolved R1.16。
  - **本切片须声明与 MA2 报告差异增量**（报告段落 9）：复用 A2.13 项目结算三轴 + PnL 状态机已证实行为，只补需求视角差异（质保金逻辑缺失 / 调度未接线 / 多币种未实现 / 看板行为验收）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-068`（tryPost 悬挂 resolved）、`P1-MA2-069`（dict 死状态 resolved）、`P1-MA1-010`（多币种 propId todo MR1——**与 UC-PRJ-06 多币种折算同域同主题**，复用 or 新增须 §7 裁决）、`P1-MA1-022`（跨域只读 todo MR1）。projects 域已有 RC finding：A1.34 产 P1-RC-048/049/050 + P2-RC-048；A1.35 产 P1-RC-051 + P2-RC-049。**最新 RC 编号 = P1-RC-051 / P2-RC-049**（本切片新 finding 续编）。本切片须 grep arm-index prj settlement/retention/质保金/pnl/cron/dashboard/multi-currency/exchangeRate 同域同控制点后裁决。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1。本切片候选偏差（质保金逻辑缺失 / 调度接线缺失）属**代码逻辑**类（预授权）。多币种折算若修复触及 `ProjectPnlCalculator` 汇率逻辑属代码逻辑预授权；若触及 ORM（加汇率载体字段）须 ask-first。

- **剩余差距**：A1.36 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源。本计划产出 A1.36 报告并登记 finding，解除 projects 域最后一个切片证据缺口。

## Goals

- 产出 A1.36 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`，含方法论 §6 **9 段全部内容**。
- 对 UC-PRJ-06/07/08/10 逐条核验**每条验收标准**（完整枚举，§3）：损益汇总（聚合算术/定时任务/多币种折算）、结算（FINAL 生成/最终收入成本损益/质保金留存返还）、转固（CLOSE→资产卡片/转固凭证/来源项目回链）、看板（KPI 实时聚合/阈值来自配置/行级权限）全链逐条。
- 对候选缺口给出分级结论：①UC-PRJ-06 **定时任务调度未接线**（pnl-calc-cron/pnl-auto-calc-enabled 声明但零 nop-job 消费方）倾向 **P1**（§4 三判据复核 profitability.md 是否显式 documented simplification Deferred + 人工批准）；②UC-PRJ-06 **多币种折算未实现**（exchangeRate=ONE）倾向 **P1/P2**（与 P1-MA1-010 同域同主题，须 §7 复用 or 新增裁决 + §4 三判据）；③UC-PRJ-07 **质保金(retentionAmount)逻辑层全缺**（schema 存在但零 service 代码读写 + 零返还工作流）倾向 **P1**（§4 三判据复核 profitability.md §结算 是否显式 Deferred + 人工批准）；④UC-PRJ-08 转固 + UC-PRJ-10 看板 → 倾向**接受**（完整实现 + 强测 + E2E 值断言）——按 §2 判据定级，若为 P0/P1 则新建 `P*-RC-xxx`（续编 P1-RC-052+ / P2-RC-050+）并按 §10 触发 MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；新 audit reports 表行）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases.md/profitability.md/dashboards.md/product-scope.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.34 projects-F1 立项/成本归集 / A1.35 projects-F2 预算/DAG 独立 plan done；A1.36 只覆盖 UC-PRJ-06/07/08/10）。
- **不复审项目立项/工时成本/多来源归集/预算/DAG**（UC-PRJ-01/02/03/04/05/09 属 A1.34/A1.35；本切片仅核损益汇总/结算/转固/看板）。
- **不重审 P1-MA2-068/069 结算状态机行为**（§去重协议：resolved，只补需求视角差异[质保金/调度/多币种]）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.36 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.36 UC 锚点）+ `docs/design/projects/use-cases.md`（L1 真相源）+ `docs/design/projects/profitability.md` + `../dashboards.md`（L2 设计参考，非真相源）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 A2.13 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-projects/erp-prj-service -Dtest=TestErpPrjProjectPnl,TestErpPrjProjectSettlement,TestErpPrjDashboard,TestErpPrjDashboardGrossMargin`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`（产出 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-PRJ-06/07/08/10 **逐验收标准一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:96/115/131/167` 验收标准原文；L2 引用 `profitability.md §损益/§结算` + `../dashboards.md §项目看板`（标注"设计参考，冲突以 L1 为准"）；L3 引用 `ProjectPnlCalculator`/`ErpPrjProjectSettlementBizModel`/`ErpPrjProjectSettlementCreateSettlementProcessor`/`ProjectSettlementAcctDocProvider`/`ErpPrjProjectSettlementApproveProcessor`/`ErpPrjDashboardBizModel`（含行号）；L4 引用 `TestErpPrjProjectPnl`/`TestErpPrjProjectSettlement`/`TestErpPrjDashboard`#method（注明断言强度）；L5 复用 A2.13（结算三轴/PnL 状态机 PASS + P1-MA2-068/069 resolved）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：UC-PRJ-06 ①**定时任务触发**（`prj.pnl-calc.batch.xml` 存在但 grep 全 `module-projects` 零 nop-job/cron/scheduler config + config key `erp-prj.pnl-calc-cron`/`erp-prj.pnl-auto-calc-enabled`(默认 false) 零消费方——**❌ 调度未接线**）②**多币种折算**（`ProjectPnlCalculator.java:105` 硬编码 exchangeRate=ONE，Javadoc `:47-48` 显式"跨币种 rollup 归 successor"——**❌ 多币种未实现**）③聚合算术 + 四分类 + 毛利率（`refreshPnl:57-132` 收入/成本/毛利/毛利率 + 承诺成本/EAC ✅）；UC-PRJ-07 ①FINAL 结算（createSettlement + finalRevenue/Cost/Profit ✅）②**质保金留存+到期返还**（schema retentionAmount/retentionDueDate 存在但 grep 全 erp-prj-service/src/main `.java` 零 setRetention/getRetention——**❌ 逻辑层全缺**）；UC-PRJ-08 转固（CLOSE→IErpAstAssetBiz.save 建卡片 + PROJECT_SETTLEMENT 凭证 Dr1601/Cr1603 + 来源项目 assetCardId 回链 ✅）；UC-PRJ-10 看板（KPI 实时聚合 + 预警阈值 + 硬上限 ALERT_MAX_ROWS=5000 ✅——行级权限为平台层本切片仅注记）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对 4 UC 给出符合性结论（取最高）：UC-PRJ-06 → 聚合算术**接受** + 定时任务调度缺失倾向 **P1**（§4 三判据复核 profitability.md 是否显式 documented simplification Deferred + 人工批准痕迹[plan-audit/owner doc 标注/product-scope 裁剪]）+ 多币种折算缺失倾向 **P1/P2**（须 §7 裁决与 P1-MA1-010 同域同主题复用 or 新增 + §4 三判据）；UC-PRJ-07 → FINAL 结算**接受** + 质保金逻辑缺失倾向 **P1**（§4 三判据复核 profitability.md §结算 是否显式 Deferred + 人工批准——质保金是竣工验收标准，schema 存在说明需求已识别但逻辑未实现）；UC-PRJ-08 转固 → 倾向**接受**（完整实现 + 强测 + E2E 值断言 Dr1601/Cr1603）；UC-PRJ-10 看板 → 倾向**接受**（KPI 实时聚合 + 预警 + 强测 + E2E GraphQL 值断言）。每结论须列明命中判据编号 + 三源对照 + §4 三判据复核（P1 项核 plan-audit/owner doc documented simplification/product-scope 裁剪 + 人工批准痕迹）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-PRJ-06/07/08/10 矩阵行（逐验收标准进入 L5 判读），L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.13 来源
- [x] 4 UC 各有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 ①-③ 有明确分级；调度缺失 + 多币种 + 质保金缺失各有 §4 三判据复核路径；转固/看板接受结论成立

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` prj settlement/retention/质保金/pnl/cron/schedule/dashboard/multi-currency/exchangeRate/rollup 同域同控制点后裁决——质保金逻辑缺失为**新根因**（既有 arm-index 无 RC finding 涉及 projects 质保金 retentionAmount 业务逻辑）→ 新建 `P1-RC-052`；调度未接线为**新根因**（P1-MA2-068 是 tryPost 吞异常悬挂不同控制点）→ 新建 `P1-RC-053`；多币种折算缺失须与 `P1-MA1-010`（projects 多币种四件套 propId 缺失）§7 裁决——同域同主题（多币种）但**不同控制点**（P1-MA1-010 = ORM 元数据 propId 缺失；本缺口 = 聚合引擎汇率逻辑 exchangeRate=ONE）→ 新建 `P2-RC-050`（与既有 RC 系列协调，续 A1.35 P1-RC-051/P2-RC-049）。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR1）。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（SP-1 多币种项目 exchangeRate=ONE 在 ProjectPnl 实际金额偏差 / SP-2 pnl-auto-calc-enabled=true 时 batch 是否可手动触发调度路径 / SP-3 质保金 schema 字段在 delta/未来定制消费的运行时行为 / SP-4 CLOSE 转固 IErpAstAssetBiz.save data map 非专用 API 的契约鲁棒性 / SP-5 看板 getProjectGrossMargin Σprofit/Σrevenue 聚合口径运行时行为；每存疑点一行）。**P0 即时通道未触发**（本切片无 P0——质保金缺失不破坏活跃数据/会计正确性 + 调度缺失仅影响自动化 + 多币种影响精度但 GL 借贷平衡不受影响）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**（无生产代码变更，注明"无回归风险"）。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-1020-arm-ma2-projects-state-machine.md`（A2.13 项目结算三轴 + PnL 状态机 PASS + P1-MA2-068/069 resolved），列明只补的需求视角差异（质保金逻辑缺失 / 调度未接线 / 多币种未实现 / 看板行为验收）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P1-RC-052` + `P1-RC-053` + `P2-RC-050` 入 RC 发现追踪分区；audit reports 表新增 A1.36 行。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据
- [x] 新 RC finding（P1-RC-052 + P1-RC-053 + P2-RC-050）已写入 `arm-index.md`；静态存疑点清单已登记（SP-1~SP-5 供 A4.1/A4.2 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_02fb14a54fferkXVjAFY3DGLVd，fresh session，未起草本计划）。全部 load-bearing 引用经实仓复核 CONFIRMED TRUE：①A1.36 UC 锚点 UC-PRJ-06/07/08/10 ✅ 一致（inventory:370 + roadmap:75）；②`ProjectPnlCalculator:105` 硬编码 `exchangeRate=BigDecimal.ONE` 行号精确；③全 `module-projects` nop-job/cron/scheduler 零注册（仅 ErpPrjConstants:19-20 config key 声明 + IErpPrjProjectPnlBiz:20 Javadoc"期望非实现"）；④retentionAmount/retentionDueDate schema 存在（orm.xml:803-804）+ grep erp-prj-service/src/main `setRetention|getRetention|Retention` **零命中**；⑤结算/转固/看板文件全存在；⑥最新 RC = P1-RC-051/P2-RC-049，arm-index grep `P1-RC-05[2-9]|P2-RC-05[0-9]` 空 → 052/053/050 可用；⑦A2.13 报告存在；⑧deps=0.2 done。scope（UC-PRJ-06/07/08/10 only，无 A1.34/A1.35 creep）、anti-slack（零禁词）、methodology §1-§9 + §4 三判据 + §去重协议 reuse A2.13 全对齐；Closure Gates 只读审计删除 build/test 门控有据。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐验收标准覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.36 报告 9 段齐全 + UC-PRJ-06/07/08/10 矩阵行（逐验收标准）+ finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议一致；与 rc-requirement-baseline-inventory A1.36 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

> **执行说明（read-only 审计，无生产代码变更）**：本计划为只读审计，产出 = A1.36 审计报告 `docs/audits/2026-08-05-2330-1-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`（9 段齐全）+ arm-index 更新（新 P1-RC-052 + P1-RC-053 + P2-RC-050 + audit reports 表 A1.36 行 + RC 交叉引用注记）。Closure Gates 按本计划声明删除 build/test 门控（无代码变更故不跑）；§8 已附 checker actual vs baseline 实测表（R2c/R2d +2 delta 系其他在途工作非本审计引入——本审计仅新增报告 + 更新 docs，零 `.java`/`orm.xml`/`api.xml`/`view.xml` 生产变更）。唯一剩余 `[ ]` = 独立结束审计门控，须由独立子代理（新会话）执行，执行者不得自我勾选。

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；本切片候选偏差（质保金逻辑 / 调度接线 / 多币种）属**代码逻辑**类（预授权——ProjectPnlCalculator 汇率逻辑 / 结算 Processor 质保金 setter / nop-job 注册；多币种若触及 ORM 加汇率载体字段须 ask-first）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行）

## Closure

Status Note: 执行完成（2026-08-05）。A1.36 审计报告 `docs/audits/2026-08-05-2330-1-rc-ma1-a1-36-projects-f3-settlement-dashboard.md` 已落盘（9 段齐全），arm-index 已更新（新 P1-RC-052[质保金逻辑层全缺] + P1-RC-053[nop-job 调度未接线] + P2-RC-050[多币种 exchangeRate=ONE] + audit reports 表 A1.36 行 + RC 交叉引用注记）。projects 域 MA1 三切片（A1.34/A1.35/A1.36）全部完成，projects 域 MA1 全覆盖。唯一剩余 Closure Gate = 独立结束审计（须独立子代理新会话执行）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，fresh session，未执行本计划任何阶段，未重用执行者上下文）。审计会话 = 本 MISSION_DRIVER 任务的独立 closure-audit 步骤。
- Evidence: 实仓复核全部 load-bearing 声明 CONFIRMED：
  - **交付物落地复核**：①审计报告 `docs/audits/2026-08-05-2330-1-rc-ma1-a1-36-projects-f3-settlement-dashboard.md` 实存（289 行），§1-§9 + 段落完整性自检 + 真相源冻结条款声明齐全（`grep '^## ' ` 9 段标题命中）；②arm-index 已更新——`grep 'P1-RC-052\|P1-RC-053\|P2-RC-050'` 在 `docs/audits/arm-index.md` 命中 3 finding 行 + audit reports 表 A1.36 行 + RC 交叉引用注记段；③`docs/logs/2026/08-05.md` 含本切片 DOCS 条目（line 3-10，覆盖结论 + 3 finding + roadmap done）。
  - **五级追踪矩阵真实复核**：§5 矩阵 4 UC（UC-PRJ-06/07/08/10）均有 L1 逐字引用（`use-cases.md:96/115/131/167`）+ L3 行号引用 + L4 测试断言强度 + L5 复用 A2.13 来源，结论分级（接受 on 主断言 + P1[质保金/调度] + P2[多币种]）与方法论 §2 判据一致。
  - **finding 真实性复核**：P1-RC-052（质保金 retentionAmount schema 存在但 service 零 writer）、P1-RC-053（nop-job 调度 config key 声明但零消费方）、P2-RC-050（exchangeRate=ONE 硬编码）三条根因均经执行计划 Current Baseline 段引用的实仓 grep 路径复核一致，非空壳。
  - **Anti-Hollow 复核**：本计划为只读审计，结果表面 = 报告 + arm-index 登记，无生产代码需接线；报告内容含具体行号 + grep 命中 + §4 三判据复核路径，非占位填充。
  - **§7 静态存疑点** SP-1~SP-5 实存（report line 241-245），每条含展开方式，供 MA4 展开。
  - **§8 过程纪律自检** 含 checker actual vs baseline 实测表（R1a~R8 全行）+ 独立性声明 + 交叉去重声明；R2c/R2d +2 delta 已声明"非本审计引入"（只读审计零生产代码变更）。
  - **Deferred honesty**：`Deferred But Adjudicated` 段唯一项 = finding 修复实施，Classification = `out-of-scope improvement`，理由正确（审计计划结果表面不含修复，修复经 MR0/MR1），非隐藏缺陷降级。
  - **文本一致性**：Plan Status `completed` ↔ Phase 1/2 Status `completed` ↔ 两阶段 Exit Criteria 全 `[x]` ↔ Closure Gates 全 `[x]`（含本审计门控已勾选）↔ logs 条目一致。
  - **checker 复核**：重跑 `node ../attractor-guided-engineering-template/tools/mission-driver/src/plan-check.mjs <plan> --strict` → `passed: true`。
