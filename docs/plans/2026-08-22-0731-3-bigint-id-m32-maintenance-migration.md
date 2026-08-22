# 2026-08-22-0731-3-bigint-id-m32-maintenance-migration 主键/外键 string 化 M3.2：maintenance 域迁移（冻结序位次 11）

> Plan Status: active（2026-08-22：iteration 1-2 独立草案审查收敛 + 保护区域双独立子 agent 批准（治理批准经 iteration 2 MAJOR-1 RESOLVED 生效），见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M3.2（maintenance，冻结序位次 11）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 11（M3.2）
> Related: `docs/plans/2026-08-22-0731-2-bigint-id-m22-inventory-migration.md`（批内序 2，本计划硬前置）、`docs/plans/2026-08-22-0002-2-bigint-id-m24-assets-migration.md`（B 退役义务先例 + ast 桥接落桥来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **mnt 域规模（2026-08-22 实况 scan）**：`module-maintenance/model/app-erp-maintenance.orm.xml` 需改列 **64 = 自有 50（15 实体：PK 15 + BIGINT FK 35，含 `orgId` FK 自有 5 + md stub 2（`ErpMdEmployee.orgId`/`ErpMdWarehouse.orgId`，计入 stub 14 内））+ notGenCode stub 14（md 13：`ErpMdOrganization`/`ErpMdLocation`/`ErpMdMaterialCategory`/`ErpMdEmployee`/`ErpMdMaterial`/`ErpMdUoM`/`ErpMdWarehouse` 7 stub 实体含 stub 内 FK 列 6——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐；ast 1：`ErpAstAsset.id`——ast 权威源自 M2.4 已 String，翻转 = 与权威源对齐）**。**不改列**：`delVersion` 及 `ErpMntVisit.assignedTo`/`completedBy`（孤儿操作人列，非 PK/FK BIGINT，规则 4 保持 long——孤儿操作人列建模问题已另行登记 follow-up）等非 PK/FK BIGINT 列；登记册 A1 延后 = 0。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-maintenance/erp-mnt-{codegen,dao,meta,service,web,app,api}`（全链构建，无延后）。`erp-mnt-service` main compile 依赖 **inv-dao（批内序 2 M2.2 后 String 新 jar——本计划硬前置）** + fin-service（M2.1 起 String）+ **notify-dao（M1.2 起 String）** + **mfg-dao + qa-dao（未迁移 Long jar——A2 桥接对象）** + common-service + nop-report-core/nop-report-pdf（报表 PDF compile 输出）；test-scope 依赖 inv-service（pom 注释：generateMove Bean 实现使测试可注入运行）+ md-service + notify-service。`erp-mnt-dao` compile 依赖 md-dao（String——mnt orm `refEntityName` md ×13，M1.1 登记的 `_gen` 关系胶水中间态**本域迁移即自愈**）+ **ast-dao（M2.4 起 String 新 jar——mnt orm `refEntityName` ast ×1（`ErpMntEquipment.assetId` → `ErpAstAsset`），M2.4 时 ast-dao 翻转后本域 `_gen` ast 胶水已随 ast jar 重编译自愈，本域 stub 翻转后完全一致）**；mnt-web test-scope 依赖 md-service/ast-dao（String）/prj-dao（Long 陈旧 jar，页面测试编译对象——预期零 id 穿越，Phase 3 核验）；mnt-app 域级 web 依赖仅 md-web（M1.1 起 String，无跨迁移边，可建）。
- **M0.2 登记册 mnt 视角（§6.11 + json5 实测对账，起草已核）**：A1 = 0；**A2 main 桥接 6 条**（mfg 5：bridge-main-080..084 `OeeCalculator:3/:4/:5/:6/:7`（`ErpMfgJobCard`/`JobCardTimeLog`/`WorkOrder`/`WorkcenterCalendar`/`WorkcenterCapacity` 类型级引用），退役 owner M3.1；qa 1：bridge-main-085 `OeeCalculator:10`（`ErpQaInspection` 类型级引用），退役 owner M2.3）——全部集中于单一文件 `support/OeeCalculator.java`（RC-R1.78 mnt OEE 引擎）；**A3 test 桥接 1 条**（bridge-test-125：`TestErpMntOee` 引用 mfg 5 实体 + qa 1 实体，owner M3.2 = 本计划 Phase 3 退役）；**B 退役/翻转义务（作为晚域，main 2 条 + retired test mock 回收）**：翻转 `IErpMntEquipmentBiz` 签名时退役 bridge-main-024/025（早域 assets，`ErpAstDisposalProcessor:266/:274` `ConvertHelper.toLong(asset.getId())`/`toLong(disposal.getAssetId())` 实值转换桥——**ast 侧桥接点代码移除 + ast 7 模块链重建绿 + ast 测试复跑（320/320 基线）为本计划义务（M2.4 fin 侧先例）**）；**retired test bridge 回收（bridge-test-107/134，note「晚域 M3.2 翻转时随 mock 一并回收」）**：ast 测试侧 `TestMockMntBizModels`（mock 桩保持 mnt Long 签名——翻转 String）+ `test-mock-mnt.beans.xml`（ioc:type FQN 不变）+ `TestErpAstDisposalEquipmentLinkage`（断言侧 ConvertHelper.toLong 桥——翻转 String 直传）；C1 后向 main 4 条（backward-164 → fin 3 文件：posting `MaintenanceIssuePostingDispatcher`/`MaintenanceLaborPostingDispatcher`/`MntPostingExecutor`；backward-165 → inv 5 文件：posting/processor `AbstractErpMntSparePartUsageProcessor`/`ErpMntSparePartUsageConfirmProcessor`/`ErpMntSparePartUsageReverseConfirmProcessor`/`SparePartIssueService`；backward-166 → md 2 文件；backward-167 → notify 3 文件）；C2 后向 test 4 条（backward-225 → fin 7 测试文件；backward-226 → inv 4 测试文件（批内序 2 兑付）；backward-227 → md 6 测试文件；backward-228 → notify 1 测试文件）。
- **被引用面（登记册 §6.11 + rg 实测吻合）**：mfg main 1 文件（`ErpMfgScheduleToJobCardProcessor`）+ mfg test 1 文件（`TestErpMfgJobCardDowntimeGate`）——位次 14 未迁移域引用 mnt Long API 的编译破坏为**已登记中间态**（successor M3.1 + M4.1 兜底），不在本计划 no-am reactor 内、不影响本计划构建。
- **手写代码冲击面（实测）**：mnt-service main 跨域 import = fin 23 处 + inv 10 处（C1 定位面；fin/inv posting 族 `PostingEvent`/`AcctDocContext` Long id 字段流转按 M2.4 盲区先例以编译器清单为准）+ md 5 + notify 3（C1）+ mfg 5 / qa 1（A2 桥接点 `OeeCalculator`）；dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 本域语义 FK Long 参数清单为 Phase 4 门控输入）。
- **测试资产（实测）**：mnt-service **27 个测试类**；`_cases` 快照 **619 文件**；`ErpMntWebPagesTest` `@Tag("full-app")` + surefire excludedGroups 模块级排除（已提交治理决策，successor = M4.1，参与 test-compile）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 4 处，迁移即失效的实时缺陷）**：`erp/mnt/pages/visit-wizard/main.page.yaml:70/:161/:270/:291`（`$vid:Long` ×4 → `ErpMntVisit__get` 查询变量 + `ErpMntVisit__start/complete/cancel(visitId:$vid)` mutation 变量）——mnt 翻转 String 后静态类型不匹配 → adaptor 静默降级、拜访向导全流程失效（M3.6 结束审计 MAJOR-1 同型）——**本计划就地 Fix + mnt-web 重建验证**（mnt-web 在本计划 7 模块 verify 范围内）。
- **owner doc 已知 Long 陈述（Phase 4 注记对象，起草实测）**：`docs/design/maintenance/equipment-integration.md:148`——`priorStatusCache`（`ConcurrentHashMap<Long,String>` 包级可见，键 = 设备 id Long）为设备状态前态缓存实现注记；mnt 翻转后键类型 String 化（`Map<Long` 语义陷阱 grep 门控将命中，属代码 Fix + 文档注记双重对象）。
- **已知风险（先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——八域中 cs 未复现（先例 delta 在位）；mnt 若复现按 fin 修正版先例落 test-scope VFS delta（根元素带 `x:extends="super"`）+ DeltaOverride delta-layer 补 default 层集；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 mfg-dao/qa-dao Long jar——A2 桥接登记例外；prj-dao test-scope 编译对象）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-maintenance` 新鲜度门控（零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：mnt orm 64 列全 `stdDataType="long"` 待改；mnt 手写代码/测试/快照全部 Long 形态；早域 ast 桥接点 2 处待退役；冻结序位次 11（位次 12 prj 归后续批次）。

