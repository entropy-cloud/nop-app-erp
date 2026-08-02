# ARM MA2 — manufacturing 生产执行状态机业务审查（A2.6a，S 级拆分 1/2）

> Audit Status: closed
> Mission: audit-remediation
> Work Item: A2.6a manufacturing 状态机审查 — 工单与报工（S 级拆分 1/2）
> Source Plan: `docs/plans/2026-07-28-0109-1-audit-remediation-ma2-mfg-work-order-jobcard-state-machine.md`
> Skill: `docs/skills/state-machine-business-review-prompt.md`（+ 项目定制化层 `docs/skills/README.md §项目定制化层`）
> Reviewed: 2026-07-28
> Scope: **工单状态机**（`ErpMfgWorkOrder.docStatus` dict `erp-mfg/work-order-status` 10 态 + `approveStatus` 审批轴 + `posted`）+ **作业卡状态机**（`ErpMfgJobCard.status` dict `erp-mfg/job-card-status` 8 态）+ **领料单状态机**（`ErpMfgMaterialIssue.docStatus` dict `erp-mfg/issue-status` 4 态 + `posted`）+ **委外加工单状态机**（`ErpMfgSubcontractOrder.docStatus` dict `erp-mfg/subcontract-status` 8 态 + 审批轴 + `posted`/`postedStatus`）。MRP/预测/BOM 计划规划类状态机归 A2.6b（后续执行）。
> Related: A2.5a/b/c finance 状态机审查三拆分全 done（`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md` + `2026-07-27-2315-arm-ma2-finance-period-budget-state-machine.md` + `2026-07-27-2315-arm-ma2-finance-arap-settlement-state-machine.md`）；A2.1/A2.2 P2P/O2C 端到端 done（完工成本结转/委外加工费过账经 finance 凭证链已确认）；A2.4 库存核算一致性 done（完工入库/领料出库移动单 + 三方对账已确认）。

## 1. 裁决

**Verdict: pass（零 P0、1 项新 P1、3 项新 P2 watch-only）**

manufacturing 生产执行四组件状态机（工单 10 态 + 作业卡 8 态 + 领料 4 态 + 委外 8 态+审批轴）核心契约经实仓逐项证据确认：状态迁移守卫齐全（`requireStatus`/`validateTransition*` 前置校验）、`@BizMutation` 事务回滚保证过账失败时工单/作业卡/领料/委外单与库存移动单/GL 凭证一致性、委外 `reverseCompletion` 红冲闭环对称回退（三段 GL + 两段库存移动 + posted=false + docStatus=CANCELLED + 财务侧监听者兜底）、领料 `reverseConfirm` 红冲闭环对称（MANUFACTURING_ISSUE 凭证 + 反向 OUTGOING 移动单 + posted=false/CANCELLED）。

**关键裁决（计划假设证伪/确认）**：

| 计划假设 | 裁决 | 证据 |
|---|---|---|
| 作业卡 `PARTIALLY_TRANSFERRED`/`MATERIAL_TRANSFERRED` 不可达致 dict 死状态（候选 P0） | **部分证伪 + 升 P1** | 全 `src/main` grep `JOB_CARD_STATUS_*_TRANSFERRED` 仅 `ErpMfgConstants.java:52-53` + `_ErpMfgDaoConstants.java:69,74` 常量定义；`ErpMfgJobCardProcessor`（188 行全文读）`setStatus` 仅调 WORK_IN_PROGRESS/SUBMITTED/COMPLETED/ON_HOLD/CANCELLED 五值。**两 TRANSFERRED 态确认不可达（dict 死状态）**。但**不破坏状态机**——作业卡主路径（OPEN→WORK_IN_PROGRESS→SUBMITTED→COMPLETED）完整覆盖工序执行生命周期；转序迁移是 owner doc `state-machine.md §适用对象二` ASCII 迁移图声明的迁移但代码未落地（owner doc/代码漂移）。按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）同型裁决：dict 项不可达 + owner doc 声明但代码无实现 → **P1-MA2-035**（MR1 清理 dict 或落地迁移）。 |
| 委外 `reverseCompletion` 红冲失败致状态与凭证悬挂半状态（候选 P0） | **证伪** | `ErpMfgSubcontractOrderProcessor.reverseCompletion:233-240` 顺序：step1 `validateCanReverse`（COMPLETED + posted=true 守卫）→ step2 `reverseGlPostings`（三段凭证逐段 try/catch 吞异常告警）→ step3 `reverseInventoryMoves`（两段移动单逐段 try/catch + `canSafelyReverse` 前置）→ step4 `doReverseCompletion`（posted=false + docStatus=CANCELLED）。`@BizMutation` 事务边界 + 财务侧兜底 `MfgSubcontractReversalListener.onVoucherReversed:43-75`（监听 SUBCONTRACT_ISSUE/RECEIPT/FEE 红冲事件回退 posted=false + docStatus=CANCELLED）。**双路径保证最终一致**，无悬挂半状态。 |
| 部分齐套强制开工缺料后领料异常路径悬挂（候选 P0） | **证伪** | `ErpMfgWorkOrderProcessor.start:118-123`（STOCK_PARTIAL→IN_PROCESS，config `erp-mfg.allow-partial-kit-start` 默认 false）后 `ErpMfgMaterialIssueBizModel.confirm:88-128` 调 `stockMoveBiz.generateMove`——库存域在可用量不足时抛 `NopException` → `@BizMutation` 事务回滚 → 领料单 DRAFT 保持 + 工单 IN_PROCESS 保持。状态机有出口（异常抛出 → 事务回滚 → 状态保持），**无悬挂**。owner doc `state-machine.md §4 异常路径` 设计接受（"部分齐套开工后领料时可用量不足 → 拒绝本次领料，等待补库"）。 |
| 工单 INSPECTING 态 config-gated 钩子偏离破坏状态机（候选 P1） | **证伪** | owner doc `state-machine.md §质检约束声明` 引用工单 INSPECTING 态，但 dict `erp-mfg/work-order-status` 无此态。代码以 `reportCompletion:188-191` config-gated 钩子替代（`erp-mfg.inspection-gate-enabled` 默认 false）：若 `ErpMfgBom.inspectionRequired=true` 且 gate 开启且完工达量 → 抛 `ERR_INSPECTION_REQUIRED` 拒绝 COMPLETED，工单保持 IN_PROCESS。`state-machine.md §实现偏离补注` 已文档化此偏离。**有出口**（拒绝迁移保持原态），不破坏状态机。 |
| 委外 REJECTED 后无法重提（可达性候选缺陷） | **证伪** | `ErpMfgSubcontractOrderProcessor.validateTransitionForSubmit:341-350` 允许 `UNSUBMITTED` 或 `REJECTED` → `doSubmit:382-386` 置 SUBMITTED。REJECTED→SUBMITTED 路径**可达**（重提）。 |
| 工单 COMPLETED 不可恢复（终态无 reverseCompletion） | **设计裁定，非缺陷** | owner doc `state-machine.md §3 终态与恢复` 明示「终态不可直接恢复；若需纠正，新建返工工单」。与领料（DONE→CANCELLED 经 reverseConfirm）/委外（COMPLETED→CANCELLED 经 reverseCompletion）不对称是有意设计：工单完工经生产差异重算（`productionVarianceDispatcher.reverseIfExists` 幂等闭环）保护数值正确性，完工移动单 + MANUFACTURING_RECEIPT 凭证红冲归 successor（owner doc 已裁定）。**不登记为 finding**，仅在残余风险中记录。 |

