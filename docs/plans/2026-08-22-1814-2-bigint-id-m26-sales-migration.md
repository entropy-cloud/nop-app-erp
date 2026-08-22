# 2026-08-22-1814-2-bigint-id-m26-sales-migration 主键/外键 string 化 M2.6：sales 域迁移（冻结序位次 16）

> Plan Status: active（2026-08-22：iteration 1 双审查 `needs revision` 修订完成，iteration 2 双审查 `acceptable as-is`/`accept` + 保护区域双批准落盘，见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M2.6（sales，冻结序位次 16）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 16（M2.6）
> Related: `docs/plans/2026-08-22-1814-1-bigint-id-m25-purchase-migration.md`（批内序 1，冻结总序先于本域执行；非编译硬前置）、`docs/plans/2026-08-22-1302-3-bigint-id-m31-manufacturing-migration.md`（bridge-main-086/087 + bridge-test-126/127 sal 半边登记来源）、`docs/plans/2026-08-22-1302-2-bigint-id-m23-quality-migration.md`（bridge-main-092..102 + bridge-test-128/129/131/132 登记来源）、`docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`（bridge-main-070 登记来源）、`docs/plans/2026-08-21-2025-2-bigint-id-m38-b2b-migration.md`（bridge-main-032 登记来源）、`docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md`（bridge-main-035..052 落桥来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **冻结序位置**：位次 16；精确前置 M2.3 + M2.2 + M2.1 + M1.1 + M1.2 + M3.6 全部 `done`（purchase 位次 15 非编译前置，但按冻结总序先行执行——本计划批内序 2）。
- **工具现状（2026-08-22 scan）**：`module-sales/model/app-erp-sales.orm.xml` **108 列 NEEDS FIX**（PK 28 + FK 80；实体构成 sal 90 + md stub 14 + ct stub 3 + prj stub 1，12 个 notGenCode stub 实体/18 stub 列——stub 实体全部属已迁移域，随本域翻转，权威源对齐 M1.1 先例；分解以 Phase 1 dry-run 为准），0 DEFERRED。
- **登记册义务（`tools/id-migration-registry.json5` + 审计文档 §6.16，Phase 1 强制全量复核）**：
  - A1 延后列：0；**A2 main 前向桥：0**（本域不引用任何晚域——登记册 §6.16 A 节明示「无」）；A3 test 前向桥：0。
  - **B main 桥接退役：23 条**：b2b 1（032 `UblInvoiceEdiProvider`——M3.8 语义级核证零 id 传递，预期零代码变更）+ contract 10（035/036/038/041/042/045/046/048/051/052，M3.6 落 ConvertHelper.toLong setter 桥 ×24 转换点跨 pur+sal 20 条 + RunAccrual/RebateAgreement eq 语义值桥 + resolve* Long 返回桥）+ finance 1（070 `DualSideConsistencyChecker`，M2.1 类型级核证零转换点）+ manufacturing 2（086/087 `DemandAggregator` orgId eq 语义值桥 + OrderLine 行值桥）+ quality 9（092 RecallTargetLocator toString ×2 / 093 findFirst eq("code") 核证 / 095/097 save map toLong ×2 / 098/099 customerId/deliveryId/materialId toLong ×4 / 100 salReturn.getId toString / 101 `ICrudBiz.get(String)` 平台签名核证 / 102 save map 值桥）。
  - **B retired-test sal 半边回收（候选招单，Phase 1 逐条对 registry retired note 重验）**：contract 110(sal 半边)/112（M3.6 落局部桥）、quality 128/129（M2.3 落 sal DELIVERY_PK Long + ConvertHelper.toLong 种子桥 + String.valueOf 断言桥）、quality 131/132（`TestStubErpSalDeliveryBiz`/`TestStubErpSalReturnBiz` 桩保持 Long 签名——本域翻转后桩签名随 IBiz String 化，M3.2 mock 先例）、manufacturing 126(sal 半边)/127（M3.1 落种子/断言局部桥 + 双向指针注释）。
  - C1 后向 main 6 条（backward-187 ct 1 文件 / 188 fin 6 / 189 inv 7 / 190 md 19 **含 sal-dao 3 手写文件**（`IErpSalReceiptBiz` + `ErpSalPriceList`/`ErpSalPriceListLine` 实体）/ 191 notify 1（`CreditLimitChecker`）/ 192 qa 2）；C2 后向 test 6 条（249 ct 1 / 250 fin 26 / 251 inv 10 / 252 md 34 / 253 notify 1 / 254 qa 1）——全部指向已迁移域。
  - 被引用面（未迁移域 → 登记中间态，不修复）：crm main 8 + test 2、drp main 1 + test 1、logistics main 1 + test 1（successor M3.4/M3.7/M3.10 + M4.1 兜底）。