## Goals

- mnt 域 64 列（自有 50 + md stub 13 + ast stub 1）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 mnt 全部手写代码 + A2 前向桥接 6 处落桥（mfg 5 + qa 1，退役 owner M3.1/M2.3）。
- **B 退役义务兑付（main 2 条 + retired test mock 回收）**：ast 侧桥接点 2 处移除（`ErpAstDisposalProcessor:266/:274` toLong 桥 → String 直传）+ ast test mock 回收（`TestMockMntBizModels`/`TestErpAstDisposalEquipmentLinkage`）+ ast 7 模块链重建绿 + ast 测试复跑（320/320）；登记册 2 条 → retired。
- 快照每域重录（RECORDING→CHECKING；619 文件基线）。
- 语义陷阱 grep 门控清零（含 `priorStatusCache` `Map<Long,String>` 键 String 化）+ page.yaml `:Long` 4 处就地 Fix（mnt-web 重建验证）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘，C1/C2 修复定位面消费（含 backward-165/226 批内序 2 inv 兑付），heal M1.1 登记的 mnt-dao `_gen` md 胶水中间态。
- 路线图 M3.2 → `done` + 日志；位次 12（prj）解锁（供后续批次）。

## Non-Goals

- 不迁移 manufacturing/quality 域（A2 桥接目标域，归 M3.1/M2.3）；不动 mfg/qa orm 与生成件（桥接仅触及本域手写代码）。
- 不修复 mfg 对 mnt 的引用破坏（`ErpMfgScheduleToJobCardProcessor` main + `TestErpMfgJobCardDowntimeGate` test——已登记中间态，successor M3.1；本计划仅在 Phase 4 登记确认）。
- 不改 `delVersion`/`assignedTo`/`completedBy` 等非 PK/FK BIGINT 列（保持 long；孤儿操作人列建模问题另案已登记）；不修 `ErpMntWebPagesTest` 治理排除（successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。
- 不修 md/notify/aps/b2b/contract 五域 IoC delta（不在触碰面，归各域 plan 触碰时或 M4.1 统一回收——bug `docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`；aps 已由批内序 2 消费，本计划执行时点余四域）。
- 不动 fin web/app 补做与 prj 域（归 M2.7 后续批次）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更 + 跨域桥接退役编辑面）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 11 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；mnt 业务语义 owner doc = `docs/design/maintenance/`（Phase 4 注记对象，已知 1 处）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install + 位次 3-10 链 install（aps/b2b/contract/fin/ast/cs/hr/**inv——本计划硬前置含 M2.2 inv-dao/inv-service String 新 jar**）。回滚策略：revert orm.xml + `mvn clean install -pl module-maintenance/erp-mnt-codegen,module-maintenance/erp-mnt-dao,module-maintenance/erp-mnt-meta,module-maintenance/erp-mnt-service,module-maintenance/erp-mnt-web,module-maintenance/erp-mnt-app,module-maintenance/erp-mnt-api -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 2/3 完成后回滚需先 revert 早域桥接退役与测试代码**——ast 桥接点已移除形态对 Long mnt jar 不可编译）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-maintenance/model/app-erp-maintenance.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M2.2 ✅（批内序 2 完成且 inv-dao/inv-service 已 install String 形态）+ M2.1 ✅ + M1.1 ✅ + M1.2 ✅（精确前置满足）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.11 mnt 节，逐条核对：(i) A1 = 0（64 列全翻转）；(ii) A2 = bridge-main-080..085（mfg 5 + qa 1，全部位于 `OeeCalculator.java`）与本地实测 import 对账；(iii) A3 = bridge-test-125 作为 Phase 3 定位面；(iv) **B 退役义务 = bridge-main-024/025（ast `ErpAstDisposalProcessor:266/:274`）+ retired bridge-test-107/134 mock 回收（`TestMockMntBizModels`/`test-mock-mnt.beans.xml`/`TestErpAstDisposalEquipmentLinkage`）作为 Phase 2/3 定位面**；(v) C1 = backward-164/165/166/167 与 C2 = backward-225/226/227/228 作为 Phase 2/3 定位面（backward-165/226 为批内序 2（M2.2）登记的 successor 兑付面）；(vi) 按 b2b/assets A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(prj|pur|sal|qa|crm|drp|log|mfg)\.' module-maintenance/erp-mnt-service/src/test module-maintenance/erp-mnt-web/src/test` 排除 import 行 + test beans.xml ioc:type FQN——覆盖本域执行时点全部未迁移晚域；起草实测零命中，执行时点复扫确认）。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-maintenance` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-maintenance/model/app-erp-maintenance.orm.xml` 逐行核对——仅 64 列 `stdDataType="long"→"string"`（自有 50 + md stub 13 + ast stub 1），`stdSqlType` 零变化、`delVersion`/`assignedTo`/`completedBy`/标签结构零变化；scan mnt 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案（含 FQN 盲区复扫结论 + B 义务定位面）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 64 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 落桥 + B 退役兑付

Status: planned
Targets: `module-maintenance/erp-mnt-{dao,service}/src/main/java/**`（手写 IBiz/BizModel/Processor/Support/SPI）；**跨域编辑面：`module-assets/erp-ast-service/src/{main,test}/**`（2 main 桥接点 + test mock 回收）**
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-maintenance/erp-mnt-codegen,module-maintenance/erp-mnt-dao,module-maintenance/erp-mnt-meta,module-maintenance/erp-mnt-service,module-maintenance/erp-mnt-web,module-maintenance/erp-mnt-app,module-maintenance/erp-mnt-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`、`-Dmaven.test.skip=true`）触发增量重生成。预期：mnt-dao `_gen` md 关系胶水自愈（M1.1 登记中间态）；ast 胶水随 stub 翻转与 ast-dao String jar 完全一致。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 mnt dao + service 手写代码类型错误（定位面：fin 3 文件（C1）+ inv 5 文件（C1，inv 已 String 直传按语境）+ md 2 文件 + notify 3 文件（C1，签名不变预期零破坏核验）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游；fin/inv posting 族 id 值流转以编译器实际清单为准；**`EquipmentStatusLinker.priorStatusCache` `ConcurrentHashMap<Long,String>` 键 String 化**——equipment-integration.md:148 实现注记对象），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接 6 处落桥（D4 消费协议）——mnt String id ↔ mfg/qa Long API 的调用点加转换桥（mfg 5 + qa 1，全部位于 `OeeCalculator.java`：`ErpMfgJobCard`/`JobCardTimeLog`/`WorkOrder`/`WorkcenterCalendar`/`WorkcenterCapacity`/`ErpQaInspection` 类型级引用——id 语境经变量流转处按编译器/域 plan grep 定位落 `ConvertHelper.toLong` 桥；**eq/filter 语义值桥主动识别**（Long 列传 String 静默空匹配，contract 037/038 + cs 059 先例）），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M3.1（mfg 5）/M2.3（qa 1）；代码内 bridge 注释双向指针。
  - Skill: `nop-backend-dev`
- [ ] Fix: **B 退役义务兑付（main 2 条）**——mnt `IErpMntEquipmentBiz.changeStatusForAssetDisposal/restoreFromAssetDisposal(assetId)` 签名翻转 String 后，ast 侧实值桥错型：`ErpAstDisposalProcessor:266/:274` `ConvertHelper.toLong(asset.getId())`/`toLong(disposal.getAssetId())` → String 直传，移除 :265/:273 bridge 注释；ast 7 模块链（`module-assets/erp-ast-{codegen,dao,meta,service,web,app,api}` no-am、`-Dmaven.test.skip=true`）重建绿；ast grep 复核 mnt 桥残留清零（M2.4 先例口径）。M2.4 assets 先例（mission 首例晚域退役早域桥接点）为执行范式。
  - Skill: `nop-backend-dev`
- [ ] Fix: **B 退役义务兑付（retired test mock 回收）**——ast 测试侧 mnt 桩翻转 String（`TestMockMntBizModels` mock 实现 `IErpMntEquipmentBiz` Long 桩签名 → String + `TestErpAstDisposalEquipmentLinkage` 断言侧 ConvertHelper.toLong 桥 → String 直传；`test-mock-mnt.beans.xml` ioc:type FQN 类名不变零改动核验）——retired bridge-test-107/134 note 兑付。
  - Skill: `nop-testing`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（7 模块全绿，reactor 不含外域模块）；mfg 对 mnt 的引用破坏（main 1 + test 1 文件）为已登记中间态（successor M3.1），Phase 4 登记；未登记破坏按路线图规则 6 停止回报。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] mnt 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案
- [ ] **B 义务：ast 7 模块链重建绿（main 口径 `-Dmaven.test.skip=true`）+ ast grep 桥残留清零**（mock 回收后的 ast test-compile 证明归 Phase 3 早域复跑——批内序 2 对 aps 同形状处理）

### Phase 3 - 测试修复 + A3 桥接退役 + 快照重录 + 域级测试 + 早域测试复跑

Status: planned
Targets: `module-maintenance/**/src/test/**`、`module-maintenance/erp-mnt-service/_cases/**`；早域复跑：`module-assets` 测试
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——27 个测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；C2 后向 test 适配（backward-225 fin 7 + backward-226 inv 4（批内序 2 登记的 successor 兑付）+ backward-227 md 6 + backward-228 notify 1 文件）；mnt-web test-scope ast-dao（String）/prj-dao（陈旧 Long jar）编译对象零 id 穿越核验（`ErpMntWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
- [ ] Fix: A3 test 桥接适配（bridge-test-125，1 条）——`TestErpMntOee` 引用 mfg 5 实体 + qa 1 实体的 id 形态桥接（String↔Long 局部转换或 mock 桩签名适配，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M3.2 = 本计划）。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 mnt service 测试 → 逐案审核 `_cases/` 新形态（619 文件基线；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正；**「断言式 + 空 autotest.yaml」范式测试不录快照——cs 81 方法目录超录回退先例**）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-maintenance/erp-mnt-service,module-maintenance/erp-mnt-web`（D3 口径：不带 `-am`）全绿——service 27 测试类 + web BUILD SUCCESS（`ErpMntWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按 fin 修正版先例修复（test-scope VFS delta 带根元素 `x:extends="super"` + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`
- [ ] Proof: **早域测试复跑（B 义务验证）**——`mvn test -pl module-assets/erp-ast-service,module-assets/erp-ast-web` 全绿（320/320 基线，web 0 tests 治理排除；含 mock 回收后形态）。红则修复至绿（桥接退役遗留问题在本计划内闭环）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] mnt 域级测试全绿（service 27 类 + web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案
- [ ] **早域复跑全绿（ast 320/320 基线维持）；A3 1 条退役在案**

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: planned
Targets: `module-maintenance/**`（手写代码 + mnt-web 手写 page.yaml）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，mnt 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（`Map<Long` 含 `priorStatusCache` 键 String 化核验；A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M3.1/M2.3）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱（String 字典序，contract idOrder 先例）专项 grep：`getId\(\)\s*[<>]|comparing.*getId`；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Fix: mnt-web 手写 page.yaml raw-GraphQL `:Long` 变量 4 处就地 String 化——`visit-wizard/main.page.yaml:70/:161/:270/:291`（`$vid:Long` → `:String`，visitId 查询/mutation 变量）；variables 链与 options value 链一致性核证（contract version-diff 先例）；随后 `rg ':Long' module-maintenance/erp-mnt-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 类型变量如 `$lim:Int` 合法保留并逐条判定）；mnt-web 重建 BUILD SUCCESS 验证。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-maintenance/erp-mnt-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
- [ ] Add: 登记册状态更新——B main 桥接 2 条（bridge-main-024/025）→ retired（兑付 note：ast 侧移除证据 + 链重建 + 测试复跑 + mock 回收指针）；A3 test 桥接 1 条（bridge-test-125）→ retired（owner M3.2 兑付 note）；A2 main 桥接 6 条保持 active（退役 owner M3.1（mfg 5）/M2.3（qa 1））；被引用面确认（mfg 2 文件 successor 指针已由 backward-pointer 登记）；登记册消费工具 fail-closed 解析验证通过（dry-run 正常消费 + mnt 段 0 待改列）。
  - Skill: none
- [ ] Add: owner doc 注记——`docs/design/maintenance/equipment-integration.md:148` `priorStatusCache` `ConcurrentHashMap<Long,String>` 陈述就地注记 Java 层已 String 化（引用本计划）；其余 mnt 设计文档 grep 复核（零 Long id 陈述则记录结论）。
  - Skill: none
- [ ] Add: 路线图 M3.2 → `done`（M2/M3 表位次 11 + 头部「最后更新」；位次 12 prj 解锁——供后续批次）+ 日志条目（含验证状态 + B 义务兑付）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外逐条核清 + 桥接例外清单在案 + priorStatusCache 键 String 化核验）；page.yaml `:Long` 清零 + mnt-web 重建绿 + view 零被动变更在案
- [ ] 路线图状态、登记册退役（B 2 条 + A3 1 条）、日志一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fd94d6b83ffe9pO64KerPOnxfm）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR。事实核对全部属实（64 列构成逐列吻合；`ErpMntVisit.assignedTo/completedBy` 孤儿操作人列正确排除；A2 080..085 全部位于 `OeeCalculator.java:3/:4/:5/:6/:7/:10` **逐行精确**；ast 桥接点实况行号**精确优于登记册**（调用 :266/:274、注释 :265/:273 原文核实；Long 桩在 erp-mnt-dao :26/:35）；retired 107/134 note + mock 文件全部在位；pom 全吻合；27/619 吻合；page.yaml 4 处唯一非 `_gen` ✓；`equipment-integration.md:148` + `EquipmentStatusLinker.java:51` 实况核实；硬前置 M2.2 批内序正确；ast 7 链 + 320/320 基线核实）。MINOR：① orgId ×7 归属口径错 2（自有 5 + md stub 2）；② mnt-app 有 md-web compile（erp-mnt-app/pom.xml:36）；③ Phase 2 退出「含 test mock 回收后 test-compile」与 `-Dmaven.test.skip=true` 矛盾。
  - 审查者 B（治理/规范视角，ses_fd94d2446ffeFbAMaE2TINLbRo）：`needs revision` — 0 BLOCKER / **1 MAJOR** / 1 MINOR。**MAJOR-1**：Phase 2 退出标准「ast 7 模块链重建绿（含 test mock 回收后 test-compile）」与声明命令 `-Dmaven.test.skip=true`（跳过 test 编译）矛盾——mock 回收项编辑的正是 ast 测试文件，按字面执行此门控在 Phase 2 不可验证，违反「退出标准必须可验证」契约（修复任选：删括注依赖 Phase 3 复跑（plan 2 对 aps 同形状先例在仓）或加显式 test-compile 局部检查）。MINOR：① 全文未引用 bug doc 路径但 Non-Goals 断言「五（四）域」依赖 plan 2 范围无 traceability 指针。其余治理检查全过（B 义务与登记册吻合且行号与 live 代码逐行精确；backward 8 条文件数逐一吻合；priorStatusCache 双重对象登记清晰；Deferred 四条分类合法）。
  - **修订（iteration 1 → 2，已落地）**：MAJOR-1 采用修复①（删括注，Phase 2 退出改 main 口径 + 「mock 回收后的 ast test-compile 证明归 Phase 3 早域复跑——批内序 2 对 aps 同形状处理」）；MINOR 三项处理——orgId 改「自有 5 + md stub 2（ErpMdEmployee.orgId/ErpMdWarehouse.orgId）」；mnt-app 改「域级 web 依赖仅 md-web」；Non-Goals 补 bug doc 路径 + 改「五域（本计划执行时点余四域）」。
- Independent draft review iteration 2（2026-08-22，独立复审 ses_fd942df62ffeOUGpNQplCRvJR5）：`passes draft review` — MAJOR-1 + 全部 MINOR RESOLVED（逐项 live 复验：mnt orm `:818` orgId 确在 ErpMdEmployee 块（812-822）、`:852` 确在 ErpMdWarehouse 块（845-855）；mnt-app pom:36 md-web compile；Phase 2 退出与 Phase 3 复跑项一致性与 plan 2 aps 形状镜像核验；bug doc 存在性与跨计划一致性），零新缺陷，列算术复核（15+35+14=64）通过。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fd94d6b83ffe9pO64KerPOnxfm，2026-08-22 — 「批准 M3.2 mnt orm 保护区域变更（技术视角批准）」。依据：64 列（md stub 13 与 M1.1 对齐 + ast stub 1 与 M2.4 对齐）、孤儿操作人列正确排除、B 义务 2 条 + mock 回收与登记册 retired note 逐字对账。
    - 批准 2（治理视角）：ses_fd94d2446ffeFbAMaE2TINLbRo，2026-08-22 — iteration 1 有条件批准（「修订 MAJOR-1 并顺手处理 MINOR-1 后……可直接随下一轮复审通过」）；iteration 2 复核 MAJOR-1 RESOLVED → **治理视角批准生效**。
- 共识达成（2026-08-22）：iteration 2 全部发现 RESOLVED + 双批准落盘（治理批准经 iteration 2 生效）→ 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（64 列落源 + no-am 7 模块重生成 + 手写代码/测试修复 + A2 落桥 6 + B 退役 2 与 mock 回收 + 快照重录 + grep 门控清零 + page.yaml 4 处 Fix）
- [ ] 相关文档对齐（owner doc 注记（equipment-integration.md:148）、路线图 M3.2 状态、登记册退役（B 2 + A3 1）、日志）
- [ ] 已运行验证：`mvn clean install -pl module-maintenance/erp-mnt-{codegen,dao,meta,service,web,app,api} -DskipTests` 全绿 + `mvn test -pl module-maintenance/erp-mnt-service,module-maintenance/erp-mnt-web` 全绿 + **早域复跑 ast 320/320** + 工具重扫零残留（mnt 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2 main 桥接 6 处（mfg 5 + qa 1，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——mfg（位次 14）/qa（位次 13）未迁移，桥接点为编译必需
- Successor Required: `yes`（M3.1 回收 mfg 5 条；M2.3 回收 qa 1 条——晚域翻转时退役条目并移除本域桥接点）

### mfg 对 mnt 的引用破坏（main 1 + test 1 文件）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册 backward-pointer 预先登记的中间态（本计划 no-am reactor 不含 mfg；被引用清单 §6.11 在案）
- Successor Required: `yes`（M3.1 plan Phase 2/3 + M4.1 兜底）

### `ErpMntWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### 平台 IoC 回归 delta（若 Phase 3 复现并落盘）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 `nopSequenceGenerator` bean-init-self-wait 为已登记平台 Bug，先例修复 = test-scope VFS delta（fin 修正版带 `x:extends="super"`）
- Successor Required: `yes`（平台修复后统一移除，M4.1 复核）

## Closure

Status Note: （待执行）

Closure Audit Evidence:

（待执行后由独立结束审计填充）

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录。）
