# 2026-07-31-0744-2-r2-12-assets-test-effectiveness R2.12 assets 折旧/Processor 链路测试有效性（残差补强）

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR2 R2.12（P1-MA4-014 残差）
> Related: `docs/audits/arm-index.md`（P1-MA4-014/013）、R1.16（业财悬挂统一裁决，已落地 DepreciationPostingDispatcher.dispatchFailureAlert 告警半侧）、R1.28（并发 UK 幂等，已落地并发首次折旧测试）、R2.10/R2.13（同族测试有效性残差范式）
> Audit: required（独立草案审查 + 独立 closure audit）

## Current Baseline

P1-MA4-014（finding 写于 R1.16/R1.28 之前）的子项 (b) 并发双计 + (d) 告警半侧**已由 R1.x 落地的测试闭合**。独立草案审查（fresh-session 实测）确认下列既有测试已覆盖原 finding 列为「零覆盖」的多项，本计划仅补**残差缺口**。逐项实测基线：

**已闭合子项（不在本计划范围，避免重复）**：
- 并发首次折旧重复双计（P1-MA2-089）：`TestErpAstDepreciation.testConcurrentFirstDepreciationNoDuplicate`（:225-284，2 线程 ExecutorService+CountDownLatch 同 assetId+period→断言仅 1 schedule + 累计折旧无双计 + 友好 ErrorCode ERR_AST_DEPRECIATION_ALREADY_EXECUTED/version，R1.28 落地）—— 闭合 (b)
- 折旧过账失败告警半侧（dispatchFailureAlert）：`TestDepreciationPostingFailureAlert`（posting/，71 行，`testFailureDispatchesAlert` :43-58 断言 NOTIFY_EVENT_DEPRECIATION_FAILURE + `testNullNotificationBizSkipsGracefully` :60-70，R1.16 落地）—— 闭合 (d) 告警 dispatch 路径（注：仅直调 dispatchFailureAlert，非端到端 tryPost 失败）

**残差缺口（本计划范围）**：
- **G1（P1-MA4-014(a)）posted=false 窗口 reverseApprove 不对称**：`TestErpAstPostingReverse` 三个 reverse 测试全部从 posted=true 起步——`testCapitalizationReverseApproveRollsBack`（:69-104 submit→approve[posted=true]→reverseApprove[断言 posted=false :89]）/ `testDepreciationReverseRollsBackAssetCard`（:106-133 executeDepreciation[posted=true]→reverseDepreciation）/ `testDisposalReverseApproveRestoresAsset`（:135-177 submit→approve[posted=true]→reverseApprove[posted=false :166]）。**无测试 seed 资本化/处置/折旧 schedule 于 posted=false 悬挂态（过账失败）后调 reverseApprove 验证资产卡片/状态是否回滚**——P1-MA2-060 不对称（悬挂态 reverseApprove 不回滚资产）未演练。grep `posted.*false|suspended` 仅命中 reverse 后断言（:89/:127/:166 等），无一从 posted=false 起步。
- **G2（P1-MA4-014(c)）批量折旧部分失败隔离**：`TestErpAstDepreciation.testBatchDepreciationProcessesAllAssets`（:128-159）seed 2 IN_SERVICE 资产全成功（processed==2，:155 注释"非使用中资产不计提"=状态跳过非异常隔离）。**无测试注入单资产处理时抛异常（如失败 GL post / 坏 category 配置）验证 try/catch 隔离失败**（一失败其余仍处理 + processed 计数仅含成功 + 无整批事务回滚）。
- **G3（P1-MA4-014(d) 残差）折旧 tryPost 悬挂状态半侧**：`DepreciationPostingDispatcher.tryPost`（:53-68 swallow→null→posted=false）无端到端测试驱动。既有 `TestDepreciationPostingFailureAlert` 仅直调 dispatchFailureAlert（告警半侧）。残差 = 确定性诱导 tryPost 失败→断言 `ErpAstDepreciationSchedule` 行 posted=false + voucherId=null 持续（实际悬挂态 P1-MA4-013）+ 后续重跑 executeDepreciation 自愈路径（DepreciationPostingDispatcher:50-52 文档）。**注**：assets 测试无 Mockito（grep 零命中 + pom 无依赖）——确定性失败诱导复用 inventory 范式，但**不可省略会计期间**（折旧 BizModel 前置守卫 `ERR_DEPRECIATION_PERIOD_NOT_FOUND/CLOSED` 先于 tryPost，见 `testPeriodControlRejectsClosedAndMissing:176-183`）；可行路径 = seed OPEN 期间通过 BizModel 守卫 + 省略 dispatcher 解析的科目映射致 posting engine 失败→tryPost catch→posted=false。
- **G4（P1-MA4-014(e)）非零残值折旧算术 + 残值约束分支**：`TestErpAstDepreciation` 全 7 处 seedAsset 调用（6 个测试方法，batch 双资产）传 residualValue=BigDecimal.ZERO（:67/:105/:135/:139/:170/:193/:231，SL :61 / DDB :99 / batch :129 / period-control :162 / idempotent :187 / concurrent :226）；非折旧测试的非零残值（TestErpAstMaintenance:71 residual=5000 等）不演练 DepreciationCalculator 算术。`DepreciationCalculator.java` 两分支未覆盖：残值约束截断 `:71-73`（nbv−amount<residual→amount=nbv−residual）+ 已达残值返 0 `:32-34`（nbv≤residual→return ZERO）；residual=0 时截断分支不触发、返 0 分支与"NBV 归零"重合未以 residual>0 证明 clamp-to-residual 语义。repo 无 `DepreciationCalculator` 直接单元测试（grep 零命中）。残差 = seed 非零残值（如 2000）→断言末期净值=残值非 0 + 截断分支触发 + 已达残值返 0 分支。

