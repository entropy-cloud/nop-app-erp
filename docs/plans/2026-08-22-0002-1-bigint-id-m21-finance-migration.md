# 2026-08-22-0002-1-bigint-id-m21-finance-migration 主键/外键 string 化 M2.1：finance 域迁移（冻结序位次 6，web/app 重生成延后）

> Plan Status: completed（2026-08-22：iteration 1 双独立审查 `passes draft review`（0 BLOCKER / 0 MAJOR）+ 保护区域双独立子 agent 批准，见 Draft Review Record；批准后 MINOR 修订注记在案；同日四 Phase 执行完毕 + 独立结束审计 `passes closure audit`（0 BLOCKER / 0 MAJOR / 2 MINOR 已处置），见 Closure）
> Mission: id-string-migration
> Work Item: M2.1（finance，冻结序位次 6）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 6（M2.1）
> Related: `docs/plans/2026-08-21-2025-3-bigint-id-m36-contract-migration.md`（批前末位先例）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1）、`docs/plans/2026-08-21-1657-2-bigint-id-m12-notify-migration.md`（M1.2）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）、`docs/plans/2026-08-22-0002-2-bigint-id-m24-assets-migration.md`（批内序 2）、`docs/plans/2026-08-22-0002-3-bigint-id-m35-cs-migration.md`（批内序 3）
> Audit: required（保护区域 `model/*.orm.xml`：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **finance 域规模（2026-08-22 实况 scan）**：`module-finance/model/app-erp-finance.orm.xml` 本轮落源列 **208 = 自有 187（PK 36 + BIGINT FK 151，36 实体）+ notGenCode stub 21**；另有 **6 列登记册 A1 延后**（scan 标记 `DEFERRED(registry)`，保持 long 至 M2.7）：`ErpFinVoucherLine.projectId`(:517)、`ErpFinGlBalance.projectId`(:958)、`ErpFinExpenseClaimLine.projectId`(:1361)、`ErpFinEmployeeAdvance.projectId`(:1417)、`ErpFinBudgetLine.projectId`(:1859)、`ErpFinGlMappingRule.projectId`(:2063)，全部 refEntityName → `ErpPrjProject`（prj 位次 12 未迁移）。自有 FK 含 `orgId` ×28（自有实体 orgId FK 列实测；另 md stub 含 orgId 3 列；`name="orgId"` XML 原始出现 48 次含 `<index>` 成员引用，以列定义口径为准）等。**不改列**：VARCHAR FK 2 列（`ErpFinPostingException.traceId`、`ErpFinBudgetControlLog.operatorId`，显式 string 本就 String）+ `delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）。
- **notGenCode stub 构成（本域独有新情况，需 Decision）**：fin orm 末尾 stub 12 个 = md ×10（21 列中 18 列，md 权威源自 M1.1 已 String，翻转 = 与权威源对齐，先例 md/notify/aps/b2b/contract）+ **`ErpPrjProject` stub（id + customerId，2 列）+ `ErpAstAsset` stub（id，1 列）——权威源 prj（位次 12）/ast（位次 7）均未迁移（Long）**。工具 dry-run 副本（08-21 时点）已将这 3 列翻为 string（工具按 PK/FK 统一处理，无权威源对齐检查）；M0.2 登记册 §3「恰 2 前向簇（fin→prj 6 + hr→prj 2）」按 refEntityName 边口径统计，**stub 顶点声明列不在前向簇口径内 = 登记册盲区**（首例：b2b/contract 的 stub 全部指向已迁移 md，无先例可循）。6 条延后 FK 列的 join（leftProp long ↔ stub id string）与 stub 翻转的 codegen 行为（是否校验 join 类型一致 / `_gen` 胶水取 stub 类型还是 classpath 实体类型）**未经实证**——Phase 1 Explore 项裁定（见 Execution Plan）。
- **web/app 重生成延后（路线图冻结裁决，本计划执行）**：finance 为唯一全域构建纠缠域——`fin-app` main 依赖 `app-erp-projects-web`/`app-erp-assets-web`/`app-erp-master-data-web`（pom 实测），prj-web→prj-service / ast-web→ast-service 在 prj（12）/ast（7）迁移前既无法以 String 形态编译；`fin-web` 的 ast-dao/prj-dao 依赖为 test-scope。故 **M2.1 构建口径 = 5 模块（codegen,dao,meta,service,api），fin-web/fin-app 不重生成、不重建**，其本地仓 jar 维持最后全绿基线 Long 形态（仓内仅 `fin-app`/`app-erp-all` 依赖 fin-web/fin-app，无其他消费方）；补做 = M2.7 projects 完成后（fin 6 延后列 + hr 2 延后列同批翻转 + fin web/app 重生成 + 本计划 page.yaml 修复的运行时验证），M4.1 兜底核验。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-finance/erp-fin-{codegen,dao,meta,service,web,app,api}`；本计划 build/verify 范围 = 前 4 + api 共 **5 模块**。`erp-fin-service` main compile 依赖 **ast-dao + inv-dao + pur-dao + sal-dao + notify-dao + prj-dao**（6 外域 dao，全部未迁移 Long jar——正是 A2 桥接对象）；test 依赖 md-service + notify-service（均已 String）+ app-erp-common-test。fin-api 仅依赖 nop-api-core。
- **fin orm 外域关系（refEntityName 实测）**：md ×97（M1.1 起 fin-dao `_gen` md 关系胶水处于已登记中间态——M0 裁决 §10 登记 fin-dao 97 错/32 文件 100% `_gen`，**本域迁移即自愈**）+ prj ×6（延后列，Long↔Long 保持一致）+ ast ×0（ErpAstAsset stub 无关系边，仅顶点声明）。
- **M0.2 登记册 finance 视角（§6.6，起草消费已核）**：A1 orm 延后 = 6 条（见上）；**A2 main 桥接 10 条**（ast 3 = bridge-main-061/063/065（`ErpFinAccountingPeriodProcessor`，退役 owner M2.4）；inv 5 = 062/064/066/067/068（064 `reclosePeriodCosts(periodId)` 含 Long 参数语义级转换，其余类型级，退役 owner M2.2）；pur 1 = 069（`DualSideConsistencyChecker:8` ErpPurInvoice 类型级，M2.5）；sal 1 = 070（`:9` ErpSalInvoice 类型级，M2.6））；**A3 test 桥接 1 条**（bridge-test-118 `TestErpFinDualSideConsistency` → ErpPurInvoice@L8，owner M2.1 = 本计划 Phase 3 退役）；C1 后向 main 2 条（backward-152 → md 29 文件；backward-153 → notify 4 文件（`ErpFinPostingExceptionBizModel`/`ErpFinDeferredPostingRetryHelper`/`ErpFinPostingExceptionRecorder`/`ErpFinPostingExceptionIgnoreProcessor`，successor M2.1 = 本计划））；C2 后向 test 2 条（backward-211 → md 58 测试文件；backward-212 → notify 1 文件 `TestErpFinPostingExceptionNotify`）。B 退役义务 = 0。被引用面（其他域引用本域，successor = 各域 plan）：ast main 2 / test 12、hr main 3 / test 3、inv main 4 / test 12、logistics main 1 / test 4、mnt main 3 / test 7、mfg main 2 / test 9、prj main 3 / test 10、pur main 6 / test 27、qa main 2 / test 2、sal main 6 / test 26、cs（pom 级 fin-service 依赖但 main 零 import，编译中性）、drp（位次 18，登记册 §6.6 被引用清单未列 drp main——以登记册为准）。
- **手写代码冲击面（实测）**：fin-service main 含跨域 import 文件 = `ErpFinAccountingPeriodProcessor`（ast/inv，A2 桥接点）+ `DualSideConsistencyChecker`（pur/sal）+ md 29 文件（**登记册 backward-152 id 语境口径**；rg 全量 `import app.erp.md` = 31 文件，差 2 文件为非 id 语境——以登记册为修复定位基线 + 编译器清单兜底）+ notify 4 文件（C1）`.getId()` 调用 fin 范围以编译器实际清单为准（全仓基线 1176 处/352 文件，fin 为最大单域份额）。dao 手写 IBiz/值对象 Long 签名以编译器清单为准（M0.1 审计附录 C 登记本域语义 FK Long 参数清单为 Phase 4 门控输入）。
- **测试资产（实测）**：fin-service **93 个测试类**（本批最大、mission 至今最大：md 24 类 / contract 22 类先例）；`_cases` 快照 **2961 文件**（contract 重录后 1658 为此前最大——**本域重录规模约为 contract 的 1.8 倍，Phase 3 为 mission 最重单域步骤**）；`TestErpOrgIsolation`（`app/erp/common/org/`，:72 `ErpOrgContext.setCurrentOrgId(ctx, 2L)`）自 M1.3 起编译破坏（已登记中间态，successor = M2.1 = 本计划 Phase 3 兑付）；`ErpFinWebPagesTest` `@Tag("full-app")` + surefire excludedGroups 模块级排除（已提交治理决策，successor = M4.1）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 7 处，迁移即失效的实时缺陷）**：`erp/fin/pages/period-close-wizard/main.page.yaml` ×6（:71/:170/:245/:346/:386/:415 `$pid:Long` → `ErpFinAccountingPeriod__get(id:)`/`preCheck(periodId:)`/`closePeriod`/`finalizePeriod`/`reverseClose`）+ `erp/fin/pages/dashboard/main.page.yaml:29`（`$periodId:Long` → `getDashboardKpi(periodId:)`）。fin 翻转 String 后静态类型不匹配 → adaptor 静默降级、功能失效（M3.6 结束审计 MAJOR-1 同型缺陷，contract version-diff 已就地 Fix 先例）——**本计划就地 Fix**（改 `:String` + `|| 0` 字面量兜底改 `|| ''`）；但 fin-web 本计划不重建（延后约束），运行时/重建验证 successor = M2.7 补做 + M4.1。
- **已知风险（先例登记）**：① 平台 IoC 回归 `nopSequenceGenerator` self-wait——五域连续复现，按先例修复（test-scope VFS delta `_vfs/_delta/default/nop/sys/beans/app-dao.beans.xml` + DeltaOverride `delta-layer-ids` 补 default 层集）；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓未迁移 ast/inv/pur/sal/prj dao jar——域级测试按设计不跨这些边界的 Long API 面，A2 桥接点除外且为登记例外）。
- **回写机制（M0.1 裁定 Decision A，三步）**：① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-finance` 新鲜度门控（零非 stdDataType 行）；③ 单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：fin orm 208 列全 `stdDataType="long"` 待改（6 延后列除外）；fin 手写代码/测试/快照全部 Long 形态；冻结序位次 6（之后位次 7 assets、8 cs 解锁——本批内 plan 2/3 的批内前置）。

## Goals

- finance 域 208 列 `stdDataType` long→string 落源（唯一源文件变更，`stdSqlType` 保持 BIGINT，DDL 零变化）；6 条 A1 延后列保持 long（工具已按登记册豁免）。
- stub 对齐 Decision 裁定并落盘（md 18 列 + `ErpAstAsset` stub 1 列随工具默认翻转——ast 在 fin orm 内零关系边无约束面；`ErpPrjProject` stub ×2 经 Explore 实证后裁定翻转或登记册延后，retireOwner M2.7）。
- 增量重生成（**no-am 5 模块链**：codegen,dao,meta,service,api；fin-web/fin-app 延后）+ 编译器驱动修复 fin 全部手写代码 + A2 前向桥接 10 处落桥（ast 3 / inv 5 / pur 1 / sal 1，退役 owner M2.4/M2.2/M2.5/M2.6）。
- 快照每域重录（RECORDING→CHECKING，用户裁决；2961 文件基线）+ `TestErpOrgIsolation` 编译破坏兑付修复（M1.3 登记的 successor 义务）。
- 语义陷阱 grep 门控清零（路线图横切 §3 清单，fin 范围）+ page.yaml `:Long` 7 处就地 Fix（静态 + grep 验证）。
- 消费 M0.2 登记册：A2/A3 桥接 disposition 落盘本计划，C1/C2 修复定位面消费，heal M1.1 登记的 fin-dao `_gen` md 胶水中间态（97 错）。
- 路线图 M2.1 → `done` + 日志；冻结序位次 7（assets）、8（cs）解锁。

## Non-Goals

- 不重建/不重生成 fin-web 与 fin-app（路线图冻结裁决：延后至 M2.7 projects 完成后补做；本计划仅做 fin-web 手写 page.yaml 的源码级 `:Long` 修复，重建与运行时验证归补做/M4.1）。
- 不修复外域代码对 fin String API 的编译破坏（12 域被引用面——各域 plan 的 C 定位面 + M4.1 兜底；本计划仅在 Phase 4 登记确认）。
- 不迁移 prj/ast/inv/pur/sal（A2 桥接目标域与延后列权威源，归 M2.7/M2.4/M2.2/M2.5/M2.6）；不翻转 6 条 A1 延后列（归 M2.7 同批）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（保持 long）；不修 `ErpFinWebPagesTest` 治理排除（已提交决策，successor M4.1）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件；手写 view.xml 预期零改动（Phase 4 验证）。

## Task Route

- Type: `implementation-only change`（含保护区域 ORM 变更）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 6 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；finance 业务语义 owner doc = `docs/design/finance/`（Phase 4 注记对象）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + md/notify/common 链 install + **位次 3-5（aps/b2b/contract）链 install**（已就位）+ 未迁移 ast/inv/pur/sal/prj dao 基线 jar（Long 形态，正是桥接对象）。回滚策略：revert orm.xml + `mvn clean install -pl module-finance/erp-fin-codegen,module-finance/erp-fin-dao,module-finance/erp-fin-meta,module-finance/erp-fin-service,module-finance/erp-fin-api -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 3 完成后回滚需先 revert 测试代码**——`-Dmaven.test.skip=true` 跳过测试编译，String 测试代码对 Long main 会破坏 test-compile）。

