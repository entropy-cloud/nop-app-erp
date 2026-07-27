# 2026-07-27-0823-1 R1d/R10/R6 合规基线裁决 + checker 注释校准

> Plan Status: completed
> Last Reviewed: 2026-07-27
> Source: `docs/audits/compliance-baseline.md`（R1d baseline=23 / actual=28 → **CI red +5 漂移**；R10 baseline=51 / actual=51 grandfathered；R6 baseline=7 / actual=7 grandfathered）+ `docs/plans/2026-07-25-1057-1-compliance-baseline-drift-adjudication.md` §Deferred But Adjudicated「R10（51 处 REQUIRES_NEW）/ R1d（23 处 findAllByQuery）既有 grandfathered 基线裁决」（Classification: optimization candidate；Successor Required: yes；触发条件：需主动降低 R10/R1d 基线时，或 REQUIRES_NEW / findAllByQuery 出现功能性问题时）——触发条件经实时仓库核实**已满足**：R1d 已 CI red +5 漂移须即时裁决，且 AGENTS.md §当前项目阶段明示「运营成熟度收尾」为当前重点
> Related: `docs/plans/2026-07-25-1057-1-compliance-baseline-drift-adjudication.md`（R2b/R2c/R2d 漂移裁决 + R8 checker 校准先例）、`docs/plans/2026-07-24-0941-2-r3-new-erp-construct-r7-system-clock-compliance-convergence.md`（R3 checker 交叉引用 orm.xml 校准先例）、`docs/plans/2026-07-25-1057-2-per-mutation-processor-file-split.md`（R8 二次校准 content-based 排除先例）、`docs/audits/2026-07-23-0000-architecture-governance-review.md` §F8（checker dead armor → live guard）
> Audit: required

## Current Baseline

合规检查器 CI 门控（`.github/workflows/compliance.yml`）解析 `docs/audits/compliance-baseline.md` 的 `## BASELINE (machine-readable)` 块与 checker 实测比对，**actual > baseline → CI 失败**。

**2026-07-27 实测复跑**（`bash docs/audits/nop-compliance-checker.sh`）vs 基线：

| 规则 | 基线 | 实测 | 漂移 | CI |
|------|------|------|------|----|
| R1d（BizModel dao().findAllByQuery） | 23 | **28** | +5 | ❌ FAIL |
| R6（@Transactional in BizModel） | 7 | 7 | 0 | ✅ |
| R10（REQUIRES_NEW 事务） | 51 | 51 | 0 | ✅ |

**R1d 已 CI red**——baseline doc line 118 注记「R1d=28 的 +5 漂移来自前序未同步基线的计划」，但基线 machine-readable 块仍为 23，门控未放行。

**R10/R6/R1d checker 共性缺陷**：三规则均经 `rgrep_bizmodel` / `rgrep_prodjava` 用 `grep -Hn '<pattern>'` 原始匹配，**不区分代码行与 javadoc/注释行**。实测三类命中中绝大多数为注释引用模式名而非真实代码站点：

- **R10（51 处）**——实测逐行核实（`grep -rn 'REQUIRES_NEW' ... | grep -v '_gen/'`）：真实代码站点仅 **~6 处**（`ErpFinVoucherBizModel:71,79` 2 处 `@Transactional(REQUIRES_NEW)` + `ErpFinPostingExceptionRecorder:95,159` 2 处 `runInTransaction(...REQUIRES_NEW...)` + `ErpFinDeferredPostingRetryHelper:74,131` 2 处 `runInTransaction(...REQUIRES_NEW...)`），其余 **~45 处均为 javadoc 注释**（`* 跨域失败隔离的事务边界由 Facade ... 的 REQUIRES_NEW 承接` 类引用，遍布 11 域 `*PostingExecutor.java` / `*PostingDispatcher.java`）。真实 6 处全部为 `processor-extension-pattern.md` 硬规则 1（独立事务边界由 Facade 显式声明传播策略）文档化的合法跨域失败隔离事务边界（Facade `IErpFinVoucherBiz.post/reverse` + 过账异常记录/重试独立事务；代码注释 `nop-check: allow @Transactional(REQUIRES_NEW)` 明示豁免并交叉引用 owner doc）。

- **R6（7 处）**——`rgrep_bizmodel '@Transactional'` 实测：真实 `@Transactional` 注解仅 **2 处**（`ErpFinVoucherBizModel:71,79`），其余 5 处为 javadoc 注释（`* @BizMutation 自动包装事务（不叠加 {@code @Transactional}）` 类引用 + `nop-check:` 注释）。R6 真实站点是 R10 的 BizModel 子集（ErpFinVoucherBizModel post/reverse），R10 另有 4 处非 BizModel `runInTransaction(...REQUIRES_NEW...)` 调用（过账异常记录/重试 helper）。