剩余差距：G1 + G2 + G3 + G4 四个残差。本计划为**纯测试新增**（无生产 Java/ORM/view.xml 变更），不触及会计保护区域运行时行为——仅补测试使悬挂态 reverseApprove 不对称、批量隔离、折旧悬挂状态、非零残值算术对测试可观测。

## Goals

- G1：posted=false 窗口 reverseApprove 不对称测试——seed 资本化/处置 schedule 于 posted=false 悬挂态（过账失败），调 reverseApprove，断言资产卡片/状态回滚行为（闭合 P1-MA2-060 测试可见性）
- G2：批量折旧部分失败隔离测试——seed 多资产 batch 含一失败资产，断言其余仍计提 + processed 计数仅含成功 + 无整批回滚
- G3：折旧 tryPost 悬挂状态测试——确定性诱导 tryPost 失败→断言 schedule posted=false + voucherId=null 持续 + 重跑自愈（闭合 P1-MA4-013 测试可见性）
- G4：非零残值折旧算术测试——seed 残值≠0（如 2000），断言末期净值=残值非 0 + 截断分支触发（DepreciationCalculator:71-73）+ 已达残值返 0 分支（:32-34）

## Non-Goals

- 不重复实现已闭合子项（并发首次折旧双计 / 折旧告警 dispatch 半侧——见 Current Baseline 既有测试清单）
- 不修改任何生产 Java 代码（DepreciationCalculator/DepreciationPostingDispatcher/ErpAstDepreciationScheduleProcessor/BizModel）
- 不实现 P1-MA4-013 折旧 dispatcher 自动重试/告警闭环升级（属 R1.16 Deferred successor，若须代码修复独立 plan）——G3 仅测悬挂状态可观测
- 不补 finance/mfg/hr/pur+sal+inv 测试有效性（分别归 R2.10 done / R2.11 / R2.13 done / R2.14）
- 不补 R2.15 view.xml drift

## Task Route

- Type: `implementation-only change`（纯测试新增）
- Owner Docs: `docs/design/assets/`（折旧算术 + 残值约束语义 + reverseApprove/红冲闭环 + 业财悬挂 owner-doc 语义）。测试断言的预期行为须与 owner doc 一致
- Skill Selection Basis: 工作方法为 Nop 服务层集成测试（`JunitAutoTestCase` + Facade Java API + seed/output/assert + 无 mock 确定性失败诱导）→ `nop-testing`（基类选择、@NopTestConfig、seed 只追加、拒绝路径快照处理、三层验证模型）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（H2 localDb 集成测试，无端口/外部服务；确定性失败诱导复用 inventory 既有范式——seed 无会计期间/清空科目配置触发 post 失败，无需 Mockito）

## Execution Plan

### Phase 1 - 折旧悬挂态 reverseApprove + 批量隔离 + 悬挂状态 + 非零残值算术（G1+G2+G3+G4）

Status: completed
Targets: `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstDepreciation.java`、`TestErpAstPostingReverse.java`（新增测试方法 + 对应 `_cases/` 快照/seed）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: R2.0 done（已 done）；R1.16 done（告警半侧）；R1.28 done（并发双计）

- [x] Add: G1 posted=false 窗口 reverseApprove 不对称测试 — seed 资本化/处置 schedule 于 posted=false 悬挂态（确定性诱导 post 失败：无会计期间/清空科目映射），调 reverseApprove，断言资产卡片/状态回滚行为（回滚 or 不回滚——按实际运行时行为断言并锁定为回归基线，闭合 P1-MA2-060 测试可见性）
  - Skill: `nop-testing`
- [x] Add: G2 批量折旧部分失败隔离测试 — seed 多资产 batch 含一确定性失败资产（如失败 GL post / 坏 category 配置），调 executeBatchDepreciation，断言其余资产仍计提 + processed 计数仅含成功 + 无整批事务回滚
  - Skill: `nop-testing`
- [x] Add: G3 折旧 tryPost 悬挂状态测试 — 确定性诱导 DepreciationPostingDispatcher.tryPost 失败（seed 无会计期间/清空科目映射，复用 `TestErpInvPosting:107-115` 无 mock 范式），断言 ErpAstDepreciationSchedule 行 posted=false + voucherId=null 持续 + 后续重跑 executeDepreciation 自愈路径（闭合 P1-MA4-013 测试可见性）
  - Skill: `nop-testing`
