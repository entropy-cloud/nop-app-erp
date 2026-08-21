# 2026-08-21-1657-1-bigint-id-m02-forward-coupling-registry 主键/外键 string 化 M0.2：前向耦合登记册 + 工具豁免机制

> Plan Status: completed（2026-08-21，iteration 2 独立复审 `passes draft review` 转正执行；三 Phase 完成后独立结束审计 `passes closure audit`，见 Closure 节）
> Mission: id-string-migration
> Work Item: M0.2
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M0.2（M0 裁决 D5(d) 新增，`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md` §10.2 D4/D5(d)）
> Related: `docs/plans/2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs.md`（M0.1，前置）、`docs/plans/2026-08-21-1657-2-bigint-id-m12-notify-migration.md`（M1.2，本登记册的直接消费者）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1，rule-6 停止与 M0 裁决触发者）
> Audit: required（工具 + 文档工作，不触及 `model/*.orm.xml` 保护区域；独立草案审查 + 独立结束审计）

## Current Baseline

- **工作项来源**：M0 裁决 D5(d)（2026-08-21，iteration 3 双 passes）新增 M0.2，定义为 M1.2 之前执行：19 orm refEntityName 跨域图 × 冻结序系统扫描 + M0 审计附录 A 205 文件 service 耦合 id 流向全量复核 + `check-bigint-id-types.mjs` 豁免机制实现，产出每域登记册（含 disposition），后续每个域 plan 起草强制消费。
- **已知 orm 级前向边（双审清，尚未机器化复验）**：恰 2 簇——fin orm→prj 6 to-one（`app-erp-finance.orm.xml` :517/:958/:1361/:1417/:1859/:2063；fin-dao pom 编译依赖 prj-dao）、hr orm→prj 2 to-one（:651-652）。两位 M0 裁决审查者已各自全量复扫 19 orm 确认无第三簇；M0.2 须以可重复的机器化扫描固化该结论（人为复扫不可重放）。
- **service 级前向边（已确认存在，完整清册缺失）**：已确认 ≥1 簇——ast-service `ErpAstDisposalProcessor:264/271` → `IErpMntEquipmentBiz:26/35`（Long assetId；ast 位次 7 早于 mnt 位次 11，构成前向）；独立抽查显示更大面（~28 文件/11 域，含 fin（6）→inv（10）`ErpFinAccountingPeriodProcessor:222` → `IErpInvCostingBiz:30` Long periodId）。完整 service 级前向清册归本工作项产出。
- **附录 A 现状**：`docs/audits/2026-08-21-1045-id-migration-m0-cross-domain-coupling-appendix.md` 已有 205 文件/289 跨域边的 file:line 证据清单（耦合点 + id-as-Long 证据行），但**未按冻结序位次做 id 流向分类**（前向=早域引用晚域 / 后向=晚域引用早域）。附录 C 另有 dao 层语义 FK Long 参数 82 处/11 域清单（本域迁移时语义门控翻转，不属前向耦合）。
- **冻结总序（M0.1 冻结、D6 维持）**：master-data(1,done) → notify(2) → aps(3) → b2b(4) → contract(5) → finance(6) → assets(7) → cs(8) → hr(9) → inventory(10) → maintenance(11) → projects(12) → quality(13) → manufacturing(14) → purchase(15) → sales(16) → crm(17) → drp(18) → logistics(19)。md 已迁移（M1.1 done）：prj-dao 27 错/15 文件 + fin-dao 97 错/32 文件（100% `_gen`）已登记中间态（successor M2.7/M2.1 + M4.1 兜底）。
- **工具现状（08-21 实况）**：`tools/check-bigint-id-types.mjs`（scan/dry-run/apply；scan 全仓 1594 NEEDS FIX = 1662 − md 已迁移 68；**无任何豁免机制**——dry-run 副本无条件翻转全部 PK/BIGINT FK 列，含应延后的 fin→prj 6 列与 hr→prj 2 列）；`tools/verify-id-fix-copy-diff.mjs`（新鲜度门控：diff 实况源 vs 时点副本零非 stdDataType 行；无豁免语义）；`tools/scan-cross-domain-id-coupling.mjs`（跨域耦合 A/B/C 三段输出：耦合点 file:line / id-as-Long 证据行 / 域对汇总；**仅扫 main 代码，无冻结序位次分类**）；`tools/freeze-id-migration-order.mjs`（冻结序判据脚本）。
- **D4 处置模板（登记册条目语义，最终设计落本计划并走草案审查）**：orm 级前向边 = 列延后（早域迁移时延后列保持 Long，工具回写显式豁免 + scan 门控例外「残留 ⊆ 登记册延后列」；M2.7 projects plan 同批翻转 prj orm + fin 6 列 + hr 2 列并显式拥有 fin/hr 链修复责任）；service 级前向边 = 临时桥接（早域 plan 调用点加 String→Long 转换桥 + grep 清单登记例外，晚域 plan 翻转 IBiz 参数并移除桥接点）；规则 6 修订 = 登记册内预先登记的自身链破坏不触发停止，未登记破坏仍触发。
- **剩余差距**：登记册不存在；工具无豁免机制（若 M2.1 finance 在无豁免机制下执行回写，dry-run 副本会翻转 fin→prj 6 列，产生早域引用晚域 Long 实体的对称 `_gen` 破坏——正是 D4 要预防的）；service 级前向边完整清册不存在；各域 plan 无强制消费对象。

