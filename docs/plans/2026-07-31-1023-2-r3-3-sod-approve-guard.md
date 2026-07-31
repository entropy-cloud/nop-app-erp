# 2026-07-31-1023-2-r3-3-sod-approve-guard R3.3 — 职责分离（创建人≠审核人）程序级强制

> Plan Status: active
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR3 R3.3（P1-MA6-001）
> Related: `docs/plans/2026-07-31-0958-1-r3-0-mr3-p1-finding-expansion.md`（R3.0 展开 R3.3）；`docs/audits/2026-07-29-1410-arm-ma6-permission-depth-sampling.md`（P1-MA6-001）；`docs/plans/2026-07-31-0310-2-r2-7-api-contract-consistency.md`（action-level RBAC，独立维度）
> Audit: required

## Current Baseline

**P1-MA6-001**：4 个 S/A 级域（finance/manufacturing/purchase/sales）的 approve 路径调用 `setApprovedBy(currentUserId())` 但**零** `createdBy`/`getCreatedBy` 比对——单据创建人可自审。owner doc `docs/design/roles-and-permissions.md §职责分离` 声明"单据创建人与审核人不可为同一人"，但标题标注"（建议配置）"（软门控），代码层完全不提供程序级强制入口。SoD 是**独立于 action-level RBAC（P1-MA3-046/R2.7）的工作流级不变量**——即使审核人角色门控落地，同一人若同时持创建+审核角色仍可自审。

**MR5 后 approve 实现模式按实体（非按域）分三类（关键，影响修复位置；实测 grep `public.*approve(String id` 全 4 域 *ApproveProcessor 核实）：**

- **Pattern A（基类单点）**：per-mutation Processor **不** override `public approve()`，委托共享基类 `module-common-service/.../AbstractApproveProcessor.java`，`setApprovedBy(currentUserId())` 在基类 `doApprove:43-47` 单点发生。基类类型界 `<T extends OrmEntity>` 无法泛型读 `getCreatedBy()`，但 `AbstractProcessor` 已有 `readStatus`/`entity.orm_propValueByName(...)` 反射读字段能力（`:64-67`）。
  - 实体：`ErpPurOrder`、`ErpPurRequisition`（purchase）、`ErpSalOrder`、`ErpSalQuotation`（sales）。
- **Pattern B（facade doApprove 委托）**：per-mutation Processor override `public approve()` 并委托 facade `processor.doApprove(...)`，setter 在 facade。
  - 实体：`ErpPurInvoice`/`ErpPurPayment`/`ErpPurReceive`/`ErpPurReturn`（purchase，如 `ErpPurInvoiceApproveProcessor.approve` → `processor.doApprove`）、finance（`ErpFinExpenseClaimProcessor.doApprove:254`、`ErpFinEmployeeAdvanceProcessor.doApprove:162`）、manufacturing（`ErpMfgWorkOrderProcessor.doApprove:347`、`ErpMfgSubcontractOrderProcessor`）。
- **Pattern C（per-mutation Processor inline setter）**：per-mutation Processor override `public approve()` 且 `setApprovedBy(currentUserId())` **inline 在该 Processor 自身**（不委托 facade 的 doApprove 做 setter）。
  - 实体：`ErpSalInvoice:35`、`ErpSalReceipt:35`、`ErpSalReturn`、`ErpSalDelivery`（sales；类头自标"模式 B"但实为 inline setter 的 Pattern C）。
- **非范围**：所有 `*ReverseApproveProcessor` 是冲销动作（reverseApprove），语义不同，本 plan Non-Goal 已排除。

**守卫插入点（三模式各异）**：Pattern A→基类 `doApprove`（1 处）；Pattern B→各 facade `doApprove`（每实体 1 处）+ purchase Pattern-B per-mutation Processor 经 facade 命中；Pattern C→各 per-mutation Processor 的 `approve()` inline（每实体 1 处）。统一策略：上提一个共享 `assertApproverNotCreator(createdBy, ctx)` helper 至 `AbstractProcessor`，三模式各自在 setter 前调用。

