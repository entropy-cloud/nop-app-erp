---
status: active
mission: ai-check-r3
work-item: M2.4
group: "2026-09-10-0705"
verify: [test]
---

# 2026-09-10-0705-2 M2.4 assets P1 修复批（域批独立项 P2-CK-ast2-024-r3 CATCHUP 汇总凭证引擎侧红冲闭环）

## Current Baseline

- 批内面 = 恰 1 条：`P2-CK-ast2-024-r3`（open，`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-assets-depreciation-r3.md`；跨轮索引行在案）。assets 域 r3 新立 P1 = 0（M1.8 新立 1×P3、M1.9 新立 2×P2 + 1×P3），无同批可合并的其他 P1——本域批 charter 为 P1 修复批，r3 语义下实际承载 = M2.0 族裁决路由的域批独立项（裁决表 row 69：ast2-024-r3 → M2.4，与分片 3 ast2-026-r3 修复同批联动）。
- 其余 assets 域 r3 新立 finding 去向（M2.0 族裁决，均非本批）：ast2-026-r3（D·分片3，测试断言依赖本批修复后语义）、ast2-025-r3（doc 批·分片5）、ast-028-r3（A·分片2）。
- 缺陷链（执行者 2026-09-10 HEAD 实核在位）：`module-assets/erp-ast-service/src/main/java/app/erp/ast/service/posting/ErpAstDepreciationReversalListener.java` L45 `CATCHUP_SUFFIX = "#CATCHUP"` + L65-68 `rollbackDepreciationSchedule` 对 billHeadCode 后缀 `#CATCHUP` 静默 return（L36/L66 注释自述「静默跳过」）；而 GL 实存 CATCHUP 汇总凭证由 `DepreciationPostingDispatcher`（finding 报告所记 Dispatcher L169-171）+ `DepreciationAcctDocProvider` 落行，billHeadCode = `资产码#当期#CATCHUP`、businessType 仍 DEPRECIATION——财务员红冲 CATCHUP 凭证 → 引擎派发通道命中静默 return → 计划行滞 posted=true/资产累计不回退且无告警。
- 自愈双断：catchUp 补提幂等跳过 no-op（已提期不重提）；逐期 `reverseDepreciation` 红冲键 `资产码#漏提期` 与实存汇总键 `资产码#当期#CATCHUP` 失配 → `ERR_REVERSE_SOURCE_NOT_FOUND`。两条自愈路径均不可达闭环。
- 谱系与分立：F2.9（ast2-005，fixed）修复体之残留面——汇总凭证子集新控制点（ast2-005 已 fixed 不适用归并）；ast2-022（域内重试通道缺口，凭证从未成功）与本条（GL 侧红冲回退缺口，凭证成功后被红冲）分立不归并。正操作锚点：`ErpAstDepreciationScheduleCatchUpDepreciationProcessor#backfillCatchUpSchedules`（L171/L192；`ErpAstDisposalProcessor` L162 亦消费）即「落行时按 voucherId 回填计划行」的逆操作参照。测试现状：`TestErpAstCatchUpDepreciation` 覆盖补提正路径，红冲侧零覆盖。
- 联动义务（裁决 §2.4）：ast2-026-r3（ReversalListener 引擎派发通道全仓零测试，M2.8 分片3）的测试断言依赖本批修复后语义——本批落修复 + 语义注记供分片3 消费；本批 Phase 1 失败测试自身覆盖引擎派发通道的最小行为断言，但不重复立项 026。
- 修复方法约束（r3 M2 前言 + M2.0 owner doc `docs/architecture/finding-remediation-method.md`，其为本计划直接 Prereq）：先写失败测试 → 修复 → 测试绿 + 既有测试零回归；错误参数传码不传散文（沿 `ErpAstErrors` 既有域码模式）；本批零 ORM/api.xml/seed/页面变更（可逆锚点不得引入新列——走 billHeadCode 既有约定或 voucherId 反查）；折旧过账引擎（会计过账族）= plan-first + owner doc + tests（本计划即载体，owner doc `docs/design/assets/depreciation-and-posting.md`）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（全 reactor 基线，surefire 口径真值 3991/0/0/1）；compliance R2b=242/R2c=1542/R12a=71；CJK `--strict` PASS（0/0/0/0）。ast service 模块参照 339/0/0/0（M1.8/M1.9 切片锚点 = 批注账 337 + 姊妹 StateMachine 计划增量）。
- 剩余差距：CATCHUP 凭证红冲 → 计划行/资产累计回退闭环缺失 + 红冲侧零测试；M2.9 收官需本批 fixed 证据指针。
- 依赖状态：M2.0（plan `2026-09-10-0425-1`）已完成；本计划组 `2026-09-10-0705` 内执行序 N=2。

## Goals

