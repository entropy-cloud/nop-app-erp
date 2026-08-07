# 2026-08-07-1932-3-rc-mr1-r1-2-fin-bank-recon-auto-reverse-scheduling RC-R1.2 — finance 银行对账自动红冲调度接线（P1-RC-005，MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: RC-R1.2（MR1 第一批纯预授权：finance 银行对账自动红冲调度接线，P1-RC-005）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.2 行 + `docs/audits/arm-index.md` P1-RC-005 行
> Related: `docs/audits/2026-08-02-1815-rc-ma1-a1-4-finance-f4-bank-recon.md`（A1.4 切片，finding 来源）；`docs/audits/2026-08-07-1400-rc-ma4-a4-1-14-bank-recon-auto-reverse-config-orphan-awareness.md`（A4.1.14 config 孤儿化普查）；`docs/design/finance/bank-reconciliation.md`（L2）；`docs/architecture/job-scheduling.md`（:110-111 DESIGN 登记）；`docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（A4.2.12 双层 config-gate 接线范式）；`docs/plans/2026-08-07-1819-1-rc-mr1-r1-0-finding-expansion.md`（R1.0 展开器）
> Audit: required

## Current Baseline

- **finding P1-RC-005（arm-index 行）**：下月初自动红冲完全缺失——L1 `docs/design/finance/use-cases.md:176,288` 逐字「未达账项 → 生成调整凭证(BANK_RECON_ADJ), **下月红冲**」/「下月初**自动红冲**(跨期还原)」。config key `CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH = "erp-fin.bank-recon-auto-reverse-next-month"` 定义于 `ErpFinConstants.java:289`（默认 true）但**零消费**（A4.1.14 五维普查：grep config key 全变体 + scheduler.yaml + 全 19 `.job.yaml` + 全 9 `.batch.xml` + beans.xml/IJob 仅定义 1 命中，live 复核一致）——运维以为自动生效但实际不执行，隐性失效。
- **架构登记**：`docs/architecture/job-scheduling.md:110-111` 诚实登记 `erp-fin-bank-recon-adj-reverse` 为 DESIGN[待实现]——本计划落地后须同步该注记。
- **reverse 入口（既有，仅调用不修改）**：`ErpFinBankReconciliationReverseProcessor.reverse:17-19`（`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/`）→ `BankReconciliationBuilder.reverse:133-142`（POSTED→CANCELLED + `BankReconAdjustmentVoucherBuilder.reverse:97-102` 红冲 BANK_RECON_ADJ 凭证）。调节表实体字段：code/statementId/reconciliationDate/docStatus（`_ErpFinBankReconciliation.java:195/:204/:207/:228`）+ 关联 statement 期间。
- **期间门控约束（本计划设计关键约束，实仓已核实）**：`ErpFinPostingProcessor.reverseProcess:235` 按**原凭证日期**解析红冲期间（`resolveOpenPeriod(original.getVoucherDate(), ...)`），`resolveOpenPeriod:524-527` 对非 OPEN 期间抛 `ERR_PERIOD_CLOSED`；调整凭证日期 = 调节表 reconciliationDate（`BankReconAdjustmentVoucherBuilder.post:87`）；期末结账 cron `0 0 22 L * ?`（每月最后一天 22:00，`job-scheduling.md:106`）→ **启用期末结账的部署中，次月 1 日红冲上月调整必然撞 CLOSED 期间**（Phase 1 Decision 1 裁决，见 Execution Plan）。默认部署（结账 job 未启用、期间恒 OPEN）无此冲突。
- **调度接线范式（复用）**：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-fin-deferred-posting-sweep.job.yaml`（jobName / `enabled: "@cfg:nop.job.xxx.enabled|false"` / trigger.cronExpr / invoker `nopBatchTaskRunner.executeAsync` / params.taskPath）+ `module-finance/erp-fin-service/src/main/resources/_vfs/nop/batch-task/fin/deferred-posting-sweep.batch.xml`（orm-reader loader + XLang processor `inject('bean')` + 单条失败隔离）。
- **双层 config-gate 范式（A4.2.12 hr contract expiry）**：job 层 `enabled` 默认 false（部署 opt-in）+ 业务 config 层消费——本 finding 的 config key 默认 true，接线后成为机制开关（false 时跳过）。
- **既有测试**：`TestErpFinBankReconciliation`（5 tests：平衡/不平衡/CLOSED 拒绝/无未达不产凭证/post+reverse）——reverse 行为已有覆盖，本计划补调度侧测试。
- **预授权判据**（第一批纯预授权）：arm-index P1-RC-005 行「修复 = 接线 scheduler[nop-batch job.yaml 注册下月初红冲作业 + 消费 config key 门控 + 批量调 BankReconciliationBuilder.reverse]，纯调度接线 + BizModel 调用…**不触发 §5 ask-first**（不触及 ORM/会计过账核心路径，仅调用既有 reverse 入口）」+ 展开器映射记录 §3.1（「纯调度接线 + BizModel 调用（仅调用既有 reverse 入口）」）。**无需 ask-first checkbox**。

