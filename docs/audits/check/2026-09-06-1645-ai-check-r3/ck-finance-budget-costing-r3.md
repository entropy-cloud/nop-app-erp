# ck-finance-budget-costing-r3 — finance「预算与成本」fin-3 五维符合性审计报告（ai-check-r3 M1.3）

> 工作项：M1.3（U05 × 五维 × fin-3，冻结清单 §4 映射表第 3 行 / §3.2 切片登记；预算控制/成本方法/成本中心切片）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `b21e0f74d12ca018b2f8a66d72f5dab861f9cfc6`；脏面 = 仅 2 个未跟踪 plan 文件（`docs/plans/2026-09-08-2238-{2,3}-*.md`，mission-driver 同批生成——2238-3 为姊妹只读审计计划，无并发写面；2238-2 即本计划），零生产路径脏面（脏树实跑 + 披露，MI.9 收官审计先例）。计划 Current Baseline 所记 `dc31a2555` 为起草时点，姊妹切片（M1.12/M1.13 等）正常落库后 HEAD 前进，按计划口径「审计证据以实跑时 HEAD + 脏面披露为准」。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U05 行 fin-3 列 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：预算控制/成本方法/成本中心（`module-finance/erp-fin-{dao,service,web}` src/main 中 fin-3 面——`service/budget` 包（`ErpFinBudgetControlBiz` 控制引擎 / `ErpFinBudgetCommitmentBizModel` 承付 SPI / `BudgetVoucherGenerator`+`CommitmentVoucherGenerator` 影子凭证生成器 / `ErpFinBudgetScenarioProcessor` facade）+ `service/classify` 包 + `entity/ErpFinBudget{Scenario,Line,ControlLog,RollforwardLog,CarryForwardLog}BizModel` 5 类 + `processor/ErpFinBudgetScenario{SubmitForApproval,Approve,Reject,Cancel,RollForward,CarryForward}Processor` 6 类 + `statemachine/ErpFinBudgetScenario{Document,Approval}StateMachine` 2 类）；owner docs `docs/design/finance/budget.md` + `docs/design/finance/costing-methods.md`（成本中心判定面辅锚 `docs/design/finance/cost-center.md`）。切片标签注记：计划将 `service/classify` 描述为「成本方法/成本中心分类族」，物理面实为 AP 文档分类族（owner doc `document-driven-ap-automation.md`，E3.5），本报告按物理面原样审计，该标签不精确不影响任何格 verdict；成本方法路由本体（CostMethodResolver/CostingStrategy 族）在 inventory 域（归 U02），本切片按 r1 先例仅核 finance 调用边界 `closeInvModule→recloseInvCosts`。
> Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物。
> 共享代码边界（冻结清单 §3.2）：posting processor 族本体归 fin-1（已闭合）——本切片只审预算凭证生成/承付通道对凭证引擎的**消费侧调用点**（含 BudgetVoucherGenerator/CommitmentVoucherGenerator 生成器本体）；common 抽象族行为缺陷归 U20（本切片只审调用点）；聚合横切面归 U21；notify 派发子系统本体归 U11。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（fin-3 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点①②③⑧⑨：预算三量口径/TOCTOU 锁/结转科目维度 + approveStatus 值域/字典合规 + 预算方案审批链）+ 维度⑮断言抽样 2 doc × 5 断言 | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0 / `@Transactional`=0（R6=2 两处 REQUIRES_NEW 豁免注释在位归 fin-1 VoucherBizModel 基线）/ `LocalDate.now` 族=0，时间取值 CoreMetrics）；checker 19 规则逐项 = M0.3 快照行零漂移；IDaoProvider 命中 9 文件——豁免注释在位 2（Classifier「IDaoProvider 直访对齐 ApsLoadSourceProvider 范式」+ CarryForwardProcessor L132-133「无对应 IBiz 覆盖此跨期间查询语义」），**7 文件命中处无注释理由**（→ 新立 P3-CK-fin3-017-r3）；`_gen`/`__XGEN_FORCE_OVERRIDE__` 命中全为 erp-fin-meta dict.yaml codegen 只读校验点（src 零手改）；聚合器含 `erp-fin.action-auth.xml`（E1 勘误路径）+ budget 四页族注册 useCases=UC-FIN-13 + budget 族 bean 全注册；15/15 维度无跳维（⑫指针 DIM-T）：三量口径（`available = budgetBalance − actualBalance − commitmentBalance` 显式三通道 + ACTUAL 通道排除 BUDGET/COMMITMENT = P1-CK-fin3-002/003 复用在位）、TOCTOU 锁（per-(subject\|costCenter\|period) ConcurrentHashMap 串行锁 = P1-CK-fin3-004 复用在位）、结转科目维度（F2.3 后按源方案科目逐行写入 + 凭证段移除 = P1-CK-fin3-001 复用在位）三焦点全数在位；⑧双轴 Bean（Document 6 值 `erp-fin/budget-status` 全合规写入 + Approval 4 值 `wf/approve-status`）——**rollForward L119 仍写 approveStatus="DRAFT" 字典外值**（dict 实仓 dump = UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）→ 归并 P2-CK-fin3-006；⑨四 per-mutation Processor + AbstractX 基类专属码改道（plan 2026-09-07-2200-1 form-3）在位，carryForward/rollForward 绕 facade 守卫为 Bean javadoc 显式 justified runtime-dead/spawn 登记在案；⑮2 doc × 5 断言：三量口径/控制级别语义/结转维度/recloseInvCosts 边界 4 处一致，budget.md L257 E2E 段「结转凭证 CARRY-FORWARD- code precision 约束」stale（F2.3 后凭证段 no-op、全仓零写点）→ 漂移 1 处（新立 P3-CK-fin3-018-r3，< 2 未触发扩样） | **finding**（2 新立 P3 + 11 归并 + 5 复用 + 1 跨切片注记，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + fin-3 面 = 预算方案/预算行/预算日志 3 页族（承付无独立页；成本中心工作台页辅锚面） | `npm run validate:flux` step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 exit 1 余项 = 325 条全部 `variant=primary` dropdown-button 既有 stub 外部漂移族（fin 域 29 条同族、非 variant ERR = 0，successor 在案，非本切片 finding）；flux-only：`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；fin-3 面 5 页族逐页走查——保留层 view.xml 全 `x:extends="_gen/_X.view.xml"`（bounded-merge 定制）+ main.page.yaml/picker.page.yaml 纯 codegen 面（`x:gen-extends` GenPage 模板，M0.4 矩阵分类一致）；BudgetScenario 业务动作 6 mutation（submit/approve/reject/cancel/rollForward/carryForward）+ 2 query 全 REST `@mutation:/@query:` 零 GraphQL 数据访问（`graphql:labelProp` 命中为平台元数据键非 API 调用）；i18n-en 承载齐备（34/14/11/1/1，模型源约定）；成本中心工作台页（物理在 md-web，M0.4 L80 行）手写 `@query:ErpMdCostCenter__findPage` REST + i18nEn 12 处合规；E2E：`fin-budget-rollforward-carryforward.action.spec.ts` 零 `data-testid/data-slot/.cxd-` 命中 + `E2E_ENGINE` 缺省 flux 实证（engine.ts L8-11 `return 'flux'`）+ GraphQL `/graphql` 调用为非页面路径 setup/action/cleanup 通道（runbook「API 断言」节豁免，fin-1/fin-2 同判例） | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + 预算方案/行/日志 seed 一致性 | `TestErpSeedDataIntegrity` **4/4 全绿**（BUILD SUCCESS，363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning）；`git status --porcelain _init-data/` 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL**（= 冻结口径）；deploy `_seed_*.sql` 命中 cs/notify 两族（各三方言）均在 seed-data.md §97 登记处表显式聚合裁决（L107 notify 行 ✅ + cs 同批），fin 无 deploy seed 无第三态；预算 seed 一致性抽查：scenario 2 行 controlLevel=HARD/WARN ∈ `erp-fin/budget-control-level`、docStatus=APPROVED/CLOSED ∈ `erp-fin/budget-status`、approveStatus=APPROVED ∈ `wf/approve-status`（**无字典外 DRAFT**——与 P2-CK-fin3-006 代码面缺陷互证 seed 面干净）；budget_line 2 行 scenarioId/subjectId/periodId FK 零悬空；control_log 2 行 actionResult=PASS/WARNED ∈ owner doc 值域 + scenarioId/budgetLineId FK 零悬空；rollforward/carryforward log 各 1 行在册 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 关键业务流（预算控制 check-then-act / 承付释放对称 / 期末结转年度结转）+ F2.3 修复回归复核 | `mvn test -pl module-finance/erp-fin-service` **533/0/0/0 全绿**（= known-good-baselines fin 533 计数零回归）；覆盖对账：Scenario 6 mutation（StateMachines + EndToEnd + RollForward 3 策略 + CarryForward 4 规则+年度关账前置）、Line getBudgetVsActual（EndToEnd）、Commitment SPI 3 方法（Commitment 6 用例 + PropertyErpFinBudgetCommitmentRelease 2 seeded property 对称不变量）、3 Log BizModel 裸 CRUD 零自定义、classify（Determinism + `_cases/classify/`）逐项有测试，`_cases` 14 目录无孤儿；关键业务流：check-then-act 结局路径（HARD blocked/WARN logged+pass/NONE pass + 三量 `500=1000−300−200` 精确断言 + RESERVATION 计 actual 回归）全覆盖、承付/释放对称全覆盖、期末结转/年度结转 P1 清单行（PeriodCloseEndToEnd/ProfitLossClosing/AnnualClose）在 533 内域级覆盖（归 fin-4 格细审）；F2.3 回归复核：001（CarryForward 5 用例 + L326 F2.3 注记）/002/003（testAvailableDeductsCommitmentSeparately 精确断言）在位，**004 串行锁与 005 org/账套/币种解析零专属回归断言**（测试恒种子 orgId=1/acctSchemaId=1 身份值；无 latch/executor 并发断言）→ **新立 P3-CK-fin3-019-r3**（§2.3）；快照纪律 `SnapshotTest.RECORDING` = 0 残留、delVersion 命中为审计行为断言合规 | **finding**（1 新立 P3-CK-fin3-019-r3） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318，单向收紧成立）；`--self-test` **PASS**；fin 探针族（CAT-1 54 / CAT-2 20）维持清零零回归；WHITELIST「批 1/2 finance」5 条抽 3（含 2 条 fin-3 面文件）：3/3 四要素齐备（路径/理由/owner doc 指针=i18n-compliance.md 准绳表 #5 + CAT-3 C1/C2/裁决来源=plan 2026-09-07-0902-1 Phase 1 Decision）且 HEAD 实仓复核登记准确（Classifier L91/94/97 三行字面量逐字在位 / IErpFinApDocumentBiz @Description L22 / PipelineProcessor P_* 12 命中）；`grep -L @Locale` *Errors.java = 空；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-finance-budget-costing.md` C3.3 fin3 族 16 条逐一比对）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（无 fin3 同型独立登记）+ §Mission 基线快照（R2b/R12 等命中均为已裁决偏离）+ fin-1 r3 报告 §2 归属标注（P3-CK-fin-018 承付红冲 dcDirection 站点已由 fin-1 r3 本轮追加证据）。**本轮新立 3 条**（`P3-CK-fin3-017/018/019-r3`，全 P3）；历史 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 5 条

