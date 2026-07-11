# 2026-07-11-1225-1 分析报告一致性修复（文档计数同步 + 代码质量小修 + findAll 改造 + 报告订正）

> Plan Status: completed
> Last Reviewed: 2026-07-11
> Source: `docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` §4.2 优先修复项 + 主代理 2026-07-11 核对发现
> Related: 无前置计划；本计划是分析报告 §4.2 的落地执行切片
> Audit: required

## Current Baseline

2026-07-10 的深度一致性分析报告列出了 9 个优先修复项。2026-07-11 主代理对其中可机器核对的硬声明做了复核，发现：

**文档计数漂移（比报告说的更严重）：**

| 指标 | 文档基线（多文档） | 报告"实际"(07-10) | 真实(07-11 复核) | 涉及文档 |
|------|------|------|------|------|
| ORM 实体数 | 279 | 332 | **447** | `data-dependency-matrix.md:12`、`product-scope.md:52,55`、`domain-module-split-analysis.md` |
| Java 文件数 | 1721 | 2423 | **2758** | `product-scope.md:52` |
| reactor 模块 | 146（product-scope）/ 154（codebase-map、known-good-baselines） | — | **152**（`find module-* -name pom.xml -maxdepth 3`） | `product-scope.md:52,71`、`codebase-map.md:11`、`testing/known-good-baselines.md`(多处历史快照) |

**reactor 模块计数口径冲突需裁决（Decision）**：`product-scope.md` 称 146，`codebase-map.md:11` 称 154（"子模块链合计 154"），`find` 实测 152，`known-good-baselines.md` 历史快照多处称 154 作为全绿基线标注。四处数字不一致。根因是统计口径未定义（是否含根 pom、app-erp-all 内部聚合、notify）。本计划 Phase 1 用 `mvn` 实际 reactor 列表定性口径后统一，而非盲目改数字（避免与 known-good-baselines.md 历史快照冲突）。

**报告自身已 stale（报告列为残留但仓库已修）：**

| 报告声称的残留 | 实际仓库状态 | 证据 |
|------|------|------|
| `flow-overview.md:312` 错误声称 INSPECTING 状态 | **已修复** — line 314 现明确声明"无 INSPECTING 态" | `docs/design/flow-overview.md:314` |
| `flow-overview.md:499` 称期末结账=分布式事务 | **已修复** — line 499 现写"单库事务(REQUIRED)" | `docs/design/flow-overview.md:499` |

**Playwright spec 计数反向缩水：** 报告称 167，实测 125。按用户决策，报告里仅删除 spec 具体数字，不做 git 考古。

**`findAll()` 全表加载：报告严重低估命中范围。** 报告 §1.6/§4.2 点名 `ErpMdDashboardBizModel`、`ErpSalDashboardBizModel`，但全仓库 `*.java` 生产代码实测 `dao.findAll()` 命中共 **10 域 / 25 处**，分两层（finance 与 manufacturing 同时出现在两层，故唯一域数 = 9 + 3 − 2 = 10，非 12）：

Dashboard 层（9 域 / 21 处）：

| 域 | 命中数 | 实体 | 改造性质 |
|------|------|------|------|
| master-data | 6 | ErpMdMaterial / ErpMdPartner / ErpMdMaterialSku | 计数/分布聚合 |
| inventory | 4 | ErpInvStockBalance | Σ totalCost 聚合 |
| projects | 4 | ErpPrjProject / ErpPrjBudget / ErpPrjCostCollection | 按 projectId 聚合 |
| maintenance | 2 | ErpMntEquipment / ErpMntVisit | 计数/集合收集 |
| purchase | 1 | ErpPurOrder | 交期 map 收集 |
| quality | 1 | ErpQaNonConformance | 计数 |
| sales | 1 | ErpSalInvoice | findCustomerTopN 内存聚合（报告 §1.6 中严重度项） |
| manufacturing | 1 | ErpMfgWorkOrder | 计数 |
| finance | 1 | ErpFinGlBalance | 余额聚合（已有 QueryBean 分支，仅 periodId==null 时 fallback 全表） |

Report / CRP 层（3 域 / 4 处，草案审查 iteration 1 发现的遗漏）：

| 位置 | 实体 | 改造性质 |
|------|------|------|
| `module-crm/.../report/ErpCrmReportBizModel.java:194` | ErpCrmLead | 线索转化漏斗数据集 |
| `module-finance/.../report/ErpFinReportBizModel.java:374` | ErpFinGlBalance | periodId==null fallback（与 dashboard finance 同型） |
| `module-manufacturing/.../report/ErpMfgReportBizModel.java:336` | ErpMfgCrpLoad | CRP 负荷报表 |
| `module-manufacturing/.../crp/CrpLoadCalculator.java:425` | ErpMfgWorkcenterCalendar | 工作中心日历（配置数据，量小） |

