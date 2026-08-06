# RC MA4 A4.1.21 — 年末反结账阻断边界与 GlBalance yearOpening 残留一致性评估

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.21（MA4 运行时行为验证 — A1.6 §7 存疑点 4：UC-FIN-07 RC-4 年度结转凭证冲销 + RC-3 反结账——`ReverseCloseProcessor:32-36` 年末反结账阻断边界：次年期间已手动删除但 `ErpFinGlBalance` yearOpening 残留时，反结账红冲年度结转凭证是否致次年年初余额与凭证不一致）
> 输入：`docs/audits/2026-08-02-2100-rc-ma1-a1-6-finance-f6-period-close.md` §7 存疑点 4 + §2.8 RC-3 + §2.9 RC-4 + §5.2 RC-3/RC-4 接受[正常路径] + §3.6 测试覆盖
> 验证 plan：`docs/plans/2026-08-06-1708-3-rc-ma4-a4-1-21-year-end-reverse-close-block-boundary.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据[含 P2① 次要验收标准边界 / P1① 行为实质偏离] + §4 Q1 真相源层级 + §7 衔接 + §8 过程纪律自检 + §9 真相源冻结 + §去重协议）
> 范式对齐：A4.1.18（`docs/audits/2026-08-06-1517-rc-ma4-a4-1-18-pc3-arap-reminder-nonblocking-runtime-behavior.md`，done — period-close 运行时行为评估同型工作项）
> 审计性质：**只读年末反结账阻断边界一致性评估**（读年末阻断 + hasNextYearPeriods + 年度凭证红冲 + yearOpening 存储/填充 + 手动删次年边界推理 + 既有测试普查；**不改代码/ORM/api.xml/真相源**）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）
> 审计 HEAD：`2cf41032f`

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.6 §7-4：年末反结账阻断边界——`ReverseCloseProcessor:32-36` 年末反结账阻断，若次年期间已手动删除但 `ErpFinGlBalance`/yearOpening 残留，反结账红冲年度结转凭证是否致次年年初余额与凭证不一致 |
| 年末阻断逻辑（写时实测） | `ErpFinAccountingPeriodReverseCloseProcessor.java:32-36` `if (facade.isYearEnd(period) && period.getYear() != null && facade.hasNextYearPeriods(period.getYear() + 1))` → `ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`（ARG_PERIOD_CODE + ARG_NEXT_YEAR）；`:31` 注释「年度结转反结账门控：若该期间为年末且次年期间已创建，阻止反结账（须先删次年期间）」 |
| hasNextYearPeriods（写时实测） | `ErpFinAccountingPeriodProcessor.java:128-134` `daoProvider.daoFor(ErpFinAccountingPeriod.class)` + `q.addFilter(eq("year", nextYear))` + `setLimit(1)` + `return !findAllByQuery(q).isEmpty()`——查次年**任意期间**是否存在（非特指 1 月） |
| isYearEnd（写时实测） | `ErpFinAccountingPeriodProcessor.java:121-125` `year != null && month != null && month == 12` |
| 年度凭证红冲（写时实测） | `ReverseCloseProcessor:46-49` `if (isYearEnd(period)) → reverseCloseVoucher(period, ANNUAL_BILL_CODE_PREFIX + code, PROFIT_TO_RETAINED_EARNINGS, context)`；`:553-562` `reverseCloseVoucher` 按 billCode+businessType 反查 VoucherBillR，存在则 `voucherBiz.reverse`，无则 no-op（`:558-560`）。**仅在年末阻断 :32-36 未触发时执行**（即无次年期间） |
| yearOpening 存储（写时实测） | `app-erp-finance.orm.xml:924-925` `yearOpeningDebit`/`yearOpeningCredit` 列直接存于 `erp_fin_gl_balance`；`:940` `to-one period` 关系**无 cascade 属性** → 删期间**不级联删** GlBalance 行 |
| yearOpening 填充（写时实测） | `AnnualCloseService.populateNextYearOpening:137-184`：`:142-146` nextJan 为空静默跳过；`:156-160` 幂等清快照 keys by `periodId = nextJan.getId()`；`:170` 新建 gl 行 `setPeriodId(nextJan.getId())`；`:180-181` `setYearOpeningDebit/Credit`；数据源 `aggregateYearSubjectActivity:293-317` 排除 `PROFIT_TO_RETAINED_EARNINGS`（:303） |
| **次年期间删除 API 普查（闭合存疑点关键变量）** | **不存在专用 API**——grep 全 `module-finance/` `deleteNextYear\|deletePeriod\|removePeriod\|deleteNextYearPeriods\|deleteAnnual` 生产代码 = **零命中**；边界场景须运维经**通用 CRUD delete 或直连 DB**删除次年 `ErpFinAccountingPeriod` 行（非常规运维路径） |
| **边界场景一致性裁决（本存疑点核心）** | **无数据不一致**——手动删次年期间后：①`hasNextYearPeriods` 返回 false（次年期间已删）→ 年末阻断失效 → 红冲年度结转凭证（恢复**本年**未分配利润，PROFIT_TO_RETAINED_EARNINGS 凭证红冲仅涉本年）；②残留 yearOpening 行孤立（keyed by 已删 nextJan.getId()）；③**残留行无消费方**——所有 GlBalance 读路径按 `periodId` 过滤（残留 periodId 指向已删期间，报表/`hasYearOpeningForNextJan` 查 valid periodId 取不到残留行）→ 无即时数据破坏 |
| 次年重建错配风险评估 | **不成立**——次年重建（`generateNextYearPeriods` 新建期间获**新 id**）+ `populateNextYearOpening:156-160` 幂等清快照 keys by **新** nextJan.getId()（不清旧残留[不同 periodId]）+ 新建行 periodId=新 nextJan.getId()；**旧残留 periodId=已删 id 永不被读**（读路径按新 periodId 过滤）→ 新行正确，旧残留孤立死行，**非错配致数据破坏**，而是**数据卫生问题**（孤立死行累积） |
| 测试覆盖边界（写时实测） | `TestErpFinAnnualClose#testReverseCloseBlockedWhenNextYearExists:77-85`（@Test :76，年末阻断覆盖，仅断言 `assertThrows(NopException.class)` 未断言 ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS 错误码/ARG_NEXT_YEAR 参数）+ `testReverseCloseReversesAnnualVoucherWhenNoNextYear:88-96`（@Test :87，**名实不符**——方法名承诺"红冲年度凭证"但仅断言年度结转凭证**生成** count>=1，未调 reverseClose 未禁用次年创建）+ 手动删次年边界场景测试 = **零覆盖** |
| RC-3/RC-4 边界场景运行时裁决（§2 判据 + 三源对照） | **维持接受 + 登记 P2-RC-084 watch-only（数据卫生/潜在风险）**——决策树分支①命中：边界场景须非常规运维[无专用删次年 API，须直连 DB/通用 CRUD] + 残留 yearOpening 孤立**无即时消费方**（periodId 指向已删期间，读路径按 valid periodId 过滤取不到残留行）→ 无即时数据破坏 + 次年重建错配**不成立**[旧残留 periodId 永不被读] → **存疑点核心问题"是否致次年年初余额与凭证不一致"答案 = 否**。主路径[次年存在→阻断 / 无次年→红冲无残留]接受维持（A1.6 §5.2 RC-3/RC-4 接受不撤销）；残留孤立死行累积 + 幂等清快照不覆盖旧残留 + 潜在 periodId-agnostic 查询污染风险 → 登记 P2 watch-only（数据卫生/潜在风险，非活跃数据破坏） |
| 新 finding | **1**（P2-RC-084，年末反结账边界 yearOpening 残留孤立 watch-only——与既有 finding 不同控制点：arm-index period-close 分区无"年末反结账边界 yearOpening 残留孤立"同控制点 finding；P1-MA2-018 = 年初余额非累计[不同机制：多年累计 vs 残留孤立]） |
| P0 即时通道 | 不触发（未出 P0/P1） |

