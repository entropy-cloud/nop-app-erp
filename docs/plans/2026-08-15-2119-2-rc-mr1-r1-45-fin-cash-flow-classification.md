# 2026-08-15-2119-2-rc-mr1-r1-45-fin-cash-flow-classification RC-R1.45 — finance 现金流量三分类 + 间接法（MR1 第二批 A 类 ORM 批量授权）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.45（P1-RC-007 finance 现金流量表 经营/投资/筹资 三分类 + 间接法完全缺失）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.45 行 + `docs/audits/arm-index.md` P1-RC-007 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 A 类：ErpMdSubject 加 cashFlowType 列**）
> Related: `docs/design/finance/use-cases.md`（L1 UC-FIN-16 断言⑧，:334-336）；`docs/design/finance/`（无专属现金流 owner doc——设计参考 `multiple-accounting-schemas.md` §账套查询与报表 + `posting.md`）；`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（A1.7 报告 P1-RC-007 新建）；`docs/plans/2026-08-15-1838-2-finance-bank-recon-counterparty-dimension.md`（A 类 ORM 批量授权执行先例）
> Audit: required

## Current Baseline

- **finding P1-RC-007（arm-index 行，UC-FIN-16 断言⑧）**：L1（`use-cases.md:334-336`）逐字「按科目现金流分类(经营/投资/筹资)调整 / 间接法: 净利润 + 非现金项目 + 营运资金变动」。L3 实仓（HEAD 核查）：
  - `ErpFinReportBizModel.buildCashFlowDataset:299-323`（`module-finance/erp-fin-service/.../report/ErpFinReportBizModel.java`）——现金科目判定 `isCashSubjectCode:549-553`（1001/1002/1012/1031 前缀硬编码）后 **`:314` `r.put("section", "OPERATING")` 硬编码全部为 OPERATING**——投资/筹资分类完全缺失；
  - **间接法（净利润 + 非现金项目 + 营运资金变动）完全缺失**（数据源上 `loadPostedVoucherLines:424-439` 仅加载已过账凭证行——docStatus=POSTED :427 + periodId :429 + org/schema scope :430，**无 postingType 过滤**（此缺口归 P2-RC-085 不同控制点，Non-Goal））；
  - `ErpMdSubject` ORM（`app-erp-master-data.orm.xml:901-935`）24 列（业务 1-17 + 审计 18-23 + remark=24），**无 `cashFlowType`/`cashFlowClass` 字段**——数据模型不支持按科目现金流分类；**propId 25 空闲**；
  - 报告 DTO 行结构：`section/code/name/direction/amount`（:313-318），模板 `cash-flow-statement.xpt.xml`（`module-finance/erp-fin-service/src/main/resources/_vfs/nop/main/report/fin/`）单 section 渲染 + 现金净增加合计（:62-65）；
  - 全仓 grep `cashFlowType|CASH_FLOW_|现金流分类|间接法|OPERATING|INVESTING|FINANCING`（排除 docs/_cases）→ 零业务实现命中（仅 `ErpFinConstants.CASH_FLOW_INFLOW/OUTFLOW` :271-272 流向常量）。
- **A1.7 报告（2026-08-02，新建 P1-RC-007）**：§2 P1①（功能实质偏离验收标准——分类维度 2/3 缺失 + 间接法缺失，非边界场景）；修复方向 = `ErpMdSubject` ORM 增 `cashFlowType` 字段[经营/投资/筹资/非现金] + `buildCashFlowDataset` 按科目 cashFlowType 分类 + 间接法补充；**触及 ORM 结构变更须 ask-first + 独立 plan-audit**。
- **预授权判据（2026-08-12 批量裁决 A 类）**：ORM 结构变更（`ErpMdSubject` 加 `cashFlowType` 列，纯加性）**批量授权**（对齐 Q3 纯加性类自动执行范围：加列不改既有语义、无 NOT NULL 无默认列、无既有数据 UK 增设、无删除/迁移/索引改造；**越界回落双独立子 agent 批准**——若执行中发现需改既有语义/加 UK/迁移 → 暂停并双独立子 agent 批准）；分类/间接法逻辑属代码逻辑修复预授权（报告读侧 BizModel，非会计过账写入路径，roadmap 预授权声明「代码逻辑修复（BizModel…）：预授权自动执行」）。roadmap RC-R1.45 行 `todo`，Deps（R1.0 done）已满足。**仍需独立 plan-audit**（ORM 变更 + 报表行为变更的标准义务）。**注：roadmap RC-R1.45 行（:437）仍载旧「须…双独立子 agent 批准 checkbox」字样，2026-08-12 批量裁决 A 类已覆盖（对齐 RC-R1.43 行改写先例）——本计划按裁决执行，Phase 4 回填时同步改写行字样消除歧义**。
- **测试基线**：`TestErpFinReportRendering#testCashFlowDataset:162-173`（仅断言现金流入额 80，**无分类断言**——A1.7 报告 L4 缺口）；`TestErpFinMultiSchemaReportIsolation`（schema 隔离范式）；`TestErpFinBudgetIsolation`（BUDGET/COMMITMENT 通道范式，间接法须排除影子凭证——P2-RC-085 边界）。
- **涉及文件**：`module-master-data/model/app-erp-master-data.orm.xml`（ErpMdSubject 加 cashFlowType 列）；`ErpFinReportBizModel.java`（buildCashFlowDataset 分类 + 间接法）；`ErpFinConstants.java`（分类/间接法常量，按需）；`cash-flow-statement.xpt.xml`（模板 section 渲染）；`ErpMdSubject` 种子/测试数据（新增分类断言）；测试（`TestErpFinReportRendering` 扩展）；owner doc（`use-cases.md` 契约段不动 + 报表设计注记落 `docs/design/finance/` 或既有 `multiple-accounting-schemas.md`）+ arm-index/roadmap/`docs/logs/`（回填）。

