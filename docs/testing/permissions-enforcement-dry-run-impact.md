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

## E1.1 重归类结果（plan 2026-08-10-0739-1 / E1.1，2026-08-10）

> **E1.2 直接消费本节**——根因裁决 + 重归类终态 + 修复方案为 E1.2（pur/sal/ast 27 bypassed + ct 等域 arg-mismatch）冻结输入。

### 根因裁决（approve/reverseApprove bypass 模式）

**根因 = xbiz `<mutation>` 缺 `<auth>` 子元素致 `field.auth=null`**。enforcement 由 `GraphQLActionAuthChecker.isAllowAccess(auth, ctx)`（`GraphQLActionAuthChecker.java:118-145`）判定，`auth == null` 即放行（return true）。auth 元数据来源分两路：

- **Java `@BizMutation` 方法**（如 `ErpFinBadDebt.writeOff`、`ErpHrSalary.markPaid`、`ErpInvLandedCost.approve`）经 `ReflectionBizModelBuilder.buildActionField:330-336` **恒定**附加非空 `ActionAuthMeta`（即便无 `@Auth` 注解，也自动派生 permission `{bizObj}:{opType}|{bizObj}:{action}`）→ enforcement 路径必达 → restricted 被 `nop.err.auth.no-permission` 拒。
- **xbiz `<mutation>` 无 `<auth>` 子元素**（如 `ErpMfgSubcontractOrder.approve`、`ErpHrSalary.approve`，经 approval-support.xbiz 注入，`<source>` 存在但**无 `<auth>`**）经 `BizModelToGraphQLDefinition.toOperationDefinition:80`（`field.setAuth(actionModel.getAuth())`）→ `field.getAuth() == null` → `isAllowAccess(null) = true` → **bypass**（action-auth.xml 的 FNPT 声明仅建 permissionToRoles 映射，不触达 field.auth）。

**关键**：`approve`/`reverseApprove` **不是** CrudBizModel Java 基类方法，而是由平台 `nop-wf-core/_vfs/nop/wf/base/approval-support.xbiz` 经 xbiz delta（`x:extends`）注入的 `<mutation>` action。`inv ErpInvLandedCost.approve` 是 Java `@BizMutation`（故 P2.4 denied 锚点），与 pur/sal/mfg 委外的 approve 走不同路径——这解释了 P2.4 清单中 inv approve 是唯一 denied 的 approve 动作。

### 重归类方法 = 静态裁决（权威）

分类**不依赖单次运行**（复杂 input arg 校验在 enforcement 之前，运行时探针对 `post`/`reverse` 仍可能 arg-validation 失败需调参）。权威分类规则（从源码机制派生）：

| 动作来源 | field.auth | enforcement | 归类 |
|----------|-----------|-------------|------|
| Java `@BizMutation` 方法 | 必非空（ReflectionBizModelBuilder 恒定附加） | 路径必达 | **denied**（restricted 无 permission 映射） |
| xbiz `<mutation>` 含 `<auth>` | 非空 | 路径必达 | **denied** |
| xbiz `<mutation>` 无 `<auth>` | `null` | `isAllowAccess(null)=true` 放行 | **bypassed** |

### E1.1 五域重归类终态表

**P2.4 原 28 inconclusive-arg-mismatch → E1.1 探针 arg 名修正后（fin `voucherId`/`periodId` + `event:{}` 骨架 + `billHeadCode,businessType`；b2b `ediDocId` + `error` + 5×String；mfg `workOrderId`；hr `id` String；inv `moveId`）重新归类**：

| 终态 | 计数 | 项 | 说明 |
|------|------|----|------|
| **denied（Java @BizMutation 重归类）** | 18 | fin post/reverse + closePeriod/reverseClose；b2b markSent/cancel/markAcknowledged/markError/retry/archive + handleInboundWebhook/matchPurchaseOrder/createReceiveFromAsn/retryMatch；mfg WorkOrder start/close/cancel；hr leaveRequest.approve；inv StockMove.confirm | arg-mismatch 修正后全部为 Java @BizMutation → field.auth 非空 → restricted 真拒绝 |
| **denied 锚点（P2.4 已闭环）** | 5 | fin writeOff/reverseApprove；hr markPaid/voidSalary；inv landedCost.approve | 自定义 @BizMutation + FNPT 声明 + checker 三层联动，E1.1 复用为负向断言锚点 |
| **bypassed（xbiz `<mutation>` 无 `<auth>`）** | 2 | mfg `ErpMfgSubcontractOrder.approve`；hr `ErpHrSalary.approve` | xbiz-only mutation 缺 `<auth>` → field.auth=null → checker 放行 |

