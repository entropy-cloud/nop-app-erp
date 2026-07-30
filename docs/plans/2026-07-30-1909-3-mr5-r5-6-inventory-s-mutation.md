# 2026-07-30-1909-3-mr5-r5-6-inventory inventory 域 S-mutation 逻辑下沉

> Plan Status: active
> Last Reviewed: 2026-07-30
> Source: `docs/backlog/audit-remediation-roadmap.md` §Milestone MR5 工作项 R5.6
> Related: `docs/plans/2026-07-30-1433-1-mr5-r5-1-purchase-s-mutation.md`（pilot 配方）、`docs/plans/2026-07-30-1909-1-mr5-r5-4-assets-s-mutation.md`、`docs/plans/2026-07-30-1909-2-mr5-r5-5-mfg-s-mutation.md`、`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`（创建 per-mutation 文件）
> Audit: required

## Current Baseline

- plan 2026-07-25-1057-2 已为 inventory 域创建 7 个 per-mutation Processor 文件（2 实体），当前全部为**空心委托**：`@Inject ErpInv{CostAdjust,LandedCost}Processor processor`，public S-mutation 方法 `return processor.method(id, context)` 一行回委托；抽象基类 step override 为 `null`/`false`/空体（`// not reached: main method delegates to monolithic Processor`）。空心形状是蓄意脚手架。
- 2 个 facade Processor 持有全部真实编排逻辑，位于同包 `app.erp.inv.service.processor`：
  - `ErpInvCostAdjustProcessor`（5 S-mutation：submitForApproval/withdrawApproval/approve/reject/reverseApprove；+ D-mutation `applyCostAdjust`/`reverseCostAdjust`）
  - `ErpInvLandedCostProcessor`（仅 2 S-mutation：`approve(Long)`/`reverseApprove(Long)`——**Long 签名**，非基类 String 签名；**不实现** submitForApproval/reject/withdrawApproval，**匹配 roadmap 声明**；reverseApprove 含 GL 凭证冲销+成本层冲销+`posted=false`+`docStatus=CANCELLED`）
- **错误纪律：facade 100% NopException + ErpInvErrors ErrorCode**（`ERR_COST_ADJUST_*`、`ERR_LANDED_COST_*`、`ERR_ILLEGAL_STATUS_TRANSITION`），facade 内零 NopScriptError；xbiz 也零 NopScriptError。本 plan 是**纯 Java facade → per-mutation 迁移**，无 inline-script 提取。
- **xbiz `<source>` 分类（实测）**：
  - **CostAdjust：5 个 delegation 可达**——`ErpInvCostAdjust.xbiz` 5 个 S-mutation `<source>` = `inject('...Processor').method(...)` 委托 per-mutation（空心）→ facade。
  - **LandedCost：empty `<actions/>`（category c）**——`ErpInvLandedCost.xbiz` 为空；2 个 S-mutation 经 BizModel `@BizMutation` Java 调 facade **直接**，绕过 per-mutation 文件。2 个 `ErpInvLandedCost{Approve,ReverseApprove}Processor` 注册为 bean 但**无任何运行时调用方**——休眠。
- **BizModel**：
  - `ErpInvCostAdjustBizModel`——`@Inject ErpInvCostAdjustProcessor`（facade），声明 D-mutation（applyCostAdjust/reverseCostAdjust），**无 S-mutation `@BizMutation`**（5 个 S-mutation 全经 xbiz）。
  - `ErpInvLandedCostBizModel`——`@Inject ErpInvLandedCostProcessor`（facade），`@BizMutation approve()/reverseApprove()` **直接调 facade**，绕过 per-mutation。
