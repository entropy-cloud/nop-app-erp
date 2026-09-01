# 2026-09-01-1245-2 M1.2a2 核心业务域 seed 扩面 A2——manufacturing（26 CSV）

> Plan Status: completed
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

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表全部必填 FK 锚点均已 seed）

- [x] 逐实体核对 ORM 表名与列（`module-manufacturing/model/app-erp-manufacturing.orm.xml` tableName / `code=`）后，按规格表 manufacturing 节（26 行，逐行引用不复制）创建 26 个 CSV，覆盖：工单扩展族（work_order_line / job_card + time_log / material_issue + line / batch_genealogy / 3 张 work_order_bom*_snapshot）、BOM 族（bom / bom_line / bom_byproduct / bom_operation）、工艺族（routing / routing_operation）、生产版本（production_version → bom + routing）、MRP 族（mrp_scenario + param + version / mrp_plan + line / mrp_demand）、成本族（cost_rollup + line）、委外族（subcontract_order + line）；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpMfgWorkOrder / ErpMfgWorkcenter 链 / ErpInvBatch / ErpMdMaterial / ErpMdUoM / ErpMdWarehouse / ErpMdEmployee / ErpMdPartner / ErpMdCurrency）或〔本批〕新增行（含 bom_operation → routing_operation〔本批〕跨族边）；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期、UoM 列 `UO_M_ID`；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_mfg_work_order.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c 先例）
      - Skill: `nop-backend-dev`
- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——grep `app-erp-all/_cases` 集成用例源码中本批 26 表引用面（重点 C08 MrpApsRelease 的 BOM/MRP 输入面）、mfg 看板/报表 BizModel 读取面（crp-load 读 workcenter 链 + crp_load、生产差异读 cost_variance、预测差异读 forecast——均不在本批扩展面）、mfg 相关 `*.value.spec.ts` 断言面，判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

**执行证据（Phase 1）**：

- **干扰面预分析结论（零漂移预期 + 两条种子侧设计裁决）**：①C08 `runSimulation` 经 `BomExpander.findDefaultBomOrNull`（过滤 `isDefault=true AND isActive=true`，`SimulationMrpEngine` :344）判定自制品/采购件并 BOM 展开（:372），`loadDemands(planId)`（:134）按 planId 取需求 → 本批全部 `erp_mfg_bom` 行 **`IS_DEFAULT=false`** + 种子 MRP 链自封闭 → C08 运算输入面零变化；②mfg-chain E2E 依赖「MAT-001 无 FIRMED rollup → calculateVariances 抛 ERR_VARIANCE_NO_STANDARD_COST 被吞」（`ProductionVarianceCalculator.findFirmedRollupLine` :117）→ 本批 cost_rollup 全部 status ∈ {DRAFT, CALCULATED, CANCELLED} **零 FIRMED**；③看板/报表 BizModel（dashboard/report/crp 包）只读既有 8 表（work_order/cost_variance/forecast(+line)/crp_load/workcenter 链）零扩展面消费；④`ErpMfgSkuReferenceChecker` 按 skuId 查询（种子行留空）、KitAvailabilityChecker 按工作单 ID 定位、CrpLoadCalculator 按 routingId（种子工单无 routingId）——全 ID/过滤作用域无全表扫描；⑤快照机制 `_chgType` 增量记录（M1.2a1 实证），纯加性插入不入既有快照。**预判漂移用例清单 = 空**（Phase 3 实测收敛：71/0/0/1 零漂移）。
- **26 CSV 落地（63 行）**：batch_genealogy 1 / bom 2（P+N-DIS）/ bom_byproduct 1 / bom_line 3 / bom_operation 3 / cost_rollup 3（P×2+N-TERM）/ cost_rollup_line 4 / job_card 3（P×2+N-TERM）/ job_card_time_log 2 / material_issue 3（P×2+N-TERM）/ material_issue_line 2 / mrp_demand 2 / mrp_plan 3（P×2+N-TERM）/ mrp_plan_line 4 / mrp_scenario 2（P+N-TERM）/ mrp_scenario_param 2 / mrp_scenario_version 2（P+N-TERM）/ production_version 2（P+N-DIS）/ routing 2（P+N-DIS）/ routing_operation 3 / subcontract_order 3（P×2+N-TERM）/ subcontract_order_line 3 / work_order_bom_line_snapshot 2 / work_order_bom_operation_snapshot 2 / work_order_bom_snapshot 1 / work_order_line 3——行数全部 ≤ 20 且符合规格表建议区间。
- **程序化核验**：26/26 表头列 ∈ ORM `code=` 且保持 propId 序（REMARK 位置 2 处按 propId 修正）；必填列无缺失无空值；ID < 100000；120 条 to-one 边 FK 闭环零悬空（脚本核对〔已seed〕∪〔本批〕）；字典码 ∈ `erp-mfg/*`（含 `ErpMfgConstants` 侧 simulation-status DRAFT/RUNNING/COMPLETED/ARCHIVED 与 simulation-param-type LEAD_TIME/LOT_SIZE/SAFETY_STOCK）+ `wf/approve-status`（APPROVED）+ `erp-md/posted-status`（DRAFT/POSTED）。
- **静态日期一致性**：冻结时钟纪律下 MI-2026-002 业务日期取 2026-07-03（对齐批次 1 LOT-20260703-001 入库 2026-07-01 之后）；基因链批次入库日期与 WO-2026-001 完工日的静态张力登记 documented simplification（owner doc 衔接节 + roadmap Non-Goal 逐字段回放豁免口径）。

