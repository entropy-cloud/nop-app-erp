# 2026-08-09-0751-2 per-action-fnpt-contract-esign-hr-salary

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.4d
> Related: mission `permissions-enforcement`；P1.3（粒度裁决，已 done，提供收敛粒度 + contract/hr 角色基线）；P1.1（敏感字段清单，已 done，合同/薪酬属保密五面）；P1.6（xwf 语义裁决，done——ErpHrSalary 是 4 xwf 实体之一，本计划与其软协调：执行序 P1.6(N=1 @1400-1) 已 done，其 Salary approve binding 裁决已落地，本计划对齐 voidSalary）；P1.4a/b/c（审批集 + EDI 生命周期范式，已 done/本批，复用其 per-action FNPT 声明模式）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.4d

## Current Baseline

P1.3 已裁决映射收敛粒度 = 角色×SUBM + **敏感动作 per-action FNPT** + 兜底策略（双命名空间分离）。`roles-and-permissions.md` §第二批扩展域 A（L151-162）定义 contract/hr 敏感域角色：**合同专员**（起草/谈判/版本管理/提交审批）、**合同审批人**（合同审批与电子签触发，含 NEGOTIATION→ACTIVE 迁移门控）、**HR 专员**（员工/合同/考勤/休假维护，提交薪酬核算）、**薪酬审批人**（薪酬审批，含个税/社保机密）。

**contract 域现状**（实测，物理目录 `module-contract` ↔ 工程名 `erp-ct`）：

- 生成文件 `_erp-ct.action-auth.xml`（`module-contract/erp-ct-web/.../_vfs/erp/ct/auth/_erp-ct.action-auth.xml`）：15 实体每实体仅 `:query`/`:mutation`，无 per-action 点、无 `roles` 种子。
- delta `erp-ct.action-auth.xml`（同目录非生成文件，99 行）：**纯 SUBM 菜单重组**（合同列表/模板/审批/折扣返利/签章管理/仓库搜索/开票消耗），**零 per-action FNPT 声明、零 `roles=` 种子**（grep 确认）。签章管理 SUBM（L65-73）仅重新暴露生成 `ErpCtSignatureRequest-main`，无 `:initSignatureRequest`/`:handleSignatureCallback` 等 FNPT children。
- app `app.action-auth.xml`：仅 `x:extends` delta + 空 `<site id="main"/>`。

**contract 域敏感 mutation 实测**（`module-contract/erp-ct-service`）：

- **ErpCtSignatureRequestBizModel**（电子签生命周期，`.../service/entity/ErpCtSignatureRequestBizModel.java`，441 行，5 mutations + 1 query）：`initSignatureRequest`(@BizMutation L98，FINALIZED 门控→创建 PENDING_SIGNATURE→调 Provider.initSignature，**HIGH**)、`handleSignatureCallback`(@BizMutation L107，**webhook 入站** HMAC+幂等+状态机推进，**HIGH**，与 b2b handleInboundWebhook 同型最高危入口)、`queryAndUpdateStatus`(@BizMutation L117，轮询回查)、`cancelSignatureRequest`(@BizMutation L124，终态迁移)、`rejectSignature`(@BizMutation L140，终态迁移)；`findExpiringRequests`(@BizQuery L156，只读)。
- **ErpCtContractVersionBizModel**（签章目标，`.../service/entity/ErpCtContractVersionBizModel.java`）：`finalizeVersion`(@BizMutation L46 DRAFT→FINALIZED，签章前置)、`signVersion`(@BizMutation L58 FINALIZED→SIGNED + isCurrent 原子翻转，由签名回调 `completeFullySigned` 触发，**HIGH**)。
- **ErpCtContractBizModel**（合同头，`.../service/entity/ErpCtContractBizModel.java`）：`activate`(L66 NEGOTIATION→ACTIVE，**设计明定由合同审批人门控**，**HIGH**)、`suspend`(L72)、`resume`(L84)、`terminate`(L96)、`expire`(L117)、`amend`(L129)。
- **contract 域无 `useWorkflow="true"` 实体**（grep `module-contract/model` 零命中）；contract 审批**不经 xwf**，「合同审批人」直接门控 `activate` + 电子签生命周期。

