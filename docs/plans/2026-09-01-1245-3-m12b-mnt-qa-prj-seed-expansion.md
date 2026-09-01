# 2026-09-01-1245-3 M1.2b 核心业务域 seed 扩面 B——maintenance + quality + projects（28 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.2b（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、同批 `2026-09-01-1245-1-m12a1-finance-seed-expansion.md`（N=1）/ `2026-09-01-1245-2-m12a2-manufacturing-seed-expansion.md`（N=2）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径），133 个有 seed（`app-erp-all/src/main/resources/_vfs/_init-data/` 实测 137 CSV = 133 app.erp + 4 平台，+ 1 SQL `zz-sequence-advance.sql`）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 133` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，非本域不触碰）。
- 三域实测已 seed / 缺 seed（与 roadmap M1.2b 清单一致）：
  - maintenance：已 seed 8（equipment / equipment_category / downtime_entry / request / schedule / spare_part_usage / visit / visit_task），缺 **7**（calibration / equipment_status_log / maintenance_team / maintenance_team_member / spare_part_usage_line / task_template / task_template_line）；
  - quality：已 seed 6（action / inspection / non_conformance / spc_chart / spc_sample / spc_capability），缺 **10**（calibration / inspection_line / inspection_template / inspection_template_line / quality_goal / recall / recall_target / review / risk_register / sampling_plan）；
  - projects：已 seed 6（project / project_type / project_pnl / budget / cost_collection / timesheet），缺 **11**（activity_type / billing / billing_line / budget_line / cost_collection_line / milestone / project_settlement / project_settlement_line / project_user / role / task）。
  - 逐实体规格见 `docs/architecture/seed-data.md` 规格表 maintenance / quality / projects 三节，本计划逐行引用不复制。
- 既有 FK 锚点已就绪：`ErpMntEquipment` / `ErpMntSparePartUsage`（0930-2）、`ErpQaInspection`、`ErpPrjProject` / `ErpPrjBudget` / `ErpPrjCostCollection`（2210-1）+ 跨域主数据（md employee/partner/material/uom）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 plan `2026-09-01-0527-1` 实测，经 M1.1a/M1.1b/M1.1c 三批维持；`known-good-baselines.md` 最新登记行 69/0/0/1 系 2026-08-31 plan-1426-2 口径，登记行滞后于 M0.2 后实测，基线追行归 M3.1）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542** 为历史实测值（+5 pre-existing 增量已登记，沿 plan `2026-09-01-0527-1` 口径协议）；全 reactor 已知 2 处预存回归（hr/drp）为 roadmap Non-Goal，与本计划无关。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；UoM 外键列名为 `UO_M_ID`（分叉拼写陷阱——seed-data.md L297 对 `erp_mnt_spare_part_usage_line` 显式标注，M1.1c 闭包实证同族）；装载拓扑序由 `DataInitInitializer` 自动排序。
- 冻结时钟纪律（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期列使用静态固定值，禁止 `now()`/滚动期间语义（calibration/检查/里程碑/结算类实体尤其注意）。
- **干扰面**（本计划核心风险）：C09 `TestErpC09QaNcrCapaScrap`（qa 面）与 C12 `TestErpC12PrjTimesheetSettlement`（prj 面）集成用例读对应域表；quality 看板 SPC 三计数器读 `spc_*` + `non_conformance`（均不在本批扩展面）；mnt/prj 看板读取面以预分析为准。新增行**可能**触发面 2 集成快照与视觉双面漂移，须预分析 + 按协议重录。
- Deferred 消费：seed-data.md L297 登记「备件消耗行 `erp_mnt_spare_part_usage_line` seed——触发条件：备件消耗明细端到端回归需行数据时（注意 UoM 列名 `UO_M_ID` 陷阱）」——本计划按 roadmap 全量覆盖口径消费该 Deferred（触发条件由全覆盖目标取代）。

## Goals

- 28 个 seed CSV 落地（mnt 7 + qa 10 + prj 11，逐行消费规格表三节），每实体最小可用数据集（行数 ≤ 20，按规格表建议行数与用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空，运营域主子表链路完整。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（基数以「133 + 已落地批次 CSV 数」为准，本批 +28），seed-data.md 对账表同步。
- `docs/design/maintenance/seed-data.md` + `docs/design/quality/seed-data.md` + `docs/design/projects/seed-data.md`「种子数据」owner doc 段落地（3 个新文件）。

