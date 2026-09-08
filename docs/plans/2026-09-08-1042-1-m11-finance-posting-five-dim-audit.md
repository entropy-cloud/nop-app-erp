---
status: active
mission: ai-check-r3
work-item: M1.1
group: "2026-09-08-1042"
verify: [test]
---

# 2026-09-08-1042-1 M1.1 finance fin-1 五维符合性审计（过账与凭证切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）。M1.1 为 M1 里程碑第一个工作项（roadmap 文档序首个 deps 满足的无计划 todo 项）。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U05 × 五维 × fin-1**（§4 映射表第 1 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- 切片范围（U05 fin-1）：posting 引擎/凭证/红冲/sweep/过账日志；owner doc `docs/design/finance/posting.md`（619 行，实仓核验在盘）+ `docs/design/finance/posting-log.md` + `docs/design/finance/state-machine.md`；物理面 `module-finance/erp-fin-{dao,service,web}` 的 `src/main`。
- 共享代码唯一归属（冻结清单 §3.2）：**posting processor 族本体（`ErpFinPostingService`/凭证引擎/dispatcher 族/`FinPostedListener`/sweep/`PostingRun`/posting.md §同步测试缝）唯一归属本格**——引擎/processor 内部缺陷一律归 fin-1；跨域消费点（sal 发票红冲前置 / inv 凭证 REQUIRES_NEW / mfg 完工入库移动 / ast 折旧过账）的消费侧行为归各域切片，本切片发现涉及消费侧时标注「归属 <域切片>」归并，不重复立项。common 抽象族（`AbstractProcessor.illegal*` 等）行为缺陷归 U20，本切片只审调用点。
- 跨轮查重源（§2）：r1 `docs/audits/check/ai-check-index.md`（同域报告 `ck-finance-posting.md` C3.1：0 P0 / 5 P1 / 10 P2 / 4 P3，done；P1-CK-fin-001~005 fixed，P2-CK-fin-006~015 open，P3-CK-fin-016~019 open）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（已裁决偏离不重复报告）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：fin 探针族（CAT-1 54 / CAT-2 20）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块与 actual 的批前在案差距归 successor `ai-check-r3-compliance-baseline-raise`，非本切片范围。
- 仓库现状（2026-09-08 实核）：HEAD `40512c567`；姊妹 plan `2026-09-07-2200-1`（StateMachine 直抛领域码）Phase 4/5 已提交，脏面仅 `docs/lessons/README.md`（伴生文档编辑）。审计证据以实跑时 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：U05 × 五维 × fin-1 五格 verdict 未落盘；本轮尚无任何 `ck-*.md` 报告（`ai-check-r3-index.md` 产物清单截至 M0.6/MI.9）。

## Goals

