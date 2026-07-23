# 2026-07-23-1145-2-cross-cutting-sensitive-operation-confirmations Cross-Cutting Sensitive Operation Confirmation Flow (Delete/Reverse/Disable Preview)

> Plan Status: active
> Last Reviewed: 2026-07-23
> Source: `docs/backlog/frontend-ui-roadmap.md` 退出标准 line 574（`[ ] 敏感操作确认流程落地（删除引用预览、反审核冲销预览、停用业务影响预览）`）+ 跨域建议 §10（line 530）+ §6（line 526）
> Related: `docs/plans/2026-07-20-1020-2-f7-non-status-visibleon-and-master-data-interactions.md`（F7 删除引用预览 + 停用 Switch 范式先例，ErpMdMaterial/ErpMdPartner/ErpMdSubject 3 实体已落地）
> Audit: required

## Current Baseline

基于实时仓库抽样核实（2026-07-23，对 F7 落地范围 + SPI 接口 + finance reverse mutation + 全域 view.xml delete/disable 按钮的审计）：

- **F7 已落地 3 实体的删除引用预览**（plan `2026-07-20-1020-2`）：
  - `ErpMdMaterialBizModel`：`isCodeUnique` @BizQuery + `countReferences` @BizQuery（经 `IErpMdMaterialReferenceChecker` SPI）+ view.xml 删除引用预览 dialog + status Switch 控件 + 停用确认 dialog
  - `ErpMdPartnerBizModel`：同上范式（经 `IErpMdPartnerReferenceChecker` SPI）
  - `ErpMdSubjectBizModel`：`isCodeUnique` @BizQuery + async blur 校验（无 `countReferences` — 科目会计语义上可停用不可删除，F1 已移除删除按钮）
- **SPI Reference Checker 接口已扩展至 Employee + Organization，但无生产实现**：
  - `IErpMdEmployeeReferenceChecker.countReferences(Long employeeId)` — `module-master-data/erp-md-dao/.../spi/IErpMdEmployeeReferenceChecker.java:26`
  - `IErpMdOrganizationReferenceChecker.countReferences(Long organizationId)` — `module-master-data/erp-md-dao/.../spi/IErpMdOrganizationReferenceChecker.java:29`
  - `ErpPartyBizModel.findReferences(partyType, partyId, context)`（`ErpPartyBizModel.java:113-140`，**非** `countReferences`）调用两者（employee checker line 128, organization checker line 133），但 checker 注入为 `@Nullable`（line 64-68）— **无生产实现时返回 `Collections.emptyMap()`**。`IErpPartyBiz.java:64-65` javadoc 明确标注"下游实现归 Deferred"。
  - 仅 Material/Partner 有测试 stub（`TestStubMaterialReferenceChecker`、`TestStubPartnerReferenceChecker`），**无 Employee/Organization 的生产或 stub 实现**。
  - API 形态差异：F7 前端调实体级 `@query:ErpMdMaterial__countReferences?id=$id`（单 id 参数），Employee/Organization 引用计数经 `@query:ErpParty__findReferences?partyType=EMPLOYEE&partyId=$id`（不同 BizModel、不同参数 shape）。
- **Employee/Organization 均保留 delete 按钮**：`ErpHrEmployee.view.xml:364` 显式 `<action id="row-delete-button"/>`；`ErpMdOrganization.view.xml:83` `<crud name="main"/>` 无 rowActions override，继承 codegen 默认含 delete。删除引用预览有消费路径。
- **反审核冲销预览：全域零实现**。finance 域有多个 reverse `@BizMutation`：
  - `ErpFinVoucherBizModel.reverse(billHeadCode, businessType)` line 71 — bill 级红字冲销（经 PostingEvent 反向）；`reverseVoucher(voucherId)` line 94 — 单凭证级红字冲销
  - `ErpFinReconciliationBizModel.reverse(reconciliationId)` line 143 — 核销单冲销（**不生成 GL 凭证**，仅反转辅助账 SETTLED→OPEN + refresh partner balance，line 49 注释明确）
  - `ErpFinBankReconciliation__reverse`、`ErpFinBadDebt__reverseApprove`、`ErpFinAccountingPeriod__reverseClose` 同存
  - **无任何 `@BizQuery` 预览冲销影响**、**无任何 view.xml 冲销确认 dialog**。冲销操作直接执行 mutation。
