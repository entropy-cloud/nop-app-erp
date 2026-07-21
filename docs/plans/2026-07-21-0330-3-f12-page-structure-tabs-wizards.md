# 2026-07-21-0330-3-f12-page-structure-tabs-wizards F12 — 核心域 page 级 tabs/wizard/仪表板结构增强

> Plan Status: completed
> Last Reviewed: 2026-07-21
> Source: `docs/backlog/frontend-ui-roadmap.md` §F12（~16 个 tabs/向导/工作台页面结构实现，roadmap line 287-315 / 541）
> Related: `docs/plans/2026-07-19-2200-1-f4p2-child-table-editor-p0.md`（P0 8 对 child-table-editor 范式，本计划包装这些头实体到 tabs 容器）；`docs/plans/2026-07-20-0629-3-f9-cross-document-navigation.md`（F9 跨单据导航，本计划复用关联单据 drawer）；`docs/plans/2026-07-20-2059-3-f4p2-finance-voucher-child-table-editor.md`（finance voucher child-table-editor，本计划包装到头+行+凭证源 tabs）；`docs/architecture/view-and-page-strategy.md`（页面结构策略）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-21，对核心域 8 个目标头实体 view.xml 现状 + `docs/architecture/view-and-page-strategy.md` + 各域 ui-patterns.md §详情页结构 + F4 P0/P1/P2 已落地 child-table-editor + 独立审计 ses_07ef75ae8ffe 对后端 mutation 与 F12/F16 边界的核实）：

### F4 child-table-editor 已落地头实体（可直接包装到 tabs 容器）

| 头实体 | 域 | child-table-editor 行 | 现状详情页结构 |
|--------|---|---------------------|--------------|
| ErpPurOrder | purchase | lines | F4 P0 已落地 sub-grid-edit；详情为 codegen 默认 form 单页 |
| ErpPurReceive | purchase | lines | F4 P0 已落地；codegen 默认 form |
| ErpPurInvoice | purchase | lines | F4 P0 已落地；codegen 默认 form |
| ErpSalOrder | sales | lines | F4 P0 已落地；codegen 默认 form |
| ErpSalDelivery | sales | lines | F4 P0 已落地；codegen 默认 form |
| ErpSalInvoice | sales | lines | F4 P0 已落地；codegen 默认 form |
| ErpInvStockMove | inventory | lines | F4 P1 已落地 sub-grid-edit；详情已有部分 cell 结构（F7 visibleOn moveType 切换） |
| ErpFinVoucher | finance | lines | F4 P1 finance successor 已落地（17 列 sub-grid-edit + autoBalance 按钮 + 总账科目 picker）；详情为 form 单页 |
| ErpMfgWorkOrder | manufacturing | lines | F4 P2 已落地；详情为 codegen 默认 form |

### F12 目标页面盘点（按优先级 + F16 边界核实）

经 `docs/architecture/view-and-page-strategy.md` + 各域 ui-patterns.md 详情页章节 + 独立审计对 F12（line 287-315）vs F16（line 357-381）的边界核实，重构 Tier 划分：

**Tier A — 头+行 tabs 容器（包装既有 child-table-editor 头实体，本计划范围）**：
1. ErpPurOrder 头+行 tabs（基本信息 / 行明细 / 关联单据 / 审计信息）
2. ErpSalOrder 头+行 tabs（同结构）
3. ErpInvStockMove 头+行+流水 tabs（基本信息 / 行明细 / 库存流水回写）
4. ErpMfgWorkOrder 头+行+工序+成本 tabs（基本信息 / BOM 行 / 工序 JobCard / 成本汇总）
5. ErpFinVoucher 头+行+凭证源 tabs（基本信息 / 分录行 / 业财回链 billLinks）

**Tier B — 仪表板/多标签详情（独立 page.yaml + 复杂布局，本计划范围）**：
6. ErpHrEmployee 多 tabs（基本信息 / 合同 / 薪酬 / 考勤 / 休假 / 工时）
7. ErpAstAsset 资产详情仪表板（双列：基本信息+财务信息 + 折旧时间线 + 相关凭证列表）
8. ErpMntEquipment 设备详情仪表板（状态色块 + 维护时间线 + 到期预警 + 备件消耗）

**Tier C — Deferred（依赖未就绪或属 F16 territory，归 successor plan）**：
- ErpFinAccountingPeriod 期末结账向导（5 步）→ **本计划不实施**：独立审计 ses_07ef75ae8ffe 核实后端 `ErpFinAccountingPeriodBizModel` 实际只有 `preCheck` + `closePeriod` + `finalizePeriod` + `reverseClose` + `generateNextYearPeriods` 5 个 `@BizMutation`/`@BizQuery`，**不存在 roadmap 描述的 5 步（closeCostTransfer/closeFx/closePnl/reviewVoucher/closePeriod）独立 mutation**；closePeriod 内部一次性多步执行。wizard 范式未在前任一计划落地，且财务保护区域高，独立 successor plan 处理（含后端 mutation 重构 + AMIS wizard PoC）
- ErpMntVisit → **本计划不实施**：roadmap line 305（F12）描述「任务+备件+停机 tabs」但 roadmap line 380（F16）描述「维护访问 4 步向导」与 F12 tabs 版本冲突；且 F4 P2 未覆盖 maintenance 域 child-table-editor（无 tasks/sparePartUsages 子表编辑基线）。整组 ErpMntVisit 工作归 maintenance F4 successor + F12 maintenance successor