**核心裁决**：存疑点 A1.6 §7-4 的年末反结账阻断边界一致性评估结论 = **存疑点核心问题"反结账红冲年度结转凭证是否致次年年初余额与凭证不一致"答案 = 否**，**维持 RC-3/RC-4 接受[正常路径] + 登记 P2-RC-084 watch-only[边界场景残留孤立数据卫生/潜在风险]**。判据三层（决策树分支①）：(1) **边界场景须非常规运维**——本报告 §2.2 主证据：grep 全 `module-finance/` 次年期间删除 API 零命中（`deleteNextYear`/`deletePeriod`/`removePeriod`/`deleteNextYearPeriods` 生产代码零命中），运维须经通用 CRUD delete 或直连 DB 删除次年 `ErpFinAccountingPeriod` 行（非常规 API 路径）；(2) **残留 yearOpening 孤立无即时消费方**——本报告 §2.3 主证据：删次年期间后红冲年度凭证仅涉**本年**（PROFIT_TO_RETAINED_EARNINGS 凭证红冲恢复本年未分配利润），次年残留 yearOpening 行 keyed by 已删 nextJan.getId()，所有 GlBalance 读路径按 periodId 过滤（报表 `loadGlBalances` / 测试 `hasYearOpeningForNextJan:219` / `populateNextYearOpening` 幂等清 :157 均按 valid periodId），残留 periodId 指向已删期间**永不被读** → 无即时数据破坏；(3) **次年重建错配不成立**——次年重建 `generateNextYearPeriods` 新建期间获**新 id**，`populateNextYearOpening:156-160` 幂等清快照 keys by **新** nextJan.getId()（不清旧残留[不同 periodId]），新建行 periodId=新 nextJan.getId()；旧残留 periodId=已删 id 永不被读 → 新行正确 + 旧残留孤立死行，**非错配致数据破坏，而是数据卫生问题**。存疑点核心问题"是否致次年年初余额与凭证不一致"经此三层推理**答案 = 否**（年度凭证红冲涉本年，次年 yearOpening 残留孤立无消费方无破坏，次年重建新行正确）。残留孤立死行累积 + 幂等清快照不覆盖旧残留 + 潜在 periodId-agnostic 查询污染风险 → 登记 P2-RC-084 watch-only（数据卫生/潜在风险，非活跃数据破坏），归 MR1 successor（纯 yearOpening 残留级联清理或 owner doc 标注可自动执行；触及 hasNextYearPeriods/populateNextYearOpening 逻辑[反结账属会计过账] 须评估 ask-first）。A1.6 §5.2 RC-3/RC-4 接受[正常路径]**不撤销**（边界场景是正常路径之外的残留风险，不撤销正常路径接受）。**本验证不实施修复**（§5 保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（§6 §1 / §1 L1，逐字引用）

> 来源：`docs/design/finance/use-cases.md`（L1 权威真相源，方法论 §4）。验收标准逐字引用，**禁止转述**（§1 L1 格式 + Q1 裁决根因守卫）。本验证只评 RC-3 年末阻断边界 + RC-4 年度凭证红冲，引用 A1.6 §1 UC-FIN-07 完整枚举。

**UC-FIN-07 反结账**（`use-cases.md:129`）RC-3 + RC-4 逐字（A1.6 §1 :81-82）：

```
CLOSED_FINAL → OPEN                      # RC-3
冲销: 结转凭证/折旧凭证/成本凭证          # RC-4（含年度结转凭证 PROFIT_TO_RETAINED_EARNINGS）
```

**RC-3 关联约束**（A1.6 §2.8 静态确认）：年末反结账阻断 `ReverseCloseProcessor:32-36`——`isYearEnd(period) && hasNextYearPeriods(year+1)` → `ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`（次年期间已创建时阻止年末反结账，须先删次年期间）。本阻断是 RC-3 状态迁移 `CLOSED_FINAL→OPEN` 的年末前置门控。

**RC-4 关联约束**（A1.6 §2.9 静态确认）：年末反结账时红冲年度结转凭证 `ReverseCloseProcessor:46-49`——`isYearEnd(period)` → `reverseCloseVoucher(ANNUAL-CLOSE- + PROFIT_TO_RETAINED_EARNINGS)`。

**存疑点核心问题**（A1.6 §7 存疑点 4）：年末阻断 `:32-36` 依赖 `hasNextYearPeriods`（查次年期间是否存在）。若次年期间被手动删除但 `ErpFinGlBalance` yearOpening 残留（由 `AnnualCloseService.populateNextYearOpening` 创建，keyed by nextJan.getId()），则阻断失效 + 红冲年度凭证 + 残留 yearOpening 孤立 → 是否致次年年初余额与凭证不一致。

**L2 设计参考**（`period-close.md §反结账流程 :170-228` + §年度结转规则 :240-282）：反结账步骤3 处理结转凭证（红冲恢复收入/费用科目余额）+ 年度结转步骤3 结转本年利润→未分配利润 + 步骤4 辅助账结转 + 步骤5 新开期间（自动创建次年 12 期间）。L2 未显式描述"手动删次年期间后反结账"边界场景——属实现层边界（`ReverseCloseProcessor:31` 注释「须先删次年期间」是唯一文档痕迹）。

---

## 2. 实现证据（§6 §2 / §1 L3，写时实测 `2cf41032f`）

### 2.1 年末反结账阻断逻辑核验（Phase 1 item 1）

> 核验目标：证实 `ReverseCloseProcessor:32-36`（`isYearEnd && hasNextYearPeriods(year+1) → ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`）+ `hasNextYearPeriods:128-134`（查次年任意期间 setLimit(1)）+ `isYearEnd:121-125`（month==12）——确认正常路径（次年期间存在 → 阻断）行为正确。

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| 年末阻断门控 | `module-finance/erp-fin-service/.../processor/ErpFinAccountingPeriodReverseCloseProcessor.java:32-36` | `:32` `if (facade.isYearEnd(period) && period.getYear() != null && facade.hasNextYearPeriods(period.getYear() + 1))` → `:33-35` `throw new NopException(ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS).param(ARG_PERIOD_CODE, ...).param(ARG_NEXT_YEAR, period.getYear()+1)`；`:31` 注释「年度结转反结账门控：若该期间为年末且次年期间已创建，阻止反结账（须先删次年期间）」 | ✅ |
| isYearEnd | `ErpFinAccountingPeriodProcessor.java:121-125` | `:122-124` `return period.getYear() != null && period.getMonth() != null && period.getMonth() == 12;`；`:120` javadoc「判定期间是否为年末（year 非空且 month=12）」 | ✅ |
| hasNextYearPeriods | `ErpFinAccountingPeriodProcessor.java:128-134` | `:129` `daoProvider.daoFor(ErpFinAccountingPeriod.class)`；`:131` `q.addFilter(eq("year", nextYear))`；`:132` `q.setLimit(1)`；`:133` `return !dao.findAllByQuery(q).isEmpty();`——查次年**任意期间**是否存在（非特指 1 月），setLimit(1) 返回非空即 true | ✅ |

**年末阻断逻辑核验结论**：`ReverseCloseProcessor:32-36` 年末阻断 = `isYearEnd(month==12) && hasNextYearPeriods(查次年任意期间 setLimit(1) 非空)` → `ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS`（ARG_PERIOD_CODE + ARG_NEXT_YEAR）——证实正常路径（次年期间存在 → 阻断年末反结账）行为正确。A1.6 §2.8 静态确认经 HEAD `2cf41032f` 复核**无回退**（行号与 A1.6 §2.8 / plan Current Baseline 精确一致：阻断 :32-36 / isYearEnd :121-125 month==12 / hasNextYearPeriods :128-134 setLimit(1) 查次年任意期间）。