## Non-Goals

- 不修改任何 ORM 模型（`module-*/model/*.orm.xml`，保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不触碰 SPC/NCR/结算引擎与 Processor 逻辑（seed 为被动数据）。
- 不覆盖 finance / manufacturing 及其余 M1.x 工作项的缺 seed 实体（归同批 N=1/N=2 与后续批次）；不触碰 finance 5 个缺 className 运行时实体（M1.2a1 口径）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不引入 production-grade 真实个人数据。
- 不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 maintenance/quality/projects 三节 + 对账表 + L297 Deferred）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.2b 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）
- Skill Selection Basis: roadmap M1.2b 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定，技能路由到平台模型文档）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批三计划（N=1/2/3）之三；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，若 N=1/N=2 先落地，常量基数以「133 + 已落地批次 CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（28 CSV = 7 + 10 + 11）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表全部必填 FK 锚点均已 seed）

- [x] 逐实体核对 ORM 表名与列（`module-maintenance` / `module-quality` / `module-projects` 三域 `model/*.orm.xml` tableName / `code=`）后，按规格表 maintenance / quality / projects 三节（28 行，逐行引用不复制）创建 28 个 CSV；主子表组：mnt（maintenance_team + member / task_template + line）、qa（inspection_template + line / recall + target）、prj（billing + line / project_settlement + line）；独立表按规格表（mnt 3 + qa 6 + prj 7）；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpMntEquipment / ErpMntSparePartUsage / ErpQaInspection / ErpPrjProject / ErpPrjBudget / ErpPrjCostCollection / ErpMdEmployee / ErpMdPartner / ErpMdMaterial / ErpMdUoM）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期、UoM 列 `UO_M_ID`（`erp_mnt_spare_part_usage_line` 显式中陷阱点）；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_mnt_spare_part_usage.csv` / `erp_prj_budget.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c 先例）
      - Skill: `nop-backend-dev`
- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——grep `app-erp-all/_cases` 集成用例源码中本批 28 表引用面（重点 C09 qa NCR/capa/scrap 面、C12 prj timesheet/settlement 面）、三域看板/报表 BizModel 读取面（quality SPC 三计数器读 spc_* + non_conformance——不在本批扩展面；mnt 看板读 equipment/visit/request/schedule——不在扩展面；prj 看板读 project/cost_collection/project_pnl——不在扩展面，均须 grep 实证）、三域相关 `*.value.spec.ts` 断言面，判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

**执行证据（Phase 1）**：

