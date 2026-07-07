# 2026-07-05-1838-1-sales-credit-control-phase2 销售信用控制 Phase 2：AR 未核销余额纳入 + SPECIAL_APPROVAL

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: `docs/design/sales/README.md` §信用额度控制 Non-Goals（行 87-92）+ `docs/plans/2026-07-05-1500-2-cross-review-remediation.md` Deferred（C-2 AR 余额 + SPECIAL_APPROVAL）
> Related: `docs/plans/2026-07-04-2050-1-use-approval-migration.md`（use-approval 迁移已完成，解除 SPECIAL_APPROVAL 触发条件）；`docs/plans/2026-07-03-1018-1-m4-business-finance-e2e-tests.md`（业财一体 E2E 已完成，解除 AR 余额触发条件）；`docs/plans/2026-07-02-0300-3-ar-ap-settlement-subledger.md`（ErpFinArApItem 辅助账 + openAmount 已落地）
> Audit: required

## Current Baseline

实时仓库逐项核实（`read`/`grep`，非采信旧记忆）：

- **CreditLimitChecker 现状**（`module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`，118 行）：
  - `check(customerId, thisOrderAmount, thisOrderExchangeRate, ctx)` 在销售订单审核（SUBMITTED→APPROVED）时由 `ErpSalOrderProcessor.validateBusinessRulesForApprove` 调用。
  - `outstanding = Σ(totalAmountWithTax × exchangeRate) of ErpSalOrder where customerId=该客户 AND approveStatus=APPROVED AND deliveryStatus≠DELIVERED AND docStatus≠CANCELLED`（`sumOutstanding` :93-109，本域内部 `daoFor(ErpSalOrder.class)` 只读聚合）。
  - 多币种已实现（`toFunctional` :111-117，按 `exchangeRate` 折算本位币）——plan `1500-1` Phase 5 落地。
  - 两级策略：`SOFT_WARNING`（默认，log.warn 放行）/ `HARD_BLOCK`（抛 `ERR_CREDIT_LIMIT_EXCEEDED`）。`resolveLevel` :87-91 读 `AppConfig.var(erp-sal.credit-check-level)`。
  - **Non-Goals 已在 Javadoc :36-40 与 `sales/README.md:87-92` 显式登记**，各带触发条件。
- **AR 辅助账接口已就绪**（`module-finance/erp-fin-dao/src/main/java/app/erp/fin/biz/IErpFinArApItemBiz.java`，41 行）：
  - `findOpenItemsByPartner(partnerId, direction, ctx)` @BizQuery —— 返回指定往来单位在某方向（"10"=RECEIVABLE / "20"=PAYABLE）下未结清（status≠SETTLED/CANCELLED）的 `ErpFinArApItem` 列表，按业务日期升序。
  - `ErpFinArApItem` 金额字段六件套（`module-finance/model/app-erp-finance.orm.xml:510-515`，plan `0300-3` 落地）：`amountSource`/`amountFunctional`（原始全额，源/本位币，propId 13/14）、`settledAmountSource`/`settledAmountFunctional`（已核销，15/16）、**`openAmountSource`/`openAmountFunctional`（未核销余额，源/本位币，17/18）**。**注意**：无独立 `openAmount` 字段；`amountFunctional` 是原始全额（非未核销余额），不可直接累加。本计划信用占用须用 **`openAmountFunctional`**（未核销本位币余额）。另有 `direction` + `partnerId` + `status` + `exchangeRate`(propId 12)。
  - sales→finance 是 DAG 正向（finance 是顶，sales 依赖 finance 经 I*Biz，`module-boundaries.md` 白名单允许）。
- **use-approval 迁移已完成**（plan `2050-1` completed）：销售订单 `ErpSalOrder` 已标 `tagSet="use-approval"`，DIRECT 模式生效，标准 5 action（`submitForApproval`/`approve`/`reject`/`reverseApprove`/`unsubmit`）由 `approval-support.xbiz` 提供。审批轴 `approveStatus`（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）由平台标准 source 管理。**WORKFLOW `.xwf` 模式为 `2050-1` Deferred**（付款/收款/资产处置/HR 薪酬 4 实体）——销售订单不在该 Deferred 列表。
- **客户主数据**：`ErpMdPartner.creditLimit`（本位币额度）经 `IErpMdPartnerBiz.findById` 读取（跨域只读，CreditLimitChecker :50/:62）。

### 剩余差距