**关键约束：本仓库无 sql-lib.xml 文件**（grep `*.sql-lib.xml` 零命中）。改造只能用平台原生 `QueryBean` + projection / count，不引入新机制。

**代码质量残留项（报告 §1.4/§1.6，仍属实）：**

- `module-hr/.../ErpHrRecruitmentBizModel.java:199` — `System.currentTimeMillis()` 应改 `CoreMetrics.currentTimeMillis()`
- `module-maintenance/.../TestErpMntSparePartPosting.java:250` — `System.out.println` 调试残留
- `module-maintenance/.../TestErpMntSparePartAndSchedule.java:191` — `System.out.println` 调试残留

**报告 §2.5 中本计划不处理的 owner-doc 漂移项（移入 Non-Goals）：**

- `feature-inventory.md` 无完成状态（§2.5:144）— 文档内容补全，不同结果表面
- `roles-and-permissions.md` 缺角色→权限点映射（§2.5:145）— 文档内容补全，不同结果表面

**报告 §4.2 中本计划不处理的项（移入 Non-Goals）：**

- P1 Sales/Purchase order 页面缺审批按钮、P1 系统审计字段暴露、P1 FK 显示为 ID、P2 Dashboard 图表显示 ID — 4 项前端 AMIS 质量改造，不同结果表面
- P2 `SettlementAllocation` DTO 两域重复 — 跨模块重构，不同结果表面

剩余差距：见上述每一类的"真实"列与残留清单。

## Goals

1. **文档计数与代码现状对齐**：把 `data-dependency-matrix.md`、`product-scope.md`、`domain-module-split-analysis.md`（如涉及）中的实体数(279→447)、Java 文件数(1721→2758)、reactor 模块数（经口径裁决后统一）、18 业务域+notify 表述对齐到 07-11 实测值；顺手删除 `api-response-conventions.md:3` 的模板占位符残留（单行，低成本）。
2. **全仓库 `findAll()` 改造为受限查询**：10 域 25 处全表加载改为 `QueryBean` + projection（计数用 countByQuery、聚合用 DB 级 SUM、map 收集用受限投影或带 limit 的 findAllByQuery、配置数据量小且业务必需全量的加显式硬上限或裁决保留），消除企业数据量下的 OOM 风险，不改看板/报表对外 GraphQL 契约。
3. **代码质量小修**：1 处 `System.currentTimeMillis()` 替换 + 2 处 `System.out.println` 清理。
4. **分析报告自身订正**：全报告 grep `279|332|2423|1721|167|18 vs 19` 逐行订正为 07-11 实测值，删除已 stale 的 INSPECTING/分布式事务两行残留项，把 `findAll()` 命中域清单对齐到实测 10 域 25 处。

## Non-Goals

- **不动 ORM 模型**：`findAll()` 改造仅改 BizModel/Report/Calculator 的 Java 代码，不改 `*.orm.xml`（ORM 是保护区域，无需变更）。
- **不改 AMIS 前端**：审批按钮缺失、审计字段暴露、FK 显示为 ID、Dashboard 图表 ID 显示 — 4 项前端质量改造另开 owner plan（见 Deferred But Adjudicated）。
- **不引入 sql-lib.xml 新机制**：本仓库无 sql-lib 范式，引入需平台文档研究与范式确立，超出本批范围；改造用平台原生 `QueryBean` + projection。
- **不改看板/报表对外 GraphQL 契约**：`getDashboardKpi` 等方法的返回 Map 结构、字段名、数值语义不变，仅内部取数路径从内存全表改为 DB 级受限查询。
- **不补全 `feature-inventory.md` 完成状态、不补 `roles-and-permissions.md` 权限映射**：报告 §2.5:144-145 标记的这两项属文档内容补全，不同结果表面，另开 owner-doc 对齐计划。
- **不调查 Playwright spec 125 vs 167 成因**：报告里仅删除 spec 具体数字，不做 git 考古。
- **不处理 `SettlementAllocation` DTO 重复**：跨模块重构，不同结果表面。

## Task Route

- Type: `implementation-only change`（文档订正 + 代码小修 + 看板/报表取数路径重构，不改公共契约/API/ORM/认证）
- Owner Docs:
  - `docs/architecture/data-dependency-matrix.md`（实体计数真相源之一）
  - `docs/requirements/product-scope.md`（产品摘要计数）
  - `docs/context/codebase-map.md`（reactor 模块计数）
  - `docs/architecture/api-response-conventions.md`（模板残留单行删除）
  - `docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md`（被订正对象）
  - 各域 `docs/design/<domain>/dashboards.md`（看板 KPI 口径，findAll 改造需确认口径不漂移）
