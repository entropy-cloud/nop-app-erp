# 2026-08-03-0900-2 rc-ma1-a1-22-assets-f1-depreciation-engine assets-F1 折旧引擎需求符合性审计

> Plan Status: active
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.22（MA1 需求追踪矩阵审计 — assets-F1 折旧引擎）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.22
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.22 的 0.2 依赖）、`2026-08-03-0900-3-rc-ma1-a1-23-assets-f2-disposal.md`（assets 域后继切片 A1.23 处置；UC-AST-05 "先补提当期折旧至出售日"前置依赖本切片 UC-AST-07 补提能力，故 F1 先于 F2）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.22 给出 UC 清单 = `UC-AST-02/07/08`（3 UC），含 `use-cases.md:31/:116/:131` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/assets/use-cases.md`（机制见 `depreciation-and-posting.md` + `state-machine.md`）：
  - UC-AST-02 期末直线法折旧（`:31`）：期末折旧任务 → 查所有 IN_SERVICE 资产 → 计算月折旧额 = (原值 − 残值) / 使用月数（直线法）；残值约束：计提后净值不低于残值（低于则截断为 0）→ 生成凭证（借 折旧费用 / 贷 累计折旧）→ 折旧计划条目 PENDING → EXECUTED；卡片.累计折旧 += 月折旧额，卡片.净值 −= 月折旧额。
  - UC-AST-07 折旧漏提补提（`:116`）：方式A（反结账）反结账漏提期间 → 补提 → 重新结账（严格，影响已结账数据）；方式B（当期补提）当期一次性补提前期漏提额（简化，不追溯）；补提凭证标注所属期间（审计）。
  - UC-AST-08 期末批量折旧容错（`:131`）：批量折旧 → 并行处理各资产；单资产失败（如科目缺失）→ 隔离，不影响其他资产；汇总成功凭证，失败资产标记待处理；失败资产可单独重试。

- **L3 代码实现现状（实测）**——UC-AST-02/08 已实现且测试强；UC-AST-07 补提能力**实质缺失**：
  - **折旧引擎入口（UC-AST-02/08，R6.3 per-mutation 拆分）**：`ErpAstDepreciationScheduleBizModel.java:26-75`（Facade，extends CrudBizModel，`@BizModel("ErpAstDepreciationSchedule")`，暴露 `executeDepreciation`/`executeBatchDepreciation`/`reverseDepreciation`/`recalculateForCapitalizationMaintenance` `:46-74`）；单资产 `ErpAstDepreciationScheduleExecuteDepreciationProcessor.executeDepreciation:38-121`；批量 `ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor.executeBatchDepreciation:37-55`；冲销 `ErpAstDepreciationScheduleReverseDepreciationProcessor.reverseDepreciation:33-55`。**无 ErpAstDepreciationBizModel 类**（R6.3 拆分取代历史单体 Processor）。
  - **直线法公式（UC-AST-02，已实现）**：`DepreciationCalculator.java:18-80` 纯函数计算器；直线法分支 `(original − residual) / months` SCALE=4 HALF_UP `:63-66`；方法/月数解析 `ErpAstDepreciationScheduleExecuteDepreciationProcessor:43-48`；Calculator 调用 `:71-73`。**L1↔code 词汇核对（无漂移）**：L1 `use-cases.md:39,40` 用中文"残值"，代码 `ErpAstAsset.residualValue`（`app-erp-assets.orm.xml:186`，propId=10，displayName="残值"）——同概念同中文术语，**无词汇漂移**（Phase 1 逐字引用 L1 时将确认"残值"一致，不产生 drift finding）。
  - **残值约束（UC-AST-02，已实现）**：`DepreciationCalculator.java:32-35`（nbv ≤ residual 提前返回 ZERO）；`:70-73`（post-compute 截断 if nbv−amount < residual then amount=nbv−residual）；`:74`（负结果 clamp ZERO）。
  - **折旧凭证（UC-AST-02，已实现）**：`DepreciationPostingDispatcher.tryPost:53-68` → buildEvent `:110-135`（businessType=`ErpFinBusinessType.DEPRECIATION`=70 `:113`）→ `AssetPostingExecutor`（REQUIRES_NEW → `IErpFinVoucherBiz`）；`DepreciationAcctDocProvider.java:26-87`（supports DEPRECIATION）→ **借 6602 折旧费用 / 贷 1602 累计折旧** `:48-54`（subject fallback 6602/1602 `:31-32`）；幂等键 `billHeadCode=assetCode#period` `:137-139`；reverse 红冲 `:97-108`。
  - **计划条目迁移 + 卡片回写（UC-AST-02，已实现）**：`ErpAstDepreciationScheduleExecuteDepreciationProcessor:91-94`（PENDING→EXECUTED + executedAt）；UK_AST_DEPRECIATION_ASSET_PERIOD 守卫 `:96-109`（catch → ERR_AST_DEPRECIATION_ALREADY_EXECUTED）；re-execute restore `:59-65`（减旧 actualAmount、加回 nbv）；newAccum/newNbv 计算 `:75-76`；回写 `setAccumulatedDepreciation`+`setNetBookValue`+saveOrUpdate `:98-100`；posted=true+voucherId `:111-119`。
  - **批量容错（UC-AST-08，部分实现）**：`ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor:37-55`（查 IN_SERVICE 资产 `:40-43` → **per-asset try/catch 隔离** `:46-53` 失败 LOG.warn 跳过 → 返回 processed 计数）。**缺口 #1**："失败资产可单独重试"——**无独立重试 API/队列**，仅手动重调 `executeDepreciation`（幂等）；`tryPost` 失败派发 `IErpSysNotificationBiz` 告警（`DepreciationPostingDispatcher:74-92`，event key `ast.depreciation-posting-failure`）。**缺口 #2**："汇总成功凭证"（单凭证多行聚合）——owner doc `depreciation-and-posting.md §十:348` 显式 Deferred（基线按资产串行 + 每资产单张凭证）。
  - **折旧漏提补提（UC-AST-07，缺口 #3 — 最高风险）**：跨模块 grep `补提|catchUp|backfill|reversePeriod` 生产代码 → 仅 `ErpAstErrors.java:96` 错误消息串"折旧期间 {period} 已结账，不允许补提折旧"（**仅拒绝消息，无逻辑**），catchUp/backfill/reversePeriod **零生产匹配**。**方式A（反结账补提）**：`IErpFinAccountingPeriodBiz.reverseClose` 在 finance 域（`ErpFinAccountingPeriodBizModel.java:70-71`），assets 域**无 reverseClose 调用**（grep 0 匹配）——两步链（finance.reverseClose + ast.executeDepreciation）技术上可达但**未编排**。**方式B（当期一次性补提）**：**完全缺失**——`executeDepreciation` 每次 Calculator 计算单月折旧（`elapsed` 来自 EXECUTED 行计数 `ErpAstDepreciationScheduleProcessor.countExecuted:126-131`），2026-05 漏提、2026-07 调用仅产生单月额而非两月补提额；无 `catchUp`/`backfill` mutation。MA2 assets 状态机审计 `2026-07-28-0400-arm-ma2-assets-state-machine.md:163,236-241` 以"两条路径技术上可达"判 **PASS**，但**未按 Q4(a) 会计正确性重审**——补提凭证归属期间标注（L1 `:124`）：`buildEvent` voucherDate=schedule.businessDate `:119-121` + billHeadCode=assetCode#period，期间标签携带但凭证日期为 schedule businessDate。
  - **跨域 Facade**：`IErpFinVoucherBiz`（post/reverse，经 `AssetPostingExecutor`）；`IErpFinAccountingPeriodBiz`（reverseClose，仅 finance 侧，assets 未编排）；`IErpSysNotificationBiz`（失败告警）。