| 原 ID | 修复在位证据（T0 = HEAD `b21e0f74`） |
| --- | --- |
| P1-CK-fin3-001（carryForward 结转传递链三重断裂） | F2.3 在位：`appendCarryForwardLines`（L279-306）按源方案明细行科目维度写入（真实 subjectId/subjectCode + periodId + budgetAmount 占比分摊 computeLineShare L385-395）；结转凭证段移除（`writeCarryForwardVoucher` no-op L309-318，javadoc 登记 F2.3 裁决）；修复测试 TestErpFinBudgetCarryForward 5 用例 + L326 F2.3 注记在位 |
| P1-CK-fin3-002（carryForward 余量漏减 commitment） | F2.3 在位：`carryForward` L65-67 三量口径 `sourceRemaining = budget − actual − commitment` + `aggregateCommitment`（L349-382，COMMITMENT 通道凭证行聚合，与控制引擎口径一致） |
| P1-CK-fin3-003（getBudgetVsActual 方向不敏感） | F2.3 在位：`ErpFinBudgetLineBizModel` L112-116 actual 通道贷方行取负（方向敏感）；修复断言 testGetBudgetVsActual/testAvailableDeductsCommitmentSeparately（`500=1000−300−200` 精确）在位 |
| P1-CK-fin3-004（预算控制 TOCTOU 无并发防护） | F2.3 在位：`ErpFinBudgetControlBiz.check` L56-75 per-(subject\|costCenter\|period) ConcurrentHashMap 串行锁使 check-then-act 原子化（同 JVM 锁队列 + 跨 JVM ControlLog insert 冲突兜底注释）；并发专属回归断言缺位登记为 P3-CK-fin3-019-r3（不阻断本复用裁决——修复体本身 HEAD 复核有效） |
| P1-CK-fin3-005（承付凭证 orgId/acctSchemaId/currencyId 硬编码） | F2.3 在位：`ErpFinBudgetCommitmentBizModel.resolveOrgAndSchema`（L164-176，期间 org→AcctSchemaResolver.resolvePrimarySchemaId）+ `resolveCurrencyId`（L153-162，账套本位币，无账套回退占位注释）；解析专属回归断言缺位同归 P3-CK-fin3-019-r3 |