### 2.2 次年期间删除 API 普查（Phase 1 item 2 关键变量，闭合存疑点触发条件）

> 核验目标：grep 次年期间删除相关 API/config 全集——确认"手动删次年期间"这一边界场景触发条件是常规 API 路径还是非常规运维。**这是存疑点触发条件可达性的核心证据。**

| 普查维度 | grep pattern（写时实测） | 命中（生产代码） | 裁决 |
|---|---|---|---|
| 次年期间删除 API 字面变体 | `rg 'deleteNextYear\|deletePeriod\|removePeriod\|deleteNextYearPeriods\|deleteAnnual' module-finance/` | **0** | 无专用次年期间删除 API |
| ErpFinAccountingPeriodBizModel 删除方法 | `rg 'delete\|remove' module-finance/erp-fin-service/.../entity/ErpFinAccountingPeriodBizModel.java` | **0 业务删除方法**（Facade 仅有 preCheck/closePeriod/finalizePeriod/reverseClose/openPeriod/generateNextYearPeriods 6 mutation，无 delete 类） | 无业务级期间删除入口 |

**次年期间删除 API 普查结论（闭合存疑点触发条件）**：边界场景"手动删次年期间"**无专用业务 API**——(a) grep 全 `module-finance/` `deleteNextYear`/`deletePeriod`/`removePeriod`/`deleteNextYearPeriods`/`deleteAnnual` 生产代码零命中；(b) `ErpFinAccountingPeriodBizModel` Facade 无 delete 类 mutation（仅 6 个结账/反结账/开期 mutation）。运维须经**通用 CRUD delete（若启用）或直连 DB** 删除次年 `ErpFinAccountingPeriod` 行——**非常规 API 路径**，属运维干预操作。`ReverseCloseProcessor:31` 注释「须先删次年期间」是唯一文档痕迹，描述的是运维干预步骤而非系统 API。结论：存疑点触发条件"手动删次年期间"**非常规 API 可达**，须运维干预（直连 DB/通用 CRUD），非默认活跃路径。

### 2.3 手动删次年期间边界场景一致性评估（Phase 1 item 2，本存疑点核心）

> 核验目标：核验边界场景运行时行为——运维手动删次年 `ErpFinAccountingPeriod` 12 行但 `ErpFinGlBalance` 次年 1 月 yearOpening 行残留（keyed by 已删 nextJan.getId()）→ ①`hasNextYearPeriods` 返回 false → 年末阻断失效；②红冲年度结转凭证（恢复本年未分配利润）；③残留 yearOpening 孤立（无所属期间）。评估残留 yearOpening 实际数据一致性影响。

#### 2.3.1 边界场景运行时行为链（写时实测推理）

| 步骤 | 证据（写时实测） | 行为 |
|---|---|---|
| ①运维删次年期间 | `ErpFinAccountingPeriod` 次年 12 行被删（直连 DB/通用 CRUD，§2.2 证实无专用 API） | 次年 `ErpFinAccountingPeriod` 行物理删除；`ErpFinGlBalance` 次年 1 月 yearOpening 行**不被删**（`orm.xml:940` period to-one 关系**无 cascade** → 删期间不级联删 GlBalance） |
| ②hasNextYearPeriods 返回 false | `ErpFinAccountingPeriodProcessor:128-134` 查 `ErpFinAccountingPeriod` where year=nextYear | 次年期间已删 → `findAllByQuery(q).isEmpty()` = true → `return false` |
| ③年末阻断失效 | `ReverseCloseProcessor:32` `if (isYearEnd && hasNextYearPeriods)` | hasNextYearPeriods=false → 条件不满足 → **阻断不触发**，反结账继续 |
| ④红冲年度结转凭证 | `ReverseCloseProcessor:46-49` `if (isYearEnd(period)) → reverseCloseVoucher(period, ANNUAL-CLOSE-+code, PROFIT_TO_RETAINED_EARNINGS, context)` | `reverseCloseVoucher:553-562` 按 billCode+businessType 反查 VoucherBillR，存在则 `voucherBiz.reverse`——红冲**本年**年度结转凭证（恢复本年未分配利润 / 本年利润科目余额） |
| ⑤残留 yearOpening 孤立 | `ErpFinGlBalance` 次年 1 月 yearOpening 行（keyed by 已删 nextJan.getId()）残留 | 残留行 `periodId` = 已删 nextJan.getId()（指向已不存在的期间）→ **孤立死行**（无所属期间） |

#### 2.3.2 残留 yearOpening 数据一致性影响评估（存疑点核心问题裁决）

| 评估维度 | 证据（写时实测） | 评估结论 |
|---|---|---|
| 红冲范围（涉本年 vs 次年） | `ReverseCloseProcessor:46-49` 红冲 `PROFIT_TO_RETAINED_EARNINGS` 凭证 = **本年**年度结转凭证（`ANNUAL-CLOSE-{本年期间code}`，由 `AnnualCloseService.transferProfitToRetainedEarnings:94-130` 生成，periodId=本年 12 月期间）；`reverseCloseVoucher:553-562` 按本年 billCode 反查 VoucherBillR | ✅ 红冲仅涉**本年**（恢复本年未分配利润 / 本年利润科目），**不触及次年 yearOpening 行**（yearOpening 在次年 GlBalance，红冲操作本年凭证） |
| 残留 yearOpening 消费方（次年不存在时） | 所有 GlBalance 读路径按 periodId 过滤：报表 `ErpFinReportBizModel.loadGlBalances`（A1.7 P2-RC-008 实测 `:396/402` `eq("periodId", ...)`）；测试 `hasYearOpeningForNextJan:218-219` `q.addFilter(eq("periodId", nextJan.getId()))`；`populateNextYearOpening:157` 幂等清 `eq("periodId", nextJan.getId())` | ✅ 残留行 periodId 指向已删期间，**次年不存在则无消费方**（报表按 valid periodId 查取不到残留行）→ **无即时数据破坏** |
| 次年重建错配风险 | 次年重建 `generateNextYearPeriods` 新建期间获**新 id**（自增 seq）；`populateNextYearOpening:156-160` 幂等清快照 `clearQ.addFilter(eq("periodId", nextJan.getId()))` keys by **新** nextJan.getId()（不清旧残留[不同 periodId]）；`:170` 新建行 `setPeriodId(nextJan.getId())` = 新 id | ✅ **错配不成立**——新 nextJan.getId() ≠ 旧残留 periodId，幂等清快照不清旧残留，但旧残留 periodId 永不被读（读路径按新 periodId 过滤）→ 新行正确 + 旧残留孤立死行 → **非错配致数据破坏，而是数据卫生问题**（孤立死行累积） |

**边界场景一致性裁决（存疑点核心问题）**：存疑点核心问题"反结账红冲年度结转凭证是否致次年年初余额与凭证不一致"经三层推理**答案 = 否**：
1. **红冲范围仅涉本年**——`:46-49` 红冲 `PROFIT_TO_RETAINED_EARNINGS` 本年年度结转凭证（恢复本年未分配利润），不触及次年 yearOpening 行；
2. **残留 yearOpening 孤立无即时消费方**——次年期间已删，残留行 periodId 指向已删期间，所有 GlBalance 读路径按 valid periodId 过滤取不到残留行 → 无即时数据破坏；
3. **次年重建错配不成立**——次年重建新期间获新 id，幂等清快照 keys by 新 periodId（不清旧残留），但旧残留 periodId 永不被读 → 新行正确 + 旧残留孤立死行（数据卫生问题，非数据破坏）。

