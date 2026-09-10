---
status: active
mission: ai-check-r3
work-item: M2.2
group: "2026-09-10-0705"
verify: [test]
---

# 2026-09-10-0705-1 M2.2 finance P1 修复批（域批独立项 P3-CK-fin4-024-r3 intercompany/抵销通道维度硬编码）

## Current Baseline

- 批内面 = 恰 1 条：`P3-CK-fin4-024-r3`（open，`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-finance-period-misc-r3.md`；跨轮索引行在案）。finance 域 r3 新立 P1 = 0（M1.1~M1.4 四切片新立全为 P2/P3），无同批可合并的其他 P1——本域批 charter 为 P1 修复批，r3 语义下实际承载 = M2.0 族裁决路由的域批独立项（裁决表 row 81：fin4-024-r3 → M2.2）。
- 其余 fin 域 r3 新立 finding 去向（M2.0 族裁决，均非本批）：fin2-018-r3（D·分片3）、fin3-019-r3（D·分片3）、fin3-017-r3（A·分片2）、fin4-022-r3（A·分片2）、fin4-023-r3（B·分片1）、fin3-018-r3（doc 批·分片5）。
- 缺陷面（执行者 2026-09-10 HEAD 实核四站在位；finding 报告所记短名 TransferBizModel/PostEliminationProcessor 对应实类如下）：
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/intercompany/ErpFinIntercompanyTransferBizModel.java`：L127/L177 `String currencyId = "1"` 双路径（intercompany 配对凭证）；L263-265 `resolveOrgAcctSchemaId` 恒 `return "1"`（该方法带 successor 注记——账套面已有登记，币种面无任何登记）。
  - `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/processor/ErpFinConsolidationEliminationPostEliminationProcessor.java`：L84 凭证头 `setAcctSchemaId("1")` + L111/L135 抵销行 `setAcctSchemaId("1")` + L107/L131 抵销行 `setCurrencyId("1")`。
- 范式不对称：P1-CK-fin3-005 修复体（F2.3 批次）已建立「resolveCurrencyId 经账套本位币解析」范式，但未覆盖本通道；同族先例 fin3-010/inv-010（P2）、mnt-016/mfg-021（P3）。
- config 语境（lesson 14 纪律）：两通道由 `intercompany-posting-enabled` / `consolidation-elimination-enabled`（finding 报告所记键名，执行时以实仓 application.yaml/代码为准复核）门控、缺省关闭——缺省关闭合法 opt-in，本批不改 config 默认值；索引登记的升 P1 触发 = 任一 config 投产启用，本批在 M2.9 消费证据中显式登记该触发条件。
- 修复方法约束（r3 M2 前言 + M2.0 owner doc `docs/architecture/finding-remediation-method.md`，其为本计划直接 Prereq）：先写失败测试 → 修复 → 测试绿 + 既有测试零回归；错误参数传码不传散文；本批零 ORM/api.xml/seed/页面变更（不触发保护区双批准与 seed 双面重录）；会计过账族改动 = plan-first + owner doc + tests（本计划即 plan-first 载体，owner doc 见 Task Targets）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（全 reactor 基线，M1.17 口径裁决：surefire 聚合真值 3991/0/0/1，基线行登记 4006 系日志拼接重复计入）；compliance R2b=242/R2c=1542/R12a=71；CJK `--strict` PASS（CAT1..4 = 0/0/0/0）。fin service 模块参照 533/0/0/0（M1.2/M1.3/M1.4 切片锚点）。
- 剩余差距：intercompany/抵销通道币种与账套维度硬编码 + 解析链零测试断言；M2.9 收官需本批 fixed 证据指针。
- 依赖状态：M2.0（plan `2026-09-10-0425-1`，组 `2026-09-10-0425`）已完成（其 plan 交付裁决表与修复方法 owner doc）；本计划组 `2026-09-10-0705` 内执行序 N=1（roadmap 文档序 M2.2 → M2.4 → M2.5；三域批无共享文件面，顺序仅为稳定序）。

## Goals

- `P3-CK-fin4-024-r3` 修复落地：intercompany 配对凭证与合并抵销凭证通道的 `currencyId` 经账套本位币解析（镜像 fin3-005 范式），`acctSchemaId` 经账套解析链取值（方案裁决见 Phase 2 Decision），消除 `"1"` 硬编码；恒等部署（orgId=1/单账套/本位币=1）下产出凭证逐字节不变（既有测试零回归为证）。
- 先写失败测试：非恒等账套场景（≥2 账套、非 "1" 本位币）断言凭证 `currencyId`/`acctSchemaId` 三元组为解析值而非 `"1"`，先红后绿，填补该通道零断言缺口。
- 零回归 + 门控持平：fin service 套件全绿 + 全 reactor `mvn test` 零新增失败；compliance/cjk 双 checker 不高于基线；seed 零改动证明。
- M2.9 消费证据落盘：fixed 证据指针 + config 投产升 P1 触发条件登记 + 零触碰声明。

## Non-Goals

- 不修 finance 域其他 r3 finding（fin2-018/fin3-017/018/019/fin4-022/023-r3——去向以 M2.0 族裁决为准，属 M2.8 分片 1/2/3/5）。
- 不改 `intercompany-posting-enabled` / `consolidation-elimination-enabled` 默认值，不做通道投产（lesson 14：缺省关闭合法 opt-in；投产触发升 P1 走独立裁决）。
- 不动 `resolveOrgAcctSchemaId` 既有的 successor 注记语义（账套面登记不重开），不重构 Intercompany/Elimination 编排结构（最小面接入解析链）。
- 不改 ORM/api.xml/seed/页面文件；不做双索引状态回填（M2.9 义务）；不做 roadmap 状态翻转。

## Phase 1 — 失败测试先行（非恒等账套场景 + 缺陷链对账）

> 统一类型：Proof（2 项 Proof）。
> Skill: bug-diagnosis-prompt（roadmap M2.2 行指定；先读 `docs/skills/bug-diagnosis-prompt.md` 四阶段定位纪律再动手）
> Targets: `module-finance/erp-fin-service/src/test/java/`（新增或扩展 intercompany/抵销集成测试类；落点执行时按现有测试布局选定，快照或 JunitBaseTestCase + IGraphQLEngine 沿 `nop-testing` skill 范式；config 键测试域启用经 `@NopTestConfig` 属性覆盖，不改生产默认值）
> Prereqs: M2.0 计划完成（修复方法 owner doc 在位）

- [x] <Proof> 写失败测试：构造非恒等维度场景（测试内 fixture 建 ≥2 账套/非 "1" 本位币——测试 case 级 `input/tables` CSV 或 @Before 直插，禁触 `_init-data/`），分别驱动 intercompany 配对凭证与合并抵销凭证生成，断言产出凭证头/行的 `currencyId` 与 `acctSchemaId` 为解析值（法人根账套本位币 / 对应账套）；恒等控制组断言现行为不变（"1"）。断言可观察产出（凭证字段实值），非仅类型。执行确认红：现硬编码 → 解析值断言失败，失败输出记入勾选注记（缺陷复现证据）。
      - Skill: bug-diagnosis-prompt
      - 注记（2026-09-10）：测试类 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/intercompany/TestErpFinIntercompanyEliminationDimension.java`（4 方法：transfer/tradeDocument/elimination 非恒等 + 恒等控制组；fixture 直插 ErpMdCurrency USD + orgA/orgB 各挂 FINANCIAL ACTIVE 账套 functionalCurrencyId=USD，config 门控经测试域 `intercompany-test.yaml` 启用，生产默认零触碰，`_init-data/` 零触碰）。执行确认红：`mvn test -Dtest=TestErpFinIntercompanyEliminationDimension` → `Tests run: 4, Failures: 3, Errors: 0`——非恒等 3 测试恰在维度断言上红（`凭证头 acctSchemaId 应为账套解析值 4 … expected: <4> but was: <1>`，transfer L91 / tradeDocument L111 / elimination L139 三站复现）＝缺陷复现证据；恒等控制组 `testIdentityDeploymentKeepsLegacyDimensions` 绿（现行为 "1" 不变基线）。
      - Skill: bug-diagnosis-prompt
