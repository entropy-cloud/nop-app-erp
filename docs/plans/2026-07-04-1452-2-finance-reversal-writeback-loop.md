# 2026-07-04-1452-2-finance-reversal-writeback-loop 冲销反写闭环

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: `docs/backlog/core-business-roadmap.md` M5 工作项 5.2（P0）；`docs/design/finance/posting.md` §冲销机制 方向二（设计 done）；`docs/analysis/2026-07-04-finance-posting-engine-gap-vs-opensource.md` §2.3（真实缺口：发票红冲→源单状态不回退）
> Related: `2026-07-04-1452-1-finance-posting-log-observability.md`（提供 traceId + 告警队列载体，前置）、`2026-07-01-0811-1-finance-posting-engine-foundation.md`（reverse() 基线，已完成）
> Audit: required

## Current Baseline

- 冲销方向一（业务侧驱动）已实现并生效：业务域作废/反审核时，先回退自身 `docStatus`，再调 `IErpFinVoucherBiz.reverse(billHeadCode, businessType)` 红冲凭证，随后域**自行置 `posted=false`**（如 `ErpPurInvoiceProcessor.reverseApprove()` `module-purchase/erp-pur-service/.../processor/ErpPurInvoiceProcessor.java:85-101` 调 `postingDispatcher.reverse` 后 `setPosted(false)`；同型模式见 PurPayment/PurReturn/PurOrder/PurReceive、SalInvoice 等）。
- 冲销方向二（财务侧驱动，本计划范围）**完全缺失**：财务员直接红冲已过账凭证时，`ErpFinPostingProcessor.reverseProcess()`（`:112-131`）仅生成红字凭证 + `arApItemGenerator.cancelOnReverse()`（**只回退 finance 辅助账 `ErpFinArApItem`，不触及源业务单据**，`ErpFinArApItemGenerator.java:112-123`）。源单 `posted` 与 `docStatus` 不回退 → 业财闭环断裂（典型案例：发票红冲后采购单仍处"已入账"，业务无法继续）。
- **无进程内事件总线**：nop-platform 无 `IEventBus`/`@EventListener`（grep 平台源码确认）；但事务回调 `txn().afterCommit(Runnable)` **存在**（`ITransactionTemplate.java:87`，runbook `transaction-boundaries.md:12`）。本仓 Java 源无任何 `afterCommit`/`publishEvent`/`EventListener` 使用。
- DAG 方向约束：财务域处于 DAG 顶层，**不可反向 import 业务域模块**。现有跨域模式为"finance 定义 SPI、业务域实现、finance 经 `@Inject List<...>` 聚合"（对标 `IErpFinAcctDocProvider`/`IErpFinFactsValidator`/`ErpFinAcctDocRegistry`）。
- `VoucherReversedEvent` 契约已在 `posting.md:355-366` 设计（字段：`voucherId`/`reversalOfVoucherId`/`billHeadCode`/`businessType`/`billType`/`traceId`；派发时机：红字凭证 post 提交后 post-commit，SYNC 模式可同事务同步通知）。
- `posted` 标志覆盖：purchase（Order/Receive/Invoice/Payment/Return）、sales（Order/Delivery/Invoice/Receipt/Return）、inventory（StockMove/TransferOrder/StockTake/OwnershipTransfer）。
- 反写哲学已裁定（`posting.md` §反写契约 / `gap-vs-opensource.md` §5）：引擎只持有事件快照不持有源实体；**不引入独立反写记录表**（4 大开源 ERP 均无）；反写语义由 `posted` 字段 + `ErpFinVoucherBillR` 业财回链承载。

## Goals

- 财务侧 `reverse()` 红字凭证过账成功后，发布 `VoucherReversedEvent`，业务域**经 finance 定义的 SPI 监听并自治回退**自身 `posted` + `docStatus`。
- 建立进程内监听者注册机制（finance 定义 `IErpFinVoucherReversedListener` SPI，业务域实现，finance `@Inject List` 聚合），不引入外部 MQ，不破坏 DAG 方向。
- 派发时序遵循 `posting.md`：默认 SYNC 同事务同步通知；ASYNC 模式 post-commit 经 `txn().afterCommit`。
- 回退失败处理：业务单据状态维持原状，已过账红字凭证不回滚（法律效力），失败落入 5.1 告警队列人工处理。
- 覆盖核心三域（purchase/sales/inventory）源单的回退监听器。

## Non-Goals

