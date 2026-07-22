# 2026-07-22-1400-2-f16-p2-ext-domain-complex-pages F16 P2 复杂页面 successor（hr/logistics/b2b/contract/drp）

> Plan Status: active
> Last Reviewed: 2026-07-22
> Source: `docs/backlog/frontend-ui-roadmap.md` §F16（line 359-382 / 547）+ `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md` §Deferred But Adjudicated「P2 F16 页面」
> Related: `docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md`（F16 低风险批 predecessor，§Deferred 明确本计划为 P2 successor）；`docs/plans/2026-07-22-0845-3-f13-non-standard-views-kanban-timeline-calendar.md`（F13 timeline each+tpl 降级先例，本计划 logistics/b2b 时间线复用该范式）；`docs/plans/2026-07-22-1400-1-f16-high-risk-gantt-bom-scan.md`（F16 高风险 successor，与本计划正交可并行）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-22，对 7 个 P2 F16 目标页面后端就绪度 + F13 timeline 降级先例 + F12 drawer 范式 + 独立后端就绪度审计 ses_076def075ffe）：

### 本计划范围：7 个 P2 复杂页面（全部前端组装既有数据）

| # | 域 | 页面 | 后端就绪度 | 前端组装范式 | 风险 |
|---|---|------|-----------|------------|------|
| 1 | hr | 薪酬核算审批（汇总+审批级联） | **YES**：`ErpHrSalaryBizModel` runPayroll/markPaid + 平台 approval-support DIRECT 审批轴（submit/approve/reject）；`ErpHrSalary__findList` 标准 findPage | 标准 crud + 平台审批 action + 前端汇总卡片（人数/应发/社保/个税/实发 group-by 部门） | 低 |
| 2 | hr | 组织架构图（树形+员工嵌入） | **PARTIAL**：`ErpHrDepartment.parentId` self-FK 就绪（树数据）；`ErpHrEmployee.departmentId` 就绪；**无 tree @BizQuery**（bare CRUD BizModel） | `ErpHrDepartment__findList` 客户端重建树 + `ErpHrEmployee__findList` 嵌入；AMIS tree 或 F13 each+tpl 自定义 | 中（树重建 PoC） |
| 3 | logistics | 发运追踪时间线 | **PARTIAL**：`handleTrackingWebhook` 等写入 `ErpLogShipmentLog`（事件日志实际字段 `actionType`/`executedAt`/`isSuccess`/`requestBody`/`responseBody`/`httpStatus`/`errorCode`/`errorMessage`）；**module-logistics 零 @BizQuery** | `ErpLogShipmentLog__findList` filter shipmentId → F13 each+tpl timeline 降级范式；包裹卡片 + 超期警告前端组装 | 低（F13 先例） |
| 4 | b2b | EDI 事务详情（状态时间线+双栏报文） | **PARTIAL**：`ErpB2bEdiDocBizModel` 每次状态迁移写 `ErpB2bEdiLog`（direction/resultCode/resultMsg/requestPayload/responsePayload/logTime）；**无专用 timeline @BizQuery** | `ErpB2bEdiLog__findList` filter ediDocId → F13 each+tpl timeline + 双栏报文 tpl（requestPayload/responsePayload）+ 语法高亮 `<pre>` | 低（F13 先例） |
| 5 | b2b | ASN 流程条 | **YES**：`ErpB2bAsn` status 字段 + 字典 `asn-status.dict.yaml` 实际 **4 值**（RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED）；`findUnmatchedAsns` @BizQuery；状态机 `RECEIVED→MATCHED→RECEIVED_TO_STOCK`（+CANCELLED 终态） | AMIS steps 或 each+tpl 流程条（3 活跃阶段色块 + 当前阶段高亮 + CANCELLED 终态 + 明细行匹配状态） | 低 |
| 6 | contract | 合同版本对比（双栏 diff） | **NO**：`ErpCtContractVersionBizModel` 仅 finalizeVersion/signVersion + `findSiblings` 为 protected helper（**非 @BizQuery，不可 GraphQL 调用**；标准 `ErpCtContractVersion__findList` filter contractId 可用）；**无 field-level diff** | 客户端 `ErpCtContractVersion__findList` filter contractId 取版本列表 → `__get` 两版本 → adaptor 逐字段对比 → 双栏 tpl（新增=绿/删除=红/修改=黄）+ 数值差值箭头 | 中高（diff 渲染 PoC） |
| 7 | drp | 净需求计算报表（分组折叠+公式） | **PARTIAL**：`DrpEngine.runDrp` 计算写入 `ErpDrpLine.netRequirement/suggestedQty/replenishmentType`；**无分组 @BizQuery** | `ErpDrpLine__findList` filter planId → 客户端按 materialId 分组折叠 + Σ 公式 tpl（safetyStock + forecastDemand − currentStock ...）+ 建议补货量可编辑 | 中（分组折叠 PoC） |

