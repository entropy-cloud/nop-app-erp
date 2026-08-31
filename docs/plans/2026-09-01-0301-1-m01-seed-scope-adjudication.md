# 2026-09-01-0301-1-m01-seed-scope-adjudication M0.1 种子全量化范围裁决

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Mission: comprehensive-test-data-and-visual-coverage
> Work Item: M0.1 范围裁决——seed 全量化精确口径 + 分层 + 视觉扩面边界 + 270 实体最小数据集规格
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` §Milestone M0 工作项 M0.1（2026-08-31 人工批准转 ready）
> Related: `2026-08-31-1143-1-app-erp-all-default-seed-loading.md`（默认装载语义裁决源）、`2026-08-15-2000-1-seed-data-referential-integrity-test.md`（门禁测试，M0.2 消费）、`2026-07-17-2010-2-pixel-snapshot-visual-regression-baseline.md`（像素基线层）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-09-01）：

- **实体计数多档口径并存**（roadmap §目的已列，M0.1 裁决落地统一口径）：
  - 实仓 grep 权威：`className="app.erp.*"` 唯一计数 **363**（含 notGenCode 子类）。
  - `docs/backlog/README.md` L134 综合条目：**已于 2026-08-31 随批准更新为「约 363」**（实仓 grep 零处 "350"）；旧口径 350 仅存于 roadmap §目的 口径对账表「backlog README L134（过时应更新）」行——该行关于 README 现值的表述已失实，裁决时按「已失效历史档」登记，roadmap 文件本体修订不在本计划范围。
  - `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java` 类注释：Phase 1 Decision (a) 写「418 = app.erp.* 352 + 平台 66」——早于后续 ORM 实体扩展。
  - `docs/architecture/seed-data.md` L26 段落：同样沿用 418 = 352 + 66 口径。
  - plan 1143-1 实证：275 缺 seed（宽口径，含 `erp_sys_config` 等 sys_* 部署表）。
  - roadmap 实仓精确盘点：**270 缺 seed**（窄口径 app.erp.*，不含 sys_* 部署表），按域分布表见 roadmap §当前基线。
- **seed 现状**：`app-erp-all/src/main/resources/_vfs/_init-data/` 共 **97 CSV + 1 SQL**（93 个 app.erp.* 实体有 seed——含 `erp_sys_notification_template` 跨域表 + `erp_md_uom`/`erp_md_uom_conversion` 软缩写命名；4 平台表 `nop_auth_*`×3 + `nop_sys_code_rule`）。
- **`docs/architecture/seed-data.md` 现状**：已有目的/模块职责/种子数据范围/快照重录义务/P2P+O2C 交易单据种子/运营域/制造域/通用引用完整性校验等段；**无「全量化裁决」段、无多档口径对账表、无 270 实体最小数据集规格表**。
- **`docs/testing/e2e-runbook.md` 现状**：已有 §837「像素级截图视觉回归层」段（2010-2 产出）；**无「视觉扩面边界」段**。
- **默认装载语义已定**（plan 1143-1，completed）：`application.yaml` 默认 `init-database-data: true` 演示/沙盒语义 + `scripts/start-app.sh` fresh-DB 重置入口 + 实证 97 CSV + 1 SQL 装载 0 冲突；生产保持缺省关闭。
- **门禁测试现状**：`TestErpSeedDataIntegrity`（2000-1）全量 418 实体（当时的口径）findAll + 1057 to-one 引用完整性 + `WHITELIST_KEYS` 当前为空；`mvn test -pl app-erp-all` 基线 69/0/0/1（known-good-baselines.md 2026-08-31 行）。
- **差距**：M0.1 的五项裁决产出（计数口径 / 数据分层 / 视觉扩面边界 / 默认装载关系 / 270 实体最小数据集规格）全部缺失；M0.2（门禁扩展）与 M1.x（11 项 seed 补齐）均以其为直接前置。

## Goals

- 在 `docs/architecture/seed-data.md` 新增「全量化裁决」段，落地：(a) app.erp.* 计数口径裁决（notGenCode 子类 / sys_* 跨域表 / 部署配置表 / 平台表是否计入 363 全量覆盖目标集）+ 多档口径对账表；(b) seed 数据分层定义（演示种子 / E2E 种子 / 业务动作 negative 种子）+ 与 1143-1 默认装载语义的关系；(e) **270 个缺 seed 实体最小可用数据集规格表**（按域逐实体列表）。
- 在 `docs/testing/e2e-runbook.md` 新增「视觉扩面边界」段，落地：(c) 视觉断言扩面边界（哪些页面进、哪些不进、何时 mask；负向视觉断言归 Non-Goal 的理由）。
- 为 M0.2（门禁按裁决口径扩展）与 M1.x（11 项 seed 补齐）提供可直接消费的权威裁决依据。

## Non-Goals

- **不修改任何 seed CSV 内容**——M0.1 是纯裁决/文档计划；seed 补齐归 M1.x。
- **不修改 `TestErpSeedDataIntegrity.java`**——门禁扩展归 M0.2。
- **不修改任何 ORM 模型**——裁决若发现某实体 seed 无法在 CSV-only 路径下满足最小数据集，登记 Deferred But Adjudicated / successor，不提 ORM 增列要求（roadmap M1.x 默认 CSV-only）。
- **不修改任何视觉 spec / mask 配置**——视觉扩面实施归 M2.x；本计划只写边界规范。
- **不修订 `docs/backlog/README.md`**——其 L134 条目已于 2026-08-31 更新为「约 363」，无遗留失实行；roadmap §目的 口径表「350 backlog」失实行的勘误不在范围，以 seed-data.md 对账表为权威登记处。roadmap §当前基线表仅在 Phase 1 Proof 取证发现漂移时作为条件性修订目标（当前实仓核验无漂移），除此之外不触碰 roadmap 本体。
- **不立即执行 seed 补齐或视觉扩面**——M0.1 仅为裁决。

## Task Route

- Type: `app-layer design change`（纯 owner-doc 裁决产出，不改产品行为与代码）
- Owner Docs: `docs/architecture/seed-data.md` + `docs/testing/e2e-runbook.md`
- Skill Selection Basis: `Skill: none`——纯文档裁决工作，无代码/测试/页面开发；roadmap M0.1 行亦指定 none。执行阶段按 AGENTS.md 强制技能加载规则重扫技能列表确认无匹配。

## Infrastructure And Config Prereqs

- 无基础设施前置（No infra prereqs beyond existing baseline）。
- 裁决取证仅依赖实仓只读命令（`rg`/`xmllint`/文件盘点），无端口/环境变量/外部服务依赖。

## Execution Plan

### Phase 1 - 实仓口径取证与四项裁决（(d) 并入分层 Decision）

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/testing/e2e-runbook.md`、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（条件性——仅 §当前基线表取证发现漂移时修订）
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: 无