- `P2-CK-ast2-024-r3` 修复落地：红冲 CATCHUP 汇总凭证后，引擎侧按修复方案（裁决见 Phase 2 Decision）回退全部相关计划行（posted=false/REVERSED 语义与逐期红冲先例一致）并回退资产累计折旧，闭环达成且无告警黑洞（方案若含显式不可回退分支，须有明确错误而非静默）。
- 先写失败测试：红冲 CATCHUP 凭证场景断言计划行回退 + 资产累计回退，先红后绿；逐期红冲既有路径（`资产码#漏提期` 键）回归保持绿。
- 零回归 + 门控持平：ast service 套件全绿 + 全 reactor `mvn test` 零新增失败；compliance/cjk 双 checker 不高于基线；seed 零改动证明。
- M2.9 消费证据落盘：fixed 证据指针 + ast2-026-r3 语义联动注记（供 M2.8 分片3 消费）+ 零触碰声明。

## Non-Goals

- 不修 assets 域其他 r3 finding（ast2-025/026-r3、ast-028-r3——去向以 M2.0 族裁决为准；026 测试批属 M2.8 分片3 义务）。
- 不修 ast2-022（域内重试通道，分立不归并）、不重开 ast2-015 §5.1/§十 互斥等既有裁决、不动 F2.9 已修面（ast2-001..006 修复复用态）。
- 不改 ORM/api.xml/seed/页面文件（可逆锚点限定既有字段约定）；不做双索引状态回填（M2.9 义务）；不做 roadmap 状态翻转。
- 不重构 ReversalListener 派发协议与 Dispatcher 落行格式（`资产码#当期#CATCHUP` 键结构保持，最小面接入回退逻辑）。

## Phase 1 — 失败测试先行（CATCHUP 红冲回退场景）

> 统一类型：Proof（2 项 Proof）。
> Skill: bug-diagnosis-prompt（roadmap M2.4 行指定——同 M2.3 行；先读 `docs/skills/bug-diagnosis-prompt.md` 四阶段定位纪律再动手）
> Targets: `module-assets/erp-ast-service/src/test/java/`（扩展 `TestErpAstCatchUpDepreciation` 或新增红冲侧测试类，落点执行时按现有测试布局选定；沿 `nop-testing` skill 快照/断言范式）
> Prereqs: M2.0 计划完成（修复方法 owner doc 在位）

- [ ] <Proof> 写失败测试：构造 CATCHUP 补提 → GL 凭证落行（`资产码#当期#CATCHUP`）→ 经引擎派发通道红冲该凭证（voucherBiz.reverse 链路，非域内直调 listener）的完整场景，断言：相关计划行回退（posted 标志/状态语义与逐期红冲先例一致）+ 资产累计折旧回退 + 无静默分支残留；对照组 = 逐期凭证红冲既有绿路径保持。断言可观察行为（实体重读状态），非仅类型。执行确认红：现静默 return → 计划行滞 posted=true 断言失败，失败输出记入勾选注记（缺陷复现证据）。
      - Skill: bug-diagnosis-prompt
- [ ] <Proof> 复现定位记录：按 bug-diagnosis 四阶段将缺陷链（listener 静默站点 L65-68 × Dispatcher 落行键结构 × 逐期红冲键失配点）与 finding 报告证据对账；实核 `backfillCatchUpSchedules` 回填结构与汇总凭证行对应关系（逆操作可行性证据）；确认无第二红冲入口遗漏（grep businessType=DEPRECIATION 红冲派发面注记）。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [ ] CATCHUP 红冲失败测试在位且当前树红（计划行/累计回退断言失败），失败输出注记在案；逐期红冲对照组绿
- [ ] 缺陷链与 finding 报告逐站对账一致，backfill 逆操作结构实核在案，无未登记第二红冲入口

## Phase 2 — 闭环修复（Decision + Fix）

> 统一类型：Decision | Fix | Add（1 Decision + 1 Fix + 1 Add）。
> Skill: bug-diagnosis-prompt
> Targets: `ErpAstDepreciationReversalListener.java`（rollbackDepreciationSchedule CATCHUP 分支）；回退 helper 复用/抽取落点（`ErpAstDepreciationScheduleCatchUpDepreciationProcessor`，protected step 沿 Processor 范式）由本阶段 Decision 裁决；owner doc 对账面 = `docs/design/assets/depreciation-and-posting.md`（红冲语义节；ast2-025-r3 已登记其漂移簇——本批仅对账，不修 doc[doc 批义务]）
> Prereqs: Phase 1 完成（失败测试在位）

