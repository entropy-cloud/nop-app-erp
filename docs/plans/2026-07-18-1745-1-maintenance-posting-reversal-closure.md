# 2026-07-18-1745-1-maintenance-posting-reversal-closure maintenance 域过账红冲闭环

> Plan Status: completed
> Last Reviewed: 2026-07-18
> Mission: erp
> Work Item: maintenance-posting-reversal-closure
> Source: 承接 `docs/plans/2026-07-18-0949-1-maintenance-labor-cost-posting.md` Deferred「cancel visit 触发已生成 ML 凭证红冲」（Successor Required: yes，触发条件「cancel visit 业务流程须回滚已生成凭证时」——本计划即此后端 successor）+ `docs/plans/2026-07-10-1100-6` Non-Goal「备件消耗 cancel 红冲」（同型 successor）；跨域过账红冲缺口系统性审计见 `docs/plans/2026-07-18-1745-2` §Current Baseline 引用。
> Related: `2026-07-18-0949-1`（维修工时费用化过账，已 completed）、`2026-07-10-1100-6`（备件消耗过账，已 completed）、`2026-07-14-1825-1`/`1934-1`（制造委外红冲范式参照）
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-18），maintenance 域业财过账**正向链路已完整**，**反向红冲链路完全缺失**：

### 正向链路（已落地）

- **维修工时费用化**：`ErpMntVisitBizModel.doComplete`（`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/entity/ErpMntVisitBizModel.java:162`）在 `complete` 后经 `laborPostingDispatcher.postLabor(visit, context)`（`:177`）生成 `MAINTENANCE_LABOR(493)` 凭证（Dr 6602 折旧费用 / Cr 2211 应付职工薪酬），config-gated `erp-mnt.labor-posting-enabled`（plan 0949-1 落地）。
- **备件消耗费用化**：`ErpMntSparePartUsageBizModel.confirm`（`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/entity/ErpMntSparePartUsageBizModel.java`）经 `issuePostingDispatcher.dispatchIfApplicable` 生成 `MAINTENANCE_ISSUE(492)` 凭证（Dr 6602 / Cr 1403 存货）+ OUTGOING 库存移动（plan 1100-6 落地）。

### 反向链路（缺失——本计划范围）

- **`ErpMntVisitBizModel.doCancel`（`:183-186`）仅翻 `status=CANCELLED` + 恢复设备状态，不红冲 `MAINTENANCE_LABOR` 凭证**——confirm 产生的工时凭证成为孤儿。
- **`ErpMntSparePartUsageBizModel` 无 cancel/void/reverse 方法**——confirm 产生的 `MAINTENANCE_ISSUE` 凭证 + OUTGOING 库存移动无任何回滚入口。

### 既有红冲基础设施（可复用）

- `MntPostingExecutor`（`module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MntPostingExecutor.java`）仅有 `postEvent`，**缺 `reverse`**。
- 范式参照：`MfgPostingExecutor.reverse(billHeadCode, businessType)`（`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/posting/MfgPostingExecutor.java:35`）→ `voucherBiz.reverse(billHeadCode, businessType, context)`；`ErpMfgSubcontractOrderProcessor.reverseCompletion`（`:233`）循环调用 `mfgPostingExecutor.reverse` 红冲多业务类型凭证 + `stockMoveBiz.reverse` 红冲库存移动。
- 库存移动红冲：`IErpInvStockMoveBiz.reverse(moveId, context)` 生成 REVERSAL 反向移动单（已由 1934-1 委外红冲 E2E 验证可用）。
- 凭证红冲：`IErpFinVoucherBiz.reverse(billHeadCode, businessType, context)` 生成红字凭证 + 原凭证 `isReversed=true`（platform 内置幂等守护，同 billHeadCode+businessType 二次调用安全）。

### 剩余差距

maintenance 域两个过账实体（Visit / SparePartUsage）正向已过账但反向无红冲入口，构成业财过账闭环缺口。

## Goals

