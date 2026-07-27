# MA2 finance 状态机审查 — 过账与凭证（A2.5a — 会计凭证状态机）

> Audit Status: closed
> 里程碑：MA2（业务正确性层 / 状态机正确性维度）
> 域/功能模块：finance / 过账引擎与凭证链路（A2.5a S 级拆分 1/3，会计凭证状态机）
> 审计 plan：`docs/plans/2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine.md`
> 来源 finding（运行时影响复核）：P0-MA1-021（done）/ P0-MA2-016（fix-plan-injected）/ P1-MA1-018 / P1-MA1-022 / P1-MA2-001 / P1-MA2-002 / P1-MA2-009 / P1-MA2-021 / P1-MA2-022 / P2-MA1-019 / P2-MA2-025
> Skill：`docs/skills/state-machine-business-review-prompt.md`
> 审计日期：2026-07-27
> 审计者：主代理（独立子代理已完成草案审查，见 plan §Draft Review Record）

## 0. 审计结论（TL;DR）

| 项 | 数量 | 处置 |
|---|---|---|
| **P0**（红字凭证可再红冲致无限循环 / 幂等键破缺致重复过账 / 异常 IGNORED 后凭证悬挂 / CLOSED_FINAL 凭证可被修改红冲 [若 P1-MA2-021 升级]） | **0** | 无即时通道修复 |
| **P1**（新登记） | **2** | P1-MA2-031（DRAFT→CANCELLED 状态不可达 + 红字凭证终态归属未定义）/ P1-MA2-032（IGNORED 凭证悬挂缺告警闭环） → 待 MR1 |
| **P2**（watch-only） | **1** | P2-MA2-033（红字凭证可再红冲的负向测试缺失） |
| MA1/MA2 finding 运行时复核 | 11 项 | P0-MA1-021 done ✓ / P0-MA2-016 fix in place ✓ / 余 9 项 — **无升级**（详见 §6） |

**整体裁决**：会计凭证状态机（DRAFT/POSTED/CANCELLED + isReversed + postingType 三轴）的核心契约——DRAFT→POSTED 前置校验、POSTED 经红字凭证冲销、幂等键 `(billHeadCode, businessType)` 经 `ErpFinVoucherBillR` 反查 + `alreadyPosted` 排除已红冲凭证、`markOriginalVoucherReversed` 引擎侧统一标记、`findAllPostedVouchers` 过滤 `postingType=REVERSAL` 阻断红字凭证再红冲的无限循环——**全部经证据确认**。`passes state-machine business review`（带 2 项 P1 残留 + 1 项 P2 watch-only）。**P1-MA2-021 CLOSED_FINAL 凭证锁定升级评估裁决：维持 P1 不升 P0**（详见 §6.7）。

---

## 1. 审计范围与方法覆盖矩阵

### 1.1 审计对象（实仓逐项核实）

| 组件 | 文件 | 行号 | 审计状态 |
|---|---|---|---|
| 凭证聚合 BizModel Facade | `module-finance/erp-fin-service/.../service/entity/ErpFinVoucherBizModel.java` | post:67-74 / reverse:76-84 / postVoucher:86-100 / reverseVoucher:102-114 / previewReverseVoucher:120-149 | ✅ |
| 过账编排 Processor | `module-finance/erp-fin-service/.../service/posting/ErpFinPostingProcessor.java` | process:126-203 / reverseProcess:209-257 / alreadyPosted:472-484 / resolveOpenPeriod:495-512 / persistVoucher:764-846 / buildReversalDraft:725-755 / markOriginalVoucherReversed:909-923 / dispatchReversalEvent:363-388 / recordPostFailure:305-329 / findAllPostedVouchers:866-882 | ✅ |
| Provider 注册表 | `ErpFinAcctDocRegistry.java`（init:45-79 fail-fast / getProvider:81-83） | — | ✅ |
| Reversal Listener Registry | `ErpFinReversalListenerRegistry.java`（dispatch:92-116 try/catch 隔离） | — | ✅ |
| 凭证状态机轴（ORM） | `module-finance/model/app-erp-finance.orm.xml` | ErpFinVoucher:411-468（postingType propId=4 / isReversed propId=12 / reversalOfVoucherId propId=13 / docStatus propId=14 dict erp-fin/voucher-status DRAFT/POSTED/CANCELLED / useLogicalDelete=true） | ✅ |
| businessType 枚举 | `ErpFinBusinessType.java` | 56 常量 + fromCode:80-87 | ✅ |
| businessType 字典 | `module-finance/model/app-erp-finance.orm.xml:60-127` | dict erp-fin/business-type | ✅（4 项 enum↔dict 漂移确认，见 §5.11） |
| 损益结转服务（P0-MA2-016 复核） | `ProfitLossClosingService.java:69-156` | closeForSchema 排除逻辑 line 91-93 | ✅（仅排除 PERIOD_CLOSE，EXCHANGE_GAIN_LOSS 正常结转） |
| 汇兑重估服务（P1-MA2-022 复核） | `ExchangeRevaluationService.java:103-155` | revalueArAp 持久化 enum.name() line 151-154 | ✅ |
| 维修工时过账派发（P1-MA1-022 复核） | `MaintenanceLaborPostingDispatcher.java:203-210` | voucherAlreadyExists 跨域只读 daoFor(ErpFinVoucherBillR) | ✅ |

### 1.2 Skill 维度覆盖（10 + 2 项目特定维度）

