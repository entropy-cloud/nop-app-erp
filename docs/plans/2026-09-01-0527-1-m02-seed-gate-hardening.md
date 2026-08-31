# 2026-09-01-0527-1-m02-seed-gate-hardening M0.2 种子门禁按 M0.1 裁决口径强化

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Mission: comprehensive-test-data-and-visual-coverage
> Work Item: M0.2 门禁强化——`TestErpSeedDataIntegrity` 按裁决口径扩展 + seed 资产清单基线断言 + 已知回归评估
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` §Milestone M0 工作项 M0.2（`ready`，Deps M0.1 已 `done`）+ M0.1 plan Follow-up（「门禁按 363/270 裁决口径扩展 + 修正两处过期计数」）
> Related: `2026-09-01-0301-1-m01-seed-scope-adjudication.md`（裁决与 270 实体规格表，本计划直接消费）、`2026-08-15-2000-1-seed-data-referential-integrity-test.md`（门禁测试原始计划）、`2026-08-31-1143-1-app-erp-all-default-seed-loading.md`（97 CSV 实证基线）、`2026-08-31-1426-2-app-erp-all-integration-regression-fix.md`（已知回归修复与 hr/drp 预存登记）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-09-01）：

- **`TestErpSeedDataIntegrity` 现状**（`app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`）：
  - `testAllSeedTablesLoadable()` 经 `IDaoProvider.getEntityNames()` **动态枚举全量实体**（363 app.erp.* + 66 平台，随 ORM 演进自动扩展）逐实体 `findAll()` + 「存在 seed CSV 的表行数 > 0」断言（CSV 查找镜像 `DataInitInitializer.loadCsvData`）——roadmap M0.2「扩展至 363+ 实体」与「CSV 存在性 + 行数 > 0」在**机制层面已由 2000-1 建成**；缺口不在机制，在**口径锚定**（见下）。
  - `testNonNullRelationKeysPointToExistingRows()` to-one 全仓引用完整性（主键 join Set 比对 + 非主键 join 列查询）+ `WHITELIST_KEYS` 白名单豁免常量表（当前为空，javadoc 已要求每项豁免注明证据来源）——roadmap M0.2「白名单豁免常量表」**已存在**，无需新建。
  - **口径缺口 1**：无 scope-pinning 断言——门禁不校验 app.erp.* 实体计数与 M0.1 裁决口径（363）一致，ORM 增删实体时门禁静默通过，口径漂移不可见。
  - **口径缺口 2**：无 seed 资产清单断言——`_init-data/` 下孤儿 CSV（无对应实体表的 CSV，M1.x 命名错误的典型形态）不被检测；app.erp.* CSV 基线数（93）与平台 CSV 基线数（4）无常量锚定，M1.x 批次落地时无对账基准。
  - **口径缺口 3**：`TestErpSeedDataIntegrity.java:36` 注释过期计数「418 = app.erp.* 352 + 平台 66」——M0.1 对账表标记为「过期历史档（计数修正归 M0.2 门禁扩展消费）」。
- **`docs/architecture/seed-data.md`「通用引用完整性校验」段**：同样沿用「418 = 352 + 66」过期口径——M0.1 Follow-up 明确修正义务归本计划。
- **M0.1 裁决产出已就绪**（plan `2026-09-01-0301-1`，completed）：`seed-data.md`「全量化裁决」段 = 计数口径权威登记处（363 / 93 / 270 六档对账表）+ seed 三层分层 + 270 实体最小数据集规格表；残留风险条款明示「M0.2 / M1.x / M3.1 各门禁点须按同命令重算刷新，漂移 > 0 时先更新本表再消费」。
- **已知回归状态**（`docs/testing/known-good-baselines.md` 2026-08-31 plan-1426-2 行）：`mvn test -pl app-erp-all` **69/0/0/1 全绿**（原 5 处 C0X 集成回归已全部修清零）；全 reactor 3975 tests 中仅剩 2 处预存回归——hr `TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed`（`module-hr/erp-hr-service`）+ drp `TestErpDrpCrossDock#testStagingTimeoutFallbackJob`（`module-drp/erp-drp-service`），均**不在 app-erp-all 门控范围内**，roadmap 明示归 successor、本 roadmap 不处置。
- **compliance 门禁锚点**：`docs/audits/compliance-baseline.md` §BASELINE 机器可读块为对照权威（当前 `R2c: 1537`）；`known-good-baselines.md` 2026-08-31 plan-1426-2 行记载的「R2c=1542」为该次执行的历史实测值（「+5 已被 ai-check 批/后续合法吸收」），非门禁基线值——本计划零漂移证明对照机器块，不沿用 1542 表述（roadmap 部分 M0.x 行内「R2c=1542 零漂移」措辞属历史实测引用，不作为本计划门控口径）。
- **差距**：M0.2 六项交付（裁决口径锚定 / CSV 清单断言 / 白名单机制核验 / 97 CSV 基线对齐 / 已知回归评估记录 / 两处过期计数修正）全部缺失；M1.x 11 项 DRAFT_PLANS 的 latch（roadmap §执行机制 5：M1.x 须待 M0.2 `done`）以本计划收口为解锁条件。

