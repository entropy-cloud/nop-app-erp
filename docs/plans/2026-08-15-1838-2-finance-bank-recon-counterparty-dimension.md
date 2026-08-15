# 2026-08-15-1838-2-finance-bank-recon-counterparty-dimension RC-R1.43 — finance 银行对账自动勾对对方账号维度（MR1 第二批 A 类 ORM 批量授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.43（P1-RC-004 finance 银行对账自动勾对对方账号维度缺失）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.43 行 + `docs/audits/arm-index.md` P1-RC-004 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 A 类：ORM 变更批量授权，ErpFinBankStatementLine 加 counterpartyAccount/counterpartyName/counterpartyBank 3 列**）
> Related: `docs/design/finance/use-cases.md`（L1 UC-FIN-09/14）；`docs/design/finance/bank-reconciliation.md`（§业务规则 2）；`docs/audits/2026-08-06-1044-rc-ma4-a4-1-11-bank-recon-counterparty-account-mismatch-rate.md`（A4.1.11 运行时触发率评估）；`docs/plans/2026-08-08-1603-2-rc-mr1-r1-12-commitment-restore-symmetry.md`（同批范式参照）
> Audit: required

## Current Baseline

- **finding P1-RC-004（arm-index 行，UC-FIN-09/14 断言② 自动勾对对方账号维度缺失）**：L1（`use-cases.md:172,279`）逐字「自动勾对: 金额 + 反向方向 + valueDate±N天 + **对方账号** 模糊匹配」——对方账号是 4 维度之一。L2（`bank-reconciliation.md §业务规则 2`：98）一致要求。L3 实仓（HEAD 核查）：`BankLedgerQuery.findCandidates:39-84` 候选过滤为 `subjectId + dcDirection + amount + voucherId(日期窗口)`（:62-72），**无 counterpartyAccount 过滤**；ORM `ErpFinBankStatementLine`（`app-erp-finance.orm.xml:1133-1173`）**无 counterpartyAccount/counterpartyName/counterpartyBank 列**。运行时影响：同额同日不同对方账号且账面仅 1 候选 → 错误 MATCHED（影响对账准确性，可经 manualMatch 取消可逆 + 余额恒等式下游兜底，故非 P0）。§2 P1①（行为实质偏离验收标准）。
- **A4.1.11 运行时评估（2026-08-06，维持 P1 不升 P0 不降 P2）**：触发面 CONFIRMED——`BankStatementMatcher.autoMatch:61-65` 单候选即自动 MATCHED（不区分对方账号），「同额同日不同 partner 且账面仅 1 候选」场景确实产生错误 MATCHED；触发率定性**中低**（需同额+同日窗口+同资金科目+反向方向+非对称候选多重前置巧合，非默认活跃路径）；P0 不成立（GL/过账解耦隔离 + 技术可逆——manualMatch 守卫拒绝已 MATCHED 行，修正需通用 CRUD 重置 matchStatus；余额恒等式对同额错配无效故不能作兜底依赖）。
- **实仓（HEAD 核查）**：
  - `BankLedgerQuery.findCandidates:39-84`（`module-finance/erp-fin-service/.../bankrecon/`）——**过滤维度接线点**：subjectId/dcDirection/amount/voucherId 四过滤后经 `findOccupiedLineIds` 排除已占用分录（:75-84）。对方账号过滤应在此查询加过滤条件（候选侧 = VoucherLine 的对方账号载体）。
  - `BankStatementMatcher.autoMatch:56-68`——**匹配入口**：`findCandidates(account, line.getAmount(), oppositeDirection, line.getTransactionDate(), daysWindow)` → size==1 自动 MATCHED（:61-65）/ 0 UNMATCHED / >1 SUSPENSE。
  - `BankStatementImporter.importStatement:44-110`（`BankStatementLineInput` DTO，`erp-fin-dao/.../dto/BankStatementLineInput.java`）——**对方账号数据入口**：DTO 无 counterparty 字段（仅 transactionDate/description/refNo/dcDirection/amount/balanceAfter），import 映射（:85-97）无 counterparty 写入。
  - **ORM 结构现状**：`ErpFinBankStatementLine`（orm.xml:1133-1173）19 列（id/statementId/lineNo/transactionDate/description/refNo/dcDirection/amount/currencyId/balanceAfter/matchStatus/matchedLineId/remark + 审计列）——**新增 3 列均为可空（无 NOT NULL/无默认值），纯加性**（对齐 2026-08-12 裁决「加列」授权 + Q3 纯加性类自动执行范围）。
  - **候选侧对方账号载体（Decision 项）**：`ErpFinVoucherLine` 有 `partnerId`（orm.xml:499 区域 to-one `partner` :518）+ `billRefCode` 相关——对方账号过滤可经 `partner`（ErpMdPartner.name/code）或 bill 关联解析；**VoucherLine 不加列**（裁决仅授权 BankStatementLine 3 列），候选侧过滤用既有维度（见 Phase 1 Decision）。
  - 测试基线：`TestErpFinBankStatementMatch`（7 个 @Test 场景：unique/multiple/none/direction/manual-match/manual-reject/occupied-exclusion）、`TestErpFinBankStatementImport`、`TestErpFinBankReconciliation`、`TestErpFinBankReconciliationEndToEnd`——**全部未设 counterparty 数据**（seed 无 counterparty 字段 → 新过滤为空放行，既有测试零回归需证明）。
