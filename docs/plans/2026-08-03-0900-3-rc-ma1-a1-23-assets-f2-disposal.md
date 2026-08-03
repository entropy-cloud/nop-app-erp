# 2026-08-03-0900-3 rc-ma1-a1-23-assets-f2-disposal assets-F2 处置需求符合性审计

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Mission: requirement-compliance
> Work Item: A1.23（MA1 需求追踪矩阵审计 — assets-F2 处置）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A1.23
> Related: `docs/plans/2026-08-02-1458-1-requirement-compliance-methodology.md`（M0.1 done）、`2026-08-02-1530-1-requirement-baseline-extraction.md`（M0.2 done，解除 A1.23 的 0.2 依赖）、`2026-08-03-0900-2-rc-ma1-a1-22-assets-f1-depreciation-engine.md`（assets 域先序切片 F1 折旧引擎；本切片 UC-AST-05"先补提当期折旧至出售日"前置依赖 F1 UC-AST-07 补提能力结论，故 F2 后于 F1）
> Audit: required

## Current Baseline

> 本计划是**审计工作项**（verification or audit work），结果表面 = 一份审计报告。基线盘点的是被审功能的现状代码/测试/既有证据，**不修改任何代码**。

- **方法论契约 + UC 锚点已就绪**：`docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）已落盘；`docs/audits/rc-requirement-baseline-inventory.md` 已为 A1.23 给出 UC 清单 = `UC-AST-04/05`（2 UC），含 `use-cases.md:65/:84` 锚点，覆盖率 `✅ 一致`（无基线分歧 D-xx）。

- **L1 需求契约（权威真相源）**：`docs/design/assets/use-cases.md`（机制见 `depreciation-and-posting.md §三` + `state-machine.md §3`）：
  - UC-AST-04 资产报废处置（`:65`）：IN_SERVICE → SCRAPPED（报废）；生成处置凭证（借 累计折旧结转、固定资产清理 / 贷 固定资产原值结转）；清理损失 = 账面净值 − 处置收入（报废收入常为 0）；损失计入营业外支出；卡片状态终态（不可恢复）。
  - UC-AST-05 资产出售处置（`:84`）：IN_SERVICE → SOLD（出售）；清理损益 = 处置收入 − 账面净值；若 >0 收益（贷 营业外收入）；若 <0 损失（借 营业外支出）；**先补提当期折旧至出售日（确保累计折旧准确）**。

- **L3 代码实现现状（实测）**——UC-AST-04 已实现且测试强；UC-AST-05 出售补提折旧**完全缺失**：
  - **处置入口（UC-AST-04/05，R6.3 per-mutation 拆分）**：`ErpAstDisposalApproveProcessor.approve:22-31`（require → idempotency → validateNotCancelled → validateTransition → validateForApproval → executeApprove）；Facade 域逻辑 `ErpAstDisposalProcessor.executeApprove:62-101`（R5.4 Pattern B 单一真相源）；BizModel 为薄壳 `ErpAstDisposalBizModel.java:17-27`（零业务逻辑）；xbiz `ErpAstDisposal.xbiz:23-31` approve 注入 Processor，实体 useWorkflow=true（orm `:578`，xwf browser-layer limit 见 state-machine.md:186-195）。
  - **状态迁移（UC-AST-04/05，已实现）**：`ErpAstDisposalProcessor:72-77`（disposalType==SOLD → ASSET_STATUS_SOLD，否则 ASSET_STATUS_SCRAPPED；常量 `ErpAstConstants.java:65-69,97-98`）。
  - **清理损益计算（UC-AST-04/05，已实现）**：`ErpAstDisposalProcessor:66-70`（original − accumDep = nbv；gainLoss = disposalAmount − nbv）；`DisposalPostingDispatcher.buildEvent:110-114`（同式重算入 BILL_DATA_GAIN_LOSS）。
  - **处置凭证（UC-AST-04/05，已实现但合并科目）**：`DisposalPostingDispatcher.tryPost:51-64` → buildEvent `:98-131`（businessType=`ErpFinBusinessType.DISPOSAL`=90 `:100`，**单 DISPOSAL 类型按 disposalType 分支**，owner doc §十:346 显式声明 §1.1/§7.1 的 DISPOSAL_SCRAP/DISPOSAL_SALE **类型命名**陈旧）→ `AssetPostingExecutor`（REQUIRES_NEW → `IErpFinVoucherBiz`）；`DisposalAcctDocProvider.java:29-86`（supports DISPOSAL）→ 借 累计折旧结转 `:69-72` / 借 银行存款(disposalAmount>0) `:74-76` / 损益分支(>0 贷 营业外收入 6301 `:78` / <0 借 营业外支出 6711 `:80-82` gainLoss.negate()) / 贷 固定资产原值结转 1601 `:83-85`。**缺口 #2（doc drift，须 §4 三判据复核）**：L1 `:72-74` 及 owner doc §三:136-137 要求"借 固定资产清理 / 贷 固定资产"两步流（含中间"固定资产清理"科目腿），实现**合并为单凭证无中间"固定资产清理"科目腿**。注意 owner doc **内部不一致**：§一/§7.1（`:19-20`）的旧表实际**匹配**实现（无固定资产清理腿），而 §三:136-137 描述含固定资产清理腿——§十 Deferred 标注仅覆盖**类型命名**（DISPOSAL_SCRAP/SALE→DISPOSAL），**未**覆盖固定资产清理腿合并。故 §4 三判据 (ii)（owner doc 显式 documented simplification 标注）复核**预期不满足** → #2 倾向重新打开为 P1 入 MR1（Phase 2 已预见此路径）。
  - **终态不可恢复守卫（UC-AST-04，已实现）**：`ErpAstDisposalProcessor.validateAssetDisposable:184-198`（status SCRAPPED/SOLD → ERR_DISPOSAL_ASSET_ALREADY_DISPOSED `:187-191`；非 IN_SERVICE/IDLE → ERR_DISPOSAL_ASSET_NOT_DISPOSABLE `:192-197`）；grep `ASSET_STATUS_SCRAPPED|ASSET_STATUS_SOLD` 仅 `ErpAstDisposalProcessor:116` executeReverseApprove 重置回 IN_SERVICE，**且该路径 gated by posted==true**（`:112`）；posted=false 窗口是 **P1-MA2-060**（resolved R1.16）。终态守卫同见于 Inventory shortage `ErpAstInventoryProcessor:264-270`、Maintenance `:137-138`、ValueAdjustment `:189-190`。
  - **出售补提折旧（UC-AST-05，缺口 #1 — 最高风险）**：跨模块 grep `catchUp|补提|preDisposal|beforeDisposal|depreciateTo|depreciate.*disposal|saleDate` 于 `ErpAstDisposalProcessor` → **零生产匹配**（仅 `ErpAstErrors.java:96` 拒绝消息串）。`ErpAstDisposalProcessor.executeApprove:62-101` 全体：validate → setStatus → cancelPendingSchedules（仅取消**未来**计划，从不跑当期）→ setApproveStatus → tryPost。**从不调用 `IErpAstDepreciationScheduleBiz.executeDepreciation(...)`**。`DisposalPostingDispatcher.buildEvent:98-131` 读 `asset.getAccumulatedDepreciation()`（截至上期末账面值），非重算至处置日值。**结论**：L1 `:94`"先补提当期折旧至出售日（确保累计折旧准确）"**未实现**——月中处置且上月末折旧则累计折旧低估 → 净值高估 → 清理损益误算 → GL 6711/6301 错报。**未登记 finding**（候选新 P1-RC-xxx，§2 P1①验收标准实质偏离 + §5 会计正确性无例外）。此为 A1.22 UC-AST-07 补提能力缺失的**下游投影**（即便补提 mutation 存在，处置也须调用它）。
  - **跨域 Facade**：`IErpFinVoucherBiz`（post/reverse 经 `AssetPostingExecutor:24-42`）；`IErpSysNotificationBiz`（失败告警 `DisposalPostingDispatcher:45,67-83` event key `ast.disposal-posting-failure`）。**处置路径无 daoFor(ErpFin*) 直写**（A2.10+A4.3 确认；P1-MA4-015 为只读 daoFor 已永久豁免）。

- **L4 测试证据现状**（`module-assets/erp-ast-service/src/test/`）：
  - `TestErpAstDisposal.java`：testScrapLossAndTerminalStatus（**强** gainLoss=−12000 + posted + APPROVED + 2 未来计划 CANCELLED + DISPOSAL bill-r）、testSaleGainAndBankCredit（**强** original=12000/accumDep=4000/NBV=8000/disposalAmount=10000 → gainLoss=+2000 + SOLD + bill-r）。
  - `TestErpAstDisposalWorkflowApproval.java`：testSubmitAgreeThenApproved（xwf 经理审批→cc-assets→approve→APPROVED+posted+SCRAPPED）/ testDisagreeThenRejected / testResubmitAfterRejected（均强）。
  - `TestErpAstPostingReverse.java`：testDisposalReverseApproveRestoresAsset（**强** posted=true → reverseApprove → posted=false+REJECTED+asset→IN_SERVICE+计划→PENDING+DISPOSAL 凭证 isReversed）、testEndToEndCapitalizationDepreciationDisposal（**强** e2e cap→2×折旧→处置 gainLoss=−10000+posted+SCRAPPED）、testCapitalizationSuspendedPostedFalseHanging（posted=false 悬挂，P1-MA2-060 证据）。
  - `TestErpAstAcctDocProviderAccountKey.java#testDisposal:43-54`（强 assertKeys 4 facts 累计折旧/银行存款/营业外支出/固定资产）。
  - E2E `reports/ast-disposal.smoke.spec.ts`（**弱/冒烟** GraphQL 200，P1-MA5-012）；`reports/ast-disposal.value.spec.ts:1-16`（**仅结构 token**——E2E seed DB 无处置行 `_vfs/_init-data/` 缺 erp_ast_disposal.csv，无数值断言，已登记 successor 待 E2E seed）。**无浏览器层 E2E 覆盖 UC-AST-04/05 直接 approve 流**（xwf browser-layer limit，DIRECT 三轴可达但无 spec）。
  - **缺口测试**：UC-AST-05 补提折旧路径（路径不存在故无测试）；DISPOSAL 凭证行级结构（仅 findBillLinks 存在性 `:98-99`，非行级科目断言）；posted=false 窗口处置资产留 SCRAPPED+reverseApprove 不恢复的不对称（仅覆盖 posted=true reverse）。