**已确认的 API/模型事实：**
- `createdBy` 是每实体 ORM 列（VARCHAR precision=50，存 userId），生成实体暴露 `entity.getCreatedBy(): String`。`approvedBy` 是独立列（precision=36，stdDomain=userId）。两者均 String，可直接 `Objects.equals(...)`。
- `currentUserId()` 在 `AbstractProcessor.java:69-76`，经 `IUserContext.get().getUserId()` 取值；**可能返回 null**（wf 回调上下文未填充线程局部，见 `TestErpPurPaymentWorkflowApproval.java:89-90` 既有注记）。
- ErrorCode 模式：每域 `ErpXxxErrors.java` interface 常量 `ErrorCode.define("erp.err.<domain>.<entity>.<reason>", "<中文描述{占位符}>", argKeys...)`，抛 `throw new NopException(ERR_...).param(ARG_..., v)`。描述为中文（i18n 处理翻译）。

**测试可行性约束（必须在 plan 内消解）：**
- 设置特定 userId 在**直调 BizModel** 路径已有先例：`TestErpPurPaymentApprovalNotifications.java:192` `setUser(String userId)` 经 `ContextProvider.getOrCreateContext().setUserId(userId)` 指定 SUBMITTER userId（lines 74/108/134 调用）。但本 plan 负向测试拟经 GraphQL RPC over `IGraphQLEngine`（对齐多数 approve 测试范式），该路径下如何注入特定 userId 仍须 Explore 确认（GraphQL RPC vs 直调的 userId 注入差异）。
- approve 测试多经 GraphQL RPC（非直调 BizModel——无 OrmSession 会失败，见 lessons/04）。负向断言范式：`assertEquals(ErpXxxErrors.ERR_....getErrorCode(), bad.getCode())`。sales `TestErpSalOrderApproval.java:400-410` `approveWithAuthChecker` 示范了 `IActionAuthChecker` 注入。
- `createdBy` 通常由平台在 insert 时自动填充；负向测试须显式置 `createdBy == currentUserId()` 才能复现自审（须确认 GraphQL 测试引擎默认 userId + `setCreatedBy` 可访问性）。

**owner doc 现状：** `roles-and-permissions.md §职责分离（建议配置）:43-47` 声明规则但软化；`purchase/state-machine.md:124`、`sales/state-machine.md:96` 各有一句"建议不可同一人"；**`finance/state-machine.md` 与 `manufacturing/state-machine.md` 无任何 SoD 语句**（grep 0 匹配）。审计 watch-item §6 #2 明确"SoD 仅抽样 4 S/A 域……MR3 修复时全域铺开"。

**审计引用行号已陈旧**：审计引 `ErpPurOrderProcessor.doApprove:335-337`，MR5 后该逻辑已移至基类 `AbstractApproveProcessor.doApprove:43-47`。

剩余差距：4 域 approve 路径（三模式 ~14 个 approve 实体）零 SoD 程序级守卫；owner doc 软化且 finance/mfg 缺语句；无 SoD 负向测试。

## Goals

- 在 4 域 approve 路径前置共享 `assertApproverNotCreator` 守卫：比对 `createdBy` 与 `currentUserId()`，相等抛 `NopException(ERR_*_APPROVER_IS_CREATOR)`。
- 统一覆盖三类模式（Pattern A 基类单点 + Pattern B facade 各点 + Pattern C per-mutation inline 各点），不重复实现（共享 helper）。
- 为每域新增 `ERR_*_APPROVER_IS_CREATOR` ErrorCode（中文描述）。
- 明确 null-user 语义并测试（守卫在 `currentUserId()` 为 null 时的行为，覆盖 Pattern A 实体 + wf 回调路径）。
- owner doc 对齐：`roles-and-permissions.md §职责分离` 去"（建议配置）"软化或标注"程序级强制已落地"；`finance/state-machine.md`、`manufacturing/state-machine.md` 补 SoD 语句（对齐 pur/sal）。
- 新增 SoD 负向测试（创建人=审核人 → approve 抛指定 ErrorCode）覆盖 4 域代表性实体。
- arm-index P1-MA6-001 回填 `MR3 done (R3.3)`。

## Non-Goals

- action-level RBAC（P1-MA3-046/R2.7，独立维度，已 deferred enforcement flip）。
- 扩展域（crm/cs/hr/...）approve 路径全域铺开（审计 watch-item #2；本 plan 聚焦抽样的 4 S/A 域，扩展域登记为 successor）。
- nop-wf 审批流层 SoD 配置（方案 B，审计 watch-item #1 能力未实证；本 plan 采用代码层方案 A）。
- reverseApprove/withdrawApproval 的 SoD（reverseApprove 是冲销不是审批，语义不同；本 plan 仅 approve）。
- 其余 MR3 工作项。

## Task Route