- **预授权判据（2026-08-12 裁决 A 类）**：ORM 结构变更（3 可空加列）**批量授权**（对齐 Q3 纯加性类自动执行范围：加列不改既有语义、无 NOT NULL 无默认列、无既有数据 UK 增设、无删除/迁移/索引改造）；匹配算法过滤属纯代码逻辑预授权；**越界回落 ask-first**（若执行中发现需改既有语义/加 UK/迁移 → 暂停人工）。roadmap RC-R1.43 行 `todo`，Deps（R1.0 done）已满足。**仍需独立 plan-audit**（ORM 变更 + 匹配算法行为变更的标准义务）。
- **涉及文件**：`module-finance/model/app-erp-finance.orm.xml`（ErpFinBankStatementLine 加 3 列）；`module-finance/erp-fin-dao/.../dto/BankStatementLineInput.java`（加 3 字段）；`BankStatementImporter.java`（映射 3 字段）；`BankLedgerQuery.java`（findCandidates 加对方账号过滤）；`BankStatementMatcher.java`（若匹配维度需扩展签名/调用）；测试 `TestErpFinBankStatementImport`/`TestErpFinBankStatementMatch`/`TestErpFinBankReconciliation`（新增 counterparty 场景）；owner doc `bank-reconciliation.md` + arm-index/roadmap/`docs/logs/`（回填）。**Non-Goal 声明**：不新增 view.xml 展示改动（xmeta 字段由生成链自动同步，前端展示留待既有 AMIS 列配置后续按需追加，非本行范围）。

## Goals