### 2.2 归并（同型 open 追加证据至原 ID）— 11 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-fin3-006 | `ErpFinBudgetScenarioRollForwardProcessor.createRollForwardScenario` L119 仍 `target.setApproveStatus(ErpFinConstants.BUDGET_STATUS_DRAFT)`——实仓 dict dump（`app-erp-all/_dump/nop-app/dict/wf/approve-status.dict.yaml`）确认值域仅 UNSUBMITTED/SUBMITTED/APPROVED/REJECTED 四值，"DRAFT" 字典外原样；seed 面无此值（互证代码面缺陷） |
| P2-CK-fin3-007 | 预算聚合全量加载原样：ControlBiz `aggregateAmount` 全凭证载入（L141-142）+ 行级二次全载（L158）；LineBizModel getBudgetVsActual 全凭证载入（L76，恒真式过滤注释自认）；CarryForwardProcessor `aggregateActualForLine`/`aggregateCommitment` 逐行 N×M 载入（L195-248/L350-382）原样 |
| P2-CK-fin3-008 | 预算 check 调用侧 costCenterId 恒传 null 原样：`ErpPurOrderProcessor` L223 / `ErpPurPaymentProcessor` L189 / `ErpFinExpenseClaimProcessor` L240 三站点全部字面 `null`——成本中心维度预算行不可达现症在位 |
| P2-CK-fin3-009 | 期间解析失败静默 fail-open 原样：调用侧 `resolvePeriodId` 返 null 直传 check（ExpenseClaimProcessor L237-240），ControlBiz doCheck 对 null periodId 无告警无日志直接走匹配（必然落空 PASS）；**新站点扩员**：`ErpFinBudgetCommitmentBizModel.commit` L60-67 承付科目解析失败（subjectId null 或科目查无）同样静默 return null 不生成承付凭证、无日志——预算余量虚高同后果链，归并本 ID 同族收口 |
| P2-CK-fin3-010 | 多币种字段失真原样：RollForward L153-154 `budgetAmountSource = budgetAmountFunctional = targetAmt`（本位币顶替源币）+ CarryForward L300-303（source=functional、exchangeRate 恒 ONE）+ CommitmentGenerator L164-166（exchangeRate ONE、amountSource=本位币 absAmount）三站点现症在位 |
| P3-CK-fin3-011 | `BudgetVoucherGenerator.toFact` 死三元原样：L189 `BigDecimal amount = isReversal ? l.getBudgetAmountFunctional() : l.getBudgetAmountFunctional();` 两分支恒等 |
| P3-CK-fin3-012 | 预算链期间解析无排序无 orgId 过滤原样：CommitmentBizModel.resolvePeriodId（L140-151，le/ge + setLimit(1) get(0)）+ RollForwardProcessor.remapPeriodId（L189-196，eq year[/month] + setLimit(1)）+ CarryForwardProcessor.isSourceFiscalYearFullyClosed（L155-158，eq year 无 orgId）三站点现症（与 P3-CK-fin-017 同型族） |
| P3-CK-fin3-013 | `ErpFinBudgetScenarioProcessor.resolveUserId` L153-161 `catch (Exception ignored) {}` 宽 catch 吞咽无日志原样（rollforward/carryforward log carriedBy/rolledBy 写 null 无信号） |
| P3-CK-fin3-014 | `CommitmentVoucherGenerator` 类 javadoc（L28-30）「仅写 Dr 行…Cr 行使用同一科目反向（保持平衡）」自相矛盾 vs 实现 `writeCommitmentVoucher` 单行单边无 Cr 行（L154-172）原样 |
| P3-CK-fin3-015 | rollForward 无幂等守卫原样（`createRollForwardScenario` L99-123 目标 code = source.code + "-" + year 无重复预检，重跑撞 UK 或增生重复方案）+ `validateNewFiscalYear` L79 `newFiscalYear <= source.getFiscalYear()` Integer 自动拆箱 NPE 风险（fiscalYear 列 mandatory 缓解但 API 直调面仍在）现症在位 |
| P3-CK-fin3-016 | `ErpFinBudgetControlBiz.findMatchingBudgetLine` L195-212 多 APPROVED 方案同维度命中无排序取首个原样——controlLevel/余量判定取决于 DB 返回序现症在位 |

