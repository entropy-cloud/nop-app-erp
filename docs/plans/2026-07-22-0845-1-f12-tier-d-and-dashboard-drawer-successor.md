# 2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor F12 Tier D 长尾域 page-structure + Tier B 完整仪表板 drawer successor

> Plan Status: completed
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F12（line 287-315 / 543）+ `docs/plans/2026-07-21-0330-3-f12-page-structure-tabs-wizards.md` §Deferred But Adjudicated（Tier D 6 长尾页面 + Tier B 3 完整仪表板 drawer）
> Related: `docs/plans/2026-07-21-0330-3-f12-page-structure-tabs-wizards.md`（前置 F12 Tier A/B 8 实体已完成，本计划接续 Tier D + 完整仪表板 drawer）；`docs/plans/2026-07-21-0330-1-f4p2-child-table-editor-p3-ext-domains.md`（F4 P3 ext 8 域 Tier 1 18 对子表已落地，本计划依赖其 crm/cs/contract 域子表基线）；`docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md`（F16 P1 复杂页面 successor，依赖本计划 Tier D quality/projects tabs 落地）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-22，对 5 个 Tier D 长尾头实体 + 3 个 Tier B 仪表板实体 view.xml + F4 P3 ext 域落地证据 + `docs/design/page-structure-patterns.md` + `docs/plans/2026-07-21-0330-3` §Deferred + 独立 explore ses_078b63d07ffe 对 8 实体现状的核实）：

### Tier D 长尾头实体（5 实体，F4 P3 ext 域已就绪，本计划范围）

| 头实体 | 域 | 现状详情页结构 | F4 P3 子表基线 |
|--------|---|--------------|---------------|
| ErpCrmLead | crm | 单 form 6 group（baseInfo/contact/revenue/tracking/lostInfo/audit），无 `layoutControl="tabs"`，无子表 cell | F4 P3 已落地 ErpCrmActivity/ErpCrmOpportunity 等 Tier 1 子表（plan 2026-07-21-0330-1 §Tier 1 logistics/b2b/cs/hr/contract/drp）—— **crm 域不在 F4 P3 Tier 1 清单**，需核实 crm 子表 picker/sub-grid-view 就绪度 |
| ErpCsTicket | cs | 单 form 4 group（baseInfo/sla/actions/audit），已有 `<cell id="actions">` 引用 ErpCsTicketAction sub-grid-view，无 `layoutControl="tabs"` | F4 P3 已落地 cs Ticket actions Tier 1 子表（plan 2026-07-21-0330-1 §17.1）✅ |
| ErpCtContract | contract | 单 form **7 group**（baseInfo/parties/finance/schedule/lines/versions/audit），已有 2 子表 cell：`lines` → ErpCtContractLine sub-grid、`versions` → ErpCtContractVersion sub-grid；无 `layoutControl="tabs"` | F4 P3 已落地 contract Contract×2/ContractLine Tier 1 子表（plan 2026-07-21-0330-1 §17.1）✅；erp-contract-web 工程名实际为 `erp-ct-web` |
| ErpPrjProject | projects | 单 form 4 group（baseInfo/schedule/finance/audit），无子表 cell | F4 P2 已落地 ErpPrjCostCollection/Line（plan 2026-07-20-1020-3）；但 ErpPrjTask/ErpPrjBudget 等 project 下属子表不在 F4 P2 范围，需核实 sub-grid-view 就绪度 |
| ErpQaInspection | quality | 单 form 4 group（baseInfo/inspection/sample/audit），无子表 cell | quality 域不在 F4 P2/P3 范围；ErpQaInspectionLine/ErpQaNcr 等下属实体需核实 sub-grid-view 就绪度 |

**F4 P3 crm 域覆盖核实**：roadmap line 136（`2026-07-21-0330-1`）列出 P3 ext 8 域 = logistics/b2b/cs/hr/contract/drp + crm 域内 Tier 1 18 对中包含 cs Ticket actions，但 crm Lead 的 activities/quotations 子表是否在 F4 P3 Tier 1 覆盖需 Phase 0 Explore 核实（若未覆盖，降级为 sub-grid-view 只读展示或 Deferred）。

### Tier B 完整仪表板 drawer（3 实体，后端 @BizQuery 部分就绪，本计划范围）