- [x] Proof: 实仓复核对账表全部六档来源仍与实仓/源文档 1:1 一致——363（`rg 'className="app\.erp\.' module-*/model/*.orm.xml` 唯一计数）/ 93+4 CSV 盘点（97 CSV + 1 SQL）/ 270（entity tableName ↔ CSV 精确匹配重算）/ 352 + 418（`TestErpSeedDataIntegrity.java` 注释与 seed-data.md L26 段现状）/ 275（plan 1143-1 宽口径原文）/ 350（仅存于 roadmap §目的 口径表的历史档，README 现值已为 363）；同时复核 roadmap §当前基线表与 §目的 口径表无其他漂移，若有漂移先修订 roadmap §当前基线表（§目的 口径表失实行按历史档在对账表中登记，本体修订不在范围）再裁决
      - Skill: none
      - Evidence（2026-09-01 实仓取证）：363 唯一 className（19 域 -c 计数求和与唯一计数双口径一致）；97 CSV（93 erp_* + 4 平台）+ 1 SQL；270 缺 seed 逐域分布与 roadmap §当前基线零漂移；`TestErpSeedDataIntegrity.java:36` 与 seed-data.md「通用引用完整性校验」段均为 418 = 352 + 66；1143-1 L56 原文 275；README 全文 0 处 350（L134 = 约 363）；roadmap §当前基线表零漂移（条件性修订未触发）；另取证 notGenCode 113 处全部无 className 属性（23 物理表 ⊆ className 实体表集）
- [x] Decision: app.erp.* 计数口径裁决——notGenCode 子类、sys_* 跨域表（`erp_sys_notification_template` + `ErpSysNotification`/`ErpSysNotificationRead`）、部署配置表（`ErpSysConfig`）、平台表（nop_auth_* 等）各是否计入「全量覆盖目标集」；在 seed-data.md 写入裁决 + 多档口径对账表（363 实测 / 350 已失效历史档——README 现值 363，该档仅存于 roadmap §目的 口径表 / 352 门禁注释 / 418 seed-data 段 / 275 宽口径缺 / 270 窄口径缺），每档标注来源、核实结论与失效原因；seed-data.md 对账表为口径权威登记处，roadmap §目的 口径表中「backlog 350」失实行的本体勘误登记为历史档不回写 roadmap；替代方案与残留风险写入裁决段
      - Skill: none
