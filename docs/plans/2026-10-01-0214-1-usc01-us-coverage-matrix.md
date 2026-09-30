# 2026-10-01-0214-1-usc01-us-coverage-matrix 全量 US 三维覆盖矩阵与隐性缺口裁定

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-01（M1 覆盖基线与缺口裁定）
> Related: `docs/requirements/2026-09-22-erp-user-stories.md`、`docs/analysis/2026-09-22-erp-user-story-gap-analysis.md`、`docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`
> Audit: required

## Current Baseline

- 输入故事集：`docs/requirements/2026-09-22-erp-user-stories.md` 实测 **53 条 US**（Epic A 主数据 4 + B P2P 6 + C O2C 6 + D 库存 6 + E 财务 8 + F 制造质量 6 + G 运营 3 + H 外围 6 + I 横切 8）；roadmap 表述「约 50 条」以实仓 53 为准。
- 既有裁决：`docs/analysis/2026-09-22-erp-user-story-gap-analysis.md` 已给逐故事满足度（✅/🔶/🕒/❌）+ 需要度（Must/Should/Could/档位）+ 裁决建议，但其「证据」列为文档级指针（owner doc / inventory 名），**未逐条落到 spec 文件名 / BizModel 类 / config 键的实时仓证据**，且无独立「隐性缺口」列。
- 既有矩阵：`docs/analysis/2026-06-30-1200-feature-coverage-matrix.md`（2026-06-30）为「调研功能 × 设计文档」口径，**非 US 三维（设计/实现/测试）口径**；roadmap 冲突权威规则原文为「以 coverage 矩阵 + 实时仓库为准」；本计划执行更严口径：最终采信以实时仓证据为准，与两份既有文档的每处差异逐条登记不静默覆盖（见 Phase 2 差异清单项）。
- 测试面实仓盘点：`tests/e2e/` 实测 298 spec 文件（business-actions 117 / reports 50 / crud 42 / dashboards 26 / visual 28 / negative 18 / orchestration 10 / 其余诊断类）；后端 JUnit 面以各域 `-service` 测试类与 `app-erp-all` 集成测试（C01-C21）为准；基线登记见 `docs/testing/known-good-baselines.md`。
- 剩余差距：无逐 US 的「设计证据路径 / 实现落点 / 测试断言锚点 / 隐性缺口分流」四维矩阵；USC-06/07 依赖本矩阵裁定真实缺口范围。

## Goals

- 产出 `docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`：53 条 US 逐条登记四列证据——设计（owner doc / product-scope / feature-inventory 精确路径）、实现（能力落点：BizModel 类 / config 键 / 实体 / 页面路径，含 config 默认态）、测试（spec 文件名或 JUnit 类名，无则记「无」）、隐性缺口（按 Must/Should/档位分流至 USC-02a..07、「无缺口、仅登记」、或归属冲突升级登记三者之一）。
- 每条证据经实时仓库核验（grep 实仓命中），不以 gap analysis 文本转抄冒充核验（lessons/13 快照型断言引用前 grep 实仓重验）。
- 隐性缺口汇总表：显式登记每项缺口 → 承接工作项（USC-02a/02b/03/04/05/06/07）映射、「无缺口、仅登记」裁决、或归属冲突升级登记（roadmap 规则 6）三者之一，供 USC-06/07 按真实缺口收窄范围。

## Non-Goals

- 不产出字段级 schema（归 `model/*.orm.xml`）。
- 不提新域提案、不改 product-scope 域范围。
- 不为 🕒/Won't 项（US-PL-06 门户、POS、原生 App、多租户启用、外部集成、AI 分析）设实现计划；仅登记边界。
- 不在本 roadmap 内维护第二套带状态覆盖表（矩阵为一次性产物，状态真相源仍为 roadmap Work Item Status）。
- 不修改任何生产代码 / 测试代码 / owner doc 行为语义（USC-01 为裁定类工作项；owner doc 漂移仅登记，修复归承接工作项）。

## Task Route

