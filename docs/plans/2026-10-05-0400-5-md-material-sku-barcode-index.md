# 2026-10-05-0400-5 ErpMdMaterialSku.barcode 索引补齐（orm.xml 保护区）

> Plan Status: completed
> Last Reviewed: 2026-10-05
> Source: docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md §3.2-F7 + §4 批次 5
> Related: 2026-09-28-0852-2（F1/F2 索引批先例：双批准流程 + deploy SQL 派生同步 + F-B3 雷区登记）；USC-05 plan 2026-10-01-0930-1（热路径接入源）
> Audit: required
> Protected Zone Note: `model/*.orm.xml` = auto + dual-agent-approval（两个独立子 agent 分别检查批准，批准记录落盘本计划）

## Current Baseline

- `module-master-data/model/app-erp-master-data.orm.xml` ErpMdMaterialSku 实体（`erp_md_material_sku` 表）：`barcode` 列（:381，VARCHAR 50，可空）**无任何索引/唯一键**；实体现有索引仅 `IDX_MD_MATERIAL_SKU_MATERIAL_ID`/`UOM_ID`/`TAX_RATE_ID`（:411-418）；实体启用逻辑删除（:371-374，`useLogicalDelete="true"`）；无 unique-keys。
- USC-05 将条码解析接入**默认开启**热路径：`ErpInvConstants.DEFAULT_BARCODE_ENABLED=true`（:170-171）；`ErpInvBarcodeBizModel.resolveSku`（:117-120）以仅 barcode 过滤的 example 做 `findFirstByExample`（每次扫码第一跳，无索引时全表扫）；md 域 `ErpMdMaterialSkuBizModel.findSkuByBarcode` 同列共用；应用层唯一性门控 `sku-barcode-unique` 默认 TRUE（`ErpMdMaterialSkuBizModel.java:478` 实核）。
- 软删+UK 先例事实（草案审查 round1 MAJOR-1 实证）：同文件中「软删 + DB 唯一键」为既定模式，大量存在——ErpMdMaterial（:191+:248-250 UK_MD_MATERIAL_CODE）、ErpMdMaterialCustoms、ErpMdMaterialCategory、ErpMdPartner、ErpMdWarehouse、ErpMdLocation、ErpMdUom、ErpMdCurrency。DB 级唯一键与软删的交互在本文件有成熟先例，**不是**未裁决风险面。
- DB 级唯一键归属：md 域 Deferred G1 既有 successor（`ErpMdMaterialSkuBizModel.java:55-56` javadoc 自认），触发条件不变。
- 先例流程：第一轮 F1/F2 索引批（plan 2026-09-28-0852-2）= 保护区双独立子 agent GRANT → orm.xml +index → 重生成 → 三方言 deploy SQL 派生同步（逐字同构裸 `CREATE INDEX`，无 IF NOT EXISTS——Oracle 不支持）。**先例 F-B3 Decision 雷区：禁止全量运行 `scripts/add-orm-indexes.js`**（全量重生成会剥离策略外手工索引；`deploy/sql/*/​_create_index.sql` 头注 "Regenerate: node scripts/add-orm-indexes.js" 即雷区入口）。
- deploy SQL 实际路径：`module-master-data/deploy/sql/{mysql,postgresql,oracle}/_create_index.sql` 三文件实存；H2/运行时索引经 ORM 模型启动期 AddIndex 生效。

## Goals

- `erp_md_material_sku.barcode` 获得查询索引，扫码热路径第一跳由全表扫变索引点查。
- 零行为变更：不加唯一约束（唯一性维持应用层门控 + Deferred G1 successor 不变），不改任何查询语义。

## Non-Goals

- DB 级唯一键（Deferred G1 维持 successor；本计划不重开其软删交互与存量数据核查义务）。
- barcode 列 mandatory 化或其他列语义变更。
- 其他实体的索引全面复查（第一轮已做过 19/19 orm.xml 索引健康审计，无新缺口报告）。
- 全量重跑 `scripts/add-orm-indexes.js`（F-B3 雷区，禁止）。

## Task Route

- Type: `implementation-only change`（保护区索引添加，先例流程复用）
- Owner Docs: `docs/design/inventory/barcode-integration.md`（热路径契约）、`docs/design/master-data/`（SKU 模型语义）、第一轮分析报告 §2.3（索引基线）+ 本轮 §3.2-F7（触发强化证据）
- Skill Selection Basis: 先例流程复用（2026-09-28-0852-2 含 F-B3 雷区登记）；`development-wisdom-gate`（自检）

