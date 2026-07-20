# 2026-07-21-0330-3-f12-page-structure-tabs-wizards F12 — 核心域 page 级 tabs/wizard/仪表板结构增强

> Plan Status: active
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

Status: planned
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision`
- Prereqs: F4 P0/P1/P2 + finance voucher successor + F9 + F5 + F6 均已 completed

- [ ] `Explore` (a)：AMIS tabs 容器替换 codegen 默认 form 单页结构的兼容性。
  - 验证方法：参考 `docs/design/notify/inbox-patterns.md` page.yaml 范式 + AMIS `<tabs>` + Nop `<view>` xdef schema——是否支持 `<view><page><tabs><tab title="..."><form>...</form></tab></tabs></page></view>` 结构
  - Skill: `nop-frontend-dev`
  - **降级方案**：若不支持 → 改用 page.yaml 独立文件（AMIS `<page>` JSON）+ view.xml 仅保留 grid
- [ ] `Explore` (b)：仪表板时间序列后端 `@BizQuery` 就绪度（ErpAstAsset 折旧时间线 + ErpMntEquipment 维护时间线）。
  - 验证方法：grep 各域 BizModel 是否已有 `findDepreciationHistory` / `findMaintenanceHistory` `@BizQuery`，或可复用既有 ErpFinVoucherLine 查询 + ErpMntVisit 查询组装
  - Skill: `nop-frontend-dev`
  - **降级方案**：若无现成 `@BizQuery` → 前端经 GraphQL 直接查 ErpFinVoucherLine（按 assetId 过滤）+ ErpMntVisit（按 equipmentId 过滤）组装时间线，接受性能可能不佳
- [ ] `Explore` (c)：F4 sub-grid-edit 在 tabs 容器内切换后的渲染稳定性。
  - 验证方法：抽样 ErpPurOrder 包装到 tabs 容器后，切换到「行明细」tab → 新增行 → picker 选择 → 自动推算 → 切换到「基本信息」→ 切换回「行明细」→ 验证行数据仍存在
  - Skill: `nop-frontend-dev`
  - **降级方案**：若 tab 切换丢失行数据 → 改用单页 form（不切换 tab）或加 AMIS `persistData` 配置
- [ ] `Explore` (d)：ErpHrEmployee 敏感字段脱敏后端就绪度。
  - 验证方法：grep hr 域 BizModel / xmeta 是否已注入脱敏 transformer（如 `@Sensitive` annotation 或 xmeta prop 级 `mask` 配置），或需新增
  - Skill: `nop-frontend-dev`
  - **降级方案**：若未就绪 → ErpHrEmployee 薪酬 tab 仅展示非敏感字段（bankAccount / salaryBase 隐藏），完整脱敏延后到独立 cross-cutting plan
- [ ] `Decision`：基于 Explore (a)~(d) 结果，确定 Tier A/B 实现方式。记录选择、替代方案、残留风险。
  - Skill: none

Exit Criteria:

- [ ] 4 个 Explore 结论已记录；对应 Decision 已落地
- [ ] Tier A/B 范围明确（5/3 页面）

### Phase 1 — Tier A 头+行 tabs 容器（5 头实体）

Status: planned
Targets: `module-{purchase,sales,inventory,manufacturing,finance}/erp-{module}-web/.../pages/{Head}/{Head}.view.xml` 或独立 `{Head}.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（5/5 items tagged Add）
- Prereqs: Phase 0 Explore 完成