- 按冻结清单对 U05 × 五维 × fin-1 五格全跑（禁止抽样、禁止跳维），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-posting-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 fin-2/fin-3/fin-4 格（M1.2~M1.4）与其他单元格；posting 消费侧各域行为不在本切片立项（按 §3.2 归并标注）。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 在位
      - Skill: none
      - 证据（2026-09-08）：目录在位（幂等复用，2026-09-06 M0.3 建立后未新建）；`ai-check-r3-index.md`（头部登记路径 = 本目录，一致）与 `m0-5-audit-checklists.md`（冻结版）均在位实读。
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露（MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点
      - Skill: none
      - 证据（审计时点 T0 = 2026-09-08）：HEAD `8825a10e12ffcf364f364076efb85d857075d2fb`（plan Current Baseline 记录的 `40512c567` 之后姊妹 plan `2026-09-07-2200-1` 收尾已提交，lessons/README.md 脏面已清）；`git status --porcelain` = 仅 3 个未跟踪 plan 文件 `docs/plans/2026-09-08-1042-{1,2,3}-*.md`（mission-driver 同批生成：本计划 M1.1 + 姊妹 M1.5/M1.8），**零生产路径脏面**；姊妹在制会话披露 = 同批 M1.5/M1.8 计划尚未执行、无并发代码会话；本审计实跑为干净树（未跟踪 plan 文件不影响任何核查程式）。
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据（T0）：compliance checker 实跑 R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42（其余 0）——与 M0.3 快照行**逐项一致，零漂移**；R12a=71>69、R12c=42>38 对机器基线块的批前在案差距沿 plan Current Baseline 归 successor `ai-check-r3-compliance-baseline-raise`（非本切片新发）。CJK checker report mode：CAT-1=0/CAT-2=0/CAT-3=0/CAT-4=0（CAT-5 注释 21158 行豁免仅统计；java 3430 + yaml 886 文件扫描），与 MI 终态行 0/0/0/0 一致零回归。

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.1 行指定）
> Targets: `module-finance/erp-fin-dao|erp-fin-service/src/main/java`（fin-1 范围 = posting 引擎/凭证/红冲/sweep/过账日志族文件）；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0）：① compliance checker 全规则对照 M0.3 快照行逐项一致零漂移（Phase 1 注记）；② 反模式 grep 族（fin-1 范围 posting/voucher/reversal/sweep/dispatch/gl-mapping/schema/registry/acctdoc/template 族）：`extends RuntimeException`=0、`@Inject private`=0、`System.currentTimeMillis`=0、`@Transactional` 实注解仅 ErpFinVoucherBizModel post/reverse 两处 REQUIRES_NEW（nop-check 豁免注释在位，=R6 基线 2，processor-extension-pattern 硬规则 1 显式独立事务边界；其余命中均为 javadoc 文字提及非注解）；`IDaoProvider|IOrmTemplate` 命中处 = posting 引擎族 Processor/编排 bean 标准用法（owner doc processor-extension-pattern 反模式表豁免域内编排组件；越权直查仅 P2-CK-fin-007 已知 open 站点，归并见 Decision 注记）；③ codegen 产物安全：`__XGEN_FORCE_OVERRIDE__` 仅命中 erp-fin-meta 5 个 dict yaml（voucher-type/voucher-status/posting-type/posting-exception-status/posting-exception-resolution，= 只读校验点，git status 干净零手改）；`_gen` 目录零脏面；④ 聚合完整性（E1 勘误路径）：`/erp/fin/auth/erp-fin.action-auth.xml` 在聚合器 x:extends 注册表 L15 在案。
- [x] <Proof> 15 维度逐维走查 fin-1 范围（程序式确定性走查落 verdict；重点：①Model→Delta→Java ②跨实体 I*Biz ③NopException ⑧状态机 ⑨审批流/作业 + processor-extension-pattern 对齐——fin-1 专属增量）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0，15/15 无跳维）：①Model→Delta→Java **pass**（编排走 Processor 范式 = posting.md 稳定约束裁定；GL 映射规则表/凭证模板/字典均模型驱动；Provider/Validator SPI 注册中心 O(1) 路由对齐 §注册方式）；②跨实体 I*Biz **pass（1 已知 open 归并）**（resolveSubjects/findCurrencyById 经 IBizObjectManager 按名解析 I*Biz 管道且注释理由在位；translateFactsForSchema L755 daoFor 直查 = r1 P2-CK-fin-007 原站点未修，归并不新立）；③NopException **pass**（0 extends RuntimeException；业务错误全 NopException+ErrorCode；RetryProcessor.parseBusinessType 裸 valueOf 为 P3-CK-fin-016 族新站点，归并）；④IoC/事务 **pass**（0 @Inject private；R6=2 两处 REQUIRES_NEW 豁免注释在位；@SingleSession 钉编排方法 process/reverseProcess 对齐硬规则 1 Session 分层）；⑤平台辅助 **pass**（CoreMetrics/StringHelper 全用）；⑥标准服务 **pass**（AbstractErpCrudBizModel 继承 + @BizQuery/@BizMutation）；⑦机制 B **pass**（跨域 md 实体读 = R2b/R12 共享内核基线已裁决条目；Maven DAG 单向）；⑧状态机 **pass**（ErpFinVoucherDocumentStateMachine Bean 唯一边 DRAFT→POSTED + isPosted 分类 helper + CANCELLED intentional reserved 不纳入任一集合；7 生成路径 §9.2 选项 c 边界注记对齐；period lock ERR_FIN_VOUCHER_PERIOD_LOCKED 在位）；⑨审批流/作业 **pass（open 归并）**（sweep=nop-batch deferred-posting-sweep.batch.xml + G2 告警经 IErpSysNotificationBiz + MAX_RETRY 升级 MANUAL；backlog 计数性能缺陷 = P2-CK-fin-012 open 归并）；⑩定制顺序 **pass**（引擎步骤 protected+IServiceContext 末参、Delta 同名 bean 覆盖、SPI 优先）；⑪多租户/l10n **pass**（无 tenant 预置、orgId 维度在位）；⑫测试 → 指针 DIM-T（Phase 5 全跑）；⑬codegen 安全 **pass**；⑭聚合完整性 **pass**；⑮断言抽样 **pass 零漂移**（见下条）。级别分布：本轮无新增 blocker/major；新增 minor 站点 2 处（均归并既有 ID，见 Decision）。
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`docs/design/finance/posting.md` + `posting-log.md` + `state-machine.md` 中 ≥2 doc × 2 关键断言（状态名/字段名/ErrorCode/迁移路径）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
      - 证据（T0，3 doc × 6 断言，6/6 一致零漂移，未触发扩样）：state-machine.md §对象一§2 断言「DRAFT→POSTED 前置=草稿+借贷平衡+期间未结账+科目有效+汇率存在」与 ErpFinVoucherBizModel.postVoucher（assertCanPost/assertBalancedFromLines L102/assertPeriodNotLocked）+ 引擎（ERR_SUBJECT_NOT_FOUND/guardExchangeRate ERR_EXCHANGE_RATE_REQUIRED）逐项一致；state-machine.md §对象二§6 断言「CLOSED/CLOSED_FINAL 抛 ERR_FIN_VOUCHER_PERIOD_LOCKED」与 BizModel L186-204 一致；posting.md §方向二「VoucherReversedEvent 六字段契约 + 监听者 try/catch 隔离 + 失败落 REVERSAL/notify-reversal-listener」与引擎 dispatchReversalEvent/recordListenerFailures/ErpFinReversalListenerRegistry 一致；posting.md §悬挂补写 F2.1「派发点仅 doRetry+retry 两处 + ERR_POSTED_LISTENER_FAILED + notify-posted-listener + 已 true 跳过/miss no-op」与 sweep L111-141/RetryProcessor L56-58/Registry/FinPostedListener 一致；posting-log.md §规则命中日志「traceId 缺失生成 StringHelper.generateUUID + 成功结构化日志不持久化」与引擎 ensureTraceId/成功日志一致；posting-log.md §失败路径「ErpFinPostingException 持久化 traceId+ErrorCode+失败阶段+重试次数、失败不静默丢弃」与 recordPostFailure/recordReverseFailure（record-then-rethrow）一致。
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-finance-posting.md` §Finding 追踪 + r2 目录 + §Mission 基线快照）：同型 fixed=复用（复核 HEAD 仍有效）/同型 open=归并原 ID/确属新发=新立 `P{n}-CK-fin-{NNN}-r3`；裁决证据落勾选注记（报告落 Phase 7）
      - Skill: code-quality-audit-prompt
      - 裁决（T0，逐条）：**复用（fixed 复核 HEAD 有效）5 条**——P1-CK-fin-001（sweep doRetry dispatchPostedEvent L111-117 + RetryProcessor L56-58 两通道派发+Registry+FinPostedListener 在位）、P1-CK-fin-002（postVoucher assertBalancedFromLines L102 在位）、P1-CK-fin-003（幂等命中返回 existing.getId() L140-146 在位）、P1-CK-fin-004（findActiveSchemasByOrg eq(status,ACTIVE) L116 + statusScore 化简在位）、P1-CK-fin-005（process try 首段 ERR_NO_ACTIVE_SCHEMA fail-closed L149-158 在位）。**归并（open 追加证据）14 条**——P2-CK-fin-006（sweep 无源单校验通道，rebuildEvent/doRetry 未变）、P2-CK-fin-007（translateFactsForSchema mdSubjectBiz 死变量 L727 + daoFor 直查 L755 原样）、P2-CK-fin-008（catch 自赋值 L219 原样）、P2-CK-fin-009（buildReversalDraft L806-820 不复制 amountSource/amountFunctional + prepareReversalContext 首行汇率原样）、P2-CK-fin-010（resolveAcctSchemaIdFromContext 恒 null L699-701 原样）、P2-CK-fin-011（existsItem/findItems L75/L199-209 无账套维度原样）、P2-CK-fin-012（countUnresolved L209-211 + 3 count× findAllByQuery().size() L297/304/312 原样）、P2-CK-fin-013（RetryProcessor rebuildEvent 汇率回退 ONE L105 + BizModel 死代码 rebuildEvent L338 原样）、P2-CK-fin-014（手动 retry 外层事务翻 RETRYING 回滚 + recordPostFailure 增生 PENDING 原样）、P2-CK-fin-015（reverseVoucher 仅置 isReversed L112-124 原样）、P3-CK-fin-016（findBillLinks L969 businessType.name() NPE 原样 + **新站点追加**：RetryProcessor.parseBusinessType L114-119 裸 valueOf 脏值抛 IAE 非业务错误，同族）、P3-CK-fin-017（resolveOpenPeriod 无 orderBy get(0) L524-545 原样）、P3-CK-fin-018（CommitmentVoucherGenerator L228-238 dcDirection 保留+借贷互换+amountSource 正数原样）、P3-CK-fin-019（PostingRun.captureTemplate 恒 null L476-478 原样）。**归并（跨切片同型 open 追加 fin-1 站点）1 条**——P3-CK-fin2-012（currentUserId 宽 catch 返 null 无日志族：RetryProcessor L121-128 新站点，finance 域同型 open 家族 ID）。**新立 0 条**（本轮全部候选均有历史同型，无确属新发项）。查重源：r1 ck-finance-posting.md 全 19 条逐一比对 + r2 目录索引 + §Mission 基线快照（R2b/R12 等命中均为已裁决偏离不重复报告）。

Exit Criteria:

- [x] U05×B×fin-1 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维
      - **verdict = finding**（0 新立；5 复用 fixed 复核有效 + 15 归并追加证据；机械程式 0 反模式、基线零漂移；owner-doc 断言 6/6 一致）——15 维度全走查无跳维（⑫指针 DIM-T）。
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成（复用/归并/新立逐条在案）
      - 机械数字：checker 全表 19 规则 = M0.3 行逐项一致；反模式族全零；R6=2 对账一致；codegen/聚合/CoreMetrics 零违规。三态裁决 5 复用 + 15 归并 + 0 新立逐条在案（上注记）。

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: `module-finance/erp-fin-web/src/main/resources/_vfs`（fin-1 面：凭证录入/凭证模板页）；E2E 涉 fin spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案，非本切片 finding）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）
      - Skill: none
      - 证据（T0）：`npm run validate:flux` 实跑 step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`（与 plan 数字逐项一致）；整体 exit=1，ERR 共 **325 条全部**为 `variant=primary` on dropdown-button 既有 stub 外部漂移族（非 variant 类 ERR = 0 条），fin 域 29 条同族（含 ErpFinVoucher 面，均为 codegen stub 生成源，successor 登记在案，非本切片 finding）——与 plan 预告 325 条逐字对账一致零漂移；flux-only grep：`component="AMIS"` 保留层命中 0、`grep -L ext:web-renderer="flux"` 于 18 个 ORM 模型 = 空缺省零命中。