| # | 维度 | 裁决 | 发现 |
|---|------|------|------|
| 1 | 状态定义 | ⚠️(P1) | 红字凭证 `isReversed=true`+`postingType=REVERSAL` 终态归属未定义；`isReversed` 标记在红字凭证上语义混淆（详见 §5.1） |
| 2 | 转换完整性 | ⚠️(P1) | DRAFT→CANCELLED 无显式 action 实现（详见 §5.2） |
| 3 | 终端状态和恢复 | ⚠️(P1) | POSTED 可被红冲→非真终态；红冲无撤销 action（与 §5.1 同根因） |
| 4 | 异常路径 | ✅ | 不平衡/期间关闭/模板缺失/科目缺失/汇率缺失/异步失败/红冲失败/并发/幂等全覆盖 |
| 5 | 可达性 | ✅ | 红字凭证 postingType=REVERSAL 被 `findAllPostedVouchers` 排除，**无限循环风险已阻断** |
| 6 | 角色和权限 | ⚠️(P1 维持) | P1-MA2-021 CLOSED_FINAL 凭证锁定未实现，**评估裁决维持 P1 不升 P0**（详见 §6.7） |
| 7 | 外部依赖 | ✅ | 8 域 posted 反写经域调用方；ASYNC listener 失败入异常工作台；dormant Provider 直写经 owner doc 裁定 |
| 8 | TODO/任务策略 | ⚠️(P1) | IGNORED 凭证悬挂缺告警闭环（详见 §5.8） |
| 9 | 场景演练 | ✅ | 7 场景（happy/拒绝/异常终止/外部触发/超时/幂等/红冲链）经证据确认 |
| 10 | 与设计文档一致性 | ⚠️(P1) | state-machine.md:35 声明 DRAFT→CANCELLED 但代码无 action；state-machine.md:37-42 终态未明确红字凭证归属 |
| 11 | businessType enum↔dict（项目特定） | ✅ | 4 项漂移确认；内部聚合用 enum.name() 一致；P0-MA2-016 fix 仅移除 EXCHANGE_GAIN_LOSS 排除保留 PERIOD_CLOSE 排除 ✓ |
| 12 | 多币种凭证路径（项目特定） | ✅ | persistVoucher:818-819 line 级无 FX 折算；状态机不因币种失败；数学上平衡（业务汇兑损益漏 plug 归 P1-MA2-002/009 MR1） |

---

## 2. 凭证状态机三轴组合状态图

### 2.1 三轴组合（docStatus × isReversed × postingType）

| # | docStatus | isReversed | postingType | 业务含义 | 可达性 | 终态？ |
|---|-----------|------------|-------------|----------|--------|--------|
| 1 | DRAFT | false | NORMAL | 手工草稿 / 自动过账前预备态（**注**：自动过账直写 POSTED，不经 DRAFT） | ✓ 入口（手工创建） | 否 |
| 2 | POSTED | false | NORMAL | 正常已过账凭证（参与总账） | ✓ 经 persistVoucher（自动）/ postVoucher（手工） | 否（可被红冲） |
| 3 | POSTED | **true** | NORMAL | 已被红冲的原正常凭证（保留审计轨迹） | ✓ 经 markOriginalVoucherReversed | 是（不可再红冲） |
| 4 | POSTED | **true** | **REVERSAL** | 红字凭证（金额取负，冲销原凭证） | ✓ 经 reverseProcess→persistVoucher | 是（不可再红冲） |
| 5 | POSTED | true/false | BUDGET | 预算凭证（影子，config-gated） | ✓ 经 BudgetVoucherGenerator（dormant） | — |
| 6 | POSTED | true/false | COMMITMENT | 承付凭证（config-gated `erp-fin.budget-commitment-enabled` 默认 false） | ✓ 经 CommitmentVoucherGenerator | — |
| 7 | CANCELLED | — | — | 作废（仅草稿可作废） | **✗ 不可达**（详见 §5.2） | 是（理论终态） |

### 2.2 状态转移矩阵

```
DRAFT (NORMAL, isReversed=false)
  ├─ postVoucher (前置：借贷平衡 + 凭证存在) → POSTED (NORMAL, isReversed=false)  [手工入口]
  ├─ persistVoucher (引擎自动过账，直写 POSTED) → POSTED (NORMAL, isReversed=false)  [自动入口]
  └─ 默认 delete (CrudBizModel logical delete → delVersion++, docStatus 不变)  [隐式]

POSTED (NORMAL, isReversed=false)  [正常已过账凭证]
  ├─ reverseProcess (生成红字凭证 + 标记原凭证) → POSTED (NORMAL, isReversed=true)  [经 markOriginalVoucherReversed]
  └─ reverseVoucher (仅置 isReversed=true，不生成红字凭证) → POSTED (NORMAL, isReversed=true)  [简化 UI 按钮]

POSTED (REVERSAL, isReversed=true)  [红字凭证]
  └─ 无出边（findAllPostedVouchers 过滤 postingType=REVERSAL → 不可再红冲）  ✓ 无限循环已阻断

CANCELLED  [理论终态]
  ✗ 无任何代码路径置 docStatus=CANCELLED（详见 §5.2）
```

---

## 3. 9 个识别控制点裁决汇总

| # | 控制点 | 裁决 | 关键证据 |
|---|--------|------|----------|
| 1 | 状态定义清晰性 | **FAIL (P1)** | 红字凭证 `isReversed=true` 与原凭证被红冲后的 `isReversed=true` 共用同一标记，语义混淆（"已红冲的产物" vs "被红冲的原凭证"） |
| 2 | 转换完整性 | **FAIL (P1)** | DRAFT→CANCELLED owner doc 声明但代码无 action；`useLogicalDelete=true` 致 CANCELLED 状态不可达 |
| 3 | 终端状态和恢复 | **FAIL (P1)** | POSTED 非真终态（可红冲）；红冲后无撤销 action（恢复路径未定义） |
| 4 | 异常路径 | **PASS** | 全覆盖：`ERR_UNBALANCED`/`ERR_PERIOD_CLOSED`/`ERR_NO_PROVIDER`/`ERR_SUBJECT_NOT_FOUND`/`ERR_REVERSE_SOURCE_NOT_FOUND`/`ERR_REVERSAL_LISTENER_FAILED`；幂等键 + 重试上限 + 异常工作台 |
| 5 | 可达性 | **PASS** | `findAllPostedVouchers:866-882` 过滤 `postingType==REVERSAL` → 红字凭证不可再红冲（无限循环已阻断） |
| 6 | 角色与权限 | **PASS (P1 维持)** | P1-MA2-021 CLOSED_FINAL 凭证锁定未实现（维持 P1，升级评估裁决见 §6.7） |
| 7 | 外部依赖 | **PASS** | 8 域 posted 反写经域调用方；ASYNC listener 失败入异常工作台 PENDING；dormant Provider 经 owner doc 裁定 |
| 8 | TODO/任务策略 | **FAIL (P1)** | `ErpFinPostingException` IGNORED 状态后凭证悬挂，告警闭环不完整 |
| 9 | 场景演练 | **PASS** | 7 场景（happy/拒绝/异常终止/外部触发/超时/幂等/红冲链）经证据确认 |

---

## 4. 凭证状态机 ASCII 图