## Goals

- **orm 级跨域图机器化系统扫描**：解析 19 orm 的全部跨域 refEntityName/关系引用，按冻结序位次分类（前向/后向），产出可重复运行的扫描产物；预期结论 = 恰 2 前向簇（fin→prj 6、hr→prj 2），任何偏差按实况登记并按路线图规则 6 语义回报（若发现第三簇）。
- **service 级耦合 id 流向全量分类**：附录 A 205 文件 + `scan-cross-domain-id-coupling.mjs` 全量重跑，逐边标注方向（前向/后向）与 scope（main/test），产出**前向边完整清册**（调用点 file:line + 被调 IBiz/实体 + Long 参数签名 + 涉及早/晚域位次）。
- **工具豁免机制**：`check-bigint-id-types.mjs` 读登记册——延后列在 dry-run 副本保持 `long`、scan 报 `DEFERRED(registry)` 而非 `NEEDS FIX`；`verify-id-fix-copy-diff.mjs` 新鲜度门控对齐豁免语义（延后列的「未翻转」不构成非 stdDataType 差异行）。
- **每域登记册汇编**：19 域 + common-service/common-test 逐域条目（orm 级列延后 / service 级临时桥接 / 后向 successor 指针三类 disposition），机器可读登记册文件 + 人类可读审计工件双载体。
- **消费协议落 roadmap**：登记册路径、消费时机（各域 plan 起草与 Phase 1 强制消费）、条目退役机制（M2.7 翻转 fin 6 列 + hr 2 列时退役对应条目）写入 `docs/backlog/id-string-migration-roadmap.md`；roadmap M0.2 → `done` + 日志。

## Non-Goals

- 不迁移任何域的 orm（M1.2 及后续各域 plan 范围）；不改任何 `model/*.orm.xml`（保护区域，本计划零触碰）。
- 不实现 service 级临时桥接代码本身（归各早域 plan，按登记册 disposition 执行）。
- 不修改冻结总序（M0.1 冻结、D6 维持；若扫描发现与冻结序矛盾的新前向簇，按路线图规则 6 语义回报裁决，不自行重排）。
- 不修复已登记中间态（prj-dao/fin-dao `_gen` 破坏，successor M2.7/M2.1 + M4.1）。
- 不做 E2E 影响面修复（归 M4.1，附录 F 已有清单）。
- 不扩展豁免语义到非 PK/FK BIGINT 列（delVersion 等本就不在改造范围，工具已防御性限定）。

## Task Route

- Type: `verification or audit work`（登记册 = 审计工件）+ 工具 Add（豁免机制，mjs 脚本层）
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`（M0.2 条目 + D4/D5(d) 裁决）、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md` §10（Decision D 权威文本）、附录 A/C（复核输入）
- Skill Selection Basis: 复用 M0.1 已用审计方法技能 `orm-model-audit-prompt`（orm 关系解析口径）+ `cross-module-dependency-audit-prompt`（跨域耦合分类口径）作为扫描方法对齐参考（与 roadmap §M1-M3 预期技能节 M0.1 行一致）；工具实现为 mjs 脚本，无平台技能匹配（`Skill: none`）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯仓内工具 + 文档工作；node 已为既有工具运行时）。

## Execution Plan

### Phase 1 - orm 级跨域图机器化扫描 + service 级前向边全量清册