- Type: `implementation-only change`（Processor/ErrorCode 业务逻辑 + owner doc + 测试；不改 ORM/API 契约）
- Owner Docs: `docs/design/roles-and-permissions.md §职责分离`；`docs/design/{finance,manufacturing,purchase,sales}/state-machine.md`；`docs/audits/arm-index.md`
- Skill Selection Basis: 触及 BizModel/Processor 后端业务逻辑 → `nop-backend-dev`（Processor hook、跨实体、ErrorCode、事务边界）。负向测试编写 → `nop-testing`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 守卫位置 Decision + 接口契约（Explore|Decision）

Status: planned
Targets: `module-common-service/.../AbstractApproveProcessor.java`；各域 facade Processor
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 无

- [ ] Explore: 确认 GraphQL RPC 测试路径下 `currentUserId()` 的注入方式——已知**直调 BizModel** 路径有 `TestErpPurPaymentApprovalNotifications.java:192` `setUser(userId)`（经 `ContextProvider.getOrCreateContext().setUserId`），但本 plan 负向测试拟经 GraphQL RPC over `IGraphQLEngine`（对齐多数 approve 测试）。须确认 GraphQL RPC 路径如何注入特定 userId + 生成实体 `setCreatedBy` 可访问性。产出可复现的负向测试编排路径。
  - Skill: none
- [ ] Decision: 守卫 createdBy 读取方式 + null-user 语义 + scope。考虑的替代方案与残留风险：
  - **createdBy 读取**：(a) 在 `AbstractProcessor` 用既有反射能力 `entity.orm_propValueByName("createdBy")` 提供**非抽象默认** `getCreatedBy(T)`，三模式共享 helper `assertApproverNotCreator` 直接调，零样板；(b) `AbstractApproveProcessor` 增 `protected abstract String getCreatedBy(T)` hook 强制 ~14 子类各实现（样板多，被拒）。预期选 (a)。
  - **守卫位置（按模式）**：Pattern A→基类 `doApprove` setter 前单点调 helper；Pattern B→各 facade `doApprove` setter 前调 helper（含 purchase Pattern-B per-mutation Processor 经 facade 命中）；Pattern C→各 per-mutation Processor `approve()` inline setter 前调 helper。三模式共享同一 helper（上提至 `AbstractProcessor`），不重复实现。
  - **null-user 语义**：`currentUserId()` 为 null 时（wf 回调）——(i) 保守阻断（null→抛错）；(ii) 跳过守卫（null→放行）。须选其一并记录理由（影响 wf 回调路径下 approve 是否仍工作）。残留风险：选 (i) 可能破坏既有 wf 回调 approve（须用 `TestErpPurPaymentWorkflowApproval` 回归验证）；选 (ii) 留回调路径自审窗口。
  - **scope**：4 域全部 approve 实体（Pattern A 4 + Pattern B 8[pur4+fin2+mfg2] + Pattern C 4[sal] = ~16 实体）vs 仅审计抽样的代表性 4 实体。审计 watch-item #2 要求全域铺开；预期选全部。finance `postVoucher` 等非 approve 路径（非 `AbstractApproveProcessor`，是过账动作）须裁决是否在范围（预期排除——非审批语义）。
  - Skill: none（裁决）；落地用 `nop-backend-dev`

**接口契约（结构边界定义，非实现伪代码）：**
- 共享 helper `assertApproverNotCreator(String createdBy, IServiceContext ctx)` 上提至 `AbstractProcessor`（全 4 域 Processor 均可调，签名稳定）；内部 `createdBy != null && Objects.equals(createdBy, currentUserId(ctx))` → 抛 `NopException(ERR_*_APPROVER_IS_CREATOR).param(ARG_USER_ID, userId)`；否则放行。null-user 行为按 Decision。
- Pattern A：基类 `AbstractApproveProcessor.doApprove` 在 `setApprovedBy` 前调 `assertApproverNotCreator(getCreatedBy(entity), context)`；`getCreatedBy(T)` 在 `AbstractProcessor` 以 `entity.orm_propValueByName("createdBy")` 提供非抽象默认（Pattern A 子类零改动即可命中守卫）。
- Pattern B：各 facade `doApprove`（finance/mfg + purchase Pattern-B facade）在 setter 前调同一 helper。
- Pattern C：各 sales per-mutation Processor `approve()` inline setter 前调同一 helper。
- 边界不变量：三模式任一 approve 路径执行 `setApprovedBy` 前必经 `assertApproverNotCreator`（grep 可验证：无 setter 前缺守卫的 approve 路径）。