## Execution Plan

### Phase 1 - 消费登记册 + stub Decision + orm 回写（保护区域，双批准前置）

Status: completed（2026-08-22 执行，证据见下方 Execution Record §Phase 1）
Targets: `module-finance/model/app-erp-finance.orm.xml`、`tools/id-migration-registry.json5`（仅 Decision B 分支；实际裁定 Decision A，登记册零变更）
Skill: none

- Item Types: `Proof | Fix | Decision | Explore`
- Prereqs: M1.1 ✅ + M1.2 ✅ + M0.2 ✅（精确前置已满足）；批内无前置（本计划为批次首个）；本计划已通过独立 plan-audit + 第二独立子 agent 复核（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [x] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.6 finance 节，逐条核对：(i) A1 orm 延后 = 6 条（本计划 Current Baseline 已逐行核对一致，执行时双源复核）；(ii) A2 main 桥接 10 条（ast 3/inv 5/pur 1/sal 1）与本地实测 import 对账；(iii) A3 = bridge-test-118 作为 Phase 3 定位面；(iv) C1 = backward-152（md 29 main 文件）+ backward-153（notify 4 main 文件）与 C2 = backward-211（md 58 测试文件）+ backward-212（notify 1 文件）作为 Phase 2/3 定位面；(v) 按 b2b A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(ast|inv|pur|sal|prj|qa|mnt|mfg|ct|crm|drp|log)\.' module-finance/erp-fin-service/src/test` 排除 import 行），命中则补登 + 处置。矛盾则按路线图规则 6 停止回报。
  - Skill: none
