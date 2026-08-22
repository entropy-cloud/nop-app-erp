# 2026-08-22-0731-2-bigint-id-m22-inventory-migration 主键/外键 string 化 M2.2：inventory 域迁移（冻结序位次 10）

> Plan Status: active（2026-08-22：iteration 1-2 独立草案审查收敛 + 保护区域双独立子 agent 批准，见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M2.2（inventory，冻结序位次 10）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 10（M2.2）
> Related: `docs/plans/2026-08-22-0731-1-bigint-id-m33-hr-migration.md`（批内序 1，冻结序先行）、`docs/plans/2026-08-22-0002-2-bigint-id-m24-assets-migration.md`（**B 退役义务先例**：mission 首例晚域退役早域桥接点）、`docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`（fin 侧桥接落桥来源）、`docs/plans/2026-08-21-2025-1-bigint-id-m39-aps-migration.md`（aps 侧桥接落桥来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）、`docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`（Deferred 消费：aps delta 修复）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **inv 域规模（2026-08-22 实况 scan）**：`module-inventory/model/app-erp-inventory.orm.xml` 需改列 **136 = 自有 120（21 实体：PK 21 + BIGINT FK 99，含 `orgId` FK 自有 13 + md stub 1（`ErpMdWarehouse.orgId`，计入 stub 16 内））+ notGenCode md stub 16（10 stub 实体：`ErpMdMaterial`/`ErpMdMaterialSku`/`ErpMdWarehouse`/`ErpMdLocation`/`ErpMdUoM`/`ErpMdCurrency`/`ErpMdOrganization`/`ErpMdAcctSchema`/`ErpMdPartner`/`ErpMdEmployee`，含 stub 内 FK 列 6——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐）**。**不改列**：`delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）；登记册 A1 延后 = 0（全仓 orm-deferral 8 条均为 fin 6 + hr 2，无 inv）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-inventory/erp-inv-{codegen,dao,meta,service,web,app,api}`（全链构建，无延后）。`erp-inv-service` main compile 依赖 **mfg-dao + pur-dao（未迁移 Long jar——A2 桥接对象）** + fin-service（M2.1 起 String）+ common-service；test-scope 依赖 md-service（pom 注释：注册跨域业务对象使 GraphQL save 跨域引用校验可通过）+ ast-dao（M2.4 起 String 新 jar）+ prj-dao（Long 陈旧 jar，页面测试编译对象——预期零 id 穿越，Phase 3 核验）+ notify-service + **jqwik（property-based 测试在列）**。`erp-inv-dao` compile 依赖 md-dao（String——inv orm `refEntityName` md ×85，M1.1 登记的 `_gen` 关系胶水中间态**本域迁移即自愈**）；inv-app 域级 web 依赖仅 md-web（M1.1 起 String，无跨迁移边，可建）。
- **M0.2 登记册 inv 视角（§6.10 + json5 实测对账，起草已核）**：A1 = 0；**A2 main 桥接 9 条**（mfg 4：bridge-main-071/072 `CostAdjustmentService:13/:14` + 073/074 `StandardCostResolver:7/:8`（`ErpMfgCostRollup`/`ErpMfgCostRollupLine` 类型级引用）；pur 5：bridge-main-075/076 `ErpInvLandedCostApproveProcessor:11/:12` + 077 `ErpInvLandedCostGenerateFreightLandedCostProcessor:5` + 078/079 `ErpInvLandedCostProcessor:14/:15`（`ErpPurReceive`/`ErpPurReceiveLine`，078/079 符号使用行含 id 语境）；退役 owner M3.1（mfg）/M2.5（pur））；**A3 test 桥接 6 条**（bridge-test-119..124：`TestErpInvCostAdjust`/`TestErpInvLandedCostEndToEnd`/`TestErpInvLandedCostReversal`/`TestErpInvStandardCosting`/`processor/TestErpInvLandedCostAllocatedGuard`/`processor/TestErpInvLandedCostReceiveMutex` 引用 mfg/pur 实体，owner M2.2 = 本计划 Phase 3 退役）；**B 退役/翻转义务（作为晚域，main 9 条 + retired test 回收）**：翻转 inv IBiz 参数签名时退役 bridge-main-009/010/011（早域 aps，`ErpApsAtpCtpServiceImpl:12/:13/:14` + 转换点 :55/:65/:77）+ 016（早域 aps，`ErpApsAutoDispatchProcessor:10`，inv 语境转换点 :322 附近）+ 062/064/066/067/068（早域 finance，`ErpFinAccountingPeriodProcessor`——登记册 callSite :15/:16/:536/:537/:544 已随代码漂移 2 行，**实况 FQN 使用行 :15/:16/:538/:539/:546，064 语义级转换点调用 `reclosePeriodCosts(ConvertHelper.toLong(period.getId()),...)` 在 :224（:223 为 bridge 注释行），执行以时点 FQN 定位为准**）——**aps 侧桥接点代码移除 + aps 7 模块链重建绿 + aps 测试复跑为本计划义务（M2.4 fin 侧先例）**；**fin 侧桥接点移除 + fin 5 模块链（codegen,dao,meta,service,api——web/app 延后 M2.7）重建绿 + fin-service 测试复跑（497/497 基线）为本计划义务**；**retired test bridge 回收（bridge-test-103..106，note「晚域 M2.2/M3.1 翻转时移除桥接点」）**：aps 4 测试文件（`TestErpApsAutoDispatch`/`TestErpApsCrossDomainIntegration`/`TestErpApsDemandPlanning`/`TestErpApsWorkOrderToOperationOrder`）的 inv 侧 `String.valueOf` 桥移除 + inv 种子 Long→String（**mfg 侧桥与种子保持 Long——归 M3.1**），随 aps 测试复跑验证；C1 后向 main 3 条（backward-157 → fin 4 文件：posting `CostAdjustmentPostingDispatcher`/`InvPostingExecutor`/`LandedCostPostingDispatcher` + `ErpInvTransferOrderConfirmProcessor`；backward-158 → md 6 文件；backward-159 → notify 2 文件）；C2 后向 test 3 条（backward-216 → fin 12 测试文件；backward-217 → md 17 测试文件；backward-218 → notify 2 测试文件）。
- **被引用面（登记册 §6.10 + rg 实测吻合）**：main = sal 7 / qa 3 / pur 6 / mfg 14 / mnt 5 / drp 5 / logistics 1 文件（+ aps 2 / fin 1 = 桥接自有文件）——位次 11 起未迁移域（mnt/prj/qa/mfg/pur/sal/crm/drp/log）引用 inv Long API 的编译破坏为**已登记中间态**（backward-pointer successor = 各域 plan + M4.1 兜底），不在本计划 no-am reactor 内、不影响本计划构建。
- **手写代码冲击面（实测）**：inv-service main 跨域 import = fin 47 处 + md 13 处 + notify 2 处（C1 定位面；fin posting 族 `PostingEvent`/`AcctDocContext` Long id 字段流转按 M2.4 盲区先例以编译器清单为准）+ mfg 4 / pur 5（A2 桥接点 5 文件）；dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 本域语义 FK Long 参数清单为 Phase 4 门控输入——inv 为 11 域 82 处中的重户）。
- **测试资产（实测）**：inv-service **37 个测试类**（含 jqwik property-based 与 perf `TestErpInvCostingReclosePerf`）；`_cases` 快照 **1910 文件**（mission 第二大，仅次 fin 4108）；`ErpInvWebPagesTest` `@Tag("full-app")` + surefire excludedGroups 模块级排除（已提交治理决策，successor = M4.1，参与 test-compile）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 3 处，迁移即失效的实时缺陷）**：`erp/inv/pages/stock-take-flow/main.page.yaml:71/:91/:111`（`$tid:Long` ×3 → `ErpInvStockTake__startTake/completeTake/cancelTake(takeId:$tid)` mutation 变量）——inv 翻转 String 后静态类型不匹配 → adaptor 静默降级、盘点流程动作失效（M3.6 结束审计 MAJOR-1 同型）——**本计划就地 Fix + inv-web 重建验证**（inv-web 在本计划 7 模块 verify 范围内）。
- **Deferred 消费（前批移交项）**：aps test-scope IoC delta `_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml` 缺 `x:extends="super"`（bug `docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`——回收建议「归各域 plan 触碰时或 M4.1 统一回收」；**本计划 B 义务触碰 aps（桥接移除 + 链重建 + 测试复跑），顺带执行 aps delta 一行修复**（fin 修正版先例：根元素补 `x:extends="super"` + `xmlns:feature="feature"`，仅 `nopSequenceGenerator` 保持 `x:override="replace"`），并更新 bug doc 状态（五域 → 四域未修正）。md/notify/b2b/contract 四域 delta 不在本计划触碰面 → 留 M4.1。
- **已知风险（先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——八域中 cs 未复现（先例 delta 在位）；inv 若复现按 fin 修正版先例落 delta（`x:extends="super"`）；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 mfg-dao/pur-dao Long jar——A2 桥接登记例外；prj-dao test-scope 编译对象）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-inventory` 新鲜度门控（零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：inv orm 136 列全 `stdDataType="long"` 待改；inv 手写代码/测试/快照全部 Long 形态；早域 aps/fin 桥接点 9 处待退役；冻结序位次 10（位次 11 mnt 为批内序 3 后继）。

## Goals

- inv 域 136 列（自有 120 + md stub 16）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 inv 全部手写代码 + A2 前向桥接 9 处落桥（mfg 4 + pur 5，退役 owner M3.1/M2.5）。
- **B 退役义务兑付（9 条 main + retired test 回收）**：aps 侧桥接点 4 处移除 + bridge-test-103..106 inv 部分回收 + aps 7 模块链重建绿 + aps 测试复跑；fin 侧桥接点 5 处移除 + fin 5 模块链重建绿 + fin-service 测试复跑（497/497）；登记册 9 条 → retired。
- **Deferred 消费**：aps test-scope IoC delta 补 `x:extends="super"`（bug doc 状态更新五域 → 四域）。
- 快照每域重录（RECORDING→CHECKING；1910 文件基线）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 3 处就地 Fix（inv-web 重建验证）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘，C1/C2 修复定位面消费，heal M1.1 登记的 inv-dao `_gen` md 胶水中间态。
- 路线图 M2.2 → `done` + 日志；位次 11（mnt，批内序 3）解锁。

## Non-Goals

- 不迁移 manufacturing/purchase 域（A2 桥接目标域，归 M3.1/M2.5）；不动 mfg/pur orm 与生成件（桥接仅触及本域手写代码）。
- 不修复位次 11 起未迁移域对 inv 的引用破坏（sal/qa/pur/mfg/mnt/drp/logistics main+test——已登记中间态，successor = 各域 plan Phase 2/3 + M4.1 兜底；本计划仅在 Phase 4 登记确认）。
- 不修 md/notify/b2b/contract 四域 IoC delta（不在触碰面，归 M4.1 统一回收）；不做 fin web/app 补做（归 M2.7）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不修 `ErpInvWebPagesTest` 治理排除（successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更 + 跨域桥接退役编辑面）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 10 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；inv 业务语义 owner doc = `docs/design/inventory/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install + 位次 3-9 链 install（aps/b2b/contract/fin/ast/cs + 批内序 1 hr——冻结序批内顺序；inv 精确前置仅为 M2.1+M1.1+M1.2）。回滚策略：revert orm.xml + `mvn clean install -pl module-inventory/erp-inv-{codegen,dao,meta,service,web,app,api} -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 2/3 完成后回滚需先 revert 早域桥接退役与测试代码**——aps/fin 桥接点已移除形态对 Long inv jar 不可编译）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-inventory/model/app-erp-inventory.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M2.1 ✅ + M1.1 ✅ + M1.2 ✅（精确前置满足）+ 批内序 1（M3.3 hr）done（冻结序执行约束）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.10 inv 节，逐条核对：(i) A1 = 0（136 列全翻转）；(ii) A2 = bridge-main-071..079（mfg 4 + pur 5）与本地实测 import 对账（5 文件）；(iii) A3 = bridge-test-119..124 作为 Phase 3 定位面；(iv) **B 退役义务 = bridge-main-009/010/011/016（aps）+ 062/064/066/067/068（fin）作为 Phase 2 定位面（含 retired bridge-test-103..106 inv 部分回收）**；(v) C1 = backward-157/158/159 与 C2 = backward-216/217/218 作为 Phase 2/3 定位面；(vi) 按 b2b/assets A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(mnt|prj|pur|sal|qa|crm|drp|log|mfg)\.' module-inventory/erp-inv-service/src/test module-inventory/erp-inv-web/src/test` 排除 import 行 + test beans.xml ioc:type FQN——覆盖本域执行时点全部未迁移晚域；起草实测零命中，执行时点复扫确认）。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-inventory` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-inventory/model/app-erp-inventory.orm.xml` 逐行核对——仅 136 列 `stdDataType="long"→"string"`（自有 120 + md stub 16），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan inv 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案（含 FQN 盲区复扫结论 + B 义务 9 条定位面）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 136 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 落桥 + B 退役兑付