1. **28 CSV 落地**（mnt 7 + qa 10 + prj 11，共 72 行，单文件 ≤ 3 行 ≤ 20 上限）：逐实体列集按三域 `model/*.orm.xml` 核对（脚本化提取 28 实体列/关系/UK），行数与用例指示编码逐行对齐规格表；UoM 分叉拼写陷阱 `UO_M_ID` 在 `erp_mnt_spare_part_usage_line.csv` 显式落实（列头第 5 列）。
2. **列错误门禁首跑拦截实证**（沿 M1.1c 先例）：首跑拦截 `erp_qa_inspection_line.csv` 行 3 SPEC_MIN 错位（`A` 串列），修正后二跑仅剩常量断言（190≠218，Phase 2 处置）→ 三跑 `loadable / nonNullRelation / scopePinned` 三断言全绿（零孤儿 CSV / 零悬空 FK / 行数 > 0）。
3. **干扰面预分析结论（零漂移预期 + 两条种子侧设计裁决）**：
   - ①**C09 qa 面**：C09 自包含建数（自有 inspection/action/NCR），qa 看板 `getDashboardKpi`（inspection/action 头表计数）+ SPC 三计数器（spc_* + non_conformance）读取面均不在本批 10 表内；`erp_qa_inspection_line` 种子行挂 seed 检验单 1/2/3（非 C09 自建 id）零交集。
   - ②**C10 mnt 面**：C10 写 `erp_mnt_spare_part_usage_line` + `erp_mnt_equipment_status_log`（本批 2 表）——过账按自建 usage id 作用域（种子行挂 seed usage id=1）；状态日志读取面唯一消费方 = `EquipmentRuntimeCalculator.findLogs`（OEE / generateDueVisits RUNTIME 触发）——C10/C11/C12 均不调用，E2E `maintenance-oee.value.spec` 断言锚定「无 workcenterId → OEE 恒 null / computedCount=0」（equipment 表不在本批，谓词输入零变化）→ 种子状态日志行对 OEE 面显示惰性；快照行为序列 id ≥ 100000（zz-sequence-advance 域），显式种子 id < 100000 不入快照、不耗序列（M1.2a2 同口径）。
   - ③**C12 prj 面**（最高风险面）：结算金额派生自 seed PNL 快照行（`pnlBiz.getProjectPnl` → `erp_prj_project_pnl`，不在本批）→ 金额断言零变化；`createSettlement` 守卫 `findActiveSettlementOfType(projectId=1, FINAL/CLOSE)` 排除 docStatus=CANCELLED → **裁决 A：种子结算行 = INTERIM/DRAFT + FINAL/CANCELLED**（P 行挂 INTERIM 类型 + N-TERM 行挂 CANCELLED，守卫双侧零触发）；`buildLines` 迭代 `findBillings(projectId)`（仅排除 CANCELLED）→ **裁决 B：种子 billing 行全批 docStatus=CANCELLED**（非取消行会为 C12 新建结算单追加 INCOME 行并使既有归集行 lineNo 移位 → output-row 快照失配）；`erp_prj_cost_collection_line` 种子行留空 sourceBillType/Code（手工归集语义，`findCollectionLine` 按 TIMESHEET + 自建单号反查零交集）；行合计与既有头表对账（budget_line 50000 / cost_collection_line 30000 / billing_line 20000+30000）。
   - ④**看板/报表/value spec 面**：mnt 看板 KPI（equipment/request/visit 计数）+ 2 报表（visit/spare_part_usage/downtime_entry 头表）、qa 看板（inspection/action/spc/non_conformance）、prj 看板（project/budget/cost_collection/project_pnl）+ `maintenance/quality/projects.value.spec` 期望值——消费表均不在本批 28 表内（grep 实证）；`maintenance-oee.value.spec` OEE null 语义与种子状态日志行正交（见 ②）。
   - ⑤**面 1**（域模块 `_cases`）：fixture-only 不声明 `init-database-data`，种子不进装载路径（M1.1x/M1.2a1/M1.2a2 同口径）。
   - **预判漂移用例清单 = 空**（Phase 3 实测收敛见 Phase 3 执行证据）。
4. **执行期发现（pre-existing ORM quirk + CSV-only 规避裁决）**：`ErpQaCalibration.targetValue/tolerance` 在运行时 ORM（`module-quality/erp-qa-dao/.../_app.orm.xml`，源 `model/app-erp-quality.orm.xml` domain="measuredValue" 覆写）为 `stdDataType=decimal + stdSqlType=VARCHAR`——该实体既往零 seed 零读路径，从未暴露；本批 seed 装载后 findAll 物化经 `orm_internalSet` 硬转换抛 ClassCastException（首跑门禁拦截实证）。**CSV-only 规避裁决（不触 Non-Goal 保护区）**：两列均 nullable，种子行留空 → null 物化零转换，28 CSV 全量覆盖维持；模型修正（stdSqlType=DECIMAL）登记 `Deferred But Adjudicated` 归 successor（ORM 修正须 codegen 重生成，超出本计划纯资源边界）。28 表全量扫描证实该 decimal/VARCHAR 例外仅此 2 列（`string/BIGINT` id 列模式为全仓常规，非本类问题）。

Exit Criteria:

- [x] 28 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [x] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内
- [x] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/maintenance/seed-data.md`、`docs/design/quality/seed-data.md`、`docs/design/projects/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「133 + 已落地批次 CSV 数」为准本批 +28，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +28，精确缺 seed −28）+ 新增 M1.2b 批次增量行 + L297 备件消耗行 Deferred 消费注记 + 快照重录义务节资产计数注记（M1.2b 批次后 CSV 总数随批推进）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/maintenance/seed-data.md` + `docs/design/quality/seed-data.md` + `docs/design/projects/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）
      - Skill: `nop-backend-dev`

