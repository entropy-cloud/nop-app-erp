# 2026-09-01-1245-1 M1.2a1 核心业务域 seed 扩面 A1——finance（26 规格表 CSV + 5 运行时补充 CSV + 过账凭证扩展）

> Plan Status: completed
> Last Reviewed: 2026-09-01
> Source: `docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md` 工作项 M1.2a1（ready，Deps M0.1 + M0.2 均 done）
> Related: `docs/plans/2026-09-01-0301-1-m01-seed-scope-adjudication.md`（M0.1，规格表权威源）、`docs/plans/2026-09-01-0527-1-m02-seed-gate-hardening.md`（M0.2，门禁常量随批更新协议 + 运行时口径登记）、`docs/design/finance/posting.md`（财务过账 owner doc）、`docs/architecture/seed-data.md`「交易单据种子」段、同批 `2026-09-01-1245-2-m12a2-manufacturing-seed-expansion.md` / `2026-09-01-1245-3-m12b-mnt-qa-prj-seed-expansion.md`
> Audit: required

## Current Baseline

- 实仓 363 个 `app.erp.*` 实体（className 口径），133 个有 seed（`app-erp-all/src/main/resources/_vfs/_init-data/` 实测 137 CSV = 133 app.erp + 4 平台，+ 1 SQL `zz-sequence-advance.sql`）；`TestErpSeedDataIntegrity` 基线常量 `EXPECTED_APP_ERP_CSV_COUNT = 133` / `EXPECTED_PLATFORM_CSV_COUNT = 4` / `EXPECTED_APP_ERP_ENTITY_COUNT = 363`（+ finance 5 缺 className 补充集）。
- **运行时口径补充档**（M0.2 落地，seed-data.md 对账表权威登记）：运行时实体集 368 = 363 + 5——finance 域 5 个声明缺 className 实体（`ErpFinCashForecast` / `ErpFinCreditFacility` / `ErpFinNotesDiscount` / `ErpFinNotesPayable` / `ErpFinNotesReceivable`）实体定义完整、无 seed、不在 270 规格表内；对账表明确「finance 仍缺 26+5=31，待 M1.2a1」。本计划按运行时口径消费：**31 CSV = 26 规格表 + 5 运行时补充**。
- finance 已 seed 7 表：`erp_fin_accounting_period` / `erp_fin_accounting_period_status` / `erp_fin_ar_ap_item` / `erp_fin_gl_balance` / `erp_fin_voucher` / `erp_fin_voucher_line` / `erp_fin_voucher_bill_r`。既有凭证种子实测行数：voucher **4** / voucher_line **8** / voucher_bill_r **4**（合计 16 行；roadmap M1.2a1 行「既有 8 行」为措辞漂移，以实仓为准，roadmap 本体勘误不在本计划范围）。
- 当前验证基线：`mvn test -pl app-erp-all` **71/0/0/1** 全绿（M0.2 plan `2026-09-01-0527-1` 实测，经 M1.1a/M1.1b/M1.1c 三批维持；`known-good-baselines.md` 最新登记行 69/0/0/1 系 2026-08-31 plan-1426-2 口径，登记行滞后于 M0.2 后实测，基线追行归 M3.1）；compliance 门控锚点 = `docs/audits/compliance-baseline.md` §BASELINE 机器块（R2c: 1537），**R2c=1542** 为历史实测值（+5 pre-existing 增量已登记，沿 plan `2026-09-01-0527-1` 口径协议）；全 reactor 已知 2 处预存回归（hr `TestErpHrDepartmentPositionDeleteGuard` + drp `TestErpDrpCrossDock`）为 roadmap Non-Goal，与本计划无关。
- 既有 CSV 约定：列头为 DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000（`zz-sequence-advance.sql` 将序列推进至 100000）；UoM 外键列名为 `UO_M_ID`（分叉拼写陷阱）；装载拓扑序由 `DataInitInitializer` 自动排序。
- **冻结时钟纪律**（`docs/bugs/2026-09-01-0017` / `2026-09-01-0058` 家族教训）：本批全部 seed 行日期/期间列使用静态固定值（对齐既有 fin 参考期），禁止 `now()`/滚动期间语义。
- **干扰面差异 vs M1.1 批**（本计划核心风险）：M1.1a/b/c 新增表不被集成用例/看板读取故零漂移；finance 相反——C03 O2C 黄金路径产出凭证（快照面含 `erp_fin_voucher*`）、C13 期末预检扫描 fin 表、finance 看板/报表/GL-mapping-rule 视觉 spec 读 fin 表。新增 26 表 + 凭证扩展行**可能**触发面 2 集成快照与视觉双面漂移，须预分析 + 按协议重录。
- 剩余差距：finance 26 规格表实体 + 5 运行时补充实体无 seed；`docs/design/finance/` 无「种子数据」owner doc 段；`erp_fin_reconciliation(+line)` 为 seed-data.md L154 登记的 Deferred（核销单文档），本计划按 roadmap 全量覆盖口径消费。

