# 2026-08-22-2311-2-bigint-id-m37-drp-migration 主键/外键 string 化 M3.7：drp 域迁移（冻结序位次 18）

> Plan Status: completed（2026-08-23：四 Phase 单次运行全部完成——Phase 1 登记册全量消费零差异 + FQN 复扫零补登 + orm 55 列落源三重证明；Phase 2 drp 7 链重生成 main 绿（dao 58 `_gen` 自愈 + service 118 错一轮清零）+ B main 090/091 退役兑付 + pur 7 链重建绿 + logistics 下游零破坏核证；Phase 3 测试 14 类三轮 javac 清零 + 快照重录 518→1155 + drp 97/97 绿 + pur 334/334 复跑维持；Phase 4 grep 门控清零 + page.yaml 1 处 String 化 + owner doc 零命中维持 + 登记册/路线图/日志三处落盘。**结束审计已由独立子代理 CLOSURE_VERIFY 通过**（2026-08-23，独立会话非执行者；live repo 现场核证 8 项抽检全对，证据见 Closure 节））
> Mission: id-string-migration
> Work Item: M3.7（drp，冻结序位次 18）
> Last Reviewed: 2026-08-23
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 18（M3.7）
> Related: `docs/plans/2026-08-22-2311-1-bigint-id-m34-crm-migration.md`（批内序 1，冻结总序先于本域执行；非编译硬前置）、`docs/plans/2026-08-22-1814-1-bigint-id-m25-purchase-migration.md`（bridge-main-090/091 落桥来源 + drp 破坏登记来源）、`docs/plans/2026-08-22-1814-2-bigint-id-m26-sales-migration.md`（backward-151/210 登记来源 + drp-service 终态复测来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **冻结序位置**：位次 18；精确前置 M2.5 + M2.6 + M2.7 + M2.3 + M2.2 + M2.1 + M2.4 + M1.1 + M1.2 + M3.6 全部 `done`（drp 为冻结序前置最广域，10 前置；crm 位次 17 非编译前置，但按冻结总序先行执行——本计划批内序 2）。
- **工具现状（2026-08-22 scan）**：`module-drp/model/app-erp-drp.orm.xml` **55 列 NEEDS FIX**（PK 17 + FK 38；其中 notGenCode stub 6 列——inv 1（`ErpInvStockMove`，M2.2 登记「drp orm 内 inv stub 列为位次 18 域自有列，successor M3.7」随本域翻转）+ md 5（`ErpMdLocation`/`ErpMdMaterial`/`ErpMdOrganization`/`ErpMdPartner`/`ErpMdWarehouse`），stub 实体全部属已迁移域（md M1.1/inv M2.2），随本域翻转、与权威源对齐（M1.1 先例；分解以 Phase 1 dry-run 为准）），0 DEFERRED。
- **登记册义务（`tools/id-migration-registry.json5` + 审计文档 §6.18，Phase 1 强制全量复核）**：
  - A1 延后列：0；**A2 main 前向桥：0；A3 test 前向桥：0**（本域不引用任何晚域——登记册 §6.18 A 节明示「无」，仅 logistics 晚于本域且登记册零 refDomain=drp 的 logistics 条目）。
  - **B main 桥接退役：2 条**（早域 purchase）：bridge-main-090/091（`ErpPurReceiveProcessor` ConvertHelper.toLong 桥 + drpMaterialIds 循环桥——M2.5 已落桥 + 双向注释，pur 334/334 绿；本域翻转 IBiz 参数签名后桥接点/注释移除 + String 直传）。
  - **B retired-test drp 半边回收：0**（登记册零 refDomain=drp 的 bridge-test 条目——实证复查于 Phase 1）。
  - C1 后向 main 6 条（backward-146 inv 5 文件 / 147 mfg 1 文件 / 148 md 3 文件 / 149 pur 3 文件 / 150 qa 2 文件 / 151 sal 1 文件；去重后 6 个手写文件：`DrpDemandAggregator`/`DrpReleaseService`/`ErpDrpCrossDockStagingTimeoutJob`/`ErpInvDrpCrossDockProcessor`/`ErpInvDrpLeadTimeProcessor`/`SafetyStockEngine`）；C2 后向 test 6 条（backward-205 inv 9 文件 / 206 mfg 1 / 207 md 9 / 208 pur 3 / 209 qa 2 / 210 sal 1；去重后 9 个测试类）——全部指向已迁移域。
  - 被引用面（未迁移域 → 登记中间态）：**零**（logistics orm 无 drp stub、登记册除 090/091 main 桥（B 义务）外零 logistics 的 refDomain=drp service/test 条目——本域翻转不新增任何下游破坏）。