## Infrastructure And Config Prereqs

- 重生成：`mvn clean install -DskipTests`（触发 gen-orm 增量链；不重跑 nop-cli gen）。
- deploy SQL 派生物：`module-master-data/deploy/sql/{mysql,postgresql,oracle}/_create_index.sql` 逐方言追加（先例语法：逐字同构裸 `CREATE INDEX`）。

## Execution Plan

### Phase 1 - 保护区双独立子 agent 批准

Status: completed
Targets: 本计划（批准记录落盘）
Skill: none（保护区流程）

- Item Types: `Decision | Proof`

- [x] Decision: 索引形态裁决——**普通非唯一索引**（`IDX_MD_MATERIAL_SKU_BARCODE`，unique="false"）。真实理由（round1 MAJOR-1 修正后）：(1) 本计划目标=点查性能，单列过滤谓词下非唯一索引即达成；(2) DB 唯一键已归 md 域 Deferred G1 既有 successor，唯一性当前由默认开启的应用层门控承载（`ErpMdMaterialSkuBizModel.java:478` 默认 TRUE），本计划不重开 G1 的软删交互与存量核查义务；(3) 「软删+UK 为本文件既定模式」如实登记（ErpMdMaterial 等先例），G1 重开时按先例模式办理即可，不构成本计划的拒绝理由。替代方案记录：unique-key（拒绝：归 G1 范围，本计划不加行为约束）、复合索引（拒绝：无第二过滤列）。
- [x] Proof: 双独立子 agent 批准达成（2026-10-05，fresh session 互不共享）：
  - **子 agent A（agent_b1ef42c1，plan-audit 标准）：GRANT** 附 3 条非阻塞条件——①deploy SQL 同步口径=追加 +1 行三方言互 diff（master-data 存在既有 37 模型 vs 32 SQL 漂移属 F-B3 类，非本计划引入不扩表）；②三方言头注计数 32→33 同步；③G1 注记落当日日志+Closure 具名（不回改历史计划）。设计证据 15 项逐一实核通过；形态裁决（拒绝 unique-key=范围归属非「无先例」）成立。
  - **子 agent B（agent_dc476935，独立复核）：GRANT** 附 2 条非阻塞条件——①头注计数同步（同 A②）；②Phase 3 文档回填补第三条 barcode 热路径受益注记（**新发现**：`ErpInvStockTakeLineBizModel.scanSave` PDA 落行同以仅 barcode example 查询——同受益零受损，plan 2026-10-05-0400-2 交付）。DENY 检查无红线命中（纯加性 DDL/零行为变更/程序合规）。
  - 条件落实：①②随 Phase 2 执行；③随 Phase 3 执行。

Exit Criteria:

- [x] Decision 落盘（形态+替代+G1 边界+软删先例事实）
- [x] 双批准记录落盘（两 agent 指针+结论+日期）

### Phase 2 - 索引落地 + 重生成

Status: completed
Targets: `app-erp-master-data.orm.xml`、`module-master-data/deploy/sql/{mysql,postgresql,oracle}/_create_index.sql`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 双 GRANT

- [x] Add: ErpMdMaterialSku `<indexes>` 增 `IDX_MD_MATERIAL_SKU_BARCODE`（单列 barcode，unique="false"，附双批准溯源注释）。
- [x] Proof: `mvn clean install -DskipTests` BUILD SUCCESS（build18）；重生成产物含新索引（erp-md-dao target/classes `_app.orm.xml:529` 抽查留证；xmllint 的 namespace 告警为该文件固有形态非回归——权威校验=平台构建解析）。
- [x] Add: 三方言 `_create_index.sql` 逐字对齐追加（裸 `CREATE INDEX`，追加于同实体 TAX_RATE_ID 行后；头注计数 32→33 同步——审计条件①②落实）；**未运行 `scripts/add-orm-indexes.js`**（F-B3 雷区，手工编辑）。口径注记（审计条件 A-M1）：同步=追加 +1 行三方言互 diff，全文件与模型的历史漂移（37 vs 32）属 F-B3 类既有事项不扩表。

Exit Criteria:

- [x] 重生成产物含新索引（抽查留证）
- [x] 全量构建绿；deploy SQL 三方言同步 diff 留证

### Phase 3 - 行为验证 + 收口