## Goals

- 31 个 seed CSV 落地（26 个按 `docs/architecture/seed-data.md`「270 个缺 seed 实体最小可用数据集规格表」finance 节逐行消费 + 5 个运行时补充实体按 ORM 推导），每实体最小可用数据集（行数 ≤ 20，按规格表建议行数与用例指示编码 P / N-TERM / N-DIS），FK 引用零悬空。
- 过账凭证扩展：`erp_fin_voucher` / `erp_fin_voucher_line` / `erp_fin_voucher_bill_r` 由 16 行（4/8/4）扩展至 ~30 行覆盖 posting path，遵循 seed-data.md「posted 一致性裁决」（凭证借贷平衡 + `voucher_bill_r` 反查 + 与 `ar_ap_item`/`gl_balance` 一致性裁决见 Phase 1 Decision）。
- `TestErpSeedDataIntegrity` CSV 基线常量按随批更新协议推进（133 → 164），seed-data.md 对账表同步（有 seed 164 / 精确缺 204 / 运行时缺 204 收敛 + 补充档 5 实体转入有 seed + 快照重录资产 168 CSV + 1 SQL）。
- `docs/design/finance/seed-data.md`「种子数据」owner doc 段落地。

## Non-Goals

- 不修改任何 ORM 模型（`module-*/model/*.orm.xml`，保护区 auto + dual-agent-approval）——本计划纯 CSV-only 路径；5 个缺 className 运行时实体定义完整，补 CSV 即可，无需 ORM 变更。
- 不修改任何生产 Java 代码（唯一 Java 触点 = `TestErpSeedDataIntegrity.java` 的 CSV 基线常量按内置协议更新）；不改过账 BizModel/Processor/编排逻辑（凭证为被动数据，action 驱动过账语义不变）。
- 不覆盖 manufacturing / maintenance / quality / projects 及其余 M1.x 工作项的缺 seed 实体（归同批 N=2/N=3 与后续批次）。
- 不新增 M2.x 像素断言、不做负向视觉断言、不做跨浏览器矩阵（roadmap Non-Goal）。
- 不处置 hr/drp 2 处预存回归；不引入 production-grade 真实个人数据。
- 不做「seed 字段值与 owner doc 业务规则一致性」的逐字段回放测试（roadmap Non-Goal；门禁覆盖存在性 + 行数 + 引用完整性）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/architecture/seed-data.md`（规格表 finance 节 + 对账表 + 交易单据种子段 + posted 一致性裁决）、`docs/backlog/comprehensive-test-data-and-visual-coverage-roadmap.md`（M1.2a1 行 + 横切关注点）、`docs/design/finance/posting.md`（过账路径语义）、`docs/testing/e2e-runbook.md`（视觉方法论 + 快照重录协议）
- Skill Selection Basis: roadmap M1.2a1 行指定 `nop-backend-dev` + `nop-testing`（同 M1.1 批先例：数据资产须对齐实体/字典/列命名约定；Proof 阶段运行测试套件与视觉 spec 属测试域）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（fresh-DB 由 `./scripts/start-app.sh` 承载；`init-database-data: true` 已默认开启）。
- 回滚策略：seed CSV 为纯新增文件 + 3 个既有 CSV 加性行追加，回滚 = 删除本批新增文件 + 还原 3 个既有 CSV + 常量回拨 + git revert 文档变更；不涉及数据迁移。

## Execution Plan

> 执行顺序约束：本计划为同批三计划（N=1/2/3）之首；`EXPECTED_APP_ERP_CSV_COUNT` 与 seed-data.md 对账表为共享触点，若 N=2/N=3 先落地，常量基数以「133 + 已落地批次 CSV 数」为准，避免覆盖写。

### Phase 1 - Seed CSV authoring（26 规格表 + 5 运行时补充 CSV）+ 过账凭证扩展

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision | Proof`
- Prereqs: 无（M0.1 规格表 + M0.2 门禁已就绪）

- [x] 逐实体核对 ORM 表名与列（`module-finance/model/app-erp-finance.orm.xml` tableName / `code=`）后，按规格表 finance 节（26 行，逐行引用不复制）创建 26 个 CSV，覆盖：AP 单据族（ap_document + log）、银行 4 件套（fund_account / bank_statement + line / bank_reconciliation + line）、预算族（budget_scenario / budget_line / carry_forward_log / control_log / rollforward_log）、核销族（reconciliation + line）、费用/预支（expense_claim + line / employee_advance）、凭证模板（voucher_template + line）、独立表（bad_debt / consolidation_elimination / gl_mapping_rule / intercompany_match / intercompany_transfer_price / posting_exception / trial_balance）；行数与用例指示编码（P / N-TERM / N-DIS）按规格表逐行执行
      - Skill: `nop-backend-dev`