- `ErpMntVisit.cancel` 在已生成 `MAINTENANCE_LABOR` 凭证时红冲该凭证（原凭证 `isReversed=true` + 红字凭证同向取负）。
- `ErpMntSparePartUsage` 新增 reverse 入口（`reverseConfirm` `@BizMutation`），在已生成 `MAINTENANCE_ISSUE` 凭证 + OUTGOING 移动时红冲两者。
- `MntPostingExecutor` 补齐 `reverse(billHeadCode, businessType)` 对齐 `MfgPostingExecutor` 范式。
- 浏览器层 E2E 断言红冲产物（凭证行同向取负 + 原凭证 `isReversed` + 库存移动 REVERSAL 反向）。

## Non-Goals

- **不新增 cancel 入口语义变化**——`ErpMntVisit.cancel` 既有状态机守卫（`validateNotTerminal`）不变；红冲仅在已过账时触发，未过账 cancel 行为零回归。
- **不改 ORM/契约/字典/种子**——纯应用层 Java（executor 方法 + dispatcher 方法 + BizModel 触发点）+ bean 注册 + 测试。
- **不实现设备级/模板级/员工级工时费率**（0949-1 Deferred，ORM 保护区域 successor）。
- **不实现 cancel SparePartUsage 时备件库存余额恢复的复杂场景**（如已被后续消耗的链式恢复）——经 `IErpInvStockMoveBiz.reverse` 走标准 REVERSAL 反向移动单，由库存域成本引擎处理（对齐 1934-1 委外红冲范式）。
- **不做 cancel 后重新 complete 的幂等链路测试**（cancel 后 visit 已终态 CANCELLED 不可再 complete，状态机守卫已覆盖）。
- **不覆盖其它域的同型红冲缺口**（inventory 到岸成本 / manufacturing 领料 / finance 坏账等归 `2026-07-18-1745-2`/`1745-3`）。

## Task Route

- Type: `implementation-only change`（应用层 Java + 测试，无 ORM/契约变更）
- Owner Docs: `docs/design/maintenance/state-machine.md`（§维修费用过账行已记录正向链路 + 1100-6/0949-1 实现注记）、`docs/design/maintenance/use-cases.md`（UC-MAIN-04 备件消耗闭环）、`docs/design/finance/posting.md`（冲销机制）、`docs/design/finance/costing-methods.md §FIFO 红冲`（库存反向移动单范式）
- Skill Selection Basis: 涉及 BizModel 新 `@BizMutation` 方法 + 跨域 `IErpFinVoucherBiz.reverse` / `IErpInvStockMoveBiz.reverse` 调用 + dispatcher/executor 扩展 → 加载 `nop-backend-dev` skill（I*Biz injection 模式 + protected step 方法 + 跨实体调用硬规则）
- Protected Areas: 无 ORM/契约/数据删除；业财过账为 config-gated 默认关，红冲仅在正向已开启时触发，向后兼容。

## Infrastructure And Config Prereqs

- 无新基础设施；复用既有 `erp-mnt.labor-posting-enabled` / `erp-mnt.spare-part-posting-enabled` config 开关——红冲逻辑仅在对应正向过账已开启时有意义，无需新 config。
- 浏览器层 E2E 经 `playwright.config.ts` 既有 webServer JVM args（已含 `-Derp-mnt.labor-posting-enabled=true` + `-Derp-mnt.spare-part-posting-enabled=true`，见 e2e-runbook）。

## Execution Plan

### Phase 1 — `MntPostingExecutor.reverse` + 两 dispatcher `reverse` 方法

Status: completed
Targets:
  - `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MntPostingExecutor.java`
  - `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceLaborPostingDispatcher.java`
  - `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/posting/MaintenanceIssuePostingDispatcher.java`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: none

- [x] `MntPostingExecutor` 新增 `reverse(String billHeadCode, ErpFinBusinessType businessType)`，镜像 `MfgPostingExecutor.reverse:35`（`voucherBiz.reverse(billHeadCode, businessType, context)` + `IServiceContext.getCtx()` 兜底）
- [x] `MaintenanceLaborPostingDispatcher` 新增 `reverseLabor(ErpMntVisit visit)`（构造 `billHeadCode = visit.code + "-ML"` 对齐正向 `postLabor` 的 billHeadCode + 调 `executor.reverse(billHeadCode, MAINTENANCE_LABOR)`）；幂等守护经 platform `IErpFinVoucherBiz.reverse` 内置（无凭证时安全 no-op）
- [x] `MaintenanceIssuePostingDispatcher` 新增 `reverseIssue(ErpMntSparePartUsage usage)`（`billHeadCode = usage.code + "-MI"` 对齐正向 `dispatchIfApplicable` + 调 `executor.reverse(billHeadCode, MAINTENANCE_ISSUE)`）