- **R1d（28 处）**——`rgrep_bizmodel 'dao()\.findAllByQuery'` 实测：真实 `dao().findAllByQuery(...)` 调用 ~17 处（crm 6 + master-data 8 + sales 3），其余 ~11 处为 javadoc 注释引用（`* 经 dao().findAllByQuery 查询绕过 findList 管道` 类说明）。真实调用全部为**同域只读内部辅助查询**，每处代码注释明示「绕过 CrudBizModel findList 管道的 objMeta 过滤/字段投影以保留全部候选」——合法模式（C3 日期范围互斥校验 + 价格清单维度查询 + 树形递归 + 唯一性前置友好校验）。

**R1d +5 漂移源**（baseline 23 → actual 28，自 `2026-07-24-0930-1` 基线锚点提交 `bd8540037` 2026-07-24 11:43:20 之后）：经 `git diff bd8540037..HEAD -- <BizModel files> | grep findAllByQuery` 核实，+5 漂移**全部来自单一已审计计划** `2026-07-26-0315-1`（C3 sales 定价推广）——3 处真实 `dao().findAllByQuery` 调用（`ErpSalPricingRuleBizModel` / `ErpSalPriceListLineBizModel` / `ErpSalPriceListBizModel` 各 1 处重叠/互斥校验）+ 2 处 javadoc 注释行引用。master-data BizModels（`ErpMdExchangeRate`/`ErpMdTaxRate`/`ErpMdSupplierApproval`/`ErpMdMaterialCustoms`）的 `findAllByQuery` 调用由 `2026-07-21-2225-1`（C3）+ `2026-07-21-1206-1`（C2）落地，**两计划均早于基线锚点**（2026-07-21），其调用已计入 baseline 23，不构成漂移。

剩余差距：(1) R1d CI red 须即时裁决（裁决性上调基线或 checker 注释校准）；(2) R10/R6 grandfathered 基线 51/7 为注释虚假命中主导，checker 注释校准可大幅下降 actual 至真实代码站点水平；(3) 三规则的 checker 原始 grep 不区分代码与注释，与 R8 先例（content-based 排除）同型缺陷。

## Goals

1. **恢复并稳固 CI green**：使 R1d actual ≤ baseline（经注释校准 + 裁决性基线对齐）。
2. **R10/R6 checker 注释校准**：排除 javadoc/注释行（`*` / `//` / `{@code` / `{@link`），使 actual 反映真实代码站点（R10 51→~6，R6 7→~2），对齐 R8 content-based 排除先例（`2026-07-25-1057-1` + `2026-07-25-1057-2`）。
3. **R1d 注释校准 + 漂移裁决**：R1d checker 同步排除注释行；真实代码站点经逐项确认合法（同域只读内部辅助查询，绕过 findList 管道为有意设计），按基线规则裁决性上调基线至真实代码计数。
4. **基线 machine-readable 块 + 裁决注记同步**：更新 `compliance-baseline.md` 三规则基线值 + 注释校准注记 + R1d 漂移源裁决注记（镜像 1057-1 §R2b/R2c/R2d 基线裁决性上调注记格式）。

## Non-Goals

- **不重构 R1d 真实 `findAllByQuery` 调用站点**——这些站点的合法性已经源计划（C3/C2/定价推广）的独立草案审查 + 结束审计验证为同域只读内部辅助查询（绕过 findList 管道为有意设计，每处代码注释明示理由），本计划仅做 checker 注释校准 + 基线裁决性对齐，不重复重构。
- **不重构 R10/R6 真实 `REQUIRES_NEW` / `@Transactional` 站点**——真实 6 处（ErpFinVoucherBizModel post/reverse + 过账异常记录/重试）全部为 `processor-extension-pattern.md` 硬规则 1 文档化的合法跨域失败隔离事务边界，本计划仅做 checker 注释校准使 actual 反映真实站点。
- **不改 ORM / 契约 / 字典 / 业务行为 / Java 业务代码**——纯合规治理（checker shell 脚本注释排除校准 + 基线文档更新 + 单模块测试回归保险）。
- **不裁决 R2a/R2b/R2c/R2d/R3/R8/R12 等其他规则**——这些规则已由前序计划（0941-1/0941-2/1057-1/1057-2/0605-2/1400-1/1600-1/0300-1）裁决到位，actual ≤ baseline。
- **不引入语义级 Java 解析**——checker 注释排除采用行级 grep 过滤（排除 `^\s*\*` / `^\s*//` / `{@code` / `{@link` 行），对齐既有 checker 的行级启发式风格；不引入 AST 解析（过度工程化，既有 checker 全部为 grep 启发式）。

## Task Route