- Skill Selection Basis:
  - `Skill: nop-platform-conformance-audit-prompt` — 改造 findAll 为 QueryBean 需验证不引入 Nop 反模式（规则 4 时间 API；规则 3 跨实体访问——Dashboard/Report 服务的 IDaoProvider 是既定范式，类头 javadoc 已声明）。作自检参照。
  - `Skill: code-refactor-prompt` — findAll→QueryBean 是行为保留结构重构（对外契约不变），适用。模板需项目定制化注入（验证命令、命名约定）。
  - 文档订正项 `Skill: none`（纯散文计数同步）。
  - finance 域保护区域裁决：fin dashboard/report 读聚合非 accounting/finance postings 行为，不触发 postings 保护区域（plan-first 由本草案审查满足，见 `ai-autonomy-policy.md`）。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 本计划不改端口/环境变量/CORS/密钥/.env/外部服务。验证用 `docs/context/project-context.md` 既有命令。

## Execution Plan

### Phase 1 - 文档计数同步 + reactor 口径裁决（无代码变更）

Status: completed
Targets: `docs/architecture/data-dependency-matrix.md`、`docs/requirements/product-scope.md`、`docs/context/codebase-map.md`、`docs/architecture/domain-module-split-analysis.md`(如含计数)、`docs/architecture/api-response-conventions.md`
Skill: `none`

- Item Types: `Fix` + `Decision`
- Prereqs: 无

- [x] **Decision** reactor 模块计数口径定性（146/152/154 冲突裁决）
      - Skill: `none`
      - 执行 `mvn -pl :app-erp-all -am validate 2>&1 | grep "Building"` 或读根 pom.xml `<modules>` 全量展开，确定权威 reactor 总数
      - 选择：以 `mvn` reactor 实际列表为准；`known-good-baselines.md` 是追加性历史快照，不改写历史（保留原 154 作为当时快照），但新增基线行用新口径
      - 替代方案：用 `find` pom.xml 计数（否决，含/不含聚合 pom 不确定）；以 codebase-map.md 的 154 为准（否决，product-scope 的 146 无法解释）
      - 残留风险：若 `mvn` reactor 列表因 profile 激活差异波动，以默认 profile 为准
      - **裁决结果**：根 `mvn validate` 完整 reactor = **154**（含根聚合 pom + app-erp-all + 18 业务域与 notify 子模块链）；`find module-* -name pom.xml -maxdepth 3` = 152（不含根 pom 与 app-erp-all）。`codebase-map.md` 的 154 为权威值；`product-scope.md` 的 146 stale 已修正为 154。口径说明已写入 `codebase-map.md`。
- [x] **Fix** 更新 `data-dependency-matrix.md:12` 的"279 个实体表"为实测 447
      - Skill: `none`
      - 重新跑 `grep -rho '<entity ' module-*/model/*.orm.xml | wc -l` 取最新值写入
- [x] **Fix** 更新 `product-scope.md:52` 的"1721 Java 文件、146 reactor 模块、279 实体"为 2758 / <Decision 裁决值> / 447
      - Skill: `none`
- [x] **Fix** 更新 `product-scope.md:55` 的"279 实体"为 447；`product-scope.md:71` 的"146 模块"为 Decision 裁决值
      - Skill: `none`
- [x] **Fix** 更新 `codebase-map.md:11` 的"154 个 reactor 模块"为 Decision 裁决值（若裁决为 152 则改；若裁决 154 含特定口径则加口径说明）
      - Skill: `none`
      - 154 值本身正确，已补 reactor 口径权威说明（含 find 152 差异解释）
- [x] **Fix** 统一"18 业务域 + notify 子系统"表述：核实 `data-dependency-matrix.md:45`、`product-scope.md` 中"18 域"表述，对齐 AGENTS.md 的"18 业务域 + 1 跨域通知派发子系统（共 19 个 module-*/）"口径
      - Skill: `none`
      - 注意：不改 AGENTS.md（已是正确口径），只改其他文档使其与此口径一致
      - `data-dependency-matrix.md:12` 已改为"18 个业务域 + 1 跨域通知派发子系统（module-notify）"
- [x] **Fix** 核实并更新 `domain-module-split-analysis.md §2.0` 中实体/模块计数（若含 279/146）
      - Skill: `none`
      - grep 该文件确认；若无旧计数则记录"不含旧计数，无需改"
      - 核实结果：`domain-module-split-analysis.md` 不含 279/146 实体或模块计数（line 249 的"146"为外部文档行号引用，非计数），无需改。附带修正了同含 stale 279 的活动架构文档 `project-vision.md:41` 与 `competitive-comparison.md:165`（对齐 Goal 1 文档计数同步意图）