- **对方账号维度运行时成立（P1-RC-004 核心）**：`ErpFinBankStatementLine` 新增 `counterpartyAccount`/`counterpartyName`/`counterpartyBank` 3 可空列（纯加性 ORM 变更）→ `BankStatementLineInput` DTO + `BankStatementImporter.importStatement` 承接导入数据 → `BankLedgerQuery.findCandidates` 增对方账号过滤（候选侧按 Phase 1 裁决的既有载体解析）→ `autoMatch` 单候选判定不再混入「对方账号不符」的候选（错误 MATCHED 消除）。
- **过滤语义（Decision 项）**：候选过滤的对方账号比较基准 = 对账单行 counterpartyAccount/counterpartyName vs 账面候选侧载体（ErpMdPartner.name/code 或 bill 关联 partner）——模糊匹配语义按 L1「模糊匹配」实现（精确相等 or 包含/前缀匹配，须显式裁决）。
- **导入数据契约**：`BankStatementLineInput` 增 3 可选字段 + import 映射（null 可空，缺省不破坏既有导入）；外部文件解析（MT940/CSV/Excel）仍属集成层 Non-Goal（DTO 承载已解析数据）。
- **测试**：新增 counterparty 维度场景——① 同额同日同科目不同对方账号且账面多候选 → 过滤后唯一候选 MATCHED（vs 现状错误 MATCHED）；② 对方账号不匹配 → 候选排除（UNMATCHED/SUSPENSE 正确归位）；③ 对账单无 counterparty 数据（null）→ 过滤空放行（既有测试零回归）；④ 导入映射断言（DTO→行字段落库）；⑤ 幂等/多 statement 场景回归。
- **零回归**：既有 `TestErpFinBankStatementMatch`/`TestErpFinBankStatementImport`/`TestErpFinBankReconciliation*` 全绿（seed 无 counterparty → 新过滤对 null 放行）+ 全仓构建 + compliance checker 零漂移（ORM 3 加列属 Q3 纯加性授权；compliance 规则 R2c 若因新 daoFor/import 面漂移则按既有先例登记 per-site 证据）。
- **owner doc 收敛**：`bank-reconciliation.md §业务规则 2` 补对方账号维度实现注记（列 + 过滤语义 + 模糊匹配判定）；不修改需求契约段（use-cases L1 不动）。
- **回填**：arm-index P1-RC-004 → `done (RC-R1.43)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-005（下月初自动红冲）**（独立 finding，属 RC-R1.2 已 done）。
- **不实现 P2-RC-001/002/003/083**（导入幂等键/valueDate 简化/多币种调整/行级断言补强——独立 P2 登记项，非本行）。
- **不给 ErpFinVoucherLine 加 counterparty 列**（2026-08-12 裁决仅授权 BankStatementLine 3 列；候选侧用既有 partnerId/bill 关联载体，见 Phase 1 Decision；若 Explore 证实候选侧必须加列 → 超出 A 类授权 → ask-first 暂停）。
- **不实现外部文件解析（MT940/CSV/Excel）**（集成层 Non-Goal，DTO 承载已解析数据）。
- **不新增 view.xml/xmeta 展示改动**（xmeta 字段由生成链自动同步；前端 AMIS 列展示留待后续按需追加，非本行范围）。
- **不重构 BankStatementMatcher 匹配主链**（仅增过滤维度，不改变单候选/多候选/无候选三分支语义）。
- **不改真相源契约段落**（use-cases L1 不动；L2 业务规则段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的 ORM 纯加性 + 代码逻辑修复，Q4=(a) 强制实现禁止方案 B；2026-08-12 裁决 A 类 ORM 批量授权）
- Owner Docs: `docs/design/finance/use-cases.md`（L1 UC-FIN-09/14）+ `docs/design/finance/bank-reconciliation.md`（§业务规则 2）
- Skill Selection Basis: 实现面 = ORM 模型变更（`nop-backend-dev`：orm.xml 模型优先 + 增量重生成）+ 匹配算法过滤 + DTO/Importer 接线（`nop-backend-dev`）；测试（`nop-testing`：JunitBaseTestCase 直断言 + GraphQL RPC 冒烟范式）。无 view.xml/xbiz 变更（xmeta 字段由生成链自动同步，前端展示非本行范围，见 Non-Goals）。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务。
- ORM 变更触发增量重生成：`mvn clean install -DskipTests`（gen-orm.xgen 增量链，对齐 AGENTS.md「ORM 模型变更后用 mvn clean install -DskipTests 触发增量重新生成，不要重跑 nop-cli gen」）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-finance/erp-fin-service`。

## Execution Plan

### Phase 1 - Explore 候选侧对方账号载体 + 模糊匹配语义（Decision）