> 接口契约：`reverse(billHeadCode, businessType)` 为跨域红冲统一入口；dispatcher 层 `reverseXxx(entity)` 负责从实体解析 billHeadCode（与正向 `buildBillCode` 对称）。**billHeadCode 派生规则已由独立草案审查核实（HEAD 2026-07-18）：`MaintenanceLaborPostingDispatcher` 正向 = `visit.code + "-ML"`（`MaintenanceLaborPostingDispatcher.java:102`，无 millis/uuid 后缀）；`MaintenanceIssuePostingDispatcher` 正向 = `usage.code + "-MI"`（`MaintenanceIssuePostingDispatcher.java:93/129`，无后缀）。reverse 直接重拼 `entity.code + "-ML"/"-MI"` 即可，无须经 `ErpFinVoucherBillR` 反查。**

Exit Criteria:

- [x] `MntPostingExecutor.reverse` 编译通过且签名与 `MfgPostingExecutor.reverse` 一致
- [x] 两 dispatcher `reverseXxx` 方法 billHeadCode 与正向对称（经核对正向 `buildBillCode` 确认）

### Phase 2 — BizModel 触发点接线

Status: completed
Targets:
  - `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/entity/ErpMntVisitBizModel.java`
  - `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/entity/ErpMntSparePartUsageBizModel.java`
  - `module-maintenance/erp-mnt-service/src/main/java/app/erp/mnt/service/IErpMntSparePartUsageBiz.java`（接口声明）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `ErpMntVisitBizModel.doCancel`（`:183`）在 `updateEntity` 后增条件红冲：`if (laborPostingDispatcher.isPostingEnabled()) laborPostingDispatcher.reverseLabor(visit);`（对齐 `doComplete` 的 config-gated 触发范式；红冲失败吞异常记日志不阻断 cancel 终态，对齐 `postLabor` 失败语义）
- [x] `ErpMntSparePartUsageBizModel` 新增 `reverseConfirm(@Name("usageId") Long, IServiceContext)` `@BizMutation`：守卫 `posted=true` 且 `docStatus=ACTIVE`（已 confirm 态）→ 调 `issuePostingDispatcher.reverseIssue(usage)` 红冲 `MAINTENANCE_ISSUE` 凭证 → 调库存域 `IErpInvStockMoveBiz.reverse(moveId, context)` 红冲 OUTGOING 移动单（moveId 经 `ErpFinVoucherBillR` 或 `relatedBillCode=usage.code` 反查）→ 翻 `usage.posted=false` + `docStatus=CANCELLED`；接口声明加入 `IErpMntSparePartUsageBiz`
- [x] 守卫：未过账（`posted=false`）调用 `reverseConfirm` 抛 `ERR_SPARE_PART_USAGE_NOT_POSTED`（新增 ErrorCode）；非法态迁移守卫对齐既有 confirm 状态机

> 触发点接线遵循 protected step 方法范式：`doCancel` 为既有 protected 方法，红冲作为其内联步骤（对齐 `doComplete` 内嵌 `postLabor`）。`reverseConfirm` 为新公开 `@BizMutation`，浏览器层经 GraphQL `ErpMntSparePartUsage__reverseConfirm` 可达。

Exit Criteria:

- [x] `ErpMntVisit.cancel` 在已过账 visit 上触发 `MAINTENANCE_LABOR` 凭证红冲（未过账 visit cancel 行为零回归）
- [x] `ErpMntSparePartUsage__reverseConfirm` GraphQL 端点可达，红冲 `MAINTENANCE_ISSUE` 凭证 + OUTGOING 移动单 + 翻 `posted=false`
- [x] `module-maintenance/erp-mnt-service` JUnit 编译通过（既有测试无回归）