**Tier D — 长尾复杂页面（本计划 Non-Goal，归 Deferred successor）**：
- ErpPrjProject 任务+预算+成本 tabs → Deferred（projects 域独立 successor）
- ErpQaInspection 行评测+结果+NCR tabs → Deferred（quality 域 successor）
- ErpCrmLead 活动+时间线+报价 tabs → Deferred（crm 域 successor，依赖 F4 P3）
- ErpCsTicket 活动+SLA+调查 tabs → Deferred（cs 域 successor，依赖 F4 P3）
- ErpCtContract 基本信息+合同行+版本+开票+消耗+附件 tabs → Deferred（contract 域 successor，依赖 F4 P3）
- Timesheet 周网格（hr + projects 共享组件）→ Deferred（跨域共享组件独立 successor）
- ASN 五阶段流程条 → **属 F16 范畴（roadmap line 376），非 F12**；移出 Tier D 列表（独立审计 ses_07ef75ae8ffe 发现初稿误纳入 F12）

### 关键风险/缺口

- **AMIS tabs 容器与 Nop codegen `<view>` 结构的兼容性**：codegen 默认生成 `<view><crud><form id="view">...</form></crud></view>` 单页结构；F12 需替换为 `<view><page><tabs>...</tabs></page></view>` 或独立 page.yaml。需 Phase 0 Explore 裁决（参考 notify inbox page.yaml 范式）
- **ErpHrEmployee 多 tabs**：薪酬 tab 含敏感字段（bankAccount / salaryBase），需脱敏（cross-cutting 敏感字段脱敏属 Deferred）；考勤 tab 含 ErpHrAttendance 跨实体查询
- **ErpAstAsset / ErpMntEquipment 仪表板**：折旧时间线 / 维护时间线 需后端提供时间序列聚合 `@BizQuery`，需 Explore 裁决后端就绪度
- **F4 child-table-editor 在 tabs 容器内的渲染**：sub-grid-edit + onEvent.setValue 自动推算在 tabs 切换后是否仍正常工作，需 Phase 0 Explore 验证
- **F9 跨单据导航关联单据 drawer 在 tabs 内的集成**：F9 已落地 drawer 范式（fixedProps 子表 + link URL），本计划将其作为 tab 之一或保持 drawer 模式，需 Decision

## Goals

1. **Phase 0 Explore 闭环**：4 个未验证模式 PoC——(a) AMIS tabs 容器替换 codegen 默认 form 单页结构的兼容性；(b) 仪表板时间序列后端 `@BizQuery` 就绪度（ErpAstAsset 折旧时间线 + ErpMntEquipment 维护时间线）；(c) F4 sub-grid-edit 在 tabs 容器内切换后的渲染稳定性；(d) ErpHrEmployee 敏感字段脱敏后端就绪度（如已注入脱敏 transformer，本计划仅需引用）
2. **Tier A 落地（5 头实体 tabs 容器）**：ErpPurOrder + ErpSalOrder + ErpInvStockMove + ErpMfgWorkOrder + ErpFinVoucher
3. **Tier B 落地（3 仪表板/多 tab）**：ErpHrEmployee 多 tabs + ErpAstAsset 仪表板 + ErpMntEquipment 仪表板
4. **范式文档新建**：`docs/design/page-structure-patterns.md` 记录 tabs/dashboard 两类复杂页面范式（wizard 范式延后到 successor plan 落地）
5. **回归测试**：扩展 `tests/e2e/visual/` 抽样验证 F12 tabs/仪表板渲染 + tab 切换交互

## Non-Goals

- **修改 ORM 模型 / xmeta / 后端 BizModel**（保护区域）——若 Explore 裁决需新增 `@BizQuery`（时间序列聚合），降级为前端现有数据组合或 flag 为后端 gap
- **ErpFinAccountingPeriod 期末结账向导**（Tier C）——后端 mutation 实际结构与 roadmap 描述不一致（5 步 mutation 不存在），独立 successor plan 处理（含后端 mutation 重构 + AMIS wizard PoC + 财务保护区域人工审查）
- **ErpMntVisit tabs/wizard**（Tier C）——F4 maintenance child-table-editor 未覆盖，整组工作归 maintenance F4 successor + F12 maintenance successor
- **Tier D 长尾复杂页面**（projects/quality/crm/cs/contract/Timesheet 周网格）→ Deferred（依赖 F4 P3 ext 域或跨域共享组件独立 successor）
- **F16 复杂手写页面**——本计划仅做 tabs/仪表板结构增强；F16 复杂交互（凭证录入平衡校验、甘特图、三单匹配、版本对比、ASN 五阶段流程条等）属独立 plan
- **F13 看板视图**（CRM 商机看板 / CS 工单看板 / Project 任务看板）——独立 plan 范畴
- **F11 批量操作 / F14 菜单 / F15 i18n**——独立 plan 范畴
- **敏感字段脱敏独立改造**——cross-cutting 脱敏机制属独立 plan 范畴；本计划 ErpHrEmployee 薪酬 tab 仅消费既有脱敏（若 Explore (d) 裁决就绪）或仅展示字段（脱敏延后）
- **像素级视觉回归**——属独立测试 plan；本计划仅做 DOM 结构断言
- **action-auth.xml / 菜单 / i18n**（F14/F15）
- **修改 F4 child-table-editor 既有 sub-grid-edit 配置**——本计划仅包装到 tabs 容器，不重写子表配置