**hr 域现状**（物理目录 `module-hr` ↔ 工程名 `erp-hr`）：

- delta `erp-hr.action-auth.xml`（`module-hr/erp-hr-web/.../_vfs/erp/hr/auth/erp-hr.action-auth.xml`，218 行）：**已声明 3 个 per-action FNPT + roles 种子**——`ErpHrLeaveRequest:approve`→HR 专员（L77-80）、`ErpHrSalary:approve`→薪酬审批人（L101-104）、`ErpHrSalary:markPaid`→薪酬审批人（L105-108）。
- **ErpHrSalaryBizModel**（`.../service/entity/ErpHrSalaryBizModel.java`）：`markPaid`✅、`approve`✅（平台/xwf 标准 action）、`voidSalary`(@BizMutation L103，**作废已审核未发放薪酬**——`paymentStatus` 非 PAID 时置 VOID，PAID 时抛 `ERR_SALARY_LOCKED_AFTER_PAID` 锁定；敏感但未声明)、`calculateSalary`(@BizMutation L80)/`runPayroll`(@BizMutation L89)/`generateBankFile`(@BizMutation L116)（计算/生成类，未声明）。
- ErpHrSalary 是 P1.6 的 4 xwf 实体之一（`useWorkflow="true"`，DIRECT 三轴可达，approve FNPT 已声明对齐 P1.6 裁决）。

**与 P1.6 的交叉**：ErpHrSalary `:approve` 已由 P1.6 裁决 binding + 既有 delta 声明落地；本计划增量 `:voidSalary`（作废已审核未发放薪酬，与 approve/markPaid 同角色门控经审核授权记录的终态），其 enforcement 测试策略（DIRECT 三轴 / xwf 轴后端单测）归 P1.6 裁决，本计划仅声明权限点。

**enforcement 状态**：`nop.auth.enable-action-auth=false`（默认 OFF），本计划仅"已就绪可授权"，不拦截任何调用。

**缺口**：(1) **contract 域零 per-action FNPT、零 roles 种子**——电子签全生命周期（init/handleCallback/queryUpdate/cancel/reject）+ 合同审批门控（activate/signVersion）全部坍缩进泛化 `mutation` 桶，是 P1.4d 实质性缺口（§action-level 表 L202-217 无 contract 行，§灰度推进路线首批未含 contract）。(2) hr `voidSalary`（作废已审核未发放薪酬，置 VOID）未声明——它作用于已审核薪酬记录（approve 已由 薪酬审批人 FNPT 门控），作废经审核授权的记录属敏感状态变更并影响下游过账/银行盘，与 approve/markPaid 不对称。

## Goals

- **补齐 contract 电子签 + 审批门控 per-action FNPT 声明**：为 ErpCtSignatureRequest 电子签生命周期（initSignatureRequest/handleSignatureCallback/queryAndUpdateStatus/cancelSignatureRequest/rejectSignature）+ ErpCtContractVersion 签章门控（signVersion/finalizeVersion）+ ErpCtContract 审批门控（activate）声明独立 per-action FNPT 点 + roles 种子（合同审批人/合同专员），保持 enforcement OFF。
- **补齐 hr 薪酬 voidSalary per-action FNPT 声明**：为 ErpHrSalary:voidSalary（作废已审核未发放薪酬，置 VOID；PAID 锁定不可作废）声明独立 per-action FNPT + roles 种子（薪酬审批人），与既有 approve/markPaid 对称（同角色门控经审核授权的薪酬记录的生命周期终态），保持 enforcement OFF。
- **每集群独立交付 + 独立回归**：contract（delta `erp-ct.action-auth.xml`）与 hr（delta `erp-hr.action-auth.xml`）各自声明 + 各自核验。

## Non-Goals

