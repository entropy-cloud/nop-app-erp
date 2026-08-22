# 2026-08-22-1302-1-bigint-id-m27-projects-migration 主键/外键 string 化 M2.7：projects 域迁移（冻结序位次 12）+ fin/hr 延后列同批兑付 + fin web/app 补做

> Plan Status: completed（2026-08-22：五 Phase 全部执行完毕、验证全绿；iteration 1-3 独立草案审查收敛 + 保护区域双独立子 agent 批准（治理 ses_fd8137a73ffe8Z7jrAW40G9N5c + 技术 ses_fd80e9fe6ffeyCuhTqqfBDUtyq），见 Draft Review Record）
> Mission: id-string-migration
> Work Item: M2.7（projects，冻结序位次 12）
> Last Reviewed: 2026-08-22
> Source: `docs/backlog/id-string-migration-roadmap.md` M2/M3 冻结总序表位次 12（M2.7）
> Related: `docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`（M2.1 web/app 延后 + 6 延后列义务移交本计划）、`docs/plans/2026-08-22-0731-1-bigint-id-m33-hr-migration.md`（hr 2 延后列登记来源）、`docs/plans/2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry.md`（M0.2 登记册，消费来源）、`docs/plans/2026-08-22-0731-3-bigint-id-m32-maintenance-migration.md`（最近域迁移范式）
> Audit: required（保护区域 `model/*.orm.xml` **三文件**（prj + fin + hr 同批）：独立 plan-audit + 双独立子 agent 批准，批准记录落盘本文件）

## Current Baseline

- **prj 域规模（2026-08-22 实况 scan）**：`module-projects/model/app-erp-projects.orm.xml` 需改列 **76 = 自有 68（PK 17 + BIGINT FK 51，含 orgId FK 自有 7）+ notGenCode md stub 8（5 stub 实体：`ErpMdOrganization`(id,parentId)/`ErpMdCurrency`(id)/`ErpMdEmployee`(id,orgId)/`ErpMdPartner`(id)/`ErpMdSubject`(id,parentId)——md 权威源自 M1.1 已 String，翻转 = 与权威源对齐）**。**不改列**：`delVersion` 等非 PK/FK BIGINT 列（规则 4 保持 long）；prj 自身 A1 延后 = 0（scan DEFERRED = 0）。
- **同批兑付列（登记册 B 义务，本域 orm 翻转时同批翻转早域延后列并退役条目，显式拥有早域链修复责任）**：fin 6 列 = `ErpFinVoucherLine.projectId`/`ErpFinGlBalance.projectId`/`ErpFinExpenseClaimLine.projectId`/`ErpFinEmployeeAdvance.projectId`/`ErpFinBudgetLine.projectId`/`ErpFinGlMappingRule.projectId`（orm-deferral-001..006，登记册 evidence 行号为 M2.1 起草时点 :517/:958/:1361/:1417/:1859/:2063，M2.1 落源后行号漂移为 :495/:932/:1343/:1398/:1836/:2046——执行时以实体+列名定位复核）；hr 2 列 = `ErpHrTimesheetLine.projectId`/`taskId`（orm-deferral-007/008）。共 **orm-deferral-001..008 全部 8 条 → retired**（登记册仅存的 8 条 active orm-column-deferral，退役后该 kind 清零）。
- **模块链与编译依赖（pom 实测）**：7 模块 = `module-projects/erp-prj-{codegen,dao,meta,service,web,app,api}`（全链构建，无延后）。`erp-prj-service` main compile 依赖 **fin-service（M2.1 起 String）+ ast-service（M2.4 起 String）** + common-service；test-scope 依赖 md-service + notify-service（均 String）。`erp-prj-dao` compile 依赖 md-dao（String——prj orm `refEntityName` md ×27，M1.1 登记的 `_gen` 关系胶水中间态**本域迁移即自愈**；prj 域内关系 ×34 不受影响）。prj-web test-scope 依赖 ast-dao（String）+ md-service（String）；prj-app 域级 web 依赖仅 md-web（M1.1 起 String，可建）。
- **M0.2 登记册 prj 视角（§6.12 + json5 实测对账，起草已核）**：A（前向义务）= 0（本域不引用任何晚域——service 无 A2 桥接、测试无 A3 桥接）；**B 退役义务 = orm 列延后 8 条（见上）**；C1 后向 main 4 条（backward-174 → ast 1 文件 `ErpPrjProjectSettlementProcessor`；backward-175 → fin 3 文件 `cost/ExpenseCostAggregator`/`posting/ProjectPostingExecutor`/`processor/ErpPrjProjectSettlementProcessor`；backward-176 → md 1 文件 `posting/TimesheetPostingDispatcher`；backward-177 → notify 2 符号 1 文件 `posting/TimesheetPostingDispatcher`）；C2 后向 test 4 条（backward-235 → ast 1 文件 `TestErpPrjProjectSettlement`；backward-236 → fin 10 文件；backward-237 → md 10 文件；backward-238 → notify 1 文件 `posting/TestTimesheetPostingFailureAlert`）。
- **被引用面（登记册 §6.12 + dao 层实测补充）**：purchase main 1 文件 + purchase test 1 文件（backward-182/244）——位次 15 未迁移域引用 prj Long API 的编译破坏为**已登记中间态**（successor M2.5 + M4.1 兜底），不在本计划 no-am reactor 内。**dao 层被引用面（登记册 backward-pointer 未列、M0 裁决 §10 对称 `_gen` 耦合实测补充）**：`module-sales/erp-sal-dao`（pom :41）与 `module-purchase/erp-pur-dao`（pom :42）compile 依赖 prj-dao，其 `_gen` 关系胶水（`_ErpSalOrderLine.getProject()/setProject(ErpPrjProject)`、pur `_ErpPurOrderLine`/`_ErpPurRequisitionLine` 同型）在 prj 翻转后破坏、唯 sal/pur 迁移时自愈（successor M2.6/M2.5）；另 pur/sal orm 内 `ErpPrjProject` notGenCode stub（id 仍 long，各 1 列）翻转后为残留 NEEDS FIX（M2.2 对 drp/mfg 内 inv stub 同型登记——successor M2.5/M2.6）。
- **fin 延后列手写残留（M2.1 登记，本计划修复面）**：`fin-service` main 2 文件 + `fin-dao` 手写 DTO 1 文件——`posting/VoucherFact.projectId`、`budget/BudgetVoucherGenerator`（projectId 保 Long 延后注释）+ **`erp-fin-dao/src/main/java/app/erp/fin/dao/dto/GlMappingDimensions.java:23`（`Long projectId`）**（`ErpFinGlMappingResolver` 本体无 Long 残留，消费 GlMappingDimensions）；另有 `BudgetLineBizModel.getBudgetVsActual` 的 BudgetVsActualRow DTO + 测试 `TestErpFinGlMappingResolver`（fin Phase 4 grep 登记的合法残留清单）。**观察项（fin 结束审计 MINOR-2 移交）**：`ErpFinBudgetControlLog.projectId` 已于 M2.1 翻转（第 7 个 project 引用列，无 prj 关系边、Java 侧转换成立），6 延后列同批翻转时须核对该列写入点**无双重转换**。
- **hr 延后列兑付面（M3.3 登记）**：hr-service 手写 main 代码对 `ErpHrTimesheetLine.projectId/taskId` **零直接引用**（`rg projectId|taskId` 于 hr-service main/test 实测零命中，仅 `_gen` 实体与 api beans 持有）→ hr 侧预期纯重生成 + 链重建 + 测试复跑，无手写代码修复面（执行时以编译器清单复核为准）。登记册 evidence 行号 :651/:652 已漂移至 live :636/:637（M3.3 落源所致）——执行时以实体+列名定位（fin 6 列同规则）。
- **fin web/app 补做（M2.1 冻结裁决显式移交本计划载体）**：fin-app main 依赖 fin-web + **prj-web** + ast-web + md-web（pom :31/:36/:41/:46）；ast-web/md-web 已 String，**prj-web 由本计划 Phase 2 install 后 fin 全链可建**。fin-web 手写 page.yaml 7 处 `:Long` 已于 M2.1 源码级 Fix（period-close-wizard ×6 + dashboard ×1），本计划补做 = fin-web/fin-app 重生成重建 + `:Long` 清零复核 + YAML 良构；fin-web 2 测试文件（`ErpFinWebCodeGen` + `ErpFinWebPagesTest`，后者 `@Tag("full-app")` 治理排除，successor M4.1）。adaptor 行为级页面验证 successor M4.1（app-erp-all `ErpAllWebPagesTest`）。
- **测试资产（实测）**：prj-service **26 个测试类 + 2 个支撑件**（`PrjFrozenClockExtension`/`ErpPrjProjectStateMachineDelta`，合计 28 个 .java 编译文件）；`_cases` 快照 **761 文件**；prj-web 2 测试文件（`ErpPrjWebCodeGen` + `ErpPrjWebPagesTest` 治理排除，successor M4.1，参与 test-compile）。**时序注意**：prj 测试中存在对 fin 6 延后列实体的 seed/断言（≥5 文件，如 `TestErpPrjTimesheetCost.java:223` `debit.getProjectId()` on `ErpFinVoucherLine`）——Phase 3 时点 fin jar 仍为 M2.1 形态（6 列 Long，本地仓陈旧 jar），Phase 3 绿灯为**对陈旧 fin jar 的临时绿灯**，Phase 4 fin 翻转后必须复跑清零（见 Phase 4 prj 复跑项）。
- **手写 page.yaml raw-GraphQL `:Long` 变量（本计划范围内 9 处，迁移即失效的实时缺陷）**：`erp/prj/pages/dashboard/main.page.yaml:65`（`$projectId:Long` → `ErpPrjDashboard__getProjectGrossMargin`）+ `erp/prj/pages/ErpPrjTask/kanban.page.yaml` ×8（`$p:Long` 查询变量 :43/:96/:176/:212 → `filter_projectId` ×4 + `$id:Long` mutation 变量 :77/:130/:150/:247 → `startTask/completeTask/blockTask/unblockTask`）——prj 翻转 String 后静态类型不匹配 → adaptor 静默降级、任务看板全流程失效（M3.6 结束审计 MAJOR-1 同型）——**本计划就地 Fix + prj-web 重建验证**。
- **owner doc 已知 Long 陈述（Phase 5 复核对象，起草实测）**：`docs/design/projects/` grep 零 Long id 陈述（预期零文档变更，Phase 5 复核记录结论）。
- **已知风险（先例登记）**：① 平台 IoC 回归 self-wait——prj 若复现按 fin 修正版先例落 test-scope VFS delta（根元素 `x:extends="super"`）+ DeltaOverride delta-layer 补 default 层集；第二环（`nopOrmSessionFactory` 经 nopDataAuthChecker）hr/aps/fin/ast 已落 delta，fin/hr 复跑时若新域侧复现按 hr 先例处理并补记 bug 域清单；② no-am 测试 classpath VFS 模块集变化（回退 = seq-proof-yaml 模块禁用模式）；③ 陈旧 jar 二进制不兼容（本地仓 sal/pur Long jar——prj compile 依赖全 String 无此面；pur 引用 prj 的破坏为登记中间态）。
- **回写机制（M0.1 裁定 Decision A，三步）与顺序细节**：**先退役登记册 orm-deferral-001..008（status → retired + 兑付 note）再 dry-run**——工具豁免随 active 条目解除（`verify-id-fix-copy-diff.mjs` 头注设计意图：「M2.7 退役 8 条目后同批翻转 prj + fin 6 列 + hr 2 列」），dry-run/新鲜度门控方将 fin 6 + hr 2 列计入可落源差异。① dry-run 时点刷新；② `verify-id-fix-copy-diff.mjs` 逐模块门控（module-projects 预期 76 / module-finance 预期 6 / module-hr 预期 2，均零非 stdDataType 行）；③ 三单文件落源 + `git diff` 逐行审核。禁止盲 cp、禁止 apply 模式。
- **剩余差距**：prj orm 76 列 + fin orm 6 列 + hr orm 2 列全 `stdDataType="long"` 待改；prj 手写代码/测试/快照全部 Long 形态；fin 手写 Long 残留 3 main + 2 测试文件 + DTO；fin web/app 本地仓 jar 维持 Long 陈旧形态；冻结序位次 12。

