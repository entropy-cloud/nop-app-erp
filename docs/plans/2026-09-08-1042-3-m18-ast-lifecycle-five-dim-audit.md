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

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用；roadmap 规则 8），确认索引与冻结清单在位
      - Skill: none
      - 证据（2026-09-08）：目录在位（M0.3 先例复用），`ai-check-r3-index.md` + `m0-5-audit-checklists.md` 均实仓可解析；本轮产物清单登记路径与本目录一致
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露（assets 为 plan `2026-09-07-2200-1` 已提交批次成员，若有新增在制编辑按 MI.9 先例披露并必要时 clone@HEAD 隔离取证）；后续全部证据注记引用该时点
      - Skill: none
      - 证据：T0 = HEAD `69851bb2dcd72d9183bf66d57e35a8ee3377f211`（基线快照 `40512c567` 已被 docs-only 提交推进，末笔即姊妹 M1.5 计划落盘提交——计划 Draft Review Record 预告情景，纪律覆盖）；`git status --porcelain` **空**（脏面零输出，无姊妹在制编辑，无需 clone 隔离）；后续证据均注记 T0
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据：checker 19 规则 = M0.3 快照行**逐项一致零漂移**（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）；CJK report mode **CAT1..4 = 0/0/0/0**（CAT5 21158 行豁免 informational，exit 0）——与 MI 终态行一致

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.8 行指定）
> Targets: `module-assets/erp-ast-dao|erp-ast-service/src/main/java`（ast-1 范围 = 资产卡片/资本化/Split/Merge/VA/Disposal/Inventory 单据族文件）
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族 + codegen 产物安全 + 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0）：checker 19 规则零漂移（Phase 1 数字）；反模式族 `extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0 / `@Transactional` 全域仅 AssetPostingExecutor javadoc 引注（真实共存=0）/ `IDaoProvider|IOrmTemplate` 命中点逐一核对（Processor 编排范式 + `orm()` 仅 flushSession + Dashboard/Report 有豁免 javadoc；唯一无理由跨实体站点 → 新立 028-r3）；`git status _gen/` 空 + `__XGEN_FORCE_OVERRIDE__` 35 处全为 codegen dict.yaml 只读校验点零手改；E1 路径聚合器含 `/erp/ast/auth/erp-ast.action-auth.xml` + `x:extends` ×2
- [x] <Proof> 15 维度逐维走查 ast-1 范围（重点：②跨实体 I*Biz（fin 凭证/inv 移动调用点）③NopException ⑧资产生命周期状态机 ⑨审批流；折旧/过账面命中标注归属 ast-2，引擎内部标注归属 fin-1）+ blocker/major/minor 分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：15/15 无跳维（⑫指针 DIM-T）——②跨实体 I*Biz 在位（capitalizationBiz/disposalBiz/scheduleBiz/mntEquipmentBiz @Nullable/voucherBiz Facade；例外站点 028-r3）③NopException+ErrorCode 集中（ErpAstErrors @Locale("zh-CN")，全链零 RuntimeException）⑧状态机 Bean 化（Asset 11 命名边直抛领域码/Inventory 6 动作/Movement 退化轴 owner doc 裁定/Split-Merge DISPOSED=owner doc §适用对象四 §3 内部重组终态）⑨审批流（xbiz auth permissions + Approval/Document 双轴 Bean + per-mutation 透传）；折旧/过账面命中已标注归属 ast-2/fin-1（报告 §2.4）；严重性：0 blocker / 0 major / 1 minor（028-r3）
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`docs/design/assets/state-machine.md` + `cip.md` + `split-merge.md` 中 ≥2 doc × 2 关键断言（状态名/迁移路径/价值守恒规则/ErrorCode）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
      - 证据：3 doc × 7 断言——state-machine.md：①§1/§2 状态集 {DRAFT,IN_SERVICE,IDLE,SCRAPPED,SOLD}+终态+迁移路径 ↔ AssetStateMachine `transitions()` 11 边一致；②§2 suspend 行「remark『闲置自 {date}』强制记录」↔ SuspendResumeProcessor L35/L48-49 一致；③§适用对象四 §4「reverseApprove 无条件抛 ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED」↔ SplitReverseApproveProcessor require 后直抛一致。cip.md：④「预计净残值按资产类别比例计算」↔ Cap L241 `setResidualValue(ZERO)` + Category 无 residualRate = **漂移**（= 既有 open P3-CK-ast-017）；⑤三态 DRAFT→IN_CONSTRUCTION→TRANSFERRED ↔ CipProcessor + StartConstruction 一致。split-merge.md：⑥§关键业务规则 5 不可逆契约 ↔ ③同源一致；⑦「剩余折旧期间按加权平均剩余期间取整」↔ Merge `resolveUsefulLifeMonths` L290-309 一致（P1-006 修复后对齐）。漂移 1 处 < 2，未触发扩样
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-assets-lifecycle.md` 27 条 finding + r2 + 基线快照；r1 open 项同型归并原 ID，fixed 项复用并复核 HEAD 有效性——注意姊妹 StateMachine 直抛码计划已改写 ast 错误码通道，fixed 复核以 HEAD 行为为准）；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决：27 条历史 ID 逐一比对——**复用 8**（P1-001..006 + P2-007/015，F1.2/F1.3/F2.8 修复 T0=HEAD `69851bb2` 实测在位，报告 §2.1 引行号证据）/ **归并 19**（P2-008..014+016 + P3-017..027，逐一现场复核仍 open，报告 §2.2）/ **新立 1**（P3-CK-ast-028-r3，维度② AssetBizModel daoFor 直查 ActionLog，同型注记 P2-CK-fin-007 跨单元另立——按 r1 ast-007/ast-009 先例）；历史 ID 零覆写；替代方案（跨格归并至 fin-007）因跨单元裁决先例（r1 分域立项惯例）否决；残留风险：028-r3 与 fin-007 同族全仓扫描面归 U20/M1.15

