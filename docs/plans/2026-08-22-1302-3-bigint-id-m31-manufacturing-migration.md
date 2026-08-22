# 2026-08-22-1302-3-bigint-id-m31-manufacturing-migration 主键/外键 string 化 M3.1：manufacturing 域迁移（冻结序位次 14）

> Plan Status: active（2026-08-22：iteration 1-3 独立草案审查收敛 + 保护区域双独立子 agent 批准（治理 ses_fd8135082ffeZpqD8bXZ01byDt + 技术 ses_fd80e7974ffeoszSrevYOvLh1n），见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.1（manufacturing，冻结序位次 14）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 14（M3.1）
> Related: `docs/plans/2026-08-22-1302-2-bigint-id-m23-quality-migration.md`（批内序 2，本计划硬前置——qa-dao/qa-service String jar）、`docs/plans/2026-08-22-1302-1-bigint-id-m27-projects-migration.md`（批内序 1，mfg-web test-scope prj-dao String jar 来源）、`docs/plans/2026-08-22-0731-3-bigint-id-m32-maintenance-migration.md`（A2 桥 5 条登记来源 + B 退役先例）、`docs/plans/2026-08-22-0731-2-bigint-id-m22-inventory-migration.md`（inv 桥 4 条登记来源 + bridge-test-103..106 mfg 半边登记）、`docs/plans/2026-08-21-2025-1-bigint-id-m39-aps-migration.md`（aps 桥 11 条登记来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **mfg 域规模（2026-08-22 实况 scan）**：`module-manufacturing/model/app-erp-manufacturing.orm.xml` 需改列 **171 = 自有 156（PK 34 + BIGINT FK 122，含 orgId FK）+ notGenCode stub 15（md 9 stub 实体 14 列：`ErpMdCurrency`(id)/`ErpMdEmployee`(id)/`ErpMdLocation`(id,warehouseId,parentId)/`ErpMdMaterial`(id)/`ErpMdMaterialSku`(id,materialId)/`ErpMdOrganization`(id,parentId)/`ErpMdPartner`(id)/`ErpMdUoM`(id)/`ErpMdWarehouse`(id,orgId)——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐；inv 1：`ErpInvBatch.id`——inv 权威源自 M2.2 已 String，翻转 = 与权威源对齐）**。**不改列**：`delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）；A1 延后 = 0（scan DEFERRED = 0）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-manufacturing/erp-mfg-{codegen,dao,meta,service,web,app,api}`（全链构建，无延后）。`erp-mfg-service` main compile 依赖 **fin-service（M2.1 起 String）+ notify-dao（M1.2 起 String）+ mnt-dao（M3.2 起 String）+ qa-dao（批内序 2 M2.3 后 String——本计划硬前置）+ sal-dao + pur-dao（未迁移 Long jar——A2 桥接对象）** + common-service；test-scope 依赖 md-service + inv-service + qa-service + notify-service + mnt-service（均 String）。`erp-mfg-dao` compile 依赖 md-dao（String——mfg orm `refEntityName` md ×62，M1.1 登记的 `_gen` 关系胶水中间态**本域迁移即自愈**）+ inv-dao（String——inv 关系边 ×2，M2.2 登记 stub 对齐后本域胶水完全一致）；mfg 域内关系 ×73 不受影响。mfg-web test-scope 依赖 ast-dao + md-service（String）+ prj-dao（批内序 1 后 String——页面测试编译对象，预期零 id 穿越，Phase 3 核验）；mfg-app 域级 web 依赖仅 md-web（String，可建）。
- **M0.2 登记册 mfg 视角（§6.14 + json5 实测对账，起草已核）**：A1 = 0；**A2 main 桥接 4 条**（sal 2：bridge-main-086/087（`mrp/DemandAggregator:11/:12` `ErpSalOrder`/`ErpSalOrderLine` 类型级），退役 owner M2.6；pur 2：bridge-main-088/089（`mrp/MrpReleaseService:13/:14` `ErpPurOrder`/`ErpPurOrderLine` 类型级），退役 owner M2.5）；**A3 test 桥接 2 条**（bridge-test-126：`TestErpMfgMrpEndToEnd` 引用 pur 2 + sal 2 实体；bridge-test-127：`TestErpMfgMrpEngine` 引用 sal 2 实体——owner M3.1 = 本计划 Phase 3 退役）；**B 退役/翻转义务（作为晚域，main 20 条——mission 最大单批）**：aps 11（bridge-main-012/013（`ErpApsAtpCtpServiceImpl:15/:16` `ErpMfgBom`/`BomOperation`）+ 014/015（`loadsource/ApsLoadSourceProvider:5/:6`）+ 017..020（`processor/ErpApsAutoDispatchProcessor:11/:12/:13/:14` `ErpMfgBom`/`BomLine`/`WorkOrder`/`Workcenter`）+ 021/022/023（`processor/ErpApsWorkOrderToOperationProcessor:8/:9/:10` `ErpMfgRoutingOperation`/`WorkOrder`/`Workcenter`））+ inv 4（bridge-main-071/072（`costing/CostAdjustmentService:13/:14` `ErpMfgCostRollup`/`CostRollupLine`）+ 073/074（`costing/StandardCostResolver:7/:8`））+ mnt 5（bridge-main-080..084，`support/OeeCalculator`——080/083/084 = workcenterId `ConvertHelper.toLong` 单转换点（eq/filter 语义值桥）+ 081/082 = 类型级零转换点）——**aps/inv/mnt 侧桥接点退役 + 链重建 + 测试复跑（aps 76/76 + inv 235/235 + mnt 156/156 基线维持）为本计划义务（M2.4/M2.2/M3.2 晚域退役早域桥接点先例）**；**retired test 桥半边回收**：bridge-test-103..106 mfg 半边（aps 测试侧 String.valueOf 桥/mfg 种子——M2.2 登记「mfg 半边归 M3.1」）+ bridge-test-119/122 mfg 半边（inv 测试侧 `TestErpInvCostAdjust:496`/`TestErpInvStandardCosting:410` mfg CostRollup 种子 + `orm_propValueByName(..., toLong(...))` 桥——M2.2 登记「桥接点移除归 M3.1 mfg ×2」）+ bridge-test-125 mfg 半边（mnt `TestErpMntOee` mfg 种子 + `String.valueOf(WC)` 局部桥——M3.2 登记「owner M3.1」）；C1 后向 main 6 条（backward-168 → fin 2 文件 `MfgPostingExecutor`/`ErpMfgSubcontractOrderProcessor`；backward-169 → inv 10 符号 14 文件（entity/mrp/posting/processor/simulation/spi 全域）；backward-170 → mnt 1 文件 `ErpMfgScheduleToJobCardProcessor`；backward-171 → md 1 符号 11 文件；backward-172 → notify 2 符号 4 文件；backward-173 → qa 13 符号 2 文件 `ErpMfgWorkOrderProcessor`/`ErpMfgWorkOrderReportCompletionProcessor`（批内序 2 兑付））；C2 后向 test 6 条（backward-229 → fin 9 文件；backward-230 → inv 15 文件；backward-231 → mnt 1 文件 `TestErpMfgJobCardDowntimeGate`（M3.2 登记 successor 兑付）；backward-232 → md 24 文件；backward-233 → notify 3 文件；backward-234 → qa 1 文件 `TestErpMfgWorkOrderCancelInspectionLinkage`（批内序 2 兑付））。
- **被引用面（登记册 §6.14）**：drp main 1 文件 + drp test 1 文件——位次 18 未迁移域引用 mfg Long API 的编译破坏为**已登记中间态**（successor M3.7 + M4.1 兜底），不在本计划 no-am reactor 内。
- **手写代码冲击面（登记册 id 语境口径 + live import 实测）**：mfg-service main 跨域 import 实测 = fin 11 文件（登记册 backward-168 为 id 语境 2 文件，posting 族 Dispatcher/AcctDocProvider/Listener/ReversalListener 为类型级扩充面——`PostingEvent`/`AcctDocContext` String id 值流转按 M2.4 盲区先例以编译器清单为准）+ inv 14 文件（C1 定位面——mission 剩余最大 inv 消费面）+ mnt 1 文件 + md 12 文件（登记册 id 语境 11）+ notify 4 文件（C1，签名不变预期零破坏核验）+ qa 2 文件（C1，批内序 2 后 String 直传）+ sal/pur（A2 桥接点 `DemandAggregator`/`MrpReleaseService`）；dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 本域语义 FK Long 参数清单为 Phase 4 门控输入）。
- **测试资产（实测）**：mfg-service **47 个测试类 + 2 个支撑件**（`ErpMfgForecastStateMachineDelta`/`MfgFrozenClockExtension`，合计 49 个 .java 编译文件——批内最大）；`_cases` 快照 **1972 文件**（批内最大）；mfg-web 2 测试文件（`ErpMfgWebCodeGen` + `ErpMfgWebPagesTest` 治理排除，successor M4.1，参与 test-compile）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 2 处，迁移即失效的实时缺陷）**：`erp/mfg/pages/dashboard/main.page.yaml:148`（`$workcenterId:Long` → `ErpMfgDashboard__getCrpLoadChartData`——与 mnt `OeeCalculator` workcenterId 桥同语义面）+ `erp/mfg/pages/dashboard/bom-tree.page.yaml:43`（`$bid:Long` → `ErpMfgBom__explode` BOM 展开失效）——mfg 翻转 String 后静态类型不匹配 → adaptor 静默降级（M3.6 结束审计 MAJOR-1 同型）——**本计划就地 Fix + mfg-web 重建验证**（`$q:BigDecimal`/`$ml:Boolean` 非 id 合法保留）。
- **owner doc 已知 Long 陈述（Phase 4 注记对象，起草实测）**：`docs/design/manufacturing/simulation-engine.md:118`（`compareMrpVersions(Long versionIdA, Long versionIdB)` 签名）、`:139`（`MrpEngine.runMrp(Long planId, ...)` 签名）、`:290`（`shortageOnlyInA: [Long]` 等字段）——mfg 翻转后 Java 层 String 化，就地注记（引用本计划）。
- **已知风险（先例登记）**：① 平台 IoC 回归 self-wait——mfg 若复现按 fin 修正版先例落 test-scope VFS delta（根元素 `x:extends="super"`）+ DeltaOverride delta-layer 补 default 层集；第二环（nopOrmSessionFactory 经 nopDataAuthChecker）hr/aps/fin/ast 已落 delta，aps/inv/mnt 复跑侧若新复现按 hr 先例处理并补记 bug 域清单；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 sal-dao/pur-dao Long jar——A2 桥接登记例外；drp 引用 mfg 的破坏为登记中间态）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-manufacturing` 新鲜度门控（零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：mfg orm 171 列全 `stdDataType="long"` 待改；mfg 手写代码/测试/快照全部 Long 形态；早域 aps/inv/mnt 桥接点 20 处待退役；冻结序位次 14。

## Goals

- mfg 域 171 列（自有 156 + md stub 14 + inv stub 1）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 mfg 全部手写代码（C1 6 条兑付含 qa 批内序 2 面）+ A2 前向桥接 4 处落桥（sal 2 + pur 2，退役 owner M2.6/M2.5）。
- **B 退役义务兑付（main 20 条，mission 最大单批）**：aps 11 + inv 4 + mnt 5 桥接点退役（mnt 080/083/084 实值桥 → String 直传；其余核证类型级/零转换点或签名对齐）+ aps/inv/mnt 7 模块链重建绿 + **aps 76/76 + inv 235/235 + mnt 156/156 基线维持复跑**；登记册 20 条 → retired。
- A3 test 桥接 2 条退役 + **retired test 半边回收**：bridge-test-103..106 mfg 半边（aps 测试侧）+ bridge-test-119/122 mfg 半边（inv 测试侧 mfg CostRollup 种子 + toLong 桥移除）+ bridge-test-125 mfg 半边（mnt `TestErpMntOee` mfg 种子 + `String.valueOf(WC)` 局部桥移除）。
- 快照每域重录（RECORDING→CHECKING；1972 文件基线，批内最大重录面）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 2 处就地 Fix（mfg-web 重建验证）+ owner doc 注记（simulation-engine.md ×3 处）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘，C1/C2 修复定位面消费（含 backward-170/231（mnt）+ backward-173/234（qa）批内/先例登记的 successor 兑付面），heal M1.1 登记的 mfg-dao `_gen` md 胶水中间态。
- 路线图 M3.1 → `done` + 日志；位次 15（purchase）解锁（供后续批次）。

## Non-Goals

- 不迁移 sales/purchase 域（A2 桥接目标域，归 M2.6/M2.5）；不动 sal/pur orm 与生成件（桥接仅触及本域手写代码）。
- 不修 drp 对 mfg 的引用破坏（main 1 + test 1 文件——已登记中间态，successor M3.7；本计划 Phase 4 登记确认）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（规则 4）；不修 `ErpMfgWebPagesTest` 治理排除（successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。
- 不修四域（md/notify/b2b/contract）IoC delta `x:extends="super"` 回收（不在触碰面，归各域 plan 触碰时或 M4.1——bug `docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`）。
- 不动 fin web/app 补做、prj/qa 域本体（归批内序 1/2）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更 + 跨域桥接退役编辑面）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 14 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；mfg 业务语义 owner doc = `docs/design/manufacturing/`（Phase 4 注记对象，已知 3 处）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + 位次 3-13 链 install（aps/b2b/contract/fin/ast/cs/hr/inv/mnt/**prj/qa——批内序 1/2 完成且 qa-dao/qa-service 已 install String 形态为本计划硬前置**）。回滚策略：revert orm.xml + `mvn clean install -pl module-manufacturing/erp-mfg-codegen,module-manufacturing/erp-mfg-dao,module-manufacturing/erp-mfg-meta,module-manufacturing/erp-mfg-service,module-manufacturing/erp-mfg-web,module-manufacturing/erp-mfg-app,module-manufacturing/erp-mfg-api -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 2/3 完成后回滚需先 revert 早域桥接退役与测试代码**——aps/inv/mnt 桥接点已移除形态对 Long mfg jar 不可编译）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-manufacturing/model/app-erp-manufacturing.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M3.2 ✅ + **M2.3 ✅（批内序 2 本批完成）** + M2.2 ✅ + M2.1 ✅ + M1.1 ✅ + M1.2 ✅（精确前置满足）；本计划已通过独立 plan-audit + 双独立子 agent 批准（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.14 mfg 节，逐条核对：(i) A1 = 0（171 列全翻转）；(ii) A2 = bridge-main-086..089（sal 2 + pur 2）与本地实测 import 对账（`DemandAggregator`/`MrpReleaseService`）；(iii) A3 = bridge-test-126/127 作为 Phase 3 定位面；(iv) **B 退役义务 = aps 11（4 文件）+ inv 4（2 文件）+ mnt 5（`OeeCalculator`，含 080/083/084 workcenterId 单转换点语义值桥）+ retired test 半边（bridge-test-103..106 aps 侧 + bridge-test-119/122 inv 侧 + bridge-test-125 mnt 侧）作为 Phase 2/3 定位面**；(v) C1 = backward-168..173 与 C2 = backward-229..234 作为 Phase 2/3 定位面（backward-170/231 为 M3.2 登记 successor 兑付面；backward-173/234 为批内序 2 登记 qa 面兑付）；(vi) 按 b2b/assets A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(pur|sal|crm|drp|log)\.' module-manufacturing/erp-mfg-service/src/test module-manufacturing/erp-mfg-web/src/test` 排除 import 行 + test beans.xml ioc:type FQN——覆盖本域执行时点全部未迁移晚域；起草实测零命中，执行时点复扫确认）。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-manufacturing` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-manufacturing/model/app-erp-manufacturing.orm.xml` 逐行核对——仅 171 列 `stdDataType="long"→"string"`（自有 156 + md stub 14 + inv stub 1），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan mfg 段重扫零 `NEEDS FIX`/零 `DEFERRED`。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案（含 FQN 盲区复扫结论 + B 义务 20 条定位面）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 171 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 落桥 + B 退役兑付