Status: completed
Targets: 测试 + 文档
Skill: development-wisdom-gate

- Item Types: `Proof`

- [x] Proof: 触域测试全绿——`TestErpInvBarcodeResolve` 7/7 + `TestErpInvStockTakeLineScanSave` 8/8（合计 15/15，索引库上运行）；E2E pda-stock-take 3/3（fresh 索引库真实点击链复验）。
- [x] Proof（文档）: 分析报告 §3.2-F7 状态回填；`barcode-integration.md` v2 段补索引落地注记（含第三受益路径 scanSave——审计条件 B-C2）；G1 注记落 `docs/logs/2026/10-05.md`（具名：javadoc ErpMdMaterialSkuBizModel:56 + plan 2026-08-06-0100-3 + logs 2026-07-07——点查性能已由本轮索引承载，DB 唯一键仍为 G1 successor，不回改历史计划）。

Exit Criteria:

- [x] 触域测试全绿（含条码解析链）
- [x] 文档三处回填完成

## Draft Review Record

- Independent draft review iteration 1: needs revision, 轻度 (agent_60688ff5) — 1 MAJOR / 3 MINOR：M1 拒绝 unique-key 的「同表无先例」理由失实（同文件软删+UK 先例大量存在：ErpMdMaterial UK_MD_MATERIAL_CODE 等 8 实体）；m2 deploy SQL 应为逐字同构裸 CREATE INDEX（非 IF NOT EXISTS）+ 未继承先例 F-B3 雷区（禁全量跑 add-orm-indexes.js）；m3 deploy SQL 实际路径应写明；m4 「若环境允许…可选」反松弛违例。
- Independent draft review iteration 2: accept (agent_e9902535) after <M1 软删+UK 先例 8 实体逐一实核成立、G1 归属与应用层门控默认 TRUE :477-479 实核；m2 F-B3 雷区双处登记实核；m3 三方言路径实存；m4 H2 explain 移 Deferred 带触发条件；无新问题>

## Closure Gates

- [x] 范围内行为完成（索引落地 + 派生物同步 + 审计条件全部落实）
- [x] 相关文档对齐（分析报告 + barcode-integration.md + G1 具名注记）
- [x] 已运行验证（全量构建 BUILD SUCCESS + 触域测试 15/15 + E2E 3/3 + 产物抽查）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（2 轮收敛）
- [x] 文本一致性已验证：状态、阶段、门控和日志一致（日志随闭包提交写入）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（round1 agent_8472a5d2 **ACCEPT**）
- [x] 结束证据存在于文件中（build18 日志 + deploy SQL diff + surefire + E2E 输出）
- [x] 保护区双独立子 agent 批准记录在案（Phase 1）

## Deferred But Adjudicated

### DB 级 barcode 唯一键（md 域 Deferred G1）

- Classification: `watch-only residual`（维持既有登记）
- Why Not Blocking Closure: 唯一性由应用层门控承载；软删交互与存量数据核查是 G1 重开时的前置义务（按同文件 ErpMdMaterial 等软删+UK 先例模式办理），非本计划（性能索引）范围。
- Successor Required: `yes`（既有 G1 登记，触发条件不变）

### H2 查询计划佐证（explain 级索引命中证明）

- Classification: `optimization candidate`
- Why Not Blocking Closure: H2 与生产方言计划器差异使 explain 证据说服力有限；索引存在性+DDL 落地+点查路径正确性已由 Phase 2/3 覆盖。
- Successor Required: `no`（触发=生产环境实测扫码 RT 不达预期时）

## Closure

Status Note: 独立结束审计 round1 **ACCEPT**（agent_8472a5d2）：orm 索引/三方言 SQL/重生成产物/156 模块构建/触域 15/15 独立复验全部通过；双 GRANT 落盘合规；零行为变更实证（git diff 仅 +index 块与注释 + SQL 各 1 行 + 头注计数）；owner-doc 抽样 4 项零漂移。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 round1 agent_8472a5d2（**ACCEPT**）
- Evidence: `_tmp/build18-p5.log`（156 模块 BUILD SUCCESS）+ deploy SQL 三方言 diff + surefire 15/15 + `test-results/.last-run.json`（E2E 3/3 passed）+ `runner16.log`（scanSave RPC 命中于索引库）

Follow-up:

- H2 explain 查询计划佐证（optimization candidate，触发=生产实测扫码 RT 不达预期，见 Deferred 节）

Follow-up:

- <非阻塞跟进项>