- Type: `verification or audit work`（合规基线漂移裁决 + checker 注释校准，结果面 = CI green + 基线对齐，对齐 1057-1 同型 Task Route）
- Owner Docs: `docs/audits/compliance-baseline.md`（基线表 + machine-readable 块 + 裁决注记段）、`docs/analysis/governed-path-cost-evaluation.md`（daoFor/事务合规路径评估）、`docs/architecture/processor-extension-pattern.md`（REQUIRES_NEW 事务边界硬规则 1 —— R10/R6 真实站点的合法性 owner doc 背书）、`docs/audits/2026-07-23-0000-architecture-governance-review.md` §F8
- Skill Selection Basis: `nop-backend-dev` 匹配「跨实体调用 / daoFor 收敛 / 事务边界 / 合规路径评估」工作方法，与 0941-1/0941-2/1057-1 同型基线/校准计划均经该技能路由；checker 注释校准对齐 1057-1 R8 content-based 排除 + 0941-2 R3 交叉引用 orm.xml 先例

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 checker shell 脚本注释排除校准 + 基线文档更新 + 单模块测试回归保险，无端口/密钥/外部服务/数据迁移）。

## Execution Plan

### Phase 1 - Explore（R1d/R6/R10 全站点三态分类 + R1d 漂移源 git 时间线核实）

Status: completed
Targets: 全 R1d/R6/R10 命中站点（经 `bash docs/audits/nop-compliance-checker.sh` 输出 + `rg -n '<pattern>' module-*/erp-*-service/src/main/java module-common-service/src/main/java | grep -v _gen/` 逐行核实）、`docs/audits/nop-compliance-checker.sh`（R1d/R6/R10 段 + `rgrep_bizmodel`/`rgrep_prodjava` helper）、git 提交时间线（R1d +5 漂移源核实）
Skill: `nop-backend-dev`

- Item Types: `Proof | Decision`
- Prereqs: 无（基线门控已上线，R1d 漂移已实测确认 CI red）

- [x] `Proof`：逐行核实 R10 全 51 命中——分类为 (A) 真实代码站点（`@Transactional...REQUIRES_NEW` 注解 + `runInTransaction(...TransactionPropagation.REQUIRES_NEW...)` 程序化调用）/ (B) javadoc 注释行（`^\s*\*` 含 `{@code}`/`{@link}` 引用）/ (C) 行注释（`^\s*//`）。产出 file:line 清单 + 三态计数，确认真实代码站点 ~6 处 + 注释 ~45 处。
      - Skill: `nop-backend-dev`
- [x] `Proof`：逐行核实 R6 全 7 命中——同三态分类，确认真实 `@Transactional` 注解 ~2 处（ErpFinVoucherBizModel post/reverse，与 R10 BizModel 子集完全重叠）+ 注释 ~5 处。
      - Skill: `nop-backend-dev`
- [x] `Proof`：逐行核实 R1d 全 28 命中——同三态分类，确认真实 `dao().findAllByQuery(...)` 调用 ~17 处 + 注释 ~11 处；真实调用逐处确认「同域只读内部辅助查询」+ 代码注释明示「绕过 findList 管道」理由。
      - Skill: `nop-backend-dev`
- [x] `Proof`：R1d +5 漂移源 git 时间线核实——`git diff bd8540037..HEAD -- module-*/erp-*-service/src/main/java/**/entity/*BizModel.java | grep findAllByQuery` 确认 +5 checker 漂移**全部来自单一已审计计划** `2026-07-26-0315-1`（3 处真实 `dao().findAllByQuery` 调用 in ErpSalPricingRule/PriceListLine/PriceList + 2 处 javadoc 注释行），全部晚于基线锚点 `bd8540037`（2026-07-24 11:43:20）；master-data BizModels 的 findAllByQuery 调用（2225-1/1206-1）均早于基线锚点（2026-07-21）已计入 baseline 23，post-baseline diff 为 0 不构成漂移。逐处合法性分类（同域只读内部辅助查询，绕过 findList 为有意设计）。
      - Skill: `nop-backend-dev`
- [x] `Decision`：checker 注释排除校准方案裁决——候选：(A) 行级 grep 过滤排除注释行（`grep -vE ':\s*(\*|//)' ` 排除 javadoc `*` 与行注释 `//` 行，并排除含 `{@code`/`{@link` 的行）；(B) 引入语义级 Java 解析（AST）；(C) 保持原样仅上调基线。推荐 (A)：最小侵入 + 对齐既有 checker 行级启发式风格 + 对齐 R8 content-based 排除先例。记录选择 + 理由 + 残留风险。
      - 考虑的替代方案：(B) 引入 AST 解析——拒绝（过度工程化，既有 checker 全部为 grep 启发式，引入 AST 破坏一致性 + 增加 checker 运行时依赖）；(C) 仅上调基线不校准——拒绝（保留注释虚假命中使基线语义模糊，未来注释增减会触发虚假漂移）
      - 残留风险：行级 grep 可能漏排除块注释 `/* ... */` 内的跨行命中——缓解：R10/R6/R1d 实测命中绝大多数为单行 javadoc `*` 引用，块注释跨行命中经 Phase 1 Proof 核实为 0；若未来出现块注释命中，升级为 AST 解析（开独立 successor）
      - Skill: `nop-backend-dev`