**结论**：边界场景**不致次年年初余额与凭证不一致**（存疑点核心问题答案 = 否）。残留 yearOpening 为孤立死行（数据卫生/潜在风险），非活跃数据破坏。

### 2.4 yearOpening 存储与填充核验（Phase 1 item 3）

> 核验目标：证实 `ErpFinGlBalance` yearOpening 直接存储（orm.xml:924-925）+ `AnnualCloseService.populateNextYearOpening:137-184`（创建 gl 行 periodId=nextJan.getId() + 幂等清快照 + nextJan 为空静默跳过 + 数据源全年 VoucherLine 聚合排除 PROFIT_TO_RETAINED_EARNINGS）——确认 yearOpening 由年度结账填充，手动删次年期间不清 yearOpening 行（无级联删除）。

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| yearOpening 直接存储 | `app-erp-finance.orm.xml:924-925` | `:924` `<column name="yearOpeningDebit" ... stdSqlType="DECIMAL" precision="20" scale="4"/>` + `:925` `<column name="yearOpeningCredit" .../>`——yearOpeningDebit/Credit 列直接存于 `erp_fin_gl_balance`（非独立结构） | ✅ |
| period to-one 关系（无级联） | `app-erp-finance.orm.xml:940` | `:940` `<to-one name="period" refEntityName="...ErpFinAccountingPeriod" tagSet="pub"><join><on leftProp="periodId" rightProp="id"/></join></to-one>`——**无 cascade 属性** → 删 `ErpFinAccountingPeriod` 期间**不级联删** `ErpFinGlBalance` 行 | ✅ |
| populateNextYearOpening 入口 | `AnnualCloseService.java:137-184` | `:138-141` year null 检查；`:142` `findNextYearJanuaryPeriod(year+1, orgId)`；`:143-146` nextJan 为空**静默跳过**（`return`，不阻断结转）；`:150` `aggregateYearSubjectActivity(year)` 聚合本年科目净额 | ✅ |
| 幂等清快照（keys by nextJan.getId()） | `AnnualCloseService.java:156-160` | `:157` `clearQ.addFilter(eq("periodId", nextJan.getId()))`；`:158-160` 遍历删除既有次年 1 月 GlBalance 行——**幂等清快照 keys by periodId=nextJan.getId()**（次年重建新 id 时不清旧残留[不同 periodId]） | ✅ |
| 新建 yearOpening 行 | `AnnualCloseService.java:167-183` | `:170` `gl.setPeriodId(nextJan.getId())`；`:180` `gl.setYearOpeningDebit(net > 0 ? net : ZERO)`；`:181` `gl.setYearOpeningCredit(net < 0 ? net.negate() : ZERO)`；`:182` `glDao.saveEntity(gl)` | ✅ |
| 数据源（全年 VoucherLine 聚合，排除 PROFIT_TO_RETAINED_EARNINGS） | `AnnualCloseService.java:293-317` | `:303` `excludeTypes.add(ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS.name())`；`:309-311` 跳过 PROFIT_TO_RETAINED_EARNINGS 分录——**数据源 = 全年已过账非红冲 VoucherLine 聚合，排除年度结转自身凭证** | ✅ |
| 接线 | `ClosePeriodProcessor` 年度分支 → `annualCloseService.executeAnnualClose:74-82` → `executeAnnualCloseForSchema:84-88` → `populateNextYearOpening:86` | 年度结账自动创建次年期间（config `auto-generate-next-year-periods` 默认 true）+ 填充次年 1 月 yearOpening | ✅ |

**yearOpening 存储与填充核验结论**：yearOpeningDebit/Credit 直接存于 `erp_fin_gl_balance`（:924-925）；`populateNextYearOpening:137-184` 由年度结账填充（periodId=nextJan.getId() + 幂等清快照 keys by nextJan.getId() + nextJan 为空静默跳过 + 数据源全年 VoucherLine 聚合排除 PROFIT_TO_RETAINED_EARNINGS）；`orm.xml:940` period to-one 关系**无 cascade** → **手动删次年期间不清 yearOpening 行**（无级联删除，残留行孤立）。A1.6 §2.8/§2.9 + plan Current Baseline 经 HEAD `2cf41032f` 复核**无回退**。

### 2.5 年度结转凭证红冲核验（Phase 1 item 4）

> 核验目标：证实 `ReverseCloseProcessor:46-49`（`isYearEnd → reverseCloseVoucher(ANNUAL-CLOSE- + PROFIT_TO_RETAINED_EARNINGS)`）+ `reverseCloseVoucher:553-562`（按 billCode+businessType 反查 VoucherBillR，存在则 reverse，无则 no-op）——确认红冲仅恢复本年未分配利润，不触及次年 yearOpening 行。

| 环节 | 文件:行（写时实测） | 关键行为断言 | 核验状态 |
|---|---|---|---|
| 年度凭证红冲触发 | `ErpFinAccountingPeriodReverseCloseProcessor.java:46-49` | `:46` `if (facade.isYearEnd(period))` → `:47-48` `facade.reverseCloseVoucher(period, ANNUAL_BILL_CODE_PREFIX + period.getCode(), ErpFinBusinessType.PROFIT_TO_RETAINED_EARNINGS, context)`——**仅在年末阻断 :32-36 未触发时执行**（即无次年期间） | ✅ |
| reverseCloseVoucher 反查 + 红冲 | `ErpFinAccountingPeriodProcessor.java:553-562` | `:555` `daoProvider.daoFor(ErpFinVoucherBillR.class)`；`:557` `q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())))`；`:558-560` `if (findAllByQuery(q).isEmpty()) return;`（无则 no-op）；`:561` `voucherBiz.reverse(billHeadCode, businessType, context)`（存在则红冲） | ✅ |
| 红冲范围（本年 vs 次年） | billCode = `ANNUAL-CLOSE-{本年期间code}`（由 `AnnualCloseService.transferProfitToRetainedEarnings:125` `BILL_CODE_PREFIX + period.getCode()` 生成，periodId=本年 12 月期间）；红冲操作 `voucherBiz.reverse` 红冲本年年度结转凭证 | ✅ 红冲仅恢复**本年**未分配利润（PROFIT_TO_RETAINED_EARNINGS 凭证红冲），**不触及次年 yearOpening 行**（yearOpening 在次年 GlBalance，红冲操作本年凭证） |

**年度凭证红冲核验结论**：`:46-49` 红冲 `PROFIT_TO_RETAINED_EARNINGS` 本年年度结转凭证 + `:553-562` 按 billCode+businessType 反查 VoucherBillR（存在则 reverse，无则 no-op）——证实红冲仅恢复本年未分配利润，**不触及次年 yearOpening 行**。A1.6 §2.9 静态确认经 HEAD `2cf41032f` 复核**无回退**。

### 2.6 MA4↔A5.6 边界声明（Phase 1 item 6）

> 方法论 §去重协议 MA4↔A5.6 边界：MA4 审「行为是否符合需求」（年末反结账边界是否致数据不一致）；A5.6（audit-remediation）审「E2E 断言强度」（测试质量视角，全量评级）。

**本验证边界执行声明**：

- 本验证审「年末反结账边界是否符合 UC-FIN-07 RC-3/RC-4 数据一致性要求」——**需求契约视角**。裁决依据 = §2 判据（边界场景触发条件可达性 + 残留 yearOpening 消费方 + 次年重建错配风险）。
- 本验证**不重做 A5.6 E2E 断言强度审计**（A5.6 已对全量 spec 做断言强度分类矩阵）。本验证只评年末反结账边界一致性这一具体控制点。
- 裁决为「维持接受 + 登记 P2 watch-only」→ P2-RC-084 是**数据卫生/潜在风险** successor（纯 yearOpening 残留级联清理或 owner doc 标注可自动执行）；名实不符测试 `testReverseCloseReversesAnnualVoucherWhenNoNextYear` + 年末阻断错误码断言缺口属 A5.6 测试质量维度 successor（纯测试代码 MR1 预授权类目），**非本 MA4 范围**。