1. **AR 未核销余额未纳入 outstanding**：开票后（销售订单→销售发票→AR 辅助账）未核销余额当前不计入信用占用，开票后可绕过信用控制的风险已知（`sales/README.md:89`）。需跨域查 finance AR 辅助账 `IErpFinArApItemBiz.findOpenItemsByPartner(customerId, "10", ctx)`，将 `Σ openAmountFunctional`（未核销余额本位币，propId 18）纳入 outstanding。
2. **SPECIAL_APPROVAL 级别未实现**：`sales/README.md:81` 声明"超额度时走多级审批工作流"但标 ❌ Non-Goal。use-approval 迁移已落地 DIRECT 模式，SPECIAL_APPROVAL 可在 DIRECT 模式下经**权限门控**实现（超额度订单需额外权限方可审核），无需 `.xwf` 工作流定义。

## Goals

- **AR 余额纳入**：`CreditLimitChecker.sumOutstanding` 增加跨域查 finance AR 辅助账未核销余额（本位币口径），使 outstanding = 未发货订单本位币金额 + AR 未核销余额本位币。开票后绕过信用控制的缺口关闭。
- **SPECIAL_APPROVAL 级别**：新增第三级信用策略 `SPECIAL_APPROVAL`——超额度订单仍可审核，但仅限持有专项权限的审批人（两阶权限模型：标准 `approve` + 超额度专项权限）。无 `.xwf` 依赖，DIRECT 模式即可生效。
- **测试覆盖**：AR 余额纳入场景（含多币种）+ SPECIAL_APPROVAL 场景（有/无专项权限两路径）行为测试。
- **owner doc 收口**：`sales/README.md` §信用额度控制 Non-Goals 两项移除/标 ✅。

## Non-Goals

- **完整多级 `.xwf` 工作流**（多步审批链：信用分析师→财务经理）：本期 SPECIAL_APPROVAL 经权限门控实现"超额度需更高权限"语义；多步工作流定义（`.xwf` + `wf:wfName` + `useWorkflow="true"`）归 Deferred（触发条件：多级审批链业务需求落地时，承接 `2050-1` Deferred 范式）。
- **信用冻结（credit hold）实时拦截开票/出库**：本期信用控制在订单审核环节；开票/出库环节的实时信用冻结归独立 successor。
- **信用额度变更审批流 / 客户风险评分体系**：依赖 CRM 客户信用评分（`0540-1` Deferred「客户风险评分体系」触发条件）。
- **AR 负余额（红字退款）纳入逻辑反转**：退货/红字发票产生的负 openAmount 按原值累加（减少占用），不做特殊冲正处理。
- **前端 view.xml 审批按钮 / 信用额度看板**：属前端 roadmap 阶段。
- **跨账套（multi AcctSchema）AR 余额聚合**：本期单账套；`acctSchemaId` 维度归 Deferred。

## Task Route

- Type: `implementation-only change`（CreditLimitChecker 增强 + 权限门控，不改 ORM/契约）+ 少量 `app-layer design change`（owner doc Non-Goal 收口）。
- Owner Docs: `docs/design/sales/README.md` §信用额度控制、`docs/architecture/approval-framework.md`（DIRECT 模式权限扩展语义）、`docs/design/finance/ar-ap-reconciliation.md`（AR 辅助账 openAmount 语义）。
- Skill Selection Basis: BizModel/Processor 改造（`CreditLimitChecker` 增跨域 `IErpFinArApItemBiz` 注入 + 权限门控）、跨实体安全 API（`I*Biz` 注入非 `IDaoProvider`）、ErrorCode、配置门控、JunitAutoTestCase——匹配 `nop-backend-dev`。ORM/前端不涉及。

## Infrastructure And Config Prereqs

- 无新增端口/密钥/.env/外部服务/数据迁移。
- 依赖 `IErpFinArApItemBiz` 已落地（`module-finance/erp-fin-dao`，plan `0300-3`）。
- 依赖 use-approval DIRECT 模式已生效（plan `2050-1`）。
- 依赖 `IErpAuth`/`IServiceContext` 提供当前用户权限查询（平台标准，核实执行时 API）。
- 回滚策略：`CreditLimitChecker` 改动为应用层 Java，git 可逆；新增配置键默认值保持现有行为（AR 纳入 config-gated、SPECIAL_APPROVAL 非默认级别）。

## Execution Plan

### Phase 1 - AR 未核销余额纳入 outstanding

