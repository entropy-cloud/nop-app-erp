# 2026-08-09-1400-2 per-action-fnpt-purchase-sales-approval

> Plan Status: active
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.4a
> Related: mission `permissions-enforcement`；P1.3（粒度裁决，已 done，提供收敛粒度）；P1.1（敏感字段清单，已 done，提供敏感动作输入）；P1.6（xwf 语义裁决，draft，提供 submit 处理输入——非硬前置，软协调）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.4a

## Current Baseline

P1.3 已裁决映射收敛粒度 = 角色×SUBM + **敏感动作 per-action FNPT** + 兜底策略（管理员=平台 admin 兜底 + 业务角色显式种子，双命名空间分离）。approve/反审核属敏感动作，**应作为独立 per-action FNPT 点脱离泛化 `mutation` 桶**（P1.3 拒绝方案 b 的理由）。

**purchase/sales 审批集 approve-path 动作现状**（实测 `_erp-pur.action-auth.xml` / `_erp-sal.action-auth.xml` 生成文件 + service Processor）——生成文件每实体仅 `:query`/`:mutation` 两 FNPT 点，敏感 approve 动作全部坍缩进 `mutation` 桶；delta `erp-pur.action-auth.xml` / `erp-sal.action-auth.xml` **未声明任何 per-action FNPT**（无 `<children>` FNPT 块）：

- **purchase** approve-path 实体（service Processor 实测，6 实体均含 `*ApproveProcessor` + `*ReverseApproveProcessor`）：ErpPurRequisition、ErpPurOrder、ErpPurReceive、ErpPurInvoice、ErpPurPayment（含 `ErpPurPaymentApproveProcessor`/`ErpPurPaymentReverseApproveProcessor`）、ErpPurReturn。标准审批动作 = submitForApproval/approve/reject/reverseApprove。
- **sales** approve-path 实体（**两种实现模式**，见 `docs/design/sales/state-machine.md` §实现模式与守卫边界 P1-MA2-056/057）：(a) **PROC 路径**——ErpSalOrder、ErpSalDelivery、ErpSalInvoice、ErpSalQuotation、ErpSalReturn、ErpSalReceipt（含 `ErpSal*Processor`）；(b) **INLINE 路径**——ErpSalContract，其 5 动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）直接在 delta `ErpSalContract.xbiz` 的 `<mutation><source>` 脚本实现（无 Processor 类，但有完整 approve 路径 + `TestErpSalContractReverseApprove.java` 覆盖）。**ErpSalContract 必须纳入审批集声明**——其 `:approve`/`:reverseApprove` 是真实敏感 mutation（reverseApprove=管理员，见 state-machine.md §6）。注：销售合同 ErpSalContract（module-sales）≠ contract 域 ErpCtContract/ErpCtSignatureRequest（module-contract，P1.4d 范围的电子签）。

**SoD 程序级守卫**（plan 2026-07-31-1023-2 R3.3）：purchase/sales approve 路径已落地 SoD——比对单据 `createdBy` 与审核人 `userId`，相等抛 `erp.err.<domain>.approver-is-creator`（共享守卫 `app.erp.common.service.SoDGuard`）。SoD 是程序级（与 RBAC FNPT 正交），enforcement 翻转不依赖本计划但与 approve 敏感动作语义互补。

**与 P1.6 的交叉**：ErpPurPayment 与 ErpSalReceipt 是 P1.6 的 4 个 `useWorkflow="true"` 实体之二（xwf 浏览器层不可达，DIRECT 三轴可达，见 2330-1 裁决）。本计划为其声明 `:approve` FNPT（权限点 + roles 种子）；其 enforcement **测试策略**（DIRECT 三轴浏览器层负向）归 sibling P1.6（`2026-08-09-1400-1`）裁决——声明与测试策略分工，无重叠。

**enforcement 状态**：`nop.auth.enable-action-auth=false`（默认 OFF），本计划仅"已就绪可授权"，不拦截任何调用；`roles` 属性在 enforcement OFF 时不生效，翻转后并入 `permissionToRoles` 校验。

