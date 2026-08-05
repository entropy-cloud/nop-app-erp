# rc-ma2-a2-1-2-finance-simplification-review finance MA2 已裁决简化/Deferred 复查报告（A2.1 + A2.2）

> Plan Status: completed
> 产出时间：2026-08-06
> 来源 Plan：`docs/plans/2026-08-07-0300-1-rc-ma2-a2-1-2-finance-simplification-review.md`（Work Item A2.1 + A2.2，规则 14 合并）
> Mission：requirement-compliance（MA2 方案 B 关闭项复查）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 分级判据 / §4「显式人工批准记录」三判据 / §5 Q4 修复义务 / §6 报告 9 段 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议）
> 路线图：`docs/backlog/requirement-compliance-roadmap.md`（A2.1 finance 会计保护区域 6 项 + A2.2 finance 非保护区域 1 项）
> 复查全集：`docs/audits/rc-existing-inventory.md`（A2.1 6 项 + A2.2 1 项 + §对账差异登记 #1/#2）
> Skill：`docs/skills/open-ended-audit-prompt.md`
> 审计性质：**只读审计**——读 plan / owner doc / product-scope / arm-index / git 历史裁决证据，**不修改任何代码/ORM/api.xml/真相源**

---

## §导出口径与复查对象

本报告复查对象 = M0.3（`rc-existing-inventory.md`）导出的 finance 域方案 B 关闭项全集 **7 项**（A2.1 会计保护区域 6 项 + A2.2 非保护区域 1 项），分区完整性已校验（无重叠无遗漏）。每项逐条应用方法论 §4「显式人工批准记录」三判据 (i)→(ii)→(iii) 核证，区分「有意设计（保留 P2 successor）」vs「静默降级（重开 MR1）」。

**Q4 修复义务绑定**（§5）：P0/P1 必须实现，禁止方案 B 关闭，无「技术不可行」例外通道。唯一合法出口 = 需求本身不合理经人工批准改 product-scope。会计/数据安全类强制实现无例外。`P0-MA2-018` 经 Q4 强制 → 重开 MR1（既有 arm-index P0 deferred 边界：经 MA2 重新分级后入 MR1，非 MR0）。

---

## 1. 方案 B 关闭项清单 + 锚点（段 1，§6 MA2 适配）

| # | Finding ID | 域 | arm-index 关闭方式标签 | owner doc 锚点（§4(ii) 审查对象） | 关闭 plan（§4(i) 审查对象） | successor（→ A3.1） | 分区 |
|---|-----------|----|---------------------|-------------------------------|--------------------------|---------------------|------|
| 1 | `P0-MA2-018` | finance | `deferred` | `posting-log.md §ErpFinVoucherBillR 索引与过账性能 §与 P0-MA2-018（deferred 字面 UK）的边界区分` | `2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk.md`（Plan Status=deferred，plan-audit REJECT/BLOCK） | 重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK（非降级） | A2.1 |
| 2 | `P1-MA2-001` | purchase+finance（主域 finance） | `方案 B 裁决（documented simplification，非降级 deferred）` | `purchase/returns.md §暂估应付冲减「正向 receive→invoice 暂估冲回」`；`finance/posting.md §GRNI 暂估冲回 documented simplification`（:95） | `2026-07-29-2322-1-r1-8-p2p-grni-accrual-reversal-payment-match.md`（completed） | 方案 A GRNI 自动冲回（需 inventory `repostPurchaseInput` SPI） | A2.1 |
| 3 | `P1-MA2-018` | finance | `documented simplification` | `period-close.md §已知简化「年初余额非累计」`（:309-314） | `2026-07-29-2322-3-r1-10-r1-11-period-close-accounting-semantics.md`（completed） | GL 余额维护引擎（opening/closing） | A2.1 |
| 4 | `P1-MA2-019` | finance | `作用域修复 + documented simplification 残留` | `period-close.md §已知简化「辅助账跨年对账作用域」`（:316-319） | `2026-07-29-2322-3`（completed，含代码修复） | 累计余额对账（依赖 GL 余额 successor） | A2.1 |
| 5 | `P1-MA2-020` | finance | `documented simplification` | `period-close.md §已知简化「反结账审批（kill-switch successor）」`（:321-325）；`state-machine.md §反结账审批已知简化（P1-MA3-036）`（:195）+ `§已知限制`（:193） | `2026-07-29-2322-3`（completed） | 完整反结账审批流（非 xwf 替代机制；`2026-07-09-2330-1` 裁决浏览器层 xwf NOT FEASIBLE） | A2.1 |
| 6 | `P1-MA2-022` | finance | `documented simplification` | `period-close.md §已知简化「FX 重估无前期 reversal（IAS 21 残留风险）」`（:331-336） | `2026-07-29-2322-3`（completed） | 前期 FX 凭证期末自动 reversal + 期间过滤 | A2.1 |
| 7 | `P1-MA1-016` | finance（→ assets 只读） | `resolved（永久只读豁免，登记于 data-dependency-matrix.md §9）` | `architecture/data-dependency-matrix.md §9 跨域只读访问裁决` | `2026-07-29-2225-1-cross-domain-daofor-governance-adjudication.md`（completed） | 永久接受（无 successor，受 nop-entropy lazy/SPI 阻塞） | A2.2 |

