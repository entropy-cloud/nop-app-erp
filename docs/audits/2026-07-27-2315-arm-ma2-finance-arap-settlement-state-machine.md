# ARM MA2 — finance AR/AP 核销状态机业务审查（A2.5c，S 级拆分 3/3）

> Audit Status: closed
> Mission: audit-remediation
> Work Item: A2.5c finance 状态机审查 — AR/AP 核销（S 级拆分 3/3）
> Source Plan: `docs/plans/2026-07-27-2315-2-audit-remediation-ma2-finance-arap-settlement-state-machine.md`
> Skill: `docs/skills/state-machine-business-review-prompt.md`（+ 项目定制化层 `docs/skills/README.md §项目定制化层`）
> Reviewed: 2026-07-28
> Scope: **AR/AP 辅助账项状态机**（`ErpFinArApItem.status` dict `erp-fin/ar-ap-status`：OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）+ **核销单状态机**（`ErpFinReconciliation.docStatus` dict `erp-fin/reconciliation-status`：DRAFT/POSTED/REVERSED，头+行）+ **坏账核销状态机**（`ErpFinBadDebt` approveStatus dict `wf/approve-status` × docType dict `erp-fin/bad-debt-type` WRITE_OFF/RECOVERY + `reverseApprove` 红冲闭环）。会计凭证状态机归 A2.5a（done）、期间/预算状态机归 A2.5b（done）。
> Related: A2.5a `docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（done）；A2.5b `docs/audits/2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md`（done）

## 1. 裁决

**Verdict: pass（零 P0、零新 P1、6 项新 P2 watch-only）**

AR/AP 核销三组件状态机（辅助账项 5 态 + 核销单 3 态 + 坏账审批状态机）核心契约经实仓逐项证据确认：状态迁移守卫齐全、`@BizMutation` 事务回滚保证核销失败时辅助账与核销单一致性、`reverseApprove` 红冲闭环对称回退（红冲凭证→ArApItem 对称回滚→APPROVED→REJECTED 强一致顺序）、坏账核销/收回经专项入口（`executeWriteOff`/`executeRecovery`）隔离于核销单路径。

**关键裁决（计划假设证伪/确认）**：

| 计划假设 | 裁决 | 证据 |
|---|---|---|
| CANCELLED 辅助账项不可达致 dict 死状态（候选 P0） | **证伪** | `ErpFinArApItemGenerator.cancelOnReverse:125-136` 置 `status=CANCELLED`+`openAmount=0`，由 `ErpFinPostingProcessor.reverse:234`（过账红冲入口）同事务调用。CANCELLED 是源单据（发票/收付款/退货/票据）过账红冲的规范终态，**可达**。 |
| 域侧与 finance 双路径核销状态机分歧致辅助账余额不一致（候选 P0） | **证伪为分歧** | 经 `ErpFinReconciliationBizModel` Javadoc + `ar-ap-reconciliation.md` 明确：域侧 `ReceiptSettler`/`PaymentSettler` 作用于域级发票/收付款行（运营核销，回写 `receivedAmount`/`paidAmount`/`receivedStatus`/`paidStatus`/`writtenOffStatus`）；finance `ErpFinReconciliation`+`ReconciliationSettler` 作用于 `ErpFinArApItem`（GL/账龄正式核销）。**两条独立并行轨道（by design）**，非分歧。残留风险：两路径余额无对账守卫（P2-MA2-038）。 |
| 坏账 `reverseApprove` 红冲失败致 ArApItem 与凭证悬挂半状态（候选 P0） | **证伪** | `ErpFinBadDebtProcessor.reverseApprove:124-164` 顺序：step1 `finPostingExecutor.reverse`（失败抛 `NopException`→tx 回滚）→ step2 ArApItem 对称回退 → step3 APPROVED→REJECTED。全程 `@BizMutation` 事务，红冲凭证失败则全回滚。`TestErpFinBadDebtReversal` 覆盖 writeOff/recovery 双向红冲闭环。 |
| 多币种核销辅助账本位币余额错误（P1-MA2-009 升级评估） | **维持 P1，不升 P0** | `ReconciliationSettler.applySettlement` 用 `settledAmountFunctional` 回写辅助账——算术上忠实记录调用方提供的金额；缺口在调用方（核销/收款过账）未做 FX plug（P1-MA2-009 已登记）。单币种路径（tested baseline，`exchangeRate=ONE`）`amountSource==amountFunctional`，辅助账余额正确。多币种为未实现功能缺口，非运行时数据破坏。 |
| 并发核销辅助账 SETTLED 判定漂移（P2-MA2-008/014 升级评估） | **维持 P2，不升 P0/P1（降级）** | `ErpFinArApItem` ORM `versionProp="version"`（propId 23，`app-erp-finance.orm.xml:732`）——Nop ORM 在 `updateEntity` flush 时自动乐观锁校验。并发核销同一辅助账项：两事务同读 version=N，后提交事务 flush 时 version 不匹配抛 stale 异常 → **无静默丢失更新**，仅乐观冲突（需重试）。将 P2-MA2-008/014 从"silent lost-update 风险"降级为"detectable optimistic conflict"，交接 A2.17。 |

### 1.1 审查范围

- 辅助账项状态机轴：`ErpFinArApItem`（`module-finance/model/app-erp-finance.orm.xml:732`）+ `ErpFinArApItemGenerator`（生成 OPEN / 红冲置 CANCELLED）+ `ReconciliationSettler.resolveStatus:83-93`（OPEN/PARTIAL/SETTLED 裁决）+ `ErpFinBadDebtProcessor.executeWriteOff:183-202`/`executeRecovery:208-226`/`reverseApprove:124-164`（WRITTEN_OFF↔OPEN）。
- 核销单状态机轴：`ErpFinReconciliation`（头，`:803`）+ `ErpFinReconciliationLine`（行，`:864`）+ `ErpFinReconciliationBizModel`（create→DRAFT / post→POSTED / reverse→REVERSED / previewReverse）+ `ReconciliationSettler.settle:32-47`/`reverseSettle:52-60`。
- 坏账核销状态机轴：`ErpFinBadDebt`（`:1665`）+ `ErpFinBadDebtProcessor`（writeOff/recover/submit/approve/reject/reverseApprove）+ `ErpFinBadDebtBizModel`（Facade，全 `@BizMutation`）。
- 域侧核销器（双路径复核）：`ReceiptSettler`（sales）+ `PaymentSettler`（purchase）。
- 票据核销联动：`ErpFinNotesReceivableProcessor.doWriteOff:217-229` + `ErpFinNotesPayableProcessor.writeOff`（经 `postingDispatcher.reverse*` → 过账红冲链 → `cancelOnReverse` → ArApItem CANCELLED）。
- owner doc：`ar-ap-reconciliation.md` + `bad-debt.md` + `bank-reconciliation.md` + `state-machine.md` + `posting.md`。
- 测试：`TestErpFinReconciliation` + `TestErpFinReconciliationReversePreview` + `TestErpFinAutoReconciliation` + `TestErpFinBadDebt` + `TestErpFinBadDebtReversal` + `TestErpFinBadDebtProvisionReversal` + `TestErpFinNotesReceivableStateMachine` + `TestErpFinNotesPayableStateMachine` + `TestErpFinNotesReceivablePosting` + `TestErpFinNotesPayablePosting` + `TestErpSalReceiptSettlement` + `TestErpPurPaymentSettlement` + `TestErpSalOrderToCashEnd` + `TestErpPurProcureToPayEnd`。

### 1.2 可达性摘要

辅助账项 5 态**全部可达**：OPEN（生成）→ PARTIAL（部分核销）/ SETTLED（全额核销）/ WRITTEN_OFF（坏账核销）/ CANCELLED（源单据过账红冲）；PARTIAL/SETTLED 经 `reverseSettle` 可回退 OPEN/PARTIAL；WRITTEN_OFF 经 `executeRecovery`/`reverseApprove` 可回退 OPEN。无死状态、无不可达终态。核销单 3 态全部可达；坏账审批状态机 UNSUBMITTED→SUBMITTED→APPROVED/REJECTED 全可达（REJECTED 为本记录终态，重试经新建记录——见 §7 P2-MA2-040）。

### 1.3 角色/权限摘要

owner doc `ar-ap-reconciliation.md §核销权限` 定义手工核销（财务员）/ 自动核销配置（财务管理员）/ 核销冲销（财务员+原因）；`bad-debt.md §SOX 控制` C21 要求坏账核销/恢复审批人与发起人分离，经 config `erp-fin.bad-debt-write-off-require-approval`（默认 true）门控。本审计未做权限绑定运行时验证（归 A4 平台合规），状态机层面无角色漂移反模式。

### 1.4 外部依赖摘要

辅助账生成依赖过账引擎 Provider（`ErpFinArApItemGenerator`，A2.5a 已确认金额一致性）；票据核销经 `postingDispatcher.reverse*` → 过账红冲链联动 AR/AP（同向核销）；期末结账 `preCheck` 经 `findUnsettledArApCodes` 扫描未核销项（`status≠SETTLED/CANCELLED/WRITTEN_OFF`，已正确排除 WRITTEN_OFF）；银行对账（`bank-match-status`）是**独立子系统状态机**（`bank-reconciliation.md:11,112` 明示与 AR/AP 核销解耦）。

### 1.5 剩余风险

- 多币种核销 FX plug 未实现（P1-MA2-009，MR1）；
- 域侧/finance 双路径余额无对账守卫（P2-MA2-038）；
- `assertOpen` 不拒绝 WRITTEN_OFF，坏账状态机与核销状态机隔离不完整（config-gated，P2-MA2-039）；
- 核销 post/reverse 无期间 CLOSED_FINAL 守卫（P2-MA2-041，扩展 P1-MA2-021 范围）；
- owner doc 漂移（P2-MA2-036/037）。

---

## 2. 状态图与转换矩阵

### 2.1 辅助账项状态机（`ErpFinArApItem.status`，dict `erp-fin/ar-ap-status` 5 态）

```
                  ┌─────────────────────────────────────────────────────┐
                  │                                                     │
                  ▼                                                     │