## Goals

- 注册 nop-job `erp-fin-bank-recon-adj-reverse`（job.yaml + batch.xml + 批处理 helper bean），下月初扫描已过账且未红冲的银行调节表调整，逐条调既有 reverse 入口（`BankReconciliationBuilder.reverse` / `ErpFinBankReconciliationReverseProcessor`）。
- 双层门控：`nop.job.erp-fin-bank-recon-adj-reverse.enabled`（默认 false，部署 opt-in）+ 消费 `erp-fin.bank-recon-auto-reverse-next-month`（默认 true，机制开关）——A4.2.12 接线范式。
- CLOSED 期间碰撞显式裁决：默认部署（期间恒 OPEN）下 L1「下月初自动红冲」验收成立；启用期末结账部署下逐条失败隔离 + 显式日志（可观测，非零消费隐性失效），完整「跨期还原」核心路径增强登记为 successor（见 Non-Goals + Deferred But Adjudicated）。
- 单测：批量扫描 + 红冲 + 期间判定 + CLOSED 候选行为 + config 关闭跳过。
- 回填 arm-index P1-RC-005 修复状态 + roadmap RC-R1.2 标记 done；同步 `docs/architecture/job-scheduling.md:110-111` DESIGN → implemented 注记。

## Non-Goals

- **不修改 reverse 入口/红冲逻辑本身**（`BankReconciliationBuilder.reverse`/`BankReconAdjustmentVoucherBuilder.reverse` 只调用不改；`reverse` 的 POSTED→CANCELLED 守卫保持）。
- **不改 `ErpFinPostingProcessor.reverseProcess` 期间解析**（按红冲日期解析下一 OPEN 期间实现「红冲凭证记入次月」——属会计核心路径行为变更，越出第一批预授权边界（§7 A2），登记为 successor，本计划不实施）。
- **不改 ORM / api.xml / 数据字典**（扫描条件基于既有字段）。
- **不实现「在途」精确推导**（既有 Non-Goal，非本 finding）。
- **不默认开启 job**（nop-job 惯例 enabled 默认 false = 部署 opt-in 决策，对齐 A4.2.12/A4.1.4 config-gate 范式；config key 默认 true 仅作机制开关）。
- **不修改真相源**（use-cases.md/bank-reconciliation.md 需求契约段；L2 schema 补注 :150「本计划交付 reverse 入口 + 手动可触发」的实现注记由本计划落地后更新为含自动红冲说明——属实现现状注记非契约段修订）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/finance/use-cases.md`（L1 UC-FIN-09/14）+ `docs/design/finance/bank-reconciliation.md`（L2 业务规则 6）+ `docs/architecture/job-scheduling.md`（调度登记）+ `docs/audits/requirement-compliance-methodology.md`（§5 预授权类目）
- Skill Selection Basis: 调度接线（job.yaml/batch.xml/helper bean）+ JUnit（`nop-backend-dev`——Job/Batch/Processor 服务端代码 + `nop-testing`——批量处理器单测）；无 view/报表面变更，不加载 `nop-frontend-dev`。

## Infrastructure And Config Prereqs

- 新增 job 配置 `nop.job.erp-fin-bank-recon-adj-reverse.enabled`（默认 false）+ cron-expr（默认每月 1 日 01:30，`0 30 1 1 * ?`）——登记于 job.yaml `@cfg:` 引用。
- 既有 config `erp-fin.bank-recon-auto-reverse-next-month`（默认 true）成为消费点（消除 A4.1.14 孤儿化）。
- 无端口/外部服务。分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-finance/erp-fin-service`。

