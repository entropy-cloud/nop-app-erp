# rc-ma1-a1-23 assets-F2 处置 需求-实现符合性审计报告

> Report Status: active
> Mission: requirement-compliance
> Work Item: A1.23（MA1 需求追踪矩阵审计 — assets-F2 处置，UC-AST-04/05，2 UC）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§1 五级追踪矩阵 / §2 四级分级判据 / §3 完整枚举 / §4 Q1 真相源层级 + 三判据 / §5 Q4 修复义务 + 保护区域暂停协议 / §6 9 段报告骨架 / §7 arm-index 衔接 / §8 过程纪律自检 / §9 真相源冻结 / §10 MR0/MR1 通道 / §去重协议）
> 锚点：`docs/audits/rc-requirement-baseline-inventory.md`（A1.23 UC 锚点 = UC-AST-04/05，覆盖率 ✅ 一致）
> L1 真相源：`docs/design/assets/use-cases.md`（机制见 `docs/design/assets/depreciation-and-posting.md` §三/§十 + `docs/design/assets/state-machine.md` §3/§实现约定 — L2 设计参考，非真相源；冲突以 L1 为准）
> L5 既有证据复用：A2.10（`2026-07-28-0400-arm-ma2-assets-state-machine.md`，资产状态机 §场景(c) 报废 / §场景(d) 出售 PARTIAL PASS + P1-MA2-060 resolved R1.16）/ A4.3（`2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md`，P1-MA4-013/014/015 resolved）/ A1.22（`2026-08-03-0900-2-rc-ma1-a1-22-assets-f1-depreciation-engine.md`，UC-AST-07 方式B 补提缺失 P1-RC-029 = 本切片 UC-AST-05"先补提"前置证据）

---

## 9. 与 MA2 报告差异增量声明（前置段，对应方法论 §去重协议）

本报告**不复跑 MA2/A4.3/A1.22 既有行为审计**，按 §去重协议只补"需求契约↔实际行为"差异：

- **复用 A2.10**（assets 状态机）：§场景(c) 资产报废（**PARTIAL PASS**：Disposal approve → 资产 SCRAPPED + cancelPendingSchedules + DISPOSAL 凭证；**场景(c2) posted=false 窗口期 reverseApprove 不回滚资产** → P1-MA2-060 `:230`）+ §场景(d) 资产出售（**PARTIAL PASS**：disposalType=SOLD → SOLD + 出售清理凭证 `:232-234`）+ §维度 2 终态守卫（SCRAPPED/SOLD 无出边 PASS `:138`）+ §维度 2 资本化/处置前置资产状态守卫（`validateAssetDisposable` PASS `:140`）。处置状态迁移/终态守卫/损益计算/幂等 reverse 行为由 A2.10 证实，本切片不重测。
- **复用 A4.3**（折旧引擎/Processor 链路代码质量）：P1-MA4-013（折旧/Cap/Disposal dispatcher posted=false 业财悬挂）**resolved R1.16**（告警派发 `IErpSysNotificationBiz` + DeferredPostingSweepJob 兜底）；P1-MA4-014（处置/Processor 测试有效性）**resolved R2.12**；P1-MA4-015（跨域 daoFor）**resolved 永久只读豁免**。
- **复用 A1.22**（assets-F1 折旧引擎 RC 切片）：UC-AST-07 方式B 当期一次性补提缺失（**P1-RC-029**）是本切片 UC-AST-05"先补提当期折旧至出售日"的**前置证据/同根因下游投影**——补提 mutation 能力（catchUpDepreciation）不存在，故处置路径即便想调用也无从调用。本切片 #1 finding（UC-AST-05 出售补提缺失）与 A1.22 P1-RC-029 同根因（补提能力缺失），但**不同 UC 不同控制点**（UC-AST-05 处置路径未调补提 vs UC-AST-07 补提 mutation 不存在），按 §7 复用 P1-RC-029 不新建编号（详见 §6.1）。
- **本切片只补的需求视角差异**（候选缺口 #1-#4 见 §5）：①UC-AST-05 出售前补提当期折旧至出售日**完全缺失**（倾向 **P1**——会计正确性：累计折旧低估→净值高估→清理损益误算→GL 6711/6301 错报；**reuse P1-RC-029** 同根因下游投影）/ ②UC-AST-04 处置凭证合并"固定资产清理"中间科目腿（doc drift：L1:72-74 + owner doc §三:136-137 要求含固定资产清理腿 vs 实现/§一:19-20 无；§十 Deferred 仅覆盖类型命名未覆盖科目腿合并，复核 §4 三判据 (ii) **预期不满足**→倾向重新打开 **P1**）/ ③DISPOSAL 无浏览器层 E2E（E2E seed 缺处置行，倾向 **P2** successor）/ ④P1-MA2-060 resolved 状态 HEAD 复核（验证 R1.16 修复实际落地）。

---

## 1. 需求契约原文（2 UC 验收标准逐字引用）

> 来源：`docs/design/assets/use-cases.md`（L1 权威功能契约）；机制引用 `docs/design/assets/depreciation-and-posting.md` §三 + `docs/design/assets/state-machine.md` §3（L2 设计参考，冲突以 L1 为准）。

### UC-AST-04 资产报废处置 — `use-cases.md:65-80`

**场景**：资产报废,结转原值/累计折旧,记录清理损失。

**可验证断言**（见 depreciation-and-posting.md §三、state-machine.md）：
```
IN_SERVICE → SCRAPPED(报废)
生成处置凭证:
  借 累计折旧(结转), 固定资产清理
  贷 固定资产(原值结转)
清理损失 = 账面净值 - 处置收入(报废收入常为0)
损失计入营业外支出
卡片状态终态(不可恢复)
```

**涉及机制**：depreciation-and-posting.md §三、state-machine.md §3

### UC-AST-05 资产出售处置 — `use-cases.md:84-97`

**场景**：资产出售,计算清理收益/损失。

**可验证断言**（见 depreciation-and-posting.md §三）：
```
IN_SERVICE → SOLD(出售)
清理损益 = 处置收入 - 账面净值
若 > 0: 收益(贷 营业外收入)
若 < 0: 损失(借 营业外支出)
先补提当期折旧至出售日(确保累计折旧准确)
```

**涉及机制**：depreciation-and-posting.md §三