**跨切片注记（不重复立项、不重复计数）**：`P3-CK-fin-018`（CommitmentVoucherGenerator 红冲行 dcDirection 保留原方向但借贷互换 + amountSource 正数）——r1 登记归属 C3.1（fin-1 格）；该生成器物理本体在 fin-3 面（承付通道），本切片复核 L232-234/L237 现症仍在；fin-1 r3 报告 §2.2 本轮已追加同站点证据（其 T0 `8825a10e`），本格按冻结清单 §0.4 归并归属切片、不重复立项。修复时随 fin-1 格 M2.x 批次收口。

### 2.3 新立 `-r3` — 3 条

**P3-CK-fin3-017-r3**（DIM-B 维度② 跨实体访问）

- **控制点**：fin-3 预算族 7 文件 IDaoProvider daoFor 直查命中处无注释理由——`ErpFinBudgetControlBiz`（L63 字段 + L133/147/196/216/246 调用）/ `ErpFinBudgetCommitmentBizModel`（L50 + L122/131/144/160/170）/ `BudgetVoucherGenerator`（L47 + L119-121/220/224/231/238/250）/ `CommitmentVoucherGenerator`（L45 + L128-130/200-202/258/274/281/293）/ `ErpFinBudgetScenarioProcessor`（L46 + L132/142/147）/ `ErpFinBudgetRollForwardProcessor`（L41 + L101/127/183/195/202）/ `ErpFinBudgetLineBizModel`（L61/62/85/147）。其中跨域站点 5 处：`ErpMdSubject` getEntityById 直查 ×4（ControlBiz L246 / LineBizModel L147 / CommitmentBizModel L122 / BudgetVoucherGenerator L220）+ `ErpMdAcctSchema` 直查（CommitmentBizModel L160）——`IErpMdSubjectBiz`/`IErpMdPartnerBiz` 在位未注入。
- **问题**：冻结清单 §1.1 程式⑤「命中处须有注释理由」机械命中——同切片对照面：`ErpFinApDocRuleClassifier`（javadoc 显式登记直访范式豁免）与 `ErpFinBudgetScenarioCarryForwardProcessor`（L132-133「无对应 IBiz 覆盖此跨期间查询语义」）均已合规登记，budget 族其余 7 文件零登记，范式不对称。
- **三态裁决**：新立。r1 fin3 族 16 条无此形态；r2 无同型独立登记；同型家族跨单元先例 = P2-CK-fin-007（fin-1 格 translateFactsForSchema）/ P3-CK-ast-028-r3 / P2-CK-qa-027-r3（qa 三站点 P2）——按 qa-027/ast-028「跨单元另立先例自立 ID」先例新立。级别 **P3**：命中全为只读（getEntityById 主键查/等值查），无跨域写、无行为分歧（ast-028-r3 同形态 P3 先例；qa-027-r3 的 P2 涉库存余额查询语义风险更强）。全仓同型扫描归 U20/M1.15。
- **修复建议**：归 M2.x——md 直查站点注入 `IErpMdSubjectBiz`（或按 fin-2 先例补豁免注释）；同域 daoFor 站点补一行豁免理由注释（对齐 CarryForwardProcessor 措辞）；与 fin-007/ast-028-r3/qa-027-r3 同族一并收口。