- **L5 既有证据（MA2 复用输入）**：
  - `docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`（A2.10）：findings P1-MA2-060（Cap/Disposal tryPost 吞异常悬挂 + reverseApprove 仅 posted=true 回滚致资产侧悬挂，**resolved R1.16** `:314`/`arm-index.md:299`）、P1-MA2-061（IDLE 零实现 resolved R1.18）、P2-MA2-059/060/061（doc 章节/模式声明/死代码 watch-only）。**无处置补提折旧 finding**。
  - `docs/audits/2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md`：P1-MA4-013（dispatcher 悬挂 resolved R1.16）、P1-MA4-014（处置/Processor 测试有效性 resolved R2.12）、P1-MA4-015（跨域 daoFor resolved 永久豁免）、P2-MA4-006（tryPost 返回类型 drift watch-only）。
  - **本切片须声明与上述 MA2/MA4 报告的差异增量**（报告段落 9）：复用其已证实行为，只补"需求契约↔行为"差异（UC-AST-05 出售补提折旧完全缺失 / 处置凭证合并科目腿 doc drift 复核 §4 三判据 / P1-MA2-060 resolved 状态 HEAD 复核）。

- **arm-index 既有 finding 衔接**：相关既有 finding：`P1-MA2-060`（Cap/Disposal tryPost 悬挂 + reverseApprove 不对称，resolved R1.16，`arm-index.md:299`）、`P1-MA4-014`（处置测试有效性 resolved R2.12，`:619`）、`P1-MA5-012`（dashboard/disposal smoke → R3.2，`:642`）。**RC 系列对 assets 为零**——A1.23 为 assets 域第二个 RC 切片（A1.22 先行）。本切片须 grep arm-index assets 处置同域同控制点后裁决：UC-AST-05 补提折旧缺失 vs P1-MA2-060（060 是悬挂不对称，本切片是补提能力完全缺失，不同根因/不同控制点）→ 裁决**新建 P1-RC-xxx**；处置凭证合并科目腿 → 新建 P2-RC-xxx doc drift。

