# A1.36 projects-F3 结算与看板 需求-实现符合性审计报告（MA1 RC）

> 里程碑：MA1（requirement-compliance mission，Work Item A1.36）
> 域/功能切片：projects / 损益汇总 / 竣工结算与质保金 / 结算转固 / 项目看板
> UC 清单：UC-PRJ-06 / UC-PRJ-07 / UC-PRJ-08 / UC-PRJ-10（4 UC）
> 来源：plan `docs/plans/2026-08-05-2330-1-rc-ma1-a1-36-projects-f3-settlement-dashboard.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）
> L1 锚点：`docs/audits/rc-requirement-baseline-inventory.md` A1.36 → UC-PRJ-06/07/08/10（✅ 一致）
> 审计类型：只读审计（无生产代码/ORM/api.xml/view.xml/真相源变更）
> 产出时间：2026-08-05

---

## 9. 与 MA2 报告差异增量声明（前置）

本切片报告与既有 MA2 行为审计报告 `docs/audits/2026-07-28-1020-arm-ma2-projects-state-machine.md`（A2.13 项目状态机：项目 5 态 + 任务 4 态 + DAG + 工时审批轴 + **项目结算三轴** + PnL 2 态）的差异增量，按 §去重协议声明：

- **复用 A2.13 已证实行为作为 L5 既有证据**（不重新核实行为本身）：
  - **项目结算三轴状态机 PASS**（A2.13 §2.6 + §场景 G）：`ErpPrjProjectSettlement` 经 Processor 链（SubmitForApproval/Approve/Reject/Cancel + CreateSettlement + ReverseSettlement）完整覆盖审批轴 + posted 轴，状态迁移守卫齐全，非法迁移全抛 `ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION`。
  - **PnL calcStatus 2 态 PASS**（A2.13 §2.7）：PENDING 经 codegen 默认值承载 + CALCULATED 经 `ProjectPnlCalculator:122 setCalcStatus(CALCULATED)`，2 态全可达。
  - **`P1-MA2-068`（TimesheetPostingDispatcher tryPost 吞异常悬挂）resolved**（R1.16 done，方案 A 实现 + 告警派发 genuine implementation，A1.34 HEAD 复核无回退）。
  - **`P1-MA2-069`（Milestone/Billing/CostCollection dict drift + CRUD 桩死状态）resolved**（R1.21 done，方案 B Deferred，A1.34 复核维持 resolved，§去重协议不重审 audit-remediation 文本一致性维度）。
- **本切片只补需求视角差异**（use-case 验收标准 vs 实际行为）：
  - **UC-PRJ-06 定时任务调度未接线**（nop-job 消费方零）
  - **UC-PRJ-06 多币种折算未实现**（exchangeRate=ONE 硬编码）
  - **UC-PRJ-07 质保金逻辑层全缺**（retentionAmount schema 存在但零 service writer + 零返还工作流）
  - **UC-PRJ-08 转固 / UC-PRJ-10 看板行为验收**（L1 验收标准逐条对照，倾向接受）

本切片不重审 A2.13 已证实的状态机行为，仅从 L1 验收标准视角补齐需求契约↔行为差异。

---

## 1. 需求契约原文（L1，逐字引用，禁止转述）

> 真相源：`docs/design/projects/use-cases.md`（权威功能契约）。L2 owner doc（`profitability.md` / `../dashboards.md`）为设计参考，冲突以 L1 为准（§4 Q1）。

### UC-PRJ-06 项目损益汇总（`use-cases.md:96`）

```
定时任务(nop-job) →
  聚合: 收入 = Σ Billing.amountFunctional
  成本 = Σ CostCollection(按人工/物料/费用/分包分类)
  毛利 = 收入 - 成本
  毛利率 = 毛利 / 收入
生成 ProjectPnl 记录
多币种折算到统一币种
```

**验收标准逐条枚举**：①定时任务(nop-job)触发 ②收入=Σ Billing.amountFunctional ③成本=Σ CostCollection 按人工/物料/费用/分包分类 ④毛利=收入-成本 ⑤毛利率=毛利/收入 ⑥生成 ProjectPnl 记录 ⑦多币种折算到统一币种。

### UC-PRJ-07 竣工结算与质保金（`use-cases.md:115`）

```
项目: → COMPLETED
基于最新 ProjectPnl 生成 ProjectSettlement(FINAL)
最终结算收入/成本/损益
质保金(retentionAmount)留存,到期返还
```

**验收标准逐条枚举**：①项目→COMPLETED ②基于最新 ProjectPnl 生成 ProjectSettlement(FINAL) ③最终结算收入/成本/损益 ④质保金(retentionAmount)留存 ⑤质保金到期返还。

### UC-PRJ-08 项目结算转固（`use-cases.md:131`）

```
ProjectSettlement(settlementType=CLOSE, transferToAsset=true) →
  调用 IErpAstAssetBiz 生成资产卡片(资产域)
  生成转固凭证(经 finance IErpFinAcctDocProvider 注册 PROJECT_SETTLEMENT)