- **不翻转 enforcement 开关**（归 P2.4/E1.x）。
- **不产 auth 表 CSV 种子**（归 P1.5b；本计划仅 `roles` 静态属性种子）。
- **不做 SUBM 菜单组层 roles 映射**（归 P1.5a）。
- **不改生成文件** `_erp-ct.action-auth.xml`/`_erp-hr.action-auth.xml`；声明只在 delta 非生成文件。
- **不动 hr 既有 `ErpHrLeaveRequest:approve`/`ErpHrSalary:approve`/`:markPaid` 声明**（已落地）。
- **不改电子签/薪酬业务逻辑**（BizModel/Processor/Provider 已落地；本计划仅声明权限点）。
- **不声明 hr 薪酬计算/生成类（calculateSalary/runPayroll/generateBankFile）独立 FNPT**（计算/生成属常规运算，非授权终态变更，留 `:mutation` 桶——Phase 2 Decision）。
- **不声明其他 hr approve（ErpHrSalarySimulation/Timesheet/ShiftSwapRequest approve）**（设计 §action-level 未列为敏感子集；归 successor）。
- **不声明 contract 全部状态迁移（suspend/resume/terminate/expire/amend）独立 FNPT**（运营生命周期，非审批/电子签门控；留 `:mutation` 桶——Phase 1 Decision）。
- **不触及合同/薪酬保密字段级可见性**（保密五面，字段级归 E3.1/E4.1；本计划仅 action 级）。

## Task Route

- Type: `implementation-only change`（delta action-auth.xml per-action FNPT 声明补齐 + roles 种子，enforcement 保持 OFF，不改运行时行为）
- Owner Docs: `docs/design/roles-and-permissions.md` §action-level 声明层 + §第二批扩展域 A（contract/hr 角色基线）；`docs/design/contract/`（电子签设计）；`docs/design/hr/`（薪酬审批）
- Skill Selection Basis: `nop-backend-dev` —— delta action-auth.xml per-action FNPT 声明属后端权限声明层工作（与 roadmap 表格 P1.4d Skill 列一致，与 P1.4a/b/c 同范式）；本计划聚焦声明层，不写 BizModel/Processor 代码（电子签/薪酬业务逻辑已由 2200-2/0831-2 plan 落地）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（仅 delta XML 声明，enforcement 保持 OFF，不改运行时）。

## Execution Plan

### Phase 1 - contract 电子签 + 审批门控 per-action FNPT 声明

Status: completed
Targets: `module-contract/erp-ct-web/src/main/resources/_vfs/erp/ct/auth/erp-ct.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add` / `Decision` / `Proof`
- Prereqs: P1.3（done）；P1.1（done，合同敏感面输入）

- [x] **Decision**：裁决 contract 运营生命周期状态迁移（`suspend`/`resume`/`terminate`/`expire`/`amend`）的 FNPT 处理。考虑的替代方案：(a) 运营迁移留 `:mutation` 桶不声明独立点——这些是合同生效后的运营状态变更（暂停/恢复/终止/到期/修订），非 activate（NEGOTIATION→ACTIVE 审批门控）/电子签触发类最高危敏感动作；按 P1.3 收敛粒度不需脱离 `mutation` 桶；管控由 SUBM 可见性（合同专员/合同审批人可见合同列表）覆盖，与 P1.4a submit / P1.4c create Decision 同型。(b) 全声明独立 FNPT（拒绝：过度拆分）。选定 (a)，运营迁移留 `:mutation` 桶，本计划仅声明 activate（审批门控）。残留风险：`terminate`（合同终止）有财务/法务影响，若后续要求"仅合同审批人可终止"，须升格（successor）。
  - Skill: none