- **ORM 关键字段**（`module-assets/model/app-erp-assets.orm.xml`）：`ErpAstAsset`（`:170-255`）卡片本身**无 disposalDate/disposalIncome/scrapReason 列**（这些在独立 ErpAstDisposal 实体）；`ErpAstDisposal`（`:575-642`，独立 action 实体 useWorkflow=true `:578`）assetId `:586`（FK relation `:613-615`）/ disposalType `:587`（dict erp-ast/disposal-type SCRAPPED/SOLD `:95-98`）/ disposalAmount `:588` / businessDate `:591`（处置日期）/ gainLoss `:592` / reason `:593`（dict disposal-reason OBSOLETE/SOLD/DONATED/STOLEN/OTHER `:104-110`）/ posted `:596` / nopFlowId `:610`；`ErpAstAssetCategory.disposalGainLossSubjectId` `:280`（映射 ErpMdSubject relation `:294-296`）。

- **保护区域**：本审计为**只读审计**。属 roadmap 预授权类目。发现的 P0/P1 finding **不在本计划实施修复**——按 §10，P0 经 MR0、P1 经 MR1；触及会计过账逻辑（处置凭证/补提折旧）或 ORM 结构（补处置补提编排）的修复行须 ask-first（§5 保护区域暂停协议）。UC-AST-05 补提缺失涉会计正确性（清理损益误算→GL 错报）——若定级 P1 须确认非 owner doc 范围裁剪（L1 `:94` 明确要求"先补提"，owner doc §三未提补提为 drift 非裁剪）。