- **L4 测试证据现状**（`module-assets/erp-ast-service/src/test/`）：
  - `TestErpAstDepreciation.java`（9 @Test 全**强**）：testStraightLinePerPeriodEqualAndLastToResidual（12 期 each=1000 + posted + Σ=12000 + accum/nbv 收敛残值）、testDoubleDecliningResidualConstraint、testBatchDepreciationProcessesAllAssets（2 资产 processed EXECUTED posted exact 1000/500）、testPeriodControlRejectsClosedAndMissing（assertThrows ERR_DEPRECIATION_PERIOD_CLOSED/NOT_FOUND）、testIdempotentReExecuteReversesAndRegenerates（3 billR 行 orig+红冲+regen 恰 1 active）、testBatchDepreciationIsolatesFailingAsset（孤儿 EXECUTED+posted seed，批处理仅干净资产 processed=1）、testDepreciationTryPostFailureLeavesSuspendedThenSelfHeals（缺 GL 科目→posted=false 持久→补 seed→重跑→posted=true 自愈）、testNonZeroResidualStraightLineClampsToResidual（残值=2000 clamp）、testConcurrentFirstDepreciationNoDuplicate（2 线程 ExecutorService+CountDownLatch 恰 1 schedule 行）。
  - `TestDepreciationCalculator.java`（3 @Test 纯函数，残值 clamp 收敛）。
  - `PropertyErpAstDepreciationResidual.java`（jqwik 属性测试，3 @Property 100 tries + 2 @Test，直线/余额递减不破残值不变式）。
  - `TestDepreciationPostingFailureAlert.java`（2 @Test 告警派发 + null graceful）。
  - `TestErpAstAcctDocProviderAccountKey.java`（ACCOUNT_KEY_DEPRECIATION_EXPENSE/ACCUMULATED_DEPRECIATION）。
  - **缺口测试**：UC-AST-07 补提语义**零测试**（仅 `testPeriodControlRejectsClosedAndMissing` 验拒绝消息）；UC-AST-08 批量重试 API（不存在故无测试）。
  - **无 E2E**：`tests/e2e/` grep `depreciation` 仅处置/资本化流，无折旧独立 E2E。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`（A2.10）：场景(a) 设备折旧 happy path **PASS** `:210-215`；场景(e) 折旧漏提补提 **PASS**"两条路径技术上可达" `:163,236-241`；异常路径"已结账拒绝"`:157`/"净值低于残值"`:158`/"漏提"`:163`/"幂等"`:165`/"并发"`:164`(→P1-MA2-089 resolved R1.28)。findings：P1-MA2-058/059（Movement，非折旧）、P1-MA2-060（Cap/Disposal tryPost 吞异常悬挂，resolved R1.16，**与折旧 dispatcher 同模式**）、P1-MA2-061（IDLE 状态机零实现，resolved R1.18，折旧仅查 IN_SERVICE 间接相关）。
  - `docs/audits/2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md`：P1-MA4-013（折旧 dispatcher posted=false 业财悬挂无重试，**resolved R1.16**）、P1-MA4-014（折旧/Processor 链测试有效性含非零残值+并发+隔离，**resolved R2.12**）、P1-MA4-015（跨域 daoFor(ErpFin*)，resolved 永久只读豁免）、P2-MA4-006（9 dispatcher copy-paste + tryPost 返回类型 drift，watch-only）。
  - **本切片须声明与上述 MA2/MA4 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（UC-AST-07 补提能力实质缺失 MA2 仅"技术可达"未按 Q4(a) 会计正确性重审 / 批量重试 API 缺失 / 汇总凭证 owner-doc Deferred 复核）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-060`（Cap/Disposal tryPost 吞异常，resolved R1.16，`arm-index.md:299`）、`P1-MA2-089`（executeDepreciation 缺 PENDING 守卫致并发双计，resolved R1.28 UK 升级，`:329`）、`P1-MA4-013`（折旧 dispatcher 悬挂，resolved R1.16，`:618`）、`P1-MA4-014`（折旧测试有效性，resolved R2.12，`:619`）、`P1-MA5-011`（折旧引擎异常路径测试缺口，归并，`:641`）、`P2-MA4-006`（dispatcher copy-paste，watch-only）。**RC 系列对 assets 为零**——A1.22 为 assets 域首个 RC 切片。本切片须 grep arm-index assets 折旧同域同控制点后裁决：UC-AST-07 补提能力缺失 vs P1-MA4-013（悬挂，不同根因——013 是悬挂，本切片是能力完全缺失）、vs P1-MA2-089（并发双计，不同控制点）——裁决**新建 P*-RC-xxx** 并列明差异依据。

