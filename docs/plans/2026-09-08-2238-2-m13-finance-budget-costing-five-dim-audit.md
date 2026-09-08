---
status: active
mission: ai-check-r3
work-item: M1.3
group: "2026-09-08-2238"
verify: [test]
---

# 2026-09-08-2238-2 M1.3 finance fin-3 五维符合性审计（预算与成本切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）；M1.1 done（plan `2026-09-08-1042-1`，closure audit ACCEPT 2026-09-08，`ck-finance-posting-r3.md` 五格矩阵落盘；roadmap 行 done 翻转归 owner/engine 机制，依赖满足以其闭包回执为准）。M1.3 与同批 M1.2（2238-1）互不依赖、均为独立只读审计；按 roadmap 文档序 fin-3 在 fin-2 之后。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U05 × 五维 × fin-3**（§4 映射表第 3 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- 切片范围（U05 fin-3，§3.2 切片行）：预算控制/成本方法/成本中心；owner doc `docs/design/finance/budget.md` + `docs/design/finance/costing-methods.md`（实仓核验在盘；成本中心判定面辅锚 `docs/design/finance/cost-center.md`）；物理面 `module-finance/erp-fin-{dao,service,web}` 的 `src/main` 中预算/成本族——`service/budget` 包（`ErpFinBudgetScenario/Line/Commitment/ControlLog/CarryForwardLog/RollforwardLog` BizModel 族 + BudgetVoucherGenerator/承付通道 + carryForward/rollForward 编排）+ `service/classify` 成本方法/成本中心分类族。
- DIM-B 焦点（§3.3 U05 行 fin-3 列）：预算三量口径/TOCTOU 锁/结转科目维度；§1.1 全套逐切片跑（processor-extension-pattern 额外对齐为 fin-1 专属增量，本切片不适用）。
- 共享代码唯一归属（冻结清单 §3.2）：posting processor 族本体（`ErpFinPostingService`/凭证引擎/dispatcher 族/`FinPostedListener`/sweep/`PostingRun`）唯一归属 fin-1（已审计闭合，0 新立）——本切片只审预算凭证生成/承付通道对凭证引擎的**消费侧调用点**（含 BudgetVoucherGenerator 等生成器本体）；common 抽象族行为缺陷归 U20，本切片只审调用点；聚合横切面归 U21；notify 派发子系统本体归 U11。
- 跨轮查重源（§2）：r1 `docs/audits/check/ai-check-index.md` 同域报告 `ck-finance-budget-costing.md`（C3.3：0 P0 / 5 P1 / 5 P2 / 6 P3，done；fin3 族 P1-CK-fin3-001..005 fixed / P2-CK-fin3-006..010 open / P3-CK-fin3-011..016 open——全表以 `ai-check-index.md` §Finding 追踪为准）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（已裁决偏离不重复报告）+ fin-1 r3 报告 `ck-finance-posting-r3.md` §2 归属标注（消费侧涉 fin-3 的归并指针）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：fin 探针族（CAT-1 54 / CAT-2 20）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块与 actual 的批前在案差距归 successor `ai-check-r3-compliance-baseline-raise`，非本切片范围。
- 仓库现状（2026-09-08 实核）：HEAD `dc31a2555`；`git status --porcelain` 干净。审计证据以实跑时 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：U05 × 五维 × fin-3 五格 verdict 未落盘；本轮尚无 `ck-finance-budget-costing-r3.md` 报告。

## Goals