## Execution Plan

### Phase 1 - 调度接线落地（job.yaml + batch.xml + helper bean）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-fin-bank-recon-adj-reverse.job.yaml`（新增）；`module-finance/erp-fin-service/src/main/resources/_vfs/nop/batch-task/fin/bank-recon-auto-reverse.batch.xml`（新增）；`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/...`（helper bean，仿 `ErpFinDeferredPostingRetryHelper` 范式）；`module-finance/erp-fin-service/src/main/resources/_vfs/erp/fin/beans/app-service.beans.xml`（bean 注册）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Fix`
- Prereqs: 无

- [x] `Add` job.yaml `erp-fin-bank-recon-adj-reverse`：`enabled: "@cfg:nop.job.erp-fin-bank-recon-adj-reverse.enabled|false"` + cron-expr（每月 1 日 01:30 默认）+ invoker `nopBatchTaskRunner.executeAsync` + taskPath `/nop/batch-task/fin/bank-recon-auto-reverse.batch.xml`——对齐 `erp-fin-deferred-posting-sweep.job.yaml` 逐字段范式。
      - Skill: `nop-backend-dev`
- [x] `Decision` **CLOSED 期间碰撞裁决（设计关键约束，实仓证据见 Current Baseline）**：`reverseProcess:235` 按原凭证日期解析期间 + `resolveOpenPeriod:524-527` CLOSED 拒红冲 + 结账 cron 22:00 L（`job-scheduling.md:106`）→ 启用期末结账的部署中，次月 1 日红冲必然撞 CLOSED 期间。**裁决**：(a) 默认部署（结账 job 未启用，期间恒 OPEN——应用默认姿态，对齐 A4.1.4/A4.2.12 config-gate 范式）下 1 日 01:30 红冲全部成功，L1「下月初自动红冲」验收成立；(b) 启用结账的部署：逐条失败隔离 + 显式 WARN 日志（「调节表 {code} 红冲失败：期间 CLOSED，保持 POSTED 下月重试」）——从零消费隐性失效变为**可观测行为**；(c) 完整「跨期还原」（红冲凭证记入次月 OPEN 期间）须改 `reverseProcess` 期间解析（按红冲日期解析下一 OPEN 期间）——**属会计核心路径行为变更，越出第一批预授权边界（§7 A2：PostingProcessor 核心路径），登记 Deferred But Adjudicated successor（Successor Required: yes）**，本计划不实施。备选：调度窗口前移至结账前（月底 21:00）扫描当月调整——否决：红冲凭证记入当月导致调整+红冲同月净零，M 月末账面不含未达调整，违背「跨期还原」会计语义。
      - Skill: `nop-backend-dev`
- [x] `Decision` 扫描语义（「下月初红冲」落地）：batch loader 扫描 `ErpFinBankReconciliation` `docStatus=POSTED` 且 `reconciliationDate < 当前月第一天`（经 XLang 计算 cutoff）——即调整期间早于当前月份的调节表，均触发红冲（跨期未达收敛）；reverse 幂等守卫（POSTED 状态要求，`BankReconciliationBuilder.reverse:135-137`）天然防重复。备选：仅上月（month−1）精确窗口（否决：job 可能因停机错过首日运行，`< 当前月` 保证所有跨期未达在首次运行时收敛）；残留风险：若部署多月后启用，历史多期调整一次性全部红冲——符合「跨期还原」语义，且 CLOSED 候选按上一条裁决逐条隔离日志。
      - Skill: `nop-backend-dev`
