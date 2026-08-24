# 2026-08-24-1943-1-v1-full-regression-green-baseline V.1 全量回归与全绿基线登记

> Plan Status: active（2026-08-24 独立草案审查 3 轮收敛，见 Draft Review Record）
> Mission: integration-test
> Work Item: V.1
> Last Reviewed: 2026-08-24
> Source: `docs/backlog/integration-test-roadmap.md` MV V.1（Deps = 全部 Bn done，2026-08-24 B10 完成后解锁）
> Related: M0.x（`docs/plans/2026-08-23-1835-{1,2,3}-*.md`）+ B1-B10 各批计划（`docs/plans/2026-08-23-2133-{1,2,3}-*.md`、`docs/plans/2026-08-24-0159-{1,2}-*.md`、`docs/plans/2026-08-24-0541-{1,2}-*.md`、`docs/plans/2026-08-24-0749-{1,2,3}-*.md`）；后继 V.2（收尾对齐，另行起草）
> Audit: required

## Current Baseline

- **B1-B10 全部 done**（2026-08-24 B10 闭包）：22 用例（C01-C21 含 C20a/C20b）+ 试点 `TestErpP2pPilot` 共 23 个集成测试类已落盘 `app-erp-all/src/test/java/io/nop/app/all/it/`（+ `ErpIntegrationTestCase` 基类 + 6 个 FrozenClockExtension：B10/B9/C07/C11C12/C13C14/C15C16C17），RECORDING→CHECKING 往返逐批验证全绿。
- **最新已登记基线**（`docs/testing/known-good-baselines.md` 2026-08-24 B10 行，权威计数源）：`mvn clean install -DskipTests` = **156 reactor 模块 BUILD SUCCESS**；全 reactor `mvn test` surefire XML 权威计数 = **3834 tests / 0 failures / 0 errors / 1 skipped / 642 报告文件**；`mvn test -pl app-erp-all` = **54/0/0/1**。已登记预存失败口径 = **none**（唯一 skipped = `ErpAllWebPagesCollectTest` `@Disabled` 预存 JDK26/ANTLR H-2）。
- **口径漂移注记（本计划必须处理）**：roadmap V.1 行文（2026-08-15 起草）引用的预存失败集合（`ErpAllFluxPagesTest`/`ErpAllWebPagesTest` mfg cell-not-prop + `TestAuthSeedLoadingProof` NPE）**已被后续修复清零**（M0.2 串行化修复 + 各基线行 Known Failures = none 佐证）。零回归边界的锚点 = **执行时点 `known-good-baselines.md` 最新行的已登记口径**（roadmap 原文即「与 known-good-baselines 已登记预存失败口径一致项数不增」），不是 roadmap 行文内的历史快照列举。
- **surefire 串行化在位**：`app-erp-all/pom.xml`（~L232-238）模块级 `forkCount=1` + `parallel=none` 覆盖父 POM `forkCount=4 + parallel=classes`（M0.2 落地，B1-B10 持续生效）；V.1 须按 M0.2 先例以 `help:effective-pom` 复证生效。
- **本 mission（M0.x + B1-B10 各计划）变更面 = 纯测试 + 文档，除两枚预先裁决例外提交**：按**计划来源枚举**核验（非 commit message 前缀——前缀不可靠，见下两条），M0.x 与 B1-B10 的提交仅触及测试 Java/`_cases` 快照/`app-erp-all` pom（surefire 测试配置）/docs/mission 元数据（`missions/integration-test.json`，driver 状态文件免审计）；例外 = 下两条预先裁决的 `750577323` 与 `63781668c`。除例外外部零生产代码、零 seed CSV 变更（横切关注点 3 的 seed 修正授权路径从未触发——B1-B10 全部自包含建数）。
- **预先裁决：commit `750577323` 非 B8 计划范围内变更**。该提交（「xwf submit 步骤 assignment=Starter 落地」）虽携带 B8 计划 `2026-08-24-0749-1` footer，但 B8 计划 Follow-up 已声明该 xwf starter 改造「归其自身会话收口，非 B8 范围」——清理提交误挂 B8 footer。其触及 4 个**生产 xwf 工作流定义**（ast/hr/pur/sal 的 disposal/salary/payment/receipt-approval `v1.xwf`，submit 步骤参与者放宽为提单人本人，属生产审批契约变更）；影响已计入 JUnit 锚点（B8 VERIFY 行 xwf 快照迁移收尾后全量全绿时点起收录，B9/B10 基线叠加于其上）。其契约变更的验证义务归该并发会话自身收口，不由 V.1 承接。
- **预先裁决：commit `63781668c` 非 V.1 审计范围内的本 mission 变更**。该提交虽带 `fix(integration-test):` 前缀，但归属**独立用户直接请求计划** `docs/plans/2026-08-24-1147-1-fix-flux-picker-xlib-schema.md`（flux picker 字段级契约修复），触及生产面资产（`_delta/.../flux-control.xlib` + 27 个 view.xml + native-image 配置）并新增 3 个 Java 测试；其时序**先于** B10 闭包提交 `76f2055ec`（祖先序），+3 测试已计入锚点计数（B9 行「52 = 47 + 2 B9 + 3 并发 flux-picker 会话测试」）——非「B10 之后新增」。其生产面变更的 E2E 验证义务归该计划自身闭包门控（两轮 closure audit 已留痕），不由 V.1 承接。
- **B10 闭包之后的新提交**（起草时点核查）：`5ce72c64d`（纯 docs）+ `88a573297`（TS spec + PNG 截屏，无 Java 测试）——预期不改 JUnit 计数；V.1 差量归因以执行时点 `git log` 实证为准。
- **执行环境风险先例**：并发会话 clean install 竞态曾致 in-repo 全量中断（B9 执行方式注记：aps/prj ClassNotFound，非代码回归 → 隔离工作树副本全量 + in-repo 模块复跑绿桥接）；并发工作树 .m2 构件污染曾致 frozen clock 失效（B8 VERIFY 注记 → master 工作树重装 6 构件）。起草时点工作树 clean、无在途并发计划执行（`2026-08-24-0900-1` 为他 mission draft 未执行）。
- **剩余差距**：V.1 未执行——尚无「B1-B10 完成后时点」的单轮全量绿色验证与基线行登记；耗时模型（设计文档 §4「22 用例套件 ≈ 8-26 分钟」）未实测登记；覆盖矩阵（设计文档 §7）未做 V.1 核验（roadmap 规则 5）。

