# 2026-07-17-2256-2-maintenance-assets-linked-visit-anti-double-deduct-e2e Maintenance × Assets 跨域维修费用防双重扣减浏览器层 E2E

> Plan Status: active
> Last Reviewed: 2026-07-17
> Mission: erp
> Work Item: 各域细化端到端验证（maintenance × assets 跨域成本闭环防双重扣减 successor）
> Source: `docs/plans/2026-07-14-0606-2-landed-cost-spare-part-posting-e2e.md` Deferred「与 assets 域 MAINTENANCE_EXPENSE(470) 并存防双重扣减 E2E」(l.163-167) — Successor Required: yes，触发条件「双域并存防双重扣减浏览器层 E2E 需求落地时」。
> 触发条件经实时仓库核实**已满足**：AGENTS.md / project-context.md:34 明示当前重点含「各域细化端到端验证」；防双重扣减后端分支（assets `MaintenanceExpensePostingDispatcher` linkedVisit 分支 + `MaintenanceExpenseAcctDocProvider` clearing/bank 分支）已由 plan 1100-6 落地（assets 维修工单关联维护工单时贷中转清算防双重），浏览器层零覆盖。
> Related: `2026-07-10-1100-6-spare-part-posting.md`（防双重扣减后端源）；`2026-07-14-0606-2-landed-cost-spare-part-posting-e2e.md`（备件消耗过账 E2E 源，本计划补其跨域并存 successor）；`2026-07-14-0215-1-assets-direct-action-e2e.md`（assets 维修 DIRECT 动作 E2E 源，覆盖独立 linkedVisit=false 路径）；`2026-07-17-2256-1` 同批 N=1（无依赖，可独立推进）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-17，`read`/`grep` 实测，非采信旧记忆）：

### 防双重扣减后端分支已落地，浏览器层零覆盖该跨域路径

- **maintenance 域备件消耗过账**（plan 1100-6 / 0606-2 已 E2E 覆盖）：`ErpMntSparePartUsageBizModel.confirm(usageId)` → `MaintenanceIssuePostingDispatcher` + `MaintenanceIssueAcctDocProvider` → `MAINTENANCE_ISSUE` 凭证 Dr 6602 维修费用 / Cr 1403 存货（备件实物出库），config `erp-mnt.spare-part-posting-enabled=true` 已在 webServer JVM arg 启用。
- **assets 域维修费用化过账**（plan 1100-6 后端）：
  - `MaintenanceExpensePostingDispatcher`（`module-assets/erp-ast-service/.../posting/`）—— 构造 `PostingEvent`，`boolean linkedVisit = maintenance.getMaintenanceVisitId() != null`（:79），billData 携带 `BILL_DATA_MAINTENANCE_LINKED_VISIT` + 科目编码集（6602 维修费用 / 2502 中转清算 / 1002 银行 / 1403 存货 / 1601 固定资产）。
  - `MaintenanceExpenseAcctDocProvider.createFacts`（:46-66）—— `MAINTENANCE_EXPENSE` 业务类型，借维修费用 6602；**贷方按 linkedVisit 分支（防双重扣减核心）**：
    - `linkedVisit=true`（关联维护工单）：贷 **2502 维修中转清算**（备件已由 maintenance 域实物出库 MaINTENANCE_ISSUE 贷 1403，assets 不再重复贷存货）。
    - `linkedVisit=false`（独立维修）：贷 **1002 银行存款**。
- **assets 维修独立路径 E2E 已覆盖**：`tests/e2e/business-actions/ast-maintenance.action.spec.ts` 存在（0215-1 落地，覆盖 DIRECT 状态机），但该 spec 走 **独立（linkedVisit=false）路径**，未覆盖 linkedVisit=true 跨域并存分支。**linkedVisit=true 防双重扣减跨域路径浏览器层零覆盖**（无 `*linked*`/`*double-deduct*` spec，实测 grep NONE）。
- **跨域实体链**：`ErpAstMaintenance.maintenanceVisitId`（→ mnt `ErpMntVisit`）为关联键；mnt `ErpMntSparePartUsage`（+Line）经 visit 关联备件消耗。两端经各自 `@BizMutation`（mnt `confirm` / assets 维修 approve→过账触发）DIRECT 可达。

### 浏览器层 E2E 范式已验证（本计划复用，零范式新增）