- 按冻结清单对 U05 × 五维 × fin-3 五格全跑（禁止抽样、禁止跳维），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-budget-costing-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 fin-1/fin-2/fin-4 格（M1.1/M1.2/M1.4）与其他单元格；posting 引擎内部缺陷归 fin-1（已闭合），本切片发现涉引擎内部时标注「归属 fin-1」归并不重复立项。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 在位
      - Skill: none
      - 证据（2026-09-09 实跑）：目录已存在，`ai-check-r3-index.md` + `m0-5-audit-checklists.md` 在位（同目录另有 M1.1/M1.2/M1.5/M1.8/M1.10/M1.12/M1.13 姊妹 r3 报告，与索引一致）
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露（同批 2238-1/2238-3 为独立只读审计计划，无并发写面；MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点
      - Skill: none
      - 证据：审计时点 T0 = HEAD `b21e0f74d12ca018b2f8a66d72f5dab861f9cfc6`（较计划起草时 `dc31a2555` 前进，姊妹切片正常落库所致；按计划 Current Baseline 口径「审计证据以实跑时 HEAD + 脏面披露为准」）；`git status --porcelain` = 2 行，均为 untracked 计划文件（本计划 `2026-09-08-2238-2-*.md` + 姊妹 `2026-09-08-2238-3-*.md`），零生产路径脏面（脏树实跑 + 披露，MI.9 收官审计先例）；姊妹在制会话：2238-1（M1.2，报告已落盘 `ck-finance-arap-r3.md`）/ 2238-3（M1.4，独立只读审计计划）无并发写面
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据：compliance checker 19 规则逐项 = M0.3 快照行**零漂移**（R1a/b/c=0、R1d=14、R2a=34、R2b=242、R2c=1542、R2d=38、R3=5、R4=0、R5=0、R6=2、R7=0、R8=0、R10=14、R11=0、R12a=71、R12b=66、R12c=42）；CJK report mode **CAT-1..4 = 0/0/0/0**（= MI 终态行，CAT-5 21158 行豁免仅统计）、exit 0。零漂移零登记项

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.3 行指定）
> Targets: `module-finance/erp-fin-dao|erp-fin-service/src/main/java`（fin-3 范围 = `service/budget`、`service/classify` 包 + 预算/承付/结转/滚动 BizModel 与生成器族文件）；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（§6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0=`b21e0f74`）：checker 19 规则 = M0.3 快照行零漂移（Phase 1 同跑）；fin-3 面（budget/classify 包 + ErpFinBudget* BizModel 5 + Scenario*Processor 6 + DocumentStateMachine）反模式族全零（extends RuntimeException=0 / @Inject private=0 / System.currentTimeMillis=0 / @Transactional=0——R6=2 两处豁免注释在位归 fin-1 VoucherBizModel 基线 / LocalDate.now 族=0）；IDaoProvider 命中 9 文件——豁免注释在位 2（ErpFinApDocRuleClassifier javadoc「IDaoProvider 直访对齐 ApsLoadSourceProvider 范式」+ ErpFinBudgetScenarioCarryForwardProcessor L132-133「无对应 IBiz 覆盖此跨期间查询语义」），**7 文件命中处无注释理由**（→ 新立 P3-CK-fin3-017-r3，见报告 §2.3）；codegen：`__XGEN_FORCE_OVERRIDE__` 命中全为 erp-fin-meta dict.yaml 只读校验点（src 面零手改，module-finance git 脏面 0）；聚合完整性：E1 勘误路径 `app.action-auth.xml` x:extends 含 `erp-fin.action-auth.xml` ✓，budget 四页族（scenario/control-log/rollforward-log/carry-forward-log）已注册 useCases=UC-FIN-13 ✓，budget 族 bean 全注册 `app-service.beans.xml` ✓
- [x] <Proof> 15 维度逐维走查 fin-3 范围（程序式确定性走查落 verdict；重点：①Model→Delta→Java ②跨实体 I*Biz ③NopException ⑧状态机（预算方案 approveStatus 值域/字典合规——r1 P2-CK-fin3-006 open 站点复核）⑨审批流/作业（预算方案审批链）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：15/15 无跳维（⑫指针 DIM-T）。①model 驱动 dict/config + Delta beans.xml 覆盖点声明 ✓ ②新立 P3-CK-fin3-017-r3（7 文件 daoFor 直查无豁免注释，含 5 处跨域 ErpMdSubject/ErpMdAcctSchema 直查、IErpMdSubjectBiz 在位未注入）③全 NopException+ErpFinErrors ✓ ④@Inject 非 private 零违例、零冗余 @Transactional ✓ ⑤CoreMetrics/StringHelper ✓ ⑥AbstractErpCrudBizModel+Facade/Processor 范式 ✓ ⑦零新增跨模块写、R12 基线内 ⑧双轴 Bean（Document 6 值 budget-status 字典全合规 + Approval 4 值 wf/approve-status）；**r1 P2-CK-fin3-006 站点复核：RollForwardProcessor L119 仍写 approveStatus="DRAFT" 字典外值**（wf/approve-status 实仓 dict dump = UNSUBMITTED/SUBMITTED/APPROVED/REJECTED 四值）→ 归并 ⑨submit→approve/reject→cancel 四 Processor + AbstractX 基类专属码改道（plan 2026-09-07-2200-1 form-3）在位 ✓（carries: carryForward 自守卫 + rollForward spawn-new metadata 边，状态机 Bean javadoc 显式 justified runtime-dead 登记在案）⑩protected step + Delta 覆盖 ✓ ⑪orgId 写入面 F2.3 后合规（聚合读面 orgId 缺口归并 fin3-012）⑬⑭见上 ⑮见下行。verdict = **finding**（0 blocker / 0 major / 2 minor 新立 + 11 归并 + 5 复用 + 1 跨切片注记）
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`docs/design/finance/budget.md` + `costing-methods.md` 中 ≥2 doc × 2 关键断言（三量口径/控制级别语义/结转维度/成本方法路由）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
      - 证据：抽样 2 doc × 5 断言——budget.md §L16/18/60 三量口径 `available = budgetBalance − actualBalance − commitmentBalance`（承付款不计入 actual 通道）↔ ControlBiz L93-96+applyPostingTypeFilter L177-179 **一致**；budget.md §L93/81 控制级别语义（HARD→throw NopException 阻断 / WARN→写日志放行 / NONE→PASS，dict NONE/WARN/HARD=常量 L415-417）↔ doCheck L102-118 **一致**；budget.md §L215-227/253 结转维度（REMAINING_FULL 三量 + 按 subjectId×costCenterId 增补 + commitment 不结转）↔ CarryForwardProcessor L65-67/L283-306 **一致**（F2.3 修复注记 L279-281 在位）；budget.md §L257 E2E 段「结转凭证 code = CARRY-FORWARD-…precision 50 约束」↔ writeCarryForwardVoucher F2.3 后为 no-op（L309-318）+ 全仓零 CARRY-FORWARD- 写点（仅测试注释明示「不再伪造前缀」）→ **漂移 1 处**（stale 描述，→ 新立 P3-CK-fin3-018-r3）；costing-methods.md §L38 期末成本兜底 finance 调用边界（closeInvModule→recloseInvCosts 经 IBizObjectManager finance→inventory R + config `erp-fin.inv-costing-reclose-on-close` + impl 未就绪 try/catch 告警跳过）↔ ErpFinAccountingPeriodProcessor L148/210-221 + ErpFinConstants:128 **一致**。漂移 1 处 < 2，未触发扩样
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-finance-budget-costing.md` §Finding 追踪 + r2 目录 + §Mission 基线快照 + fin-1 r3 报告归属标注）：同型 fixed=复用（复核 HEAD 仍有效）/同型 open=归并原 ID/确属新发=新立 `P{n}-CK-fin3-{NNN}-r3`；裁决证据落勾选注记（报告落 Phase 7）
      - Skill: code-quality-audit-prompt
      - 证据：三态裁决完成（详表落 `ck-finance-budget-costing-r3.md` §2）：复用 5（P1-CK-fin3-001..005，F2.3 修复 T0 复核全部在位）/ 归并 11（fin3-006/007/008/009/010/011/012/013/014/015/016 现症逐站点复核在位 + fin3-009 扩员承付科目静默 fail-open 新站点）+ 跨切片注记 1（P3-CK-fin-018 承付红冲 dcDirection——r1 登记归属 C3.1，fin-1 r3 本轮已追加同站点证据，本格不重复计数）/ 新立 2（P3-CK-fin3-017-r3 daoFor 无注释理由族 + P3-CK-fin3-018-r3 budget.md L257 stale）；历史 ID 零覆写；r2 目录无 fin3 同型独立登记；§Mission 基线快照确认 R2b/R12 等命中为已裁决偏离

Exit Criteria:

- [x] U05×B×fin-3 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成（复用/归并/新立逐条在案）

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: `module-finance/erp-fin-web/src/main/resources/_vfs`（fin-3 面：预算方案/预算行/承付/预算日志/成本方法与成本中心页）；E2E 涉 fin spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案，非本切片 finding）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）
      - Skill: none
      - 证据：`npm run validate:flux`（完整输出存 /var/folders/.../opencode/flux-validate-m13.log）step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`（= 基线）；整体 exit 1 余项 325 条 ERR **全部** `variant=primary` dropdown-button 既有 stub 外部漂移族（fin 域 29 条同族、非 variant ERR = 0，successor 在案，非本切片 finding——与 fin-1/fin-2 r3 报告同口径）；flux-only：`component="AMIS"` 保留层 0、`grep -L ext:web-renderer="flux"` 全部 ORM = 空