- **可达性（实测）**：**5 CostAdjust 可达**（source-backed）；**2 LandedCost 休眠**（BizModel→facade 直接，per-mutation 无调用方）。
- **doReject/doReverseApprove 抽象骨架偏离风险（CRITICAL，会计保护区域）**：
  - 抽象基类 `doReverseApprove` 设 **SUBMITTED** + 清 approvedBy/approvedAt。
  - CostAdjust facade `reverseApprove` 设 **REJECTED**（非 SUBMITTED）、**不**清审计字段、含 `posted` 守卫（`posted=true` 则报错"先冲销再反审"）。
  - LandedCost facade `reverseApprove` 设 **REJECTED + `docStatus=CANCELLED` + `posted=false`** + **GL 凭证冲销**（try/catch + notify fallback）+ **成本层冲销**（`costAdjustmentService.reverseCostAdjust`）。
  - CostAdjust facade `reject` 设 REJECTED，**不**设/清 approvedBy/approvedAt（基类会设——偏离）。
  - Pattern B custom public override 绕过基类模板，custom override 精确保留 facade 语义（尤其 LandedCost 会计保护区域：GL 凭证 + 成本层冲销 + posted/docStatus 副作用）。
- **LandedCost Long 签名**：facade `approve(Long)`/`reverseApprove(Long)`，基类为 String——per-mutation 在调用边界 `Long.valueOf(id)` 转换（空心文件已含此转换，Pattern B 复刻时保留）。
- **wf:wfName**：CostAdjust / LandedCost xmeta 均空壳，零 `wf:wfName`——无工作流，DIRECT 审批范式。
- 同包，无跨包问题；facade helper 全 `protected`（step）+ `public`（S-mutation 入口）。StockMove/OwnershipTransfer 确认为纯 D-mutation（无 S-mutation per-mutation 文件），out-of-scope。
- inv 域既有测试全绿，作为行为等价基线。

## Goals

- inv 域全部 7 个 per-mutation Processor 各自自包含：Pattern B custom public override（1:1 复刻 facade 编排流，经 facade protected helper 单一真相源），不再空心回委托 facade（含 5 CostAdjust source-backed + 2 LandedCost 休眠）。
- 5 个 CostAdjust source-backed per-mutation 经 inv 域既有测试验证行为等价。
- 2 个 LandedCost 休眠 per-mutation 经静态 parity 校验确认保真（会计保护区域：GL 凭证冲销 + 成本层冲销 + posted/docStatus 副作用逐项对照），运行时验证显式移交 R5.8。
- 域特有约束保真：CostAdjust reverseApprove=REJECTED + posted 守卫、LandedCost reverseApprove=REJECTED+CANCELLED+posted=false+GL/成本层冲销、Long 签名边界。

## Non-Goals