- 凭证行精确数值断言范式（`findVoucherIdByBillCode` + `assertVoucherLines` 两原语，0704-1/0742-1 起稳定）：按 postingType NORMAL/REVERSAL 区分 + 逐行断言 subjectCode + dcDirection + 借贷金额。
- 跨域编排 setup 范式（`orchestration/_helper.ts` runP2pChain/runO2cChain/runMfgChain + cleanup，1249-1/0704-2 起）：自包含建测试专用物料/实体隔离 + finally 兜底 cleanup（凭证+移动+辅助账逐域删）保护共享 DB 数值断言基线。

### 剩余差距

跨域并存防双重扣减浏览器层零覆盖 —— 当 assets 维修工单关联 mnt visit（linkedVisit=true）且该 visit 有备件消耗（mnt `MAINTENANCE_ISSUE` 已贷 1403）时，assets `MAINTENANCE_EXPENSE` 应贷 2502 中转清算（非 1403），避免备件成本双重扣减。该后端分支无浏览器层端到端验证。

## Goals

- **跨域并存防双重扣减浏览器层 E2E**（1 新 spec）：自包含 setup 双域链 —— mnt Visit + 备件消耗 Usage(+Line) → `confirm` → `MAINTENANCE_ISSUE` 凭证 Dr 6602/Cr 1403；assets Maintenance（`maintenanceVisitId` 指向该 visit）→ approve → `MAINTENANCE_EXPENSE` 凭证 Dr 6602/**Cr 2502 中转清算**（linkedVisit=true 分支）。断言：
  - 两凭证行精确数值（subjectCode + dcDirection + 借贷金额）经 `findVoucherIdByBillCode` + `assertVoucherLines`。
  - assets `MAINTENANCE_EXPENSE` 贷方为 2502（非 1403）—— 备件成本未被双重扣减（mnt 已贷 1403，assets 贷 2502 中转）。
  - 对照：独立 assets 维修（linkedVisit=false，无关联 visit）→ `MAINTENANCE_EXPENSE` 贷方为 1002 银行存款（与既有 ast-maintenance spec 路径一致，本计划仅作同 spec 内对照断言确认分支差异）。
- **owner doc 收口**：解除 0606-2 Deferred「与 assets 域 MAINTENANCE_EXPENSE(470) 并存防双重扣减 E2E」（补 `**RELEASED by 2026-07-17-2256-2**`）；e2e-runbook 业务动作表 + 跨域防双重扣减行 + 套件计数对齐；当日日志聚合条目。

## Non-Goals

- **不重测 assets 维修独立路径全状态机**：0215-1 `ast-maintenance.action.spec.ts` 已覆盖 DIRECT 状态机；本计划独立路径仅作分支差异对照（1 断言），不重复状态机覆盖。
- **不重测 maintenance 备件消耗过账生命周期**：0606-2 `mnt-spare-part-posting.action.spec.ts` 已覆盖 confirm + MAINTENANCE_ISSUE 凭证行；本计划仅消费其产物作为跨域 setup 前置。
- **不实现维修工时费用化过账**：maintenance 工时（labor）计提 GL 过账后端未实现（1018-3/1100-6 Deferred，需工时归集体系），属后端 successor（触发条件：维修工时成本核算后端落地时）。
- **不实现新后端/契约/ORM 模型**：本计划仅消费侧 DIRECT `@BizMutation` E2E + 测试层 + 种子 COA 加性补充（2502 中转清算科目）。若 Explore 发现 linkedVisit 分支有 bug，属执行期豁免（即时修复 + 记录 + 模块 JUnit 回归），仅确证为生产缺陷时开显式 successor。
- **不做 assets 维修资本化（CAPITALIZATION）路径**：assets 维修可资本化或费用化，本计划仅费用化（MAINTENANCE_EXPENSE）防双重扣减面；资本化路径归独立 successor。

## Task Route

- Type: `implementation-only change`（maintenance × assets 已落地 DIRECT `@BizMutation` + 过账分支浏览器层 E2E + 测试层 + 种子 COA 加性补充；ORM/契约/codegen 无变更）。
- Owner Docs: `docs/design/maintenance/state-machine.md`（§实现偏离 L163 备件消耗过账 + 跨域并存说明，既有）、`docs/design/assets/maintenance.md`（§维修费用化 + 防双重扣减，既有）、`docs/testing/e2e-runbook.md`（业务动作表 + 凭证行断言表 + 套件结构，既有）。
- Skill Selection Basis: DIRECT `@BizMutation` 经 GraphQL 浏览器层 + 跨域 setup 编排 + 凭证行数值断言 → E2E 测试本体 `Skill: none`（对齐 0606-2/0742-1 范式裁决）；种子 COA 加性补充（2502 科目）参考 `nop-backend-dev` 路由文档但不写平台业务代码。若 Explore 发现 latent defect 需根因诊断，重新加载 `nop-debugging`。
- Protected Areas: 测试在根 `tests/e2e/` 非 reactor 模块；种子 COA 仅加性行（不动既有科目）；不改 ORM/契约/`_gen/`；任何生产缺陷须 ask-first / 开 successor。

## Infrastructure And Config Prereqs

- 无新外部端口/密钥/.env/外部服务/数据迁移。
- 复用既有 Playwright 基础设施（`playwright.config.ts` webServer fresh-DB + 种子 + auth fixtures + `erp-mnt.spare-part-posting-enabled=true` 已启用）。
- **种子 COA 加性补充**：`erp_md_subject.csv` 追加 2502 维修中转清算科目行（经实时仓库核实部署期种子 `app-erp-all/.../_init-data/erp_md_subject.csv` **不含** 2502，仅有 1001/1002/2202/1403/1601/6602/6603 等，**必须补**，非条件性）。对齐 1430-1/1800-1 范式（按需补科目使 Provider 硬编码科目码可达）。注：种子 display name 与本计划 prose 不同（1403 实际 display="在途物资"、6602="折旧费用"），断言按 `subjectCode` 不受影响，断言凭证行 subjectName 时须用种子实际 display。
- **webServer JVM arg 无需新增**（两域过账 config `erp-mnt.spare-part-posting-enabled` 已启用；assets 维修过账无 config 门控 DIRECT 可达）。
- 回滚策略：全部改动为测试层（spec .ts）+ 种子 COA 加性行 + 文档，git 可逆；自包含 setup（测试专用物料/实体隔离，避免污染 inventory/maintenance dashboard 基线）+ finally 兜底 cleanup（双域凭证+移动+辅助账逐域删）。

## Execution Plan

### Phase 1 - Explore：跨域链路可达性 + linkedVisit 分支 + setup 隔离冷核实

Status: planned
Targets: `MaintenanceExpensePostingDispatcher`、`MaintenanceExpenseAcctDocProvider`、`ErpMntSparePartUsageBizModel.confirm`、`ErpAstMaintenanceProcessor`、`tests/e2e/business-actions/ast-maintenance.action.spec.ts`、`tests/e2e/orchestration/_helper.ts`
Skill: none

- Item Types: `Decision | Proof`
- Prereqs: 无

- [ ] `Proof`：冷核实跨域实体链 + 浏览器层可达性 —— `ErpAstMaintenance.maintenanceVisitId`（→ mnt `ErpMntVisit`）经 `__save` 可置；mnt `ErpMntSparePartUsage.confirm(usageId)` DIRECT 可达（已由 0606-2 验证）；assets 维修 approve→过账触发 DIRECT 可达（已由 0215-1 验证）。核实两域 `@BizMutation` 入参名/返回类型。
  - Skill: none
- [ ] `Proof`：冷核实 linkedVisit 分支判定源 —— `MaintenanceExpensePostingDispatcher` linkedVisit = `maintenance.getMaintenanceVisitId() != null`（确认非 config 或其他判定）；`MaintenanceExpenseAcctDocProvider.createFacts` linkedVisit=true → Cr 2502 / linkedVisit=false → Cr 1002（确认分支科目）。**核实并记录 provider docstring 漂移**：`MaintenanceExpenseAcctDocProvider` l.24 docstring 称 false 分支「贷存货（备件消耗）/银行存款」，但 `createFacts`（:60-64）仅贷 bankSubject 1002，`SUBJECT_INVENTORY=1403` 常量声明（:38）但 `createFacts` 未引用（dead constant）—— 断言以 `createFacts` 实际行为为准（false → Cr 1002 非 1403）。核实种子 `erp_md_subject.csv` 是否含 2502（实测缺失，Phase 2 必须补，见下）。
  - Skill: none
- [ ] `Proof`：冷核实既有 `ast-maintenance.action.spec.ts` 路径 —— 确认其走独立（linkedVisit=false）路径（无 maintenanceVisitId），本计划 linkedVisit=true 为真实净增缺口（非 1005-1 式伪基线）。核实 `findVoucherIdByBillCode` + `assertVoucherLines` 两原语在 orchestration/_helper.ts 可用。
  - Skill: none
- [ ] `Decision`：选定跨域 setup 编排 + 隔离策略（测试专用备件物料，避免 WEIGHTED_AVERAGE 混合 + 清理抹除种子 inventory dashboard totalValue 基线，对齐 0704-2 范式）+ cleanup 顺序（assets 凭证→mnt 凭证→mnt 移动→双域实体）。记录入计划。
  - Skill: none

Exit Criteria:

- [ ] 跨域链路 + linkedVisit 分支 + setup 隔离经实时仓库核实明确（无伪基线）。
- [ ] Decision（编排 + 隔离 + cleanup 顺序）记录入计划，确定性可复现。

---

### Phase 2 - 跨域并存防双重扣减浏览器层 E2E spec + 种子 COA

Status: planned
Targets: `tests/e2e/business-actions/mnt-ast-linked-visit-anti-double-deduct.action.spec.ts`（新）、`tests/e2e/orchestration/_helper.ts`（复用/按需扩）、种子 `erp_md_subject.csv`（+2502 加性行若 Explore 确认缺失）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] `Add`：种子 `erp_md_subject.csv` 追加 2502 维修中转清算科目行（经实时仓库核实部署期种子**缺失** 2502，必须补，加性，不动既有科目）。
  - Skill: none
  - Skill: none
- [ ] `Add`：`mnt-ast-linked-visit-anti-double-deduct.action.spec.ts` —— 自包含双域 setup（finally 兜底 cleanup 按 Decision 顺序）：
  - 测试 1（linkedVisit=true 防双重扣减正路径）：建测试专用备件物料 + INCOMING 备货 + mnt 设备 + Visit + SparePartUsage(+Line) → `confirm(usageId)` → 断言 `MAINTENANCE_ISSUE` 凭证 Dr 6602/Cr 1403；建 assets Maintenance(`maintenanceVisitId`=该 visit，totalCostAmount=备件金额) → approve → 断言 `MAINTENANCE_EXPENSE` 凭证 Dr 6602/**Cr 2502**（linkedVisit=true，非 Cr 1403）—— 备件成本未被双重扣减。
  - 测试 2（linkedVisit=false 独立对照）：建 assets Maintenance（无 maintenanceVisitId，totalCostAmount=X）→ approve → 断言 `MAINTENANCE_EXPENSE` 凭证 Dr 6602/**Cr 1002** 银行（linkedVisit=false 分支，与既有 ast-maintenance spec 路径一致，确认分支差异）。
  - 凭证行精确数值经 `findVoucherIdByBillCode`（按 billHeadCode 区分两域凭证）+ `assertVoucherLines` 逐行断言 subjectCode + dcDirection + 借贷金额。
  - Skill: none
- [ ] `Proof`：spec 独立运行全绿（`npx playwright test mnt-ast-linked-visit-anti-double-deduct.action.spec.ts`）；maintenance/assets 既有 spec 抽样回归 0 新增失败。
  - Skill: none

Exit Criteria:

- [ ] 2 测试全绿；linkedVisit=true 贷 2502（非 1403）+ linkedVisit=false 贷 1002 分支差异断言；凭证行精确数值对齐。
- [ ] finally cleanup 保护共享 DB（inventory/maintenance/assets dashboard 数值断言基线无漂移，经测试专用物料隔离）。

---

### Phase 3 - 文档对齐 + Deferred RELEASED + 日志

Status: planned
Targets: `docs/testing/e2e-runbook.md`、`docs/plans/2026-07-14-0606-2-landed-cost-spare-part-posting-e2e.md` Deferred 段、`docs/logs/2026/07-17.md`、`docs/backlog/README.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2

- [ ] `Add`：`docs/testing/e2e-runbook.md` 业务动作表 +maintenance × assets 跨域防双重扣减行 + 凭证行断言表 +2 行（MAINTENANCE_ISSUE Cr1403 / MAINTENANCE_EXPENSE Cr2502 linkedVisit=true）+ 套件计数对齐（实测 spec 数 +N）；0606-2 Deferred 段补 `**RELEASED by 2026-07-17-2256-2**` 行（触发条件已满足 + 本计划交付证据）；`docs/logs/2026/07-17.md` 增聚合条目（spec 数/验证状态/范围纪律）；`docs/backlog/README.md` +1 done 行。
  - Skill: none

Exit Criteria:

- [ ] e2e-runbook 业务动作表 + 凭证行断言表 + 套件计数对齐实际；0606-2 Deferred RELEASED 登记落地；日志 + backlog 条目在位。

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is** (`ses_08f6822acffeYvbp17Cc0vN7Vv`，general，新会话冷重播无起草者上下文，2026-07-17) — 全部 load-bearing 基线主张经实时仓库核实**truthful**（区别于被 cancel 的 1005-1 伪基线）：`ast-maintenance.action.spec.ts`（l.66-73 无 maintenanceVisitId，l.139 断言 Cr 1002）确走独立 linkedVisit=false 路径；无 `*linked*`/`*double-deduct*`/`mnt-ast*` spec（glob NONE）；`MaintenanceExpensePostingDispatcher:79` linkedVisit 判定逐字核实；`MaintenanceExpenseAcctDocProvider.createFacts` l.60-64 Cr 2502(true)/Cr 1002(false) 核实；mnt `confirm` + `MaintenanceIssueAcctDocProvider` Dr 6602/Cr 1403 核实；`ErpAstMaintenance.maintenanceVisitId`（assets ORM :1303-1304,1356）核实；部署期种子 `erp_md_subject.csv` **不含** 2502（仅 1001/1002/2202/1403/1601/6602/6603）→ Explore-gated seed-add 必要；0606-2 Deferred（l.163-167）+ 触发条件逐字核实；`project-context.md:34`「各域细化端到端验证」触发 argued-as-met；反松弛 clean；R14 单结果面（跨域防双重扣减，linkedVisit=false 为 1-assertion 对照非独立面）；item typing + per-item Skill 完整；`findVoucherIdByBillCode`/`assertVoucherLines`（_helper.ts l.92/151）在位。0 BLOCKER / 0 MAJOR / 3 MINOR：(M1) Phase 1 Explore 未记录 provider docstring l.24 漂移（false 分支 docstring 称「贷存货」但 createFacts 仅贷 bank，SUBJECT_INVENTORY=1403 dead constant）—— 已在 Phase 1 Proof 增「断言以 createFacts 实际行为为准（false → Cr 1002 非 1403）」；(M2) 种子 display name 与 prose 不同（1403=在途物资/6602=折旧费用）—— 已在 Infra 段增注「断言按 subjectCode 不受影响，subjectName 用种子实际 display」；(M3) 2502 seed-add 应更果断（实测缺失非条件）—— 已将 Phase 2 seed-add item + Infra 段由条件性改为「必须补」。**共识达成 → Plan Status 置 `active`。**

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。本计划为测试层 + 种子 COA 加性行 + 文档（预期零生产契约变更）。结束时运行新增 spec + maintenance/assets 既有 spec 抽样回归 + 全量构建。

- [ ] 范围内行为完成（跨域防双重扣减 linkedVisit=true 正路径 + linkedVisit=false 对照 2 测试）
- [ ] 相关文档对齐（e2e-runbook 业务动作表 + 凭证行断言表、0606-2 Deferred RELEASED、当日日志、backlog）
- [ ] 已运行验证：新增 spec 独立运行全绿 + maintenance/assets 既有 spec 抽样回归 0 新增失败 + `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（closure gate，确认零后端污染）
- [ ] 无范围内项目降级为 deferred/follow-up（维修工时费用化/资本化路径均为计划内 Non-Goal 附触发条件）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 维修工时（labor）费用化过账

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: maintenance 工时计提 GL 过账后端未实现（1018-3/1100-6 Deferred，需工时归集体系，无 labor 实体/字段）。本计划仅备件消耗 + assets 维修费用化防双重扣减面。
- Successor Required: `yes`（触发条件：维修工时成本核算后端落地时）

### assets 维修资本化（CAPITALIZATION）路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: assets 维修可费用化（MAINTENANCE_EXPENSE）或资本化（计入资产成本）。本计划仅费用化防双重扣减面；资本化路径为不同结果面。
- Successor Required: `yes`（触发条件：assets 维修资本化浏览器层 E2E 需求落地时）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <待独立子代理（新会话）执行>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷须以显式 successor 承接，不得出现在此处>
