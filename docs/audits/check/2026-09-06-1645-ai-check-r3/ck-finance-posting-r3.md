# ck-finance-posting-r3 — finance「过账与凭证」fin-1 五维符合性审计报告（ai-check-r3 M1.1）

> 工作项：M1.1（U05 × 五维 × fin-1，含 posting processor 族本体唯一归属，冻结清单 §4 映射表第 1 行 / §3.2 归属裁决）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `8825a10e12ffcf364f364076efb85d857075d2fb`；脏面 = 仅 3 个未跟踪 plan 文件（`docs/plans/2026-09-08-1042-{1,2,3}-*.md`，mission-driver 同批生成），零生产路径脏面（脏树实跑 + 披露，MI.9 收官审计先例）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U05 行 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：posting 引擎/凭证/红冲/sweep/过账日志（`module-finance/erp-fin-{dao,service,web}` src/main）；owner docs `posting.md`/`posting-log.md`/`state-machine.md`。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（fin-1 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查 + processor-extension-pattern 对齐 + 维度⑮断言抽样 | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0）；R6=2 两处 REQUIRES_NEW 豁免注释在位（=基线）；checker 19 规则逐项 = M0.3 快照行零漂移；`_gen`/`__XGEN_FORCE_OVERRIDE__` 零手改；聚合器含 `erp-fin.action-auth.xml`（E1 勘误路径）；15/15 维度无跳维（⑫指针 DIM-T）；维度⑮ 3 doc × 6 断言 6/6 一致零漂移未触发扩样 | **finding**（0 新立；5 复用 + 15 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + fin-1 面 = 凭证录入/凭证模板页 | `npm run validate:flux` step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 exit 1 余项 = 325 条全部 `variant=primary` dropdown-button 既有 stub 外部漂移族（fin 域 29 条同族，非本切片 finding，successor 在案）；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；凭证录入页保留层 `x:extends="_gen/…"` + REST `@query:/@mutation:` + i18n-en 全合规；凭证模板页纯 codegen 合规；E2E：`E2E_ENGINE` 缺省 flux 实证（engine.ts L8-11）、PageObject 模式、GraphQL 调用 = runbook L122 豁免的非页面路径 API 断言通道 | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + P2P/O2C posted 一致性 | `TestErpSeedDataIntegrity` 4/4 全绿（BUILD SUCCESS）；`git status --porcelain _init-data/` 空（零 seed 变更）；资产清点 372 CSV + 1 SQL（= 冻结口径）；deploy `_seed_*.sql` 命中 cs/notify 两族均已在 seed-data.md §97 登记处聚合（无第三态）；P2P/O2C `posted=true` 恰 4 单（PINV/PAY/SINV/REC-2026-001）↔ 4 回链双向命中；8 voucher ↔ 8 billR 双射；零「已审核+posted=false」行 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P0/P1 关键业务流 + postNow 同步缝 | `mvn test -pl module-finance/erp-fin-service` **533/0/0/0 全绿**（= MI 终态行计数零回归）；覆盖对账：Voucher BizModel 5 方法 / PostingException 6 / Template 1 / GlMappingRule 2 逐方法有测试，三裸 CRUD BizModel 零自定义方法；`_cases posting/` 20 族 + src/test 27 类无孤儿；P0 凭证生成+过账+红字冲销（PostingService/ReversalDispatch/BalanceAndSchemaFilter）、P1 多币种（FxRateGuard/MultiCurrencyBalance）+ 期末结账/成本核算行（PeriodCloseEndToEnd/ProfitLossClosing/AnnualClose）、postNow 同步缝（PostingService + dispatcher 族 + FaultInjection）全覆盖，**缺口 = 0**；`SnapshotTest.RECORDING` = 0 残留；快照版本列/审计列屏蔽合规（533 全绿 = CHECKING 态等价证明） | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，单向收紧成立）；`--self-test` **PASS**；fin 探针族（CAT-1 54 / CAT-2 20）维持清零零回归；WHITELIST fin 条目抽样 5（≥3）：5/5 四要素齐备（路径/理由/owner doc 指针/裁决来源），实仓复核 `@Description`×1 / `增值税专用发票` L91 / OCR `P_*` 常量族均在位（登记准确零缺陷）；`grep -L @Locale` *Errors.java = 空（22 文件全标）；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-finance-posting.md` C3.1 全 19 条逐一比对）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/` + §Mission 基线快照（R2b/R12 等命中均为已裁决偏离）。**本轮新立 0 条**（`P{n}-CK-fin-{NNN}-r3` 无新增）；历史 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 5 条