## Goals

- prj 76 列 + fin 6 列 + hr 2 列 `stdDataType` long→string 同批落源（三 orm 文件、stdDataType-only，`stdSqlType` 保持 BIGINT，DDL 零变化）；登记册 orm-deferral-001..008 → retired（该 kind 清零）。
- prj 增量重生成（no-am 7 模块链）+ 编译器驱动修复 prj 全部手写代码（A2 = 0 无前向桥接；C1 后向 4 条兑付）。
- fin/hr 延后列兑付链重建：fin 手写 Long 残留修复（VoucherFact/GlMappingDimensions（及其消费链 GlMappingResolver）/BudgetVoucherGenerator/BudgetVsActualRow/TestErpFinGlMappingResolver）+ `ErpFinBudgetControlLog.projectId` 写入点双重转换核对 + fin 7 模块链重生成重建（**含 web/app 补做**）+ hr 7 模块链重建。
- prj 快照每域重录（RECORDING→CHECKING；761 文件基线）+ prj 域级测试全绿。
- 早域复跑基线维持：fin-service **497/497** + hr-service **237/237**（fin-web/hr-web 0 tests 治理排除）+ fin page.yaml 7 处 M2.1 Fix 的重建级验证。
- prj 语义陷阱 grep 门控清零 + page.yaml `:Long` 9 处就地 Fix（prj-web 重建验证）+ 手写 view 零被动变更核验。
- 路线图 M2.7 → `done` + 日志；fin 延后义务移交闭环登记（M2.1/M3.3 Deferred 条目兑付指针）。

## Non-Goals

- 不迁移 quality/manufacturing/purchase/sales 等后续位次域（归 M2.3/M3.1/M2.5/M2.6 批次）。
- 不修 purchase/sales 对 prj 的引用与 dao 耦合破坏（pur main 1 + test 1 文件 + sal-dao/pur-dao `_gen` prj 关系胶水 + pur/sal orm prj stub 残留——已登记中间态，successor M2.5/M2.6；本计划 Phase 5 登记确认）。
- 不改 `delVersion` 等非 PK/FK BIGINT 列（规则 4）。
- 不动 fin/hr 6+2 列以外的任何 fin/hr orm 列（fin 其余 208 列（187 自有 + 21 stub）+ hr 136 列（130 自有 + 6 stub）已 String，本计划只翻登记册延后列）。
- 不跑全量构建/全量测试/E2E/compliance checker（归 M4.1）；不手改任何生成件。
- 不修四域（md/notify/b2b/contract）IoC delta `x:extends="super"` 回收（不在触碰面，归各域 plan 触碰时或 M4.1——bug `docs/bugs/2026-08-22-ioc-delta-missing-extends-super.md`）。
- web 页面测试治理排除不修（`ErpPrjWebPagesTest`/`ErpFinWebPagesTest`，successor M4.1）；fin adaptor 行为级页面验证 successor M4.1。