- Type: `verification or audit work`（分析/裁定产出，零生产行为变更）
- Owner Docs: `docs/requirements/2026-09-22-erp-user-stories.md`（输入）、`docs/analysis/2026-09-22-erp-user-story-gap-analysis.md`（裁决输入）、`docs/design/feature-inventory.md`、`docs/requirements/product-scope.md`
- Skill Selection Basis: `Skill: none`——已对照方法相近项：`requirement-compliance-audit-prompt`（其输入为 L1 use-cases + arm-index、输出为 P1-RC finding 生命周期，与本项输入 US 故事集 + gap analysis、输出覆盖矩阵不同形，不整体套用）与 `open-ended-audit-prompt`（通用模板须全量定制，收益低）；结论 none 与 roadmap 第 2 节 Skill 列及横切关注点 7 同判。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（只读仓库盘点 + 文档产出）。

## Execution Plan

### Phase 1 — 实仓证据盘点（按 Epic 分簇核验）

Status: completed
Targets: `docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`（草稿）
Skill: none

- Item Types: `Proof`
- Prereqs: 无

- [x] Proof: Epic A/B/C（主数据 + P2P + O2C，16 US）逐条核验：owner doc 路径存在且含对应能力段；实现落点（实体/BizModel/config）grep 实仓命中；测试锚点（spec/JUnit 文件名实测存在）。
      Skill: none
- [x] Proof: Epic D/E（库存 + 财务，14 US）同口径核验；US-FN-06 按 gap analysis 收口口径登记（主表 ✅ / 叙述 prod 默认关），config 默认态以 `module-meta.yaml`/application 实测为准。
      Skill: none
- [x] Proof: Epic F/G/H（制造质量 + 运营 + 外围，15 US）同口径核验；US-MF-*/US-B2-01 按档位（纯商贸/制造/完整）标注；US-HR-02 xwf 已知限制（2330-1 裁决）如实登记。
      Skill: none
- [x] Proof: Epic I（横切 8 US）同口径核验；US-PL-01/02/03/07/08 config 默认态与页面/架构证据实测；US-PL-06 按 portal/README future 语义登记为 🕒。
      Skill: none

Exit Criteria:

- [x] 53 US 每条在矩阵终稿具备四列证据，证据指针全部为实仓核验命中（文件路径 + 关键锚点），无「文档提到」转抄残留（4 路独立子代理实仓盘点 + 执行者 3 处矛盾点交叉复核，见矩阵 §七诚实性声明）。

### Phase 2 — 矩阵合成与隐性缺口裁定

Status: completed
Targets: `docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`（终稿）
Skill: none

- Item Types: `Decision | Add`

- [x] Add: 按 9 Epic 合成四列矩阵主表（US × 设计/实现/测试/隐性缺口），每行携带满足度判定（✅ 40 / 🔶 12 / 🕒 1，与 gap analysis 对照，10 项差异显式登记于矩阵 §六）。
      Skill: none
- [x] Decision: 隐性缺口逐项裁定——三分类 + 第三分支均已执行：分流表 12 项（承接编号×9：USC-02a/02b/03×2/04/05/06×2/07；归属冲突升级×2：US-PO-01、US-SO-06；仅登记×1+36 条注记级）。升级项落盘矩阵 §五 + roadmap 审查记录段。
      Skill: none
- [x] Decision: 差异清单已落盘矩阵 §六（10 项，含执行中代理误判的纠正记录 #9），逐条「既有说法 → 实仓证据 → 采信口径」。
      Skill: none

Exit Criteria:

- [x] 矩阵终稿五要件齐备（`docs/analysis/2026-10-01-0300-1-us-coverage-matrix.md`：§一摘要 40/12/1/0 分布 + §三九 Epic 53 行主表 + §四分流表 12 项 + §六差异清单 10 项 + §七诚实性声明）。
- [x] USC-06/07 范围可据分流表直接确定（USC-06 降级为档位验收登记×2、USC-07 承接 US-IV-02 在途行为断言；每项三分支之一，零悬空引用——comm 校验 ID 集合与需求文档精确一致）。

### Phase 3 — 归属冲突升级（条件分支，仅当 Phase 2 触发第三分支）

Status: completed（条件阶段已触发：Phase 2 判定 2 项归属冲突缺口，升级登记已落盘 roadmap 审查记录段）
Targets: `docs/backlog/user-story-coverage-roadmap.md`（仅登记注记，不改队列）
Skill: none