Exit Criteria:

- [x] U06×B×ast-1 格 verdict 落盘，15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 + `docs/design/assets/ui-patterns.md`）
> Targets: `module-assets/erp-ast-web/src/main/resources/_vfs`（ast-1 面：资产台账/变动/盘点页）
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（导出 0 error；325 条既有 variant 外部漂移为 successor 在案，非本切片 finding）+ flux-only grep（AMIS 保留层 / ORM `ext:web-renderer="flux"`）
      - Skill: none
      - 证据：step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；[3/4] `files=855 validated=855 errors=325`——325 条逐条 `variant="primary"`×dropdown-button 同型（非变体过滤零逃逸），ast 域 15 条同族成员，整体 exit 1 余项即既有外部漂移（successor 在案）；`component="AMIS"` 保留层 0、`grep -L ext:web-renderer` 空
- [x] <Proof> ast-1 页面走查：资产台账/变动/盘点页对照 view-and-page-strategy（REST `/r/` / M0.4 源头链查表 / `x:extends` 定制 / i18n-en 承载）逐页落 verdict；涉 ast E2E spec 时核对 PageObject + flux 引擎缺省 + 禁 GraphQL 断言
      - Skill: none
      - 证据：台账 `ErpAstAsset.view.xml` 保留层 `x:extends="_gen/…"` + bounded-merge + `i18n-en:label` 全覆盖 + GenPage 源头链（M0.4 row 35/36 禁改义务在位）；盘点页 `@query:ErpAstInventory__findPage`（REST 约定）；dashboard 孪生 `.flux.yaml`/`.page.yaml` 参数一致（periodId，无 mfg-023 型孪生漂移）；4 手写页 i18nEn 在位（13/17/16/2，MI.8 修复态）；页面 graphql 0；E2E：ast spec（inventory-count/value-adjustment）违规选择器 0、GraphQL 命中 = runbook L122 豁免 + L229 登记 API 驱动通道、`E2E_ENGINE` 缺省 flux（engine.ts L8-13）

Exit Criteria:

- [x] U06×F×ast-1 格 verdict 落盘；全局面门禁数字在案对账一致
- [x] ast-1 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md` + `docs/design/assets/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ ast 相关 seed（asset/depreciation_schedule 族）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿
      - Skill: none
      - 证据：**Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**
- [x] <Proof> ast-1 seed 面核对：`git status --porcelain` seed 路径 expect 空 + deploy `_seed_*.sql` 同步义务查登记处表 + asset↔最新 depreciation_schedule 金额自洽抽查（2210-1 批裁决口径）
      - Skill: none
      - 证据：seed 路径 dirty **空**（双面快照重录义务未触发）；资产清点 **372 CSV + 1 SQL**（= 冻结口径）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族且均已在 seed-data.md §97 登记处已聚合（ast 无 deploy seed 零同步义务）；自洽抽查 2 资产——id=2（AST-2026-002：120000/6000/114000）↔ 最新 schedule（2026-07 EXECUTED accum=6000/NBV=114000）双字段全等（seed-data.md L193 约束）+ id=1/3（accum=0 无 schedule 行）内部自洽