## Goals

- `TestErpSeedDataIntegrity` 按 M0.1 裁决口径锚定：app.erp.* 目标集计数断言（363 快照常量 + 更新协议）、sys_* 实体在集断言、平台表 findAll 语义保留断言。
- seed 资产清单断言：零孤儿 CSV + app.erp.* CSV 基线常量（93）+ 平台 CSV 基线常量（4），对齐 1143-1「97 CSV」实证基线，为 M1.x 每批落地提供对账基准。
- 修正两处过期计数（`TestErpSeedDataIntegrity.java:36` 注释 + seed-data.md「通用引用完整性校验」段）——M0.1 Follow-up 指定。
- 登记 snapshots 双面重录前置评估记录 + 已知回归状态评估记录（roadmap M0.2 交付项）。
- 满足 M0.2 `done` latch（roadmap §横切 10）：门禁按裁决口径全绿 + 评估记录 + compliance 零漂移 + 日志条目。

## Non-Goals

- **不修改任何 seed CSV 内容**——roadmap M0.2 行明文约束；seed 补齐归 M1.x。
- **不修改任何 ORM 模型**——本计划纯测试代码 + 文档；ORM 变更属保护区域（auto + dual-agent-approval）。
- **不处置 2 处 hr/drp 预存回归**（`TestErpHrDepartmentPositionDeleteGuard` / `TestErpDrpCrossDock`）——roadmap Non-Goal 明示归 successor；本计划仅在评估记录中登记其状态与范围归属（域模块测试，不在 app-erp-all 门控内）。
- **不做逐字段 seed 数据回放测试**——roadmap Non-Goal（「seed 字段值与 owner doc 业务规则一致性」归各域 BizModel 测试）。
- **不改 `DataInitInitializer` 装载机制与 `application.yaml` 配置**——1143-1 裁决语义不变。

## Task Route

- Type: `verification or audit work`（既有门禁测试的口径强化 + 评估记录产出；无生产代码变更）
- Owner Docs: `docs/architecture/seed-data.md`（计数修正 + 门禁语义段对齐）+ `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`（roadmap M0.2 行 Owner Doc 列指明）
- Skill Selection Basis: `Skill: nop-testing`——roadmap M0.2 行指定；本计划为 JUnit 测试扩展（`BaseTestCase` + `CoreInitialization` 手动初始化范式已在既有测试类确立，扩展遵循同范式），技能路由的 `02-core-guides/testing.md` 与 `03-runbooks/write-tests.md` 为执行期必读。

## Infrastructure And Config Prereqs

- 无新增基础设施前置（No infra prereqs beyond existing baseline）。
- 测试沿用既有独立 H2 文件（`jdbc:h2:./db/erp-integrity`）+ `ALL_EAGER` 容器模式 + `nop.orm.init-database-data=true` 测试配置（既有 `@BeforeAll` 已确立，见 `TestErpSeedDataIntegrity.java:68-88`）。

