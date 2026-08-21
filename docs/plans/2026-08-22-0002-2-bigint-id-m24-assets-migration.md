# 2026-08-22-0002-2-bigint-id-m24-assets-migration 主键/外键 string 化 M2.4：assets 域迁移（冻结序位次 7，含 fin 桥接退役义务）

> Plan Status: active（2026-08-22：iteration 1-3 独立草案审查收敛 + 保护区域双独立子 agent 批准（技术 ses_fdaecac03ffezjlpdWu00j4cJW / 治理 ses_fdaec67abffet95xJrKGrvjzSu），见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M2.4（assets，冻结序位次 7）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 7（M2.4）
> Related: `docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`（批内序 1，本计划硬前置）、`docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md`（标准结构先例）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）、`docs/plans/2026-08-22-0002-3-bigint-id-m35-cs-migration.md`（批内序 3）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **assets 域规模（2026-08-22 实况 scan）**：`module-assets/model/app-erp-assets.orm.xml` 需改列 **110 = 自有 98（PK 18 + BIGINT FK 80，18 实体）+ notGenCode md stub 12（PK 6 + FK 6）**；全文件 FK 合计 86 = 自有 80 + stub 6（scan FK 口径）。自有 FK 含 `orgId` ×17（自有实体 orgId FK 列实测；另 md stub orgId 1 列；`name="orgId"` XML 原始出现 39 次含 `<index>` 成员引用，以列定义口径为准）+ `assetId`/`categoryId`/`currencyId`/`cipId` 等本域 FK。**弱引用列专项**：`ErpAstCip.projectId`(:741) 为可空弱引用 BIGINT 列（orm 注释自述「不强外键」、**无 refEntityName 关系边**，登记册 §3 前向簇口径正确不含它）——无 `_gen` 关系胶水、无对称耦合，随本域直接翻转安全。**不改列**：VARCHAR FK 1 列（`ErpAstDisposal.nopFlowId` 显式 string）+ `delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）。
- **stub 构成**：ast orm 末尾 md stub（`ErpMdCurrency`/`ErpMdEmployee`/`ErpMdLocation`/`ErpMdMaterialCategory`/`ErpMdOrganization`/`ErpMdSubject` 共 12 列）——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐（先例：b2b/contract 的 md stub）。ast orm 无指向未迁移域的 stub（contrast：fin 的 prj/ast stub，plan 1 Decision 项）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-assets/erp-ast-{codegen,dao,meta,service,web,app,api}`（全链构建，无 finance 式延后）。`erp-ast-service` main compile 依赖 **fin-service（M2.1 后 String）+ mnt-dao（未迁移 Long——A2 桥接对象）** + common-service + md（经 ast-dao→md-dao 传递）；ast-web main 依赖本域 meta/service + nop-web（md-service 为 **test-scope**）；**ast-web test-scope 依赖 prj-dao（未迁移 Long 陈旧 jar，页面测试编译对象——预期零 id 穿越，Phase 3 核验）**；ast-app main 依赖 ast-service/ast-web/md-web（无 prj/fin web 依赖，可建）。
- **ast orm 外域关系（refEntityName 实测）**：md ×50（M1.1 起 ast-dao `_gen` md 关系胶水处于已登记中间态（惰性 jar 陈旧未重编译），**本域迁移即自愈**）；prj/fin/mnt ×0（projectId 为弱引用无关系边；fin/mnt 耦合全在 service 代码层）。
- **M0.2 登记册 assets 视角（§6.7，起草消费已核）**：A1 orm 延后 = 0；**A2 main 桥接 2 条**（bridge-main-024/025：`ErpAstDisposalProcessor:264/:271` → `IErpMntEquipmentBiz.changeStatusForAssetDisposal/restoreFromAssetDisposal(assetId Long)`，退役 owner M3.2（mnt，位次 11））；**A3 test 桥接 1 条**（bridge-test-107：`TestMockMntBizModels` import `IErpMntEquipmentBiz`/`ErpMntEquipment`，owner M2.4 = 本计划 Phase 3 退役）；**B 退役/翻转义务（作为晚域，3 条）**：翻转 `IErpAstDepreciationScheduleBiz` 相关签名时退役 bridge-main-061/063/065（fin 侧桥接，位于 fin `ErpFinAccountingPeriodProcessor`——**fin 侧桥接点代码移除 + fin 5 模块链重建绿为本计划义务**，见 Phase 2）；C1 后向 main 3 条（backward-134 → fin 2 文件（`AssetPostingExecutor`/`ErpAstDepreciationScheduleProcessor`）；backward-135 → md 9 文件（posting Dispatcher ×9）；backward-136 → notify 3 文件）；C2 后向 test 3 条（backward-194 → fin 12 测试文件（含 `AstTestSupport`）；backward-195 → md 3 文件；backward-196 → notify 1 文件）。
- **fin 桥接退役专项（B 义务，本计划新增跨域编辑面）**：M2.1 在 fin `ErpFinAccountingPeriodProcessor` 落的 ast 侧桥接（bridge-main-061 类型级 import + 063 `executeBatchDepreciation` + 065 `reverseDepreciation(assetId)` 转换点）在本域 IBiz 翻转 String 后变为错型（fin 传 Long 桥值 → ast String 参数编译错）——本计划 Phase 2 须同步移除 fin 侧桥接点并重建 fin 5 模块链（fin-service 已 String，配合新 ast-dao String jar 重编译即绿）。此为 mission 首例「晚域退役早域桥接点」执行（登记册 §1 消费协议 ②）。
- **手写代码冲击面（实测，含登记册扫描器盲区预登记）**：ast-service main 跨域 fin import = **20 文件（rg 实测：posting Dispatcher ×9 + AcctDocProvider ×9 + `AssetPostingExecutor` + `ErpAstDepreciationScheduleProcessor`）**——登记册 backward-134 仅登记 2 文件（`AssetPostingExecutor`/`ErpAstDepreciationScheduleProcessor`）为 **id 语境子集**，posting Dispatcher/Provider 族经 fin `PostingEvent`/`AcctDocContext` 的 Long id 字段（acctSchemaId/orgId/currencyId/periodId 等）流转 id 值（如 `DepreciationPostingDispatcher` `event.setOrgId(asset.getOrgId())` 型调用），M2.1 翻转后**全部为本计划编译修复面**——此为登记册扫描器与 b2b A3' 同类的盲区，本计划按 A3' 先例预登记（Phase 1 消费时追注登记册，修复以编译器清单为准）；另 md 9 文件（C1 backward-135）+ notify 3 文件（C1 backward-136）+ mnt（A2 桥接点 `ErpAstDisposalProcessor`）；dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 本域语义 FK Long 参数清单为 Phase 4 门控输入）。
- **测试资产（实测）**：ast-service **37 个测试类**（fin 93/contract 22 之间）；`_cases` 快照 **1464 文件**；`ErpAstWebPagesTest` `@Tag("full-app")` + surefire excludedGroups 模块级排除（已提交治理决策，successor = M4.1）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 1 处，迁移即失效的实时缺陷）**：`erp/ast/pages/disposal-wizard/main.page.yaml:74`（`$aid:Long` → `ErpAstAsset__get(id:$aid)`）——ast 翻转 String 后静态类型不匹配（M3.6 结束审计 MAJOR-1 同型，contract version-diff 就地 Fix 先例）——**本计划就地 Fix + ast-web 重建验证**（ast-web 在本计划 7 模块 verify 范围内，可实际重建）。
- **已知风险（先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——六域连续复现，按先例修复（test-scope VFS delta + DeltaOverride delta-layer 补 default 层集）；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 mnt-dao Long jar——A2 桥接登记例外；prj-dao test-scope 陈旧 jar 页面测试编译对象）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-assets` 新鲜度门控；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：ast orm 110 列全 `stdDataType="long"` 待改；ast 手写代码/测试/快照全部 Long 形态；冻结序位次 7（位次 8 cs 为批内序 3 后继）。

## Goals

- assets 域 110 列（自有 98 + md stub 12）`stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）；弱引用列 `ErpAstCip.projectId` 随域翻转（无关系边，零耦合）。
- 增量重生成（no-am 7 模块链）+ 编译器驱动修复 ast 全部手写代码 + A2 前向桥接 2 处落桥（mnt，退役 owner M3.2）。
- **B 退役义务兑付**：fin 侧 bridge-main-061/063/065 桥接点移除 + 登记册退役 + fin 5 模块链重建绿。
- 快照每域重录（RECORDING→CHECKING；1464 文件基线）。
- 语义陷阱 grep 门控清零 + page.yaml `:Long` 1 处就地 Fix（ast-web 重建验证）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘，C1/C2 修复定位面消费，heal M1.1 登记的 ast-dao `_gen` md 胶水中间态。
- 路线图 M2.4 → `done` + 日志；位次 8（cs，本批 plan 3）解锁。

