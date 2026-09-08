---
status: active
mission: ai-check-r3
work-item: M1.6
group: "2026-09-09-0232"
verify: [test]
---

# 2026-09-09-0232-1 M1.6 manufacturing mfg-2 五维符合性审计（BOM/MRP/CRP 切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK report mode CAT1..4 = 0/0/0/0、双 checker 零回归、白名单 27 文件四要素齐备）；前置切片 M1.5（mfg-1，plan `2026-09-08-1042-2`）已执行且闭包审计 ACCEPT（2026-09-08，审计时点 HEAD `dd39e6cce`；roadmap done 翻转按 M1.x 同批先例归 owner/engine 处置，不阻塞本切片）。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U08 × 五维 × mfg-2**（§4 映射表）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准；mfg-2 单元焦点 = BOM/路由/MRP 净额归集/低阶码/仿真版本/CRP 负荷链。
- 切片范围（U08 mfg-2）：BOM 结构与展开（ErpMfgBom 族 + 展开消费交点）/工艺路由/MRP 引擎（MrpEngine + SimulationMrpEngine 净额归集/低阶码）/MRP 计划单生成/CRP 负荷链（crp_load）；owner doc `docs/design/manufacturing/mrp.md`（120 行）+ `crp.md`（135 行）（roadmap M1.6 行指定）+ `bom-and-routing.md`（187 行）+ `simulation-engine.md`（323 行）+ `use-cases.md`（269 行，实仓核验在盘）；物理面 `module-manufacturing/erp-mfg-{dao,service,web}` 的 `src/main`。
- 切片边界（冻结清单 §3.2 + U08 行 + r1 切片边界）：工单/作业卡/领料/预留/齐套/完工入库面归 mfg-1（M1.5 已审）——本切片仅审 `KitAvailabilityChecker.explodeRequirements` 的 BOM 展开参数消费交点；委外释放/批次基因/差异公式（`ProductionVarianceCalculator`/CostVariance 族）归 mfg-3（M1.7 同批）邻接标注；common 抽象族（`AbstractProcessor.illegal*`、状态锁基类）行为缺陷归 U20（M1.15），本切片只审调用点合规；聚合横切面归 U21（M1.16）；posting 引擎内部归 fin-1（M1.1 已审）。
- 跨轮查重源（§2）：r1 `docs/audits/check/ck-mfg-bom-mrp.md`（0 P0 / 3 P1 / 9 P2 / 11 P3 = 23 条；跨轮索引状态 4 fixed / 19 open——P1-CK-mfg2-001/002 等经 F2.6 修复，HEAD 复核义务在 Phase 2）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照；新立 ID 序列接续 `P{n}-CK-mfg2-{NNN}-r3`（自 024 起，历史 ID 永不覆写）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：mfg 探针族（CAT-1 31 / CAT-2 9）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块差距归 successor `ai-check-r3-compliance-baseline-raise`。
- 仓库现状（2026-09-09 草案时点实核）：HEAD `7ad2e426b`（姊妹 M1.4 计划落盘提交）；脏面 = 本批 3 份 `2026-09-09-0232-*` 计划文件（草案产物）。审计证据一律以实跑时 HEAD + 脏面披露为准（Phase 1 机械登记）。
- 模块级回归参照：`mvn test -pl module-manufacturing/erp-mfg-service` 308 全绿（M1.5 执行两轮与 MI.6 批 2/2 批注账一致）；known-good-baselines 无模块级 mfg 计数行，实跑计数照实登记不预填。
- 剩余差距：U08 × 五维 × mfg-2 五格 verdict 未落盘；mfg-2 无 `-r3` 切片报告。

## Goals