### Phase 3 — JUnit + 浏览器层 E2E

Status: completed
Targets:
  - `module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntVisitCancelReversal.java`（新建）
  - `module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/TestErpMntSparePartUsageReversal.java`（新建）
  - `tests/e2e/business-actions/mnt-visit-cancel-reversal.action.spec.ts`（新建）
  - `tests/e2e/business-actions/mnt-spare-part-usage-reversal.action.spec.ts`（新建）
  - `docs/testing/e2e-runbook.md`（业务动作表 +2 行）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] `TestErpMntVisitCancelReversal`：complete 产 ML 凭证 → cancel → 原凭证 `isReversed=true` + 红字凭证行同向取负（Dr 6602=-X/Cr 2211=-X）+ config 关闭时 cancel 零红冲（向后兼容）
- [x] `TestErpMntSparePartUsageReversal`：confirm 产 `MAINTENANCE_ISSUE` 凭证 + OUTGOING 移动 → reverseConfirm → 凭证红冲 + REVERSAL 移动单生成 + `usage.posted=false`/`docStatus=CANCELLED` + 未过账守卫
- [x] E2E `mnt-visit-cancel-reversal`：复用 0949-1 既有 `runMfgChain`-同形 setup（建 Equipment+Visit 隔离）→ complete → cancel → `findVoucherIdByBillCode(code+'-ML','REVERSAL')` + `assertVoucherLines` 同向取负 + 原凭证 `isReversed=true` 经 `__get` 断言
- [x] E2E `mnt-spare-part-usage-reversal`：自包含建测试专用备件物料+INCOMING 备货+Usage+Line → `confirm` → `reverseConfirm` mutation → 凭证红冲 + REVERSAL 移动单断言 + `posted=false`/`docStatus=CANCELLED` 经 `verifyState`
- [x] e2e-runbook 业务动作表 +2 maintenance 红冲行 + 套件计数更新

Exit Criteria:

- [x] 两 JUnit 类全绿（红绿反转证明：注释掉 reverse 调用则 `isReversed` 断言红）
- [x] 两 E2E spec 全绿，断言红字凭证行精确数值 + 原凭证 `isReversed` + REVERSAL 移动单

## Draft Review Record

- Independent draft review iteration 1: `accept`（independent-draft-review-session-1）because 全部 Current Baseline 主张经实时仓库核实准确：(1) `ErpMntVisitBizModel.doCancel`（`:183-186`）确实仅翻 status + 恢复设备状态，不红冲 ML 凭证；(2) `MntPostingExecutor`（33 行）仅有 `postEvent`，无 `reverse`；(3) `ErpMntSparePartUsageBizModel` 仅有 `confirm`，无 cancel/reverse；(4) `MfgPostingExecutor.reverse`（`:35`）存在作为范式参照。Item Types 在阶段级正确声明（Phase 1/2 `Add`，Phase 3 `Add | Proof`，均符合规则 7 80%+ 阈值）；Skills 每阶段刻意记录（`nop-backend-dev` / `nop-testing`，符合规则 8）；Closure Gates 完整（规则 10/3）；Deferred But Adjudicated 三项均带 Classification + 触发条件（规则 11）。范围无过度拆分（规则 4/14：maintenance 域单结果面 + 两实体同型红冲）。**已修订一处**：Phase 1 blockquote 中关于 billHeadCode 拼接规则的"Phase 1 Explore"提示，经实时核实两 dispatcher 正向 billHeadCode 均无 millis/uuid 后缀（`visit.code + "-ML"` / `usage.code + "-MI"`），将 Explore 提示改为已核实事实，避免执行者浪费探索周期。可直接 flip 到 `active`。

## Closure Gates