**执行证据（Phase 2）**：常量 190→**218**（javadoc 批次沿革追加 M1.2b 行 + 类 javadoc 「M1.2b 起」同步）；`EXPECTED_PLATFORM_CSV_COUNT=4` / `EXPECTED_APP_ERP_ENTITY_COUNT=363` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES` 未触碰。seed-data.md 五处同步——对账表有 seed 190→218 / 精确缺 178→150（复算注记含规格表九节消费完毕）、新增 M1.2b 批次增量行（含零漂移双裁决 + qa_calibration 留空规避登记）、L297 两条 Deferred 消费注销记（calibration 族 + 备件消耗行，沿 SPC 先例删除线格式）、快照重录义务节资产计数（M1.2b 批次后 = 222 CSV + 1 SQL）、门禁强化段批次链推进至 218；3 个域 seed-data.md owner doc 落地（mnt 7 表 18 行 / qa 10 表 28 行 / prj 11 表 26 行，各含 FK 闭环图 + 零漂移设计裁决节 + 衔接约束 + negative 行语义 + 约定对齐）。

Exit Criteria:

- [x] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [x] 3 个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移（预期高风险：C09 qa 面 / C12 prj 面），按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏 → 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行三域相关视觉 spec（`dashboards.visual` + `dashboards.snapshot`（含 mnt/qa/prj 看板）/ `reports.visual` + `reports.snapshot` / `maintenance-visit-wizard`（visit 链不在扩展面，预期零漂移），实仓 `tests/e2e/visual/` 清单复核后如有额外三域相关 spec 一并纳入）；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：检查本批 28 表是否被既有 `*.value.spec.ts` / `*.list-value.spec.ts` / 看板 KPI 期望值消费（quality.value.spec 读 SPC/NCR 面——不在扩展面，以 Phase 1 预分析为准）；有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 6 张本批表（mnt/qa/prj 各 ≥ 2，含主子表头）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议；known-good-baselines 1542 为历史实测引用）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.2b `ready` → `done`（含批次证据摘要，格式沿 M1.1 批先例）
      - Skill: none

**执行证据（Phase 3，2026-09-01 实测）**：

1. `TestErpSeedDataIntegrity` **4/4 全绿**（loadable / nonNullRelation 零悬空白名单零增量 / scopePinned / seedAssetInventory 常量 218+4 断言通过）。
2. `mvn test -pl app-erp-all` **71/0/0/1 = M0.2/M1.1x/M1.2a1/M1.2a2 基线精确一致**，BUILD SUCCESS——C09（qa 面）/C10（mnt 面）/C11/C12（prj 面）全绿零重录，与 Phase 1 预分析「零漂移预期」及两条种子侧裁决（结算 INTERIM/CANCELLED + 账单全批 CANCELLED）逐条对账成立；快照重录义务**未触发**（未触发声明见下节）。
3. 视觉双面：`dashboards.visual` + `dashboards.snapshot`（含 mnt/qa/prj 看板）+ `reports.visual` + `reports.snapshot` + `maintenance-visit-wizard.visual` **50 passed / 1 skipped（skip 为基线内预存 skip 项）零漂移**——DOM 与像素双面均未触发重录。
4. value spec 联动评估：`maintenance.value`（KPI 4 值）+ `maintenance-oee.value`（OEE null 语义 2 用例）+ `quality.value` + `qa-dashboard-spc-attributes.value` + `projects.value`（KPI 4 值 + 毛利 5 值）+ 三域 smoke **14/14 全绿零联动**——本批 28 表零进入任何期望值断言面（与预分析 ④ 一致）。
5. fresh-DB 运行时装载证明：`./scripts/start-app.sh restart` 12s ready、装载零冲突零列映射错误（app.log 复核）；GraphQL `findPage` 抽样 **18 张本批表**（mnt 7 + qa 7 + prj 4，含全部 6 主子表头）行数 == CSV 行数 **18/18 全对**（临时 Proof spec 实测后移除）。
6. compliance checker **exit 0**：R2c=1542（机器块 1537 + 已登记 pre-existing +5）/ R2b=242（240+2）/ R12a=71（70+1），其余规则与机器块逐值一致（R1d=14 / R2a=34 / R2d=38 / R3=5 / R6=2 / R10=14 / R12b=66 / R12c=42）——**零新增漂移**（本批纯资源 + 测试常量 + docs，无生产代码变更）。
7. 行政收尾：日志条目 `docs/logs/2026/09-01.md` 追加在案；roadmap M1.2b 回写 `done`（独立结束审计 APPROVE 后执行）。

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa4b4240fffepEG6l7497He1Ry`）——0 Blocker + 0 Major；2 Minor（①基线登记滞后注记——known-good-baselines 最新行 69/0/0/1 滞后于 M0.2 后实测 71/0/0/1，基线追行归 M3.1（M1.1a 先例措辞）；②FK 白名单例举补 `ErpMdPartner`（规格表 ErpPrjBilling 行要求））——①②均已并入本稿。审查者实仓核验：137 CSV、三域已 seed 8/6/6 与缺 seed 7/10/11 清单 1:1（含 16 独立表 + 6 主子对拆分 3+6+7）、6 个 FK 锚点 CSV 在位、L297 Deferred 登记（UO_M_ID 陷阱）逐字核实、C09/C12 测试类在位且三域看板读取面全部在批外、maintenance-visit-wizard spec 在位、3 个 owner doc 目标文件确证为新建、命名/状态/规则 1-14 合规。