- 按冻结清单对 U08 × 五维 × mfg-2 五格全跑（禁止抽样、禁止跳维），逐格落 verdict，产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-bom-mrp-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`。
- 全程零生产代码改动（roadmap 规则 6），收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（修复归 M2.x）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 mfg-1（M1.5 已审）/ mfg-3（M1.7 同批）格与其他单元格；工单/委外/批次/差异面缺陷按边界标注归并不立项。
- 不接管 r1/r2 工作项；不重开既有裁决（F2.6 修复方案、lesson 09/10）；不做 roadmap 状态翻转。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用）；盘点注记落本计划勾选注记
> Prereqs: 无（M1.5 已执行；本批执行序第 1，与 M1.7/M1.9 并行起草）

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用；roadmap 规则 8），确认索引与冻结清单在位
      - Skill: none
      - 证据（2026-09-09）：目录幂等复用在位；`ai-check-r3-index.md` 头部登记路径 = 本目录一致；`m0-5-audit-checklists.md` 冻结清单在位（§1/§2/§3.3 U08 行/§4 M1.6 映射/§6 勘误 E1 实读）
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露；后续全部证据注记引用该时点
      - Skill: none
      - 证据：T0 HEAD `7ad2e426b`（= 基线草案时点一致）；脏面 = 本批 3 份 `2026-09-09-0232-*` 计划文件（untracked：-1 本计划 / -2 M1.7 mfg-3 / -3 M1.9 ast-2 姊妹并行起草披露，零其他脏面）；全报告证据引用该时点
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据：checker 19 规则实测 R1a-c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42 = M0.3 快照行逐项一致零漂移；CJK report mode CAT-1..4 = **0/0/0/0**（CAT-5 注释 21158 行豁免统计）= MI 终态行一致零漂移

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.6 行指定）
> Targets: `module-manufacturing/erp-mfg-dao|erp-mfg-service/src/main/java`（mfg-2 范围 = BOM/路由/MRP/仿真计划/CRP 族文件）
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族 + codegen 产物安全 + 聚合完整性（`app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：checker 19 规则 = M0.3 快照行零漂移（Phase 1 实跑）；反模式族 mfg dao+service 全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0；@Transactional∩@BizMutation 7 命中均 mfg-1/mfg-3/posting 族 javadoc 注记，mfg-2 范围=0）；`IDaoProvider/IOrmTemplate` mfg-2 命中：核心 7 服务助手豁免注释齐备 + IOrmTemplate 仅 flushSession（弱覆盖站点→026-r3）；`git status` 生成产物零脏面；`__XGEN_FORCE_OVERRIDE__` 20 处全为 erp-mfg-meta codegen dict.yaml 只读校验点；聚合完整性 E1 勘误路径实存（多行 x:extends 含 `mfg/auth/erp-mfg.action-auth.xml`）+ 保留层文件在位
- [x] <Proof> 15 维度逐维走查 mfg-2 范围（重点：MRP 净额归集/低阶码正确性——F2.6 修复复用 HEAD 复核、仿真版本语义、CRP 负荷链口径、BOM 展开消费交点；涉 posting 引擎内部时标注归属 fin-1）+ blocker/major/minor 分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：15/15 无跳维（报告 §1 DIM-B 行）：①范式合规 ②直 DAO 豁免注释普查（弱覆盖→026-r3）③NopException+ErrorCode 全合规 ④@Inject 非 private/无冗余 @Transactional ⑤CoreMetrics/StringHelper 合规（可控时钟零违例）⑥CrudBizModel+@BizQuery/@BizMutation ⑦机制 B notGenCode 零违例 ⑧MrpPlanStateMachine 3 边声明式+Deferred CANCELLED ⑨R6.2 per-mutation Processor 全套+**FNPT 缺口（→024-r3）** ⑩零 delta 手改需求 ⑪orgId 业务维度非 tenantId ⑫指针 DIM-T ⑬codegen 安全（见上）⑭聚合注册在位+域内 FNPT 面（024-r3）⑮见下行；F2.6 三修复 HEAD 复核有效（003 经 nop-api-core `QueryBean.addOrderField(name, boolean desc)` L435 签名实核 `true`=DESC）；mfg2-012 F1.3 基类 19/19 接入复核；严重性分级：新立 0 blocker / 0 major / 5 P3 minor（024/025/026/027/028-r3）
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`mrp.md` + `crp.md` + `bom-and-routing.md` + `simulation-engine.md` 中 ≥2 doc × 2 关键断言（净额归集/低阶码/负荷口径/版本语义）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
      - 证据：4 doc × 7 断言（超 ≥2×2 义务）：mrp.md L92 低层码/净额归集机制 = F2.6 修正声明与双引擎实现一致 ✓；mrp.md L98 释放两级机制语义一致（行号引用 stale→025-r3 附带）；mrp.md L95 委外释放 = **漂移 1（→025-r3）**；mrp.md L116 4 维 diff/L60 orgId = 既有裁决 006/019 交叉引用；crp.md L79/L128-129 APS 双源 SPI + L100-101 cron 门控 = 一致 ✓✓；simulation-engine.md L96 4 维公式 = 既有裁决 006；bom-and-routing.md L64 use_multi_level_bom = 既有裁决 009；漂移 1 < 2 未触发全量扩样（实际已遍及 4 doc）
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-mfg-bom-mrp.md` 23 条 finding + r2 + 基线快照；fixed 项（F2.6 族）复用并复核 HEAD 有效性，open 项同型归并原 ID）；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 证据：r1 23 条逐一比对（报告 §2）——复用 4（001/002/003 F2.6 + 012 F1.3 HEAD 复核有效）/ 归并 19（004..023 现症复核在位，020 追加 requireComparable L138 新站点）/ 新立 3（024-r3 FNPT、025-r3 ⑮漂移、026-r3 豁免注释）；r2 目录 mfg FNPT 族零命中；checker 命中均为基线快照已裁决偏离不重复立项

Exit Criteria:

- [x] U08×B×mfg-2 格 verdict 落盘，15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2）
> Targets: `module-manufacturing/erp-mfg-web/src/main/resources/_vfs`（mfg-2 面：BOM 树看板/MRP/CRP 相关页面）
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（导出 0 error；325 条既有 variant 外部漂移为 successor 在案，非本切片 finding）+ flux-only grep（AMIS 保留层 / ORM `ext:web-renderer="flux"`）
      - Skill: none
      - 证据：`FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855` + `files=855 validated=855 errors=325 warnings=18491`——325 条逐条同型 variant:"primary"×dropdown-button，mfg 域 32 文件 34 条（= M1.5 记录精确一致），successor 在案非本切片 finding；`component="AMIS"` 保留层 0；ORM `ext:web-renderer="flux"` 缺失 0
- [x] <Proof> mfg-2 页面走查：BOM 树看板 `dashboard/bom-tree`（M1.5 邻接记录承接入本格 verdict）+ MRP/CRP/路由相关页对照 view-and-page-strategy + dashboards pattern（REST `/r/` / M0.4 源头链查表 / `x:extends` 定制 / i18n-en 承载）逐页落 verdict；涉 mfg E2E spec 时核对 PageObject + flux 引擎缺省 + 禁 GraphQL 断言
      - Skill: none
      - 证据：bom-tree.flux.yaml（运行时权威）REST `@query:ErpMfgBom__findBomTree` + flux 原生 tree + i18nEn 全合规——孪生遮蔽 bom-tree.page.yaml 端点/结构错配（explode 扁平 vs childrenKey 嵌套）→ 归并 P3-CK-mfg-023-r3 追加站点（运行时零影响）；crp-load-report.page.yaml REST `@query:ErpMfgReport__renderHtml` + `/p/` 下载参数三处一致 + i18nEn 合规；MRP/仿真/路由/BOM/CostRollup 实体页 = M0.4 GenPage stub（view.xml i18n-en 源头承载，row 61-62/73-79）；view.xml `graphql` 命中 = `graphql:labelProp` 平台 meta 键非 API；E2E：mfg spec 5 个选择器违规=0、`E2E_ENGINE` 缺省 flux（engine.ts L8-9）、mfg-mrp-simulation spec GraphQL 命中 = runbook L229 业务动作套件登记 API 驱动通道（L122 页面断言禁令不适用）

Exit Criteria:

- [x] U08×F×mfg-2 格 verdict 落盘；全局面门禁数字在案对账一致
- [x] mfg-2 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md` + `docs/design/manufacturing/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ mfg 相关 seed（bom/mrp 计划/crp_load/routing 族 + workcenter 配置链）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿
      - Skill: none
      - 证据：**4/0/0/0 全绿 BUILD SUCCESS**（14.3s；全表 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 门控）
- [x] <Proof> mfg-2 seed 面核对：`git status --porcelain` seed 路径 expect 空 + deploy `_seed_*.sql` 同步义务查登记处表 + bom/mrp_plan/crp_load/routing seed 自洽约束抽查（按 seed-data.md 运营域约束段；SPC/CRP 双层门控默认关核对）
      - Skill: none
      - 证据：`git status --porcelain _init-data/` 空（零 seed 变更，快照重录义务未触发）；资产清点 372 CSV + 1 SQL（= 冻结口径「M1.5 批次后」登记值）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族（= seed-data.md §97 登记处已聚合，mfg 零 deploy seed 零同步义务无第三态）；自洽抽查：bom isDefault=false 干扰面裁决 + PHANTOM N-DIS 负例 + bom_line FK 全实存（机器门禁背书）、mrp_plan DRAFT/COMPLETED/FIRMED 三态 + `-PROMOTED-2` 后缀与代码约定一致、crp_load workcenterId=1→WC-001 + workOrderId=1→WO-2026-001 实存（= M1.5 同行种子）、routing/workcenter/capacity 配置链在位（0628-1 口径）；双层门控默认关：crp-run-cron 空=跳过 + simulation/subcontract-release/overhead-allocation 默认 false（application.yaml 零 erp-mfg 覆盖）

Exit Criteria:

- [x] U08×S×mfg-2 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 自洽抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-manufacturing/erp-mfg-service`（`<SVC>` 绑定：`<SVC>` = `module-manufacturing/erp-mfg-service`，冻结清单 §1.4 程式记号）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-manufacturing/erp-mfg-service` 全绿零失败（模块级参照 = 308 全绿（M1.5 执行 + MI.6 批注账）；全仓聚合对照面 = known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1；实跑计数照实登记不预填）
      - Skill: none
      - 证据：三轮实跑照实登记——第一轮 **308/0/0/0 全绿**（= 参照值零回归）；第二轮 1 Error（TestErpMfgWorkOrderEndToEnd 快照 @var 厘秒/毫秒精度竞态 → 新立 P3-CK-mfg2-028-r3，隔离复跑 4/0/0/0 绿证实 flaky 非生产回归）；第三轮全量 **308/0/0/0 全绿**收口
- [x] <Proof> 覆盖缺口对账：公开方法清单 × `_cases` 测试目录清单逐项对账（mfg-2 范围 = BOM/MRP/仿真计划/CRP 族 BizModel）；关键业务流清单核对——MRP 计算→计划单生成→CRP 负荷清单行逐行核覆盖；缺口按业务关键度定级
      - Skill: none
      - 证据：mfg-2 自定义动作 14 个对账——runMrp↔MrpEngine 8 测试+EndToEnd / explode+findDefaultBom↔BomExplosion / rollupCost↔CostRollup 9 测试 / runSimulation+promote+compareVersions↔MrpSimulation 9 测试 / calculateLoad+getLoadReport↔CrpLoad 7+CrpLoadSource 3 / release×3↔EndToEnd+Subcontracting；关键业务流 MRP 计算→计划单生成→CRP 负荷逐段覆盖在位（链路断点本体 = P2-CK-mfg2-005 open 承载）；缺口 1：**findBomTree 零测试断言**→ P3-CK-mfg2-027-r3（只读可视化助手定级 P3）
- [x] <Proof> 快照纪律：`SnapshotTest.RECORDING` 提交态零残留 + `*` 通配/`delVersion` 屏蔽合规抽查
      - Skill: none
      - 证据：`SnapshotTest.RECORDING` = 0 残留；mfg-2 测试文件 `*` 通配/delVersion 零命中（createTime/updateTime 框架自动屏蔽合规）；显式 @var 绑定面屏蔽缺口（028-r3，mfg-1 E2E 资产归属注记）

Exit Criteria:

- [x] U08×T×mfg-2 格 verdict 落盘；本地回归全绿数字在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + mfg 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `--self-test` PASS；脚本红 = MI 回归升级报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据：`--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318 基线记录值，现树实测 0/0/0/0）；`--self-test` **PASS**；mfg 探针族（CAT-1 31/CAT-2 9）维持清零零回归
- [x] <Proof> 白名单合规抽查：§WHITELIST mfg 相关条目抽 ≥3 条核对四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none
      - 证据：WHITELIST mfg 文件条目 1 条（ErpMfgDashboardBizModel）+ 按消费链扩样共抽 4 条（mfg/inv/pur Dashboard 同簇 + aps IErpApsOperationOrderBiz mfg 建卡 SPI）——**4/4 四要素齐备**（路径/cats/理由/owner doc i18n-compliance.md 准绳表 #5 + 裁决来源 plan 2026-09-07-1715-1）零登记缺陷；`grep -L @Locale` *Errors.java = 空（ErpMfgErrors 实核 1 处声明）；meta + `_vfs/i18n` 零手改