> **L2 owner doc 锚点**（设计参考，非真相源；冲突以 L1 为准）：
> - `depreciation-and-posting.md §三 资产处置清理 :130-176`：§3.1 处置类型表（:134-137）描述**含"固定资产清理"中间科目腿**的两步流——「报废: 借：累计折旧 / 借：固定资产清理 / 贷：固定资产 → 借：营业外支出 / 贷：固定资产清理」+「出售: 借：累计折旧 / 借：银行存款 / 借：固定资产清理（亏损） / 贷：固定资产 / 贷：固定资产清理（收益）」；§3.2 处置流程（:139-164）+ §3.3 清理损益计算（:166-176，账面净值=原值−累计折旧）。
> - `depreciation-and-posting.md §十 实现约定 :339-348`：**`:346` 业务类型码段**——「§一/§7.1 旧表仍列 DISPOSAL_SCRAP/DISPOSAL_SALE 等业务类型——实际 ErpFinBusinessType 含单一 DISPOSAL(90)（报废/出售的科目分解差异由 DisposalAcctDocProvider 按 disposalType=SCRAPPED/SOLD 内部分支处理，不拆业务类型常量）」。**§十 Deferred 标注仅覆盖类型命名（DISPOSAL_SCRAP/SALE→DISPOSAL），未覆盖"固定资产清理"科目腿合并**——§一:19-20 旧表实际匹配实现（无固定资产清理腿），而 §三:136-137 描述含固定资产清理腿，owner doc 内部不一致。
> - `depreciation-and-posting.md §一 :14-21`：折旧业务类型注册旧表（DISPOSAL_SCRAP/DISPOSAL_SALE），科目方向为"借：累计折旧 / 借：清理损失 / 贷：固定资产"（报废）+ "借：累计折旧 / 借：银行存款 / 贷：固定资产 / 贷：清理收益"（出售），**无"固定资产清理"中间科目腿**——与实现一致，与 §三:136-137 冲突。
> - `state-machine.md §3 终态与恢复 :48-53`：终态 SCRAPPED/SOLD 不可恢复；处置错误经"处置冲销"（反向清理凭证）+ 重新处置。
> - `state-machine.md §实现约定 reverseApprove posted=false 窗口不对称 :68-70`：Capitalization/Disposal reverseApprove 仅 posted=true 时回滚资产行为；posted=false 窗口期 reverseApprove 仅设业务单据 REJECTED，资产保持终态——deliberate 不对称（Phase 3 方案 B），DeferredPostingSweepJob 兜底重试 + 告警闭环。
> - `state-machine.md §已知限制 浏览器层 xwf 审批路径 :186-195`：ErpAstDisposal useWorkflow=true xwf 审批轴浏览器层不可达；DIRECT 三轴审批浏览器层可达。

---

## 2. 实现代码路径（L3 含行号 + 跨域调用链）