```
                ┌──────────────────────────────────────────────────────┐
                │                                                      │
                │   ┌──────────┐  postVoucher / persistVoucher          │
                │   │  DRAFT   │ ─────────────────────────┐             │
                │   │ (NORMAL, │                          ▼             │
                │   │  isRev=  │                   ┌────────────┐       │
       手工创建 ────►│ false)   │                   │  POSTED    │       │
                │   └────┬─────┘                   │ (NORMAL,   │       │
                │        │                         │  isRev=    │       │
                │        │ 默认 delete             │  false)    │       │
                │        │ (logical delete,        └─────┬──────┘       │
                │        │  docStatus 不变)              │              │
                │        ▼                              │ reverseProcess│
                │   (CANCELLED 状态                    │ /reverseVoucher│
                │    不可达 — P1)                      ▼              │
                │                              ┌────────────────┐      │
                │                              │ POSTED         │      │
                │                              │ (NORMAL,       │      │
                │                              │  isRev=true)   │      │
                │                              │ [原凭证已红冲] │      │
                │                              └────────────────┘      │
                │                                       ▲              │
                │                                       │              │
                │                  生成红字凭证         │              │
                │                  ┌──────────────────┐│              │
                │                  │ POSTED           ││              │
                │                  │ (REVERSAL,       ││ markOriginal │
                │                  │  isRev=true)     ││ VoucherReversed
                │                  │ [红字凭证]       │└──────────────┘
                │                  └──────────────────┘                │
                │                  (无出边 — postingType=REVERSAL      │
                │                   被 findAllPostedVouchers 过滤)     │
                │                                                      │
                └──────────────────────────────────────────────────────┘
```

---

## 5. 各维度详细裁决

### 5.1 维度 1：状态定义

**裁决**：FAIL (P1)

**审查**：
- `docStatus`（DRAFT/POSTED/CANCELLED）三态本身清晰——DRAFT="等待过账"，POSTED="已过账参与总账"，CANCELLED="作废"。每个状态名清楚表达业务等待点。
- `isReversed`（布尔）：在**原凭证**上表达"已被红冲"（合法用途）；在**红字凭证**上同时为 true（`persistVoucher:789 voucher.setIsReversed(isReversed)` 传入 `isReversed=true`），语义混淆——红字凭证是"已红冲的产物"还是"被红冲的原凭证"？owner doc `state-machine.md:37-42` 终端 POSTED/CANCELLED 未明确红字凭证的终态归属。
- `postingType`（NORMAL/REVERSAL/BUDGET/COMMITMENT）：本身语义清晰，但与 `isReversed` 存在**冗余轴**——红字凭证可由 `postingType=REVERSAL`+`reversalOfVoucherId!=null` 完全推导，`isReversed=true` 是冗余标记；原凭证被红冲可由"存在指向它的 postingType=REVERSAL 凭证"推导。owner doc 未澄清两轴的语义分工。

**P1-MA2-031（部分）**：红字凭证终态归属未在 owner doc 明确，`isReversed` 在红字凭证上语义混淆。
- 严重性：major（不破坏运行时正确性——`findAllPostedVouchers` 用 `postingType=REVERSAL` 阻断循环，不依赖 `isReversed` 区分；但破坏审计/排障可读性，UI 显示红字凭证时"是否红冲"字段误导）。
- 修复建议：MR1 在 owner doc `state-machine.md §3 终态与恢复` 追加红字凭证终态表（红字凭证 = POSTED+REVERSAL，是"红冲产物"而非"被红冲"），或拆分 `isReversed` 为 `isReversalProduct`（红字凭证）+ `isReversed`（原凭证被红冲）两标记（推荐方案 A：owner doc 澄清，零代码变更）。

### 5.2 维度 2：转换完整性

**裁决**：FAIL (P1)

**审查**：
- DRAFT→POSTED：经 `postVoucher:88-100`（手工 UI 入口，前置校验 docStatus=DRAFT）或 `persistVoucher:793`（引擎自动过账直写 POSTED，不经 DRAFT）。前置条件：借贷平衡（`assertBalanced:717-723`）+ 期间开放（`resolveOpenPeriod:495-512`）+ 科目有效（`resolveSubjects:562-618`）+ 汇率存在（多币种归 P1-MA2-002/009 MR1）。✓
- POSTED→红字凭证：经 `reverseProcess:209-257`，找 POSTED+未红冲原凭证 → 建负数 draft → 持久化新凭证（`postingType=REVERSAL`+`isReversed=true`+`reversalOfVoucherId`）→ `markOriginalVoucherReversed` 原凭证 isReversed=true。✓
- DRAFT→CANCELLED：owner doc `state-machine.md:35` 声明"直接作废（未影响总账，无需红冲）"，但**代码无显式 action**（grep `setDocStatus` 仅 `ErpFinVoucherBizModel.postVoucher:95` 置 POSTED + `ErpFinConsolidationEliminationBizModel:187` 置 DRAFT，无任何路径置 CANCELLED）。配合 `useLogicalDelete="true" deleteFlagProp="delVersion"`，CrudBizModel 默认 delete 走逻辑删除（`delVersion++`），**CANCELLED 状态在 DB 永不出现**。

**P1-MA2-031（核心）**：CANCELLED 状态不可达 + DRAFT→CANCELLED 缺失 action。
- 严重性：major（状态机声明与实现不一致；UI 筛选 CANCELLED 时永远空集；但实际业务无影响——DRAFT 凭证可逻辑删除，POSTED 凭证红冲纠正）。
- 修复建议：MR1 裁决——方案 A（推荐）：owner doc `state-machine.md` 删除 CANCELLED 状态（DRAFT 废弃走 `useLogicalDelete` 而非状态迁移），同步删除 `erp-fin/voucher-status` 字典的 CANCELLED 项；方案 B：实现显式 `cancelVoucher` action 置 `docStatus=CANCELLED`（不允许逻辑删除）。推荐 A（与 `useLogicalDelete=true` 的 ORM 设计一致）。

### 5.3 维度 3：终端状态和恢复

**裁决**：FAIL (P1)（与 §5.1 同根因）