- [x] <Proof> fin-3 页面走查：预算方案/预算行/承付/预算日志/成本方法与成本中心页对照 view-and-page-strategy（REST `/r/` 数据访问 / codegen vs 手写边界查 M0.4 矩阵 / 定制走 `x:extends` / i18n-en 承载约定）逐页落 verdict；涉 fin E2E spec（预算管控族）时核对 PageObject 模式 + `E2E_ENGINE` flux 缺省 + 禁 GraphQL 断言（runbook L122 非页面路径 API 断言豁免通道除外）
      - Skill: none
      - 证据：fin-3 物理面 5 页族逐页走查（BudgetScenario/Line/ControlLog/RollforwardLog/CarryForwardLog）——保留层 view.xml 全 `x:extends="_gen/_X.view.xml"`（bounded-merge 定制）+ main.page.yaml/picker.page.yaml 纯 codegen 面（`x:gen-extends` GenPage 模板，M0.4 矩阵分类一致）；BudgetScenario 业务动作 6 mutation（submit/approve/reject/cancel/rollForward/carryForward）+ 2 query 全 REST `@mutation:/@query:` 零 GraphQL 数据访问（view.xml 中 `graphql:labelProp` 命中为平台元数据键非 API 调用）；i18n-en 承载齐备（34/14/11/1/1，模型源约定）；承付无独立页（SPI 生成凭证显于凭证页 fin-1 面）；成本中心工作台页（辅锚面，物理在 md-web，M0.4 L80 行）手写 `@query:ErpMdCostCenter__findPage` REST + i18nEn 12 处合规；成本方法无独立页（material 字段 + dict，路由在 inventory 策略族）；E2E：`fin-budget-rollforward-carryforward.action.spec.ts` 零 `data-testid/data-slot/.cxd-` 命中（PageObject/helper 模式）+ `E2E_ENGINE` 缺省 flux 实证（engine.ts L8-11 `return 'flux'`）+ GraphQL `/graphql` 调用为非页面路径 setup/action/cleanup 通道（runbook「API 断言」节 L122 豁免，fin-1/fin-2 同判例）。verdict = **pass**