> 实仓源：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/`。跨域 Facade：`IErpFinVoucherBiz`（post/reverse 经 `AssetPostingExecutor`）/ `IErpSysNotificationBiz`（失败告警）。**处置路径无 daoFor(ErpFin*) 直写**（A2.10+A4.3 确认；P1-MA4-015 永久豁免）。

### UC-AST-04 资产报废处置 / UC-AST-05 资产出售处置（共享处置入口，disposalType 分支）

| 组件 | 文件:line | 职责 |
|------|----------|------|
| 处置 BizModel（薄壳） | `entity/ErpAstDisposalBizModel.java:17-27`（CrudBizModel 桩，零业务逻辑；xbiz `ErpAstDisposal.xbiz:23-31` approve 注入 Processor）| Facade 入口，委托 per-mutation Processor（R6.3 拆分）|
| 处置审批入口（Facade per-mutation Processor） | `processor/ErpAstDisposalApproveProcessor.java:22-31`（approve 编排：requireDisposal `:23` → idempotency `:24-26` → validateNotCancelled `:27` → validateTransitionForApprove `:28` → validateForApproval `:29` → executeApprove `:30`）| R5.4 Pattern B 单一真相源 |
| 处置审批域逻辑（Facade 共享 helper） | `processor/ErpAstDisposalProcessor.java:62-101`（`executeApprove` 编排：validateAssetDisposable `:64` → 损益计算 `:66-70` → 状态迁移 `:72-77` → saveOrUpdateEntity `:77` → cancelPendingSchedules `:79` → setApproveStatus APPROVED `:82` → flushSession `:87` → tryPost `:90` → posted 回写 `:94-98`）| 处置完整编排（UC-AST-04/05 共用）|
| **状态迁移（UC-AST-04/05 断言①，已实现）** | `ErpAstDisposalProcessor:72-77`（`disposalType==SOLD → ASSET_STATUS_SOLD，否则 ASSET_STATUS_SCRAPPED`；常量 `ErpAstConstants.java:65-69,97-98`）| IN_SERVICE → SCRAPPED/SOLD 终态迁移 |
| **清理损益计算（UC-AST-04/05，已实现）** | `ErpAstDisposalProcessor:66-70`（`original=asset.originalValue` + `accumDep=asset.accumulatedDepreciation` + `nbv=original−accumDep` + `disposalAmount=disposal.disposalAmount` + `gainLoss=disposalAmount−nbv`）+ `DisposalPostingDispatcher.buildEvent:110-114`（同式重算入 BILL_DATA_GAIN_LOSS）| 清理损益 = 处置收入 − 账面净值 |
| **终态不可恢复守卫（UC-AST-04 断言⑤，已实现）** | `ErpAstDisposalProcessor.validateAssetDisposable:184-198`（`status==SCRAPPED||SOLD → ERR_DISPOSAL_ASSET_ALREADY_DISPOSED :187-191`；非 IN_SERVICE/IDLE → `ERR_DISPOSAL_ASSET_NOT_DISPOSABLE :192-197`）| 终态守卫（同见于 Inventory `ErpAstInventoryProcessor:264-270`、Maintenance `:137-138`、ValueAdjustment `:189-190`）|
| 折旧计划联动（取消未来计划） | `ErpAstDisposalProcessor.cancelPendingSchedules:202-211`（查 PENDING 计划 → setStatus CANCELLED）+ `restoreCancelledSchedules:213-222`（reverse 时 CANCELLED→PENDING）| 仅取消**未来**计划，从不跑当期折旧 |
| **处置凭证过账派发** | `posting/DisposalPostingDispatcher.java:51-64`（`tryPost`：buildEvent `:52` → executor.postEvent `:54` → catch 吞异常返回 null 保持 posted=false `:55-63` + dispatchFailureAlert `:61`）+ `buildEvent:98-131`（businessType=`ErpFinBusinessType.DISPOSAL`=90 `:100` + billHeadCode=disposal.code `:101` + voucherDate=businessDate `:106-108` + 损益重算 `:110-114` + 科目解析 `:123-128`）| DISPOSAL 业财过账事件组装 + 幂等键 + 失败告警 |
| **处置凭证科目分解（缺口 #2 — 合并科目，无"固定资产清理"中间腿）** | `posting/DisposalAcctDocProvider.java:29-86`（supports DISPOSAL `:50-52`；createFacts `:55-86`：**借 累计折旧 1602** `:69-72` / **借 银行存款 1002（disposalAmount>0）** `:74-76` / **损益分支**：gainLoss>0 **贷 营业外收入 6301** `:78-79` / gainLoss<0 **借 营业外支出 6711 gainLoss.negate()** `:80-82` / **贷 固定资产原值 1601** `:83-85`）| **合并为单凭证，无"固定资产清理"(1606) 中间科目腿**——直接将损益计入 6711/6301，与 L1:72-74 + §三:136-137 含"固定资产清理"腿冲突，与 §一:19-20 一致 |
| 跨域过账 Facade | `posting/AssetPostingExecutor`（REQUIRES_NEW → `IErpFinVoucherBiz` post/reverse）| 跨域失败隔离 + 凭证持久化 |
| **失败告警派发（#4 P1-MA2-060 HEAD 复核）** | `DisposalPostingDispatcher.dispatchFailureAlert:67-83`（tryPost 失败时派发 `IErpSysNotificationBiz.notify("ast.disposal-posting-failure", ctx, serviceCtx)` `:78`，event key `ast.disposal-posting-failure` `:45`；通知失败降级 warn 不阻断 `:79-82`）| 失败告警闭环（R1.16 resolved P1-MA4-013/060 告警部分）|
| reverseApprove（posted=true 回滚资产） | `ErpAstDisposalProcessor.executeReverseApprove:111-129`（**gated by posted==true** `:112`：postingDispatcher.reverse `:113` + asset→IN_SERVICE `:116` + restoreCancelledSchedules `:119` + posted=false/gainLoss=null `:121-124`；posted=false 窗口仅设 REJECTED `:126`）| reverseApprove 不对称（deliberate Phase 3 方案 B，state-machine.md §实现约定:68-70）|
| **出售补提折旧（UC-AST-05 断言⑤，缺口 #1 — 完全缺失）** | **零生产匹配**——grep `catchUp|补提|preDisposal|beforeDisposal|depreciateTo|saleDate` 于 `ErpAstDisposalProcessor`/`ErpAstDisposalApproveProcessor`/`DisposalPostingDispatcher`/`DisposalAcctDocProvider` 处置 4 文件 → **0 命中**（仅 `ErpAstErrors.java:96` 拒绝消息串"不允许补提折旧"）。`executeApprove:62-101` 全体：validate → setStatus → cancelPendingSchedules（仅取消**未来**计划，从不跑当期）→ setApproveStatus → tryPost。**从不调用 `IErpAstDepreciationScheduleBiz.executeDepreciation(...)`**。`buildEvent:110-114` 读 `asset.getAccumulatedDepreciation()`（截至上期末账面值），非重算至出售日值 | **UC-AST-05"先补提当期折旧至出售日"完全未实现**——月中处置且上月末折旧则累计折旧低估 → 净值高估 → 清理损益误算 → GL 6711/6301 错报 |

---

## 3. 测试断言证据（L4 注明断言强度）

> 测试源：`module-assets/erp-ast-service/src/test/java/app/erp/ast/service/`。强度评级对齐 MA5（A5.4 assets 测试覆盖）+ A4.3 resolved R2.12。

### UC-AST-04 资产报废处置

| 测试文件#方法 | 覆盖断言 | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpAstDisposal#testScrapLossAndTerminalStatus` | 报废损失 + 终态 + 未来计划取消 + DISPOSAL 凭证回链 | **强** | gainLoss=−12000 + posted=true + APPROVED + 2 未来计划 CANCELLED + DISPOSAL bill-r 回链非空 |
| `TestErpAstDisposalWorkflowApproval#testSubmitAgreeThenApproved` | xwf 经理审批→cc-assets→approve→APPROVED+posted+SCRAPPED | **强** | 审批链完整 + 状态终态 + 过账闭环 |
| `TestErpAstPostingReverse#testDisposalReverseApproveRestoresAsset` | posted=true reverseApprove 回滚资产 | **强** | reverseApprove → posted=false+REJECTED+asset→IN_SERVICE+计划→PENDING+DISPOSAL 凭证 isReversed |
| `TestErpAstPostingReverse#testEndToEndCapitalizationDepreciationDisposal` | e2e cap→2×折旧→处置 | **强** | gainLoss=−10000+posted+SCRAPPED 全链 |
| `TestErpAstPostingReverse#testCapitalizationSuspendedPostedFalseHanging` | posted=false 悬挂 | **强** | P1-MA2-060 证据（posted=false 窗口 reverseApprove 不回滚资产）|
| `TestErpAstAcctDocProviderAccountKey#testDisposal:43-54` | DISPOSAL 4 facts 科目键 | **强** | assertKeys 4 facts（累计折旧/银行存款/营业外支出/固定资产）— **无"固定资产清理"(1606) 键**（#2 证据）|
| **处置凭证行级科目断言（缺口 #2）** | "固定资产清理"中间科目腿 | **零断言** | `TestErpAstAcctDocProviderAccountKey#testDisposal` 断言 4 facts（1602/1002/6711/1601），**不断言 1606 固定资产清理腿**（与实现同步偏离 L1:72-74）|

### UC-AST-05 资产出售处置

| 测试文件#方法 | 覆盖断言 | 断言强度 | 证据摘要 |
|--------------|---------|---------|---------|
| `TestErpAstDisposal#testSaleGainAndBankCredit` | 出售收益 + SOLD + 银行存款贷 | **强** | original=12000/accumDep=4000/NBV=8000/disposalAmount=10000 → gainLoss=+2000 + SOLD + bill-r |
| `TestErpAstDisposalWorkflowApproval#testDisagreeThenRejected` / `#testResubmitAfterRejected` | 审批拒绝 + 重新提交 | **强** | reject/resubmit 状态机守卫 |
| **出售补提折旧（缺口 #1）** | 出售前补提当期折旧至出售日 | **零测试**（功能缺失导致无可测路径）| grep `catchUp|补提|preDisposal|depreciateTo|saleDate` 于处置测试代码 → **0 命中**；`testSaleGainAndBankCredit` 的 accumDep=4000 为**手动 seed 的上期末值**，非出售日补提后的准确值 |

### E2E 覆盖

| spec | 覆盖 | 断言强度 | 证据摘要 |
|------|------|---------|---------|
| `reports/ast-disposal.smoke.spec.ts` | GraphQL 200 冒烟 | **弱/冒烟**（P1-MA5-012）| 仅断言 GraphQL 200 + DOM 渲染，无业务断言 |
| `reports/ast-disposal.value.spec.ts:1-16` | 报表标题 token | **仅结构 token**（缺口 #3）| E2E seed DB **无处置行**（`_vfs/_init-data/` 缺 erp_ast_disposal.csv），无数值断言，仅断言报表标题"资产处置明细表"token；已登记 successor 待 E2E seed |
| **浏览器层 E2E 覆盖 UC-AST-04/05 直接 approve 流** | DIRECT 三轴 approve | **无 spec** | xwf browser-layer limit（state-machine.md:186-195）；DIRECT 三轴可达但无 spec 覆盖 |