- [x] `Decision`：R1d 基线裁决——注释校准后真实代码站点（~17 处，含 pre-baseline 的 master-data 合法调用 + post-baseline 0315-1 sales 合法调用）全部为合法同域只读内部辅助查询（C3 互斥校验 + 价格清单维度查询 + 树形递归 + 唯一性前置友好校验），按基线规则裁决为「合法模式」并设定新基线 = 注释校准后真实代码计数。R10/R6 基线同步下调至真实代码计数（R10 ~6 / R6 ~2）。记录每条裁决的合法性依据 + owner doc 背书（`processor-extension-pattern.md` 独立事务边界硬规则 1 for R10/R6；C3 `date-ranged-validity-pattern.md` + 各 BizModel 代码注释 for R1d）。pre-baseline 调用（2225-1/1206-1）与 post-baseline 漂移调用（0315-1）均为合法，但仅后者构成「漂移」——裁决性上调基线以吸收漂移并反映真实代码计数。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] R10/R6/R1d 全站点三态分类清单落盘（file:line + 三态 + 真实代码计数），与 actual 51/7/28 精确对账
- [x] R1d +5 漂移源 git 时间线核实 + 逐处合法性分类落盘
- [x] checker 注释排除校准方案 + R1d/R10/R6 基线裁决方案记录

### Phase 1 Evidence

#### R10 三态分类（actual 51 = A:6 + B:41 + C:4）

**A 真实代码站点（6 处）**：

| # | file:line | 形态 |
|---|-----------|------|
| A1 | `module-finance/erp-fin-service/.../entity/ErpFinVoucherBizModel.java:71` | `@Transactional(propagation = TransactionPropagation.REQUIRES_NEW)`（post Facade 独立事务） |
| A2 | `module-finance/erp-fin-service/.../entity/ErpFinVoucherBizModel.java:79` | `@Transactional(propagation = TransactionPropagation.REQUIRES_NEW)`（reverse Facade 独立事务） |
| A3 | `module-finance/erp-fin-service/.../posting/ErpFinDeferredPostingRetryHelper.java:74` | `transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->`（单条延迟过账重试独立事务） |
| A4 | `module-finance/erp-fin-service/.../posting/ErpFinDeferredPostingRetryHelper.java:131` | `transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->`（O-16 补偿重试独立事务） |
| A5 | `module-finance/erp-fin-service/.../posting/ErpFinPostingExceptionRecorder.java:95` | `transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->`（异常记录独立事务） |
| A6 | `module-finance/erp-fin-service/.../posting/ErpFinPostingExceptionRecorder.java:159` | `transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->`（notify 落库第二独立事务） |

合法性：全部为 `docs/architecture/processor-extension-pattern.md` 硬规则 1「事务入口钉 Facade；跨域失败隔离由 Facade 显式声明传播策略」文档化的合法跨域失败隔离事务边界（A1/A2 = Facade `IErpFinVoucherBiz.post/reverse`；A3-A6 = 过账异常记录/重试 helper 独立 session）。代码注释 `nop-check: allow @Transactional(REQUIRES_NEW)`（ErpFinVoucherBizModel:68,77）交叉引用 owner doc。

**B javadoc 注释行（41 处）**：跨 11 域 `*PostingExecutor.java` / `*PostingDispatcher.java` 的 `* ...{@code REQUIRES_NEW} 承接...` 类 javadoc 引用 + ErpFinPostingExceptionBizModel/Recorder/DeferredRetryHelper 的方法 javadoc + IErpFinVoucherBiz/ErpFinPostingProcessor 接口 javadoc + ErpFinVoucherBizModel:42,49 类 javadoc。全部 `^\s*\*` 前缀。

**C 行注释（4 处）**：`ErpMfgWorkOrderProcessor:213`(`// generateCompletionMove ...`) / `ErpFinVoucherBizModel:68`(`// nop-check: allow ...`) / `ErpFinVoucherBizModel:77`(`// nop-check: allow ...`) / `ErpInvStockMoveBizModel:99`(`// 容错：实体可能因 REQUIRES_NEW ...`)。全部 `^\s*//` 前缀。

#### R6 三态分类（actual 7 = A:2 + B:3 + C:2）

**A 真实代码站点（2 处）**：= R10 的 A1/A2（ErpFinVoucherBizModel post/reverse 的 `@Transactional(REQUIRES_NEW)`）。R6 是 R10 的 BizModel 注解子集（R10 另有 4 处非 BizModel `runInTransaction` 程序化调用）。

**B javadoc 注释行（3 处）**：`ErpFinVoucherBizModel:44`(`* ...特意叠加 @Transactional`) / `ErpPrjTimesheetBizModel:40`(`* @BizMutation 自动包装事务（不叠加 {@code @Transactional}）`) / `ErpPrjTaskBizModel:41`（同前）。

**C 行注释（2 处）**：`ErpFinVoucherBizModel:68,77`（`// nop-check: allow @Transactional(REQUIRES_NEW)`）。

#### R1d 三态分类（actual 28 = A:17 + B:9 + C:2）

**A 真实代码站点（17 处，全部同域只读内部辅助查询，绕过 findList objMeta 管道为有意设计）**：

