---
status: active
mission: ai-check-r3
work-item: M1.4
group: "2026-09-08-2238"
verify: [test]
---

# 2026-09-08-2238-3 M1.4 finance fin-4 五维符合性审计（期间结账与银行对账 + 跨域凭证链路切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）；M1.1 done（plan `2026-09-08-1042-1`，closure audit ACCEPT 2026-09-08，`ck-finance-posting-r3.md` 五格矩阵落盘；roadmap 行 done 翻转归 owner/engine 机制，依赖满足以其闭包回执为准）。M1.4 与同批 M1.2（2238-1）/M1.3（2238-2）互不依赖、均为独立只读审计；按 roadmap 文档序 fin-4 在 fin-2/fin-3 之后。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U05 × 五维 × fin-4**（§4 映射表第 4 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- 切片范围（U05 fin-4，§3.2 切片行）：期间/结账/银行对账/合并抵销/跨法人；owner doc `docs/design/finance/period-close.md` + `docs/design/finance/bank-reconciliation.md`（实仓核验在盘；抵销/跨法人判定面辅锚 `docs/design/finance/intercompany-consolidation.md`）；物理面 `module-finance/erp-fin-{dao,service,web}` 的 `src/main` 中期间/结账/银行/抵销/跨法人族——`service/close`、`service/annualclose`、`service/profitloss`、`service/bankrecon`、`service/intercompany`、`service/fx`（重估）包 + `ErpFinAccountingPeriod`/`ErpFinAccountingPeriodStatus` BizModel/`ErpFinTrialBalanceBizModel`/`ErpFinBankReconciliation(+)Line`/`ErpFinBankStatement(+)Line`/`ErpFinConsolidationElimination`/`ErpFinIntercompany*` BizModel 族 + ProfitLossClosingService/AnnualCloseService/FX 重估服务族。
- DIM-B 焦点（§3.3 U05 行 fin-4 列）：结转账套过滤/FX 重估累计口径/期间竞态；§1.1 全套逐切片跑（processor-extension-pattern 额外对齐为 fin-1 专属增量，本切片不适用）。
- 跨域凭证链路边界（§3.2 共享归属 + r1 裁决）：跨法人调拨的 **inventory 侧确认 processor 消费点**行为归 inv 切片（M1.13 已闭合，`ck-inventory-r3.md` 在案）——本切片审 finance 侧本体（intercompany 转移/定价/匹配 BizModel 与凭证生成通道）；发现涉消费侧时标注「归属 <域切片>」归并，不重复立项。
- 共享代码唯一归属（冻结清单 §3.2）：posting processor 族本体（`ErpFinPostingService`/凭证引擎/dispatcher 族/`FinPostedListener`/sweep/`PostingRun`）唯一归属 fin-1（已审计闭合，0 新立）——本切片只审结账/结转/重估/抵销/跨法人对凭证引擎的**消费侧调用点**及引擎外直写通道（r1 P2-CK-fin4-007 同型面）；common 抽象族行为缺陷归 U20，本切片只审调用点；聚合横切面归 U21；notify 派发子系统本体归 U11。
- 跨轮查重源（§2）：r1 `docs/audits/check/ai-check-index.md` 同域报告 `ck-finance-period-misc.md`（C3.4：0 P0 / 3 P1 / 8 P2 / 10 P3，done；fin4 族 P1-CK-fin4-001..003 fixed / P2-CK-fin4-004..011 open / P3-CK-fin4-012..021 open（共 10 条，与 C3.4 统计 10 P3 一致）——全表以 `ai-check-index.md` §Finding 追踪为准）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（已裁决偏离不重复报告）+ fin-1 r3 报告 `ck-finance-posting-r3.md` §2 归属标注（消费侧涉 fin-4 的归并指针）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：fin 探针族（CAT-1 54 / CAT-2 20）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块与 actual 的批前在案差距归 successor `ai-check-r3-compliance-baseline-raise`，非本切片范围。
- 仓库现状（2026-09-08 实核）：HEAD `dc31a2555`；`git status --porcelain` 无已跟踪文件改动（未跟踪面 = 本批 2238-1/2/3 三份计划文件自身）。审计证据以实跑时 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：U05 × 五维 × fin-4 五格 verdict 未落盘；本轮尚无 `ck-finance-period-misc-r3.md` 报告。

## Goals