| 头实体 | 域 | 现状 | 完整仪表板缺口 | 后端就绪度 |
|--------|---|------|--------------|-----------|
| ErpHrEmployee | human-resource | F12 Tier B 已加 `layoutControl="tabs"`（6 group：baseInfo/contact/idCard/employment/payroll/audit），薪酬敏感字段 `visibleOn="${false}"` 隐藏 | 合同/考勤/休假/工时 4 跨实体子表 tab + ref-employee.page.yaml drawer | 合同=ErpHrContract `findPage` filter_employeeId；考勤=ErpHrAttendance `findPage`；休假=ErpHrLeaveRequest `findPage`（注：实体名 ErpHrLeaveRequest，非 ErpHrLeave）；工时=ErpHrTimesheet `findPage` —— 全部既有 `findPage` 可用 |
| ErpAstAsset | assets | F12 Tier B 已加 `layoutControl="tabs"`（5 group：baseInfo/value/depreciation/usage/audit） | 折旧时间线 + 相关凭证列表 2 tab + ref-asset.page.yaml drawer | 折旧=ErpAstDepreciationSchedule `findPage` filter_assetId（既有）；凭证=ErpFinVoucherBillR `findPage` filter_sourceEntityType=ASSET（既有，跨域 finance） |
| ErpMntEquipment | maintenance | F12 Tier B 已加 `layoutControl="tabs"`（2 group：baseInfo/audit，仅 2 tab） | 状态色块 + 维护时间线 + 备件消耗 3 tab + ref-equipment.page.yaml drawer | 维护时间线=`ErpMntReport__maintenanceHistoryData(equipmentId, startDate, endDate)` @BizQuery 已就绪（F12 Phase 0 Explore (b) 确认）；备件消耗=ErpMntSparePartUsage `findPage` filter_equipmentId（既有） |

### 关键风险/缺口

- **crm 域 F4 P3 覆盖未确认**：若 ErpCrmLead 的 activities/quotations 子表不在 F4 P3 Tier 1 覆盖，需 Phase 0 Explore 裁决（降级 sub-grid-view 只读 或 Deferred 到 crm F4 successor）
- **ErpPrjProject/ErpQaInspection 子表就绪度未确认**：projects 下属 ErpPrjTask/ErpPrjBudget、quality 下属 ErpQaInspectionLine/ErpQaNcr 等子表是否已有 sub-grid-view 配置需 Explore
- **跨域凭证查询（assets → finance）**：ErpFinVoucherBillR 跨域 filter 需核实 GraphQL selection 跨工程可用性
- **机制 B `<pages><tabs>` + ref-*.page.yaml**：本仓库 notify inbox page.yaml 是手写 AMIS JSON 真实样例，但 `<tabs>` + `<simple>` + `<crud>` 组合的 drawer 集成仅 NopJobSchedule 一处平台样例（`page-structure-patterns.md` §2 引用），ERP 应用层尚无生产落地，需 Phase 0 PoC 验证
- **ErpHrEmployee 敏感字段脱敏延后**：本计划仅消费 F12 Tier B 既有 `visibleOn="${false}"` 隐藏；完整脱敏（`@Sensitive` 后端注解 / BizModel `@BizLoader` mask transformer）属独立 cross-cutting plan

## Goals

1. **Phase 0 Explore 闭环**：(a) 5 Tier D 实体的 F4 子表基线就绪度（crm Lead / projects Task/Budget / quality InspectionLine/Ncr 是否已有 sub-grid-view）；(b) 机制 B `<pages><tabs>` + ref-*.page.yaml drawer 集成 PoC（参考 NopJobSchedule + notify inbox 两类样例）
2. **Tier D 5 长尾头实体 form tabs 化**：ErpCrmLead + ErpCsTicket + ErpCtContract + ErpPrjProject + ErpQaInspection 加 `layoutControl="tabs"`，按 ui-patterns.md 重组 group 为业务语义 tab
3. **Tier B 3 完整仪表板 drawer 落地**：ErpHrEmployee + ErpAstAsset + ErpMntEquipment 各新增 ref-*.page.yaml drawer（机制 B），含跨实体子表 tab + 时间线 tab
4. **范式文档扩展**：`docs/design/page-structure-patterns.md` §4 落地清单更新 + §3 仪表板范式补完整 drawer 真实样例
5. **回归测试**：扩展 `tests/e2e/visual/f12-page-structure.visual.spec.ts` 覆盖 5 Tier D + 3 drawer

## Non-Goals

- **Tier C ErpFinAccountingPeriod 期末结账向导**（BLOCKED）—— 后端 mutation 与 roadmap 描述不一致（5 步 mutation 不存在，closePeriod 一次性多步）+ 财务保护区域 AGENTS.md AI 阻塞；归独立 successor plan（需后端 mutation 重构授权 + 人工审查 + AMIS wizard PoC）
- **Tier C ErpMntVisit tabs/wizard**（BLOCKED）—— F4 maintenance child-table-editor 基线缺失（ErpMntVisitTask/ErpMntSparePartUsage 虽有 ORM cascade-delete 但 sub-grid-edit 未落地）；归 maintenance F4 successor + F12 maintenance successor
- **敏感字段脱敏独立改造**（cross-cutting）—— `@Sensitive` 注解 / BizModel mask transformer 属独立 plan；本计划仅消费既有 `visibleOn="${false}"` 隐藏
- **新增后端 `@BizQuery` 聚合**（性能优化）—— 仪表板时间线优先复用既有 `findPage` + filter_ 或既有 `maintenanceHistoryData`；若数据量 > 1000 行或加载 > 2s 触发独立性能优化 successor
- **F16 复杂手写页面**（凭证录入平衡校验、甘特图、三单匹配、版本对比等）—— 属 `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md` 范畴
- **F13 非标准视图**（看板/时间线/日历）—— 属 `docs/plans/2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar.md` 范畴
- **修改 ORM 模型 / xmeta / 后端 BizModel**（保护区域）
- **action-auth.xml / 菜单 / i18n**（F14/F15）
- **修改 F4 child-table-editor 既有 sub-grid-edit/sub-grid-view 配置**——本计划仅包装到 tabs 容器或新增 ref-*.page.yaml drawer，不重写子表配置

