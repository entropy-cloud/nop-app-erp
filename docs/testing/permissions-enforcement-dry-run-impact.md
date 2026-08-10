# 权限 enforcement dry-run 影响面清单（P2.4）

> 生成：2026-08-10（plan `2026-08-10-0741-1` Phase 3 / P2.4 dry-run 门控）
> 配置基线：`enable-action-auth=true`（%test profile，L62 翻启）+ `skip-check-for-admin=true` + data-auth 双开关 OFF + role-row-filter OFF
> runner jar：`app-erp-all-1.0-SNAPSHOT-runner.jar`（2026-08-10 07:56 build）
> 产出 spec：`tests/e2e/negative/dry-run-impact.smoke.spec.ts`
> 主体账号：`role-restricted`（userId=10，绑平台 `user` 角色，无敏感 FNPT 授权）
> 子集边界：P1.4a-d per-action FNPT 已补齐域（fin/pur/sal/mfg/ast/b2b/ct/hr/inv）的 `FNPT:` 声明动作，共 61 项

## 三类分布汇总

| 分类 | 计数 | 占比 | 含义 | E1.1 消费指引 |
|------|------|------|------|---------------|
| **denied**（enforcement 真拒绝） | 5 | 8% | FNPT 声明 + 角色种子 + checker 三层联动生效——`nop.err.auth.no-permission` | 已闭环，E1.1 复用为负向断言锚点 |
| **bypassed**（enforcement 未拒绝，业务校验前置） | 28 | 46% | role-restricted 调用 FNPT 声明动作，**未** 被权限拦截，进入业务逻辑（哨兵 id 999999 → `*-not-found` / `unknown-entity`） | **E1.1 高优先级**：调查 approve/reverseApprove 模式为何未触发 FNPT checker（CrudBizModel 内建方法权限模型 / FNPT 名不匹配 / checker 路径未覆盖） |
| **inconclusive-arg-mismatch**（探针 arg 不匹配） | 28 | 46% | 动作不接受 `id` 参数（自定义 arg 名），GraphQL arg 校验先于 enforcement → `nop.err.graphql.undefined-field-arg`，enforcement 未被触达 | E1.1 消费前**须修正探针 arg 名**（按各 BizModel 方法签名），再分类 denied vs bypassed |

**关键 finding**：61 项子集中仅 5 项（8%）被 enforcement 真正拒绝。28 项 approve/reverseApprove 模式动作 bypass（最显著覆盖缺口），28 项探针 arg 不匹配（待 E1.1 修正后归类）。即「per-action FNPT 已声明」≠「enforcement 已覆盖」——FNPT 声明层与 checker 执行层之间存在覆盖缺口，E1.1 须按本清单逐域闭环。

## denied 清单（5 项，enforcement 真拒绝）

| 域 | 动作 | FNPT 声明角色 | role-restricted 结果 | 说明 |
|----|------|---------------|---------------------|------|
| fin | `ErpFinBadDebt.writeOff` | 财务员 | denied (`nop.err.auth.no-permission`) | 自定义 @BizMutation + FNPT 声明 + checker 覆盖——三层联动闭环 |
| fin | `ErpFinBadDebt.reverseApprove` | 管理员 | denied | 同上 |
| hr | `ErpHrSalary.markPaid` | 薪酬审批人 | denied | 同上 |
| hr | `ErpHrSalary.voidSalary` | 薪酬审批人 | denied | 同上 |
| inv | `ErpInvLandedCost.approve` | （P1.5a 种子） | denied | approve 动作中唯一被 enforcement 覆盖的——可能是该方法显式声明了 action-auth 注解 |

## bypassed 清单（28 项，enforcement 未拒绝，业务校验前置）

> **统一特征**：全部是 `approve` / `reverseApprove` 模式动作。role-restricted 调用进入业务逻辑（哨兵 id 999999 → `*-not-found`），未被 `nop.err.auth.no-permission` 拦截。
>
> **根因假设**（E1.1 须确认）：`approve`/`reverseApprove` 可能是 `CrudBizModel`/状态机基类的内建方法，其权限检查走 SUBM 级（`ErpXxx:*`）或无显式 FNPT 注解，而 action-auth.xml 的 `FNPT:ErpXxx:approve` 仅是菜单/按钮级声明，未绑定到 BizModel 方法的 enforcement 路径。role-restricted（平台 `user` 角色）可能因 checker 不进入该路径而被放行。