---

## 2. §4 三判据逐项证据（段 2）

> 判据应用顺序 (i)→(ii)→(iii)；判据三仅当 (i)/(ii) 均不成立时兜底。**代理独立审计通过 = 「审计裁决质量证据」（可区分「静默降级」vs「经审计裁决的简化」），不算人工批准**（方法论 §4）。证据来源 = 关闭 plan 的 `Draft Review Record` / `## Closure`（§4(i)）+ owner doc 显式标注 + git 批准痕迹（§4(ii)）+ product-scope（§4(iii)）。

### 2.1 `P0-MA2-018`（凭证幂等键字面 UK）— 三判据均不成立

- **(i) plan 含独立 plan-audit 通过记录**：**不成立**。关闭 plan `2026-07-28-1249-arm-fix-p0-ma2-018` 的独立 plan-audit 子代理出具 **REJECT/BLOCK**（非通过）：提议的字面 UK `(billCode, businessType)` 与红冲「同键 2 行」（`TestErpFinPostingService.testReverse:225`）+ 多账套「同键 N 行」（`TestErpFinMultiSchemaPropagatesTwoVouchersWithDistinctSchema:89`）+ 软删除重插生命周期（`useLogicalDelete="true"`）三重契约冲突，按字面落地必然回归。Plan Status 置 `deferred` 等候人工裁决修复方向 A/B/C/D，**未通过**。EXECUTE 三次重跑复核阻断性发现仍在活仓成立。
- **(ii) owner doc 显式 documented simplification 标注且经人工批准**：**不成立**。`posting-log.md §ErpFinVoucherBillR 索引`（:56-67）记录的是**非唯一索引**裁决（加速查询，不施加唯一约束），明确区分「本索引是 `unique="false"` 非唯一索引——不触发 P0-MA2-018 的三重冲突」；P0-MA2-018 的 UK 缺口本身在 owner doc 中是**审查对象**（标注 deferred 字面 UK 边界），无人工批准的 documented simplification 标注。arm-index 标签 = `deferred`（非 documented simplification）。
- **(iii) product-scope 范围裁剪登记**：**不成立**。product-scope.md 无凭证幂等/UK 相关范围裁剪条目（grep `GRNI|幂等|bill_r|凭证幂等` 零命中）。
- **核证结论**：三判据均不成立 → **静默降级**。叠加 Q4 裁决（§5）：P0 必须实现，无「技术不可行」例外——技术不可行项须更深设计变更（重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK），非退缩到 deferred。