## Task Route

- Type: `implementation-only change`（保护区域 ORM 三文件变更 + 早域兑付编辑面）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M2/M3 表位次 12 + 横切 §5 设计证据（`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B + `docs/design/domain-design-guidelines.md` §16A.4 + M0.1 审计结论 + M0 裁决 §10）；prj 业务语义 owner doc = `docs/design/projects/`（Phase 5 复核，预期零变更）
- Skill Selection Basis: 路线图 §M1-M3「预期技能」指定域迁移 plan 加载 `nop-backend-dev` + `nop-testing`；ORM 变更机制由 M0.1 审计与平台文档背书。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（无 DB DDL 变更；DB 列保持 BIGINT）。no-am 构建硬前置 = 最后全绿基线 commit 全量 install + 位次 3-11 链 install（aps/b2b/contract/fin/ast/cs/hr/inv/mnt）。回滚策略：revert 三 orm 文件 + 登记册退役 note + `mvn clean install -pl <prj/fin/hr 各自 7 模块显式列表> -Dmaven.test.skip=true` 重生成回 Long 形态（**Phase 4 完成后回滚需先 revert fin 手写修复与 prj 代码**——fin String 残留形态对 Long prj jar 不可编译）。

## Execution Plan

### Phase 1 - 消费登记册 + 三 orm 同批回写（保护区域，双批准前置）

Status: completed
Targets: `module-projects/model/app-erp-projects.orm.xml`、`module-finance/model/app-erp-finance.orm.xml`（6 列）、`module-hr/model/app-erp-hr.orm.xml`（2 列）、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix`
- Prereqs: M2.4 ✅ + M2.1 ✅ + M1.1 ✅ + M1.2 ✅（精确前置满足）；本计划已通过独立 plan-audit + 双独立子 agent 批准（保护区域 `auto + dual-agent-approval`，批准记录落盘 Draft Review Record）

- [x] Proof: 消费 M0.2 登记册——读取 `tools/id-migration-registry.json5` + 登记册文档 §6.12 prj 节，逐条核对：(i) A = 0（无前向桥接）；(ii) **B = orm-deferral-001..008（fin 6 + hr 2 列，同批翻转 + 条目退役 + 早域链修复责任）**；(iii) C1 = backward-174/175/176/177 与 C2 = backward-235/236/237/238 作为 Phase 2/3 定位面；(iv) 按 b2b/assets A3' 先例做 FQN 盲区复扫（`rg 'app\.erp\.(pur|sal|qa|crm|drp|log|mfg)\.' module-projects/erp-prj-service/src/test module-projects/erp-prj-web/src/test` 排除 import 行 + test beans.xml ioc:type FQN——覆盖本域执行时点全部未迁移晚域）；(v) M2.1/M3.3 Deferred 移交项核对（fin 6 列实体+列名定位 + BudgetControlLog 写入点 + hr 2 列）。矛盾则按路线图规则 6 停止回报。
  - Skill: none
  - 结果：scan 实测 prj 段 76 NEEDS FIX（68 自有 + 8 notGenCode md stub）/ fin 段 6 DEFERRED / hr 段 2 DEFERRED；backward-174（ast 1 文件 `ErpPrjProjectSettlementProcessor`）/175（fin 3 文件）/176（md 1）/177（notify 1 符号）/235-238（test 1+10+10+1 文件）evidence 与 live 一致；FQN 盲区复扫零命中（import 行排除后）+ ioc:type 零命中 → A = 0 确认；M2.1/M3.3 移交项与登记册 8 条逐条对齐无矛盾。
- [x] Proof: 双独立子 agent 批准记录落盘（批准人指针 + 结论 + 时间；批准范围 = prj 76 列 + fin 6 列 + hr 2 列 stdDataType-only 同批变更），未获批不得进入回写。
  - Skill: none
  - 结果：Draft Review Record「双独立子 agent 批准」节在案（治理 ses_fd8137a73ffe8Z7jrAW40G9N5c 条件批准 + 技术 ses_fd80e9fe6ffeyCuhTqqfBDUtyq 无条件批准，均 2026-08-22）。
- [x] Fix: 登记册先行退役——`tools/id-migration-registry.json5` 中 orm-deferral-001..008 status → `retired` + 兑付 note（指向本计划；登记册转为手工维护权威，不重跑生成器）；fail-closed 解析验证（`node tools/check-bigint-id-types.mjs scan` 正常消费，prj 段 76 NEEDS FIX / fin 段 6 / hr 段 2 进入可落源口径）。
  - Skill: none
  - 结果：8 条 status → retired + note 落盘；scan fail-closed 正常消费（DEFERRED 0 → 8 列计入待改口径）。
- [x] Fix: 回写三 orm（M0.1 裁定三步机制）——① `node tools/check-bigint-id-types.mjs dry-run` 时点刷新；② `node tools/verify-id-fix-copy-diff.mjs module-projects`（预期变更行 76）+ `module-finance`（预期 6）+ `module-hr`（预期 2）新鲜度门控（均零非 stdDataType 行）；③ 门控通过后三单文件分别落源。禁止盲 cp 静态副本、禁止 apply 模式。
  - Skill: none
  - 结果：dry-run 752 列（744+8）XML 校验 10/10 幂等；门控 module-projects 76 行 / module-finance 6 行 / module-hr 2 行均「零非法差异行、门控通过」；三单文件 cp 落源。
- [x] Proof: `git diff` 三文件逐行核对——仅既定列集 `stdDataType="long"→"string"`（76+6+2），`stdSqlType` 零变化、`delVersion`/标签结构零变化、fin/hr 其余列零变化；scan 重扫三段零 `NEEDS FIX`/零 `DEFERRED`（orm-column-deferral kind 清零后口径）。
  - Skill: none
  - 结果：git diff --stat 84 insertions/84 deletions；changed 行中非 stdDataType 行 = 0；scan 重扫三段零 NEEDS FIX + 全局 DEFERRED = 0 + 校验告警 0。

Exit Criteria:

- [x] 登记册消费核对在案（含 FQN 盲区复扫结论）；双批准记录在案；8 条先行退役 + fail-closed 验证在案；新鲜度门控 + git diff + 工具重扫三重证明变更面精确 = 84 列（76+6+2）stdDataType

### Phase 2 - prj 增量重生成 + 主代码编译修复

Status: completed
Targets: `module-projects/erp-prj-{dao,service}/src/main/java/**`（手写 IBiz/BizModel/Processor/posting/cost）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1

- [x] Fix: `mvn clean install -pl module-projects/erp-prj-codegen,module-projects/erp-prj-dao,module-projects/erp-prj-meta,module-projects/erp-prj-service,module-projects/erp-prj-web,module-projects/erp-prj-app,module-projects/erp-prj-api -Dmaven.test.skip=true`（D3 口径：7 模块显式列表、不带 `-am`）触发增量重生成。预期：prj-dao `_gen` md 关系胶水（27 处）自 M1.1 登记中间态自愈。**install（非 package）落本地仓——prj-web String jar 为 Phase 4 fin-app 重建硬前置**。
  - Skill: `nop-backend-dev`