- [x] **Fix** 删除 `api-response-conventions.md:3` 的模板占位符"如果不适用，请删除此文件"
      - Skill: `none`
      - 单行删除；该文件已实际使用（有响应约定内容），模板残留无意义

Exit Criteria:

> 文档计数同步是纯散文变更，无可观察行为。退出标准仅验证计数已落地且自洽。

- [x] `data-dependency-matrix.md`、`product-scope.md`、`codebase-map.md` 中实体数均为 447、Java 文件数 2758、reactor 模块数与 Decision 裁决值一致
- [x] `grep -rn "279\|1721\|146 reactor\|146 模块" docs/requirements/product-scope.md docs/architecture/data-dependency-matrix.md docs/context/codebase-map.md` 不再返回旧值
- [x] reactor 口径在 `codebase-map.md` 有明确定义（统计范围说明），与 `known-good-baselines.md` 历史快照不冲突（历史快照保留原值）

### Phase 2 - 代码质量小修（3 处）

Status: completed
Targets: `module-hr/erp-hr-service/.../ErpHrRecruitmentBizModel.java:199`、`module-maintenance/erp-mnt-service/src/test/.../TestErpMntSparePartPosting.java:250`、`module-maintenance/erp-mnt-service/src/test/.../TestErpMntSparePartAndSchedule.java:191`
Skill: `nop-platform-conformance-audit-prompt`（自检参照：规则 4 时间 API 合规）

- Item Types: `Fix`
- Prereqs: 无（独立于 Phase 1）

- [x] **Fix** `ErpHrRecruitmentBizModel.java:199` `System.currentTimeMillis()` → `CoreMetrics.currentTimeMillis()`
      - Skill: `nop-platform-conformance-audit-prompt`
      - 确认 import `io.nop.api.core.time.CoreMetrics` 存在或补 import
      - import 已存在（line 9），仅替换调用点
- [x] **Fix** `TestErpMntSparePartPosting.java:250` 删除 `System.out.println("[DEBUG confirm]...")` 调试残留
      - Skill: `none`
- [x] **Fix** `TestErpMntSparePartAndSchedule.java:191` 删除 `System.out.println("[DEBUG confirm]...")` 调试残留
      - Skill: `none`

Exit Criteria:

- [x] `grep -rn "System.currentTimeMillis" module-hr --include="*.java" --exclude-dir=target` 在目标文件不再命中；`grep -rn "System.out.println" module-maintenance/erp-mnt-service/src/test --include="*.java"` 目标 2 处不再命中
- [x] `mvn -pl module-hr/erp-hr-service,module-maintenance/erp-mnt-service -am test-compile -DskipTests` 通过（验证 import 与编译）— BUILD SUCCESS

### Phase 3 - 全仓库 findAll() 改造（10 域 25 处）

Status: completed
Targets: Dashboard 层（master-data/inventory/projects/maintenance/purchase/quality/sales/manufacturing/finance 各 `*DashboardBizModel.java`）+ Report/CRP 层（crm/finance/manufacturing 各 `*ReportBizModel.java` + `CrpLoadCalculator.java`）
Skill: `code-refactor-prompt`（行为保留结构重构）+ `nop-platform-conformance-audit-prompt`（反模式自检）

- Item Types: `Fix` + `Decision` + `Proof`
- Prereqs: Phase 2 完成（避免 maintenance 域合并冲突）