- 按冻结清单对 U05 × 五维 × fin-4 五格全跑（禁止抽样、禁止跳维），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-period-misc-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 fin-1/fin-2/fin-3 格（M1.1/M1.2/M1.3）与其他单元格；posting 引擎内部缺陷归 fin-1（已闭合），本切片发现涉引擎内部时标注「归属 fin-1」归并不重复立项；inventory 侧调拨确认消费点行为归 inv 切片（已闭合）。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 在位
      - Skill: none
      - 证据（T0=2026-09-09）：目录在位；`ai-check-r3-index.md` + `m0-5-audit-checklists.md` 双文件确认存在（本轮索引 L3 头部登记路径与本目录一致）
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露（同批 2238-1/2238-2 为独立只读审计计划，无并发写面；MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点
      - Skill: none
      - 证据：HEAD `65750e0701faad23f27bc0ac364222df89a62efe`；`git status --porcelain` = 仅 1 个未跟踪文件（本计划 `docs/plans/2026-09-08-2238-3-*.md` 自身），零已跟踪文件改动；姊妹 2238-1（fin-2）/2238-2（fin-3）已落库（git log `b21e0f74d`/`65750e070` 闭包 ACCEPT），无并发写面。计划 Current Baseline 所记 `dc31a2555` 为起草时点，姊妹切片正常落库后 HEAD 前进，按计划口径「审计证据以实跑时 HEAD + 脏面披露为准」（MI.9 先例）。后续证据注记统一引用 T0=`65750e07`
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据：checker 19 规则逐项 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42）；CJK report mode CAT-1..4 = 0/0/0/0（CAT-5 注释豁免 21158 行仅统计）= MI 终态行零回归

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.4 行指定）
> Targets: `module-finance/erp-fin-dao|erp-fin-service/src/main/java`（fin-4 范围 = close/annualclose/profitloss/bankrecon/intercompany/fx 包 + 期间/试算平衡/抵销/跨法人 BizModel 族文件）；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由；`LocalDate.now` 等非可控时钟对照 r1 P3-CK-fin4-013 open 站点）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（§6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0=`65750e07`）：①checker 19 规则 = M0.3 快照行零漂移（见 Phase 1）；②反模式族 fin-4 scope 65 文件全零（extends RuntimeException=0 / @Inject private=0 / System.currentTimeMillis=0 / @Transactional=0——R6=2 基线条目均在 fin-1 VoucherBizModel，非本切片）；③非可控时钟=0——**r1 P3-CK-fin4-013 open 站点已修复**：`e37ddfb15`（2026-08-28 plan-2026-08-28-0219-1 P2-15「LocalDate.now → CoreMetrics」）将 `ErpFinBankReconAutoReverseHelper` 候选窗口改 `CoreMetrics.currentDate().withDayOfMonth(1)`（L117 现场复核在位），索引行仍 open = 回填缺口（hr-1 r3 P1-CK-hr-001 同判例：复用 + 状态回填归索引 owner 流程）；④`IDaoProvider|IOrmTemplate` 命中 15 文件 63 站点——豁免注释在位 3（BankStatementImporter L36-37「跨实体访问经 daoProvider…无独立 IBiz」/ ErpFinBankReconAutoReverseHelper L110-111「batch helper 非 BizModel…对齐 ErpFinDeferredPostingRetryHelper 范式」/ ErpFinAccountingPeriodProcessor L65-66 session 边界注记），**13 文件命中处无注释理由**，其中跨域直查站点 11 处（ErpMdSubject ×7 / ErpMdCurrency ×2 / ErpMdWarehouse ×1 / ErpMdOrganization ×1，ErpFinIntercompanyTransferBizModel 作为 BizModel 注入 IDaoProvider 直查 md 族）→ 新立 P3-CK-fin4-022-r3（fin3-017-r3 同族判例）；⑤codegen：`__XGEN_FORCE_OVERRIDE__` 命中全为 erp-fin-meta dict.yaml codegen 只读校验点（src 零手改、`_gen` 脏面空）；⑥E1 勘误路径实存且 `erp-fin.action-auth.xml` 已注册（x:extends 含 `/erp/fin/auth/erp-fin.action-auth.xml`）
- [x] <Proof> 15 维度逐维走查 fin-4 范围（程序式确定性走查落 verdict；重点：①Model→Delta→Java ②跨实体 I*Biz ③NopException ⑧状态机（期间/银行对账单状态机与 dict 死状态面——r1 P3-CK-fin4-015 open 站点复核）⑨审批流/作业（结账前置检查链/银行对账 autoMatch）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：15/15 无跳维。①⑥标准模式（Facade BizModel extends AbstractErpCrudBizModel + 6 per-mutation Processor 单一真相源 protected step + StateMachine Bean 纯函数）合规；②跨实体=④命中面（见上，新立 022-r3）+ `IBizObjectManager` G3 门控跨域（inv/ast 集成错误传播分级注释齐备）；③异常全部 `NopException`+`ErpFinErrors` 领域码（ERR_TRANSFER_PRICE_NOT_FOUND/ERR_PRE_CHECK_BLOCKED/ERR_REVERSE_CLOSE_* 等，无裸 RuntimeException）；⑧期间 5 态 dict 实仓 dump 与 StateMachine Bean 4 命名动作/openPeriod/两段 close（CLOSING 瞬态事务内注释在位）逐边一致——**BankStatement docStatus 永驻 DRAFT 站点复核在位**（仅 import 写 DRAFT，无 POSTED/CANCELLED writer，dict DRAFT→POSTED→CANCELLED 状态机未接线 → 归并 P3-CK-fin4-015）；BankReconciliation DRAFT→POSTED→CANCELLED 三态 writer + illegalTransition 守卫在位（BankReconciliationBuilder L96/121-140）；⑨结账前置检查链（unposted/unsettled/allowance shortfall 硬阻断 + auto-post-on-close 门控降级）与 owner doc §结账前置检查逐条对齐；autoMatch 逐行 findCandidates（窗口凭证全载 + occupied 全载 per line）→ 归并 P2-CK-fin4-011；job 面 bank-recon-auto-reverse 双层门控 + per-item REQUIRES_NEW 失败隔离在位；⑮抽样见下条。三焦点：FX 重估累计口径（`aggregateBankSubjectBookFunctional` L225-258 全期间聚合 + 自身分录/影子凭证排除 = P1-CK-fin4-001 复用在位）、结转账套过滤（AnnualClose `findYearPostedVoucherIds` L346-348 + 行级 L306-309 + populateNextYearOpening clear L158-163 = P1-CK-fin4-002 复用在位；ProfitLossClosing L81/L194-197 同）、期间竞态（closePeriod L51 入口 assertCanClose + L87-95 状态簿记无提交时复查 → 归并 P3-CK-fin4-021）。跨域凭证链路边界：intercompany 转移/定价/匹配 BizModel + 生成器通道已审（fin4-007/018 站点归并），inventory 侧消费点（ErpInvTransferOrderConfirmProcessor L65-67 catch RuntimeException→warn）标注「归属 inv 切片（M1.13 已闭合）」不重复立项
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`docs/design/finance/period-close.md` + `bank-reconciliation.md` 中 ≥2 doc × 2 关键断言（结账前置条件/反结账边界/银行对账调节恒等式/autoMatch 语义/期间锁定守卫）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
      - 证据：2 doc × 4 断言 = 8 核查点全一致零漂移（< 2 未触发扩样）。period-close.md：A1 §期间控制 5 态 + 迁移边 ↔ dict 实仓 dump（OPEN/CLOSING/CLOSED/NEVER_OPENED/CLOSED_FINAL）+ StateMachine Bean 逐边一致；A2 §反结账审计轨迹（reason 必填→kill-switch 默认 true→次年期间门控→审计三列写入时序）↔ ReverseCloseProcessor L39-70 + isReverseCloseApprovalRequired L648-650 默认 TRUE 逐条一致；A3 §期末结账向导 5 Facade mutation ↔ BizModel L51-87 全在位（preCheck/closePeriod/finalizePeriod/reverseClose/generateNextYearPeriods）；A4 §同事务期末凭证可见性裁决（CloseVoucherWriter flush 契约 + PERIOD_CLOSE 必含 FX 腿）↔ CloseVoucherWriter L143-145 flushSession + ProfitLossClosingService L91-96 仅排除 PERIOD_CLOSE 自身、EXCHANGE_GAIN_LOSS 腿参与结转。bank-reconciliation.md：B1 §schema补注 恒等式实现承载（statementBalance−bookBalance = bankCreditUnrecorded−bankDebitUnrecorded）↔ BankReconciliationBuilder L66-81 逐项一致；B2 §schema补注「无 posted 三件套 + 无 adjustVoucherId 列 + 经 VoucherBillR 反查」↔ orm.xml 0 命中 adjustVoucherId + BankReconAdjustmentVoucherBuilder L111 billR 直写；B3 §业务规则3 方向语义（银行借=账面贷）↔ BankStatementMatcher L106-114 oppositeDirection；B4 §schema补注 导入幂等键 refNo 优先+组合键回退+strict 配置 ↔ BankStatementImporter L150-210 逐项一致
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-finance-period-misc.md` §Finding 追踪 + r2 目录 + §Mission 基线快照 + fin-1 r3 报告归属标注）：同型 fixed=复用（复核 HEAD 仍有效）/同型 open=归并原 ID/确属新发=新立 `P{n}-CK-fin4-{NNN}-r3`；裁决证据落勾选注记（报告落 Phase 7）
      - Skill: code-quality-audit-prompt
      - 裁决（DIM-B 面，全 21 条 r1 fin4 ID 逐一复核 + 新立 3）：复用 4 = P1-CK-fin4-001（FX 累计口径 L225-258）/002（账套过滤三处）/003（qty×price L95-129，多物料近似 successor 注记在位）+ P3-CK-fin4-013（时钟已修 e37ddfb15，索引回填缺口注记）；归并 17 = P2-CK-fin4-004（FX/年度聚合无 orgId：ExchangeRevaluation L105-112/L164-169/L225-258 + AnnualClose L330-331 现症在位）/005（notIn 仅 SETTLED/CANCELLED L107-108，WRITTEN_OFF 未排除）/006（候选识别无 exists 前检 L45-106）/007（voucherDate=today L298/L206 + 无期间锁定 + 科目静默降级 L276-280/L357-366）/009（findStatementIdByAccount 仅最近一张 L201-210）/010（generateNextYearPeriods L40-42 无 orgId + L54 orgId 兜底 "1"/他组织值 + findNextYearJanuaryPeriod L360-366 收 orgId 不用 + hasNextYearPeriods L130-136 无 orgId）/011（autoMatch per-line 窗口全载 + occupied 全载 L52-60 + findCandidates L57-93）/012（reverseClose L63-88 零 TrialBalance 清理）/014（isExpenseTypeRequired L374-377 挂 CONFIG_EXPENSE_APPROVAL_REQUIRED）/015（BankStatement 永驻 DRAFT）/016（REVENUE_COST/IP 复用 MATCHED 金额 L72-106 注释「简化」owner doc 未登记）/017（候选不存在误抛 ALREADY_POSTED L47-50 + DRAFT 抵销凭证 L89 阻断结账）/018（红冲行 dcDirection 保留借贷互换 L229-231）/019（endingBalance 回退 currentBalance Importer L68/108）/020（billData 字面量 L90-92 + config 键字面量 L133/136）/021（无提交时复查 L87-95）+ P3-CK-fin4-012/013 之外的 P3 全数现场复核在位（P2-CK-fin4-008 = inventory 侧消费点，归属 inv 切片 M1.13 已闭合，本格注记不重复立项）；新立 3 = P3-CK-fin4-022-r3（13 文件 daoFor 无豁免注释、跨域 11 站点）/ P3-CK-fin4-023-r3（fin-4 自定义 mutation 13 个零 FNPT 注册，仅 closePeriod/reverseClose 在册——drp-018/b2b-011/prj-022-r3/qa-029-r3 同族）/ P3-CK-fin4-024-r3（intercompany 配对凭证 + 抵销凭证 currencyId/acctSchemaId 硬编码 "1"，currency 面无 successor 登记且对照 fin3-005 修复体未覆盖本通道）；跨切片注记 1 = ExchangeRevaluationService.resolveAcctSchemaId L314 / AnnualCloseService L408/L420 魔法默认 "1" 同型追加至 P3-CK-fin2-017 族证据（不另立 ID）。历史 21 ID 零覆写。

Exit Criteria:

- [x] U05×B×fin-4 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成（复用/归并/新立逐条在案）

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: `module-finance/erp-fin-web/src/main/resources/_vfs`（fin-4 面：期间管理/期末结账向导/试算平衡/银行对账/银行对账单/合并抵销/跨法人页——§3.3 U05 F 行「报表/期末结账页」清单成员）；E2E 涉 fin spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案，非本切片 finding）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）
      - Skill: none
      - 证据：step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`（导出零 error）；整体 exit 1 余项 = **325 条 ERR 全部** `variant "primary"` dropdown-button 既有 stub 外部漂移族（非 variant ERR = 0），与计划预期数字逐一吻合，successor 在案非本切片 finding；flux-only：`component="AMIS"` 保留层 = 0、ORM `ext:web-renderer="flux"` 缺失 = 空（双 grep 全零）