- [x] Add: G4 非零残值折旧算术测试 — seed 残值≠0（如原值 12000/残值 2000），直线法多期折旧，断言末期净值=残值（2000）非 0 + 截断分支触发（DepreciationCalculator:71-73 nbv−amount<residual 截断）+ 已达残值返 0 分支（:32-34 nbv≤residual→ZERO）
  - Skill: `nop-testing`
- [x] Proof: Phase 1 新增测试方法首次 RECORDING 后切 CHECKING 全绿
  - `mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstDepreciation,TestErpAstPostingReverse`
  - Skill: none

Exit Criteria:

> assets 悬挂态 reverseApprove + 批量隔离 + 悬挂状态 + 非零残值算术补齐，使 4 类缺陷对测试可观测。

- [x] G1（posted=false 窗口 reverseApprove 行为锁定）+ G2（批量失败隔离）+ G3（schedule posted=false+voucherId=null+自愈）+ G4（末期净值=残值非 0 + 截断/返 0 分支）测试在 CHECKING 模式绿
- [x] 若 G1/G3 测试发现与 owner doc 不符的真实行为缺陷，升级为独立 Fix 计划并记录（不静默改生产代码）

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04a92fae3ffe4RFKZbfrSkfSTz) — fresh-session 实测复核：(b) 并发 testConcurrentFirstDepreciationNoDuplicate:225-284 + (d)-告警半侧 testFailureDispatchesAlert:43-58（仅直调 dispatchFailureAlert 非 tryPost）闭合声明精确；G1-G4 残差全部真实（TestErpAstPostingReverse 3 reverse 全从 posted=true 起步 / batch:128-159 全成功 :155 状态跳过 / tryPost 无端到端失败测试 / seedAsset 全 ZERO + DepreciationCalculator:71-73/33-34 两分支 + 无直接单元测试）；P1-MA4-014 (a)-(e) 全子项无遗漏；规则合规 PASS。G3 无 mock 机制澄清已采纳（不可省略期间——BizModel ERR_DEPRECIATION_PERIOD_NOT_FOUND 前置守卫；应 seed OPEN 期间 + 省略科目映射）。无阻塞项。草案审查收敛，转 active。

## Closure Gates

> 纯测试新增，无生产代码/ORM/view.xml 变更。完整仓库验证在此处一次。

- [x] 范围内行为完成（G1 + G2 + G3 + G4 残差测试方法落地并 CHECKING 绿）
- [x] 相关文档对齐（若 G1-G4 测试发现与 owner doc 不符的真实缺陷，已升级为独立 Fix 计划并记录；否则无 owner-doc 更新）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿 + `mvn test -pl module-assets/erp-ast-service` 全绿（含新测试）+ `bash docs/audits/nop-compliance-checker.sh` 零新增命中
- [x] 无范围内项目降级为 deferred/follow-up（发现的真实行为缺陷按不可降级规则升级 Fix，不降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA4-013 折旧 dispatcher 自动重试/告警闭环升级（R1.16 Deferred successor）

- Classification: `out-of-scope improvement`（沿用 R1.16 裁决边界）
- Why Not Blocking Closure: 本计划为测试有效性（R2.12）；013 自动重试/sweep 升级属代码修复非测试；G3 仅测悬挂状态可观测（posted=false+voucherId=null 对测试可见即达测试有效性目标）
- Successor Required: `yes`（沿用 R1.16 命名 successor：折旧 dispatcher 接线 finance 异常记录/重试 plan；触发 = 人工批准保护区域 owner doc 后）

## Closure

Status Note: 全 done。G1-G4 残差测试落地并 CHECKING 绿，零生产代码变更，行为与 owner-doc `depreciation-and-posting.md §7.2` 一致（无需升级 Fix）。独立结束审计 PASS（10 项全绿，无阻塞项）。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent（新会话 ses_04a53875dffepLyNWc8KvTTcZp）
- Evidence: 10 项核验全 PASS — 零生产代码 diff（仅 src/test/ + docs）、G1 不对称断言（TestErpAstPostingReverse:117-168 资产保持 IN_SERVICE/计划保持 PENDING）、G2 批量隔离 processed==1（TestErpAstDepreciation:233-272）、G3 悬挂+自愈（:283-323）、G4 非零残值+截断/返 0 分支（:334-368 + TestDepreciationCalculator）、Tests run: 104 Failures: 0 Errors: 0、BUILD SUCCESS、零新增合规命中、roadmap R2.12 done、owner-doc §7.2 行为一致。

Follow-up:

- P1-MA4-013 折旧 dispatcher 自动重试/告警闭环升级（沿用 R1.16 Deferred successor，属代码修复非测试，G3 仅测悬挂可观测）——触发条件：人工批准保护区域 owner doc 后，独立 plan。