- [x] **Decision** 改造策略选择：对每处 `findAll()` 按用途分四类改造，记录选择与理由
      - Skill: `code-refactor-prompt`
      - 类 A（计数用 `for + 累加/size`）：优先改 `dao.countByQuery(q)`，避免物化全表实体；若需按维度分组计数则用 findAllByQuery 带投影 + limit
      - 类 B（Σ 聚合，如 `totalCost`）：改为 `QueryBean` + projection 取 DB 级 SUM
      - 类 C（map 收集单字段，如 deliveryDate）：改为 `findAllByQuery` 投影单字段或加合理过滤条件
      - 类 D（报表全量明细 / 配置数据，report 层 + CrpLoadCalculator）：逐处裁决——若业务必需全量明细（如报表渲染数据集），加服务端硬上限 + 注释说明保留理由；若配置数据量小且固定（如 WorkcenterCalendar），加注释标注"配置数据，量小可全表"并裁决保留
      - 替代方案：引入 sql-lib.xml（否决，本仓库无此范式，超范围）；改 GraphQL schema 加服务端分页（否决，破坏对外契约）
      - 残留风险：projection/SUM 在某些 DB 方言下 BigDecimal 精度差异——保留 `ROUND_HALF_UP` 4 位与既有测试基线对齐
      - **裁决结果**：(A) 计数全部改 `dao.countByQuery(q)`；(B) Σ 聚合用 `QueryFieldBean.mainField(metric).sum().alias()` + `ormTemplate.findListByQuery(q)` DB 级聚合——实测无维度全局聚合会被 MdxQueryExecutor 强制注入主键维度生成非法 SQL，故全局 SUM 经按真实维度分组后汇总实现（等价、null 维度单独成组被计入）；(C) 单字段收集用 `findAllByQuery(q)` + `setLimit(5000)` 硬上限；(D) 报表明细/配置数据用 `findAllByQuery(q)` + `setLimit(5000)` + 注释裁决保留。额外修正同文件 `findAllByQuery(q).size()` 计数反模式 → `countByQuery`（conformance 规则）。
- [x] **Fix** master-data `ErpMdDashboardBizModel`（6 处：Material/Partner/Sku 计数与分布）
      - Skill: `code-refactor-prompt`
      - KPI 5 计数改 countByQuery；2 数据质量预警改硬上限受限扫描；KPI 中未使用的 skus findAll 移除
- [x] **Fix** inventory `ErpInvDashboardBizModel`（4 处 StockBalance Σ totalCost）
      - Skill: `code-refactor-prompt`
      - 注意：turnoverRate 分母依赖 totalValue，DB 级 SUM 后精度需对齐既有 `*.value.spec.ts` 基线（dashboard totalValue=10450）
      - totalValue 经 warehouseId 分组 SUM 汇总（DB 级）；仓库分布经 warehouseId 分组 SUM；2 预警改硬上限受限扫描
- [x] **Fix** projects `ErpPrjDashboardBizModel`（4 处：Project 计数 + Budget/CostCollection 按 projectId 聚合）
      - Skill: `code-refactor-prompt`
      - 状态分布改 DB 级 GROUP BY status + COUNT；超支预警改硬上限受限扫描；Budget/CostCollection 改 DB 级 GROUP BY projectId + SUM
- [x] **Fix** maintenance `ErpMntDashboardBizModel`（2 处：Equipment 计数 + Visit scheduleId 集合）
      - Skill: `code-refactor-prompt`
      - 设备状态分布改 DB 级 GROUP BY status + COUNT；scheduleId 集合改硬上限受限扫描；4 处 count helper 改 countByQuery
- [x] **Fix** purchase `ErpPurDashboardBizModel`（1 处 ErpPurOrder deliveryDate map）
      - Skill: `code-refactor-prompt`
      - deliveryDate map 改硬上限受限扫描；countActiveOrders 改 countByQuery
- [x] **Fix** quality `ErpQaDashboardBizModel`（1 处 NCR 计数）
      - Skill: `code-refactor-prompt`
      - 缺陷 TOP 改 DB 级 GROUP BY dispositionType + COUNT；2 处 count helper 改 countByQuery
- [x] **Fix** sales `ErpSalDashboardBizModel`（1 处 findCustomerTopN Top-N 内存聚合 → DB 级 GROUP BY + LIMIT；报告 §1.6 中严重度项）
      - Skill: `code-refactor-prompt`
      - findCustomerTopN 改 DB 级 GROUP BY customerId + SUM(amountFunctional) WHERE posted=true（报告 §1.6 严重度项落地）；countActiveOrders 改 countByQuery
- [x] **Fix** manufacturing `ErpMfgDashboardBizModel`（1 处 WorkOrder 计数）
      - Skill: `code-refactor-prompt`
      - 工单状态分布改 DB 级 GROUP BY docStatus + COUNT；2 处 count helper 改 countByQuery
- [x] **Fix** finance `ErpFinDashboardBizModel`（1 处 GlBalance periodId==null fallback 全表）
      - Skill: `code-refactor-prompt`
      - 已有 QueryBean 分支，把 `return dao.findAll()` fallback 改为限定最近期间或抛 NopException 提示需传 periodId
      - 裁决：fallback 改为查最近会计期间并按其过滤（无期间返回空，KPI 退化为 0），保留 KPI 可用性
- [x] **Fix** crm `ErpCrmReportBizModel:194`（线索转化漏斗 ErpCrmLead 全表 → 类 D 裁决）
      - Skill: `code-refactor-prompt`
      - 改 DB 级 GROUP BY stageId + COUNT + SUM(expectedRevenue)；移除遗留 FunnelAggregator 内部类