[生成] ──► OPEN ──┤                                                    │
            │     │                                                    │
            │     │ applySettlement(settled>0, <total)                 │
            │     ▼                                                    │
            │  PARTIAL ──┐                                             │
            │     ▲      │ applySettlement(settled>=total)             │
            │     │      ▼                                             │
            │     │  SETTLED ──┐                                       │
            │     │      │     │ reverseSettle (settled-=amt)          │
            │     │      │     ▼                                       │
            │     └──────┴── ◄─┘ (回退 OPEN 或 PARTIAL, resolveStatus)  │
            │                                                       │   │
            │ executeWriteOff (openAmount→0)                        │   │
            ▼                                                       │   │
        WRITTEN_OFF ◄────────────────────────────────────────────────┘   │
            │     ▲                                                       │
            │     │ executeRecovery / reverseApprove(writeOff反向)        │
            │     │       (WRITTEN_OFF → OPEN)                            │
            │     │                                                       │
            │ reverseApprove(recovery反向) (OPEN → WRITTEN_OFF)           │
            ▼                                                             │
        (可经 recover 再回 OPEN)                                           │
                                                                          │
[源单据过账红冲] ──► CANCELLED  (终态；openAmount→0)                       │
            ▲                                                             │
            │ ErpFinArApItemGenerator.cancelOnReverse                     │
            │   (由 ErpFinPostingProcessor.reverse 调用)                   │
            └─────────────────────────────────────────────────────────────┘
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (生成)→OPEN | 发票/收付款过账 | `ErpFinArApItemGenerator.generate:65-119` (setStatus OPEN:115) | 幂等 `(sourceBillType,sourceBillCode)` | PASS |
| OPEN→PARTIAL | 部分核销 | `ReconciliationSettler.applySettlement:62-81`+`resolveStatus:83-93` | `assertNotOver`（默认 config） | PASS |
| OPEN/PARTIAL→SETTLED | 全额核销 | 同上 (settled>=total) | 同上 | PASS |
| SETTLED/PARTIAL→OPEN/PARTIAL | 核销冲销 | `ReconciliationSettler.reverseSettle:52-60` (reverse=true) | 核销单须 POSTED | PASS |
| OPEN→WRITTEN_OFF | 坏账核销 | `ErpFinBadDebtProcessor.executeWriteOff:183-202` (setStatus WRITTEN_OFF:190) | `requireOpenArApItem`+`validateAmount` | PASS |
| WRITTEN_OFF→OPEN | 坏账收回 / 反审核 | `executeRecovery:208-226`/`reverseApprove:142-148` | `requireWrittenOffArApItem` | PASS |
| (任一正常态)→CANCELLED | 源单据过账红冲 | `ErpFinArApItemGenerator.cancelOnReverse:125-136` ← `ErpFinPostingProcessor.reverse:234` | 过账引擎红冲前置 | PASS |
| **WRITTEN_OFF→SETTLED（跨状态机覆写）** | 核销单 post 含 WRITTEN_OFF 项 | `ReconciliationSettler.applySettlement`（resolveStatus 不查当前态） | `assertOpen:308-315` **不拒绝 WRITTEN_OFF**；默认 config 由 `assertNotOver`+openAmount=0 间接阻挡；`allow-over-reconcile=true` 时可覆写 | **FAIL→P2-MA2-039**（隔离不完整，config-gated） |