### 2.2 `P1-MA2-001`（GRNI 自动冲回）— 判据 (i) 成立

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。关闭 plan `2026-07-29-2322-1` 含 `Draft Review Record`（2 轮迭代，独立子代理 `ses_051849071` / `ses_0517fb0b9` acceptable-as-is）+ `Closure Audit Evidence`（独立结束审计子代理，新会话，PASS）。
- **(ii) owner doc 显式 documented simplification 标注且经人工批准**：owner doc 标注存在（`returns.md §暂估应付冲减「正向 receive→invoice 暂估冲回（documented simplification）」` + `posting.md §GRNI 暂估冲回 documented simplification:95`），但批准来源 = 独立子代理审计裁决（非人工）。依方法论「代理独立审计不算人工批准」，(ii) 单独不成立——但 (i) 已成立，无需触发 (ii) 的人工批准判定。
- **(iii) product-scope 范围裁剪登记**：**不成立**（product-scope 无 GRNI/暂估冲回范围裁剪；grep 零命中）。
- **核证结论**：判据 (i) 成立 → 该简化经独立审计裁决（区别于静默降级）。裁决理由（plan Phase 1 Explore 证伪简单冲回可行性：`reverse()` 仅全额红冲致部分开票少计暂估 + reverseApprove 反冲回需 inventory 域 `repostPurchaseInput` SPI 缺失 + 跨期语义）有 owner-doc 证据支撑。

### 2.3 `P1-MA2-018`（年初余额非累计）— 判据 (i) 成立

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。关闭 plan `2026-07-29-2322-3` 含 `Draft Review Record`（独立子代理 `ses_05184514d` acceptable-as-is）+ `Closure Audit Evidence`（独立结束审计子代理，新会话，逐项 grep/read 核实，PASS）。
- **(ii) owner doc 显式 documented simplification 标注且经人工批准**：owner doc 标注存在（`period-close.md §已知简化「年初余额非累计（documented simplification）」:309-314`），批准来源 = 独立子代理审计（非人工）；(ii) 单独不成立但 (i) 已成立。
- **(iii) product-scope 范围裁剪登记**：**不成立**（product-scope 无年初余额范围裁剪）。
- **核证结论**：判据 (i) 成立 → 经审计裁决的简化。裁决理由（`AnnualCloseService:49-50` 注释明示 ErpFinGlBalance 未由过账引擎维护，补 GL 余额维护触及 PostingProcessor + 全 Provider 跨模块架构变更）有 Explore 证据支撑。

### 2.4 `P1-MA2-019`（辅助账跨年对账作用域）— 判据 (i) 成立（含代码修复）

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。同 plan `2026-07-29-2322-3`（见 2.3）。**特别**：本项裁决为「作用域修复（代码）+ documented simplification 残留」——`AnnualCloseService.sumArApOpenFunctional:237-257` 已增 `businessDate` 年度过滤（代码修复落地），仅累计余额对账残留为 successor。
- **(ii) owner doc 显式标注**：`period-close.md §已知简化「辅助账跨年对账作用域」:316-319` 存在（作用域一致性 + documented simplification 残留）；批准来源 = 独立子代理审计。
- **(iii) product-scope 范围裁剪登记**：**不成立**。
- **核证结论**：判据 (i) 成立 → 经审计裁决的简化（主缺陷已代码修复，残留为 successor）。

### 2.5 `P1-MA2-020`（反结账审批）— 判据 (i) 成立（Q4 张力已 ack）

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。同 plan `2026-07-29-2322-3`。
- **(ii) owner doc 显式标注**：`period-close.md §反结账审批（documented simplification — kill-switch successor）:321-325` + `state-machine.md §反结账审批已知简化（P1-MA3-036）:195` + `§已知限制:193` 存在；批准来源 = 独立子代理审计。
- **(iii) product-scope 范围裁剪登记**：**不成立**。
- **Q4 张力核证**（本计划 Current Baseline 指定）：successor「完整反结账审批流」原依赖浏览器层 xwf 审批路径，`2026-07-09-2330-1-use-workflow-browser-e2e-feasibility.md` 裁决 xwf 浏览器层 **NOT FEASIBLE**（`WorkflowEngineImpl.newSteps` fallback `sysUser(0)` 与 NopAuthUser `tagSet=seq` 冲突致 `allowCallByUser` 拒绝）。**Q4=(a) 无「技术不可行」例外**——若 successor 回队（A3.1），修复须找**更深设计变更（非 xwf 的替代审批机制）**，不退缩到方案 B。当前 kill-switch 关闭裁决本身经 §4(i) 独立审计，属经审计裁决的简化。
- **核证结论**：判据 (i) 成立 → 经审计裁决的简化（kill-switch 关闭）；successor 回队条件（A3.1）须重新评估非 xwf 替代机制，非本 A2.x 范围。