**审查**：
- POSTED 是否真终态：**否**——可被红冲（`reverseProcess` 找 POSTED+未红冲凭证）。"红冲后"的 POSTED+isReversed=true 是真终态（不可再红冲）。
- CANCELLED 是否终态：**理论是**，但**不可达**（见 §5.2）。
- 红冲后原凭证 `isReversed=true` 但 `docStatus` 仍 POSTED——是否可"撤销红冲"（恢复）：**无显式 action**。若红字凭证本身有错（如红冲错了原凭证），当前无路径恢复原凭证 `isReversed=false`。owner doc 也未规定恢复路径。
- 红字凭证终态归属：owner doc `state-machine.md:37-42` 未明确，与 §5.1 同 finding。

**P1-MA2-031（同 §5.1）**：归档（红字凭证 + 已红冲原凭证）与活动凭证（POSTED+isReversed=false）的区分经 `isReversed`+`postingType` 联合判定可工作，但语义未在 owner doc 澄清。修复同 §5.1。

### 5.4 维度 4：异常路径

**裁决**：PASS

**审查**：
- 不平衡拒绝：`assertBalanced:717-723` 抛 `ERR_UNBALANCED`，凭证不创建。✓
- 期间关闭拒绝：`resolveOpenPeriod:495-512` 抛 `ERR_PERIOD_CLOSED`，凭证不创建。✓
- 模板缺失：`resolveProvider:486-493` 抛 `ERR_NO_PROVIDER`。✓
- 科目缺失：`resolveSubjects:600-610` 抛 `ERR_SUBJECT_NOT_FOUND`。✓
- 异步失败：`erp-fin.reversal-dispatch-mode=ASYNC` afterCommit + listener 失败经 `ErpFinReversalListenerRegistry.dispatch:92-116` try/catch 隔离 → 落 `ErpFinPostingException` PENDING。✓
- 红冲失败：`reverseProcess:217-220` 抛 `ERR_REVERSE_SOURCE_NOT_FOUND`，监听者失败抛 `ERR_REVERSAL_LISTENER_FAILED`。✓
- 并发过账：`(billHeadCode, businessType)` 幂等键经 `alreadyPosted:472-484` 兜底（**P2-MA2-025 期间门控并发敏感点交接 A2.17**）。
- 幂等重发：`alreadyPosted:472-484` 显式排除 `isReversed=true`，允许同 billCode 重新过账（红冲后重新过账场景）。✓
- 重复触发器幂等性：`ErpFinVoucherBillR` 反查 + docStatus=POSTED + !isReversed 三重过滤。✓
- 重试上限：`ErpFinDeferredPostingRetryHelper` MAX_RETRY=3 后转 IGNORED/MANUAL（`TestErpFinPostingExceptionWorkbench` 覆盖）。✓

**注**：ASYNC dispatch 模式下 listener 失败仅落异常工作台 PENDING；若无人处置，业务单据状态悬挂——归 §5.8 P1-MA2-032。

### 5.5 维度 5：可达性

**裁决**：PASS

**审查**：
- 从 DRAFT 可达 POSTED（手工 postVoucher / 引擎 persistVoucher）✓
- 从 POSTED 可达 红字凭证（reverseProcess）✓
- CANCELLED 不可达（见 §5.2 P1-MA2-031）—— 归 §5.2 finding
- **重点核验（无限循环风险）**：`findAllPostedVouchers:866-882` 过滤条件：
  ```java
  voucher != null
    && VOUCHER_STATUS_POSTED.equals(voucher.getDocStatus())
    && !Boolean.TRUE.equals(voucher.getIsReversed())
    && (voucher.getPostingType() == null
        || Objects.equals(voucher.getPostingType(), POSTING_TYPE_NORMAL))
  ```
  红字凭证 `postingType=REVERSAL` 被排除 → **不可被再红冲**。INFINITE LOOP BLOCKED ✓。
- `markOriginalVoucherReversed:909-923` 仅标记 NORMAL 过账类型凭证（line 917-918），REVERSAL 跳过 → 双重防护。✓
- **P2-MA2-033（新）**：红字凭证可再红冲的负向测试缺失（结构上已阻断但无测试断言）。
- commitment 凭证循环（commit→release）：合法循环有退出条件（`ErpPurOrder` CANCELLED 或 `ErpPurInvoice` approve 触发 release）✓

### 5.6 维度 6：角色和权限

**裁决**：PASS (P1 维持)

**审查**：
- `post/reverse/postVoucher/reverseVoucher` 均标注 `@BizMutation`（A6.1 全域 grep 待执行，本审计仅确认 finance 子集）。
- **重点 P1-MA2-021 升级评估**：`postVoucher/reverseVoucher:88-114` 仅校验凭证自身 `docStatus`，**不校验期间状态**——CLOSED_FINAL 凭证可被修改/红冲。

  **裁决：维持 P1 不升 P0**。理由：
  1. **正确路径存在**：CLOSED_FINAL 期间凭证修改/红冲的正确业务路径是反结账（owner doc `state-machine.md:113-114` 场景 C：反结账→修改/红冲→重新结账），而非直接修改。直接修改已结账期间凭证违反会计准则，应经反结账流程。
  2. **影响范围有限**：实际影响仅 UI 手工按钮入口 `postVoucher`/`reverseVoucher`。业务自动过账入口 `post()` 走 `resolveOpenPeriod:495-512` 强制期间校验，CLOSED_FINAL 期间自动过账被拒绝。引擎产出的凭证 DRAFT→POSTED 不经 `postVoucher` 路径。
  3. **不破坏总账平衡**：即便用户红冲 CLOSED_FINAL 凭证，红冲仍生成红字凭证，借贷平衡（`buildReversalDraft` 取负数行）；破坏的是 CLOSED_FINAL 不可变性承诺（业务规则违反），非数据一致性破坏。
  4. **修复路径已明确**：MR1 经 P1-MA2-021 修复（`postVoucher`/`reverseVoucher`/update/delete 前校验 `period.status != CLOSED/CLOSED_FINAL`，或 owner doc 标注"锁定语义经期间状态机+操作权限间接保证"）。
  
  本审计不预判升级（与 plan §Draft Review Record 非阻塞注记一致）。**维持 P1-MA2-021 不升 P0**。

- 多角色冲突（制单员 vs 审核员 vs 会计）：归 A6.1/A6.2 全域权限审计，本审计范围外。
- 角色名与状态名同业务词汇表：owner doc `state-machine.md §6` 角色表用"财务员/管理员"，与 dict 标签一致 ✓。

### 5.7 维度 7：外部依赖

