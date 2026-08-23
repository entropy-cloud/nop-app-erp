# 2026-08-23-1752-1-id-string-mission-closure-backfill id-string-migration mission 收尾状态回填（backlog 登记行 + 架构模板 + follow-up 实体化）

> Plan Status: completed
> Last Reviewed: 2026-08-23
> Source: `docs/audits/2026-08-22-0702-open-audit-id-string-migration.md`（P1 发现 1/2/3）+ `docs/audits/2026-08-22-0702-multi-audit-id-string-migration.md`（P1 发现 1/2/3，同题独立并审）
> Related: `docs/backlog/id-string-migration-roadmap.md`（mission 已完成，本计划仅收尾状态回填）；M4.1 计划 `2026-08-23-0434-1/2/3`
> Audit: required

## Current Baseline

- **mission 交付本体已完成且经双审计独立复跑确认**：19 域 1662 列（PK 477 + BIGINT FK 1185）Java 层全 String / DB 层 BIGINT 保持；登记册 259/259 retired；四源证据链（build 156 / test 3808-0-0-1 / E2E 564-30-8 / compliance exit=0）落盘 `known-good-baselines.md`。本计划不触碰任何生产代码。
- **[P1] backlog 登记行未回填**（两审计发现 1 同题）：`docs/backlog/README.md:70` id-string-migration 行仍为 `todo` + 废止计数 1605（08-16 口径；权威 1662）。roadmap 头部已宣告「MISSION 完成 🎉 M0-M4 全 done」；同表 30+ 已完成行惯例 `✅ done`。`AGENTS.md` 快速路由将「选择下一个工作项」指向该表——P0 优先级 + `todo` 状态会诱导后续会话对已完成 mission 重复立项。
- **[P1] 架构 owner-doc 契约模板漂移**（两审计发现 2 同题）：`docs/architecture/data-dependency-matrix.md:632`（§5.6.4 标准代码模式 (b)，notGenCode 外部实体声明模板）示例 id 列仍 `stdDataType="long"`。实况权威源已全量翻转（live 验证：`module-finance/model/app-erp-finance.orm.xml:2245` ErpMdSubject stub id = `stdDataType="string"`）。该节是新增跨域引用的复制粘贴入口——按旧模板执行会重新引入 mission 刚消灭的 stub↔权威源类型错配（`_gen` to-one 胶水对称编译破坏，M0 审计 §10.1 核心的失败模式）。
- **[P1] 承诺的 follow-up 无登记实体**（两审计发现 3 同题）：`docs/backlog/id-string-migration-roadmap.md:15` 声称「孤儿操作人列建模问题已登记 follow-up，另案裁决」，但全 `docs/` 树零登记（README/其他 roadmap/discussions/analysis 均无）。约 15 个操作人列（completedBy/assignedTo/verificationPerson/signedBy/responsiblePerson/resolvedBy/requestedBy/reconciledBy/acceptedBy）在 String-id 世界保持 Long，已产生运行时桥产物（live 验证：`ErpMntVisitReportAdditionalFaultProcessor.java:79-88` `toLongUserId` try-catch 桥）。范围排除本身合法（规则 4），但「已登记」声明无证据支持——建模债无主、无触发条件、不可路由。
- **源审计状态**：两份审计在本计划起草同批已由 `open` 流转为 `> Audit Status: planned`（头部与结论尾行均含指向本计划的分流指针）；仓内惯例终态为 `closed`（89 例先例）。本计划负责兑现修复后将其转入 `closed`。
- **P2 分流已在本计划起草同批完成**：4 项 P2（E2E TS 类型注解 ×49 处/22 文件、roadmap 368→380 计数、`_tmp` 清除注记、mission JSON 废止口径）已登记至 roadmap `## Follow-up Backlog` 节，不进入本计划。

## Goals

