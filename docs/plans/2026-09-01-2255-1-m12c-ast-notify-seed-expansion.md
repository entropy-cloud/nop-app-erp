# 2026-09-01-2255-1 M1.2c 核心业务域 seed 扩面 C——assets + notify 跨域派发（19 CSV）

> Plan Status: completed
> Last Reviewed: 2026-09-02
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.2c（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议）、同批 `2026-09-01-2255-2-m13-hr-seed-expansion.md`（N=2）/ `2026-09-01-2255-3-m14a-crm-cs-seed-expansion.md`（N=3）
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径；运行时注册 368 = 363 + finance 5 缺 className 补充档，本域不涉及），218 个有 seed（`app-erp-all/src/main/resources/_vfs/_init-data/` 实测 222 CSV = 218 app.erp + 4 平台，+ 1 SQL `zz-sequence-advance.sql`）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 218` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集，非本域不触碰）。
- 两域实测已 seed / 缺 seed（与 roadmap M1.2c 清单一致）：
  - assets：已 seed 3（asset / asset_category / depreciation_schedule，2210-1），缺 **17**（asset_action_log / asset_capitalization / asset_model / cip / cip_cost_item / cip_progress_billing / disposal / inventory / inventory_line / maintenance / maintenance_cost / merge / merge_line / movement / split / split_line / value_adjustment）；
  - notify：已 seed 1（sys_notification_template，0330-1 聚合 27 行），缺 **2**（sys_notification / sys_notification_read）。
  - 逐实体规格见 `docs/architecture/seed-data.md` 规格表 assets / notify 两节，本计划逐行引用不复制。
- 既有 FK 锚点已就绪：`ErpAstAsset`〔已seed〕（规格表 assets 节 7 行必填 FK 目标：action_log / disposal / maintenance / merge_line / movement / split / value_adjustment）+ `ErpSysNotificationTemplate`〔已seed〕；notify 2 表无指向批外实体的必填 FK（sys_notification_read 必填 FK = sys_notification〔本批〕，批内主子闭环）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 实测，经 M1.1a/M1.1b/M1.1c/M1.2a1/M1.2a2/M1.2b 六批维持；known-good-baselines 最新登记行滞后于实测，基线追行归 M3.1）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542** 为历史实测值（+5 pre-existing 增量已登记，沿 plan `2026-09-01-0527-1` 口径协议）；全 reactor 已知 2 处预存回归（hr/drp）为 roadmap Non-Goal，与本计划无关。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000；UoM 外键列名为 `UO_M_ID`（分叉拼写陷阱——若本批实体存在 UoM 引用列，以 ORM/XMeta 生成列名为准先抽样核对）；装载拓扑序由 `DataInitInitializer` 自动排序。
- 冻结时钟纪律（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期列使用静态固定值，禁止 `now()`/滚动期间语义（action_log / maintenance / disposal 类实体尤其注意）。
- **干扰面**（本计划核心风险，读取面均 2026-09-01 实仓核实）：`app-erp-all/_cases` 集成用例 grep `ErpAst|ErpSysNotification` **零直接引用**，但集成快照 DB 状态面需按 `_chgType` 增量机制评估（种子纯加性插入不入既有快照，M1.2b 实证）；**assets 看板 KPI 消费本批表**——`ErpAstDashboardBizModel.getDashboardKpi → sumCipBalance()`（`daoFor(ErpAstCip.class)` 过滤 `isCompleted=false` 求 Σ `accumulatedCost`）输出 `cipBalance`，经 `/ast-dashboard-main` 页面渲染且 `dashboards.snapshot.spec.ts` 含该看板像素快照——本批 `ErpAstCip` seed 若按在建语义（isCompleted=false + accumulatedCost>0）落行将改变 `cipBalance` 并触发像素双面重录，故 Phase 1 设零漂移种子侧裁决（沿 M1.2a2/M1.2b 先例）；assets 报表消费面——`ast-disposal.value.spec.ts` 消费本批 `ErpAstDisposal` 且其注释**预注册 successor 触发条件「ast-disposal gains E2E seed data」（本批恰好触发，须显式裁决）**，`reports.visual.spec.ts` 含 `ast-asset-disposal-detail` DOM 断言；`assets.value`/`assets.smoke` 为部分键断言（`assertDashboardKpiValues`），不含 `cipBalance` 键——预期零影响，执行期复证；`notify-inbox.action.spec` 为相对/下界断言自包含（0330-1 实证），种子通知行预期零影响；种子 `sys_notification_read` 行仅挂种子通知 id（< 100000），与运行时派发行（≥ 100000，zz-sequence-advance 域）零交集。
- **保护区域**：notify 跨域派发子系统触部署配置/外部集成区域 = **plan-first**（roadmap 横切关注点 1 + `ai-autonomy-policy.md` 保护区域表 `deployment / external integrations` 行）——本批 seed 为被动数据行（通知记录 + 已读记录），不改派发引擎/模板/Processor 行为；owner doc = `docs/design/notify/`（inbox-patterns.md / use-cases.md）+ `docs/architecture/seed-data.md` §notify 聚合先例（0330-1）。plan 内显式「独立 plan-audit」checkbox 见 Phase 1。

## Goals

- 19 个 seed CSV 落地（assets 17 + notify 2，逐行消费规格表两节），每实体最小可用数据集（行数 ≤ 20，按规格表建议行数与用例指示编码 P / N-TERM），FK 引用零悬空，资产生命周期主子表链路 + 跨域派发三件套补齐（template〔已seed〕+ notification + read）。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（基数以「218 + 已落地批次 CSV 数」为准，本批 +19），seed-data.md 对账表同步。
- `docs/design/assets/seed-data.md` + `docs/design/notify/seed-data.md`「种子数据」owner doc 段落地（2 个新文件）。

## Non-Goals

- 不修改任何 ORM 模型（`module-*/model/*.orm.xml`，保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不触碰通知派发引擎 / 模板渲染 / Processor 逻辑（seed 为被动数据行，不改 `erp_sys_notification_template` 既有 27 行）。
- 不覆盖 hr / crm / cs 及其余 M1.x 工作项的缺 seed 实体（归同批 N=2/N=3 与后续批次）；不触碰 finance 5 个缺 className 运行时实体。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不引入 production-grade 真实个人数据。
- 不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 assets/notify 两节 + 对账表）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.2c 行 + 横切关注点）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）、`docs/design/notify/`（跨域派发 owner doc）
- Skill Selection Basis: roadmap M1.2c 行指定 `nop-backend-dev`（数据资产须对齐实体/字典/列命名约定，技能路由到平台模型文档）；Proof 阶段运行测试套件与视觉 spec 属测试域，加载 `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件，回滚 = 删除本批新增文件 + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批三计划（N=1/2/3）之一；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，若 N=2/N=3 先落地，常量基数以「218 + 已落地批次 CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（19 CSV = 17 + 2）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪；规格表 assets/notify 两节全部必填 FK 锚点均已 seed）

- [x] 逐实体核对 ORM 表名与列（`module-assets` / `module-notify` 两域 `model/*.orm.xml` tableName / `code=`）后，按规格表 assets / notify 两节（19 行，逐行引用不复制）创建 19 个 CSV；主子表组：assets 11 独立表（含 5 主子表头：cip×2 子 / inventory / maintenance / merge / split）+ 6 子表，notify 1 组主子（notification + read）；行数与用例指示编码（P / N-TERM）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（ErpAstAsset / 跨域主数据）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样同域既有 CSV，如 `erp_ast_asset.csv` / `erp_sys_notification_template.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c/M1.2b 先例）
      - Skill: `nop-backend-dev`
- [x] **零漂移种子侧设计裁决（cip → 看板 KPI 面，沿 M1.2a2/M1.2b 先例）**：`ErpAstCip` 种子行全部落转固完成终态语义（`isCompleted=true`）或 `accumulatedCost` 零值，维持看板 `cipBalance=0` 与既有像素快照零漂移；若规格表用例指示证伪该语义（P 行须在建态），改为显式预判像素漂移并预登记双面重录分支；裁决与理由落本计划执行证据
      - Skill: `nop-backend-dev`
- [x] Decision: `ast-disposal.value.spec` 预注册 successor 触发条件（「ast-disposal gains E2E seed data」）由本批达成——裁决处置方式（默认：既有 title-token 断言经预分析证实不破，「升级数值 token 断言」不属本计划范围，登记 Deferred But Adjudicated 归 M2.x 报表 value 工作；若预分析证伪断言稳定性则在本计划内同步基线），理由与替代方案落本节
      - Skill: none
- [x] **保护区域暂停协议（notify 段 = plan-first，roadmap 执行机制 4）**：独立 plan-audit——独立子代理（fresh session）审查本计划 notify 2 CSV 的字段/状态/引用设计与 `docs/design/notify/` owner doc 的一致性，批准记录落盘本计划后方可落地 notify 段 CSV；assets 段无保护区、不阻塞
      - Skill: none
- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——assets 看板 BizModel KPI 读取面复证（`module-assets/erp-ast-service` `ErpAstDashboardBizModel` 消费表全枚举 + cip 零漂移裁决落实核对）、assets 报表 spec 消费面（`tests/e2e/reports/ast-disposal.value.spec` / `ast-depreciation.*` / `reports.visual.spec` ast 断言）、`app-erp-all/_cases` 集成用例表引用面复证、`notify-inbox.action.spec` 相对断言面、assets/notify 相关 value spec 断言面，判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

Exit Criteria:

- [x] 19 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表
- [x] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内
- [x] cip 零漂移种子侧裁决（或预登记重录分支）与 disposal successor 处置裁决均已登记
- [x] notify 段独立 plan-audit 批准记录落盘本计划
- [x] 干扰面预分析结论在案（零漂移预期清单或预判漂移用例清单）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/assets/seed-data.md`、`docs/design/notify/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：基数以「218 + 已落地批次 CSV 数」为准本批 +19，`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed +19，精确缺 seed −19）+ 新增 M1.2c 批次增量行 + 快照重录义务节资产计数注记（本批后 = 241 CSV + 1 SQL，以对账表批次链为准）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/assets/seed-data.md` + `docs/design/notify/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注）、用例指示编码、negative 行语义（N-TERM 终态行）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量与对账表数值一致，且与 `_init-data/` 实际 CSV 构成一致
- [x] 2 个域 seed-data.md owner doc 段落地且与实际 CSV 内容一致

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移，按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏 → 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行 assets 域相关视觉 spec（`dashboards.visual` + `dashboards.snapshot` 含 assets 看板 / `reports.visual` + `reports.snapshot` / assets 域相关 value spec），实仓 `tests/e2e/visual/` 清单复核后如有额外两域相关 spec 一并纳入；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段（载体按 e2e-runbook §快照重录合规声明协议裁决落位）；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：`assets.value` / `assets.smoke` / `assets.list-value` / `notify-inbox.action.spec` + assets 报表 spec（`ast-disposal.value` / `ast-depreciation.*`）及 Phase 1 预分析标记的消费面——有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 6 张本批表（assets ≥ 5 含主子表头 + notify 2 表全抽）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.2c `ready` → `done`（含批次证据摘要，格式沿 M1.1/M1.2 批先例）
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过
- [x] `mvn test -pl app-erp-all` 与基线一致或更优；快照漂移处置已登记（含快照重录合规声明，若触发）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误）
- [x] compliance checker 零漂移；日志条目在位；roadmap 状态回写完成

## Execution Evidence（执行证据）

### Phase 1 · 干扰面预分析（Proof，执行首项，2026-09-01 实仓核实）

**1. assets 看板 KPI 消费面复证**（`module-assets/erp-ast-service/.../dashboard/ErpAstDashboardBizModel.java`）：
- 消费表全枚举 = `ErpAstAsset`（IN_SERVICE Σ originalValue / Σ accumulatedDepreciation）+ `ErpAstAssetCategory` + `ErpAstDepreciationSchedule`（EXECUTED Σ actualAmount）+ `ErpAstCip`（`sumCipBalance()` L197-206：`eq("isCompleted", FALSE)` 过滤后 Σ `accumulatedCost`）。**17 张本批表中仅 `ErpAstCip` 被看板消费**。
- cip 零漂移裁决落实核对：见下节裁决——全部种子 Cip 行 `isCompleted=true` → `isCompleted=false` 过滤集为空 → `cipBalance` 恒为 0，与既有像素快照零漂移。

**2. assets 报表 spec 消费面**：
- `tests/e2e/reports/ast-disposal.value.spec.ts`：`expectedTokens=['资产处置明细表']` 纯 title token 子集断言（L8 注释自证「无数字 token 可断言」）；新增 disposal 数据行不移除 title → 断言不破。其预注册 successor 触发条件（L7-8「ast-disposal gains E2E seed data」）由本批达成 → 处置裁决见下节。
- `tests/e2e/reports/ast-depreciation.value.spec.ts`：断言 token（120000.00/6000.00/2000.00/114000.00）全部来自**既有** `erp_ast_depreciation_schedule` 行（asset 2），本批不改该表 → 零漂移。
- `tests/e2e/reports/ast-depreciation.smoke.spec.ts` / `ast-disposal.smoke.spec.ts`：GraphQL-200/DOM 泛化检查，与数据量无关。
- `tests/e2e/visual/reports.visual.spec.ts` L118-123：`ast-asset-disposal-detail` DOM 断言 tokens = `['资产处置明细表','处置类型','清理损益']` 全部为静态标题/列头 → 数据行新增零影响。
- `tests/e2e/visual/reports.snapshot.spec.ts`：像素快照仅覆盖 fin-income-statement / md-material-price-list / crm-lead-conversion-funnel / fin-ar-ap-aging / cs-ticket-sla-csat-summary / mfg-crp-load 六页，**无任何 ast 报表页** → disposal 种子行零像素漂移。
- `tests/e2e/reports/reports.download.spec.ts`：asset-disposal-detail 下载断言 tokens 同为静态标题/列头 → 零影响。
- 报表数据集实现（`ErpAstReportBizModel.buildAssetDisposalDetailDataset` L260+）：按 businessDate 区间读 `ErpAstDisposal`，资产编码经 `getAsset()` 关系解析——种子 disposal 行 assetId 指向已 seed 资产 1/2/3，关系解析安全。

**3. `app-erp-all/_cases` 集成用例引用面复证**：grep `ErpAst|ErpSysNotification` 的命中全部位于 `output/tables/*.csv`（运行时生成行的增量快照，含 `erp_sys_notification(_read).csv` 运行时派发行），**零 input 侧种子引用**；按 `_chgType` 增量机制（M1.2b 实证），纯加性 `_init-data` 种子行不入既有快照 → 预期 `mvn test -pl app-erp-all` 快照面零漂移（Phase 3 实测复核）。

**4. `notify-inbox.action.spec.ts` 相对断言面**：全部断言为相对/下界（自建唯一 eventType 带 `Date.now()` 后缀；`countUnread >= 1`；`afterN == beforeN - 1`；`markAllRead >= 2`），清理仅删自建行。种子通知行按 recipient=`demo-user`（≠ E2E 登录用户 `nop`）落行 → 与 `findUnread(userId:"nop")` / `markAllRead(userId:"nop")` 查询集零交集。

**5. 其他消费面扫掠**：`tests/e2e/dashboards/assets.value.spec.ts` 断言键 = originalValue/accumulatedDepreciation/netBookValue/periodDepreciation 四键（无 cipBalance 键），数据源为 ErpAstAsset + ErpAstDepreciationSchedule 既有行 → 零影响（与计划基线预判一致）；`assets.smoke` 静态关键词；`visual/dashboards.snapshot.spec.ts` assets 像素快照消费看板 KPI（cipBalance=0 维持）→ 零漂移；`ast-cip-capitalization / ast-inventory-count / ast-maintenance / ast-value-adjustment.action.spec` 均自建自清理、断言自建行状态迁移 → 种子行零干扰；`negative/e1-2-menu-filter.smoke` 仅菜单可见性；`crud/placeholder-pages.smoke` disposal-wizard 页静态关键词。grep 全 E2E 无 notify 像素/DOM 快照断言面（仅 notify-inbox.action.spec 一处）。

**结论：零漂移预期清单**（无预判漂移用例）：全部 19 CSV 按「cip 全转固终态 + 通知行 recipient=demo-user + 文档行 posted=false」设计，预期 E2E/集成/compliance 三面零漂移。

### Phase 1 · cip 零漂移种子侧裁决（Decision）

**裁决**：`ErpAstCip` 2 行种子全部落**转固完成终态语义**——`STATUS=TRANSFERRED` + `IS_COMPLETED=true` + `ACCUMULATED_COST>0`（与子表 Σ 自洽），`COMPLETED_ASSET_ID` 指向已 seed 资产。
**理由**：规格表用例指示 P+N-TERM 中 N-TERM 天然 = TRANSFERRED 终态；P 行若按在建语义（isCompleted=false）落行将进入 `sumCipBalance()` 求和集改变 `cipBalance` → 像素双面重录。转固终态分支使看板过滤集为空，是最小代价零漂移方案；P 行取「已转固工程」正例语义同样满足「最小可用数据集」。
**替代方案**：(a) P 行 isCompleted=false + `ACCUMULATED_COST=0`——保留在建态但子表成本项与 0 累计成本语义冲突（域内金额自洽破坏），rejected；(b) 在建态 + 预登记像素双面重录——把零漂移可达成的问题升级为重录成本，违背最小干预，rejected。
**残留风险**：未来看板若新增「已转固工程数」类 KPI 将消费 TRANSFERRED 行——触发条件登记于 Deferred But Adjudicated。

### Phase 1 · disposal successor 处置裁决（Decision）

**裁决**：默认处置成立——`ast-disposal.value.spec` 既有 title-token 断言经预分析证实**不破**（`expectedTokens=['资产处置明细表']` 为渲染结果子集断言，报表从「仅标题+空合计」变为「标题+数据行」仍含该 token）；「升级数值 token 断言」不属本计划范围，登记 **Deferred But Adjudicated → M2.x 报表 value 工作**（successor 触发条件「ast-disposal gains E2E seed data」已由本批达成，无需再守候）。
**替代方案**：本计划内同步升级数值 token 基线——越出「CSV-only + 不改断言」计划边界且需重录协议面扩张，rejected。
**残留风险**：若 Phase 3 视觉/报表扫掠实测发现 title 断言破（预分析证伪），则在本计划内同步修基线（预案已备）。

### Phase 1 · 保护区暂停协议（notify 段）独立 plan-audit 记录

- 审计者/会话：独立子代理 fresh session，task id `ses_fa21e8f42ffeNQR7Kt71kgSqMO`（2026-09-02 执行日）
- 审查范围：本计划 notify 2 CSV（`erp_sys_notification.csv` / `erp_sys_notification_read.csv`）的字段/状态/引用设计与 `docs/design/notify/` owner doc（inbox-patterns.md / use-cases.md / README.md）+ `docs/architecture/seed-data.md` §notify 聚合先例的一致性
- 批准结论：**approve**（0 Blocker / 0 Major / 3 Minor 非阻塞）。审计实仓核验 A-F 六问全部通过：字典值与必填列完备、TEMPLATE_ID 可选 FK 指向已 seed 7101/7102 且类型 1:1 匹配、生命周期语义一致（SENT+SENT_AT / FAILED+errorMsg；`unreadOf` 过滤 `status IN (SENT,MERGED)` 使 FAILED 行对未读面结构不可见）、已读记录满足 `UK_SYS_NOTIFY_READ_NOTIF_USER` 且 userId=recipientUserId、零干扰证明（`findUnread`/`findRead` 按 recipientUserId/userId 过滤，E2E 用户 `nop` 与 demo-user 零交集；`_cases` 仅 output 侧增量快照）、ID 7301-7303 与模板 71xx 段及运行时 ≥100000 段零冲突、列头/审计列省略/ISO-T 时间戳与既有 CSV 约定一致
- 3 Minor 非阻塞发现（归 Phase 2 owner doc 记录义务）：(1) `demo-user` 非平台 seed 用户（无 ORM to-one，门禁与查询不受影响；既有 `_cases` 已有 `autotest-ref` 等虚构用户先例）→ Phase 2 owner doc 显式登记 demo-user 为「展示用非登录接收人」；(2) N-TERM 正文引用 `V-DEMO-001` 无对应 seed 单据（正文为自由渲染快照文本，无 FK 无门禁影响）；(3) `FAILED` 为当前同步派发路径不产生的预留态（字典合法，owner doc 注明该行为演示终态非运行时产物）

### Phase 1 · 19 CSV 设计裁决（域内金额/状态自洽设计）

主状态语义统一沿 M1.2b 先例：P 行文档 = `DRAFT/UNSUBMITTED` 或业务完成态 + `posted=false`（统一 posted=false 先例）；N-TERM 行 = `CANCELLED/APPROVED` 终态。资产 3 张已 seed 卡片（1 笔记本 12000/0 折旧、2 机床 120000/6000、3 打印机 3000/0）为唯一 FK 锚点；员工 1/2/3、组织 1/2、币种 1/2、类别 1/2、项目 1、库位 1/2 均已 seed。逐实体行数与用例编码严格按规格表：asset_model 1(P) / asset_action_log 1(P) / movement 2(P+N-TERM) / value_adjustment 2(P+N-TERM) / disposal 2(P+N-TERM) / asset_capitalization 2(P+N-TERM) / cip 2(P+N-TERM，全转固终态) / cip_cost_item 2(P，挂 cip1，Σ=80000=cip1.accumulated_cost) / cip_progress_billing 2(P，挂 cip1) / split 2(P+N-TERM) / split_line 2(P，Σ=源资产原值/折旧/净值) / merge 2(P+N-TERM) / merge_line 2(P) / inventory 2(P=COUNTING 进行中+N-TERM=CANCELLED) / inventory_line 1(P，挂 inventory1，book_value=资产1净值 12000) / maintenance 2(P=COMPLETED+EXPENSE+N-TERM=CANCELLED) / maintenance_cost 2(P，Σ=800=maintenance1.total_cost_amount) / sys_notification 2(P=SENT+N-TERM=FAILED，recipient=demo-user) / sys_notification_read 1(P，挂 notification 种子行)。ID < 100000（notify 沿模板 7xxx 段取 7301+）；静态日期 2026-06/07；无 UoM 引用列（19 实体 ORM 零 UoM 列，分叉拼写陷阱不适用，已按 ORM code= 全量核对）。

### Phase 3 · 验证证据与快照重录未触发声明（2026-09-02 执行日实测）

**六项 Proof 实测记录**：

1. `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` **4/4 全绿**（执行期 1 次门禁拦截即修：`erp_ast_inventory.csv` 初版 `,,` 空单元格计数错位致 DATE 值落 CURRENCY_ID（`convert-to-type-fail`）+ REMARK 错落 `amountFunctional`（BigDecimal 转换失败）双信号拦截，修正单元格序列后全绿；M1.1c/M1.2a1/M1.2b「列错位由门禁首跑拦截」先例同型）。
2. `mvn test -pl app-erp-all` **71/0/0/1 = M0.2/M1.1x/M1.2a1/M1.2a2/M1.2b 基线精确一致**（C01-C21 + P2P 试点全绿，含 C09/C10/C11/C12/C13/C20a）。
3. 视觉双面 **50/50 全绿零漂移**（`dashboards.visual` 10 + `dashboards.snapshot` 10 + `reports.visual` 24 + `reports.snapshot` 6）：assets 看板 DOM + 像素基线维持（cip 全转固终态裁决实测证成——`cipBalance=0` 像素零漂移）；`ast-asset-disposal-detail` DOM 断言 title/列头 token 全数命中（disposal successor 处置裁决实测证成——title-token 断言未破）。
4. 消费面联动评估 **23/23 全绿零联动**：`assets.value`（KPI 四键数值不变：originalValue=135000/accumulatedDepreciation=6000/netBookValue=129000/periodDepreciation=2000）/ `assets.smoke` / `crud/assets.smoke` / `crud/assets.list-value` / `ast-disposal.value`（title token 命中）/ `ast-depreciation.value`（既有行数值 token 命中）/ `ast-depreciation.smoke` / `ast-disposal.smoke` / `notify-inbox.action`（相对断言面零干扰）+ `ast-cip-capitalization` / `ast-inventory-count` / `ast-maintenance` / `ast-value-adjustment` 四 action spec（自建自清理零种子干扰）。
5. 运行时装载证明：fresh-DB 启动（playwright webServer 全 flag 路径，`rm db` fresh 重置 + `init-database-data=true`）后 GraphQL `findPage` 抽样 **19/19 表 total==CSV 行数**（临时 Proof spec `tests/e2e/proof-m12c-seed-counts.spec.ts` 全 19 表断言，超计划 ≥6 表要求；用后即删；0 冲突 / 0 列映射错误）。jar 重打包 `mvn clean install -DskipTests -pl app-erp-all` BUILD SUCCESS（`unzip -l` 实测打包 241 CSV）。
6. `bash docs/audits/nop-compliance-checker.sh` **exit 0，零新增漂移**：R2c=1542（机器块 1537 + 已登记 pre-existing +5）/ R2b=242（机器块 240 + 已登记 +2）/ R12a=71（机器块 70 + 已登记 +1），沿 plan `2026-09-01-0527-1` 口径协议。

**快照重录合规声明**：未触发。集成面（`mvn test -pl app-erp-all` 71/0/0/1 基线一致）与视觉面（双面 50/50 零漂移）均零漂移，与 Phase 1 干扰面预分析「零漂移预期清单（无预判漂移用例）」结论**逐项对账一致**：_cases 增量机制下种子纯加性行未入既有快照；cip 零漂移裁决、disposal title-token 断言、notify 相对断言面三项预判全部实测证成；面 1（各域 `_cases`）/面 2（app-erp-all 集成用例）双面重录义务零触发，无双面重录动作，无单面重录。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 fresh session `ses_fa28192f6ffe2cdI5Cy3sMd5rk`）——0 Blocker + 2 Major + 5 Minor。Major M-1：assets 看板 `sumCipBalance()` 消费本批 `ErpAstCip`（isCompleted=false 求 Σ accumulatedCost → cipBalance KPI → 像素快照在案），原「17 表为子表/日志/动作类预期零漂移」定性错误，须零漂移种子侧裁决；Major M-2：`ast-disposal.value.spec` 消费本批 `ErpAstDisposal` 且预注册 successor 触发条件被本批达成，消费面遗漏。Minor 5 项（FK 锚点 8→7 行、执行机制 4 编号错位、`erp-ast-service` 模块路径笔误、notify FK 表述、主子表计数口径）。全部已并入本稿：干扰面段重写（cip 消费链 + disposal successor 登记）、Phase 1 增 cip 零漂移裁决项 + disposal 处置 Decision 项 + 预分析/联动面扩充、5 项 Minor 修正。审查者实仓核验 16 项（CSV 构成/常量/缺 seed 清单 1:1/_cases 零命中/规格表对齐/notify spec 相对断言/zz-sequence 100000 等）全部记录在案。
- Independent draft review iteration 2: accept（独立子代理 fresh session `ses_fa2731eebffeS7LOY7GZvOgNOv`）——9/9 修订核验项全部通过（cip 消费链定性 1:1 吻合 BizModel 实仓、disposal successor 触发条件引文逐字一致、FK 7 行/执行机制 4/erp-ast-service/notify FK/计数口径全对齐、Review Record 在案、反松弛零命中、快照载体注记在案），无新引入缺陷。共识达成，转 `active`。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（19 CSV + 常量 + 对账表 + 2 个 owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/快照重录注记 + 2 个域 seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

- **disposal 报表数值 token 断言升级 → M2.x 报表 value 工作**（Phase 1 Decision 项登记）：`ast-disposal.value.spec` 预注册 successor 触发条件「ast-disposal gains E2E seed data」已由本批达成；既有 title-token 断言（`expectedTokens=['资产处置明细表']` 子集断言）经预分析证实不破并经 Phase 3 实测证成（23/23 消费面全绿）。升级数值 token 断言越出本计划「CSV-only + 不改断言」边界，归 M2.x 报表 value 工作（报表已可断言确定性种子数据行）。
- **cip TRANSFERRED 行的未来 KPI 消费监视**（Phase 1 cip 裁决残留风险登记）：若未来看板新增「已转固工程数」类 KPI 将消费本批 2 行 `TRANSFERRED` 终态行——触发条件：assets 看板 KPI 集扩展时复核。当前 `sumCipBalance()` 唯一消费面过滤集为空，零影响。

## Closure

Status Note: 2026-09-02 闭包。全 3 Phase 落地：19 seed CSV（assets 17 + notify 2，26 行）+ 门禁常量 218→237 + seed-data.md 对账表同步 + 2 个域 owner doc；notify 段 plan-first 保护区独立 plan-audit APPROVE 在先，结束审计 APPROVE 收尾。验证全绿：TestErpSeedDataIntegrity 4/4、`mvn test -pl app-erp-all` 71/0/0/1 基线精确一致（快照重录双面义务零触发）、fresh-DB findPage 19/19 全批表、视觉双面 50/50 零漂移、消费面联动 23/23 零联动、compliance checker exit 0（R2c=1542 零新增漂移）。执行期 1 次门禁拦截即修（inventory CSV 空单元格计数错位），沿批次先例闭环。Deferred But Adjudicated 登记 2 项非阻塞跟进（disposal 数值 token 升级归 M2.x；cip TRANSFERRED 行未来 KPI 消费监视）。roadmap M1.2c 已回写 `done`。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，task id `ses_fa1f73a83ffeb3Bs1OuhplnQHh`，2026-09-02）
- Evidence: A..J 十项实仓核验全 pass——(A) 19 CSV 逐表 `wc -l` 行数 1:1 + 全目录 241 CSV + 1 SQL；(B) FK 锚点/字典码逐值核验（含 movement 列序逐列复核）；(C) cip 全转固终态 + 成本行 Σ=80000 与 `sumCipBalance()` 过滤面交叉核对；(D) 常量 237/4/363 与 javadoc 批次沿革；(E) seed-data.md 237/131/M1.2c 批次行/241 CSV 双处；(F) 2 个 owner doc 与实仓一致 + 3 条审计 Minor 义务在案；(G) notify plan-audit 批准记录含 task id；(H) 验证两项独立复跑全复现（TestErpSeedDataIntegrity 4/4 绿 + compliance checker exit 0 R2c=1542/R2b=242/R12a=71）；(I) 日志条目在位；(J) 计划文本一致性（2 Minor 计划文本行政项 = Phase 3 Status 标签 + Deferred But Adjudicated 段落填实，均已于闭包 pass 落实）。执行者未自我审计。

Follow-up:

- （仅非阻塞跟进项；已确认的缺陷不得出现在此处）
- disposal 报表数值 token 断言升级 → M2.x 报表 value 工作（Deferred But Adjudicated 登记，successor 触发条件已达成）
- assets 看板若新增「已转固工程数」类 KPI，复核本批 2 行 TRANSFERRED 种子行消费面（Deferred But Adjudicated 登记）
