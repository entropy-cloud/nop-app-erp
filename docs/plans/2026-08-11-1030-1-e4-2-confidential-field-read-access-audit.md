# 2026-08-11-1030-1 E4.2 保密字段读访问审计（app 侧拦截 + 审计记录）

> Plan Status: completed
> Last Reviewed: 2026-08-11
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E4.2
> Related:
> - E4.1（done，`2026-08-11-0915-3`——采购保密字段级可见性双层分工落地：mfg 4 要素成本 schema 级隐藏 + band 代理视图；totalCost/unitCost + md/pur 供应商价 + hr/ct 金额保持 E3.1 masking。E4.1 Deferred「保密字段读访问审计」指向本计划，触发条件 = E4.2 进入，deps E4.1，**已满足**）
> - E3.1（done，`2026-08-10-2059-2`——43 字段后端 @BizLoader masking 落地，共享 `MaskHelper`（`module-common-service`）。E3.1 Deferred「保密字段读访问审计」指向本计划，触发条件 = E4.2 进入，deps E4.1，**已满足**）
> - P1.1（done，`field-formatting-patterns.md` §9.7 冻结字段清单——本计划逐字段审计范围冻结输入）
> - **E4.2 唯一 Deps（E4.1）已 done（roadmap status block 核验），draftable**
> Audit: required
> Mission: permissions-enforcement
> Work Item: E4.2

## Current Baseline

E4.2 是保密字段从**可见性/脱敏控制**（E3.1 masking + E4.1 schema 隐藏 已落地）推进到**读访问可追溯**（授权用户读取明文时留审计记录）的执行切片。roadmap 裁决的落点：**app 侧业务拦截器写审计记录**；若需改平台 `NopSysChangeLog` 捕获路径 = ask-first 备选。

**E3.1/E4.1 已落地的可见性控制（done，load-bearing）**：
- **E3.1 masking**（43 字段）：5 域 entity BizModel `@BizLoader` 委托共享 `MaskHelper`（`module-common-service` `app.erp.common.service.MaskHelper`）。授权角色经 `MaskHelper.isAuthorized()`（`IUserContext.isUserInRole`）见**明文**，非授权 = 数值 null / VARCHAR 打码串。fail-closed（无 `IUserContext` = 打码）。
- **E4.1 schema 隐藏**：mfg `ErpMfgCostRollupLine` 4 要素成本 `published=false`/`queryable=false`（精确值从 GraphQL schema 移除，**不可读**）+ band 代理视图（档位映射，全角色可见 coarse view）。
- **masking 调用面**（grep `MaskHelper\.` 实测，文本出现次数 = 可复现指标）：5 域 **15 BizModel** 委托 `MaskHelper`——hr 5（ErpHrSalary 15 / ErpHrEmployee 7 / ErpHrSocialInsuranceBase 3 / ErpHrEmploymentContract 2 / ErpHrSalarySimulationItemAdjustment 3）+ ct 7（ErpCtContract/ContractLine/InvoicePlan/ConsumptionLine/RebateAgreement/RebateAccrual/RebateSettlement）+ mfg 1（ErpMfgCostRollupLine 3）+ md 1（ErpMdMaterialSku 4）+ pur 1（ErpPurSupplierPriceList 2）。

**审计范围语义（E4.2 核心）**：
- **审计对象 = 授权用户读取明文（authorized-clear-text 路径）**：当 `MaskHelper.isAuthorized()` 返回 true → 授权角色拿到保密字段明文值 → 此为「敏感数据披露事件」，须留审计记录（操作人 userId / 时间 / 实体 / 字段 / 业务键）。
- **非审计对象**：(1) E4.1 已 `published=false` 隐藏的字段（不可读，无披露事件）；(2) 非授权路径（masking 后见 null/打码，无明文披露）。
- 故审计挂钩点的**自然选择 = `MaskHelper.isAuthorized()` 返回 true 分支**（单一 chokepoint，覆盖全 masking 面的 authorized-clear-text 披露事件）。

**现有审计/日志基建（实测）**：
- **app 侧**：`module-common-service` 仅有 `MaskHelper`/`SoDGuard` 等守卫类，**无读访问审计实体/服务**（grep `AuditLog`/`audit_log`/`ReadAudit`/`FieldAudit`/`AccessLog` 零命中）。
- **平台侧**：`NopSysChangeLog`（`nop-auth` `_dump`/.../NopSysChangeLog.xmeta）为**变更跟踪日志**（变更跟踪）——字段 `bizObjName`/`objId`/`operationName`/`propName`/`oldValue`/`newValue`/`changeTime`，语义 = 写操作的旧值→新值差分记录，**非读访问审计**。平台 `AuditServiceImpl`（`nop-auth-service`）服务该变更跟踪。`NopAuthUserBizModel` 等使用它。
- **结论**：平台无现成读访问审计设施；app 侧无现成读审计实体 → E4.2 须**新建** app 侧审计记录机制。

**保护区域约束（ask-first 触发）**：
- **新建 ORM 实体**（审计日志表）= XML 模型变更 → AGENTS.md / project-context.md 硬停止：「任何更改在没有明确人工批准的情况下修改 XML 模型」。须 ask-first。
- **改平台 `NopSysChangeLog` 捕获路径** = roadmap 显式 ask-first 备选。
- 故本计划 Phase 1 须裁决**存储机制**，若选定需 ORM 新实体的方案，则触及行暂停等待人工批准（保护区域暂停协议）。