## Goals

- **三分类运行时成立（P1-RC-007 核心）**：`ErpMdSubject` 增 `cashFlowType` 列（纯加性，dict `erp-md/cash-flow-type`：OPERATING/INVESTING/FINANCING/NON_CASH，默认值语义按 Phase 1 裁决）→ `buildCashFlowDataset` 按科目 `cashFlowType` 分类（`section` 从硬编码 OPERATING 改为读 subject.cashFlowType），经营/投资/筹资三分类各自聚合——消除「分类维度 2/3 缺失」。
- **间接法运行时成立**：`buildCashFlowDataset`（或姊妹数据集）补充间接法口径——净利润 + 非现金项目（折旧/摊销/坏账准备等 NON_CASH 科目调整）+ 营运资金变动（流动资产/负债科目期初期末变动），数据源与口径按 Phase 1 裁决（VoucherLine 聚合 vs GlBalance 期初/期末差额，见 D3）。
- **分类数据完备**：种子/测试数据的 ErpMdSubject 补 `cashFlowType` 分类值（1001/1002/1012/1031 现金科目 + 主要损益/资产科目），`isCashSubjectCode` 与 cashFlowType 判定关系按 Phase 1 裁决收敛（现金科目判定继续以 code 前缀为准 or 改以 cashFlowType 为准——不破坏既有行为）。
- **测试**：① 三分类断言（经营/投资/筹资各聚合值正确——对照现状全 OPERATING）；② 间接法断言（净利润+非现金+营运资金变动公式值正确）；③ 既有 `testCashFlowDataset` 现金流入额断言零回归（分类后总额不变）；④ 无分类值科目回退行为断言（默认 OPERATING 或按裁决）；⑤ 影子凭证（BUDGET/COMMITMENT）不污染断言（P2-RC-085 边界——间接法聚合过滤 postingType 或显式声明 Non-Goal）。
- **零回归**：既有 finance 测试全绿 + 全仓构建 + compliance checker 零漂移（ORM 1 加列属 Q3 纯加性授权；零新增 daoFor/import 面——分类经 ORM to-one getter `line.getSubject().getCashFlowType()` 或 subjectId 查询，不新增跨域 daoFor 站点，见 Phase 1 Proof）。
- **owner doc 收敛**：报表设计注记（三分类 + 间接法实现口径 + cashFlowType dict 语义）落 `docs/design/finance/`（或既有报表文档）；不修改需求契约段（use-cases L1 不动）。
- **回填**：arm-index P1-RC-007 → `done (RC-R1.45)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P2-RC-008（CLOSED 期间门控）**（独立 P2 登记项，`loadGlBalances:386-413` 无 status 门控——不同控制点）。
- **不实现 P2-RC-085（cash flow 读路径 postingType 过滤）**（独立 P2 watch-only——直接法当前运行时无污染[A4.1.22 实证 BUDGET/COMMITMENT 科目分布非现金类]；**间接法聚合侧例外：D3 子裁决已纳入 postingType notIn(BUDGET,COMMITMENT) 过滤**——因 6601/6001 等损益类科目确被影子凭证写入，净利润聚合必须排除，属本行间接法正确性内在要求，非 P2-RC-085 范围扩张）。
- **不实现 P1-RC-091（试算平衡 COMMITMENT 排除）**（独立 finding，属 RC-R1.46 已 done）。
- **不改 Voucher/VoucherLine 结构**（postingType 在 Voucher 头 :418，间接法所需数据源用既有字段）。
- **不实现完整 IAS7 直接法细化**（现有直接法行结构保留，本行补分类 + 间接法两维度）。
- **不改真相源契约段落**（use-cases L1 不动；报表设计注记只补实现口径）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 纯加性 1 列[A 类批量授权] + 报表读侧分类/间接法逻辑[预授权]；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/finance/use-cases.md`（L1 UC-FIN-16 断言⑧）+ `docs/design/finance/`（报表设计注记落点，无专属现金流 owner doc——落 `multiple-accounting-schemas.md` 或新增注记段）
- Skill Selection Basis: 实现面 = ORM 模型变更（`nop-backend-dev`：orm.xml 模型优先 + 增量重生成）+ 报表 BizModel 分类/间接法（`nop-backend-dev`：读侧聚合 + FilterBeans + 平台辅助工具）；模板渲染（`nop-frontend-dev`：xpt.xml section 渲染，按 D4 裁决）；测试（`nop-testing`：JunitBaseTestCase 直断言 + 种子分类数据）。无 xbiz/Processor/过账路径变更。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务（分类语义走 ErpMdSubject.cashFlowType 数据，非 config）。
- ORM 变更触发增量重生成：`mvn clean install -DskipTests`（gen-orm.xgen 增量链，对齐 AGENTS.md）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-finance/erp-fin-service` + `mvn test -pl module-master-data/erp-md-service`（ORM 变更波及 master-data 生成链）。

## Execution Plan

### Phase 1 - 分类语义与数据源裁决（Decision）

Status: completed
Targets: `ErpFinReportBizModel.java`（buildCashFlowDataset/loadPostedVoucherLines）；`ErpMdSubject` ORM；`app-erp-master-data.orm.xml`；测试 `TestErpFinReportRendering`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **D1 分类语义（cashFlowType 值域 + 默认行为）**：**选项 A（选定）** = dict `erp-md/cash-flow-type` 四值 OPERATING/INVESTING/FINANCING/NON_CASH，`cashFlowType` 列**可空无默认**，`buildCashFlowDataset` 分类读 `subject.cashFlowType`，**null/非现金科目回退 OPERATING**（保持既有全 OPERATING 行为为零回归基线）；**选项 B（否决）** = 加默认值 OPERATING（column defaultValue——改变种子/既有数据物化行为，且「非现金科目默认经营」语义混淆）；**选项 C（否决）** = 值域仅三分类无 NON_CASH（间接法需要非现金调整科目分类，NON_CASH 承载折旧/摊销/坏账等）。**理由**：L1 字面「经营/投资/筹资」三分类 + 间接法需要「非现金项目」调整维度 → 四值语义完整；null 回退 OPERATING 保持零回归；dict 新增为数据非结构变更（RC-R1.88 先例）。**残留风险**：科目未分类时归经营可能误分类——种子补分类 + 后续数据治理义务（报告注记声明）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D2 现金科目判定收敛**：**选项 A（选定）** = `isCashSubjectCode` 前缀判定保留为现金科目准入门槛（1001/1002/1012/1031），`cashFlowType` 只作**分类**（section）维度——行为最小变化；**选项 B（否决）** = 现金判定改以 `cashFlowType=OPERATING/INVESTING/FINANCING` 为准（语义更数据驱动，但改动现金科目准入判定面 + 种子现金科目分类缺失时行为漂移）。**理由**：L1 断言⑧「按科目现金流分类」指分类维度，现金科目准入（哪些科目进表）维持既有硬编码判定零回归。**残留风险**：非标准现金科目（自定义前缀）无法进入现金流表——数据驱动准入重构登记 Deferred But Adjudicated（触发条件：科目体系定制化需新增非标准现金科目）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D3 间接法数据源与口径**：**选项 A（选定）** = 基于 VoucherLine 聚合（复用 `loadPostedVoucherLines` 已加载数据——净利润 = 损益类科目 net 聚合[INCOME: credit−debit / EXPENSE+COST: debit−credit，经 businessType 排除 PERIOD_CLOSE 结转凭证，对齐 `ProfitLossClosingService:84-107` 范式] + 非现金项目 = subject.cashFlowType=NON_CASH 科目 net 聚合[同损益净额符号约定，折旧/减值等加回] + 营运资金变动 = 流动资产/负债科目 net 聚合[定义：ASSET 类且 direction=DEBIT 且非现金前缀（1001/1002/1012/1031）且非 160x 非流动资产前缀 ∪ LIABILITY 类且 direction=CREDIT，调整额 = Σ(credit−debit)——资产增加减现金流/负债增加加现金流，CREDIT 方向资产（1231 坏账准备/1602 累计折旧/1604 减值准备）经 direction 规则天然排除防与 NON_CASH 加回重复计算]；三组数据同一数据源同口径）；**选项 B（否决）** = 基于 `ErpFinGlBalance` 期初/期末差额（`loadGlBalances:386-413`，与 BS/IS 同源——但 GlBalance 当前由年度结转 populate，期初/期末差额在月中口径缺失，且 P2-RC-008 CLOSED 门控缺口波及）。**理由**：VoucherLine 是现金流水同源（buildCashFlowDataset 已读），间接法三组件在同一期间窗口内聚合口径自洽；GlBalance 路径受 P2-RC-008 波及且月度差额口径不完整。**间接法聚合 postingType 过滤（D3 子裁决，MAJOR-1 修复）**：净利润/非现金/营运资金三组件聚合的**损益类科目净额**（6601 销售费用/6001 主营业务成本等损益类科目）会被 BUDGET/COMMITMENT 影子凭证行污染（A4.1.22 实证 BUDGET 凭证 ID=12 行=6601+6001，同为损益类科目）——间接法聚合**必须**在 voucher 头侧过滤 `postingType notIn(BUDGET, COMMITMENT)`（vq 查询层 `or(isNull, notIn(BUDGET,COMMITMENT))` 对齐 RC-R1.46 已落地模式，属读侧过滤预授权自动执行）；直接法（isCashSubjectCode 准入）保持既有行为（影子凭证科目分布非现金类，A4.1.22 实证无污染）。**残留风险**：间接法净利润与利润表口径差异（结转凭证排除 + 影子凭证排除）须在报告注记声明。
      - Skill: `nop-backend-dev`
- [x] `Proof` **数据面核实**：①种子 ErpMdSubject 全集 44 行（`app-erp-all/_vfs/_init-data/erp_md_subject.csv`，ID 1-45 缺 22 号）分类清单产出：现金科目 2 行（1001 库存现金/1002 银行存款 → OPERATING；**1012/1031 生产种子不存在**——与 draft review iteration 3 非门控观察一致，测试面补 1012/1031 作 INVESTING/FINANCING 示例[isCashSubjectCode 前缀准入成立]）；损益类收入 2 行（5001 主营业务收入/6301 营业外收入 → OPERATING）；损益类成本 3 行（6001 主营业务成本/6401 主营业务成本/5101 项目成本 → OPERATING）；损益类费用-经营性 4 行（6601 销售费用/6711 营业外支出-报废损失/6603 财务费用-利息支出/6051 汇兑损益 → OPERATING）；损益类费用-非现金 3 行（**6602 折旧费用/6701 信用减值损失/6702 资产减值损失 → NON_CASH**）；权益类 2 行（4103 本年利润/4002 资本公积 → **显式 OPERATING**——功能惰性[直接法 code 前缀准入 + 间接法三组件不含权益]但显式标注对齐 null 回退语义，否决 NON_CASH 标注[与 P&L 非现金调整混淆]；2211 应付职工薪酬属负债类非权益，归营运资金变动组件）；资产类 21 行 + 负债类 7 行 → OPERATING（营运资金组件经 direction/前缀规则运行时判定，cashFlowType 对资产负债类功能惰性）；②`loadPostedVoucherLines:424-439` 数据面确认：ErpFinVoucherLine 实体无 postingType 列（`app-erp-finance.orm.xml:508-509` voucher/subject to-one 关系实证），影子凭证边界经 voucher 头侧 vq 过滤（D3 子裁决，`postingType` 在 ErpFinVoucher 头 orm.xml:418）；③`TestErpFinReportRendering` 种子/断言结构确认（`seed():67-98` / `testCashFlowDataset:162-173` / `seedSubject:281-291` / `seedPostedVoucherWithCashLine:313-362`）——**基础 seed() 保持 cashFlowType null 零改动**（既有 6 方法 `_cases` 快照零重录：CHECKING 比对仅遍历录制 CSV 既有列，新列不参与比对——AutoTestCaseResultChecker.checkRowMatch:142-165 实证）；新断言用独立测试方法 + 直断言范式（`TestErpCtContractCreateValidate` 无快照先例）+ 科目编码规避（新增断言用 5001/1012/1031/6602/1602/1122/1405 等独立编码，禁 4103 撞生产语义）；④**零新增 daoFor 声明**：分类经 `line.getSubject()` to-one 惰性加载（候选集窄小），间接法聚合仅消费已加载的 VoucherLine 内存数据 + voucher 头 postingType（vq 查询层过滤 = voucherIds 集合维度，复用既有 `daoProvider.daoFor(ErpFinVoucher.class)` 站点，无新增跨域 daoFor 站点——R2c 零漂移）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D3 裁决记录落盘计划（选择 + 备选 + 理由 + 残留风险），种子分类值清单 + 数据面核实证据产出
- [x] propId 25 空闲实证 + 生成链机制确认（RC-R1.43/R1.40 先例）：`app-erp-master-data.orm.xml:907-931` ErpMdSubject 列 propId 1-24（业务 1-17 + 审计 18-23 + remark 24）**25 空闲无重复**；生成链 = `mvn clean install -DskipTests` 触发 gen-orm.xgen 增量链（XMeta/_app.orm.xml/DDL 三同步，RC-R1.40 先例实证）

### Phase 2 - ORM cashFlowType 列 + dict + 种子分类（A 类授权变更）

Status: completed
Targets: `module-master-data/model/app-erp-master-data.orm.xml`（ErpMdSubject）；dict `erp-md/cash-flow-type`；种子数据（`_init-data/erp_md_subject.csv`）
Skill: `nop-backend-dev`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1 D1 裁决

- [x] `Add` `app-erp-master-data.orm.xml` ErpMdSubject 增 `cashFlowType` 列（propId 25，`ext:dict="erp-md/cash-flow-type"`，VARCHAR precision 20，**可空无默认无索引无 UK**——Q3 纯加性授权范围；⚠ propId 不可占用 18-23 审计列 + remark=24，显式声明 25 防漂移，对齐 RC-R1.43 先例）。
      - Skill: `nop-backend-dev`
- [x] `Add` 新 dict `erp-md/cash-flow-type.dict.yaml`（OPERATING 经营/INVESTING 投资/FINANCING 筹资/NON_CASH 非现金——D1 选项 A 值域；对齐既有 dict 文件模式 `module-master-data/erp-md-meta/.../dict/erp-md/`；**`posted-status.dict.yaml` 非 orm.xml `<dicts>` 手写 dict 先例**——dict 属运行时数据非结构变更，零 orm.xml `<dicts>` 声明，codegen 不生成 `CASH_FLOW_TYPE_*` 常量[实证 `_ErpMdDaoConstants.java` 零命中]，常量按需落 `ErpFinConstants`）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 种子数据 `_init-data/erp_md_subject.csv` 补 `CASH_FLOW_TYPE` 列 + 分类值（1001/1002/1012/1031 → OPERATING；损益类按 D3 聚合语义标注[5001 主营业务收入/6001 主营业务成本/6601 销售费用 → OPERATING、6602 折旧 → NON_CASH 等，**以生产种子实际科目名为准**]；权益/负债类按 Phase 1 Proof 清单策略标注或 null 回退——**追加列并逐行补充分类值，不改既有列值/不删行**（种子只能追加纪律，每行仅新增一个 cell））；`TestErpFinReportRendering` 种子 helper（`seedSubject:281-291`）同步 cashFlowType 参数（测试内不依赖 CSV）。
      - Skill: `nop-backend-dev`
- [x] `Proof` 增量重生成验证：`mvn clean install -DskipTests` 生成链通过（Entity/xmeta/DDL 三同步）+ propId 分配核对 + 分域编译通过。
      - Skill: `nop-testing`

Exit Criteria:

- [x] ORM 列 + dict + 种子分类落地（orm.xml/Entity/xmeta/DDL 四同步 grep 核对 + dict 文件存在 + CSV 追加列），`mvn clean install -DskipTests` 生成链通过

### Phase 3 - 三分类 + 间接法实现（P1-RC-007 核心）

Status: completed
Targets: `ErpFinReportBizModel.java`（buildCashFlowDataset + 间接法数据集）；`ErpFinConstants.java`（按需常量）；`cash-flow-statement.xpt.xml`（模板 section 渲染）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 2 完成

- [x] `Fix` `buildCashFlowDataset` 分类：`:314` 硬编码 `OPERATING` 改为按 `l.getSubject().getCashFlowType()` 分类（null/非现金回退 OPERATING——D1 选项 A；section 值 = OPERATING/INVESTING/FINANCING，NON_CASH 科目不进直接法行[现金科目准入由 isCashSubjectCode 保证，NON_CASH 判定在间接法侧消费]）；section/code/name/direction/amount 行结构不变。
      - Skill: `nop-backend-dev`
- [x] `Add` 间接法数据集（D3 选项 A + 子裁决）：复用 `loadPostedVoucherLines` 加载数据内存聚合——**聚合级 postingType 过滤（D3 子裁决，三组件共享）**：净利润/非现金/营运资金三组件基于同一已加载凭证集聚合，voucher 头侧 `postingType notIn(BUDGET, COMMITMENT)` 过滤对**三组件统一生效**（voucherIds 集合维度过滤，非仅净利润子句——避免窄实现）；**净利润**（损益类科目 net，businessType != PERIOD_CLOSE 过滤）+ **非现金项目**（subject.cashFlowType=NON_CASH 科目 net）+ **营运资金变动**（应收/应付/存货/预付/预收类科目 net，按 Phase 1 清单）；产出间接法 section 行或独立间接法块（数据结构按 D1/D3 裁决落定，`cashFlowStatementData` 返回契约扩展或姊妹方法）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D4 模板与返回契约形态**（执行期裁决，落盘记录）：**选项 A（选定）** = `cashFlowStatementData` 返回结构扩展 section 值域（OPERATING/INVESTING/FINANCING + 间接法键），**模板侧按数据集拆分渲染**——`prepareDataset` 对 cash-flow-statement 注入 `ds`（直接法行，section 排序 OPERATING→INVESTING→FINANCING）+ `indirectDs`（间接法行，section=INDIRECT + 组件键 NET_PROFIT/NON_CASH/WORKING_CAPITAL/NET），直接法块与「现金净增加（直接法）」SUM 合计零改动（既有断言零回归），新增间接法补充块；新增 `indirectCashFlowData` @BizQuery 姊妹方法供前端独立取数；**选项 B（否决）** = 独立模板块仅经姊妹方法注入（单一返回契约仍为 D4 选项 A 优先，前端/模板改动最小）。**与 D1（section 值域）/D3（数据源）关系**：D4 只定返回结构承载形态，不重定义分类语义/数据源——D1/D3 先行裁决，D4 在 Phase 3 首项落定后序项按之执行。
      - Skill: `nop-frontend-dev`
- [x] `Add` 模板渲染（按 D4 裁决）：`cash-flow-statement.xpt.xml` section 按分类分组渲染（经营/投资/筹资各段 + 间接法块）——若 D4 裁决模板最小改动则仅加 section 分组列/块。落地 = 直接法块（活动分类列渲染 OPERATING/INVESTING/FINANCING）+ 间接法补充块（`indirectDs` 展开：code/name/direction/amount），新增 section 样式。
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] 三分类 section 落地（grep 证据：无硬编码全 OPERATING）+ 间接法聚合实现 + 模板渲染核对（分域渲染冒烟）
- [x] 既有 `testCashFlowDataset` 现金流入额断言零回归（分类后总额不变）

### Phase 4 - 测试 + 零回归 + 文档回填

Status: completed
Targets: `TestErpFinReportRendering`（扩展）；owner doc 报表注记；arm-index/roadmap/`docs/logs/`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 3 完成

- [x] `Add` 测试扩展（`TestErpFinReportRendering` 或新测试类）：① 三分类断言（种子科目分类 OPERATING/INVESTING/FINANCING → section 聚合值正确）；② 间接法断言（净利润+非现金+营运资金变动公式值）；③ 既有现金流入额断言零回归；④ null cashFlowType 回退 OPERATING 断言；⑤ 影子凭证边界断言（间接法聚合含 BUDGET/COMMITMENT 凭证时净利润不含其损益行——D3 子裁决过滤实证；直接法不受污染）。**⚠ 科目编码冲突规避**：测试种子禁用 4103 作为「实收资本」（生产种子 4103=本年利润，`TestErpFinReportRendering` 既有 :74 亦用 4103 当实收资本——新增间接法断言用独立编码如 4104/4003 或按 subjectCode 语义明确命名，避免 UK_MD_SUBJECT_CODE 冲突与语义混淆）。
      - Skill: `nop-testing`
- [x] `Add` owner doc 注记：报表设计注记（三分类 + 间接法口径 + postingType 过滤子裁决 + cashFlowType dict 语义 + 种子分类义务 + 与 P2-RC-008/P2-RC-085 边界声明）落 `docs/design/finance/`（`multiple-accounting-schemas.md` 或既有报表文档补段）；`use-cases.md` 契约段不动。
      - Skill: `nop-backend-dev`
- [x] `Proof` 零回归验证：`mvn test -pl module-finance/erp-fin-service` + `mvn test -pl module-master-data/erp-md-service` 全绿 + **`_cases` 快照 CSV 重录/核验**（`erp_md_subject.csv` 列变更波及各域 `_cases` 输入/输出快照目录——对齐 RC-R1.40 先例「4 个 `_cases` output 快照 CSV 重录」，执行期 glob 定位受波及目录逐项重录）+ `mvn clean install -DskipTests` 全量构建 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（零新增 daoFor/import 面）+ 回填（arm-index P1-RC-007 → done (RC-R1.45) + roadmap 行 done + **roadmap RC-R1.45 行「须…双独立子 agent 批准 checkbox」字样改写为 A 类裁决框架，对齐 RC-R1.43 行先例** + `docs/logs/2026/08-15.md` 日志条目）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试全绿（①-⑤，科目编码冲突规避）+ 既有 finance/master-data 测试零回归 + `_cases` 快照重录核验完成
- [x] owner doc 注记 + 三处回填（arm-index/roadmap 行字样改写/log）+ compliance checker 零漂移

> **执行注记（2026-08-16）**：`_cases` 快照核验结论 = **零重录**——`erp_md_subject.csv` 加列后，CHECKING 模式快照比对仅遍历录制 CSV 既有列（`AutoTestCaseResultChecker.checkRowMatch:142-165` 实证），新列不参与比对，既有 404 output + 305 input 快照（全仓）经全量 `mvn test` BUILD SUCCESS 实证零回归；仅 `TestErpFinReportRendering` 新增 4 测试方法产生空脚手架目录（直断言范式，对齐 `TestErpCtContractCreateValidate` 未跟踪先例）。日志落 `docs/logs/2026/08-16.md`（执行期为 2026-08-16，按日志指南落当日文件，对齐 RC-R1.44 先例）。

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_ffa61b383ffemOOkupqtuLoc15`) — 1 MAJOR + 6 MINOR。MAJOR-1 已修正：**间接法影子凭证污染论证自相矛盾**——BUDGET/COMMITMENT 影子凭证科目 6601/6001 属损益类，会被净利润聚合吸收（原「无现金科目行」前提只保护直接法）→ D3 增子裁决：间接法三组件聚合必须经 voucher 头侧 `postingType notIn(BUDGET,COMMITMENT)` 过滤（对齐 RC-R1.46 模式，读侧过滤预授权），Non-Goals P2-RC-085 边界声明 + Deferred 条目同步改写，Phase 4 测试⑤改实证过滤。6 MINOR 已修正：(1) 补显式越界回落条款（改既有语义/加 UK/迁移 → 暂停并双独立子 agent 批准）；(2) roadmap RC-R1.45 行旧字样标注 + Phase 4 回填改写承诺；(3) Phase 4 补 `_cases` 快照 CSV 重录/核验项（RC-R1.40 先例）；(4) 4103 科目编码冲突规避注记（生产种子 4103=本年利润 vs 测试种子 4103=实收资本 → 新断言用独立编码）；(5) D4 裁决移到模板渲染项之前 + 与 D1/D3 关系界定；(6) 种子措辞精确化（追加列并逐行补值，不改既有列值/不删行）+ 权益类科目分类策略显式确认。其余 baseline 声明实仓核实 PASS（buildCashFlowDataset/isCashSubjectCode/loadPostedVoucherLines 行号、ErpMdSubject propId 25 空闲、模板路径、种子 CSV、测试结构、D3 选项 B 的 GlBalance 依据）。
- Independent draft review iteration 2: `needs revision` (`ses_ffa56b4bbffeTJc0YjYXm2GA63`) — MAJOR-1 + 6 MINOR 修正全部确认落地，但新增 2 个种子事实性 MINOR。已修正：(1) Phase 1 Proof 权益类枚举与生产种子不符——4002=**资本公积**（非实收资本，生产种子无实收资本科目）、2211=**应付职工薪酬属负债类**（归营运资金变动组件，非权益，删出「功能惰性」断言）；(2) Phase 2 种子项「6001 收入类」标签错误——生产种子 6001=**主营业务成本**（收入类为 5001），改「以生产种子实际科目名为准」。另吸收非门控观察：Phase 3 间接法 Add 项把 postingType 过滤明确为**三组件共享的聚合级过滤**（voucherIds 集合维度，防窄实现）。
- Independent draft review iteration 3: `acceptable` (`ses_ffa5020d4ffeMs6nETSz5wwLHz`) — 迭代 2 三项修正逐项复核确认（权益类枚举 vs CSV row 33/34/37 实证、6001 标签 + hedge、聚合级过滤措辞）；1 残留 MINOR（D3 子裁决同型「6001 收入类」标签未同步修正）已就地修正为「6001 主营业务成本」。非门控观察（1012/1031 现金科目种子不存在，仅 1001/1002 有行——Phase 1 Proof 数据面核实为执行期自然暴露点）记录备查。共识达成，计划可转 active。

