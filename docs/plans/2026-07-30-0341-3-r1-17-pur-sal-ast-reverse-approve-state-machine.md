# 2026-07-30-0341-3-r1-17-pur-sal-ast-reverse-approve-state-machine pur/sal/ast reverseApprove + INLINE 守卫一致性修复

> Plan Status: active
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.17（P1-MA2-049/050/051 [pur] + P1-MA2-056/057 [sal] + P1-MA2-058/059 [ast] = 7 findings），源自 A2.8/A2.9/A2.10 状态机审查
> Related: `docs/audits/2026-07-28-0230-arm-ma2-purchase-state-machine.md`、`docs/audits/2026-07-28-0400-arm-ma2-sales-state-machine.md`、`docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`；`docs/design/{purchase,sales,assets}/state-machine.md` + `domain-design-guidelines.md §16.4`
> Audit: required

## Current Baseline

七项 finding 经实仓逐项确认：均为「reverseApprove 目标态漂移（SUBMITTED vs owner doc 强制 REJECTED）+ INLINE 动作缺守卫 + reversal listener 不对称」，**不破坏红冲闭环一致性**（Quotation/Rfq/Contract/Movement 无 posted 副作用——不过账、无凭证需 reverse；危害限于 approveStatus 审计轨迹副轴漂移）。

**reverseApprove→SUBMITTED 漂移（4 实体 / 3 finding，owner doc §2 + domain-design-guidelines §16.4 强制 REJECTED）— 确认：**
- `ErpPurQuotation.xbiz:97` + `ErpPurRfq.xbiz:97` reverseApprove 设 `entity.approveStatus = 'SUBMITTED'`（P1-MA2-049，含 Quotation 与 Rfq 两实体）。
- `ErpSalContract.xbiz:97` reverseApprove 设 `entity.approveStatus = 'SUBMITTED'`（P1-MA2-056）。
- `ErpAstMovement.xbiz:97` reverseApprove 设 `entity.approveStatus = 'SUBMITTED'`（P1-MA2-058）。
- 对照：各域大 Processor 合规（ErpPurOrderProcessor.doReverseApprove + 6 大 sales Processor + assets 6 大 Processor 全设 APPROVE_STATUS_REJECTED + 清 approvedBy/At）；漂移限于「无大 Processor 的实体 xbiz 直设 SUBMITTED」。

**INLINE 缺 isCancelled/业务守卫（3 实体族）— 确认：**
- pur Receive/Invoice/Payment/Return/Requisition 的 INLINE reject + 全实体 INLINE withdrawApproval（P1-MA2-050）仅校验 `approveStatus src` 后设新状态，缺 PROC 路径 `validateNotCancelled`/`requireSupplierActive`/`requireLinesNonEmpty`。
- sal 6 实体 INLINE withdrawApproval + Contract 全 5 INLINE 动作（P1-MA2-057）同型缺守卫。
- ast Movement 全 5 INLINE 动作（P1-MA2-059）同型缺守卫；Movement 无 cancel mutation 暴露，docStatus=CANCELLED 经 useLogicalDelete 承载（危害更轻）。

**PurReversalListener.rollbackReceive 不对称（P1-MA2-051）— 确认：**
- `PurReversalListener.rollbackReceive:112-123` 仅设 posted=false 保留 APPROVED（Javadoc deliberate），与 rollbackInvoice/Payment/Return（全降级 APPROVED→REJECTED）不对称；冲销后 receive APPROVED+posted=false 悬挂。不破坏业财一致（凭证已红冲 GL 平衡）。

**保护区域：** reverseApprove 目标态修改 + INLINE 守卫补齐不触及会计/数据删除保护区域（无 posted 副作用实体 + 副轴漂移修复）；PurReversalListener 修改触及冲销路径但 posted 标志回退属既有行为对齐（非新写凭证）。本计划为 xbiz/listener 一致性修复，仍走标准 plan-audit + closure-audit。

## Goals

- 三实体（四实体 / 三 finding）reverseApprove 目标态由 SUBMITTED 改为 REJECTED（与大 Processor + owner doc §2 + §16.4 对齐）—— P1-MA2-049/056/058。
- 三实体族 INLINE 动作补 isCancelled 等守卫，消除 CANCELLED 单据 approveStatus 副轴漂移 —— P1-MA2-050/057/059。
- PurReversalListener.rollbackReceive 与其他三实体对齐（APPROVED→REJECTED）—— P1-MA2-051。
- owner doc state-machine.md 各域补「实现模式 + INLINE 守卫边界 + reversal listener 回退目标态表」。

## Non-Goals

- 不为 Quotation/Rfq/Contract/Movement 引入完整大 Processor（方案A 全守卫迁移——工作量大，危害限于副轴漂移不必要；死代码 Processor 类处置归 P2-MA2-054 watch-only）。
- 不改 sales SalReversalListener.rollbackDelivery（P2-MA2-057 watch-only——业务侧恢复路径完整，已降 P2）。
- 不补多币种/核销/三单匹配测试（归 MR2）；本计划测试聚焦 reverseApprove 目标态 + 守卫阻断负向。