- 构建通用领域事件总线（平台无；本计划仅建 finance 范围 reversal 监听注册）。
- 外部 MQ（`nop-message-*`）采纳。
- 冲销审批工作流（开源共识不内建；Deferred）。
- 冲销独立 LOCKED 状态（iDempiere 用行级锁 + 乐观锁已足够；Deferred）。
- 资产/费用/票据/薪酬/物流等域的回退监听器（核心三域先行；其余域按需 Follow-up，SPI 已预留）。
- 反结账后的复合状态回退编排（红字凭证已走正常平衡/期间校验，状态语义由各域自决）。

## Task Route

- Type: `implementation-only change`（设计 done 于 `posting.md` §冲销机制方向二 + §反写契约），含派发机制 Decision。
- Owner Docs: `docs/design/finance/posting.md`（§冲销机制方向二权威）、`docs/design/finance/posting-log.md`（失败入告警队列）。
- Skill Selection Basis: finance 定义 SPI + `@Inject List` 聚合（对标既有 `IErpFinAcctDocProvider` 模式）+ 跨域经 I*Biz + `txn().afterCommit` + `ErrorCode` 守门 + 各域 Processor 回退自身状态，匹配 `nop-backend-dev`（SPI/跨实体/事务边界/ErrorCode/Processor）。

## Infrastructure And Config Prereqs

- 无新增端口/外部服务/密钥/.env。
- **告警队列载体自洽**：监听者回退失败是与"过账失败"不同的失败类别（不适用"重试 post()"），5.1 的异常工作台载体（PENDING/RETRYING/IGNORED/MANUAL，面向过账重试）不天然收纳监听者失败。本计划自带最小失败记录载体（源单类型+billHeadCode+ErrorCode+处置状态），并 config-gated：当 5.1 异常工作台存在且其载体经 Phase 1 Decision 扩展了失败类别判别字段时，路由并入 5.1 工作台统一处置；否则用本计划最小载体独立呈现。两者不产生歧义分叉。

## Execution Plan

### Phase 1 - Decision: 派发机制与 SPI 形状

Status: completed
Targets: `posting.md`（实现策略）、本计划
Skill: `nop-backend-dev`

- Item Types: `Decision | Explore`
- Prereqs: 5.1（traceId 字段）已 active 或本批次先行落地 traceId

- [x] Explore：核实 `IErpFinVoucherBiz.reverse()` Facade 事务边界（现 `@Transactional(REQUIRES_NEW)`，`FinPostingExecutor.java:14-16` 注释），确认 SYNC 同事务通知与 `txn().afterCommit` 在该边界的可用性；核实各域源单 `docStatus` 状态机当前"已过账"态名以确定回退目标态。
      - Skill: `nop-backend-dev`
- [x] Decision（派发机制）：选择"finance 定义 `IErpFinVoucherReversedListener` SPI + `@Inject List` 聚合 + 默认 SYNC 同事务同步通知、ASYNC post-commit afterCommit"。替代方案：外部 MQ（被拒：破坏 SYNC 强一致默认 + 引入 infra）；Spring ApplicationEvent（被拒：平台无该设施）。残留风险：监听者抛错隔离策略（见 Phase 3）记录。
      - Skill: `nop-backend-dev`
- [x] Decision（回退目标态）：逐域裁定"红冲→源单回退到哪个 docStatus"（如 posted invoice → 待开票/草稿态，各域按其状态机自决并在监听器内实现）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 派发机制 Decision + 各域回退目标态结论写入 `posting.md` 实现策略与本计划

### Phase 2 - VoucherReversedEvent + SPI + 发布接线

Status: completed
Targets: `IErpFinVoucherReversedListener`（新 SPI）、`VoucherReversedEvent`（新 DTO）、`ErpFinAcctDocRegistry`（或新 `ErpFinReversalListenerRegistry`）、`ErpFinPostingProcessor.reverseProcess()`、`IErpFinVoucherBiz` Facade
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [x] Add：finance 定义 `VoucherReversedEvent`（字段对齐 `posting.md:355-366` 契约）与 `IErpFinVoucherReversedListener`（`void onVoucherReversed(VoucherReversedEvent, IServiceContext)`）；聚合 bean 收集所有实现（对标 `ErpFinAcctDocRegistry` 收集 Provider 的模式）。
      - Skill: `nop-backend-dev`
- [x] Add：`reverseProcess()` 生成红字凭证并 `cancelOnReverse` 后，构造 `VoucherReversedEvent`（携带 traceId，来自 Phase 2 of 5.1）并按 Decision 派发——默认 SYNC 同事务遍历监听者同步通知；ASYNC 配置下经 `txn().afterCommit`。
      - Skill: `nop-backend-dev`