**裁决**：PASS

**审查**：
- **8 域 posted 反写**：源业务单据（purchase/sales/inventory/manufacturing/assets/projects/quality/maintenance/hr）的 `posted`/`postedAt`/`postedBy` 字段由域调用方在 `IErpFinVoucherBiz.post` 返回 voucherId 后自行置位（owner doc `posting.md §反写契约`）。反写契约一致 ✓。
- **外部过账回调（ASYNC）入站通道**：`erp-fin.reversal-dispatch-mode=ASYNC` afterCommit 派发 `VoucherReversedEvent` → 域监听者回写自身状态；listener 失败入异常工作台。✓
- **外部系统超时/不可用回退**：`ErpFinDeferredPostingRetryHelper` MAX_RETRY=3 后转 IGNORED/MANUAL（归 §5.8 P1-MA2-032）。
- **commitment/intercompany dormant Provider 直写凭证**：`CommitmentVoucherGenerator:89` + `BudgetVoucherGenerator:90` + `IntercompanyVoucherGenerator:134` 直接 `daoFor(ErpFinVoucher.class).updateEntity`——但**均在 finance 域内**（grep 跨域 `daoFor(ErpFinVoucher` 在 finance 外零命中），config-gated 默认关闭，owner doc `posting.md §承付/跨法人内部交易` 已显式裁定为"不走 Provider 路由"（Non-Goal §72）。**非状态机 bypass**，归 owner doc 裁定 ✓。
- **8 域 reversal 统一路径复核（P0-MA1-021 复核）**：跨域 `daoFor(ErpFinVoucher` 在 finance 外零命中，38 Provider 一致经引擎 reverse 路径，无域绕过引擎直写凭证。✓

### 5.8 维度 8：TODO / 任务策略

**裁决**：FAIL (P1)

**审查**：
- `ErpFinPostingException` 状态机（PENDING/RETRYING/RETRIED/IGNORED/MANUAL）覆盖：
  - "需要人工决策" = MANUAL（分配给财务员手工补录）✓
  - "只是等待" = RETRYING（监控任务，重试中）✓
  - "准备好需确认" = RETRIIED（确认任务）✓
  - "无未处置异常" = PENDING（异常工作台入口）✓
- **重点核验 IGNORED 后凭证悬挂**：异常工作台 PENDING 入口存在（`TestErpFinPostingExceptionWorkbench` 覆盖），但 **IGNORED 是显式"放弃处置"状态**——若业务自动过账失败（凭证未创建 → 业务侧 `posted=false`）后异常被显式置 IGNORED，凭证永不创建，业务侧 `posted=false` 永久悬挂。`TestErpFinPostingExceptionNotify` 覆盖告警通知，但告警通道仅是日志/通知（无强制处置门控）。期末结账前置检查扫描未处置异常（`period-close.md`）阻止结账是间接保护，但日常运营中 IGNORED 凭证可静默下沉。

**P1-MA2-032**：IGNORED 凭证悬挂缺告警闭环。
- 严重性：major（不破坏状态机正确性，但破坏运营可观测性——IGNORED 后业务单据 `posted=false` 永久悬挂，期末结账虽拦截但日常无门控）。
- 修复建议：MR1 裁决——方案 A：IGNORED 状态强制要求处置理由 + 关联业务单据标记（业务侧 `posted=false` 时反查关联异常记录的处置状态，IGNORED 时业务侧置 visible flag）；方案 B：期末结账前置检查增加 IGNORED 异常的强制复核（已有 PENDING 拦截，扩展至 IGNORED）。

### 5.9 维度 9：场景演练（最重要）

**裁决**：PASS

**审查**（7 场景经证据确认）：

#### 场景 A：业务自动过账 happy path
1. 业务 approve（如 `ErpPurInvoiceProcessor.approve`）→ 调 `IErpFinVoucherBiz.post(event)` 
2. `ErpFinPostingProcessor.process:126-203`：幂等前置 → Provider 路由 → createFacts → resolveSubjects → balanceTotals → assertBalanced → resolveOpenPeriod → persistVoucher（docStatus=POSTED）→ 返回 voucherId
3. 域调用方置 `posted=true`/`postedAt`/`postedBy`
4. 业财回链 `ErpFinVoucherBillR` 记录 `(billHeadCode, businessType) ↔ voucherId`
- 覆盖测试：`TestErpFinPostingService` happy path、跨域 `TestErpPurInvoicePosting` 等 ✓

#### 场景 B：拒绝/返回
1. 不平衡：`assertBalanced:717-723` 抛 `ERR_UNBALANCED`，凭证不创建（无 DRAFT 残留——引擎直写 POSTED，前置全过才 saveEntity）。业务侧 `posted=false`。✓
2. 期间关闭：`resolveOpenPeriod:507-510` 抛 `ERR_PERIOD_CLOSED`。✓
- 覆盖测试：`TestErpFinPostingService`（不平衡拒绝 + 期间关闭拒绝）✓

#### 场景 C：异常终止
1. ASYNC reversal 失败 → `ErpFinReversalListenerRegistry.dispatch:92-116` try/catch 隔离 → 落 `ErpFinPostingException` PENDING
2. `ErpFinDeferredPostingRetryHelper` MAX_RETRY=3 → RETRYING → RETRIED 或 IGNORED/MANUAL
3. 凭证状态：红字凭证已 POSTED（法律效力，不回滚）；业务侧状态悬挂（归 §5.8 P1-MA2-032）
- 覆盖测试：`TestErpFinPostingExceptionWorkbench` + `TestErpFinReversalDispatch` + `TestErpFinPostingExceptionNotify` ✓

#### 场景 D：外部触发（业务域 reverse）
1. 业务域 reverse（如 `ErpPurInvoiceProcessor.reverseApprove`）→ 调 `IErpFinVoucherBiz.reverse(billHeadCode, businessType)`
2. `reverseProcess:209-257`：findAllPostedVouchers → buildReversalDraft → persistVoucher（红字凭证 postingType=REVERSAL）→ markOriginalVoucherReversed → dispatchReversalEvent
3. 域 listener 收到 `VoucherReversedEvent` → 回写自身状态（如 `approveStatus: APPROVED→REJECTED`）
- 覆盖测试：`TestErpPurFinanceReversalWriteback`/`TestErpSalFinanceReversalWriteback`/`TestErpInvFinanceReversalWriteback`/`TestErpMfgVarianceRecomputeReversal`/`TestErpMfgSubcontractReverse`/`TestErpMntVisitCancelReversal` ✓