| 域 | file:line | 用途 |
|----|-----------|------|
| crm(6) | `ErpCrmLeadFunnelBizModel:203` | 漏斗全量快照读取 |
| | `ErpCrmTerritoryBizModel:191,203` | 树形递归子树收集 |
| | `ErpCrmLeadSequenceProgressBizModel:288,294,303` | 序列进度只读聚合 |
| master-data(8) | `ErpMdExchangeRateBizModel:64` | C3 日期范围汇率候选 |
| | `ErpMdMaterialCustomsBizModel:89` | 海关维度候选迭代 |
| | `ErpMdSupplierApprovalBizModel:78` | 同 partner 唯一性前置友好校验 |
| | `ErpMdTaxRateBizModel:57` | C3 日期范围税率候选 |
| | `ErpMdPartnerBizModel:69` | 唯一性 isEmpty 校验 |
| | `ErpMdAcctSchemaBizModel:37` | 全部 acct schema 候选 |
| | `ErpMdMaterialBizModel:102` | 唯一性 isEmpty 校验 |
| | `ErpMdSubjectBizModel:57` | 唯一性 isEmpty 校验 |
| sales(3) | `ErpSalPriceListBizModel:72` | 同维度价格清单歧义判定 |
| | `ErpSalPriceListLineBizModel:61` | 同维度价格清单行歧义判定 |
| | `ErpSalPricingRuleBizModel:79` | 定价规则互斥校验 |

每处代码注释明示「绕过 CrudBizModel findList 管道的 objMeta 过滤/字段投影以保留全部候选」。owner doc 背书：`docs/design/date-ranged-validity-pattern.md`（C3）+ 各 BizModel inline 注释。

**B javadoc 注释行（9 处）**：`ErpCrmLeadFunnelBizModel:174` / `ErpCrmTerritoryBizModel:183` / `ErpMdExchangeRateBizModel:50` / `ErpMdMaterialCustomsBizModel:78` / `ErpMdPartnerBizModel:53` / `ErpMdMaterialBizModel:86` / `ErpMdSubjectBizModel:41` / `ErpSalPriceListBizModel:61` / `ErpSalPriceListLineBizModel:48`（均为 `* <p>实现说明：直接经 {@code dao().findAllByQuery(query)} ...` 类 javadoc）。

**C 行注释（2 处）**：`ErpCrmLeadSequenceProgressBizModel:278`(`// 以下 dao().findAllByQuery 调用均为同域只读内部辅助：`) / `ErpMdAcctSchemaBizModel:34`(`// 经 dao().findAllByQuery 绕过 findList ...`)。

#### R1d +5 漂移源 git 时间线核实（baseline 锚点 `bd8540037` 2026-07-24 11:43:20）

`git diff bd8540037..HEAD -- '*BizModel.java' | grep '^\+.*dao()\.findAllByQuery'` 实测**精确 5 行**，全部来自 sales 域（plan `2026-07-26-0315-1` C3 sales 定价推广），全部晚于基线锚点：

| # | file:line | 形态 | 来源计划 |
|---|-----------|------|----------|
| 1 | `ErpSalPriceListBizModel:61` | javadoc（B） | 0315-1 |
| 2 | `ErpSalPriceListBizModel:72` | 真实调用（A） | 0315-1 |
| 3 | `ErpSalPriceListLineBizModel:48` | javadoc（B） | 0315-1 |
| 4 | `ErpSalPriceListLineBizModel:61` | 真实调用（A） | 0315-1 |
| 5 | `ErpSalPricingRuleBizModel:79` | 真实调用（A） | 0315-1 |

= 3 真实调用 + 2 javadoc 注释行。master-data BizModels 的 `findAllByQuery` 调用由 `2026-07-21-2225-1`（C3，commit `0907e8d8d` 2026-07-21 23:35:54）+ `2026-07-21-1206-1`（C2，commit `5c5d129d9` 2026-07-21 20:21:34）落地，**均早于基线锚点 bd8540037（2026-07-24）**，其调用已计入 baseline 23，post-baseline diff 为 0，不构成漂移。

> 注：diff 另有 1 行 `dao.findAllByQuery`（无 `()`，ErpFinIntercompanyTransferPrice 局部 var `dao`）不匹配 R1d regex `dao()\.findAllByQuery`，不计入漂移。

#### Decision 1：checker 注释排除校准方案 = (A) 行级 grep 过滤，per-rule（option b）

选择 (A) 行级 grep 过滤排除注释行 + per-rule 实施（option b，仅 R1d/R6/R10 三规则管道内加过滤，不动 `rgrep_bizmodel`/`rgrep_prodjava` helper，避免影响已稳定的 R1a/R1b/R1c/R2a/R2b）。

过滤管道：`grep -vE ':[0-9]+:[[:space:]]*(\*|//|/\*|\*/)' | grep -vE '\{@code|\{@link'`
- 第一段排除 javadoc 续行（`*`）+ 行注释（`//`）+ 块注释开闭（`/*`/`*/`）
- 第二段安全网排除含 `{@code`/`{@link` 的行（已被 `*` 覆盖，防回归）