### F13 timeline 降级先例（可复用）

F13 plan Phase 0 PoC 结论：AMIS 原生 timeline/calendar 组件 prop 契约失败 → 降级为 `each` + `tpl` 自定义 JSON 组装。logistics 发运追踪时间线（#3）+ b2b EDI 事务详情（#4）直接复用此范式。

### F12 drawer 范式（可复用）

F12 Tier B successor（plan 2026-07-22-0845-1）落地 `<pages><tabs>` + `<simple>` headerForm + `<crud>` 跨实体子表 drawer 范式（ErpHrEmployee/ErpAstAsset/ErpMntEquipment 3 drawer）。hr 组织架构图（#2）+ 合同版本对比（#6）可复用 drawer 作为详情展开容器。

### 关键风险/缺口

- **contract 版本对比 diff 渲染**：无后端 diff 端点。客户端 `__get` 两版本 → 逐字段对比 → 双栏 tpl 渲染。需 Phase 0 PoC 验证 tpl 内条件渲染（新增/删除/修改三态颜色）的 AMIS 可行性。降级方案：单栏高亮变更字段（非双栏并排）
- **hr 组织架构图树重建**：无 tree @BizQuery。客户端 findList 重建树需 PoC。降级方案：AMIS tree-select 单选展开（非完整 org chart 可视化）
- **drp 分组折叠**：AMIS 是否有原生分组/折叠组件需 PoC。降级方案：each+tpl 按物料分组 section + 表格
- **b2b EDI 报文语法高亮**：纯文本 `<pre>` 即可（非代码编辑器级高亮），低风险
- **所有 7 页面均为独立 page.yaml（非标准 CRUD）**：菜单接入需 `*.action-auth.xml` 新增菜单项

## Goals

1. **Phase 0 Explore 闭环**：(a) contract 版本对比 diff 渲染 PoC；(b) hr 组织架构图树重建 PoC；(c) drp 分组折叠 PoC
2. **7 P2 复杂页面落地**：hr 薪酬核算审批 + hr 组织架构图 + logistics 发运追踪时间线 + b2b EDI 事务详情 + b2b ASN 流程条 + contract 合同版本对比 + drp 净需求计算报表
3. **范式文档扩展**：`docs/design/page-structure-patterns.md` §8 F16 复杂页面范式补 §8.9 汇总审批页 + §8.10 版本 diff 对比 + §8.11 分组折叠报表 + §8.12 流程步骤条
4. **回归测试**：每页面至少 1 visual spec（核心交互路径 DOM 断言）

## Non-Goals

