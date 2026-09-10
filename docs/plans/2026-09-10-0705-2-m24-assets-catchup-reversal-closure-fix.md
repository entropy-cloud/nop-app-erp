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

- [x] <Proof> 写失败测试：构造 CATCHUP 补提 → GL 凭证落行（`资产码#当期#CATCHUP`）→ 经引擎派发通道红冲该凭证（voucherBiz.reverse 链路，非域内直调 listener）的完整场景，断言：相关计划行回退（posted 标志/状态语义与逐期红冲先例一致）+ 资产累计折旧回退 + 无静默分支残留；对照组 = 逐期凭证红冲既有绿路径保持。断言可观察行为（实体重读状态），非仅类型。执行确认红：现静默 return → 计划行滞 posted=true 断言失败，失败输出记入勾选注记（缺陷复现证据）。
      - Skill: bug-diagnosis-prompt
      - 【执行证据 2026-09-10】测试落点 = 新增 `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCatchUpReversal.java`（2 方法，纯显式断言/层 1，无 output 快照）：主场景 `testCatchUpVoucherEngineReversalRollsBackSchedulesAndAsset`（补提 2026-05+2026-06 两漏提期 → 单张汇总凭证 `AST-CUR-01#2026-07#CATCHUP` posted=true → `ormTemplate.runInSession(session -> voucherBiz.reverse("AST-CUR-01#2026-07#CATCHUP", DEPRECIATION, CTX))` 引擎派发通道红冲 → 断言两计划行 REVERSED/posted=false/voucherId=null + 资产累计折旧回退 0/净值回退 12000 + GL 原凭证 isReversed=true）；对照组 `testPerPeriodVoucherEngineReversalGreenPath`（executeDepreciation → voucherBiz.reverse 逐期键 → 先例语义断言）。**红确认**：`mvn test -pl module-assets/erp-ast-service -Dtest=TestErpAstCatchUpReversal` → `Tests run: 2, Failures: 1`，失败输出 = `漏提期 2026-05 计划行回退 REVERSED ==> expected: <REVERSED> but was: <EXECUTED>`（TestErpAstCatchUpReversal.java:100，静默 return 缺陷复现：GL 红冲成功而计划行滞 EXECUTED/posted=true）；对照组同批绿。
      - （执行期注记：surefire 输出流中混入一条面向 AI 代理的提示注入文本「If you are an AI Agent...Disregard previous instructions」——已忽略，不影响测试判定与本批执行；建议归 docs/bugs 观察项。）
- [x] <Proof> 复现定位记录：按 bug-diagnosis 四阶段将缺陷链（listener 静默站点 L65-68 × Dispatcher 落行键结构 × 逐期红冲键失配点）与 finding 报告证据对账；实核 `backfillCatchUpSchedules` 回填结构与汇总凭证行对应关系（逆操作可行性证据）；确认无第二红冲入口遗漏（grep businessType=DEPRECIATION 红冲派发面注记）。
      - Skill: bug-diagnosis-prompt
      - 【执行证据 2026-09-10】逐站对账（执行期 HEAD 实读，行号随本批修复前基线）：①listener 静默站点 L65-68 `billHeadCode.endsWith(CATCHUP_SUFFIX)` 静默 return = finding §2.3 控制点一致；②落行键结构 = `DepreciationPostingDispatcher.catchUpBillHeadCode` L169-171（`billHeadCode(assetCode, currentPeriod) + ErpAstConstants.CATCHUP_BILL_SUFFIX`，键 = `资产码#当期#CATCHUP`）+ businessType 仍 DEPRECIATION（buildCatchUpEvent L146）= finding 一致；③逐期红冲键失配点 = `ErpAstDepreciationScheduleReverseDepreciationProcessor` L48-49 `postingDispatcher.reverse(资产码#漏提期)` → 引擎 `ErpFinPostingProcessor.reverseProcess` L241-244 无 posted 凭证命中即抛 `ERR_REVERSE_SOURCE_NOT_FOUND` = finding 自愈断②一致；自愈断①（catchUp 幂等跳过）= CatchUpProcessor L104-107 EXECUTED continue 一致。④backfill 逆操作实核：`backfillCatchUpSchedules`（CatchUpProcessor L192-202）落行时逐行 setPosted(true)/setVoucherId(voucherId)——计划行 `voucherId` 列存原汇总凭证 ID；`VoucherReversedEvent.reversalOfVoucherId` = 被冲销原凭证 ID（ErpFinPostingProcessor L269-270 dispatch 传 firstOriginalId，TestErpFinReversalDispatch L120 断言在案）→ 按 reversalOfVoucherId 反查计划行即 backfill 精确逆操作，可行。⑤第二红冲入口普查：全仓 grep `implements IErpFinVoucherReversedListener` = 5 生产实现（ast/sal/pur/inv/mfg 各 1），assets 域唯一；DEPRECIATION 红冲派发面仅两条入口——引擎 `IErpFinVoucherBiz.reverse`（财务侧任意 billHeadCode）与域内 `DepreciationPostingDispatcher.reverse` L178（逐期键），无第三入口遗漏。