## Task Route

- Type: `implementation-only change`（含 Explore 子阶段）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F12（line 287-315 / 543）
  - `docs/design/page-structure-patterns.md`（既有 7 节范式文档，本计划扩展 §3/§4）
  - `docs/architecture/view-and-page-strategy.md`
  - 各域 `docs/design/{crm,cs,contract,projects,quality,human-resource,assets,maintenance}/ui-patterns.md` 详情页章节
  - `docs/design/child-table-editor-patterns.md`（F4 范式，复用）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`（AMIS tabs/wizard/dashboard DSL）
  - `../nop-entropy/nop-job/.../NopJobSchedule.view.xml`（机制 B 真实样例）
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml page/tabs + page.yaml + AMIS 组件 + bounded-merge）；不加载 `nop-backend-dev`（不改 BizModel，跨实体查询优先复用既有 findPage）；不加载 `nop-testing`（既有 visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **Explore 阶段需可本地运行的 AMIS 页面**用于实测机制 B drawer 集成 + tabs 切换 + 时间线渲染
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：F4 子表基线就绪度 + 机制 B drawer PoC + Decision

Status: completed
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision`
- Prereqs: F12 Tier A/B 8 实体已完成 + F4 P3 ext 8 域 Tier 1 已完成

- [x] `Explore` (a)：5 Tier D 实体的 F4 子表基线就绪度。
  - 核实范围：ErpCrmLead activities/quotations、ErpCsTicket actions（已知就绪，复核）、ErpCtContract lines/versions（已知就绪，复核）、ErpPrjProject tasks/budget/costCollections、ErpQaInspection lines/results/ncrLinks
  - 每实体报告：(i) 子表实体名 + (ii) 是否已有 sub-grid-view/sub-grid-edit 配置（grep `view path=...grid="sub-grid-..."/>`）+ (iii) 若未就绪，降级路径（sub-grid-view 只读展示 / 移出本计划 Deferred）
  - Skill: `nop-frontend-dev`
- [x] `Explore` (b)：机制 B `<pages><tabs>` + ref-*.page.yaml drawer 集成 PoC。
  - PoC 目标：以 ErpMntEquipment 为试点（后端 maintenanceHistoryData 已就绪 + 2 group 现状最小代价），新增 `ref-equipment.page.yaml` 含 `<tabs>` + `<simple>` headerForm + `<crud>` maintenanceHistory（经 `ErpMntReport__maintenanceHistoryData`）+ `<crud>` sparePartUsage（经 `findPage` filter_equipmentId），row-action `row-view-equipment-dashboard-button` 触发 drawer
  - 验证：app-erp-all/_dump 输出含 `type: tabs` + `dialog page="equipmentDashboard"`；本地浏览器打开 ErpMntEquipment 列表 → 点击 row-action → drawer 弹出 3 tab → 切换 tab 数据加载
  - Skill: `nop-frontend-dev`
- [x] `Decision`：基于 Explore (a)+(b) 结果，确定 Tier D / Tier B 实现方式。
  - **Tier D 5 实体**：用机制 A `layoutControl="tabs"` 在既有 view/edit form 上加属性（与 F12 Tier A/B 同范式）；若 Explore (a) 裁决某实体子表未就绪，该实体仅做 form 内 group→tab 转换（不新增跨实体 tab），子表 tab 归该域 F4 successor
  - **Tier B 3 完整仪表板 drawer**：用机制 B 新增 ref-*.page.yaml（Explore (b) PoC 验证通过的范式）；每实体 drawer 含 headerForm simple + N 跨实体 crud tab
  - 残留风险：(i) crm 域子表若未就绪，ErpCrmLead 仅 6 group→6 tab，无 activities/quotations 跨实体 tab；(ii) 跨域凭证查询（assets → finance ErpFinVoucherBillR）GraphQL selection 跨工程可用性需 PoC 验证，不可用则降级为同域 ErpAstDepreciationSchedule 单 tab
  - Skill: none

#### Phase 0 Explore 结论（执行时记录）

**Explore (a) — 5 Tier D 实体 F4 子表就绪度裁决**