- [x] <Proof> fin-4 页面走查：期间管理/期末结账向导/试算平衡/银行对账/银行对账单/合并抵销/跨法人页对照 view-and-page-strategy（REST `/r/` 数据访问 / codegen vs 手写边界查 M0.4 矩阵 / 定制走 `x:extends` / i18n-en 承载约定）逐页落 verdict；涉 fin E2E spec（期末结账/银行对账/跨法人族）时核对 PageObject 模式 + `E2E_ENGINE` 缺省 flux + 禁 GraphQL 断言（runbook L122 非页面路径 API 断言豁免通道除外）
      - Skill: none
      - 证据：fin-4 面 11 页族逐页走查无跳页——10 实体页族（AccountingPeriod/AccountingPeriodStatus/BankReconciliation(+Line)/BankStatement(+Line)/ConsolidationElimination/IntercompanyMatch/IntercompanyTransferPrice/TrialBalance）全为 codegen 面：保留层 view.xml `x:extends="_gen/_X.view.xml"`（bounded-merge）+ main/picker page.yaml（M0.4 矩阵边界一致）；手写页 period-close-wizard（M0.4 L46-47 行 MI.8 i18nEn 已补，main.flux.yaml 21 处 + main.page.yaml 47 处 i18nEn）步骤映射注释与 owner doc §期末结账向导 5 mutation 链一致、零后端 delta；数据访问全 REST `@query:`/`@mutation:` 零 GraphQL 调用（preCheck/closePeriod/finalizePeriod/reverseClose 4 action + findPage/get 直查，codegen 面 i18n-en 承载在 ORM 模型源——orm.xml `i18n-en:displayName` 1051 处，页面 yaml CAT-4 = 0 见 Phase 1）；E2E：fin-4 涉 4 action spec（fin-bank-recon/fin-intercompany-cross-company/fin-intercompany-matching-elimination/fin-period-close-wizard）+ 1 visual spec（fin-period-close-wizard）——`data-slot|data-testid|.cxd-` 全零、`E2E_ENGINE` 缺省 flux 实证（engine.ts L7-12 `return 'flux'`）、3 spec 头注释明示 GraphQL `/graphql` 为非页面路径 setup/action/cleanup API 断言通道（runbook 豁免，fin-1/fin-2/fin-3 同判例）、PageObject 经 `_helper`+`pages` adapter 层