Status: completed（2026-08-21）
Targets: `tools/scan-id-coupling-directions.mjs`（新增）、`docs/audits/2026-08-21-1657-id-m02-orm-graph.json`、`docs/audits/2026-08-21-1657-id-m02-coupling-directions.json`
Skill: `orm-model-audit-prompt`（方法对齐）+ `cross-module-dependency-audit-prompt`（分类口径）

- Item Types: `Add | Proof | Decision`
- Prereqs: M0.1 done ✅

- [x] Decision: 扫描实现载体——**新增专用可重复脚本 `tools/scan-id-coupling-directions.mjs`**（非一次性脚本，非扩展 `scan-cross-domain-id-coupling.mjs`）。理由：① 产物可重复运行（两次运行 diff 为零，DETERMINISTIC 实测）且冻结序位次表内嵌权威常量 + 每次运行 spawn `freeze-id-migration-order.mjs --format json` 交叉核验、不一致非零退出（fail-closed，核验方式即内嵌于运行时）；② 不改动 M0.1 的 `scan-cross-domain-id-coupling.mjs`，保持附录 A 再生成口径稳定（对账基线不被污染）；③ M0.2 需求（orm 解析 + 位次分类 + JSON 产物 + IBiz 签名解析 + test 扫描）与现有扫描器的 markdown 输出结构差异过大。替代方案（扩展现有扫描器）被否：会改变附录 A 复现行为且单一脚本职责混杂。**test-scope 扫描规则裁定**：探测信号 = 跨域 `import app.erp.<pkg>.`（含 static import），而非 `@Inject` 字段正则——实测 fin→inv 簇的形态是局部变量 + `bizObjectManager.getBizObject("ErpInvCosting").asProxy()`（`ErpFinAccountingPeriodProcessor.java:213-215`），`@Inject` 模式不足以覆盖；import 信号统一覆盖 @Inject 字段 / 局部变量 / 运行时代理三种形态。扫描范围 = 19 域全部 `erp-*` 层的 `src/test/java` + common-service/common-test 的 main+test；java 侧统一做注释打码（保留换行）区分编译级引用与 javadoc `{@link}` 注释级引用（后者登记册排除）。orm 侧行号计算须在注释打码时保留 `\n`（长度保持但换行坍缩会导致行号偏移——实现期发现并修复，fin 首簇偏移 34 行）。
  - Skill: none
- [x] Add: test-scope 耦合扫描能力——已实现（`scanTestScope()`）：434 个 test 耦合文件，其中**前向引用文件 30 个**（早域 test 引用晚域，清单见 directions 工件 `testScope.files[].refs[].direction`，每条含 file:line + 引用形态）；产出路径独立于 main 扫描（`testScope` 段），M1.2 Phase 1 (iv) 等各域 plan 的消费对象。
  - Skill: none
- [x] Add: orm 级跨域图扫描——已实现：19 orm 全量解析 to-one/to-many refEntityName（列级：entity.column → 外域实体），**640 条跨域边**（前向 8 / 后向 632），机器可读工件 `docs/audits/2026-08-21-1657-id-m02-orm-graph.json`。未做跨域 dao 手写 import 复核（附录 A §5 已双审清，直接引用）。
  - Skill: none
- [x] Proof: 扫描结论与双审已知结论一致性——**orm 级前向边恰 = fin→prj 6 列 + hr→prj 2 列，file:line 逐行吻合 M0 裁决 §10.1**：`app-erp-finance.orm.xml:517/:958/:1361/:1417/:1859/:2063`（全部 `projectId`→ErpPrjProject to-one）+ `app-erp-hr.orm.xml:651/:652`（`projectId`→ErpPrjProject / `taskId`→ErpPrjTask to-one）。无第三簇、无计数偏差，规则 6 不触发。
  - Skill: none
- [x] Add: service 级 id 流向全量分类——已实现：main 侧 205 耦合文件 / 300 文件×外域对 / 82 域对（与附录 A 域对汇总行数一致），逐 ref 标注 direction × kind（ibiz/entity/dto）× commentLevel；**前向边完整清册 95 条**（编译级 94 + 注释级 1 条已标记排除：`IErpMfgMrpPlanLineBiz.java:14` javadoc `{@link ErpPurOrder}`，与附录 B「补充」段一致），每条含 callSite file:line、被调 IBiz 方法/实体符号、Long 参数名（IBiz 接口签名解析）、早/晚域位次、disposition=临时桥接、retireOwner=晚域工作项。**ast→mnt 已知簇验证**：`ErpAstDisposalProcessor.java:264/271` → `IErpMntEquipmentBiz.changeStatusForAssetDisposal/restoreFromAssetDisposal`，Long 参数 `assetId` ✓；**fin→inv 抽查簇验证**：`ErpFinAccountingPeriodProcessor.java:222` → `IErpInvCostingBiz.reclosePeriodCosts`，Long 参数 `periodId` ✓（另有 :15 import、:536/:537/:544 ErpInvLandedCost daoFor 实体引用共 5 条）。`--dao` 口径对账：本扫描器与 `scan-cross-domain-id-coupling.mjs --dao` 再生成输出做程序化对账（`--reconcile`），文件×外域对集合 **300 vs 300，match=true，双向零差集**。
  - Skill: none