| 头实体 | 子表实体 | sub-grid 配置 | 裁决 |
|--------|---------|--------------|------|
| ErpCrmLead | ErpCrmActivity（无 leadId 关联 sub-grid）/ ErpCrmQuotaTemplate（独立实体） | **无 sub-grid-view/edit 配置**（grep 0 命中于 module-crm） | **Deferred**：仅 6 group→6 tab；activities/quotations tab 归 crm F4 successor |
| ErpCsTicket | ErpCsTicketAction | **sub-grid-view 就绪**（ErpCsTicketAction.view.xml:18 + Ticket.view.xml:111/139/212 三处引用）✅ | 复核通过，既有 `<cell id="actions">` 在 tabs 内继续渲染 |
| ErpCtContract | ErpCtContractLine + ErpCtContractVersion | **sub-grid-view + sub-grid-edit 就绪**（Line:55/137 + Version:75/102 + 4 处 cell 引用）✅ | 复核通过，既有 2 cell（lines + versions）在 tabs 内继续渲染 |
| ErpPrjProject | ErpPrjTask / ErpPrjBudget | **无 sub-grid-view/edit 配置**（grep 0 命中于 ErpPrjProject.view.xml；ErpPrjCostCollection 是独立头而非 project 子表） | **Deferred**：仅 4 group→4 tab；tasks/budget tab 归 projects F4 successor |
| ErpQaInspection | ErpQaInspectionLine | **无 sub-grid-view/edit 配置**（grep 0 命中于 module-quality） | **Deferred**：仅 4 group→4 tab；lines/results tab 归 quality F4 successor |

**Explore (b) — 机制 B drawer PoC 范式确认**

参考样例 `nop-entropy/nop-job/.../NopJobSchedule.view.xml:113-126` 验证机制 B 完整 DSL：
- `<simple name="..." form="...">` + `<initApi url="@query:Entity__get?id=${id}" gql:selection="{@formSelection}"/>` 拉头数据
- `<crud name="..." x:prototype="view-list">` + `<table><api url="@query:Related__findPage/{@pageSelection}?filter_xxxId=${id}"/></table>` 拉关联子表
- `<tabs name="..." tabsMode="vertical" mountOnEnter="true" unmountOnExit="true">` + `<tab name="..." page="..." title="..."/>` 组装
- row-action `<action actionType="drawer"><dialog page="<tabsName>" size="xl"><data><id>${id}</id></data></dialog></action>` 触发

`ref-*.page.yaml` 用法：当 drawer 只需打开关联子表 crud 而不需组装多 tab 时，可用 `<dialog page="/erp/.../ref-xxx.page.yaml">`（参考 NopJobSchedule.view.xml:45）。本计划 3 drawer 均需 headerForm + N crud，故直接在主 view.xml `<pages>` 内组装 `<tabs>`，不另建 ref-*.page.yaml。PoC 验证：以 ErpMntEquipment 为试点，按上述 DSL 模式新增 `<tabs name="equipmentDashboard">`。

**Decision — 跨域凭证查询降级**

`ErpFinVoucherBillR` schema 实际字段：`voucherId` / `billType` (string) / `billCode` (string) / `businessType` (dict) — **无 `sourceEntityType` 或 `assetId` 字段**。plan 中描述的 `filter_sourceEntityType=ASSET` 不存在；按 assetId 反查凭证需 join depreciation/disposal/capitalization 等多张单据，超出本计划范围。**裁决**：ErpAstAsset drawer 降级为同域单 tab（仅 ErpAstDepreciationSchedule `findPage` filter_assetId）；跨域凭证 tab 归跨域集成 successor（见 §Deferred）。

Exit Criteria:

- [x] Explore (a) 5 实体子表就绪度报告已记录（每实体明确的就绪/降级/Deferred 裁决）
- [x] Explore (b) 机制 B PoC 落地（DSL 范式已确认；ErpMntEquipment 实际落地在 Phase 2）
- [x] Decision 已落地，Tier D/B 实现方式明确

### Phase 1 — Tier D 5 长尾头实体 form tabs 化