## 快照重录合规声明

**实测未触发双面重录**（沿 M1.2a2 批六点核查格式，2026-09-01）：

1. **变更面**：28 个 seed CSV 纯新增（`_init-data/` 190→218）+ `TestErpSeedDataIntegrity` 常量 190→218 + docs——零既有 CSV 行修改、零生产代码变更。
2. **面 2（`app-erp-all/_cases` 集成快照）**：零重录零改动——快照机制 `_chgType` 增量记录（A/U/D），种子纯加性插入不入既有快照、显式 id（<100000）不消耗 default 序列（C10 自建行 id 实测 100002+/100011+ 佐证）；预分析标记的高风险面 C09/C12 经种子侧裁决（结算 INTERIM/CANCELLED 守卫零触发 + 账单全批 CANCELLED 保持 `findBillings` 空 → C12 结算行序零移位）实测零漂移，`mvn test -pl app-erp-all` 71/0/0/1 基线精确一致。
3. **面 1（各域 `module-<domain>/_cases`）**：零影响（域模块测试不声明 `nop.orm.init-database-data`，种子不进其装载路径）——M1.1a/M1.1b/M1.1c/M1.2a1/M1.2a2 同口径，零重录。
4. **视觉双面义务**：`dashboards.visual` + `dashboards.snapshot`（含 mnt/qa/prj 看板）+ `reports.visual` + `reports.snapshot` + `maintenance-visit-wizard.visual` 实测 50 passed / 1 skipped（基线内预存 skip）**零漂移**——DOM 与像素双面均未触发重录（看板/报表读取面只读既有表，本批 28 表零消费；OEE 卡片 null 语义与状态日志种子行正交），双面均为实测通过态，无单面重录情形。
5. **E2E 数值断言联动**：maintenance/quality/projects/maintenance-oee/qa-spc-attributes value spec + 三域 smoke 14/14 全绿零联动——本批 28 表零进入任何期望值断言面。
6. **提交说明登记**：本批 seed 变更 → 双面重录范围 = 空（面 2 零用例重录、面 1 零影响、视觉双面零漂移、value spec 零联动）。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（28 CSV + 常量 + 对账表 + 3 个 owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/快照重录注记/L297 消费注记 + 3 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- **seed-data.md L297「备件消耗行 seed」Deferred**：本计划 Phase 1 全量覆盖口径消费（`erp_mnt_spare_part_usage_line` 落地）；消费注记由 Phase 2 回写 seed-data.md，无需独立 successor。
  - Classification: `consumed by this plan`
  - Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务。
  - Successor Required: `no`