- [x] 创建 5 个运行时补充实体 CSV（`erp_fin_cash_forecast` / `erp_fin_credit_facility` / `erp_fin_notes_discount` / `erp_fin_notes_payable` / `erp_fin_notes_receivable`，各 1~3 行）：列集以 ORM 实体定义为准（orm.xml 应收/应付票据/贴现/授信/现金预测实体段），必填 FK 以 ORM mandatory 列为准闭环（已知链：notes_payable → credit_facility〔本批〕、notes_discount → notes_receivable〔本批〕，是否 mandatory 执行期核验）；行数与用例指示比照规格表惯例（P + 必要 N-TERM/N-DIS 按状态列存在性），并在 seed-data.md 对账表补充档登记消费
      - Skill: `nop-backend-dev`
- [x] `Decision`（过账凭证扩展范围与一致性口径，触财务过账保护区 = plan-first 载体）：voucher/line/bill_r 16 行 → ~30 行的扩展须裁决一致性口径——(A) 仅扩展凭证族 3 文件，范围限定为与既有 `ar_ap_item`/`gl_balance` 种子一致性中立的方向（如红字冲销对、非 AR/AP 源凭证族），(B) 全口径扩展并加性追加 `ar_ap_item`/`gl_balance` 对应行。候选裁决输入：`docs/architecture/seed-data.md`「交易单据种子」核心范式 + 「posted 一致性裁决」（posted=true 当且仅当有凭证经 bill_r 串联）+「已知简化」（税额并入相邻科目）+ `docs/design/finance/posting.md`。记录选择、替代方案与残留风险于本计划；扩展行日期使用静态固定值（对齐既有 fin 参考期，冻结时钟纪律）
      - Skill: `nop-backend-dev`
- [x] `独立 plan-audit`（财务过账段保护区义务，roadmap 横切关注点 1.b「accounting/finance postings plan-first → M1.2a1 finance 段」+ 执行机制 4 保护区暂停协议 + 规则 2）：由独立子代理（fresh session）审查 Phase 1 凭证扩展行与 posted 一致性裁决、既有 C03 O2C 集成语义零冲突；批准记录落盘本计划 Draft Review Record 区；非触及行不受阻塞
      - Skill: none
- [x] FK 闭环按规格表逐行落实：必填 FK 仅指向〔已seed〕（域内锚点 ErpFinArApItem / ErpFinAccountingPeriod + 跨域 ErpMd* 锚点，如规格表所列）或〔本批〕新增行；可选 FK 留空或指向已 seed 行；禁止悬空引用；字典码 ∈ ORM dict
      - Skill: `nop-backend-dev`
- [x] 列头与格式对齐既有 CSV 约定：DB 列名大写下划线、省略审计列、ISO 日期、小写布尔、ID < 100000、静态日期；列集以 ORM/XMeta 生成的实体列为准（先抽样 `erp_fin_voucher.csv` / `erp_fin_ar_ap_item.csv`）；Proof: 列错误由 Phase 3 门禁首跑拦截（M1.1c 先例：price_list 误含 REMARK 列即被拦截）
      - Skill: `nop-backend-dev`
- [x] Proof: 干扰面预分析（执行首项，产出落本计划执行证据）——grep `app-erp-all/_cases` 集成用例源码中本批 26+3 表引用面（重点 C03 O2C / C13 期末）、finance 看板/报表 BizModel 读取面、`gl-mapping-rule` / `fin-period-close-wizard` 视觉 spec 断言面，判定零漂移预期或预判漂移用例清单
      - Skill: `nop-testing`

Exit Criteria:

- [x] 31 个 CSV 存在于 `_init-data/`，逐实体行数与用例指示编码符合规格表/补充档口径（实测 168 CSV，门禁零孤儿/零悬空背书；intercompany_match N-TERM 偏差已按反松弛规则登记）
- [x] 凭证扩展完成（3 文件合计 32 行 ≈ ~30 行）且一致性口径 Decision + 独立 plan-audit 批准记录在案（APPROVE，ses_fa3fe6aacffenhsfKE9kRw5Vxa）
- [x] 无悬空 FK：全部必填 FK 落在〔已seed〕∪〔本批〕集合内（TestErpSeedDataIntegrity 4/4 全绿，白名单零增量）
- [x] 干扰面预分析结论在案（预判漂移清单 + 实测收敛对账，见「独立 plan-audit 批准记录」段 Phase 1 执行证据）

### Phase 2 - 门禁常量 + owner doc 同步

Status: completed
Targets: `app-erp-all/src/test/java/io/nop/app/all/seed/TestErpSeedDataIntegrity.java`、`docs/architecture/seed-data.md`、`docs/design/finance/seed-data.md`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] 按常量 javadoc 随批更新协议更新 `EXPECTED_APP_ERP_CSV_COUNT`：133 → 164（+31），`EXPECTED_PLATFORM_CSV_COUNT = 4` 不变；不触碰 `EXPECTED_APP_ERP_ENTITY_COUNT` / `EXPECTED_MODEL_CLASSNAME_OMITTED_ENTITIES`（实体集未变）
      - Skill: `nop-backend-dev`