- [x] **Add**：在 delta `erp-ct.action-auth.xml` 为 contract 电子签 + 审批门控敏感动作声明独立 per-action FNPT 点，挂在对应实体 `<resource id="Erp*-main">` 的 `<children>` 下（参照 finance `ErpFinVoucher:post`/`:reverse` + b2b `handleInboundWebhook` 范式）。`<permissions>` = `{Entity}:{action}`；`roles` 种子按 `roles-and-permissions.md` §第二批扩展域 A 角色职责：
  - **ErpCtSignatureRequest**：`:initSignatureRequest`→合同审批人（电子签触发，发起授权）、`:handleSignatureCallback`→合同审批人（webhook 入站最高危，与 b2b handleInboundWebhook 同型）、`:queryAndUpdateStatus`→合同专员（运营轮询回查）、`:cancelSignatureRequest`→合同审批人（授权终止签章）、`:rejectSignature`→合同审批人（授权拒绝签章）。
  - **ErpCtContractVersion**：`:signVersion`→合同审批人（FINALIZED→SIGNED 门控）、`:finalizeVersion`→合同专员（DRAFT→FINALIZED 版本管理）。
  - **ErpCtContract**：`:activate`→合同审批人（NEGOTIATION→ACTIVE 审批门控，设计明定）。
  - 具体权限点 ID 执行时按生成文件 `_erp-ct.action-auth.xml` 核验（不在本计划冻结逐点清单，遵循 P1.3「不冻结具体动作清单」残留风险）。
  - 落地证据：`ErpCtContract-main`（orderNo=100）挂 `ErpCtContract:activate`(10010,合同审批人) + `ErpCtContractVersion:finalizeVersion`(10020,合同专员) + `ErpCtContractVersion:signVersion`(10030,合同审批人)；`ErpCtSignatureRequest-main`（orderNo=170）挂 5 点 initSignatureRequest/handleSignatureCallback/cancelSignatureRequest/rejectSignature→合同审批人(17010/17020/17040/17050) + queryAndUpdateStatus→合同专员(17030)。`ErpCtContractVersion` 无独立 `-main`（delta 仅暴露合同列表页），其 FNPT 挂 `ErpCtContract-main` children（permission 仍 `ErpCtContractVersion:*` 正确 scope）。
  - Skill: `nop-backend-dev`
- [x] **Proof**：xmllint well-formed 校验 `erp-ct.action-auth.xml` 通过 + `permissionToRoles` 静态映射一致性自检（电子签触发/callback/签章门控/activate→合同审批人；版本管理/轮询→合同专员；角色名与 §第二批扩展域 A 一致）。
  - 证据：`xmllint --noout erp-ct.action-auth.xml` PASS；grep 实测 8 FNPT roles 全部一致（合同审批人×6：activate/signVersion/init/handleCallback/cancel/reject；合同专员×2：finalizeVersion/queryAndUpdateStatus）。
  - Skill: none

Exit Criteria:

- [x] 运营迁移 FNPT 处理 Decision 落地（留 `:mutation` 桶）+ contract 8 动作（SignatureRequest 5 + ContractVersion 2 + Contract activate）per-action FNPT 声明落地，xmllint 通过，roles 种子与 contract 角色基线一致。

### Phase 2 - hr 薪酬 voidSalary per-action FNPT 声明

Status: completed
Targets: `module-hr/erp-hr-web/src/main/resources/_vfs/erp/hr/auth/erp-hr.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Decision` / `Add` / `Proof`
- Prereqs: Phase 1（范式复用）；P1.3（done）；**软协调 P1.6**（ErpHrSalary approve binding 裁决已 done——本计划 voidSalary 对齐薪酬审批人，与 approve/markPaid 同角色）

- [x] **Decision**：裁决 hr 薪酬 `voidSalary` 升格为独立 FNPT，以及计算/生成类（`calculateSalary`/`runPayroll`/`generateBankFile`）的处理。
  - **voidSalary 升格**：实测 `voidSalary`(@BizMutation L103) 在 `paymentStatus` 非 PAID 时置 VOID、PAID 时抛 `ERR_SALARY_LOCKED_AFTER_PAID`——即**作废已审核未发放薪酬**（非反向冲销已发放资金）。考虑的替代方案：(a) 声明独立 `:voidSalary` FNPT——**采纳**：voidSalary 作用于经 `:approve`(薪酬审批人) 授权的薪酬记录，将其置 VOID 是对审核授权记录的敏感状态变更（移除已审核薪酬 + 影响下游过账/银行盘生成），与 approve/markPaid 同属"经审核授权的薪酬记录生命周期终态"管控面，须独立门控脱离泛化 `mutation` 桶；(b) 留 `:mutation` 桶——**拒绝**：与既有 approve/markPaid 已独立门控不对称，作废已审核记录不应坍缩进泛化桶。roles 种子→**薪酬审批人**（与 approve/markPaid 同角色，对齐 P1.6 薪酬 approve binding 裁决）。
  - **计算/生成类处理**：考虑的替代方案：(a) 计算/生成类（calculateSalary/runPayroll/generateBankFile）留 `:mutation` 桶——**采纳**：这些是运算/批量生成动作（核算单薪/批量跑薪/生成银行盘），非 approve/markPaid/void 类对审核授权记录的终态变更；按 P1.3 收敛粒度不需脱离 `mutation` 桶；管控由 SUBM 可见性（薪酬审批人/HR 专员可见薪酬）+ 既有 `:approve`/`:markPaid`/`:voidSalary` 门控覆盖，与 P1.4a submit / Phase 1 运营迁移 Decision 同型；(b) 声明独立 FNPT（拒绝：过度拆分，计算属运算非授权终态）。选定 (a)，计算/生成类留 `:mutation` 桶。残留风险：若后续要求"仅薪酬审批人可跑薪/生成银行盘"，须升格（successor）。
  - Skill: none