Exit Criteria:

- [x] U08×I×mfg-2 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-bom-mrp-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：三态一致性、级别（P0~P3）、ID 规范（`P{n}-CK-mfg2-{NNN}-r3`，自 024 起）、归属标注（mfg-1/M1.7/fin-1/U20/U21 归并指针）复核
      - Skill: code-quality-audit-prompt
      - 证据：汇总复裁决完成（报告 §2/§3）——新立 5 全 P3（024 FNPT / 025 ⑮漂移 / 026 豁免注释 / 027 findBomTree 覆盖 / 028 快照精度竞态，ID 序列 `P3-CK-mfg2-024..028-r3` 自 024 起合规零冲突）；复用 4 / 归并 19 + 跨切片站点追加 1（mfg-023-r3）；归属标注复核：BOM 展开消费交点=本格、委外/差异本体=mfg-3、posting 内部=fin-1、common 基类=U20、聚合横切=U21（§2.5）；历史 23 ID 零覆写
- [x] <Add> 落盘 `ck-mfg-bom-mrp-r3.md`：五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明
      - Skill: none
      - 证据：报告已落盘（5/5 覆盖矩阵 B=finding/F=finding(归并态)/S=pass/T=finding/I=pass；§2 三态裁决 5.1/2.2/2.3/2.4/2.5 + §3 统计 + §4 剩余风险四件套）