- [ ] `Add`：ErpPurOrder 头+行 tabs 容器
  - 4 tabs：基本信息（头表单）/ 行明细（既有 child-table-editor）/ 关联单据（F9 drawer 集成）/ 审计信息（status/createdBy/createdAt 等）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：ErpSalOrder 头+行 tabs 容器（同结构）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：ErpInvStockMove 头+行+流水 tabs 容器
  - 3 tabs：基本信息 / 行明细（既有 child-table-editor）/ 库存流水回写（按 moveId 关联 ErpInvStockLedger 子表展示）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：ErpMfgWorkOrder 头+行+工序+成本 tabs 容器
  - 4 tabs：基本信息 / BOM 行（既有 child-table-editor）/ 工序 JobCard（ErpMfgJobCard 按 workOrderId 关联展示）/ 成本汇总（materialCost + laborCost + totalCost + unitCost 显示）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：ErpFinVoucher 头+行+凭证源 tabs 容器
  - 3 tabs：基本信息（头表单含 totalDebit/totalCredit + autoBalance 按钮）/ 分录行（既有 17 列 child-table-editor）/ 业财回链 billLinks（按 voucherId 关联 ErpFinVoucherBillR 展示）
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] Tier A 全 5 头实体 tabs 容器落地
- [ ] 既有 F4 child-table-editor 在 tabs 内仍可正常工作（Phase 0 Explore (c) 验证）
- [ ] F9 关联单据 drawer 集成不破坏既有 F9 范式
- [ ] 本地运行时抽样 3 头实体（PurOrder + StockMove + Voucher）tab 切换交互通过

### Phase 2 — Tier B 仪表板/多 tab 详情（ErpHrEmployee + ErpAstAsset + ErpMntEquipment）

Status: planned
Targets: 3 头实体 view.xml / page.yaml
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Decision`（3/4 items tagged Add，1 Decision 数据加载策略）
- Prereqs: Phase 0 Explore (b)+(d) 完成

- [ ] `Add`：ErpHrEmployee 多 tabs 详情
  - 6 tabs：基本信息 / 合同（ErpHrEmploymentContract）/ 薪酬（ErpHrSalary 历史含敏感字段，按 Phase 0 (d) 裁决——脱敏 transformer 就绪则展示，否则隐藏敏感字段）/ 考勤（ErpHrAttendance 近 N 月摘要）/ 休假（ErpHrLeaveRequest + LeaveBalance）/ 工时（ErpHrTimesheet 近 N 月摘要）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：ErpAstAsset 资产详情仪表板
  - 双列布局：左列基本信息（assetCode / name / category / status 着色 / 入账日期 / 原值 / 净值）+ 右列财务信息（累计折旧 / 残值 / 月折旧 / 折旧方法）
  - 下方：折旧时间线（按 Phase 0 (b) 裁决——后端 `@BizQuery` 或前端组装 ErpFinVoucherLine）+ 相关凭证列表（ErpFinVoucherBillR 关联）
  - Skill: `nop-frontend-dev`
- [ ] `Add`：ErpMntEquipment 设备详情仪表板
  - 顶部状态色块（status / utilization / health）+ 维护时间线（按 Phase 0 (b) 裁决——ErpMntVisit 历史）+ 到期预警（下次预防性维护 schedule）+ 备件消耗（ErpMntSparePartUsage 近 N 月聚合）
  - Skill: `nop-frontend-dev`
- [ ] `Decision`：仪表板数据加载策略——一次 GraphQL 拉全部 vs 每 tab/component 独立拉取。记录选择。
  - Skill: none

Exit Criteria:

- [ ] Tier B 全 3 仪表板/多 tab 详情落地
- [ ] 时间线 + 凭证列表按 Phase 0 (b) 裁决落地（后端 `@BizQuery` 或前端组装）
- [ ] ErpHrEmployee 薪酬敏感字段按 Phase 0 (d) 裁决处理（脱敏展示 / 隐藏）
- [ ] 本地运行时抽样 2 仪表板（Asset + Equipment）渲染通过

### Phase 3 — 范式文档 + 回归测试

Status: planned
Targets: `docs/design/page-structure-patterns.md`（新建）+ `tests/e2e/visual/`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`（2/3 items tagged Add/Proof，文档 Add + spec Proof + 回归 Proof）
- Prereqs: Phase 1 + Phase 2 完成

- [ ] `Add`：范式文档 `docs/design/page-structure-patterns.md`（新建）
  - §1 tabs 容器范式（view.xml vs page.yaml 选择 + `<tabs>` + tab 切换数据持久化）
  - §2 仪表板范式（双列布局 + 时间线 + 凭证列表 + 数据加载策略）
  - §3 F12 落地清单（本计划 8 页面 + Deferred Tier C 2 页面 + Tier D 6 页面）
  - §4 wizard 范式占位（ErpFinAccountingPeriod / ErpMntVisit successor 落地后回填）
  - Skill: none