## Execution Plan

### Phase 1 - 门禁测试裁决口径锚定与资产清单断言

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`
Skill: `nop-testing`

- Item Types: `Fix | Add | Proof`（Fix 1 + Add 2 + Proof 1，共 4 项）
- Prereqs: 无（M0.1 裁决已 `done`，规格表与对账表可直接消费）

- [x] Fix: 修正 `TestErpSeedDataIntegrity.java:36` javadoc 过期计数——「418 = app.erp.* 352 + 平台 66」改为动态 findAll 语义表述 + 2026-09-01 快照计数（363 app.erp.* + 66 平台）+ 指向 `seed-data.md` 对账表为口径权威登记处（含 M0.1 残留风险条款的更新协议引用）
      - Skill: `nop-testing`
- [x] Add: 裁决口径 scope-pinning 断言（新增测试方法，如 `testAdjudicatedScopePinned`）——(a) app.erp.* 实体唯一计数断言 = 快照常量 `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（常量 javadoc 写明更新协议：ORM 演进后先重跑 `rg 'className="app\.erp\.' module-*/model/*.orm.xml` 唯一计数 → 更新 seed-data.md 对账表 → 再改常量，禁止跳过对账表直接改常量——消费 M0.1 残留风险条款）；(b) `ErpSysNotification` / `ErpSysNotificationRead` / `ErpSysConfig` 三个 sys_* 语义实体在 app.erp.* 集内（M0.1 计入裁决）；(c) 平台实体（如 `NopAuthUser`）仍在动态 findAll 全量语义内但不计入 363 目标集常量（M0.1 排除裁决）
      - Skill: `nop-testing`
      - 执行注记（2026-09-01）：断言落地时实测运行时 `getEntityNames()` app.erp.* 集 = **368 而非 363**——finance 域 5 个实体（`ErpFinCashForecast` / `ErpFinCreditFacility` / `ErpFinNotesDiscount` / `ErpFinNotesPayable` / `ErpFinNotesReceivable`）模型声明缺 `className` 属性（`rg 'className="app\.erp\.'` 口径天然遗漏），但实体定义完整（列/关系/物理表/生成 Java 类均在，codegen 与运行时按 entity name 默认补齐 className）。M0.1 六档对账表与 270 缺 seed 规格表按该 grep 口径编制，未收录此 5 个（它们也无 seed CSV）。处置：不修改 ORM 模型（保护区），常量保持 363（计划指令口径），新增 `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES` 集合锚定已知 5 个，运行时计数断言 = 363 + 5 双向漂移显式失败；发现登记 seed-data.md「全量化裁决」段补充档（见 Phase 2 Fix 项）+ 本计划执行注记；M1.x seed 补齐须按运行时口径消费（finance 实际缺 seed 26+5=31，全集 270+5=275）——roadmap 本体勘误归 roadmap owner，本计划不越权改写
      - Skill: `nop-testing`