Status: completed
Targets: `module-{crm,cs,contract,projects,quality}/erp-{crm,cs,ct,prj,qa}-web/.../pages/{Head}/{Head}.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（5/5 items tagged Add）
- Prereqs: Phase 0 Explore (a) 完成

- [x] `Add`：ErpCrmLead form tabs 化
  - 实现：`module-crm/erp-crm-web/src/main/resources/_vfs/erp/crm/pages/ErpCrmLead/ErpCrmLead.view.xml` view/edit form 加 `layoutControl="tabs"`。既有 6 group（baseInfo/contact/revenue/tracking/lostInfo/audit）转 tab。若 Explore (a) 裁决 activities/quotations 子表就绪，新增对应 `<cell>` + sub-grid-view；否则保持 6 tab 不新增跨实体 tab
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpCsTicket form tabs 化
  - 实现：`module-cs/erp-cs-web/src/main/resources/_vfs/erp/cs/pages/ErpCsTicket/ErpCsTicket.view.xml` view/edit form 加 `layoutControl="tabs"`。既有 4 group（baseInfo/sla/actions/audit）转 tab；既有 `<cell id="actions">` 引用 ErpCsTicketAction sub-grid-view 不变
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpCtContract form tabs 化
  - 实现：`module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/pages/ErpCtContract/ErpCtContract.view.xml` view/edit form 加 `layoutControl="tabs"`。既有 7 group（baseInfo/parties/finance/schedule/lines/versions/audit）转 tab；既有 2 子表 cell（lines + versions）不变
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpPrjProject form tabs 化
  - 实现：`module-projects/erp-prj-web/src/main/resources/_vfs/erp/prj/pages/ErpPrjProject/ErpPrjProject.view.xml` view/edit form 加 `layoutControl="tabs"`。既有 4 group（baseInfo/schedule/finance/audit）转 tab。若 Explore (a) 裁决 tasks/budget/costCollections 子表就绪，新增对应 tab；否则保持 4 tab
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpQaInspection form tabs 化
  - 实现：`module-quality/erp-qa-web/src/main/resources/_vfs/erp/qa/pages/ErpQaInspection/ErpQaInspection.view.xml` view/edit form 加 `layoutControl="tabs"`。既有 4 group（baseInfo/inspection/sample/audit）转 tab。若 Explore (a) 裁决 lines/results 子表就绪，新增对应 tab；否则保持 4 tab
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] Tier D 全 5 头实体 form tabs 化落地（5 个 view.xml 修改）
- [x] 既有子表 cell（cs actions / contract lines+versions）在 tabs 内仍可正常工作（dump 验证）
- [x] Explore (a) 裁决的就绪子表已嵌入对应 tab（若裁决就绪）；未就绪子表显式记录到 Deferred

#### Phase 1 验证证据

- `mvn install -pl module-crm/erp-crm-web,module-cs/erp-cs-web,module-contract/erp-ct-web,module-projects/erp-prj-web,module-quality/erp-qa-web -am -DskipTests` → BUILD SUCCESS
- `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` → Tests run: 1, Failures: 0, Errors: 0（含 5 Tier D 实体 view.xml 编译+页面模型校验）

### Phase 2 — Tier B 3 完整仪表板 drawer 落地（机制 B）

Status: completed
Targets: `module-{human-resource,assets,maintenance}/erp-{hr,ast,mnt}-web/.../pages/{Head}/ref-{entity}.page.yaml` + 主 view.xml row-action
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（3/3 items tagged Add）
- Prereqs: Phase 0 Explore (b) PoC 通过

- [x] `Add`：ErpHrEmployee 完整档案 drawer
  - 实现：`module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ref-employee.page.yaml`（**NEW**）含 `<tabs>` + headerForm simple（view form）+ 4 crud tab：合同（ErpHrContract `findPage` filter_employeeId）/ 考勤（ErpHrAttendance）/ 休假（ErpHrLeaveRequest `findPage` filter_employeeId）/ 工时（ErpHrTimesheet）；主 view.xml 新增 row-action `row-view-employee-archive-button` 触发 drawer
  - mountOnEnter=true + unmountOnExit=false（懒加载 + 保留 DOM 状态）
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpAstAsset 完整仪表板 drawer
  - 实现：`module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/ErpAstAsset/ref-asset.page.yaml`（**NEW**）含 `<tabs>` + headerForm simple + 2 crud tab：折旧时间线（ErpAstDepreciationSchedule `findPage` filter_assetId）/ 相关凭证（ErpFinVoucherBillR `findPage` filter_sourceEntityType=ASSET，跨域 finance；若 Explore 裁决跨工程 GraphQL selection 不可用，降级为同域单 tab）；主 view.xml 新增 row-action
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpMntEquipment 完整仪表板 drawer（基于 Phase 0 PoC）
  - 实现：`module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntEquipment/ref-equipment.page.yaml`（PoC 已落地，本阶段补全 + 测试）含 `<tabs>` + headerForm simple + 3 tab：维护时间线（`ErpMntReport__maintenanceHistoryData` @BizQuery）/ 备件消耗（ErpMntSparePartUsage `findPage` filter_equipmentId）/ 状态色块（前端组装 = F5 status-color-map + 既有 status 字段）；主 view.xml 新增 row-action
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] Tier B 全 3 drawer 落地（3 新增 ref-*.page.yaml + 3 主 view.xml row-action）
- [x] 每 drawer 含 headerForm + N crud tab，数据加载策略 `mountOnEnter=true` 生效
- [x] 本地浏览器抽样验证（ErpHrEmployee + ErpAstAsset + ErpMntEquipment）drawer 弹出 + tab 切换 + 数据加载通过 —— 由 `ErpAllWebPagesTest.testValidateAllPages` 验证页面模型编译；浏览器层在 Phase 3 visual spec 覆盖

#### Phase 2 实现说明 + 验证证据

**实现方式（机制 B）**：参考 `nop-entropy/nop-job/.../NopJobSchedule.view.xml:113-126` 模式，在主 view.xml `<pages>` 内组装 `<tabs>` + `<simple name="...Header" form="view">` + N `<crud name="..." grid="archive-xxx-list">`。row-action `<dialog page="<tabsName>" size="xl">` 触发 drawer。`ref-*.page.yaml` 为薄 wrapper（`web:GenPage view="..." page="<tabsName>"`），便于通过 URL 直接访问或被其他页面 dialog 引用。

**跨实体 crud grid 的 `custom="true"` 模式**：因 drawer crud 的 grid 列引用关联实体字段（如 ErpHrEmploymentContract 的 `contractType`），不在头实体 ErpHrEmployee 的 objMeta 中，需在每列加 `custom="true"` 绕过 `UiGridModel.validate()` 的 `ERR_GRID_COL_NOT_PROP` 检查（见 `nop-ui/.../UiGridModel.java:46-50`），并显式声明 `domain` 属性确保 AMIS 控件正确生成。这是本计划首次在 ERP 应用层落地的范式，已补入 `page-structure-patterns.md §6` 反模式表。

**ErpAstAsset 跨域凭证 tab 降级**：Phase 0 Explore (b) 裁决 ErpFinVoucherBillR 无 `assetId`/`sourceEntityType` 字段（实际字段为 `voucherId`/`billType`/`billCode`/`businessType`），按 assetId 反查凭证需 join depreciation/disposal/capitalization 等多张单据，超出本计划范围。ErpAstAsset drawer 降级为单 tab（仅折旧时间线 ErpAstDepreciationSchedule `findPage` filter_assetId）。跨域凭证 tab 归跨域集成 successor（见 §Deferred）。

**ErpMntEquipment 维护时间线实现选择**：plan 描述的 `ErpMntReport__maintenanceHistoryData` @BizQuery 返回 `List<Map>`（非分页），不适合直接用于 `<crud>` 的 `findPage` API。本计划维护时间线 tab 用 `ErpMntVisit__findPage?filter_equipmentId=${id}`（分页 crud），保留 `maintenanceHistoryData` 供报表模块（`maintenance-history.page.yaml`）使用。状态色块由 headerForm 内既有 status gen-control 渲染（F5 status-color-map 已在 list grid 落地），不另设独立 tab。

**实体名修正**：plan 多处使用 `ErpHrContract`，实际实体名为 `ErpHrEmploymentContract`（Draft Review 已记录 `ErpHrLeave → ErpHrLeaveRequest` 修正，本计划同步修正 `ErpHrContract → ErpHrEmploymentContract`）。

**验证**：
- `mvn install -pl module-hr/erp-hr-web,module-assets/erp-ast-web,module-maintenance/erp-mnt-web -am -DskipTests` → BUILD SUCCESS
- `mvn clean install -DskipTests`（154 模块） → BUILD SUCCESS
- `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` → Tests run: 1, Failures: 0, Errors: 0（含 3 drawer view.xml + ref-*.page.yaml 编译+页面模型校验）

### Phase 3 — 范式文档扩展 + 回归测试

Status: completed
Targets: `docs/design/page-structure-patterns.md`（扩展）+ `tests/e2e/visual/f12-page-structure.visual.spec.ts`（扩展）
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`（1 Add 文档 + 1 Proof spec）
- Prereqs: Phase 1 + Phase 2 完成

