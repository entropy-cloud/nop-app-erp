# rc-ma4 A4.2.79 inventory 批次效期拦截落地后 reserved/available 一致性运行时验证审计报告

> Report Status: done
> Mission: requirement-compliance（MA4 核心域展开器运行时确认——A4.2.79 回队行）
> Work Item: A4.2.79（MA4 回队行：MR1 P1-RC-031 修复落地后 reserved/available 一致性——expiry check 拦截点选择 doConfirm vs doComplete 运行时确认）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 四级分级判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §去重协议）
> 计划：`docs/plans/2026-08-07-2340-1-rc-ma4-a4-2-79-inv-expiry-interception-reserved-available-consistency.md`
> Source Audits: `docs/audits/2026-08-03-1200-3-rc-ma1-a1-26-inventory-f2-batch-traceability-expiry-negative-stock.md`（A1.26 §7 SP-4）
> Skill: `docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定 Skill）+ `nop-testing`（测试探针，镜像 `TestErpInvBatchExpiryInterception`）
> 审计类型：**运行时行为验证（只读确认 + 测试探针，零生产代码/ORM/api.xml/view.xml/config 默认值/真相源变更）**

---

## 0. TL;DR 裁决表

| 维度 | 结论 |
|------|------|
| 拦截点运行时确认 | **主路径闭合**——expiry 检查实仓接线于 `validateAvailable`（doConfirm 内、applyReservation 前），`doComplete` 零 expiry 检查（SP-4 双分支全部成立） |
| SP-4 三断言 | **全部运行时成立**——expiry 拒绝路径 `applyReservation` 未执行 + `reservedQuantity` 不变 + `balance` 不变（既有 ①/⑦ 断言 + 实仓代码顺序证据 + 新增探针） |
| 新增 dedicated 探针（最强边界） | **通过**——批次余额行已有既有预留占用（reservedQuantity=3）时，过期拒绝的 confirm 不触碰既有预留/余额（reserved=3 / total=100 / available=97 全不变，移动单保持 DRAFT） |
| 新 finding | **0**（不新建，不重新裁决 P1-RC-031 分级） |
| MR0 | 不触发（运行时未发现活跃数据破坏） |
| 验证 | `mvn test -pl module-inventory/erp-inv-service` **152 tests 全绿**（既有 151 + 新增探针 1 零回归）；compliance checker actual == baseline 零漂移（R2c=1383 等 19 规则一致） |
| roadmap | A4.2.79 `todo → done ✅` |

---

## 1. 存疑点原文（A1.26 §7 SP-4，三列摘录合并，非逐字单列引用）

A1.26 审计报告 `2026-08-03-1200-3-...-a1-26-...md:209` 存疑点行三列（存疑点描述 + 触发条件 + 验证方式）合并摘录如下：

> **SP-4**：MR1 修复落地后 reserved/available 一致性（与 A1.8 SP-3 同根因）：UC-INV-02 `validateAvailable` + `applyReservation` 在 `doConfirm` 内顺序执行（校验 → 占预留）。当前实现下，UC-INV-02 拒绝路径不进入 applyReservation（满足"余额不变"）。未来若 MR1 P1-RC-031 修复在 `validateAvailable` 内增 expiry check（早于 applyReservation），expiry 拒绝路径同样不进入 applyReservation——一致性保持。但若修复扩展到 `doComplete`（DONE 时再校验 expiry），须确认 reserved 已被 release（避免假阴性）。
>
> 触发条件：MR1 P1-RC-031 修复实现的拦截点选择（doConfirm vs doComplete）。
>
> 验证方式：A4.1/MR1 修复 plan 自身审计——mock `validateAvailable` 抛 `ERR_BATCH_EXPIRED` → 断言 `applyReservation` 未执行 + `reservedQuantity` 不变 + `balance` 不变。

---

## 2. 拦截点运行时证据（Phase 1）

### 2.1 实仓代码顺序（`ErpInvStockMoveProcessor.java`，只读 read）

- **`doConfirm:97-109`**：状态迁移守卫 `:99-104` → **`validateAvailable:105` → `applyReservation:106`** → `setDocStatus(CONFIRMED):107` → `saveOrUpdateEntity:108`。**校验先于占预留**，拒绝路径抛异常即短路，不进入 `applyReservation`。
- **`validateAvailable:127-149`**：**首行接线 `validateBatchExpiry:129`**（注释 `:128`「效期守卫为合规门禁，置于负库存短路之前（RC-R1.20 Decision：allow-negative-stock 不豁免批次过期）」）→ 负库存短路 `:130-132` → 类型范围 `:133-135` → per-line 可用量校验 `:136-148`。**expiry 检查先于任何 balance 触碰**（`upsertBalance` 仅在校验循环内 :137，且 expiry 拒绝早于该循环——探针实仓可执行性前提）。
- **`validateBatchExpiry:159-188`**：config 门控 `:160-162` → 类型范围（reservesOnConfirm 出库/内部转移）`:163-165` → per-line 带 batchNo 行 `:167-170` → 物料 isBatchManaged 经 `IErpMdMaterialBiz.get(id, true)` 管道读取（容忍物料不存在跳过）`:171-175` → `expiryDate == null` 跳过（A4.2.78 null 语义）`:176-180` → **`expiryDate.isBefore(today)` 抛 `ERR_BATCH_EXPIRED`**（param ARG_MATERIAL_ID/ARG_BATCH_NO/ARG_EXPIRY_DATE）`:181-186`。
- **`doComplete:111-125`**：状态守卫 `:113-119` → `releaseReservation:120` → `bookkeeper.bookCompletion:121` → `setDocStatus(DONE):122` → `saveOrUpdateEntity:123` → **`postingDispatcher.dispatchIfApplicable:124`（末步）**。**全文无任何 expiry 检查**——「修复放 doComplete 时 reserved 已 release 致假阴性」的窗口**不存在**。

### 2.2 既有测试复跑证据

`mvn test -pl module-inventory/erp-inv-service`（2026-08-08 复跑）：**152 tests，0 Failures，0 Errors，BUILD SUCCESS**。其中 `TestErpInvBatchExpiryInterception` 9 组全绿（既有 8 组 + 新增探针 1 组）。

既有断言（RC-R1.20 落地，保留实证）：

- **① `testExpiredBatchRejectedOnConfirm`**：`assertNull(move)`（整笔回滚不残留移动单）`:107` + `assertNull(balance)`（拒绝路径不进入 applyReservation/upsertBalance，不产生余额行）`:110`。
- **⑦ `testTwoStepConfirmRejectedKeepsDraft`**：confirm 拒绝后 `assertNotNull(move)` + `assertEquals(DRAFT, move.getDocStatus())`（applyReservation 未执行）`:150-151` + `assertNull(balance)`（不占用预留量/不产生余额行）`:155`。

### 2.3 SP-4 双分支结论

- **分支 A「拒绝路径不进入 applyReservation → reserved/balance 不变」：运行时成立**。实仓顺序 `doConfirm:105-106`（validateAvailable → applyReservation）+ `validateAvailable:129` 首行接线 expiry 守卫——`validateBatchExpiry` 抛 `ERR_BATCH_EXPIRED` 时异常沿 @BizMutation 事务回滚，`applyReservation`（:106）不执行。①/⑦ 既有断言（assertNull balance / DRAFT 保持）实证「不产生余额行、不占预留」；新增探针（§3）实证「既有预留/余额不被触碰」。
- **分支 B「doComplete 再校验致假阴性」：窗口不存在**。`doComplete:111-125` 零 expiry 检查（grep `validateBatchExpiry|getExpiryDate|ERR_BATCH_EXPIRED` 于 doComplete 方法体零命中），不存在「DONE 时再校验」路径，故「reserved 已 release 后校验致假阴性」的担忧无实仓落点。

---

## 3. dedicated 运行时探针（Phase 2，A4.2.79 验证缺口补齐）

### 3.1 探针设计（`TestErpInvBatchExpiryInterception#testExpiryRejectedLeavesExistingReservationIntact`）