**P3-CK-fin3-018-r3**（DIM-B 维度⑮ owner-doc 漂移）

- **控制点**：`docs/design/finance/budget.md` §浏览器层验证（A2）L257——「voucher code precision 50 约束：结转凭证 code = "CARRY-FORWARD-"+sourceCode+"-"+targetCode+"-"+uuid8，source/target code 用单字符短前缀规避」。
- **问题**：F2.3（P1-CK-fin3-001 修复）已将结转凭证段整体移除（`writeCarryForwardVoucher` no-op，CarryForwardProcessor L309-318 javadoc 显式登记「凭证载体归 owner doc 后续裁决」），全仓生产代码零 `CARRY-FORWARD-` 凭证写点（仅测试注释明示「不再伪造前缀」）——doc 该断言描述的凭证与其 precision 约束已不存在，E2E 段与实现脱节（同段其余断言 carriedAmount 派生/docStatus=CLOSED/Log 写入仍与实现一致）。
- **三态裁决**：新立。r1 fin3 族无 doc 漂移条目；r2 无同型；同型先例 = P3-CK-pur-016-r3（javadoc 口径措辞漂移）/ P3-CK-prj-025-r3（owner-doc 断言过期）→ 归 doc 维护批。级别 **P3**（纯文档措辞级 stale，零行为影响；⑮抽样漂移计数 1 < 2 未触发扩样）。
- **修复建议**：归 doc 维护批——L257 移除结转凭证 code 约束句，改注「F2.3 后结转经 BudgetLine 承载、凭证段移除（载体归后续裁决）」；与 E2E spec 头注释同步。