- **`ErpQaCalibration.targetValue`/`tolerance` 模型列 stdSqlType 预存问题**：运行时 ORM 两列为 `stdDataType=decimal + stdSqlType=VARCHAR`（源 `module-quality/model/app-erp-quality.orm.xml` `domain="measuredValue"` 覆写所致；28 表全量扫描唯一命中，其余 `string/BIGINT` id 列模式为全仓常规）。该实体既往零 seed 零读路径从未暴露；本批 seed 后 findAll 物化 `orm_internalSet` 硬转换抛 ClassCastException（Phase 1 门禁首跑拦截实证）。本计划 Non-Goal 禁改 ORM（保护区），CSV-only 规避 = 种子行两列留空（nullable，null 物化零转换），28 CSV 全量覆盖维持。
  - Classification: `blocked by pre-existing model issue`（非本计划引入；修正须改 `module-quality/model/app-erp-quality.orm.xml` 两列 `stdSqlType="DECIMAL"` + `mvn clean install -DskipTests` 增量重生成 + 补齐种子行数值——跨保护区边界，独立工作项）
  - Why Not Blocking Closure: 本计划以 CSV-only 路径达成规格表全量覆盖（28/28 实体有 seed、行数/编码合规）；模型列缺陷为 pre-existing 且已规避，不阻塞 seed 覆盖目标。
  - Successor Required: `yes`（M1.x 后续或技术债工作项：修 ORM 列型 → 重生成 → seed 行补值；登记处 = 本节 + seed-data.md Phase 2 注记）

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: 计划可闭包——28 seed CSV（72 行 = mnt 18 + qa 28 + prj 26）全部落地（190→222 CSV，maintenance/quality/projects 三域 15/16/17 全覆盖），门禁常量 190→218 与 seed-data.md 对账表（有 seed 218 / 精确缺 150）及 `_init-data/` 实仓构成（222 CSV = 218 app.erp + 4 平台 + 1 SQL）一致；3 个域 seed-data.md owner doc 段落地。干扰面零漂移双裁决（结算 INTERIM/CANCELLED + 账单全批 CANCELLED）及状态日志/归集行惰性设计经源码核对与实测双重背书（C09/C10/C11/C12 全绿零重录）。执行期发现 1 项 pre-existing ORM 列型 quirk（ErpQaCalibration.targetValue/tolerance decimal/VARCHAR）经 CSV-only 留空规避，28/28 全量覆盖维持，模型修正登记 Deferred But Adjudicated 归 successor。验证终态：`TestErpSeedDataIntegrity` 4/4 全绿、`mvn test -pl app-erp-all` 71/0/0/1 = 基线精确一致（快照重录双面义务未触发，未触发声明六点在案）、fresh-DB 装载 18/18 抽样表 findPage==CSV 行数、视觉双面 dashboards/reports/visit-wizard 50 passed 零漂移 + 三域 value/smoke 14/14 零联动、compliance checker exit 0 R2c=1542 零新增漂移。审计 Minor 2 项（日志条目 / 义务规则 1 计数链）均已落实。roadmap M1.2b 已回写 `done`。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话）
- Evidence: task `ses_fa2a5d560ffeOs8lxphg7SFCkR`——VERDICT **APPROVE**（0 Blocker / 0 Major / 2 Minor）。审计 A..J 十项全 pass 实仓核验：①28 CSV 逐文件行数/列约定/静态日期/ID<100000 与 owner doc 1:1（72 行分布 18/28/26 复算一致）；②FK 抽查全锚点（设备/使用/检验/项目/预算/归集/PNL/NCR/员工/往来/物料/UoM/科目 32）闭环零悬空；③17 个字典值域逐值合法；④零漂移双裁决在数据中实证（billing 全 CANCELLED / settlement INTERIM-DRAFT+FINAL-CANCELLED / 归集行无来源列 / qa_calibration 两列留空含行内注记）；⑤门禁常量 218 + 实仓 222 CSV；⑥对账表 218/150/222 计数链自洽；⑦3 个 owner doc 行数与实仓逐一相等；⑧验证两项独立复跑全复现（gate 4/4 + compliance exit 0 R2c=1542/R2b=242/R12a=71）；⑨plan 零 `- [ ]` 残留、3 Phase completed、未触发声明六点、Deferred 2 条目核验；⑩roadmap M1.2b 审计时点仍为 `ready`（回写协议时序正确）。Minor ①日志条目缺失（审计时点预期态）+ ②义务规则 1 计数链缺 M1.2b 项——均已在闭包收尾落实。

Follow-up:

- （无非阻塞跟进项；ErpQaCalibration 列型修正已登记 Deferred But Adjudicated 为 successor-required 项，非本计划 Follow-up 范畴）