**测试缺口汇总**：
- UC-AST-05 出售补提折旧路径（零测试，功能缺失 → 下游 reuse P1-RC-029）
- DISPOSAL 凭证行级"固定资产清理"(1606) 科目断言（仅 findBillLinks 存在性，非行级科目断言 → 下游 P1-RC-030 新建）
- posted=false 窗口处置资产留 SCRAPPED+reverseApprove 不恢复的不对称（仅覆盖 posted=true reverse，已由 P1-MA2-060 resolved R1.16 处置）
- 浏览器层 E2E（seed 缺处置行 → 下游 P2-RC-027 新建）

---

## 4. 运行时行为证据（L5 — 复用 MA2 + 单测）

### UC-AST-04 资产报废处置

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| IN_SERVICE → SCRAPPED 状态迁移 | `ErpAstDisposalProcessor:72-77`（disposalType≠SOLD → SCRAPPED）+ `testScrapLossAndTerminalStatus` SCRAPPED 强断言 | **行为已证实**（A2.10 §场景(c) PASS `:224-230`）|
| 处置凭证（合并科目，无固定资产清理腿）| `DisposalAcctDocProvider:69-85`（借 1602/借 1002/损益分支 6711\|6301/贷 1601）+ `TestErpAstAcctDocProviderAccountKey#testDisposal` 4 facts 强断言 | **行为已证实（合并科目实现）**——凭证借贷平衡 + GL 正确，但**无"固定资产清理"(1606) 中间科目腿**（与 L1:72-74 + §三:136-137 冲突 → #2 doc drift）|
| 清理损失 = 账面净值 − 处置收入 → 营业外支出 | `ErpAstDisposalProcessor:66-70`（nbv=original−accumDep，gainLoss=disposalAmount−nbv）+ `:80-82`（gainLoss<0 借 6711 gainLoss.negate()）+ `testScrapLossAndTerminalStatus` gainLoss=−12000 强断言 | **行为已证实**（A2.10 §场景(c) PASS）|
| 终态不可恢复守卫 | `validateAssetDisposable:184-198`（SCRAPPED/SOLD → ERR_DISPOSAL_ASSET_ALREADY_DISPOSED）+ A2.10 §维度 2 PASS（grep 全 src/main 无任何 setStatus 从 SCRAPPED/SOLD 迁出）| **行为已证实**（A2.10 §维度 2/3 PASS `:138,146`）|
| posted=false 窗口 reverseApprove 不回滚资产（#4 HEAD 复核）| `executeReverseApprove:111-129`（gated by posted==true `:112`）+ `testCapitalizationSuspendedPostedFalseHanging` 强断言 + `DisposalPostingDispatcher.dispatchFailureAlert:67-83` 告警派发 | **行为已证实（deliberate 不对称）**——posted=false 窗口 reverseApprove 仅设 REJECTED，资产保持 SCRAPPED；**告警派发已落地**（R1.16 resolved P1-MA2-060/P1-MA4-013 告警部分）+ DeferredPostingSweepJob 兜底重试 |

### UC-AST-05 资产出售处置

| 行为 | 证据来源 | 结论 |
|------|---------|------|
| IN_SERVICE → SOLD 状态迁移 | `ErpAstDisposalProcessor:72-77`（disposalType==SOLD → SOLD）+ `testSaleGainAndBankCredit` SOLD 强断言 | **行为已证实**（A2.10 §场景(d) PASS `:232-234`）|
| 清理损益分支（>0 贷营业外收入 / <0 借营业外支出）| `DisposalAcctDocProvider:78-82`（gainLoss>0 贷 6301 / gainLoss<0 借 6711 negate）+ `testSaleGainAndBankCredit` gainLoss=+2000 强断言 | **行为已证实**（A2.10 §场景(d) PASS）|
| **先补提当期折旧至出售日（缺口 #1 — 完全缺失）** | grep `catchUp|补提|preDisposal|depreciateTo|saleDate` 于处置 4 文件 → **0 生产匹配**；`executeApprove:62-101` 从不调 `IErpAstDepreciationScheduleBiz.executeDepreciation`；`buildEvent:110-114` 读 `asset.getAccumulatedDepreciation()`（陈旧上期末值）；`cancelPendingSchedules:202-211` 仅取消 PENDING 未来计划从不跑当期 | **行为缺失（能力层）**——UC-AST-05"先补提当期折旧至出售日（确保累计折旧准确）"完全未实现；月中处置（如 15 日出售）且上月末折旧则累计折旧低估半月折旧 → 净值高估 → gainLoss 误算（收益高估或损失低估）→ GL 6301/6711 错报 |

---

## 5. 符合性结论（五级追踪矩阵 + 每 UC 结论）

### 5.1 五级追踪矩阵（2 UC × 5 列，逐 UC 一行）