Exit Criteria:

- [x] U05×F×fin-4 格 verdict 落盘；全局面门禁数字（导出 error 数 / flux-only 命中数）在案对账一致
- [x] fin-4 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ fin-4 相关 seed（期间 OPEN/gl_balance/银行账户面）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - 证据：`Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` + BUILD SUCCESS（363 实体全量 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数门控全过）
- [x] <Proof> fin-4 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空（只读审计零 seed 变更）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表 + 期间/余额 seed 一致性抽查（accounting_period OPEN 状态与 org 维度、gl_balance 与 voucher 汇总自洽、银行账户 seed 与 fund_account FK 自洽）
      - Skill: none
      - 证据：`git status --porcelain _init-data/` 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结口径；deploy `_seed_*.sql` 命中 cs/notify 两族（各三方言）——与 fin-3 r3 实跑一致，均在 seed-data.md 登记处表显式聚合裁决，fin 域无 deploy seed 无第三态；fin-4 seed 一致性抽查：`erp_fin_accounting_period.csv` 单行 2026-07 org=2 STATUS=OPEN（org 维度在位）；`erp_fin_gl_balance.csv` 6 行 orgId/acctSchemaId/periodId/subjectId FK 齐 + 行内算术自洽（1130.00−960.50=169.50 closing debit 精确）；`erp_fin_fund_account.csv` 4 行 BANK 类型 subjectId FK 非空 + currentBalance 正值；bank_statement/reconciliation/consolidation_elimination/intercompany_match/transfer_price/trial_balance 种子文件族在册（FK 完整性由 TestErpSeedDataIntegrity 4/4 背书）