**缺口**：pur/sal approve-path 敏感动作无独立 per-action FNPT 点 + roles 种子——enforcement 翻转后这些敏感动作会坍缩进泛化 `mutation` 桶，丧失独立管控（违背 P1.3 收敛粒度）。

## Goals

- **补齐 purchase/sales 审批集 per-action FNPT 声明**：为 approve-path 敏感动作（approve / reverseApprove 反审核；submitForApproval 留 `:mutation` 桶，见 Phase 1 Decision）声明独立 per-action FNPT 权限点（脱离 `mutation` 桶），并承载 `<resource ... roles="...">` 静态 role-resource 种子，保持 enforcement OFF。覆盖 sales 全 7 approve-path 实体（含 ErpSalContract INLINE 路径）。
- **roles 种子对齐角色矩阵**：approve→审核人、reverseApprove→管理员（业务角色「管理员」，与平台 admin 兜底双命名空间分离——P1.3 横切 2）；submit 留 `:mutation` 桶（本计划内部 Decision）。
- **每集群独立交付 + 独立回归**：purchase 与 sales 各自声明 + 各自核验（permissionToRoles 一致性 + xmllint well-formed + compliance 零漂移）。

## Non-Goals

- **不翻转 enforcement 开关**（归 P2.4/E1.x）。
- **不产 auth 表 CSV 种子**（角色/用户/账号归 P1.5b；本计划仅 `roles` 静态属性种子，等价 `nop_auth_role_resource` 静态种子）。
- **不做 SUBM 菜单组层 roles 映射**（SUBM 粗粒度归 P1.5a 静态种子补全；本计划聚焦敏感动作 per-action FNPT）。
- **不改生成文件** `_erp-*.action-auth.xml`（真相源，AGENTS.md 规则 7）；声明只在 delta 非生成文件 `erp-*.action-auth.xml`。
- **不改 SoD 程序级守卫**（已落地，与 FNPT 正交）。
- **不动 finance/b2b/mfg/inv/hr 既有 per-action 声明**（本计划仅 pur/sal）。

## Task Route

- Type: `implementation-only change`（delta action-auth.xml per-action FNPT 声明补齐 + roles 种子，enforcement 保持 OFF，不改运行时行为）
- Owner Docs: `docs/design/roles-and-permissions.md` §action-level 声明层、§角色→权限点映射
- Skill Selection Basis: `nop-backend-dev` —— delta action-auth.xml per-action FNPT 声明属后端权限声明层工作（与 roadmap 表格 P1.4a Skill 列一致）；本计划聚焦声明层，不写 BizModel/Processor 代码（approve 业务逻辑已由 R3.3 SoD + 既有 Processor 落地）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（仅 delta XML 声明，enforcement 保持 OFF，不改运行时）。

## Execution Plan

### Phase 1 - purchase 审批集 per-action FNPT 声明

Status: planned
Targets: `module-purchase/erp-pur-web/.../_vfs/erp/pur/auth/erp-pur.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add` / `Decision` / `Proof`
- Prereqs: P1.3（done）；P1.1（done，敏感动作输入）

- [ ] **Decision**：裁决 submitForApproval（提交审核）的 FNPT 处理。考虑的替代方案：(a) submit 留 `:mutation` 桶不声明独立点——submit 是提交动作（创建人发起），非 approve/reverseApprove 类最高危敏感动作，按 P1.3 收敛粒度不需脱离 `mutation` 桶；(b) submit 声明独立 `:submitForApproval` FNPT（拒绝：过度拆分，submit 的管控由 SUBM 菜单可见性 + SoD 程序级守卫覆盖，非 per-action 层职责）。选定 (a)，submit 留 `:mutation` 桶，本计划不声明 submit 独立点。残留风险：若后续要求"仅特定角色可提交"，须升格（successor）。
  - Skill: none