- [x] 同步 `docs/architecture/seed-data.md`：对账表计数（有 seed 133 → 164，精确缺 230 → 204，运行时缺 235 → 204 与精确口径收敛）+ 新增 M1.2a1 批次增量行 + 补充档注记更新（finance 5 实体转入有 seed）+ 快照重录义务节资产计数注记（M1.2a1 批次后 = 168 CSV + 1 SQL）
      - Skill: `nop-backend-dev`
- [x] 新建 `docs/design/finance/seed-data.md`「种子数据」段：逐实体 CSV 文件、行数、FK 闭环图（〔已seed〕/〔本批〕标注）、用例指示编码、negative 行语义（N-TERM 终态 / N-DIS 禁用）+ 凭证扩展一致性口径与行数分布
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 常量与对账表数值一致（164 / 204 / 204），且与 `_init-data/` 实际 CSV 构成一致（实测 168 CSV = 164 app.erp + 4 平台，门禁断言通过）
- [x] finance seed-data.md owner doc 段落地且与实际 CSV 内容一致（31 表清单 + 凭证扩展口径逐项对账）

### Phase 3 - Proof：门禁全绿 + 回归扫掠 + 运行时装载证明

Status: completed
Targets: 本仓库验证命令
Skill: `nop-testing`

- Item Types: `Proof`（6 项验证）+ `Add`（2 项行政收尾：日志条目 + roadmap 回写）
- Prereqs: Phase 1 + Phase 2