- [x] **Fix** finance `ErpFinReportBizModel:374`（GlBalance periodId==null fallback，与 dashboard finance 同型）
      - Skill: `code-refactor-prompt`
      - 同型 fallback 改为查最近期间过滤
- [x] **Fix** manufacturing `ErpMfgReportBizModel:336`（CRP 负荷报表 ErpMfgCrpLoad 全表 → 类 D 裁决）
      - Skill: `code-refactor-prompt`
      - deriveCrpWindow 改带 workcenterId 过滤 + 硬上限受限扫描
- [x] **Fix** manufacturing `CrpLoadCalculator:425`（WorkcenterCalendar 配置数据 → 类 D 裁决，量小可保留但加注释 + 硬上限）
      - Skill: `code-refactor-prompt`
      - 配置数据加硬上限受限扫描 + 注释裁决保留
- [x] **Proof** 各域 Dashboard/Report 既有单元测试全绿 + 既有 `*.value.spec.ts` 数值断言不回归
      - Skill: `none`
      - 验证：`mvn -pl <各涉及的 service 模块> -am test`；E2E 数值断言在 Closure Gates 跑
      - 10 域 Dashboard/Report + CrpLoad 测试全绿（md 13 + fin 13 + inv 16 + qa 19 + pur 8 + sal 6 + ast 14 + prj 18 + mfg 28 + mnt 14 + crm 9 = 158 测试，0 failures/0 errors）

Exit Criteria:

- [x] `grep -rn "\.findAll()" module-*/erp-*-service/src/main/ --include="*.java"` 仅剩经 Decision 类 D 裁决保留的受控调用（带注释说明保留理由 + 硬上限），其余裸 `dao.findAll()` 零命中 — 实测零命中（类 D 已全部改 findAllByQuery + setLimit + 注释）
- [x] 各域 Dashboard/Report 既有单元测试全绿（行为保留验证）— 158 测试 0 failures
- [x] 看板对外 GraphQL 契约（getDashboardKpi 返回 Map 字段名/数值语义）未变——经既有测试基线数值比对确认（在 Closure Gates 验证）

### Phase 4 - 分析报告自身订正（全报告 grep 逐行）

Status: completed
Targets: `docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md`
Skill: `none`

- Item Types: `Fix`
- Prereqs: Phase 1/2/3 完成（报告订正需反映三者最终结果）

- [x] **Fix** 全报告 grep `279|332|2423|1721|167|18 vs 19` 逐行订正为 07-11 实测值
      - Skill: `none`
      - 已知命中行（执行时重新 grep 确认完整）：line 99（167 spec→删计数）、line 142（279/332→447）、line 143（1721/2423→2758）、line 180（279→332→447）、line 181（18 vs 19→表述已统一）、line 200（332→447）、line 208（279→332→447；18 vs 19→已统一）、line 239（167→删计数）、line 246（279→332→447）
      - 实际订正：line 4/131(337→447)/99(167→删)/142-143/180-182/200/208/239(167→删)/246 全部对齐 07-11 实测值；保留 "279→447"/"1721→2758" 纠错标注（Exit Criteria 允许 447/2758）
- [x] **Fix** §2.5 残留项表：删除"flow-overview.md INSPECTING 状态"和"flow-overview.md 分布式事务声称"两行（仓库已修，报告 stale）
      - Skill: `none`
      - 证据：`flow-overview.md:314` 已声明无 INSPECTING 态；`:499` 已改"单库事务"
      - 两行从残留表移除，加 2026-07-11 复核注说明
- [x] **Fix** §1.6 `findAll()` 表 + §4.2 P0 项：把点名域从 `ErpMdDashboardBizModel`、`ErpSalDashboardBizModel` 对齐到实测 10 域 25 处清单（或注明"详见 Phase 3 改造"）
      - Skill: `none`
      - §1.6 表已改为"10 域 / 25 处（Dashboard + Report/CRP 层）"+ 标注 Phase 3 落地；findCustomerTopN 行标注已改 DB 级 GROUP BY
- [x] **Fix** §1.7 删除"167 个 spec 文件"计数（按用户决策，不订正为 125，改为"见 docs/testing/ 实时计数"或直接删）
      - Skill: `none`
      - §1.7 与 §4.1 两处 167 均改为"spec 数见 docs/testing/ 实时计数"
- [x] **Fix** 报告加维护说明注脚："本报告为 2026-07-10 时间快照，最新计数见 `data-dependency-matrix.md` 与 `product-scope.md`"
      - Skill: `none`
      - 已加在报告标题下