### 1.1 审查范围

- **工单状态机**：`ErpMfgWorkOrder`（`orm.xml:570-665`，列 `docStatus`/`approveStatus`/`posted` 在 :599/600/604）+ `ErpMfgWorkOrderProcessor.java`（513 行，11 个迁移方法 + 守卫）+ `ErpMfgWorkOrderBizModel.java`（Facade，106 行）+ 5 个独立审批 Processor（SubmitForApproval/Approve/Reject/ReverseApprove/WithdrawApproval）。
- **作业卡状态机**：`ErpMfgJobCard`（`orm.xml:1305-1356`，列 `status` 在 :1316）+ `ErpMfgJobCardProcessor.java`（188 行，7 个迁移方法）+ `ErpMfgJobCardBizModel.java`（Facade，77 行）。
- **领料单状态机**：`ErpMfgMaterialIssue`（`orm.xml:976-1039`，列 `docStatus`/`approveStatus`/`posted` 在 :987/988/991）+ `ErpMfgMaterialIssueBizModel.java`（307 行，`confirm`/`reverseConfirm` + 守卫 `validateCanReverse`）。
- **委外加工单状态机**：`ErpMfgSubcontractOrder`（`orm.xml:1098-1179`，列 `docStatus`/`approveStatus`/`posted`/`postedStatus` 在 :1116/1117/1120/1121）+ `ErpMfgSubcontractOrderProcessor.java`（577 行，审批轴 5 + 业务动作 4 + 红冲闭环 4 step）+ `ErpMfgSubcontractOrderBizModel.java`（Facade，70 行）+ 5 个独立审批 Processor + 财务侧监听者 `MfgSubcontractReversalListener.java`（110 行）。
- **过账副作用（状态迁移的副作用，不破坏状态机裁决）**：`ManufacturingIssuePostingDispatcher.java` + `SubcontractPostingDispatcher.java` + `ProductionVarianceDispatcher.java`（含 `reverseIfExists` 幂等闭环）+ `MfgPostingExecutor.java`（统一红冲入口经 `IErpFinVoucherBiz.reverse`）+ 6 个 `IErpFinAcctDocProvider`（完工/领料/委外 SI/SR/SF + 生产差异）。
- **owner doc**：`docs/design/manufacturing/state-machine.md`（适用对象一工单 / 适用对象二作业卡 / 适用对象三委外 + §实现偏离补注 + §质检约束声明）+ `subcontracting.md` + `bom-and-routing.md` + `docs/design/inventory/cross-domain.md` + `docs/architecture/processor-extension-pattern.md` + `docs/architecture/posting-exemptions.md`。
- **测试**：`TestErpMfgWorkOrderStateMachine`（312 行，happy path/齐套/部分齐套/停工恢复关闭/取消/非法迁移/超报）+ `TestErpMfgWorkOrderEndToEnd`（含 JobCard 状态机 + 检验门控）+ `TestErpMfgSubcontracting` + `TestErpMfgSubcontractReverse`（红冲闭环）+ `TestErpMfgMaterialIssue` + `TestErpMfgMaterialIssueReversal`（领料红冲）+ `TestErpMfgCompletionPosting` + `TestErpMfgScheduleToJobCard`（APS 建卡状态门）+ `TestErpMfgVarianceRecomputeReversal`（生产差异重算幂等闭环）。

### 1.2 可达性摘要

- **工单 10 态全部可达**：DRAFT→SUBMITTED→NOT_STARTED→（STOCK_RESERVED 或 STOCK_PARTIAL）→IN_PROCESS→COMPLETED；IN_PROCESS→STOPPED→（IN_PROCESS 恢复 或 CLOSED）；NOT_STARTED/SUBMITTED/DRAFT→CANCELLED。无不可达终态、无死锁。
- **作业卡 8 态中 6 态可达，2 态不可达**：OPEN→WORK_IN_PROGRESS→SUBMITTED→COMPLETED；WORK_IN_PROGRESS↔ON_HOLD；OPEN/WORK_IN_PROGRESS/ON_HOLD→CANCELLED；**PARTIALLY_TRANSFERRED 与 MATERIAL_TRANSFERRED 无任何 `setStatus(..._TRANSFERRED)` 调用 → 不可达**（P1-MA2-035）。
- **领料 4 态全部可达**：DRAFT→CONFIRMED→DONE；DONE→CANCELLED 经 `reverseConfirm` 红冲恢复；DRAFT 废弃走 `useLogicalDelete=true`（无独立 CANCELLED 直迁，草稿废弃与红冲恢复语义分离）。
- **委外 8 态全部可达**：DRAFT→SUBMITTED→（APPROVED 或 REJECTED）；APPROVED→ISSUED→RECEIVED→COMPLETED；DRAFT/SUBMITTED/APPROVED→CANCELLED；REJECTED→SUBMITTED 经 `submit` 重提；COMPLETED→CANCELLED 经 `reverseCompletion` 红冲恢复（非真终态）。

### 1.3 角色/权限摘要

owner doc `state-machine.md §6` + `subcontracting.md` 定义工单/作业卡/委外角色矩阵（提交=计划员 / 审核=生产主管 / 开工=生产主管 / 报工=作业员 / 委外审核=采购员）。本审计未做权限绑定运行时验证（归 A4/A6 平台合规与权限审计），状态机层面无角色漂移反模式。危险操作门控完整：`close`（影响成本结转，owner doc 标注需管理员）/ `STOCK_PARTIAL→IN_PROCESS`（强制开工缺料风险，config `erp-mfg.allow-partial-kit-start` 默认 false 阻断）/ `reverseCompletion`（红冲恢复余额，需 posted=true 前置守卫）。

### 1.4 外部依赖摘要

- **库存写**：领料/完工/委外发料/委外收货均经 `IErpInvStockMoveBiz.generateMove`（跨域 I*Biz Facade）；posting dispatcher 跨域 `daoFor(ErpInv*)`/`daoFor(ErpMd*)` 只读（P1-MA1-022 已登记 MR1，本审计复核状态机异常路径无悬挂升级）。
- **finance 凭证链**：完工（MANUFACTURING_RECEIPT Dr 1401/Cr 1411）/ 领料（MANUFACTURING_ISSUE Dr 1411/Cr 1401）/ 委外三段（SI/SR/SF）/ 生产差异（PRODUCTION_VARIANCE）均经 `IErpFinVoucherBiz.reverse` 红冲入口与 `MfgPostingExecutor.reverse` 统一闭环。**production 代码无 `daoFor(ErpFin*)`**（45 匹配全在 `src/test`），符合 `AGENTS.md` 跨实体访问规则。
- **质检联动**（config-gated）：`InspectionTrigger.enforceGate` 在 `reportCompletion:194-201` 调用 quality 域 `IErpQaInspectionBiz`，gate=BLOCKED 时抛 `ERR_INSPECTION_REQUIRED`，工单保持 IN_PROCESS。
- **APS 排程建卡**（config-gated）：`ErpMfgScheduleToJobCardProcessor.generateJobCardsFromSchedule` 状态门齐全（NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/STOPPED 允许，DRAFT/SUBMITTED/终态拒绝）。
- **事务回滚**：所有状态迁移动作经 `@BizMutation`（Nop 平台自动事务包装），任一外部步骤失败（库存写/过账/质检）抛异常 → 事务回滚 → 状态保持事务开始前值，**保证强一致**。

### 1.5 剩余风险