**seed 约束（review 修订后方案）**：冻结时钟（`InvFrozenClockExtension` REFERENCE_DATE=2026-07-17）下过期批次守卫自 seed 起即激活——「他单 confirm 落预留」不可行（该 confirm 自身会被拒）；`ErpInvStockReserve` 实体不存在（全仓零命中）——预留载体即 `ErpInvStockBalance.reservedQuantity`。故**扩展 `seedBalance` helper（`:413-431`，现硬编码 reservedQuantity=ZERO）增加 overload 直接 seed `reservedQuantity>0`**：reserved=3 / total=100 / available=97（available = total − reserved − locked 保持一致）。

**探针流程**（镜像 ⑦ 两步流 + ① 拒绝断言范式）：

1. seed 过期批次（BATCH-EXPIRED-01 / MATERIAL_BATCH / expiry=yesterday）+ seed 余额行 reserved=3 / total=100 / available=97。
2. 两步流：CRUD `ErpInvStockMove__save`（DRAFT）+ `ErpInvStockMoveLine__save`（qty=5, batchNo=过期批次）→ `ErpInvStockMove__confirm(moveId)`。
3. 断言：拒绝 `ERR_BATCH_EXPIRED` + 错误消息含物料/批次/效期三参数 → 移动单保持 DRAFT → **余额行 reservedQuantity 原值不变（3）** + total 不变（100）+ available 不变（97）。