- [x] <Proof> fin-1 页面走查：凭证录入/凭证模板页对照 view-and-page-strategy（REST `/r/` 数据访问 / codegen vs 手写边界查 M0.4 矩阵 / 定制走 `x:extends` / i18n-en 承载约定）逐页落 verdict；涉 fin E2E spec 时核对 PageObject 模式 + `E2E_ENGINE` flux 缺省 + 禁 GraphQL 断言
      - Skill: none
      - 证据（T0，逐页）：**ErpFinVoucher main**（凭证录入）——保留层 `main.view.xml` 经 `x:extends="_gen/_ErpFinVoucher.view.xml"` 定制（codegen 边界合规，无 `_` 前缀手改）；数据访问全 REST（`@query:ErpFinVoucher__previewReverseVoucher` L182 / `@mutation:ErpFinVoucher__reverseVoucher` L214 / `@mutation:ErpFinVoucher__postVoucher` L235，零 GraphQL 引用）；label/title/button i18n-en 承载在位（`i18n-en:label="Business Org"` 等）；M0.4 矩阵本页无 CJK 残留行（手写行仅 ErpFinVoucherBillR bills-by-voucher/voucher-by-bill 两页，MI.8 已收口）。**ErpFinVoucherTemplate(+Line)**——纯 codegen 页（`_gen/_ErpFinVoucherTemplate.view.xml` 源 + i18n-en 全覆盖），无手写定制层，边界合规。**ErpFinVoucherBillR/ErpFinGlMappingRule/ErpFinPostingException 页**——codegen 标准形态，同上合规。E2E：fin 涉凭证 spec（finance voucher 手工 post / p2p-reverse / o2c-reverse / fin-bad-debt 族）——`E2E_ENGINE` 缺省 flux 实证（tests/e2e/pages/engine.ts L8-11 unset→'flux'）；spec 经 `_helper`/`orchestration/_helper` 业务概念层调用（PageObject 模式，选择器在 adapter 层）；其 GraphQL `/graphql` 调用为 runbook L122 明文豁免的「非页面路径」API 断言通道（业务动作套件 sanctioned 范式，runbook §业务动作套件 L229/337 在案），非「flux 页面 GraphQL 断言」违规。
      - 查重：全局面 325 ERR 族与 fin 29 条 = 既有外部漂移（plan Current Baseline + MI.8 收官行披露在案）→ 不立项；无新候选 finding。

