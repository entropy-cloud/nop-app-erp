# 2026-09-01-1245-3 M1.2b 核心业务域 seed 扩面 B——maintenance + quality + projects（28 CSV）

> Plan Status: active
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

Status: planned
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表全部必填 FK 锚点均已 seed）

- [ ] 逐实体核对 ORM 表名与列（`module-maintenance` / `module-quality` / `module-projects` 三域 `model/*.orm.xml` tableName / `code=`）后，按规格表 maintenance / quality / projects 三节（28 行，逐行引用不复制）创建 28 个 CSV；主子表组：mnt（maintenance_team + member / task_template + line）、qa（inspection_template + line / recall + target）、prj（billing + line / project_settlement + line）；独立表按规格表（mnt 3 + qa 6 + prj 7）；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [ ] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpMntEquipment / ErpMntSparePartUsage / ErpQaInspection / ErpPrjProject / ErpPrjBudget / ErpPrjCostCollection / ErpMdEmployee / ErpMdPartner / ErpMdMaterial / ErpMdUoM）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [ ] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期、UoM 列 `UO_M_ID`（`erp_mnt_spare_part_usage_line` 显式中陷阱点）；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_mnt_spare_part_usage.csv` / `erp_prj_budget.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c 先例）
      - Skill: `nop-backend-dev`
- [ ] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——grep `app-erp-all/_cases` 集成用例源码中本批 28 表引用面（重点 C09 qa NCR/capa/scrap 面、C12 prj timesheet/settlement 面）、三域看板/报表 BizModel 读取面（quality SPC 三计数器读 spc_* + non_conformance——不在本批扩展面；mnt 看板读 equipment/visit/request/schedule——不在扩展面；prj 看板读 project/cost_collection/project_pnl——不在扩展面，均须 grep 实证）、三域相关 `*.value.spec.ts` 断言面，判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 28 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [ ] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内
- [ ] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/maintenance/seed-data.md`、`docs/design/quality/seed-data.md`、`docs/design/projects/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [ ] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「133 + 已落地批次 CSV 数」为准本批 +28，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [ ] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +28，精确缺 seed −28）+ 新增 M1.2b 批次增量行 + L297 备件消耗行 Deferred 消费注记 + 快照重录义务节资产计数注记（M1.2b 批次后 CSV 总数随批推进）
      - Skill: `nop-backend-dev`
- [ ] 新建 `docs/design/maintenance/seed-data.md` + `docs/design/quality/seed-data.md` + `docs/design/projects/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [ ] 3 个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: planned
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [ ] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [ ] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移（预期高风险：C09 qa 面 / C12 prj 面），按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏 → 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [ ] 视觉快照双面义务核查：运行三域相关视觉 spec（`dashboards.visual` + `dashboards.snapshot`（含 mnt/qa/prj 看板）/ `reports.visual` + `reports.snapshot` / `maintenance-visit-wizard`（visit 链不在扩展面，预期零漂移），实仓 `tests/e2e/visual/` 清单复核后如有额外三域相关 spec 一并纳入）；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [ ] E2E 数值断言联动评估：检查本批 28 表是否被既有 `*.value.spec.ts` / `*.list-value.spec.ts` / 看板 KPI 期望值消费（quality.value.spec 读 SPC/NCR 面——不在扩展面，以 Phase 1 预分析为准）；有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [ ] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 6 张本批表（mnt/qa/prj 各 ≥ 2，含主子表头）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [ ] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议；known-good-baselines 1542 为历史实测引用）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [ ] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [ ] 独立结束审计通过后回写 roadmap 工作项 M1.2b `ready` → `done`（含批次证据摘要，格式沿 M1.1 批先例）
      - Skill: none

Exit Criteria:

- [ ] TestErpSeedDataIntegrity 全绿且常量断言通过
- [ ] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [ ] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [ ] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa4b4240fffepEG6l7497He1Ry`）——0 Blocker + 0 Major；2 Minor（①基线登记滞后注记——known-good-baselines 最新行 69/0/0/1 滞后于 M0.2 后实测 71/0/0/1，基线追行归 M3.1（M1.1a 先例措辞）；②FK 白名单例举补 `ErpMdPartner`（规格表 ErpPrjBilling 行要求））——①②均已并入本稿。审查者实仓核验：137 CSV、三域已 seed 8/6/6 与缺 seed 7/10/11 清单 1:1（含 16 独立表 + 6 主子对拆分 3+6+7）、6 个 FK 锚点 CSV 在位、L297 Deferred 登记（UO_M_ID 陷阱）逐字核实、C09/C12 测试类在位且三域看板读取面全部在批外、maintenance-visit-wizard spec 在位、3 个 owner doc 目标文件确证为新建、命名/状态/规则 1-14 合规。

## 快照重录合规声明

**（占位——执行期按实况填写：触发双面重录时在此追加声明段；实测未触发则改为未触发声明，沿 M1.1 批六点核查格式）**

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [ ] 范围内行为完成（28 CSV + 常量 + 对账表 + 3 个 owner doc 段）
- [ ] 相关文档对齐（seed-data.md 对账表/快照重录注记/L297 消费注记 + 3 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [ ] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

- **seed-data.md L297「备件消耗行 seed」Deferred**：本计划 Phase 1 全量覆盖口径消费（`erp_mnt_spare_part_usage_line` 落地）；消费注记由 Phase 2 回写 seed-data.md，无需独立 successor。
  - Classification: `consumed by this plan`
  - Why Not Blocking Closure: 非阻塞项——为消费登记而非遗留债务。
  - Successor Required: `no`

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: <待闭包填写>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理（新会话）>
- Evidence: <task id / 复核记录>

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