- 作业卡两 TRANSFERRED 态 dict 死状态（P1-MA2-035，MR1）；
- 报工超产 config-gate 缺失（P2-MA2-042）；
- 领料状态机 owner doc 无独立章节（P2-MA2-043）；
- 字典命名漂移 `subcontract-status` vs 惯称 `subcontract-order-status`（P2-MA2-044）；
- 工单 COMPLETED 无 reverseCompletion（设计裁定 successor，非缺陷，仅观察记录）；
- 并发领料扣减同批次 / 并发 reportCompletion / 并发 reverseCompletion（owner doc `§异常路径` 列"乐观锁"——@Version 透明乐观锁覆盖评估交接 A2.17）。

---

## 2. 状态图与转换矩阵

### 2.1 工单状态机（`ErpMfgWorkOrder.docStatus`，dict `erp-mfg/work-order-status` 10 态 + 审批轴 + posted）

```
                         submitForApproval             approve
[DRAFT] ──────────────────────────► [SUBMITTED] ────────────────► [NOT_STARTED]
   │                                     │                              │
   │                                     │ reject                      │ checkAvailability
   │                                     ▼                              ▼
   │                                [approveStatus=REJECTED]   [STOCK_RESERVED] 或 [STOCK_PARTIAL]
   │                                     │                              │
   │                                     │ submit (重提)                │ start
   │                                     └──────► [SUBMITTED]           ▼
   │                                                                  [IN_PROCESS]
   │                                                                    │
   │                                                  ┌─────────────────┼─────────────┐
   │                                                  │                 │             │
   │                                                  ▼                 ▼             ▼
   │                                            [STOPPED]          reportCompletion  [CLOSED]
   │                                                  │              (达量)             (部分完工关闭)
   │                                                  │ resume          │
   │                                                  └──► [IN_PROCESS] ▼
   │                                                                 [COMPLETED]
   │
   └─────────────────────────────► [CANCELLED]  ◄─── (NOT_STARTED/SUBMITTED 亦可取消)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| DRAFT→SUBMITTED | submitForApproval | `WorkOrderProcessor:296-300`（doSubmit）+ `:286-288`（validateBusinessRulesForSubmit requireStatus DRAFT） | requireStatus DRAFT + 审批轴 UNSUBMITTED/REJECTED | PASS |
| SUBMITTED→NOT_STARTED | approve | `:307-313`（doApprove set approveStatus=APPROVED + docStatus=NOT_STARTED） | `:263-268`（validateTransitionForApprove 审批轴 SUBMITTED）+ `:290-292`（requireStatus SUBMITTED） | PASS |
| NOT_STARTED→STOCK_RESERVED | checkAvailability（齐套足） | `:109-116`（setDocStatus result.getResultingStatus()） | requireStatus NOT_STARTED + KitAvailabilityChecker 返回 RESERVED | PASS |
| NOT_STARTED→STOCK_PARTIAL | checkAvailability（齐套不足） | 同上（result.getResultingStatus()=PARTIAL） | 同上 | PASS |
| STOCK_RESERVED→IN_PROCESS | start | `:118-123` + `:344-350`（doStart） | `:329-340`（validateTransitionForStart：STOCK_RESERVED 直放 / STOCK_PARTIAL 需 config） | PASS |
| STOCK_PARTIAL→IN_PROCESS | start（强制） | 同上 | config `erp-mfg.allow-partial-kit-start` 默认 false 阻断；置 true 时放行 | PASS（config-gated 危险操作门控正确） |
| IN_PROCESS→COMPLETED | reportCompletion（达量） | `:219-223`（willFinish setDocStatus COMPLETED + actualEndDate） | requireStatus IN_PROCESS + 超量守卫（:181-185 ERR_OVER_REPORT）+ 质检 gate（:188-201）+ 完工移动单 + 差异重算 | PASS（超产 config-gate 缺失见 P2-MA2-042） |
| IN_PROCESS→STOPPED | stop | `:125-131` | requireStatus IN_PROCESS | PASS |
| STOPPED→IN_PROCESS | resume | `:133-139` | requireStatus STOPPED | PASS |
| STOPPED→CLOSED | close | `:141-154` | requireStatus STOPPED 或 IN_PROCESS | PASS（管理员级，影响成本结转） |
| IN_PROCESS→CLOSED | close（部分完工关闭） | 同上 | 同上 | PASS |
| NOT_STARTED/SUBMITTED/DRAFT→CANCELLED | cancel | `:156-167` | requireStatus DRAFT/SUBMITTED/NOT_STARTED | PASS（齐套校验只读不写预留，cancel 为纯状态迁移无预留释放，对齐 `state-machine.md §实现偏离补注`） |
| SUBMITTED→DRAFT（审批撤回） | withdrawApproval | `:80-85` + `:302-305`（doWithdrawSubmit） | `:256-261`（审批轴 SUBMITTED） | PASS |
| SUBMITTED→REJECTED（审批驳回） | reject | `:95-100` + `:315-318`（doReject set approveStatus=REJECTED，docStatus 不变） | `:270-275`（审批轴 SUBMITTED） | PASS（驳回保持 docStatus=SUBMITTED，重提经 submit） |
| APPROVED→REJECTED（反审核） | reverseApprove | `:102-107` + `:320-325`（doReverseApprove 清 approvedBy/At） | `:277-282`（审批轴 APPROVED） | PASS |

**工单终态**：COMPLETED / CLOSED / CANCELLED 三终态无出边（owner doc §3 明示需新建返工工单），不可恢复。

### 2.2 作业卡状态机（`ErpMfgJobCard.status`，dict `erp-mfg/job-card-status` 8 态）

```
[OPEN] ──startJob──► [WORK_IN_PROGRESS] ──submitJob──► [SUBMITTED] ──completeJob──► [COMPLETED]
                          │  ▲                                             
                          │  │ resumeJob                                   
                          ▼  │                                             
                      [ON_HOLD] ──────────────────────────────────────────── 
                          │                                                  
                  (OPEN / WORK_IN_PROGRESS / ON_HOLD) ──cancelJob──► [CANCELLED]

        [PARTIALLY_TRANSFERRED]   [MATERIAL_TRANSFERRED]    ❌ 不可达（dict 死状态）
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| (生成)→OPEN | 手工创建 / APS 排程建卡 | `ErpMfgScheduleToJobCardProcessor:215`（setStatus OPEN） | WorkOrder 状态门（NOT_STARTED/STOCK_RESERVED/STOCK_PARTIAL/IN_PROCESS/STOPPED 允许） | PASS |
| OPEN→WORK_IN_PROGRESS | startJob | `JobCardProcessor:37-43` | requireStatus OPEN | PASS |
| WORK_IN_PROGRESS→SUBMITTED | submitJob | `:73-83` | status WORK_IN_PROGRESS 或 ON_HOLD | PASS |
| SUBMITTED→COMPLETED | completeJob | `:85-91` | requireStatus SUBMITTED | PASS |
| WORK_IN_PROGRESS→ON_HOLD | holdJob | `:93-99` | requireStatus WORK_IN_PROGRESS | PASS |
| ON_HOLD→WORK_IN_PROGRESS | resumeJob | `:101-107` | requireStatus ON_HOLD | PASS |
| OPEN/WORK_IN_PROGRESS/ON_HOLD→CANCELLED | cancelJob | `:109-120` | status ∈ {OPEN, WORK_IN_PROGRESS, ON_HOLD} | PASS |
| recordWork（无状态变更） | recordWork | `:45-71`（写 TimeLog + laborCost 回写 WorkOrder） | status WORK_IN_PROGRESS 或 SUBMITTED | PASS（owner doc §4 重复报工幂等：实现经累加 completedQuantity，需应用层去重——交接 A2.17 并发审计） |
| **WORK_IN_PROGRESS→PARTIALLY_TRANSFERRED** | （未实现） | **无 setStatus 调用** | — | **FAIL → P1-MA2-035**（dict 死状态） |
| **WORK_IN_PROGRESS→MATERIAL_TRANSFERRED** | （未实现） | **无 setStatus 调用** | — | **FAIL → P1-MA2-035**（dict 死状态） |