| 原 ID | 修复在位证据（T0 = HEAD `8825a10e`） |
| --- | --- |
| P1-CK-fin-001（sweep 成功不回写 posted） | F2.1 闭环在位：sweep `doRetry` 成功后 `dispatchPostedEvent`（ErpFinDeferredPostingRetryHelper L111-117）+ 手动 `RetryProcessor.retry` L56-58 双通道 + `ErpFinPostedListenerRegistry`（collect-beans + try/catch 隔离 + O-19 warn）+ `FinPostedListener`（finance 本域四族回写、已 true 跳过、miss no-op）+ `TestFinPostedListenerWriteback`/`TestPostedListenerSuspensionRecovery` 测试在位 |
| P1-CK-fin-002（postVoucher 无平衡校验） | `ErpFinVoucherBizModel.assertBalancedFromLines`（L102 调用，L212-230 实现：Σdebit vs Σcredit + 头合计重算持久化）在位 |
| P1-CK-fin-003（幂等命中返回 null 悬挂） | 引擎 `process` 幂等命中返回 `existing.getId()`（L140-146，账套过滤重载 findPostedVoucher）在位 |
| P1-CK-fin-004（SchemaPropagator 无 ACTIVE 过滤） | `findActiveSchemasByOrg` `eq("status", AcctSchemaResolver.STATUS_ACTIVE)`（L116）+ statusScore 死代码化简（nature 排序）在位 |
| P1-CK-fin-005（null 账套静默成功） | 引擎 try 首段 fail-closed 守卫 `ERR_NO_ACTIVE_SCHEMA`（L149-158，经 recordPostFailure→PENDING→sweep 链）+ `testNullSchemaFailsClosed` 在位 |

### 2.2 归并（同型 open 追加证据至原 ID）— 15 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-fin-006 | sweep `doRetry`/batch loader 仍无源单有效性校验通道（rebuildEvent 直 post/reverse） |
| P2-CK-fin-007 | `translateFactsForSchema` 死变量 `mdSubjectBiz`（L727）+ daoFor 直查 `ErpMdSubject`（L755）原样，无豁免注释 |
| P2-CK-fin-008 | catch 块自赋值 `event.setAcctSchemaId(event.getAcctSchemaId())`（L219）原样 |
| P2-CK-fin-009 | `buildReversalDraft`（L806-820）不复制 amountSource/amountFunctional + `prepareReversalContext` 首行汇率（L601-605）原样 |
| P2-CK-fin-010 | `resolveAcctSchemaIdFromContext` 恒返回 null（L699-701）原样 |
| P2-CK-fin-011 | `existsItem`/`findItems`（L75/L199-209）查重无 acctSchemaId 维度原样（cancelOnReverse L134 同） |
| P2-CK-fin-012 | `countUnresolved` findAllByQuery().size()（L209-211）+ countVouchersSince/countExceptionsSince/countManualResolutionsSince（L297/304/312）原样 |
| P2-CK-fin-013 | 手动 retry `rebuildEvent` 汇率回退 ONE（RetryProcessor L105）+ BizModel 死代码 rebuildEvent（L338）原样 |
| P2-CK-fin-014 | 手动 retry 外层事务翻 RETRYING（回滚丢计数）+ recordPostFailure 增生 PENDING（RetryProcessor L42-47）原样 |
| P2-CK-fin-015 | `reverseVoucher` 仅置 isReversed（L112-124）、源单不回退/辅助账不取消/无事件原样 |
| P3-CK-fin-016 | `findBillLinks` null businessType NPE（L969）原样 + **新增同族站点**：`RetryProcessor.parseBusinessType`（L114-119）裸 `valueOf` 脏值抛 IAE 非业务错误（sweep 侧 parseBusinessType 已有 warn+null 处理，手动通道行为分裂同型） |
| P3-CK-fin-017 | `resolveOpenPeriod` 无 orderBy + get(0)（L524-545）原样 |
| P3-CK-fin-018 | `CommitmentVoucherGenerator` 红冲行 dcDirection 保留 + 借贷互换 + amountSource 正数（L228-238）原样 |
| P3-CK-fin-019 | `PostingRun.captureTemplate` 恒置 null（L476-478）原样 |
| P3-CK-fin2-012（跨切片归并：finance 域 currentUserId 宽 catch 家族） | **fin-1 新站点**：`ErpFinPostingExceptionRetryProcessor.currentUserId`（L121-128）宽 catch 返 null 无日志，与该 open 家族（ck-finance-arap 登记「跨域同型」）同型，追加证据不重复立项 |