Status: planned
Targets: `module-manufacturing/erp-mfg-{dao,service}/src/main/java/**`（手写 IBiz/BizModel/Processor/mrp/posting/costing/genealogy/simulation/spi）；**跨域编辑面：`module-aps/erp-aps-service/src/main/**`（11 桥接点）+ `module-inventory/erp-inv-service/src/main/**`（4 桥接点）+ `module-maintenance/erp-mnt-service/src/main/**`（5 桥接点）**
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-manufacturing/erp-mfg-codegen,module-manufacturing/erp-mfg-dao,module-manufacturing/erp-mfg-meta,module-manufacturing/erp-mfg-service,module-manufacturing/erp-mfg-web,module-manufacturing/erp-mfg-app,module-manufacturing/erp-mfg-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`）触发增量重生成。预期：mfg-dao `_gen` md 关系胶水（62 处）自 M1.1 登记中间态自愈 + inv 胶水（2 处）与 inv-dao String jar 完全一致。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 mfg dao + service 手写代码类型错误（定位面：fin 2 文件（登记册 id 语境；live import 11——Current Baseline 口径）+ inv 14 文件（C1——mission 剩余最大 inv 消费面，posting/mrp/processor/simulation/spi 全域）+ mnt 1 文件（C1，`ErpMfgScheduleToJobCardProcessor`——M3.2 登记 successor 兑付）+ md 11 文件 + notify 4 文件（C1，签名不变预期零破坏核验）+ qa 2 文件（C1，批内序 2 后 String 直传）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游；fin/inv/md posting 族 id 值流转以编译器实际清单为准——md 12 文件中登记册 id 语境 11 + 类型级 1 扩充面同规则），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接 4 处落桥（D4 消费协议）——mfg String id ↔ sal/pur Long API 的调用点加转换桥（`DemandAggregator`（sal 实体）/`MrpReleaseService`（pur 实体）——id 语境经变量流转处按编译器/grep 定位落 `ConvertHelper.toLong` 桥；**eq/filter 语义值桥主动识别**（Long 列传 String 静默空匹配——contract/cs/mnt 先例）），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M2.6（sal 2）/M2.5（pur 2）；代码内 bridge 注释双向指针。
  - Skill: `nop-backend-dev`
- [ ] Fix: **B 退役义务兑付（main 20 条）**——mfg IBiz 签名翻转 String 后：mnt 侧 `OeeCalculator` 080/083/084 workcenterId `ConvertHelper.toLong` 单转换点 → String 直传 + bridge 注释移除 + 081/082 核证零转换点（Long 原生流转随翻转消失复核）；aps 侧 4 文件 11 处核证类型级/实值桥按实际转换点退役（M2.2 对 aps 4 条同形状先例）；inv 侧 2 文件 4 处同前例；aps/inv/mnt 各自 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）重建绿；三域 grep 复核 mfg 桥残留清零（M2.4 先例口径）。M2.4/M2.2/M3.2 先例为执行范式。
  - Skill: `nop-backend-dev`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（reactor 仅 7 模块）；drp 对 mfg 的引用破坏为已登记中间态（successor M3.7），Phase 4 登记；未登记破坏按路线图规则 6 停止回报。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] mfg 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案
- [ ] **B 义务：aps + inv + mnt 7 模块链重建绿（main 口径）+ 三域 grep 桥残留清零**（三域 test-compile 证明归 Phase 3 早域复跑——批内先例同形状处理）

### Phase 3 - 测试修复 + A3 桥接退役 + retired 半边回收 + 快照重录 + 域级测试 + 早域测试复跑

Status: planned
Targets: `module-manufacturing/**/src/test/**`、`module-manufacturing/erp-mfg-service/_cases/**`；早域复跑：`module-aps`、`module-inventory`、`module-maintenance` 测试；跨域测试编辑面：aps 测试侧（bridge-test-103..106 mfg 半边）、inv 测试侧（bridge-test-119/122 mfg 半边：`TestErpInvCostAdjust`/`TestErpInvStandardCosting`）、mnt `TestErpMntOee`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——47 个测试类（+2 支撑件）的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；C2 后向 test 适配（backward-229 fin 9 + backward-230 inv 15 + backward-231 mnt 1（`TestErpMfgJobCardDowntimeGate`——M3.2 登记 successor 兑付）+ backward-232 md 24 + backward-233 notify 3 + backward-234 qa 1（批内序 2 登记 qa 面兑付）文件）；mfg-web test-scope ast-dao/md-service/prj-dao（批内序 1 后 String）编译对象零 id 穿越核验（`ErpMfgWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
- [ ] Fix: A3 test 桥接适配（bridge-test-126/127，2 条）——`TestErpMfgMrpEndToEnd`/`TestErpMfgMrpEngine` 引用 pur/sal 实体的 id 形态桥接（String↔Long 局部转换或 mock 桩签名适配，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M3.1 = 本计划）。
  - Skill: `nop-testing`
- [ ] Fix: **retired test 半边回收**——aps 侧 bridge-test-103..106 mfg 半边（aps 测试中 mfg 种子 String 化 + `String.valueOf` 局部桥移除——M2.2 登记「mfg 半边归 M3.1」兑付）+ inv 侧 bridge-test-119/122 mfg 半边（`TestErpInvCostAdjust`/`TestErpInvStandardCosting` 中 mfg CostRollup 种子 String 化 + `toLong` 桥/双向指针注释移除——M2.2 登记「桥接点移除归 M3.1 mfg ×2」兑付；注意反射式 `orm_propValueByName(..., toLong(...))` 经平台 coercion 宽容可能不翻红——以 grep 定位清零而非测试红绿为凭）+ mnt 侧 bridge-test-125 mfg 半边（`TestErpMntOee` mfg 种子 String 化 + `String.valueOf(WC)` 局部桥与双向指针注释移除——M3.2 登记 owner M3.1 兑付）；三域测试编译与运行证明归早域复跑。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 mfg service 测试 → 逐案审核 `_cases/` 新形态（1972 文件基线，批内最大重录面；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正；「断言式 + 空 autotest.yaml」范式测试不录——cs 回退先例）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-manufacturing/erp-mfg-service,module-manufacturing/erp-mfg-web`（D3 口径：不带 `-am`）全绿——service 47 测试类 + web BUILD SUCCESS（`ErpMfgWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按 fin 修正版先例修复（test-scope VFS delta 带根元素 `x:extends="super"` + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`
- [ ] Proof: **早域测试复跑（B 义务验证）**——`mvn test -pl module-aps/erp-aps-service,module-aps/erp-aps-web` 全绿（**76/76 基线维持**，含 bridge-test-103..106 mfg 半边回收后形态）+ `mvn test -pl module-inventory/erp-inv-service` 全绿（**235/235 基线维持**，默认口径，含 bridge-test-119/122 mfg 半边回收后形态；perf `@Tag("perf")` 治理排除同 M2.2 口径）+ `mvn test -pl module-maintenance/erp-mnt-service,module-maintenance/erp-mnt-web` 全绿（**156/156 基线维持**，含 `TestErpMntOee` mfg 半边回收后形态）。红则修复至绿（桥接退役遗留问题在本计划内闭环；IoC 第二环若新侧复现按 hr 先例 delta 处理并补记 bug 域清单）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] mfg 域级测试全绿（service 47+1（49 个编译文件口径见 Current Baseline）+ web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案
- [ ] **早域复跑全绿（aps 76/76 + inv 235/235 + mnt 156/156 基线维持）；A3 2 条退役 + retired 半边回收（103..106 + 119/122 + 125）在案**（登记册状态更新归 Phase 4 落盘）

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: planned
Targets: `module-manufacturing/**`（手写代码 + mfg-web 手写 page.yaml）、`docs/design/manufacturing/simulation-engine.md`、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，mfg 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M2.6/M2.5）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱专项（`getId\(\)\s*[<>]|comparing.*getId`——String 字典序，contract idOrder 先例）；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Fix: mfg-web 手写 page.yaml raw-GraphQL `:Long` 变量 2 处就地 String 化——dashboard:148（`$workcenterId:Long` → `:String`）+ bom-tree:43（`$bid:Long` → `:String`，`$q:BigDecimal`/`$ml:Boolean` 非 id 合法保留）；variables 链与 options value 链一致性核证（contract version-diff 先例）；随后 `rg ':Long' module-manufacturing/erp-mfg-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零；mfg-web 重建 BUILD SUCCESS 验证。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-manufacturing/erp-mfg-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
- [ ] Add: 登记册状态更新——B main 桥接 20 条（aps 11 + inv 4 + mnt 5）→ retired（兑付 note：早域侧移除/核证证据 + 链重建 + 复跑指针）；A3 test 桥接 2 条（bridge-test-126/127）→ retired（owner M3.1 兑付 note）；bridge-test-103..106 + 119/122 + 125 retired note 补 mfg 半边兑付指针；A2 main 桥接 4 条保持 active（退役 owner M2.6/M2.5）；被引用面确认（drp successor 指针已由 backward-pointer 登记）；fail-closed 解析验证通过（dry-run 正常消费 + mfg 段 0 待改列）。
  - Skill: none
- [ ] Add: owner doc 注记——`docs/design/manufacturing/simulation-engine.md:118/:139/:290` 三处 Long 签名/字段陈述就地注记（Java 层已 String 化，引用本计划）；其余 mfg 设计文档 grep 复核（零 Long id 陈述则记录结论）。
  - Skill: none
- [ ] Add: 路线图 M3.1 → `done`（M2/M3 表位次 14 + 头部「最后更新」；位次 15 purchase 解锁——供后续批次）+ 日志条目（含验证状态 + B 义务 20 条兑付 + 三早域复跑基线）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外逐条核清 + 桥接例外清单在案）；page.yaml `:Long` 清零 + mfg-web 重建绿 + view 零被动变更在案
- [ ] 路线图状态、登记册退役（B 20 条 + A3 2 条 + 半边兑付）、owner doc 注记、日志一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，技术/执行视角 plan-audit，ses_fd81fcf92ffe76GeZEW50GSSn4）：`needs revision` — 1 MAJOR / 3 MINOR。事实核对大部属实（171 列构成/7 模块链/pom 依赖/orm 关系计数/A2 4 条/A3 126,127/B 20 条含 mnt 080/083/084 单转换点机制描述/C1-C2 文件清单/被引用面 drp/早域基线 76+235+156/retired 半边 103..106 与 125/page.yaml 2/_cases 1972/simulation-engine.md 三处/FQN 复扫零命中/前置链与批内序均验证通过）。MAJOR：retired 半边回收漏 bridge-test-119/122（inv 侧 `TestErpInvCostAdjust:496`/`TestErpInvStandardCosting:410` mfg CostRollup 种子 + toLong 桥——M2.2 登记「桥接点移除归 M3.1 mfg ×2」；反射式 `orm_propValueByName(..., toLong(...))` 经平台 coercion 宽容可能不翻红——静默跳过登记义务风险）。MINOR：① md stub 枚举 8 实体 → 实际 9（漏 `ErpMdEmployee.id`；总数 14 列不变）；② C1 fin/md 计数为登记册 id 语境口径（live import fin 11/md 12）未如实入 Baseline；③ 49 测试类 → 47+2 支撑件。
- Independent draft review iteration 2（2026-08-22，治理/规范视角，ses_fd8135082ffeZpqD8bXZ01byDt）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR（① 本节未记录 iteration 1；② Baseline 引用不存在的 `$lim:Int` 变量（bom-tree 仅 `$bid/$q/$ml`，rg 零命中）；③ Phase 2 md 12 文件与登记册 11 的对账未带入 Phase 2 caveat）。治理检查全过（命名/路线图对齐位次 14 + M2.3 批内硬前置诚实框架/保护区域协议/anti-slack 零违规/检查清单完整性域级口径/内部一致性独立复核（205 = 171 + 34 delVersion 自洽）/iteration-1 四项修订全部核实/模板合规）。
- **修订（iteration 1 → 2，已落地）**：全部 1 MAJOR + 3 MINOR 处理——bridge-test-119/122 inv 侧 mfg 半边回收补入 Current Baseline/Goals/Phase 1(iv)/Phase 3（含 coercion 宽容「以 grep 定位清零而非测试红绿为凭」警示）/Phase 4 登记册 note/退出标准/Closure Gates；md stub 枚举改 9 实体；live import 计数入 Baseline + Phase 2 caveat（fin 2→live 11 注记 + md 11→12 同规则）；47+2 口径。iteration 2 的 3 MINOR 已就地修正（本节记录 + `$lim:Int` 删除 + md 对账 caveat）。
- **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
  - 批准 1（治理视角）：ses_fd8135082ffeZpqD8bXZ01byDt，2026-08-22 — 批准 orm 变更面（mfg 171 列 = 自有 156 + md stub 14 + inv stub 1，stdDataType-only、stdSqlType/DDL 不变、delVersion 排除、md/inv stub 翻转 = 与已迁移权威源对齐），条件 = MINOR-1 本节记录落地（已落地）+ MINOR-2/3 同批编辑（已落地）。
  - 批准 2（技术视角）：ses_fd81fcf92ffe76GeZEW50GSSn4 iteration 1 为 `needs revision`；iteration 3 复核（ses_fd80e7974ffeoszSrevYOvLh1n，2026-08-22）确认 MAJOR（119/122）+ 全部 MINOR RESOLVED（逐项 live 复验：bridge 注释恰在 :496/:410、反射式 Long seeding + eq toLong 过滤两形态 coercion 机制均实证、md stub 9 实体 14 列求和吻合、205 = 171 + 34 delVersion 自洽、fin 11/md 12 live 复核）+ 零新缺陷 → **技术视角批准生效（无条件）**。
- Independent draft review iteration 3（2026-08-22，技术侧复核，ses_fd80e7974ffeoszSrevYOvLh1n）：`passes draft review` — 全部发现 RESOLVED、零新增缺陷、可转 `active`。
- 共识达成（2026-08-22）：iteration 3 全部发现 RESOLVED + 双批准落盘（治理批准经 iteration 2 条件落地生效；技术批准经 iteration 3 生效）→ 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（171 列落源 + no-am 7 模块重生成 + 手写代码/测试修复 + A2 落桥 4 + B 退役 20 与 retired 半边回收（103..106/119/122/125） + 快照重录 + grep 门控清零 + page.yaml 2 处 Fix）
- [ ] 相关文档对齐（owner doc 注记（simulation-engine.md ×3）、路线图 M3.1 状态、登记册退役（B 20 + A3 2 + 半边兑付）、日志）
- [ ] 已运行验证：`mvn clean install -pl module-manufacturing/erp-mfg-{codegen,dao,meta,service,web,app,api} -DskipTests` 全绿 + `mvn test -pl module-manufacturing/erp-mfg-service,module-manufacturing/erp-mfg-web` 全绿 + **早域复跑 aps 76/76 + inv 235/235 + mnt 156/156** + 工具重扫零残留（mfg 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2 main 桥接 4 处（sal 2 + pur 2，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——sal（位次 16）/pur（位次 15）未迁移，桥接点为编译必需
- Successor Required: `yes`（M2.6 回收 sal 2 条；M2.5 回收 pur 2 条——晚域翻转时退役条目并移除本域桥接点）

### drp 对 mfg 的引用破坏（main 1 + test 1 文件）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册 backward-pointer 预先登记的中间态（本计划 no-am reactor 不含 drp；被引用清单 §6.14 在案）
- Successor Required: `yes`（M3.7 plan Phase 2/3 + M4.1 兜底）

### `ErpMfgWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策，实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### 平台 IoC 回归兼容层 delta（若复现落位）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 bean-init self-wait 为已登记平台 Bug（`docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-*.md` + 第二环 bug 补记）；本计划按先例 delta 断环
- Successor Required: `yes`（平台修复后统一移除全部兼容层 delta，M4.1 复核）

## Closure

Status Note: （draft——尚未执行）

Closure Audit Evidence:

- （待独立结束审计）

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录。）