Status: completed
Targets: `BankLedgerQuery.java`（findCandidates）；`BankStatementMatcher.java`（autoMatch）；`ErpFinVoucherLine` ORM 既有维度（partnerId/bill 关联）；`bank-reconciliation.md`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **候选侧对方账号载体裁决（C1）**：**选项 A（倾向）** = 经 `ErpFinVoucherLine.partnerId` → `ErpMdPartner`（name/code）作为账面侧对方账号基准（`findCandidates` 内对候选行解析 partner 信息后与对账单行 counterpartyAccount/counterpartyName 比较；或 SQL 层 join/查询过滤）；**选项 B（否决，越界）** = 给 `ErpFinVoucherLine` 加 counterpartyAccount 列（超出 2026-08-12 裁决 A 类「ErpFinBankStatementLine 加 3 列」授权范围 → ask-first 暂停）；**选项 C（否决）** = 经 `billRefCode`→bill 头 → partner 解析（链路过长，findCandidates 性能与复杂度不匹配）。**理由**：裁决授权面明确限定 BankStatementLine 3 列；VoucherLine.partnerId 是既有辅助核算维度（orm.xml:497），ErpMdPartner 承载往来单位名称/编码，可直接作为账面侧对方账号；A 方案在既有数据模型内完成过滤维度，零新增列面。**注意**：partner 维度 ≠ 银行对方账号字面（银行流水对方账号是银行侧账号，partner 是 ERP 往来单位）——语义等价性须在计划内显式裁决（L1「对方账号」字面 vs ERP 数据模型可承载的伙伴维度，倾向以 partner 作为代理载体并记录残留风险）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **模糊匹配语义裁决（C2）**：**选项 A（倾向）** = 对账单行 counterpartyName 与账面侧 partner.name 做**非空精确匹配**（两侧均非空才比较，任一为空放行——保持既有行为兼容）；**选项 B（否决）** = 包含/前缀模糊匹配（L1「模糊匹配」字面，但 SQL 层 LIKE 匹配跨方言差异 + 误配面扩大，A4.1.11 触发率评估表明精确匹配已消除核心错误 MATCHED 场景）；**选项 C（否决）** = counterpartyAccount 精确匹配（银行账号侧数据种子缺失，实际导入数据多为户名）。**理由**：L1「模糊匹配」在 ERP 数据模型内以户名（counterpartyName vs partner.name）承载最贴近实际导入数据；精确匹配消除「同额同日不同对方账号」错误 MATCHED 的核心场景（A4.1.11 触发面）；模糊化（LIKE）增强留待户名精确匹配被实际误配暴露后按需追加（Successor Required: no——无触发条件不登记）。记录备选方案与残留风险。
      - Skill: `nop-backend-dev`
- [x] `Proof` **运行时验证前置**：既有测试基线确认——`TestErpFinBankStatementMatch` 7 场景全绿（seed 无 counterparty）；`BankLedgerQuery.findCandidates` 过滤链（:62-72 四过滤 + :75-84 occupied 排除）读侧纯查询无副作用；`BankStatementImporter` 导入链路（DTO→行映射 :85-97）无 counterparty 写入。ORM 3 列加列后生成产物（Entity/xmeta/DDL）自动同步的机制确认（既有 R1.40 先例：ORM defaultValue 收敛经 `mvn clean install` 增量重生成成功）。
      - Skill: `nop-testing`

Exit Criteria:

> 仅写此阶段实际交付的可观察结果，以及解除后续阶段阻塞所需的任何本地化检查。

- [x] C1（候选侧载体）+ C2（模糊匹配语义）裁决记录落盘计划（选择 + 备选 + 理由 + 残留风险），Explore 证据（findCandidates 过滤链 + partnerId 载体 + 测试基线）确认
- [x] ORM 3 列加列后的生成链机制确认（R1.40 先例），无语法/引用错误风险点识别完成

### Phase 1 裁决与证据（执行落盘，2026-08-15）

**C1 候选侧对方账号载体裁决 = 选项 A（选定）**：`ErpFinVoucherLine.partnerId → ErpMdPartner.name`（既有辅助核算维度，`app-erp-finance.orm.xml:493` partnerId propId=17 + :512 partner to-one；ErpMdPartner.name 承载往来单位名称 precision 200）。**选项 B（否决，越界）**——VoucherLine 加 counterpartyAccount 列超出 2026-08-12 裁决 A 类「ErpFinBankStatementLine 加 3 列」授权范围。**选项 C（否决）**——billRefCode→bill 头→partner 解析链路过长。残留风险：partner（ERP 往来单位）≠ 银行流水对方账号字面（银行侧账号）；本计划以 partner 作为代理载体承载 L1「对方账号」维度。实现注：候选侧伙伴名解析经 ORM to-one 关系 getter `line.getPartner().getName()`（惰性加载，候选集窄小无 N+1 问题），**不新增 daoFor(ErpMdPartner) 站点**（R2c=1399 零漂移）。