| 域 | 实体 | 动作 | 业务返回 |
|----|------|------|----------|
| pur | ErpPurRequisition | approve / reverseApprove | `erp.err.pur.req-not-found` |
| pur | ErpPurOrder | approve / reverseApprove | `erp.err.pur.order-not-found` |
| pur | ErpPurReceive | approve / reverseApprove | `erp.err.pur.receive-not-found` |
| pur | ErpPurInvoice | approve / reverseApprove | `erp.err.pur.invoice-not-found` |
| pur | ErpPurPayment | approve / reverseApprove | `erp.err.pur.payment-not-found` |
| pur | ErpPurReturn | approve / reverseApprove | `erp.err.pur.return-not-found` |
| sal | ErpSalQuotation | approve / reverseApprove | `erp.err.sal.quotation-not-found` |
| sal | ErpSalOrder | approve / reverseApprove | `erp.err.sal.order-not-found` |
| sal | ErpSalContract | approve / reverseApprove | `nop.err.dao.unknown-entity` |
| sal | ErpSalDelivery | approve / reverseApprove | `erp.err.sal.delivery-not-found` |
| sal | ErpSalReceipt | approve / reverseApprove | `erp.err.sal.receipt-not-found` |
| sal | ErpSalInvoice | approve / reverseApprove | `erp.err.sal.invoice-not-found` |
| sal | ErpSalReturn | approve / reverseApprove | `erp.err.sal.return-not-found` |
| mfg | ErpMfgSubcontractOrder | approve | `erp.err.mfg.subcontract-order.not-found` |
| ast | ErpAstDisposal | approve | `erp.err.ast.disposal.not-found` |

**E1.1 消费次序建议**：本 28 项是高危覆盖缺口（approve/reverseApprove 是单据审批核心动作）。E1.1 优先调查根因（CrudBizModel 内建方法权限模型），按域分批：pur 12 → sal 14 → mfg 1 + ast 1。

## inconclusive-arg-mismatch 清单（28 项，探针 arg 不匹配，待 E1.1 修正）

> **统一特征**：动作不接受 `id` 参数（自定义 arg 名，如 `voucherId` / `workOrderId` / `ediDocId` / `signatureRequestId` 等）。GraphQL arg 校验先于 enforcement check → `nop.err.graphql.undefined-field-arg`，enforcement 未被触达。
>
> **E1.1 消费前**：须按各 BizModel 方法签名修正探针 arg 名，再分类 denied vs bypassed。这些动作的 enforcement 状态**未确定**（inconclusive）。

| 域 | 实体 | 动作（需 arg 名修正） |
|----|------|----------------------|
| fin | ErpFinVoucher | post / reverse |
| fin | ErpFinAccountingPeriod | closePeriod / reverseClose |
| mfg | ErpMfgWorkOrder | start / close / cancel |
| b2b | ErpB2bEdiDoc | markSent / cancel / markAcknowledged / markError / retry / archive |
| b2b | ErpB2bAsn | handleInboundWebhook / matchPurchaseOrder / createReceiveFromAsn / retryMatch |
| ct | ErpCtContract | activate |
| ct | ErpCtContractVersion | finalizeVersion / signVersion |
| ct | ErpCtSignatureRequest | initSignatureRequest / cancelSignatureRequest / handleSignatureCallback / queryAndUpdateStatus / rejectSignature |
| hr | ErpHrSalary | approve（markPaid/voidSalary 已 denied，approve 探针 arg 不匹配） |
| hr | ErpHrLeaveRequest | approve |
| inv | ErpInvStockMove | confirm |

## E1.1 进入门消费指引

1. **优先级 P0**：28 项 bypassed（approve/reverseApprove 模式覆盖缺口）——调查根因 + 按域分批闭环（admin 正向 + role-restricted 负向双侧验证）。
2. **优先级 P1**：28 项 arg-mismatch——E1.1 修正探针 arg 名后重新归类，denied 闭环 / bypassed 并入 P0 批次。
3. **已闭环锚点**：5 项 denied——E1.1 复用 `expectActionDenied({ errorCode: NO_PERMISSION })` 断言，不再重复验证。
4. **数据层（data-auth）**：本清单仅 action-auth 层；data-auth（E2.1）的影响面独立承担，不在本清单。

## 测试基建 finding（P2.4 期间发现，已修）

- **`GraphQLClient` auth 传输 gap**：`page.request.post('/graphql')` 不携带 flux 前端的 token（前端存 sessionStorage + fetch 拦截器注入 header，`page.request` 绕过拦截器，且 `__Host-nop-token` cookie 因 `__Host-` 前缀 Secure 约束不被 `page.request` 发送）。action-auth OFF 时无影响（server 不校验）；action-auth ON 时所有 `page.request` GraphQL 调用抵达时 `roles=[]` → 全 `no-permission`。**修正**：`GraphQLClient.post` 读 `__Host-nop-token` cookie + 显式注入 `Authorization: Bearer <token>` header（server `/graphql` 接受 Authorization header 或 cookie——P2.4 实测两者均接受）。
- **`GraphQLClient.callMutation` 不返回 `json` envelope**：business-actions/_helper `callMutation` 声明返回 `{data, errors, json}` 但 GraphQLClient.callMutation 只返 `{data, errors}` → `json` undefined → `expectActionDenied` errorCode 断言失效（P2.3 demo 仅用 token 断言未触发）。**修正**：GraphQLClient.callMutation/callQuery 补返 `json` envelope。