- **新增后端 @BizQuery/@BizMutation**（保护区域）—— 全部 7 页面前端组装既有数据；若某页面确需新端点，该页面降级为 Deferred（非本计划阻塞）
- **修改 ORM 模型**（保护区域）
- **地图集成**（logistics 发运追踪）—— roadmap Non-Goal；时间线用纯列表呈现，不含地理地图
- **F16 高风险页面**（aps gantt/mfg BOM/inventory PDA/maintenance wizard）—— 属 Plan 1 范畴
- **敏感字段脱敏**（hr 薪酬敏感字段）—— 属 Plan 3 范畴；本计划 hr 薪酬审批页仅消费 F12 既有 `visibleOn="${false}"` 隐藏
- **F15 i18n**—— 属 Plan 3 范畴
- **code-level 语法高亮库集成**（b2b EDI 报文）—— 纯 `<pre>` 文本展示，不引入 highlight.js 等

## Task Route

- Type: `implementation-only change`（全前端，零后端变更）
- Owner Docs:
  - `docs/backlog/frontend-ui-roadmap.md` §F16（line 359-382 / 547）
  - `docs/design/human-resource/ui-patterns.md` §薪酬 + §组织架构
  - `docs/design/logistics/ui-patterns.md` §发运追踪
  - `docs/design/b2b/ui-patterns.md` §EDI 事务 + §ASN
  - `docs/design/contract/ui-patterns.md` §版本管理
  - `docs/design/drp/ui-patterns.md` §净需求计算
  - `docs/design/page-structure-patterns.md` §8 F16 复杂页面范式（本计划扩展 §8.9-§8.12）
  - `docs/design/non-standard-views-patterns.md` §时间线（F13 timeline each+tpl 降级范式，复用）
  - `../nop-entropy/docs-for-ai/02-core-guides/page-dsl-pattern-catalog.md`（AMIS DSL）
- Skill Selection Basis: 加载 `nop-frontend-dev`（page.yaml + AMIS 组件 + each+tpl + bounded-merge + F13 timeline 范式复用）；不加载 `nop-backend-dev`（零后端变更）；不加载 `nop-testing`（visual spec 归 Closure Gates）。

## Infrastructure And Config Prereqs

- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- Explore 阶段需可本地运行 AMIS 页面用于实测 diff 渲染 + 树重建 + 分组折叠
- 无新 config / 端口 / 密钥依赖

## Execution Plan

### Phase 0 — Explore：3 PoC + Decision

Status: planned
Targets: plan 内 Explore 结论 + Decision 记录
Skill: `nop-frontend-dev`

- Item Types: `Explore | Decision`（Explore 经指南规则 9 授权：pre-Decision 探索）
- Prereqs: F16 低风险批已完成（plan 2026-07-22-0845-2）

- [ ] `Explore` (a)：contract 版本对比 diff 渲染 PoC。
  - PoC 目标：以 `ErpCtContractVersion__get`（两版本）为基础，验证 AMIS tpl 内条件渲染双栏 diff（新增=绿/删除=红/修改=黄 + 数值差值箭头）的可行性；评估字段对比 adaptor（逐字段 walk → diff map）
  - 候选 (a)：双栏并排 tpl（左旧右新 + 差异行高亮颜色 + 数值差值 ↑↓ 箭头）
  - 候选 (b)：降级单栏高亮（仅列出变更字段 + 新值/旧值 inline + 颜色标记）
  - 倾向候选 (a) 但若 tpl 复杂度过高降级 (b)
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (b)：hr 组织架构图树重建 PoC。
  - PoC 目标：`ErpHrDepartment__findList`（扁平 + parentId）→ adaptor 栈算法重建嵌套 → AMIS tree 渲染 + `ErpHrEmployee__findList` 嵌入部门节点（节点含员工数/负责人）；验证 tree 多级展开 + 搜索高亮
  - 候选 (a)：AMIS `type:tree` 嵌套 options（部门树 + 叶子 tooltip 员工列表）
  - 候选 (b)：降级 F10 tree-list 范式（parentId self-FK tree-list grid，非可视化 org chart）
  - 倾向候选 (a)（可视化效果更好），降级 (b)（与 F10 一致已有范式）
  - Skill: `nop-frontend-dev`