- [x] <Add> 双索引同步：本轮索引产物清单追加 + 跨轮索引 §报告清单 M1.6 行 + §Finding 追踪新立 ID 行
      - Skill: none
      - 证据：本轮 `ai-check-r3-index.md` 产物清单 M1.6 行已追加；跨轮 `ai-check-index.md` §报告清单 M1.6 行（0/0/0/5）+ §Finding 追踪 5 条新 ID 行（024..028-r3，状态 open）+ M1.6 r3 复核注记块已追加
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤生产路径 expect 空
      - Skill: none
      - 证据：收官实跑——过滤 `module-*`/`app-erp-all` 生产路径 = **空**（零生产代码改动）；全部脏面 = 本报告 + 双索引（M）+ 本计划勾选 + 姊妹 2 计划文件（untracked），均审计文档产物
- [x] <Proof> 收尾回归：`mvn test -pl module-manufacturing/erp-mfg-service` 复跑全绿
      - Skill: none
      - 证据：复跑 **308/0/0/0 全绿 BUILD SUCCESS**（第三轮；第二轮 1 Error 已裁决 = P3-CK-mfg2-028-r3 测试资产 flaky，隔离复跑绿在案，非生产回归）

Exit Criteria:

- [x] `ck-mfg-bom-mrp-r3.md` 落盘且五维矩阵 5 格 verdict 完整
- [x] 双索引行追加在案；零生产代码改动核证通过；mfg service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-08-193051-mission-driver-2026-09-09-0232-1-m16-mfg-bom-mrp-five-dim-audit-1-db5b360b to 2026-09-08-193051-mission-driver
- 2026-09-09：iteration 1，共识 accept #review-2026-09-08-193051-mission-driver-2026-09-09-0232-1-m16-mfg-bom-mrp-five-dim-audit-1-db5b360b（补 Closure Gates——按同族 M1.1/M1.5 先例为本只读审计计划定制门控；基线快照断言逐一实仓复验在盘——HEAD `7ad2e426b`、脏面=本批 3 计划、MI 终态行 4006/0/0/1 与 CAT 0/0/0/0 + 白名单 27 文件、M0.3 checker 全表 11 值、r1 mfg2 23 finding（0 P0/3 P1/9 P2/11 P3）与跨轮 4 fixed/19 open、F2.6 修复 P1-CK-mfg2-001/002 索引在案、新 ID 自 024 起、五份 owner doc 行数（120/135/187/323/269）、`dashboard/bom-tree` 页面、`TestErpSeedDataIntegrity`、auth 聚合路径、roadmap 规则 6/8 引用；M1.5 前置执行态与 roadmap 翻转归属披露诚实，不阻塞本切片；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-mfg-bom-mrp-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单 M1.6 行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-manufacturing/erp-mfg-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 mfg-1/M1.7/fin-1/U20/U21」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-09.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-mfg-bom-mrp-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

