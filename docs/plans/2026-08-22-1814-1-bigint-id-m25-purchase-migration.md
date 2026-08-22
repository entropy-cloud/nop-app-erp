# 2026-08-22-1814-1-bigint-id-m25-purchase-migration 主键/外键 string 化 M2.5：purchase 域迁移（冻结序位次 15）

> Plan Status: completed（2026-08-22：四 Phase 全部完成 + 独立结束审计两轮——首轮 `fails`（BLOCKER-1 登记册 554/704 行未转义引号 + MAJOR-1 fin/inv 计数口径）→ 修复后复核 `passes closure audit`（ses_fd6832d55ffe8TRerprCbeUMRg）；治理 + 技术双批准见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M2.5（purchase，冻结序位次 15）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 15（M2.5）
> Related: `docs/plans/2026-08-22-1302-3-bigint-id-m31-manufacturing-migration.md`（bridge-main-088/089 落桥来源 + B 退役最大单批先例）、`docs/plans/2026-08-22-1302-2-bigint-id-m23-quality-migration.md`（bridge-main-094/096 + bridge-test-130 桩登记来源）、`docs/plans/2026-08-22-0731-2-bigint-id-m22-inventory-migration.md`（bridge-main-075..079 + bridge-test-119..124 pur 半边登记来源）、`docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`（bridge-main-069 + bridge-test-118 pur 半边登记来源）、`docs/plans/2026-08-21-2025-2-bigint-id-m38-b2b-migration.md`（bridge-main-026..031 + bridge-test-133 登记来源）、`docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md`（bridge-main-033..050 落桥来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **冻结序位置**：位次 15；精确前置 M2.7 + M2.3 + M2.2 + M2.1 + M2.4 + M1.1 + M1.2 + M3.6 全部 `done`（位次 1-14 已完成，M3.1 2026-08-22 收官）。工作项可转 `ready`。
- **工具现状（2026-08-22 scan）**：`module-purchase/model/app-erp-purchase.orm.xml` **118 列 NEEDS FIX**（PK 33 + FK 85；实体构成 pur 99 + md stub 15 + ct stub 3 + prj stub 1，16 个 notGenCode stub 实体/19 stub 列——stub 实体全部属已迁移域，随本域翻转，权威源对齐 M1.1 先例；分解以 Phase 1 dry-run 为准），0 DEFERRED；全仓剩余待改 439 列/5 文件（pur 118 + sal 108 + crm 126 + drp 55 + logistics 32）。
- **登记册义务（`tools/id-migration-registry.json5` + 审计文档 §6.15，Phase 1 强制全量复核）**：
  - A1 延后列：0；A2 main 前向桥：**2 条**（bridge-main-090 `ErpPurReceiveProcessor:324` `IErpInvDrpCrossDockBiz.markReceivedFromPurchase(inboundMoveId)` + 091 `:378` `IErpInvDrpLeadTimeRecordBiz.recordFromPurchaseReceive(supplierId)`，晚域 drp(18)，退役 owner M3.7——本 plan 落 String→Long 桥 + grep 例外登记）；A3 test 前向桥：0。
  - **B main 桥接退役：26 条**（本域翻转 IBiz 参数签名时退役 + 通知早域移除桥接点）：b2b 6（026..031）+ contract 10（033/034/037/039/040/043/044/047/049/050）+ finance 1（069 `DualSideConsistencyChecker`，M2.1 类型级核证零转换点）+ inventory 5（075..079，M2.2 落桥含 eq 语义值桥）+ manufacturing 2（088/089 `MrpReleaseService` toLong 桥 ×6 转换点）+ quality 2（094/096 `NcrReturnOrchestrator` save map toLong 桥）。预计超 mfg 批（20 条）成 mission 新最大单批。
  - **B retired-test pur 半边回收（候选招单，Phase 1 逐条对 registry retired note 重验）**：b2b 133（`TestErpB2bAsnInventoryIntegration` FQN ×20 行/22 token，6 转换点）、contract 108/109/110(pur 半边)/111（M3.6 落局部桥/读路径核证）、finance 118（M2.1 seed 侧 `Long.valueOf` 局部桥）、inventory 119/120/121/123/124（M2.2 落桥）、manufacturing 126 pur 半边（M3.1 落断言/种子 toLong 桥）、quality 130（`TestStubErpPurReturnBiz` 桩保持 Long 签名——本域翻转后桩签名随 IErpPurReturnBiz String 化，M3.2 TestMockMntBizModels 先例）。
  - C1 后向 main 6 条（backward-178 ct 1 文件 / 179 fin 6 / 180 inv 6 / 181 md 16 含 pur-dao `IErpPurPaymentBiz` / 182 prj 1（`ErpPurReceiveProcessor`）/ 183 qa 2）；C2 后向 test 7 条（239 ct 1 / 240 fin 27 / 241 inv 8 / 242 md 38 / 243 notify 1 / 244 prj 1 / 245 qa 1）——全部指向已迁移域，编译器驱动修复 + 测试适配定位面。
  - 被引用面（未迁移域 → 登记中间态，不修复）：drp main 3 + test 3、logistics test 1（successor M3.7/M3.10 + M4.1 兜底）。