### 2.6 `P1-MA2-022`（FX 重估无前期 reversal）— 判据 (i) 成立

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。同 plan `2026-07-29-2322-3`。
- **(ii) owner doc 显式标注**：`period-close.md §已知简化「FX 重估无前期 reversal（documented simplification — IAS 21 残留风险）」:331-336` 存在（含 config-gated `erp-fin.exchange-revaluation-enabled`）；批准来源 = 独立子代理审计。
- **(iii) product-scope 范围裁剪登记**：**不成立**。
- **核证结论**：判据 (i) 成立 → 经审计裁决的简化（config-gated + IAS 21 残留风险显式标注）。

### 2.7 `P1-MA1-016`（finance→assets 永久只读豁免）— 判据 (i) 成立（归类核实）

- **(i) plan 含独立 plan-audit 通过记录**：**成立**。关闭 plan `2026-07-29-2225-1` 含 `Draft Review Record`（2 轮迭代，独立子代理 `ses_051b8f106` acceptable-as-is）+ `Closure Audit Evidence`（独立结束审计子代理 `ses_051a2e94`，逐项核实，PASS）。
- **(ii) owner doc 显式标注**：`data-dependency-matrix.md §9 跨域只读访问裁决`（architecture owner doc）登记「finance→assets 目标域=永久只读豁免（受 nop-entropy lazy/SPI 解耦阻塞）」；批准来源 = 独立子代理审计。
- **(iii) product-scope 范围裁剪登记**：**不成立**。
- **归类核实**（M0.3 §对账差异登记 #1 要求）：arm-index 关闭标签字面为 `resolved（永久只读豁免）`，非 `方案 B / documented simplification / Deferred` 三标签之一。**本复查裁决归类恰当**：该关闭无生产代码逻辑变更以修复 finding 本身（`ErpFinAccountingPeriodProcessor.reverseDepreciation` 仍走 daoFor 只读，状态写经 `IErpAstDepreciationScheduleBiz.reverseDepreciation` I*Biz），关闭方式 = 登记 governance 豁免 + 文档化，与方法论 §4(ii)「owner doc 显式 documented simplification 标注」实质同构。属 MA2 复查范围，归类 KEEP 成立。
- **核证结论**：判据 (i) 成立 → 经审计裁决的简化（永久只读豁免，受平台 SPI 阻塞）。

---

## 3. 实现证据（段 3，复用既有 arm 审计，§去重协议）

> 本复查为方案 B 关闭项复查（需求契约视角），不重做 doc↔code 文本一致性。实现证据复用既有 arm MA2/MA4 报告已证实的代码路径，仅列锚点供 §4 三判据核证溯源。