**C2 模糊匹配语义裁决 = 选项 A（选定）**：对账单行 counterpartyName 与账面侧 partner.name 做**非空精确匹配**——两侧均非空才比较，任一为空放行（保持既有行为兼容，seed 无 counterparty 零回归）。**选项 B（否决）**——LIKE 包含/前缀模糊匹配（SQL 层跨方言差异 + 误配面扩大；A4.1.11 触发率评估表明精确匹配已消除核心错误 MATCHED 场景）。**选项 C（否决）**——counterpartyAccount 精确匹配（银行账号侧数据种子缺失，实际导入数据多为户名）。残留风险：户名精确匹配无法覆盖「户名差一」SUSPENSE 语义（L2 原文），LIKE 增强留待实际误配暴露后按需追加（Deferred 已登记 Successor Required: no）。

**Proof 证据（2026-08-15 实仓）**：
- 测试基线：`mvn test -pl module-finance/erp-fin-service` **471 tests 全绿**（0 failures/0 errors），含 TestErpFinBankStatementMatch 7 场景 + TestErpFinBankStatementImport 6 + TestErpFinBankReconciliation 5 + TestErpFinBankReconciliationEndToEnd 1 + TestErpFinBankReconAutoReverseJob 5。
- `BankLedgerQuery.findCandidates:39-84` 过滤链（subjectId + dcDirection + amount + voucherId 窗口 + occupied 排除）读侧纯查询无副作用（代码实读 :50-83）。
- `BankStatementImporter.importStatement:80-102` 行映射无 counterparty 写入（代码实读）。
- 生成链机制（R1.40 先例）：roadmap `requirement-compliance-roadmap.md:432` 实证——ORM 变更经 `mvn clean install -DskipTests` 增量重生成 BUILD SUCCESS + 生成产物核对（XMeta `_*.xmeta` + `_app.orm.xml` + mysql/oracle/postgresql DDL 同步）。
- propId 分配实证：ErpFinBankStatementLine 当前 19 列 propId 1-19（业务 1-13 + 审计 14-19 显式声明，orm.xml:1137-1155），**20/21/22 空闲**；`ErpMdPartner.customerGroup` propId=100 显式声明先例（master-data orm.xml:443）。语法/引用错误风险点：新列仅生成链消费（Entity getter/setter + XMeta + DDL），无手工 Java 引用需同步（DTO/Importer/过滤代码为 Phase 2/3 手工接线）。