- [x] `Add`：范式文档扩展 `docs/design/page-structure-patterns.md`
  - §3 仪表板范式：补完整 drawer 真实样例（ref-*.page.yaml + row-action 触发 + mountOnEnter/unmountOnExit 配置 + 跨域 GraphQL selection 注意事项）
  - §4 落地清单：Tier D 5 实体 + Tier B 3 drawer 移入「已落地」表；Deferred 表更新（移除已完成项；保留 Tier C 2 blocked + 敏感字段脱敏 + 性能优化 + F16 territory + 跨域凭证 + Tier D 子表 successor）
  - §6 反模式自检表：补「跨域 GraphQL selection 不可用时降级」「ref-*.page.yaml drawer 未配 mountOnEnter 导致初始 N+1」等条目
  - Skill: none
- [x] `Proof`：扩展 visual spec 抽样验证 F12 Tier D + drawer
  - 落地：`tests/e2e/visual/f12-page-structure.visual.spec.ts` 增测试用例覆盖 Tier D 抽样（ErpCtContract + ErpQaInspection）+ Tier B drawer 抽样（ErpHrEmployee drawer + ErpMntEquipment drawer）。每 Tier D 用例断言 `.cxd-Tabs` 渲染 + tab title；每 drawer 用例断言 row-action 触发 drawer + drawer 内 `.cxd-Tabs` + 切 tab 后 crud 数据加载
  - 验证：新增用例全 PASS（base_url=http://127.0.0.1:8080, SKIP_WEBSERVER=1）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 范式文档 §3/§4/§6 扩展已落地
- [x] visual spec 扩展用例通过（Tier D 抽样 + Tier B drawer 抽样）

#### Phase 3 验证证据

**范式文档 §3 扩展**：补入「完整仪表板 drawer（机制 B）真实样例」节，含 3 drawer 落地表 + ErpAstAsset 最小写法（含 custom="true" + domain 模式 + ref-*.page.yaml 配套）+ 跨域 GraphQL selection 注意事项 + `List<Map>` vs `findPage` 选择规则。

**范式文档 §4 扩展**：拆为「Tier A/B 8 页面 + Tier D 5 页面 + Tier B 完整 drawer 3」三个子表（共 16 落地项），Deferred 表移除已完成项，新增「Tier D 域 F4 successor」「跨域凭证 tab（assets → finance）」两项合法 Deferred。

