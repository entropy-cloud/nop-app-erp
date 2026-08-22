# 2026-08-22-1302-2-bigint-id-m23-quality-migration 主键/外键 string 化 M2.3：quality 域迁移（冻结序位次 13）

> Plan Status: active（2026-08-22：iteration 1-3 独立草案审查收敛 + 保护区域双独立子 agent 批准（治理 ses_fd8136414ffey4foiwZl50sRoA + 技术 ses_fd80e8bcbffeMggNsjdQQ7hR23），见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M2.3（quality，冻结序位次 13）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 13（M2.3）
> Related: `docs/plans/2026-08-22-1302-1-bigint-id-m27-projects-migration.md`（批内序 1，非硬前置——M2.3 精确前置不含 prj，按冻结序先执行）、`docs/plans/2026-08-22-0731-3-bigint-id-m32-maintenance-migration.md`（B 退役义务先例 + bridge-test-125 登记）、`docs/plans/2026-08-22-0002-3-bigint-id-m35-cs-migration.md`（bridge-main-057..060 桥接落桥来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **qa 域规模（2026-08-22 实况 scan）**：`module-quality/model/app-erp-quality.orm.xml` 需改列 **58 = 自有 50（PK 16 + BIGINT FK 34，含 orgId FK）+ notGenCode md stub 8（5 stub 实体：`ErpMdOrganization`(id,parentId)/`ErpMdMaterial`(id)/`ErpMdPartner`(id)/`ErpMdWarehouse`(id,orgId)/`ErpMdEmployee`(id,orgId)——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐）**。**不改列**：`delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）；A1 延后 = 0（scan DEFERRED = 0）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-quality/erp-qa-{codegen,dao,meta,service,web,app,api}`（全链构建，无延后）。`erp-qa-service` main compile 依赖 **inv-dao（M2.2 起 String）+ fin-service（M2.1 起 String）+ sal-dao + pur-dao（未迁移 Long jar——A2 桥接对象）** + common-service；test-scope 依赖 md-service + inv-service + notify-service + sal-meta。`erp-qa-dao` compile 依赖 md-dao（String——qa orm `refEntityName` md ×19，M1.1 登记的 `_gen` 关系胶水中间态**本域迁移即自愈**；qa 域内关系 ×15 不受影响）。qa-web test-scope 依赖 ast-dao + md-service（String）+ prj-dao（Long 陈旧 jar 或批内序 1 后 String jar——页面测试编译对象，预期零 id 穿越，Phase 3 核验）；qa-app 域级 web 依赖仅 md-web（String，可建）。
- **M0.2 登记册 qa 视角（§6.13 + json5 实测对账，起草已核）**：A1 = 0；**A2 main 桥接 11 条（sal 9 / pur 2）**（sal：bridge-main-092/093（`RecallTargetLocator:17/:141` `ErpSalDelivery` 类型级 + `IErpSalDeliveryBiz.findFirst`）+ 095/097（`NcrReturnOrchestrator:11/:117` `ErpSalReturn` + `IErpSalReturnBiz.save`）+ 098/099/100/101/102（`ErpQaRecallGenerateReturnsProcessor:8/:9/:10/:55/:77` `ErpSalDelivery`/`DeliveryLine`/`Return` + `IErpSalDeliveryBiz.get`/`IErpSalReturnBiz.save`），退役 owner M2.6；pur：bridge-main-094（`NcrReturnOrchestrator:6` `ErpPurReturn`）+ 096（`:99` `IErpPurReturnBiz.save`），退役 owner M2.5——其中含「方法未声明于接口文件（ICrudBiz 继承方法）」类型级条目，Long 参数签名以晚域 plan 翻转时为准）；**A3 test 桥接 5 条**（bridge-test-128/129（`TestErpQaRecallE2E`/`TestErpQaRecallLocateNotifyReturn` 引用 sal 实体）+ 130/131/132（`TestStubErpPurReturnBiz`/`TestStubErpSalDeliveryBiz`/`TestStubErpSalReturnBiz` mock 桩），owner M2.3 = 本计划 Phase 3 退役）；**B 退役/翻转义务（作为晚域，main 5 条 + cs 侧 retired test 兑付）**：翻转 qa IBiz 参数签名时退役——bridge-main-057/058/059/060（早域 cs，`ErpCsTicketEscalateToQualityProcessor`——登记册行号 :10/:151/:184/:189，live 已因桥注释漂移（:154/:174/:187/:195 附近），执行以符号定位；M3.5 落桥，其中 059 含实值转换桥 ×2 `ConvertHelper.toLong(materialId/supplierId)`，057/058/060 类型级/接口级）+ bridge-main-085（早域 mnt，`OeeCalculator` qa import——登记册 :10，live :11，类型级零转换点）——**cs/mnt 侧桥接点退役 + 链重建 + 测试复跑（cs 185/185 + mnt 156/156 基线维持）为本计划义务（M2.4/M3.2 晚域退役早域桥接点先例）**；**cs 侧 retired test 兑付（M3.5 登记 owner M2.3）**：bridge-test-117（`TestMockQaBizModels` mock 桩 7 方法 `Long ncrId` 签名——`IErpQaNonConformanceBiz` 属 qa，翻转后桩 String 化）+ bridge-test-114（`TestErpCsQualityEscalation` `MATERIAL_ID_LONG = 8301L` 常量 + `assertEquals(Long.valueOf(8401L), ...supplierId)` 断言——059 桥退役后 String 直传形态）；**retired test 桥半边回收**：bridge-test-125 note「qa 种子 Long 保持」——mnt `TestErpMntOee` qa 侧种子（QaInspection）随 qa 翻转 String 化 + `String.valueOf` 局部桥复核（Phase 3 兑付，mnt 复跑覆盖验证）；C1 后向 main 3 条（backward-184 → fin 2 文件 `NcrPostingDispatcher`/`NcrPostingExecutor`；backward-185 → inv 10 符号 3 文件 `RecallTargetLocator`/`NcrPostingDispatcher`/`NcrReturnOrchestrator`；backward-186 → md 1 文件 `ErpQaReportBizModel`）；C2 后向 test 3 条（backward-246 → fin 2 文件；backward-247 → inv 3 文件；backward-248 → md 4 文件）。
- **被引用面（登记册 §6.13）**：drp main 2/test 2、manufacturing main 2/test 1、purchase main 2/test 1、sales main 2/test 1——未迁移晚域引用 qa Long API 的编译破坏为**已登记中间态**（successor M3.7/M3.1/M2.5/M2.6），不在本计划 no-am reactor 内。
- **手写代码冲击面（实测）**：qa-service main 跨域 import = fin + inv（C1 定位面；posting 族 `PostingEvent`/`AcctDocContext` String id 值流转按 M2.4 盲区先例以编译器清单为准）+ md（C1）+ sal/pur（A2 桥接点）——**notify main import 零命中**（notify-service 仅 test-scope classpath，签名不变零破坏核验面归 Phase 3 测试侧）；dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 本域语义 FK Long 参数清单为 Phase 4 门控输入）。
- **测试资产（实测）**：qa-service **34 个测试类 + 1 个支撑件**（`QaFrozenClockExtension`，合计 35 个 .java 编译文件）；`_cases` 快照 **791 文件**；qa-web 2 测试文件（`ErpQaWebCodeGen` + `ErpQaWebPagesTest` 治理排除，successor M4.1，参与 test-compile）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 3 处，迁移即失效的实时缺陷）**：`erp/qa/pages/dashboard/main.page.yaml:190` + `erp/qa/pages/spc-chart/main.page.yaml:53/:102`（`$chartId:Long` → `ErpQaDashboard__getSpcControlChartData` 查询 + `ErpQaSpcSample__findPage` 过滤）——qa 翻转 String 后静态类型不匹配 → adaptor 静默降级、SPC 图表失效（M3.6 结束审计 MAJOR-1 同型）——**本计划就地 Fix + qa-web 重建验证**（`$lim:Int` 非 id 合法保留）。
- **owner doc 已知 Long 陈述（Phase 4 注记对象，起草实测）**：`docs/design/quality/recall.md:88`——「类型桥（残留风险）：`batchTrace` 入参 `batchNo:String`，而 `ErpQaRecall.batchId` 为 Long FK」陈述——qa 翻转后 batchId Java 层 String 化，注记就地更新（引用本计划）。
- **已知风险（先例登记）**：① 平台 IoC 回归 self-wait——qa 若复现按 fin 修正版先例落 test-scope VFS delta（根元素 `x:extends="super"`）+ DeltaOverride delta-layer 补 default 层集；第二环（nopOrmSessionFactory 经 nopDataAuthChecker）hr/aps/fin/ast 已落 delta，cs/mnt 复跑侧若新复现按 hr 先例处理并补记 bug 域清单；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 sal-dao/pur-dao Long jar——A2 桥接登记例外；prj-dao test-scope 编译对象）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-quality` 新鲜度门控（零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：qa orm 58 列全 `stdDataType="long"` 待改；qa 手写代码/测试/快照全部 Long 形态；早域 cs/mnt 桥接点 5 处待退役；冻结序位次 13。

## Goals

- qa 域 58 列（自有 50 + md stub 8）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 qa 全部手写代码 + A2 前向桥接 11 处落桥（sal 9 + pur 2，退役 owner M2.6/M2.5）。
- **B 退役义务兑付（main 5 条 + cs 侧 retired test 兑付）**：cs 4 处（`ErpCsTicketEscalateToQualityProcessor`——059 实值桥 → String 直传 + 057/058/060 核证零转换点/签名对齐）+ mnt 1 处（`OeeCalculator` 核证零转换点）+ **cs 测试侧兑付（bridge-test-114/117）**：`TestMockQaBizModels` mock 桩 7 方法 String 化（`IErpQaNonConformanceBiz` 属 qa——mnt「接口属翻转域则桩 String 化」先例）+ `TestErpCsQualityEscalation` Long 常量/断言 String 直传形态 + cs/mnt 7 模块链重建绿 + **cs 185/185 + mnt 156/156 基线维持复跑**；登记册 5 条 → retired + 114/117 note 兑付。
- A3 test 桥接 5 条退役（Recall 测试局部桥 + TestStub* 桩适配——**桩实现 sal/pur 所属 IBiz 接口（未迁移 jar），方法签名保持 Long + 桩内 qa 侧值转换**（cs 先例；接口属未迁移域，与 mnt「翻转域接口桩 String 化」分支互斥，按接口归属裁定））+ **bridge-test-125 qa 半边回收**（mnt `TestErpMntOee` qa 种子 String 化）。
- 快照每域重录（RECORDING→CHECKING；791 文件基线）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 3 处就地 Fix（qa-web 重建验证）+ owner doc 注记（recall.md:88）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘，C1/C2 修复定位面消费，heal M1.1 登记的 qa-dao `_gen` md 胶水中间态。
- 路线图 M2.3 → `done` + 日志；位次 14（manufacturing，批内序 3）的 qa 前置满足。

## Non-Goals

- 不迁移 sales/purchase 域（A2 桥接目标域，归 M2.6/M2.5）；不动 sal/pur orm 与生成件（桥接仅触及本域手写代码）。
- 不修 drp/mfg/pur/sal 对 qa 的引用破坏（已登记中间态，successor M3.7/M3.1/M2.5/M2.6；本计划 Phase 4 登记确认）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（规则 4）；不修 `ErpQaWebPagesTest` 治理排除（successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。
- 不修四域（md/notify/b2b/contract）IoC delta `x:extends="super"` 回收（不在触碰面，归各域 plan 触碰时或 M4.1——bug `docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`）。
- 不动 fin web/app 补做与 prj 域（归批内序 1 / M2.7）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更 + 跨域桥接退役编辑面）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 13 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；qa 业务语义 owner doc = `docs/design/quality/`（Phase 4 注记对象，已知 1 处）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install + 位次 3-11 链 install（aps/b2b/contract/fin/ast/cs/hr/inv/mnt——**inv-dao/inv-service String 新 jar 为本计划硬前置**）。回滚策略：revert orm.xml + `mvn clean install -pl module-quality/erp-qa-codegen,module-quality/erp-qa-dao,module-quality/erp-qa-meta,module-quality/erp-qa-service,module-quality/erp-qa-web,module-quality/erp-qa-app,module-quality/erp-qa-api -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 2/3 完成后回滚需先 revert 早域桥接退役与测试代码**——cs/mnt 桥接点已移除形态对 Long qa jar 不可编译）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-quality/model/app-erp-quality.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M2.2 ✅ + M2.1 ✅ + M1.1 ✅ + M1.2 ✅（精确前置满足；批内序 1（M2.7）非硬前置——M2.3 依赖列不含 prj，批内按冻结序先执行）；本计划已通过独立 plan-audit + 双独立子 agent 批准（批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.13 qa 节，逐条核对：(i) A1 = 0（58 列全翻转）；(ii) A2 = bridge-main-092..102（**sal 9 + pur 2**——refDomain 逐条对账，sal = 092/093/095/097/098/099/100/101/102、pur = 094/096）与本地实测 import 对账（`RecallTargetLocator`/`NcrReturnOrchestrator`/`ErpQaRecallGenerateReturnsProcessor` 三文件族）；(iii) A3 = bridge-test-128..132 作为 Phase 3 定位面；(iv) **B 退役义务 = bridge-main-057..060（cs 4 处，含 059 实值桥 ×2）+ bridge-main-085（mnt 1 处）+ cs 侧 retired test 兑付（bridge-test-114/117，`TestErpCsQualityEscalation`/`TestMockQaBizModels`）+ bridge-test-125 qa 半边（mnt `TestErpMntOee` qa 种子）作为 Phase 2/3 定位面**；(v) C1 = backward-184/185/186 与 C2 = backward-246/247/248 作为 Phase 2/3 定位面；(vi) 按 b2b/assets A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(pur|sal|prj|crm|drp|log|mfg)\.' module-quality/erp-qa-service/src/test module-quality/erp-qa-web/src/test` 排除 import 行 + test beans.xml ioc:type FQN——prj 覆盖本域先于 M2.7 执行的时序分支；起草实测：非 import 行命中 = `TestStubErpSalReturnBiz.java:88` 内联 FQN `List<app.erp.sal.biz.ErpSalExchangeDeliveryLine>`（已登记 A3 桩文件面内）+ `test-mock-sales/test-mock-posting/test-mock-purchase.beans.xml` ioc:type FQN ×6（sales 2 + posting 3 + purchase 1，mock 基建，目标属 sal/pur 未迁移域）——java 内联 1 + beans ioc:type 6 合计 7——**全部落于已登记 A3/mock 基建面，零补登**；执行时点复扫确认）。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-quality` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-quality/model/app-erp-quality.orm.xml` 逐行核对——仅 58 列 `stdDataType="long"→"string"`（自有 50 + md stub 8），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan qa 段重扫零 `NEEDS FIX`/零 `DEFERRED`。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案（含 FQN 盲区复扫结论 + B 义务定位面）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 58 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 落桥 + B 退役兑付