- [x] Add: seed 资产清单断言（并入 scope-pinning 方法或独立方法）——(a) 零孤儿 CSV：`_init-data/` 每个 `.csv` 文件名 ↔ 已知实体 tableName 精确匹配（含 `erp_md_uom`/`erp_md_uom_conversion` 软缩写命名既有事实），不匹配即失败（M1.x 新增 CSV 命名错误的前置门禁）；(b) app.erp.* CSV 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 93` + 平台 CSV 基线常量 `EXPECTED_PLATFORM_CSV_COUNT = 4`（对齐 1143-1「97 CSV」实证基线快照；javadoc 写明 M1.x 每批落地后的随批更新协议：更新常量 + 同步 seed-data.md 对账表 + 在该批 plan 中登记）；(c) 「存在 seed CSV 的表行数 > 0」既有断言保留核实（不重写，Proof 项确认覆盖）
      - Skill: `nop-testing`
      - 执行注记（2026-09-01）：落独立方法 `testSeedAssetInventoryBaselines`；CSV 归类按实体名前缀（`erp_sys_notification_template` 等表归属由 className 决定，文件名前缀 `erp_` 恰与 93/4 分项一致）；(c) 已核实：既有「行数 > 0」断言保留于 `testAllSeedTablesLoadable` 未重写，覆盖确认
- [x] Proof: 白名单豁免机制与 M0.1 裁决对齐核验——roadmap M0.2「新增白名单豁免常量表」与实仓现状对账：机制已由 2000-1 建成（`WHITELIST_KEYS` + `whitelistKey()` + javadoc 证据来源要求），本项输出核验注记（写于测试类 javadoc 或计划执行注记）：三元组登记纪律、证据来源要求、占位软引用天然跳过语义均已满足 M0.2 需求；仅当核验发现缺口（如缺登记格式约束）才升级为独立 Add 项并在计划中记录理由
      - Skill: `nop-testing`
      - 执行注记（2026-09-01）：核验通过零缺口——三元组格式约束（`whitelistKey()` helper 统一 `ownerEntity|relationName|key` 拼接）+ 每项豁免证据来源要求（字段 javadoc）+ 占位软引用天然跳过（无 `<to-one>` 关系不入 `getRelations()` 扫描）三项均满足，未升级 Add 项；核验注记已写于测试类 javadoc（「白名单豁免机制核验注记」段）

Exit Criteria:

- [x] 新增断言全绿：scope-pinning（363 / sys_* 在集 / 平台语义）+ 资产清单（零孤儿 CSV / 93 + 4 基线常量）在 `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 下通过
      - 实测（2026-09-01）：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` BUILD SUCCESS（既有 2 方法 + 新增 2 方法）
- [x] 常量更新协议已写入 javadoc（scope-pinning 常量 + CSV 基线常量两处），协议与 M0.1 残留风险条款及 seed-data.md 对账表一致

### Phase 2 - 已知回归评估、文档对齐与 latch 验证

Status: completed
Targets: `docs/architecture/seed-data.md`、`docs/logs/2026/09-01.md`
Skill: `nop-testing`

- Item Types: `Fix | Add | Proof`（Fix 1 + Add 1 + Proof 2，共 4 项）
- Prereqs: Phase 1（评估记录须基于新门禁全绿的实测结果）

- [x] Add: snapshots 双面重录前置评估记录（落本计划执行注记 + seed-data.md 相关段一句话登记）——(a) 本计划不修改任何 seed CSV → 「执行期 seed 变更触发快照重录双面义务」（roadmap 横切 3）**未触发**，M2.x 相关 spec 无需重录；(b) 已知回归状态评估：C03/C04/C08/C09/C12 已由 plan 1426-2 修清零（2026-08-31 基线行 69/0/0/1 实证，无需重录评估）；2 处 hr/drp 预存回归位于域模块测试（`module-hr/erp-hr-service` / `module-drp/erp-drp-service`），不在 app-erp-all 门控与 M1.x latch 判据内，维持 successor 归属；该评估记录即 roadmap M0.2 行「snapshots 双面重录前置评估」交付物
      - Skill: `nop-testing`
      - 执行注记（2026-09-01）：评估记录落 plan「Execution Record」节（两项齐备 + latch 四条件对照）；seed-data.md「通用引用完整性校验」段「门禁强化已落地」bullet 内一句话登记（快照重录义务未触发）
- [x] Fix: 修正 `seed-data.md`「通用引用完整性校验」段过期计数（418 = 352 + 66 → 动态 findAll 全量语义 + 363 快照 + 指向「全量化裁决」段对账表）——M0.1 Follow-up 指定的两处修正之一（另一处已在 Phase 1 Fix 项完成）
      - Skill: `none`
      - 执行注记（2026-09-01）：已修正（动态全量语义 + 363+5+66 快照 + 对账表指针）；历史实证行「418 实体」补历史口径标注；对账表补「运行时注册实体集 368 = 363 + 5」补充档（Phase 1 发现登记，消费 M0.1 残留风险条款「先更新本表再消费」）
- [x] Proof: `mvn test -pl app-erp-all` 全绿（≥ 70 tests——69 既有 + ≥ 1 个新增 `@Test` 方法，0 failures 0 errors；M0.2 latch 第一条件）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移：对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块（当前 `R2c: 1537`）；实测 actual 与机器块的既有差值（known-good-baselines 1426-2 行登记的 1542 实测）在 Phase 2 评估记录中登记为 pre-existing，不作为本计划新增漂移（本计划零生产代码变更；若出现机器块之外的新增漂移，按 known-failure-mode 规则处置——M0.2 latch 第二条件）
      - Skill: `nop-testing`
      - 执行注记（2026-09-01）：实测 `Tests run: 71, Failures: 0, Errors: 0, Skipped: 1` BUILD SUCCESS（既有 skip 1 与基线行一致；69 既有通过 + 2 新增通过 = 70 通过 ≥ 70 判据）；checker 实测 R2c=1542 / R2b=242 / R12a=71 三处既有差值经计数域论证（checker `-prune test` 目录 + `.md` 不在扫描面）登记为 pre-existing 与历史实测精确吻合，机器块之外零新增漂移——详见 Execution Record「compliance 实测与既有差值登记」
- [x] Proof: 执行条目登记 `docs/logs/2026/09-01.md`（M0.2 latch 第三条件；roadmap §横切 10 的「08-{day}」为 8 月起草时点的日期模式表述，按 AGENTS.md 日志规则取实际执行日 `2026/09-01.md`；条目含验证命令输出摘要与「本计划不修改 seed CSV」声明）
      - Skill: `none`
      - 执行注记（2026-09-01）：已登记（倒序置顶；含 4/4 + 71/0/0/1 + checker 差值登记 + 「不修改任何 seed CSV」声明 + stash 事故恢复登记）

Exit Criteria:

- [x] seed-data.md「通用引用完整性校验」段计数与「全量化裁决」段对账表一致（零内部矛盾）
- [x] 评估记录两项（快照重录未触发 + 已知回归状态）落盘且与 roadmap M0.2 行交付描述对应
- [x] `mvn test -pl app-erp-all` 全绿 + compliance 零漂移实测输出登记

## Execution Record（M0.2 交付物评估记录，2026-09-01）

**snapshots 双面重录前置评估**（roadmap M0.2 行交付物；横切 3 义务触发判定）：

1. **快照重录双面义务未触发**：本计划零 seed CSV 变更（Non-Goal 1 约束全程遵守——`git status` 实测仅测试类 + 文档变更），「执行期任何 seed 变更触发快照重录双面义务」（roadmap 横切 3）不满足触发条件；面 1（各域 `_cases`）与面 2（`app-erp-all/_cases/it`）快照基线均无需重录，M2.x 相关 spec 无需重录评估。
2. **已知回归状态评估**：
   - C03/C04/C08/C09/C12 五处 app-erp-all 集成回归：已由 plan `2026-08-31-1426-2` 修复清零（`docs/testing/known-good-baselines.md` 2026-08-31 plan-1426-2 行实证 `mvn test -pl app-erp-all` **69/0/0/1 全绿**），无需重录评估；
   - 2 处 hr/drp 预存回归：`TestErpHrDepartmentPositionDeleteGuard#testDeleteEmptyPositionAllowed`（`module-hr/erp-hr-service`）+ `TestErpDrpCrossDock#testStagingTimeoutFallbackJob`（`module-drp/erp-drp-service`）——均位于**域模块测试**，不在 app-erp-all 门控范围内、不在 M1.x latch 判据内（roadmap §已知 2 处预存回归明示），维持 **successor 归属**，本计划不处置（Non-Goal 3）。