Exit Criteria:

- [x] U05×S×fin-4 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 期间/余额 seed 一致性抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-finance/erp-fin-service`（`<SVC>` = 本模块，同冻结清单 §1.4 记法）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-finance/erp-fin-service` 全绿零失败（数字落注记，对照在案 fin service 计数：F2.4 修复后 **530**——`docs/audits/check/ai-check-index.md` §Finding P1-CK-fin4-001..003 修复注记，MI.6 批1 先例 525 见 `docs/audits/cjk-baseline.md` 批注账；known-good-baselines 仅登记全 reactor 4006/0/0/1 聚合行，无分模块 fin 计数）
      - Skill: none
      - 证据：`Tests run: 533, Failures: 0, Errors: 0, Skipped: 0` + BUILD SUCCESS——533 = fin-2/fin-3 r3 轮实跑同值（530 基数 + fin2-018/fin3 修复批测试增量，known-good fin 计数 533 零回归）
- [x] <Proof> 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 测试目录清单逐项对账（fin-4 范围 = AccountingPeriod/TrialBalance/BankReconciliation/BankStatement/ConsolidationElimination/Intercompany 族 BizModel + ProfitLossClosing/AnnualClose/FX 重估服务）；关键业务流清单核对——期末结账+损益结转、年度结转+年初余额、FX 重估累计口径、银行对账自动匹配/自动红冲、合并抵销生成与过账（testing-strategy P1 清单行 + `period-close.md`/`bank-reconciliation.md` 用例段；r1 P1-CK-fin4-001..003 fixed 修复的回归测试在位复核）逐行核覆盖；缺口按业务关键度定级
      - Skill: none
      - 证据：fin-4 BizModel 自定义公开方法 15 个（AccountingPeriod 6：preCheck/closePeriod/finalizePeriod/reverseClose/openPeriod/generateNextYearPeriods；BankStatement importStatement；BankStatementLine autoMatch/manualMatch；BankReconciliation generate/post/reverse；ConsolidationElimination generateEliminationCandidates/postElimination；IntercompanyMatch runMatching/checkDualSideConsistency）逐项对账测试在位——期间/结账族 10 测试类（PeriodCloseEndToEnd/PeriodPreCheck/ProfitLossClosing/AnnualClose/AuxiliaryReconGate/ModuleCloseOrder/ClosingMultiSchema/ReverseClose(+AuditTrail)/PeriodStateMachine(+Matrix)/TrialBalanceCommitmentExclusion/DepreciationIntegration/ExchangeRevaluation/PeriodClosePerf）、银行对账族 6（BankStatementImport/Match/CounterpartyMatch + BankReconciliation(+EndToEnd) + ReconAutoReverseJob）、抵销/匹配族 3（IntercompanyMatchingAndElimination + PropertyErpFinConsolidationElimination + DualSideConsistency）+ `IntercompanyTransfer`（fin4-003 修复测试 testOnTransferConfirmedQuantityAmount 在位）；关键业务流清单 5 行全覆盖：期末结账+损益结转 ✓ / 年度结转+年初余额 ✓ / FX 重估累计口径 ✓（testBankFxRevaluationCrossPeriodCumulative——P1-CK-fin4-001 修复回归）/ 银行对账自动匹配+自动红冲 ✓ / 合并抵销生成与过账 ✓；F2.4 三修复体回归测试全部在位（ClosingMultiSchema 账套过滤 + AnnualClose 累计口径 + IntercompanyTransfer 数量金额），无 fin3-019 型「修复体零专属回归断言」缺口，零新 finding
