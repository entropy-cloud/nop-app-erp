# 财务域状态机

> **设计要点依据**：本状态机按 `docs/skills/state-machine-business-review-prompt.md` 的 10 个审查维度组织。审查本状态机时使用该提示词。
>
> 财务域有五类状态对象：**会计凭证**（单据级状态机）、**会计期间**（时间窗口状态机）、**应收票据**与**应付票据**（票据生命周期状态机，见 §对象三/§对象四）、**费用报销单**（审批 + 生命周期双轴，见 §对象五）。凭证与期间的状态迁移相互约束（已结账期间不可新增凭证）。

## 适用对象

- 会计凭证（Voucher）：业务单据审核触发生成的、或财务员手工创建的复式记账凭证。
- 会计期间（AccountingPeriod）：财务结账的时间窗口（月/季/年）。
- 应收票据（NotesReceivable）：收到/贴现/背书/托收/承兑/拒付/注销的票据生命周期。
- 应付票据（NotesPayable）：开出/兑付/拒付/注销的票据生命周期。
- 费用报销单（ExpenseClaim）：报销单的**审批轴**（approveStatus，5 动作）与**业务生命周期轴**（docStatus，cancel 作废），见 §对象五。

## 对象一：会计凭证状态机

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 可修改 | 参与总账 |
|------|----------------------|--------|----------|
| 草稿（DRAFT） | 等待过账 | 是 | 否 |
| 已过账（POSTED） | 终态：已过账，参与总账汇总 | 否（需红冲） | 是 |
| 已作废（CANCELLED） | **预留语义入口**（dict 项保留，未启用迁移）。草稿凭证的废弃经 logical delete（`useLogicalDelete`）承载，不经 DRAFT→CANCELLED 状态迁移。 | 否 | 否 |

### 2. 迁移完整性

```
草稿 (DRAFT)
  ├─ 过账 → 已过账 (POSTED)
  │            └─ 红冲 → 原凭证置 isReversed=true（保留 POSTED，单边标记，不建立双向回链）
  └─ [预留] 作废 → 已作废（CANCELLED）
       ↑ 未实现迁移：草稿废弃经 logical delete 承载，CANCELLED dict 项保留为未来显式作废工作流的语义入口
```

| 迁移 | 触发人/系统 | 前置条件 | 结果 |
|------|-------------|----------|------|
| DRAFT → POSTED | 财务员 / 系统（业财自动触发） | 草稿状态、**借贷平衡**、**会计期间未结账**、科目有效、汇率存在（多币种） | 参与总账、凭证号按凭证字分类连续生成 |
| POSTED → 红冲（原凭证置 isReversed=true） | 财务员 | 已过账状态 | 原凭证保留 POSTED 终态；置 `isReversed=true` 单边标记（**已知简化**：不建立 `reversedVoucherId` 双向回链，红冲闭环功能完整，红字凭证经 `postingType=REVERSAL` 与原凭证关联） |
| ~~DRAFT → CANCELLED~~（预留，未实现） | — | — | 草稿废弃经 logical delete（`useLogicalDelete`）承载；CANCELLED 作为预留 dict 项保留，待未来「保留审计轨迹的显式作废动作」PM 需求时实现 |

### 3. 终态与恢复

- **终态**：`已过账（POSTED）`。CANCELLED 为预留 dict 项（未启用迁移，非活跃终态）。
- **已过账不可直接修改**：已影响总账，纠错只能红冲（原凭证置 `isReversed=true`）。
- **红字凭证（已知简化，对齐实现）**：红冲在原凭证上置 `isReversed=true` 单边标记（保留 POSTED），不建立 `reversedVoucherId` 双向回链。归档凭证（已红冲原凭证）与活动凭证（POSTED + `isReversed=false`）的区分经 `isReversed` + `postingType` 联合判定。红冲闭环功能完整（含 `reverseVoucher` 与业财回链红冲）。`reversedVoucherId` 双向回链为 successor（报表需求驱动时实现）。
- **草稿废弃**：经 logical delete（`useLogicalDelete`）承载，不经状态迁移；CANCELLED dict 项保留为未来显式作废工作流的语义入口（successor）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 借贷不平衡 | DRAFT → POSTED 时拒绝（通常是凭证模板配置错误） |
| 会计期间已结账 | 拒绝过账；需反结账期间或计入当前开启期间 |
| 科目映射缺失 | 业务自动触发的凭证报错并标记，等待人工配置科目映射后重试 |
| 汇率缺失（多币种） | 拒绝过账，不静默用默认值 |
| 财务过账异步失败 | 业务单据 `posted=false` + 兜底扫描重试 |
| 业务单据作废触发红冲失败 | 按业财回链反查原凭证失败 → 标记异常，人工介入 |
| 并发过账同一凭证 | 乐观锁 |
| 重复过账（幂等） | 已过账凭证再次过账为空操作 |

### 5. 可达性