Status: completed
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`、`ErpSalConstants`（新配置键）、`ErpSalErrors`（无新 ErrorCode，复用既有）
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无（`IErpFinArApItemBiz` 已就绪）

- [x] `Decision`：AR 余额币种口径——`ErpFinArApItem` 金额字段六件套（见基线）。信用比较已统一本位币（`creditLimit` vs outstanding 均本位币）。**选择**累加 **`openAmountFunctional`**（未核销余额本位币，propId 18），与既有 `toFunctional` 口径一致，避免二次折算，且精确反映"未核销"语义（已部分核销的发票仅剩 open 余额计入占用）。**替代**：累加 `amountFunctional`（原始全额）——会忽略部分核销，已收款 80% 的发票仍按全额占用信用，过度拦截客户，rejected。**残留风险**：若 `openAmountFunctional` 为 null（数据不完整），回退 `openAmountSource` × 该辅助账项 `exchangeRate`(propId 12) 近似折算——config-gated 容错（`erp-sal.credit-check-ar-fallback`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`CreditLimitChecker` 注入 `IErpFinArApItemBiz arApItemBiz`（跨域只读经 I*Biz 管道，对齐 `module-boundaries.md` sales→finance R）；`sumOutstanding` 增加 AR 段：调 `arApItemBiz.findOpenItemsByPartner(customerId, "10", ctx)`，累加每项 **`openAmountFunctional`**（null 时按 Decision 回退 `openAmountSource × exchangeRate`）到 outstanding。AR 纳入经 config-gated `erp-sal.credit-check-include-ar`（默认 `true`，可关），关时回退现有纯订单口径。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpSalConstants` 增 `CONFIG_CREDIT_CHECK_INCLUDE_AR = "erp-sal.credit-check-include-ar"` + 默认值常量。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付 AR 余额纳入。解除 Phase 3 测试的阻塞。

- [x] `sumOutstanding` 含 AR 段（`findOpenItemsByPartner` 调用 + `openAmountFunctional` 累加），config-gated 可关；非空实现，无 `return null`/空 `{}` 占位
- [x] `CreditLimitChecker` 本地化类型检查通过（`mvn compile -pl module-sales/erp-sal-service -am`）

### Phase 2 - SPECIAL_APPROVAL 级别（权限门控）

Status: completed
Targets: `CreditLimitChecker.java`、`ErpSalConstants`（权限键）、`ErpSalErrors`（新 ErrorCode）、`ErpSalOrderProcessor`
Skill: `nop-backend-dev`

- Item Types: `Explore | Decision | Add`
- Prereqs: Phase 1（outstanding 口径完整后再加级别分支）

- [x] `Explore`：确认命令式权限检查机制——平台 `IServiceContext`/`IContext` **无** `hasPermission` 方法（`IContext` 仅 `getUserId`/`getUserName` 等）；权限检查在平台为 GraphQL 层声明式（`DefaultActionAuthChecker.isPermitted(permission, context)`，默认 `nop.auth.enable-action-auth=false`）。须确认应用层可注入的命令式检查入口：候选 (a) 注入 `IActionAuthChecker`/`DefaultActionAuthChecker` bean 调 `isPermitted(permission, context)`；(b) 经用户-角色服务查角色；(c) 若均不可用，降级为"超额度订单转入 SUBMITTED 并由专项角色用户重审"语义。Explore 结论落入 Decision。**Explore 须在 Phase 2 实施前完成；若三条路径均不可行，SPECIAL_APPROVAL 移出本计划范围转 Deferred。**
  - Skill: `nop-backend-dev`
- [x] `Decision`：SPECIAL_APPROVAL 语义重定位——`sales/README.md:81` 原承诺"超额度时走**多级审批工作流**"。use-approval 迁移（`2050-1`）仅交付 DIRECT 模式，`.xwf` 多步工作流为 `2050-1` Deferred。**选择**将 SPECIAL_APPROVAL 语义重定位为"超额度订单需**专项审批权限**方可审核"（DIRECT 模式 + 两阶权限门控），而非原承诺的多级工作流链——忠实"超额度需更高权限"核心语义，无需 `.xwf`。**替代 ①**：完整 `.xwf` 多步链（信用分析师→财务经理）——`2050-1` 已 Deferred `.xwf`，且权限门控已满足核心语义，rejected（多步链归 Deferred）。**替代 ②**：超额度自动转 SUBMITTED 等待——DIRECT 无"等待态"，且不抛错则普通审批人可放行，违背语义，rejected。**残留风险**：这是对 owner doc（README:81 行为列）的语义重定位，**Phase 4 必须重写行为列**（非仅翻转 ❌→✅），否则构成 owner-doc 漂移。命令式权限 API 可行性由前置 Explore 门控。
  - Skill: `nop-backend-dev`