## Closure Gates

- [x] 范围内行为完成（P1-RC-007 三分类 + 间接法 + ORM 列 + 测试）
- [x] 相关文档对齐（报表注记 + arm-index P1-RC-007 → done (RC-R1.45) + roadmap 行 done）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service,module-master-data/erp-md-service` + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 现金科目准入数据驱动化（isCashSubjectCode → cashFlowType 驱动）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D2 裁决现金科目准入维持 code 前缀硬编码（1001/1002/1012/1031）——分类维度（section）由 cashFlowType 承载已满足 L1 断言⑧；准入判定重构（数据驱动）改动现金科目准入面 + 种子依赖，风险大于收益
- Successor Required: `yes`（触发条件：科目体系定制化需新增非标准现金科目时，前缀硬编码成为扩展瓶颈）

### 影子凭证 postingType 过滤（P2-RC-085）

- Classification: `watch-only residual`
- Why Not Blocking Closure: A4.1.22 已登记 P2 watch-only——直接法（isCashSubjectCode 准入）当前运行时无污染（BUDGET/COMMITMENT 科目分布非现金类 + config 默认关闭）；**间接法聚合侧本行已自带 postingType notIn(BUDGET,COMMITMENT) 过滤（D3 子裁决，Phase 3 落地）——影子凭证不破坏净利润数值**；直接法读路径 `loadPostedVoucherLines:424-439` 的全局 postingType 过滤修复归独立后续
- Successor Required: `no`

### CLOSED 期间门控（P2-RC-008）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 独立 P2 登记项（`loadGlBalances` 渲染侧门控），本行间接法基于 VoucherLine 数据源不读 GlBalance；报表期间门控整体治理归 P2-RC-008
- Successor Required: `no`

## Closure

Status Note: 计划于 2026-08-16 执行完成并关闭。四 Phase 全部 `[x]` + `Status: completed`；D1-D4 裁决落盘（Phase 1/3，含 D3 子裁决影子凭证 postingType 过滤 + D4 双数据集模板形态）；A 类 ORM 批量授权变更（2026-08-12 裁决：ErpMdSubject.cashFlowType 纯加性 1 列）经独立 plan-audit（草案审查 3 轮收敛 acceptable）。验证全绿：`mvn test -pl module-finance/erp-fin-service` **497/497**（493 基线 + 4 新增，maven reactor 权威计数；surefire .xml 聚合 489 差值为 jqwik 容器级计数机制差异，复跑 `mvn clean test` 恒 497 实证）+ `mvn test -pl module-master-data/erp-md-service` **146/146** + 全量 `mvn test` BUILD SUCCESS（含 TestErpSeedDataIntegrity 种子完整性 + ErpAllFluxPagesTest/ErpAllWebPagesTest）+ `mvn clean install -DskipTests` 全量 BUILD SUCCESS + compliance checker **actual == baseline 零漂移**（R2c=1399 / R12a=69 / R12b=66 / R12c=40 精确一致——零新增 daoFor/import 面，分类经 `line.getSubject()` to-one）。`_cases` 既有快照零重录核验（CHECKING 仅比对录制列，全量 mvn test 实证）。回填完成：arm-index P1-RC-007 → done (RC-R1.45) + roadmap RC-R1.45 → done ✅（行字样改写为 A 类裁决框架，对齐 RC-R1.43）+ owner doc `multiple-accounting-schemas.md §账套查询与报表` 注记 + `docs/logs/2026/08-16.md` 日志条目（执行期为 2026-08-16，按日志指南落当日文件，对齐 RC-R1.44 先例）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计员（新会话，无执行者上下文），task `ses_ff9428f18ffeMtkwr9qLWMQGIi`
- Evidence: 独立结束审计 2026-08-16 执行——Gate 1 范围内行为 PASS（ORM `app-erp-master-data.orm.xml:932` cashFlowType propId=25 可空无默认无索引无 UK + 生成物四同步 `_ErpMdSubject.java:122/1358` + xmeta:123 + DDL 三方言 + dict yaml 四值 + 种子 44 行全填 + `ErpFinReportBizModel` 硬编码 OPERATING 已删（`:335` sectionOfCashFlowType）+ `buildIndirectCashFlowDataset:367-402` 三组件 + `loadPostedVoucherLines:512-531` vq 层 `or(isNull, notIn(BUDGET,COMMITMENT))` 过滤 + 模板 indirectDs 块 + 4 新测试方法全在 + `git diff` 新增行 daoFor 计数 0）；Gate 2 文档对齐 PASS（multiple-accounting-schemas.md:201 注记 + arm-index:143 done (RC-R1.45) + roadmap:437 done ✅ A 类字样 + log 08-16.md 顶部条目）；Gate 3 验证证据 PASS（TestErpFinReportRendering 12/0/0 + erp-md-service 146/0/0 + checker R2c=1399/R12a=69/R12b=66/R12c=40 与 compliance-baseline.md 精确一致 + git status 无 `_cases` 快照 CSV 修改[仅 4 新方法空脚手架目录符合仓库先例] + 四 Phase 全 [x] 无残留 [ ]）；Gate 4 无降级 PASS（Deferred 三项 pre-adjudicated 含 successor 触发条件）；Gate 5 草案审查 PASS（3 轮迭代记录，iter3 acceptable）；Gate 6 文本一致性 PASS；Gate 7 独立审计 PASS（本审计即独立子代理，执行者未自我审计，审计前 Closure Evidence 占位未填 + 审计门控保持 [ ] 实证）；Gate 8 结束证据 PASS。**总体裁决：PASS**（1 MINOR：erp-fin-service 测试总数声称 497 vs surefire .txt/.xml 聚合 489 差 8——已复跑 `mvn clean test` 恒得 reactor 摘要 497（= 493 基线 + 4 新增），差值归因 jqwik 容器级计数机制（surefire 控制台摘要计数含 jqwik property container，xml testsuite 属性不含），三处回填数字与 reactor 权威计数一致**无需修正**；1 观察：变更处于未提交工作树待仓库流程 commit，非本审计范围）。

Follow-up:

- 现金科目准入数据驱动化（isCashSubjectCode → cashFlowType 驱动）——Deferred But Adjudicated 已登记（successor yes，触发条件：科目体系定制化需新增非标准现金科目时）
- 影子凭证直接法侧 postingType 过滤（P2-RC-085 watch-only，间接法侧本行已自带过滤）