#### 场景 E：超时
1. 重试上限 MAX_RETRY=3 后转 IGNORED/MANUAL ✓（同场景 C）

#### 场景 F：幂等
1. 重复 approve 同 billCode → `alreadyPosted:472-484` 检测 POSTED+!isReversed → 空操作返回 null
2. 红冲后重新过账 → `isReversed=true` 被排除 → 允许重新过账 ✓
- 覆盖测试：`TestErpFinPostingService` 幂等用例 ✓

#### 场景 G：红冲链
1. post → POSTED（NORMAL, isReversed=false）
2. reverse → 红字凭证（REVERSAL, isReversed=true）+ 原凭证 isReversed=true
3. 再 reverse（同 billCode）→ `findAllPostedVouchers` 找不到符合条件凭证（NORMAL+!isReversed）→ 抛 `ERR_REVERSE_SOURCE_NOT_FOUND`
- **覆盖测试**：间接覆盖（红冲链各域 reversal writeback 测试断言红字凭证存在+原凭证 isReversed=true），但**无显式"reverse of reversal raises ERR_REVERSE_SOURCE_NOT_FOUND"负向测试**（归 §5.5 P2-MA2-033）。

### 5.10 维度 10：与设计文档一致性

**裁决**：FAIL (P1)（与 §5.1/§5.2 同根因）

**审查**：
- owner doc `state-machine.md §1 状态定义` 列出 DRAFT/POSTED/CANCELLED 三态——但 CANCELLED 不可达（§5.2）。
- owner doc `state-machine.md §2 迁移完整性:35` 声明 "DRAFT → CANCELLED | 财务员 | 草稿状态 | 直接作废"——但代码无 action 实现。
- owner doc `state-machine.md §3 终态与恢复:37-42` 列出 POSTED/CANCELLED 为终态——但 POSTED 可被红冲（非真终态）；红字凭证终态归属未明确。
- owner doc `state-machine.md §9 场景 C:113-114` 描述反结账路径——但 P1-MA2-021 CLOSED_FINAL 锁定未实现（§5.6）。
- owner doc `state-machine.md §已知限制:249-265` 浏览器层 useWorkflow xwf 审批路径不可达（仅 DIRECT 三轴审批可达）——记录完整，影响凭证 POSTED 状态的可达路径仅限 DIRECT 审批驱动。✓

**P1-MA2-031（同 §5.1/§5.2）**：owner doc 与实现不一致。修复同 §5.2。

### 5.11 项目特定维度 11：businessType enum↔dict 一致性（P1-MA1-018 运行时复核）

**裁决**：PASS（P1-MA1-018 已登记 MR1）

**审查**：
- **4 项漂移确认**：
  - `MANUFACTURING_COST_CLOSE(100)` ↔ dict `PRODUCTION_COST`
  - `PROJECT_COST_COLLECTION(110)` ↔ dict `PROJECT_COST`
  - `PERIOD_CLOSE(120)` ↔ dict `PERIOD_CLOSING`
  - `EXCHANGE_GAIN_LOSS(130)` ↔ dict `FX_REVALUATION`
- **持久化站点确认**：`ErpFinPostingProcessor.persistVoucher:822,839,841,888` 全用 `businessType.name()`（即 enum 名，如 `PERIOD_CLOSE`/`EXCHANGE_GAIN_LOSS`）。`ExchangeRevaluationService:151-154,212-215` 写 `ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name()`。`ProfitLossClosingService:152-153` 写 `ErpFinBusinessType.PERIOD_CLOSE.name()`。
- **内部聚合一致性**：`ProfitLossClosingService:91` 用 `ErpFinBusinessType.PERIOD_CLOSE.name()` 排除自身分录——与持久化值一致 ✓。`ExchangeRevaluationService.aggregateBankSubjectBookFunctional:241-244` 用 `ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name()` 排除——与持久化值一致 ✓。
- **结论**：内部聚合全用 `enum.name()`，**非运行时正确性 bug**。但 UI dict 下拉值（如 `PERIOD_CLOSING`）与 DB 存储值（`PERIOD_CLOSE`）不符 → UI/审计筛选用 dict value 漏命中（如按 `PERIOD_CLOSING` 筛选找不到 `PERIOD_CLOSE` 持久化记录）。维持 P1-MA1-018（MR1 修复）。
- **P0-MA2-016 fix 复核**：`ProfitLossClosingService:88-93` 注释明示"仅排除 PERIOD_CLOSE（结转凭证自身分录，防重复结转）。汇兑重估凭证（EXCHANGE_GAIN_LOSS）的汇兑损益（费用类）须正常结转至本年利润"。代码 line 91 仅 `Objects.equals(bt, ErpFinBusinessType.PERIOD_CLOSE.name())` 排除，**未排除 EXCHANGE_GAIN_LOSS**。fix 正确 ✓。持久化值一致（两者均用 enum.name()），fix 不引入漂移。

### 5.12 项目特定维度 12：多币种凭证路径（P1-MA2-002/009 运行时复核）

**裁决**：PASS（P1-MA2-002/009 已登记 MR1）

**审查**：
- **line 级无 FX 折算确认**：`ErpFinPostingProcessor.persistVoucher:818-819` 写 `line.setAmountSource(amt)` + `line.setAmountFunctional(amt)`，两者同值（`amt` 来自 `VoucherFact.amount`，由 Provider 用源币金额填充）。
- **凭证状态机行为**：DRAFT→POSTED 不因币种失败——`assertBalanced:717-723` 用同一 `amt` 算借贷，数学上平衡（借=贷）。状态机正确 ✓。
- **业务正确性问题**：本位币金额 = 源币金额（同值），多币种下"借贷平衡"在本位币维度**数学上**成立但**业务上**漏汇兑损益——如外币 AR 收款时若汇率变动，应收（按历史汇率折算）与银行存款（按当期汇率）差额应进 6051 汇兑损益科目；`SalAcctDocProvider.RECEIPT` 无此 plug（P1-MA2-009 已登记 MR1）。
- **凭证状态机正确性不受影响**：状态机行为（DRAFT→POSTED 转移、POSTED→红冲）正确，问题是凭证**内容**业务正确性（漏汇兑损益），归 P1-MA2-002/009 MR1 修复。
- **结论**：状态机审计范围内 PASS；多币种业务正确性归 P1-MA2-002/009 MR1。