- [x] Fix: 编译器驱动修复主代码——逐条修复 prj dao + service 手写代码类型错误（定位面：C1 backward-174（ast `ErpPrjProjectSettlementProcessor`）+ backward-175（fin 3 文件 posting/cost 族 `PostingEvent`/`AcctDocContext` String id 值流转——M2.4 盲区先例）+ backward-176/177（md/notify，签名不变预期零破坏核验）+ 全域 IBiz/值对象 Long 签名 + `.getId()` 下游），直到 7 模块链 `-Dmaven.test.skip=true` 构建全绿。修复清单落盘本计划。**fin 6 延后列跨域影响时序声明**：Phase 2/3 时点 prj 编译/测试面向本地仓 M2.1 形态 fin jar（6 列 Long）——编译器对该面**不可见**（陈旧 jar 不报错），prj 侧 voucherLine/glBalance 等 6 列读写点的 String 化适配延至 Phase 4 fin 翻转后复跑闭环；本阶段禁止为该面引入持久 `toLong` 桥（若测试编译必需临时桥，逐处登记并在 Phase 4 移除）。
  - Skill: `nop-backend-dev`
  - 修复清单（编译器驱动，三波）：**dao 层 6 IBiz 文件**（IErpPrjProjectBiz/TaskBiz/PnlBiz/TimesheetBiz/ProjectSettlementBiz/CostCollectionBiz——@Name id 参数 Long→String，先于 BizModel 按 skill 强制顺序）；**service 主代码 30 文件**：cost 族 5（ProjectCostAggregator/MaterialCostAggregator/ExpenseCostAggregator/BudgetChecker/CostRateResolver——id 参数/局部变量/List<Long>→String + PostingEvent billData String 值流转）、pnl 1（ProjectPnlCalculator——refreshPnl 族 10 签名 + Set<Long> 2 处）、posting 4（TimesheetPostingDispatcher——loadProject/Type/ActivityType/resolveAcctSchemaId/resolveExchangeRate/findCurrencyById/findExchangeRate/resolveSubjectCode 签名 + voucherId；ProjectPostingExecutor——postEvent 返回 String（fin IBiz String）；ProjectSettlementPostingDispatcher——postRetentionReturn/resolveAcctSchemaId/voucherId）、processor 12（SettlementProcessor 8 签名 + CreateSettlement/Reverse/ReturnRetention/PnlRefresh/TimesheetApprove/TimesheetCancel/TimesheetSubmit/CloseProject/HoldProject/ResumeProject/RefreshActualCost/RefreshExpenseCost/AggregateMaterialCost + SubmitForApproval/Approve/Reject/Cancel 4 个 per-mutation 移除 `Long.valueOf(id)` 桥）、entity 7（ProjectBizModel/TaskBizModel/TimesheetBizModel/ProjectSettlementBizModel/ProjectPnlBizModel/CostCollectionBizModel——@BizMutation/@BizQuery id 参数 + Set/Queue<Long>；TaskBizModel findBoardData）、report 1（ErpPrjReportBizModel——asLong→asString + Map<String,Long>→<String,String> + Aggregator）、dashboard 1（ErpPrjDashboardBizModel——collectIds/Set/Map<Long,BigDecimal>→String；count 比较器保留 Long 非 id）、job 1（ErpPrjProjectPnlCalcHelper）。md/notify 侧（backward-176/177 `TimesheetPostingDispatcher`）签名不变零破坏核验通过（IErpMdCurrencyBiz/IErpSysNotificationBiz 均既有 String 签名，仅 prj 侧值类型适配）。
- [x] Fix: **eq/filter 语义值桥专项清扫（fin 6 延后列，编译器不可见面）**——`rg` 扫描 prj 手写 main/test 中对 fin 延后列实体的 `eq(`/`in(`/filter 查询值（已知命中：`cost/ExpenseCostAggregator.findLinesForProject` 对 `ErpFinExpenseClaimLine` 的 `eq("projectId", projectId)` :190-196——prj 翻转后 String 过滤 Long 列静默空匹配，contract 037/038 + M2.1/M3.2 语义值桥先例），逐处判定并登记：Phase 2-3 窗口内（fin 仍 Long）该查询保持 Long 值传递或显式 `toLong` 桥 + 登记；Phase 4 fin 翻转后统一 String 直传复核（终态两侧全 String）。清单落盘本计划。
  - Skill: `nop-backend-dev`
  - 清单结论：prj main 对 6 个 fin 延后列实体（VoucherLine/GlBalance/ExpenseClaimLine/EmployeeAdvance/BudgetLine/GlMappingRule）+ hr TimesheetLine 的直接引用面 = **仅 `ExpenseCostAggregator`（ErpFinExpenseClaimLine）1 处**——已落显式 `toLong` 桥 + `TEMP-BRIDGE(M2.7)` 注释（:192-197，登记 orm-deferral-003），Phase 4 复核清零；prj main 其余 `eq("projectId",...)` 18 处全部面向 prj 自有实体（两侧全 String 直传，无桥）；Provider 侧 `readLong(event, BILL_DATA_PROJECT_ID)` 2 处（ProjectCostCollectionProvider:52/ProjectSettlementAcctDocProvider:67）为 billData(Object)→Long 解析——String 输入经 `Long.valueOf(s)` 分支自然兼容、向陈旧 jar VoucherFact.setProjectId(Long) 传值正确，Phase 4 fin 翻转时由编译器驱动改 String（VoucherFact 修复面）。
- [x] Fix: 自身链破坏处置（D4 carve-out）——no-am 口径下预期零外域破坏（reactor 仅 7 模块）；pur/sal 对 prj 的引用与 dao 耦合破坏（Current Baseline 登记清单）为已登记中间态（successor M2.5/M2.6），Phase 5 登记；未登记破坏按路线图规则 6 停止回报。
  - Skill: `nop-backend-dev`
  - 结果：7 模块 reactor BUILD SUCCESS——no-am 口径零外域破坏，无未登记破坏。

Exit Criteria:

- [x] prj 7 模块链（显式列表、no-am、`-Dmaven.test.skip=true`）构建全绿（main 代码）+ install 落仓（prj-web String jar 在位）；主代码修复清单在案

### Phase 3 - prj 测试修复 + 快照重录 + 域级测试

Status: completed
Targets: `module-projects/**/src/test/**`、`module-projects/erp-prj-service/_cases/**`
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2