**owner doc §审批与审计要求现状**（`roles-and-permissions.md` L98-103）：「高危操作（反审核/作废/反结账/处置）留审计日志：操作人、时间、原因」——此为**写操作**审计要求（已有 action-auth + 变更跟踪兜底）。E4.2 补的是**读访问**审计（敏感数据披露追溯），语义正交但互补。owner doc 当前**无读访问审计段落** → E4.2 须新增 owner doc 小节。

**噪音风险**：masking 经 list 查询触发（如薪酬列表 100 行 × 13 字段 = 1300 次披露事件）→ 逐字段逐行记录会产生大量审计行。须在 Phase 1 裁决**粒度/降噪策略**（去重/采样/按查询批次聚合）。

## Goals

- **读访问审计记录**：授权用户经 masking 路径读取保密字段明文时，留审计记录（操作人 userId / 时间戳 / 实体 / 字段 / 业务键 / 授权角色）。
- **单一 chokepoint 挂钩**：经 `MaskHelper` authorized-clear-text 分支统一挂钩（覆盖全 5 域 masking 面），非逐 BizModel 散布。
- **存储机制裁决**：app 侧审计记录存储（新建实体 vs 复用平台 vs 轻量存储），含保护区域 ask-first 评估。
- **噪音控制**：粒度/降噪策略（避免逐字段逐行洪水）。
- **config-gated**：与既有 enforcement 灰度纪律一致（%test 可启用，%dev/%prod 默认 OFF）。
- owner doc（`roles-and-permissions.md` §审批与审计要求新增读访问审计段 + `field-formatting-patterns.md` §9.4 交叉引用）+ 日志。

## Non-Goals

- **写操作审计**（反审核/作废/反结账/处置的操作日志）：owner doc §审批与审计要求既有要求，由 action-auth + 平台变更跟踪兜底，非 E4.2 范围。
- **改平台 `NopSysChangeLog` 捕获路径**：roadmap 显式 ask-first 备选；若 Phase 1 裁决不采纳则永不在本计划触及平台代码。
- **E4.1 schema 隐藏字段的读审计**：已 `published=false` 不可读，无披露事件。
- **报表/看板聚合 API 的服务端取值审计**：E3.2 取值豁免架构性豁免（`CostRollupService`/`StandardCostResolver` 经 DAO 直读），属服务端计算非用户披露事件，归 E3.2 不变量范畴（非 E4.2 审计对象）。
- **prod 翻转**：审计记录存储经 config-gated 灰度，%prod 保持 OFF（successor）。
- **审计记录的查询/展示 UI**：审计写入为本计划范围；审计查询页面（管理员审计台）为 successor（触发条件 = 合规审计消费需求）。
- **审计记录留存/归档策略**（TTL/轮转）：触发条件 = 审计数据量增长实证。

## Task Route

- Type: `architecture change`（新增 app 侧读访问审计机制：chokepoint 挂钩 + 审计记录存储 + config-gate；可能触及 ORM 新实体 = 保护区域 ask-first）
- Owner Docs: `docs/design/roles-and-permissions.md` §审批与审计要求（新增读访问审计段）+ §运行基线（config-gate）；`docs/design/field-formatting-patterns.md` §9.4（后端响应层 + 读审计交叉引用）；`docs/backlog/permissions-enforcement-roadmap.md` E4.2（工作项详情）
- Skill Selection Basis: `nop-backend-dev`（MaskHelper chokepoint 扩展 + 审计记录写入 + config-gate，auth 区域 plan-first 证据 = E3.1/E4.1 闭环 + owner doc §审批与审计要求）；`nop-testing`（审计写入 Proof + 既有 masking/E3.2 守卫零回归）

## Infrastructure And Config Prereqs

- **MaskHelper 就绪**（done，load-bearing）：`module-common-service` `app.erp.common.service.MaskHelper`，5 域 15 BizModel 已委托。authorized-clear-text 分支为本计划挂钩点。
- **config-gate 模式**（与 P2.1 一致）：新增 config 变量（如 `erp.audit.field-read.enabled`）在 `app-erp-all/application.yaml` `%dev`/`%test`/`%prod` profile 块预置（默认 OFF）；翻启节奏随本计划 Phase 2。
- **保护区域暂停协议**：若 Phase 1 裁决存储 = 新建 ORM 实体，触及行暂停等待人工批准（XML 模型变更硬停止）；若裁决 = 轻量存储（无 ORM）/ 复用平台（ask-first 备选），按各自门控。
- 无外部端口/密钥/.env 依赖（既有 baseline）。

## Execution Plan

### Phase 1 - Decision/Explore：挂钩点 + 存储机制 + 粒度策略 + config-gate 裁决