- **停用业务影响预览：仅 ErpMdMaterial/ErpMdPartner 2 实体有停用确认 dialog**（F7 落地）。其余 master-data 实体（Employee/Organization/Warehouse/Location/Currency 等）的 status 变更无前端确认。
- **F7 删除引用预览范式**：实际为 reference-**blocker** dialog（`ErpMdMaterial.view.xml:349-385`）— 调 `countReferences` → 有引用时弹 dialog "无法删除：存在引用" + 仅"知道了"按钮（阻断删除），无引用时静默放行。非 preview-then-confirm 流程。

剩余差距：
1. Employee/Organization 有 delete 按钮但无删除引用预览 dialog；后端 SPI 接口存在但无生产实现（`@Nullable` 注入返回空 Map）
2. Finance 域 reverse mutation 无冲销预览（用户盲操作红字凭证生成 / 辅助账回退）
3. 其余 master-data 实体停用无业务影响预览

## Goals

- Employee 和 Organization 获得 F7 同范式的删除引用阻断 dialog（含生产 SPI 实现 + 前端 dialog）
- Finance 域 `ErpFinVoucher__reverseVoucher`（单凭证红字冲销）和 `ErpFinReconciliation__reverse`（核销单冲销）获得冲销预览 dialog（预览将生成的红字凭证/将回退的辅助账状态）
- 范式文档记录删除引用预览扩展 + 冲销预览新模式
- 对应测试覆盖

## Non-Goals

- 不修改后端 BizModel 状态机或 reverse mutation 逻辑（仅新增 `@BizQuery` 预览方法，不动既有 `@BizMutation`）
- 不为全域所有实体实现删除/停用/冲销预览（仅覆盖最高价值子集：Employee/Organization 删除预览 + Finance 核心 reverse 预览）
- 不实现 ErpMdSubject 删除引用预览（科目不可删除，F1 已移除删除按钮，`TestErpMdSubjectBiz.java:27` 明确归 Non-Goal）
- 不实现非 master-data 实体的删除引用预览（业务实体如 PurOrder/SalOrder 的删除由状态机守卫控制，非引用计数模式）
- 不实现页级像素视觉回归
- 不修改 ORM 模型

## Task Route

- Type: `app-layer design change`（新增 `@BizQuery` 预览方法 = 公共读 API 契约扩展；新增 SPI 生产实现 = 跨域契约补全）
- Owner Docs: `docs/design/visible-on-patterns.md`（master-data 交互范式）；`docs/design/page-structure-patterns.md`（dialog 确认范式）
- Skill Selection Basis: `nop-frontend-dev`（view.xml dialog + gen-control tpl + visibleOn 是前端核心技能）；`nop-backend-dev`（新增 `@BizQuery` 冲销预览方法 + SPI 生产实现需后端技能）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 0 — F7 Pattern Alignment Decision

Status: planned
Targets: N/A（裁决记录在计划中）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [ ] `Decision`: F7 删除引用范式对齐裁决。F7 实际实现为 reference-**blocker** dialog（有引用时阻断删除 + "知道了"按钮，无引用时静默放行），非 preview-then-confirm 流程。Employee/Organization 删除引用预览应：(a) 复制 F7 blocker 范式（有引用时阻断，一致性最优）；(b) 改为 preview-then-confirm（展示引用列表 + 用户确认后仍可删除，更灵活但偏离 F7）。裁决依据：一致性 vs 灵活性权衡。记录选择 + 替代方案。
  - Skill: `none`

Exit Criteria:

- [ ] F7 范式对齐 Decision 已记录（blocker vs preview-then-confirm 裁决）

### Phase 1 — Employee/Organization SPI Implementation + Delete Reference Blocker