- [x] **Add**：在 delta `erp-hr.action-auth.xml` 为 `ErpHrSalary:voidSalary` 声明独立 per-action FNPT 点，挂在 `ErpHrSalary-main` 的 `<children>` 下（参照既有 `ErpHrSalary:approve`/`:markPaid` 范式 L101-108）。`<permissions>` = `ErpHrSalary:voidSalary`；`roles` 种子→**薪酬审批人**（见上 Decision）。voidSalary 是 xwf 实体 ErpHrSalary 的动作——声明仅落权限点 + 种子，**测试策略（DIRECT 三轴 / xwf 轴后端单测）归 P1.6，不在本计划**。
  - 落地证据：`ErpHrSalary-main`（orderNo=500）children 增 `FNPT:ErpHrSalary:voidSalary`(orderNo=50030,roles=薪酬审批人)，紧随既有 approve(50010)/markPaid(50020)，`<permissions>ErpHrSalary:voidSalary</permissions>`。
  - Skill: `nop-backend-dev`
- [x] **Proof**：xmllint well-formed 校验 `erp-hr.action-auth.xml` 通过 + `permissionToRoles` 一致性自检（voidSalary→薪酬审批人，与既有 approve/markPaid 同角色对称）。
  - 证据：`xmllint --noout erp-hr.action-auth.xml` PASS；grep 实测 `voidSalary` roles=薪酬审批人，与 approve/markPaid 三点同角色对称。
  - Skill: none

Exit Criteria:

- [x] voidSalary 升格 Decision + 计算/生成类留 `:mutation` 桶 Decision 落地（行为描述以实测 ERR_SALARY_LOCKED_AFTER_PAID 为准）+ ErpHrSalary:voidSalary per-action FNPT 声明落地，xmllint 通过，roles 种子与薪酬审批人对齐 P1.6 裁决 + 既有 markPaid 对称。

### Phase 3 - owner doc 实现注记 + 日志

Status: completed
Targets: `docs/design/roles-and-permissions.md` §action-level 声明层 + §既有种子证据
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2

- [x] **Add**：§action-level 声明层「已落地」表（L202-217）增列 contract（SignatureRequest 5 + ContractVersion 2 + Contract activate）+ hr voidSalary per-action FNPT 点 + roles 种子（delta 文件行号证据），与既有行对齐；§灰度推进路线（L223）首批灰度域增列 contract（电子签 + activate 门控）、hr voidSalary 补齐；§既有种子证据块增 contract + hr voidSalary 行号。
  - 落地证据：§已落地表 hr ErpHrSalary 行补 `:voidSalary` + 新增 contract 3 行（SignatureRequest 电子签全生命周期 / ContractVersion 签章门控 / Contract activate）；§灰度推进路线补 contract 电子签 + hr voidSalary 移出「其余」（9 点由 plan P1.4d 补齐）；§既有种子证据由「7 域」扩为「9 域」并补 contract（L17-27/L88-106）+ hr voidSalary（L109-111）行号；日志 `docs/logs/2026/08-09.md` 增 P1.4d 条目（时间倒序置顶）。
  - Skill: none

Exit Criteria:

- [x] owner doc 实现注记落地，与 delta 文件真相源一致；§灰度推进路线 contract + hr voidSalary 标记已补齐。

## Draft Review Record