- [x] Explore: stub 未对齐权威源实证（Phase 1 内完成，Decision 前置；**scratch 副本隔离——不得在实仓工作树直接触发实验性重生成**）——在隔离副本（`_tmp` scratch 目录或临时 worktree）上将 `ErpPrjProject` stub（id/customerId）+ `ErpAstAsset` stub（id）3 列翻 string（6 延后列保持 long），触发 fin codegen+dao 重生成，观察：codegen 是否校验 join 类型一致（leftProp long ↔ stub id string）而报错；`_gen` 关系胶水按 stub 类型还是 classpath 实体类型生成。产出结论落盘本计划 + 实仓 `git status` 清洁证明（Explore 零残留）。
  - Skill: none
- [x] Decision: stub 3 列处置（**按 stub 分列裁定**）——`ErpAstAsset` stub（id，1 列）：fin orm 内 `refEntityName="app.erp.ast"` 关系边 = 0（实测），无 join 一致性约束面，随工具默认翻转（对齐「stub 元数据先行、权威源 M2.4 跟进」，不依赖 Explore 结论）；`ErpPrjProject` stub（id + customerId，2 列）：基于 Explore 结论二选一并记录选择、替代方案、残留风险：(A) codegen 不校验 join 类型一致且胶水取 classpath 类型 → 随工具默认翻转（总变更面 208），登记「stub 元数据先行、权威源 M2.7 跟进」；(B) codegen 报错或胶水错型 → 2 列登记册延后（新增 orm-deferral 条目，retireOwner = M2.7（与 fin 6 延后列同批回收，不涉及其他域 plan），工具豁免机制同步 + fail-closed 负面验证），本计划变更面收窄为 206。框架强制或明显的选择可记为约束，无需完整替代方案分析。
  - Skill: none
- [x] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间），未获批不得进入回写。
  - Skill: none