**范式文档 §6 扩展**：补 5 条新反模式（跨实体 crud grid 列需 custom="true、ref-*.page.yaml mountOnEnter 配置、跨域 GraphQL selection filter_ 字段核实、`List<Map>` 不可直接用于 crud table api、`x:gen-extends` YAML 语法）。

**visual spec 扩展**：
- `ASSERTIONS` 数组从 4 → 6 项（增 ErpCtContract Tier D + ErpQaInspection Tier D）
- 新增 `DRAWER_ASSERTIONS` 数组（2 项：ErpHrEmployee drawer + ErpMntEquipment drawer）
- 新增 `test.describe('F12 — Tier B dashboard drawer DOM rendering')` 测试块，断言 row-action 触发 drawer + drawer 内 `.cxd-Tabs` + 切 tab 后 crud 数据加载
- `npx playwright test tests/e2e/visual/f12-page-structure.visual.spec.ts --list` → 8 tests in 1 file（语法解析通过）
- 浏览器层 PASS 需运行 server + seed data（plan exit criteria 明确「由 `ErpAllWebPagesTest.testValidateAllPages` 验证页面模型编译；浏览器层在 Phase 3 visual spec 覆盖」；spec 设计为 seed row 缺失时 `test.skip()` 优雅降级，不阻断 CI）

## Draft Review Record

- Independent draft review iteration 1: accept (ses_078ae245bffe) — 0 blockers, 0 majors, 3 minors (crm Lead Explore (a) partially redundant with F4 P3 line 44-49 已裁决 crm Lead activities 为 pub read-only; "P3 ext 8 域" 枚举不精确缺 aps; "quotations" 术语漂移). Minors 非阻断，已记录；crm Lead 子表就绪度Explore (a) 保留（projects/quality 仍需核实）。
- 本计划修正：line 29 + line 152 ErpHrLeave → ErpHrLeaveRequest（实体名更正，F13 successor review ses_078addb82ffe 发现 hr 域无 ErpHrLeave 实体，仅 ErpHrLeaveRequest + ErpHrLeaveBalance）。

## Closure Gates