- D-mutation（`applyCostAdjust`/`reverseCostAdjust`、`generateMove`/`confirm`/`complete`、`confirm`/`done` 等）保留在 facade——MR5 范围外（roadmap 明示）。
- BizModel 配线从 `@Inject` facade 改为 `@Inject` per-mutation + xbiz `<source>` 清理 + beans.xml——属 R5.8（roadmap 明示）。LandedCost BizModel `@BizMutation` 改为经 per-mutation 属 R5.8 重配线。
- 抽象骨架 doReject/doReverseApprove 默认行为修正——Pattern B 绕过基类模板（与 R5.1 一致）。
- `ErpInvStockMove`、`ErpInvOwnershipTransfer`（纯 D-mutation，无 S-mutation per-mutation 文件，实测确认）——不在 R5.6 范围。
- LandedCost 补 submitForApproval/reject/withdrawApproval——roadmap 明示 LandedCost 仅 approve+reverseApprove，facade 未实现这三个，不发明 mutation。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/inventory/state-machine.md`、`docs/design/inventory/`（成本调整/到岸成本 owner doc）、`docs/design/finance/costing-methods.md`（成本层冲销语义）、`docs/architecture/processor-extension-pattern.md`、`docs/analysis/per-mutation-processor-split-plan.md`
- Skill Selection Basis: 后端 Processor 重构匹配 `nop-backend-dev`（Processor 模式、protected step、跨实体、错误处理自检）。LandedCost reverseApprove 涉及会计保护区域（GL 凭证 + 成本层冲销），须对照 R1.12（成本方法缺陷修复）owner doc 静态校验语义不变。`nop-testing` 用于回归。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - CostAdjust source-backed 可达 per-mutation 填充（5 文件）

Status: planned
Targets: `module-inventory/erp-inv-service/.../processor/ErpInvCostAdjust*{SubmitForApproval,Approve,Reject,ReverseApprove,WithdrawApproval}Processor.java`、`ErpInvCostAdjustProcessor.java`（facade，读不改）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: MR1 done（已满足）；R5.1 共享 hook 策略裁决（候选 A）沿用

- [ ] Add: 5 个 CostAdjust per-mutation 填充——删除空心 `return processor.method(...)` 回委托，改为 Pattern B custom public override（1:1 复刻 facade 公共 S-mutation 方法编排流：`requireAdjustment → validateTransitionForXxx → posted 守卫 → doXxx → updateEntity`），域逻辑经 facade protected helper 调用（单一真相源）。
  - Skill: `nop-backend-dev`
  - 域特有保真点：
    - `reverseApprove`——custom override 设 **REJECTED**（非基类 SUBMITTED）、**不**清审计字段、保留 `posted=true` 守卫（报错"先冲销再反审"）。
    - `reject`——custom override 设 REJECTED，不设/清 approvedBy/approvedAt（基类会设——偏离，须 override 保留 facade 语义）。
- [ ] Proof: 5 文件本地编译通过（`mvn compile -pl module-inventory/erp-inv-service -am -DskipTests`）。
  - Skill: none

Exit Criteria:

> 本阶段交付 5 个 CostAdjust 可达 per-mutation 的自包含化（既有测试可验证）。

- [ ] 5 个 CostAdjust per-mutation 自包含（0 个空心回委托）
- [ ] 本地编译通过

### Phase 2 - LandedCost 休眠 per-mutation 填充（2 文件，会计保护区域）

Status: planned
Targets: `module-inventory/erp-inv-service/.../processor/ErpInvLandedCost{Approve,ReverseApprove}Processor.java`、`ErpInvLandedCostProcessor.java`（facade，读不改）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: 2 个 LandedCost 休眠 per-mutation 填充——Pattern B custom public override 复刻 facade `approve(Long)`/`reverseApprove(Long)` 编排流，**保留 Long 签名边界**（custom override 内 `Long.valueOf(id)` 转换——基类公共方法签名为 `String id`，仅此转换可行）。运行时经 BizModel→facade 旧路径，R5.8 重配线前不在 per-mutation 路径。
  - Skill: `nop-backend-dev`
- [ ] Proof: 静态 parity 校验——LandedCost reverseApprove 会计保护区域不变量逐项对照 facade（GL 凭证冲销方向/金额 + try/catch notify fallback 时序 + 成本层冲销 `reverseCostAdjust` + `posted=false` + `postedAt=now` + 关联 CostAdjust 实体同步 `posted=false`/`postedStatus=CANCELLED` + `docStatus=CANCELLED` + `approveStatus=REJECTED` + 不清审计字段），确认迁移仅改编排位置不改会计规则。逻辑全部经 facade helper（单一真相源），per-mutation 不复制会计规则。
  - Skill: `nop-backend-dev`
- [ ] Proof: 休眠 per-mutation 迁移**不破坏**既有测试（休眠文件不在运行时路径，既有测试走 BizModel→facade 旧路径，应全绿——证明迁移未引入编译/依赖回归）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 LandedCost 休眠 per-mutation 自包含化 + 会计保护区域静态 parity 证据（运行时验证移交 R5.8）。

- [ ] 2 个 LandedCost per-mutation 本地编译通过
- [ ] 静态 parity 校验通过（会计保护区域不变量逐项确认）
- [ ] 既有测试全绿（休眠文件迁移未引入回归）

### Phase 3 - inv 域行为等价回归

Status: planned
Targets: `module-inventory/erp-inv-service/src/test/`
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1 + Phase 2

- [ ] Proof: inv 域既有测试全绿——覆盖 5 个 CostAdjust 可达路径（迁移后行为等价）；快照漂移仅限 Processor 类名/堆栈变化，重录为新基线。
  - Skill: `nop-testing`
- [ ] Proof: 确认 LandedCost 休眠文件迁移不破坏既有测试（休眠文件不在运行时路径，既有测试走 BizModel→facade 旧路径全绿）。
  - Skill: `nop-testing`

Exit Criteria:

> 本阶段交付 inv 域迁移后行为等价的完整证据。

- [ ] inv 域 `mvn test -pl module-inventory/erp-inv-service -am` 全绿（含重录快照）
- [ ] LandedCost 休眠文件运行时验证缺口已显式移交 R5.8（在 Deferred 记录 successor）

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（task ses_04d46747bffeNg3IfUsPa8jbk3，新会话 fresh context，read-only）—全部基线声明经实时仓库验证精确（7 per-mutation 空心 + CostAdjust 5/LandedCost 仅 approve+reverseApprove + Long 签名 + doReverseApprove 三向偏离 + 5 reachable/2 dormant + LandedCost BizModel 直调 facade + 零 NopScriptError + StockMove/OwnershipTransfer 无 S-mutation 文件），0 blocking。Pattern B 视为 R5.4/R5.5 已建立的继承约束（非新决策）。已吸收非阻塞观察：Phase 2 删除不可行的"保留 Long 参数"选项（基类 String 签名）、parity 清单补 `postedAt=now` + 关联 CostAdjust 实体同步。

## Closure Gates

> 仅在所有项目和每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证（`mvn clean install -DskipTests` + `mvn test`）在 R5.8 统一执行；本 plan 仅跑 inv 域局部验证。

- [ ] inv 域 7 个 per-mutation Processor 自包含（无空心回委托；含 5 CostAdjust + 2 LandedCost）
- [ ] 5 个 CostAdjust per-mutation 经 inv 域 `mvn test` 行为等价验证
- [ ] 2 个 LandedCost 休眠 per-mutation 经静态 parity 校验确认保真（会计保护区域不变量逐项确认；运行时验证移交 R5.8）
- [ ] 域特有约束保真：CostAdjust reverseApprove=REJECTED+posted 守卫、LandedCost reverseApprove=REJECTED+CANCELLED+posted=false+GL/成本层冲销、Long 签名边界
- [ ] inv 域 `mvn test -pl module-inventory/erp-inv-service -am` 全绿（含重录快照）
- [ ] 快照漂移仅限类名/堆栈变化，已重录并注明
- [ ] 相关文档对齐：`per-mutation-processor-split-plan.md` 回注（若 inv 实测揭示分类偏差）
- [ ] 无范围内项目降级为 deferred/follow-up（LandedCost 运行时验证是显式 successor 所有权转移，非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### LandedCost 休眠 per-mutation 运行时验证

- Classification: `explicit successor ownership transfer`
- Why Not Blocking Closure: 2 个 LandedCost per-mutation 在 R5.8 重配线 BizModel（`@BizMutation approve/reverseApprove` 改经 per-mutation）前不在运行时路径，既有测试走 BizModel→facade 旧路径。R5.6 已完成静态 parity 校验。运行时激活 + 测试覆盖归 R5.8。
- Successor Required: `yes`（R5.8）

### D-mutation + 纯 D-mutation 实体保留在 facade

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: roadmap MR5 §D-mutation 明示范围外。纯 D-mutation facade（ErpInvStockMove、ErpInvOwnershipTransfer）无 S-mutation per-mutation 文件，实测确认。
- Successor Required: `no`

### BizModel 配线 + beans.xml + xbiz 清理

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属 R5.8（roadmap 明示）。
- Successor Required: `yes`（R5.8 在 R5.1-R5.7 全 done 后执行）

## Closure

Status Note: _（待执行 + 独立结束审计）_

Closure Audit Evidence:

- Auditor / Agent: _（待独立结束审计）_
- Evidence: _（待填充）_

Follow-up:

- 共享 hook 策略沿用候选 A（R5.1 裁决），inv 域同包适用性在本 plan 确认。
- LandedCost Long 签名边界 + 会计保护区域（GL 凭证 + 成本层冲销）经 facade helper 单一真相源的模式回注配方供后续参考。
- doReject/doReverseApprove 偏离修正（reverseApprove=REJECTED 非基类 SUBMITTED）回注配方——R5.1 已记录此通用检查项。