| UC | L1 use-case 需求契约 | L2 owner doc 契约 | L3 代码路径 | L4 测试断言 | L5 运行时行为 | 符合性结论 |
|----|---------------------|------------------|------------|------------|--------------|-----------|
| **UC-AST-04** 资产报废处置 | `use-cases.md:65-80`（5 条断言：①IN_SERVICE→SCRAPPED / ②生成处置凭证[借 累计折旧, 固定资产清理 / 贷 固定资产] / ③清理损失=账面净值−处置收入[报废收入常为 0] / ④损失计入营业外支出 / ⑤卡片状态终态[不可恢复]）— 验收标准原文见 §1 | `depreciation-and-posting.md §三 :130-176`（§3.1:134-137 处置类型表描述**含"固定资产清理"中间科目腿**两步流 + §3.3 清理损益计算）+ `§一:19-20`（旧表**无固定资产清理腿**，与实现一致）+ `§十:346`（Deferred **仅覆盖类型命名**DISPOSAL_SCRAP/SALE→DISPOSAL，未覆盖科目腿合并）+ `state-machine.md §3:48-53`（终态不可恢复）— **owner doc 内部不一致：§三含固定资产清理腿 vs §一/实现无；§十 Deferred 未覆盖科目腿合并** | 状态迁移：`ErpAstDisposalProcessor:72-77`；损益计算：`:66-70`；终态守卫：`validateAssetDisposable:184-198`；凭证科目分解：`DisposalAcctDocProvider:69-85`（**无 1606 固定资产清理腿**，仅 1602/1002/6711\|6301/1601）；reverseApprove：`executeReverseApprove:111-129`（gated posted==true）；跨域：`AssetPostingExecutor`→`IErpFinVoucherBiz` | `testScrapLossAndTerminalStatus`（**强**：gainLoss=−12000+SCRAPPED+posted+2 计划 CANCELLED）+ `testDisposalReverseApproveRestoresAsset`（**强**：posted=true reverse 回滚）+ `testEndToEndCapitalizationDepreciationDisposal`（**强**：e2e）+ `TestErpAstAcctDocProviderAccountKey#testDisposal`（**强**：4 facts，**无 1606**）| 状态迁移 → A2.10 §场景(c) PASS；损益 → 单测强覆盖；终态 → A2.10 §维度 2/3 PASS；凭证 → 合并科目实现（无固定资产清理腿）；reverseApprove 不对称 → deliberate（R1.16 resolved 告警+sweep）| **接受 on ①③④⑤**（状态迁移+清理损失+营业外支出+终态守卫行为正确，测试强）+ **P1 on ② 处置凭证合并科目腿**（**新建 P1-RC-030**——L1:72-74 + §三:136-137 要求含"固定资产清理"中间科目腿，实现合并为单凭证无此腿；§4 三判据复核均不满足[§十 Deferred 仅覆盖类型命名]→ 非 documented simplification → §2 P1① 行为实质偏离验收标准；**注：GL 借贷平衡不破坏，仅凭证结构/审计轨迹偏离**）|
| **UC-AST-05** 资产出售处置 | `use-cases.md:84-97`（5 条断言：①IN_SERVICE→SOLD / ②清理损益=处置收入−账面净值 / ③若>0 收益贷营业外收入 / ④若<0 损失借营业外支出 / **⑤先补提当期折旧至出售日[确保累计折旧准确]**）— 验收标准原文见 §1 | `depreciation-and-posting.md §三 :130-176`（§3.1:134-137 处置类型表 + §3.3:166-176 清理损益计算）— **§三未提"出售前补提折旧"**（L1:94 明确要求"先补提"vs owner doc §三 drift 非裁剪，按 §4 Q1 以 L1 为准）+ `state-machine.md §3`（终态）| 状态迁移：`ErpAstDisposalProcessor:72-77`（SOLD）；损益分支：`DisposalAcctDocProvider:78-82`（>0 贷 6301/<0 借 6711）；**补提折旧：零生产匹配**（`executeApprove:62-101` 从不调 executeDepreciation + grep catchUp/补提/depreciateTo 0 命中 + buildEvent 读陈旧 accumulatedDepreciation）| `testSaleGainAndBankCredit`（**强**：gainLoss=+2000+SOLD+bill-r，**但 accumDep=4000 为手动 seed 上期末值非补提后准确值**）+ 审批流 2 方法（**强**）| 状态迁移 → A2.10 §场景(d) PASS；损益分支 → 单测强覆盖；**补提折旧 → 行为缺失**（从不调 executeDepreciation）| **接受 on ①②③④**（状态迁移+损益公式+收益/损失分支行为正确，测试强）+ **P1 on ⑤ 出售前补提折旧完全缺失**（**reuse P1-RC-029**——A1.22 UC-AST-07 方式B 补提能力缺失的下游投影；同根因[补提 mutation catchUpDepreciation 不存在]同控制点[处置路径无从调用]；§2 P1① 功能完全缺失 + §5 Q4 会计正确性类无例外：累计折旧低估→净值高估→清理损益误算→GL 6301/6711 错报）|

### 5.2 候选缺口分级汇总（4 项）

| 缺口# | UC | 描述 | 分级 | 命中判据 | finding ID |
|-------|----|------|------|---------|-----------|
| #1 | UC-AST-05 ⑤ | **出售前补提当期折旧至出售日完全缺失**（grep catchUp/补提/depreciateTo/saleDate 于处置 4 文件 0 生产匹配；`executeApprove:62-101` 从不调 executeDepreciation；`buildEvent:110-114` 读陈旧 accumulatedDepreciation；`cancelPendingSchedules` 仅取消未来计划从不跑当期；月中处置累计折旧低估→净值高估→清理损益误算→GL 错报）| **P1**（reuse）| §2 P1①（功能完全缺失）+ §5 Q4（会计正确性类——累计折旧低估→净值高估→清理损益误算→GL 6301/6711 错报，无例外）| **reuse P1-RC-029**（A1.22 UC-AST-07 方式B 补提能力缺失的下游投影——补提 mutation 不存在故处置路径无从调用）|
| #2 | UC-AST-04 ② | **处置凭证合并"固定资产清理"中间科目腿**（L1:72-74 + §三:136-137 要求含"固定资产清理"(1606) 两步流 vs 实现/§一:19-20 无；`DisposalAcctDocProvider:69-85` 仅 1602/1002/6711\|6301/1601 四类科目，无 1606；§十:346 Deferred 仅覆盖类型命名[DISPOSAL_SCRAP/SALE→DISPOSAL]未覆盖科目腿合并）| **P1**（须 §4 三判据复核→不满足→重开）| §2 P1①（行为实质偏离验收标准——L1:72-74 显式列出"固定资产清理"借腿，实现完全缺失该腿）+ §4 三判据复核均不满足（§十 Deferred 未覆盖科目腿合并）| **新建 P1-RC-030** |
| #3 | UC-AST-04/05 | **DISPOSAL 无浏览器层 E2E**（E2E seed DB `_vfs/_init-data/` 缺 erp_ast_disposal.csv；`ast-disposal.value.spec.ts` 仅结构 token 无数值断言；`ast-disposal.smoke.spec.ts` 仅 GraphQL 200 冒烟 P1-MA5-012；无浏览器层 DIRECT approve 流 spec）| **P2**（successor）| §2 P2①（次要验收标准[E2E 行级断言]未完全满足，主路径[JUnit 单测强覆盖]OK 边界[E2E seed 缺]弱）| **新建 P2-RC-027** |
| #4 | UC-AST-04/05 | **P1-MA2-060 resolved 状态 HEAD 复核**（posted=false 窗口 reverseApprove 是否回滚资产 + 失败告警派发是否生效）| **HEAD 复核结论（不新建 finding）**| 复用 MA2 行为证据（§去重协议）| **不新建**（reuse P1-MA2-060 resolved R1.16，详见 §6.1 + §6.3）|

### 5.3 每 UC 总结论

- **UC-AST-04 资产报废处置**：
  - **接受 on ①③④⑤**（§2 接受——状态迁移 IN_SERVICE→SCRAPPED + 清理损失=净值−收入 + 损失计入营业外支出 + 终态不可恢复守卫，行为正确，A2.10 §场景(c) PASS + 单测强覆盖含 e2e cap→折旧→处置链）
  - **P1 on ② 处置凭证合并科目腿**（**新建 P1-RC-030**，§2 P1① + §4 三判据复核——L1:72-74 + §三:136-137 要求含"固定资产清理"(1606) 中间科目腿，实现 `DisposalAcctDocProvider:69-85` 合并为单凭证无此腿；§十:346 Deferred 仅覆盖类型命名（DISPOSAL_SCRAP/SALE→DISPOSAL）未覆盖科目腿合并 → §4 (ii) 不满足；§4 (i) 无独立 plan-audit 裁决科目腿合并；§4 (iii) product-scope 未裁剪 → **三判据均不满足 → 非 documented simplification → 重开 P1**；**注：GL 借贷平衡不破坏（Dr Σ = Cr Σ），仅凭证结构/审计轨迹偏离 L1 字面**——倾向 P1 但 MR1 修复时若人工批准可作为 documented simplification 合法化则降 P2，修复触及会计过账逻辑[DisposalAcctDocProvider/VoucherFact 核心路径]须 ask-first + 独立 plan-audit §5）