- [x] Proof: 抽样复核——**前向清册按涉及域分层抽样 14 条逐条人工核对**（源码打开验证 Long 语境与方向）：ast→mnt `ErpAstDisposalProcessor:264/271`（`asset.getId()`/`disposal.getAssetId()` → Long assetId ✓）；fin→inv `:222`（`period.getId()` → Long periodId ✓）；aps→mfg/inv `ErpApsAtpCtpServiceImpl:141/212/234/244`（`Long reservationId`、`bom.getId()`、`findDefaultBom(Long materialId)`、`loadBomOperations(Long bomId)` ✓）；ct→pur/sal `ErpCtInvoicePlanBizModel:32/34`（实体引用 + 变量级 id 流转 `invoice.setSupplierId(contract.getPartnerId())` 等 :142-165 ✓）；qa→sal `RecallTargetLocator:16/17` + `NcrReturnOrchestrator:99/117`（`IErpPurReturnBiz.save`/`IErpSalReturnBiz.save` 继承方法调用点 ✓）；cs→crm `TicketAssignResolver:3`（`crmTeamBiz.findList` :52 ✓）；b2b→pur `ErpB2bAsnCreateReceiveFromAsnProcessor:10`（`findPoLines(po.getId())` :118 ✓）；inv→pur `ErpInvLandedCostApproveProcessor:11`（`landedCost.getReceiveId()` :66/:71 ✓）；pur→drp `ErpPurReceiveProcessor:324`（`move.getId()` → Long inboundMoveId ✓）；mnt→qa `OeeCalculator:10`（`daoFor(ErpQaInspection.class)` :340 ✓）。方向判定全部与冻结序位次一致。**后向边对账**：域对汇总 82 对 = 附录 A 82 行（零流失）；文件×外域对 300/300 程序化 match；main 前向 43 / 后向 257 文件对，test 前向 34 / 后向 441 / infra 161 文件对。
  - Skill: none

Exit Criteria:

- [x] orm 级跨域图中间工件在案（`docs/audits/2026-08-21-1657-id-m02-orm-graph.json`，可重复生成、确定性输出）且结论 = 恰 2 前向簇（fin→prj 6 + hr→prj 2，file:line 与 M0 裁决 §10.1 逐行一致，无偏差→规则 6 未触发）
- [x] service 级前向边完整清册在案（`docs/audits/2026-08-21-1657-id-m02-coupling-directions.json` `forwardInventory` 段，含 ast→mnt 与 fin→inv 簇验证结果），main（编译级 94 条）与 test（30 文件，`testScope` 段独立产出路径）两个 scope 分列，抽样复核记录在案（14 条分层人工核对 + 程序化对账 300/300 + 域对 82/82）

### Phase 2 - 登记册数据结构 + 工具豁免机制实现

Status: completed（2026-08-21）
Targets: `tools/check-bigint-id-types.mjs`、`tools/verify-id-fix-copy-diff.mjs`、登记册数据文件（路径见 Decision）
Skill: none

- Item Types: `Decision | Add | Proof`
- Prereqs: Phase 1