Exit Criteria:

- [x] 26 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [x] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内
- [x] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/manufacturing/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「133 + 已落地批次 CSV 数」为准本批 +26，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +26，精确缺 seed −26）+ 新增 M1.2a2 批次增量行 + 快照重录义务节资产计数注记（M1.2a2 批次后 CSV 总数随批推进）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/manufacturing/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注，含 batch_genealogy → ErpInvBatch 跨域边）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）
      - Skill: `nop-backend-dev`

**执行证据（Phase 2）**：常量 164→**190**（javadoc 批次沿革追加 M1.2a2 行，类 javadoc 同步）；seed-data.md 五处同步——对账表有 seed 164→190 / 精确缺 204→178（复算注记含规格表六节消费完毕）、新增 M1.2a2 批次增量行（含零默认 BOM + 零 FIRMED 滚算零漂移设计登记）、运行时口径补充档注记收敛（运行时缺 = 精确缺 = 178）、快照重录义务节两处资产计数（M1.2a2 批次后 = 194 CSV + 1 SQL）、门禁强化段批次链推进至 190；`docs/design/manufacturing/seed-data.md` 落地（26 表清单 + 63 行分布 / FK 闭环图 120 边含 batch_genealogy→ErpInvBatch 与 bom_operation→routing_operation 跨族边 / 干扰面零漂移设计裁决节 / 与既有 8 表衔接约束 / N-TERM·N-DIS 行语义 / 约定对齐）。

Exit Criteria:

- [x] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [x] manufacturing seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移（预期高风险：C08 MRP 计算输入面），按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏（如 MRP 释放语义被新 BOM/需求行改变）→ 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行 mfg 相关视觉 spec（`dashboards.visual` + `dashboards.snapshot`（含 mfg 看板）/ `reports.visual` + `reports.snapshot`，实仓 `tests/e2e/visual/` 清单复核后如有额外 mfg 相关 spec 一并纳入）；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：检查本批 26 表是否被既有 `*.value.spec.ts` / `*.list-value.spec.ts` / 看板 KPI 期望值消费（mfg-crp-load.value 等以 Phase 1 预分析为准）；有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 5 张本批表（至少 1 张 MRP 族主子表头 + 1 张 BOM 族表 + 1 张快照表）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议；known-good-baselines 1542 为历史实测引用）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.2a2 `ready` → `done`（含批次证据摘要，格式沿 M1.1 批先例）
      - Skill: none

**执行证据（Phase 3）**：