Exit Criteria:

- [x] U05×F×fin-3 格 verdict 落盘；全局面门禁数字（导出 error 数 / flux-only 命中数）在案对账一致
- [x] fin-3 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ fin-3 相关 seed（预算方案/预算行/科目维度面）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - 证据：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` → **Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（2026-09-09T01:09 实跑）
- [x] <Proof> fin-3 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空（只读审计零 seed 变更）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表 + 预算 seed 一致性抽查（budget scenario/line 科目与期间维度 FK 自洽、控制级别字典值域合规）
      - Skill: none
      - 证据：`git status --porcelain _init-data/` 空（零 seed 变更）✓；资产清点 **372 CSV + 1 SQL**（= 冻结口径 M1.5 批次后现值）✓；deploy `_seed_*.sql` 命中 cs/notify 两族（各三方言）——均在 `docs/architecture/seed-data.md §97` 登记处表显式聚合裁决（L107 notify 行 ✅ 已聚合 + cs 同批），fin 无 deploy seed 无第三态 ✓；预算 seed 一致性抽查：scenario 2 行 controlLevel=HARD/WARN ∈ `erp-fin/budget-control-level` 字典、docStatus=APPROVED/CLOSED ∈ `erp-fin/budget-status`、approveStatus=APPROVED ∈ `wf/approve-status`（**无字典外 DRAFT**，与 P2-CK-fin3-006 代码面缺陷互证 seed 面干净）；budget_line 2 行 scenarioId/subjectId/periodId FK 零悬空；control_log 2 行 actionResult=PASS/WARNED ∈ owner doc L74 值域 + scenarioId/budgetLineId FK 零悬空；rollforward/carryforward log 各 1 行在册 ✓

Exit Criteria:

- [x] U05×S×fin-3 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 预算 seed 一致性抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-finance/erp-fin-service`（`<SVC>` = 本模块，同冻结清单 §1.4 记法）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-finance/erp-fin-service` 全绿零失败（数字落注记，对照 known-good-baselines fin 533 计数）
      - Skill: none
      - 证据：**Tests run: 533, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（= known-good-baselines fin 533 计数零回归，2026-09-09 实跑）
- [x] <Proof> 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 测试目录清单逐项对账（fin-3 范围 = BudgetScenario/Line/Commitment/ControlLog/CarryForwardLog/RollforwardLog 及成本分类族 BizModel）；关键业务流清单核对——预算控制 check-then-act、承付/释放对称、期末结转/年度结转（testing-strategy P1 清单行 + `budget.md` 用例段；r1 P1-CK-fin3-001..005 fixed 修复的回归测试在位复核）逐行核覆盖；缺口按业务关键度定级
      - Skill: none
      - 证据：公开方法 × 测试对账——Scenario 6 mutation（submit/approve/reject/cancel→TestErpFinBudgetScenarioStateMachines + EndToEnd；rollForward→TestErpFinBudgetRollForward 3 策略；carryForward→TestErpFinBudgetCarryForward 4 规则+年度关账前置）✓；Line getBudgetVsActual→EndToEnd testGetBudgetVsActual ✓；Commitment SPI 3 方法→TestErpFinBudgetCommitment 6 用例（commit/release-on-cancel/release-on-invoice-approve/double-release 守卫/sales billType 派发/多发票单红冲）+ PropertyErpFinBudgetCommitmentRelease 2 seeded property（对称不变量）✓；3 Log BizModel 裸 CRUD 零自定义 ✓；classify→TestErpFinApDocRuleClassifierDeterminism + `_cases/classify/` ✓；`_cases` 14 目录无孤儿。关键业务流：check-then-act 结局路径（HARD blocked/WARN logged+pass/NONE pass + 三量 500=1000−300−200 精确断言 + RESERVATION 计 actual 回归）全覆盖 ✓；承付/释放对称 ✓；期末结转/年度结转 P1 清单行（PeriodCloseEndToEnd/ProfitLossClosing/AnnualClose）在 533 内域级覆盖 ✓（归 fin-4 格细审）；F2.3 修复回归复核——001（CarryForward 5 用例 + L326 F2.3 注记）/002/003（testAvailableDeductsCommitmentSeparately 精确断言）在位 ✓，**004 串行锁与 005 org/账套/币种解析零专属回归断言**（测试恒种子 orgId=1/acctSchemaId=1 身份值无法区分新旧实现；无 latch/executor 并发断言）→ **新立 P3-CK-fin3-019-r3**（inv-012-r3/fin2-018-r3「已修复残留无回归承接」先例；行为结局路径已覆盖故 P3 非 P2）
- [x] <Proof> 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` expect 0（提交态零残留）+ `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - 证据：`SnapshotTest.RECORDING` = **0** 残留 ✓；`delVersion` 命中仅 TestErpFinVoucherTemplateAuditLog（审计行为断言用途合规）；533 全绿 = CHECKING 态等价证明（快照屏蔽合规）

Exit Criteria:

- [x] U05×T×fin-3 格 verdict 落盘；本地回归全绿数字在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + fin 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据：`--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4 = 0/0/209/1318，单向收紧成立）；`--self-test` **PASS**（self-test green）；fin 探针族（CAT-1 54 / CAT-2 20）维持清零零回归（report mode 全局 CAT1/2 = 0）
- [x] <Proof> 白名单合规抽查：`docs/audits/cjk-baseline.md` §WHITELIST fin 相关条目抽 ≥3 条核对四要素（文件路径/理由/owner doc 指针/裁决来源）+ `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空（`_` 前缀生成 i18n yaml 禁手改）；四要素缺失 = 白名单登记缺陷 finding（非同类 CJK finding）
      - Skill: none
      - 证据：§WHITELIST「批 1/2 finance」5 条抽 3（含 2 条 fin-3 面文件）——①`ErpFinApDocRuleClassifier`（fin-3 classify 面本体）：四要素齐备（C2 窄类：3 行 excerpt.contains 发票类型字面量为行为本体）+ HEAD 复核 L91/94/97 三行逐字在位（登记准确）；②`IErpFinApDocumentBiz`：C1 @Description 豁免四要素齐备 + HEAD L22 在位；③`ErpFinApDocumentPipelineProcessor`：C2 OCR P_* 解析 Pattern 豁免四要素齐备 + HEAD 12 命中在位；`grep -L @Locale` *Errors.java = 空（全标）；`module-*/erp-*-meta/**` + `_vfs/i18n/**` 零手改（git 脏面空）

Exit Criteria:

- [x] U05×I×fin-3 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-budget-costing-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：逐条确认三态（复用/归并/新立）与级别（P0~P3）一致性与 ID 规范（`P{n}-CK-fin3-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - 证据：复裁决一致——复用 5（全 P1，fin3-001..005，F2.3 修复体 HEAD 逐一复核在位）/ 归并 11（P2×5 + P3×6，fin3-006..016 现症逐站点复核 + fin3-009 承付科目静默 fail-open 新站点扩员追加至原 ID 证据）+ 跨切片注记 1（P3-CK-fin-018 归属 fin-1 不重复计数）/ 新立 3（全 P3：fin3-017-r3 DIM-B② daoFor 豁免注释族、fin3-018-r3 DIM-B⑮ doc stale、fin3-019-r3 DIM-T 修复体回归承接——级别定级均记录与同型先例的对齐理由：ast-028-r3/qa-027-r3、pur-016-r3/prj-025-r3、fin2-018-r3/inv-012-r3）；ID 规范 `P3-CK-fin3-{017,018,019}-r3` 连号无冲突；历史 16 fin3 ID + 全部跨域 ID 零覆写
- [x] <Add> 落盘 `ck-finance-budget-costing-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计（P0~P3 计数）+ 剩余风险声明（横切关注点 13 完整四件套）
      - Skill: none
      - 证据：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-budget-costing-r3.md` 落盘——§1 覆盖矩阵 **5/5**（B=finding / F=pass / S=pass / T=finding / I=pass）+ §2 三态裁决（2.1 复用 5 表 / 2.2 归并 11 表 + 跨切片注记 / 2.3 新立 3 详条 / 2.4 归属标注）+ §3 统计表（新立 3 全 P3 / 复用 5 全 P1 / 归并 11）+ §4 剩余风险四件套（已查/未深查边界归属/残留风险登记/successor 触发条件）；含切片标签注记（classify 物理面 = AP 文档分类族）与 HEAD/脏面披露
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加本报告行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.3 行 + §Finding 追踪追加新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - 证据：本轮 `ai-check-r3-index.md` 产物清单追加 M1.3 行（ck-finance-budget-costing-r3.md 全景摘要）✓；跨轮 `ai-check-index.md` §报告清单追加 M1.3 行（0/0/0/3 done）✓ + §Finding 追踪表追加 3 行（P3-CK-fin3-017/018/019-r3，插于 fin3 r1 族后保持域分组）✓ + 末尾追加 M1.3 r3 复核注记 blockquote（对齐 M1.2 先例——归并证据落报告 §2.2，原 ID 行零改动零覆写）✓
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6；触碰面 = 执行目录报告 + 索引 + 本计划勾选注记）
      - Skill: none
      - 证据：`git status --porcelain` 过滤 `module-*|app-erp-all`（排除 docs/plans）= **空** ✓；全量脏面 = `M ai-check-r3-index.md` + `M ai-check-index.md` + `?? ck-finance-budget-costing-r3.md` + 2 个 untracked plan 文件——全部为执行目录报告/索引/计划文档面，零生产路径
- [x] <Proof> 收尾回归：`mvn test -pl module-finance/erp-fin-service` 复跑全绿（审计只读不变式复证；全仓验证归 closure 审计机制）
      - Skill: none
      - 证据：**Tests run: 533, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS**（第二次复跑，与 Phase 5 首跑及 known-good-baselines fin 533 计数一致——审计只读不变式复证成立）

Exit Criteria:

- [x] `ck-finance-budget-costing-r3.md` 落盘且五维矩阵 5 格 verdict 完整（缺一格不算完）
- [x] 双索引行追加在案；零生产代码改动核证通过；fin service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-08-193051-mission-driver-2026-09-08-2238-2-m13-finance-budget-costing-five-dim-audit-1-6b1b809f to 2026-09-08-193051-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-08-193051-mission-driver-2026-09-08-2238-2-m13-finance-budget-costing-five-dim-audit-1-6b1b809f（补 Closure Gates——按 M1.1 同批先例为本只读审计计划定制门控；frontmatter `status: draft` → `active`；基线引用已实仓复验——HEAD `dc31a2555`/冻结清单 §1.1·§1.5·§3.2·§3.3·§4·§6-E1/三项技能名/`TestErpSeedDataIntegrity`/MI 终态行与 M0.3 快照行数字/r1 fin3 finding 状态/325+999 flux 数字/fin 533 锚点（M1.1 闭包回执同命令 `erp-fin-service` 533/0/0/0 实证）逐一在盘）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-finance-budget-costing-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-finance/erp-fin-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 fin-1/U20/U21/U11」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-08.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-finance-budget-costing-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- pass test 20260909-0115-exec-r1 exit=0
- pass test 20260909-0133-closure-r1 exit=0

> 闭包 visit 记录（2026-09-09 01:33，独立闭包审计，fresh session）：`mvn test -pl module-finance/erp-fin-service` BUILD SUCCESS（exit 0，Tests run: 533, Failures: 0, Errors: 0, Skipped: 0 = known-good-baselines fin 533 锚点零回归）。增量口径：闭包时点工作树改动全为 docs 审计产物面（`git status` 过滤 `module-*`/`app-erp-all` 生产路径 = 0 行），按 mission 增量构建指引以 `-pl` 定面本切片 service 模块（本计划唯一涉码验证面，Phase 5/7 同锚点），不清 target 全量重编；完整仓库回归归 roadmap 收官机制（只读审计计划：验证命令组即结果表面本身，MV.1 对照面 = known-good-baselines 2026-09-08 MI 终态行）。

> 执行期记录（2026-09-08 晚 ~ 2026-09-09，执行会话；T0 = HEAD `b21e0f74` 实跑，各 Phase 勾选注记含同源数字）：本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行）；`## Closure` 留予独立结束审计（新会话非执行者上下文）落回执。增量口径：执行时点工作树改动全为 docs 审计产物面（`git status` 过滤 `module-*`/`app-erp-all` 生产路径 = 0 行）。

- PASS 2026-09-09（执行期 Phase 1/2）`bash docs/audits/nop-compliance-checker.sh` — 19 规则 = M0.3 快照行逐值一致零漂移（R1a/b/c=0/0/0、R1d=14、R2a=34、R2b=242、R2c=1542、R2d=38、R3=5、R6=2、R10=14、R12a=71、R12b=66、R12c=42，其余 0）
- PASS 2026-09-09（执行期 Phase 1/6）`node tools/check-hardcoded-cjk.mjs`（report mode）— CAT1..4 = 0/0/0/0（CAT-5 注释统计 21158 行）= MI 终态行精确一致
- PASS 2026-09-09（执行期 Phase 3）`npm run validate:flux` — step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；整体 exit 1 余项 = 325 条既有 `variant=primary` dropdown-button stub 外部漂移族（fin 域 29 条同族、非 variant ERR = 0，successor 在案，非本切片 finding）；flux-only：`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0
- PASS 2026-09-09（执行期 Phase 4）`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` — Tests run: 4, Failures: 0, Errors: 0, Skipped: 0，BUILD SUCCESS
- PASS 2026-09-09（执行期 Phase 5/7）`mvn test -pl module-finance/erp-fin-service`（×2）— 533/0/0/0 全绿（= known-good-baselines fin 533 锚点零回归）
- PASS 2026-09-09（执行期 Phase 6）`node tools/check-hardcoded-cjk.mjs --strict` — PASS exit 0（`0 new violations vs frozen snapshot; 170 baseline files, totals CAT1..4=0/0/209/1318`，单向收紧成立）+ `--self-test` — PASS
- PASS 2026-09-09（执行期 Phase 7）`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 — 0 行（零生产代码改动，roadmap 规则 6；脏面 = 执行目录报告 + 双索引 + 本计划 + 姊妹计划文件，全为 docs 审计产物面）

## Closure

Status Note: 本计划（只读五维审计切片 U05 × 五维 × fin-3，零生产代码改动）7 Phase 全部执行项与退出标准 `[x]`（35/35，机械红线数字注记在案）；五格 verdict 全落盘（B=finding（2 新立 P3）/ F=pass / S=pass / T=finding（1 新立 P3）/ I=pass）；三态裁决 复用 5 / 归并 11 + 跨切片注记 1 / 新立 3（`P3-CK-fin3-017/018/019-r3`，历史 16 fin3 ID 零覆写）+ `ck-finance-budget-costing-r3.md`（矩阵 5/5 + 统计 + 剩余风险四件套）+ 双索引行 + 当日日志条目均在案。ledger 协议：frontmatter `status: active` 保持，完成态由全勾选 + `## Verification` pass 线 + 本节回执派生；roadmap M1.3 行 done 翻转归 owner/engine 机制（M1.1/M1.2/M1.5/M1.8 同批先例），不在本闭包翻转。

Closure Audit Evidence:

- Auditor / Agent: 独立闭包审计子代理（fresh session、read-only、非执行者上下文；mission-driver 流 CLOSURE_SCRIPT_CHECK → 闭包 visit 单一独立 closer）
- Evidence: 本计划 Phase 1~7 勾选注记（含全部红线数字）+ `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-budget-costing-r3.md`（五维矩阵 5/5 + 三态裁决 + 统计 + 剩余风险四件套）+ 双索引追加行（本轮 `ai-check-r3-index.md` 产物清单 1 行 + 跨轮 `ai-check-index.md` §报告清单 M1.3 行、§Finding 追踪 `P3-CK-fin3-017/018/019-r3` 3 行、fin3 族 r3 轮复核注记）+ `docs/backlog/ai-check-r3-roadmap.md` M1.3 行执行完成注记 + `docs/logs/2026/09-08.md` M1.3 条目 + 闭包 visit 实跑记录（见 `## Verification`）

- dispatch audit #audit-20260909-0133-m13-finance-budget-costing-five-dim-audit-1-50b4afce to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260909-0133-m13-finance-budget-costing-five-dim-audit-1-50b4afce：独立闭包审计 ACCEPT——35/35 勾选全绿，五维矩阵 5/5 格 verdict 落盘（B=finding：复用 5（fin3-001..005 F2.3 修复 HEAD 复核有效）+ 归并 11（fin3-006..016 现症复核 + fin3-009 承付科目静默 fail-open 新站点扩员）+ 新立 2 P3（fin3-017-r3 预算族 daoFor 无豁免注释族 / fin3-018-r3 budget.md L257 结转凭证 stale）；F=pass（5 页族逐页 + E2E_ENGINE 缺省 flux 实证）；S=pass（TestErpSeedDataIntegrity 4/4 + 预算 seed FK/字典值域抽查干净）；T=finding（新立 P3-CK-fin3-019-r3 F2.3 修复体零专属回归断言；533 全绿零回归）；I=pass（--strict/--self-test 双 PASS + 白名单 3 抽查四要素齐备））；三态裁决 复用 5/归并 11+跨切片注记 1/新立 3，历史 16 fin3 ID 零覆写，双索引行与报告 §2/§3 逐条吻合（新立计数 0|0|3|0）；闭包 visit（2026-09-09 01:33）实跑 `mvn test -pl module-finance/erp-fin-service` 全绿（exit 0，533/0/0/0 = 锚点零回归）+ `git status` 生产路径零触碰核证（只读审计零改动红线保持）+ 实仓抽核（RollForwardProcessor L119 approveStatus DRAFT 现症与 fin3-006 归并一致、budget.md L257 CARRY-FORWARD stale 文句与 fin3-018-r3 一致、budget/classify 包 daoFor 命中在位与新立 fin3-017-r3 一致）；语义核对（退出标准对照实仓产物 / anti-hollow 报告与双索引实存非桩 / deferred honesty 新立 3 ID 登记 open + M2.x 修复方向 / 文档四方一致：报告↔双索引↔roadmap 行↔日志条目）全 PASS；plan-check `--strict` 结构绿，derivedCompleted 由本回执 + `pass test` 线成立（单模型降级如实声明：exec = aud = zhipuai-coding-plan/glm-5.3-flash）