---

## 3. 测试证据（§6 §3 / §1 L4，断言强度标注）

### 3.1 测试覆盖边界普查（Phase 1 item 5）

> grep `testReverseCloseBlockedWhenNextYearExists`（年末阻断覆盖）+ `testReverseCloseReversesAnnualVoucherWhenNoNextYear`（名实不符）+ 手动删次年边界场景测试全集。引用 A1.6 §3.6 已有评级依据。

| 测试方法 | 文件:行（写时实测） | 覆盖范围 | 断言强度 | RC-3/RC-4 年末相关覆盖 |
|---|---|---|---|---|
| `testReverseCloseBlockedWhenNextYearExists` | `TestErpFinAnnualClose.java:77-85`（@Test :76） | 年末反结账阻断（次年期间已存在 → assertThrows） | **中**（`:83` `assertThrows(NopException.class, () -> ...reverseClose(periodId, CTX))` + `:84` message "次年期间已存在时反结账被阻止"） | ⚠ **年末阻断覆盖但断言浅**——仅断言 `assertThrows(NopException.class)`，**未断言具体 `ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS` 错误码 / `ARG_NEXT_YEAR` 参数**（如 `assertEquals(ErpFinErrors.ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS, e.getErrorCode())`） |
| `testReverseCloseReversesAnnualVoucherWhenNoNextYear` | `TestErpFinAnnualClose.java:88-96`（@Test :87） | 年度结转凭证生成（无次年场景） | **中**（`:94-95` `assertTrue(countVouchersByBillCode("ANNUAL-CLOSE-2025-12", PROFIT_TO_RETAINED_EARNINGS) >= 1)`） | ❌ **名实不符**——方法名承诺"反结账红冲年度结转凭证[WhenNoNextYear]"但方法体：①**未调 `reverseClose`**（仅 `closePeriod` :93）；②**未禁用次年创建**（默认 `annual-close-test.yaml` auto-generate=true，次年仍创建）；③仅断言年度结转凭证**生成** count>=1（:94-95），**未断言红冲**。方法名承诺的"红冲"路径完全未验证 |
| `testAnnualCloseTransferProfitToRetainedEarnings` | `TestErpFinAnnualClose.java:46-73`（@Test :45） | 12 月结账 → 本年利润清零 + 未分配利润累计 + PROFIT_TO_RETAINED_EARNINGS 凭证 + 次年 12 期间 + yearOpening | **深**（数值精确 + 期间计数 + 状态 + `hasYearOpeningForNextJan:72`） | ✅ 年度结转凭证生成 + 次年期间创建 + yearOpening populate 深覆盖（正常路径） |
| 手动删次年边界场景测试 | grep 全 `module-finance/erp-fin-service/src/test` `deleteNextYear\|deletePeriod\|手动删次年\|边界` | **0 命中** | — | ❌ **缺口**（边界场景零覆盖；§2.2 一致——无专用删次年 API 故无测试） |

**测试覆盖边界清单**：

1. **年末阻断覆盖** ⚠——`testReverseCloseBlockedWhenNextYearExists:83` 覆盖年末阻断（assertThrows），但断言浅（未断言具体错误码 ERR_REVERSE_CLOSE_NEXT_YEAR_EXISTS / ARG_NEXT_YEAR 参数）。A1.6 §3.6 已记「仅断言 NopException 未断言具体错误码/参数」。
2. **名实不符测试缺口** ❌——`testReverseCloseReversesAnnualVoucherWhenNoNextYear:88-96` 方法名承诺"红冲年度凭证[无次年场景]"但方法体仅断言年度结转凭证**生成**（未调 reverseClose + 未禁用次年创建）。方法名承诺的红冲路径完全未验证——实际测试的是 `testAnnualCloseTransferProfitToRetainedEarnings` 已覆盖的"年度结转凭证生成"。
3. **手动删次年边界场景测试缺口** ❌——无专用删次年 API（§2.2）故无对应测试；本验证边界场景一致性经静态推理裁决（§2.3，无需运行时重现——存疑点核心问题答案 = 否）。

**断言强度评级**：RC-3/RC-4 年末相关测试覆盖 = **正常路径深[年度结转凭证生成 + 次年期间 + yearOpening] + 年末阻断中[assertThrows 浅断言] + 名实不符测试缺口 + 边界场景零覆盖**。年末阻断功能行为正确（assertThrows 证实阻断触发）；名实不符测试 + 错误码断言缺口 + 边界场景缺口属测试覆盖补强项（A5.6 维度），非合规缺陷（年末阻断运行时行为已由 assertThrows 间接证实；边界场景一致性经 §2.3 静态推理裁决无数据破坏）。

---

## 4. 运行时行为证据（§6 §4 / §1 L5）

### 4.1 MA2/A1.6 复用（§去重协议）

| 已证实行为 | 引用 | 本验证复用判定 |
|---|---|---|
| RC-3 年末反结账阻断（CLOSED_FINAL→OPEN + 年末门控） | A1.6 §2.8 + §5.2 RC-3 接受 + A2.3 period-close E2E `2026-07-27-1949-arm-ma2-period-close-e2e.md:95,114`（年末次年期间已创建阻止反结账 :284-288[pre-R6.1 行号]） | ✅ 复用（年末阻断正常路径行为已证实）；本验证只补「手动删次年边界场景」差异 |
| RC-4 年度结转凭证冲销（PROFIT_TO_RETAINED_EARNINGS 红冲） | A1.6 §2.9 + §5.2 RC-4 接受 + A2.3 period-close E2E（年末结转凭证红冲） | ✅ 复用（年度凭证红冲正常路径行为已证实）；本验证闭合边界场景数据一致性裁决 |
| yearOpening 存储与填充 | A1.6 §2.8（orm.xml:924-925）+ A2.5b `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md:27`（populateNextYearOpening）+ A2.3 P1-MA2-018（年初余额非累计） | ✅ 复用（yearOpening 存储与填充机制已证实）；本验证补「删期间无级联→残留孤立」差异 |

**声明**：本验证只补「手动删次年期间边界场景一致性推理 + 次年期间删除 API census + RC-3/RC-4 边界场景运行时裁决 + 名实不符测试普查」差异（A1.6 §7 存疑点 4 标注为「需运行时探针」），不重新核实 RC-3/RC-4 正常路径行为本身（A1.6 §2.8/§2.9 + A2.3 已证实）。

### 4.2 本切片运行时行为增量

本验证相对 A1.6/A2.3 的**运行时行为增量**：

1. **次年期间删除 API census**（A1.6 §7-4 触发条件"手动删次年期间"未核实）：§2.2 grep census 证实**无专用 API**——触发条件"手动删次年期间"须运维干预（直连 DB/通用 CRUD），**非常规 API 路径**。
2. **边界场景一致性裁决**（A1.6 §7-4 核心问题"是否致次年年初余额与凭证不一致"未裁决）：§2.3 三层推理裁决**答案 = 否**——红冲仅涉本年 + 残留 yearOpening 孤立无消费方 + 次年重建错配不成立。
3. **次年重建错配风险评估**（A1.6 §7-4 未评估）：§2.3.2 证实次年重建新期间获新 id，幂等清快照 keys by 新 periodId（不清旧残留），旧残留 periodId 永不被读 → 错配不成立（数据卫生问题，非数据破坏）。

---

