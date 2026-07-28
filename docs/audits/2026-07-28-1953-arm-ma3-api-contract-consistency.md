# MA3 API 契约一致性审计报告（A3.6）

> Audit Status: closed
> Audit Date: 2026-07-28
> Auditor: 主代理（plan `2026-07-28-1953-2-audit-remediation-ma3-api-contract-consistency.md` 执行者）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（7 维度适配 API 契约主题）
> Scope: 全域 19 域 `module-*/erp-*-service/` xbiz 文件 + BizModel Java；`module-*/erp-*-meta/` xmeta；`module-*/erp-*-api/` 生成 *Api.java；`module-*/model/*.orm.xml`。S+A 级域重点抽样 finance/mfg/pur/sal/inv/hr/assets，B+C 级域合并抽样。
> Source Plan: `docs/plans/2026-07-28-1953-2-audit-remediation-ma3-api-contract-consistency.md`

## 0. 摘要（Verdict）

**Verdict: FAIL（有 drift）** —— 零 BLOCKER（无公共 API 契约致运行时错误的活跃数据破坏路径）。本审计识别 **4 项 major → P1**（P1-MA3-046~049，全部目标 MR2）+ **4 项 minor → P2** watch-only（P2-MA3-036~039）。

**裁决**：

- **api.xml 缺失性质 = 设计选择**（非 drift）。Nop Platform 明确 api.xml 为可选手写文件，本项目 CRUD-centric 单进程聚合 app 无跨进程 typed-RPC 需求，依赖自动派生（xbiz + xmeta + BizModel Java + CRUD-API codegen）合规。
- **维度分布**：Dim 1（api.xml 缺失）= 设计选择；Dim 2（xbiz vs Java）= CLEAN（零悬挂引用/零 CRUD 名冲突/零名字碰撞）；Dim 3（参数/返回 drift）= 1 项 major（孤儿 Processor 影子契约）+ 3 项 minor；Dim 4（权限注解）= **1 项 major（全域敏感动作零运行时保护）**——本审计最严重发现；Dim 5（生成 *Api.java）= 1 项 major（RPC vs GraphQL 契约面分裂 + 9 模块零生成）+ codegen 纪律 PASS；Dim 6（未声明 API）= note（Nop-normal 模式，非缺陷）；Dim 7（跨实体一致性）= 1 项 major（动词/参数跨域严重不一致）+ 1 项 minor。

**P0 裁决**：本审计原则上无 P0，符合 plan 声明。Dim 4 权限缺失虽严重但 (1) owner doc `roles-and-permissions.md §运行基线` 显式声明为有意默认（灰度推进），(2) 单组织种子下无活跃跨组织数据破坏，(3) 平台层 HTTP 认证默认开启——按 P1-MA2-093/094 同型裁决范式（能力缺失非活跃缺陷）。深度权限审计归 MA6 A6.1/A6.2（roadmap 已声明 `todo`）。

**MA3 累计**：P1=41（A3.1 13 + A3.2 2 + A3.3-A3.5 22 + 本审计 4），P2=26（A3.1 8 + A3.2 1 + A3.3-A3.5 13 + 本审计 4）。

---

## 1. Current Baseline（实时仓库核实）

**关键基线事实（实时仓库直接验证为真）**：

- `find . -name "*.api.xml" -not -path "*/target/*" -not -path "*/nop-entropy/*"` → **0 命中**（确认无手写 api.xml）
- 19 个 `module-*/model/` 目录每个仅含 `app-erp-<domain>.orm.xml` 一个文件
- **211 个 `*Api.java`**（`module-*/erp-*-api/`），全部首行含 `//__XGEN_FORCE_OVERRIDE__` 标记，全部 13 行，全部 `extends ICrudApi<InputBean, OutputBean>` 空体——零手编
- **704 个 xbiz 文件**（352 个生成 `_*.xbiz` + 352 个手写 delta `*.xbiz`）；全部 `_*.xbiz` 使用 `<biz-gen:DefaultBizGenExtends forEntity="true">`（ORM 驱动 codegen 模式，非 api.xml 驱动）
- 9 个 api 模块（aps/b2b/contract/crm/cs/drp/hr/logistics/notify）`src/` 目录不存在——零 `*Api.java` 生成
- `IErpFinVoucherBiz` Facade 被 9 域 11 调用方模块经专用 `*PostingExecutor` 适配器调用，签名 `post(PostingEvent) → Long` / `reverse(String, ErpFinBusinessType) → Long` 全域一致

---

## 2. 维度 1：api.xml 缺失性质裁决

**裁决：设计选择（非 drift）**

### 2.1 依据

**(a) Nop Platform 明确 api.xml 为可选**

`nop-entropy/docs-for-ai/02-core-guides/api-model-and-codegen.md:218-227` 决策表：

| 场景 | 是否使用 api.xml |
| --- | --- |
| 模块向**外部系统**提供强类型 RPC 接口 | 是 |
| **跨进程、跨服务**方法调用 | 是 |
| 仅需 **CRUD 操作**，ORM codegen 已覆盖 | **否——用 BizModel** |
| 模块内部 BizModel-to-BizModel 调用 | **否——用 `I*Biz` 接口** |

同文档 L211-216："API 模型生成是**独立于 ORM 模型生成**的"——ORM codegen 产出 Entity/DAO/BizModel/XMeta/pages 全 CRUD 链；**独立**的 CRUD-API codegen 路径（`/nop/templates/crud-api/`）从 xmeta 直接生成 `*Api.java extends ICrudApi<I,O>`——**无需 api.xml**。