| Finding ID | 代码锚点（复用 arm MA2/MA4 已证实） | 既有证实报告 |
|-----------|--------------------------------------|-------------|
| P0-MA2-018 | `ErpFinPostingProcessor.alreadyPosted:472` TOCTOU pre-check + `erp_fin_voucher_bill_r` 无 `(billCode,businessType)` UK（`app-erp-finance.orm.xml:643-647` 仅非唯一 IDX） | `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md §11`；`2026-07-28-1510-arm-ma2-multi-company-isolation.md §6.4`（多公司维度复核：billR 无 acctSchemaId 列，加 orgId 不足修复） |
| P1-MA2-001 | `InvPostingDispatcher.java:203`（PURCHASE_INPUT billHeadCode=stockMove.code）+ `PurInvoicePostingDispatcher.java:74`（AP_INVOICE billHeadCode=invoice.code，无共享键） | `2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`；`2026-07-28-1249-arm-ma2-budget-commitment-release.md`（承付释放与 GRNI 正交） |
| P1-MA2-018 | `AnnualCloseService.populateNextYearOpening:148` + `aggregateYearSubjectActivity`（仅本年）+ `:49-50` 注释（ErpFinGlBalance 未由过账引擎维护） | `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md:40`；`2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md:249` |
| P1-MA2-019 | `AnnualCloseService.sumArApOpenFunctional:237-257`（已增年度过滤）+ `subjectNetForYear:266-281` | 同上 MA2 状态机报告 :41；MA4 报告 :250 |
| P1-MA2-020 | `ErpFinAccountingPeriodProcessor.reverseClose:278-281` config kill-switch + `isReverseCloseApprovalRequired:653-656` | 同上 MA2 状态机报告 :42, :273-297（升级评估维持 P1） |
| P1-MA2-022 | `ExchangeRevaluationService.revalueArAp:106-108`（无期间过滤 + 无前期 reversal） | 同上 MA2 状态机报告 :44；MA4 报告 :253（+ 相邻路径 P2-MA4-003(a) 性能缺陷） |
| P1-MA1-016 | `ErpFinAccountingPeriodProcessor.reverseDepreciation` `daoFor(ErpAstDepreciationSchedule).findAllByQuery`（只读，状态写经 I*Biz） | `2026-07-27-1227-arm-ma1-cross-module-dag.md:257`；`2026-07-28-0400-arm-ma2-assets-state-machine.md:299`；`2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md:143` |

---

## 4. 运行时行为证据（段 4，复用既有 arm MA2/MA3，§去重协议）

> 本 mission MA2 = 方案 B 关闭项复查（需求契约视角），与 audit-remediation MA2（状态机/链路行为视角）/ MA3（doc↔code drift）/ MA4（代码质量）维度不重叠（方法论 §去重协议 §MA2(本)↔MA3(audit-remediation) 边界）。既有 arm 报告已证实的运行时行为直接引用：

- **P0-MA2-018**：并发 post/兜底重试可双 INSERT 重复凭证（`ErpFinDeferredPostingRetryHelper` REQUIRES_NEW 同 TOCTOU race），GL 借贷双计——经 `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md` 逐项裁决确认（约 50 处交接并发敏感点中 3 项升级 P0）。
- **P1-MA2-001**：GL 2202 暂估应付双计 + 1403/1401 存货双计，辅助账层（ErpFinArApItem）不受影响（`ErpFinArApItemGenerator.resolveProfile` 不处理 PURCHASE_INPUT）——经 P2P E2E 报告证实。
- **P1-MA2-018/019/020/022**：经 `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md` 逐项运行时复核确认「仅治理缺陷/数值漂移/作用域精度/kill-switch 门控」，状态机迁移路径正确，无运行时数据破坏升级（7 项 MA2 finding 复核无升级）。
- **P1-MA1-016**：finance→assets 跨域**只读** DAO（findAllByQuery posted=true 折旧凭证冲销对象），状态写经 I*Biz，assets 侧 reverseDepreciation 守卫 + 回滚正确——经 assets 状态机 + MA4 报告复核确认 read-only 无数据破坏。

---

## 5. 符合性结论（段 5，§6 MA2 适配：复查结论 + 是否重开 MR1）

> 复查结论二分：`有意设计（保留 P2 successor）`（§4 三判据满足其一）/ `静默降级（重开 MR1）`（三判据均不满足）。P0 经 Q4 强制重开 MR1（既有 P0 deferred 边界）。

### 5.1 逐项复查结论 + 三源对照