> 执行时点：2026-09-09，T0 HEAD `7ad2e426b`；各命令数字亦落 Phase 勾选注记，命令面与 Closure Gates 所列一致。

- PASS `bash docs/audits/nop-compliance-checker.sh` — 19 规则 R1a-c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42 = M0.3 快照行逐项一致零漂移（Phase 1/2）
- PASS `node tools/check-hardcoded-cjk.mjs`（report mode）— CAT-1..4 = 0/0/0/0 = MI 终态行一致（Phase 1）
- PASS `npm run validate:flux` — 导出 0 error（999 页/855 erp 页）+ `files=855 validated=855 errors=325`（325 条全 variant 既有漂移 successor 在案，mfg 32 文件 34 条与 M1.5 记录精确一致）（Phase 3）
- PASS `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` — 4/0/0/0 全绿 BUILD SUCCESS（Phase 4）
- PASS `mvn test -pl module-manufacturing/erp-mfg-service` — 三轮：308/0/0/0 绿 + 1 轮 1 Error（= P3-CK-mfg2-028-r3 测试资产 flaky，隔离复跑 4/0/0/0 绿裁决非生产回归）+ 收尾复跑 308/0/0/0 全绿 BUILD SUCCESS（Phase 5/7）
- PASS `node tools/check-hardcoded-cjk.mjs --strict` — exit 0（0 new violations；170 baseline files）；`--self-test` PASS（Phase 6）
- PASS `git status --porcelain` 生产路径过滤 — 空（零生产代码改动；脏面仅审计文档产物：本报告 + 双索引 + 计划勾选 + 姊妹 2 计划文件）（Phase 7）
- 五维覆盖矩阵 5/5 完整：`ck-mfg-bom-mrp-r3.md` §1（B=finding / F=finding(归并态) / S=pass / T=finding / I=pass）；三态裁决 复用 4 / 归并 19+1 / 新立 5（`P3-CK-mfg2-024..028-r3`）；历史 23 ID 零覆写；双索引同步在案
- pass test 2026-09-09-0404 exit=0