- **继承中间态（M2.6 Phase 2 实测登记，2026-08-22；M2.5 早期 78 错/5 文件口径已被 M2.6 翻转后复测取代）**：drp-dao 58 javac 错 100% `_gen`（md 关系胶水对称耦合，D3 已登记）——本域翻转 + 重生成自愈；drp-service 错误仅 `DrpDemandAggregator` + `DrpReleaseService` 两文件且 import 面仅 inv/pur（零 sal import）——由 Phase 2 编译器驱动手写修复愈合（非重生成自愈）。
- **findFirstByOrg 消费点：零**（2026-08-22 grep 实证；无 md 语义参数桥义务）。
- **page.yaml**：drp-web 手写面 `:Long` **1 处**（`dashboard/net-requirement:38` `$pid`）——Phase 4 就地 String 化（mfg CRP / M2.6 three-way-match 先例）+ YAML 良构校验 + drp-web 重建绿。
- **代码与测试规模**：drp-service main 手写修复面以 C1 全清单 6 文件为定位面（编译器可发现更多本域 Long id 面：drp engine/scenario/plan 族 BizModel 与 IBiz）；test 14 个 `Test*.java`（C2 登记 9 个）；快照 `_cases/` 现值 **518** 文件（重录基线）。
- **早域复跑基线（B 义务验证）**：pur 334/334（B main 2 条）。
- **owner doc**：`docs/design/drp/` grep 复核零 Long id/BIGINT 陈述（2026-08-22 实证；Phase 4 复核确认）。

## Goals

- 55 列 `stdDataType long→string` 落源（stdSqlType 保持 BIGINT，DDL 零变化），三重证明（新鲜度门控 + git diff + 工具重扫 drp 段 0 NEEDS FIX）。
- drp 7 模块链 no-am main 绿 + 域级测试全绿（`mvn test -pl module-drp/erp-drp-service,module-drp/erp-drp-web`）；继承中间态（drp-dao `_gen` + drp-service 两文件前向边破坏）全数自愈。
- B main 退役 2 条兑付（pur 侧桥接点/注释移除 + String 直传 + pur 7 链重建绿 + grep 桥残留清零 + pur 测试基线复跑维持）。
- 快照 RECORDING→CHECKING 每域重录（force-save-output 系统属性模式，mfg/sal 先例）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 1 处就地 String 化 + owner doc 复核 + 登记册状态更新 + 路线图 M3.7 → done + 日志。

## Non-Goals

- 不迁移 logistics（冻结序位次 19，successor plan）；本域翻转预期零下游破坏（登记册实证），若发现未登记 logistics 破坏按 rule-6/D4 停止回报。
- 不改 `stdSqlType`/DDL/CSV 种子/序列号引擎；不动 `delVersion` 等非 PK/FK BIGINT 列（孤儿操作人列规则 4 保持 long）。
- 不跑全量构建/全量测试/E2E（M4.1 专属）；不修 M4.1 登记的存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）。
- 不翻转 md 侧 `findFirstByOrg(Long)` 签名（本域零消费点；pur/sal 先例登记例外不适用）。

## Task Route