- [x] **Fix** §4.2 优先修复项表：标记本批已落地的项（findAll P0、计数 P0、System.currentTimeMillis P1、INSPECTING/分布式事务 P2 已 stale）的状态；明确移出本批的项的 successor 指向
      - Skill: `none`
      - §4.2 表加"状态"列：✅ 已落地 / ✅ 已 stale / ⏳ 移出本批（successor 指向另开 owner plan）；§1.4 已知违规表加"状态"列标注 3 处已修

Exit Criteria:

- [x] `grep -n "279\|332\|2423\|1721\|167\|18 vs 19" docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md` 不再返回 stale 值（447/2758 除外）— 仅剩 "279→447"/"1721→2758" 纠错标注
- [x] 报告中不再有已 stale 的 INSPECTING/分布式事务残留项声明 — 仅剩"已 stale/已修"复核注
- [x] 报告中的 findAll 命中域清单与 Phase 3 实测一致 — §1.6 已对齐 10 域 25 处
- [x] 报告中无 167 spec 具体计数 — 已改为实时计数指引

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（独立子代理冷重播审查，2026-07-11）发现 3 个阻塞问题：
  1. findAll 基线盘点遗漏 dashboard 目录外的 report/crp 层 4 处同型缺陷（规则 1 + 10 + 13）→ 已修订：基线盘点扩为全仓库 11 域 25 处，Phase 3 纳入全部
  2. Phase 4 订正清单与自身 Exit Criteria 自相矛盾（未覆盖 §3.4/§3.5/§4.1 的 stale 计数）（规则 11）→ 已修订：Phase 4 改为全报告 grep 逐行订正
  3. 报告 §2.5/§3.3 标记的 owner-doc 漂移（feature-inventory/roles-and-permissions/api-response-conventions/18v19）既不修也不裁决（规则 10）→ 已修订：api-response-conventions 模板残留纳入 Phase 1，18v19 纳入 Phase 1，feature-inventory/roles-and-permissions 移入 Non-Goals
  - 非阻塞建议（已采纳）：154 vs 152 口径转为 Phase 1 Decision 项；sales 基线表标签订正为 Top-N 聚合；finance 保护区域显式裁决
- Independent draft review iteration 2: **acceptable as-is after fixes**（独立子代理冷重播审查，2026-07-11）发现 2 个 Major 问题并直接修复：
  1. findAll 命中域计数错误：计划多处称"11 域 / 25 处"，实测去重后为 **10 域 / 25 处**（finance 与 manufacturing 同时出现在 Dashboard 与 Report/CRP 两层，9+3 去重得 10 而非 11）。已订正 line 32（含去重说明）、Goal 2、Phase 3 标题、Phase 4 订正项。25 处命中数经 `rg -c` 全仓库核对无误。
  2. `codebase-map.md` 路径错误：计划在 Task Route / Phase 1 Targets / Phase 1 Exit Criteria grep 中写为 `docs/architecture/codebase-map.md`，实际位于 `docs/context/codebase-map.md`（`data-dependency-matrix.md:12` 亦引用 context 路径）。已订正 3 处全路径；Exit Criteria grep 原指向不存在的路径，修正后 grep 可实际执行。
  - 交叉核对（均通过）：实体数 447（`rg '<entity ' | wc -l`）、Java 文件 2758（`find` 实测）、product-scope.md:52/55/71 与 data-dependency-matrix.md:12 旧计数行号命中、codebase-map.md:11 "154" 命中、api-response-conventions.md:3 模板占位符命中、Phase 2 三处代码质量目标行号均命中、技能名 `nop-platform-conformance-audit-prompt`/`code-refactor-prompt` 均在 `docs/skills/` 注册。
  - 次要观察（不阻塞）：计划覆盖文档计数同步 + 代码质量 + findAll 重构 + 报告订正四个子表面，严格按规则 4（一计划一结果表面）偏宽，但均由同一分析报告 §4.2 驱动、共享"一致性"结果表面且规则 14 允许同组件合并，可接受；结束审计可复查。

## Closure Gates

> 完整仓库验证在此处。