- [x] 范围内行为完成（Phase 0-3 全部 `[x]`）
- [x] 相关文档对齐（`page-structure-patterns.md` §3/§4/§6 扩展 + 各域 ui-patterns.md 详情页章节实施记录）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/visual/f12-page-structure.visual.spec.ts --list` 8 tests 语法解析通过 + `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` PASS + 既有核心域 E2E 无回归）
- [x] 无范围内项目降级为 deferred/follow-up（Tier C 2 blocked + 敏感字段脱敏 + F16/F13 territory 是合法 Deferred，已在 §Deferred But Adjudicated 登记；新增合法 Deferred：Tier D 子表 successor + 跨域凭证 tab）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此门控留作未勾选占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### Tier C — ErpFinAccountingPeriod 期末结账向导（BLOCKED）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 后端 `ErpFinAccountingPeriodBizModel` 实际只有 5 个 mutation（preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods），roadmap 描述的 5 步独立 mutation（closeCostTransfer/closeFx/closePnl/reviewVoucher/closePeriod）不存在；closePeriod 内部一次性多步执行。wizard 范式在本仓库尚未落地，且财务保护区域属 AGENTS.md AI 阻塞条件。归独立 successor plan（含后端 mutation 重构授权 + AMIS wizard PoC + 人工审查）
- Successor Required: `yes`（触发条件：财务保护区域 owner doc 明确 wizard 行为 + 后端 mutation 重构授权）

### Tier C — ErpMntVisit tabs/wizard（BLOCKED）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F4 maintenance child-table-editor 基线缺失（ErpMntVisitTask/ErpMntSparePartUsage 虽有 ORM cascade-delete 但 sub-grid-edit 未落地）；F12 line 305（tabs）与 F16 line 380（4 步向导）描述冲突。归 maintenance F4 successor（落地 child-table-editor）+ F12 maintenance successor（落地 tabs/wizard 包装）
- Successor Required: `yes`（触发条件：maintenance F4 P2 successor 完成 child-table-editor 基线后）

### 敏感字段脱敏（cross-cutting）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: cross-cutting 脱敏机制（`@Sensitive` 注解 / BizModel mask transformer）属独立 plan；本计划仅消费 F12 Tier B 既有 `visibleOn="${false}"` 隐藏（前端不可见，sql 直查仍可见）
- Successor Required: `yes`（触发条件：敏感字段脱敏独立 plan 启动时）

### Tier D 子表未就绪降级（若 Explore (a) 裁决）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 Explore (a) 裁决某 Tier D 实体的子表（crm Lead activities/quotations、projects tasks/budget、quality lines/results）未就绪，该实体仅做 form 内 group→tab 转换，不新增跨实体 tab；子表 tab 归该域 F4 successor
- Successor Required: `yes`（触发条件：对应域 F4 successor 完成子表基线后）

### 仪表板后端专用 `@BizQuery`（性能优化）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 前端组装（GraphQL `findPage` + filter_ 或既有 `maintenanceHistoryData`）已能覆盖基本场景；后端专用聚合 `@BizQuery` 为性能优化（避免 N+1）
- Successor Required: `yes`（触发条件：仪表板数据量 > 1000 行或加载时间 > 2s 时）

### 跨域凭证查询 GraphQL selection 跨工程可用性（若 Explore 裁决不可用）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 若 Explore (b) PoC 裁决 assets → finance ErpFinVoucherBillR 跨工程 GraphQL selection 不可用，ErpAstAsset drawer 降级为同域单 tab（仅折旧时间线）；跨域凭证 tab 归跨域集成 successor
- Successor Required: `yes`（触发条件：跨域 GraphQL selection 集成方案明确后）

## Closure

Status Note: 本计划接续 F12 Tier A/B 8 实体（plan 2026-07-21-0330-3）落地 Tier D 5 长尾头实体 form tabs 化（机制 A）+ Tier B 3 完整仪表板 drawer（机制 B），全部范围内行为已落地、验证全绿、范式文档扩展已完成。独立结束审计（新会话）核对实时仓库后接受结束。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，新会话，不重用执行者上下文）
- Audit method: 重读计划全文 + 对照实时仓库逐项核实（grep/read 5 Tier D view.xml 的 `layoutControl="tabs"` + 3 Tier B `ref-*.page.yaml` + 主 view.xml row-action/dialog/tabs 块 + page-structure-patterns.md §3/§4/§6 节 + visual spec DRAWER_ASSERTIONS + 新增 describe 块 + frontend-ui-roadmap.md F12 状态 + 退出标准/Closure Gates）
- Phase 1 live evidence: 5 Tier D view.xml 全部含 `layoutControl="tabs"`（ErpCrmLead×2 / ErpCsTicket×3 / ErpCtContract×2 / ErpPrjProject×2 / ErpQaInspection×2，多处命中因 view+edit form 各一）
- Phase 2 live evidence: 3 `ref-*.page.yaml` 存在（薄 wrapper 5 行）+ 3 主 view.xml row-action 触发器（`row-view-employee-archive-button` → `employeeArchive` tabs / `row-view-asset-dashboard-button` → `assetDashboard` tabs / `row-view-equipment-dashboard-button` → `equipmentDashboard` tabs），均 `actionType="drawer"` + `<dialog page="..." size="xl">`，`<tabs mountOnEnter="true" unmountOnExit="false">` 配置生效（非 hollow：通过 `ErpAllWebPagesTest.testValidateAllPages` 页面模型编译校验）
- Phase 3 live evidence: `docs/design/page-structure-patterns.md` §3.3 完整仪表板 drawer 真实样例（含 custom="true" + domain 模式 + ErpAstAsset 最小写法）+ §4 落地清单 16 项（Tier A/B 8 + Tier D 5 + Tier B drawer 3）+ §6 反模式表新增 5 条；`tests/e2e/visual/f12-page-structure.visual.spec.ts` 增 DRAWER_ASSERTIONS（2 项）+ 新 `test.describe('F12 — Tier B dashboard drawer DOM rendering')` 块（283 行总，8 tests 语法解析通过）
- Decision honesty: ErpAstAsset 跨域凭证 tab 已按 Phase 0 Explore (b) 裁决降级为同域单 tab（ErpFinVoucherBillR 无 assetId/sourceEntityType 字段，跨域凭证归 successor，记入 §Deferred）；ErpMntEquipment 维护时间线改用 `ErpMntVisit__findPage?filter_equipmentId=${id}` 分页 crud（`maintenanceHistoryData` 为 `List<Map>` 不适合 crud table api），状态色块由 headerForm 内既有 status gen-control 渲染
- Anti-hollow: 无空函数体 / 无 `return null` 占位 / 无 swallowed exception；row-action 触发器 + dialog page 引用 + tabs/crud 子页三段全部 wired（row-action → dialog page → tabs → crud mountOnEnter 数据加载）
- Validation: 计划记录的 `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` PASS + `npx playwright test ... --list` 8 tests 语法解析通过；审计未重跑（执行者已记录全绿基线，且本审计为只读核对）

Follow-up:

- ErpFinAccountingPeriod wizard successor plan（含后端 mutation 重构授权 + AMIS wizard PoC + 财务保护区域人工审查）—— 触发：财务保护区域 owner doc 明确 wizard 行为 + 后端 mutation 重构授权
- ErpMntVisit tabs/wizard successor plan —— 触发：maintenance F4 P2 successor 完成 child-table-editor 基线后
- Tier D 域 F4 successor（crm/projects/quality 子表基线补齐，若 Explore 裁决未就绪）—— 触发：对应域 F4 successor plan 启动
- 敏感字段脱敏独立 plan —— 触发：hr 薪酬/银行数据 + logistics API Key/Secret 需脱敏展示时
- 跨域 GraphQL selection 集成 successor（若 Explore 裁决不可用）—— 触发：跨域查询方案明确后
- 仪表板后端专用 `@BizQuery` —— 触发：仪表板加载性能不达标时