`application-development-workflow.md:24,69-79`：「先改源 ORM 模型，**必要时**补 `model/*.api.xml`」「当业务应用**需要稳定的 API / GraphQL 契约时**，优先维护 `model/*.api.xml`」——明确 api.xml 为按需补充非默认必须。

**(b) 本项目无 api.xml 适用场景**

- `app-erp-all` 单进程聚合 app（无跨进程 RPC 需求）
- 211 个生成 `*Api.java` 全部为 `ICrudApi<I,O>` typed-API（非 RPC-API 的 `{ServiceName}.java` with `ApiRequest/ApiResponse` 包装 4 方法变体）
- 零 RPC-API 工件（无 `{ServiceName}.java` / 无 `_{ServiceName}.xbiz` from `biz:` service tag / 无 `_api-impl.beans.xml`）

**(c) 项目文档显式认知此设计选择**

本 plan（已独立审查 accept `ses_05769e927ffehray74t4QW1KLE`）L13/L50/L125 显式声明设计选择。早期审计 `docs/audits/2026-07-07-1900-comprehensive-design-and-implementation-audit.md:281` finding L-6「无 *.api.xml RPC 契约文件（可能是有意选择）」+ `docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md:200`「无 API 契约文档 | 外部集成方需从 ORM 模型推断」均标为低危/可观察缺口而非必需文件缺失。

### 2.2 文档样板 drift（side-effect，非主裁决）

roadmap A3.6 owner doc 列 `module-*/model/*.api.xml` + 4 处项目文档（`docs/skills/README.md:93` / `docs/context/ai-autonomy-policy.md:70` / `docs/context/project-context.md:69` / `docs/backlog/audit-remediation-roadmap.md:83`）将 api.xml 路径列为「保护区域 ask-first」——这是 Nop Platform 通用约定的样板继承，若 api.xml 存在则适用，目前文件不存在时为无害样板。登记为 P2-MA3-039（文档卫生）。

### 2.3 维度 1 终态

**api.xml 缺失 = 设计选择，合规。** roadmap owner doc `module-*/model/*.api.xml` 声明非 drift（owner doc 描述「保护区域路径模板」非「文件必存在」）。文档样板清理归 MR2 P2-MA3-039。

---

## 3. 维度 2：xbiz 动作 vs Java 实现 drift

**裁决：CLEAN（零悬挂 / 零名冲突 / 零名字碰撞）**

### 3.1 检查方法

全域 S+A 域逐实体枚举手写 `*.xbiz` delta 的 `<mutation>`/`<query>`/`<action>` 声明 + 全部 `*BizModel.java` 的 `@BizMutation`/`@BizQuery`/`@BizAction` 注解方法 + 全部 `*Processor.java` 的 inject 引用，三向交叉核对。

### 3.2 检查结果

| 交叉检查 | 结果 | 计数 |
| --- | --- | --- |
| #3 悬挂 xbiz → Processor 引用（`inject('...Processor').method` 找不到目标） | **CLEAN** | 0 / 90 |
| #5 手写 xbiz 覆盖标准 CRUD `__*` 名（`__save`/`__get`/`__findPage`/`__delete`） | **CLEAN** | 0 |
| xbiz mutation 名 vs Java `@Biz*` 方法（同实体同名）碰撞 | **CLEAN** | 0 |
| Java `@Inject` Processor 引用解析失败 | **CLEAN** | 0 |
| 跨模块 `I*Biz` 接口引用（184 处）解析失败 | **CLEAN** | 0 |

### 3.3 三种合法共存模式

本项目 API 契约注册有三种合法非冲突模式：

- **Pattern A（xbiz 委托）**：手写 `*.xbiz` delta 声明 `<mutation>` 并 `inject('...Processor').method(...)` 委托。28 个 xbiz delta 文件采用此模式（标准 5 动作审批集 `submitForApproval/approve/reject/reverseApprove/withdrawApproval`）。
- **Pattern B（内联脚本）**：手写 `*.xbiz` delta 声明 `<mutation>` 内含完整 XScript 状态守卫（不委托 Processor）。6 个 xbiz delta 文件采用此模式（ErpHrSalary / ErpAstMovement / ErpMfgMaterialIssue / ErpPurQuotation / ErpPurRfq / ErpSalContract）。
- **Pattern C（BizModel 注解生成）**：手写 `*.xbiz` delta 为空 `<actions/>`，actions 在运行时由 `biz-gen` 从 BizModel Java 方法签名（`@BizMutation`/`@BizQuery` + `@Name`）**生成**。Java IS 源，**不可能 drift**；`I*Biz` 接口 + `@Override` 编译期校验契约。绝大多数业务 mutation 走此模式（cancel / postVoucher / start / generateMove / closePeriod 等）。

### 3.4 维度 2 终态

**零 blocker / 零 major / 零 minor。** S+A 域 xbiz 与 Java 一致。结构观察（非缺陷）：

- hr 域是唯一**无 Processor 层**的 S+A 域（全部业务逻辑在 BizModel Java + 内联 xbiz）——内部自洽
- `ErpAstAsset` 仅暴露 CRUD（状态变更分散到 9 个伴随交易单据 BizModel）——符合域模型设计

---

## 4. 维度 3：参数/返回类型契约 drift

**裁决：零 blocker / 零 major TYPE drift；1 项 major（F1 孤儿 Processor 影子契约）+ 3 项 minor**