模拟实测（apply 到当前 actual）：R10 51→**6** / R6 7→**2** / R1d 28→**17**，精确对齐三态分类的 A 子集。

替代方案否决：(B) AST 解析——过度工程化，既有 checker 全部 grep 启发式，引入 AST 破坏一致性 + 增加运行时依赖；(C) 仅上调基线不校准——保留注释虚假命中使基线语义模糊，未来注释增减触发虚假漂移。

残留风险：行级 grep 可能漏排除块注释 `/* ... */` 内的跨行命中（无 `*` 续行前缀）——Phase 1 实测三规则块注释跨行命中为 0；若未来出现，升级为 AST 解析（开独立 successor）。

#### Decision 2：基线裁决

| 规则 | 旧基线 | 新基线 | 裁决依据 |
|------|--------|--------|----------|
| R1d | 23 | **17** | 注释校准后真实代码计数（=A 子集 17）。pre-baseline master-data 合法调用（2225-1/1206-1，8 处）+ post-baseline sales 合法漂移（0315-1，3 处）+ crm 既有合法（6 处）= 17。全部为同域只读内部辅助查询（C3 互斥校验 + 价格清单维度查询 + 树形递归 + 唯一性前置友好校验），按基线规则裁决为「合法模式」。 |
| R10 | 51 | **6** | 注释校准后真实代码计数（=A 子集 6）。全部为 `processor-extension-pattern.md` 硬规则 1 文档化的合法跨域失败隔离事务边界（Facade `IErpFinVoucherBiz.post/reverse` + 过账异常记录/重试独立事务）。原 51 grandfathered 基线为注释虚假命中主导，校准后下调至真实站点。 |
| R6 | 7 | **2** | 注释校准后真实代码计数（=A 子集 2 = R10 BizModel 子集 ErpFinVoucherBizModel post/reverse）。原 7 grandfathered 基线为注释虚假命中主导，校准后下调至真实站点。 |

裁决性吸收：R1d +5 漂移经注释校准 + 裁决性下调基线（23→17）双重吸收——注释校准排除 2 javadoc 漂移行，剩余 3 真实调用经合法性裁决计入新基线 17（pre+post 既有合法调用一并反映真实代码计数）。

### Phase 2 - checker 注释校准实施 + 基线更新

Status: completed
Targets: `docs/audits/nop-compliance-checker.sh`（R1d/R6/R10 段注释排除逻辑 + `rgrep_bizmodel`/`rgrep_prodjava` helper 或各规则的 pipe 后过滤）、`docs/audits/compliance-baseline.md`（基线表 + machine-readable 块 + 裁决注记）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Item Types Note: Phase 2 is Fix-heavy（3 处 checker 注释排除校准）+ Add（基线文档裁决注记）
- Prereqs: Phase 1 裁决完成

- [x] `Fix`：按 Phase 1 Decision 校准 `nop-compliance-checker.sh`——为 R1d/R6/R10 三规则的 grep 管道追加注释行排除过滤（排除 `^\s*\*` javadoc 行 + `^\s*//` 行注释行；含 `{@code`/`{@link` 的行已被 `^\s*\*` 覆盖，显式追加为安全网防回归）。校准后复跑 checker 确认 R1d/R6/R10 actual 下降至真实代码计数。实施方式裁决：(a) 在 `rgrep_bizmodel`/`rgrep_prodjava` helper 内统一加过滤（影响所有使用 helper 的规则）；(b) 仅在 R1d/R6/R10 三规则管道内单独加过滤（最小爆破半径）。推荐 (b)：最小侵入，避免影响已稳定的 R1a/R1b/R1c/R2a/R2b 等规则计数（这些规则的注释命中经前序计划证实已为 0 或已校准）。
      - Skill: `nop-backend-dev`
- [x] `Add`：更新 `docs/audits/compliance-baseline.md`——(a) 基线表 R1d/R6/R10 值更新为注释校准后真实代码计数；(b) `## BASELINE (machine-readable)` 块同步更新；(c) 新增裁决注记段（§R1d/R10/R6 注释校准 + 基线裁决注记 plan 2026-07-27-0823-1）：记录三规则 checker 注释排除校准（排除 javadoc `*` / 行注释 `//` / `{@code`/`{@link` 行）+ R1d +5 漂移源（C3/C2/定价推广三已审计计划）+ 逐项合法性分类 + R10/R6 真实站点 owner doc 背书（`processor-extension-pattern.md` 硬规则 1）+ 纪律强化引用。
      - Skill: `nop-backend-dev`
- [x] `Add`：`docs/analysis/governed-path-cost-evaluation.md` 补「checker 注释排除校准范式」段——记录行级 grep 注释排除作为 checker 校准的标准手段（对齐 R8 content-based 排除先例），引用本计划 + 1057-1/1057-2/0941-2 三先例构成 checker 校准范式矩阵。
      - Skill: none