- **已知 md 语义参数桥消费点**：`DeliveryStockMoveBuilder:50`/`ReturnStockMoveBuilder:59` 调 `IErpMdAcctSchemaBiz.findFirstByOrg(Long)`（md 侧手写 Long 签名遗留，实测仍 Long）——本域 String 化后须落 `ConvertHelper.toLong(orgId)` 语义桥 + 退役条件注释（mfg `MaterialIssueStockMoveBuilder:54-55` 先例，登记例外；mfg Phase 4 已将 sal 侧同签名消费面 successor 登记至本计划）。
- **page.yaml**：sal-web 手写面 `:Long` 零命中（2026-08-22 grep 实证；Phase 4 复核维持）。
- **代码与测试规模**：sal-service main 手写修复面以 C1 全清单为准（§6.16 计 service 21 文件：processor 8 + entity 8 + support 2 + dashboard 1 + posting 1 + spi 1）+ sal-dao 3 手写文件；test 53 个 `Test*.java`；快照 `_cases/` 现值 **3232** 文件（重录基线）。
- **早域复跑基线（B 义务验证）**：b2b 80/80、ct 168/168、fin 497/497、mfg 289/289、qa 182/182（五个域）。
- **owner doc**：`docs/design/sales/` grep 复核零 Long/BIGINT id 陈述（2026-08-22 实证；Phase 4 复核确认）。

## Goals

- 108 列 `stdDataType long→string` 落源（stdSqlType 保持 BIGINT，DDL 零变化），三重证明（新鲜度门控 + git diff + 工具重扫 sal 段 0 NEEDS FIX）。
- sal 7 模块链 no-am main 绿 + 域级测试全绿（`mvn test -pl module-sales/erp-sal-service,module-sales/erp-sal-web`）。
- B main 退役 23 条兑付（五早域转换点/注释移除 + 7 链重建绿 + grep 桥残留清零 + 测试基线复跑维持）；retired-test sal 半边回收 + qa 桩 String 化 ×2。
- 快照 RECORDING→CHECKING 每域重录（force-save-output 系统属性模式，mfg 先例）。
- 语义陷阱 grep 门控清零 + page.yaml 复核维持零 + owner doc 注记 + 登记册状态更新 + 路线图 M2.6 → done + 日志。

## Non-Goals

- 不迁移 crm/drp/logistics（冻结序位次 17-19，successor 各自 plan）；不修复 crm/drp/logistics 对 sal 的引用破坏（登记中间态）。
- 不改 `stdSqlType`/DDL/CSV 种子/序列号引擎；不动 `delVersion`/`signedBy` 等非 PK/FK BIGINT 列（孤儿操作人列规则 4 保持 long）。
- 不跑全量构建/全量测试/E2E（M4.1 专属）；不修 M4.1 登记的存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）。
- 不翻转 md 侧 `findFirstByOrg(Long)` 签名（同 purchase plan Deferred 登记，另案/M4.1 兜底）。

## Task Route

- Type: `implementation-only change`（orm 模型变更 → 增量重生成 → 编译器驱动修复，路线图 M1-M3 标准结构）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（标准结构 + 规则 1-8 + 横切 §5 保护区域 design 证据链）、`docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md` §6.16 + `tools/id-migration-registry.json5`（消费协议）、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B、`docs/design/domain-design-guidelines.md` §16A、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`（M0 裁决与冻结序结论）、`docs/design/sales/`（业务 owner doc，Phase 4 注记对象）
- Skill Selection Basis: 域迁移 plan 预期技能 = `nop-backend-dev`（BizModel/IBiz 手写修复 + 跨实体调用规则）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；orm 回写机制走 M0.1 裁定工具链，不经技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。工具链：`node tools/check-bigint-id-types.mjs dry-run` + `node tools/verify-id-fix-copy-diff.mjs module-sales` + `node tools/scan-cross-domain-id-coupling.mjs`；回写一律走「时点 dry-run + 新鲜度门控」三步，禁止盲 cp/apply 模式。
- 硬前置 = 最后全绿基线 commit 的全量 install + 每个已完成域链 install 已在本地 Maven 仓库（D3 修订口径「每个已完成域链」；purchase 位次 15 按冻结总序先行执行，非本域编译前置）。
- 回滚策略（mfg/qa 先例）：orm 落源后回滚 = `git revert` orm 变更 + 7 模块链增量重生成；Phase 2/3 完成后回滚需先 revert 五早域桥接退役与测试代码变更，再 revert 本域 orm + 重生成。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-sales/model/app-erp-sales.orm.xml`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 精确前置 M2.3 + M2.2 + M2.1 + M1.1 + M1.2 + M3.6 全部 done（roadmap 规则 1 readiness 口径）；批内序 1 purchase（位次 15）按冻结总序先行，非本域编译硬前置