- [x] <Proof> 复现定位记录：按 bug-diagnosis 四阶段将硬编码站点（上列四站 + `resolveOrgAcctSchemaId`）与 finding 报告证据对账，复核 config 键名实仓真值（application.yaml/代码默认值），确认无第五站点（grep `currencyId = "1"` / `setAcctSchemaId("1")` 全 fin 域实核注记；fin2-017 族魔法默认 "1" 站点归 r1 通道仅互链）；对账 P1-CK-fin3-005 修复体解析链（复用点与差异），结论记入注记。
      - Skill: bug-diagnosis-prompt
      - 注记（2026-09-10，bug-diagnosis 四阶段对账）：①站点对账——grep 实仓（HEAD）`currencyId = "1"|setCurrencyId("1")` 全 fin main 恰 4 站 = `ErpFinIntercompanyTransferBizModel` L127/L177 + `PostEliminationProcessor` L107/L131；`setAcctSchemaId("1")` 恰 3 站 = PostElimination L84/L111/L135；`resolveOrgAcctSchemaId` L263-265 恒返 "1"（带 successor 注记）——与 finding 报告 P3-CK-fin4-024-r3 逐站一致，**无第五站点**。②邻近命中分类——`CreditFacilityInterestVoucherBuilder` L60-63 为 `AcctSchemaResolver.resolvePrimarySchemaId` 解析后 null→"1" 恒等回退（fin3-005 范式跟随者，非缺陷站点）；fin2-017 族魔法回退站点（ExchangeRevaluation L288/L297、AnnualClose L391/L411、ProfitLoss L205/L225、BadDebt L346/L366、NotesReceivable L318）均为解析链 + "1" 回退形态，归 r1 通道仅互链不重开。③config 键名实仓真值——`ErpFinConstants` L488 `erp-fin.intercompany-posting-enabled` / L490 `erp-fin.consolidation-elimination-enabled`，代码默认 `Boolean.FALSE`，application.yaml 无投产设置（与 owner doc `intercompany-consolidation.md` §配置项一致，lesson 14 缺省关闭合法 opt-in）。④fin3-005 修复体对账——复用点 = `app.erp.md.dao.AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)`（F2.1 ACTIVE 字面量单一来源）+ `schema.getFunctionalCurrencyId()`；差异 = fin3-005（BudgetCommitment `resolveOrgAndSchema`）经 periodId→period.orgId 间接解析，本通道已持有法人根 orgId（fromLegal/toLegal/seller/buyer/candidate.orgId）可直连解析（更短链，语义同型）。⑤假设排除——凭证头无 currencyId 列（币种仅存行级，ORM 实核）→ 断言面 = 头 acctSchemaId + 行 currencyId/acctSchemaId 三元组；抵销候选实体无账套/币种维度列（实核 columns 清单）→ Phase 2 走「同一解析链」选项。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [x] 非恒等场景失败测试在位且当前树红（两通道解析值断言失败 + 恒等控制组绿），失败输出注记在案