- **已知 md 语义参数桥消费点**：`ReceiveStockMoveBuilder:52`/`ReturnStockMoveBuilder:53` 调 `IErpMdAcctSchemaBiz.findFirstByOrg(Long)`（md 侧手写 Long 签名遗留，`ErpMdAcctSchemaBizModel.java:28` 实测仍 Long）——本域 String 化后须落 `ConvertHelper.toLong(orgId)` 语义桥 + 退役条件注释（mfg `MaterialIssueStockMoveBuilder:54-55` 先例，登记例外）。
- **page.yaml**：pur-web 手写面 `:Long` ×3（`dashboard/three-way-match.page.yaml:79/:121/:159` `$f:Long` filter_supplierId ×3，M3.6 结束审计 MAJOR-1 adaptor 静默降级同型缺陷）。
- **代码与测试规模**：pur-service main 手写修复面以 C1 全清单为准（§6.15 计 21 文件：processor 9 + entity 4 + support 2 + service 根 2 + dashboard 1 + posting 1 + spi 1 + dao 1 `IErpPurPaymentBiz`）；test 58 个 `Test*.java`（43 快照式 JunitAutoTestCase）；快照 `_cases/` 现值 **3568** 文件（重录基线）。
- **早域复跑基线（B 义务验证）**：b2b 80/80、ct 168/168、fin 497/497、inv 235/235、mfg 289/289、qa 182/182（六个域，超 mfg 批三域）。
- **owner doc**：`docs/design/purchase/` grep 复核仅 `requisition.md:100` 一处 `Map<String,...>` 陈述（已 String 键，无需改）；预期注记面近零（Phase 4 复核确认）。

## Goals

