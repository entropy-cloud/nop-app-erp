# 2026-09-01-1245-2 M1.2a2 核心业务域 seed 扩面 A2——manufacturing（26 CSV）

> Plan Status: active
> Last Reviewed: 2026-09-01
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.2a2（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、同批 `2026-09-01-1245-1-m12a1-finance-seed-expansion.md`（N=1）/ `2026-09-01-1245-3-m12b-mnt-qa-prj-seed-expansion.md`（N=3）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径），133 个有 seed（`app-erp-all/src/main/resources/_vfs/_init-data/` 实测 137 CSV = 133 app.erp + 4 平台，+ 1 SQL `zz-sequence-advance.sql`）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 133` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，非本域不触碰）。
- manufacturing 实测已 seed 8 表：`erp_mfg_work_order` / `erp_mfg_workcenter` / `erp_mfg_workcenter_calendar` / `erp_mfg_workcenter_capacity` / `erp_mfg_crp_load` / `erp_mfg_forecast` / `erp_mfg_forecast_line` / `erp_mfg_cost_variance`；缺 seed 26（与 roadmap M1.2a2 清单一致，逐实体规格见 `docs/architecture/seed-data.md` 规格表 manufacturing 节，本计划逐行引用不复制）。
- 既有 FK 锚点已就绪：`ErpMfgWorkOrder`（WO-2026-001，1234-1/0930-1）、工作中心配置链（0628-1）、`ErpInvBatch`（M1.1b 批次已落地——规格表 `ErpMfgBatchGenealogy` 行「跨域:inv·本批」现指 M1.1b 已 seed 集）、跨域主数据（md material/uom/warehouse/employee/partner/currency）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 plan `2026-09-01-0527-1` 实测，经 M1.1a/M1.1b/M1.1c 三批维持；`known-good-baselines.md` 最新登记行 69/0/0/1 系 2026-08-31 plan-1426-2 口径，登记行滞后于 M0.2 后实测，基线追行归 M3.1）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542** 为历史实测值（+5 pre-existing 增量已登记，沿 plan `2026-09-01-0527-1` 口径协议）；全 reactor 已知 2 处预存回归（hr/drp）为 roadmap Non-Goal，与本计划无关。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；UoM 外键列名为 `UO_M_ID`（分叉拼写陷阱，M1.1c 闭包实证 + seed-data.md L297 登记）；装载拓扑序由 `DataInitInitializer` 自动排序。
- 冻结时钟纪律（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期列使用静态固定值，禁止 `now()`/滚动期间语义（工单/工序/班次日志类实体尤其注意）。
- **干扰面差异 vs M1.1 批**（本计划核心风险）：C08 `TestErpC08MrpApsRelease` 集成用例读 mfg BOM/MRP 面（known-good-baselines 2026-08-31 行 F2.6 历史：BOM/MRP 快照 NET_REQUIREMENT/PLANNED_QUANTITY 曾漂移）；本批新增 `erp_mfg_bom*` / `erp_mfg_mrp_*` / `erp_mfg_work_order_line` 等表**可能**改变 C08 的 MRP 计算输入与面 2 快照；mfg 看板/报表（crp-load / 生产差异 / 预测差异）读取面以预分析为准。须预分析 + 按协议重录。

## Goals

- 26 个 seed CSV 落地（逐行消费规格表 manufacturing 节），每实体最小可用数据集（行数 ≤ 20，按规格表建议行数与用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空，工单/BOM/工艺/MRP/cost_rollup/委外/批次追溯链路完整。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（基数以「133 + 已落地批次 CSV 数」为准，本批 +26），seed-data.md 对账表同步。
- `docs/design/manufacturing/seed-data.md`「种子数据」owner doc 段落地。

## Non-Goals

- 不修改任何 ORM 模型（`module-*/model/*.orm.xml`，保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不触碰 MRP/BOM 计算引擎与 Processor 逻辑（seed 为被动数据）。
- 不覆盖 finance / maintenance / quality / projects 及其余 M1.x 工作项的缺 seed 实体（归同批 N=1/N=3 与后续批次）；不触碰 finance 5 个缺 className 运行时实体（M1.2a1 口径）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不引入 production-grade 真实个人数据。
- 不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 manufacturing 节 + 对账表）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.2a2 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）
- Skill Selection Basis: roadmap M1.2a2 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定，技能路由到平台模型文档）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批三计划（N=1/2/3）之二；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，若 N=1/N=3 先落地，常量基数以「133 + 已落地批次 CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（26 CSV）

Status: planned
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表全部必填 FK 锚点均已 seed）

- [ ] 逐实体核对 ORM 表名与列（`module-manufacturing/model/app-erp-manufacturing.orm.xml` tableName / `code=`）后，按规格表 manufacturing 节（26 行，逐行引用不复制）创建 26 个 CSV，覆盖：工单扩展族（work_order_line / job_card + time_log / material_issue + line / batch_genealogy / 3 张 work_order_bom*_snapshot）、BOM 族（bom / bom_line / bom_byproduct / bom_operation）、工艺族（routing / routing_operation）、生产版本（production_version → bom + routing）、MRP 族（mrp_scenario + param + version / mrp_plan + line / mrp_demand）、成本族（cost_rollup + line）、委外族（subcontract_order + line）；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [ ] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpMfgWorkOrder / ErpMfgWorkcenter 链 / ErpInvBatch / ErpMdMaterial / ErpMdUoM / ErpMdWarehouse / ErpMdEmployee / ErpMdPartner / ErpMdCurrency）或〔本批〕新增行（含 bom_operation → routing_operation〔本批〕跨族边）；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [ ] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期、UoM 列 `UO_M_ID`；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_mfg_work_order.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c 先例）
      - Skill: `nop-backend-dev`
- [ ] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——grep `app-erp-all/_cases` 集成用例源码中本批 26 表引用面（重点 C08 MrpApsRelease 的 BOM/MRP 输入面）、mfg 看板/报表 BizModel 读取面（crp-load 读 workcenter 链 + crp_load、生产差异读 cost_variance、预测差异读 forecast——均不在本批扩展面）、mfg 相关 `*.value.spec.ts` 断言面，判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 26 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [ ] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内
- [ ] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: planned
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/manufacturing/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [ ] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「133 + 已落地批次 CSV 数」为准本批 +26，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [ ] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +26，精确缺 seed −26）+ 新增 M1.2a2 批次增量行 + 快照重录义务节资产计数注记（M1.2a2 批次后 CSV 总数随批推进）
      - Skill: `nop-backend-dev`
- [ ] 新建 `docs/design/manufacturing/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注，含 batch_genealogy → ErpInvBatch 跨域边）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [ ] manufacturing seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: planned
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [ ] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [ ] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移（预期高风险：C08 MRP 计算输入面），按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏（如 MRP 释放语义被新 BOM/需求行改变）→ 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [ ] 视觉快照双面义务核查：运行 mfg 相关视觉 spec（`dashboards.visual` + `dashboards.snapshot`（含 mfg 看板）/ `reports.visual` + `reports.snapshot`，实仓 `tests/e2e/visual/` 清单复核后如有额外 mfg 相关 spec 一并纳入）；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [ ] E2E 数值断言联动评估：检查本批 26 表是否被既有 `*.value.spec.ts` / `*.list-value.spec.ts` / 看板 KPI 期望值消费（mfg-crp-load.value 等以 Phase 1 预分析为准）；有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [ ] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 5 张本批表（至少 1 张 MRP 族主子表头 + 1 张 BOM 族表 + 1 张快照表）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [ ] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议；known-good-baselines 1542 为历史实测引用）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [ ] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [ ] 独立结束审计通过后回写 roadmap 工作项 M1.2a2 `ready` → `done`（含批次证据摘要，格式沿 M1.1 批先例）
      - Skill: none

Exit Criteria:

- [ ] TestErpSeedDataIntegrity 全绿且常量断言通过
- [ ] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [ ] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [ ] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa4b45335ffeb00kE4tIT1rsDc`）——0 Blocker + 0 Major；3 Minor（①compliance 锚点口径沿 `2026-09-01-0527-1` 协议明示（§BASELINE 机器块 1537 + pre-existing 增量登记，1542 为历史实测引用）；②Phase 3 行政项（日志/roadmap 回写）类型纯度——阶段头改 `Proof`（6）+ `Add`（2 行政收尾）；③视觉 spec 清单去 hedge 落实名）——①②③均已并入本稿。审查者实仓核验：137 CSV、mfg 8 表已 seed、26 规格表实体 1:1（9+4+2+1+6+2+2）、`ErpMfgBomOperation.operationId` mandatory → `ErpMfgRoutingOperation`（orm.xml:291-331）、`ErpInvBatch` 已 seed（M1.1b）、C08 保存 BOM/BomLine/MrpPlan/MrpDemand/MrpScenario（干扰面预分析必要性成立）、看板/报表读取面在批外、71/0/0/1 + R2c 溯源 0527-1、Related 链接全部可达。

## 快照重录合规声明

**（占位——执行期按实况填写：触发双面重录时在此追加声明段；实测未触发则改为未触发声明，沿 M1.1 批六点核查格式）**

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [ ] 范围内行为完成（26 CSV + 常量 + 对账表 + manufacturing owner doc 段）
- [ ] 相关文档对齐（seed-data.md 对账表/快照重录注记 + manufacturing seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [ ] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

（无——M0.1 规格 CSV-only 可满足性核验已确认本批 26 实体零 ORM 依赖，规格表全部必填 FK 锚点均已 seed；触发条件未发生。执行期若证伪按反松弛规则移入本节分类登记）

## Closure

Status Note: <待闭包填写>

Closure Audit Evidence:

- Auditor / Agent: <独立子代理（新会话）>
- Evidence: <task id / 复核记录>

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