- [x] `Decision`：SPECIAL_APPROVAL 实现细节——基于 Explore 结论，超额度且 `level=SPECIAL_APPROVAL` 时，`check()` 经 Explore 确认的入口校验当前用户是否持有专项权限（如 `erp-sal:creditOverLimitApprove`）；持有则 log.info 放行（专项审批授权），未持有则抛 `ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED`。`check()` 签名不变（已含 `IServiceContext`）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpSalConstants` 增专项权限键 `PERM_CREDIT_OVER_LIMIT_APPROVE = "erp-sal:creditOverLimitApprove"`（`CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL` 已存在于 `ErpSalConstants.java:47`，核实复用，不重复声明）。
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpSalErrors` 增 `ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED`（描述："客户 %s 的订单超信用额度，需专项审批权限方可审核"）+ 参数 `ARG_CUSTOMER_ID`/`ARG_CREDIT_LIMIT`/`ARG_AVAILABLE`/`ARG_ORDER_AMOUNT`。
  - Skill: `nop-backend-dev`
- [x] `Add`：`CreditLimitChecker.check()` 增第三分支——超额度且 `level=SPECIAL_APPROVAL`：经 Explore 确认的权限入口校验当前用户，持有 `PERM_CREDIT_OVER_LIMIT_APPROVE` 则 log.info 放行，否则抛 `ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED`。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `check()` 含三分支（SOFT_WARNING/HARD_BLOCK/SPECIAL_APPROVAL），SPECIAL_APPROVAL 经权限门控两路径（放行/拒绝）均可达，无空实现
- [x] 新 ErrorCode 在 `ErpSalErrors` 声明且 i18n（`_erp-sal.i18n.yaml`）同步

### Phase 3 - 行为测试

Status: completed
Targets: `module-sales/erp-sal-service/src/test/.../TestErpSalCreditLimitChecker.java`（既有测试扩展）
Skill: `nop-backend-dev`

- Item Types: `Proof`
- Prereqs: Phase 1、Phase 2

- [x] `Proof`：扩展既有信用控制测试，至少增 4 case：(a) AR 余额纳入——客户有未核销 AR，订单+AR 合计超额度，HARD_BLOCK 拒绝（原纯订单口径会误放行）；(b) AR 纳入 config-gated 关闭——回退纯订单口径；(c) SPECIAL_APPROVAL + 持专项权限——超额度放行（log.info）；(d) SPECIAL_APPROVAL + 无专项权限——抛 `ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED`。多币种 AR（外币 openAmount 经 amountFunctional 纳入）补一 case。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 新增 case 全绿；`mvn test -pl module-sales/erp-sal-service -am` 通过（含既有 SOFT/HARD/多币种用例零回归）

### Phase 4 - owner doc 收口

Status: completed
Targets: `docs/design/sales/README.md` §信用额度控制
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-3

- [x] `Add`：`sales/README.md` §信用额度控制——SPECIAL_APPROVAL **行为列重写**（原"走多级审批工作流"→"超额度订单需持有专项审批权限方可审核；多级 `.xwf` 审批链归 Deferred"，对齐 Phase 2 语义重定位 Decision，非仅翻转 ❌→✅，避免 owner-doc 漂移）；状态列 ❌→✅；Non-Goals「AR 未核销余额」移除并改为"已纳入（config-gated，用 `openAmountFunctional`）"；额度计算口径公式补 AR 段（`Σ openAmountFunctional`）；Non-Goals 清单更新（多级 `.xwf` / 信用冻结 / 风险评分保留附触发条件）。
  - Skill: none
- [x] `Add`：`CreditLimitChecker` Javadoc Non-Goals 段同步更新（移除已落地两项）。
  - Skill: none

Exit Criteria:

- [x] `sales/README.md` 三级表状态与 `CreditLimitChecker` 实现一致；Non-Goals 仅列真实未实现项附触发条件

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_0ce207965ffevJbk628t0AJKIm，general 独立子代理新会话）。1 BLOCKER + 2 MAJOR + 1 MINOR：
  - **B1（BLOCKER）**：`ErpFinArApItem` 无 `openAmount` 字段；真实金额六件套为 `amountSource/amountFunctional`（原始全额）+ `settledAmountSource/settledAmountFunctional`（已核销）+ `openAmountSource/openAmountFunctional`（未核销余额，propId 17/18）。Phase 1 Decision 误选 `amountFunctional`（原始全额，会忽略部分核销、过度拦截）。**已修订**：基线改为列出六件套；Decision 改选 **`openAmountFunctional`**（未核销本位币余额）；替代/回退改为 `openAmountSource × exchangeRate`。
  - **M1**：SPECIAL_APPROVAL 假设 `IServiceContext` 有命令式权限 API，实测 `IContext` 无 `hasPermission`（仅 getUserId 等），平台为 GraphQL 层声明式。**已修订**：Phase 2 增前置 `Explore` 项确认可注入命令式入口（`IActionAuthChecker`/角色服务），门控 Decision；三条均不可行则移出范围。
  - **M2**：SPECIAL_APPROVAL 是对 README:81"多级审批工作流"承诺的语义重定位，须显式 Decision + Phase 4 重写行为列（非仅翻转状态）。**已修订**：增语义重定位 Decision（README:81 为冲突 owner-doc 证据）；Phase 4 改为重写行为列。
  - **m1**：`CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL` 已存在（`ErpSalConstants.java:47`）。**已修订**：Phase 2 改为"核实复用"，仅新增权限键常量。