Exit Criteria:

- [x] U05×F×fin-1 格 verdict 落盘；全局面门禁数字（导出 error 数 / flux-only 命中数）在案对账一致
      - **verdict = pass**（导出 0 error/999 页；余项 325 条全部既有外部漂移族零漂移对账；flux-only 双 grep 零命中；fin-1 页面走查全合规）。
- [x] fin-1 范围页面逐页走查完成，无跳页
      - 凭证录入（ErpFinVoucher main）+ 凭证模板（ErpFinVoucherTemplate/Line）+ 回链页（ErpFinVoucherBillR ×4）+ GL 映射规则页 + 异常工作台页逐页落 verdict（上注记），无跳页。

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ fin 相关 seed（voucher/ar_ap_item/gl_balance/期间 OPEN）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - 证据（T0）：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` → **Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（exit 0）。
- [x] <Proof> fin-1 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空（只读审计零 seed 变更）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表 + P2P/O2C 已过账财务产物 posted 一致性抽查（`posted=true` 当且仅当凭证回链，按 seed-data.md 業财范式段）
      - Skill: none
      - 证据（T0）：① `git status --porcelain _init-data/` = 空（零 seed 变更，只读不变式成立）；② 资产清点 = **372 CSV + 1 SQL**（与冻结清单 §1.3 ②「M1.5 批次后 = 372 CSV + 1 SQL」对账一致）；③ deploy `_seed_*.sql` find 命中 = 仅 module-notify（三方言）+ module-cs（三方言）两族，逐命中查 seed-data.md §97 登记处表 L107-108：**均已聚合**（notify→`erp_sys_notification_template.csv` 27 行 / cs→`nop_sys_code_rule.csv` 1 行，2026-08-25 登记），无第三态；④ posted 一致性抽查：P2P/O2C 四表 `posted=true` 单据恰 4 张（PINV-2026-001/PAY-2026-001/SINV-2026-001/REC-2026-001）↔ `erp_fin_voucher_bill_r.csv` 对应 4 条回链（AP_INVOICE/PAYMENT/AR_INVOICE/RECEIPT）一一双向命中；全 8 voucher 行 ↔ 8 回链行双射（NP-2026-001 两凭证=票据开出两业务事件，合法）；四表零「已审核+posted=false」行（无 posted 悬挂种子）；seed voucher 首行 PZ-2026-001 POSTED/balanced 960.50=960.50 与 PINV 价税合计一致。業财范式段裁决满足。

Exit Criteria:

- [x] U05×S×fin-1 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
      - **verdict = pass**（4/4 全绿 + 零 seed 变更 + 同步义务零缺口 + posted 双射一致）。
- [x] seed 零变更 + 同步义务核对 + posted 一致性抽查结果在案
      - 三项结果均在案（上注记）。

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-finance/erp-fin-service`（`<SVC>` = 本模块，同冻结清单 §1.4 记法）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-finance/erp-fin-service` 全绿零失败（数字落注记，对照 known-good-baselines 各域计数）
      - Skill: none
      - 证据（T0）：**Tests run: 533, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（exit 0）——与 known-good-baselines MI 终态行 fin 533 计数一致零回归。
- [x] <Proof> 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 测试目录清单逐项对账（fin-1 范围 = posting/凭证/红冲/sweep 族 BizModel）；关键业务流清单核对——P0 凭证生成+过账+红字冲销、P1 多币种+期末结账+成本核算行、postNow 同步缝纪律（posting.md §同步测试缝）逐行核覆盖；缺口按业务关键度定级
      - Skill: none
      - 证据（T0）：BizModel 公开方法 × 测试对账——ErpFinVoucherBizModel 5 方法（post/reverse/postVoucher/reverseVoucher/previewReverseVoucher）全被覆盖（TestErpFinPostingService + TestErpFinReversalDispatch + TestErpFinVoucherBalanceAndSchemaFilter + TestErpFinVoucherPeriodLock + TestErpFinVoucherReversePreview）；ErpFinPostingExceptionBizModel 6 方法覆盖（TestErpFinPostingExceptionWorkbench/Notify + TestErpFinPostingObservability）；ErpFinVoucherTemplateBizModel 1 方法覆盖（TemplateRender/AuditLog/CrudSmoke）；ErpFinGlMappingRuleBizModel 2 方法覆盖（TestErpFinGlMappingResolver）；三裸 CRUD BizModel（VoucherLine/BillR/TemplateLine）零自定义方法无新增义务。`_cases/.../posting/` 20 测试族 + `src/test` 27 类（含 Registry/Metrics/FaultInjection/两 posted-listener 悬挂恢复测试）逐目录对账无孤儿。关键业务流：P0 凭证生成+过账+红字冲销 = TestErpFinPostingService（引擎全链+幂等+红冲）+ TestErpFinVoucherBalanceAndSchemaFilter + ReversalDispatch **已覆盖**；P1 多币种 = TestErpFinFxRateGuard + PropertyErpFinMultiCurrencyBalance + treasury FX 族、期末结账/成本核算行 = entity 目录 TestErpFinPeriodCloseEndToEnd/ProfitLossClosing/AnnualClose（fin service 单模块运行内）**已覆盖**；postNow 同步缝 = SYNC 直调路径经 TestErpFinPostingService + ExpenseClaim/EmployeeAdvance dispatcher 族测试 + TestFinPostingFaultInjection **已覆盖**。缺口 = 0（无 P2 起 finding）。
- [x] <Proof> 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` expect 0（提交态零残留）+ `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - 证据（T0）：RECORDING grep = **0 命中**（exit 1）；快照表 CSV 抽查（TestErpFinPostingService/testPostIdempotent input erp_fin_voucher_bill_r.csv）列头含 DEL_VERSION/VERSION 版本列、审计时间列走框架自动屏蔽——533 全绿即 CHECKING 态全量快照校验通过的等价证明，屏蔽合规。

Exit Criteria:

- [x] U05×T×fin-1 格 verdict 落盘；本地回归全绿数字在案
      - **verdict = pass**（533/0/0/0 全绿 + 覆盖缺口 0 + 快照纪律零残留）。
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案
      - 三项结果均在案（上注记）。

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + fin 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据（T0）：`--strict` → **PASS（exit 0：0 new violations vs frozen snapshot；170 baseline files，snapshot totals CAT1..4=0/0/209/1318 为冻结批前值，单向收紧成立，actual 全零）**；`--self-test` → **RESULT: PASS (self-test green)**（含「strict compare flags injected delta」反伪造断言）。fin 探针族（CAT-1 54 / CAT-2 20）维持清零零回归——无 MI 回归，DIM-I 零同类 finding（§1.5 内嵌口径履行）。
- [x] <Proof> 白名单合规抽查：`docs/audits/cjk-baseline.md` §WHITELIST fin 相关条目抽 ≥3 条核对四要素（文件路径/理由/owner doc 指针/裁决来源）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空（`_` 前缀生成 i18n yaml 禁手改）；四要素缺失 = 白名单登记缺陷 finding（非同类 CJK finding）
      - Skill: none
      - 证据（T0，抽样 5 条 ≥3 要求，超配）：① `IErpFinApDocumentBiz.java`（CAT-3，C1 `@Description` E3 豁免；四要素齐：路径/理由/`i18n-compliance.md` 准绳表 #5/plan 2026-09-07-0902-1 C1；实仓复核 @Description ×1 在位）；② `ErpFinDashboardBizModel.java`（C1，四要素齐）；③ `ErpFinApDocRuleClassifier.java`（C2 发票类型文档匹配窄类豁免；四要素齐；实仓 L91 `增值税专用发票` 在位=登记准确）；④ `ErpFinApDocumentPipelineProcessor.java`（C2 中文发票 OCR Pattern 契约；四要素齐；实仓 P_* 常量族在位）；⑤ `ErpFinApDocumentBizModel.java`（C1，四要素齐）。**5/5 四要素齐备，零登记缺陷**。`grep -L "@Locale"` *Errors.java = 空输出（22 文件全标 @Locale，MI.1 后零回归）；`git status --porcelain 'module-*/erp-*-meta/**' + _vfs/i18n/**` = 空（生成 i18n yaml 零手改）。

Exit Criteria:

- [x] U05×I×fin-1 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
      - **verdict = pass**（strict PASS exit 0 + self-test PASS + fin 探针族零回归；无同类 finding、无登记缺陷）。
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案
      - 抽样 5/5 四要素齐备 + `@Locale` 全量 + meta 零手改结果均在案（上注记）。

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-posting-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：逐条确认三态（复用/归并/新立）与级别（P0~P3）一致性与 ID 规范（`P{n}-CK-fin-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - 证据（T0）：复裁决与 Phase 2 Decision 一致无翻改——复用 5（P1-CK-fin-001..005，级别不变 P1，fixed 终态证据 HEAD 复核有效）/ 归并 15（P2-CK-fin-006..015 十条级别不变 P2 + P3-CK-fin-016..019 四条 + P3-CK-fin2-012 跨切片站点，全部 open 状态不动、原 ID 零覆写）/ 新立 0（无 `-r3` 新 ID，无需 §3.1 升级裁决）。ID 规范核对：本轮零新立故无新 ID；报告中历史 ID 引用全部带原前缀无改写。
- [x] <Add> 落盘 `ck-finance-posting-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计（P0~P3 计数）+ 剩余风险声明（横切关注点 13 完整四件套）
      - Skill: none
      - 证据（T0）：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-posting-r3.md` 落盘——§1 覆盖矩阵 **5/5 格 verdict 全落**（B=finding（归并态 0 新立）/ F=pass / S=pass / T=pass / I=pass）+ §2 三态裁决（2.1 复用 5 / 2.2 归并 15 / 2.3 新立 0 / 2.4 归属标注含消费侧四域归并指针）+ §3 统计（P0~P3 新立 0/复用 5/归并 15）+ §4 剩余风险四件套（已查/未查+边界归属/残留风险登记/successor 触发条件）。
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加本报告行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.1 行 + §Finding 追踪追加新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - 证据（T0）：① 本轮索引产物清单追加 `ck-finance-posting-r3.md` 行（来源 M1.1，含 verdict 与时点）；② 跨轮索引 §报告清单追加 r3 轮 M1.1 行（0/0/0/0 新立计数 + 复用/归并口径）；③ §Finding 追踪：新立 0 条故无新 ID 行；归并证据以表后「r3 轮复核注记」集中落盘（fin 19 条逐一处置：5 复用/14 归并/1 跨切片站点追加，指针至 r3 报告 §2.2，历史 ID 零覆写零状态翻改）。
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6；触碰面 = 执行目录报告 + 索引 + 本计划勾选注记）
      - Skill: none
      - 证据（T0 收尾时点）：`git status --porcelain | grep 'module-.*src/\|app-erp-all/src/'` = **空（零生产路径触碰）**；全量触碰面 = `ai-check-r3-index.md`（M）+ `ai-check-index.md`（M）+ `ck-finance-posting-r3.md`（新）+ 3 个未跟踪 plan 文件（mission-driver 同批，非本审计产物）——与规则 6 触碰面白名单一致。
- [x] <Proof> 收尾回归：`mvn test -pl module-finance/erp-fin-service` 复跑全绿（审计只读不变式复证；全仓验证归 Closure Gates 机制）
      - Skill: none
      - 证据（T0 收尾时点）：复跑 **Tests run: 533, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（exit 0），与首轮一致零回归。

Exit Criteria:

- [x] `ck-finance-posting-r3.md` 落盘且五维矩阵 5 格 verdict 完整（缺一格不算完）
      - 落盘在案，矩阵 5/5（B/F/S/T/I 各带判定面+机械结果+verdict），无缺格。
- [x] 双索引行追加在案；零生产代码改动核证通过；fin service 回归根绿
      - 双索引三处行/注记追加在案；零生产路径核证通过；533/0/0/0 复跑全绿在案。

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-08-1042-1-m11-finance-posting-five-dim-audit-1-063ed376 to 2026-09-07-171530-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-08-1042-1-m11-finance-posting-five-dim-audit-1-063ed376（补 Closure Gates——按 MI.9 同批先例为本只读审计计划定制门控；frontmatter `status: draft` → `active`；Phase 5 Targets 补 `<SVC>` 绑定注记；基线引用已实仓复验——HEAD `40512c567`/脏面/冻结清单 §1.5·§3.2·§4·§6-E1/三项技能名/`TestErpSeedDataIntegrity`/MI 终态行数字/325+999 flux 数字逐一在盘）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-finance-posting-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-finance/erp-fin-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 <域切片>」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-08.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-finance-posting-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- pass test 20260908-1158-closure-r1 exit=0

## Closure

- dispatch audit #audit-20260908-1158-m11-finance-posting-1-66e6b064 to 2026-09-07-171530-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260908-1158-m11-finance-posting-1-66e6b064：独立闭包审计 ACCEPT——35/35 勾选全绿，五维矩阵 5/5 verdict 落盘（B=finding 归并态 0 新立 / F/S/T/I=pass；复用 5 / 归并 15 / 新立 0，历史 ID 零覆写）；闭包 visit 实跑 `mvn test -pl module-finance/erp-fin-service` 533/0/0/0 BUILD SUCCESS（exit 0，与 known-good-baselines MI 终态行一致）+ `check-hardcoded-cjk --strict` PASS exit 0 + `git status` 生产路径零触碰核证；plan-check `--strict` derivedCompleted 成立