- 从 DRAFT 可达 POSTED。
- POSTED 的"红冲"是原凭证上置 `isReversed=true`（单边标记），不是状态回退，也不生成新状态。
- CANCELLED 当前为预留 dict 项，无入边（草稿废弃经 logical delete 承载）；DRAFT→CANCELLED 迁移为 successor。
- 无死锁、无循环。DRAFT→POSTED 是有向无环路径。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| 过账（DRAFT→POSTED） | 财务员 / 系统（业财自动） |
| 红冲（原凭证置 isReversed=true） | 财务员（需权限，因影响总账） |
| 作废（DRAFT→CANCELLED） | **预留**（未实现迁移；草稿废弃经 logical delete，权限由 delete action 承载） |

危险操作：
- **红冲已过账凭证**：需财务员权限，建议二次确认（因影响报表）。
- **业务自动触发的凭证**：无需人工过账动作，但红冲需财务员介入。

### 7. 外部依赖

| 外部场景 | 内部处理 |
|----------|----------|
| 业务单据（采购发票/付款/销售发票/收款/库存移动）触发凭证生成 | 通过 `IErpFinAcctDocProvider` 跨工程聚合，业务单据不直接写凭证 |
| 业务单据作废 | 通过业财回链反查凭证，生成红字冲销 |

外部触发渠道：
- 业务单据审核联动（主要渠道，异步 post-commit）。
- 财务员手工创建（调整凭证、期末结转凭证）。

#### 7.1 实现注记：生成路径与状态机 Bean 边界（plan 2026-08-13-2045-3）

凭证 `docStatus` 轴由实体级状态机 Bean `ErpFinVoucherDocumentStateMachine`（契约 `docs/architecture/entity-state-machine-bean.md`）承载。**唯一命名动作迁移边**为财务员/系统触发的 `postVoucher`：`DRAFT→POSTED`（Bean `assertCanPost(DRAFT)` + `postVoucherTargetStatus()`）。Bean 严格无状态，按类型注入，可经 Delta 同名覆盖（契约 §6）。

下列 **7 条程序化生成路径**（契约 §9.2 选项 c 初始态/生成写入）直接写 `POSTED`/`DRAFT`，**不经 `postVoucher` 命名动作，也不经 Bean `assertCanPost`**——凭证生成即落目标态，属生成写入而非用户命名动作迁移，Bean 不覆盖此路径：

| 生成路径 | 写入值 | 场景 |
|----------|--------|------|
| `ErpFinPostingProcessor.persistVoucher` | POSTED | 业财自动过账引擎（业务单据审核联动生成凭证即 POSTED） |
| `CloseVoucherWriter` | POSTED | 期末结转（损益结转凭证） |
| `BudgetVoucherGenerator` | POSTED | 预算影子凭证（postingType=BUDGET） |
| `CommitmentVoucherGenerator` | POSTED | 承付占用/释放凭证（postingType=COMMITMENT） |
| `IntercompanyVoucherGenerator` | POSTED | 内部交易配对凭证 |
| `ErpFinBudgetScenarioCarryForwardProcessor` | POSTED | 预算方案结转凭证 |
| `ErpFinConsolidationEliminationPostEliminationProcessor` | DRAFT | 合并抵销候选凭证（生成后经命名动作过账） |

> 「生成路径统一经 Bean `assertCanPost`」属更强的 CRUD/生成路径写入锁范畴，为 M0.1 successor（契约 §9.4 + 路线图规则），不在本轴迁移范围内静默实施。

**`isReversed` 非 `docStatus` 轴边界**：`reverseVoucher`（及其只读预览 `previewReverseVoucher`）在已过账凭证上置 `isReversed=true`（单边标记，保留 POSTED），**不写 `docStatus`，不产生 docStatus 迁移边**。其 `docStatus==POSTED` 前置守卫经 Bean 的 `isPosted(status)` 分类 helper 承载（一致性，非迁移边）。红冲闭环以 `isReversed` + `postingType=REVERSAL` 为契约，`reversedVoucherId` 双向回链为 successor（报表需求驱动）。`CANCELLED` 为 intentional reserved 死状态（零 writer，草稿废弃经 `useLogicalDelete`），dict 项保留为未来显式作废工作流的语义入口，Bean 不纳入 initial/terminal/transitions 任一集合。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| DRAFT | 是（手工创建的） | assigned（财务员） |
| DRAFT（业务自动生成的） | 否（异步自动过账） | — |
| POSTED | 否 | — |
| CANCELLED | 否 | — |

业务自动生成的凭证不产生人工 TODO（走异步过账）；手工创建的草稿凭证产生财务员待办。**过账失败的凭证**需产生异常 TODO（分配给财务员处理），避免凭证长期滞留草稿。

### 9. 场景演练

#### 场景 A：业务自动过账 happy path

1. 采购发票审核通过 → 触发应付凭证生成（DRAFT）+ `posted=false`。
2. 异步过账成功 → POSTED + `posted=true`。
3. 业财回链表记录凭证号 ↔ 采购发票号。

#### 场景 B：业务单据作废触发红冲