| Finding ID | §4 (i) plan-audit 通过 | §4 (ii) owner doc 标注+人工批准 | §4 (iii) product-scope 裁剪 | 命中判据 | 复查结论 | 重开路由 |
|-----------|----------------------|-------------------------------|---------------------------|---------|---------|---------|
| `P0-MA2-018` | ❌（REJECT/BLOCK，deferred） | ❌（deferred，无人工批准标注） | ❌ | **无** | **静默降级** | **重开 MR1**（Q4 强制 + §2 既有 P0 deferred 边界，非 MR0） |
| `P1-MA2-001` | ✅（plan 2322-1，2 轮 + closure PASS） | ⚠️（标注有，人工批准=子代理审计非人工） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开 |
| `P1-MA2-018` | ✅（plan 2322-3，1 轮 + closure PASS） | ⚠️（同上） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开 |
| `P1-MA2-019` | ✅（plan 2322-3 + 代码修复） | ⚠️（同上） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开（主缺陷已代码修复） |
| `P1-MA2-020` | ✅（plan 2322-3） | ⚠️（同上） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开（successor 回队须非 xwf 机制，归 A3.1） |
| `P1-MA2-022` | ✅（plan 2322-3） | ⚠️（同上） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开 |
| `P1-MA1-016` | ✅（plan 2225-1，2 轮 + closure PASS） | ⚠️（豁免登记于 §9，人工批准=子代理审计） | ❌ | **(i)** | **有意设计（保留 P2 successor）** | 不重开（归类恰当，§对账差异 #1 核实通过） |

**三源对照声明**（每项 arm-index 关闭标签 vs owner doc 标注 vs product-scope）：7 项 owner doc 标注与 arm-index 关闭标签语义一致（M0.3 导出已校验）；product-scope 全部无范围裁剪登记（§4(iii) 全不成立，(i)/(ii) 已分别裁决）。

### 5.2 P0-MA2-018 Q4 强制结论（最高优先）

`P0-MA2-018` 三重张力（Current Baseline 已框定）经复查确认：
- (a) 它是 **P0**（业财幂等键不变量破坏，GL 借贷双计，§2 判据①④）；
- (b) roadmap §当前基线「P0 deferred 边界声明」将其列为既有 arm-index P0 deferred；
- (c) **Q4=(a)「P0/P1 必须实现，禁止方案 B 无例外」+ 方法论 §5「技术不可行项须更深设计变更（重构 billR 加 acctSchemaId/postingType/isReversed 判别列 + 对应 UK），非退缩到方案 B」**。

§4 三判据均不成立（plan-audit REJECT 非「通过记录」；deferred 非 documented simplification；product-scope 无裁剪）。**结论：Q4 强制实现 → 重开入 MR1（R1.0 展开为 RC-R1.n）**。重开路由 = MR1（非 MR0）：依方法论 §2「既有 arm-index P0 deferred 项仅当 MA2 重新分级时进入 MR1」+ roadmap §当前基线（MR0 仅对本审计新发现 P0）。修复行触及 ORM 结构变更（billR 判别列 + UK）+ 会计过账逻辑，**须 ask-first + 独立 plan-audit**（§5 保护区域暂停协议）；修复方向 = 反范式化判别列到 billR/voucher（方向 B）或部分唯一索引（方向 A），禁止字面 UK 回归。

### 5.3 统计

- **重开 MR1**：1 项（`P0-MA2-018`，P0，Q4 强制）
- **有意设计（保留 P2 successor）**：6 项（5 项 A2.1 P1 + 1 项 A2.2 P1）
- **本审计新发现 P0**：0 项（无 MR0 即时通道触发）

---

## 6. 与 arm-index 衔接（段 6，§7「复用 or 新增」裁决）

> §7 规则：本复查的 7 项均为既有 arm finding，原则上**复用既有 ID 追加 RC 注记**；仅当发现新根因/新控制点/新维度才新建 `P*-RC-xxx`。

### 6.1 逐项「复用 or 新增」裁决