**P3-CK-fin3-019-r3**（DIM-T 覆盖缺口）

- **控制点**：F2.3 修复体中 P1-CK-fin3-004（`ErpFinBudgetControlBiz` per-维度 ConcurrentHashMap 串行锁，L56-75）与 P1-CK-fin3-005（`ErpFinBudgetCommitmentBizModel` resolveOrgAndSchema/resolveCurrencyId 账套解析链，L153-176）的回归断言。
- **问题**：全测试树 grep `CHECK_LOCKS|串行锁|TOCTOU|CountDownLatch|ExecutorService` = 0——锁的并发语义无任何断言；TestErpFinBudgetCommitment/TestErpFinBudgetEndToEnd 种子恒用 `orgId="1"`/`acctSchemaId="1"` 身份值且无凭证 orgId/acctSchemaId/currencyId 输出断言——解析实现与旧硬编码 `"1"` 在测试矩阵下不可区分，两修复体回归将静默穿透。
- **三态裁决**：新立。r1 fin3-004/005 终态证据列均未登记修复测试（对比 001 明登 TestErpFinBudgetCarryForward 5/5）；r2 无同型；先例 = P2-CK-fin2-018-r3 / P2-CK-inv-012-r3（「已修复残留无回归承接 → 新立 `-r3` 承接」）。级别 **P3**（区别于 fin2-018-r3 的 P2：彼为整条 FX 业务路径全暗；本切片 check 结局路径/三量口径/结转规则均有行为断言，仅两防御机制缺区分度断言，且锁为 JVM 本地 deterministic 同步）。
- **修复建议**：归 M2.x 测试批——commit 用例加非身份 orgId 种子 + 断言生成凭证 orgId/acctSchemaId/functionalCurrencyId 三元组；check 并发子路径以双线程 latch 断言 HARD 控制下仅一笔通过（或登记 watch-only 裁决）。

### 2.4 归属标注（§3.2 共享代码边界）