- **ORM 关键字段**（`module-assets/model/app-erp-assets.orm.xml`）：`ErpAstAsset`（`:170-255`）originalValue `:184` / residualValue `:186`（=L1 salvageValue）/ depreciationMethod `:187`（dict erp-ast/depreciation-method STRAIGHT_LINE/DECLINING/UNITS `:79`）/ usefulLifeMonths `:189` / status `:195`（dict erp-ast/asset-status）/ accumulatedDepreciation `:204` / netBookValue `:205`；`ErpAstDepreciationSchedule`（`:324-390`）assetId `:332` / period `:334` / plannedAmount `:335` / actualAmount `:336` / status `:339`（dict depreciation-schedule-status PENDING/EXECUTED/REVERSED/CANCELLED `:84`）/ posted `:341` / voucherId `:344` / businessDate `:345`；UK_AST_DEPRECIATION_ASSET_PERIOD(assetId,period,delVersion) `:369`。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；触及会计过账逻辑（折旧凭证/补提重算）或 ORM 结构（补 catchUp mutation）的修复行须 ask-first（§5 保护区域暂停协议）。UC-AST-07 补提涉会计正确性——若定级 P1 须人工确认是否 owner doc 范围裁剪（`depreciation-and-posting.md` 是否将方式A/B Deferred 登记为显式简化须经 §4 三判据复核）。