Status: planned
Targets: `module-quality/erp-qa-{dao,service}/src/main/java/**`（手写 IBiz/BizModel/Processor/posting/report）；**跨域编辑面：`module-cs/erp-cs-service/src/main/**`（4 桥接点）+ `module-maintenance/erp-mnt-service/src/main/**`（1 桥接点）**
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-quality/erp-qa-codegen,module-quality/erp-qa-dao,module-quality/erp-qa-meta,module-quality/erp-qa-service,module-quality/erp-qa-web,module-quality/erp-qa-app,module-quality/erp-qa-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`）触发增量重生成。预期：qa-dao `_gen` md 关系胶水（19 处）自 M1.1 登记中间态自愈。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 qa dao + service 手写代码类型错误（定位面：fin 2 文件 + inv 3 文件（C1，inv 已 String 直传按语境）+ md 1 文件（C1）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游；fin/inv posting 族 id 值流转以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接 11 处落桥（D4 消费协议）——qa String id ↔ sal/pur Long API 的调用点加转换桥（`RecallTargetLocator`/`NcrReturnOrchestrator`/`ErpQaRecallGenerateReturnsProcessor`——id 语境经变量流转处按编译器/grep 定位落 `ConvertHelper.toLong` 桥；**eq/filter 语义值桥主动识别**（Long 列传 String 静默空匹配——contract/cs/mnt 先例）；「方法未声明于接口文件」条目以晚域翻转时实际签名为准登记形态），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M2.6（sal 9）/M2.5（pur 2）；代码内 bridge 注释双向指针。
  - Skill: `nop-backend-dev`
- [ ] Fix: **B 退役义务兑付（main 5 条）**——qa IBiz 签名翻转 String 后：cs 侧 `ErpCsTicketEscalateToQualityProcessor` 059 实值桥 2 处 `ConvertHelper.toLong(...)` → String 直传 + bridge 注释移除，057/058/060 核证类型级/接口级零转换点或签名对齐；mnt 侧 `OeeCalculator` qa import（登记册 :10，live :11）核证零转换点（`in("relatedBillCode", codes)` 零 id 穿越）+ import 复核；cs 7 模块链 + mnt 7 模块链（各自显式列表、no-am、`-Dmaven.test.skip=true`）重建绿；cs/mnt grep 复核 qa 桥残留清零（M2.4 先例口径）。M2.4/M3.2 先例为执行范式。
  - Skill: `nop-backend-dev`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（reactor 仅 7 模块）；drp/mfg/pur/sal 对 qa 的引用破坏为已登记中间态（successor M3.7/M3.1/M2.5/M2.6），Phase 4 登记；未登记破坏按路线图规则 6 停止回报。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] qa 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案
- [ ] **B 义务：cs + mnt 7 模块链重建绿（main 口径）+ cs/mnt grep 桥残留清零**（cs/mnt test-compile 证明归 Phase 3 早域复跑——批内先例同形状处理）

### Phase 3 - 测试修复 + A3 桥接退役 + 快照重录 + 域级测试 + 早域测试复跑

Status: planned
Targets: `module-quality/**/src/test/**`、`module-quality/erp-qa-service/_cases/**`；早域复跑：`module-cs`、`module-maintenance` 测试；跨域测试编辑面：mnt `TestErpMntOee` + cs `TestMockQaBizModels`/`TestErpCsQualityEscalation`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——34 个测试类（+1 支撑件）的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；C2 后向 test 适配（backward-246 fin 2 + backward-247 inv 3 + backward-248 md 4 文件）；qa-web test-scope ast-dao/md-service（String）+ prj-dao（批内序 1 后 String 或 Long 陈旧 jar）编译对象零 id 穿越核验（`ErpQaWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
- [ ] Fix: A3 test 桥接适配（bridge-test-128..132，5 条）——Recall 测试局部桥（qa String ↔ sal/pur Long seed/断言转换或 mock 桩签名适配，与 Phase 2 桥接同型）+ TestStub* 桩适配（桩签名与未迁移 jar 一致性按编译器实测裁定：桩实现 sal/pur IBiz 接口（Long jar 接口），桩内 qa 侧值 String 化——cs「mock 桩保持 Long 签名 + 断言 ConvertHelper.toLong 桥」先例 vs mnt「mock 桩 String 化」先例，以接口归属域为准：接口属 sal/pur → 桩方法签名保持 Long，桩内转换），适配后在登记册退役对应 test 桥接条目（owner M2.3 = 本计划）。
  - Skill: `nop-testing`
- [ ] Fix: **cs 侧 retired test 兑付（bridge-test-114/117，M3.5 登记 owner M2.3）**——cs `TestMockQaBizModels` mock 桩 7 方法 `Long ncrId` 签名 → String（接口 `IErpQaNonConformanceBiz` 属 qa 翻转域——mnt 先例）+ `TestErpCsQualityEscalation` `MATERIAL_ID_LONG` 常量与 `assertEquals(Long.valueOf(8401L), ...)` 断言 → String 直传形态（059 桥退役对齐）；cs 测试编译与运行证明归早域复跑。
  - Skill: `nop-testing`
- [ ] Fix: **bridge-test-125 qa 半边回收**——mnt `TestErpMntOee` qa 侧种子（QaInspection）String 化 + 相关局部桥/注释复核（bridge-test-125 retired note 兑付；mnt 测试编译与运行证明归早域复跑）。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 qa service 测试 → 逐案审核 `_cases/` 新形态（791 文件基线；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正；「断言式 + 空 autotest.yaml」范式测试不录——cs 回退先例）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-quality/erp-qa-service,module-quality/erp-qa-web`（D3 口径：不带 `-am`）全绿——service 34 测试类（+1 支撑件）+ web BUILD SUCCESS（`ErpQaWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按 fin 修正版先例修复（test-scope VFS delta 带根元素 `x:extends="super"` + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`
- [ ] Proof: **早域测试复跑（B 义务验证）**——`mvn test -pl module-cs/erp-cs-service,module-cs/erp-cs-web` 全绿（**185/185 基线维持**，web 0 tests 治理排除）+ `mvn test -pl module-maintenance/erp-mnt-service,module-maintenance/erp-mnt-web` 全绿（**156/156 基线维持**，含 `TestErpMntOee` qa 半边回收后形态）。红则修复至绿（桥接退役遗留问题在本计划内闭环；IoC 第二环若新侧复现按 hr 先例 delta 处理并补记 bug 域清单）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] qa 域级测试全绿（service 34+1（35 个编译文件）+ web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案
- [ ] **早域复跑全绿（cs 185/185 + mnt 156/156 基线维持）；A3 5 条退役 + bridge-test-125 qa 半边 + bridge-test-114/117 cs 侧兑付在案**（登记册状态更新归 Phase 4 落盘）

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: planned
Targets: `module-quality/**`（手写代码 + qa-web 手写 page.yaml）、`docs/design/quality/recall.md`、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，qa 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M2.6/M2.5）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱专项（`getId\(\)\s*[<>]|comparing.*getId`——String 字典序，contract idOrder 先例）；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Fix: qa-web 手写 page.yaml raw-GraphQL `:Long` 变量 3 处就地 String 化——dashboard:190 + spc-chart:53/:102（`$chartId:Long` → `:String`，查询/mutation 变量与 `eq:{chartId:...}` 过滤链一致性核证）；随后 `rg ':Long' module-quality/erp-qa-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 类型变量如 `$lim:Int` 合法保留并逐条判定）；qa-web 重建 BUILD SUCCESS 验证。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-quality/erp-qa-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
- [ ] Add: 登记册状态更新——B main 桥接 5 条（bridge-main-057..060 + 085）→ retired（兑付 note：cs/mnt 侧移除/核证证据 + 链重建 + 复跑指针）；A3 test 桥接 5 条（bridge-test-128..132）→ retired（owner M2.3 兑付 note）；bridge-test-125 retired note 补 qa 半边兑付指针；**bridge-test-114/117 retired note 补 cs 侧兑付指针**；A2 main 桥接 11 条保持 active（退役 owner M2.6/M2.5）；被引用面确认（drp/mfg/pur/sal successor 指针已由 backward-pointer 登记）；fail-closed 解析验证通过（dry-run 正常消费 + qa 段 0 待改列）。
  - Skill: none
- [ ] Add: owner doc 注记——`docs/design/quality/recall.md:88` 类型桥陈述就地更新（`ErpQaRecall.batchId` Java 层已 String 化，桥接语义变更说明，引用本计划）；其余 qa 设计文档 grep 复核（零 Long id 陈述则记录结论）。
  - Skill: none
- [ ] Add: 路线图 M2.3 → `done`（M2/M3 表位次 13 + 头部「最后更新」；位次 14（manufacturing，批内序 3）qa 前置满足）+ 日志条目（含验证状态 + B 义务兑付 + 早域复跑基线）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外逐条核清 + 桥接例外清单在案）；page.yaml `:Long` 清零 + qa-web 重建绿 + view 零被动变更在案
- [ ] 路线图状态、登记册退役（B 5 条 + A3 5 条 + 125 半边兑付）、owner doc 注记、日志一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，技术/执行视角 plan-audit，ses_fd81ff84effen2KU9jKYocDd35）：`needs revision` — 0 BLOCKER / 3 MAJOR / 4 MINOR。事实核对大部属实（58 列构成/7 模块链/pom 依赖/A2 11 条 file:line 精确/A3 128..132/B main 5 条含 059 实值桥 ×2 与 057/058/060/085 零转换点核证/bridge-test-125 qa 半边实锤/C1-C2 文件清单/page.yaml 3/_cases 791/recall.md:88/md ×19 均验证通过）。MAJOR：① A2 sal/pur 拆分错误（sal 7/pur 4 → 实际 sal 9/pur 2——refDomain 逐条对账 + §6.15/§6.16 交叉证实）；② B 义务漏 cs 侧 retired test 兑付（bridge-test-117 `TestMockQaBizModels` 7 方法 Long ncrId 桩 + bridge-test-114 `TestErpCsQualityEscalation` Long 常量/断言——M3.5 登记 owner M2.3，qa 翻转破坏 cs test-compile）；③ Phase 1 (vi) FQN 复扫「起草实测零命中」失实（`TestStubErpSalReturnBiz.java:88` 内联 FQN + test beans.xml ioc:type 命中）。MINOR：④ notify 不在 qa main import 面（零命中）；⑤ B 点行号漂移未注记；⑥ 35 测试类 → 34+1 支撑件；⑦ FQN 正则漏 prj（先于 M2.7 执行时序分支）。
- Independent draft review iteration 2（2026-08-22，治理/规范视角，ses_fd8136414ffey4foiwZl50sRoA）：`needs revision` — 1 MAJOR / 2 MINOR。MAJOR：Draft Review Record 未记录 iteration 1（失实空节，违反指南执行时规则 1）。MINOR：① beans ioc:type FQN 计数 ×7 → ×6（+java 内联 1 = 合计 7）；② Phase 3「service 35 测试类/35 类」措辞与 34+1 定义漂移。治理检查其余全过（命名/路线图对齐位次 13 + 精确前置与 prj 非硬前置的诚实框架/保护区域协议/anti-slack/检查清单完整性/内部一致性独立复核/iteration-1 七项修订全部核实/模板合规）。
- **修订（iteration 1 → 2，已落地）**：全部 3 MAJOR + 4 MINOR 处理——① A2 全文改 sal 9/pur 2（Current Baseline/Goals/Phase 1(ii)/Phase 2/Deferred 五处）；② B 义务 + Goals + Phase 3 Targets/新增 cs 侧 retired test 兑付项（114/117）+ Phase 4 登记册 note + 退出标准/Closure Gates 补 114/117；③ FQN 复扫如实重述（命中全部落于已登记 A3/mock 基建面，零补登）；④ notify 移出 main import 面；⑤ 行号漂移注记；⑥ 34+1 口径；⑦ 正则加 prj。iteration 2 的 MAJOR + 2 MINOR 已就地修正（本节记录 + 计数 + 措辞）。
- **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
  - 批准 1（治理视角）：ses_fd8136414ffey4foiwZl50sRoA，2026-08-22 — 批准 orm 变更面（qa 58 列 stdDataType-only、stdSqlType BIGINT 保持、delVersion 不动、A1 = 0——工具核证 58/58），条件 = MAJOR-1（本节如实记录）落地（已落地）+ MINOR 文书修正（已落地）+ 回写前取得技术侧第二独立批准。
  - 批准 2（技术视角）：ses_fd81ff84effen2KU9jKYocDd35 iteration 1 为 `needs revision`；iteration 3 复核（ses_fd80e8bcbffeMggNsjdQQ7hR23，2026-08-22）确认全部 3 MAJOR + 4 MINOR（iter 1）+ 1 MAJOR + 2 MINOR（iter 2）RESOLVED（逐项 live 复验：A2 拆分 9/2 五处一致、114/117 七个落点齐备、FQN 命中 7 = java 1 + beans 6 精确复现、漂移行号 :10/:154/:173-174/:187/:195 与 :11 核实）+ 零新缺陷 → **技术视角批准生效（无条件）**。
- Independent draft review iteration 3（2026-08-22，技术侧复核，ses_fd80e8bcbffeMggNsjdQQ7hR23）：`passes draft review` — 全部发现 RESOLVED、零新增缺陷、双批准齐备后可转 `active` 并进入 Phase 1 回写。
- 共识达成（2026-08-22）：iteration 3 全部发现 RESOLVED + 双批准落盘（治理批准经 iteration 2 条件落地生效；技术批准经 iteration 3 生效）→ 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（58 列落源 + no-am 7 模块重生成 + 手写代码/测试修复 + A2 落桥 11 + B 退役 5 与 cs 侧 retired test 兑付（114/117）+ bridge-test-125 qa 半边回收 + 快照重录 + grep 门控清零 + page.yaml 3 处 Fix）
- [ ] 相关文档对齐（owner doc 注记（recall.md:88）、路线图 M2.3 状态、登记册退役（B 5 + A3 5）、日志）
- [ ] 已运行验证：`mvn clean install -pl module-quality/erp-qa-{codegen,dao,meta,service,web,app,api} -DskipTests` 全绿 + `mvn test -pl module-quality/erp-qa-service,module-quality/erp-qa-web` 全绿 + **早域复跑 cs 185/185 + mnt 156/156** + 工具重扫零残留（qa 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2 main 桥接 11 处（sal 9 + pur 2，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——sal（位次 16）/pur（位次 15）未迁移，桥接点为编译必需
- Successor Required: `yes`（M2.6 回收 sal 9 条；M2.5 回收 pur 2 条——晚域翻转时退役条目并移除本域桥接点）

### drp/mfg/pur/sal 对 qa 的引用破坏（main 2+2+2+2 / test 2+1+1+1 文件）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册 backward-pointer 预先登记的中间态（本计划 no-am reactor 不含这些域；被引用清单 §6.13 在案）
- Successor Required: `yes`（M3.7/M3.1/M2.5/M2.6 plan Phase 2/3 + M4.1 兜底）

### `ErpQaWebPagesTest` 页面校验

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