- [ ] **Add**：在 delta `erp-pur.action-auth.xml` 为 purchase approve-path 敏感动作声明独立 per-action FNPT 点（approve / reverseApprove），挂在对应实体 `<resource id="ErpPur*-main">` 的 `<children>` 下（参照 finance delta `ErpFinVoucher:post`/`:reverse` 范式）。`<permissions>` = `{Entity}:{action}`；`roles` 种子：approve→审核人、reverseApprove→管理员。具体实体×动作清单执行时按生成文件 `_erp-pur.action-auth.xml` 权限点 ID 核验 + 各域 state-machine.md「角色与权限」节核对（不在本计划冻结逐点清单，遵循 P1.3「不冻结具体动作清单」残留风险）。
  - Skill: `nop-backend-dev`
- [ ] **Proof**：xmllint well-formed 校验 `erp-pur.action-auth.xml` 通过 + `permissionToRoles` 静态映射一致性自检（approve→审核人、reverseApprove→管理员，角色名与 `roles-and-permissions.md` §角色体系一致）。
  - Skill: none

Exit Criteria:

- [ ] submit FNPT 处理 Decision 落地（留 `:mutation` 桶）+ purchase 审批集敏感动作 per-action FNPT 声明落地，xmllint 通过，roles 种子与角色矩阵一致。

### Phase 2 - sales 审批集 per-action FNPT 声明

Status: planned
Targets: `module-sales/erp-sal-web/.../_vfs/erp/sal/auth/erp-sal.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add` / `Proof`
- Prereqs: Phase 1（purchase 范式确立后复用）；P1.3（done）

- [ ] **Add**：在 delta `erp-sal.action-auth.xml` 为 sales approve-path 敏感动作声明独立 per-action FNPT 点（approve / reverseApprove），范式复用 Phase 1。roles 种子：approve→审核人、reverseApprove→管理员。实体×动作清单执行时按生成文件 `_erp-sal.action-auth.xml` + sales state-machine.md 核对。**含 ErpSalContract**（INLINE xbiz 路径，但其 `:approve`/`:reverseApprove` FNPT 权限点声明方式与 PROC 实体一致——权限点与实现模式无关，挂在 `ErpSalContract-main` 的 `<children>` 下）。
  - Skill: `nop-backend-dev`
- [ ] **Proof**：xmllint well-formed 校验 `erp-sal.action-auth.xml` 通过 + `permissionToRoles` 一致性自检。
  - Skill: none

Exit Criteria:

- [ ] sales 审批集敏感动作 per-action FNPT 声明落地，xmllint 通过，roles 种子与角色矩阵一致。

### Phase 3 - owner doc 实现注记 + 日志

Status: planned
Targets: `docs/design/roles-and-permissions.md` §action-level 声明层
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2

- [ ] **Add**：§action-level 声明层「已落地」表增列 purchase/sales 审批集 per-action FNPT 点 + roles 种子（行号证据），与既有 finance/b2b/mfg/inv/hr 行对齐。
  - Skill: none

Exit Criteria:

- [ ] owner doc 实现注记落地，与 delta 文件真相源一致。

## Draft Review Record