- Item Types: `Decision`

- [x] Decision: 2 项归属冲突（US-PO-01 Must⚠、US-SO-06 Should）已登记矩阵 §五（缺口描述 + 不承接理由 + 双向建议归属），并在 roadmap 审查记录段追加升级登记行；未新增/修改 roadmap 工作项队列。
      Skill: none

Exit Criteria:

- [x] 升级登记落盘（roadmap 审查记录段 2026-10-01 行），无静默缺口残留。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_9313cdfd，0 Blocker + 1 Major + 3 Minor）——M1 缺口分流缺「真实缺口无既有承接」第三分支；m1 Skill basis 产物导向非方法对照；m2 Last Reviewed 预填与审查记录矛盾；m3 roadmap 冲突规则转述漂移。已整改：第三分支 + 条件阶段 Phase 3、Skill 方法对照式、头部改回填占位、忠实引用原文 + 更严口径注记、去「为主」冗余。
- Independent draft review iteration 2: needs revision（同一独立子代理 agent_9313cdfd 定点复审，0 Blocker + 1 Major M2）——第三分支未传播到 Goals/Closure Gates/Deferred/Follow-up 四处摘要表述，Closure Gates「已运行验证」与 Phase 2 Exit 三分支口径直接矛盾。已按复审建议逐字落实 5 处（Goals 两条、Closure Gates 验证项、Deferred 理由、Follow-up 理由）。复审明示：落实后无需再轮审查，即标记 accept。
- Independent draft review 终裁: accept（agent_9313cdfd iteration 2 复审意见授权置位——「完成上述 4-5 行措辞同步后无需再轮审查……即可标记 accept，Last Reviewed 待回填、计划转 active」）。

## Closure Gates

- [x] 范围内行为完成（53 US × 四列证据 + 缺口分流表 + 差异清单全部落盘）
- [x] 相关文档对齐（矩阵为新增分析文档；roadmap USC-01 行状态回写 + 审查记录段升级登记；不触 owner doc 行为语义）
- [x] 已运行验证（主表 53 行 + ID 集合 comm 校验与需求文档 53 条精确一致零缺失零多余 + 5 锚点抽检重放全部命中 [enforceBarcodeUnique/convertToOrder/forwardTrace|backwardTrace/fin-budget-control+qa-recall spec/getQuotaRollup] + 分流表 12 项三分支之一；纯文档计划无 build/test 门控，理由：零生产行为变更）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（2 轮收敛 accept，见 Draft Review Record）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘 docs/logs/2026/10-01.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + 矩阵 + roadmap 审查记录段 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

（无——USC-01 为单次可交付裁定工作项；缺口项按 Phase 2 三分支处置（承接编号 / 仅登记 / 归属冲突升级登记），不在本计划挂 Deferred。）

## Closure

Status Note: 独立结束审计 ACCEPT（0 Blocker + 0 Major + 3 Minor 已随 closure 整改：分流表余量计数 40→36 修正、计划承接计数 ×8→×9 修正、日志落盘）。计划契约、交付物五要件、实仓证据、分流三分支、门控防御、范围完整性、程序独立性七项全过。USC-01 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh read-only session，2026-10-01）
- Evidence: 独立复跑全通过——主表 53 行（grep wc）；ID 集合 comm 双向与需求文档 53 条精确一致；9 锚点跨 6 Epic 重放全命中（enforceBarcodeUnique:321 / SoDGuard+ErpPurOrderProcessor:345 / RecallTargetLocator / ErpCtContractExpiryJob / log-delivered-freight-posting spec 含 handleTrackingWebhook×9 / e2-3 spec / application.yaml :54/:66/:81 三 profile 分层 / forwardTrace / getQuotaRollup:49）；US-PO-01 降档、US-IV-02 在途缺口、US-LG-01 纠正记录三条判定抽检属实；分流表 12 项三分支之一 + 升级 2 项落盘 roadmap 审查记录段；门控 8 项中 2 审计项经审计方确认保持 [ ] 未预勾（lessons/25 防御成立）。

Follow-up:

- （无——缺口按 Phase 2 三分支处置落盘，本计划不另立 successor。）