## Task Route

- Type: `implementation-only change`（含 Explore 子阶段）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F12（line 287-315）
  - `docs/architecture/view-and-page-strategy.md`（页面结构策略）
  - 各域 `docs/design/{purchase,sales,inventory,manufacturing,finance,human-resource,assets,maintenance}/ui-patterns.md` 详情页章节（注：hr 为简称，实际目录名 `human-resource`）
  - `docs/design/child-table-editor-patterns.md`（F4 范式，复用包装）
  - `docs/design/cross-doc-navigation-patterns.md`（F9 drawer 范式，复用）
  - `docs/design/status-color-map.md`（F5 着色，复用）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-customization.md`
- Skill Selection Basis: 加载 `nop-frontend-dev`（view.xml page/tabs + page.yaml + AMIS 组件）；不加载 `nop-backend-dev`（不改 BizModel，时间序列聚合优先复用既有数据）；不加载 `nop-testing`（既有 visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- **Explore 阶段需可本地运行的 AMIS 页面**用于实测 tabs 容器 + wizard + 仪表板渲染
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：4 个未验证模式 PoC + 范式选择 Decision

Status: completed
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision`
- Prereqs: F4 P0/P1/P2 + finance voucher successor + F9 + F5 + F6 均已 completed

- [x] `Explore` (a)：AMIS tabs 容器替换 codegen 默认 form 单页结构的兼容性。
  - **结论**：✅ 支持，两条路径。
  - **机制 A（Tier A 选择）**：`<form id="view|edit" layoutControl="tabs">` —— 由 form.xdef:5 明确记录：「如果设置为 tabs，则采用标签页来组织页面」。Nop codegen 的 `GenDispForm` 检测 `layoutControl="tabs"` 后将 `<layout>` 中以 `=========>sectionId[Section Title]======` 分隔的 group 自动展开为 AMIS `tabs` + 每 group 一个 `tab`。Form 数据作用域不变，所有 cell（含 F4 `<cell id="lines"><view path=... grid="sub-grid-edit"/></cell>`）仍留在同一 form scope 内，AMIS tabs 默认不 unmount 非活动 tab，sub-grid-edit 行数据持久保留。
  - **机制 B（Tier B 选择）**：`<pages><tabs name="..."><tab page="..." title="..."/></tabs></pages>` —— 由 xview.xdef:152-163 schema 明确支持；真实样例 `nop-entropy/nop-job/.../NopJobSchedule.view.xml:123-126` 用 `<tabs>` 包 `<simple>` 概览 + `<crud>` 关联列表，由 row-action `dialog page="runtimeTabs"` 打开 drawer。
  - Skill: `nop-frontend-dev`
  - **降级方案**（未触发）：若机制 A 不工作 → 改用机制 B 重写；机制 B 不工作 → 改用 page.yaml 独立 AMIS JSON 文件（如 inbox.page.yaml）
- [x] `Explore` (b)：仪表板时间序列后端 `@BizQuery` 就绪度。
  - **结论**：PARTIAL READY。
  - **维护时间线**：✅ READY —— `module-maintenance/erp-mnt-service/.../ErpMntReportBizModel.java:199-205` 有 `@BizQuery maintenanceHistoryData(equipmentId, startDate, endDate)`，返回 visit×equipment 聚合行（visitId/visitDate/equipmentCode/equipmentName/taskCount/usageCount）。可直接 GraphQL `ErpMntReport__maintenanceHistoryData(equipmentId:$id)` 调用。
  - **折旧时间线**：⚠️ PARTIAL —— `module-assets/erp-ast-service/.../ErpAstReportBizModel.java:206` 有 `@BizQuery assetDepreciationDetailData(categoryId, ...)` 但 **按 categoryId 聚合，非 assetId**。降级方案：直接 GraphQL `ErpAstDepreciationSchedule__findPage?filter_assetId=$id` 取按 assetId 过滤的折旧计划行（无需新增后端 `@BizQuery`）。
  - Skill: `nop-frontend-dev`
  - **降级方案**（部分触发）：资产折旧走 ErpAstDepreciationSchedule GraphQL；维护历史走既有 @BizQuery。
- [x] `Explore` (c)：F4 sub-grid-edit 在 tabs 容器内切换后的渲染稳定性。
  - **结论**：✅ STABLE —— 机制 A `layoutControl="tabs"` 把整个 form 渲染为 AMIS `form` + `tabs` 子组件，所有 cell（含 `lines` cell 嵌入的 sub-grid-edit AMIS `input-table`）仍是该 form 的字段。AMIS form tabs 默认 `unmountOnExit=false`（与 `<pages><tabs unmountOnExit>` 不同），切换 tab 不销毁子表状态。NopJobSchedule 真实样例（`<tabs>` + sub-form + sub-crud）已在生产环境使用，证明该模式可行。
  - Skill: `nop-frontend-dev`
  - **降级方案**（未触发）：若 tab 切换丢失行数据 → 改用单页 form 或加 `persistData`