- [ ] <Decision> CATCHUP 回退方案裁决：推荐 (a) listener 识别 `#CATCHUP` 后按 voucherId 反查全部汇总凭证行 → 映射回计划行聚合回退（`backfillCatchUpSchedules` 逆操作，逐行 posted/状态回退 + 资产累计回退，镜像逐期红冲语义）。替代方案：(b) 落行时登记 `#CATCHUP#漏提期` 粒度可逆锚点（改 Dispatcher 落行键结构 + 历史存量凭证无锚点不可回退，需迁移面）——被否，扩大落行格式变更面且不解决存量；(c) 保持静默 + 告警日志——被否，闭环缺陷未修仅降噪，违反先红后绿义务。残余风险：(a) 对 Phase 2 之前已存在的滞留 posted=true 存量计划行无追溯回退（运行数据非本仓证据面），在注记显式登记；若 owner doc 对账发现红冲语义已声明其他预期，以 doc 为先并记录。裁决与 owner doc 对账：核对 `depreciation-and-posting.md` 红冲节语义一致；发现漂移即登记注记归 ast2-025-r3 doc 批（不扩本批 scope 落 doc 修订，除非漂移直接阻塞方案语义——此时先修 doc 并注记）。
      - Skill: bug-diagnosis-prompt
- [ ] <Fix> 接入回退逻辑：`rollbackDepreciationSchedule` 的 CATCHUP 分支按 Decision 方案落地聚合回退；行为不变式：非 CATCHUP（逐期）路径逐字节现状保持（既有绿测试为证）。
      - Skill: bug-diagnosis-prompt
- [ ] <Add> 回退不可达时的显式错误（若方案 (a) 实核后发现个别行无法映射回计划行——如数据残缺）：`ErpAstErrors` 增域内错误码（中文描述合规 + `@Locale("zh-CN")` 接口注解已在位[MI.1]；异常参数传码不传散文——MI.5a CAT-2 契约），禁止恢复静默分支；若实核后全部行均可映射（无不可达分支），本项以注记记录「无需新增错误码」收口，不留空转。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [ ] Phase 1 CATCHUP 红冲测试全绿（计划行回退 + 资产累计回退断言通过）+ 逐期红冲/补提正路径既有测试保持绿
- [ ] 守卫/回退落在 listener 分支层、Dispatcher 落行键结构零改动；Decision 三要素注记在案；owner doc 对账结论在案

## Phase 3 — 批级证明与收官门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [ ] <Proof> 域级回归：`mvn test -pl module-assets/erp-ast-service -am` 全绿零新增失败（参照 339/0/0/0，数字记入注记；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）。
      - Skill: none
- [ ] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行，surefire 口径真值 3991/0/0/1）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（`git ls-files` 同路径非空集，证明构成）+ `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: none
- [ ] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P2-CK-ast2-024-r3 fixed 证据指针（测试类/方法 + 回退站点）；ast2-026-r3 语义联动注记（修复后 CATCHUP 分支可观察语义描述 + 测试指针，供 M2.8 分片3 消费）；存量滞留行无追溯回退残余风险登记；seed/ORM/api.xml 零触碰声明（`git status --porcelain` 全树无 `module-*/model/`、`*.api.xml`、`_init-data/`、页面命中）。
      - Skill: none

Exit Criteria:

- [ ] ast service 全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案
- [ ] M2.9 消费证据四件（fixed 指针 / 026 联动注记 / 残余风险登记 / 零触碰声明）落盘于计划注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-0705-2-m24-assets-catchup-reversal-closure-fix-1-f815ce76 to opencode-reviewer-session-2026-09-10
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-0705-2-m24-assets-catchup-reversal-closure-fix-1-f815ce76（零 Blocker；2 Minor 已修：Phase 2 统一类型补 Add、Targets「必要时」改绑本阶段 Decision 裁决；基线锚点 HEAD 实核逐一命中——ReversalListener `CATCHUP_SUFFIX` L45 + CATCHUP 分支静默 return 站点、finding `P2-CK-ast2-024-r3` 登记于 `ck-assets-depreciation-r3.md` §2.3、M2.0 族裁决表 row 69（ast2-024-r3 → M2.4 域批独立项 + ast2-026-r3 → 分片 3 联动）实存于 `m2-0-family-adjudication.md`、`ErpAstErrors`/`TestErpAstCatchUpDepreciation`/`backfillCatchUpSchedules`（CatchUpDepreciationProcessor + DisposalProcessor 消费）在位、known-good-baselines 2026-09-09 行 R2b=242/R2c=1542/R12a=71 + CJK strict PASS 与计划引用一致、surefire 口径真值 3991 与 M1.17 闭包审计裁决（基线行 4006 系日志拼接重复计入）及批内 M2.2/M2.3 先例一致、ast service 参照 339/0/0/0 与 roadmap M1.9 行一致；技能 `bug-diagnosis-prompt` 注册在案且 roadmap M2.4 行指定（同 M2.3 行）；Prereq M2.0 plan `2026-09-10-0425-1` 三 Phase 落盘在案；Decision 三要素（推荐 (a)/替代 (b)(c) 含否决理由/残余风险登记）齐备且 owner doc 对账步骤在位；各 Phase Exit Criteria 可观察可验证；组 `2026-09-10-0705` 执行序 N=2 与目录字母序一致；章节结构与批内已审 M2.2 ledger 先例一致；frontmatter `status: draft` → `active`）

## Verification

## Closure