### Phase 2 - ORM 3 列 + 导入接线（A 类授权变更）

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（ErpFinBankStatementLine）；`BankStatementLineInput.java`；`BankStatementImporter.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成（C1/C2 裁决）

- [x] `Add` `app-erp-finance.orm.xml` ErpFinBankStatementLine 增 3 可空列：`counterpartyAccount`（displayName 对方账号，VARCHAR precision 50，stdDataType string，可空）+ `counterpartyName`（对方户名，VARCHAR precision 200，可空）+ `counterpartyBank`（对方开户行，VARCHAR precision 200，可空）——**propId 取 20/21/22**（当前实体 19 列 propId 1-19：业务列 1-13 + 审计列 14-19 已**显式声明**于 orm.xml（delVersion=14/version=15/createdBy=16/createTime=17/updatedBy=18/updateTime=19），**不可占用**；新列要么显式声明 propId 20/21/22 要么省略 propId 由模型初始化器自动顺延（`OrmEntityModelInitializer` 顺序分配），二者等价——建议显式声明 20/21/22 防漂移；仓库先例 `ErpMdPartner.customerGroup` 用 propId=100 显式声明），**均无 NOT NULL/无默认值/无索引/无 UK**（Q3 纯加性授权范围）。⚠ 若错赋 propId 14/15/16 会抛 `ERR_ORM_MODEL_DUPLICATE_PROP_ID`（模型初始化器重复检查）——Phase 2 Proof 的生成产物核对须含 propId 分配核对。
      - Skill: `nop-backend-dev`
- [x] `Fix` `BankStatementLineInput` DTO 增 3 可选字段（counterpartyAccount/counterpartyName/counterpartyBank + getter/setter，Java 字段风格对齐既有）——**不可为必填**（外部文件解析 Non-Goal，既有调用方/测试不传不破坏）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `BankStatementImporter.importStatement` 行映射增 3 字段写入（`line.setCounterpartyAccount(in.getCounterpartyAccount())` 等，null 透传）——导入链路闭环。
      - Skill: `nop-backend-dev`
- [x] `Proof` 增量重生成验证：`mvn clean install -DskipTests` 触发 gen-orm.xgen → 生成产物核对（Entity 类 3 getter/setter + xmeta 3 字段 + DDL 3 列，无结构性漂移）+ 分域编译通过。
      - Skill: `nop-testing`

Exit Criteria:

- [x] ORM 3 列 + DTO 3 字段 + Importer 映射落地（grep 显示 3 列在 orm.xml/Entity/xmeta/DDL 同步 + import 写入），`mvn clean install -DskipTests` 生成链通过
- [x] 生成产物核对完成（无结构性漂移），分域 `mvn compile -pl module-finance/erp-fin-service` 通过

### Phase 2 执行证据（2026-08-15）

- **orm.xml 3 列落地**：`app-erp-finance.orm.xml` ErpFinBankStatementLine 增 `counterpartyAccount`（propId=20，VARCHAR 50）/`counterpartyName`（propId=21，VARCHAR 200）/`counterpartyBank`（propId=22，VARCHAR 200），均无 NOT NULL/无默认值/无索引/无 UK（Q3 纯加性授权范围）。
- **DTO + Importer**：`BankStatementLineInput` 增 3 可选字段 + getter/setter；`BankStatementImporter.importStatement` 行映射增 3 字段写入（null 透传）。
- **生成产物核对**：`mvn clean install -DskipTests` BUILD SUCCESS → `_ErpFinBankStatementLine.java` 3 getter/setter（:1172-1218）+ `_ErpFinBankStatementLine.xmeta` propId 20/21/22 + DDL 三方言同步（mysql `COUNTERPARTY_ACCOUNT VARCHAR(50) NULL` 等 / oracle `VARCHAR2(50)` / postgresql `counterparty_account VARCHAR(50)`）——无结构性漂移。propId 分配核对：审计列 14-19 保留，新列 20/21/22 无重复（无 ERR_ORM_MODEL_DUPLICATE_PROP_ID）。
- 分域编译：`mvn install -DskipTests -pl module-finance/erp-fin-service -am` BUILD SUCCESS。

### Phase 3 - findCandidates 对方账号过滤 + 测试（P1-RC-004 核心）

Status: completed
Targets: `BankLedgerQuery.java`（findCandidates）；`BankStatementMatcher.java`（autoMatch 调用）；测试 `TestErpFinBankStatementMatch`/`TestErpFinBankStatementImport`/`TestErpFinBankReconciliation`；owner doc `bank-reconciliation.md`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 2 完成（ORM/导入落地）

- [x] `Fix` `findCandidates` 增对方账号过滤（按 C1/C2 裁决）：对账单行 counterpartyName 非空时，候选 ErpFinVoucherLine 须满足 partner 解析基准匹配（C1 选项 A 实现——经 partnerId→ErpMdPartner.name 比较或查询过滤）；两侧任一为空放行（保持 null 兼容零回归）。
      - Skill: `nop-backend-dev`
- [x] `Add` 测试新增 counterparty 维度场景（`TestErpFinBankStatementMatch` 或新 `TestErpFinBankStatementCounterpartyMatch`）：① 同额同日同科目不同对方账号 + 账面多候选 → 过滤后唯一 MATCHED（对照现状错误 MATCHED 场景）；② counterpartyName 不匹配 → 候选排除（SUSPENSE/UNMATCHED 正确归位）；③ 对账单无 counterparty（null）→ 过滤空放行（既有测试零回归重跑）；④ `TestErpFinBankStatementImport` 增 DTO→行字段落库断言；⑤ 幂等/多 statement 回归。
      - Skill: `nop-testing`
- [x] `Add` owner doc 注记：`bank-reconciliation.md §业务规则 2` 补对方账号维度实现注记（3 列 + 过滤语义 + C1/C2 裁决 + 残留风险）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `findCandidates` 对方账号过滤落地 + 新测试全绿（①-⑤ 通过）+ 既有 `TestErpFinBankStatementMatch`/`TestErpFinBankStatementImport`/`TestErpFinBankReconciliation*` 零回归
- [x] owner doc 注记落地 + compliance checker 零漂移（或 per-site 证据登记）

### Phase 3 执行证据（2026-08-15）

- **findCandidates 过滤落地**：`BankLedgerQuery.findCandidates` 签名增 `String counterpartyName` 参数，occupied 排除后增对方账号过滤（`StringHelper.isNotBlank(counterpartyName)` 时经 `line.getPartner().getName()` 解析候选侧 partner.name，两侧均非空才比较，任一为空放行）；`BankStatementMatcher.autoMatch` 传 `line.getCounterpartyName()`（null 兼容零回归）。**零新增 daoFor 站点**（partner 经 ORM to-one 关系 getter 惰性加载）→ compliance R2c=1399 不变。
- **测试**：新增 `TestErpFinBankStatementCounterpartyMatch` 4 组[①多候选过滤后唯一 MATCHED 且 matchedLineId=partner 相符行（对照现状错误 MATCHED）/②单候选户名不符排除 → UNMATCHED（错误 MATCHED 消除）/③null 过滤空放行多候选 SUSPENSE 保持/⑤多 statement 过滤+占用排除联动]；`TestErpFinBankStatementImport` 增 2 组[④DTO→行字段落库断言/null 透传]。**erp-fin-service 485 tests 全绿**（477 既有零回归 + 8 新增），含既有 `TestErpFinBankStatementMatch` 7 场景 / `TestErpFinBankReconciliation*` 全绿。
- **owner doc 注记**：`bank-reconciliation.md` schema 补注增对方账号维度实现注记（3 列 + 过滤语义 + C1/C2 裁决 + 残留风险；§业务规则 2 契约段未动）。
- **compliance checker 零漂移**：`bash docs/audits/nop-compliance-checker.sh` 全 16 规则 actual ≤ baseline（R2c=1399 / R10=9 等全部持平）。



## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_ffaf7e805ffevNHDaGDHOzApfl`) — 1 MAJOR + 4 MINOR。MAJOR-1 已修正：Phase 2 propId 声明错误（审计列 14-19 已显式声明于 orm.xml，错赋 14/15/16 抛 `ERR_ORM_MODEL_DUPLICATE_PROP_ID`；改为显式声明 20/21/22 或省略顺延，并补 `ErpMdPartner.customerGroup` propId=100 先例 + 生成产物核对含 propId 分配核对）。4 MINOR 已修正：(1) `TestErpFinBankStatementMatch` 场景数 5→7（unique/multiple/none/direction/manual-match/manual-reject/occupied-exclusion，无「幂等」命名测试）；(2) 自引用笔误「R1.43 先例」→「R1.40 先例」；(3) C2 与 Deferred 的 successor 不一致统一（LIKE 增强 Successor Required: no——无触发条件不登记）；(4) xmeta/view「评估后决定」软化措辞改为显式 Non-Goal（不新增 view.xml 展示改动，xmeta 由生成链自动同步）。其余全部 baseline 声明实仓核实 PASS（findCandidates 四过滤链/autoMatch 三分支/Importer 映射/DTO 无 counterparty/ORM 19 列可空加列纯加性/partnerId 载体非空——`persistVoucher` 拷贝 fact partnerId + AR/AP Provider 设置/08-12 裁决 A 类文本/R1.40 重生成先例/零回归结构性成立——全仓零 counterparty 字段故 seed 必为空）。
- Independent draft review iteration 2: `acceptable` (`ses_ffaefb03cffe6SVLi4DNXDXp0R`) — 逐项复核 5 项修正全部正确落地（orm.xml:1137-1155 审计列 propId 14-19 实证 + 20/21/22 空闲；TestErpFinBankStatementMatch 恰 7 个 @Test :62-223 实证；R1.40 先例双站点修正；C2/Deferred successor 一致；xmeta/view Non-Goal 显式化）。2 项残留措辞已就地修正（Task Route 软化短语清理 + Non-Goals 增 xmeta/view 专属 bullet）。共识达成，计划可转 active。