## 5. 符合性结论（§6 §5 / §2 判据 + 三源对照）

### 5.1 RC-3/RC-4 边界场景运行时裁决（Phase 1 item 7，方法论 §2 判据 + plan 决策树两分支）

| 决策分支 | 判据条件（plan Phase 1 item 7） | 本验证结果 | 命中 |
|---|---|---|---|
| **① 维持接受 + 登记 P2 watch-only（数据卫生/潜在风险）** | 手动删次年期间非常规运维 **且** 残留 yearOpening 孤立无即时消费方（次年不存在则报表取不到行，无即时数据破坏）→ 登记 P2 watch-only（§2 P2① 次要验收标准边界弱） | (a) 手动删次年期间**无专用 API**（§2.2 grep census 证实）→ 非常规运维（须直连 DB/通用 CRUD）✅；(b) 残留 yearOpening 孤立**无即时消费方**（§2.3.2 所有 GlBalance 读路径按 valid periodId 过滤，残留 periodId 指向已删期间永不被读）✅；(c) **无即时数据破坏**（红冲仅涉本年 + 残留无消费方）✅；(d) 次年重建错配**不成立**（新期间获新 id + 幂等清快照不清旧残留 + 旧残留 periodId 永不被读 → 数据卫生问题非数据破坏）✅；(e) 残留孤立死行累积 + 幂等清快照不覆盖旧残留 + 潜在 periodId-agnostic 查询污染风险 → 登记 P2 watch-only ✅ | **命中** |
| ② 升 P1（须修复） | 残留 yearOpening 在次年重建时错配致数据破坏（populateNextYearOpening 幂等清快照不覆盖旧残留）→ 登记 P1（触及一致性，归 MR1，§2 P1①） | 次年重建错配**不成立**（§2.3.2：新期间获新 id，旧残留 periodId 永不被读 → 新行正确 + 旧残留孤立死行，**非错配致数据破坏，而是数据卫生问题**）→ **无活跃数据破坏** | 否 |

**裁决 = ① 维持 RC-3/RC-4 接受[正常路径] + 登记 P2-RC-084 watch-only[边界场景残留孤立数据卫生/潜在风险]**。

> **裁决理由（决策树两分支的关键区分）**：plan 决策树分支②的 P1 升级须满足「残留 yearOpening 在次年重建时错配致数据破坏」——即 populateNextYearOpening 幂等清快照不覆盖旧残留须导致**实际数据破坏**（报表读到错配行）。本验证 §2.3.2 证实次年重建时新期间获**新 id**（自增 seq），幂等清快照 `:157` keys by **新** nextJan.getId()（不清旧残留[不同 periodId]），新建行 `:170` periodId=新 nextJan.getId()——**旧残留 periodId=已删 id 永不被读**（所有 GlBalance 读路径按新 periodId 过滤）→ 新行正确 + 旧残留孤立死行 → **非错配致数据破坏，而是数据卫生问题**（孤立死行累积）。故分支②「错配致数据破坏」条件**不成立**，分支①匹配。**残留孤立死行无消费方 + 次年重建新行正确 = 无活跃数据破坏 = 分支①匹配的核心 hinge**。存疑点核心问题"是否致次年年初余额与凭证不一致"经此裁决**答案 = 否**（红冲涉本年 + 残留孤立无消费方 + 次年重建新行正确）。与 A1.6 §5.2 RC-3/RC-4 接受[正常路径]分层一致——本验证补**边界路径**（手动删次年 + 残留）差异，**不撤销正常路径接受**（次年存在→阻断 / 无次年→红冲无残留 主路径接受维持）。

### 5.2 §2 判据编号 + 三源 + 分层一致性

- **§2 判据**：P2①（次要验收标准边界场景弱——主路径[次年存在→阻断 / 无次年→红冲无残留]OK，边界[手动删次年非常规运维 + 残留孤立死行累积]弱；无活跃数据破坏[红冲涉本年 + 残留无消费方 + 次年重建新行正确]）。
- **§4 Q1**：L1 RC-3/RC-4 正常路径接受维持（A1.6 §5.2）；边界场景是正常路径之外的残留风险，不撤销正常路径接受。
- **L1/L2/L3 三源**：L1 `use-cases.md:129` RC-3/RC-4（CLOSED_FINAL→OPEN + 冲销结转凭证[含年度结转]）/ L2 `period-close.md §反结账流程 :170-228` + `§年度结转规则 :240-282`（设计参考，未显式描述手动删次年边界）/ L3 §2.1-§2.5（年末阻断 :32-36 + hasNextYearPeriods :128-134 + 年度凭证红冲 :46-49 + yearOpening 存储 :924-925 + populateNextYearOpening :137-184 + period 关系无 cascade :940）。
- **与 A1.6 §5.2 RC-3/RC-4 接受[正常路径]分层一致**：A1.6 §5.2 RC-3/RC-4 接受是**正常路径**（次年存在→阻断 / 无次年→红冲无残留）；本验证补**边界路径**（手动删次年 + 残留孤立）差异——边界路径残留无活跃数据破坏 → 维持接受 + 登记 P2 watch-only（不撤销正常路径接受）。
- **与 P1-MA2-018（年初余额非累计）不同控制点/不同机制**：P1-MA2-018 = populateNextYearOpening 仅聚合**本年度**分录净额（非累计），资产负债类科目缺上年结转额（多年累计机制缺陷）；P2-RC-084 = 手动删次年期间后 yearOpening 行孤立（残留孤立机制，keyed by 已删 periodId）。**不同机制**（多年累计 vs 残留孤立），不同控制点，不合并。
- **与 P1-RC-006（反结账审计轨迹）/ P2-RC-007（反结账成本凭证冲销缺失）不同控制点**：P1-RC-006 = 反结账操作审计轨迹（操作人/原因/时间）；P2-RC-007 = RC-6 成本凭证冲销缺失（INV costing 无 finance 侧期间凭证可冲）；P2-RC-084 = 年末反结账边界 yearOpening 残留孤立（数据卫生）。三者互补不重复。
- **与 A2.3 period-close E2E 分层一致**：A2.3 证实年末反结账正常路径行为（年末次年期间已创建阻止反结账 + 年末结转凭证红冲）；本验证只补边界场景（手动删次年 + 残留）一致性裁决，不重审正常路径行为。

### 5.3 与同族 A4.1 工作项裁决分层对照

| 同族工作项 | 存疑点性质 | 裁决 | 与本验证（A4.1.21）区分 |
|---|---|---|---|
| A4.1.18（done） | PC-3 AR/AP reminder 模式 L1 限定词活跃性 | 维持 P2-RC-006（强制核销 config 不存在→L1 限定词不活跃） | A4.1.18 是 L1 限定词活跃性裁决（需求契约维度）→ 维持 P2；A4.1.21 是边界场景数据一致性裁决（运行时数据卫生维度）→ 维持接受 + 新建 P2。同属 A4.1 MA4 运行时行为验证族 period-close 子集，控制点不同 |
| A4.1.19（done） | PC-4 资产折旧 auto-execute + 悬挂阻断交互 | 维持 PC-4 接受（rethrow + 悬挂扫描双路径有效阻断） | A4.1.19 是结账侧异常传播交互（行为达成）；A4.1.21 是反结账侧边界一致性（数据卫生）。结账 vs 反结账不同方向 |
| A4.1.20（done） | RC-9 反结账审计缺失降级证据 | 维持 P1-RC-006（降级证据存在但 reason 不可追合规硬伤） | A4.1.20 是审计轨迹维度（P1）；A4.1.21 是数据一致性维度（P2 watch-only）。不同控制点 |