1. `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` **4/4 全绿**（首跑通过零拦截——Phase 1 程序化预核验先行的 M1.2a1 同口径效果；实跑前作者侧曾以 ORM `code=` 对照脚本预拦截并修正 3 处字段错位 + 2 处 REMARK propId 序）。
2. `mvn test -pl app-erp-all` **71/0/0/1 = M0.2/M1.1x/M1.2a1 基线精确一致**——C07（mfg 工单生命周期）与 C08（MRP→APS 释放）全绿零重录，与预分析「零漂移预期」逐条对账成立（零默认 BOM + 零 FIRMED 滚算两裁决生效）；快照重录义务**未触发**。
3. 视觉双面：`dashboards.visual` + `dashboards.snapshot` + `reports.visual` + `reports.snapshot` **50/50 全绿零漂移**（含 mfg 看板 DOM/像素与生产差异/预测差异/CRP 负荷报表基线）——像素与 DOM 均未触发重录；另实跑 mfg 相关 value/smoke spec（manufacturing.value/smoke + mfg-crp-load / mfg-production-variance / mfg-forecast-variance value/smoke）**8/8 全绿**。
4. E2E 数值断言联动评估：本批 26 表零消费（看板 KPI 与报表 value 断言只读既有 8 表，8/8 value spec 实测全绿零期望值改动）→ 零联动登记。
5. 运行时装载证明：`./scripts/start-app.sh restart`（fresh-DB 重置）后 `/r/{Entity}__findPage` 抽样 **26/26 表 total == CSV 行数**（2/3/1/3/3/3/3/3/3/2/2/2/3/4/2/2/2/2/2/4/3/3/1/1/2/2，含 MRP 族主子表头 ErpMfgMrpPlan=3、BOM 族 ErpMfgBom=2、快照表 3 张）——真实装载 0 冲突 / 0 列映射错误，超出 ≥5 张要求。
6. `bash docs/audits/nop-compliance-checker.sh` **exit 0**：R2c=1542（机器块 1537 + 已登记 pre-existing +5）/ R2b=242（240+2）/ R12a=71（70+1），其余规则与机器块逐值一致（R1d=14 / R2a=34 / R2d=38 / R3=5 / R6=2 / R10=14 / R12b=66 / R12c=42）——**零新增漂移**（本批纯资源 + 测试常量 + docs，无生产代码变更）。
7. 执行期发现并登记 1 项**预存失败**（与本批无关，移除本批种子重建 jar 后 fresh-DB 对照实测复现同型失败）：`tests/e2e/orchestration/mfg-subcontract-chain.spec.ts` 2 用例（红冲凭证 1405 科目借方 -60 vs spec 期望 -50）——对照实验记录与溯源线索落 `docs/bugs/2026-09-01-2115-mfg-subcontract-chain-preexisting-reversal-voucher-drift.md`，归 successor（同 hr/drp 预存回归家族待遇，本批 Non-Goal 不处置）。
8. 行政收尾：`docs/logs/2026/09-01.md` 追加执行条目（本批全量验证记录）；独立结束审计 APPROVE 后回写 roadmap M1.2a2 `done`（批次证据摘要沿 M1.1 批格式）。

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa4b45335ffeb00kE4tIT1rsDc`）——0 Blocker + 0 Major；3 Minor（①compliance 锚点口径沿 `2026-09-01-0527-1` 协议明示（§BASELINE 机器块 1537 + pre-existing 增量登记，1542 为历史实测引用）；②Phase 3 行政项（日志/roadmap 回写）类型纯度——阶段头改 `Proof`（6）+ `Add`（2 行政收尾）；③视觉 spec 清单去 hedge 落实名）——①②③均已并入本稿。审查者实仓核验：137 CSV、mfg 8 表已 seed、26 规格表实体 1:1（9+4+2+1+6+2+2）、`ErpMfgBomOperation.operationId` mandatory → `ErpMfgRoutingOperation`（orm.xml:291-331）、`ErpInvBatch` 已 seed（M1.1b）、C08 保存 BOM/BomLine/MrpPlan/MrpDemand/MrpScenario（干扰面预分析必要性成立）、看板/报表读取面在批外、71/0/0/1 + R2c 溯源 0527-1、Related 链接全部可达。

## 快照重录合规声明

**未触发声明（2026-09-01 执行期实测，沿 M1.1 批六点核查格式）**：

1. **变更面**：本批 26 张新增 `erp_mfg_*.csv`（63 行，纯加性零既有行修改）+ `TestErpSeedDataIntegrity` 基线常量 164→190 + docs（seed-data.md 对账表 / manufacturing owner doc 新建）——除常量外均为 seed 资产纯新增。
2. **面 2（`app-erp-all/_cases` 集成快照）**：零重录零改动——快照机制 `_chgType` 增量记录（A/U/D），种子纯加性插入不入既有快照、显式 id（<100000）不消耗 default 序列（M1.2a1 面引用面 grep 同口径）；预分析标记的高风险面 C08 经两条种子侧裁决（零默认 BOM → `findDefaultBomOrNull` 对种子物料返回 null 与既有录制口径一致；MRP 需求按 planId 作用域加载 → 种子 MRP 链不入 C08 运算输入）实测零漂移，C07/C08 全绿。
3. **面 1（各域 `module-<domain>/_cases`）**：零影响（域模块测试不声明 `nop.orm.init-database-data`，种子不进其装载路径）——M1.1a/M1.1b/M1.1c/M1.2a1 同口径，零重录。
4. **视觉双面义务**：`dashboards.visual`（10/10）+ `dashboards.snapshot`（10/10，含 mfg 看板）+ `reports.visual`（24/24）+ `reports.snapshot`（6/6，含生产差异/预测差异/CRP 报影像素基线）实测全绿**零漂移**——DOM 与像素双面均未触发重录（看板/报表读取面只读既有 8 表，本批 26 表零消费），双面均为实测通过态，无单面重录情形。
5. **E2E 数值断言联动**：mfg 相关 value/smoke spec 8/8 全绿（manufacturing 看板 KPI 期望值与 3 张 mfg 报表 value 断言零改动）——联动评估结论「零消费零联动」实测背书。
6. **提交说明登记**：本批 seed 变更 → 双面重录范围 = 空（面 2 零用例重录、面 1 零影响、视觉双面零漂移）；预存失败 1 项（mfg-subcontract-chain 2 用例）经移除本批种子对照实验判定与本批无关，登记 `docs/bugs/2026-09-01-2115` 归 successor。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（26 CSV + 常量 + 对账表 + manufacturing owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/快照重录注记 + manufacturing seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无——M0.1 规格 CSV-only 可满足性核验已确认本批 26 实体零 ORM 依赖，规格表全部必填 FK 锚点均已 seed；触发条件未发生。执行期若证伪按反松弛规则移入本节分类登记）

## Closure

Status Note: 计划可闭包——26 seed CSV（63 行）全部落地（164→194 CSV，manufacturing 域内 34/34 全覆盖），门禁常量 164→190 与 seed-data.md 对账表（有 seed 190 / 精确缺 178 / 运行时口径收敛 178）及 `_init-data/` 实仓构成（194 CSV = 190 app.erp + 4 平台 + 1 SQL）一致；`docs/design/manufacturing/seed-data.md` owner doc 段落地。干扰面零漂移双裁决（零默认 BOM + 零 FIRMED 滚算）经源码核对与实测双重背书。验证终态：`TestErpSeedDataIntegrity` 4/4 全绿、`mvn test -pl app-erp-all` 71/0/0/1 = M0.2/M1.1x/M1.2a1 基线精确一致（快照重录双面义务未触发，未触发声明在案）、fresh-DB 装载 26/26 抽样表 findPage==CSV 行数、视觉双面 dashboards 20/20 + reports 30/30 零漂移 + mfg value/smoke 8/8 零消费零联动、compliance checker exit 0 R2c=1542 零新增漂移。执行期登记 1 项预存失败（mfg-subcontract-chain 2 用例，移除本批种子对照实验判定与本批无关）落 `docs/bugs/2026-09-01-2115` 归 successor。roadmap M1.2a2 已回写 `done`。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session）`ses_fa2f053acffe6XN5ZuUBDVwxOI`
- Evidence: VERDICT **APPROVE**（0 Blocker / 0 Major / 0 Minor）。审计 A..J 十项全 pass 实仓核验：①194 CSV + 1 SQL 实测、26 张本批 CSV 行数合计 63 与计划一致、全量扫描零审计列/零大写布尔/零滚动日期/零 ID≥100000；②12 张表 FK 抽查逐值闭环零悬空（含 bom_operation→routing_operation 跨族边、batch_genealogy→erp_inv_batch 跨域边物料语义一致）；③8 个字典逐值合法（含 ErpMfgConstants simulation-status/param-type 侧）；④零漂移双裁决逐行核实且源码依据成立（BomExpander.java:62-75 / ProductionVarianceCalculator.java:345-354 + :117-121）；⑤常量 190（javadoc 沿革含 M1.2a2）+ 363/5 未触碰；⑥对账表 190/178 + 增量行 + 194 CSV 注记，算术自洽（93+97=190 / 270−92=178）；⑦验证三项独立复跑全复现（gate 4/4 + compliance exit 0 R2c=1542 逐规则对表 + 全量 suite 71/0/0/1）；⑧日志条目在案一致、计划零 `- [ ]` 残留、未触发声明六点齐全；⑨owner doc 26 表行数与实仓逐一相等、FK 抽查 3 边一致；⑩预存失败登记证据链完整（对照实验 168 CSV 重建 jar 复跑同型失败同差值 -60）。

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