1. 采购发票作废 → 按业财回链反查已过账凭证。
2. 生成红字凭证（金额取负）→ DRAFT → POSTED → 应付冲销。

#### 场景 C：期间结账后才发现错误

1. 6 月已结账，7 月发现 6 月某凭证错误。
2. 选项一：反结账 6 月 → 修改/红冲凭证 → 重新结账（高权限，影响报表）。
3. 选项二：在 7 月（当前开启期间）生成红字凭证冲销 + 新正确凭证（更安全，保留 6 月审计轨迹）。

### 10. 与设计文档一致性

- 凭证模型与业财打通见 `finance/README.md` 与 `finance/posting.md`。
- 状态码持久化值归 `model/app-erp-finance.orm.xml`。
- 期间约束见下文会计期间状态机。

---

## 对象二：会计期间状态机

### 1. 状态定义

| 状态 | 业务含义（等待什么） | 可新增凭证 | 可结账 |
|------|----------------------|------------|--------|
| 未开启（NEVER_OPENED） | 期间未到或已财务关闭 | 否 | — |
| 已开启（OPEN） | 当前可记账期间 | 是 | 是 |
| 结账中（CLOSING） | 正在执行期末结账流程 | 否 | — |
| 已结账（CLOSED） | 结账完成，待复核 | 否 | 否（需反结账） |
| 已复核（CLOSED_FINAL） | 终态：报表复核后最终锁定 | 否 | 否（需反结账） |

> **状态码权威来源**：5 态对齐 ORM dict `erp-fin/period-status`（`app-erp-finance.orm.xml`）与 `ErpFinConstants.PERIOD_STATUS_*`。`CLOSED`=已结账（待复核），`CLOSED_FINAL`=已复核（最终锁定），`NEVER_OPENED`=未开启。

### 2. 迁移完整性