- **剩余差距**：A1.23 切片五级追踪审计报告缺失 = MA4 及 MR1 的该切片证据缺口来源，且 UC-AST-05 出售补提折旧缺失是 A1.22 UC-AST-07 补提能力缺失的下游投影证据。本计划产出 A1.23 报告并登记 finding，解除其链路证据缺口。

## Goals

- 产出 A1.23 切片审计报告 `docs/audits/<执行时间戳>-rc-ma1-a1-23-assets-f2-disposal.md`，含方法论 §6 **9 段全部内容**。
- 对 2 UC（UC-AST-04/05）逐条核验**每条验收标准**（完整枚举，§3）：逐 UC 一矩阵行，禁止合并。
- 对候选缺口给出分级结论：#1 UC-AST-05 出售前补提当期折旧至出售日**完全缺失**（倾向 P1——会计正确性：累计折旧低估→净值高估→清理损益误算→GL 6711/6301 错报，§2 P1①验收标准实质偏离 + §5 会计正确性无例外）、#2 处置凭证合并"固定资产清理"中间科目腿（doc drift：owner doc §三:136-137 要求 vs 实现/§一:19-20 无；§十 Deferred 仅覆盖类型命名未覆盖科目腿合并，复核 §4 三判据 (ii) 预期不满足→倾向重新打开 P1）、#3 DISPOSAL 无浏览器层 E2E（E2E seed 缺处置行，倾向 P2 successor）、#4 P1-MA2-060 resolved 状态 HEAD 复核（验证 R1.16 修复实际落地：posted=false 窗口 reverseApprove 是否回滚资产 + 失败告警派发是否生效）——按 §2 判据定级，若为 P0/P1 则新建 `P0-RC-xxx`/`P1-RC-xxx` 并按 §10 触发 MR0/MR1（本计划仅登记，不实施修复）。
- 报告产出即更新 `docs/audits/arm-index.md`（新 RC finding 入对应分区；既有行追加 RC 注记）。

## Non-Goals

