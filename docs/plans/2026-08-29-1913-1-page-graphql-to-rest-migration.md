# 2026-08-29-1913-1-page-graphql-to-rest-migration 页面 /graphql 调用迁移到 REST /r/

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Source: docs/architecture/view-and-page-strategy.md §页面数据访问（/r/ REST 约定，强制）
> Related: 2026-08-03-1232-1-flux-crud-migration（AMIS→flux 迁移）
> Audit: required

## Current Baseline

- **view.xml 文件**：codegen 生成，全部使用 `@query:` 和 `@mutation:` 前缀 ✅
- **.page.yaml 文件**：手写，仍有大量使用 `url: /graphql` ❌
- **涉及模块**：19 个模块（inventory, finance, crm, sales, purchase, manufacturing, maintenance, projects, aps, b2b, drp, contract, notify, assets, cs, hr, logistics, quality, master-data）
- **涉及文件数量**：74 个 `.page.yaml` 源文件（不含 target 目录）
- **模块分布**：
  - finance: 28 文件
  - crm: 14 文件
  - quality: 12 文件
  - projects: 12 文件
  - master-data: 10 文件
  - manufacturing: 10 文件
  - hr: 10 文件
  - assets: 10 文件
  - maintenance: 8 文件
  - cs: 8 文件
  - inventory: 6 文件
  - purchase: 4 文件
  - b2b: 4 文件
  - sales: 2 文件
  - notify: 2 文件
  - logistics: 2 文件
  - drp: 2 文件
  - contract: 2 文件
  - aps: 2 文件
- **flux-only 约定**：docs/architecture/view-and-page-strategy.md 第 61-76 行明确约定 flux 模式下页面数据访问不使用 GraphQL，全部经 REST `/r/` 端点
- **AMIS 残留背景**：74 个文件均为 AMIS 格式（`type: crud`/`type: form`/`type: page`），非 flux 格式（`type: data-source` + `selection` + `crud source`）。按 `docs/architecture/view-and-page-strategy.md` 第 53-59 行约定，AMIS 残留属迁移期历史，按 `2026-08-03-1232-{1,2,3,4,5}` 计划分阶段重写。`2026-08-03-1232-3` 已完成部分复杂页重写（如 dashboard），但仍有大量页面未覆盖。

## 工作量评估与拆分建议

**关键发现**：74 个文件均为 AMIS 格式，不能仅通过修改 URL 实现迁移。需要：
1. 将 AMIS `type: crud` 改为 flux `data-source` + `crud source` 模式
2. 将 AMIS `type: form` action 改为 flux `data-source` + `button onClick` 模式
3. 移除 GraphQL `query`/`variables`/`adaptor` 包装，改用 `selection` 字段过滤
4. 处理 `dataType: raw` 特殊处理（`$var` 转义问题按 `view-and-page-strategy.md` §115 解决）

**工作量估算**：
- 简单迁移（仅 URL 替换）：约 10-20 文件（无 adaptor、简单 selection 的场景）
- 中等迁移（需重写 data-source + crud）：约 30-40 文件
- 复杂迁移（向导、聚合查询、多步骤）：约 15-25 文件（含甘特、向导、特殊控件）

**建议拆分方案**（避免单计划过广）：
- **本计划范围**（核心 + 高频使用模块）：inventory, sales, purchase, master-data, notify, logistics, drp, contract, aps（约 26 文件）
- **后续计划 1**（复杂业务）：finance, crm（约 42 文件，含向导）
- **后续计划 2**（运营成熟度）：quality, projects, hr, assets, cs（约 50 文件）
- **后续计划 3**（制造运维）：manufacturing, maintenance（约 18 文件）

**风险**：
- 部分页面使用 GraphQL Map 返回类型（无 selection set），需后端 `@BizQuery` 增加 `selection` 支持
- 部分页面使用 `dataType: raw` + `${'$'}` 转义，迁移时需确保参数类型推断正确
- 向导类页面（period-close-wizard, visit-wizard）已由 `2026-08-03-1232-3` 重写，需验证是否还有 `/graphql` 残留

## Goals

- **本计划范围**：迁移核心 + 高频使用模块的 `.page.yaml` 文件（inventory, sales, purchase, master-data, notify, logistics, drp, contract, aps）
- 将范围内的 `.page.yaml` 文件中的 `/graphql` 调用迁移到 `@query:`/`@mutation:` 前缀
- 确保范围内页面符合 flux-only 约定，不使用 GraphQL
- 保持功能不变，仅改变数据访问方式

## Non-Goals