Exit Criteria:

- [ ] Explore 产出 GraphQL RPC 负向测试编排路径（userId 注入 + setCreatedBy 可访问性确认）
- [ ] Decision 记录 createdBy 读取方式 + 守卫位置（三模式）+ null-user 语义 + scope（含 finance postVoucher 排除裁决）+ 替代方案 + 残留风险

### Phase 2 - 守卫 + ErrorCode 落地（4 域，三模式）

Status: planned
Targets: `module-common-service/.../AbstractProcessor.java` + `AbstractApproveProcessor.java`；各域 `Erp*ApproveProcessor.java` + facade `Erp*Processor.java` + `ErpXxxErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 Decision

- [ ] Add: 共享 helper `assertApproverNotCreator` + 非抽象默认 `getCreatedBy`（反射读 createdBy）上提至 `AbstractProcessor`。
  - Skill: `nop-backend-dev`
- [ ] Add: Pattern A——基类 `AbstractApproveProcessor.doApprove` setter 前调 helper（pur Order/Requisition + sal Order/Quotation 命中）。
  - Skill: `nop-backend-dev`
- [ ] Add: Pattern B——各 facade `doApprove` setter 前调 helper（pur Invoice/Payment/Receive/Return facade + fin ExpenseClaim/EmployeeAdvance + mfg WorkOrder/SubcontractOrder）。
  - Skill: `nop-backend-dev`
- [ ] Add: Pattern C——各 sales per-mutation Processor `approve()` inline setter 前调 helper（sal Invoice/Receipt/Return/Delivery）。
  - Skill: `nop-backend-dev`
- [ ] Add: 每域 `ErpXxxErrors.java` 增 `ERR_*_APPROVER_IS_CREATOR`（中文描述，如"审核人与单据创建人不可为同一人（违反职责分离）：{userId}"），按 `erp.err.<domain>.<entity>.approver-is-creator` 命名。
  - Skill: `nop-backend-dev`
- [ ] Fix: 确认守卫在既有 approve 测试下不误伤——既有测试若同用户创建+审核须调整编排（用不同用户或断言新错误码）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三模式守卫落地，4 域全部 approve 实体 setter 前有守卫（grep 验证：每个 `setApprovedBy(currentUserId())` 前有 `assertApproverNotCreator` 调用，无遗漏路径）
- [ ] 4 域 ErrorCode 常量落地（中文描述）

### Phase 3 - owner doc 对齐 + 负向测试

Status: planned
Targets: `docs/design/roles-and-permissions.md`；`docs/design/{finance,manufacturing}/state-machine.md`；4 域 approve 测试类
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: `roles-and-permissions.md §职责分离` 去"（建议配置）"或标注"程序级强制已落地（R3.3）"；`finance/state-machine.md` + `manufacturing/state-machine.md` 补 SoD 语句（对齐 pur/sal 既有表述）。
  - Skill: none
- [ ] Proof: 4 域各加 SoD 负向测试——置 `createdBy == currentUserId()`，approve → 断言抛 `ERR_*_APPROVER_IS_CREATOR`（按 Phase 1 Explore 编排路径）。**null-user 覆盖**：至少 1 个 Pattern A 实体 + 1 个 wf 回调路径（如 `TestErpPurPaymentWorkflowApproval`）验证所选 null-user 语义不破坏既有 wf 回调 approve。沿用 GraphQL RPC 范式 + `assertEquals(errorCode, bad.getCode())`。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] owner doc 三处对齐（roles-and-permissions + finance + manufacturing state-machine）
- [ ] 4 域 SoD 负向测试落地，自审被阻断可证明（指定验证：4 域 approve 测试类含 ERR_*_APPROVER_IS_CREATOR 断言且通过）

### Phase 4 - arm-index 回填 + 日志

Status: planned
Targets: `docs/audits/arm-index.md` §P1 详细清单；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 3

- [ ] Add: arm-index P1-MA6-001「修复状态」回填 `MR3 done (R3.3)`，附守卫位置/null-user 语义/scope 裁决指针 + 扩展域 successor 注记。
  - Skill: none
- [ ] Add: 追加 `docs/logs/2026/07-31.md` 条目（R3.3 守卫落地 + ErrorCode + owner doc + 负向测试）。
  - Skill: none
- [ ] Proof: 一致性复核——grep 4 域 setApprovedBy 前均有守卫；arm-index P1-MA6-001 非裸 todo。
  - Skill: none

Exit Criteria:

- [ ] arm-index P1-MA6-001 回填，无裸 todo
- [ ] 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: needs-revision (task `ses_049f80b6bffehtTXRpyEo0dW37`) because Pattern 分类按域（Pattern A=pur/sal、Pattern B=fin/mfg）是 **false**——实测 grep `public.*approve(String id` 全 4 域 *ApproveProcessor 证实模式按**实体**分三类：Pattern A（基类单点：pur Order/Requisition + sal Order/Quotation）、Pattern B（facade doApprove 委托：pur Invoice/Payment/Receive/Return + fin + mfg）、**Pattern C**（per-mutation Processor inline setter：sal Invoice/Receipt/Return/Delivery，此前未识别）。原接口契约称基类 `getCreatedBy` hook 覆盖 pur/sal Pattern A 是 false——Pattern B/C 的 pur/sal 实体 override `public approve()` 不经基类。已修订：baseline 改为按实体三模式枚举 + 守卫插入点；Phase 1 Decision 增 createdBy 读取方式（反射非抽象默认 vs 抽象 hook）+ 三模式位置 + null-user；接口契约重写覆盖三模式；Phase 2 拆三模式 item；Phase 3 null-user 覆盖 Pattern A + wf 回调；closure gate 引用 postVoucher 排除裁决。非阻塞采纳：setUser() 直调先例（TestErpPurPaymentApprovalNotifications:192）已补入 baseline。
- Independent draft review iteration 2: accept (task `ses_049e89006ffe12qTvmq4qqgeFJ`) — 5 项 resolution check 全 resolved，零 blocking。三模式按实体枚举（A: PurOrder/Requisition+SalOrder/Quotation；B: PurInvoice/Payment/Receive/Return+Fin+mfg；C: SalInvoice/Receipt/Return/Delivery inline setter）经 grep `public.*approve(String id` 全 4 域复核确认；接口契约覆盖三模式守卫插入点；createdBy 反射默认 readStatus:64-67 可行；null-user 覆盖 Pattern A + wf 回调；postVoucher（ErpFinVoucherBizModel:88 过账动作无 setApprovedBy）排除裁决 + closure gate 引用。- Independent draft review iteration 1: _（待独立子代理审查填充）_

## Closure Gates

- [ ] 范围内行为完成（4 域三模式 ~16 approve 实体守卫 + ErrorCode + owner doc + 负向测试含 null-user 覆盖；finance postVoucher 非审批路径经 Phase 1 裁决显式排除并记理由）
- [ ] 相关文档对齐（roles-and-permissions + finance/manufacturing state-machine + arm-index + 日志）
- [ ] 已运行验证（4 域 touched 模块 `mvn test` + 全量 `mvn clean install -DskipTests`）
- [ ] 无范围内项目降级为 deferred/follow-up（扩展域全域铺开为显式 Non-Goal successor，非 in-scope 降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 扩展域（crm/cs/hr/...）approve 路径 SoD 全域铺开

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 审计 P1-MA6-001 抽样范围为 4 S/A 域；扩展域 approve 路径未在审计中确认存在同型缺口（部分扩展域无标准审批 S-mutation 或无 approve 路径）。本 plan 聚焦审计确认的 4 域。
- Successor Required: `yes`（触发条件 = 扩展域 approve 路径 SoD 抽样审计，或任一扩展域出现自审回归时）

### nop-wf 审批流层 SoD 配置（方案 B）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本 plan 采用代码层方案 A（程序级守卫）已闭合 P1-MA6-001；nop-wf step-owner≠submitter 配置能力未实证（审计 watch-item #1），属平台能力调研 successor。
- Successor Required: `yes`（触发条件 = nop-wf SoD 配置能力经实证可用后，评估迁移守卫至工作流层）

### null-user 路径残留（视 Phase 1 Decision）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 若 Decision 选 null-user 跳过守卫，wf 回调路径下 approve 留自审窗口；此为既有 Processor `currentUserId()` null 限制（非本 plan 引入），且 wf 回调通常经审批流本身已有 SoD 语义。
- Successor Required: `yes`（触发条件 = wf 回调路径 approve 自审被证实可利用时，补回调上下文 userId 填充）

## Closure

Status Note: _（待 EXECUTE + 独立结束审计后填充）_

Closure Audit Evidence:

- Auditor / Agent: _（独立结束审计子代理新会话，待填充）_
- Evidence: _（待填充）_

Follow-up:

- _（非阻塞跟进见 Deferred But Adjudicated）_