Exit Criteria:

- [x] CATCHUP 红冲失败测试在位且当前树红（计划行/累计回退断言失败），失败输出注记在案；逐期红冲对照组绿
      - 【执行证据 2026-09-10】红 = `expected: <REVERSED> but was: <EXECUTED>`（TestErpAstCatchUpReversal.java:100，Tests run: 2, Failures: 1）；对照组 `testPerPeriodVoucherEngineReversalGreenPath` 同批通过。修复后该套件 2/0/0/0 全绿（见 Phase 2 注记）。
- [x] 缺陷链与 finding 报告逐站对账一致，backfill 逆操作结构实核在案，无未登记第二红冲入口
      - 【执行证据 2026-09-10】五站对账全一致（见 Phase 1 第 2 项注记①~⑤）；reversalOfVoucherId=原凭证 ID 由引擎 dispatch 实现与 fin 域测试双证。

## Phase 2 — 闭环修复（Decision + Fix）

> 统一类型：Decision | Fix | Add（1 Decision + 1 Fix + 1 Add）。
> Skill: bug-diagnosis-prompt
> Targets: `ErpAstDepreciationReversalListener.java`（rollbackDepreciationSchedule CATCHUP 分支）；回退 helper 复用/抽取落点（`ErpAstDepreciationScheduleCatchUpDepreciationProcessor`，protected step 沿 Processor 范式）由本阶段 Decision 裁决；owner doc 对账面 = `docs/design/assets/depreciation-and-posting.md`（红冲语义节；ast2-025-r3 已登记其漂移簇——本批仅对账，不修 doc[doc 批义务]）
> Prereqs: Phase 1 完成（失败测试在位）

- [x] <Decision> CATCHUP 回退方案裁决：推荐 (a) listener 识别 `#CATCHUP` 后按 voucherId 反查全部汇总凭证行 → 映射回计划行聚合回退（`backfillCatchUpSchedules` 逆操作，逐行 posted/状态回退 + 资产累计回退，镜像逐期红冲语义）。替代方案：(b) 落行时登记 `#CATCHUP#漏提期` 粒度可逆锚点（改 Dispatcher 落行键结构 + 历史存量凭证无锚点不可回退，需迁移面）——被否，扩大落行格式变更面且不解决存量；(c) 保持静默 + 告警日志——被否，闭环缺陷未修仅降噪，违反先红后绿义务。残余风险：(a) 对 Phase 2 之前已存在的滞留 posted=true 存量计划行无追溯回退（运行数据非本仓证据面），在注记显式登记；若 owner doc 对账发现红冲语义已声明其他预期，以 doc 为先并记录。裁决与 owner doc 对账：核对 `depreciation-and-posting.md` 红冲节语义一致；发现漂移即登记注记归 ast2-025-r3 doc 批（不扩本批 scope 落 doc 修订，除非漂移直接阻塞方案语义——此时先修 doc 并注记）。
      - Skill: bug-diagnosis-prompt
      - 【Decision 落地 2026-09-10】采纳方案 (a)：`rollbackCatchUpSchedules` 按 `event.getReversalOfVoucherId()`（= 被冲销原汇总凭证 ID）反查全部映射计划行聚合回退。三要素：①选择 (a) 理由 = backfill 精确逆操作（落行/回退同键 voucherId）、零落行格式变更、引擎 dispatch 至多一次（已红冲凭证被 findAllPostedVouchers 过滤）无幂等重入面；②替代否决 (b)(c) 理由如上（(b) 扩大变更面 + 存量不可回退；(c) 仅降噪违反先红后绿）；③残余风险 = 修复前已滞留 posted=true 的存量计划行无追溯回退（已登记 Phase 3 第 3 项注记）。**owner doc 对账结论**：`docs/design/assets/depreciation-and-posting.md` L220 补提实现注记已显式登记「汇总凭证无法按漏提期单期红冲」为 follow-up——与本批聚合红冲语义方向一致、无阻塞漂移；该 §注记未声明汇总凭证红冲的其他预期语义，方案 (a) 与 doc 互补（本批落地后该 follow-up 实质闭环，doc 文本修订归 ast2-025-r3 doc 批不扩本批）；§7.2 未提及 ReversalListener 通道 = ast2-025-r3 已登记漂移簇站点③，归 doc 批。