- [x] <Proof> 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` expect 0（提交态零残留）+ `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - 证据：`SnapshotTest.RECORDING` = 0 残留；`delVersion` 命中 2 处均为 TestErpFinVoucherTemplateAuditLog 审计行为断言（逻辑删除轨迹验证，合规用途非屏蔽）；bankrecon 测试树 decimal `*` 通配零命中

Exit Criteria:

- [x] U05×T×fin-4 格 verdict 落盘；本地回归全绿数字在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + fin 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据：`--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318，单向收紧成立）+ `--self-test` **PASS**（self-test green）；fin 探针族（CAT-1 54 / CAT-2 20）维持 MI.9 清零零回归
- [x] <Proof> 白名单合规抽查：`docs/audits/cjk-baseline.md` §WHITELIST fin 相关条目抽 ≥3 条核对四要素（文件路径/理由/owner doc 指针/裁决来源）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空（`_` 前缀生成 i18n yaml 禁手改）；四要素缺失 = 白名单登记缺陷 finding（非同类 CJK finding）
      - Skill: none
      - 证据：WHITELIST「批 1/2 finance」5 条抽 3 条——IErpFinApDocumentBiz（E3 @Description 豁免）/ ErpFinApDocRuleClassifier（C2 功能型中文内容契约：发票类型字面量即行为本体）/ ErpFinApDocumentPipelineProcessor（C2 中文发票版式 OCR Pattern）——3/3 四要素齐备（路径/理由/owner doc=i18n-compliance.md 判定准绳表 #5 + CAT-3 行/裁决来源=plan 2026-09-07-0902-1 Phase 1 Decision C1/C2①）且 HEAD 实仓复核登记准确（Classifier `增值税专用发票/电子发票/收据` 3 字面量在位、PipelineProcessor P_INVOICE_NO/P_AMOUNT_WITH_TAX 等 4 Pattern 在位、IErpFinApDocumentBiz @Description 1 行在位）；`grep -L @Locale` *Errors.java = 空（全含 @Locale）；`erp-*-meta` + `_vfs/i18n` 零手改

Exit Criteria:

- [x] U05×I×fin-4 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-period-misc-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：逐条确认三态（复用/归并/新立）与级别（P0~P3）一致性与 ID 规范（`P{n}-CK-fin4-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - 证据：全切片汇总复裁决一致——复用 4（P1×3 fixed HEAD 复核 + P3-013 站点已修索引回填缺口注记）/ 归并 17（P2×8 + P3×9，含 008 归属 inv 注记；每条 T0 现场证据行落报告 §2.2，原 ID 状态零改动）/ 新立 3 全 P3 且 ID 规范合规（`P3-CK-fin4-{022,023,024}-r3`，续接 r1 fin4 族 021 之后，跨单元先例逐条引用：fin3-017-r3 / drp-018 / b2b-011 / prj-022-r3 / qa-029-r3 / fin3-005 修复范式对照）；级别一致性核验：3 新立均 P3 校准（只读直查面 / 权限纵深防御层 / config 默认关通道），无升级触发条件（无 P0/P1 级行为缺陷新发、无 deferred 触发条件已满足项）；跨切片追加证据 1（fin2-017 族 3 站点）不计数不另立
- [x] <Add> 落盘 `ck-finance-period-misc-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计（P0~P3 计数）+ 剩余风险声明（横切关注点 13 完整四件套）
      - Skill: none
      - 证据：报告已落盘 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-period-misc-r3.md`——五维矩阵 5/5 格 verdict 完整（B=finding / F=pass / S=pass / T=pass / I=pass）；§2 三态裁决四段（2.1 复用 4 表 / 2.2 归并 17 表 + 跨切片追加 / 2.3 新立 3 详条 / 2.4 归属标注）+ §3 统计表（P0 0/0/0、P1 0/3/0、P2 0/0/8、P3 3/1/9，合计 3/4/17）+ §4 剩余风险完整四件套（已查/未深查边界/残留风险登记/successor 触发条件）
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加本报告行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.4 行 + §Finding 追踪追加新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - 证据：`ai-check-r3-index.md` 产物清单追加 M1.4 行（ck-finance-period-misc-r3.md，五维矩阵 + 三态裁决 + HEAD `65750e07` 摘要）；跨轮 `ai-check-index.md` §报告清单追加 M1.4 行（P0/P1/P2/P3 = 0/0/0/3 done）+ §Finding 追踪在 fin4 族尾部追加 3 行（P3-CK-fin4-022/023/024-r3，状态 open，修复归 M2.x 列明）+ §r3 轮复核注记追加 M1.4 块（复用 4 / 归并 17 / 新立 3 / 历史 ID 零覆写）；归并证据落报告 §2.2 原 ID 行内（索引原 ID 行状态未动）
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6；触碰面 = 执行目录报告 + 索引 + 本计划勾选注记）
      - Skill: none
      - 证据：`git status --porcelain | grep -E "module-.*\/(src|model|deploy)|app-erp-all\/(src|pom)"` = 空（rc=1 零命中）；实脏面 = `M ai-check-r3-index.md` + `M ai-check-index.md` + `?? ck-finance-period-misc-r3.md` + `?? 本计划文件`——全部为申报触碰面（执行目录报告 + 双索引 + 计划注记），零生产路径