- [x] `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（scope-pinning 不变 + 零孤儿 CSV + 新 CSV 行数 > 0 + 引用完整性零悬空白名单零增量）
      - Skill: `nop-testing`
- [x] `mvn test -pl app-erp-all` 对照基线 71/0/0/1 评估：若本批 seed 行引起集成快照漂移（预期高风险：C03 O2C 凭证面 / C13 期末预检 fin 扫描面），按重录协议处置——良性内容增量 → 双面重录并在本计划追加「快照重录合规声明」段；行为破坏 → 先 Fix 再闭包；零漂移 → 登记与预分析结论的对账
      - Skill: `nop-testing`
- [x] 视觉快照双面义务核查：运行 finance 相关视觉 spec（`gl-mapping-rule` / `fin-period-close-wizard` / `dashboards.visual` + `dashboards.snapshot` / `reports.visual` + `reports.snapshot`，实仓 `tests/e2e/visual/` 清单复核后如有额外 finance 相关 spec 一并纳入）；若 seed 变更触发 DOM/像素漂移，按 `docs/testing/e2e-runbook.md` 视觉方法论双面（DOM + 像素）同步重录，并在本计划追加「快照重录合规声明」段；禁止单面重录
      - Skill: `nop-testing`
- [x] E2E 数值断言联动评估：检查本批 31+3 表是否被既有 `*.value.spec.ts` / `*.list-value.spec.ts` / 看板 KPI 期望值消费（finance 看板/报表读取面以 Phase 1 预分析为准）；有消费 → 同步期望值基线并登记
      - Skill: `nop-testing`
- [x] 运行时装载行为证明：`./scripts/start-app.sh restart`（fresh-DB）后 GraphQL `/r/{Entity}__findPage` 抽样 ≥ 5 张本批表（至少 2 张运行时补充实体 + 1 张主子表头 + `ErpFinVoucher` 行数 == CSV 行数）findPage 行数 == CSV 行数
      - Skill: `nop-testing`
- [x] `bash docs/audits/nop-compliance-checker.sh` exit 0，对照 `docs/audits/compliance-baseline.md` §BASELINE 机器块核对（机器块 R2c: 1537 + 已登记 pre-existing 增量 R2c +5 / R2b +2 / R12a +1，沿 plan `2026-09-01-0527-1` 口径协议；known-good-baselines 1542 为历史实测引用）零新增漂移（纯资源 + 测试常量变更，预期零漂移；若有漂移按失败模式速查开独立基线裁决）
      - Skill: `nop-testing`
- [x] `docs/logs/2026/09-{day}.md`（实际执行日）追加执行条目（已在 `docs/logs/2026/09-01.md` 首条目落地，含 ⑧ 验证全量记录；复跑实测与条目一致：gate 4/4、71/0/0/1、compliance exit 0 R2c=1542 零漂移）
      - Skill: none
- [x] 独立结束审计通过后回写 roadmap 工作项 M1.2a1 `ready` → `done`（含批次证据摘要，格式沿 M1.1a/M1.1b/M1.1c 行先例）（独立结束审计 APPROVE（fresh session task `ses_fa3329f3cffeKMVKDbQpF6cnLF`，0 Blocker/Major + 2 Minor 非阻塞），回写已落地）
      - Skill: none

Exit Criteria:

- [x] TestErpSeedDataIntegrity 全绿且常量断言通过（复跑实测 4/4 全绿，`EXPECTED_APP_ERP_CSV_COUNT=164` 断言通过）
- [x] `mvn test -pl app-erp-all` 与基线一致或更优（复跑实测 71/0/0/1 = M0.2 基线精确一致）；快照漂移处置已登记（C13 单用例重录，「快照重录合规声明」在案）
- [x] fresh-DB 重启后抽样表行数与 CSV 一致（真实装载 0 冲突 / 0 列映射错误；执行记录 9/9 表 findPage total==CSV 行数，含 3 张运行时补充实体 + voucher==8）
- [x] compliance checker 零漂移（exit 0，R2c=1542/R2b=242/R12a=71 与已登记 pre-existing 增量一致）；日志条目在位；roadmap 状态回写完成（M1.2a1 `ready`→`done` 含批次证据摘要）

## Draft Review Record

- Independent draft review iteration 1: accept（独立子代理 fresh session `ses_fa4b4811fffeCXeQX2VEuqt5Cs`）——0 Blocker + 0 Major；3 Minor（①「横切关注点 4」引证错位，实为横切关注点 1.b + 执行机制 4 + 规则 2；②FK 闭环例举误含 `ErpInvBatch`（finance 规格表无此锚点，系同批 N=2 措辞带染）；③Deferred 项缺显式重开触发）——①②已并入本稿修正，③以「roadmap 本体勘误归 roadmap owner（seed-data.md 对账表 L496/L511 权威登记）」为既有裁决 + M1.1b/M1.1c 先例保持原状。审查者实仓核验：137 CSV、finance 7 表已 seed、凭证 4/8/4 行数、常量 133/4/363、5 补充实体 ORM 在位（notes_payable→credit_facility L1555 / notes_discount→notes_receivable L1585）、26 规格表实体 1:1、C03/C13 快照面含 erp_fin_voucher*、视觉 spec 在位、164/204/204 算术自洽。

## 独立 plan-audit 批准记录（财务过账段）

- **APPROVE**（独立子代理 fresh session `ses_fa3fe6aacffenhsfKE9kRw5Vxa`，2026-09-01）：A..G 七项全 pass——①V5..V8 借贷平衡 + SUBJECT_ID/CODE/NAME 与 erp_md_subject 逐一吻合 + 字典码全合法（ORM dict 逐值核对）；②4 张 POSTED 凭证 bill_r 反查 1:1 闭环，billCode 均指向真实种子单据（NR/EA/NP-2026-001）；③git diff 实证既有 4 凭证行原 11 列逐字不变、ar_ap_item/gl_balance 零触碰、追加科目全 ASSET/LIABILITY → C13 PL_TOTAL=1280 与 finance.value.spec 1130/1130/0 不受影响；④V7/V8 与 C13 运行时 REVERSAL 录制范式逐字段镜像（负金额 + REVERSAL_OF_VOUCHER_ID + 双方 IS_REVERSED=true，对照 posting.md §冲销机制）；⑤EC-2026-001 posted=false（bill_r 零引用）与 EA-2026-001 posted=true（bill_r id=6 串联）均字面满足 posted 一致性裁决，notes 族 posted 静态值已按残留风险 ① 登记；⑥表头加性扩展下既有行过滤器语义零变化（V7/V8 经 isReversed=false 过滤正确排除，重录后 C13 TB 输出实证）；⑦方案 A/B 权衡与执行期 6601→全资产负债表科目修正如实登记。非阻塞建议 3 条：过滤器措辞精确化（`isNull or notIn[BUDGET,COMMITMENT]`，已修）、快照重录合规声明闭包前落地（已落地）、treasury.md 种子段 future note（登记 Follow-up）。

### Phase 1 执行证据

**Proof: 干扰面预分析结论（执行首项，实测命令与证据面）**

- **面 2 集成用例（`app-erp-all/_cases/io/nop/app/all/it/`，27 用例类）引用面 grep 实测**：erp_fin_voucher / voucher_bill_r / ar_ap_item 出现于 13 用例类 DB-state 快照（C01/C02/C03/C04/C05/C06/C07/C09/C10/C11/C12/C13/C14/P2pPilot）；快照机制为 `_chgType` 增量记录（A/U/D），既有 seed 行不入快照、显式 id（<100000）不消耗 default 序列 → 种子纯加性插入预期零快照漂移。
- **C13 期末预检读取面（`ErpFinAccountingPeriodProcessor` 逐方法核实）**：①`findUnresolvedFinanceExceptions` 仅扫 `posting_exception` status ∈ [PENDING,RETRYING,MANUAL] 且 voucherDate ∈ 结账期间且 voucherId 为空 → 种子取 RETRIED/IGNORED 即零进榜；②`unpostedVoucherCodes` → 种子凭证全 POSTED 即零进榜；③`populateAllowanceCheck`（allowanceBalance = 1231 科目凭证行聚合）→ 种子零 1231 行即保持 0；④`populateTrialBalanceForAllSchemas` **删除并按 POSTED 非 REVERSAL 凭证行全量重生成**期间试算平衡 → 种子凭证行必然改变 C13 TB 快照内容（预判漂移，须重录）。
- **C14 读取面**：`BankReconciliationBuilder/BankStatementMatcher` 按 statementId 定位（种子行不被其动作触碰）；`BadDebtProvisionService` 读非 SETTLED RECEIVABLE ar_ap_item + 1231 科目凭证行 → 种子零触碰。
- **AP 摄取管道**：`ErpFinApDocumentPipelineProcessor.processPending` 仅扫 status=RECEIVED → 种子取 DRAFTED/FAILED 零被摄取；去重守卫按 fileName 查询 → 种子用 `seed-fin-ap-` 前缀文件名隔离。
- **finance 看板/报表读取面**：`ErpFinDashboardBizModel` 读 gl_balance（revenue/netProfit/expense）+ fund_account（bankBalance 预警）+ ar_ap_item OPEN（AR/AP 余额）；`ErpFinReportBizModel` 读 voucher/line/bill_r + ar_ap_item + gl_balance + fund_account；budget 报表经 `IErpFinBudgetLineBiz.getBudgetVsActual` 读 budget_line。**利润表/现金流量表断言 token（1130.00/960.50/OUTFLOW）源自既有凭证行** → 追加凭证若含 P&L 类科目（收入/成本/费用 class）将改变利润表内容与 C13 损益结转合计（finance.value.spec `expense: 0`/利润表锚点 1280 均读该路径）。
- **视觉 spec 断言面**：gl-mapping-rule / fin-period-close-wizard 为结构断言（Crud 容器/向导步骤文本），数据不敏感；dashboards.visual + dashboards.snapshot（finance 条目）+ reports.snapshot（fin-income-statement/fin-ar-ap-aging）为内容/像素敏感面。
- **预判漂移清单**（写入时预期）：C13 TB 快照重生成漂移（必然）；C13 响应快照 id 序列漂移（TB 重生成行数 +N → 后续运行时 id 顺移）；dashboards finance DOM/像素漂移（bankBalance 0→15000）；fin-income-statement 像素漂移（若追加 P&L 科目凭证行）。**实测收敛**：追加凭证最终方案全资产负债表科目（见 Decision），实测 dashboards 10/10 + reports 24/6 + reports/dashboards value/smoke 全绿零漂移，仅 C13 需重录（已按协议执行并恢复 `*` 通配，见「快照重录合规声明」）。

**Decision（过账凭证扩展范围与一致性口径）**

- **选择：方案 A（仅扩展凭证族 3 文件，与既有 ar_ap_item/gl_balance 种子一致性中立的方向）**。
- **方案 B（rejected）**：全口径扩展须加性追加 ar_ap_item / gl_balance 对应行——gl_balance 行变化将直接改变 finance 看板 `getDashboardKpi` 的 revenue/netProfit/expense（`finance.value.spec.ts` 断言 1130/1130/0）与资产负债表 169.50 token，触发数值基线同步 + 更大双面重录面；且 gl_balance 聚合与凭证行聚合的双向一致性维护放大种子脆弱性。收益不抵风险，rejected。
- **方案 A 落地设计**（posting.md 语义对齐）：追加 4 凭证 = 2 张独立 NORMAL（V5 收票 NOTES_RECEIVABLE_RECEIVED：Dr 1121/Cr 1122 500；V6 预支抵扣 EMPLOYEE_ADVANCE_SETTLE：Dr 2241/Cr 1221 300）+ 1 组红字冲销对（V7 开票 NOTES_PAYABLE_ISSUED：Dr 2202/Cr 2203 800，posting.md「借应付账款/贷应付票据」逐字对齐；V8 REVERSAL postingType + reversalOfVoucherId=7 + 负金额红字，镜像 C13 运行时 REVERSAL 录制范式）。全部 POSTED / period 1 / 静态日期 2026-07 / 借贷平衡 / bill_r 反查闭环（V5↔NR-2026-001、V6↔EA-2026-001、V7·V8↔NP-2026-001）。**科目选择约束（执行期修正）**：初版 V5 曾用 6601 费用科目，`mvn test -pl app-erp-all` 首跑实测 C13 损益结转锚点漂移（PL 合计 1280→1580，`requireVoucherBalanced` fail）——按 M1.1b「seed 侧 Fix 清零」先例改为全资产负债表科目方案，P&L 结转/利润表/现金流量表/gl_balance 全部回归与既有种子字节级一致。`erp_fin_voucher.csv` 表头加性扩展 3 列（POSTING_TYPE/REVERSAL_OF_VOUCHER_ID/REMARK），既有 4 行新列留空 = null，期间/坏账准备凭证过滤器（`isNull(postingType) or notIn(postingType,[BUDGET,COMMITMENT])`，`ErpFinAccountingPeriodProcessor.findPostedVoucherIds` / `BadDebtProvisionService.findAllPostedVouchers`）语义不变。
- **残留风险**：①补充实体（notes 族等）`posted` 列取静态 false（V5/V7·V8 为其业务事件凭证，但「posted  iff 凭证串联」裁决定义域为 P2P/O2C 源单据族；补充实体 posted 语义归 treasury.md owner，登记为 documented simplification）；②V6 预支抵扣 300 与 EA-2026-001 辅助账 settledAmount=0 并存（核销回写由动作驱动、种子静态，roadmap Non-Goal「不做逐字段回放测试」口径内）；③C13 试算平衡快照含种子凭证行内容（已重录，见合规声明）。

**规格表偏差登记（intercompany_match N-TERM 不适用）**：状态字典 `erp-fin/intercompany-match-status` = UNMATCHED/MATCHED/DIFF，无终态值 → N-TERM 编码不适用，2 行按 P 覆盖 MATCHED/DIFF 两态（对应规格表「P+N-TERM」行的偏差，按反松弛规则在此显式登记而非静默降级）。

## 快照重录合规声明

**触发：已触发（面 2 C13 单用例，良性内容增量 + 双面像素/DOM 实测零漂移）。六点核查（沿 M1.1a/M1.1b/M1.1c 格式）：**

1. **seed 变更面**：新增 31 CSV（26 规格表 + 5 运行时补充）+ 既有 3 凭证族 CSV 加性行追加（16→32 行，含 voucher.csv 表头加性扩展 3 列；全清单见 plan Phase 1 与 `docs/design/finance/seed-data.md`）；既有种子行零修改（plan-audit ③ git diff 实证）。
2. **面 2（app-erp-all 集成快照）重录范围**：仅 `TestErpC13FinPeriodCloseReverse`——根因 = `populateTrialBalanceForAllSchemas` 按 POSTED 凭证行全量重生成试算平衡，种子凭证行（V5/V6，V7/V8 经 isReversed=false 过滤排除）新增 1121/2241/1221 三个 TB 科目行 → TB 快照内容增量 + 后续运行时 id 序列 +3 顺移（良性内容增量，非行为破坏；C13 损益结转锚点 1280 行为漂移已在重录前经 seed 侧 Fix 清零）。重录方式 = 方法级 `@EnableSnapshot(saveOutput=true)` 录制后移除注解，CHECKING 复跑 1/1 绿。其余 26 用例类（含 C01/C03/C14 finance 触面）零重录、零改动。
3. **面 2 重录合规性**：重录产生的响应快照 `*` 通配覆写已按 e2e-runbook「JUnit 快照 delVersion 列语义」节 B4-B10 先例恢复（3/4/5/6_period_*.json5 的 endDate/closedAt/createTime/updateTime/reverseCloseAt 五字段恢复 `*`，git diff 复核仅剩 TB/voucher/line/bill_r 四表合法内容增量）；input 侧 seed 表 CREATE_TIME/UPDATE_TIME 字面刷新为语义惰性（runbook 口径）。
4. **面 1（各域 module-<domain>/_cases）**：零影响（域模块测试不声明 `nop.orm.init-database-data`，种子不进其装载路径）——M1.1a/M1.1b/M1.1c 同口径，零重录。
5. **视觉双面义务**：`dashboards.visual`（10/10）+ `dashboards.snapshot`（10/10，含 finance 条目）+ `reports.visual`（24/24）+ `reports.snapshot`（6/6，含 fin-income-statement/fin-ar-ap-aging 像素基线）实测全绿**零漂移**——像素基线与 DOM 断言均未触发重录（追加凭证全资产负债表科目设计使利润表/现金流量表/gl 读取面字节级不变；fund_account 银行余额变化不落入 finance 看板快照断言面）。双面均为实测通过态，无单面重录情形。预存的 gl-mapping-rule.visual 2 失败经移除本批种子重启对照实测为**预存失败**（与本批无关，flux 迁移期结构断言漂移家族，登记 Follow-up 归 successor）；fin-ap-document.value 2 失败为 config-gate 环境性预存（`ap-doc-pipeline-enabled=false`，与本批无关）。
6. **E2E 数值断言联动**：`finance.value.spec.ts`（KPI 1130/1130/0 读 gl_balance，零变化）+ `finance.list-value.spec.ts`（expectedCount 为 ≥ 断言，凭证 4→8 通过且 PZ-2026-001 token 在位）+ fin-* 五张报表 value spec（token 容断言，全绿）实测零基线同步需求；其余消费面（26 个 fin-* business-action specs）26/28 通过，2 失败为预存 config-gate（见第 5 点）。

## Closure Gates

> 全量仓库验证（`mvn clean install -DskipTests` / 全 reactor `mvn test`）不在本计划闭包门控内：本计划变更为资源 + 单测试常量 + docs，`mvn test -pl app-erp-all` + compliance checker 已覆盖变更面；全量基线登记归 M3.1 兜底工作项。

- [x] 范围内行为完成（31 CSV + 凭证扩展 + 常量 + 对账表 + finance owner doc 段）
- [x] 相关文档对齐（seed-data.md 对账表/补充档/快照重录注记 + finance seed-data.md；若快照重录触发，e2e-runbook 协议遵循已登记）
- [x] 已运行验证（Phase 3 全部 Proof 项，含 compliance checker 复跑；独立结束审计复跑全量 suite 71/0/0/1 确认）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred But Adjudicated 仅措辞漂移登记；gl-mapping-rule 2 失败 / fin-ap-document 2 失败经对照实测为预存，非本批引入）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 accept）
- [x] 财务过账段独立 plan-audit 已执行且批准记录落盘（保护区义务，APPROVE `ses_fa3fe6aacffenhsfKE9kRw5Vxa`）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（Plan Status completed ↔ Phase 1/2/3 全 completed ↔ Exit Criteria 全 [x] ↔ Closure Gates 全 [x] ↔ docs/logs/2026/09-01.md 条目在位）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（APPROVE，fresh session task `ses_fa3329f3cffeKMVKDbQpF6cnLF`）
- [x] 结束证据存在于文件中（本计划 Closure 段 + docs/logs/2026/09-01.md + roadmap M1.2a1 行）

## Deferred But Adjudicated

- **roadmap「既有 8 行 voucher/line/bill_r」措辞漂移**：实仓实测 16 行（4/8/4），本计划以实仓为基线；roadmap 本体勘误按 M0.1 先例不在工作项范围（对账以 seed-data.md 对账表为权威）。
  - Classification: `documentation drift (roadmap owner)`
  - Why Not Blocking Closure: 数值口径以 seed-data.md 对账表 + 本计划 Current Baseline 实仓实测为权威，扩展目标（~30 行）不受措辞漂移影响。
  - Successor Required: `no`（roadmap 本体勘误归 roadmap owner，同 M1.1b/M1.1c「措辞漂移」先例口径）

（其余待执行期裁定：若规格表某行证伪 CSV-only 可满足性，按反松弛规则移入本节分类登记）

## Closure

Status Note: 计划可闭包——31 seed CSV（26 规格表 + 5 运行时补充）+ 过账凭证扩展 16→32 行全部落地，门禁常量 133→164 与 seed-data.md 对账表（164/204/204）及 `_init-data/` 实仓构成（168 CSV = 164 app.erp + 4 平台）一致；`docs/design/finance/seed-data.md` owner doc 段落地。验证终态：`TestErpSeedDataIntegrity` 4/4 全绿、`mvn test -pl app-erp-all` 71/0/0/1 = M0.2 基线精确一致（C13 单用例快照重录 = 良性内容增量，合规声明在案）、fresh-DB 装载 9/9 抽样表 findPage==CSV 行数、视觉双面 dashboards 20/20 + reports 30/30 零漂移、compliance checker exit 0 R2c=1542 零新增漂移。财务过账保护区义务（plan-first + 独立 plan-audit）与独立草案审查、独立结束审计三重证据齐备。roadmap M1.2a1 已回写 `done`。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，task id `ses_fa3329f3cffeKMVKDbQpF6cnLF`）
- Evidence: VERDICT **APPROVE**（0 Blocker / 0 Major / 2 Minor 非阻塞：①Phase 3 `Status` 标签滞后于实际进度——已随闭包翻转修正；②本批计划文件 git 未跟踪——同批起草期常态，沿 M0.x/M1.1x 先例）。审计实测复现：168 CSV（38 erp_fin_* = 7 既有 + 31 本批，5 补充实现在位）；凭证族 8/16/8 = 32 行，ΣDr 4981.00 = ΣCr 4981.00，V8 REVERSAL_OF_VOUCHER_ID=7 负金额红字镜像成立，bill_r 5–8 反查 NR/EA/NP-2026-001 闭环；常量 164/4/363；seed-data.md 164/204/204 + 批次行 + 资产注记；finance seed-data.md 三节齐备；日志 ⑧ 验证记录一致；独立复跑 gate 4/4 全绿 + 全量 suite **71/0/0/1 BUILD SUCCESS**（C03/C13/C14 全绿）+ compliance exit 0（R2c=1542 = 机器块 1537 + 已登记 +5，R2b=242、R12a=71 同口径）；git 脏面 = 本批预期资产（纯资源 + 测试常量 + docs，零生产代码）。

Follow-up:

- （仅非阻塞跟进项目；已确认的缺陷不得出现在此处）