- **剩余差距**：A1.22 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源，且 UC-AST-07 补提能力分歧是 A1.23 UC-AST-05"先补提当期折旧至出售日"的前置依赖证据。本计划产出 A1.22 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.22 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-22-assets-f1-depreciation-engine.md`，含方法论 §6 **9 段全部内容**。
- 对 3 UC（UC-AST-02/07/08）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：#1 UC-AST-07 方式B 当期一次性补提完全缺失（倾向 P1——会计正确性：补提额计算与凭证归属期间）、#2 UC-AST-07 方式A 反结账补提未编排（须人工确认是否 owner doc Deferred 范围裁剪）、#3 UC-AST-08 批量失败资产无独立重试 API（倾向 P2 watch-only——手动重调幂等可达）、#4 UC-AST-08 汇总成功凭证 Deferred（复核 owner doc §十 Deferred 是否满足 §4 三判据）、salvageValue↔residualValue 词汇漂移（P2 doc drift）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。对 owner doc Deferred（#4）按 §4 复核人工批准证据，若不符合三判据则重新打开并入 MR1。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；既有行追加 RC 注记）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/depreciation-and-posting.md/state-machine.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.23 处置各自独立 plan；A1.22 只覆盖 UC-AST-02/07/08）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：折旧 happy path/残值约束/幂等/并发/批量隔离行为由 A2.10 + A4 证实，只补需求视角差异）。
- **不自决 UC-AST-07 范围裁剪**——方式A/B 是否为 product-scope 范围裁剪须人工确认（裁剪→§4 出口 iii 改真相源非降级；未裁剪→会计正确性 P1 强制实现 Q4）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.22 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.22 UC 锚点）+ `docs/design/assets/use-cases.md`（L1 真相源）+ `docs/design/assets/depreciation-and-posting.md`（L2 设计参考 §1/§五/§十，非真相源）+ `docs/design/assets/state-machine.md`（L2 §1/§2/§4）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2/MA4 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstDepreciation,TestDepreciationCalculator,TestDepreciationPostingFailureAlert`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-22-assets-f1-depreciation-engine.md`（落盘 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [ ] `Proof` 对 UC-AST-02/07/08 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:31/:116/:131` 验收标准原文；L2 引用 `depreciation-and-posting.md`（§1.2/§1.3 直线法、§五 批量容错、§十 实现约定 `:348` Deferred）+ `state-machine.md`（§1 状态定义、§4 漏提补提，标注"设计参考，冲突以 L1 为准"，注意 salvageValue↔residualValue 词汇漂移、方式A/B 是否 Deferred）；L3 引用 `module-assets/.../DepreciationCalculator.java:<line>` / `ErpAstDepreciationScheduleExecuteDepreciationProcessor` / `ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor` / `DepreciationPostingDispatcher` / `DepreciationAcctDocProvider` / `ErpAstErrors`（含跨域 `IErpFinVoucherBiz`/`IErpFinAccountingPeriodBiz`/`IErpSysNotificationBiz`）；L4 引用 `TestErpAstDepreciation.java#method` / `TestDepreciationCalculator` / `PropertyErpAstDepreciationResidual` / `TestDepreciationPostingFailureAlert`（注明断言强度）；L5 复用 MA2 A2.10 + MA4 + 单测。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-AST-02 直线法公式（已实现 `DepreciationCalculator:63-66`）；②UC-AST-02 残值约束（已实现 `:32-35,70-74`）；③UC-AST-02 凭证借贷（已实现 `DepreciationAcctDocProvider:48-54` 借 6602/贷 1602）；④UC-AST-02 计划迁移+卡片回写（已实现 `ExecuteDepreciationProcessor:91-100`）；⑤#1 UC-AST-07 方式B 当期一次性补提**完全缺失**（grep catchUp/backfill 0 生产匹配，executeDepreciation 每次单月）；⑥#2 UC-AST-07 方式A 反结账补提**未编排**（assets 无 reverseClose 调用）；⑦UC-AST-07 补提凭证归属期间标注（buildEvent voucherDate=businessDate `:119-121`）；⑧UC-AST-08 批量隔离（已实现 `ExecuteBatchDepreciationProcessor:46-53` per-asset try/catch）；⑨#3 UC-AST-08 失败资产无独立重试 API（仅手动重调幂等 + 告警派发 `:74-92`）；⑩#4 UC-AST-08 汇总成功凭证 Deferred（owner doc §十:348 显式 Deferred，复核 §4 三判据）；⑪salvageValue↔residualValue 词汇核对（L1 残值 `use-cases.md:39,40` = code residualValue displayName 残值 `orm.xml:186`，**同概念同术语无 drift**，Phase 1 逐字引用确认）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：UC-AST-02 直线法+残值+凭证→**接受**（行为正确，测试强含属性测试）；UC-AST-07 方式B 补提缺失（倾向 **P1**——会计正确性：漏提额不补致累计折旧低估/净值高估，§2 P1①功能实质偏离 + §5 会计正确性无例外）须人工确认范围裁剪；UC-AST-07 方式A 未编排（须人工确认 owner doc 是否 Deferred 范围裁剪，复核 §4 三判据）；UC-AST-08 批量隔离→**接受**（行为正确）；UC-AST-08 失败重试 API→倾向 **P2**（手动幂等可达，watch-only）；UC-AST-08 汇总凭证→复核 §4 三判据（owner doc §十 Deferred 显式标注，复核人工批准证据）；L1↔code 残值术语一致无 drift（不产 finding）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 报告 §1-§5 已落盘：UC-AST-02/07/08 各一矩阵行，L1 逐字引用、L3 含行号、L4 注明断言强度、L5 标注复用 A2.10/A4 来源
- [ ] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#4 有明确分级（非悬空"待查"）；#4 documented simplification 复核结论已记录（满足/不满足 §4 三判据）；UC-AST-07 须人工确认范围裁剪已标注不自决；L1↔code 残值术语一致性已确认（不产 drift finding）

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: planned
Targets: `docs/audits/<执行时间戳>-rc-ma1-a1-22-assets-f1-depreciation-engine.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [ ] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` assets 折旧同域同控制点（P1-MA4-013 悬挂 / P1-MA2-089 并发双计 / P1-MA5-011 测试缺口 / P2-MA4-006 copy-paste）后裁决——同根因同控制点 → 复用（追加 RC 注记）；新根因 → 新建 `P1-RC-xxx` 列明差异依据。#1 方式B 补提缺失 vs 013（013 是悬挂，本切片是能力完全缺失，不同根因）→ 新建。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）；记录 #4 documented simplification 复核结论（满足三判据则维持 P2 watch-only；不满足则 §4 重新打开为 P1 入 MR1）。
      - Skill: none