- Type: `implementation-only change`（orm 模型变更 → 增量重生成 → 编译器驱动修复，路线图 M1-M3 标准结构）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（标准结构 + 规则 1-8 + 横切 §5 保护区域 design 证据链）、`docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md` §6.18 + `tools/id-migration-registry.json5`（消费协议）、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B、`docs/design/domain-design-guidelines.md` §16A、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`（M0 裁决与冻结序结论）、`docs/design/drp/`（业务 owner doc，Phase 4 复核对象）
- Skill Selection Basis: 域迁移 plan 预期技能 = `nop-backend-dev`（BizModel/IBiz 手写修复 + 跨实体调用规则）+ `nop-testing`（快照重录 RECORDING→CHECKING 流程）；orm 回写机制走 M0.1 裁定工具链，不经技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。工具链：`node tools/check-bigint-id-types.mjs dry-run` + `node tools/verify-id-fix-copy-diff.mjs module-drp` + `node tools/scan-cross-domain-id-coupling.mjs`；回写一律走「时点 dry-run + 新鲜度门控」三步，禁止盲 cp/apply 模式。
- 硬前置 = 最后全绿基线 commit 的全量 install + 每个已完成域链 install 已在本地 Maven 仓库（D3 修订口径「每个已完成域链」；drp 位次 18 前的 17 域链全部 install）。
- 回滚策略（mfg/qa/sal 先例）：orm 落源后回滚 = `git revert` orm 变更 + 7 模块链增量重生成；Phase 2/3 完成后回滚需先 revert pur 桥接退役与测试代码变更，再 revert 本域 orm + 重生成。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: completed
Targets: `module-drp/model/app-erp-drp.orm.xml`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 精确前置 M2.5 + M2.6 + M2.7 + M2.3 + M2.2 + M2.1 + M2.4 + M1.1 + M1.2 + M3.6 全部 done（roadmap 规则 1 readiness 口径）；批内序 2（crm 位次 17 按冻结总序先行，非本域编译硬前置）

- [x] Proof: 登记册全量消费——`registry §6.18 + json5` 逐条核对与 Baseline 一致（A1=0 / A2=0 / A3=0 / B main 2 / retired-test drp 半边零实证 / C1 146-151 / C2 205-210 / 被引用面零）；差异即补登落册。
      - Skill: none
      - 执行证据（2026-08-23）：json5 逐条 grep 复核——domain=drp 的 orm-deferral 0 条、service-bridge main/test 0 条；refDomain=drp 恰 2 条 = bridge-main-090（`IErpInvDrpCrossDockBiz.markReceivedFromPurchase`，pur `ErpPurReceiveProcessor:324`，active，retireOwner=M3.7）+ bridge-main-091（`IErpInvDrpLeadTimeRecordBiz.recordFromPurchaseReceive`，:378，active，retireOwner=M3.7）；domain=drp backward-pointer main 6 条 = 146（inv 5 文件）/147（mfg 1）/148（md 3）/149（pur 3）/150（qa 2）/151（sal 1）+ test 6 条 = 205（inv 9 文件）/206（mfg 1）/207（md 9）/208（pur 3）/209（qa 2）/210（sal 1）——与 Baseline 全等，零未解释差异；retired-test refDomain=drp 零条实证。
- [x] Proof: FQN 复扫（b2b A3' 盲区先例：java 内联 FQN 非 import 行 + `*.beans.xml` ioc:type 双口径）——drp 引用面与被引用面双向补登核对，预期仅命中已登记条目。
      - Skill: none
      - 执行证据（2026-08-23）：`rg 'app\.erp\.drp\.'` 全仓（排除 module-drp）java 双口径命中恰 1 文件 = `ErpPurReceiveProcessor`（已登记 090/091 的 import/字段/调用点）；`ErpInvDrp|ErpDrp` 附加命中 `ErpPurReceiveApproveProcessor:53` 为注释行非代码引用；beans 命中零；被引用面 `rg 'ErpInvDrp|ErpDrp|app\.erp\.drp\.' module-logistics/` 零命中 + logistics orm 零 drp stub——零补登。
- [x] Proof: 双独立子 agent 批准（治理 + 技术，fresh session）落盘 Draft Review Record（保护区域 `model/*.orm.xml` 要求）。
      - Skill: none
      - 执行证据：已落盘本文件 Draft Review Record（技术侧 ses_fd5f6fea7ffeTT78V27peu1PnH iteration 1 `acceptable as-is` + `approve` + 治理侧 ses_fd5f6e3a4ffeTJR34PhIaf8ikr iteration 1 `accept` + `approve`，2026-08-22 共识达成），转 `active` 前完成，无需重录（crm M3.4 先例）。
- [x] Fix: orm 落源三步——① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-drp` 新鲜度门控（55 行 stdDataType-only、零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核（stdSqlType 全保持 BIGINT、非 PK/FK 列零变化、inv stub 1 + md stub 5 列与权威源一致）。
      - Skill: none
      - 执行证据（2026-08-23）：① dry-run 汇总 87 列/2 文件（drp 55 + logistics 32）；② 门控输出「module-drp 变更行 55 / 非法差异行 0 / 门控通过」；③ cp 落源 + git diff 55 insertions/55 deletions——删除行全含 `stdDataType="long"`、新增行全含 `stdDataType="string"`（非 std 差异行双侧 grep exit=1），PK id ×17 + FK ×38（orgId 9/materialId 7/warehouseId 5/supplierId 2/scenarioId 2/单列 12）；stdSqlType 全保持 BIGINT；stub 6 列（md `ErpMdOrganization`/`ErpMdWarehouse`/`ErpMdLocation`/`ErpMdPartner`/`ErpMdMaterial` id + inv `ErpInvStockMove` id）与 md/inv 权威源 string 形态逐一比对一致。

Exit Criteria:

- [x] 工具重扫 drp 段 0 NEEDS FIX / 0 DEFERRED（总待改 213 − 126 crm（批内序 1 先行前提）− 55 本域 = logistics 32 余量），三重证明落盘。
      - 执行证据（2026-08-23）：`scan` 汇总「实际修改 stdDataType 的列: 32（不含延后列）/ DEFERRED: 0」，NEEDS FIX 全部落于 module-logistics（log 27 + logistics orm 内 md stub 5 = 位次 19 域自有列，successor M3.10），drp 段全部 `ok` 零命中。
- [x] 登记册消费零未解释差异。

### Phase 2 - 增量重生成 + 主代码编译修复 + B main 退役 + 早域链重建

Status: completed（2026-08-23：drp 7 模块链 no-am `mvn clean install -Dmaven.test.skip=true` BUILD SUCCESS——drp-dao 58 `_gen` 错误随重生成自愈实证（dao/meta 先行模块 SUCCESS）；drp-service 首轮 118 编译错/9 文件编译器驱动清零 = dao 手写面 8 文件 IBiz/值对象翻转（`IErpDrpPlanBiz`/`IErpDrpLineBiz`/`IErpDrpScenarioBiz`/`IErpInvDrpCrossDockBiz`/`IErpInvDrpLeadTimeRecordBiz`/`IErpInvDrpSafetyStockCalcBiz` + `LeadTimeStatsBean`/`DrpSimulationDiffResult` 值对象——M0 审计附录 C 语义 FK 面全数显式翻转，含 090/091 退役目标 `markReceivedFromPurchase`/`recordFromPurchaseReceive` 签名）+ service main 28 文件 Long→String（C1 定位面 6 文件 + engine/scenario/simulation/processor/BizModel 扩展面 + `ErpDrpLineBizModel` String.valueOf 残余清理；语义面复核：`DrpSimulationVersionComparator` Objects.equals/字符串 key 安全、CrossDockProcessor 排序按 deliveryDate 非 id、LeadTimeProcessor Collections.sort 为 BigDecimal median 非 id）；B main 2 条退役兑付：pur `ErpPurReceiveProcessor` ConvertHelper.toLong 桥 ×2 + drpMaterialIds 循环桥 ×2 移除 + String 直传 + 双向 bridge 注释 ×2 删除 + ConvertHelper 引用清零（原为内联 FQN 无 import）+ pur grep `bridge-main-09[01]` 清零；pur 7 模块链 no-am 重建 BUILD SUCCESS；下游核证 `rg 'app\.erp\.drp\.' module-logistics/` 零命中 + logistics orm 零 drp stub（与 Phase 1 证据源一致，零新增破坏））

Targets: `module-drp/erp-drp-{codegen,dao,meta,service,web,app,api}`、pur 桥接点文件
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 1 完成

- [x] Fix: 7 模块链 no-am 重生成构建 main 绿（`-pl` 显式列表 + `-Dmaven.test.skip=true`）；编译器驱动修复手写面（drp-dao IBiz/值对象 id 参数族 + service C1 全清单 6 文件为定位面：`DrpDemandAggregator`/`DrpReleaseService`/`ErpDrpCrossDockStagingTimeoutJob`/`ErpInvDrpCrossDockProcessor`/`ErpInvDrpLeadTimeProcessor`/`SafetyStockEngine`；drp-dao 58 `_gen` 错误随重生成自愈复核 + drp-service 两文件继承破坏随编译器驱动手写修复愈合复核）。
      - Skill: `nop-backend-dev`
      - 执行证据（2026-08-23）：dao 首轮即过（`_gen` 自愈），service 118 错/9 文件一轮清零（批量翻转前置核证：全仓 `Long` 出现 100% id 语境——rg 非 id 过滤仅 4 命中全为 materialId/scenarioId 键容器与 preferredSourceWarehouseId；零 `toLong`/`parseLong`/`Long` 复合类型名）；drp 7 链 reactor 7/7 SUCCESS。
- [x] Fix: B main 2 条退役兑付——pur 侧 `ErpPurReceiveProcessor` ConvertHelper.toLong 桥 + drpMaterialIds 循环桥移除、String 直传 + 双向 bridge 注释删除 + ConvertHelper import 清零复核，pur grep `bridge-main-09[01]` 引用清零。
      - Skill: `nop-backend-dev`
      - 执行证据（2026-08-23）：090（markCrossDockReceived）+ 091（recordLeadTime）两处 ConvertHelper.toLong ×3 + 循环桥 ×2 移除改 String 直传 + bridge 注释 ×2 删除；`rg 'ConvertHelper'` 该文件零命中（内联 FQN 形态零 import 面）+ `rg 'bridge-main-09[01]' module-purchase/` 零命中。
- [x] Proof: pur 7 模块链 no-am 重建 BUILD SUCCESS。
      - Skill: none
      - 执行证据（2026-08-23）：`mvn clean install -pl module-purchase/erp-pur-{codegen,dao,meta,service,web,app,api} -Dmaven.test.skip=true` BUILD SUCCESS（7/7 模块绿）。

Exit Criteria:

- [x] drp 7 链 main 绿；pur 7 链重建绿。
      - 执行证据（2026-08-23）：两链 reactor summary 7/7 各绿。
- [x] 下游破坏核证：logistics 链零新增破坏（证据源 = Phase 1 FQN 双向复扫被引用面零 + logistics orm 无 drp stub + `rg 'app\.erp\.drp\.' module-logistics` 零命中机械复核；出现未登记破坏即 rule-6 停止回报）。
      - 执行证据（2026-08-23）：`rg 'app\.erp\.drp\.' module-logistics/` exit=1 零命中 + `rg 'ErpInvDrp|ErpDrp' module-logistics/model/app-erp-logistics.orm.xml` exit=1——与登记册「被引用面零」一致，零新增破坏。

### Phase 3 - 测试修复 + 快照重录 + 域级测试 + 早域复跑

Status: completed（2026-08-23：14 Test*.java 编译器驱动三轮清零（首轮 javac 截断 200 错/6 文件 + 次轮 118 错/3 文件暴露（Engine/Simulation/WiringRegression）+ 收尾单点 1 错）——种子常量 String 字面量数字保真（`static final Long X = 6201L` → `"6201"` 全族）+ 算术派生 id 数值保真（`String.valueOf(NNNNL + Long.parseLong(id))` ×17 处 + hash 派生 `String.valueOf(6001L + Math.abs(code.hashCode() % 600))` ×6 + SEQ 派生 ×2 + `headId * 1000 + 10` ×2）+ sed 副作用数值保真修复 1 处（`orderId + 100L` 误拼接 → `String.valueOf(Long.parseLong(orderId) + 100L)`）+ `String.valueOf(lineId)` 残余清理；快照重录 force-save-output 系统属性模式：97 测试 79 snapshot-finished 零真实失败初录 + CHECKING 97/97 绿 ×2；`_cases/` 518 → 1155 = 637 首录 + 58 内容 diff，json5 id String 实证（`"id": "4"`），注解零残留（RECORDING/forceSaveOutput/saveOutput grep 空），零「断言式」类误录（首录类 grep 断言式标记零命中），非确定性单元格零需求（CHECKING 一次全绿无 `*` 通配）；域级测试 `mvn test -pl module-drp/erp-drp-service,module-drp/erp-drp-web` 97/97 + web 0 tests（治理排除 successor M4.1）双绿 ×2；平台 IoC 回归 self-wait 未复现零 delta（test resources git status 零变更）；早域复跑 pur **334/334** 基线维持零干预（B 义务验证，快照零形态漂移））

Targets: `module-drp/erp-drp-service/src/test`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 完成

- [x] Fix: 测试修复（14 个 `Test*.java` 编译器驱动清零 + 语义陷阱：id 序比较 → `ConvertHelper.toLong` 数值序（contract idOrder 先例）、装箱 `==` → `.equals()`、算术派生 id 数值保真、种子常量 String 字面量数字保真、`orm_propValueByName("id", String)` 形态）。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：三轮 javac 清零（200 错/6 文件 → 118 错/3 文件 → 1 错）后 BUILD SUCCESS；算术派生 ×17 + hash 派生 ×6 + SEQ ×2 + 乘法派生 ×2 全部 `String.valueOf(long 算术)` 数值保真形态；`orm_propValueByName("id", String)` 形态全落；id 序比较/装箱 == 语义陷阱 grep 零命中（测试侧 Objects.equals/字符串 key 安全）。
- [x] Proof: 快照重录 RECORDING→CHECKING（`-Dnop.autotest.force-save-output=true` 全局重录 + grep 零注解残留；`_cases/` 518 基线 → String 形态落盘；非确定性单元格 `*` 通配（hr/mfg 先例）；逐案审核 id String 实证 + 拒绝「断言式」类误录（cs 81 目录/sal 7 类超录回退先例））。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：初录 79 snapshot-finished = 100% 录制正常结束标记（throwSnapshotFinishedError 栈复核零真实失败）；CHECKING 97/97 绿 ×2 零 `*` 通配需求；_cases 518 → 1155（637 首录 + 58 diff，git status -uall 口径）；json5 id String 实证（`"id": "4"`）；注解零残留 + 断言式零误录双 grep exit=1。
- [x] Proof: 域级测试 `mvn test -pl module-drp/erp-drp-service,module-drp/erp-drp-web` 全绿（web 0 tests 治理排除 `@Tag("full-app")`，successor M4.1）；平台 IoC 回归 self-wait 处置双分支证据——未复现零 delta（mfg/sal 先例）或复现按 fin 修正版先例 delta 落位（含 hr 第二环 nopDataAuthChecker daoProvider lazy-property 变体先例）。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：service 97/97 + web 0 tests 双绿 BUILD SUCCESS ×2；self-wait 未复现 → 零新增 delta（test resources `git status --porcelain` 零输出）。
- [x] Proof: 早域复跑 pur 基线维持（334/334；快照形态漂移就地 String 化 + `*` 通配按先例处置）。
      - Skill: `nop-testing`
      - 执行证据（2026-08-23）：`mvn test -pl module-purchase/erp-pur-service` 334/334 Failures 0 Errors 0 BUILD SUCCESS 零干预（090/091 退役后桥接面测试自然对齐，快照零形态漂移）。

Exit Criteria:

- [x] drp 域级测试全绿 + pur 基线维持全绿。
      - 执行证据（2026-08-23）：drp 97/97 + web 0 tests；pur 334/334。
- [x] 本域无 retired-test 半边义务（Phase 1 零实证复核）。
      - 执行证据（2026-08-23）：登记册 refDomain=drp 的 bridge-test 条目零条（Phase 1 全量消费复核），无半边回收义务。

### Phase 4 - 语义陷阱 grep 门控 + page.yaml String 化 + 收尾登记

Status: completed（2026-08-23：grep 门控清零——`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/装箱 `==`/id 序比较 main+dao 全零命中（排序位点复核：CrossDockProcessor:317 按 deliveryDate、LeadTimeProcessor:459 BigDecimal median，均非 id；`==` 命中均为 int size/total 比较）；`Long` 残留全为 `_gen` delVersion（规则 4 合法保持）；sql-lib 零存在注明；page.yaml `:Long` 1 处就地 String 化（net-requirement:38 `$pid:Long`→`$pid:String` + `pid: "${planId || 0}"`→`|| null` 兜底 + adaptor `pidVal !== 0`→`!== ''` 空值守卫，pur three-way-match 先例形态 + `ErpDrpPlan__get` 平台 ICrudBiz String 签名核证）+ YAML 良构校验（python yaml.safe_load OK）+ drp-web 重建绿 + 手写 view 零被动变更核证（非 `_gen` 资源变更 = 恰 1 page.yaml 主动 Fix）；owner doc `docs/design/drp/` grep Long id/BIGINT 零命中维持（2026-08-22 基线一致，零文档变更）；登记册 B main 090/091 → retired 附逐条兑付 note + fail-closed 解析验证（json5 宽松解析 + 结构断言 2/2 retired 在位）+ 工具 scan 复跑维持 32 余量（登记册消费联动零告警）+ 路线图 M3.7 → done（M2/M3 表位次 18 证据摘要 + 头部「最后更新」续链 + 位次 19 解锁）+ 日志 `docs/logs/2026/08-23.md` M3.7 条目（含验证状态 + B 义务兑付 + pur 复跑基线）；收尾验证 drp 7 链 no-am `mvn clean install -DskipTests` BUILD SUCCESS 7/7 + 域级测试 97/97 + web 0 tests）

Targets: drp-service main、drp-web 手写面、`docs/design/drp/`、登记册、路线图、日志
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 3 完成

- [x] Fix: 语义陷阱 grep 门控清零（路线图横切 §3 清单：`.longValue()`/`Long.parseLong`/`Map<Long`/`String.format("%d")`/装箱 `==`/id 序比较；合法非 id 项逐条注明；sql-lib 零存在注明）。
      - Skill: none
      - 执行证据（2026-08-23）：六类模式 main+dao 全零命中（无合法非 id 例外项需注明）；排序/比较位点逐条复核非 id；`_gen` delVersion 为规则 4 合法；`module-drp` 无 `*.sql-lib.xml`。
- [x] Fix: page.yaml `:Long` 1 处就地 String 化（net-requirement `$pid`；BizModel 签名一致性核证，mfg CRP / sal three-way-match 先例）+ YAML 良构校验 + drp-web 重建绿 + 手写 view 零被动变更核证（git status 非 `_gen` 变更 = 预期仅 page.yaml 主动 Fix）。
      - Skill: `nop-backend-dev`
      - 执行证据（2026-08-23）：`$pid:String` + `|| null` + adaptor 空值守卫三处落位；python yaml.safe_load OK；`:Long` 全资源复扫 exit=1 零命中；drp-web clean install BUILD SUCCESS；非 `_gen` 变更 = 恰 1 个 page.yaml。
- [x] Proof: owner doc 复核——`docs/design/drp/` grep 复核 Long id 陈述（2026-08-22 基线零命中，Phase 4 复核确认；命中即就地注记引用本计划）。
      - Skill: none
      - 执行证据（2026-08-23）：`rg 'Long|BIGINT|long 主键|数字 id' docs/design/drp/` exit=1 零命中——零文档变更维持。
- [x] Add: 登记册状态更新（B main 2 条 → retired 附逐条兑付 note + fail-closed 解析验证）+ 路线图 M3.7 → done（M2/M3 表位次 18 证据摘要 + 头部「最后更新」续链 + 位次 19 解锁）+ 日志 `docs/logs/2026/{执行当日}.md`（rule 8 日期口径，含验证状态 + B 义务兑付 + pur 复跑基线）。
      - Skill: none
      - 执行证据（2026-08-23）：090/091 retired note 双落（含 toLong/循环桥移除 + pur 7 链 + 334/334 要素）+ node 解析验证 2/2；roadmap 位次 18 `done` 证据摘要 + 头部 2026-08-23 M3.7 续链 + 位次 19 logistics 解锁；日志 `docs/logs/2026/08-23.md` M3.7 条目倒序落盘。

Exit Criteria:

- [x] grep 门控清零（登记例外除外）+ page.yaml `:Long` 清零。
      - 执行证据（2026-08-23）：六类语义陷阱零命中 + `:Long` 复扫零命中。
- [x] 登记册/路线图/日志三处一致落盘。
      - 执行证据（2026-08-23）：registry（090/091 retired + 解析验证）/ roadmap（位次 18 done + 头部续链）/ log（08-23.md M3.7 条目）三处 M3.7 口径一致。

## Draft Review Record

- Independent draft review iteration 1（技术/执行契约视角，fresh session，ses_fd5f6fea7ffeTT78V27peu1PnH）: `acceptable as-is` + 保护区域技术侧 `approve`——14 项事实验证全对（55 列/PK 17+FK 38/stub 6 含 inv stub 1/090/091 active+M2.5 落桥核证/零 bridge-test refDomain=drp/C1 6 文件+C2 9 类/14 测试/_cases 518/page.yaml 1 处/findFirstByOrg 0/继承中间态双源核证/owner doc 零命中/logistics 零 drp 引用/334/334）；5 MINOR（M2.6 归属 / 「自愈全部」措辞 / 被引用面字面冲突 / logistics 核证缺机械命令 / 批内序算术前提）已全部修正（继承中间态改 M2.6 单源 + 自愈/手写修复分述 / 被引用面加 090/091 限定 / Phase 2 证据源补 `rg` 机械复核 / Phase 1 退出口径补 213−126−55 算式与批内序前提）。
- Independent draft review iteration 1（治理/规范视角，fresh session，ses_fd5f6e3a4ffeTJR34PhIaf8ikr）: `accept` + 保护区域治理侧 `approve`——模板/命名/状态生命周期/Item typing/Skill/反松弛/保护区域证据链/Closure Gates 域级口径/路线图规则/文本一致性/回滚策略/Prereqs 11 项全 PASS；2 MINOR（时间戳溯源——批重命名 2311 已修复 / 批内序算术括号——已补）已修正。
- **共识达成（2026-08-22）**：技术侧（ses_fd5f6fea7ffeTT78V27peu1PnH）+ 治理侧（ses_fd5f6e3a4ffeTJR34PhIaf8ikr）iteration 1 双 approve、双独立子 agent 批准完成，5+2 MINOR 已全部就地修正并记录于上，转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（55 列落源 + 7 链绿 + B/C 兑付 + 快照重录 + 测试绿）
- [x] 相关文档对齐（登记册 + 路线图 + owner doc + 日志）
- [x] 已运行验证：`mvn clean install -pl module-drp/erp-drp-{codegen,dao,meta,service,web,app,api} -DskipTests`（no-am）BUILD SUCCESS + `mvn test -pl module-drp/erp-drp-service,module-drp/erp-drp-web` 全绿 + pur 复跑基线（334/334）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（保护区域双批准落盘）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - 执行证据（2026-08-23）：CLOSURE_VERIFY 独立会话（mission-driver task 2026-08-22-070202-mission-driver，非执行者上下文）执行结束审计通过并落盘本门控与 Closure 节。
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### logistics 下游破坏（预期零）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册实证零被引用面（logistics orm 无 drp stub、零 service/test 条目）；若 Phase 2 实测出现未登记破坏按 rule-6/D4 停止回报，不静默登记
- Successor Required: no（预期不存在；实测存在时升级为 M0 裁决）

### 存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b 四处）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: M4.1 专项全量清扫已登记（M3.6 结束审计发现的模式盲区）；本域 1 处属本计划范围就地修复
- Successor Required: yes（M4.1）

### web 页面测试治理排除（`@Tag("full-app")`）

- Classification: `watch-only residual`
- Why Not Blocking Closure: M1.1 已提交治理决策（surefire excludedGroups），实证依赖全量 classpath
- Successor Required: yes（M4.1 `ErpAllWebPagesTest` 兜底核证）

## Closure

Status Note: 四 Phase 全部 completed，Exit Criteria / Closure Gates 全 `[x]`，独立结束审计通过，M3.7 可关闭（roadmap 位次 18 done、位次 19 logistics 已解锁）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（CLOSURE_VERIFY，mission-driver task 2026-08-22-070202-mission-driver，新会话非执行者）；执行证据（2026-08-23）live repo 现场抽检 8 项全对：
  - orm：`module-drp/model/app-erp-drp.orm.xml` 残余 `stdDataType="long"` 恰 11 处且全部为 `delVersion`（规则 4 合法保持），PK/FK 列均 `stdDataType="string"` 且 `stdSqlType="BIGINT"` 保持。
  - 登记册：`tools/id-migration-registry.json5` bridge-main-090/091 双条 `status: "retired"` 附 M3.7 逐条兑付 note。
  - pur 退役兑付：`ErpPurReceiveProcessor` grep `ConvertHelper`/`bridge-main` 零命中，`drpMaterialIds` 为 `List<String>` 直传形态（循环转换桥已移除）。
  - drp IBiz 签名：`IErpInvDrpCrossDockBiz.markReceivedFromPurchase` / `IErpInvDrpLeadTimeRecordBiz.recordFromPurchaseReceive` 均 String 参数签名。
  - page.yaml：`pages/dashboard/net-requirement.page.yaml:38` `$pid:String` + `pid: "${planId || null}"` + adaptor 空值守卫在位；drp-web 全资源 `:Long` 复扫零命中。
  - 路线图：`docs/backlog/id-string-migration-roadmap.md` 位次 18 M3.7 `done` + 头部「最后更新 2026-08-23」续链 + 位次 19 解锁。
  - 日志：`docs/logs/2026/08-23.md` M3.7 完成条目在位（含验证状态 + B 义务兑付 + pur 334/334）。
  - 文本一致性：Plan Status / 四 Phase Status / Exit Criteria / Closure Gates / Draft Review Record（双独立子 agent 批准 ses_fd5f6fea7ffeTT78V27peu1PnH + ses_fd5f6e3a4ffeTJR34PhIaf8ikr）全一致。
- Evidence: 本节上述抽检记录 + `node tools/mission-driver/src/plan-check.mjs <本计划> --strict` 复跑 PASS。

Follow-up:

- 无（已确认缺陷不得出现在此处；存量 notify/aps/b2b page.yaml raw-GraphQL `:Long` 与 web 页面测试治理排除均已在 Deferred But Adjudicated 登记，successor M4.1）