- **不修复 finding**（修复属 MR0/MR1；本计划是审计）。
- **不修改真相源**（§9 冻结条款——分歧记入报告，不直改 use-cases/depreciation-and-posting.md/state-machine.md）。
- **不修改代码/ORM/api.xml**（只读审计）。
- **不审计其他 MA1 切片**（A1.22 折旧引擎独立 plan；A1.23 只覆盖 UC-AST-04/05）。
- **不执行 MA4 运行时探针展开**（本计划只产出静态存疑点清单）。
- **不重跑既有 MA2 行为审计**（§去重协议：处置状态迁移/终态守卫/损益计算/幂等 reverse 行为由 A2.10 证实，只补需求视角差异）。
- **不自决 UC-AST-05 范围裁剪**——L1 `:94` 明确要求"先补提当期折旧至出售日"，owner doc §三未提补提属 drift 非裁剪；若审计发现需求本身不合理须经人工批准改 product-scope（§4 出口 iii，需求变更非降级）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§1-§10 + §去重协议 + §4 三判据）+ `docs/backlog/requirement-compliance-roadmap.md`（A1.23 工作项）+ `docs/audits/rc-requirement-baseline-inventory.md`（A1.23 UC 锚点）+ `docs/design/assets/use-cases.md`（L1 真相源）+ `docs/design/assets/depreciation-and-posting.md`（L2 §三 处置/§十 实现约定 Deferred，非真相源）+ `docs/design/assets/state-machine.md`（L2 §3 终态）+ `docs/audits/arm-index.md`（finding 衔接）+ 上述 MA2/MA4 报告（L5 既有证据）
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA1 全部 A1.x 指定）。其必需输入均已就绪。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 审计以读代码/测试/报告为主（纯分析）。L5 行为证据复用既有 MA2/MA4 报告 + 单测；若需即时行为确认可跑既有 JUnit（如 `mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstDisposal,TestErpAstDisposalWorkflowApproval,TestErpAstPostingReverse`）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter，退出码恒 0；本审计无生产代码变更）。

## Execution Plan

### Phase 1 - 五级追踪矩阵填充与逐 UC 符合性结论

Status: completed
Targets: `docs/audits/2026-08-03-0900-3-rc-ma1-a1-23-assets-f2-disposal.md`（落盘 §1-§5）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0.1 + M0.2 done

- [x] `Proof` 对 UC-AST-04/05 **逐 UC 一矩阵行**填 L1-L5（§1 格式）：L1 逐字引用 `use-cases.md:65/:84` 验收标准原文（UC-AST-05 须逐字引用":94 先补提当期折旧至出售日"）；L2 引用 `depreciation-and-posting.md`（§三 处置 `:130-176` + §十 实现约定 `:346` Deferred 合并科目）+ `state-machine.md`（§3 终态，标注"设计参考，冲突以 L1 为准"，注意 §三未提补提 vs L1:94 要求补提的 drift）；L3 引用 `module-assets/.../ErpAstDisposalProcessor.java:<line>` / `ErpAstDisposalApproveProcessor` / `DisposalPostingDispatcher` / `DisposalAcctDocProvider` / `ErpAstDisposalBizModel` / `AssetPostingExecutor`（含跨域 `IErpFinVoucherBiz`/`IErpSysNotificationBiz`）；L4 引用 `TestErpAstDisposal.java#method` / `TestErpAstDisposalWorkflowApproval` / `TestErpAstPostingReverse` / E2E spec（注明断言强度）；L5 复用 MA2 A2.10 + MA4 + 单测。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 重点核验**候选缺口**（逐条验收标准对照）：①UC-AST-04 状态迁移 IN_SERVICE→SCRAPPED（已实现 `ErpAstDisposalProcessor:72-77`）；②UC-AST-04 处置凭证借贷（已实现但合并科目 `DisposalAcctDocProvider:69-85` 无"固定资产清理"中间腿——#2 doc drift：owner doc §三:136-137 要求含固定资产清理腿 vs 实现/§一:19-20 无；§十 Deferred 仅覆盖类型命名未覆盖科目腿合并，§4 三判据 (ii) 复核预期不满足→倾向重新打开 P1）；③UC-AST-04 清理损失=净值−收入→营业外支出（已实现 `:66-70,80-82`）；④UC-AST-04 终态不可恢复守卫（已实现 `validateAssetDisposable:184-198`）；⑤UC-AST-05 状态迁移 IN_SERVICE→SOLD（已实现）；⑥UC-AST-05 损益分支 >0 贷营业外收入/<0 借营业外支出（已实现 `:78-82`）；⑦#1 UC-AST-05 出售前补提折旧**完全缺失**（`ErpAstDisposalProcessor.executeApprove:62-101` 从不调 executeDepreciation；grep catchUp/补提/depreciateTo 零匹配；buildEvent 读陈旧 accumulatedDepreciation）；⑧#4 P1-MA2-060 resolved HEAD 复核（posted=false 窗口 reverseApprove 是否回滚资产 + 失败告警派发 `DisposalPostingDispatcher:67-83` 是否生效）；⑨#3 DISPOSAL 无浏览器层 E2E（seed 缺处置行）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` 按 §2 判据对每 UC 给出符合性结论（取最高）：UC-AST-04 状态/损失/终态→**接受**（行为正确，测试强）；UC-AST-04 凭证合并科目腿→#2 doc drift 复核 §4 三判据（§十 Deferred 未覆盖科目腿合并，(ii) 预期不满足→倾向重新打开 P1）；UC-AST-05 状态/损益分支→**接受**（行为正确）；UC-AST-05 出售补提折旧缺失（倾向 **P1**——§2 P1①验收标准实质偏离"先补提当期折旧至出售日" + §5 会计正确性无例外：累计折旧低估→净值高估→清理损益误算→GL 错报；L1:94 明确要求，owner doc §三 drift 非裁剪）；UC-AST-05 E2E→倾向 **P2** successor（seed 缺数据）。每结论须列明命中判据编号 + 三源对照。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 报告 §1-§5 已落盘：UC-AST-04/05 各一矩阵行，L1 逐字引用（含 UC-AST-05:94 补提原文）、L3 含行号、L4 注明断言强度、L5 标注复用 A2.10/A4 来源
- [x] 每 UC 有符合性结论（P0/P1/P2/接受）且列明 §2 判据编号；候选缺口 #1-#4 有明确分级（非悬空"待查"）；#2 documented simplification 复核结论已记录（满足/不满足 §4 三判据）；#4 P1-MA2-060 HEAD 复核结论已记录