**分层一致**：A4.1.21 与 A4.1.18/19/20 同属 A4.1 MA4 运行时行为验证族 period-close 子集，结论差异源于控制点性质（数据卫生 vs L1 限定词 vs 异常传播 vs 审计轨迹），无矛盾。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增裁决）

> 产出 finding 前 grep `arm-index.md` finance period-close / 反结账 / yearOpening / 年末 / 残留 同域同控制点。本验证裁决 = 维持 RC-3/RC-4 接受 + **新建 1 项 P2 finding**（P2-RC-084）。

### 6.1 grep 比对结果

| 候选既有 finding | 控制点 | 与本验证关系 | 裁决 |
|---|---|---|---|
| **P1-RC-006**（arm-index :136）UC-FIN-07⑨ RC-9 反结账审计轨迹缺失 | 反结账操作审计轨迹（操作人/原因/时间） | **不同控制点**（审计轨迹记录机制 vs yearOpening 残留孤立数据卫生）；A4.1.20 已评估降级证据 | 不合并（不同控制点） |
| **P2-RC-007**（arm-index :138）UC-FIN-07⑦ RC-6 反结账成本凭证冲销缺失 | INV costing 无 finance 侧期间凭证可冲 | **不同控制点**（成本凭证冲销缺失 vs yearOpening 残留孤立） | 不合并（不同控制点） |
| **P1-MA2-018**（arm-index，resolved documented simplification）年初余额非累计 | populateNextYearOpening 仅聚合本年度（非累计），资产负债类科目缺上年结转额 | **不同机制/不同控制点**：P1-MA2-018 = 多年累计机制缺陷（aggregateYearSubjectActivity 仅本年）；P2-RC-084 = 残留孤立机制（删期间无级联→yearOpening 行 keyed by 已删 periodId 孤立）。**不同机制**（多年累计 vs 残留孤立） | 不合并（不同机制/不同控制点） |
| A1.6 §5.2 RC-3/RC-4 接受[正常路径] | UC-FIN-07 RC-3/RC-4 正常路径接受 | 本验证是其**边界路径差异**，确认正常路径接受维持 | 复用（分层一致，确认正常路径接受维持，不撤销） |
| A2.3 period-close E2E（P1-MA2-018 + 年末反结账正常路径） | 年末反结账正常路径行为 | 本验证复用其已证实正常路径行为，不重审 | 复用（§去重协议） |

grep `arm-index.md` 「年末反结账边界」「yearOpening 残留」「yearOpening 孤立」「reverse-close boundary」RC 系列 = **零新控制点命中**（P1-RC-006/P2-RC-007/P1-MA2-018 均不同控制点/不同机制）。**「年末反结账边界 yearOpening 残留孤立」是新控制点**。

### 6.2 P2-RC-084 新建 finding（Phase 2 item 1）

**裁决**：**新建 P2-RC-084（P2 watch-only，年末反结账边界 yearOpening 残留孤立——数据卫生/潜在风险）**。控制点 = 手动删次年期间后 `ErpFinGlBalance` yearOpening 行孤立（keyed by 已删 periodId，无级联删除 + 无消费方 + 幂等清快照不覆盖）。与既有 finding 不同控制点/不同机制：
- vs P1-MA2-018（年初余额非累计）：不同机制（残留孤立 vs 多年累计）；
- vs P1-RC-006（反结账审计轨迹）/ P2-RC-007（反结账成本凭证冲销缺失）：不同控制点。

**P2-RC-084 finding 内容**（写入 arm-index RC 发现追踪分区）：

> **`P2-RC-084`** | rc-ma1-a1-6-finance-f6-period-close | finance | UC-FIN-07 RC-3/RC-4 边界 | **年末反结账边界 yearOpening 残留孤立（数据卫生/潜在风险）**：边界场景——运维手动删次年 `ErpFinAccountingPeriod` 12 行（无专用 API，§A4.1.21 证实须直连 DB/通用 CRUD）但 `ErpFinGlBalance` 次年 1 月 yearOpening 行残留（`orm.xml:940` period to-one 无 cascade → 删期间不级联删 GlBalance）。运行时行为：①`hasNextYearPeriods:128-134` 返回 false → 年末阻断 `:32-36` 失效；②红冲年度结转凭证 `:46-49`（恢复**本年**未分配利润，不涉次年）；③残留 yearOpening 行孤立（keyed by 已删 nextJan.getId()）。**无活跃数据破坏**：残留行 periodId 指向已删期间，所有 GlBalance 读路径按 valid periodId 过滤（报表/测试/populateNextYearOpening 幂等清）→ 残留永不被读；次年重建新期间获新 id，幂等清快照 keys by 新 periodId（不清旧残留），旧残留 periodId 永不被读 → 新行正确 + 旧残留孤立死行（数据卫生问题非数据破坏）。**存疑点核心问题"是否致次年年初余额与凭证不一致"答案 = 否**。残留孤立死行累积 + 幂等清快照不覆盖旧残留 + 潜在 periodId-agnostic 查询污染风险 → P2 watch-only。**与 P1-MA2-018 不同机制**（残留孤立 vs 多年累计）；**与 P1-RC-006/P2-RC-007 不同控制点**。 | §2 P2①（次要验收标准边界弱——主路径[次年存在→阻断/无次年→红冲无残留]OK，边界[手动删次年非常规运维 + 残留孤立死行累积]弱；无活跃数据破坏） | successor watch-only（P2 登记不强制） | todo（修复 = ①纯 owner doc 标注[`period-close.md §反结账流程` 补注「手动删次年期间后 yearOpening 残留孤立死行，无即时数据破坏，次年重建新行正确」] 可自动执行；②yearOpening 残留级联清理[`populateNextYearOpening` 幂等清快照增 by subjectId+acctSchemaId 清旧残留，或删期间时级联清 GlBalance]——前者纯文档可自动执行，后者触及 populateNextYearOpening/删期间逻辑[反结账属会计过账] 须评估 ask-first；不触发 §5 ask-first 当仅文档/纯 GlBalance 清理）

### 6.3 双向可追溯

- **新 finding → arm-index**：P2-RC-084 将写入 arm-index §RC 发现追踪分区（Phase 2 同步）。
- **静态存疑点闭合**：A1.6 §7 存疑点 4 经本评估**正向消解为接受（存疑点核心问题答案 = 否）+ 登记 P2-RC-084 watch-only（数据卫生/潜在风险）**，闭合。
- **与 RC-3/RC-4 接受[正常路径]维持 + P1-RC-006（不同控制点）+ P2-RC-007（不同控制点）+ P1-MA2-018（不同机制）+ A2.3 period-close E2E（复用正常路径行为）分层一致**。

---

## 7. 静态存疑点清单（§6 §7）

无。本验证是 MA4 运行时确认，存疑点 A1.6 §7-4 经年末阻断逻辑核验 + 次年期间删除 API census + 边界场景一致性三层推理 + 次年重建错配风险评估 + 测试覆盖边界普查**正向消解为接受（存疑点核心问题答案 = 否）+ 登记 P2-RC-084 watch-only**，无遗留运行时存疑点。