- [ ] `Explore` (c)：drp 分组折叠 PoC。
  - PoC 目标：`ErpDrpLine__findList` filter planId → 客户端按 materialId 分组 → AMIS 分组/折叠呈现（每组：物料名 + Σ 公式 + 明细行 + 建议补货量可编辑）
  - 候选 (a)：AMIS crud + groupBy column（若 AMIS crud 原生支持行分组）
  - 候选 (b)：each+tpl 按物料分组 section + 嵌套 table
  - Skill: `nop-frontend-dev`
- [ ] `Decision`：基于 Explore (a)(b)(c) 结果确定 3 风险页面实现方式。
  - Skill: none

Exit Criteria:

- [ ] Explore (a)(b)(c) 结论已记录；Decision 已落地
- [ ] 3 风险页面实现方式明确；降级方案已记录

### Phase 1 — hr 域 2 页面（薪酬核算审批 + 组织架构图）

Status: planned
Targets: `module-hr/erp-hr-web/.../pages/dashboard/payroll-approval.page.yaml`（**NEW**）+ `org-chart.page.yaml`（**NEW**）+ `erp-hr.action-auth.xml` 菜单
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（2/2 items tagged Add）
- Prereqs: Phase 0 Explore (b) 完成

- [ ] `Add`：hr 薪酬核算审批页
  - 实现：独立 `payroll-approval.page.yaml`：(1) 顶部 form 年月选择 + 部门筛选；(2) 汇总卡片（前端 group-by 部门：人数/应发/社保/个税/实发，从 `ErpHrSalary__findList` 客户端聚合）；(3) 明细 crud（ErpSalary 列表 + 平台审批 action 按钮 submit/approve/reject + markPaid/voidSalary row-action）；(4) 导出为 Non-Goal（平台 Excel 导出归 successor）。菜单接入 `erp-hr.action-auth.xml`
  - Skill: `nop-frontend-dev`
- [ ] `Add`：hr 组织架构图页
  - 实现：按 Phase 0 (b) Decision 落地独立 `org-chart.page.yaml`：(1) `ErpHrDepartment__findList` → adaptor 重建嵌套树；(2) AMIS tree 渲染（部门多级展开 + 节点含员工数/负责人）+ `ErpHrEmployee__findList` 嵌入；(3) 搜索高亮（前端 filter tree nodes）。菜单接入 `erp-hr.action-auth.xml`
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] hr 2 页面落地 + 菜单可达
- [ ] 薪酬审批汇总卡片数据正确（group-by 聚合）；组织架构图树渲染 + 员工嵌入生效（或降级 tree-list 生效）

### Phase 2 — logistics + b2b 域 3 页面（发运追踪时间线 + EDI 事务详情 + ASN 流程条）

Status: planned
Targets: `module-logistics/erp-log-web/.../pages/dashboard/shipment-tracking.page.yaml`（**NEW**）+ `module-b2b/erp-b2b-web/.../pages/dashboard/edi-detail.page.yaml`（**NEW**）+ `asn-flow.page.yaml`（**NEW**）+ 各域 action-auth 菜单
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（3/3 items tagged Add）
- Prereqs: F13 timeline each+tpl 降级范式（已完成）

- [ ] `Add`：logistics 发运追踪时间线页
  - 实现：独立 `shipment-tracking.page.yaml`：(1) 顶部 form 发运单选择（ErpLogShipment picker）；(2) F13 each+tpl timeline 范式（`ErpLogShipmentLog__findList` filter shipmentId → 按 `executedAt` 倒序 → tpl 时间线节点 `actionType`/`executedAt`/`isSuccess` + `errorCode`/`errorMessage`（失败时） + 包裹卡片信息）；(3) 超期警告（estimatedDelivery < now 红色标记，前端组装）。地图为 Non-Goal。菜单接入 `erp-log.action-auth.xml`
  - Skill: `nop-frontend-dev`