**作业卡终态**：COMPLETED / CANCELLED。两终态无出边。

### 2.3 领料单状态机（`ErpMfgMaterialIssue.docStatus`，dict `erp-mfg/issue-status` 4 态 + posted）

```
[DRAFT] ──confirm──► [CONFIRMED] ──(stockMoveBiz DONE 后)──► [DONE]
                                                          │
                                                  reverseConfirm
                                                          ▼
                                                      [CANCELLED]  (posted=false)

[DRAFT 废弃] ──useLogicalDelete=true──► (无独立 CANCELLED 状态迁移)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| DRAFT→CONFIRMED→DONE | confirm | `MaterialIssueBizModel:88-128`（setDocStatus CONFIRMED → 生成 OUTGOING 移动单 → setDocStatus DONE） | requireStatus DRAFT + lines 非空 + DONE 幂等（重复 confirm 直接返回） | PASS（DONE 由 `stockMoveBiz.generateMove` 推进，flushSession 后落地当前事务连接） |
| DONE→CANCELLED（红冲恢复） | reverseConfirm | `:130-172`（reverseConfirm） | `validateCanReverse:249-256`（posted=true 且 DONE，未过账抛 ERR_MATERIAL_ISSUE_NOT_POSTED） | PASS（红冲闭环：step1 GL 凭证红冲 try/catch 吞异常 → step2 反向 OUTGOING 移动单 try/catch → step3 posted=false + docStatus=CANCELLED） |

**领料终态**：DONE（真终态不可恢复，仅红冲回 CANCELLED）/ CANCELLED（红冲恢复态）。

### 2.4 委外加工单状态机（`ErpMfgSubcontractOrder.docStatus`，dict `erp-mfg/subcontract-status` 8 态 + 审批轴 + posted）

```
                         submitForApproval            approve
[DRAFT] ──────────────────────────► [SUBMITTED] ─────────────► [APPROVED] ──issueMaterials──► [ISSUED]
   │                                     │                          │                            │
   │                                     │ reject                   │ cancel                     │ receiveFinished
   │                                     ▼                          ▼                            ▼
   │                                [REJECTED]                  [CANCELLED]               [RECEIVED]
   │                                     │                                                     │
   │                                     │ submit (重提)                                         │ postProcessingFee
   │                                     └──────► [SUBMITTED]                                    ▼
   │                                                                                          [COMPLETED]
   │                                                                                              │
   │                                                                                       reverseCompletion
   │                                                                                              ▼
   └─────────────────────────────► [CANCELLED]  ◄─── (SUBMITTED/APPROVED 亦可取消)         [CANCELLED] (posted=false)