- **UC-AST-05 资产出售处置**：
  - **接受 on ①②③④**（§2 接受——状态迁移 IN_SERVICE→SOLD + 清理损益=收入−净值 + 收益贷营业外收入/损失借营业外支出分支，行为正确，A2.10 §场景(d) PASS + 单测强覆盖）
  - **P1 on ⑤ 出售前补提折旧完全缺失**（**reuse P1-RC-029**，§2 P1① + §5 Q4 会计正确性类无例外——L1:94 逐字"先补提当期折旧至出售日（确保累计折旧准确）"，实现 `executeApprove:62-101` 从不调 `executeDepreciation` + grep catchUp/补提 0 生产匹配；owner doc §三未提补提属 drift 非裁剪，按 §4 Q1 以 L1 为准；此为 A1.22 UC-AST-07 方式B 补提能力缺失[P1-RC-029]的**下游投影**——补提 mutation 不存在故处置路径无从调用；MR1 修复行应与 A1.22 P1-RC-029 补提能力修复**协同**：catchUpDepreciation mutation 实现后，`ErpAstDisposalProcessor.executeApprove` 须在损益计算前调用它重算 accumulatedDepreciation 至出售日）

---

## 6. 与 arm-index 衔接（复用 or 新增 裁决）

### 6.1 arm-index grep 比对 + 复用 or 新增裁决（§7 规则）

> 对每条候选缺口 grep arm-index assets 处置同域同控制点后裁决（禁止未经比对新建）。**RC 系列对 assets 已有 A1.22（P1-RC-029/P2-RC-025/P2-RC-026）**——本切片为 assets 域第二个 RC 切片。

| 缺口# | grep 关键词 | 既有 finding | 裁决 |
|-------|------------|-------------|------|
| #1 | 「补提」「catchUp」「backfill」「当期补提」「出售补提」「UC-AST-05」「先补提」「depreciateTo」 | **P1-RC-029**（A1.22 UC-AST-07 方式B 当期一次性补提缺失）——**同根因**（补提 mutation catchUpDepreciation 不存在）+ **下游投影**（UC-AST-05 处置路径即便想调补提也无从调用）| **reuse P1-RC-029**（§7 同根因同控制点 → 复用不新建；MR1 修复行须协同：catchUpDepreciation mutation 实现后处置 executeApprove 须调用它。列明差异：A1.22 P1-RC-029 覆盖 UC-AST-07 补提能力不存在[能力层]，本切片覆盖 UC-AST-05 处置路径未调用[调用层]——同一根因的两个投影面，修复 catchUpDepreciation mutation + 处置路径接线一并解决）。既有 P1-MA4-013（折旧 dispatcher 悬挂 resolved R1.16）/ P1-MA2-089（并发双计 resolved R1.28）/ P1-MA5-011（测试空洞归并）均不同根因/不同控制点/不同维度。|
| #2 | 「固定资产清理」「1606」「disposal 凭证」「科目腿」「合并科目」「disposal voucher structure」「DISPOSAL_SCRAP」 | **assets 处置凭证科目结构零命中**——既有 finding 覆盖：P2-MA4-006（dispatcher copy-paste watch-only，不同维度）/ §十:346 Deferred（本切片复核 §4 三判据——仅覆盖类型命名不覆盖科目腿合并）/ P1-MA1-022（跨域只读 daoFor，不同维度）| **新建 P1-RC-030**（assets 域首个处置凭证科目结构 finding；§4 三判据复核均不满足 → 非 documented simplification → 重开 P1；列明：owner doc 内部不一致[§三含固定资产清理腿 vs §一/实现无] + §十 Deferred 仅覆盖类型命名 + GL 平衡不破坏仅凭证结构偏离）|
| #3 | 「E2E seed」「disposal E2E」「浏览器层」「ast-disposal」「DIRECT approve」 | **P1-MA5-012**（dashboard/disposal smoke → R3.2 successor，`:642`）——**同控制点**（E2E 覆盖弱）但不同维度（MA5 测试覆盖 successor vs RC 需求契约视角）| **新建 P2-RC-027**（RC 视角新 finding——E2E seed 缺处置行 + 无浏览器层 DIRECT approve spec；与 P1-MA5-012 互补不重复：P1-MA5-012 是 MA5 测试有效性维度 successor，本 finding 是 RC 需求契约维度 E2E 行级断言缺口）|
| #4 | 「P1-MA2-060」「reverseApprove」「posted=false」「悬挂」「不对称」「告警」 | **P1-MA2-060**（Cap/Disposal tryPost 吞异常悬挂 + reverseApprove 仅 posted=true 回滚资产致资产侧状态悬挂，**resolved R1.16**）——**同 finding HEAD 复核** | **不新建**（reuse P1-MA2-060 resolved R1.16——HEAD 复核结论见 §6.3：(a) reverseApprove 不对称 deliberate Phase 3 方案 B 仍存在[state-machine.md §实现约定:68-70 显式 documented simplification]；(b) 失败告警派发 `DisposalPostingDispatcher.dispatchFailureAlert:67-83` 已落地[R1.16 resolved 告警部分]；(c) DeferredPostingSweepJob 兜底重试。RC 视角复核：reverseApprove 不对称是 operational edge case 非 L1 验收标准直接覆盖[UC-AST-04/05 未提 reverseApprove]，且 §4 (i)/(ii) 满足[R1.16 plan-audit + owner doc Deferred 标注] → 维持 resolved 不重开）|

### 6.2 双向可追溯（finding ↔ 修复行预留 MR0/MR1）