```
未开启 (NEVER_OPENED)
  └─ 到达期间开始日期 → 已开启 (OPEN)
                        ├─ 新增凭证（正常业务）
                        ├─ 期末结账 → 结账中 (CLOSING)
                        │              ├─ 结账成功 → 已结账 (CLOSED，待复核)
                        │              └─ 结账失败 → 回到已开启 (OPEN)
                        ├─ 复核 → 已复核 (CLOSED_FINAL，最终锁定)
                        └─ 反结账（已复核→已开启）→ 回到已开启
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| NEVER_OPENED → OPEN | 系统（定时任务，到达期间开始日期） | 上一期间已结账或允许跨期 | 开启记账 |
| OPEN → CLOSING | 财务员（发起期末结账） | 本期所有业务单据已审核、`posted=true`（无未过账单据） | 冻结新增凭证 |
| CLOSING → CLOSED | 系统（结账流程完成） | 成本核算、折旧/摊销、结转损益全部成功 | 标记已结账（待复核） |
| CLOSING → OPEN | 系统（结账失败） | 任一结账步骤失败 | 回退，允许修复后重试 |
| CLOSED → CLOSED_FINAL | 财务员/系统（报表复核） | 试算平衡表快照生成 | 最终锁定 |
| CLOSED_FINAL → OPEN（反结账） | 管理员（高权限） | 特殊情况需调整已结账期间 | 允许修改凭证后重新结账 |

### 3. 终态与恢复

- **终态**：`已复核（CLOSED_FINAL）`。`已结账（CLOSED）` 为待复核中间态（结账完成但尚未最终锁定）。
- **反结账恢复**：管理员可反结账回到 OPEN，修改凭证后重新结账。需严格权限控制（影响已出具报表）。反结账 kill-switch 保留原位（见 §6 已知简化 P1-MA3-036）。
- **结账失败的恢复**：CLOSING → OPEN（自动回退），修复问题后重新发起结账。

> **CLOSING 瞬态实现注记**（plan 2026-08-13-2045-1）：`closePeriod` 为 `@BizMutation`（事务包裹）——结账步骤（成本核算/折旧/汇兑重估/损益结转/模块结账子状态，均在期间仍 OPEN 时执行）全部成功后，事务内先 `setStatus(CLOSING)` 紧接 `setStatus(CLOSED)`（`ErpFinAccountingPeriodClosePeriodProcessor:81-82`）。故上文「CLOSING → OPEN（结账失败）」即**事务回滚语义**——任一结账步骤失败则整 mutation 回滚，CLOSING 不持久化——而非独立的显式 writer。`ErpFinAccountingPeriodStateMachine` 状态矩阵 Bean 仅守卫 close 动作入口来源态 OPEN（`assertCanClose(CLOSING)` 抛非法：CLOSING 不可作「发起结账」入口，仅事务内瞬态中间态），编码 close 两段边 OPEN→CLOSING→CLOSED，不为 CLOSING→OPEN 发明独立命名边（契约 §11.2 M4 + `entity-state-machine-bean.md §8` 瞬态轴）。

### 4. 异常路径

| 异常场景 | 处理 |
|----------|------|
| 结账时存在未过账单据（`posted=false`） | 拒绝进入 CLOSING，提示先完成过账 |
| 成本核算失败 | CLOSING → OPEN，提示成本异常 |
| 结转损益借贷不平衡 | CLOSING → OPEN（通常是科目映射错误） |
| 并发结账（多人同时发起） | 乐观锁，后者失败 |
| 反结账后修改凭证影响下游期间 | 需重新结账本期间及所有下游已结账期间 |

### 5. 可达性

- NEVER_OPENED → OPEN → CLOSING → CLOSED → CLOSED_FINAL 是主路径。
- CLOSING → OPEN（失败回退）与 CLOSED_FINAL → OPEN（反结账）是恢复路径。
- 无死锁：CLOSED_FINAL 是终态，反结账是显式路径。

### 6. 角色与权限

| 迁移 | 执行角色 |
|------|----------|
| 发起结账（OPEN→CLOSING） | 财务员 |
| 反结账（CLOSED_FINAL→OPEN） | **管理员**（高权限；当前为 config kill-switch，非独立审批 action，见下方已知简化） |
| 期间自动开启 | 系统 |

危险操作：
- **反结账**：受 config kill-switch 门控（影响已出具报表与税务申报），完整审批流为 successor。

> **反结账审批已知简化（P1-MA3-036）**：当前 `reverse-close-approval-required`（默认 true）为**保护性 kill-switch**——true 时 `reverseClose` 直接抛异常拒绝反结账（纯开关，无审批 action），false 时由 @BizMutation 角色权限门控（无独立审批 action）。owner doc 表述「管理员 + 审批」的实际落位 = kill-switch + 角色权限。完整审批流（反结账申请→审批→执行，解除条件见 §已知限制：浏览器层 xwf 审批路径）为 successor（P1-MA2-020 触发：审批流落地时实现）。

> **CLOSED_FINAL 凭证锁定**：`ErpFinVoucherBizModel.postVoucher`/`reverseVoucher` 前校验凭证所属期间状态，CLOSED/CLOSED_FINAL 时抛 `ERR_FIN_VOUCHER_PERIOD_LOCKED` 拒绝操作（对齐下方 §1 状态定义「CLOSED_FINAL 可修改凭证=否」）。

### 7. 外部依赖

期间状态本身不依赖外部，但结账流程涉及：
- 成本核算（依赖库存域流水数据）。
- 折旧/摊销（依赖资产数据，若启用资产模块）。
- 税务申报（依赖发票数据，可能对接外部税务系统）。

### 8. TODO / 任务策略

| 状态 | 是否产生 TODO | TODO 类型 |
|------|---------------|-----------|
| OPEN（月末） | 是 | assigned（财务员）—— 月末待结账提醒 |
| CLOSING（失败回退） | 是 | assigned（财务员）—— 结账失败待处理 |
| CLOSED（待复核） | 是 | assigned（财务员）—— 待复核最终锁定 |
| CLOSED_FINAL | 否 | — |
| NEVER_OPENED（未到期间） | 否 | — |

### 9. 场景演练

#### 场景 A：正常月末结账

1. 6 月末，财务员确认所有业务单据已审核、`posted=true`。
2. 发起 6 月期末结账 → OPEN → CLOSING。
3. 系统执行：成本核算 → 折旧/摊销 → 结转损益 → 全部成功 → CLOSED_FINAL。
4. 7 月期间自动开启 → OPEN。

#### 场景 B：结账失败

1. 6 月结账 CLOSING，成本核算失败（某物料单位成本异常）。
2. CLOSING → OPEN（回退）。
3. 财务员处理成本异常后重新发起结账。

#### 场景 C：反结账调整

1. 6 月已结账，7 月发现 6 月某收入科目记错。
2. 管理员审批反结账 → CLOSED_FINAL → OPEN。
3. 红冲错误凭证 + 新正确凭证 → 重新结账 → 重新出具 6 月报表。

### 10. 与设计文档一致性

- 期间约束（已结账不可新增凭证）在凭证状态机的"异常路径"中体现。
- 期末结账流程见 `finance/posting.md`（业财打通的期末部分）。

---

## 对象三：应收票据状态机

> 票据字段与业务规则权威来源：`docs/design/finance/treasury.md` §ErpFinNotesReceivable（`:56-75` 状态机 + `:197-202` 关键业务规则）；业财过账业务类型见 `docs/design/finance/posting.md`（NOTES_RECEIVABLE_RECEIVED/DISCOUNTED/ENDORSED/COLLECTION）。

### 1. 状态定义

| 状态 | 业务含义 |
|------|----------|
| 收到（RECEIVED） | 收到票据，挂账应收票据（资产）。**initial 态**（票据收到即 RECEIVED，无 DRAFT 前态） |
| 已贴现（DISCOUNTED） | 已向银行贴现取得资金（贴现息走财务费用，treasury.md §规则 2） |
| 已背书（ENDORSED） | 已背书转让给供应商（抵应付）。**非终态中间态**：背书后票据所有权已转移，**仅可 writeOff 出边**——不可再 collect/discount/背书 |
| 托收中（COLLECTION_PENDING） | 到期已送银行托收，等待兑付（**在途态，不过账**） |
| 已承兑（HONORED） | 终态：银行承兑付款 |
| 拒付（DISHONORED） | 终态：拒付（转应收账款追索，treasury.md §规则 3；**终态重分类，不过账**） |
| 已注销（WRITE_OFF） | 终态：注销（票据遗失/作废，需审批，treasury.md §规则 4） |

### 2. 迁移完整性

```
收到 (RECEIVED)                       ← initial（receive，无 DRAFT 前态；幂等 re-receive 合法）
  ├─ 贴现 → 已贴现 (DISCOUNTED)
  │            └─ 到期托收 → 托收中 (COLLECTION_PENDING) → 承兑 (HONORED, 终态) / 拒付 (DISHONORED)
  ├─ 背书转让 → 已背书 (ENDORSED)      ── 仅 writeOff 出边
  └─ 到期托收 → 托收中 (COLLECTION_PENDING) → 承兑 (HONORED) / 拒付 (DISHONORED)