- 118 列 `stdDataType long→string` 落源（stdSqlType 保持 BIGINT，DDL 零变化），三重证明（新鲜度门控 + git diff + 工具重扫 pur 段 0 NEEDS FIX）。
- pur 7 模块链（codegen,dao,meta,service,web,app,api）no-am main 绿 + 域级测试全绿（`mvn test -pl module-purchase/erp-pur-service,module-purchase/erp-pur-web`）。
- A2 桥 2 条落位（090/091，退役 owner M3.7）；B main 退役 26 条兑付（六早域转换点/注释移除 + 7 链重建绿 + grep 桥残留清零 + 测试基线复跑维持）；retired-test pur 半边回收 + qa 桩 String 化。
- 快照 RECORDING→CHECKING 每域重录（force-save-output 系统属性模式，mfg 先例）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` ×3 就地 String 化 + owner doc 注记 + 登记册状态更新 + 路线图 M2.5 → done + 日志。

## Non-Goals

- 不迁移 sal/crm/drp/logistics（冻结序位次 16-19，successor 各自 plan）；不修复 drp/logistics 对 pur 的引用破坏（登记中间态）。
- 不改 `stdSqlType`/DDL/CSV 种子/序列号引擎；不动 `delVersion` 等非 PK/FK BIGINT 列（含孤儿操作人列，另案 follow-up）。
- 不跑全量构建/全量测试/E2E（M4.1 专属）；不修 M4.1 登记的存量 page.yaml raw-GraphQL `:Long`（notify inbox:177 / aps schedule-gantt:59 / b2b edi-detail:45、asn-flow:79）。
- 不翻转 md 侧 `findFirstByOrg(Long)` 签名（md 域已迁移，该签名遗留登记在案，退役条件 = md 侧签名翻转，另案/M4.1 兜底）。

## Task Route

- Type: `implementation-only change`（orm 模型变更 → 增量重生成 → 编译器驱动修复，路线图 M1-M3 标准结构）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（标准结构 + 规则 1-8 + 横切 §5 保护区域 design 证据链）、`docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md` §6.15 + `tools/id-migration-registry.json5`（消费协议）、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B、`docs/design/domain-design-guidelines.md` §16A、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`（M0 裁决与冻结序结论）、`docs/design/purchase/`（业务 owner doc，Phase 4 注记对象）
- Skill Selection Basis: 域迁移 plan 预期技能 = `nop-backend-dev`（BizModel/IBiz 手写修复 + 跨实体调用规则）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；orm 回写机制走 M0.1 裁定工具链，不经技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。工具链：`node tools/check-bigint-id-types.mjs dry-run` + `node tools/verify-id-fix-copy-diff.mjs module-purchase` + `node tools/scan-cross-domain-id-coupling.mjs`；回写一律走「时点 dry-run + 新鲜度门控」三步，禁止盲 cp/apply 模式。
- 硬前置 = 最后全绿基线 commit 的全量 install + 每个已完成域链 install 已在本地 Maven 仓库（D3 修订口径）。
- 回滚策略（mfg/qa 先例）：orm 落源后回滚 = `git revert` orm 变更 + 7 模块链增量重生成；Phase 2/3 完成后回滚需先 revert 六早域桥接退役与测试代码变更，再 revert 本域 orm + 重生成。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: completed（2026-08-22：登记册逐条核对一致——A1=0（registry 零 pur orm-column-deferral）/ A2=090,091（active，retireOwner M3.7）/ B main 26 条 retireOwner=M2.5 全对（awk 逐条枚举）+ bridge-test-133 同挂 M2.5 / retired-test 候选重验：118/120/121/123/124/126(pur 半边)/130/133 均有 M2.5 兑付义务，**119（TestErpInvCostAdjust）实测零 pur 引用**（rg 全文零命中，registry note 目标=mfg 且 mfg 半边已 M3.1 兑付）= 候选清单过包含，无工作项，差异已解释；FQN 复扫双口径（java 内联非 import = b2b 133 文件 20 行 + mfg javadoc @link 注释 1 行非代码；beans.xml ioc:type = qa test-mock-purchase/posting 2 处 bridge-test-130 基建）全部命中已登记条目零补登；三步落源：dry-run 时点刷新 + 新鲜度门控 118 行 stdDataType-only/0 非法 + cp 落源 git diff 归一化逐行核证（minus sed 归一后与 plus 全等、stdSqlType 全保持 BIGINT、delVersion 零变化）+ 工具重扫 pur 段 0 NEEDS FIX/0 DEFERRED（总待改 439→321））
Targets: `module-purchase/model/app-erp-purchase.orm.xml`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 位次 1-14 全部 done（已满足）

- [x] Proof: 登记册全量消费——`registry §6.15 + json5` 逐条核对与 Baseline 一致（A1=0 / A2 090,091 / B main 26 / retired-test pur 半边候选 / C1 178-183 / C2 239-245 / 被引用面 drp+logistics）；差异即补登落册。
      - Skill: none
- [x] Proof: FQN 复扫（b2b A3' 盲区先例：java 内联 FQN 非 import 行 + `*.beans.xml` ioc:type 双口径）——pur 引用面补登核对，预期仅命中已登记条目。
      - Skill: none
- [x] Proof: 双独立子 agent 批准（治理 + 技术，fresh session）落盘 Draft Review Record（保护区域 `model/*.orm.xml` 要求）——已在 plan 落盘（治理 ses_fd7013b03ffevClMPTUuvejjqd + 技术 ses_fd70168fcffeLDHL9wpgkNzat8，iteration 1 双 `approve`，见 Draft Review Record 节）。
      - Skill: none