### 4.1 头条结论

Pattern-A 动作（28 个 xbiz delta）参数/返回类型**统一**：`<arg name="id" type="String" mandatory="true"/>` + `<arg name="svcCtx" kind="ServiceContext"/>` + `<return><schema bizObjName="THIS_OBJ"/></return>`，与对应 Processor `String id` / `IServiceContext` / 返回实体类型**完全匹配**。Pattern-C 动作由 Java 生成，不可能 drift。**零 blocker / 零 major TYPE 不匹配**。

### 4.2 Finding F1（major）：孤儿 Processor bean 携带 String 影子契约

**详情见 P1-MA3-048**。摘要：

- 44 个 `*WithdrawApprovalProcessor` bean 注册，但 28 个采用 Pattern-A 的实体中 **18 个用内联脚本绕过 Processor**
- `ErpFinBadDebt` 整套 4 动作 per-mutation Processor（Submit/Approve/Reject/ReverseApprove）注册但**未被任何 xbiz `<source>` 引用**——BizModel 走 `Long id` 路径，孤儿 Processor 走 `String id` + `Long.valueOf(id)` 适配器
- 风险：维护者若复制 EmployeeAdvance Pattern-A 模板到 BadDebt.xbiz，**静默翻转公共参数类型** Long → String + 引入 `NumberFormatException` 路径

### 4.3 Minor findings

- **P2-MA3-036**：`ErpInvCostAdjust.xbiz:4-30` 5 个 `<mutation>` 块**仅含 `<source>`**，**无 `<arg>` 无 `<return>` 形式声明**——是全域唯一省略形式参数/返回契约的 Pattern-A xbiz delta。运行时不破坏（id/svcCtx 经继承生成基解析），但若继承基被移除则丢失类型化参数。
- **P2-MA3-037**：`cancel` 动作 Long↔String 适配跨域不一致——purchase/sales 用 `Long orderId` BizModel + `String.valueOf(orderId)` 适配 Processor `String`；manufacturing/inventory 保持 `Long` 端到端。非 bug（有意适配 + 文档化），但跨域契约惯例分裂。
- **P2-MA3-038**：`withdrawApproval` 内联脚本绕过 Processor 状态校验——18 个实体的 `withdrawApproval` 用内联 XScript 仅校验 `approveStatus=='SUBMITTED'`，绕过对应 `*WithdrawApprovalProcessor`（已注册 bean）中的 `validateNotCancelled`/`validateTransitionForWithdraw` 等更严格守卫。

### 4.4 维度 3 终态

**零 blocker / 1 major（F1 → P1-MA3-048）/ 3 minor（→ P2-MA3-036/037/038）**。ServiceContext 处理 100% 一致；返回类型零 drift；mandatory 框架委托守卫。

---

## 5. 维度 4：权限注解一致性

**裁决：零 blocker / 1 major（全域敏感动作零运行时保护）——本审计最严重发现**

### 5.1 基线计数（实时仓库 grep）