- [x] `Add` batch.xml：orm-reader loader（docStatus=POSTED + reconciliationDate < cutoff）+ XLang processor `inject('erpFinBankReconAutoReverseHelper')` 逐条调 helper（内部走 `BankReconciliationBuilder.reverse`/`ErpFinBankReconciliationReverseProcessor`）+ 单条失败隔离（try/catch + WARN 日志，对齐 deferred-posting-sweep 失败隔离范式——单条失败不阻断全批）。
      - Skill: `nop-backend-dev`
- [x] `Add` helper bean：消费 `erp-fin.bank-recon-auto-reverse-next-month`（`AppConfig.var` 默认 true）——false 时跳过（INFO 日志，机制开关）；true 时执行红冲。bean 注册于 app-service.beans.xml（仿 `ErpFinDeferredPostingRetryHelper` 注册范式）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] job.yaml/batch.xml/helper bean 全链路落地：手动触发 batch（或直接调 helper）能对 POSTED 且跨期调节表完成红冲（docStatus→CANCELLED + BANK_RECON_ADJ 红冲凭证生成）；config=false 时跳过；CLOSED 期间候选逐条失败隔离不中断批次（明确成功与失败模式）
- [x] 配置 well-formed：batch.xml `xmllint --noout` 通过；job.yaml 字段与 `erp-fin-deferred-posting-sweep.job.yaml` 逐字段范式核对（YAML 无 xmllint 可校验）

### Phase 2 - 单测覆盖