Status: planned
Targets: `module-master-data/erp-md-service/`（SPI 生产实现）；`module-hr/erp-hr-web/.../ErpHrEmployee.view.xml`；`module-master-data/erp-md-web/.../ErpMdOrganization.view.xml`
Skill: `nop-backend-dev` | `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 完成

- [ ] `Add`: 实现 `IErpMdEmployeeReferenceChecker` 生产类（在对应 service 模块），`countReferences(Long employeeId)` 查询引用该员工的开放业务单据（在职工时/薪酬记录/考勤等），返回 `Map<String, Long>`（对齐 Material/Partner SPI 范式）。注册到 IoC 容器。
  - Skill: `nop-backend-dev`
- [ ] `Add`: 实现 `IErpMdOrganizationReferenceChecker` 生产类，`countReferences(Long organizationId)` 查询引用该组织的开放业务单据，返回 `Map<String, Long>`。注册到 IoC 容器。
  - Skill: `nop-backend-dev`
- [ ] `Add`: 在 `ErpHrEmployee.view.xml` 删除按钮增引用阻断 dialog（依 Phase 0 裁决：blocker or preview-then-confirm）。调 `@query:ErpParty__findReferences?partyType=EMPLOYEE&partyId=$id` → 有引用时弹 dialog 展示引用计数 → 依裁决阻断 or 确认后执行。注意 API 形态差异：经 `ErpParty__findReferences`（非实体级 `countReferences`），参数为 `partyType + partyId`。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 在 `ErpMdOrganization.view.xml`（或 master-data 对应实体 view）增同范式引用阻断 dialog。`ErpMdOrganization.view.xml:83` 当前 `<crud name="main"/>` 无 rowActions override — 需先 override rowActions 显式声明 delete 按钮 + 引用阻断 dialog。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] Employee/Organization 删除前弹出引用阻断/预览 dialog（依 Phase 0 裁决）
- [ ] 引用计数经生产 SPI 实现返回非空真实数据（非 `@Nullable` emptyMap）
- [ ] 本地化验证：master-data/hr service + web 模块 typecheck 通过

### Phase 2 — Finance Reverse Posting Preview (Voucher + Reconciliation)

Status: planned
Targets: `module-finance/erp-fin-service/.../ErpFinVoucherBizModel.java`；`module-finance/erp-fin-service/.../ErpFinReconciliationBizModel.java`；`module-finance/erp-fin-web/.../ErpFinVoucher.view.xml`；`module-finance/erp-fin-web/.../ErpFinReconciliation.view.xml`
Skill: `nop-backend-dev` | `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 完成（Phase 2 不依赖 Phase 1）

- [ ] `Add`: 在 `ErpFinVoucherBizModel` 新增 `@BizQuery previewReverseVoucher` 方法（入参 `voucherId`，对齐 `reverseVoucher(voucherId)` 入口 line 94）— 返回冲销预览 DTO：原凭证号/凭证字/金额/凭证行数 + 红字凭证预估（同向取负金额预览）+ 原凭证 `isReversed` 将置 true + 关联 `ErpFinVoucherBillR` 回链的域单据将 posted→false + approveStatus→REJECTED 列表。不执行实际冲销。
  - Skill: `nop-backend-dev`
- [ ] `Add`: 在 `ErpFinVoucher.view.xml` 的 reverseVoucher 按钮（或新增 row-action）增冲销预览 dialog — 点击后先调 `@query:ErpFinVoucher__previewReverseVoucher?voucherId=$id` → dialog 展示预览 DTO → 用户确认后执行 `@mutation:ErpFinVoucher__reverseVoucher?voucherId=$id`。
  - Skill: `nop-frontend-dev`
- [ ] `Add`: 在 `ErpFinReconciliationBizModel` 新增 `@BizQuery previewReverse` 方法（入参 `reconciliationId`）— 返回冲销预览 DTO：核销单号/金额 + **将反转的 AR/AP 辅助账 SETTLED→OPEN 列表** + partner balance 将刷新预估。**注意：核销冲销不生成 GL 凭证**（BizModel line 49 明确），预览 DTO 不含红字凭证段。
  - Skill: `nop-backend-dev`
- [ ] `Add`: 在 `ErpFinReconciliation.view.xml` 的 reverse 按钮增冲销预览 dialog — 点击后先调 `@query:ErpFinReconciliation__previewReverse?reconciliationId=$id` → dialog 展示预览 DTO（辅助账回退列表 + partner balance 影响）→ 用户确认后执行 `@mutation:ErpFinReconciliation__reverse?reconciliationId=$id`。
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] `previewReverseVoucher` @BizQuery 返回结构化冲销预览（原凭证信息 + 红字预估 + bill_r 回退列表）
- [ ] `previewReverse`(Reconciliation) @BizQuery 返回辅助账回退列表（无红字凭证段，对齐 BizModel 行为）
- [ ] view.xml reverse 按钮点击后先展示预览 dialog，用户确认后才执行实际 reverse mutation
- [ ] 本地化验证：finance service + web 模块 typecheck 通过

### Phase 3 — Tests + Pattern Doc Update