Exit Criteria:

- [x] checker R1d/R6/R10 三规则注释排除校准后实测下降至真实代码计数（R10 ~6 / R6 ~2 / R1d ~17），各 ≤ 新基线
- [x] `compliance-baseline.md` 基线表 + machine-readable 块 + 裁决注记更新完成
- [x] `governed-path-cost-evaluation.md` checker 校准范式段补齐

### Phase 3 - CI green 验证 + 全仓库回归

Status: completed
Targets: `docs/audits/compliance-baseline.md`（最终基线）、CI workflow 本地等价验证
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 2 完成

- [x] `Proof`：复跑 `bash docs/audits/nop-compliance-checker.sh` 确认全 16 规则 actual ≤ baseline（重点验证 R1d ≤ 新基线 / R10 ≤ 新基线 / R6 ≤ 新基线，且 R1d CI red 已解除）。
      - Skill: none
- [x] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）确认 checker 脚本校准不影响构建（checker 为独立 shell 脚本，不侵入 Java 构建链路，此步为回归保险）。
      - Skill: none
- [x] `Proof`：模拟 CI workflow 的 baseline gate 逻辑（python 解析 checker 汇总表 vs baseline machine-readable 块），确认本地等价验证通过（actual ≤ baseline 全绿，R1d red 已解除）。
      - Skill: none

Exit Criteria:

- [x] checker 复跑全 16 规则 actual ≤ baseline（CI gate 等价验证通过，R1d CI red 已解除）
- [x] 全仓库 BUILD SUCCESS（checker 校准无构建回归）

## Draft Review Record

- Independent draft review iteration 1: `needs-revision`（`ses_05f0a108fffenHB9SrHCR13Hrk`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-27）— 0 Blocker / 2 Major / 4 Minor。全部 load-bearing 数值主张（R1d=28/R6=7/R10=51/baseline R1d=23/CI red/三态分类 ~6-45/~2-5/~17-11）经实时仓库逐行核实**精确匹配**；owner doc + 1057-1 Deferred successor trigger 经核实确认；Rule 1/2/4/6/7/14 + anti-slack 全 PASS。**2 Major 已全部修订**：(M1) R1d +5 漂移源归因错误——原草案列 2225-1/1206-1/0315-1 三计划，实测 git diff `bd8540037..HEAD` 证实 2225-1(2026-07-21)/1206-1(2026-07-21) 均**早于**基线锚点 bd8540037(2026-07-24)，其 findAllByQuery 调用已计入 baseline 23 不构成漂移；+5 漂移**全部来自 0315-1**(2026-07-26，3 真实调用 + 2 javadoc 行)；原「经 git 提交时间线核实」为虚假主张已删除，Current Baseline + Phase 1 Proof/Decision 已按 git diff 实测修正；(M2) Phase 1 Proof「5 处真实调用」混淆 checker 命中与真实调用，已改为「+5 checker 漂移 = 3 真实调用 + 2 javadoc 注释行」。**4 Minor 已全部修订**：(m1) processor-extension-pattern.md 未含字面 "REQUIRES_NEW"，措辞改为「独立事务边界硬规则 1（Facade 显式声明传播策略）」+ 代码注释交叉引用桥接；(m2) 「R6 与 R10 完全重叠」改为「R6 是 R10 BizModel 子集」；(m3) `{@code`/`{@link` 过滤标注为 `^\s*\*` 覆盖的安全网防回归；(m4) Phase 2 helper-vs-per-rule 子裁决保留 inline 形式（Rule 9 内容要求已满足）。
- Independent draft review iteration 2: `acceptable-as-is`（`ses_05f046e26ffexQbV85o5ZIhYfd`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-27）— 0 Blocker / 0 Major / 0 Minor。M-1/M-2 修订经实时仓库 git diff 独立复核精确匹配：master-data post-baseline findAllByQuery diff = 0（2225-1/1206-1 确为 pre-baseline 已计入 baseline 23）+ sales post-baseline diff = 5 行（3 真实调用 + 2 javadoc，全部来自 0315-1）；core numeric baseline 复跑确认 R1d=28/R6=7/R10=51 + baseline R1d=23 CI red 不变；4 Minor 修订逐项 spot-check 通过；Rules 1-14 + anti-slack 全 PASS；无修订引入新问题。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划触及 checker shell 脚本（R1d/R6/R10 注释排除逻辑）+ 合规基线文档。无 ORM/契约/字典/Java 业务代码变更。完整仓库验证：`mvn clean install -DskipTests`（回归保险）+ checker 复跑（CI gate 等价）。

- [x] 范围内行为完成（R1d/R6/R10 checker 注释校准 + R1d 漂移裁决 + 基线对齐 + CI green 恢复）
- [x] 相关文档对齐（compliance-baseline.md + governed-path-cost-evaluation.md）
- [x] 已运行验证：`mvn clean install -DskipTests` + checker 复跑（全 16 规则 actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### R10/R6 真实代码站点的语义级事务边界审计