**断言可区分性**：无守卫时 `applyReservation` 会把 reserved 抬至 3+5=8、available 重算至 92——断言 reserved==3 / total==100 / available==97 可区分「守卫拦截」与「无守卫放行」两种结局。

### 3.2 探针结果

- **探针测试 PASS**（`_cases/.../testExpiryRejectedLeavesExistingReservationIntact/` 快照录制落盘：`1_confirm_rejection_code.json5` = `"erp.err.inv.batch-expired"` + `1_balance_state.json5` = total=100.0000 / available=97.0000 / reserved=3.0000 / locked=0.0000 / avgCost=10 / totalCost=1000，与 seed 一致）。
- **一致性未破坏**：拒绝路径未触碰既有预留/余额（reserved=3 原值不变、available=97 不变、total=100 不变），移动单保持 DRAFT（无 applyReservation 副作用——预留未新增/未释放）。
- 按计划 Exit Criteria 预期行为实现——**未升级 finding**（若探针暴露一致性破坏则按 methodology 判据处置；实仓未发生）。

### 3.3 SP-4 三断言完整运行时证据链

| SP-4 断言 | 证据 |
|-----------|------|
| `applyReservation` 未执行 | ⑦ DRAFT 保持 + 探针 reserved=3 原值不变（若执行则抬至 8）+ ① assertNull(balance) |
| `reservedQuantity` 不变 | ①/⑦ assertNull(balance)（无行=无占用）+ 探针 reserved=3 原值不变（既有预占边界） |
| `balance` 不变 | ①/⑦ assertNull(balance)（不产生行）+ 探针 total/available/locked 全不变（既有预占边界） |

---

## 4. 与既有 finding 衔接（去重声明）

| Finding ID | 域 | 本审计结论 | 维持/变更 |
|-----------|---|--------------|----------|
| `P1-RC-031`（UC-INV-06 效期拦截缺失 + isBatchManaged 条件分支） | inventory | 本审计确认修复落地后一致性成立（拦截点 = validateAvailable 首行，doConfirm 内、applyReservation 前；doComplete 零 expiry 检查；拒绝路径不触碰既有预留/余额）。 | **维持 done（RC-R1.20）**——本审计只做落地后运行时一致性验证，**不重新裁决/不撤销分级**（plan Non-Goals） |
| `P1-MA2-062`（StockTake completeTake stub） | inventory | 本审计非盘点维度，无增量。 | 维持（不重审） |
| `P0-MA2-020`（StockBalance 自然键 UK） | inventory | 探针 seed 余额行经 `daoProvider.daoFor(ErpInvStockBalance)` 显式 ID 落库，与 UK 无冲突；拒绝路径不触碰余额行，无并发增量。 | 维持 done |
| `P1-MA4-001` family / `P2-MA2-028` / `P1-MA4-021` / `P1-MA4-020` | finance/inventory | 本审计非这些维度，无增量。 | 维持 |