- [ ] `Add`：b2b EDI 事务详情页
  - 实现：独立 `edi-detail.page.yaml`：(1) 顶部 form EDI 文档选择（ErpB2bEdiDoc picker）；(2) F13 each+tpl timeline（`ErpB2bEdiLog__findList` filter ediDocId → 状态时间线 direction/resultCode/resultMsg/logTime）；(3) 双栏报文查看（requestPayload/responsePayload `<pre>` 纯文本 + 左右分栏 toggle）；(4) 交互日志 = timeline 已覆盖。语法高亮库 Non-Goal。菜单接入 `erp-b2b.action-auth.xml`
  - Skill: `nop-frontend-dev`
- [ ] `Add`：b2b ASN 流程条页
  - 实现：独立 `asn-flow.page.yaml`：(1) ErpB2bAsn 列表 crud；(2) row-action drawer 展开三阶段流程条（RECEIVED→MATCHED→RECEIVED_TO_STOCK 三活跃阶段 + CANCELLED 终态，AMIS steps 或 each+tpl 色块 + 当前阶段高亮 + 明细行匹配状态）。**注：字典 `asn-status.dict.yaml` 实际 4 值，roadmap 描述「五阶段」为笔误，本计划以实时仓库字典为准**。菜单接入 `erp-b2b.action-auth.xml`
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] logistics + b2b 3 页面落地 + 菜单可达
- [ ] timeline 范式复用 F13（each+tpl）；ASN 流程条（三活跃阶段 RECEIVED→MATCHED→RECEIVED_TO_STOCK + CANCELLED 终态）当前阶段高亮生效

### Phase 3 — contract + drp 域 2 页面（版本对比 + 净需求报表）