- [x] Decision: seed 数据分层定义——演示种子（demo/沙盒，1143-1 默认装载）/ E2E 种子（视觉与行为断言数据可见性）/ 业务动作 negative 种子（隔离用例）三层的边界、命名约定与装载语义关系（prod 关闭语义不变）；写入 seed-data.md 裁决段
      - Skill: none
- [x] Decision: 视觉断言扩面边界——CRUD 页面 / 业务动作对话框 / 报表 / 看板四场景哪些进、哪些不进、何时 mask（日期参数、时间戳、用户名、echarts canvas 末态、AMIS 自适应断点）；负向视觉断言归 Non-Goal 的理由（与数据负向测试耦合度高，successor 候选）；写入 e2e-runbook.md 新增「视觉扩面边界」段
      - Skill: none

Exit Criteria:

- [x] seed-data.md 含「全量化裁决」段：计数口径裁决 + 多档口径对账表 + seed 三层分层定义 + 1143-1 默认装载关系，且对账表每档均标注实仓来源；分层裁决含替代方案与残留风险
- [x] e2e-runbook.md 含「视觉扩面边界」段：四场景进出裁决 + mask 时机标准 + 负向视觉断言 Non-Goal 理由，含替代方案与残留风险

### Phase 2 - 270 实体最小可用数据集规格表

Status: completed
Targets: `docs/architecture/seed-data.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1（计数口径裁决确定 270 目标集后才能列规格表）

- [x] Add: seed-data.md「全量化裁决」段内新增「270 个缺 seed 实体最小可用数据集规格表」——按域逐实体列表（19 域，精确对齐 roadmap §当前基线缺 seed 列），每实体给出：建议 CSV 文件名、行数上限（≤ 20）、主子表组归属、FK 引用闭环依赖（跨域引用显式标注）、最小正例 + 必要 negative 用例指示；与 M1.x 11 工作项拆分对齐（每工作项 ≤ 50 实体），供 M1.x 各 plan 直接引用
      - Skill: none
      - Evidence：规格表程序化生成（ORM 解析 363 实体 ↔ CSV 精确匹配），seed-data.md 实测 270 条 `| Erp*` 规格行 + 19 个带 M1.x 编号的域小节；用例指示编码依据 = ORM status/docStatus/enabled/isActive 列逐实体核验（124/270 带状态列）；mandatory FK 零环、零未知引用（CSV-only 可满足性核验注记已落规格表）

Exit Criteria:

- [x] 规格表覆盖实体数与 Phase 1 复核后的缺 seed 精确数一致（逐域小计相加 = 总数，零遗漏零重复）
- [x] 规格表按域分节且每域标注对应 M1.x 工作项编号（M0.2 / M1.x 起草时可逐行引用）

## Draft Review Record

- Independent draft review iteration 1: needs revision (独立子代理 ses_fa6c8a550fferRAGPcz0dzgIuy) because 1 Blocker——基线误引「backlog README 350」为实仓事实，而 README L134 已于 2026-08-31 更新为「约 363」（grep 零处 350），350 仅存于 roadmap §目的 口径表历史档；对账表按失实档登记会违反 guide 最低规则 1（从实时基线开始）。另 2 Minor：取证项只覆盖 6 档中 3 档、Phase 1 标题「四项裁决」与 3 个 Decision 计数不符。已修订：基线改述 README@363 现值 + 350 降为历史档；对账表 Decision 与 Proof 项覆盖全部六档并登记 roadmap §目的失实行处置方式（历史档登记、本体勘误不在范围）；标题标注「(d) 并入分层 Decision」；Non-Goal 同步改写。
- Independent draft review iteration 2: accept (独立子代理 ses_fa6bf054affe0UiF8Qciwxy75m) because 1 Blocker + 2 Minor 全部确认解决，无新增 Blocker/Major；实仓抽样核验通过（363 实体 / 97 CSV + 1 SQL / 418=352+66 注释 / 1143-1 275 档 / §837 与 seed-data.md 缺段属实）。残留 2 Minor 已随审意见内联修复：(1) Non-Goal 与 Proof 项关于 roadmap 修订的条件性表述张力——Non-Goal 限定 §目的 350 行勘误不在范围 + §当前基线表为条件性目标并列入 Phase 1 Targets；(2) 分层与视觉边界两 Decision 的替代方案/残留风险要求补入 Exit Criteria。

## Closure Gates

> 本计划无代码更改（纯 owner-doc 裁决），删除构建/测试验证门控（理由：无生产代码、无测试代码变更，`mvn test` 结果不受影响）；按 roadmap §规则 9 保留 compliance checker 复跑以确认零漂移。

- [x] 范围内行为完成（两段产出全部落地 seed-data.md / e2e-runbook.md）
- [x] 相关文档对齐（裁决与 roadmap §目的 / §当前基线 / M1.x 拆分无矛盾；与 1143-1、2000-1 既有裁决无冲突）
- [x] 已运行验证：`bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移（docs-only 变更预期零漂移，运行作保险登记）——实测 exit 0、R2c=1542 与 2026-08-31 基线行一致（结束审计独立复跑确认）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 段零条目——CSV-only 可满足性核验通过，触发条件未发生）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision → iteration 2 accept，见 Draft Review Record）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 无法 CSV-only 满足最小数据集的实体（若 Phase 2 发现）

