# 2026-08-29-1913-2-page-graphql-to-rest-migration-phase2 剩余 58 个页面 /graphql 迁移

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Source: docs/plans/2026-08-29-1913-1-page-graphql-to-rest-migration.md (Deferred But Adjudicated)
> Related: 2026-08-29-1913-1-page-graphql-to-rest-migration
> Audit: required

## Current Baseline

- **已完成**：Phase 1 迁移 16 文件（inventory, sales, purchase, master-data, notify, logistics, drp, contract, aps 9 模块）✅
- **本计划范围**：剩余 58 文件（10 模块）
- **模块分布**：
  - finance: 14 文件
  - crm: 7 文件
  - quality: 6 文件
  - projects: 6 文件
  - hr: 5 文件
  - assets: 5 文件
  - manufacturing: 5 文件
  - maintenance: 4 文件
  - cs: 4 文件
  - b2b: 2 文件
- **> 实仓重盘（2026-09-01，执行时按 1913-1 结束审计修正要求执行）**：1913-1 审计 finding 3 指出 commit `751749e17`（plan 2026-08-30-2238-1 F2 修复批）**已实际迁移全部 74 个 `.page.yaml`**——本计划 target 大概率 complete-on-arrival。执行时实仓复核证实：全仓 `-g '*.page.yaml'` 零 `/graphql`（含 `url: /graphql`）残留；10 模块 `@query:`/`@mutation:` 文件计数与本计划分布**逐一精确吻合**（fin 14 / crm 7 / qa 6 / prj 6 / hr 5 / ast 5 / mfg 5 / mnt 4 / cs 4 / b2b 2 = 58）。本计划按 complete-on-arrival 处置：执行工作 = 实仓复核 + 验证取证 + 收尾，非代码迁移。

## Goals

- 将剩余 58 个 `.page.yaml` 文件中的 `/graphql` 调用迁移到 `@query:`/`@mutation:` 前缀
- 确保全部 74 个页面符合 flux-only 约定

## Non-Goals

- 不修改 `view.xml` 文件
- 不修改测试文件中的 `/graphql`
- 不修改报表下载按钮（已修复为 `/p/` 路径）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/view-and-page-strategy.md`
- Skill Selection Basis: `nop-frontend-dev`

## Execution Plan

### Phase 1 - 按模块批量迁移

Status: completed
Targets: `module-{finance,crm,quality,projects,hr,assets,manufacturing,maintenance,cs,b2b}/erp-*-web/src/main/resources/_vfs/erp/*/pages/**/*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1 (1913-1) 完成

- [x] **Finance 模块**（14 文件）
- [x] **CRM 模块**（7 文件）
- [x] **Quality 模块**（6 文件）
- [x] **Projects 模块**（6 文件）
- [x] **HR 模块**（5 文件）
- [x] **Assets 模块**（5 文件）
- [x] **Manufacturing 模块**（5 文件）
- [x] **Maintenance 模块**（4 文件）
- [x] **CS 模块**（4 文件）
- [x] **B2B 模块**（2 文件）

Exit Criteria:

- [x] 所有 58 个文件的 `/graphql` 调用已迁移到 `@query:`/`@mutation:`
- [x] 全仓库 `grep "url: /graphql"` 返回 0

Execution Evidence (2026-09-01，complete-on-arrival 复核闭合)：