- [x] Proof：单元测试——注册 mock 监听者，断言 `reverse()` 后监听者收到事件且字段（voucherId/reversalOfVoucherId/billHeadCode/businessType/traceId）正确；无监听者时不报错。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `VoucherReversedEvent` + SPI + 聚合落地；`reverse()` 派发事件，mock 监听者收到正确字段，单测通过
- [x] finance service 改动包类型检查通过（解除 Phase 3 域监听接线阻塞）

### Phase 3 - 业务域回退监听器 + 失败隔离

Status: completed
Targets: purchase/sales/inventory 各域 `*Processor` 或新 `*ReversalListener`、`ErpFinVoucherBillR` 反查
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] Add：purchase/sales/inventory 各实现 `IErpFinVoucherReversedListener`——经 `ErpFinVoucherBillR` 反查源单（billType+billHeadCode），按 Phase 1 回退目标态回退源单 `docStatus` + 置 `posted=false`/`postedAt=null`/`postedBy=null`。复用各域既有 reverseApprove/cancel 中已验证的状态回退逻辑，不重复造。
      - Skill: `nop-backend-dev`
- [x] Add：失败隔离——单监听者抛 `NopException` 不中断其他监听者、不回滚已过账红字凭证；失败记录（源单类型+billHeadCode+ErrorCode）落入 5.1 告警队列（或本计划最小载体），状态供人工处置。
      - Skill: `nop-backend-dev`