- 不修改 `view.xml` 文件（已符合约定）
- 不修改测试文件（`tests/e2e/`）中的 `/graphql` 调用
- 不修改报表下载按钮（已修复为 `/p/` 路径）
- **后续计划范围**（明确排除，待 successor 计划覆盖）：
  - Finance 模块（28 文件，含 period-close-wizard 等复杂向导）
  - CRM 模块（14 文件，含 kanban/calendar/timeline）
  - Quality 模块（12 文件，含 SPC 图表）
  - Projects 模块（12 文件）
  - Manufacturing 模块（10 文件，含 BOM 树）
  - HR 模块（10 文件，含组织架构图）
  - Assets 模块（10 文件，含资产盘点向导）
  - Maintenance 模块（8 文件，含 visit-wizard 向导）
  - CS 模块（8 文件，含 kanban/timeline）
  - B2B 模块（4 文件，含 ASN 流程条/EDI 报文）

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/view-and-page-strategy.md`
- Skill Selection Basis: `nop-frontend-dev`（页面开发技能）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline
- 需要理解每个 `.page.yaml` 文件中 `/graphql` 的使用场景

## Execution Plan

### Phase 1 - 本计划范围内模块迁移

Status: completed
Targets: `module-{inventory,sales,purchase,master-data,notify,logistics,drp,contract,aps}/erp-*-web/src/main/resources/_vfs/erp/*/pages/**/*.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Fix | Decision`
- Prereqs: 无

- [x] **Inventory 模块**（3 文件，10 处 `/graphql`）
  - [x] `module-inventory/erp-inv-web/.../stock-take-flow/main.page.yaml`（4 处）
  - [x] `module-inventory/erp-inv-web/.../dashboard/main.page.yaml`（6 处）
  - [x] `module-inventory/erp-inv-web/.../report/inventory-trace-report.page.yaml`（1 处）
  - Skill: `nop-frontend-dev`

- [x] **Master-Data 模块**（5 文件，5 处 `/graphql`）
  - [x] `module-master-data/erp-md-web/.../cost-center/main.page.yaml`（1 处）
  - [x] `module-master-data/erp-md-web/.../dashboard/main.page.yaml`（1 处）
  - [x] `module-master-data/erp-md-web/.../party-search/main.picker.page.yaml`（1 处）
  - [x] `module-master-data/erp-md-web/.../report/material-price-list.page.yaml`（1 处）
  - [x] `module-master-data/erp-md-web/.../report/partner-list.page.yaml`（1 处）
  - Skill: `nop-frontend-dev`

- [x] **Purchase 模块**（2 文件，9 处 `/graphql`）
  - [x] `module-purchase/erp-pur-web/.../dashboard/main.page.yaml`（5 处）
  - [x] `module-purchase/erp-pur-web/.../dashboard/three-way-match.page.yaml`（4 处）
  - Skill: `nop-frontend-dev`

- [x] **Sales 模块**（1 文件，4 处 `/graphql`）
  - [x] `module-sales/erp-sal-web/.../dashboard/main.page.yaml`（4 处）
  - Skill: `nop-frontend-dev`

- [x] **Notify 模块**（1 文件，6 处 `/graphql`）
  - [x] `module-notify/erp-notify-web/.../ErpSysNotification/inbox.page.yaml`（6 处）
  - Skill: `nop-frontend-dev`
  - > **交接注记（2026-08-30，plan 2026-08-30-1126-1 结束审计整改）**：本文件当时携带 3 处同级 `then:` 键重复（duplicate-key，页面无法解析），已由 plan 2026-08-30-1126-1 的结束审计整改按嵌套 then 链规范修复（见该计划「结束审计整改」节）；本计划继续处理本文件的 `/graphql` → REST 迁移时基于已修复版本。

- [x] **Logistics 模块**（1 文件，1 处 `/graphql`）
  - [x] `module-logistics/erp-log-web/.../dashboard/shipment-tracking.page.yaml`（1 处）
  - Skill: `nop-frontend-dev`

- [x] **DRP 模块**（1 文件，1 处 `/graphql`）
  - [x] `module-drp/erp-drp-web/.../dashboard/net-requirement.page.yaml`（1 处）
  - Skill: `nop-frontend-dev`

- [x] **Contract 模块**（1 文件，2 处 `/graphql`）
  - [x] `module-contract/erp-ct-web/.../dashboard/version-diff.page.yaml`（2 处）
  - Skill: `nop-frontend-dev`

- [x] **APS 模块**（1 文件，1 处 `/graphql`）
  - [x] `module-aps/erp-aps-web/.../dashboard/schedule-gantt.page.yaml`（1 处）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 本计划范围内（9 模块，16 文件）的 `/graphql` 调用已迁移到 `@query:`/`@mutation:` 或 `/r/`
- [x] 功能保持不变，仅改变数据访问方式
- [x] 无新增 `/graphql` 调用

Execution Evidence (2026-09-01 复核闭合)：

- 迁移批次由前次中断执行落地，随 commit `751749e17`（plan 2026-08-30-2238-1 F2 修复批）入库；本执行对 HEAD `211572283` 实仓复核：范围内 9 模块全部 `.page.yaml` 零 `/graphql` 残留（grep 实证，含 `xmlns:graphql` 命名空间声明非调用），16 文件全部使用 `@query:`/`@mutation:` 前缀且 YAML 解析 0 错误；42 个去重 biz 方法目标全部存在于 service 层（`findPage`/`get` 等标准 CRUD 由 CrudBizModel 提供）。
- 范围外新发现：master-data 4 个手写 `.view.xml`（ErpMdSubject/ErpMdMaterial/ErpMdOrganization/ErpMdPartner）仍含 7 处 `/graphql`（AMIS 残留 gen-control 业务按钮），属本计划 Non-Goals 排除面，已登记 Deferred But Adjudicated。

### Phase 2 - 验证与回归测试

Status: completed
Targets: `tests/e2e/`
Skill: `nop-frontend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1 完成