- 本报告全部控制点属 fin-3 格（预算控制/成本方法/成本中心切片本体：控制引擎/承付 SPI/双生成器/facade/6 per-mutation Processor/双状态机 Bean/5 BizModel/classify 族）。
- posting 引擎消费侧（BudgetVoucherGenerator/CommitmentVoucherGenerator 直写 Voucher/VoucherLine/VoucherBillR 三表，不经引擎 Provider 通道）为生成器本体消费面，属本格；引擎内部归 fin-1（已闭合）。`P3-CK-fin-018` 承付红冲 dcDirection 站点 r1 归属 C3.1、fin-1 r3 本轮已追加证据，本格注记不重复计数（见 §2.2 末）。
- common 抽象族（`AbstractErpCrudBizModel` 状态锁基类/`Abstract{SubmitForApproval,Approve,Reject,Cancel}Processor` 骨架 + `illegalStatusException` 专属码改道）调用点已审合规；基类行为缺陷归 U20（M1.15）。
- 成本方法路由本体（CostMethodResolver/CostingStrategy 族/StockMoveBookkeeper）物理在 inventory 域（归 U02/M1.13 已闭合格）；本格仅核 finance 侧调用边界 `closeInvModule→recloseInvCosts`（DIM-B ⑮ 断言一致）。成本中心 GL 分摊（ErpFinGlDistributionValidator）r1 已于 C3.1 验证，本格不重复。
- `validate:flux` 全局面与 E2E runbook 全局合规抽样归 U21（M1.16）；本格仅按 §1.2 ③ 核 fin 涉及 spec（结果见矩阵 DIM-F 行）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 5（fin3-001..005） | 0 |
| P2 | 0 | 0 | 5（fin3-006..010） |
| P3 | 3（fin3-017/018/019-r3） | 0 | 6（fin3-011..016） |
| **合计** | **3** | **5** | **11** |

新立合计 3（全 P3，DIM-B×2 + DIM-T×1）；复用合计 5（全 P1）；归并合计 11（P2×5 + P3×6）+ 跨切片注记 1（P3-CK-fin-018，不计数）；历史 16 fin3 ID + 跨域家族 ID 零覆写。五格 verdict：DIM-B **finding** / DIM-F **pass** / DIM-S **pass** / DIM-T **finding** / DIM-I **pass**。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：fin-3 范围 22 生产文件全读（budget 包 5 + classify 包 3 + entity Budget* BizModel 5 + Scenario*Processor 6 + DocumentStateMachine；ApprovalStateMachine 结构复核）+ 控制引擎/承付 SPI/双生成器/结转/滚动编排逐行；机械程式全套实跑（checker 19 规则零漂移 / 反模式族全零 / IDaoProvider 逐文件注释核对 / codegen 安全 / E1 聚合路径 / validate:flux 双数字 / flux-only 双 grep / TestErpSeedDataIntegrity 4/4 + 预算 seed FK 与字典值域抽查 / fin 回归 533 全绿 ×1 + 快照纪律 / --strict + --self-test 双 PASS + 白名单 3 抽查 HEAD 复核）；15 维度无跳维；⑮ 2 doc × 5 断言；r1 fin3 族 16 条逐一比对裁决 + 跨切片 1 注记。
- **未深查（边界归属）**：成本方法路由 inventory 侧本体与 7 策略族（归 U02，r1/M1.13 已闭合）；成本中心 GL 分摊 ErpFinGlDistributionValidator（r1 C3.1 已验证，本格不重复）；`ErpFinApDocument` 摄取管道 Processor 全文（PipelineProcessor 仅白名单面复核，摄取管道行为归 E3.5/AP 自动化面，r1 未列 fin3 格）；erp-fin-web `_gen` 视图细节（codegen 面，M0.4 矩阵判定）；测试代码仅作覆盖对账与 F2.3 回归复核消费。
- **残留风险（登记不裁决）**：① 11 条归并 open finding 修复归 M2.x（finance 修复批），其中 fin3-008（costCenterId 恒 null 使成本中心维度预算控制不可达）与 fin3-006（approveStatus 字典外值）建议优先——两者均为 config 投产（budget-check/roll-forward enabled）即显性化的门控后缺陷；② 3 条新立 P3 的修复与 P3-CK-fin-018 同批 M2.x 收口，同族跨域站点（daoFor 豁免注释族）全仓扫描归 U20/M1.15；③ budget.md L257 §浏览器层验证段与 E2E spec 头注释的 stale 描述在 doc 维护批前将持续误导后来者对「结转凭证」存在性的判断（= fin3-018-r3 承接面）；④ `validate:flux` 325 条 variant 漂移与 compliance 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；`erp-fin.budget-check-enabled`/`budget-commitment-enabled`/`budget-roll-forward-enabled`/`budget-carry-forward-enabled` 任一 config 投产启用时，fin3-006/008/009 及 fin3-019-r3 应即升优先级先写失败测试。