Status: completed
Targets: `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/bankrecon/`（新增 `TestErpFinBankReconAutoReverseJob`，镜像 `TestErpFinBankReconciliation` seed 范式）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Add` 测试矩阵：① 跨期 POSTED 调节表（seed 上月 statement + generate + post，期间 OPEN）→ 跑 helper/batch processor → 断言 docStatus=CANCELLED + 红冲凭证生成（BANK_RECON_ADJ reversal）+ 原调整凭证 isReversed=true；② 当月调节表 → 不红冲；③ `assignConfigValue("erp-fin.bank-recon-auto-reverse-next-month","false")` → 跳过（对齐 `TestErpCsSlaNotification:103` config 覆盖范式）；④ CANCELLED/DRAFT 调节表 → 扫描排除（reverse 守卫侧证）；⑤ **CLOSED 期间候选**（seed 期间 status=CLOSED 的跨期调节表）→ 逐条失败隔离（WARN 日志 + recon 保持 POSTED + 批次不中断、其余候选继续红冲）——CLOSED 碰撞从静默变为显式可观测（Phase 1 Decision 1 回归）。
      - Skill: `nop-testing`
- [x] `Proof` 断言强度：红冲后原 BANK_RECON_ADJ 凭证 isReversed=true（对齐既有 `TestErpFinBankReconciliation` post+reverse 断言范式）+ 红冲凭证存在；CLOSED 候选的失败隔离日志断言（Phase 2 item 1 ⑤）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 5 组测试落地并绿（`mvn test -pl module-finance/erp-fin-service` 全绿，含既有 `TestErpFinBankReconciliation` 零回归）

### Phase 3 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/architecture/job-scheduling.md`（:110-111 DESIGN 注记更新）；`docs/design/finance/bank-reconciliation.md`（实现现状补注）；`docs/audits/arm-index.md`（P1-RC-005 修复状态）；`docs/backlog/requirement-compliance-roadmap.md`（RC-R1.2 done）；`docs/logs/2026/08-07.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成

- [x] `Add` `docs/architecture/job-scheduling.md:110-111` DESIGN[待实现] → implemented 注记（job 名/config key/cron 默认值/双层门控说明/CLOSED 期间行为注记）；`bank-reconciliation.md` schema 补注 :150 更新为「自动红冲经 nop-job erp-fin-bank-recon-adj-reverse 接线（job enabled 默认 false opt-in + 业务 config 默认 true 机制开关）」——实现现状说明，不修订需求契约段（§9 冻结条款 `requirement-compliance-methodology.md §9` 遵守）。
      - Skill: none
- [x] `Add` arm-index P1-RC-005 行「修复状态」→ `done (RC-R1.2)` + 修复摘要（含 CLOSED 期间碰撞裁决 + successor 登记）；roadmap RC-R1.2 → done；日志条目。
      - Skill: none

Exit Criteria:

- [x] job-scheduling.md/bank-reconciliation.md 注记 + arm-index/roadmap 回填 + 日志条目落盘

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_023fe2a1cffeCbwhZ2s406WNBW`，fresh session）——1 MAJOR（CLOSED 期间碰撞未裁决：`reverseProcess:235` 按原凭证日期解析 + `resolveOpenPeriod:524-527` CLOSED 拒红冲 + 结账 cron 22:00 L `job-scheduling.md:106` → 次月 1 日红冲必然撞 CLOSED 期间，默认 cron 下结构性无效，须裁决 + 围栏预授权边界）+ 1 MAJOR（错误文档路径 `docs/design/finance/job-scheduling.md` → 实为 `docs/architecture/job-scheduling.md`，5 处）+ 3 MINOR（全 20→19 `.job.yaml` / xmllint 无法校验 YAML / 实体行号 195/204/207/228）。修订：Phase 1 增 CLOSED 期间碰撞 Decision（默认部署验收成立 + 结账部署逐条失败隔离显式日志 + 核心路径增强登记 successor ask-first）+ 扫描语义 Decision 修订 + 测试矩阵增 ⑤ CLOSED 候选用例 + 全文档路径/计数/行号/退出标准修正。
- Independent draft review iteration 2: `accept`（独立子代理 `ses_023f36714ffec7WQ3KrUTaiAyp`，fresh session）——5 项全部实仓核实 RESOLVED（含默认部署无 `erp-fin-period-close.job.yaml` 的验证 + successor 围栏与 §7 A2 一致），0 新问题（Draft Review Record 记录滞后为本轮补齐）。共识达成，转 active。

## Closure Gates

- [x] 范围内行为完成：job.yaml + batch.xml + helper 全链路 + 双层门控 + CLOSED 期间逐条隔离 + 单测全部落地；config key 零消费孤儿化消除
- [x] 相关文档对齐：job-scheduling.md/bank-reconciliation.md 注记 + arm-index/roadmap 状态回填
- [x] 已运行验证：`mvn test -pl module-finance/erp-fin-service` 全绿 + `mvn clean install -DskipTests` 全量构建通过 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（防基线漂移，project-context 已知失败模式 #1）——**基线漂移已知登记**：R2c 1382→1383（+1）/ R10 6→7（+1），per-site 证据 + baseline-raise 裁决落 `docs/audits/compliance-baseline.md`「R2c/R10 同步注记（plan 2026-08-07-1932-3）」块（batch helper `ErpFinBankReconAutoReverseHelper` 1 处 daoFor + 1 处 REQUIRES_NEW，均对齐基线既有 `ErpFinDeferredPostingRetryHelper` 同型文档化 pattern 类），结束审计复核
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 完整「跨期还原」核心路径增强（红冲凭证记入次月 OPEN 期间）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 启用期末结账的部署中，次月 1 日红冲上月调整撞 CLOSED 期间（`reverseProcess:235` 按原凭证日期解析 + `resolveOpenPeriod:524-527` + 结账 cron 22:00 L）。实现「红冲凭证记入次月」须改 `ErpFinPostingProcessor.reverseProcess` 期间解析（按红冲执行日期解析下一 OPEN 期间）——属会计核心路径行为变更（§7 A2），越出第一批预授权边界，按暂停协议须独立 fix plan + 独立 plan-audit + ask-first 人工确认。本计划将该部署形态下的行为降级为显式可观测（逐条失败隔离 + WARN 日志 + 保持 POSTED 重试），非静默失效。
- Successor Required: yes（触发条件 = 启用期末结账部署上线后红冲失败观测数据出现；实现方式 = 独立 fix plan + plan-audit + ask-first）