- [ ] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如方式A 跨域 finance.reverseClose + ast.executeDepreciation 实际编排可达性及凭证期间效果 / 补提在多漏提期下的实际累计折旧偏差 / 批量隔离在 GL 科目部分缺失场景的实际跳过行为等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [ ] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [ ] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-assets-state-machine.md`（折旧 happy path/残值/幂等/并发/批量隔离 PASS + 漏提"技术可达"）+ `2026-07-29-0024-...-code-quality.md`（P1-MA4-013/014 resolved），列明只补的需求视角差异（UC-AST-07 补提能力实质缺失按 Q4(a) 重审 / 批量重试 API 缺失 / 汇总凭证 Deferred 复核 / 词汇漂移）。
      - Skill: none
- [ ] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区（assets 首个 RC 切片）；既有行（P1-MA4-013/P1-MA2-089）追加 RC 复核注记。
      - Skill: none
- [ ] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [ ] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据；#4 复核结论已记录
- [ ] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [ ] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03ba8d28bffe1rUv0iWya2qsg2，fresh session，未起草本计划）。规则 1-13 全 PASS：(1) Deps A1.22=0.2 done；(2) 单结果表面（A1.22 报告 UC-AST-02/07/08，无 A1.23 处置范围泄漏）；(3) 格式 + 命名合规（N=2 = assets-F1，在 N=3 assets-F2 之前；理由：UC-AST-07 补提能力是 UC-AST-05 先补提的上游）；(4) UC 覆盖精确（baseline-inventory = UC-AST-02/07/08）；(5) Baseline 11/11 spot-check 全 CONFIRMED——DepreciationCalculator 直线法 `(original−residual)/months`:65 + 残值 clamp :33-35/:71-73/:74 / 批量 per-asset try/catch 隔离 :46-53 / catchUp/backfill/补提/reversePeriod 生产代码 0 匹配（仅 ErpAstErrors.java:96 消息串）/ reverseClose 在 module-assets 0 匹配（仅 finance 域）/ executeDepreciation 每次单月（elapsed=countExecuted，无多月循环）/ voucherDate=businessDate :119-121 / DepreciationAcctDocProvider 借 6602 贷 1602 / owner doc §十:348 汇总凭证 Deferred / arm-index P1-MA4-013(:618)/014(:619)/P1-MA2-089(:329)/P1-MA2-060(:299)/P1-MA5-011(:641) 均存在且状态正确；(6) 方法论 §1-§10 + §去重 + §4 三判据 + §7 对齐；(7) 反松弛；(8) Q4(a) 正确——UC-AST-07 补提涉会计正确性，P1 + 人工确认范围裁剪不自决，不禁方案 B；§十 Deferred 复核 wired 进 Phase 1 Decision + Phase 2 + 两段 exit；(9) Closure Gates audit-only 有据。无阻塞。Non-blocking（**已应用修订**）：reviewer 指出"salvageValue 词汇漂移"前提不准——L1 实际用中文"残值"（use-cases.md:39,40），code residualValue displayName="残值"，同概念同术语**无 drift**；已将 Baseline/Phase 1 ⑪/Decision/Exit Criteria 从"词汇漂移 P2 doc drift"修订为"残值术语一致无 drift（不产 finding）"，避免审计产出伪 drift finding（Phase 1 逐字引用纪律会自校正确认）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A1.22 报告 9 段齐全 + 3 UC 逐矩阵行 + finding 登记入 arm-index
- [ ] 相关文档对齐：报告与方法论 §1-§10 + §去重协议 + §4 三判据一致；与 rc-requirement-baseline-inventory A1.22 锚点一致
- [ ] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录 + finding 复用/新增裁决可追溯 + documented simplification 复核结论可追溯（无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；触及会计过账逻辑（折旧凭证/补提重算）或 ORM 结构（补 catchUp mutation）的修复行须 ask-first + 独立 plan-audit（§5）。UC-AST-07 方式A/B 缺失须人工确认是否为 owner doc/product-scope 范围裁剪（裁剪→§4 出口 iii 改真相源非降级；未裁剪→会计正确性 P1 强制实现 Q4）。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-AST-07 待人工确认 product-scope 范围；本切片 UC-AST-07 结论为 A1.23 UC-AST-05"先补提当期折旧至出售日"前置证据）

## Closure

Status Note: <待完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立结束审计填写>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围（见 Deferred But Adjudicated）
- UC-AST-07 方式A/B 缺失须人工确认 product-scope 是否范围裁剪