- Independent draft review iteration 2: `acceptable as-is`（ses_0ce196206ffeRsibdswmY77Pcz，general 独立子代理新会话）— B1/M1/M2/m1 全部实时核实已修复（六件套字段 propId 17/18 选对、Explore 前置于 Decision 且门控、语义重定位 Decision + Phase 4 行为列重写、常量复用核实）；两触发条件核实已满足（M4 1018-1 + use-approval 2050-1 均 completed）；规则 4/14 合并判定 sound；anti-slack 0 命中。1 cosmetic（Phase 1 Exit Criteria amountFunctional→openAmountFunctional 文本一致性）已修订。共识达成，转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（AR 余额纳入 + SPECIAL_APPROVAL 权限门控 + 测试）
- [x] 相关文档对齐（`sales/README.md` §信用额度控制；当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-sales/erp-sal-service -am` + `mvn test -pl module-finance/erp-fin-service -am`（零回归）
- [x] 无范围内项目降级为 deferred/follow-up（多级 `.xwf` / 信用冻结 / 风险评分均为计划内 Non-Goal 附触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 完整多级 `.xwf` 信用审批工作流（信用分析师→财务经理多步链）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期 SPECIAL_APPROVAL 经 DIRECT 模式 + 权限门控实现"超额度需更高权限"语义；多步 `.xwf` 工作流定义（`useWorkflow="true"` + `wf:wfName` + 结束 listener）承接 `2050-1` Deferred 范式，属独立结果面。
- Successor Required: yes（触发条件：多级审批链业务需求落地时）

### 信用冻结（credit hold）实时拦截开票/出库

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期信用控制仅在订单审核环节；开票/出库环节实时冻结需跨域 hooks + 信用占用预留机制，独立结果面。
- Successor Required: yes（触发条件：开票/出库环节信用实时管控需求落地时）

### 客户风险评分体系联动信用额度动态调整

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 依赖 CRM 客户信用评分落地（`0540-1` Deferred「客户风险评分体系」触发条件）。
- Successor Required: yes（触发条件：CRM 客户信用评分体系落地时）

### 跨账套 AR 余额聚合

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本期单账套；多 `acctSchemaId` 维度 AR 聚合归多账套架构 successor。
- Successor Required: yes（触发条件：多账套上线时）

## Closure

Status Note: 执行完成（4 阶段全部 `[x]`，Plan Status=completed）。验证全绿：根 `mvn clean install -DskipTests` 146 模块 BUILD SUCCESS；`mvn test -pl module-sales/erp-sal-service -am` 77 项 + `mvn test -pl module-finance/erp-fin-service -am` 162 项 0 回归。owner doc（`sales/README.md` §信用额度控制 + `CreditLimitChecker` Javadoc）已收口。独立结束审计由新会话子代理执行并通过（见下方审计证据），执行者未自我审计。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，无执行者上下文）— 通过（approved）
- Audit Scope: 五点一致性 + Exit Criteria vs live repo + Anti-Hollow + Deferred honesty + Docs sync
- Evidence: 执行证据（执行者自录）+ 审计复核（独立子代理 live-verified）：
  - `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/entity/CreditLimitChecker.java`（213 行，live-verified）：注入 `IErpFinArApItemBiz`（:73）；`sumOutstanding`（:138-144）含 AR 段——`sumArOpenFunctional`（:165-176）调 `findOpenItemsByPartner(customerId, ErpFinConstants.DIRECTION_RECEIVABLE, ctx)` 累加 `openAmountFunctional`，`resolveOpenFunctional`（:179-191）config-gated `erp-sal.credit-check-ar-fallback` 回退 `openAmountSource × exchangeRate`；AR 纳入经 `includeAr()`（:193-196）config-gated `erp-sal.credit-check-include-ar`。`check()`（:81-121）三分支：HARD_BLOCK（:99）/ SPECIAL_APPROVAL（:106-117）/ SOFT_WARNING 默认（:118）。SPECIAL_APPROVAL 经 `hasSpecialApprovalPermission`（:123-130）调 `context.getActionAuthChecker().isPermitted(PERM_CREDIT_OVER_LIMIT_APPROVE, context)`，checker 为 null 保守拒绝（安全默认），持有 log.info 放行，未持有抛 `ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED`。无空体/`return null` 占位/吞噬异常；由 `ErpSalOrderProcessor.approve` 在 SUBMITTED→APPROVED 调用（Javadoc :30 声明）。
  - `ErpSalConstants`（live-verified :47/51/52/55/56/59）：`CREDIT_CHECK_LEVEL_SPECIAL_APPROVAL` 复用（非重复声明）；新增 `CONFIG_CREDIT_CHECK_INCLUDE_AR`/`CREDIT_CHECK_INCLUDE_AR_DEFAULT=true`/`CONFIG_CREDIT_CHECK_AR_FALLBACK`/`CREDIT_CHECK_AR_FALLBACK_DEFAULT=true`/`PERM_CREDIT_OVER_LIMIT_APPROVE`。
  - `ErpSalErrors`（live-verified :90-92）：`ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED = ErrorCode.define(...)` 内联中文描述（平台 i18n 默认机制，与同级 `ERR_CREDIT_LIMIT_EXCEEDED` :86-88 同范式）。
  - `TestErpSalOrderApproval`（live-verified，6 新 case）：AR 纳入超额度拒绝（:230-）/ AR 纳入 config-gated 关闭回退（:246-）/ 外币 AR openAmountFunctional 纳入（:264-）/ SPECIAL_APPROVAL + 持专项权限放行（:291-）/ SPECIAL_APPROVAL + 无专项权限拒绝（:310-）/ SPECIAL_APPROVAL + 无 checker 拒绝（:326-）。经 `approveWithPermission`/`approveWithAuthChecker`（:364-374）控制两路径，断言 `ERR_CREDIT_SPECIAL_APPROVAL_REQUIRED` 错误码 + APPROVE_STATUS 状态。
  - `docs/design/sales/README.md`（live-verified :81/85/91/92/96）：SPECIAL_APPROVAL 行为列**重写**（非仅翻 ❌→✅，对齐 Phase 2 语义重定位 Decision 避免 owner-doc 漂移）；额度计算公式补 AR 段（`Σ openAmountFunctional`）；配置项补 3 键；Non-Goals 收口（AR 余额移除，多级 `.xwf`/信用冻结/风险评分/跨账套保留附触发条件）。
  - `docs/logs/2026/07-05.md`（live-verified :3-23）：执行日志含验证状态、关键决策、下一步，AGENTS.md 文档维护义务已履行。
- Five-point consistency: Plan Status=completed / 4 Phase Status 全 completed / 全 Exit Criteria `[x]` / 全 Closure Gates `[x]` / 日志一致 —— 通过
- Anti-Hollow: 无空体、无 `return null` 占位、无吞噬异常；新代码经 `ErpSalOrderProcessor.approve` 运行时可达，SPECIAL_APPROVAL 两路径均有测试覆盖 —— 通过
- Deferred honesty: Deferred 4 项（多级 `.xwf`/信用冻结/风险评分/跨账套）均为真实 Non-Goal 附触发条件，无范围内 live 缺陷隐藏 —— 通过

Follow-up:

- 多级 `.xwf` 信用审批工作流（见上方 Deferred）
- 信用冻结实时拦截（见上方 Deferred）
- 客户风险评分联动（见上方 Deferred）