- [x] Fix: 测试代码修复——26 个测试类（+2 支撑件）的 Long 用法（字面量断言、helper 签名、seed `orm_propValueByName("id", id)` 形态——md/notify 先例），逐文件修复至测试编译通过；C2 后向 test 适配（backward-235 ast 1 + backward-236 fin 10 + backward-237 md 10 + backward-238 notify 1 文件）；**fin 6 延后列实体 seed/断言（≥5 文件）按 Phase 2 时序声明处理**（对陈旧 fin jar 的 Long 形态临时保留 + 逐处登记，Phase 4 复跑清零）；prj-web test-scope ast-dao/md-service（均 String）编译对象核验（`ErpPrjWebPagesTest` 治理排除但参与 test-compile）。
  - Skill: `nop-testing`
  - 修复清单（编译器驱动三波 + 断言类型收尾）：20 测试文件 Long→String——CostRateTier/TaskDependency/ProjectPnl/ProjectSettlementRetention/BudgetAndCollection/ExpenseAggregation/MaterialAggregation/ProjectSettlement/PnlCalcJob/ProjectPrecheck/TimesheetCost/TimesheetMulticurrencyPosting + validator/TestTaskDependencyValidator（Map<Long>/字面量 1L→"1"/Arrays.asList(2L..)→"2"..）+ dashboard×3（TestErpPrjDashboard seedProject/seedBudget/seedCost + 断言 `assertEquals(121L,...)`→`"121"` + orm_propValue(1,id)→orm_propValueByName("id",id)；GrossMargin seedPnl 字面量；DashboardGrossMargin projectCount 保留 Long 非 id）+ report/TestErpPrjReportRendering（ORG_ID/CURRENCY_ID 常量 String + seed 字面量）。**fin 6 延后列临时面登记（3 文件 4 处，Phase 4 清零）**：`ExpenseCostAggregator`（main，eq toLong 桥——Phase 2 已登记）+ test `TestErpPrjExpenseAggregation`（`line.setProjectId(Long.valueOf(projectId))` TEMP-BRIDGE，orm-deferral-003）+ test `TestErpPrjProjectSettlementRetention`（`String.valueOf(l.getProjectId())` 形态中立断言 ×2，orm-deferral-001——两窗口均成立，Phase 4 复核可简化）。C2 核验：backward-236 fin 10 文件全部在本批修复（断言式经 IBiz/实体 String 签名）；backward-237 md 10 文件同批；backward-235 ast（TestErpPrjProjectSettlement）+ backward-238 notify（TestTimesheetPostingFailureAlert）签名兼容零额外修复（ast/notify IBiz 既有 String）；prj-web test-compile BUILD SUCCESS（ast-dao/md-service String jar 编译通过）。
- [x] Fix: 快照每域重录（用户裁决固定步骤）——`RECORDING` 模式运行 prj service 测试 → 逐案审核 `_cases/` 新形态（761 文件基线；id 以 String 形态落盘；非确定性单元格按 aps/contract 先例 `*` 通配修正；「断言式 + 空 autotest.yaml」范式测试不录——cs 回退先例）→ 注解还原（grep 零 RECORDING/forceSaveOutput 残留）→ 切回 `CHECKING` 复跑确认全绿。重录足迹（内容 diff vs 新增落盘分列）记录本计划。
  - Skill: `nop-testing`
  - **执行证据**：快照式 1 类（`TestErpPrjProjectCrudSmoke`——115 个 autotest.yaml 中唯一持 response 快照的类；其余断言式 + 空 autotest.yaml 类不录，cs 先例）`snapshotTest = RECORDING` 运行（5 方法 snapshot-finished 异常 = 录制完成）→ 注解还原 grep 零残留 → CHECKING 复跑全绿。**重录足迹 = 9 内容 diff + 1 新增落盘 = 10 文件（761 → 762）**：内容 diff = `"projectId": 1` → `"projectId": "1"`（String 形态实证）+ xmeta 重生成字段序刷新；新增 = testCreateHead/input/tables/erp_prj_project.csv 1 文件。全量快照 grep 零 `"id": [0-9]` 数字形态残留。
- [x] Proof: `mvn test -pl module-projects/erp-prj-service,module-projects/erp-prj-web`（D3 口径：不带 `-am`）全绿——service 26 测试类 + web BUILD SUCCESS（`ErpPrjWebPagesTest` 治理排除，0 tests 预期，successor M4.1）。**本项绿灯为对陈旧 fin jar 的临时绿灯（Phase 2 时序声明），终态由 Phase 4 prj 复跑项确立**。若复现平台 IoC 回归，按 fin 修正版先例修复（test-scope VFS delta 带根元素 `x:extends="super"` + DeltaOverride delta-layer 补 default 层集）并登记。
  - Skill: `nop-testing`
  - 结果：**172/172 全绿**（service 26 类）+ prj-web BUILD SUCCESS（0 tests 治理排除）。**平台 IoC 回归未复现**（prj-service no-am classpath 下 self-wait 未出现，零 delta 落位需要）。

Exit Criteria:

- [x] prj 域级测试全绿（service 26 类 + web 治理排除偏差登记；**对陈旧 fin jar 的临时绿灯 + 6 延后列相关临时桥/断言登记在案**）；快照重录完成且 `CHECKING` 复跑通过；重录清单在案（fin 6 延后列相关快照单元 Phase 4 复跑后可能二次刷新，属预期登记项）
  - 临时面登记汇总：main 1 处（ExpenseCostAggregator eq toLong 桥）+ test 2 文件 3 处（ExpenseAggregation setProjectId 桥 / SettlementRetention String.valueOf 断言 ×2）；涉及 fin 延后列的快照单元 = TestErpPrjProjectCrudSmoke 已重录 String 形态（该类不触 fin 延后列——`"projectId"` 均为 prj 自有列；fin 延后列快照面经 TestErpPrjTimesheetCost/Retention 断言式覆盖，无 response 快照），Phase 4 复跑复核。

### Phase 4 - fin/hr 延后列兑付链重建 + fin web/app 补做 + 早域复跑

Status: completed
Targets: `module-finance/erp-fin-{codegen,dao,meta,service,web,app,api}/**`（含 fin-dao 手写 DTO `GlMappingDimensions`）、`module-hr/erp-hr-{codegen,dao,meta,service,web,app,api}/**`、`module-finance/erp-fin-service/src/{main,test}/**`（延后列残留修复）、`module-projects/**`（fin 翻转后 prj 复跑清零）
Skill: `nop-backend-dev`

- Item Types: `Fix | Proof`
- Prereqs: Phase 3（传递含 Phase 2——prj-web String jar install + prj 测试临时桥/断言登记清单）

- [x] Fix: fin 延后列手写残留修复——6 列翻转后 `_gen` 实体 getter String 化，编译器驱动修复：`VoucherFact.projectId`、`erp-fin-dao` 手写 DTO `GlMappingDimensions.java:23`（`Long projectId`）及其消费链（`ErpFinGlMappingResolver` cacheKey/loadFromDb——resolver 本体无 Long 残留）、`BudgetVoucherGenerator`（projectId 延后注释与 Long 保持点移除）、`BudgetVsActualRow` DTO、`TestErpFinGlMappingResolver` 及其他编译器报到文件；**`ErpFinBudgetControlLog.projectId` 写入点双重转换核对**（M2.1 MINOR-2 观察项——该列已 String + 写入值源自延后列时存在 Java 侧转换，6 列翻转后核对写入链无 String→Long→String 双重转换，结论落盘）。
  - Skill: `nop-backend-dev`
  - 修复清单：fin-service `posting/VoucherFact`（field/getter/setter Long→String + 注释翻转标记）+ `budget/BudgetVoucherGenerator`（内部 VoucherFact record 类 projectId Long→String + :165 延后注释移除）+ `posting/ErpFinPostingProcessor`（:681 GlMappingDimensions 装配 / :732 copy / :803 fact 装配 / :886 voucherLine 写入——随 VoucherFact 翻转自动对齐）+ `posting/ErpFinGlDistributionValidator:195`（copy 链）+ `entity/ErpFinBudgetLineBizModel:137`（BudgetVsActualRow 装配）；fin-dao 手写 DTO `GlMappingDimensions`（field/getter/setter）+ `BudgetVsActualRow`（同型）；test `TestErpFinGlMappingResolver:354`（seedRule 签名 Long projectId→String）。**BudgetControlLog 写入点核对结论**：`ErpFinBudgetControlBiz.writeControlLog`（:199-224 唯一 Java 写入点）不写 projectId 列（仅 orgId/scenarioId/budgetLineId/subjectId/costCenterId/periodId 等）；全仓 grep 零 `BudgetControlLog.*setProjectId` 调用——该列仅经 GraphQL save(Map) 管道填充，**无 String→Long→String 双重转换面**（MINOR-2 观察项闭环，结论 = 无风险）。