任何非终态 → 注销 (WRITE_OFF, 终态，需审批)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| receive → RECEIVED | 财务员 | initial（null）或幂等 re-receive | 挂应收票据；**过账** NOTES_RECEIVABLE_RECEIVED（借应收票据/贷应收账款，抵客户欠款） |
| discount → DISCOUNTED | 财务员 | RECEIVED | **过账** NOTES_RECEIVABLE_DISCOUNTED（科目分解五件套 + FX plug） |
| endorse → ENDORSED | 财务员 | RECEIVED | **过账** NOTES_RECEIVABLE_ENDORSED（借应付账款/贷应收票据，抵供应商） |
| collect → COLLECTION_PENDING | 财务员 | RECEIVED **或** DISCOUNTED（双源） | 在途态，**不过账** |
| honor → HONORED | 财务员 | COLLECTION_PENDING | **过账** NOTES_RECEIVABLE_COLLECTION（借银行存款/贷应收票据） |
| dishonor → DISHONORED | 财务员 | COLLECTION_PENDING | 终态重分类，**不过账**；拒付转应收（treasury.md §规则 3） |
| writeOff → WRITE_OFF | 财务员（需审批） | 任意非终态（loose） | 若已过账则红冲（`reverseReceivable`） |

### 3. 终态与恢复

- **终态**：`HONORED` / `DISHONORED` / `WRITE_OFF`（均无出边）。**initial**：`RECEIVED`。
- **ENDORSED 非终态**：背书后票据所有权已转移，仅可 writeOff 出边（不可 collect/discount/再次背书）。
- 终态不可恢复；纠错仅能经注销/红冲路径。

### 4. 过账边界（§11.2 M4 治理裁定）

- receive/discount/endorse/honor 四动作触发业财过账（`NotesPostingDispatcher`→`FinPostingExecutor` 生成凭证 + `ErpFinArApItem` 辅助账，RECEIVED→RECEIVABLE 抵销客户 AR / ENDORSED→PAYABLE 抵销供应商 AP）；writeOff 若已过账则红冲。**过账时序/编排/失败回退（posted 回写）/红冲闭环由过账引擎 + `posted`/`postedBy`/`postedAt` 契约管理**，状态机 Bean 不触碰。
- **COLLECTION_PENDING（在途）/DISHONORED（终态重分类）不过账**——属过账侧（`NotesPostingDispatcher`）语义，非状态机轴范畴。

### 5. 实现注记（plan 2026-08-13-1146-1）

`status` 轴由实体级状态机 Bean `ErpFinNotesReceivableStateMachine`（契约 `docs/architecture/entity-state-machine-bean.md`）承载：7 命名动作矩阵（receive/discount/endorse/collect 双源/honor/dishonor/writeOff 非终态）+ initial/terminal 分类 + 只读 `transitions()` 元数据（7 命名边）。**writer 放置不对称（intentional legacy layout）**：collect/dishonor 的 writer 在 per-mutation Processor（`ErpFinNotesReceivableCollectProcessor`/`ErpFinNotesReceivableDishonorProcessor`）直接 `setStatus(collectTargetStatus()/dishonorTargetStatus())`，其余 5 动作在 facade `do*` 写回——迁移仅改守卫委托 Bean，不改 writer 物理位置。receive 守卫为**有意收窄**（仅接受 null initial 写入 / RECEIVED 幂等，实仓零路径从非 initial 态 receive）。`posted` 不入轴（契约 §3）。非法边由 Bean 抛 common 码，Processor 映射领域码 `ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION`（common 作 cause，`ARG_NOTES_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS` 对外不变）。

---

## 对象四：应付票据状态机