## Non-Goals

- 不修复外域代码对 ast 的引用（被引用面仅 projects main 1 + test 1 文件（`ErpPrjProjectSettlementProcessor` 等）——prj plan（M2.7）的 C 定位面；本计划仅在 Phase 4 登记确认）。
- 不迁移 maintenance 域（A2 桥接目标域，归 M3.2）；不动 fin orm（fin 侧桥接点移除仅触及 fin **service 手写代码**，不触及 fin orm/生成件）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不修 `ErpAstWebPagesTest` 治理排除（successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。
- 不做 fin web/app 补做（归 M2.7 后补做载体，见 plan 1 Deferred 登记）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更 + 跨域桥接退役编辑）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 7 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；assets 业务语义 owner doc = `docs/design/assets/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install + 位次 3-5 链 install + **批内前置：M2.1 fin 5 模块链 install（String 形态）**——ast-service main 编译依赖 fin-service String jar。回滚策略：revert orm.xml + `mvn clean install -pl module-assets/erp-ast-codegen,module-assets/erp-ast-dao,module-assets/erp-ast-meta,module-assets/erp-ast-service,module-assets/erp-ast-web,module-assets/erp-ast-app,module-assets/erp-ast-api -Dmaven.test.skip=true` 重生成回 Long 形态（fin 侧桥接点回滚同步；**Phase 3 完成后回滚需先 revert 测试代码**——`-Dmaven.test.skip=true` 跳过测试编译，String 测试代码对 Long main 会破坏 test-compile）。

## Execution Plan

### Phase 1 - 消费登记册 + orm 回写（保护区域，双批准前置）

Status: planned
Targets: `module-assets/model/app-erp-assets.orm.xml`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M2.1 ✅（批内序 1 完成且 fin 链已 install）+ M1.1 ✅ + M1.2 ✅（精确前置满足）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [ ] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.7 assets 节，逐条核对：(i) A1 orm 延后 = 0（110 列全翻转）；(ii) A2 main 桥接 2 条（mnt）与本地实测对账；(iii) A3 = bridge-test-107 作为 Phase 3 定位面；(iv) B 退役义务 3 条（bridge-main-061/063/065，fin 侧）作为 Phase 2 定位面；(v) C1 = backward-134（fin 2）/135（md 9）/136（notify 3）与 C2 = backward-194（fin 12）/195（md 3）/196（notify 1）作为 Phase 2/3 定位面；(vi) 按 b2b A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(cs|hr|inv|mnt|prj|pur|sal|qa|ct|crm|drp|log|mfg)\.' module-assets/erp-ast-service/src/test` 排除 import 行——覆盖本域执行时点全部未迁移晚域，早域 md/notify/aps/b2b/contract/fin 已 String 无盲区风险），命中则补登 + 处置；main 侧 backward-134 盲区（fin import 20 文件 vs 登记册 2 文件）按 Current Baseline 预登记结论在登记册 §6.7 追注。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [ ] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [ ] Fix: 回写 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-assets` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
- [ ] Proof: `git diff module-assets/model/app-erp-assets.orm.xml` 逐行核对——仅 110 列 `stdDataType="long"→"string"`（自有 98 = PK 18 + FK 80 + md stub 12（PK 6 + FK 6）；全文件 FK 口径 86 = 自有 80 + stub 6），`stdSqlType` 零变化、`delVersion`/标签结构零变化；scan assets 段重扫零 `NEEDS FIX`/零 `DEFERRED` 残留。
  - Skill: none

Exit Criteria:

- [ ] 登记册消费核对在案（含 B 义务定位面 + FQN 盲区复扫结论）；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 110 列 stdDataType

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 落桥 + fin 桥接退役（B 义务）

Status: planned
Targets: `module-assets/erp-ast-dao/src/main/java/**`、`module-assets/erp-ast-service/src/main/java/**`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinAccountingPeriodProcessor.java`（fin 侧桥接点移除）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [ ] Fix: `mvn clean install -pl module-assets/erp-ast-codegen,module-assets/erp-ast-dao,module-assets/erp-ast-meta,module-assets/erp-ast-service,module-assets/erp-ast-web,module-assets/erp-ast-app,module-assets/erp-ast-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`）触发增量重生成。预期：ast-dao `_gen` md 关系胶水自愈（M1.1 登记中间态）；`install` 落本地仓供 fin 链重建与 prj 后继消费。
  - Skill: `nop-backend-dev`
- [ ] Fix: 编译器驱动修复主代码——逐条修复 ast dao + service 手写代码类型错误（定位面：**fin 20 文件（rg 实测，含登记册 backward-134 的 2 文件 id 语境子集 + posting Dispatcher/Provider 族经 PostingEvent/AcctDocContext Long id 字段的值流转面——对 String 化 fin API 的适配，backward-134 兑付 + 盲区预登记面）** + md 9 文件（C1，String 直传/toLong 桥按语境）+ notify 3 文件（C1，签名不变预期零破坏核验）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游；以编译器实际清单为准），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。
  - Skill: `nop-backend-dev`
- [ ] Fix: A2 前向桥接 2 处落桥（D4 消费协议）——`ErpAstDisposalProcessor:264/:271` 对 `IErpMntEquipmentBiz.changeStatusForAssetDisposal/restoreFromAssetDisposal(assetId Long)` 的调用点加 `ConvertHelper.toLong` 桥（ast String → mnt Long），登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M3.2（mnt）；代码内 bridge 注释双向指针；**eq/filter 语义值桥主动清扫**（contract 037/038 先例）：grep 本域对 mnt 实体的 `eq(`/filter 查询构造逐条核对过滤值类型。
  - Skill: `nop-backend-dev`
- [ ] Fix: B 退役义务兑付（fin 侧桥接点移除 + 登记册退役 + fin 回归证明）——ast IBiz（`IErpAstDepreciationScheduleBiz.executeBatchDepreciation/reverseDepreciation` 及相关值对象）翻转 String 后：① 移除 fin `ErpFinAccountingPeriodProcessor` 中 bridge-main-061/063/065 桥接点（toLong 转换/类型级适配恢复直传）；② `tools/id-migration-registry.json5` 三条 status → retired；③ `mvn clean install -pl module-finance/erp-fin-codegen,module-finance/erp-fin-dao,module-finance/erp-fin-meta,module-finance/erp-fin-service,module-finance/erp-fin-api -Dmaven.test.skip=true` 重建 fin 5 模块链绿（fin-service 直传 String → ast String API；fin-web/app 仍不建，M2.1 延后约束维持）；④ **fin 侧回归证明：`mvn test -pl module-finance/erp-fin-service` 复跑全绿**（fin 93 测试类覆盖被编辑的 `ErpFinAccountingPeriodProcessor` period-close 流程——B 退役不得引入 fin 回归；桥接移除为唯一 fin 变更，预期快照零 diff，若 diff 则逐案审核）；⑤ fin 侧 grep 门控复核（被退役桥接的例外清单条目清出）。
  - Skill: `nop-backend-dev`
- [ ] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏；未登记破坏按路线图规则 6 停止回报；已登记破坏按中间态继续并履行登记义务（prj 引用面 `ErpPrjProjectSettlementProcessor` 破坏 = 已登记中间态，successor M2.7）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] ast 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）且已 install；**fin 5 模块链重建绿 + bridge-main-061/063/065 退役（代码移除 + 登记册 retired + fin-service 测试复跑全绿）**；主代码修复清单 + A2 桥接例外清单在案

### Phase 3 - 测试修复 + A3 桥接适配 + 快照重录 + 域级测试

Status: planned
Targets: `module-assets/**/src/test/**`、`module-assets/erp-ast-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [ ] Fix: 测试代码修复——37 个测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；ast-web test-scope prj-dao 陈旧 jar 编译对象零 id 穿越核验（`ErpAstWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
- [ ] Fix: A3 test 桥接适配（bridge-test-107）——`TestMockMntBizModels` 引用 mnt `IErpMntEquipmentBiz`/`ErpMntEquipment` 的 id 形态桥接（String↔Long 局部转换或 mock 桩签名适配），适配后在登记册退役该条目（owner M2.4 = 本计划）。
  - Skill: `nop-testing`
- [ ] Fix: C2 后向 test 适配——fin 12 + md 3 + notify 1 测试文件对 String 化 API 的适配（M2.1/M1.1/M1.2 登记的 successor 义务兑付；fin 侧含 `AstTestSupport` 公共基座 String 化）。
  - Skill: `nop-testing`
- [ ] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 ast service 测试 → 逐案审核 `_cases/` 新形态（1464 文件基线；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹与审核结论记录本计划。
  - Skill: `nop-testing`
- [ ] Proof: `mvn test -pl module-assets/erp-ast-service,module-assets/erp-ast-web`（D3 口径：不带 `-am`）全绿——service 37 测试类 + web BUILD SUCCESS（`ErpAstWebPagesTest` 治理排除，0 tests 预期）。若复现平台 IoC 回归，按先例修复（test-scope VFS delta + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] ast 域级测试全绿（service 37 类；web 治理排除偏差登记）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: planned
Targets: `module-assets/**`（手写代码 + ast-web 手写 page.yaml）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [ ] Proof: 语义陷阱 grep 门控（路线图横切 §3，ast 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，退役 owner M3.2）；Long 装箱 `==`/`!=`（id 上下文）逐条核清；id 序比较陷阱（String 字典序）专项 grep：`getId\(\)\s*[<>]|comparing.*getId`；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
- [ ] Fix: ast-web 手写 page.yaml raw-GraphQL `:Long` 变量 1 处就地 String 化——disposal-wizard:74 `$aid:Long` → `$aid:String`（variables 兜底 `|| 0` 改 `|| ''` 若存在；options value 链一致性核证）；随后 `rg ':Long' module-assets/erp-ast-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 类型变量逐条判定合法保留）；ast-web 重建 BUILD SUCCESS 验证（contract version-diff 先例）。
  - Skill: none
- [ ] Proof: 手写 view.xml 零改动验证——`git status module-assets/erp-ast-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
- [ ] Add: 登记册状态更新——A3 桥接 1 条（bridge-test-107）+ B 义务 3 条（bridge-main-061/063/065）status → retired（owner M2.4 兑付 note）；A2 main 桥接 2 条（bridge-main-024/025）保持 active（退役 owner M3.2）；被引用面确认（projects main 1 + test 1 文件，successor = M2.7 C 定位面；M4.1 兜底）。
  - Skill: none
- [ ] Add: owner doc 注记——grep `docs/design/assets/` 中关于 ast id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [ ] Add: 路线图 M2.4 → `done`（M2/M3 表位次 7 + 头部「最后更新」）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [ ] grep 门控零残留（例外逐条核清 + 桥接例外清单在案）；page.yaml `:Long` 清零 + ast-web 重建绿 + view 零被动变更在案
- [ ] 路线图状态、登记册退役（4 条）、日志三者一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdaecac03ffezjlpdWu00j4cJW）：`needs revision` — 0 BLOCKER / 2 MAJOR / 4 MINOR。事实核对大部分属实（110 面、stub 12、弱引用列无关系边、A2 024/025 行级、B 义务 §1② 执行正确、37/1464、C1/C2 rg 精确）。**MAJOR-1**：自有 FK 分解算术不可调和（18+86=104≠98）——真实自有 FK = 80，86 为含 stub FK 6 的全文件口径，Phase 1 git-diff Proof 指令嵌入错误算术。**MAJOR-2**：ast-service main fin import 实测 **20 文件**（posting Dispatcher ×9 + AcctDocProvider ×10 + ErpAstDepreciationScheduleProcessor）而非登记册 backward-134 的 2 文件——posting 族经 fin `PostingEvent`/`AcctDocContext` Long id 字段流转 id 值（`DepreciationPostingDispatcher:148-150` `event.setOrgId(asset.getOrgId())` 型），登记册扫描器盲区（b2b A3' 同类），基线低报最大修复面 10×。MINOR：③ ast-web md-service 为 test-scope；④ orgId ×39 XML 口径（真实 17）；⑤ 回滚 test-compile 陷阱；⑥ md 经 ast-dao→md-dao 传递非 common。
  - 审查者 B（治理/规范视角，ses_fdaec67abffet95xJrKGrvjzSu）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR。治理检查全过（命名 N=2 / 依赖处理 / **B 义务与登记册 §1② 精确匹配——mission 首例跨域退役正确声明并门控** / §6.7 消费逐条准确 / 保护区域门控 + 证据链 / 7 模块域级 verify）。MINOR：① Goals「位次 9（hr）解锁」错误（M2.4 done 解锁位次 8 cs = 本批 plan 3）；② FQN 复扫 regex 漏晚域 hr/mfg/drp/log（重新引入 A3' 盲区）；③ B 退役后 fin 仅 main 重建无测试复跑（fin 93 类覆盖被编辑的 AccountingPeriodProcessor）+ Phase 2 A2 缺 eq/filter 语义值桥清扫语。
  - **修订（iteration 1 → 2，已落地）**：全部 2 MAJOR + 7 MINOR 已处理——自有 FK 改 80（含全文件 86 = 80+stub 6 口径说明）；fin import 面改 20 文件实测 + 盲区按 A3' 先例预登记（Phase 1 消费时登记册 §6.7 追注）；ast-web/cs 侧 md-service test-scope 标注；orgId ×17；回滚 `-Dmaven.test.skip=true` + 阶段差异；md 传递路径修正；Goals 解锁位次改 cs；FQN regex 覆盖全部未迁移晚域（cs|hr|inv|mnt|prj|pur|sal|qa|ct|crm|drp|log|mfg）；B 退役增加 `mvn test -pl module-finance/erp-fin-service` 复跑全绿回归证明（Closure Gates 同步）；A2 增加 eq/filter 语义值桥主动清扫。
- Independent draft review iteration 2（2026-08-22，双审查者同会话复审）：
  - 审查者 A（技术视角）：`needs revision` — 1 MAJOR 残留：Phase 1 git-diff Proof（line 71）仍含不可调和括号「PK 18 + FK 86 + md stub 12」（baseline line 13 已修但 proof 门控位置未同步）；其余全部核实解决（MAJOR-2 20 文件盲区预登记完整落地 + Phase 1 追注 + Phase 2 修复面；MINOR 全清；治理侧 MINOR 全采纳；baseline 算术 live-verified）。审查者声明：「one-line fix; no re-audit needed after it lands——技术视角批准随该修正立即生效」。
  - 审查者 B（治理视角）：`needs revision` — 同一 MAJOR 残留（line 71 proof 门控嵌入虚假算术 + 修订记录夸大）；全部 MINOR 核实解决；**批准 2（治理视角）维持**（iteration 1 已授予，依据不受残留影响——110 列机器门控表面/证据链/B 义务合规均完好），计划须在 line 71 修正后方可转 active。
  - **修订（iteration 2 → 3，已落地）**：line 71 括号修正为「自有 98 = PK 18 + FK 80 + md stub 12（PK 6 + FK 6）；全文件 FK 口径 86 = 自有 80 + stub 6」；baseline fin import 构成分列精确化（Dispatcher ×9 + AcctDocProvider ×9 + AssetPostingExecutor + ErpAstDepreciationScheduleProcessor = 20，合计不变）。
- Independent draft review iteration 3（2026-08-22）：iteration 2 双审查者预先声明的一行修正机械落地（line 71 + 分列精确化，无其他变更），按审查者 A「no re-audit needed」+ 审查者 B「批准维持」指示结束草案审查。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fdaecac03ffezjlpdWu00j4cJW，2026-08-22（iteration 2 声明 + line 71 修正落地生效）— 「批准 M2.4 assets orm 保护区域变更（技术视角批准）」。依据：110 列面/弱引用列安全/md stub 对齐/B 义务 §1② 设计/20 文件 fin import 盲区预登记均经 live-verified；stdDataType-only、DDL 零变化。
    - 批准 2（治理视角）：ses_fdaec67abffet95xJrKGrvjzSu，2026-08-22（iteration 1 授予，iteration 2 复核维持）— 「批准 M2.4 assets orm 保护区域变更（治理视角批准）」。依据：证据链完整、登记册 §6.7 全集准确消费、跨域 fin 编辑为登记册协议自身指派义务、110 列 stdDataType-only 表面精确有界。
- 共识达成（2026-08-22）：iteration 3 修正落地 + 双批准在案 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [ ] 范围内行为完成（110 列落源 + no-am 7 模块重生成 + 手写代码/测试修复 + A2 落桥 + A3/B 退役（含 fin 侧桥接移除与 fin 链重建绿）+ 快照重录 + grep 门控清零 + page.yaml Fix）
- [ ] 相关文档对齐（owner doc 注记结论、路线图 M2.4 状态、登记册退役（4 条）、日志）
- [ ] 已运行验证：`mvn clean install -pl module-assets/erp-ast-{codegen,dao,meta,service,web,app,api} -DskipTests` 全绿 + `mvn test -pl module-assets/erp-ast-service,module-assets/erp-ast-web` 全绿 + fin 5 模块链重建绿 + fin-service 测试复跑全绿 + 工具重扫零残留（assets 段 `NEEDS FIX` = 0）
- [ ] 无范围内项目降级为 deferred/follow-up（web 页面测试治理排除为已提交决策 + M4.1 successor 登记，属偏差登记而非范围降级）
- [ ] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### A2 main 桥接 2 处（mnt，String↔Long 临时转换）

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——maintenance（位次 11）未迁移，桥接点为编译必需
- Successor Required: `yes`（M3.2 回收 2 条并移除本域桥接点）

### projects 对 assets 的被引用面（main 1 + test 1 文件）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 晚域（prj，位次 12）代码引用本域 String API 的编译破坏属预期中间态（登记册被引用清单），本域 verify 闭包（7 模块自身链，no-am）不含外域模块
- Successor Required: `yes`（M2.7 的 C 定位面；M4.1 兜底）

### `ErpAstWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策（plan 2026-07-24-0930-1），实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### 平台 IoC 回归 delta（若 Phase 3 复现并落盘）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 `nopSequenceGenerator` bean-init-self-wait 为已登记平台 Bug，先例修复 = test-scope VFS delta
- Successor Required: `yes`（平台修复后统一移除，M4.1 复核）

## Closure

Status Note: （待执行与结束审计）

Closure Audit Evidence:

- （待独立结束审计）

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录。）