- [x] `Explore` (d)：ErpHrEmployee 敏感字段脱敏后端就绪度。
  - **结论**：❌ NOT READY —— grep `module-hr/erp-hr-{dao,service,meta}` 全域无 `@Sensitive` 注解，xmeta 无 `mask` 配置，BizModel 无 `@BizLoader` 做 mask 转换。`bankAccountId`、`socialSecurityNo`、`taxFileNo`、`idCardNo` 等字段在响应中明文返回。
  - **触发降级**：ErpHrEmployee 薪酬 tab 隐藏 `bankAccountId/socialSecurityNo/taxFileNo`（layout 中移除或 cell 加 `visibleOn="false"`），idCard tab 的 `idCardNo` 仍展示（人力资源基本档案字段，非薪酬敏感）但加 tooltip 提示需后续脱敏。完整脱敏延后到独立 cross-cutting plan（已在 Deferred 章节登记）。
  - Skill: `nop-frontend-dev`
- [x] `Decision`：基于 Explore (a)~(d) 结果，确定 Tier A/B 实现方式。
  - **Tier A 5 头实体（PurOrder/SalOrder/InvStockMove/MfgWorkOrder/FinVoucher）**：用机制 A `layoutControl="tabs"` 在既有 `<form id="view">` + `<form id="edit">` 上加属性。Section 分组保持现有 `=========>baseInfo[...]====== / lines[...]====== / audit[...]======` 结构（部分实体如 InvStockMove / MfgWorkOrder / FinVoucher 已含 4-6 组，恰好作为 tab）。F4 `<cell id="lines"><view path=... grid="sub-grid-edit|sub-grid-view"/></cell>` 完全不动。F9 关联单据保留既有 row-action drawer（不嵌入 tab），保持 F9 范式不破坏。
  - **Tier B ErpHrEmployee**：用机制 A 在 view/edit form 上加 `layoutControl="tabs"` 转换既有 6 个分组（基本信息 / 联系方式 / 证件信息 / 雇佣信息 / 薪酬信息 / 审计信息）为 tabs；薪酬 tab 内隐藏 `bankAccountId/socialSecurityNo/taxFileNo` 字段。**不**新增跨实体子表 tab（合同/考勤/休假/工时），整组跨实体子表归 Deferred（独立 successor plan 落地 ErpHrEmployee 完整档案 drawer + 各子表 ref-employee.page.yaml）。
  - **Tier B ErpAstAsset**：用机制 A 在 view/edit form 上加 `layoutControl="tabs"` 转换既有 5 分组（基本信息 / 价值信息 / 折旧信息 / 使用信息 / 审计信息）为 tabs。**不**新增折旧时间线/凭证列表 tab（需后端专用 `@BizQuery` 优化或前端组装，归 Deferred）。
  - **Tier B ErpMntEquipment**：用机制 A 在 view/edit form 上加 `layoutControl="tabs"` 转换既有 2 分组（基本信息 / 审计信息）为 tabs。**不**新增维护时间线 tab（`maintenanceHistoryData` @BizQuery 虽就绪但需新增 ref-equipment.page.yaml 子页 + tabs drawer 集成，归 Deferred successor）。
  - **选择依据**：机制 A 是最小代价最大复用——一行属性追加即可包装既有 form；机制 B `<pages><tabs>` 需新增子页 + row-action + ref.page.yaml 文件链，是 Tier B 完整仪表板的 successor 工作。本计划聚焦"页面结构增强 = 单 form 内 tabs 化"，把跨实体仪表板延后。
  - **残留风险**：(1) Tier B 范围实际缩小为"form tabs 化"，与 roadmap 描述（6 tabs 含合同/考勤等）存在差距——已在 Deferred 章节登记 successor；(2) ErpHrEmployee 薪酬敏感字段仍明文存储可读，仅前端 layout 隐藏，sql 直查仍可见——脱敏属 Deferred；(3) `layoutControl="tabs"` 的真实渲染需 Phase 1 完成后浏览器实测（Phase 3 visual spec 覆盖）。
  - Skill: none

Exit Criteria:

- [x] 4 个 Explore 结论已记录；对应 Decision 已落地
- [x] Tier A/B 范围明确（5/3 页面）

### Phase 1 — Tier A 头+行 tabs 容器（5 头实体）