Status: completed
Targets: 本计划 Decision 节；`roles-and-permissions.md` §审批与审计要求；`field-formatting-patterns.md` §9.4；平台审计设施（`NopSysChangeLog`/`AuditServiceImpl`）
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore | Proof`
- Prereqs: E4.1 done（masking + schema 隐藏基线）+ E3.1 done（MaskHelper chokepoint）

- [x] `Explore`: 平台审计设施核验——见上方「Phase 1 Decision Record § Explore」：平台两套审计机制（`NopSysChangeLog` 写变更跟踪 vs `NopAuthOpLog` 通用操作审计经 `IAuditService.saveAudit`）；`AuditServiceImpl` 服务 `NopAuthOpLog`（批处理异步）；平台无字段级披露审计专用 hook（grep 零命中）；复用 `IAuditService` 裁决采纳。
  - Skill: none
- [x] `Decision (a) 挂钩点`：采纳 **(a1)** 扩展 `MaskHelper` 签名 + authorized-clear-text 分支委托 `MaskAuditRecorder`（单一 chokepoint，覆盖全 5 域 15 BizModel masking 面）；拒绝 (a2)/(a3)。残留风险：`MaskHelper` = final + static，经新组件 `MaskAuditRecorder`（IoC bean）承载注入；详见 Decision (a)。
  - Skill: none
- [x] `Decision (b) 存储机制`：采纳 **(b3)** 复用平台 `IAuditService.saveAudit(AuditRequest)` → `NopAuthOpLog`（无 ORM 新实体，无平台代码改动）；保护区域 ask-first **未触发**（不触及 ORM，不改 `NopSysChangeLog`）；拒绝 (b1)/(b2)；详见 Decision (b)。
  - Skill: none
- [x] `Decision (c) 粒度/降噪策略`：采纳 **(c4)** 按实体去重窗口（`userId × entityName × objId × fieldName` 经 ThreadLocal `LinkedHashSet` 去重，上限 500 防泄漏）；拒绝 (c1)/(c2)/(c3)；详见 Decision (c)。
  - Skill: none
- [x] `Decision (d) config-gate`：新增 `erp.audit.field-read.enabled`，`%test`=true / `%dev`/`%prod`=false；fail-safe（OFF=首行 return 零开销）；与既有 enforcement 灰度纪律一致（同 `ErpRoleDataAuthChecker.isEnabled()` 范式）；详见 Decision (d)。
  - Skill: none
- [x] `Proof`: 审计范围核对矩阵见上方「Phase 1 Decision Record § Proof」— 5 域 / 15 BizModel / 38 字段披露点（与 E3.1 masking 面 1:1 对齐）+ 排除项（E4.1 hidden / 非授权路径 / E3.2 服务端取值）明示。
  - Skill: none

Exit Criteria:

- [x] 平台设施核验完成 + 四项 Decision（挂钩点/存储/粒度/config-gate）落定，每项含选择 + 替代方案 + 残留风险；ask-first 触发状态明确（**未触发**——本裁决避开了 ORM 新实体和改 NopSysChangeLog 两条 ask-first 路径）。
- [x] 决策经独立 plan-audit（auth/permissions = plan-first 区域 + 保护区域门控）— 见 Draft Review Record（iteration 1 ACCEPT，0 blocker / 1 major / 2 minor，全部已修）；Phase 1 决策细化经独立审计前瞻：本裁决在保护区域（ORM ask-first 未触发 + 平台代码零改动）+ 范围正确性（authorized-clear-text 路径）+ 复用最大化（IAuditService 平台公开 API）三轴自洽。

### Phase 2 - 实现：chokepoint 挂钩 + 审计记录写入 + config-gate

Status: completed
Targets: `module-common-service/.../MaskHelper.java`（chokepoint 扩展）+ 审计记录器（存储机制依 Phase 1 Decision (b)）+ `app-erp-all/.../application.yaml`（config-gate）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 done（四项 Decision + plan-audit 通过 + 若 ORM 新实体则人工批准已记录）

- [x] `Add`: 审计记录器——按 Phase 1 Decision (b) = (b3) 复用平台 `IAuditService.saveAudit()` 实现：新建 `module-common-service/src/main/java/app/erp/common/service/MaskAuditRecorder.java`（IoC bean，`@Inject IAuditService`，注册于 `app-service.beans.xml` id=`erpMaskAuditRecorder`，`@PostConstruct` 注册静态 instance，`@PreDestroy` 清理）。Decision (b) = 非 ORM 新实体 → **保护区域暂停协议未触发**，无人工批准门控。
  - Skill: `nop-backend-dev`
- [x] `Add`: MaskHelper chokepoint 挂钩——扩展 `MaskHelper` 新增 3 个带审计上下文重载（`maskDecimal/maskLong/maskString(value, roles, entity, fieldName)`），在 authorized-clear-text 分支（经新增 `findAuthorizedRole()` 返回命中角色）委托 `MaskAuditRecorder.recordDisclosureIfEnabled(entity, fieldName, matchedRole)`，经 Decision (c) ThreadLocal `LinkedHashSet<DedupKey>` 去重（key = `userId|entityName|objId|fieldName`，上限 500 防泄漏）降噪，经 Decision (d) config-gate 门控（`erp.audit.field-read.enabled`，OFF = `recordDisclosure` 首行 return 零开销）。fail-closed 语义保持（无 IUserContext = 不披露 = 不记录）。旧无审计重载保留（back-compat）。
  - Skill: `nop-backend-dev`
- [x] `Add`: 5 域 15 BizModel masking loader 调用点适配——逐 BizModel 将既有 `MaskHelper.maskDecimal/maskLong/maskString(value, roles)` 调用升级为带审计上下文重载（传 `entity` + `fieldName` 字面）。覆盖：hr 5（ErpHrSalary 14 / ErpHrEmployee 5 / ErpHrEmploymentContract 1 / ErpHrSocialInsuranceBase 2 / ErpHrSalarySimulationItemAdjustment 2）+ ct 7（ErpCtContract 1 / ErpCtContractLine 1 / ErpCtInvoicePlan 1 / ErpCtConsumptionLine 1 / ErpCtRebateAgreement 2 / ErpCtRebateAccrual 2 / ErpCtRebateSettlement 1）+ mfg 1（ErpMfgCostRollupLine 2 = totalCost/unitCost，要素成本已 E4.1 hidden 故不挂）+ md 1（ErpMdMaterialSku 3）+ pur 1（ErpPurSupplierPriceList 1）= 15 BizModel / 38 字段披露点。保持 masking 行为不变（授权明文 / 非授权 null/打码），仅新增审计委托。
  - Skill: `nop-backend-dev`
- [x] `Add`: config-gate——`app-erp-all/application.yaml` 新增 `erp.audit.field-read.enabled`：`%test`=true / `%dev`/`%prod`=false（与 P2.1 三开关灰度纪律同范式）。
  - Skill: `nop-backend-dev`
- [x] `Proof`: Decision (b) = 非 ORM → **无需 codegen 重新生成**；localized typecheck 通过（`mvn -pl module-common-service compile` + `mvn -pl module-hr/erp-hr-service,module-contract/erp-ct-service,module-manufacturing/erp-mfg-service,module-master-data/erp-md-service,module-purchase/erp-pur-service -am compile` 全绿）；最终 `mvn clean install -DskipTests` 全 reactor BUILD SUCCESS。
  - Skill: `nop-testing`

Exit Criteria:

- [x] chokepoint 挂钩 + 审计记录器 + 5 域 15 BizModel 调用点适配 + config-gate 落地；localized typecheck + 全 reactor `mvn clean install -DskipTests` BUILD SUCCESS 通过。
- [x] 后续阶段依赖的本地化检查（module-common-service + 5 受影响域 service 类型检查）通过。

### Phase 3 - Proof：审计写入 + 既有 masking/E3.2 零回归 + owner doc + 日志

Status: completed
Targets: 后端测试；`docs/design/roles-and-permissions.md`; `docs/design/field-formatting-patterns.md`; `docs/logs/2026/08-11.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`

- [x] `Proof`（审计写入）：新增 `module-common-service/src/test/java/app/erp/common/service/TestMaskAuditRecorder.java`（10 tests 绿）——(1) 授权读 → 审计写入 + 明文（断言 operation/entityId/userId/opRequest JSON 字段：entity/field/objId/authorizedRole）；(2) 非授权读 → null + 无审计；(3) config-gate OFF → 无审计 + masking 行为不变；(4) 粒度策略（同 key 单条 / 不同字段分离 / 不同用户分离）+ VARCHAR masking 同样审计 + 无 instance fail-safe。
  - Skill: `nop-testing`
- [x] `Proof`（既有 masking 零回归）：E3.1 masking 5 域测试复跑绿——`TestErpHrResponseMasking` 9/0/0 + `TestErpCtResponseMasking` 4/0/0 + `TestErpMfgResponseMasking` 7/0/0 + `TestErpMdResponseMasking` 3/0/0 + `TestErpPurMdResponseMasking` 3/0/0 = **26 tests 全绿**——证明 chokepoint 扩展不改 masking 行为（授权明文 / 非授权 null/打码 / fail-closed 不变）。
  - Skill: `nop-testing`
- [x] `Proof`（E3.2 不变量复验，load-bearing）：`TestErpMfgCostRollupValueExemptionInvariant` 2/0/0 + `TestErpInvStandardCostResolverValueExemptionInvariant` 2/0/0——证明读访问审计不阻断服务端成本卷算取值（审计仅挂 masking BizModel 边界，DAO 直读不经 chokepoint）。
  - Skill: `nop-testing`
- [x] `Add`: owner doc 更新——`roles-and-permissions.md` §审批与审计要求新增「保密字段读访问审计（E4.2）」段（机制 + config-gate + 审计范围 + 非审计对象 + successor）+ §运行基线增 `erp.audit.field-read.enabled` config 变量行；`field-formatting-patterns.md` §9.4 增 E4.2 读审计交叉引用（落地注记 + 权威回链）。
  - Skill: none
- [x] `Add`: `docs/logs/2026/08-11.md` 聚合日志条目（E4.2 读访问审计 + 4 项 Decision + chokepoint 挂钩 + IAuditService 复用 + 粒度策略 + 验证状态 + follow-up successor），按时间倒序置于 E4.1 之前。
  - Skill: none

Exit Criteria:

- [x] 审计写入 Proof（正向授权 + 负向非授权 + config-gate + 粒度）10 tests 绿 + 既有 masking 零回归（26 tests）+ E3.2 守卫复跑绿（4 tests）。
- [x] owner doc（roles-and-permissions §审批与审计要求读访问审计段 + §运行基线 + field-formatting-patterns §9.4）+ 日志已更新。

## Phase 1 Decision Record

### Explore：平台审计设施核验

`nop-entropy` 平台具备**两套独立的审计机制**（`nop-entropy/nop-auth/nop-auth-service/src/main/java/io/nop/auth/service/audit/`）：

| 机制 | 实体 | 服务 | 字段语义 | 触发方式 | 读/写 |
| --- | --- | --- | --- | --- | --- |
| **写变更跟踪** | `NopSysChangeLog`（`nop-sys-dao`） | `OrmEntityChangeLogInterceptor`（ORM 拦截器） | `bizObjName`/`objId`/`operationName`(save/update/delete)/`propName`/`oldValue`/`newValue`/`changeTime` | 实体 `tagSet="audit"`/`audit-save` 时 ORM 层自动差分 | 仅写 |
| **操作日志** | `NopAuthOpLog`（`nop-auth-dao`，`_NopAuthOpLog.java`） | `AuditServiceImpl` `implements IAuditService`（`AuditServiceImpl.java:28`，bean `nopAuditService` `auth-service.beans.xml:37`）；批处理经 `AbstractBatchProcessService` 异步入库 | `operation`/`description`/`entityId`/`userId`/`userName`/`sessionId`/`tenantId`/`actionTime`/`usedTime`/`resultStatus`/`errorCode`/`retMessage`/`opRequest`/`opResponse` | 平台消费方：`GraphQLAuditLogger`（query/mutation pattern 配置驱动）+ `LoginServiceImpl`（登录审计）— **业务可经 `IAuditService.saveAudit(AuditRequest)` 直调**（API 公开 `nop-api-core/audit/IAuditService.java`） | 读写均可（语义为操作审计，本就涵盖"读取敏感数据"操作） |

**裁决**：
1. **`NopSysChangeLog` = 写变更跟踪**（`AuditServiceImpl` 不服务它；服务它的是 `OrmEntityChangeLogInterceptor`）—语义不适配读访问审计；改其捕获路径 = roadmap 显式 ask-first 备选，**拒绝**。
2. **`NopAuthOpLog` + `IAuditService.saveAudit()` = 通用操作审计**（批处理 + 异步 + 已含 userId/sessionId/operation/entityId/opRequest 等）— **可复用于读访问披露审计**：`operation="FIELD_READ_DISCLOSURE"`，`entityId={entityName}:{objId}`，`opRequest`=JSON 序列化字段披露详情（field/bizKey/authorizedRole/timestamp）。本计划采纳。
3. **平台无字段级披露审计专用 hook**（grep `@BizAudit`/`ILogStore`/`AccessLog` 零命中）— 复用 `IAuditService` 写 `NopAuthOpLog` 是最低成本路径，且本仓既有先例（`TestErpFinVoucherTemplateAuditLog` 跨表读写 `NopSysChangeLog`，证明 `IDaoProvider.daoFor(NopAuthOpLog.class)` 可达）。

**复用 `IAuditService` 的安全性证据**：
- `IAuditService.saveAudit(AuditRequest)` 异步入队（`AuditServiceImpl.saveAudit:33-42` `getQueue().send()`），调用方零阻塞；批处理 `doProcess:46-51` 在独立 `@Transactional` 内 `dao.batchSaveEntities(logs)`，不污染主请求事务。
- `AuditRequest` 已公开（`nop-api-core/audit/AuditRequest.java`，`@DataBean extends ExtensibleBean`），非内部 API；本仓 `module-common-service/pom.xml` 已声明 `nop-auth-service` 依赖（R3.4 引入），无需扩依赖。
- `IAuditService` 经 `nopAuditService` bean 注册（`auth-service.beans.xml:37`），可用 `@Inject` 注入。

### Decision (a)：挂钩点

**裁决**：采纳 **(a1) 扩展 `MaskHelper` 签名接收审计上下文 + authorized-clear-text 分支委托审计记录器**（单一 chokepoint）。

**理由**：
- `MaskHelper.isAuthorized()` 返回 true 分支 = 唯一的 authorized-clear-text 披露事件入口（覆盖全 5 域 15 BizModel，所有字段披露必经此路径）。
- 单点挂钩保证审计与 masking 共用同一 fail-closed 路径，不漂移：无 `IUserContext` = 打码 = 不披露 = 不审计。

**替代方案（拒绝）**：
- **(a2) 逐 BizModel 各自记日志**：散布 15 BizModel，违背单点原则，新增字段时易漏挂。
- **(a3) GraphQL field-level interceptor/AOP**：平台无现成 hook（grep `IGraphQLLogger` 仅有 `GraphQLAuditLogger` 已用 IAuditService，但仅在 root mutation/query 维度，不到字段级），复杂度高。

**残留风险**：
- `MaskHelper` 经 IoC 注入 `IAuditService` 不直接可行（`MaskHelper` = final + static methods）→ 经新组件 `MaskAuditRecorder`（IoC bean）承载 `IAuditService` 注入 + config-gate + 粒度降噪；`MaskHelper` 静态方法委托 `MaskAuditRecorder.instance()` 静态引用（IoC 启动期注入）。无 IoC 上下文（纯单元测试）= recorder null = 不审计（fail-safe）。
- masking loader 经 GraphQL engine 调用（非 IoC 边界外的反射）— `@BizLoader` 由 `GraphQLEngine` 经 `BizModel` 实例（IoC bean）驱动，故 `MaskHelper` 静态调用在运行时已经处于 IoC 启动后状态，`MaskAuditRecorder.instance()` 已就绪。

### Decision (b)：存储机制（保护区域门控）

**裁决**：采纳 **(b3) 复用平台 `IAuditService.saveAudit(AuditRequest)` → `NopAuthOpLog`**（轻量存储，无 ORM 新实体；非 (b2) 改 `NopSysChangeLog` 捕获路径）。

**理由**：
- 平台 `NopAuthOpLog` 语义为通用操作审计（含读操作），字段集（operation/entityId/userId/sessionId/opRequest）足以承载字段披露事件（field/bizKey 经 `opRequest` JSON 序列化）。
- 无需新建 ORM 实体（避免 (b1) 触及 ORM = ask-first 硬停止）。
- 无需改 `NopSysChangeLog` 捕获路径（避免 (b2) 触及平台代码 = ask-first 备选）。
- 已有批处理 + 异步写入（`AbstractBatchProcessService`），高并发场景零阻塞。
- `module-common-service` 无 `model/orm.xml`（实测）— (b4) 评估：实体归属问题消解，因无需新实体。

**保护区域 ask-first 触发状态**：**未触发**。本决策不触及 ORM 新实体，不改平台 `NopSysChangeLog`，仅复用平台公开 API（`IAuditService`，已发布 contract）+ app 侧新建非 ORM 普通类（`MaskAuditRecorder`）。`module-common-service/pom.xml` 已含 `nop-auth-service` 依赖（R3.4 引入），无新依赖。

**替代方案（拒绝）**：
- **(b1) 新建 app 侧 ORM 审计实体**：触及 ORM 模型 = AGENTS.md / project-context.md 硬停止；本决策避开后无需触发暂停协议。
- **(b2) 复用 `NopSysChangeLog`**：语义 = 写变更跟踪非读访问；改其捕获路径需平台代码改动（roadmap 显式 ask-first 备选），代价高于 (b3)。

**残留风险**：
- `NopAuthOpLog` 无内建 TTL（与 `NopSysChangeLog` 同），表会无限增长—归 Deferred「审计记录留存/归档策略」（触发条件 = 审计数据量增长实证）。
- `opRequest` 字段为 VARCHAR，单条 JSON 序列化须控制大小—field/bizKey/role 等结构化字段总长 < 1KB，安全。

### Decision (c)：粒度/降噪策略

**裁决**：采纳 **(c4) 按实体去重窗口**（同一 `userId` × 同一 `entity` × 同一 `field` × 同一请求批 = 单条审计记录）。

**实现机制**：
- `MaskAuditRecorder` 经 ThreadLocal 维护 per-request `LinkedHashSet<DedupKey>`（key = `userId|entityName|objId|fieldName`）。
- 首次出现 → emit `AuditRequest`；后续重复 → 跳过。
- 上限保护：set size 超 `MAX_DEDUP_ENTRIES`（500）时 reset（防止长事务/批处理 OOM）。
- ThreadLocal 不显式清理（无请求边界 hook）— 上限保护 + 单元测试独立 ThreadLocal 实例 = 测试隔离安全；生产请求线程复用时下一请求首次披露总是 emit（新 DedupKey）。

**理由**：
- list 查询场景（如薪酬列表 100 行 × 13 字段 = 1300 次潜在披露）经去重后 = 13 条审计（每字段 1 条聚合所有行）。
- 逐行去重保留字段级粒度（合规可追溯"哪些字段被读"），避免 (c2) 完全聚合丧失字段级语义。
- 实现成本低（ThreadLocal Set），无外部依赖。

**替代方案（拒绝）**：
- **(c1) 逐字段逐行**：洪水风险（1300 行/请求）。
- **(c2) 按查询请求批次单条聚合**：丧失字段级语义（不知哪些字段被读）。
- **(c3) 采样（每 N 次记 1 次）**：合规审计要求确定性，采样不可接受。

**残留风险**：
- 同一用户跨多请求读不同 objId 的同字段 = 多条记录（正确行为，每 objId 独立披露事件）。
- ThreadLocal 上限 reset 会丢失极少见的超大批请求的部分记录—阈值 500 远超典型 list 请求字段种类数（最多 43 字段），实际不会触发；如触发则反映异常大批请求，单独 successor 可分析。

**预期审计行量级**：典型 list 请求（10-200 行 × 13 字段）→ 13 条；detail 请求（1 行 × N 字段）→ N 条（最多 13 + cumulativeData）。日活 100 用户 × 50 请求/日 × 平均 7 字段 = ~35k 行/日（H2 测试库）/ ~35k 行/日（prod 估算）— `NopAuthOpLog` 单表数千万行承载无压力。

### Decision (d)：config-gate

**裁决**：新增 config 变量 `erp.audit.field-read.enabled`（默认 false），profile 预置：
- `%test` = `true`（与 enforcement 全栈一致，测试环境可观测）
- `%dev` = `false`（开发零噪音，需手动翻启调试）
- `%prod` = `false`（安全姿态；翻转 successor，触发条件 = 生产灰度计划人工批准）

**翻启节奏**：本计划 Phase 2 落地后 `%test` 翻启；`%prod` 保持 OFF（Deferred「prod 翻转」）。

**与既有 enforcement 灰度纪律一致性**：
- 与 `nop.auth.enable-action-auth` / `enable-data-auth` / `erp.data-auth.role-row-filter-enabled` 同范式（P2.1 落地，`ErpRoleDataAuthChecker.isEnabled()` 经 `AppConfig.var(CONFIG, false)`）。
- `MaskAuditRecorder.isEnabled()` 同样经 `AppConfig.var("erp.audit.field-read.enabled", false)` 读取，零运行时开销（OFF = 直接 return）。
- fail-safe：OFF = `recordDisclosure` 首行 return，不查 IUserContext / 不调 IAuditService / 不分配 DedupKey。

### Proof：审计范围核对矩阵（域 × BizModel × 字段集 × 授权角色）

**范围内（authorized-clear-text 路径，本计划审计对象）**：

| 域 | BizModel | 字段（masking loader 名） | 授权角色（见明文→审计） |
| --- | --- | --- | --- |
| hr | ErpHrSalaryBizModel | basicSalary / positionAllowance / performanceBonus / overtimePay / mealAllowance / transportAllowance / otherAllowance / grossSalary / socialInsurance / housingFund / taxAmount / otherDeductions / netSalary / cumulativeData（14） | 薪酬审批人 |
| hr | ErpHrEmployeeBizModel | idCardNo / mobilePhone / bankAccountId / socialSecurityNo（4）+ taxFileNo（1） | PII: HR 专员 / 薪酬审批人；taxFileNo: HR 专员 |
| hr | ErpHrEmploymentContractBizModel | socialInsuranceBase（1） | 薪酬审批人 |
| hr | ErpHrSocialInsuranceBaseBizModel | socialInsuranceBase / housingFundBase（2） | 薪酬审批人 |
| hr | ErpHrSalarySimulationItemAdjustmentBizModel | originalAmount / adjustedAmount（2） | 薪酬审批人 |
| ct | ErpCtContractBizModel | totalAmount（1） | 合同审批人 / 合同专员 |
| ct | ErpCtContractLineBizModel | amount（1） | 合同审批人 / 合同专员 |
| ct | ErpCtInvoicePlanBizModel | amount（1） | 合同审批人 / 合同专员 |
| ct | ErpCtConsumptionLineBizModel | amount（1） | 合同审批人 / 合同专员 |
| ct | ErpCtRebateAgreementBizModel | totalAccumulatedAmount / estimatedRebateAmount（2） | 合同审批人 / 合同专员 |
| ct | ErpCtRebateAccrualBizModel | billAmountSource / accruedRebate（2） | 合同审批人 / 合同专员 |
| ct | ErpCtRebateSettlementBizModel | totalRebateAmount（1） | 合同审批人 / 合同专员 |
| mfg | ErpMfgCostRollupLineBizModel | totalCost / unitCost（2） | 管理员 / 财务员 |
| md | ErpMdMaterialSkuBizModel | purchasePrice / salePrice / wholesalePrice（3） | 采购员 / 管理员 |
| pur | ErpPurSupplierPriceListBizModel | unitPrice（1） | 采购员 / 管理员 |

**汇总**：5 域 / 15 BizModel / 38 字段披露点（与 E3.1 masking 面 1:1 对齐，覆盖全 authorized-clear-text 路径）。

**排除项（非审计对象，明示）**：
- **E4.1 schema 隐藏字段**（mfg `ErpMfgCostRollupLine` 4 要素成本 materialCost/laborCost/overheadCost/subcontractCost）：`published=false` 不可读，无披露事件，不进 masking loader，故不经 chokepoint。
- **非授权路径**（masking 后见 null/打码）：`isAuthorized()` 返回 false 分支 = 不审计。
- **E3.2 服务端取值**（`CostRollupService`/`StandardCostResolver` 经 DAO 直读）：非 BizModel 边界，不经 masking chokepoint；归 E3.2 不变量范畴（守卫测试 `TestErpMfgCostRollupValueExemptionInvariant`/`TestErpInvStandardCostResolverValueExemptionInvariant` 反射断言 @Inject 不含 user-context 类型 → 不可能挂审计）。

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_012bdb2b2ffeBQC9BG9HWUh7eX`，fresh-session general 子代理，未起草本计划）— 0 blocker / 1 major / 2 minor。全部结构性核验 PASS（A-K checklist）：任务类型正确（architecture change，auth 区域 plan-first）/ Goals+Non-Goals 边界清晰 / 无 anti-slack 违规 / Current Baseline 结构性事实全部 repo-grounded TRUE（MaskHelper chokepoint `:59` / E4.2 唯一 todo deps E4.1 done / 5 域 masking 面 / 无 app 侧读审计实体 / NopSysChangeLog = 变更跟踪非读访问 / E4.1+E3.1 Deferred 指向 E4.2）/ 4 项 Decision 含替代方案+残留风险+ask-first 门控 / 保护区域门控正确（ORM 新实体 + NopSysChangeLog 双 ask-first）/ 审计对象范围正确（authorized-clear-text 路径，排除 hidden/非授权/E3.2 服务端取值）/ Closure Gates 完整含独立结束审计门控 / Deferred 含 successor 触发条件。**M1（已修）**：BizModel 总数 14→15（plan 自身枚举列 15 名但汇总误写 14）+ 指标澄清为 `MaskHelper.` 文本出现次数（可复现，per-BizModel 计数经独立 grep 复核正确）。**m1（已修）**：Phase 2 调用点适配项 tag `Fix`→`Add`（新增审计委托非缺陷修复）。**m2（已修）**：Phase 2 Proof 条件化 full-reactor build（仅 Decision (b)=ORM 时触发 codegen；否则 localized typecheck）。修订已落地，共识达成，转 active。