### 2.2 核销单状态机（`ErpFinReconciliation.docStatus`，dict `erp-fin/reconciliation-status` 3 态）

```
create ──► DRAFT ──post──► POSTED ──reverse──► REVERSED (终态)
             │                  │
             │                  └──previewReverse (只读预览，@BizQuery)
             │
             └──(草稿废弃：useLogicalDelete=true，无 CANCELLED 状态迁移)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| create→DRAFT | `ErpFinReconciliationBizModel.create:70-112` | DRAFT 初始化:96 | direction/partner/date/lines 非空 | PASS |
| DRAFT→POSTED | `post:116-140` | settler.settle + POSTED:133 | `docStatus==DRAFT`:118 + `validateLine`（同方向/同往来/超额/日期） | PASS |
| POSTED→REVERSED | `reverse:144-157` | settler.reverseSettle + REVERSED:152 | `docStatus==POSTED`:146 | PASS |
| POSTED→(预览) | `previewReverse:165-190`（@BizQuery 只读） | 镜像 reverse 前置校验 | `docStatus==POSTED`:168 | PASS |
| DRAFT→(废弃) | 逻辑删除（`useLogicalDelete=true`，deleteFlagProp=delVersion） | CrudBizModel 默认 delete | dict 无 CANCELLED 项，草稿废弃不经状态迁移 | PASS（设计一致；非缺陷，区别于凭证 P1-MA2-031——凭证 dict 有 CANCELLED 项不可达） |

> **REVERSED 核销单可再核销**：经**新核销单**（DRAFT→POSTED）作用于同辅助账项，非 REVERSED 单自身复活。符合状态机语义（冲销后如需重新核销须新建核销单）。PASS。

### 2.3 坏账核销状态机（`ErpFinBadDebt.approvalStatus` × `docType`）

```
writeOff/recover 创建
       │ (approvalStatus=UNSUBMITTED)
       ▼
   UNSUBMITTED ──submit──► SUBMITTED ──approve──► APPROVED ──reverseApprove──► REJECTED
       │                       │                     │  (生效: executeWriteOff/    ▲
       │                       │                     │   executeRecovery 变异 ArApItem ▲
       │                       ▼                     │   + 凭证；红冲闭环对称)        │
       │                    REJECTED (reject)        │                                │
       │                                               (反审核: 红冲凭证 + ArApItem      │
       │                                                对称回退 + APPROVED→REJECTED)    │
       │                                                                                │
       └── config `bad-debt-write-off-require-approval`=false 时：创建即 approveInternal → APPROVED（跳过 SUBMITTED）
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| writeOff→UNSUBMITTED/APPROVED | `ErpFinBadDebtProcessor.writeOff:61-71` | requireOpenArApItem + config 门控 | OPEN/PARTIAL + openAmount>0 | PASS |
| recover→UNSUBMITTED/APPROVED | `recover:73-82` | requireWrittenOffArApItem + config 门控 | WRITTEN_OFF | PASS |
| UNSUBMITTED→SUBMITTED | `submit:86-91`+`validateTransitionForSubmit:238-243` | 仅 UNSUBMITTED | — | PASS |
| SUBMITTED/UNSUBMITTED→APPROVED | `approve:93-100`+`approveInternal:168-177`（生效 executeWriteOff/Recovery） | `validateTransitionForApprove` 允许 UNSUBMITTED/SUBMITTED | 幂等 `isApproved`:95 | PASS |
| SUBMITTED/UNSUBMITTED→REJECTED | `reject:102-107`+`validateTransitionForReject` | 同上 | — | PASS |
| APPROVED→REJECTED（反审核红冲） | `reverseApprove:124-164` | `isApproved && voucherId!=null`:127；红冲凭证→ArApItem 对称→REJECTED | 强一致顺序 | PASS |
| **REJECTED→SUBMITTED（重提）** | `validateTransitionForSubmit` **仅允许 UNSUBMITTED** | 不允许 REJECTED 重提 | retry 经新建 writeOff/recover 记录 | **P2-MA2-040**（与 EmployeeAdvance 不对称；设计可接受） |