**无新 finding 新建**（0 新 finding，全部衔接既有分级）。**A4.2.3（P1-RC-008 预留写路径）不覆盖**——不同控制点（拦截点一致性验证 vs 预留写路径实现），仍 MR1-blocked todo（plan Deferred But Adjudicated）。运行时未发现活跃数据破坏，**不触发 MR0**。

---

## 5. 裁决摘要

| 工作项 | 存疑点 | 运行时裁决 |
|--------|--------|-----------|
| A4.2.79 | SP-4 MR1 P1-RC-031 修复落地后 reserved/available 一致性（expiry check 拦截点选择 doConfirm vs doComplete） | **主路径闭合 / 一致性成立，0 新 finding**——拦截点 = `validateAvailable`（doConfirm 内 :105、applyReservation :106 之前，:129 首行接线）；`doComplete:111-125` 零 expiry 检查（分支 B 假阴性窗口不存在）；SP-4 三断言全部运行时成立（applyReservation 未执行 / reservedQuantity 不变 / balance 不变）；新增既有预占边界探针（reserved=3 原值不变）补齐 A4.2.79 验证缺口 |

**整体裁决**：A4.2.79 运行时一致性验证完成。**0 新 finding / 0 翻转 / 不触发 MR0 / 不归 MR1（本审计）**。P1-RC-031 修复（RC-R1.20）落地后的一致性语义——包括最强边界「既有预留占用下过期拒绝不触碰预留/余额」——经 dedicated 探针运行时证实成立。A4.2.79 在 MA4/R1.0 链路的运行时证据缺口**闭合**。

---

## 6. 过程纪律自检

- [x] **checker 门控核查**：本审计零生产代码变更（仅测试探针新增 + 文档更新，测试类目限 erp-inv-service），checker 无回归风险。本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`——**actual == baseline**（R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=229/R2c=1383/R2d=34/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=7/R11=0/R12a=69/R12b=66/R12c=40，与 `compliance-baseline.md` §BASELINE machine-readable 块逐行一致，0 漂移）。checker 脚本为纯 reporter 退出码恒 0，真正门控在 CI workflow 解析 actual > baseline => sys.exit(1)；本报告以 actual == baseline 作为零漂移依据。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本审计全部结论衔接既有 finding（P1-RC-031 维持 done / P1-MA2-062 + P0-MA2-020 + P1-MA4-001 family + P2-MA2-028 + P1-MA4-021 + P1-MA4-020 维持），无未经比对直接新建的 finding。A4.2.79 一致性注记已追加至 arm-index P1-RC-031 行。
- [x] **业财保护区域探针纪律**：探针仅 seed `ErpInvStockBalance.reservedQuantity` 余额行 + 两步流 confirm 拒绝断言——**READ-ONLY 不改生产代码/ORM/api.xml/view.xml/config 默认值/真相源**；不触及 StockMoveBookkeeper/InvPostingDispatcher/过账逻辑（拒绝路径在 validateBatchExpiry 即短路，未达过账）。
- [x] **分层与去重协议**：A4.2.79 与 A4.2.3（MR1 P1-RC-008 预留写路径，仍 MR1-blocked todo，不覆盖）不同控制点（拦截点一致性验证 vs 预留写路径实现）；与 A4.2.76（负库存下界 watch-only）不同维度；不重复核实 P1-RC-031 分级（RC-R1.20 已落地，无分级裁决义务）。

---

## 7. 完整性自检

- [x] 存疑点原文摘录（三列合并，非逐字单列——已标注）
- [x] 拦截点证据（实仓 file:line 顺序 + 测试复跑结果 + SP-4 双分支结论）
- [x] 测试证据（新增探针 + 快照录制 + 断言可区分性）
- [x] §2 判据（0 新 finding，维持既有分级，去重声明）
- [x] 过程纪律自检（checker 门控核查 + closure-audit 独立性声明 + 交叉去重 + 业财保护区域探针纪律）