> 票据字段与业务规则权威来源：`docs/design/finance/treasury.md` §ErpFinNotesPayable（`:77-93`）+ 关键业务规则（`:197-202`，授信强一致校验 §规则 1）；业财过账业务类型见 `docs/design/finance/posting.md`（NOTES_PAYABLE_ISSUED/HONORED）。

### 1. 状态定义

| 状态 | 业务含义 |
|------|----------|
| 已开出（ISSUED） | 开出应付票据（负债）。**initial 态**（开出即 ISSUED，无 DRAFT 前态；商业/银行承兑） |
| 已兑付（HONORED） | 终态：到期兑付（借应付票据/贷银行存款） |
| 拒付（DISHONORED） | 终态：拒付 |
| 已注销（WRITE_OFF） | 终态：注销（需审批） |

### 2. 迁移完整性

```
开出 (ISSUED)                          ← initial（issue，无 DRAFT 前态；幂等 re-issue 合法）
  ├─ 兑付 → 已兑付 (HONORED, 终态)
  ├─ 拒付 → 拒付 (DISHONORED, 终态)
  └─ 注销 → 已注销 (WRITE_OFF, 终态，需审批)
```

| 迁移 | 触发人 | 前置条件 | 结果 |
|------|--------|----------|------|
| issue → ISSUED | 财务员 | initial（null）或幂等 re-issue | 挂应付票据；**过账** NOTES_PAYABLE_ISSUED（借应付账款/贷应付票据）；银承**占用授信**（`reserveCreditIfNeeded`，config-gated `erp-fin.credit-check-on-issue`） |
| honor → HONORED | 财务员 | ISSUED | **过账** NOTES_PAYABLE_HONORED（借应付票据/贷银行存款）；**释放授信** |
| dishonor → DISHONORED | 财务员 | ISSUED | **释放授信**（不产生过账） |
| writeOff → WRITE_OFF | 财务员（需审批） | 任意非终态（loose） | 若已过账则红冲（`reversePayable`）；**释放授信** |

### 3. 终态与恢复

- **终态**：`HONORED` / `DISHONORED` / `WRITE_OFF`（均无出边）。**initial**：`ISSUED`。
- 终态不可恢复。

### 4. 过账 + 授信边界（§11.2 M4 治理裁定）

- issue/honor 两动作触发业财过账（`NotesPostingDispatcher`→`FinPostingExecutor` 生成凭证）；writeOff 若已过账则红冲。**过账时序/编排/失败回退（posted 回写）/红冲闭环由过账引擎 + `posted`/`postedBy`/`postedAt` 契约管理**，状态机 Bean 不触碰。
- **授信占用/释放为 Payable 独有受保护维度**（`IErpFinCreditFacilityBiz`）：issue 时银承 `reserveCreditIfNeeded`（占用授信，treasury.md §规则 1 强一致校验），honor/dishonor/writeOff 时 `releaseOccupiedCredit`（释放授信）——时序保留 Processor 原位，Bean 不触碰。

### 5. 实现注记（plan 2026-08-13-1146-1）

`status` 轴由实体级状态机 Bean `ErpFinNotesPayableStateMachine`（契约 `docs/architecture/entity-state-machine-bean.md`）承载：4 命名动作矩阵（issue/honor/dishonor/writeOff）+ initial/terminal 分类 + 只读 `transitions()` 元数据（4 命名边）。**writer 全部在 facade `do*`**（无 per-mutation 不对称，区别于 Receivable 的 collect/dishonor per-mutation writer）。issue 守卫为**有意收窄**（仅接受 null initial 写入 / ISSUED 幂等，实仓零路径从非 initial 态 issue）。`posted` 不入轴（契约 §3）。非法边由 Bean 抛 common 码，Processor 映射领域码 `ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION`（common 作 cause，`ARG_NOTES_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS` 对外不变）。

---

## 对象五：费用报销单状态机（双轴）

> 报销单字段与业务规则权威来源：`docs/design/finance/expense-claim.md`；业财过账业务类型见 `docs/design/finance/posting.md`（EXPENSE_CLAIM：Dr 6602 管理费用 / Dr 2221 进项税 / Cr 2241 应付-员工 或 1002 银行存款）+ `docs/design/finance/expense-claim.md`（ArApItem PAYABLE + 员工借款冲抵 `AdvanceOffsetOrchestrator`）。
>
> **实现注记（plan 2026-08-13-1146-2，M4.4 + M4.5）**：双轴各自独立 Bean（契约 `docs/architecture/entity-state-machine-bean.md` §3 双轴分离，不合并笛卡尔积）——`ErpFinExpenseClaimApprovalStateMachine`（approveStatus 轴）+ `ErpFinExpenseClaimDocumentStateMachine`（docStatus 轴）。5 审批守卫 + 1 作废守卫接线至 `ErpFinExpenseClaimProcessor.validateTransitionFor*`（Bean 抛 common 码作 cause，Processor 映射领域码 `ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION` / `ERR_EXPENSE_CLAIM_ILLEGAL_DOC_STATUS_TRANSITION`，参数对外不变）。

### 1. 状态定义（双轴）