| Finding ID | arm-index grep 结果（同域同控制点） | 裁决 | 操作 |
|-----------|--------------------------------------|------|------|
| `P0-MA2-018` | 既有 arm finding（finance 凭证幂等 UK），无新根因（同一字面 UK 缺口），MA2 仅重新分级（deferred→Q4 强制重开 MR1） | **复用** | 既有行追加「RC MA2 复查：静默降级，Q4 强制重开 MR1（R1.0 展开为 RC-R1.n，修复须更深设计变更非字面 UK）」注记 |
| `P1-MA2-001` | 既有 arm finding（GRNI 冲回），MA2 复查确认方案 B 关闭裁决正当 | **复用** | 既有行追加「RC MA2 复查：有意设计，§4(i) 成立，保留 P2 successor」注记 |
| `P1-MA2-018/019/020/022` | 既有 arm finding（period-close 族），MA2 复查确认方案 B 关闭裁决正当 | **复用** | 同上注记 |
| `P1-MA1-016` | 既有 arm finding（跨域只读豁免），MA2 复查确认归类恰当 | **复用** | 同上注记 |

**裁决依据**：7 项均为既有 arm finding 的同一根因/同一控制点，MA2 复查仅做「方案 B 关闭裁决正当性」裁决（需求契约视角），无新根因/新控制点/新维度——全部复用既有 ID，**不新建 P*-RC-xxx**（禁止未经比对直接新建）。

### 6.2 双向可追溯

- **重开项 ↔ MR1 R1.0 预留展开行**：`P0-MA2-018` → MR1 R1.0 展开为 `RC-R1.n`（修复行须含 finding ID 交叉引用 `P0-MA2-018` + 触及保护区域标注「ORM 结构变更 + 会计过账逻辑」+ Skill）。
- **有意设计项 ↔ A3.1 successor 复查**：6 项 P2 successor 交叉引用 A3.1（finance 域 successor 复查），由 A3.1 复查 successor 触发条件（successor 触发条件复查属 MA3，独立 plan；本 A2.x 只复查关闭裁决正当性，方法论 §MA2↔MA3 协作）。
- **arm-index 回填**：§6.1 注记已写入 `arm-index.md`（既有行追加，非新分区）。

---

## 7. 静态存疑点清单（段 7，供 MA4 A4.1 展开）

> L5 无法静态定论、需运行时确认的点。本复查为方案 B 关闭项复查（读 plan/owner doc/product-scope），以下为复查中静态无法定论、建议 MA4 运行时确认的点：

1. **P0-MA2-018 并发重复凭证实际触发面**：字面 UK 修复方向（反范式化判别列）落地后，并发 `IErpFinVoucherBiz.post` + `ErpFinDeferredPostingRetryHelper` 兜底重试 + 人工重试的 TOCTOU 实际触发概率需运行时并发探针确认（静态已确认 race window 存在，但触发频率依赖部署负载）——建议 MR1 修复行（RC-R1.n）附并发负向测试。
2. **P1-MA2-020 反结账 `=false` 时权限门控实际强度**：`reverseClose` 为 `@BizMutation`，理论上经角色-resource 种子门控，但 MA2 报告（`2026-07-27-2315:273-297`）指出「无显式 @BizAuth，依赖配置层 enableActionAuth」。`=false` 时「任何能调 mutation 的角色均可反结账」的实际权限强度需运行时角色矩阵确认——交接 A3.1 successor（完整审批流）+ A2.18 权限注解审计 successor。

> 其余 5 项（P1-MA2-001/018/019/022 + P1-MA1-016）的运行时行为已由既有 arm MA2/MA4 报告充分证实（§4），无新增静态存疑点。

---