**Phase 2 编制时程序化核验结果（2026-09-01）：零发现**——270 实体 mandatory to-one 关系零环、零指向未知实体，全部可在 CSV-only 路径承载最小数据集（核验注记已落 seed-data.md 规格表「CSV-only 可满足性核验」段），本节保持零条目。

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M1.x 默认 CSV-only 路径；个别实体若确需 ORM 增列才能表达最小数据集，属 ORM 保护区域（auto + dual-agent-approval），须独立 successor plan 承载，不应阻塞裁决收口
- Successor Required: `yes`（触发条件：Phase 2 规格表编制中发现 CSV 列集无法承载该实体最小正例/negative 语义；裁决选项含「降级该实体最小数据集定义」与「ORM 增列 successor」两路，须登记选择理由。**2026-09-01 执行结果：触发条件未发生**；若后续 M1.x 执行中发现新证据，按本条重新评估）

## Closure

Status Note: 两 Phase 全部落地——seed-data.md「全量化裁决」段（计数口径裁决 + 六档对账表 + seed 三层分层 + 270 实体规格表）+ e2e-runbook.md「视觉断言扩面边界」段。全部量化声明经独立结束审计实仓复算 1:1 复现（363 / 93+4 CSV / 270 / 19 域分布 / 113 notGenCode / R2c=1542 零漂移）；8 实体 FK 抽查与 ORM ground truth 一致；roadmap 本体未触碰（符合 Non-Goal）；Deferred 段零条目（CSV-only 可满足性核验通过）。docs-only 零代码变更，构建/测试门控按 Closure Gates 前言删除，以 compliance checker 零漂移替代（执行者 + 独立审计各复跑一次）。

Closure Audit Evidence:

- Auditor / Agent: independent subagent（fresh session，general agent，task id `ses_fa6a73879ffeBM5b2DBaOkH59L`）
- Evidence: 审计报告 VERDICT = **APPROVE**（2026-09-01）——7 审计步全 PASS：①两 Phase 全勾 + Status: completed；②实仓复算 363/93+4/270 与对账表 1:1（README 0 处 350、`TestErpSeedDataIntegrity.java:36` 418=352+66、1143-1:56 275 全部核实）；③规格表 270 行 / 19 域带 M1.x 编号 / 小计和 270 / 零重复 / 8 实体跨 8 域 FK 抽查全对；④e2e-runbook 新段落位正确（像素层段后、报表下载段前）且四场景 + mask 五条 + Non-Goal + 替代方案齐全；⑤roadmap 未改（git status 实证）、与 1143-1/2000-1 无冲突；⑥compliance checker exit 0 + R2c=1542 零漂移（审计独立复跑）+ 日志条目在位 + 工作树仅 3 处 docs 变更零代码；⑦文本一致。审计 Minor 1 项（非阻塞）：工作树存在兄弟计划 `2026-09-01-0301-2-m03-visual-methodology-codification.md`（M0.3 计划，非本计划变更集），提交卫生归各自计划生命周期处置。

Follow-up:

- 无阻塞跟进。M0.2（门禁按 363/270 裁决口径扩展 + 修正 `TestErpSeedDataIntegrity.java:36` 与 seed-data.md「通用引用完整性校验」段两处过期计数）与 M1.x（11 项 seed 补齐）现在可直接消费本裁决与规格表。