## Task Route

- Type: `implementation-only change`（xbiz/listener 一致性修复）
- Owner Docs: `docs/design/purchase/state-machine.md`、`docs/design/sales/state-machine.md`、`docs/design/assets/state-machine.md`、`docs/design/domain-design-guidelines.md §16.4`
- Skill Selection Basis: xbiz source 脚本 + Processor 守卫模式 + reversal listener → `Skill: nop-backend-dev`（xbiz 动作/守卫/跨实体调用 + 产品化可定制性自检）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - 七项 finding 裁决（Decision）

Status: planned
Targets: 本计划（裁决记录）
Skill: `nop-backend-dev`

- Item Types: `Decision`
- Prereqs: none

- [ ] **Decision**：七项 finding 处置方案逐项裁决。
      - 049/056/058 reverseApprove→SUBMITTED：**方案A（机械 Fix）**——xbiz 改设 `entity.approveStatus = 'REJECTED'` + 清 approvedBy/At（与大 Processor + owner doc §2/§16.4 对齐）。无合理替代（owner doc 强制规则）。
      - 050/057/059 INLINE 缺守卫：**方案B（INLINE 补 isCancelled 守卫）**。理由：危害限于副轴漂移（docStatus=CANCELLED 主终态持有 + settle/过账查询校验 approveStatus=APPROVED 不误纳）+ 最小变更 + 三域一致；requireSupplier/Customer/Lines 等业务守卫在 submit 时点已门控（审批时点重复校验冗余，审计 P1-MA2-050/057 证据），故 isCancelled 守卫即足以消除副轴漂移主缺陷；方案A（迁移到既有死代码 Processor）更重且与危害不成比例（死代码处置归 P2-MA2-054）；方案C（owner doc 永久接受）被拒——副轴漂移虽危害有限但守卫缺失是真实缺陷，补最小守卫即可。残留风险：INLINE 守卫与 PROC 守卫仍非完全对齐（requireSupplier/Customer/Lines 守卫不补）→ owner doc 标注 INLINE 守卫边界。
      - 051 rollbackReceive 不对称：**方案A（对齐 REJECTED）**——与 rollbackInvoice/Payment/Return 一致，`if (approveStatus == APPROVED) setApproveStatus(REJECTED)`；方案B（deliberate 不对称）被拒——receive 无业务侧恢复路径（与 sales delivery 不同），保留 APPROVED 致真悬挂。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] Phase 1 Decision 逐项记录；后续 Phase 严格遵循（049/056/058→Phase 2，050/057/059→Phase 3 守卫，051→Phase 3 listener）。

### Phase 2 - reverseApprove 目标态 REJECTED 对齐（P1-MA2-049/056/058）

Status: planned
Targets: `ErpPurQuotation.xbiz`、`ErpPurRfq.xbiz`（pur）；`ErpSalContract.xbiz`（sal）；`ErpAstMovement.xbiz`（ast）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] **Fix（049）**：`ErpPurQuotation.xbiz:97` + `ErpPurRfq.xbiz` reverseApprove 改 `entity.approveStatus = 'REJECTED'` + `entity.approvedBy = null; entity.approvedAt = null`（与 pur 大 Processor 对齐）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（056）**：`ErpSalContract.xbiz:97` reverseApprove 改 REJECTED + 清 approvedBy/At。
      - Skill: `nop-backend-dev`
- [ ] **Fix（058）**：`ErpAstMovement.xbiz:97` reverseApprove 改 REJECTED + 清 approvedBy/At。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：测试——三实体 reverseApprove 后断言 approveStatus='REJECTED' + approvedBy/At cleared；submit→approve→reverseApprove 链路主路径通过。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三实体 xbiz reverseApprove 目标态为 REJECTED（grep `approveStatus = 'SUBMITTED'` 在 reverseApprove 段为零）；与大 Processor 一致。

### Phase 3 - INLINE 守卫补齐（050/057/059）+ rollbackReceive 对称（051）

Status: planned
Targets: pur/sal/ast 各 INLINE xbiz、`PurReversalListener.java`、各域 state-machine.md
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1

- [ ] **Fix（050，方案B）**：pur 全 INLINE 实体前置补 isCancelled 守卫——覆盖 Receive/Invoice/Payment/Return/Requisition/Order 的 INLINE reject + withdrawApproval **以及 Quotation/Rfq 全 INLINE 动作**（submitForApproval/approve/reject/reverseApprove/withdrawApproval，P1-MA2-050 明确含 Quotation/Rfq 全 INLINE；Phase 2 仅改其 reverseApprove 目标态，其余动作仍需守卫）：脚本前置 `if (entity.docStatus === 'CANCELLED') throw NopScriptError(...)`。
      - Skill: `nop-backend-dev`