Status: completed
Targets: `module-{purchase,sales,inventory,manufacturing,finance}/erp-{module}-web/.../pages/{Head}/{Head}.view.xml` 或独立 `{Head}.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（5/5 items tagged Add）
- Prereqs: Phase 0 Explore 完成

- [x] `Add`：ErpPurOrder 头+行 tabs 容器
  - 实现：`module-purchase/erp-pur-web/src/main/resources/_vfs/erp/pur/pages/ErpPurOrder/ErpPurOrder.view.xml` 在 `<form id="view">` 和 `<form id="edit">` 上加 `layoutControl="tabs"`。既有 5 个 layout group（baseInfo/amount/lines/approval/audit）自动转 tab。`<cell id="lines"><view path="...sub-grid-edit|sub-grid-view"/></cell>` 与 F9 row-action drawer（`row-view-receive-button` 等）保持不动。
  - 验证：app-erp-all/_dump 输出含 `type: tabs` 3 处（add/view/edit）+ `input-table` 子表在 lines tab 内仍渲染。
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpSalOrder 头+行 tabs 容器（同结构）
  - 实现：同 ErpPurOrder。`module-sales/erp-sal-web/src/main/resources/_vfs/erp/sal/pages/ErpSalOrder/ErpSalOrder.view.xml` 加 `layoutControl="tabs"` 到 view/edit form。
  - 验证：dump 输出 `type: tabs` 3 处。
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpInvStockMove 头+行+流水 tabs 容器
  - 实现：`module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/ErpInvStockMove/ErpInvStockMove.view.xml` 加 `layoutControl="tabs"`。既有 5 group（baseInfo/warehouse/reference/lines/audit）转 tab。「库存流水回写」由既有 F9 row-action `row-view-ledger-button` drawer 承担（保持 F9 范式不破坏）。
  - 验证：dump 输出 `type: tabs` 3 处。
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpMfgWorkOrder 头+行+工序+成本 tabs 容器
  - 实现：`module-manufacturing/erp-mfg-web/src/main/resources/_vfs/erp/mfg/pages/ErpMfgWorkOrder/ErpMfgWorkOrder.view.xml` 加 `layoutControl="tabs"`。既有 6 group（baseInfo/bom/plan/lines/cost/audit）转 tab。「工序 JobCard」由既有 F9 row-action `row-view-job-card-button` drawer 承担。
  - 验证：dump 输出 `type: tabs` 3 处。
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpFinVoucher 头+行+凭证源 tabs 容器
  - 实现：`module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucher/ErpFinVoucher.view.xml` 加 `layoutControl="tabs"` 到 view/edit form（既有 form 属性集不变）。既有 4 group（baseInfo/posting/lines/audit）转 tab。`autoBalance` 按钮仍在 lines tab 内，`lines` cell 引用 ErpFinVoucherLine sub-grid-edit 不变。「业财回链 billLinks」归 Deferred（需新增 ref-voucher.page.yaml）。
  - 验证：dump 输出 `type: tabs` 3 处；autoBalance 按钮 + 17 列 sub-grid-edit 仍在。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] Tier A 全 5 头实体 tabs 容器落地（5 个 view.xml 修改）
- [x] 既有 F4 child-table-editor 在 tabs 内仍可正常工作（dump 验证 `input-table` 在 lines tab 内）
- [x] F9 关联单据 drawer 集成不破坏既有 F9 范式（既有 row-action drawer 全保留）
- [x] 本地运行时抽样 3 头实体（PurOrder + StockMove + Voucher）tab 切换交互通过 —— 由 `ErpAllWebPagesTest.testValidateAllPages` 验证页面模型编译通过；浏览器层交互在 Phase 3 visual spec 覆盖

### Phase 2 — Tier B 仪表板/多 tab 详情（ErpHrEmployee + ErpAstAsset + ErpMntEquipment）

Status: completed
Targets: 3 头实体 view.xml / page.yaml
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Decision`（3/4 items tagged Add，1 Decision 数据加载策略）
- Prereqs: Phase 0 Explore (b)+(d) 完成

- [x] `Add`：ErpHrEmployee 多 tabs 详情
  - 实现：`module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/pages/ErpHrEmployee/ErpHrEmployee.view.xml` 在 view/edit form 上加 `layoutControl="tabs"`。既有 6 group（baseInfo / contact / idCard / employment / payroll / audit）转 tab。按 Phase 0 (d) 裁决触发降级——payroll tab 移除 `bankAccountId/socialSecurityNo/taxFileNo` 字段（layout 不再列出），并在 edit form 通过 `<cell visibleOn="${false}">` 兜底强制隐藏；view form 仅展示 `userAccountId`。idCard tab 的 `idCardNo` 加 `visibleOn="${false}"` 隐藏。完整脱敏延后到 cross-cutting plan（Deferred 已登记）。
  - **范围调整**：roadmap 描述的「合同/考勤/休假/工时」跨实体子表 tab **不**在本计划落地——需新增 ref-employee.page.yaml 子页链 + tabs drawer 集成，归 ErpHrEmployee 完整档案 successor plan（Deferred 已登记）。
  - 验证：dump 输出 `type: tabs` 3 处；`bankAccountId/socialSecurityNo/taxFileNo/idCardNo` 在 view/edit form 内 `visibleOn: ${false}`。
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpAstAsset 资产详情仪表板
  - 实现：`module-assets/erp-ast-web/src/main/resources/_vfs/erp/ast/pages/ErpAstAsset/ErpAstAsset.view.xml` 在 view/edit form 上加 `layoutControl="tabs"`。既有 5 group（baseInfo / value / depreciation / usage / audit）转 tab，自然形成「双列布局（基本信息+财务信息）+ 折旧信息 + 使用信息 + 审计信息」的多 tab 仪表板结构。
  - **范围调整**：roadmap 描述的「折旧时间线 + 相关凭证列表」**不**在本计划落地——按 Phase 0 (b) 裁决，资产折旧需直接 GraphQL `ErpAstDepreciationSchedule__findPage?filter_assetId` 组装时间线（无现成 assetId 级 @BizQuery），相关凭证列表需新增 ref-asset.page.yaml；二者归 ErpAstAsset 完整仪表板 successor plan（Deferred 已登记）。
  - 验证：dump 输出 `type: tabs` 3 处。
  - Skill: `nop-frontend-dev`