- [ ] Proof: 登记册全量消费——`registry §6.16 + json5` 逐条核对与 Baseline 一致（A1=0 / A2=0 / B main 23 / retired-test sal 半边候选 / C1 187-192 / C2 249-254 / 被引用面 crm+drp+logistics）；差异即补登落册。
      - Skill: none
- [ ] Proof: FQN 复扫（b2b A3' 盲区先例：java 内联 FQN 非 import 行 + `*.beans.xml` ioc:type 双口径）——sal 引用面补登核对，预期仅命中已登记条目。
      - Skill: none
- [ ] Proof: 双独立子 agent 批准（治理 + 技术，fresh session）落盘 Draft Review Record（保护区域 `model/*.orm.xml` 要求）。
      - Skill: none
- [ ] Fix: orm 落源三步——① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-sales` 新鲜度门控（108 行 stdDataType-only、零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核（stdSqlType 全保持 BIGINT、delVersion/signedBy 零变化、stub 列与权威源一致）。
      - Skill: none

Exit Criteria:

- [ ] 工具重扫 sal 段 0 NEEDS FIX / 0 DEFERRED，三重证明落盘。
- [ ] 登记册消费零未解释差异（或补登已落册 + fail-closed 解析验证通过）。

### Phase 2 - 增量重生成 + 主代码编译修复 + B main 退役 + 早域链重建

Status: planned
Targets: `module-sales/erp-sal-{codegen,dao,meta,service,web,app,api}`、五早域桥接点文件
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision | Proof`
- Prereqs: Phase 1 完成

- [ ] Fix: 7 模块链 no-am 重生成构建 main 绿（`-pl` 显式列表 + `-Dmaven.test.skip=true`）；编译器驱动修复手写面（**sal-dao 3 手写文件翻转**（`IErpSalReceiptBiz` id 参数 + `ErpSalPriceList`/`ErpSalPriceListLine` 实体，fin/ast dao 手写先例对齐）+ service C1 全清单 21 文件为定位面；sal-dao 对 md `SettlementAllocation`/`IDateRange` 3 行非实体 import（IDateRange ×2 + SettlementAllocation ×1）为类型级用法，随 md String jar 自愈核证）。
      - Skill: `nop-backend-dev`
- [ ] Fix: md 语义参数桥 ×2（`DeliveryStockMoveBuilder`/`ReturnStockMoveBuilder` `findFirstByOrg(ConvertHelper.toLong(orgId))` + 退役条件注释，mfg `MaterialIssueStockMoveBuilder:54-55` 先例，登记例外）。
      - Skill: `nop-backend-dev`
- [ ] Fix: B main 23 条退役兑付——五早域侧转换点/桥注释/import 移除 + String 直传（b2b 032 预期零代码变更核证 / ct 035..052 十条 setter 桥 ×24 + resolve* 桥 / fin 070 类型级核证 / mfg 086/087 语义值桥 + 行值桥 / qa 092..102 九条含 eq 过滤值桥），各早域 grep `bridge-main-0xx` 引用清零。
      - Skill: `nop-backend-dev`
- [ ] Proof: 五早域 7 模块链 no-am 重建 BUILD SUCCESS（b2b/ct/fin/mfg/qa）。
      - Skill: none
- [ ] Decision: crm/drp/logistics 引用破坏登记中间态——破坏模块清单 + successor 指针（M3.4/M3.7/M3.10/M4.1）+ javac 错误点 100% 位于 `_gen` 或已登记前向边的逐模块证明（rule 6 / D4 联动）。
      - Skill: none

Exit Criteria:

- [ ] sal 7 链 main 绿；五早域 7 链重建绿。
- [ ] 桥接例外 grep 仅剩登记例外（findFirstByOrg ×2）。

### Phase 3 - 测试修复 + retired 半边回收 + 快照重录 + 域级测试 + 早域复跑

Status: planned
Targets: `module-sales/erp-sal-service/src/test`、五早域 test 桥文件
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成

- [ ] Fix: 测试修复（53 个 `Test*.java` 编译器驱动清零 + 语义陷阱：id 序比较陷阱 → `ConvertHelper.toLong` 数值序（contract idOrder 先例）、装箱 `==` → `.equals()`、算术派生 id 数值保真、种子常量 String 字面量数字保真、`orm_propValueByName("id", String)` 形态）。
      - Skill: `nop-testing`
- [ ] Fix: retired-test sal 半边回收（ct 110 sal 半边/112 局部桥移除 / qa 128/129 sal DELIVERY_PK Long + 种子/断言桥移除 / mfg 126 sal 半边 + 127 局部桥移除 + 双向指针注释删除）+ qa `TestStubErpSalDeliveryBiz`/`TestStubErpSalReturnBiz` 桩签名 String 化（M3.2 mock 先例）；各早域 test-compile 绿。
      - Skill: `nop-testing`
- [ ] Proof: 快照重录 RECORDING→CHECKING（`-Dnop.autotest.force-save-output=true` 全局重录 + grep 零注解残留；`_cases/` 3232 基线 → String 形态落盘；非确定性单元格 `*` 通配（hr/mfg 先例）；逐案审核 id String 实证 + 拒绝「断言式」类误录（cs 81 目录超录回退先例）。
      - Skill: `nop-testing`
- [ ] Proof: 域级测试 `mvn test -pl module-sales/erp-sal-service,module-sales/erp-sal-web` 全绿（web 0 tests 治理排除，successor M4.1）；平台 IoC 回归 self-wait 处置双分支证据——未复现则记录零 delta 证据（mfg M3.1 先例），复现则 fin 修正版先例 delta（`x:extends="super"`）落位（hr 第二环环境性回归 bug 文档参照）。
      - Skill: `nop-testing`
- [ ] Proof: 早域复跑五域基线维持（b2b 80/80 + ct 168/168 + fin 497/497 + mfg 289/289 + qa 182/182；快照形态漂移就地 String 化 + `*` 通配按先例处置，逻辑破坏为零）。
      - Skill: `nop-testing`

Exit Criteria:

- [ ] sal 域级测试全绿 + 五早域基线维持全绿。
- [ ] retired sal 半边 grep 清零（`bridge-test-110|112|126|127|128|129|131|132` 早域侧引用清零或形态核证）。

### Phase 4 - 语义陷阱 grep 门控 + page.yaml 复核 + 收尾登记

Status: planned
Targets: sal-service main、sal-web 手写面、`docs/design/sales/`、登记册、路线图、日志
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 3 完成