Status: planned
Targets: `module-contract/erp-ct-web/.../pages/dashboard/version-diff.page.yaml`（**NEW**）+ `module-drp/erp-drp-web/.../pages/dashboard/net-requirement.page.yaml`（**NEW**）+ 各域 action-auth 菜单
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy`（2/2 items tagged Add）
- Prereqs: Phase 0 Explore (a)+(c) 完成

- [ ] `Add`：contract 合同版本对比页
  - 实现：按 Phase 0 (a) Decision 落地独立 `version-diff.page.yaml`：(1) 顶部 form 选择合同 + 两版本（`ErpCtContractVersion__findList` filter contractId → 下拉选择两版本；注 `findSiblings` 是 protected helper 非 @BizQuery 不可 GraphQL 调用）；(2) 客户端 `__get` 两版本 → adaptor 逐字段对比 → 双栏 tpl diff（新增=绿/删除=红/修改=黄 + 数值差值 ↑↓ 箭头）或降级单栏高亮；(3) 仅差异行过滤 toggle。菜单接入 `erp-ct.action-auth.xml`
  - Skill: `nop-frontend-dev`
- [ ] `Add`：drp 净需求计算报表页
  - 实现：按 Phase 0 (c) Decision 落地独立 `net-requirement.page.yaml`：(1) 顶部 form 选择 plan（ErpDrpPlan picker）；(2) `ErpDrpLine__findList` filter planId → 客户端按 materialId 分组折叠（AMIS groupBy 或 each+tpl section）；(3) 每组：物料名 + Σ 公式可视化（safetyStock + forecastDemand − currentStock + allocatedQty − onOrderQty = netRequirement）+ 明细行 + 建议补货量（suggestedQty）展示。菜单接入 `erp-drp.action-auth.xml`
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] contract + drp 2 页面落地 + 菜单可达
- [ ] 版本对比 diff 渲染生效（双栏或降级单栏）；净需求分组折叠 + 公式可视化生效

### Phase 4 — 范式文档扩展 + 回归测试

Status: planned
Targets: `docs/design/page-structure-patterns.md`（扩展 §8.9-§8.12）+ `tests/e2e/visual/`
Skill: `nop-frontend-dev`

- Item Types: `Add-heavy | Proof`
- Prereqs: Phase 1-3 完成

- [ ] `Add`：范式文档扩展 `docs/design/page-structure-patterns.md`
  - §8.9 汇总审批页（薪酬核算：group-by 聚合卡片 + 平台审批 action + 明细 crud）
  - §8.10 版本 diff 对比（双栏/单栏 tpl diff + 字段对比 adaptor）
  - §8.11 分组折叠报表（drp：groupBy/each+tpl 分组 + Σ 公式可视化）
  - §8.12 流程步骤条（ASN：AMIS steps/each+tpl 色块 + 阶段高亮）
  - §4 Deferred 表更新：P2 项移入「已落地」
  - Skill: none
- [ ] `Proof`：visual spec
  - 落地：`tests/e2e/visual/f16-p2-complex-pages.visual.spec.ts`（7 页面抽样 DOM 断言：薪酬汇总卡片 + org tree + logistics timeline + edi timeline/payload + asn steps + contract diff + drp grouping）
  - 验证：`npx playwright test` 新增用例全绿（seed-data 缺失 graceful skip）；既有 dashboards/f16 无回归
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 范式文档 §8.9-§8.12 新增 + §4 更新
- [ ] visual spec 通过（无失败；seed-data 缺失 graceful skip）

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_076d1b333ffe) — 2 blockers + 1 major：
  1. **BLOCKER**：ASN 状态模型虚构——plan 列 5 态 RECEIVED/MATCHED/VALIDATED/PENDING_RECEIPT/RECEIVED，实时仓库字典 `asn-status.dict.yaml` 实际 4 值 RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED（VALIDATED/PENDING_RECEIPT 不存在）；「五阶段」为 roadmap 笔误传播。已修正 baseline row #5 + Phase 2 item 3 为三活跃阶段 + CANCELLED 终态
  2. **BLOCKER**：`ErpLogShipmentLog` 字段名错误——plan 列 eventType/logTime/result，实际字段 actionType/executedAt/isSuccess/requestBody/responseBody/httpStatus/errorCode/errorMessage。已修正 baseline row #3 + Phase 2 item 1 tpl 字段引用
  3. **MAJOR**：`ErpCtContractVersion__findSiblings` 是 protected helper 非 @BizQuery 不可 GraphQL 调用，与「零后端变更」Task Route 矛盾。已改用标准 `ErpCtContractVersion__findList` filter contractId
- Independent draft review iteration 2: accept (ses_076c7cfb2ffe) — B2（ShipmentLog 字段）+ M1（findSiblings）已修正核实通过；B1（ASN 状态模型）修正发现 1 处遗漏（Phase 2 Exit Criteria line 158 仍写「五阶段」）→ 已修正为「三活跃阶段 + CANCELLED 终态」与 baseline row #5 + Phase 2 item 3 一致。全文「五阶段」仅余 roadmap 笔误注释 + 本审查记录（历史），合法。0 blockers, 0 majors。


## Closure Gates

- [ ] 范围内行为完成（Phase 0-4 全部 `[x]`）
- [ ] 相关文档对齐（`page-structure-patterns.md` §8.9-§8.12 + 各域 ui-patterns）
- [ ] 已运行验证（`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `npx playwright test` 新增用例全绿 + 既有无回归）
- [ ] 无范围内项目降级为 deferred/follow-up（地图/语法高亮库/导出/敏感脱敏是合法 Non-Goal，已声明）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### logistics 发运追踪地图集成

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap Non-Goal；时间线用纯列表呈现
- Successor Required: `no`（项目 2.x）

### b2b EDI 报文 code-level 语法高亮

- Classification: `optimization candidate`
- Why Not Blocking Closure: 纯 `<pre>` 文本展示满足可读性需求；highlight.js 等库引入增加前端体积
- Successor Required: `yes`（触发条件：EDI 报文调试高频需求时）

### hr 薪酬核算 Excel 导出

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 平台 Excel 导出归 successor；本计划聚焦前端页面结构
- Successor Required: `yes`（触发条件：平台导出能力就绪时）

## Closure

Status Note: <pending>

Closure Audit Evidence:

- <pending>