```

| 迁移 | 触发 | 代码位置 | 守卫 | 裁决 |
|---|---|---|---|---|
| DRAFT→SUBMITTED | submitForApproval | `SubcontractOrderProcessor:89-94` + `:382-386`（doSubmit） | 审批轴 UNSUBMITTED 或 REJECTED | PASS |
| SUBMITTED→APPROVED | approve | `:103-108` + `:393-399`（doApprove） | 审批轴 SUBMITTED | PASS |
| SUBMITTED→REJECTED | reject | `:110-115` + `:401-405`（doReject set approveStatus=REJECTED + docStatus=REJECTED） | 审批轴 SUBMITTED | PASS |
| REJECTED→SUBMITTED | submit（重提） | `:89-94` + `:341-350`（validateTransitionForSubmit 允许 REJECTED） | 审批轴 REJECTED | PASS（重提路径可达） |
| APPROVED→ISSUED | issueMaterials | `:151-171`（generateIssueMove + dispatchIssuePosting + setStatus ISSUED） | requireStatus APPROVED + lines 非空 | PASS |
| ISSUED→RECEIVED | receiveFinished | `:185-205`（generateReceiptMove + dispatchReceiptPosting + setStatus RECEIVED） | requireStatus ISSUED + receivedQty 默认行汇总 | PASS |
| RECEIVED→COMPLETED | postProcessingFee | `:211-223`（dispatchFeePosting + setStatus COMPLETED） | requireStatus RECEIVED | PASS |
| COMPLETED→CANCELLED（红冲恢复） | reverseCompletion | `:233-240` + step1-4 | `validateCanReverse:247-255`（COMPLETED + posted=true） | PASS（红冲闭环对称） |
| DRAFT/SUBMITTED/APPROVED→CANCELLED | cancel | `:124-135` | status ∈ {DRAFT, SUBMITTED, APPROVED} | PASS |
| 审批轴 withdrawApproval / reverseApprove | withdrawApproval/reverseApprove | `:96-101`/`:117-122` | 审批轴 SUBMITTED/APPROVED | PASS |

**委外终态**：COMPLETED（可红冲回 CANCELLED，非真终态）/ CANCELLED（终态）/ REJECTED（可重提，非终态）。

---

## 3. 10 维度审查裁决

### 3.1 维度「状态定义」 — ⚠️ FAIL（作业卡两悬空态）

**工单 10 态语义清晰性**：每个状态清楚表达"等待什么"——STOCK_PARTIAL 是"等待补料或强制开工决策"（等待点，非动作），符合 `state-machine.md §1` 表定义。STOCK_RESERVED 是"等待开工"（齐套已确认）。COMPLETED/CLOSED/CANCELLED 三终态业务语义明确。**PASS**。

**委外 8 态**：REJECTED 设计为"等待修改重提"（非终态，可 submit 重提）；其他态语义清晰。**PASS**。

**作业卡**：OPEN/WORK_IN_PROGRESS/ON_HOLD/SUBMITTED/COMPLETED/CANCELLED 6 态语义清晰；**PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 2 态在 `ErpMfgJobCardProcessor` 无任何 setStatus 调用**——dict 项悬空（owner doc §适用对象二 ASCII 迁移图声明但代码未落地），按"动作作为状态"反模式裁决：转序是动作，其结果状态应在工单维度表达（多工序工单的工序卡完工即代表转序完成），而非独立 JobCard 状态。**FAIL → P1-MA2-035**。

**领料 4 态**：DRAFT/CONFIRMED/DONE/CANCELLED 完整。DRAFT 废弃走 useLogicalDelete=true（无独立 CANCELLED 直迁——草稿废弃与红冲恢复语义分离，符合 Nop 平台范式）。reverseConfirm 回 CANCELLED 是红冲恢复而非草稿废弃。**PASS**。

### 3.2 维度「转换完整性」 — ⚠️ FAIL（作业卡转序迁移未实现 + owner doc 声明漂移）

**工单**：迁移矩阵（§2.1）覆盖 15 个迁移路径，每个有前置守卫与触发人。INSPECTING 态 owner doc `§质检约束声明` 引用但 dict 无此态——`state-machine.md §实现偏离补注` 已文档化为 config-gated 钩子（`reportCompletion` 抛 ERR_INSPECTION_REQUIRED 拒绝 COMPLETED，保持 IN_PROCESS）。**有出口，不破坏状态机。PASS**。

**作业卡**：6 个已实现迁移守卫齐全；**2 个转序迁移（WORK_IN_PROGRESS→PARTIALLY_TRANSFERRED / WORK_IN_PROGRESS→MATERIAL_TRANSFERRED）owner doc `state-machine.md §适用对象二` ASCII 迁移图明确声明但代码无实现**——owner doc/代码漂移。**FAIL → P1-MA2-035**。

**领料**：DRAFT→CONFIRMED→DONE 主路径 + DONE→CANCELLED 红冲路径完整，前置守卫齐全（`validateCanReverse`）。**PASS**。

**委外**：3 段业务动作（issueMaterials/receiveFinished/postProcessingFee）+ 审批轴（submit/approve/reject/reverseApprove/withdrawApproval）+ cancel + reverseCompletion 全覆盖；reverseCompletion 红冲路径状态回写一致（COMPLETED→CANCELLED + posted=false）。**PASS**。

### 3.3 维度「终端状态和恢复」 — ✅ PASS

**工单三终态**（COMPLETED/CLOSED/CANCELLED）：无出边，不可直接恢复（owner doc §3 明示需新建返工工单）。验证：`WorkOrderProcessor` 无 reOpen/reverse 路径。**PASS**。

**作业卡两终态**（COMPLETED/CANCELLED）：无出边。completeJob 单向迁移无 reverse——与工单 COMPLETED 对称（owner doc 设计接受）。**PASS**。

**领料 DONE**：经 `reverseConfirm` 可回 CANCELLED（红冲恢复，非真终态）；CANCELLED 是真终态。**PASS**。

**委外 COMPLETED**：经 `reverseCompletion` 可回 CANCELLED（红冲恢复，非真终态）；CANCELLED 是真终态；REJECTED 可重提（非终态）。**PASS**。

**归档与活动区分**：所有单据经 `posted`/`postedStatus` 布尔 + `docStatus` 字段双层区分（active = 非终态 + posted=false；archived = 终态或 posted=true）。**PASS**。

### 3.4 维度「异常路径」 — ✅ PASS

逐项核验：

| 异常场景 | 处理 | 证据 | 裁决 |
|---|---|---|---|
| 齐套校验子件不足 | →STOCK_PARTIAL 等待补料或强制开工 | `KitAvailabilityChecker` 返回 PARTIAL | PASS |
| 部分齐套强制开工后领料缺料 | confirm 抛异常 → @BizMutation 事务回滚 → 工单 IN_PROCESS 保持 / 领料 DRAFT 保持 | `MaterialIssueBizModel.confirm:108-110`（stockMoveBiz 抛 NopException） | PASS（状态机有出口） |
| 报工数量超过工单数量 | 拒绝（ERR_OVER_REPORT） | `WorkOrderProcessor.reportCompletion:181-185` 硬编码拒绝 | ⚠️ owner doc `§4 异常路径` 承诺「除非配置允许超产」——实现无 config-gate 允许超产（P2-MA2-042） |
| BOM 变更影响已开工工单 | 已开工不追溯（快照原则） | 工单创建时记录 bomId，运行时通过 `wo.getBom()` 读取快照 | PASS（owner doc 设计接受） |
| 并发领料扣减同批次 | owner doc 列"乐观锁" | ErpMfgMaterialIssue `versionProp="version"`（`orm.xml:1000`）+ ErpInvStockBalance `versionProp`（库存域）乐观锁 detectable conflict | 交接 A2.17（@Version 透明乐观锁覆盖评估） |
| 重复报工幂等 | owner doc §4 声明已报工工序再次提交为空操作 | `JobCardProcessor.recordWork:62-66` 累加 completedQuantity（非幂等空操作）——需应用层去重 | ⚠️ 残留观察（不破坏状态机，作业卡状态不迁移；交接 A2.17 并发与幂等审计） |
| 委外 reverseCompletion 非已完成单拒绝 | validateCanReverse 守卫 | `SubcontractOrderProcessor.validateCanReverse:247-255` 抛 ERR_SUBCONTRACT_CANNOT_REVERSE | PASS |
| reverseConfirm 未过账领料单拒绝 | validateCanReverse 守卫 | `MaterialIssueBizModel.validateCanReverse:249-256` 抛 ERR_MATERIAL_ISSUE_NOT_POSTED | PASS |
| 外部过账失败 | @BizMutation 事务回滚覆盖状态+凭证+移动单一致性 | 所有 mutation 经平台事务；`MaterialIssueBizModel.confirm` 中 `stockMoveBiz.generateMove` 在 DONE 前抛异常 → 全回滚 | PASS |

### 3.5 维度「可达性」 — ⚠️ FAIL（作业卡两态不可达）

**作业卡**：PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 不可达（§2.2 表 FAIL 行）——dict 项死状态，按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）同型裁决 **P1-MA2-035**。其余 6 态全部可达。

**工单**：DRAFT 主路径可达 COMPLETED；分支可达 STOPPED→CLOSED、CANCELLED；STOCK_PARTIAL 经强制开工或补料后可达 IN_PROCESS；无不可达终态、无死循环。**PASS**。

**领料**：DRAFT→CONFIRMED→DONE 可达；DONE→CANCELLED 经 reverseConfirm 可达；DRAFT 废弃经 useLogicalDelete。**PASS**。

**委外**：DRAFT→SUBMITTED→APPROVED/REJECTED→ISSUED→RECEIVED→COMPLETED 全可达；COMPLETED→CANCELLED 经 reverseCompletion 可达；REJECTED→SUBMITTED 重提可达。**PASS**。

### 3.6 维度「角色和权限」 — ✅ PASS

工单（§1.3）+ 委外（`subcontracting.md`）+ 作业卡（owner doc §适用对象二「与工单类似」）+ 领料（同工单）角色矩阵 owner doc 完整定义。状态机层面无角色漂移反模式（每个迁移绑定执行角色，无审核员执行最终关闭操作）。本审计未做权限注解运行时验证（归 A4/A6 平台合规与权限审计）。**PASS**。

**危险操作门控**：
- `close`（影响成本结转）：owner doc §6 标注需管理员；
- `STOCK_PARTIAL→IN_PROCESS`（强制开工缺料风险）：config `erp-mfg.allow-partial-kit-start` 默认 false 阻断；
- `reverseCompletion`（红冲恢复余额）：posted=true 前置守卫；
- `reverseConfirm`（领料红冲）：posted=true + DONE 前置守卫。

### 3.7 维度「外部依赖」 — ✅ PASS

- **领料/完工/委外写库存**：全经 `IErpInvStockMoveBiz.generateMove`（跨域 I*Biz Facade）；posting dispatcher 跨域 `daoFor(ErpInv*)`/`daoFor(ErpMd*)` 只读（P1-MA1-022 已登记 MR1，本审计复核异常路径无悬挂升级——过账失败经 try/catch + @BizMutation 事务回滚覆盖跨域写一致性）。
- **成本结转/加工费过账经 finance 凭证链**：production 代码**无 `daoFor(ErpFin*)`**（45 匹配全在 `src/test`），全经 `IErpFinVoucherBiz.reverse` 红冲入口与 `MfgPostingExecutor.reverse` 统一闭环。
- **完工质检 quality 域联动**（config-gated）：`InspectionTrigger.enforceGate` 在 `reportCompletion:194-201` 调用 `IErpQaInspectionBiz`，gate=BLOCKED 抛 ERR_INSPECTION_REQUIRED 保持 IN_PROCESS。
- **工作中心停机 maintenance 域**：状态机层面不直接耦合（owner doc `§7` 注记为人工决策停工或等待）。
- **APS 排程建卡**（config-gated）：`generateJobCardsFromSchedule` 状态门齐全。
- **外部步骤失败阻断状态迁移**：@BizMutation 事务回滚保证。**PASS**。

### 3.8 维度「TODO/任务策略」 — ✅ PASS（设计接受无自动 TODO）

owner doc `state-machine.md §8` 定义工单各非终态的 TODO 类型（DRAFT assigned/SUBMITTED pool/STOCK_PARTIAL assigned 缺料待补/IN_PROCESS monitor/STOPPED assigned）。当前实现**无自动 TODO 生成**——这与 nop-app-erp 整体设计一致（无统一 TODO 子系统，运营经仪表板/列表筛选发现滞留单据）。owner doc 表是设计意图声明（"应产生"），实现是手工运营工作流。**裁决**：设计接受（与全域一致），不构成本审计 scope 内的状态机缺陷。**PASS**。

> 残留观察：STOCK_PARTIAL 长期滞留静默下沉是运营关注点（owner doc §8 已声明需 TODO 提醒），归 nop-app-erp 运营成熟度后续阶段（非状态机业务正确性问题）。

### 3.9 维度「场景演练」 — ✅ PASS（含 9 个场景）

#### 场景 A：工单快乐路径（DRAFT→SUBMITTED→NOT_STARTED→STOCK_RESERVED→IN_PROCESS→领料→报工→COMPLETED+成本结转凭证）

证据：`TestErpMfgWorkOrderStateMachine` happy path + `TestErpMfgWorkOrderEndToEnd` + `TestErpMfgCompletionPosting`。流程：提交→审核→齐套校验（RESERVED）→开工→领料（出库移动单 + MANUFACTURING_ISSUE 凭证）→报工（JobCard recordWork 累加 laborCost）→完工达量（COMPLETED + 完工入库移动单 + MANUFACTURING_RECEIPT 凭证 Dr 1401/Cr 1411 + 生产差异 config-gated 重算）。**PASS**。

#### 场景 B：部分齐套强制开工（STOCK_PARTIAL→IN_PROCESS 缺料后续补领）

证据：`TestErpMfgWorkOrderStateMachine` 部分齐套场景。流程：齐套校验返回 PARTIAL → config `allow-partial-kit-start=true` 允许强制开工 → 后续领料缺料时 confirm 抛异常 + 事务回滚 → 工单保持 IN_PROCESS 等待补库。**PASS**。

#### 场景 C：停工/恢复/关闭（IN_PROCESS→STOPPED→IN_PROCESS 或 →CLOSED 部分完工结转成本）

证据：`TestErpMfgWorkOrderStateMachine` 停工恢复关闭场景。流程：stop（IN_PROCESS→STOPPED）→ resume（STOPPED→IN_PROCESS）或 close（部分完工结转 →CLOSED）。**PASS**。

#### 场景 D：完工质检不合格返工（config-gated 钩子阻止 COMPLETED + 新建返工工单）

证据：`TestErpMfgWorkOrderEndToEnd` 检验门控场景 + `state-machine.md §实现偏离补注 §INSPECTING 态字典缺失`。流程：BOM inspectionRequired=true + config `inspection-gate-enabled=true` + 完工达量 → 抛 ERR_INSPECTION_REQUIRED 工单保持 IN_PROCESS → 质检结果 REJECTED → 新建返工工单（关联原工单）。**PASS**。

#### 场景 E：作业卡全生命周期（OPEN→WORK_IN_PROGRESS→SUBMITTED→COMPLETED）+ 转序（当前未实现）

证据：`TestErpMfgWorkOrderEndToEnd` JobCard 状态机场景。流程：startJob → recordWork（累加 laborCost 回写 WorkOrder）→ submitJob → completeJob。**转序迁移（→PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED）未实现**——dict 项死状态（P1-MA2-035）。多工序工单的工序卡完工即代表转序完成（设计可接受的简化），不破坏主路径。**PASS（主路径）+ FAIL（转序迁移 → P1-MA2-035）**。

#### 场景 F：领料确认→过账 + 领料红冲（reverseConfirm 红冲 MANUFACTURING_ISSUE 凭证+反向 OUTGOING 移动单+posted=false/CANCELLED）

证据：`TestErpMfgMaterialIssue` + `TestErpMfgMaterialIssueReversal`。流程：confirm（DRAFT→CONFIRMED→DONE + 出库移动单 + MANUFACTURING_ISSUE 凭证 + posted=true）→ reverseConfirm（红冲凭证 + 反向移动单 + posted=false/CANCELLED）。**PASS**。

#### 场景 G：委外 3 段（发料→收货→加工费过账→COMPLETED）+ reverseCompletion 红冲闭环（状态+凭证对称回退）

证据：`TestErpMfgSubcontracting` + `TestErpMfgSubcontractReverse`。流程：issueMaterials（APPROVED→ISSUED + OUTGOING 移动单 + SI 凭证）→ receiveFinished（ISSUED→RECEIVED + MANUFACTURING 入库移动单 + SR 凭证）→ postProcessingFee（RECEIVED→COMPLETED + SF 凭证 + posted=true）→ reverseCompletion（COMPLETED→CANCELLED + 红冲 SI/SR/SF 三段 + 反向两段移动单 + posted=false）。**PASS**。

#### 场景 H：委外审核驳回→REJECTED→重提

证据：`SubcontractOrderProcessor.validateTransitionForSubmit:341-350`（允许 REJECTED→SUBMITTED）+ `doReject:401-405`（set docStatus=REJECTED）。流程：submit→SUBMITTED→reject→REJECTED→submit（重提）→SUBMITTED→approve→APPROVED。**PASS**。

#### 场景 I：并发领料扣减同批次（@Version 覆盖评估，交接 A2.17）

owner doc `§4 异常路径` 列"乐观锁"——`ErpMfgMaterialIssue.versionProp="version"`（`orm.xml:1000`）+ 库存域 `ErpInvStockBalance.versionProp` 透明乐观锁保护。并发扣减同批次：两事务同读 version=N，后提交事务 flush 时 version 不匹配抛 stale 异常 → 无静默丢失更新，仅乐观冲突（需重试）。**交接 A2.17**（系统性并发审计）。

### 3.10 维度「与设计文档一致性」 — ⚠️ FAIL（4 处漂移）

| # | 漂移 | owner doc | 代码 | 裁决 |
|---|---|---|---|---|
| 1 | **作业卡 dict 含 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 但 Processor 无 setStatus** | `state-machine.md §适用对象二` ASCII 迁移图声明「部分转序 → 部分转序 (PARTIALLY_TRANSFERRED)」「转序 → 已转序 (MATERIAL_TRANSFERRED)」 | `ErpMfgJobCardProcessor` 188 行全文读无 TRANSFERRED 的 setStatus；grep 全域仅 ErpMfgConstants.java:52-53 + _ErpMfgDaoConstants.java:69,74 常量定义零使用 | **P1-MA2-035**（dict 死状态 + owner doc/代码漂移） |
| 2 | 工单 INSPECTING 态 owner doc §质检约束声明引用但 dict 无此态 | `state-machine.md §质检约束声明`（INSPECTING→COMPLETED/CONTINUE） | dict `erp-mfg/work-order-status` 无 INSPECTING；`reportCompletion:188-191` config-gated 钩子抛 ERR_INSPECTION_REQUIRED 保持 IN_PROCESS | **PASS**（`state-machine.md §实现偏离补注 §INSPECTING 态字典缺失` 已文档化，config-gated 钩子有出口不破坏状态机） |
| 3 | 委外 `subcontracting.md` 设计 10 态 vs 实现 8 态 | `subcontracting.md §状态机设计`（10 态含 PRODUCED/RETURNED） | dict `erp-mfg/subcontract-status` 8 态（舍 PRODUCED/RETURNED） | **PASS**（`subcontracting.md §实现偏离补注` 已文档化舍 2 态归 successor） |
| 4 | 字典名 `erp-mfg/subcontract-status` vs roadmap/owner doc 惯称 `subcontract-order-status` | roadmap/owner doc 惯称 `subcontract-order-status` | dict name = `erp-mfg/subcontract-status`（`orm.xml:100`） | **P2-MA2-044**（命名漂移，无运行时影响） |
| 5 | 领料状态机 owner doc 散落在 `§实现偏离补注`（无独立领料状态机章节） | `state-machine.md §实现偏离补注 §领料红冲实现注记`（散落） | `ErpMfgMaterialIssueBizModel` 实现完整 4 态 | **P2-MA2-043**（owner doc 组织问题，无运行时影响） |

---

## 4. 已登记 finding 运行时影响复核

| Finding ID | 原描述 | 状态机角度复核 | 终态裁决 |
|---|---|---|---|
| `P1-MA1-001` | `ErpMfgWorkOrder`/`ErpMfgMaterialIssue` 多币种四件套 7 列 propId 缺失 | 状态机判定字段（`docStatus` WorkOrder propId=26 / MaterialIssue propId=8；`approveStatus` 27/9；`posted` 29/10）均**有 propId**。缺 propId 的 `exchangeRate`/`amountSource`/`amountFunctional` 是成本/币种字段，不参与状态机判定。 | **仅规范缺陷**（mechanical），状态机角度**无升级**，维持 P1 |
| `P1-MA1-022` | posting dispatcher 跨域只读 `daoFor(ErpInv*)`/`daoFor(ErpMd*)`（9 域合并） | manufacturing posting dispatcher（`ManufacturingIssuePostingDispatcher`/`SubcontractPostingDispatcher`/`ProductionVarianceDispatcher`）跨域只读访问 ErpInvStockMove/StockLedger + ErpMdMaterial。状态机角度：跨域访问是状态迁移的副作用（过账/库存写），不破坏状态机裁决本身。异常路径：posting dispatcher 失败时经 try/catch（`MfgPostingExecutor.reverse` 内部 try/catch + `dispatchIfApplicable`/`reverse` 调用方 try/catch）+ @BizMutation 事务回滚保证一致性。 | **仅治理缺陷**，状态机角度**无悬挂升级**，维持 P1 |

> 复核结论：已登记 finding 在生产执行状态机运行时**无行为升级**。P1-MA1-001 是 ORM 规范缺陷（propId 缺失不参与状态机判定）；P1-MA1-022 跨域只读是状态迁移副作用（异常路径经事务回滚覆盖）。

---

## 5. 新发现汇总

### P1-MA2-035 — 作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态（不可达 + owner doc 声明漂移）

- **严重性**：P1（major）
- **位置**：`module-manufacturing/model/app-erp-manufacturing.orm.xml:50-51`（dict `erp-mfg/job-card-status` 含 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED）+ `ErpMfgConstants.java:52-53` + `_ErpMfgDaoConstants.java:69,74`（常量定义零使用）+ `ErpMfgJobCardProcessor.java`（188 行无 setStatus(..._TRANSFERRED) 调用）
- **问题**：作业卡 dict 8 态中 PARTIALLY_TRANSFERRED 与 MATERIAL_TRANSFERRED **无任何代码路径可达**——`ErpMfgJobCardProcessor` 的 setStatus 仅调 WORK_IN_PROGRESS/SUBMITTED/COMPLETED/ON_HOLD/CANCELLED 五值。`docs/design/manufacturing/state-machine.md §适用对象二` ASCII 迁移图明确声明「部分转序 → 部分转序 (PARTIALLY_TRANSFERRED)」「转序 → 已转序 (MATERIAL_TRANSFERRED)」迁移，但代码未落地（owner doc/代码漂移）。按 finance A2.5a P1-MA2-031（DRAFT→CANCELLED 不可达）同型裁决：dict 项不可达 + owner doc 声明但代码无实现。
- **重要性原因**：dict 项不可达致查询筛选语义混乱（UI 按 dict 渲染状态选项包含永不到达的状态）+ owner doc 声明的转序迁移缺失（多工序工单的转序协同无显式状态支持）。不破坏主路径（OPEN→WORK_IN_PROGRESS→SUBMITTED→COMPLETED 完整覆盖工序执行生命周期；多工序工单的工序卡完工即代表转序完成，是设计可接受的简化）。
- **处置**：MR1 裁决——方案 A（推荐）删除 dict `erp-mfg/job-card-status` 中 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED 两项 + 删除 ErpMfgConstants.java:52-53 + _ErpMfgDaoConstants.java:69,74 常量 + owner doc `state-machine.md §适用对象二` ASCII 迁移图标注「转序经工序卡完工表达，无独立状态」；方案 B 实现转序迁移（多工序工单的工序间物料转移单 + setStatus(PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED) + owner doc 已声明的迁移图）。方案 A 更符合实际业务（多工序工单转序是工序卡完工的副作用，无独立状态必要）。

### P2-MA2-042 — 报工超产 config-gate 缺失（owner doc 承诺允许超产，实现硬编码拒绝）

- **严重性**：P2（watch-only，「承诺但无证据」控制点）
- **位置**：`ErpMfgWorkOrderProcessor.reportCompletion:181-185`
- **问题**：owner doc `state-machine.md §4 异常路径` 写「报工数量超过工单数量 | 拒绝（除非配置允许超产）」。实现 `reportCompletion` 硬编码拒绝（`newCompleted.compareTo(planned) > 0` 抛 ERR_OVER_REPORT），无 config-gate 允许超产。
- **处置**：watch-only，MR1 裁决——方案 A 补 config `erp-mfg.allow-over-production`（默认 false）+ 超产阈值参数；方案 B owner doc 标注「当前硬编码拒绝超产，允许超产归 successor」为已知简化。

### P2-MA2-043 — 领料状态机 owner doc 无独立章节（散落 §实现偏离补注）

- **严重性**：P2（watch-only，文档组织问题）
- **位置**：`docs/design/manufacturing/state-machine.md`（适用对象一工单 + 适用对象二作业卡 + 适用对象三委外，无适用对象四领料）
- **问题**：领料单状态机（4 态 + reverseConfirm 红冲闭环）owner doc 散落在 `state-machine.md §实现偏离补注 §领料红冲实现注记` + `inventory/cross-domain.md §余量校验规则`，无独立领料状态机章节。与 P2-MA2-037（finance state-machine.md 缺 AR/AP 核销 + 坏账核销独立章节）同型。
- **处置**：watch-only，MR1 顺手——方案 A（推荐）`state-machine.md` 新增"适用对象四：领料单状态机"章节（本审计 §2.3 状态图可直接采用）；方案 B 交叉链接到 `inventory/cross-domain.md`。

### P2-MA2-044 — 字典命名漂移 `erp-mfg/subcontract-status` vs 惯称 `subcontract-order-status`

- **严重性**：P2（watch-only，无运行时影响）
- **位置**：`module-manufacturing/model/app-erp-manufacturing.orm.xml:100`（dict name = `erp-mfg/subcontract-status`）+ roadmap/owner doc 惯称 `subcontract-order-status`
- **问题**：字典名 `erp-mfg/subcontract-status` 与 roadmap/owner doc 惯称 `subcontract-order-status` 漂移。其他 mfg 字典命名规范一致（`work-order-status`/`job-card-status`/`issue-status`/`mrp-status`/`forecast-status`/`cost-rollup-status`），仅 subcontract 简写为 `subcontract-status` 而非完整 `subcontract-order-status`。
- **处置**：watch-only，MR1 顺手——方案 A 统一为 `subcontract-order-status`（需数据迁移）；方案 B owner doc/roadmap 改用 `subcontract-status`（无运行时影响，推荐）。

---

## 6. 并发敏感点（交接 A2.17）

| 敏感点 | 位置 | 现状 | 交接 |
|---|---|---|---|
| 并发领料扣减同批次 | `MaterialIssueBizModel.confirm` → `stockMoveBiz.generateMove` | `ErpMfgMaterialIssue.versionProp="version"` + `ErpInvStockBalance.versionProp` 透明乐观锁 detectable conflict | A2.17（owner doc `§4` 列"乐观锁"） |
| 并发 reportCompletion（同工单多作业卡同时报工） | `WorkOrderProcessor.reportCompletion` + `JobCardProcessor.applyLaborCostToWorkOrder` | `ErpMfgWorkOrder.versionProp="version"` 乐观锁保护（laborCost/materialCost 累加 read-modify-write）；同事务内多个 JobCard recordWork 串行调用 applyLaborCostToWorkOrder | A2.17 |
| 并发 reverseCompletion（委外红冲与财务侧监听者并发） | `SubcontractOrderProcessor.reverseCompletion` + `MfgSubcontractReversalListener.onVoucherReversed` | `ErpMfgSubcontractOrder.versionProp="version"` 乐观锁；监听者与域级 reverseCompletion 经 posted=true 守卫幂等（监听者 posted==false 时 no-op） | A2.17 |
| 重复报工幂等 | `JobCardProcessor.recordWork:62-66` | 累加 completedQuantity（非幂等空操作）——需应用层去重（如同一 operatorId+workDate+operationId 检查） | A2.17 |
| 并发 confirm（同领料单） | `MaterialIssueBizModel.confirm` | `ErpMfgMaterialIssue.versionProp="version"` 乐观锁 + DONE 幂等守卫（status==DONE 直接返回） | A2.17 |

> **重要降级**：本审计发现 `ErpMfgWorkOrder`/`ErpMfgMaterialIssue`/`ErpMfgSubcontractOrder` 均声明 `versionProp="version"`，Nop ORM 透明乐观锁将"silent lost-update 风险"降级为"detectable optimistic conflict"。A2.17 系统性并发审计时应纳入此事实。

---

## 7. 残留风险

1. **作业卡两 TRANSFERRED 态 dict 死状态**（P1-MA2-035，MR1）——多工序工单转序经工序卡完工表达，dict 项不可达需清理。
2. **报工超产 config-gate 缺失**（P2-MA2-042）——承诺但无证据控制点。
3. **领料 owner doc 无独立章节**（P2-MA2-043）+ **字典命名漂移**（P2-MA2-044）——owner doc 治理。
4. **工单 COMPLETED 无 reverseCompletion**（设计裁定 successor，非缺陷）——与领料/委外/采购/销售不对称是有意设计（owner doc §3 明示需新建返工工单）；完工经生产差异重算 `reverseIfExists` 幂等闭环保护数值正确性，完工移动单 + MANUFACTURING_RECEIPT 凭证红冲归 successor（owner doc 已裁定）。
5. **重复报工幂等**（残留观察）——`recordWork` 累加而非幂等空操作，需应用层去重；交接 A2.17。
6. **并发敏感点 5 处交接 A2.17**（含 @Version 透明乐观锁降级重要事实）。

## 8. 审计范围声明

本审计严格限定 A2.6a scope = manufacturing 生产执行状态机（工单 + 作业卡 + 领料 + 委外四组件）。以下明确排除（Non-Goal）：

- **A2.6b MRP/BOM/预测状态机**（S 级拆分 2/2，后续执行）——本审计只确认生产执行不依赖计划状态机的正确性（生产执行消费 MRP 释放的工单，但状态机独立）；
- **A2.1/A2.2 P2P/O2C 端到端编排正确性**（done）——本审计只复核完工成本结转/委外加工费过账与 finance 凭证链的**状态机迁移**正确性（过账是状态迁移的副作用）；
- **A2.5 finance 状态机**（done）——本审计只确认制造过账经 finance I*Biz/凭证链（非制造域直接写 finance 实体——production 代码无 `daoFor(ErpFin*)` 已确认）；
- **A4.2a/b manufacturing 代码质量**——工单/委外 Processor 代码质量（异常处理/N+1/索引/辅助方法）系统性审查归 A4.2a/b；本审计只做状态机业务正确性审查；
- **A2.17 并发与乐观锁**（并发领料扣减同批次）——owner doc `§异常路径` 列"乐观锁"，实际 @Version 覆盖评估交接 A2.17；本审计只标注观察到的并发敏感点；
- **config-gated 偏离本身是否应实现**（INSPECTING 态 / overhead 分配率 / 委外 PRODUCED/RETURNED / APS 自动建卡）——owner doc 已裁定的 Non-Goal/successor，本审计只确认其 config-gated 钩子在状态机上不引入悬挂；
- **owner doc 已裁定的 Non-Goal 子项**（工单 reverseCompletion / 作业卡 completeJob reverse / 领料 DRAFT 废弃状态迁移）。

## 9. 结论

manufacturing 生产执行四组件状态机（工单 10 态 + 作业卡 8 态 + 领料 4 态 + 委外 8 态+审批轴）核心契约经实仓逐项证据确认：状态迁移守卫齐全（`requireStatus`/`validateTransition*` 前置校验）、事务边界清晰（@BizMutation 自动事务）、红冲闭环对称（领料 reverseConfirm / 委外 reverseCompletion + 财务侧监听者兜底）、过账副作用经 I*Biz Facade 跨域（production 代码无 `daoFor(ErpFin*)` 已确认）。**零 P0**（三个候选 P0 经证据证伪：作业卡 TRANSFERRED 死状态不破坏状态机主路径 / 委外 reverseCompletion 双路径保证最终一致 / 部分齐套强制开工缺料经 @BizMutation 事务回滚有出口）；**1 项新 P1**（P1-MA2-035 作业卡 PARTIALLY_TRANSFERRED/MATERIAL_TRANSFERRED dict 死状态 + owner doc 迁移图声明漂移）；**3 项新 P2** watch-only（报工超产 config-gate 缺失 / 领料状态机 owner doc 散落 / 字典命名漂移）；MA1 finding 运行时复核**无升级**（P1-MA1-001 维持仅规范缺陷 / P1-MA1-022 维持仅治理缺陷）；并发敏感点 5 处交接 A2.17（含 @Version 透明乐观锁降级重要事实）。

**Verdict: pass**。A2.6a 完成，manufacturing 生产执行状态机系统性审查 done。A2.6b MRP/BOM 计划规划状态机（S 级拆分 2/2）后续执行。