- [ ] **Fix（057，方案B）**：sal 6 实体 INLINE withdrawApproval + Contract 全 INLINE 动作前置补 isCancelled 守卫。
      - Skill: `nop-backend-dev`
- [ ] **Fix（059，方案B）**：ast Movement 全 5 INLINE 动作前置补 isCancelled 守卫（Movement 经 useLogicalDelete 承载 CANCELLED，守卫为防御性）。
      - Skill: `nop-backend-dev`
- [ ] **Fix（051，方案A）**：`PurReversalListener.rollbackReceive` 与 rollbackInvoice/Payment/Return 对齐：`if (approveStatus == APPROVED) setApproveStatus(REJECTED)`；purchase state-machine.md / returns.md 回退目标态表补 receive→REJECTED。
      - Skill: `nop-backend-dev`
- [ ] **Proof**：测试——CANCELLED 单据调 reject/withdrawApproval/reverseApprove assertThrows（守卫阻断）；PurReversalListener rollbackReceive 后断言 receive approveStatus=REJECTED + posted=false。
      - Skill: `nop-backend-dev`
- [ ] **Add（owner doc）**：三域 state-machine.md 补「§实现模式」注记（PROC vs INLINE 守卫边界：INLINE 仅 isCancelled + src 状态校验，无 requireSupplier/Customer/Lines 业务守卫）+ reversal listener 回退目标态表（receive→REJECTED）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 三域 INLINE 动作 isCancelled 守卫可测（CANCELLED 单据阻断）；PurReversalListener.rollbackReceive 对称（REJECTED）；owner doc 实现模式 + 回退表与代码一致。

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_05094afc2ffe4Lg255JqVYBzOm) because Phase 3 Fix(050) 仅枚举 Receive/Invoice/Payment/Return/Requisition/Order，遗漏 P1-MA2-050 明确含的 Quotation/Rfq 全 INLINE 动作（ErpPurRfq.xbiz 同为 5 守卫缺失 INLINE，Phase 2 仅改 reverseApprove 目标态，其余动作仍无守卫）→ 实现者按复选框执行会漏 2 实体；另实体计数「三实体」应为「四实体/三 finding」（049 含 Quotation+Rfq），baseline 049 条目漏 Rfq；方案B 残留风险缓解未引用 submit 时点门控审计证据。基线锚点全绿（reverseApprove→SUBMITTED ×3 + rollbackReceive:112-123 + 大 Processor REJECTED）、7/7 finding 覆盖、Non-Goals 守纪。
- Independent draft review iteration 2: accept (ses_05090fe0dffep6j1IuANpHiR37) after Phase 3 Fix(050) 显式补「以及 Quotation/Rfq 全 INLINE 动作」+ 实体计数更正「四实体/三 finding」+ baseline 049 注明含 Quotation 与 Rfq + 方案B 引用 submit 时点门控审计证据（requireSupplier/Customer/Lines 在 submit 已门控，审批时点冗余）。ErpPurRfq.xbiz 实仓确认同为 5 守卫缺失 INLINE；7/7 finding 覆盖、mvn 在 Closure Gates、无禁用词、Non-Goals（rollbackDelivery P2 / 死代码 Processor P2-054）正确排除。

## Closure Gates

- [ ] 范围内行为完成（7 项 finding 全部修复：3 reverseApprove REJECTED + 3 INLINE 守卫 + 1 listener 对称）
- [ ] 相关文档对齐（三域 state-machine.md 实现模式 + 回退目标态表）
- [ ] 已运行验证（`mvn clean install -DskipTests` 全绿 + `mvn test` 全绿 + compliance checker 基线不高于 M0；grep 验证 reverseApprove 段零 SUBMITTED + INLINE 守卫存在）
- [ ] 无范围内项目降级为 deferred/follow-up（方案B INLINE 守卫是范围内存活实现项；死代码 Processor 处置归 P2-MA2-054 watch-only 不在范围）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### INLINE 动作迁移到完整大 Processor（方案A successor）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 方案B（INLINE 补 isCancelled 守卫）已消除副轴漂移主缺陷；方案A 迁移（启用既有死代码 Processor 类 P2-MA2-054）提供 requireSupplier/Customer/Lines 等业务守卫，但危害限于副轴漂移不必要。
- Successor Required: `yes`（若 INLINE 守卫边界被业务误用导致 CANCELLED 单据业务规则绕过时，迁移到大 Processor）

### SalReversalListener.rollbackDelivery 不对称

- Classification: `watch-only residual`
- Why Not Blocking Closure: P2-MA2-057 watch-only——sales delivery 业务侧恢复路径完整（ensureReversed 链触发库存 reverse），与 purchase receive 不同；Javadoc deliberate。
- Successor Required: `yes`（若 sales delivery 悬挂经运营反馈成实际痛点时对齐 REJECTED）

## Closure

Status Note: _（结束审计后填充）_

Closure Audit Evidence:

- _（独立结束审计后填充）_

Follow-up:

- _（非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件）_