Exit Criteria:

- [x] U06×S×ast-1 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 金额自洽抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-assets/erp-ast-service`（`<SVC>`）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-assets/erp-ast-service` 全绿零失败（ast 计数对照 `docs/audits/cjk-baseline.md` §批注账 MI.6 批1 / MI.8 批2 行 `erp-ast 337` 全绿——known-good-baselines 仅载全 reactor 聚合 4006，无 per-module 计数，勿在彼处寻 337；注意姊妹 StateMachine 计划改写 ast 错误码后的矩阵测试断言随其提交已绿）
      - Skill: none
      - 证据：**Tests run: 339, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（×2 复跑同值）——锚点 337（cjk-baseline §批注账 MI.6 批1 行实核在案）零失败零回归；339 = 337 + 姊妹 StateMachine 直抛码计划矩阵测试增量（计划基线已预告），计数增长良性
- [x] <Proof> 覆盖缺口对账：公开方法清单 × `_cases` 测试目录清单逐项对账（ast-1 范围 = 卡片/资本化/Split/Merge/VA/Disposal/Inventory 族 BizModel）；关键业务流清单核对——建档/变动/盘点族清单行逐行核覆盖（折旧→过账链行归 ast-2 切片核对）；缺口按业务关键度定级
      - Skill: none
      - 证据：`_cases` 19 族逐项对账——Asset 3 动作↔CrudSmoke+IdleStateMachine+ExtFieldsAndAuditTrail；Cap 5 审批动作（xbiz 委托）↔TestErpAstCapitalization；Cip 7↔CipTransfer；Split/Merge 各 6↔SplitMerge（10 测试含 001/002/006 修复回归行 + reverse-not-supported 负路径）；VA 6↔ValueAdjustment（8 测试含 005 上限 + F1.2 配对）；Disposal↔Disposal×3 族；Inventory 8 动作↔Inventory（6 测试含 003 回归行）+ E2E ast-inventory-count；Movement↔MovementReverseApprove；**缺口 = 0**；折旧→过账链行（Depreciation/CatchUp/PostingReverse 族在位且全绿）归 ast-2 切片逐行核对
- [x] <Proof> 快照纪律：`SnapshotTest.RECORDING` 提交态零残留 + `*` 通配/`delVersion` 屏蔽合规抽查
      - Skill: none
      - 证据：`grep -rn "SnapshotTest.RECORDING" src/test/java` = **0**；`delVersion` 命中仅 javadoc 注解（TestErpAstExtFieldsAndAuditTrail L358）、`*` 通配零滥用；339 全绿 = CHECKING 态等价证明

Exit Criteria:

- [x] U06×T×ast-1 格 verdict 落盘；本地回归全绿数字在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + ast 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `--self-test` PASS；脚本红 = MI 回归升级报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据：`--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 baseline files，单向收紧成立）；`--self-test` **PASS**（自证断言全绿）
- [x] <Proof> 白名单合规抽查：§WHITELIST ast 相关条目抽 ≥3 条核对四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'` expect 空（§1.5 ④ 全式）；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none
      - 证据：抽样 4 条（≥3，批 1/2 assets 段全抽）——`IErpAstAssetBiz`（@Description 专属 C1）/ `ErpAstDashboardBizModel`（C1）/ `ErpAstAssetBizModel`（混合文件 C1+C2③）/ `ErpAstAssetSuspendResumeProcessor`（IDLE_SINCE_PREFIX="闲置自 " C2 契约）——**4/4 四要素齐备**（路径/理由/owner doc 指针 i18n-compliance.md/裁决来源 plan 2026-09-07-0902-1），实仓字符串逐条复核在位零登记缺陷；`grep -L "@Locale"` *Errors.java = 空；meta/i18n 路径 dirty = 空

Exit Criteria:

- [x] U06×I×ast-1 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-lifecycle-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：三态一致性、级别（P0~P3）、ID 规范（`P{n}-CK-ast-{NNN}-r3`）、归属标注（ast-2/fin-1/U20 归并指针）复核
      - Skill: code-quality-audit-prompt
      - 裁决：复裁决一致——新立 1（P3-CK-ast-028-r3，ID 规范合规，DIM-B②，同型注记 P2-CK-fin-007）/ 复用 8 / 归并 19；级别复核（028-r3 P3 定档：只读+同模块+I*Biz 现成，对齐 fin-007 P2 跨域子型降档理据）；归属标注复核（posting dispatcher/Provider 族与折旧引擎本体 → ast-2；引擎内部 → fin-1；AbstractErpCrudBizModel/AbstractReverseApproveProcessor 基类 → U20；聚合横切 → U21——报告 §2.4）；替代方案（028-r3 归并 fin-007 跨格）已否决并记录理据（r1 跨单元同型另立先例 ast-007/ast-009）
- [x] <Add> 落盘 `ck-assets-lifecycle-r3.md`：五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明
      - Skill: none
      - 证据：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-lifecycle-r3.md` 落盘——五维矩阵 5/5（B=finding / F=pass / S=pass / T=pass / I=pass）+ §2 三态裁决（8 复用 / 19 归并 / 1 新立，历史 27 ID 零覆写）+ §3 统计表 + §4 剩余风险四件套（已查/未深查/残留风险/successor 触发条件）
- [x] <Add> 双索引同步：本轮索引产物清单追加 + 跨轮索引 §报告清单 M1.8 行 + §Finding 追踪新立 ID 行
      - Skill: none
      - 证据：`ai-check-r3-index.md` 产物清单追加 M1.8 行；`ai-check-index.md` §报告清单追加 `ck-assets-lifecycle-r3.md` M1.8 行 + §Finding 追踪追加 `P3-CK-ast-028-r3` 行（紧随 mfg r3 新立 ID 行区）
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤生产路径 expect 空
      - Skill: none
      - 证据：脏面 = 3 个审计 docs 文件（本轮索引 M + 跨轮索引 M + 本报告 ??），生产路径过滤（非 docs/）**空**
- [x] <Proof> 收尾回归：`mvn test -pl module-assets/erp-ast-service` 复跑全绿
      - Skill: none
      - 证据：复跑 **Tests run: 339, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（与首跑同值零回归）

Exit Criteria:

- [x] `ck-assets-lifecycle-r3.md` 落盘且五维矩阵 5 格 verdict 完整
- [x] 双索引行追加在案；零生产代码改动核证通过；ast service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-08-1042-3-m18-ast-lifecycle-five-dim-audit-1-7f6b4252 to 2026-09-07-171530-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-08-1042-3-m18-ast-lifecycle-five-dim-audit-1-7f6b4252（修正 Phase 5 ast 计数锚点归属——337 权威源为 `cjk-baseline.md` §批注账非 known-good-baselines；补 Closure Gates 只读收官定制门控（MI.9 同系列先例）；Phase 6 ④ 补 `app-erp-all .../i18n/**` 路径对齐冻结清单 §1.5 全式；frontmatter `status: draft` → `active`。基线 HEAD 快照 `40512c567` 已被后续 docs-only 提交推进——计划自身「实跑时 HEAD + 脏面披露」纪律覆盖，快照不改，裁决非缺陷）

## Closure Gates

> 仅在所有阶段执行项与退出标准全部勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动，roadmap 规则 6），验证命令组即结果表面本身（各 Phase 所列红线），完整仓库 build/test 不适用——验证面 = Phase 1 双 checker 红线 + Phase 4/5 局部回归 + Phase 7 收尾复跑。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果（MI.9 先例）。

- 范围内行为完成（U06 × 五维 × ast-1 五格 verdict 全落盘且无跳维；`ck-assets-lifecycle-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）落盘）
      - 核对：报告 §1 矩阵 5/5（B=finding / F=pass / S=pass / T=pass / I=pass）、15/15 维度无跳维注记、§3 统计（新立 1 / 复用 8 / 归并 19）与 §2 逐条裁决一致
- 相关文档对齐（本轮索引 `ai-check-r3-index.md` 与跨轮 `ai-check-index.md` 追加行与报告实际产物一致；roadmap 状态翻转不适用（Non-Goal））
      - 核对：本轮索引产物行 + 跨轮索引 §报告清单 M1.8 行（`0|0|0|1`——初版 `0|0|1|0` P3 误植 P2 列经闭包审计 B-1 修正）+ §Finding 追踪 P3-CK-ast-028-r3 行；roadmap M1.8 保持 `todo` + 行内执行完成证据（同批 M1.1/M1.5 先例）
- 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode CAT1..4=0 / `--strict` / `--self-test`）+ `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` + `npm run validate:flux`（0 error，325 variant 既有漂移 successor 在案）+ `mvn test -pl module-assets/erp-ast-service` 全绿——各 Phase 红线数字在案对账一致（若 compliance 漂移，按已知失败模式「Compliance 基线漂移」登记移交独立裁决，不就地裁决、不闭包）
      - 核对：全部 PASS，数字见 `## Verification`（8 条执行期 PASS 线 + 闭包 visit `pass test` 全仓复跑线）；checker 零漂移未触发移交条款
- 无范围内项目降级为 deferred/follow-up（ast-2/fin-1/U20 仅为 finding 归属归并指针，非本计划工作项降级）
      - 核对：报告 §2.4 归属标注均为 §3.2 共享代码边界归并指针；19 open 归并项 + 1 新立项全部保留在 finding 追踪（无降级、无静默移出）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1 accept）
- 文本一致性已验证：frontmatter `status: active` 保持 = ledger 协议，完成态由全勾选 + `## Verification` pass 线 + `## Closure` 回执派生；五格 verdict 与报告统计逐格一致
      - 核对：frontmatter 零改动；报告 §1 verdict 列 × §3 统计 × 双索引行 × 日志条目四方一致（闭包审计第 2/3 项独立复核）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - 核对：第一轮独立闭包审计 `#audit-20260908-1411-m18-ast-lifecycle-1-b4e2c9a7`（subagent task `ses_f806211abffeGH0nZIJRY20gP4`，fresh session，read-only）判 REJECT（blocking B-1 = 跨轮索引 M1.8 行 P3 计数误植 P2 列）；执行者修正 B-1 + 同批勘误 M1.5 行列序（附勘误注记）后，第二轮独立闭包审计 `#audit-20260908-1415-m18-ast-lifecycle-2-87969a78`（subagent task `ses_f805954adffeVsl0cbu2BEj6NH`，fresh session，light scope：remedy + no-regression 5 项全 PASS）判 **ACCEPT**。执行者未自我审计，门控由审计回执支撑勾选
- 结束证据存在于文件中（Phase 勾选注记含数字红线 + 报告/索引产物 + `## Closure` 审计回执 + 当日 `docs/logs/` 条目）
      - 核对：7 Phase 勾选注记（含全部红线数字）+ 报告/双索引/roadmap 产物 + 本节回执 + `docs/logs/2026/09-08.md` 顶部 M1.8 条目（含收官回执注记）全部在案

## Verification

- pass test 20260908-1441-closure-r1 exit=0

> 闭包 visit 记录：全仓 `mvn test` BUILD SUCCESS（exit 0，Finished at 2026-09-08T14:41:26+08:00，surefire 聚合 3991/0/0/1——1 条为既有 `@Disabled` skip，与 MI 终态行 3991/0/0/1 模式一致零回归）；`mvn clean install -DskipTests` 156/156 BUILD SUCCESS（14:24:46）。以下 8 条为执行期各 Phase 红线的 PASS 记录，闭包 visit 复跑覆盖其收官态等价。

- PASS 2026-09-08 `bash docs/audits/nop-compliance-checker.sh`——19 规则 = M0.3 快照行逐项一致零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）
- PASS 2026-09-08 `node tools/check-hardcoded-cjk.mjs`（report mode）——CAT1..4 = 0/0/0/0（= MI 终态行，exit 0）
- PASS 2026-09-08 `node tools/check-hardcoded-cjk.mjs --strict`——exit 0，0 new violations vs 冻结 SNAPSHOT（170 baseline files）
- PASS 2026-09-08 `node tools/check-hardcoded-cjk.mjs --self-test`——PASS（自证断言全绿）
- PASS 2026-09-08 `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity`——Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS
- PASS 2026-09-08 `npm run validate:flux`——step [1/3] FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855；[3/4] files=855 validated=855 errors=325（325 条逐条为既有 `variant="primary"`×dropdown-button 外部漂移族，successor 在案，非本切片 finding；整体 exit 1 余项即此）
- PASS 2026-09-08 `mvn test -pl module-assets/erp-ast-service`（×2 首跑 + 收尾复跑）——Tests run: 339, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS（锚点 cjk-baseline §批注账 MI.6 批1 行 erp-ast 337 全绿，零失败零回归）
- PASS 2026-09-08 `git status --porcelain` 生产路径过滤——空（脏面仅审计 docs 产物：本轮索引 M + 跨轮索引 M + 本切片报告 ?? + 计划/日志编辑）
- 说明：本计划为只读审计（零生产代码改动，roadmap 规则 6），验证命令组即结果表面本身（Closure Gates 定制门控），完整仓库 build/test 不适用

## Closure

Status Note: 本计划（只读五维审计切片，零生产代码改动）全部 7 Phase 执行项与退出标准 `[x]`（数字红线注记在案）；Closure Gates 8/8 普通条目核对通过；`## Verification` 8 条执行期 PASS 线 + 闭包 visit 全仓复跑 `pass test` 线（双 checker 零漂移对账 + seed 门禁 4/4 + flux 导出 0 error + ast service 339/0/0/0 ×2 全绿 + 闭包 visit 全仓 `mvn test` 3991/0/0/1 全绿 + 生产路径零触碰核证）。ledger 协议：frontmatter `status: active` 保持，完成态由本节回执 + 全勾选派生。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理两轮（均 fresh session、read-only、非执行者上下文）
  - 第一轮（全量 7 项）：`#audit-20260908-1411-m18-ast-lifecycle-1-b4e2c9a7`（subagent task `ses_f806211abffeGH0nZIJRY20gP4`，2026-09-08 14:11）——verdict **REJECT**，blocking B-1（跨轮索引 §报告清单 M1.8 行 `0|0|1|0` 将唯一新立 P3-CK-ast-028-r3 误植 P2 列）；其余 6 项（Phase 完整性 14+ 注记实仓复验 / 三态裁决抽验 6 条 / 零生产改动 / anti-hollow / deferred honesty / 红线复跑）全 PASS
  - 执行者整改：B-1 修正（M1.8 行 → `0|0|0|1`）+ 审计建议的索引卫生同批勘误（M1.5 行 `1|1|0|0` → `0|1|0|1`，附 inline 勘误注记声明列序误植与修正来源）
  - 第二轮（light re-audit：remedy + no-regression 5 项）：`#audit-20260908-1415-m18-ast-lifecycle-2-87969a78`（subagent task `ses_f805954adffeVsl0cbu2BEj6NH`，2026-09-08 14:15）——verdict **ACCEPT**（B-1 exactly cured；一致性三角 report↔Finding 追踪↔索引行吻合；索引其余行零扰动（M1.1/md/mfg/ast r1 行抽验）；脏面仅 docs/；plan frontmatter `status: active` 保持、回执未预写）
- Evidence: 本计划勾选注记 + `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-lifecycle-r3.md` + 双索引追加行 + `docs/backlog/ai-check-r3-roadmap.md` M1.8 行 + `docs/logs/2026/09-08.md` M1.8 条目

- dispatch audit #audit-20260908-1441-m18-ast-lifecycle-1-bc20ef3e to 2026-09-07-171530-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260908-1441-m18-ast-lifecycle-1-bc20ef3e：独立闭包审计 ACCEPT——35/35 勾选全绿，五维矩阵 5/5 verdict 落盘（B=finding（新立 P3-CK-ast-028-r3）/ F/S/T/I=pass；三态裁决 复用 8 / 归并 19 / 新立 1，历史 27 ID 零覆写，跨轮索引 M1.8 行 `0|0|0|1` B-1 修正复核在位）；闭包 visit 实跑全仓 `mvn test` BUILD SUCCESS exit=0（surefire 聚合 3991/0/0/1，1 条为既有 `@Disabled` skip）+ `mvn clean install -DskipTests` 156/156 BUILD SUCCESS + `git status` 生产路径零触碰核证（只读审计零改动红线保持）；plan-check `--strict` derivedCompleted 成立

Follow-up:

- 无本计划工作项降级。§2.2 归并 19 条 open + §2.3 新立 P3-CK-ast-028-r3 的修复归 M2.x（assets 修复批，先写失败测试）；P3-CK-ast-028-r3 与 P2-CK-fin-007 同族全仓扫描面归 U20/M1.15 裁决；ast-2 切片（M1.9）承接折旧→过账链行核对与过账面归并指针。