**P0 即时通道**：本验证 Phase 1 定级**未出 P0/P1**（维持接受 + P2 watch-only），按 §10 **不触发 MR0/MR1**（P2 登记不强制）。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`（HEAD=`2cf41032f`），actual vs baseline 汇总如下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0，本验证实测 EXIT=0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => `sys.exit(1)`。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本验证无生产代码变更**（只读评估：读年末阻断 + hasNextYearPeriods + 年度凭证红冲 + yearOpening 存储/填充 + 次年期间删除 API census + 既有测试普查 + 引用 A1.6/A2.3），checker 无回归风险。

  | 规则 | Baseline（`compliance-baseline.md §BASELINE (machine-readable)` 权威块） | Actual（本验证 HEAD `2cf41032f` 实测） | 状态 |
  |------|-----------------------------------------------------|----------------------------|------|
  | R1a (dao().saveEntity BizModel) | 0 | 0 | = ✅ |
  | R1b (dao().updateEntity BizModel) | 0 | 0 | = ✅ |
  | R1c (dao().getEntityById BizModel) | 0 | 0 | = ✅ |
  | R1d (dao().findAllByQuery BizModel) | 14 | 14 | = ✅ |
  | R2a (BizModel daoFor ErpMd*) | 34 | 34 | = ✅ |
  | R2b (BizModel daoFor Erp* 跨域) | 229 | 229 | = ✅ |
  | R2c (全生产 daoFor 总量) | 1382 | （脚本 R2c 段既有行为：未输出计数即返回——A4.1.11/A4.1.13/A4.1.18 已记录同型行为；脚本汇总行 `生产代码总计: 1382 处`） | 不适用（脚本行为，零代码变更无回归风险） |
  | R2d (Processor daoFor ErpMd*) | 34 | 34 | = ✅ |

  > R1/R2（除 R2c 脚本既有行为外）全部 actual == baseline，**0 漂移**。权威基线以 `compliance-baseline.md §BASELINE (machine-readable)` 块为准。R2c 脚本输出截断/未返回是既有工具行为（A4.1 展开器 / A4.1.11 / A4.1.13 / A4.1.18 报告同款记录）；本验证零生产代码变更（docs-only），checker 无回归风险。

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding（1 项新 finding P2-RC-084）已按 §7 规则 grep arm-index 同域同控制点后给出"复用 or 新增"裁决（§6），无未经比对直接新建的 finding。交叉去重声明：与 A1.6 §5.2 RC-3/RC-4 接受[正常路径]（确认维持，不撤销）+ 与 A1.6 §6.1 P1-RC-006/P2-RC-007（不同控制点，不合并）+ 与 P1-MA2-018（不同机制：残留孤立 vs 多年累计，不合并）+ 与 A2.3 period-close E2E（复用正常路径行为，§去重协议）+ MA4↔A5.6 边界（需求契约视角边界一致性评估 vs 测试质量全量评级，不重做 A5.6）。

---

## 9. 真相源冻结声明（§9）

本验证未修改任何冻结真相源（`product-scope.md` / 各域 `use-cases.md` / owner doc `period-close.md` 需求契约段落）。只读评估（读年末阻断 + hasNextYearPeriods + 年度凭证红冲 + yearOpening 存储/填充 + 次年期间删除 API census + 既有测试普查 + 引用 A1.6/A2.3），未修改代码/ORM/api.xml/view.xml/真相源。边界场景一致性裁决记入报告（§2.3），不直改 L1/L2（§9 冻结条款）。

---

## 10. 与 A1.6/A2.3 报告差异增量声明（§去重协议）

本验证复用 A1.6 §2.8（RC-3 年末反结账阻断静态确认）+ §2.9（RC-4 年度凭证红冲静态确认）+ §5.2（RC-3/RC-4 接受[正常路径]）+ A2.3 period-close E2E（年末反结账正常路径行为 + P1-MA2-018 年初余额非累计）+ A2.5b（populateNextYearOpening 机制）+ A4.1.18（period-close 运行时行为评估同型范式），**不重新核实 RC-3/RC-4 正常路径行为本身**。只补 A1.6 §7 存疑点 4 标注为「需运行时探针」的差异：

1. **次年期间删除 API census**（A1.6 §7-4 触发条件"手动删次年期间"未核实）：§2.2 grep census 证实**无专用 API**——触发条件须运维干预（直连 DB/通用 CRUD），非常规 API 路径。
2. **边界场景一致性裁决**（A1.6 §7-4 核心问题"是否致次年年初余额与凭证不一致"未裁决）：§2.3 三层推理裁决**答案 = 否**——红冲仅涉本年 + 残留 yearOpening 孤立无消费方 + 次年重建错配不成立。
3. **次年重建错配风险评估**（A1.6 §7-4 未评估）：§2.3.2 证实次年重建新期间获新 id，幂等清快照 keys by 新 periodId（不清旧残留），旧残留 periodId 永不被读 → 错配不成立（数据卫生问题，非数据破坏）。
4. **RC-3/RC-4 边界场景运行时裁决**（A1.6 §5.2 RC-3/RC-4 接受[正常路径]未补边界路径）：§5.1 按决策树分支①裁决**维持接受 + 登记 P2-RC-084 watch-only**，arm-index 新建 finding（§6.2）。

差异增量与本验证范围一致，无与 A1.6/A2.3 重叠的重新核实。

---

## 11. Verdict

**Verdict: passes requirement-compliance runtime-behavior evaluation**（RC-3/RC-4 边界场景存疑点核心问题答案 = 否[无数据不一致]，维持正常路径接受 + 登记 P2-RC-084 watch-only[数据卫生/潜在风险]，零 P0/P1）

**审查范围**：A1.6 §7-4 存疑点（年末反结账阻断边界 yearOpening 残留一致性）评估——年末阻断逻辑核验（`ReverseCloseProcessor:32-36` + `hasNextYearPeriods:128-134` + `isYearEnd:121-125`）+ **次年期间删除 API census**（grep `deleteNextYear`/`deletePeriod`/`removePeriod` 生产代码零命中）+ 边界场景一致性三层推理（红冲涉本年 + 残留孤立无消费方 + 次年重建错配不成立）+ yearOpening 存储/填充核验（`orm.xml:924-925` + `:940` 无 cascade + `populateNextYearOpening:137-184`）+ 年度凭证红冲核验（`:46-49` + `reverseCloseVoucher:553-562`）+ 测试覆盖边界普查（年末阻断中[assertThrows 浅] + 名实不符测试缺口 + 边界场景零覆盖）+ MA4↔A5.6 边界声明 + §2 判据裁决（决策树分支①）+ 与 arm-index 衔接（新建 P2-RC-084）+ §8 过程纪律自检 + §9 真相源冻结 + §10 差异增量声明。

**维持接受 + P2 类**：RC-3/RC-4 边界场景一致性评估确认存疑点核心问题答案 = 否——手动删次年期间非常规运维（无专用 API 须直连 DB/通用 CRUD）→ 残留 yearOpening 孤立无即时消费方（periodId 指向已删期间，读路径按 valid periodId 过滤取不到残留行）→ 无即时数据破坏；次年重建新期间获新 id，旧残留 periodId 永不被读 → 错配不成立（数据卫生问题非数据破坏）。主路径[次年存在→阻断 / 无次年→红冲无残留]接受维持（A1.6 §5.2 RC-3/RC-4 接受不撤销）。残留孤立死行累积 + 幂等清快照不覆盖旧残留 + 潜在 periodId-agnostic 查询污染风险 → 登记 P2-RC-084 watch-only（数据卫生/潜在风险）。

**P0/P1**：无。不触发 MR0/MR1（P2 登记不强制）。A1.6 §5.2 UC-FIN-07 整体 P1 维持（RC-9 P1-RC-006 + RC-1 复用 P1-MA3-046；RC-3/RC-4 正常路径接受维持 + 边界 P2-RC-084 watch-only）。

**剩余风险**：无遗留运行时存疑点。P2-RC-084 修复归 MR1 successor（纯 yearOpening 残留级联清理或 owner doc 标注可自动执行；触及 hasNextYearPeriods/populateNextYearOpening 逻辑[反结账属会计过账] 须评估 ask-first）；名实不符测试 `testReverseCloseReversesAnnualVoucherWhenNoNextYear` + 年末阻断错误码断言缺口归 A5.6 测试质量维度 successor（纯测试代码 MR1 预授权类目）。两者均非本 MA4 范围。
