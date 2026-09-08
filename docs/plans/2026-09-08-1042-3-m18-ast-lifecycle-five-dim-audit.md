---
status: active
mission: ai-check-r3
work-item: M1.8
group: "2026-09-08-1042"
verify: [test]
---

# 2026-09-08-1042-3 M1.8 assets ast-1 五维符合性审计（资产生命周期切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK report mode CAT1..4 = 0/0/0/0、双 checker 零回归、白名单 27 文件四要素齐备）。M1.8 与 M1.1 同为 M1 首批 deps 满足项；本计划执行顺序居本批第 3。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U06 × 五维 × ast-1**（§4 映射表第 8 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准。
- 切片范围（U06 ast-1）：资产建档/变动/盘点——资产卡片生命周期状态机、Cap/CIP 资本化、Split/Merge、ValueAdjustment、Disposal、Inventory 盘点单生命周期与变动处置；owner doc `docs/design/assets/state-machine.md`（424 行，实仓核验在盘）+ `docs/design/assets/cip.md` + `docs/design/assets/split-merge.md` + `docs/design/assets/use-cases.md`；物理面 `module-assets/erp-ast-{dao,service,web}` 的 `src/main`。
- 切片边界（冻结清单 §3.2 + U06 行）：折旧批量/资产过账/盘点→凭证闭环归 ast-2（M1.9，`depreciation-and-posting.md`）；本切片发现折旧/过账面缺陷时标注「归属 ast-2」归并；涉及 posting 引擎内部时标注「归属 fin-1」；common 抽象族行为缺陷归 U20，本切片只审调用点。
- 跨轮查重源（§2）：r1 `docs/audits/check/ck-assets-lifecycle.md`（C4.4：0 P0 / 6 P1 / 10 P2 / 11 P3，done；P1-CK-ast-001~006 fixed；P2-CK-ast-007/015 fixed，P2-CK-ast-008~014/016 open；P3-CK-ast-017~027 open）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：ast 探针族（CAT-1 39 / CAT-2 29±）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块差距归 successor `ai-check-r3-compliance-baseline-raise`。
- 仓库现状（2026-09-08 实核）：HEAD `40512c567`；**assets 为姊妹 plan `2026-09-07-2200-1`（StateMachine 直抛领域码）已提交批次成员之一（其 Phase 系列已提交至 Phase 5，全仓 3991/0/0/1 full-green 随提交落账）**，当前脏面仅 `docs/lessons/README.md`。审计证据以实跑时 HEAD + 脏面披露为准；若执行期姊妹会话新增在制编辑，按 MI.9 收官审计先例（脏树实跑 + 披露 + 必要时 clone@HEAD 隔离）处置。
- 剩余差距：U06 × 五维 × ast-1 五格 verdict 未落盘；ast-1 无 `-r3` 切片报告。

## Goals

- 按冻结清单对 U06 × 五维 × ast-1 五格全跑（禁止抽样、禁止跳维），逐格落 verdict，产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-lifecycle-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`。
- 全程零生产代码改动（roadmap 规则 6），收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（修复归 M2.x）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 ast-2（折旧/资产过账/盘点→凭证闭环，M1.9）格与其他单元格；折旧与过账面缺陷按边界标注归并不立项。
- 不接管 r1/r2 工作项；不重开既有裁决（lesson 09/10、r1 fixed 项修复方案、StateMachine 直抛码姊妹计划的范围）；不做 roadmap 状态翻转。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用）；盘点注记落本计划勾选注记
> Prereqs: 无（与本批 M1.1/M1.5 计划并行，执行序第 3 不阻塞）

- [ ] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用；roadmap 规则 8），确认索引与冻结清单在位
      - Skill: none
- [ ] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露（assets 为 plan `2026-09-07-2200-1` 已提交批次成员，若有新增在制编辑按 MI.9 先例披露并必要时 clone@HEAD 隔离取证）；后续全部证据注记引用该时点
      - Skill: none
- [ ] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none

Exit Criteria:

- [ ] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [ ] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.8 行指定）
> Targets: `module-assets/erp-ast-dao|erp-ast-service/src/main/java`（ast-1 范围 = 资产卡片/资本化/Split/Merge/VA/Disposal/Inventory 单据族文件）
> Prereqs: Phase 1 完成

- [ ] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族 + codegen 产物安全 + 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
- [ ] <Proof> 15 维度逐维走查 ast-1 范围（重点：②跨实体 I*Biz（fin 凭证/inv 移动调用点）③NopException ⑧资产生命周期状态机 ⑨审批流；折旧/过账面命中标注归属 ast-2，引擎内部标注归属 fin-1）+ blocker/major/minor 分级
      - Skill: nop-platform-conformance-audit-prompt
- [ ] <Proof> 维度⑮ owner-doc 关键断言抽样：`docs/design/assets/state-machine.md` + `cip.md` + `split-merge.md` 中 ≥2 doc × 2 关键断言（状态名/迁移路径/价值守恒规则/ErrorCode）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
- [ ] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-assets-lifecycle.md` 27 条 finding + r2 + 基线快照；r1 open 项同型归并原 ID，fixed 项复用并复核 HEAD 有效性——注意姊妹 StateMachine 直抛码计划已改写 ast 错误码通道，fixed 复核以 HEAD 行为为准）；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt

Exit Criteria:

- [ ] U06×B×ast-1 格 verdict 落盘，15 维度无跳维
- [ ] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 + `docs/design/assets/ui-patterns.md`）
> Targets: `module-assets/erp-ast-web/src/main/resources/_vfs`（ast-1 面：资产台账/变动/盘点页）
> Prereqs: Phase 1 完成

- [ ] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（导出 0 error；325 条既有 variant 外部漂移为 successor 在案，非本切片 finding）+ flux-only grep（AMIS 保留层 / ORM `ext:web-renderer="flux"`）
      - Skill: none
- [ ] <Proof> ast-1 页面走查：资产台账/变动/盘点页对照 view-and-page-strategy（REST `/r/` / M0.4 源头链查表 / `x:extends` 定制 / i18n-en 承载）逐页落 verdict；涉 ast E2E spec 时核对 PageObject + flux 引擎缺省 + 禁 GraphQL 断言
      - Skill: none

Exit Criteria:

- [ ] U06×F×ast-1 格 verdict 落盘；全局面门禁数字在案对账一致
- [ ] ast-1 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md` + `docs/design/assets/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ ast 相关 seed（asset/depreciation_schedule 族）
> Prereqs: Phase 1 完成

- [ ] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿
      - Skill: none
- [ ] <Proof> ast-1 seed 面核对：`git status --porcelain` seed 路径 expect 空 + deploy `_seed_*.sql` 同步义务查登记处表 + asset↔最新 depreciation_schedule 金额自洽抽查（2210-1 批裁决口径）
      - Skill: none

Exit Criteria:

- [ ] U06×S×ast-1 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [ ] seed 零变更 + 同步义务核对 + 金额自洽抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-assets/erp-ast-service`（`<SVC>`）
> Prereqs: Phase 1 完成

- [ ] <Proof> 切片本地回归：`mvn test -pl module-assets/erp-ast-service` 全绿零失败（ast 计数对照 `docs/audits/cjk-baseline.md` §批注账 MI.6 批1 / MI.8 批2 行 `erp-ast 337` 全绿——known-good-baselines 仅载全 reactor 聚合 4006，无 per-module 计数，勿在彼处寻 337；注意姊妹 StateMachine 计划改写 ast 错误码后的矩阵测试断言随其提交已绿）
      - Skill: none
- [ ] <Proof> 覆盖缺口对账：公开方法清单 × `_cases` 测试目录清单逐项对账（ast-1 范围 = 卡片/资本化/Split/Merge/VA/Disposal/Inventory 族 BizModel）；关键业务流清单核对——建档/变动/盘点族清单行逐行核覆盖（折旧→过账链行归 ast-2 切片核对）；缺口按业务关键度定级
      - Skill: none
- [ ] <Proof> 快照纪律：`SnapshotTest.RECORDING` 提交态零残留 + `*` 通配/`delVersion` 屏蔽合规抽查
      - Skill: none

Exit Criteria:

- [ ] U06×T×ast-1 格 verdict 落盘；本地回归全绿数字在案
- [ ] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + ast 白名单条目抽查
> Prereqs: Phase 1 完成

- [ ] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `--self-test` PASS；脚本红 = MI 回归升级报 MI 通道，不立 DIM-I finding
      - Skill: none
- [ ] <Proof> 白名单合规抽查：§WHITELIST ast 相关条目抽 ≥3 条核对四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'` expect 空（§1.5 ④ 全式）；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none