**M0.2 latch 四条件对照**（roadmap §横切 10）：门禁按 M0.1 裁决口径全绿（Phase 1 Exit Criteria 实测 4/4）+ 已知回归评估记录（本节）+ compliance 零漂移（Phase 2 Proof 项实测）+ 日志执行条目（`docs/logs/2026/09-01.md`）。

**compliance 实测与既有差值登记**（2026-09-01，`bash docs/audits/nop-compliance-checker.sh`）：

- 实测：R2c=**1542**（基线 §BASELINE 机器块 1537，差 +5）；R2b=**242**（240，+2）；R12a=**71**（70，+1）；其余规则全部 ≤ 基线（R1d=14 / R2a=34 / R2d=38 / R3=5 / R6=2 / R10=14 / R12b=66 / R12c=42，R4/R5/R7/R8/R11=0）。
- **差值登记为 pre-existing（非本计划新增漂移）**：本计划变更面 = `src/test/java` 测试类 + docs/Markdown，checker 计数域实证排除二者——`rgrep_prodjava`/`rgrep_bizmodel` 均 `-type d -name test -prune`（checker 脚本 L44-57），`.md` 不在 `*.java` 扫描面 → 计数输出与本计划变更无关（有/无本计划变更输出恒等）。三处差值与登记在案的历史实测精确吻合：R2c=1542 = `known-good-baselines.md` 2026-08-31 plan-1426-2 行「R2c=1542（与基线 1537 差 +5 已被 ai-check 批/后续合法吸收）」；R2b=242 = `docs/logs/2026/09-01.md` F2.4 条目「R2b=242/R2c=1542 vs 基线 240/1537 漂移归属兄弟 mission 提交」；R12a +1 同窗口兄弟 mission 增量。机器块之外无新增漂移，未触发 known-failure-mode 处置路径；baseline-raise 归 compliance 审计轮（与 F2.4 条目既有登记一致，非本计划义务）。