- [x] 硬编码站点与 finding 报告逐站对账一致，config 键名实仓复核在案，无未登记第五站点

## Phase 2 — 解析链接入（Decision + Fix）

> 统一类型：Decision | Fix（1 Decision + 2 Fix）。
> Skill: bug-diagnosis-prompt
> Targets: `ErpFinIntercompanyTransferBizModel.java`（currencyId 双路径 + resolveOrgAcctSchemaId）、`ErpFinConsolidationEliminationPostEliminationProcessor.java`（currencyId/acctSchemaId 五站）；owner doc 对账面 = `docs/design/finance/period-close.md`（intercompany/抵销语义）
> Prereqs: Phase 1 完成（失败测试在位）

- [x] <Decision> acctSchemaId 解析方案裁决：推荐 (a) 沿 `resolveOrgAcctSchemaId` 既有挂点接入真实账套解析链（orgId → 法人根账套，镜像 fin3-005 修复体解析范式），`PostEliminationProcessor` 凭证头/行账套取抵销候选行携带维度或同一解析链（二选一在注记记录，依据 GlMappingResolver/候选行结构实核）。替代方案：(b) 全部经 GlMappingResolver 动态解析——若实核发现 resolver 仅承担科目映射非账套解析则否决；(c) 候选行全面加维度列——涉 ORM 变更，本批禁触（保护区），否决。残余风险：非恒等部署下两通道行为首次可变，须以 Phase 1 恒等控制组 + 全 reactor 回归兜底。币种面无替代方案分歧（直接镜像 fin3-005 范式：账套本位币）。裁决与 owner doc 对账：核对 `period-close.md` intercompany/抵销节语义一致，发现漂移先修 doc 注记再落码。
      - Skill: bug-diagnosis-prompt
      - 注记（2026-09-10，Decision 三要素）：**选择 = (a)**——`resolveOrgAcctSchemaId` 挂点接入 `AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId)` 真实解析（法人根 orgId → FINANCIAL 优先 ACTIVE 主账套；null→"1" 恒等回退经注记说明）；`PostEliminationProcessor` 二选一裁决 = **同一解析链**（实核依据：`ErpFinConsolidationElimination` columns 全清单 id/code/orgId/eliminationType/periodId/pairKey/matchId/fromOrgId/toOrgId/eliminationAmount/draftVoucherId/status/remark——**无 acctSchemaId/currencyId 维度列**，「取候选行携带维度」不可行；GlMappingResolver 实核仅承担科目映射 `resolveSubjectCode(billType, accountKey, dims, acctSchemaId)`，非账套解析）。**替代否决**：(b) 否决——resolver 仅科目映射非账套解析（实核 `IntercompanyVoucherGenerator.resolveSubjectCode` L269-280 消费形态）；(c) 否决——ORM 变更涉保护区（`model/*.orm.xml` auto + dual-agent-approval），本批 Non-Goal 禁触。**残余风险**：①非恒等部署下两通道凭证维度首次可变——Phase 1 恒等控制组绿 + 全 reactor 回归兜底（Phase 3 门控）；②配对凭证生成器单 currencyId 参数（`generatePairedVouchers` 签名实核）——两法人本位币不同时取 AR 侧法人根账套本位币（transfer=fromLegal / tradeDocument=sellerLegal / elimination=candidate.orgId 即 AR 侧），跨币种配对精确解析归 successor（登记不阻塞，恒等部署不受影响）。**owner doc 对账结论**：`period-close.md` 无 intercompany/抵销凭证币种/账套维度语义条款（grep 实核，仅「实现范围注记」与 A2 注记行命中 intercompany 字样）；配置键与门控语义锚 = `intercompany-consolidation.md` §配置项（3 键与 `ErpFinConstants` 一致）——本批零 config 默认值改动、修复走平台既有 `AcctSchemaResolver` 解析链与 owner doc 无冲突，**无漂移，无需修 doc**。
      - Skill: bug-diagnosis-prompt