- [x] Proof：集成测试——purchase 发票红冲 → 采购单 `posted=false` + docStatus 回退目标态；构造监听者抛错 → 红字凭证仍在、其他域监听仍执行、告警队列有记录；inventory/sales 同型断言。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 核心三域红冲后源单 `posted=false`+状态回退；监听者抛错隔离且红字凭证不回滚、告警队列留痕，集成测试通过

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（ses_0d4159e6cffenc8xLYYJKef3B0，general 独立子代理）because 基线准确（方向一已实现带文件引用、方向二缺口定位准确、无事件总线但 afterCommit 存在、DAG/SPI 约束正确）、派发机制 Decision 前置 Explore、无 ORM 保护区域工作故无需人工批准 prereq、Deferred 项均带触发条件。无阻塞。已采纳非阻塞改进：Phase 1 Item Types 修正为 `Decision | Explore`；告警队列载体自洽化（5.2 自带最小失败记录载体 + config-gated 路由并入 5.1 工作台，消除两载体分叉歧义）。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（VoucherReversedEvent + SPI + finance 发布 + 核心三域回退监听 + 失败隔离）
- [x] 相关文档对齐（`posting.md` §冲销方向二实现策略；`core-business-roadmap.md` 5.2 标进展；当日日志）
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service -am` + 受影响 purchase/sales/inventory service 测试
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 冲销审批工作流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 开源共识（iDempiere/Metasfresh/Odoo）均不内建冲销审批，靠权限控制；`posting.md:384` 已定位为可选增强。
- Successor Required: yes（触发条件：客户合规诉求要求双人审批红冲时）

### 冲销独立 LOCKED 状态

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: iDempiere 用行级锁 + 乐观锁（`docStatus` 状态约束 + 版本号）已防并发，无独立 LOCKED 态（`posting.md:383`）。
- Successor Required: yes（触发条件：高并发冲销出现乐观锁冲突频发时）

### 资产/费用/票据/薪酬/物流域回退监听器

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 核心三域（purchase/sales/inventory）覆盖 P0 业财闭环主路径；SPI 已预留，其余域监听为增量扩展。
- Successor Required: yes（触发条件：对应域财务侧红冲需回退源单时，按 SPI 增监听器）

## Closure

Status Note: 业财闭环方向二（财务侧红冲→业务单据回退）已实现并通过全仓验证。finance 定义 `IErpFinVoucherReversedListener` SPI + `ErpFinReversalListenerRegistry`（镜像既有 `ErpFinAcctDocRegistry` 范式），`ErpFinPostingProcessor.reverseProcess()` 在红字凭证+回链+辅助账落库后构造 `VoucherReversedEvent` 并按配置派发（默认 SYNC 同事务同步通知；ASYNC 经 `txn().afterCommit`）。失败隔离：派发循环 try/catch 包裹——单监听者抛错不阻断其他监听者、不回滚已过账红字凭证（法律效力），失败落入 5.1 异常工作台（`ErpFinPostingException`，postingType=REVERSAL，failedStage=`notify-reversal-listener`）。purchase/sales/inventory 三域各实现监听者（`PurReversalListener`/`SalReversalListener`/`InvReversalListener`），按裁决 4 回退目标态表回退自身 `posted`+`docStatus`/`approveStatus`。

Closure Audit Evidence:

- 实现：`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/posting/` 新增 `VoucherReversedEvent.java`、`IErpFinVoucherReversedListener.java`、`ErpFinReversalListenerRegistry.java`；`ErpFinPostingProcessor.reverseProcess()` 接线派发（`dispatchReversalEvent`+`recordListenerFailures`）；`ErpFinPostingErrors` 增 `ERR_REVERSAL_LISTENER_FAILED`；`ErpFinConstants` 增 `CONFIG_REVERSAL_DISPATCH_MODE`/`REVERSAL_DISPATCH_MODE_SYNC`/`REVERSAL_DISPATCH_MODE_ASYNC`/`FAILED_STAGE_NOTIFY_REVERSAL_LISTENER`；`app-service.beans.xml` 注册 `ErpFinReversalListenerRegistry` bean（`ioc:collect-beans` 收集监听者）。
- 业务域监听者：`module-purchase/.../posting/PurReversalListener.java`（AP_INVOICE/PAYMENT/PURCHASE_RETURN/PURCHASE_INPUT）、`module-sales/.../posting/SalReversalListener.java`（AR_INVOICE/RECEIPT/SALES_RETURN/SALES_OUTPUT）、`module-inventory/.../posting/InvReversalListener.java`（OWNERSHIP_TRANSFER/INTER_TRANSFER）；各域 `app-service.beans.xml` 注册监听者 bean。
- 设计裁决：`docs/design/finance/posting.md` §冲销机制方向二新增「实现策略」小节（裁决 3 派发机制 + 裁决 4 各域回退目标态表）。
- 验证基线（全绿 — full-green verification）：
  - `mvn clean install -DskipTests`（根 reactor）：BUILD SUCCESS
  - `mvn test`（全量 reactor）：所有模块测试通过，零失败零回归
  - 新增测试：`TestErpFinReversalListenerRegistry`（4 单元测试：空监听者空操作/事件字段透传/失败隔离不阻断其他监听者/addListener 追加）、`TestErpFinReversalDispatch`（3 集成测试：reverse() 派发事件含正确字段/无监听者不报错/失败监听者隔离+红字凭证保留+5.1 异常工作台留痕）、`TestErpPurFinanceReversalWriteback`（2 集成测试：财务红冲→采购发票 posted=false+REJECTED/源单不存在时监听者静默）、`TestErpSalFinanceReversalWriteback`（1 集成测试：财务红冲→销售发票 posted=false+REJECTED）、`TestErpInvFinanceReversalWriteback`（1 集成测试：财务红冲→所有权转移单 posted=false，docStatus 保留审计轨迹）。
- 结束审计：由独立子代理（新会话，closure-auditor，不重用执行者上下文）执行并通过。逐项核对：(1) 结构合规——front matter `completed`+`Last Reviewed`、三 Phase 均带 `Status: completed` 与 `Exit Criteria` 全 `[x]`、`## Closure` 含真实证据；(2) Phase 0 一致性——各 Phase body 无残留 `[ ]`；(3) Exit Criteria 对照实时仓库——`VoucherReversedEvent`/`IErpFinVoucherReversedListener`/`ErpFinReversalListenerRegistry`（finance posting 目录）+ `PurReversalListener`/`SalReversalListener`/`InvReversalListener`（三域 posting 目录）+ 5 个测试类均存在；`ErpFinPostingProcessor.reverseProcess()` 于 `dispatchReversalEvent`（:186-187）真实接线派发；`ErpFinPostingErrors.ERR_REVERSAL_LISTENER_FAILED`（:74）、`ErpFinConstants` 4 常量（:149-156）、4 份 `app-service.beans.xml` 注册齐备；(4) Anti-Hollow——`dispatchReversalEvent` 构造事件+SYNC/ASYNC 派发+失败落 5.1 工作台，`Registry.dispatch()` 真实 try/catch 隔离，`PurReversalListener.rollbackInvoice` 真实回退 posted/approveStatus，无空体/return-null/吞异常；(5) 五点一致；(6) Deferred 三项均为带触发条件的 out-of-scope improvement，无活缺陷隐藏；(7) 文档同步——`docs/logs/2026/07-04.md` 有本计划条目、`docs/design/finance/posting.md` §实现策略 裁决3/4 对齐。审计无阻塞，Closure Gates 全 `[x]`。

Follow-up:

- 冲销审批工作流（见上方 Deferred）
- 冲销 LOCKED 状态（见上方 Deferred）
- 其余业务域回退监听器（见上方 Deferred）