| Finding ID | 域 | UC | 分级 | 目标 MR | 触及保护区域 | 修复状态 |
|-----------|---|----|------|--------|------------|---------|
| `P1-RC-029`（reuse，A1.22 建立覆盖 UC-AST-07 + 本切片追加 UC-AST-05 投影注记）| assets | UC-AST-07 方式B + **UC-AST-05 ⑤ 出售补提**（本切片投影）| P1（须人工确认范围裁剪）| MR1（R1.0 → RC-R1.n）/ §4 (iii) product-scope 修订（若人工确认裁剪）| **是——ORM 结构变更 + 会计过账逻辑**（方式B 实现 = `ErpAstDepreciationScheduleBizModel` 增 `catchUpDepreciation` mutation + 处置 `ErpAstDisposalProcessor.executeApprove` 在损益计算前调用它重算 accumulatedDepreciation 至出售/报废日 + 可能新增 `ErpAstDepreciationSchedule.isCatchUp` 列；**触及 ORM 结构变更 + 会计过账逻辑须 ask-first + 独立 plan-audit §5**）| todo（本审计仅登记，不实施修复；**先须人工确认 product-scope 是否裁剪方式B**：若裁剪 → §4 (iii) 改真相源非降级；若未裁剪 → P1 强制实现 Q4 无例外。**本切片追加处置路径接线义务**：catchUpDepreciation mutation 实现后，`executeApprove:62-101` 须在 `:66-70` 损益计算前调用它，将 `asset.accumulatedDepreciation` 重算至 disposal.businessDate 对应的当期折旧）|
| `P1-RC-030`（新建）| assets | UC-AST-04 ② 处置凭证科目结构 | P1（§4 三判据不满足→重开；GL 平衡不破坏仅凭证结构偏离）| MR1（R1.0 → RC-R1.n）| **是——会计过账逻辑**（修复 = `DisposalAcctDocProvider.createFacts` 拆分为两步流：Step1 结转原值+累计折旧至"固定资产清理"(1606) + Step2 处置收入入清理 + 损益从清理结转至 6711/6301；**触及会计过账逻辑[DisposalAcctDocProvider/VoucherFact 核心路径]须 ask-first + 独立 plan-audit §5**；或经 §4 (ii) 人工批准将科目腿合并登记为 documented simplification 则降 P2）| todo（本审计仅登记，不实施修复；**MR1 修复时人工裁决**：方案 A 实现 1606 中间科目腿两步流 / 方案 B 经 §4 三判据人工批准将合并登记为 documented simplification 合法化）|
| `P2-RC-027`（新建）| assets | UC-AST-04/05 E2E | P2 | successor watch-only（P2 登记不强制）| 否（纯测试补充——补 E2E seed `erp_ast_disposal.csv` + 浏览器层 DIRECT approve 流 spec + ast-disposal.value.spec.ts 数值断言；**纯测试/E2E 补充，按 roadmap 预授权类目[测试补充]可自动执行，不触发 §5 ask-first**）| todo |

### 6.3 §4 三判据复核结论汇总

| 缺口# | §4 (i) plan-audit | §4 (ii) owner doc Deferred | §4 (iii) product-scope 裁剪 | 复核结论 |
|-------|-------------------|---------------------------|---------------------------|---------|
| #1 出售补提缺失（reuse P1-RC-029）| **无**（无独立 plan-audit 专门裁决补提裁剪）| **无**（owner doc §三未提补提 drift；§十 Deferred 清单不含补提；state-machine.md §4 描述方式A/B 为并列选项）| **无**（product-scope 未将补提列入范围裁剪）| **三判据均不满足 → 非 documented simplification**（A1.22 P1-RC-029 已裁决）→ 须按 Q4=(a) 评估：补提能力完全缺失 + 会计正确性类 → **P1 强制实现**（须人工确认是否裁剪，裁剪则 §4 (iii) 改真相源非降级）|
| #2 处置凭证合并科目腿（新建 P1-RC-030）| **无**（无独立 plan-audit 专门裁决科目腿合并）| **不满足**（§十:346 Deferred **仅覆盖类型命名**[DISPOSAL_SCRAP/SALE→DISPOSAL]，**未覆盖"固定资产清理"科目腿合并**；owner doc 内部不一致：§三:136-137 含固定资产清理腿 vs §一:19-20/实现无）| **无**（product-scope 未将科目腿合并列入范围裁剪）| **三判据均不满足 → 非 documented simplification** → 重开 P1（§2 P1① 行为实质偏离验收标准——L1:72-74 显式列出"固定资产清理"借腿）。**注：GL 借贷平衡不破坏[Dr Σ = Cr Σ]，仅凭证结构/审计轨迹偏离 L1 字面**；MR1 修复时若人工批准可作为 documented simplification 合法化则降 P2（§4 (ii) 路径）|
| #3 DISPOSAL 无浏览器层 E2E（新建 P2-RC-027）| N/A（P2 不强制 §4 复核）| N/A | N/A | P2 watch-only（主路径[JUnit 单测强覆盖]OK，边界[E2E seed 缺+浏览器层 spec 缺]弱）|
| #4 P1-MA2-060 resolved HEAD 复核（不新建）| **满足**（R1.16 plan-audit 存在）| **满足**（state-machine.md §实现约定:68-70 显式「deliberate 不对称 Phase 3 方案 B」Deferred 标注）| N/A | **§4 (i)/(ii) 满足 → 维持 resolved**（reverseApprove 不对称是 operational edge case，L1 UC-AST-04/05 未提 reverseApprove 验收标准；告警派发 `DisposalPostingDispatcher.dispatchFailureAlert:67-83` 已落地 R1.16 + DeferredPostingSweepJob 兜底）。RC 视角不重开（§去重协议——MA2 已裁决行为维度，RC 不重做）|

---

## 7. 静态存疑点清单（供 MA4 展开）

> L5 无法静态定论、需运行时确认的点。每存疑点一行；MA4（A4.1/A4.2 展开器）运行时验证后回填结论。

- **SP-1**（#1 出售补提缺失的运行时会计影响量化）：当前 `executeApprove:66-70` 读 `asset.getAccumulatedDepreciation()`（截至上期末账面值），若资产月中（如 15 日）出售且上月末已折旧，则累计折旧低估半月折旧额 → NBV 高估 → gainLoss 误算（收益高估或损失低估）。需运行时构造场景：资产 original=12000/residual=0/10 月寿命/直线法月折旧 100，2026-07-15 出售（disposalAmount=5000），若 2026-06 末 accumDep=600（6 个月），则当前实现 gainLoss=5000−(12000−600)=−6500；若补提至 7 月 15 日（半月 50），accumDep=650，gainLoss=5000−(12000−650)=−6350。偏差 = 150（半月折旧 50 × 3 倍杠杆？需运行时量化断言 GL 6301/6711 实际错报金额）。
- **SP-2**（#2 合并科目凭证在不同 disposalType 下的实际行级结构）：`DisposalAcctDocProvider:69-85` 在 SCRAPPED（收入 0）/SOLD（收入>0）/SOLD 负 gainLoss（亏损）/SOLD 正 gainLoss（收益）四种组合下的实际行级 facts 结构——需运行时构造 4 场景断言每行 subjectCode/dcDirection/amount，确认与 L1:72-74 + §三:136-137 含"固定资产清理"腿的字面偏离程度（是否所有组合均缺 1606）。
- **SP-3**（#4 posted=false 窗口 reverseApprove 实际行为）：构造处置过账失败（如缺 GL 科目）→ posted=false + 资产 SCRAPPED + disposal APPROVED → 触发 reverseApprove → 断言 disposal=REJECTED 但资产保持 SCRAPPED + schedules 保持 CANCELLED + 告警是否实际派发（`IErpSysNotificationBiz.notify("ast.disposal-posting-failure")`）+ DeferredPostingSweepJob 是否实际重试。验证 R1.16 resolved P1-MA2-060 的运行时落地。

