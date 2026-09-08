---
status: active
mission: ai-check-r3
work-item: M1.5
group: "2026-09-08-1042"
verify: [test]
---

# 2026-09-08-1042-2 M1.5 manufacturing mfg-1 五维符合性审计（工单与报工切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK report mode CAT1..4 = 0/0/0/0、双 checker 零回归、白名单 27 文件四要素齐备）。M1.5 与 M1.1 同为 M1 首批 deps 满足项；本计划执行顺序居本批第 2（M1.1 解锁 fin 后续切片，先行的文档序首批成员）。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U08 × 五维 × mfg-1**（§4 映射表第 5 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准。
- 切片范围（U08 mfg-1）：工单双轴状态机（approveStatus×docStatus）/作业卡/领料红冲三件套回退/预留/完工入库幂等键；owner doc `docs/design/manufacturing/state-machine.md`（290 行，实仓核验在盘）+ `docs/design/manufacturing/use-cases.md` + `docs/design/manufacturing/material-reservation.md`；物理面 `module-manufacturing/erp-mfg-{dao,service,web}` 的 `src/main`。
- 共享代码唯一归属（冻结清单 §3.2）：完工入库移动为跨域消费点——**消费侧行为归本格**，涉及 posting 引擎内部时标注「归属 fin-1」归并；common 抽象族（`AbstractProcessor.illegal*`、状态锁基类）行为缺陷归 U20（M1.15），本切片只审调用点合规；聚合横切面归 U21（M1.16）。
- 跨轮查重源（§2）：r1 `docs/audits/check/ck-mfg-workorder.md`（C4.1：1 P0 / 4 P1 / 6 P2 / 10 P3，done；P0-CK-mfg-001 + P1-CK-mfg-002~005 fixed；P2-CK-mfg-006~011 open——其中 011 部分修复、残余归 F2.5 successor；P3-CK-mfg-012~021 open）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：mfg 探针族（CAT-1 31 / CAT-2 9）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块差距归 successor `ai-check-r3-compliance-baseline-raise`。
- 仓库现状（2026-09-08 草案时点实核，草案审查时点已复验）：草案基线 HEAD `40512c567`；姊妹 plan `2026-09-07-2200-1`（StateMachine 直抛领域码，mfg 批次 `1166339ae` 先行入库、非其 Phase 4 六小域批次成员）已于 `67308e144` 独立 closure audit ACCEPT 落账收官；审查时点 HEAD `8825a10e1`（lesson 19 索引登记行），脏面仅本批 3 份 `2026-09-08-1042-*` 计划未入库。DIM-B/T 走查将面对姊妹重构后的 mfg 代码态。审计证据一律以实跑时 HEAD + 脏面披露为准（Phase 1 机械登记）。
- 剩余差距：U08 × 五维 × mfg-1 五格 verdict 未落盘；mfg-1 无 `-r3` 切片报告。

## Goals

- 按冻结清单对 U08 × 五维 × mfg-1 五格全跑（禁止抽样、禁止跳维），逐格落 verdict，产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-workorder-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`。
- 全程零生产代码改动（roadmap 规则 6），收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（修复归 M2.x）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 mfg-2（BOM/MRP/CRP，M1.6）/ mfg-3（委外/批次追溯/差异，M1.7）格与其他单元格。
- 不接管 r1/r2 工作项；不重开既有裁决（含 P2-CK-mfg-011 的 F2.5 successor 归属、lesson 09/10）；不做 roadmap 状态翻转。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用）；盘点注记落本计划勾选注记
> Prereqs: 无（与 M1.1 计划并行起草，执行序居后不阻塞）

- [ ] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用；roadmap 规则 8），确认索引与冻结清单在位
      - Skill: none
- [ ] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露；后续全部证据注记引用该时点
      - Skill: none
- [ ] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none

Exit Criteria:

- [ ] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [ ] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.5 行指定）
> Targets: `module-manufacturing/erp-mfg-dao|erp-mfg-service/src/main/java`（mfg-1 范围 = 工单/作业卡/领料/预留/完工入库族文件）
> Prereqs: Phase 1 完成

- [ ] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族 + codegen 产物安全 + 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
- [ ] <Proof> 15 维度逐维走查 mfg-1 范围（重点：②跨实体 I*Biz（inv 预留/库存调用点）③NopException ⑧双轴状态机 ⑨审批流；完工入库移动消费侧涉及引擎内部时标注归属 fin-1）+ blocker/major/minor 分级
      - Skill: nop-platform-conformance-audit-prompt
- [ ] <Proof> 维度⑮ owner-doc 关键断言抽样：`docs/design/manufacturing/state-machine.md` + `use-cases.md` + `material-reservation.md` 中 ≥2 doc × 2 关键断言（双轴状态名/迁移路径/ErrorCode）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
- [ ] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-mfg-workorder.md` 21 条 finding + r2 + 基线快照；r1 open 项同型归并原 ID，fixed 项复用并复核 HEAD 有效性）；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt

Exit Criteria:

- [ ] U08×B×mfg-1 格 verdict 落盘，15 维度无跳维
- [ ] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2）
> Targets: `module-manufacturing/erp-mfg-web/src/main/resources/_vfs`（mfg-1 面：排产/工单看板页面）
> Prereqs: Phase 1 完成