- Independent draft review iteration 1: needs revision（**1 blocker** / 0 major / 3 minor）（ses_01a3e76a0ffewdM4uYxgntEmv2）。**B1** voidSalary 行为描述与实测代码相反——代码 `voidSalary`(@BizMutation L103) 在 paymentStatus 非 PAID 时置 VOID、PAID 时抛 `ERR_SALARY_LOCKED_AFTER_PAID`（作废已审核**未发放**薪酬），计划误述为「反向冲销已发薪酬/资金变动」，致 Phase 2 Decision 前提失实（Rule 1）；m1 contract BizModel 行号 off-by-one（引 @Override 行，应引 @BizMutation）；m2 handleSignatureCallback 角色类比未注角色层级差；m3 占位符卫生提醒。delta 文件基线（contract 零 FNPT + hr 3 既有 FNPT）+ contract 无 useWorkflow + ErpHrSalary useWorkflow + 模块区分均实测确认准确；Deps（P1.3 done，P1.6 done）确认；anti-slack 通过；范式与 P1.4a/b 同构。
- 合并修订（iteration 1 → v2）：**B1** 全量订正 voidSalary 行为描述（作废已审核未发放薪酬 + ERR_SALARY_LOCKED_AFTER_PAID 锁定），Phase 2 升格为独立 Decision（voidSalary 升格 + 计算/生成类留桶两 Decision），重接地于"对审核授权记录的终态变更"敏感度，roles 薪酬审批人保留；m1 contract 行号订正为 @BizMutation 行（init L98/handle L107/query L117/cancel L124/reject L140/finalize L46/signVersion L58）；m2 注角色层级差；m3 占位符待独立复审填。
- Independent draft review iteration 2: accept（0 blocker / 0 major / 1 minor，信息性）（ses_01a364ae9ffe4q4WKNRnK9LWb4）。**B1 完全解决**——voidSalary 行为描述与实测代码精确一致（paymentStatus 非 PAID→置 VOID；PAID→抛 ERR_SALARY_LOCKED_AFTER_PAID；活跃正文无残留「反向冲销已发薪酬/资金变动/markPaid 的反向冲销」误述），Phase 2 Decision 重接地于「对审核授权记录的终态变更」（移除已审核薪酬 + 影响下游过账/银行盘），经实测 `existsNonVoidSalary`/`findPayableSalaries` 验证下游影响成立；contract 7 mutations + 1 query 行号均实测对齐 @BizMutation/@BizQuery 行；Rule 14 bundling（同 roadmap 工作项 + 同 owner doc + 同结果面）成立；Rule 9/13 + plan-first + protected-area（enforcement OFF、禁改生成文件）通过。residual m-info-1：handleSignatureCallback 「同型」措辞未显式注角色层级差（信息性，「同型」指 webhook 入站最高危分类，合同审批人归属独立由授权终止论证支撑，无需动作）。
- Plan Status → active（两轮独立审查共识，0 blocker / 0 major）。

## Closure Gates

> 本计划改 delta action-auth.xml（声明层，enforcement 保持 OFF，不改运行时行为）。Closure Gates 运行 delta XML well-formed + compliance checker 对照 `known-good-baselines.md` 零漂移（横切关注点 7）。完整 build/test 在此运行一次。

- [x] 范围内行为完成（contract 8 + hr voidSalary 1 = 9 个 per-action FNPT 声明 + roles 种子 + 两个 Decision + owner doc 注记）
- [x] 相关文档对齐（`roles-and-permissions.md` §action-level 声明层 + §灰度推进路线 + §既有种子证据）
- [x] 已运行验证：`xmllint --noout` 两个 delta 文件 + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移 + `mvn clean install -DskipTests`
- [x] 无范围内项目降级为 deferred/follow-up（运营迁移 / 计算-生成类留 `:mutation` 桶为 Decision 选定项，非降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### contract 运营生命周期（suspend/resume/terminate/expire/amend）升格为独立 FNPT 点

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 1 Decision 已裁决运营迁移留 `:mutation` 桶（非 activate 审批门控/电子签触发类最高危；P1.3 收敛粒度不需独立点；管控由 SUBM 可见性覆盖）。`terminate`（合同终止）有财务/法务影响为重点观察项。
- Successor Required: yes（触发条件 = 出现"仅合同审批人可终止/暂停"的精细化需求，或终止门控审计要求）