- [x] Fix: orm 落源三步——① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-purchase` 新鲜度门控（118 行 stdDataType-only、零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核（stdSqlType 全保持 BIGINT、delVersion 零变化、stub 列与权威源一致）。
      - Skill: none

Exit Criteria:

- [x] 工具重扫 pur 段 0 NEEDS FIX / 0 DEFERRED（总待改 439→321），三重证明落盘。
- [x] 登记册消费零未解释差异（或补登已落册 + fail-closed 解析验证通过）——唯一差异 = 候选 119 无 pur 引用（候选清单过包含，已解释，见 Phase 1 Status）。

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 落桥 + B main 退役 + 早域链重建

Status: completed（2026-08-22：pur 7 链 no-am main 绿（dao 11 文件 IBiz/值对象 id 参数族 + service 33 手写文件 = 首轮 164 错/22 文件三波编译器驱动清零 + BizModel @Override 涟漪 11 文件）；A2 090/091 落桥（ErpPurReceiveProcessor:328-330/:386-390 ConvertHelper.toLong ×2 + drpMaterialIds 循环桥 + 双向注释）；md findFirstByOrg 桥 ×2（Receive:53-54/Return:54-55，mfg 先例注释）；B main 26 条退役兑付——b2b 6 + ct 10（转换点/语义过滤值桥/桥注释移除，sal 半边 035/036/038/041/042/045/046/048/051/052 保持 owner M2.6）+ fin 069 类型级零转换点核证 + inv 075..079（含 LandedCostAllocationEngine DTO Long→String）+ mfg 088/089 ×5 转换点 + qa 094/096 save map ×3（sal 095/097 保持）；六早域 7 链 no-am 重建全绿 + grep bridge-main-0xx 清零；drp 破坏 = dao 58 错 100% `_gen` + service 78 错 5 文件 100% 已登记 backward-146/148/149 前向边，logistics main 绿（test 侧破坏归 backward-208/logistics test 登记），successor M3.7/M3.10/M4.1）
Targets: `module-purchase/erp-pur-{codegen,dao,meta,service,web,app,api}`、六早域桥接点文件
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Decision | Proof`
- Prereqs: Phase 1 完成

- [x] Fix: 7 模块链 no-am 重生成构建 main 绿（`-pl` 显式列表 + `-Dmaven.test.skip=true`）；编译器驱动修复手写面（dao 手写 IBiz id 参数族 + service processor/posting/entity/dashboard/support/spi，C1 清单为定位面）。
      - Skill: `nop-backend-dev`
- [x] Add: A2 桥 2 条落位（bridge-main-090/091：`ConvertHelper.toLong` 桥 + 代码内 bridge 注释双向指针，退役 owner M3.7）。
      - Skill: `nop-backend-dev`
- [x] Fix: md 语义参数桥 ×2（`ReceiveStockMoveBuilder`/`ReturnStockMoveBuilder` `findFirstByOrg(ConvertHelper.toLong(orgId))` + 退役条件注释，mfg `MaterialIssueStockMoveBuilder:54-55` 先例，登记例外）。
      - Skill: `nop-backend-dev`
- [x] Fix: B main 26 条退役兑付——六早域侧转换点/桥注释/import 移除 + String 直传（b2b 026..031 / ct 033..050 十条 / fin 069 / inv 075..079 / mfg 088/089 / qa 094/096），各早域 grep `bridge-main-0xx` 引用清零。
      - Skill: `nop-backend-dev`
- [x] Proof: 六早域 7 模块链 no-am 重建 BUILD SUCCESS（b2b/ct/fin/inv/mfg/qa）。
      - Skill: none
- [x] Decision: drp/logistics 引用破坏登记中间态——破坏模块清单 + successor 指针（M3.7/M3.10/M4.1）+ javac 错误点 100% 位于 `_gen` 或已登记前向边的逐模块证明（rule 6 / D4 联动，登记内破坏不触发停止）。
      - Skill: none

Exit Criteria:

- [x] pur 7 链 main 绿；六早域 7 链重建绿。
- [x] 桥接例外 grep 仅剩登记例外（A2 090/091 + findFirstByOrg ×2）。

### Phase 3 - 测试修复 + retired 半边回收 + 快照重录 + 域级测试 + 早域复跑