Exit Criteria:

- [ ] U06×I×ast-1 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [ ] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-lifecycle-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [ ] <Decision> 全切片候选 finding 汇总复裁决：三态一致性、级别（P0~P3）、ID 规范（`P{n}-CK-ast-{NNN}-r3`）、归属标注（ast-2/fin-1/U20 归并指针）复核
      - Skill: code-quality-audit-prompt
- [ ] <Add> 落盘 `ck-assets-lifecycle-r3.md`：五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明
      - Skill: none
- [ ] <Add> 双索引同步：本轮索引产物清单追加 + 跨轮索引 §报告清单 M1.8 行 + §Finding 追踪新立 ID 行
      - Skill: none
- [ ] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤生产路径 expect 空
      - Skill: none
- [ ] <Proof> 收尾回归：`mvn test -pl module-assets/erp-ast-service` 复跑全绿
      - Skill: none

Exit Criteria:

- [ ] `ck-assets-lifecycle-r3.md` 落盘且五维矩阵 5 格 verdict 完整
- [ ] 双索引行追加在案；零生产代码改动核证通过；ast service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-08-1042-3-m18-ast-lifecycle-five-dim-audit-1-7f6b4252 to 2026-09-07-171530-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-08-1042-3-m18-ast-lifecycle-five-dim-audit-1-7f6b4252（修正 Phase 5 ast 计数锚点归属——337 权威源为 `cjk-baseline.md` §批注账非 known-good-baselines；补 Closure Gates 只读收官定制门控（MI.9 同系列先例）；Phase 6 ④ 补 `app-erp-all .../i18n/**` 路径对齐冻结清单 §1.5 全式；frontmatter `status: draft` → `active`。基线 HEAD 快照 `40512c567` 已被后续 docs-only 提交推进——计划自身「实跑时 HEAD + 脏面披露」纪律覆盖，快照不改，裁决非缺陷）

## Closure Gates

> 仅在所有阶段执行项与退出标准全部勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动，roadmap 规则 6），验证命令组即结果表面本身（各 Phase 所列红线），完整仓库 build/test 不适用——验证面 = Phase 1 双 checker 红线 + Phase 4/5 局部回归 + Phase 7 收尾复跑。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果（MI.9 先例）。

- [ ] 范围内行为完成：U06 × 五维 × ast-1 五格 verdict 全落盘且无跳维；`ck-assets-lifecycle-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）落盘
- [ ] 相关文档对齐：本轮索引 `ai-check-r3-index.md` 与跨轮 `ai-check-index.md` 追加行与报告实际产物一致；roadmap 状态翻转不适用（Non-Goal）
- [ ] 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode CAT1..4=0 / `--strict` / `--self-test`）+ `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` + `npm run validate:flux`（0 error，325 variant 既有漂移 successor 在案）+ `mvn test -pl module-assets/erp-ast-service` 全绿——各 Phase 红线数字在案对账一致（若 compliance 漂移，按已知失败模式「Compliance 基线漂移」登记移交独立裁决，不就地裁决、不闭包）
- [ ] 无范围内项目降级为 deferred/follow-up（ast-2/fin-1/U20 仅为 finding 归属归并指针，非本计划工作项降级）
- [ ] 独立草案审查已完成并记录（见 Draft Review Record，iteration 1 accept）
- [ ] 文本一致性已验证：frontmatter `status: active` 保持 = ledger 协议，完成态由全勾选 + `## Verification` pass 线 + `## Closure` 回执派生；五格 verdict 与报告统计逐格一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中（Phase 勾选注记含数字红线 + 报告/索引产物 + `## Closure` 审计回执 + 当日 `docs/logs/` 条目）

## Verification

## Closure