- [ ] `Proof`：扩展 visual spec 抽样验证 F12 页面渲染
  - 新增或扩展 `tests/e2e/visual/f12-page-structure.visual.spec.ts`，抽样 4 页面（ErpPurOrder tabs + ErpFinVoucher tabs + ErpHrEmployee multi-tabs + ErpAstAsset dashboard）断言：tabs 渲染 + tab 切换 + 仪表板组件渲染
  - Skill: `nop-frontend-dev`
- [ ] `Proof`：本地运行时回归——既有 E2E（`tests/e2e/crud/` + `tests/e2e/business-actions/`）核心域无回归
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 范式文档已落地（含 4 节，§4 占位）
- [ ] 新增/扩展 visual spec 通过
- [ ] 既有核心域 E2E 全绿

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_07ef75ae8ffe) — 2 blockers:
  1. ErpMntVisit scope ownership violation：roadmap line 305 (F12) 仅要求「任务+备件+停机 tabs」但 roadmap line 380 (F16) 描述「4 步向导」冲突；且 F4 P2 未覆盖 maintenance child-table-editor → ErpMntVisit 整组移出本计划，归 maintenance F4 successor + F12 maintenance successor
  2. ErpFinAccountingPeriod wizard 后端 mutation 不匹配：实际 BizModel 只有 `preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods`，**roadmap 描述的 5 步独立 mutation（closeCostTransfer/closeFx/closePnl/reviewVoucher/closePeriod）不存在**；closePeriod 内部一次性多步。wizard 范式 + 财务保护区域 → 整组移出本计划，归独立 successor plan（含后端 mutation 重构 + AMIS wizard PoC + 人工审查）
  Plus majors addressed: Tier D count mismatch 6 vs 7 (ASN 五阶段流程条属 F16，移出)；Phase 1 Item Types `Add | Fix` → `Add-heavy`；ASN 五阶段流程条从 Tier D 移除；Phase 0 Explore items 5 → 4（去掉 wizard 相关 Explore b/c，原 (d)(e) 重编号为 (b)(c)，新增 (d) ErpHrEmployee 脱敏 transformer 后端就绪度）；Phase 2 (wizard) 删除；Phase 3 (dashboard) → Phase 2；Phase 4 (docs/test) → Phase 3；plan 范围从 10 页面缩至 8 页面（5 Tier A + 3 Tier B）。
- Independent draft review iteration 2: accept (ses_07eee2030ffe) — 0 blockers, 0 majors, 2 minors (Tier D successor triggering wording for projects/quality as core domains; Phase 3 item-types label cosmetic). Plan ready for `Plan Status: active`.

## Closure Gates

- [ ] 范围内行为完成（3 Phase 全部 `[x]`）
- [ ] 相关文档对齐（page-structure-patterns.md 新建 §1-§4 + 各核心域 ui-patterns.md 详情页章节实施记录）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test` 抽样 visual + 既有核心域 E2E 全绿）
- [ ] 无范围内项目降级为 deferred/follow-up（Tier C 2 页面 wizard + Tier D 6 长尾页面是合法 Deferred，不属此条）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <pending>

Closure Audit Evidence:

- Auditor / Agent: <pending independent closure auditor>

Follow-up:

- ErpFinAccountingPeriod wizard successor plan（含后端 mutation 重构 + AMIS wizard PoC + 财务保护区域人工审查）—— 触发：财务保护区域 owner doc 明确 wizard 行为 + 后端 mutation 重构授权
- ErpMntVisit tabs/wizard successor plan —— 触发：maintenance F4 P2 successor 完成（child-table-editor 基线就绪）后
- Tier D 长尾 6 复杂页面（projects/quality/crm/cs/contract/Timesheet）—— F4 P3 ext 域完成后启动
- 敏感字段脱敏独立 plan —— 触发：hr 薪酬/银行数据 + logistics API Key/Secret 需脱敏展示时
- F16 高风险复杂页面 —— 触发：本计划 Tier A/B 完成后启动 F16 plan
- 仪表板后端专用 `@BizQuery` —— 触发：仪表板加载性能不达标时