Status: completed（2026-08-22：58 测试类编译器驱动清零（三组并行 agent + 涟漪收尾，58 文件全 String 化——种子常量/断言/helper 签名/mock @Override；语义陷阱：算术派生 id `String.valueOf(idSeq.incrementAndGet())` 数值保真 ×9+ 文件 + seedInspection hash 派生 String.valueOf + 零 id 序比较/装箱 == 残留）；retired pur 半边回收：b2b 133 六转换点+种子 String 化 + ct 108/109/110 读路径核证零穿越 + 111 createPostedApInvoice 直传 + fin 118 setSupplierId 直传 + inv 120/121/123/124 桥移除种子 String 化（含 TestErpInvLandedCostAllocationEngine DTO 涟漪 ×9）+ mfg 126 pur 半边断言/种子直传（sal 半边注释改写 owner M2.6）+ qa 130 桩签名 String 化 asLong→asString；六早域 test-compile 绿；快照重录 force-save-output 全局重录（201 snapshot-finished 全部录制完成标记零真实失败 + 46 处 @EnableSnapshot-less 断言式目录超录按 cs 81 目录先例回退 + `_cases` 3568→3569（+1 合法新增 input 种子表 erp_qa_inspection.csv 空表头）+ json5 id String 实证 `"orderId": "7"`/CSV 列刷新）；pur 域级 334/334 绿 + web 0 tests 治理排除；IoC self-wait 未复现零 delta（surefire 全报零 self-wait，mfg M3.1 先例分支）；早域复跑六域基线维持全绿：b2b 80/80（第二环 self-wait 复现 → hr 第二 delta nopDataAuthChecker lazy-property 落位 + 首环 delta 补 x:extends="super" fin 修正版）+ ct 168/168（同双 delta + SIGN_DATE 日期漂移 `*` 通配 ×4）+ fin 497/497（console 口径；XML 口径 489——jqwik 属性引擎计数口径差，双口径均 0 失败 0 错误）+ inv 235/235（console 口径；XML 口径 230 同因）（算术派生 id int 溢出修复 `(long) hashCode()*10+n` ×4——数值保真陷阱实例）+ mfg 289/289（delVersion 毫秒边界 @var 漂移 `*` 通配，隔离复跑绿证 flake）+ qa 182/182）
Targets: `module-purchase/erp-pur-service/src/test`、六早域 test 桥文件
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成

- [x] Fix: 测试修复（58 个 `Test*.java`（43 快照式）编译器驱动清零 + 语义陷阱：id 序比较陷阱 → `ConvertHelper.toLong` 数值序（contract idOrder 先例）、装箱 `==` → `.equals()`、算术派生 id 数值保真 `String.valueOf(Long.parseLong(...))`、种子常量 String 字面量数字保真）。
      - Skill: `nop-testing`
- [x] Fix: retired-test pur 半边回收（b2b 133 六转换点 / ct 108-111 pur 半边 / fin 118 / inv 119/120/121/123/124 / mfg 126 pur 半边局部桥移除 + 双向指针注释删除）+ qa `TestStubErpPurReturnBiz` 桩签名 String 化（M3.2 mock 先例）；各早域 test-compile 绿。
      - Skill: `nop-testing`
- [x] Proof: 快照重录 RECORDING→CHECKING（`-Dnop.autotest.force-save-output=true` 全局重录，免注解编辑 + grep 零注解残留；`_cases/` 3568 基线 → String 形态落盘；非确定性单元格 `*` 通配（hr/mfg 先例）；逐案审核 id String 实证 + 拒绝「断言式」类误录（cs 81 目录超录回退先例）。
      - Skill: `nop-testing`
- [x] Proof: 域级测试 `mvn test -pl module-purchase/erp-pur-service,module-purchase/erp-pur-web` 全绿（web 0 tests 治理排除，successor M4.1）；平台 IoC 回归 self-wait 处置双分支证据——未复现则记录零 delta 证据（mfg M3.1 先例），复现则 fin 修正版先例 delta（`x:extends="super"`）落位（hr 第二环环境性回归 bug 文档参照）。
      - Skill: `nop-testing`