| 机制 | 总计 | 备注 |
| --- | --- | --- |
| `@BizAuth`（Java 源） | **0** | 7 命中全部在 docs/*.md 描述缺失 |
| `@BizAuthorize`（Java 源） | **0** | 1 命中在 plan 检查清单 |
| `@BizPermission`（Java 源） | **0** | 不存在 |
| `@PreAuthorize`/`@Secured`/`@RolesAllowed`（任何 JSR/Spring） | **0** | 无 |
| `<auth>` 元素（xbiz） | **0** | 704 xbiz 文件 |
| `permission` 属性（xbiz action） | **0** | — |
| `publicAccess` 属性（xbiz） | **0** | — |
| `permission:` 声明（AMIS view.xml） | **0** | 全部 view 文件 |
| Java 源 import `io.nop.api.core.annotations.*(Auth|Permission|Authorize|Role)` | **0** | — |

### 5.2 实际运行时机制——三层全部 OFF 或空

**(a) HTTP 路径认证**：默认开启（`/r/*`/`/graphql*`/`/p/*`/`/f/*`/`/jsonrpc` 需登录用户）。任何认证用户通过此层，**无角色检查**。

**(b) Action-level（FNPT）授权**：**OFF**。
- `app-erp-all/src/main/resources/application.yaml` 未设 `nop.auth.enable-action-auth` → 平台默认 `false`
- 即使开启，FNPT 粒度**粗**：每实体仅 2 权限点（`{Entity}:query` / `{Entity}:mutation`）。所有敏感动作（post/reverse/close/approve/writeOff/handleInboundWebhook）塌缩为单一 `{Entity}:mutation` 桶——**无 per-action 区分**
- `_erp-*.action-auth.xml` 每个仅声明 `2 × entity_count` 权限点（finance 72=36×2 / mfg 62=31×2 / crm 68=34×2 / notify 6=3×2）——**无 per-action FNPT** 如 `ErpFinVoucher:post`
- 全仓零 `nop_auth_role_resource` 种子数据——**即使开启 enforcement 也无角色被授予任何 FNPT**

**(c) Data-level 授权**：声明但**空**。
- 19 个 `erp-<short>.data-auth.xml` 全部为 147 字节桩 `<objs/>`（零行过滤规则）
- 平台文档：「bizObj 无任何规则 → `isPermitted` 返回 `true`」——checker wired 但无规则可应用，许可一切
- `roles-and-permissions.md:91`「数据权限不依赖操作级开关，始终启用」技术真但**运营上空洞**

### 5.3 敏感动作无保护清单（19 域抽样）

**全域所有抽样的敏感动作均无运行时权限保护（仅 HTTP 登录）**：

- **finance**：post voucher / reverse voucher / closePeriod / reverseClose（反结账 kill-switch）/ reverseApprove（expense claim/bad debt/budget）/ writeOff（bad debt/notes rec/payable）—— `ErpFinAccountingPeriodProcessor.java:130,274` / `ErpFinPostingProcessor.java:209` / `ErpFinBadDebtProcessor.java:61,93`
- **manufacturing**：start/close/cancel work order / approve subcontract / release MRP —— `ErpMfgWorkOrderProcessor.java:118,141,156`
- **purchase/sales**：approve/reject/reverseApprove 全 5 动作审批集（ErpPurOrder/Invoice/Payment/Receive/Return/Requisition/ErpSalOrder/Delivery/Invoice/Receipt/Return）—— xbiz 全部零 `<auth>`
- **inventory**：confirm stock move / cancel / approve landed cost / approve cost adjust —— `ErpInvStockMoveProcessor.java:82,96` / `ErpInvLandedCostProcessor.java:74`
- **hr**：approve payroll / approve leave / approve recruitment / approve shift swap —— `ErpHrSalarySimulationBizModel.java:406` / `ErpHrLeaveRequestBizModel.java:87`
- **assets**：capitalize / suspend / resume / dispose / split / merge / depreciation
- **b2b（最敏感）**：`handleInboundWebhook`（`IErpB2bAsnBiz.java:27-33`）—— 依赖应用层 HMAC 校验（业务代码非平台 RBAC）+ 全 EDI 生命周期（`createOutbound`/`markSent`/`markError`/`retry`/`cancel`/`createInbound`/`archive`，`IErpB2bEdiDocBiz.java:27-58`）
- **cs/crm/contract/maintenance/quality/projects/aps/logistics/drp/notify**：follow 同一全域模式——零权限注解

### 5.4 xbiz/Java 不一致

**裁决：无不一致——因两层均统一为空**。352 手写 xbiz delta 零 `<auth>`；380 BizModel + 198 Processor 零权限注解；反射发现动作（xbiz 空 `<actions/>`）继承自 BizModel——同样无注解。交叉核对零不匹配仅因**两层均未声明任何权限**。

### 5.5 与 MA2/MA6 关系

- **不与 P1-MA2-093/094 重复**：MA2-093/094 是 orgId 多公司数据级行过滤隔离（不同维度——data-level row filter by orgId），本审计是 action-level 权限注解（API 契约层）
- **与 roadmap A6.1/A6.2（todo）深度交叉**：A6.1「全域 @BizMutation/@BizQuery 权限注解完整性 grep」+ A6.2「finance+mfg+pur+sal 权限深度抽样」——本审计**前置汇总**了 A6.1/A6.2 的 grep 基线（零注解）+ 全域敏感动作无保护清单。MR2 修复 P1-MA3-046 时应与 A6.1/A6.2 执行**协同**（A6.1/A6.2 deep-dive 时复核本清单 + 补运行时验证）

### 5.6 P0 裁决

**维持 P1 不升 P0**——(1) owner doc `roles-and-permissions.md §运行基线:123-138` 显式声明为有意默认（灰度推进）；(2) 单组织种子下无活跃跨组织数据破坏（与 P1-MA2-093/094 同型）；(3) 平台层 HTTP 认证默认开启；(4) `enable-action-auth=false` 是保护性默认（错误方向偏保守）。深度权限审计归 MA6。

### 5.7 维度 4 终态

**零 blocker / 1 major（→ P1-MA3-046）/ 0 minor**。

---

## 6. 维度 5：生成 *Api.java vs xbiz/orm 契约漂移

**裁决：codegen 纪律 PASS（零手编）；1 major（RPC vs GraphQL 契约面分裂 + 9 模块零生成）**

### 6.1 Codegen 纪律核查——EXCELLENT

- 211 个 `*Api.java` 全部首行含 `//__XGEN_FORCE_OVERRIDE__`（强制覆盖标记）
- 全部 13 行，全部 `extends ICrudApi<InputBean, OutputBean>` 空体——零自定义方法声明
- 跨域抽样（assets/finance/master-data/purchase 等）extends 子句唯一模式 `ICrudApi<I,O>`——由 `/nop/templates/crud-api/` 模板从 xmeta 生成（非 api.xml 驱动）
- **零手编痕迹**——codegen 纪律 PASS

### 6.2 Finding（major）：RPC 契约面 vs GraphQL 契约面分裂 + 9 模块零生成

**详情见 P1-MA3-049**。摘要：

- **211 个 `*Api.java` 全部仅 CRUD**（`ICrudApi` 暴露约 25 方法：findCount/findPage/findFirst/findList/get/batchGet/asDict/save/saveOrUpdate/copyForNew/update/batchUpdate/updateByQuery/delete/batchDelete/batchModify/deleteByQuery/M2M ops 等）
- **所有业务动作（approve/cancel/post/reverse/close/submitForApproval/createFromRequisition/跨聚合写 Facade 等）仅 GraphQL/xbiz 可达**——RPC 客户端无法触发任何业务行为
- 抽样：`ErpPurOrderApi`（RPC）0 自定义 vs `IErpPurOrderBiz`（GraphQL）5 自定义（cancel/batchApprove/createFromRequisition/existsActiveByRequisition/updateReceiveStatus）+ 5 动作审批集；`ErpFinVoucherApi`（RPC）0 vs `IErpFinVoucherBiz` 5（post/reverse/postVoucher/reverseVoucher/previewReverseVoucher）
- **9 个 api 模块（aps/b2b/contract/crm/cs/drp/hr/logistics/notify）`src/` 目录不存在**——零 `*Api.java` 生成，这些域无 RPC 契约面（甚至无 CRUD RPC）

### 6.3 设计性质裁决

RPC vs GraphQL 分裂**部分**是设计选择（RPC = data-CRUD channel；GraphQL/xbiz = business-behavior channel），但 **9 模块零生成 + RPC 客户端无法触发 approve/post/cancel 等业务动作**对「外部集成方需从 ORM 模型推断」（`docs/analysis/2026-07-10-deep-code-and-doc-consistency-analysis.md:200`）的运营可见性是重大缺口。登记 P1-MA3-049，MR2 裁决方向（补 api.xml 显式契约 / 补 9 模块生成 / 文档标注 RPC 仅 CRUD）。

### 6.4 维度 5 终态

**codegen 纪律 PASS / 1 major（→ P1-MA3-049）**。

---

## 7. 维度 6：未声明/未文档化 API（code→contract 反向 drift）

**裁决：note（Nop-normal 模式，非缺陷）**

### 7.1 模式说明

全域绝大多数业务 mutation 走 Pattern C（BizModel Java 注解 → 运行时 biz-gen 反射注册到 xbiz）。这些方法**手写 xbiz delta 中不声明**（delta 为空 `<actions/>`），但运行时 API 面完整。这是 Nop 平台 normal 模式——**不是缺陷**。所有 Java `@BizMutation` 方法对 `I*Biz` 接口 `@Override`，编译期契约校验。

### 7.2 信息性标注（非缺陷）

Pattern C 的敏感 state-changing `@BizMutation` 方法（Java-only，xbiz delta 不声明）：

- **finance**：ErpFinVoucher（post/reverse/postVoucher/reverseVoucher）/ ErpFinAccountingPeriod（closePeriod/finalizePeriod/reverseClose）/ ErpFinBadDebt（writeOff/recover/submit/approve/reverseApprove/reject/runBadDebtProvision）/ ErpFinReconciliation（create/post/reverse/runAutoReconciliation）/ ErpFinBudgetScenario（submit/approve/reject/cancel/rollForward/carryForward）等
- **manufacturing**：ErpMfgWorkOrder（checkAvailability/start/stop/resume/close/cancel/reportCompletion）/ ErpMfgJobCard（startJob/recordWork/submitJob/completeJob/holdJob/resumeJob/cancelJob）/ ErpMfgSubcontractOrder（cancel/issueMaterials/receiveFinished/postProcessingFee/reverseCompletion）/ ErpMfgMrpPlan（runMrp）等
- **inventory**：ErpInvStockMove（generateMove/confirm/complete/cancel/reverse）/ ErpInvCostAdjust（applyCostAdjust/reverseCostAdjust）/ ErpInvStockTake（startTake/completeTake/cancelTake）等
- **hr**：ErpHrSalary（calculateSalary/runPayroll/markPaid/voidSalary）/ ErpHrLeaveRequest（submit/approve/reject/cancel）/ ErpHrRecruitment（moveToScreening/scheduleInterview/makeOffer/hire）等
- **assets**：ErpAstDepreciationSchedule（executeDepreciation/executeBatchDepreciation/reverseDepreciation）/ ErpAstCip（startConstruction/addCostItem/transferToAsset/reverseTransfer）等

这些方法**全部**通过 `I*Biz` 接口 `@Override` 编译期契约化。**非未声明 API**——只是 xbiz delta 不重复声明（避免双真相源）。Nop-normal。

### 7.3 模式选择不一致（信息性）

finance 域内**等价语义**的审批流存在两种模式选择：
- xbiz 委托（Pattern A）：`ErpFinEmployeeAdvance`/`ErpFinExpenseClaim`
- Java 直连（Pattern C）：`ErpFinBadDebt`/`ErpFinBudgetScenario`/`ErpFinNotesPayable`/`ErpFinNotesReceivable`

两者均编译通过 + 运行时正确注册。**非缺陷**——仅为模式选择不一致（认知负荷问题）。归 P1-MA3-048 同型根因（孤儿 Processor）。

### 7.4 维度 6 终态

**零 blocker / 零 major / 零 minor（note 级观察）**。

---

## 8. 维度 7：跨实体 API 一致性

**裁决：1 major（动词/参数跨域严重不一致）+ 1 minor**

### 8.1 CRUD 基模式——100% 一致

全域 `I*Biz` 接口均 `extends ICrudBiz<T>`（核对 200+ 实体）。标准 CRUD 动作（findCount/findPage/findFirst/findList/get/batchGet/save/saveOrUpdate/update/delete/batchDelete 等）经基类继承全域一致。零实体覆盖或遮蔽标准 CRUD 方法名。

### 8.2 跨域 Facade（`IErpFinVoucherBiz`）——EXCELLENT

9 域 11 调用方模块经专用 `*PostingExecutor` 适配器调用，签名 `post(PostingEvent) → Long` / `reverse(String, ErpFinBusinessType) → Long` 全域一致：

- 调用方：purchase/sales/inventory/hr/assets/manufacturing/maintenance/projects/quality/logistics/finance(internal)
- 全部 `@Inject IErpFinVoucherBiz voucherBiz;` + `voucherBiz.post(event, ctx)` / `voucherBiz.reverse(billHeadCode, businessType, ctx)`
- 零调用方绕过 Facade 直达 `ErpFinPostingProcessor`（符合 `processor-extension-pattern.md` 硬规则 2）

### 8.3 Finding（major）：API 命名/参数模式跨域严重不一致

**详情见 P1-MA3-047**。摘要：

**(a) 审批模式两约定共存，~14 实体偏离标准**

- **Pattern A 标准约定**（39 实体）：`submitForApproval`/`approve`/`reject`/`reverseApprove`/`withdrawApproval` 5 动作集 + `String id` + xbiz 委托
- **Pattern B 偏离**（~14 实体）：动词分歧（9 实体用 `submit` 而非 `submitForApproval`；`ErpHrSalarySimulation` 唯一用 `submitForReview`）/ 动作集不对称（`ErpInvLandedCost` 仅 approve+reverseApprove；`ErpHrDevelopmentPlan` 仅 submit+approve；`ErpHrTimesheet` 仅 submit）/ id 类型不一致（13 实体用 `Long id`，3 实体 `ErpHrLeaveRequest`/`ErpHrRecruitment`/`ErpMfgForecast` 用 `String id`）/ 参数命名不一致（`id` vs `simulationId`/`assessmentId`/`swapRequestId`/`timesheetId`/`approvalId`）
- `ErpMdSupplierApproval` 复用 `approve`/`reject` 表达供应商准入生命周期（DRF→APP→PRS→SUS→REJ）非单据审批——**语义碰撞命名风险**

**(b) 状态迁移动词跨域严重分歧**

| 语义 | 跨域命名变体 |
| --- | --- |
| "开始" | bare `start`（4 实体）/ `startConstruction`/`startRepair`/`startProject`/`startTask`/`startAction` |
| "完成" | bare `complete`（5 实体）/ `completeTake`/`completeShipment`/`completeJob`/`completeWork`/`completeAssessment`/`completePlan`/`completeTask`/ `reportCompletion`（ErpMfgWorkOrder 唯一变体） |
| "过账/支付" | `post`（finance/inventory/assets 主流）/ **`markPaid`（hr 唯一）** / `postNcr` / `postSettlement` |
| "结账" | bare `close` / `closePeriod`/`finalizePeriod`/`closeProject` |
| "红冲" | bare `reverse` / `reverseVoucher`/`reverseCostAdjust`/`reverseNcr`/`reverseConfirm`/`reverseCompletion`/`reverseDepreciation`/`reverseSettlement`/`reverseCashRepay`/`previewReverseVoucher`（`reverse` 前缀在"撤销审批"与"反向状态迁移+红字凭证"间**重载**） |
| "取消" | bare `cancel`（40+ 实体，参数名分裂 `id` vs `orderId`/`paymentId`/`invoiceId`/...）；2 实体用 `String id`（ErpHrLeaveRequest/ErpMfgForecast），其余 `Long` |

**(c) HR `markPaid` 跨域异类**

`IErpHrSalaryBiz.markPaid(Long salaryId)` 是全域唯一用 `markPaid` 表达"过账/支付"语义的动作。finance/inventory/assets 统一用 `post`。HR 跨域集成（工资过账触发 IErpFinVoucherBiz.post Facade）的语义入口与对接的目标动词不一致。

**(d) 批量操作覆盖不对称**

仅 4 个 batch 操作存在（`ErpPurOrder.batchApprove`/`ErpSalOrder.batchApprove`/`ErpQaInspection.batchPassInspection`/`ErpApsOperationOrder.batchScheduleForward`），形状一致（`Collection<String> ids → BatchOperationResult`）但**覆盖不对称**——零 `batchCancel`/`batchReject`/`batchReverse`/`batchPost`。UI 批量审批订单存在但无对称批量拒绝/取消。

### 8.4 维度 7 终态

**零 blocker / 1 major（→ P1-MA3-047）/ 0 minor**（cancel 参数 Long↔String 分歧已在 P2-MA3-037 登记）。

---

## 9. Finding 清单（汇总）

### 9.1 Major → P1（4 项，全部目标 MR2）

| Finding ID | 维度 | 域 | drift 方向 | 严重性 | 受影响文件 | drift 描述 | 影响 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `P1-MA3-046` | Dim 4 权限 | 全 19 域 | code→contract（缺失） | major | 全域 380 BizModel + 198 Processor + 704 xbiz + 19 data-auth.xml | 全域敏感动作（post/reverse/close/reverseClose/approve/cancel/writeOff/handleInboundWebhook/全 EDI 生命周期）**零运行时权限保护**——`@BizAuth`=0 + xbiz `<auth>`=0 + `enable-action-auth=false` + 全部 data-auth.xml 空 `<objs/>` + 零角色-resource 种子。仅 HTTP 登录屏障 | 任何认证用户可执行任意敏感动作（反结账 kill-switch / 红冲已审核凭证 / B2B webhook 处理 / 坏账核销）|
| `P1-MA3-047` | Dim 7 跨实体一致 | 全 19 域 | contract 内部不一致 | major | 全域 I*Biz 接口 + xbiz delta | API 命名/参数模式跨域严重不一致：(a) 审批两约定共存 14 实体偏离标准（`submit` vs `submitForApproval` vs `submitForReview`；Long id vs String id；5 动作集 vs 部分子集）；(b) 状态迁移动词跨域分歧（`start` vs `startProject`；`complete` vs `completeJob`；`post` vs `markPaid`；`reverse` 重载"撤销审批"与"红字凭证"）；(c) `cancel` 参数名分裂（`id` vs `orderId`/`paymentId`/...）| 公共 API 集成方需逐实体学习，跨域调用易混淆；维护者复制模板引入静默语义偏移；批量操作覆盖不对称（UI 批量审批无对称拒绝） |
| `P1-MA3-048` | Dim 3 参数/返回 | finance + purchase + 6 域 | code→contract 影子 | major | 44 个 `*WithdrawApprovalProcessor` bean + ErpFinBadDebt 4 动作 per-mutation Processor 全套 + `app-service.beans.xml` | 孤儿 Processor bean 携带 `String id` 影子契约——44 WithdrawApprovalProcessor 注册但 18 被 inline script 绕过；BadDebt 整套 4 动作 Processor 注册但未被任何 xbiz `<source>` 引用（BizModel 走 `Long id` 路径影子）。维护者若复制 Pattern-A 模板到 BadDebt.xbiz 静默翻转公共参数类型 Long→String + 引入 NumberFormatException 路径 | 维护性/潜伏风险——非活跃缺陷但模板复制即出错；BadDebt 4 动作整套 Processor 死代码 |
| `P1-MA3-049` | Dim 5 生成 *Api.java | 全域（重 9 模块） | contract 缺口 | major | 211 `*Api.java`（仅 CRUD）+ 9 api 模块零生成（aps/b2b/contract/crm/cs/drp/hr/logistics/notify） | RPC 契约面 vs GraphQL 契约面分裂——211 `*Api.java` 全部仅 `ICrudApi`（约 25 方法），所有业务动作（approve/cancel/post/reverse/submitForApproval/跨聚合写 Facade）仅 GraphQL/xbiz 可达；9 个 api 模块零 `*Api.java` 生成（无 RPC 契约面，甚至无 CRUD RPC） | 外部 RPC 集成方无法触发业务行为；9 模块零 RPC 契约；与 P1-MA3-005（logistics 缺 architecture 拆分）部分相关 |

### 9.2 Minor → P2（4 项，watch-only）

| Finding ID | 维度 | 域 | drift 方向 | 严重性 | 受影响文件 | drift 描述 |
| --- | --- | --- | --- | --- | --- | --- |
| `P2-MA3-036` | Dim 3 参数/返回 | inventory | xbiz 形式声明缺失 | minor | `ErpInvCostAdjust.xbiz:4-30` | 全域唯一 Pattern-A xbiz delta 5 个 `<mutation>` 块仅含 `<source>` **无 `<arg>` 无 `<return>` 形式声明**——运行时不破坏（继承生成基解析）但若继承基移除则丢失类型化参数 |
| `P2-MA3-037` | Dim 3 + 7 参数 | purchase/sales/mfg/inv | 跨域契约惯例分裂 | minor | ErpPurOrderBizModel.java:50 / ErpSalOrderBizModel.java:64 / ErpMfgWorkOrderProcessor / ErpInvStockMoveProcessor | `cancel` 动作 Long↔String 适配跨域不一致——purchase/sales BizModel `Long orderId` + `String.valueOf(orderId)` 适配 Processor `String`；mfg/inv 保持 `Long` 端到端。有意适配非 bug，但契约惯例分裂 |
| `P2-MA3-038` | Dim 3 + 2 | 18 实体（purchase/sales/mfg/assets/contract） | xbiz 内联脚本绕过 Processor | minor | 18 个 `*.xbiz` 的 `withdrawApproval` 内联块 vs 对应 `*WithdrawApprovalProcessor`（已注册 bean） | `withdrawApproval` 内联脚本仅校验 `approveStatus=='SUBMITTED'`，绕过对应 Processor 的 `validateNotCancelled`/`validateTransitionForWithdraw` 等更严格守卫。运行时行为与 Processor 实现不一致 |
| `P2-MA3-039` | Dim 1 文档样板 | docs（4 处） | 文档样板 drift | note | `docs/skills/README.md:93` / `docs/context/ai-autonomy-policy.md:70` / `docs/context/project-context.md:69` / `docs/backlog/audit-remediation-roadmap.md:83` | 4 处项目文档将 `module-*/model/*.api.xml` 列为「保护区域 ask-first」——Nop 通用样板继承，文件不存在时无害但产生"声明未物化"误读 |

### 9.3 维度裁决汇总

| 维度 | 裁决 | Finding |
| --- | --- | --- |
| 1 api.xml 缺失性质 | **设计选择**（非 drift） | P2-MA3-039（文档样板 side-effect） |
| 2 xbiz vs Java | **CLEAN** | 无 |
| 3 参数/返回 drift | 零 blocker/major TYPE drift | P1-MA3-048 + P2-MA3-036/037/038 |
| 4 权限注解 | **major**（全域零保护） | P1-MA3-046 |
| 5 生成 *Api.java | codegen 纪律 PASS；contract 缺口 major | P1-MA3-049 |
| 6 未声明 API | note（Nop-normal） | 无 |
| 7 跨实体一致 | major（动词/参数跨域不一致） | P1-MA3-047 |

---

## 10. 与既有审计的交叉去重

### 10.1 与 A3.1~A3.5 已登记 P1-MA3-001~045 去重

- **P1-MA3-046（权限注解全缺失）** ≠ P1-MA3-008（admin/super-admin 角色身份冲突）/ P1-MA3-010（8 扩展域无角色基线）/ P1-MA3-012（危险操作审计重复 4 处）——后者是**文档层**角色定义/审计描述问题，本 finding 是**代码层**动作权限注解 + data-auth + action-auth enforcement 全缺失
- **P1-MA3-047（API 命名不一致）** ≠ P1-MA3-013（状态码目录重复）——后者是字典值文档双真相源，本 finding 是动作动词/参数跨域不一致
- **P1-MA3-048（孤儿 Processor）** ≠ P2-MA3-030（reverse() REQUIRES_NEW doc↔code 冲突）/ P1-MA3-031（CommitmentAcctDocProvider budget.md vs posting.md 矛盾）——后者是文档↔代码语义 drift，本 finding 是 xbiz↔Processor 注册漂移
- **P1-MA3-049（RPC vs GraphQL 分裂）** ≠ P1-MA3-005（logistics 缺 architecture 拆分）——后者是文档分层缺失，本 finding 是契约面分裂（部分相关但不重叠）

**结论：经交叉去重，本审计 4 项 P1 与 A3.1~A3.5 已登记 P1-MA3-001~045 无重复登记。**

### 10.2 与 MA2 已登记 P1-MA2-* 去重

- **P1-MA3-046** ≠ P1-MA2-093/094（orgId 多公司隔离）——不同维度（action-level 注解 vs data-level orgId 行过滤）
- **P1-MA3-046** ≠ P1-MA2-020（reverse-close approval kill-switch）——后者是单点 kill-switch 语义，本 finding 是全域权限基础设施缺失（P1-MA2-020 是 P1-MA3-046 的一个具体子例）
- **P1-MA3-048** ≠ P1-MA2-054（WithdrawApproval/Reject Processor 死代码未接线，purchase 域，已 MA2 登记）——**部分重叠**：MA2-054 是 purchase 域死代码登记，本 finding 是全域 44 WithdrawApprovalProcessor + BadDebt 整套的系统性汇总（MA2-054 是子例）。MR2 修复 P1-MA3-048 时应吸纳 P1-MA2-054 一并裁决

**结论：本审计 4 项 P1 与 MA2 已登记 P1 经交叉去重无升级/降级/重复登记。P1-MA3-048 与 P1-MA2-054 部分重叠（子例关系），MR2 协同裁决。**

---

## 11. scope matrix §2.3「API 契约一致性」行终态

**`新维度` → `⚠️(P1)`**

依据：本审计完成（Verdict FAIL，零 BLOCKER），4 项 P1 待 MR2（P1-MA3-046~049），4 项 P2 watch-only（P2-MA3-036~039）。

---

## 12. 剩余风险

1. **MR2 修复时序风险**：P1-MA3-046（权限缺失）依赖 MA6 A6.1/A6.2（todo）深度权限审计的协同裁决——若 MR2 先于 A6.1/A6.2 执行，可能仅做表面注解补全而非架构级 RBAC 推进。建议 MR2 P1-MA3-046 推进时强制协同 A6.1/A6.2。
2. **api.xml 引入决策**：P1-MA3-049 MR2 裁决时可能触发"是否引入手写 api.xml"的架构决策（plan Non-Goal 已声明此类决策不在审计范围）——需人工决策。
3. **P1-MA3-048 模板复制风险**：在 MR2 修复前，维护者复制 Pattern-A 模板到 BadDebt/NotesReceivable/NotesPayable/BudgetScenario.xbiz 会静默翻转公共参数类型。建议 MR2 优先级提升。
4. **P1-MA3-047 跨域命名重构风险**：API 命名重构（如统一 `submit` → `submitForApproval` / `markPaid` → `post`）会破坏现有 AMIS 前端调用 + 测试断言——MR2 裁决时需评估向后兼容策略（保留别名 vs 一次性破坏）。

---

## 13. 裁决通过/失败

**FAIL（有 drift）**——零 BLOCKER，4 项 major → P1 全部目标 MR2，4 项 minor → P2 watch-only。本审计原则上无 P0，符合 plan 声明。

**audit 关闭条件**：本报告产出 + arm-index §P1 汇总登记 P1-MA3-046~049 + scope matrix §2.3 行标记 `⚠️(P1)` + roadmap A3.6 推进 `todo → done`。

---

## 14. 审计方法与限制

- **方法**：`multi-dimensional-audit-prompt.md` 7 维度适配 API 契约主题。每维度至少一句裁决（含"本维度无 drift"）。维度内搜索加权参考 `docs/skills/README.md §已知失败模式`（codegen 纪律 / 跨域 Facade 一致性）。
- **覆盖**：S+A 级域（finance/mfg/pur/sal/inv/hr/assets）逐实体抽样深核；B+C 级域（crm/cs/aps/logistics/b2b/contract/drp/projects/quality/maintenance/master-data/notify）合并抽样。
- **限制**：本审计**只识别 drift + 分类**，不做批量修复（修复在 MR2）。本审计**不审**测试覆盖深度（归 MA5）/ view.xml drift（归 MA4 A4.6-A4.8）/ 业务语义层 drift（归 A3.3-A3.5）。本审计**不手写 api.xml**（架构决策不在范围）。
- **验证**：审计不改代码/文档（除审计报告 + 索引/矩阵更新），故无单测回归。完整仓库 `mvn test` 仅作 Closure 回归基线确认（同型审计 plan 标准 Closure 实践）。