- [ ] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（导出 0 error；325 条既有 variant 外部漂移为 successor 在案，非本切片 finding）+ flux-only grep（AMIS 保留层 / ORM `ext:web-renderer="flux"`）
      - Skill: none
- [ ] <Proof> mfg-1 页面走查：排产/工单看板页对照 view-and-page-strategy + dashboards pattern（REST `/r/` / M0.4 源头链查表 / `x:extends` 定制 / i18n-en 承载）逐页落 verdict；涉 mfg E2E spec 时核对 PageObject + flux 引擎缺省 + 禁 GraphQL 断言
      - Skill: none

Exit Criteria:

- [ ] U08×F×mfg-1 格 verdict 落盘；全局面门禁数字在案对账一致
- [ ] mfg-1 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md` + `docs/design/manufacturing/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ mfg 相关 seed（work_order/cost_variance/forecast 族 + workcenter 配置链 + crp_load）
> Prereqs: Phase 1 完成

- [ ] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿
      - Skill: none
- [ ] <Proof> mfg-1 seed 面核对：`git status --porcelain` seed 路径 expect 空 + deploy `_seed_*.sql` 同步义务查登记处表 + work_order/workcenter/crp_load seed 自洽约束抽查（按 seed-data.md 运营域约束段；SPC/CRP 双层门控默认关核对）
      - Skill: none

Exit Criteria:

- [ ] U08×S×mfg-1 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [ ] seed 零变更 + 同步义务核对 + 自洽抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-manufacturing/erp-mfg-service`（`<SVC>` 绑定：`<SVC>` = `module-manufacturing/erp-mfg-service`，冻结清单 §1.4 程式记号）
> Prereqs: Phase 1 完成

- [ ] <Proof> 切片本地回归：`mvn test -pl module-manufacturing/erp-mfg-service` 全绿零失败（全仓聚合对照面 = known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1；模块级参照 = 姊妹 plan `2026-09-07-2200-1` mfg 批次提交 `1166339ae` 308 green——known-good-baselines 无模块级 mfg 计数行，实跑计数照实登记不预填）
      - Skill: none
- [ ] <Proof> 覆盖缺口对账：公开方法清单 × `_cases` 测试目录清单逐项对账（mfg-1 范围 = 工单/作业卡/领料/预留/完工入库族 BizModel）；关键业务流清单核对——P1 工单→领料→报工→完工入库→成本结转清单行逐行核覆盖；缺口按业务关键度定级
      - Skill: none
- [ ] <Proof> 快照纪律：`SnapshotTest.RECORDING` 提交态零残留 + `*` 通配/`delVersion` 屏蔽合规抽查
      - Skill: none

Exit Criteria:

- [ ] U08×T×mfg-1 格 verdict 落盘；本地回归全绿数字在案
- [ ] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + mfg 白名单条目抽查
> Prereqs: Phase 1 完成

- [ ] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `--self-test` PASS；脚本红 = MI 回归升级报 MI 通道，不立 DIM-I finding
      - Skill: none
- [ ] <Proof> 白名单合规抽查：§WHITELIST mfg 相关条目抽 ≥3 条核对四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none

Exit Criteria:

- [ ] U08×I×mfg-1 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [ ] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-workorder-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [ ] <Decision> 全切片候选 finding 汇总复裁决：三态一致性、级别（P0~P3）、ID 规范（`P{n}-CK-mfg-{NNN}-r3`）、归属标注（fin-1/U20 归并指针）复核
      - Skill: code-quality-audit-prompt
- [ ] <Add> 落盘 `ck-mfg-workorder-r3.md`：五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明
      - Skill: none
- [ ] <Add> 双索引同步：本轮索引产物清单追加 + 跨轮索引 §报告清单 M1.5 行 + §Finding 追踪新立 ID 行
      - Skill: none
- [ ] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤生产路径 expect 空
      - Skill: none
- [ ] <Proof> 收尾回归：`mvn test -pl module-manufacturing/erp-mfg-service` 复跑全绿
      - Skill: none

Exit Criteria:

- [ ] `ck-mfg-workorder-r3.md` 落盘且五维矩阵 5 格 verdict 完整
- [ ] 双索引行追加在案；零生产代码改动核证通过；mfg service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-08-1042-2-m15-mfg-workorder-five-dim-audit-1-83e0756c to 2026-09-07-171530-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-08-1042-2-m15-mfg-workorder-five-dim-audit-1-83e0756c（补 Closure Gates——按姊妹 M1.1 同批先例为本只读审计计划定制门控；基线「仓库现状」快照刷新至审查时点实核——HEAD `40512c567`→`8825a10e1`、姊妹 plan `2026-09-07-2200-1` 已 ACCEPT 收官、脏面=本批 3 计划；Phase 5 回归锚重指——known-good-baselines 无模块级 mfg 计数行，308 参照改挂 git `1166339ae` + MI 终态行聚合对照面；Phase 5 Targets `<SVC>` 补绑定注记；冻结清单 §1.5/§3.2/§4/§6-E1、MI 终态行数字、r1 统计 21 finding、CAT 31/9、state-machine.md 290 行、三项技能名、`TestErpSeedDataIntegrity`/`validate:flux`/auth 勘误路径逐一实仓复验在盘；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-mfg-workorder-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单 M1.5 行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-manufacturing/erp-mfg-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 fin-1/U20」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-08.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-mfg-workorder-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

## Closure