- [x] 运行 `mvn clean install -DskipTests` 确保构建通过
  - Skill: none
- [x] 抽样验证迁移后的页面功能正常（选择 3-5 个代表性页面）
  - Skill: `nop-frontend-dev`
- [x] 确认本计划范围内无 `/graphql` 调用残留（grep 检查）
  - Skill: none

Exit Criteria:

- [x] 构建通过
- [x] 抽样页面功能正常
- [x] 本计划范围内无 `/graphql` 残留

Execution Evidence (2026-09-01)：

- `mvn clean install -DskipTests`：BUILD SUCCESS（156 reactor 模块，01:45 min，m2 已刷新后复跑 scoped 测试）。
- 行为级抽样验证：`ErpAllFluxPagesExportTest` + `ErpAllFluxPagesTest` 2/2 绿、`FLUX_PAGE_ERROR_COUNT: 0`（erp/* 全量页面 flux 编译含本计划 16 文件）；静态抽样 inventory dashboard / purchase three-way-match / notify inbox / aps schedule-gantt / drp net-requirement 的 `@query:`/`@mutation:` 目标方法全部存在于对应 BizModel。
- grep 残留检查：9 模块 `*.page.yaml` 零 `/graphql`（唯一残留为 Non-Goals 排除的 `.view.xml`，见 Deferred But Adjudicated）。
- 全 reactor `mvn test -fae`：139 模块 SUCCESS；13 errors 全部为月初时钟滚动时间炸弹（ast `TestErpAstMaintenance` 5 + cs `TestErpCsCatalogFulfillmentEngine` 1 + cs `TestErpCsTicketCreateEnrichment` 7，日历派生值 2026-08→2026-09 失配），与本计划零因果（HEAD 与 2026-08-31 全绿基线记录同 commit `211572283`，本执行零代码变更）；`-fae` 跳过的依赖模块补跑 pur/prj/drp service 98/98 绿。登记 `docs/bugs/2026-09-01-0058-frozen-clock-escape-ast-period-cs-ticket-code.md`。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fb2c58ea4ffe0qwHuf7D9pK8za) because 遗漏 6 个模块（assets, cs, hr, logistics, quality, master-data），文件计数不准确，Draft Review Record 为空
- Independent draft review iteration 2: acceptable as-is after 补充遗漏的 6 个模块，修正所有模块的文件计数，更新 Draft Review Record

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：在结束时运行 `typecheck`/`build`/`lint`/`test`（或项目等效命令）一次。不要在阶段退出标准中重复这些 — 阶段仅验证其交付的内容以及解除后续阶段阻塞的内容（见执行时规则 7）。对于无代码更改的计划（仅文档），删除验证命令门控并说明原因。

- [x] 范围内行为完成
- [x] 相关文档对齐
- [x] 已运行验证（`mvn clean install -DskipTests` BUILD SUCCESS 156 模块 01:45 + `ErpAllFluxPagesExportTest`/`ErpAllFluxPagesTest` 2/2 绿 FLUX_PAGE_ERROR_COUNT:0 + 全 reactor `mvn test -fae` 139 模块 SUCCESS，13 errors 为月初时钟时间炸弹预存类已登记 bug record，跳过依赖模块补跑 98/98 绿）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 测试文件中的 /graphql 调用

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 测试文件中的 `/graphql` 调用是测试基础设施的一部分，与页面数据访问约定无关
- Successor Required: `no`

### 后续模块的 /graphql 迁移

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 74 个文件中，本计划范围仅 16 文件；其余 58 文件（finance 28、crm 14、quality 12、projects 12、manufacturing 10、hr 10、assets 10、maintenance 8、cs 8、b2b 4）涉及复杂向导、kanban、SPC、BOM 树等场景，需独立计划覆盖
- Successor Required: `yes`
- Successor 计划：一次性迁移剩余 58 文件（10 模块：finance/crm/quality/projects/manufacturing/hr/assets/maintenance/cs/b2b），计划名 `2026-08-29-1913-2-page-graphql-to-rest-migration-phase2`
- > **审计修正（2026-09-01，独立结束审计 finding 3）**：commit `751749e17` 实际迁移了**全部 74 个** `.page.yaml`（含上述 Non-Goals 列名模块；实仓 grep 全模块 `.page.yaml` 零 `/graphql` 残留佐证），本项 target 已清空（complete-on-arrival）。successor `2026-08-29-1913-2` 起草时须按计划指南最低规则 1 重新盘点实仓基线（大概率 supersede 或改立案），不得沿用本计划的 per-module 文件计数（审计 finding 1：Baseline/Non-Goals 各模块计数虚增恰 2×， operative 总数 74/16/58 亦需重验）。

### master-data 手写 view.xml 的 /graphql 残留（AMIS 残留 gen-control 业务按钮）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划 Targets 仅 `**/*.page.yaml`，Non-Goals 明确排除 view.xml（计划基线「view.xml 已全部符合约定」的断言对 4 个手写 Delta view.xml 不成立，系基线盘点误差非实现缺口）。7 处 `/graphql` 位于 AMIS 残留 gen-control 业务按钮（ErpMdSubject×2 code 唯一性校验、ErpMdMaterial×3、ErpMdOrganization×1 删除引用阻断、ErpMdPartner×1），属 AMIS 残留页迁移期历史（owner doc `view-and-page-strategy.md` §53-59，由 `2026-08-03-1232-*` 系列重写）；且 owner doc §119 明确警告此类场景勿机械替换为 `@query:`（`guessDefinition` 对 Long 参数推断 Int 被 GraphQL 校验拒绝），迁移须随页重写按 REST `/r/` 显式路径设计，故不在本计划内 drive-by。
- Successor Required: `yes`
- Successor 计划：随 master-data 相关页 AMIS→flux 重写（`2026-08-03-1232-*` 系列）落地；若 1232 系列不覆盖，由 `2026-08-29-1913-2` 范围扩展裁决

## Closure

Status Note: 本计划范围（9 模块 16 个 `.page.yaml` 的 `/graphql` → `@query:`/`@mutation:` REST 迁移）已全部落地并经 HEAD `211572283` 实仓复核：范围内零 `/graphql` 残留、16 文件 YAML 解析 0 错误、42 个 biz 方法目标全部存在、全量页面 flux 编译 0 error、全量构建 156 模块 BUILD SUCCESS。迁移批次由前次中断执行落地（随 commit `751749e17` 入库），本执行完成复核、证据落盘与范围外发现登记（master-data view.xml 残留 → Deferred；ast/cs 月初时钟时间炸弹 → bug record `docs/bugs/2026-09-01-0058`）。Owner doc `view-and-page-strategy.md` §页面数据访问 与实现一致，无文档更新义务。Mission 收尾步骤 4b/4c：本计划 front matter 无 `> Work Item:` 标签、无 `> Source Audits:` 行，`> Source:` 指向的 architecture doc 无 ❌/✅ 工作项清单——两项均为 no-op。

Closure Audit Evidence:

- Auditor / Agent: independent closure audit subagent（fresh session，task id `ses_fa731ffc8ffeonogU9YtXFDZXS`）
- Evidence: **VERDICT: APPROVE**（2026-09-01）。审计员独立重放：范围内 `.page.yaml` grep 0 残留（无 include 过滤 7 处全部落在本计划 Non-Goals 的 4 个 master-data view.xml + 无害 xmlns:graphql 命名空间声明）；4 文件抽样确认 `@query:`/`@mutation:`；5 个 biz 方法目标在 BizModel 源码逐一定位（含 CrudBizModel 继承路径）；16/16 YAML 解析通过；bug record 与日志存在性确认；文本一致性（41 `[x]` + 唯一未勾项=审计门控本身）；`751749e17^` git 溯源确认 16/58 文件划分与计数算术；工作树零代码变更。3 MINOR findings（per-module 计数虚增 2× / Phase 1 处数 slip / successor target 已被 `751749e17` 清空为 complete-on-arrival）均非阻塞，修正注记已按审计处置建议落盘 Deferred 节，并要求 successor `2026-08-29-1913-2` 起草时重盘实仓基线。

Follow-up:

- successor 计划 `2026-08-29-1913-2`（剩余 58 文件 10 模块）现在可启动（其 Prereqs「Phase 1 (1913-1) 完成」已满足）。
- master-data 4 个手写 view.xml 的 7 处 `/graphql`（Deferred 节已登记 successor 归属）。
- ast/cs 冻结时钟逃逸修复切片（bug record `docs/bugs/2026-09-01-0058-frozen-clock-escape-ast-period-cs-ticket-code.md`，open）。