资产卡片.来源项目 == 本项目
```

**验收标准逐条枚举**：①ProjectSettlement(CLOSE, transferToAsset=true) 触发 ②调用 IErpAstAssetBiz 生成资产卡片 ③生成转固凭证(经 IErpFinAcctDocProvider 注册 PROJECT_SETTLEMENT) ④资产卡片.来源项目 == 本项目。

### UC-PRJ-10 项目看板（`use-cases.md:167`）

```
// KPI 指标数据源正确(实时聚合, 非硬编码)
KPI 卡片值 == 对应实体的实时聚合(按期间/orgId/权限过滤)
  在手项目/预算/已发生成本, 预算执行率, 毛利率, 超支/延期预警

// 预警触发
预警项 == 满足阈值条件的记录(阈值来自系统配置, 非硬编码)

// 权限
看板数据受行级权限约束(只看自己组织/部门/成本中心)
```

**验收标准逐条枚举**：①KPI 卡片值==实时聚合非硬编码 ②指标项[在手项目/预算/已发生成本/预算执行率/毛利率/超支预警/延期预警] ③按期间/orgId/权限过滤 ④预警项==满足阈值条件的记录 ⑤阈值来自系统配置非硬编码 ⑥看板数据受行级权限约束。

---

## 2. 实现证据（L3 代码路径，含行号 + 跨域调用链）

### UC-PRJ-06 损益汇总聚合引擎
- `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/pnl/ProjectPnlCalculator.java`：
  - `refreshPnl:57-132` 主聚合入口：收入 `:84` + 四分类成本 `:85` + `totalCost:86` + `grossProfit:87` + `grossMarginPct:88` + 承诺成本/EAC `:90-93`；幂等（posted=true 冻结重算抛 `ERR_PRJ_PNL_RECALC_FROZEN:80-82`）。
  - 收入聚合 `sumRevenue:144-161`（读 `Billing.amountFunctional` 过滤 businessDate）。
  - 成本四分类 `sumCostByCategory:163-194`（LABOR/MATERIAL/EXPENSE/SUBCONTRACT）。
  - `grossMarginPct` 辅助 `marginPct:262-268`。
- **调度**：`IErpPrjProjectPnlBiz.java:20` 契约注释「经 nop-job（erp-prj-pnl-calc）周期触发」为**期望非实现**；config key `erp-prj.pnl-calc-cron`（`ErpPrjConstants.java:20`）+ `erp-prj.pnl-auto-calc-enabled`（`ErpPrjConstants.java:22` + `module-projects/erp-prj-meta/module-meta.yaml:14-16` feature flag + `ErpPrjConfigs.java:83` `AppConfig.var(...)` 读值）**已声明但全 `module-projects` 零 nop-job / 零 cron 注册 / 零 scheduler Job bean 消费**（grep `nop-job|IJobInvoker|JobBean|@Scheduled|IScheduler` 实证）。batch task `prj.pnl-calc.batch.xml`（`module-projects/erp-prj-service/src/main/resources/_vfs/nop/batch-task/prj/pnl-calc.batch.xml`，loader 过滤 status∈[DRAFT,OPEN,ON_HOLD]，processor 调 `IErpPrjProjectPnlBiz.refreshPnl`）存在但**仅手动可触发**。
- **多币种**：`ProjectPnlCalculator.java:105` `pnl.setExchangeRate(BigDecimal.ONE)` 硬编码；Javadoc `:47-48` 显式「多币种基线：聚合使用 amountFunctional（本位币）… exchangeRate=1。跨币种 rollup 精度归 successor」。

### UC-PRJ-07 竣工结算与质保金
- `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/entity/ErpPrjProjectSettlementBizModel.java:28-91`：状态机 Facade（createSettlement/submit/approve/reject/cancel/reverseSettlement 全委托 Processor）。
- `ErpPrjProjectSettlementCreateSettlementProcessor.java:26-56`：要求 PnL 快照（`:30` 缺失抛 `ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING`）+ 置 `settlementType:41` + `setTransferToAsset(CLOSE):48`。
- `ErpPrjProjectSettlementProcessor.java:149-172`：行生成器（INCOME 行 from Billing + COST 行 from CostCollection）+ 状态迁移校验 `:91-148`。
- 最终收入/成本/损益：`finalRevenue`/`finalCost`/`finalProfit` 经 `ProjectSettlementAcctDocProvider.java:59-60` 读取（BILL_DATA_FINAL_REVENUE/FINAL_COST）。
- **质保金**：`retentionAmount`（propId 16）+ `retentionDueDate`（propId 17）**schema 存在**（`module-projects/model/app-erp-projects.orm.xml:803-804` + `_ErpPrjProjectSettlement.java` 生成 getter/setter）但 grep 全 `module-projects/erp-prj-service/src/main` `.java` 源码 `setRetention|getRetention|Retention` **零命中**（仅 `_gen/_ErpPrjProjectSettlement.java` + api InputBean/OutputBean）。createSettlement Processor 不填 retentionAmount/retentionDueDate，无留存工作流，无到期返还路径。

### UC-PRJ-08 结算转固（跨域链全列）
- `ErpPrjProjectSettlementApproveProcessor.java:30-33`：`settlementType=CLOSE && transferToAsset=true && assetCardId==null` → `processor.createAndActivateAsset`。
- 跨域建资产卡片：`ErpPrjProjectSettlementProcessor.java:174-194`（注入 `IErpAstAssetBiz assetBiz:59` + 构建 data map[code/name/orgId/acquisitionDate/originalValue=finalCost/currentValue=finalCost/residualValue=0/status=IN_SERVICE] `:176-184` + `assetBiz.save(data, context):186` + 回写 `settlement.assetCardId:188` + 失败抛 `ERR_SETTLEMENT_CAPITALIZATION_FAILED:191-192`）。
- 红冲回退资产：`ErpPrjProjectSettlementProcessor.java:196-206`（资产 status 回 DRAFT）+ `ErpPrjProjectSettlementReverseSettlementProcessor.java:21-37`（要求 posted=true 否则 `ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION` + postingDispatcher.reverse + rollbackAssetIfNeeded）。
- PROJECT_SETTLEMENT 过账：`ProjectSettlementAcctDocProvider.java:52-100`（CLOSE 分支 Dr `1601` 固定资产/Cr `1603` 在建工程 `:67-76`；FINAL/INTERIM 分支 Dr `5101` + Dr/Cr `4103` / Cr `6001` `:77-100`；facts 标 `projectId`）。
- 来源项目回链：`ErpPrjProjectSettlement.assetCardId` + 资产卡片经 `IErpAstAssetBiz.save` data map 写入（assetCardId 回写 settlement）。

### UC-PRJ-10 项目看板
- `module-projects/erp-prj-service/src/main/java/app/erp/prj/service/dashboard/ErpPrjDashboardBizModel.java`（bean `app-service.beans.xml:43`）：
  - `getDashboardKpi:62-81`：openProjectCount + totalBudget（ErpPrjBudget.totalAmount）+ incurredCost（ErpPrjCostCollection.totalAmount）+ executionRate=incurredCost/totalBudget `:70-72`。
  - `getProjectStatusDistribution:84-106`：DB GROUP BY status。
  - `findCostOverrunAlert:110-135`：cost>budget，硬上限 `ALERT_MAX_ROWS=5000:55`。
  - `findDelayedProjectAlert:139-163`：endDate<today && status!=COMPLETED，算 overdueDays `:156`。
  - `getProjectGrossMargin:173-205`：聚合 ErpPrjProjectPnl Σrevenue/Σcost/ΣgrossProfit + grossMarginPct=Σprofit/Σrevenue `:193-195`。
- 全部 `@BizQuery`，经 `QueryBean` + `IOrmTemplate`/`IDaoProvider` 实时聚合（非硬编码常量）。`ALERT_MAX_ROWS=5000` 是行数封顶（防 OOM）非预警阈值。

---

## 3. 测试证据（L4，注明断言强度）

| 测试 | 文件#方法 | 覆盖验收标准 | 断言强度 |
|------|-----------|------------|---------|
| 损益汇总 | `TestErpPrjProjectPnl.java`（4 @Test：收入+四分类成本算术 `:67-106` / 空项目 `:109-129` / 非法期间抛错 `:132-148` / 幂等重算 `:151-172`） | UC-PRJ-06 ②③④⑤⑥（手动 refreshPnl 主路径） | 强断言（金额精确 + 异常路径 + 幂等） |
| 结算 + 转固 | `TestErpPrjProjectSettlement.java`（4 @Test：FINAL approve+post `:83-112` / CLOSE 转固 `:115-140` / 非法迁移 `:143-159` / 红冲凭证+资产 `:162-191`） | UC-PRJ-07 ①②③ + UC-PRJ-08 ①②③④ | 强断言（凭证行 Dr1601/Cr1603 + 资产卡片 + 状态守卫） |
| 看板 KPI + 预警 | `TestErpPrjDashboard.java`（5 @Test）+ `TestErpPrjDashboardGrossMargin.java`（4 @Test） | UC-PRJ-10 ①②（KPI 实时聚合）+ 预警 + 毛利率 | 强断言（KPI 值 + 预警触发/不触发 + overdueDays） |
| 结算 E2E | `tests/e2e/business-actions/projects-settlement-posting.action.spec.ts`（CLOSE 链 + 凭证行 Dr1601/Cr1603 + 红冲 `:58-184`）+ `projects-pnl-settlement.action.spec.ts`（FINAL/INTERIM 转账凭证行 `:141+, :220+`） | UC-PRJ-08 转固凭证 + UC-PRJ-07 FINAL 结算凭证 | 强断言（凭证行级 subjectCode/dcDirection/amount） |
| 看板 E2E | `tests/e2e/dashboards/projects.value.spec.ts`（getDashboardKpi + getProjectGrossMargin GraphQL 值断言） | UC-PRJ-10 ①② KPI 值 | 强断言（GraphQL 返回值精确比对） |

**测试缺口**（与缺口裁决一致）：①质保金**零测试**（retentionAmount/retentionDueDate 零 service writer，无测试可写）；②PnL **自动调度零测试**（nop-job 未接线，无调度路径可测）；③多币种折算零测试（exchangeRate=ONE 单币种路径）。

---

## 4. 运行时行为证据（L5）

| 来源 | 证实的行为 | 复用/补充 |
|------|-----------|----------|
| A2.13 §2.6 + §场景 G | 项目结算三轴状态机（Processor 链审批轴 + posted 轴）PASS，状态迁移守卫齐全 | **复用 MA2**（§去重协议，不重新核实） |
| A2.13 §2.7 | PnL calcStatus 2 态（PENDING/CALCULATED）全可达 PASS | **复用 MA2** |
| A2.13 §场景 I + R1.16 | P1-MA2-068 tryPost 悬挂 resolved（方案 A 实现 + 告警派发） | **复用 MA2**（HEAD 无回退） |
| `TestErpPrjProjectSettlement` + E2E | UC-PRJ-08 转固链：CLOSE→IErpAstAssetBiz.save 建资产卡片 + PROJECT_SETTLEMENT 凭证 Dr1601/Cr1603 + 来源项目 assetCardId 回链 + 红冲回退 | **补充**（L1 验收标准视角行为验收） |
| `TestErpPrjDashboard` + E2E | UC-PRJ-10 KPI 实时聚合 + 预警触发 + 毛利率 GraphQL 值断言 | **补充**（L1 验收标准视角行为验收） |

L5 存疑点（无法静态定论，需运行时确认）登记入 §7 静态存疑点清单交 MA4 展开。

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 分级，§2 判据）

### 五级追踪矩阵

| UC | L1 use-case | L2 owner doc | L3 代码路径 | L4 测试 | L5 运行时 | 结论 |
|----|-------------|--------------|------------|---------|-----------|------|
| UC-PRJ-06 | `use-cases.md:96`（§1 逐字引用 7 验收标准） | `profitability.md §关键流程 1:82`（设计参考，"依赖 nop-job 定时任务" + "多币种统一折算"与 L1 一致） | 聚合引擎 `ProjectPnlCalculator:57-132`（②③④⑤⑥ ✅）+ **①调度未接线**（config key 声明但零 nop-job 消费）+ **⑦多币种未实现**（`:105` exchangeRate=ONE） | `TestErpPrjProjectPnl` 4 @Test 强测覆盖②③④⑤⑥（无①⑦测试） | A2.13 PnL 2 态 PASS（复用）+ 缺①⑦运行时路径 | 见下方逐条 |
| UC-PRJ-07 | `use-cases.md:115`（§1 逐字引用 5 验收标准） | `profitability.md §结算:84`（设计参考，"质保金/保留款"列于 schema `:57-58` 未声明 Deferred） | 结算引擎 `ErpPrjProjectSettlementBizModel:28-91` + CreateSettlement Processor `:26-56`（①②③ ✅）+ **④⑤质保金逻辑层全缺**（schema 存在但零 service writer + 零返还工作流） | `TestErpPrjProjectSettlement` 4 @Test 强测覆盖①②③（无④⑤测试） | A2.13 结算三轴 PASS（复用）+ 缺④⑤运行时路径 | 见下方逐条 |
| UC-PRJ-08 | `use-cases.md:131`（§1 逐字引用 4 验收标准） | `profitability.md §结算:84`（设计参考，转固链与 L1 一致） | 转固链 `ApproveProcessor:30-33` + `Processor:174-194`（IErpAstAssetBiz.save 建卡片）+ `ProjectSettlementAcctDocProvider:52-100`（Dr1601/Cr1603）+ assetCardId 回写（①②③④ ✅） | `TestErpPrjProjectSettlement` 4 @Test + E2E 强断言覆盖①②③④ | A2.13 结算三轴 PASS（复用）+ E2E 凭证行级断言 | **接受**（§2 判据"接受"） |
| UC-PRJ-10 | `use-cases.md:167`（§1 逐字引用 6 验收标准） | `dashboards.md §6 项目看板:144-159`（设计参考，与 L1 一致） | `ErpPrjDashboardBizModel:62-205`（KPI 实时聚合①②③ + 预警④⑤ + 行级权限为平台层⑥） | `TestErpPrjDashboard` 5 @Test + `TestErpPrjDashboardGrossMargin` 4 @Test + E2E GraphQL 值断言覆盖①②③④⑤ | E2E 值断言证实实时聚合 | **接受**（§2 判据"接受"） |

### 逐 UC 结论（取最高）

#### UC-PRJ-06 项目损益汇总 → **部分接受 + 2 finding**（①调度 P1 + ⑦多币种 P2）

- **②③④⑤⑥ 聚合算术/四分类/毛利/毛利率/生成记录 = 接受**（§2 判据"接受"）：`ProjectPnlCalculator:57-132` 完整实现 + `TestErpPrjProjectPnl` 4 @Test 强测 + A2.13 PnL 2 态 PASS。三源对照一致。
- **①定时任务(nop-job)触发 → P1（新 `P1-RC-053`）**（§2 P1①功能实质偏离验收标准）：L1 逐字「定时任务(nop-job) →」要求 nop-job 自动触发路径；L3 实仓 config key `erp-prj.pnl-calc-cron` + `erp-prj.pnl-auto-calc-enabled` 已声明但**零 nop-job/cron/scheduler 消费方**，`IErpPrjProjectPnlBiz.java:20` 注释「经 nop-job 周期触发」为期望非实现，仅 `pnl-calc.batch.xml` 手动可触发。**§4 三判据复核**：(i) 无独立 plan-audit 专门裁决调度裁剪；(ii) owner doc `profitability.md:82` 显式「依赖 nop-job 定时任务」**未声明 Deferred**，L2 与 L1 一致——**推定 L2 已向实现妥协不成立**（L2 明确要求 nop-job，实现未接线属实现未达标非设计妥协）；(iii) product-scope 未将 PnL 自动调度列入范围裁剪。**三判据均不成立 → 非 documented simplification → Q4=(a) 强制实现**。**非 P0**：损益汇总功能本身完整（手动 refreshPnl 可达），调度缺失仅影响自动化（运营须手动触发 batch），不破坏活跃数据/会计正确性（GL 不受 PnL 影响——PnL 是管理会计汇总非总账凭证）/核心循环（损益计算主路径完整）。**新根因**（既有 arm-index grep `pnl.?calc.?cron|nop-job.*prj|prj.*schedule` 无 RC finding 涉及 PnL 调度接线缺失；P1-MA2-068 是 tryPost 吞异常悬挂**不同控制点**）。
- **⑦多币种折算到统一币种 → P2（新 `P2-RC-050`）**（§2 P2①次要验收标准未完全满足，主路径[单币种]OK 边界[多币种]弱）：L1 逐字「多币种折算到统一币种」；L3 `ProjectPnlCalculator:105` 硬编码 exchangeRate=ONE + Javadoc `:47-48` 显式「跨币种 rollup 精度归 successor」。**§4 三判据复核**：(i) 无独立 plan-audit；(ii) owner doc `profitability.md:26` schema 列「currencyId/exchangeRate/amountSource/amountFunctional 多币种四件套」**设计意图含多币种**但 Javadoc `:47-48` 显式 documented「跨币种 rollup 归 successor」属 AI 自标无人工批准痕迹（methodology §4 line 168「AI 自写标注不算人工批准」）；(iii) product-scope 未裁剪。**与 `P1-MA1-010` §7 裁决**：P1-MA1-010 = projects 多币种四件套 propId 缺失（**ORM 元数据层**），本缺口 = 聚合引擎汇率逻辑 exchangeRate=ONE（**业务逻辑层**），同域同主题（多币种）但**不同控制点不同根因**（propId 已 fixed vs 汇率逻辑未实现）→ 新建 `P2-RC-050` 不复用。**非 P1**：①主路径[单币种项目]exchangeRate=ONE 正确（本位币聚合，amountFunctional 已折算到本位币）；②不破坏 GL 平衡（PnL 非总账凭证）；③跨币种 rollup 仅影响精度不影响借贷平衡。声明 Q4=(a) 张力（若严格按 Q4 应升级 P1，但实际影响限于精度非活跃数据破坏）。

#### UC-PRJ-07 竣工结算与质保金 → **部分接受 + 1 finding**（④⑤质保金 P1）

- **①②③ 项目→COMPLETED + 基于 PnL 生成 FINAL 结算 + 最终收入/成本/损益 = 接受**（§2 判据"接受"）：`ErpPrjProjectSettlementBizModel:28-91` + `CreateSettlementProcessor:26-56` + `ProjectSettlementAcctDocProvider:59-100` 完整实现 + `TestErpPrjProjectSettlement` 4 @Test 强测 + A2.13 结算三轴 PASS。三源对照一致。
- **④⑤质保金(retentionAmount)留存 + 到期返还 → P1（新 `P1-RC-052`）**（§2 P1①功能完全缺失）：L1 逐字「质保金(retentionAmount)留存,到期返还」；L3 实仓 `retentionAmount`(propId 16) + `retentionDueDate`(propId 17) **schema 存在**（`app-erp-projects.orm.xml:803-804` + `_ErpPrjProjectSettlement.java` getter/setter）但 grep 全 `module-projects/erp-prj-service/src/main` `setRetention|getRetention|Retention` **零业务命中**（仅 `_gen` entity + api bean），createSettlement Processor 不填、无留存工作流、无到期返还 mutation。**质保金是竣工验收标准**，schema 存在说明需求已识别但逻辑层完全未实现。**§4 三判据复核**：(i) 无独立 plan-audit 专门裁决质保金裁剪；(ii) owner doc `profitability.md:57-58` schema 列 retentionAmount/retentionDueDate **设计意图含质保金**，未声明 Deferred；(iii) product-scope 未裁剪。**三判据均不成立 → 非 documented simplification → Q4=(a) 强制实现**。**非 P0**：质保金缺失不破坏活跃数据（结算主路径 FINAL 收入/成本/损益正确 + GL 借贷平衡）/不破坏会计正确性（质保金是 AR/资金面辅助账概念非 GL 核心）/非核心循环断裂（结算→转固主链完整）。**新根因**（既有 arm-index grep `retention|质保金|质保` 无 RC finding 涉及 projects 质保金 retentionAmount 业务逻辑）。

#### UC-PRJ-08 项目结算转固 → **接受**（§2 判据"接受"）

- L1 ①②③④ 全部实现：CLOSE→IErpAstAssetBiz.save 建资产卡片（`Processor:174-194`）+ PROJECT_SETTLEMENT 凭证 Dr1601/Cr1603（`AcctDocProvider:67-76`）+ 来源项目 assetCardId 回链（`:188`）+ 红冲回退（`Processor:196-206` + `ReverseSettlementProcessor:21-37`）。L3/L4/L5 三源一致 + E2E 凭证行级强断言（Dr1601/Cr1603）。无 §4 三判据问题。无候选缺口。

#### UC-PRJ-10 项目看板 → **接受**（§2 判据"接受"）

- L1 ①②③④⑤ 实现：KPI 实时聚合（`getDashboardKpi:62-81` + `getProjectGrossMargin:173-205`，非硬编码，经 `QueryBean`+`IOrmTemplate` 实时计算）+ 指标项[在手项目/预算/已发生成本/预算执行率/毛利率/超支预警/延期预警]齐全 + 预警=满足条件记录（`findCostOverrunAlert:110-135` cost>budget + `findDelayedProjectAlert:139-163` endDate<today）+ `ALERT_MAX_ROWS=5000` 是行数封顶非阈值（阈值维度本切片无 config 驱动项——成本超支阈值=budget 本身、延期阈值=today 本身，均为业务语义非可配置阈值，无 L1 ⑤"阈值来自系统配置"违规）。L3/L4/L5 三源一致 + E2E GraphQL 值断言。
- **⑥行级权限**：本切片仅注记——看板 `@BizQuery` 经平台层 `QueryBean` 自动注入 orgId/行级权限（与 finance/inventory 等域看板一致），行级权限落地属平台层 + A2.18 orgId 隔离 + A6.3 数据权限运行时审计范围，本切片不重复审计（§去重协议），注记为平台层行为。

### 切片总结

| UC | 结论 | 命中判据 |
|----|------|---------|
| UC-PRJ-06 | 部分接受 + P1(①调度) + P2(⑦多币种) | §2 P1① / §2 P2① |
| UC-PRJ-07 | 部分接受 + P1(④⑤质保金) | §2 P1① |
| UC-PRJ-08 | 接受 | §2 接受 |
| UC-PRJ-10 | 接受 | §2 接受 |

**零 P0**（候选缺口均不破坏活跃数据/会计正确性/核心循环：①调度缺失仅影响自动化非 GL 破坏 + ⑦多币种影响精度非借贷平衡 + ④⑤质保金缺失不破坏结算主路径/GL 平衡）。

---

## 6. 与 arm-index 衔接（§7 复用/新增裁决）

### 6.1 复用裁决

- **`P1-MA2-068`（TimesheetPostingDispatcher tryPost 吞异常悬挂）**：resolved R1.16（方案 A 实现）。**本切片复用其已证实行为**（PnL 调度缺失与 tryPost 悬挂不同控制点，不交叉），仅注记 P1-MA2-068 已 resolved 无回退。
- **`P1-MA2-069`（dict drift + CRUD 桩死状态）**：resolved R1.21（方案 B Deferred）。**本切片复用其已证实行为**，§去重协议不重审 audit-remediation 文本一致性维度。
- **`P1-MA1-010`（projects 多币种四件套 propId 缺失）**：✅ fixed（propId 重编号）。与本切片多币种缺口**同域同主题不同控制点**（propId ORM 元数据层 vs 聚合引擎汇率逻辑层）→ 见 §6.2 新建 `P2-RC-050` 裁决，不复用。
- **`P1-MA1-022`（跨域只读 daoFor todo MR1）**：本切片范围外的 daoFor 站点（`ErpPrjProjectSettlementProcessor.java:200` daoFor(ErpAstAsset) 回退只读 + `ExpenseCostAggregator.java:165` daoFor(ErpFinExpenseClaimLine)）归既有 finding 不重审。
- **A2.13 项目结算三轴 + PnL 状态机 PASS**：复用为 L5 既有证据（§去重协议，§4 复用）。

### 6.2 新增裁决（grep arm-index 后无同域同控制点 RC finding）

| 新 Finding | 域 | UC | 根因 | 与既有 finding 差异 | 分级 |
|-----------|---|----|------|-------------------|------|
| **`P1-RC-052`** | projects | UC-PRJ-07 ④⑤ | 质保金(retentionAmount/retentionDueDate) schema 存在但 service 层零 writer + 零返还工作流 | 新根因（arm-index grep `retention|质保金` 无 RC finding 涉及 projects 质保金业务逻辑） | P1（§2 P1①） |
| **`P1-RC-053`** | projects | UC-PRJ-06 ① | 损益汇总 nop-job 调度未接线（config key 声明但零消费方） | 新根因（vs P1-MA2-068 tryPost 悬挂不同控制点：调度接线缺失 vs 吞异常悬挂） | P1（§2 P1①） |
| **`P2-RC-050`** | projects | UC-PRJ-06 ⑦ | 多币种折算 exchangeRate=ONE 硬编码 + 跨币种 rollup 归 successor | vs P1-MA1-010 同域同主题不同控制点（propId ORM 元数据层 fixed vs 聚合引擎汇率逻辑层未实现） | P2（§2 P2①） |

### 6.3 双向可追溯

- 新 finding 入 arm-index RC 发现追踪分区（§见 arm-index 更新）。
- finding 修复行预留 MR1（R1.0 展开为 RC-R1.n 时引用 finding ID）。
- arm-index finding 行修复状态列待 MR1 修复完成后回填 `done`。

### 6.4 修复触及保护区域标注（§5 预授权/ask-first）

| Finding | 修复范围 | 保护区域 | 门控 |
|---------|---------|---------|------|
| P1-RC-052（质保金） | `ErpPrjProjectSettlementCreateSettlementProcessor` 增 retentionAmount/retentionDueDate 填充（按合同/结算比例）+ 新增到期返还 mutation（扫 retentionDueDate<=today 的 FINAL 结算 + 资金面返还）+ 质保金凭证（经 finance Provider） | 纯 BizModel/Processor 代码逻辑（retentionAmount schema 已存在，不加 ORM 列） | **预授权**（代码逻辑类，不触 §5 ask-first——schema 已存在不加列；质保金凭证若新增 businessType 经 Provider 注册属既有范式） |
| P1-RC-053（调度接线） | 接线 nop-job：注册 `erp-prj-pnl-calc` Job bean 消费 `erp-prj.pnl-calc-cron` config + 门控 `erp-prj.pnl-auto-calc-enabled` + 调 `pnl-calc.batch.xml` 或 `IErpPrjProjectPnlBiz.refreshPnl` 批量 | 纯调度接线 + BizModel 调用 | **预授权**（代码逻辑类，不触 §5 ask-first） |
| P2-RC-050（多币种） | `ProjectPnlCalculator:105` 解析项目汇率（经 master-data ErpMdExchangeRate 跨域只读）+ 跨币种 Billing/CostCollection 折算到项目本位币 | BizModel 汇率逻辑（不加 ORM 列，多币种四件套 propId 已 fixed） | **预授权**（代码逻辑类，不触 §5 ask-first——P1-MA1-010 propId 已 fixed，汇率载体已存在） |

---

## 7. 静态存疑点清单（供 MA4 A4.1/A4.2 运行时展开）

> L5 无法静态定论、需运行时确认的点。**P0 即时通道未触发**（本切片无 P0——质保金缺失不破坏活跃数据/会计正确性 + 调度缺失仅影响自动化 + 多币种影响精度但 GL 借贷平衡不受影响）。

| 编号 | 存疑点 | 展开方式 |
|------|--------|---------|
| SP-1 | **多币种项目 exchangeRate=ONE 在 ProjectPnl 实际金额偏差**：跨币种 Billing/CostCollection 经 amountFunctional（已折算到本位币）聚合，PnL exchangeRate=ONE 快照——多币种项目 PnL 的 revenueAmount/cost 是否因 source 币种混合产生精度偏差（与 P2-RC-050 + P1-MA1-010 运行时确认复用） | MA4 构造多币种 Billing（不同 currencyId）+ CostCollection → refreshPnl → 断言 PnL 金额与单币种基线偏差 |
| SP-2 | **pnl-auto-calc-enabled=true 时 batch 是否可手动触发调度路径**：config key 默认 false，若运维置 true 但无 nop-job 消费，batch task `pnl-calc.batch.xml` 是否经其他路径（如手动 API）可达（与 P1-RC-053 调度接线缺失复用） | MA4 模拟 config=true + 手动触发 batch → 断言 PnL 计算执行 + 记录无自动 cron 触发 |
| SP-3 | **质保金 schema 字段在 delta/未来定制消费的运行时行为**：retentionAmount/retentionDueDate 列存在但零 service writer，若经 delta/EAV 定制填充后 ProjectSettlementAcctDocProvider 是否感知（质保金凭证是否需新增 businessType）（与 P1-RC-052 复用） | MA4 经 delta 填充 retentionAmount → 断言结算凭证是否含质保金分录 + 到期返还路径行为 |
| SP-4 | **CLOSE 转固 IErpAstAssetBiz.save data map 非专用 API 的契约鲁棒性**：`ErpPrjProjectSettlementProcessor:176-184` 经 `assetBiz.save(data map)` 建 assets 卡片（非专用转固 API），data map 字段约定[code/name/orgId/acquisitionDate/originalValue/currentValue/residualValue/status]的契约鲁棒性 | MA4 构造 CLOSE 结算 → 断言资产卡片字段完整性 + 来源项目回链 + 资产域侧字段语义 |
| SP-5 | **看板 getProjectGrossMargin Σprofit/Σrevenue 聚合口径运行时行为**：`ErpPrjDashboardBizModel:193-195` grossMarginPct=ΣgrossProfit/ΣrevenueAmount，多项目混合（含亏损项目 revenue=0）的聚合口径 + orgId/行级权限自动注入行为（与 A2.18 orgId 隔离 + A6.3 数据权限运行时复用） | MA4 构造多项目（盈利+亏损+revenue=0）+ 不同 orgId 用户 → 断言聚合值 + 权限过滤 |

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual vs baseline 汇总如下表。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本次实测 `EXIT=0`），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以** checker 脚本退出码 0 作为门控通过依据。

  | 规则 | baseline | actual | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a | 34 | 34 | ✅ |
  | R2b | 240 | 229 | ✅ (≤) |
  | R2c | 1380 | 1382 | ⚠ +2（**非本审计引入**——本审计为只读审计零生产代码变更，delta 来自其他在途工作，登记供 CI/后续基线对账） |
  | R2d | 32 | 34 | ⚠ +2（**非本审计引入**——同上，只读审计） |
  | R3 | 5 | 5 | ✅ |
  | R4/R5/R7/R8/R11 | 0/0/0/0/0 | 0/0/0/0/0 | ✅ |

  **本报告无生产代码变更（纯审计报告），checker 无回归风险**。R2c/R2d 的 +2 delta 系本审计之外的在途工作引入，非本切片所致；本审计未修改任何 `.java`/`.xml`/`.yaml` 生产文件（仅新增本报告 + 更新 arm-index + plan 状态）。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（P1-RC-052/053 + P2-RC-050）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6.1/§6.2），无未经比对直接新建的 finding。

---

## 9. 与 MA2 报告差异增量声明（重申）

见报告开头 §9（前置声明）。**复用 A2.13（项目结算三轴 + PnL 状态机 PASS + P1-MA2-068/069 resolved）已证实行为，只补需求视角差异**：UC-PRJ-06 调度未接线 + 多币种未实现 / UC-PRJ-07 质保金逻辑层全缺 / UC-PRJ-08 转固 + UC-PRJ-10 看板行为验收（接受）。

---

## 段落完整性自检（§6 报告输出格式，9 段齐全）

- [x] §1 需求契约原文（L1 逐字引用，4 UC 验收标准完整枚举）
- [x] §2 实现证据（L3 含行号 + 跨域调用链）
- [x] §3 测试证据（L4 注明断言强度 + 缺口）
- [x] §4 运行时行为证据（L5 复用 MA2 + 补充）
- [x] §5 符合性结论（五级矩阵 + 每 UC 分级 + §2 判据 + §4 三判据复核）
- [x] §6 与 arm-index 衔接（复用/新增裁决 + 双向可追溯 + 保护区域标注）
- [x] §7 静态存疑点清单（SP-1~SP-5 供 MA4 展开）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置 + 重申）

**真相源冻结条款遵守声明**：本审计未修改任何真相源（`product-scope.md` / `use-cases.md` / `profitability.md` / `dashboards.md` 的需求契约段落）。发现的 doc 分歧（profitability.md:82 nop-job + :57-58 质保金设计意图 vs 实现未达）记入本报告 §5，不直改真相源（§9 冻结条款）。