- [x] <Fix> currencyId 解析化：`ErpFinIntercompanyTransferBizModel` L127/L177 双路径改为经账套本位币解析（镜像 fin3-005 范式）；`ErpFinConsolidationEliminationPostEliminationProcessor` L107/L131 抵销行币种同范式接入。行为不变式：恒等部署解析结果 = "1"，既有测试全绿。
      - Skill: bug-diagnosis-prompt
      - 注记（2026-09-10）：落地 = `resolveOrgCurrencyId`（TransferBizModel 新增 private，fromLegal/sellerLegal 实参）+ `resolveCandidateCurrencyId`（PostEliminationProcessor protected，candidate.getOrgId() 实参）；恒等回退 "1" 均经注记。验证：TestErpFinIntercompanyEliminationDimension 4/4 绿（含恒等控制组）+ TestErpFinIntercompanyTransfer 7/7 + TestErpFinIntercompanyMatchingAndElimination 8/8 + PropertyErpFinConsolidationElimination 5/5 = 24/24 BUILD SUCCESS。
      - Skill: bug-diagnosis-prompt
- [x] <Fix> acctSchemaId 解析化：按 Decision 方案落地 `resolveOrgAcctSchemaId` 真实解析 + `PostEliminationProcessor` 凭证头 L84/行 L111/L135 账套取值；successor 注记同步收敛（登记面闭合注记，不改其裁决语义）。
      - Skill: bug-diagnosis-prompt
      - 注记（2026-09-10）：`resolveOrgAcctSchemaId` 改真实解析（null→"1" 回退），原「多账套精确解析归 successor」注记改为收敛声明（语义闭合，不改 r1 登记裁决）；PostElimination 头/行三站统一取 `resolveCandidateAcctSchemaId`。grep 复核归零：`currencyId = "1"|setCurrencyId("1")|setAcctSchemaId("1")` 全 fin main 0 命中；两修复类余留 `"1"` 字面量仅恒等回退 return 与 `"1".equals` 守卫，逐处带注记（Exit Criteria 满足）。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [x] Phase 1 非恒等场景测试全绿（解析值断言通过）+ 恒等控制组与既有 fin 套件保持绿
- [x] Decision 三要素（选择/替代/残余风险）注记在案；owner doc 对账结论在案；硬编码站点 grep 复核归零（`"1"` 字面量仅存于恒等回退/常量定义处且经注记说明）

