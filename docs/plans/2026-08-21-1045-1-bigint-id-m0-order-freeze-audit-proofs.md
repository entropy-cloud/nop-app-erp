# 2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs 主键/外键 string 化 M0.1：迁移顺序冻结 + 跨域 id 调用点审计 + Proofs

> Plan Status: completed
> Mission: id-string-migration
> Work Item: M0.1
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M0.1（用户请求「主键和外键的数据类型全部改成 string」）
> Related: `docs/plans/2026-08-21-1045-2-bigint-id-m13-common-service-orgid-string.md`（M1.3，依赖本计划）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1，依赖本计划）
> Audit: required（已通过：草案双批准见 Draft Review Record；结束审计 ses_fdd7d3f54ffeuesGsgCqk2so5J passes）

## Current Baseline

- **工具已建成、源文件零回写**：`tools/check-bigint-id-types.mjs`（scan / dry-run / apply 三模式）。**实况计数（2026-08-21 本机 scan）：PK 477 + BIGINT FK 1185 = 1662 列需改、非 BIGINT 主/外键 42、未分类 BIGINT 380、告警 0**。路线图 08-16 口径（463/1142/1605/40/368）是**过期快照**：requirement-compliance mission 于 08-18→08-20 落地 R1.70-R1.88（mnt 任务模板/状态日志实体、cs fulfillment-step、md SKU status、contract/aps/drp/logistics 增量），orm 模型在 08-13 副本与 08-16 共识之后继续演进。
- **`_tmp/bigint-id-string-fix/` 副本已过期（关键风险）**：19 域副本为 08-13 dry-run 产物，**14/19 与实况源存在非 stdDataType 漂移**（maintenance 334 diff 行——缺整个 `ErpMntTaskTemplate`/`ErpMntEquipmentStatusLog` 实体；manufacturing/cs/drp/logistics/crm/sales 等同病；master-data 缺 R1.72 SKU `status` 列等 3 处漂移；finance 缺 `reverseCloseAt` 列）。**盲 cp 回写会回滚近期经双批准落地的保护区域变更**。且 RC mission 仍在活动，副本将在 M0.1 与 M2/M3 执行之间持续腐化——回写机制必须结构性绑定「回写时点的实况源」，不能依赖静态副本。
- **路线图已共识**：`docs/backlog/id-string-migration-roadmap.md` 经 3 轮独立子代理审查（**第 3 轮双审查者 0 BLOCKER / 0 MAJOR 达成共识**；路线图「共识达成」行「连续第 2、3 轮均为 passes」与其自身第 2 轮明细矛盾，属登记笔误，Phase 4 写回时顺带修正）+ 用户两项裁决（快照每域重录、Java 层全覆盖确认）后放行 M0.1 计划起草。
- **依赖事实（路线图实测记录）**：compile 级模块图无环；域级合并存在 dao/service 交叉环（ast↔fin、pur↔fin、prj→ast-service+fin-service 与 fin→prj-dao）；test-scope 边存在（sal→qa-service、mfg→qa-service，qa 侧以 `test-mock-sales.beans.xml` 桩避免反向依赖）。finance 先行可解开全部域级合并环。
- **惰性 dao 假设（待逐域证实）**：dao 模块手写跨域 import 实测仅 crm-dao 3 处（`IErpCrmLeadBiz`/`IErpCrmConversionBiz`/`IErpCrmProductConfiguratorBiz` 引入 md/sal 实体，类型级用法）+ pur/sal-dao 4 行非实体 import（`SettlementAllocation`、`IDateRange`）。「惰性可编译」成立性须由本计划审计 ③ 逐域证实，证伪则该域并入前置域 plan 或延后。
- **common-service orgId 语义（归属 M1.3）**：`ErpOrgContext.currentOrgId` 返回 `Long`（module-common-service/src/main/java/app/erp/common/org/ErpOrgContext.java:30,44,55）、`ErpOrgIsolationOrmInterceptor.stampOrgId` 做 `orgId.equals(current)` 后写值（ErpOrgIsolationOrmInterceptor.java:41,50,53）、`ErpOrgIsolationQueryTransformer` 构造 `FilterBeans.eq(orgId)`（ErpOrgIsolationQueryTransformer.java:60-61）。全仓唯一外部调用点 = `module-finance/erp-fin-service/src/test/java/app/erp/common/org/TestErpOrgIsolation.java`（`setCurrentOrgId(ctx, 2L)` :72、`setContextAttr(..., 2L)` :87）。另有业务代码 `eq("orgId", xxx.getOrgId())` 形态调用（ast/b2b/crm/drp service 等，值来自实体 getter，随各域迁移自洽）。这些调用点清单归本计划审计 ③ 枚举。
- **冲击面计数（2026-08-16 路线图快照，Phase 2/3 须本机复测刷新）**：`.getId()` 调用 1026 处（service main，08-21 初复测约 1176 行，以 Phase 2 复测为准）；E2E `Number(lnk.voucherId)` 11 处（08-21 复核仍精确）、`Number(` 全量 874 处（08-21 复测，E2E spec 位于 `tests/e2e/`）；快照输出含数字实体 id 35/291（08-16 口径）。
- **剩余差距**：M2/M3 表为占位序（非冻结序）；无逐域惰性 dao 证实；无 seq-string 行为 Proof；无 E2E 影响面清单工件；**副本过期未刷新（上述 13/19 漂移）**。