**执行期事故登记（已完全恢复，零净影响）**：compliance 前置证明尝试使用 `git stash push/pop` 做有/无本计划变更的对照实测——push 因 plan 文件未跟踪而失败（零 stash 创建），但误触 pop 弹出了**预存旧 stash**（`stash@{0}` WIP: plan-1351-3，本会话开始前已存在），致 7 个与本计划无关的 finance/sales 文件出现合并冲突。处置：7 文件全部 `git checkout HEAD --` 精确还原（它们在 pop 前均为 clean 态），冲突标记 grep 复核零残留，旧 stash 条目原样保留未动，本计划变更（测试类 + seed-data.md）完好。教训登记：mission 会话工作树常带预存 stash/dirty 态，禁用 `git stash` 做对照实验，改用 checker 计数域静态论证（见上）。

## Draft Review Record

- Independent draft review iteration 1: needs revision (独立子代理 ses_fa643c894ffev6vkCnkyYp1E0g) because 1 Major + 2 Minor——Major：compliance 锚点误引「R2c=1542（compliance-baseline.md §BASELINE）」（实仓 §BASELINE 机器块 = R2c: 1537；1542 仅为 known-good-baselines 1426-2 行历史实测值，而 compliance 零漂移是 M0.2 latch 四条件之一）；Minor 1：「≥ 71 tests」与计划自身「断言可并入单方法」选项冲突（并入时仅 70）；Minor 2：roadmap §横切 10 字面「08-{day}」与执行日 `09-01.md` 的日期模式差异未加说明。全部已修订：基线锚点改 §BASELINE 机器块（R2c: 1537）+ 1542 降级为历史实测引用 + 1537/1542 差值登记为 pre-existing 的处置路径；测试数改「≥ 70（69 既有 + ≥1 新增 @Test 方法）」；日志项补日期模式说明。
- Independent draft review iteration 2: accept (独立子代理 ses_fa63c0e7cffe80QWacL2j8iTuX) after 上述 1 Major + 2 Minor 全部确认修复（§BASELINE=R2c:1537 实仓核实、修订后零漂移证明可执行——含已知差值登记与未知漂移升级路径；latch 四条件全覆盖；基线诚实性实仓抽查通过）；残留 1 Minor 为非阻塞记账（Draft Review Record 补填），已随转 active 前内联修复。

## Closure Gates