- [x] Decision: 登记册载体与路径——机器可读登记册文件（建议 `tools/id-migration-registry.json5`，含条目结构 `{id, kind: orm-column-deferral | service-bridge | backward-pointer, domain, entity, column?, callSite?, deferredUntil(工作项号), retireOwner(工作项号), evidence(file:line), status: active | retired}`）+ 人类可读权威审计工件（`docs/audits/` 下，含全部条目表格与消费协议）；工具只读机器可读文件。初始条目集 = Phase 1 产出的全部前向边（orm 级 8 列 + service 级全量）；后向 successor 指针条目为**强制登记内容**（各域 plan 起草消费集的组成部分，含本域被外域 test-scope 引用清单）；「非工具门控对象」的限定保留——scan/dry-run 仅消费 orm-column-deferral 条目，门控范围与登记范围分离。理由与替代方案（登记册并入 roadmap 正文 vs 独立文件）记录本计划。
  - **已落地**：载体 = `tools/id-migration-registry.json5`（机器可读，**254 条 active** = orm-column-deferral 8 + service-bridge main 94 / test 30 + backward-pointer main 60 / test 62；由 `scan-id-coupling-directions.mjs --registry-out` 生成，文件头登记退役协议：退役后本文件转手工维护权威，重跑生成器仅用于对账复核）；人类可读权威工件 = `docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md`（Phase 3 落盘）；门控范围（scan/dry-run/门控仅消费 active orm-column-deferral）与登记范围（全量 254 条含后向指针与 test-scope 被引用清单）分离已实现于两工具代码。替代方案（登记册并入 roadmap 正文）被否：roadmap 是人类导航文档，工具消费需结构化数据文件 + fail-closed 解析，254 条目内联正文会淹没唯一动态状态块并使工具解析耦合文档格式。
  - Skill: none
- [x] Add: `check-bigint-id-types.mjs` 豁免机制——读取登记册 active 条目：scan 模式将延后列报 `DEFERRED(registry)`（不计入 NEEDS FIX）；dry-run 模式生成副本时延后列保持 `long` 不翻转；豁免仅作用于 orm-column-deferral 条目，service-bridge 条目不影响列翻转逻辑。行为开关无需引入（登记册文件存在即生效）；**fail-closed 钉死**：登记册文件缺失或不可解析时，两工具显式报错并以非零退出，禁止警告后静默回退（静默回退 = dry-run 无条件翻转延后 8 列，正是 D4 要预防的破坏）。
  - **已实现**：`loadRegistry`（缺失/不可解析/缺 entries 数组三态均 `[FAIL]` + exit 1）+ `buildDeferredMap`（仅 kind=orm-column-deferral 且 active）+ `analyzeFile` deferred 标记（scan 报 `DEFERRED(registry)`，summary 单列计数）+ `buildFixedText` 排除 deferred（dry-run/apply 不翻转）+ 延后条目命中核对（条目未命中任何 NEEDS FIX 列时 WARN，提示转 retired——M2.7 翻转后防陈旧条目）。fail-closed 实测：缺失 registry → `[FAIL] ...exit 1`；不可解析 registry → `[FAIL] ...exit 1`。
  - Skill: none
- [x] Add: `verify-id-fix-copy-diff.mjs` 对齐——新鲜度门控 diff 中，延后列的「源 long ↔ 副本 long」本就无差异行；需对齐的是：门控输出的「预期变更列计数」须排除延后列（避免 M2.1/M3.3 执行时计数误判），延后列若意外出现在差异行（如被翻转）须报 FAIL。该对齐同时保证 M2.1/M2.7 落源闭环在豁免语义下自洽：延后列在时点副本与实况源同为 `long`，整文件 cp 落源不引入回滚或误翻转，门控计数与实际落源行集一致（M2.7 退役 8 条目后同批翻转 prj + fin 6 列 + hr 2 列，闭环还原）。
  - **已实现**：复用 `loadRegistry`（同 fail-closed）+ `parseFile`（自 check-bigint 导出）计算延后列 `<column>` 标签行号区间 → 差异行落入区间即 `[DEFERRED-FLIPPED]` 计非法差异行 FAIL；逐域/合计变更行计数天然排除延后列并显式标注（「登记册延后列 N 保持 long，不计入变更行」）。实现期发现并修复一处门控既有缺陷：全 19 域模式下已迁移域（md）无待改列 → dry-run 不生成副本 → `[MISSING-COPY]` 恒 FAIL；修正为实况源零待改列（BIGINT PK/FK 非 string 且非延后）时按 `[NO-COPY-NEEDED]` 合法缺席放行，有待改列仍 FAIL（实测 md 放行 exit 0、删 hr 副本 FAIL exit 1）——否则 M2.1 迁移完成后 gate module-finance 将永久误报，正是「计数误判」类破坏。
  - Skill: none