### hr 薪酬计算/生成类（calculateSalary/runPayroll/generateBankFile）升格为独立 FNPT 点

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 2 Decision 已裁决计算/生成类留 `:mutation` 桶（运算/批量生成非对审核授权记录的终态变更；P1.3 收敛粒度不需独立点）。
- Successor Required: yes（触发条件 = 出现"仅薪酬审批人可跑薪/生成银行盘"的精细化需求）

### 其他 hr approve（ErpHrSalarySimulation/Timesheet/ShiftSwapRequest）per-action 声明

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 设计 §action-level 声明层未将 simulation/timesheet/shift-swap approve 列为敏感子集（这些审批非薪酬机密核心）；本计划聚焦 contract 电子签 + hr 薪酬审核（approve/markPaid/voidSalary）。
- Successor Required: yes（触发条件 = 考勤/排班/仿真审批纳入敏感动作灰度批准前）

### SUBM 菜单组层 roles 映射

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SUBM 粗粒度角色映射归 P1.5a 静态种子补全；本计划聚焦敏感动作 per-action FNPT。
- Successor Required: yes（触发条件 = P1.5a 进入）

## Closure

Status Note: 独立结束审计（新会话，非执行者上下文）通过。9 个 per-action FNPT 声明（contract 8 + hr voidSalary 1）+ 两个 Decision + owner doc 注记全部实测落地，enforcement 保持 OFF，无范围内项目降级，无 Hollow（声明均为含 permissions + roles 的完整 FNPT 元素，经 app.action-auth.xml `x:extends` 接入）。Deferred 三项均为 Decision 选定留桶或显式 out-of-scope，已带 successor 触发条件，非隐藏缺陷。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（MISSION_DRIVER:2026-08-09-075057-mission-driver，新会话，非执行者）
- Evidence: 
  - 复审 delta 文件真相源：`module-contract/erp-ct-web/.../erp-ct.action-auth.xml` 实测 8 FNPT——`ErpCtContract:activate`(L17-19,合同审批人,orderNo=10010)/`ErpCtContractVersion:finalizeVersion`(L21-23,合同专员,10020)/`:signVersion`(L25-27,合同审批人,10030)/`ErpCtSignatureRequest:initSignatureRequest`(L88-90,合同审批人,17010)/`:handleSignatureCallback`(L92-94,合同审批人,17020)/`:queryAndUpdateStatus`(L96-98,合同专员,17030)/`:cancelSignatureRequest`(L100-102,合同审批人,17040)/`:rejectSignature`(L104-106,合同审批人,17050)。
  - `module-hr/erp-hr-web/.../erp-hr.action-auth.xml` 实测 `ErpHrSalary:voidSalary`(L109-111,薪酬审批人,orderNo=50030，紧随既有 approve 50010/markPaid 50020，未动既有声明)。
  - 重跑 `xmllint --noout` 两 delta 文件均 PASS（well-formed）。
  - owner doc `docs/design/roles-and-permissions.md` 实测：§action-level 已落地表 hr ErpHrSalary 行补 `:voidSalary`(L215) + 新增 contract 3 行(L219-220)；§灰度推进路线补 contract 电子签 + hr voidSalary 9 点(L227)；§既有种子证据由 7 域扩 9 域补 contract(L251)+hr voidSalary(L247)。
  - 日志 `docs/logs/2026/08-09.md` P1.4d 条目实测存在且时间倒序置顶(L3+)。
  - 五点一致性：Plan Status completed / 3 Phase 全 completed / 3 Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 证据落地，均一致；执行者未自我勾选审计门控（plan 自述「除结束审计项待独立审计」，由本独立会话补勾）。
  - 反 Hollow：FNPT 为 resourceType=FNPT 元素含 `<permissions>` 子节点 + `roles` 属性，挂 `-main` children 经 app.action-auth.xml `x:extends` 接入运行时权限声明层（enforcement OFF 时不拦截但权限点已就绪可授权），无空体/return null。
  - Deferred 诚实性：3 项（contract 运营迁移 / hr 计算-生成类留 `:mutation` 桶；其他 hr approve / SUBM 映射 out-of-scope）均带 successor 触发条件，无隐藏缺陷或契约漂移。

Follow-up:

- 非 successor（已在 Deferred But Adjudicated 记录触发条件，不在此重复列缺陷）。