### Phase 2 - finding 登记 / arm-index 衔接 / 静态存疑点 / 过程纪律自检 / 报告完整性

Status: completed
Targets: `docs/audits/2026-08-03-0900-3-rc-ma1-a1-23-assets-f2-disposal.md`（补 §6-§9）；`docs/audits/arm-index.md`（新 RC finding 入分区）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1 完成

- [x] `Decision` **复用 or 新增 裁决**（§7）：grep `arm-index.md` assets 处置同域同控制点（P1-MA2-060 悬挂不对称 / P1-MA4-014 测试有效性 / P1-MA5-012 E2E）后裁决——同根因同控制点 → 复用（追加 RC 注记）；新根因 → 新建 `P1-RC-xxx` 列明差异依据。#1 出售补提折旧缺失 vs P1-MA2-060（060 悬挂不对称，本切片补提能力完全缺失，不同根因/控制点）→ 新建。禁止未经比对新建。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Add` 报告 §6 与 arm-index 衔接段：列明每条 finding 复用/新增裁决 + 双向可追溯（finding ID ↔ 修复行预留 MR0/MR1）；记录 #2 documented simplification 复核结论（满足三判据则维持 P2 watch-only；不满足则 §4 重新打开为 P1 入 MR1）；记录 #4 P1-MA2-060 HEAD 复核结论。
      - Skill: none
- [x] `Add` 报告 §7 静态存疑点清单（供 MA4 展开）：登记 L5 无法静态定论、需运行时确认的点（如月中处置在不同折旧已计提状态下的实际累计折旧/清理损益偏差 / posted=false 窗口处置 reverseApprove 实际是否回滚资产 + 告警实际派发 / 合并科目凭证在不同 disposalType 下的实际行级结构等；每存疑点一行；无则注明"无"）。**P0 即时通道**：若 Phase 1 定级出 P0，按 §10 登记 + 本计划记录"已触发 MR0 追加 R0.n"（不实施修复）。
      - Skill: none