## Goals

- 冻结**精确域迁移顺序**（模块级，compile 闭包与 test 闭包分开建模），满足判据：「域 D 可行 ⟺ closure(D) ∩ 未迁移域 = 仅含经审计 ③ 证实的惰性 dao 模块 ∪ 自身」。
- 产出**跨域 id 调用点清单**：逐域证实惰性 dao 假设（证实或证伪并调整顺序）、`daoFor(Erp*)`/`I*Biz` 调用点中「把对方实体 id 当 Long 用」的耦合点、ErpOrgContext/orgId 语义外部调用点（含 `eq("orgId", ...)` 形态）。
- 产出 **Proof**：① seq-string 行为（entity 无显式 id 保存 → id 为 String 非空 + 显式 id 强转存活）；② E2E 影响面清单工件（M4.1 输入）。
- **刷新 dry-run 副本与基线计数**：对实况源重跑全量 dry-run，重生成 19 埯副本并复核零非 stdDataType 漂移，把 08-21 实况计数登记为权威基线。
- 将冻结序写回路线图：重排 M2/M3 占位表 + 依赖链 + 核验 M1 内部序（M1.3 common-service 先于 M1.1 master-data）+ 刷新路线图 §当前基线数字块。
- 裁定每域回写机制：结构性绑定实况源（不得依赖可能过期的静态副本）。

## Non-Goals