## 8. 过程纪律自检（段 8，§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（actual 见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码不反映 actual vs baseline），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不以 checker 脚本退出码作为门控通过依据**。**本审计无生产代码变更（纯审计报告 + arm-index 文档注记），checker 无回归风险**——actual 计数与本审计行为正交（未触及任何生产代码），任何 actual vs baseline 差异均非本审计引入。

  | 规则 | 基线（compliance-baseline.md） | actual（本次实测） | 漂移 | 归因 |
  |------|-------------------------------|-------------------|------|------|
  | R1d | 14 | 14 | 0 | — |
  | R2a | 34 | 34 | 0 | — |
  | R2c | 1380 | 1382 | +2 | 非本审计引入（本审计零代码变更；漂移源自既有生产代码演化，与本审计正交） |
  | R2d | 32 | 34 | +2 | 同上 |

  > 本审计仅产出本报告 + `arm-index.md` 注记（纯文档），未触及 `module-*/` 任何生产代码。actual 与基线的差异在本审计执行前已存在，非本审计行为导致，故无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计（见来源 plan Closure Gates）。
- [x] **与 arm-index 交叉去重声明**：本报告全部 7 项 finding 已按 §7 规则 grep arm-index 同域同控制点后给出「复用」裁决（§6.1），无未经比对直接新建的 `P*-RC-xxx` finding。

---

## 9. 与既有审计差异增量声明（段 9，§去重协议）

本报告与既有 arm 审计（`docs/audits/2026-07-2*-arm-ma2-*` / `arm-ma3-*` / `arm-ma4-*`）的差异增量：

- **复用既有证据**（不重复验证）：
  - `2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（P0-MA2-018 并发缺陷已证实）；
  - `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（P1-MA2-018/019/020/022 状态机行为已证实，运行时复核无升级）；
  - `2026-07-27-1949-arm-ma2-procure-to-pay-e2e.md`（P1-MA2-001 GRNI 冲回行为已证实）；
  - `2026-07-28-1510-arm-ma2-multi-company-isolation.md §6.4`（P0-MA2-018 多公司维度复核）；
  - `2026-07-28-2130-arm-ma4-finance-budget-arap-cost-period-code-quality.md` + `2026-07-28-2130-arm-ma4-finance-posting-voucher-code-quality.md`（代码层无新缺陷）；
  - `2026-07-28-1510-arm-ma3-design-doc-baseline.md`（doc↔code drift 视角，与本文档需求契约视角正交）。

- **本复查只补的差异增量**：**需求契约 vs 方案 B 关闭裁决正当性**——从 §4 三判据（i）plan-audit 通过 / (ii) owner doc 显式标注+人工批准 / (iii) product-scope 范围裁剪 出发，逐项核证 arm-index 方案 B 关闭项的关闭裁决是否「有意设计（经审计裁决）」vs「静默降级」。这是既有 arm 审计（doc↔code / 状态机行为 / 代码质量维度）未覆盖的「需求契约 vs 关闭裁决正当性」维度（方法论 §去重协议 §MA2(本)↔MA3(audit-remediation) 边界）。

- **不重复**：不重做 doc↔code 文本一致性（audit-remediation MA3 已收口）、不重做状态机/链路行为（arm MA2 已收口）、不重做代码质量（arm MA4 已收口）、不裁决 successor 是否回队（属 MA3 A3.1，独立 plan）。

---

## 结论

finance MA2 复查（A2.1 + A2.2）完成：7 项方案 B 关闭项逐项经 §4 三判据核证。

- **重开 MR1**：1 项（`P0-MA2-018`，P0，Q4 强制实现，§4 三判据均不成立 → 静默降级；修复须更深设计变更非字面 UK，触及 ORM + 会计过账保护区域须 ask-first）。
- **有意设计（保留 P2 successor）**：6 项（`P1-MA2-001/018/019/020/022` + `P1-MA1-016`，§4(i) 独立 plan-audit 通过记录均成立 → 经审计裁决的简化）。
- **arm-index 衔接**：7 项全部复用既有 ID 追加 RC 注记（无新 `P*-RC-xxx`）；`P0-MA2-018` 重开标记入 MR1 R1.0 展开行。
- **本审计无生产代码变更**（纯报告 + arm-index 文档注记），§9 真相源冻结条款遵守（未修改 product-scope / owner doc 需求契约段落 / arm-index 已关闭 finding 的关闭事实）。