## Goals

- 在 B1-B10 完成后的当前时点，交付 roadmap V.1 定义的全量回归证明：全部集成用例三层全比对（CHECKING）全绿 + 全 reactor `mvn test` + `mvn clean install -DskipTests` BUILD SUCCESS。
- 按零回归边界定义裁决差量：JUnit 层零新增失败（锚定执行时点 known-good-baselines 最新行口径）；E2E 层零意外回归（边界裁定的执行方式见 Phase 1 Decision）。
- 确认 surefire 串行化在 app-erp-all 生效（effective-pom 证据）。
- 实测登记集成套件总耗时（对照设计文档 §4 模型 8-26 分钟）+ 全量回归墙钟时间。
- 核验覆盖矩阵（设计文档 §7）与实仓 22 用例类一一对应（roadmap 规则 5）。
- 全绿后在 `docs/testing/known-good-baselines.md` 登记 V.1 基线行（含 surefire 证据拷贝路径）；roadmap V.1 → done；日志更新。

## Non-Goals

- V.2 收尾对齐（seed-data.md 双面登记 / e2e-runbook 集成测试节基线刷新与互斥段补全 / backlog README 登记）——后继独立计划。
- CI 自动接线（roadmap Deferred But Adjudicated：out-of-scope improvement，successor 触发条件未满足）。
- 修复**其他 mission** 引入的失败（V.1 只做归因裁决与登记；修复归 owning mission/successor 计划）。本 mission 测试资产自身的失败属范围内 Fix。
- 全量 Playwright E2E 套件重跑（~82 分钟；默认边界裁定为不经由本 mission 变更面传导，见 Phase 1 Decision；如裁定被推翻按 Decision 记录的升级路径处理）。
- `ErpAllWebPagesCollectTest` `@Disabled`（预存 JDK26/ANTLR H-2，依赖 nop-entropy 外部仓库，既有 successor）。
- 任何生产代码 / seed CSV / ORM 变更。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/backlog/integration-test-roadmap.md`（MV V.1 行 + 规则 5 + 横切关注点 4）、`docs/design/integration-testing.md`（§4 耗时模型、§7 覆盖矩阵）、`docs/testing/known-good-baselines.md`（零回归锚点 + 登记载体）、`docs/testing/e2e-runbook.md`「集成测试」节（执行纪律：fresh-DB、与 E2E server 互斥）
- Skill Selection Basis: roadmap V.1 行指定 `nop-testing`——本计划虽为纯验证，但失败排查/快照 CHECKING 模式理解/基类机制均在其覆盖内；若执行中零快照交互则技能仅作背景约束。

## Infrastructure And Config Prereqs

- 无新基础设施。执行纪律前置：`lsof -i :8011` / `lsof -i :8080` 确认无 live E2E server（runbook 互斥纪律）；本地 Maven 仓库含 nop-entropy 父 POM（既有基线）。
- 环境风险预案（非默认步骤，触发时启用）：并发会话竞态 → 隔离工作树副本全量（B9 先例：rsync 排除 .git/target/_tmp/node_modules）；.m2 构件污染疑点 → 涉疑构件从本工作树重装（B8 VERIFY 先例）。

## Execution Plan

### Phase 1 - 边界锚定与执行前裁决

Status: planned
Targets: `docs/testing/known-good-baselines.md`（只读取锚点）、本计划（Decision 落盘）、`docs/design/integration-testing.md` §7（只读核验）
Skill: `nop-testing`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [ ] Proof: 实仓盘点——23 个集成测试类（22 用例 + 试点）+ 基类 + 6 FrozenClockExtension 存在于 `app-erp-all/src/test/java/io/nop/app/all/it/`；设计文档 §7 覆盖矩阵的 22 用例与实仓类 1:1 对应、无矩阵外类/类外矩阵行（roadmap 规则 5 核验，结果记入本计划）。
      - Skill: `nop-testing`
- [ ] Proof: 锚定零回归边界——读取执行时点 `known-good-baselines.md` **最新含全 reactor 计数的基线行**（当前 = B10 行，亦为文件首行；flux-picker 行为 scoped-only 无全量计数，不作锚点），记录权威对照计数（当前预期 = B10 行 3834/0/0/1/642 + 156 模块 + app-erp-all 54/0/0/1）与已登记预存失败口径（当前 = none + 1 skipped）；后续差量以此为准。
      - Skill: `none`
- [ ] Decision: **E2E 层零回归边界的执行方式**。背景：roadmap V.1 的 E2E 子句（「seed 修正引起的既有 value spec/JUnit 快照更新视为已裁决变更逐条登记」）的触发前提是 seed 修正，而 B1-B10 零 seed 变更、本 mission 各计划（M0.x + B1-B10，按计划来源枚举）提交面纯测试/文档（两枚生产面例外已在 Current Baseline 预先裁决归属他计划/他会话）。选项：(a) 全量 E2E 重跑（~82 分钟，重复他 mission 闭包门控，且本 mission 变更面无从传导 E2E 回归）；(b) **构造性满足裁定（推荐）**——按 `Related:` 列举的 M0.x + B1-B10 计划清单逐提交 `git show --stat` 审计，证明枚举计划提交仅触及测试/`_cases`/docs/测试 pom 配置/mission 元数据（`missions/*.json` driver 状态文件免审计；Current Baseline 已预先裁决的 `750577323`/`63781668c` 两枚例外按其裁决归属处理、不计入推翻证据），E2E 层「零意外回归」由本 mission 变更面为零传导成立，审计证据落盘本计划；**审计范围 = 计划来源枚举，非 commit message 前缀**（前缀/footer 均不可靠先例：`63781668c` 前缀误标、`750577323` footer 误挂，均已预先裁决）；(c) 有界 E2E 冒烟（dashboards value 抽样，先例 4/4）。残留风险：枚举遗漏混合提交 → 以逐提交 `git show --stat` 全列而非抽样规避；审计若发现枚举计划内存在**未预先裁决的**生产/seed 变更提交，裁定不成立，按升级路径处理（seed → 横切关注点 1/3 联动重录义务；生产 → 该变更归属计划的闭包门控补证）。
      - Skill: `none`
- [ ] Proof: 互斥与串行化前置——`lsof` 确认无 live server；`mvn help:effective-pom -pl app-erp-all` 实证模块级 `forkCount=1`/`parallel=none` 生效（M0.2 先例）。
      - Skill: `none`

Exit Criteria:

- [ ] 覆盖矩阵核验结果（1:1 对应 + 逐域 ≥2 结论）已记录于本计划。
- [ ] 零回归对照锚点（计数 + 预存口径）与 E2E 边界 Decision（含 git 审计证据）已落盘本计划。
- [ ] 无 live server 占用；effective-pom 输出含模块级串行化配置。

### Phase 2 - 集成套件模块级全绿门（快速隔离门）

Status: planned
Targets: `app-erp-all`（测试执行，零代码变更预期）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1

- [ ] Proof: `mvn test -pl app-erp-all` 全绿——全部 23 集成测试类（含 22 用例 CHECKING 三层全比对）+ 既有基建类通过；计数对照锚点（当前预期 ≥54/0/0/1，差量全额归因执行时点 git log 新增测试，若有）。失败处置分叉：归因本 mission 测试资产 → 范围内 Fix 后复跑；发现**真实产品缺陷** → 横切关注点 4 保护区域门禁（会计/过账 = plan-first + owner doc + tests；ORM/API/数据删除/外部仓库 = auto + dual-agent-approval），**不得在 V.1 内直接改生产代码**，登记显式 successor；归因他 mission/环境 → 按预案处置并记录，不改归因结论。
      - Skill: `nop-testing`
- [ ] Proof: 实测登记套件墙钟时间（对照设计文档 §4 模型 8-26 分钟，结论回写 Phase 4）。
      - Skill: `none`

Exit Criteria:

- [ ] app-erp-all 模块级全绿（0 failures / 0 errors），计数与差量归因记录于本计划。
- [ ] 套件墙钟时间已实测记录。

### Phase 3 - 全 reactor 全量回归与构建验证

Status: planned
Targets: 全仓（测试执行，零代码变更预期）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 2

- [ ] Proof: `mvn clean install -DskipTests` = 156 reactor 模块 BUILD SUCCESS（模块数零漂移；非 156 须归因登记）。
      - Skill: `none`
- [ ] Proof: 全 reactor `mvn test` 单轮运行——surefire XML 权威计数聚合（tests/failures/errors/skipped/报告文件数），对照 Phase 1 锚点差量裁决：**零新增失败**；新增 tests（若有）以 git log 全额归因。并发竞态中断（ClassNotFound/target 清扫类）按 B9 先例隔离副本复跑并桥接登记，不视为代码回归。证据拷贝 `_tmp/v1-surefire-evidence/`（clean 前拷贝）。
      - Skill: `none`
- [ ] Proof: 全量回归墙钟时间实测登记。
      - Skill: `none`

Exit Criteria:

- [ ] install 156 模块 BUILD SUCCESS（或漂移全额归因）。
- [ ] 全 reactor 单轮计数落盘：零新增失败 + 差量全额归因 + 证据目录存在。

### Phase 4 - 基线登记与文档对齐

Status: planned
Targets: `docs/testing/known-good-baselines.md`、`docs/backlog/integration-test-roadmap.md`、`docs/design/integration-testing.md` §4、`docs/logs/2026/08-24.md`（或执行日）
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 3

- [ ] Add: `known-good-baselines.md` 新增 V.1 全绿基线行（命令、计数、Known Failures = 预存口径项数不增、Git State、证据路径 `_tmp/v1-surefire-evidence/`）。
- [ ] Add: roadmap V.1 → done（引用本计划与基线行）+ V.1 details 增一行口径漂移注记（2026-08-15 行文列举的预存失败已修复清零，零回归锚点 = known-good-baselines 执行时点最新行口径，防止陈旧列举误导后继）；设计文档 §4 增耗时实测注记（实测 vs 模型结论）；日志条目（含验证状态）。

Exit Criteria:

- [ ] 基线行、roadmap 状态、设计文档注记、日志四处的计数与结论互相一致（文本一致性检查）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（ses_fcc69283bffed75rHnPnUMwSFr，2026-08-24）because 1 MAJOR（Current Baseline 误刻画 commit `63781668c`：其为独立用户请求计划 `2026-08-24-1147-1` 的生产面变更且时序先于 B10 闭包，非「B10 之后」亦非本 mission 测试面变更，原 Phase 1 git 审计按前缀执行会自我证伪）+ 2 MINOR（FrozenClockExtension 计数 8→6；roadmap 陈旧预存失败列举需漂移注记防误导）+ 2 INFO（Phase 2 失败分叉补横切关注点 4 保护区域门禁条款；基线事实核验全过）。已修订：审计范围改计划来源枚举 + `63781668c` 预先裁决落 Current Baseline + post-B10 提交清单更正（`5ce72c64d`/`88a573297`）+ 扩展计数 6 + Phase 2 产品缺陷门禁条款 + Phase 4 roadmap 漂移注记项。
- Independent draft review iteration 2: needs revision（ses_fcc621fa7ffeI0Am694QjFdraQ，2026-08-24）because 1 MAJOR（枚举内存在未预先裁决的生产变更 commit `750577323`：4 个生产 xwf 工作流定义（ast/hr/pur/sal 审批流 submit 步骤放宽）误挂 B8 计划 footer，B8 Follow-up 已声明归并发会话自身收口——原「零生产代码」断言不成立）+ 1 MINOR（Decision (b) 引用 `Related:` 为枚举源但 Related 缺 M0.x 计划）+ 3 INFO（mission 元数据文件免审计豁免；基线行排序锚点钉住「最新含全 reactor 计数行」；Phase 2 保护区域条款补外部仓库）。已修订：Current Baseline 增 `750577323` 预先裁决（归属并发会话收口 + 影响已含锚点）+ 变更面断言改「除两枚预先裁决例外」+ Related 补 M0.x + Decision (b) 豁免/例外/升级路径完整化 + 锚点行钉住 + Phase 2 条款补全。
- Independent draft review iteration 3: acceptable as-is（ses_fcc5ab632ffeP6y71qjUSlJbeD，2026-08-24）——无 BLOCKER/MAJOR 残留；两轮 MAJOR 修订（63781668c/750577323 预先裁决、枚举审计范围、Related 补 M0.x、豁免/锚点/门禁条款）全部经实仓独立复核落定；残留 1 MINOR（锚点行时序注记方向反了——flux-picker 行实为更早登记，剔除该时序注记即正）+ 2 INFO（Deferred E2E 条目补两例外限定词回声；「新增 3 个 Java 测试」实为 2 类 +1 方法，与 B9 行口径一致），均已就地修正。审查者结论：可接受的执行契约，无需再轮。**草案审查收敛，计划转 active。**

## Closure Gates

> 验证型计划：验证命令即交付物本体，完整仓库验证已在 Phase 2/3 交付，此处汇总门控。

- [ ] 范围内行为完成：Phase 1-4 全部退出标准 `[x]`
- [ ] 零回归边界达成：JUnit 层零新增失败（锚定执行时点基线行口径）+ E2E 层边界裁定成立（git 审计证据在案）
- [ ] surefire 串行化生效确认（effective-pom 证据）
- [ ] 覆盖矩阵 V.1 核验完成（roadmap 规则 5）
- [ ] 耗时实测登记（套件 + 全量墙钟）
- [ ] 基线行登记 + 证据目录在盘
- [ ] 相关文档对齐（known-good-baselines / roadmap / 设计文档 §4 / 日志一致）
- [ ] 无范围内项目降级为 deferred/follow-up（他 mission 归因失败以「显式 successor 归属」登记，非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

- **E2E 全量套件重跑**（若 Phase 1 Decision 采 (b) 构造性满足）：Classification: `watch-only residual`。Why Not Blocking Closure: 本 mission 枚举计划变更面纯测试/文档（git 审计证据；两枚生产面例外提交已预先裁决归属他计划/他会话收口，见 Current Baseline），E2E 回归无从传导；生产面变更的 E2E 验证归各 owning mission 闭包门控。Successor Required: `yes`（触发条件：V.1 执行时点 git 审计发现枚举计划内存在未预先裁决的生产/seed 变更提交，或后续任何计划变更 seed/生产代码时按横切关注点 1/3 联动）。
- **他 mission 归因的新增失败**（若全量回归出现且归因非本 mission）：Classification: `watch-only residual`。Why Not Blocking Closure: 归因裁决 + 显式 successor 登记（owning mission 或独立 fix 计划）后不阻塞 V.1 闭包；V.1 义务是诚实归因而非越权修复。Successor Required: `yes`（触发条件：裁决时点即指定归属）。

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- V.2 收尾对齐计划另行起草（下一 mission driver 轮次；Deps = V.1 done）。