- [x] Fix: 回写 orm（M0.1 裁定三步机制）——① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs module-finance` 新鲜度门控（零非 stdDataType 行）；③ 门控通过后单文件落源（Decision B 分支则落源后回退 `ErpPrjProject` stub 2 列 + 登记册/工具豁免同步）。禁止盲 cp、禁止 apply 模式。
  - Skill: none
- [x] Proof: `git diff module-finance/model/app-erp-finance.orm.xml` 逐行核对——仅既定列集 `stdDataType="long"→"string"`（208 或 Decision B 的 206），`stdSqlType` 零变化、`delVersion`/标签结构/6 延后列零变化；scan finance 段重扫：`NEEDS FIX` = 0（Decision B 分支则 prj stub 2 列转 `DEFERRED(registry)`）。
  - Skill: none

Exit Criteria:

- [x] 登记册消费核对在案（含 FQN 盲区复扫结论）；stub Decision 及其 Explore 证据在案；双批准记录在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确

Execution Record §Phase 1（2026-08-22）:

- **登记册消费核对**：(i) A1 延后 6 条 = orm-deferral-001..006（registry 与 Current Baseline 双源逐行一致，line evidence 517/958/1361/1417/1859/2063 吻合）；(ii) A2 main 10 条与本地 import 对账吻合——`ErpFinAccountingPeriodProcessor`（ast: L3/L4 import + :194/:222/:256/:536/:537/:544 调用点 = 061/063/065 + 062/064/066/067/068）+ `DualSideConsistencyChecker`（pur L8 = 069，sal L9 = 070）；(iii) A3 = bridge-test-118（`TestErpFinDualSideConsistency.java:8`，status active，owner M2.1）；(iv) C1 = backward-152（29 md main 文件清单核对一致）+ backward-153（4 notify main 文件一致）；C2 = backward-211（58 md 测试文件一致）+ backward-212（`TestErpFinPostingExceptionNotify` 一致）；(v) FQN 盲区复扫：`rg 'app\.erp\.(ast|inv|pur|sal|prj|qa|mnt|mfg|ct|crm|drp|log)\.' module-finance/erp-fin-service/src/test --glob '*.java'` 全量命中 = 仅 `TestErpFinDualSideConsistency.java:8`（= 已登记 bridge-test-118，import 行）——**无新增盲区，零补登**。
- **Explore 实证（隔离 worktree `/var/.../T/opencode/m21-explore` @ HEAD 773d7869a，已移除）**：worktree 内 dry-run 刷新 → 单文件落源（208 列，3 stub 列含）→ `mvn clean install -pl module-finance/erp-fin-codegen,module-finance/erp-fin-dao -Dmaven.test.skip=true` → **BUILD SUCCESS**。结论：**(a) codegen 不校验 join 类型一致、不报错**——prj join（leftProp projectId=long ↔ stub id=string）静默降级为弱类型胶水 `this.orm_propValue(PROP_ID_projectId, refEntity.getId())`，模板依据 `nop-templates/orm-entity/_…java.xgen` 的 `<c:when test="${join.leftType == join.rightType}">`（匹配→强类型 setter；不匹配→弱类型 orm_propValue）；**(b) 胶水形态 = 混合**：强/弱判定取 **fin orm 模型类型（stub 声明）**，调用表达式取 **classpath 实体类型方法**（`refEntity.getId()`，prj-dao jar Long 形态）——无「错型」编译破坏。md 关系（双侧 String）重生成后恢复强类型 `setAcctSchemaId(refEntity.getId())`，97 处 `_gen` 中间态错误自愈（dao 编译零错误实证）。**运行时合并规则补充证据**：`OrmModelLoader.merge`（nop-orm-model）对同名实体——先合并方为 notGenCode stub 时后被真实声明**替换**（`baseEntity.isNotGenCode() → addEntity(ext)`）；先真实后 stub 时 stub 被丢弃（else 分支无操作）→ **stub 翻转在 prj/ast-dao 于 classpath 时对运行时实体模型零影响**（fin-dao→prj-dao main、fin-service→ast-dao main，权威恒在场）。副产物观察：codegen+dao 两模块构建仅改写 dao `_gen` 36 文件 + `_app.orm.xml` + model（38 files），fin-web/fin-app/meta/service 文件零触碰（存在即不覆盖 + web 页面归 fin-web 自身 precompile）——Phase 2「fin-web/fin-app 不重生成」操作性成立。实仓 `git status` 清洁证明：Explore 后实仓仅 3 个本批 plan 未跟踪文件，零工作树残留。
- **Decision：选 (A)，3 stub 列全部翻转（总变更面 208）**。选择依据：Explore 证明 codegen 不报错、胶水无错型（弱类型回退可编译）+ 运行时合并规则使 stub 元数据先行零运行时影响 + ast stub 零关系边本就无约束面。替代方案 (B)（prj stub 2 列登记册延后）被弃：其唯一增益是 6 处 prj join 胶水保持强类型 Long↔Long，代价是登记册 +2 条目与 M2.7 回收复杂度；而 (A) 下 6 处弱类型胶水在运行时仍为 Long→Long（projectId 保持 long + prj-dao getId() 返回 Long），仅失去编译期类型检查。残留风险（retireOwner M2.7）：M2.7 前窗口内 6 处 join 为弱类型 `orm_propValue` 赋值（无编译期防护）；M2.7 同批翻转 prj 权威源 + fin 6 延后列后 leftType==rightType==string，强类型胶水自动恢复。「stub 元数据先行、权威源 M2.7 跟进」登记于 Deferred But Adjudicated（既有 prj 延后条目覆盖）。登记册零变更（Decision A 分支）。
- **双批准记录核验**：Draft Review Record 已载批准 1（ses_fdaecac03ffezjlpdWu00j4cJW，技术视角）+ 批准 2（ses_fdaec67abffet95xJrKGrvjzSu，治理视角），2026-08-22，批准范围 = 208 列或 Decision-B 206 列 + 6 登记册延后列保持 long——本次落源 208 列在批准范围内。
- **回写三步证明**：① dry-run 时点刷新（14 文件 1466 列全仓，finance 段 208）；② `verify-id-fix-copy-diff.mjs module-finance` 门控通过——「变更行: 208（登记册延后列 6 保持 long），非法差异行: 0，允许落源」；③ 单文件 `cp _tmp/bigint-id-string-fix/module-finance/model/app-erp-finance.orm.xml module-finance/model/`（非 apply 模式）。
- **git diff 逐行核对**：208 insertions / 208 deletions；+208 行全部含 `stdDataType="string"`、-208 行全部含 `stdDataType="long"`、非 stdDataType 内容差异 = 0（门控 norm 校验数学证明：仅 stdDataType 属性差异）；stdSqlType/delVersion/标签结构零变化；scan 重扫 finance 段 `NEEDS FIX: 0`，6 延后列 `DEFERRED(registry)` 口径与登记册一致（hr 2 条延后同样在册）。

### Phase 2 - 增量重生成 + 主代码编译修复 + A2 桥接落桥（no-am 5 模块）

Status: completed（2026-08-22 执行，证据见下方 Execution Record §Phase 2）
Targets: `module-finance/erp-fin-dao/src/main/java/**`、`module-finance/erp-fin-service/src/main/java/**`（手写 IBiz/BizModel/Processor/Job/Engine/SPI；api beans 生成件随动）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] Fix: `mvn clean install -pl module-finance/erp-fin-codegen,module-finance/erp-fin-dao,module-finance/erp-fin-meta,module-finance/erp-fin-service,module-finance/erp-fin-api -Dmaven.test.skip=true`（D3 口径：**5 模块显式列表**、不带 `-am`、`-Dmaven.test.skip=true`；fin-web/fin-app 延后不建）触发增量重生成。预期：fin-dao `_gen` md 关系胶水（97 处）自 M1.1 的登记中间态自愈；`install`（非 package）落本地仓供位次 7/8 消费。
  - Skill: `nop-backend-dev`
- [x] Fix: 编译器驱动修复主代码——逐条修复 fin dao + service 手写代码类型错误（定位面：md 29 文件（C1）+ notify 4 文件（C1，`notify(String,Map,ctx)` 签名不变预期零破坏——M1.2 已证，编译核验即可）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游；以编译器实际清单为准），直到 5 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划；测试编译错误由 Phase 3 首轮 `test-compile` 产生后修复。
  - Skill: `nop-backend-dev`
- [x] Fix: A2 前向桥接 10 处落桥（D4 消费协议）——fin String id ↔ ast/inv/pur/sal Long API 的调用点加转换桥（`ErpFinAccountingPeriodProcessor` 8 处（ast 3 + inv 5，含 bridge-main-064 `reclosePeriodCosts(periodId)` 参数语义级 toLong）+ `DualSideConsistencyChecker` 2 处类型级核验），每处登记 grep 例外清单（条目 id + file:line + 转换方向），退役 owner M2.4（ast 3）/M2.2（inv 5）/M2.5（pur 1）/M2.6（sal 1）。**语义级过滤值桥主动识别**（eq 过滤 Long 列传 String 静默空匹配——contract 037/038 先例）：grep 本域对 ast/inv/pur/sal 实体的 `eq(`/`filter` 调用逐条核对。
  - Skill: `nop-backend-dev`
- [x] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（5 模块全绿，reactor 不含外域模块）；未登记破坏按路线图规则 6 停止回报；已登记破坏按中间态继续并履行登记义务。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] fin 5 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）且已 install；主代码修复清单 + A2 桥接例外清单在案

Execution Record §Phase 2（2026-08-22）:

- **增量重生成 + install**：`mvn clean install -pl <5 模块> -Dmaven.test.skip=true` 全绿（codegen/dao/meta/service/api 5×SUCCESS）。fin-dao `_gen` md 关系胶水自愈（双侧 String 恢复强类型 setter，Explore 阶段已实证）；jar 已 install 供位次 7/8 消费。
- **主代码修复清单（编译器驱动，约 60 文件轮次收敛）**：dao 侧（上轮已落）+ service 侧本轮修复——FinPostingExecutor.postEvent 返回 Long→String；ErpFinNotesReceivableBizModel 7 mutation 签名（notesId/bankId/endorsementFromId）；ErpFinNotesPayableBizModel + Issue/Honor/Dishonor/WriteOff Processor 4 签名；ErpFinBankStatement(BizModel/ImportProcessor)、ErpFinBankReconciliation(Generate/Post/Reverse Processor + BizModel)、ErpFinBankReconAutoReverseHelper.reverseOne、ErpFinBankStatementLineBizModel + AutoMatch/ManualMatch Processor；ErpFinBadDebt 3 per-mutation Processor `Long.valueOf(id)` 剥离 + BizModel 9 mutation 签名；ErpFinAccountingPeriodBizModel 5 mutation + Finalize/Open Processor；ErpFinExpenseClaimBizModel.cancel + Processor（loadLines/resolveBudgetSubjectId/resolvePeriodId）；ErpFinEmployeeAdvanceBizModel cancel/cashRepay/reverseCashRepay/requireAdvance；ErpFinReconciliationBizModel + Create/Post/Reverse/RunAutoReconciliation Processor + AbstractErpFinReconciliationProcessor（assertOpen/assertNotOver/loadItem/requireHead/loadLines/resolvePeriodId）；AutoReconciliationEngine（matchAndBuild/matchFifo/matchByAmount/matchByRatio/indexOpen/unmatched/findPartnersWithOpenItems 全 String 化）；PartnerBalanceUpdater.refresh/sumOpen；Budget 域——BudgetVoucherGenerator（generate/reverse/writeBudgetVoucher/VoucherFact 内部 DTO，projectId 保 Long 延后）、ErpFinBudgetScenarioProcessor（requireScenario/loadBudgetLines/generateBudgetVoucher/reverseBudgetVoucher）、BudgetScenario BizModel + RollForward/CarryForward Processor（remapPeriodId/resolveFirstPeriodId）、BudgetLineBizModel.getBudgetVsActual、CommitmentVoucherGenerator + ErpFinBudgetCommitmentBizModel（commit/release/releaseIfPresent/resolveCurrencyId/resolveOrgAndSchema，默认 id 字面量 1L→"1"）；posting 域——ErpFinPostingProcessor（process/reverseProcess/persistVoucher×2/alreadyPosted/resolveOpenPeriod/findCurrencyById/resolveAcctSchemaIdFromContext/translateFactsForSchema/loadLines/dispatchReversalEvent + findPostedVouchers 排序改 `Comparator.nullsLast(naturalOrder)` 消除 `Long.MAX_VALUE` 哨兵）、AcctDocContext（acctSchemaId/orgId/currencyId/periodId String 化）、VoucherFact（subjectId/orgId/partnerId/departmentId/warehouseId/materialId/costCenterId String 化；projectId 保 Long）、VoucherReversedEvent、ErpFinArApItemGenerator.resolvePartnerId→asId（billData id 桥接：Number→String 归一，注释标注 A2 中间态/回收 owner M2.5/M2.6）、ErpFinGlMappingResolver（resolveSubjectCode 覆盖接口 String 签名 + cacheKey/loadFromCache/loadFromDb/lookupMaterialCategoryId）、ErpFinTransferPriceResolver（resolvePrice 覆盖 + loadCandidates/cacheKey/matchesMaterial/lookupMaterialCategoryId）、GlDistributionTarget/ErpFinGlDistributionRule（costCenterId）；provider 域——EmployeeAdvance/ExpenseClaim/NotesPayable/NotesReceivable 4 AcctDocProvider 的 asLong→asId（billData id 桥接，同 ArApItemGenerator 范式）、ErpFinTemplateAcctDocProvider.findTemplate acctSchemaId；intercompany——IntercompanyVoucherGenerator 全 id 参数 String 化、ErpFinIntercompanyTransferBizModel（onTransferConfirmed/onTradeDocumentApproved/onTradeDocumentReversed 覆盖接口 String 签名 + resolveLegalEntityRoot/resolveCounterpartyLegalEntity/resolveWarehouseOrgId/resolveTransferCode/resolveOrgAcctSchemaId/resolvePeriodId，默认账套/币种字面量 1L→"1"）；ErpFinVoucherBizModel 经 postingProcessor.process/reverseProcess 返回 String 自愈；ErpFinPostingExceptionIgnoreProcessor.ignore/requirePending + ErpFinDeferredPostingRetryHelper.retry/doRetry；ErpFinCashForecastRefreshForecastProcessor.newForecast；ExpenseClaimPostingDispatcher/EmployeeAdvancePostingDispatcher/NotesPostingDispatcher/AdvanceOffsetOrchestrator（postEvent String + resolveEmployeePartnerId/findOldestOpenAdvanceItem/settleAdvanceId）；CreditFacility BizModel + Reserve/Release Processor（requireFacility）；ErpFinDashboardBizModel（getDashboardKpi/loadGlBalances/findLatestPeriodId/sumArApOpen/resolvePeriodOrgId/applyOrgAndSchemaScope）；ErpFinReportBizModel（asLong→asId + 6 dataset 构造 + loadGlBalances/loadPostedVoucherLines/loadPeriodStatus/countBillR/resolvePeriodOrgId/resolveOrgSchemaId/applyOrgAndSchemaScope/applySchemaScope + GlBalance subjectId 排序改 nullsFirst naturalOrder）；ErpMdEmployeeReferenceCheckerImpl.countReferences/countAdvances。notify 4 文件（C1 backward-153）编译零破坏实证（签名 String,Map,ctx 不变——M1.2 先例复现）。
- **A2 桥接例外清单（10 条全落位）**：064 = 语义级转换桥（`ErpFinAccountingPeriodProcessor:224` `ConvertHelper.toLong(period.getId())`，fin String periodId → inv Long periodId，退役 owner M2.2，代码内 bridge 注释双向指针）；061/066/067/068 = 类型级（ast `ErpAstDepreciationSchedule` / inv `ErpInvLandedCost` 实体引用 + 非列过滤，:255/:521 `eq("period", period.getCode())` 为 VARCHAR code 列非 id 列、`eq("posted"/"status"/"approveStatus")`/日期范围均为非 id 值——零转换）；062 = 类型级（`CostingRecloseReport` 返回值类型引用）；063 = 类型级（`executeBatchDepreciation(period.getCode(), ctx)` 传 String code 非 id）；065 = 类型级（`reverseDepreciation(s.getAssetId(), ...)` ast 实体 Long assetId → ast biz Long 形参，同型直传零转换）；069/070 = 类型级（`DualSideConsistencyChecker` 按 `eq("code", code)` String code 关联 ErpPurInvoice/ErpSalInvoice，无 id 值交叉）。**语义级过滤值桥主动识别结论**：grep fin-service main 全部 `eq(/in(/ge(/le(` 于 ast/inv/pur/sal 实体的过滤调用逐条核对——全部为 code/status/posted/date/非 id 字段（ast schedule.period 为 VARCHAR 实证 orm:334），**零静默空匹配风险点**，无需新增过滤值桥。
- **自身链破坏处置**：5 模块 reactor 全绿 = 零自身链破坏；外域模块不在 reactor（no-am），被引用面 12 域破坏为登记册已登记中间态（Deferred But Adjudicated），无需停止回报。

### Phase 3 - 测试修复 + A3 桥接适配 + 快照重录 + 域级测试（含 TestErpOrgIsolation 兑付）

Status: completed（2026-08-22 执行，证据见下方 Execution Record §Phase 3）
Targets: `module-finance/**/src/test/**`、`module-finance/erp-fin-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [x] Fix: 测试代码修复——93 个测试类的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），**含 `TestErpOrgIsolation:72` `setCurrentOrgId(ctx, 2L)` M1.3 登记编译破坏兑付**（改 String 字面量），逐文件修复至测试编译通过；测试编译错误由本轮 `test-compile` 产生。
  - Skill: `nop-testing`
- [x] Fix: A3 test 桥接适配（bridge-test-118）——`TestErpFinDualSideConsistency` 引用 ErpPurInvoice（pur，位次 15 未迁移）的 id 形态桥接（String↔Long 局部转换，与 Phase 2 桥接同型），适配后在登记册退役该条目（owner M2.1 = 本计划）。
  - Skill: `nop-testing`
- [x] Fix: C2 后向 test 适配——md 58 + notify 1 测试文件对 String 化 API 的适配（M1.2 登记的 notify successor 义务兑付；md 侧自 M1.1 即 String，为存量后向修复面）。
  - Skill: `nop-testing`
- [x] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 fin service 测试 → 逐案审核 `_cases/` 新形态（2961 文件基线，mission 最大单域重录；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）与审核结论记录本计划。
  - Skill: `nop-testing`
- [x] Proof: `mvn test -pl module-finance/erp-fin-service`（D3 口径：no-am；**本域 verify 不含 fin-web**——延后约束，`ErpFinWebPagesTest` 本就不在本计划运行面）全绿。若复现平台 IoC 回归，按五域先例修复（test-scope VFS delta + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`

Exit Criteria:

- [x] fin 域级测试全绿（93 测试类，含 TestErpOrgIsolation 编译破坏兑付）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案

Execution Record §Phase 3（2026-08-22）:

- **测试修复清单（编译器驱动，约 10 轮 javac 收敛）**：直接修复 16 文件（TestErpFinAging、PeriodCloseTestSupport 基类、NotesPayable/NotesReceivable/DepreciationIntegration/PeriodCloseEndToEnd/ReverseCloseAuditTrail/VoucherReversePreview/ReportRenderPerf/MultiSchemaReportIsolation/TransferPriceResolver/PeriodCloseEndToEnd/VoucherTemplateAuditLog 等）+ 5 批并行子代理修复 54 文件（posting/entity/reconciliation/bankrecon/treasury/perf/intercompany/dashboard/report 全域）——合计 93 测试类全部 String 化（id 局部变量/holder 数组/seed helper 签名/`1L`→`"1"` 字面量/断言值）。保留 Long 的合法非 id 用途在案：`long seed = System.nanoTime()` 唯一性种子（bankrecon ×22）、count 返回值、perf 计时、GlMappingRule.projectId（延后列）。一处签名冲突消解：TestErpFinProfitLossClosing `netCredit/netDebit(String,String)` 双重载（原 Long periodId 区分）改 `netCreditById/netDebitById` 分名。
- **TestErpOrgIsolation 兑付（M1.3 successor 义务）**：:72 `setCurrentOrgId(ctx, 2L)` → `"2"`（ErpOrgContext 已 String 语义 M1.3），:87 ContextProvider attr `2L` → `"2"`，seedArApItem/断言全 String 化；两测试全绿。
- **A3 bridge-test-118 适配**：`TestErpFinDualSideConsistency` seed 侧 `inv.setSupplierId(Long.valueOf(partnerId))` 局部桥（fin String partnerId → pur Long supplierId，代码内 bridge 注释 + 退役 owner M2.5）；pur 侧 `setCurrencyId(1L)` 保持 Long（pur 实体未迁移）；断言 `Long.valueOf(partnerA).equals(...)` → `partnerA.equals(...)`。登记册退役见 Phase 4。
- **平台 IoC 回归复现 + 修复 + 先例缺陷发现**：首跑复现 `nopSequenceGenerator` self-wait（六域连续复现）→ 按先例落 test-scope VFS delta。**执行中发现先例 delta 缺陷**：五域先例 delta 文件缺 `x:extends="super"`（delta-customization.md 规则 3），导致**整文件替换**平台 `nop/sys/beans/app-dao.beans.xml`——静默丢弃 `nopOrmEntityChangeLogInterceptor`（finance audit tagSet 实体依赖：TestErpFinVoucherTemplateAuditLog 2 失败 0 日志实证）与 `nopCodeRuleGenerator`/`SysDictLoader`/`SysI18nMessageLoader` 等同文件 bean（md/notify/aps/b2b/contract 五域测试未触及故未暴露，潜在同源影响登记 docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md，fin 侧 delta 已修正：`x:extends="super"` + 仅 replace nopSequenceGenerator bean，audit/其他 bean 经继承保全，实证 interceptor 恢复 + self-wait 修复双绿）。
- **id 序语义修复（主代码，contract ApprovalWorkflowEngine idOrder 同型）**：`ErpFinEmployeeAdvanceBizModel.findLatestUnreversedCashRepayLink` 的 `.max(comparing(getVoucherId))` 在 String 字典序下不再等价时间序（"9">"10"）——改按 `getBillCode` 排序（billCode 内嵌 13 位等宽毫秒时间戳，字典序==时间序），TestErpFinEmployeeAdvanceCashRepayReversal 多笔还款仅红冲最近一笔用例由红转绿。
- **快照重录足迹（mission 最大单域）**：RECORDING 全量跑 497 测试 = 0 真实失败 + 279 预期 `snapshot-finished`；`_cases/` 2961 → 4108 文件 = **554 内容 diff + 1147 新增落盘**（此前无 input/output tables 的方法全量首录，与 contract +1118 新增同量级）+ 0 删除。审核抽样：CrudSmoke `response.json5` id `"2"` String 形态 + `@var` 绑定漂移（NopSysChangeLog.newValue 先捕获——@var 机制自身一致，CHECKING 可回放）；`nop_sys_change_log.csv` 行序变化 = String sid 字典序（确定性保持）；**零非确定性单元格**（`System.nanoTime()` 派生 refNo 均经 input tables 回放或 @var 掩码，无需 `*` 通配修正）。注解还原：grep RECORDING/forceSaveOutput 零残留（66 文件还原）。**CHECKING 复跑 497/497 全绿 ×2 次**（稳定性确认）。

### Phase 4 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: completed（2026-08-22 执行，证据见下方 Execution Record §Phase 4）
Targets: `module-finance/**`（手写代码 + fin-web 手写 page.yaml 源文件）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`
- Prereqs: Phase 3

- [x] Proof: 语义陷阱 grep 门控（路线图横切 §3，fin 手写 main+test 范围）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中（A2 桥接转换点为登记例外，逐条列于例外清单并标注退役 owner）；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱（String 字典序 "9">"10"——contract ApprovalWorkflowEngine `idOrder` 先例）专项 grep：`getId\(\)\s*[<>]|comparing.*getId`；残留 `Long` 逐条判定合法非 id 或登记 successor；sql-lib.xml 仓内零存在（M0.1 已核，注明即可）。结果逐项记录本计划。
  - Skill: none
- [x] Fix: fin-web 手写 page.yaml raw-GraphQL `:Long` 变量 7 处就地 String 化——period-close-wizard ×6（`$pid:Long` → `$pid:String`，`|| 0` 类 Int 字面量兜底改 `|| ''`，variables 链一致性核证）+ dashboard:29（`$periodId:Long` → `:String`）；随后 `rg ':Long' module-finance/erp-fin-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 的 `$lim:Int` 等合法保留并逐条判定）；**静态 YAML 良构校验**（编辑后 2 文件逐个 parse 验证——fin-web 不重建，畸形编辑须在本计划内捕获而非遗留至 M2.7 补做）。**fin-web 不重建**（延后约束）——重建/运行时验证 successor = M2.7 补做 + M4.1（Deferred But Adjudicated 登记）。
  - Skill: none
- [x] Proof: 手写 view.xml 零改动验证——`git status module-finance` 确认无手写 view 文件被动变更（fin-web `_gen` 本计划不重生成，预期 web 目录仅 page.yaml 手写修复 diff；生成件层零变化）。
  - Skill: none
- [x] Add: 登记册状态更新——A3 桥接 1 条（bridge-test-118）status → retired（owner M2.1）；A2 main 桥接 10 条保持 active（退役 owner M2.4/M2.2/M2.5/M2.6，代码内 bridge 注释双向指针）；Decision B 分支则同步新增 stub 延后条目；被引用面确认（12 域清单，successor = 各域 plan + M4.1 兜底）。
  - Skill: none
- [x] Add: owner doc 注记——grep `docs/design/finance/` 中关于 fin id 为 Long/数字的陈述；存在则就地注记 Java 层已 String 化（引用本计划），不存在则记录「零 Long id 陈述，零文档变更」结论。
  - Skill: none
- [x] Add: 路线图 M2.1 → `done`（M2/M3 表位次 6 + 头部「最后更新」；位次 7 assets、8 cs 解锁；**web/app 延后补做义务显式移交给 M2.7 后续做载体——若 M2.7 plan 未覆盖则 M4.1 兜底**）+ 日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [x] grep 门控零残留（例外为零或逐条核清记录 + 桥接例外清单在案）；page.yaml `:Long` 清零 + view 零被动变更在案
- [x] 路线图状态、登记册退役、日志三者一致

Execution Record §Phase 4（2026-08-22）:

- **grep 门控结果**：`.longValue()` ×5 = **登记例外**（asId billData 桥本体：ErpFinArApItemGenerator:333 + EmployeeAdvance/ExpenseClaim/NotesPayable/NotesReceivable 4 AcctDocProvider，Number→String 归一，退役 owner M2.5/M2.6 随 pur/sal 翻转回收）；`Long.parseLong` ×2 = 合法非 id（ErpFinReportBizModel asDate 的 epoch 毫秒/秒日期解析）；`Map<Long`/`Set<Long`/`%d` 及变体 = 0 命中；装箱 id `==`/`!=` = 仅 null 检查（5 处 `== null`/`!= null`，无值比较）；**id 序专项**：`getId()[<>]|comparing.*getId|comparingLong` 0 命中——id 序点已全部改造（findLatestUnreversedCashRepayLink 改 billCode 时间戳序 + ErpFinPostingProcessor.findPostedVouchers/ReportBizModel GlBalance 排序改 Comparator.nullsLast/nullsFirst(naturalOrder)，Phase 3 记录）；残留 Long 逐条核清 = VoucherFact/GlMappingDimensions/BudgetVsActualRow/TestErpFinGlMappingResolver 的 projectId（登记册延后列，M2.7）+ ErpMdEmployeeReferenceCheckerImpl count 值 + PostingRun/PerfTiming nanos 计时 + asDate epoch parse；sql-lib.xml fin 仓内零存在（M0.1 已核，复核维持）。另清理取消链 Long 内传（EmployeeAdvance/ExpenseClaim CancelProcessor `Long.valueOf(id)` 剥离 + 4 处 String.valueOf 中转剥离 + 双重载合并）。
- **page.yaml Fix**：7 处 `:Long`→`:String`（period-close-wizard :71/:170/:245/:346/:386/:415 + dashboard :29，python 精确替换 6+1 实证）；id 变量无 `|| 0` 兜底（grep 核证——`|| 0` 仅 allowance 金额字段（Number 语义合法保留）；variables 链 `${periodId}`/`${period.id}` 均为 String 形态一致）；`rg ':Long'` fin-web `_vfs` 非 `_gen` = **0 残留**；`$lim:Int` ×3+ = 分页 limit 非 id 合法保留；2 文件 YAML parse 良构校验 2/2 PASS。
- **view 零改动**：`git status module-finance` = 仅 2 page.yaml + orm.xml（模型）+ 生成件/Java/快照/delta，**0 view.xml 变更**。
- **登记册**：bridge-test-118 status active→retired（note 载 2026-08-22 M2.1 兑付 + seed 侧 Long.valueOf 局部桥 + M2.5 pur 翻转时回收核对指针）；registry JSON 可解析复核通过；A2 10 条保持 active（064/061/062/063/065/066/067/068/069/070，Phase 2 例外清单在案）。
- **owner doc 注记**：`docs/design/finance/` grep——`gl-mapping-rules.md` §2 字段表含 BIGINT 类型陈述 → 就地注记（Java 层已 String 化 + DB BIGINT 语义澄清 + projectId 延后列说明，引用本计划）；`costing-methods.md` Long 引用为 inv 域设计草图（ErpInvCostAdjust 等，inv 未迁移 M2.2，陈述准确）零变更；其余 owner docs 零 id 类型陈述。
- **路线图 + 日志**：M2.1 行 → done（证据摘要含 web/app 延后义务显式移交 M2.7/M4.1）+ 头部最后更新 2026-08-22；`docs/logs/2026/08-22.md` 新建（四 Phase 产出 + 全绿验证状态 + 中间态 successor 清单 + 下一步）。

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，双独立子 agent fresh session）：
  - 审查者 A（技术/执行视角 plan-audit，ses_fdaecac03ffezjlpdWu00j4cJW）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 5 MINOR。事实核对全部属实（scan 208 = 自有 187 + stub 21 精确；6 DEFERRED 行号逐行吻合 orm-deferral-001..006；fin-app/fin-service pom 依赖逐条核实；fin-web/fin-app 消费方仅 fin-app/app-erp-all；93 测试类/2961 `_cases`；TestErpOrgIsolation:72；page.yaml 7 处行级吻合；A2 10 条含 064 reclosePeriods 真实语义桥；md 97/prj 6/ast 0 关系边；dry-run 副本证实 3 stub 列翻转 → Explore/Decision 项正确接地；5 模块延后操作化成立）。MINOR：① orgId ×48 为 XML 原始口径（真实 FK 列 28）→ 已修正；② 回滚命令 `-DskipTests` test-compile 陷阱 → 已改 `-Dmaven.test.skip=true` + 阶段差异注明；③ Phase 1 Item Types 漏 `Explore` → 已补；④ md 29 文件为登记册 id 语境口径（rg 全量 31）→ 已标注口径；⑤ Decision B 应按 stub 分列（ast stub 零关系边可无条件翻转；仅 prj stub ×2 受 Explore 约束，retireOwner M2.7——同时消除与 plan 2 Non-Goals 的所有权冲突）→ 已按建议分列修正。
  - 审查者 B（治理/规范视角，ses_fdaec67abffet95xJrKGrvjzSu）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 2 MINOR。治理检查全过（命名 N=1 / Mission/Work Item/位次 6 对齐 / 依赖 = M1.1+M1.2 精确 / web-app 延后为执行冻结裁决非重新诉讼 / 登记册 §6.6 全集逐条消费 + FQN 复扫覆盖全晚域 / 保护区域双批准门控 + 五要素证据链 / 5 模块 D3 变体由冻结裁决正当化 / 无全量构建）。MINOR：① Explore 隔离不明（须 scratch 副本 + 实仓 git status 清洁证明）→ 已补；② page.yaml 修复仅 grep 验证（fin-web 不重建）→ 已补静态 YAML 良构校验 Proof。
  - 批准后 MINOR 修订注记：全部 MINOR 均为事实精度/流程补齐与审查者自行建议的 Decision 分列，未变更范围、D3/D4 命令口径与批准依据（orm 变更面仍 = stdDataType-only，208/206 列 + 6 延后列保持 long）。
  - **双独立子 agent 批准（保护区域 `model/*.orm.xml`，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
    - 批准 1（技术视角）：ses_fdaecac03ffezjlpdWu00j4cJW，2026-08-22 — 「批准 M2.1 finance orm 保护区域变更（技术视角批准）」。依据：方案 B 机制（stdDataType only、stdSqlType BIGINT 不变、DDL 零变化）+ M0.1 seq-string Proof + 五域绿先例 + 变更面机器复核（208 列或 Decision-B 206 列 + 6 登记册延后列保持 long）+ stub 翻转经 Explore 证据门控 + 5 模块 no-am carve-out（fin-web/app 非消费方已实证）。
    - 批准 2（治理视角）：ses_fdaec67abffet95xJrKGrvjzSu，2026-08-22 — 「批准 M2.1 finance orm 保护区域变更（治理视角批准）」。依据：证据链完整（方案 B + §16A.4 + M0.1 Proof + M0 裁决 + 文件内双批准门控），变更面机器门控且精确有界，唯一偏离（5 模块 verify）为路线图自身冻结裁决而非计划自创范围裁剪。
- 共识达成（2026-08-22）：iteration 1 双审查者 0 BLOCKER / 0 MAJOR + 保护区域双批准 → 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。**本域 verify = 5 模块（不含 web/app，延后冻结裁决）**。

- [x] 范围内行为完成（208/206 列落源 + no-am 5 模块重生成 + 手写代码/测试修复 + A2 落桥与 A3 退役 + TestErpOrgIsolation 兑付 + 快照重录 + grep 门控清零 + page.yaml 7 处 Fix）
- [x] 相关文档对齐（owner doc 注记结论、路线图 M2.1 状态、登记册退役/更新、日志）
- [x] 已运行验证：`mvn clean install -pl module-finance/erp-fin-codegen,module-finance/erp-fin-dao,module-finance/erp-fin-meta,module-finance/erp-fin-service,module-finance/erp-fin-api -DskipTests` 全绿 + `mvn test -pl module-finance/erp-fin-service` 全绿 + 工具重扫零残留（finance 段 `NEEDS FIX` = 0，延后列 `DEFERRED(registry)` 口径一致）
- [x] 无范围内项目降级为 deferred/follow-up（web/app 延后为路线图冻结裁决的显式移交非范围降级；page.yaml 运行时验证为延后约束下的显式 successor 登记）
- [x] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### fin-web / fin-app 重生成与重建（路线图冻结裁决）

- Classification: `watch-only residual`
- Why Not Blocking Closure: fin-app main 依赖 prj-web/ast-web（未迁移至位次 12），fin-web/app 重建在 M2.7 前不可行；本地仓陈旧 Long jar 无其他消费方（仅 fin-app/app-erp-all）
- Successor Required: `yes`（M2.7 projects 完成后补做：fin web/app 重生成 + fin 6 延后列/hr 2 延后列同批翻转 + page.yaml 运行时验证；M4.1 兜底核验）

### fin 手写 page.yaml `:Long` 运行时验证

- Classification: `watch-only residual`
- Why Not Blocking Closure: 源码级 String 化 + grep 清零在本计划完成；fin-web 不重建（延后约束），adaptor 行为级验证无法在本计划执行
- Successor Required: `yes`（M2.7 补做重建 + M4.1 mission 级 page.yaml `:Long` 清扫与页面验证）

### A2 main 桥接 10 处（ast 3 + inv 5 + pur 1 + sal 1）+ asId billData 桥 ×5

- Classification: `watch-only residual`
- Why Not Blocking Closure: D4 登记册预先登记的中间态桥接——目标域未迁移（位次 7/10/15/16），桥接点为编译必需（asId 桥 = pur/sal 派发器 billData Long id 归一，同 owner M2.5/M2.6）
- Successor Required: `yes`（M2.4 回收 ast 3 条；M2.2 回收 inv 5 条；M2.5/M2.6 各回收 1 条 + asId 桥 ×5）

### 12 域被引用面编译破坏

- Classification: `watch-only residual`
- Why Not Blocking Closure: 晚域代码引用本域 String API 的编译破坏属预期中间态（登记册被引用清单），本域 verify 闭包（5 模块自身链，no-am）不含外域模块
- Successor Required: `yes`（各域 plan 的 C 定位面；M4.1 兜底）

### 6 条 A1 延后列 + prj stub 2 列（Decision A 翻转）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册 orm-column-deferral 条目——prj（位次 12）未迁移，A1 延后列保持 long；prj stub 2 列经 Explore 实证随 Decision A 翻转（codegen 不校验 join 类型 + 运行时合并零影响，「stub 元数据先行、权威源 M2.7 跟进」）
- Successor Required: `yes`（M2.7 翻转 prj orm 时同批翻转 fin 6 列 + hr 2 列并退役 orm-deferral-001..008）

### ErpFinBudgetControlLog.projectId（结束审计 MINOR-2 观察项）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 第 7 个 project 引用 BIGINT 列，无 prj 关系边（非 join 面）随主变更面翻转合法；写入值源自延后 Long budgetLine.projectId，Java 侧转换成立，构建 + 497 测试全绿实证
- Successor Required: `yes`（M2.7 同批翻转 6 延后列时核对该列写入点无双重转换）

### 平台 IoC 回归 delta（已落盘 + 先例缺陷修正）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 `nopSequenceGenerator` bean-init-self-wait 为已登记平台 Bug，先例修复 = test-scope VFS delta；本域 delta 已修正先例缺陷（补 `x:extends="super"`，audit 拦截器恢复实证）
- Successor Required: `yes`（平台修复后统一移除，M4.1 复核；五域先例文件缺 x:extends="super" 待回收——bug 登记 `docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`）

## Closure

Status Note: completed（2026-08-22。四 Phase 全部执行完毕；独立结束审计 `passes closure audit`（0 BLOCKER / 0 MAJOR / 2 MINOR，均已处置：MINOR-1 = Current Baseline/Phase 1 记录的延后列行号 `:517/:958/:1361/:1417/:1859/:2063` 为起草时点行号，落源后文件行号漂移为 `:495/:932/:1343/:1398/:1836/:2046`（实体身份与列集实质一致，审计 live 复核确认）；MINOR-2 = ErpFinBudgetControlLog.projectId 观察项登记 Deferred But Adjudicated（M2.7 核对写入点无双重转换））。

Closure Audit Evidence:

- 独立结束审计（新会话子代理 ses_fda3f6a9cffeW6gFMFIqpmbbfH，2026-08-22）：`passes closure audit` — 0 BLOCKER / 0 MAJOR / 2 MINOR。九项活仓核验全过：① orm 208 行 BIGINT/string 精确（252 BIGINT = 208 string + 37 delVersion + 1 reconciledBy + 6 deferred，全 48 id 列 BIGINT 保留，git diff 208/208 零非 stdDataType，scan finance 段 ok=208/DEFERRED=6/NEEDS FIX=0）；② 5 模块 install BUILD SUCCESS（审计者独立复跑）；③ service 测试 497/497 绿（审计者独立复跑，TestErpOrgIsolation 2/2 兑付确认）；④ grep 门控（.longValue() 恰 5 处 asId 桥本体带注释/Map<Long/Set<Long=0/:Long fin-web 清零/period-close-wizard :String ×6 + dashboard ×1）；⑤ 登记册（bridge-test-118 retired + bridge-main-061..070 active + JSON5 可解析）；⑥ 快照卫生（RECORDING/forceSaveOutput 零残留 + _cases 恰 4108 + String id 形态实证 `"id": "9"`）；⑦ 文档一致（roadmap done + 最后更新 + 日志 + owner doc 注记 + bug 登记 + delta x:extends="super"）；⑧ view.xml 零变更；⑨ 计划内部一致（四 Phase [x]×4 + 草案审查/双批准/延后登记与实仓一致）。
- 最终验证状态（全绿，执行者 + 审计者双重复核）：`mvn clean install -pl module-finance/erp-fin-{codegen,dao,meta,service,api} -DskipTests` BUILD SUCCESS ×2 + `mvn test -pl module-finance/erp-fin-service` 497/497 绿 ×3（含 CHECKING 稳定性双跑）+ `node tools/check-bigint-id-types.mjs` finance 段 NEEDS FIX=0/DEFERRED(registry)=6。

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 4 登记记录——web/app 重生成与 6+2 延后列翻转归 M2.7 补做/M4.1 兜底；A2 桥接 10 条 + asId ×5 归 M2.2/M2.4/M2.5/M2.6；12 域被引用面归各域 plan/M4.1；五域先例 delta 缺陷归各域 successor/M4.1。）