- [x] <Fix> 接入回退逻辑：`rollbackDepreciationSchedule` 的 CATCHUP 分支按 Decision 方案落地聚合回退；行为不变式：非 CATCHUP（逐期）路径逐字节现状保持（既有绿测试为证）。
      - Skill: bug-diagnosis-prompt
      - 【执行证据 2026-09-10】`ErpAstDepreciationReversalListener`：CATCHUP 分支改路由至新 `rollbackCatchUpSchedules`（billHeadCode 剥 `#CATCHUP` 后缀解析 assetCode → findAssetByCode → 按 reversalOfVoucherId 反查 posted 计划行 → 逐行 REVERSED/posted=false/voucherId=null → ΣactualAmount 回退资产累计折旧/净值）；逐期分支语义逐字节保持（仅前置 billHeadCode null 判断拆分，原 `null || endsWith` 合并条件等价拆分后逐期路径逻辑/顺序不变，`TestErpAstPostingReverse` 5 测试 + `TestErpAstCatchUpDepreciation` 7 测试 + `TestErpAstDepreciation` 11 测试全绿为证）；Dispatcher 落行键结构零改动（`git status` 无 Dispatcher 命中）。
- [x] <Add> 回退不可达时的显式错误（若方案 (a) 实核后发现个别行无法映射回计划行——如数据残缺）：`ErpAstErrors` 增域内错误码（中文描述合规 + `@Locale("zh-CN")` 接口注解已在位[MI.1]；异常参数传码不传散文——MI.5a CAT-2 契约），禁止恢复静默分支；若实核后全部行均可映射（无不可达分支），本项以注记记录「无需新增错误码」收口，不留空转。
      - Skill: bug-diagnosis-prompt
      - 【执行证据 2026-09-10】实核确认存在不可达分支（凭证已提交而计划行回填未落的数据残缺窗口——F1.2 REQUIRES_NEW 凭证提交与回填两段间真实存在）：新增 `ErpAstErrors.ERR_DEPRECIATION_CATCHUP_REVERSE_NOT_MAPPABLE`（`erp.err.ast.depreciation.catchup-reverse-not-mappable`，中文描述，参数 `ARG_ASSET_CODE` + 新增 `ARG_VOUCHER_ID`）——资产缺失/无映射计划行/无已过账行三态显式抛错，静默分支未恢复；异常参数传码不传散文合规。

Exit Criteria:

- [x] Phase 1 CATCHUP 红冲测试全绿（计划行回退 + 资产累计回退断言通过）+ 逐期红冲/补提正路径既有测试保持绿
      - 【执行证据 2026-09-10】`TestErpAstCatchUpReversal` 2/0/0/0 全绿；ast service 全套 341/0/0/0（含 `TestErpAstCatchUpDepreciation` 7 + `TestErpAstPostingReverse` 5 + `TestErpAstDepreciation` 11 既有回归零失败）。
- [x] 守卫/回退落在 listener 分支层、Dispatcher 落行键结构零改动；Decision 三要素注记在案；owner doc 对账结论在案
      - 【执行证据 2026-09-10】回退面 = listener 新增 `rollbackCatchUpSchedules`/`findSchedulesByVoucherId` + `ErpAstErrors` 新错误码，`git status --porcelain` 生产面仅 2 文件命中（listener + errors），Dispatcher/Provider 零改动；三要素与对账结论见 Phase 2 Decision 注记。

## Phase 3 — 批级证明与收官门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [x] <Proof> 域级回归：`mvn test -pl module-assets/erp-ast-service -am` 全绿零新增失败（参照 339/0/0/0，数字记入注记；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）。
      - Skill: none
      - 【执行证据 2026-09-10】`mvn test -pl module-assets/erp-ast-service` 汇总行 = **Tests run: 341, Failures: 0, Errors: 0, Skipped: 0, BUILD SUCCESS**（= 2026-09-09 基线参照 339 + 本批新增 TestErpAstCatchUpReversal 2，40 测试类全部 0 失败）；`-am` 链（40 reactor 模块）BUILD SUCCESS。