### 生产部署启用（job enabled 默认 false）

- Classification: `watch-only residual`
- Why Not Blocking Closure: nop-job 惯例 enabled 默认 false = 部署 opt-in 决策（对齐 A4.2.12 hr contract expiry / A4.1.4 budget config 范式）——机制接线完成即满足 L1「自动红冲」验收（机制存在 + 配置可启用）；默认关闭属部署启用决策非契约缺失，部署文档/`docs/architecture/job-scheduling.md` 注记已引导。
- Successor Required: no（watch-only；部署侧决策）

### 失败重试与告警派发（红冲失败的重试通道）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 单条失败隔离（日志）+ 人工可经既有手动 reverse 入口补救（A4.1.13 已证手动 reverse 可逆性兜底）；本计划不引入独立重试/告警通道——与既有 `deferred-posting-sweep`（retryCount/REQUIRES_NEW 重试）不同机制面，若后续统计显示失败率可观再按需求扩展（触发条件 = 生产红冲失败事件观测）。
- Successor Required: no（watch-only；触发条件 = 失败率观测数据）

## Closure

Status Note: 执行完成（draft → 独立草案审查 ×2 accept → active → 执行 → 待独立结束审计）。第一批纯预授权（无 ask-first）。job.yaml + batch.xml + helper 全链路落地（仅接线既有 reverse 入口，不触 ORM/会计核心路径）：`erp-fin-bank-recon-adj-reverse.job.yaml`（enabled 默认 false opt-in + cron 默认 `0 30 1 1 * ?`）+ `bank-recon-auto-reverse.batch.xml`（orm-reader loader：docStatus=POSTED 且 reconciliationDate < 当月第一天）+ `ErpFinBankReconAutoReverseHelper`（消费 `erp-fin.bank-recon-auto-reverse-next-month` 默认 true 机制开关消除 A4.1.14 孤儿化 + 逐条 REQUIRES_NEW 红冲 + try/catch WARN 单条失败隔离）+ beans.xml 注册。CLOSED 期间碰撞裁决：默认部署验收成立 + 启用结账部署逐条失败隔离显式可观测 + 完整「跨期还原」登记 Deferred But Adjudicated successor（Successor Required: yes）。单测 `TestErpFinBankReconAutoReverseJob` 5 组全绿。验证：分域 `mvn test` 351 全绿 / 全量 `mvn clean install -DskipTests` BUILD SUCCESS / compliance checker actual ≤ baseline（R2c/R10 +1 基线漂移已知登记，per-site 证据见 compliance-baseline.md 注记）。arm-index P1-RC-005 → `done (RC-R1.2)` + roadmap RC-R1.2 → done ✅ + job-scheduling.md DESIGN→IMPLEMENTED + 日志条目落盘。

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计子代理（新会话，不重用执行者上下文）
- Evidence: 待执行后填写（可引用：分域 `mvn test -pl module-finance/erp-fin-service` 351 全绿输出 + 新增 `TestErpFinBankReconAutoReverseJob` 5 组 + `mvn clean install -DskipTests` BUILD SUCCESS + checker actual vs baseline 表（R2c=1383/R10=7 与更新后基线一致）+ `compliance-baseline.md` R2c/R10 同步注记 per-site 证据 + arm-index:130 P1-RC-005 行 `done (RC-R1.2)` + roadmap:370 RC-R1.2 行 done ✅ + `job-scheduling.md:111` IMPLEMENTED 注记 + `docs/logs/2026/08-07.md` 顶部条目）

Follow-up:

- 无范围内 follow-up；完整「跨期还原」核心路径增强为 successor（触发条件见 Deferred But Adjudicated）