- **迁移批次归属**：全部 58 文件由 commit `751749e17`（2026-08-31，plan 2026-08-30-2238-1 F2 flux 页面编译修复批）随 AMIS→flux 重写一并落地，非本执行产生代码变更；本执行在 HEAD `bfbbc3b97` 实仓复核闭合。
- **残留检查**：`rg 'url: */graphql' module-*/src/main/resources -g '*.page.yaml'` 0 命中；全仓（含 tests/src，排除 target）`rg '/graphql' -g '*.page.yaml'` 0 命中——10 模块及全仓 `.page.yaml` 零 `/graphql` 残留。
- **逐模块计数吻合**：10 模块含 `@query:`/`@mutation:` 的 `.page.yaml` 文件数 = fin 14 / crm 7 / qa 6 / prj 6 / hr 5 / ast 5 / mfg 5 / mnt 4 / cs 4 / b2b 2，与本计划 Baseline 分布逐一精确相等（合计 58/58）。
- **目标方法存在性**：58 文件去重提取 97 个 `@query:`/`@mutation:` 目标；60 个自定义方法在对应 service 层源码（BizModel/xbiz）字面定位命中；37 个平台标准 CRUD（30 `*__findPage` + 5 `*__get` + 2 `*__save`）全部经对应 `*BizModel extends AbstractErpCrudBizModel<T>`（其 `extends CrudBizModel`，module-common-service）类确认由平台提供（37/37）。97/97 目标全部可达（split 算术经结束审计独立复核修正）。
- **YAML 解析**：58/58 文件 PyYAML safe_load 解析 56 通过；2 个「失败」（fin period-close-wizard:164 / qa dashboard:197）为 PyYAML 对 `${...}` 模板表达式内嵌引号的严格性差异（两行均由 `751749e17` 引入，该 commit 自带 erp/* 855 页 flux 编译 0 error 验证），以权威运行时解析器 `ErpAllFluxPagesTest`（PageProvider.getPage flux 模式逐页加载）复核通过为准（见 Phase 2）。

### Phase 2 - 验证

Status: completed
Targets: 全仓库
Skill: none

- Item Types: `Proof`

- [x] `grep -r "url: /graphql" module-*/src/main/resources --include="*.page.yaml"` 返回 0
- [x] 抽样验证 3-5 个代表性页面渲染正常

Execution Evidence (2026-09-01)：

- grep 残留检查：`rg 'url: */graphql' module-*/src/main/resources -g '*.page.yaml'` 与全仓 `rg '/graphql' -g '*.page.yaml'`（排除 target）均 0 命中。
- **行为级全量验证（强于抽样）**：`mvn clean install -DskipTests` BUILD SUCCESS；`mvn test -pl app-erp-all -Dtest='ErpAllFluxPagesTest,ErpAllFluxPagesExportTest'` 2/2 绿、`FLUX_PAGE_ERROR_COUNT: 0`——flux 渲染模式下 `PageProvider.getPage` 逐页加载 erp/* 全量页面（含本计划全部 58 文件）编译渲染 0 error。
- 静态抽样 5 个代表性页面（跨复杂度谱系）：① fin `period-close-wizard/main.page.yaml`（复杂向导，8 目标含 `closePeriod`/`finalizePeriod`/`reverseClose` 自定义 mutation）；② qa `dashboard/main.page.yaml`（SPC 看板，6 目标 `ErpQaDashboard__*`）；③ crm `ErpCrmLead/opportunity-kanban.page.yaml`（kanban，`moveStage` mutation）；④ hr `dashboard/org-chart.page.yaml`（组织架构图）；⑤ b2b `dashboard/edi-detail.page.yaml`（EDI 报文详情）——`@query:`/`@mutation:` 前缀全部正确，目标方法全部存在（含 CrudBizModel 继承路径）。
- 全 reactor `mvn test -fae`：139 模块 SUCCESS；2 模块 13 errors 全部为**预存已知**月初冻结时钟时间炸弹（ast `TestErpAstMaintenance` 5 + cs `TestErpCsCatalogFulfillmentEngine` 1 / `TestErpCsTicketCreateEnrichment` 7），与 bug record `docs/bugs/2026-09-01-0058-frozen-clock-escape-ast-period-cs-ticket-code.md`（**open**，successor 独立切片）逐一精确吻合，与本计划零因果（本执行零代码变更，HEAD 代码与 2026-08-31 全绿基线 `211572283` 一致，`bfbbc3b97` 仅 docs 差异）；`-fae` 跳过的 15 个下游依赖模块（prj/pur/ast web+app、fin-app、cs web+app、drp service+web+app、app-erp-all）补跑 BUILD SUCCESS 全绿（app-erp-all 全量集成 69 用例含 flux 双测）。

## Closure Gates

- [x] 范围内行为完成
- [x] 已运行验证（grep 检查）
- [x] 独立结束审计完成

## Closure

Status Note: 本计划为 complete-on-arrival 闭合——target 范围（10 模块 58 个 `.page.yaml` 的 `/graphql` → `@query:`/`@mutation:`）已由 commit `751749e17`（plan 2026-08-30-2238-1 F2 批）随 AMIS→flux 重写先行落地；本执行在 HEAD `bfbbc3b97` 完成实仓复核、验证取证与收尾：全仓 `.page.yaml` 零 `/graphql` 残留、58 文件逐模块计数与本计划分布精确吻合、97/97 目标方法可达、flux 全量页面编译 `FLUX_PAGE_ERROR_COUNT: 0`、全 reactor `-fae` 139 模块 SUCCESS（唯一失败 = 预存 ast/cs 冻结时钟时间炸弹 13 errors，bug record `2026-09-01-0058` open，零代码因果）+ 15 跳过下游模块补跑全绿。本执行零代码变更。Mission 收尾 4b/4c：front matter 无 `> Work Item:` 与 `> Source Audits:` 行；`> Source:` 指向的 1913-1 Deferred「后续模块的 /graphql 迁移」项已补 successor-completed 指针。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent（fresh session，task id `ses_fa7045e19ffebFR99kcmU6VOVr`）
- Evidence: **VERDICT: APPROVE**（2026-09-01）。审计员独立重放：全仓 `.page.yaml` grep 0 残留（856 文件扫描）；10 模块 `@query:`/`@mutation:` 计数 14/7/6/6/5/5/5/4/4/2 = 58 精确吻合 + 全仓 74 文件口径一致；97 目标抽样定位（60/60 自定义字面命中 + 30/30 findPage 经 `AbstractErpCrudBizModel extends CrudBizModel` 链确认）；PyYAML 56/58 与声称一致（2 处 `${...}` 引号转义严格性差异，`751749e17` blame 佐证）；工作树零代码变更（仅本 plan 文档）；`git diff 211572283..HEAD` docs-only；复跑 scoped flux 双测 2/2 绿 `FLUX_PAGE_ERROR_COUNT: 0`（erpPages=855 failed=0）；全 reactor 13 errors 与 bug record 0058 逐项吻合。3 MINOR findings（Closure Gates 未随证据即时勾选 / 目标 split 算术 72+25→60+37 文本修正 / 1913-1 Deferred 项缺 successor-completed 指针）均非阻塞，处置已全部落地。