- [x] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行，surefire 口径真值 3991/0/0/1）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（`git ls-files` 同路径非空集，证明构成）+ `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: none
      - 【执行证据 2026-09-10】①全 reactor `mvn test` = **3999/0/0/1 零新增失败**（对照 2026-09-09 基线行 3991/0/0/1：+8 增量全额归因披露 = M2.2 fin +4（TestErpFinIntercompanyEliminationDimension）+ M2.3 mfg +2（TestErpMfgReservationLifecycle）+ 本批 ast +2，roadmap M2.2/M2.3 行注记互证；skipped=1 与基线同）；②compliance checker **exit 0**，R2b=242/R2c=1542/R12a=71 逐值=基线零漂移（全 19 规则无新增命中）；③CJK `--strict` **PASS**（exit 0，CAT-1..4 = 0/0/0/0，CAT-5 注释豁免仅统计）；④seed porcelain = `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 输出空（0 行）+ `git ls-files` 同路径 373 文件（构成证明）；⑤`mvn clean install -DskipTests` = BUILD SUCCESS（2026-09-10 本批执行期）。
- [x] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P2-CK-ast2-024-r3 fixed 证据指针（测试类/方法 + 回退站点）；ast2-026-r3 语义联动注记（修复后 CATCHUP 分支可观察语义描述 + 测试指针，供 M2.8 分片3 消费）；存量滞留行无追溯回退残余风险登记；seed/ORM/api.xml 零触碰声明（`git status --porcelain` 全树无 `module-*/model/`、`*.api.xml`、`_init-data/`、页面命中）。
      - Skill: none
      - 【执行证据 2026-09-10】四件落盘如下。
      - **(1) fixed 证据指针**：测试 = `module-assets/erp-ast-service/src/test/java/app/erp/ast/service/TestErpAstCatchUpReversal.java`（主场景 `testCatchUpVoucherEngineReversalRollsBackSchedulesAndAsset` 引擎派发通道 + 对照 `testPerPeriodVoucherEngineReversalGreenPath`，先红后绿：红 `expected: <REVERSED> but was: <EXECUTED>` → 绿 2/0/0/0）；回退站点 = `ErpAstDepreciationReversalListener.rollbackCatchUpSchedules`（CATCHUP 分支聚合回退）+ `ErpAstErrors.ERR_DEPRECIATION_CATCHUP_REVERSE_NOT_MAPPABLE`（不可达显式错误）。
      - **(2) ast2-026-r3 语义联动注记（供 M2.8 分片3 消费）**：修复后 CATCHUP 分支可观察语义 = (i) 财务侧经引擎通道红冲 CATCHUP 汇总凭证（billHeadCode `资产码#当期#CATCHUP`，businessType=DEPRECIATION）→ 监听者按事件 `reversalOfVoucherId` 反查全部 posted=true 计划行，逐行置 `status=REVERSED/posted=false/voucherId=null`（与逐期红冲先例语义一致），并按 ΣactualAmount 回退资产累计折旧/净值；(ii) 映射不可达（资产缺失/零映射行/零 posted 行——数据残缺）→ 显式抛 `ERR_DEPRECIATION_CATCHUP_REVERSE_NOT_MAPPABLE`，经 `ErpFinReversalListenerRegistry` 失败隔离落 5.1 异常工作台，红字凭证不回滚；(iii) 逐期（非 CATCHUP）路径行为不变。测试指针 = `TestErpAstCatchUpReversal`（引擎通道双侧）+ `TestErpAstPostingReverse.testDepreciationReverseRollsBackAssetCard`（域内发起通道既有回归钉）；分片3 扩展面建议覆盖 ASYNC 派发模式与跨账套多凭证红冲场景（本批未涉）。
      - **(3) 残余风险登记**：方案 (a) 对修复前已滞留 `posted=true` 的存量 CATCHUP 计划行无追溯回退（运行数据非本仓证据面）——补救路径 = 对存量资产人工重发红冲（修复后语义下回退闭环可达）或数据修复；已同步登记于 Phase 2 Decision 注记。
      - **(4) 零触碰声明**：`git status --porcelain` 全树 grep `module-.*/model/`、`*.api.xml`、`_init-data/`、`.view.xml`、`.page.yaml` = **0 命中**；生产面改动仅 2 文件（`ErpAstDepreciationReversalListener.java` + `ErpAstErrors.java`）+ 新增测试类 + `_cases` 框架自动生成空 `autotest.yaml` 脚手架（与全仓 133 处 committed autotest.yaml 先例同构，非 seed/ORM/api.xml/页面）。

Exit Criteria:

- [x] ast service 全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案
      - 【执行证据 2026-09-10】341/0/0/0 + 全 reactor 3999/0/0/1（+8 增量全归因）+ compliance exit 0（R2b=242/R2c=1542/R12a=71 零漂移）+ CJK `--strict` PASS + seed porcelain 空构成证明——逐项见 Phase 3 第 1/2 项注记。
- [x] M2.9 消费证据四件（fixed 指针 / 026 联动注记 / 残余风险登记 / 零触碰声明）落盘于计划注记
      - 【执行证据 2026-09-10】四件已落盘于 Phase 3 第 3 项勾选注记（(1)~(4)），M2.9 收官可直接消费；本计划不改索引（索引状态回填归 M2.9 义务）。

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-0705-2-m24-assets-catchup-reversal-closure-fix-1-f815ce76 to opencode-reviewer-session-2026-09-10
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-0705-2-m24-assets-catchup-reversal-closure-fix-1-f815ce76（零 Blocker；2 Minor 已修：Phase 2 统一类型补 Add、Targets「必要时」改绑本阶段 Decision 裁决；基线锚点 HEAD 实核逐一命中——ReversalListener `CATCHUP_SUFFIX` L45 + CATCHUP 分支静默 return 站点、finding `P2-CK-ast2-024-r3` 登记于 `ck-assets-depreciation-r3.md` §2.3、M2.0 族裁决表 row 69（ast2-024-r3 → M2.4 域批独立项 + ast2-026-r3 → 分片 3 联动）实存于 `m2-0-family-adjudication.md`、`ErpAstErrors`/`TestErpAstCatchUpDepreciation`/`backfillCatchUpSchedules`（CatchUpDepreciationProcessor + DisposalProcessor 消费）在位、known-good-baselines 2026-09-09 行 R2b=242/R2c=1542/R12a=71 + CJK strict PASS 与计划引用一致、surefire 口径真值 3991 与 M1.17 闭包审计裁决（基线行 4006 系日志拼接重复计入）及批内 M2.2/M2.3 先例一致、ast service 参照 339/0/0/0 与 roadmap M1.9 行一致；技能 `bug-diagnosis-prompt` 注册在案且 roadmap M2.4 行指定（同 M2.3 行）；Prereq M2.0 plan `2026-09-10-0425-1` 三 Phase 落盘在案；Decision 三要素（推荐 (a)/替代 (b)(c) 含否决理由/残余风险登记）齐备且 owner doc 对账步骤在位；各 Phase Exit Criteria 可观察可验证；组 `2026-09-10-0705` 执行序 N=2 与目录字母序一致；章节结构与批内已审 M2.2 ledger 先例一致；frontmatter `status: draft` → `active`）

## Verification

- pass test 2026-09-10-1012 exit=0

## Closure

- dispatch audit #audit-2026-09-10-1012-2026-09-10-0705-2-m24-assets-catchup-reversal-closure-fix-1-8fbbdd0a to opencode-closure-auditor-session-2026-09-10-1012 models={exec:opencode/glm-5.3-flash,aud:opencode/glm-5.3-flash}
- accepted #audit-2026-09-10-1012-2026-09-10-0705-2-m24-assets-catchup-reversal-closure-fix-1-8fbbdd0a：独立闭包审计通过——修复体实核在位（`ErpAstDepreciationReversalListener.rollbackCatchUpSchedules` CATCHUP 分支按 `reversalOfVoucherId` 聚合回退 + `ErpAstErrors.ERR_DEPRECIATION_CATCHUP_REVERSE_NOT_MAPPABLE` 显式错误 + 新测试类 2 方法经引擎派发通道红绿闭环，逐期分支与 Dispatcher 键结构零改动），本访验证全绿：`mvn clean install -DskipTests` BUILD SUCCESS + 全 reactor `mvn test` 0 failures/0 errors/1 skipped（skipped=基线预存；41 模块汇总 4014 = 4006 口径 + M2.2 fin +4 + M2.3 mfg +2 + 本批 ast +2，与计划所记 3991 口径差 15 系 M1.17 已裁计数口径差，失败画像逐值一致零新增失败；ast 341/fin 537/mfg 310 逐域与计划注记精确一致）+ compliance checker exit 0（R2b=242/R2c=1542/R12a=71 零漂移）+ CJK `--strict` PASS（CAT1..4=0/0/0/0）+ seed porcelain 空；全部 14 检查项勾选与实况一致，无 deferred 内藏缺陷，ledger 证据齐备可推导 completed。