- [x] Proof: 早域复跑六域基线维持（b2b 80/80 + ct 168/168 + fin 497/497 + inv 235/235 + mfg 289/289 + qa 182/182；快照形态漂移就地 String 化 + `*` 通配按先例处置，逻辑破坏为零）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] pur 域级测试全绿 + 六早域基线维持全绿。
- [x] retired pur 半边 grep 清零（`bridge-test-118|119|120|121|123|124|133|108|109|110|111|126|130` 早域侧引用清零或形态核证）——119 零 pur 引用（Phase 1 重验结论，无工作项）；126 余 1 命中 = 改写后 sal-only 注释（形态核证）；其余全清零。

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: completed（2026-08-22：grep 门控清零——`.longValue()`/`Long.parseLong`/`Map<Long`/`Long.valueOf`/id `%d`/装箱 ==/id 序比较 main 全零（`String.format("%02d")` = dashboard 月份格式化合法非 id；`getId()==null`/`!=null` ×2 = null 检查合法；残留 Long 仅 A2 090/091 drpMaterialIds = 登记例外）；sql-lib 零存在注明（find 计 0）；page.yaml `:Long` ×3 就地 String 化（three-way-match:79/:121/:159 `$f:Long`→`$f:String`，findPage filter_supplierId 平台路径 String 语义核证）+ YAML 良构（python yaml.safe_load 过）+ pur-web 重建绿 + 手写 view 零被动变更（非 `_gen` 变更仅 1 个 page.yaml 主动 Fix）；owner doc 零变更（docs/design/purchase grep 复核仅 requisition.md:100 `Map<String,...>` 已 String 键确认无需改）；登记册收尾（B main 26 → retired 附逐条兑付 note + retired-test 12 条半边 note 指针 + A2 090/091 保持 active 附落位 note + fail-closed scan 复验 0 告警 151 active/107 retired）+ roadmap M2.5 → done（位次 15 行证据摘要 + 头部最后更新续链 + 位次 16 解锁）+ 日志 docs/logs/2026/08-22.md M2.5 完成条目）
Targets: pur-service main、`three-way-match.page.yaml`、`docs/design/purchase/`、登记册、路线图、日志
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 3 完成