## Closure

- **裁决：ACCEPT**（passes closure audit，2026-09-09，独立结束审计员 fresh session 实证复核，审计时点 HEAD `7ad2e426b` 未变）
- 五点一致性 5/5 pass：①范围完成（Phase 1~7 全 `[x]`，报告 `ck-mfg-bom-mrp-r3.md` 在盘，矩阵 5/5 verdict 完整，统计表↔finding 列表一致 5/4/19+1）；②抽样实证 ≥7 项全过（HEAD `7ad2e426b`/脏面仅 docs 审计产物零生产路径；checker 19 规则 11 值逐项精确一致；CJK `--strict` exit 0 + `--self-test` PASS；mfg 回归审计员独立复跑命中 1 Error → 隔离复跑 4/0/0/0 绿 → 全量复跑 308/0/0/0 绿，flaky 裁决链独立重现且强化 028-r3 登记；双索引 M1.6 行/5 新 ID 行/复核注记块在盘；FNPT 实核 9 节点全 mfg-1/mfg-3 面、8 个 mfg-2 mutation 零注册 = 024-r3 事实成立；`SimulationMrpEngine.nextVersionNo` L505-516 `addOrderField("versionNo", true)` DESC 修复在位 = 003 复用成立）；③anti-hollow pass（勾选证据实质，1 Error 诚实登记）；④历史 ID 零覆写（跨轮索引 diff 8 insertions/0 deletions，r1 23 ID = 4 fixed/19 open 原样）；⑤deferred honesty（Non-Goal 归属与冻结清单 §3.2 逐项一致，无范围内降级）；⑥frontmatter `status: active` 保持，报告/索引/计划计数一致（新立 5/复用 4/归并 19+1）
- Minor（登记不阻塞）：报告 §2 前言「本轮新立 4 条（024..027）」与 §2.3 标题「— 4 条」为 stale 计数（该节实际列 5 条，§3 统计/计划/双索引均 5）——执行者已于审计回执后当场修正（2026-09-09，两处 4→5，修正后报告计数三角一致；zero 生产代码影响）
- 验证范围注记：本计划为只读审计切片，验证命令组即结果表面（Closure Gates 已声明 MV.1 对照面 = known-good-baselines 2026-09-08 MI 终态行）；mfg service 为模块级 scoped 回归，不作为全仓 full-green 依据（全仓对照由 roadmap 收官机制承载）
- dispatch audit #audit-2026-09-09-0404-2026-09-09-0232-1-m16-mfg-bom-mrp-five-dim-audit-1-1dc109b2 to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-2026-09-09-0404-2026-09-09-0232-1-m16-mfg-bom-mrp-five-dim-audit-1-1dc109b2：独立结束审计 ACCEPT——35/35 计数域全 `[x]`、五维矩阵 5/5、报告/双索引/roadmap/日志对账一致；closure visit 实跑 `mvn test -pl module-manufacturing/erp-mfg-service` 308/0/0/0 全绿 BUILD SUCCESS（exit=0），机械 checker `plan-check.mjs --strict` 复核通过