- [ ] Fix: 语义陷阱 grep 门控清零（路线图横切 §3 清单：`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/装箱 `==`/id 序比较；合法非 id 项逐条注明；sql-lib 零存在注明）。
      - Skill: none
- [ ] Proof: page.yaml `:Long` 复核维持零命中 + 手写 view 零被动变更核证（git status 非 `_gen` 变更预期为零；若发现新 `:Long` 就地 String 化 + BizModel 签名一致性核证，mfg CRP/BOM 同型）。
      - Skill: `nop-backend-dev`
- [ ] Fix: owner doc 注记——`docs/design/sales/` grep 复核 Long id 陈述（2026-08-22 基线零命中，Phase 4 复核确认；命中即就地注记引用本计划）。
      - Skill: none
- [ ] Add: 登记册状态更新（B main 23 条 → retired 附兑付 note；retired-test sal 半边 note 补指针；fail-closed 解析验证）+ 路线图 M2.6 → done（M2/M3 表位次 16 证据摘要 + 头部「最后更新」续链 + 位次 17 解锁）+ 日志 `docs/logs/2026/{执行当日}.md`（rule 8 日期口径，含验证状态 + B 义务兑付 + 五早域复跑基线）。
      - Skill: none

Exit Criteria:

- [ ] grep 门控清零（登记例外除外）+ page.yaml `:Long` 维持零。
- [ ] 登记册/路线图/日志三处一致落盘。

## Draft Review Record

- Independent draft review iteration 1（技术/执行契约视角，fresh session，ses_fd70111fcffeePLNMGeuy9002z）: `needs revision`——1 MAJOR（109 列口径错误，实仓 scan = 108 = PK 28 + FK 80，四处落点已全部修正）+ 5 MINOR（notGenCode 15→12 实体/18 列、test 53 类、C1 分解 21 文件、sal-dao import 3 行、×24 措辞归属 pur+sal 合计）已全部就地修正；其余事实断言（A2=0/7 链/dao 3 手写文件/findFirstByOrg/page.yaml 零命中/_cases 3232/早域基线/qa 桩/登记册 23 条 retired-test 半边）逐项实仓核证全对。
- Independent draft review iteration 1（治理/规范视角，fresh session，ses_fd700f38affeYN5n0ZLQ9uXYrs）: `needs revision`——2 MAJOR（Infra 缺回滚策略 → 已补 mfg/qa 先例回滚行；Task Route design 证据链不完整 → 已补 domain-design-guidelines §16A + M0.1 freeze audit + docs/design/sales/ 全链）+ 5 MINOR（Item Types 对齐 Phase 1 `Fix|Proof`/Phase 2 `Fix|Decision|Proof`、Phase 1 phase-level Skill→none、D3「已完成域链」措辞 + Prereqs 精确前置口径、owner doc 入 Task Route、批内交叉提示）已全部就地修正。
- Independent draft review iteration 2（技术/执行契约视角，fresh session，ses_fd6f7059cffehxW3w2mwWeyQ57）: `acceptable as-is` + 保护区域技术侧 `approve`——iteration-1 全部发现逐项验证 RESOLVED（108 = PK 28 + FK 80 实仓复测、notGenCode 12 实体/18 列、53 测试类、C1 21+3、3 行 import、×24 措辞、回滚行、证据链、Item Types/Skill、D3 口径）；附加抽查 B main 23/retired 半边/C2/被引用面/findFirstByOrg ×2/page.yaml 零命中/_cases 3232/早域基线全对；0 BLOCKER / 0 MAJOR / 0 MINOR。
- Independent draft review iteration 2（治理/规范视角，fresh session，ses_fd6f6dd53ffeaOtPO04E2eiwtS）: `accept` + 保护区域治理侧 `approve`——iteration-1 全部发现逐项验证已解决；反松弛零命中、Deferred 合法、Closure Gates 与 108 口径一致、状态自洽、模板全合规；0 BLOCKER / 0 MAJOR。
- **共识达成（2026-08-22）**：iteration 2 双审查者 `acceptable as-is`/`accept` + 双 `approve`（0 BLOCKER / 0 MAJOR），保护区域双独立子 agent 批准已落盘（治理 ses_fd6f6dd53ffeaOtPO04E2eiwtS + 技术 ses_fd6f7059cffehxW3w2mwWeyQ57），转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（108 列落源 + 7 链绿 + B/C 兑付 + 快照重录 + 测试绿）
- [ ] 相关文档对齐（登记册 + 路线图 + owner doc + 日志）
- [ ] 已运行验证：`mvn clean install -pl module-sales/erp-sal-{codegen,dao,meta,service,web,app,api} -DskipTests`（no-am）+ `mvn test -pl module-sales/erp-sal-service,module-sales/erp-sal-web` + 五早域复跑基线
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（保护区域双批准落盘）
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### crm/drp/logistics 引用 sal 破坏（登记中间态）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 冻结序设计使然（rule 6 / D4 修订：登记内自身链破坏不触发停止）；Phase 2 已登记破坏模块清单 + javac 错误点 100% `_gen`/已登记边证明
- Successor Required: yes（M3.4 crm / M3.7 drp / M3.10 logistics 各自 plan 愈合，M4.1 兜底）

### md `findFirstByOrg(Long)` 签名遗留

- Classification: `watch-only residual`
- Why Not Blocking Closure: md 域已迁移后遗留的手写 Long 签名，本计划落登记例外语义桥（×2 转换点），mfg 先例同型
- Successor Required: yes（md 侧签名翻转另案或 M4.1 兜底裁决时退役）

### 存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M4.1 专项全量清扫已登记（M3.6 结束审计发现的模式盲区）
- Successor Required: yes（M4.1）

## Closure

Status Note: 待执行完成并经独立结束审计后填写。

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（已确认缺陷不得出现在此处）