Status: planned
Targets: `module-inventory/erp-inv-{dao,service}/src/main/java/**`（手写 IBiz/BizModel/Processor/SPI）；**跨域编辑面：`module-aps/erp-aps-service/src/main/**`（4 桥接点 + delta）、`module-finance/erp-fin-service/src/main/**`（5 桥接点）**
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-inventory/erp-inv-codegen,module-inventory/erp-inv-dao,module-inventory/erp-inv-meta,module-inventory/erp-inv-service,module-inventory/erp-inv-web,module-inventory/erp-inv-app,module-inventory/erp-inv-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`、`-Dmaven.test.skip=true`）触发增量重生成。预期：inv-dao `_gen` md 关系胶水自愈（M1.1 登记中间态）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 inv dao + service 手写代码类型错误（定位面：fin 4 文件（C1）+ md 6 文件（C1，String 直传按语境）+ notify 2 文件（C1，签名不变预期零破坏核验）+ 全域 IBiz/值对象 Long 签名（inv 为语义 FK Long 参数重户，M0.1 附录 C）+ `.getId()` 下游；fin posting 族 id 值流转以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接 9 处落桥（D4 消费协议）——inv String id ↔ mfg/pur Long API 的调用点加转换桥（mfg 4：`CostAdjustmentService` + `StandardCostResolver` 的 `ErpMfgCostRollup`/`RollupLine` 引用；pur 5：`ErpInvLandedCostApproveProcessor`/`ErpInvLandedCostGenerateFreightLandedCostProcessor`/`ErpInvLandedCostProcessor` 的 `ErpPurReceive`/`ReceiveLine` 引用——**eq/filter 语义值桥主动识别**（Long 列传 String 静默空匹配，contract 037/038 + cs 059 先例）），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M3.1（mfg 4）/M2.5（pur 5）；代码内 bridge 注释双向指针。
  - Skill: `nop-backend-dev`
- [ ] Fix: **B 退役义务兑付（main 9 条）**——inv IBiz/实体签名翻转 String 后，早域桥接点错型（fin 传 Long 桥值 → inv String 参数）：① aps 侧 4 处（`ErpApsAtpCtpServiceImpl` :55/:65/:77 转换点 + :12/:13/:14 import 面；`ErpApsAutoDispatchProcessor` :10 + inv 语境调用点）移除 toLong 桥改 String 直传，移除 bridge 注释；② fin 侧 5 处（`ErpFinAccountingPeriodProcessor` :15/:16 + FQN 使用行 :538/:539/:546（登记册 callSite 已漂移 2 行，以执行时点 FQN 定位为准）+ 064 语义级转换点 :224 `ConvertHelper.toLong(period.getId())` → 直传 String（:223 bridge 注释行一并移除））同样移除；③ aps 7 模块链（`module-aps/erp-aps-{codegen,dao,meta,service,web,app,api}` no-am、`-Dmaven.test.skip=true`）重建绿；④ fin 5 模块链（`module-finance/erp-fin-{codegen,dao,meta,service,api}`，web/app 延后 M2.7）重建绿；⑤ fin grep 复核桥残留清零（M2.4 先例口径）。M2.4 assets 先例（mission 首例晚域退役早域桥接点）为执行范式。
  - Skill: `nop-backend-dev`
- [ ] Fix: **B 退役义务兑付（retired test bridge 回收）**——bridge-test-103..106（aps 4 测试文件）inv 侧 `String.valueOf` 桥移除 + inv 种子 Long→String（mfg 侧桥与种子保持 Long，归 M3.1；逐文件登记 mfg/inv 侧拆分）；随 Phase 3 前的 aps 测试复跑验证。
  - Skill: `nop-testing`
- [ ] Fix: **Deferred 消费：aps IoC delta 修复**——`module-aps/erp-aps-service/src/test/resources/_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml` 根元素补 `x:extends="super"` + `xmlns:feature="feature"`（仅 `nopSequenceGenerator` 保持 `x:override="replace"`——fin 修正版先例，bug doc 修复节）；bug doc 状态更新（五域 → 四域未修正）。
  - Skill: none
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（7 模块全绿，reactor 不含外域模块）；位次 11 起未迁移域引用 inv 的破坏为已登记中间态（登记册 backward-pointer + §6.10 被引用清单），Phase 4 登记；未登记破坏按路线图规则 6 停止回报。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] inv 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）；主代码修复清单 + A2 桥接例外清单在案
- [ ] **B 义务：aps 7 模块链重建绿 + fin 5 模块链重建绿 + fin grep 桥残留清零；aps delta 修复落地 + bug doc 更新**

### Phase 3 - 测试修复 + A3 桥接退役 + 快照重录 + 域级测试 + 早域测试复跑

Status: planned
Targets: `module-inventory/**/src/test/**`、`module-inventory/erp-inv-service/_cases/**`；早域复跑：`module-aps` 测试、`module-finance` 测试
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——37 个测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例；含 jqwik property generator 与 perf 测试），逐文件修复至测试编译通过；C2 后向 test 适配（backward-216 fin 12 + backward-217 md 17 + backward-218 notify 2 文件——M2.1/M1.1/M1.2 登记的 successor 义务兑付）；inv-web test-scope ast-dao（String 新 jar）/prj-dao（陈旧 Long jar）编译对象零 id 穿越核验（`ErpInvWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
- [ ] Fix: A3 test 桥接适配（bridge-test-119..124，6 条）——本域测试引用 mfg/pur 实体的 id 形态桥接（String↔Long 局部转换或 mock 桩签名适配，与 Phase 2 桥接同型），适配后在登记册退役对应 test 桥接条目（owner M2.2 = 本计划）。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 inv service 测试 → 逐案审核 `_cases/` 新形态（1910 文件基线；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正；**「断言式 + 空 autotest.yaml」范式测试不录快照——cs 81 方法目录超录回退先例**）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-inventory/erp-inv-service,module-inventory/erp-inv-web`（D3 口径：不带 `-am`）全绿——service 37 测试类 + web BUILD SUCCESS（`ErpInvWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按 fin 修正版先例修复（test-scope VFS delta 带根元素 `x:extends="super"` + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`
- [ ] Proof: **早域测试复跑（B 义务验证）**——`mvn test -pl module-aps/erp-aps-service,module-aps/erp-aps-web` 全绿（76/76 基线，web 0 tests 治理排除；含 bridge-test-103..106 inv 侧回收后形态）+ `mvn test -pl module-finance/erp-fin-service` 全绿（497/497 基线，fin web/app 延后口径）。任一红则修复至绿（桥接退役遗留问题在本计划内闭环）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] inv 域级测试全绿（service 37 类 + web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案
- [ ] **早域复跑全绿（aps 76/76 + fin 497/497 基线维持）；A3 6 条退役在案**

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: planned
Targets: `module-inventory/**`（手写代码 + inv-web 手写 page.yaml）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`、`docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，inv 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner M3.1/M2.5）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱（String 字典序，contract idOrder / fin billCode 时间戳序先例）专项 grep：`getId\(\)\s*[<>]|comparing.*getId`；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Fix: inv-web 手写 page.yaml raw-GraphQL `:Long` 变量 3 处就地 String 化——`stock-take-flow/main.page.yaml:71/:91/:111`（`$tid:Long` → `:String`，takeId mutation 变量）；variables 链与 options value 链一致性核证（contract version-diff 先例）；随后 `rg ':Long' module-inventory/erp-inv-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 类型变量如 `$lim:Int` 合法保留并逐条判定）；inv-web 重建 BUILD SUCCESS 验证。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-inventory/erp-inv-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
- [ ] Add: 登记册状态更新——B main 桥接 9 条（bridge-main-009/010/011/016 + 062/064/066/067/068）→ retired（兑付 note：aps/fin 侧移除证据 + 链重建 + 测试复跑指针）；A3 test 桥接 6 条（bridge-test-119..124）→ retired（owner M2.2 兑付 note）；A2 main 桥接 9 条保持 active（退役 owner M3.1（mfg 4）/M2.5（pur 5））；被引用面确认（位次 11 起未迁移域 successor 指针已由 backward-pointer 登记）；登记册消费工具 fail-closed 解析验证通过（dry-run 正常消费 + inv 段 0 待改列）。
  - Skill: none
- [ ] Add: owner doc 注记——grep `docs/design/inventory/` 中关于 inv id 为 Long/数字/BIGINT 的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [ ] Add: 路线图 M2.2 → `done`（M2/M3 表位次 10 + 头部「最后更新」；位次 11 mnt 解锁）+ 日志条目（含验证状态 + B 义务兑付 + aps delta 消费）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外逐条核清 + 桥接例外清单在案）；page.yaml `:Long` 清零 + inv-web 重建绿 + view 零被动变更在案
- [ ] 路线图状态、登记册退役（B 9 条 + A3 6 条）、bug doc 更新、日志一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fd94d6b83ffe9pO64KerPOnxfm）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR。事实核对全部属实（136 列构成 + stub 16/10 实体逐列吻合；A2 071..079 行号**逐行精确**；B 9 条与 json5 全一致；aps 证据 :12/:13/:14 + :55/:65/:77 + :10 + :322 精确；pom 逐条吻合含 jqwik（:127-130 含 `**/Property*.java` include）；37/1910 吻合；page.yaml 3 处唯一非 `_gen` ✓；B 命令清单（aps 7 链 + fin 5 链）与 M2.4 先例逐字一致、基线 497/497（M2.4 Closure）与 76/76（M3.9 记录）核实；**Deferred 消费正确——aps delta 文件存在且确缺 `x:extends="super"`，范围裁剪与 bug doc 指引一致**；被引用面七域计数逐一复测吻合；跨域 import fin 47/md 13/notify 2/mfg 4/pur 5 全部精确；FQN 复扫零命中属实）。MINOR：① fin 证据行号陈旧（登记册 :536/:537/:544 已漂移，实况 :538/:539/:546；:223 为注释行、调用在 :224）；② orgId ×14 归属口径错 1（自有 13 + md stub `ErpMdWarehouse.orgId`）；③「inv-app 无域级 web 依赖」不准（有 md-web compile，erp-inv-app/pom.xml:36）。
  - 审查者 B（治理/规范视角，ses_fd94d2446ffeFbAMaE2TINLbRo）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 1 MINOR。治理检查全过（B 义务 9 条 retireOwner 逐条吻合；retired test 回收拆分正确（inv 侧本计划、mfg 侧 M3.1，与 bridge-test-103 note 原文一致）；backward 6 条文件数逐一吻合；**Deferred 消费合法性成立**——B 义务确实触碰 aps，符合 bug doc「归各域 plan 触碰时」条款，bug doc 状态更新项四处一致、四域显式留 M4.1；fin web/app 正确归 M2.7；早域复跑属 B 义务验证而非全量验证，与执行时规则 7 不冲突）。MINOR：① bridge-main-064 行号 ±1 漂移（:223 注释 / :224 调用）。
  - **修订（iteration 1 → 2，已落地）**：全部 3+1 MINOR 已处理——fin 行号改实况锚点（:15/:16 + FQN 使用行 :538/:539/:546 + 064 调用 :224/:223 注释行，登记册漂移注记 + 「以执行时点 FQN 定位为准」）；orgId 改「自有 13 + md stub 1（ErpMdWarehouse.orgId）」；inv-app 改「域级 web 依赖仅 md-web（String，无跨迁移边）」。
- Independent draft review iteration 2（2026-08-22，独立复审 ses_fd942df62ffeOUGpNQplCRvJR5）：`passes draft review` — 4/4 发现 RESOLVED（逐项 live 复验：`:223` = bridge 注释、`:224` = 调用行原文核实；inv orm `:1141` orgId 确在 ErpMdWarehouse stub 块（1134-1145）内、自有 13；inv-app pom:36 md-web compile 核实），零新缺陷，列算术复核（21+99+16=136）通过、baseline 段落结构完整。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fd94d6b83ffe9pO64KerPOnxfm，2026-08-22 — 「批准 M2.2 inv orm 保护区域变更（技术视角批准）」。依据：136 列（含 md stub 16 与 M1.1 权威源对齐）、A1=0、回写走 M0.1 三步门控；B 义务 9 条退役与登记册 retireOwner 完全对账。
    - 批准 2（治理视角）：ses_fd94d2446ffeFbAMaE2TINLbRo，2026-08-22 — 「批准 M2.2 inv orm 保护区域变更（治理视角批准）」。依据：aps delta 消费合法性、范围登记与 bug doc 状态更新义务均完备。
- 共识达成（2026-08-22）：iteration 2 全部发现 RESOLVED + 双批准落盘 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（136 列落源 + no-am 7 模块重生成 + 手写代码/测试修复 + A2 落桥 9 + B 退役 9 与 retired test 回收 + 快照重录 + grep 门控清零 + page.yaml 3 处 Fix + aps delta 修复）
- [ ] 相关文档对齐（owner doc 注记结论、路线图 M2.2 状态、登记册退役（B 9 + A3 6）、bug doc 状态、日志）
- [ ] 已运行验证：`mvn clean install -pl module-inventory/erp-inv-{codegen,dao,meta,service,web,app,api} -DskipTests` 全绿 + `mvn test -pl module-inventory/erp-inv-service,module-inventory/erp-inv-web` 全绿 + **早域复跑 aps 76/76 + fin 497/497** + 工具重扫零残留（inv 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2 main 桥接 9 处（mfg 4 + pur 5，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——mfg（位次 14）/pur（位次 15）未迁移，桥接点为编译必需
- Successor Required: `yes`（M3.1 回收 mfg 4 条；M2.5 回收 pur 5 条——晚域翻转时退役条目并移除本域桥接点）

### bridge-test-103..106 mfg 侧桥与种子（aps 测试）

- Classification: `watch-only residual`
- Why Not Blocking Closure: aps 测试对 mfg 的 `String.valueOf` 桥与 Long 种子为 M3.9 登记退役形态的剩余半边（inv 半边由本计划回收），mfg 未迁移前为编译/断言必需
- Successor Required: `yes`（M3.1 翻转 mfg 时移除）

### 位次 11+ 未迁移域对 inv 的引用破坏（sal/qa/pur/mfg/mnt/drp/logistics）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册 backward-pointer 预先登记的中间态（本计划 no-am reactor 不含该等域；被引用清单 §6.10 在案）
- Successor Required: `yes`（各域 plan Phase 2/3 + M4.1 兜底）

### `ErpInvWebPagesTest` 页面校验

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