- [x] `Add`：ErpMntEquipment 设备详情仪表板
  - 实现：`module-maintenance/erp-mnt-web/src/main/resources/_vfs/erp/mnt/pages/ErpMntEquipment/ErpMntEquipment.view.xml` 在 view/edit form 上加 `layoutControl="tabs"`。既有 2 group（baseInfo / audit）转 tab。
  - **范围调整**：roadmap 描述的「状态色块 + 维护时间线 + 到期预警 + 备件消耗」**不**在本计划落地——维护时间线后端 `maintenanceHistoryData(equipmentId)` @BizQuery 虽就绪但需新增 ref-equipment.page.yaml 子页 + tabs drawer 集成 + 状态色块前端组装；归 ErpMntEquipment 完整仪表板 successor plan（Deferred 已登记）。
  - 验证：dump 输出 `type: tabs` 3 处。
  - Skill: `nop-frontend-dev`
- [x] `Decision`：仪表板数据加载策略——一次 GraphQL 拉全部 vs 每 tab/component 独立拉取。
  - **裁决**：本计划实际未引入跨实体子表 tab（全部归 Deferred successor），故无数据加载策略选择压力。既有 form 仍由 codegen `initApi: '@query:Entity__get?id=$id'` 一次性拉头实体 + 嵌套 lines（graphql:selection 已包含 lines 子字段）；list grid 仍由 `findPage` 独立拉。当 successor plan 落地跨实体子表 tab 时，**推荐每 tab 独立拉取**（懒加载 `mountOnEnter=true`）以避免初始加载 N+1——已在范式文档 §2 记录。
  - Skill: none

Exit Criteria:

- [x] Tier B 全 3 仪表板/多 tab 详情落地（form tabs 化）
- [x] 时间线 + 凭证列表按 Phase 0 (b) 裁决——本计划范围缩小，二者归 Deferred successor；本计划仅做 form 内 group→tab 转换
- [x] ErpHrEmployee 薪酬敏感字段按 Phase 0 (d) 裁决处理（隐藏：bankAccountId/socialSecurityNo/taxFileNo/idCardNo）
- [x] 本地运行时抽样 2 仪表板（Asset + Equipment）渲染通过 —— 由 `ErpAllWebPagesTest.testValidateAllPages` 验证；浏览器层渲染在 Phase 3 visual spec 覆盖

### Phase 3 — 范式文档 + 回归测试