- [x] `Proof` 报告 §8 过程纪律自检段：实际运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明"无回归风险"）；closure-audit 独立性声明；与 arm-index 交叉去重声明。**不以 checker 退出码 0 为门控通过依据**。
      - Skill: none
- [x] `Add` 报告 §9 与 MA2 报告差异增量声明：复用 `2026-07-28-0400-arm-ma2-assets-state-machine.md`（处置状态迁移/终态/损益/幂等 reverse PASS + P1-MA2-060 resolved）+ `2026-07-29-0024-...-code-quality.md`（P1-MA4-014/015 resolved），列明只补的需求视角差异（UC-AST-05 出售补提折旧完全缺失 / 处置凭证合并科目 doc drift 复核 / P1-MA2-060 HEAD 复核）。
      - Skill: none
- [x] `Add` 报告产出即更新 `docs/audits/arm-index.md`：新 `P*-RC-xxx` 入对应分区；既有行（P1-MA2-060/P1-MA4-014）追加 RC 复核注记。
      - Skill: none
- [x] `Proof` 报告 9 段完整性自检：落盘前自查 §1-§9 全部存在。
      - Skill: none

Exit Criteria:

- [x] 报告 §6-§9 已落盘，9 段齐全；finding 复用/新增裁决均有 arm-index grep 依据；#2/#4 复核结论已记录
- [x] 新 RC finding 已写入 `arm-index.md`；静态存疑点清单已登记（供 A4.1 展开）
- [x] §8 自检段含 checker actual vs baseline 实测表 + 独立性 + 交叉去重声明

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立子代理 ses_03ba8b8c2ffeGi7nlgAH17uvas，fresh session，未起草本计划）。规则 1-13 全 PASS：(1) Deps A1.23=0.2 done（A1.22 非硬依赖，仅证据/顺序 cross-ref；Phase 1 Prereqs 仅 M0.1+M0.2 done）；(2) 单结果表面（A1.23 报告 UC-AST-04/05，无 A1.22 折旧范围泄漏）；(3) 格式 + 命名合规（N=3 = assets-F2，在 N=2 assets-F1 之后；理由：UC-AST-05 先补提是 UC-AST-07 补提能力的下游投影——证据/顺序非硬依赖）；(4) UC 覆盖精确（baseline-inventory:357 = UC-AST-04/05）；(5) Baseline 9/9 spot-check 全 CONFIRMED——`ErpAstDisposalProcessor.executeApprove:62-101` 从不调任何折旧方法 / catchUp|补提|preDisposal|depreciateTo|saleDate 于处置 4 文件 0 匹配 / `DisposalAcctDocProvider` 无"固定资产清理"(1606) 中间腿（仅 1602/1002/6711|6301/1601）/ `validateAssetDisposable:184-198` 拒 SCRAPPED/SOLD / P1-MA2-060 resolved R1.16 / executeReverseApprove posted==true gated :112 reset :116 / UC 锚点 use-cases.md:65/:84/:94/:72-74 行号精确；(6) 方法论 §1-§10 + §去重 + §4 三判据 + §7 对齐；(7) 反松弛；(8) Q4(a) 正确——UC-AST-05 补提涉会计正确性，P1 + L1:94 明确要求"先补提"，owner doc §三 drift 非裁剪，不自决范围裁剪，禁方案 B；§4 三判据复核 wired 进 Phase 1 Decision + Phase 2 + exit；(9) Closure Gates audit-only 有据。无阻塞。Non-blocking（**已应用修订**）：reviewer 指出 #2 处置凭证合并科目腿的 doc drift 源引用不准——§十:346 仅覆盖**类型命名**（DISPOSAL_SCRAP/SALE→DISPOSAL），**未**覆盖"固定资产清理"腿合并；真实 drift 源是 §三:136-137（含固定资产清理腿）vs 实现/§一:19-20（无）；owner doc 内部不一致使 §4 三判据 (ii) 复核**预期不满足**→#2 倾向重新打开 P1（Phase 2 已预见）。已将 Baseline/Phase 1 ②/Goals/Decision 修订：drift 源改为 §三:136-137，标注 owner doc 内部不一致 + (ii) 预期不满足→倾向 P1；并统一缺口编号（#1=出售补提最高风险 / #2=凭证合并科目 / #3=E2E / #4=P1-MA2-060 HEAD 复核）。共识达成，转 active。