- [x] 范围内行为完成（Visit.cancel 红冲 ML 凭证 + SparePartUsage.reverseConfirm 红冲 ISSUE 凭证+移动单）
- [x] 相关文档对齐（`docs/design/maintenance/state-machine.md` §维修费用过账补红冲实现注记 + e2e-runbook + `docs/logs/2026/07-18.md`）
- [x] 已运行验证：`mvn test -pl module-maintenance/erp-mnt-service -am` 全绿 + 154 模块 `mvn clean install -DskipTests` 全绿 + 新 E2E spec 全绿
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 设备级/模板级/员工级工时费率物化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 0949-1 已裁定属 ORM 保护区域 successor；本计划复用 config 全局费率红冲，费率来源不影响红冲闭环正确性。
- Successor Required: `yes`（触发条件：产品要求设备级精确费率时）

### cancel 后重新生成 visit 的幂等链

- Classification: `watch-only residual`
- Why Not Blocking Closure: cancel 后 visit 已终态 CANCELLED，状态机守卫禁止再 complete；幂等链路为操作场景变体非闭环缺口。
- Successor Required: `no`

### SparePartUsage 多次累计/链式备件消耗恢复

- Classification: `watch-only residual`
- Why Not Blocking Closure: 经 `IErpInvStockMoveBiz.reverse` 走标准 REVERSAL 反向移动单，库存域成本引擎处理链式恢复（对齐 1934-1 范式）；复杂链式场景由库存域覆盖。
- Successor Required: `no`

## Closure

Status Note: 全部 3 Phase 落地完成。Phase 1 `MntPostingExecutor.reverse` + 两 dispatcher `reverseLabor`/`reverseIssue` 方法（billHeadCode 与正向对称：`visit.code+"-ML"` / `usage.code+"-MI"`）；Phase 2 `ErpMntVisitBizModel.doCancel` 内嵌红冲触发 + `ErpMntSparePartUsageBizModel.reverseConfirm` 新 `@BizMutation` + 接口声明 + `ERR_SPARE_PART_USAGE_NOT_POSTED` 守卫；Phase 3 两 JUnit 类（5 用例）+ 两 E2E spec（3 用例）+ e2e-runbook 业务动作表 +2 行 + 套件计数 75→77。验证：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test -pl module-maintenance/erp-mnt-service` 54 tests 0 failures/0 errors（既有 49 + 新增 5，0 回归）。解除 0949-1/1100-6 两处 Deferred「cancel 触发已生成 ML/MI 凭证红冲」。设计文档 `docs/design/maintenance/state-machine.md:163` 已补红冲闭环实现注记，日志 `docs/logs/2026/07-18.md` 已添加 1745-1 条目。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理冷审计（fresh session，ses_08b9f7f58ffeP2RTEburhZCo23，2026-07-18）APPROVED — 全部 Phase 1/2/3 deliverables 经实时仓库核实
- Evidence (Phase 1): `MntPostingExecutor.reverse:38-44` 镜像 `MfgPostingExecutor.reverse`；`MaintenanceLaborPostingDispatcher.reverseLabor:146-152` billHeadCode `visit.code+"-ML"` 与正向 `postLabor:102` 对称；`MaintenanceIssuePostingDispatcher.reverseIssue:238-244` billHeadCode `usage.code+"-MI"` 与正向 `dispatchIfApplicable:93/129` 对称
- Evidence (Phase 2): `ErpMntVisitBizModel.doCancel:183-201` config-gate + try/catch 吞异常与 `doComplete:176` 同范式；`ErpMntSparePartUsageBizModel.reverseConfirm:75-116` + `validateCanReverse:150-157` 守卫 + `doReverseConfirm:180-184` 状态翻转；`IErpMntSparePartUsageBiz.reverseConfirm:30-31` 接口声明；`ErpMntErrors.ERR_SPARE_PART_USAGE_NOT_POSTED:79-81`
- Evidence (Phase 3): 两 JUnit 类 5 用例 + 两 E2E spec 3 用例 + e2e-runbook:305/306/412 套件计数全绿
- Evidence (anti-pattern scan): 反模式扫描全清（无 `@Inject private` / `@BizMutation @Transactional` / `dao().saveEntity()` / `new RuntimeException` / 缺 `IServiceContext` 参数）
- Evidence (verification): `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test -pl module-maintenance/erp-mnt-service` 54 tests 0 failures/0 errors + 两 E2E spec 全绿