- [x] <Proof> 收尾回归：`mvn test -pl module-finance/erp-fin-service` 复跑全绿（审计只读不变式复证；全仓验证归 closure 审计机制）
      - Skill: none
      - 证据：复跑 `Tests run: 533, Failures: 0, Errors: 0, Skipped: 0` + BUILD SUCCESS——与 Phase 5 首跑同值，审计过程零代码改动不变式复证成立

Exit Criteria:

- [x] `ck-finance-period-misc-r3.md` 落盘且五维矩阵 5 格 verdict 完整（缺一格不算完）
- [x] 双索引行追加在案；零生产代码改动核证通过；fin service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-08-193051-mission-driver-2026-09-08-2238-3-m14-finance-period-close-five-dim-audit-1-d8618546 to opencode-main-20260908-193051
- 2026-09-08：iteration 1，共识 accept #review-2026-09-08-193051-mission-driver-2026-09-08-2238-3-m14-finance-period-close-five-dim-audit-1-d8618546

## Verification

- pass test 20260909-0223-closure-r1 exit=0

> 闭包 visit 记录（2026-09-09 02:23，独立闭包审计，fresh session）：`mvn test -pl module-finance/erp-fin-service` BUILD SUCCESS（exit 0，Tests run: 533, Failures: 0, Errors: 0, Skipped: 0 = known-good-baselines fin 533 锚点零回归）。增量口径：闭包时点工作树改动全为 docs 审计产物面（`git status` 过滤 `module-*`/`app-erp-all` 生产路径 = 0 行），按 mission 增量构建指引以 `-pl` 定面本切片 service 模块（本计划唯一涉码验证面，Phase 5/7 同锚点），不清 target 全量重编；完整仓库回归归 roadmap 收官机制（只读审计计划：验证命令组即结果表面本身，MV.1 对照面 = known-good-baselines 2026-09-08 MI 终态行）。

> 执行期记录（2026-09-09，执行会话；T0 = HEAD `65750e07` 实跑，各 Phase 勾选注记含同源数字）：本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行）；`## Closure` 留予独立结束审计（新会话非执行者上下文）落回执。增量口径：执行时点工作树改动全为 docs 审计产物面（`git status` 过滤 `module-*`/`app-erp-all` 生产路径 = 0 行）。

- PASS 2026-09-09（执行期 Phase 1/2）`bash docs/audits/nop-compliance-checker.sh` — 19 规则 = M0.3 快照行逐值一致零漂移（R1a/b/c=0/0/0、R1d=14、R2a=34、R2b=242、R2c=1542、R2d=38、R3=5、R6=2、R10=14、R12a=71、R12b=66、R12c=42，其余 0）
- PASS 2026-09-09（执行期 Phase 1/6）`node tools/check-hardcoded-cjk.mjs`（report mode）— CAT1..4 = 0/0/0/0（CAT-5 注释统计 21158 行）= MI 终态行精确一致
- PASS 2026-09-09（执行期 Phase 3）`npm run validate:flux` — step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 exit 1 余项 = 325 条既有 `variant=primary` dropdown-button stub 外部漂移族（非 variant ERR = 0，successor 在案，非本切片 finding）；flux-only：`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0
- PASS 2026-09-09（执行期 Phase 4）`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` — Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS
- PASS 2026-09-09（执行期 Phase 5/7）`mvn test -pl module-finance/erp-fin-service`（×2）— 533/0/0/0 全绿（= known-good-baselines fin 533 锚点零回归）
- PASS 2026-09-09（执行期 Phase 6）`node tools/check-hardcoded-cjk.mjs --strict` — PASS exit 0（`0 new violations vs frozen snapshot; 170 baseline files, totals CAT1..4=0/0/209/1318`，单向收紧成立）+ `--self-test` — PASS
- PASS 2026-09-09（执行期 Phase 7）`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 — 0 行（零生产代码改动，roadmap 规则 6；脏面 = 执行目录报告 + 双索引 + 本计划，全为 docs 审计产物面）