> 测试代码 + 文档变更，无生产代码变更；构建验证以 `mvn test -pl app-erp-all` 为准（全量 `mvn clean install -DskipTests` 在 Closure 运行一次确认 156 reactor 模块不受影响——新增常量与断言仅存在于测试类，预期无编译外溢）。

- [x] 范围内行为完成（Phase 1 断言落地 + Phase 2 评估与文档修正落地）
- [x] 相关文档对齐（seed-data.md 两段一致；与 M0.1 对账表 / 1143-1 基线 / 1426-2 回归登记无矛盾）
- [x] 已运行验证（`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` + `mvn test -pl app-erp-all` 全绿 + `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 零漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] M0.2 latch 条件全部满足（roadmap §横切 10）：门禁按 M0.1 裁决口径全绿（Phase 1 Exit Criteria：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 4/4/0/0）+ 已知回归评估记录（Execution Record 两项）+ compliance 零新增漂移（Phase 2 Proof：实测差值经计数域论证登记 pre-existing，与历史实测精确吻合）+ 日志执行条目（`docs/logs/2026/09-01.md` 置顶条目）

## Deferred But Adjudicated

（无——本计划范围闭环；2 处 hr/drp 预存回归为 roadmap Non-Goal 显式排除项 + 既有 successor 归属登记，非本计划降级）

## Closure

Status Note: 2026-09-01 全 Phase 落地并经独立结束审计 APPROVE 后闭包。交付：`TestErpSeedDataIntegrity` M0.2 门禁强化（scope-pinning 363+5 双向锚定 / sys_* 在集 / 平台语义保留 / 零孤儿 CSV / 93+4 CSV 基线常量，4/4/0/0 全绿）+ 白名单机制核验注记（零缺口）+ 两处过期计数修正（测试类 javadoc + seed-data.md 通用校验段）+ 快照重录义务未触发与已知回归评估记录（plan Execution Record）+ 日志条目。执行期发现 M0.1 className-grep 口径盲区（finance 域 5 个声明缺 className 实体，运行时集 368=363+5），经 seed-data.md 对账表补充档 + 测试常量双向锚定登记，M1.x 按运行时口径消费（roadmap 本体勘误归 owner）。compliance 实测差值（R2c +5 / R2b +2 / R12a +1）经 checker 计数域论证登记 pre-existing。零 seed CSV / 零 ORM / 零生产代码变更。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 fresh session（`ses_fa61519d9ffet6f8rOhOK6Jbhm`）——**CLOSURE_AUDIT: APPROVE**（8/8 审计项全 PASS：计划一致性 / 测试代码 / 实仓真相抽查（363 复算、5 实体无 className 有生成类、97=93+4 CSV）/ 文档零矛盾 / 范围纪律（零 seed CSV/ORM/生产代码 diff）/ 日志条目 / 已知回归处置 / latch 四条件 + pre-existing 登记健全性）
- Evidence: 审计独立复算（非采信执行者声明）：className 唯一计数 363、5 个 finance 实体 ORM 声明与生成类并存、`_init-data/` 97 CSV 构成、`git status` 变更面、checker 脚本 `-prune test` 计数域（nop-compliance-checker.sh:55）、§BASELINE 机器块（R2c:1537/R2b:240/R12a:70）与 known-good-baselines 1426-2 行（1542 历史实测）及 F2.4 条目（R2b=242）交叉吻合；stash 事故 7 文件 `git diff HEAD` 零残留 + 旧 stash 条目原样保留复核通过。4 Minor 非阻塞观察：①闭包收尾勾选与状态翻转（本节即履行）；②R12a=71 无独立历史锚点——建议下轮 compliance 审计在 known-good-baselines 登记；③368 发现的系统性口径盲区已由 seed-data.md:508 补充档登记，M1.x 按运行时口径消费；④stash 恢复依赖「7 文件事前 clean」假设——当前零 diff 状态与该假设一致。

Follow-up:

- (仅非阻塞跟进项目；M1.x 11 项 seed 补齐为本计划收口后 roadmap 自然解锁的后续工作项，非本计划 Follow-up)