## Closure Gates

> 本计划为**只读审计**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 报告 9 段完整性 + 五级矩阵逐 UC 覆盖 + finding arm-index 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A1.23 报告 9 段齐全 + 2 UC 逐矩阵行 + finding 登记入 arm-index
- [x] 相关文档对齐：报告与方法论 §1-§10 + §去重协议 + §4 三判据一致；与 rc-requirement-baseline-inventory A1.23 锚点一致
- [x] 已运行验证：报告 9 段完整性自检 + §8 checker actual vs baseline 实测记录（0 漂移，全 16 规则 actual==baseline）+ finding 复用/新增裁决可追溯 + documented simplification 复核 + P1-MA2-060 HEAD 复核结论可追溯（无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### finding 的修复实施

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划是审计，结果表面 = 报告 + arm-index 登记。finding 的修复按 §10 经 MR0（P0）/ MR1（R1.0 展开 RC-R1.n，P1）实施；触及会计过账逻辑（处置凭证/补提折旧编排）或 ORM 结构（补处置补提调用）的修复行须 ask-first + 独立 plan-audit（§5）。UC-AST-05 补提缺失与 A1.22 UC-AST-07 补提能力缺失同根（补提 mutation 不存在），修复行应与 A1.22 的 RC-R1.n 补提能力修复协同。
- Successor Required: yes（MR0/MR1 按本报告 finding 交叉引用展开修复行；UC-AST-05 补提修复与 A1.22 UC-AST-07 补提能力修复协同）

## Closure

Status Note: A1.23 assets-F2 处置需求符合性审计完成。报告 `docs/audits/2026-08-03-0900-3-rc-ma1-a1-23-assets-f2-disposal.md` 9 段齐全，2 UC（UC-AST-04/05）逐矩阵行五级追踪完成。裁决：UC-AST-04 接受 on ①③④⑤ + P1 on ②（凭证合并科目腿新建 P1-RC-030，§4 三判据均不满足→重开，GL 平衡不破坏仅凭证结构偏离）；UC-AST-05 接受 on ①②③④ + P1 on ⑤（出售补提缺失 reuse P1-RC-029，A1.22 UC-AST-07 下游投影）。新 P2-RC-027（DISPOSAL 无浏览器层 E2E successor）。P1-MA2-060 HEAD 复核确认 resolved R1.16（§4 (i)/(ii) 满足维持 resolved）。arm-index 已追加 A1.23 交叉引用注记 + P1-RC-030/P2-RC-027 finding 行。§8 checker 全 16 规则 actual==baseline 0 漂移（无生产代码变更）。P0 即时通道未触发。结束审计已由独立子代理（新会话，未执行本计划）执行并通过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure-audit 新会话，未参与本计划执行/起草）
- Evidence: 报告 `docs/audits/2026-08-03-0900-3-rc-ma1-a1-23-assets-f2-disposal.md`（9 段齐全，实测 46KB 落盘）；arm-index `docs/audits/arm-index.md`（报告清单 A1.23 行 done + RC 发现追踪 P1-RC-030/P2-RC-027 行 + P1-RC-029 reuse UC-AST-05 投影注记 + A1.23 summary 交叉引用注记段 :175）；语义核验通过——Phase 1/2 全 items + Exit Criteria [x]、Closure Gates 全 [x]、findings 在 arm-index 实际存在（grep 确认 P1-RC-029 :151 / P1-RC-030 :154 / P2-RC-027 :155）、无 anti-hollow（findings 含行号证据 + MR1 修复路径）、无 deferred 造假（finding 修复归 MR1 successor 正确）、日志同步 `docs/logs/2026/08-03.md` A1.23 条目已落

Follow-up:

- finding 修复属 MR0（P0）/MR1（P1 R1.0 → RC-R1.n）实施义务，非本审计计划范围（见 Deferred But Adjudicated）
- UC-AST-05 出售补提折旧修复须与 A1.22 UC-AST-07 补提能力修复协同