- [x] Fix: fin 7 模块链重生成重建（**web/app 补做本体**）——`mvn clean install -pl module-finance/erp-fin-codegen,module-finance/erp-fin-dao,module-finance/erp-fin-meta,module-finance/erp-fin-service,module-finance/erp-fin-web,module-finance/erp-fin-app,module-finance/erp-fin-api -Dmaven.test.skip=true`（D3 口径 no-am）：fin-dao 6 列 join 胶水自弱类型恢复强类型 String；fin-web/fin-app 首次以 String 形态重生成重建（fin 7 处 page.yaml Fix 的重建级验证 + `rg ':Long' module-finance/erp-fin-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零复核 + YAML 良构抽验）。
  - Skill: `nop-backend-dev`
  - 结果：fin 7 模块 BUILD SUCCESS（**fin-web/fin-app 首次 String 形态构建**——prj-web String jar 前置满足）；`:Long` 手写 page 清零（rg 计数 0）；page.yaml YAML 良构抽验 10/10 通过；git 变更仅 `_gen` view 重生成（预期）。
- [x] Fix: hr 7 模块链重建——`mvn clean install -pl module-hr/erp-hr-codegen,module-hr/erp-hr-dao,module-hr/erp-hr-meta,module-hr/erp-hr-service,module-hr/erp-hr-web,module-hr/erp-hr-app,module-hr/erp-hr-api -Dmaven.test.skip=true`（2 列重生成；hr 手写 main 预期零修复面，编译器清单复核；若报到 `_gen` 关联手写调用点则修复）。
  - Skill: `nop-backend-dev`
  - 结果：hr 7 模块 BUILD SUCCESS——**手写 main 零修复面**（M3.3 预测验证：2 列仅 `_gen` 实体持有，编译器零报到）。
- [x] Fix: **prj 复跑清零（fin 翻转后终态确立，Phase 2/3 时序声明的兑付面）**——fin install 后：prj 侧 6 延后列实体 seed/断言 String 化（≥5 测试文件，如 `TestErpPrjTimesheetCost.java:223`）+ Phase 2/3 登记的全部临时桥/Long 断言移除 + `ExpenseCostAggregator.findLinesForProject` eq 语义值桥终态复核（两侧全 String 直传）+ prj 7 模块链 `-Dmaven.test.skip=true` 重建绿 + `mvn test -pl module-projects/erp-prj-service,module-projects/erp-prj-web` 复跑全绿（**Phase 3 临时绿灯转终态绿灯**；快照若二次刷新按重录流程处理并登记足迹）。
  - Skill: `nop-testing`
  - 结果：临时面清零 4 处——`ExpenseCostAggregator` eq toLong 桥移除（String 直传）+ `TestErpPrjExpenseAggregation` setProjectId 桥移除 + `TestErpPrjProjectSettlementRetention` String.valueOf 形态中立断言 ×2 简化为直接 equals + 两 Provider `readLong`→`readString`（未用 readLong helper 移除）；grep TEMP-BRIDGE/TEMP(M2.7)/toLong 残留 = 0；prj 7 模块重建 BUILD SUCCESS + **复跑 172/172 终态全绿**；`TestErpPrjTimesheetCost` 凭证断言（:223 族）经 String jar 直接通过；快照零二次刷新（_cases 762 文件稳定，`"projectId": 数字` 形态 0 残留）。
- [x] Proof: 早域复跑（B 义务验证）——`mvn test -pl module-finance/erp-fin-service,module-finance/erp-fin-web` 全绿（**497/497 基线维持** + web 0 tests 治理排除）+ `mvn test -pl module-hr/erp-hr-service,module-hr/erp-hr-web` 全绿（**237/237 基线维持** + web 0 tests 治理排除）。红则修复至绿（延后列兑付遗留问题在本计划内闭环；IoC 第二环若新侧复现按 hr 先例 delta 处理并补记 bug）。
  - Skill: `nop-testing`
  - 结果：**fin 497/497 + hr 237/237 双基线维持**（fin-web/hr-web 0 tests 治理排除 BUILD SUCCESS）；fin 侧仅 `TestErpFinGlMappingResolver` seedRule 签名 1 处编译修复后全绿；IoC 第二环未在新侧复现（零 delta 增补）。

Exit Criteria:

- [x] fin 7 模块链 + hr 7 模块链重建全绿（含 fin-web/fin-app 首次 String 形态）；fin 497/497 + hr 237/237 基线维持；**prj 复跑终态全绿（临时桥/断言清零 + eq 语义值桥终态复核）**；BudgetControlLog 写入点核对结论在案；fin page.yaml `:Long` 清零复核在案

### Phase 5 - 语义陷阱 grep 门控 + page.yaml Fix + 收尾登记

Status: completed
Targets: `module-projects/**`（手写代码 + prj-web 手写 page.yaml）、`module-finance/erp-fin-service/src/**`（延后列残留复核）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/{08-22 或执行日}.md`、`tools/id-migration-registry.json5`
Skill: none

- Item Types: `Proof | Fix | Add`

- Prereqs: Phase 3 + Phase 4

- [x] Proof: 语义陷阱 grep 门控（路线图横切 §3，prj 手写 main+test 范围 + fin 延后列相关文件复核）清零——`\.longValue\(\)`、`Long\.parseLong\(`、`Map<Long`、`Set<Long`、`String\.format\("%d` 及 `%d` 变体零命中；Long 装箱 `==`/`!=` 比较（id 上下文）逐条核清；id 序比较陷阱专项（`getId\(\)\s*[<>]|comparing.*getId`——String 字典序，contract idOrder 先例）；fin 侧 VoucherFact/GlMappingDimensions/BudgetVsActualRow 残留 Long 清零核验（延后列已翻转，合法残留应仅剩非 id 用途）；sql-lib.xml 仓内零存在（注明即可）。结果逐项记录本计划。
  - Skill: none
  - 结果：命中 3 处逐条核清——`ErpPrjDashboardBizModel:99` `((Number) row.get("cnt")).longValue()` = count 聚合合法非 id；`ErpPrjReportBizModel:196/197` `Long.parseLong` = epoch-millis/epoch-sec 日期 parse 合法非 id；`ErpPrjTaskBizModel:238` 装箱 == 为 javadoc 文本非代码。id 序比较（`getId\(\)\s*[<>]|comparing.*getId`）零命中；`Map<Long`/`Set<Long` 零命中；fin 侧 VoucherFact/GlMappingDimensions/BudgetVsActualRow/ErpFinGlMappingResolver/BudgetVoucherGenerator/ErpFinBudgetLineBizModel 延后列 Long 残留零；sql-lib.xml 仓内零存在（全 module-* 手写范围）。
- [x] Fix: prj-web 手写 page.yaml raw-GraphQL `:Long` 变量 9 处就地 String 化——dashboard:65（`$projectId:Long` → `:String`）+ kanban ×8（`$p:Long` ×4 + `$id:Long` ×4）；variables 链与 options value 链一致性核证（contract version-diff 先例）；随后 `rg ':Long' module-projects/erp-prj-web/src/main/resources/_vfs --glob '!**/_gen/**'` 清零（非 id 类型变量如 `$lim:Int` 合法保留并逐条判定）；prj-web 重建 BUILD SUCCESS 验证。
  - Skill: none
  - 结果：kanban `$p:Long` ×4 + `$id:Long` ×4 + dashboard `$projectId:Long` ×1 全部 → `:String`；variables 链核证（`p: "${projectId || null}"` 页面参数 + `id: "${id}"` 行作用域值源均 String 实体 id）+ options 链无 Long 字面量；`:Long` 手写清零（仅 `$lim:Int` 合法保留）；YAML 良构 2/2；prj-web 重建 BUILD SUCCESS。
- [x] Proof: 手写 view.xml 零改动验证——`git status module-projects/erp-prj-web` 确认无手写 view 文件被动变更（生成 view 随 codegen 更新不在此列；page.yaml 修复 diff 为本计划主动变更）。
  - Skill: none
  - 结果：prj-web 非 `_gen` 变更仅 2 个 page.yaml（本计划主动 Fix）；手写 view.xml 零被动变更在案。
- [x] Add: 登记册与移交闭环登记——orm-deferral-001..008 retired note 补兑付证据指针（三 orm diff + 链重建 + 复跑基线）；pur 被引用面破坏确认（backward-182/244 successor M2.5 指针在位）+ **dao 层被引用面登记**（sal-dao/pur-dao `_gen` prj 关系胶水对称耦合破坏 + pur/sal orm `ErpPrjProject` stub 长期 Long 残留，successor M2.6/M2.5——Current Baseline 实测清单落盘）；M2.1/M3.3 计划 Deferred 条目兑付指针（义务性最低要求 = 本计划记录；如执行时点相关计划可编辑，则在其 Closure 追注兑付标记）。
  - Skill: none
  - 结果：登记册 8 条 retired note 指向本计划（Phase 1 落盘，含三 orm diff + 链重建 + 复跑基线证据链）；backward-182（main :3027 successor M2.5）/backward-244（test :4235 successor M2.5）指针 live 复核在位；dao 层被引用面（sal-dao pom :41 / pur-dao pom :42 compile 依赖 prj-dao `_gen` 胶水 + pur/sal orm prj stub）已在 Current Baseline/Non-Goals/本项登记（successor M2.5/M2.6）；M2.1/M3.3 计划 Closure 节已追注 Deferred 兑付标记（两文件 Status Note 下新增兑付追注段）。
- [x] Add: 路线图 M2.7 → `done`（M2/M3 表位次 12 + 头部「最后更新」；标注 fin web/app 补做 + 8 条 orm-deferral 退役完成）+ 日志条目（含验证状态 + 早域复跑基线 + 移交闭环）。
  - Skill: none
  - 结果：roadmap 位次 12 行 `todo` → `done`（完整证据摘要：84 列构成/五 Phase 要点/基线/中间态）+ 头部「最后更新」改写（M2.7 done，位次 13 qa 解锁）+ `docs/logs/2026/08-22.md` 新增 M2.7 执行完成条目（时间倒序首位，含验证状态全绿段）。

Exit Criteria:

- [x] grep 门控零残留（例外逐条核清；fin 延后列残留清零）；page.yaml `:Long` 清零 + prj-web 重建绿 + view 零被动变更在案
- [x] 路线图状态、登记册退役（8 条 + 兑付 note）、移交闭环登记、日志一致

## Draft Review Record

- Independent draft review iteration 1（2026-08-22，技术/执行视角 plan-audit，ses_fd82022a7ffej4N9hD6lZZaleS）：`needs revision` — 0 BLOCKER / 3 MAJOR / 4 MINOR。事实核对大部属实（84 列构成/登记册 8 条/backward 4+4/page.yaml 9/_cases 761/pom 依赖链/fin-app :31/:36/:41/:46/工具头注 retire-first 设计/fin 残留清单/hr 零直接引用均逐项验证通过）。MAJOR：① Phase 2/3 对陈旧 fin jar 的临时绿灯未声明——fin 6 延后列跨域影响编译器不可见（prj 测试 ≥5 文件 seed/断言 fin 延后列实体），缺 Phase 4 后 prj 复跑终态确立机制；② 被引用面缺 sal-dao/pur-dao `_gen` prj 关系胶水对称耦合 + pur/sal orm prj stub Long 残留登记（M2.2 对 drp/mfg inv stub 同型先例）；③ `ExpenseCostAggregator.findLinesForProject` eq("projectId") 对 fin 延后列语义值桥陷阱（编译器不可见）无门控项。MINOR：④ 测试类 28 → 26+2 支撑件；⑤ GlMappingDimensions 实际位于 erp-fin-dao（resolver 本体无残留）；⑥ hr evidence 行号漂移 :651/:652 → :636/:637 未注记；⑦ Phase 5「可选」措辞反松弛违规。
- Independent draft review iteration 2（2026-08-22，治理/规范视角，ses_fd8137a73ffe8Z7jrAW40G9N5c）：`passes draft review` — 0 BLOCKER / 0 MAJOR / 4 MINOR（① 本节未记录 iteration 1；② Phase 4 Prereqs 应为 Phase 3 传递；③ Non-Goals fin/hr 已翻计数口径混基（hr 应 136 = 130 自有 + 6 stub）；④ Goals 残留清单 GlMappingResolver → GlMappingDimensions 命名对齐）。治理检查全过（命名/路线图对齐位次 12 + 精确前置/保护区域协议/anti-slack 零违规/检查清单完整性域级口径/内部一致性/iteration-1 七项修订全部核实在案/模板合规）。
- **修订（iteration 1 → 2，已落地）**：全部 3 MAJOR + 4 MINOR 处理——① Current Baseline/Phase 2/Phase 3 增加「陈旧 fin jar 临时绿灯」时序声明 + Phase 2 禁止持久 toLong 桥 + Phase 4 新增「prj 复跑清零」项（终态确立）+ Closure Gates 改终态复跑口径；② Current Baseline/Non-Goals/Phase 2/Phase 5/Deferred 登记 sal-dao/pur-dao `_gen` 耦合 + orm stub 残留（successor M2.5/M2.6）；③ Phase 2 新增 eq/filter 语义值桥专项清扫项（:190-196 已知命中 + Phase 4 终态复核）；④⑤⑥⑦ 计数/位置/漂移注记/措辞全部修正。iteration 2 的 4 MINOR 已就地修正（本节记录 + Phase 4 Prereqs + hr 136 口径 + Goals 命名）。
- **双独立子 agent 批准（保护区域 `model/*.orm.xml` 三文件，`ai-autonomy-policy.md` `auto + dual-agent-approval`）**：
  - 批准 1（治理视角）：ses_fd8137a73ffe8Z7jrAW40G9N5c，2026-08-22 — 批准 orm 变更面（prj 76 + fin 6 + hr 2 列 stdDataType-only、stdSqlType BIGINT 保持），条件 = MINOR 文书修正落地（已落地）+ 回写前取得技术侧第二独立批准（Phase 1 硬门控）+ 按 retire-first 顺序执行。
  - 批准 2（技术视角）：ses_fd82022a7ffej4N9hD6lZZaleS iteration 1 为 `needs revision`；iteration 3 复核（ses_fd80e9fe6ffeyCuhTqqfBDUtyq，2026-08-22）确认全部 3 MAJOR + 4 MINOR RESOLVED（逐项 live 复验，含 :223 断言/pom :41/:42/orm :1312/:1270/eq :193/_cases 761/page.yaml 9 行号逐吻合）+ iteration 2 四项 MINOR 落实 + 零新缺陷 → **技术视角批准生效（无条件）**。
- Independent draft review iteration 3（2026-08-22，技术侧复核，ses_fd80e9fe6ffeyCuhTqqfBDUtyq）：`passes draft review` — 全部发现 RESOLVED、零新增缺陷、批准记录按本节结构落盘后可转 `active`（额外复核：scan 76 = 17+51+8 逐字一致、登记册 DEFERRED 恰 8 条、84 = 76+6+2 全文算术一致、交叉引用无断链）。
- 共识达成（2026-08-22）：iteration 3 全部发现 RESOLVED + 双批准落盘（治理批准经 iteration 2 条件落地生效；技术批准经 iteration 3 生效）→ 计划转 `active`。

## Closure Gates

> 完整仓库验证定制为域级口径（路线图规则 3 D3 修订：禁止以全量构建为中间 gate；全量构建仅存在于 M4.1）。

- [x] 范围内行为完成（84 列落源 + prj no-am 7 模块重生成 + 手写代码/测试修复 + 快照重录 + fin/hr 链重建含 fin web/app 补做 + 早域复跑 497/497 + 237/237 + grep 门控清零 + page.yaml 9 处 Fix）
- [x] 相关文档对齐（路线图 M2.7 状态、登记册 8 条退役 + 兑付 note、移交闭环登记（M2.1/M3.3 Closure 追注）、日志）
- [x] 已运行验证：`mvn clean install -pl <prj 7 模块显式列表> -DskipTests` 全绿 + `mvn clean install -pl <fin 7 模块显式列表> -DskipTests` 全绿 + `mvn clean install -pl <hr 7 模块显式列表> -DskipTests` 全绿 + `mvn test -pl module-projects/erp-prj-service,module-projects/erp-prj-web` 全绿（**Phase 4 fin 翻转后终态复跑口径，172/172**）+ `mvn test -pl module-finance/erp-fin-service,module-finance/erp-fin-web` 全绿（497/497）+ `mvn test -pl module-hr/erp-hr-service,module-hr/erp-hr-web` 全绿（237/237）+ 工具重扫三段零残留
- [x] 无范围内项目降级为 deferred/follow-up（fin adaptor 行为级页面验证为治理排除下的显式 successor 登记，非范围降级）
- [x] 保护区域双独立子 agent 批准记录落盘（Phase 1 前置）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（执行者声明：本项留待 mission CLOSURE_VERIFY 独立审计，执行者未自我审计；2026-08-22 独立结束审计会话执行通过并勾选本门控，证据见 Closure 节）
- [x] 结束证据存在于文件中（各 Phase 结果记录 + 验证状态全绿段；独立结束审计证据见 Closure 节）

## Deferred But Adjudicated

### purchase/sales 对 prj 的引用与 dao 耦合（main 1 + test 1 文件 + sal-dao/pur-dao `_gen` 胶水 + orm stub 残留）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 登记册 backward-pointer（backward-182/244）+ M0 裁决 §10 对称 `_gen` 耦合（sal-dao/pur-dao compile 依赖 prj-dao，翻转破坏唯两端同时 String 自愈）+ pur/sal orm `ErpPrjProject` stub Long 残留（M2.2 对 drp/mfg 内 inv stub 同型）——均为预先登记中间态（本计划 no-am reactor 不含 sal/pur）
- Successor Required: `yes`（M2.5/M2.6 plan Phase 2/3 自愈 + M4.1 兜底）

### `ErpPrjWebPagesTest`/`ErpFinWebPagesTest` 页面校验

- Classification: `watch-only residual`
- Why Not Blocking Closure: `@Tag("full-app")` + surefire excludedGroups 为先于本 mission 的已提交治理决策，实证依赖全量 classpath
- Successor Required: `yes`（M4.1 app-erp-all `ErpAllWebPagesTest`）

### fin adaptor 行为级页面验证（7 处 M2.1 Fix + 本计划重建级验证之后）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 重建 + grep 清零 + YAML 良构在本计划完成；页面运行时行为验证依赖全量 app classpath（治理排除决策）
- Successor Required: `yes`（M4.1 mission 级 page.yaml 清扫与页面验证）

### 平台 IoC 回归兼容层 delta（若复现落位）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 平台 bean-init self-wait 为已登记平台 Bug（`docs/bugs/2026-08-21-nop-sequence-generator-ioc-self-wait-*.md` + 第二环 bug 补记）；本计划按先例 delta 断环
- Successor Required: `yes`（平台修复后统一移除全部兼容层 delta，M4.1 复核）

## Closure

Status Note: completed（2026-08-22：五 Phase 全部执行完毕，全部验证全绿——prj/fin/hr 三链 7 模块 `-DskipTests` BUILD SUCCESS（含 fin-web/fin-app 首次 String 形态）+ prj service **172/172 终态绿**（web 0 tests 治理排除）+ **fin 497/497 + hr 237/237 双基线维持** + 工具重扫三段零残留（orm-column-deferral kind 清零）；位次 13 quality 解锁供后续批次。独立结束审计通过（2026-08-22，证据见下）。）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission CLOSURE_VERIFY 步骤，新会话、无执行者上下文，2026-08-22）
- Evidence: 独立审计会话对 live 仓库逐项复核通过——① 三 orm 落源实证：prj `id` PK 列族 `stdDataType="string"`（残留 long 仅 delVersion 等 17 处非 PK/FK 列，符合规则 4）+ fin 6+1 projectId 列（:495/:932/:1343/:1398/:1836/:2046 + BudgetControlLog :1899）+ hr projectId/taskId（:636/:637）全 string；② 登记册 `tools/id-migration-registry.json5` orm-deferral-001..008 全部 retired + 兑付 note 指向本计划；③ 路线图位次 12 M2.7 → `done`（完整证据摘要）+ 头部最后更新 + `docs/logs/2026/08-22.md` M2.7 条目（含验证状态全绿段）；④ prj-web 手写 `_vfs` `rg ':Long'` 零命中 + TEMP-BRIDGE/TEMP(M2.7)/toLong 残留零；⑤ fin `VoucherFact.projectId`/`GlMappingDimensions.projectId` = `private String`（延后列残留清零实证）；⑥ M2.1/M3.3 计划 Closure 节 Deferred 兑付追注在位（M2.1 :261 / M3.3 :215）；⑦ 快照零 `"id": 数字` 形态残留。结论：approved——五点一致（Plan Status/各 Phase Status/Exit Criteria/Closure Gates/Closure 证据）、Deferred 条目均为预登记中间态 successor 指针（M2.5/M2.6/M4.1）非范围降级、文档同步义务履行。

Follow-up:

- （无范围内跟进项；已确认缺陷不得出现在此处。中间态 successor 指针见 Deferred But Adjudicated 与 Phase 5 登记记录：purchase main 1 + test 1 文件 + sal-dao/pur-dao `_gen` prj 胶水 + pur/sal orm prj stub（M2.5/M2.6）+ `ErpPrjWebPagesTest`/`ErpFinWebPagesTest` 治理排除 + fin adaptor 行为级页面验证（M4.1）。）