- [x] Fix: 语义陷阱 grep 门控清零（路线图横切 §3 清单：`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/装箱 `==`/id 序比较；合法非 id 项逐条注明；sql-lib 零存在注明）。
      - Skill: none
- [x] Fix: page.yaml `:Long` ×3 就地 String 化（three-way-match:79/:121/:159 `$f:Long`→`:String`，BizModel `@Name("supplierId") String` 签名一致性核证）+ YAML 良构校验 + pur-web 重建绿 + 手写 view 零被动变更核证（git status 非 `_gen` 变更仅 page.yaml 主动 Fix）。
      - Skill: `nop-backend-dev`
- [x] Fix: owner doc 注记——`docs/design/purchase/` grep 复核 Long id 陈述（预期近零；`requisition.md:100` 已 String 键无需改），命中即就地注记引用本计划。
      - Skill: none
- [x] Add: 登记册状态更新（B main 26 条 → retired 附兑付 note；retired-test pur 半边 note 补指针；A2 090/091 保持 active；fail-closed 解析验证）+ 路线图 M2.5 → done（M2/M3 表位次 15 证据摘要 + 头部「最后更新」续链 + 位次 16 解锁）+ 日志 `docs/logs/2026/{执行当日}.md`（rule 8 日期口径，含验证状态 + B 义务兑付 + 六早域复跑基线）。
      - Skill: none

Exit Criteria:

- [x] grep 门控清零（登记例外除外）+ page.yaml `:Long` 清零 + YAML 良构。
- [x] 登记册/路线图/日志三处一致落盘。

## Draft Review Record

- Independent draft review iteration 1（技术/执行契约视角，fresh session，ses_fd70168fcffeLDHL9wpgkNzat8）: `acceptable as-is` + 保护区域技术侧 `approve`——118 列/PK33+FK85/实体构成/7 链/B main 26/C1/C2/早域基线/findFirstByOrg/page.yaml ×3/_cases 3568 逐项实仓核证全对；3 MINOR（sal 108 口径、C1 分解括注、test 43/58 口径）已全部就地修正。
- Independent draft review iteration 1（治理/规范视角，fresh session，ses_fd7013b03ffevClMPTUuvejjqd）: `acceptable as-is` + 保护区域治理侧 `approve`——命名/模板/状态一致性/反松弛/Deferred 卫生/技能映射/D3 口径/登记册消费协议全 PASS；3 MINOR（日志日期钉死、IoC 未复现分支、design 证据链补全）已全部就地修正（另按 sales 批审查 MINOR-5 交叉提示补 Infra 回滚策略与 Item Types 对齐）。
- **共识达成（2026-08-22）**：双审查者 iteration 1 均 `acceptable as-is` + `approve`（0 BLOCKER / 0 MAJOR），保护区域双独立子 agent 批准已落盘（治理 ses_fd7013b03ffevClMPTUuvejjqd + 技术 ses_fd70168fcffeLDHL9wpgkNzat8），MINOR 修订后转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（118 列落源 + 7 链绿 + A2/B/C 兑付 + 快照重录 + 测试绿）
- [x] 相关文档对齐（登记册 + 路线图 + owner doc + 日志）
- [x] 已运行验证：`mvn clean install -pl module-purchase/erp-pur-{codegen,dao,meta,service,web,app,api} -DskipTests`（no-am）BUILD SUCCESS + `mvn test -pl module-purchase/erp-pur-service,module-purchase/erp-pur-web` 334/334 绿 + 六早域复跑基线维持（b2b 80/80 + ct 168/168 + fin 497/497（console 口径，XML 489 jqwik 计数口径差双绿）+ inv 235/235（console 口径，XML 230 同因）+ mfg 289/289 + qa 182/182）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 节仅 3 条预裁定项）
- [x] 独立草案审查已完成并记录（保护区域双批准落盘）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行（ses_fd68e0da0ffeW7A0SbW1NFd5nX 首轮 + ses_fd6832d55ffe8TRerprCbeUMRg 修复后复核 `passes closure audit`；执行者未自我审计）
- [x] 结束证据存在于文件中（见 Closure 节）

## Deferred But Adjudicated

### drp/logistics 引用 pur 破坏（登记中间态）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 冻结序设计使然（rule 6 / D4 修订：登记内自身链破坏不触发停止）；Phase 2 已登记破坏模块清单 + javac 错误点 100% `_gen`/已登记边证明
- Successor Required: yes（M3.7 drp / M3.10 logistics 各自 plan 愈合，M4.1 兜底）

### md `findFirstByOrg(Long)` 签名遗留

- Classification: `watch-only residual`
- Why Not Blocking Closure: md 域已迁移后遗留的手写 Long 签名，本计划落登记例外语义桥（×2 转换点），mfg 先例同型
- Successor Required: yes（md 侧签名翻转另案或 M4.1 兜底裁决时退役）

### 存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M4.1 专项全量清扫已登记（M3.6 结束审计发现的模式盲区）
- Successor Required: yes（M4.1）

## Closure

Status Note: 执行完成（2026-08-22）。独立结束审计两轮：首轮（ses_fd68e0da0ffeW7A0SbW1NFd5nX，fresh session）13 项核证 11 PASS，判 `fails closure audit`——BLOCKER-1（登记册 554/704 行退役 note 含未转义引号致 fail-closed 不可解析）+ MAJOR-1（fin/inv 测试计数 console 497/235 vs XML 489/230 口径差未注明）；两项修复后复核（ses_fd6832d55ffe8TRerprCbeUMRg，fresh session）四点全 PASS，判 **`passes closure audit`**，无新问题。

Closure Audit Evidence:

- Auditor / Agent: ses_fd68e0da0ffeW7A0SbW1NFd5nX（首轮）+ ses_fd6832d55ffe8TRerprCbeUMRg（修复后复核），均 fresh session 独立子代理
- Evidence: 首轮逐项核证表（orm 118/118 归一化全等 + 7 链 jar 装仓 + 334/334 surefire + A2/B grep 清零 + 登记册 26 retired/12 note/090,091 active + 路线图/日志/快照纪律/反松弛全 PASS；BLOCKER/MAJOR 修复后复verify：scan 0 告警 321 待改 + 引号清零 + 双口径 XML 实测 489/230 与 console 497/235 双绿吻合 + plan/log/roadmap 三处口径注记落位）

Follow-up:

- 无（已确认缺陷不得出现在此处）