## Phase 3 — 批级证明与收官门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [x] <Proof> 域级回归：`mvn test -pl module-finance/erp-fin-service -am` 全绿零新增失败（参照 533/0/0/0，数字记入注记；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）。
      - Skill: none
      - 注记（2026-09-10）：`mvn test -pl module-finance/erp-fin-service -am` = **537/0/0/0 BUILD SUCCESS**（基线参照 533/0/0/0 + 4 = 本计划新测试类 4 方法，零新增失败；修复后二次复跑同数，含 Phase 2 重构后复验）。
      - Skill: none
- [x] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行，surefire 口径真值 3991/0/0/1）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（`git ls-files` 同路径非空集，证明构成）+ `mvn clean install -DskipTests` BUILD SUCCESS。
      - Skill: none
      - 注记（2026-09-10）：①全 reactor `mvn test` **BUILD SUCCESS**，surefire 报告聚合 **3997/0/0/1**（基线 3991/0/0/1 + 4 本计划新测试 + 2 姊妹计划 M2.3 测试增量，已披露；失败/错误零新增，skip=1 持平）。②compliance checker **exit 0 全 19 规则逐条持平机器块**（R2a=34/R2b=242/R2c=1542/R2d=38/R12a=71/R12c=42，其余=基线）——过程披露：初版修复体直连 `daoFor(ErpMdAcctSchema)` 曾致 R2a+1/R2b+1/R2c+2/R12c+2 四规则漂移，按 fin3-005 修复体真实形态重构归零（md-dao `AcctSchemaResolver` 增 `resolvePrimarySchema` 实体级解析方法 net-zero daoFor、fin 侧经 FQN 共享解析器调用零新增 daoFor/import，镜像 `ErpFinBudgetCommitmentBizModel` L174 既有形态），复跑全 19 规则持平，无需独立基线裁决。③CJK `--strict` **PASS exit 0**（0 new violations vs 冻结快照，170 baseline files，totals CAT1..4=0/0/209/1318 持平）。④seed 零改动：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空 + `git ls-files` 同路径 373 文件非空集。⑤`mvn clean install -DskipTests` **BUILD SUCCESS**（156 reactor 模块，重构后复跑）。
      - Skill: none
- [x] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P3-CK-fin4-024-r3 fixed 证据指针（测试类/方法 + 解析站点清单）；config 投产升 P1 触发条件显式登记（任一通道 config 启用即触发独立裁决）；seed/ORM/api.xml 零触碰声明（`git status --porcelain` 全树无 `module-*/model/`、`*.api.xml`、`_init-data/`、页面命中）。
      - Skill: none
      - 注记（2026-09-10，M2.9 消费证据三件）：**①fixed 证据指针**——测试类 `module-finance/erp-fin-service/src/test/java/app/erp/fin/service/intercompany/TestErpFinIntercompanyEliminationDimension.java`（4 方法：testTransferChannelResolvesDimensionsInNonIdentityDeployment / testTradeDocumentChannelResolvesDimensionsInNonIdentityDeployment / testEliminationChannelResolvesDimensionsInNonIdentityDeployment / testIdentityDeploymentKeepsLegacyDimensions，先红后绿全链在案）；解析站点清单（修复后实态）——`ErpFinIntercompanyTransferBizModel.resolveOrgAcctSchemaId/resolveOrgCurrencyId/resolveOrgAcctSchema`（onTransferConfirmed fromLegal/toLegal + onTradeDocumentApproved sellerLegal/buyerLegal 双路径 6 消费点）、`ErpFinConsolidationEliminationPostEliminationProcessor.resolveCandidateAcctSchemaId/resolveCandidateCurrencyId/resolveCandidateAcctSchema`（凭证头+借贷两行 5 写点）、共享解析器 `app.erp.md.dao.AcctSchemaResolver.resolvePrimarySchema`（md-dao 新增实体级方法，`resolvePrimarySchemaId` 委托实现 net-zero）；grep 复核 `currencyId = "1"|setCurrencyId("1")|setAcctSchemaId("1")` 全 fin main 0 命中。**②config 投产升 P1 触发登记**——`erp-fin.intercompany-posting-enabled` / `erp-fin.consolidation-elimination-enabled` 任一投产启用（application.yaml/ErpSysConfig 置 true）即触发 fin4-024-r3 升 P1 独立裁决（本批不改默认值，生产默认 false 经 `ErpFinConstants` L488/L490 实核）。**③零触碰声明**——`git status --porcelain` 全树命中仅：2 个 fin service 主代码文件（计划 Targets 内）+ 1 个 md-dao 解析器文件（净零使能重构，上注记披露）+ 1 测试类 + 1 `_cases` 快照目录（姊妹测试类同型约定）+ 4 个 plan 文档（本组三计划含未执行姊妹批）；`module-*/model/*.orm.xml`、`*.api.xml`、`_init-data/`、页面（view.xml/page.yaml）零命中。
      - Skill: none