Status: planned
Targets: `tests/e2e/visual/sensitive-operation-confirmations.visual.spec.ts`；`tests/e2e/business-actions/reverse-preview.action.spec.ts`；`docs/design/visible-on-patterns.md`；`docs/design/page-structure-patterns.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 + Phase 2 完成

- [ ] `Add`: 新建 `sensitive-operation-confirmations.visual.spec.ts` — 断言 Employee/Organization 删除（或停用）引用预览 dialog 渲染 + finance reverse 预览 dialog 渲染。
  - Skill: `nop-testing`
- [ ] `Add`: 新建 `reverse-preview.action.spec.ts` — 经 GraphQL 调 `previewReverse` @BizQuery 断言返回结构（原凭证信息 + 红字预估 + 回退列表），然后调实际 `reverse` mutation 验证预览与实际一致。
  - Skill: `nop-testing`
- [ ] `Add`: 在 `docs/design/visible-on-patterns.md` 增 §删除引用预览扩展段（Employee/Organization 范式 + party 统一接口裁决）+ §冲销预览新模式段（previewReverse @BizQuery + dialog 确认范式）。
  - Skill: `none`

Exit Criteria:

- [ ] visual spec 断言 3 类预览 dialog（Employee/Organization + finance reverse）渲染正确
- [ ] action spec 断言 previewReverse 返回结构非空 + 与实际 reverse 结果一致

## Draft Review Record

- Independent draft review iteration 1: **needs revision** (`ses_072e54d5cffeKYIxiPGKRN16xK`) — 5 blockers: (1) Employee/Organization delete 按钮存在性可从仓库直接确认不应留 Decision ✅fixed（baseline 已述事实）; (2) SPI 无生产实现，"后端就绪"虚假 ✅fixed（baseline 已纠正 + Phase 1 增显式 SPI 生产实现 work item）; (3) 方法名 findReferences 非 countReferences + API shape 差异 ✅fixed; (4) Finance scope 应在 baseline 裁决 ✅fixed（commit to Voucher + Reconciliation）; (5) Task Route 误分类 implementation-only ✅fixed（改为 app-layer design change）。4 recommendations: Reconciliation reverse 不生成凭证 ✅fixed; Voucher reverse 入口 billHeadCode vs voucherId 区分 ✅fixed; F7 blocker vs preview-then-confirm 范式差异 → Phase 0 Decision; previewReverse DTO shape 应更明确 → 实现期细化。
- Independent draft review iteration 2: **accept** (`ses_072dd166cffe8jYP0peNLM1oTJ`) — 无 blocker。迭代 1 的 5 blockers 全部修复（delete 按钮事实化 ✅、SPI 无生产实现纠正 + 显式 work item ✅、findReferences API shape ✅、finance scope 裁决 ✅、Task Route 重分类 ✅）+ 4 recommendations 全部 addressed（Reconciliation 无凭证 ✅、Voucher reverse 入口区分 ✅、F7 blocker 范式事实化 + Phase 0 Decision ✅、previewReverse DTO 字段列出 ✅）。3 项非阻塞建议：reverseVoucher line 94→95 off-by-one（实现期修正）、previewReverse DTO 副作用镜像（实现期验证）、Phase 0 Decision 可降级为 constraint（保留为 Decision 可接受）。计划可晋 `active`。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。

- [ ] 范围内行为完成（Phase 0-3 全部 done）
- [ ] 相关文档对齐（visible-on-patterns.md 增段）
- [ ] 已运行验证（`mvn clean install -DskipTests` + `npx playwright test sensitive-operation` + `npx playwright test reverse-preview`）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 全域删除/停用引用预览扩展

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划覆盖最高价值子集（Employee/Organization + Finance reverse）。其余 master-data 实体（Warehouse/Location/Currency/ProjectType 等）和业务实体停用预览为长尾，按需逐域补齐
- Successor Required: `yes`（触发条件：各域细化端到端验证推进到对应实体时）

### 非 Finance 域冲销预览

- Classification: `optimization candidate`
- Why Not Blocking Closure: Finance 域 reverse 最高风险（红字凭证 + 辅助账回退 + 域单据状态回滚）。其余域 reverse（如 quality NCR reverseNcr）已有 E2E 覆盖且风险较低
- Successor Required: `yes`（触发条件：其余域 reverse 操作被标记为高风险时）

## Closure

Status Note: <pending>

Closure Audit Evidence:

- Auditor / Agent: <pending>

Follow-up:

- 全域删除/停用引用预览扩展 successor（依触发条件）
- 非 Finance 域冲销预览 successor（依触发条件）