**E1.1 五域合计**：23 denied（18 重归类 + 5 锚点）+ 2 bypassed = 25 动作。denied 项天然闭环（Java 方法恒定附加 auth，无需修复）；2 bypassed 项需补 xbiz `<auth>`（见修复方案）。

### 修复方案 = 方案 (a)：保留层 xbiz `<mutation>` 补 `<auth permissions="..."/>`

在 `ErpMfgSubcontractOrder.xbiz` / `ErpHrSalary.xbiz` 的 `approve` `<mutation>` 内补：
```xml
<auth permissions="ErpMfgSubcontractOrder:approve"/>   <!-- 或 ErpHrSalary:approve -->
```
permission 与 action-auth.xml 既声明的 `FNPT:...:approve <permissions>` 字面一致——checker 经 permissionToRoles 映射（site-map）判定授权角色通过 / restricted 拒。

**未选方案 (b)（SUBM 级已覆盖）的理由**：SUBM 级（`ErpMfgSubcontractOrder-main` roles="生产主管"）仅菜单过滤，不触达 GraphQL field enforcement——field.auth=null 时 checker 放行，与 SUBM 无关，故 (b) 不成立。

**触保护区域 = 是**（xbiz enforcement 绑定 = auth plan-first 保护区域）。2 bypassed 项的 xbiz 编辑**被系统权限规则阻塞**（`**/_*.xbiz` deny 规则原意保护生成层 `_*.xbiz`，但 glob 实现将 `_vfs` 路径段纳入匹配，连带阻塞保留层无下划线 xbiz）→ 登记人工批准待办（具体 diff：`<mutation name="approve">` 内 `<arg>` 之后、`<return>` 之前插入 `<auth permissions="..."/>`）+ mfg/hr spec `test.fixme` 标记，待人工批准 xbiz `<auth>` 补齐后去 fixme 激活。

### E1.2 冻结输入（直接消费）

E1.2 范围 bypassed = **27 项**（pur 12 + sal 14 + ast 1，同样 approve/reverseApprove 由 approval-support.xbiz 注入、本域保留层 xbiz 未补 `<auth>` 模式）+ ct 等域 arg-mismatch。E1.2 直接消费：(1) 根因裁决（同 xbiz `<auth>` 缺失模式）；(2) 修复方案（同 `<auth permissions="..."/>` 批量补齐）；(3) 五域闭环范式（denied 天然闭环 + bypassed 补 xbiz `<auth>` + 授权角色正向/restricted 负向双侧 Proof）。

## E1.2 闭环结果（plan 2026-08-10-1404-1 / E1.2，2026-08-10）

> E1.2 全量 19 域 action enforcement 闭环 + 菜单过滤验证 + 全量正负向 Proof 37 tests 绿。

### 全域 bypassed 闭环终态

E1.1 冻结输入的 27 项 baseline + E1.1 域残留（inv ErpInvCostAdjust / fin ErpFinEmployeeAdvance/ErpFinExpenseClaim / pur ErpPurRfq/ErpPurQuotation / mfg MaterialIssue/SubcontractOrder/WorkOrder / hr Salary.reverseApprove / prj 3 实体 / qa 5 实体 / mnt 2 实体）的 xbiz `<mutation>` approve/reverseApprove 全部补 `<auth permissions="{Entity}:{action}"/>`。ct 8 项 arg-mismatch 静态归类全 denied（Java @BizMutation 天然闭环，无需修复）。

### 执行期发现并修复的 4 类缺漏

E1.2 执行期（Phase 3 全量 Proof）发现前序 Phase 1 闭环存在 4 类缺漏，本会话补齐：