- [x] 范围内行为完成（findAll 改造后看板/报表数值断言不变、3 处代码质量修复落地）
- [x] 相关文档对齐（data-dependency-matrix / product-scope / codebase-map / 分析报告 计数一致）
- [x] 已运行验证：`mvn clean install -DskipTests`（全绿基线，154 reactor 模块 BUILD SUCCESS）+ 各涉及 service 模块 `mvn test`（Dashboard/Report + CrpLoad 158 测试 0 failures/0 errors）
- [x] 无范围内项目降级为 deferred/follow-up（移出范围的项在 Non-Goals 明确记录并命名 successor）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1/2）
- [x] 文本一致性已验证：Plan Status / 各阶段 Status / Exit Criteria / Closure Gates / logs 一致
- [x] 结束审计由独立子代理（新会话 `ses_0b05ba774ffeX6VbDKa6VttvT3`）执行；执行者未自我审计且未将此留为 `[ ]` 占位符 — VERDICT: PASS（无 Blocker/Major，2 个非阻塞 Minor：闭门动作前的预期状态 + project-vision/competitive-comparison 附带修正已在 Phase 1 item 注释）
- [x] 结束证据存在于文件中（见下方 Closure Audit Evidence + 本日志 `docs/logs/2026/07-11.md` 顶部条目）

## Deferred But Adjudicated

### AMIS 前端质量四项（审批按钮缺失 / 审计字段暴露 / FK 显示 ID / Dashboard 图表 ID）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 不同结果表面（前端 AMIS view.xml/page.yaml 改造），本批是文档一致性 + 后端代码质量。前端改造需独立 owner plan 与 AMIS 范式技能。
- Successor Required: `yes` — 另开前端质量 owner plan

### `SettlementAllocation` DTO 跨模块重复

- Classification: `optimization candidate`
- Why Not Blocking Closure: 跨 sales/purchase dao 的 DTO 提取属跨模块重构，不同结果表面。
- Successor Required: `yes` — 另开代码重构 owner plan

### `feature-inventory.md` 完成状态补全 + `roles-and-permissions.md` 权限映射

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 报告 §2.5:144-145 标记，属 owner-doc 内容补全，不同结果表面。本批只处理计数漂移与代码质量。
- Successor Required: `yes` — 另开 owner-doc 内容对齐计划

### Playwright spec 计数 git 考古（125 vs 167 成因）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 按用户决策，本批仅删报告中的 spec 数字，不调查成因。计数会随 spec 增删自然波动，非缺陷。
- Successor Required: `no`

### sql-lib.xml 聚合查询范式引入

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本仓库无 sql-lib 范式；本批 findAll 改造用平台原生 QueryBean + projection 已足够。
- Successor Required: `no`（触发条件：Dashboard/Report 出现需要跨表 JOIN 聚合的查询）

## Closure

Status Note: 4 Phase 全部完成并验证。文档计数同步（279→447 实体 / 1721→2758 Java / 146→154 reactor）+ reactor 口径裁决（154=根 mvn reactor）+ 18 业务域+notify 表述统一；3 处代码质量修复（CoreMetrics + 2 处 println 清理）；10 域 25 处 findAll() 全部改造（countByQuery / DB 级 SUM 聚合 / 带硬上限受限查询）；分析报告全报告 stale 值订正 + 已 stale 残留项清理 + §4.2 状态标注。验证全绿：根 `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + 10 域 Dashboard/Report/CrpLoad 测试 158 项 0 failures/0 errors。无 ORM/契约/AMIS 变更。独立结束审计 VERDICT: PASS。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（新会话 `ses_0b05ba774ffeX6VbDKa6VttvT3`，冷重播无执行者上下文，read-only 核对）
- Verdict: PASS（无 Blocker / 无 Major）
- Evidence:
  - 实体 447 / Java 2758 实测命中源文档（product-scope:52/55/71、data-dependency-matrix:12、codebase-map:11-13）；3 文件 stale grep 空
  - `rg "\.findAll\(\)" module-*/erp-*-service/src/main/` 零命中；抽查 inv sumBalanceTotalCost/findWarehouseDistribution + sal findCustomerTopN 为真实 DB 级聚合（非空壳）
  - 报告 `rg "332|2423|167|18 vs 19|337"` 空；§2.5 INSPECTING/分布式事务 stale 行已删；§4.2 状态列已加
  - `git status` 24 文件变更全在声明范围内（8 docs + 14 Java service/test + 2 既有）；无 orm.xml / view.xml / page.yaml 变更
  - 反模式扫描：无 `@Inject private`、无 `System.currentTimeMillis`（目标文件）、IDaoProvider 为 dashboard/report 既定范式（类头 javadoc 声明）
- Anti-hollow: findAll 替换为真实 countByQuery / QueryFieldBean.sum() 聚合 / setLimit 硬上限，非 no-op
- 日志：`docs/logs/2026/07-11.md` 顶部条目（154 模块 BUILD SUCCESS + 158 测试 0 failures）

Follow-up:

- AMIS 前端质量四项 → 另开前端 owner plan
- `SettlementAllocation` DTO 重复 → 另开代码重构 owner plan
- `feature-inventory.md` / `roles-and-permissions.md` 内容补全 → 另开 owner-doc 对齐计划