- Classification: `watch-only residual`
- Why Not Blocking Closure: 真实 R10/R6 站点（~6/~2）全部为 `processor-extension-pattern.md` 硬规则 1 文档化的合法跨域失败隔离事务边界（Facade `IErpFinVoucherBiz.post/reverse` + 过账异常记录/重试独立事务），经 owner doc 背书 + 代码注释 `nop-check:` 标记。本计划仅做 checker 注释校准使 actual 反映真实站点，不审计事务边界的语义正确性。
- Successor Required: `no`（触发条件：REQUIRES_NEW 出现功能性问题时——如跨域失败隔离事务边界导致数据不一致报告；当前无此类报告）

### R1d 真实 `findAllByQuery` 站点的 findList 管道对齐

- Classification: `watch-only residual`
- Why Not Blocking Closure: 真实 R1d 站点（~17）全部为同域只读内部辅助查询，每处代码注释明示「绕过 CrudBizModel findList 管道的 objMeta 过滤/字段投影以保留全部候选」为有意设计。本计划仅做 checker 注释校准 + 基线裁决，不重构为 findList 管道（findList 管道的 objMeta 过滤会破坏这些内部辅助查询的语义）。
- Successor Required: `no`（触发条件：findList 管道支持「绕过 objMeta 过滤」选项时，或 objMeta 过滤语义变更使这些查询可经 findList 表达时）

## Closure

Status Note: 三阶段全部完成——Phase 1 三态分类（R10 51=A6+B41+C4 / R6 7=A2+B3+C2 / R1d 28=A17+B9+C2）+ R1d +5 漂移源 git 时间线核实（全部来自 0315-1，master-data pre-baseline）+ 校准方案/基线裁决 Decision 落盘；Phase 2 checker R1d/R6/R10 注释排除校准（per-rule option b）+ compliance-baseline.md 基线表/machine-readable 块/裁决注记 + governed-path-cost-evaluation.md checker 校准范式矩阵；Phase 3 验证全绿（checker actual≤baseline 全 16 规则，R1d 17≤17/R6 2≤2/R10 6≤6，R1d CI red 解除；154 模块 `mvn clean install -DskipTests` BUILD SUCCESS；CI workflow baseline gate python 模拟 PASSED exit 0）。无 ORM/契约/字典/Java 业务代码变更（纯合规治理）。两项 watch-only residual 已 adjudicated，Successor Required=no。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话 `ses_05ef94212ffezGsRZh4jlAOUJd`，冷重播无执行者上下文，2026-07-27）
- Verdict: `pass` — 计划可标记 `Plan Status: completed`
- Evidence: 全部 load-bearing 主张经实时仓库独立复核精确匹配——(A) checker R1d/R6/R10 per-rule 注释排除校准（helper 未动）；(B) compliance-baseline.md 基线表与 YAML 块一致（R1d=17/R6=2/R10=6）+ 裁决注记段存在；(C) 三态分类复跑 R10 51→6 / R6 7→2 / R1d 28→17 精确（6 处 R10 真实站点全为 `@Transactional`/`runInTransaction` 代码非注释）；(D) `git diff bd8540037..HEAD` 精确 5 行 +5 漂移全在 sales 域；(E) `processor-extension-pattern.md` 硬规则 1 owner doc 背书 REQUIRES_NEW；(F) CI gate python 模拟 `baseline rules: 19 | parsed actual rules: 19 → Gate PASSED` exit 0；(G) 全 Phase 1/2/3 `[x]`+`Status: completed`，Non-Goals 守护（git status 仅 4 文件：checker shell + 2 markdown + plan，无 ORM/契约/字典/Java 业务代码）。Draft Review 2 迭代收敛。1 Minor（预存的「16 规则」措辞，实际 19 规则，R12a/b/c 加入后未更新表头——非阻塞，CI gate 正确解析全 19 规则）。

Executor Evidence（执行者记录，供独立审计复核）：
- Phase 1 Evidence：见本计划 §Phase 1 Evidence（R10/R6/R1d 三态分类 file:line 清单 + R1d +5 漂移源 git diff 表 + Decision 1/2）
- Phase 2 产物：`docs/audits/nop-compliance-checker.sh` R1d(行 107-115)/R6(行 233-240)/R10(行 333-343) 注释排除校准；`docs/audits/compliance-baseline.md` 基线表 R1d 23→17/R6 7→2/R10 51→6 + machine-readable 块同步 + §R1d/R10/R6 注释校准+基线裁决注记；`docs/analysis/governed-path-cost-evaluation.md` §3.8 checker 校准范式矩阵
- Phase 3 验证：`bash docs/audits/nop-compliance-checker.sh` 汇总表（R1d=17/R6=2/R10=6，其余不变）；`mvn clean install -DskipTests` BUILD SUCCESS（154 模块，exit 0，01:35 min）；CI gate python 模拟 `OK: no rule exceeds baseline. Gate PASSED.`（exit 0）

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