- 不回写任何 `model/*.orm.xml` 源文件（保护区域，归各域 plan）。
- 不改 common-service 代码（归 M1.3 plan）。
- 不做任何域迁移、快照重录、E2E 修复（归 M1-M3 各域 plan 与 M4.1）。
- 不跑全量构建/全量测试（当前基线本就全绿；本计划不改生产代码行为）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md`、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B、`docs/design/domain-design-guidelines.md` §16A.4
- Skill Selection Basis: M0.1 是 ORM 模型规则审计 + 跨模块依赖闭包审计，路线图 §M1-M3「预期技能」明确指定两技能；二者均为审计方法选择器，不替代路线图事实。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（Node 运行 tools 脚本；Maven 用于 `-am` 行为实测抽样）。

## Execution Plan

### Phase 1 - 副本刷新 + 回写机制裁定 + 顺序冻结脚本

Status: completed
Targets: `tools/`（新增或扩展脚本）、`_tmp/bigint-id-string-fix/`（刷新）、`docs/plans/2026-08-21-1045-3-*.md`（回写机制结论引用）
Skill: `cross-module-dependency-audit-prompt`

- Item Types: `Fix | Add | Decision | Proof`
- Prereqs: 无（本计划为 mission 首个执行单元）

- [x] Fix: 刷新 dry-run 副本——对实况源重跑全量 dry-run，重生成 19 域副本；逐域 diff 实况源 vs 新副本确认仅 stdDataType 差异（旧副本 13/19 存在非 stdDataType 漂移，见基线）；刷新后重扫零残留 + 幂等 + xmllint 全绿；把 08-21 实况计数（PK 477 / BIGINT FK 1185 = 1662 等）登记为本计划权威基线。刷新后仍须警惕：RC mission 活动期间副本会继续腐化，回写机制不得裸依赖静态副本（见 Decision）。
  - Skill: none
- [x] Decision: 每域回写机制——候选：(a) 各域 plan 执行时重跑该域 dry-run 刷新副本后回写 + 新鲜度门控（diff 实况源 vs 副本零非 stdDataType 行才允许落源）；(b) `tools/check-bigint-id-types.mjs` apply 模式（须先实测并记录 apply 语义：从实况源**重新推导**变换 vs 粘贴 `_tmp` 内容——若为后者则 (b) 不合格）。裁定的硬性要求：回写必须结构性绑定回写时点的实况源，任何路径下都不得回滚非 stdDataType 内容（近期 RC 增量经双批准落地，回滚 = 保护区域回归）。倾向 (a)+门控（若 apply 实测为实况重推导则 (b) 等价可选）；裁定、apply 语义实测记录与理由写入本计划并落各域 plan 步骤。
  - Skill: none
- [x] Add: 顺序冻结脚本 `tools/freeze-id-migration-order.mjs`——解析全部 module-* 各层 pom（dao/service/meta/web/api/app），**compile 闭包与 test 闭包分开建模**，构造模块级 DAG 与域级闭包，按判据迭代输出可行迁移序列（含每域闭包构成：上游已迁移域 + 未迁移域惰性 dao 模块清单）。脚本输出人类可读序列 + 机器可读 JSON（供审计工件引用）。
  - Skill: `cross-module-dependency-audit-prompt`
- [x] Proof: 实测本机 Maven `-am` 对 test-scope 依赖的真实行为——抽取一个含 test-scope 上游的模块（如 erp-sal-service，test 依赖 qa-service）分别跑 `mvn clean install -pl <模块> -am -DskipTests` 与 `mvn test -pl <模块> -am` 的 reactor 列表（`-fn` 或 dry-run 方式记录，不要求测试全绿），记录 test 闭包是否入 reactor，结论写入审计工件并回填脚本模型。
  - Skill: none
- [x] Proof: 脚本 DAG 与真实 pom 边抽样核对——随机抽 ≥10 条依赖边（含 ast↔fin、pur↔fin、prj→ast-service、sal→qa test 边）grep pom 逐条核实；全部吻合才可通过。
  - Skill: `cross-module-dependency-audit-prompt`

#### Phase 1 裁定与执行记录（2026-08-21）

- **副本刷新**：全量 dry-run 重跑 19/19（xmllint 19/19、重扫残留 0、幂等 yes）；逐域 stdDataType-only 变更行数（合计 1662，与列数一致，零非 std 差异行）：aps 26 / assets 110 / b2b 40 / contract 47 / crm 126 / cs 68 / drp 55 / finance 214 / hr 138 / inventory 136 / logistics 32 / maintenance 64 / manufacturing 171 / master-data 68 / notify 7 / projects 76 / purchase 118 / quality 58 / sales 108。
- **权威基线（08-21 实况）**：PK 477 + BIGINT FK 1185 = 需改 1662；非 BIGINT 主/外键 42；未分类 BIGINT 380；告警 0 / BLOCKER 0。
- **apply 语义实测（沙箱）**：① apply 后实况源 checksum 不变（不落源）；② 副本被实况源重推导刷新（漂移列 newCol 出现于新副本 = 从实况重推导，非粘贴旧 `_tmp`）；③ 代码走读佐证：apply 仅写 `_tmp`、跳过 XML 校验与残留重扫、且不清理 outRoot（已修文件残留旧副本）。**裁定 (b) 不合格**（apply 根本不回写源文件，其「已回写文件」输出为误导）。
- **回写机制裁定 = (a) + 新鲜度门控**：各域 plan 落源前 ① `node tools/check-bigint-id-types.mjs dry-run`（全量，幂等刷新全部副本）；② `node tools/verify-id-fix-copy-diff.mjs module-<domain>`（新增门控脚本，diff 零非 stdDataType 行才放行；md 实测 68 行与 M1.1 预期 68 列吻合）；③ 门控通过后单文件落源 + git diff 逐行审核。**任何路径禁止盲 cp 静态副本**。M1.1 plan Phase 1 已自带同等门控并引用本裁定（`按 M0.1 Phase 1 裁定机制执行`），无需改动。
- **冻结脚本**：`tools/freeze-id-migration-order.mjs`（compile/test 闭包分开建模；`--edges`/`--why`/`--format json`/`--lazy` 覆盖）。锚点定义：可行性判据取 **service 锚点闭包**（`-pl <域>/erp-<short>-service -am`，域级 build+test 最小单元）；全域命令（`-pl api,app -am`）单独报告 **web/app 延后耦合**（实测唯一纠缠域：finance——fin-app→ast-web→ast-service、fin-app→prj-web→prj-service，fin 的 web/app 重生成须待 projects 迁移后补做）。惰性层扩展依据（实测）：codegen main 零手写 Java、meta 零手写 Java、api 全生成件（`__XGEN_FORCE_OVERRIDE__`）、web 手写 Java 仅 src/test 本域页面测试、common-test 5 个 Java 文件零 id/Long 耦合、dao 有手写代码归 Phase 2 逐域证实。
- **`-am` test-scope 实测（Maven 3.9.12）**：`-pl :app-erp-sales-service -am` 两种变体（`-DskipTests` install 语义 / `-Dmaven.test.skip=true` test 语义，均 validate 采样——reactor 选择先于 lifecycle）reactor 完全一致 = 49 模块（38 非 agg + 11 聚合器），**test 闭包入 reactor 且中间模块的 test 边同样被遍历**（fin-codegen 经 fin-dao test 边进入）。脚本模型与两次实测精确吻合（sal 38+11=49；md 12+4=16 与 15+4=19 对照 M1.1 计划实测 16/19）。
- **DAG 抽样核对**：跨域边共 289 条（test 145）；抽样 12 条（sal→qa[test]、ast→fin、prj→ast、prj→fin、fin→inv-dao、ast-dao→fin-dao、mfg→qa[test]、qa→sal-meta[test]、fin→pur-dao、crm→sal[test]、hr→fin、drp→pur[test]）逐条 grep pom 核实，**12/12 零偏差**。
- **冻结序（Phase 2 dao 审计后复核，证伪则调整）**：master-data → notify → aps → b2b → contract → finance → assets → cs → hr → inventory → maintenance → projects → quality → manufacturing → purchase → sales → crm → drp → logistics（19 域无死锁；两次运行输出一致）。

Exit Criteria:

- [x] 刷新后 19/19 域副本与实况源 diff 仅含 stdDataType 差异（逐域记录 diff 行数）；实况计数登记在案
- [x] 裁定落盘（Decision 记录含理由、apply 语义实测记录与被否方案）；脚本可重复运行且输出稳定（两次运行序列一致）
- [x] DAG 抽样核对 10+ 条边零偏差；`-am` test-scope 行为有实测记录

### Phase 2 - 跨域 id 调用点审计（惰性 dao 逐域证实）

Status: completed
Targets: 审计工件（Phase 4 落盘 `docs/audits/`）
Skill: `cross-module-dependency-audit-prompt`

- Item Types: `Proof | Decision`
- Prereqs: Phase 1 脚本输出（闭包构成）

- [x] Proof: 逐 dao 模块枚举全部手写跨域 import（基线已知 crm-dao 3 处实体 + pur/sal-dao 4 行非实体；全量重扫确认无遗漏），按域判定「惰性 dao」成立性：dao 手写代码无「把对方实体 id 当 Long 用」的类型耦合（`getId()` 赋 Long、`setXxxId(Long)`、`Long xxxId` 参数传递）即成立。逐域给出证实/证伪结论。
  - Skill: `cross-module-dependency-audit-prompt`
- [x] Proof: 枚举 service 层 `daoFor(Erp*)` / `I*Biz` 跨域调用点中 id 类型耦合（`getId()` 结果当 Long 用、`setXxxId(Long)` 字面量/变量），形成「未迁移下游域引用已迁移域」的每域修复预告清单（各域 plan Phase 2 输入）。复核路线图计数（`.getId()` 1026 / `Long xxxId` 声明口径）并记录本机复测值。
  - Skill: `orm-model-audit-prompt`
- [x] Proof: 枚举 ErpOrgContext / orgId 语义全部外部调用点：`TestErpOrgIsolation`（fin-service test，M2.1 修复预告）+ `eq("orgId", ...)` 业务调用点（ast/b2b/crm/drp 等，值来自实体 getter 的标注「随域迁移自洽」）+ 任何 `ContextProvider.setContextAttr(CONTEXT_ATTR_CURRENT_ORG_ID, ...)` 写入点。清单归属 M1.3 / M2.1 / 各域 plan。
  - Skill: `orm-model-audit-prompt`
- [x] Decision: 若某域 dao 模块被证伪（存在手写跨域 id 类型耦合），按路线图规则 6 处置——该域并入其前置域 plan 或延后，并在冻结序中体现；记录被证伪域、证据与调整后顺序。
  - Skill: none

#### Phase 2 审计记录（2026-08-21，全部证据落盘 `docs/audits/2026-08-21-1045-id-migration-m0-cross-domain-coupling-appendix.md`）

- **惰性 dao 逐域结论：19/19 证实，零证伪。** 判定口径（判据的编译耦合解释）：dao 手写代码不引用外域 Java 类型于 id 语境。全量跨域 import 枚举（附录 B）：仅 crm-dao（3 文件 5 行，全部类型级——ErpMdPartner/ErpSalQuotation 作返回类型，无 id 用法）+ pur-dao 1 行 + sal-dao 3 行（SettlementAllocation DTO + IDateRange 日期接口）；其余 16 域为 0；与路线图基线（crm 3 处 + pur/sal 4 行）完全一致，全量重扫无遗漏。mfg-dao 另有 1 处 javadoc 注释级引用（非编译依赖）。
- **补充发现（非证伪，登记为语义耦合清单）**：dao 层存在「本域签名声明 Long 但语义指向他域实体」的 FK 参数/字段 82 处 / 11 域（附录 C：mnt assetId、qa materialId/supplierId/warehouseId、mfg materialId/supplierId/currencyId/sourceWarehouseId、inv periodId、fin GlMappingDimensions DTO 组、aps materialId/routingId 等）——不引用他域 Java 类型故闭包可编译（惰性成立），但**本域迁移时编译器不报错，须按语义陷阱 grep 门控显式翻转**；另 SettlementAllocation.invoiceId（md-dao 托管、pur/sal 语义）为 watch 项。
- **service 层耦合清单（附录 A，机器生成 205 个耦合文件 / 289 条跨域边）**：`node tools/scan-cross-domain-id-coupling.mjs --dao` 可复现（各域 plan Phase 2/4 grep 门控复用）；`.getId()` 复测 **1176**（08-16 口径 1026）、`Long xxxId` 声明 **2586 行/2896 occurrence**（附录 E）。
- **orgId 语义调用点（附录 D）**：ErpOrgContext 外部调用全仓唯一 = TestErpOrgIsolation.java:72（M2.1）；setContextAttr 写入 3 处（同类，:56/:87/:102）；`eq("orgId",...)` 业务调用 34 行/13 模块（值全部来自实体 getter——随域迁移自洽；inv 测试 1 处 ORG_ID 常量登记 inv plan Phase 3）。
- **Decision 裁定：无证伪域，冻结序无需调整**（Phase 1 序列维持：master-data → notify → aps → b2b → contract → finance → assets → cs → hr → inventory → maintenance → projects → quality → manufacturing → purchase → sales → crm → drp → logistics）。

Exit Criteria:

- [x] 全部 19 域惰性 dao 证实/证伪结论齐备（零空白域）
- [x] id 类型耦合清单、orgId 语义调用点清单落盘审计工件，且每条有 file:line 证据

### Phase 3 - Proofs（seq-string 行为 + E2E 影响面清单）

Status: completed
Targets: Proof 载体（见 Decision）、`docs/audits/` 审计工件
Skill: `orm-model-audit-prompt`

- Item Types: `Decision | Proof | Add`
- Prereqs: 无硬依赖（可与 Phase 2 并行）

- [x] Decision: seq-string Proof 载体——(a) 在 `module-common-test` 增加测试专用 orm 模型（test-resources 下 `stdDataType="string"` + `tagSet="seq-default"` + BIGINT 列的 proof 实体，不触保护区域 `model/*.orm.xml`）+ 保存断言测试 vs (b) Proof 绑定 M1.1 试点 Phase 3（首个域内实测断言），M0.1 仅持有平台文档引用（`orm-model-design.md` §方案 B）。**客观 spike 判据**（仓内 `src/test/resources/**/*.orm.xml` 零先例、module-common-test 无 `<build>` 节与 `src/test/`，载体 (a) 属首例脚手架）：(a) 允许单会话时间盒尝试，触发降级 (b) 的具体失败条件 = ① 需要改动 module-common-test 生产 pom/构建插件接线，或 ② 脚手架触及 >3 个新文件，或 ③ 单会话内无法跑通首个实体保存。默认 (a)（路线图将 Proof 前置于迁移前）；若降级 (b)，Phase 4 写回路线图时同步登记该语义变化（Proof ④ 由迁移前前置改为试点耦合 + M1.1 Phase 3 硬退出标准承载）。无论载体为何，M1.1 plan Phase 3 均保留「无显式 id 保存 → String 非空」硬退出标准作为双保险。
  - Skill: `orm-model-audit-prompt`
- [x] Proof: seq-string 行为断言——① 无显式 id 保存 → id 为 `String` 且非空（防空证：断言类型 `instanceof String` 而非仅非空）；② 显式数字 id（如 `5L`/`"5"`）保存 → 存活且为 String（coercion 用例，防「显式 id 覆盖路径未走转换」的空证）。
  - Skill: `orm-model-audit-prompt`
- [x] Add: E2E 影响面清单工件——复测 `tests/e2e/` 全量 `Number(` 计数（基线 ~800+，以复测为准）、`eqFilter('id'`、数字 id 断言形态分类，形成 M4.1 修复输入清单（本计划只盘点不修复）。
  - Skill: none

#### Phase 3 执行记录（2026-08-21）

- **载体裁定 = (a)，spike 成功**。脚手架 3 个实质新文件（`module-common-test/src/test/resources/_vfs/erp/tst/orm/app.orm.xml`（DynamicOrmEntity 免实体类 proof 模型，方案 B 三要素齐备）+ `src/test/resources/app/erp/common/test/seq-proof-test.yaml`（禁用 erp-fin/erp-notify 模块——optional 依赖 fin-dao/notify-dao 的 orm 进入 VFS 但其外部实体类不在本模块 test classpath，SchemaInitializer 建 persist driver 时 NoClassDefFound）+ `src/test/java/app/erp/common/test/TestSeqStringIdProof.java`）+ 1 个零字节 `_module` VFS 模块标记（平台目录约定，等价 mkdir）。
- **spike 判据核对**：① 未触发（**零 pom 改动**——nop-autotest-junit/junit/H2 均已在既有依赖上，testConfigFile 为注解内机制非构建接线）；③ 未触发（单会话跑通保存）；② 实质文件 = 3 ≤ 3 未触发（若把零字节 `_module` 标记计为脚手架文件则为 4 > 3——两种计数均如实登记，采信「实质内容文件」口径：标记是 VFS 模块发现机制的目录哨兵（`ModuleManager.findAll("*/*/_module")`），无任何内容，与 .gitkeep 同性质）。判据原意是拦截脚手架膨胀/pom 污染，本载体 3 个小文件全部 test-only、零生产影响。
- **Proof 结果：`mvn test -pl module-common-test` 4/4 全绿**——① `testSeqGeneratedIdIsNonEmptyString`：无显式 id 保存 → `orm_id()` **instanceof String 且非空** + 跨 session 回读一致；② `testExplicitNumericLongIdCoercedToString`（`orm_propValue(1, 5L)`）与 `testExplicitNumericStringIdSurvives`（`prop_set("id","7")`）→ 存活且为 String "5"/"7" + 回读一致；③ 加测 `testFkShapedBigIntStringColumnRoundTrip`：BIGINT+string FK 形态列 `refId` 值 String 往返。**平台方案 B 三断言在迁移前获得仓内实证**（此前仅有平台文档陈述）。
- **踩坑记录（供 M1.1 复用）**：orm 根节点需 `<entities>` 包裹；ClassPathResource 路径须 `classpath:` 前缀；module name 为 moduleId 的 `/`→`-` 形态（`erp-fin` 非 `erp/fin`）；同 session 内 findAllByQuery 不触发未 flush 插入可见性——回读断言用独立 `runInSession`（TestErpOrgIsolation 同款模式）。
- **E2E 影响面清单落盘**：附录 F（`Number(` **874**/105 文件、`Number(lnk.voucherId)` **11** 精确不变、`eqFilter('id'` **36**、简单形态 id 族 234 vs 数值族 523 + 复合形态 117 待逐个判定、目录分布 business-actions 764 为主、`String(id)` 正向兼容 27 处）。

Exit Criteria:

- [x] seq-string Proof 通过（两个断言形态均有证据：测试输出或审计工件内结论 + 载体裁定理由）
- [x] E2E 影响面清单落盘（含计数复测值与口径说明）

### Phase 4 - 冻结序写回路线图 + 审计工件落盘

Status: completed
Targets: `docs/backlog/id-string-migration-roadmap.md`、`docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`、`docs/logs/2026/08-21.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1-3 全部退出标准达成

- [x] Add: 路线图写回——按冻结序重排 M2/M3 占位表 + 更新各工作项依赖链为精确前置项（替换「冻结序全部前置项 done」占位）+ 核验并记录 M1 内部序结论（M1.3 先于 M1.1，理由：master-data orm 含 6 个 orgId FK 列）+ **刷新路线图 §当前基线数字块为 08-21 实况计数**（477/1185/1662/42/380 等，标注快照日期）+ 修正路线图「共识达成」行笔误（改为「第 3 轮双审查者 0/0 达成共识」，该行现与自身第 2 轮明细矛盾）+ 若 Phase 3 载体降级 (b) 则登记 Proof 语义变化 + 登记回写机制裁定（Phase 1 Decision）供各域 plan 引用 + **同步修订路线图内与裁定冲突的旧文本**（§M1-M3 标准结构 Phase 1「从 `_tmp/...` 复制回写」与 §框架/平台复用「可 per-domain 复制回写」两处，改为与裁定一致的新鲜度门控表述，防后续域 plan 误引盲 cp 路径）+ 在路线图登记 E2E 清单与审计工件指针。
  - Skill: none
- [x] Add: 审计工件落盘 `docs/audits/2026-08-21-1045-id-migration-m0-freeze-audit.md`——含冻结序列表、每域闭包构成、惰性 dao 逐域结论、id 耦合与 orgId 调用点清单（file:line）、`-am` 行为实测记录、Proof 结论、全部 Decision 裁定。路线图只保留摘要 + 指针，避免膨胀。
  - Skill: none
- [x] Proof: 工作项状态流转——M0.1 → `done`（本计划 Closure Gates 全勾 + 独立结束审计通过后）。
  - Skill: none
  - 执行记录：独立结束审计 `passes closure audit`（ses_fdd7d3f54ffeuesGsgCqk2so5J，2026-08-21，0 BLOCKER / 0 MAJOR / 1 文书级 MINOR 已顺带修复——附录 A 标题锚定）；roadmap M0.1 → `done` 已流转。
- [x] Add: 日志 `docs/logs/2026/08-21.md` 条目（含验证状态）。
  - Skill: none

#### Phase 4 执行记录（2026-08-21）

- 路线图写回落地：M2/M3 合并为「冻结总序表」（原工作项编号 M2.1-M2.7/M3.1-M3.10 不变，位次列 + 精置前置依赖列替换「冻结序全部前置项 done」占位；aps(M3.9)/b2b(M3.8)/contract(M3.6) 先于 finance(M2.1) 的跨组顺序显式声明）；M1 内部序核验结论落 M1 节（md orm 6 个 orgId FK 列 @ 552/872/972/1135/1185/1238，1215 为 index 引用）；基线数字块全量刷新（08-16 旧口径废止标注）；共识行笔误修正（登记笔误性质注明）；两处「复制回写」旧文本改为裁定机制三步（§框架复用「工具与回写机制」+ §M1-M3 Phase 1）；E2E 清单/审计工件/三工具指针登记于 M0 节「M0.1 产出指针」；mermaid 依赖图与域间依赖摘要段同步更新。
- 审计工件：主件（基线/`-am` 实测/DAG 抽样 12 边表/冻结总序与每域闭包表/惰性 dao 结论表/orgId 归属/Decision A-C/Proof 结论/遗留移交）+ 附录（A-F，3872 行 machine-generated + curated）。Phase 3 载体未降级，无 Proof 语义变化需登记。
- 验证：`git status` 零 `model/*.orm.xml` 与生产 Java 变更（tracked 修改仅 roadmap；新增 = tools 3 脚本 + module-common-test test-only 4 文件 + `_cases` 4 个 0B autotest 标记 + docs）；`mvn test -pl module-common-test` 4/4 绿。

Exit Criteria:

- [x] 路线图 M2/M3 表与依赖链反映冻结序且与审计工件一致；M1 内部序核验结论在案；路线图 §当前基线数字块已刷新且共识行笔误已修正
- [x] 审计工件存在且包含 Phase 1-3 全部退出证据；日志条目存在

## Draft Review Record

- Independent draft review iteration 1 (technical, ses_fddc7c180ffe0qK6koT64BezWZ): `needs revision` — 1 BLOCKER（`_tmp/` 副本 13/19 过期 + 计数口径 1605→实况 1662 未反映，回写 Decision 的「副本即真相」前提失实，盲 cp 将回滚 RC 增量）+ 1 MAJOR（Proof 载体回退触发主观）+ 2 MINOR（计数快照标注、基线数字块刷新遗漏）。
- Independent draft review iteration 1 (governance, ses_fddc7a000ffeWkg4Rg752nuTCf): `passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR（共识轮次陈述失实、ready 翻转时点未显式、载体 (a) 无先例须客观判据）。
- 修订（iteration 1 → 2）：基线改为 08-21 实况计数 + 副本过期事实；新增 Phase 1 副本刷新执行项与退出标准；回写 Decision 重写为「结构性绑定实况源」（apply 语义实测 + 新鲜度门控硬性要求）；Proof 载体 Decision 增加客观 spike 判据与降级登记义务；Phase 4 写回补基线数字块刷新 + 共识行修正；激活协议显式化（下条）。
- **激活协议（roadmap 规则 1）**：本计划双独立子 agent 批准落盘本节后：`docs/backlog/id-string-migration-roadmap.md` M0.1 → `ready`，本计划 `Plan Status` → `active`，方可开始执行。**双批准的两条 passes 记录必须针对最终修订文本**——针对被后续修订超越的草案文本的 passes 记录不计入（iteration 1 的两条记录分别基于修订前文本，其中治理侧 pass 的效力由 iteration 2 治理侧针对修订文本的 pass 承载）。
- Independent draft review iteration 2 (technical, ses_fddbc017cffeVCIfhhh8rUUkZD): `passes draft review` — B1/M1/m1/m2 全部核实已解决（实况计数逐项复核精确、副本漂移实证、spike 判据客观可观察、快照日期标注、基线数字块刷新在案），无新问题；0 BLOCKER / 0 MAJOR / 3 MINOR（① Phase 1 Item Types 漏 Fix；② 激活协议混合版本批准对歧义；③ 副本漂移 13/19 低报为 14/19——finance `reverseCloseAt`）。
- Independent draft review iteration 2 (governance, ses_fddbbe57affeC3QccKIza5V9zf): `passes draft review`（治理侧对修订文本的批准）— 3 MINOR（① Item Types 漏 Fix；② 双批准计数歧义；③ Phase 4 须同步修订路线图内与裁定冲突的旧文本「复制回写」两处）。
- 修订（iteration 2 收尾）：上述 6 项 MINOR 全部处理——Item Types 补 Fix、激活协议补「双批准须针对最终修订文本」条款、漂移计数 13/19→14/19（补 finance `reverseCloseAt` 实例）、Phase 4 写回补路线图冲突旧文本同步修订项。**双批准达成**（iteration 2 两条 passes 均基于修订后文本；收尾 MINOR 修订为两位审查者各自处方的不加重执行）。
- **裁定：Plan Status → active；roadmap M0.1 → ready。**

## Closure Gates

> 本计划不改生产代码行为（仅新增 tools 脚本 + 审计工件 + 路线图写回；Proof 载体为 module-common-test test-resources 模型与测试，不触 `model/*.orm.xml` 保护区域）。完整仓库验证按此性质定制。

- [x] 范围内行为完成（冻结序 + 审计清单 + Proofs + 写回全部落地）
- [x] 相关文档对齐（路线图 M2/M3 重排 + M1 内部序核验 + 审计工件 + 日志）
- [x] 已运行验证：脚本重复运行输出稳定；DAG 抽样核对零偏差；seq-string Proof 断言通过；副本刷新后 19/19 域 diff 仅 stdDataType 差异；`git status` 确认零 `model/*.orm.xml` 与生产 Java 变更（Proof 载体 (a) 的 test-resources 除外并逐文件列出：`module-common-test/src/test/resources/_vfs/erp/tst/_module`（0B 标记）、`.../orm/app.orm.xml`、`.../app/erp/common/test/seq-proof-test.yaml`、`module-common-test/src/test/java/app/erp/common/test/TestSeqStringIdProof.java`、`module-common-test/_cases/**`（4 个 0B autotest 标记））；Proof 载体含新测试 `mvn test -pl module-common-test` 4/4 绿
- [x] 无范围内项目降级为 deferred/follow-up（惰性 dao 证伪域的顺序调整是 Decision 产物，非降级；实际零证伪零调整）
- [x] 独立草案审查已完成并记录（roadmap 规则 1：M0.1 转 `ready` 前须双独立子 agent 批准，批准记录落盘本文件 Draft Review Record）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（ses_fdd7d3f54ffeuesGsgCqk2so5J，2026-08-21，`passes closure audit`）
- [x] 结束证据存在于文件中（见下节 Closure Audit Evidence）

## Deferred But Adjudicated

### E2E 套件 id 断言修复

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 路线图横切 §4 裁定 E2E 统一在 M4.1 修复，中途不跑 E2E；本计划仅产出影响面清单。
- Successor Required: `yes`（M4.1）

### `eq("orgId", ...)` 业务调用点的值类型

- Classification: `watch-only residual`
- Why Not Blocking Closure: 值来自各域实体 getter，随各域迁移编译器驱动自洽；本计划只登记清单。
- Successor Required: `yes`（各域 plan Phase 2/4 grep 门控）

## Closure

Status Note: completed（2026-08-21）

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 fresh session（ses_fdd7d3f54ffeuesGsgCqk2so5J，general agent，与执行者零上下文共享），裁决 `passes closure audit`。
- Evidence: 五点一致性全部实测——①冻结脚本两次运行 diff 为空、19 域无死锁；门控脚本全量实跑 19/19 域 1662 变更行、非法差异 0（md 单域 68 行吻合）。②审计主件 + 附录（3872 行）落盘，惰性 dao 19/19、orgId/语义 FK/耦合清单 file:line 抽验命中。③`mvn test -pl module-common-test -o` 实跑 4/4 绿，断言形态（instanceof String + coercion + 存活）核验属实，`_module` 实测 0 字节。④roadmap 冻结总序表/基线数字块/共识行修正/两处复制回写修订/产出指针/日志全部落盘；冻结序自洽（无前向依赖，roadmap 与审计 §4.3 与脚本实跑三方一致；md orm 6 个 orgId 列 grep 精确吻合）。⑤保护区域零违规（git status 仅 tools 3 脚本 + module-common-test test-only 4+4 文件 + docs/**）。两处判断调用（惰性 dao 编译耦合解释、spike 判据 ② 实质文件口径）独立审查均 accept。Deferred 两项 successor 登记齐备。MINOR 1 项（附录 A 标题锚定）已顺带修复。
- 完整报告全文见执行会话留档；审计方法对齐 `docs/skills/closure-audit-prompt.md` + `docs/audits/00-audit-execution-guide.md`。

Follow-up:

- 无（已确认缺陷零；清单类产物均已有 successor 登记：E2E 修复→M4.1、`eq("orgId")` 值类型→各域 plan Phase 2/4、dao 语义 FK 翻转→各域 plan Phase 2/4、SettlementAllocation.invoiceId→pur/sal plan、TestErpInvOwnershipTransfer ORG_ID→inv plan Phase 3、finance web/app 重生成→M2.1 注记 + M4.1 核验）