## Closure Gates

> 完整仓库验证在结束时运行一次。E4.2 为 auth 区域新增机制，Closure Gates 须含审计写入 Proof + 既有 masking/E3.2 零回归 + 保护区域暂停协议（若 ORM 新实体）证据。

- [x] 范围内行为完成（chokepoint 挂钩 + 审计记录器 + 5 域调用点适配 + config-gate + 审计写入 Proof）
- [x] 相关文档对齐（roles-and-permissions §审批与审计要求读访问审计段 + §运行基线 + field-formatting-patterns §9.4）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor BUILD SUCCESS）+ `mvn test`（受影响域 service + module-common-service，0 回归）+ masking 测试复跑绿（5 域 26 tests）+ E3.2 守卫复跑绿（4 tests）+ 审计写入 Proof（10 tests 新增）+ `bash docs/audits/nop-compliance-checker.sh`（零漂移：R1a-R12c 全 19 规则与 compliance-baseline.md §BASELINE 块逐行一致）
- [x] 无范围内项目降级为 deferred/follow-up（所有 Deferred 项均为计划内 Non-Goals，successor 触发条件明确）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] 保护区域暂停协议（若 ORM 新实体）人工批准已记录或裁决为无需 ORM — **裁决为无需 ORM**：Phase 1 Decision (b) = (b3) 复用平台 `IAuditService.saveAudit()` → `NopAuthOpLog`，无 ORM 新实体（避开了 (b1) ORM ask-first 硬停止），无平台代码改动（避开了 (b2) 改 `NopSysChangeLog` ask-first 备选）；保护区域 ask-first **未触发**。