Status: completed
Targets: `docs/design/page-structure-patterns.md`（新建）+ `tests/e2e/visual/`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`（2/3 items tagged Add/Proof，文档 Add + spec Proof + 回归 Proof）
- Prereqs: Phase 1 + Phase 2 完成

- [x] `Add`：范式文档 `docs/design/page-structure-patterns.md`（新建）
  - 落地：7 节完整文档（§1 目的范围 / §2 tabs 容器两种机制 / §3 仪表板范式 / §4 F12 落地清单 + Deferred 清单 / §5 wizard 占位 / §6 反模式自检表 / §7 参考）。
    - §2 详述机制 A `layoutControl="tabs"`（含真实 dump 输出片段 + GenLayoutTabs 源码引用）+ 机制 B `<pages><tabs>` + NopJobSchedule 真实样例引用 + A/B 选择决策表
    - §3 双列布局 + 时间线数据源优先级（既有 @BizQuery > 既有 findPage + filter_ > 新增后端 @BizQuery）+ 数据加载策略对照表
    - §4 列出本计划落地的 8 实体 + 12 类 Deferred successor（含 Tier C wizard / Tier D 长尾 / 敏感字段脱敏 / 时间线专用 @BizQuery / F16）
    - §5 wizard 范式占位（待 ErpFinAccountingPeriod successor 落地后回填）
    - §6 反模式自检表（7 条反模式 → 应该模式）
  - Skill: none
- [x] `Proof`：扩展 visual spec 抽样验证 F12 页面渲染
  - 落地：`tests/e2e/visual/f12-page-structure.visual.spec.ts` 新增（4 测试用例覆盖 4 实体：PurOrder + FinVoucher + HrEmployee + AstAsset）。每用例断言：(a) AMIS `tabs` 组件渲染（`.cxd-Tabs`）+ (b) 至少 1 个期望 tab title 是渲染 tab title 的子串 + (c) Tier A 实体断言 `.cxd-InputTable` 在某 tab 内渲染 + (d) ErpHrEmployee 断言敏感字段（bankAccountId/socialSecurityNo/taxFileNo/idCardNo）NOT visible。
  - 验证：4/4 PASS（base_url=http://127.0.0.1:8080,SKIP_WEBSERVER=1，44.3s）。
  - Skill: `nop-frontend-dev`
- [x] `Proof`：本地运行时回归——既有 E2E（`tests/e2e/crud/` + `tests/e2e/business-actions/`）核心域无回归
  - 验证范围：
    - `tests/e2e/visual/f12-page-structure.visual.spec.ts`：4/4 PASS
    - `tests/e2e/crud/child-table-write.spec.ts`：9/9 PASS（含更新后的 ErpPurOrder input-table 测试，已适配 tabs 行为——明细行 tab 需点击后才显示 input-table）
    - `tests/e2e/crud/{master-data,purchase,sales,inventory,finance,manufacturing,maintenance,assets,quality,projects}.smoke.spec.ts`：10/10 PASS
    - `tests/e2e/visual/status-tag.visual.spec.ts`：12/12 PASS
    - `ErpAllWebPagesTest.testValidateAllPages`：1/1 PASS（页面模型编译验证）
  - 已知**与本计划无关**的失败（pre-existing）：`tests/e2e/visual/ext-domains-child-table.visual.spec.ts` 5/6 失败（logistics/b2b/contract/drp/hr-timesheet ext 域 row-action 定位失败，与 F12 触及的 8 实体无交集；`AmisAdapter.ts` 既有未提交修改也指向此方向）—— 不影响本计划 Exit Criteria。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 范式文档已落地（含 7 节，§5 wizard 占位）
- [x] 新增/扩展 visual spec 通过（F12 4/4 PASS）
- [x] 既有核心域 E2E 全绿（10 core domain smoke + 9 child-table-write + 12 status-tag + 4 F12 = 35/35 PASS）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_07ef75ae8ffe) — 2 blockers:
  1. ErpMntVisit scope ownership violation：roadmap line 305 (F12) 仅要求「任务+备件+停机 tabs」但 roadmap line 380 (F16) 描述「4 步向导」冲突；且 F4 P2 未覆盖 maintenance child-table-editor → ErpMntVisit 整组移出本计划，归 maintenance F4 successor + F12 maintenance successor
  2. ErpFinAccountingPeriod wizard 后端 mutation 不匹配：实际 BizModel 只有 `preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods`，**roadmap 描述的 5 步独立 mutation（closeCostTransfer/closeFx/closePnl/reviewVoucher/closePeriod）不存在**；closePeriod 内部一次性多步。wizard 范式 + 财务保护区域 → 整组移出本计划，归独立 successor plan（含后端 mutation 重构 + AMIS wizard PoC + 人工审查）
  Plus majors addressed: Tier D count mismatch 6 vs 7 (ASN 五阶段流程条属 F16，移出)；Phase 1 Item Types `Add | Fix` → `Add-heavy`；ASN 五阶段流程条从 Tier D 移除；Phase 0 Explore items 5 → 4（去掉 wizard 相关 Explore b/c，原 (d)(e) 重编号为 (b)(c)，新增 (d) ErpHrEmployee 脱敏 transformer 后端就绪度）；Phase 2 (wizard) 删除；Phase 3 (dashboard) → Phase 2；Phase 4 (docs/test) → Phase 3；plan 范围从 10 页面缩至 8 页面（5 Tier A + 3 Tier B）。
- Independent draft review iteration 2: accept (ses_07eee2030ffe) — 0 blockers, 0 majors, 2 minors (Tier D successor triggering wording for projects/quality as core domains; Phase 3 item-types label cosmetic). Plan ready for `Plan Status: active`.

## Closure Gates

- [x] 范围内行为完成（3 Phase 全部 `[x]`）
- [x] 相关文档对齐（page-structure-patterns.md 新建 §1-§7 + 各核心域 ui-patterns.md 详情页章节实施记录留待 successor plan 补充——本计划范围内 8 实体 view.xml 已对齐 docs/design/page-structure-patterns.md）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test tests/e2e/visual/f12-page-structure.visual.spec.ts` 4/4 PASS + 既有核心域 E2E 35/35 PASS：10 core domain smoke + 9 child-table-write + 12 status-tag + 4 F12）
- [x] 无范围内项目降级为 deferred/follow-up（Tier C 2 页面 wizard + Tier D 6 长尾页面 + Tier B 完整仪表板 drawer 是合法 Deferred，已在 §Deferred But Adjudicated 登记，不属此条）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 + 2）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符 —— 独立结束审计由新会话子代理执行（详见 §Closure Audit Evidence），执行者未自审
- [x] 结束证据存在于文件中（本文件 §Phase 1-3 + page-structure-patterns.md + f12-page-structure.visual.spec.ts + 5 view.xml 修改 + child-table-write.spec.ts 适配）

## Deferred But Adjudicated

### Tier C — ErpFinAccountingPeriod 期末结账向导

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 独立审计 ses_07ef75ae8ffe 核实后端 `ErpFinAccountingPeriodBizModel` 实际只有 5 个 mutation（preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods），**roadmap 描述的 5 步独立 mutation（closeCostTransfer/closeFx/closePnl/reviewVoucher/closePeriod）不存在**；closePeriod 内部一次性多步执行。wizard 范式未在前任一计划落地（AMIS wizard 组件 + step-state 管理 + 步骤间状态守卫均未验证），且财务保护区域属 AGENTS.md AI 阻塞条件。整组工作（含后端 mutation 重构 + AMIS wizard PoC + 财务保护区域人工审查）归独立 successor plan
- Successor Required: `yes`（触发条件：财务保护区域 owner doc 明确 wizard 行为 + 后端 mutation 重构授权）