- Independent draft review iteration 1: needs revision（0 blocker / 2 major / 1 minor）（ses_01adf76c8ffe33h3tzoG0HK2kS）。M1 ErpSalContract 列入 sales approve-path 但其为 bare CrudBizModel 无 approve 路径（无 Processor/xbiz/state-machine 覆盖）；M2 submitForApproval deferral 指向 P1.6 Phase 2 作 successor，但 P1.6 范围为 4 xwf 实体，不拥有 pur/sal submit，触发条件永不成立（反 slack）；m1 purchase 实体动作括注低估 reverseApprove（6 实体均含 Approve + ReverseApprove）。
- 合并修订（iteration 1 → v2）：sales approve-path 移除 ErpSalContract（注明归 contract 域 P1.4d）；submit 改为 Phase 1 内部 Decision（选定留 `:mutation` 桶，含替代方案 + 残留风险，不再依赖 P1.6 successor）；purchase 实体动作括注改为统一「6 实体均含 Approve + ReverseApprove」；补 ErpPurPayment/ErpSalReceipt 与 P1.6 交叉说明（声明在此、测试策略在 P1.6）。
- Independent draft review iteration 2: needs revision（1 blocker / 1 major / 0 minor）（ses_01ad8eda3ffeueCi7YFcTEiCMP）。B1 iteration-1 的 M1（ErpSalContract 无 approve 路径）**前提事实错误**——ErpSalContract 经 INLINE xbiz 路径有完整 5 动作（`ErpSalContract.xbiz` delta `<mutation><source>` + state-machine.md:160-170 P1-MA2-056/057 + `TestErpSalContractReverseApprove.java`），v2 误删致覆盖缺口（reverseApprove=管理员 坍缩进 mutation 桶，违背 P1.3）；M1 Goals 残留 "submitForApproval 由 P1.6 Phase 2 裁决对齐" 与 Phase 1 内部 Decision 矛盾（文本不一致，规则 11）。
- 合并修订（iteration 2 → v3）：**恢复** ErpSalContract 入 sales approve-path（注明 INLINE xbiz 路径 + 全 5 动作 + reverseApprove=管理员，区分 module-sales ErpSalContract ≠ module-contract P1.4d 电子签）；Phase 2 显式纳入 ErpSalContract（权限点声明与实现模式无关）；Goals 去 P1.6 submit 引用，改"submit 留 :mutation 桶（本计划内部 Decision）"。
- Independent draft review iteration 3: accept（0 blocker / 0 major / 0 minor）（ses_01ad55853ffeJiyzqckSnb3klg）。B1/M1 RESOLVED 无回归：ErpSalContract 准确恢复为 sales 第 7 approve-path 实体（INLINE xbiz 5 动作，实测 `ErpSalContract.xbiz` + state-machine.md §实现模式 P1-MA2-056/057 + TestErpSalContractReverseApprove.java；区分 module-sales ≠ module-contract P1.4d 电子签）；Phase 2 显式纳入 ErpSalContract（权限点声明与实现模式无关）；Goals 去 P1.6 submit 引用，改"submit 留 :mutation 桶（本计划内部 Decision）"，与 Phase 1 Decision + Deferred 一致（规则 11 文本一致性扫描无矛盾）。purchase 6 实体（Approve+ReverseApprove）、ErpPurPayment/ErpSalReceipt↔P1.6 交叉、submit 内部 Decision 均保持。Plan Status → active。

## Closure Gates

> 本计划改 delta action-auth.xml（声明层，enforcement 保持 OFF，不改运行时行为）。Closure Gates 运行 delta XML well-formed + compliance checker 对照 `known-good-baselines.md` 零漂移（横切关注点 7）。完整 build/test 在此运行一次。

- [ ] 范围内行为完成（purchase + sales 审批集 per-action FNPT 声明 + roles 种子 + owner doc 注记）
- [ ] 相关文档对齐（`roles-and-permissions.md` §action-level 声明层）
- [ ] 已运行验证：`xmllint --noout` 两个 delta 文件 + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移 + `mvn clean install -DskipTests`（标准构建验证）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### submitForApproval 升格为独立 FNPT 点

- Classification: `watch-only residual`
- Why Not Blocking Closure: Phase 1 Decision 已裁决 submit 留 `:mutation` 桶（submit 非最高危敏感动作，P1.3 收敛粒度不需独立点；管控由 SUBM 可见性 + SoD 守卫覆盖）。本计划不声明 submit 独立点。
- Successor Required: yes（触发条件 = 出现"仅特定角色可提交"的精细化需求，submit 须升格为独立 `:submitForApproval` FNPT）

### SUBM 菜单组层 roles 映射

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: SUBM 粗粒度角色映射归 P1.5a 静态种子补全；本计划聚焦敏感动作 per-action FNPT。
- Successor Required: yes（触发条件 = P1.5a 进入）

## Closure

Status Note: <待执行后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立审计者或独立子代理>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