## Closure

Status Note: 本计划（只读五维审计切片 U05 × 五维 × fin-4，零生产代码改动）7 Phase 全部执行项与退出标准 `[x]`（35/35，机械红线数字注记在案）；五格 verdict 全落盘（B=finding（3 新立 P3）/ F=pass / S=pass / T=pass / I=pass）；三态裁决 复用 4（fin4-001..003 F2.4 修复 HEAD 复核有效 + fin4-013 时钟已修 e37ddfb15 索引回填缺口注记）/ 归并 17（fin4-004..011 + 012/014..021 现症复核，008 归属 inv 切片注记）+ 跨切片追加 1（fin2-017 族魔法默认 "1" 3 站点）/ 新立 3 全 P3（`P3-CK-fin4-022/023/024-r3`，历史 21 fin4 ID 零覆写）+ `ck-finance-period-misc-r3.md`（矩阵 5/5 + 统计 + 剩余风险四件套）+ 双索引行 + 当日日志条目均在案。ledger 协议：frontmatter `status: active` 保持，完成态由全勾选 + `## Verification` pass 线 + 本节回执派生；roadmap M1.4 行 done 翻转归 owner/engine 机制（M1.1/M1.2/M1.3 同批先例），不在本闭包翻转。

Closure Audit Evidence:

- Auditor / Agent: 独立闭包审计子代理（fresh session、read-only、非执行者上下文；mission-driver 流 CLOSURE_SCRIPT_CHECK → 闭包 visit 单一独立 closer）
- Evidence: 本计划 Phase 1~7 勾选注记（含全部红线数字）+ `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-period-misc-r3.md`（五维矩阵 5/5 + 三态裁决 + 统计 + 剩余风险四件套）+ 双索引追加行（本轮 `ai-check-r3-index.md` 产物清单 1 行 + 跨轮 `ai-check-index.md` §报告清单 M1.4 行、§Finding 追踪 `P3-CK-fin4-022/023/024-r3` 3 行、fin4 族 r3 轮复核注记）+ `docs/backlog/ai-check-r3-roadmap.md` M1.4 行执行完成注记 + `docs/logs/2026/09-09.md` M1.4 条目 + 闭包 visit 实跑记录（见 `## Verification`）

- dispatch audit #audit-20260909-0223-m14-finance-period-close-five-dim-audit-1-34934a68 to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260909-0223-m14-finance-period-close-five-dim-audit-1-34934a68：独立闭包审计 ACCEPT——35/35 勾选全绿，五维矩阵 5/5 格 verdict 落盘（B=finding：复用 4（fin4-001..003 F2.4 修复 HEAD 复核有效 + fin4-013 时钟已修 e37ddfb15 索引回填缺口注记）+ 归并 17（fin4-004..011 + 012/014..021 现症复核 + 008 归属 inv 切片注记）+ 新立 3 P3（fin4-022-r3 fin-4 族 13 文件 daoFor 无豁免注释跨域 11 站点 / fin4-023-r3 自定义 mutation 13 个零 FNPT 注册 / fin4-024-r3 intercompany/抵销通道 currencyId/acctSchemaId 硬编码 "1"）；F=pass（11 页族逐页 + validate:flux 双数字 999/855 + E2E_ENGINE 缺省 flux 实证）；S=pass（TestErpSeedDataIntegrity 4/4 + 期间/余额 seed 自洽抽查干净）；T=pass（533 全绿零回归 + 15 公开方法覆盖对账 + 关键业务流 5 行全覆盖 + F2.4 三修复回归在位）；I=pass（--strict/--self-test 双 PASS + 白名单 3 抽查四要素齐备 HEAD 复核））；三态裁决 复用 4/归并 17+跨切片追加 1/新立 3，历史 21 fin4 ID 零覆写，双索引行与报告 §2/§3 逐条吻合（新立计数 0|0|3|0）；闭包 visit（2026-09-09 02:23）实跑 `mvn test -pl module-finance/erp-fin-service` 全绿（exit 0，533/0/0/0 = 锚点零回归）+ `git status` 生产路径零触碰核证（只读审计零改动红线保持）+ 实仓抽核（PeriodStateMachine 5 态 dict↔Bean 逐边一致、BankStatementImporter L72 DRAFT 单 writer 与 fin4-015 归并一致、ErpFinIntercompanyTransferBizModel daoFor 直查 md 族在位与新立 fin4-022-r3 一致、报告 §3 统计表 3/4/17 与双索引行计数一致）；语义核对（退出标准对照实仓产物 / anti-hollow 报告与双索引实存非桩 / deferred honesty 新立 3 ID 登记 open + M2.x 修复方向 / 文档四方一致：报告↔双索引↔roadmap 行↔日志条目）全 PASS；plan-check `--strict` 结构绿，derivedCompleted 由本回执 + `pass test` 线成立（单模型降级如实声明：exec = aud = zhipuai-coding-plan/glm-5.3-flash）