Exit Criteria:

- [x] fin service 全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案
- [x] M2.9 消费证据三件（fixed 指针 / config 触发登记 / 零触碰声明）落盘于计划注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-0705-1-m22-finance-intercompany-dimension-fix-1-b87f9fad to 2026-09-09-210030-mission-driver
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-0705-1-m22-finance-intercompany-dimension-fix-1-b87f9fad（零 Blocker 零 Minor 修订——基线五站点+行号 HEAD 实核逐一命中（`ErpFinIntercompanyTransferBizModel` L127/L177 `currencyId="1"` + L263-265 `resolveOrgAcctSchemaId` 恒 "1" 带 successor 注记；`ErpFinConsolidationEliminationPostEliminationProcessor` L84/L111/L135 `setAcctSchemaId("1")` + L107/L131 `setCurrencyId("1")`）；finding 记录/config 键名/known-good-baselines 2026-09-09 行/M1.17 口径裁决（3991 真值 + fin=533 锚点）实仓一致；技能 `bug-diagnosis-prompt` 注册在案且 roadmap M2.2 行指定；Decision 三要素（推荐 (a)/替代 (b)(c)/残余风险）在案且 owner doc 对账步骤在位；Phase Exit Criteria 可观察可验证；命名/frontmatter/章节结构与批内已审 M2.0 ledger 先例一致；frontmatter `status: draft` → `active`）

## Verification

- 2026-09-10 闭包审计独立复跑 **pass**（auditor 新会话）：`mvn clean install -DskipTests` exit 0 BUILD SUCCESS（156 reactor 模块，01:41 min）；全 reactor `mvn test` exit 0 BUILD SUCCESS，surefire 报告逐 XML 聚合 **3997/0/0/1**（与计划 Phase 3 记录口径精确一致：基线真值 3991 + 本计划新测试 4 + 姊妹计划 M2.3 增量 2，失败/错误零新增，skip=1 基线预存）。
- 2026-09-10 语义审计 **pass**：硬编码 grep `currencyId = "1"|setCurrencyId("1")|setAcctSchemaId("1")` 全 fin main 0 命中；解析链运行时在位（TransferBizModel L125-129/L176-180 双路径消费 `resolveOrgAcctSchemaId`/`resolveOrgCurrencyId` → md-dao `AcctSchemaResolver.resolvePrimarySchema`，PostElimination 凭证头+借贷两行 5 写点消费 `resolveCandidate*`）；新测试类本 visit surefire 实证 4/4 绿（含恒等控制组）；anti-hollow/deferred honesty/docs sync（roadmap M2.2 行执行证据 + 每日日志）无阻塞。
- pass test 20260910-0855 exit=0

## Closure

- dispatch audit #audit-20260910-0855-2026-09-10-0705-1-m22-finance-intercompany-dimension-fix-1-56992459 to 2026-09-09-210030-mission-driver models={exec:glm-5.3-flash,aud:glm-5.3-flash}
- accepted #audit-20260910-0855-2026-09-10-0705-1-m22-finance-intercompany-dimension-fix-1-56992459：独立闭包审计通过——P3-CK-fin4-024-r3 修复落地成立（intercompany 配对凭证与合并抵销凭证通道 currencyId/acctSchemaId 经账套解析链取值，"1" 硬编码 grep 全 fin main 归零，`resolveOrgAcctSchemaId` successor 注记收敛）；审计新会话复跑全绿：`mvn clean install -DskipTests` BUILD SUCCESS + 全 reactor `mvn test` BUILD SUCCESS（surefire 聚合 3997/0/0/1 零新增失败，skip=1 持平基线）；14/14 检查项全勾，Exit Criteria 与 live repo 逐项实核一致（解析站点/恒等回退注记/共享解析器 net-zero 形态），语义审计（anti-hollow/deferred honesty/docs sync）无阻塞发现，frontmatter `status: active` 保持（ledger 协议，completion 由引擎派生）。