- [x] Proof: 豁免机制端到端验证（不落任何源文件）——① `node tools/check-bigint-id-types.mjs scan` 重跑：预期 `NEEDS FIX` = 1594 − 8（fin 6 + hr 2 延后）+ `DEFERRED(registry)` = 8（md 段保持 0）；② `node tools/check-bigint-id-types.mjs dry-run` 重跑后检查 `_tmp/bigint-id-string-fix/` 中 fin/hr 副本：延后 8 列保持 `long`、其余列照常翻转；③ `node tools/verify-id-fix-copy-diff.mjs module-finance` 输出的预期变更计数排除 6 延后列。三项结果逐条记录本计划。
  - **结果在案**：① scan exit 0 = **1586 NEEDS FIX（1594−8）+ 8 DEFERRED(registry)**（fin 6：ErpFinVoucherLine/GlBalance/ExpenseClaimLine/EmployeeAdvance/BudgetLine/GlMappingRule.projectId；hr 2：ErpHrTimesheetLine.projectId/taskId）+ md 段 0 + 0 WARN/0 BLOCKER；② dry-run exit 0 = 修改 18 文件/1586 列、XML 18/18、副本重扫残留 0、幂等 yes；副本程序化核验 fin **6/6 延后保持 long + 208/208 其余翻转**、hr **2/2 + 136/136**（零误翻/零漏翻）；③ gate module-finance exit 0 = **变更行 208（= fin 全量待改 214 − 6 延后）**，显式输出「登记册延后列 6 保持 long，不计入变更行」。负面测试 4 项全过：副本内人为翻转延后列 → `[DEFERRED-FLIPPED]:932` exit 1（dry-run 重刷还原后复绿）；registry 缺失 → 两工具 `[FAIL]` exit 1；registry 不可解析 → exit 1；有待改列域副本缺席 → `[MISSING-COPY]` exit 1。附：全 19 域 gate = 合计 1586 + md NO-COPY-NEEDED + exit 0。
  - Skill: none

Exit Criteria:

- [x] 登记册数据文件落盘（Phase 1 全部前向边条目 active）且 Decision 记录在案（`tools/id-migration-registry.json5` 254 active 条目 = 前向全量 132（8 orm + 94 main + 30 test）+ 后向强制登记 122；orm 级 8 条 evidence file:line 与 M0 裁决 §10.1 逐行复核一致）
- [x] 三项端到端 Proof 通过（scan 计数 = 1586 NEEDS FIX + 8 DEFERRED；dry-run 副本延后列保持 long；门控计数排除延后列）

### Phase 3 - 每域登记册汇编 + 消费协议落 roadmap + 收尾