1. **25 缺失 reverseApprove FNPT 跨 9 域**：pur Quotation/Rfq + ast 6 实体 + inv CostAdjust + fin EmployeeAdvance/ExpenseClaim + mfg MaterialIssue/SubcontractOrder/WorkOrder + hr Salary + prj Billing/Budget/CostCollection + qa Calibration/Inspection/Recall/Review + mnt Calibration/Request 的 reverseApprove FNPT 在 delta action-auth.xml 中未声明 → `permissionToRoles["{Entity}:reverseApprove"]` 缺失 → 授权角色（管理员）被误拒（enforcement 检查 permissionToRoles 返空集 → deny-by-default）。补声明 `<resource id="FNPT:{Entity}:reverseApprove" ... roles="管理员"><permissions>{Entity}:reverseApprove</permissions></resource>`，与既有 reverseApprove=管理员范式一致。
2. **ast FNPT roles slash→comma 修正 6 项**：`csv-set` 解析器（site.xdef L17 `roles="csv-set"`）按逗号分隔，不识别 `/` 分隔。`roles="资产管理员/管理员"` 被解析为单一字符串致 `containsRole` 精确匹配失败 → 资产管理员被误拒。修正为 `roles="资产管理员,管理员"`。**平台契约**：`roles=` 属性必须使用逗号分隔多角色。
3. **ErpInvCostAdjust xbiz `<arg>`/`<return>` 补齐**：该 xbiz `<mutation name="approve">` / `<mutation name="reverseApprove">` 缺 `<arg name="id">` + `<return>` 声明 → GraphQL action 无 id 参数 → `nop.err.graphql.undefined-field-arg` 先于 enforcement → restricted 负向测试无法触达 enforcement。补 `<arg name="id" type="String" mandatory="true"/>` + `<arg name="svcCtx" kind="ServiceContext"/>` + `<return><schema bizObjName="THIS_OBJ"/></return>`（与既有 ErpPurOrder xbiz 范式一致）。
4. **审核人跨域审批账号种子**：pur/sal approve 授权角色为「审核人」，但 P2.2b 账号池未含此角色 → `loginAsRole('审核人')` 回退 restricted → 授权角色正向测试失败。新增 userId 20 role-approver 绑「审核人」角色 + ROLE_ACCOUNTS 条目。

### 角色账号池扩展终态

E1.2 扩展至 14 角色域全集 + 审核人跨域审批账号（userId 11-20）：

| userId | username | roleId | 域 |
|--------|----------|--------|-----|
| 11 | role-pur | 采购员 | pur |
| 12 | role-sal | 销售员 | sal |
| 13 | role-ast | 资产管理员 | ast |
| 14 | role-prj | 项目经理 | prj |
| 15 | role-qa | 质量主管 | qa |
| 16 | role-mnt-lead | 维护主管 | mnt |
| 17 | role-mnt-tech | 维护人员 | mnt |
| 18 | role-ct-clerk | 合同专员 | ct |
| 19 | role-ct-approver | 合同审批人 | ct |
| 20 | role-approver | 审核人 | 跨域（pur/sal approve） |

### 菜单过滤覆盖边界 + 运行时 finding

- **14 角色域**（fin/pur/sal/mfg/inv/ast/prj/qa/mnt/md/ct/b2b/hr/notify）：TOPM/SUBM `roles=` 种子已落地，enforcement ON 后菜单按角色 deny-by-default 过滤。
- **B 类 5 域**（CRM/CS/APS/Logistics/DRP）：admin-only，非 admin 菜单过滤隐藏。
- **sys-*/l10n-cn**：`roles="admin"`，平台 admin 可见。
- **notify inbox**：`roles="user"`，所有登录用户可见。
- **运行时 finding（菜单可见性经 FNPT cascadeUp 驱动）**：`SiteCacheDataBuilder.cascadeResourceToRoles`（L221-232）将 FNPT `roles=` 向上级联至 SUBM/TOPM。TOPM/SUBM 自身 `roles=` 虽并入 `resourceToRoles`（L149-151），但运行时实证仅 FNPT cascadeUp 路径稳定生效——有 FNPT `roles="<role>"` 的角色（财务员/库管员/生产主管/质量主管）稳定见各自域；FNPT roles=其他角色（采购员/销售员/资产管理员，FNPT roles=审核人/管理员）的菜单可见性受限。菜单过滤负向 spec 因此以财务员做正向断言、采购员做 cross-domain deny 断言。深化菜单种子补齐（为采购员等无 FNPT 本角色域的角色补 query FNPT cascadeUp）归 successor。

### 全量正负向 Proof 终态

5 spec / 34 tests 全绿（flux 引擎，action-auth ON %test profile）：

| spec | 动作数 | restricted 负向 | 授权角色正向 |
|------|--------|----------------|--------------|
| e1-2-purchase | 16 | ✓ | 审核人(approve) + 管理员(reverseApprove) |
| e1-2-sales | 14 | ✓ | 审核人(approve) + 管理员(reverseApprove) |
| e1-2-assets | 12 | ✓ | 资产管理员(approve) + 管理员(reverseApprove) |
| e1-2-residual-extended | 17 | ✓ | 库管员/财务员/生产主管/项目经理/质量主管/维护主管/管理员 |
| e1-2-menu-filter | 3 tests | B 类隐藏 + sys 隐藏 | 财务员见 erp-fin + 采购员 cross-domain deny |