**P0 即时通道**：本切片**未触发 P0**（最高级 = P1[#1 出售补提缺失 reuse P1-RC-029 + #2 凭证合并科目腿 P1-RC-030]，无活跃数据破坏/会计过账破坏/安全漏洞/核心循环断裂；#1 虽影响会计正确性——累计折旧低估→清理损益误算→GL 错报——但 GL 借贷平衡不破坏故非 §2 P0④[借贷仍平衡，仅金额错报] + 月中出售是中频场景 + 手动补提折旧后重算可达兜底 + 可经期末试算平衡人工发现；#2 GL 平衡完全不破坏仅凭证结构偏离）。

---

## 8. 过程纪律自检

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`，actual ≤ baseline（详见下表）。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter（退出码恒 0），真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码 0 作为门控通过依据。**本报告无生产代码变更**（纯审计报告 + arm-index 文档更新），checker 无回归风险。

  | 规则 | Baseline | Actual | 状态 |
  |------|----------|--------|------|
  | R1a/R1b/R1c | 0/0/0 | 0/0/0 | ✅ |
  | R1d | 14 | 14 | ✅ |
  | R2a/R2b/R2c/R2d | 34/229/1382/34 | 34/229/1382/34 | ✅ |
  | R3 | 5 | 5 | ✅ |
  | R4/R5 | 0/0 | 0/0 | ✅ |
  | R6 | 2 | 2 | ✅ |
  | R7 | 0 | 0 | ✅ |
  | R8 | 0 | 0 | ✅ |
  | R10 | 6 | 6 | ✅ |
  | R11 | 0 | 0 | ✅ |
  | R12a/R12b/R12c | 69/66/40 | 69/66/40 | ✅ |

  全 16 规则 actual ≤ baseline（精确匹配，0 漂移；与 A1.22 报告基线一致——本切片仅追加文档无生产代码变更）。
- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告全部 finding 已按 §7 规则 grep arm-index assets 处置同域同控制点后给出"复用 or 新增"裁决（详见 §6.1 比对表），无未经比对直接新建的 finding。#1 出售补提缺失 reuse P1-RC-029（A1.22 同根因下游投影）；#2 凭证合并科目腿新建 P1-RC-030（assets 域首个处置凭证科目结构 finding，§4 三判据复核均不满足）；#3 E2E 新建 P2-RC-027（与 P1-MA5-012 互补不同维度）；#4 reuse P1-MA2-060 resolved R1.16（HEAD 复核确认）。既有 P1-MA4-013/P1-MA2-089/P1-MA5-011/P1-MA2-060/P2-MA4-006 经 grep 确认不同根因/不同控制点/不同维度 → 新建/复用并列明差异依据。

---

## 落盘完整性自检（§6 9 段完整性）

报告产出 agent 在落盘前自查 9 段全部存在：

- [x] §1 需求契约原文（2 UC 验收标准逐字引用）
- [x] §2 实现代码路径（含行号 + 跨域调用链）
- [x] §3 测试断言证据（注明强度）
- [x] §4 运行时行为证据（复用 MA2 + 单测）
- [x] §5 符合性结论（2 UC × 5 列矩阵 + 候选缺口 4 项分级 + 每 UC 总结论 + §4 三判据复核汇总）
- [x] §6 与 arm-index 衔接（grep 比对 + 复用/新增裁决 + 双向可追溯 + §4 三判据复核汇总）
- [x] §7 静态存疑点清单（3 项 SP-1..SP-3 + P0 即时通道未触发声明）
- [x] §8 过程纪律自检（checker actual vs baseline 实测表 + closure-audit 独立性 + 交叉去重）
- [x] §9 与 MA2 报告差异增量声明（前置段 — 复用 A2.10/A4.3/A1.22 + 只补需求视角差异）

9 段齐全，落盘。

---

## 附录：A1.23 切片裁决摘要

- **新 P1（1 项）**：P1-RC-030（UC-AST-04 ② 处置凭证合并"固定资产清理"中间科目腿，§4 三判据均不满足→重开；GL 平衡不破坏仅凭证结构偏离 L1:72-74）
- **reuse P1（1 项）**：P1-RC-029（UC-AST-05 ⑤ 出售补提折旧缺失——A1.22 UC-AST-07 方式B 补提能力缺失的下游投影，同根因同控制点；MR1 修复须协同 catchUpDepreciation mutation + 处置路径接线）
- **新 P2（1 项）**：P2-RC-027（UC-AST-04/05 DISPOSAL 无浏览器层 E2E，watch-only successor——E2E seed 缺处置行 + 无 DIRECT approve spec）
- **不新建（1 项 HEAD 复核）**：P1-MA2-060 resolved R1.16 HEAD 复核确认（reverseApprove 不对称 deliberate Phase 3 方案 B + 告警派发已落地 + DeferredPostingSweepJob 兜底；§4 (i)/(ii) 满足 → 维持 resolved，RC 视角不重开——operational edge case 非 L1 验收标准直接覆盖）
- **P0 即时通道**：未触发（本切片无 P0——#1/#2 虽涉会计正确性但 GL 借贷平衡不破坏故非 §2 P0④，且月中出售是中频场景+手动补提可达兜底+#2 仅凭证结构偏离）
- **静态存疑点**：3 项（SP-1 出售补提缺失运行时会计影响量化 / SP-2 合并科目凭证 4 组合行级结构 / SP-3 posted=false 窗口 reverseApprove 实际行为+告警派发）交 MA4 A4.1/A4.2 运行时展开
- **§4 三判据复核结论**：#1 三判据均不满足 → 非 documented simplification → P1 强制实现（须人工确认范围裁剪）；#2 三判据均不满足[§十 Deferred 仅覆盖类型命名不覆盖科目腿合并]→ 非 documented simplification → 重开 P1（MR1 可经 §4 (ii) 人工批准降 P2）；#3 P2 watch-only；#4 P1-MA2-060 §4 (i)/(ii) 满足 → 维持 resolved
- **行为接受面**：UC-AST-04 ①③④⑤ 接受（状态迁移+清理损失+营业外支出+终态守卫，A2.10 §场景(c) PASS + 单测强覆盖含 e2e）；UC-AST-05 ①②③④ 接受（状态迁移+损益公式+收益/损失分支，A2.10 §场景(d) PASS + 单测强覆盖）
- **既有 finding 衔接**：P1-MA2-060（Cap/Disposal tryPost 吞咽+reverseApprove 不对称，resolved R1.16）/ P1-MA4-013（dispatcher 悬挂 resolved R1.16）/ P1-MA4-014（测试有效性 resolved R2.12）/ P1-MA4-015（跨域 daoFor resolved 永久豁免）/ P1-MA5-012（E2E smoke successor）/ P2-MA4-006（dispatcher copy-paste watch-only）/ P1-RC-029（A1.22 补提缺失，本切片 reuse）—— 除 P1-RC-029 reuse 外均不同根因/不同控制点/不同维度