---

## 6. MA1/MA2 finding 运行时影响复核

| Finding ID | 状态 | 运行时影响复核结论 | 终态 |
|------------|------|-------------------|------|
| **P0-MA1-021** | done（plan 2026-07-27-1430-1） | 引擎 reversal 统一路径**已就位**：`markOriginalVoucherReversed:909-923` 在引擎内承担原凭证标记，38 Provider 一致。**跨域 grep `daoFor(ErpFinVoucher` 在 finance 外零命中**——无域绕过引擎直写凭证。commitment/intercompany/budget 三个 dormant Provider 直写均在 finance 内 + config-gated + owner doc 已裁定（Non-Goal）。 | **sustained done** |
| **P0-MA2-016** | fix-plan-injected（plan 2026-07-27-1949-arm-fix） | `ProfitLossClosingService:91-93` fix **已就位**：仅排除 `PERIOD_CLOSE`，`EXCHANGE_GAIN_LOSS` 正常结转。注释明示设计意图。持久化 `enum.name()` 一致，fix 不引入漂移。 | **sustained fix** |
| **P1-MA1-018** | todo MR1 | 4 项 enum↔dict 漂移**确认**。内部聚合用 `enum.name()` 一致，**非运行时正确性 bug**。UI/审计筛选漏命中（dict value ≠ DB 存储值）。 | **governance P1**（无升级） |
| **P1-MA1-022** | todo MR1 | mnt `MaintenanceLaborPostingDispatcher.voucherAlreadyExists:203-210` 跨域只读 `daoFor(ErpFinVoucherBillR.class)` **确认**。是 config-gated 性能预检，引擎 `alreadyPosted:472-484` 兜底，**非运行时正确性 bug**（重复检查冗余但状态机正确）。 | **governance P1**（无升级） |
| **P1-MA2-001** | todo MR1 | GRNI 自动冲回缺失——receive→invoice 凭证状态机交互：`PURCHASE_INPUT`（receive）与 `AP_INVOICE`（invoice）各自独立过账，互不 reversal。状态机行为正确（各自 POSTED，可独立红冲），但 GL 2202 暂估应付双计未自动清理。 | **business P1**（无升级） |
| **P1-MA2-002 + P1-MA2-009** | todo MR1 | 多币种 line 级无 FX 折算（`persistVoucher:818-819`）**确认**。状态机不因币种失败（数学平衡），但本位币金额=源币金额漏汇兑损益 plug。 | **business P1**（无升级，归 MR1） |
| **P1-MA2-021** | todo MR1 | CLOSED_FINAL 凭证锁定未实现**确认**。`postVoucher/reverseVoucher:88-114` 不校验期间状态。**升级评估裁决：维持 P1 不升 P0**（详见 §5.6）。 | **business P1**（无升级，归 MR1） |
| **P1-MA2-022** | todo MR1 | FX 凭证无前期 reversal + 无期间过滤**确认**：`revalueArAp:103-155` 查所有未核销外币项不按期间过滤，不 reversal 前期 FX 凭证，累计漂移。状态机行为正常（FX 凭证 POSTED 可红冲），但 IAS 21 spot-rate 语义未实现。 | **business P1**（无升级，归 MR1） |
| **P2-MA1-019** | watch-only | `ErpFinBusinessType.fromCode:86` 抛 `IllegalArgumentException` + `ErpFinVoucherTemplateBizModel:95` 用 `LocalDate.now()`。**确认**。programmer-error 路径，非 GraphQL 面向，严重性 P2。 | **watch-only**（无升级） |
| **P2-MA2-025** | watch-only（A2.17） | `closePeriod:157-158` 连续 setStatus 无 flush，CLOSING 永不对外可见——`resolveOpenPeriod:507` 期间门控的并发敏感点。**确认**。归 A2.17 并发审计。 | **watch-only**（交接 A2.17） |

**11 项 finding 运行时复核结论**：**0 项升级 P0**。已 done 的 P0-MA1-021 与 fix-plan-injected 的 P0-MA2-016 fix 均在实仓就位且正确；9 项 P1/P2 finding 的运行时行为与原登记一致，无新发现的运行时正确性破坏。

---

## 7. 并发敏感点交接 A2.17

| # | 敏感点 | 文件:行 | 风险 | 交接状态 |
|---|--------|---------|------|----------|
| 1 | `(billHeadCode, businessType)` 幂等键竞态 | `ErpFinPostingProcessor.alreadyPosted:472-484` + `findBillLinks:884-890` | 并发同 billCode 过账：两线程同时 `alreadyPosted=false` → 双双创建凭证 → 状态机不破坏但凭证重复 | **交接 A2.17** |
| 2 | 并发 reverse 同原凭证 | `reverseProcess.findAllPostedVouchers:866-882` + `markOriginalVoucherReversed:909-923` | 并发同 billCode 红冲：两线程同时找原凭证 → 双双生成红字凭证 → 原凭证 isReversed=true 双写（幂等）但红字凭证重复 | **交接 A2.17** |
| 3 | 期间门控并发（P2-MA2-025） | `ErpFinAccountingPeriodProcessor.closePeriod:157-158` + `resolveOpenPeriod:507` | CLOSING 中间态无 flush 致并发结账互斥失效；`resolveOpenPeriod` 与 `closePeriod` 间存在 TOCTOU | **交接 A2.17**（P2-MA2-025 已登记） |

本审计范围内**仅标注观察到的并发敏感点**，不做系统性并发正确性裁决（Non-Goal §70）。

---

## 8. 新发现汇总

### 8.1 P1-MA2-031 — DRAFT→CANCELLED 状态机迁移缺失 + 红字凭证终态归属未定义