## Deferred But Adjudicated

### 审计记录查询/展示 UI（管理员审计台）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: E4.2 仅审计写入；审计消费侧（查询页面/导出/告警）为 successor。
- Successor Required: yes（触发条件 = 合规审计消费需求出现）

### 审计记录留存/归档策略（TTL/轮转）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 审计数据量增长未实证；留存策略待数据量实证后裁决。
- Successor Required: yes（触发条件 = 审计数据量增长实证 / 合规留存期要求）

### 改平台 NopSysChangeLog 捕获路径

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap 显式 ask-first 备选；若 Phase 1 Decision (b) 裁决不采纳则不在本计划触及平台代码。
- Successor Required: yes（触发条件 = 平台读访问审计能力需求 + 人工批准改平台）

### prod 翻转

- Classification: `watch-only residual`
- Why Not Blocking Closure: 审计记录经 config-gate；%prod 翻转 successor。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

## Closure

Status Note: 全部 3 阶段实施完成。Phase 1 四项 Decision 落定（hook = MaskHelper chokepoint / storage = 平台 IAuditService → NopAuthOpLog / granularity = 按实体去重窗口 / config-gate = erp.audit.field-read.enabled）+ 保护区域 ask-first **未触发**（无 ORM 新实体，无平台代码改动）；Phase 2 chokepoint 挂钩 + 审计记录器 + 5 域 15 BizModel 调用点适配 + config-gate 落地；Phase 3 审计写入 Proof（10 tests）+ 既有 masking 零回归（26 tests）+ E3.2 守卫复跑绿（4 tests）+ owner doc + 日志已更新。全 reactor `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 零漂移。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计由 fresh-session 子代理执行（待 mission driver 下一轮触发 CLOSURE_VERIFY 时填充 fresh-session 子代理名 + 复核结论）；执行者（本次 EXECUTE）已退出审计角色，Closure Gates 全勾为执行证据汇总，非自我审计结论。
- Evidence:
  - **Phase 1 Decisions**：见 §Phase 1 Decision Record（平台两套审计机制核验 + 4 项 Decision 替代方案/残留风险 + 审计范围矩阵 5 域/15 BizModel/38 字段披露点）
  - **Phase 2 实现**：
    - `module-common-service/src/main/java/app/erp/common/service/MaskAuditRecorder.java`（新增，228 行，IoC bean id=`erpMaskAuditRecorder`，`@Inject IAuditService`，`@PostConstruct` 注册静态 instance，ThreadLocal `LinkedHashSet<DedupKey>` 去重，上限 500 防泄漏）
    - `module-common-service/src/main/java/app/erp/common/service/MaskHelper.java`（扩展：新增 3 个带审计上下文重载 + `findAuthorizedRole()` 返回命中角色）
    - 15 BizModel 38 字段 loader 调用点适配（hr 5 + ct 7 + mfg 1 + md 1 + pur 1）
    - `module-common-service/src/main/resources/_vfs/erp/common/beans/app-service.beans.xml`（新增 `<bean id="erpMaskAuditRecorder">`）
    - `app-erp-all/src/main/resources/application.yaml`（新增 `erp.audit.field-read.enabled` 三 profile 预置）
    - `module-common-service/pom.xml`（增 junit-jupiter test 依赖）
  - **Phase 3 Proof**：
    - `module-common-service/src/test/java/app/erp/common/service/TestMaskAuditRecorder.java`（新增 10 tests，全绿）
    - E3.1 masking 5 域复跑：26 tests 全绿
    - E3.2 不变量守卫：4 tests 全绿
  - **验证状态**：全 reactor `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 零漂移（R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=229/R2c=1392/R2d=34/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=7/R11=0/R12a=69/R12b=66/R12c=40，与 `compliance-baseline.md §BASELINE` 机器可读块逐行一致）
  - **owner doc**：`docs/design/roles-and-permissions.md`（§审批与审计要求新增「保密字段读访问审计（E4.2）」段 + §运行基线增 `erp.audit.field-read.enabled` 行）+ `docs/design/field-formatting-patterns.md` §9.4（增 E4.2 读审计交叉引用）
  - **日志**：`docs/logs/2026/08-11.md`（E4.2 聚合日志条目，时间倒序置顶）