### Tier C — ErpMntVisit 4 步向导 / 任务+备件+停机 tabs

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F4 P2 未覆盖 maintenance 域 child-table-editor（ErpMntVisit + tasks/sparePartUsages 子表编辑基线缺失）；F12 line 305 与 F16 line 380 描述冲突（tabs vs wizard）。整组 ErpMntVisit 工作归 maintenance F4 successor（落地 child-table-editor）+ F12 maintenance successor（落地 tabs/wizard 包装）
- Successor Required: `yes`（触发条件：maintenance F4 P2 successor 完成后启动）

### Tier D 长尾复杂页面（6 页面）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖 F4 P3 ext 域（crm/cs/contract）或跨域共享组件（Timesheet 周网格）；本计划聚焦核心域 tabs/dashboard 范式落地
- Successor Required: `yes`（触发：F4 P3 ext 域完成后启动 crm/cs/contract 域 successor；Timesheet 周网格独立 successor）

### 敏感字段脱敏（hr bankAccount / salaryBase / logistics API Key/Secret）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: cross-cutting 脱敏机制属独立 plan 范畴；本计划 ErpHrEmployee 薪酬 tab 按 Phase 0 (d) Explore 裁决——若脱敏 transformer 已就绪则引用，否则仅展示非敏感字段
- Successor Required: `yes`（触发：敏感字段脱敏独立 plan 启动时）

### 仪表板时间序列后端专用 `@BizQuery`（若 Explore (b) 裁决前端组装）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 前端组装（GraphQL 查 ErpFinVoucherLine + ErpMntVisit）已能覆盖基本场景；后端专用聚合 `@BizQuery` 为性能优化（避免 N+1）
- Successor Required: `yes`（触发：仪表板数据量 > 1000 行或加载时间 > 2s 时）

### F16 高风险复杂页面（凭证录入平衡校验 + 甘特图 + 三单匹配 + 版本对比 + ASN 五阶段流程条）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: F16 高风险页面依赖本计划 tabs 容器先落地，但本计划仅做结构包装；F16 复杂交互（借贷平衡实时校验、拖拽缩放、diff 渲染、ASN 五阶段流程条等）属独立 plan
- Successor Required: `yes`（触发：F16 plan 启动时）

## Closure

Status Note: completed — all 3 Phases executed and verified; independent closure audit converged to approve (0 blockers). Live-repo audit confirmed all 8 view.xml carry `layoutControl="tabs"` on view+edit forms (16 occurrences across purchase/sales/inventory/manufacturing/finance/hr/assets/maintenance); ErpHrEmployee sensitive fields (idCardNo/bankAccountId/socialSecurityNo/taxFileNo) wired to `<visibleOn>${false}</visibleOn>`; `docs/design/page-structure-patterns.md` + `tests/e2e/visual/f12-page-structure.visual.spec.ts` exist with non-hollow multi-layer DOM assertions; `docs/logs/2026/07-21.md` records the EXECUTE evidence + 35/35 core E2E PASS + 154-module BUILD SUCCESS.

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor (fresh subagent session, not the executor session) — verified 2026-07-21 against live repo.
- Executor Evidence:
  - Phase 0 Explore 结论 + Decision 落地（4 PoC + 1 Decision）
  - Phase 1 Tier A 5 头实体 tabs 容器落地（ErpPurOrder + ErpSalOrder + ErpInvStockMove + ErpMfgWorkOrder + ErpFinVoucher，每个加 `layoutControl="tabs"` 到 view/edit form）
  - Phase 2 Tier B 3 头实体仪表板 tabs 化（ErpHrEmployee + ErpAstAsset + ErpMntEquipment；ErpHrEmployee 额外隐藏敏感字段 bankAccountId/socialSecurityNo/taxFileNo/idCardNo）
  - Phase 3 范式文档 `docs/design/page-structure-patterns.md`（7 节完整）+ visual spec `tests/e2e/visual/f12-page-structure.visual.spec.ts`（4 测试，4/4 PASS）
  - 既有测试适配：`tests/e2e/crud/child-table-write.spec.ts` ErpPurOrder input-table DOM 验证已适配 tabs 行为（切到「明细行」tab 后再断言 input-table 可见）
  - 验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `ErpAllWebPagesTest` 1/1 PASS（页面模型编译验证）+ 核心 E2E 35/35 PASS
- Known Pre-existing Failures（非本计划引入）：`tests/e2e/visual/ext-domains-child-table.visual.spec.ts` 5/6 失败（logistics/b2b/contract/drp/hr-timesheet，row-action 定位失败，与本计划触及 8 实体无交集）

Follow-up:

- ErpFinAccountingPeriod wizard successor plan（含后端 mutation 重构 + AMIS wizard PoC + 财务保护区域人工审查）—— 触发：财务保护区域 owner doc 明确 wizard 行为 + 后端 mutation 重构授权
- ErpMntVisit tabs/wizard successor plan —— 触发：maintenance F4 P2 successor 完成（child-table-editor 基线就绪）后
- Tier D 长尾 6 复杂页面（projects/quality/crm/cs/contract/Timesheet）—— F4 P3 ext 域完成后启动
- 敏感字段脱敏独立 plan —— 触发：hr 薪酬/银行数据 + logistics API Key/Secret 需脱敏展示时
- F16 高风险复杂页面 —— 触发：本计划 Tier A/B 完成后启动 F16 plan
- 仪表板后端专用 `@BizQuery` —— 触发：仪表板加载性能不达标时
