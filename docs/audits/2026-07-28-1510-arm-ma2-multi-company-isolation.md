# MA2 多账套/多公司隔离审计报告（A2.18）

> Audit Status: closed
> 里程碑：MA2 | 维度：多账套/多公司隔离（A2.18 收口裁决——MA2 最后一项，done 后 MA2 全完成）
> 域/功能模块：全 19 域（orgId 查询/写入隔离 + acctSchemaId 账套隔离 + 法人根解析 + 自然键 orgId 维度 + 跨公司配对/抵消作用域 + 配置继承 cache key + 数据权限运行时 + 与设计文档一致性）
> 报告日期：2026-07-28
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`（多维隔离正确性审查）
> 来源 plan：`docs/plans/2026-07-28-1510-1-audit-remediation-ma2-multi-company-isolation.md`
> 交接范围：A2.17 closure Follow-up P0-MA2-018/020 UK orgId 维度复核（A2.17 Deferred line 171 + Follow-up line 210）

## 0. 执行摘要

本审计是 MA2 多公司/多账套隔离维度的**收口裁决**，也是 MA2 里程碑的最后一项（done 后 MA2 全部 18 个工作项完成）。结论：

- **零 P0**（4 个候选 P0 经证据**证伪或降级**——见 §11；最严重的「orgId 隔离未落地」经评估为「能力缺失/文档-实现差距」而非「活跃数据腐败」，降级 P1-MA2-093）
- **7 项新 P1**（登记 arm-index §P1 汇总，目标 MR1）：
  - `P1-MA2-093` orgId 查询隔离**全仓未落地**（平台仅支持 tenant 自动过滤，本项目 0 个实体启用 `useTenant`；19 个模块 `data-auth.xml` 全部为 `<objs/>` 空规则；11 个 dashboard BizModel 经 `IDaoProvider` 直访绕过仅有的（空）认证管道）→ **owner doc `multi-company.md:29`「所有业务单据按 orgId 隔离查询」声明完全未实现**
  - `P1-MA2-094` orgId 写入**客户端可任意指定**（无 `IUserContext.getOrgId()` + 无 auto-stamp 拦截器 + 无 `@BizEventListener`；AMIS 表单 `orgId` 是可编辑列；测试代码自由 `setOrgId(1101L/1201L/...)` 证明无规范化）
  - `P1-MA2-095` acctSchemaId **写路径彻底，读路径泄漏**（过账/多账套传播/红冲在写时正确 stamp `acctSchemaId`；但 `ErpFinReportBizModel`/`ErpFinDashboardBizModel`/期间前置检查 12 处查询仅按 `periodId` 过滤，省略 `acctSchemaId`+`orgId` → 多账套部署 `multi-schema-enabled=true` 时报表/看板双计）
  - `P1-MA2-096` `ErpFinGlBalance` **无 DB 强制自然键**（与 P0-MA2-020 inventory 余额同型结构缺口——仅非唯一索引，无 `(orgId, acctSchemaId, periodId, subjectId, ...)` UK，正确性完全依赖应用层 upsert）
  - `P1-MA2-097` 跨公司配对 **owner doc 算法漂移 + ErpFinIntercompanyMatch 审计列全空**（owner doc `multi-company.md:197` 声明 `pairKey=min(fromOrgId,toOrgId)+":"+max+":"+materialId`，**实测代码 `pairKey=billCode`**；`arOrgId`/`apOrgId`/`arSideVoucherId`/`apSideVoucherId`/`materialId` 五列**从不被 `runMatching` 填充** → 配对审计/抵消追溯断链）
  - `P1-MA2-098` `runMatching` **非幂等**（`ErpFinIntercompanyMatch` 无 `(pairKey, periodId)` UK → 同期重复调用产生重复 Match 行）
  - `P1-MA2-099` GL 映射 cache **默认配置下跨组织泄漏**（`erp-fin.gl-mapping.org-dimension-enabled` 默认 `false` 时所有规则塌缩到 `"_"` cache bucket + matcher 跳过 orgId 校验 → orgA 规则可匹配 orgB 过账请求；owner doc `multi-company.md:244-249` 已标注「dormant」，本审计正式登记为多公司部署的活跃泄漏面）
- **A2.17 交接点复核结论**（P0-MA2-018/020 UK orgId 维度）：
  - **P0-MA2-018（`erp_fin_voucher_bill_r` UK）**：**结论——单纯加 orgId 列不足修复**。billR **无 `acctSchemaId` 列**（`orm.xml:626-639` 实测确认），区分「并发重复正向 INSERT（应阻止）」与「合法多行：红冲 REVERSAL / 多账套 / 冲销后重过账（应允许）」所需的判别列（`postingType`/`isReversed`/`acctSchemaId`）**全部位于 `ErpFinVoucher` 而非 billR**。即使加 orgId，红冲「同键 2 行」（`TestErpFinPostingService:225`）+ 多账套「同键 N 行」（`TestErpFinMultiSchemaPosting:89`）+ 软删除重插生命周期三重冲突依然存在。**多公司维度复核结论：orgId 单独无法修复 P0-MA2-018**，deferred plan 的方向 A/B/C/D 裁决**维持不变**，本审计不重新打开。
  - **P0-MA2-020（`erp_inv_stock_balance` UK）**：**结论——方案 A 已正确含 orgId，对多公司隔离正确**。fix plan 方案 A 的 UK `(orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` **已显式含 orgId**——不同组织同物料/仓库不碰撞（合法重号），同组织并发首次移动单获 DB 保护。**多公司维度复核结论：P0-MA2-020 已 done（plan `2026-07-28-1249-arm-fix-p0-ma2-020` Status: completed），UK 设计对多公司隔离正确，无须变更**。
- **维度通过/失败裁决**：
  - ✅ **PASS**：法人根解析环形引用正确性（`resolveLegalEntityRoot:208` visited set + 迭代 + 空守卫）/ 单据编号唯一约束 orgId 维度（全域 ~70 事务单据 UK_*_CODE_ORG 正确）/ 库存按仓库（仓库归属组织）隔离 / 配置继承（CoA/CostMethod/折旧/转移定价 cache 正确，仅 GL mapping 默认配置泄漏见 P1-MA2-099）
  - ⚠️ **FAIL→P1**：orgId 查询隔离彻底性 / orgId 写入正确性 / acctSchemaId 读路径隔离 / ErpFinGlBalance 自然键 / 跨公司配对作用域（owner doc 漂移 + 审计列空）/ runMatching 幂等性
  - ❌ 无 P0

多公司/多账套维度终态：**全域 ⚠️(P1)**——核心隔离不变量在**写路径 + 自然键层基本成立**（事务单据 UK 含 orgId / 多账套传播 stamp acctSchemaId / 法人根解析环形守卫），但**读路径隔离机制全仓未落地**（无 `IUserContext.getOrgId()` + 空 data-auth + 报表/看板聚合无作用域）。这是「文档声明的多公司能力」与「实际落地的单组织骨架」之间的系统性差距，归 MR1 通过「补全局 `IQueryTransformer` 或填充 `data-auth.xml` + 报表/看板作用域 + `IUserContext.getOrgId()`」收口。**单组织种子（176 行全部 orgId=2 ERP-CO）掩盖了所有跨组织泄漏——单组织部署下无实际数据腐败**。

## 1. Current Baseline 复核

实时仓库已落地的隔离机制（经实仓证据复核）：

- **orgId 列存在性**：业务单据头实体普遍声明 `orgId`（如 `ErpPurOrder.orgId` BIGINT 持久化列 `app-erp-purchase.orm.xml:540` propId=3 表达「业务组织」；`ErpFinVoucher.orgId` mandatory `app-erp-finance.orm.xml:421`）
- **法人根解析**：`resolveLegalEntityRoot(orgId)` walk-up helper 存在于 `ErpFinIntercompanyTransferBizModel.java:207-226`（intercompany 路径已用），含环形检测 + 空守卫
- **凭证账套隔离**：`ErpFinVoucher.acctSchemaId` mandatory（`app-erp-finance.orm.xml:422`）；多账套并行经 `SchemaPropagator.resolveTargetSchemas(orgId, primarySchemaId)`（`SchemaPropagator.java:44-81`）+ `ErpFinPostingProcessor.process:131,161-188` 循环按账套生成独立凭证
- **种子现实**：种子仅 1 个 COMPANY 法人根（`erp_md_organization.csv` org=2 ERP-CO；org=1 GROUP-HQ 被 0 个业务单据引用）；176 行业务单据种子**全部 orgId=2**——单组织种子掩盖了所有跨组织泄漏回归

**审计方法论**：多维审查（`multi-dimensional-audit-prompt.md`）——跨多个隔离面同时挑战工作，而非单维深挖。审查维度：(1) orgId 查询隔离彻底性；(2) orgId 写入正确性；(3) acctSchemaId 隔离；(4) 法人根解析正确性与性能；(5) 自然键唯一性含 orgId（含 P0-MA2-018/020 复核）；(6) 跨公司配对/抵消作用域；(7) 配置继承与 cache key；(8) 数据权限多公司角度；(9) 与设计文档一致性。

**证据采集**：3 个并行独立 explore 子代理（fresh-context）跨维度采集——(a) orgId 查询/写入隔离专项（平台机制 + 项目侧 data-auth + 5 域 @BizQuery 抽样 + 11 dashboard KPI + 种子统计）/ (b) 法人根解析 + intercompany 配对/抵消作用域专项（cycle 检测 + pairKey + elimination scope + 实体 UK）/ (c) acctSchemaId 隔离 + GL 映射/成本/折旧 cache key 专项（多账套传播 + 报表/看板泄漏面 + cache 分区）。主代理交叉裁决，无自我审计。

## 2. 维度一：orgId 查询隔离彻底性（核心）

> owner doc `multi-company.md:29` 声明：「**所有业务单据按 orgId 隔离查询**」

### 2.1 平台机制核实

**Nop 平台 `CrudBizModel.prepareFindPageQuery` 是唯一自动注入点**（`nop-entropy/nop-service-framework/nop-biz/.../CrudBizModel.java:381`）：

```java
query = AuthHelper.appendFilter(context.getDataAuthChecker(), query, authObjName, action, context);
```

**但 `AuthHelper.appendFilter` 是纯委托**（`nop-entropy/nop-service-framework/nop-biz-auth-api/.../AuthHelper.java:27-41`）：`checker == null` 或 `context.getUserContext() == null` → 返回 query 不变；否则委托给 `checker.getFilter(bizObjName, action, context)`。

**`DefaultDataAuthChecker.getFilter` 在无规则配置时返回 null**（`nop-entropy/nop-auth/nop-auth-service/.../DefaultDataAuthChecker.java:195-213`）：`ObjDataAuthModel objAuth = authModel.getObj(bizObj);` **if `objAuth == null` return null**。

**平台另有 `useTenant` + `nopTenant` 列机制**（`nop-entropy/nop-persistence/nop-orm/.../EntityPersisterImpl.java:426-452` auto-stamp + `GenSqlHelper.java:92-95,624-648` SQL 自动追加 tenant 过滤），**但仅 tenant 列，仅当 `useTenant="true"` 时生效**。

### 2.2 项目侧 data-auth 配置

**全域 grep**：`useTenant` 在所有 19 个 `*.orm.xml` 中 = **0 命中**（项目实体不启用平台 tenant 机制）。

**19 个模块级 `erp-{module}.data-auth.xml` 全部仅含 `<objs/>`**（空规则）：

```
module-finance/erp-fin-service/.../auth/erp-fin.data-auth.xml — 4 行，<objs/>
module-purchase/erp-pur-service/.../auth/erp-pur.data-auth.xml — 4 行，<objs/>
...（17 个模块同型）
```

19 个 app 级 `app.data-auth.xml` 仅 `<auth-gen:GenFromModules>` 聚合——合并 19 个空 `<objs/>` 产出空聚合。**因此 `DefaultDataAuthChecker.getFilter()` 对本项目所有 bizObj 永远返回 null → `AuthHelper.appendFilter` 在每条查询上 no-op**。

**项目侧 `IDataAuthChecker`/`IQueryTransformer`/`ISiteDataProvider` 注册**：全域 grep = **0 命中**（项目不注册任何自定义 data-auth 检查器或全局查询转换器）。**`IServiceContext`/`IContext` 接口均无 `getOrgId()` 方法**（`nop-entropy/nop-kernel/nop-core/.../IServiceContext.java:105-124` 仅有 `getUserId()`；`IContext.java:31-37` 仅有 `getTenantId()`）。

**结论**：`CrudBizModel.findPage` → `prepareFindPageQuery:381` 这条唯一自动注入路径，在本项目是**永久 no-op**。

### 2.3 五域 @BizQuery 抽样

| 域 | @BizQuery 文件:行 | orgId 过滤? | 调用路径 | 实体含 orgId 列? |
|---|---|---|---|---|
| finance | `ErpFinVoucherBizModel.java:121` `previewReverseVoucher` | NO | `requireEntity`（CrudBizModel）+ helper 内 `daoProvider().daoFor()` 直访 | YES |
| purchase | （无自定义 @BizQuery——用默认 `findPage`） | NO | CrudBizModel 默认 | YES（`app-erp-purchase.orm.xml:540`） |
| sales | （无自定义 @BizQuery） | NO | CrudBizModel 默认 + helper 内 `daoFor(ErpSalPricingRule).findAllByQuery` 直访 | YES |
| inventory | `ErpInvStockBalanceBizModel.java`（15 行空类） | NO | CrudBizModel 默认 | YES |
| manufacturing | `ErpMfgWorkOrderBizModel.java:94` `findWorkOrdersPendingJobCards` | NO | 处理器内 `dao.findAllByQuery` 直访绕过 CrudBizModel | YES |

**全域 grep `addFilter(eq("orgId"` ≈30 命中，全部落入两类**非隔离**桶**：
1. **功能作用域**（在已知单据的组织内避免捡到其他组织成本层）：`FifoCostingStrategy.java:184` / `LifoCostingStrategy.java:170` / `BatchCostingStrategy.java:177` / `SpecificCostingStrategy.java:173`
2. **辅助方法接收 orgId 作 Java 参数**：`ErpCrmLeadBizModel.java:217 loadActiveRules(Long orgId)` / `ErpMdAcctSchemaBizModel.java:28 findFirstByOrg(@Name("orgId") Long orgId)`

**无一处 orgId 取自调用用户的会话/上下文**——因为该 API 不存在。

### 2.4 看板聚合查询

11 个 dashboard BizModel（`ErpFinDashboardBizModel`/`ErpInvDashboardBizModel`/`ErpPurDashboardBizModel`/`ErpSalDashboardBizModel`/`ErpMfgDashboardBizModel`/`ErpAstDashboardBizModel`/`ErpQaDashboardBizModel`/`ErpMdDashboardBizModel`/`ErpCsQualityDashboardBizModel`/`ErpPrjDashboardBizModel`/`ErpMntDashboardBizModel`）**全部 `@Inject IDaoProvider` + 直访 `daoProvider.daoFor(...).findAllByQuery(q)`**——**绕过 CrudBizModel 的（空）认证管道**。

代表性证据（`ErpFinDashboardBizModel.java`）：
- `loadGlBalances:160-176` 仅 `eq("periodId", periodId)`
- `sumBankBalance:202-212` 仅 `eq("accountType", "BANK")`——**汇总所有组织的银行账户**
- `sumArApOpen:214-227` 仅 `direction`+`status`

`ErpInvDashboardBizModel.java` grep `orgId` = **0 命中**；`findWarehouseDistribution:132-145` SQL `GROUP BY warehouseId` 无 orgId → **返回跨组织仓库总额**。

### 2.5 种子现实

`erp_md_organization.csv`（master 组织种子）：

```
ID,CODE,NAME,ORG_TYPE,STATUS,FUNCTIONAL_CURRENCY_ID
1,GROUP-HQ,演示集团总部,GROUP,ACTIVE,1
2,ERP-CO,演示 ERP 有限公司,COMPANY,ACTIVE,1
```

**全域业务单据种子（49 表 / 176 行）的 `ORG_ID` 列 distinct 值统计：仅 `[2]`**——每个业务单据表种子**独占 orgId=2**，0 行引用 orgId=1（GROUP-HQ）。

**测试代码无跨组织隔离断言**：grep 全 `src/test` 无「在 orgId=1 建数据，以 orgId=2 用户查询，断言空结果」形态的负向测试。intercompany 测试（`TestErpFinIntercompanyMatchingAndElimination.java:56-60`）故意种跨组织数据（orgId=1 SALE + orgId=2 PURCHASE）正是因为平台不自动分隔——配对算法需要双方在一个查询内可见。

### 2.6 裁决

**VERDICT: 维度 FAIL → P1-MA2-093**

owner doc `multi-company.md:29`「所有业务单据按 orgId 隔离查询」**完全未实现**。这是「文档声明的多公司能力」与「实际落地的单组织骨架」之间的系统性差距。

**为何降级 P1 而非 P0**：
- 单组织种子（176 行全部 orgId=2）下**无实际跨组织数据腐败发生**（不存在 orgId=1 的业务数据可泄漏给 orgId=2 用户）
- 这是**能力缺失/未实现**（missing enforcement layer），而非**活跃缺陷**（active corruption）
- 修复是**架构级补能力**（`IUserContext.getOrgId()` + 全局 `IQueryTransformer` 或填充 `data-auth.xml`），与本 plan Non-Goal「不实现缺失的多公司特性」冲突
- 不破坏当前单组织部署的数据正确性

但**风险评级为 P1 最高优先级**——多组织部署（owner doc `project-vision.md` 定位的「产品化通用 ERP」核心能力）一旦开启即触发跨组织泄漏。归 MR1 收口。

## 3. 维度二：orgId 写入正确性

> 控制点：「单据创建时 `orgId` 是否从上下文/当前组织正确写入，还是可被客户端任意指定（跨法人越权写候选）」

### 3.1 平台默认 save 路径

`CrudBizModel.doSaveEntity`（`nop-entropy/nop-service-framework/nop-biz/.../CrudBizModel.java:788-795`）仅 `dao().saveEntity(entityData.getEntity())` + `afterEntityChange(...)`，**无 orgId 引用**。全域 grep nop-entropy `setOrgId|NOP_ORG` = **0 命中**（仅 `setTenantId` 平台 stamp）。

### 3.2 三域 @BizMutation 抽样

`ErpPurOrderBizModel`/`ErpSalOrderBizModel`/`ErpInvStockMoveBizModel` **均 `extends CrudBizModel<T>` 不重写 `doSave`/`doSaveEntity`/`save`**，使用平台默认 input bean → entity 字段拷贝。**全文件 grep `setOrgId` = 0 命中**。

Input bean 把 `_orgId` 声明为普通 Long 字段——AMIS 表单提交该字段：
- `ErpPurOrderInputBean.java:44-54`、`ErpPurInvoiceInputBean.java:44-54`、`ErpPurReceiveInputBean.java:44-54`、`ErpPurReturnInputBean.java:44-54`、`ErpPurPaymentInputBean.java:44-54`
- `ErpSalOrderInputBean.java:53`、`ErpSalDeliveryInputBean.java:53`、`ErpSalInvoiceInputBean.java:53`、`ErpSalReceiptInputBean.java:53`
- `ErpInvStockMoveInputBean.java:67`

**AMIS 前端把 `orgId` 渲染为可编辑列**：`ErpPurOrder.view.xml:10,88,121` `<col id="orgId" label="业务组织"/>`；同型在 `ErpPurRequisition.view.xml:10,56,71` / `ErpPurReturn.view.xml:10,57,86` / `ErpPurInvoice.view.xml:65,115,145`。

**唯一 orgId null 校验是 silent-skip**：`ErpPurOrderProcessor.java:268` `if (order.getOrgId() == null || order.getBusinessDate() == null) { return; }`（无 enforce-from-context），`ErpSalOrderProcessor.java:305` 同型。

### 3.3 测试代码 orgId 设置证明客户端语义

测试构造实体直接 `setOrgId(<常量>)`，取值广泛（证明应用接受任何 orgId）：
- `TestErpPurOrderApproval.java:197` `order.setOrgId(ORG_ID)` 其中 `ORG_ID = 1101L`（L37）
- `TestErpSalOrderApproval.java:389` 其中 `ORG_ID = 1301L`（L48）
- `TestErpPurReceiveStockMove.java:297` 其中 `ORG_ID = 1201L`（L55）
- `TestErpPurInvoicePosting.java:235` 其中 `ORG_ID = 1003L`（L52）
- 全域其他测试中见到：9101L、1401L、1501L、1601L、1701L、1751L、11001L、2351L、2251L、1251L、1351L、3905L、3501L、3801L、3701L、3602L、3804L、5401L、6401L、7401L、8401L、9401L、7301L……

### 3.4 auto-stamp 拦截器/事件监听器搜索

全域 grep `@BizEventListener|IPrepareSaveHook|@GlobalEventListener|OrmEntityListener|IOrmInterceptor|@InsertInterceptor|implements IOrmSessionHook|IEntityPersistHook|onPreInsert|@PrepareSave` = **0 命中**（main 代码）。grep `ContextProvider.*orgId|currentContext.*orgId|getUserContext.*getOrgId` = **0 命中**。

### 3.5 裁决

**VERDICT: 维度 FAIL → P1-MA2-094**

**orgId 完全客户端可指定**——平台零强制（不同于 `tenantId`），项目侧零 `@BizEventListener`/零拦截器。**有任意业务单据 `__save` 权限的用户可向任何 orgId 写入**。与维度一组合，形成跨组织写 + 跨组织读双重越权面。同 P1-MA2-093 降级理由：单组织种子下无实际腐败，归 MR1 收口。

## 4. 维度三：账套（acctSchemaId）隔离

> owner doc `multi-company.md:32` 声明：「凭证按 `acctSchemaId`（账套）隔离」

### 4.1 写路径——彻底

- `ErpFinVoucher.acctSchemaId` mandatory（`app-erp-finance.orm.xml:422`）+ 索引 `IDX_FIN_VOUCHER_ACCT_SCHEMA_ID:459-461`
- `ErpFinVoucherLine.acctSchemaId` mandatory（`orm.xml:489`）+ 索引 `:530-532`
- `ErpFinPostingProcessor.persistVoucher:785` `voucher.setAcctSchemaId(acctSchemaId)` + `:820` `line.setAcctSchemaId(acctSchemaId)`
- 多账套传播 `SchemaPropagator.resolveTargetSchemas(orgId, primarySchemaId)`（`SchemaPropagator.java:44-81`）+ `ErpFinPostingProcessor.process:131,161-188` 按账套循环每账套一张凭证，每张凭证 + 分录行正确 stamp `acctSchemaId`
- 红冲镜像：`findAllPostedVouchers:866-882` 跨账套返回所有已过账凭证并逐张红冲
- 正面对照：`ErpFinAccountingPeriodProcessor.populateTrialBalanceForAllSchemas:464-516` 显式按 `acctSchemaId` 分组（`:481-492`）+ `tb.setAcctSchemaId(acctSchemaId)`（`:500`）——正确范式
- `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)`（`AcctSchemaResolver.java:28-40`）按 `eq("orgId", orgId)` 查询 → **schema 解析依赖 orgId 正确**

### 4.2 读路径——泄漏

12 处查询 `ErpFinVoucher`/`ErpFinVoucherLine`/`ErpFinGlBalance`/`ErpFinArApItem`/`ErpFinAccountingPeriod`/`ErpFinAccountingPeriodStatus` **省略 `acctSchemaId`（多数也省略 `orgId`）**，按 `periodId` 或更弱条件聚合：

| 文件:行 | 方法 | 查询实体 | 过滤条件 | 缺失 |
|---|---|---|---|---|
| `ErpFinReportBizModel.java:380-405` | `loadGlBalances` | `ErpFinGlBalance` | `periodId` | acctSchemaId, orgId |
| `ErpFinReportBizModel.java:416-428` | `loadPostedVoucherLines` | `ErpFinVoucherLine` | docStatus+periodId | acctSchemaId, orgId |
| `ErpFinReportBizModel.java:438-444` | `loadPeriodStatus` | `ErpFinAccountingPeriodStatus` | periodId → `list.get(0)` | acctSchemaId |
| `ErpFinReportBizModel.java:407-414` | `findLatestPeriodId` | `ErpFinAccountingPeriod` | order by startDate, limit 1 | orgId |
| `ErpFinReportBizModel.java:446-457` | `countBillR` | `ErpFinVoucherBillR` | periodId | acctSchemaId, orgId |
| `ErpFinDashboardBizModel.java:160-176` | `loadGlBalances` | `ErpFinGlBalance` | periodId | acctSchemaId, orgId |
| `ErpFinDashboardBizModel.java:187-200` | `loadGlBalancesInRange` | `ErpFinGlBalance` | periodId range | acctSchemaId, orgId |
| `ErpFinDashboardBizModel.java:202-212` | `sumBankBalance` | `ErpFinFundAccount` | accountType=BANK | orgId |
| `ErpFinDashboardBizModel.java:214-227` | `sumArApOpen` | `ErpFinArApItem` | direction+status | acctSchemaId, orgId |
| `ErpFinPostingProcessor.java:495-512` | `resolveOpenPeriod` | `ErpFinAccountingPeriod` | date range → `periods.get(0)` | orgId |
| `ErpFinAccountingPeriodProcessor.java:636-657` | `findOrCreatePeriodStatus` | `ErpFinAccountingPeriodStatus` | periodId → `list.get(0)` | acctSchemaId |
| `ErpFinAccountingPeriodProcessor.java:575-584` | `findUnsettledArApCodes` | `ErpFinArApItem` | date range | orgId, acctSchemaId |

**影响**：多账套部署（`erp-fin.multi-schema-enabled=true`）时，资产负债表/利润表/现金流量表/所有看板 KPI（收入/费用/净利润/银行余额/AR/AP 余额）**双计或三计**——同一业务事件产生 N 张凭证（每账套一张），这些读路径全汇总。子系统明显为单账套默认设计（注释「向后兼容现状」/「原全表加载」）。

### 4.3 ErpFinGlBalance 自然键结构缺口

`ErpFinGlBalance`（`app-erp-finance.orm.xml:904-980`）含 mandatory `orgId`（`:909`）+ `acctSchemaId`（`:910`）+ `periodId`/`subjectId`/`currencyId` + 4 辅助维度，**但无 `<unique-key>` 定义**——仅非唯一索引（`:955-980`）。预期自然键 `(orgId, acctSchemaId, periodId, subjectId, currencyId, ...)` **未 DB 强制**，正确性完全依赖应用层 upsert 逻辑。

**与 P0-MA2-020 同型结构缺口**——P0-MA2-020 已为 inventory 余额加 UK 收口，finance GL 余额无 UK 是平行缺口（但因 GL 余额由过账引擎单线程维护、非并发 check-then-insert，并发腐败风险低于 inventory，降级 P1）。

### 4.4 ErpFinVoucherBillR 关键事实（P0-MA2-018 复核输入）

**billR 无 `acctSchemaId` 列**（`app-erp-finance.orm.xml:626-639` 实测确认）。列集：`id, voucherId, billType, billCode, billLineCode, businessType, delVersion, version, createdBy/Time, updatedBy/Time`。仅索引 `IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID`（`:643-647` 非唯一）。

**billR 按 schema 隔离间接**——经 `voucherId → ErpFinVoucher.acctSchemaId` join 可恢复 schema。任何**仅在 billR 上的查询**（如 `ErpFinPostingProcessor.findBillLinks:884-890` 仅按 `billCode`+`businessType`）**无法区分账套**，返回所有账套的行。

**多账套传播对同一业务单据产生 N 行 billR**（每凭证/账套一行），均在 `persistVoucher:836-843` 创建，每行 `billR.setVoucherId(voucherId)` 指向账套特定凭证。

### 4.5 裁决

**VERDICT: 维度 PARTIAL FAIL → P1-MA2-095 + P1-MA2-096**

- **写路径 PASS**：凭证/分录行/多账套传播/红冲在写时正确 stamp `acctSchemaId`
- **读路径 FAIL → P1-MA2-095**：报表 + 看板 + 期间前置检查 12 处查询省略 `acctSchemaId`+`orgId` 过滤，多账套部署双计。归 MR1 补作用域过滤。
- **GL 余额自然键结构 FAIL → P1-MA2-096**：与 P0-MA2-020 同型，但 GL 余额由过账引擎单线程维护，并发风险低于 inventory，降级 P1。归 MR1 加 UK 收口。

## 5. 维度四：法人根解析正确性与性能

> 控制点：「`resolveLegalEntityRoot` walk-up 在深层组织树/环形引用下的正确性 + N+1 查询风险（性能归 A7.3，但环形引用致无限递归属正确性）」

### 5.1 实现核实

`ErpFinIntercompanyTransferBizModel.java:207-226`：

```java
207: Long resolveLegalEntityRoot(Long orgId) {
208:     Map<Long, Boolean> visited = new HashMap<>();
209:     Long current = orgId;
210:     IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
211:     while (current != null && visited.putIfAbsent(current, Boolean.TRUE) == null) {
212:         ErpMdOrganization org = dao.getEntityById(current);
213:         if (org == null) {
214:             return null;
215:         }
216:         if (ErpFinConstants.ORG_TYPE_COMPANY.equals(org.getOrgType())) {
217:             return current;
218:         }
219:         // 集团顶层无 COMPANY 时，退而认顶层组织为法人根（向后兼容单公司场景）
220:         if (ErpFinConstants.ORG_TYPE_GROUP.equals(org.getOrgType()) && org.getParentId() == null) {
221:             return current;
222:         }
223:         current = org.getParentId();
224:     }
225:     return null;
226: }
```

### 5.2 各场景裁决

| 关注点 | 裁决 | 证据 |
|---|---|---|
| 环形检测（visited set） | **PRESENT** | L208（visited map）+ L211（`putIfAbsent(current, TRUE) == null` 作循环条件） |
| 空守卫（不存在的 id） | **PRESENT** | L213-215 `getEntityById` 返回 null → return null |
| 空守卫（null 输入） | **PRESENT** | L211 `current != null` 短路 |
| 终止 — orgType==COMPANY | YES | L216-218 |
| 终止 — parentId==null | 部分 | 仅当 orgType==GROUP 在顶层（L220-222）；DEPARTMENT/WORKSHOP 叶子 parentId==null 会落入 `current = null` 后从 L211 退出返回 null |
| 环形 A→B→A 行为 | **SAFE** | 重访 A 时 `putIfAbsent` 返回既有 TRUE（非 null）→ 循环条件 false → 退出 → return null。无无限循环。 |
| 深层树（100 级） | **SAFE — 迭代** | `while` 循环非递归。每级一次 DB 查询（N+1 性能风险，归 A7.3，非正确性）。无栈溢出。 |
| parentId → 不存在 id | **SAFE** | 下次迭代 `getEntityById` 返回 null → return null |

### 5.3 ErpMdOrganization DB 约束

`module-master-data/model/app-erp-master-data.orm.xml:1075-1118`：
- `parentId` **NULLABLE — 无 `mandatory="true"`**（L1085）
- `orgType` mandatory（L1086，dict `erp-md/org-type`）
- UK 仅 `code`（L1108 `UK_MD_ORGANIZATION_CODE`）
- `<to-one name="parent">`（L1097-1099）**无 DB 级 FK / 无环防止**
- `parentId` 索引非唯一（L1111-1113 `IDX_MD_ORGANIZATION_PARENT_ID` unique="false"）

**DB 提供零环防止**。正确性完全依赖应用层 `visited` 守卫——好在守卫**存在且正确**。

### 5.4 调用方

- 类内 `ErpFinIntercompanyTransferBizModel.java:74-75`（`onTransferConfirmed` 库存 intercompany 路径，解析双方 `fromLegalId`/`toLegalId`）
- 类内 `:115`（`onTradeDocumentApproved` PO/SO intercompany 路径，解析 `executingLegal`）
- **合并/抵消代码不调用**（`generateEliminationCandidates`/`postElimination` 继承 Match 记录的 pairKey，不重新解析法人根）

### 5.5 裁决

**VERDICT: 维度 PASS ✅**

环形引用正确性守卫**存在且正确**——`visited` set + `putIfAbsent` + 迭代 while 循环。深层树安全（迭代非递归）。空守卫完整。**无 P0 候选**（计划点「环形引用致无限递归/栈溢出」经证据**证伪**）。N+1 性能归 A7.3。

## 6. 维度五：自然键唯一性是否含 orgId（含 P0-MA2-018/020 复核）

> owner doc `multi-company.md:30` 声明：「单据编号在 orgId 内唯一」

### 6.1 全域事务单据 UK_*_CODE_ORG 覆盖

**所有事务性业务单据**（用户列出的 13 个 + ~70 个其他事务单据）**全部正确携带 `(code, orgId)` UK**：

| 实体 | UK | 列 | 文件:行 |
|---|---|---|---|
| `ErpPurOrder` | `UK_PUR_ORDER_CODE_ORG` | code, orgId | `app-erp-purchase.orm.xml:586` |
| `ErpPurReceive` | `UK_PUR_RECEIVE_CODE_ORG` | code, orgId | `:727` |
| `ErpPurInvoice` | `UK_PUR_INVOICE_CODE_ORG` | code, orgId | `:854` |
| `ErpPurPayment` | `UK_PUR_PAYMENT_CODE_ORG` | code, orgId | `:966` |
| `ErpPurReturn` | `UK_PUR_RETURN_CODE_ORG` | code, orgId | `:1069` |
| `ErpPurRequisition` | `UK_PUR_REQUISITION_CODE_ORG` | code, orgId | `:132` |
| `ErpSalOrder` | `UK_SAL_ORDER_CODE_ORG` | code, orgId | `app-erp-sales.orm.xml:349` |
| `ErpSalDelivery` | `UK_SAL_DELIVERY_CODE_ORG` | code, orgId | `:513` |
| `ErpSalInvoice` | `UK_SAL_INVOICE_CODE_ORG` | code, orgId | `:659` |
| `ErpSalReceipt` | `UK_SAL_RECEIPT_CODE_ORG` | code, orgId | `:789` |
| `ErpInvStockMove` | `UK_INV_STOCK_MOVE_CODE_ORG` | code, orgId | `app-erp-inventory.orm.xml:189` |
| `ErpInvStockBalance` | `UK_INV_STOCK_BALANCE_NATURAL` | orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId | `:415`（**P0-MA2-020 修复后含 orgId**） |
| `ErpInvTransferOrder` | `UK_INV_TRANSFER_ORDER_CODE_ORG` | code, orgId | `:631` |
| `ErpMfgWorkOrder` | `UK_MFG_WORK_ORDER_CODE_ORG` | code, orgId | `app-erp-manufacturing.orm.xml:632` |
| `ErpFinVoucher` | `UK_FIN_VOUCHER_CODE_ORG` | code, orgId | `app-erp-finance.orm.xml:452`（orgId mandatory 在 `:421`） |
| `ErpFinAccountingPeriod` | `UK_FIN_ACCOUNTING_PERIOD_CODE_ORG` | code, orgId | `:682` |
| `ErpMdAcctSchema` | `UK_MD_ACCT_SCHEMA_CODE_ORG` | code, orgId | `app-erp-master-data.orm.xml:994`（orgId mandatory 在 `:968`） |
| `ErpMdWarehouse` | `UK_MD_WAREHOUSE_CODE_ORG` | code, orgId | `:574` |
| `ErpFinGlMappingRule`（biz） | `UK_FIN_GL_MAPPING_RULE_BIZ` | orgId, businessType, accountKey, acctSchemaId, partnerGroupId, materialCategoryId, warehouseId, departmentId, projectId | `:2055`（orgId 首位；NULL≠NULL 语义下 UK 仅强制全非 NULL 重复） |

### 6.2 全局 UK（无 orgId）——设计正确

| 实体 | UK | 严重性 | 说明 |
|---|---|---|---|
| `ErpMdOrganization` | `UK_MD_ORGANIZATION_CODE (code)` | None | 该实体**就是**组织注册表；全局 code 是外部身份 |
| `ErpMdSubject`/`ErpMdMaterial`/`ErpMdCurrency`/`ErpMdUoM`/`ErpMdLocation`/`ErpMdMaterialCategory`/`ErpMdSettlementMethod` | `(code)` | None | 全局主数据引用（单一主数据租户假设） |
| `ErpMdPartner` | `UK_MD_PARTNER_CODE (code)` | 已声明 | `multi-company.md:139` 显式：「`ErpMdPartner` 无 `orgId` 列」——架构决策；跨组织 intercompany 经 `ErpFinIntercompanyTransferPrice` 反向查找 |
| `ErpMdTaxRate` | `UK_MD_TAX_RATE_CODE (code)` | Medium | 无 per-org 税务管辖支持；多管辖税 successor |
| `ErpFinVoucherBillR` | （无 UK） | Low | 隐式经 voucherId FK 限定（凭证有 orgId/acctSchemaId）——见 P0-MA2-018 复核 §6.4 |

### 6.3 P0-MA2-020 复核（库存余额自然键 UK）

`UK_INV_STOCK_BALANCE_NATURAL (orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)`（`app-erp-inventory.orm.xml:415`，P0-MA2-020 fix plan `2026-07-28-1249-arm-fix-p0-ma2-020` Status: completed）。

**多公司维度复核结论**：✅ **方案 A 已正确含 orgId，对多公司隔离正确**——
- 不同组织同物料/仓库不碰撞（合法重号）
- 同组织并发首次移动单获 DB 保护
- owner doc 声明「库存按仓库隔离，仓库归属组织」（`multi-company.md:31`）——库存余额按 `(orgId, warehouse, material, ...)` 自然键成立

**P0-MA2-020 复核结论：UK 设计对多公司隔离正确，无须变更，fix plan 维持 done**。

### 6.4 P0-MA2-018 复核（业财回链表 UK）

`erp_fin_voucher_bill_r` **无 UK** + **无 orgId 列** + **无 acctSchemaId 列**（`app-erp-finance.orm.xml:623-647`）。

**多公司维度复核分析**：

1. **加 orgId 列是否足够修复 P0-MA2-018？** **否**。区分「并发重复正向 INSERT（应阻止）」与「合法多行（应允许）」所需的判别列：
   - `postingType`（NORMAL/REVERSAL）→ 区分红冲第 2 行
   - `isReversed`（原凭证被红冲标记）→ 区分冲销后重过账第 3 行
   - `acctSchemaId`（账套）→ 区分多账套传播第 N 行
   
   **此三列全部位于 `ErpFinVoucher` 而非 billR**。即使加 orgId，红冲「同键 2 行」（`TestErpFinPostingService.java:225` `assertEquals(2, countBillLinks("AP-REV-001", BUSINESS_TYPE_AP_INVOICE))`）+ 多账套「同键 N 行」（`TestErpFinMultiSchemaPosting.java:89` `assertEquals(2, vouchers.size())`）+ 软删除重插生命周期（`useLogicalDelete="true"` L625）三重冲突依然存在。

2. **多公司部署下 billCode 是否需要 orgId 前缀以避免跨组织重号？** **不必要**。billCode 由 `ErpFinPostingProcessor.java:925-930` 生成（`PST-{type}-{uuid}`），UUID 保证全局唯一；上游单据 code 在 `UK_*_CODE_ORG` 下 orgId 内唯一但不全局唯一——经 `event.getBillHeadCode()` 传入时已绑定特定组织凭证。

3. **多公司维度是否引入新的 P0-MA2-018 复杂度？** **否**。多公司部署下，每个组织独立过账，每组织独立凭证 + 独立 billR。同一业务事件跨组织 intercompany 时经配对凭证（INTERCOMPANY_SALE/AR + INTERCOMPANY_PURCHASE/AP）——这些走独立 `IntercompanyVoucherGenerator` 路径，billCode 各异（生成器接受 docCode 参数）。**多公司部署不增加 P0-MA2-018 的字面 UK 冲突面**。

**P0-MA2-018 复核结论**：✅ **deferred plan 的方向 A/B/C/D 裁决维持不变**——多公司维度**不改变** P0-MA2-018 的 disposition。deferred plan 已正确识别「判别列在 voucher 不在 billR」+「部分唯一索引/反范式化/悲观锁/分布式锁」四个 successor 方向。本审计**不重新打开** P0-MA2-018，亦**不改变**其 deferred 状态。

### 6.5 裁决

**VERDICT: 维度 PASS ✅**

- 全域 ~70 事务单据 UK_*_CODE_ORG 正确（owner doc「单据编号 orgId 内唯一」落地）
- 主数据全局 UK 是设计决策（单一主数据租户假设）
- **P0-MA2-020 复核结论**：UK 已正确含 orgId，多公司隔离正确，维持 done
- **P0-MA2-018 复核结论**：billR 无 acctSchemaId 列且加 orgId 不足修复；deferred plan 方向 A/B/C/D 维持；本审计不重新打开

**结论回填 3 个 P0 fix plan**：
- `2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk.md`：**维持 deferred**（多公司维度不改变 disposition；successor 方向 A/B/C/D 须经人工裁决）
- `2026-07-28-1249-arm-fix-p0-ma2-019-aps-capacity-lock.md`：**与本维度无关**（aps 排产产能并发，不触及多公司隔离——`UK_APS_CAPACITY_RESERVATION_SLOT (machineId, plannedStartT, plannedEndT)` machine 自身 org-scoped，多公司维度复核无新发现）
- `2026-07-28-1249-arm-fix-p0-ma2-020-inv-stock-balance-uk.md`：**维持 completed**（UK 已正确含 orgId，多公司隔离正确）

## 7. 维度六：跨公司配对/抵消作用域

> owner doc `multi-company.md:197`：「配对键 = `(pairKey, periodId)`，其中 `pairKey = min(fromOrgId,toOrgId) + ":" + max(fromOrgId,toOrgId) + ":" + materialId`」
> 控制点：「`runMatching`/`generateEliminationCandidates` 的 periodId + org 配对作用域——不同法人对的内部交易是否正确隔离」

### 7.1 runMatching 算法实测

`ErpFinIntercompanyMatchBizModel.java:56-97`：

**实测 pairKey = `billCode`**（**非 owner doc 声明的 min/max+materialId 算法**）：
- L86 `record.setPairKey(billCode)` — billCode 来自 `ErpFinVoucherBillR.billCode`（L129）
- L70-72 `allBillCodes` 是 SALE 侧 + PURCHASE 侧 billCode 键的并集
- L74 迭代 `for (String billCode : allBillCodes)`

**期间作用域**：YES。`findIntercompanyVoucherIdsByBillCode(billType, periodId)` L118 按 `eq("periodId", periodId)` 过滤凭证 + 排除 `isReversed=true`（L123-125）。

### 7.2 多法人对正确性

`pairKey = billCode`（非 org-pair hash）。每笔 intercompany 交易产生配对 SALE + PURCHASE 凭证**共享同一 billCode**（由 `intercompanyVoucherGenerator.generatePairedVouchers(docCode, ...)` L149-150 设置，docCode 是 PO/SO code 或 `TRANSFER-{id}`）。

**结论**：对**合法配对凭证流**，billCode 正确区分同期多对——因为每笔交易有全局唯一 billCode（PO/SO code 经 `UK_*_CODE_ORG` orgId 内唯一但配对凭证 docCode 经 UUID 或组织内唯一 code 已足够区分；TRANSFER-{id} 全局唯一）。

**理论跨对污染风险（低）**：`runMatching` 不做 SALE 侧凭证 org 与 PURCHASE 侧凭证 org 的 org-pair 一致性校验。若两笔交易意外共享 billCode（如手工过账碰撞 code），会被静默合并。

### 7.3 ErpFinIntercompanyMatch 审计列全空（数据质量缺口）

`ErpFinIntercompanyMatch` 实体（`app-erp-finance.orm.xml:2126-2173`）有列：
- `arOrgId`（L2136）/ `apOrgId`（L2138）/ `arSideVoucherId`（L2135）/ `apSideVoucherId`（L2137）/ `materialId`（L2139）

**`runMatching` 从不填充此五列**（L84-91 仅设 `code`/`orgId=1L hardcoded`/`pairKey=billCode`/`periodId`/`matchedAmount`/`diffAmount`/`status`）。

**后果**：
- Match 记录不携带 org-pair 证据——org-pair 仅可经 `billCode`/`voucherId` 反查凭证恢复
- `checkDualSideConsistency:154-188`（L175 `row.setPartnerId(m.getArOrgId())`）→ `partnerId` 永远 null，差异报告退化
- 下游抵消候选追溯链断

### 7.4 runMatching 非幂等

`ErpFinIntercompanyMatch` 无 `(pairKey, periodId)` UK（L2164-2167 `IDX_FIN_INTERCOMPANY_MATCH_PAIR_PERIOD` unique="false" 非唯一索引）→ **同期重复调用 `runMatching` 产生重复 Match 行**（每次调用 code UUID 后缀，code UK 不阻止）。

### 7.5 generateEliminationCandidates 作用域

`ErpFinConsolidationEliminationBizModel.java:55-127`：
- 3 抵消类型：AR_AP（L74-86）/ REVENUE_COST（L89-101）/ INVENTORY_PROFIT（L104-118 config-gated `erp-fin.elimination-inventory-profit-enabled`）
- 源查询：`ErpFinIntercompanyMatch` 过滤 `periodId` + `status=MATCHED`（L64-68）
- 每个 match，候选继承 `m.getPairKey()`（L80/95/111）→ **作用域 = pairKey = billCode → 每笔交易隔离传递性保持**
- **但** `orgId` hardcoded `1L`（L77/92/108）；`fromOrgId`/`toOrgId` 列存在（orm.xml L2189-2190）但**从不设置**

### 7.6 ErpFinIntercompanyTransferPrice 多对手歧义

`ErpFinIntercompanyTransferPrice`（`app-erp-finance.orm.xml:2076-2122`）：
- `fromOrgId`/`toOrgId` 均 NULLABLE（空=通配，L2084-2085）
- UK 仅 `code`（L2111），`(fromOrgId, toOrgId)` 索引非唯一（L2114-2117）
- **不强制自然键唯一性** → 多个 active 规则同 org 对允许
- 配合 `resolveCounterpartyLegalEntity:174-192` 的 `setLimit(1)` 无 `orderBy` → 执行法人有**多对手方**时（A 与 B 和 C 都交易），resolver **任意非确定性**挑一个

`materialId` 是声明列（L2086）但**counterparty 解析时不查询**。

### 7.7 裁决

**VERDICT: 维度 PARTIAL FAIL → P1-MA2-097 + P1-MA2-098**

- **配对键设计本身对合法流 PASS**（billCode 区分多法人对）
- **FAIL → P1-MA2-097**：(a) owner doc `multi-company.md:197` pairKey 算法描述（`min/max+materialId`）与实现（`billCode`）漂移；(b) `arOrgId`/`apOrgId`/`arSideVoucherId`/`apSideVoucherId`/`materialId` 五列从不填充 → 配对审计/抵消追溯断链
- **FAIL → P1-MA2-098**：`runMatching` 非幂等，无 `(pairKey, periodId)` UK → 同期重复调用产生重复 Match 行

owner doc `multi-company.md:142` 已 Deferred「精确 partner→法人关联（多对手消歧）」归 successor，本审计不重复登记。

## 8. 维度七：配置继承与 cache key

> owner doc `multi-company.md:52-60`：「科目表/成本核算方法/折旧方法按账套或公司独立」
> 控制点：「cache key 是否含 orgId/acctSchemaId（避免跨组织配置串用）」

### 8.1 GL 映射 cache（默认配置泄漏面）

`ErpFinGlMappingResolver.java`：
- Cache key（L289-291）：`(orgId == null ? "_" : orgId.toString()) + ":" + businessType + ":" + accountKey`（**acctSchemaId 故意不在 key**——它是通配维度，在 `matches():163-165` 匹配）
- org 维度 config（L51, L246-248）：`erp-fin.gl-mapping.org-dimension-enabled` **默认 `false`**
- OFF（默认）时：`resolveOrgIdFromDimensions()` 返回 null（L236-238）；`reloadCache` 把所有规则归入 `"_"` bucket（L278）；`matches()` 跳过 orgId 校验（L159 守卫 `isOrgDimensionEnabled()`）→ **orgA 规则可匹配 orgB 过账请求**
- ON 时：`reloadCache` 按 `rule.getOrgId()` 分桶；查询用请求真实 orgId；`matches()` 强制 `Objects.equals(rule.getOrgId(), dims.getOrgId())`（L159-162）；`specificity()` 计 orgId（L148）→ **正确按 orgId 分区，无跨组织泄漏**（e2e 测试 `gl-mapping-rules.md:514,517-518` 验证）

**owner doc `multi-company.md:244-249` 已标注「dormant」**——本审计正式登记为多公司部署的活跃泄漏面。

### 8.2 转移定价 cache（正确）

`ErpFinTransferPriceResolver.java`：
- Cache key（L203-206）：`(fromOrgId == null ? "_" : fromOrgId.toString()) + ":" + (toOrgId == null ? "_" : toOrgId.toString())`——**正确方向性**
- Matcher（L97-102）**无条件 orgId 强制**（非 config-gated）：
  ```java
  if (rule.getFromOrgId() != null && !Objects.equals(rule.getFromOrgId(), fromOrgId)) return false;
  if (rule.getToOrgId() != null && !Objects.equals(rule.getToOrgId(), toOrgId)) return false;
  ```
- 通配符明确 per-rule opt-in

### 8.3 其他 cache / 解析器

| 解析器/Cache | Cache? | Key 含 orgId? | 裁决 |
|---|---|---|---|
| `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)` | NO | N/A — 每次 `eq("orgId", orgId)` 查询 | SAFE |
| `ErpMdAcctSchemaBizModel.findFirstByOrg` | NO | N/A | SAFE |
| `CostMethodResolver.resolve(line, acctSchemaId)` | NO | N/A — 每次 `acctSchemaId` 解析 `schema.getCostingMethod()` | SAFE |
| 折旧 `ErpAstDepreciationScheduleProcessor.executeDepreciation` | NO | N/A — 按 asset 解析（asset 有 orgId UK_AST_ASSET_CODE_ORG；category 全局但 org 从 asset 流入） | SAFE |
| 税率 `ErpMdTaxRate` | NO | N/A — 全局共享，无 orgId 列 | 无泄漏，但无 per-org 支持（successor） |
| CoA `ErpMdAcctSchemaCoa` | NO | N/A — 按 acctSchemaId FK | SAFE |
| DRP/MRP 仿真参数 resolver | YES | per-scenarioId（scenario 经 UK_*_SCENARIO_CODE_ORG org 上游限定） | SAFE |
| 汇率 cache | YES | per-currency triplet | SAFE（currency 全局） |

### 8.4 裁决

**VERDICT: 维度 PARTIAL FAIL → P1-MA2-099**

- GL 映射 cache **默认配置（org-dimension-enabled=false）下跨组织泄漏**——多公司部署必须显式启用 config。owner doc 已标注 dormant，本审计正式登记 P1 → MR1。
- 其他 cache（转移定价/AcctSchema/CostMethod/折旧/CoA）**全部 SAFE**——无跨组织/跨账套 cache 串用。

## 9. 维度八：数据权限多公司角度

> 与 A6.3 交叉点——本审计只核验多公司 orgId 隔离角度，不深入角色矩阵抽样（归 MA6）

### 9.1 运行时数据权限机制

如 §2 所述：
- 平台 `DefaultDataAuthChecker.getFilter()`（`nop-entropy/nop-auth/nop-auth-service/.../DefaultDataAuthChecker.java:195-213`）在无 `<obj>` 规则时返回 null
- 19 个 `erp-{module}.data-auth.xml` **全部 `<objs/>`**（空规则）
- 项目侧 0 个自定义 `IDataAuthChecker` / 0 个 `IQueryTransformer` 注册

**结论**：`CrudBizModel.prepareFindPageQuery:381` 的 `AuthHelper.appendFilter` 对本项目所有 bizObj **永远 no-op**。

### 9.2 数据权限运行时有效性裁决

**VERDICT: 维度 FAIL（同 P1-MA2-093 根因）**

多公司 orgId 隔离角度的运行时有效性 = **零**——既无平台 tenant 机制（项目 0 个 useTenant），亦无项目侧 data-auth 规则（19 模块全空），亦无 `IUserContext.getOrgId()` API。

**与 A6.3 交叉点**：A6.3（数据权限运行验证）将复核角色/权限矩阵深度抽样；本审计仅标注多公司角度的运行时有效性 = 零。两者在「orgId 运行时无强制」上交叉，根因同一（P1-MA2-093）。归 A6.3 successor 深入角色维度。

## 10. 维度九：与设计文档一致性

owner doc `multi-company.md §数据隔离`（L27-32）+ 各域 README/cross-domain.md 多组织声明 vs 实现逐项裁决：

| owner doc 声明 | 实现 | 裁决 |
|---|---|---|
| `multi-company.md:29`「所有业务单据按 orgId 隔离查询」 | **未实现**——平台/项目侧均无 orgId 自动过滤 | ⚠️ **FAIL → P1-MA2-093**（owner doc 声明 vs 实现差距） |
| `multi-company.md:30`「单据编号在 orgId 内唯一」 | **实现**——全域 ~70 事务单据 UK_*_CODE_ORG 正确 | ✅ **PASS** |
| `multi-company.md:31`「库存按仓库隔离，仓库归属组织」 | **实现**——`UK_MD_WAREHOUSE_CODE_ORG (code, orgId)` + `UK_INV_STOCK_BALANCE_NATURAL` 含 orgId | ✅ **PASS** |
| `multi-company.md:32`「凭证按 acctSchemaId（账套）隔离」 | **部分实现**——写路径 stamp 正确，读路径（报表/看板/期间前置）省略 acctSchemaId 过滤 | ⚠️ **FAIL → P1-MA2-095**（读路径） |
| `multi-company.md:197` pairKey = `min(fromOrgId,toOrgId)+":"+max+":"+materialId` | **实现漂移**——实测 `pairKey = billCode` | ⚠️ **FAIL → P1-MA2-097**（owner doc 算法描述漂移） |
| `multi-company.md:244-249` GL 映射 orgId 维度「dormant」经 config-gate `erp-fin.gl-mapping.org-dimension-enabled`（默认 false）激活 | **一致**——config-gate 默认 false，开启后正确按 orgId 分区 | ✅ **PASS（但多公司部署须显式启用 → P1-MA2-099）** |
| `multi-company.md:139`「`ErpMdPartner` 无 orgId 列」 | **一致**——`ErpMdPartner` UK_MD_PARTNER_CODE (code) 无 orgId；intercompany 经 `ErpFinIntercompanyTransferPrice` 反向查找 | ✅ **PASS（owner doc Deferred successor 一致）** |
| `multiple-accounting-schemas.md:98-104`「凭证/分录行/库存流水/存货成本层/总账余额均带 acctSchemaId」 | **一致**——`ErpFinVoucher.acctSchemaId`/`ErpFinVoucherLine.acctSchemaId`/`ErpFinGlBalance.acctSchemaId` mandatory | ✅ **PASS（写路径）** |
| `multiple-accounting-schemas.md:243-247`「数据隔离：用户只能访问被授权的账套；未授权账套对用户不可见」 | **未实现**——无账套级 data-auth 规则 + 无 `IUserContext.getAcctSchemaId()` | ⚠️ **FAIL → P1-MA2-095**（账套级权限未落地，与 orgId 隔离同根因） |

## 11. P0 候选项证伪/降级裁决

计划点列出 4 个 P0 候选（`[若破坏 ... 不变量]`）：

| 候选 P0 | 触发条件 | 证据裁决 | 终态 |
|---|---|---|---|
| **@BizQuery 跨组织泄漏致 A 公司查到 B 公司单据** | 若破坏 orgId 隔离不变量 | **不变量确实未落地**（P1-MA2-093），但单组织种子（176 行全 orgId=2）下**无实际跨组织数据腐败发生**；这是「能力缺失」非「活跃缺陷」；修复属架构级补能力（Non-Goal 不实现多公司特性） | **降级 P1-MA2-093** |
| **账套串户致不同核算体系凭证混算** | 若破坏 acctSchemaId 隔离 | **写路径不破坏**（多账套传播 stamp 正确）；读路径（报表/看板）聚合泄漏但**仅在 `multi-schema-enabled=true` 时显现**，默认 off 不破坏；多账套部署须显式启用 | **降级 P1-MA2-095** |
| **法人根解析环形引用致无限递归/栈溢出** | 若破坏组织树不变量 | **守卫存在且正确**——`resolveLegalEntityRoot:208` visited set + `putIfAbsent` + 迭代 while；环形 A→B→A 安全退出 return null；深层树迭代无栈溢出 | **证伪 PASS** |
| **合并抵消作用域错配致跨法人对内部交易误抵消** | 若破坏配对键不变量 | **配对键设计本身对合法流正确**（billCode 区分多法人对）；但 owner doc 算法描述漂移 + 审计列空（P1-MA2-097）+ runMatching 非幂等（P1-MA2-098）；**实际「误抵消」候选仅理论**（须手工 billCode 碰撞） | **降级 P1-MA2-097 + P1-MA2-098** |

**零 P0 sustained**。

## 12. 控制点 PASS/FAIL 汇总

| # | 控制点 | 裁决 | Finding |
|---|---|---|---|
| 1 | CrudBizModel 默认查询 orgId 过滤机制 | FAIL | P1-MA2-093 |
| 2 | 自定义 @BizQuery orgId 过滤完整性 | FAIL | P1-MA2-093 |
| 3 | 看板/报表聚合 orgId 作用域 | FAIL | P1-MA2-093 + P1-MA2-095 |
| 4 | 凭证/余额/科目 acctSchemaId 隔离彻底性 | PARTIAL | P1-MA2-095 + P1-MA2-096 |
| 5 | 过账 Provider 解析 acctSchema 路径 | PASS | — |
| 6 | 跨账套查询泄漏 | FAIL | P1-MA2-095 |
| 7 | 多账套并行余额/科目互不串户 | PASS（写）/FAIL（读） | P1-MA2-095 |
| 8 | ErpFinAccountingPeriod 按账套隔离 | PARTIAL（period 按 org；close state 按 schema 经 ErpFinAccountingPeriodStatus） | — |
| 9 | resolveLegalEntityRoot 环形引用正确性 | PASS | — |
| 10 | resolveLegalEntityRoot 深层树正确性 | PASS | — |
| 11 | 单据编号唯一约束 orgId 维度 | PASS | — |
| 12 | P0-MA2-018 erp_fin_voucher_bill_r UK orgId 维度复核 | 维持 deferred | 不重新打开 |
| 13 | P0-MA2-020 erp_inv_stock_balance 自然键 UK orgId 维度复核 | PASS（已正确含 orgId） | 维持 done |
| 14 | ErpFinIntercompanyTransferPrice orgId 维度 | PARTIAL（materialId 维度声明但不查询；多对手歧义 successor） | — |
| 15 | runMatching/elimination 配对作用域 | PARTIAL | P1-MA2-097 + P1-MA2-098 |
| 16 | pairKey 算法在多对法人下正确性 | PARTIAL（合法流正确；owner doc 漂移） | P1-MA2-097 |
| 17 | GL 映射 cache key orgId 维度 | PARTIAL（默认 off 泄漏） | P1-MA2-099 |
| 18 | 成本核算方法 cache key | PASS | — |
| 19 | 折旧方法 cache key | PASS | — |
| 20 | 转移定价 cache key | PASS | — |
| 21 | 数据权限运行时（多公司角度） | FAIL（同根因 P1-MA2-093） | P1-MA2-093 |
| 22 | 与 multi-company.md 数据隔离声明一致性 | PARTIAL | P1-MA2-093/095/097 |
| 23 | 与 multiple-accounting-schemas.md 账套隔离声明一致性 | PARTIAL | P1-MA2-095 |

## 13. orgId 查询隔离覆盖矩阵

> 全 19 域 × @BizQuery × orgId 过滤机制

| 域 | 默认 findPage orgId 过滤 | 自定义 @BizQuery orgId 过滤 | 看板聚合 orgId 过滤 | 主数据 UK 含 orgId | 终态 |
|---|---|---|---|---|---|
| finance | ❌ 平台 no-op | ❌ `ErpFinVoucherBizModel.previewReverseVoucher` 无 | ❌ `ErpFinDashboardBizModel` 全泄漏 | ✅ `UK_FIN_VOUCHER_CODE_ORG`/`UK_FIN_ACCOUNTING_PERIOD_CODE_ORG`/`UK_FIN_AR_AP_ITEM_CODE_ORG`/`UK_FIN_GL_MAPPING_RULE_BIZ` | ⚠️(P1) |
| mfg | ❌ | ❌ `findWorkOrdersPendingJobCards` 经处理器直访 | ❌ `ErpMfgDashboardBizModel` | ✅ `UK_MFG_WORK_ORDER_CODE_ORG`/`UK_MFG_MRP_PLAN_CODE_ORG` 等 | ⚠️(P1) |
| hr | ❌ | （抽样无自定义查询） | （hr 无 dashboard） | ✅ `UK_HR_EMPLOYEE_CODE_ORG`/`UK_HR_LEAVE_BALANCE_EMP_TYPE_YEAR_ORG` | ⚠️(P1) |
| assets | ❌ | （无） | ❌ `ErpAstDashboardBizModel` | ✅ `UK_AST_ASSET_CODE_ORG` | ⚠️(P1) |
| pur | ❌ | （无自定义 @BizQuery；helper 内 `existsActiveByRequisition` 直访） | ❌ `ErpPurDashboardBizModel` | ✅ `UK_PUR_ORDER_CODE_ORG` 等 8 个 | ⚠️(P1) |
| sal | ❌ | （无） | ❌ `ErpSalDashboardBizModel` | ✅ `UK_SAL_ORDER_CODE_ORG` 等 7 个 | ⚠️(P1) |
| qa | ❌ | （无） | ❌ `ErpQaDashboardBizModel` | ⚠️ `UK_QA_INSPECTION_CODE_ORG` 等 inspection 有；NCR/Recall 全局 | ⚠️(P1) |
| crm | ❌ | （helper `loadActiveRules` 接收 orgId 参数——功能作用域非用户隔离） | （crm 无独立 dashboard） | ✅ Lead/Event 等 | ⚠️(P1) |
| prj | ❌ | （无） | ❌ `ErpPrjDashboardBizModel` | ✅ Project 等 | ⚠️(P1) |
| cs | ❌ | （无） | ❌ `ErpCsQualityDashboardBizModel` | ✅ Ticket 等 | ⚠️(P1) |
| ct | ❌ | （无） | （无） | ⚠️ InvoicePlan 等 | ⚠️(P1) |
| b2b | ❌ | （无） | （无） | ⚠️ EDI/ASN 等 | ⚠️(P1) |
| inv | ❌ | （`ErpInvStockBalanceBizModel` 15 行空类用默认） | ❌ `ErpInvDashboardBizModel` | ✅ `UK_INV_STOCK_BALANCE_NATURAL` 含 orgId + `UK_INV_*_CODE_ORG` 多个 | ⚠️(P1) |
| md | ❌ | （helper `findFirstByOrg` 接收 orgId 参数） | ❌ `ErpMdDashboardBizModel` | ⚠️ AcctSchema/Warehouse/Employee/CostCenter 含 orgId；Material/Partner/Subject/Currency/UoM/TaxRate 全局 | ⚠️(P1) |
| mnt | ❌ | （无） | ❌ `ErpMntDashboardBizModel` | ⚠️ `UK_MNT_*_CODE` 全局 | ⚠️(P1) |
| drp | ❌ | （无） | （无） | ⚠️ `UK_DRP_SCENARIO_CODE_ORG`（scenario 含）；其他全局 | ⚠️(P1) |
| aps | ❌ | （无） | （无） | ⚠️ `UK_APS_CAPACITY_RESERVATION_SLOT`（machine 自身 org-scoped） | ⚠️(P1) |
| log | ❌ | （无） | （无） | ⚠️ Shipment 等 | ⚠️(P1) |
| notify | N/A（跨域通知派发子系统，非业务单据） | N/A | N/A | N/A | N/A |

**矩阵结论**：全域 18 业务域 orgId 查询隔离**全部 ⚠️(P1)**——既无平台机制又无项目侧规则。md/drp/notify 部分维度因主数据/跨域子系统 N/A。UK 层基本正确（事务单据含 orgId），但读路径零强制。

## 14. acctSchemaId（账套）隔离裁决表

| 实体 | 写路径 stamp acctSchemaId | 读路径过滤 acctSchemaId | 自然键含 acctSchemaId | 裁决 |
|---|---|---|---|---|
| `ErpFinVoucher` | ✅ mandatory `:422` + `persistVoucher:785` | ❌（报表/看板省略） | ✅ `UK_FIN_VOUCHER_CODE_ORG (code, orgId)`（code UUID 生成，schema 在列不在键） | ⚠️(P1-MA2-095) |
| `ErpFinVoucherLine` | ✅ mandatory `:489` + `:820` | ❌（报表 `loadPostedVoucherLines` 省略） | — | ⚠️(P1-MA2-095) |
| `ErpFinGlBalance` | ✅ mandatory `:910`（由过账引擎维护） | ❌（报表/看板 `loadGlBalances` 省略） | ❌ **无 UK**（仅非唯一索引） | ⚠️(P1-MA2-095 + P1-MA2-096) |
| `ErpFinArApItem` | ✅（generator 写） | ❌（看板 `sumArApOpen` 省略） | ✅ `UK_FIN_AR_AP_ITEM_CODE_ORG (code, orgId)` | ⚠️(P1-MA2-095) |
| `ErpFinVoucherBillR` | ✅ 经 voucherId 间接 | ❌（billR 无 acctSchemaId 列；查询不可区分） | ❌ 无 UK 无 acctSchemaId 列 | P0-MA2-018 维持 deferred |
| `ErpFinAccountingPeriod` | ✅ mandatory orgId（period 按 org） | ❌（`resolveOpenPeriod`/`findLatestPeriodId` 省略 orgId） | ✅ `UK_FIN_ACCOUNTING_PERIOD_CODE_ORG (code, orgId)` | ⚠️(P1-MA2-095) |
| `ErpFinAccountingPeriodStatus` | ✅ mandatory acctSchemaId `:699` | ❌（`findOrCreatePeriodStatus`/`loadPeriodStatus` 省略 acctSchemaId） | — | ⚠️(P1-MA2-095) |
| `ErpInvStockLedger`/`ErpInvCostLayer` | ✅（costing 写） | （库存域查询经库存余额自然键含 orgId 间接隔离） | （库存余额 UK_INV_STOCK_BALANCE_NATURAL 含 orgId） | ✅ PASS |

## 15. 法人根解析正确性裁决

| 场景 | 守卫 | 裁决 |
|---|---|---|
| 环形 A→B→A | `visited` set + `putIfAbsent` 循环条件（L208, L211） | ✅ SAFE（重访退出 return null） |
| 深层 100 级 | `while` 迭代非递归 | ✅ SAFE（无栈溢出；N+1 性能归 A7.3） |
| null 输入 | L211 `current != null` 短路 | ✅ SAFE |
| parentId → 不存在 id | L213-215 `getEntityById==null` → return null | ✅ SAFE |
| DEPARTMENT 叶子 parentId==null | L220-222 仅 GROUP 顶层 fallback；其他落入 `current=null` 退出 return null | ✅ SAFE（返回 null 表示无 COMPANY 根，调用方应处理） |
| DB 层环防止 | `ErpMdOrganization.parentId` nullable + 非唯一索引 + 无 FK | ⚠️ 无 DB 守卫（依赖应用层 visited，好在存在） |

**裁决：PASS ✅**——环形引用正确性经应用层 visited set 守卫保证，无 P0 候选。

## 16. 自然键 orgId 维度裁决（含 P0-MA2-018/020 复核结论）

### 16.1 业务单据编号唯一约束 orgId 维度

**PASS ✅**——全域 ~70 事务单据 UK_*_CODE_ORG (code, orgId) 正确落地。

### 16.2 P0-MA2-018 复核结论

**billR UK orgId 维度复核**：**维持 deferred，不重新打开**。

理由：(1) billR 无 acctSchemaId 列；(2) 区分合法多行（红冲/多账套/重过账）所需判别列（postingType/isReversed/acctSchemaId）全在 voucher 不在 billR；(3) 即使加 orgId，三重冲突（红冲「同键 2 行」+ 多账套「同键 N 行」+ 软删除重插）依然存在；(4) 多公司部署不增加字面 UK 冲突面（每组织独立凭证 + 独立 billR，intercompany 走独立生成器路径）。

deferred plan `2026-07-28-1249-arm-fix-p0-ma2-018` 的 successor 方向 A（部分唯一索引）/B（反范式化判别列到 voucher+复合 UK）/C（SELECT FOR UPDATE）/D（分布式锁 SPI）**维持不变**，须人工裁决后另起 successor plan。

### 16.3 P0-MA2-020 复核结论

**库存余额自然键 UK orgId 维度复核**：**维持 completed，无须变更**。

`UK_INV_STOCK_BALANCE_NATURAL (orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)` 已正确含 orgId——多公司隔离正确。

### 16.4 回填 3 个 P0 fix plan

| fix plan | 多公司维度复核结论 | 状态 |
|---|---|---|
| `2026-07-28-1249-arm-fix-p0-ma2-018-voucher-bill-r-uk.md` | 多公司维度不改变 disposition（billR 加 orgId 不足修复，三重冲突依然存在） | **维持 deferred** |
| `2026-07-28-1249-arm-fix-p0-ma2-019-aps-capacity-lock.md` | 与多公司隔离无关（aps 排产产能并发；machine 自身 org-scoped） | **维持既有状态**（本审计不改） |
| `2026-07-28-1249-arm-fix-p0-ma2-020-inv-stock-balance-uk.md` | UK 已正确含 orgId，多公司隔离正确 | **维持 completed** |

## 17. 配对/抵消作用域裁决

| 控制点 | 裁决 | Finding |
|---|---|---|
| pairKey 区分同期多法人对（合法流） | PASS（billCode 全局唯一区分） | — |
| owner doc pairKey 算法描述一致性 | FAIL（漂移：声明 min/max+materialId，实现 billCode） | P1-MA2-097 |
| ErpFinIntercompanyMatch arOrgId/apOrgId/arSideVoucherId/apSideVoucherId/materialId 填充 | FAIL（五列从不填充） | P1-MA2-097 |
| runMatching 幂等性 | FAIL（无 (pairKey, periodId) UK） | P1-MA2-098 |
| generateEliminationCandidates 按 pairKey 作用域 | PASS（候选继承 m.getPairKey） | — |
| postElimination 按 org 隔离 | PARTIAL（orgId hardcoded 1L；fromOrgId/toOrgId 不设） | P1-MA2-097（合并裁决） |
| 多对手方法人消歧 | Deferred（owner doc `multi-company.md:142` 已声明 successor） | — |

## 18. 配置继承 cache key 裁决

| Cache/Resolver | Cache? | Key 含 orgId? | 多公司隔离裁决 |
|---|---|---|---|
| ErpFinGlMappingResolver | YES | **默认 NO**（org-dimension-enabled=false 时归 `"_"` bucket） | ⚠️(P1-MA2-099) |
| ErpFinTransferPriceResolver | YES | YES（(fromOrgId, toOrgId)） | ✅ PASS |
| AcctSchemaResolver | NO | N/A（每次 eq("orgId", orgId)） | ✅ PASS |
| ErpMdAcctSchemaBizModel.findFirstByOrg | NO | N/A | ✅ PASS |
| CostMethodResolver | NO | N/A（每次 acctSchemaId 解析） | ✅ PASS |
| DepreciationScheduleProcessor | NO | N/A（按 asset 解析，asset org 上游） | ✅ PASS |
| CoA (ErpMdAcctSchemaCoa) | NO | N/A（按 acctSchemaId FK） | ✅ PASS |
| ErpMdTaxRate | NO | N/A（全局共享，无 orgId 列） | ⚠️ 无 per-org 支持（successor） |
| DRP/MRP 仿真参数 resolver | YES | per-scenarioId（scenario org 上游） | ✅ PASS |
| 汇率 cache | YES | per-currency triplet | ✅ PASS |

## 19. 多维审计各维度裁决（反窄化自检）

> 每维度至少一句裁决（含「本维度无发现」）

1. **orgId 查询隔离彻底性**：FAIL——平台/项目侧均无 orgId 自动过滤，11 个看板绕过仅有的（空）认证管道。P1-MA2-093。
2. **orgId 写入正确性**：FAIL——orgId 客户端可任意指定，无 auto-stamp。P1-MA2-094。
3. **acctSchemaId 隔离**：PARTIAL FAIL——写路径彻底，读路径（报表/看板/期间前置）省略过滤。P1-MA2-095 + P1-MA2-096。
4. **法人根解析正确性与性能**：PASS——环形守卫存在且正确，N+1 性能归 A7.3。无发现。
5. **自然键唯一性含 orgId**：PASS——事务单据 UK_*_CODE_ORG 全域正确；P0-MA2-018 维持 deferred（多公司不改变 disposition）；P0-MA2-020 维持 completed（已含 orgId）。无新发现。
6. **跨公司配对/抵消作用域**：PARTIAL FAIL——owner doc 算法漂移 + 审计列空 + runMatching 非幂等。P1-MA2-097 + P1-MA2-098。
7. **配置继承与 cache key**：PARTIAL FAIL——GL 映射默认配置跨组织泄漏。P1-MA2-099。其他 cache SAFE。
8. **数据权限多公司角度**：FAIL（同 P1-MA2-093 根因）——19 模块 data-auth.xml 全空。归 A6.3 深入角色维度。
9. **与设计文档一致性**：PARTIAL FAIL——multi-company.md:29/32/197 + multiple-accounting-schemas.md:243-247 部分声明未实现。P1-MA2-093/095/097。

**多维审计反窄化自检**：审查跨 9 维度，非单维深挖。每维度至少一句裁决（含 PASS 维度）。

## 20. 残留风险与 successor

- **A6.3 数据权限运行验证（角色深度抽样）**：归 MA6——本审计仅标注多公司角度，不深入角色矩阵
- **A7.3 N+1 查询 / A7.2 索引完整性（隔离性能）**：归 MA7——`resolveLegalEntityRoot` walk-up N+1 + 报表/看板缺 acctSchemaId/orgId 索引覆盖
- **缺失的多公司特性（owner doc Deferred）**：`ErpMdPartner.orgId` 精确映射（多对手消歧）/ 实时合并报表渲染 / receive-delivery intercompany 联级 / per-org 税务管辖 / 账套级 data-auth——均归业务需求触发时 successor
- **3 个 P0 fix plan**：P0-MA2-018 维持 deferred（多公司不改变 disposition）；P0-MA2-019 与多公司无关；P0-MA2-020 维持 completed
- **7 项新 P1** → MR1：orgId 查询隔离 / orgId 写入 / acctSchemaId 读路径 / GL 余额自然键 / 配对 owner doc 漂移+审计列空 / runMatching 幂等 / GL 映射 cache 默认泄漏

## 21. 结论

A2.18 多账套/多公司隔离系统性审查完成（MA2 最后一项收口裁决）。

**核心结论**：多公司/多账套隔离的**写路径 + 自然键层基本成立**（事务单据 UK_*_CODE_ORG 全域正确 / 多账套传播 stamp acctSchemaId / 法人根解析环形守卫 / 转移定价 cache 方向性正确），但**读路径隔离机制全仓未落地**——这是「文档声明的多公司能力」（`multi-company.md:29`「所有业务单据按 orgId 隔离查询」）与「实际落地的单组织骨架」（19 模块 data-auth.xml 全空 + 无 `IUserContext.getOrgId()` + 报表/看板聚合无作用域）之间的系统性差距。**单组织种子（176 行全 orgId=2）掩盖了所有跨组织泄漏——单组织部署下无实际数据腐败**。

**A2.17 交接点复核收口**：
- P0-MA2-018：billR 加 orgId 不足修复（判别列在 voucher 不在 billR），deferred plan 方向 A/B/C/D 维持，**多公司维度不重新打开**
- P0-MA2-020：UK 已正确含 orgId，多公司隔离正确，**维持 completed**

**零 P0**（4 个候选 P0 经证据证伪或降级）。**7 项新 P1** 登记 arm-index 待 MR1（依赖 MA1+MA2 done，由 R1.0 展开机制转化为具体修复工作项行）。**MA2 全部 18 个工作项（A2.1–A2.18）现已全部 done**——MR1 R1.0 可启动。

多公司/多账套维度终态：**全域 ⚠️(P1)**——隔离正确性在 UK/写路径成立，读路径须 MR1 经「补全局 `IQueryTransformer` 或填充 `data-auth.xml` + 报表/看板作用域 + `IUserContext.getOrgId()` + GL 余额自然键 UK + 配对审计列填充 + runMatching 幂等 + GL 映射 config 默认值评估」收口。