## Closure Gates

- [x] 范围内行为完成（R1.43 对方账号维度：ORM 3 列 + 导入 + 过滤 + 测试）
- [x] 相关文档对齐（bank-reconciliation.md 注记 + arm-index P1-RC-004 → done (RC-R1.43) + roadmap 行 done）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service` + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### VoucherLine 侧 counterpartyAccount 列（裁决未授权）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 2026-08-12 裁决仅授权 BankStatementLine 3 列；候选侧以 partnerId→ErpMdPartner 代理承载（C1 选项 A），VoucherLine 加列超出授权须 ask-first
- Successor Required: `yes`（若未来需要银行侧账号精确匹配（counterpartyAccount 双向），按 ORM ask-first 流程立项）

### 模糊匹配增强（LIKE 前缀/包含匹配）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1「模糊匹配」字面在 ERP 数据模型内以户名精确匹配承载（C2 选项 A），已消除 A4.1.11 核心错误 MATCHED 场景；LIKE 模糊化跨方言差异 + 误配面扩大，属后续增强
- Successor Required: `no`

### 外部文件解析（MT940/CSV/Excel）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: DTO 承载已解析数据（集成层 Non-Goal，bank-reconciliation.md 既有声明），本行不实现解析器
- Successor Required: `no`

## Closure

Status Note: 三 Phase 全部完成且经独立结束审计 ACCEPT——P1-RC-004 对方账号维度运行时成立（ORM 3 可空列 + DTO/Importer 接线 + findCandidates 对方账号过滤[C1 选项 A partnerId→ErpMdPartner.name + C2 选项 A 非空精确匹配] + 测试 8 组新增），erp-fin-service 485/485 全绿（477 基线零回归 + 8 新增）+ 全量 `mvn clean install -DskipTests` BUILD SUCCESS + compliance checker 零漂移（R2c=1399 持平）；Non-Goals 零违反；Deferred 3 项 adjudicated 与裁决一致。计划可关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计员（新会话，无执行者上下文），task `ses_ffaa97c6fffe4wOam34d8B9mlV`
- Evidence: 实仓逐项核对——orm.xml:1150-1152 3 可空列（propId 20/21/22，无 NOT NULL/默认/索引/UK，审计列 14-19 未破坏）；BankStatementLineInput.java:20-25,75-97 3 字段；BankStatementImporter.java:91-93 映射；BankLedgerQuery.java:44-46,93-98 counterpartyName 过滤（非空精确匹配、任一为空放行、经 to-one getter 零新增 daoFor）；BankStatementMatcher.java:58-60 传参且三分支语义未动；生成产物 Entity（:1172-1218）/xmeta（propId 20/21/22）/DDL 三方言（deploy/sql/*/_create_erp-fin.sql:878-880）同步；TestErpFinBankStatementCounterpartyMatch 4 组（:65/:101/:129/:160）+ TestErpFinBankStatementImport 2 组（:181/:204）逻辑正确。验证：`mvn test -pl module-finance/erp-fin-service` **485/485 全绿**（含定向 12/12）；`bash docs/audits/nop-compliance-checker.sh` 全 16 规则 actual ≤ baseline（R2c=1399 持平）。回填：arm-index:129 done (RC-R1.43) + roadmap:435 done ✅ + docs/logs/2026/08-15.md:1 日志条目。Non-Goals 零违反（VoucherLine/use-cases/契约段零 diff，_gen view 为生成链产物）。Deferred 3 项 adjudicated 与裁决一致。**裁决：ACCEPT**。唯一记录项：API beans 生成链同步未列入计划文件清单（生成产物，非人工改动，不影响门控）。

Follow-up:

- 无（范围内零未决项；VoucherLine 侧 counterpartyAccount 列 + LIKE 模糊增强 + 外部文件解析均已按 Deferred But Adjudicated 登记，Successor 触发条件已记录）