**审批轴 approveStatus**（字典 `wf/approve-status` 4 值，平台共享 dict）：

| 状态 | 业务含义 |
|------|----------|
| 未提交（UNSUBMITTED） | 草稿，未进入审批流。**initial 态** |
| 已提交（SUBMITTED） | 已提交审批，等待审核 |
| 已审核（APPROVED） | 审核通过，触发过账（凭证 + ArApItem PAYABLE + 员工借款冲抵） |
| 已驳回（REJECTED） | 审核驳回，可修改重提 |

**业务生命周期轴 docStatus**（字典 `erp-fin/expense-claim-status` 5 值）：

| 状态 | 业务含义 |
|------|----------|
| 草稿（DRAFT） | **initial 态**（创建时写入） |
| 已作废（CANCELLED） | 终态：作废（已过账时先红冲再作废） |
| ~~SUBMITTED / APPROVED / REJECTED~~ | **残余值（intentional reserved）**：dict 5 值但代码仅写 DRAFT/CANCELLED——生命周期推进由 approveStatus 轴承载，docStatus 仅 DRAFT→CANCELLED。残余值不纳入状态机 Bean 任一集合（initial/terminal/transitions），dict 项保留不删（dict 治理归 successor） |

### 2. 迁移完整性

```
审批轴 approveStatus:
未提交 (UNSUBMITTED) ← initial
  ├─ 提交 → 已提交 (SUBMITTED)
  │            ├─ 撤回 → 未提交 (UNSUBMITTED)
  │            ├─ 审核 → 已审核 (APPROVED)
  │            └─ 驳回 → 已驳回 (REJECTED)
  ├─ 驳回后重提 → 已提交 (SUBMITTED)
  └─ 已审核 → 反审核 → 已驳回 (REJECTED)   ← reverseApprove，触发红冲

生命周期轴 docStatus:
草稿 (DRAFT) ← initial
  └─ 作废 → 已作废 (CANCELLED, 终态)        ← 已过账时先红冲（reverseOffset + reverse）再作废
```

| 迁移 | 触发人/系统 | 前置条件 | 结果 |
|------|-------------|----------|------|
| submitForApproval {UNSUBMITTED,REJECTED} → SUBMITTED | 报销人 | 未作废、报销人启用/partnerId 存在、行非空、价税合计一致、事由必填（按配置）、预算校验（按配置） | 进入审批流 |
| withdrawApproval {SUBMITTED} → UNSUBMITTED | 报销人 | 已提交未审核 | 撤回 |
| approve {SUBMITTED} → APPROVED | 审核人（**SoD：不得为创建人**） | 已提交、SoD 通过 | **过账**（EXPENSE_CLAIM 凭证 + ArApItem PAYABLE + 员工借款冲抵，posted=true） |
| reject {SUBMITTED} → REJECTED | 审核人 | 已提交 | 驳回 |
| reverseApprove {APPROVED} → REJECTED | 审核人（需权限） | 已审核 | 若已过账则**红冲**（reverse + reverseOffset + posted=false） |
| cancel {非 CANCELLED} → CANCELLED | 报销人 | 未作废 | 若已过账则先红冲再作废 |

### 3. 终态与恢复

- **审批轴终态**：`APPROVED` / `REJECTED`（均为**可逆终态**——APPROVED 经 reverseApprove 有出边、REJECTED 经 submitForApproval 有出边，不适用「终态无出边」强可达性断言）。**initial**：`UNSUBMITTED`。
- **生命周期轴终态**：`CANCELLED`（无出边）。**initial**：`DRAFT`。
- 作废不可恢复（逻辑删除不可逆；已作废再 cancel 抛非法迁移拒绝）。

### 4. 过账边界（§11.2 M4 治理裁定）

- approve 触发业财过账（`ExpenseClaimPostingDispatcher`→`FinPostingExecutor` 生成 EXPENSE_CLAIM 凭证 + `ErpFinArApItem` PAYABLE + `AdvanceOffsetOrchestrator` 员工借款冲抵）；reverseApprove/cancel 在已过账时触发红冲（reverse + reverseOffset）。**过账时序/编排/失败兜底（posted 回写）/红冲闭环由过账引擎 + `posted`/`postedBy`/`postedAt` 契约管理**（§11.2 M4 (ii)/(v)），状态机 Bean 不触碰。
- **`posted` 不入轴**（契约 §3）：`posted`（boolean）为业财过账/红冲契约，不是状态轴。approve→`posted=true` + ArApItem PAYABLE + offset；reverseApprove/cancel 红冲→`posted=false`。
- **SoD（approver-is-creator）为动态业务守卫**：保留在 `ErpFinExpenseClaimProcessor.doApprove` 原位（`SoDGuard.assertApproverNotCreator`，抛 `ERR_FIN_APPROVER_IS_CREATOR`），**非 Bean 范畴**（架构契约 `entity-state-machine-bean.md:274`）。
- **残余值边界**：docStatus dict 中 SUBMITTED/APPROVED/REJECTED 为 intentional reserved（workflow 轴 = approveStatus），Bean 不纳入任一集合（见 §1）。