### 2.3 新立 `-r3` — 0 条

本轮全部候选发现均命中历史同型（fixed 复用 / open 归并），无确属新发项。

### 2.4 归属标注（§3.2 共享代码边界）

- posting processor 族本体（引擎/dispatcher 族/Registry ×2/sweep/PostingRun/`FinPostedListener`/posting.md §同步测试缝）= **本格 fin-1 唯一归属**，上表全部控制点属本格。
- 消费侧涉及点：sal 红冲前置/inv 凭证 REQUIRES_NEW/mfg 完工入库/ast 折旧过账的消费侧行为**归各域切片**（M1.13/M1.5/M1.9 等），本格不重复立项（r1 已裁决的 P1-CK-sal-003→fin-001 承接链即此范式）。
- common 抽象族（`AbstractProcessor.illegal*` 等）调用点已审合规；基类行为缺陷归 U20（M1.15）。
- `E2E runbook 合规抽样`全局面归 U21（M1.16）；本格仅按 §1.2 ③ 核 fin 涉及 spec（结果见矩阵 DIM-F 行）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 5（fin-001..005） | 0 |
| P2 | 0 | 0 | 10（fin-006..015） |
| P3 | 0 | 0 | 5（fin-016..019 + fin2-012 跨切片站点） |
| **合计** | **0** | **5** | **15** |

五格 verdict：DIM-B **finding**（归并态，无新立）/ DIM-F **pass** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：fin-1 范围引擎（1025 行全读）+ sweep + VoucherBizModel + RetryProcessor + FinPostedListener + PostedListenerRegistry + SchemaPropagator + ArApItemGenerator 关键区 + DocumentStateMachine 全读；机械程式全套实跑（checker 19 规则 / 反模式族 / codegen 安全 / 聚合完整性 / validate:flux / seed 门禁 / fin 回归 ×2 / strict+self-test）；owner docs 3 份 × 6 断言抽样；r1 19 条逐一比对裁决。
- **未深查（边界归属）**：`erp-fin-web` 与后端契约全量 drift（归 U21/M1.16 全局面）；`ErpFinBusinessType` 枚举与字典逐项比对（voucher-back-link-patterns 已声明刻意分歧，r1 同口径保留）；`ErpFinPostingMetrics` 环形采样并发正确性（仅结构扫描，r1 同口径）；intercompany/budget/period 生成器与引擎交点外的内部逻辑（归 fin-3/fin-4 切片）；测试代码仅作覆盖对账消费。
- **残留风险（登记不裁决）**：① 15 条归并 open finding 的修复归 M2.2（finance P1 修复批）及其后批次，其中 P2-CK-fin-015（reverseVoucher 业务凭证失配）与 P2-CK-fin-011（多账套辅助账幂等）在多账套/红冲活跃场景下影响面最大，建议 M2.2 优先级排序参考；② P3-CK-fin-016 新站点（RetryProcessor 裸 valueOf）与 P3-CK-fin2-012 新站点（currentUserId 宽 catch）已追加至原 ID 证据，修复时同族一并收口；③ `validate:flux` 325 条 variant 漂移与 compliance R12a/c 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接上表 open 项。