- **报告**：本审计（A2.5a）
- **域**：finance
- **描述**：会计凭证状态机两项相关缺陷：(a) owner doc `state-machine.md §1+§2:35` 声明 DRAFT→CANCELLED 迁移 + CANCELLED 终态，但代码无 `cancelVoucher` action 实现，配合 `useLogicalDelete=true` 致 CANCELLED 状态在 DB 永不出现（CANCELLED dict 项不可达）；(b) owner doc `state-machine.md §3 终态与恢复:37-42` 未明确红字凭证（`postingType=REVERSAL`+`isReversed=true`）的终态归属，`isReversed` 标记在红字凭证上语义混淆（"已红冲的产物" vs "被红冲的原凭证"）。
- **运行时影响**：(a) UI 筛选 CANCELLED 永远空集；CANCELLED dict 项无业务效果（实际 DRAFT 废弃走逻辑删除）。(b) 审计/排障时红字凭证的"是否红冲"字段误导（红字凭证 isReversed=true 但其本身是冲销产物，未被再红冲）。**不破坏状态机运行时正确性**——`findAllPostedVouchers` 用 `postingType=REVERSAL` 阻断循环，不依赖 `isReversed` 区分。
- **严重性**：major（治理 + 文档一致性缺陷）
- **修复方式**：MR1 裁决——方案 A（推荐）：owner doc `state-machine.md` 删除 CANCELLED 状态（DRAFT 废弃走 `useLogicalDelete`），同步删除 `erp-fin/voucher-status` CANCELLED 项；owner doc §3 追加红字凭证终态表澄清 `isReversed` 在红字凭证 vs 原凭证上的语义。方案 B：实现显式 `cancelVoucher` action 置 `docStatus=CANCELLED` + 拆分 `isReversed` 为两标记。推荐 A（与 ORM `useLogicalDelete=true` 设计一致）。
- **目标 MR**：MR1

### 8.2 P1-MA2-032 — IGNORED 凭证悬挂缺告警闭环

- **报告**：本审计（A2.5a）
- **域**：finance
- **描述**：`ErpFinPostingException` 异常工作台 IGNORED 状态是显式"放弃处置"——若业务自动过账失败（凭证未创建 → 业务侧 `posted=false`）后异常被显式置 IGNORED，凭证永不创建，业务侧 `posted=false` 永久悬挂。`TestErpFinPostingExceptionNotify` 覆盖告警通知，但告警通道仅是日志/通知，无强制处置门控；期末结账前置检查扫描未处置异常（PENDING）阻止结账是间接保护，但日常运营中 IGNORED 凭证可静默下沉。
- **运行时影响**：IGNORED 后业务单据 `posted=false` 永久悬挂，日常运营无门控（仅期末结账间接拦截）。不破坏状态机正确性，破坏运营可观测性。
- **严重性**：major（运营可观测性缺陷）
- **修复方式**：MR1 裁决——方案 A：IGNORED 状态强制要求处置理由 + 关联业务单据 visible flag（业务侧 `posted=false` 时反查关联异常处置状态，IGNORED 时业务侧标记需人工复核）；方案 B：期末结账前置检查扩展至 IGNORED（已有 PENDING 拦截，扩展强制复核 IGNORED）。
- **目标 MR**：MR1

### 8.3 P2-MA2-033 — 红字凭证可再红冲的负向测试缺失

- **报告**：本审计（A2.5a）
- **域**：finance
- **描述**：`findAllPostedVouchers:866-882` 用 `postingType==NORMAL||null` 过滤阻断红字凭证被再红冲（结构上无限循环已阻断），但**无显式负向测试**断言此行为（如 `assertThat(reverse(reversalVoucher)).throws(ERR_REVERSE_SOURCE_NOT_FOUND)`）。结构变更若意外移除过滤条件，无测试保护。
- **运行时影响**：当前无（结构上正确）。未来回归风险（无测试护栏）。
- **严重性**：minor
- **处置**：watch-only，MR1 顺手补负向测试（红冲红字凭证应抛 `ERR_REVERSE_SOURCE_NOT_FOUND`）。

---

## 9. 残留风险

1. **P1-MA2-021 CLOSED_FINAL 凭证锁定**（维持 P1）：UI 手工入口 `postVoucher`/`reverseVoucher` 不校验期间状态。MR1 修复。日常运营可经操作权限 + 反结账流程间接保护。
2. **P1-MA2-031 状态机文档/实现不一致**：CANCELLED 不可达 + 红字凭证终态未定义。MR1 修复。不影响运行时正确性。
3. **P1-MA2-032 IGNORED 凭证悬挂**：日常运营无门控。MR1 修复。期末结账间接拦截。
4. **多币种凭证路径**（P1-MA2-002/009）：line 级无 FX 折算，状态机正确但凭证内容业务正确性有问题。MR1 修复。
5. **并发敏感点 3 处**（§7）：交接 A2.17 系统性并发审计。
6. **dormant Provider 直写凭证**（commitment/intercompany/budget）：owner doc 已裁定为合法 bypass，config-gated 默认关闭。启用时需 successor 审计。

---

## 10. Verdict

**Verdict: pass**（带 2 项 P1 + 1 项 P2 watch-only 残留风险）

**审查范围**：会计凭证状态机（docStatus DRAFT/POSTED/CANCELLED + isReversed + postingType NORMAL/REVERSAL/BUDGET/COMMITMENT 三轴组合）+ 9 个识别控制点 + 10 维度 + 2 项目特定维度 + 11 项 MA1/MA2 finding 运行时复核。

**可达性摘要**：DRAFT→POSTED 可达；POSTED→红字凭证可达；CANCELLED 不可达（P1-MA2-031）；红字凭证不可再红冲（无限循环已阻断）✓。

**角色/权限摘要**：`@BizMutation` 标注完整；P1-MA2-021 CLOSED_FINAL 锁定维持 P1 不升 P0（正确反结账路径存在 + 影响范围有限 + 不破坏总账平衡）。

**外部依赖摘要**：8 域 posted 反写经域调用方；ASYNC listener 失败入异常工作台；dormant Provider 直写经 owner doc 裁定；跨域 grep 零绕过引擎直写凭证。

**剩余风险或跳过的区域**：
- A2.5b 期间状态机（OPEN/CLOSING/CLOSED/CLOSED_FINAL）系统性审查 — 本审计仅覆盖凭证侧期间守卫（P1-MA2-021）
- A2.5c AR/AP 核销状态机 — 本审计仅确认过账引擎产出 AR/AP 凭证的金额一致性
- A2.17 并发与乐观锁 — 3 处并发敏感点交接
- A4.1a 过账引擎代码质量 — 异常类型/N+1/索引系统性审查
- commitment/intercompany/budget dormant Provider — owner doc 已裁定
- 浏览器层 useWorkflow xwf 审批路径 — `state-machine.md:249-265` 已记录不可达