### 5. 实现注记（plan 2026-08-13-1146-2）

- `ErpFinExpenseClaimApprovalStateMachine`：5 命名动作矩阵（submit×2 源 + withdraw + approve + reject + reverseApprove）+ initial/terminal + 只读 `transitions()` 元数据（6 边）。
- `ErpFinExpenseClaimDocumentStateMachine`：1 命名动作（cancel，loose 非 CANCELLED 源）+ initial/terminal + `transitions()`（1 代表边 DRAFT→CANCELLED）。
- **writer 放置**：6 动作 writer 全部在 facade `do*`（`ErpFinExpenseClaimProcessor`），迁移仅改守卫委托 Bean + 目标态写回 Bean 目标态方法，writer 物理位置与副作用（SoD/tryPost/reverse/offset/reverseOffset/posted/approvedBy/approvedAt 写入/乐观锁/业务规则校验）原序保留。
- **reverseApprove→REJECTED**：已合规 `domain-design-guidelines.md §16.4`（与 purchase 一致，无 drift）。
- 非法边由 Bean 抛 common 码（携带 `action`/`fromStatus` 元数据），Processor 映射领域码（common 作 cause，`ARG_CLAIM_CODE`/`ARG_CURRENT_STATUS`/`ARG_EXPECTED_STATUS` / `ARG_CURRENT_DOC_STATUS`/`ARG_EXPECTED_DOC_STATUS` 对外不变）。

---

## 两类状态机的耦合约束

| 约束 | 说明 |
|------|------|
| 凭证过账需期间开启 | 凭证 DRAFT→POSTED 前置条件：会计期间处于 OPEN 状态 |
| 期间结账需凭证已过账 | 期间 OPEN→CLOSING 前置条件：本期无 `posted=false` 的业务单据 |
| 反结账影响凭证 | 期间 CLOSED_FINAL→OPEN 后，该期间的凭证可被红冲/修改 |

## 审查提示

审查本状态机时，使用 `docs/skills/state-machine-business-review-prompt.md`，重点检查：
- 红字凭证冲销路径是否完整（原凭证 `isReversed=true` 单边标记，**已知简化**：无 `reversedVoucherId` 双向回链——successor）。
- CANCELLED 是否仍为预留 dict 项（无入边；草稿废弃经 logical delete 承载）。
- 业务自动触发的凭证过账失败时是否产生异常 TODO（避免草稿滞留）。
- 期间结账的前置校验（无未过账单据）是否落实。
- 反结账权限是否严格（管理员 + 审批）。
- 两类状态机的耦合约束（期间 vs 凭证）是否一致。

## 职责分离（程序级强制）

财务域审批单据（费用报销 ErpFinExpenseClaim、员工借款 ErpFinEmployeeAdvance 等）的创建人与审核人不可为同一人：approve 守卫比对 `createdBy` 与审核人 userId，相等抛 `erp.err.fin.approver-is-creator`（plan 2026-07-31-1023-2 R3.3）。注：会计凭证 `postVoucher` 为过账动作（非审批），不在 SoD 范围。

## 已知限制：浏览器层 xwf 审批路径

> M-2（plan `2026-07-20-2200-1`）补充；权威裁决见 plan `2026-07-09-2330-1`。

财务域的 **ErpFinPayment**（付款单）与 **ErpFinReceipt**（收款单）的 `useWorkflow="true"` xwf 审批轴在浏览器层 E2E 不可达：

- 根因：nop-wf `WorkflowEngineImpl.newSteps` 在浏览器层 `submitForApproval` 时 fallback `sysUser(0)` 作 step owner，但 `NopAuthUser.userId` 因 `tagSet="seq"` 覆盖显式 "0" 为 UUID，致 `allowCallByUser:1053` 拒绝。
- **替代路径**：Payment / Receipt 的 DIRECT 三轴审批（`approveStatus` DIRECT，`docs/plans/2026-07-05-0540-3` 范式）不依赖 xwf，浏览器层 E2E 可达且全绿。
- **影响范围**：本状态机文档涉及的 `POSTED` 状态由 DIRECT 三轴审批驱动可达；多级审批链的业务场景仅在浏览器层外（后端单测）覆盖。
- **解除条件**：见 `docs/design/roles-and-permissions.md §浏览器层审批路径已知限制`。

Payment / Receipt 的 `approve_direct` 路径状态迁移：

| 迁移 | 触发人/系统 | 前置条件 | 结果 |
|------|-------------|----------|------|
| SUBMITTED → APPROVED（DIRECT） | 财务员 | 已提交、`useWorkflow="true"` 实体的 DIRECT `approve_direct` action | `approveStatus=APPROVED`；触发后续过账与凭证写入 |
| SUBMITTED → REJECTED（DIRECT） | 财务员 | 已提交、DIRECT `reject_direct` action | `approveStatus=REJECTED`；不触发过账 |