- 三项 P1 修复义务全部落地：README 登记行回填 `✅ done` + 权威计数；§5.6.4(b) 模板 String 化 + 方案 B 注记；孤儿操作人列 follow-up 在 backlog 获得登记实体（含触发条件）。
- 两份源审计状态流转到 `closed` 并互相印证（修复兑付指针指向本计划）。
- roadmap:15「已登记 follow-up」声明由 README 行证实（声明-证据一致）。

## Non-Goals

- 不改任何 Java/ORM/E2E/config 生产代码（P2-4 TS 类型注解清理已归 roadmap Follow-up Backlog，触发条件 = 下一轮 E2E 维护批次）。
- 不重跑 build/test/E2E/compliance（mission 四源证据链已在案；本计划零生产代码变更，无回归面）。
- 不处理审计 watch 项（id 非数字串假设、visual 像素套件、E2E 白名单/bug successor——均为 mission 外义务，已有各自 successor 归属）。
- 不批量归档 `docs/plans/`（属季度人工批准流程）。

## Task Route

- Type: `implementation-only change`（已完结 mission 的文档登记/模板收尾修复，非新功能）
- Owner Docs: `docs/backlog/README.md`、`docs/backlog/id-string-migration-roadmap.md`、`docs/architecture/data-dependency-matrix.md`
- Skill Selection Basis: 纯文档行级编辑，无代码/模型/页面/测试面——nop-backend-dev / nop-frontend-dev / nop-testing 均不匹配，`Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 — mission 收尾状态回填（Fix-heavy：4/5 项为 Fix，1 项为 Proof）

Status: completed
Targets: `docs/backlog/README.md`、`docs/architecture/data-dependency-matrix.md`、`docs/audits/2026-08-22-0702-open-audit-id-string-migration.md`、`docs/audits/2026-08-22-0702-multi-audit-id-string-migration.md`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 无（源审计已给出精确落点与修复方向）

- [x] Fix（源：open-audit 发现 1 / multi-audit 发现 1）：`docs/backlog/README.md:70` id-string-migration 行状态 `todo` → `✅ done`；描述中废止计数 1605 对齐权威口径 1662（PK 477 + BIGINT FK 1185）并附「权威口径见 roadmap」指针；行内保留既有 `plan-first`（ORM 保护区域）自主权注记。
  - Skill: none
- [x] Fix（源：open-audit 发现 2 / multi-audit 发现 2）：`docs/architecture/data-dependency-matrix.md:632` §5.6.4(b) 示例 id 列 `stdDataType="long"` → `stdDataType="string"`（`stdSqlType="BIGINT"` 保持），并在代码块下方追加一行注记：Java 层 String / DB 层 BIGINT（orm-model-design.md §主键设计方案 B），防止未来新增跨域引用时 stub 与权威源类型错配。
  - Skill: none
- [x] Fix（源：open-audit 发现 3 / multi-audit 发现 3）：`docs/backlog/README.md` 工作项表增补孤儿操作人列 watch/follow-up 行——优先级定为 `—`（未排期 watch 项，与表内 `—` 行携带 successor/触发条件语义一致）、路线图列指向 `id-string-migration-roadmap.md`、状态 `todo`、描述含触发条件（「操作人列需 FK 化/审计实体化时立案裁决；现状保持 Long + ConvertHelper 桥」），使 roadmap:15「已登记 follow-up」声明被证据支持。
  - Skill: none
- [x] Fix：两份源审计头部与结论尾行 `> Audit Status: planned` → `> Audit Status: closed`，尾行附修复兑付指针（本 plan-id + 三项 Fix 对应关系），保持仓内 `closed` 终态惯例（89 例先例）。
  - Skill: none
- [x] Proof：grep 落点验证四组事实——① README id-string 行含 `✅ done` 与 `1662`；② `data-dependency-matrix.md` §5.6.4(b) 代码块含 `stdDataType="string"` 且全文件该模板无 `stdDataType="long"` 的 id 列示例；③ README 含孤儿操作人列 follow-up 行（grep「孤儿操作人」在 README 命中）；④ 两审计文件 `Audit Status: closed`。全仓 grep「孤儿操作人」的登记缺口（backlog/README 零命中）转为命中。
  - Skill: none

Exit Criteria:

- [x] `docs/backlog/README.md` id-string-migration 行显示 `✅ done` 且计数为 1662 权威口径
- [x] `data-dependency-matrix.md` §5.6.4(b) 示例 id 列为 `stdDataType="string"` 且带方案 B 注记（stdSqlType 保持 BIGINT）
- [x] `docs/backlog/README.md` 含孤儿操作人列 follow-up 行且描述含触发条件
- [x] 两份源审计 `> Audit Status: closed` 且含兑付指针
- [x] roadmap:15「已登记 follow-up」声明可由 README 行证实（声明-证据闭环）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fd1f4b570ffeplbMKBYxeAIbah) — 0 BLOCKER / 1 MAJOR / 3 MINOR：① Current Baseline 误述两审计为 `open`（实况已 `planned`，违最低规则 1）；② Phase 头 `Fix-heavy 5/5` 计数与 Item Types 未含 Proof；③ Fix 1「可附」模糊措辞；④ Fix 3 优先级未钉定（P8 或 —）。修订已落地：基线行改述 `planned` + 分流指针、Phase 头改 `Fix-heavy：4/5 项为 Fix，1 项为 Proof` + `Item Types: Fix | Proof`、Fix 1 改「并附」强制、Fix 3 钉定 `—`。
- Independent draft review iteration 2: accept (ses_fd1f28775ffew3anGnXLAEn2Nr) — 0 BLOCKER / 0 MAJOR / 1 MINOR（迭代 1 记录当时仍为 pending，属本记录填写前的机械占位）。审查者独立复核实况基线全部为真（README:70 `todo`+1605、matrix:632 全文件唯一 `long` 模板、孤儿操作人登记零命中、89 例 closed 审计、finance orm stub `string`、Mnt processor :79-88 桥），四项迭代 1 修订全部验证落地，P1×3 双源归属完整、P2×4 分流可追溯，**共识达成，计划可执行**。

## Closure Gates

> 本计划为 docs-only（零生产代码/契约/模型变更），按计划指南模板规范删除 typecheck/build/lint/test 验证命令门控，以 Phase 1 Proof 的 grep 落点验证替代。

- [x] 范围内行为完成（3 项 P1 Fix + 审计关闭全落地）
- [x] 相关文档对齐（README 登记行 / 架构模板 / roadmap 声明-证据一致；`docs/logs/` 条目在案）
- [x] 已运行验证（grep 落点验证四组事实，见 Phase 1 Proof；docs-only 故无构建/测试门控）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——P2 ×4 已在本计划起草同批分流至 roadmap `## Follow-up Backlog` 节，非本计划范围内项目。）

## Closure

Status Note: 三项 P1 修复义务全部兑付（README:70 `✅ done` + 1662 权威口径 / matrix §5.6.4(b) String 化 + 方案 B 注记 / 孤儿操作人列 watch 行含触发条件），两份源审计转 `closed` 含兑付指针，roadmap:15 声明-证据闭环；docs-only 零生产代码变更（git status 仅 docs/ 路径），grep 落点四组事实全过。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理新会话（general agent，ses_fd1eba3d8ffeeNqubHF6QkVujK）
- Evidence: `VERDICT: passes closure audit`——8 项检查全过（README:70 五列完整 + 1662 与 roadmap :9/:87 权威互证；matrix :632/:640 + 全文件 long 零残留；README:71 触发条件逐字 + roadmap:15 闭环；两审计头部/尾行双处 closed + 发现→Fix 对应；grep 四组复跑全过；git status 仅 docs/ 且 roadmap/log 预存 diff 与计划基线一致、1645-1 显式 superseded 不需处置；Phase 1 全勾 + Closure Gates 审计前正确留空；finance orm:2245 权威 stub string 与注记一致）

Follow-up:

- 无（范围内无已确认缺陷遗留；P2 项归 roadmap Follow-up Backlog，触发条件已登记）