Status: completed（2026-08-21）
Targets: `docs/audits/`（登记册权威工件）、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/08-21.md`（或执行日）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] Add: 人类可读登记册工件落盘 `docs/audits/`——19 域 + common-service/common-test 逐域视角的条目表（本域作为早域的前向义务 = 列延后/桥接；本域作为晚域的退役/翻转义务；本域 plan 起草时须消费的条目全集），每条含 evidence file:line 与 owner 工作项指针。
  - **已落地**：`docs/audits/2026-08-21-1657-id-m02-forward-coupling-registry.md`（740 行，由登记册程序化生成 + 手写协议段）——§1 消费协议（起草消费 / 退役机制 / 规则 6 联动 / 工具门控范围）、§2 总览矩阵（19 域 × A1/A2/A3/C1/C2/被引用/B 计数；列合计 8/94/30/60/62 与登记册零偏差，B 合计 102 = 94 main 桥接 + 8 orm）、§3 orm 延后 8 条全量表、§4/§5 前向边与后向指针索引（行级证据指针 → coupling-directions.json）、§6 逐域视角（冻结序 19 域，每域 A 前向义务（A1 列延后 / A2 main 桥接表含调用点 file:line + Long 参数 + 退役 owner / A3 test 桥接表）+ B 退役义务 + C 消费集（C1/C2 后向指针逐条含 evidence 文件清单 + successor + 被引用清单汇总））、§7 特殊行（master-data 已迁移中间态注记 / notify 零条目 / common-service M1.3 done / common-test Proof 载体）、§8 复核与再生成（对账复核命令 + 勿覆盖警告）。
  - Skill: none
- [x] Add: roadmap 消费协议写回——M0.2 条目补登记册路径与消费协议（各域 plan 起草与 Phase 1 强制消费本域条目；M2.7 翻转 fin 6 列 + hr 2 列时退役对应 orm 级条目；service 桥接条目由晚域 plan 翻转 IBiz 参数时退役）；M0.2 → `done`。
  - **已落地**：roadmap 五处更新——① M0.2 状态 `todo` → `done`（含执行摘要）；② 新增「M0.2 产出指针」段（登记册两载体路径 + 扫描器 + 消费协议四条：起草/Phase 1 强制消费本域条目全集（A1 延后不翻转 / A2 桥接 / A3 测试适配 / C 定位面）、退役机制（M2.7 同批翻转 + 退役 001..008 / main 桥接晚域退役 / test 桥接早域自身退役）、工具门控仅消费 active orm-column-deferral + fail-closed、规则 6 联动）；③ M1-M3 标准结构 Phase 1 补「M0.2 登记册强制消费」注记（各域 plan 复制此结构时自动继承）；④ 头部「最后更新」行；⑤ mermaid M0_2 → done。
  - Skill: none
- [x] Proof: 消费协议可操作性预演——以 M1.2 notify（本批下一个 plan）为消费者走查一遍：notify 视角条目集（预期 = 零前向义务 + 作为晚域零退役义务 + 后向被引用清单）能否从登记册工件直接读出；走查结论记录本计划（如不可读出，修工件结构后再收尾）。
  - **走查结论（可直读，无需回修）**：以 M1.2 plan 起草者视角仅读登记册工件——§6.2 notify 节直接给出：A 前向义务「无（本域不引用任何晚域）」（= 0 orm 延后 + 0 main 桥接 + 0 test 桥接）、B 退役义务「无」、C 被引用清单 = 14 域 main（43 文件）/ 14 域 test（28 文件）逐域文件数（backward refDomain=notify 共 28 条，successor 均为引用方自身工作项——notify 迁移后 String API 由引用方 plan 各自消费）；§7 特殊行同步确认零条目 + 零义务。与机器可读登记册程序化核验一致（domain=notify / refDomain=notify 全维度计数 0-0-0-0 / 被引用 28 条）。**M1.2 plan 的登记册消费段可直接引用 §6.2 + §7 notify 行落 plan**。
  - Skill: none
- [x] Add: 日志条目（含验证状态）。
  - **已落地**：`docs/logs/2026/08-21.md` 顶部新增 M0.2 条目（产出四块 + 验证状态全绿明细 + 下一步 M1.2）。
  - Skill: none

Exit Criteria:

- [x] 登记册工件 + roadmap 消费协议 + M0.2 done 状态 + 日志四者一致，notify 走查结论在案（四文件交叉引用同一套计数与路径：254 条目 / 8 延后 / 1586+8 / M2.7 退役链）

## Draft Review Record

- Independent draft review iteration 1（2026-08-21，独立子 agent fresh session，ses_fdc69b661ffe70u76Gyu62clpE）：`needs revision` — 0 BLOCKER / **2 MAJOR** / 3 MINOR。事实核对 A①-A⑤ 全部属实（scan 1594 精确；fin→prj 6 + hr→prj 2 行号逐行吻合，审查者另做 19 orm × 638 处跨域 refEntityName 机器化全量复扫确认前向边恰 8 处无第三簇；ast→mnt 与 fin→inv 簇 file:line 属实；4 工具存在且零豁免机制；1586+8 算术正确）。发现与处置：
  - MAJOR-1 后向 successor 指针条目「可选登记」与 Goals 三类 disposition / Phase 3 notify 走查 / M1.2 Phase 1 (iv) 硬消费冲突且违反反松弛措辞 → **已修**：Phase 2 Decision 改为强制登记内容（含本域被外域 test-scope 引用清单），保留「非工具门控对象」限定（门控范围与登记范围分离）。
  - MAJOR-2 test-scope 边分类承诺无产出路径（`scan-cross-domain-id-coupling.mjs:64` 硬编码 `src/main/java`）→ **已修**：Phase 1 新增「test-scope 耦合扫描能力」Add 项（载体 Decision 同时裁定 test 探测规则，test 耦合形态 = 直接 import 静态引用而非 `@Inject` 字段）+ Phase 1 Exit Criteria 补「main 与 test 两个 scope 分列」。
  - MINOR-1 登记册文件缺失行为「报错或警告，执行期定夺」未钉死 → **已修**：fail-closed（缺失/不可解析 = 报错 + 非零退出，禁止静默回退）。
  - MINOR-2 M2.1/M2.7 落源闭环自洽性为隐式成立 → **已修**：Phase 2 `verify-id-fix-copy-diff.mjs` 项补显式论证句。
  - MINOR-3 Deferred But Adjudicated 收录从未在范围内的项 → **已修**：改述为 D4 既定归属注记，非范围收缩。
- Independent draft review iteration 2（2026-08-21，独立子 agent fresh session，ses_fdc6058b9ffey0JTrPnMQCBuyY）：`passes draft review` — 逐条核对 5 项发现在计划正文真正落地修复（MAJOR-1 强制后向登记 + 门控/登记范围分离声明；MAJOR-2 test-scope Add 项 + 载体 Decision 裁定 test 探测规则（`scan-cross-domain-id-coupling.mjs:64` 硬编码实测复核）+ Exit Criteria main/test 分列；MINOR-1/2/3 均落地），修订未引入 BLOCKER/MAJOR 级新问题；反松弛零违禁词；数字链三处一致（1586+8+md 0）；审查者另独立完成 19 orm 全量跨域 refEntityName 复扫（前向边恰 = fin→prj 6 + hr→prj 2，无第三簇）。2 MINOR 放行不修：① Phase 2 Proof ① 等式两计数器并列初读易混（Exit Criteria 已消歧）；② Phase 1 行 62（test 产出）/行 68（汇编标注）分工可推断未点破。**批准计划转 active**。
- 共识达成（2026-08-21）：iteration 2 `passes draft review`（0 BLOCKER / 0 MAJOR）→ 计划转 `active`。

## Closure Gates

> 本计划为工具 + 文档工作，零 Java 代码、零 orm 源文件变更——完整仓库 `typecheck`/`build` 门控不适用，验证命令 = Phase 2/3 的三项工具端到端 Proof + 重跑一致性（下述第 3 项）。

- [x] 范围内行为完成（orm 图扫描 + service 前向清册 + 豁免机制 + 登记册工件 + 消费协议）
- [x] 相关文档对齐（roadmap M0.2 状态与消费协议、登记册工件、日志）
- [x] 已运行验证：`node tools/check-bigint-id-types.mjs scan`（1586 NEEDS FIX + 8 DEFERRED + md 段 0）+ `node tools/check-bigint-id-types.mjs dry-run` 延后列保持 long + `node tools/verify-id-fix-copy-diff.mjs module-finance` 计数排除延后列（终态复跑全绿 + 负面测试 4 项 + 全 19 域 gate exit 0）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（无范围内降级项。service 级临时桥接代码实现归各早域 plan 为 **D4 既定归属**（见 Non-Goals 与登记册 disposition 的 retireOwner 字段），非本计划范围收缩。）

## Closure

Status Note: 三 Phase 全部完成且经独立结束审计 `passes closure audit`（0 BLOCKER / 0 MAJOR / 1 MINOR 已修正）：orm 级跨域图机器化复扫固化「恰 2 前向簇」结论、service 级前向边全量清册（94 main 编译级 + 30 test 文件）与后向指针 122 条全部登记入册（254 active），两工具豁免机制落地并以 3 项端到端 Proof + 4 项负面测试验证，登记册工件 + roadmap 消费协议 + M1.2 notify 走查 + 日志四者一致。零 Java / 零 orm 源变更（Non-Goal 核验通过），Maven 门控按计划声明不适用。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 fresh session（general，ses_fdb9a38d7ffeV343h80hI006iM，2026-08-21）
- Verdict: `passes closure audit`（0 BLOCKER / 0 MAJOR / 1 MINOR）
- MINOR-1 及处置：roadmap M0.2 状态行曾提前使用「独立结束审计」字样（写入时审计尚未执行）——已按审计建议改述为「独立结束审计 `passes closure audit`（ses_fdb9a38d7ffeV343h80hI006iM，证据见 plan Closure 节）」；审计同时独立复核该行全部数字结论属实。
- Evidence: 审计员独立运行全部验证命令并复核——registry 254 全 active（8/94/30/60/62）；8 条 orm evidence 行号与实况 orm 逐行吻合（含实体归属程序化确认）；scan = 1586 + 8 DEFERRED + md 段 0 + 0 WARN/BLOCKER；dry-run 18 文件/1586 列幂等 + 副本 fin 6/6、hr 2/2 延后保持 long + 208/136 其余翻转；gate module-finance = 208 行排除 6 延后；gate 全 19 域 = 1586 + md NO-COPY-NEEDED + exit 0；loadRegistry 不存在目录 → `[FAIL]` exit 1；两工具代码核验仅消费 active orm-column-deferral；登记册工件 §2 矩阵 19 域 × 6 列程序化对账 all-match；notify 走查 0-0-0-0/28 条/43+28 文件全维度一致；git status 零 orm/Java 变更；反松弛零违禁词。

Follow-up:

- （无；已确认缺陷不得出现在此处）