> **docType × approvalStatus 组合语义清晰**：WRITE_OFF+APPROVED（已核销，ArApItem=WRITTEN_OFF）/ RECOVERY+APPROVED（已恢复，ArApItem=OPEN）/ REJECTED（未生效或反审核，ArApItem 维持迁移前态）。两轴正交，无歧义组合。PASS。

---

## 3. 九大控制点 + 项目特定维度裁决

### 控制点 1：状态定义 — **PASS（含 1 项 P2 owner doc 漂移）**

- 辅助账项 5 态语义清晰：OPEN（等待核销）/ PARTIAL（部分核销，等待继续核销）/ SETTLED（已核销，可经冲销恢复）/ WRITTEN_OFF（已坏账核销，可经收回/反审核恢复）/ CANCELLED（源单据红冲，终态）。每个状态名清楚表达业务等待点。
- WRITTEN_OFF **非真终态**（可 recover/reverseApprove → OPEN）——这是坏账业务语义决定的（已核销坏账事后回款需恢复），设计正确，非缺陷。
- SETTLED **非真终态**（可 reverseSettle 回退）——核销冲销业务需求，设计正确。
- CANCELLED **真终态**（源单据红冲后辅助账项不再可核销）——入口经 `cancelOnReverse`，可达。
- 核销单 3 态完整；草稿废弃走 `useLogicalDelete`（dict 无 CANCELLED 项，无不可达死状态）——设计一致。
- 坏账 approveStatus×docType 组合语义清晰（见 §2.3）。
- **P2-MA2-036**：owner doc `ar-ap-reconciliation.md §核销状态:139-152` 发票核销状态 4 态（UNRECONCILED/PARTIAL/RECONCILED/**OVER**）/ 收付款核销状态 3 态（UNRECONCILLED/PARTIAL/RECONCILLED）vs dict 5 态（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）——**命名+数量均漂移**；OVER 状态 owner doc 声明但 dict 无 OVER 项（实现以 `assertNotOver` 拒绝超额，不设 OVER 状态——设计选择，但 owner doc 描述了一个未实现的状态）。owner doc 治理缺陷，无运行时影响。

### 控制点 2：转换完整性 — **PASS**

- 辅助账项每个状态的传入/传出转换已列（§2.1 矩阵）。`ReconciliationSettler.resolveStatus:83-93` 仅返回 OPEN/PARTIAL/SETTLED，**CANCELLED 与 WRITTEN_OFF 永不由 ReconciliationSettler 设置**——二者经专项入口（cancelOnReverse / executeWriteOff），状态机职责隔离正确。
- 核销单 DRAFT→POSTED（settle）/ POSTED→REVERSED（reverseSettle）转换完整；草稿废弃走逻辑删除（设计选择）。
- 坏账审批状态机 submit/approve/reject/reverseApprove 转换完整（§2.3）。
- 票据核销联动：`doWriteOff` 经 `postingDispatcher.reverseReceivable/Payable` → 过账红冲链 → `cancelOnReverse` → ArApItem CANCELLED（间接联动，同向核销语义）。
- 无非法跳转或缺失条件分支。

### 控制点 3：终端状态和恢复 — **PASS**

- 终态：CANCELLED（真终态，源单据红冲）。SETTLED/WRITTEN_OFF/APPROVED/REVERSED 均可恢复（分别经 reverseSettle / recover+reverseApprove / reverseApprove / 新建核销单）——业务需要，设计正确。
- 归档/活动辅助账项可区分：`settledAmount`/`openAmount` + `status`（OPEN/PARTIAL=活动；SETTLED/CANCELLED/WRITTEN_OFF=归档/终态）。
- REVERSED 核销单可再核销（经新核销单，非自身复活）。

### 控制点 4：异常路径 — **PASS（含 1 项 P2 期间守卫缺口）**

- 超额核销：`assertNotOver:317-325`（config `allow-over-reconcile=false` 默认拒绝；精度 `reconcile-precision`）PASS。
- 跨往来单位核销：`validateLine:287-291` `ERR_RECONCILIATION_PARTNER_MISMATCH` PASS。
- 同方向核销：`validateLine:282-286` `ERR_RECONCILIATION_DIRECTION_MISMATCH` PASS。
- 日期校验：`validateLine:300-305` `ERR_RECONCILIATION_DATE_BEFORE_INVOICE` PASS。
- 坏账核销金额超 openAmount：`validateAmount:305-314` `ERR_BAD_DEBT_AMOUNT_OVER_OPEN` PASS。
- recover 非 WRITTEN_OFF 项：`requireWrittenOffArApItem:293-303` `ERR_BAD_DEBT_AR_AP_ITEM_NOT_WRITTEN_OFF` PASS。
- reverseApprove 未过账坏账单：`reverseApprove:127-130` `ERR_BAD_DEBT_NOT_APPROVED_OR_NOT_POSTED` PASS。
- 核销项非开放态：`assertOpen:308-315` 拒绝 SETTLED/CANCELLED —— **但不拒绝 WRITTEN_OFF**（P2-MA2-039）。
- **P2-MA2-041**：已结账期间核销——owner doc `ar-ap-reconciliation.md §异常处理:321` 声明"已结账期间核销 | 拒绝核销，需先反结账"，但 `ErpFinReconciliationBizModel.post/reverse` **不校验期间 CLOSED_FINAL 状态**（核销无 periodId 字段，仅 businessDate）。扩展 P1-MA2-021（CLOSED_FINAL 锁定范围）至核销路径。owner doc 承诺无证据，无运行时 GL 影响（核销不动 GL 凭证）。
- 幂等性：重复 post 同核销单——`post:118` 守卫 `docStatus==DRAFT`，二次 post 抛 `ERR_RECONCILIATION_STATUS_INVALID` PASS。重复 writeOff 同 ArApItem——`requireOpenArApItem` 拒绝非 OPEN/PARTIAL（WRITTEN_OFF 后不可再 writeOff）PASS。

### 控制点 5：可达性 — **PASS**

- 辅助账项 5 态全部可达（CANCELLED 经 `cancelOnReverse`，非死状态——证伪计划假设）。
- 坏账 REJECTED 后**不可经 submit 重提**（`validateTransitionForSubmit` 仅 UNSUBMITTED）——但 retry 经新建 writeOff/recover 记录（requireOpenArApItem/requireWrittenOffArApItem 基于 ArApItem 状态而非坏账单状态），故无死锁，仅 REJECTED 记录为本记录终态（P2-MA2-040）。
- 无死循环、无不可达终态路径。

### 控制点 6：角色和权限 — **PASS（状态机层面）**

- 核销动作角色绑定见 owner doc `§核销权限`（手工核销财务员 / 自动核销配置财务管理员 / 核销冲销财务员+原因）。
- 坏账核销/收回审批门控：config `erp-fin.bad-debt-write-off-require-approval`（默认 true）。
- 危险操作：坏账核销影响报表（`bad-debt.md §SOX 控制` C21 职责分离）/ 核销冲销恢复余额 / reverseApprove 反审核。
- 本审计未做权限绑定运行时验证（归 A4 平台合规维度），状态机层面无角色漂移反模式（无"审核员执行最终关闭"型越权）。

### 控制点 7：外部依赖 — **PASS（含 1 项 P2 双路径治理）**

- **域侧核销器（ReceiptSettler/PaymentSettler）与 finance 核销单（ErpFinReconciliation）双路径**：经证据确认二者**独立并行**（非分歧）——
  - 域侧作用于域级实体（`ErpSalInvoice.receivedAmount/receivedStatus`、`ErpSalReceipt.writtenOffStatus` / `ErpPurInvoice.paidAmount/paidStatus`、`ErpPurPayment.writtenOffStatus`），运营核销权威；
  - finance 作用于 `ErpFinArApItem`（辅助账），GL/账龄正式核销权威；
  - 二者均各自有完整状态裁决（域侧 `recomputeInvoiceReceived/Paid` 3 态 / finance `resolveStatus` 3 态），**不共享回写路径**。
  - `ErpFinReconciliationBizModel` Javadoc（line 46-48）+ `ar-ap-reconciliation.md` 已明确此为设计裁定（"核销单是 finance 域 GL/账龄视角的正式核销，独立作用于辅助账；purchase/sales 域级核销作为运营核销权威并行"）。
  - **P2-MA2-038**：两路径余额（域侧 `receivedAmount` vs 辅助账 `settledAmountFunctional`）无对账守卫保证一致——理论上可 silently diverge（如域侧核销后未触发 finance 核销单）。设计并行但缺一致性断言，MR1 裁决（补 dual-side 对账断言或文档化并行关系）。
- 辅助账生成依赖过账引擎 Provider（`ErpFinArApItemGenerator`，A2.5a 已确认与凭证金额一致性）。
- 票据核销联动 AR/AP（同方向核销，经 postingDispatcher→过账红冲链→cancelOnReverse）。
- 期末结账 preCheck `findUnsettledArApCodes` 扫描未核销项（`ErpFinAccountingPeriodProcessor:581-583` 排除 SETTLED/CANCELLED/WRITTEN_OFF）——正确排除已核销项。
- 银行对账（`bank-match-status`）独立子系统（`bank-reconciliation.md:11,112` 明示与 AR/AP 核销解耦）——边界清晰，归本审计 Non-Goal。

### 控制点 8：TODO/任务策略 — **PASS（含已知静默下沉，已登记 P1-MA2-017）**

- 未核销 AR/AP 账龄逾期：owner doc `§账龄分级` 定义 5 级风险等级，但**无显式催收/付款待办生成**——长期 OPEN 辅助账项静默下沉。期末结账 preCheck `findUnsettledArApCodes` 仅提示（P1-MA2-017 已登记 auto-post-on-close 阻断分级不一致，MR1 裁决），非本审计新发现。
- 坏账核销 SUBMITTED 待审批：无显式待办生成（审批经 config 门控 + 手工 approve），属 watch-only（`bad-debt.md §SOX 控制` C21 职责分离 successor）。
- 核销冲销：无待办生成（冲销为即时操作）。
- 不产生"期望有人行动但不产生待办"的隐藏状态机缺陷（静默下沉已在 P1-MA2-017 登记，归期间结账维度）。

### 控制点 9：场景演练（最重要）— **PASS**

| 场景 | 演练 | 测试覆盖 | 裁决 |
|---|---|---|---|
| (a) 应收核销快乐路径 | AR 发票过账→生成 ArApItem(OPEN)→收款→核销 post→applySettlement→SETTLED | `TestErpSalOrderToCashEnd` + `TestErpFinReconciliation` | PASS |
| (b) 部分核销 | OPEN→PARTIAL→继续核销→SETTLED | `TestErpSalReceiptSettlement`（部分/全额/超额守卫） | PASS |
| (c) 核销冲销 | POSTED→reverseSettle→REVERSED + ArApItem SETTLED→PARTIAL/OPEN | `TestErpFinReconciliationReversePreview` + `TestErpFinReconciliation` | PASS |
| (d) 坏账核销 | OPEN→writeOff→executeWriteOff→WRITTEN_OFF + BAD_DEBT_WRITE_OFF 凭证 | `TestErpFinBadDebt` | PASS |
| (e) 坏账收回 | WRITTEN_OFF→recover→executeRecovery→OPEN + RECOVERY 凭证 | `TestErpFinBadDebt` | PASS |
| (f) 坏账反审核 | APPROVED→reverseApprove→红冲凭证 + ArApItem 对称回退 + REJECTED（强一致） | `TestErpFinBadDebtReversal` | PASS |
| (g) 多币种核销汇兑损益 | **未实现**（P1-MA2-009，MR1）——applySettlement 用 settledAmountFunctional 回写，无 FX plug | 无（单币种 baseline） | **P1-MA2-009 维持**（非本审计新发现） |
| (h) 并发核销 | ErpFinArApItem `versionProp="version"` 乐观锁——后提交事务 stale 异常（无静默丢失更新） | 无显式并发测试 | **P2-MA2-008/014 维持并降级**（detectable conflict，交接 A2.17） |
| (i) 票据核销联动 | 票据 writeOff→postingDispatcher.reverse→过账红冲链→cancelOnReverse→辅助账 CANCELLED | `TestErpFinNotesReceivableStateMachine` + `TestErpFinNotesPayableStateMachine` + `TestErpFinNotesReceivable/PayablePosting` | PASS |

### 控制点 10：与设计文档一致性 — **FAIL（2 项 P2 owner doc 漂移）**

- **P2-MA2-036**（见控制点 1）：`ar-ap-reconciliation.md §核销状态` 命名+数量漂移（UNRECONCILLED/PARTIAL/RECONCILLED/OVER vs OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）+ OVER 状态声明但 dict 无 + `§核销冲销` reversalFlag 布尔 vs 实现 docStatus=REVERSED（owner doc line 51-55 已部分注记）+ `§异常处理:322` "并发核销冲突 | 使用悲观锁保证数据一致性"——**owner doc 错误承诺**（实现无悲观锁，依赖 ORM versionProp 乐观锁）。
- **P2-MA2-037**：`state-machine.md` **无 AR/AP 核销独立状态机章节**（仅对象一凭证+对象二期间），AR/AP 核销+坏账状态机散落在 `ar-ap-reconciliation.md §核销状态` + `bad-debt.md §状态含义`。与 P2-MA2-034（期间状态机 owner doc 漂移）同型，但 AR/AP 核销涉及 5 态辅助账+3 态核销单+审批状态机组合，散落更分散。
- `bad-debt.md` 坏账状态机与 `ErpFinBadDebtProcessor` reverseApprove 红冲闭环**一致**（§步骤 3/4a/6 + 状态含义表与代码 executeWriteOff/executeRecovery/reverseApprove 对称回退完全匹配）PASS。

### 项目特定维度 11：多币种核销辅助账本位币回写（P1-MA2-009 复核）— **维持 P1，不升 P0**

- `ReconciliationSettler.applySettlement:71-80` 用 `settledAmountFunctional`/`settledAmountSource` 双字段回写辅助账 `settled/openAmountFunctional/Source`——**双字段承载源币/本位币分离**（优于 `VoucherFact` 单 amount 字段 P1-MA2-002）。
- `ErpFinReconciliationLine` 有 `settledAmountSource`/`settledAmountFunctional` 双字段（ORM 确认）——schema 支持多币种。
- 缺口：调用方（核销 post / 收款过账）**未计算 FX plug**（`SalAcctDocProvider.RECEIPT` 无 6051 汇兑损益科目，P1-MA2-009）——核销时本位币金额折算缺失。
- 单币种路径（tested baseline）：`amountSource==amountFunctional`，辅助账余额正确。
- 多币种：辅助账忠实记录调用方提供的金额，但提供金额未含 FX plug——**非辅助账状态机破坏**，是上游 FX 功能缺口。
- **裁决：维持 P1-MA2-009，不升 P0**（单币种正确+多币种为未实现功能缺口，非运行时数据破坏）。MR1 与 P1-MA2-002 一并裁决（VoucherFact 双字段 + RECEIPT 过账补 6051 plug）。

### 项目特定维度 12：并发核销辅助账状态竞态（P2-MA2-008/014 复核）— **维持 P2 并降级**

- `ErpFinArApItem` ORM `versionProp="version"`（`app-erp-finance.orm.xml:732`，propId 23 column `version`）——Nop ORM 在 `updateEntity` flush 时自动乐观锁（`where version = oldVersion` + version+=1）。
- `ReconciliationSettler.applySettlement` 无 `@Version` 方法注解（计划假设"无 @Version"字面正确），但**实体级 versionProp 提供透明乐观锁**——计划假设"无锁致 SETTLED 判定漂移"被证据**降级**：
  - 并发两事务同读 ArApItem(version=N, openAmount=100)，各自 applySettlement(settled+=60)；
  - Tx1 flush 提交（version N→N+1）；
  - Tx2 flush 提交 → `where version=N` 不命中 → 抛 stale 异常 → tx 回滚；
  - **无静默丢失更新**，仅乐观冲突（用户需重试）。
- 同型域侧 `ReceiptSettler`/`PaymentSettler` 作用于域级实体（`ErpSalInvoice`/`ErpPurInvoice` 等同样有 versionProp）——同乐观锁保护。
- **裁决：P2-MA2-008/014 维持 P2 并降级**（silent lost-update → detectable optimistic conflict）。无自动重试/用户友好提示属 UX（watch-only），系统性并发正确性裁决归 A2.17。

---

## 4. 已登记 MA2 finding 运行时影响复核

| Finding | 原登记 | AR/AP 核销状态机运行时影响复核 | 终态 |
|---|---|---|---|
| P1-MA2-003 | 付款核销缺三单匹配完成态复核 | 域侧 `PaymentSettler.settle:80` 仅校验 invoice approveStatus=APPROVED，不复核三单匹配完成态。AR/AP 状态机层面：辅助账项状态迁移（OPEN→SETTLED）不依赖三单匹配标记——核销前置校验属域侧运营核销职责，非 finance 辅助账状态机裁决。 | **仅治理缺陷**（前置校验，非状态机破坏），维持 P1 |
| P1-MA2-009 | 多币种 O2C + 收款核销汇兑损益未实现 | 见维度 11。辅助账本位币回写忠实记录调用方金额，缺口在上游 FX plug。 | **仅治理/功能缺陷**（单币种正确，多币种未实现），维持 P1，不升 P0 |
| P1-MA2-002 | 多币种 P2P 本位币凭证路径未验证 | 辅助账 amountFunctional 与凭证一致性 A2.5a 已确认（`ErpFinArApItemGenerator` 用 PostingEvent.exchangeRate 折算）。P2P 核销侧同 P1-MA2-009（FX plug 缺口）。 | **仅治理缺陷**，维持 P1 |
| P2-MA2-008 | PaymentSettler 并发核销 lost-update | 见维度 12。ErpPurInvoice 等 versionProp 乐观锁保护，silent lost-update → detectable conflict。 | **降级**（detectable conflict），维持 P2，交接 A2.17 |
| P2-MA2-014 | ReceiptSettler 并发核销 lost-update | 同 P2-MA2-008（对称）。ErpFinArApItem versionProp 乐观锁保护。 | **降级**，维持 P2，交接 A2.17 |
| P2-MA2-013 | 订单维度核销未实现 | `SettlementAllocation`+`ReceiptSettler` 仅 invoiceId 维度，预收款 against order before invoice 未实现。AR/AP 状态机层面：预收/预付辅助账项（PAYMENT/RECEIPT 生成）存在且 OPEN，核销路径（匹配发票项）正常——订单维度核销是额外交互层，非状态机缺陷。 | **仅功能未实现**，维持 P2 |

---

## 5. 新发现汇总

### P2-MA2-036 — `ar-ap-reconciliation.md` owner doc 漂移

- **严重性**：P2（watch-only，无运行时影响）
- **位置**：`docs/design/finance/ar-ap-reconciliation.md §核销状态:139-152` + `§核销冲销:222-250` + `§异常处理:322`
- **问题**：(1) 发票核销状态 4 态（UNRECONCILLED/PARTIAL/RECONCILLED/OVER）vs dict 5 态（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF）命名+数量均漂移；(2) 收付款核销状态 3 态（UNRECONCILLED/PARTIAL/RECONCILLED）命名漂移；(3) OVER 状态 owner doc 声明但 dict 无 OVER 项（实现以 `assertNotOver` 拒绝超额，不设 OVER 状态）；(4) `§核销冲销` 描述 `reversalFlag=true` 布尔 vs 实现 `docStatus=REVERSED`（line 51-55 已部分注记）；(5) `§异常处理:322` "并发核销冲突 | 使用悲观锁保证数据一致性"——**owner doc 错误承诺**（实现依赖 ORM versionProp 乐观锁，无悲观锁）。
- **处置**：watch-only，MR1 顺手更新 owner doc——(a) `§核销状态` 表改为 dict 5 态命名（OPEN/PARTIAL/SETTLED/CANCELLED/WRITTEN_OFF），删除 OVER 行（超额核销以 `assertNotOver` 守卫表达，非状态）；(b) `§异常处理` 并发行改为"使用 ORM 乐观锁（versionProp）保证数据一致性，并发冲突抛 stale 异常需重试"。

### P2-MA2-037 — `state-machine.md` 缺 AR/AP 核销 + 坏账核销独立状态机章节

- **严重性**：P2（watch-only）
- **位置**：`docs/design/finance/state-machine.md`（仅对象一凭证+对象二期间，无对象三 AR/AP 核销）
- **问题**：AR/AP 核销（辅助账 5 态 + 核销单 3 态）+ 坏账核销（approveStatus×docType + reverseApprove 红冲闭环）状态机散落在 `ar-ap-reconciliation.md §核销状态` + `bad-debt.md §状态含义`，未在 `state-machine.md` 集中。与 P2-MA2-034（期间 owner doc 漂移）同型。
- **处置**：watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增"对象三：AR/AP 辅助账项状态机"+"对象四：坏账核销审批状态机"章节（本审计 §2.1/§2.3 状态图可直接采用）；方案 B 交叉链接到 ar-ap-reconciliation.md/bad-debt.md。

### P2-MA2-038 — 域侧/finance 双路径核销无对账守卫

- **严重性**：P2（watch-only，设计并行非缺陷）
- **位置**：`ReceiptSettler`/`PaymentSettler`（域侧）vs `ErpFinReconciliation`+`ReconciliationSettler`（finance）
- **问题**：两条核销路径独立并行（设计裁定，非分歧），但域侧 `receivedAmount`/`paidAmount` 与 finance `ErpFinArApItem.settledAmountFunctional` **无对账守卫**保证一致——理论上可 silently diverge（如域侧核销后未触发 finance 核销单，或反之）。
- **处置**：watch-only，MR1 裁决——方案 A 补 dual-side 对账断言（如 `DualSideConsistencyChecker` 已存在的扩展，定期校验域侧余额==辅助账余额）；方案 B owner doc 文档化并行关系 + 运营流程约束（域侧核销后须触发 finance 核销单）。

### P2-MA2-039 — `assertOpen` 不拒绝 WRITTEN_OFF（坏账状态机隔离不完整）

- **严重性**：P2（watch-only，config-gated）
- **位置**：`ErpFinReconciliationBizModel.assertOpen:308-315`
- **问题**：`assertOpen` 仅拒绝 SETTLED/CANCELLED，**不拒绝 WRITTEN_OFF**。`ReconciliationSettler.resolveStatus` 不查当前态，仅按 settled 金额裁决——理论上 WRITTEN_OFF 辅助账项可被纳入核销单行，`applySettlement` 将其覆写为 SETTLED/PARTIAL，破坏坏账状态机。实际阻断：默认 config `allow-over-reconcile=false` + WRITTEN_OFF 项 openAmount=0 → `assertNotOver` 拒绝（amt>0 > open=0）；仅 `allow-over-reconcile=true` 时可覆写。
- **处置**：watch-only，MR1 在 `assertOpen` 增 `WRITTEN_OFF` 拒绝（`Objects.equals(status, WRITTEN_OFF)` 抛 `ERR_RECONCILIATION_ITEM_NOT_OPEN`），完整隔离坏账状态机与核销状态机。

### P2-MA2-040 — 坏账 REJECTED 后无 resubmit 路径

- **严重性**：P2（watch-only，设计可接受）
- **位置**：`ErpFinBadDebtProcessor.validateTransitionForSubmit:238-243`
- **问题**：`validateTransitionForSubmit` 仅允许 UNSUBMITTED→SUBMITTED，REJECTED 不可重提。与 `ErpFinEmployeeAdvanceProcessor:86-87`（允许 REJECTED→SUBMITTED）不对称。实际无死锁：retry 经新建 writeOff/recover 记录（基于 ArApItem 状态而非坏账单状态）。
- **处置**：watch-only，MR1 裁决——方案 A 补 REJECTED→SUBMITTED 重提路径（与 EmployeeAdvance 对称）；方案 B owner doc 文档化"REJECTED 为本记录终态，重试经新建记录"。

### P2-MA2-041 — 核销 post/reverse 无期间 CLOSED_FINAL 守卫

- **严重性**：P2（watch-only，扩展 P1-MA2-021 范围）
- **位置**：`ErpFinReconciliationBizModel.post:116-140` / `reverse:144-157`
- **问题**：owner doc `ar-ap-reconciliation.md §异常处理:321` 声明"已结账期间核销 | 拒绝核销"，但 post/reverse 不校验期间 CLOSED_FINAL 状态（核销单无 periodId，仅 businessDate）。核销不动 GL 凭证（仅辅助账），无 GL 影响，但可对已结账期间辅助账继续核销/冲销。扩展 P1-MA2-021（CLOSED_FINAL 凭证锁定）范围至辅助账/核销。
- **处置**：watch-only，MR1 与 P1-MA2-021 一并裁决——post/reverse 增期间状态守卫（按 businessDate 解析期间，CLOSED_FINAL 拒绝）或 owner doc 标注"核销期间锁定经辅助账 periodId 间接保证"。

---

## 6. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 现状 | 交接 |
|---|---|---|---|
| 并发核销同一辅助账项 | `ReconciliationSettler.applySettlement` | ErpFinArApItem `versionProp="version"` 乐观锁保护（detectable conflict） | A2.17（P2-MA2-008/014 降级） |
| 域侧并发核销同一发票 | `ReceiptSettler.settle`/`PaymentSettler.settle` | 域级实体 versionProp 乐观锁保护 | A2.17（P2-MA2-008/014） |
| 坏账 reverseApprove 并发 | `ErpFinBadDebtProcessor.reverseApprove` | @BizMutation 事务 + ArApItem versionProp 乐观锁 | A2.17 |
| 核销单 post 并发（同核销单） | `ErpFinReconciliationBizModel.post` | `docStatus==DRAFT` 守卫 + 核销单 versionProp | A2.17 |
| 核销 post check-then-act（validateLine→applySettlement） | `ErpFinReconciliationBizModel` | 同事务 + ArApItem versionProp 乐观锁（validateLine 与 settler 共享 session 内同一实体实例） | A2.17 |

> **重要降级**：本审计发现 `ErpFinArApItem`（及域侧 `ErpSalInvoice`/`ErpPurInvoice` 等）均声明 `versionProp="version"`，Nop ORM 透明乐观锁将"silent lost-update 风险"降级为"detectable optimistic conflict"。A2.17 系统性并发审计时应纳入此事实，重新评估 P2-MA2-008/014 的实际严重性（可能整体降级为"需用户友好重试提示"UX 改进）。

---

## 7. 残留风险

1. **多币种核销 FX plug 未实现**（P1-MA2-009，MR1）——单币种正确，多币种辅助账本位币回写缺 FX plug。
2. **域侧/finance 双路径余额一致性**（P2-MA2-038）——无对账守卫，理论可 diverge。
3. **`assertOpen` 不拒绝 WRITTEN_OFF**（P2-MA2-039）——config-gated 隔离缺口。
4. **核销无期间 CLOSED_FINAL 守卫**（P2-MA2-041）——扩展 P1-MA2-021。
5. **owner doc 漂移**（P2-MA2-036/037）——命名/章节治理。
6. **并发重试 UX**（P2-MA2-008/014 降级后）——乐观冲突无自动重试/友好提示。

## 8. 审计范围声明

本审计严格限定 A2.5c scope = AR/AP 核销状态机（辅助账项 + 核销单 + 坏账核销三组件）。以下明确排除（Non-Goal）：

- 会计凭证状态机（A2.5a done）/ 期间/预算状态机（A2.5b done）；
- P2P/O2C 端到端编排正确性（A2.1/A2.2 done）——本审计只复核核销环节辅助账状态机迁移；
- 期末结账链路（A2.3 done）——本审计只确认 preCheck 未核销项扫描 + 坏账门控交互；
- finance 代码质量（A4.1b）——核销/坏账 Processor 代码质量（异常类型/N+1/索引）归 A4.1b；
- 并发与乐观锁系统性审计（A2.17）——本审计只标注并发敏感点；
- 银行对账独立状态机（`bank-match-status`，独立子系统）；
- owner doc 已裁定的 Non-Goal 子项（自动核销定时调度 / 订单维度核销预收款 / 多币种核销汇兑损益实现）。

## 9. 结论

AR/AP 核销状态机三组件（辅助账项 5 态 + 核销单 3 态 + 坏账审批状态机）核心契约经实仓逐项证据确认，状态迁移守卫齐全、事务边界清晰、红冲闭环对称、CANCELLED/WRITTEN_OFF 入口明确。**零 P0、零新 P1**；6 项新 P2 watch-only（owner doc 漂移 + 双路径治理 + 状态机隔离 config-gated 缺口 + 期间守卫扩展 + REJECTED 重提不对称）；MA2 finding 运行时复核**无升级**（P1-MA2-003/009/002 维持 / P2-MA2-008/014/013 维持并降级）；并发敏感点 5 处交接 A2.17（含 versionProp 乐观锁降级重要事实）。

**Verdict: pass**。A2.5c 完成，S 级状态机审查三拆分（凭证/期间-预算/AR-AP 核销）全部 done。
